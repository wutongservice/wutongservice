package com.borqs.server.base.context;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.ElapsedCounter;
import com.borqs.server.base.util.RandomUtils;

import java.util.Set;

public class Context {
    private long viewerId;
    private String ua;
    private String location;
    private String language;
    private String appId;
    private String serverCallId;
    private String clientCallId;
    private Record sessions = new Record();
    private ElapsedCounter elapsedCounter;

    public Context() {
    }

    public long getViewerId() {
        return viewerId;
    }

    public String getViewerIdString() {
        return String.valueOf(viewerId);
    }

    public void setViewerId(long viewerId) {
        this.viewerId = viewerId;
    }

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Record getSessions() {
        return sessions;
    }

    public void putSession(String key, Object value) {
        sessions.put(key, value);
    }

    public Object getSession(String key) {
        return sessions.get(key);
    }

    public String getServerCallId() {
        return serverCallId;
    }

    public String getClientCallId() {
        return clientCallId;
    }

    public void setServerCallId(String serverCallId) {
        this.serverCallId = serverCallId;
    }

    public void setClientCallId(String clientCallId) {
        this.clientCallId = clientCallId;
    }

    public ElapsedCounter getElapsedCounter() {
        if (elapsedCounter == null) {
            setElapsedCounter(new ElapsedCounter());
        }
        return elapsedCounter;
    }

    public void setElapsedCounter(ElapsedCounter elapsedCounter) {
        this.elapsedCounter = elapsedCounter;
    }

    @SuppressWarnings("unchecked")
    public boolean inUserScope(String scopeName) {
        Set<String> inUserScopes = (Set<String>) getSession("inUserScopes");
        return inUserScopes != null && inUserScopes.contains(scopeName);
    }

    public static Context dummy() {
        Context ctx = new Context();
        ctx.setElapsedCounter(new ElapsedCounter());
        ctx.setViewerId(0L);
        ctx.setUa("DUMMY");
        ctx.setLocation("");
        ctx.setLanguage("");
        ctx.setAppId("0");
        ctx.setClientCallId("");
        ctx.setServerCallId(Long.toString(RandomUtils.generateId()));
        return ctx;
    }
}
