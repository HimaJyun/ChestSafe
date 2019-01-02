package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.ProtectionCleaner;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

public class Cleanup extends SubCommand {
    private final MainConfig config;
    private final Plugin plugin;
    private final ProtectionRepository repository;

    private ProtectionCleaner cleaner = null;

    public Cleanup(MessageConfig message, MainConfig config, Plugin plugin, ProtectionRepository repository) {
        super(message);
        this.config = config;
        this.plugin = plugin;
        this.repository = repository;
    }

    @Override
    protected boolean execCommand(CommandSender sender, Queue<String> args) {
        int speed = config.cleanup.checkPerSecond;
        // argument check.
        if (!args.isEmpty()) {
            String value = args.remove();
            // cancel
            if (value.equalsIgnoreCase("cancel")) {
                if (running()) {
                    cleaner.cancel();
                    cleaner = null;
                }
                sender.sendMessage(message.cleanup.cancelled.toString());
                return true;
            }

            // speed
            try {
                speed = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                sender.sendMessage(message.invalidArgument.toString("value", value));
                return false;
            }
        }

        if (running()) {
            sender.sendMessage(message.cleanup.already.toString());
            return true;
        }

        CommandSender[] senders;
        if (sender instanceof Player) {
            // In case of execution from within game, output log to console
            senders = new CommandSender[]{sender, Bukkit.getConsoleSender()};
        } else {
            senders = new CommandSender[]{sender};
        }
        cleaner = new ProtectionCleaner(config, message, repository, speed, senders);
        cleaner.runTaskTimer(plugin, 0, 20);
        return true;
    }

    @Override
    protected List<String> execTabComplete(CommandSender sender, Deque<String> args) {
        if (args.size() == 1 && "cancel".startsWith(args.removeFirst())) {
            return Collections.singletonList("cancel");
        }

        return Collections.emptyList();
    }

    private boolean running() {
        return cleaner != null && !cleaner.isCancelled();
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.cleanup";
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe cleanup [speed]",
            message.help.cleanup.toString(),
            "/chestsafe cleanup",
            "/chestsafe cleanup 100",
            "/chestsafe cleanup cancel"
        );
    }
}
