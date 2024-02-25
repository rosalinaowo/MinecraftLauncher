import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.google.gson.*;

public class Downloader
{
    static String separator = File.separator.equals("\\") ? "\\\\" : "/";
    public static void main(String[] args)
    {
        final String manifestsOnlyOption = "--manifests-only";
        final String jarsOnlyOption = "--jars-only";
        final String libsOnlyOption = "--libs-only";
        final String assetsOnlyOption = "--assets-only";
        final String dryRunOption = "--dry-run";
        final String helpMenu = "Usage: Downloader [options] [version/manifest] [destination path]\n" +
                                "Default behaviour: Download everything (jars, libs, and assets)\n" +
                                "Options:\n  " +
                                manifestsOnlyOption + ": download only the manifests\n  " +
                                jarsOnlyOption + ": download only client and server jars\n  " +
                                libsOnlyOption + ": download only the libraries\n  " +
                                assetsOnlyOption + ": download only the assets\n  " +
                                dryRunOption + ": test without downloading anything";
        String path = null, downloadDir = null;
        boolean[] flags = new boolean[4]; // jars, libs, assets, dry run
        Arrays.fill(flags, true); // assume we want to download everything

        if(args.length < 2)
        {
            System.out.println(helpMenu);
            System.exit(0);
        }

        for(String arg : args)
        {
            if(arg.startsWith("--"))
            {
                switch(arg)
                {
                    case "--help": System.out.println(helpMenu); System.exit(0);
                    case manifestsOnlyOption: Arrays.fill(flags, false); break; // assets are downloaded/loaded in all cases
                    case jarsOnlyOption: Arrays.fill(flags, false); flags[0] = true; break;
                    case libsOnlyOption: Arrays.fill(flags, false); flags[1] = true; break;
                    case assetsOnlyOption: Arrays.fill(flags, false); flags[2] = true; break;
                    case dryRunOption: flags[3] = true;
                    default: System.out.println("Invalid flag. Exiting."); System.exit(-1);
                }
            }
            else
            {
                if(path == null)
                {
                    path = arg;
                } else { downloadDir = arg; }
            }
        }

        Download(path, downloadDir, flags[0], flags[1], flags[2]);
    }

    public static void Download(String path, String downloadDir, boolean jars, boolean libs, boolean assets)
    {
        downloadDir += File.separator;
        JsonObject versionManifest = null, assetsManifest = null;
        if(path.contains(".json"))
        {
            versionManifest = ManifestParser.GetJsonObjectFromFile(path);
            //assetsManifest = ManifestParser.GetJsonObjectFromFile(Launcher.ASSETS_INDEXES_DIR + ManifestParser.GetAssetIndexVersionFromVersion(versionManifest) + ".json");
            assetsManifest = ManifestParser.GetLocalAssetIndexFromVersion(versionManifest);
        } else
        {
            ManifestWrapper mw = DownloadManifests(path);
            versionManifest = mw.GetVersionManifest();
            assetsManifest = mw.GetAssetsManifest();
        }

        if(jars && libs && assets) { DownloadJava(versionManifest); }
        if(jars)
        {
            String versionPath;
            if(path.contains(".json"))
            {
                versionPath = ManifestParser.GetGameVersionFromVersion(versionManifest);
            } else { versionPath = path; }
            DownloadClientJar(versionManifest, downloadDir + Launcher.VERSIONS_DIR + versionPath);
            DownloadServerJar(versionManifest, downloadDir + Launcher.VERSIONS_DIR + versionPath);
        }
        if(libs)
        {
            DownloadLibsFromVersion(versionManifest, downloadDir + Launcher.LIBRARIES_DIR);
        }
        if(assets)
        {
            DownloadAssetsFromAssetIndex(assetsManifest, downloadDir);
        }
    }

    public static ManifestWrapper DownloadManifests(String version)
    {
        JsonObject manifest = ManifestParser.GetManifestFromVersion(version);
        JsonObject assets = ManifestParser.GetAssetIndexFromVersion(manifest);
        String manifestPath = Launcher.VERSIONS_DIR + version + File.separator + version + ".json";
        String assetsIndexPath = Launcher.ASSETS_INDEXES_DIR + ManifestParser.GetAssetIndexVersionFromVersion(manifest) + ".json";

        ManifestParser.SaveJsonObject(manifest, manifestPath);
        System.out.println("Saved version manifest to " + manifestPath);
        ManifestParser.SaveJsonObject(assets, assetsIndexPath);
        System.out.println("Saved assets index to " + assetsIndexPath);

        return new ManifestWrapper(manifest, assets);
    }

    public static String GetOSName()
    {
        String name = System.getProperty("os.name").toLowerCase().split(" ")[0];
        switch (name)
        {
            case "mac": return "osx";
            default: return name;
        }
    }

    private static int DownloadURL(String urlString, String destination)
    {
        try
        {
            URL url = new URL(urlString);
            HttpURLConnection req = (HttpURLConnection) url.openConnection();
            req.setRequestMethod("GET");
            if(req.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                File destinationFile = new File(destination);
                if(!destinationFile.getParentFile().exists())
                {
                    destinationFile.getParentFile().mkdirs();
                }

                InputStream istream = req.getInputStream();
                FileOutputStream ostream = new FileOutputStream(destination);
                byte[] buffer = new byte[4096];
                int bytesRead;

                while((bytesRead = istream.read(buffer)) != -1)
                {
                    ostream.write(buffer, 0, bytesRead);
                }

                ostream.close();
                istream.close();
                req.disconnect();
            }
            return req.getResponseCode();
        } catch (IOException e) { e.printStackTrace(); }
        return -1;
    }

    /**
     *
     * @param urls              URLs to download
     * @param destinationPath   Destination folder
     */
    private static void DownloadURLs(String[] urls, String destinationPath)
    {
        for(String url : urls)
        {
            String destDirty = destinationPath + File.separator + url.split("/")[url.split("/").length - 1]; // .../fileName
            String dest = destDirty.replaceAll(separator + "+", separator); // Clean path for excess of file separators
            System.out.println("Downloading: " + url);
            int response;
            if((response = DownloadURL(url, dest)) == 200)
            {
                System.out.println("Saved to: " + dest);
            } else { System.out.println("HTTP status code " + response); }
        }
    }

    public static void DownloadLibsFromVersion(JsonObject manifest, String destinationPath)
    {
        Pair<String, String>[] libraries = CompareLibraries(ManifestParser.GetLibsFromVersion(manifest, GetOSName()), destinationPath);
        System.out.println("Need to get " + libraries.length + " libraries");

        for(int i = 0; i < libraries.length; i++)
        {
            Pair<String, String> lib = libraries[i];
            String fileName = lib.getValue().split("/")[lib.getValue().split("/").length - 1];
            String dest = destinationPath + lib.getKey().replaceAll("/", separator) + fileName;
            System.out.println("Downloading (" + (i+1) + "/" + libraries.length + ")" + ": " + lib.getValue());

            int response;
            if((response = DownloadURL(lib.getValue(), dest)) == 200)
            {
                System.out.println("Saved to: " + dest);
                if(dest.contains("-natives-"))
                {
                    String nativesPath = Launcher.VERSIONS_DIR + manifest.get("id").getAsString() + File.separator + "natives" + File.separator;
                    ExtractZipFile(dest, nativesPath);
                }
            } else { System.out.println("HTTP status code " + response); }
        }
    }

    public static Pair<String, String>[] CompareLibraries(Pair<String, String>[] libraries, String destinationPath)
    {
        List<Pair<String, String>> missingLibs = new ArrayList<>();
        System.out.println("Comparing libs...");
        for(Pair<String, String> lib : libraries)
        {
            String fileName = lib.getValue().split("/")[lib.getValue().split("/").length - 1];
            String dest = destinationPath + lib.getKey().replaceAll("/", separator) + fileName;
            if(!new File(dest).exists() || dest.contains("-natives-")) { missingLibs.add(lib); }
        }

        return missingLibs.toArray(new Pair[0]);
    }

    public static void DownloadClientJar(JsonObject manifest, String destinationPath)
    {
        String url = ManifestParser.GetClientJarURLFromVersion(manifest);
        String dest = (destinationPath + File.separator + manifest.get("id").getAsString() + ".jar").replaceAll(separator + "+", separator);
        System.out.println("Downloading: " + url);
        int response;
        if((response = DownloadURL(url, dest)) == 200)
        {
            System.out.println("Saved to: " + dest);
        } else { System.out.println("HTTP status code " + response); }
    }

    public static void DownloadServerJar(JsonObject manifest, String destinationPath)
    {
        String url = ManifestParser.GetServerJarURLFromVersion(manifest);
        if(url != null)
        {
            DownloadURLs(new String[] { url }, destinationPath);
        } else { System.out.println("No server jar found."); }
    }

    public static void DownloadAssetsFromAssetIndex(JsonObject assetManifest, String destinationPath)
    {
        boolean[] flags = ManifestParser.GetSpecialAssetsFlags(assetManifest);
        destinationPath = ManifestParser.GetAssetsDirFromAssetIndex(assetManifest, destinationPath);
        Pair<String, String>[] assets = CompareAssets(ManifestParser.GetAssetsFromAssetIndex(assetManifest), destinationPath, flags[0], flags[1]);
        System.out.println("Need to get " + assets.length + " assets");

        for(int i = 0; i < assets.length; i++)
        {
            Pair<String, String> asset = assets[i];
            String url = "https://resources.download.minecraft.net/" + asset.getValue().substring(0, 2) + "/" + asset.getValue();
            String dest = destinationPath + ManifestParser.GetAssetRelativePathFromAssetPair(asset, flags[0], flags[1]);
            System.out.println("Downloading (" + (i+1) + "/" + assets.length + ")" + ": " + url);
            int response;
            if((response = DownloadURL(url, dest)) == 200)
            {
                System.out.println("Saved to: " + dest);
            } else { System.out.println("HTTP status code " + response); }
        }
    }

    public static Pair<String, String>[] CompareAssets(Pair<String, String>[] assets, String destinationPath, boolean isMapToResources, boolean isVirtual)
    {
        List<Pair<String, String>> missingAssets = new ArrayList<>();
        System.out.println("Comparing assets...");
        for(Pair<String, String> asset : assets)
        {
            String dest = destinationPath + ManifestParser.GetAssetRelativePathFromAssetPair(asset, isMapToResources, isVirtual);
            if(!new File(dest).exists()) { missingAssets.add(asset); }
        }

        return missingAssets.toArray(new Pair[0]);
    }

    public static String[] ExtractZipFile(String archivePath, String destinationPath)
    {
        List<String> extractedFiles = new ArrayList<>();
        System.out.println("Extracting " + archivePath);
        File dest = new File(destinationPath);
        if(!dest.exists()) { dest.mkdirs(); }
        try
        {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archivePath));
            ZipEntry entry;
            while((entry = zipInputStream.getNextEntry()) != null)
            {
                if(entry.isDirectory()) // If the entry is a directory, create it
                {
                    extractedFiles.add(entry.getName());
                    new File(destinationPath, entry.getName()).mkdirs();
                    continue;
                }

                // If it isn't, extract the file
                FileOutputStream outputStream = new FileOutputStream(new File(destinationPath, entry.getName()));
                byte[] buffer = new byte[4096];
                int bytesRead;
                while((bytesRead = zipInputStream.read(buffer)) != -1)
                {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
            }

            zipInputStream.close();
            System.out.println("Extracted " + archivePath + " to " + destinationPath);
        } catch (IOException e) { e.printStackTrace(); }
        return extractedFiles.toArray(new String[0]);
    }

    public static void DownloadJava(JsonObject manifest)
    {
        // API used: https://do-api.adoptopenjdk.net/q/swagger-ui/#/Binary/getBinary
        String arch = System.getProperty("os.arch"), os = System.getProperty("os.name").toLowerCase();
        switch(arch)
        {
            case "x86": case "i386": arch = "x86"; break;
            case "x86_64": case "amd64": arch = "x64"; break;
        }
        if(os.contains("windows"))
        {
            os = "windows";
        } else if(os.contains("linux"))
        {
            os = "linux";
        } else if(os.contains("mac")) { os = "mac"; }
        Pair<String, Integer> runtimeData = ManifestParser.GetJavaRuntimeFromVersion(manifest);
        String path = runtimeData.getKey();
        int version = runtimeData.getValue();
        File runtime = new File(Launcher.RUNTIME_PATH);
        runtime.mkdirs();
        if(ManifestParser.GetRuntimePathFromVersion(manifest).exists())
        {
            System.out.println("Java " + version + " already installed");
            return;
        }
        String dest = Launcher.RUNTIME_PATH + "jre" + version + os + arch + ".zip";
        String url = "https://api.adoptopenjdk.net/v3/binary/latest/" + version + "/ga/" + os + "/" + arch + "/jre/hotspot/normal/adoptopenjdk";
        System.out.println("Trying to get JRE " + version + " for " + os + " " + arch);
        int response;
        if((response = DownloadURL(url, dest)) == 200)
        {
            System.out.println("Downloaded " + dest);
            File extractedDir = new File(Launcher.RUNTIME_PATH + ExtractZipFile(dest, Launcher.RUNTIME_PATH)[0]);
            extractedDir.renameTo(new File(Launcher.RUNTIME_PATH + path));
            new File(dest).delete();
        } else { System.out.println("HTTP status code " + response); }
    }
}
