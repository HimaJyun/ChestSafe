package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Bed;

public class BedNormalizer implements LocationNormalizer {

    private static final BedNormalizer instance = new BedNormalizer();
    private BedNormalizer() { }
    public static BedNormalizer getInstance() {
        return instance;
    }

    @Override
    public Location normalize(Block block) {
        if (!isBed(block)) {
            return block.getLocation();
        }

        Bed bed = (Bed) block.getState().getData();
        if (!bed.isHeadOfBed()) {
            return block.getLocation();
        }

        BlockFace target;
        switch (bed.getFacing()) {
            case SOUTH:
                target = BlockFace.NORTH;
                break;
            case NORTH:
                target = BlockFace.SOUTH;
                break;
            case WEST:
                target = BlockFace.EAST;
                break;
            case EAST:
                target = BlockFace.WEST;
                break;
            default:
                throw new IllegalArgumentException("Not Bed");
        }

        Block b2 = block.getRelative(target);
        if (!isBed(b2) || ((Bed) b2.getState().getData()).isHeadOfBed()) {
            // ?!
            return block.getLocation();
        }

        return b2.getLocation();
    }

    public static boolean isBed(Block block) {
        return isBed(block.getType());
    }

    public static boolean isBed(Material type) {
        return (type == Material.BED_BLOCK);
    }

}
