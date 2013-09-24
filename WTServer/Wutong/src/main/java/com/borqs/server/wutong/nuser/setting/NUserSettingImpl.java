package com.borqs.server.wutong.nuser.setting;


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
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NUserSettingImpl implements NUserSettingLogic, Initializable {
    Logger L = Logger.getLogger(NUserSettingImpl.class);
    public final Schema settingSchema = Schema.loadClassPath(NUserSettingImpl.class, "nuser_setting.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String settingTable = "nuser_setting";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("setting.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("setting.simple.db", null);
        this.settingTable = conf.getString("setting.simple.nUserSettingTable", "nuser_setting");
    }

    @Override
    public void destroy() {

    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    public boolean set(Context ctx, String userId, Record values) {
        final String METHOD = "set";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, values);
        String sql = "INSERT INTO " + settingTable + " (user, setting_key, setting_value) VALUES ";

        Iterator iter = values.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            sql += "(" + userId + ", '" + key + "', '" + value + "'), ";
        }

        sql = StringUtils.substringBeforeLast(sql, ",");
        sql += " ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)";

        String sql2 = "DELETE FROM " + settingTable + " WHERE setting_value=''";

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        se.executeUpdate(sql2);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }


    public Record gets(Context ctx, String userId, String key0) {
        final String METHOD = "gets";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, key0);
        List<String> keys = StringUtils2.splitList(key0, ",", true);
        Record result = new Record();
        for (String key : keys) {
            result.put(key, (getDefault(ctx, userId, key)));
        }

        final String SQL = "SELECT ${alias.setting_key},${alias.setting_value} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.setting_key} IN (${vjoin(l)})";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", settingTable},
                {"alias", settingSchema.getAllAliases()},
                {"user", userId},
                {"l", keys}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(settingSchema, recs);

        for (Record rec : recs) {
            String key = rec.getString("setting_key");
            String value = rec.getString("setting_value");
            result.put(key, value);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return result;
    }

    @Override
    public Record getsByStartsWith(Context ctx, String userId, String startsWith) {
        final String METHOD = "getsByStartsWith";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, startsWith);
        Record result = new Record();
        final String SQL = "SELECT ${alias.setting_key},${alias.setting_value} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.setting_key} LIKE '${starts}%'";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", settingTable},
                {"alias", settingSchema.getAllAliases()},
                {"user", userId},
                {"starts", startsWith}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(settingSchema, recs);

        for (Record rec : recs) {
            String key = rec.getString("setting_key");
            String value = rec.getString("setting_value");
            result.put(key, value);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return result;
    }


    public Record getByUsers(Context ctx, String key, String user0) {
        final String METHOD = "getByUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, key, user0);
        List<String> users = StringUtils2.splitList(user0, ",", true);

        Record result = new Record();
        for (String user : users) {
            result.put(user, getDefault(ctx, user, key));
        }

        final String SQL = "SELECT ${alias.user},${alias.setting_value} FROM ${table} WHERE ${alias.setting_key}=${v(setting_key)} AND ${alias.user} IN (${vjoin(l)})";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", settingTable},
                {"alias", settingSchema.getAllAliases()},
                {"setting_key", key},
                {"l", users}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(settingSchema, recs);

        for (Record rec : recs) {
            String user = rec.getString("user");
            String value = rec.getString("setting_value");
            result.put(user, value);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return result;
    }

    @Override
    public String getDefault(Context ctx, String userId, String key) {
        String sKey = (key);
        if (org.codehaus.plexus.util.StringUtils.equals(sKey, Constants.SOCIALCONTACT_AUTO_ADD)) {
            return "1";
        }
        //    	else if(StringUtils.equals(sKey, Constants.NTF_PROFILE_UPDATE))
        //    	{
        //    		return "1";
        //    	}
        else {
            return "0";
        }
    }
}
