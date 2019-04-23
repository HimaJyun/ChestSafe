package jp.jyn.chestsafe.config;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.util.normalizer.BedNormalizer;
import jp.jyn.chestsafe.util.normalizer.DoorNormalizer;
import jp.jyn.jbukkitlib.cache.CacheFactory;
import jp.jyn.jbukkitlib.util.PackagePrivate;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MainConfig {

    public final boolean actionBar;
    public final boolean versionCheck;

    public final Map<Material, ProtectionConfig> protectable = new EnumMap<>(Material.class);

    public final CleanupConfig cleanup;
    public final DatabaseConfig database;
    public final CacheConfig cache;

    @PackagePrivate
    MainConfig(FileConfiguration config) {
        actionBar = config.getBoolean("actionBar");
        versionCheck = config.getBoolean("versionCheck");

        ProtectionConfig defaultValue = new ProtectionConfig(config.getConfigurationSection("default"));
        for (Protection.Flag flag : Protection.Flag.values()) {
            // Initialize unset flags
            defaultValue.flag.putIfAbsent(flag, Boolean.FALSE);
        }

        if (config.contains("protectable")) {
            for (String key : config.getConfigurationSection("protectable").getKeys(false)) {
                ProtectionConfig value = new ProtectionConfig(config.getConfigurationSection("protectable." + key), defaultValue);
                for (Material material : getMaterial(key)) {
                    protectable.put(material, value);
                }
            }
        }

        cleanup = new CleanupConfig(config.getConfigurationSection("cleanup"));
        database = new DatabaseConfig(config.getConfigurationSection("database"));
        cache = new CacheConfig(config.getConfigurationSection("cache"));
    }

    public static class CleanupConfig {
        public final boolean enable;
        public final long delay;
        public final long interval;
        public final int checkPerSecond;

        private CleanupConfig(ConfigurationSection config) {
            enable = config.getBoolean("enable", true);
            delay = config.getLong("delay", TimeUnit.MINUTES.toSeconds(10));
            interval = config.getLong("interval", TimeUnit.DAYS.toSeconds(1));
            checkPerSecond = config.getInt("checkPerSecond", 100);
        }
    }

    public static class DatabaseConfig {
        public final String url;
        public final String username;
        public final String password;
        public final String init;
        public final Properties properties = new Properties();

        public final int maximumPoolSize;
        public final int minimumIdle;
        public final long maxLifetime;
        public final long connectionTimeout;
        public final long idleTimeout;

        private DatabaseConfig(ConfigurationSection config) {
            String tmpType = config.getString("type", "").toLowerCase(Locale.ENGLISH);
            switch (tmpType) {
                case "sqlite":
                    Plugin plugin = ChestSafe.getInstance();
                    File db = new File(plugin.getDataFolder(), config.getString("sqlite.file", "chestsafe.db"));
                    //noinspection ResultOfMethodCallIgnored
                    db.getParentFile().mkdirs();
                    url = "jdbc:sqlite:" + db.getPath();
                    init = "PRAGMA `foreign_keys`=`ON`"; // Enable FOREIGN KEY
                    break;
                case "mysql":
                    url = String.format(
                        "jdbc:mysql://%s/%s",
                        config.getString("mysql.host", "localhost:3306"),
                        config.getString("mysql.name", "chestsafe")
                    );
                    init = config.getString("mysql.init", "/* ChestSafe */SELECT 1");
                    break;
                default:
                    throw new IllegalArgumentException("Invalid value: Database.Type(config.yml)");
            }

            username = config.getString(tmpType + ".username");
            password = config.getString(tmpType + ".password");

            String tmpKey = tmpType + ".properties";
            if (config.contains(tmpKey)) {
                for (String key : config.getConfigurationSection(tmpKey).getKeys(false)) {
                    properties.put(key, config.getString(tmpKey + "." + key));
                }
            }

            maximumPoolSize = config.getInt("database.connectionPool.maximumPoolSize", -1);
            minimumIdle = config.getInt("database.connectionPool.minimumIdle", -1);
            maxLifetime = config.getLong("database.connectionPool.maxLifetime", -1);
            connectionTimeout = config.getLong("database.connectionPool.connectionTimeout", -1);
            idleTimeout = config.getLong("database.connectionPool.idleTimeout", -1);
        }
    }

    public static class CacheConfig {
        public final CacheFactory id;
        public final CacheFactory protection;
        public final CacheFactory location;

        private CacheConfig(ConfigurationSection config) {
            id = new CacheFactory.Sized(config.getInt("id", -1));
            location = new CacheFactory.Sized(config.getInt("location", 30000));
            protection = new CacheFactory.Sized(config.getInt("protection", 10000));
        }
    }

    public static class ProtectionConfig {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        public final Optional<Protection.Type> auto;
        public final Map<Protection.Flag, Boolean> flag = new EnumMap<>(Protection.Flag.class);

        private ProtectionConfig(ConfigurationSection config) {
            auto = getType(config);
            flag.putAll(getFlag(config));
        }

        private ProtectionConfig(ConfigurationSection config, ProtectionConfig defaultValue) {
            auto = config.contains("auto") ? getType(config) : defaultValue.auto;

            flag.putAll(defaultValue.flag);
            flag.putAll(getFlag(config));
        }

        private Map<Protection.Flag, Boolean> getFlag(ConfigurationSection config) {
            if (!config.contains("flag")) {
                return Collections.emptyMap();
            }

            Map<Protection.Flag, Boolean> r = new EnumMap<>(Protection.Flag.class);

            for (String key : config.getConfigurationSection("flag").getKeys(false)) {
                r.put(
                    Protection.Flag.valueOf(key.toUpperCase(Locale.ENGLISH)),
                    config.getBoolean("flag." + key)
                );
            }

            return r;
        }

        private Optional<Protection.Type> getType(ConfigurationSection config) {
            switch (config.getString("auto", "false").toLowerCase(Locale.ENGLISH)) {
                case "off":
                case "0":
                case "false":
                case "no":
                case "disable":
                case "none":
                    return Optional.empty();
                case "private":
                    return Optional.of(Protection.Type.PRIVATE);
                case "public":
                    return Optional.of(Protection.Type.PUBLIC);
                default:
                    throw new IllegalArgumentException(config.getCurrentPath() + ".auto is an invalid value.");
            }
        }
    }

    private Collection<Material> getMaterial(String material) {
        String key = material.toUpperCase(Locale.ENGLISH);
        switch (key) {
            case "_CHEST":
                return Arrays.asList(Material.CHEST, Material.TRAPPED_CHEST);
            case "_SHULKER_BOX":
                return Arrays.asList(
                    Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
                    Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
                    Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
                    Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
                    Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.RED_SHULKER_BOX,
                    Material.BLACK_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX
                );
            case "_FENCE_GATE":
                return Arrays.asList(
                    Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
                    Material.JUNGLE_FENCE_GATE, Material.DARK_OAK_FENCE_GATE, Material.ACACIA_FENCE_GATE
                );
            case "_SIGN":
                return Arrays.asList(Material.SIGN, Material.WALL_SIGN);
            case "_DOOR":
                return DoorNormalizer.getDoors();
            case "_BANNER":
                return Arrays.asList(
                    Material.BLACK_BANNER, Material.BLACK_WALL_BANNER,
                    Material.BLUE_BANNER, Material.BLUE_WALL_BANNER,
                    Material.BROWN_BANNER, Material.BROWN_WALL_BANNER,
                    Material.CYAN_BANNER, Material.CYAN_WALL_BANNER,
                    Material.GRAY_BANNER, Material.GRAY_WALL_BANNER,
                    Material.GREEN_BANNER, Material.GREEN_WALL_BANNER,
                    Material.LIGHT_BLUE_BANNER, Material.LIGHT_BLUE_WALL_BANNER,
                    Material.LIGHT_GRAY_BANNER, Material.LIGHT_GRAY_WALL_BANNER,
                    Material.LIME_BANNER, Material.LIME_WALL_BANNER,
                    Material.MAGENTA_BANNER, Material.MAGENTA_WALL_BANNER,
                    Material.ORANGE_BANNER, Material.ORANGE_WALL_BANNER,
                    Material.PINK_BANNER, Material.PINK_WALL_BANNER,
                    Material.PURPLE_BANNER, Material.PURPLE_WALL_BANNER,
                    Material.RED_BANNER, Material.RED_WALL_BANNER,
                    Material.WHITE_BANNER, Material.WHITE_WALL_BANNER,
                    Material.YELLOW_BANNER, Material.YELLOW_WALL_BANNER);
            case "_TRAPDOOR":
                return Arrays.asList(
                    Material.OAK_DOOR, Material.IRON_TRAPDOOR,
                    Material.ACACIA_TRAPDOOR, Material.BIRCH_TRAPDOOR,
                    Material.DARK_OAK_TRAPDOOR, Material.JUNGLE_TRAPDOOR,
                    Material.SPRUCE_TRAPDOOR
                );
            case "_BED":
                return BedNormalizer.getBeds();
            case "*":
                return Arrays.asList(Material.values());
            default:
                return Collections.singletonList(Material.valueOf(key));
        }
    }
}
