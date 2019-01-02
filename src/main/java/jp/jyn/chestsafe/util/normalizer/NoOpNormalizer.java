package jp.jyn.chestsafe.util.normalizer;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class NoOpNormalizer implements LocationNormalizer {

    private static final NoOpNormalizer instance = new NoOpNormalizer();
    private NoOpNormalizer() { }
    public static NoOpNormalizer getInstance() {
        return instance;
    }

    @Override
    public Location normalize(Block block) {
        return block.getLocation();
    }
    @Override
    public Location normalize(Location location) {
        return location;
    }
}
