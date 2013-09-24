package com.borqs.server.platform.staticfile;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;

public class SimpleStaticFile extends StaticFileBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String staticFileTable = "static_file";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.staticFileTable = conf.getString("staticFile.simple.staticFileTable", "static_file");
    }

    @Override
    public void destroy() {
        this.staticFileTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveStaticFile0(Record staticFile) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, staticFile)}";

        String sql = SQLTemplate.merge(SQL,
                "table", staticFileTable, "alias", staticFileSchema.getAllAliases(),
                "staticFile", staticFile);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getStaticFile0(String userId, boolean asc, int page, int count) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user_id='" + userId + "' and destroyed_time=0 ORDER BY created_time ${asc} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", staticFileTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected Record getStaticFileById0(String file_id) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE file_id='" + file_id + "'";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", staticFileTable},
        });

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }

    @Override
    protected boolean deleteStaticFile0(String file_ids) {
        String sql = "delete from "+staticFileTable+" where file_id in (" + file_ids + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
}
