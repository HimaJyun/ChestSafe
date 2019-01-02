package jp.jyn.chestsafe.db.driver.mysql;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.chestsafe.db.driver.IDDriver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class IDMysql extends IDDriver {

    public IDMysql(HikariDataSource hikari) {
        super(hikari);

        try (Connection connection = hikari.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_world` (" +
                "   `id`   INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT," +
                "   `name` VARCHAR(128) NOT NULL UNIQUE KEY" + // In order to use VARCHAR(255) with utf8mb4, additional setting becomes necessary
                ")"
            );
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_user` (" +
                "   `id`   INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT," +
                "   `uuid` BINARY(16)   NOT NULL UNIQUE KEY" +
                ")"
            );
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_protection` (" +
                "   `id`    INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT," +
                "   `world` INT UNSIGNED NOT NULL," +
                "   `x`     INT          NOT NULL," +
                "   `y`     INT          NOT NULL," +
                "   `z`     INT          NOT NULL," +
                "   UNIQUE KEY(`world`,`x`,`y`,`z`)" +
                ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
