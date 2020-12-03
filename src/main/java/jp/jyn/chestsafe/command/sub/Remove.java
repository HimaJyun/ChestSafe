package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentVariable;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public class Remove extends SubCommand {
    private final BukkitLocale<MessageConfig> message;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Remove(BukkitLocale<MessageConfig> message, ProtectionRepository repository, PlayerAction action) {
        this.message = message;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        Player player = (Player) sender;

        action.setAction(player, block -> removeProtection(player, block));
        message.get(player).ready.apply().send(player);
        return Result.OK;
    }

    private void removeProtection(Player player, Block block) {
        ComponentVariable variable = ComponentVariable.init();
        CommandUtils.checkProtection(message, repository, player, block, variable).ifPresent(protection -> {
            repository.remove(protection);
            message.get(player).removed.apply(variable).send(player);
        });
    }

    @Override
    protected boolean isPlayerOnly() {
        return true;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.remove";
    }
}
