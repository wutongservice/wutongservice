package com.borqs.server.feature.stream;


import com.borqs.server.base.util.StringUtils2;
import net.sf.json.JSONObject;
import org.codehaus.jackson.JsonNode;
import org.codehaus.plexus.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
public class Post {
    private long postId;
    private long from;
    private long[] to;
    private long createdTime;
    private long destroyedTime;
    private long reference;
    private boolean privacy;
    private int app;
    private int type;
    private String message;
    private String[] targets;
    private String device;
    private String location;
    private boolean canComment;
    private boolean canLike;
    private boolean canFavorite;
    private boolean canReshare;

    public Post() {
        this(0);
    }

    public Post(long postId) {
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

    public long[] getTo() {
        return to;
    }

    public void setTo(long[] to) {
        this.to = to;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getDestroyedTime() {
        return destroyedTime;
    }

    public void setDestroyedTime(long destroyedTime) {
        this.destroyedTime = destroyedTime;
    }

    public long getReference() {
        return reference;
    }

    public void setReference(long reference) {
        this.reference = reference;
    }

    public boolean isPrivacy() {
        return privacy;
    }

    public void setPrivacy(boolean privacy) {
        this.privacy = privacy;
    }

    public int getApp() {
        return app;
    }

    public void setApp(int app) {
        this.app = app;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String[] getTargets() {
        return targets;
    }

    public void setTargets(String[] targets) {
        this.targets = targets;
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

    public boolean isCanComment() {
        return canComment;
    }

    public void setCanComment(boolean canComment) {
        this.canComment = canComment;
    }

    public boolean isCanLike() {
        return canLike;
    }

    public void setCanLike(boolean canLike) {
        this.canLike = canLike;
    }

    public boolean isCanFavorite() {
        return canFavorite;
    }

    public void setCanFavorite(boolean canFavorite) {
        this.canFavorite = canFavorite;
    }

    public boolean isCanReshare() {
        return canReshare;
    }

    public void setCanReshare(boolean canReshare) {
        this.canReshare = canReshare;
    }


    public static String postToJson(Post post) {
        JSONObject obj = new  JSONObject();

        obj.put("post_id", post.getPostId());
        obj.put("from_", post.getFrom());
        obj.put("created_time", post.getCreatedTime());
        obj.put("destroyed_time", post.getDestroyedTime());
        obj.put("ref_", post.getReference());

        long toL[] = post.getTo();
        String toStr = "";
        for (int i = 0; i < toL.length; i++) {
            toStr += String.valueOf(toL[i]) + ",";
        }
        toStr = StringUtils.stripEnd(toStr, ",");
        obj.put("to_", toStr);

        obj.put("privacy", post.isPrivacy() ? 1 : 0);
        obj.put("app", post.getApp());
        obj.put("type", post.getType());
        obj.put("message", post.getMessage());

        String[] tar = post.getTargets();
        String tarStr = "";
        for (int i = 0; i < tar.length; i++) {
            tarStr += tar[i] + ",";
        }
        tarStr = StringUtils.stripEnd(tarStr, ",");
        obj.put("targets", tarStr);

        obj.put("device", post.getDevice());
        obj.put("location", post.getLocation());
        obj.put("can_comment", post.isCanComment() ? 1 : 0);
        obj.put("can_favorite", post.isCanFavorite() ? 1 : 0);
        obj.put("can_reshare", post.isCanReshare() ? 1 : 0);
        obj.put("can_like", post.isCanLike() ? 1 : 0);

        return obj.toString();
    }

    public static Post JsonToPost(JsonNode obj) {
        Post post = new Post();
        post.setPostId(obj.get(0).get("post_id") != null ? Long.parseLong(obj.get(0).get("post_id").toString()) : 0);
        post.setFrom(obj.get(0).get("from_") != null ? Long.parseLong(obj.get(0).get("from_").toString())  : 0);
        post.setCreatedTime(obj.get(0).get("created_time") != null ? Long.parseLong(obj.get(0).get("created_time").toString()) : 0);
        post.setDestroyedTime(obj.get(0).get("destroyed_time") != null ? Long.parseLong(obj.get(0).get("destroyed_time").toString()) : 0);
        post.setReference(obj.get(0).get("ref_") != null ? Long.parseLong(obj.get(0).get("ref_").toString()) : 0);

        String toStr = obj.get(0).get("to_") != null ? obj.get(0).get("to_").toString() : "";
        long[] intTo = new long[0];
        if (!toStr.equals("")) {
            String[] to = StringUtils2.splitArray(toStr, ",", true);
            intTo = new long[to.length];
            for (int i = 0; i < to.length; i++) {
                intTo[i] = Long.valueOf(to[i]);
            }
        }
        post.setTo(intTo);

        post.setPrivacy(obj.get(0).get("privacy") != null ? (Long.parseLong(obj.get(0).get("privacy").toString()) == 0 ? false : true) : false);
        post.setApp(obj.get(0).get("app") != null ? Integer.valueOf(obj.get(0).get("app").toString()) : 1);
        post.setType(obj.get(0).get("type") != null ? Integer.valueOf(obj.get(0).get("type").toString()) : 1);
        post.setMessage(obj.get(0).get("type") != null ? obj.get(0).get("message").toString() : "");

        String[] tar = StringUtils2.splitArray(obj.get(0).get("targets").toString() != null ? obj.get(0).get("targets").toString() : "", ",", true);
        post.setTargets(tar);

        post.setDevice(obj.get(0).get("device").toString() != null ? obj.get(0).get("device").toString() : "");
        post.setLocation(obj.get(0).get("location").toString() != null ? obj.get(0).get("location").toString() : "");
        post.setCanComment(obj.get(0).get("can_comment") != null ? (Long.parseLong(obj.get(0).get("can_comment").toString()) == 0 ? false : true) : true);
        post.setCanFavorite(obj.get(0).get("can_like") != null ? (Long.parseLong(obj.get(0).get("can_like").toString()) == 0 ? false : true) : true);
        post.setCanReshare(obj.get(0).get("can_favorite") != null ? (Long.parseLong(obj.get(0).get("can_favorite").toString()) == 0 ? false : true) : true);
        post.setCanLike(obj.get(0).get("can_reshare") != null ? (Long.parseLong(obj.get(0).get("can_reshare").toString()) == 0 ? false : true) : true);

        return post;
    }

    public static Post rsToPost(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setPostId((int) rs.getInt("post_id"));
        post.setFrom((int) rs.getInt("from_"));
        post.setCreatedTime((int) rs.getInt("created_time"));
        post.setDestroyedTime((int) rs.getInt("destroyed_time"));
        post.setReference((int) rs.getInt("ref_"));

        String toStr = rs.getString("to_");
        String[] to = StringUtils2.splitArray(toStr, ",", true);
        long[] intTo = new long[to.length];
        for (int i = 0; i < to.length; i++) {
            intTo[i] = Long.valueOf(to[i]);
        }
        post.setTo(intTo);

        post.setPrivacy((int) rs.getInt("privacy") == 0 ? false : true);
        post.setApp((int) rs.getInt("app"));
        post.setType((int) rs.getInt("type"));
        post.setMessage(rs.getString("message"));

        String[] tar = StringUtils2.splitArray(rs.getString("targets"), ",", true);
        post.setTargets(tar);

        post.setDevice(rs.getString("device"));
        post.setLocation(rs.getString("location"));
        post.setCanComment((int) rs.getInt("can_comment") == 0 ? false : true);
        post.setCanFavorite((int) rs.getInt("can_favorite") == 0 ? false : true);
        post.setCanReshare((int) rs.getInt("can_reshare") == 0 ? false : true);
        post.setCanLike((int) rs.getInt("can_like") == 0 ? false : true);

        return post;
    }
}
