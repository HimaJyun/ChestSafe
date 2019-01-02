package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MessageConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.util.Queue;

public class Reload extends SubCommand {
    private final Plugin main;

    public Reload(MessageConfig message, Plugin main) {
        super(message);
        this.main = main;
    }

    @Override
    protected boolean execCommand(CommandSender sender, Queue<String> args) {
        // reload
        main.getServer().getPluginManager().callEvent(new PluginEnableEvent(main));
        main.onDisable();
        main.onEnable();
        main.getServer().getPluginManager().callEvent(new PluginDisableEvent(main));

        sender.sendMessage(message.reloaded.toString());
        if (sender instanceof Player) {
            Bukkit.getConsoleSender().sendMessage(message.reloaded.toString());
        }
        return true;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.reload";
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe reload",
            message.help.reload.toString()
        );
    }
}
