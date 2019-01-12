package jp.jyn.chestsafe.protection;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnsavedProtection implements Protection {
    public static Protection newInstance() {
        return new ProtectionProxy(new UnsavedProtection());
    }

    private UnsavedProtection() {

    }

    private Type type = Type.PRIVATE;
    private UUID owner;
    private final Set<UUID> members = new HashSet<>();
    private final Map<Flag, Boolean> flags = new EnumMap<>(Flag.class);

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
        return members;
    }

    @Override
    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    @Override
    public boolean hasMember() {
        return !members.isEmpty();
    }

    @Override
    public Map<Flag, Boolean> getFlags() {
        return Collections.unmodifiableMap(flags);
    }

    @Override
    public Optional<Boolean> getFlag(Flag flag) {
        return Optional.ofNullable(flags.get(flag));
    }

    @Override
    public boolean hasFlag() {
        return !flags.isEmpty();
    }

    @Override
    public Protection setType(Type type) {
        this.type = type;
        return this;
    }

    @Override
    public Protection setOwner(UUID owner) {
        this.owner = owner;
        return this;
    }

    @Override
    public Protection addMember(UUID member) {
        members.add(member);
        return this;
    }

    @Override
    public Protection addMembers(Collection<UUID> members) {
        this.members.addAll(members);
        return this;
    }

    @Override
    public Protection removeMember(UUID member) {
        members.remove(member);
        return this;
    }

    @Override
    public Protection removeMembers(Collection<UUID> members) {
        this.members.removeAll(members);
        return this;
    }

    @Override
    public Protection clearMembers() {
        members.clear();
        return this;
    }

    @Override
    public Protection setFlag(Flag flag, boolean value) {
        flags.put(flag, value);
        return this;
    }

    @Override
    public Protection removeFlag(Flag flag) {
        flags.remove(flag);
        return this;
    }

    @Override
    public Protection clearFlags() {
        flags.clear();
        return this;
    }

    @Override
    public String toString() {
        return "UnsavedProtection{" +
            "type=" + type.name() + ", " +
            "owner=" + owner.toString() + ", " +
            "members=" + members.stream().map(UUID::toString).collect(Collectors.joining(",", "[", "]")) + ", " +
            "flags=" + flags.entrySet().stream().map(flag -> flag.getKey() + "=" + flag.getValue()).collect(Collectors.joining(",", "{", "}")) +
            "}";
    }
}
