package com.borqs.server.feature.comment;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Comment {
    private long commentId;
    private int targetType;
    private String targetId;
    private long createdTime;
    private long destroyedTime;
    private long from;
    private String message;
    private String device;
    private String location;
    private int canLike;

    public long getDestroyedTime() {
        return destroyedTime;
    }

    public void setDestroyedTime(long destroyedTime) {
        this.destroyedTime = destroyedTime;
    }

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    public int getTargetType() {
        return targetType;
    }

    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCanLike() {
        return canLike;
    }

    public void setCanLike(int canLike) {
        this.canLike = canLike;
    }

    public Comment() {
        this(0);
    }

    public Comment(long commentId) {
        this.commentId = commentId;
    }

    public static String commentsToJsonArray(List<Comment> comments) {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < comments.size(); i++) {
            JSONObject obj = new JSONObject();
            obj.put("comment_id", comments.get(i).getCommentId());
            obj.put("target_type", comments.get(i).getTargetType());
            obj.put("target_id", comments.get(i).getTargetId());
            obj.put("created_time", comments.get(i).getCreatedTime());
            obj.put("destroyed_time", comments.get(i).getDestroyedTime());
            obj.put("from_", comments.get(i).getFrom());
            obj.put("message", comments.get(i).getMessage());
            obj.put("device", comments.get(i).getDevice());
            obj.put("location", comments.get(i).getLocation());
            obj.put("can_like", comments.get(i).getCanLike());
            jsonArray.add(obj);
        }
        return jsonArray.toString();
    }

    public static String commentToJson(Comment comment) {
        JSONObject obj = new JSONObject();
        obj.put("comment_id", comment.getCommentId());
        obj.put("target_type", comment.getTargetType());
        obj.put("target_id", comment.getTargetId());
        obj.put("created_time", comment.getCreatedTime());
        obj.put("destroyed_time", comment.getDestroyedTime());
        obj.put("from_", comment.getFrom());
        obj.put("message", comment.getMessage());
        obj.put("device", comment.getDevice());
        obj.put("location", comment.getLocation());
        obj.put("can_like", comment.getCanLike());
        return obj.toString();
    }

    public static Comment JsonToComment(JSONObject obj) {
        Comment c = new Comment();
        c.setCommentId(obj.get("comment_id") != null ? Long.parseLong(obj.get("comment_id").toString()) : 0);
        c.setTargetType(obj.get("target_type") != null ? Integer.parseInt(obj.get("target_type").toString()) : 0);
        c.setTargetId(obj.get("target_id") != null ? obj.get("target_id").toString() : "");
        c.setFrom(obj.get("from_") != null ? Long.parseLong(obj.get("from_").toString()) : 0);
        c.setCreatedTime(obj.get("created_time") != null ? Long.parseLong(obj.get("created_time").toString()) : 0);
        c.setDestroyedTime(obj.get("destroyed_time") != null ? Long.parseLong(obj.get("destroyed_time").toString()) : 0);
        c.setMessage(obj.get("message") != null ? obj.get("message").toString() : "");
        c.setDevice(obj.get("device") != null ? obj.get("device").toString() : "");
        c.setLocation(obj.get("location") != null ? obj.get("location").toString() : "");
        c.setCanLike(obj.get("can_like") != null ? Integer.parseInt(obj.get("can_like").toString()) : 0);

        return c;
    }

    public static List<Comment> JsonArrayToCommentList(JSONArray objArray) {
        List<Comment> lc = new ArrayList<Comment>();
        for (int i = 0; i < objArray.size(); i++) {
            JSONObject obj = (JSONObject) objArray.get(i);
            Comment c = new Comment();
            c.setCommentId(obj.get("comment_id") != null ? Long.parseLong(obj.get("comment_id").toString()) : 0);
            c.setTargetType(obj.get("target_type") != null ? Integer.parseInt(obj.get("target_type").toString()) : 0);
            c.setTargetId(obj.get("target_id") != null ? obj.get("target_id").toString() : "");
            c.setFrom(obj.get("from_") != null ? Long.parseLong(obj.get("from_").toString()) : 0);
            c.setCreatedTime(obj.get("created_time") != null ? Long.parseLong(obj.get("created_time").toString()) : 0);
            c.setDestroyedTime(obj.get("destroyed_time") != null ? Long.parseLong(obj.get("destroyed_time").toString()) : 0);
            c.setMessage(obj.get("message") != null ? obj.get("message").toString() : "");
            c.setDevice(obj.get("device") != null ? obj.get("device").toString() : "");
            c.setLocation(obj.get("location") != null ? obj.get("location").toString() : "");
            c.setCanLike(obj.get("can_like") != null ? Integer.parseInt(obj.get("can_like").toString()) : 0);

            lc.add(c);
        }
        return lc;
    }

    public static List<Comment> rsToCommentList(ResultSet rs) throws SQLException {
        List<Comment> lc = new ArrayList<Comment>();
        while (rs.next()) {
            Comment c = new Comment();
            c.setCommentId(rs.getInt("comment_id"));
            c.setTargetType(rs.getInt("target_type"));
            c.setTargetId(rs.getString("target_id"));
            c.setFrom(rs.getInt("from_"));
            c.setCreatedTime(rs.getInt("created_time"));
            c.setDestroyedTime(rs.getInt("destroyed_time"));
            c.setMessage(rs.getString("message"));
            c.setDevice(rs.getString("device"));
            c.setLocation(rs.getString("location"));
            c.setCanLike(rs.getInt("can_like"));
            lc.add(c);
        }
        return lc;
    }

}
