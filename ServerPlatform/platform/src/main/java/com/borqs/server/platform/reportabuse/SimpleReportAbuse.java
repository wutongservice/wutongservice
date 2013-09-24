package com.borqs.server.platform.reportabuse;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;

public class SimpleReportAbuse extends ReportAbuseBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String reportAbuseTable = "report_abuse";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.reportAbuseTable = conf.getString("reportAbuse.simple.reportAbuseTable", "report_abuse");
    }

    @Override
    public void destroy() {
        this.reportAbuseTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveReportAbuse0(Record reportAbuse) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, reportAbuse)}";

        String sql = SQLTemplate.merge(SQL,
                "table", reportAbuseTable, "alias", reportAbuseSchema.getAllAliases(),
                "reportAbuse", reportAbuse);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected int getReportAbuseCount0(String post_id) {
        String sql = "select count(*) from "+reportAbuseTable+" where post_id in (" + post_id + ")";
        SQLExecutor se = getSqlExecutor();
        Number count = (Number)se.executeScalar(sql);
        return count.intValue();
    }

    @Override
    protected int iHaveReport0(String viewerId,String post_id) {
        String sql = "select count(*) from "+reportAbuseTable+" where post_id=" + post_id + " and user_id="+viewerId+"";
        SQLExecutor se = getSqlExecutor();
        Number count = (Number)se.executeScalar(sql);
        return count.intValue();
    }
}
