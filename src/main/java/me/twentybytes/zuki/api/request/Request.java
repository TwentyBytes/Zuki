package me.twentybytes.zuki.api.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.twentybytes.zuki.api.callback.Callback;
import me.twentybytes.zuki.api.callback.SelectCallback;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

@Getter @ToString @EqualsAndHashCode
public class Request {

    private final RequestType requestType;
    private Callback callback;
    private String body;
    private Object[] args;
    private boolean queue;
    private Runnable timeoutRunnable;
    private long timeout;
    private boolean sync;

    public Request(RequestType type) {
        this.requestType = type;
    }

    public Request body(@NotNull @Language("SQL") String body) {
        this.body = body;
        return this;
    }

    public Request callback(Callback callback) {
        this.callback = callback;
        return this;
    }

    public Request selectCallback(SelectCallback callback) {
        return callback(callback);
    }

    public Request updateCallback(SelectCallback callback) {
        return callback(callback);
    }

    public Request arguments(Object... args) {
        this.args = args;
        return this;
    }

    public Request queue(boolean state) {
        this.queue = state;
        return this;
    }

    public Request timeout(Runnable timeoutRunnable, long timeout) {
        this.timeoutRunnable = timeoutRunnable;
        this.timeout = timeout;
        return this;
    }

    public Request sync(boolean sync) {
        this.sync = sync;
        return this;
    }

    public static Request newBuilder(RequestType requestType) {
        return new Request(requestType);
    }

}
