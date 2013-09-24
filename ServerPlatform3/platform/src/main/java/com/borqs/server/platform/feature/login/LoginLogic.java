package com.borqs.server.platform.feature.login;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

public interface LoginLogic extends Logic {
    long whoLogined(Context ctx, String ticket);

    boolean hasTicket(Context ctx, String ticket);

    long validatePassword(Context ctx, String name, String password);

    LoginResult login(Context ctx, String name, String password, int app);

    boolean logout(Context ctx, String ticket);
}
