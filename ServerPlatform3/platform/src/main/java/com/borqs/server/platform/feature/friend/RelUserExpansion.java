package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.expansion.ExpansionHelper;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.UserExpansion;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.Map;

public class RelUserExpansion implements UserExpansion {

    private static final Logger L = Logger.get(RelUserExpansion.class);

    public static final String COL_REMARK = "remark";
    public static final String COL_IN_CIRCLES = "in_circles";
    public static final String COL_HIS_FRIEND = "his_friend";
    public static final String COL_BIDI = "bidi";
    public static final String COL_FRIENDS_COUNT = "friends_count";
    public static final String COL_FOLLOWERS_COUNT = "followers_count";

    public static final String[] RELATION_COLUMNS = {
            COL_IN_CIRCLES, COL_HIS_FRIEND, COL_BIDI, COL_FRIENDS_COUNT, COL_FOLLOWERS_COUNT
    };

    public static final String[] EXPAND_COLUMNS = {
            COL_REMARK, COL_IN_CIRCLES, COL_HIS_FRIEND, COL_BIDI, COL_FRIENDS_COUNT, COL_FOLLOWERS_COUNT
    };

    static {
        User.registerColumnsAlias("@xfriend,#xfriend", EXPAND_COLUMNS);
        User.registerColumnsAlias("@xrel,#xrel", RELATION_COLUMNS);
    }

    private FriendLogic friend;

    public RelUserExpansion() {
    }

    public RelUserExpansion(FriendLogic friend) {
        this.friend = friend;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Users data) {
        final LogCall LC = LogCall.startCall(L, RelUserExpansion.class, "expand", ctx,
                "expCols", expCols, "data", data);

        ParamChecker.notNull("ctx", ctx);


        if (CollectionUtils.isEmpty(data)) {
            LC.endCall();
            return;
        }

        if (!ctx.isLogined()) {
            LC.endCall();
            return;
        }

        if (!ExpansionHelper.needExpand(expCols, EXPAND_COLUMNS)) {
            LC.endCall();
            return;
        }

        try {
            expand0(ctx, expCols, data);
        } catch (RuntimeException e) {

            LC.endCall(e);
            throw e;
        }

    }

    protected void expand0(Context ctx, String[] expCols, Users data) {
        long[] userIds = data.getUserIds();
        PeopleId[] friendIds = PeopleIds.forUserIds(userIds).toIdArray();

        // COL_REMARK
        if (ArrayUtils.contains(expCols, COL_REMARK)) {
            Map<PeopleId, String> remarks = friend.getRemarks(ctx, ctx.getViewer(), friendIds);
            for (User user : data) {
                String remark = remarks.get(PeopleId.user(user.getUserId()));
                user.setAddon(COL_REMARK, ObjectUtils.toString(remark));
            }
        }


        // COL_IN_CIRCLES, COL_HIS_FRIEND, COL_BIDI
        boolean expandInCircles = ArrayUtils.contains(expCols, COL_IN_CIRCLES);
        boolean expandHisFriend = ArrayUtils.contains(expCols, COL_HIS_FRIEND);
        boolean expandBidi = ArrayUtils.contains(expCols, COL_BIDI);
        if (expandInCircles || expandHisFriend || expandBidi) {
            Relationships rels = friend.getRelationships(ctx, PeopleId.user(ctx.getViewer()), friendIds);
            Circles circles = friend.getCircles(ctx, ctx.getViewer(), null, false);
            for (User user : data) {
                Relationship rel = rels.getRelation(PeopleId.user(ctx.getViewer()), PeopleId.user(user.getUserId()));
                if (expandInCircles) {
                    int[] circleIds = rel != null ? rel.getTargetInViewerCircleIds() : null;
                    Circles cc = circles.getCircles(circleIds, false);
                    String ccJson = cc.toJson(Circle.CIRCLE_COLUMNS, true);
                    user.setAddon(COL_IN_CIRCLES, User.jsonAddonValue(ccJson));
                }
                if (expandHisFriend)
                    user.setAddon(COL_HIS_FRIEND, rel != null && rel.isTargetFriend());
                if (expandBidi)
                    user.setAddon(COL_BIDI, rel != null && rel.isBidi());
            }
        }

        // COL_FRIENDS_COUNT
        if (ArrayUtils.contains(expCols, COL_FRIENDS_COUNT)) {
            Map<Long, Integer> friendsCounts = friend.getFriendsCounts(ctx, userIds);
            for (User user : data) {
                Integer friendCount = friendsCounts.get(user.getUserId());
                user.setAddon(COL_FRIENDS_COUNT, friendCount != null ? friendCount : 0);
            }
        }

        // COL_FOLLOWERS_COUNT
        if (ArrayUtils.contains(expCols, COL_FOLLOWERS_COUNT)) {
            Map<PeopleId, Integer> followersCounts = friend.getFollowersCounts(ctx, friendIds);
            for (User user : data) {
                Integer followersCount = followersCounts.get(PeopleId.user(user.getUserId()));
                user.setAddon(COL_FOLLOWERS_COUNT, followersCount != null ? followersCount : 0);
            }
        }
    }
}
