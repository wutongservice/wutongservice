package com.borqs.server.platform.test;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.feature.comment.Comments;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.RandomHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * the memory implement of CommentLogic
 */
public class TestComment implements CommentLogic {
    public ArrayList<Comment> comments = new ArrayList<Comment>();

    @Override
    public Comment createComment(Context ctx, Comment comment) {
        if (comment == null) {
            Comment comment2 = new Comment();
            return createComments(ctx, comment2, 1).get(0);
        } else {

            Comment comment2 = new Comment();
            comment2.setCanLike(comment.getCanLike());
            comment2.setAddTo(comment.getAddTo());
            comment2.setCommenterId(comment.getCommenterId());
            comment2.setCommentId(RandomHelper.generateId());
            comment2.setCreatedTime(DateHelper.nowMillis());
            comment2.setDestroyedTime(0);
            comment2.setDevice(comment.getDevice());
            comment2.setMessage(comment.getMessage());
            Target target = new Target(comment.getTarget().type, comment.getTarget().id);
            comment2.setTarget(target);
            comments.add(comment2);
            return comment2;
        }
    }

    public List<Comment> createComments(Context ctx, Comment comment, int number) {

        if (number < 1) {
            return comments;
        }

        for (int i = 0; i < number; i++) {
            comment = new Comment();
            comment.setCanLike(true);
            comment.setAddTo(new PeopleIds(PeopleId.user(10001 + i)));
            comment.setCommenterId(SteveAndBill.STEVE_ID);
            comment.setCommentId(RandomHelper.generateId());
            comment.setCreatedTime(DateHelper.nowMillis());
            comment.setDestroyedTime(0);
            comment.setDevice("device" + i);
            comment.setMessage("message" + i);
            Target target = new Target(Target.APK, "" + i);
            comment.setTarget(target);
            comments.add(comment);
        }
        return comments;
    }

    @Override
    public boolean destroyComment(Context ctx, long commentId) {
        try {
            Comment destroyed = null;
            for (Comment comment : comments) {
                if (comment.getCommentId() == commentId) {
                    destroyed = comment;
                    break;
                }
            }
            if (destroyed != null)
                comments.remove(destroyed);

            return destroyed != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateComment(Context ctx, Comment comment) {
        Map<Long, Comment> map = converterComments(comments);
        Comment ori = null;
        if (map.containsKey(comment.getCommentId()))
            ori = map.get(comment.getCommentId());
        if (CollectionUtils.isNotEmpty(comment.getAddTo())) {
            ori.setAddTo(comment.getAddTo());
        }
        if (StringUtils.isNotEmpty(comment.getDevice())) {
            ori.setDevice(comment.getDevice());
        }
        if (StringUtils.isNotEmpty(comment.getMessage())) {
            ori.setMessage(comment.getMessage());
        }
        if (comment.getCommenterId() > 0) {
            ori.setCommenterId(comment.getCommenterId());
        }
        ori.setCanLike(comment.getCanLike());
        ori.setTarget(comment.getTarget());

        return true;
    }


    @Override
    public Map<Target, Integer> getCommentCounts(Context ctx, Target... targets) {
        Map<Long, Target> mapTarget = converterComment(comments);
        Map<Target, Integer> map = new HashMap<Target, Integer>();

        for (int i = 1, j = 0; j < targets.length; j++) {
            for (Comment comment : comments) {
                if (comment.getTarget().type == targets[j].type && comment.getTarget().id.equals(targets[j].id))
                    map.put(targets[j], i);
            }
        }
        return map;
    }

    @Override
    public int getCommentCount(Context ctx, Target target) {
        Map<Target, Integer> map = this.getCommentCounts(ctx, new Target[]{target});
        return map.get(target);
    }

    @Override
    public Map<Target, Comment[]> getCommentsOnTarget(Context ctx, String[] expCols, Page page, Target... targets) {
        List<Comment> list = new ArrayList<Comment>();
        Map<Target, Comment[]> map = new HashMap<Target, Comment[]>();
        for (Target target : targets) {
            for (Comment comment : comments) {
                if (comment.getTarget().type == target.type && comment.getTarget().id == target.id)
                    list.add(comment);
            }
            if (!list.isEmpty()) {
                Comment[] comments = new Comment[list.size()];
                map.put(target, list.toArray(comments));
            }
        }
        return map;
    }

    @Override
    public Comments getCommentsOnTarget(Context ctx, String[] expCols, Page page, Target target) {
        Target[] targets = {target};
        Map<Target, Comment[]> map = this.getCommentsOnTarget(ctx, expCols, page, targets);
        if (map.isEmpty()) {
            return null;
        } else {
            Comments c = new Comments();
            c.addAll(Arrays.asList(map.get(target)));
            return c;
        }

    }

    @Override
    public Comments getComments(Context ctx, String[] expCols, long... commentIds) {
        Comments list = new Comments();
        for (Long l : commentIds) {
            for (Comment comment : comments) {
                if (comment.getCommentId() == l)
                    list.add(comment);
            }
        }
        return list;
    }

    @Override
    public Comment getComment(Context ctx, String[] expCols, long commentId) {
        for (Comment c : comments) {
            if (c != null && c.getCommentId() == commentId)
                return c;
        }
        return null;
    }

    @Override
    public long[] getUsersOnTarget(Context ctx, Target target, Page page) {
        List<Long> list = new ArrayList<Long>();

        for (Comment comment : comments) {
            if (comment.getTarget().type == target.type && comment.getTarget().id == target.id)
                list.add(comment.getCommentId());
        }
        long[] longs = new long[list.size()];
        int i = 0;
        for (Long l : list) {
            longs[i++] = l;
        }
        return longs;
    }




    private Map<Long, Comment> converterComments(List<Comment> list) {
        Map<Long, Comment> map = new HashMap<Long, Comment>();
        int i = 1;
        for (Comment comment : list) {
            map.put(comment.getCommentId(), comment);
        }
        return map;
    }

    private Map<Long, Target> converterComment(List<Comment> list) {
        Map<Long, Target> map = new HashMap<Long, Target>();
        int i = 1;
        for (Comment comment : list) {
            map.put(comment.getCommentId(), comment.getTarget());
        }
        return map;
    }

    @Override
    public String[] getTargetIdsOrderByCommentCount(Context ctx, int targetType, boolean asc, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Comment expand(Context ctx, String[] expCols, Comment comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void expand(Context ctx, String[] expCols, Comments data) {
        throw new UnsupportedOperationException();
    }
}
