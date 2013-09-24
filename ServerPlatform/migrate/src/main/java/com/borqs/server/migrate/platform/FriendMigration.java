package com.borqs.server.migrate.platform;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.migrate.Migrate;
import com.borqs.server.base.migrate.MigrateStopException;
import com.borqs.server.base.migrate.handler.CounterMigrateHandler;
import com.borqs.server.base.migrate.input.SQLInput;
import com.borqs.server.base.migrate.output.SQLInsertOutput;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.migrate.Migration;
import com.borqs.server.service.platform.Constants;
import org.apache.commons.collections.iterators.ArrayListIterator;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class FriendMigration extends Migration {
    // old: friendships
    /*
        +-------------+------------+------+-----+---------+-------+
        | Field       | Type       | Null | Key | Default | Extra |
        +-------------+------------+------+-----+---------+-------+
        | uid         | bigint(20) | NO   | PRI | NULL    |       |
        | followingid | bigint(20) | NO   | PRI | NULL    |       |
        | isvalid     | tinyint(1) | NO   |     | 1       |       |
        +-------------+------------+------+-----+---------+-------+
     */

    // new: friend
    /*
        +--------+-------------+------+-----+---------+-------+
        | Field  | Type        | Null | Key | Default | Extra |
        +--------+-------------+------+-----+---------+-------+
        | user   | bigint(20)  | NO   | PRI | NULL    |       |
        | friend | bigint(20)  | NO   | PRI | NULL    |       |
        | circle | smallint(4) | NO   | PRI | 0       |       |
        +--------+-------------+------+-----+---------+-------+

     */

    // new:
    /*
        +--------------+-------------+------+-----+---------+-------+
        | Field        | Type        | Null | Key | Default | Extra |
        +--------------+-------------+------+-----+---------+-------+
        | user         | bigint(20)  | NO   | PRI | NULL    |       |
        | circle       | smallint(4) | NO   | PRI | 0       |       |
        | name         | varchar(64) | YES  |     |         |       |
        | created_time | bigint(20)  | NO   |     | NULL    |       |
        | updated_time | bigint(20)  | YES  |     | 0       |       |
        | member_count | smallint(6) | YES  |     | 0       |       |
        +--------------+-------------+------+-----+---------+-------+
     */


    @Override
    public void migrate() {
        friend(getOldQiupuDb(), getOldPlatformDb(), getNewPlatformDb());
    }

    public static void friend(String inQiupuDb, String inAccountDb, String outPlatformDb) {
        SQLExecutor.executeUpdate(getConnectionFactory(), outPlatformDb, "DELETE FROM circle");
        SQLExecutor.executeUpdate(getConnectionFactory(), outPlatformDb, "DELETE FROM friend");

        final LinkedHashSet<Long> userIds = new LinkedHashSet<Long>();
        SQLExecutor.executeStatements(
                new CfDb(getConnectionFactory(), inQiupuDb),
                new CfDb(getConnectionFactory(), inAccountDb),
                new CfDb(getConnectionFactory(), outPlatformDb),
                new SQLStatementsHandler() {
                    @Override
                    public void handle(Statement[] stmts) throws SQLException {
                        final Statement inQiupuStmt = stmts[0];
                        final Statement inAccountStmt = stmts[1];
                        final Statement outPlatformStmt = stmts[2];

                        SQLExecutor.executeRecordHandler(inQiupuStmt, "SELECT * FROM friendships", new RecordHandler() {
                            @Override
                            public void handle(Record in) {
                                long userId = in.getInt("uid");
                                long friendId = in.getInt("followingid");

                                try {
                                    boolean userExists = SQLExecutor.executeScalar(inAccountStmt, makeUserExistsSql(userId)) != null;
                                    boolean friendExists = SQLExecutor.executeScalar(inAccountStmt, makeUserExistsSql(friendId)) != null;

                                    if (userExists) {
                                        SQLExecutor.executeUpdate(outPlatformStmt, makeCreateCircleSql(userId));
                                    }

                                    if (userExists && friendExists) {
                                        SQLExecutor.executeUpdate(outPlatformStmt,
                                                SQLBuilder.forInsert("friend",
                                                        Record.of("user", userId, "friend", friendId, "circle", Constants.DEFAULT_CIRCLE)));
                                        userIds.add(userId);
                                    }
                                } catch (SQLException e) {
                                    throw new SQLException2(e);
                                }
                            }
                        });
                    }
                });


        SQLExecutor.executeStatement(getConnectionFactory(), outPlatformDb, new SQLStatementHandler() {
            @Override
            public void handle(Statement stmt) throws SQLException {
                for (Long userId : userIds) {
                    long memberCount = SQLExecutor.executeIntScalar(stmt, makeGetMemberCountSql(userId), 0);
                    stmt.executeUpdate(makeUpdateMemberCountSql(userId,  memberCount));
                }
            }
        });

    }

    private static String makeUserExistsSql(long userId) {
        return String.format("SELECT id FROM users WHERE id=%s", userId);
    }

    private static String makeGetMemberCountSql(long user) {
        return String.format("SELECT COUNT(*) FROM friend WHERE user=%s AND circle=%s", user, Constants.DEFAULT_CIRCLE);
    }

    private static String makeUpdateMemberCountSql(long user, long memberCount) {
        return String.format("UPDATE circle SET member_count=%s WHERE user=%s AND circle=%s", memberCount, user, Constants.DEFAULT_CIRCLE);
    }

    private static List<String> makeCreateCircleSql(long userId) {
        final String SQL = "INSERT IGNORE INTO circle (user, circle, name, created_time, updated_time, member_count) " +
                "VALUES (%s, %s, '%s', %s, %s, 0)";

        long now = DateUtils.nowMillis();
        ArrayList<String> l = new ArrayList<String>();
        l.add(String.format(SQL, userId, Constants.ADDRESS_BOOK_CIRCLE, "Address Book", now, now));
        l.add(String.format(SQL, userId, Constants.DEFAULT_CIRCLE, "Default", now, now));
        l.add(String.format(SQL, userId, Constants.BLOCKED_CIRCLE, "Blocked", now, now));
        //System.out.println(buff);
        return l;
    }
}
