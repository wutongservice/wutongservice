package com.borqs.server.platform.web;


import org.apache.commons.lang.StringUtils;

public class UserAgent {

    private final boolean unknown;
    private final String raw;
    private String arch = "";                // Arch                     eg: x86, arm
    private String os = "";                  // OS                       eg: Windows, Android
    private String osVersion = "";           // OS version               eg: NT 5.1, 8
    private String client = "";              // Client /Browser          eg: Chrome, IE, com.user.qiupu
    private String clientVersion = "";       // Client Version           eg: 13.0, 115
    private String engine = "";              // Browser engine           eg: WebKit, IE
    private String engineVersion = "";       // Browser engine version
    private String locale = "";              // Locale                   eg: zh_CN, en_US
    private String model = "";               // Device model             eg: Samsung-i9100
    private String screen = "";              // Device screen size:      eg: WXGA
    private String deviceType = "";          // Device type              eg: pc, phone, pad
    private String deviceId = "";            // Device ID:               eg: IMEI or IMSI

    private UserAgent(String raw, boolean unknown) {
        this.raw = raw;
        this.unknown = unknown;
    }

    private static UserAgent newKnown(String raw) {
        return new UserAgent(raw, false);
    }

    private static UserAgent newUnknown(String raw) {
        return new UserAgent(raw, true);
    }

    public static final UserAgent EMPTY = newUnknown("");

    public static UserAgent parse(String s) {
        if (StringUtils.isBlank(s))
            return newUnknown(s);

        if (isWebBrowser0(s)) {
            nl.bitwalker.useragentutils.UserAgent ua0 = null;
            try {
                ua0 = nl.bitwalker.useragentutils.UserAgent.parseUserAgentString(s);
                if (ua0 == null)
                    return newUnknown(s);
            } catch (Exception e) {
                return newUnknown(s);
            }

            UserAgent ua = new UserAgent(s, false);
            ua.arch = "";
            ua.os = ua0.getOperatingSystem().getName();
            ua.osVersion = "";
            ua.client = ua0.getBrowser().getName();
            ua.clientVersion = ua0.getBrowserVersion().toString();
            ua.engine = ua0.getBrowser().getRenderingEngine().name();
            ua.engineVersion = "";
            ua.locale = "";
            ua.model = "";
            ua.screen = "";
            ua.deviceType = ua0.getOperatingSystem().getDeviceType().name();
            ua.deviceId = "";
            return ua;
        } else if (isBorqsClient0(s)) {
            // TODO: parse user client
            return newUnknown(s);
        } else {
            return newUnknown(s);
        }
    }


    public boolean isUnknown() {
        return unknown;
    }

    public String getRaw() {
        return raw;
    }

    public String getArch() {
        return arch;
    }

    public String getOs() {
        return os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getClient() {
        return client;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public String getEngine() {
        return engine;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public String getLocale() {
        return locale;
    }

    public String getModel() {
        return model;
    }

    public String getScreen() {
        return screen;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isWebBrowser() {
        return isWebBrowser0(raw);
    }

    @Override
    public String toString() {
        return raw;
    }

    private static boolean isWebBrowser0(String raw) {
        return StringUtils.contains(raw, "Mozilla");
    }

    private static boolean isBorqsClient0(String raw) {
        return !isWebBrowser0(raw);
    }
}
