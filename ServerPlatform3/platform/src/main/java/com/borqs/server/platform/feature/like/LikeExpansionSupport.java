package com.borqs.server.platform.feature.like;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class LikeExpansionSupport {

    public static final String COL_LIKES = "likes";
    public static final String COL_LIKED = "liked";

    public static final String SUBCOL_COUNT = "count";
    public static final String SUBCOL_USERS = "users";

    public static final String[] EXPAND_COLUMNS = {COL_LIKED, COL_LIKES};


    protected LikeLogic like;
    protected AccountLogic account;
    protected FriendLogic friend;
    protected int lastLikedCount = 4;

    protected LikeExpansionSupport() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public LikeLogic getLike() {
        return like;
    }

    public void setLike(LikeLogic like) {
        this.like = like;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    public int getLastLikedCount() {
        return lastLikedCount;
    }

    public void setLastLikedCount(int lastLikedCount) {
        this.lastLikedCount = lastLikedCount;
    }

    protected static final String[] LIKED_USER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_NAME,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };

    protected Map<Target, String> expandLikesHelper(Context ctx, Target[] targets) {
        long[] friendIds = null;
        if (ctx.isLogined())
            friendIds = friend.getBorqsFriendIds(ctx, ctx.getViewer());

        Map<Target, Long> likedCounts = like.getLikeCounts(ctx, targets);
        Map<Target, long[]> likedUserIds = like.getLikedUsers(ctx, targets, friendIds, lastLikedCount);
        Users likedUsers = account.getUsers(ctx, LIKED_USER_COLUMNS, CollectionsHelper.getValuesUnionSet(likedUserIds));

        HashMap<Target, String> m = new HashMap<Target, String>();
        final Users subLikedUsers = new Users();
        for (final Target target : targets) {
            final Long count = likedCounts.get(target);
            final long[] subLikedUserIds = likedUserIds.get(target);
            subLikedUsers.clear();
            likedUsers.getUsers(subLikedUsers, subLikedUserIds != null ? subLikedUserIds : new long[0]);

            String json = JsonHelper.toJson(new JsonGenerateHandler() {
                @Override
                public void generate(JsonGenerator jg, Object arg) throws IOException {
                    jg.writeStartObject();
                    jg.writeNumberField(SUBCOL_COUNT, count != null ? count : 0L);
                    jg.writeFieldName(SUBCOL_USERS);
                    jg.writeRawValue(subLikedUsers.toJson(LIKED_USER_COLUMNS, true));
                    jg.writeEndObject();
                }
            }, true);
            m.put(target, json);
        }
        return m;
    }

    protected Map<Target, Boolean> expandLikedHelper(Context ctx, Target[] postTargets) {
        /*
            "liked":true,
         */

        return like.isLiked(ctx, ctx.getViewer(), postTargets);
    }
}
