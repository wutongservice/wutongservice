
package com.borqs.server.wutong.photo;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sfs.StaticFileStorage;
import org.apache.commons.fileupload.FileItem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PhotoLogic {
    public static int ALBUM_TYPE_PROFILE = 0;
    public static int ALBUM_TYPE_SHARE_OUT = 1;
    public static int ALBUM_TYPE_COVER = 2;
    public static int ALBUM_TYPE_RECEIVED = 3;
    public static int ALBUM_TYPE_GROUP = 4;
    public static int ALBUM_TYPE_TO_GROUP = 5;     //              我在group里面发图片创建的相册
    public static int ALBUM_TYPE_MY_SYNC = 8;
    public static int ALBUM_TYPE_OTHERS = 9;

    public static String bucketName = "wutong-data";
    public static String bucketName_photo_key = "media/photo/" ;
    public static String bucketName_video_key = "media/video/" ;
    public static String bucketName_audio_key = "media/audio/" ;
    public static String bucketName_static_file_key = "files/" ;

    public static String PHOTO_TYPE_SMALL = "small";
    public static String PHOTO_TYPE_ORIGINAL = "original";
    public static String PHOTO_TYPE_LARGE = "large";

    boolean createAlbum(Context ctx, Record record);
    boolean createAlbums(Context ctx, RecordSet recs);

    RecordSet getUserAlbum(Context ctx,String viewerId, String userId);

    Record getAlbumById(Context ctx, String viewerId, String userId, String album_id);

    Record getLatestPhoto(Context ctx, String viewerId, String album_id);

    Record getAlbumOriginal(Context ctx, String album_id);

    boolean updateAlbum(Context ctx, String album_id, Record rc);

    boolean deleteAlbumById(Context ctx, String userId, String album_id, String bucketName_photo_key, StaticFileStorage photoStorage);

    RecordSet getPhotoByIds(Context ctx, String photo_ids);

    RecordSet getAlbumPhotos(Context ctx, String viewerId, String album_id, int page, int count);

    boolean updatePhoto(Context ctx, String photo_id, Record rc);

    boolean deletePhotoById(Context ctx, String viewerId, String photo_ids, boolean delete_all, String bucketName_photo_key, StaticFileStorage sfs);

    Record tagPhoto(Context ctx, String photo_id, int top, int left, int frame_width, int frame_height, String tagUserId, String tagText, boolean addTag);

    boolean updatePhotoStreamId(Context ctx, int album_type, String asc);

    RecordSet getPhotosIncludeMe(Context ctx, String viewerId, String user_id, int page, int count);

    RecordSet getAllPhotos(Context ctx, String user_id);

    String getAlbum(Context ctx, String userId, int album_type, String albumName);

    Map<String, String> getAlbums(Context ctx, List<String> userIds, int albumType, String albumName);

    boolean isAlbumExist(Context ctx, String album_id);

    boolean saveUploadPhoto(Context ctx, Record record);

    String getPhotoPath(Context ctx,String viewerId, String album_id);

    void saveUploadPhoto(Context ctx,FileItem fileItem, String file,String path,Record record);

    boolean saveUploadPhotos(RecordSet recs);

    boolean updatePhotoStreamId(Context ctx,String stream_id, List<String> photo_ids);
    RecordSet dealWithGroupPhoto(Context ctx, Record viewerPhotoRec, List<String> groupIds);

}