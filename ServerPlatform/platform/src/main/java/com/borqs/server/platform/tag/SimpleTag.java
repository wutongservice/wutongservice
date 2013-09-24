package com.borqs.server.platform.tag;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SimpleTag extends TagBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String tag0 = "tag0";
    private String tag1 = "tag1";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.tag0 = conf.getString("tag.simple.tag0", "tag0");
        this.tag1 = conf.getString("tag.simple.tag1", "tag1");
    }

    @Override
    public void destroy() {
        this.tag0 = null;
        this.tag1 = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected Record saveTag(Record tag) {
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
        se.executeUpdate(sqls);
        return tag;
    }

    @Override
    protected RecordSet findUserByTag0(String tag, int page, int count) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE tag='" + tag + "'" + " and destroyed_time=0 ORDER BY created_time  ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", this.tag1},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        return rec;
    }

    @Override
    protected boolean hasTag0(String user, String tag) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user='" + user + "'"
                + " and tag = '" + tag + "'";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", tag0},
        });

        SQLExecutor se = getSqlExecutor();
        long recs = se.executeIntScalar(sql, -1);
        return recs > 0;
    }

    @Override
    protected boolean hasTarget0(String user, String target_id, String type) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user='" + user + "'"
                + " and target_id = '" + target_id + "'"
                + " and type =" + type;
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", tag0},
        });

        SQLExecutor se = getSqlExecutor();
        long recs = se.executeIntScalar(sql, -1);
        return recs > 0;
    }

    @Override
    protected RecordSet findTagByUser0(String user, int page, int count) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user='" + user + "'" + " and destroyed_time=0 ORDER BY created_time  ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", this.tag1},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        return rec;
    }

    @Override
    protected RecordSet findTargetsByTag0(String tag, String type, int page, int count) {
        String type0 = "";
        if (StringUtils.isNotEmpty(type))
            type0 = " and type = " + type;
        String SQL = "SELECT * FROM ${table}"
                + " WHERE tag='" + tag + "'"
                + type0
                + " and destroyed_time=0 ORDER BY created_time  ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", this.tag1},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        return rec;
    }

    @Override
    protected RecordSet findTargetsByUser0(String user, String type, int page, int count) {
        String type0 = "";
        if (StringUtils.isNotEmpty(type))
            type0 = " and type = " + type;
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user='" + user + "'"
                + type0
                + " and destroyed_time=0 ORDER BY created_time  ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", this.tag1},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        return rec;
    }

    @Override
    protected RecordSet findUserTagByTarget0(String target_id, String type, int page, int count) {

        String SQL = "SELECT * FROM ${table}"
                + " WHERE target_id='" + target_id + "'"
                + " and type = " + type
                + " and destroyed_time=0 ORDER BY created_time  ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", this.tag0},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql, null);
        return rec;
    }

    /**
     * 删除tag，暂时不提供update方式的修改
     * @param tag
     * @return
     */
    @Override
    protected boolean destroyedTag0(Record tag) {
        String sql0 = "delete from " + tag0
                + " where user = '"  + tag.getString("user")+";"
                + " and tag = "+ tag.getString("tag")+"'"
                + " and target_id='"+tag.getString("target_id")+"'"
                + " and type='"+tag.getString("type")+"'";
        String sql1 = "delete from " + tag1
                + " where user = '"  + tag.getString("user")+";"
                + " and tag = "+ tag.getString("tag")+"'"
                + " and target_id='"+tag.getString("target_id")+"'"
                + " and type='"+tag.getString("type")+"'";

        List<String> sqls = new ArrayList<String>();
        sqls.add(sql0);
        sqls.add(sql1);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sqls);
        return n > 0;
    }
}
