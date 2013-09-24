package com.borqs.server.sharelink;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;

public class SimpleShareLink {

    Configuration conf = Configuration.loadFiles(Constants.confPath).expandMacros();
    private ConnectionFactory connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("comment.simple.connectionFactory", "dbcp"));
    private String db = conf.getString("account.simple.db", null);

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    protected boolean saveLink(String host,String tag_div,String tag_p,String tag_table,String img_id,int permission) {
        final String sql = "INSERT INTO link(host,tag_div,tag_p,tag_table,img_id,permission)" +
                " values ('"+host+"','"+tag_div+"','"+tag_p+"','"+tag_table+"','"+img_id+"','"+permission+"')";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    protected boolean updateLink(String host,String tag_div,String tag_p,String tag_table,String img_id,int permission) {
        final String sql = "update link set tag_div='"+tag_div+"',tag_p='"+tag_p+"',tag_table='"+tag_table+"',img_id='"+img_id+"',permission='"+permission+"' " +
                " where host='"+host+"'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    protected boolean deleteLink(String host) {
        final String sql = "delete from link where host='"+host+"'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    protected Record getLink(String host) {
        final String sql = "select * from link where host='"+host+"'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }
}
