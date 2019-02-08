package jp.jyn.chestsafe.config;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.jbukkitlib.config.YamlLoader;
import org.bukkit.plugin.Plugin;

public class ConfigLoader {
    private final YamlLoader mainLoader;
    private MainConfig mainConfig;

    private final YamlLoader messageLoader;
    private MessageConfig messageConfig;

    public ConfigLoader() {
        Plugin plugin = ChestSafe.getInstance();
        this.mainLoader = new YamlLoader(plugin, "config.yml");
        this.messageLoader = new YamlLoader(plugin, "message.yml");
    }

    public void reloadConfig() {
        mainLoader.saveDefaultConfig();
        messageLoader.saveDefaultConfig();
        if (mainConfig != null || messageConfig != null) {
            mainLoader.reloadConfig();
            messageLoader.reloadConfig();
        }

        mainConfig = new MainConfig(mainLoader.getConfig());
        messageConfig = new MessageConfig(messageLoader.getConfig());
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public MessageConfig getMessageConfig() {
        return messageConfig;
    }
}
