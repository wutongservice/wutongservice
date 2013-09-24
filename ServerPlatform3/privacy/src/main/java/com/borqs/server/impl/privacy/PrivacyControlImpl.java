package com.borqs.server.impl.privacy;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.feature.privacy.*;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class PrivacyControlImpl implements PrivacyControlLogic {
    private static final Logger L = Logger.get(PrivacyControlImpl.class);

    // logic
    private AccountLogic account;
    private FriendLogic friend;

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    // db
    private final PrivacyDb db = new PrivacyDb();

    public PrivacyControlImpl() {
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

    public Table getPrivacyTable() {
        return db.getPrivacyTable();
    }

    public void setPrivacyTable(Table privacyTable) {
        db.setPrivacyTable(privacyTable);
    }


    @Override
    public void setPrivacy(Context ctx, PrivacyEntry... entries) {
        final LogCall LC = LogCall.startCall(L, PrivacyControlImpl.class, "setPrivacy",
                ctx, "entries", entries);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("entries", entries);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (entries.length > 0) {
                // remove entry if entry.target.isUser(entry.userId)
                entries = removeSelf(entries);
                if (entries.length > 0) {
                    db.sets(ctx, entries);
                    OpLine.append(ctx, Actions.UPDATE, StringUtils.join(entries, ","), ctx.getViewerAsPeople());
                }
            }

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    private PrivacyEntry[] removeSelf(PrivacyEntry[] pes) {
        boolean needRemoveSelf = false;
        for (PrivacyEntry pe : pes) {
            if (pe.target.isUser(pe.userId)) {
                needRemoveSelf = true;
                break;
            }
        }

        if (!needRemoveSelf)
            return pes;

        ArrayList<PrivacyEntry> l = new ArrayList<PrivacyEntry>(pes.length);
        for (PrivacyEntry pe : pes) {
            if (!pe.target.isUser(pe.userId))
                l.add(pe);
        }
        return l.toArray(new PrivacyEntry[l.size()]);
    }

    @Override
    public List<PrivacyEntry> getPrivacy(Context ctx, long userId, String... res) {
        final LogCall LC = LogCall.startCall(L, PrivacyControlImpl.class, "getPrivacy",
                ctx, "userId", userId, "res", res);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Invalid user id");

            AccountHelper.checkUser(account, ctx, userId);

            if (ArrayUtils.isEmpty(res))
                res = PrivacyResources.RESOURCES;

            List<PrivacyEntry> entries = db.gets(ctx, userId, res);

            Set<String> set = new HashSet<String>();
            for (PrivacyEntry entry : entries) {
                if ((entry.target.scope == PrivacyTarget.SCOPE_ALL)
                        || (entry.target.scope == PrivacyTarget.SCOPE_FRIEND))
                    set.add(entry.resource);
            }

            for (String r : res) {
                if (!set.contains(r)) {
                    entries.add(PrivacyPolicies.getDefault(r));
                }
            }

            LC.endCall();
            return entries;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void clearPrivacy(Context ctx, String... res) {
        final LogCall LC = LogCall.startCall(L, PrivacyControlImpl.class, "clearPrivacy",
                ctx, "res", res);

        try {
            ParamChecker.notNull("ctx", ctx);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (ArrayUtils.isEmpty(res))
                res = PrivacyResources.RESOURCES;

            db.delete(ctx, res);
            OpLine.append(ctx, Actions.DESTROY, StringUtils.join(res, ","), ctx.getViewerAsPeople());

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean check(Context ctx, long viewerId, String res, long userId) {
        final LogCall LC = LogCall.startCall(L, PrivacyControlImpl.class, "check",
                ctx, "viewerId", viewerId, "res", res, "userId", userId);

        try {
            Map<Long, Boolean> m = check(ctx, viewerId, res, new long[]{userId});
            LC.endCall();
            return MapUtils.getBoolean(m, userId, false);
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Long, Boolean> check(Context ctx, long viewerId, String res, long[] userIds) {
        final LogCall LC = LogCall.startCall(L, PrivacyControlImpl.class, "check",
                ctx, "viewerId", viewerId, "res", res, "userIds", userIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notBlank("res", res);
            ParamChecker.notNull("userIds", userIds);

            Map<Long, Boolean> m = new LinkedHashMap<Long, Boolean>();
            if (ArrayUtils.isEmpty(userIds))
                return m;

            List<Long> userIdsLst = CollectionsHelper.toLongList(userIds);
            if (userIdsLst.contains(viewerId))
                m.put(viewerId, true);

            userIdsLst = computeMissingIds(userIdsLst, m);
            block(ctx, userIdsLst, m, viewerId);

            userIdsLst = computeMissingIds(userIdsLst, m);
            userScope(ctx, userIdsLst, m, viewerId, res);

            userIdsLst = computeMissingIds(userIdsLst, m);
            circleScope(ctx, userIdsLst, m, viewerId, res);

            userIdsLst = computeMissingIds(userIdsLst, m);
            friendScope(ctx, userIdsLst, m, viewerId, res);

            userIdsLst = computeMissingIds(userIdsLst, m);
            allScope(ctx, userIdsLst, m, viewerId, res);

            userIdsLst = computeMissingIds(userIdsLst, m);
            defaultPolicy(userIdsLst, m, res);

            LC.endCall();
            return m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    private List<Long> computeMissingIds(List<Long> userIds, Map<Long, Boolean> m) {
        List<Long> missingIds = new ArrayList<Long>();

        for (Long userId : userIds) {
            if (!m.containsKey(userId))
                missingIds.add(userId);
        }

        return missingIds;
    }

    private void block(Context ctx, List<Long> userIds, Map<Long, Boolean> m, long viewerId) {
        for (long userId : userIds) {
            int[] circleIds = friend.getRelationship(ctx, PeopleId.fromId(viewerId), PeopleId.fromId(userId))
                    .getViewerInTargetCircleIds();

            if (ArrayUtils.contains(circleIds, Circle.CIRCLE_BLOCKED))
                m.put(userId, false);
        }
    }

    private void userScope(Context ctx, List<Long> userIds, Map<Long, Boolean> m, long viewerId, String res) {
        List<PrivacyEntry> entries = db.check(ctx, CollectionsHelper.toLongArray(userIds),
                res, PrivacyTarget.SCOPE_USER, String.valueOf(viewerId));

        for (PrivacyEntry pe : entries) {
            m.put(pe.userId, pe.allow);
        }
    }

    private void circleScope(Context ctx, List<Long> userIds, Map<Long, Boolean> m, long viewerId, String res) {
        Map<Long, int[]> userCircles = new LinkedHashMap<Long, int[]>();
        for (long userId : userIds) {
            int[] circleIds = friend.getRelationship(ctx, PeopleId.fromId(viewerId), PeopleId.fromId(userId))
                    .getViewerInTargetCircleIds();
            userCircles.put(userId, circleIds);
        }

        List<PrivacyEntry> entries = db.check(ctx, CollectionsHelper.toLongArray(userIds),
                res, PrivacyTarget.SCOPE_CIRCLE, String.valueOf(viewerId));

        for (PrivacyEntry pe : entries) {

            int[] circleIds = userCircles.get(pe.userId);
            if (ArrayUtils.contains(circleIds, Integer.parseInt(pe.target.id))) {
                if (m.containsKey(pe.userId) && m.get(pe.userId))
                    continue;
                m.put(pe.userId, pe.allow);
            }
        }
    }

    private void friendScope(Context ctx, List<Long> userIds, Map<Long, Boolean> m, long viewerId, String res) {
        Map<Long, int[]> userCircles = new LinkedHashMap<Long, int[]>();
        for (long userId : userIds) {
            int[] circleIds = friend.getRelationship(ctx, PeopleId.fromId(viewerId), PeopleId.fromId(userId))
                    .getViewerInTargetCircleIds();
            userCircles.put(userId, circleIds);
        }

        List<PrivacyEntry> entries = db.check(ctx, CollectionsHelper.toLongArray(userIds),
                res, PrivacyTarget.SCOPE_FRIEND, String.valueOf(viewerId));

        for (PrivacyEntry pe : entries) {
            int[] circleIds = userCircles.get(pe.userId);
            if (circleIds.length > 0)
                m.put(pe.userId, pe.allow);
        }
    }

    private void allScope(Context ctx, List<Long> userIds, Map<Long, Boolean> m, long viewerId, String res) {
        List<PrivacyEntry> entries = db.check(ctx, CollectionsHelper.toLongArray(userIds),
                res, PrivacyTarget.SCOPE_ALL, String.valueOf(viewerId));

        for (PrivacyEntry pe : entries) {
            m.put(pe.userId, pe.allow);
        }
    }

    private void defaultPolicy(List<Long> userIds, Map<Long, Boolean> m, String res) {
        boolean allow = PrivacyPolicies.getDefaultBoolean(res);

        for (long userId : userIds) {
            m.put(userId, allow);
        }
    }

    @Override
    public AllowedIds getAllowIds(Context ctx, long userId, String res) {
        final LogCall LC = LogCall.startCall(L, PrivacyControlImpl.class, "getAllowIds",
                ctx, "userId", userId, "res", res);

        try {
            List<PrivacyEntry> entries = getPrivacy(ctx, userId, res);

            List<Long> ids = new ArrayList<Long>();

            boolean isNormal = true;
            for (PrivacyEntry entry : entries) {
                if ((entry.target.scope == PrivacyTarget.SCOPE_ALL)
                        && (entry.allow)) {
                    isNormal = false;
                    break;
                }
            }

            if (L.isDebugEnabled())
                L.debug(ctx, "Is normal mode: " + isNormal);

            for (PrivacyEntry entry : entries) {
                switch (entry.target.scope) {
                    case PrivacyTarget.SCOPE_FRIEND:
                        if (isNormal == entry.allow) {
                            PeopleIds friendIds = friend.getFriends(ctx, userId);
                            ids.addAll(CollectionsHelper.toLongList(friendIds.getIdsAsLongArray(PeopleId.USER)));
                        }
                        break;
                    case PrivacyTarget.SCOPE_CIRCLE:
                        if (isNormal == entry.allow) {
                            PeopleIds friendIds = friend.getFriendsInCircles(ctx, userId, Integer.parseInt(entry.target.id));
                            ids.addAll(CollectionsHelper.toLongList(friendIds.getIdsAsLongArray(PeopleId.USER)));
                        }
                        break;
                    case PrivacyTarget.SCOPE_USER:
                        if (isNormal == entry.allow)
                            ids.add(Long.parseLong(entry.target.id));
                        break;
                }
            }

            LC.endCall();
            return isNormal ? AllowedIds.normal(ids) : AllowedIds.exclusion(ids);
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void mutualAllow(Context ctx, String res, long userId) {
        final LogCall LC = LogCall.startCall(L, PrivacyControlImpl.class, "mutualAllow",
                ctx, "res", res, "userId", userId);

        try {
            long viewerId = ctx.getViewer();
            PrivacyEntry pe0 = PrivacyEntry.of(viewerId, res,
                    new PrivacyTarget(PrivacyTarget.SCOPE_USER, String.valueOf(userId)), true);
            setPrivacy(ctx, pe0);

//        Context ctx1 = ctx.copy();
            Context ctx1 = ctx.create();
            ctx1.setViewer(userId);
            PrivacyEntry pe1 = PrivacyEntry.of(userId, res,
                    new PrivacyTarget(PrivacyTarget.SCOPE_USER, String.valueOf(viewerId)), true);
            setPrivacy(ctx1, pe1);

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }
}
