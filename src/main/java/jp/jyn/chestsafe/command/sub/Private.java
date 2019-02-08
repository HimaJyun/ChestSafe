package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.uuid.UUIDRegistry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class Private extends SubCommand {
    private final MessageConfig message;
    private final UUIDRegistry registry;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Private(MessageConfig message, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        this.message = message;
        this.registry = registry;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected Result execCommand(Player sender, Queue<String> args) {
        // get and convert member uuid
        registry.getMultipleUUIDAsync(args).thenAcceptSync(map -> {
            Set<UUID> members = new HashSet<>(args.size());
            while (!args.isEmpty()) {
                String name = args.remove();
                UUID uuid = map.get(name);
                if (uuid == null) {
                    sender.sendMessage(message.playerNotFound.toString("name", name));
                    return;
                }
                members.add(uuid);
            }

            action.setAction(sender, block -> CommandUtils.setProtection(
                message, repository,
                sender, block,
                Protection.newProtection()
                    .setType(Protection.Type.PRIVATE)
                    .setOwner(sender)
                    .addMembers(members)
            ));
            sender.sendMessage(message.ready.toString());
        });
        return Result.OK;
    }

    @Override
    protected List<String> execTabComplete(CommandSender sender, Deque<String> args) {
        return CommandUtils.tabCompletePlayer(args);
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.create.private";
    }

    @Override
    protected boolean isPlayerOnly() {
        return true;
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe private [member]",
            message.help.private_.toString(),
            "/chestsafe private",
            "/chestsafe private member1",
            "/chestsafe private member1 member2"
        );
    }
}
