package com.borqs.server.impl.conversation;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConversationImpl implements ConversationLogic {
    private static final Logger L = Logger.get(ConversationImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final ConversationDb db = new ConversationDb();

    public ConversationImpl() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getConvTable0() {
        return db.getConvTable0();
    }

    public void setConvTable0(Table convTable0) {
        db.setConvTable0(convTable0);
    }

    public Table getConvTable1() {
        return db.getConvTable1();
    }

    public void setConvTable1(Table convTable1) {
        db.setConvTable1(convTable1);
    }

    @Override
    public boolean has(Context ctx, long userId, Target target, int reason) {
        final LogCall LC = LogCall.startCall(L, ConversationImpl.class, "has",
                ctx, "userId", userId, "target", target, "reason", reason);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Invalid user id");
            ParamChecker.notNull("target", target);

            Map<Target, Long> m = db.getCount(ctx, reason, userId, target);
            long count = m.get(target);
            LC.endCall();
            return  count > 0;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Target, Boolean> has(Context ctx, long userId, Target[] targets, int reason) {
        final LogCall LC = LogCall.startCall(L, ConversationImpl.class, "has",
                ctx, "userId", userId, "targets", targets, "reason", reason);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Invalid user id");
            ParamChecker.notNull("targets", targets);

            LinkedHashMap<Target, Boolean> m = new LinkedHashMap<Target, Boolean>();
            if (targets.length == 0)
                return m;
            if (targets.length == 1) {
                m.put(targets[0], has(ctx, userId, targets[0], reason));
                return m;
            }

            Map<Target, Long> map = db.getCount(ctx, reason, userId, targets);
            for (Map.Entry<Target, Long> entry : map.entrySet()) {
                m.put(entry.getKey(), entry.getValue() > 0);
            }
            LC.endCall();
            return  m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void create(Context ctx, ConversationBase... conversations) {
        final LogCall LC = LogCall.startCall(L, ConversationImpl.class, "create",
                ctx, "conversations", conversations);
        
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("conversations", conversations);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (conversations.length == 0)
                return;

            Conversation[] convs = new Conversation[conversations.length];
            long createdTime = DateHelper.nowMillis();
            for (int i = 0; i < conversations.length; i++) {
                convs[i] = new Conversation();
                convs[i].setTarget(conversations[i].getTarget());
                convs[i].setReason(conversations[i].getReason());
                convs[i].setUser(userId);
                convs[i].setCreatedTime(createdTime);
            }
            
            db.create(ctx, convs);
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void delete(Context ctx, ConversationBase... conversations) {
        final LogCall LC = LogCall.startCall(L, ConversationImpl.class, "delete",
                ctx, "conversations", conversations);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("conversations", conversations);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (conversations.length == 0)
                return;

            Conversation[] convs = new Conversation[conversations.length];
            long createdTime = DateHelper.nowMillis();
            for (int i = 0; i < conversations.length; i++) {
                convs[i] = new Conversation();
                convs[i].setTarget(conversations[i].getTarget());
                convs[i].setReason(conversations[i].getReason());
                convs[i].setUser(userId);
                convs[i].setCreatedTime(createdTime);
            }

            db.delete(ctx, convs);
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void delete(Context ctx, Target... targets) {
        final LogCall LC = LogCall.startCall(L, ConversationImpl.class, "delete",
                ctx, "targets", targets);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("targets", targets);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (targets.length == 0)
                return;

            db.delete(ctx, targets);
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public long getCount(Context ctx, int reason, Target target) {
        return MapUtils.getLong(getCounts(ctx, reason, target), target, 0L);
    }

    @Override
    public Map<Target, Long> getCounts(Context ctx, int reason, Target... targets) {
        final LogCall LC = LogCall.startCall(L, ConversationImpl.class, "getCounts",
                ctx, "reason", reason, "targets", targets);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("targets", targets);

            Map<Target, Long> m = db.getCount(ctx, reason, 0, targets);
            LC.endCall();
            return  m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Conversations findByTarget(Context ctx, Conversations reuse, Page page, Target... targets) {
        return findByTarget(ctx, reuse, new int[]{}, page, targets);
    }

    @Override
    public Conversations findByTarget(Context ctx, Conversations reuse, int[] reasons, Page page, Target... targets) {
        final LogCall LC = LogCall.startCall(L, ConversationImpl.class, "findByTarget",
                ctx, "reuse", reuse, "reasons", reasons, "page", page, "targets", targets);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("reasons", reasons);
            ParamChecker.notNull("targets", targets);

            if (reuse == null)
                reuse = new Conversations();
            reuse.addAll(db.findByTarget(ctx, reasons, page, targets));
            LC.endCall();
            return reuse;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, Page page, long... userIds) {
        return findByUser(ctx, reuse, new int[0], Target.NONE, page, userIds);
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, int[] reasons, Page page, long... userIds) {
        return findByUser(ctx, reuse, reasons, Target.NONE, page, userIds);
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, Page page, int type, long... userIds) {
        return findByUser(ctx, reuse, new int[0], type, page, userIds);
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, int reason, Page page, long... userIds) {
        return findByUser(ctx, reuse, new int[] {reason}, Target.NONE, page, userIds);
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, int reason, int type, Page page, long... userIds) {
        return findByUser(ctx, reuse, new int[] {reason}, type, page, userIds);
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, int[] reasons, int type, Page page, long... userIds) {
        final LogCall LC = LogCall.startCall(L, ConversationImpl.class, "findByUser",
                ctx, "reuse", reuse, "reasons", reasons, "page", page, "userIds", userIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("reasons", reasons);
            ParamChecker.notNull("userIds", userIds);

            if (reuse == null)
                reuse = new Conversations();
            reuse.addAll(db.findByUser(ctx, reasons, type, page, userIds));
            LC.endCall();
            return reuse;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public long[] getTargetUsers(Context ctx, Target target, int reason, Page page) {
        Conversations conversations = findByTarget(ctx, null, new int[]{reason}, page, target);
        return conversations.getUsers();
    }

    @Override
    public long[] getTargetUsers(Context ctx, Target target, int[] reasons, Page page) {
        Conversations conversations = findByTarget(ctx, null, reasons, page, target);
        return conversations.getUsers();
    }



    @Override
    public Target[] getUserTargets(Context ctx, long userId, int reason, Page page) {
        Conversations conversations = findByUser(ctx, null, new int[]{reason}, page, userId);
        return conversations.getTargets();
    }

    @Override
    public Target[] getUserTargets(Context ctx, long userId, int[] reasons, Page page) {
        Conversations conversations = findByUser(ctx, null, reasons, page, userId);
        return conversations.getTargets();
    }

    @Override
    public Target[] getUserTargets(Context ctx, long userId, int reason, int type, Page page) {
        Conversations conversations = findByUser(ctx, null, new int[]{reason}, type, page, userId);
        return conversations.getTargets();
    }

    @Override
    public Target[] getUserTargets(Context ctx, long userId, int[] reasons, int type, Page page) {
        Conversations conversations = findByUser(ctx, null, reasons, type, page, userId);
        return conversations.getTargets();
    }

    @Override
    public String[] getUserTargetIds(Context ctx, long userId, int reason, int type, Page page) {
        Conversations conversations = findByUser(ctx, null, new int[]{reason}, type, page, userId);
        return conversations.getTargetIds(type);
    }

    @Override
    public String[] getUserTargetIds(Context ctx, long userId, int[] reasons, int type, Page page) {
        Conversations conversations = findByUser(ctx, null, reasons, type, page, userId);
        return conversations.getTargetIds(type);
    }

    @Override
    public long[] getUserTargetIdsAsLong(Context ctx, long userId, int reason, int type, Page page) {
        Conversations conversations = findByUser(ctx, null, new int[]{reason}, type, page, userId);
        return conversations.getTargetIdsAsLong(type);
    }

    @Override
    public long[] getUserTargetIdsAsLong(Context ctx, long userId, int[] reasons, int type, Page page) {
        Conversations conversations = findByUser(ctx, null, reasons, type, page, userId);
        return conversations.getTargetIdsAsLong(type);
    }

    @Override
    public Map<Target, long[]> getLastTargetUsers(Context ctx, Target[] targets, int reason, long[] prefUserIds, int userCount) {
        Conversations conversations = findByTarget(ctx, null, new int[]{reason}, null, targets);
        LinkedHashMap<Target, long[]> map = new LinkedHashMap<Target, long[]>();
        Map<Target, long[]> m = conversations.getGroupedUsers();
        for (Map.Entry<Target, long[]> entry : m.entrySet()) {
            Target target = entry.getKey();
            long[] allUserIds = entry.getValue();
            long[] userIds = new long[]{};
            for (long prefUserId : prefUserIds) {
                if (ArrayUtils.contains(allUserIds, prefUserId)
                        && (userIds.length < userCount))
                    userIds = ArrayUtils.add(userIds, prefUserId);
            }
            if (userIds.length < userCount)
                userIds = ArrayUtils.addAll(userIds, ArrayUtils.subarray(allUserIds, 0, userCount - userIds.length));
            map.put(target, userIds);
        }
        return map;
    }

    @Override
    public long[] getLastTargetUsers(Context ctx, Target target, int reason, long[] prefUserIds, int userCount) {
        return getLastTargetUsers(ctx, new Target[]{target}, reason, prefUserIds, userCount).get(target);
    }
}
