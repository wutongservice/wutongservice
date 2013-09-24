package com.borqs.server.platform.signin;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.base.util.DateUtils;

public class SimpleSignIn extends SignInBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String signinTable = "sign_in";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.signinTable = conf.getString("signin.simple.settingTable", "sign_in");
    }

    @Override
    public void destroy() {
        this.signinTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveSignIn0(Record sign_in) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, sign_in)}";

        String sql = SQLTemplate.merge(SQL,
                "table", signinTable, "alias", signinSchema.getAllAliases(),
                "sign_in", sign_in);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getSignIn0(String userId, boolean asc, int page, int count) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user_id='" + userId + "' and type=0 ORDER BY created_time ${asc} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", signinTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getUserShaking0(String userId, long dateDiff,boolean asc, int page, int count) {
        long dateDiff0 = DateUtils.nowMillis() - dateDiff;
        String SQL = "SELECT distinct(user_id) FROM ${table}"
                + " WHERE user_id<>'" + userId + "' and type=1 and created_time>=" + dateDiff0 + " ORDER BY created_time ${asc} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", signinTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        RecordSet out_recs = new RecordSet();
        for (Record rec : recs){
            String sql0 = "select user_id,longitude,latitude,geo from "+signinTable+" where" +
                    " user_id='"+rec.getString("user_id")+"' and type=1 and created_time>=" + dateDiff0 + "" +
                    "  ORDER BY created_time desc limit 1";
            Record rec0 = se.executeRecord(sql0, null);
            out_recs.add(rec0);
        }
        return out_recs;
    }

    @Override
    protected RecordSet getUserNearBy0(String userId,  int page, int count) {
        String SQL = "SELECT distinct(user_id) FROM ${table}"
                + " WHERE user_id<>'" + userId + "' ORDER BY created_time desc ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", signinTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        RecordSet out_recs = new RecordSet();
        for (Record rec : recs){
            String sql0 = "select user_id,longitude,latitude,geo from "+signinTable+" where" +
                    " user_id='"+rec.getString("user_id")+"'" +
                    "  ORDER BY created_time desc limit 1";
            Record rec0 = se.executeRecord(sql0, null);
            out_recs.add(rec0);
        }
        return out_recs;
    }

    @Override
    protected boolean deleteSignIn0(String sign_ids) {
        String sql = "delete from sign_in where  sign_id in (" + sign_ids + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
}
