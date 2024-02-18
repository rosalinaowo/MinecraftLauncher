import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

public class ManifestDownloader
{
    static final boolean isWindows = true;
    public static void main(String[] args)
    {
        if(args.length < 3)
        {
            System.out.println("Usage: ManifestDownloader <manifest> <downloadDir> <Dry run? 1/0)");
            System.exit(-1);
        }
        String path = args[0];
        String downloadDir = args[1];
        boolean dryRun = Integer.parseInt(args[2]) > 0;
        JsonObject manifest = ReadManifest(path);

        if(manifest != null)
        {
            ParseManifest(manifest, downloadDir, dryRun);
        }
    }

    private static JsonObject ReadManifest(String path)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (FileNotFoundException e)
        {
            System.out.println("File not found: " + path);
            e.printStackTrace();
        }
        return null;
    }

    private static void ParseManifest(JsonObject manifest, String downloadDir, boolean dryRun)
    {
        List<String> libs = new ArrayList<>(); // we need to save the names of the libs we download, since
                                               // in the manifest multiple versions are provided
        List<String> urls = new ArrayList<>();
        Gson parser = new GsonBuilder().setPrettyPrinting().create();

        if(dryRun) { System.out.println("Libraries detected:"); }
        for(JsonElement lib : manifest.getAsJsonArray("libraries"))
        {
            JsonObject library = lib.getAsJsonObject();
            String fileName = null;
            String name = null;
            String url = null;
            try
            {
                url = name = fileName = null;
                name = library.get("name").getAsString().split(":")[1];
                JsonObject downloads = library.getAsJsonObject("downloads");
                if(downloads.getAsJsonObject("artifact") != null)
                {
                    url = downloads.getAsJsonObject("artifact").get("url").getAsString();
                }
                else if(downloads.getAsJsonObject("classifiers") != null)
                {
                    if(isWindows)
                    {
                        url = downloads.getAsJsonObject("classifiers").getAsJsonObject("natives-windows").get("url").getAsString();
                    }
                    else
                    {
                        url = downloads.getAsJsonObject("classifiers").getAsJsonObject("natives-linux").get("url").getAsString();
                    }
                }
            } catch (NullPointerException e) {  }

            if(url != null)
            {
                if(!libs.contains(name))
                {
                    fileName = url.split("/")[url.split("/").length - 1];
                    libs.add(name);
                    urls.add(url);
                    if(!dryRun)
                    {
                        File dir = new File(downloadDir + File.separator);
                        if(!dir.exists()) { dir.mkdirs(); }
                        System.out.println("Downloading " + fileName);
                        DownloadLib(url, downloadDir + File.separator + fileName);
                    } else { System.out.println(fileName + "," + url); }
                }
            }
        }
    }

    private static void DownloadLib(String libUrl, String dest)
    {
        try
        {
            URL url = new URL(libUrl);
            HttpURLConnection req = (HttpURLConnection) url.openConnection();
            req.setRequestMethod("GET");
            if(req.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                InputStream istream = req.getInputStream();
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
