package com.borqs.server.wutong.tag;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.Initializable;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagImpl implements TagLogic, Initializable {
    private static final Logger L = Logger.getLogger(TagImpl.class);
    public final Schema tagSchema = Schema.loadClassPath(TagImpl.class, "tag.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String tag0 = "tag0";
    private String tag1 = "tag1";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.tag0 = conf.getString("tag.simple.tag0", "tag0");
        this.tag1 = conf.getString("tag.simple.tag1", "tag1");
        tagSchema.loadAliases(GlobalConfig.get().getString("schema.tag.alias", null));
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public Record createTag(Context ctx, Record tag) {
        final String METHOD = "createTag";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, tag);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, tag)}";

        String sql0 = SQLTemplate.merge(SQL,
                "table", this.tag0, "alias", tagSchema.getAllAliases(),
                "tag", tag);
        String sql1 = SQLTemplate.merge(SQL,
                "table", this.tag1, "alias", tagSchema.getAllAliases(),
                "tag", tag);
        List<String> sqls = new ArrayList<String>();
        sqls.add(sql0);
        sqls.add(sql1);
        SQLExecutor se = getSqlExecutor();
        L.op(ctx, "createTag");
        se.executeUpdate(sqls);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return tag;
    }

    @Override
    public boolean hasTag(Context ctx, String user, String tag, String scope, String target_id, String type) {
        final String METHOD = "hasTag";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, tag);
        String sql = new SQLBuilder.Select()
                .selectCount()
                .from(tag0)
                .where("user=" + user)
                .and("tag=${v(tag)}","tag",tag)
                .andIf(StringUtils.isNotBlank(scope), "scope=" + scope)
                .and("target_id=" + target_id)
                .and("type=" + type).toString();


        SQLExecutor se = getSqlExecutor();
        long recs = se.executeIntScalar(sql, -1);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs > 0;
    }

    /**
     * scope can be null
     *
     * @param ctx
     * @param user
     * @param scope
     * @param page
     * @param count
     * @return
     */
    @Override
    public RecordSet findTagByUser(Context ctx, String user, String scope, int page, int count) {
        final String METHOD = "destroyedTag";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, user, page, count);

        String sql = new SQLBuilder.Select()
                .select("*")
                .from(tag1)
                .where("user=" + user)
                .andIf(StringUtils.isNotBlank(scope), "scope=" + scope)
                .and("destroyed_time=0")
                .orderBy("created_time", "asc")
                .limit(page, count).toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    /**
     * scope can be null
     * type can be null
     *
     * @param ctx
     * @param tag
     * @param scope
     * @param type
     * @param page
     * @param count
     * @return
     */
    @Override
    public RecordSet findUserByTagScopeType(Context ctx, String tag, String scope, String type, int page, int count) {
        final String METHOD = "destroyedTag";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, tag, scope, page, count);

        String sql = new SQLBuilder.Select()
                .select("*")
                .from(tag1)
                .where("tag=${v(tag)}","tag",tag)
                .andIf(StringUtils.isNotBlank(scope), "scope=" + scope)
                .andIf(StringUtils.isNotBlank(type), "type=" + type)
                .and("destroyed_time=0")
                .orderBy("created_time", "asc")
                .limit(page, count).toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    @Override
    public RecordSet findTargetsByUser(Context ctx, String user, String scope, String type, int page, int count) {
        final String METHOD = "findTargetsByUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, user, type, page, count);

        String sql = new SQLBuilder.Select()
                .select("*")
                .from(tag1)
                .where("user=" + user)
                .andIf(StringUtils.isNotBlank(scope), "scope=" + scope)
                .andIf(StringUtils.isNotBlank(type), "type=" + type)
                .and("destroyed_time=0")
                .orderBy("created_time", "asc")
                .limit(page, count).toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    @Override
    public RecordSet findUserTagByTarget(Context ctx, String scope, String target_id, String type, int page, int count) {

        final String METHOD = "findUserTagByTarget";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, target_id, type, page, count);

        String SQL = new SQLBuilder.Select()
                .select(" * ")
                .from(this.tag0)
                .where("target_id=${target_id}", "target_id", target_id)
                .andIf(StringUtils.isNotBlank(scope), "scope=" + scope)
                .and("type=" + type)
                .and(" destroyed_time=0")
                .orderBy("created_time", "asc").limit(page, count).toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(SQL, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    /**
     * 删除tag，暂时不提供update方式的修改
     *
     * @param tag
     * @return
     */
    @Override
    public boolean destroyedTag(Context ctx, Record tag) {
        final String METHOD = "destroyedTag";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, tag);
        String sql0 = new SQLBuilder.Delete()
                .deleteFrom(tag0)
                .where("user=" + tag.getString("user"))
                .and("tag=${v(tag)}","tag",tag.getString("tag") )
                .andIf(StringUtils.isNotBlank(tag.getString("scope")), "scope=" + tag.getString("scope"))
                .and("target_id=" + tag.getString("target_id"))
                .and("type=" + tag.getString("type")).toString();
        String sql1 = new SQLBuilder.Delete()
                .deleteFrom(tag1)
                .where("user=" + tag.getString("user"))
                .and("tag=${v(tag)}","tag",tag.getString("tag") )
                .andIf(StringUtils.isNotBlank(tag.getString("scope")), "scope=" + tag.getString("scope"))
                .and("target_id=" + tag.getString("target_id"))
                .and("type=" + tag.getString("type")).toString();

        List<String> sqls = new ArrayList<String>();
        sqls.add(sql0);
        sqls.add(sql1);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sqls);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public String[] getTagContentsByTarget(Context ctx, String target_id, String type) {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, String[]> getTagContentsByTargets(Context ctx, String[] targetIds, String scope, String type) {
        final String METHOD = "getTagContentsByTargets";
        L.traceStartCall(ctx, METHOD, targetIds, type);
        final HashMap<String, String[]> m = new HashMap<String, String[]>();
        // select target_id, GROUP_CONCAT(distinct(tag)) AS tags from tag0 where type=2 and target_id IN (2785074077919989157, 2843548164975822524) group by target_id ;
        String sql = new SQLBuilder.Select()
                .select("target_id", "GROUP_CONCAT(DISTINCT(tag)) AS tags")
                .from(tag0)
                .where("type=${v(type)} AND scope=${v(scope)} AND target_id IN (${vjoin(target_ids)})",
                        "type", Integer.parseInt(type), "scope", scope, "target_ids", targetIds)
                .groupBy("target_id")
                .toString();

        SQLExecutor se = getSqlExecutor();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                m.put(rec.getString("target_id"), StringUtils.split(rec.getString("tags"), ","));
            }
        });
        L.traceEndCall(ctx, METHOD);
        return m;
    }

    @Override
    public RecordSet findAllUserByTargetTag(Context ctx, String tag, String scope, String target_id, String type) {
        final String METHOD = "findAllUserByTargetTag";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, tag, target_id, type);

        String sql = new SQLBuilder.Select().select("*")
                .from(tag1)
                .where("tag=${v(tag)}","tag",tag)
                .andIf(StringUtils.isNotBlank(scope), "scope=" + scope)
                .and("target_id="+target_id)
                .and("type=" + type)
                .and("destroyed_time=0")
                .orderBy("created_time","asc")
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }
}
