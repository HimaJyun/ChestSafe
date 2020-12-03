package jp.jyn.chestsafe.util;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentVariable;
import jp.jyn.jbukkitlib.util.updater.GitHubReleaseChecker;
import jp.jyn.jbukkitlib.util.updater.UpdateChecker;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class VersionChecker {
    private final static long CHECK_PERIOD = TimeUnit.HOURS.toMillis(12);

    private final boolean enable;
    private final BukkitLocale<MessageConfig> message;
    private final UpdateChecker checker = new GitHubReleaseChecker("HimaJyun", "ChestSafe");

    private long nextCheck = 0;
    private ComponentVariable variable = null;

    public VersionChecker(boolean enable, BukkitLocale<MessageConfig> message) {
        this.enable = enable;
        this.message = message;
    }

    public void check(CommandSender sender) {
        if (!enable) {
            return;
        }

        if (nextCheck > System.currentTimeMillis()) {
            if (variable != null) {
                sender.sendMessage(MessageConfig.HEADER);
                message.get(sender).newVersion.forEach(c -> c.apply(variable).send(sender));
            }
            return;
        }

        Plugin plugin = ChestSafe.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            nextCheck = CHECK_PERIOD + System.currentTimeMillis();
            UpdateChecker.LatestVersion latest = checker.callEx();

            String currentVersion = plugin.getDescription().getVersion();
            if (currentVersion.equals(latest.version)) {
                return;
            }

            variable = ComponentVariable.init()
                .put("old", currentVersion)
                .put("new", latest.version)
                .put("url", c -> {
                    c.setText(latest.url);
                    c.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, latest.url));
                });
            MessageConfig m = message.get(sender);
            sender.sendMessage(MessageConfig.HEADER);
            m.newVersion.forEach(c -> c.apply(variable).send(sender));
        });
    }
}
