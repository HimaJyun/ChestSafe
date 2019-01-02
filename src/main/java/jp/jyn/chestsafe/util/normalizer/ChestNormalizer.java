package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Directional;

import java.util.Optional;

public class ChestNormalizer implements LocationNormalizer {

    private static final ChestNormalizer instance = new ChestNormalizer();

    private ChestNormalizer() { }

    public static ChestNormalizer getInstance() {
        return instance;
    }

    @Override
    public Location normalize(Block block) {
        Optional<Block> pair = searchDoubleChestPair(block);
        if (!pair.isPresent()) {
            return block.getLocation();
        }

        Block dc = pair.get();
        Location l1 = dc.getLocation();
        //Location l1 = searchDoubleChestPair(block).map(Block::getLocation).orElse(block.getLocation());
        Location l2 = block.getLocation();

        int x = Math.min(l1.getBlockX(), l2.getBlockX());
        int z = Math.min(l1.getBlockZ(), l2.getBlockZ());

        return new Location(l1.getWorld(), x, l1.getBlockY(), z);
    }

    public static boolean isChest(Block block) {
        return isChest(block.getType());
    }

    public static boolean isChest(Material type) {
        return (type == Material.CHEST || type == Material.TRAPPED_CHEST);
    }

    public static boolean isDoubleChest(Block block) {
        if (!isChest(block)) {
            return false;
        }
        return isDoubleChest((Chest) block.getState());
    }

    public static boolean isDoubleChest(Chest chest) {
        // Single Chest -> getBlockInventory == getInventory
        // Double Chest -> getBlockInventory != getInventory
        return (!chest.getBlockInventory().equals(chest.getInventory()));

        // Alternate
        //return !(chest.getInventory() instanceof DoubleChestInventory);
    }

    public static Optional<Block> searchDoubleChestPair(Block chest) {
        // not chest
        if (!isChest(chest)) {
            return Optional.empty();
        }

        Chest c = (Chest) chest.getState();
        // not double chest
        if (!isDoubleChest(c)) {
            return Optional.empty();
        }

        DoubleChest dc = (DoubleChest) c.getInventory().getHolder();
        InventoryHolder self = c.getBlockInventory().getHolder();
        boolean isLeft = self.equals(dc.getLeftSide());

        /*
                N
            W       E
                S
         */
        BlockFace face = ((Directional) c.getData()).getFacing();
        BlockFace target;
        switch (face) {
            case NORTH:
            case SOUTH:
                target = isLeft ? BlockFace.EAST : BlockFace.WEST;
                break;
            case WEST:
            case EAST:
                target = isLeft ? BlockFace.SOUTH : BlockFace.NORTH;
                break;
            default:
                // あり得ない(チェストは東西南北のどちらかしか向かない)
                throw new IllegalArgumentException("Not Chest");
        }

        // 特定した方角に相方のチェストが居る
        return Optional.of(chest.getRelative(target));
    }
}
