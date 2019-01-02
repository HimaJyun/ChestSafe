package jp.jyn.chestsafe.cache;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class CacheFactoryTest {

    @Test
    public void createCache() {
        assertTrue(CacheFactory.INFINITY.create() instanceof HashMap);
        assertSame(CacheFactory.DISABLE.create(), NoOpMap.getInstance());
        assertTrue(new CacheFactory(1).create() instanceof LRUMap);
    }
}
