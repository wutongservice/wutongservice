package com.borqs.server.platform.account2;


public abstract class HistoryInfo {

    public static final String COL_FROM = "from";
    public static final String COL_TO = "to";

    protected String from;
    protected String to;

    protected HistoryInfo() {
    }

    protected HistoryInfo(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
