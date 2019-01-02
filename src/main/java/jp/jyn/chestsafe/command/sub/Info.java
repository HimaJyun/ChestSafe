package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.chestsafe.command.SubCommand;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.config.parser.Parser;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.uuid.UUIDRegistry;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Info extends SubCommand {
    private final UUIDRegistry registry;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Info(MessageConfig message, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        super(message);
        this.registry = registry;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected boolean execCommand(Player sender, Queue<String> args) {
        action.setAction(sender, block -> showInfo(sender, block));
        sender.sendMessage(message.ready.toString());
        return true;
    }

    private void showInfo(Player player, Block block) {
        Protection protection = repository.get(block).orElse(null);
        if (protection == null) {
            player.sendMessage(message.notProtected.toString("block", block.getType()));
            return;
        }

        // uuid -> name convert
        Set<UUID> request = new LinkedHashSet<>(protection.getMembers());
        request.add(protection.getOwner());
        registry.getMultipleNameAsync(request, map -> {
            // set variable
            Parser.Variable variable = new Parser.StringVariable()
                .put("type", protection.getType().name())
                .put("owner", map.get(protection.getOwner()))
                .put("uuid", protection.getOwner())
                .put("members",
                    protection.getMembers().stream()
                        .map(map::get)
                        .collect(Collectors.joining(", "))
                )
                .put("flags",
                    protection.getFlags().stream()
                        .map(v -> v.getKey().name().toLowerCase(Locale.ENGLISH) + "=" + v.getValue())
                        .collect(Collectors.joining(", "))
                );

            player.sendMessage(MessageConfig.HEADER);
            message.info.forEach(msg -> player.sendMessage(msg.toString(variable)));
        });
    }

    @Override
    protected boolean isPlayerOnly() {
        return true;
    }

    @Override
    protected String requirePermission() {
        return "chestsafe.info";
    }

    @Override
    public CommandHelp getHelp() {
        return new CommandHelp(
            "/chestsafe info",
            message.help.info.toString()
        );
    }
}
