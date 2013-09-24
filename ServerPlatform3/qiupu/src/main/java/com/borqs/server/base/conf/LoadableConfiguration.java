package com.borqs.server.base.conf;


import com.borqs.server.base.util.Initializable;

public class LoadableConfiguration extends Configuration implements Initializable {
    private String path;

    public LoadableConfiguration() {
        this(null);
    }

    public LoadableConfiguration(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void init() {
        loadIn(path);
        expandMacros();
    }

    @Override
    public void destroy() {
    }
}
