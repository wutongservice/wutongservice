package com.borqs.server.platform.context;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.io.RW;
import com.borqs.server.platform.io.Writable;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.GeoLocation;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.web.UserAgent;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Context implements Writable, Copyable<Context> {
    public static final long DUMMY_ACCESS_ID = 1;

    public long accessId;

    private long viewer;
    private int app = 0;
    private String rawUserAgent;
    private String remote = "";
    private boolean internal = false;
    private boolean privacyEnabled = true;
    private int callIndent = 0;
    private GeoLocation geoLocation;
    private String location;
    private Map<String, Object> sessions;


    public Context() {
        this(RandomHelper.generateId());
    }

    private Context(long accessId) {
        this.accessId = accessId;
    }

    public Context setViewer(long viewer) {
        this.viewer = viewer;
        return this;
    }

    public long getViewer() {
        return viewer;
    }

    public PeopleId getViewerAsPeople() {
        return PeopleId.user(viewer);
    }

    public Context setApp(int app) {
        this.app = app;
        return this;
    }

    public int getApp() {
        return app;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getRawUserAgent() {
        return rawUserAgent;
    }

    public void setRawUserAgent(String rawUserAgent) {
        this.rawUserAgent = rawUserAgent;
    }

    public UserAgent getUserAgent() {
        return UserAgent.parse(rawUserAgent);
    }

    public int getCallIndent() {
        return callIndent;
    }

    public void setCallIndent(int callIndent) {
        this.callIndent = callIndent;
    }

    public boolean isInternal() {
        return internal;
    }

    public Context setInternal(boolean internal) {
        this.internal = internal;
        return this;
    }

    public boolean isPrivacyEnabled() {
        return !internal || privacyEnabled;
    }

    public void setPrivacyEnabled(boolean privacyEnabled) {
        this.privacyEnabled = privacyEnabled;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isLogined() {
        return viewer > 0;
    }

    public long checkLogined() {
        if (!isLogined())
            throw new ServerException(E.NOT_LOGIN, "Need context with viewer");
        return viewer;
    }

    public boolean hasSession() {
        return MapUtils.isNotEmpty(sessions);
    }

    public boolean hasSession(String key) {
        return sessions != null && sessions.containsKey(key);
    }

    public Object getSession(String key) {
        return sessions != null ? sessions.get(key) : null;
    }

    public Object getSession(String key, String def) {
        return sessions != null ? sessions.get(key) : def;
    }

    public Context putSession(String key, Object value) {
        if (sessions == null)
            sessions = new HashMap<String, Object>();

        sessions.put(key, value);
        return this;
    }

    public Context removeSession(String key) {
        if (sessions != null)
            sessions.remove(key);
        return this;
    }

    public Context clearSessions() {
        if (sessions != null)
            sessions.clear();
        return this;
    }

    public String[] getSessionKeys() {
        if (sessions != null) {
            Set<String> keys = sessions.keySet();
            return keys.toArray(new String[keys.size()]);
        } else {
            return new String[0];
        }
    }

    public String getLocale() {
        return StringUtils.isBlank(rawUserAgent) ? "" : getUserAgent().getLocale();
    }

    @Override
    public String toString() {
        return (isLogined() ? Long.toString(viewer) : "anonymous") + "_" + accessId;
    }

    public static Context create() {
        return new Context();
    }

    public static Context createForViewer(long viewer) {
        return create().setViewer(viewer);
    }

    public static Context createInternal() {
        return create().setInternal(true);
    }

    public static Context createInternalForViewer(long viewer) {
        return createInternal().setViewer(viewer);
    }

    public static Context createDummy() {
        Context ctx = new Context(DUMMY_ACCESS_ID);
        ctx.viewer = 0;
        ctx.app = 0;
        ctx.rawUserAgent = "";
        ctx.remote = "";
        ctx.internal = false;
        ctx.privacyEnabled = true;
        ctx.callIndent = 0;
        ctx.geoLocation = new GeoLocation(0.0, 0.0);
        ctx.location = "";
        ctx.sessions = null;
        return ctx;
    }

    public static Context createInternalDummy() {
        return createDummy().setInternal(true);
    }

    public static Context createDummyForViewer(long viewer) {
        return createDummy().setViewer(viewer);
    }

    public static Context createInternalDummyForViewer(long viewer) {
        return createInternalDummy().setViewer(viewer);
    }

    @Override
    public Context copy() {
        Context ctx = new Context(accessId);
        ctx.setViewer(viewer);
        ctx.setRawUserAgent(rawUserAgent);
        ctx.setApp(app);
        ctx.setRemote(remote);
        ctx.setInternal(internal);
        ctx.setPrivacyEnabled(privacyEnabled);
        ctx.setCallIndent(callIndent);
        if (sessions != null) {
            ctx.sessions = new HashMap<String, Object>();
            ctx.sessions.putAll(sessions);
        } else {
            ctx.sessions = null;
        }
        return ctx;
    }

    @Override
    public void write(Encoder out, boolean flush) throws IOException {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("accessId", accessId);
        m.put("viewer", viewer);
        m.put("app", app);
        m.put("remote", remote);
        m.put("userAgent", rawUserAgent);
        m.put("internal", internal);
        m.put("callIndent", callIndent);
        m.put("privacyEnabled", privacyEnabled);
        m.put("geoLocation", geoLocation);
        m.put("location", location);
        m.put("sessions", sessions);
        RW.write(out, m, flush);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readIn(Decoder in) throws IOException {
        Map<String, Object> m = (Map<String, Object>) RW.read(in);
        accessId = (Long) m.get("accessId");
        viewer = (Long) m.get("viewer");
        app = (Integer) m.get("app");
        remote = (String) m.get("remote");
        callIndent = (Integer) m.get("callIndent");
        rawUserAgent = (String) m.get("userAgent");
        internal = (Boolean) m.get("internal");
        privacyEnabled = (Boolean) m.get("privacyEnabled");
        geoLocation = (GeoLocation) m.get("geoLocation");
        location = (String) m.get("location");
        sessions = (Map<String, Object>) m.get("sessions");
    }

    public void setToggle(String key, boolean toggle) {
        if (toggle)
            putSession(key, true);
        else
            removeSession(key);
    }

    public boolean toggleEnabled(String key) {
        return BooleanUtils.isTrue((Boolean) getSession(key));
    }
}
