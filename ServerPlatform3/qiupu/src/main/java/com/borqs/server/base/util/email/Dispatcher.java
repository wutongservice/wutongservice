package com.borqs.server.base.util.email;


public interface Dispatcher {

    void invokeLater(Runnable task);

    void shutdown();
}
