package com.borqs.server.wutong.ignore;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.comment.CommentLogic;
import com.borqs.server.wutong.like.LikeLogic;
import com.borqs.server.wutong.stream.StreamLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class IgnoreImpl implements IgnoreLogic, Initializable {
    Logger L = Logger.getLogger(IgnoreImpl.class);
    public final Schema ignoreSchema = Schema.loadClassPath(IgnoreImpl.class, "ignore.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String ignoreTable = "ignore_";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("stream.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("stream.simple.db", null);
        this.ignoreTable = conf.getString("ignore.simple.ignoreTable", "ignore_");
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    public boolean saveIgnore(Context ctx, Record ignore) {
        if (getExistsIgnore(ctx, ignore.getString("user"), ignore.getString("target_type"), ignore.getString("target_id")))
            return false;
        final String SQL = "INSERT INTO ${table} ${values_join(alias, ignore, add)}";
        String sql = SQLTemplate.merge(SQL,
                "table", ignoreTable, "alias", ignoreSchema.getAllAliases(),
                "ignore", ignore, "add", Record.of("created_time", DateUtils.nowMillis()));
        SQLExecutor se = getSqlExecutor();
        try {
            long n = se.executeUpdate(sql);
            return n > 0;
        } catch (ServerException e) {
            return false;
        }
    }

    public boolean createIgnore0(Context ctx, String userId, String targetType, String targetId) {

        if (!userId.equals("0") && !targetType.equals("0") && !targetId.equals("0")) {
            Record ignore = Record.of("user", userId, "target_type", targetType, "target_id", targetId);
            return saveIgnore(ctx, ignore);
        } else {
            return false;
        }

    }

    @Override
    public boolean deleteIgnore(Context ctx, String userId, String targetType, String targetId) {
        final String METHOD = "deleteIgnore";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, targetType, targetId);

        String sql = "DELETE FROM ignore_ WHERE user='" + userId + "'" +
                " AND target_type='" + targetType + "' and target_id='" + targetId + "'";
        SQLExecutor se = getSqlExecutor();
        L.op(ctx, "deleteIgnore");
        long n = se.executeUpdate(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public boolean getExistsIgnore(Context ctx, String userId, String targetType, String targetId) {
        final String METHOD = "getExistsIgnore";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, targetType, targetId);
        String sql = "select count(user) as count1 FROM ignore_ WHERE user='" + userId + "'" +
                " AND target_type='" + targetType + "' and target_id='" + targetId + "'";
        SQLExecutor se = getSqlExecutor();
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return ((Number) se.executeScalar(sql)).intValue() > 0;
    }

    public RecordSet getIgnoreList(Context ctx, String userId, String target_type, int page, int count) {

        IgnoreLogic ignore = GlobalLogics.getIgnore();
        CommentLogic comment = GlobalLogics.getComment();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        StreamLogic streamLogic = GlobalLogics.getStream();

        RecordSet recs = getIgnoreList0(ctx, userId, target_type, page, count);
        RecordSet out_recs = new RecordSet();
        if (recs.size() > 0) {
            if (target_type.equals(String.valueOf(Constants.IGNORE_STREAM))) {
                RecordSet rs = streamLogic.getPosts(ctx, recs.joinColumnValues("target_id", ","), Constants.POST_FULL_COLUMNS);
                out_recs = streamLogic.transTimelineForQiupuP(ctx, userId, rs, 2, 5, false);
            }

            if (target_type.equals(String.valueOf(Constants.IGNORE_COMMENT))) {
                RecordSet rs = comment.getComments(ctx, recs.joinColumnValues("target_id", ","), Constants.FULL_COMMENT_COLUMNS);
                out_recs = transComment(ctx, userId, rs);
            }

            if (target_type.equals(String.valueOf(Constants.IGNORE_USER))) {
                RecordSet rs = accountLogic.getUsers(ctx, userId, recs.joinColumnValues("target_id", ","), AccountLogic.USER_FULL_COLUMNS);
                out_recs = rs;
            }
        }
        return out_recs;

    }

    public RecordSet transComment(Context ctx, String viewerId, RecordSet commentDs) {
        if (commentDs.size() > 0) {
            AccountLogic accountLogic = GlobalLogics.getAccount();
            LikeLogic l = GlobalLogics.getLike();
            for (Record r : commentDs) {
                r.put("image_url", accountLogic.getUser(ctx, r.getString("commenter"), r.getString("commenter"), "image_url").getString("image_url"));
                Record likes = new Record();
                likes.put("count", l.getLikeCount(ctx, String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id")));
                RecordSet lu = l.loadLikedUsers(ctx, String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id"), 0, 500);
                if (lu.size() > 0) {
                    String uid = lu.joinColumnValues("liker", ",");
                    likes.put("users", accountLogic.getUsers(ctx, viewerId, uid, AccountLogic.USER_LIGHT_COLUMNS_LIGHT));
                } else {
                    likes.put("users", new RecordSet());
                }
                r.put("likes", likes);

                if (!viewerId.equals("")) {
                    r.put("iliked", l.ifUserLiked(ctx, viewerId, String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id")));
                } else {
                    r.put("iliked", false);
                }
                if (r.getString("add_to").trim().length() > 0) {
                    RecordSet recs = accountLogic.getUsers(ctx, viewerId, r.getString("add_to"), AccountLogic.USER_LIGHT_COLUMNS_LIGHT);
                    r.put("add_new_users", recs.toString());
                } else {
                    r.put("add_new_users", new RecordSet());
                }
            }
        }
        return commentDs;
    }

    public RecordSet getIgnoreList0(Context ctx, String user_id, String target_type, int page, int count) {
        final String METHOD = "getIgnoreList";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, user_id, target_type, page, count);
        String sql = "select * FROM ignore_ WHERE user='" + user_id + "'";
        if (!target_type.equals("") && !target_type.equals("0"))
            sql += " AND target_type='" + target_type + "'";
        sql += " order by created_time desc ";
        if (count > 0)
            sql += " " + SQLUtils.pageToLimit(page, count) + "";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public boolean createIgnore(Context ctx, String userId, String target_type, String targetIds) {
        final String METHOD = "createIgnore";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, target_type, targetIds);
        List<String> targetIds0 = StringUtils2.splitList(targetIds, ",", true);
        L.op(ctx, "createIgnore");
        for (String target_id : targetIds0) {
            createIgnore0(ctx, userId, target_type, target_id);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;

    }

    @Override
    public String formatIgnoreUsers(Context ctx, String viewerId, String userIds) {
        final String METHOD = "formatIgnoreUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userIds);
        List<String> user_ignore = new ArrayList<String>();
        List<String> user_old = StringUtils2.splitList(userIds, ",", true);
        if (!viewerId.equals("") && !viewerId.equals("0")) {
            RecordSet recs_ignore = getIgnoreListSimpleP(ctx, viewerId, String.valueOf(Constants.IGNORE_USER), 0, 1000);
            if (recs_ignore.size() > 0) {
                for (Record r : recs_ignore) {
                    user_ignore.add(r.getString("target_id"));
                }
            }
        }
        for (int i = user_old.size() - 1; i >= 0; i--) {
            if (user_ignore.contains(user_old.get(i)))
                user_old.remove(i);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return StringUtils.join(user_old, ",");
    }

    @Override
    public RecordSet getIgnoreListSimpleP(Context ctx, String userId, String target_type, int page, int count) {
        RecordSet recs = getIgnoreList(ctx, userId, target_type, page, count);
        return recs;
    }

    public RecordSet formatIgnoreStreamOrCommentsP(Context ctx, String viewerId, String sORc, RecordSet recs) {
        if (!viewerId.equals("") && !viewerId.equals("0")) {
            List<String> stream_ignore = new ArrayList<String>();
            List<String> user_ignore_list = new ArrayList<String>();
            String target_type = sORc.equals("stream") ? String.valueOf(Constants.IGNORE_STREAM) : String.valueOf(Constants.IGNORE_COMMENT);
            String column_name = sORc.equals("stream") ? "post_id" : "comment_id";
            String source_name = sORc.equals("stream") ? "source" : "commenter";
            RecordSet recs_ignore = getIgnoreListSimpleP(ctx, viewerId, target_type, 0, 1000);
            RecordSet recs_user_ignore = getIgnoreListSimpleP(ctx, viewerId, String.valueOf(Constants.IGNORE_USER), 0, 1000);
            if (recs_ignore.size() > 0) {
                for (Record r : recs_ignore) {
                    stream_ignore.add(r.getString("post_id"));
                }
            }

            if (recs_user_ignore.size() > 0) {
                for (Record r : recs_user_ignore) {
                    user_ignore_list.add(r.getString("user_id"));
                }
            }

            for (int i = recs.size() - 1; i >= 0; i--) {
                if (stream_ignore.contains(recs.get(i).getString(column_name)) || user_ignore_list.contains(recs.get(i).getString(source_name))) {
                    recs.remove(i);
                }
            }
        }
        return recs;
    }

    @Override
    public List<Long> formatIgnoreUserListP(Context ctx, List<Long> userList, String post_id, String comment_id) {
        final String METHOD = "formatIgnoreUserListP";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userList, post_id, comment_id);
        //先看这个人有没有ignore我
        for (int i = userList.size() - 1; i >= 0; i--) {
            if (getExistsIgnore(ctx, String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_USER), ctx.getViewerIdString()))
                userList.remove(i);
        }

        if (!post_id.equals("")) {
            //看看那个人有没有ignore这个stream ,如果有，把那个人干掉
            //看看发stream的是谁
//                String source_user = RecordSet.fromByteBuffer(stream.getPosts(post_id, "source")).getFirstRecord().getString("source");
            for (int i = userList.size() - 1; i >= 0; i--) {
                if (getExistsIgnore(ctx, String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_STREAM), post_id))
                    userList.remove(i);
            }
//                if (!source_user.equals("")) {
//                    for (int i = userList.size() - 1; i >= 0; i--) {
//                        if (ignore.getExistsIgnore(String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_STREAM), post_id))
//                            userList.remove(i);
//                    }
//                }
        }

        if (!comment_id.equals("")) {
            //看看发comment的是谁
//                String commenter_user = RecordSet.fromByteBuffer(comment.getComments(comment_id, "commenter")).getFirstRecord().getString("commenter");
            for (int i = userList.size() - 1; i >= 0; i--) {
                if (getExistsIgnore(ctx, String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_COMMENT), comment_id))
                    userList.remove(i);
            }
//                if (!comment_id.equals("")) {
//                    for (int i = userList.size() - 1; i >= 0; i--) {
//                        if (ignore.getExistsIgnore(String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_COMMENT), comment_id))
//                            userList.remove(i);
//                    }
//                }
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return userList;
    }
}

