# Minecraft Launcher
A Java 8 compatible custom Minecraft launcher, only supports offline accounts (for now).

## Usage
Download Minecraft:
```bash
java -jar Downloader.jar [version] [destination path]
```
eg. ```java -jar Downloader.jar 1.8.9 .```

Launch the game:
```
java -jar Launcher.jar [version] [username]
```
eg. ```java -jar Launcher.jar 1.8.9 pino```

## Manifests
* [Versions manifest](#versions-manifest)
* [Version]()
* [Assets Index]()

### [Versions manifest](https://launchermeta.mojang.com/mc/game/version_manifest.json)
This file contains a list of all Minecraft versions JSONs URLs
### [Version](https://piston-meta.mojang.com/v1/packages/d546f1707a3f2b7d034eece5ea2e311eda875787/1.8.9.json)
This file contains the URLs for the **assets index**, **client.jar**, **server.jar**, and the **libraries** necessary to the game.
Some libraries are OS specific, since they provide natives (DLLs, SOs, DYLIBs), when they are their JSON differs from the usual one, eg.
#### Universal library
```
{
	"downloads": {
		"artifact": {
			"path": "com/mojang/netty/1.8.8/netty-1.8.8.jar",
			"sha1": "0a796914d1c8a55b4da9f4a8856dd9623375d8bb",
			"size": 15966,
			"url": "https://libraries.minecraft.net/com/mojang/netty/1.8.8/netty-1.8.8.jar"
		}
	},
	"name": "com.mojang:netty:1.8.8"
}
```
#### OS specific
```
{
      "downloads": {
        "artifact": {
          "path": "org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209.jar",
          "sha1": "b04f3ee8f5e43fa3b162981b50bb72fe1acabb33",
          "size": 22,
          "url": "https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209.jar"
        },
        "classifiers": {
          "natives-linux": {
            "path": "org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-linux.jar",
            "sha1": "931074f46c795d2f7b30ed6395df5715cfd7675b",
            "size": 578680,
            "url": "https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-linux.jar"
          },
          "natives-osx": {
            "path": "org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-osx.jar",
            "sha1": "bcab850f8f487c3f4c4dbabde778bb82bd1a40ed",
            "size": 426822,
            "url": "https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-osx.jar"
          },
          "natives-windows": {
            "path": "org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-windows.jar",
            "sha1": "b84d5102b9dbfabfeb5e43c7e2828d98a7fc80e0",
            "size": 613748,
            "url": "https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-windows.jar"
          }
        }
      },
      "extract": {
        "exclude": [
          "META-INF/"
        ]
      },
      "name": "org.lwjgl.lwjgl:lwjgl-platform:2.9.4-nightly-20150209",
      "natives": {
        "linux": "natives-linux",
        "osx": "natives-osx",
        "windows": "natives-windows"
      },
      "rules": [
        {
          "action": "allow"
        },
        {
          "action": "disallow",
          "os": {
            "name": "osx"
          }
        }
      ]
    },
```
It is so important to download the correct one for our OS and architecture (if needed), since these can be also architecture dependent, eg.
```
{
      "downloads": {
        "classifiers": {
          ...
          "natives-windows-32": {
            "path": "tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-32.jar",
            "sha1": "206c4ccaecdbcfd2a1631150c69a97bbc9c20c11",
            "size": 474225,
            "url": "https://libraries.minecraft.net/tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-32.jar"
          },
          "natives-windows-64": {
            "path": "tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-64.jar",
            "sha1": "9fdd0fd5aed0817063dcf95b69349a171f447ebd",
            "size": 580098,
            "url": "https://libraries.minecraft.net/tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-64.jar"
          }
        }
      }
}
```