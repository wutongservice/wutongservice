package com.borqs.server.wutong.messagecenter;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.Initializable;

public class MessageConfig implements Initializable {
    private ConnectionFactory connectionFactory;
    private String db;
    private String messageConfigTable;
    private Configuration conf;

    private static RecordSet rs;

    public void init() {
        conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.messageConfigTable = "message_config";
    }

    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    private RecordSet getSystemConfig() {
        String sql = "SELECT * FROM " + messageConfigTable + " where destroyed_time = 0";
        sql += " order by created_time ";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    public static Record getMessageRecord(String key) {
        if (rs == null) {
            initMessageConfig();
        }
        Record record = new Record();
        record = getMessageConfigItem(key, record);

        if(record.size()==0){
            initMessageConfig();
            getMessageConfigItem(key,record);
        }
        return record;
    }

    private static Record getMessageConfigItem(String key, Record record) {
        for (Record r : rs) {
            if (key.equals(r.getString("message_key"))) {
                record = r;
            }
        }
        return record;
    }

    private static void initMessageConfig() {
        MessageConfig messageConfig = new MessageConfig();
        messageConfig.init();
        rs = messageConfig.getSystemConfig();
        messageConfig.destroy();
    }

    public static boolean getMessageConfigKeyBoolean(String key, String Column) {
        Record record = getMessageRecord(key);
        long isDone = 1;
        if (record != null) {
            isDone = record.getInt(Column, 1);
        }
        return isDone == 1;
    }

    public static String getMessageConfigKeyString(String key, String Column) {
        Record record = getMessageRecord(key);
        String isDone = "";
        if (record != null) {
            isDone = record.getString(Column, "");
        }
        return isDone;
    }

}
