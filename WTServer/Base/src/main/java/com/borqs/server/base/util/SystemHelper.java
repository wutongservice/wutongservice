package com.borqs.server.base.util;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

public class SystemHelper {

    public static final String HOME_KEY = "BS_HOME";
    public static final String LOCAL_PROPERTY_KEY = "local";

    public static String getHomeDirectory() {
        return getPropOrEnv(HOME_KEY, "");
    }

    public static String getTempDirectory() {
        return getProperty("java.io.tmpdir", "");
    }

    public static String getPathInTempDir(String file) {
        String tmpDir = getTempDirectory();
        return FilenameUtils.concat(tmpDir, file);
    }

    public static String getPropOrEnv(String key, String def) {
        String val = getProperty(key, null);
        return val != null ? val : getEnvironment(key, def);
    }

    public static String getEnvOrProp(String key, String def) {
        String val = getEnvironment(key, null);
        return val != null ? val : getProperty(key, def);
    }

    public static String getProperty(String key, String def) {
        return System.getProperty(key, def);
    }

    public static String getEnvironment(String key, String def) {
        String val = System.getenv(key);
        return val != null ? val : def;
    }

    public static boolean isLocalRun() {
        return Boolean.parseBoolean(getProperty(LOCAL_PROPERTY_KEY, "false"));
    }

    public static String getOS() {
        return getProperty("os.name", "");
    }

    public static boolean osIsWindows() {
        String os = getOS();
        return StringUtils.containsIgnoreCase(os, "Windows");
    }
}
