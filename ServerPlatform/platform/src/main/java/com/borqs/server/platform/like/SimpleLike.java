package com.borqs.server.platform.like;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class SimpleLike extends LikeBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String likeTable = "like_";

    public SimpleLike() {
    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("like.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("like.simple.db", null);
        this.likeTable = conf.getString("like.simple.likeTable", "like_");
    }

    @Override
    public void destroy() {
        this.likeTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveLike(Record like) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, like, add)}";

        String sql = SQLTemplate.merge(SQL,
                "table", likeTable, "alias", likeSchema.getAllAliases(),
                "like", like, "add", Record.of("created_time", DateUtils.nowMillis()));
        
        SQLExecutor se = getSqlExecutor();
        try {
            long n = se.executeUpdate(sql);
            return n > 0;
        } catch (SQLException2 e) {
            return false;
        }
    }

    @Override
    protected boolean deleteLike(String userId, String targetId) {
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL="";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "DELETE FROM ${table} WHERE ${alias.liker}=${v(user_id)} AND ${alias.target} like '%"+a2[0]+"-%'";
        } else {
            SQL = "DELETE FROM ${table} WHERE ${alias.liker}=${v(user_id)} AND ${alias.target}=${v(target)}";
        }

        String sql = SQLTemplate.merge(SQL, "table", likeTable, "alias", likeSchema.getAllAliases(),
                "user_id", userId, "target", targetId);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected int getLikeCount0(String targetId) {
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL="";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT count(*) FROM ${table} WHERE ${alias.target} like '%"+a2[0]+"-%'";
        } else {
            SQL = "SELECT count(*) FROM ${table} WHERE ${alias.target}=${v(target)}";
        }
        String sql = SQLTemplate.merge(SQL, "table", likeTable, "alias", likeSchema.getAllAliases(), "target", targetId);
        SQLExecutor se = getSqlExecutor();
        if (se.executeScalar(sql) == null || ((Number) se.executeScalar(sql)).intValue() < 0) {
            return 0;
        } else {
            return ((Number) se.executeScalar(sql)).intValue();
        }

    }

    @Override
    protected boolean userLiked(String userId, String targetId) {
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL="";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${alias.target} FROM ${table} WHERE ${alias.target} like '%"+a2[0]+"-%' AND ${alias.liker}=${v(user_id)}";
        } else {
            SQL = "SELECT ${alias.target} FROM ${table} WHERE ${alias.target}=${v(target)} AND ${alias.liker}=${v(user_id)}";
        }
    String sql = SQLTemplate.merge(SQL, "alias", likeSchema.getAllAliases(), "table", likeTable,
                "target", targetId, "user_id", userId);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return !rec.isEmpty();
    }
    
    @Override
    protected RecordSet likedUsers(String targetId, int page, int count) {
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL="";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${alias.liker} FROM ${table}"
                + " WHERE ${alias.target} like '%"+a2[0]+"%' ORDER BY created_time DESC ${limit}";
        } else {
            SQL = "SELECT ${alias.liker} FROM ${table}"
                + " WHERE ${alias.target}=${v(target)} ORDER BY created_time DESC ${limit}";
        }

        String sql = SQLTemplate.merge(SQL, new Object[][] {
                {"alias", likeSchema.getAllAliases()},
                {"table", likeTable},
                {"target", targetId},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(likeSchema, recs);
        return recs;
    } 
    
    @Override
    protected RecordSet findLikedPost(String userId, int page, int count,int objectType) {
        final String SQL = "SELECT DISTINCT(${alias.target}) FROM ${table} use index (liker)"
                + " WHERE ${alias.liker}=${liker} AND LEFT(${alias.target},1)=${objectType} AND LEFT(${alias.target},1)<>${v(objectType1)} ORDER BY ${alias.created_time} DESC ${limit}";

        String sql = SQLTemplate.merge(SQL, new Object[][] {
                {"alias", likeSchema.getAllAliases()},
                {"table", likeTable},
                {"objectType", String.valueOf(objectType)},
                {"objectType1", String.valueOf(objectType) + ":0"},
                {"liker", userId},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(likeSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet getObjectLikedByUsers0(String viewerId, String userIds, String objectType,int page, int count) {
        List<String> cols0 = StringUtils2.splitList(toStr("target,liker"), ",", true);
        final String sql = new SQLBuilder.Select(likeSchema)
                .select(cols0)
                .from("like_")
                .where("0 = 0")
                .and("left(target,2)='" + objectType + ":'")
                .and("length(target)>10")
                .andIf(!userIds.isEmpty(), "liker IN (" + userIds + ")")
                .orderBy("created_time", "DESC")
                .limitByPage(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        for (Record r : recs) {
            r.put("target", r.getString("target").replace(objectType + ":", ""));
            r.renameColumn("liker", "source");
        }
        return recs;
    }

    @Override
    protected boolean updateLikeTarget0(String old_target, String new_target) {
        String sql = "update "+ likeTable +" set target = '" + new_target + "' where target='" + old_target + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
}
