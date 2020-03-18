package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class DoorNormalizer implements LocationNormalizer {
    private static final Set<Material> DOORS = EnumSet.noneOf(Material.class);

    static {
        Arrays.stream(Material.values())
            .filter(m -> m.name().endsWith("_DOOR"))
            .filter(m -> !m.name().startsWith("LEGACY_"))
            .forEach(DOORS::add);
    }

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

        Door door = (Door) block.getBlockData();
        if (door.getHalf() == Bisected.Half.BOTTOM) {
            return block.getLocation();
        }

        Location loc = block.getLocation().clone();
        loc.setY(loc.getY() - 1);

        Block b2 = loc.getBlock();
        if (!isDoor(b2) || ((Door) b2.getBlockData()).getHalf() == Bisected.Half.TOP) {
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
