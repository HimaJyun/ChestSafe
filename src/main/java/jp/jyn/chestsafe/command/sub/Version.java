package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MessageConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Queue;

public class Version extends SubCommand {
    private final PluginDescriptionFile description;

    public Version(MessageConfig message, Plugin plugin) {
        super(message);
        this.description = plugin.getDescription();
    }

    @Override
    protected boolean execCommand(CommandSender sender, Queue<String> args) {
        sender.sendMessage(MessageConfig.HEADER);
        sender.sendMessage(description.getName() + " - " + description.getVersion());
        sender.sendMessage(description.getDescription());
        sender.sendMessage("Developer: " + String.join(",", description.getAuthors()));
        sender.sendMessage("SourceCode: " + description.getWebsite());
        return true;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.version";
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe version",
            message.help.version.toString()
        );
    }
}
