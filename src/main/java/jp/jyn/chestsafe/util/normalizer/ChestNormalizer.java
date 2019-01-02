package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;

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

        Location l1 = pair.get().getLocation();
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
        return isDoubleChest((Chest) block.getBlockData());
    }

    public static boolean isDoubleChest(Chest chest) {
        return chest.getType() != Chest.Type.SINGLE;
    }

    public static Optional<Block> searchDoubleChestPair(Block block) {
        // not chest
        if (!isChest(block)) {
            return Optional.empty();
        }

        Chest chest = (Chest) block.getBlockData();
        // not double chest
        if (!isDoubleChest(chest)) {
            return Optional.empty();
        }

        /*
                N
            W       E
                S
         */
        BlockFace target =
            chest.getType() == Chest.Type.LEFT
                ? leftChest(chest.getFacing())
                : rightChest(chest.getFacing());

        // 特定した方角に相方のチェストが居る
        return Optional.of(block.getRelative(target));
    }

    private static BlockFace leftChest(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.EAST;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            case EAST:
                return BlockFace.SOUTH;
        }
        throw new IllegalArgumentException("Illegal BlockFace: " + face);
    }

    private static BlockFace rightChest(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.WEST;
            case SOUTH:
                return BlockFace.EAST;
            case WEST:
                return BlockFace.SOUTH;
            case EAST:
                return BlockFace.NORTH;
        }
        throw new IllegalArgumentException("Illegal BlockFace: " + face);
    }
}
