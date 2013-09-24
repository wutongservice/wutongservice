package com.borqs.server.impl.account;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.expansion.ExpansionHelper;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfo;
import com.borqs.server.platform.feature.TargetInfoFetcher;
import com.borqs.server.platform.feature.account.*;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.feature.status.Status;
import com.borqs.server.platform.feature.status.StatusHook;
import com.borqs.server.platform.feature.status.StatusLogic;
import com.borqs.server.platform.fts.FTResult;
import com.borqs.server.platform.fts.FTS;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ChineseSegmentHelper;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.ParamChecker;
import com.borqs.server.platform.util.RandomHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountImpl implements AccountLogic, StatusLogic, TargetInfoFetcher.Provider {

    private static final Logger L = Logger.get(AccountImpl.class);

    // db
    private final UserDb db = new UserDb();
    private FTS userFts;

    // hook
    private List<UserHook> createUserHooks;
    private List<UserIdHook> destroyUserHooks;
    private List<UserIdHook> recoverUserHooks;
    private List<UserHook> updateUserHooks;
    private List<StatusHook> updateStatusHooks;

    // expansion
    private List<UserExpansion> expansions;

    public AccountImpl() {
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getUserTable() {
        return db.getUserTable();
    }

    public void setUserTable(Table userTable) {
        this.db.setUserTable(userTable);
    }

    public Table getPropertyTable() {
        return db.getPropertyTable();
    }

    public void setPropertyTable(Table propertyTable) {
        this.db.setPropertyTable(propertyTable);
    }

    public FTS getUserFts() {
        return userFts;
    }

    public void setUserFts(FTS userFts) {
        this.userFts = userFts;
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

    public List<StatusHook> getUpdateStatusHooks() {
        return updateStatusHooks;
    }

    public void setUpdateStatusHooks(List<StatusHook> updateStatusHooks) {
        this.updateStatusHooks = updateStatusHooks;
    }

    public List<UserExpansion> getExpansions() {
        return expansions;
    }

    public void setExpansions(List<UserExpansion> expansions) {
        this.expansions = expansions;
    }

    @Override
    public User createUser(final Context ctx, final User user0) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "createUser", ctx, "user", user0);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("user", user0);
            final User user = user0.copy();
            ParamChecker.notEmpty("user.password", user.getPassword());

            HookHelper.before(createUserHooks, ctx, user);

            User r = db.createUser(ctx, user);
            userFts.saveDoc(ctx, UserFts.toFtDoc(r));
            OpLine.append(ctx, Actions.CREATE, user.toJson(null, false), PeopleId.user(r.getUserId()));

            HookHelper.after(createUserHooks, ctx, r);

            LC.endCall();
            return r;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }

    }

    @Override
    public boolean destroyUser(final Context ctx) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "destroyUser", ctx);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("ctx", ctx.getViewer() > 0, "Illegal viewer");

            long userId = ctx.getViewer();
            HookHelper.before(destroyUserHooks, ctx, userId);
            boolean b = db.destroyUser(ctx, userId);
            OpLine.append(ctx, Actions.DESTROY, userId, ctx.getViewerAsPeople());
            HookHelper.after(destroyUserHooks, ctx, userId);

            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean recoverUser(final Context ctx) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "recoverUser", ctx);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("ctx", ctx.getViewer() > 0, "Illegal viewer");

            long userId = ctx.getViewer();

            HookHelper.before(recoverUserHooks, ctx, userId);
            boolean r = db.recoverUser(ctx, userId);
            OpLine.append(ctx, Actions.RECOVER, userId, ctx.getViewerAsPeople());
            HookHelper.after(recoverUserHooks, ctx, userId);

            LC.endCall();
            return r;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean update(Context ctx, User user) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "update", ctx, "user", user);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("user", user);

            long userId = ctx.getViewer();
            ParamChecker.mustTrue("user", userId == user.getUserId(), "Illegal user.user_id");
            boolean b = update0(ctx, user);
            userFts.saveDoc(ctx, UserFts.toFtDoc(user));

            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    private boolean update0(final Context ctx, final User user) {
        HookHelper.before(updateUserHooks, ctx, user);

        boolean b = db.update(ctx, user);
        OpLine.append(ctx, Actions.UPDATE, user.toJson(null, false), ctx.getViewerAsPeople());
        HookHelper.after(updateUserHooks, ctx, user);
        return b;
    }

    @Override
    public String resetRandomPassword(Context ctx) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "resetRandomPassword", ctx);

        try {
            String newPwd = RandomHelper.generateRandomNumberString(8);
            updatePassword(ctx, null, Encoders.md5Hex(newPwd), false);
            LC.endCall();
            return newPwd;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void updatePassword(Context ctx, String oldPwd, String newPwd, boolean verify) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "updatePassword", ctx,
                "oldPwd", oldPwd, "newPwd", newPwd, "verify", verify);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("newPwd", newPwd);
            ParamChecker.mustTrue("ctx", ctx.getViewer() > 0, "Illegal viewer");

            long userId = ctx.getViewer();
            if (verify && oldPwd != null) {
                User user = getUser(ctx, null, userId);
                if (!StringUtils.equals(oldPwd, user.getPassword()))
                    throw new ServerException(E.INVALID_USER_OR_PASSWORD, "old password error");
            }
            User newUser = new User(userId);
            newUser.setPassword(newPwd);
            update0(ctx, newUser);

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Users getUsers(Context ctx, String[] expCols, long... userIds) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "getUsers", ctx, "userIds", userIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            if (ArrayUtils.isEmpty(userIds)) {
                LC.endCall();
                return new Users();
            } else {
                Users users = db.getUsers(ctx, userIds);
                // resort by userIds
                users.sortByUserIds(userIds);

                ExpansionHelper.expand(expansions, ctx, expCols, users);

                LC.endCall();
                return users;
            }
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public User getUser(Context ctx, String[] expCols, long userId) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "getUser", ctx, "userId", userId);

        try {
            List<User> users = getUsers(ctx, expCols, userId);
            User user = CollectionUtils.isEmpty(users) ? null : users.get(0);

            LC.endCall();
            return user;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public String getPassword(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "getPassword", ctx, "userId", userId);
        try {
            ParamChecker.notNull("ctx", ctx);
            String pwd = db.getPassword(ctx, userId);
            LC.endCall();
            return pwd;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAllUser(Context ctx, long... userIds) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "hasAllUser", ctx, "userIds", userIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = db.hasAllUser(ctx, userIds);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public boolean hasAnyUser(Context ctx, long... userIds) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "hasAnyUser", ctx, "userIds", userIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = db.hasAnyUser(ctx, userIds);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public boolean hasUser(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "hasUser", ctx, "userId", userId);
        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = db.hasUser(ctx, userId);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public long[] getExistsIds(Context ctx, long... userIds) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "getExistsIds", ctx, "userIds", userIds);
        try {
            ParamChecker.notNull("ctx", ctx);

            if (userIds.length == 0)
                return new long[0];

            long[] ids = db.getExistsIds(ctx, userIds);
            LC.endCall();
            return ids;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Status getStatus(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "getStatus", ctx,
                "userId", userId);
        try {
            ParamChecker.notNull("ctx", ctx);
            Status st = db.getStatus(ctx, userId);
            LC.endCall();
            return st;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Long, Status> getStatuses(Context ctx, long... userIds) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "getStatuses", ctx,
                "userIds", userIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            Map<Long, Status> sts;
            if (ArrayUtils.isNotEmpty(userIds)) {
                sts = db.getStatuses(ctx, userIds);
            } else {
                sts = new HashMap<Long, Status>();
            }
            LC.endCall();
            return sts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void updateStatus(Context ctx, Status status) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "updateStatus", ctx,
                "status", status);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("status", status);

            HookHelper.before(updateStatusHooks, ctx, status);
            db.updateStatus(ctx, status);
            OpLine.append(ctx, Actions.STATUS, status.status, ctx.getViewerAsPeople());
            HookHelper.after(updateStatusHooks, ctx, status);
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    // TODO: optimize
    @Override
    public Users search(Context ctx, String word, String[] expCols, Page page) {
        final LogCall LC = LogCall.startCall(L, AccountImpl.class, "search", ctx,
                "word", word);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notBlank("word", word);
            ParamChecker.notNull("page", page);
            ParamChecker.mustTrue("page.page", page.page == 0, "page.page must be 0");

            int count = (int)page.count;

            String ftNameWord = ChineseSegmentHelper.segmentNameString(word);
            String ftWord = ChineseSegmentHelper.segmentString(word);

            //long[] userIds = db.searchUserIds(ctx, opts, word, page);

            // SELF_MATCH > NAME_FULL_MATCH + NAME_FULLTEXT_MATCH + OTHER_FULLTEXT_MATCH + 1.0
            // NAME_FULL_MATCH > NAME_FULLTEXT_MATCH + OTHER_FULLTEXT_MATCH + 1.0
            // NAME_FULLTEXT_MATCH > OTHER_FULLTEXT_MATCH + 1.0
            final double SELF_MATCH = 100.0;
            final double NAME_FULL_MATCH = 42.0;
            final double NAME_FULLTEXT_MATCH = 18.0;
            final double OTHER_FULLTEXT_MATCH = 10.0;

            FTResult ftr = new FTResult();

            // search self first
            if (ctx.isLogined()) {
                long viewer = ctx.getViewer();
                userFts.search(ctx, ftr, UserFts.CATEGORY, ftNameWord,
                        new FTS.Options().setMethod(FTS.Options.METHOD_FT_MATCH).setInIds(viewer).setIncrWeight(SELF_MATCH),
                        1);
            }

            // search full match by name
            userFts.search(ctx, ftr, UserFts.CATEGORY, word,
                    new FTS.Options().setMethod(FTS.Options.METHOD_EQUALS).setInFields(UserFts.NAME, UserFts.NAME_FULL_PINYIN, UserFts.NAME_SHORT_PINYIN).setIncrWeight(NAME_FULL_MATCH),
                    count);


            // search full text match by name
            userFts.search(ctx, ftr, UserFts.CATEGORY, ftNameWord,
                    new FTS.Options().setMethod(FTS.Options.METHOD_FT_MATCH).setInFields(UserFts.NAME, UserFts.NAME_FULL_PINYIN).setIncrWeight(NAME_FULLTEXT_MATCH),
                    count);

            // search other
            userFts.search(ctx, ftr, UserFts.CATEGORY, ftWord,
                    new FTS.Options().setMethod(FTS.Options.METHOD_FT_MATCH).setIncrWeight(OTHER_FULLTEXT_MATCH),
                    count);

            for (FTResult.Entry entry : ftr) {
                String hitName = MapUtils.getString(entry.hitContents, UserFts.NAME, null);
                if (hitName != null) {
                    entry.weight += NameInfo.calculateSimilarity(word, hitName);
                }
            }

            ftr.sortByWeight(false);
            long[] userIds = ftr.getDocIdsAsLong();
            return getUsers(ctx, expCols, userIds);
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public TargetInfo[] fetchTargetInfo(Context ctx, Target... targets) {
        long[] userIds = Target.getIdsAsLong(targets, Target.USER);
        Users users = getUsers(ctx, User.STANDARD_COLUMNS, userIds);
        return users.getTargetInfo();
    }
}
