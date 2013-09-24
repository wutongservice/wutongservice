package com.borqs.server.platform.feature.comment;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.like.LikeExpansionSupport;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class CommentExpansionSupport {

    public static final String COL_COMMENTS = "comments";
    public static final String SUBCOL_COUNT = "count";
    public static final String SUBCOL_LATEST = "latest_comments";

    protected CommentLogic comment;
    protected FriendLogic friend;
    protected int lastCommentCount = 2;

    protected CommentExpansionSupport() {
    }

    public CommentLogic getComment() {
        return comment;
    }

    public void setComment(CommentLogic comment) {
        this.comment = comment;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    

    protected static final String[] COMMENT_COLUMNS = ArrayHelper.merge(Comment.FULL_COLUMNS, new String[]{
            LikeExpansionSupport.COL_LIKED,
            LikeExpansionSupport.COL_LIKES,
    });

    protected Map<Target, String> expandCommentsHelper(Context ctx, Target[] targets) {
        final Map<Target, Integer> counts = comment.getCommentCounts(ctx, targets);
        final Map<Target, Comment[]> comments = comment.getCommentsOnTarget(ctx, COMMENT_COLUMNS, Page.of(lastCommentCount), targets);

        HashMap<Target, String> m = new HashMap<Target, String>();
        for (final Target target : targets) {
            String json = JsonHelper.toJson(new JsonGenerateHandler() {
                @Override
                public void generate(JsonGenerator jg, Object arg) throws IOException {
                    long count = MapUtils.getInteger(counts, target, 0);
                    Comment[] comments0 = comments.get(target);

                    jg.writeStartObject();
                    jg.writeNumberField(SUBCOL_COUNT, count);
                    jg.writeFieldName(SUBCOL_LATEST);
                    jg.writeStartArray();
                    if (ArrayUtils.isNotEmpty(comments0)) {
                        for (Comment comment : comments0) {
                            if (comment != null)
                                comment.serialize(jg, COMMENT_COLUMNS);
                        }
                    }
                    jg.writeEndArray();
                    jg.writeEndObject();
                }
            }, true);

            m.put(target, json);
        }
        return m;
    }

}
