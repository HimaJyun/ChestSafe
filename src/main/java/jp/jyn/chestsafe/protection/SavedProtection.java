package jp.jyn.chestsafe.protection;

import jp.jyn.chestsafe.db.driver.ProtectionDriver;
import jp.jyn.jbukkitlib.util.Lazy;
import jp.jyn.jbukkitlib.util.PackagePrivate;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SavedProtection implements Protection {
    public final int id;

    private final ProtectionDriver protectionDriver;
    private final IDRepository idRepository;

    private Type type;
    private UUID owner;
    private final Lazy<Set<UUID>> members;
    private final Lazy<Map<Flag, Boolean>> flags;

    @PackagePrivate
    SavedProtection(int id, ProtectionDriver protectionDriver, IDRepository idRepository, Protection protection) {
        this.id = id;
        this.protectionDriver = protectionDriver;
        this.idRepository = idRepository;

        type = protection.getType();
        owner = protection.getOwner();

        members = new Lazy.Simple<>(HashSet::new);
        members.get().addAll(protection.getMembers());
        flags = new Lazy.Simple<>(() -> new EnumMap<>(Flag.class));
        protection.getFlags().forEach((key, value) -> flags.get().put(key, value));
    }

    @PackagePrivate
    SavedProtection(int id, ProtectionDriver protectionDriver, IDRepository idRepository, ProtectionDriver.ProtectionInfo info) {
        this.id = id;
        this.protectionDriver = protectionDriver;
        this.idRepository = idRepository;

        type = Type.valueOf(info.type);
        owner = Objects.requireNonNull(idRepository.idToUUID(info.owner));

        members = new Lazy.Simple<>(info.hasMember ? this::loadMembers : HashSet::new);
        flags = new Lazy.Simple<>(info.hasFlag ? this::loadFlags : () -> new EnumMap<>(Flag.class));
    }

    // region lazyload
    private Set<UUID> loadMembers() {
        return protectionDriver.getMembers(this.id)
            .stream()
            .map(idRepository::idToUUID)
            .collect(Collectors.toCollection(HashSet::new));
    }

    private Map<Flag, Boolean> loadFlags() {
        return protectionDriver.getFlags(this.id)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                v -> Flag.valueOf(v.getKey()),
                Map.Entry::getValue,
                (v1, v2) -> v1,
                () -> new EnumMap<>(Flag.class)
            ));
    }
    // endregion

    private void update() {
        protectionDriver.updateProtection(id, idRepository.UUIDToId(owner), type.id, hasMember(), hasFlag());
    }

    @Override
    public Protection setType(Type type) {
        if (this.type != type) {
            this.type = type;
            update();
        }
        return this;
    }

    @Override
    public Protection setOwner(UUID owner) {
        if (!this.owner.equals(owner)) {
            this.owner = owner;
            update();
        }
        return this;
    }

    @Override
    public Protection addMember(UUID member) {
        if (members.get().contains(member)) {
            return this;
        }

        boolean modify = !hasMember();

        protectionDriver.addMember(id, idRepository.UUIDToId(member));
        members.get().add(member);

        if (modify) {
            update();
        }
        return this;
    }

    @Override
    public Protection addMembers(Collection<UUID> members) {
        boolean modify = !hasMember();

        List<Integer> ids = members.stream()
            .filter(uuid -> !this.members.get().contains(uuid))
            .map(idRepository::UUIDToId)
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return this;
        }

        protectionDriver.addMembers(id, ids);
        this.members.get().addAll(members);

        if (modify) {
            update();
        }
        return null;
    }

    @Override
    public Protection removeMember(UUID member) {
        if (members.get().contains(member)) {
            protectionDriver.removeMember(id, idRepository.UUIDToId(member));
            members.get().remove(member);

            if (!hasMember()) {
                update();
            }
        }

        return this;
    }

    @Override
    public Protection removeMembers(Collection<UUID> members) {
        if (!hasMember()) {
            return this;
        }

        List<Integer> ids = members.stream()
            .filter(uuid -> this.members.get().contains(uuid))
            .map(idRepository::UUIDToId)
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return this;
        }

        protectionDriver.removeMembers(id, ids);
        this.members.get().removeAll(members);

        if (!hasMember()) {
            update();
        }
        return this;
    }

    @Override
    public Protection clearMembers() {
        if (hasMember()) {
            protectionDriver.clearMembers(id);
            members.get().clear();
            update();
        }

        return this;
    }

    @Override
    public Protection setFlag(Flag flag, boolean value) {
        Boolean current = flags.get().get(flag);
        if (current != null && current == value) {
            return this;
        }

        boolean modify = !hasFlag();

        protectionDriver.setFlag(id, flag.id, value);
        flags.get().put(flag, value);

        if (modify) {
            update();
        }
        return this;
    }

    @Override
    public Protection removeFlag(Flag flag) {
        if (flags.get().containsKey(flag)) {
            protectionDriver.removeFlag(id, flag.id);
            flags.get().remove(flag);

            if (!hasFlag()) {
                update();
            }
        }

        return this;
    }

    @Override
    public Protection clearFlags() {
        if (hasFlag()) {
            protectionDriver.clearFlags(id);
            flags.get().clear();
            update();
        }

        return this;
    }

    // region getter
    @Override
    public Type getType() {
        return type;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members.get());
    }

    @Override
    public boolean isMember(UUID player) {
        return members.get().contains(player);
    }

    @Override
    public boolean hasMember() {
        return !members.get().isEmpty();
    }

    @Override
    public Map<Flag, Boolean> getFlags() {
        return Collections.unmodifiableMap(flags.get());
    }

    @Override
    public Optional<Boolean> getFlag(Flag flag) {
        return Optional.ofNullable(flags.get().get(flag));
    }

    @Override
    public boolean hasFlag() {
        return !flags.get().isEmpty();
    }
    // endregion

    @Override
    public String toString() {
        return "SavedProtection{" +
            "id=" + id + ", " +
            "type=" + type.name() + ", " +
            "owner=" + owner.toString() + ", " +
            "members=" + members.get().stream().map(UUID::toString).collect(Collectors.joining(",", "[", "]")) + ", " +
            "flags=" + flags.get().entrySet().stream().map(flag -> flag.getKey() + "=" + flag.getValue()).collect(Collectors.joining(",", "{", "}")) +
            "}";
    }
}
