import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.google.gson.*;
import org.apache.commons.text.StringSubstitutor;

public class ManifestParser
{
    final static String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    /**
     *
     * @param url   The URL to the JSON file
     * @return      The JsonObject for the URL specified
     */
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

    /**
     *
     * @param path  The path to the JSON file
     * @return      The JsonObject for the file specified
     */
    public static JsonObject GetJsonObjectFromFile(String path)
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

    /**
     *
     * @param obj   The JsonObject to save
     * @param path  The path to the saved file
     */
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

    /**
     *
     * @return  The main version manifest
     */
    public static JsonObject GetVersionManifest() { return GetJsonObjectFromURL(VERSION_MANIFEST_URL); }

    /**
     *
     * @return  URLs of downloadable
     */
    public static String[] GetDownloadableVersions()
    {
        List<String> versions = new ArrayList<>();
        for(JsonElement vers : GetVersionManifest().getAsJsonArray("versions"))
        {
            versions.add(vers.getAsJsonObject().get("id").getAsString());
        }

        return versions.toArray(new String[0]);
    }

    /**
     *
     * @param version   The minecraft version
     * @return          The version's manifest
     */
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

    /**
     *
     * @param manifest  The version's manifest
     * @return          The version's asset index
     */
    public static JsonObject GetAssetIndexFromVersion(JsonObject manifest)
    {
        return GetJsonObjectFromURL(manifest.getAsJsonObject("assetIndex").get("url").getAsString());
    }

    public static JsonObject GetLocalAssetIndexFromVersion(JsonObject manifest)
    {
        return ManifestParser.GetJsonObjectFromFile(Launcher.ASSETS_INDEXES_DIR + ManifestParser.GetAssetIndexVersionFromVersion(manifest) + ".json");
    }

    /**
     *
     * @param assetManifest The version's asset manifest
     * @return              [isMapToResources, isVirtual]
     */
    public static boolean[] GetSpecialAssetsFlags(JsonObject assetManifest)
    {
        boolean mapToResources = false;
        boolean virtual = false;
        try
        {
            mapToResources = assetManifest.get("map_to_resources").getAsBoolean();
        } catch (NullPointerException e) { }
        try
        {
            virtual = assetManifest.get("virtual").getAsBoolean();
        } catch (NullPointerException e) { }
        return new boolean[] {mapToResources, virtual};
    }

    public static String GetAssetsDirFromAssetIndex(JsonObject assetManifest, String destinationPath)
    {
        boolean mapToResources = GetSpecialAssetsFlags(assetManifest)[0], virtual = GetSpecialAssetsFlags(assetManifest)[1];
        destinationPath += Launcher.ASSETS_DIR;
        if(mapToResources)
        {
            destinationPath = Launcher.DEFAULT_MC_PATH + Launcher.RESOURCES_DIR;
        } else if(virtual)
        {
            destinationPath += "virtual" + File.separator + "legacy" + File.separator;
        } else { destinationPath += "objects" + File.separator; }
        return destinationPath;
    }

    public static String GetAssetRelativePathFromAssetPair(Pair<String, String> asset, boolean isMapToResources, boolean isVirtual)
    {
        return isMapToResources || isVirtual ? asset.getKey() : asset.getValue().substring(0, 2) + File.separator + asset.getValue();
    }

    /**
     *
     * @param manifest  The version's manifest
     * @return          Runtime path and version
     */
    public static Pair<String, Integer> GetJavaRuntimeFromVersion(JsonObject manifest)
    {
        try
        {
            JsonObject j = manifest.getAsJsonObject("javaVersion");
            return new Pair<>(j.get("component").getAsString(), j.get("majorVersion").getAsInt());
        } catch (Exception e)
        {
            System.out.println("Couldn't parse Java version from manifest, assuming Java 8. Stack trace:");
            e.printStackTrace();
            return new Pair<>("jre-legacy", 8);
        }
    }

    public static File GetRuntimePathFromVersion(JsonObject manifest)
    {
        String executable = System.getProperty("os.name").toLowerCase().contains("windows") ? "java.exe" : "java";
        return new File(Launcher.RUNTIME_PATH + GetJavaRuntimeFromVersion(manifest).getKey() + File.separator + "bin" + File.separator + executable);
    }

    /**
     *
     * @param manifest  The version manifest
     * @return          The assets version
     */
    public static String GetAssetIndexVersionFromVersion(JsonObject manifest)
    {
        return manifest.getAsJsonObject("assetIndex").get("id").getAsString();
    }

    public static String GetGameVersionFromVersion(JsonObject manifest)
    {
        return manifest.get("id").getAsString();
    }

    public static String GetClientJarURLFromVersion(JsonObject manifest)
    {
        return manifest.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
    }

    public static String GetServerJarURLFromVersion(JsonObject manifest)
    {
        try
        {
            return manifest.getAsJsonObject("downloads").getAsJsonObject("server").get("url").getAsString();
        } catch (NullPointerException e) { return null; }
    }

    /**
     *
     * @param manifest  The version's manifest
     * @param platform  The OS platform ("windows", "linux", "osx")
     * @return          Pairs of library path and URL
     */
    public static Pair<String, String>[] GetLibsFromVersion(JsonObject manifest, String platform)
    {
        JsonArray libsArray = manifest.getAsJsonArray("libraries");
        List<String> libs = new ArrayList<>(); // we need to save the names of the libs we download, since
                                               // in the manifest multiple versions are provided
        List<String> urls = new ArrayList<>();
        List<Pair<String, String>> libraries = new ArrayList<>();

        for(int i = 0; i < libsArray.size(); i++)
        {
            JsonObject library = libsArray.get(i).getAsJsonObject();
            String libPath = null;
            String name = null;
            String url = null;
            try
            {
                url = name = libPath = null;
                name = library.get("name").getAsString().split(":")[1];
                JsonObject downloads = library.getAsJsonObject("downloads");
                List<JsonObject> artifacts = new ArrayList<>();
                JsonObject artifact = null;
                if((artifact = downloads.getAsJsonObject("artifact")) != null) // Check if lib is universal
                {
                    //artifact = downloads.getAsJsonObject("artifact");
                    artifacts.add(artifact);
                    //url = artifact.get("url").getAsString();
                    //libPath = artifact.get("path").getAsString();
                }
                if((artifact = downloads.getAsJsonObject("classifiers")) != null) // if it isn't, check if it is a native
                {
                    //artifact = downloads.getAsJsonObject("classifiers").getAsJsonObject("natives-" + platform);
                    JsonObject a = artifact.getAsJsonObject("natives-" + platform);
                    if(a == null)
                    {
                        artifacts.add(artifact.getAsJsonObject("natives-" + platform + "-" +
                                                             (System.getProperty("os.arch").contains("86") ? "32" : "64")));
                    } else { artifacts.add(a); }
                    //url = nativeLib.get("url").getAsString();
                    //libPath = nativeLib.get("path").getAsString();
                }

                for(JsonObject a : artifacts)
                {
                    url = a.get("url").getAsString();
                    String filePath = a.get("path").getAsString();
                    int lastDirIdx = filePath.lastIndexOf("/");
                    libPath = filePath.substring(0, lastDirIdx + 1);

                    if(url != null && libPath != null)
                    {
                        //if(!libs.contains(name))
                        //{
                            //fileName = url.split("/")[url.split("/").length - 1];
                            libs.add(name);
                            urls.add(url);
                            libraries.add(new Pair<>(libPath, url));
                        //}
                    }
                }
            } catch (NullPointerException e) {  }
        }
        //return urls.toArray(new String[0]);
        return libraries.toArray(new Pair[0]);
    }

    /**
     *
     * @param manifest  The version's manifest
     * @return          Pairs of fileName and hash
     */
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

    public static String GetLaunchParams(JsonObject manifest, Map<String, String> values)
    {
        String params = "";
        if(manifest.get("minecraftArguments") != null)
        {
            params = manifest.get("minecraftArguments").getAsString();
        } else
        {
            JsonArray p = manifest.getAsJsonObject("arguments").getAsJsonArray("game");
            for(JsonElement element : p)
            {
                if(element.isJsonPrimitive()) { params += element.getAsString() + " "; }
            }
        }

        StringSubstitutor substitutor = new StringSubstitutor(values);
        return substitutor.replace(params);
    }
}
