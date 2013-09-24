package com.borqs.server.platform.staticfile;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.service.platform.SignIn;
import com.borqs.server.service.platform.StaticFile;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class StaticFileBase extends RPCService implements StaticFile {

    protected final Schema staticFileSchema = Schema.loadClassPath(StaticFileBase.class, "staticfile.schema");

    @Override
    public Class getInterface() {
        return StaticFile.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        staticFileSchema.loadAliases(getConfig().getString("schema.staticfile.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract boolean saveStaticFile0(Record staticFile);

    @Override
    public boolean saveStaticFile(ByteBuffer staticFile) throws AvroRemoteException {
        try {
            Record staticFile0 = Record.fromByteBuffer(staticFile);
            return saveStaticFile0(staticFile0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getStaticFile0(String userId, boolean asc, int page, int count);
    
    @Override
    public ByteBuffer getStaticFile(CharSequence userId, boolean asc, int page, int count) throws AvroRemoteException {
    	try {
    		return getStaticFile0(toStr(userId), asc, page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getStaticFileById0(String file_id);

     @Override
    public ByteBuffer getStaticFileById(CharSequence file_id) throws AvroRemoteException {
    	try {
    		return getStaticFileById0(toStr(file_id)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteStaticFile0(String file_ids);
    
    @Override
    public boolean deleteStaticFile(CharSequence file_ids) throws AvroRemoteException, ResponseError {
    	try {            
    		return deleteStaticFile0(toStr(file_ids));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
