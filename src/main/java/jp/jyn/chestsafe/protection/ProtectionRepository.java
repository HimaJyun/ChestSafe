package jp.jyn.chestsafe.protection;

import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.db.DBConnector;
import jp.jyn.chestsafe.db.driver.IDDriver.IntLocation;
import jp.jyn.chestsafe.db.driver.ProtectionDriver;
import jp.jyn.chestsafe.db.driver.ProtectionDriver.ProtectionInfo;
import jp.jyn.chestsafe.util.normalizer.BedNormalizer;
import jp.jyn.chestsafe.util.normalizer.ChestNormalizer;
import jp.jyn.chestsafe.util.normalizer.DoorNormalizer;
import jp.jyn.chestsafe.util.normalizer.LocationNormalizer;
import jp.jyn.chestsafe.util.normalizer.NoOpNormalizer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

public class ProtectionRepository {

    public enum Result {
        /**
         * Successful completion.
         */
        SUCCESS,
        /**
         * Need to {@link ProtectionRepository#set(Protection, Block)} first.
         */
        NOT_SAVED,
        /**
         * Not protectable.
         */
        NOT_PROTECTABLE,
        /**
         * Already protected
         */
        ALREADY_PROTECTED
    }

    private final Map<Material, LocationNormalizer> normalizer = new EnumMap<>(Material.class);
    private final Set<Material> protectable = EnumSet.noneOf(Material.class);

    private final ProtectionDriver protectionDriver;
    private final IDRepository idRepository;

    private final Map<Integer, Protection> idToProtectionCache;

    public ProtectionRepository(MainConfig config, DBConnector db) {
        this.protectionDriver = db.protectionDriver;

        idRepository = new IDRepository(config.cache, db.idDriver);
        protectable.addAll(config.protectable.keySet());
        idToProtectionCache = config.cache.protection.create();

        // add normalizer
        normalizer.put(Material.CHEST, ChestNormalizer.getInstance());
        normalizer.put(Material.TRAPPED_CHEST, ChestNormalizer.getInstance());
        BedNormalizer.getBeds().forEach(material -> normalizer.put(material, BedNormalizer.getInstance()));
        DoorNormalizer.getDoors().forEach(material -> normalizer.put(material, DoorNormalizer.getInstance()));
    }

    /**
     * <p>Get protection</p>
     * <br>
     * <p>Warning:<br>
     * <b>Do not caching(reuse)</b> the acquired Protection.<br>
     * Protection may be deleted (by other plugins) without your knowledge.<br>
     * The result of the change to the deleted protection is undefined.<br>
     * (in many cases it is expected that an exception will occur or the change will not be preserved correctly)</p>
     *
     * @param block target block
     * @return protection
     */
    public Optional<Protection> get(Block block) {
        // Protectable check.
        if (!protectable.contains(block.getType())) {
            return Optional.empty();
        }

        // search protection id
        OptionalInt optionalInt = idRepository.locationToId(normalizeLocation(block));
        if (!optionalInt.isPresent()) {
            return Optional.empty();
        }
        int id = optionalInt.getAsInt();

        // search protection cache
        Protection protection = idToProtectionCache.get(id);
        if (protection != null) {
            return Optional.of(protection);
        }

        // db request
        Optional<ProtectionInfo> infoOptional = protectionDriver.getProtection(id);
        if (!infoOptional.isPresent()) {
            // Inconsistent
            idRepository.remove(id);
            return Optional.empty();
        }

        // update cache
        protection = new SavedProtection(id, protectionDriver, idRepository, infoOptional.get());
        idToProtectionCache.put(id, protection);
        return Optional.of(protection);
    }

    /**
     * <p>Add protection</p>
     *
     * @param block      protection block
     * @param protection protection
     * @return result
     */
    public Result set(Protection protection, Block block) {
        if (!protectable.contains(block.getType())) {
            return Result.NOT_PROTECTABLE;
        }

        Location location = normalizeLocation(block);
        // duplicate check
        if (idRepository.locationToId(location).isPresent()) {
            return Result.ALREADY_PROTECTED;
        }

        // add protection
        IntLocation intLocation = intLocation(location);
        int id = idRepository.add(intLocation);

        // set value
        int[] members = protection.getMembers().stream().mapToInt(idRepository::UUIDToId).toArray();
        Collection<Map.Entry<Byte, Boolean>> flags = protection.getFlags().stream()
            .map(entry -> new AbstractMap.SimpleEntry<>((byte) entry.getKey().id, entry.getValue()))
            .collect(Collectors.toSet());

        protectionDriver.add(
            id,
            idRepository.UUIDToId(protection.getOwner()),
            (byte) protection.getType().id,
            members,
            flags
        );
        // update cache
        Protection newProtection = new SavedProtection(id, protectionDriver, idRepository, protection);
        idToProtectionCache.put(id, newProtection);

        // Replace contents.
        if (protection instanceof ProtectionProxy) {
            ((ProtectionProxy) protection).setReal(newProtection);
        }

        return Result.SUCCESS;
    }

    /**
     * <p>Convert single chest to double chest.</p>
     * <p>Note: Must be called <b>after</b> becoming a double chest.</p>
     *
     * @param block Chest
     * @return True if protection exists, Otherwise false.
     */
    public boolean chestExpand(Block block) {
        if (!ChestNormalizer.isDoubleChest(block)) {
            return false;
        }

        Location normalized = ChestNormalizer.getInstance().normalize(block);
        if (idRepository.locationToId(normalized).isPresent()) {
            // normalized protection.
            return true;
        }

        Optional<Block> pair = ChestNormalizer.searchDoubleChestPair(block);
        if (!pair.isPresent()) {
            return false;
        }

        Block target;
        if (pair.get().getLocation().equals(normalized)) { // pair == normalized -> search argument block.
            target = block;
        } else { // pair != normalized -> search pair block.
            target = pair.get();
        }

        OptionalInt id = idRepository.locationToId(target.getLocation());
        if (id.isPresent()) {
            idRepository.move(id.getAsInt(), intLocation(normalized));
            return true;
        }
        return false;
    }

    /**
     * <p>Convert double chest to single chest</p>
     * <p>Note: Must be called <b>before</b> becoming a single chest.</p>
     *
     * @param block The remaining chest
     */
    public void chestShrink(Protection protection, Block block) {
        SavedProtection saved = castSaved(protection);
        if (saved == null) {
            return;
        }

        idRepository.move(saved.id, intLocation(block.getLocation()));
    }

    /**
     * Move protection
     *
     * @param protection target protection
     * @param moveTo     Protection move destination
     * @return result
     */
    public Result move(Protection protection, Block moveTo) {
        // unnecessary
        SavedProtection saved = castSaved(protection);
        if (saved == null) {
            return Result.NOT_SAVED;
        }

        // protectable
        if (!protectable.contains(moveTo.getType())) {
            return Result.NOT_PROTECTABLE;
        }

        Location location = normalizeLocation(moveTo);
        // duplicate check
        if (idRepository.locationToId(location).isPresent()) {
            return Result.ALREADY_PROTECTED;
        }

        idRepository.move(saved.id, intLocation(location));
        return Result.SUCCESS;
    }

    /**
     * remove protection
     *
     * @param protection protection
     * @return result
     */
    public Result remove(Protection protection) {
        SavedProtection p = castSaved(protection);
        if (p == null) {
            return Result.NOT_SAVED;
        }

        idRepository.remove(p.id);
        return Result.SUCCESS;
    }

    /**
     * Search for protection within area.
     *
     * @param world world
     * @param x1    x1
     * @param y1    y1
     * @param z1    z1
     * @param x2    z2
     * @param y2    y2
     * @param z2    z2
     * @return Protection within area.
     */
    public List<Protection> searchArea(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        int worldId = idRepository.worldToId(world);

        Collection<Integer> protections = idRepository.searchArea(worldId, x1, y1, z1, x2, y2, z2);
        List<Protection> result = new ArrayList<>(protections.size());

        for (Integer protection : protections) {
            Protection p = idToProtectionCache.computeIfAbsent(protection, id -> {
                Optional<ProtectionInfo> info = protectionDriver.getProtection(id);
                if (!info.isPresent()) {
                    idRepository.remove(id);
                    return null;
                }
                return new SavedProtection(id, protectionDriver, idRepository, info.get());
            });
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Search for protection within area.
     *
     * @param location1 location1
     * @param location2 location2
     * @return Protection within area.
     * @throws IllegalArgumentException In the case of Location of argument is different world
     */
    public List<Protection> searchArea(Location location1, Location location2) throws IllegalArgumentException {
        if (!location1.getWorld().equals(location2.getWorld())) {
            throw new IllegalArgumentException("Different world.");
        }

        return searchArea(
            location1.getWorld(),
            location1.getBlockX(),
            location1.getBlockY(),
            location1.getBlockZ(),
            location2.getBlockX(),
            location2.getBlockY(),
            location2.getBlockZ()
        );
    }

    /**
     * Protection cleanup
     *
     * @param limit    Number of protections to check
     * @param offsetId Paging offset
     * @param checker  Protection remove checker
     * @return offset id
     */
    public int cleanup(int limit, int offsetId, CleanupChecker checker) {
        Integer id = 0;
        // cache bypass search.
        for (Map.Entry<Integer, IntLocation> protection : idRepository.getProtections(limit, offsetId)) {
            id = protection.getKey();
            IntLocation location = protection.getValue();
            String world = idRepository.idToWorld(location.world).orElse(null);

            if (checker.remove(world, location.x, location.y, location.z)) {
                // remove
                idToProtectionCache.remove(id);
                idRepository.remove(id, location);
            }
        }
        return id;
    }

    @FunctionalInterface
    public interface CleanupChecker {
        /**
         * Decide whether to delete protection.
         *
         * @param world world name
         * @param x     x
         * @param y     y
         * @param z     z
         * @return If true, protection is deleted.
         */
        boolean remove(String world, int x, int y, int z);
    }

    private Location normalizeLocation(Block block) {
        return normalizer.getOrDefault(block.getType(), NoOpNormalizer.getInstance()).normalize(block);
    }

    private SavedProtection castSaved(Protection protection) {
        if (protection instanceof ProtectionProxy) {
            protection = ((ProtectionProxy) protection).getReal();
        }
        if (protection instanceof SavedProtection) {
            return (SavedProtection) protection;
        }
        return null;
    }

    private IntLocation intLocation(Location location) {
        return new IntLocation(
            idRepository.worldToId(location.getWorld()),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }
}
