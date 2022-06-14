package me.twentybytes.zuki.impl.database;

import com.zaxxer.hikari.HikariConfig;
import me.twentybytes.zuki.api.config.ZukiConfig;
import me.twentybytes.zuki.api.database.ZukiDatabase;
import org.jetbrains.annotations.NotNull;

public class SimpleZukiDatabase extends ZukiDatabase {

    /**
     * @param config zuki connection config.
     */
    public SimpleZukiDatabase(@NotNull ZukiConfig config) {
        this(config.address(), config.port(), config.database(), config.username(), config.password());
    }

    /**
     * @param address  connection address.
     * @param database connection database name.
     * @param username connection username.
     * @param password connection user password.
     */
    public SimpleZukiDatabase(@NotNull String address, @NotNull String database, @NotNull String username, @NotNull String password) {
        this(address, 3306, database, username, password);
    }

    /**
     * @param address  connection address.
     * @param port     connection port.
     * @param database connection database name.
     * @param username connection username.
     * @param password connection user password.
     */
    public SimpleZukiDatabase(@NotNull String address, int port, @NotNull String database, @NotNull String username, @NotNull String password) {
        this(String.format("jdbc:mysql://%s:%s/%s", address, port, database), username, password);
    }

    /**
     * @param url      formatted connection url.
     * @param username connection username.
     * @param password connection user password.
     */
    public SimpleZukiDatabase(@NotNull String url, @NotNull String username, @NotNull String password) {
        HikariConfig config = new HikariConfig();

        // Connection data
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // Max active connections count.
        config.setMaximumPoolSize(6);

        // Connection die time.
        config.setLeakDetectionThreshold(10000L);

        // Simply caching for improve prepare speed similar statements.
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "50");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "512");

        // Sets config for super class.
        setConfig(config);
    }

}
