package com.borqs.server.platform.ignore;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import org.apache.avro.AvroRemoteException;
import com.borqs.server.service.platform.Ignore;

import java.nio.ByteBuffer;

public abstract class IgnoreBase extends RPCService implements Ignore {
    protected final Schema ignoreSchema = Schema.loadClassPath(IgnoreBase.class, "ignore.schema");

    protected IgnoreBase() {
    }

    @Override
    public final Class getInterface() {
        return Ignore.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        ignoreSchema.loadAliases(getConfig().getString("schema.ignore.alias", null));
    }

    @Override
    public void destroy() {
    }

    protected abstract boolean saveIgnore0(Record ignore);

    @Override
    public boolean createIgnore(CharSequence userId, CharSequence targetType, CharSequence targetId) throws AvroRemoteException, ResponseError {
        try {
            if (!toStr(userId).equals("0") && !toStr(targetType).equals("0") && !toStr(targetId).equals("0")) {
                Record ignore = Record.of("user", toStr(userId), "target_type", toStr(targetType), "target_id", toStr(targetId));
                return saveIgnore0(ignore);
            } else {
                return false;
            }
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteIgnore0(String userId, String targetType,String targetId);

    @Override
    public boolean deleteIgnore(CharSequence userId, CharSequence targetType, CharSequence targetId) throws AvroRemoteException, ResponseError {
        try {
            return deleteIgnore0(toStr(userId), toStr(targetType), toStr(targetId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getIgnoreList0(String user_id,String target_type,int page,int count);

    @Override
    public ByteBuffer getIgnoreList(CharSequence user_id, CharSequence target_type, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getIgnoreList0(toStr(user_id), toStr(target_type), page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean getExistsIgnore0(String userId, String targetType,String targetId);

    @Override
    public boolean getExistsIgnore(CharSequence user_id, CharSequence target_type, CharSequence targetId) throws AvroRemoteException, ResponseError {
        try {
            return getExistsIgnore0(toStr(user_id), toStr(target_type), toStr(targetId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

}
