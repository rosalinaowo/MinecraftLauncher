import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Launcher 
{
    public static  final String USER_DIR = System.getProperty("user.dir") + File.separator;
    public static final String VERSIONS_DIR = "versions" + File.separator;
    public static final String ASSETS_DIR = "assets" + File.separator;
    public static final String ASSETS_INDEXES_DIR = ASSETS_DIR + "indexes" + File.separator;
    public static final String RESOURCES_DIR = "resources" + File.separator;
    public static final String LIBRARIES_DIR = "libraries" + File.separator;
    public static final String DEFAULT_MC_PATH = System.getenv("APPDATA") + File.separator + ".minecraft" + File.separator;   // This is necessary for older versions, since they
                                                                                                                                    // require their assets to be under appdata/.minecraft
    public static final String RUNTIME_PATH = "runtime" + File.separator;
    public static void main(String[] args)
    {
        final String customJavaOption = "--java";
        final String memoryOption = "--memory";
        final String dryRunOption = "--dry-run";
        final String helpMenu = "Usage: Launcher [options] [version] [username]\n" +
                                "Options:\n  " +
                                customJavaOption + " [java.exe]: set a custom Java executable\n  " +
                                memoryOption + " [number of MBs]: set the game max memory\n  " +
                                dryRunOption + ": output only the launch command";
        String customJavaExePath = "";
        int memoryAmount = 1024;
        boolean isDrRun = false;
        String version = null, username = null;
        if(args.length < 2)
        {
            System.out.println(helpMenu);
            System.exit(0);
        }

        for(int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if(arg.startsWith("--"))
            {
                switch(arg)
                {
                    case "--help": System.out.println(helpMenu); System.exit(0);
                    case customJavaOption: customJavaExePath = args[++i]; break;
                    case memoryOption: memoryAmount = Integer.parseInt(args[++i]); break;
                    case dryRunOption: isDrRun = true; break;
                    default: System.out.println("Invalid flag. Exiting."); System.exit(-1);
                }
            }
            else
            {
                if(version == null)
                {
                    version = arg;
                } else { username = arg; }
            }
        }

        Launch(version, username, memoryAmount, isDrRun, customJavaExePath);
    }

    public static void Launch(String version, String username, int memoryAmountMB, boolean isDryRun, String customJavaExe)
    {
        JsonObject manifest = ManifestParser.GetJsonObjectFromFile(VERSIONS_DIR + version + File.separator + version + ".json");
        JsonObject assetsManifest = ManifestParser.GetLocalAssetIndexFromVersion(manifest);
        String nativesPath = VERSIONS_DIR + version + File.separator + "natives";
        String clientJar = VERSIONS_DIR + version + File.separator + version + ".jar";
        String assetIndex = manifest.getAsJsonObject("assetIndex").get("id").getAsString();
        String startingClass = manifest.get("mainClass").getAsString();
        boolean pre16 = assetIndex.equals("pre-1.6");
        //String startingClass = !assetIndex.equals("pre-1.6") ? manifest.get("mainClass").getAsString() : "net.minecraft.client.Minecraft";
        String assetsDir = "\"" + ManifestParser.GetAssetsDirFromAssetIndex(assetsManifest, "").replaceAll("objects" + Downloader.separator, "") + "\"";
        Map<String, String> paramValues = new HashMap<String, String>() {{ // All parameters we could need
            put("auth_player_name", username); // --username
            put("version_name", version); // --version
            put("game_directory", pre16 ? "\"" + DEFAULT_MC_PATH + "\"" : "\"" + System.getProperty("user.dir") + "\""); // --gameDir
            put("assets_root", assetsDir);  // --assetsDir
            put("game_assets", assetsDir); // --assetsDir for some classic versions
            put("assets_index_name", assetIndex); // --assetIndex
            put("auth_uuid", UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString().replaceAll("-", ""));
            put("auth_access_token", "69420");
            put("clientid", "\"\"");
            put("auth_xuid", "\"\"");
            put("user_properties", "{}");
            put("user_type", "legacy");
            put("version_type", manifest.get("type").getAsString());
        }};
        // Populate list of needed libraries
        String libsPaths = "";
        String divider = System.getProperty("os.name").toLowerCase().contains("windows") ? ";" : ":";
        for(Pair<String, String> lib : ManifestParser.GetLibsFromVersion(manifest, Downloader.GetOSName()))
        {
            String path = LIBRARIES_DIR + lib.getKey();
            libsPaths += path + lib.getValue().split("/")[lib.getValue().split("/").length - 1] + divider;
        }

        List<String> cmd = new ArrayList<>();
        if(Objects.equals(customJavaExe, ""))
        {
            cmd.add(ManifestParser.GetRuntimePathFromVersion(manifest).getAbsolutePath());
        } else { cmd.add(customJavaExe); }
        if(Objects.equals(System.getProperty("os.name"), "Windows 11"))
        {
            cmd.add("-Dos.name=\"Windows 10\"");
            cmd.add("-Dos.version=10.0");
        }
        cmd.add("-Djava.library.path=\"" + nativesPath + "\"");
        cmd.add("-Dminecraft.client.jar=\"" + clientJar + "\"");
        cmd.add("-cp");
        cmd.add("\"" + clientJar + divider + libsPaths + "\"");
        cmd.add("-Xmx" + memoryAmountMB + "M");
        // Garbage collector flags, provides better performance
        cmd.add("-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M -Xss1M");
        cmd.add(startingClass);
        cmd.add(ManifestParser.GetLaunchParams(manifest, paramValues));

        String cmdString = String.join(" ", cmd);
        cmd = Arrays.asList(cmdString.replaceAll(Downloader.separator, "/").split(" "));

        System.out.println(String.join(" ", cmd));
        if(isDryRun) { System.exit(0); }
        try
        {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // Redirect the output of the process
            pb.directory(new File("."));
            System.out.println("Starting Minecraft " + version + "...");
            Process mcProc = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(mcProc.getInputStream()));
            String line;
            while((line = reader.readLine()) != null) { System.out.println(line); } // and print it
            int exitCode = mcProc.waitFor();

            System.out.println("Minecraft exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) { e.printStackTrace(); }
    }
}