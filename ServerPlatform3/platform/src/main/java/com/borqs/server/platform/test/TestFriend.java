package com.borqs.server.platform.test;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestFriend extends AbstractFriendImpl {

    private final Map<Long, FriendEntries> friends = new HashMap<Long, FriendEntries>();
    private final Map<Long, RemarkEntries> remarks = new HashMap<Long, RemarkEntries>();

    public TestFriend() {
    }


    @Override
    protected FriendEntries getFriendEntries(Context ctx, long userId) {
        FriendEntries fes = friends.get(userId);
        if (fes == null) {
            fes = new FriendEntries();
        } else {
            fes = fes.copy();
        }

        Circles circles = new Circles();
        if (CollectionUtils.isNotEmpty(fes.circles)) {
            for (Circle c : fes.circles)
                circles.add(c.copy());
        }

        for (int circleId : Circle.BUILTIN_ACTUAL_CIRCLES) {
            circles.add(new Circle(circleId, Circle.getBuiltinCircleName(circleId, ""), 0));
        }
        fes.circles = circles;
        return fes;
    }

    @Override
    protected Map<PeopleId, String> getRemarks(Context ctx, long userId) {
        RemarkEntries res = remarks.get(userId);
        return res != null ? new RemarkEntries(res) : new RemarkEntries();
    }

//    private FriendEntries ensureFriendEntries(long userId) {
//        FriendEntries fes = friends.get(userId);
//        if (fes == null) {
//            fes = new FriendEntries();
//            friends.put(userId, fes);
//        }
//        return fes;
//    }

    private RemarkEntries ensureRemarkEntries(long userId) {
        RemarkEntries res = remarks.get(userId);
        if (res == null) {
            res = new RemarkEntries();
            remarks.put(userId, res);
        }
        return res;
    }

    @Override
    public Circle createCustomCircle(Context ctx, String circleName) {
        ParamChecker.notNull("ctx", ctx);
        long viewerId = ctx.getViewer();
        AccountHelper.checkUser(account, ctx, viewerId);

        FriendEntries fes = getFriendEntries(ctx, viewerId);
        int[] customCircleIds = fes.circles.getCircleIds(Circle.MIN_CUSTOM_CIRCLE_ID);
        int newCircleId = newCircleId(customCircleIds);
        if (newCircleId < 0)
            throw new ServerException(E.TOO_MANY_CIRCLES, "Too many circles");

        Circle newCircle = new Circle(newCircleId, circleName, DateHelper.nowMillis(), null);
        fes.circles.add(newCircle);
        putFriendEntries(viewerId, fes);
        return newCircle;
    }

    @Override
    public boolean destroyCustomCircle(Context ctx, int circleId) {
        ParamChecker.notNull("ctx", ctx);
        long viewerId = ctx.getViewer();
        AccountHelper.checkUser(account, ctx, viewerId);

        FriendEntries fes = getFriendEntries(ctx, viewerId);
        if (fes != null) {
            boolean b = fes.destroyCircle(circleId);
            putFriendEntries(viewerId, fes);
            return b;
        } else {
            return false;
        }
    }

    @Override
    public boolean updateCustomCircleName(Context ctx, int circleId, String circleName) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notEmpty("circleName", circleName);
        long viewerId = ctx.getViewer();
        AccountHelper.checkUser(account, ctx, viewerId);

        if (!Circle.isCustomCircleId(circleId))
            return false;

        FriendEntries fes = getFriendEntries(ctx, viewerId);
        if (fes != null) {
            fes.updateCircleName(circleId, circleName);
            putFriendEntries(viewerId, fes);
            return true;
        } else {
            return false;
        }
    }

    private void putFriendEntries(long userId, FriendEntries fes) {
        fes.trimFriends();
        fes = fes.copy();
        if (CollectionUtils.isNotEmpty(fes.circles)) {
            for (int i = fes.circles.size() - 1; i >= 0; i--) {
                Circle c = fes.circles.get(i);
                if (ArrayUtils.contains(Circle.BUILTIN_ACTUAL_CIRCLES, c.getCircleId()))
                    fes.circles.remove(i);
            }
        }
        friends.put(userId, fes.copy());
    }

    @Override
    public void setFriendIntoCircles(Context ctx, int reason, PeopleId friendId, int... circleIds) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("friendId", friendId);
        ParamChecker.notNull("circleIds", circleIds);
        long viewerId = ctx.getViewer();
        AccountHelper.checkUser(account, ctx, viewerId);

        FriendEntries fes = getFriendEntries(ctx, viewerId);
        if (fes == null || !fes.hasAllCircles(circleIds))
            throw new ServerException(E.INVALID_CIRCLE);

        FriendEntry fe = fes.ensureFriend(friendId);
        fe.setCircles(reason, DateHelper.nowMillis(), circleIds);
        putFriendEntries(viewerId, fes);
    }

    @Override
    public void addFriendsIntoCircle(Context ctx, int reason, PeopleIds friendIds, int circleId) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("friendIds", friendIds);
        ParamChecker.notNull("circleI", circleId);

        long viewerId = ctx.getViewer();
        AccountHelper.checkUser(account, ctx, viewerId);

        long now = DateHelper.nowMillis();
        FriendEntries fes = getFriendEntries(ctx, viewerId);
        if (fes == null || !fes.hasCircle(circleId))
            throw new ServerException(E.INVALID_CIRCLE);


        for (PeopleId friendId : friendIds) {
            FriendEntry fe = fes.ensureFriend(friendId);
            fe.addCircle(circleId, reason, now);
        }
        putFriendEntries(viewerId, fes);
    }

    @Override
    public void removeFriendsInCircle(Context ctx, PeopleIds friendIds, int circleId) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("friendIds", friendIds);
        ParamChecker.notNull("circleId", circleId);

        long viewerId = ctx.getViewer();
        AccountHelper.checkUser(account, ctx, viewerId);

        FriendEntries fes = getFriendEntries(ctx, viewerId);
        for (PeopleId friendId : friendIds) {
            FriendEntry fe = fes.ensureFriend(friendId);
            fe.removeCircle(circleId);
        }
        putFriendEntries(viewerId, fes);
    }

    @Override
    public long[] getFollowers(Context ctx, PeopleId friendId, Page page) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("friendId", friendId);

        ArrayList<Long> l = new ArrayList<Long>();
        for (Map.Entry<Long, FriendEntries> e : friends.entrySet()) {
            long userId = e.getKey();
            FriendEntries fes = e.getValue();

            if (fes.isFollowerOf(friendId))
                l.add(userId);
        }

        if (page != null)
            page.retains(l);

        return CollectionsHelper.toLongArray(l);
    }

    @Override
    public int getFollowersCount(Context ctx, PeopleId friendId) {
        return getFollowers(ctx, friendId, null).length;
    }

    @Override
    public Relationships getRelationships(Context ctx, PeopleId viewer, PeopleId... targets) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("viewer", viewer);
        ParamChecker.notNull("targets", targets);

        Relationships rels = new Relationships();
        if (targets.length > 0) {
            for (PeopleId target : targets)
                rels.add(Relationship.disrelated(viewer, target));

            if (viewer.isUser()) {
                FriendEntries fes = getFriendEntries(ctx, viewer.getIdAsLong());
                if (fes != null) {
                    for (PeopleId target : targets)
                        rels.getRelation(viewer, target).setTargetInViewerCircles(fes.getInCirclesByFriend(target));
                }
            }

            for (PeopleId target : targets) {
                if (!target.isUser())
                    break;

                FriendEntries fes = getFriendEntries(ctx, target.getIdAsLong());
                if (fes != null)
                    rels.getRelation(viewer, target).setViewerInTargetCircles(fes.getInCirclesByFriend(viewer));

            }
        }
        return rels;
    }

    @Override
    public void setRemark(Context ctx, PeopleId friendId, String remark) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("friendId", friendId);

        RemarkEntries res = ensureRemarkEntries(ctx.getViewer());
        if (StringUtils.isEmpty(remark))
            res.remove(friendId);
        else
            res.put(friendId, remark);
    }

    private static class RemarkEntries extends HashMap<PeopleId, String> {
        private RemarkEntries() {
        }

        private RemarkEntries(Map<? extends PeopleId, ? extends String> m) {
            super(m);
        }
    }
}
