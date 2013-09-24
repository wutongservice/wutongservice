package com.borqs.server.impl.migration.friend;


import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FriendMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(FriendMigImpl.class);

    private final FriendMigDb db_migration = new FriendMigDb();

    public void setFriendTable(Table friendTable) {
        db_migration.setFriendTable(friendTable);
    }

    public void setFriendOldTable(Table friendOldTable) {
        db_migration.setFirendOldTable(friendOldTable);
    }

    public void setFollowerTable(Table followerTable) {
        db_migration.setFollowerTable(followerTable);
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db_migration.setSqlExecutor(sqlExecutor);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        list.add("circle.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("friend.mig")) {
            friendMigration(Context.create());
        }
    }

    public void friendMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, FriendMigImpl.class, "friendMigration", ctx);

        try {
            db_migration.friendMigration(ctx);
            LC.endCall();
        } catch (Exception e) {
            LC.endCall(e);
        }

    }

}
