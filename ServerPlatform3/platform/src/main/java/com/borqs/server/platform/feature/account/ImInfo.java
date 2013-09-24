package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.util.Copyable;
import org.apache.commons.lang.ArrayUtils;

public class ImInfo extends TypedInfo implements Copyable<ImInfo> {

    public static final String TYPE_AIM = "aim";
    public static final String TYPE_MSN = "msn";
    public static final String TYPE_YAHOO = "yahoo";
    public static final String TYPE_SKYPE = "skype";
    public static final String TYPE_QQ = "qq";
    public static final String TYPE_GOOGLE_TALK = "google_talk";
    public static final String TYPE_ICQ = "icq";
    public static final String TYPE_JABBER = "jabber";
    public static final String TYPE_NETMEETING = "netmeeting";

    public static final String[] TYPES = {
            TYPE_AIM,
            TYPE_MSN,
            TYPE_YAHOO,
            TYPE_SKYPE,
            TYPE_QQ,
            TYPE_GOOGLE_TALK,
            TYPE_ICQ,
            TYPE_JABBER,
            TYPE_NETMEETING,
    };

    public ImInfo() {
    }

    public ImInfo(String type, String info) {
        super(type, info);
    }

    public ImInfo(String type, String info, boolean primary, String label) {
        super(type, info, primary, label);
    }

    @Override
    public ImInfo copy() {
        return (ImInfo)new ImInfo().assignFields(type, info, flag, label);
    }

    @Override
    protected boolean checkType(String type) {
        return type.isEmpty() || type.startsWith("x-") || ArrayUtils.contains(TYPES, type);
    }
}
