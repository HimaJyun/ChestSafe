package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.config.parser.Parser;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Queue;

public class Public extends SubCommand {
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Public(MessageConfig message, ProtectionRepository repository, PlayerAction action) {
        super(message);
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected boolean execCommand(Player sender, Queue<String> args) {
        action.setAction(sender, block -> setProtection(sender, block));
        sender.sendMessage(message.ready.toString());
        return true;
    }

    private void setProtection(Player player, Block block) {
        Protection protection = Protection.newProtection()
            .setType(Protection.Type.PUBLIC)
            .setOwner(player);
        ProtectionRepository.Result result = repository.set(protection, block);

        Parser.Variable variable = new Parser.StringVariable().put("block", block.getType());
        switch (result) {
            case NOT_PROTECTABLE:
                player.sendMessage(message.notProtectable.toString(variable));
                break;
            case ALREADY_PROTECTED:
                player.sendMessage(message.alreadyProtected.toString(variable));
                break;
            case SUCCESS:
                variable.put("type", protection.getType());
                player.sendMessage(message.protected_.toString(variable));
                break;
        }
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
