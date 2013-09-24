package com.borqs.server.wutong.messagecenter;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.Initializable;

public class MessageUserDelayVisitTimeImpl implements MessageUserDelayVisitTimeLogic, Initializable {
    private static final Logger L = Logger.getLogger(MessageUserDelayVisitTimeImpl.class);
    public final Schema messageUserDelaySchema = Schema.loadClassPath(MessageUserDelayVisitTimeImpl.class, "messageuserdelayvisittime.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String messageUserDelayTable;

    private Configuration conf;

    public void init() {
        conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.messageUserDelayTable = "message_user_delaytime";
    }

    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public boolean createUpdateMessageUserDelayVisitTime(Context ctx, Record message_center) {
        final String METHOD = "createUpdateMessageUserDelayVisitTime";
        L.traceStartCall(ctx, METHOD, message_center);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, message_center)}";

        String sql = SQLTemplate.merge(SQL,
                "table", messageUserDelayTable, "alias", messageUserDelaySchema.getAllAliases(),
                "message_center", message_center);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public Record getMessageByUserId(Context ctx, String userId, String type) {
        final String METHOD = "getMessageByUserId";
        L.traceStartCall(ctx, METHOD, userId);
        String sql = "";

        final String SQL = "SELECT * FROM ${table} WHERE ${alias.type}=${v(type)} and ${alias.user_id}=${v(user_id)}";

        sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", messageUserDelayTable},
                {"alias", messageUserDelaySchema.getAllAliases()},
                {"user_id", userId},
                {"type", type},
        });


        SQLExecutor se = getSqlExecutor();
        Record recs = se.executeRecord(sql, null);
        Schemas.standardize(messageUserDelaySchema, recs);

        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public boolean updateMessageUserDelayTimeById(Context ctx, Record record) {
        final String METHOD = "updateMessageUserDelayTimeById";
        L.traceStartCall(ctx, METHOD, record);
        String sql = "update " + messageUserDelayTable + " set delay_time=" + record.getString("delay_time") + " where user_id=" + record.getString("user_id") + " and type =" + record.getString("type");
        SQLExecutor se = getSqlExecutor();
        try {
            se.executeUpdate(sql);
        } catch (Exception e) {
            L.info(ctx, "更新延迟时间数据失败");
        }
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

}
