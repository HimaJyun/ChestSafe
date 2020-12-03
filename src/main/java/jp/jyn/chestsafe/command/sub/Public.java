package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public class Public extends SubCommand {
    private final BukkitLocale<MessageConfig> message;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Public(BukkitLocale<MessageConfig> message, ProtectionRepository repository, PlayerAction action) {
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
        message.get(player).ready.apply().send(player);
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
}
