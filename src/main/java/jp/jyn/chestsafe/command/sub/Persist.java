package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Persist extends SubCommand {
    private final MessageConfig message;
    private final PlayerAction action;

    public Persist(MessageConfig message, PlayerAction action) {
        this.message = message;
        this.action = action;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        Player player = (Player)sender;

        boolean persist = action.getPersist(player);

        String value = args.poll();
        if (value == null) {
            // reverse
            persist = !persist;
        } else {
            try {
                persist = CommandUtils.str2Bool(value);
            } catch (IllegalArgumentException e) {
                player.sendMessage(message.invalidArgument.toString("value", value));
                return Result.ERROR;
            }
        }

        action.setPersist(player, persist);
        if (persist) {
            player.sendMessage(message.persistEnabled.toString());
        } else {
            player.sendMessage(message.persistDisabled.toString());
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

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe persist [value]",
            message.help.persist.toString(),
            "/chestsafe persist",
            "/chestsafe persist true"
        );
    }
}
