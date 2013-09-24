package com.borqs.server.impl.photo;

import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import static com.borqs.server.platform.sql.Sql.value;
import static com.borqs.server.platform.sql.Sql.valueIf;

public class PhotoSql {

    public static String insertAlbum(String table, Album album) {
        String sql = new Sql().insertIgnoreInto(table)
                .values(
                        value("`album_id`", album.getAlbum_id()),
                        value("`album_type`", album.getAlbum_type()),
                        value("`user_id`", album.getUser_id()),
                        value("`title`", album.getTitle()),
                        value("`summary`", album.getSummary()),
                        value("`cover_photo_id`", album.getCover_photo_id()),
                        value("`privacy`", album.getPrivacy()),
                        value("`can_upload`", album.getCan_upload()),
                        value("`num_photos`", album.getNum_photos()),
                        value("`location`", album.getLocation()),
                        value("`html_page_url`", album.getHtml_page_url()),
                        value("`thumbnail_url`", album.getThumbnail_url()),
                        value("`bytes_used`", album.getBytes_used()),
                        value("`created_time`", DateHelper.nowMillis()),
                        value("`updated_time`", DateHelper.nowMillis()),
                        value("`publish_time`", DateHelper.nowMillis()),
                        value("`photos_etag`", album.getPhotos_etag()),
                        value("`photos_dirty`", album.getPhotos_dirty())
                )
                .toString();
        return sql;
    }

    public static String disableAlbum(String table, long userId, long... albumId) {
        Sql sql = new Sql().deleteFrom(table).where("1 = 1 ");
        if (albumId.length == 1)
            sql.and("`album_id`=:album_id", "album_id", albumId[0]);
        else
            sql.and("`album_id` IN (album_id)", "album_id", StringHelper.join(albumId, ","));
        if (userId != 0) {
            sql.and("user_id =:user_id", "user_id", userId);
        }
        return sql.toString();
    }

    public static String ifExistsAlbumName(String table, long userId, String albumName, int albumType) {
        return new Sql().select("*")
                .from(table)
                .where("`user_id`=:user_id", "user_id", userId)
                .and("`title`=:title", "title", albumName)
                .and("`album_type`=:album_type", "album_type", albumType)
                .toString();
    }

    public static String ifExistsAlbumNameForUpdate(String table, String album_id, String user_id, String album_name, int album_type) {
        return new Sql().select("count(album_id)")
                .from(table)
                .where("`user_id`=:user_id", "user_id", user_id)
                .and("`title`=:title", "title", album_name)
                .and("`album_type`=:album_type", "album_type", album_type)
                .and("`album_id`<>:album_id", "album_id", album_id)
                .toString();
    }

    public static String updateAlbum(String table, Album album) {
        return new Sql()
                .update(table)
                .setValues(
                        valueIf("`title`", album.getTitle(), StringUtils.isNotEmpty(album.getTitle())),
                        valueIf("`summary`", album.getSummary(), StringUtils.isNotEmpty(album.getSummary())),
                        //valueIf("`cover_photo_id`", album.getCover_photo_id(), album.getCover_photo_id() >= 0),
                        valueIf("`privacy`", album.getPrivacy(), album.getPrivacy())
                        /*valueIf("`can_upload`", album.getCan_upload(), album.getCan_upload() >= 0),
                        valueIf("`num_photos`", album.getNum_photos(), album.getNum_photos() > 0),
                        valueIf("`location`", album.getLocation(), StringUtils.isNotEmpty(album.getLocation())),
                        valueIf("`html_page_url`", album.getHtml_page_url(), StringUtils.isNotEmpty(album.getHtml_page_url())),
                        valueIf("`thumbnail_url`", album.getThumbnail_url(), StringUtils.isNotEmpty(album.getThumbnail_url())),
                        valueIf("`bytes_used`", album.getBytes_used(), album.getBytes_used() > 0),
                        valueIf("`updated_time`", album.getUpdated_time(), album.getUpdated_time() > 0),
                        valueIf("`publish_time`", album.getPublish_time(), album.getPublish_time() > 0),
                        valueIf("`photos_etag`", album.getPhotos_etag(), StringUtils.isNotEmpty(album.getPhotos_etag())),
                        valueIf("`photos_dirty`", album.getPhotos_dirty(), album.getPhotos_dirty() >= 0)*/
                ).where("album_id=:album_id", "album_id", album.getAlbum_id())
                .toString();
    }

    public static String getAlbums(String table, long... albumIds) {
        Sql sql = new Sql().select("*")
                .from(table)
                .where("1=1");
        if (albumIds.length == 1)
            sql.and("`album_id`=:albumIds", "albumIds", albumIds[0]);
        else
            sql.and("`album_id` IN (albumIds)", "albumIds", StringHelper.join(albumIds, ","));
        sql.orderBy("created_time ","DESC");
        return sql.toString();
    }

    public static String getUserAlbums(String table, long userId, int albumType, String title) {
        Sql sql = new Sql().select("*")
                .from(table).useIndex("`user_id`")
                .where("`user_id`=:user_id", "user_id", userId);
        if (albumType >= 0)
            sql.and("`album_type`=:album_type", "album_type", albumType);
        if (StringUtils.isNotEmpty(title))
            sql.and("`title`=:title", "title", title);
        return sql.toString();
    }

    public static String insertPhoto(String table, Photo photo) {
        Sql sql = new Sql().insertIgnoreInto(table)
                .values(
                        value("`photo_id`", photo.getPhoto_id()),
                        value("`user_id`", photo.getUser_id()),
                        value("`title`", photo.getTitle()),
                        value("`thumbnail_url`", photo.getThumbnail_url()),
                        value("`summary`", photo.getSummary()),
                        value("`keywords`", photo.getKeywords()),
                        value("`content_type`", photo.getContent_type()),
                        value("`content_url`", photo.getContent_url()),
                        value("`html_page_url`", photo.getHtml_page_url()),
                        value("`size`", photo.getSize()),
                        value("`height`", photo.getHeight()),
                        value("`width`", photo.getWidth()),
                        value("`album_id`", photo.getAlbum_id()),
                        value("`longitude`", photo.getLongitude()),
                        value("`latitude`", photo.getLatitude()),
                        value("`location`", photo.getLocation()),
                        value("`tags`", photo.getTags()),
                        value("`fingerprint`", photo.getFingerprint()),
                        value("`face_rectangles`", photo.getFace_rectangles()),
                        value("`face_names`", photo.getFace_names()),
                        value("`face_ids`", photo.getFace_ids()),
                        value("`exif_model`", photo.getExif_model()),
                        value("`exif_make`", photo.getExif_make()),
                        value("`exif_focal_length`", photo.getExif_focal_length()),
                        value("`created_time`", DateHelper.nowMillis()),
                        value("`updated_time`", DateHelper.nowMillis()),
                        value("`taken_time`", DateHelper.nowMillis()),
                        value("`published_time`", DateHelper.nowMillis()),
                        value("`fingerprint_hash`", photo.getFingerprint_hash()),
                        value("`display_index`", photo.getDisplay_index()),
                        value("`exif_exposure`", photo.getExif_exposure()),
                        value("`exif_flash`", photo.getExif_flash()),
                        value("`rotation`", photo.getRotation()),
                        value("`camera_sync`", photo.getCamera_sync()),
                        value("`exif_iso`", photo.getExif_iso()),
                        value("`exif_fstop`", photo.getExif_fstop())
                );

        return sql.toString();
    }

    public static String disablePhoto(String table, long userId, long... photoId) {
        Sql sql = new Sql().deleteFrom(table).where("1 = 1 ");
        if (photoId.length == 1)
            sql.and("`photo_id`=:photo_id", "photo_id", photoId[0]);
        else
            sql.and("`photo_id` IN (photo_id)", "photo_id", StringHelper.join(photoId, ","));
        if (userId != 0) {
            sql.and("user_id =:user_id", "user_id", userId);
        }
        return sql.toString();
    }

    public static String updatePhoto(String table, Photo photo) {
        return new Sql()
                .update(table)
                .setValues(
                        valueIf("`title`", photo.getTitle(), StringUtils.isNotEmpty(photo.getTitle())),
                        /*valueIf("`summary`", photo.getSummary(), StringUtils.isNotEmpty(photo.getSummary())),
                        valueIf("`keywords`", photo.getKeywords(), StringUtils.isNotEmpty(photo.getKeywords())),
                        valueIf("`longitude`", photo.getLongitude(), StringUtils.isNotEmpty(photo.getLongitude())),
                        valueIf("`latitude`", photo.getLatitude(), StringUtils.isNotEmpty(photo.getLatitude())),*/
                        valueIf("`location`", photo.getLocation(), StringUtils.isNotEmpty(photo.getLocation()))
                        /*valueIf("`tags`", photo.getTags(), StringUtils.isNotEmpty(photo.getTags())),
                        valueIf("`fingerprint`", photo.getFingerprint(), StringUtils.isNotEmpty(photo.getFingerprint())),
                        valueIf("`face_rectangles`", photo.getFace_rectangles(), StringUtils.isNotEmpty(photo.getFace_rectangles())),
                        valueIf("`face_names`", photo.getFace_names(), StringUtils.isNotEmpty(photo.getFace_names())),
                        valueIf("`face_ids`", photo.getFace_ids(), StringUtils.isNotEmpty(photo.getFace_ids())),
                        valueIf("`exif_model`", photo.getExif_model(), StringUtils.isNotEmpty(photo.getExif_model())),
                        valueIf("`exif_make`", photo.getExif_make(), StringUtils.isNotEmpty(photo.getExif_make())),
                        valueIf("`exif_focal_length`", photo.getExif_focal_length(), StringUtils.isNotEmpty(photo.getExif_focal_length())),
                        valueIf("`updated_time`", photo.getUpdated_time(), photo.getUpdated_time() > 0),
                        valueIf("`taken_time`", photo.getTaken_time(), photo.getTaken_time() > 0),
                        valueIf("`published_time`", photo.getPublished_time(), photo.getPublished_time() > 0),
                        valueIf("`fingerprint_hash`", photo.getFingerprint_hash(), photo.getFingerprint_hash() >= 0),
                        valueIf("`display_index`", photo.getDisplay_index(), photo.getFingerprint_hash() >= 0),
                        valueIf("`exif_exposure`", photo.getExif_exposure(), StringUtils.isNotEmpty(photo.getExif_exposure())),
                        valueIf("`exif_flash`", photo.getExif_flash(), StringUtils.isNotEmpty(photo.getExif_exposure())),
                        valueIf("`rotation`", photo.getRotation(), photo.getRotation() > 0),
                        valueIf("`camera_sync`", photo.getCamera_sync(), photo.getCamera_sync() > 0),
                        valueIf("`exif_iso`", photo.getExif_iso(), photo.getExif_iso() > 0),
                        valueIf("`exif_fstop`", photo.getExif_fstop(), photo.getExif_fstop() > 0)*/
                ).where("photo_id=:photo_id", "photo_id", photo.getPhoto_id())
                .toString();
    }

    public static String tagPhoto(String table, long photoId, String tags) {
        Photo photo = new Photo();
        photo.setPhoto_id(photoId);
        photo.setTags(tags);
        return updatePhoto(table, photo);
    }

    public static String getLastedPhoto(String table, long albumId) {
        return new Sql().select("*")
                .from(table)
                .where("`album_id`=:album_id", "album_id", albumId)
                .orderBy("created_time","DESC")
                .limit(1)
                .toString();
    }

    public static String getPhotos(String table, long... photoId) {
        Sql sql = new Sql().select("*").from(table).where("1 = 1 ");
        if (photoId.length == 1)
            sql.and("`photo_id`=:photo_id", "photo_id", photoId[0]);
        else
            sql.and("`photo_id` IN (photo_id)", "photo_id", StringHelper.join(photoId, ","));

        return sql.toString();
    }

    public static String getPhotosByAlbumId(String table,Page page, long... albumIds) {
        Sql sql = new Sql().select("*").from(table).where("1 = 1 ");
        if (albumIds.length == 1)
            sql.and("`album_id`=:album_id", "album_id", albumIds[0]);
        else
            sql.and("`album_id` IN (albumIds)", "albumIds", StringHelper.join(albumIds, ","));

        sql.orderBy("created_time","DESC");
        sql.page(page);
        return sql.toString();
    }

    public static String searchPhotos(String table, String keyWords) {
        Sql sql = new Sql().select("*").from(table).where("1 = 1 ");
        if (!keyWords.equals("")) {
            sql.and("`keywords` like '%" + keyWords + "%'");
        } else {
            sql.and("1=2");
        }
        return sql.toString();
    }

    public static String getPhotosNearBy(String table, String longitude, String latitude) {
        Sql sql = new Sql().select("*").from(table).where("length(longitude)>0 and length(latitude)>0 ");
        //TODO : ADD LOGIC
        return sql.toString();
    }

    public static String getPhotosIncludeMe(String table, long userId) {
        Sql sql = new Sql().select("*").from(table).where("1 = 1 ");
        sql.and("instr(concat(',',face_ids,','),concat(','," + userId + ",','))>0").orderBy("created_time", "DESC");
        return sql.toString();
    }

    public static String getPhotosByUserId(String table, long userId) {
        Sql sql = new Sql().select("*").from(table);
        sql.and("user_id =:user_id", "user_id", userId)
                .orderBy("created_time", "DESC");
        return sql.toString();
    }
}
