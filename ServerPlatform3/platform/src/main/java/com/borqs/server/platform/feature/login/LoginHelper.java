package com.borqs.server.platform.feature.login;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;

public class LoginHelper {
    public static long checkTicket(LoginLogic login, Context ctx, String ticket) {
        long userId = login.whoLogined(ctx, ticket);
        if (userId <= 0)
            throw new ServerException(E.INVALID_TICKET);
        return userId;
    }
}
