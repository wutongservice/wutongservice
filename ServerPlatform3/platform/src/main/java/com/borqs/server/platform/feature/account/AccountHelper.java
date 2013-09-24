package com.borqs.server.platform.feature.account;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.SystemHelper;
import com.borqs.server.platform.util.image.ImageMagickHelper;
import org.apache.commons.io.FileUtils;
import org.w3c.tidy.ParserImpl;

import java.io.File;
import java.io.IOException;

public class AccountHelper {
    public static long checkUser(AccountLogic account, Context ctx, long userId) {
        if (!account.hasUser(ctx, userId))
            throw new ServerException(E.INVALID_USER, "User error %s", userId);
        return userId;
    }

    public static long[] checkAllUser(AccountLogic account, Context ctx, long... userIds) {
        if (!account.hasAllUser(ctx, userIds))
            throw new ServerException(E.INVALID_USER);
        return userIds;
    }

    public static long[] checkAnyUser(AccountLogic account, Context ctx, long... userIds) {
        if (!account.hasAnyUser(ctx, userIds))
            throw new ServerException(E.INVALID_USER);
        return userIds;
    }
}
