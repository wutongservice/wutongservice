package com.borqs.server.base.conf;


public class GlobalConfig {
    private static Configuration instance;

    public static Configuration get() {
        return instance;
    }

    public static void loadArgs(String[] args) {
        instance = Configuration.loadArgs(args).expandMacros();
    }
    
    public static void loadFiles(String... paths) {
        instance = Configuration.loadFiles(paths).expandMacros();
    }
}
