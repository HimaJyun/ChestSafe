package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.command.CommandUtils;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.parser.template.variable.StringVariable;
import jp.jyn.jbukkitlib.config.parser.template.variable.TemplateVariable;
import jp.jyn.jbukkitlib.uuid.UUIDRegistry;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Member extends SubCommand {
    private enum Operation {ADD, REMOVE}

    private final MessageConfig message;
    private final UUIDRegistry registry;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Member(MessageConfig message, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        this.message = message;
        this.registry = registry;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected Result execCommand(Player sender, Queue<String> args) {
        // get members args
        Map<String, Operation> members = new HashMap<>();
        String tmp = args.remove().toLowerCase(Locale.ENGLISH);
        switch (tmp) {
            case "add":
                args.forEach(name -> members.put(name, Operation.ADD));
                break;
            case "remove":
                args.forEach(name -> members.put(name, Operation.REMOVE));
                break;
            case "modify":
                while (!args.isEmpty()) {
                    String value = args.remove();
                    Operation operation = Operation.ADD;
                    if (value.charAt(0) == '-') {
                        operation = Operation.REMOVE;
                        value = value.substring(1);
                        if (value.isEmpty()) {
                            sender.sendMessage(message.invalidArgument.toString("value", "-"));
                            return Result.ERROR;
                        }
                    }
                    members.put(value, operation);
                }
                break;
            default:
                sender.sendMessage(message.invalidArgument.toString("value", tmp));
                return Result.ERROR;
        }

        // get uuid
        registry.getMultipleUUIDAsync(members.keySet()).thenAcceptSync(map -> {
            Set<UUID> add = new HashSet<>();
            Set<UUID> remove = new HashSet<>();
            // check user exists
            for (Map.Entry<String, Operation> entry : members.entrySet()) {
                UUID uuid = map.get(entry.getKey());
                if (uuid == null) {
                    sender.sendMessage(message.playerNotFound.toString("name", entry.getKey()));
                    return;
                }

                switch (entry.getValue()) {
                    case ADD:
                        add.add(uuid);
                        break;
                    case REMOVE:
                        remove.add(uuid);
                        break;
                }
            }

            action.setAction(sender, b -> modifyMember(sender, b, add, remove));
            sender.sendMessage(message.ready.toString());
        });
        return Result.OK;
    }

    private void modifyMember(Player player, Block block, Collection<UUID> add, Collection<UUID> remove) {
        TemplateVariable variable = StringVariable.init();
        CommandUtils.checkProtection(message, repository, player, block, variable).ifPresent(protection -> {
            if (!add.isEmpty()) {
                protection.addMembers(add);
            }
            if (!remove.isEmpty()) {
                protection.removeMembers(remove);
            }
            player.sendMessage(message.memberChanged.toString(variable));
        });
    }

    @Override
    protected List<String> execTabComplete(CommandSender sender, Deque<String> args) {
        if (args.size() == 1) {
            return Stream.of("add", "remove", "modify")
                .filter(str -> str.startsWith(args.getFirst()))
                .collect(Collectors.toList());
        }
        args.removeFirst(); // remove 1st args

        return CommandUtils.tabCompletePlayer(args);
    }

    @Override
    protected boolean isPlayerOnly() {
        return true;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.member";
    }

    @Override
    protected int minimumArgs() {
        return 1;
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe member <add/remove/modify> [value]",
            message.help.member.toString(),
            "/chestsafe member add member1",
            "/chestsafe member remove member1",
            "/chestsafe member modify member1 -member2"
        );
    }
}
