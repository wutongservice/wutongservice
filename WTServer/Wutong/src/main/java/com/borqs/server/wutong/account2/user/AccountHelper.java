package com.borqs.server.wutong.account2.user;


import com.borqs.server.ServerException;
import com.borqs.server.base.context.Context;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;

public class AccountHelper {
    public static long checkUser(Context ctx,AccountLogic account, long userId) {
        if (!account.hasUser(ctx,userId))
            throw new ServerException(WutongErrors.USER_CHECK_ERROR, "User error %s", userId);
        return userId;
    }

    public static long[] checkAllUser(Context ctx,AccountLogic account, long... userIds) {
        if (!account.hasAllUser(ctx,userIds))
            throw new ServerException(WutongErrors.USER_CHECK_ERROR);
        return userIds;
    }

    public static long[] checkAnyUser(Context ctx,AccountLogic account, long... userIds) {
        if (!account.hasAnyUser(ctx, userIds))
            throw new ServerException(WutongErrors.USER_CHECK_ERROR);
        return userIds;
    }
}
