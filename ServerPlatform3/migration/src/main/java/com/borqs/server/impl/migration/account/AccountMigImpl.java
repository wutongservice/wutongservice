package com.borqs.server.impl.migration.account;


import com.borqs.server.impl.account.UserDb;
import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.*;

public class AccountMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(AccountMigImpl.class);
    private UserDb db_account_to = new UserDb();
    private AccountMigDb db_account_from = new AccountMigDb();

    public void setUserTable(Table userTable) {
        db_account_to.setUserTable(userTable);
    }

    public void setUserPropertyTable(Table userPropertyTable) {
        db_account_to.setPropertyTable(userPropertyTable);
    }

    public void setOldUserTable(Table oldUserTable) {
        db_account_from.setUserTable(oldUserTable);
    }

    public void setOldUserPropertyTable(Table oldUserPropertyTableTable) {
        db_account_from.setPropertyTable(oldUserPropertyTableTable);
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db_account_from.setSqlExecutor(sqlExecutor);
        db_account_to.setSqlExecutor(sqlExecutor);
    }

    public void setCibindTable(Table cibindTable) {
        db_account_from.setCibindTable(cibindTable);
    }

    @Override
    public List<String> getDependencies() {
        return null;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("account.mig")) {
            accountMigration(Context.create());
        }
    }

    public void accountMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, AccountMigImpl.class, "accountMigration", ctx);
        long[] list = db_account_from.getAllUserIds(ctx);
        Users users = db_account_from.getUsers(ctx, list);

        //migrate user record
        List<User> errorList = new ArrayList<User>();
        for (User user : users) {
            try {
                db_account_to.createUserMigration(ctx, user);

            } catch (Exception e) {
                errorList.add(user);

            }
        }


        // migrate cibind record
        for (User user : users) {
            List<BindingInfo> bindList = getBindInfo(user);
            for (BindingInfo bi : bindList) {
                ctx.setViewer(user.getUserId());
                try {
                    db_account_from.bind(ctx, bi);
                    LC.endCall();
                } catch (Exception e) {
                    LC.endCall();
                }
            }
        }
    }

    private List<BindingInfo> getBindInfo(User user) {
        List<BindingInfo> list = new ArrayList<BindingInfo>();

        for (String col : user.getAddonColumns()) {
            BindingInfo bindingInfo = null;
            if (col.startsWith("login_email"))
                bindingInfo = new BindingInfo(BindingInfo.EMAIL, (String) user.getAddon(col, null));

            if (col.startsWith("login_phone"))
                bindingInfo = new BindingInfo(BindingInfo.MOBILE_TEL, (String) user.getAddon(col, null));

            if (bindingInfo != null)
                list.add(bindingInfo);

        }

        return list;
    }

    public Map<Long, String> getAllUserIdMap(Context ctx) {
        long[] list = db_account_from.getAllUserIds(ctx);
        Map<Long, String> map = new HashMap<Long, String>();
        for (Long l : list) {
            map.put(l, String.valueOf(l));
        }
        return map;
    }

   /* public void accountSortKeyMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, AccountMigImpl.class, "accountMigration", ctx);
        long[] list = db_account_from.getAllUserIds(ctx);
        Users users = db_account_from.getUsers(ctx, list);

        //migrate user record
        List<User> errorList = new ArrayList<User>();
        for (User user : users) {
            try {
                String disPlayName = user.getDisplayName();
                updateSortKey(user);
                db_account_from.updateSortKey(ctx, user);

            } catch (Exception e) {
                errorList.add(user);
            }
        }

    }*/

   /* private void updateSortKey(User user){
        try{
        if(user.getName() != null){
            String displayName = user.getDisplayName();
        Hanyu2Pinyin hanyu = new Hanyu2Pinyin();
        String sort_key = hanyu.getStringPinYin(displayName);
        sort_key += displayName;
        user.setAddon("sort_key", sort_key);
        }}catch(Exception e){
            
        }

    }*/
}
