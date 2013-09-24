package com.borqs.server.wutong.comment;

import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.*;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.*;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.like.LikeLogic;
import com.borqs.server.wutong.stream.StreamLogic;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class CommentImpl implements CommentLogic, Initializable {
    private static final Logger L = Logger.getLogger(CommentImpl.class);
    public final Schema commentSchema = Schema.loadClassPath(CommentImpl.class, "comment.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String commentTable;
    private Commons commons;
    private Configuration conf;


    public void init() {
        commons = new Commons();
        conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.commentTable = conf.getString("comment.simple.commentTable", "comment");
    }

    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    public boolean saveComment(Context ctx, Record comment) {
        final String METHOD = "saveComment";
        L.traceStartCall(ctx, METHOD, comment);

        final String SQL = "INSERT INTO ${table} ${values_join(alias, comment, add)}";

        String sql = SQLTemplate.merge(SQL,
                "table", commentTable, "alias", commentSchema.getAllAliases(),
                "comment", comment, "add", Record.of("destroyed_time", 0));
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public Record destroyComments(Context ctx, String userId, String commentId, String fromSource, String objectType) {
        final String METHOD = "destroyComments";
        L.traceStartCall(ctx, METHOD, userId, commentId, fromSource, objectType);
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
        L.traceEndCall(ctx, METHOD);
        return Record.of("comment_id", commentId, "result", n > 0);
    }

    public boolean updateCanLike0(Context ctx, String userId, String commentId, boolean can_like) {
        final String METHOD = "updateCanLike0";
        L.traceStartCall(ctx, METHOD, userId, commentId, can_like);
        String sql = "update comment set can_like=" + can_like + "" +
                " where comment_id=" + commentId + " and (commenter=" + userId + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public int getCommentCount(Context ctx, String viewerId, String targetId) {
        final String METHOD = "getCommentCount";
        L.traceStartCall(ctx, METHOD, targetId);
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL = "";
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
        Number count = (Number) se.executeScalar(sql);
        L.traceEndCall(ctx, METHOD);
        return count.intValue();
    }

    @Override
    public Map<String, Integer> getCommentCounts(Context ctx,String viewerId, String[] targetIds) {
        if (ArrayUtils.isEmpty(targetIds))
            return new HashMap<String, Integer>();

        LinkedHashSet<String> apkTargetIds = new LinkedHashSet<String>();
        LinkedHashSet<String> otherTargetIds = new LinkedHashSet<String>();
        separateApkTargetIdsAndOtherTargetIds(targetIds, apkTargetIds, otherTargetIds);
        return getCommentCounts0(ctx, viewerId, apkTargetIds, otherTargetIds);
    }
    private Map<String,Integer> getCommentCounts0(Context ctx,String viewerId,Collection<String> apkTargetIds, Collection<String> otherTargetIds) {
        ArrayList<String> sqls = new ArrayList<String>();
        for (String apkTargetId : apkTargetIds) {
            String targetTypeWithApkPackage = StringUtils.substringBefore(apkTargetId, "-");
            String sql = new SQLBuilder.Select()
                    .select(Sql.sqlValue(apkTargetId) + "AS target", "COUNT(*) AS comment_count")
                    .from(commentTable)
                    .where("target like ${v(package)}", "package", targetTypeWithApkPackage + "-%")
                    .toString();
            if (!viewerId.equals("") && !viewerId.equals("0")) {
                sql += " and comment_id not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_COMMENT + "' and user='" + viewerId + "') ";
                sql += " and commenter not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_USER + "' and user='" + viewerId + "') ";
            }
            sql += " group by target";
            sqls.add(sql);
        }
        if (!otherTargetIds.isEmpty()) {
            String sql = new SQLBuilder.Select()
                    .select("target", "COUNT(*) as comment_count")
                    .from(commentTable)
                    .where("target IN (${vjoin(target_ids)})", "target_ids", otherTargetIds)
                    .toString();

            if (!viewerId.equals("") && !viewerId.equals("0")) {
                sql += " and comment_id not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_COMMENT + "' and user='" + viewerId + "') ";
                sql += " and commenter not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_USER + "' and user='" + viewerId + "') ";
            }
            sql += " group by target";
            sqls.add(sql);
        }
        String sql = SQLBuilder.union(sqls);
        final HashMap<String, Integer> m = new HashMap<String, Integer>();
        SQLExecutor se = getSqlExecutor();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                m.put(rec.getString("target"), (int) rec.getInt("comment_count"));
            }
        });
        CollectionUtils2.fillMissingWithValue(m, apkTargetIds.toArray(new String[apkTargetIds.size()]), 0);
        CollectionUtils2.fillMissingWithValue(m, otherTargetIds.toArray(new String[otherTargetIds.size()]), 0);
        return m;
    }
    private static void separateApkTargetIdsAndOtherTargetIds(String[] targetIds, Collection<String> apkTargetIds, Collection<String> otherTargetIds) {
        for (String targetId : targetIds) {
            if (StringUtils.contains(targetId, ':')
                    && Integer.toString(Constants.APK_OBJECT).equals(StringUtils.substringBefore(targetId, ":"))) {
                if (apkTargetIds != null)
                    apkTargetIds.add(targetId);
            } else {
                if (otherTargetIds != null)
                    otherTargetIds.add(targetId);
            }
        }
    }
    public RecordSet findCommentsFor(Context ctx, String targetId, List<String> cols, boolean asc, int page, int count) {
        final String METHOD = "findCommentsFor";
        L.traceStartCall(ctx, METHOD, targetId);
        if (!cols.contains("comment_id"))
            cols.add("comment_id");
        if (!cols.contains("parent_id"))
            cols.add("parent_id");
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                    + " WHERE destroyed_time = 0 AND ${alias.target} like '%" + a2[0] + "-%' ORDER BY ${alias.comment_id} ${asc} ${limit}";
        } else {
            SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                    + " WHERE destroyed_time = 0 AND ${alias.target}=${v(target)} ORDER BY ${alias.comment_id} ${asc} ${limit}";
        }

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", commentSchema.getAllAliases()},
                {"cols", cols},
                {"table", commentTable},
                {"target", targetId},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(commentSchema, recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet findCommentsForContainsIgnore(Context ctx, String viewerId, String targetId, List<String> cols, boolean asc, int page, int count) {

        final String METHOD = "findCommentsForContainsIgnore";
        L.traceStartCall(ctx, METHOD, targetId);
        if (!cols.contains("comment_id"))
            cols.add("comment_id");
        if (!cols.contains("parent_id"))
            cols.add("parent_id");
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                    + " WHERE destroyed_time = 0 AND ${alias.target} like '%" + a2[0] + "-%'";
        } else {
            SQL = "SELECT ${as_join(alias, cols)} FROM ${table}"
                    + " WHERE destroyed_time = 0 AND ${alias.target}=${v(target)}";
        }
        if (!viewerId.equals("") && !viewerId.equals("0")) {
            SQL += " and comment_id not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_COMMENT + "' and user='" + viewerId + "') ";
            SQL += " and commenter not in (select target_id from ignore_ where target_type='" + Constants.IGNORE_USER + "' and user='" + viewerId + "') ";
        }
        SQL += "  ORDER BY ${alias.comment_id} ${asc} ${limit}";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet findComments(Context ctx, List<String> commentId0, List<String> cols) {
        final String METHOD = "findComments";
        L.traceStartCall(ctx, METHOD, commentId0, cols);
        if (!cols.contains("comment_id"))
            cols.add("comment_id");
        if (!cols.contains("parent_id"))
            cols.add("parent_id");
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet findCommentsAll(Context ctx, List<String> commentId0, List<String> cols) {
        final String METHOD = "findCommentsAll";
        L.traceStartCall(ctx, METHOD, commentId0, cols);
        if (!cols.contains("comment_id"))
            cols.add("comment_id");
        if (!cols.contains("parent_id"))
            cols.add("parent_id");
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet findCommentedPost(Context ctx, String userId, int page, int count, int objectType) {
        final String METHOD = "findCommentedPost";
        L.traceStartCall(ctx, METHOD, userId, page, count, objectType);
        final String SQL = "SELECT DISTINCT(${alias.target}) FROM ${table} use index (commenter) "
                + " WHERE ${alias.commenter}=${commenter} AND LEFT(${alias.target},1)=${objectType} " +
                " AND ${alias.destroyed_time}=0" +
                " ORDER BY ${alias.comment_id} DESC ${limit}";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet findWhoCommentTarget0(Context ctx, String target, int limit) {
        final String METHOD = "findWhoCommentTarget0";
        L.traceStartCall(ctx, METHOD, target, limit);
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getObjectCommentByUsers0(Context ctx, String viewerId, String userIds, String objectType, int page, int count) {
        final String METHOD = "getObjectCommentByUsers0";
        L.traceStartCall(ctx, METHOD, viewerId, userIds, objectType, page, count);
        List<String> cols0 = StringUtils2.splitList("target,commenter", ",", true);
        final String sql = new SQLBuilder.Select(commentSchema)
                .select(cols0)
                .from("comment")
                .where("destroyed_time = 0")
                .and("left(target,2)='" + objectType + ":'")
                .and("length(target)>" + Constants.USER_ID_MAX_LEN)
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public boolean getIHasCommented(Context ctx, String commenter, String object) {
        final String METHOD = "getIHasCommented";
        L.traceStartCall(ctx, METHOD, commenter, object);
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(object), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT comment_id FROM ${table} WHERE destroyed_time = 0 and commenter=" + commenter + " and target like '%" + a2[0] + "%'";
        } else {
            SQL = "SELECT comment_id FROM ${table} WHERE destroyed_time = 0 and commenter=" + commenter + " and target='" + object + "'";
        }
        final String sql = SQLTemplate.merge(SQL,
                "table", commentTable);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs.size() > 0;
    }


    public RecordSet getHotTargetByCommented(Context ctx, String targetType, long max, long min, int page, int count) {
        final String METHOD = "getHotTargetByCommented";
        L.traceStartCall(ctx, METHOD, targetType, max, min, page, count);
        String sql = "select distinct(target),count(target) as count1 from comment where" +
                " destroyed_time=0 and substr(target,1,1)='" + targetType + "' group by target order by count1 desc";

        if (count > 0)
            sql += " " + SQLUtils.pageToLimit(page, count) + "";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }


    public Record findMyLastedCommented(Context ctx, String target, String commenter) {
        final String METHOD = "findMyLastedCommented";
        L.traceStartCall(ctx, METHOD, target, commenter);
        String sql = "select comment_id,created_time from comment where" +
                " destroyed_time=0 and target='" + target + "' and commenter='" + commenter + "'" +
                " order by created_time desc limit 1";

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        L.traceEndCall(ctx, METHOD);
        return rec;
    }


    public boolean updateCommentTarget(Context ctx, String old_target, String new_target) {
        final String METHOD = "updateCommentTarget";
        L.traceStartCall(ctx, METHOD, old_target, new_target);
        String sql = "update comment set target = '" + new_target + "' where target='" + old_target + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    //====================================================================================

    protected static String genCommentId() {
        return Long.toString(RandomUtils.generateId());
    }

    public static Record addCommentIdStrCol(Record rec) {
        if (rec != null && rec.has("comment_id") && !rec.has("comment_id_s"))
            rec.put("comment_id_s", rec.getString("comment_id"));
        return rec;
    }

    public static RecordSet addCommentIdStrCol(RecordSet recs) {
        if (recs != null) {
            for (Record rec : recs)
                addCommentIdStrCol(rec);
        }
        return recs;
    }

    public RecordSet addParentMessageCol(Context ctx, RecordSet recs) {
        final String METHOD = "addParentMessageCol";
        L.traceStartCall(ctx, METHOD, recs);

        String parentIds = recs.joinColumnValues("parent_id", ",");
        if (StringUtils.isNotBlank(parentIds)) {
            String sql = "SELECT * FROM comment WHERE comment_id IN (" + parentIds + ") AND destroyed_time=0";
            SQLExecutor se = getSqlExecutor();
            RecordSet rs = se.executeRecordSet(sql, null);
            Record map = new Record();
            for (Record r : rs) {
                map.put(r.getString("comment_id"), r);
            }

            for (Record rec : recs) {
                Record parentComment = (Record)map.get(rec.getString("parent_id"));
                rec.put("parent_comment", parentComment == null ? new Record() : parentComment);
            }
        }

        return recs;
    }

    public String createComment(Context ctx, String userId0, String targetId0, Record comment0) {
        final String METHOD = "createComment";
        L.traceStartCall(ctx, METHOD, userId0, targetId0, comment0);
        Schemas.checkRecordIncludeColumns(comment0, "message", "commenter_name");
        comment0.put("target", targetId0);
        comment0.put("created_time", DateUtils.nowMillis());
        comment0.putMissing("can_like", true);
        comment0.putMissing("device", "");
        String commentId = genCommentId();
        comment0.put("comment_id", commentId);
        comment0.put("commenter", userId0);

        Schemas.standardize(commentSchema, comment0);
        boolean b = saveComment(ctx, comment0);
        if (!b)
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "Save comment error");

        L.traceEndCall(ctx, METHOD);
        return commentId;
    }

    public RecordSet getCommentsFor(Context ctx, String targetId, String cols, boolean asc, int page, int count) {
        final String METHOD = "getCommentsFor";
        L.traceStartCall(ctx, METHOD, targetId, cols, asc, page, count);
        try {
            List<String> cols0 = StringUtils2.splitList(cols, ",", true);
            if (cols0.isEmpty())
                return new RecordSet();

            RecordSet recs = addCommentIdStrCol(findCommentsFor(ctx, targetId, cols0, asc, page, count));
            recs = addParentMessageCol(ctx, recs);
            L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "get comment error");
        }
    }

    public RecordSet getCommentsForContainsIgnore(Context ctx, String viewerId, String targetId, String cols, boolean asc, int page, int count) {
        final String METHOD = "getCommentsForContainsIgnore";
        L.traceStartCall(ctx, METHOD, viewerId, targetId, cols, asc, page, count);
        try {
            List<String> cols0 = StringUtils2.splitList(cols, ",", true);
            if (cols0.isEmpty())
                return new RecordSet();

            RecordSet recs = addCommentIdStrCol(findCommentsForContainsIgnore(ctx, viewerId, targetId, cols0, asc, page, count));
            recs = addParentMessageCol(ctx, recs);
            L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "get comment error");
        }
    }

    public RecordSet getComments(Context ctx, String commentIds, String cols) {
        final String METHOD = "getComments";
        L.traceStartCall(ctx, METHOD, commentIds, cols);
        try {
            List<String> commentIds0 = StringUtils2.splitList(commentIds, ",", true);
            List<String> cols0 = StringUtils2.splitList(cols, ",", true);
            if (cols0.isEmpty() || commentIds0.isEmpty())
                return new RecordSet();

            RecordSet recs = addCommentIdStrCol(findComments(ctx, commentIds0, cols0));
            recs = addParentMessageCol(ctx, recs);
            L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "get comment error");
        }
    }

    public RecordSet getCommentsAll(Context ctx, String commentIds, String cols) {
        final String METHOD = "getCommentsAll";
        L.traceStartCall(ctx, METHOD, commentIds, cols);
        try {
            List<String> commentIds0 = StringUtils2.splitList(commentIds, ",", true);
            List<String> cols0 = StringUtils2.splitList(cols, ",", true);
            if (cols0.isEmpty() || commentIds0.isEmpty())
                return new RecordSet();

            RecordSet recs = addCommentIdStrCol(findCommentsAll(ctx, commentIds0, cols0));
            recs = addParentMessageCol(ctx, recs);
            L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "get comment error");
        }
    }

    public RecordSet getCommentedPost(Context ctx, String userId, int page, int count, int objectType) {
        final String METHOD = "getCommentedPost";
        L.traceStartCall(ctx, METHOD, userId, page, count, objectType);
        try {
            RecordSet recs = addCommentIdStrCol(findCommentedPost(ctx, userId, page, count, objectType));
            recs = addParentMessageCol(ctx, recs);
            L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "get comment error");
        }
    }

    public RecordSet findWhoCommentTarget(Context ctx, String target, int limit) {
        final String METHOD = "findWhoCommentTarget";
        L.traceStartCall(ctx, METHOD, target, limit);
        try {
            RecordSet recs = findWhoCommentTarget0(ctx, target, limit);
            L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "get comment error");
        }
    }

    public RecordSet getObjectCommentByUsers(Context ctx, String viewerId, String userIds, String objectType, int page, int count) {
        final String METHOD = "getObjectCommentByUsers";
        L.traceStartCall(ctx, METHOD, viewerId, userIds, objectType, page, count);
        try {
            RecordSet recs = getObjectCommentByUsers0(ctx, viewerId, userIds, objectType, page, count);
            L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "get comment error");
        }
    }

    public boolean updateCanLike(Context ctx, String userId, String commentId, boolean can_like) {
        final String METHOD = "updateCanLike";
        L.traceStartCall(ctx, METHOD, userId, commentId, can_like);
        try {
            boolean b = updateCanLike0(ctx, userId, commentId, can_like);
            L.traceEndCall(ctx, METHOD);
            return b;
        } catch (Throwable t) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "update comment error");
        }
    }


    //===============================================PPPPPPPPPPPPPPPP=========================================
    public boolean updateCommentTargetP(Context ctx, String target_type, String old_target, String new_target) {
        final String METHOD = "updateCommentTargetP";
        L.traceStartCall(ctx, METHOD, target_type, old_target, new_target);
        old_target = target_type + ":" + old_target;
        new_target = target_type + ":" + new_target;
        boolean b = updateCommentTarget(ctx, old_target, new_target);
        L.traceEndCall(ctx, METHOD);
        return b;
    }

    public RecordSet findWhoCommentTargetP(Context ctx, String target, int limit) {
        return findWhoCommentTarget(ctx, target, limit);
    }

    public Record createCommentP(Context ctx, String userId, int objectType, String target, String message, String device, Boolean canLike, String location, String add_to, String appId, String parentId) {
        final String METHOD = "createCommentP";
        L.traceStartCall(ctx, METHOD, userId, userId, objectType,target,message,device,canLike,location,add_to,appId);
        // check user
        ConversationLogic conversationLogic = GlobalLogics.getConversation();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        StreamLogic streamLogic = GlobalLogics.getStream();
        Record user = accountLogic.getUser(ctx, userId, userId, "user_id, display_name, login_email1, login_email2, login_email3", false);
        if (user.isEmpty())
            throw new ServerException(WutongErrors.USER_NOT_EXISTS, "User '" + userId + "' is not exists");

        // check can comment
        String targetObjectId;
        if (objectType == Constants.POST_OBJECT) {
            if (!streamLogic.postCanCommentP(ctx, target))
                throw new ServerException(WutongErrors.STREAM_CANT_COMMENT, "The formPost '%s' is not can comment", target);

            targetObjectId = Constants.postObjectId(target);
        } else if (objectType == Constants.APK_OBJECT) {
            targetObjectId = Constants.apkObjectId(target);
        } else if (objectType == Constants.FILE_OBJECT) {
            targetObjectId = Constants.fileObjectId(target);
        } else if (objectType == Constants.PHOTO_OBJECT) {
            targetObjectId = Constants.photoObjectId(target);
        } else if (objectType == Constants.POLL_OBJECT) {
            targetObjectId = Constants.pollObjectId(target);
        } else {
            throw new ServerException(WutongErrors.COMMENT_CANT_COMMENT, "The object '%s' is not can comment", Constants.objectId(objectType, target));
        }

        //get user lasted comment for target
        Record last_comment = findMyLastedCommented(ctx, objectType + ":" + target, userId);
        if (last_comment.getString("message").equals(message)) {
            long created_time = last_comment.getInt("created_time");
            if (DateUtils.nowMillis() - created_time <= 1000 * 60 * 10L)
                throw new ServerException(WutongErrors.COMMENT_REPEAT_CONTENT, "Repeat Comment in 10 minutes!", Constants.objectId(objectType, target));
        }

        // create comment
        Record rec = Record.of("message", message,
                "commenter_name", user.getString("display_name"));
        rec.put("device", device);
        if (canLike != null)
            rec.put("can_like", canLike);

        rec.put("add_to", add_to);
        rec.put("parent_id", parentId);

        String commentId;

        Record thisUser = accountLogic.getUsers(ctx, userId, userId, "display_name", true).getFirstRecord();

        commentId = createComment(ctx, userId, targetObjectId, rec);
        conversationLogic.createConversationP(ctx, Constants.COMMENT_OBJECT, commentId, Constants.C_COMMENT_CREATE, userId);
        if (add_to.length() > 0)
            conversationLogic.createConversationP(ctx, Constants.COMMENT_OBJECT, commentId, Constants.C_COMMENT_ADDTO, add_to);
        if (objectType == Constants.POST_OBJECT) {
            conversationLogic.createConversationP(ctx, Constants.POST_OBJECT, target, Constants.C_STREAM_COMMENT, userId);
            Record this_stream = streamLogic.getPostP(ctx, target, "post_id,source,message");
            String body = message;

            commons.sendNotification(ctx, Constants.NTF_MY_STREAM_COMMENT,
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(userId),
                    commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), this_stream.getString("source"), this_stream.getString("message")),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(target, commentId),
                    commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), this_stream.getString("source"), this_stream.getString("message"), commentId),
                    commons.createArrayNodeFromStrings(body),
                    commons.createArrayNodeFromStrings(body),
                    commons.createArrayNodeFromStrings(target),
                    commons.createArrayNodeFromStrings(target, userId, commentId)
            );
        }
        if (objectType == Constants.APK_OBJECT) {
            String[] o = target.split("-");
            String t = "";
            if (o.length == 3 || o.length == 1)
                t = o[0];
            if (t.length() > 0)
                conversationLogic.createConversationP(ctx, Constants.APK_OBJECT, t, Constants.C_APK_COMMENT, userId);
            Record mcs = commons.thisTrandsGetApkInfo(ctx, userId, target, "app_name", 1000).getFirstRecord();
            String body = message;

            commons.sendNotification(ctx, Constants.NTF_MY_APP_COMMENT,
                    commons.createArrayNodeFromStrings(appId),
                    commons.createArrayNodeFromStrings(userId),
                    commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(target, commentId),
                    commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                    commons.createArrayNodeFromStrings(body),
                    commons.createArrayNodeFromStrings(body),
                    commons.createArrayNodeFromStrings(target),
                    commons.createArrayNodeFromStrings(target, userId, commentId)
            );
        }

        if (objectType == Constants.APK_OBJECT) {
            String m = "";
            String tempNowAttachments = "[]";
            GlobalLogics.getStream().autoPost(ctx, userId, Constants.APK_COMMENT_POST, message, tempNowAttachments, String.valueOf(Constants.APP_TYPE_QIUPU), "", target, m, "", false, Constants.QAPK_FULL_COLUMNS, device, location, true, true, true, "", "", false,Constants.POST_SOURCE_SYSTEM);
//                String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
//                sendCommentOrLikeEmail(Constants.APK_OBJECT, userId, user, target, message, lang);
        } else if (objectType == Constants.BOOK_OBJECT) {//for book

        } else if (objectType == Constants.PHOTO_OBJECT) {//for PHOTO
            conversationLogic.createConversationP(ctx, Constants.PHOTO_OBJECT, target, Constants.C_PHOTO_COMMENT, userId);

            commons.sendNotification(ctx, Constants.NTF_PHOTO_COMMENT,
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(userId),
                    commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), message, commentId),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(target, commentId),
                    commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), message, commentId),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(target),
                    commons.createArrayNodeFromStrings(target, userId, commentId)
            );

        } else if (objectType == Constants.FILE_OBJECT) {//for PHOTO
            conversationLogic.createConversationP(ctx, Constants.FILE_OBJECT, target, Constants.C_FILE_COMMENT, userId);

            commons.sendNotification(ctx, Constants.NTF_FILE_COMMENT,
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(userId),
                    commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), message, commentId),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(target, commentId),
                    commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), message, commentId),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(target),
                    commons.createArrayNodeFromStrings(target, userId, commentId)
            );

        } else if (objectType == Constants.POLL_OBJECT) {//for poll
            conversationLogic.createConversationP(ctx, Constants.POLL_OBJECT, target, Constants.C_POLL_COMMENT, userId);
            String title = GlobalLogics.getPoll().getPolls(ctx, target).getFirstRecord().getString("title");
            commons.sendNotification(ctx, Constants.NTF_POLL_COMMENT,
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(userId),
                    commons.createArrayNodeFromStrings(title),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(),
                    commons.createArrayNodeFromStrings(target, commentId),
                    commons.createArrayNodeFromStrings(title, target),
                    commons.createArrayNodeFromStrings(message),
                    commons.createArrayNodeFromStrings(message),
                    commons.createArrayNodeFromStrings(target),
                    commons.createArrayNodeFromStrings(target, userId, commentId)
            );

        }

        Record commentRec = getCommentP(ctx, userId, commentId, "comment_id, target, commenter, commenter_name, created_time, message,add_to");
        if (objectType == Constants.POST_OBJECT) {
            streamLogic.touch(ctx, target);
        }

        L.traceEndCall(ctx, METHOD);
        return commentRec;
    }


    public Record createComment1P(Context ctx, String userId, int objectType, String target, String message, String device, String location, String add_to, String appId, String parentId) {
        return createCommentP(ctx, userId, objectType, target, message, device, true, location, add_to, appId, parentId);
    }

    public RecordSet destroyCommentsP(Context ctx, String userId, String commentIds) {
        final String METHOD = "destroyCommentsP";
        L.traceStartCall(ctx, METHOD, userId, userId, commentIds);
        // check user
        AccountLogic accountLogic = GlobalLogics.getAccount();
        StreamLogic streamLogic = GlobalLogics.getStream();
        ConversationLogic conversationLogic = GlobalLogics.getConversation();
        if (Constants.isNullUserId(userId)) {
            if (!accountLogic.hasUser(ctx, Long.parseLong(userId))) {
                throw new ServerException(WutongErrors.USER_NOT_EXISTS, "User '%s' is not exists", userId);
            }
        }

        RecordSet rsd = new RecordSet();
        //update lasted stream' acctachments
        //get target from comment_id
        List<String> cIds = StringUtils2.splitList(commentIds, ",", true);

        for (String commentId : cIds) {
            Record rec = getCommentsAll(ctx, commentId, "target").getFirstRecord();
            String[] ss = StringUtils.split(StringUtils.trimToEmpty(rec.getString("target")), ':');

            if (ss[0].equals(String.valueOf(Constants.POST_OBJECT))) {
                //get stream，
                Record r0 = streamLogic.getPostP(ctx, ss[1], "source");
                rsd.add(destroyComments(ctx, userId, commentId, r0.getString("source"), ss[0]));
            } else {
                rsd.add(destroyComments(ctx, userId, commentId, "", ss[0]));
            }

            //must after delete comment
            conversationLogic.deleteConversationP(ctx, Constants.COMMENT_OBJECT, commentId, -1, 0);
            if (ss[0].equals(String.valueOf(Constants.POST_OBJECT))) {
                conversationLogic.deleteConversationP(ctx, Constants.POST_OBJECT, ss[1], Constants.C_STREAM_COMMENT, Long.parseLong(userId));
            } else if (ss[0].equals(String.valueOf(Constants.APK_OBJECT))) {
                String pg = ss[1];
                if (ss[1].split("-").length > 1)
                    pg = ss[1].split("-")[0].toString();
                conversationLogic.deleteConversationP(ctx, Constants.APK_OBJECT, pg, Constants.C_APK_COMMENT, Long.parseLong(userId));
            } else if (ss[0].equals(String.valueOf(Constants.PHOTO_OBJECT))) {
                String pg = ss[1];
                conversationLogic.deleteConversationP(ctx, Constants.PHOTO_OBJECT, pg, Constants.C_PHOTO_COMMENT, Long.parseLong(userId));
            }
        }

        for (String commentId : cIds) {
            Record rec = getCommentsAll(ctx, commentId, "target").getFirstRecord();
            String[] ss = StringUtils.split(StringUtils.trimToEmpty(rec.getString("target")), ':');
            if (ss[0].equals(String.valueOf(Constants.APK_OBJECT))) {
                //get stream，update
                String apk_id = ss[1];
                Record r = streamLogic.topOneStreamByTarget(ctx, Constants.APK_COMMENT_POST, apk_id).getFirstRecord();
                String attach = commons.thisTrandsGetApkInfo(ctx, userId, apk_id, Constants.QAPK_FULL_COLUMNS, 1000).toString(false, false);
                streamLogic.updateAttachment(ctx, r.getString("post_id"), attach);
            }
        }

        L.traceEndCall(ctx, METHOD);
        return rsd;
    }

    public RecordSet destroyCommentsP(Context ctx, String commentIds) {
        return destroyCommentsP(ctx, Constants.NULL_USER_ID, commentIds);
    }

    public int getCommentCountP(Context ctx, String viewerId, int objectType, Object id) {
        String targetObjectId = Constants.objectId(objectType, id);
        return getCommentCount(ctx, viewerId, targetObjectId);
    }

    public boolean updateCommentCanLikeP(Context ctx, String userId, String commentId, boolean can_like) {
        return updateCanLike(ctx, userId, commentId, can_like);
    }

    public RecordSet getCommentsForP(Context ctx, String viewerId, int objectType, Object id, String cols, boolean asc, int page, int count) {
        final String METHOD = "getCommentsForP";
        L.traceStartCall(ctx, METHOD, objectType, id, cols,asc,page, count);
        String targetObjectId = Constants.objectId(objectType, id);
        RecordSet recs = getCommentsFor(ctx, targetObjectId, cols, asc, page, count);
        recs = GlobalLogics.getIgnore().formatIgnoreStreamOrCommentsP(ctx, viewerId, "comment", recs);
        L.debug(ctx, "recs="+recs);
        RecordSet recordSet = transComment(ctx, viewerId, recs);
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    public RecordSet getCommentsForContainsIgnore(Context ctx, String viewerId, int objectType, Object id, String cols, boolean asc, int page, int count) {
        final String METHOD = "getCommentsForContainsIgnore";
        L.traceStartCall(ctx, METHOD, objectType, id, cols, page, count);
        String targetObjectId = Constants.objectId(objectType, id);
        RecordSet recs = getCommentsForContainsIgnore(ctx, viewerId, targetObjectId, cols, asc, page, count);
        RecordSet recordSet = transComment(ctx, viewerId, recs);
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    public RecordSet formatIgnoreStreamOrComments(Context ctx, String viewerId, String sORc, RecordSet recs) {
        final String METHOD = "formatIgnoreStreamOrComments";
        L.traceStartCall(ctx, METHOD, sORc,recs);
        if (!viewerId.equals("") && !viewerId.equals("0")) {
            List<String> stream_ignore = new ArrayList<String>();
            List<String> user_ignore_list = new ArrayList<String>();
            String target_type = sORc.equals("stream") ? String.valueOf(Constants.IGNORE_STREAM) : String.valueOf(Constants.IGNORE_COMMENT);
            String column_name = sORc.equals("stream") ? "post_id" : "comment_id";
            String source_name = sORc.equals("stream") ? "source" : "commenter";
            RecordSet recs_ignore = GlobalLogics.getIgnore().getIgnoreListSimpleP(ctx, viewerId, target_type, 0, 1000);
            L.debug(ctx, "recs_ignore="+recs_ignore);
            RecordSet recs_user_ignore = GlobalLogics.getIgnore().getIgnoreListSimpleP(ctx, viewerId, String.valueOf(Constants.IGNORE_USER), 0, 1000);
            L.debug(ctx, "recs_user_ignore="+recs_user_ignore);
            if (recs_ignore.size() > 0) {
                for (Record r : recs_ignore) {
                    stream_ignore.add(r.getString("target_id"));
                }
            }

            if (recs_user_ignore.size() > 0) {
                for (Record r : recs_user_ignore) {
                    user_ignore_list.add(r.getString("target_id"));
                }
            }

            for (int i = recs.size() - 1; i >= 0; i--) {
                if (stream_ignore.contains(recs.get(i).getString(column_name)) || user_ignore_list.contains(recs.get(i).getString(source_name))) {
                    recs.remove(i);
                }
            }
        }
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getFullCommentsForP(Context ctx, String viewerId, int objectType, Object id, boolean asc, int page, int count) {
        return getCommentsForContainsIgnore(ctx, viewerId, objectType, id, Constants.FULL_COMMENT_COLUMNS, asc, page, count);
    }

    public RecordSet getCommentsP(Context ctx, String viewerId, String commentIds, String cols) {
        final String METHOD = "getCommentsP";
        L.traceStartCall(ctx, METHOD, commentIds,cols);
        if (cols.isEmpty() || cols.equals("")) {
            cols = Constants.FULL_COMMENT_COLUMNS;
        }
        RecordSet recs = getComments(ctx, commentIds, cols);
        recs = GlobalLogics.getIgnore().formatIgnoreStreamOrCommentsP(ctx, viewerId, "comment", recs);
        RecordSet recordSet = transComment(ctx, viewerId, recs);
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    public RecordSet transComment(Context ctx, String viewerId, RecordSet commentDs) {
        final String METHOD = "transComment";
        L.traceStartCall(ctx, METHOD, commentDs);
        if (commentDs.size() > 0) {
            LikeLogic likeLogic = GlobalLogics.getLike();
            AccountLogic accountLogic = GlobalLogics.getAccount();
            for (Record r : commentDs) {
                r.put("image_url",GlobalLogics.getAccount().getUsersBaseColumns(ctx,r.getString("commenter")).getFirstRecord().getString("image_url"));
                Record likes = new Record();
                likes.put("count", likeLogic.getLikeCount(ctx, String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id")));
                RecordSet lu = likeLogic.loadLikedUsers(ctx, String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id"), 0, 500);
                if (lu.size() > 0) {
                    String uid = lu.joinColumnValues("liker", ",");
                    likes.put("users", accountLogic.getUsersBaseColumns(ctx, uid));
                } else {
                    likes.put("users", new RecordSet());
                }
                r.put("likes", likes);

                if (!viewerId.equals("")) {
                    r.put("iliked", likeLogic.ifUserLiked(ctx, viewerId, String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id")));
                } else {
                    r.put("iliked", false);
                }
                if (r.getString("add_to").trim().length() > 0) {
                    RecordSet recs = accountLogic.getUsersBaseColumns(ctx, r.getString("add_to"));
                    r.put("add_new_users", recs.toString());
                } else {
                    r.put("add_new_users", new RecordSet());
                }
            }
        }
        L.traceEndCall(ctx, METHOD);
        return commentDs;
    }

    public RecordSet getCommentsAllP(Context ctx, String commentIds, String cols) {
        final String METHOD = "getCommentsAllP";
        L.traceStartCall(ctx, METHOD, commentIds, cols);
        if (cols.isEmpty() || cols.equals("")) {
            cols = Constants.FULL_COMMENT_COLUMNS;
        }
        RecordSet recs = getCommentsAll(ctx, commentIds, cols);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getFullComments(Context ctx, String viewerId, String commentIds) {
        return getCommentsP(ctx, viewerId, commentIds, Constants.FULL_COMMENT_COLUMNS);
    }

    public Record getCommentP(Context ctx, String viewerId, String commentId, String cols) {
        return getCommentsP(ctx, viewerId, firstId(commentId), cols).getFirstRecord();
    }

    private static String firstId(String ids) {
        return StringUtils.substringBefore(ids, ",").trim();
    }

    public Record getFullComment(Context ctx, String viewerId, String commentId) {
        return getCommentP(ctx, viewerId, commentId, Constants.FULL_COMMENT_COLUMNS);
    }


    public boolean commentCanLikeP(Context ctx, String viewerId, String commentId) {
        return getCommentP(ctx, viewerId, commentId, "can_like").getBoolean("can_like", false);
    }


    public RecordSet getCommentedPostTargetP(Context ctx, String userId, int objectType, int page, int count) {
        RecordSet recs = getCommentedPost(ctx, userId, page, count, objectType);
        return recs;
    }

    public boolean getIHasCommentedP(Context ctx, String commenter, int target_type, String target_id) {
        return getIHasCommented(ctx, commenter, String.valueOf(target_type) + ":" + target_id);
    }

    public RecordSet getCommentedPostsP(Context ctx, String userId, String cols, int objectType, int page, int count) {
        final String METHOD = "getCommentedPostsP";
        L.traceStartCall(ctx, METHOD, userId, cols,objectType,page,count);
        RecordSet recs = getCommentedPost(ctx, userId, page, count, objectType);
        StreamLogic streamLogic = GlobalLogics.getStream();
        for (Record rec : recs) {
            rec.put("post_id", rec.getString("target").replace(objectType + ":", ""));
        }

        if (cols.isEmpty() || cols.equals("")) {
            cols = Constants.POST_FULL_COLUMNS;
        }

        RecordSet recordSet = streamLogic.getPosts(ctx, recs.joinColumnValues("post_id", ","), cols);
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    public RecordSet getCommentsForContainsIgnoreP(Context ctx, String viewerId, int objectType, Object id, String cols, boolean asc, int page, int count) {
        final String METHOD = "getCommentsForContainsIgnoreP";
        L.traceStartCall(ctx, METHOD, objectType,id,cols,asc,page,count);
        String targetObjectId = Constants.objectId(objectType, id);
        RecordSet recs = getCommentsForContainsIgnore(ctx, viewerId, targetObjectId, cols, asc, page, count);
        RecordSet recordSet = transComment(ctx, viewerId, recs);
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }
}
