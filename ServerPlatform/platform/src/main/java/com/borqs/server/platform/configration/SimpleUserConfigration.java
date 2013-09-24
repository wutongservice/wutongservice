package com.borqs.server.platform.configration;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;

public class SimpleUserConfigration extends UserConfigrationBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String configrationTable = "configration";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.configrationTable = conf.getString("configration.simple.configrationTable", "configration");
    }

    @Override
    public void destroy() {
        this.configrationTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveConfigration0(Record configration) {
        RecordSet recs = getConfigration0(configration.getString("user_id"), configration.getString("config_key"), (int) configration.getInt("version_code"));
        if (recs.size() > 0)
            deleteConfigration0(configration.getString("user_id"), configration.getString("config_key"), (int) configration.getInt("version_code"));

        final String SQL = "INSERT INTO ${table} ${values_join(alias, configration)}";
        String sql = SQLTemplate.merge(SQL,
                "table", configrationTable, "alias", configrationSchema.getAllAliases(),
                "configration", configration);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getConfigration0(String userId, String key, int version_code) {
        String SQL = "SELECT config_key,version_code,content_type,value,created_time FROM ${table}"
                + " WHERE user_id='" + userId + "' and `config_key`='" + key + "' and version_code='" + version_code + "'";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", configrationTable},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected boolean deleteConfigration0(String userId, String key, int version_code) {
        String sql = "delete from " + configrationTable + " WHERE user_id='" + userId + "'";
        if (!key.equals(""))
            sql += "and `config_key`='" + key + "'";
        if (version_code != -1)
            sql += "and version_code='" + version_code + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getUserConfigration0(String userId) {
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

        return out_recs;
    }


}
