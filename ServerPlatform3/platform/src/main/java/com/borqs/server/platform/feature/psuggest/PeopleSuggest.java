package com.borqs.server.platform.feature.psuggest;

import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.StringHelper;

public class PeopleSuggest {
    private long user;
    private PeopleId suggested;
    private int reason;
    private String source;
    private int status;
    private long createdTime;
    private long dealTime;

    public PeopleSuggest() {
    }

    public static PeopleSuggest of(long user, PeopleId suggested, int reason, String source) {
        return new PeopleSuggest(user, suggested, reason ,source, Status.INIT, DateHelper.nowMillis(), 0);
    }

    public PeopleSuggest(long user, PeopleId suggested, int reason, String source, int status, long createdTime, long dealTime) {
        this.user = user;
        this.suggested = suggested;
        this.reason = reason;
        this.source = source;
        this.status = status;
        this.createdTime = createdTime;
        this.dealTime = dealTime;
    }

    public long getUser() {
        return user;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public PeopleId getSuggested() {
        return suggested;
    }

    public void setSuggested(PeopleId suggested) {
        this.suggested = suggested;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long[] getSourceAsLongArray() {
        return source == null ? new long[0] : StringHelper.splitLongArray(source, ",");
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getDealTime() {
        return dealTime;
    }

    public void setDealTime(long dealTime) {
        this.dealTime = dealTime;
    }

    @Override
    public String toString() {
        return this.user + "-" + this.suggested.toString() + "-" + this.reason;
    }
}
