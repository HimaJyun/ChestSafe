package jp.jyn.chestsafe.protection;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A proxy for seamlessly switching between "unsaved protection" and "saved protection"
 */
public class ProtectionProxy implements Protection {
    private Protection real;

    public ProtectionProxy(Protection real) {
        if (real instanceof ProtectionProxy) {
            throw new IllegalArgumentException("ProtectionProxy like onion.");
        }

        this.real = real;
    }

    public void setReal(Protection real) {
        this.real = real;
    }

    public Protection getReal() {
        return real;
    }

    @Override
    public Type getType() {
        return real.getType();
    }

    @Override
    public UUID getOwner() {
        return real.getOwner();
    }

    @Override
    public Set<UUID> getMembers() {
        return real.getMembers();
    }

    @Override
    public Map<Flag, Boolean> getFlags() {
        return real.getFlags();
    }

    @Override
    public Protection setType(Type type) {
        real.setType(type);
        return this;
    }

    @Override
    public Protection setOwner(UUID owner) {
        real.setOwner(owner);
        return this;
    }

    @Override
    public Protection addMember(UUID member) {
        real.addMember(member);
        return this;
    }

    @Override
    public Protection addMembers(Collection<UUID> members) {
        real.addMembers(members);
        return this;
    }

    @Override
    public Protection removeMember(UUID member) {
        real.removeMember(member);
        return this;
    }

    @Override
    public Protection removeMembers(Collection<UUID> members) {
        real.removeMembers(members);
        return this;
    }

    @Override
    public Protection clearMembers() {
        real.clearMembers();
        return this;
    }

    @Override
    public Protection setFlag(Flag flag, boolean value) {
        real.setFlag(flag, value);
        return this;
    }

    @Override
    public Protection removeFlag(Flag flag) {
        real.removeFlag(flag);
        return this;
    }

    @Override
    public Protection clearFlags() {
        real.clearFlags();
        return this;
    }

    @Override
    public boolean isMember(UUID player) {
        return real.isMember(player);
    }

    @Override
    public boolean hasMember() {
        return real.hasMember();
    }

    @Override
    public Optional<Boolean> getFlag(Flag flag) {
        return real.getFlag(flag);
    }

    @Override
    public boolean hasFlag() {
        return real.hasFlag();
    }

    @Override
    public String toString() {
        return "ProtectionProxy[" + real.toString() + "]";
    }
}
