package com.borqs.server.platform.ignore;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;

public class SimpleIgnore extends IgnoreBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String ignoreTable = "ignore_";

    public SimpleIgnore() {
    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("stream.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("stream.simple.db", null);
        this.ignoreTable = conf.getString("ignore.simple.ignoreTable", "ignore_");
    }

    @Override
    public void destroy() {
        this.ignoreTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveIgnore0(Record ignore) {
        if (getExistsIgnore0(ignore.getString("user"),ignore.getString("target_type"),ignore.getString("target_id")))
            return false;
        final String SQL = "INSERT INTO ${table} ${values_join(alias, ignore, add)}";
        String sql = SQLTemplate.merge(SQL,
                "table", ignoreTable, "alias", ignoreSchema.getAllAliases(),
                "ignore", ignore, "add", Record.of("created_time", DateUtils.nowMillis()));
        SQLExecutor se = getSqlExecutor();
        try {
            long n = se.executeUpdate(sql);
            return n > 0;
        } catch (SQLException2 e) {
            return false;
        }
    }

    @Override
    protected boolean deleteIgnore0(String userId, String targetType,String targetId) {
        String sql="DELETE FROM ignore_ WHERE user='"+userId+"'" +
                " AND target_type='"+targetType+"' and target_id='"+targetId+"'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean getExistsIgnore0(String userId, String targetType,String targetId) {
        String sql="select count(user) as count1 FROM ignore_ WHERE user='"+userId+"'" +
                " AND target_type='"+targetType+"' and target_id='"+targetId+"'";
        SQLExecutor se = getSqlExecutor();
        return  ((Number) se.executeScalar(sql)).intValue()>0;
    }

    @Override
    protected RecordSet getIgnoreList0(String user_id,String target_type,int page,int count) {
        String sql="select * FROM ignore_ WHERE user='"+user_id+"'";
        if (!target_type.equals("") && !target_type.equals("0"))
               sql+= " AND target_type='"+target_type+"'";
        sql+=" order by created_time desc ";
        if (count>0)
            sql += " " + SQLUtils.pageToLimit(page, count) + "";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql,null);
        return recs;
    }

}
