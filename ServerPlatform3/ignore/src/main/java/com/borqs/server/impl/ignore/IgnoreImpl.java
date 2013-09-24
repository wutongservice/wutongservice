package com.borqs.server.impl.ignore;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ParamChecker;


public class IgnoreImpl implements IgnoreLogic {
    private static final Logger L = Logger.get(IgnoreImpl.class);
    // db
    private final IgnoreDb db = new IgnoreDb();

    private AccountLogic account;

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getIgnoreTable() {
        return db.getIgnoreTable();
    }

    public void setIgnoreTable(Table ignoreTable) {
        db.setIgnoreTable(ignoreTable);
    }


    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }


    @Override
    public void ignore(Context ctx, int feature, Target... targets) {
        final LogCall LC = LogCall.startCall(L, IgnoreImpl.class, "ignore",
                ctx, "feature", feature, "targets", targets);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("feature", feature);
            ParamChecker.notNull("targets", targets);

            db.ignore(ctx, feature,targets);
            OpLine.append(ctx, Actions.IGNORE, feature, targets);

            LC.endCall();

        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void unignore(Context ctx, int feature, Target... targets) {
        final LogCall LC = LogCall.startCall(L, IgnoreImpl.class, "unignore",
                ctx, "feature", feature, "targets", targets);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("feature", feature);
            ParamChecker.notNull("targets", targets);

            db.unIgnore(ctx, feature, targets);
            OpLine.append(ctx, Actions.UNIGNORE, feature, targets);

            LC.endCall();

        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Target[] getIgnored(Context ctx, long userId, int feature) {
                final LogCall LC = LogCall.startCall(L, IgnoreImpl.class, "getIgnored",
                ctx, "feature", feature, "userId", userId);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("feature", feature);
            ParamChecker.notNull("userId", userId);

            Target[] targets =  db.getIgnore(ctx,userId, feature);

            LC.endCall();
            return targets;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }
}
