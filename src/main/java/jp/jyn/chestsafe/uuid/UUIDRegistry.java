package jp.jyn.chestsafe.uuid;

import jp.jyn.chestsafe.config.config.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UUIDRegistry {
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    private final Map<String, Optional<UUID>> nameToUUIDCache;
    private final Map<UUID, Optional<String>> uuidToNameCache;

    private final Plugin plugin;

    public UUIDRegistry(Plugin plugin, MainConfig.CacheConfig cache) {
        this.plugin = plugin;

        nameToUUIDCache = cache.uuid.create();
        uuidToNameCache = cache.uuid.create();
    }

    public void updateCache(UUID uuid, String name) {
        nameToUUIDCache.put(name.toLowerCase(Locale.ENGLISH), Optional.of(uuid));
        uuidToNameCache.put(uuid, Optional.of(name));
    }

    public void updateCache(Player player) {
        updateCache(player.getUniqueId(), player.getName());
    }

    // region getName
    @SuppressWarnings("OptionalAssignedToNull")
    private Optional<String> tryGetName(UUID uuid) {
        Optional<String> value = uuidToNameCache.get(uuid);
        if (value != null) {
            return value;
        }

        // Get online player(it is fast)
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            updateCache(player);
            return Optional.of(player.getName());
        }
        return null;
    }

    /**
     * Get player Name
     *
     * @param uuid Target UUID
     * @return name
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<String> getName(UUID uuid) {
        Optional<String> value = tryGetName(uuid);
        if (value != null) {
            return value;
        }

        // Use getOfflinePlayer only when using Sync API.
        // It is early if another plug-in has already inquired.
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer != null && offlinePlayer.getName() != null) {
            updateCache(uuid, offlinePlayer.getName());
            return Optional.of(offlinePlayer.getName());
        }

        // use Mojang API(slow)
        return ifPresentOrElse((new UUIDConverter.NameGetter(uuid)).callEx(),
            v -> updateCache(uuid, v),
            () -> uuidToNameCache.put(uuid, Optional.empty())
        );
    }

    /**
     * Get the name of the player without blocking the thread.
     *
     * @param uuid     Target UUID
     * @param callback Callback function
     * @param sync     If true is specified and it is running on another thread, it synchronizes with the main thread.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public void getNameAsync(final UUID uuid, Consumer<Optional<String>> callback, boolean sync) {
        Objects.requireNonNull(callback);

        Optional<String> value = tryGetName(uuid);
        if (value != null) {
            callback.accept(value);
            return;
        }

        // Since getOfflinePlayer may block the main thread, it is not used.

        // use Mojang API with threads.
        pool.submit(() -> {
            final Optional<String> result = (new UUIDConverter.NameGetter(uuid)).callEx();
            Runnable updateCache = () -> {
                if (result.isPresent()) {
                    updateCache(uuid, result.get());
                } else {
                    uuidToNameCache.put(uuid, Optional.empty());
                }
            };

            if (sync) {
                syncRun(() -> {
                    updateCache.run();
                    callback.accept(result);
                });
            } else {
                callback.accept(result);
                syncRun(updateCache);
            }
        });
    }

    /**
     * Get the name of the player without blocking the thread.<br>
     * The callback is executed on the main thread.
     * To execute asynchronously, use {@link jp.jyn.chestsafe.uuid.UUIDRegistry#getNameAsync(UUID, Consumer, boolean)}.
     *
     * @param uuid     Target UUID
     * @param callback Callback function
     */
    public void getNameAsync(UUID uuid, Consumer<Optional<String>> callback) {
        getNameAsync(uuid, callback, true);
    }
    // endregion

    // region getUUID
    @SuppressWarnings("OptionalAssignedToNull")
    private Optional<UUID> tryGetUUID(String name) {
        Optional<UUID> value = nameToUUIDCache.get(name.toLowerCase(Locale.ENGLISH));
        if (value != null) {
            return value;
        }

        // Get online player(it is fast)
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            updateCache(player);
            return Optional.of(player.getUniqueId());
        }
        return null;
    }

    /**
     * Get player UUID
     *
     * @param name Target name
     * @return UUID
     */
    @SuppressWarnings({"OptionalAssignedToNull", "deprecation"})
    public Optional<UUID> getUUID(String name) {
        Optional<UUID> value = tryGetUUID(name);
        if (value != null) {
            return value;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        if (offlinePlayer != null && offlinePlayer.getUniqueId() != null && offlinePlayer.getName() != null) {
            updateCache(offlinePlayer.getUniqueId(), offlinePlayer.getName());
            return Optional.of(offlinePlayer.getUniqueId());
        }

        // Note: If the user does not exist this request will fail and it will be slow.
        return ifPresentOrElse((new UUIDConverter.UUIDGetter(name)).callEx(),
            v -> updateCache(v.getValue(), v.getKey()),
            () -> nameToUUIDCache.put(name.toLowerCase(Locale.ENGLISH), Optional.empty())
        ).map(Map.Entry::getValue);
    }

    /**
     * Get the uuid of the player without blocking the thread.
     *
     * @param name     Target Name
     * @param callback Callback function
     * @param sync     If true is specified and it is running on another thread, it synchronizes with the main thread.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public void getUUIDAsync(String name, Consumer<Optional<UUID>> callback, boolean sync) {
        Objects.requireNonNull(callback);

        Optional<UUID> value = tryGetUUID(name);
        if (value != null) {
            callback.accept(value);
            return;
        }

        pool.submit(() -> {
            final Optional<Map.Entry<String, UUID>> result = (new UUIDConverter.UUIDGetter(name)).callEx();
            Runnable updateCache = () -> {
                if (result.isPresent()) {
                    updateCache(result.get().getValue(), result.get().getKey());
                } else {
                    nameToUUIDCache.put(name.toLowerCase(Locale.ENGLISH), Optional.empty());
                }
            };

            final Optional<UUID> uuid = result.map(Map.Entry::getValue);
            if (sync) {
                syncRun(() -> {
                    updateCache.run();
                    callback.accept(uuid);
                });
            } else {
                callback.accept(uuid);
                syncRun(updateCache);
            }
        });
    }

    /**
     * Get the uuid of the player without blocking the thread.<br>
     * The callback is executed on the main thread.
     * To execute asynchronously, use {@link jp.jyn.chestsafe.uuid.UUIDRegistry#getUUIDAsync(String, Consumer, boolean)}.
     *
     * @param name     Target UUID
     * @param callback Callback function
     */
    public void getUUIDAsync(String name, Consumer<Optional<UUID>> callback) {
        getUUIDAsync(name, callback, true);
    }

    // endregion
    // region multiple

    /**
     * Get the uuid of the multiple player without blocking the thread.<br>
     * The callback is executed on the main thread.
     * To execute asynchronously, use {@link jp.jyn.chestsafe.uuid.UUIDRegistry#getMultipleUUIDAsync(Collection, Consumer, boolean)}.
     *
     * @param names    Target Names
     * @param callback Callback function
     */
    public void getMultipleUUIDAsync(Collection<String> names, Consumer<Map<String, UUID>> callback) {
        getMultipleUUIDAsync(names, callback, true);
    }

    /**
     * Get the uuid of the multiple player without blocking the thread.
     *
     * @param names    Target Names
     * @param callback Callback function
     * @param sync     If true is specified and it is running on another thread, it synchronizes with the main thread.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public void getMultipleUUIDAsync(Collection<String> names, Consumer<Map<String, UUID>> callback, boolean sync) {
        Objects.requireNonNull(callback);

        Map<String, UUID> result = new HashMap<>((names.size() * 4) / 3);
        Map<String, String> request = new HashMap<>();

        for (String name : names) {
            Optional<UUID> uuid = tryGetUUID(name);
            if (uuid == null) {
                request.put(name.toLowerCase(Locale.ENGLISH), name);
            } else {
                uuid.ifPresent(u -> result.put(name, u));
            }
        }

        if (request.isEmpty()) {
            callback.accept(result);
            return;
        }

        pool.submit(() -> {
            Map<String, UUID> uuid = new UUIDConverter.MultipleUUIDGetter(request.keySet()).callEx();

            // "Right name" is likely to be different from "requested name". (upper case or lower case, etc.)
            // Since we do not know the "Right name" from the code that called this method,
            //   we need to create a mapping of "requested name" and UUID.
            Map<String, Optional<UUID>> cache = new HashMap<>((request.size() * 4) / 3);
            for (Map.Entry<String, UUID> entry : uuid.entrySet()) {
                String nameL = entry.getKey().toLowerCase(Locale.ENGLISH);
                String requestName = request.remove(nameL);
                if (requestName != null) {
                    result.put(requestName, entry.getValue());
                }

                cache.put(entry.getKey(), Optional.of(entry.getValue()));
            }
            // UUIDs that are missing (do not exist) are negative cached
            for (String name : request.keySet()) {
                cache.put(name, Optional.empty());
            }
            Runnable updateCache = () -> {
                for (Map.Entry<String, Optional<UUID>> entry : cache.entrySet()) {
                    if (entry.getValue().isPresent()) {
                        updateCache(entry.getValue().get(), entry.getKey());
                    } else {
                        nameToUUIDCache.put(entry.getKey().toLowerCase(Locale.ENGLISH), Optional.empty());
                    }
                }
            };

            if (sync) {
                syncRun(() -> {
                    updateCache.run();
                    callback.accept(result);
                });
            } else {
                callback.accept(result);
                syncRun(updateCache);
            }
        });
    }

    /**
     * Get the name of the multiple player without blocking the thread.<br>
     * The callback is executed on the main thread.
     * To execute asynchronously, use {@link jp.jyn.chestsafe.uuid.UUIDRegistry#getMultipleNameAsync(Collection, Consumer, boolean)}.
     *
     * @param uuids    target UUIDs
     * @param callback Callback function
     */
    public void getMultipleNameAsync(Collection<UUID> uuids, Consumer<Map<UUID, String>> callback) {
        getMultipleNameAsync(uuids, callback, true);
    }

    /**
     * Get the name of the multiple player without blocking the thread.
     *
     * @param uuids    Target UUIDs
     * @param callback Callback function
     * @param sync     If true is specified and it is running on another thread, it synchronizes with the main thread.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public void getMultipleNameAsync(Collection<UUID> uuids, Consumer<Map<UUID, String>> callback, boolean sync) {
        Objects.requireNonNull(callback);

        Map<UUID, String> result = new HashMap<>((uuids.size() * 4) / 3);
        Set<UUID> request = new HashSet<>();
        for (UUID uuid : uuids) {
            Optional<String> name = tryGetName(uuid);
            if (name == null) {
                request.add(uuid);
            } else {
                name.ifPresent(n -> result.put(uuid, n));
            }
        }

        if (request.isEmpty()) {
            callback.accept(result);
            return;
        }

        pool.submit(() -> {
            Map<UUID, Optional<String>> cache = new HashMap<>((request.size() * 4) / 3);
            Runnable updateCache = () -> {
                for (Map.Entry<UUID, Optional<String>> value : cache.entrySet()) {
                    if (value.getValue().isPresent()) {
                        updateCache(value.getKey(), value.getValue().get());
                    } else {
                        uuidToNameCache.put(value.getKey(), Optional.empty());
                    }
                }
            };

            for (UUID uuid : request) {
                final Optional<String> name = (new UUIDConverter.NameGetter(uuid)).callEx();
                cache.put(uuid, name);
                name.ifPresent(n -> result.put(uuid, n));
            }

            if (sync) {
                syncRun(() -> {
                    updateCache.run();
                    callback.accept(result);
                });
            } else {
                callback.accept(result);
                syncRun(updateCache);
            }
        });
    }
    // endregion

    private void syncRun(Runnable runnable) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T> Optional<T> ifPresentOrElse(Optional<T> optional, Consumer<? super T> action, Runnable emptyAction) {
        if (optional.isPresent()) {
            action.accept(optional.get());
        } else {
            emptyAction.run();
        }
        return optional;
    }
}
