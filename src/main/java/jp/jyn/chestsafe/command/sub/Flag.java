package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.config.parser.Parser;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Flag extends SubCommand {
    private enum Value {TRUE, FALSE, REVERSE, REMOVE}

    private final String availableFlags;

    private final MainConfig config;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Flag(MessageConfig message, MainConfig config, ProtectionRepository repository, PlayerAction action) {
        super(message);
        this.config = config;
        this.repository = repository;
        this.action = action;

        this.availableFlags = message.help.availableFlags.toString(
            "flags",
            Arrays.stream(Protection.Flag.values()).map(Enum::name).map(str -> str.toLowerCase(Locale.ENGLISH)).collect(Collectors.joining(", "))
        );
    }

    @Override
    protected boolean execCommand(Player sender, Queue<String> args) {
        String tmp = args.remove();
        Protection.Flag flag;
        try {
            flag = Protection.Flag.valueOf(tmp.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(message.invalidArgument.toString("value", tmp));
            return false;
        }

        // permission
        if (!sender.hasPermission("chestsafe.flag." + flag.name().toLowerCase(Locale.ENGLISH))) {
            sender.sendMessage(message.doNotHavePermission.toString());
            return true;
        }

        Value value = parseValue(args.peek());
        if (value == null) {
            sender.sendMessage(message.invalidArgument.toString("value", args.peek()));
            return false;
        }

        action.setAction(sender, b -> setFlag(sender, b, flag, value));
        sender.sendMessage(message.ready.toString());
        return true;
    }

    private Value parseValue(String value) {
        if (value == null) {
            return Value.REVERSE;
        }

        value = value.toLowerCase(Locale.ENGLISH);
        switch (value) {
            case "remove":
            case "default":
            case "delete":
            case "none":
                return Value.REMOVE;
        }

        try {
            if (str2Bool(value)) {
                return Value.TRUE;
            } else {
                return Value.FALSE;
            }
        } catch (IllegalArgumentException ignore) { }
        return null;
    }

    private void setFlag(Player player, Block block, Protection.Flag flag, Value value) {
        Parser.Variable variable = new Parser.StringVariable().put("block", block.getType());

        Protection protection = repository.get(block).orElse(null);
        if (protection == null) {
            player.sendMessage(message.notProtected.toString(variable));
            return;
        }

        if (!protection.isOwner(player) &&
            !player.hasPermission("chestsafe.passthrough")) {
            variable.put("type", protection.getType());
            player.sendMessage(message.denied.toString(variable));
            return;
        }

        boolean defaultValue = config.protectable.get(block.getType()).flag.get(flag); // get should not always be null
        boolean newValue = defaultValue;
        switch (value) {
            case REMOVE:
                //newValue = defaultValue;
                break;
            case REVERSE:
                newValue = !protection.getFlag(flag).orElse(defaultValue);
                break;
            case TRUE:
                newValue = true;
                break;
            case FALSE:
                newValue = false;
                break;
        }

        if (newValue == defaultValue) {
            // Delete if flag is same as default value
            protection.removeFlag(flag);
        } else {
            protection.setFlag(flag, newValue);
        }

        // send message
        variable.clear()
            .put("flag", flag.name().toLowerCase(Locale.ENGLISH))
            .put("value", newValue);
        player.sendMessage(message.flagSet.toString(variable));
    }

    @Override
    protected List<String> execTabComplete(CommandSender sender, Deque<String> args) {
        if (args.size() == 1) {
            return Arrays.stream(Protection.Flag.values())
                .map(Enum::name)
                .map(str -> str.toLowerCase(Locale.ENGLISH))
                .filter(str -> str.startsWith(args.getFirst()))
                .collect(Collectors.toList());
        }

        if (args.size() == 2) {
            return Stream.of("true", "false", "remove")
                .filter(str -> str.startsWith(args.getLast()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    protected boolean isPlayerOnly() {
        return true;
    }

    @Override
    protected int minimumArgs() {
        return 1;
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe flag <flag> [value]",
            message.help.flag.toString(),
            "/chestsafe flag hopper true",
            "/chestsafe flag explosion remove",
            "/chestsafe flag redstone",
            availableFlags
        );
    }
}
