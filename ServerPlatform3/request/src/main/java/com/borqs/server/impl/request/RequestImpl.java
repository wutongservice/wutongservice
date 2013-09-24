package com.borqs.server.impl.request;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.feature.request.*;
import com.borqs.server.platform.hook.Hook;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.test.JsonAssert;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.aop.TargetSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequestImpl implements RequestLogic {
    private static final Logger L = Logger.get(RequestImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final RequestDb db = new RequestDb();

    // hooks
    private List<RequestHook> createHooks;

    // expansion
    private final BuiltinExpansion builtinExpansion = new BuiltinExpansion();

    public AccountLogic getAccount() {
        return account;
    }

    public RequestImpl() {
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public List<RequestHook> getCreateHooks() {
        return createHooks;
    }

    public void setCreateHooks(List<RequestHook> createHooks) {
        this.createHooks = createHooks;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getRequestTable() {
        return db.getRequestTable();
    }

    public void setRequestTable(Table requestTable) {
        db.setRequestTable(requestTable);
    }

    public Table getRequestIndex() {
        return db.getRequestIndex();
    }

    public void setRequestIndex(Table requestIndex) {
        db.setRequestIndex(requestIndex);
    }

    @Override
    public void create(Context ctx, Request... requests) {
        final LogCall LC = LogCall.startCall(L, RequestImpl.class, "create",
                ctx, "requests", requests);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("requests", requests);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (requests.length == 0)
                return;

            Requests reqs = new Requests(requests);
            HookHelper.before(createHooks, ctx, reqs);

            db.create(ctx, requests);
            for (Request req : requests) {
                if (req != null)
                    OpLine.append2(ctx,
                            Actions.REQUEST, "", PeopleId.user(req.getTo()),
                            Actions.CREATE, "", Target.forRequest(req.getRequestId()));
            }

            HookHelper.after(createHooks, ctx, reqs);

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void done(Context ctx, long... requestIds) {
        final LogCall LC = LogCall.startCall(L, RequestImpl.class, "done",
                ctx, "requestIds", requestIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("requestIds", requestIds);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (requestIds.length != 0) {
                db.done(ctx, requestIds);
                OpLine.append(ctx, Actions.DONE, "", Target.forRequests(requestIds));
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Requests getAllRequests(Context ctx, long toId, int app, int type, int limit) {
        final LogCall LC = LogCall.startCall(L, RequestImpl.class, "getAllRequests",
                ctx, "toId", toId, "app", app, "type", type, "limit", limit);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("toId", toId > 0, "Invalid user id");

            Requests requests = db.gets(ctx, 0, toId, app, type, limit);
            expandRequests(ctx, Request.FULL_COLUMNS, requests);

            LC.endCall();
            return requests;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Requests getPendingRequests(Context ctx, long toId, int app, int type) {
        final LogCall LC = LogCall.startCall(L, RequestImpl.class, "getPendingRequests",
                ctx, "toId", toId, "app", app, "type", type);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("toId", toId > 0, "Invalid user id");

            Requests requests = db.gets(ctx, Request.STATUS_PENDING, toId, app, type, 0);
            expandRequests(ctx, Request.FULL_COLUMNS, requests);

            LC.endCall();
            return requests;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Requests getDoneRequests(Context ctx, long toId, int app, int type, int limit) {
        final LogCall LC = LogCall.startCall(L, RequestImpl.class, "getDoneRequests",
                ctx, "toId", toId, "app", app, "type", type, "limit", limit);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("toId", toId > 0, "Invalid user id");

            Requests requests = db.gets(ctx, Request.STATUS_DONE, toId, app, type, limit);
            expandRequests(ctx, Request.FULL_COLUMNS, requests);

            LC.endCall();
            return requests;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public long getPendingCount(Context ctx, long toId, int app, int type) {
        final LogCall LC = LogCall.startCall(L, RequestImpl.class, "getPendingCount",
                ctx, "toId", toId, "app", app, "type", type);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("toId", toId > 0, "Invalid user id");

            long count = db.getPendingCount(ctx, toId, app, type);

            LC.endCall();
            return count;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Long, int[]> getPendingTypes(Context ctx, long fromId, long... toIds) {
        final LogCall LC = LogCall.startCall(L, RequestImpl.class, "getPendingTypes",
                ctx, "fromId", fromId, "toIds", toIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("fromId", fromId > 0, "Invalid user id");

            Map<Long, int[]> m = db.getPendingTypes(ctx, fromId, toIds);

            LC.endCall();
            return m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    private void expandRequests(Context ctx, String[] expCols, Requests reqs) {
        builtinExpansion.expand(ctx, expCols, reqs);
    }

    private static final String[] SOURCE_COLUMNS = {
            User.COL_USER_ID,
            User.COL_NAME,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };

    private class BuiltinExpansion implements RequestsExpansion {
        @Override
        public void expand(Context ctx, String[] expCols, Requests data) {
            if (CollectionUtils.isEmpty(data))
                return;

            if (expCols == null || ArrayUtils.contains(expCols, Request.COL_SOURCE))
                expandSource(ctx, data);
        }

        private void expandSource(Context ctx, Requests reqs) {
            long[] userIds = reqs.getFromIds();
            Users fromUsers = account.getUsers(ctx, SOURCE_COLUMNS, userIds);
            Requests removed = new Requests();
            for (Request req : reqs) {
                if (req == null) {
                    removed.add(req);
                    continue;
                }

                User fromUser = fromUsers.getUser(req.getFrom());
                if (fromUser == null) {
                    removed.add(req);
                    continue;
                }

                String json = fromUser.toJson(SOURCE_COLUMNS, true);
                req.setAddon(Request.COL_SOURCE, Addons.jsonAddonValue(json));
            }
            reqs.removeAll(removed);
        }
    }
}
