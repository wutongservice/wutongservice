package com.borqs.server.platform.feature.like;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationBaseLogic;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.util.ParamChecker;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LikeLogic extends ConversationBaseLogic {

    private List<LikeHook> likeHooks;
    private List<LikeHook> unlikeHooks;

    public LikeLogic() {
    }

    public LikeLogic(ConversationLogic conversation) {
        super(conversation);
    }

    public List<LikeHook> getLikeHooks() {
        return likeHooks;
    }

    public void setLikeHooks(List<LikeHook> likeHooks) {
        this.likeHooks = likeHooks;
    }

    public List<LikeHook> getUnlikeHooks() {
        return unlikeHooks;
    }

    public void setUnlikeHooks(List<LikeHook> unlikeHooks) {
        this.unlikeHooks = unlikeHooks;
    }

    public boolean isLiked(Context ctx, long userId, Target target) {
        return conversation.has(ctx, userId, target, Actions.LIKE);
    }

    public Map<Target, Boolean> isLiked(Context ctx, long userId, Target[] targets) {
        return conversation.has(ctx, userId, targets, Actions.LIKE);
    }

    public void like(Context ctx, Target target) {
        ConversationBase conv = new ConversationBase(target, Actions.LIKE);
        HookHelper.before(likeHooks, ctx, target);

        // add by wangpeng at 2012-09-12
        // for APK , should split the package name without version
        if (target.type != Target.APK) {
            conversation.create(ctx, conv);
        } else {
            String apkId = target.id;
            String[] o = apkId.split("-");
            String t = "";
            if (o.length == 3 || o.length == 1)
                t = o[0];
            if (t.length() > 0) {
                conversation.create(ctx, new ConversationBase(Target.forApk(t), Actions.LIKE));
            }
        }
        HookHelper.after(likeHooks, ctx, target);
    }

    public void unlike(Context ctx, Target target) {
        ConversationBase conv = new ConversationBase(target, Actions.LIKE);
        HookHelper.before(unlikeHooks, ctx, target);
        conversation.delete(ctx, conv);
        HookHelper.after(unlikeHooks, ctx, target);
    }

    public long getLikeCount(Context ctx, Target target) {
        return conversation.getCount(ctx, Actions.LIKE, target);
    }

    public Map<Target, Long> getLikeCounts(Context ctx, Target... targets) {
        ParamChecker.notNull("targets", targets);

        LinkedHashMap<Target, Long> m = new LinkedHashMap<Target, Long>();
        if (targets.length == 0)
            return m;

        HashSet<Integer> set = new HashSet<Integer>();
        for (Target target : targets)
            set.add(target.type);

        for (int type : set)
            m.putAll(conversation.getCounts(ctx, Actions.LIKE, targets));

        return m;
    }

    public Conversations getTargetHistories(Context ctx, Target target, Page page) {
        return conversation.findByTarget(ctx, null,
                new int[]{Actions.LIKE}, page, target);
    }

    public Conversations getUserHistories(Context ctx, long userId, Page page) {
        return conversation.findByUser(ctx, null, new int[]{Actions.LIKE}, page, userId);
    }

    public long[] getLikedUsers(Context ctx, Target target, Page page) {
        return conversation.getTargetUsers(ctx, target, Actions.LIKE, page);
    }

    public Target[] getUserLiked(Context ctx, long userId, int type, Page page) {
        return conversation.getUserTargets(ctx, userId, Actions.LIKE, type, page);
    }

    public Target[] getUserLiked(Context ctx, long userId, Page page) {
        return conversation.getUserTargets(ctx, userId, Actions.LIKE, page);
    }

    public String[] getUserLikedIds(Context ctx, long userId, int type, Page page) {
        return conversation.getUserTargetIds(ctx, userId, Actions.LIKE, type, page);
    }

    public long[] getUserLikedIdsAsLong(Context ctx, long userId, int type, Page page) {
        return conversation.getUserTargetIdsAsLong(ctx, userId, Actions.LIKE, type, page);
    }


    public Map<Target, long[]> getLikedUsers(Context ctx, Target[] targets, long[] prefUserIds, int userCount) {
        return conversation.getLastTargetUsers(ctx, targets, Actions.LIKE, prefUserIds, userCount);
    }

    public long[] getLikedUsers(Context ctx, Target target, long[] prefUserIds, int userCount) {
        return conversation.getLastTargetUsers(ctx, target, Actions.LIKE, prefUserIds, userCount);
    }
}
