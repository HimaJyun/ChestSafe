package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.Door;

public class DoorNormalizer implements LocationNormalizer {

    private static final DoorNormalizer instance = new DoorNormalizer();
    private DoorNormalizer() { }
    public static DoorNormalizer getInstance() {
        return instance;
    }

    @Override
    public Location normalize(Block block) {
        if (!isDoor(block)) {
            return block.getLocation();
        }

        Door door = (Door) block.getState().getData();
        if (!door.isTopHalf()) {
            return block.getLocation();
        }

        Location loc = block.getLocation().clone();
        loc.setY(loc.getY() - 1);

        Block b2 = loc.getBlock();
        if (!isDoor(b2) || ((Door) b2.getState().getData()).isTopHalf()) {
            // ?!
            return block.getLocation();
        }

        return loc;
    }

    public static boolean isDoor(Block block) {
        return isDoor(block.getType());
    }

    public static boolean isDoor(Material type) {
        switch (type) {
            case DARK_OAK_DOOR:
            case ACACIA_DOOR:
            case BIRCH_DOOR:
            case JUNGLE_DOOR:
            case SPRUCE_DOOR:
            case WOODEN_DOOR:
            case IRON_DOOR_BLOCK:
            case WOOD_DOOR:
                return true;
            default:
                return false;
        }
    }
}
