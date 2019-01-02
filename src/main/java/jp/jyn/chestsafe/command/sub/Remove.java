package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.config.parser.Parser;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Queue;

public class Remove extends SubCommand {
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Remove(MessageConfig message, ProtectionRepository repository, PlayerAction action) {
        super(message);
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected boolean execCommand(Player sender, Queue<String> args) {
        action.setAction(sender, block -> removeProtection(sender, block));
        sender.sendMessage(message.ready.toString());
        return true;
    }

    private void removeProtection(Player player, Block block) {
        Parser.Variable variable = new Parser.StringVariable().put("block", block.getType());

        Protection protection = repository.get(block).orElse(null);
        if (protection == null) {
            player.sendMessage(message.notProtected.toString(variable));
            return;
        }
        variable.put("type", protection.getType());

        if (!protection.isOwner(player) &&
            !player.hasPermission("chestsafe.passthrough")) {
            player.sendMessage(message.denied.toString(variable));
            return;
        }

        repository.remove(protection);
        player.sendMessage(message.removed.toString(variable));
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
