package com.borqs.server.impl.comment;


import com.borqs.server.base.context.Context;
import com.borqs.server.feature.comment.Comment;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class CommentImpl {

    CommentDB commentDB = new CommentDB();
    CommentCache commentCache = new CommentCache();

    public void init() throws SQLException {
        commentDB.init();
        commentCache.init();
    }

    public void destroy() {
        commentDB.destroy();
        commentCache.destroy();
    }

    public Comment createComment(Context ctx,Comment comment) throws SQLException {
        commentDB.createComment(ctx,comment);
        commentCache.createCommentCache(comment);
        return comment;
    }

    public Comment getComment(Context ctx,long commentId) throws IOException, SQLException {
        Comment comment = new Comment();
        try {
            comment = commentCache.getCommentCache(commentId);
        } catch (Exception e) {
        }
        if (comment == null) {
            comment = commentDB.getComment(ctx,commentId);
            commentCache.createCommentCache(comment);
        }
        return comment;
    }

    public List<Comment> getCommentsByIds(Context ctx,long[] commentIds) throws SQLException, IOException {
        List<Comment> hasCommentFromCacheList = commentCache.getComments(commentIds);
        List<Comment> outList = new ArrayList<Comment>();

        List<Long> missCommentId = new ArrayList<Long>();
        List<Long> hasCommentId = new ArrayList<Long>();

        for (Comment comment0 : hasCommentFromCacheList) {
            if (comment0 != null && comment0.getCommentId() > 0) {
                outList.add(comment0);
                hasCommentId.add(comment0.getCommentId());
            }
        }

        for (long commentId : commentIds) {
            if (!hasCommentId.contains(commentId)) {
                missCommentId.add(commentId);
            }
        }

        long[] ll = new long[missCommentId.size()];
        for (int i = 0; i < missCommentId.size(); i++) {
            ll[i] = missCommentId.get(i);
        }

        List<Comment> hasCommentFromDbList = commentDB.getCommentsByIds(ctx,ll);
        for (Comment c : hasCommentFromDbList) {
            commentCache.createCommentCache(c);
        }

        hasCommentFromCacheList.addAll(hasCommentFromDbList);

        for (long pId : commentIds) {
            for (Comment comment2 : hasCommentFromCacheList) {
                if (pId == comment2.getCommentId()) {
                    outList.add(comment2);
                    break;
                }
            }
        }

        ComComment comparator = new ComComment();
        Collections.sort(outList, comparator);

        return outList;
    }

    public List<Comment> getCommentsByTargetId(Context ctx, int targetType, String targetId, int page, int count) throws SQLException, IOException {
        List<Comment> comment = commentCache.getTargetCache(targetType, targetId);
        ComComment comparator = new ComComment();
        if (comment.size() <= 0) {
            comment = commentDB.getCommentsByTargetId(ctx, targetType, targetId, page, count);
            if (comment.size() > 0) {
                for (Comment c : comment) {
                    commentCache.createCommentCache(c);
                }
            }
        } else {
            Collections.sort(comment, comparator);
            comment = commentSplitPage(comment, page, count);
        }
        return comment;
    }

    public boolean destroyComment(Context ctx,long[] commentIds) throws SQLException {
        boolean b = commentDB.destroyComment(ctx,commentIds);
        commentCache.destroyComment(commentIds);
        return b;
    }

    public List<Comment> commentSplitPage(List<Comment> comments, int page, int count) {
        List<Comment> outComments = new ArrayList<Comment>();
        int rowsCont = comments.size();
        int pageCount = 0;
        if (comments.size() % count == 0) {
            pageCount = comments.size() / count;
        } else {
            pageCount = (comments.size() / count) + 1;
        }
        if (page > pageCount) {
            page = pageCount;
        }
        if (page == 0) {
            if (count <= rowsCont) {
                for (int i = 0; i < count; i++) {
                    outComments.add(comments.get(i));
                }
            } else {
                outComments = comments;
            }
        } else {
            for (int i = ((page - 1) * count); i < comments.size() && i < ((page) * count) && page > 0; i++) {
                outComments.add(comments.get(i));
            }
        }
        return outComments;
    }
}

class ComComment implements Comparator {
    public int compare(Object arg0, Object arg1) {
        Comment c0 = (Comment) arg0;
        Comment c1 = (Comment) arg1;
        int flag = String.valueOf(c0.getCreatedTime()).compareTo(String.valueOf(c1.getCreatedTime()));
        return flag;
    }
}

