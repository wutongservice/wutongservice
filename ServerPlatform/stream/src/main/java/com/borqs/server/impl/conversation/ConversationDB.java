package com.borqs.server.impl.conversation;


import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.sql.Sql;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.feature.conversation.Conversation;

import java.sql.*;
import java.util.List;

import static com.borqs.server.base.sql.Sql.value;
import static com.borqs.server.feature.conversation.Conversation.rsToConversationList;

public class ConversationDB extends ConfigurableBase {

    private String db;
    private Connection con;
    private String ConversationTable;

    public void init() throws SQLException {
        Configuration conf = getConfig();
        this.db = conf.getString("account.simple.db", null);
        this.con = DriverManager.getConnection(db);
        this.ConversationTable = conf.getString("conversation.simple.conversationTable", "conversation");
    }

    public void destroy() {
        db = null;
        con = null;
    }

    public Conversation createConversation(Context ctx,long postId, long userId, int reason) throws SQLException {
        Conversation conversation = new Conversation();
        long created_time = DateUtils.nowMillis();

        try {
            final String sql = new Sql().insertIgnoreInto(ConversationTable).values(
                    value("post_id", postId),
                    value("from_", userId),
                    value("created_time", created_time),
                    value("reason", reason)
            ).toString();

            Statement stmt = con.createStatement();
            int n = stmt.executeUpdate(sql);
            if (n <= 0)
                stmt.executeUpdate(new Sql()
                        .update(ConversationTable)
                        .setValues(value("created_time", created_time))
                        .where("post_id=" + postId + " AND from_=" + userId + " AND reason=" + reason + "").toString());

            conversation.setReason(reason);
            conversation.setCreatedTime(created_time);
            conversation.setPostId(postId);
            conversation.setFrom(userId);

        } catch (Exception e) {
        } finally {
            return conversation;
        }
    }

    public List<Conversation> getConversationByPostIds(Context ctx,long[] postIds, long[] reason, int page, int count) throws SQLException {
        String sql = "";
        if (reason.length > 0) {
            sql = new Sql().select("*").from(ConversationTable).where("post_id IN ("+StringUtils2.join(",",postIds)+") AND reason IN (" + StringUtils2.join(",", reason) + ")").orderBy("created_time desc").limit(page * count, count).toString();
        } else {
            sql = new Sql().select("*").from(ConversationTable).where("post_id IN ("+StringUtils2.join(",",postIds)+")").orderBy("created_time desc").limit(page * count, count).toString();
        }

        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Conversation> lc = rsToConversationList(rs);
        return lc;
    }

    public List<Conversation> getConversationByUser(Context ctx,long userId, long[] reason,int page,int count) throws SQLException {
        String sql = "";
        if (reason.length > 0) {
            sql = new Sql().select("*").from(ConversationTable).where("from_=" + userId + " AND reason IN (" + StringUtils2.join(",", reason) + ")").orderBy("created_time desc").limit(page*count,count).toString();
        } else {
            sql = new Sql().select("*").from(ConversationTable).where("from_=" + userId + "").orderBy("created_time desc").limit(page*count,count).toString();
        }

        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Conversation> lc = rsToConversationList(rs);
        return lc;
    }


    public boolean destroyConversation(Context ctx,long postId,long userId,long reason) throws SQLException {
       String sql = "";
        if (reason > 0) {
            sql = new Sql().deleteFrom(ConversationTable).where("post_id=" + postId + " AND from_="+userId+" AND reason=" + reason).toString();
        } else {
            sql = new Sql().deleteFrom(ConversationTable).where("post_id=" + postId + " AND from_=" + userId + "").toString();
        }

        Statement stmt = con.createStatement();
        return stmt.execute(sql);
    }
}

