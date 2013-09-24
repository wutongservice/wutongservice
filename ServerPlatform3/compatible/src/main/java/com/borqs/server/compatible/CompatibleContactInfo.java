package com.borqs.server.compatible;


import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.*;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.util.ObjectHolder;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class CompatibleContactInfo {


    public static Record getLoginEmailsAndPhones(List<TelInfo> tels, List<EmailInfo> emails, List<ImInfo> ims, List<SipAddressInfo> sis) {
        Record rec = new Record();

        if (CollectionUtils.isNotEmpty(tels)) {
            for (TelInfo tel : tels) {
                if (tel == null)
                    continue;

                if (tel.isBinding()) {
                    boolean b = addLoginPhone(rec, tel.getInfo());
                    if (!b)
                        break;
                }
            }
        }

        if (CollectionUtils.isNotEmpty(emails)) {
            for (EmailInfo email : emails) {
                if (email == null)
                    continue;

                if (email.isBinding()) {
                    boolean b = addLoginEmail(rec, email.getInfo());
                    if (!b)
                        break;
                }
            }
        }

        return rec;
    }

    private static boolean addLoginEmail(Record rec, String email) {
        if (!rec.has(CompatibleUser.V1COL_LOGIN_EMAIL1)) {
            rec.put(CompatibleUser.V1COL_LOGIN_EMAIL1, email);
            return true;
        }

        if (!rec.has(CompatibleUser.V1COL_LOGIN_EMAIL2)) {
            rec.put(CompatibleUser.V1COL_LOGIN_EMAIL2, email);
            return true;
        }

        if (!rec.has(CompatibleUser.V1COL_LOGIN_EMAIL3)) {
            rec.put(CompatibleUser.V1COL_LOGIN_EMAIL3, email);
            return true;
        }

        return false;
    }

    private static boolean addLoginPhone(Record rec, String phone) {
        if (!rec.has(CompatibleUser.V1COL_LOGIN_PHONE1)) {
            rec.put(CompatibleUser.V1COL_LOGIN_PHONE1, phone);
            return true;
        }

        if (!rec.has(CompatibleUser.V1COL_LOGIN_PHONE2)) {
            rec.put(CompatibleUser.V1COL_LOGIN_PHONE2, phone);
            return true;
        }

        if (!rec.has(CompatibleUser.V1COL_LOGIN_PHONE3)) {
            rec.put(CompatibleUser.V1COL_LOGIN_PHONE3, phone);
            return true;
        }

        return false;
    }

    public static void serializeContactInfo(JsonGenerator jg, List<TelInfo> tels, List<EmailInfo> emails, List<ImInfo> ims, List<SipAddressInfo> sis) throws IOException {
        // {"mobile_3_telephone_number":"222222","email_2_address":"gg@163.com","mobile_telephone_number":"18618481850","email_address":"e13310@gmail.com","email_3_address":"yy@126.com"}
        Map<String, String> ci = toContactInfo(tels, emails, ims, sis);
        jg.writeStartObject();
        for (Map.Entry<String, String> e : ci.entrySet())
            jg.writeStringField(e.getKey(), e.getValue());
        jg.writeEndObject();
    }

    public static Map<String, String> toContactInfo(List<TelInfo> tels, List<EmailInfo> emails, List<ImInfo> ims, List<SipAddressInfo> sis) {
        LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();

        int i = 1;
        if (CollectionUtils.isNotEmpty(tels)) {
            for (TelInfo tel : tels) {
                if (tel == null)
                    continue;

                if (ArrayHelper.inArray(tel.getType(), TelInfo.TYPE_MOBILE, TelInfo.TYPE_OTHER,
                        TelInfo.TYPE_HOME, TelInfo.TYPE_WORK, TelInfo.TYPE_WORK_MOBILE)) {
                    String key = i == 1 ? "mobile_telephone_number" : String.format("mobile_%s_telephone_number", i);
                    i++;
                    m.put(key, ObjectUtils.toString(tel.getInfo()));
                }
            }
        }

        i = 1;
        if (CollectionUtils.isNotEmpty(emails)) {
            for (EmailInfo email : emails) {
                if (email == null)
                    continue;

                String key = i == 1 ? "email_address" : String.format("email_%s_address", i);
                i++;
                m.put(key, ObjectUtils.toString(email.getInfo()));
            }
        }

        return m;
    }

    public static void fromContactInfo(Map<String, String> newContactInfo,
                                       Collection<String> removedResult,
                                       ObjectHolder<List<TelInfo>> tels,
                                       ObjectHolder<List<EmailInfo>> emails,
                                       ObjectHolder<List<ImInfo>> ims,
                                       ObjectHolder<List<SipAddressInfo>> sis) {
        Map<String, String> oldContactInfo = toContactInfo(tels.value, emails.value, ims.value, sis.value);
        for (Map.Entry<String, String> e : newContactInfo.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();

            if (StringUtils.isEmpty(v)) {
                if (oldContactInfo.containsKey(k)) {
                    removedResult.add(oldContactInfo.get(k));
                }
            } else {
                if ("email_address".equals(k) || Pattern.matches("^email_\\d+_address$", k)) {
                    emails.value = EmailInfo.append(emails.value, new EmailInfo(EmailInfo.TYPE_OTHER, v));
                } else if ("mobile_telephone_number".equals(k) || Pattern.matches("^mobile_\\d+_telephone_number$", k)) {
                    tels.value = TelInfo.append(tels.value, new TelInfo(TelInfo.TYPE_MOBILE, v));
                }
            }
        }
        for (String removed : removedResult) {
            removeInfo(tels.value, removed);
            removeInfo(emails.value, removed);
        }
    }

    private static void removeInfo(List<? extends TypedInfo> cis, String ci) {
        if (cis == null)
            return;

        ArrayList<TypedInfo> removing = new ArrayList<TypedInfo>(8);
        for (TypedInfo ti : cis) {
            if (ti != null && StringUtils.equalsIgnoreCase(ti.getInfo(), ci))
                removing.add(ti);
        }
        cis.removeAll(removing);
    }
}
