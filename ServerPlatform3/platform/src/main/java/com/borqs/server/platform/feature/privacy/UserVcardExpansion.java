package com.borqs.server.platform.feature.privacy;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Map;

@Deprecated
public class UserVcardExpansion implements UserExpansion {
    public PrivacyControlLogic getPrivacy() {
        return privacy;
    }

    public void setPrivacy(PrivacyControlLogic privacy) {
        this.privacy = privacy;
    }

    private PrivacyControlLogic privacy;

    @Override
    public void expand(Context ctx, String[] expCols, Users users) {
        if (!ArrayUtils.contains(expCols, "profile_privacy")
                || CollectionUtils.isEmpty(users))
            return;

        if(!ctx.isPrivacyEnabled()) {
            for (User user : users)
                user.setAddon("profile_privacy", false);
            return;
        }

        Map<Long, Boolean> m = privacy.check(ctx, ctx.getViewer(), PrivacyResources.RES_VCARD, users.getUserIds());
        for(Map.Entry<Long, Boolean> e : m.entrySet()) {
            long userId = e.getKey();
            boolean allow = e.getValue();

            User user = users.getUser(userId);
            user.setAddon("profile_privacy", !allow);
            if (!allow) {                
                user.setEmail(new ArrayList<EmailInfo>());
                user.setTel(new ArrayList<TelInfo>());
            }
        }
    }
}
