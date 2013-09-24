package com.borqs.server.platform.feature.favorite;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationBaseLogic;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.util.ParamChecker;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;


public class FavoriteLogic extends ConversationBaseLogic {

    public FavoriteLogic() {
    }

    public FavoriteLogic(ConversationLogic conversation) {
        super(conversation);
    }
    
    public boolean isFavorite(Context ctx, long userId, Target target) {
        return conversation.has(ctx, userId, target, Actions.FAVORITE);
    }

    public void favorite(Context ctx, Target target) {
        ConversationBase conv = new ConversationBase(target, Actions.FAVORITE);
        conversation.create(ctx, conv);
    }

    public void unfavorite(Context ctx, Target target) {
        ConversationBase conv = new ConversationBase(target, Actions.FAVORITE);
        conversation.delete(ctx, conv);
    }

    public long getFavoriteCount(Context ctx, Target target) {
        return conversation.getCount(ctx, Actions.FAVORITE, target);
    }

    public Map<Target, Long> getFavoriteCounts(Context ctx, Target... targets) {
        ParamChecker.notNull("targets", targets);

        LinkedHashMap<Target, Long> m = new LinkedHashMap<Target, Long>();
        if (targets.length == 0)
            return m;

        HashSet<Integer> set = new HashSet<Integer>();
        for (Target target : targets)
            set.add(target.type);

        for (int type : set)
            m.putAll(conversation.getCounts(ctx, Actions.FAVORITE, targets));

        return m;
    }

    public Conversations getTargetHistories(Context ctx, Target target, Page page) {
        return conversation.findByTarget(ctx, null,
                new int[]{Actions.FAVORITE}, page, target);
    }


    public Conversations getUserHistories(Context ctx, long userId, Page page) {
        return conversation.findByUser(ctx, null, new int[]{Actions.FAVORITE}, page, userId);
    }

    public long[] getFavoriteUsers(Context ctx, Target target, Page page) {
        return conversation.getTargetUsers(ctx, target, Actions.FAVORITE, page);
    }

    public Target[] getUserFavorites(Context ctx, long userId, int type, Page page) {
        return conversation.getUserTargets(ctx, userId, Actions.FAVORITE, page);
    }

    public Target[] getUserFavorites(Context ctx, long userId, Page page) {
        return conversation.getUserTargets(ctx, userId, Actions.FAVORITE, page);
    }
}
