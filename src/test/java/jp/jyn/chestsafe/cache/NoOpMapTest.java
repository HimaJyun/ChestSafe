package jp.jyn.chestsafe.cache;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class NoOpMapTest {
    private final Map<Object, Object> m = NoOpMap.getInstance();

    private Object p() {
        Object key = new Object();
        m.put(key, new Object());
        return key;
    }

    @Test
    public void getInstance() {
        assertNotNull(m);
        assertSame("NoOpMap is not Singleton", m, NoOpMap.getInstance());
    }

    @Test
    public void size() {
        assertEquals(m.size(), 0);
        p();
        assertEquals(m.size(), 0);
    }

    @Test
    public void isEmpty() {
        assertTrue(m.isEmpty());
        p();
        assertTrue(m.isEmpty());
    }

    @Test
    public void containsKey() {
        Object key = p();
        assertFalse(m.containsKey(key));
    }

    @Test
    public void containsValue() {
        Object value = new Object();
        m.put(new Object(), value);
        assertFalse(m.containsValue(value));
    }

    @Test
    public void get() {
        Object key = p();
        assertNull(m.get(key));
    }

    @Test
    public void put() {
        p();
    }

    @Test
    public void remove() {
        Object key = p();
        assertNull(m.remove(key));
    }

    @Test
    public void putAll() {
        Object key = new Object();

        m.putAll(new HashMap<Object, Object>() {{
            put(key, new Object());
        }});
        assertNull(m.get(key));
    }

    @Test
    public void keySet() {
        assertTrue(m.keySet().isEmpty());
    }

    @Test
    public void values() {
        assertTrue(m.values().isEmpty());
    }

    @Test
    public void entrySet() {
        assertTrue(m.entrySet().isEmpty());
    }

    @Test
    public void getOrDefault() {
        Object key = p();
        Object def = new Object();
        assertSame(m.getOrDefault(key, def), def);
    }

    @Test
    public void forEach() {
        m.forEach((k, v) -> fail());
    }

    @Test
    public void replaceAll() {
        m.replaceAll((k, v) -> {
            fail();
            return new Object();
        });
    }

    @Test
    public void putIfAbsent() {
        assertNull(m.putIfAbsent(new Object(), new Object()));
    }

    @Test
    public void remove1() {
        Object k = new Object();
        Object v = new Object();
        m.put(k, v);
        assertFalse(m.remove(k, v));
    }

    @Test
    public void replace() {
        Object k = new Object();
        Object v = new Object();
        m.put(k, v);
        assertFalse(m.replace(k, v, new Object()));
    }

    @Test
    public void replace1() {
        Object k = p();
        assertNull(m.replace(k, new Object()));
    }

    @Test
    public void computeIfAbsent() {
        Object v = new Object();
        assertSame(m.computeIfAbsent(new Object(), k -> v), v);
    }

    @Test
    public void computeIfPresent() {
        Object k = p();
        assertNull(m.computeIfPresent(k, (k2, v) -> new Object()));
    }

    @Test
    public void compute() {
        Object k = p();
        Object v = new Object();
        assertNull(m.compute(k, (k2, v2) -> {
            assertNull(v2);
            return null;
        }));
        assertSame(m.compute(k, (k2, v2) -> {
            assertNull(v2);
            return v;
        }), v);
    }

    @Test
    public void merge() {
        Object k = p();
        Object v = new Object();
        assertSame(m.merge(k, v, (k2, v2) -> {
            fail();
            return new Object();
        }), v);
    }
}
