package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.logic.Logic;

public interface AccountLogic extends Logic {
    User createUser(Context ctx, User user);

    boolean destroyUser(Context ctx);

    boolean recoverUser(Context ctx);

    boolean update(Context ctx, User user);

    String resetRandomPassword(Context ctx);

    void updatePassword(Context ctx, String oldPwd, String newPwd, boolean verify);

    Users getUsers(Context ctx, String[] expCols, long... userIds);

    User getUser(Context ctx, String[] expCols, long userId);

    String getPassword(Context ctx, long userId);

    boolean hasAllUser(Context ctx, long... userIds);

    boolean hasAnyUser(Context ctx, long... userIds);

    boolean hasUser(Context ctx, long userId);

    long[] getExistsIds(Context ctx, long... userIds);

    Users search(Context ctx, String word, String[] expCols, Page page);
}
