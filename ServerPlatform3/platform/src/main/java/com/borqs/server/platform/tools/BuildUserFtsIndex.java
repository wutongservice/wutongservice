package com.borqs.server.platform.tools;


import com.borqs.server.platform.app.AppMain;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.UserSimpleFts;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ObjectHolder;
import org.apache.commons.lang.ArrayUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class BuildUserFtsIndex extends SqlSupport implements AppMain {

    private Table userTable;
    private AccountLogic account;
    private UserSimpleFts userFts;

    public BuildUserFtsIndex() {
    }

    public Table getUserTable() {
        return userTable;
    }

    public void setUserTable(Table userTable) {
        this.userTable = userTable;
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public UserSimpleFts getUserFts() {
        return userFts;
    }

    public void setUserFts(UserSimpleFts userFts) {
        this.userFts = userFts;
    }

    @Override
    public void run(String[] args) throws Exception {
        ObjectHolder<Long> startUserId = new ObjectHolder<Long>(0L);
        for (;;) {
            long[] userIds = readUserIds(startUserId, 1000);
            if (ArrayUtils.isEmpty(userIds))
                break;

            createFtIndex(userIds);
        }
    }

    private void createFtIndex(long[] userIds) {
        Context ctx = Context.createInternalDummy();
        Users users = account.getUsers(ctx, User.FULL_COLUMNS, userIds);
        userFts.saveUsers(ctx, users.toUserArray());
    }

    private long[] readUserIds(final ObjectHolder<Long> startId, final int count) {
        final ShardResult userSR = userTable.getShard(0);
        final ArrayList<Long> userIds = new ArrayList<Long>();
        sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = new Sql()
                        .select("user_id")
                        .from(userSR.table)
                        .where("user_id > :start", "start", startId.value)
                        .orderBy("user_id", "ASC")
                        .limit(count).toString();

                SqlExecutor.executeList(null, conn, sql, userIds, new ResultSetReader<Long>() {
                    @Override
                    public Long read(ResultSet rs, Long reuse) throws SQLException {
                        return rs.getLong("user_id");
                    }
                });
                return null;
            }
        });
        if (!userIds.isEmpty())
            startId.value = userIds.get(userIds.size() - 1);
        return CollectionsHelper.toLongArray(userIds);
    }
}
