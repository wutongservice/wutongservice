package com.borqs.server.wutong.conf;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.ErrorUtils;
import com.borqs.server.base.util.Initializable;

public class ConfImpl implements ConfLogic, Initializable {
    protected final Schema configrationSchema = Schema.loadClassPath(ConfImpl.class, "configration.schema");
    Logger L = Logger.getLogger(ConfImpl.class);
    private ConnectionFactory connectionFactory;
    private String db;
    private String configrationTable = "configration";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.configrationTable = conf.getString("configration.simple.configrationTable", "configration");
    }

    @Override
    public void destroy() {

    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public boolean saveConfiguration(Context ctx, Record configration) {
        final String METHOD = "saveConfiguration";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, configration);
        try {
            L.op(ctx, "saveConfiguration");

            configration.put("created_time", DateUtils.nowMillis());
            RecordSet recs = getConfiguration(ctx, configration.getString("user_id"), configration.getString("config_key"), (int) configration.getInt("version_code"));
            if (recs.size() > 0)
                deleteConfiguration(ctx, configration.getString("user_id"), configration.getString("config_key"), (int) configration.getInt("version_code"));

            final String SQL = "INSERT INTO ${table} ${values_join(alias, configration)}";
            String sql = SQLTemplate.merge(SQL,
                    "table", configrationTable, "alias", configrationSchema.getAllAliases(),
                    "configration", configration);
            SQLExecutor se = getSqlExecutor();
            long n = se.executeUpdate(sql);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return n > 0;
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }


    @Override
    public RecordSet getConfiguration(Context ctx, String userId, String key, int version_code) {
        final String METHOD = "getConfiguration";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, key, version_code);
        try {
            String SQL = "SELECT config_key,version_code,content_type,value,created_time FROM ${table}"
                    + " WHERE user_id='" + userId + "' and `config_key`='" + key + "' and version_code='" + version_code + "'";
            String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", configrationTable},
            });

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }


    @Override
    public boolean deleteConfiguration(Context ctx, String userId, String key, int version_code) {
        final String METHOD = "deleteConfiguration";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, key, version_code);
        try {
            String sql = "delete from " + configrationTable + " WHERE user_id='" + userId + "'";
            if (!key.equals(""))
                sql += "and `config_key`='" + key + "'";
            if (version_code != -1)
                sql += "and version_code='" + version_code + "'";
            SQLExecutor se = getSqlExecutor();
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            L.op(ctx, "deleteConfiguration");
            long n = se.executeUpdate(sql);
            return n > 0;
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }


    @Override
    public RecordSet getUserConfiguration(Context ctx, String userId) {
        final String METHOD = "getUserConfiguration";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);
        try {
            SQLExecutor se = getSqlExecutor();
            RecordSet out_recs = new RecordSet();
            String sql0 = "select distinct(config_key) from " + configrationTable + " where user_id='" + userId + "'";
            RecordSet recs0 = se.executeRecordSet(sql0, null);
            for (Record rec : recs0) {
                Record out_rec = new Record();
                out_rec.put("config_key", rec.getString("config_key"));
                String sql1 = "select version_code,content_type,value,created_time from " + configrationTable + " where" +
                        " user_id='" + userId + "' and config_key='" + rec.getString("config_key") + "' order by created_time desc";
                RecordSet recs1 = se.executeRecordSet(sql1, null);
                out_rec.put("versions", recs1);
                out_recs.add(out_rec);
            }
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return out_recs;
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }
}
