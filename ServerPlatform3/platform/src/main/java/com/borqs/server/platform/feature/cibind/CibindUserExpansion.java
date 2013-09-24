package com.borqs.server.platform.feature.cibind;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CibindUserExpansion implements UserExpansion {

    private CibindLogic cibind;

    public CibindUserExpansion() {
    }

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Users data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (!ctx.isLogined())
            return;

        long[] userIds = data.getUserIds();
        Map<Long, BindingInfo[]> m = cibind.getBindings(ctx, userIds);
        for (User user : data) {
            long userId = user.getUserId();
            if (userId <= 0)
                continue;

            BindingInfo[] bis = m.get(userId);
            if (ArrayUtils.isNotEmpty(bis))
                mergeBindingInfo(user, bis);
        }
    }

    private static void mergeBindingInfo(User user, BindingInfo[] bis) {
        for (BindingInfo bi : bis) {
            if (bi.getType().equals(BindingInfo.EMAIL)) {
                List<EmailInfo> emails = user.getEmail();
                EmailInfo ei = findEmail(emails, bi.getInfo());
                if (ei == null) {
                    if (emails == null)
                        emails = new ArrayList<EmailInfo>();

                    ei = new EmailInfo(EmailInfo.TYPE_OTHER, bi.getInfo());
                    emails.add(ei);
                    user.setEmail(emails);
                }
                ei.setBinding(true);
            }

            if (bi.getType().equals(BindingInfo.MOBILE_TEL)) {
                List<TelInfo> tels = user.getTel();
                TelInfo ti = findTel(tels, bi.getInfo());
                if (ti == null) {
                    if (tels == null)
                        tels = new ArrayList<TelInfo>();

                    ti = new TelInfo(TelInfo.TYPE_MOBILE, bi.getInfo());
                    tels.add(ti);
                    user.setTel(tels);
                }
                ti.setBinding(true);
            }
        }
    }

    private static EmailInfo findEmail(List<EmailInfo> emails, String email) {
        if (CollectionUtils.isEmpty(emails))
            return null;

        for (EmailInfo e : emails) {
            if (e == null)
                continue;

            if (StringUtils.equals(e.getInfo(), email))
                return e;
        }
        return null;
    }

    private static TelInfo findTel(List<TelInfo> tels, String tel) {
        if (CollectionUtils.isEmpty(tels))
            return null;

        for (TelInfo e : tels) {
            if (e == null)
                continue;

            if (StringUtils.equals(e.getInfo(), tel))
                return e;
        }
        return null;
    }
}
