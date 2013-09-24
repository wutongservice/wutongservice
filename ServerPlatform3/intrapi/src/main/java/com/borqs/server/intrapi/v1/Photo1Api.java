package com.borqs.server.intrapi.v1;

import com.borqs.server.compatible.CompatiblePhoto;
import com.borqs.server.intrapi.InternalApiSupport;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.photo.*;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

@RoutePrefix("/internal")
@IgnoreDocument
public class Photo1Api extends InternalApiSupport {
    private PhotoLogic photoLogic;

    public PhotoLogic getPhotoLogic() {
        return photoLogic;
    }

    public void setPhotoLogic(PhotoLogic photoLogic) {
        this.photoLogic = photoLogic;
    }

    public Photo1Api() {
    }

    @Route(url = "/album/create")
    public void createAlbum(Request req, Response resp) {
        //TODO add before hook
        Context ctx = checkContext(req, true);

        Album album = new Album();
        album.setTitle(req.checkString("title"));
        album.setAlbum_type(req.checkInt("type"));
        album.setBytes_used(0);
        album.setCan_upload(1);
        album.setCreated_time(DateHelper.nowMillis());
        album.setSummary(req.getString("summary", ""));
        album.setLocation(ctx.getLocation());

        photoLogic.createAlbum(ctx, album);
        resp.body(true);

    }

    @Route(url = "album/all")
    public void getAlbum(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long user_id = req.checkLong("user_id");
        boolean with_photo_ids = req.getBoolean("with_photo_ids", false);
        Albums albums = photoLogic.getUserAlbum(ctx, user_id, -1, with_photo_ids);
        resp.body(JsonHelper.toJson(albums, true));
    }

    @Route(url = "/album/destory")
    public void albumDestroyed(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long[] longs = req.checkLongArray("album_id", ",");
        photoLogic.destroyAlbum(ctx, longs);
        resp.body(true);
    }

    @Route(url = "/album/update")
    public void albumUpdate(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        Album album = new Album();
        Album rs = photoLogic.getAlbums(ctx, false, req.checkLong("album_id")).get(0);
        album.setAlbum_id(req.checkLong("album_id"));
        album.setAlbum_type(req.getInt("album_type", 0) != 0 ? req.getInt("album_type", 0) : rs.getAlbum_type());
        album.setUser_id(ctx.getViewer());
        album.setTitle(req.getString("title", null) != null ? req.getString("title", null) : rs.getTitle());
        album.setSummary(req.getString("summary", null) != null ? req.getString("summary", null) : rs.getSummary());
        album.setCover_photo_id(rs.getCover_photo_id());
        album.setPrivacy(req.getBoolean("privacy", false) ? req.getBoolean("privacy", false) : rs.getPrivacy());
        album.setCan_upload(req.getInt("can_upload", 0) != 0 ? req.getInt("can_upload", 0) : rs.getCan_upload());
        album.setNum_photos(req.getInt("num_photos", 0) != 0 ? req.getInt("num_photos", 0) : rs.getNum_photos());
        album.setLocation(req.getString("location", null) != null ? req.getString("location", null) : rs.getLocation());
        album.setHtml_page_url(rs.getHtml_page_url());
        album.setThumbnail_url(rs.getThumbnail_url());
        album.setBytes_used(req.getInt("bytes_used", 0) != 0 ? req.getInt("bytes_used", 0) : rs.getBytes_used());
        album.setCreated_time(rs.getCreated_time());
        album.setUpdated_time(DateHelper.nowMillis());
        album.setPublish_time(rs.getPublish_time());
        album.setPhotos_etag(req.getString("photos_etag", null) != null ? req.getString("photos_etag", null) : rs.getPhotos_etag());
        album.setPhotos_dirty(req.getInt("photos_dirty", 0) != 0 ? req.getInt("photos_dirty", 0) : rs.getPhotos_dirty());

        photoLogic.updateAlbum(ctx, album);
        resp.body(true);
    }

    @Route(url = "/album/get")
    public void getAlbums(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        boolean withPhotos = req.getBoolean("with_photo_ids", false);
        long album_id = req.checkLong("album_id");
        List<Album> listAlbums = photoLogic.getAlbums(ctx, withPhotos, album_id);
        resp.body(JsonHelper.toJson(listAlbums, true));
    }

    /*@Route(url = "/album/user_get")
    public void getUserAlbum(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        long userId = req.getLong("user_id", ctx.getViewer());
        int albumType = req.checkInt("album_type");
        Albums listAlbums = photoLogic.getUserAlbum(ctx, userId, albumType,false);
        resp.body(JsonHelper.toJson(listAlbums, true));
    }*/

    @Route(url = "/photo/upload")
    public void uploadPhoto(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long albumId = req.getLong("album_id", 0);
        FileItem fi = req.checkFile("photo_image");

        Photo p = photoLogic.uploadPhoto(ctx, albumId, fi);
        resp.body(JsonHelper.toJson(p, true));
    }

    @Route(url = "/photo/update")
    public void commentCanLike(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        Photo photo = new Photo();
        Photo rs = photoLogic.getPhotos(ctx, req.checkLong("photo_id")).get(0);
        photo.setPhoto_id(req.checkLong("photo_id"));
        photo.setUser_id(ctx.getViewer());
        photo.setTitle(req.getString("title", "") != null ? req.getString("title", "") : rs.getTitle());
        photo.setThumbnail_url(rs.getThumbnail_url());
        photo.setSummary(req.getString("summary", "") != null ? req.getString("summary", "") : rs.getSummary());
        photo.setKeywords(req.getString("keywords", "") != null ? req.getString("keywords", "") : rs.getKeywords());
        photo.setContent_type(rs.getContent_type());
        photo.setContent_url(rs.getContent_url());
        photo.setHtml_page_url(rs.getHtml_page_url());
        photo.setSize(rs.getSize());
        photo.setHeight(rs.getHeight());
        photo.setWidth(rs.getWidth());
        photo.setAlbum_id(req.getLong("album_id", 0) != 0 ? req.getLong("album_id", 0) : rs.getAlbum_id());
        photo.setLongitude(rs.getLongitude());
        photo.setLatitude(rs.getLatitude());
        photo.setLocation(req.getString("location", "") != null ? req.getString("location", "") : rs.getLocation());
        photo.setTags(rs.getTags());
        photo.setFingerprint(req.getString("fingerprint", "") != null ? req.getString("fingerprint", "") : rs.getFingerprint());
        photo.setFace_rectangles(req.getString("face_rectangles", "") != null ? req.getString("face_rectangles", "") : rs.getFace_rectangles());
        photo.setFace_names(rs.getFace_names());
        photo.setFace_ids(rs.getFace_ids());
        photo.setExif_model(req.getString("exif_model", "") != null ? req.getString("exif_model", "") : rs.getExif_model());
        photo.setExif_make(req.getString("exif_make", "") != null ? req.getString("exif_make", "") : rs.getExif_make());
        photo.setExif_focal_length(req.getString("exif_focal_length", "") != null ? req.getString("exif_focal_length", "") : rs.getExif_focal_length());
        photo.setCreated_time(rs.getCreated_time());
        photo.setUpdated_time(DateHelper.nowMillis());
        photo.setTaken_time(rs.getTaken_time());
        photo.setPublished_time(rs.getPublished_time());
        photo.setFingerprint_hash(req.getLong("fingerprint_hash", 0) != 0 ? req.getLong("fingerprint_hash", 0) : rs.getFingerprint_hash());
        photo.setDisplay_index(0);
        photo.setExif_exposure(req.getString("exif_exposure", "") != null ? req.getString("exif_exposure", "") : rs.getExif_exposure());
        photo.setExif_flash(req.getString("exif_flash", "") != null ? req.getString("exif_flash", "") : rs.getExif_flash());
        photo.setRotation(req.getInt("rotation", 0) != 0 ? req.getInt("rotation", 0) : rs.getRotation());
        photo.setCamera_sync(req.getInt("camera_sync", 0) != 0 ? req.getInt("camera_sync", 0) : rs.getRotation());
        photo.setExif_iso(req.getInt("exif_iso", 0) != 0 ? req.getInt("exif_iso", 0) : rs.getExif_iso());
        photo.setExif_fstop(req.getInt("exif_fstop", 0) != 0 ? req.getInt("exif_fstop", 0) : rs.getExif_fstop());

        photoLogic.updatePhoto(ctx, photo);
        resp.body(true);
    }

    @Route(url = "/photo/tag")
    public void tagPhoto(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long photoId = req.checkLong("photo_id");
        long tagUserId = req.checkLong("tag_user_id");
        int x = req.checkInt("x");
        int y = req.checkInt("y");
        String tagText = req.getString("tag_text", "");
        boolean addTag = req.getBoolean("add_tag", true);

        photoLogic.tagPhoto(ctx, photoId, x, y, tagUserId, tagText, addTag);
        resp.body(true);
    }

    @Route(url = "/photo/delete")
    public void deletePhoto(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long[] longs = req.checkLongArray("photo_ids", ",");
        photoLogic.deletePhoto(ctx, ctx.getViewer(), longs);
        resp.body(true);
    }

    @Route(url = "/photo/get")
    public void getPhoto(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String[] v1Cols = CompatiblePhoto.V1_FULL_COLUMNS;
        long[] longs = req.checkLongArray("photo_ids", ",");
        Photos listPhoto = photoLogic.getPhotos(ctx, longs);
        //resp.body(JsonHelper.toJson(listPhoto, true));
        resp.body(RawText.of(CompatiblePhoto.photosToJson(listPhoto, v1Cols, true)));
    }

    @Route(url = "/photo/album_get")
    public void getPhotoByAlbum(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long[] longs = req.checkLongArray("album_ids", ",");
        Page page = new Page();
        page.count = req.getLong("count",20);
        page.page = req.getInt("page",0);
        List<Photo> listPhoto = photoLogic.getPhotosByAlbum(ctx,page, longs);
        resp.body(JsonHelper.toJson(listPhoto, true));
    }

    @Route(url = "/photo/search")
    public void searchPhotos(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String[] v1Cols = CompatiblePhoto.V1_FULL_COLUMNS;
        String keyWords = req.checkString("key_word", ",");
        Photos listPhoto = photoLogic.searchPhotos(ctx, keyWords);
        resp.body(RawText.of(CompatiblePhoto.photosToJson(listPhoto, v1Cols, true)));
    }

    @Route(url = "/photo/near_by")
    public void photosNearBy(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String[] v1Cols = CompatiblePhoto.V1_FULL_COLUMNS;
        Photos listPhoto = photoLogic.getPhotosNearBy(ctx);
        resp.body(RawText.of(CompatiblePhoto.photosToJson(listPhoto, v1Cols, true)));
    }

    @Route(url = "/photo/include_me")
    public void photosIncludeMe(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String[] v1Cols = CompatiblePhoto.V1_FULL_COLUMNS;
        Photos listPhoto = photoLogic.getPhotosIncludeMe(ctx);
        resp.body(RawText.of(CompatiblePhoto.photosToJson(listPhoto, v1Cols, true)));
    }
}
