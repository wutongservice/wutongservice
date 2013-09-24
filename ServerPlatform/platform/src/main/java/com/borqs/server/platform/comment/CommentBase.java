package com.borqs.server.platform.comment;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Comment;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;
import java.util.List;

public abstract class CommentBase extends RPCService implements Comment {
    protected final Schema commentSchema = Schema.loadClassPath(CommentBase.class, "comment.schema");

    protected CommentBase() {
    }

    @Override
    public final Class getInterface() {
        return Comment.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        commentSchema.loadAliases(getConfig().getString("schema.comment.alias", null));
    }

    @Override
    public void destroy() {
    }

    protected static String genCommentId() {
        return Long.toString(RandomUtils.generateId());
    }

    protected abstract boolean saveComment(Record comment);

    private static Record addCommentIdStrCol(Record rec) {
        if (rec != null && rec.has("comment_id") && !rec.has("comment_id_s"))
            rec.put("comment_id_s", rec.getString("comment_id"));
        return rec;
    }

    private static RecordSet addCommentIdStrCol(RecordSet recs) {
        if (recs != null) {
            for (Record rec : recs)
                addCommentIdStrCol(rec);
        }
        return recs;
    }

    @Override
    public CharSequence createComment(CharSequence userId, CharSequence targetId, ByteBuffer comment) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            String targetId0 = toStr(targetId);
            Record comment0 = Record.fromByteBuffer(comment);
            Schemas.checkRecordIncludeColumns(comment0, "message", "commenter_name");
            comment0.put("target", targetId0);
            comment0.put("created_time", DateUtils.nowMillis());
            comment0.putMissing("can_like", true);
            comment0.putMissing("device", "");
            String commentId = genCommentId();
            comment0.put("comment_id", commentId);
            comment0.put("commenter", userId0);

            Schemas.standardize(commentSchema, comment0);
            boolean b = saveComment(comment0);
            if (!b)
                throw new CommentException("Save comment error");

            return commentId;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record disableComments(String userId, String commentId,String fromSource,String objectType);

    @Override
    public ByteBuffer destroyComments(CharSequence userId, CharSequence commentId, CharSequence fromSource, CharSequence objectType) throws AvroRemoteException, ResponseError {
        try {
            return disableComments(toStr(userId), toStr(commentId),toStr(fromSource),toStr(objectType)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int getCommentCount0(String viewerId,String targetId);

    @Override
    public int getCommentCount(CharSequence viewerId,CharSequence targetId) throws AvroRemoteException, ResponseError {
        try {
            return getCommentCount0(toStr(viewerId),toStr(targetId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findCommentsFor(String targetId, List<String> cols, boolean asc, int page, int count);

    @Override
    public ByteBuffer getCommentsFor(CharSequence targetId, CharSequence cols, boolean asc, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
            if (cols0.isEmpty())
                return new RecordSet().toByteBuffer();

            return addCommentIdStrCol(findCommentsFor(toStr(targetId), cols0, asc, page, count)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findCommentsForContainsIgnore(String viewerId,String targetId, List<String> cols, boolean asc, int page, int count);

    @Override
    public ByteBuffer getCommentsForContainsIgnore(CharSequence viewerId, CharSequence targetId, CharSequence cols, boolean asc, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
            if (cols0.isEmpty())
                return new RecordSet().toByteBuffer();

            return addCommentIdStrCol(findCommentsForContainsIgnore(toStr(viewerId),toStr(targetId), cols0, asc, page, count)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }


    protected abstract RecordSet findComments(List<String> commentIds, List<String> cols);

    @Override
    public ByteBuffer getComments(CharSequence commentIds, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            List<String> commentIds0 = StringUtils2.splitList(toStr(commentIds), ",", true);
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
            if (cols0.isEmpty() || commentIds0.isEmpty())
                return new Record().toByteBuffer();

            return addCommentIdStrCol(findComments(commentIds0, cols0)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet findCommentsAll(List<String> commentIds, List<String> cols);
    
    @Override
    public ByteBuffer getCommentsAll(CharSequence commentIds, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            List<String> commentIds0 = StringUtils2.splitList(toStr(commentIds), ",", true);
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
            if (cols0.isEmpty() || commentIds0.isEmpty())
                return new Record().toByteBuffer();

            return addCommentIdStrCol(findCommentsAll(commentIds0, cols0)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet findCommentedPost(String userId, int page, int count,int objectType);
    
    @Override
    public ByteBuffer getCommentedPost(CharSequence userId,int page, int count,int objectType) throws AvroRemoteException, ResponseError {
        try {
            return addCommentIdStrCol(findCommentedPost(toStr(userId),  page,count,objectType)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet findWhoCommentTarget0(String target, int limit);
 
    @Override
    public ByteBuffer findWhoCommentTarget(CharSequence target,int limit) throws AvroRemoteException, ResponseError {
        try {
            return findWhoCommentTarget0(toStr(target), limit).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getObjectCommentByUsers0(String viewerId, String userIds,String objectType, int page, int count);

    @Override
    public ByteBuffer getObjectCommentByUsers(CharSequence viewerId,CharSequence userIds,CharSequence objectType,int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getObjectCommentByUsers0(toStr(viewerId), toStr(userIds),toStr(objectType),page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateCanLike0(String userId, String commentId, boolean can_like);

    public boolean updateCanLike(CharSequence userId,CharSequence commentId,boolean can_like) throws AvroRemoteException, ResponseError {
        try {
            return updateCanLike0(toStr(userId), toStr(commentId),can_like);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean getIHasCommented0(String commenter, String object);

    @Override
    public boolean getIHasCommented(CharSequence commenter,CharSequence object) throws AvroRemoteException, ResponseError {
        try {
            return getIHasCommented0(toStr(commenter), toStr(object));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getHotTargetByCommented0(String targetType,long max,long min,int page, int count);

    @Override
    public ByteBuffer getHotTargetByCommented(CharSequence targetType,long max,long min,int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getHotTargetByCommented0(toStr(targetType),max,min,page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record findMyLastedCommented0(String target,String commenter);
    @Override
    public ByteBuffer findMyLastedCommented(CharSequence target,CharSequence commenter) throws AvroRemoteException, ResponseError {
        try {
            return findMyLastedCommented0(toStr(target),toStr(commenter)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateCommentTarget0(String old_target, String new_target);

     @Override
    public boolean updateCommentTarget(CharSequence old_target,CharSequence new_target) throws AvroRemoteException, ResponseError {
        try {
            return updateCommentTarget0(toStr(old_target), toStr(new_target));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
