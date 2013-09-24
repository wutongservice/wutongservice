package com.borqs.server.base.conf;


public class ConfigurableBase implements Configurable {
    private Configuration configuration;

    @Override
    public void setConfig(Configuration conf) {
        this.configuration = conf;
    }

    @Override
    public Configuration getConfig() {
        return configuration;
    }
}
