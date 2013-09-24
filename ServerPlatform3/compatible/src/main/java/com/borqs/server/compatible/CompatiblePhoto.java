package com.borqs.server.compatible;


import com.borqs.server.platform.feature.comment.CommentPhotoExpansion;
import com.borqs.server.platform.feature.like.LikePhotoExpansion;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.feature.photo.Photos;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.LinkedHashSet;

public class CompatiblePhoto {
    public static final String V1COL_PHOTO_ID = "photo_id";
    public static final String V1COL_ALBUM_ID = "album_id";
    public static final String V1COL_USER_ID = "user_id";
    public static final String V1COL_IMG_MIDDLE = "photo_img_middle";
    public static final String V1COL_IMG_BIG = "photo_img_big";
    public static final String V1COL_IMG_SMALL = "photo_img_small";
    public static final String V1COL_CAPTION = "photo_caption";
    public static final String V1COL_LOCATION = "photo_location";
    public static final String V1COL_TAG = "photo_tag";
    public static final String V1COL_CREATED_TIME = "photo_created_time";

    public static final String V1COL_LIKES = "likes";
    public static final String V1COL_LIKED = "iliked";
    public static final String V1COL_COMMENTS = "comments";

    //----------------album------------------
    public static final String V1COL_ALBUM_NAME = "album_name";
    public static final String V1COL_ALBUM_PHOTO_COUNT = "album_photo_count";
    public static final String V1COL_ALBUM_COVER_PHOTO_ID = "album_cover_photo_id";
    public static final String V1COL_ALBUM_COVER_PHOTO_MIDDLE = "album_cover_photo_id";
    public static final String V1COL_ALBUM_COVER_PHOTO_BIG = "album_cover_photo_big";
    public static final String V1COL_ALBUM_COVER_PHOTO_SMALL = "album_cover_photo_small";
    public static final String V1COL_ALBUM_DESCRIPTION = "album_description";
    public static final String V1COL_ALBUM_VISIBLE = "album_visible";

    public static final String[] V1_FULL_COLUMNS = {
            V1COL_PHOTO_ID,
            V1COL_ALBUM_ID,
            V1COL_USER_ID,
            V1COL_IMG_MIDDLE,
            V1COL_IMG_BIG,
            V1COL_IMG_SMALL,
            V1COL_CAPTION,
            V1COL_LOCATION,
            V1COL_TAG,
            V1COL_LIKES,
            V1COL_LIKED,
            V1COL_COMMENTS,
            V1COL_CREATED_TIME,
            /*V1COL_ALBUM_NAME,
            V1COL_ALBUM_PHOTO_COUNT,
            V1COL_ALBUM_COVER_PHOTO_ID,
            V1COL_ALBUM_COVER_PHOTO_MIDDLE,
            V1COL_ALBUM_COVER_PHOTO_BIG,
            V1COL_ALBUM_COVER_PHOTO_SMALL,
            V1COL_ALBUM_DESCRIPTION,
            V1COL_ALBUM_VISIBLE*/

    };

    public static String[] v1ToV2Columns(String[] v1Cols) {
        LinkedHashSet<String> l = new LinkedHashSet<String>();
        if (ArrayUtils.contains(v1Cols, V1COL_PHOTO_ID))
            l.add(Photo.COL_PHOTO_ID);
        if (ArrayUtils.contains(v1Cols, V1COL_ALBUM_ID))
            l.add(Photo.COL_ALBUM_ID);
        if (ArrayUtils.contains(v1Cols, V1COL_USER_ID))
            l.add(Photo.COL_USER_ID);

        if (ArrayUtils.contains(v1Cols, V1COL_IMG_MIDDLE))
            l.add(Photo.COL_THUMBNAIL_URL);
        if (ArrayUtils.contains(v1Cols, V1COL_IMG_BIG))
            l.add(Photo.COL_THUMBNAIL_URL);
        if (ArrayUtils.contains(v1Cols, V1COL_IMG_SMALL))
            l.add(Photo.COL_THUMBNAIL_URL);

        if (ArrayUtils.contains(v1Cols, V1COL_CAPTION))
            l.add(Photo.COL_TITLE);
        if (ArrayUtils.contains(v1Cols, V1COL_LOCATION))
            l.add(Photo.COL_LOCATION);
        if (ArrayUtils.contains(v1Cols, V1COL_TAG))
            l.add(Photo.COL_TAGS);

        if (ArrayUtils.contains(v1Cols, V1COL_LIKES))
            l.add(LikePhotoExpansion.COL_LIKES);
        if (ArrayUtils.contains(v1Cols, V1COL_LIKED))
            l.add(LikePhotoExpansion.COL_LIKED);

        if (ArrayUtils.contains(v1Cols, V1COL_COMMENTS))
            l.add(CommentPhotoExpansion.COL_COMMENTS);
        if (ArrayUtils.contains(v1Cols, V1COL_CREATED_TIME))
            l.add(Photo.COL_CREATED_TIME);

        return l.toArray(new String[l.size()]);
    }

    public static String photoToJson(final Photo photo, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializePhoto(jg, photo, v1Cols);
            }
        }, human);
    }

    public static String photosToJson(final Photos photos, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializePhotos(jg, photos, v1Cols);
            }
        }, human);
    }

    public static String v2JsonNodeToV1Json(JsonNode jn, String[] v1Cols, boolean human) {
        if (jn.isArray()) {
            final Photos photos = Photos.fromJsonNode(null, jn);
            return photosToJson(photos, v1Cols, human);
        } else if (jn.isObject()) {
            final Photo photo = Photo.fromJsonNode(jn);
            return photoToJson(photo, v1Cols, human);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static String v2ToV1Json(String json, String[] v1Cols, boolean human) {
        JsonNode jn = JsonHelper.parse(json);
        return v2JsonNodeToV1Json(jn, v1Cols, human);
    }


    public static void serializePhotos(JsonGenerator jg, Photos photos, final String[] v1Cols) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(photos)) {
            for (Photo photo : photos) {
                if (photo != null)
                    serializePhoto(jg, photo, v1Cols);
            }
        }
        jg.writeEndArray();
    }

    public static void serializePhoto(JsonGenerator jg, Photo photo, String[] v1Cols) throws IOException {
        jg.writeStartObject();
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_PHOTO_ID))
            jg.writeNumberField(V1COL_PHOTO_ID, photo.getPhoto_id());


        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ALBUM_ID))
            jg.writeNumberField(V1COL_ALBUM_ID, photo.getAlbum_id());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_USER_ID))
            jg.writeNumberField(V1COL_USER_ID, photo.getUser_id());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_IMG_MIDDLE))
            jg.writeStringField(V1COL_IMG_MIDDLE, StringUtils.replace(photo.getThumbnail_url(),"_S","_O"));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_TAG))
            jg.writeStringField(V1COL_TAG, photo.getTags());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_IMG_BIG))
            jg.writeStringField(V1COL_IMG_BIG, StringUtils.replace(photo.getThumbnail_url(), "_S", "_L"));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_IMG_SMALL))
            jg.writeStringField(V1COL_IMG_SMALL, photo.getThumbnail_url());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CAPTION))
            jg.writeStringField(V1COL_CAPTION, photo.getTitle());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LOCATION))
            jg.writeStringField(V1COL_LOCATION, photo.getLocation());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CREATED_TIME))
            jg.writeNumberField(V1COL_CREATED_TIME, photo.getCreated_time());

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LIKED))
            photo.writeAddonJsonAs(jg, LikePhotoExpansion.COL_LIKED, V1COL_LIKED);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LIKES))
            photo.writeAddonJsonAs(jg, LikePhotoExpansion.COL_LIKES, V1COL_LIKES, CompatibleLike.LIKES_ADDON_TRANSFORMER);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_COMMENTS))
            photo.writeAddonJsonAs(jg, CommentPhotoExpansion.COL_COMMENTS, V1COL_COMMENTS, CompatibleComment.COMMENTS_ADDON_TRANSFORMER);
        
        jg.writeNumberField("album_photo_count",0);//TODO XX
        jg.writeNumberField("album_cover_photo_id",0);//TODO XX
        jg.writeStringField("album_description","");//TODO XX
        jg.writeBooleanField("album_visible",true);//TODO XX
        jg.writeStringField("album_name","");//TODO XX

        jg.writeEndObject();
    }


}
