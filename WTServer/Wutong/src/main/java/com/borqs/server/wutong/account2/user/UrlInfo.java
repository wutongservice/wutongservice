package com.borqs.server.wutong.account2.user;


import com.borqs.server.base.util.Copyable;
import org.apache.commons.lang.ArrayUtils;

public class UrlInfo extends TypedInfo implements Copyable<UrlInfo> {

    public static final String TYPE_HOMEPAGE = "homepage";
    public static final String TYPE_BLOG = "blog";
    public static final String TYPE_PROFILE = "profile";
    public static final String TYPE_HOME = "home";
    public static final String TYPE_WORK= "work";
    public static final String TYPE_FTP = "ftp";
    public static final String TYPE_OTHER = "other";

    public static final String[] TYPES = {
            TYPE_HOMEPAGE,
            TYPE_BLOG,
            TYPE_PROFILE,
            TYPE_HOME,
            TYPE_WORK,
            TYPE_FTP,
            TYPE_OTHER,
    };

    public UrlInfo() {
    }

    public UrlInfo(String type, String info) {
        super(type, info);
    }

    public UrlInfo(String type, String info, boolean primary, String label) {
        super(type, info, primary, label);
    }

    @Override
    public UrlInfo copy() {
        return (UrlInfo) new UrlInfo().assignFields(type, info, flag, label);
    }

    @Override
    protected boolean checkType(String type) {
        return type.isEmpty() || type.startsWith("x-") || ArrayUtils.contains(TYPES, type);
    }
}
