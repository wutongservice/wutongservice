package com.borqs.server.platform.comment;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class SimpleComment extends CommentBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String commentTable;

    public SimpleComment() {
    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("comment.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("comment.simple.db", null);
        this.commentTable = conf.getString("comment.simple.commentTable", "comment");
    }

    @Override
    public void destroy() {
        this.commentTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveComment(Record comment) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, comment, add)}";

        String sql = SQLTemplate.merge(SQL,
                "table", commentTable, "alias", commentSchema.getAllAliases(),
                "comment", comment, "add", Record.of("destroyed_time", 0));

//        System.out.println(sql);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected Record disableComments(String userId, String commentId, String fromSource, String objectType) {
        String sql = "";
        if (Integer.valueOf(objectType) == Constants.POST_OBJECT) {
            if (userId.equals(fromSource)) {
                sql = "update comment set destroyed_time=" + DateUtils.nowMillis() + "" +
                        " where comment_id=" + commentId + " and destroyed_time = 0";
            } else {
                sql = "update comment set destroyed_time=" + DateUtils.nowMillis() + "" +
                        " where comment_id=" + commentId + " and destroyed_time = 0 and " +
                        "(commenter=" + userId + ")";
            }

        } else {
            sql = "update comment set destroyed_time=" + DateUtils.nowMillis() + "" +
                    " where comment_id=" + commentId + " and destroyed_time = 0 and " +
                    "(commenter=" + userId + ")";
        }
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return Record.of("comment_id", commentId, "result", n > 0);
    }

    @Override
    protected boolean updateCanLike0(String userId, String commentId, boolean can_like) {
        String sql = "update comment set can_like=" + can_like + "" +
                    " where comment_id=" + commentId + " and (commenter=" + userId + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected int getCommentCount0(String viewerId,String targetId) {
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL="";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT count(*) FROM ${table} WHERE ${alias.target} like '%" + a2[0] + "-%' AND ${alias.destroyed_time}=0";
        } else {
            SQL = "SELECT count(*) FROM ${table} WHERE ${alias.target}=${v(target_id)} AND ${alias.destroyed_time}=0";
        }

        if (!viewerId.equals("") && !viewerId.equals("0")) {
            SQL += " and comment_id not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_COMMENT + "' and user='" + viewerId + "') ";
            SQL += " and commenter not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_USER + "' and user='" + viewerId + "') ";
        }

        String sql = SQLTemplate.merge(SQL,
                "table", commentTable, "alias", commentSchema.getAllAliases(), "target_id", targetId);

        SQLExecutor se = getSqlExecutor();
        Number count = (Number)se.executeScalar(sql);
        return count.intValue();
    }

    @Override
    protected RecordSet findCommentsFor(String targetId, List<String> cols, boolean asc, int page, int count) {
        if (!cols.contains("comment_id"))
            cols.add("comment_id");
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL="";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                + " WHERE destroyed_time = 0 AND ${alias.target} like '%"+a2[0]+"-%' ORDER BY ${alias.comment_id} ${asc} ${limit}";
        } else {
            SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                + " WHERE destroyed_time = 0 AND ${alias.target}=${v(target)} ORDER BY ${alias.comment_id} ${asc} ${limit}";
        }

        String sql = SQLTemplate.merge(SQL, new Object[][] {
                {"alias", commentSchema.getAllAliases()},
                {"cols", cols},
                {"table", commentTable},
                {"target", targetId},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(commentSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet findCommentsForContainsIgnore(String viewerId,String targetId, List<String> cols, boolean asc, int page, int count) {
        if (!cols.contains("comment_id"))
            cols.add("comment_id");
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL="";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                + " WHERE destroyed_time = 0 AND ${alias.target} like '%"+a2[0]+"-%'";
        } else {
            SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                + " WHERE destroyed_time = 0 AND ${alias.target}=${v(target)}";
        }
        if (!viewerId.equals("") && !viewerId.equals("0")) {
            SQL += " and comment_id not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_COMMENT + "' and user='" + viewerId + "') ";
            SQL += " and commenter not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_USER + "' and user='" + viewerId + "') ";
        }
        SQL+="  ORDER BY ${alias.comment_id} ${asc} ${limit}";

        String sql = SQLTemplate.merge(SQL, new Object[][] {
                {"alias", commentSchema.getAllAliases()},
                {"cols", cols},
                {"table", commentTable},
                {"target", targetId},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(commentSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet findComments(List<String> commentId0, List<String> cols) {
        if (!cols.contains("comment_id"))
            cols.add("comment_id");
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                + " WHERE destroyed_time = 0 AND ${alias.comment_id} IN (${comment_ids})";

        String sql = SQLTemplate.merge(SQL,
                "alias", commentSchema.getAllAliases(),
                "cols", cols,
                "table", commentTable,
                "comment_ids", StringUtils.join(commentId0, ","));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(commentSchema, recs);
        return recs;
    }
    
    @Override
    protected RecordSet findCommentsAll(List<String> commentId0, List<String> cols) {
        if (!cols.contains("comment_id"))
            cols.add("comment_id");
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                + " WHERE ${alias.comment_id} IN (${comment_ids})";

        String sql = SQLTemplate.merge(SQL,
                "alias", commentSchema.getAllAliases(),
                "cols", cols,
                "table", commentTable,
                "comment_ids", StringUtils.join(commentId0, ","));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(commentSchema, recs);
        return recs;
    }
    
    @Override
    protected RecordSet findCommentedPost(String userId,  int page, int count,int objectType) {
        final String SQL = "SELECT DISTINCT(${alias.target}) FROM ${table} use index (commenter) "
                + " WHERE ${alias.commenter}=${commenter} AND LEFT(${alias.target},1)=${objectType} " +
                " AND ${alias.destroyed_time}=0" +
                " ORDER BY ${alias.comment_id} DESC ${limit}";

        String sql = SQLTemplate.merge(SQL, new Object[][] {
                {"alias", commentSchema.getAllAliases()},
                {"table", commentTable},
                {"objectType", String.valueOf(objectType)},
                {"objectType1", String.valueOf(objectType) + ":0"},
                {"commenter", userId},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(commentSchema, recs);
        return recs;
    }
    
    @Override
    protected RecordSet findWhoCommentTarget0(String target, int limit) {
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(target), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT DISTINCT(commenter) FROM ${table} WHERE target like '%" + a2[0] + "-%' and destroyed_time = 0 ORDER BY comment_id DESC LIMIT " + limit + "";
        } else {
            SQL = "SELECT DISTINCT(commenter) FROM ${table} WHERE target='" + target + "' and destroyed_time = 0 ORDER BY comment_id DESC LIMIT " + limit + "";
        }
        final String sql = SQLTemplate.merge(SQL,
                "alias", commentSchema.getAllAliases(),
                "table", commentTable);
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getObjectCommentByUsers0(String viewerId, String userIds, String objectType,int page, int count) {
        List<String> cols0 = StringUtils2.splitList(toStr("target,commenter"), ",", true);
        final String sql = new SQLBuilder.Select(commentSchema)
                .select(cols0)
                .from("comment")
                .where("destroyed_time = 0")
                .and("left(target,2)='" + objectType + ":'")
                .and("length(target)>10")
                .andIf(!userIds.isEmpty(), "commenter IN (" + userIds + ")")
                .orderBy("comment_id", "DESC")
                .limitByPage(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        for (Record r : recs) {
            r.put("target", r.getString("target").replace(objectType + ":", ""));
            r.renameColumn("commenter", "source");
        }
        return recs;
    }

    @Override
    protected boolean getIHasCommented0(String commenter, String object) {
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(object), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT comment_id FROM ${table} WHERE destroyed_time = 0 and commenter="+commenter+" and target like '%" + a2[0] + "%'";
        } else {
            SQL = "SELECT comment_id FROM ${table} WHERE destroyed_time = 0 and commenter="+commenter+" and target='" + object + "'";
        }
        final String sql = SQLTemplate.merge(SQL,
                "table", commentTable);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.size()>0;
    }

    @Override
    protected RecordSet getHotTargetByCommented0(String targetType, long max,long min,int page, int count) {
        String sql = "select distinct(target),count(target) as count1 from comment where" +
                " destroyed_time=0 and substr(target,1,1)='" + targetType + "' group by target order by count1 desc";

        if (count > 0)
            sql += " " + SQLUtils.pageToLimit(page, count) + "";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected Record findMyLastedCommented0(String target,String commenter) {
        String sql = "select comment_id,created_time,message from comment where" +
                " destroyed_time=0 and target='" + target + "' and commenter='" + commenter + "'" +
                " order by created_time desc limit 1";

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }

    @Override
    protected boolean updateCommentTarget0(String old_target, String new_target) {
        String sql = "update comment set target = '" + new_target + "' where target='" + old_target + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

}
