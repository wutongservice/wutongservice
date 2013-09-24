package com.borqs.server.wutong.stream;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.*;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import com.borqs.server.wutong.action.ActionLogic;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.photo.PhotoLogic;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.MetadataException;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.borqs.server.wutong.commons.Commons.getDecodeHeader;

public class StreamServlet extends WebMethodServlet {
    private static final Logger L = Logger.getLogger(StreamServlet.class);

    private StaticFileStorage photoStorage;
    private Configuration conf;
    private String qiupuUid;

    @Override
    public void init() throws ServletException {
        super.init();
        conf = GlobalConfig.get();
        qiupuUid = conf.getString("qiupu.uid", "102");
        conf = GlobalConfig.get();
        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
    }

    private void signIn4PostCreate(Context ctx, String userId, String loc) {
        String longitude = Constants.parseLocation(loc, "longitude");
        String latitude = Constants.parseLocation(loc, "latitude");
        String altitude = Constants.parseLocation(loc, "altitude");
        String speed = Constants.parseLocation(loc, "speed");
        String geo = Constants.parseLocation(loc, "geo");
        if (latitude.length() > 0 && latitude.length() > 0) {
            GlobalLogics.getSignIn().signInP(ctx, userId, longitude, latitude, altitude, speed, geo, 2);
        }
    }

    private void attachAlbumInfo(Context ctx, Record album, String photoId, Record sRecord) {
        sRecord.put("album_id", album.getString("album_id"));
        sRecord.put("album_name", album.getString("title"));
        sRecord.put("photo_id", photoId);

        sRecord.put("album_photo_count", 0);
        sRecord.put("album_cover_photo_id", 0);
        sRecord.put("album_description", "");
        sRecord.put("album_visible", false);
    }

    private void attachPhotoImage(Context ctx, Record sRecord, String m, String o, String b, String s, String t) {
        sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), m));
        sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), o));
        sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), b));
        sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), s));
        sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), t));
    }

    private void attachPhotoInfo(Context ctx, Record sRecord, Record rc) {
        sRecord.put("photo_caption", rc.getString("caption"));
        sRecord.put("photo_location", rc.getString("location"));
        sRecord.put("photo_tag", rc.getString("tag"));
        sRecord.put("photo_created_time", rc.getString("created_time"));
        sRecord.put("longitude", rc.getString("longitude"));
        sRecord.put("latitude", rc.getString("latitude"));
        sRecord.put("orientation", rc.getString("orientation"));
    }

    private Record uploadPhoto4PostCreate(Context ctx, FileItem fi, QueryParams qp, List<String> groupIds, Record postMetaData) {
        ElapsedCounter ec = ctx.getElapsedCounter();
        String viewerId = ctx.getViewerIdString();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        PhotoLogic photoLogic = GlobalLogics.getPhoto();
        List<String> pids = new ArrayList<String>();
        String post_id = "";
        Record mock = new Record();
        String fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
        String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());

        if (!fi.getContentType().contains("image/"))
            throw new ServerException(WutongErrors.PHOTO_CONTENT_TYPE_ERROR, "file type error,not image");

        String album_id = photoLogic.getAlbum(ctx, viewerId, photoLogic.ALBUM_TYPE_SHARE_OUT, "Sharing Pictures");
        String path = photoLogic.getPhotoPath(ctx, viewerId, album_id);

        if (!(photoStorage instanceof OssSFS)) {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdir();
            }
        }
        if (!photoLogic.isAlbumExist(ctx, album_id)) {
            throw new ServerException(WutongErrors.PHOTO_ALBUM_NOT_EXISTS, "album not exist, please create album first");
        }

        String photoID = Long.toString(RandomUtils.generateId());
        String caption = qp.getString("caption", "");
        String imageName = viewerId + "_" + album_id + "_" + photoID;

        String longitude = "";
        String latitude = "";
        String orientation = "";
        Record extendExif = new Record();

        ec.record("Begin PhotoJWD");
        if (expName.equalsIgnoreCase("jpg") || expName.equalsIgnoreCase("jpeg")) {
            try {
                extendExif = Commons.getExifGpsFromJpeg(fi);
            } catch (JpegProcessingException e) {
            } catch (MetadataException e) {
            }
            if (!extendExif.isEmpty()) {
                if (extendExif.has("longitude"))
                    longitude = String.valueOf(Commons.formatJWD(extendExif.getString("longitude")));
                if (extendExif.has("latitude"))
                    latitude = String.valueOf(Commons.formatJWD(extendExif.getString("latitude")));
                if (extendExif.has("orientation"))
                    orientation = extendExif.getString("orientation");
            }
        }
        ec.record("End PhotoJWD");

        ec.record("Begin SavePhoto");
        Record rc = new Record();
        rc.put("photo_id", photoID);
        rc.put("album_id", album_id);
        rc.put("user_id", viewerId);
        rc.put("img_middle", imageName + "_O." + expName);
        rc.put("img_original", imageName + "_O." + expName);
        rc.put("img_big", imageName + "_L." + expName);
        rc.put("img_small", imageName + "_S." + expName);
        rc.put("caption", caption);
        rc.put("created_time", DateUtils.nowMillis());
        rc.put("location", loc);
        rc.put("tag", qp.getString("tag", ""));
        rc.put("from_user", viewerId);
        rc.put("original_pid", photoID);
        rc.put("longitude", longitude);
        rc.put("latitude", latitude);
        rc.put("orientation", orientation);

        photoLogic.saveUploadPhoto(ctx, fi, imageName, path, rc);
//            boolean result = photo.saveUploadPhoto(rc);
        RecordSet group_recs = new RecordSet();
        if (!groupIds.isEmpty()) {
            group_recs = GlobalLogics.getPhoto().dealWithGroupPhoto(ctx, rc, groupIds);
            pids.add(photoID);
        }

        boolean result = groupIds.isEmpty() ? photoLogic.saveUploadPhoto(ctx, rc) : photoLogic.saveUploadPhotos(group_recs);
//            boolean result = photo.saveUploadPhoto(rc);
        pids.add(photoID);
        //然后保存在mentions列表中的received相册中
        String tmp_ids = postMetaData.getString("tmp_ids");
        String add_to = postMetaData.getString("add_to");
        List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
        if (add_to.length() > 0) {
            List<String> l01 = StringUtils2.splitList(add_to, ",", true);
            for (String l011 : l01) {
                if (!l00.contains(l011) && l011.length() < Constants.USER_ID_MAX_LEN)
                    l00.add(l011);
            }
        }
        if (l00.size() > 0) {
            try {
                RecordSet rcs = new RecordSet();
                Map<String, String> map = photoLogic.getAlbums(ctx, l00, photoLogic.ALBUM_TYPE_RECEIVED, "Received Pictures");

                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String uid = entry.getKey();
                    String other_album_id = entry.getValue();
                    String path00 = photoLogic.getPhotoPath(ctx, uid, other_album_id);
                    if (!(photoStorage instanceof OssSFS)) {
                        File file0 = new File(path00);
                        if (!file0.exists()) {
                            file0.mkdir();
                        }
                    }

                    Record rc00 = new Record();
                    rc00.put("photo_id", photoID);
                    rc00.put("album_id", other_album_id);
                    rc00.put("user_id", uid);
                    rc00.put("img_middle", imageName + "_O." + expName);
                    rc00.put("img_original", imageName + "_O." + expName);
                    rc00.put("img_big", imageName + "_L." + expName);
                    rc00.put("img_small", imageName + "_S." + expName);
                    rc00.put("caption", "");
                    rc00.put("created_time", DateUtils.nowMillis());
                    rc00.put("location", loc);
                    rc00.put("tag", "");
                    rc00.put("from_user", viewerId);
                    rc00.put("original_pid", photoID);
                    rc00.put("longitude", longitude);
                    rc00.put("latitude", latitude);
                    rc00.put("orientation", orientation);
                    rcs.add(rc00);
//                        pids.add(photoID00);
                }

                photoLogic.saveUploadPhotos(rcs);
            } catch (Exception e) {
            }

        }


        if (result) {
            Record sRecord = new Record();
            Record album = photoLogic.getAlbumOriginal(ctx, album_id);
            attachAlbumInfo(ctx, album, photoID, sRecord);

            if (photoStorage instanceof OssSFS) {
                attachPhotoImage(ctx, sRecord, imageName + "_O." + expName, imageName + "_O." + expName, imageName + "_L." + expName,
                        imageName + "_S." + expName, imageName + "_T." + expName);
            } else {
                String m = viewerId + "/" + album.getString("album_id") + "/" + imageName + "_O." + expName;
                String o = viewerId + "/" + album.getString("album_id") + "/" + imageName + "_O." + expName;
                String b = viewerId + "/" + album.getString("album_id") + "/" + imageName + "_L." + expName;
                String s = viewerId + "/" + album.getString("album_id") + "/" + imageName + "_S." + expName;
                String t = viewerId + "/" + album.getString("album_id") + "/" + imageName + "_T." + expName;
                attachPhotoImage(ctx, sRecord, m, o, b, s, t);
            }

            attachPhotoInfo(ctx, sRecord, rc);

            ec.record("End SavePhoto");

            ec.record("Begin PostUploadPhoto");
            String msg = qp.getString("msg", "");
            String app_data = postMetaData.getString("app_data");
            String mentions = postMetaData.getString("mentions");
            boolean privacy = postMetaData.checkGetBoolean("privacy");
            boolean can_comment = postMetaData.checkGetBoolean("can_comment");
            boolean can_like = postMetaData.checkGetBoolean("can_like");
            boolean can_reshare = postMetaData.checkGetBoolean("can_reshare");
            boolean sendEmail = postMetaData.checkGetBoolean("send_email");
            boolean sendSms = postMetaData.checkGetBoolean("send_sms");
            boolean isTop = postMetaData.checkGetBoolean("is_top");
            long scene = postMetaData.checkGetInt("scene");

            mock = GlobalLogics.getStream().postP(ctx, viewerId, Constants.PHOTO_POST, msg, sRecord.toString(false, false), qp.getString("appid", "1"),
                    "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to, sendEmail, sendSms, isTop,Constants.POST_SOURCE_PEOPLE, scene);
            post_id = mock.checkGetString("post_id");
            if (pids.size() > 0 && !post_id.equals(""))
                photoLogic.updatePhotoStreamId(ctx, post_id, pids);
            ec.record("End PostUploadPhoto");
        }

        return mock;
    }

    private Record sharePhoto4PostCreate(Context ctx, String photo_id, QueryParams qp, List<String> groupIds, Record postMetaData) {
        ElapsedCounter ec = ctx.getElapsedCounter();
        String viewerId = ctx.getViewerIdString();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        PhotoLogic photoLogic = GlobalLogics.getPhoto();
        List<String> pids = new ArrayList<String>();
        String post_id = "";

        Record old_photo = photoLogic.getPhotoByIds(ctx, photo_id).getFirstRecord();
        if (old_photo.isEmpty())
            throw new ServerException(WutongErrors.PHOTO_NOT_EXISTS, "this photo is not exist, author has deleted");
        String album_id = photoLogic.getAlbum(ctx, viewerId, photoLogic.ALBUM_TYPE_SHARE_OUT, "Sharing Pictures");

        Record rc = new Record();
        rc.put("photo_id", photo_id);
        rc.put("album_id", album_id);
        rc.put("user_id", viewerId);
        rc.put("img_middle", old_photo.getString("img_middle"));
        rc.put("img_original", old_photo.getString("img_original"));
        rc.put("img_big", old_photo.getString("img_big"));
        rc.put("img_small", old_photo.getString("img_small"));
        rc.put("caption", old_photo.getString("caption"));
        rc.put("created_time", DateUtils.nowMillis());
        rc.put("location", old_photo.getString("location"));
        rc.put("tag", old_photo.getString("tag"));
        rc.put("longitude", old_photo.getString("longitude"));
        rc.put("latitude", old_photo.getString("latitude"));
        rc.put("orientation", old_photo.getString("orientation"));

        boolean result = groupIds.isEmpty() ? photoLogic.saveUploadPhoto(ctx, rc) : photoLogic.saveUploadPhotos(photoLogic.dealWithGroupPhoto(ctx, rc, groupIds));
        pids.add(photo_id);

        String mentions = postMetaData.getString("mentions");
        String add_to = postMetaData.getString("add_to");
        List<String> l00 = StringUtils2.splitList(mentions, ",", true);
        if (add_to.length() > 0) {
            List<String> l01 = StringUtils2.splitList(add_to, ",", true);
            for (String l011 : l01) {
                if (!l00.contains(l011) && l011.length() < Constants.USER_ID_MAX_LEN)
                    l00.add(l011);
            }
        }
        if (l00.size() > 0) {
            RecordSet rcs = new RecordSet();
            Map<String, String> map = photoLogic.getAlbums(ctx, l00, photoLogic.ALBUM_TYPE_RECEIVED, "Received Pictures");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String uid = entry.getKey();
                String other_album_id = entry.getValue();
                Record rc00 = new Record();
                rc00.put("photo_id", photo_id);
                rc00.put("album_id", other_album_id);
                rc00.put("user_id", uid);
                rc00.put("img_middle", old_photo.getString("img_middle"));
                rc00.put("img_original", old_photo.getString("img_original"));
                rc00.put("img_big", old_photo.getString("img_big"));
                rc00.put("img_small", old_photo.getString("img_small"));
                rc00.put("caption", old_photo.getString("caption"));
                rc00.put("created_time", DateUtils.nowMillis());
                rc00.put("location", old_photo.getString("location"));
                rc00.put("tag", old_photo.getString("tag"));
                rc00.put("longitude", old_photo.getString("longitude"));
                rc00.put("latitude", old_photo.getString("latitude"));
                rc00.put("orientation", old_photo.getString("orientation"));
                rcs.add(rc00);
            }
            photoLogic.saveUploadPhotos(rcs);
        }

        Record sRecord = new Record();
        Record album = photoLogic.getAlbumOriginal(ctx, album_id);
        attachAlbumInfo(ctx, album, photo_id, sRecord);

        if (photoStorage instanceof OssSFS) {
            attachPhotoImage(ctx, sRecord, old_photo.getString("img_original"), old_photo.getString("img_original"), old_photo.getString("img_big"),
                    old_photo.getString("img_small"), old_photo.getString("img_original").replace("O", "T"));
        } else {
            String m = viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original");
            String o = viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original");
            String b = viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_big");
            String s = viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original");
            String t = viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original").replace("O", "T");
            attachPhotoImage(ctx, sRecord, m, o, b, s, t);
        }
        attachPhotoInfo(ctx, sRecord, rc);

        ec.record("Begin PostSharePhoto");
        String msg = qp.getString("msg", "share photo");
        String app_data = postMetaData.getString("app_data");
        boolean privacy = postMetaData.checkGetBoolean("privacy");
        boolean can_comment = postMetaData.checkGetBoolean("can_comment");
        boolean can_like = postMetaData.checkGetBoolean("can_like");
        boolean can_reshare = postMetaData.checkGetBoolean("can_reshare");
        boolean sendEmail = postMetaData.checkGetBoolean("send_email");
        boolean sendSms = postMetaData.checkGetBoolean("send_sms");
        boolean isTop = postMetaData.checkGetBoolean("is_top");
        long scene = postMetaData.checkGetInt("scene");

        Record mock = GlobalLogics.getStream().postP(ctx, viewerId, Constants.PHOTO_POST, msg, sRecord.toString(false, false), qp.getString("appid", "1"),
                "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to, sendEmail, sendSms, isTop,Constants.POST_SOURCE_PEOPLE, scene);
        post_id = mock.checkGetString("post_id");
        if (pids.size() > 0 && !post_id.equals(""))
            photoLogic.updatePhotoStreamId(ctx, post_id, pids);

        ec.record("End PostSharePhoto");

        return mock;
    }

    private Record shareTextOrLink4PostCreate(Context ctx, QueryParams qp, Record postMetaData) {
        ElapsedCounter ec = ctx.getElapsedCounter();
        String viewerId = ctx.getViewerIdString();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        String post_id = "";

        String app_data = postMetaData.getString("app_data");
        boolean privacy = postMetaData.checkGetBoolean("privacy");
        boolean can_comment = postMetaData.checkGetBoolean("can_comment");
        boolean can_like = postMetaData.checkGetBoolean("can_like");
        boolean can_reshare = postMetaData.checkGetBoolean("can_reshare");
        boolean sendEmail = postMetaData.checkGetBoolean("send_email");
        boolean sendSms = postMetaData.checkGetBoolean("send_sms");
        boolean isTop = postMetaData.checkGetBoolean("is_top");
        long scene = postMetaData.checkGetInt("scene");
        String mentions = postMetaData.getString("mentions");
        String add_to = postMetaData.getString("add_to");

        String m = qp.checkGetString("msg");
        m = StringUtils.replace(m, "'", "");
        Pattern pat2 = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\!\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\!\\@?^=%&amp;/~\\+#])?", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pat2.matcher(m);
        Record mock;
        if (!matcher2.find()) {
            ec.record("Begin Share Text");
            mock = GlobalLogics.getStream().postP(ctx, viewerId, (int) qp.getInt("type", 1), qp.checkGetString("msg"), qp.getString("attachments", "[]"), qp.checkGetString("appid"),
                    qp.getString("package", ""), qp.getString("apkId", ""), app_data, mentions, privacy, Constants.QAPK_COLUMNS, ua, loc, "", "", can_comment, can_like, can_reshare, add_to, sendEmail, sendSms, isTop,Constants.POST_SOURCE_PEOPLE, scene);
            ec.record("End Share Text");
        } else {
            ec.record("Begin Share Link");
            String url = matcher2.group();
            if (url.trim().equals(qp.checkGetString("msg").trim()))
                m = "";
            mock = GlobalLogics.getStream().sendShareLinkP(ctx, viewerId, m, qp.checkGetString("appid"),
                    mentions, app_data, privacy, ua, loc, url, "", GlobalConfig.get().getString("platform.servlet.linkImgAddr", ""), can_comment, can_like, can_reshare, add_to, sendEmail, sendSms, isTop, scene);
            ec.record("End Share Link");
        }

        matcher2.reset();

        return mock;
    }

    @WebMethod("post/create")
    public Record createPost(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        L.debug(null,"===begin post create 1========2013-05-03");
        final String METHOD = "post/create";
        Context ctx = WutongContext.getContext(qp, true);
        ElapsedCounter ec = ctx.getElapsedCounter();
//        L.debug(null,"===begin post create 2========2013-05-03");
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        GroupLogic groupLogic = GlobalLogics.getGroup();
        String app_data = qp.getString("app_data", "");
        long scene = qp.getInt("scene", 0L);

        String url = "";
        String ua = ctx.getUa();
        String loc = ctx.getLocation();

        ec.record("Begin SignIn");
        if (!StringUtils.isBlank(loc)) {
            signIn4PostCreate(ctx, viewerId, loc);
        }
        ec.record("End SignIn");

        String post_id = "";
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        boolean sendEmail = qp.getBoolean("send_email", false);
        boolean sendSms = qp.getBoolean("send_sms", false);
        boolean isTop = qp.getBoolean("is_top", false);

        ec.record("Begin AddTo");
        String add_to = Commons.getAddToUserIds(qp.checkGetString("msg"));
        ec.record("End AddTo");

        ec.record("Begin Mentions");
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);
        List<String> groupIds = new ArrayList<String>();
        String tmp_ids = "";
        StringBuilder changeMentions = new StringBuilder();
        if (mentions.length() > 0) {
            privacy = GlobalLogics.getGroup().getUserAndGroup(ctx, changeMentions, mentions, groupIds);
            mentions = changeMentions.toString();
            tmp_ids = Commons.parseUserIds(ctx, viewerId, mentions);
            List<String> l = StringUtils2.splitList(tmp_ids, ",", true);
            if (l.size() > Constants.MAX_GUSY_SHARE_TO)
                throw new ServerException(WutongErrors.STREAM_CANT_SHARE_TOO_MANY_PEOPLE, "Only can share to less than 400 guys!");
        }
        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "You don't have right to post!");
        ec.record("End Mentions");

        Record postMetaData = new Record();
        postMetaData.put("add_to", add_to);
        postMetaData.put("tmp_ids", tmp_ids);
        postMetaData.put("app_data", app_data);
        postMetaData.put("mentions", mentions);
        postMetaData.put("privacy", privacy);
        postMetaData.put("can_comment", can_comment);
        postMetaData.put("can_like", can_like);
        postMetaData.put("can_reshare", can_reshare);
        postMetaData.put("send_email", sendEmail);
        postMetaData.put("send_sms", sendSms);
        postMetaData.put("is_top", isTop);
        postMetaData.put("scene", scene);

        Record mock;
        FileItem fi = qp.getFile("photo_image");
        String photo_id = qp.getString("photo_id", "");

        if (fi != null && StringUtils.isNotEmpty(fi.getName()) && photo_id.equals("")) {
            ec.record("Begin UploadPhoto");
            mock = uploadPhoto4PostCreate(ctx, fi, qp, groupIds, postMetaData);
            ec.record("End UploadPhoto");
        } else if (fi == null && !photo_id.equals("")) {
            ec.record("Begin SharePhoto");
            mock = sharePhoto4PostCreate(ctx, photo_id, qp, groupIds, postMetaData);
            ec.record("End SharePhoto");
        } else {
            ec.record("Begin Share Text or Link");
            mock = shareTextOrLink4PostCreate(ctx, qp, postMetaData);
            ec.record("End Share Text or Link");
        }

        // category
        post_id = mock.checkGetString("post_id");
        if (!post_id.equals("") && !qp.getString("category_id","").equals(""))
            GlobalLogics.getCategory().createCategory(ctx,viewerId,qp.getString("category_id",""),String.valueOf(Constants.POST_OBJECT),post_id);

        ec.record("Begin GetPost");
//        Record post = GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, post_id, true).getFirstRecord();
        ec.record("End GetPost");

        // add by wangpeng at 2013-03-15 Tag Action
        ec.record("Begin ActionTag");
        try{
            JsonNode jn = JsonHelper.parse(app_data);
            if(jn.has("tag_id")){
                ActionLogic action = GlobalLogics.getAction();
                action.sendActionQueue(ctx, mock);
            }
        }catch (Exception e){
            L.error(ctx,e);
        }
        ec.record("End ActionTag");

        L.debug(ctx,"post create end");
        return mock;
    }

    @WebMethod("post/share_apk")
    public Record createApkPost(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        final String METHOD = "post/share_apk";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        Commons commons = new Commons();
        String app_data = qp.getString("app_data", "");

        String apkId = qp.checkGetString("apkId");

        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0) {
                GlobalLogics.getSignIn().signInP(ctx, viewerId, longitude, latitude, altitude, speed, geo, 2);
            }
        }
        String post_id = "";
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        boolean sendEmail = qp.getBoolean("send_email", false);
        boolean sendSms = qp.getBoolean("send_sms", false);
        boolean isTop = qp.getBoolean("is_top", false);
        long scene = qp.getInt("scene", 0L);

        String msg = qp.getString("msg", "");
        msg = StringUtils.replace(msg, "'", "");
        String add_to = "";
        if (msg.length() > 0)
            add_to = commons.getAddToUserIds(msg);
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);

        List<String> groupIds = new ArrayList<String>();
        StringBuilder changeMentions = new StringBuilder();
        privacy = GlobalLogics.getGroup().getUserAndGroup(ctx, changeMentions, mentions, groupIds);
            mentions = changeMentions.toString();
            String ids = commons.parseUserIds(ctx, viewerId, mentions);
            List<String> l = StringUtils2.splitList(ids, ",", true);
            if (l.size() > Constants.MAX_GUSY_SHARE_TO)
                throw new ServerException(WutongErrors.STREAM_CANT_SHARE_TOO_MANY_PEOPLE, "Only can share to less than 400 guys!");


        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "You don't have right to post!");
        Record mock = GlobalLogics.getStream().postP(ctx, viewerId, Constants.APK_POST, msg, "", qp.checkGetString("appid"),
                qp.getString("package", ""), apkId, app_data, mentions, privacy, Constants.QAPK_COLUMNS, ua, loc, "", "", can_comment, can_like, can_reshare, add_to, sendEmail, sendSms, isTop,Constants.POST_SOURCE_PEOPLE, scene);
        post_id = mock.checkGetString("post_id");
        if (!post_id.equals("") && !qp.getString("category_id","").equals(""))
            GlobalLogics.getCategory().createCategory(ctx,viewerId,qp.getString("category_id",""),String.valueOf(Constants.POST_OBJECT),post_id);
//        return GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, post_id, true).getFirstRecord();
        return mock;
    }

    @WebMethod("feedback/create")
    public Record postFeedBack(QueryParams qp, HttpServletRequest req) throws UnsupportedEncodingException {
        final String METHOD = "feedback/create";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        String post_id = "";
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        long scene = qp.getInt("scene", 0L);
        Record mock = GlobalLogics.getStream().postP(ctx, viewerId, 1, qp.checkGetString("msg"), "", qp.checkGetString("appid"),
                "", "", qp.getString("app_data", ""), qiupuUid, true, "", ua, loc, "", "", can_comment, can_like, can_reshare, "", false, false, false,Constants.POST_SOURCE_PEOPLE, scene);
//        return GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, post_id, true).getFirstRecord();
        return mock;
    }


    @WebMethod("link/create")
    public Record createLinkPost(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        final String METHOD = "link/create";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        Commons commons = new Commons();
        String url = qp.checkGetString("url");
        String app_data = qp.getString("app_data", "");
        String title = qp.getString("title", "");
        String msg = qp.getString("msg", "");
        Pattern pat1 = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\!\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\!\\@?^=%&amp;/~\\+#])?", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pat1.matcher(url);
        while (matcher1.find()) {
            url = matcher1.group();
            if (url.length() > 0)
                break;
        }
        matcher1.reset();
        if (url.length() < 5)
            throw new ServerException(WutongErrors.SYSTEM_HTTP_METHOD_NOT_SUPPORT, "url error");

        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0) {
                GlobalLogics.getSignIn().signInP(ctx, viewerId, longitude, latitude, altitude, speed, geo, 2);
            }
        }
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        String add_to = commons.getAddToUserIds(msg);
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);
        boolean sendEmail = qp.getBoolean("send_email", false);
        boolean sendSms = qp.getBoolean("send_sms", false);
        boolean isTop = qp.getBoolean("is_top", false);
        long scene = qp.getInt("scene", 0L);

        List<String> groupIds = new ArrayList<String>();
        StringBuilder changeMentions = new StringBuilder();
        privacy = GlobalLogics.getGroup().getUserAndGroup(ctx, changeMentions, mentions, groupIds);
            mentions = changeMentions.toString();
            String ids = commons.parseUserIds(ctx, viewerId, mentions);
            List<String> l = StringUtils2.splitList(ids, ",", true);
            if (l.size() > Constants.MAX_GUSY_SHARE_TO)
                throw new ServerException(WutongErrors.STREAM_CANT_SHARE_TOO_MANY_PEOPLE, "Only can share to less than 400 guys!");


        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "You don't have right to post!");
        Record mock = GlobalLogics.getStream().sendShareLinkP(ctx, viewerId, msg, qp.checkGetString("appid"),
                mentions, app_data, privacy, ua, loc, url, title, GlobalConfig.get().getString("platform.servlet.linkImgAddr", ""), can_comment, can_like, can_reshare, add_to, sendEmail, sendSms, isTop, scene);
        String post_id = mock.checkGetString("post_id");
        if (!post_id.equals("") && !qp.getString("category_id","").equals(""))
            GlobalLogics.getCategory().createCategory(ctx,viewerId,qp.getString("category_id",""),String.valueOf(Constants.POST_OBJECT),post_id);
//        return GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, post_id, true).getFirstRecord();
        return mock;
    }

    @WebMethod("post/delete")
    public boolean destroyPosts(QueryParams qp) {
        final String METHOD = "post/delete";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();

        List<String> postIds0 = StringUtils2.splitList(qp.checkGetString("postIds"), ",", true);
        if (postIds0.size() == 1) {
            Record rec = GlobalLogics.getStream().findStreamTempP(ctx, postIds0.get(0), "destroyed_time");
            if (rec.isEmpty()) {
                return false;
            } else {
                long destroyed_time = rec.getInt("destroyed_time");
                if (destroyed_time > 0)
                    throw new ServerException(WutongErrors.STREAM_DELETED, "The Post has deleted", postIds0.get(0));
            }
        }

        return GlobalLogics.getStream().destroyPostsP(ctx, viewerId, qp.getString("postIds", ""));
    }

    @WebMethod("post/test")
    public boolean sssss(QueryParams qp) {
        final String METHOD = "post/delete";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getStream().updateAttachment(ctx, viewerId, "");
    }


    @WebMethod("post/repost")
    public Record rePost(QueryParams qp, HttpServletRequest req) throws UnsupportedEncodingException {
        final String METHOD = "post/repost";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        Commons commons = new Commons();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        boolean sendEmail = qp.getBoolean("send_email", false);
        boolean sendSms = qp.getBoolean("send_sms", false);
        boolean isTop = qp.getBoolean("is_top", false);
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        String add_to = commons.getAddToUserIds(qp.getString("newmsg", ""));
        boolean privacy = qp.getBoolean("secretly", false);
        if (privacy == true) {
            qp.checkGetString("to");
        }
        String mentions = qp.getString("to", "");

        List<String> groupIds = new ArrayList<String>();
        StringBuilder changeMentions = new StringBuilder();
        privacy = GlobalLogics.getGroup().getUserAndGroup(ctx, changeMentions, mentions, groupIds);
            mentions = changeMentions.toString();
            String ids = commons.parseUserIds(ctx, viewerId, mentions);
            List<String> l = StringUtils2.splitList(ids, ",", true);
            if (l.size() > Constants.MAX_GUSY_SHARE_TO)
                throw new ServerException(WutongErrors.STREAM_CANT_SHARE_TOO_MANY_PEOPLE, "Only can share to less than 400 guys!");


        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "You don't have right to post!");

        String post_id = GlobalLogics.getStream().repostP(ctx, viewerId, mentions, privacy, qp.checkGetString("postId"), qp.getString("newmsg", ""), ua, loc, qp.getString("app_data", ""), can_comment, can_like, can_reshare, add_to, sendEmail, sendSms, isTop);
        if (!post_id.equals("") && !qp.getString("category_id","").equals(""))
            GlobalLogics.getCategory().createCategory(ctx,viewerId,qp.getString("category_id",""),String.valueOf(Constants.POST_OBJECT),post_id);
        return GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, post_id, true).getFirstRecord();
    }

    @WebMethod("post/update")
    public boolean updatePost(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/update";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getStream().updatePostP(ctx, viewerId, qp.checkGetString("postId"), qp.getString("msg", ""));
    }

    @WebMethod("post/updateaction")
    public boolean updateAction(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/updateaction";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();

        String can_comment = qp.getString("can_comment", null);
        String can_like = qp.getString("can_like", null);
        String can_reshare = qp.getString("can_reshare", null);

        Record rec = new Record();
        if (can_comment != null)
            rec.put("can_comment", can_comment);
        if (can_like != null)
            rec.put("can_like", can_like);
        if (can_reshare != null)
            rec.put("can_reshare", can_reshare);

        return GlobalLogics.getStream().updateStreamCanCommentOrcanLike(ctx, qp.checkGetString("postId"), viewerId, rec);
    }

    @WebMethod("post/get")
    public RecordSet getPosts(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/get";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        return GlobalLogics.getStream().getPostsP(ctx, qp.checkGetString("postIds"), qp.getString("cols", Constants.POST_FULL_COLUMNS));
    }

    @WebMethod("post/qiupuget")
    public RecordSet getPostsForQiuPu(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/qiupuget";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        List<String> postIds0 = StringUtils2.splitList(qp.checkGetString("postIds"), ",", true);
        if (postIds0.size() == 1) {
            Record rec = GlobalLogics.getStream().findStreamTempP(ctx, postIds0.get(0), "destroyed_time");
            if (rec.isEmpty()) {
                return new RecordSet();
            } else {
                long destroyed_time = rec.getInt("destroyed_time");
                if (destroyed_time > 0)
                    throw new ServerException(WutongErrors.STREAM_DELETED, "The Post has deleted", postIds0.get(0));
            }
        }
        boolean single_get = true;
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, qp.checkGetString("postIds"), single_get);
        } else {
            return GlobalLogics.getStream().getPostsForQiuPuP(ctx, viewerId, qp.checkGetString("postIds"), qp.checkGetString("cols"), single_get);
        }
    }

    @WebMethod("post/publictimeline")
    public RecordSet getPublicTimeline(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/publictimeline";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        viewerId = qp.containsKey("ticket") ? viewerId : Constants.NULL_USER_ID;

        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", Constants.ALL_POST);
        String appId = qp.checkGetString("appid");

        if (qp.containsKey("cols")) {
            return GlobalLogics.getStream().getPublicTimelineP(ctx, viewerId, qp.checkGetString("cols"), since, max, type, appId, page, count);
        } else {
            return GlobalLogics.getStream().getFullPublicTimelineP(ctx, viewerId, since, max, type, appId, page, count);
        }
    }

    @WebMethod("post/hot")
    public RecordSet getHotStreams(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/hot";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();

        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        long max = (int) qp.getInt("end_time", 0);
        long min = (int) qp.getInt("start_time", 0);
        int type = (int) qp.getInt("type", Constants.ALL_POST);

        if (max == 0)
            max = DateUtils.nowMillis();
        if (min == 0) {
            long dateDiff = 24 * 60 * 60 * 1000 * 30L;
            min = max - dateDiff;
        }

        if (count > 100)
            count = 100;
        String circle_id = qp.getString("circle_id", "");
        return GlobalLogics.getStream().getHotStream(ctx, viewerId, circle_id, qp.getString("cols", ""), type, max, min, page, count);
    }

    @WebMethod("post/qiupupublictimeline")
    public RecordSet getQiupuPublicTimeline(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/qiupupublictimeline";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        viewerId = qp.containsKey("ticket") ? viewerId : Constants.NULL_USER_ID;
        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", Constants.ALL_POST);
        String appId = qp.checkGetString("appid");

        if (qp.containsKey("cols")) {
            return GlobalLogics.getStream().getPublicTimelineForQiuPuP(ctx, viewerId, qp.checkGetString("cols"), since, max, type, appId, page, count);
        } else {
            return GlobalLogics.getStream().getFullPublicTimelineForQiuPuP(ctx, viewerId, since, max, type, appId, page, count);
        }
    }

//    @WebMethod("post/updateacctchments")
//    public boolean updateAcctchment() throws AvroRemoteException {
//        Platform p = platform();
//        Qiupu q = qiupu();
//        return p.updatePostAttachments(q.QAPK_COLUMNS);
//    }

    @WebMethod("post/userstimeline")
    public RecordSet getUsersTimeline(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/userstimeline";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        long maxt = qp.getInt("end_time", 0) <= 0 ? DateUtils.nowMillis() : qp.getInt("end_time", 0);
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return GlobalLogics.getStream().getFullUsersTimelineP(ctx, viewerId, qp.checkGetString("users"), qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        } else {
            return GlobalLogics.getStream().getUsersTimelineP(ctx, viewerId, qp.checkGetString("users"), qp.checkGetString("cols"), qp.getInt("start_time", 0), qp.getInt("max", DateUtils.nowMillis()), (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        }
    }
    @WebMethod("post/qiupuusertimeline_home")
        public RecordSet getUsersTimeHomelineForQiuPu(QueryParams qp) throws AvroRemoteException {
            final String METHOD = "post/qiupuusertimeline_home";
            Context ctx = WutongContext.getContext(qp, false);
            L.traceStartCall(ctx, METHOD, qp.toString());
            String viewerId = ctx.getViewerIdString();
            long maxt = qp.getInt("end_time", 0) <= 0 ? DateUtils.nowMillis() : qp.getInt("end_time", 0);


            // add at 2013-04-18
            ctx.putSession("scene",qp.getString("scene","10000000072"));
            // contain mentions =''
            ctx.putSession("from_home", true);

            String category_id = qp.getString("category_id", "");
            if (!category_id.equals("")) {
                RecordSet c_recs = GlobalLogics.getCategory().getCategories(ctx, category_id, qp.getInt("start_time", 0), maxt, (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
                String postIds = c_recs.joinColumnValues("target_id", ",");
                if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
                    return GlobalLogics.getStream().getPostsForQiuPuP(ctx, ctx.getViewerIdString(), postIds, Constants.POST_FULL_COLUMNS, false);
                } else {
                    return GlobalLogics.getStream().getPostsForQiuPuP(ctx, ctx.getViewerIdString(), postIds, qp.getString("cols", ""), false);
                }
            } else {
                String userIds = qp.checkGetString("users");
                // ------------- add by wangpeng  filter timeLine ------------

                if (StringUtils2.splitSet(userIds, ",", true).size() == 1)
                {
                    GroupLogic g = GlobalLogics.getGroup();
                    if (g.isPublicCircle(ctx, Long.parseLong(userIds))) {
                        Record circle = g.getSimpleGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END,
                                userIds, "formal,circle_ids,event_ids").getFirstRecord();
                        if (circle.getInt("formal", Constants.PUBLIC_CIRCLE_FORMAL_FREE) == Constants.PUBLIC_CIRCLE_FORMAL_TOP) {
                            String subCircleIds = circle.getString("circle_ids", "");
                            String subEventIds = circle.getString("event_ids", "");
                            String subIds = subCircleIds;
                            if (StringUtils.isNotBlank(subEventIds)) {
                                if (StringUtils.isBlank(subIds))
                                    subIds = subEventIds;
                                else
                                    subIds += "," + subEventIds;
                            }
                            if (StringUtils.isNotBlank(subIds)) {
                                Map<Long, Integer> map = g.getRolesWithGroups(ctx, StringUtils2.splitIntArray(subIds, ","), ctx.getViewerId());
                                for (Map.Entry<Long, Integer> entry : map.entrySet()) {
                                    long circleId = entry.getKey();
                                    int role = entry.getValue();
                                    if (role >= Constants.ROLE_MEMBER)
                                        userIds += "," + circleId;
                                }
                            }
                        }
                    }
                }
                if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
                    return GlobalLogics.getStream().getFullUsersTimelineForQiuPuP(ctx, viewerId, userIds, qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
                } else {
                    return GlobalLogics.getStream().getUsersTimelineForQiuPuP(ctx, viewerId, userIds, qp.checkGetString("cols"), qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
                }
            }
        }
    @WebMethod("post/qiupuusertimeline")
    public RecordSet getUsersTimelineForQiuPu(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/qiupuusertimeline";
        Context ctx = WutongContext.getContext(qp, false);
        ElapsedCounter ec = ctx.getElapsedCounter();
        ec.record("qiupuusertimeline api START");

        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        long maxt = qp.getInt("end_time", 0) <= 0 ? DateUtils.nowMillis() : qp.getInt("end_time", 0);


        // add at 2013-04-18
        //boolean from_home = qp.getBoolean("from_home", false);
        ctx.putSession("scene",qp.getString("scene","10000000072"));
        // not contain mentions =''

        // TODO 等客户端完成后需要修改为 false
        ctx.putSession("from_home", true);

        String category_id = qp.getString("category_id", "");
        if (!category_id.equals("")) {
            ec.record("GET CATEGORY");
            RecordSet c_recs = GlobalLogics.getCategory().getCategories(ctx, category_id, qp.getInt("start_time", 0), maxt, (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
            String postIds = c_recs.joinColumnValues("target_id", ",");
            ec.record("END CATEGORY");

            RecordSet result ;
            if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
                result =  GlobalLogics.getStream().getPostsForQiuPuP(ctx, ctx.getViewerIdString(), postIds, Constants.POST_FULL_COLUMNS, false);
                ec.record("qiupuusertimeline api end");
                ////L.info(ctx,"----------------------ElapsedCounter--------------------------"+ec.getTotalTime());
                return result;
            } else {

                result  =  GlobalLogics.getStream().getPostsForQiuPuP(ctx, ctx.getViewerIdString(), postIds, qp.getString("cols", ""), false);
                ec.record("qiupuusertimeline api end");
                //L.info(ctx,"----------------------ElapsedCounter--------------------------"+ec.getTotalTime());
                return result;
            }
        } else {
            String userIds = qp.checkGetString("users");
            ctx.putSession("users_or_circle",userIds);
            // ------------- add by wangpeng  filter timeLine ------------

            if (StringUtils2.splitSet(userIds, ",", true).size() == 1)
            {
                GroupLogic g = GlobalLogics.getGroup();
                if (g.isPublicCircle(ctx, Long.parseLong(userIds))) {
                    ec.record("START QUERY GROUP");
                    Record circle = g.getSimpleGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END,
                            userIds, "formal,circle_ids,event_ids").getFirstRecord();
                    if (circle.getInt("formal", Constants.PUBLIC_CIRCLE_FORMAL_FREE) == Constants.PUBLIC_CIRCLE_FORMAL_TOP) {
                        String subCircleIds = circle.getString("circle_ids", "");
                        String subEventIds = circle.getString("event_ids", "");
                        String subIds = subCircleIds;
                        if (StringUtils.isNotBlank(subEventIds)) {
                            if (StringUtils.isBlank(subIds))
                                subIds = subEventIds;
                            else
                                subIds += "," + subEventIds;
                        }
                        if (StringUtils.isNotBlank(subIds)) {
                            Map<Long, Integer> map = g.getRolesWithGroups(ctx, StringUtils2.splitIntArray(subIds, ","), ctx.getViewerId());
                            for (Map.Entry<Long, Integer> entry : map.entrySet()) {
                                long circleId = entry.getKey();
                                int role = entry.getValue();
                                if (role >= Constants.ROLE_MEMBER)
                                    userIds += "," + circleId;
                            }
                        }
                    }
                    ec.record("END QUERY GROUP");
                }
            }

            RecordSet result;
            if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
                ec.record("START getFullUsersTimelineForQiuPuP");
                result =  GlobalLogics.getStream().getFullUsersTimelineForQiuPuP(ctx, viewerId, userIds, qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
                ec.record("END getFullUsersTimelineForQiuPuP");
                //L.info(ctx,"----------------------ElapsedCounter--------------------------"+ec.getTotalTime());
                return result;
            } else {
                ec.record("START getUsersTimelineForQiuPuP ");
                result =  GlobalLogics.getStream().getUsersTimelineForQiuPuP(ctx, viewerId, userIds, qp.checkGetString("cols"), qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
                ec.record("END getUsersTimelineForQiuPuP");
                //L.info(ctx,"----------------------ElapsedCounter--------------------------"+ec.getTotalTime());
                return result;
            }
        }

    }

    @WebMethod("post/related_timeline")
    public RecordSet getRelatedTimeline(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String userId = qp.getString("user_id", ctx.getViewerIdString());
        long maxt = qp.getInt("end_time", 0) <= 0 ? DateUtils.nowMillis() : qp.getInt("end_time", 0);

        String circleIds = qp.getString("circle_ids", "");
        if (StringUtils2.splitSet(circleIds, ",", true).size() == 1)
        {
            GroupLogic g = GlobalLogics.getGroup();
            if (g.isPublicCircle(ctx, Long.parseLong(circleIds))) {
                Record circle = g.getSimpleGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END,
                        circleIds, "formal,circle_ids,event_ids").getFirstRecord();
                if (circle.getInt("formal", Constants.PUBLIC_CIRCLE_FORMAL_FREE) == Constants.PUBLIC_CIRCLE_FORMAL_TOP) {
                    String subCircleIds = circle.getString("circle_ids", "");
                    String subEventIds = circle.getString("event_ids", "");
                    String subIds = subCircleIds;
                    if (StringUtils.isNotBlank(subEventIds)) {
                        if (StringUtils.isBlank(subIds))
                            subIds = subEventIds;
                        else
                            subIds += "," + subEventIds;
                    }
                    if (StringUtils.isNotBlank(subIds)) {
                        Map<Long, Integer> map = g.getRolesWithGroups(ctx, StringUtils2.splitIntArray(subIds, ","), ctx.getViewerId());
                        for (Map.Entry<Long, Integer> entry : map.entrySet()) {
                            long circleId = entry.getKey();
                            int role = entry.getValue();
                            if (role >= Constants.ROLE_MEMBER)
                                circleIds += "," + circleId;
                        }
                    }
                }
            }
        }

        List<String> groupIds = StringUtils2.splitList(circleIds, ",", true);
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return GlobalLogics.getStream().getFullRelatedTimelineForQiupuP(ctx, userId, qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20), groupIds);
        } else {
            return GlobalLogics.getStream().getRelatedTimelineForQiupuP(ctx, userId, qp.checkGetString("cols"), qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20), groupIds);
        }
    }

    @WebMethod("post/myshare")
    public RecordSet getMyShare(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/myshare";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        long max = qp.getInt("end_time", 0) <= 0 ? DateUtils.nowMillis() : qp.getInt("end_time", 0);
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return GlobalLogics.getStream().getMyShareFullTimelineP(ctx, viewerId, qp.checkGetString("users"), qp.getInt("start_time", 0), max, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        } else {
            return GlobalLogics.getStream().getMyShareTimelineP(ctx, viewerId, qp.checkGetString("users"), qp.checkGetString("cols"), qp.getInt("start_time", 0), max, (int) qp.getInt("type", Constants.ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        }

    }


    @WebMethod("post/friendtimeline")
    public RecordSet getFriendsTimeline(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/friendtimeline";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", Constants.ALL_POST);
        String appId = qp.checkGetString("appid");
        if (qp.containsKey("cols")) {
            return GlobalLogics.getStream().getFriendsTimelineP(ctx, viewerId, qp.getString("circleIds", ""), qp.checkGetString("cols"), since, max, type, appId, page, count);
        } else {
            return GlobalLogics.getStream().getFullFriendsTimelineP(ctx, viewerId, qp.getString("circleIds", ""), since, max, type, appId, page, count);
        }
    }

    @WebMethod("post/qiupufriendtimeline")
    public RecordSet getFriendTimelineForQiuPu(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/qiupufriendtimeline";
        Context ctx = WutongContext.getContext(qp, true);
        //add at 2013-04-17
        ctx.putSession("scene",qp.getString("scene","10000000072"));

        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", Constants.ALL_POST);
        String appId = qp.checkGetString("appid");
        if (qp.containsKey("cols")) {
            return GlobalLogics.getStream().getFriendsTimelineForQiuPuP(ctx, viewerId, viewerId, qp.getString("circleIds", String.valueOf(Constants.FRIENDS_CIRCLE)), qp.checkGetString("cols"), since, max, type, appId, page, count);
        } else {
            return GlobalLogics.getStream().getFullFriendsTimelineForQiuPuP(ctx, viewerId, viewerId, qp.getString("circleIds", String.valueOf(Constants.FRIENDS_CIRCLE)), since, max, type, appId, page, count);
        }
    }

    @WebMethod("post/nearby")
    public RecordSet getPostNearBy(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        final String METHOD = "post/nearby";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", Constants.ALL_POST);
        String appId = qp.checkGetString("appid");

        String loc = getDecodeHeader(req, "location", "", viewerId);
        String longitude_me = Constants.parseLocation(loc, "longitude");
        String latitude_me = Constants.parseLocation(loc, "latitude");
        int distance = (int) qp.getInt("distance", 1000);

        if (longitude_me.equals("") || latitude_me.equals(""))
            throw new ServerException(WutongErrors.COMMON_GEO_ERROR, "want Correct location");
        return GlobalLogics.getStream().getNearByStreamP(ctx, viewerId, qp.getString("cols", ""), since, max, type, appId, page, count, loc, distance);
    }


    @WebMethod("post/canlike")
    public boolean postCanLike(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/canlike";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getStream().postCanLikeP(ctx, qp.checkGetString("postId"));
    }

    @WebMethod("post/cancomment")
    public boolean postCanComment(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/cancomment";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getStream().postCanCommentP(ctx, qp.checkGetString("postId"));
    }

    @WebMethod("post/commented")
    public RecordSet getCommentedPosts(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/commented";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getComment().getCommentedPostsP(ctx, viewerId, qp.getString("cols", ""), (int) qp.getInt("objectType", 2), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
    }

    @WebMethod("post/liked")
    public RecordSet getLikedPosts(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "post/liked";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getLike().getLikedPostsP(ctx, viewerId, qp.getString("cols", ""), (int) qp.getInt("objectType", 2), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
    }

    private String groupTopPostsSet(Context ctx, long groupId, QueryParams qp) {
        String viewerId = ctx.getViewerIdString();
        RecordSet recs = GlobalLogics.getGroup().getSimpleGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END, String.valueOf(groupId), Constants.COMM_COL_TOP_POSTS);
        String oldPostIds = recs.get(0).getString(Constants.COMM_COL_TOP_POSTS);
        Set<String> postIds = StringUtils2.splitSet(oldPostIds, ",", true);
        String set = qp.getString("set", "");
        String unset = qp.getString("unset", "");
        Set<String> sl = StringUtils2.splitSet(set, ",", true);
        Set<String> ul = StringUtils2.splitSet(unset, ",", true);
        postIds.addAll(sl);
        postIds.removeAll(ul);
        String topPosts = StringUtils2.joinIgnoreBlank(",", postIds);

        Record info = new Record();
        Record properties = new Record();
        properties.put(Constants.COMM_COL_TOP_POSTS, topPosts);
        boolean result = GlobalLogics.getGroup().updateGroup(ctx, groupId, info, properties);
        if (result)
            return topPosts;
        else
            return oldPostIds;
    }

    private String accountTopPostsSet(Context ctx, QueryParams qp) {
        String viewerId = ctx.getViewerIdString();
        RecordSet recs = GlobalLogics.getAccount().getUsers(ctx, viewerId, viewerId, "top_posts", false, false);
        String oldPostIds = recs.get(0).getString("top_posts");
        Set<String> postIds = StringUtils2.splitSet(oldPostIds, ",", true);
        String set = qp.getString("set", "");
        String unset = qp.getString("unset", "");
        Set<String> sl = StringUtils2.splitSet(set, ",", true);
        Set<String> ul = StringUtils2.splitSet(unset, ",", true);
        postIds.addAll(sl);
        postIds.removeAll(ul);
        String topPosts = StringUtils2.joinIgnoreBlank(",", postIds);

        boolean result = GlobalLogics.getAccount().updateAccount(ctx, viewerId, Record.of("top_posts", topPosts));
        if (result)
            return topPosts;
        else
            return oldPostIds;
    }

    @WebMethod("post/top_posts_set")
    public String circleTopPostsSet(QueryParams qp) {
        final String METHOD = "post/top_posts_set";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        long id = qp.checkGetInt("id");
        if (id >= Constants.PUBLIC_CIRCLE_ID_BEGIN && id <= Constants.GROUP_ID_END)
            return groupTopPostsSet(ctx, id, qp);
        else
            return accountTopPostsSet(ctx, qp);
    }

    private RecordSet groupTopPostsGet(Context ctx, long groupId, QueryParams qp) {
        String viewerId = ctx.getViewerIdString();
        RecordSet recs = GlobalLogics.getGroup().getSimpleGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END, String.valueOf(groupId), Constants.COMM_COL_TOP_POSTS);
        String postIds = recs.get(0).getString(Constants.COMM_COL_TOP_POSTS);
        boolean single_get = true;
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, postIds, single_get);
        } else {
            return GlobalLogics.getStream().getPostsForQiuPuP(ctx, viewerId, postIds, qp.checkGetString("cols"), single_get);
        }
    }

    private RecordSet accountTopPostsGet(Context ctx, String userId, QueryParams qp) {
        String viewerId = ctx.getViewerIdString();
        RecordSet recs = GlobalLogics.getAccount().getUsers(ctx, userId, userId, "top_posts", false, false);
        String postIds = recs.get(0).getString("top_posts");
        boolean single_get = true;
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, postIds, single_get);
        } else {
            return GlobalLogics.getStream().getPostsForQiuPuP(ctx, viewerId, postIds, qp.checkGetString("cols"), single_get);
        }
    }

    @WebMethod("post/top_posts_get")
    public RecordSet circleTopPostsGet(QueryParams qp) {
        final String METHOD = "post/top_posts_get";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        long id = qp.checkGetInt("id");
        if (id >= Constants.PUBLIC_CIRCLE_ID_BEGIN && id <= Constants.GROUP_ID_END)
            return groupTopPostsGet(ctx, id, qp);
        else
            return accountTopPostsGet(ctx, String.valueOf(id), qp);
    }

    @WebMethod("post/mute")
    public boolean mute(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        int targetType = (int)qp.getInt("object_type", Constants.POST_OBJECT);
        String targetId = qp.checkGetString("object_id");
        ConversationLogic c = GlobalLogics.getConversation();
        int enabled = c.getEnabled(ctx, targetType, targetId);
        if (enabled == 1) {
            return c.enableConversion(ctx, targetType, targetId, 0);
        } else if (enabled == 0) {
            return c.enableConversion(ctx, targetType, targetId, 1);
        } else {
            return c.createConversationP(ctx, targetType, targetId, Constants.C_SUBSCRIBE_STREAM, ctx.getViewerIdString());
        }
    }

//    @WebMethod("post/category_get")
//    public RecordSet categoryPostsGet(QueryParams qp) {
//        final String METHOD = "post/category_get";
//        Context ctx = WutongContext.getContext(qp, true);
//        L.traceStartCall(ctx, METHOD, qp.toString());
//        String category_id = qp.checkGetString("category_id");
//
//        RecordSet target_recs = GlobalLogics.getCategory().getCategories(ctx,category_id);
//        String postIds = target_recs.joinColumnValues("target_id",",");
//        RecordSet out_recs = GlobalLogics.getStream().getPostsForQiuPuP(ctx,ctx.getViewerIdString(),postIds,Constants.POST_FULL_COLUMNS,false);
//        return out_recs;
//    }

    @WebMethod("post/update_old_photo")
    public int mqqqqqqqqqute(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        return GlobalLogics.getStream().updatePhotoOld(ctx);
    }
}
