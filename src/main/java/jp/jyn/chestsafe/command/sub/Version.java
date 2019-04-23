package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.util.VersionChecker;
import jp.jyn.jbukkitlib.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Queue;

public class Version extends SubCommand {
    private final MessageConfig message;
    private final VersionChecker checker;
    private final PluginDescriptionFile description;

    public Version(MessageConfig message, VersionChecker checker) {
        this.message = message;
        this.checker = checker;
        this.description = ChestSafe.getInstance().getDescription();
    }

    @Override
    protected Result execCommand(CommandSender sender, Queue<String> args) {
        sender.sendMessage(MessageConfig.HEADER);
        sender.sendMessage(description.getName() + " - " + description.getVersion());
        sender.sendMessage(description.getDescription());
        sender.sendMessage("Developer: " + String.join(",", description.getAuthors()));
        sender.sendMessage("SourceCode: " + description.getWebsite());
        checker.check(sender);
        return Result.OK;
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
