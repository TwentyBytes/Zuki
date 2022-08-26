package me.twentybytes.zuki.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import lombok.SneakyThrows;
import me.twentybytes.zuki.impl.config.SimpleZukiConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;

/**
 * @author TwentyBytes.
 * created in 14.06.2022.
 *
 * ZukiConfig - abstraction for zuki database config.
 */
public interface ZukiConfig {

    /**
     * @return hikari connection address (url).
     */
    String address();

    /**
     * @return database name.
     */
    String database();

    /**
     * @return database user name.
     */
    String username();

    /**
     * @return database user password.
     */
    String password();

    /**
     * @return database connection port.
     */
    int port();

    String[] keys = {
            "address",
            "port",
            "database",
            "username",
            "password"
    };

    /**
     * @param file config file.
     * @param type config parser.
     * @return     parsed ZukiConfig from file.
     */
    @SneakyThrows @NotNull
    static ZukiConfig from(@NotNull File file, @NotNull ConfigType type) {
        if (!file.exists()) {
            throw new IllegalStateException("Config file does`nt exists...");
        }

        switch (type) {
            case TOML:
                return new ObjectMapper(new TomlFactory()).readValue(file, SimpleZukiConfig.class);
            case YAML:
                return new ObjectMapper(new YAMLFactory()).readValue(file, SimpleZukiConfig.class);
            case JSON:
                JsonReader reader = new JsonReader((new FileReader(file)));
                SimpleZukiConfig config = new Gson().fromJson(reader, SimpleZukiConfig.class);
                reader.close();
                return config;
        }
        return null;
    }

    /**
     * @param address  config address.
     * @param port     config port.
     * @param database config database name.
     * @param username config user name.
     * @param password config user password.
     * @return         Simply ZukiConfig instance from specified params.
     */
    static ZukiConfig from(@NotNull String address, int port, @NotNull String database, @NotNull String username, @NotNull String password) {
        return new SimpleZukiConfig(address, port, database, username, password);
    }

}
