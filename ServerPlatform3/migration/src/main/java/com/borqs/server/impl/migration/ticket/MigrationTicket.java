package com.borqs.server.impl.migration.ticket;


public class MigrationTicket {
    private long user;
    private String ticket;
    private int app;
    private long cratedTime;

    public long getCratedTime() {
        return cratedTime;
    }

    public void setCratedTime(long cratedTime) {
        this.cratedTime = cratedTime;
    }

    public long getUser() {
        return user;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public int getApp() {
        return app;
    }

    public void setApp(int app) {
        this.app = app;
    }
}
