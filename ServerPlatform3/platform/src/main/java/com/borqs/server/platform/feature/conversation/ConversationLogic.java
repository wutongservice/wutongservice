package com.borqs.server.platform.feature.conversation;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface ConversationLogic extends Logic {
    boolean has(Context ctx, long userId, Target target, int reason);
    Map<Target,Boolean> has(Context ctx, long userId, Target[] targets, int reason);
    
    void create(Context ctx, ConversationBase... conversations);
    void delete(Context ctx, ConversationBase... conversations);
    void delete(Context ctx, Target... targets);

    long getCount(Context ctx, int reason, Target target);
    Map<Target, Long> getCounts(Context ctx, int reason, Target... targets);

    Conversations findByTarget(Context ctx, Conversations reuse, Page page, Target... targets);
    Conversations findByTarget(Context ctx, Conversations reuse, int[] reasons, Page page, Target... targets);

    Conversations findByUser(Context ctx, Conversations reuse, Page page, long... userIds);
    Conversations findByUser(Context ctx, Conversations reuse, int[] reasons, Page page, long... userIds);
    Conversations findByUser(Context ctx, Conversations reuse, Page page, int type, long... userIds);
    Conversations findByUser(Context ctx, Conversations reuse, int[] reasons, int type, Page page, long... userIds);
    Conversations findByUser(Context ctx, Conversations reuse, int reason, Page page, long... userIds);
    Conversations findByUser(Context ctx, Conversations reuse, int reason, int type, Page page, long... userIds);

    long[] getTargetUsers(Context ctx, Target target, int reason, Page page);
    long[] getTargetUsers(Context ctx, Target target, int[] reasons, Page page);


    Target[] getUserTargets(Context ctx, long userId, int reason, Page page);
    Target[] getUserTargets(Context ctx, long userId, int[] reasons, Page page);

    Target[] getUserTargets(Context ctx, long userId, int reason, int type, Page page);
    Target[] getUserTargets(Context ctx, long userId, int[] reasons, int type, Page page);

    String[] getUserTargetIds(Context ctx, long userId, int reason, int type, Page page);
    String[] getUserTargetIds(Context ctx, long userId, int[] reasons, int type, Page page);

    long[] getUserTargetIdsAsLong(Context ctx, long userId, int reason, int type, Page page);
    long[] getUserTargetIdsAsLong(Context ctx, long userId, int[] reasons, int type, Page page);

    Map<Target, long[]> getLastTargetUsers(Context ctx, Target[] targets, int reason, long[] prefUserIds, int userCount);
    long[] getLastTargetUsers(Context ctx, Target target, int reason, long[] prefUserIds, int userCount);
}
