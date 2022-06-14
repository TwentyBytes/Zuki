package me.twentybytes.zuki.api.callback;

import java.sql.ResultSet;

public interface SelectCallback extends Callback {

    void run(ResultSet set) throws Throwable;

}
