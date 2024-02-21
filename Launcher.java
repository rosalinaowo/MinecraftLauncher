import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public static void main(String[] args)
    {
        if(args.length < 2)
        {
            System.out.println("Usage: Launcher <version> <username>");
            System.exit(-1);
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        LaunchCustom(args[0], args[1]);
    }

    public static void Launch(String version, String username)
    {
        final String StartingClass = "net.minecraft.client.Minecraft";
        final String StartingClassPost16 = "net.minecraft.client.main.Main";
        int versionNumber = 0;
        boolean post16;
        try
        {
            versionNumber = Integer.parseInt(String.format("%-3s", version.replace(".", "")).replace(' ', '0')); // 1.6.4 -> 164, 1.7 -> 170
            post16 = version.split("\\.")[1].length() >= 2 || versionNumber >= 160;
        } catch (NumberFormatException e) { post16 = false; }


        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("\"-Djava.library.path=versions/" + version + "/natives\"");
        cmd.add("-cp");
        cmd.add("\"versions/" + version + "/client.jar;" + LIBRARIES_DIR.replaceAll("\\\\", "") + "/" + (post16 ? version : "pre1.6") + "/*\"");
        cmd.add(post16 ? StartingClassPost16 : StartingClass);
        if(post16) // If we are post 1.6 we need to specify --version and --username
        {
            cmd.add("--version");
            cmd.add(version);
            cmd.add("--username");
        }
        cmd.add(username);
        cmd.add("--gameDir");
        cmd.add("\"versions/" + version + "/.minecraft\"");
        if(post16) // as well as --asset{sDir, Index}, --accessToken and --userProperties (which we can leave blank)
        {
            cmd.add("--assetsDir");
            cmd.add("assets/" + (versionNumber >= 170 ? "" : "virtual/legacy"));
            cmd.add("--assetIndex");
            if(versionNumber >= 180 && versionNumber <= 189) { cmd.add("1.8"); } else { cmd.add(version); }
            //cmd.add(version);
            cmd.add("--accessToken");
            cmd.add("1234");
            cmd.add("--userProperties");
            cmd.add("\"{}\"");
        }

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

    public static void LaunchCustom(String version, String username)
    {
        JsonObject manifest = ManifestParser.GetJsonObjectFromFile(VERSIONS_DIR + version + File.separator + version + ".json");
        String nativesPath = VERSIONS_DIR + version + File.separator + "natives";
        String clientJar = VERSIONS_DIR + version + File.separator + "client.jar";
        String assetIndex = manifest.getAsJsonObject("assetIndex").get("id").getAsString();
        String startingClass = manifest.get("mainClass").getAsString();
        boolean pre16 = assetIndex.equals("pre-1.6");
        //String startingClass = !assetIndex.equals("pre-1.6") ? manifest.get("mainClass").getAsString() : "net.minecraft.client.Minecraft";
        // Populate list of needed libraries
        String libsPaths = "";
        for(Pair<String, String> lib : ManifestParser.GetLibsFromVersion(manifest, Downloader.GetOSName()))
        {
            String path = LIBRARIES_DIR + lib.getKey().replaceAll("/", "\\\\");
            libsPaths += path + lib.getValue().split("/")[lib.getValue().split("/").length - 1] + ";";
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        if(pre16)
        {
            cmd.add("\"-Dos.name=Windows 10\"");
            cmd.add("-Dos.version=10.0");
        }
        cmd.add("\"-Djava.library.path=" + nativesPath + "\"");
        cmd.add("-cp");
        cmd.add("\"" + clientJar + ";" + libsPaths + "\"");
        cmd.add(startingClass);
        if(!pre16)
        {
            cmd.add("--version");
            cmd.add(version);
            cmd.add("--username");
        }
        cmd.add(username);
        cmd.add("--gameDir");
        if(pre16)
        {
            cmd.add("\"" + DEFAULT_MC_PATH + "\""); // we need to use the default .minecraft path for older versions
        } else { cmd.add("\"" + System.getProperty("user.dir") + File.separator + ".minecraft\""); }
        cmd.add("--assetsDir");
        if(pre16)
        {
            cmd.add("\"" + DEFAULT_MC_PATH + RESOURCES_DIR + "\"");
        } else
        {
            cmd.add("\"" + ASSETS_DIR + "\"");
            cmd.add("--assetIndex");
            cmd.add(assetIndex);
        }
//        cmd.add("--assetIndex");
//        cmd.add(assetIndex);
        if(!pre16)
        {
//            cmd.add("--assetIndex");
//            cmd.add(assetIndex);
            cmd.add("--accessToken");
            cmd.add("1234");
            cmd.add("--userProperties");
            cmd.add("\"{}\"");
        }
        String cmdString = String.join(" ", cmd);
        String separator = File.separator.equals("\\") ? "\\\\" : "/";
        cmd = Arrays.asList(cmdString.replaceAll(separator, "/").split(" "));

        System.out.println(String.join(" ", cmd));
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