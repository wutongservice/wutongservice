package com.borqs.server.platform.feature.photo;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.logic.Logic;
import org.apache.commons.fileupload.FileItem;

public interface PhotoLogic extends Logic {

    Album createAlbum(Context ctx, Album album);
    boolean destroyAlbum(Context ctx, long ...albumIds);
    boolean updateAlbum(Context ctx, Album album);
    Albums getAlbums(Context ctx,  boolean with_photos,long...albumIds);
    Albums getUserAlbum(Context ctx, long user_id,int album_type,boolean withPhotoIds);

    boolean createPhoto(Context ctx, Photo photo);
    Photo uploadPhoto(Context ctx, long albumId, FileItem fileItem);
    boolean updatePhoto(Context ctx,Photo photo) ;
    boolean tagPhoto(Context ctx,long photo_id,int x,int y,long tagUserId,String tagText,boolean addTag) ;
    boolean deletePhoto(Context ctx,long userId,long...photoIds) ;
    Photos getPhotos(Context ctx, long...photoIds);
    Photos getPhotosByAlbum(Context ctx,Page page, long...albumIds);
    Photos searchPhotos(Context ctx, String keyWords);
    Photos getPhotosNearBy(Context ctx);
    Photos getPhotosIncludeMe(Context ctx);
    boolean isAlbumExist(Context ctx,long album_id);
    boolean isMyAlbumExist(Context ctx, long userId, int albumType, String albumTitle);
    Albums saveUploadPhoto(Context ctx, Albums albums);
    Photos getPhotosByUserId(Context ctx, long userId);
    long getAlbum(Context ctx, long userId, int album_type, String albumName);
}
