package com.borqs.server.feature.conversation;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Conversation {
    private long postId;
    private long from;
    private long createdTime;
    private long reason;

    public Conversation() {
        this(0);
    }

    public Conversation(long postId) {
        this.postId = postId;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getReason() {
        return reason;
    }

    public void setReason(long reason) {
        this.reason = reason;
    }


    public static String conversationToJsonArray(List<Conversation> conversation) {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < conversation.size(); i++) {
            JSONObject obj = new JSONObject();
            obj.put("post_id", conversation.get(i).getPostId());
            obj.put("from_", conversation.get(i).getFrom());
            obj.put("created_time", conversation.get(i).getCreatedTime());
            obj.put("reason", conversation.get(i).getReason());
            jsonArray.add(obj);
        }
        return jsonArray.toString();
    }

    public static List<Conversation> JsonArrayToConversationList(JSONArray objArray) {
        List<Conversation> lc = new ArrayList<Conversation>();
        for (int i = 0; i < objArray.size(); i++) {
            JSONObject obj = (JSONObject) objArray.get(i);
            Conversation c = new Conversation();
            c.setPostId(obj.get("post_id") != null ? Long.parseLong(obj.get("post_id").toString()) : 0);
            c.setFrom(obj.get("from_") != null ? Long.parseLong(obj.get("from_").toString()) : 0);
            c.setCreatedTime(obj.get("created_time") != null ? Long.parseLong(obj.get("created_time").toString()) : 0);
            c.setReason(obj.get("reason") != null ? Long.parseLong(obj.get("reason").toString()) : 0);
            lc.add(c);
        }
        return lc;
    }

    public static List<Conversation> rsToConversationList(ResultSet rs) throws SQLException {
        List<Conversation> lc = new ArrayList<Conversation>();
        while (rs.next()) {
            Conversation c = new Conversation();
            c.setPostId((int) rs.getInt("post_id"));
            c.setFrom((int) rs.getInt("from_"));
            c.setCreatedTime((int) rs.getInt("created_time"));
            c.setReason((int) rs.getInt("reason"));
            lc.add(c);
        }

        return lc;
    }

}
