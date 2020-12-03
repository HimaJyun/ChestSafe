package jp.jyn.chestsafe.config;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.migrator.MainMigrator;
import jp.jyn.jbukkitlib.config.YamlLoader;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.locale.MultiLocale;
import jp.jyn.jbukkitlib.config.locale.SingleLocale;

import java.util.Collections;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ConfigLoader {
    private final YamlLoader mainLoader;
    private MainConfig mainConfig;

    private Map<String, YamlLoader> messageLoader = Collections.emptyMap();
    private BukkitLocale<MessageConfig> messageConfig;

    public ConfigLoader() {
        this.mainLoader = new YamlLoader(ChestSafe.getInstance(), "config.yml");
    }

    public void reloadConfig() {
        mainLoader.saveDefaultConfig();
        if (mainConfig != null) {
            mainLoader.reloadConfig();
        }
        if (MainMigrator.migration(mainLoader.getConfig())) {
            mainLoader.saveConfig();
        }
        mainConfig = new MainConfig(mainLoader.getConfig());

        YamlLoader.copyDir(ChestSafe.getInstance(), "locale");
        if (!mainConfig.localeEnable) {
            String defaultLocale = mainConfig.localeDefault;
            YamlLoader defaultLoader = reloadLocaleYaml(defaultLocale);
            messageLoader = Collections.singletonMap(defaultLocale, defaultLoader);
            messageConfig = new SingleLocale<>(defaultLocale, new MessageConfig(defaultLocale, defaultLoader.getConfig()));
            return;
        }

        // ファイルが存在
        //   する ->
        //     ロードしてない -> ロード
        //     ロード済み -> リロード
        //   しない -> mapから捨てる(古いmap自体を捨てて存在しないファイルを無視する)
        messageLoader = YamlLoader.findYaml(ChestSafe.getInstance(), "locale")
            .stream()
            .map(YamlLoader::removeExtension)
            .collect(Collectors.toMap(UnaryOperator.identity(), this::reloadLocaleYaml));
        messageConfig = new MultiLocale<>(
            mainConfig.localeDefault,
            messageLoader.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> new MessageConfig(e.getKey(), e.getValue().getConfig())
            ))
        );
    }

    private YamlLoader reloadLocaleYaml(String locale) {
        YamlLoader loader = messageLoader.get(locale);
        if (loader == null) {
            return new YamlLoader(ChestSafe.getInstance(), "locale/" + locale + ".yml");
        } else {
            loader.reloadConfig();
            return loader;
        }
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public BukkitLocale<MessageConfig> getMessageConfig() {
        return messageConfig;
    }
}
