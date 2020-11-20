package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public class Public extends SubCommand {
    private final MessageConfig message;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Public(MessageConfig message, ProtectionRepository repository, PlayerAction action) {
        this.message = message;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        Player player = (Player) sender;

        action.setAction(player, block -> CommandUtils.setProtection(
            message, repository,
            player, block,
            Protection.newProtection()
                .setType(Protection.Type.PUBLIC)
                .setOwner(player)
        ));
        player.sendMessage(message.ready.toString());
        return Result.OK;
    }

    @Override
    protected boolean isPlayerOnly() {
        return true;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.create.public";
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe public",
            message.help.public_.toString()
        );
    }
}
