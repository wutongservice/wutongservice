package com.borqs.server.platform.request;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Request;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.io.ValidatingEncoder;
import org.apache.commons.lang.Validate;
import org.codehaus.plexus.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.List;

public abstract class RequestBase extends RPCService implements Request {

    protected final Schema requestSchema = Schema.loadClassPath(RequestBase.class, "request.schema");;

    @Override
    public Class getInterface() {
        return Request.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        requestSchema.loadAliases(getConfig().getString("schema.request.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract long saveRequest(String userId, String sourceId, String app, String type, String message, String data, String options);

    protected abstract boolean saveRequests(String userIds, String sourceId, String app, String type, String message, String data, String options);

    @Override
    public CharSequence createRequest(CharSequence userId, CharSequence sourceId, CharSequence app, CharSequence type, CharSequence message, CharSequence data, CharSequence options) throws AvroRemoteException, ResponseError {
        try {
            Validate.isTrue(!StringUtils.equals(toStr(userId), toStr(sourceId)));
            long reqId = saveRequest(toStr(userId), toStr(sourceId), toStr(app), toStr(type), toStr(message), toStr(data), toStr(options));
            return Long.toString(reqId);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean createRequests(CharSequence userIds, CharSequence sourceId, CharSequence app, CharSequence type, CharSequence message, CharSequence data, CharSequence options) throws AvroRemoteException, ResponseError {
        try {
            return saveRequests(toStr(userIds), toStr(sourceId), toStr(app), toStr(type), toStr(message), toStr(data), toStr(options));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract void destroyRequests0(String userId, List<Long> reqIds);

    @Override
    public boolean destroyRequests(CharSequence userId, CharSequence requests) throws AvroRemoteException {
        try {
            List<Long> reqIds = StringUtils2.splitIntList(toStr(requests), ",");
            destroyRequests0(toStr(userId), reqIds);
            return true;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getRequests0(String userId, String appId, String type);

    @Override
    public ByteBuffer getRequests(CharSequence userId, CharSequence app, CharSequence type) throws AvroRemoteException, ResponseError {
        try {
            return getRequests0(toStr(userId), toStr(app), toStr(type)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract String getRelatedRequestIds0(String sourceId, String data);

    @Override
    public CharSequence getRelatedRequestIds(CharSequence sourceIds, CharSequence datas) throws AvroRemoteException, ResponseError {
        try {
            return getRelatedRequestIds0(toStr(sourceIds), toStr(datas));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean doneRequest0(String userId, List<Long> requestIds);

    @Override
    public boolean doneRequest(CharSequence userId, CharSequence requestIds) throws AvroRemoteException, ResponseError {
        try {
            return doneRequest0(toStr(userId), StringUtils2.splitIntList(toStr(requestIds), ","));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int getCount0(String userId, String app, String type);

    @Override
    public int getCount(CharSequence userId, CharSequence app, CharSequence type) throws AvroRemoteException, ResponseError {
        try {
            return getCount0(toStr(userId), toStr(app), toStr(type));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract String getPeddingRequests0(String source, String user);
    
    @Override
    public CharSequence getPeddingRequests(CharSequence source, CharSequence user) throws AvroRemoteException, ResponseError {
    	return getPeddingRequests0(toStr(source), toStr(user));
    }

     protected abstract RecordSet getPeddingRequests1(String source, String userIds);

    @Override
    public ByteBuffer getPeddingRequestsAll(CharSequence userId, CharSequence userIds) throws AvroRemoteException, ResponseError {
        try {
            return getPeddingRequests1(toStr(userId), toStr(userIds)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
