package jp.jyn.chestsafe.command.sub;

import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.jbukkitlib.command.SubCommand;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentParser;
import jp.jyn.jbukkitlib.config.parser.component.ComponentVariable;
import jp.jyn.jbukkitlib.util.lazy.Lazy;
import jp.jyn.jbukkitlib.uuid.UUIDRegistry;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Entity;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

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

        action.setAction(player, block -> getInfo(player, block));
        message.get(player).ready.apply().send(player);
        return Result.OK;
    }

    private void getInfo(Player player, Block block) {
        Protection protection = repository.get(block).orElse(null);
        if (protection == null) {
            message.get(player).notProtected.apply("block", block.getType()).send(player);
            return;
        }

        // uuid -> name convert
        Set<UUID> request = new HashSet<>(protection.getMembers());
        request.add(protection.getOwner());
        registry.getMultipleNameAsync(request).thenAcceptSync(map -> showInfo(player,protection,map));
    }

    private void showInfo(Player player, Protection protection, Map<UUID, String> name) {
        ComponentVariable variable = ComponentVariable.init().put("type", protection.getType().name());

        UUID ownerUUID = protection.getOwner();
        String ownerName = name.get(ownerUUID);
        Lazy<HoverEvent> ownerHover = Lazy.of(() -> hoverPlayer(ownerUUID,ownerName));
        variable.put("owner",c -> {
            c.setText(ownerName);
            c.setHoverEvent(ownerHover.get());
        });
        variable.put("uuid",c -> {
            c.setText(ownerUUID.toString());
            c.setHoverEvent(ownerHover.get());
        });

        variable.put("member",(c,a) -> {
            final String separator = a.size() == 0 ? ", ": a.get(0);
            c.setText("");
            List<BaseComponent> m = new ArrayList<>((protection.getMembers().size()*2)-1);
            boolean first = true;
            for (UUID member : protection.getMembers()) {
                if(first) {
                    first = false;
                } else {
                    m.add(new TextComponent(separator));
                }
                String n = name.get(member);
                TextComponent component = new TextComponent(n);
                component.setHoverEvent(hoverPlayer(member,n));
                m.add(component);
            }
            c.setExtra(m);
        });
        variable.put("flag",(c,a) -> {
            final String separator;
            final String joint;
            if(a.size() == 0) {
                separator = ", ";
                joint = "=";
            } else if (a.size() == 1) {
                separator = ", ";
                joint = a.get(0);
            } else {
                separator = a.get(1);
                joint = a.get(0);
            }

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<Protection.Flag, Boolean> entry : protection.getFlags().entrySet()) {
                if(first) {
                    first = false;
                } else {
                    sb.append(separator);
                }
                sb.append(entry.getKey().name().toLowerCase(Locale.ENGLISH));
                sb.append(joint);
                sb.append(entry.getValue().toString());
            }
            c.setText(sb.toString());
        });

        player.sendMessage(MessageConfig.HEADER);
        for (ComponentParser c : message.get(player).info) {
            c.apply(variable).send(player);
        }
    }

    private HoverEvent hoverPlayer(UUID uuid, String name) {
        return new HoverEvent(HoverEvent.Action.SHOW_ENTITY,new Entity(
            EntityType.PLAYER.getKey().getKey(),
            uuid.toString(),
            new TextComponent(name)
        ));
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
