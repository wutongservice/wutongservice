package com.borqs.server.impl.comment;


import com.borqs.server.base.memcache.XMemcached;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.feature.comment.Comment;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.feature.comment.Comment.*;

public class CommentCache {

    XMemcached mc = new XMemcached();

    public void init() {
        mc.init();
    }

    public void destroy() {
        mc.destroy();
    }

    public String commentKey(String key) {
        return "c" + key;
    }
    public String targetCommentKey(int targetType,String targetId) {
        return "n" + targetType + "-" + targetId;
    }

    public Comment createCommentCache(Comment comment) {
        mc.deleteCache(commentKey(String.valueOf(comment.getCommentId())));
        mc.writeCache(commentKey(String.valueOf(comment.getCommentId())), commentToJson(comment));

        String cache = mc.readCache(targetCommentKey(comment.getTargetType(), comment.getTargetId()));
        if (cache.length() > 0) {
            JSONArray jsonArray = JsonUtils.fromJson(cache, JSONArray.class);
            List<Comment> lc = JsonArrayToCommentList(jsonArray);
            lc.add(comment);
            String newCache = commentsToJsonArray(lc);
            mc.writeCache(targetCommentKey(comment.getTargetType(), comment.getTargetId()), newCache);
        } else {
            List<Comment> lc = new ArrayList<Comment>();
            lc.add(comment);
            mc.writeCache(targetCommentKey(comment.getTargetType(), comment.getTargetId()), commentsToJsonArray(lc));
        }
        return comment;
    }

    public Comment getCommentCache(long commentId) {
        String commentKey = commentKey(String.valueOf(commentId));
        String cache = mc.readCache(commentKey);
        Comment comment = null;
        if (cache.length() > 0) {
            comment = JsonToComment(JsonUtils.fromJson(cache, JSONObject.class));
        }
        return comment;
    }

    public List<Comment> getTargetCache(int targetType, String targetId) {
        String cache = mc.readCache(targetCommentKey(targetType, targetId));
        List<Comment> comments = null;
        JSONArray jsonArray = JsonUtils.fromJson(cache, JSONArray.class);
        List<Comment> lc = JsonArrayToCommentList(jsonArray);
        return lc;
    }

    public List<Comment> getComments(long[] commentIds) {
        List<Comment> commentList = new ArrayList<Comment>();
        for (long commentId : commentIds) {
            Comment c = getCommentCache(commentId);
            if (c != null) {
                commentList.add(c);
            }
        }
        return commentList;
    }

    public boolean destroyComment(long[] commentIds) {
        boolean b = true;
        for (long commentId : commentIds) {
            String c = mc.readCache(commentKey(String.valueOf(commentId)));
            if (c.length() > 0) {
                JSONObject obj = JsonUtils.fromJson(c, JSONObject.class);
                Comment comment = JsonToComment(obj);

                String cache = mc.readCache(targetCommentKey(comment.getTargetType(), comment.getTargetId()));
                JSONArray jsonArray = JsonUtils.fromJson(cache, JSONArray.class);
                List<Comment> lc = JsonArrayToCommentList(jsonArray);
                for (int i = lc.size() - 1; i >= 0; i--) {
                    if (lc.get(i).getCommentId() == commentId) {
                        lc.remove(i);
                    }
                }
                mc.deleteCache(targetCommentKey(comment.getTargetType(), comment.getTargetId()));
                if (lc.size() > 0) {
                    mc.writeCache(targetCommentKey(comment.getTargetType(), comment.getTargetId()), commentsToJsonArray(lc));
                }
            }

            mc.deleteCache(commentKey(String.valueOf(commentId)));
        }
        return b;
    }
}

