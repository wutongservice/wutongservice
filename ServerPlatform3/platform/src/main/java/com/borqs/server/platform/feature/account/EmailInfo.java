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

    public static List<EmailInfo> append(List<EmailInfo> emails, EmailInfo email) {
        if (emails == null)
            emails = new ArrayList<EmailInfo>();

        if (email != null) {
            boolean find = false;
            for (EmailInfo email0 : emails) {
                if (email0 != null && StringUtils.equals(email0.getInfo(), email.getInfo())) {
                    find = true;
                    break;
                }
            }

            if (!find)
                emails.add(email);
        }

        return emails;
    }

    public static void remove(List<EmailInfo> emails, String email) {
        if (CollectionUtils.isNotEmpty(emails)) {
            ArrayList<EmailInfo> removing = new ArrayList<EmailInfo>();
            for (EmailInfo email0 : emails) {
                if (email0 != null && StringUtils.equals(email, email0.getInfo()))
                    removing.add(email0);
            }
            emails.removeAll(removing);
        }
    }

    public static String[] getEmailInfo(Collection<EmailInfo> emails) {
        if (CollectionUtils.isEmpty(emails))
            return new String[0];

        LinkedHashSet<String> l = new LinkedHashSet<String>();
        for (EmailInfo email : emails) {
            if (email != null)
                l.add(ObjectUtils.toString(email.getInfo()));
        }
        return l.toArray(new String[l.size()]);
    }
}
