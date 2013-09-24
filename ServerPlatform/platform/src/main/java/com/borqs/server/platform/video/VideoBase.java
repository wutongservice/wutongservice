package com.borqs.server.platform.video;


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
import com.borqs.server.service.platform.Video;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class VideoBase extends RPCService implements Video {

    protected final Schema videoSchema = Schema.loadClassPath(VideoBase.class, "video.schema");

    @Override
    public Class getInterface() {
        return Video.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        videoSchema.loadAliases(getConfig().getString("schema.video.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract boolean saveVideo0(Record video);

    @Override
    public boolean saveVideo(ByteBuffer video) throws AvroRemoteException {
        try {
            Record video0 = Record.fromByteBuffer(video);
            return saveVideo0(video0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getVideo0(String userId, boolean asc, int page, int count);
    
    @Override
    public ByteBuffer getVideo(CharSequence userId, boolean asc, int page, int count) throws AvroRemoteException {
    	try {
    		return getVideo0(toStr(userId), asc, page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getVideoById0(String video_id);

     @Override
    public ByteBuffer getVideoById(CharSequence video_id) throws AvroRemoteException {
    	try {
    		return getVideoById0(toStr(video_id)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteVideo0(String video_ids);
    
    @Override
    public boolean deleteVideo(CharSequence video_ids) throws AvroRemoteException, ResponseError {
    	try {            
    		return deleteVideo0(toStr(video_ids));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
