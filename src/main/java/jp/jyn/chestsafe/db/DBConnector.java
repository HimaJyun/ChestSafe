package jp.jyn.chestsafe.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jp.jyn.chestsafe.ChestSafe;
import jp.jyn.chestsafe.config.config.MainConfig;
import jp.jyn.chestsafe.db.driver.IDDriver;
import jp.jyn.chestsafe.db.driver.ProtectionDriver;
import jp.jyn.chestsafe.db.driver.mysql.IDMysql;
import jp.jyn.chestsafe.db.driver.mysql.ProtectionMysql;
import jp.jyn.chestsafe.db.driver.sqlite.IDSqlite;
import jp.jyn.chestsafe.db.driver.sqlite.ProtectionSqlite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DBConnector {
    private final HikariDataSource hikari;

    public final IDDriver idDriver;
    public final ProtectionDriver protectionDriver;

    public DBConnector(MainConfig.DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(config.url);
        hikariConfig.setPoolName("chestsafe-hikari");
        hikariConfig.setAutoCommit(true);
        hikariConfig.setConnectionInitSql(config.init);
        hikariConfig.setDataSourceProperties(config.properties);

        if (config.maximumPoolSize > 0) {
            hikariConfig.setMaximumPoolSize(config.maximumPoolSize);
        }
        if (config.minimumIdle > 0) {
            hikariConfig.setMinimumIdle(config.minimumIdle);
        }
        if (config.maxLifetime > 0) {
            hikariConfig.setMaxLifetime(config.maxLifetime);
        }
        if (config.connectionTimeout > 0) {
            hikariConfig.setConnectionTimeout(config.connectionTimeout);
        }
        if (config.idleTimeout > 0) {
            hikariConfig.setIdleTimeout(config.idleTimeout);
        }

        Logger logger = ChestSafe.getInstance().getLogger();
        if (config.url.startsWith("jdbc:sqlite:")) {
            // SQLite
            logger.info("Use SQLite");
            hikari = new HikariDataSource(hikariConfig);
            checkVersion();

            idDriver = new IDSqlite(hikari);
            protectionDriver = new ProtectionSqlite(hikari);
        } else if (config.url.startsWith("jdbc:mysql:")) {
            // MySQL
            logger.info("Use MySQL");
            hikariConfig.setUsername(config.username);
            hikariConfig.setPassword(config.password);
            hikari = new HikariDataSource(hikariConfig);
            checkVersion();

            idDriver = new IDMysql(hikari);
            protectionDriver = new ProtectionMysql(hikari);
        } else {
            throw new IllegalArgumentException("Unknown jdbc");
        }
    }

    private void checkVersion() {
        try (Connection connection = hikari.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS `meta` (" +
                    "   `key` TEXT," +
                    "   `value` TEXT" +
                    ")"
            );

            try (ResultSet resultSet = statement.executeQuery("SELECT `value` FROM `meta` WHERE `key`='dbversion'")) {
                if (resultSet.next()) {
                    if (!resultSet.getString("value").equals("1")) {
                        throw new RuntimeException("An incompatible change was made (database can not be downgraded)");
                    }
                } else {
                    statement.executeUpdate("INSERT INTO `meta` VALUES('dbversion','1')");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        if (hikari != null) {
            hikari.close();
        }
    }
}
