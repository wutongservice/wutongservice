package com.borqs.server.impl.opline;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.feature.opline.OpLineLogic;
import com.borqs.server.platform.feature.opline.Operation;
import com.borqs.server.platform.feature.opline.Operations;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.lang.ArrayUtils;


public class OpLineImpl implements OpLineLogic, Initializable {

    private static final Logger L = Logger.get(OpLineImpl.class);

    private final OpLineDb db = new OpLineDb();

    private static final OpLineImpl instance = new OpLineImpl();

    private OpLineImpl() {

    }

    public static OpLineImpl getInstance() {
        return instance;
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getHistoryTable() {
        return db.getHistoryTable();
    }

    public void setHistoryTable(Table historyTable) {
        db.setHistoryTable(historyTable);
    }

    @Override
    public void init() throws Exception {
        OpLine.setInstance(instance);
    }

    @Override
    public void destroy() {
        OpLine.setInstance(null);
    }

    @Override
    public void appends(Context ctx, Operation... opers) {
        final LogCall LC = LogCall.startCall(L, OpLineImpl.class, "appends", ctx,
                "opers", opers);
        try {
            ParamChecker.notNull("ctx", ctx);
            if (ArrayUtils.isNotEmpty(opers))
                db.puts(ctx, new Operations(opers));
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Operations getOperationsBefore(Context ctx, long userId, long beforeOperId, int count) {
        final LogCall LC = LogCall.startCall(L, OpLineImpl.class, "getOperationsBefore", ctx,
                "userId", userId, "beforeOperId", beforeOperId, "count", count);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("count", count > 0 && count <= 100, "Illegal count");
            Operations opers = db.getOperationsBefore(ctx, userId, beforeOperId, count);
            LC.endCall();
            return opers;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Operation getLastOperation(Context ctx, long userId, int[] actions) {
        final LogCall LC = LogCall.startCall(L, OpLineImpl.class, "getLastOperation", ctx,
                "userId", userId, "actions", actions);
        try {
            ParamChecker.notNull("ctx", ctx);
            Operation op = db.getLastOperation(ctx, userId, actions);
            LC.endCall();
            return op;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Operations getOpsWithFlag(Context ctx, long userId, int[] actions, int flag, long minTime) {
        final LogCall LC = LogCall.startCall(L, OpLineImpl.class, "getOpsWithFlag", ctx,
                "userId", userId, "actions", actions, "flag", flag, "minTime", minTime);
        try {
            ParamChecker.notNull("ctx", ctx);
            Operations opers = db.getOpsWithFlag(ctx, userId, actions, flag, minTime);
            LC.endCall();
            return opers;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Operations getOpsWithFlagByInterval(Context ctx, long userId, int[] actions, int flag, long maxInterval, long minTime) {
        final LogCall LC = LogCall.startCall(L, OpLineImpl.class, "getOpsWithFlagByInterval", ctx,
                new Object[][] {
                        {"userId", userId},
                        {"actions", actions},
                        {"flag", flag},
                        {"maxInterval", maxInterval},
                        {"minTime", minTime},
                });
        try {
            ParamChecker.notNull("ctx", ctx);
            Operations opers = db.getOpsWithFlagByInterval(ctx, userId, actions, flag, maxInterval, minTime);
            LC.endCall();
            return opers;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void setFlag(Context ctx, int flag, long... operIds) {
        final LogCall LC = LogCall.startCall(L, OpLineImpl.class, "setFlag", ctx,
                "flag", flag, "operIds", operIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            db.setFlag(ctx, flag, operIds);
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }
}
