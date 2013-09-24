package com.borqs.server.wutong.category;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.tag.TagLogic;

import java.util.ArrayList;
import java.util.List;

public class CategoryImpl implements CategoryLogic, Initializable {
    private static final Logger L = Logger.getLogger(CategoryImpl.class);
    public final Schema categorySchema = Schema.loadClassPath(CategoryImpl.class, "category.schema");
    public final Schema categoryTypeSchema = Schema.loadClassPath(CategoryImpl.class, "categorytype.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String categoryType = "category_type";
    private String category = "category";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.category = conf.getString("tag.simple.category", "category");
        this.categoryType = conf.getString("tag.simple.category_type", "category_type");
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public RecordSet createCategoryType(Context ctx, RecordSet categories) {
        final String METHOD = "createCategoryType";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, categories);

        RecordSet rs = new RecordSet();

        List<String> sqls = new ArrayList<String>();
        SQLExecutor se = getSqlExecutor();

        for (Record r : categories) {
            r.set("category_id", Long.toString(RandomUtils.generateId()));
            final String SQL = "INSERT INTO ${table} ${values_join(alias, category_type)}";
            String sql0 = SQLTemplate.merge(SQL,
                    "table", this.categoryType, "alias", categoryTypeSchema.getAllAliases(),
                    "category_type", r);

            String sql1 = new SQLBuilder.Select()
                    .selectCount()
                    .from(categoryType)
                    .where("category=${v(category)}", "category", r.getString("category"))
                    .and("scope=${v(scope)}", "scope", r.getString("scope")).toString();

            long result = se.executeIntScalar(sql1, -1);

            if (result == 0) {
                sqls.add(sql0);
                rs.add(r);
            }
        }


        L.op(ctx, "createCategoryType");
        se.executeUpdate(sqls);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rs;
    }

    @Override
    public Record createCategory(Context ctx, String userId, String categoryId, String targetType, String targetId) {

        Record record = Record.of("user_id", ctx.getViewerIdString(), "category_id", categoryId, "target_type", targetType, "target_id", targetId, "created_time", DateUtils.nowMillis());

        final String SQL = "INSERT INTO ${table} ${values_join(alias, category_type)}";
        record.set("id", Long.toString(RandomUtils.generateId()));
        String sql0 = SQLTemplate.merge(SQL,
                "table", this.category, "alias", categorySchema.getAllAliases(),
                "category_type", record);

        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql0);

        //add tag when create post with a category
        TagLogic tag = GlobalLogics.getTag();
        Record categoryType = this.getCategoryType(ctx, categoryId);
        String category = categoryType.getString("category");
        Record t = Record.of("user", ctx.getViewerIdString(), "tag", category, "type", "0", "target_id", targetId, "created_time", DateUtils.nowMillis());
        tag.createTag(ctx, t);

        return record;
    }

    @Override
    public Record updateCategoryType(Context ctx, Record category) {
        //只能修改分类名称
        String sql1 = new SQLBuilder.Update()
                .update(categoryType)
                .value("category", category.getString("category"))
                .where("category_id=" + category.getString("category_id")).toString();

        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql1);
        return category;
    }

    @Override
    public Record updateCategory(Context ctx, Record category) {
        String sql1 = new SQLBuilder.Update()
                .update(this.category)
                .value("category_id", category.getString("category_id"))
                .where("id=" + category.getString("id")).toString();

        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql1);
        return category;
    }

    @Override
    public boolean destroyedCategories(Context ctx, Record category) {
        //删掉分类，对应所有这个分类下的记录都会被置为不可用
        String sql0 = new SQLBuilder.Update()
                .update(this.category)
                .value("destroyed_time", DateUtils.nowMillis())
                .where("category_id=" + category.getString("category_id"))
                .and("destroyed_time=0").toString();

        String sql1 = new SQLBuilder.Update()
                .update(this.categoryType)
                .value("destroyed_time", DateUtils.nowMillis())
                .where("category_id=" + category.getString("category_id"))
                .and("destroyed_time=0").toString();

        List<String> list = new ArrayList<String>();
        list.add(sql0);
        list.add(sql1);
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(list);
        return true;
    }

    @Override
    public RecordSet getCategoryTypes(Context ctx, String scope) {
        String sql0 = new SQLBuilder.Select()
                .select("category_id,category,user_id,scope")
                .from(this.categoryType)
                .where("scope=" + scope)
                .and("destroyed_time=0")
                .orderBy("sort", "asc", "created_time", "asc")
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql0, null);
    }

    @Override
    public Record getCategoryType(Context ctx, String category_id) {
        String sql0 = new SQLBuilder.Select()
                .select("category_id,category,user_id,scope")
                .from(this.categoryType)
                .where("category_id=" + category_id)
                .and("destroyed_time=0")
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecord(sql0, null);
    }

    @Override
    public RecordSet getCategories(Context ctx, String category_id, long startTime, long endTime, int page, int count) {
        String sql0 = new SQLBuilder.Select()
                .select("target_type,target_id")
                .from(this.category)
                .where("category_id=" + category_id)
                .andIf(startTime > 0, "created_time>=" + startTime)
                .andIf(endTime > 0, "created_time<=" + endTime)
                .and("destroyed_time=0")
                .limitByPage(page, count).toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql0, null);
    }
}
