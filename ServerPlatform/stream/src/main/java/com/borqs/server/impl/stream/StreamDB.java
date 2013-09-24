package com.borqs.server.impl.stream;


import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Values;
import com.borqs.server.base.sql.Sql;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.feature.stream.Post;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.base.sql.Sql.value;
import static com.borqs.server.feature.stream.Post.rsToPost;

public class StreamDB extends ConfigurableBase {

    private String db;
    private Connection con;
    private String StreamTable;

    public void init() throws SQLException {
        Configuration conf = getConfig();
        this.db = conf.getString("account.simple.db", null);
        this.con = DriverManager.getConnection(db);
        this.StreamTable = conf.getString("stream.simple.streamNewTable", "stream_new");
    }

    public void destroy() {
        db = null;
        con = null;
    }

    public Post createPost(Context ctx,Post post) throws SQLException {
        try {
            final String sql = new Sql().insertInto(StreamTable).values(
                    value("post_id", Values.toString(post.getPostId())),
                    value("from_", Values.toString(post.getFrom())),
                    value("created_time", Values.toInt(post.getCreatedTime())),
                    value("destroyed_time", Values.toInt(post.getDestroyedTime())),
                    value("ref_", Values.toString(post.getReference())),
                    value("to_", StringUtils2.join(",", post.getTo())),
                    value("privacy", Values.toInt(post.isPrivacy())),
                    value("app", Values.toInt(post.getApp())),
                    value("type", Values.toInt(post.getType())),
                    value("message", Values.toString(post.getMessage())),
                    value("targets", Values.toString(post.getTargets())),
                    value("device", Values.toString(post.getDevice())),
                    value("location", Values.toString(post.getLocation())),
                    value("can_comment", Values.toInt(post.isCanComment())),
                    value("can_like", Values.toInt(post.isCanLike())),
                    value("can_favorite", Values.toInt(post.isCanFavorite())),
                    value("can_reshare", Values.toInt(post.isCanReshare()))
            ).toString();

            Statement stmt = con.createStatement();
            stmt.execute(sql);
        } catch (Exception e) {

        } finally {
            return post;
        }
    }

    public Post getPost(Context ctx,long postId) throws SQLException {
        final String sql=new Sql().select("*").from(StreamTable).where("post_id=" + postId + " AND destroyed_time=0").toString();

        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        Post post = new Post();
        while (rs.next()) {
            post = rsToPost(rs);
        }
        return post;
    }

    public boolean updatePost(Context ctx,Post post) throws SQLException {
        final String sql=new Sql().update(StreamTable).setValues(
                value("can_comment", Values.toInt(post.isCanComment())),
                value("can_like", Values.toInt(post.isCanLike())),
                value("can_favorite", Values.toInt(post.isCanFavorite())),
                value("can_reshare", Values.toInt(post.isCanReshare()))).toString();
        Statement stmt = con.createStatement();
        return stmt.execute(sql);
    }

    public List<Post> getPosts(Context ctx,long[] postIds) throws SQLException {
        final String sql=new Sql().select("*").from(StreamTable).where("post_id IN ("+StringUtils2.join(",",postIds)+") AND destroyed_time=0").orderBy("created_time desc").toString();

        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Post> pl = new ArrayList<Post>();
        while (rs.next()) {
            pl.add(rsToPost(rs));
        }
        return pl;
    }

    public boolean destroyPost(Context ctx,long[] postIds) throws SQLException {
        final String sql=new Sql().update(StreamTable).setValues(value("destroyed_time", DateUtils.nowMillis()))
                .where("post_id IN ("+StringUtils2.join(",",postIds)+")")
                .toString();
        Statement stmt = con.createStatement();
        return stmt.execute(sql);
    }
}

