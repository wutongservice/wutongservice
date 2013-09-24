package com.borqs.server.platform.test;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.expansion.ExpansionHelper;
import com.borqs.server.platform.feature.account.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.RandomHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class TestAccount implements AccountLogic {
    public static final long LOW_USER_ID = 10001;

    protected final AtomicLong idInc = new AtomicLong();
    protected final Map<Long, UserEntry> users = new HashMap<Long, UserEntry>();

    // hook
    private List<UserHook> createUserHooks;
    private List<UserIdHook> destroyUserHooks;
    private List<UserIdHook> recoverUserHooks;
    private List<UserHook> updateUserHooks;

    // expansion
    private List<UserExpansion> expansions;

    public TestAccount() {
        reset();
    }

    public long[] getAllUserIds() {
        long[] userIds =  CollectionsHelper.toLongArray(users.keySet());
        Arrays.sort(userIds);
        return userIds;
    }

    public List<UserHook> getCreateUserHooks() {
        return createUserHooks;
    }

    public void setCreateUserHooks(List<UserHook> createUserHooks) {
        this.createUserHooks = createUserHooks;
    }

    public List<UserIdHook> getDestroyUserHooks() {
        return destroyUserHooks;
    }

    public void setDestroyUserHooks(List<UserIdHook> destroyUserHooks) {
        this.destroyUserHooks = destroyUserHooks;
    }

    public List<UserIdHook> getRecoverUserHooks() {
        return recoverUserHooks;
    }

    public void setRecoverUserHooks(List<UserIdHook> recoverUserHooks) {
        this.recoverUserHooks = recoverUserHooks;
    }

    public List<UserHook> getUpdateUserHooks() {
        return updateUserHooks;
    }

    public void setUpdateUserHooks(List<UserHook> updateUserHooks) {
        this.updateUserHooks = updateUserHooks;
    }

    public List<UserExpansion> getExpansions() {
        return expansions;
    }

    public void setExpansions(List<UserExpansion> expansions) {
        this.expansions = expansions;
    }

    public void reset() {
        resetIdCounter(LOW_USER_ID);
    }

    public void resetIdCounter(long lowUserId) {
        idInc.set(lowUserId);
        users.clear();
    }

    @Override
    public User createUser(Context ctx, User user0) {
        User user = user0.copy();
        long userId = idInc.getAndIncrement();
        user.setUserId(userId);
        users.put(userId, new UserEntry(user));
        return user;
    }

    @Override
    public boolean destroyUser(Context ctx) {
        UserEntry entry = users.get(ctx.getViewer());
        if (entry != null)
            entry.enabled = false;
        return entry != null;
    }

    @Override
    public boolean recoverUser(Context ctx) {
        UserEntry entry = users.get(ctx.getViewer());
        if (entry != null)
            entry.enabled = true;
        return entry != null;
    }

    @Override
    public boolean update(Context ctx, User user0) {
        UserEntry entry = users.get(user0.getUserId());
        if (entry != null)
            entry.user = user0.copy();
        return entry != null;
    }

    @Override
    public String resetRandomPassword(Context ctx) {
        String newPwd = RandomHelper.generateRandomNumberString(6);
        updatePassword(ctx, "", Encoders.md5Hex(newPwd), false);
        return newPwd;
    }

    @Override
    public void updatePassword(Context ctx, String oldPwd, String newPwd, boolean verify) {
        UserEntry entry = users.get(ctx.getViewer());
        if (entry != null) {
            User user = entry.user;
            if (verify) {
                if (!StringUtils.equals(oldPwd, user.getPassword()))
                    throw new ServerException(E.INVALID_USER_OR_PASSWORD, "");
                user.setPassword(newPwd);
            } else {
                user.setPassword(newPwd);
            }
        }
    }

    @Override
    public Users getUsers(Context ctx, String[] expCols, long... userIds) {
        Users users = new Users();
        for (long userId : userIds) {
            UserEntry entry = this.users.get(userId);
            if (entry != null && entry.enabled && entry.user != null)
                users.add(entry.user.copy());
        }
        ExpansionHelper.expand(expansions, ctx, expCols, users);
        return users;
    }

    @Override
    public User getUser(Context ctx, String[] expCols, long userId) {
        List<User> users = getUsers(ctx, expCols, userId);
        return CollectionUtils.isNotEmpty(users) ? users.get(0) : null;
    }

    @Override
    public String getPassword(Context ctx, long userId) {
        User user = getUser(ctx, null, userId);
        return user != null ? user.getPassword() : null;
    }

    @Override
    public boolean hasAllUser(Context ctx, long... userIds) {
        for (long userId : userIds) {
            UserEntry entry = users.get(userId);
            if (entry == null || !entry.enabled || entry.user == null)
                return false;
        }
        return true;
    }

    @Override
    public boolean hasAnyUser(Context ctx, long... userIds) {
        for (long userId : userIds) {
            UserEntry entry = users.get(userId);
            if (entry != null && entry.enabled && entry.user != null)
                return true;
        }
        return false;
    }

    @Override
    public boolean hasUser(Context ctx, long userId) {
        UserEntry entry = users.get(userId);
        return entry != null && entry.enabled && entry.user != null;
    }

    @Override
    public long[] getExistsIds(Context ctx, long... userIds) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (long userId : userIds) {
            if (hasUser(ctx, userId))
                l.add(userId);
        }
        return CollectionsHelper.toLongArray(l);
    }

    private static class UserEntry {
        boolean enabled = true;
        User user;

        private UserEntry(User user) {
            this.user = user;
        }
    }

    @Override
    public Users search(Context ctx, String word, String[] expCols, Page page) {
        throw new UnsupportedOperationException();
    }
}
