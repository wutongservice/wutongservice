package com.borqs.server.platform.audio;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.service.platform.Audio;
import com.borqs.server.service.platform.SignIn;
import org.apache.avro.AvroRemoteException;

import java.nio.ByteBuffer;

public abstract class AudioBase extends RPCService implements Audio {

    protected final Schema audioSchema = Schema.loadClassPath(AudioBase.class, "audio.schema");

    @Override
    public Class getInterface() {
        return Audio.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        audioSchema.loadAliases(getConfig().getString("schema.audio.alias", null));
    }

    @Override
    public void destroy() {

    }

    protected abstract boolean saveAudio0(Record audio);

    @Override
    public boolean saveAudio(ByteBuffer audio) throws AvroRemoteException {
        try {
            Record audio0 = Record.fromByteBuffer(audio);
            return saveAudio0(audio0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getAudio0(String userId, boolean asc, int page, int count);
    
    @Override
    public ByteBuffer getAudio(CharSequence userId, boolean asc, int page, int count) throws AvroRemoteException {
    	try {
    		return getAudio0(toStr(userId), asc, page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getAudioById0(String audio_id);

     @Override
    public ByteBuffer getAudioById(CharSequence audio_id) throws AvroRemoteException {
    	try {
    		return getAudioById0(toStr(audio_id)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean deleteAudio0(String audio_ids);
    
    @Override
    public boolean deleteAudio(CharSequence audio_ids) throws AvroRemoteException, ResponseError {
    	try {            
    		return deleteAudio0(toStr(audio_ids));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
