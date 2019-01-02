package jp.jyn.chestsafe.cache;

import java.util.HashMap;
import java.util.Map;

public class CacheFactory {
    public final static CacheFactory DISABLE = new CacheFactory(0);
    public final static CacheFactory INFINITY = new CacheFactory(-1);

    private final int size;

    public CacheFactory(int size) {
        this.size = size;
    }

    public <K, V> Map<K, V> create() {
        if (size < 0) {
            return new HashMap<>();
        } else if (size == 0) {
            return NoOpMap.getInstance();
        } else {
            return new LRUMap<>(size);
        }
    }
}
