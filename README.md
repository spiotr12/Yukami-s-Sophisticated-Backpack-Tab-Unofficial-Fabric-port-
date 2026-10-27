# Yukami's Sophisticated Backpack Tab - Fabric port (Minecraft 1.21.1)

Unofficial Fabric port of [Yukami's Sophisticated Backpack Tab](https://github.com/domanhthang2110/yukamibackpacktab/tree/1.21.1) (NeoForge original).
It is a **client-only** mod that adds tabs to inventory/container screens for switching to your equipped Sophisticated Backpack.

Press **F9** in a container screen to open the tab offset editor.

## Requirements

- JDK 21 (e.g. `C:\Programs\Java\jdk-21.0.12.1+1`)
- Internet access on first build (Gradle downloads Minecraft, Fabric and dependencies)

Runtime mods (all for Minecraft 1.21.1, Fabric):
- Fabric Loader 0.16.9+ and Fabric API
- [Sophisticated Backpacks (Unofficial Fabric port)](https://modrinth.com/mod/sophisticated-backpacks-unoffical-fabric-port)
- [Sophisticated Core (Unofficial Fabric port)](https://modrinth.com/mod/sophisticated-core-unofficial-fabric-port)

## Build

PowerShell, from the project root:

```powershell
$env:JAVA_HOME = 'C:\Programs\Java\jdk-21.0.12.1+1'
.\gradlew.bat build
```

The jar is written to `build/libs/yukamibackpacktab-1.21.1-2.1.1.jar` (ignore the `-sources` jar).
Copy it into the `mods` folder of a Fabric 1.21.1 instance, together with Fabric API, Sophisticated Core and Sophisticated Backpacks.
(Sophisticated Core also needs Forge Config API Port and Team Reborn Energy; the Modrinth downloads of the Fabric ports include them or list them as dependencies.)

## Run in development

```powershell
$env:JAVA_HOME = 'C:\Programs\Java\jdk-21.0.12.1+1'
.\gradlew.bat runClient
```

This starts a Minecraft 1.21.1 client with all dependencies already on the classpath. The game directory is `run/`.
In IntelliJ IDEA you can instead run the generated "Minecraft Client" run configuration after importing the Gradle project (set the Gradle JVM to JDK 21).

## Tests

```powershell
.\gradlew.bat test
```

## Notes

- Config: `config/yukamibackpacktab-client.json` (tab position, blacklisted screens) and `config/yukamibackpacktab/screen_offsets.json`.
- Differences from the NeoForge version: events use Fabric API callbacks plus one mixin (`AbstractContainerScreenMixin`) for rendering tabs; the config is JSON instead of TOML.
- Dependency versions are in `gradle.properties`.
- Gradle 8.12 and Loom 1.10 are used because newer Loom needs JDK 25.

## License

GPL-3.0-only. See `LICENSE`.