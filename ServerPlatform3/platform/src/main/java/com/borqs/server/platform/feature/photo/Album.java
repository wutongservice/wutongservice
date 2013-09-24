package com.borqs.server.platform.feature.photo;


import com.borqs.server.platform.data.Addons;

public class Album extends Addons {

    private long album_id;
    private int album_type;
    private long user_id;
    private String title;
    private String summary;
    private long cover_photo_id;
    private boolean privacy;
    private int can_upload;
    private int num_photos;
    private String location;
    private String html_page_url;
    private String thumbnail_url;
    private int bytes_used;
    private long created_time;
    private long updated_time;
    private long publish_time;
    private String photos_etag;
    private int photos_dirty;

/*    public static final String COL_ALBUM_ID = "album_id";
    public static final String COL_ALBUM_TYPE = "album_type";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_TITLE = "title";
    public static final String COL_SUMMARY = "summary";
    public static final String COL_COVER_PHOTO_ID = "cover_photo_id";
    public static final String COL_PRIVACY = "privacy";
    public static final String COL_CAN_UPLOAD = "can_upload";
    public static final String COL_NUM_PHOTOS = "num_photos";
    public static final String COL_LOCATION = "location";
    public static final String COL_HTML_PAGE_URL = "html_page_url";
    public static final String COL_HTML_PAGE_URL = "html_page_url";*/



    public Album() {
    }

    public Album(long album_id, int album_type,long user_id, String title,String summary,long cover_photo_id,boolean privacy,
                 int can_upload,int num_photos,String location,String html_page_url,int bytes_used,long created_time,
                 long updated_time,long publish_time,String photos_etag,int photos_dirty) {
        this.album_id = album_id;
        this.album_type = album_type;
        this.user_id = user_id;
        this.title = title;
        this.summary = summary;
        this.cover_photo_id = cover_photo_id;
        this.privacy = privacy;
        this.can_upload = can_upload;
        this.num_photos = num_photos;
        this.location = location;
        this.html_page_url = html_page_url;
        this.bytes_used = bytes_used;
        this.created_time = created_time;
        this.updated_time = updated_time;
        this.publish_time = publish_time;
        this.photos_etag = photos_etag;
        this.photos_dirty = photos_dirty;
    }

    public long getAlbum_id() {
        return album_id;
    }

    public void setAlbum_id(long album_id) {
        this.album_id = album_id;
    }

    public int getAlbum_type() {
        return album_type;
    }

    public void setAlbum_type(int album_type) {
        this.album_type = album_type;
    }

    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public long getCover_photo_id() {
        return cover_photo_id;
    }

    public void setCover_photo_id(long cover_photo_id) {
        this.cover_photo_id = cover_photo_id;
    }

    public boolean getPrivacy() {
        return privacy;
    }

    public void setPrivacy(boolean privacy) {
        this.privacy = privacy;
    }

    public int getCan_upload() {
        return can_upload;
    }

    public void setCan_upload(int can_upload) {
        this.can_upload = can_upload;
    }

    public int getNum_photos() {
        return num_photos;
    }

    public void setNum_photos(int num_photos) {
        this.num_photos = num_photos;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getHtml_page_url() {
        return html_page_url;
    }

    public void setHtml_page_url(String html_page_url) {
        this.html_page_url = html_page_url;
    }

    public String getThumbnail_url() {
        return thumbnail_url;
    }

    public void setThumbnail_url(String thumbnail_url) {
        this.thumbnail_url = thumbnail_url;
    }

    public int getBytes_used() {
        return bytes_used;
    }

    public void setBytes_used(int bytes_used) {
        this.bytes_used = bytes_used;
    }

    public long getCreated_time() {
        return created_time;
    }

    public void setCreated_time(long created_time) {
        this.created_time = created_time;
    }

    public long getUpdated_time() {
        return updated_time;
    }

    public void setUpdated_time(long updated_time) {
        this.updated_time = updated_time;
    }

    public long getPublish_time() {
        return publish_time;
    }

    public void setPublish_time(long publish_time) {
        this.publish_time = publish_time;
    }

    public String getPhotos_etag() {
        return photos_etag;
    }

    public void setPhotos_etag(String photos_etag) {
        this.photos_etag = photos_etag;
    }

    public int getPhotos_dirty() {
        return photos_dirty;
    }

    public void setPhotos_dirty(int photos_dirty) {
        this.photos_dirty = photos_dirty;
    }
}
