package com.borqs.server.base.util.threadpool;

public abstract class NotifTask implements Runnable {
    private String json = "";

    public NotifTask(String json) {
        this.json = json;
    }

    public String getJsonString() {
        return this.json;
    }
}
