package com.borqs.server.platform.feature.comment;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.expansion.Expander;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.Conversationable;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface CommentLogic extends Logic, Expander<Comments> {

    Comment createComment(Context ctx, Comment comment);

    boolean destroyComment(Context ctx, long commentId);

    boolean updateComment(Context ctx, Comment comment);

    Map<Target, Integer> getCommentCounts(Context ctx, Target... targets);

    int getCommentCount(Context ctx, Target target);

    Map<Target, Comment[]> getCommentsOnTarget(Context ctx, String[] expCols, Page page, Target... targets);

    Comments getCommentsOnTarget(Context ctx, String[] expCols, Page page, Target target);

    Comments getComments(Context ctx, String[] expCols, long... commentIds);

    Comment getComment(Context ctx, String[] expCols, long commentId);

    //List<Comment> getCommentedPost(Context ctx, long userId, int page, int count, int objectType);

    long[] getUsersOnTarget(Context ctx, Target target, Page page);

    // List<Comment> getObjectCommentByUsers(Context ctx, int objectType, Page page, long... userIds);

    // boolean getIHasCommented(Context ctx, long commenter, int object);

    String[] getTargetIdsOrderByCommentCount(Context ctx, int targetType, boolean asc, Page page);

    Comment expand(Context ctx, String[] expCols, Comment comment);
}
