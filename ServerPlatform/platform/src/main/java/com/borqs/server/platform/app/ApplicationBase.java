package com.borqs.server.platform.app;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.service.platform.Application;
import org.apache.avro.AvroRemoteException;

public abstract class ApplicationBase extends RPCService implements Application {
    protected ApplicationBase() {
    }

    @Override
    public final Class getInterface() {
        return Application.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    protected abstract String findAppSecret(String appId);

    @Override
    public CharSequence getAppSecret(CharSequence appId) throws AvroRemoteException, ResponseError {
        try {
            return findAppSecret(toStr(appId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract String findQiupuMinVersion0();
    
    @Override
    public CharSequence findQiupuMinVersion() throws AvroRemoteException, ResponseError {
        try {
            return findQiupuMinVersion0();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
