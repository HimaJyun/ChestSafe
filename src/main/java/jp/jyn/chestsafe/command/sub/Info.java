package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentVariable;
import jp.jyn.jbukkitlib.uuid.UUIDRegistry;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Info extends SubCommand {
    private final BukkitLocale<MessageConfig> message;
    private final UUIDRegistry registry;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    public Info(BukkitLocale<MessageConfig> message, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        this.message = message;
        this.registry = registry;
        this.repository = repository;
        this.action = action;
    }

    @Override
    protected Result onCommand(CommandSender sender, Queue<String> args) {
        Player player = (Player) sender;

        action.setAction(player, block -> showInfo(player, block));
        message.get(player).ready.apply().send(player);
        return Result.OK;
    }

    private void showInfo(Player player, Block block) {
        Protection protection = repository.get(block).orElse(null);
        if (protection == null) {
            message.get(player).notProtected.apply("block",block.getType()).send(player);
            return;
        }

        // uuid -> name convert
        Set<UUID> request = new HashSet<>(protection.getMembers());
        request.add(protection.getOwner());
        registry.getMultipleNameAsync(request).thenAcceptSync(map -> {
            // set variable
            ComponentVariable variable = ComponentVariable.init()
                .put("type", protection.getType().name())
                .put("owner", map.get(protection.getOwner()))
                .put("uuid", protection.getOwner())
                .put("members",
                    protection.getMembers().stream()
                        .map(map::get)
                        .collect(Collectors.joining(", "))
                )
                .put("flags",
                    protection.getFlags().entrySet().stream()
                        .map(v -> v.getKey().name().toLowerCase(Locale.ENGLISH) + "=" + v.getValue())
                        .collect(Collectors.joining(", "))
                );

            player.sendMessage(MessageConfig.HEADER);
            message.get(player).info.forEach(c -> c.apply(variable).send(player));
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
}
