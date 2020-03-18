package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class BedNormalizer implements LocationNormalizer {
    private static final Set<Material> BEDS = EnumSet.noneOf(Material.class);

    static {
        Arrays.stream(Material.values())
            .filter(m -> m.name().endsWith("_BED"))
            .filter(m -> !m.name().startsWith("LEGACY_"))
            .forEach(BEDS::add);
    }

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

        Bed bed = (Bed) block.getBlockData();
        if (bed.getPart() == Bed.Part.FOOT) {
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
        if (!isBed(b2) || ((Bed) b2.getBlockData()).getPart() == Bed.Part.HEAD) {
            // ?!
            return block.getLocation();
        }

        return b2.getLocation();
    }

    public static boolean isBed(Block block) {
        return isBed(block.getType());
    }

    public static boolean isBed(Material type) {
        return BEDS.contains(type);
    }

    public static Collection<Material> getBeds() {
        return BEDS;
    }
}
