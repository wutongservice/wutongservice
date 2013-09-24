package com.borqs.server.wutong.photo;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.comment.CommentLogic;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.like.LikeLogic;
import com.borqs.server.wutong.page.PageLogicUtils;
import com.borqs.server.wutong.signin.SignInLogic;
import com.borqs.server.wutong.stream.StreamLogic;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhotoServlet extends WebMethodServlet {
    private StaticFileStorage photoStorage;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = GlobalConfig.get();
        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
    }

    @WebMethod("album/create")
    public boolean createAlbum(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String userId = ctx.getViewerIdString();
        PhotoLogic photo = GlobalLogics.getPhoto();
        String album_name = qp.checkGetString("title");
        int visible = (int) qp.getInt("privacy", 0);         //0 open 1 only me 2 friend open
        String description = qp.getString("summary", "");
        String album_id = Long.toString(RandomUtils.generateId());
        Record rc = new Record();

        rc.put("album_id", album_id);
        rc.put("album_type", photo.ALBUM_TYPE_OTHERS);
        rc.put("user_id", userId);
        rc.put("title", album_name);
        rc.put("created_time", DateUtils.nowMillis());
        rc.put("summary", description);
        rc.put("privacy", visible);
        photo.createAlbum(ctx, rc);
        return true;
    }


    @WebMethod("album/all")
    public RecordSet getAlbums(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photo = GlobalLogics.getPhoto();

        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        String ua = ctx.getUa();
        String userId = qp.getString("user_id", viewerId);
        RecordSet recs = photo.getUserAlbum(ctx, viewerId, userId);
        //L.debug("album/all:recs=" + recs.toString());
        for (Record rec : recs) {
            rec = addAlbumLastedPhoto(ctx, viewerId, rec, rec.getString("album_id"));
            rec.put("title", formatAlbumName(ua, (int) rec.getInt("album_type"), rec.getString("title")));
        }
        //L.debug("album/all:recs new=" + recs.toString());
        return recs;
    }

    public String formatAlbumName(String ua, int album_type, String album_name) {
        PhotoLogic photo = GlobalLogics.getPhoto();
        if (album_type == photo.ALBUM_TYPE_PROFILE)
            album_name = Constants.getBundleString(ua, "album.name.profile");
        if (album_type == photo.ALBUM_TYPE_SHARE_OUT)
            album_name = Constants.getBundleString(ua, "album.name.sharing");
        if (album_type == photo.ALBUM_TYPE_COVER)
            album_name = Constants.getBundleString(ua, "album.name.cover");
        if (album_type == photo.ALBUM_TYPE_RECEIVED)
            album_name = Constants.getBundleString(ua, "album.name.received");
        if (album_type == photo.ALBUM_TYPE_MY_SYNC)
            album_name = Constants.getBundleString(ua, "album.name.cloud");
        return album_name;
    }

    @WebMethod("album/get")
    public Record getAlbumById(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photo = GlobalLogics.getPhoto();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        String ua = ctx.getUa();
        String userId = qp.getString("user_id", viewerId);
        String album_id = qp.checkGetString("album_id");

        Record rec = photo.getAlbumById(ctx, viewerId, userId, album_id);
        rec = addAlbumLastedPhoto(ctx, viewerId, rec, album_id);
        rec.put("title", formatAlbumName(ua, (int) rec.getInt("album_type"), rec.getString("title")));
        return rec;
    }

    public Record addAlbumLastedPhoto(Context ctx, String viewerId, Record rec, String album_id) {
        PhotoLogic photo = GlobalLogics.getPhoto();
        Record lp = photo.getLatestPhoto(ctx, viewerId, album_id);
        Configuration conf = getConfiguration();
        if (!lp.isEmpty()) {
            rec.put("album_cover_photo_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), lp.getString("img_middle")));
            rec.put("album_cover_photo_original", String.format(conf.checkGetString("platform.photoUrlPattern"), lp.getString("img_middle")));
            rec.put("album_cover_photo_big", String.format(conf.checkGetString("platform.photoUrlPattern"), lp.getString("img_big")));
            rec.put("album_cover_photo_small", String.format(conf.checkGetString("platform.photoUrlPattern"), lp.getString("img_small")));
            rec.put("orientation", lp.getString("orientation"));
        } else {
            rec.put("album_cover_photo_middle", "");
            rec.put("album_cover_photo_original", "");
            rec.put("album_cover_photo_big", "");
            rec.put("album_cover_photo_small", "");
        }
        return rec;
    }

    @WebMethod("album/update")
    public boolean updateAlbum(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photo = GlobalLogics.getPhoto();
        String viewerId = ctx.getViewerIdString();

        String album_id = qp.checkGetString("album_id");
        Record r = photo.getAlbumOriginal(ctx, album_id);
        int album_type = (int) r.getInt("album_type");
        if (album_type != photo.ALBUM_TYPE_OTHERS)
            throw new ServerException(WutongErrors.PHOTO_ALBUM_TYPE_ERROR, "only can update user album");
        if (!viewerId.equals(r.getString("user_id")))
            throw new ServerException(WutongErrors.PHOTO_CANT_ACTION, "can't update other album");
        String album_name = qp.getString("title", null);
        String description = qp.getString("summary", null);
        String visible = qp.getString("privacy", null);

        if (!StringUtils.isNotBlank(visible)) {
            if (!visible.equals("0") && !visible.equals("1") && !visible.equals("2"))
                throw new ServerException(WutongErrors.PHOTO_PRIVACY_TYPE_ERROR, "privacy error, privacy must be 0,1,2");
        }

        Record rc = new Record();
        if (StringUtils.isNotBlank(album_name)) {
            rc.put("title", album_name);
        }
        if (StringUtils.isNotBlank(description)) {
            rc.put("summary", description);
        }
        if (StringUtils.isNotBlank(visible)) {
            rc.put("privacy", Integer.valueOf(visible));
        }

        return photo.updateAlbum(ctx, album_id, rc);
    }

    @WebMethod("album/delete")
    public boolean deleteAlbum(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        PhotoLogic photo = GlobalLogics.getPhoto();

        String album_id = qp.checkGetString("album_id");
        Record album = photo.getAlbumOriginal(ctx, album_id);
        if (album.getInt("album_type") != photo.ALBUM_TYPE_OTHERS)
            throw new ServerException(WutongErrors.PHOTO_CANT_ACTION, "can't delete this album");
        if (!viewerId.equals(album.getString("user_id")))
            throw new ServerException(WutongErrors.PHOTO_CANT_ACTION, "can't delete other album");
        return photo.deleteAlbumById(ctx, viewerId, album_id, PhotoLogic.bucketName_photo_key, photoStorage);
    }

    @WebMethod("photo/get")
    public RecordSet getPhotoByIds(QueryParams qp, HttpServletResponse resp) {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photo = GlobalLogics.getPhoto();

        String viewerId = ctx.getViewerIdString();
        RecordSet recs = photo.getPhotoByIds(ctx, qp.checkGetString("photo_ids"));
        boolean need_comments = qp.getBoolean("need_comments",true);
        boolean need_likes = qp.getBoolean("need_likes",true);
        Configuration conf = getConfiguration();
        for (Record rec : recs) {
            rec = formatPhotoUrlAndExtend(ctx, viewerId, rec, conf,need_comments,need_likes);
        }
        return recs;
    }

    @WebMethod("photo/album_get")
    public RecordSet getPhotoByAlbumIds(QueryParams qp, HttpServletResponse resp) {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photo = GlobalLogics.getPhoto();
        String viewerId = ctx.getViewerIdString();

        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        boolean need_comments = qp.getBoolean("need_comments",false);
        boolean need_likes = qp.getBoolean("need_likes",false);
        List<String> album_ids0 = StringUtils2.splitList(qp.checkGetString("album_ids"), ",", true);
//        Record album = photo.getAlbumOriginal(ctx, album_ids0.get(0));
//        String user_id = album.getString("user_id");
        //        String cols = "photo_id,album_id,user_id,img_middle,img_original,img_big,img_small,caption,created_time,location,tag,tag_ids,from_user,original_pid,longitude,latitude,orientation,stream_id,privacy";
        RecordSet recs = photo.getAlbumPhotos(ctx, viewerId, album_ids0.get(0), page, count);
        Configuration conf = getConfiguration();
        for (Record rec : recs) {
            rec = formatPhotoUrlAndExtend(ctx, viewerId, rec, conf,need_comments,need_likes);
        }
        return recs;
    }

    @WebMethod("photo/update")
    public boolean updatePhoto(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photo = GlobalLogics.getPhoto();
        String photo_id = qp.checkGetString("photo_id");
        String caption = qp.getString("caption", null);
        String location = qp.getString("location", null);
        Record rc = new Record();
        if (caption != null) {
            rc.put("caption", caption);
        }
        if (location != null) {
            rc.put("location", location);
        }
        return photo.updatePhoto(ctx, photo_id, rc);
    }

    @WebMethod("photo/delete")
    public boolean deletePhoto(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        PhotoLogic photo = GlobalLogics.getPhoto();
        String pIDs = qp.checkGetString("photo_ids");
        boolean delete_all = qp.getBoolean("delete_all", false);
        return photo.deletePhotoById(ctx, viewerId, pIDs, delete_all, PhotoLogic.bucketName_photo_key, photoStorage);
    }

    @WebMethod("photo/tag")
    public Record tagPhoto(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        PhotoLogic photo = GlobalLogics.getPhoto();

        String photo_id = qp.checkGetString("photo_id");
        String tagUserId = qp.checkGetString("user_id");
        boolean need_comments = qp.getBoolean("need_comments",false);
        boolean need_likes = qp.getBoolean("need_likes",false);
        String tagText = qp.checkGetString("tag_text");
        boolean addTag = qp.getBoolean("add_tag", true);
        int top = 0;
        int left = 0;
        int frame_width = 0;
        int frame_height = 0;
        if (addTag) {
            top = (int) qp.checkGetInt("top");
            left = (int) qp.checkGetInt("left");
            frame_width = (int) qp.checkGetInt("frame_width");
            frame_height = (int) qp.checkGetInt("frame_height");
        }
        Record record = photo.tagPhoto(ctx, photo_id, top, left, frame_width, frame_height, tagUserId, tagText, addTag);
        Configuration conf = getConfiguration();
        record = formatPhotoUrlAndExtend(ctx, viewerId, record, conf,need_comments,need_likes);
        return record;
    }

    @WebMethod("photo/update_stream_id")
    public boolean updatePhotoStreamId(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photo = GlobalLogics.getPhoto();
        photo.updatePhotoStreamId(ctx, (int) qp.getInt("album_type", 1), qp.getString("asc", "DESC"));

        return true;
    }

    @WebMethod("photo/include_me")
    public RecordSet photoContainsMe(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        PhotoLogic photo = GlobalLogics.getPhoto();

        String user_id = qp.getString("user_id", viewerId);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        boolean need_comments = qp.getBoolean("need_comments",false);
        boolean need_likes = qp.getBoolean("need_likes",false);
        RecordSet recs = photo.getPhotosIncludeMe(ctx, viewerId, user_id, page, count);
        Configuration conf = getConfiguration();
        for (Record rec : recs) {
            rec = formatPhotoUrlAndExtend(ctx, viewerId, rec, conf,need_comments,need_likes);
        }
        return recs;
    }

    public static boolean isNumeric(String str) {
        for (int i = str.length(); --i >= 0; ) {
            int chr = str.charAt(i);
            if (chr < 48 || chr > 57)
                return false;
        }
        return true;
    }

    public Record formatPhotoUrlAndExtend(Context ctx, String viewerId, Record rec, Configuration conf,boolean need_comments,boolean need_likes) {
        AccountLogic accountLogic = GlobalLogics.getAccount();
        LikeLogic likeLogic = GlobalLogics.getLike();
        CommentLogic commentLogic = GlobalLogics.getComment();

        if (!rec.isEmpty()) {
            rec.put("photo_url_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_middle")));
            rec.put("photo_url_original", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_original")));
            rec.put("photo_url_big", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_big")));
            rec.put("photo_url_small", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_small")));
            rec.put("photo_url_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_small").replace("S", "T")));
            //who shared to me

            String from_user = rec.getString("from_user");
            if (!from_user.equals("") && !from_user.equals("0")) {
                rec.put("from_user", accountLogic.getUser(ctx, viewerId, from_user, "user_id, display_name, image_url,perhaps_name"));
            } else {
                rec.put("from_user", new Record());
            }
            rec.remove("img_middle");
            rec.remove("img_big");
            rec.remove("img_small");
            rec.remove("img_original");

            //add comments and likes
            String photo_id = rec.getString("photo_id");
            String objectPhotoId = String.valueOf(Constants.PHOTO_OBJECT) + ":" + String.valueOf(photo_id);
            if (need_likes) {
                Record Rec_photo_like = new Record();
                int photo_like_count = likeLogic.getLikeCount(ctx, objectPhotoId);
                Rec_photo_like.put("count", photo_like_count);
                if (photo_like_count > 0) {

                    RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, objectPhotoId, 0, 5);
                    List<Long> list_photo_liked_users = recs_liked_users.getIntColumnValues("liker");
                    String likeuids = StringUtils.join(list_photo_liked_users, ",");
                    RecordSet recs_user_liked = accountLogic.getUsers(ctx, rec.getString("source"), likeuids, AccountLogic.USER_LIGHT_COLUMNS_LIGHT);
                    Rec_photo_like.put("users", recs_user_liked);
                } else {
                    Rec_photo_like.put("users", new Record());//3
                }
                Rec_photo_like.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, objectPhotoId));
                rec.put("likes", Rec_photo_like);
            } else {
                rec.put("likes", new Record());
            }

            if (need_comments) {
                Record Rec_comment = new Record();
                int comment_count = commentLogic.getCommentCountP(ctx, viewerId, Constants.PHOTO_OBJECT, String.valueOf(photo_id));
                Rec_comment.put("count", comment_count);
                if (comment_count > 0) {
                    RecordSet recs_com = commentLogic.getCommentsForContainsIgnore(ctx, viewerId, Constants.PHOTO_OBJECT, photo_id, Constants.FULL_COMMENT_COLUMNS, false, 0, 2);
                    Rec_comment.put("latest_comments", recs_com);
                } else {
                    Rec_comment.put("latest_comments", new Record());
                }
                rec.put("comments", Rec_comment);
            } else {
                rec.put("comments", new Record());
            }

            return rec;
        } else {
            return new Record();
        }
    }

    @WebMethod("photo/download_photo")
    public NoResponse downloadPhoto(QueryParams qp, HttpServletResponse resp) {
        Context ctx = WutongContext.getContext(qp, true);
        String photo_id = qp.checkGetString("photo_id");
        String ft = qp.getString("filetype", PhotoLogic.PHOTO_TYPE_ORIGINAL);
        SFSUtils.writeResponse(resp, photoStorage, getFileName(ctx, photo_id, ft), "image/JPEG");
        return NoResponse.get();
    }

    private String getFileName(Context ctx, String photo_id, String filetype) {
        PhotoLogic photo = GlobalLogics.getPhoto();
        Record rc = photo.getPhotoByIds(ctx, photo_id).getFirstRecord();
        if (rc.isEmpty())
            throw new ServerException(WutongErrors.PHOTO_NOT_EXISTS, "photo is not exist!!");
        if (PhotoLogic.PHOTO_TYPE_LARGE.equals(filetype)) {
            return rc.getString("img_big");
        } else if (PhotoLogic.PHOTO_TYPE_SMALL.equals(filetype)) {
            return rc.getString("img_small");
        } else
            return rc.getString("img_middle");
    }

    @WebMethod("photo/format")
    public RecordSet formatPhotoThumbnail(QueryParams qp, HttpServletResponse resp) throws Exception {
        PhotoLogic photo = GlobalLogics.getPhoto();
        Context ctx = WutongContext.getContext(qp, true);

        RecordSet recs = photo.getAllPhotos(ctx, qp.getString("user_id", "0"));
        Configuration conf = getConfiguration();
        RecordSet outList = new RecordSet();
        for (Record rec : recs) {
            Record tt = new Record();
            tt.put("photo_id", rec.getString("photo_id"));
            tt.put("img_small", rec.getString("img_small"));
            try {
                String new_file_name = makImage(conf.getString("platform.fileUrlPattern", "http://oss.aliyuncs.com/") + "wutong-data/media/photo/" + rec.getString("img_small"), rec.getString("img_small"));
                tt.put("img_t", String.format(conf.checkGetString("platform.photoUrlPattern"), new_file_name));
            } catch (Exception e) {
            }
            outList.add(tt);
        }
        return outList;
    }

    @WebMethod("photo/share")
    public Record sharePhoto(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photoLogic = GlobalLogics.getPhoto();
        StreamLogic streamLogic = GlobalLogics.getStream();

        String userId = WutongContext.checkTicket(qp);
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);
        List<String> groupIds = new ArrayList<String>();
        List<String> pids = new ArrayList<String>();
        long scene = qp.getInt("scene", 0L);
        String msg = qp.getString("msg", "share photo");
        String add_to = getAddToUserIds(msg);
        String tmp_ids = "";
        if (mentions.length() > 0) {
            List<String> l0 = StringUtils2.splitList(mentions, ",", true);
            if (l0.contains("#-2")) {
                l0.remove("#-2");
                mentions = StringUtils.join(l0, ",");
            } else {
                //                privacy = true;
            }

            //group
            GroupLogic groupLogic = GlobalLogics.getGroup();
            groupIds = groupLogic.getGroupIdsFromMentions(ctx, l0);
            for (String groupId : groupIds) {
                l0.remove("#" + groupId);
                l0.remove(groupId);
                Record groupRec = groupLogic.getGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END, ctx.getViewerIdString(),
                        groupId, Constants.GRP_COL_CAN_MEMBER_POST, true).getFirstRecord();
                long canMemberPost = groupRec.getInt(Constants.GRP_COL_CAN_MEMBER_POST, 1);
                if ((canMemberPost == 1 && groupLogic.hasRight(ctx, Long.parseLong(groupId), ctx.getViewerId(), Constants.ROLE_MEMBER))
                        || (canMemberPost == 0 && groupLogic.hasRight(ctx, Long.parseLong(groupId), ctx.getViewerId(), Constants.ROLE_ADMIN))
                        || canMemberPost == 2) {
                    l0.add(groupId);
                }
            }
            PageLogicUtils.removeIllegalPageIds(ctx, l0);
            mentions = StringUtils.join(l0, ",");
            tmp_ids = parseUserIds(ctx, userId, mentions);
            List<String> l = StringUtils2.splitList(tmp_ids, ",", true);
            if (l.size() > Constants.MAX_GUSY_SHARE_TO)
                throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "Only can share to less than 400 guys!");
        }
        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new ServerException(WutongErrors.GROUP_CANNOT_APPLY, "You don't have right to post!");

        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                signIn(ctx, userId, longitude, latitude, altitude, speed, geo, 1);
        }

        FileItem fi = qp.checkGetFile("photo_image");
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            if (!fi.getContentType().contains("image/"))
                throw new ServerException(WutongErrors.PHOTO_CONTENT_TYPE_ERROR, "file type error,not image");
            String fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
            String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());

            String album_id = qp.getString("album_id", "");
            if (StringUtils.isEmpty(album_id))
                album_id = photoLogic.getAlbum(ctx, userId, PhotoLogic.ALBUM_TYPE_SHARE_OUT, "Sharing Pictures");

            String path = photoLogic.getPhotoPath(ctx, userId, album_id);
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
            String imageName = userId + "_" + album_id + "_" + photoID;

            String longitude = "";
            String latitude = "";
            String orientation = "";
            Record extendExif = new Record();
            if (expName.equalsIgnoreCase("jpg") || expName.equalsIgnoreCase("jpeg")) {
                try {
                    extendExif = getExifGpsFromJpeg(fi);
                } catch (JpegProcessingException e) {
                } catch (MetadataException e) {
                }
                if (!extendExif.isEmpty()) {
                    if (extendExif.has("longitude"))
                        longitude = String.valueOf(formatJWD(extendExif.getString("longitude")));
                    if (extendExif.has("latitude"))
                        latitude = String.valueOf(formatJWD(extendExif.getString("latitude")));
                    if (extendExif.has("orientation"))
                        orientation = extendExif.getString("orientation");
                }
            }


            Record rc = new Record();
            rc.put("photo_id", photoID);
            rc.put("album_id", album_id);
            rc.put("user_id", userId);
            rc.put("img_middle", imageName + "_O." + expName);
            rc.put("img_original", imageName + "_O." + expName);
            rc.put("img_big", imageName + "_L." + expName);
            rc.put("img_small", imageName + "_S." + expName);
            rc.put("caption", caption);
            rc.put("created_time", DateUtils.nowMillis());
            rc.put("location", loc);
            rc.put("tag", "");
            rc.put("original_pid", photoID);
            rc.put("longitude", longitude);
            rc.put("longitude", longitude);
            rc.put("latitude", latitude);
            rc.put("orientation", orientation);

            photoLogic.saveUploadPhoto(ctx,fi, imageName, path, rc);
            RecordSet group_recs = new RecordSet();
            if (!groupIds.isEmpty()) {
                group_recs = dealWithGroupPhoto(ctx, rc, groupIds);
                pids.add(photoID);
            }
            boolean result = groupIds.isEmpty() ? photoLogic.saveUploadPhoto(ctx,rc) : photoLogic.saveUploadPhotos(group_recs);

            pids.add(photoID);
            List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < Constants.USER_ID_MAX_LEN)
                        l00.add(l011);
                }
            }
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= Constants.USER_ID_MAX_LEN) {
                        try {
                            String other_album_id = photoLogic.getAlbum(ctx,uid, photoLogic.ALBUM_TYPE_RECEIVED, "Received Pictures");
                            String path00 = photoLogic.getPhotoPath(ctx,uid, other_album_id);
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
                            rc00.put("caption", caption);
                            rc00.put("created_time", DateUtils.nowMillis());
                            rc00.put("location", loc);
                            rc00.put("tag", "");
                            rc00.put("from_user", userId);
                            rc00.put("original_pid", photoID);
                            rc00.put("longitude", longitude);
                            rc00.put("latitude", latitude);
                            rc00.put("orientation", orientation);
                            photoLogic.saveUploadPhoto(ctx,rc00);
                            //                        pids.add(photoID00);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            String post_id = "";
            Record mock = new Record();
            Record album = photoLogic.getAlbumOriginal(ctx,album_id);
            if (result) {            // && !album.getBoolean("privacy",false)
                Record sRecord = new Record();
                Configuration conf = getConfiguration();

                sRecord.put("album_id", album.getString("album_id"));
                sRecord.put("album_name", album.getString("title"));
                sRecord.put("photo_id", photoID);
                sRecord.put("album_photo_count", 0);
                sRecord.put("album_cover_photo_id", 0);
                sRecord.put("album_description", "");
                sRecord.put("album_visible", false);


                if (photoStorage instanceof OssSFS) {
                    sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_L." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                    sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_S." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                    sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_T." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                } else {
                    sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_L." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                    sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_S." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                    sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_T." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                }
                sRecord.put("photo_caption", rc.getString("caption"));
                sRecord.put("photo_location", rc.getString("location"));
                sRecord.put("photo_tag", rc.getString("tag"));
                sRecord.put("photo_created_time", rc.getString("created_time"));
                sRecord.put("longitude", rc.getString("longitude"));
                sRecord.put("latitude", rc.getString("latitude"));
                sRecord.put("orientation", rc.getString("orientation"));

                String app_data = qp.getString("app_data", "");
                if (qp.getBoolean("secretly", false) == true) {
                    qp.checkGetString("mentions");
                }

                boolean can_comment = qp.getBoolean("can_comment", true);
                boolean can_like = qp.getBoolean("can_like", true);
                boolean can_reshare = qp.getBoolean("can_reshare", true);

                mock = streamLogic.postP(ctx,userId, Constants.PHOTO_POST, msg, sRecord.toString(false, false), qp.getString("appid", "1"),
                        "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to,Constants.POST_SOURCE_PEOPLE, scene);
                post_id = mock.checkGetString("post_id");
                if (!post_id.equals("") && !qp.getString("category_id", "").equals(""))
                    GlobalLogics.getCategory().createCategory(ctx, userId, qp.getString("category_id", ""), String.valueOf(Constants.POST_OBJECT), post_id);
                if (pids.size() > 0 && !post_id.equals(""))
                    photoLogic.updatePhotoStreamId(ctx,post_id, pids);
            }
//            return streamLogic.getFullPostsForQiuPuP(ctx,userId, post_id, true).getFirstRecord();
            return mock;
        } else {
            String photoID = qp.checkGetString("photo_id");
            Record old_photo = photoLogic.getPhotoByIds(ctx,photoID).getFirstRecord();
            if (old_photo.isEmpty())
                throw new ServerException(WutongErrors.PHOTO_NOT_EXISTS, "this photo is not exist, author has deleted");

            String album_id = photoLogic.getAlbum(ctx,userId, photoLogic.ALBUM_TYPE_SHARE_OUT, "Sharing Pictures");

            Record rc = new Record();
            rc.put("photo_id", photoID);
            rc.put("album_id", album_id);
            rc.put("user_id", userId);
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

            boolean result = groupIds.isEmpty() ? photoLogic.saveUploadPhoto(ctx,rc) : photoLogic.saveUploadPhotos(dealWithGroupPhoto(ctx, rc, groupIds));

            pids.add(photoID);
            List<String> l00 = StringUtils2.splitList(mentions, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < Constants.USER_ID_MAX_LEN)
                        l00.add(l011);
                }
            }
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= Constants.USER_ID_MAX_LEN) {
                        String other_album_id = photoLogic.getAlbum(ctx,uid, photoLogic.ALBUM_TYPE_RECEIVED, "Received Pictures");
                        String path00 = photoLogic.getPhotoPath(ctx,uid, other_album_id);
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
                        photoLogic.saveUploadPhoto(ctx,rc00);
                    }
                }
            }

            String post_id = "";
            Record mock = new Record();
            Record album = photoLogic.getAlbumOriginal(ctx,album_id);
            Record sRecord = new Record();
            Configuration conf = getConfiguration();

            sRecord.put("album_id", album.getString("album_id"));
            sRecord.put("album_name", album.getString("title"));
            sRecord.put("photo_id", photoID);
            sRecord.put("album_photo_count", "");
            sRecord.put("album_cover_photo_id", "");
            sRecord.put("album_description", "");
            sRecord.put("album_visible", false);

            if (photoStorage instanceof OssSFS) {
                sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_big")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_small")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original").replace("O", "T")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
            } else {
                sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_big")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original").replace("O", "T")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
            }
            sRecord.put("photo_caption", rc.getString("caption"));
            sRecord.put("photo_location", rc.getString("location"));
            sRecord.put("photo_tag", rc.getString("tag"));
            sRecord.put("photo_created_time", rc.getString("created_time"));
            sRecord.put("longitude", rc.getString("longitude"));
            sRecord.put("latitude", rc.getString("latitude"));
            sRecord.put("orientation", rc.getString("orientation"));

            String app_data = qp.getString("app_data", "");
            if (qp.getBoolean("secretly", false) == true) {
                qp.checkGetString("mentions");
            }

            boolean can_comment = qp.getBoolean("can_comment", true);
            boolean can_like = qp.getBoolean("can_like", true);
            boolean can_reshare = qp.getBoolean("can_reshare", true);

            mock = streamLogic.postP(ctx,userId, Constants.PHOTO_POST, msg, sRecord.toString(false, false), qp.getString("appid", "1"),
                    "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to,Constants.POST_SOURCE_PEOPLE, scene);
            post_id = mock.checkGetString("post_id");
            if (!post_id.equals("") && !qp.getString("category_id", "").equals(""))
                GlobalLogics.getCategory().createCategory(ctx, userId, qp.getString("category_id", ""), String.valueOf(Constants.POST_OBJECT), post_id);
            if (pids.size() > 0 && !post_id.equals(""))
                photoLogic.updatePhotoStreamId(ctx,post_id, pids);
//            return streamLogic.getFullPostsForQiuPuP(ctx,userId, post_id, true).getFirstRecord();
            return mock;
        }
    }

    public Record getExifGpsFromJpeg(FileItem fileItem) throws JpegProcessingException, MetadataException {
        Record pics = new Record();
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(fileItem.getInputStream());
            Directory exifGPS = metadata.getDirectory(GpsDirectory.class);
            Iterator tags_pgs = exifGPS.getTagIterator();

            Directory exif = metadata.getDirectory(ExifDirectory.class);
            Iterator tags = exif.getTagIterator();

            while (tags.hasNext()) {
                Tag tag = (Tag) tags.next();
                if (tag.getTagName().contains("Orientation")) {
                    pics.put("orientation", tag.getDescription());
                    break;
                }
            }

            while (tags_pgs.hasNext()) {
                Tag tag0 = (Tag) tags_pgs.next();
                if (tag0.getTagName().equalsIgnoreCase("GPS Latitude Ref")) {
                    pics.put("latitude ref", tag0.getDescription());
                }
                if (tag0.getTagName().equalsIgnoreCase("GPS Latitude")) {
                    pics.put("latitude", tag0.getDescription());
                }
                if (tag0.getTagName().equalsIgnoreCase("GPS Longitude Ref")) {
                    pics.put("longitude ref", tag0.getDescription());
                }
                if (tag0.getTagName().equalsIgnoreCase("GPS Longitude")) {
                    pics.put("longitude", tag0.getDescription());
                }
            }
        } catch (IOException e) {
        }
        return pics;
    }

    public boolean signIn(Context ctx, String userId, String longitude, String latitude, String altitude, String speed, String geo, int type) {
        SignInLogic signInLogic = GlobalLogics.getSignIn();

        Record r = new Record();
        r.put("user_id", userId);
        r.put("longitude", longitude);
        r.put("latitude", latitude);
        r.put("altitude", altitude);
        r.put("speed", speed);
        r.put("geo", geo);
        r.put("type", type);
        return signInLogic.saveSignIn(ctx, r);

    }

    protected String getAddToUserIds(String message) {
        String outUserId = "";
        if (message.trim().length() > 0 && message.trim().contains("uid=")) {
            List<String> l = new ArrayList<String>();
            Pattern pat = Pattern.compile("(<A [^>]+>(.+?)<\\/A>)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pat.matcher(message);
            while (matcher.find()) {
                String a = matcher.group();
                if (a.contains("borqs")) {
                    String[] s = a.split("uid=");
                    if (s.length > 0) {
                        String uu = s[1];
                        String uu2 = "";
                        char[] aa = uu.toCharArray();
                        for (int i = 0; i < aa.length - 1; i++) {
                            if (StringUtils.isNumeric(String.valueOf(aa[i]))) {
                                uu2 += String.valueOf(aa[i]);
                            } else {
                                break;
                            }
                        }
                        l.add(uu2);
                    }
                }
            }
            matcher.reset();
            outUserId = StringUtils.join(l, ",");
        }
        return outUserId;
    }

    @WebMethod("photo/myt")
    public boolean Photo(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        FileItem fi = qp.checkGetFile("file");
        Record extendExif = new Record();
        String orientation = "";
        try {
            extendExif = getExifGpsFromJpeg(fi);
        } catch (JpegProcessingException e) {
            e.printStackTrace();
        } catch (MetadataException e) {
            e.printStackTrace();
        }
        if (!extendExif.isEmpty()) {
            if (extendExif.has("orientation"))
                orientation = extendExif.getString("orientation");
        }
        Record record = new Record();
        record.put("orientation", orientation);
        //saveUploadPhoto(fi, "test.jpg", "D:/test/"+DateUtils.nowNano()+".jpg", record);
        GlobalLogics.getPhoto().saveUploadPhoto(ctx,fi, "test.jpg", "/home/zhengwei/data/photo/" + DateUtils.nowNano() + ".jpg", record);
        return true;
    }

    @WebMethod("photo/sync")
    public boolean syncPhoto(QueryParams qp, HttpServletRequest req)throws JpegProcessingException,MetadataException {
        Context ctx = WutongContext.getContext(qp, true);
        PhotoLogic photoLogic = GlobalLogics.getPhoto();
        String userId = WutongContext.checkTicket(qp);
        FileItem fi = qp.checkGetFile("photo_image");
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            String fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
            String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());

            String album_id = qp.getString("album_id", "");
            if (StringUtils.isEmpty(album_id))
                album_id = photoLogic.getAlbum(ctx,userId, photoLogic.ALBUM_TYPE_MY_SYNC, "Cloud Album");

            String path = photoLogic.getPhotoPath(ctx,userId, album_id);
            if (!(photoStorage instanceof OssSFS)) {
                File file = new File(path);
                if (!file.exists()) {
                    file.mkdir();
                }
            }

            String photoID = Long.toString(RandomUtils.generateId());
            String loc = ctx.getLocation();
            String imageName = userId + "_" + album_id + "_" + photoID;


            String longitude = "";
            String latitude = "";
            String orientation = "";
            Record extendExif = new Record();
            if (expName.equalsIgnoreCase("jpg") || expName.equalsIgnoreCase("jpeg")) {
                try {
                    extendExif = getExifGpsFromJpeg(fi);
                } catch (JpegProcessingException e) {
                } catch (MetadataException e) {
                }
                if (!extendExif.isEmpty()) {
                    if (extendExif.has("longitude"))
                        longitude = String.valueOf(formatJWD(extendExif.getString("longitude")));
                    if (extendExif.has("latitude"))
                        latitude = String.valueOf(formatJWD(extendExif.getString("latitude")));
                    if (extendExif.has("orientation"))
                        orientation = extendExif.getString("orientation");
                }
            }

            Record rc = new Record();
            rc.put("photo_id", photoID);
            rc.put("album_id", album_id);
            rc.put("user_id", userId);
            rc.put("img_middle", imageName + "_O." + expName);
            rc.put("img_original", imageName + "_O." + expName);
            rc.put("img_big", imageName + "_L." + expName);
            rc.put("img_small", imageName + "_S." + expName);
            rc.put("caption", fileName);
            rc.put("created_time", DateUtils.nowMillis());
            rc.put("location", loc);
            rc.put("tag", qp.getString("tag", ""));
            rc.put("longitude", longitude);
            rc.put("latitude", latitude);
            rc.put("orientation", orientation);
            rc.put("privacy", true);

            photoLogic.saveUploadPhoto(ctx,fi, imageName, path, rc);

            boolean result = photoLogic.saveUploadPhoto(ctx,rc);

            return result;
        } else {
            return false;
        }
    }

    public static InputStream getUrlImage(String URLName) throws Exception {
        return getUrlImage(URLName, null);
    }

    public static InputStream getUrlImage(String URLName, StringBuilder retryUrl) throws Exception {
        int HttpResult = 0;
        try {
            URL url = new URL(URLName);
            URLConnection urlConn = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            HttpResult = httpConn.getResponseCode();
            if (HttpResult != HttpURLConnection.HTTP_OK) {
                return null;
            } else {
                if (retryUrl != null)
                    retryUrl.append(httpConn.getURL().toString());
                return new BufferedInputStream(urlConn.getInputStream());
            }
        } catch (Exception e) {
            //L.error(e.toString());
            return null;
        }
    }

    public String makImage(String url, String file_name) throws Exception {
        InputStream input = getUrlImage(url);
        if (input != null) {
            byte[] imgBuf = IOUtils.toByteArray(input);
            BufferedImage image = null;
            image = ImageIO.read(new ByteArrayInputStream(imgBuf));

            long sw = 0;
            long sh = 0;
            if (image != null) {
                long bw = image.getWidth();
                long bh = image.getHeight();
                if (bw > bh) {
                    sh = 120;
                    sw = (bw * 120) / bh;
                } else {
                    sw = 120;
                    sh = (bh * 120) / bw;
                }
            }
            String img_name_thumbnail = file_name.replace("S", "T");
            img_name_thumbnail = "media/photo/" + img_name_thumbnail;

            SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), photoStorage, img_name_thumbnail, String.valueOf(sw), String.valueOf(sh), StringUtils.substringAfterLast(file_name, "."));
            return img_name_thumbnail;
        } else {
            return "";
        }
    }

    public String parseUserIds(Context ctx, String viewerId, String userIds) {
        final String METHOD = "parseUserIds";


        StringBuilder buff = new StringBuilder();
        for (String userId : StringUtils2.splitList(userIds, ",", true)) {
            if (userId.startsWith("#")) {
                if (!viewerId.equals("0") && !viewerId.equals("")) {
                    String circleId = StringUtils.removeStart(userId, "#");
                    RecordSet friendRecs = getFriends0(ctx, viewerId, circleId, 0, -1);
                    buff.append(friendRecs.joinColumnValues("friend", ","));
                }
            } else {
                buff.append(userId);
                //                if (hasUser(userId)) {
                //                    buff.append(userId);
                //                }
            }
            buff.append(",");
        }

        return StringUtils2.stripItems(buff.toString(), ",", true);
    }

    public double formatJWD(String in_jwd) {
        //  116"29'17.13
        double out = 0;
        if (in_jwd.length() > 0) {
            List<String> ss = new ArrayList<String>();
            for (String sss : in_jwd.replaceAll("[^0-9|^\\.]", ",").split(",")) {
                if (sss.length() > 0)
                    ss.add(sss);
            }
            out = Double.parseDouble(ss.get(0)) + Double.parseDouble(ss.get(1)) / 60 + Double.parseDouble(ss.get(2)) / 3600;
        }
        return out;
    }

    public RecordSet getFriends0(Context ctx, String userId, String circleIds, int page, int count) {
        FriendshipLogic fs = GlobalLogics.getFriendship();
        return fs.getFriends(ctx, userId, circleIds, page, count);

    }



    private RecordSet dealWithGroupPhoto(Context ctx, Record viewerPhotoRec, List<String> groupIds) {
        RecordSet recs = new RecordSet();
        String viewerId = viewerPhotoRec.getString("user_id");
        recs.add(viewerPhotoRec);
        GroupLogic groupLogic = GlobalLogics.getGroup();
        PhotoLogic photoLogic = GlobalLogics.getPhoto();

        RecordSet groups = groupLogic.getGroups(ctx, 0, 0, viewerId, StringUtils2.joinIgnoreBlank(",", groupIds), Constants.GROUP_LIGHT_COLS, false);
        for (Record group : groups) {
            String groupId = group.getString("id", "0");
            String groupName = group.getString("name", "Default");
            Record rec = viewerPhotoRec.copy();
            rec.put("user_id", groupId);
            rec.put("album_id", photoLogic.getAlbum(ctx,groupId, photoLogic.ALBUM_TYPE_GROUP, groupName));
            recs.add(rec);

            Record viewerGroupPhoto = viewerPhotoRec.copy();
            viewerGroupPhoto.put("album_id", photoLogic.getAlbum(ctx,viewerId, photoLogic.ALBUM_TYPE_TO_GROUP, groupName));
            recs.add(viewerGroupPhoto);
        }

        return recs;
    }
}
