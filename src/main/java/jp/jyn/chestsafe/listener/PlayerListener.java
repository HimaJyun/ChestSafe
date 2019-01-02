package jp.jyn.chestsafe.listener;

import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.config.parser.Parser;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.normalizer.ChestNormalizer;
import jp.jyn.chestsafe.util.ActionBarSender;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.chestsafe.uuid.UUIDRegistry;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public class PlayerListener implements Listener {
    private final Map<Material, MainConfig.ProtectionConfig> protectable;
    private final UUIDRegistry registry;
    private final ProtectionRepository repository;
    private final PlayerAction action;
    private final boolean useActionBar;

    private final Parser notice, denied, protected_, removed;
    private final Sender sender;

    private interface Sender {
        void send(Player player, String message);
    }

    public PlayerListener(MainConfig config, MessageConfig message, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        this.protectable = config.protectable;
        this.registry = registry;
        this.repository = repository;
        this.action = action;

        this.useActionBar = config.actionBar;
        if (config.actionBar) {
            notice = message.actionbar.notice;
            denied = message.actionbar.denied;
            protected_ = message.actionbar.protected_;
            removed = message.actionbar.removed;

            ActionBarSender actionbar = new ActionBarSender();
            sender = (p, m) -> {
                if (p.hasPermission("chestsafe.notice")) {
                    actionbar.send(p, m);
                }
            };
        } else {
            notice = message.notice;
            denied = message.denied;
            protected_ = message.protected_;
            removed = message.removed;

            sender = (p, m) -> {
                if (p.hasPermission("chestsafe.notice")) {
                    p.sendMessage(m);
                }
            };
        }

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        registry.updateCache(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block block = e.getClickedBlock();
        if (!isProtectable(block)) {
            return;
        }

        Player player = e.getPlayer();
        if (action.execAction(player, block)) {
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setCancelled(true);
            return;
        }

        Protection protection = getProtection(block);
        if (protection == null) {
            return;
        }

        Parser.Variable variable = new Parser.StringVariable()
            .put("block", block.getType())
            .put("type", protection.getType());

        // available check
        if (protection.getType() != Protection.Type.PUBLIC &&
            !protection.isAvailable(player) &&
            !isPassthrough(player)) {
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setCancelled(true);
            sender.send(player, denied.toString(variable));
            return;
        }

        variable.put("uuid", protection.getOwner());
        registry.getNameAsync(protection.getOwner(), name -> sender.send(
            player,
            notice.toString(
                variable.put("name", name.orElse("Unknown"))
            ))
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (!isProtectable(block)) {
            return;
        }

        Protection protection = getProtection(block);
        if (protection == null) {
            return;
        }

        Player player = e.getPlayer();
        if (!protection.isOwner(player) && !isPassthrough(player)) {
            e.setCancelled(true);
            sender.send(
                e.getPlayer(),
                denied.toString(
                    new Parser.StringVariable()
                        .put("block", block.getType())
                        .put("type", protection.getType())
                )
            );
            return;
        }

        // In case of double chest it is necessary to move protection.
        Optional<Block> doubleChest = ChestNormalizer.searchDoubleChestPair(block);
        if (doubleChest.isPresent()) {
            repository.chestShrink(protection, doubleChest.get());
            return;
        }
        repository.remove(protection);

        if (useActionBar && player.getGameMode() == GameMode.CREATIVE) {
            // In the creative mode, since PlayerInteractEvent and BlockBreakEvent occur simultaneously,
            //   it is necessary not to overwrite the ActionBar.
            return;
        }

        sender.send(
            e.getPlayer(),
            removed.toString(
                new Parser.StringVariable()
                    .put("block", block.getType())
                    .put("type", protection.getType())
            )
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Block block = e.getBlockPlaced();
        MainConfig.ProtectionConfig config = protectable.get(block.getType());
        if (config == null) {
            return;
        }

        // not auto register
        if (!config.auto.isPresent()) {
            return;
        }

        // already protected
        if (repository.chestExpand(block)) {
            return;
        }

        Protection protection = Protection.newProtection()
            .setType(config.auto.get())
            .setOwner(e.getPlayer());
        if (repository.set(protection, block) != ProtectionRepository.Result.SUCCESS) {
            return;
        }

        sender.send(
            e.getPlayer(),
            protected_.toString(
                new Parser.StringVariable()
                    .put("block", block.getType())
                    .put("type", protection.getType())
            )
        );
    }

    private Protection getProtection(Block block) {
        return repository.get(block).orElse(null);
    }

    private boolean isProtectable(Material material) {
        return protectable.containsKey(material);
    }

    private boolean isProtectable(Block block) {
        return isProtectable(block.getType());
    }

    private boolean isPassthrough(Player player) {
        return player.hasPermission("chestsafe.passthrough");
    }
}
