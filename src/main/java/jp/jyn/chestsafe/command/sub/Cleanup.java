package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.MainConfig;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.ProtectionCleaner;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class Cleanup extends SubCommand {
    private final MainConfig config;
    private final BukkitLocale<MessageConfig> message;
    private final ProtectionRepository repository;

    public Cleanup(MainConfig config, BukkitLocale<MessageConfig> message, ProtectionRepository repository) {
        this.config = config;
        this.message = message;
        this.repository = repository;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        int limit = config.cleanup.limit;
        // argument check.
        if (!args.isEmpty()) {
            String value = args.remove();
            // cancel
            if (value.equalsIgnoreCase("cancel")) {
                ProtectionCleaner.addSender(sender);
                if (!ProtectionCleaner.cancel()) {
                    // 何もメッセージが出ないのも不自然なので適当に出しておく
                    message.get(sender).cleanup.cancelled.apply().send(sender);
                }
                return Result.OK;
            }

            // limit
            try {
                limit = Integer.parseInt(value);
                if (limit >= 1000 || limit <= 0) {
                    message.get(sender).invalidArgument.apply("value", value).send(sender);
                    return Result.ERROR;
                }
            } catch (NumberFormatException e) {
                message.get(sender).invalidArgument.apply("value", value).send(sender);
                return Result.ERROR;
            }
        }

        if (ProtectionCleaner.isRunning()) {
            ProtectionCleaner.addSender(sender);
            message.get(sender).cleanup.already.apply().send(sender);
            return Result.OK;
        }

        CommandSender[] senders;
        if (sender instanceof Player) {
            // In case of execution from within game, output log to console
            senders = new CommandSender[]{sender, Bukkit.getConsoleSender()};
        } else {
            senders = new CommandSender[]{sender};
        }
        new ProtectionCleaner(ChestSafe.getInstance(), config, message, repository, limit, TimeUnit.MILLISECONDS, config.cleanup.unloaded, senders);
        return Result.OK;
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, Deque<String> args) {
        if (args.size() == 1 && "cancel".startsWith(args.removeFirst())) {
            return Collections.singletonList("cancel");
        }

        return Collections.emptyList();
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.cleanup";
    }
}
