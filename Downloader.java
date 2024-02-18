import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.*;
import javafx.util.Pair;

public class Downloader
{
    public static void main(String[] args)
    {
//        if(args.length < 3)
//        {
//            System.out.println("Usage: ManifestDownloader [manifest] [downloadDir] ...");
//            System.out.println("Options:\n  --dry-run: only show libraries");
//            System.exit(-1);
//        }
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
        DownloadResourcesFromAssetIndex("1.8.9", "assets/objects");
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
                byte[] buffer = new byte[1024];
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

    private static void DownloadURLs(String[] urls, String destinationPath)
    {
        for(String urlString : urls)
        {
            System.out.println("Downloading: " + urlString);
            try
            {
                URL url = new URL(urlString);
                HttpURLConnection req = (HttpURLConnection) url.openConnection();
                req.setRequestMethod("GET");
                if(req.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    InputStream istream = req.getInputStream();
                    String dest = destinationPath + File.separator + urlString.split("/")[urlString.split("/").length - 1];
                    FileOutputStream ostream = new FileOutputStream(dest);
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while((bytesRead = istream.read(buffer)) != -1)
                    {
                        ostream.write(buffer, 0, bytesRead);
                    }

                    ostream.close();
                    istream.close();
                    req.disconnect();

                    System.out.println("Downloaded: " + dest);
                }
                else
                {
                    System.out.println("Failed to download library: HTTP status code " + req.getResponseCode());
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public static void DownloadLibsFromVersion(String version, String destinationPath)
    {
        //DownloadURLs(ManifestParser.GetLibsFromVersion(ManifestParser.GetManifestFromVersion(version), GetOSName()), destinationPath);
        for(String url : ManifestParser.GetLibsFromVersion(ManifestParser.GetManifestFromVersion(version), GetOSName()))
        {
            String dest = destinationPath + File.separator + url.split("/")[url.split("/").length - 1];
            System.out.println("Downloading: " + url);
            int response;
            if((response = DownloadURL(url, dest)) == 200)
            {
                System.out.println("Saved to: " + dest);
            } else { System.out.println("HTTP status code " + response); }
        }
    }

    public static void DownloadResourcesFromAssetIndex(String version, String destinationPath)
    {
        JsonObject versionManifest = ManifestParser.GetManifestFromVersion(version);
        JsonObject assetIndex = ManifestParser.GetAssetIndexFromVersion(versionManifest);
        String indexPath = "assets/indexes/" + ManifestParser.GetAssetIndexVersionFromVersion(versionManifest) + ".json";
        ManifestParser.SaveJsonObject(assetIndex, indexPath);
        Pair<String, String>[] assets = ManifestParser.GetAssetsFromAssetIndex(assetIndex);

        System.out.println("Saved asset index at " + indexPath);
        System.out.println("Need to get " + assets.length + " assets");

        for(Pair<String, String> asset : assets)
        {
            String url = "https://resources.download.minecraft.net/" + asset.getValue().substring(0, 2) + "/" + asset.getValue();
            String dest = destinationPath + "/" + asset.getValue().substring(0, 2) + "/" + asset.getValue();
            System.out.println("Downloading: " + url);
            int response;
            if((response = DownloadURL(url, dest)) == 200)
            {
                System.out.println("Saved to: " + dest);
            } else { System.out.println("HTTP status code " + response); }
        }
    }
}
