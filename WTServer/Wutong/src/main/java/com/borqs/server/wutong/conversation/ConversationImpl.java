package com.borqs.server.wutong.conversation;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.comment.CommentLogic;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConversationImpl implements ConversationLogic, Initializable {
    Logger L = Logger.getLogger(ConversationImpl.class);
    public final Schema conversationSchema = Schema.loadClassPath(ConversationImpl.class, "conversation.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String conversationTable = "conversation_";
    private String qiupuUid = "";

    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("stream.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("platform.simple.db", null);
        this.conversationTable = conf.getString("conversation.simple.conversationTable", "conversation_");
        this.qiupuUid = conf.getString("qiupu.uid", "10002");
    }

    @Override
    public void destroy() {

    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public boolean createConversation(Context ctx, Record conversation) {
        final String METHOD = "createConversation";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, conversation);
        try {
            Record conversation0 = conversation;
            Schemas.checkRecordIncludeColumns(conversation0, "target_type", "target_id", "reason", "from_");

            long now = DateUtils.nowMillis();
            conversation0.put("created_time", now);
            conversation0.put("enabled", 1);
            Schemas.standardize(conversationSchema, conversation0);
            boolean b = false;
            L.op(ctx, "createConversation");
            if (!ifExistConversation(ctx, (int) conversation0.getInt("target_type"), conversation0.getString("target_id"), (int) conversation0.getInt("reason"), conversation0.getInt("from_"))) {
                b = saveConversation(ctx, conversation0);
            } else {
                b = updateConversation(ctx, (int) conversation0.getInt("target_type"), conversation0.getString("target_id"), (int) conversation0.getInt("reason"), conversation0.getInt("from_"));
            }
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return b;
        } catch (Throwable t) {
            ServerException e = new ServerException(WutongErrors.SYSTEM_DB_ERROR);
            L.error(ctx, e);
            throw e;
        }
    }

    public boolean saveConversation(Context ctx, Record conversation) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, conversation)}";
        String sql = SQLTemplate.merge(SQL,
                "table", conversationTable, "alias", conversationSchema.getAllAliases(),
                "conversation", conversation);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    public boolean deleteConversation(Context ctx, int target_type, String target_id, int reason, long from) {
        final String METHOD = "deleteConversation";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, target_type, target_id, reason, from);
        String sql = new SQLBuilder.Delete(conversationSchema)
                .deleteFrom(conversationTable)
                .where("0 = 0")
                .and("target_type='" + target_type + "'")
                .and("target_id='" + target_id + "'")
                .andIf(reason != -1, "reason='" + reason + "'")
                .andIf(from > 0, "from_='" + from + "'")
                .toString();

        SQLExecutor se = getSqlExecutor();
        L.op(ctx, "deleteConversation");
        long n = se.executeUpdate(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean updateConversation(Context ctx, int target_type, String target_id, int reason, long from) {
        final String METHOD = "updateConversation";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, target_type, target_id, reason, from);
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
        L.op(ctx, "updateConversation");
        long n = se.executeUpdate(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public boolean updateConversationTarget(Context ctx, String old_target_id, String new_target_id) {
        final String METHOD = "updateConversationTarget";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, old_target_id, new_target_id);
        String sql = "update " + conversationTable + " set target_id = '" + new_target_id + "' where target_id='" + old_target_id + "'";
        SQLExecutor se = getSqlExecutor();
        L.op(ctx, "updateConversationTarget");
        long n = se.executeUpdate(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public RecordSet getConversation(Context ctx, int target_type, String target_id, List<String> reasons, long from, int page, int count) {
        final String METHOD = "getConversation";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, target_type, target_id, reasons, from, page, count);
        String cols = "target_type,target_id,reason,from_";
        String sql = new SQLBuilder.Select(conversationSchema)
                .select(StringUtils2.splitList(cols, ",", true))
                .from(conversationTable)
                .where("0 = 0")
                .andIf(target_type > 0, "target_type='" + target_type + "'")
                .andIf(target_id.length() > 0, "target_id='" + target_id + "'")
                .andIf(reasons.size() > 0, "reason IN (" + StringUtils.join(reasons, ",") + ")")
                .andIf(from > 0, "from_='" + from + "'")
                .and("from_<>'" + qiupuUid + "'")
                .and("enabled=1")
                .orderBy("created_time", "DESC")
                .limitByPage(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public boolean ifExistConversation(Context ctx, int target_type, String target_id, int reason, long from) {
        final String METHOD = "ifExistConversation";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, target_type, target_id, reason, from);
        final String sql = "SELECT reason FROM conversation_ WHERE target_type='" + target_type + "'" +
                " AND target_id='" + target_id + "'" +
                " AND reason='" + reason + "'" +
                " AND from_='" + from + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs.size() > 0;
    }

    @Override
    public boolean createConversationP(Context ctx, int target_type, String target_id, int reason, String fromUsers) {
        final String METHOD = "createConversationP";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, target_type, target_id, reason, fromUsers);
        ConversationLogic c = GlobalLogics.getConversation();
        List<String> l = StringUtils2.splitList(fromUsers, ",", true);
        boolean b = true;
        L.op(ctx, "createConversationP");
        for (String l0 : l) {
            if (!l0.equals("0") && l0.length() < 12) {
                Record r0 = new Record();
                r0.put("target_type", target_type);
                r0.put("target_id", target_id);
                r0.put("reason", reason);
                r0.put("from_", l0);
                c.createConversation(ctx, r0);
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;

    }

    @Override
    public boolean deleteConversationP(Context ctx, int target_type, String target_id, int reason, long from) {
        final String METHOD = "createConversationP";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, target_type, target_id, reason, from);
        ConversationLogic c = GlobalLogics.getConversation();
        CommentLogic comment = GlobalLogics.getComment();
        int flag = 0;
        if ((target_type == Constants.POST_OBJECT && reason == Constants.C_STREAM_COMMENT) ||
                (target_type == Constants.APK_OBJECT && reason == Constants.C_APK_COMMENT) ||
                (target_type == Constants.PHOTO_OBJECT && reason == Constants.C_PHOTO_COMMENT)) {
            if (comment.getIHasCommentedP(ctx, String.valueOf(from), target_type, target_id))
                flag = 1;
        }

        L.op(ctx, "deleteConversationP");
        if (flag == 0)
            c.deleteConversation(ctx, target_type, target_id, reason, from);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;

    }

    @Override
    public boolean enableConversion(Context ctx, int targetType, String targetId, int enabled) {
        final String METHOD = "enableConversion";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, targetType, targetId, enabled);

        String sql = "update " + conversationTable + " set enabled=" + enabled + " where target_type=" + targetType
                + " and target_id='" + targetId + "' and from_=" + ctx.getViewerId();
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public int getEnabled(Context ctx, int targetType, String targetId) {
        final String METHOD = "getEnabled";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, targetType, targetId);

        String sql = "select enabled from " + conversationTable + " where target_type=" + targetType
                + " and target_id='" + targetId + "' and from_=" + ctx.getViewerId();
        SQLExecutor se = getSqlExecutor();
        int enabled = (int)se.executeIntScalar(sql, -1L);
        return enabled;
    }

    @Override
    public Map<String, Integer> getEnabledByTargetIds(Context ctx, String targetIds) {
        final String METHOD = "getEnabledByTargetIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, targetIds);

        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        if (StringUtils.isBlank(targetIds))
            return map;

        String[] targets = StringUtils2.splitArray(targetIds, ",", true);
        String sql = "select target_id,enabled from " + conversationTable + " where target_id IN ("
                + SQLUtils.valueJoin(",", targets) + ") and from_=" + ctx.getViewerId();
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        for (String target : targets) {
            map.put(target, -1);
        }

        for (Record rec : recs) {
            String targetId = rec.getString("target_id");
            int enabled = (int)rec.getInt("enabled");
            map.put(targetId, enabled);
        }

        return map;
    }
}
