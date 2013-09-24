package com.borqs.server.wutong.account2.user;


import com.borqs.server.base.util.Copyable;
import org.apache.commons.lang.ArrayUtils;

public class EmailInfo extends TypedInfo implements Copyable<EmailInfo> {

    public static final String TYPE_HOME = "home";
    public static final String TYPE_WORK = "work";
    public static final String TYPE_MOBILE = "mobile";
    public static final String TYPE_OTHER = "other";

    public static final String[] TYPES = {
            TYPE_HOME,
            TYPE_WORK,
            TYPE_MOBILE,
            TYPE_OTHER,
    };

    public EmailInfo() {
    }

    public EmailInfo(String type, String info) {
        super(type, info);
    }

    public EmailInfo(String type, String info, boolean primary, String label) {
        super(type, info, primary, label);
    }

    @Override
    public EmailInfo copy() {
        return (EmailInfo)new EmailInfo().assignFields(type, info, flag, label);
    }

    @Override
    protected boolean checkType(String type) {
        return type.isEmpty() || type.startsWith("x-") || ArrayUtils.contains(TYPES, type);
    }
}
