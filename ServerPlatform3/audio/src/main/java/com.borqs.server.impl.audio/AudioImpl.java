package com.borqs.server.impl.audio;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.audio.Audio;
import com.borqs.server.platform.feature.audio.AudioHook;
import com.borqs.server.platform.feature.audio.AudioLogic;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.sfs.SFSHelper;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

public class AudioImpl implements AudioLogic {
    private static final Logger L = Logger.get(AudioImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final AudioDb db = new AudioDb();

    // hooks
    private List<AudioHook> audioHooks;


    public AudioImpl() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }


    public void setAudioTable(Table audioTable) {
        db.setAudioTable(audioTable);
    }

    public List<AudioHook> getAudioHooks() {
        return audioHooks;
    }

    public void setAudioHooks(List<AudioHook> audioHooks) {
        this.audioHooks = audioHooks;
    }


    @Override
    public boolean saveAudio(Context ctx, Audio audio) {
        final LogCall LC = LogCall.startCall(L, AudioImpl.class, "saveAudio", ctx, "audio", audio);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("audio", audio);

            db.saveAudio(ctx, audio);
            LC.endCall();
            return true;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public List<Audio> getAudio(Context ctx, long userId, boolean asc, Page page) {
        final LogCall LC = LogCall.startCall(L, AudioImpl.class, "getAudio", ctx, "userId", userId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("audioIds", userId);
            ParamChecker.notNull("asc", asc);
            ParamChecker.notNull("page", page);

            List<Audio> audios = db.getAudioByUserIds(ctx, userId);
            LC.endCall();
            return audios;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public List<Audio> getAudiosById(Context ctx, long... audioIds) {
        final LogCall LC = LogCall.startCall(L, AudioImpl.class, "getAudiosById", ctx);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("userIds", audioIds);


            List<Audio> audios = db.getAudios(ctx, audioIds);
            LC.endCall();
            return audios;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean deleteAudio(Context ctx, long... audioId) {
        final LogCall LC = LogCall.startCall(L, AudioImpl.class, "getAudiosById", ctx);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("audioIds", audioId);


            boolean b = db.deleteAudio(ctx, audioId);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean uploadAudio(Context ctx, SFS sfs, FileItem fileItem, String name) {
        try {
            SFSHelper.saveUpload(sfs, fileItem, name);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("upload file error");
        }
    }


}
