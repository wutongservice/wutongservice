package com.borqs.server.platform.feature.verify;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

public interface MessageVerifyLogic extends Logic {
    int nextSendSpan(Context ctx, String phone);
    void sendCode(Context ctx, String phone);
    int verifyCode(Context ctx, String phone, String code);
}
