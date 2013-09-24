package com.borqs.server.platform.like;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.service.platform.Like;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class LikeBase extends RPCService implements Like {
    protected final Schema likeSchema = Schema.loadClassPath(LikeBase.class, "like.schema");

    protected LikeBase() {
    }

    @Override
    public final Class getInterface() {
        return Like.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        likeSchema.loadAliases(getConfig().getString("schema.like.alias", null));
    }

    @Override
    public void destroy() {
    }

    protected abstract boolean saveLike(Record like);

    @Override
    public boolean createLike(CharSequence userId, CharSequence targetId) throws AvroRemoteException, ResponseError {
        try {
            if (!userLiked(toStr(userId), toStr(targetId))) {
                Record like = Record.of("target", toStr(targetId), "liker", toStr(userId));
                Schemas.standardize(likeSchema, like);
                return saveLike(like);
            } else {
                return false;
            }
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteLike(String userId, String targetId);

    @Override
    public boolean destroyLike(CharSequence userId, CharSequence targetId) throws AvroRemoteException, ResponseError {
        try {
            return deleteLike(toStr(userId), toStr(targetId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }


    protected abstract int getLikeCount0(String targetId);

    @Override
    public int getLikeCount(CharSequence targetId) throws AvroRemoteException, ResponseError {
        try {
            return getLikeCount0(toStr(targetId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean userLiked(String userId, String targetId);
    @Override
    public boolean ifUserLiked(CharSequence userId, CharSequence targetId) throws AvroRemoteException, ResponseError {
        try {
            return userLiked(toStr(userId),toStr(targetId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet likedUsers(String targetId, int page, int count);
    
    @Override
    public ByteBuffer loadLikedUsers(CharSequence targetId, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return likedUsers(toStr(targetId),page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet findLikedPost(String userId, int page, int count,int objectType);
    
    @Override
    public ByteBuffer getLikedPost(CharSequence userId,int page, int count,int objectType) throws AvroRemoteException, ResponseError {
        try {
            return findLikedPost(toStr(userId), page,count,objectType).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getObjectLikedByUsers0(String viewerId, String userIds,String objectType, int page, int count);

    @Override
    public ByteBuffer getObjectLikedByUsers(CharSequence viewerId,CharSequence userIds,CharSequence objectType,int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getObjectLikedByUsers0(toStr(viewerId),toStr(userIds),toStr(objectType), page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateLikeTarget0(String old_target, String new_target);

     @Override
    public boolean updateLikeTarget(CharSequence old_target,CharSequence new_target) throws AvroRemoteException, ResponseError {
        try {
            return updateLikeTarget0(toStr(old_target), toStr(new_target));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
