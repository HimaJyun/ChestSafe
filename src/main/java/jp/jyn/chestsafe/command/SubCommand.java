package jp.jyn.chestsafe.command;

import jp.jyn.chestsafe.config.config.MessageConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public abstract class SubCommand implements CommandExecutor, TabCompleter {
    private final static Deque<String> EMPTY_DEQUE = new ArrayDeque<>(0);

    protected final MessageConfig message;

    public SubCommand(MessageConfig message) {
        this.message = message;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isPlayer = sender instanceof Player;
        // check player only mode.
        if (!isPlayer && isPlayerOnly()) {
            sender.sendMessage(MessageConfig.PLAYER_ONLY);
            return true;
        }

        // check permission.
        if (!Optional.ofNullable(requirePermission()).map(sender::hasPermission).orElse(true)) {
            sender.sendMessage(message.doNotHavePermission.toString());
            return true;
        }

        // check args length
        if (args.length < (minimumArgs() + 1)) {
            sender.sendMessage(message.missingArgument.toString());
            return false;
        }

        // create args queue
        Queue<String> subArgs = argsDeque(args);

        if (isPlayer) {
            return execCommand((Player) sender, subArgs);
        } else {
            return execCommand(sender, subArgs);
        }
    }

    protected boolean execCommand(Player sender, Queue<String> args) {
        return execCommand((CommandSender) sender, args);
    }

    protected boolean execCommand(CommandSender sender, Queue<String> args) {
        throw new UnsupportedOperationException("This command is not implemented.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return execTabComplete(sender, argsDeque(args));
    }

    protected List<String> execTabComplete(CommandSender sender, Deque<String> args) {
        return Collections.emptyList();
    }

    private Deque<String> argsDeque(String[] args) {
        if (args.length <= 1) {
            EMPTY_DEQUE.clear();
            return EMPTY_DEQUE;
        }

        Deque<String> result = new ArrayDeque<>(Arrays.asList(args));
        result.removeFirst(); // remove first args(sub command has already been switched)

        return result;
    }

    protected boolean isPlayerOnly() {
        return false;
    }

    protected String requirePermission() {
        return null;
    }

    protected int minimumArgs() {
        return 0;
    }

    public CommandHelp getHelp() {
        return null;
    }

    protected boolean str2Bool(String str) throws IllegalArgumentException {
        Objects.requireNonNull(str);

        switch (str.toLowerCase(Locale.ENGLISH)) {
            case "on":
            case "1":
            case "true":
            case "yes":
            case "enable":
                return true;
            case "off":
            case "0":
            case "false":
            case "no":
            case "disable":
                return false;
            default:
                throw new IllegalArgumentException("Argument is not interpretable as boolean.");
        }
    }

    protected final static class CommandHelp {
        public final String usage;
        public final String description;
        public final String[] example;

        public CommandHelp(String usage, String description, String... example) {
            this.usage = usage;
            this.description = description;
            this.example = example;
        }
    }
}
