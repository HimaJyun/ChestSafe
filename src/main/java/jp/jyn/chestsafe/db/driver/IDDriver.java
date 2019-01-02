package jp.jyn.chestsafe.db.driver;

import com.zaxxer.hikari.HikariDataSource;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public abstract class IDDriver {
    private final HikariDataSource hikari;

    public IDDriver(HikariDataSource hikari) {
        this.hikari = hikari;
    }

    // ======== id_world ========
    // | id(int) | name(String) |
    // ==========================
    public int worldToId(String name) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `id` FROM `id_world` WHERE `name`=?"
             )) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            // create id
            try (PreparedStatement s2 = connection.prepareStatement(
                "INSERT INTO `id_world` (`name`) VALUES (?)"
            )) {
                s2.setString(1, name);
                s2.executeUpdate();
            }

            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Failed to assign world ID");
    }

    public Optional<String> idToWorld(int id) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `name` FROM `id_world` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(result.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    // ========= id_user ========
    // | id(int) | uuid(byte[]) |
    // ==========================
    public int UUIDToId(UUID uuid) {
        byte[] bytes = uuidToByte(uuid);

        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `id` FROM `id_user` WHERE `uuid`=?"
             )) {
            statement.setBytes(1, bytes);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            // create id
            try (PreparedStatement s2 = connection.prepareStatement(
                "INSERT INTO `id_user` (`uuid`) VALUES (?)"
            )) {
                s2.setBytes(1, bytes);
                s2.executeUpdate();
            }

            statement.setBytes(1, bytes);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Failed to assign user ID");
    }

    public Optional<UUID> idToUUID(int id) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `uuid` FROM `id_user` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(byteToUUID(result.getBytes(1)));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    private byte[] uuidToByte(UUID uuid) {
        return ByteBuffer.allocate(16)
            .putLong(uuid.getMostSignificantBits())
            .putLong(uuid.getLeastSignificantBits())
            .array();
    }

    private UUID byteToUUID(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long most = bb.getLong();
        long least = bb.getLong();
        return new UUID(most, least);
    }

    // ================== id_protection ==================
    // | id(int) | world(int) | x(int) | y(int) | z(int) |
    // ===================================================
    public OptionalInt locationToId(int world, int x, int y, int z) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `id` FROM `id_protection` WHERE `world`=? AND `x`=? AND `y`=? AND `z`=?"
             )) {
            statement.setInt(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return OptionalInt.of(result.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return OptionalInt.empty();
    }

    public OptionalInt locationToId(IntLocation location) {
        return locationToId(location.world, location.x, location.y, location.z);
    }

    public Optional<IntLocation> idToLocation(int id) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `world`,`x`,`y`,`z` FROM `id_protection` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(new IntLocation(
                        result.getInt("world"),
                        result.getInt("x"),
                        result.getInt("y"),
                        result.getInt("z")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public int add(int world, int x, int y, int z) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO `id_protection` (`world`,`x`,`y`,`z`) VALUES (?,?,?,?)"
             )) {
            statement.setInt(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.executeUpdate();

            OptionalInt id = locationToId(world, x, y, z);
            if (id.isPresent()) {
                return id.getAsInt();
            } else {
                throw new RuntimeException("Failed to assign protection ID");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int add(IntLocation location) {
        return add(location.world, location.x, location.y, location.z);
    }

    public void remove(int id) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM `id_protection` WHERE `id`=?"
             )) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void move(int id, int newWorld, int newX, int newY, int newZ) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE `id_protection` SET `world`=?, `x`=?, `y`=?, `z`=? WHERE `id`=?"
             )) {
            statement.setInt(1, newWorld);
            statement.setInt(2, newX);
            statement.setInt(3, newY);
            statement.setInt(4, newZ);
            statement.setInt(5, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void move(int id, IntLocation location) {
        move(id, location.world, location.x, location.y, location.z);
    }

    public Collection<Integer> searchArea(int world, int x1, int y1, int z1, int x2, int y2, int z2) {
        List<Integer> result = new ArrayList<>();
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `id` FROM `id_protection` WHERE `world`=? AND (`x` BETWEEN ? AND ?) AND (`y` BETWEEN ? AND ?) AND (`z` BETWEEN ? AND ?)"
             )) {
            statement.setInt(1, world);
            statement.setInt(2, minX);
            statement.setInt(3, maxX);
            statement.setInt(4, minY);
            statement.setInt(5, maxY);
            statement.setInt(6, minZ);
            statement.setInt(7, maxZ);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(resultSet.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public List<Map.Entry<Integer, IntLocation>> getProtections(int limit, int offsetId) {
        List<Map.Entry<Integer, IntLocation>> result = new ArrayList<>(limit);

        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT `id`,`world`,`x`,`y`,`z` FROM `id_protection` WHERE `id`>? ORDER BY `id` LIMIT ?"
                 // LIMIT OFFSET is slow.
             )) {
            statement.setInt(1, offsetId);
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new AbstractMap.SimpleEntry<>(
                        resultSet.getInt("id"),
                        new IntLocation(
                            resultSet.getInt("world"),
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z")
                        )
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static class IntLocation {
        public final int world;
        public final int x;
        public final int y;
        public final int z;

        public IntLocation(int world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntLocation that = (IntLocation) o;
            return world == that.world &&
                x == that.x &&
                y == that.y &&
                z == that.z;
        }

        @Override
        public int hashCode() {
            int result = world;

            result *= 31;
            result += x;

            result *= 31;
            result += y;

            result *= 31;
            result += z;

            return result;
        }
    }
}
