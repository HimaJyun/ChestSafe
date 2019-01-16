package jp.jyn.chestsafe.listener;

import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.config.config.MessageConfig;
import jp.jyn.chestsafe.config.parser.Parser;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.chestsafe.util.ActionBarSender;
import jp.jyn.chestsafe.util.PlayerAction;
import jp.jyn.chestsafe.util.normalizer.ChestNormalizer;
import jp.jyn.chestsafe.uuid.UUIDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public class PlayerListener implements Listener {
    private final Plugin plugin;
    private final BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

    private final UUIDRegistry registry;
    private final ProtectionRepository repository;
    private final PlayerAction action;

    private final Map<Material, MainConfig.ProtectionConfig> protectable;
    private final boolean useActionBar;

    private final Parser notice, denied, protected_, removed;
    private final Sender sender;

    private interface Sender {
        void send(Player player, String message);
    }

    public PlayerListener(MainConfig config, MessageConfig message, Plugin plugin, UUIDRegistry registry, ProtectionRepository repository, PlayerAction action) {
        this.plugin = plugin;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
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

        Player player = e.getPlayer();
        Protection protection = Protection.newProtection()
            .setType(config.auto.get())
            .setOwner(e.getPlayer());

        // In the case of a chest you have to delay to correctly check the double chest.
        // Otherwise it will not be checked correctly
        if (isRelatedChest(block)) {
            scheduler.runTask(plugin, () -> {
                // already protected.
                if (repository.chestExpand(block)) {
                    return;
                }

                setProtection(config.auto.get(), player, block);
            });
            return;
        }

        setProtection(config.auto.get(), player, block);
    }

    private void setProtection(Protection.Type type, Player player, Block block) {
        Protection protection = Protection.newProtection()
            .setType(type)
            .setOwner(player);

        if (repository.set(protection, block) != ProtectionRepository.Result.SUCCESS) {
            return;
        }

        sender.send(
            player,
            protected_.toString(new Parser.StringVariable().put("block", block.getType()).put("type", protection.getType()))
        );
    }

    private boolean isRelatedChest(Block block) {
        if (!ChestNormalizer.isChest(block)) {
            return false;
        }
        for (BlockFace blockFace : new BlockFace[]{BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST}) {
            Block relative = block.getRelative(blockFace);
            if (relative.getType() == block.getType()) {
                return true;
            }
        }
        return false;
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
