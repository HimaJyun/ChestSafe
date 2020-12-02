package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.util.VersionChecker;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.Objects;
import java.util.Queue;

public class Version extends SubCommand {
    private final BukkitLocale<MessageConfig> message;
    private final VersionChecker checker;
    private final PluginDescriptionFile description;

    public Version(BukkitLocale<MessageConfig> message, VersionChecker checker) {
        this.message = message;
        this.checker = checker;
        this.description = ChestSafe.getInstance().getDescription();
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        sender.sendMessage(MessageConfig.HEADER);
        sender.sendMessage(description.getName() + " - " + description.getVersion());
        sender.sendMessage(Objects.requireNonNull(description.getDescription()));
        sender.sendMessage("Developer: " + String.join(",", description.getAuthors()));
        sender.sendMessage("SourceCode: " + description.getWebsite());
        if (sender instanceof Player) {
            sender.sendMessage("Locale: " + ((Player) sender).getLocale());
        }
        checker.check(sender);
        return Result.OK;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.version";
    }
}
