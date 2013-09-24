package com.borqs.server.impl.migration.suggest;


import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.impl.psuggest.PeopleSuggestDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.*;

public class SuggestMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(SuggestMigImpl.class);

    private final SuggestMigDb db_migration = new SuggestMigDb();
    private final PeopleSuggestDb suggestDb = new PeopleSuggestDb();

    private AccountMigImpl account;


    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        suggestDb.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
    }

    public void setNewSuggestTable(Table newSuggestTable) {
        suggestDb.setPsuggestTable(newSuggestTable);
    }


    public void setOldSuggestTable(Table oldSuggestTable) {
        db_migration.setSuggestTable(oldSuggestTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("suggest.mig")) {
            suggestMigration(Context.create());
        }
    }

    public void suggestMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, SuggestMigImpl.class, "suggestMigration", ctx);

        List<PeopleSuggest> suggestList = null;

        try {

            db_migration.setUserIdMap(getAllUserIdMap(ctx));


            suggestList = db_migration.getSuggest(ctx);

            for (PeopleSuggest suggest : suggestList) {
                try {
                    if (suggest != null) {
                        ctx.setViewer(suggest.getUser());
                        suggestDb.create(ctx, suggest);
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
