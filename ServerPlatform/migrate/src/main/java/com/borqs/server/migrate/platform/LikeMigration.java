package com.borqs.server.migrate.platform;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.migrate.Migrate;
import com.borqs.server.base.migrate.MigrateStopException;
import com.borqs.server.base.migrate.handler.CounterMigrateHandler;
import com.borqs.server.base.migrate.input.SQLInput;
import com.borqs.server.base.migrate.output.PrintOutput;
import com.borqs.server.base.migrate.output.SQLInsertIgnoreOutput;
import com.borqs.server.base.migrate.output.SQLInsertOutput;
import com.borqs.server.base.sql.SQLException2;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.migrate.Migration;
import com.borqs.server.service.platform.Constants;

import java.sql.SQLException;

public class LikeMigration extends Migration {
    // old: likes
    /*
        +--------------+--------------+------+-----+---------+----------------+
        | Field        | Type         | Null | Key | Default | Extra          |
        +--------------+--------------+------+-----+---------+----------------+
        | id           | bigint(20)   | NO   | PRI | NULL    | auto_increment |
        | uid          | bigint(20)   | NO   | MUL | NULL    |                |
        | post_id      | varchar(255) | NO   |     | NULL    |                |
        | created_time | bigint(20)   | YES  |     | NULL    |                |
        +--------------+--------------+------+-----+---------+----------------+

     */

    // old: likes
    /*
        +--------------+------------+------+-----+---------+----------------+
        | Field        | Type       | Null | Key | Default | Extra          |
        +--------------+------------+------+-----+---------+----------------+
        | id           | bigint(20) | NO   | PRI | NULL    | auto_increment |
        | uid          | bigint(20) | NO   | MUL | NULL    |                |
        | apk_id       | bigint(20) | NO   |     | NULL    |                |
        | created_time | bigint(20) | YES  |     | NULL    |                |
        +--------------+------------+------+-----+---------+----------------+
     */

    // new: like
    /*
        +--------------+-------------+------+-----+---------+-------+
        | Field        | Type        | Null | Key | Default | Extra |
        +--------------+-------------+------+-----+---------+-------+
        | target       | varchar(32) | NO   | PRI | NULL    |       |
        | liker        | bigint(20)  | NO   | PRI | NULL    |       |
        | created_time | bigint(20)  | NO   |     | NULL    |       |
        +--------------+-------------+------+-----+---------+-------+

     */

    @Override
    public void migrate() {
        like(getOldPlatformDb(), getOldQiupuDb(), getNewPlatformDb());
    }

    public static void like(final String inAccountDb, final String inQiupuDb, final String outPlatformDb) {
        SQLExecutor.executeUpdate(getConnectionFactory(), outPlatformDb, "DELETE FROM like_");
        Migrate.migrate(
                new SQLInput(getConnectionFactory(), inAccountDb, "SELECT * FROM likes"),
                new SQLInsertOutput(getConnectionFactory(), outPlatformDb, "like_"),
                new CounterMigrateHandler() {
                    @Override
                    protected void handle0(Record in, Record[] out) throws MigrateStopException {
                        Record out1 = out[0];

                        try {
                            String oldPostId = in.checkGetString("post_id");
                            long ts = SQLExecutor.executeIntScalar(getConnectionFactory(), inAccountDb, String.format("SELECT created_time FROM streams WHERE id='%s'", oldPostId), 0);
                            long newPostId = SQLExecutor.executeIntScalar(getConnectionFactory(), outPlatformDb, String.format("SELECT post_id FROM stream WHERE created_time=%s", ts), 0);
                            if (newPostId != 0) {
                                out1.put("target", Constants.postObjectId(newPostId));
                                out1.put("liker", in.checkGetInt("uid"));
                                out1.put("created_time", in.checkGetInt("created_time"));
                            } else {
                                out1.clear();
                            }
                        } catch (SQLException e) {
                            throw new SQLException2(e);
                        }
                    }
                }
        );

        Migrate.migrate(
                new SQLInput(getConnectionFactory(), inQiupuDb, "SELECT * FROM likes"),
                new SQLInsertIgnoreOutput(getConnectionFactory(), outPlatformDb, "like_"),
                new CounterMigrateHandler() {
                    @Override
                    protected void handle0(Record in, Record[] out) throws MigrateStopException {
                        Record out1 = out[0];
                        out1.put("target", Constants.apkObjectId(in.checkGetInt("apk_id")));
                        out1.put("liker", in.checkGetInt("uid"));
                        out1.put("created_time", in.checkGetInt("created_time"));
                    }
                }
        );
    }
}
