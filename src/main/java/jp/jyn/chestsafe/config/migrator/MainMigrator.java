package jp.jyn.chestsafe.config.migrator;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.jbukkitlib.config.YamlMigration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

public class MainMigrator {
    private final static String FILE = "config.yml";
    private final static int CURRENT_VERSION = 2;

    private MainMigrator() { }

    public static boolean migration(ConfigurationSection config) {
        int version = config.getInt("version", -1);
        if (version == CURRENT_VERSION) {
            return false;
        }
        Logger logger = ChestSafe.getInstance().getLogger();
        logger.info("Migrate " + FILE);

        YamlMigration.backup(ChestSafe.getInstance(), FILE, version);

        if (version == 1) {
            v1to2(config);
        } else {
            logger.severe("Settings cannot be migrated automatically.");
            logger.severe(String.format("Please delete %s and restart.", FILE));
            throw new IllegalStateException(String.format("Unknown version (%s:%d)", FILE, version));
        }
        return true;
    }

    private static void v1to2(ConfigurationSection config) {
        config.set("cleanup.checkPerSecond", null);
        config.set("cleanup.checkMs", 50);
    }
}
