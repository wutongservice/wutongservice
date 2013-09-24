package com.borqs.server.platform.test;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.lang.ArrayUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestConversation implements ConversationLogic {
    private Conversations convs_ = new Conversations();

    @Override
    public boolean has(Context ctx, long userId, Target target, int reason) {
        for (Conversation conv : convs_) {
            if (conv.getUser() == userId 
                    && conv.getTarget().equals(target)
                    && conv.getReason() == reason)
                return true;
        }

        return false;
    }

    @Override
    public Map<Target, Boolean> has(Context ctx, long userId, Target[] targets, int reason) {
        LinkedHashMap<Target, Boolean> m = new LinkedHashMap<Target, Boolean>();
        for (Target target : targets) {
            m.put(target, false);
        }

        for (Conversation conv : convs_) {
            Target target = conv.getTarget();
            if (conv.getUser() == userId
                    && ArrayUtils.contains(targets, target)
                    && conv.getReason() == reason) {
                m.put(target, true);
            }
        }

        return m;
    }

    @Override
    public void create(Context ctx, ConversationBase... conversations) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("conversations", conversations);

        long userId = ctx.getViewer();

        if (conversations.length == 0)
            return;

        Conversation[] convs = new Conversation[conversations.length];

        for (int i = 0; i < conversations.length; i++) {
            convs[i] = new Conversation();
            convs[i].setTarget(conversations[i].getTarget());
            convs[i].setReason(conversations[i].getReason());
            convs[i].setUser(userId);
            convs[i].setCreatedTime(0);
        }

        Collections.addAll(convs_, convs);
    }

    @Override
    public void delete(Context ctx, ConversationBase... conversations) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("conversations", conversations);

        long userId = ctx.getViewer();

        if (conversations.length == 0)
            return;

        Conversation[] convs = new Conversation[conversations.length];

        for (int i = 0; i < conversations.length; i++) {
            convs[i] = new Conversation();
            convs[i].setTarget(conversations[i].getTarget());
            convs[i].setReason(conversations[i].getReason());
            convs[i].setUser(userId);
            convs[i].setCreatedTime(0);
            convs_.remove(convs[i]);
        }        
    }

    @Override
    public void delete(Context ctx, Target... targets) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("targets", targets);

        long userId = ctx.getViewer();

        if (targets.length == 0)
            return;
        
        Conversations convs = new Conversations();
        for (Conversation conv : convs_) {
            if (ArrayUtils.contains(targets, conv.getTarget())
                    && (userId == conv.getUser()))
                convs.add(conv);
        }

        convs_.removeAll(convs);
    }

    @Override
    public long getCount(Context ctx, int reason, Target target) {
        return getCounts(ctx, reason, target).get(target);
    }

    @Override
    public Map<Target, Long> getCounts(Context ctx, int reason, Target... targets) {
        LinkedHashMap<Target, Long> m = new LinkedHashMap<Target, Long>();
        for (Conversation conv : convs_) {
            Target target = conv.getTarget();
            if (ArrayUtils.contains(targets, target)
                    && (reason == conv.getReason())) {
                long count = m.get(target) == null ? 0 : m.get(target);
                m.put(target, count + 1);
            }
        }
        return m;
    }

    @Override
    public Conversations findByTarget(Context ctx, Conversations reuse, Page page, Target... targets) {
        return findByTarget(ctx, reuse, new int[]{}, page, targets);
    }

    @Override
    public Conversations findByTarget(Context ctx, Conversations reuse, int[] reasons, Page page, Target... targets) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("reasons", reasons);
        ParamChecker.notNull("targets", targets);

        if (reuse == null)
            reuse = new Conversations();

        Conversations convs = new Conversations();
        for (Conversation conv : convs_) {
            if (ArrayUtils.contains(targets, conv.getTarget())) {
                if ((reasons.length == 0) || (reasons.length > 0 && ArrayUtils.contains(reasons, conv.getReason())))
                    convs.add(conv);
            }
        }
        
        reuse.addAll(page(convs, page));
        return reuse;
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, Page page, long... userIds) {
        return findByUser(ctx, reuse, new int[]{}, page, userIds);
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, int[] reasons, Page page, long... userIds) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("reasons", reasons);
        ParamChecker.notNull("userIds", userIds);

        if (reuse == null)
            reuse = new Conversations();

        Conversations convs = new Conversations();
        for (Conversation conv : convs_) {
            if (ArrayUtils.contains(userIds, conv.getUser())) {
                if ((reasons.length == 0) || (reasons.length > 0 && ArrayUtils.contains(reasons, conv.getReason())))
                    convs.add(conv);
            }
        }

        reuse.addAll(page(convs, page));
        return reuse;
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

    private Conversations page(Conversations convs, Page page) {
        if (convs == null)
            return null;
        else if (page == null)
            return convs;
        else {
            int fromIndex = (int)(page.page * page.count);
            int toIndex = fromIndex + (int)page.count;

            return (Conversations)convs.subList(fromIndex, toIndex);
        }
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, Page page, int type, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, int[] reasons, int type, Page page, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, int reason, Page page, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Conversations findByUser(Context ctx, Conversations reuse, int reason, int type, Page page, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Target[] getUserTargets(Context ctx, long userId, int reason, int type, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Target[] getUserTargets(Context ctx, long userId, int[] reasons, int type, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getUserTargetIds(Context ctx, long userId, int reason, int type, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getUserTargetIds(Context ctx, long userId, int[] reasons, int type, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] getUserTargetIdsAsLong(Context ctx, long userId, int reason, int type, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long[] getUserTargetIdsAsLong(Context ctx, long userId, int[] reasons, int type, Page page) {
        throw new UnsupportedOperationException();
    }
}
