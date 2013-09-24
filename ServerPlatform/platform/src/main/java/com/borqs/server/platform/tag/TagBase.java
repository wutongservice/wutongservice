package com.borqs.server.platform.tag;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.service.platform.Tag;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class TagBase extends RPCService implements Tag {

    protected final Schema tagSchema = Schema.loadClassPath(TagBase.class, "tag.schema");

    @Override
    public Class getInterface() {
        return Tag.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        tagSchema.loadAliases(getConfig().getString("schema.tag.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract Record saveTag(Record tag);

    @Override
    public String createTag(ByteBuffer tag) throws AvroRemoteException {
        try {
            Record tag0 = Record.fromByteBuffer(tag);
            return saveTag(tag0).toString();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findUserByTag0(String tag, int page, int count);

    @Override
    public ByteBuffer findUserByTag(CharSequence tag, int page, int count) throws AvroRemoteException {
        try {
            return findUserByTag0(toStr(tag), page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean hasTag0(String user, String tag);

    @Override
    public boolean hasTag(CharSequence userId, CharSequence tag) throws AvroRemoteException, ResponseError {
        try {
            return hasTag0(toStr(userId), toStr(tag));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean hasTarget0(String user_id, String target_id, String type);

    @Override
    public boolean hasTarget(CharSequence userId, CharSequence target_id, CharSequence type) throws AvroRemoteException, ResponseError {

        try {
            return hasTarget0(toStr(userId), toStr(target_id), toStr(type));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findTagByUser0(String user_id, int page, int count);

    @Override
    public ByteBuffer findTagByUser(CharSequence user_id, int page, int count) throws AvroRemoteException {
        try {
            return findTagByUser0(toStr(user_id), page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findTargetsByTag0(String tag, String type, int page, int count);

    @Override
    public ByteBuffer findTargetsByTag(CharSequence tag, CharSequence type, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return findTargetsByTag0(toStr(tag), toStr(type), page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findTargetsByUser0(String user_id, String type, int page, int count);

    @Override
    public ByteBuffer findTargetsByUser(CharSequence userId, CharSequence type, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return findTargetsByUser0(toStr(userId), toStr(type), page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findUserTagByTarget0(String target_id, String type, int page, int count);
    @Override
    public ByteBuffer findUserTagByTarget(CharSequence target_id, CharSequence type, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return findUserTagByTarget0(toStr(target_id), toStr(type), page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean destroyedTag0(Record tag);

    @Override
    public boolean destroyedTag(ByteBuffer tagId) throws AvroRemoteException {
        try {
            Record tag = Record.fromByteBuffer(tagId);
            return destroyedTag0(tag);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
