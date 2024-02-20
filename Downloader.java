import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.gson.*;
import javafx.util.Pair;

public class Downloader
{
    public static void main(String[] args)
    {
        final String saveManifestOption = "--save-manifests";
        final String libsOnlyOption = "--libs-only";
        final String assetsOnlyOption = "--assets-only";
        final String dryRunOption = "--dry-run";
        final String helpMenu = "Usage: ManifestDownloader [options] [version/manifest] [downloadDir]\n" +
                                "       ManifestDownloader --save-manifests [version]\n" +
                            "Default behaviour: Download libs and assets\n" +
                            "Options: \n  " +
                            saveManifestOption + ": download and save the version's manifest\n  " +
                            libsOnlyOption + ": download only the libraries\n  " +
                            assetsOnlyOption + ": download only the assets" +
                            dryRunOption + ": test without downloading anything";
        String path = null, downloadDir = null;
        boolean saveManifests, libsOnly, assetsOnly, isDryRun;
        saveManifests = libsOnly = assetsOnly = isDryRun = false;

        if(args.length < 2)
        {
            System.out.println(helpMenu);
            System.exit(-1);
        }

        if(args.length == 2 && Objects.equals(args[0], saveManifestOption))
        {
            DownloadManifests(args[1]);
            System.exit(0);
        }

        for(String arg : args)
        {
            switch(arg)
            {
                case saveManifestOption: saveManifests = true; break;
                case libsOnlyOption: libsOnly = true; break;
                case assetsOnlyOption: assetsOnly = true; break;
                case dryRunOption: isDryRun = true;
                default: if(path == null) { path = arg; } else if(downloadDir == null) { downloadDir = arg; } break;
            }
        }

        Download(path, downloadDir, saveManifests, libsOnly, assetsOnly);
        //ExtractZipFile("jinput-platform-2.0.5-natives-windows.jar", "extractedJar" + File.separator);
//        boolean isDryRun = Arrays.asList(args).contains("--dry-run");
//        String path = args[0];
//        String downloadDir = args[1];
//        boolean dryRun = Integer.parseInt(args[2]) > 0;
//        JsonObject manifest = ReadManifest(path);
//
//        if(manifest != null)
//        {
//            ParseManifest(manifest, downloadDir, dryRun);
//        }
//        for(String l : ManifestParser.GetLibsFromVersion(ManifestParser.GetManifestFromVersion("1.5.2"), "linux"))
//        {
//            System.out.println(l);
//        }
        //System.out.println(GetOSName());
        //ParseManifest(ManifestParser.GetManifestFromVersion("1.5.2"), "aa", true);
        //DownloadURLs(ManifestParser.GetLibsFromVersion(ManifestParser.GetManifestFromVersion("b1.7.3"), "windows"), "dl");
        //DownloadLibsFromVersion("b1.7.3", "dl");
        //System.out.println(ManifestParser.GetClientURLFromVersion(ManifestParser.GetManifestFromVersion("1.5.2")));
        //System.out.println(ManifestParser.GetAssetIndexFromVersion(ManifestParser.GetManifestFromVersion("1.9.4")));
//        for(Pair<String, String> asset : ManifestParser.GetAssetsFromAssetIndex(ManifestParser.GetAssetIndexFromVersion(ManifestParser.GetManifestFromVersion("1.9.4"))))
//        {
//            System.out.println("Name: " + asset.getKey() + " Hash: " + asset.getValue());
//        }
        //DownloadResourcesFromAssetIndex("1.8.9", "assets/objects");
    }

    public static ManifestWrapper DownloadManifests(String version)
    {
        JsonObject manifest = ManifestParser.GetManifestFromVersion(version);
        JsonObject assets = ManifestParser.GetAssetIndexFromVersion(manifest);
        String manifestPath = Launcher.VERSIONS_DIR + version + File.separator + version + ".json";
        String assetsIndexPath = Launcher.ASSETS_DIR + "indexes" + File.separator + ManifestParser.GetAssetIndexVersionFromVersion(manifest) + ".json";

        ManifestParser.SaveJsonObject(manifest, manifestPath);
        System.out.println("Saved version manifest to " + manifestPath);
        ManifestParser.SaveJsonObject(assets, assetsIndexPath);
        System.out.println("Saved assets index to " + assetsIndexPath);

        return new ManifestWrapper(manifest, assets);
    }

    public static void Download(String path, String downloadDir, boolean saveManifests, boolean libsOnly, boolean assetsOnly)
    {
        downloadDir += File.separator;
        JsonObject versionManifest = null, assetsManifest = null;
        if(path.contains(".json"))
        {
            versionManifest = ManifestParser.GetJsonObjectFromFile(path);
            assetsManifest = ManifestParser.GetAssetIndexFromVersion(versionManifest);
        }
        else
        {
            ManifestWrapper manifests = DownloadManifests(path);
            versionManifest = manifests.GetVersionManifest();
            assetsManifest = manifests.GetAssetsManifest();
        }

        DownloadClientJar(versionManifest, downloadDir + Launcher.VERSIONS_DIR + path);
        DownloadServerJar(versionManifest, downloadDir + Launcher.VERSIONS_DIR + path);

        if(libsOnly || assetsOnly)
        {
            if(libsOnly)
            {
                DownloadLibsFromVersion(versionManifest, downloadDir + "/libs/");
            } else { DownloadAssetsFromAssetIndex(assetsManifest, downloadDir + "/assets/"); }
        }
        else
        {
            DownloadLibsFromVersion(versionManifest, downloadDir + Launcher.LIBRARIES_DIR);
            DownloadAssetsFromAssetIndex(assetsManifest, downloadDir + Launcher.ASSETS_DIR);
        }
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
        String separator = File.separator.equals("\\") ? "\\\\" : "/";
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
        //DownloadURLs(ManifestParser.GetLibsFromVersion(manifest, GetOSName()), destinationPath);
        Pair<String, String>[] libraries = ManifestParser.GetLibsFromVersion(manifest, GetOSName());
        System.out.println("Need to get " + libraries.length + " libraries");
        String separator = File.separator.equals("\\") ? "\\\\" : "/";

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

    public static void DownloadAssetsFromAssetIndex(JsonObject manifest, String destinationPath)
    {
        Pair<String, String>[] assets = ManifestParser.GetAssetsFromAssetIndex(manifest);
        System.out.println("Need to get " + assets.length + " assets");

        for(int i = 0; i < assets.length; i++)
        {
            Pair<String, String> asset = assets[i];
            String url = "https://resources.download.minecraft.net/" + asset.getValue().substring(0, 2) + "/" + asset.getValue();
            String dest = destinationPath + "objects" + File.separator + asset.getValue().substring(0, 2) + File.separator + asset.getValue();
            System.out.println("Downloading (" + (i+1) + "/" + assets.length + ")" + ": " + url);
            int response;
            if((response = DownloadURL(url, dest)) == 200)
            {
                System.out.println("Saved to: " + dest);
            } else { System.out.println("HTTP status code " + response); }
        }
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
