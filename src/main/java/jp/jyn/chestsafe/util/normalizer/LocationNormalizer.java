package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.block.Block;

@FunctionalInterface
public interface LocationNormalizer {
    Location normalize(Block block);

    default Location normalize(Location location) {
        return normalize(location.getBlock());
    }
}
