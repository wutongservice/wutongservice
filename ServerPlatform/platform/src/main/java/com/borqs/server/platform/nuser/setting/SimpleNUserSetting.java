package com.borqs.server.platform.nuser.setting;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.*;

import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SimpleNUserSetting extends NUserSettingBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String nUserSettingTable = "nuser_setting";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("nuser.setting.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("nuser.setting.simple.db", null);
        this.nUserSettingTable = conf.getString("nuser.setting.simple.nUserSettingTable", "nuser_setting");
    }

    @Override
    public void destroy() {
        this.nUserSettingTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean set0(String userId, Record values) {
        String sql = "INSERT INTO " + nUserSettingTable + " (user, setting_key, setting_value) VALUES ";

        Iterator iter = values.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            sql += "('" + userId + "', '" + key + "', '" + value + "'), ";
        }

        sql = StringUtils.substringBeforeLast(sql, ",");
        sql += " ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)";

        String sql2 = "DELETE FROM " + nUserSettingTable + " WHERE setting_value=''";

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        se.executeUpdate(sql2);

        return n > 0;
    }

    @Override
    protected Record gets0(String userId, List<String> keys) throws ResponseError, AvroRemoteException {
        Record result = new Record();
        for(String key : keys)
        {
            result.put(key, toStr(getDefault(userId, key)));
        }

        final String SQL = "SELECT ${alias.setting_key},${alias.setting_value} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.setting_key} IN (${vjoin(l)})";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", nUserSettingTable},
                {"alias", settingSchema.getAllAliases()},
                {"user", userId},
                {"l", keys}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(settingSchema, recs);

        for(Record rec : recs)
        {
            String key = rec.getString("setting_key");
            String value = rec.getString("setting_value");
            result.put(key, value);
        }

        return result;
    }

    @Override
    protected Record gets0(String userId, String startsWith) throws ResponseError, AvroRemoteException {
        Record result = new Record();
        final String SQL = "SELECT ${alias.setting_key},${alias.setting_value} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.setting_key} LIKE '${starts}%'";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", nUserSettingTable},
                {"alias", settingSchema.getAllAliases()},
                {"user", userId},
                {"starts", startsWith}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(settingSchema, recs);

        for(Record rec : recs)
        {
            String key = rec.getString("setting_key");
            String value = rec.getString("setting_value");
            result.put(key, value);
        }

        return result;
    }

    @Override
    protected Record get(String key, List<String> users) throws ResponseError,
            AvroRemoteException {
        Record result = new Record();
        for(String user : users)
        {
            result.put(user, toStr(getDefault(user, key)));
        }

        final String SQL = "SELECT ${alias.user},${alias.setting_value} FROM ${table} WHERE ${alias.setting_key}=${v(setting_key)} AND ${alias.user} IN (${vjoin(l)})";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", nUserSettingTable},
                {"alias", settingSchema.getAllAliases()},
                {"setting_key", key},
                {"l", users}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(settingSchema, recs);

        for(Record rec : recs)
        {
            String user = rec.getString("user");
            String value = rec.getString("setting_value");
            result.put(user, value);
        }

        return result;
    }
}
