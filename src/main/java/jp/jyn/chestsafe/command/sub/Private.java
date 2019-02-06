package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.config.parser.template.variable.StringVariable;
import jp.jyn.jbukkitlib.config.parser.template.variable.TemplateVariable;
import jp.jyn.jbukkitlib.uuid.UUIDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Private extends SubCommand {
    private final UUIDRegistry registry;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Private(MessageConfig message, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        super(message);
        this.registry = registry;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected boolean execCommand(Player sender, Queue<String> args) {
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

            action.setAction(sender, block -> setProtection(sender, members, block));
            sender.sendMessage(message.ready.toString());
        });
        return true;
    }

    private void setProtection(Player player, Collection<UUID> members, Block block) {
        Protection protection = Protection.newProtection()
            .setType(Protection.Type.PRIVATE)
            .setOwner(player)
            .addMembers(members);
        ProtectionRepository.Result result = repository.set(protection, block);

        TemplateVariable variable = StringVariable.init().put("block", block.getType());
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
    protected List<String> execTabComplete(CommandSender sender, Deque<String> args) {
        Set<String> result = Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(str -> str.startsWith(args.getLast()))
            .collect(Collectors.toSet());
        args.removeLast();

        args.stream().map(str -> str.toLowerCase(Locale.ENGLISH)).forEach(result::remove);

        return new ArrayList<>(result);
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
