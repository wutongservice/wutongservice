package com.borqs.server.wutong.messagecenter;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.RandomUtils;
import org.apache.commons.lang.StringUtils;

public class MessageCenterImpl implements MessageCenterLogic, Initializable {
    private static final Logger L = Logger.getLogger(MessageCenterImpl.class);
    public final Schema messageCenterSchema = Schema.loadClassPath(MessageCenterImpl.class, "messagecenter.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String messageCenterTable;
    private String messageCenterFinishTable;

    private Configuration conf;

    public void init() {
        conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.messageCenterTable = "message_center";
        this.messageCenterFinishTable = "message_center_finish";
    }

    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    public boolean saveMessageCenter(Context ctx, Record message_center) {
        final String METHOD = "saveMessageCenter";
        L.traceStartCall(ctx, METHOD, message_center);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, message_center)}";

        String sql = SQLTemplate.merge(SQL,
                "table", messageCenterTable, "alias", messageCenterSchema.getAllAliases(),
                "message_center", message_center);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean createMessageCenter(Context ctx, MessageCenter messageCenter) {
        final String METHOD = "saveMessageCenter";
        L.traceStartCall(ctx, METHOD, messageCenter);

        Record m = new Record();
        long date = DateUtils.nowMillis();
        m.put("message_id", Long.toString(RandomUtils.generateId()));
        m.put("from_id", StringUtils.defaultIfBlank(messageCenter.getFromId(), "0"));
        m.put("from_username", StringUtils.defaultIfEmpty(messageCenter.getFromUsername(), ""));
        m.put("title", messageCenter.getTitle());
        m.put("to_", messageCenter.getTo());
        m.put("username", messageCenter.getUsername());
        m.put("target_type", StringUtils.defaultIfBlank(messageCenter.getTargetType(), "1"));
        m.put("target_id", StringUtils.defaultIfBlank(messageCenter.getTargetId(), "0"));
        m.put("target_name", StringUtils.defaultIfBlank(messageCenter.getTargetName(), ""));
        m.put("content", messageCenter.getContent());
        m.put("send_key", StringUtils.defaultIfBlank(messageCenter.getSendKey(), "default"));
        m.put("delay_type", StringUtils.defaultIfBlank(messageCenter.getDelayType(), "default"));
        m.put("created_time", date);
        m.put("updated_time", date);
        m.put("email_combine", StringUtils.defaultIfBlank(messageCenter.getEmailCombine(), "0"));
        boolean b = saveMessageCenter(ctx, m);
        L.traceEndCall(ctx, METHOD);
        return b;
    }

    public boolean destroyMessageById(Context ctx, String message_ids) {
        final String METHOD = "destroyMessageById";
        L.traceStartCall(ctx, METHOD, message_ids);
        String migrateSql = "INSERT INTO message_center_finish(message_id,title,to_,username,from_username,from_id,target_type,target_id,target_name,content,email_combine,send_key,delay_type,created_time,updated_time,destroyed_time)(SELECT message_id,title,to_,username,from_username,from_id,target_type,target_id,target_name,content,email_combine,send_key,delay_type,created_time,updated_time,NOW() FROM message_center  where message_id in (" + message_ids + "))";
        String sql = "DELETE FROM " + messageCenterTable + " where message_id in (" + message_ids + ")";
        SQLExecutor se = getSqlExecutor();
        try {
            se.executeUpdate(migrateSql);
        } catch (Exception e) {
            L.info(ctx, "迁移email延迟数据失败");
        }
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public RecordSet getMessageByKey(Context ctx, String sendKey, String to, String content) {
        final String METHOD = "getMessageByLevel";
        L.traceStartCall(ctx, METHOD, sendKey);
        String sql = "";
        if (StringUtils.isNotBlank(content)) {
            final String SQL = "SELECT * FROM ${table} WHERE ${alias.send_key}=${v(sendKey)} and ${alias.to_}=${v(to_)} and ${alias.content}=${v(content)}";

            sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", messageCenterTable},
                    {"alias", messageCenterSchema.getAllAliases()},
                    {"sendKey", sendKey},
                    {"to_", to},
                    {"content", content},
            });
        } else {
            final String SQL = "SELECT * FROM ${table} WHERE ${alias.send_key}=${v(sendKey)} and ${alias.to_}=${v(to_)}";

            sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", messageCenterTable},
                    {"alias", messageCenterSchema.getAllAliases()},
                    {"sendKey", sendKey},
                    {"to_", to},
            });
        }

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);


        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getMessageByKeyAndTarget(Context ctx, String sendKey, String to, String target_id) {
        final String METHOD = "getMessageByLevel";
        L.traceStartCall(ctx, METHOD, sendKey);
        String sql = "";

        final String SQL = "SELECT * FROM ${table} WHERE ${alias.send_key}=${v(sendKey)} and ${alias.to_}=${v(to_)} and ${alias.target_id}=${v(target_id)}";

        sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", messageCenterTable},
                {"alias", messageCenterSchema.getAllAliases()},
                {"sendKey", sendKey},
                {"to_", to},
                {"target_id", target_id},
        });

        sql += " order by created_time desc";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);


        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getMessageBySendKey(Context ctx, String sendKey) {
        final String METHOD = "getMessageByLevel";
        L.traceStartCall(ctx, METHOD, sendKey);
        String sql = "";

        final String SQL = "SELECT * FROM ${table} WHERE ${alias.send_key}=${v(sendKey)}";

        sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", messageCenterTable},
                {"alias", messageCenterSchema.getAllAliases()},
                {"sendKey", sendKey},
        });


        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(messageCenterSchema, recs);

        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getMessageDistinctListByDelayType(Context ctx, String delayType) {
        String SQL = "SELECT to_,send_key,count(message_id) as num,username,target_id FROM ${table} WHERE ${alias.delay_type}=${v(delay_type)} group by to_,send_key,target_id";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", messageCenterTable},
                {"alias", messageCenterSchema.getAllAliases()},
                {"delay_type", delayType},
        });
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(messageCenterSchema, recs);
        return recs;
    }

    @Override
    public RecordSet getMessageDistinctListByDelayTypeAndSendKey(Context ctx, String delayType, String sendKey) {
        String SQL = "SELECT to_,send_key,count(message_id) as num,username,content FROM ${table} WHERE ${alias.delay_type}=${v(delay_type)} and ${alias.send_key}=${v(sendKey)} group by to_,send_key";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", messageCenterTable},
                {"alias", messageCenterSchema.getAllAliases()},
                {"delay_type", delayType},
                {"sendKey", sendKey},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(messageCenterSchema, recs);
        return recs;
    }

    @Override
    public Record getMessageFinish(Context ctx, String sendKey, String target_id, String to) {
        String SQL = "SELECT * FROM ${table} WHERE to_=${v(to_)} and send_key=${v(sendKey)} and target_id=${v(target_id)} order by created_time desc";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", messageCenterFinishTable},
                {"to_", to},
                {"target_id", target_id},
                {"sendKey", sendKey},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.getFirstRecord();
    }

}
