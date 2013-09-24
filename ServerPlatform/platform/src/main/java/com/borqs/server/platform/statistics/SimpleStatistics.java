package com.borqs.server.platform.statistics;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class SimpleStatistics extends StatisticsBase {
    private static final Logger L = LoggerFactory.getLogger(SimpleStatistics.class);
    private ConnectionFactory connectionFactory;
    private String db;

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("setting.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("statistics.simple.db", "mysql/127.0.0.1/statistics/root/111111");
    }

    @Override
    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean save0(Record statistics) {
//        L.debug("In SimpleStatistics save0");
        long n = 0L;
        long n2 = 0L;
        if (statistics.size() > 0) {
            long time = DateUtils.getTimesmorning();
//            L.debug("Today timestamp: " + time);

            String sql = "INSERT INTO httpcall(api, total_count, today_count) VALUES ";
            String sql2 = "INSERT INTO httpcall_detail(api, count, time) VALUES ";

//            L.debug("Statistics record size: " + statistics.size());
//            L.debug("111");
            SQLExecutor se = getSqlExecutor();
            Iterator iter = statistics.entrySet().iterator();
//            L.debug("222");
            while (iter.hasNext()) {
//                L.debug("In while loop");
                Map.Entry entry = (Map.Entry) iter.next();
                String api = (String) entry.getKey();
                long increment = ((Long) entry.getValue()).longValue();

//                L.debug("api: " + api);
//                L.debug("increment: " + increment);
                String sql0 = "SELECT count FROM httpcall_detail WHERE time=" + time + " AND api='" + api + "'";
                long todayCount = se.executeIntScalar(sql0, 0L) + increment;

                sql += "('" + api + "', " + increment + ", " + todayCount + "), ";
                sql2 += "('" + api + "', " + increment + ", " + time + "), ";

//                L.debug("sql in cycle: " + sql);
//                L.debug("sql2 in cycle: " + sql);
            }

            sql = StringUtils.substringBeforeLast(sql, ",");
            sql += " ON DUPLICATE KEY UPDATE total_count=total_count+VALUES(total_count), today_count=VALUES(today_count)";

            sql2 = StringUtils.substringBeforeLast(sql2, ",");
            sql2 += " ON DUPLICATE KEY UPDATE count=count+VALUES(count)";

//            L.debug("statistics sql: " + sql);
//            L.debug("statistics sql2: " + sql2);

            n = se.executeUpdate(sql);
            n2 = se.executeUpdate(sql2);
        }
        return (n > 0) && (n2 > 0);
    }
}
