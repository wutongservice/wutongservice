package com.borqs.server.feature.comment;


import com.borqs.server.base.context.Context;

import java.util.List;

public interface CommentLogic {
    Comment createComment(Context ctx, Comment post);
    boolean destroyComment(Context ctx, long commentId);
    List<Comment> getCommentsByIds(Context ctx, long[] commentIds);
    List<Comment> getCommentsByTargetId(Context ctx, int targetType,String targetId);
    Comment getComment(Context ctx, long commentId);
}
