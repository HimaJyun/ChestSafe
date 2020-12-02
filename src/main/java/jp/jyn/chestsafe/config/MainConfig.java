package jp.jyn.chestsafe.config;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.util.normalizer.BedNormalizer;
import jp.jyn.chestsafe.util.normalizer.DoorNormalizer;
import jp.jyn.jbukkitlib.cache.CacheFactory;
import jp.jyn.jbukkitlib.cache.SizedFactory;
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
import java.util.stream.Collectors;

public class MainConfig {

    public final boolean actionBar;
    public final boolean versionCheck;
    public final boolean hopperDestroy;

    public final boolean localeEnable;
    public final String localeDefault;

    public final Map<Material, ProtectionConfig> protectable = new EnumMap<>(Material.class);

    public final CleanupConfig cleanup;
    public final DatabaseConfig database;
    public final CacheConfig cache;

    @PackagePrivate
    MainConfig(FileConfiguration config) {
        actionBar = config.getBoolean("actionBar");
        versionCheck = config.getBoolean("versionCheck");
        hopperDestroy = config.getBoolean("hopperDestroy");

        localeEnable = config.getBoolean("locale.enable");
        localeDefault = config.getString("locale.default");

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
        public final int limit;
        public final boolean unloaded;

        private CleanupConfig(ConfigurationSection config) {
            enable = config.getBoolean("enable", true);
            delay = config.getLong("delay", TimeUnit.MINUTES.toSeconds(10));
            interval = config.getLong("interval", TimeUnit.DAYS.toSeconds(1));
            unloaded = config.getBoolean("unloaded", false);

            int l = config.getInt("limit", 50);
            if (l >= 1000 || l <= 0) {
                ChestSafe.getInstance().getLogger().warning("range of cleanup.limit is 1-999");
                l = 50;
            }
            limit = l;
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

            maximumPoolSize = config.getInt("connectionPool.maximumPoolSize", -1);
            minimumIdle = config.getInt("connectionPool.minimumIdle", -1);
            maxLifetime = config.getLong("connectionPool.maxLifetime", -1);
            connectionTimeout = config.getLong("connectionPool.connectionTimeout", -1);
            idleTimeout = config.getLong("connectionPool.idleTimeout", -1);
        }
    }

    public static class CacheConfig {
        public final CacheFactory id;
        public final CacheFactory protection;
        public final CacheFactory location;

        private CacheConfig(ConfigurationSection config) {
            id = new SizedFactory(config.getInt("id", -1));
            location = new SizedFactory(config.getInt("location", 30000));
            protection = new SizedFactory(config.getInt("protection", 10000));
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
                return Arrays.asList(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
            case "_SHULKER_BOX":
                return Arrays.stream(Material.values())
                    .filter(m -> m.name().endsWith("SHULKER_BOX"))
                    .filter(m -> !m.name().startsWith("LEGACY_"))
                    .collect(Collectors.toList());
            case "_FENCE_GATE":
                return Arrays.stream(Material.values())
                    .filter(m -> m.name().endsWith("_FENCE_GATE"))
                    .filter(m -> !m.name().startsWith("LEGACY_"))
                    .collect(Collectors.toList());
            case "_SIGN":
                return Arrays.stream(Material.values())
                    .filter(m -> m.name().endsWith("_SIGN"))
                    .filter(m -> !m.name().startsWith("LEGACY_"))
                    .collect(Collectors.toList());
            case "_DOOR":
                return DoorNormalizer.getDoors();
            case "_BANNER":
                return Arrays.stream(Material.values())
                    .filter(m -> m.name().endsWith("_BANNER"))
                    .filter(m -> !m.name().startsWith("LEGACY_"))
                    .collect(Collectors.toList());
            case "_TRAPDOOR":
                return Arrays.stream(Material.values())
                    .filter(m -> m.name().endsWith("_TRAPDDOR"))
                    .filter(m -> !m.name().startsWith("LEGACY_"))
                    .collect(Collectors.toList());
            case "_BED":
                return BedNormalizer.getBeds();
            case "*":
                return Arrays.asList(Material.values());
            default:
                return Collections.singletonList(Material.valueOf(key));
        }
    }
}
