package com.borqs.server.wutong.comment;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

import java.util.List;
import java.util.Map;

public interface CommentLogic {
    String createComment(Context ctx,String userId, String targetId, Record comment) ;
    Record destroyComments(Context ctx,String userId, String commentId,String fromSource,String objectType) ;

    int getCommentCount(Context ctx,String viewerId,String targetId) ;
    RecordSet getCommentsFor(Context ctx,String targetId, String cols, boolean asc, int page, int count) ;
    RecordSet getCommentsForContainsIgnore(Context ctx,String viewerId, String targetId, String cols, boolean asc, int page, int count) ;
    RecordSet getComments(Context ctx,String commentIds, String cols) ;
    RecordSet getCommentsAll(Context ctx,String commentIds, String cols) ;
    RecordSet getCommentedPost(Context ctx,String userId, int page, int count,int objectType) ;
    RecordSet findWhoCommentTarget(Context ctx,String target, int limit) ;
    RecordSet getObjectCommentByUsers(Context ctx,String viewerId,String userIds,String objectType, int page,int count) ;
    boolean updateCanLike(Context ctx,String userId, String commentId, boolean can_like) ;
    boolean getIHasCommented(Context ctx,String commenter, String object) ;
    RecordSet getHotTargetByCommented(Context ctx,String targetType,long max,long min, int page,int count) ;
    Record findMyLastedCommented(Context ctx,String target, String commenter) ;
    boolean updateCommentTarget(Context ctx,String old_target, String new_target) ;

    RecordSet getCommentedPostsP(Context ctx, String userId, String cols, int objectType, int page, int count);
    RecordSet getCommentsForContainsIgnoreP(Context ctx, String viewerId, int objectType, Object id, String cols, boolean asc, int page, int count);




    boolean saveComment(Context ctx,Record comment);



    boolean updateCanLike0(Context ctx,String userId, String commentId, boolean can_like) ;



    RecordSet findCommentsFor(Context ctx,String targetId, List<String> cols, boolean asc, int page, int count);

    RecordSet findCommentsForContainsIgnore(Context ctx,String viewerId, String targetId, List<String> cols, boolean asc, int page, int count);

    RecordSet findComments(Context ctx,List<String> commentId0, List<String> cols);

    RecordSet findCommentsAll(Context ctx,List<String> commentId0, List<String> cols) ;

    RecordSet findCommentedPost(Context ctx,String userId, int page, int count, int objectType);

    RecordSet findWhoCommentTarget0(Context ctx,String target, int limit) ;

    RecordSet getObjectCommentByUsers0(Context ctx,String viewerId, String userIds, String objectType, int page, int count);

    //====================================================================================




    //===============================================PPPPPPPPPPPPPPPP=========================================
    boolean updateCommentTargetP(Context ctx, String target_type, String old_target, String new_target) ;

    RecordSet findWhoCommentTargetP(Context ctx, String target, int limit);

    Record createCommentP(Context ctx, String userId, int objectType, String target, String message, String device, Boolean canLike, String location, String add_to, String appId, String parentId) ;


    Record createComment1P(Context ctx, String userId, int objectType, String target, String message, String device, String location, String add_to, String appId, String parentId) ;

    RecordSet destroyCommentsP(Context ctx, String userId, String commentIds);

    RecordSet destroyCommentsP(Context ctx, String commentIds) ;

    int getCommentCountP(Context ctx, String viewerId, int objectType, Object id);
    boolean updateCommentCanLikeP(Context ctx, String userId, String commentId, boolean can_like) ;

    RecordSet getCommentsForP(Context ctx, String viewerId, int objectType, Object id, String cols, boolean asc, int page, int count) ;

    RecordSet getCommentsForContainsIgnore(Context ctx, String viewerId, int objectType, Object id, String cols, boolean asc, int page, int count);
    RecordSet formatIgnoreStreamOrComments(Context ctx,String viewerId, String sORc, RecordSet recs);
    RecordSet getFullCommentsForP(Context ctx, String viewerId, int objectType, Object id, boolean asc, int page, int count) ;

    RecordSet getCommentsP(Context ctx, String viewerId, String commentIds, String cols)  ;

    RecordSet transComment(Context ctx, String viewerId, RecordSet commentDs);

    RecordSet getCommentsAllP(Context ctx, String commentIds, String cols) ;

    RecordSet getFullComments(Context ctx, String viewerId, String commentIds) ;
    Record getCommentP(Context ctx, String viewerId, String commentId, String cols);

    Record getFullComment(Context ctx, String viewerId, String commentId) ;


    boolean commentCanLikeP(Context ctx, String viewerId, String commentId)  ;


    RecordSet getCommentedPostTargetP(Context ctx, String userId, int objectType, int page, int count) ;

    boolean getIHasCommentedP(Context ctx, String commenter, int target_type, String target_id);

    Map<String, Integer> getCommentCounts(Context ctx, String viewerId, String[] targetIds);
}