package jp.jyn.chestsafe.listener;

import jp.jyn.chestsafe.config.MainConfig;
import jp.jyn.chestsafe.protection.Protection;
import jp.jyn.chestsafe.protection.ProtectionRepository;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class BlockListener implements Listener {
    private final Map<Material, MainConfig.ProtectionConfig> protectable;
    private final ProtectionRepository repository;

    public BlockListener(MainConfig config, ProtectionRepository repository) {
        this.protectable = config.protectable;
        this.repository = repository;
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

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        // notice: Occurs when the hopper attempts to transfer an item
        // That is, if the transfer is canceled, this event will occur indefinitely.
        Inventory hopper = e.getInitiator();
        Inventory block;
        if (e.getSource() == e.getInitiator()) {
            // hopper/dropper -> chest
            block = e.getDestination();
        } else /*if (e.getDestination() == e.getInitiator())*/ {
            // chest -> hopper
            block = e.getSource();
        }

        // chest first search(There are many cases where the chest is protected than the hopper)
        if (block.getLocation() != null &&
            checkFlag(block.getLocation().getBlock(), Protection.Flag.HOPPER)) {
            e.setCancelled(true);
            return;
        }

        if (checkFlag(hopper.getLocation().getBlock(), Protection.Flag.HOPPER)) {
            e.setCancelled(true);
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
