package me.twentybytes.zuki.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import me.twentybytes.zuki.api.callback.SelectCallback;
import me.twentybytes.zuki.api.callback.UpdateCallback;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@SuppressWarnings({"all"})
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class ZukiDatabase {

    @Setter
    HikariConfig config;
    HikariDataSource source;
    ExecutorService service = Executors.newFixedThreadPool(2);

    /**
     * Create data source.
     *
     * @param config connection config.
     */
    public ZukiDatabase start(@NotNull HikariConfig config) {
        close();
        source = new HikariDataSource(config);
        return this;
    }

    /**
     * Create data source.
     */
    public ZukiDatabase start() {
        return start(config);
    }

    /**
     * Close current data source if exists.
     */
    public ZukiDatabase close() {
        if (source != null) {
            source.close();
            source = null;
        }
        return this;
    }

    /**
     * @return database connection.
     */
    @SneakyThrows
    public Connection connection() {
        return source.getConnection();
    }


    /**
     * Executes all queries from file.
     *
     * @param file executing file.
     */
    @SneakyThrows
    public ZukiDatabase file(@NotNull File file) {
        return stream(new FileInputStream(file));
    }

    /**
     * Executes all queries from input stream using
     * splitter ';'
     *
     * @param stream input stream.
     */
    public ZukiDatabase stream(@NotNull InputStream stream) {
        service.execute(() -> {
            try (Connection connection = connection(); Scanner scanner = new Scanner(stream).useDelimiter(";")) {
                while (scanner.hasNext()) {
                    String query = scanner.next().trim();

                    Statement statement;
                    if (!query.isEmpty()) {
                        statement = connection.createStatement();
                        statement.execute(query);
                        if (statement.getResultSet() != null) {
                            statement.getResultSet().close();
                        }
                        statement.close();
                    }
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        return this;
    }

    /**
     * Use for update queries... (UPDATE, INSERT...)
     *
     * @param query    executing mysql command.
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    public CompletableFuture<Void> update(@NotNull String query, Object... args) {
        return update(query, null, args);
    }

    /**
     * Use for non-update queries... (SELECT)
     *
     * @param query    executing mysql command.
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    @SneakyThrows
    public CompletableFuture<Void> select(@NotNull String query, Object... args) {
        return select(query, null, args);
    }

    /**
     * Use for update queries... (UPDATE, INSERT...)
     *
     * @param query    executing mysql command.
     * @param callback query callback.
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    public CompletableFuture<Void> update(@NotNull String query, UpdateCallback callback, Object... args) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = connection()) {
                Statement statement = args.length == 0 ? connection.createStatement() : connection.prepareStatement(query);
                if (args.length == 0) {
                    statement.execute(query);
                } else {
                    PreparedStatement prepared = (PreparedStatement) statement;
                    for (int i = 0; i < args.length; i++) {
                        prepared.setObject(i + 1, args[i]);
                    }

                    prepared.execute();
                }

                if (callback != null) {
                    callback.run(statement.getUpdateCount());
                }
                if (statement.getResultSet() != null) {
                    statement.getResultSet().close();
                }
                statement.close();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Use for non-update queries... (SELECT)
     *
     * @param query    executing mysql command.
     * @param callback query callback.
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    @SneakyThrows
    public CompletableFuture<Void> select(@NotNull String query, SelectCallback callback, Object... args) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = connection()) {
                Statement statement = args.length == 0 ? connection.createStatement() : connection.prepareStatement(query);

                if (args.length == 0) {
                    statement.execute(query);
                } else {
                    PreparedStatement prepared = (PreparedStatement) statement;
                    for (int i = 0; i < args.length; i++) {
                        prepared.setObject(i + 1, args[i]);
                    }

                    prepared.execute();
                }

                try (ResultSet set = statement.getResultSet()) {
                    if (callback != null) {
                        callback.run(set);
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
            return null;
        });

    }

}
