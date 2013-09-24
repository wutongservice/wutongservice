package com.borqs.server.wutong.appsettings;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.wutong.WutongErrors;

public class AppSettingImpl implements AppSettingLogic, Initializable {
    private static final Logger L = Logger.getLogger(AppSettingImpl.class);
    protected final Schema appSettingSchema = Schema.loadClassPath(AppSettingImpl.class, "appSetting.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String app_setting = "app_setting";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public Record setSetting(Context ctx, Record setting) {

        RecordSet rs = getSettings(ctx,setting.getString("key_"),(int)setting.getInt("version"));
        if(rs.size()>0)
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR,"duplicate version");

        String sql0 = new SQLBuilder.Insert()
                .insertInto(app_setting)
                .values(setting).toString();

        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql0);
        return setting;
    }

    @Override
    public RecordSet getSettings(Context ctx, String key, int version) {
        String sql0 = new SQLBuilder.Select().select(" key_,version,value_")
                .from(this.app_setting)
                .where("key_=${v(key)}","key",key)
                .andIf(version!=0,"version=" + version).toString();
        if(version == 0)
            sql0 += " order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql0, null);
        Schemas.standardize(appSettingSchema, rs);
        return rs;
    }


}
