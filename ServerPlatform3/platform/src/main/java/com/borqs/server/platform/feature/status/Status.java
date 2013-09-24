package com.borqs.server.platform.feature.status;


public class Status {
    public final String status;
    public final long updatedTime;

    public Status(String status, long updatedTime) {
        this.status = status;
        this.updatedTime = updatedTime;
    }
}
