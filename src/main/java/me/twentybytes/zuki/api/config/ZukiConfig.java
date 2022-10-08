package me.twentybytes.zuki.api.config;

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.dataformat.toml.TomlFactory;
//import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
//import com.google.gson.Gson;
//import com.google.gson.stream.JsonReader;

import lombok.SneakyThrows;
import me.twentybytes.zuki.impl.config.SimpleZukiConfig;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * @author TwentyBytes.
 * created in 14.06.2022.
 * <p>
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

    String[] CONFIG_PARAMS = {
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
            case YAML:
                try (FileInputStream stream = new FileInputStream(file)) {
                    Map<String, Object> storage = new Yaml().load(stream);
                    return new SimpleZukiConfig(
                            (String) storage.get("address"),
                            (int) storage.get("port"),
                            (String) storage.get("database"),
                            (String) storage.get("username"),
                            (String) storage.get("password")
                    );
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            case JSON:
                try (FileInputStream stream = new FileInputStream(file)) {
                    JSONTokener tokener = new JSONTokener(stream);
                    JSONObject object = new JSONObject(tokener);
                    return new SimpleZukiConfig(
                            object.optString("address"),
                            object.optInt("port"),
                            object.optString("database"),
                            object.optString("username"),
                            object.optString("password")
                    );
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
        }

        throw new IllegalStateException("WTF? How the code get here?");
    }

    /**
     * @param address  config address.
     * @param port     config port.
     * @param database config database name.
     * @param username config user name.
     * @param password config user password.
     * @return Simply ZukiConfig instance from specified params.
     */
    static ZukiConfig from(@NotNull String address, int port, @NotNull String database, @NotNull String username, @NotNull String password) {
        return new SimpleZukiConfig(address, port, database, username, password);
    }

}
