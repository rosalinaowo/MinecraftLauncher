public class Config
{
    public int memoryAmount;
    public String customJavaExePath;
    public String username;
    public int[] pos;
    public Config(int memoryAmount, String customJavaExePath, String username, int[] pos)
    {
        this.memoryAmount = memoryAmount;
        this.customJavaExePath = customJavaExePath;
        this.username = username;
        this.pos = pos;
    }
}
