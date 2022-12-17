package me.twentybytes.zuki.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import me.twentybytes.zuki.api.callback.SelectCallback;
import me.twentybytes.zuki.api.callback.UpdateCallback;
import org.apache.logging.log4j.Level;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.*;

@Log4j2
@Getter
@SuppressWarnings({"all"})
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class ZukiDatabase {

    @Setter
    HikariConfig config;
    HikariDataSource source;
    ScheduledExecutorService service = Executors.newScheduledThreadPool(3);

    /**
     * ResultSet type.
     */
    @Setter
    int resultSetType = ResultSet.TYPE_FORWARD_ONLY;

    /**
     * Result set concurrency (only-read or editable)
     */
    @Setter
    int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;

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
        // real stacktrace
        StackTraceElement[] sourceStackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement[] stackTrace = Arrays.copyOfRange(sourceStackTrace, 2,
                sourceStackTrace.length);

        service.execute(() -> {
            Statement statement = null;
            try (Connection connection = connection(); Scanner scanner = new Scanner(stream).useDelimiter(";")) {
                while (scanner.hasNext()) {
                    String query = scanner.next().trim();

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
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException exception) {
                        log.log(Level.ERROR,"Unclosed statement... Message: " + exception.getMessage());
                    }
                }

                log.log(Level.ERROR, "Throwed SQL exception on stream method.");
                log.log(Level.ERROR, "Message: " + throwable.getMessage());
                log.log(Level.ERROR, "Stacktrace:");
                for (StackTraceElement traceElement : stackTrace) {
                    log.log(Level.ERROR, "\tat " + traceElement);
                }
            }
        });
        return this;
    }

    /**
     * Use for update queries... (UPDATE, INSERT...)
     *
     * @param query    executing mysql command.
     * @param callback query callback.
     * @param timeoutRunnable runnable calling if base
     *                        call doesnt back result.
     * @param timeOut call timeout
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    public CompletableFuture<Void> update(@NotNull @Language("SQL") String query, UpdateCallback callback, Runnable timeoutRunnable, long timeOut, Object... args) {
        CompletableFuture<Void> future = update(query, callback, args);

        service.schedule(() -> {
            if (!future.isDone()) {
                timeoutRunnable.run();
            }
        }, timeOut, TimeUnit.MILLISECONDS);

        return future;
    }

    /**
     * Use for non-update queries... (SELECT)
     *
     * @param query    executing mysql command.
     * @param callback query callback.
     * @param timeoutRunnable runnable calling if base
     *                        call doesnt back result.
     * @param timeOut call timeout
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    public CompletableFuture<Void> select(@NotNull @Language("SQL") String query, SelectCallback callback, Runnable timeoutRunnable, long timeOut, Object... args) {
        CompletableFuture<Void> future = select(query, callback, args);

        service.schedule(() -> {
            if (!future.isDone()) {
                timeoutRunnable.run();
            }
        }, timeOut, TimeUnit.MILLISECONDS);

        return future;
    }


    /**
     * Use for update queries... (UPDATE, INSERT...)
     *
     * @param query    executing mysql command.
     * @param timeoutRunnable runnable calling if base
     *                        call doesnt back result.
     * @param timeOut call timeout
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    public CompletableFuture<Void> update(@NotNull @Language("SQL") String query, Runnable timeoutRunnable, long timeOut, Object... args) {
        CompletableFuture<Void> future = update(query, args);

        service.schedule(() -> {
            if (!future.isDone()) {
                timeoutRunnable.run();
            }
        }, timeOut, TimeUnit.MILLISECONDS);

        return future;
    }

    /**
     * Use for non-update queries... (SELECT)
     *
     * @param query    executing mysql command.
     * @param timeoutRunnable runnable calling if base
     *                        call doesnt back result.
     * @param timeOut call timeout
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    public CompletableFuture<Void> select(@NotNull @Language("SQL") String query, Runnable timeoutRunnable, long timeOut, Object... args) {
        CompletableFuture<Void> future = select(query, args);

        service.schedule(() -> {
            if (!future.isDone()) {
                timeoutRunnable.run();
            }
        }, timeOut, TimeUnit.MILLISECONDS);

        return future;
    }

    /**
     * Use for update queries... (UPDATE, INSERT...)
     *
     * @param query    executing mysql command.
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    public CompletableFuture<Void> update(@NotNull @Language("SQL") String query, Object... args) {
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
    public CompletableFuture<Void> select(@NotNull @Language("SQL") String query, Object... args) {
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
    public CompletableFuture<Void> update(@NotNull @Language("SQL") String query, UpdateCallback callback, Object... args) {
        // real stacktrace
        StackTraceElement[] sourceStackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement[] stackTrace = Arrays.copyOfRange(sourceStackTrace, 2,
                sourceStackTrace.length);

        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = connection(); Statement statement = args.length == 0 ? connection.createStatement() : connection.prepareStatement(query)) {
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
            } catch (SQLException exception) {
                log.log(Level.ERROR, "Throwed SQL exception on update method. Stacktrace:");
                log.log(Level.ERROR, "Message: " + exception.getMessage());
                log.log(Level.ERROR, "Stacktrace: ");
                for (StackTraceElement traceElement : stackTrace) {
                    log.log(Level.ERROR, "\tat " + traceElement);
                }
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
    public CompletableFuture<Void> select(@NotNull @Language("SQL") String query, SelectCallback callback, Object... args) {
        // real stacktrace
        StackTraceElement[] sourceStackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement[] stackTrace = Arrays.copyOfRange(sourceStackTrace, 2,
                sourceStackTrace.length);

        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = connection(); Statement statement = args.length == 0 ? connection.createStatement(resultSetType, resultSetConcurrency) :
                    connection.prepareStatement(query, resultSetType, resultSetConcurrency)) {

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
                log.log(Level.ERROR, "Throwed SQL exception on select method. Stacktrace:");
                log.log(Level.ERROR, "Message: " + exception.getMessage());
                log.log(Level.ERROR, "Stacktrace:");
                for (StackTraceElement traceElement : stackTrace) {
                    log.log(Level.ERROR, "\tat " + traceElement);
                }
            }
            return null;
        });

    }

}
