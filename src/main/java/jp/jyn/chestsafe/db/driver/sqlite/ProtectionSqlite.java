package jp.jyn.chestsafe.db.driver.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.chestsafe.db.driver.ProtectionDriver;

import java.sql.Connection;
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
}
