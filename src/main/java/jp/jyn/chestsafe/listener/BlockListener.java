package jp.jyn.chestsafe.listener;

import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.MainConfig;
import jp.jyn.chestsafe.config.MessageConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import jp.jyn.jbukkitlib.config.locale.BukkitLocale;
import jp.jyn.jbukkitlib.config.parser.component.ComponentVariable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public class BlockListener implements Listener {
    private final Plugin plugin;

    private final Map<Material, MainConfig.ProtectionConfig> protectable;
    private final ProtectionRepository repository;

    private final boolean hopperDestroy;
    private final BukkitLocale<MessageConfig> message;

    public BlockListener(MainConfig config, ProtectionRepository repository, BukkitLocale<MessageConfig> message) {
        this.plugin = ChestSafe.getInstance();
        this.protectable = config.protectable;
        this.repository = repository;
        this.hopperDestroy = config.hopperDestroy;
        this.message = message;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (checkFlag(e.getBlock(), Protection.Flag.MOB)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (checkFlag(e.getBlock(), Protection.Flag.FIRE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent e) {
        if (checkFlag(e.getBlock(), Protection.Flag.REDSTONE)) {
            e.setNewCurrent(e.getOldCurrent());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (checkFlag(block, Protection.Flag.EXPLOSION)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private Inventory previousHopperMove;

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        // notice: Occurs when the hopper attempts to transfer an item
        // That is, if the transfer is canceled, this event will occur indefinitely.
        Inventory hopper = e.getInitiator();
        Inventory chest;
        if (e.getSource() == e.getInitiator()) {
            // hopper/dropper -> chest
            chest = e.getDestination();
        } else /*if (e.getDestination() == e.getInitiator())*/ {
            // chest -> hopper
            chest = e.getSource();
        }

        // chest first search(There are many cases where the chest is protected than the hopper)
        Location chestLocation = chest.getLocation();
        if (chestLocation != null) {
            Block chestBlock = chestLocation.getBlock();
            Protection chestProtection = repository.get(chestBlock).orElse(null);

            if (denyTransfer(chestBlock, chestProtection)) {
                e.setCancelled(true);
                if (!hopperDestroy ||
                    hopper.equals(previousHopperMove)) { // many calls in chest -> hopper
                    return;
                }
                previousHopperMove = hopper;

                Location l = hopper.getLocation();
                if (l != null) {
                    Block b = l.getBlock();
                    if (repository.get(b).orElse(null) == null) {
                        destroy(b, chestBlock, chestProtection);
                    }
                }
                return;
            }
        }

        Location hopperLocation = hopper.getLocation();
        if (hopperLocation != null) {
            Block hopperBlock = hopperLocation.getBlock();
            Protection hopperProtection = repository.get(hopperBlock).orElse(null);

            if (denyTransfer(hopperBlock, hopperProtection)) {
                e.setCancelled(true);
            }
        }
    }

    private boolean denyTransfer(Block block, Protection protection) {
        if (protection == null || protection.getType() == Protection.Type.PUBLIC) {
            return false;
        }

        Optional<Boolean> f = protection.getFlag(Protection.Flag.HOPPER);
        //noinspection OptionalIsPresent
        return f.isPresent() ? !f.get() : !protectable.get(block.getType()).flag.get(Protection.Flag.HOPPER);
    }

    private void destroy(Block block, Block target, Protection protection) {
        // イベントの中で壊すとクラッシュする
        Bukkit.getScheduler().runTask(plugin, (Runnable) block::breakNaturally);
        Player player = Bukkit.getPlayer(protection.getOwner());
        if (player != null) {
            ComponentVariable variable = ComponentVariable.init();
            variable.put("world", target.getWorld().getName()).put("x", target.getX()).put("y", target.getY()).put("z", target.getZ());
            variable.put("from", block.getType().name()).put("to", target.getType().name());
            message.get(player).hopperDestroy.apply(variable).send(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent e) {
        Block block = e.getInventory().getLocation().getBlock();
        if (checkFlag(block, Protection.Flag.HOPPER)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent e) {
        if (checkPiston(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent e) {
        if (checkPiston(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    private boolean checkPiston(List<Block> blocks) {
        for (Block block : blocks) {
            MainConfig.ProtectionConfig config = protectable.get(block.getType());
            if (config == null) {
                continue;
            }

            Protection protection = getProtection(block);
            if (protection != null) {
                // There is no flag to allow piston movement.
                // It needs to move protection. (It is complicated to implement)
                return true;
            }
        }
        return false;
    }

    private boolean checkFlag(Block block, Protection.Flag flag) {
        MainConfig.ProtectionConfig config = protectable.get(block.getType());
        if (config == null) {
            return false;
        }

        Protection protection = getProtection(block);
        if (protection == null) {
            return false;
        }

        return !getFlag(protection, config, flag);
    }

    private Protection getProtection(Block block) {
        return repository.get(block).orElse(null);
    }

    private Boolean getFlag(Protection protection, MainConfig.ProtectionConfig config, Protection.Flag flag) {
        return protection.getFlag(flag).orElse(config.flag.get(flag));
    }
}
