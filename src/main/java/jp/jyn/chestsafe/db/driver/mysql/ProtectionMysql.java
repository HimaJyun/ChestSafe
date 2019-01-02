package jp.jyn.chestsafe.db.driver.mysql;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.chestsafe.db.driver.ProtectionDriver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class ProtectionMysql extends ProtectionDriver {
    public ProtectionMysql(HikariDataSource hikari) {
        super(hikari);

        try (Connection connection = hikari.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `protection_info` (" +
                    "   `id`    INT     UNSIGNED NOT NULL PRIMARY KEY," +
                    "   `owner` INT     UNSIGNED NOT NULL," +
                    "   `type`  TINYINT UNSIGNED NOT NULL," +
                    "   `has_member` BIT(1)   NOT NULL DEFAULT b'0'," +
                    "   `has_flag`   BIT(1)   NOT NULL DEFAULT b'0'," +
                    "   FOREIGN KEY (`id`) REFERENCES `id_protection`(`id`)" +
                    "       ON DELETE CASCADE" +
                    ")"
            );

            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `protection_member` (" +
                    "   `id`     INT UNSIGNED NOT NULL," +
                    "   `member` INT UNSIGNED NOT NULL," +
                    "   INDEX (`id`)," +
                    "   FOREIGN KEY (`id`) REFERENCES `id_protection`(`id`)" +
                    "       ON DELETE CASCADE" +
                    ")"
            );

            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `protection_flag` (" +
                    "   `id`    INT     UNSIGNED NOT NULL," +
                    "   `flag`  TINYINT UNSIGNED NOT NULL," +
                    "   `value` BIT(1)        NOT NULL DEFAULT b'0'," +
                    "   INDEX (`id`)," +
                    "   FOREIGN KEY (`id`) REFERENCES `id_protection`(`id`)" +
                    "       ON DELETE CASCADE" +
                    ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setProtection(int id, int owner, byte type, boolean hasMember, boolean hasFlag) {
        try (Connection connection = hikari.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO `protection_info` (`id`,`owner`,`type`,`has_member`,`has_flag`) VALUES (?,?,?,?,?) " +
                     "  ON DUPLICATE KEY UPDATE " +
                     "`owner` = VALUES(`owner`), `type` = VALUES(`type`), `has_member` = VALUES(`has_member`), `has_flag` = VALUES(`has_flag`)"
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
