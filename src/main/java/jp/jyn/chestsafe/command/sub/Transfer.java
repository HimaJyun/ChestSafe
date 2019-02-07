package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.parser.template.variable.StringVariable;
import jp.jyn.jbukkitlib.uuid.UUIDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

public class Transfer extends SubCommand {
    private final MessageConfig message;
    private final UUIDRegistry registry;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Transfer(MessageConfig message, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        this.message = message;
        this.registry = registry;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected Result execCommand(Player sender, Queue<String> args) {
        registry.getUUIDAsync(args.element()).thenAcceptSync(uuid -> {
            if (!uuid.isPresent()) {
                sender.sendMessage(message.playerNotFound.toString("name", args.remove()));
                return;
            }

            action.setAction(sender, block -> transferOwner(sender, block, uuid.get()));
            sender.sendMessage(message.ready.toString());
            sender.sendMessage(message.transferWarning.toString());
        });
        return Result.OK;
    }

    private void transferOwner(Player player, Block block, UUID newOwner) {
        Protection protection = repository.get(block).orElse(null);
        if (protection == null) {
            player.sendMessage(message.notProtected.toString("block", block.getType()));
            return;
        }

        if (!protection.isOwner(player) &&
            !player.hasPermission("chestsafe.passthrough")) {
            player.sendMessage(message.denied.toString(StringVariable.init().put("block", block.getType()).put("type", protection.getType())));
            return;
        }
        protection.setOwner(newOwner);
        player.sendMessage(message.transferSuccess.toString());
    }

    @Override
    protected List<String> execTabComplete(CommandSender sender, Deque<String> args) {
        if (args.size() == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(str -> str.startsWith(args.getFirst()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    protected boolean isPlayerOnly() {
        return true;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.transfer";
    }

    @Override
    protected int minimumArgs() {
        return 1;
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe transfer <owner>",
            message.help.transfer.toString(),
            "/chestsafe transfer new_owner"
        );
    }
}
