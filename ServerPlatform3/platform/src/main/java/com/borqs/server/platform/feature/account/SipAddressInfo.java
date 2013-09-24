package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.util.Copyable;
import org.apache.commons.lang.ArrayUtils;

public class SipAddressInfo extends TypedInfo implements Copyable<SipAddressInfo> {

    public static final String TYPE_HOME = "home";
    public static final String TYPE_WORK = "work";
    public static final String TYPE_OTHER = "other";

    public static final String[] TYPES = {
            TYPE_HOME,
            TYPE_WORK,
            TYPE_OTHER,
    };

    public SipAddressInfo() {
    }

    public SipAddressInfo(String type, String info) {
        super(type, info);
    }

    public SipAddressInfo(String type, String info, boolean primary, String label) {
        super(type, info, primary, label);
    }

    @Override
    public SipAddressInfo copy() {
        return (SipAddressInfo)new SipAddressInfo().assignFields(type, info, flag, label);
    }

    @Override
    protected boolean checkType(String type) {
        return type.isEmpty() || type.startsWith("x-") || ArrayUtils.contains(TYPES, type);
    }
}
