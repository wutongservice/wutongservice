package com.borqs.server.impl.migration.photo;


import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.Photo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class PhotoMigRs {


    public static Album readAlbum(ResultSet rs, Album album, Map<Long, String> mapAccount) throws SQLException {
        if (album == null)
            album = new Album();

        album.setAlbum_id(rs.getLong("album_id"));
        long userId = rs.getLong("user_id");
        if (!mapAccount.containsKey(userId))
            return null;
        album.setUser_id(userId);

        album.setTitle(rs.getString("album_name"));
        album.setSummary(rs.getString("description"));
        album.setPrivacy(rs.getBoolean("visible"));
        album.setCreated_time(rs.getLong("created_time"));

        return album;
    }

    public static Photo readPhoto(ResultSet rs, Photo photo, Map<Long, String> mapAccount) throws SQLException {
        if (photo == null)
            photo = new Photo();
        photo.setAlbum_id(rs.getLong("album_id"));
        photo.setPhoto_id(rs.getLong("photo_id"));
        photo.setUser_id(rs.getLong("user_id"));
        photo.setTitle(rs.getString("caption"));
        photo.setThumbnail_url(rs.getString("img_small"));
        photo.setLocation(rs.getString("location"));
        photo.setCreated_time(rs.getLong("created_time"));
        photo.setTags(rs.getString("tag"));

        return photo;
    }


}
