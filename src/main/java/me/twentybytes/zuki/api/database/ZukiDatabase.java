package me.twentybytes.zuki.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import me.twentybytes.zuki.api.callback.SelectCallback;
import me.twentybytes.zuki.api.callback.UpdateCallback;
import me.twentybytes.zuki.api.request.Request;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@SuppressWarnings({"all"})
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class ZukiDatabase {

    @Setter
    HikariConfig config;
    HikariDataSource source;
    ScheduledExecutorService service = Executors.newScheduledThreadPool(3);
    Deque<Request> queue = new ArrayDeque<>();

    boolean lock;

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
                        log.error("Unclosed statement... Message: " + exception.getMessage());
                    }
                }

                log.error("Throwed SQL exception on stream method.");
                log.error("Message: " + throwable.getMessage());
                log.error("Stacktrace:");
                for (StackTraceElement traceElement : stackTrace) {
                    log.error("\tat " + traceElement);
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
     * @param args     arguments for prepared statement.
     * @return {@link CompletableFuture<Void>} result set.
     */
    protected CompletableFuture<Void> update(@NotNull @Language("SQL") String query, boolean queued, UpdateCallback callback, Object... args) {
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

                if (queued) {
                    lock = false;
                    if (!queue.isEmpty()) {
                        execute(queue.poll());
                    }
                }
            } catch (SQLException exception) {
                log.error("Throwed SQL exception on update method. Stacktrace:");
                log.error("Message: " + exception.getMessage());
                log.error("Stacktrace: ");
                for (StackTraceElement traceElement : stackTrace) {
                    log.error("\tat " + traceElement);
                }
            }
            return null;
        }, service);
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
    protected CompletableFuture<Void> select(@NotNull @Language("SQL") String query, boolean queued, SelectCallback callback, Object... args) {
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

                    if (queued) {
                        lock = false;
                        if (!queue.isEmpty()) {
                            execute(queue.poll());
                        }
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            } catch (SQLException exception) {
                log.error("Throwed SQL exception on select method. Stacktrace:");
                log.error("Message: " + exception.getMessage());
                log.error("Stacktrace:");
                for (StackTraceElement traceElement : stackTrace) {
                    log.error("\tat " + traceElement);
                }
            }
            return null;
        }, service);

    }

    @SneakyThrows
    public ZukiDatabase execute(Request request) {
        if (request.getBody() == null || request.getBody().isEmpty()) {
            throw new IllegalStateException("Illegal state: request body is empty or null");
        }

        if (request.isQueue()) {
            if (lock) {
                queue.add(request);
                return this;
            } else {
                lock = true;
            }
        }

        final CompletableFuture<Void> future;
        switch (request.getRequestType()) {
            case UPDATE -> {
                future = update(request.getBody(), request.isQueue(), (UpdateCallback) request.getCallback(), request.getArgs());
            }
            default -> future = select(request.getBody(), request.isQueue(), (SelectCallback) request.getCallback(), request.getArgs());
        }

        if (request.getTimeoutRunnable() != null) {
            service.schedule(() -> {
                if (!future.isDone()) {
                    request.getTimeoutRunnable().run();
                }
            }, request.getTimeout(), TimeUnit.MILLISECONDS);
        }

        if (request.isSync()) {
            future.get();
        }

        return this;
    }

}
