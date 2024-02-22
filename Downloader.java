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
            assetsManifest = ManifestParser.GetJsonObjectFromFile(Launcher.ASSETS_INDEXES_DIR + ManifestParser.GetAssetIndexVersionFromVersion(versionManifest) + ".json");
        } else
        {
            ManifestWrapper mw = DownloadManifests(path);
            versionManifest = mw.GetVersionManifest();
            assetsManifest = mw.GetAssetsManifest();
        }

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
            DownloadAssetsFromAssetIndex(assetsManifest, downloadDir + Launcher.ASSETS_DIR);
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
            if(!new File(dest).exists()) { missingLibs.add(lib); }
        }

        return missingLibs.toArray(new Pair[0]);
    }

    public static void DownloadClientJar(JsonObject manifest, String destinationPath)
    {
        String url = ManifestParser.GetClientJarURLFromVersion(manifest);
        DownloadURLs(new String[] { url }, destinationPath);
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
        boolean mapToResources = false;
        try
        {
            mapToResources = assetManifest.get("map_to_resources").getAsBoolean();
        } catch (NullPointerException e) { }
        Pair<String, String>[] assets = CompareAssets(ManifestParser.GetAssetsFromAssetIndex(assetManifest), destinationPath, mapToResources);
        System.out.println("Need to get " + assets.length + " assets");

        for(int i = 0; i < assets.length; i++)
        {
            Pair<String, String> asset = assets[i];
            String url = "https://resources.download.minecraft.net/" + asset.getValue().substring(0, 2) + "/" + asset.getValue();
            String dest = mapToResources ? Launcher.DEFAULT_MC_PATH + Launcher.RESOURCES_DIR + asset.getKey() : destinationPath + "objects" + File.separator + asset.getValue().substring(0, 2) + File.separator + asset.getValue();
            System.out.println("Downloading (" + (i+1) + "/" + assets.length + ")" + ": " + url);
            int response;
            if((response = DownloadURL(url, dest)) == 200)
            {
                System.out.println("Saved to: " + dest);
            } else { System.out.println("HTTP status code " + response); }
        }
    }

    public static Pair<String, String>[] CompareAssets(Pair<String, String>[] assets, String destinationPath, boolean mapToResources)
    {
        List<Pair<String, String>> missingAssets = new ArrayList<>();
        System.out.println("Comparing assets...");
        for(Pair<String, String> asset : assets)
        {
            String dest = mapToResources ? Launcher.DEFAULT_MC_PATH + Launcher.RESOURCES_DIR + asset.getKey() : destinationPath + "objects" + File.separator + asset.getValue().substring(0, 2) + File.separator + asset.getValue();
            if(!new File(dest).exists()) { missingAssets.add(asset); }
        }

        return missingAssets.toArray(new Pair[0]);
    }

    public static void ExtractZipFile(String archivePath, String destinationPath)
    {
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
    }
}
