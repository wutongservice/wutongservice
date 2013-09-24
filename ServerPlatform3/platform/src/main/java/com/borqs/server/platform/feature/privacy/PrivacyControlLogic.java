package com.borqs.server.platform.feature.privacy;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

import java.util.List;
import java.util.Map;

public interface PrivacyControlLogic extends Logic {
    void setPrivacy(Context ctx, PrivacyEntry... entries);
    List<PrivacyEntry> getPrivacy(Context ctx, long userId, String... res);
    void clearPrivacy(Context ctx, String... res);

    boolean check(Context ctx, long viewerId, String res, long userId);
    Map<Long, Boolean> check(Context ctx, long viewerId, String res, long[] userIds);

    AllowedIds getAllowIds(Context ctx, long userId, String res);
    void mutualAllow(Context ctx, String res, long userId);
}
