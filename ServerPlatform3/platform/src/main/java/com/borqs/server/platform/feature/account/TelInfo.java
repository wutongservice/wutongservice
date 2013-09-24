package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.util.Copyable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

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
        return (TelInfo) new TelInfo().assignFields(type, info, flag, label);
    }

    @Override
    protected boolean checkType(String type) {
        return type.isEmpty() || type.startsWith("x-") || ArrayUtils.contains(TYPES, type);
    }

    public static List<TelInfo> append(List<TelInfo> tels, TelInfo tel) {
        if (tels == null)
            tels = new ArrayList<TelInfo>();

        if (tel != null) {
            boolean find = false;
            for (TelInfo tel0 : tels) {
                if (tel0 != null && StringUtils.equals(tel0.getInfo(), tel.getInfo())) {
                    find = true;
                    break;
                }
            }

            if (!find)
                tels.add(tel);
        }

        return tels;
    }

    public static void remove(List<TelInfo> tels, String tel) {
        if (CollectionUtils.isNotEmpty(tels)) {
            ArrayList<TelInfo> removing = new ArrayList<TelInfo>();
            for (TelInfo tel0 : tels) {
                if (tel0 != null && StringUtils.equals(tel, tel0.getInfo()))
                    removing.add(tel0);
            }
            tels.removeAll(removing);
        }
    }

    public static String[] getTelInfo(Collection<TelInfo> tels) {
        if (CollectionUtils.isEmpty(tels))
            return new String[0];

        LinkedHashSet<String> l = new LinkedHashSet<String>();
        for (TelInfo tel : tels) {
            if (tel != null)
                l.add(ObjectUtils.toString(tel.getInfo()));
        }
        return l.toArray(new String[l.size()]);
    }
}
