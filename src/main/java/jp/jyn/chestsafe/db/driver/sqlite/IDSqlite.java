package jp.jyn.chestsafe.db.driver.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.chestsafe.db.driver.IDDriver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class IDSqlite extends IDDriver {
    public IDSqlite(HikariDataSource hikari) {
        super(hikari);

        try (Connection connection = hikari.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_world` (" +
                    "   `id`   INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "   `name` TEXT    NOT NULL UNIQUE" +
                    ")"
            );
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_user` (" +
                    "   `id`   INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "   `uuid` BLOB    NOT NULL UNIQUE" +
                    ")"
            );
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `id_protection` (" +
                    "   `id`    INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "   `world` INTEGER NOT NULL," +
                    "   `x`     INTEGER NOT NULL," +
                    "   `y`     INTEGER NOT NULL," +
                    "   `z`     INTEGER NOT NULL," +
                    "   UNIQUE(`world`,`x`,`y`,`z`)" +
                    ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
