package com.borqs.server.impl.photo;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.expansion.ExpansionHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.photo.*;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ParamChecker;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class PhotoImpl implements PhotoLogic {
    private static final Logger L = Logger.get(PhotoImpl.class);
    // db
    private PhotoDb db = new PhotoDb();
    // expansion
    private List<PhotoExpansion> photoExpansions;
    private List<AlbumExpansion> albumExpansions;

    private ConversationLogic conversationLogic;
    private String path;
    private AccountLogic accountLogic;

    private SFS photoSFS;

    private List<AlbumHook> createAlbumHook;
    private List<AlbumHook> updateAlbumHook;
    private List<AlbumHook> destroyAlbumHook;

    private List<PhotoHook> createPhotoHook;
    private List<PhotoHook> updatePhotoHook;
    private List<PhotoHook> destroyPhotoHook;


    public PhotoImpl() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public AccountLogic getAccountLogic() {
        return accountLogic;
    }

    public void setAccountLogic(AccountLogic accountLogic) {
        this.accountLogic = accountLogic;
    }

    public SFS getPhotoSFS() {
        return photoSFS;
    }

    public void setPhotoSFS(SFS photoSFS) {
        this.photoSFS = photoSFS;
    }

    public List<PhotoHook> getCreatePhotoHook() {
        return createPhotoHook;
    }

    public void setCreatePhotoHook(List<PhotoHook> createPhotoHook) {
        this.createPhotoHook = createPhotoHook;
    }

    public List<PhotoHook> getUpdatePhotoHook() {
        return updatePhotoHook;
    }

    public void setUpdatePhotoHook(List<PhotoHook> updatePhotoHook) {
        this.updatePhotoHook = updatePhotoHook;
    }

    public List<PhotoHook> getDestroyPhotoHook() {
        return destroyPhotoHook;
    }

    public void setDestroyPhotoHook(List<PhotoHook> destroyPhotoHook) {
        this.destroyPhotoHook = destroyPhotoHook;
    }

    public PhotoDb getDb() {
        return db;
    }

    public void setDb(PhotoDb db) {
        this.db = db;
    }


    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getAlbumTable() {
        return db.getAlbumTable();
    }

    public void setAlbumTable(Table albumTable) {
        db.setAlbumTable(albumTable);
    }

    public Table getPhotoTable() {
        return db.getPhotoTable();
    }

    public void setPhotoTable(Table photoTable) {
        db.setPhotoTable(photoTable);
    }

    public ConversationLogic getConversationLogic() {
        return conversationLogic;
    }

    public void setConversationLogic(ConversationLogic conversationLogic) {
        this.conversationLogic = conversationLogic;
    }

    public void setPhotoExpansions(List<PhotoExpansion> photoExpansions) {
        this.photoExpansions = photoExpansions;
    }

    public List<AlbumHook> getCreateAlbumHook() {
        return createAlbumHook;
    }

    public void setCreateAlbumHook(List<AlbumHook> createAlbumHook) {
        this.createAlbumHook = createAlbumHook;
    }

    public List<AlbumHook> getUpdateAlbumHook() {
        return updateAlbumHook;
    }

    public void setUpdateAlbumHook(List<AlbumHook> updateAlbumHook) {
        this.updateAlbumHook = updateAlbumHook;
    }

    public List<AlbumHook> getDestroyAlbumHook() {
        return destroyAlbumHook;
    }

    public void setDestroyAlbumHook(List<AlbumHook> destroyAlbumHook) {
        this.destroyAlbumHook = destroyAlbumHook;
    }

    public void setAlbumExpansions(List<AlbumExpansion> albumExpansions) {
        this.albumExpansions = albumExpansions;
    }

    @Override
    public Album createAlbum(Context ctx, Album album) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "createAlbum",
                ctx, "album", album);

        try {

            if (Photo.ALBUM_TYPE_OTHERS == album.getAlbum_type()) {
                if (db.isMyAlbumExist(ctx, album.getUser_id(), album.getAlbum_type(), album.getTitle()))
                    throw new RuntimeException("This album is exist!");
            } else {
                if (db.isMyAlbumExist(ctx, album.getUser_id(), album.getAlbum_type(), null))
                    throw new RuntimeException("This album is exist!");
            }

            album.setAlbum_id(RandomHelper.generateId());
            ParamChecker.notNull("ctx", ctx);
            album.setUser_id(ctx.getViewer());
            ParamChecker.notNull("album", album);

            HookHelper.before(createAlbumHook, ctx, album);
            Album albumRes = db.createAlbum(ctx, album);
            HookHelper.after(createAlbumHook, ctx, album);

            LC.endCall();
            return albumRes;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean destroyAlbum(Context ctx, long... albumIds) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "destroyAlbums",
                ctx, "albumIds", albumIds);
        boolean isDestoryed = false;
        try {
            ParamChecker.notNull("ctx", ctx);
            if (ArrayUtils.isEmpty(albumIds)) {
                return false;
            }
            for (Long l : albumIds) {
                List<Album> list = this.getAlbums(ctx, false, l);
                Album album = null;
                if (list.size() > 0) {
                    album = list.get(0);
                    int type = album.getAlbum_type();
                    if (type != Photo.ALBUM_TYPE_OTHERS)
                        throw new RuntimeException("server error, can't delete album");
                }else{
                    throw new RuntimeException("no record");
                }
                HookHelper.before(destroyAlbumHook, ctx, album);
                isDestoryed = db.destroyAlbums(ctx, ctx.getViewer(), l);
                HookHelper.after(destroyAlbumHook, ctx, album);
            }
            LC.endCall();
            return isDestoryed;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean updateAlbum(Context ctx, Album album) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "updateAlbums",
                ctx, "album", album);
        boolean isDestoryed = false;
        try {
            ParamChecker.notNull("ctx", ctx);

            HookHelper.before(updateAlbumHook, ctx, album);
            isDestoryed = db.updateAlbum(ctx, album);
            HookHelper.after(updateAlbumHook, ctx, album);
            LC.endCall();
            return isDestoryed;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public Albums getAlbums(Context ctx, boolean with_photos, long... albumIds) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "getAlbums",
                ctx, "album_ids", albumIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            if (ArrayUtils.isEmpty(albumIds)) {
                return new Albums();
            }
            Albums albums = db.getAlbums(ctx, albumIds);
            ctx.putSession("with_photos", with_photos);
            ExpansionHelper.expand(albumExpansions, ctx, null, albums);
            return albums;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    public long ifExistsAlbumName(Context ctx, String album_name, int albumType) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "ifExistsAlbumName",
                ctx, "me", ctx.getViewer());
        try {
            ParamChecker.notNull("ctx", ctx);
            long albumId = db.ifExistsAlbumName(ctx, ctx.getViewer(), album_name, albumType);
            if (albumId == 0) {
                Album album = new Album();
                album.setTitle("User default Album");
                album.setAlbum_type(albumType);
                album.setBytes_used(0);
                album.setCan_upload(1);
                album.setCreated_time(DateHelper.nowMillis());
                album.setSummary("User default Album");
                album.setLocation(ctx.getLocation());
                Album album0 = createAlbum(ctx, album);
                albumId = album0.getAlbum_id();
            }
            return albumId;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean isAlbumExist(Context ctx, long album_id) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "isAlbumExist",
                ctx, "album_id", album_id);
        try {
            ParamChecker.notNull("ctx", ctx);
            Albums albums = db.getAlbums(ctx, album_id);

            if (albums.size() > 0)
                return true;
            return false;

        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean isMyAlbumExist(Context ctx, long userId, int albumType, String albumTitle) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "isMyAlbumExist",
                ctx, "userId", albumType, "albumType", albumTitle, "albumTitle", albumTitle);
        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = db.isMyAlbumExist(ctx, userId, albumType, albumTitle);
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Albums getUserAlbum(Context ctx, long user_id, int album_type, boolean with_photos) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "getUserAlbum",
                ctx, "user_id+album_type", user_id + album_type);
        try {
            ParamChecker.notNull("ctx", ctx);
            Albums albums = db.getUserAlbum(ctx, album_type, user_id);
            ctx.putSession("with_photos", with_photos);
            ExpansionHelper.expand(albumExpansions, ctx, null, albums);

            return albums;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public long getAlbum(Context ctx, long userId, int album_type, String albumName) {
        if (!isMyAlbumExist(ctx, userId, album_type, albumName)) {
            Album album = new Album();
            album.setUser_id(userId);
            album.setAlbum_type(album_type);
            album.setTitle(albumName);
            createAlbum(ctx, album);
        }
        if (album_type == Photo.ALBUM_TYPE_OTHERS) {
            return this.getAlbum(ctx, userId, album_type, albumName);
        } else {
            return this.getAlbum(ctx, userId, album_type, null);
        }

    }

    @Override
    public Photo uploadPhoto(Context ctx, long albumId, FileItem fileItem) {
        Photo photo = null;
        try {
            photo = photoUpload(ctx, fileItem, albumId);
        } catch (IOException e) {
            return photo;
        }
        return photo;
    }

    @Override
    public boolean createPhoto(Context ctx, Photo photo) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "createPhoto",
                ctx, "photo", photo);
        boolean isCreate = false;
        try {
            ParamChecker.notNull("ctx", ctx);
            HookHelper.before(createPhotoHook, ctx, photo);
            isCreate = db.createPhoto(ctx, photo);
            //update album
            Album album = new Album();
            Album rs = getAlbums(ctx, false, photo.getAlbum_id()).get(0);
            album.setAlbum_id(photo.getAlbum_id());
            album.setAlbum_type(rs.getAlbum_type());
            album.setUser_id(ctx.getViewer());
            album.setTitle(rs.getTitle());
            album.setSummary(rs.getSummary());
            album.setCover_photo_id(photo.getPhoto_id());
            album.setPrivacy(rs.getPrivacy());
            album.setCan_upload(rs.getCan_upload());
            album.setNum_photos(rs.getNum_photos() + 1);
            album.setLocation(rs.getLocation());
            album.setHtml_page_url(rs.getHtml_page_url());
            album.setThumbnail_url(rs.getThumbnail_url());
            album.setBytes_used(rs.getBytes_used() + (int) photo.getSize());
            album.setCreated_time(rs.getCreated_time());
            album.setUpdated_time(DateHelper.nowMillis());
            album.setPublish_time(rs.getPublish_time());
            album.setPhotos_etag(rs.getPhotos_etag());
            album.setPhotos_dirty(rs.getPhotos_dirty());
            updateAlbum(ctx, album);

            HookHelper.after(createPhotoHook, ctx, photo);
            LC.endCall();
            return isCreate;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean updatePhoto(Context ctx, Photo photo) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "updatePhoto",
                ctx, "photo", photo);
        boolean isUpdate = false;
        try {
            ParamChecker.notNull("ctx", ctx);

            HookHelper.before(updatePhotoHook, ctx, photo);
            isUpdate = db.updatePhoto(ctx, photo);
            HookHelper.after(updatePhotoHook, ctx, photo);
            LC.endCall();
            return isUpdate;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Albums saveUploadPhoto(Context ctx, Albums albums) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "saveUploadPhoto",
                ctx, "albums", albums);

        try {
            ParamChecker.notNull("ctx", ctx);

            for (Album album : albums) {
                HookHelper.before(createAlbumHook, ctx, album);
                db.createAlbum(ctx, album);
                HookHelper.after(createAlbumHook, ctx, album);
            }
            LC.endCall();
            return albums;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    // not migrate yet
    @Override
    public boolean tagPhoto(Context ctx, long photo_id, int x, final int y, long tagUserId, String tagText, boolean addTag) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "tagPhoto",
                ctx, "photo_id", photo_id);
        boolean isUpdate = false;
        try {
            ParamChecker.notNull("ctx", ctx);
            Photo photo = getPhotos(ctx, photo_id).get(0);
            List<Long> oldFaceIds = StringHelper.splitLongList(photo.getFace_ids(), ",");
            List<Tag> lt = new ArrayList<Tag>();
            if (!photo.getTags().toString().equals("")) {
                for (JsonNode j : (ArrayNode) JsonHelper.fromJson(photo.getTags(), JsonNode.class)) {
                    Tag t0 = new Tag();
                    t0.setUser_id(j.path("user_id").getLongValue());
                    t0.setTag_text(j.path("tag_text").getTextValue());
                    t0.setX(j.path("x").getIntValue());
                    t0.setY(j.path("y").getIntValue());
                    lt.add(t0);
                }
            }

            if (addTag) {
                int flag = 1;
                if (lt.size() > 0) {
                    for (Tag tag1 : lt) {
                        long userId = tag1.getUser_id();
                        int x1 = tag1.getX();
                        int y1 = tag1.getY();
                        if ((userId == tagUserId) && (x == x1 && y == y1)) {
                            flag = 0;
                            break;
                        }
                    }
                }

                if (flag == 1) {
                    Tag tag = new Tag();
                    tag.setX(x);
                    tag.setY(y);
                    tag.setUser_id(tagUserId);
                    tag.setTag_text(tagText);
                    lt.add(tag);
                }

                if (!oldFaceIds.contains(tagUserId))
                    oldFaceIds.add(tagUserId);
            } else {
                for (int i = lt.size() - 1; i >= 0; i--) {
                    long userId = lt.get(i).getUser_id();
                    int x1 = lt.get(i).getX();
                    int y1 = lt.get(i).getY();
                    if ((userId == tagUserId) && (x == x1 && y == y1))
                        lt.remove(i);
                }
                if (oldFaceIds.contains(tagUserId))
                    oldFaceIds.remove(tagUserId);
            }
            Photo p = new Photo();
            p.setPhoto_id(photo_id);
            p.setTags(JsonHelper.toJson(lt, false));
            long[] uIds = new long[oldFaceIds.size()];

            for (int i = 0; i <= oldFaceIds.size() - 1; i++) {
                if (oldFaceIds.get(i) > 0)
                    uIds[i] = oldFaceIds.get(i);
            }
            p.setFace_ids(StringHelper.join(uIds, ","));

            HookHelper.before(updatePhotoHook, ctx, photo);
            isUpdate = db.updatePhoto(ctx, p);
            HookHelper.after(updatePhotoHook, ctx, photo);
            LC.endCall();
            return isUpdate;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean deletePhoto(Context ctx, long userId, long... photoIds) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "deletePhoto",
                ctx, "photoIds", photoIds);
        boolean isDelete = false;
        try {
            ParamChecker.notNull("ctx", ctx);
            for (long photoId : photoIds) {
                Photos list = this.getPhotos(ctx, photoId);
                Photo photo = null;
                if (list.size() > 0) {
                    photo = list.get(0);
                }
                HookHelper.before(destroyPhotoHook, ctx, photo);
                isDelete = db.destroyPhoto(ctx, userId, photoId);
                HookHelper.after(destroyPhotoHook, ctx, photo);
            }
            LC.endCall();
            return isDelete;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Photos getPhotos(Context ctx, long... photoIds) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "getPhotos",
                ctx, "photoIds", photoIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            Photos photoList = db.getPhotos(ctx, photoIds);
            ExpansionHelper.expand(photoExpansions, ctx, Photo.FULL_COLUMNS, photoList);
            return photoList;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Photos getPhotosByAlbum(Context ctx, Page page, long... album_ids) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "getPhotosByAlbum",
                ctx, "album_ids", album_ids);
        try {
            ParamChecker.notNull("ctx", ctx);
            Photos photoList = db.getPhotosByAlbumId(ctx, page, album_ids);
            ExpansionHelper.expand(photoExpansions, ctx, Photo.FULL_COLUMNS, photoList);
            return photoList;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Photos getPhotosByUserId(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "getPhotosByUserId",
                ctx, "userId", userId);
        try {
            ParamChecker.notNull("ctx", ctx);
            Photos photoList = db.getPhotosByUserId(ctx, userId);
            return photoList;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Photos searchPhotos(Context ctx, String keyWords) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "searchPhotos",
                ctx, "keyWords", keyWords);
        try {
            ParamChecker.notNull("ctx", ctx);
            Photos photoList = db.searchPhotos(ctx, keyWords);
            return photoList;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Photos getPhotosNearBy(Context ctx) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "getPhotosNearBy",
                ctx, "longitude+latitude", ctx.getLocation());
        try {
            ParamChecker.notNull("ctx", ctx);
            Photos photoList = db.getPhotosNearBy(ctx, parseLocation(ctx.getLocation(), "longitude"), parseLocation(ctx.getLocation(), "latitude"));
            return photoList;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Photos getPhotosIncludeMe(Context ctx) {
        final LogCall LC = LogCall.startCall(L, PhotoImpl.class, "getPhotosIncludeMe",
                ctx, "me", ctx.getViewer());
        try {
            ParamChecker.notNull("ctx", ctx);
            Photos photoList = db.getPhotosIncludeMe(ctx, ctx.getViewer());
            return photoList;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    public String getPhotoPath(Context ctx, long albumId) {
        String addr = getPath();
        String c = StringUtils.substring(addr, 1, addr.length() - 1).replace("dir=", "");
        String path = c + "/" + String.valueOf(ctx.getViewer()) + "/" + String.valueOf(albumId) + "/";
        return path;
    }

    public Photo photoUpload(Context ctx, FileItem fi, long albumId) throws IOException {
        Photo photo = new Photo();
        String postFix = ".jpg";
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            if (albumId == 0) {
                albumId = ifExistsAlbumName(ctx, "User default Album", 0);
            }
            String fName = fi.getName();
            if (StringUtils.isNotEmpty(fName)) {
                postFix = fName.substring(fName.lastIndexOf("."), fName.length());
            }
            String fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
            String filename0 = "";
            Date date = new Date();
            DateFormat f_year = new SimpleDateFormat("yyyyMMddhhmmss");
            String c_name = f_year.format(date).toString();
            String[] names = null;
            try {
                names = StringHelper.splitArray(fileName, ".", true);
                filename0 = names[0];
            } catch (Exception e) {
                filename0 = c_name;
            }

            if (names.length > 1) {
                String ex = names[names.length - 1];
            }
            String path = getPhotoPath(ctx, albumId);
            File file = new File(path);
            if (!file.exists()) {
                file.mkdir();
            }

            long photoID = RandomHelper.generateId();
            String caption = "";
            String imageName = String.valueOf(ctx.getViewer()) + "_" + albumId + "_" + String.valueOf(photoID);

            int width, height;
            InputStream is = null;
            try {
                is = fi.getInputStream();
                BufferedImage image = ImageIO.read(is);

                byte[] data = fi.get();

                width = image.getWidth();
                height = image.getHeight();

                //saveUploadPhoto(ctx, data, image, width, height, imageName, path, fileName);
                PhotoImageHelper.savePhotoImage(photoSFS, fi, fileName, imageName);
            } catch (IOException e) {
                throw new RuntimeException("save img error " + e);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            photo.setPhoto_id(photoID);
            photo.setAlbum_id(albumId);
            photo.setUser_id(ctx.getViewer());
            photo.setTitle(caption);
            photo.setLocation(ctx.getLocation());
            photo.setCreated_time(DateHelper.nowMillis());
            photo.setTaken_time(DateHelper.nowMillis());
            photo.setKeywords(caption);
            photo.setHeight(height);
            photo.setWidth(width);
            photo.setLatitude(parseLocation(ctx.getUserAgent().getLocale(), "latitude"));
            photo.setLongitude(parseLocation(ctx.getUserAgent().getLocale(), "longitude"));
            photo.setTags("");
            photo.setFace_ids("");
            photo.setContent_type(fi.getContentType());
            photo.setThumbnail_url(imageName + "_S" + postFix);
            //reserve
            //photo.setHtml_page_url(imageName + "_L.jpg");
            boolean result = createPhoto(ctx, photo);
        }
        return photo;
    }


    /*private void saveUploadPhoto(Context ctx, byte[] bytes, BufferedImage image, int width, int height, String file, String path, String fileName) throws IOException {
 int sWidth = 0, sHeight = 0, mWidth = 0, mHeight = 0;
 try {
    *//* if (width == height) {
                sHeight = sWidth = 360;
                mHeight = mWidth = 640;
            }
            if (width > height) {
                sHeight = 360;
                sWidth = (int) 360 * width / height;
            }
            if (height > width) {
                sHeight = (int) 360 * height / width;
                sWidth = 360;
            }

            mHeight = height;
            mWidth = width;
            if (width > 640 || height > 640) {
                if (width > height) {
                    if (width > 640) {
                        mWidth = (int) 640 * width / height;
                        mHeight = 640;
                    }
                }
                if (height > width) {
                    if (height > 640) {
                        mHeight = (int) 640 * height / width;
                        mWidth = 640;
                    }
                }
            }
*//*

            //SFS photoStorage = (SFS) ClassHelper.newInstance(photoSFS.getClass());

            //SFSHelper.saveScaledUploadImage(fileItem, photoStorage, file + "_O.jpg", Integer.toString(mWidth), Integer.toString(mHeight), "jpg",true);
            //SFSHelper.saveScaledUploadImage(fileItem, photoStorage, file + "_S.jpg", Integer.toString(sWidth), Integer.toString(sHeight), "jpg",true);
            //SFSHelper.saveScaledUploadImage(fileItem, photoStorage, file + "_L.jpg", null, null, "jpg",true);
            //PhotoImageHelper.saveUploadedPhotoImage(ctx, photoSFS, fileItem);


            PhotoImageHelper.savePhotoImage(photoSFS, bytes, fileName,file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public static String parseLocation(String location, String key) {
        if (!StringUtils.isBlank(location)) {
            String l[] = StringHelper.splitArray(location, ";", true);

            for (int i = 0; i < l.length; i++) {
                if (l[i].toString().contains(key)) {
                    return StringUtils.substringAfter(l[i].toString(), "=");
                }
            }
        }
        return "";
    }
}
