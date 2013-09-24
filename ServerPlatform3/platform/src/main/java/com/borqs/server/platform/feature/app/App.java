package com.borqs.server.platform.feature.app;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.util.Copyable;

public class App extends Addons implements Copyable<App> {
    public static final int APP_NONE = 0;

    private int appId;
    private String secret;
    private String name;

    public App() {
    }

    public App(int appId, String secret, String name) {
        this.appId = appId;
        this.secret = secret;
        this.name = name;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public App copy() {
        App app = new App();
        app.appId = appId;
        app.secret = secret;
        app.name = name;
        app.addons.putAll(addons);
        return app;
    }
}
