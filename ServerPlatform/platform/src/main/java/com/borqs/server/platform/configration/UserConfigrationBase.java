package com.borqs.server.platform.configration;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.service.platform.UserConfigration;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class UserConfigrationBase extends RPCService implements UserConfigration {

    protected final Schema configrationSchema = Schema.loadClassPath(UserConfigrationBase.class, "configration.schema");

    @Override
    public Class getInterface() {
        return UserConfigration.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        configrationSchema.loadAliases(getConfig().getString("schema.configration.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract boolean saveConfigration0(Record configration);

    @Override
    public boolean saveConfigration(ByteBuffer configration) throws AvroRemoteException {
        try {
            Record configration0 = Record.fromByteBuffer(configration);
            return saveConfigration0(configration0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getConfigration0(String userId, String key,int version_code);
    
    @Override
    public ByteBuffer getConfigration(CharSequence userId, CharSequence key, int version_code) throws AvroRemoteException {
    	try {
    		return getConfigration0(toStr(userId), toStr(key),version_code).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteConfigration0(String userId, String key,int version_code);

     @Override
    public boolean deleteConfigration(CharSequence userId, CharSequence key, int version_code) throws AvroRemoteException {
    	try {
    		return deleteConfigration0(toStr(userId), toStr(key),version_code);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getUserConfigration0(String userId);
    
    @Override
    public ByteBuffer getUserConfigration(CharSequence userId) throws AvroRemoteException, ResponseError {
    	try {            
    		return getUserConfigration0(toStr(userId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
