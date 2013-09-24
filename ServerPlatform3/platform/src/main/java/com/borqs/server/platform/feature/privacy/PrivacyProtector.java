package com.borqs.server.platform.feature.privacy;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.Users;

import java.util.Map;


public interface PrivacyProtector {
    public void protect(Context ctx, Users users, Map<Long, ? extends CharSequence> heAllowed);
}
