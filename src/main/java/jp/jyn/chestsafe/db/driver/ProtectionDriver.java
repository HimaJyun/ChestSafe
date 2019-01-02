package jp.jyn.chestsafe.db.driver;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Protection CRUD
 */
public abstract class ProtectionDriver {
    protected final HikariDataSource hikari;

    public ProtectionDriver(HikariDataSource hikari) {
        this.hikari = hikari;
    }

    // =============================== protection_info ===============================
    // | id(int) | owner(int) | type(byte) | has_member(boolean) | has_flag(boolean) |
    // ===============================================================================

    /**
     * Get protection
     *
     * @param id Protection id
     * @return Obtained protection information
     */
    public Optional<ProtectionInfo> getProtection(int id) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `owner`,`type`,`has_member`,`has_flag` FROM `protection_info` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(new ProtectionInfo(
                        result.getInt("owner"),
                        result.getByte("type"),
                        result.getBoolean("has_member"),
                        result.getBoolean("has_flag")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    /**
     * Set protection
     * If you know that protection exists, use "updateProtection" (you can run with less SQL)
     *
     * @param id        Protection id
     * @param owner     Protection owner id
     * @param type      Protection type
     * @param hasMember Protection has members
     * @param hasFlag   Protection has flag
     */
    public abstract void setProtection(int id, int owner, byte type, boolean hasMember, boolean hasFlag);

    /**
     * Set protection
     * If you know that protection exists, use "updateProtection" (you can run with less SQL)
     *
     * @param id         Protection id
     * @param protection Protection info
     */
    public void setProtection(int id, ProtectionInfo protection) {
        setProtection(id, protection.owner, protection.type, protection.hasMember, protection.hasFlag);
    }

    public void updateProtection(int id, int owner, byte type, boolean hasMember, boolean hasFlag) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE `protection_info` " +
                     "  SET `owner`=?,`type`=?,`has_member`=?,`has_flag`=? " +
                     "WHERE `id`=?"
             )) {
            statement.setInt(1, owner);
            statement.setByte(2, type);
            statement.setBoolean(3, hasMember);
            statement.setBoolean(4, hasFlag);
            statement.setInt(5, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateProtection(int id, ProtectionInfo protection) {
        updateProtection(id, protection.owner, protection.type, protection.hasMember, protection.hasFlag);
    }

    public static class ProtectionInfo {
        public final int owner;
        public final byte type;
        public final boolean hasMember;
        public final boolean hasFlag;

        public ProtectionInfo(int owner, byte type, boolean hasMember, boolean hasFlag) {
            this.owner = owner;
            this.type = type;
            this.hasMember = hasMember;
            this.hasFlag = hasFlag;
        }
    }

    // === protection_member ===
    // | id(int) | member(int) |
    // =========================
    public Set<Integer> getMembers(int id) {
        Set<Integer> set = new HashSet<>();

        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `member` FROM `protection_member` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    set.add(result.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return set;
    }

    public void clearMembers(int id) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM `protection_member` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMember(int id, int member) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO `protection_member` (`id`,`member`) VALUES (?,?)"
             )) {
            statement.setInt(1, id);
            statement.setInt(2, member);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMembers(int id, Collection<Integer> members) {
        if (members.isEmpty()) {
            return;
        }

        try (Connection connection = hikari.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `protection_member` (`id`,`member`) VALUES (?,?)"
            )) {
                connection.setAutoCommit(false);
                statement.setInt(1, id);
                for (int member : members) {
                    statement.setInt(2, member);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeMember(int id, int member) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM `protection_member` WHERE `id`=? AND `member`=?"
             )) {
            statement.setInt(1, id);
            statement.setInt(2, member);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeMembers(int id, Collection<Integer> members) {
        if (members.isEmpty()) {
            return;
        }

        try (Connection connection = hikari.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM `protection_member` WHERE `id`=? AND `member`=?"
            )) {
                connection.setAutoCommit(false);
                statement.setInt(1, id);
                for (int member : members) {
                    statement.setInt(2, member);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ============ protection_flag ============
    // | id(int) | flag(byte) | value(boolean) |
    // =========================================
    public Map<Byte, Boolean> getFlags(int id) {
        Map<Byte, Boolean> map = new HashMap<>();

        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `flag`,`value` FROM `protection_flag` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    map.put(result.getByte("flag"), result.getBoolean("value"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public void clearFlags(int id) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM `protection_flag` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFlag(int id, byte flag, boolean value) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO `protection_flag` (`id`,`flag`,`value`) VALUES (?,?,?)"
             )) {
            statement.setInt(1, id);
            statement.setByte(2, flag);
            statement.setBoolean(3, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFlag(int id, byte flag) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM `protection_flag` WHERE `id`=? AND `flag`=?"
             )) {
            statement.setInt(1, id);
            statement.setByte(2, flag);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Bulk operation

    /**
     * Add new protection.<br>
     * Note: This should be so as not to violate uniqueness.
     *
     * @param id      protection id
     * @param owner   protection owner
     * @param type    protection type
     * @param members protection member
     * @param flags   protection flags
     */
    public void add(int id, int owner, byte type, int[] members, Collection<Map.Entry<Byte, Boolean>> flags) {
        try (Connection connection = hikari.getConnection()) {
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `protection_info` (`id`,`owner`,`type`,`has_member`,`has_flag`) VALUES (?,?,?,?,?)"
                )) {
                    statement.setInt(1, id);
                    statement.setInt(2, owner);
                    statement.setByte(3, type);
                    statement.setBoolean(4, members.length != 0);
                    statement.setBoolean(5, flags.size() != 0);
                    statement.executeUpdate();
                }

                if (members.length != 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO `protection_member` (`id`,`member`) VALUES (?,?)"
                    )) {
                        statement.setInt(1, id);
                        for (int member : members) {
                            statement.setInt(2, member);
                            statement.executeUpdate();
                        }
                    }
                }

                if (flags.size() != 0) {
                    try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO `protection_flag` (`id`,`flag`,`value`) VALUES (?,?,?)"
                    )) {
                        statement.setInt(1, id);
                        for (Map.Entry<Byte, Boolean> flag : flags) {
                            statement.setByte(2, flag.getKey());
                            statement.setBoolean(3, flag.getValue());
                            statement.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
