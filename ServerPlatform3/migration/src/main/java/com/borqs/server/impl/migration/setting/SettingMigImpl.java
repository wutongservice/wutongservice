package com.borqs.server.impl.migration.setting;


import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.impl.setting.SettingDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.*;

public class SettingMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(SettingMigImpl.class);

    private final SettingMigDb db_migration = new SettingMigDb();
    private final SettingDb dbNewSetting = new SettingDb();

    private AccountMigImpl account;


    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        dbNewSetting.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
    }

    public void setNewSettingTable(Table newSettingTable) {
        dbNewSetting.setSettingTable(newSettingTable);
    }


    public void setOldSettingTable(Table oldSettingTable) {
        db_migration.setSettingTable(oldSettingTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("setting.mig")) {
            settingMigration(Context.create());
        }
    }

    public void settingMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, SettingMigImpl.class, "settingMigration", ctx);

        List<MigrationSetting> settings = null;

        try {

            db_migration.setUserIdMap(getAllUserIdMap(ctx));


            settings = db_migration.getSetting(ctx);

            for (MigrationSetting setting : settings) {
                try {
                    if (setting != null) {
                        ctx.setViewer(setting.getUser());
                        Map<String,String> map = new HashMap<String, String>();
                        map.put(setting.getSettingKey(),setting.getSettingValue());
                        dbNewSetting.sets(ctx, map);
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
