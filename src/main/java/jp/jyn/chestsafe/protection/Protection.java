package jp.jyn.chestsafe.protection;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface Protection {
    // getter

    /**
     * Get protection type
     *
     * @return type
     */
    Type getType();

    /**
     * Get protection owner
     *
     * @return owner
     */
    UUID getOwner();

    /**
     * Get protection members
     *
     * @return get all members
     */
    Set<UUID> getMembers();

    /**
     * <p>Get protection flags.</p>
     * <p>Note: Map is unmodifiable</p>
     *
     * @return get all flags
     */
    Map<Flag, Boolean> getFlags();

    // setter

    /**
     * Set protection type
     *
     * @param type new type
     * @return for method chain
     */
    Protection setType(Type type);

    /**
     * Set protection owner
     *
     * @param owner new owner
     * @return for method chain
     */
    Protection setOwner(UUID owner);

    /**
     * Set protection owner
     *
     * @param owner new owner
     * @return for method chain
     */
    default Protection setOwner(Player owner) {
        return setOwner(owner.getUniqueId());
    }

    /**
     * Add protection member
     *
     * @param member new member
     * @return for method chain
     */
    Protection addMember(UUID member);

    /**
     * Add protection member
     *
     * @param member new member
     * @return for method chain
     */
    default Protection addMember(Player member) {
        return addMember(member.getUniqueId());
    }

    /**
     * Add protection members
     *
     * @param members new members
     * @return for method chain
     */
    default Protection addMembers(Collection<UUID> members) {
        for (UUID member : members) {
            addMember(member);
        }
        return this;
    }

    /**
     * Remove protection member
     *
     * @param member remove member
     * @return for method chain
     */
    Protection removeMember(UUID member);

    /**
     * Remove protection member
     *
     * @param member remove member
     * @return for method chain
     */
    default Protection removeMember(Player member) {
        return removeMember(member.getUniqueId());
    }

    /**
     * Remove protection members
     *
     * @param members remove members
     * @return for method chain
     */
    default Protection removeMembers(Collection<UUID> members) {
        for (UUID member : members) {
            removeMember(member);
        }
        return this;
    }

    /**
     * Clear protection members
     *
     * @return for method chain
     */
    Protection clearMembers();

    /**
     * Set protection flag
     *
     * @param flag  flag
     * @param value flag value
     * @return for method chain
     */
    Protection setFlag(Flag flag, boolean value);

    /**
     * Remove protection flag
     *
     * @param flag flag
     * @return for method chain
     */
    Protection removeFlag(Flag flag);

    /**
     * Clear protection flags
     *
     * @return flag
     */
    Protection clearFlags();

    // functions

    /**
     * Is it owner of protection?
     *
     * @param player target
     * @return result
     */
    default boolean isOwner(UUID player) {
        return getOwner().equals(player);
    }

    /**
     * Is it owner of protection?
     *
     * @param player target
     * @return result
     */
    default boolean isOwner(Player player) {
        return isOwner(player.getUniqueId());
    }

    /**
     * Is it included in the member
     *
     * @param player target
     * @return result
     */
    boolean isMember(UUID player);

    /**
     * Is it included in the member
     *
     * @param player target
     * @return result
     */
    default boolean isMember(Player player) {
        return isMember(player.getUniqueId());
    }

    /**
     * have members?
     *
     * @return result
     */
    boolean hasMember();

    /**
     * Get flag value
     *
     * @param flag flag
     * @return Flag value
     */
    Optional<Boolean> getFlag(Flag flag);

    /**
     * have flag
     *
     * @return result
     */
    boolean hasFlag();

    /**
     * True for owner or member
     *
     * @param player target player
     * @return true for owner or member
     */
    default boolean isAvailable(UUID player) {
        return isOwner(player) || isMember(player);
    }

    /**
     * True for owner or member
     *
     * @param player target player
     * @return true for owner or member
     */
    default boolean isAvailable(Player player) {
        return isAvailable(player.getUniqueId());
    }

    /**
     * Create a new protection.
     * This protection is not saved. To save, please call {@link jp.jyn.chestsafe.protection.ProtectionRepository#set(Protection, Block)}.
     *
     * @return Protection
     */
    static Protection newProtection() {
        return UnsavedProtection.newInstance();
    }

    enum Flag {
        HOPPER(0),
        EXPLOSION(1),
        FIRE(2),
        REDSTONE(3),
        MOB(4);

        public final int id;

        Flag(int id) {
            this.id = id;
        }

        private final static Map<Integer, Flag> byId = new HashMap<Integer, Flag>() {{
            for (Flag value : Flag.values()) {
                put(value.id, value);
            }
        }};

        public static Flag valueOf(Integer id) {
            Flag flag = byId.get(id);
            if (flag == null) {
                throw new IllegalArgumentException("ID not present");
            }
            return flag;
        }
    }

    enum Type {
        PRIVATE(0),
        PUBLIC(1);
        // It is not supported. It is an unnecessary function.
        // donate: It can be realized by hopper connecting private and public.
        // password: do you need it? In most cases it is enough for owner to add member.
        // However, it is not difficult to implement this function.
        // If there are "many requests", I may implement it.
        //PASSWORD(2),
        //DONATE(3);

        public final int id;

        Type(int id) {
            this.id = id;
        }

        private final static Map<Integer, Type> byId = new HashMap<Integer, Type>() {{
            for (Type value : Type.values()) {
                put(value.id, value);
            }
        }};

        public static Type valueOf(Integer id) {
            Type type = byId.get(id);
            if (type == null) {
                throw new IllegalArgumentException("ID not present");
            }
            return type;
        }
    }
}
