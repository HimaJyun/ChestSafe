package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.util.Queue;

public class Reload extends SubCommand {
    private final BukkitLocale<MessageConfig> message;

    public Reload(BukkitLocale<MessageConfig> message) {
        this.message = message;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        final Plugin plugin = ChestSafe.getInstance();
        // reload
        plugin.getServer().getPluginManager().callEvent(new PluginDisableEvent(plugin));
        plugin.onDisable();
        plugin.onEnable();
        plugin.getServer().getPluginManager().callEvent(new PluginEnableEvent(plugin));

        message.get(sender).reloaded.apply().send(sender);
        if (sender instanceof Player) {
            message.get().reloaded.apply().console();
        }
        return Result.OK;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.reload";
    }
}
