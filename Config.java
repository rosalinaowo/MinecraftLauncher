public class Config
{
    public int memoryAmount;
    public String customJavaExePath;
    public String username;
    public Config(int memoryAmount, String customJavaExePath, String username)
    {
        this.memoryAmount = memoryAmount;
        this.customJavaExePath = customJavaExePath;
        this.username = username;
    }
}
