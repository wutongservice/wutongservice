package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.expansion.ExpansionHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractFriendImpl implements FriendLogic {
    private static final Logger L = Logger.get(AbstractFriendImpl.class);

    protected AccountLogic account;

    protected final BuiltinCirclesExpansion builtinCirclesExpansion = new BuiltinCirclesExpansion();
    protected List<CirclesExpansion> circlesExpansions;

    protected AbstractFriendImpl() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public List<CirclesExpansion> getCirclesExpansions() {
        return circlesExpansions;
    }

    public void setCirclesExpansions(List<CirclesExpansion> circlesExpansions) {
        this.circlesExpansions = circlesExpansions;
    }

    @Override
    public Circles getCircles(Context ctx, long userId, int[] circleIds, boolean withUsers) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getCircles", ctx,
                "userId", userId, "circleIds", circleIds, "withUsers", withUsers);

        try {
            ParamChecker.notNull("ctx", ctx);

            FriendEntries fes = getFriendEntries(ctx, userId);
            Circles circles = new Circles();
            if (fes != null) {
                if (fes.circles != null) {
                    for (Circle c : fes.circles) {
                        if (ArrayUtils.isEmpty(circleIds) || ArrayUtils.contains(circleIds, c.getCircleId())) {
                            Circle cc = c.copy();
                            cc.setMemberCount(fes.getFriendCountInCircles(c.getCircleId()));
                            circles.add(cc);
                        }
                    }
                }

                if (withUsers) {
                    for (Circle circle : circles)
                        circle.setFriendIds(fes.getFriendIds(null, circle.getCircleId()));
                }


                expandCircles(ctx, withUsers ? Circle.CIRCLE_COLUMNS_WITH_MEMBERS : Circle.CIRCLE_COLUMNS, circles);
            }
            LC.endCall();
            return circles;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasCircle(Context ctx, long userId, int circleId) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasCircle", ctx,
                "userId", userId, "circleId", circleId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");

            FriendEntries fes = getFriendEntries(ctx, userId);
            boolean b = fes != null && fes.hasCircle(circleId);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAllCircles(Context ctx, long userId, int... circleIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasAllCircles", ctx,
                "userId", userId, "circleIds", circleIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");

            FriendEntries fes = getFriendEntries(ctx, userId);
            boolean b = fes != null && fes.hasAllCircles(circleIds);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAnyCircles(Context ctx, long userId, int... circleIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasAnyCircles", ctx,
                "userId", userId, "circleIds", circleIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");

            FriendEntries fes = getFriendEntries(ctx, userId);
            boolean b = fes != null && fes.hasAnyCircles(circleIds);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public PeopleIds getFriendsInCircles(Context ctx, long userId, int... circleIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getFriendsInCircles", ctx,
                "userId", userId, "circleIds", circleIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");

            PeopleIds friendIds = new PeopleIds();
            FriendEntries fes = getFriendEntries(ctx, userId);
            if (fes != null) {
                if (ArrayUtils.isNotEmpty(circleIds)) {
                    fes.getFriendIds(friendIds, circleIds);
                } else {
                    if (ctx.isLogined() && ctx.getViewer() == userId)
                        fes.getFriendIdsExcept(friendIds);
                    else
                        fes.getFriendIdsExcept(friendIds, Circle.CIRCLE_BLOCKED);
                }
            }

            LC.endCall();
            return friendIds;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public PeopleIds getFriends(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getFriends", ctx,
                "userId", userId);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");

            PeopleIds friendIds = new PeopleIds();
            FriendEntries fes = getFriendEntries(ctx, userId);
            if (fes != null) {
                if (ctx.isLogined() && ctx.getViewer() == userId)
                    fes.getFriendIdsExcept(friendIds);
                else
                    fes.getFriendIdsExcept(friendIds, Circle.CIRCLE_BLOCKED);
            }
            LC.endCall();
            return friendIds;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public int getFriendCountInCircles(Context ctx, long userId, int... circleIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getFriendCountInCircles", ctx,
                "userId", userId, "circleIds", circleIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");

            int count = 0;
            FriendEntries fes = getFriendEntries(ctx, userId);
            if (fes != null)
                count = fes.getFriendCountInCircles(circleIds);

            LC.endCall();
            return count;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public int getFriendCount(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getFriendCount", ctx,
                "userId", userId);

        try {
            ParamChecker.notNull("ctx", ctx);

            int count = 0;
            FriendEntries fes = getFriendEntries(ctx, userId);
            if (fes != null)
                count = fes.getFriendCountInCirclesExcept(Circle.CIRCLE_BLOCKED);

            LC.endCall();
            return count;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Long, Integer> getFriendsCounts(Context ctx, long... userIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getFriendsCounts", ctx,
                "userIds", userIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("userIds", userIds);

            LinkedHashMap<Long, Integer> m = new LinkedHashMap<Long, Integer>();
            for (long userId : userIds) {
                int friendCount = getFriendCount(ctx, userId);
                m.put(userId, friendCount);
            }
            LC.endCall();
            return m;

        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasFriendInCircles(Context ctx, long userId, int[] circleIds, PeopleId friendId) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasFriendInCircles", ctx,
                "userId", userId, "friendId", friendId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");

            boolean b = false;
            if (friendId != null) {
                FriendEntries fes = getFriendEntries(ctx, userId);
                if (fes != null)
                    b = fes.hasFriendInCircles(circleIds, friendId);
            }
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAllFriendsInCircles(Context ctx, long userId, int[] circleIds, PeopleId... friendIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasAllFriendsInCircles", ctx,
                "userId", userId, "friendIds", friendIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");
            boolean b = false;
            if (ArrayUtils.isNotEmpty(friendIds)) {
                FriendEntries fes = getFriendEntries(ctx, userId);
                if (fes != null)
                    b = fes.hasAllFriendsInCircles(circleIds, friendIds);
            }
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAnyFriendsInCircles(Context ctx, long userId, int[] circleIds, PeopleId... friendIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasAnyFriendsInCircles", ctx,
                "userId", userId, "friendIds", friendIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Illegal userId");
            boolean b = false;
            if (ArrayUtils.isNotEmpty(friendIds)) {
                FriendEntries fes = getFriendEntries(ctx, userId);
                if (fes != null)
                    b = fes.hasAnyFriendsInCircles(circleIds, friendIds);
            }
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasFriend(Context ctx, long userId, PeopleId friendId) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasFriend", ctx,
                "userId", userId, "friendId", friendId);
        try {
            boolean b = hasFriendInCircles(ctx, userId, null, friendId);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAllFriends(Context ctx, long userId, PeopleId... friendIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasAllFriends", ctx,
                "userId", userId, "friendIds", friendIds);
        try {
            boolean b = hasAllFriendsInCircles(ctx, userId, null, friendIds);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAnyFriends(Context ctx, long userId, PeopleId... friendIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "hasAnyFriends", ctx,
                "userId", userId, "friendIds", friendIds);
        try {
            boolean b = hasAnyFriendsInCircles(ctx, userId, null, friendIds);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public Relationship getRelationship(Context ctx, PeopleId viewer, PeopleId target) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getRelationship", ctx,
                "viewer", viewer, "target", target);
        try {
            Relationships rels = getRelationships(ctx, viewer, target);
            Relationship rel = rels.isEmpty() ? Relationship.disrelated(viewer, target) : rels.get(0);
            LC.endCall();
            return rel;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<PeopleId, String> getRemarks(Context ctx, long userId, PeopleId... friendIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getRemarks", ctx,
                "userId", userId, "friendIds", friendIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("friendIds", friendIds);

            Map<PeopleId, String> remarks = getRemarks(ctx, userId);
            CollectionsHelper.retainKeys(remarks, friendIds);
            LC.endCall();
            return remarks;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public String getRemark(Context ctx, long userId, PeopleId friendId) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getRemark", ctx,
                "userId", userId, "friendId", friendId);

        try {
            Map<PeopleId, String> remarks = getRemarks(ctx, userId, friendId);
            String remark = MapUtils.getString(remarks, friendId, "");
            LC.endCall();
            return remark;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    protected List<FriendsHook.Entry> getFriendsHookData(Context ctx, long userId, PeopleIds friendIds, int reason) {
        FriendEntries fes = getFriendEntries(ctx, userId);
        ArrayList<FriendsHook.Entry> hd = new ArrayList<FriendsHook.Entry>();
        for (PeopleId friendId : friendIds) {
            FriendEntry fe = fes.findFriend(friendId);
            hd.add(new FriendsHook.Entry(userId, friendId, reason, fe != null ? fe.getCircleIds() : new int[0]));
        }
        return hd;
    }

    public static class FriendEntries implements Copyable<FriendEntries> {
        public Circles circles;
        public List<FriendEntry> friends;

        public void trimFriends() {
            if (friends != null) {
                ArrayList<FriendEntry> l = new ArrayList<FriendEntry>();
                for (FriendEntry fe : friends) {
                    if (CollectionUtils.isNotEmpty(fe.inCircles))
                        l.add(fe);
                }
                friends = l;
            }
        }

        @Override
        public FriendEntries copy() {
            FriendEntries fes = new FriendEntries();
            if (circles != null) {
                fes.circles = new Circles();
                for (Circle c : circles)
                    fes.circles.add(c.copy());
            }

            if (friends != null) {
                fes.friends = new ArrayList<FriendEntry>();
                for (FriendEntry fe : friends)
                    fes.friends.add(fe.copy());
            }
            return fes;
        }

        public List<Relationship.InCircle> getInCirclesByFriend(PeopleId friendId) {
            if (CollectionUtils.isEmpty(friends))
                return null;

            ArrayList<Relationship.InCircle> l = new ArrayList<Relationship.InCircle>();
            for (FriendEntry fe : friends) {
                if (ObjectUtils.equals(friendId, fe.friendId) && fe.inCircles != null) {
                    l.addAll(fe.inCircles);
                }
            }
            return l;
        }

        public boolean isFollowerOf(PeopleId friendId) {
            if (CollectionUtils.isEmpty(friends))
                return false;

            for (FriendEntry fe : friends) {
                if (ObjectUtils.equals(fe.friendId, friendId)) {
                    boolean inBlockedCircle = false;
                    if (CollectionUtils.isNotEmpty(fe.inCircles)) {
                        for (Relationship.InCircle ic : fe.inCircles) {
                            if (ic.circleId == Circle.CIRCLE_BLOCKED) {
                                inBlockedCircle = true;
                                break;
                            }
                        }
                    }
                    if (!inBlockedCircle)
                        return true;
                }
            }
            return false;
        }

        public void addFriend(FriendEntry fe) {
            if (friends == null)
                friends = new ArrayList<FriendEntry>();

            friends.add(fe);
        }

        public PeopleIds getFriendIds(PeopleIds reuse, int circleId) {
            PeopleIds friendIds = reuse != null ? reuse : new PeopleIds();
            if (CollectionUtils.isNotEmpty(friends)) {
                for (FriendEntry fe : friends) {
                    if (fe.hasCircle(circleId))
                        friendIds.add(fe.friendId);
                }
            }
            return friendIds;
        }


        public FriendEntry findFriend(PeopleId friendId) {
            if (friends != null) {
                for (FriendEntry fe : friends) {
                    if (fe != null && fe.friendId.equals(friendId))
                        return fe;
                }
            }
            return null;
        }

        public PeopleIds getFriendIds(PeopleIds reuse, int... circleIds) {
            PeopleIds friendIds = reuse != null ? reuse : new PeopleIds();
            if (CollectionUtils.isNotEmpty(friends) && ArrayUtils.isNotEmpty(circleIds)) {
                for (FriendEntry fe : friends) {
                    boolean find = false;
                    for (int circleId : circleIds) {
                        if (fe.hasCircle(circleId)) {
                            find = true;
                            break;
                        }
                    }
                    if (find)
                        friendIds.add(fe.friendId);
                }
            }
            return friendIds;
        }

        public PeopleIds getFriendIdsExcept(PeopleIds reuse, int... circleIds) {
            PeopleIds friendIds = reuse != null ? reuse : new PeopleIds();
            if (CollectionUtils.isNotEmpty(friends)) {
                for (FriendEntry fe : friends) {
                    boolean find = false;
                    for (int circleId : circleIds) {
                        if (fe.hasCircle(circleId)) {
                            find = true;
                            break;
                        }
                    }
                    if (!find)
                        friendIds.add(fe.friendId);
                }
            }
            return friendIds;
        }

        public int getFriendCountInCircles(int... circleIds) {
            int count = 0;
            if (ArrayUtils.isNotEmpty(circleIds) && CollectionUtils.isNotEmpty(friends)) {
                for (FriendEntry fe : friends) {
                    boolean find = false;
                    for (int circleId : circleIds) {
                        if (fe.hasCircle(circleId)) {
                            find = true;
                            break;
                        }
                    }
                    if (find)
                        count++;
                }
            }
            return count;
        }

        public int getFriendCountInCirclesExcept(int... circleIds) {
            int count = 0;
            if (ArrayUtils.isNotEmpty(circleIds) && CollectionUtils.isNotEmpty(friends)) {
                for (FriendEntry fe : friends) {
                    boolean find = false;
                    for (int circleId : circleIds) {
                        if (fe.hasCircle(circleId)) {
                            find = true;
                            break;
                        }
                    }
                    if (!find)
                        count++;
                }
            }
            return count;
        }

        public boolean hasCircle(int circleId) {
            if (CollectionUtils.isNotEmpty(circles)) {
                for (Circle circle : circles) {
                    if (circle.getCircleId() == circleId)
                        return true;
                }
            }
            return false;
        }

        public boolean hasAllCircles(int[] circleIds) {
            for (int circleId : circleIds) {
                boolean find = false;
                for (Circle circle : circles) {
                    if (circle.getCircleId() == circleId) {
                        find = true;
                        break;
                    }
                }
                if (!find)
                    return false;
            }
            return true;
        }

        public boolean hasAnyCircles(int[] circleIds) {
            for (int circleId : circleIds) {
                boolean find = false;
                for (Circle circle : circles) {
                    if (circle.getCircleId() == circleId) {
                        find = true;
                        break;
                    }
                }
                if (find)
                    return true;
            }
            return false;
        }

        public boolean hasFriendInCircles(int[] circleIds, PeopleId friendId) {
            boolean b = ArrayUtils.isEmpty(circleIds);
            if (CollectionUtils.isNotEmpty(friends)) {
                for (FriendEntry fe : friends) {
                    if (fe.friendId != null && fe.friendId.equals(friendId)) {
                        if (b) {
                            return true;
                        } else {
                            if (fe.hasAnyCircles(circleIds))
                                return true;
                        }
                    }
                }
            }
            return false;

        }

        public boolean hasAllFriendsInCircles(int[] circleIds, PeopleId... friendIds) {
            for (PeopleId friendId : friendIds) {
                if (!hasFriendInCircles(circleIds, friendId))
                    return false;
            }
            return true;
        }

        public boolean hasAnyFriendsInCircles(int[] circleIds, PeopleId... friendIds) {
            for (PeopleId friendId : friendIds) {
                if (hasFriendInCircles(circleIds, friendId))
                    return true;
            }
            return false;
        }

        public boolean destroyCircle(int circleId) {
            if (!Circle.isCustomCircleId(circleId))
                return false;

            if (CollectionUtils.isEmpty(circles))
                return false;

            Circle c = null;
            for (Circle circle : circles) {
                if (circle != null && circle.getCircleId() == circleId) {
                    c = circle;
                    break;
                }
            }
            if (c != null)
                circles.remove(c);


            if (CollectionUtils.isNotEmpty(friends)) {
                for (FriendEntry fe : friends)
                    fe.destroyCircle(circleId);
            }

            return c != null;
        }

        public boolean updateCircleName(int circleId, String circleName) {
            if (CollectionUtils.isEmpty(circles))
                return false;

            for (Circle c : circles) {
                if (c != null && c.getCircleId() == circleId) {
                    c.setCircleName(circleName);
                    return true;
                }
            }
            return false;
        }

        public FriendEntry ensureFriend(PeopleId friendId) {
            FriendEntry fe = findFriend(friendId);
            if (fe == null) {
                fe = new FriendEntry();
                fe.friendId = friendId;
                if (friends == null)
                    friends = new ArrayList<FriendEntry>();
                friends.add(fe);
            }
            return fe;
        }
    }

    public static class FriendEntry implements Copyable<FriendEntry> {

        public PeopleId friendId;
        public List<Relationship.InCircle> inCircles;

        @Override
        public FriendEntry copy() {
            FriendEntry fe = new FriendEntry();
            if (friendId != null)
                fe.friendId = friendId.copy();

            if (inCircles != null) {
                fe.inCircles = new ArrayList<Relationship.InCircle>();
                for (Relationship.InCircle ic : inCircles)
                    fe.inCircles.add(ic.copy());
            }
            return fe;
        }

        public int[] getCircleIds() {
            if (CollectionUtils.isNotEmpty(inCircles)) {
                int[] circleIds = new int[inCircles.size()];
                for (int i = 0; i < circleIds.length; i++)
                    circleIds[i] = inCircles.get(i).circleId;
                return circleIds;
            } else {
                return new int[0];
            }
        }

        public void addCircle(Relationship.InCircle circle) {
            if (inCircles == null)
                inCircles = new ArrayList<Relationship.InCircle>();

            boolean find = false;
            for (Relationship.InCircle c : inCircles) {
                if (c != null && c.circleId == circle.circleId) {
                    find = true;
                    break;
                }
            }
            if (!find)
                inCircles.add(circle);
        }

        public void addCircle(int circleId, int reason, long updatedTime) {
            addCircle(new Relationship.InCircle(circleId, reason, updatedTime));
        }

        public void removeCircle(int circleId) {
            if (CollectionUtils.isNotEmpty(inCircles)) {
                Relationship.InCircle c = null;
                for (Relationship.InCircle ic : inCircles) {
                    if (ic != null && ic.circleId == circleId) {
                        c = ic;
                        break;
                    }
                }
                if (c != null)
                    inCircles.remove(c);
            }
        }

        public Relationship.InCircle findCircle(int circleId) {
            if (inCircles != null) {
                for (Relationship.InCircle circle : inCircles) {
                    if (circle != null && circle.circleId == circleId)
                        return circle;
                }
            }
            return null;
        }

        public boolean hasCircle(int circleId) {
            if (CollectionUtils.isNotEmpty(inCircles)) {
                for (Relationship.InCircle inCircle : inCircles) {
                    if (inCircle.circleId == circleId)
                        return true;
                }
            }
            return false;
        }

        public boolean hasAnyCircles(int... circleIds) {
            if (ArrayUtils.isEmpty(circleIds))
                return false;

            for (int circleId : circleIds) {
                if (hasCircle(circleId))
                    return true;
            }
            return false;
        }

        public void destroyCircle(int circleId) {
            if (CollectionUtils.isNotEmpty(inCircles)) {
                Relationship.InCircle c = null;
                for (Relationship.InCircle ic : inCircles) {
                    if (ic != null && ic.circleId == circleId) {
                        c = ic;
                        break;
                    }
                }
                if (c != null)
                    inCircles.remove(c);
            }
        }

        public void setCircles(int reason, long now, int[] circleIds) {
            if (inCircles != null)
                inCircles.clear();

            for (int circleId : circleIds)
                addCircle(circleId, reason, now);
        }
    }


    public static int newCircleId(int[] customCircleIds) {
        if (ArrayUtils.isEmpty(customCircleIds))
            return Circle.MIN_CUSTOM_CIRCLE_ID;

        if (customCircleIds.length >= Circle.MAX_CUSTOM_CIRCLE_COUNT)
            return -1;

        int max = NumberUtils.max(customCircleIds);
        if (max < Circle.MAX_CUSTOM_CIRCLE_ID) {
            return max + 1;
        } else {
            for (int circleId = Circle.MIN_CUSTOM_CIRCLE_ID; circleId <= Circle.MAX_CUSTOM_CIRCLE_ID; circleId++) {
                if (!ArrayUtils.contains(customCircleIds, circleId))
                    return circleId;
            }
            return -1;
        }
    }

    @Override
    public Map<PeopleId, Integer> getFollowersCounts(Context ctx, PeopleId... friendIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getFollowersCounts", ctx,
                "friendIds", friendIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("friendIds", friendIds);

            LinkedHashMap<PeopleId, Integer> m = new LinkedHashMap<PeopleId, Integer>();
            for (PeopleId friendId : friendIds) {
                int followerCount = getFollowersCount(ctx, friendId);
                m.put(friendId, followerCount);
            }
            LC.endCall();
            return m;

        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public long[] getBorqsFriendIdsInCircles(Context ctx, long userId, int... circleIds) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getBorqsFriendIdsInCircles", ctx,
                "userId", userId, "circleIds", circleIds);

        try {
            PeopleIds friendIds = getFriendsInCircles(ctx, userId, circleIds);
            long[] r = friendIds.getIdsAsLongArray(PeopleId.USER);
            LC.endCall();
            return r;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Users getBorqsFriendsInCircles(Context ctx, long userId, int[] circleIds, String[] expCols) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getBorqsFriendsInCircles", ctx,
                "userId", userId, "circleIds", circleIds, "expCols", expCols);
        try {
            long[] ids = getBorqsFriendIdsInCircles(ctx, userId, circleIds);
            Users users = account.getUsers(ctx, expCols, ids);
            LC.endCall();
            return users;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public long[] getBorqsFriendIds(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getBorqsFriendIds", ctx,
                "userId", userId);

        try {
            PeopleIds friendIds = getFriends(ctx, userId);
            long[] r = friendIds.getIdsAsLongArray(PeopleId.USER);
            LC.endCall();
            return r;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Users getBorqsFriends(Context ctx, long userId, String[] expCols) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getBorqsFriends", ctx,
                "userId", userId, "expCols", expCols);
        try {
            long[] ids = getBorqsFriendIds(ctx, userId);
            Users users = account.getUsers(ctx, expCols, ids);
            LC.endCall();
            return users;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Users getFollowerUsers(Context ctx, PeopleId friendId, String[] expCols, Page page) {
        final LogCall LC = LogCall.startCall(L, AbstractFriendImpl.class, "getFollowerUsers", ctx,
                "friendId", friendId, "expCols", expCols);
        try {
            long[] ids = getFollowers(ctx, friendId, page);
            Users users = account.getUsers(ctx, expCols, ids);
            LC.endCall();
            return users;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    protected abstract FriendEntries getFriendEntries(Context ctx, long userId);

    protected abstract Map<PeopleId, String> getRemarks(Context ctx, long userId);

    protected void expandCircles(Context ctx, String[] expCols, Circles circles) {
        builtinCirclesExpansion.expand(ctx, expCols, circles);
        ExpansionHelper.expand(circlesExpansions, ctx, expCols, circles);
    }

    private static final String[] CIRCLE_MEMBER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
            RelUserExpansion.COL_REMARK,
    };
    protected class BuiltinCirclesExpansion implements CirclesExpansion {
        @Override
        public void expand(Context ctx, String[] expCols, Circles data) {
            if (CollectionUtils.isEmpty(data))
                return;

            if (expCols == null || ArrayUtils.contains(expCols, Circle.COL_MEMBERS))
                expandMembers(ctx, data);
        }

        private void expandMembers(Context ctx, Circles circles) {
            PeopleIds friendIds = circles.getAllFriendIds();
            if (CollectionUtils.isEmpty(friendIds)) {
                for (Circle circle : circles)
                    circle.setAddon(Circle.COL_MEMBERS, Addons.jsonAddonValue("[]"));
            } else {
                Users users = account.getUsers(ctx, CIRCLE_MEMBER_COLUMNS, friendIds.getUserIds());
                Users circleUsers = new Users();
                for (Circle circle : circles) {
                    if (circle == null || circle.getFriendIds() == null)
                        continue;

                    circleUsers.clear();
                    users.getUsers(circleUsers, circle.getFriendIds().getUserIds());
                    String json = circleUsers.toJson(CIRCLE_MEMBER_COLUMNS, true);
                    circle.setAddon(Circle.COL_MEMBERS, Addons.jsonAddonValue(json));
                }
            }
        }
    }
}
