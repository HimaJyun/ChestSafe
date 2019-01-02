package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.Door;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class DoorNormalizer implements LocationNormalizer {

    private static final Set<Material> DOORS = EnumSet.of(
        Material.OAK_DOOR, Material.IRON_DOOR,
        Material.DARK_OAK_DOOR, Material.ACACIA_DOOR, Material.BIRCH_DOOR,
        Material.JUNGLE_DOOR, Material.SPRUCE_DOOR
    );

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
        return DOORS.contains(type);
    }

    public static Collection<Material> getDoors() {
        return DOORS;
    }
}
