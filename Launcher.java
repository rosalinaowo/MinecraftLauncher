import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Launcher 
{
    public static void main(String[] args)
    {
        if(args.length < 2)
        {
            System.out.println("Usage: Launcher <version> <username>");
            System.exit(-1);
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Launch(args[0], args[1]);
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
        cmd.add("\"versions/" + version + "/client.jar;libs/" + (post16 ? version : "pre1.6") + "/*\"");
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
            //for(int i = 0; i < cmd.size(); i++) { System.out.println(cmd.get(i) + " "); }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // Redirect the output of the process
            pb.directory(new File("."));
            Process mcProc = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(mcProc.getInputStream()));
            String line;
            while((line = reader.readLine()) != null) { System.out.println(line); } // and print it
            int exitCode = mcProc.waitFor();

            System.out.println("Minecraft exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) { e.printStackTrace(); }
    }
}