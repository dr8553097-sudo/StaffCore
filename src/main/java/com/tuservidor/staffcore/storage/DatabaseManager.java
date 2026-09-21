package com.tuservidor.staffcore.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseManager implements AutoCloseable {

    private final Logger logger;
    private final StorageMode mode;
    private HikariDataSource dataSource;

    public DatabaseManager(File dataFolder, FileConfiguration config, Logger logger) {
        this.logger = logger;
        this.mode = StorageMode.fromConfig(config.getString("storage.mode", "yaml"));
        if (mode == StorageMode.SQLITE || mode == StorageMode.MYSQL) {
            initDataSource(dataFolder, config);
            initTables();
        }
    }

    private void initDataSource(File dataFolder, FileConfiguration config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("StaffCore-HikariPool");

        if (mode == StorageMode.SQLITE) {
            String dbName = config.getString("storage.sqlite.file", "staffcore.db");
            File dbFile = new File(dataFolder, dbName);
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setMaximumPoolSize(1); // SQLite is single-writer
            hikari.setConnectionTestQuery("SELECT 1");
        } else if (mode == StorageMode.MYSQL) {
            String host = config.getString("storage.mysql.host", "127.0.0.1");
            int port = config.getInt("storage.mysql.port", 3306);
            String db = config.getString("storage.mysql.database", "staffcore");
            String user = config.getString("storage.mysql.username", "root");
            String pass = config.getString("storage.mysql.password", "");
            boolean ssl = config.getBoolean("storage.mysql.ssl", false);
            int poolSize = config.getInt("storage.mysql.pool-size", 8);

            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            hikari.setUsername(user);
            hikari.setPassword(pass);
            hikari.setMaximumPoolSize(Math.max(2, poolSize));
            hikari.setMinimumIdle(2);
            hikari.setIdleTimeout(30000);
            hikari.setMaxLifetime(1800000);
            hikari.setConnectionTimeout(10000);
        }

        try {
            this.dataSource = new HikariDataSource(hikari);
            logger.info("[StaffCore] Successfully initialized " + mode.name() + " connection pool via HikariCP.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[StaffCore] Failed to initialize database connection pool for " + mode.name() + ": " + e.getMessage(), e);
        }
    }

    private void initTables() {
        if (dataSource == null) {
            return;
        }

        String autoInc = (mode == StorageMode.SQLITE) ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "INT AUTO_INCREMENT PRIMARY KEY";

        String[] ddlStatements = new String[]{
            "CREATE TABLE IF NOT EXISTS staffcore_reports ("
                + "id " + autoInc + ", "
                + "reporter VARCHAR(64) NOT NULL, "
                + "reported VARCHAR(64) NOT NULL, "
                + "reason TEXT NOT NULL, "
                + "status VARCHAR(32) NOT NULL DEFAULT 'OPEN', "
                + "timestamp BIGINT NOT NULL, "
                + "resolved_by VARCHAR(64)"
                + ");",

            "CREATE TABLE IF NOT EXISTS staffcore_notes ("
                + "id " + autoInc + ", "
                + "target_name VARCHAR(64) NOT NULL, "
                + "note TEXT NOT NULL, "
                + "author VARCHAR(64) NOT NULL, "
                + "timestamp BIGINT NOT NULL"
                + ");",

            "CREATE TABLE IF NOT EXISTS staffcore_duty ("
                + "uuid VARCHAR(64) PRIMARY KEY, "
                + "name VARCHAR(64) NOT NULL, "
                + "total_duty_seconds BIGINT NOT NULL DEFAULT 0, "
                + "weekly_duty_seconds BIGINT NOT NULL DEFAULT 0, "
                + "monthly_duty_seconds BIGINT NOT NULL DEFAULT 0, "
                + "actions_count INT NOT NULL DEFAULT 0, "
                + "last_seen_epoch_ms BIGINT NOT NULL"
                + ");"
        };

        try (Connection conn = getConnection()) {
            if (conn == null) return;
            try (Statement stmt = conn.createStatement()) {
                for (String sql : ddlStatements) {
                    stmt.execute(sql);
                }
            }
            logger.info("[StaffCore] Database tables verified & initialized successfully.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[StaffCore] Error initializing database tables: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            return null;
        }
        return dataSource.getConnection();
    }

    public boolean isAvailable() {
        return dataSource != null && !dataSource.isClosed();
    }

    public StorageMode getMode() {
        return mode;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("[StaffCore] Database connection pool closed.");
        }
    }
}
