package com.borqs.server.impl.migration.request;


import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.impl.request.RequestDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.*;

public class RequestMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(RequestMigImpl.class);

    private final RequestMigDb db_migration = new RequestMigDb();
    private final RequestDb dbNewRequest = new RequestDb();

    private AccountMigImpl account;


    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        dbNewRequest.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
    }

    public void setNewRequestTable(Table newRequestTable) {
        dbNewRequest.setRequestTable(newRequestTable);
    }

    public void setNewRequestIndexTable(Table newRequestIndexTable) {
        dbNewRequest.setRequestIndex(newRequestIndexTable);
    }

    public void setOldRequestTable(Table oldSettingTable) {
        db_migration.setRequestTable(oldSettingTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("request.mig")) {
            requestMigration(Context.create());
        }
    }

    public void requestMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, RequestMigImpl.class, "settingMigration", ctx);

        List<Request> requestList = null;

        try {

            db_migration.setUserIdMap(getAllUserIdMap(ctx));


            requestList = db_migration.getRequest(ctx);

            for (Request request : requestList) {
                try {
                    if (request != null) {

                        dbNewRequest.create(ctx,request);
                    }
                } catch (RuntimeException e) {
                    LC.endCall();
                    throw e;
                }
            }

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall();
            throw e;
        }
    }


    private Map<Long, String> getAllUserIdMap(Context ctx) {
        return account.getAllUserIdMap(ctx);
    }

}
