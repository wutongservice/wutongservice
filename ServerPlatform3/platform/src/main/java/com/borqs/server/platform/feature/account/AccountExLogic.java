package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

public interface AccountExLogic extends Logic {
    long findByOpenfacePhone(Context ctx, String phone);
}
