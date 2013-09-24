package com.borqs.server.wutong.stream;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.DBCPConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.StringUtils2;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class PostUserIndexDataUpdater {
    public static void main(String[] args) {
        String db = "mysql/192.168.5.22/accounts/root/111111";

        ConnectionFactory cf = new DBCPConnectionFactory();
        final SQLExecutor se = new SQLExecutor(cf, db);

        String sql = new SQLBuilder.Select()
                .select("*")
                .from("stream")
                .toString();

        final AtomicLong counter = new AtomicLong(0L);
        final ArrayList<String> sqls = new ArrayList<String>();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                String mentionsStr = rec.getString("mentions");
                String addToStr = rec.getString("addto");
                if (StringUtils.isNotBlank(mentionsStr)) {
                    long[] mentions = StringUtils2.splitIntArray(mentionsStr, ",");
                    for (long uid : mentions) {
                        String sql = new SQLBuilder.Insert()
                                .insertIgnoreInto("post_user_index")
                                .value("post_id", rec.getInt("post_id"))
                                .value("user_id", uid)
                                .value("reason", 1)
                                .toString();
                        sqls.add(sql);
                    }
                }
                if (StringUtils.isNotBlank(addToStr)) {
                    long[] addTos = StringUtils2.splitIntArray(addToStr, ",");
                    for (long uid : addTos) {
                        String sql = new SQLBuilder.Insert()
                                .insertIgnoreInto("post_user_index")
                                .value("post_id", rec.getInt("post_id"))
                                .value("user_id", uid)
                                .value("reason", 2)
                                .toString();
                        sqls.add(sql);
                    }
                }

                if (sqls.size() > 100) {
                    counter.addAndGet(sqls.size());
                    se.executeUpdate(sqls);
                    sqls.clear();
                }
            }
        });
        if (!sqls.isEmpty()) {
            counter.addAndGet(sqls.size());
            se.executeUpdate(sqls);
            sqls.clear();
        }
        System.out.println(counter.get());
    }


}
