package com.borqs.server.impl.comment;


import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Values;
import com.borqs.server.base.sql.Sql;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.feature.comment.Comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.base.sql.Sql.value;
import static com.borqs.server.feature.comment.Comment.rsToCommentList;

public class CommentDB extends ConfigurableBase {

    private String db;
    private Connection con;
    private String CommentTable;

    public void init() throws SQLException {
        Configuration conf = getConfig();
        this.db = conf.getString("account.simple.db", null);
        this.con = DriverManager.getConnection(db);
        this.CommentTable = conf.getString("stream.simple.commentNewTable", "comment_new");
    }

    public void destroy() {
        db = null;
        con = null;
    }

    public Comment createComment(Context ctx,Comment comment) throws SQLException {
        try {
            final String sql = new Sql().insertInto(CommentTable).values(
                    value("comment_id", Values.toString(comment.getCommentId())),
                    value("target_type", Values.toInt(comment.getTargetType())),
                    value("target_id", Values.toString(comment.getTargetId())),
                    value("created_time", Values.toInt(comment.getCreatedTime())),
                    value("destroyed_time", Values.toInt(comment.getDestroyedTime())),
                    value("from_", Values.toString(comment.getFrom())),
                    value("message", Values.toString(comment.getMessage())),
                    value("device", Values.toString(comment.getDevice())),
                    value("location", Values.toString(comment.getLocation())),
                    value("message", Values.toString(comment.getMessage())),
                    value("can_like", Values.toInt(comment.getCanLike()))
            ).toString();

            Statement stmt = con.createStatement();
            stmt.execute(sql);
        } catch (Exception e) {

        } finally {
            return comment;
        }
    }

    public Comment getComment(Context ctx,long commentId) throws SQLException {
        final String sql=new Sql().select("*").from(CommentTable).where("comment_id=" + commentId + " AND destroyed_time=0").toString();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Comment> commentList = new ArrayList<Comment>();
        commentList = rsToCommentList(rs);
        return commentList.get(0);
    }

    public List<Comment> getCommentsByIds(Context ctx,long[] commentIds) throws SQLException {
        final String sql=new Sql().select("*").from(CommentTable).where("comment_id IN ("+StringUtils2.join(",",commentIds)+") AND destroyed_time=0").orderBy("created_time desc").toString();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Comment> pl = new ArrayList<Comment>();
        pl = rsToCommentList(rs);
        return pl;
    }

    public List<Comment> getCommentsByTargetId(Context ctx,int targetType,String targetId,int page,int count) throws SQLException {
        final String sql=new Sql().select("*").from(CommentTable).where("target_type="+targetType+" AND TARGET_ID='"+targetId+"' AND destroyed_time=0").orderBy("created_time desc").limit(page*count,count).toString();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Comment> pl = new ArrayList<Comment>();
        pl = rsToCommentList(rs);
        return pl;
    }

    public boolean destroyComment(Context ctx,long[] commentIds) throws SQLException {
        final String sql=new Sql().update(CommentTable).setValues(value("destroyed_time", DateUtils.nowMillis()))
                .where("comment_id IN ("+StringUtils2.join(",",commentIds)+")")
                .toString();
        Statement stmt = con.createStatement();
        return stmt.execute(sql);
    }
}

