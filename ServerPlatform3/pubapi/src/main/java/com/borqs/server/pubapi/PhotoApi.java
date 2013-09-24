package com.borqs.server.pubapi;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.Albums;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.feature.photo.PhotoLogic;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

@RoutePrefix("/v2")
public class PhotoApi extends PublicApiSupport {
    private PhotoLogic photoLogic;

    public PhotoLogic getPhotoLogic() {
        return photoLogic;
    }

    public void setPhotoLogic(PhotoLogic photoLogic) {
        this.photoLogic = photoLogic;
    }

    public PhotoApi() {
    }

    @Route(url = "/album/create")
    public void createAlbum(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        Album album = new Album();
        album.setTitle(req.checkString("title"));
        album.setAlbum_type(Photo.ALBUM_TYPE_OTHERS);
        album.setBytes_used(0);
        album.setCan_upload(1);
        album.setCreated_time(DateHelper.nowMillis());
        album.setSummary(req.getString("summary", ""));
        album.setLocation(ctx.getLocation());
        album.setPrivacy(req.getBoolean("privacy", true));
        album.setUser_id(ctx.getViewer());

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

    @Route(url = "/album/get")
    public void getAlbums(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        boolean withPhotos = req.getBoolean("with_photo_ids", false);
        long album_id = req.checkLong("album_id");
        List<Album> listAlbums = photoLogic.getAlbums(ctx, withPhotos, album_id);
        resp.body(JsonHelper.toJson(listAlbums, true));
    }

    @Route(url = "/album/update")
    public void albumUpdate(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        Album album = new Album();
        album.setAlbum_id(req.checkLong("album_id"));
        album.setTitle(req.getString("title", null));
        album.setSummary(req.getString("summary", null));
        album.setPrivacy(req.getBoolean("privacy", false));

        photoLogic.updateAlbum(ctx, album);
        resp.body(true);
    }

    @Route(url = "/album/delete")
    public void albumDestroyed(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long longs = req.checkLong("album_id");
        photoLogic.destroyAlbum(ctx, longs);
        resp.body(true);
    }

    /*@Route(url = "/album/user_get")
    public void getUserAlbum(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        long userId = req.getLong("user_id", ctx.getViewer());
        int albumType = req.checkInt("album_type");
        List<Album> listAlbums = photoLogic.getUserAlbum(ctx, userId, albumType,false);
        resp.body(JsonHelper.toJson(listAlbums, true));
    }*/

    @Route(url = "/photo/get")
    public void getPhoto(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long[] longs = req.checkLongArray("photo_ids", ",");
        List<Photo> listPhoto = photoLogic.getPhotos(ctx, longs);
        resp.body(JsonHelper.toJson(listPhoto, true));
    }

    @Route(url = "/photo/album_get")
    public void getPhotoByAlbum(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long[] longs = req.checkLongArray("album_ids", ",");

        int p = req.getInt("page", 0);
        int count = req.getInt("count", 20);
        Page page = new Page(p, count);

        List<Photo> listPhoto = photoLogic.getPhotosByAlbum(ctx, page, longs);
        resp.body(JsonHelper.toJson(listPhoto, true));
    }

    @Route(url = "/photo/upload")
    public void uploadPhoto(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long albumId = req.getLong("album_id", 0);
        FileItem fi = req.checkFile("photo_image");

        Photo p = photoLogic.uploadPhoto(ctx, albumId, fi);
        resp.body(JsonHelper.toJson(p, true));
    }

    @Route(url = "/photo/update")
    public void commentCanLike(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        Photo photo = new Photo();

        photo.setPhoto_id(req.checkLong("photo_id"));
        photo.setUser_id(ctx.getViewer());
        photo.setTitle(req.getString("caption", ""));
        photo.setLocation(req.getString("location", ""));

        photoLogic.updatePhoto(ctx, photo);
        resp.body(true);
    }

    @Route(url = "/photo/tag")
    public void tagPhoto(Request req, Response resp) {
        Context ctx = checkContext(req, false);
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
        Context ctx = checkContext(req, false);
        long[] longs = req.checkLongArray("photo_ids", ",");
        photoLogic.deletePhoto(ctx, ctx.getViewer(), longs);
        resp.body(true);
    }


    @Route(url = "/photo/search")
    public void searchPhotos(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        String keyWords = req.checkString("key_word", ",");
        List<Photo> listPhoto = photoLogic.searchPhotos(ctx, keyWords);
        resp.body(JsonHelper.toJson(listPhoto, true));
    }

    @Route(url = "/photo/near_by")
    public void photosNearBy(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        List<Photo> listPhoto = photoLogic.getPhotosNearBy(ctx);
        resp.body(JsonHelper.toJson(listPhoto, true));
    }

    @Route(url = "/photo/include_me")
    public void photosIncludeMe(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        List<Photo> listPhoto = photoLogic.getPhotosIncludeMe(ctx);
        resp.body(JsonHelper.toJson(listPhoto, true));
    }

    @Route(url = "photo/download_photo")
    public void downLoadPhoto(Request req, Response resp) {
        //TODO
    }

    @Route(url = "photo/format")
    public void formatPhotoThumbnail(Request req, Response resp) {
        //TODO
    }
}