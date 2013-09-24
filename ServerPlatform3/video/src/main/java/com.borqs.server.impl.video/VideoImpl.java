package com.borqs.server.impl.video;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.video.Video;
import com.borqs.server.platform.feature.video.VideoHook;
import com.borqs.server.platform.feature.video.VideoLogic;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.sfs.SFSHelper;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.fileupload.FileItem;

import java.util.List;
import java.util.Map;

public class VideoImpl implements VideoLogic {
    private static final Logger L = Logger.get(VideoImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final VideoDb db = new VideoDb();

    // hooks
    private List<VideoHook> videoHooks;


    // limits
    private Map<String, Integer> limits;

    public VideoImpl() {
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


    public void setVideoTable(Table videoTable) {
        db.setVideoTable(videoTable);
    }

    public List<VideoHook> getVideoHooks() {
        return videoHooks;
    }

    public void setVideoHooks(List<VideoHook> videoHooks) {
        this.videoHooks = videoHooks;
    }


    @Override
    public boolean saveVideo(Context ctx, Video video) {
        final LogCall LC = LogCall.startCall(L, VideoImpl.class, "saveVideo", ctx, "video", video);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("audio", video);

            db.saveVideo(ctx, video);
            LC.endCall();
            return true;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public List<Video> getVideo(Context ctx, long userId, boolean asc, Page page) {
        final LogCall LC = LogCall.startCall(L, VideoImpl.class, "getVideo", ctx, "userId", userId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("userId", userId);
            ParamChecker.notNull("asc", asc);
            ParamChecker.notNull("page", page);

            List<Video> videos = db.getVideoByUserIds(ctx, userId);
            LC.endCall();
            return videos;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public List<Video> getVideoById(Context ctx, long... videoId) {
        final LogCall LC = LogCall.startCall(L, VideoImpl.class, "getVideoById", ctx, "videoId", videoId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("userIds", videoId);


            List<Video> videos = db.getVideos(ctx, videoId);
            LC.endCall();
            return videos;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean deleteVideo(Context ctx, long... videoId) {
        final LogCall LC = LogCall.startCall(L, VideoImpl.class, "deleteVideo", ctx, "videoId", videoId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("audioIds", videoId);


            boolean b = db.deleteVideo(ctx, videoId);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean uploadVideo(Context ctx, SFS sfs, FileItem fileItem, String name) {
        try {
            SFSHelper.saveUpload(sfs, fileItem, name);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("upload file error");
        }
    }

}
