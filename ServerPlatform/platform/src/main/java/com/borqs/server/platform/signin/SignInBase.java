package com.borqs.server.platform.signin;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.service.platform.SignIn;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class SignInBase extends RPCService implements SignIn {

    protected final Schema signinSchema = Schema.loadClassPath(SignInBase.class, "signin.schema");

    @Override
    public Class getInterface() {
        return SignIn.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        signinSchema.loadAliases(getConfig().getString("schema.signin.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract boolean saveSignIn0(Record sign_in);

    @Override
    public boolean saveSignIn(ByteBuffer sinIn) throws AvroRemoteException {
        try {
            Record sinIn0 = Record.fromByteBuffer(sinIn);
            Schemas.checkRecordIncludeColumns(sinIn0, "user_id", "longitude", "latitude");
            sinIn0.put("sign_id", Long.toString(RandomUtils.generateId()));
            sinIn0.put("created_time", DateUtils.nowMillis());
            return saveSignIn0(sinIn0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSignIn0(String userId, boolean asc, int page, int count);
    
    @Override
    public ByteBuffer getSignIn(CharSequence userId, boolean asc, int page, int count) throws AvroRemoteException {
    	try {
    		return getSignIn0(toStr(userId), asc,page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract boolean deleteSignIn0(String sign_ids);
    
    @Override
    public boolean deleteSignIn(CharSequence sign_ids) throws AvroRemoteException, ResponseError {
    	try {            
    		return deleteSignIn0(toStr(sign_ids));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getUserShaking0(String userId, long dateDiff, boolean asc, int page, int count);

    @Override
    public ByteBuffer getUserShaking(CharSequence userId, long dateDiff, boolean asc, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getUserShaking0(toStr(userId), dateDiff, asc, page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getUserNearBy0(String userId,  int page, int count);

    @Override
    public ByteBuffer getUserNearBy(CharSequence userId, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getUserNearBy0(toStr(userId), page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
