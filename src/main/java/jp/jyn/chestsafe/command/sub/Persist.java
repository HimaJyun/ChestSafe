package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Persist extends SubCommand {
    private final BukkitLocale<MessageConfig> message;
    private final PlayerAction action;

    public Persist(BukkitLocale<MessageConfig> message, PlayerAction action) {
        this.message = message;
        this.action = action;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        Player player = (Player) sender;

        boolean persist = action.getPersist(player);

        String value = args.poll();
        if (value == null) {
            // reverse
            persist = !persist;
        } else {
            try {
                persist = CommandUtils.str2Bool(value);
            } catch (IllegalArgumentException e) {
                message.get(player).invalidArgument.apply("value", value).send(player);
                return Result.ERROR;
            }
        }

        action.setPersist(player, persist);
        if (persist) {
            message.get(player).persistEnabled.apply().send(player);
        } else {
            message.get(player).persistDisabled.apply().send(player);
        }

        return Result.OK;
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, Deque<String> args) {
        if (args.size() == 1) {
            return Stream.of("true", "false").filter(str -> str.startsWith(args.getFirst())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    protected boolean isPlayerOnly() {
        return true;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.persist";
    }
}
