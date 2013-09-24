package com.borqs.server.migrate.qiupu;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.migrate.Migration;
import com.borqs.server.service.qiupu.Qiupu;

import java.sql.SQLException;
import java.sql.Statement;

public class ApkHistoryMigration extends Migration {
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

    // new: qapk_history
    /*
       +-----------------+--------------+------+-----+---------+-------+
       | Field           | Type         | Null | Key | Default | Extra |
       +-----------------+--------------+------+-----+---------+-------+
       | qapk_history_id | bigint(20)   | NO   | PRI | NULL    |       |
       | user            | bigint(20)   | NO   | MUL | NULL    |       |
       | package         | varchar(255) | NO   | MUL | NULL    |       |
       | version_code    | int(11)      | NO   | MUL | NULL    |       |
       | architecture    | tinyint(4)   | NO   |     | NULL    |       |
       | version_name    | varchar(255) | YES  |     | NULL    |       |
       | created_time    | bigint(20)   | NO   |     | NULL    |       |
       | action          | smallint(6)  | NO   |     | NULL    |       |
       +-----------------+--------------+------+-----+---------+-------+

    */


    @Override
    public void migrate() {
        apkHistory(getOldQiupuDb(), getNewQiupuDb());
    }

    public static void apkHistory(String inDb, String outDb) {
        SQLExecutor.executeUpdate(getConnectionFactory(), outDb, "DELETE FROM qapk_history");
        SQLExecutor.executeStatements(
                new CfDb(getConnectionFactory(), inDb),
                new CfDb(getConnectionFactory(), inDb),
                new CfDb(getConnectionFactory(), outDb),
                new SQLStatementsHandler() {
                    @Override
                    public void handle(Statement[] stmts) throws SQLException {
                        final Statement inStmt1 = stmts[0];
                        final Statement inStmt2 = stmts[1];
                        final Statement outStmt = stmts[2];

                        SQLExecutor.executeRecordHandler(inStmt1, "SELECT * FROM apkdownload", new RecordHandler() {
                            @Override
                            public void handle(Record in) {
                                try {
                                    Record out = new Record();
                                    String sql = String.format("SELECT apkcomponentname, apkVersionCode, apkversionName FROM apks WHERE id=%s", in.checkGetInt("apk_id"));
                                    Record apkRec = SQLExecutor.executeRecord(inStmt2, sql, null);
                                    if (!apkRec.isEmpty()) {
                                        out.put("qapk_history_id", RandomUtils.generateId(in.checkGetInt("created_time")));
                                        out.put("user", in.checkGetInt("uid"));
                                        out.put("package", apkRec.checkGetString("apkcomponentname"));
                                        out.put("version_code", apkRec.checkGetInt("apkVersionCode"));
                                        out.put("version_name", apkRec.checkGetString("apkversionName"));
                                        out.put("architecture", Qiupu.ARCH_ARM);
                                        out.put("created_time", in.checkGetInt("created_time"));
                                        out.put("action", Qiupu.ACTION_DOWNLOAD);

                                        SQLExecutor.executeUpdate(outStmt, SQLBuilder.forInsert("qapk_history", out));
                                    }
                                } catch (SQLException e) {
                                    throw new SQLException2(e);
                                }

                            }
                        });

                        SQLExecutor.executeRecordHandler(inStmt1, "SELECT * FROM apkinstall", new RecordHandler() {
                            @Override
                            public void handle(Record in) {
                                try {
                                    Record out = new Record();
                                    String sql = String.format("SELECT apkcomponentname, apkVersionCode, apkversionName FROM apks WHERE id=%s", in.checkGetInt("apk_id"));
                                    Record apkRec = SQLExecutor.executeRecord(inStmt2, sql, null);
                                    if (!apkRec.isEmpty()) {
                                        out.put("qapk_history_id", RandomUtils.generateId(in.checkGetInt("created_time")));
                                        out.put("user", in.checkGetInt("uid"));
                                        out.put("package", apkRec.checkGetString("apkcomponentname"));
                                        out.put("version_code", apkRec.checkGetInt("apkVersionCode"));
                                        out.put("version_name", apkRec.checkGetString("apkversionName"));
                                        out.put("architecture", Qiupu.ARCH_ARM);
                                        out.put("created_time", in.checkGetInt("created_time"));
                                        out.put("action", Qiupu.ACTION_INSTALL);

                                        SQLExecutor.executeUpdate(outStmt, SQLBuilder.forInsert("qapk_history", out));
                                    }
                                } catch (SQLException e) {
                                    throw new SQLException2(e);
                                }

                            }
                        });
                    }
                });
    }
}
