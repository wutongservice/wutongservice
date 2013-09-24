package com.borqs.server.impl.migration.circle;


import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CircleMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(CircleMigImpl.class);
    private CircleMigDb db_circle = new CircleMigDb();

    public void setNewCircleTable(Table newCircleTable) {
        db_circle.setCircleTable(newCircleTable);
    }
    public void setOldCircleTable(Table oldCircleTable) {
        db_circle.setCircleOldTable(oldCircleTable);
    }


    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db_circle.setSqlExecutor(sqlExecutor);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("circle.mig")) {
            circleMigration(Context.create());
        }
    }

    public void circleMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, CircleMigImpl.class, "accountMigration", ctx);

            try {
                db_circle.circleMigration(ctx);
                LC.endCall();
            } catch (Exception e) {
                LC.endCall(e);
            }
        }
    

}
