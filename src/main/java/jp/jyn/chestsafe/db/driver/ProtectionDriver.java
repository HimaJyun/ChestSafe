package jp.jyn.chestsafe.db.driver;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.jbukkitlib.util.PackagePrivate;

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
    private final HikariDataSource hikari;

    protected ProtectionDriver(HikariDataSource hikari) {
        this.hikari = hikari;
    }

    // ============================== protection_info ===============================
    // | id(int) | owner(int) | type(int) | has_member(boolean) | has_flag(boolean) |
    // ==============================================================================

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
                        result.getInt("type"),
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
     * <p>Set protection</p>
     * <p>Note: This should be so as not to violate uniqueness.</p>
     *
     * @param id        Protection id
     * @param owner     Protection owner id
     * @param type      Protection type
     * @param hasMember Protection has members
     * @param hasFlag   Protection has flag
     */
    public void setProtection(int id, int owner, int type, boolean hasMember, boolean hasFlag) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO `protection_info` (`id`,`owner`,`type`,`has_member`,`has_flag`) VALUES (?,?,?,?,?)"
             )) {
            statement.setInt(1, id);
            statement.setInt(2, owner);
            statement.setByte(3, (byte) type);
            statement.setBoolean(4, hasMember);
            statement.setBoolean(5, hasFlag);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Set protection</p>
     * <p>Note: This should be so as not to violate uniqueness.</p>
     *
     * @param id         Protection id
     * @param protection Protection info
     */
    public void setProtection(int id, ProtectionInfo protection) {
        setProtection(id, protection.owner, protection.type, protection.hasMember, protection.hasFlag);
    }

    public void updateProtection(int id, int owner, int type, boolean hasMember, boolean hasFlag) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE `protection_info` " +
                     "  SET `owner`=?,`type`=?,`has_member`=?,`has_flag`=? " +
                     "WHERE `id`=?"
             )) {
            statement.setInt(1, owner);
            statement.setByte(2, (byte) type);
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
        public final int type;
        public final boolean hasMember;
        public final boolean hasFlag;

        @PackagePrivate
        ProtectionInfo(int owner, int type, boolean hasMember, boolean hasFlag) {
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

    // =========== protection_flag ============
    // | id(int) | flag(int) | value(boolean) |
    // ========================================
    public Map<Integer, Boolean> getFlags(int id) {
        Map<Integer, Boolean> map = new HashMap<>();

        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `flag`,`value` FROM `protection_flag` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    map.put(result.getInt("flag"), result.getBoolean("value"));
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

    public void setFlag(int id, int flag, boolean value) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO `protection_flag` (`id`,`flag`,`value`) VALUES (?,?,?)"
             )) {
            statement.setInt(1, id);
            statement.setByte(2, (byte) flag);
            statement.setBoolean(3, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFlag(int id, int flag) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM `protection_flag` WHERE `id`=? AND `flag`=?"
             )) {
            statement.setInt(1, id);
            statement.setByte(2, (byte) flag);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // Bulk operation

    /**
     * <p>Add new protection.</p>
     * <p>Note: This should be so as not to violate uniqueness.</p>
     *
     * @param id      protection id
     * @param owner   protection owner
     * @param type    protection type
     * @param members protection member
     * @param flags   protection flags
     */
    public void add(int id, int owner, int type, int[] members, Map<Integer, Boolean> flags) {
        try (Connection connection = hikari.getConnection()) {
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO `protection_info` (`id`,`owner`,`type`,`has_member`,`has_flag`) VALUES (?,?,?,?,?)"
                )) {
                    statement.setInt(1, id);
                    statement.setInt(2, owner);
                    statement.setByte(3, (byte) type);
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

                if (!flags.isEmpty()) {
                    try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO `protection_flag` (`id`,`flag`,`value`) VALUES (?,?,?)"
                    )) {
                        statement.setInt(1, id);
                        for (Map.Entry<Integer, Boolean> flag : flags.entrySet()) {
                            statement.setByte(2, flag.getKey().byteValue());
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
