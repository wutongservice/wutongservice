package com.borqs.server.platform.conversation;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class SimpleConversation extends ConversationBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String conversationTable = "conversation_";
    private String qiupuUid = "";
    public SimpleConversation() {
    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("stream.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("platform.simple.db", null);
        this.conversationTable = conf.getString("conversation.simple.conversationTable", "conversation_");
        this.qiupuUid = conf.getString("qiupu.uid", "10002");
    }

    @Override
    public void destroy() {
        this.conversationTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveConversation0(Record conversation) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, conversation)}";
        String sql = SQLTemplate.merge(SQL,
                "table", conversationTable, "alias", conversationSchema.getAllAliases(),
                "conversation", conversation);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean deleteConversation0(int target_type,String target_id,int reason,long from) {
        String sql = new SQLBuilder.Delete(conversationSchema)
                .deleteFrom(conversationTable)
                .where("0 = 0")
                .and("target_type='" + target_type + "'")
                .and("target_id='" + target_id + "'")
                .andIf(reason!= -1 , "reason='" + reason + "'")
                .andIf(from > 0, "from_='" + from + "'")
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean updateConversation0(int target_type,String target_id,int reason,long from) {
        final String SQL = "UPDATE ${table} SET ${alias.created_time}=${v(created_time)} WHERE ${alias.target_type}=${v(target_type)}" +
                " AND ${alias.target_id}=${v(target_id)}" +
                " AND ${alias.reason}=${v(reason)}" +
                " AND ${alias.from_}=${v(from_)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", conversationTable},
                {"alias", conversationSchema.getAllAliases()},
                {"target_type", target_type},
                {"target_id", target_id},
                {"reason", reason},
                {"from_", from},
                {"created_time", DateUtils.nowMillis()},
        });
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean updateConversationTarget0(String old_target_id, String new_target_id) {
        String sql = "update " + conversationTable + " set target_id = '" + new_target_id + "' where target_id='" + old_target_id + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getConversation0(int target_type, String target_id, List<String> reasons,long from, int page, int count) {
        String cols = "target_type,target_id,reason,from_";
        String sql = new SQLBuilder.Select(conversationSchema)
                .select(StringUtils2.splitList(cols, ",", true))
                .from(conversationTable)
                .where("0 = 0")
                .andIf(target_type > 0, "target_type='" + target_type + "'")
                .andIf(target_id.length() > 0, "target_id='" + target_id + "'")
                .andIf(reasons.size() > 0, "reason IN (" + StringUtils.join(reasons, ",") + ")")
                .andIf(from > 0, "from_='" + from + "'")
                .and("from_<>'"+qiupuUid+"'")
                .orderBy("created_time", "DESC")
                .limitByPage(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected boolean ifExistConversation0(int target_type, String target_id, int reason, long from) {
        final String sql = "SELECT reason FROM conversation_ WHERE target_type='"+target_type+"'" +
                " AND target_id='"+target_id+"'" +
                " AND reason='"+reason+"'" +
                " AND from_='"+from+"'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.size()>0;
    }
}
