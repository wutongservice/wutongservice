package com.borqs.server.wutong.statistics;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class StatisticsImpl implements StatisticsLogic, Initializable {
    private ConnectionFactory connectionFactory;
    private String db;


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("statistics.simple.db", null);
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public RecordSet showKeyValue(Record rec)
    {
        SQLExecutor se = getSqlExecutor();
        String sql=String.format("select * from key_value limit %1$s, %2$s", rec.getInt("start", 0), rec.getInt("end", 10000000));
        return se.executeRecordSet(sql,  null);
    }

    @Override
    public RecordSet showCount() {
        SQLExecutor se = getSqlExecutor();
        String sql=String.format("select COUNT(*) from key_value");
        return se.executeRecordSet(sql,  null);
    }

    @Override
    public boolean keyValue(Record statistics)
    {
        long n = 0L;
        //try{
            if (statistics.size() > 0) {
                long time = DateUtils.getTimesmorning();
                String sql = "INSERT INTO key_value(user, `key`, `value`, created_time) VALUES ";

                sql += "('" + statistics.getString("device", "") + "', '" +  statistics.getString("key", "") + "', '" +  statistics.getString("value", "") + "', '" + statistics.getString("created_time", new Date().toString()) +"') ";

                SQLExecutor se = getSqlExecutor();
                n = se.executeUpdate(sql);
            }
        //}catch(Exception ne){ }

        return (n > 0);
    }
    @Override
    public boolean save( Record statistics) {
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
