package com.borqs.server.platform.util.sender;


import java.util.concurrent.Executor;

public abstract class AbstractAsyncSender<T> implements AsyncSender<T> {
    protected Executor executor;

    protected AbstractAsyncSender() {
        this(null);
    }

    protected AbstractAsyncSender(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
