package com.borqs.server.platform.account2;


import com.borqs.server.E;
import com.borqs.server.ServerException;

public class AccountHelper {
    public static long checkUser(AccountLogic account, long userId) {
        if (!account.hasUser(userId))
            throw new ServerException(E.INVALID_USER, "User error %s", userId);
        return userId;
    }

    public static long[] checkAllUser(AccountLogic account, long... userIds) {
        if (!account.hasAllUser( userIds))
            throw new ServerException(E.INVALID_USER);
        return userIds;
    }

    public static long[] checkAnyUser(AccountLogic account, long... userIds) {
        if (!account.hasAnyUser( userIds))
            throw new ServerException(E.INVALID_USER);
        return userIds;
    }
}
