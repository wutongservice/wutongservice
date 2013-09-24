package com.borqs.server.test.setting.test1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;

public class AccountMock implements AccountLogic {
    @Override
    public User createUser(Context ctx, User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean destroyUser(Context ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean recoverUser(Context ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean update(Context ctx, User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String resetRandomPassword(Context ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updatePassword(Context ctx, String oldPwd, String newPwd, boolean verify) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Users getUsers(Context ctx, String[] expCols, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User getUser(Context ctx, String[] expCols, long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPassword(Context ctx, long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAllUser(Context ctx, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasAnyUser(Context ctx, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasUser(Context ctx, long userId) {
        return userId == SettingLogicTest1.USER1_ID;
    }

    @Override
    public long[] getExistsIds(Context ctx, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Users search(Context ctx, String word, String[] expCols, Page page) {
        throw new UnsupportedOperationException();
    }
}
