package jp.jyn.chestsafe.db.driver.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.chestsafe.db.driver.ProtectionDriver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class ProtectionSqlite extends ProtectionDriver {
    public ProtectionSqlite(HikariDataSource hikari) {
        super(hikari);

        try (Connection connection = hikari.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `protection_info` (" +
                    "   `id`    INTEGER NOT NULL PRIMARY KEY," +
                    "   `type`  INTEGER NOT NULL," +
                    "   `owner` INTEGER NOT NULL," +
                    "   `has_member` INTEGER NOT NULL DEFAULT 0," +
                    "   `has_flag`   INTEGER NOT NULL DEFAULT 0," +
                    "   FOREIGN KEY (`id`) REFERENCES `id_protection`(`id`)" +
                    "       ON DELETE CASCADE" +
                    ")"
            );

            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `protection_member` (" +
                    "   `id`     INTEGER NOT NULL," +
                    "   `member` INTEGER NOT NULL DEFAULT 0," +
                    "   FOREIGN KEY (`id`) REFERENCES `id_protection`(`id`)" +
                    "       ON DELETE CASCADE" +
                    ")"
            );
            statement.executeUpdate(
                "CREATE INDEX IF NOT EXISTS `protection_member_id` ON `protection_member`(`id`)"
            );

            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `protection_flag` (" +
                    "   `id`    INTEGER NOT NULL," +
                    "   `flag`  INTEGER NOT NULL," +
                    "   `value` INTEGER NOT NULL DEFAULT 0," +
                    "   FOREIGN KEY (`id`) REFERENCES `id_protection`(`id`)" +
                    "       ON DELETE CASCADE" +
                    ")"
            );
            statement.executeUpdate(
                "CREATE INDEX IF NOT EXISTS `protection_flag_id` ON `protection_flag`(`id`)"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setProtection(int id, int owner, byte type, boolean hasMember, boolean hasFlag) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT OR REPLACE INTO `protection_info` (`id`,`owner`,`type`,`has_member`,`has_flag`) VALUES (?,?,?,?,?)"
             )) {
            statement.setInt(1, id);
            statement.setInt(2, owner);
            statement.setByte(3, type);
            statement.setBoolean(4, hasMember);
            statement.setBoolean(5, hasFlag);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
