package com.borqs.server.wutong.account2.user;


import com.borqs.server.base.util.Copyable;
import org.apache.commons.lang.ArrayUtils;

public class TelInfo extends TypedInfo implements Copyable<TelInfo> {

    public static final String TYPE_HOME = "home";
    public static final String TYPE_MOBILE = "mobile";
    public static final String TYPE_WORK = "work";
    public static final String TYPE_FAX_WORK = "fax_work";
    public static final String TYPE_FAX_HOME = "fax_home";
    public static final String TYPE_PAGER = "pager";
    public static final String TYPE_OTHER = "other";
    public static final String TYPE_CALLBACK = "callback";
    public static final String TYPE_ISDN = "isdn";
    public static final String TYPE_COMPANY_MAIN = "company_main";
    public static final String TYPE_CAR = "car";
    public static final String TYPE_MAIN = "main";
    public static final String TYPE_OTHER_FAX = "other_fax";
    public static final String TYPE_RADIO = "radio";
    public static final String TYPE_TELEX = "telex";
    public static final String TYPE_TTY_TTD = "tty_ttd";
    public static final String TYPE_WORK_MOBILE = "work_mobile";
    public static final String TYPE_WORK_PAGER = "work_pager";
    public static final String TYPE_ASSISTANT = "assistant";
    public static final String TYPE_MMS = "mms";

    public static final String[] TYPES = {
            TYPE_HOME,
            TYPE_MOBILE,
            TYPE_WORK,
            TYPE_FAX_WORK,
            TYPE_FAX_HOME,
            TYPE_PAGER,
            TYPE_OTHER,
            TYPE_CALLBACK,
            TYPE_ISDN,
            TYPE_COMPANY_MAIN,
            TYPE_CAR,
            TYPE_MAIN,
            TYPE_OTHER_FAX,
            TYPE_RADIO,
            TYPE_TELEX,
            TYPE_TTY_TTD,
            TYPE_WORK_MOBILE,
            TYPE_WORK_PAGER,
            TYPE_ASSISTANT,
            TYPE_MMS,
    };

    public TelInfo() {
    }

    public TelInfo(String type, String info) {
        super(type, info);
    }

    public TelInfo(String type, String info, boolean primary, String label) {
        super(type, info, primary, label);
    }

    @Override
    public TelInfo copy() {
        return (TelInfo)new TelInfo().assignFields(type, info, flag, label);
    }

    @Override
    protected boolean checkType(String type) {
        return type.isEmpty() || type.startsWith("x-") || ArrayUtils.contains(TYPES, type);
    }
}
