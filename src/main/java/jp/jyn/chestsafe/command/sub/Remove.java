package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.parser.template.variable.StringVariable;
import jp.jyn.jbukkitlib.config.parser.template.variable.TemplateVariable;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public class Remove extends SubCommand {
    private final MessageConfig message;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Remove(MessageConfig message, ProtectionRepository repository, PlayerAction action) {
        this.message = message;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        Player player = (Player) sender;

        action.setAction(player, block -> removeProtection(player, block));
        player.sendMessage(message.ready.toString());
        return Result.OK;
    }

    private void removeProtection(Player player, Block block) {
        TemplateVariable variable = StringVariable.init();
        CommandUtils.checkProtection(message, repository, player, block, variable).ifPresent(protection -> {
            repository.remove(protection);
            player.sendMessage(message.removed.toString(variable));
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

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe remove",
            message.help.remove.toString()
        );
    }
}
