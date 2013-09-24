package com.borqs.server.pubapi.v1;

import com.borqs.server.compatible.CompatiblePost;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.audio.Audio;
import com.borqs.server.platform.feature.audio.AudioLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.staticfile.video.StaticFile;
import com.borqs.server.platform.feature.staticfile.video.StaticFileLogic;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.feature.video.Video;
import com.borqs.server.platform.feature.video.VideoLogic;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.platform.mq.QueueName;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import java.util.List;

@IgnoreDocument
public class StaticFile1Api extends PublicApiSupport {
    private VideoLogic video;
    private AudioLogic audio;
    private StaticFileLogic staticFile;
    private AccountLogic account;
    private QueueName postQueue;
    private StreamLogic stream;

    private SFS sfsVideo;
    private SFS sfsAudio;
    private SFS sfsStaticFile;

    public static String bucketName = "wutong-data";
    public static String bucketName_video_key = "media/video/";
    public static String bucketName_audio_key = "media/audio/";
    public static String bucketName_static_file_key = "files/";

    public void setVideo(VideoLogic video) {
        this.video = video;
    }

    public void setAudio(AudioLogic audio) {
        this.audio = audio;
    }

    public void setStaticFile(StaticFileLogic staticFile) {
        this.staticFile = staticFile;
    }

    public void setSfsVideo(SFS sfsVideo) {
        this.sfsVideo = sfsVideo;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public void setPostQueue(QueueName postQueue) {
        this.postQueue = postQueue;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public void setSfsAudio(SFS sfsAudio) {
        this.sfsAudio = sfsAudio;
    }

    public void setSfsStaticFile(SFS sfsStaticFile) {
        this.sfsStaticFile = sfsStaticFile;
    }

    public StaticFile1Api() {
    }


    @Route(url = "/file/upload")
    public void fileUpload(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        FileItem fileItem = req.checkFile("file");
        String json = uploadFile(req, ctx, fileItem);
        resp.body(RawText.of(json));
    }

    @Route(url = "/file/share")
    public void fileShare(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        FileItem fileItem = req.checkFile("file");
        uploadFile(req, ctx, fileItem);

        if (req.getBoolean("secretly", false))
            req.checkString("mentions");

        String message = req.checkString("msg");

        long now = DateHelper.nowMillis();
        Post post = new Post(RandomHelper.generateId(now));
        post.setType(req.getInt("type", Post.POST_TEXT));
        post.setAddTo(PeopleIds.parseAddTo(message));
        post.setTo(PeopleIds.forStringIds(PeopleId.USER, req.getStringArray("mentions", ",", new String[]{})));
        post.setUpdatedTime(now);
        post.setCreatedTime(now);
        post.setDestroyedTime(0);
        post.setApp(ctx.getApp());
        post.setAttachments(req.getString("attachments", "[]"));
        post.setPrivate(req.getBoolean("secretly", false));
        post.setMessage(message);
        post.setCanComment(req.getBoolean("can_comment", true));
        post.setCanLike(req.getBoolean("can_like", true));
        post.setCanQuote(req.getBoolean("can_reshare", true));
        post.setDevice(req.getRawUserAgent());
        post.setGeoLocation(ctx.getGeoLocation());
        post.setQuote(0L); // not repost
        post.setSourceId(ctx.getViewer());
        post.setAppData(req.getString("app_data", ""));
        post.setLocation(ctx.getLocation());


        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
        post = stream.expand(ctx, Post.X_FULL_COLUMNS, post);
        resp.body(RawText.of(CompatiblePost.postToJson(post, CompatiblePost.V1_FULL_COLUMNS, true)));
    }

    @Route(url = "/file/delete")
    public void fileDelete(Request req, Response resp) {

        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        String file_ids = req.checkString("file_ids");
        String file_type = req.checkString("file_type");

        boolean b = deleteStaticFiles(ctx, file_type, file_ids);
        resp.body(b);
    }

    @Route(url = "/file/my_files")
    public void fileGetMyShare(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        String fileType = req.getString("file_type", "");
        int page0 = req.getInt("page", 0);
        int count = req.getInt("count", 20);
        if (count > 100)
            count = 100;
        boolean asc = req.getBoolean("asc", false);

        Page page = new Page(page0, count);
        String json = getMyshare(ctx, fileType, asc, page);

        resp.body(RawText.of(json));
    }

    private String getMyshare(Context ctx, String file_type, boolean asc, Page page) {
        if (file_type.equalsIgnoreCase("video")) {
            List<Video> videoList = video.getVideo(ctx, ctx.getViewer(), asc, page);
            return JsonHelper.toJson(videoList, false);
        } else if (file_type.equalsIgnoreCase("audio")) {
            List<Audio> audioList = audio.getAudio(ctx, ctx.getViewer(), asc, page);
            return JsonHelper.toJson(audioList, false);
        } else {
            List<StaticFile> staticFileList = staticFile.getStaticFile(ctx, ctx.getViewer(), asc, page);
            return JsonHelper.toJson(staticFileList, false);
        }
    }

    private boolean deleteStaticFiles(Context ctx, String fileType, String fileIds) {
        if (StringUtils.isEmpty(fileType) || StringUtils.isEmpty(fileIds))
            return false;
        long[] files = StringHelper.splitLongArray(fileIds, ",");
        boolean b = false;
        if (fileType.equalsIgnoreCase("video")) {
            b = video.deleteVideo(ctx, files);
        } else if (fileType.equalsIgnoreCase("audio")) {
            b = audio.deleteAudio(ctx, files);
        } else {
            b = staticFile.deleteStaticFile(ctx, files);
        }
        return b;
    }

    private String uploadFile(Request req, Context ctx, FileItem fileItem) {

        if (fileItem.getSize() > 50 * 1024 * 1024)
            throw new RuntimeException("file size is too large");

        String content_type = req.getString("content_type", "");
        String fileName = req.getString("file_name", "");

        if (StringUtils.isEmpty(fileName))
            fileName = fileItem.getName().substring(fileItem.getName().lastIndexOf("\\") + 1, fileItem.getName().length());
        String expName = "";
        if (fileName.contains(".")) {
            expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        }
        //2,取得文件类型，
        if (StringUtils.isEmpty(content_type))
            content_type = fileItem.getContentType();

        content_type = StaticFile.correctContentType(expName, content_type);
        String new_screen_shot_fileName = "";

        new_screen_shot_fileName = getShotName(req, new_screen_shot_fileName);

        String json = "";
        if (content_type.contains("video/")) {
            json = saveVideo(ctx, req, fileItem, fileName, expName, new_screen_shot_fileName, content_type);
        } else if (content_type.contains("audio/")) {
            json = saveAudio(ctx, req, fileItem, fileName, expName, content_type);
        } else {
            json = saveStaticFile(ctx, req, fileItem, fileName, expName, content_type);
        }
        return json;
    }


    // init and save video
    private String saveVideo(Context ctx, Request req, FileItem fileItem, String fileName, String expName, String new_screen_shot_fileName, String content_type) {
        String newFileName = "";
        String video_id = Long.toString(RandomHelper.generateId());
        if (expName.equals("")) {
            newFileName = ctx.getViewer() + "_" + video_id;
        } else {
            newFileName = ctx.getViewer() + "_" + video_id + "." + expName;
        }
        long time = DateHelper.nowMillis();

        Video video = new Video();
        video.setVideoId(RandomHelper.generateId());
        video.setTitle(fileName);
        video.setSummary(req.getString("summary", ""));
        video.setDescription(req.getString("description", ""));
        video.setFileSize(fileItem.getSize());
        video.setThumbnail(new_screen_shot_fileName);
        video.setUserId(ctx.getViewer());
        video.setExpName(expName);
        video.setContentType(content_type);
        video.setNewFileName(newFileName);
        video.setCreatedTime(time);
        video.setUpdatedTime(time);

        this.video.saveVideo(ctx, video);
        this.video.uploadVideo(ctx, sfsVideo, fileItem, newFileName);

        String key = formatBucketKey(content_type);
        String path = bucketName + "/" + key + video.getUserId() + "/" + video.getNewFileName();
        video.setAddon("file_url", path);

        return video.toJson(Video.STANDARD_COLUMNS, false);
    }

    //init and save audio
    private String saveAudio(Context ctx, Request req, FileItem fileItem, String fileName, String expName, String content_type) {
        String newFileName = "";
        String video_id = Long.toString(RandomHelper.generateId());
        if (expName.equals("")) {
            newFileName = ctx.getViewer() + "_" + video_id;
        } else {
            newFileName = ctx.getViewer() + "_" + video_id + "." + expName;
        }
        long time = DateHelper.nowMillis();

        Audio audio = new Audio();
        audio.setAudioId(RandomHelper.generateId());
        audio.setTitle(fileName);
        audio.setSummary(req.getString("summary", ""));
        audio.setDescription(req.getString("description", ""));
        audio.setFileSize(fileItem.getSize());
        audio.setUserId(ctx.getViewer());
        audio.setExpName(expName);
        audio.setContentType(content_type);
        audio.setNewFileName(newFileName);
        audio.setCreatedTime(time);
        audio.setUpdatedTime(time);

        this.audio.saveAudio(ctx, audio);
        this.audio.uploadAudio(ctx, sfsAudio,fileItem,newFileName);

        String key = formatBucketKey(content_type);
        String path = bucketName + "/" + key + audio.getUserId() + "/" + audio.getNewFileName();
        audio.setAddon("file_url", path);

        return audio.toJson(Audio.STANDARD_COLUMNS, false);
    }

    //init and save staticFile
    private String saveStaticFile(Context ctx, Request req, FileItem fileItem, String fileName, String expName, String content_type) {
        String newFileName = "";
        String video_id = Long.toString(RandomHelper.generateId());
        if (expName.equals("")) {
            newFileName = ctx.getViewer() + "_" + video_id;
        } else {
            newFileName = ctx.getViewer() + "_" + video_id + "." + expName;
        }
        long time = DateHelper.nowMillis();

        StaticFile staticFile = new StaticFile();
        staticFile.setFileId(RandomHelper.generateId());
        staticFile.setTitle(fileName);
        staticFile.setSummary(req.getString("summary", ""));
        staticFile.setDescription(req.getString("description", ""));
        staticFile.setFileSize(fileItem.getSize());
        staticFile.setUserId(ctx.getViewer());
        staticFile.setExpName(expName);
        staticFile.setContentType(content_type);
        staticFile.setNewFileName(newFileName);
        staticFile.setCreatedTime(time);
        staticFile.setUpdatedTime(time);

        this.staticFile.saveStaticFile(ctx, staticFile);
        this.staticFile.uploadStaticFile(ctx, sfsStaticFile,fileItem,newFileName);

        String key = formatBucketKey(content_type);
        String path = bucketName + "/" + key + staticFile.getUserId() + "/" + staticFile.getNewFileName();
        staticFile.setAddon("file_url", path);

        return staticFile.toJson(StaticFile.STANDARD_COLUMNS, false);
    }

    public static String formatBucketKey(String content_type) {
        String key = "";
        if (content_type.contains("video/")) {
            key = bucketName_video_key;
        }
        if (content_type.contains("audio/")) {
            key = bucketName_audio_key;
        }
        if (content_type.contains("text/") || content_type.contains("application/") || content_type.contains("image/")) {
            key = bucketName_static_file_key;
        }
        return key;
    }


    private String getShotName(Request req, String new_screen_shot_fileName) {
        FileItem screen_shot = req.getFile("screen_shot");
        String screen_shot_expName = "";

        if (screen_shot != null && StringUtils.isNotEmpty(screen_shot.getName())) {
            String screen_shot_fileName = screen_shot.getName().substring(screen_shot.getName().lastIndexOf("\\") + 1, screen_shot.getName().length());
            if (screen_shot_fileName.contains(".")) {
                screen_shot_expName = screen_shot_fileName.substring(screen_shot_fileName.lastIndexOf(".") + 1, screen_shot_fileName.length());
            }
            if (!screen_shot_expName.equals(""))
                new_screen_shot_fileName = String.valueOf(DateHelper.nowMillis()) + "." + screen_shot_expName;
            //获取截图url end
        }
        return new_screen_shot_fileName;
    }
}
