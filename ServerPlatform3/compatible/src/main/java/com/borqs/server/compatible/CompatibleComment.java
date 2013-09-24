package com.borqs.server.compatible;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.CommentExpansionSupport;
import com.borqs.server.platform.feature.comment.Comments;
import com.borqs.server.platform.feature.like.LikeCommentExpansion;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.LinkedHashSet;

public class CompatibleComment {

    public static final String V1COL_COMMENT_ID = "comment_id";
    public static final String V1COL_TARGET = "target";
    public static final String V1COL_CREATED_TIME = "created_time";
    public static final String V1COL_COMMENTER = "commenter";
    public static final String V1COL_COMMENTER_NAME = "commenter_name";
    public static final String V1COL_MESSAGE = "message";
    public static final String V1COL_DEVICE = "device";
    public static final String V1COL_CAN_LIKE = "can_like";
    public static final String V1COL_DESTROYED_TIME = "destroyed_time";
    public static final String V1COL_ADD_TO = "add_to";
    public static final String V1COL_LIKES = "likes";
    public static final String V1COL_LIKED = "iliked";
    public static final String V1COL_ADD_NEW_USERS = "add_new_users";

    public static final String[] V1_FULL_COLUMNS = {
            V1COL_COMMENT_ID,
            V1COL_TARGET,
            V1COL_CREATED_TIME,
            V1COL_COMMENTER,
            V1COL_COMMENTER_NAME,
            V1COL_MESSAGE,
            V1COL_DEVICE,
            V1COL_CAN_LIKE,
            V1COL_DESTROYED_TIME,
            V1COL_ADD_TO,
            V1COL_LIKES,
            V1COL_LIKED,
            V1COL_ADD_NEW_USERS,
    };

    public static String[] expandV1Columns(String[] v1Cols) {
        return ColumnsExpander.expand(v1Cols, "#full", V1_FULL_COLUMNS);
    }

    public static String[] v1ToV2Columns(String[] v1Cols) {
        LinkedHashSet<String> l = new LinkedHashSet<String>();
        if (ArrayUtils.contains(v1Cols, V1COL_COMMENT_ID))
            l.add(Comment.COL_COMMENT_ID);
        if (ArrayUtils.contains(v1Cols, V1COL_TARGET))
            l.add(Comment.COL_TARGET);
        if (ArrayUtils.contains(v1Cols, V1COL_CREATED_TIME))
            l.add(Comment.COL_CREATED_TIME);
        if (ArrayUtils.contains(v1Cols, V1COL_COMMENTER) || ArrayUtils.contains(v1Cols, V1COL_COMMENTER_NAME))
            l.add(Comment.COL_COMMENTER);
        if (ArrayUtils.contains(v1Cols, V1COL_MESSAGE))
            l.add(Comment.COL_MESSAGE);
        if (ArrayUtils.contains(v1Cols, V1COL_DEVICE))
            l.add(Comment.COL_DEVICE);
        if (ArrayUtils.contains(v1Cols, V1COL_CAN_LIKE))
            l.add(Comment.COL_CAN_LIKE);
        if (ArrayUtils.contains(v1Cols, V1COL_DESTROYED_TIME))
            l.add(Comment.COL_DESTROYED_TIME);
        if (ArrayUtils.contains(v1Cols, V1COL_ADD_TO))
            l.add(Comment.COL_ADD_TO);
        if (ArrayUtils.contains(v1Cols, V1COL_LIKES))
            l.add(LikeCommentExpansion.COL_LIKES);
        if (ArrayUtils.contains(v1Cols, V1COL_LIKED))
            l.add(LikeCommentExpansion.COL_LIKED);

        return l.toArray(new String[l.size()]);
    }

    public static String commentToJson(final Comment comment, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeComment(jg, comment, v1Cols);
            }
        }, human);
    }

    public static String commentsToJson(final Comments comments, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeComments(jg, comments, v1Cols);
            }
        }, human);
    }

    public static void serializeComment(JsonGenerator jg, Comment comment, String[] v1Cols) throws IOException {
        /*
        {
  "comment_id" : 2738635382909503219,
  "target" : "4:3",
  "created_time" : 1305883113341,
  "commenter" : 34,
  "commenter_name" : "xiaofei.luo@borqs.com",
  "message" : "2222",
  "device" : "",
  "can_like" : true,
  "destroyed_time" : 0,
  "add_to" : "",
  "comment_id_s" : "2738635382909503219",
  "likes" : {
    "count" : 0,
    "users" : [ ]
  },
  "iliked" : false,
  "add_new_users" : [ ]
}
         */
        jg.writeStartObject();
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_COMMENT_ID))
            jg.writeNumberField(V1COL_COMMENT_ID, comment.getCommentId());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CAN_LIKE))
            jg.writeBooleanField(V1COL_CAN_LIKE, comment.getCanLike());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_COMMENTER))
            jg.writeNumberField(V1COL_COMMENTER, comment.getCommenterId());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_COMMENTER_NAME)) {
            String commenterJson = comment.getStringAddon(Comment.COL_COMMENTER, null);
            if (commenterJson != null) {
                User commenter = User.fromJson(commenterJson);
                jg.writeStringField(V1COL_COMMENTER_NAME, commenter != null ? ObjectUtils.toString(commenter.getDisplayName()) : "");
            } else {
                jg.writeStringField(V1COL_COMMENTER_NAME, "");
            }
        }

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_DESTROYED_TIME))
            jg.writeNumberField(V1COL_DESTROYED_TIME, comment.getDestroyedTime());

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CREATED_TIME))
            jg.writeNumberField(V1COL_CREATED_TIME, comment.getCreatedTime());

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_DEVICE))
            jg.writeStringField(V1COL_DEVICE, ObjectUtils.toString(comment.getDevice()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_MESSAGE))
            jg.writeStringField(V1COL_MESSAGE, ObjectUtils.toString(comment.getMessage()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_TARGET))
            jg.writeObjectField(V1COL_TARGET, comment.getTarget() != null ? comment.getTarget().toCompatibleString() : "");

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ADD_TO))
            jg.writeStringField(V1COL_ADD_TO, comment.getAddTo() != null ? comment.getAddTo().toString() : "");

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ADD_NEW_USERS))
            comment.writeAddonJsonAs(jg, Comment.COL_ADD_TO, V1COL_ADD_NEW_USERS, USER_TRANSFORMER);

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LIKED))
            comment.writeAddonJsonAs(jg, LikeCommentExpansion.COL_LIKED, V1COL_LIKED);

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LIKES))
            comment.writeAddonJsonAs(jg, LikeCommentExpansion.COL_LIKES, V1COL_LIKES, CompatibleLike.LIKES_ADDON_TRANSFORMER);
        jg.writeEndObject();
    }

    public static void serializeComments(JsonGenerator jg, Comments comments, String[] v1Cols) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(comments)) {
            for (Comment comment : comments) {
                if (comment != null)
                    serializeComment(jg, comment, v1Cols);
            }
        }
        jg.writeEndArray();
    }

    private static final String[] USER_V1_COLUMNS = {
            CompatibleUser.V1COL_USER_ID,
            CompatibleUser.V1COL_DISPLAY_NAME,
            CompatibleUser.V1COL_IMAGE_URL,
    };

    private static final CompatibleUser.UserJsonTransformer USER_TRANSFORMER = new CompatibleUser.UserJsonTransformer(USER_V1_COLUMNS);

    public static String v2JsonNodeToV1Json(JsonNode jn, String[] v1Cols, boolean human) {
        if (jn.isArray()) {
            final Comments comments = Comments.fromJsonNode(null, jn);
            return commentsToJson(comments, v1Cols, human);
        } else if (jn.isObject()) {
            final Comment comment = Comment.fromJsonNode(jn);
            return commentToJson(comment, v1Cols, human);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static String v2ToV1Json(String json, String[] v1Cols, boolean human) {
        JsonNode jn = JsonHelper.parse(json);
        return v2JsonNodeToV1Json(jn, v1Cols, human);
    }

    public static final String V1SUBCOL_COUNT = "count";
    public static final String V1SUBCOL_LATEST_COMMENTS = "latest_comments";

    public static String v2ToV1CommentsAddonJson(String json) {
        final JsonNode jn = JsonHelper.parse(json);
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                jg.writeStartObject();
                jg.writeFieldName(V1SUBCOL_COUNT);
                jg.writeTree(jn.path(CommentExpansionSupport.SUBCOL_COUNT));
                jg.writeFieldName(V1SUBCOL_LATEST_COMMENTS);
                jg.writeRawValue(v2JsonNodeToV1Json(jn.path(CommentExpansionSupport.SUBCOL_LATEST), V1_FULL_COLUMNS, true));
                jg.writeEndObject();
            }
        }, true);
    }

    public static Addons.AddonValueTransformer COMMENTS_ADDON_TRANSFORMER = new CommentsAddonTransformer();

    private static class CommentsAddonTransformer implements Addons.AddonValueTransformer {
        @Override
        public Object transform(Object old) {
            String json = ObjectUtils.toString(old);
            return Addons.jsonAddonValue(v2ToV1CommentsAddonJson(json));

        }
    }

}
