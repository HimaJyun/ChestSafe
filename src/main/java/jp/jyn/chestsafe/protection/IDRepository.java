package jp.jyn.chestsafe.protection;

import jp.jyn.chestsafe.config.MainConfig;
import jp.jyn.chestsafe.db.driver.IDDriver;
import jp.jyn.chestsafe.db.driver.IDDriver.IntLocation;
import jp.jyn.jbukkitlib.cache.CacheFactory;
import jp.jyn.jbukkitlib.util.PackagePrivate;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

@PackagePrivate
class IDRepository {
    private final IDDriver idDriver;

    private final Map<String, Integer> worldToIdCache = CacheFactory.Sized.INFINITY.create();
    private final Map<Integer, Optional<String>> idToWorldCache = CacheFactory.Sized.INFINITY.create();
    private final Map<Integer, UUID> idToUUIDCache;
    private final Map<UUID, Integer> UUIDToIdCache;
    private final Map<IntLocation, OptionalInt> locationToIdCache;
    private final Map<Integer, IntLocation> idToLocationCache;

    public IDRepository(MainConfig.CacheConfig cache, IDDriver idDriver) {
        this.idDriver = idDriver;

        idToUUIDCache = cache.id.create();
        UUIDToIdCache = cache.id.create();
        locationToIdCache = cache.location.create();
        idToLocationCache = cache.location.create();
    }

    public int add(IntLocation location) {
        int id = idDriver.add(location);
        // update cache
        idToLocationCache.put(id, location);
        locationToIdCache.put(location, OptionalInt.of(id));

        return id;
    }

    public void remove(int id) {
        // Cache needs to be deleted reliably.
        this.remove(id, idToLocation(id).orElse(null));
    }

    public void remove(int id, IntLocation location) {
        // remove cache
        if (location != null) {
            locationToIdCache.put(location, OptionalInt.empty()); // negative
        }
        idToLocationCache.remove(id);

        // remove protection(The related table is deleted with FOREIGN KEY)
        idDriver.remove(id);
    }

    public void move(int id, IntLocation location) {
        // get old location
        IntLocation oldLocation = idToLocation(id).orElse(null);
        if (location.equals(oldLocation)) {
            return;
        }

        // update cache
        idToLocationCache.put(id, location);
        locationToIdCache.put(location, OptionalInt.of(id));
        if (oldLocation != null) {
            locationToIdCache.put(oldLocation, OptionalInt.empty()); // negative
        }
        idDriver.move(id, location);
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public OptionalInt locationToId(Location location) {
        IntLocation intLocation = new IntLocation(worldToId(location.getWorld()), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        OptionalInt id = locationToIdCache.get(intLocation);
        if (id != null) {
            return id;
        }

        id = idDriver.locationToId(intLocation);
        locationToIdCache.put(intLocation, id);
        id.ifPresent(i -> idToLocationCache.put(i, intLocation));
        return id;
    }

    public Optional<IntLocation> idToLocation(int id) {
        IntLocation intLocation = idToLocationCache.get(id);
        if (intLocation != null) {
            return Optional.of(intLocation);
        }

        Optional<IntLocation> optional = idDriver.idToLocation(id);
        optional.ifPresent(location -> {
            idToLocationCache.put(id, location);
            locationToIdCache.put(location, OptionalInt.of(id));
        });

        return optional;
    }

    public int UUIDToId(UUID uuid) {
        Integer id = UUIDToIdCache.get(uuid);
        if (id != null) {
            return id;
        }

        id = idDriver.UUIDToId(uuid);
        UUIDToIdCache.put(uuid, id);
        idToUUIDCache.put(id, uuid);
        return id;
    }

    public UUID idToUUID(int id) {
        UUID uuid = idToUUIDCache.get(id);
        if (uuid != null) {
            return uuid;
        }

        Optional<UUID> uuidOptional = idDriver.idToUUID(id);
        if (uuidOptional.isPresent()) {
            uuid = uuidOptional.get();
            UUIDToIdCache.put(uuid, id);
            idToUUIDCache.put(id, uuid);
        }

        return uuid;
    }

    public int worldToId(World world) {
        String name = world.getName();
        Integer id = worldToIdCache.get(name);
        if (id != null) {
            return id;
        }

        id = idDriver.worldToId(name);
        worldToIdCache.put(name, id);
        idToWorldCache.put(id, Optional.of(name));
        return id;
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<String> idToWorld(int id) {
        Optional<String> name = idToWorldCache.get(id);
        if (name != null) {
            return name;
        }

        name = idDriver.idToWorld(id);
        idToWorldCache.put(id, name);
        name.ifPresent(world -> worldToIdCache.put(world, id));
        return name;
    }

    public Collection<Integer> searchArea(int world, int x1, int y1, int z1, int x2, int y2, int z2) {
        return idDriver.searchArea(world, x1, y1, z1, x2, y2, z2);
    }

    public List<Map.Entry<Integer, IntLocation>> getProtections(int limit, int offsetId) {
        return idDriver.getProtections(limit, offsetId);
    }
}
