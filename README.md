# ChestSafe
Chest protection plugin for Bukkit/Spigot.

## Feature
- UUID Ready
- Command tab complete
- Compatibility with legacy commands(e.g. /cprivate)
- Easy to use
- ActionBar message
- API available

# Installation
1. [Download this plugin.](https://github.com/HimaJyun/ChestSafe/releases/latest)
2. drop in plugins directory.
3. Server start.
4. config edit.
5. reload.

## Import from other plugin.
Please use [ChestSafe-Converter](https://github.com/HimaJyun/Jecon/ChestSafe-Converter).

# Command/Permission
## Commands
|Command|Description|Permission|Default|
|:------|:----------|:----------|:-----|
|/chestsafe private|Create private protection.|chestsafe.create.private|ALL|
|/chestsafe public|Create public protection.|chestsafe.create.public|ALL|
|/chestsafe flag|Specify a flag for protection.|chestsafe.flag.*|ALL|
|/chestsafe remove|Remove protection.|chestsafe.remove|ALL|
|/chestsafe info|Display protection information.|chestsafe.info|ALL|
|/chestsafe member|Add/Remove protection members.|chestsafe.member|ALL|
|/chestsafe transfer|Change protection owner.|chestsafe.transfer|ALL|
|chestsafe persist|Perform operations permanently.|chestsafe.persist|ALL|
|/chestsafe cleanup|Delete protection of nonexistent blocks.|chestsafe.cleanup|OP|
|/chestsafe reload|Reload config.|chestsafe.reload|OP|
|/chestsafe version|Show version and check new version|chestsafe.version|OP|
|/chestsafe help|Show helps.|N/A|ALL|

## Flags
|Flag|Description|Permission|
|:---|:----------|:---------|
|hopper|Hopper will be available|chestsafe.flag.hopper|
|explosion|It will be destroyed by an explosion|chestsafe.flag.explosion|
|fire|It will burn down|chestsafe.flag.fire|
|redstone|Redstone will be available|chestsafe.flag.redstone|
|mob|Destruction by MOB becomes possible|chestsafe.flag.mob|

## Other permissions.
|Permission|Description|Default|
|:---------|:----------|:------|
|chestsafe.notice|Protection information will be displayed automatically|ALL|
|chestsafe.passthrough|Can operate a chest that is not yours.|OP|

## Alias

|Command|Alias|
|:------|:----|
|/c|/chestsafe|
|/lock|/chestsafe private|
|/unlock|/chestsafe remove|
|/cprivate|/chestsafe private|
|/cpublic|/chestsafe public|
|/cremove|/chestsafe remove|
|/cinfo|/chestsafe info|
|/cmodify|/chestsafe member modify|
|/cpersist|/chestsafe persist|
|/chopper|/chestsafe flag hopper|
|/callowexplosions|/chestsafe flag explosion|
|/ctnt|/chestsafe flag explosion|
|/cfire|/chestsafe flag fire|
|/credstone|/chestsafe flag redstone|
|/cmob|/chestsafe flag mob|

# API
## Maven
```xml
<repositories>
    <repository>
        <id>himajyun-repo</id>
        <url>https://himajyun.github.io/mvn-repo/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>jp.jyn</groupId>
        <artifactId>ChestSafe</artifactId>
        <version>1.1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Usage
```java
public class Main extends JavaPlugin {
    private ChestSafe chestSafe;

    @Override
    public void onEnable() {
        // get plugin
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ChestSafe");
        if(plugin == null || !plugin.isEnabled()) {
            // not available
            getLogger().warning("ChestSafe is not available.");
        }

        this.chestSafe = (ChestSafe) plugin;
    }

    public void usage(Block block) {
        // get
        Optional<Protection> protection = chestSafe.getRepository().get(block);

        // set
        Protection newProtection = Protection.newProtection();
        chestSafe.getRepository().set(newProtection,block);
    }
}
```
