package com.borqs.server.platform.util.sender;


public interface AsyncSender<T> {
    void asyncSend(T o);
}
