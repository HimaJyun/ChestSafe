package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.MainConfig;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentVariable;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Flag extends SubCommand { // APIの邪魔
    private enum Value {TRUE, FALSE, REVERSE, REMOVE}

    private final MainConfig config;
    private final BukkitLocale<MessageConfig> message;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Flag(MainConfig config, BukkitLocale<MessageConfig> message, ProtectionRepository repository, PlayerAction action) {
        this.config = config;
        this.message = message;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        Player player = (Player) sender;

        String tmp = args.remove();
        Protection.Flag flag;
        try {
            flag = Protection.Flag.valueOf(tmp.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            message.get(player).invalidArgument.apply("value", tmp).send(player);
            return Result.ERROR;
        }

        // permission
        if (!player.hasPermission("chestsafe.flag." + flag.name().toLowerCase(Locale.ENGLISH))) {
            message.get(player).doNotHavePermission.apply().send(player);
            return Result.OK;
        }

        Value value = parseValue(args.peek());
        if (value == null) {
            message.get(player).invalidArgument.apply("value", args.peek()).send(player);
            return Result.ERROR;
        }

        action.setAction(player, b -> setFlag(player, b, flag, value));
        message.get(player).ready.apply().send(player);
        return Result.OK;
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
            if (CommandUtils.str2Bool(value)) {
                return Value.TRUE;
            } else {
                return Value.FALSE;
            }
        } catch (IllegalArgumentException ignore) { }
        return null;
    }

    private void setFlag(Player player, Block block, Protection.Flag flag, Value value) {
        ComponentVariable variable = ComponentVariable.init();
        Optional<Protection> optional = CommandUtils.checkProtection(message, repository, player, block, variable);
        if (!optional.isPresent()) {
            return;
        }

        Protection protection = optional.get();
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
        variable.put("value", newValue).put("flag", flag.name().toLowerCase(Locale.ENGLISH));
        message.get(player).flagSet.apply(variable).send(player);
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, Deque<String> args) {
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
}
