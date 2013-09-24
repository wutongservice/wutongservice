package com.borqs.server.platform.reportabuse;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.service.platform.ReportAbuse;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class ReportAbuseBase extends RPCService implements ReportAbuse {

    protected final Schema reportAbuseSchema = Schema.loadClassPath(ReportAbuseBase.class, "reportabuse.schema");

    @Override
    public Class getInterface() {
        return ReportAbuse.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        reportAbuseSchema.loadAliases(getConfig().getString("schema.reportabuse.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract boolean saveReportAbuse0(Record reportAbuse);

    @Override
    public boolean saveReportAbuse(ByteBuffer reportAbuse) throws AvroRemoteException {
        try {
            Record reportAbuse0 = Record.fromByteBuffer(reportAbuse);
            return saveReportAbuse0(reportAbuse0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }


   protected abstract int getReportAbuseCount0(String post_id);
    
    @Override
    public int getReportAbuseCount(CharSequence post_id) throws AvroRemoteException, ResponseError {
    	try {            
    		return getReportAbuseCount0(toStr(post_id));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int iHaveReport0(String viewerId,String post_id);

   @Override
    public int iHaveReport(CharSequence viewerId,CharSequence post_id) throws AvroRemoteException, ResponseError {
    	try {
    		return iHaveReport0(toStr(viewerId),toStr(post_id));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
