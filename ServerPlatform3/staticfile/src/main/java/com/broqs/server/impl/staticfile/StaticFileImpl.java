package com.broqs.server.impl.staticfile;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.staticfile.video.StaticFile;
import com.borqs.server.platform.feature.staticfile.video.StaticFileHook;
import com.borqs.server.platform.feature.staticfile.video.StaticFileLogic;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.sfs.SFSHelper;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

public class StaticFileImpl implements StaticFileLogic {
    private static final Logger L = Logger.get(StaticFileImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final StaticFileDb db = new StaticFileDb();

    // hooks
    private List<StaticFileHook> staticFileHooks;


    public StaticFileImpl() {
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


    public void setStaticFileTable(Table audioTable) {
        db.setStaticFileTable(audioTable);
    }

    public List<StaticFileHook> getStaticFileHooks() {
        return staticFileHooks;
    }

    public void setStaticFileHooks(List<StaticFileHook> staticFileHooks) {
        this.staticFileHooks = staticFileHooks;
    }

    @Override
    public boolean saveStaticFile(Context ctx, StaticFile staticFile) {
        final LogCall LC = LogCall.startCall(L, StaticFileImpl.class, "saveStaticFile", ctx, "staticFile", staticFile);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("staticFile", staticFile);

            db.saveStaticFile(ctx, staticFile);
            LC.endCall();
            return true;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public List<StaticFile> getStaticFile(Context ctx, long userId, boolean asc, Page page) {
        final LogCall LC = LogCall.startCall(L, StaticFileImpl.class, "getStaticFile", ctx, "userId", userId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("userId", userId);
            ParamChecker.notNull("asc", asc);
            ParamChecker.notNull("page", page);

            List<StaticFile> videos = db.getStaticFileByUserIds(ctx, userId);
            LC.endCall();
            return videos;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public List<StaticFile> getStaticFileById(Context ctx, long... staticFileId) {
        final LogCall LC = LogCall.startCall(L, StaticFileImpl.class, "getVideoById", ctx, "staticFileId", staticFileId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("staticFileId", staticFileId);


            List<StaticFile> staticFiles = db.getStaticFiles(ctx, staticFileId);
            LC.endCall();
            return staticFiles;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean deleteStaticFile(Context ctx, long... staticFileId) {
        final LogCall LC = LogCall.startCall(L, StaticFileImpl.class, "deleteVideo", ctx, "staticFileId", staticFileId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("staticFileId", staticFileId);


            boolean b = db.deleteStaticFile(ctx, staticFileId);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean uploadStaticFile(Context ctx, SFS sfs, FileItem fileItem, String name) {
        try {
            SFSHelper.saveUpload(sfs, fileItem, name);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("upload file error");
        }
    }
}
