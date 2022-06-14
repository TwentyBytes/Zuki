package me.twentybytes.zuki.impl.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import me.twentybytes.zuki.api.config.ZukiConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Simply implementation from {@link ZukiConfig}
 * Used for config parsing.
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public class SimpleZukiConfig implements ZukiConfig {

    String address;
    String database;
    String username;
    String password;
    int port;

    public SimpleZukiConfig(@NotNull String address, int port, @NotNull String database, @NotNull String username, @NotNull String password) {
        this.address = address;
        this.database = database;
        this.username = username;
        this.password = password;
        this.port = port;
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public String database() {
        return database;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public int port() {
        return port;
    }

}
