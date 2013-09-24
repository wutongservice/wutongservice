package com.borqs.server.migrate.platform;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.migrate.Migrate;
import com.borqs.server.base.migrate.MigrateStopException;
import com.borqs.server.base.migrate.handler.CounterMigrateHandler;
import com.borqs.server.base.migrate.input.SQLInput;
import com.borqs.server.base.migrate.output.SQLInsertOutput;
import com.borqs.server.base.sql.SQLException2;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.migrate.Migration;
import com.borqs.server.service.platform.Constants;

import java.sql.SQLException;

public class CommentMigration extends Migration {
    // old: account comments
    /*
        +--------------+---------------+------+-----+---------+----------------+
        | Field        | Type          | Null | Key | Default | Extra          |
        +--------------+---------------+------+-----+---------+----------------+
        | id           | bigint(20)    | NO   | PRI | NULL    | auto_increment |
        | uid          | bigint(20)    | NO   | MUL | NULL    |                |
        | username     | varchar(25)   | NO   |     | NULL    |                |
        | post_id      | varchar(255)  | NO   |     | NULL    |                |
        | content      | varchar(2048) | YES  |     | NULL    |                |
        | created_time | bigint(20)    | YES  |     | NULL    |                |
        +--------------+---------------+------+-----+---------+----------------+

     */

    // old: qiupu comments
    /*
        +-----------------+---------------+------+-----+---------+----------------+
        | Field           | Type          | Null | Key | Default | Extra          |
        +-----------------+---------------+------+-----+---------+----------------+
        | id              | bigint(20)    | NO   | PRI | NULL    | auto_increment |
        | uid             | bigint(20)    | NO   | MUL | NULL    |                |
        | username        | varchar(25)   | NO   |     | NULL    |                |
        | comments_obj_id | bigint(20)    | NO   |     | NULL    |                |
        | content         | varchar(2048) | YES  |     | NULL    |                |
        | created_time    | bigint(20)    | YES  |     | NULL    |                |
        +-----------------+---------------+------+-----+---------+----------------+

     */

    // new: comment
    /*
        +----------------+---------------+------+-----+---------+-------+
        | Field          | Type          | Null | Key | Default | Extra |
        +----------------+---------------+------+-----+---------+-------+
        | comment_id     | bigint(20)    | NO   | PRI | NULL    |       |
        | target         | varchar(32)   | NO   | MUL | NULL    |       |
        | created_time   | bigint(20)    | NO   | MUL | NULL    |       |
        | destroyed_time | bigint(20)    | YES  |     | 0       |       |
        | commenter      | bigint(20)    | NO   |     | NULL    |       |
        | commenter_name | varchar(64)   | NO   |     | NULL    |       |
        | message        | varchar(4096) | NO   |     | NULL    |       |
        | device         | varchar(64)   | YES  |     | NULL    |       |
        | can_like       | tinyint(4)    | YES  |     | 1       |       |
        +----------------+---------------+------+-----+---------+-------+

     */

    @Override
    public void migrate() {
        comment(getOldPlatformDb(), getOldQiupuDb(), getNewPlatformDb());
    }

    public static void comment(final String inAccountDb,final String inQiupuDb, final String outPlatformDb) {
        SQLExecutor.executeUpdate(getConnectionFactory(), outPlatformDb, "DELETE FROM comment");
        Migrate.migrate(
                new SQLInput(getConnectionFactory(), inAccountDb, "SELECT * FROM comments"),
                new SQLInsertOutput(getConnectionFactory(), outPlatformDb, "comment"),
                new CounterMigrateHandler() {
                    @Override
                    protected void handle0(Record in, Record[] out) throws MigrateStopException {
                        Record out1 = out[0];
                        long outCommentId = RandomUtils.generateId(in.checkGetInt("created_time"));
                        String oldPostId = in.checkGetString("post_id");
                        try {
                            long ts = SQLExecutor.executeIntScalar(getConnectionFactory(), inAccountDb, String.format("SELECT created_time FROM streams WHERE id='%s'", oldPostId), 0);
                            long newPostId = SQLExecutor.executeIntScalar(getConnectionFactory(), outPlatformDb, String.format("SELECT post_id FROM stream WHERE created_time=%s", ts), 0);
                            if (newPostId != 0) {
                                out1.put("comment_id", outCommentId);
                                out1.put("target", Constants.postObjectId(newPostId));
                                out1.put("created_time", in.checkGetInt("created_time"));
                                out1.put("destroyed_time", 0);
                                out1.put("commenter", in.checkGetInt("uid"));
                                out1.put("commenter_name", in.checkGetString("username"));
                                out1.put("message", in.checkGetString("content"));
                                out1.put("device", "");
                                out1.put("can_like", 1);
                            } else {
                                out1.clear();
                            }
                        } catch (SQLException e) {
                            throw new SQLException2(e);
                        }
                    }
                });
        Migrate.migrate(
                new SQLInput(getConnectionFactory(), inQiupuDb, "SELECT * FROM comments"),
                new SQLInsertOutput(getConnectionFactory(), outPlatformDb, "comment"),
                new CounterMigrateHandler() {
                    @Override
                    protected void handle0(Record in, Record[] out) throws MigrateStopException {
                        Record out1 = out[0];
                        long outCommentId = RandomUtils.generateId(in.checkGetInt("created_time"));

                            long oldApkId = in.checkGetInt("comments_obj_id");
                            if (oldApkId > 0) {
                                out1.put("comment_id", outCommentId);
                                out1.put("target", Constants.apkObjectId(oldApkId));
                                out1.put("created_time", in.checkGetInt("created_time"));
                                out1.put("destroyed_time", 0);
                                out1.put("commenter", in.checkGetInt("uid"));
                                out1.put("commenter_name", in.checkGetString("username"));
                                out1.put("message", in.checkGetString("content"));
                                out1.put("device", "");
                                out1.put("can_like", 1);
                            } else {
                                out1.clear();
                            }

                    }
                });
    }
}
