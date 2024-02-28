# Minecraft Launcher
A Java 8 compatible custom Minecraft launcher, only supports offline accounts (for now).

## Usage
Download [latest build](https://github.com/rosalinaowo/MinecraftLauncher/releases)'s LauncherGUI.jar or use the [standalone jars](#standalone-jars)

### Standalone jars
Download Minecraft:
```bash
java -jar Downloader.jar [options] [version] [destination path]
```
e.g. ```java -jar Downloader.jar 1.8.9 .```

Launch the game:
```bash
java -jar Launcher.jar [options] [version] [username]
```
e.g. ```java -jar Launcher.jar 1.8.9 pino```

## Building
Install your desired JDK version and clone this repository, run the build script (NOTE: set your Java binaries path in build.bat).