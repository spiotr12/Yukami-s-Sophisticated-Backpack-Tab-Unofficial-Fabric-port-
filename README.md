# Yukami's Sophisticated Backpack Tab - Fabric port (Minecraft 1.21.1)

**ALL THE CREDITS GO TO [Yukami](https://github.com/domanhthang2110). I just port it to fabric using AI.** 

Unofficial Fabric port of [Yukami's Sophisticated Backpack Tab](https://github.com/domanhthang2110/yukamibackpacktab/tree/1.21.1) (NeoForge original).
It is a **client-only** mod that adds tabs to inventory/container screens for switching to your equipped Sophisticated Backpack.

Press **F9** in a container screen to open the tab offset editor.

The editor also selects the **Backpack tabs** mode:
- *Single worn backpack*: one tab for the worn backpack.
- *Any backpack*: a tab for every backpack you carry.
- *Inception upgrade only*: the worn backpack always has a tab. A backpack opened from the inventory gets a tab until you go back to the inventory. While a backpack with an Inception Upgrade is open, tabs for the backpacks inside it appear after it, tinted with the main color of the backpack they are inside, so you can see which tab they belong to. Going back to a parent closes the tabs below it.
- *Any backpack with inception*: like *Inception upgrade only*, and additionally every carried backpack with an Inception Upgrade has a tab (worn backpacks first). The client only learns a backpack's upgrades once it has its contents, so a carried backpack may only get its tab after you opened it once.

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