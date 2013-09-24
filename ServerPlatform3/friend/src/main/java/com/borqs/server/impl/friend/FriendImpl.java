package com.borqs.server.impl.friend;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.util.ParamChecker;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FriendImpl extends AbstractFriendImpl implements FriendLogic {
    private static final Logger L = Logger.get(FriendImpl.class);

    private final FriendDb db = new FriendDb();

    private List<CircleHook> createCircleHooks;
    private List<CircleHook> destroyCircleHooks;
    private List<CircleHook> updateCircleHooks;
    private List<FriendsHook> addFriendsHooks;
    private List<RemarkHook> remarkHooks;


    public FriendImpl() {
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getCircleTable() {
        return db.getCircleTable();
    }

    public void setCircleTable(Table circleTable) {
        db.setCircleTable(circleTable);
    }

    public Table getFollowerTable() {
        return db.getFollowerTable();
    }

    public void setFriendTable(Table friendTable) {
        db.setFriendTable(friendTable);
    }

    public void setRemarkTable(Table remarkTable) {
        db.setRemarkTable(remarkTable);
    }

    public void setFollowerTable(Table followerTable) {
        db.setFollowerTable(followerTable);
    }

    public Table getFriendTable() {
        return db.getFriendTable();
    }

    public Table getRemarkTable() {
        return db.getRemarkTable();
    }

    public List<CircleHook> getCreateCircleHooks() {
        return createCircleHooks;
    }

    public void setCreateCircleHooks(List<CircleHook> createCircleHooks) {
        this.createCircleHooks = createCircleHooks;
    }

    public List<CircleHook> getDestroyCircleHooks() {
        return destroyCircleHooks;
    }

    public void setDestroyCircleHooks(List<CircleHook> destroyCircleHooks) {
        this.destroyCircleHooks = destroyCircleHooks;
    }

    public List<CircleHook> getUpdateCircleHooks() {
        return updateCircleHooks;
    }

    public void setUpdateCircleHooks(List<CircleHook> updateCircleHooks) {
        this.updateCircleHooks = updateCircleHooks;
    }

    @Override
    protected FriendEntries getFriendEntries(Context ctx, long userId) {
        return db.getFriendEntries(ctx, userId);
    }

    public List<FriendsHook> getAddFriendsHooks() {
        return addFriendsHooks;
    }

    public void setAddFriendsHooks(List<FriendsHook> addFriendsHooks) {
        this.addFriendsHooks = addFriendsHooks;
    }

    public List<RemarkHook> getRemarkHooks() {
        return remarkHooks;
    }

    public void setRemarkHooks(List<RemarkHook> remarkHooks) {
        this.remarkHooks = remarkHooks;
    }

    @Override
    public Circle createCustomCircle(Context ctx, String circleName) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "createCustomCircle", ctx,
                "circleName", circleName);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notEmpty("circleName", circleName);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());

            Circle hd = new Circle();
            hd.setCircleName(circleName);
            HookHelper.before(createCircleHooks, ctx, hd);
            circleName = hd.getCircleName();

            hd = db.createCustomCircle(ctx, circleName);
            OpLine.append(ctx, Actions.CREATE, circleName);

            HookHelper.after(createCircleHooks, ctx, hd);

            LC.endCall();
            return hd;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean destroyCustomCircle(Context ctx, int circleId) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "destroyCustomCircle", ctx,
                "circleId", circleId);
        try {
            ParamChecker.notNull("ctx", ctx);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());

            boolean b = false;
            if (Circle.isCustomCircleId(circleId)) {
                Circle hd = new Circle(circleId);
                HookHelper.before(destroyCircleHooks, ctx, hd);

                b = db.destroyCustomCircle(ctx, circleId);
                OpLine.append(ctx, Actions.CREATE, circleId);

                HookHelper.after(destroyCircleHooks, ctx, hd);
            }


            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean updateCustomCircleName(Context ctx, int circleId, String circleName) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "updateCustomCircleName", ctx,
                "circleId", circleId, "circleName", circleName);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notEmpty("circleName", circleName);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());

            boolean b = false;
            if (Circle.isCustomCircleId(circleId)) {
                Circle hd = new Circle(circleId);
                hd.setCircleName(circleName);

                HookHelper.before(updateCircleHooks, ctx, hd);

                b = db.updateCustomCircleName(ctx, circleId, circleName);
                OpLine.append(ctx, Actions.UPDATE, circleId + "->" + circleName);

                HookHelper.after(updateCircleHooks, ctx, hd);
            }


            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public void setFriendIntoCircles(Context ctx, int reason, PeopleId friendId, int... circleIds) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "setFriendIntoCircles", ctx,
                "friendId", friendId, "circleIds", circleIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("friendId", friendId);
            ParamChecker.notNull("circleIds", circleIds);
            for (int circleId : circleIds) {
                ParamChecker.mustTrue("circleIds", Circle.isActualCircle(circleId), "Illegal circle id");
            }

            AccountHelper.checkUser(account, ctx, ctx.getViewer());

            circleIds = trimCircleIds(circleIds);

            ArrayList<FriendsHook.Entry> hd = new ArrayList<FriendsHook.Entry>();
            hd.add(new FriendsHook.Entry(ctx.getViewer(), friendId, reason, circleIds));
            HookHelper.before(addFriendsHooks, ctx, hd);

            db.setFriendIntoCircles(ctx, reason, friendId, circleIds);
            OpLine.append(ctx, Actions.SET_FRIENDS, StringHelper.join(circleIds, ","), friendId);

            HookHelper.after(addFriendsHooks, ctx, hd);
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    private static int[] trimCircleIds(int[] circleIds) {
        return ArrayUtils.contains(circleIds, Circle.CIRCLE_BLOCKED) ? new int[]{Circle.CIRCLE_BLOCKED} : circleIds;
    }

    @Override
    public void addFriendsIntoCircle(Context ctx, int reason, PeopleIds friendIds, int circleId) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "addFriendsIntoCircle", ctx,
                "friendIds", friendIds, "int", circleId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("friendIds", friendIds);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());

            if (!friendIds.isEmpty()) {
                List<FriendsHook.Entry> hd = getFriendsHookData(ctx, ctx.getViewer(), friendIds, reason);
                for (FriendsHook.Entry fhe : hd)
                    fhe.circleIds = ArrayHelper.addAsSet(fhe.circleIds, circleId);

                HookHelper.before(addFriendsHooks, ctx, hd);

                db.addFriendsIntoCircle(ctx, reason, friendIds, circleId);
                OpLine.append(ctx, Actions.ADD_FRIENDS, circleId, friendIds.toIdArray());

                HookHelper.after(addFriendsHooks, ctx, hd);
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void removeFriendsInCircle(Context ctx, PeopleIds friendIds, int circleId) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "removeFriendsInCircle", ctx,
                "friendIds", friendIds, "int", circleId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("friendIds", friendIds);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());

            if (!friendIds.isEmpty()) {
                List<FriendsHook.Entry> hd = getFriendsHookData(ctx, ctx.getViewer(), friendIds, 0);
                for (FriendsHook.Entry fhe : hd)
                    fhe.circleIds = ArrayUtils.removeElement(fhe.circleIds, circleId);

                //HookHelper.before(addFriendsHooks, ctx, hd);

                db.removeFriendsInCircle(ctx, friendIds, circleId);
                OpLine.append(ctx, Actions.DELETE_FRIENDS, circleId, friendIds.toIdArray());

                //HookHelper.after(addFriendsHooks, ctx, hd);
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public long[] getFollowers(Context ctx, PeopleId friendId, Page page) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "getFollowers", ctx,
                "friendId", friendId, "page", page);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("friendId", friendId);
            long[] followerIds = db.getFollowers(ctx, friendId, page);
            LC.endCall();
            return followerIds;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public int getFollowersCount(Context ctx, PeopleId friendId) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "getFollowersCount", ctx,
                "friendId", friendId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("friendId", friendId);
            int followerCount = db.getFollowersCount(ctx, friendId);
            LC.endCall();
            return followerCount;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Relationships getRelationships(Context ctx, PeopleId viewer, PeopleId... targets) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "getRelationships", ctx,
                "viewer", viewer, "targets", targets);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("viewer", viewer);
            ParamChecker.notNull("targets", targets);

            Relationships rels = new Relationships();
            if (targets.length > 0) {
                db.getRelationships(rels, ctx, viewer, targets);
            }
            LC.endCall();
            return rels;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    protected Map<PeopleId, String> getRemarks(Context ctx, long userId) {
        return db.getRemarks(ctx, userId);
    }

    @Override
    public void setRemark(Context ctx, PeopleId friendId, String remark) {
        final LogCall LC = LogCall.startCall(L, FriendImpl.class, "setRemark", ctx,
                "friendId", friendId, "remark", remark);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("friendId", friendId);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());

            RemarkHook.Data hd = new RemarkHook.Data(ctx.getViewer(), friendId, remark);
            HookHelper.before(remarkHooks, ctx, hd);

            db.setRemark(ctx, friendId, remark);
            OpLine.append(ctx, Actions.SET_REMARK, remark, friendId);

            HookHelper.after(remarkHooks, ctx, hd);
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }
}
