package com.borqs.server.wutong.action;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.Initializable;
import org.codehaus.plexus.util.StringUtils;

public class ActionImpl implements ActionLogic, Initializable {
    private static final Logger L = Logger.getLogger(ActionImpl.class);

    private ConnectionFactory connectionFactory;
    private String db;
    private String actionConfig = "action_config";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);

        this.actionConfig = conf.getString("tag.simple.action_config", "action_config");
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public RecordSet getActionConfig(Context ctx, String scope, String name) {
        String sql0 = new SQLBuilder.Select()
                .select("name , content")
                .from(this.actionConfig)
                .where("scope=" + scope)
                .andIf(StringUtils.isNotBlank(name), "name =${v(name)}", "name", name)
                .and("destroyed_time=0")
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql0, null);
    }

    @Override
    public RecordSet getAllActionConfigs(Context ctx) {
        String sql0 = new SQLBuilder.Select()
                .select("id ,name ,scope,url,user,submit_type, content")
                .from(this.actionConfig)
                .where("destroyed_time=0")
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql0, null);
    }

    /**
     * 发送post 后将过滤的Action类型的post 加入队列中
     *
     * @param ctx
     * @param post
     */
    @Override
    public void sendActionQueue(Context ctx, Record post) {
        if (post == null)
            return;

        MQ mq = MQCollection.getMQ("platform");
        try {
            mq.send("action", post.toString(false, false));
        } catch (Exception e) {
            L.error(ctx, e);
        }
    }

}
