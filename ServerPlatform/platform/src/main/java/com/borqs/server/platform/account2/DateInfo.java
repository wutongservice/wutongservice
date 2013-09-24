package com.borqs.server.platform.account2;


import com.borqs.server.platform.util.Copyable;
import org.apache.commons.lang.ArrayUtils;

public class DateInfo extends TypedInfo implements Copyable<DateInfo> {

    public static final String TYPE_ANNIVERSARY = "anniversary";
    public static final String TYPE_BIRTHDAY = "birthday";
    public static final String TYPE_OTHER = "other";

    public static final String[] TYPES = {
            TYPE_ANNIVERSARY,
            TYPE_BIRTHDAY,
            TYPE_OTHER,
    };

    public DateInfo() {
    }

    public DateInfo(String type, String info) {
        super(type, info);
    }

    public DateInfo(String type, String info, String label) {
        super(type, info, false, label);
    }

    @Override
    public DateInfo copy() {
        return (DateInfo) new DateInfo().assignFields(type, info, flag, label);
    }

    @Override
    protected boolean checkType(String type) {
        return type.isEmpty() || type.startsWith("x-") || ArrayUtils.contains(TYPES, type);
    }
}
