package com.borqs.server.platform.statistics;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.service.platform.Statistics;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class StatisticsBase extends RPCService implements Statistics {

    @Override
    public Class getInterface() {
        return StatisticsBase.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {

    }

    protected abstract boolean save0(Record statistics);


    @Override
    public boolean save(ByteBuffer statistics) throws AvroRemoteException, ResponseError {
        try {
            return save0(Record.fromByteBuffer(statistics));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
