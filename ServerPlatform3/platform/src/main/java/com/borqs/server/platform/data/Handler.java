package com.borqs.server.platform.data;


public interface Handler<T> {
    void handle(T o);
}
