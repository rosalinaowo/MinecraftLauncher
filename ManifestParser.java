import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.google.gson.*;
import javafx.util.Pair;

public class ManifestParser
{
    final static String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static JsonObject GetJsonObjectFromURL(String url)
    {
        StringBuilder json = new StringBuilder();
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String line;
            while((line = reader.readLine()) != null) { json.append(line); }
        } catch (IOException e) { e.printStackTrace(); }

        return new Gson().fromJson(json.toString(), JsonObject.class);
    }

    private static JsonObject GetJsonObjectFromFile(String path)
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

    public static void SaveJsonObject(JsonObject obj, String path)
    {
        Gson g = new GsonBuilder().setPrettyPrinting().create();
        String json = g.toJson(obj);
        try
        {
            File destinationFile = new File(path);
            if(!destinationFile.getParentFile().exists())
            {
                destinationFile.getParentFile().mkdirs();
            }
            FileWriter fw = new FileWriter(path);
            fw.write(json);
            fw.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static JsonObject GetVersionManifest()
    {
        return GetJsonObjectFromURL(VERSION_MANIFEST_URL);
    }

    public static JsonObject GetManifestFromVersion(String version)
    {
        JsonObject mainManifest = GetVersionManifest();

        for(JsonElement vm : mainManifest.getAsJsonArray("versions"))
        {
            JsonObject versionManifest = vm.getAsJsonObject();
            try
            {
                if(Objects.equals(versionManifest.get("id").getAsString(), version))
                {
                    return GetJsonObjectFromURL(versionManifest.get("url").getAsString());
                }
            } catch (NullPointerException e) { e.printStackTrace(); }
        }

        return null;
    }

    public static JsonObject GetAssetIndexFromVersion(JsonObject manifest)
    {
        return GetJsonObjectFromURL(manifest.getAsJsonObject("assetIndex").get("url").getAsString());
    }

    public static String GetAssetIndexVersionFromVersion(JsonObject manifest)
    {
        return manifest.getAsJsonObject("assetIndex").get("id").getAsString();
    }

    public static String GetClientURLFromVersion(JsonObject manifest)
    {
        return manifest.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
    }

    public static String GetServerURLFromVersion(JsonObject manifest)
    {
        return manifest.getAsJsonObject("downloads").getAsJsonObject("server").get("url").getAsString();
    }

    public static String[] GetLibsFromVersion(JsonObject manifest, String platform)
    {
        List<String> libs = new ArrayList<>(); // we need to save the names of the libs we download, since
                                               // in the manifest multiple versions are provided
        List<String> urls = new ArrayList<>();
        Gson parser = new GsonBuilder().setPrettyPrinting().create();

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
                    url = downloads.getAsJsonObject("classifiers").getAsJsonObject("natives-" + platform).get("url").getAsString();
                }
            } catch (NullPointerException e) {  }

            if(url != null)
            {
                if(!libs.contains(name))
                {
                    //fileName = url.split("/")[url.split("/").length - 1];
                    libs.add(name);
                    urls.add(url);
                }
            }
        }
        return urls.toArray(new String[0]);
    }

    public static Pair<String, String>[] GetAssetsFromAssetIndex(JsonObject manifest)
    {
        List<Pair<String, String>> assets = new ArrayList<>();
        JsonObject objs = manifest.getAsJsonObject("objects");
        for(Map.Entry<String, JsonElement> entry : objs.entrySet())
        {
            String dest = entry.getKey();
            String hash = entry.getValue().getAsJsonObject().get("hash").getAsString();
            assets.add(new Pair<>(dest, hash));
        }

        return assets.toArray(new Pair[0]);
    }
}
