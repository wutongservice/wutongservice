package com.borqs.server.platform.feature.privacy;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.Map;

public class VcardProtector implements PrivacyProtector {
    @Override
    public void protect(Context ctx, Users users, Map<Long, ? extends CharSequence> heAllowed) {
        for (User user : users) {
            if (user == null)
                continue;

            String heAllowed0 = ObjectUtils.toString(heAllowed.get(user.getUserId()));
            String[] heAllowed0a = StringHelper.splitArray(heAllowed0, ",", false);
            if (!ArrayUtils.contains(heAllowed0a, PrivacyResources.RES_VCARD))
                disableVcard(user);
        }
    }

    private void disableVcard(User user) {
        user.setTel();
        user.setEmail();
        user.setAddress();
    }
}
