package com.borqs.server.impl.photo;


import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.Photo;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PhotoRs {

    public static Album readAlbum(ResultSet rs) throws SQLException {
        Album album = new Album();
        album.setAlbum_id(rs.getLong("album_id"));
        album.setAlbum_type(rs.getInt("album_type"));
        album.setUser_id(rs.getLong("user_id"));
        album.setTitle(rs.getString("title"));
        album.setSummary(rs.getString("summary"));
        album.setCover_photo_id(rs.getLong("cover_photo_id"));
        album.setPrivacy(rs.getBoolean("privacy"));
        album.setCan_upload(rs.getInt("can_upload"));
        album.setNum_photos(rs.getInt("num_photos"));
        album.setLocation(rs.getString("location"));
        album.setHtml_page_url(rs.getString("html_page_url"));
        album.setThumbnail_url(rs.getString("thumbnail_url"));
        album.setBytes_used(rs.getInt("bytes_used"));
        album.setCreated_time(rs.getLong("created_time"));
        album.setUpdated_time(rs.getLong("updated_time"));
        album.setPublish_time(rs.getLong("publish_time"));
        album.setPhotos_etag(rs.getString("photos_etag"));
        album.setPhotos_dirty(rs.getInt("photos_dirty"));
        return album;
    }

    public static Photo readPhoto(ResultSet rs) throws SQLException {
        Photo photo = new Photo();
        photo.setPhoto_id(rs.getLong("photo_id"));
        photo.setUser_id(rs.getLong("user_id"));
        photo.setTitle(rs.getString("title"));
        photo.setThumbnail_url(rs.getString("thumbnail_url"));
        photo.setSummary(rs.getString("summary"));
        photo.setKeywords(rs.getString("keywords"));
        photo.setContent_type(rs.getString("content_type"));
        photo.setContent_url(rs.getString("content_url"));
        photo.setHtml_page_url(rs.getString("html_page_url"));
        photo.setSize(rs.getLong("size"));
        photo.setHeight(rs.getInt("height"));
        photo.setWidth(rs.getInt("width"));
        photo.setAlbum_id(rs.getLong("album_id"));
        photo.setLongitude(rs.getString("longitude"));
        photo.setLatitude(rs.getString("latitude"));
        photo.setLocation(rs.getString("location"));
        photo.setTags(rs.getString("tags"));
        photo.setFingerprint(rs.getString("fingerprint"));
        photo.setFace_rectangles(rs.getString("face_rectangles"));
        photo.setFace_names(rs.getString("face_names"));
        photo.setFace_ids(rs.getString("face_ids"));
        photo.setExif_model(rs.getString("exif_model"));
        photo.setExif_make(rs.getString("exif_make"));
        photo.setExif_focal_length(rs.getString("exif_focal_length"));
        photo.setCreated_time(rs.getLong("created_time"));
        photo.setUpdated_time(rs.getLong("updated_time"));
        photo.setTaken_time(rs.getLong("taken_time"));
        photo.setPublished_time(rs.getLong("published_time"));
        photo.setFingerprint_hash(rs.getLong("fingerprint_hash"));
        photo.setDisplay_index(rs.getLong("display_index"));
        photo.setExif_exposure(rs.getString("exif_exposure"));
        photo.setExif_flash(rs.getString("exif_flash"));
        photo.setRotation(rs.getInt("rotation"));
        photo.setCamera_sync(rs.getInt("camera_sync"));
        photo.setExif_iso(rs.getInt("exif_iso"));
        photo.setExif_fstop(rs.getInt("exif_fstop"));
        return photo;
    }
}
