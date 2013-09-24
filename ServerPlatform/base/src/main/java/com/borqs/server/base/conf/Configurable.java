package com.borqs.server.base.conf;


public interface Configurable {
    void setConfig(Configuration conf);
    Configuration getConfig();
}
