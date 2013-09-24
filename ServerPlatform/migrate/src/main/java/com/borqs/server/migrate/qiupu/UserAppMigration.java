package com.borqs.server.migrate.qiupu;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.CfDb;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLStatementsHandler;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.migrate.Migration;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserAppMigration extends Migration {
    // old: apkdownload
    /*
       +--------------+------------+------+-----+---------+----------------+
       | Field        | Type       | Null | Key | Default | Extra          |
       +--------------+------------+------+-----+---------+----------------+
       | id           | bigint(20) | NO   | PRI | NULL    | auto_increment |
       | uid          | bigint(20) | NO   |     | NULL    |                |
       | apk_id       | bigint(20) | NO   |     | NULL    |                |
       | created_time | bigint(20) | YES  |     | NULL    |                |
       +--------------+------------+------+-----+---------+----------------+
    */

    // old: apkinstall
    /*
       +--------------+------------+------+-----+---------+----------------+
       | Field        | Type       | Null | Key | Default | Extra          |
       +--------------+------------+------+-----+---------+----------------+
       | id           | bigint(20) | NO   | PRI | NULL    | auto_increment |
       | uid          | bigint(20) | NO   |     | NULL    |                |
       | apk_id       | bigint(20) | NO   |     | NULL    |                |
       | created_time | bigint(20) | YES  |     | NULL    |                |
       +--------------+------------+------+-----+---------+----------------+

    */

    // old: apkkeeps
    /*
       +--------+------------+------+-----+---------+-------+
       | Field  | Type       | Null | Key | Default | Extra |
       +--------+------------+------+-----+---------+-------+
       | uid    | bigint(20) | NO   | MUL | 0       |       |
       | apk_id | bigint(20) | NO   |     | 0       |       |
       +--------+------------+------+-----+---------+-------+
    */


    // new: user_qapp
    /*
       +---------+--------------+------+-----+---------+-------+
       | Field   | Type         | Null | Key | Default | Extra |
       +---------+--------------+------+-----+---------+-------+
       | user    | bigint(20)   | NO   | PRI | NULL    |       |
       | package | varchar(255) | NO   | PRI | NULL    |       |
       | reason  | int(11)      | YES  |     | NULL    |       |
       | privacy | tinyint(4)   | YES  |     | NULL    |       |
       +---------+--------------+------+-----+---------+-------+
    */


    @Override
    public void migrate() {
        userApp(getOldQiupuDb(), getNewQiupuDb());
    }

    public static void userApp(String inDb, String outDb) {

        SQLExecutor.executeUpdate(getConnectionFactory(), outDb, "DELETE FROM user_qapp");

        final LinkedHashMap<String, Integer> userQApps = new LinkedHashMap<String, Integer>();
        RecordSet recs;

        recs = SQLExecutor.executeRecordSet(getConnectionFactory(), inDb, "SELECT * FROM apkdownload", null);
        for (Record rec : recs) {
            String key = StringUtils2.join("-", rec.checkGetInt("uid"), rec.checkGetInt("apk_id"));
            int n = userQApps.containsKey(key) ? userQApps.get(key) : 0;
            n |= Qiupu.REASON_DOWNLOADED;
            userQApps.put(key, n);
        }

        recs = SQLExecutor.executeRecordSet(getConnectionFactory(), inDb, "SELECT * FROM apkinstall", null);
        for (Record rec : recs) {
            String key = StringUtils2.join("-", rec.checkGetInt("uid"), rec.checkGetInt("apk_id"));
            int n = userQApps.containsKey(key) ? userQApps.get(key) : 0;
            n |= Qiupu.REASON_INSTALLED;
            userQApps.put(key, n);
        }

        recs = SQLExecutor.executeRecordSet(getConnectionFactory(), inDb, "SELECT * FROM apkkeeps", null);
        for (Record rec : recs) {
            String key = StringUtils2.join("-", rec.checkGetInt("uid"), rec.checkGetInt("apk_id"));
            int n = userQApps.containsKey(key) ? userQApps.get(key) : 0;
            n |= Qiupu.REASON_FAVORITE;
            userQApps.put(key, n);
        }

        SQLExecutor.executeStatements(
                new CfDb(getConnectionFactory(), inDb),
                new CfDb(getConnectionFactory(), outDb),
                new SQLStatementsHandler() {
                    @Override
                    public void handle(Statement[] stmts) throws SQLException {
                        Statement inStmt = stmts[0];
                        Statement outStmt = stmts[1];


                        LinkedHashMap<String, Record> outRecs = new LinkedHashMap<String, Record>();
                        for (Map.Entry<String, Integer> e : userQApps.entrySet()) {
                            long userId = Long.parseLong(StringUtils.substringBefore(e.getKey(), "-"));
                            int apkId = Integer.parseInt(StringUtils.substringAfter(e.getKey(), "-"));
                            int reason = e.getValue();
                            String sql = String.format("SELECT apkcomponentname FROM apks WHERE id=%s", apkId);
                            String package_ = (String) SQLExecutor.executeScalar(inStmt, sql);

                            if (package_ != null) {
                                String outKey = StringUtils2.join("-", Long.toString(userId), package_);
                                Record outRec = outRecs.containsKey(outKey)
                                        ? outRecs.get(outKey)
                                        : Record.of("user", userId, "package", package_, "reason", reason, "privacy", Qiupu.PRIVACY_PUBLIC);
                                outRec.put("reason", reason | (int) outRec.getInt("reason"));
                                outRecs.put(outKey, outRec);
                            }
                        }

                        int counter = 0;
                        for (Record outRec : outRecs.values()) {
                            String sql = SQLBuilder.forInsert("user_qapp", outRec);
                            SQLExecutor.executeUpdate(outStmt, sql);

                            counter++;
                            if (counter % 100 == 0)
                                System.out.println(counter);
                        }
                    }
                });
    }
}
