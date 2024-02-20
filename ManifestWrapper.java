import com.google.gson.JsonObject;

public class ManifestWrapper
{
    private JsonObject versionManifest, assetsManifest;

    public ManifestWrapper(JsonObject versionManifest, JsonObject assetsManifest)
    {
        this.versionManifest = versionManifest;
        this.assetsManifest = assetsManifest;
    }

    public JsonObject GetVersionManifest() { return versionManifest; }
    public JsonObject GetAssetsManifest() { return  assetsManifest; }
}