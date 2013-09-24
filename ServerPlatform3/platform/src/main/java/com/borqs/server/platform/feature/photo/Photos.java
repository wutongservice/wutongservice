package com.borqs.server.platform.feature.photo;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.*;

public class Photos extends ArrayList<Photo> implements JsonBean {
    public static final String V1COL_PHOTO_ID = "photo_id";
    public static final String V1COL_ALBUM_ID = "album_id";
    public static final String V1COL_USER_ID = "user_id";
    public static final String V1COL_IMG_MIDDLE = "img_middle";
    public static final String V1COL_IMG_BIG = "img_big";
    public static final String V1COL_IMG_SMALL = "img_small";
    public static final String V1COL_CAPTION = "caption";
    public static final String V1COL_LOCATION = "location";
    public static final String V1COL_TAG = "tag";
    public static final String V1COL_ALBUM_NAME = "album_name";
    public static final String V1COL_ALBUM_PHOTO_COUNTE = "album_photo_count";
    public static final String V1COL_ALBUM_COVER_PHOTO_ID = "album_cover_photo_id";
    public static final String V1COL_ALBUM_COVER_PHOTO_BIG = "album_cover_photo_big";
    public static final String V1COL_ALBUM_COVER_PHOTO_MIDDLE = "album_cover_photo_middle";
    public static final String V1COL_ALBUM_COVER_PHOTO_SMALL = "album_cover_photo_small";
    public static final String V1COL_ALBUM_DESCRIPTION = "album_description";
    public static final String V1COL_ALBUM_VISIBLE = "album_visible";

    public Photos() {
    }

    public Photos(int initialCapacity) {
        super(initialCapacity);
    }

    public Photos(Collection<? extends Photo> c) {
        super(c);
    }

    public Photos(Photo... photos) {
        Collections.addAll(this, photos);
    }


    public long[] getPhotoIds() {
        Set<Long> set = new HashSet<Long>();
        for (Photo photo : this) {
            long Photo_id = photo.getPhoto_id();
            if (!set.contains(Photo_id))
                set.add(Photo_id);
        }
        return CollectionsHelper.toLongArray(set);
    }

    public String[] gePhotoTitle() {
        Set<String> set = new HashSet<String>();
        for (Photo photo : this) {
            String title = photo.getTitle();
            if (!set.contains(title))
                set.add(title);
        }
        return set.toArray(new String[set.size()]);
    }

    public String[] gePhotoHtmlPageUrl() {
        Set<String> set = new HashSet<String>();
        for (Photo photo : this) {
            String html_page_url = photo.getHtml_page_url();
            if (!set.contains(html_page_url))
                set.add(html_page_url);
        }
        return set.toArray(new String[set.size()]);
    }

    public String[] gePhotoThumbnailUrl() {
        Set<String> set = new HashSet<String>();
        for (Photo photo : this) {
            String thumbnail_url = photo.getThumbnail_url();
            if (!set.contains(thumbnail_url))
                set.add(thumbnail_url);
        }
        return set.toArray(new String[set.size()]);
    }

    @Override
    public void deserialize(JsonNode jn) {
        for (int i = 0; i < jn.size(); i++) {
            Photo photo = Photo.fromJsonNode(jn.get(i));
            add(photo);
        }
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[]) null);
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartArray();
        for (Photo photo : this) {
            if (photo != null)
                photo.serialize(jg, cols);
        }
        jg.writeEndArray();
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }

    public static Photos fromJsonNode(Photos reuse, JsonNode jn) {
        Photos photos = reuse != null ? reuse : new Photos();
        photos.deserialize(jn);
        return photos;
    }

    public static Photos fromJson(Photos reuse, String json) {
        return fromJsonNode(reuse, JsonHelper.parse(json));
    }

    @Override
    public String toString() {
        return toJson(null, true);
    }


    public Photo getPhoto(long photoId) {
        for (Photo photo : this) {
            if (photo != null && photo.getPhoto_id() == photoId)
                return photo;
        }
        return null;
    }

    public Photos getComments(Photos reuse, long... commentIds) {
        if (reuse == null)
            reuse = new Photos();

        for (Photo photo : this) {
            if (photo != null && ArrayUtils.contains(commentIds, photo.getPhoto_id()))
                reuse.add(photo);
        }

        return reuse;
    }

    public Photo[] getPhotoArray() {
        return toArray(new Photo[size()]);
    }

    /* public static void serializePhotos(JsonGenerator jg, List<Photo> photos) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(photos)) {
            for (Photo photo : photos) {
                if (photo != null)
                    serializePhoto(jg, photo);
            }
        }
        jg.writeEndArray();
    }

    private static void serializePhoto(JsonGenerator jg, Photo photo) throws IOException {
        jg.writeStartObject();

        jg.writeNumberField(V1COL_PHOTO_ID, photo.getPhoto_id());
        jg.writeNumberField(V1COL_ALBUM_ID, photo.getAlbum_id());
        jg.writeNumberField(V1COL_USER_ID, photo.getUser_id());
        jg.writeStringField(V1COL_IMG_MIDDLE,photo.getThumbnail_url());
        jg.writeStringField(V1COL_IMG_BIG,photo.getThumbnail_url());
        jg.writeStringField(V1COL_IMG_SMALL, photo.getThumbnail_url());
        jg.writeStringField(V1COL_CAPTION, photo.getTitle());
        jg.writeStringField(V1COL_LOCATION, photo.getLocation());
        jg.writeStringField(V1COL_TAG, photo.getTags());

        jg.writeStringField(V1COL_ALBUM_NAME, (String) photo.getAddon(V1COL_ALBUM_NAME, ""));
        jg.writeStringField(V1COL_ALBUM_PHOTO_COUNTE, (String) photo.getAddon(V1COL_ALBUM_PHOTO_COUNTE, ""));
        jg.writeStringField(V1COL_ALBUM_COVER_PHOTO_ID, (String) photo.getAddon(V1COL_ALBUM_COVER_PHOTO_ID, ""));
        jg.writeStringField(V1COL_ALBUM_COVER_PHOTO_BIG, (String) photo.getAddon(V1COL_ALBUM_COVER_PHOTO_BIG, ""));
        jg.writeStringField(V1COL_ALBUM_COVER_PHOTO_MIDDLE, (String) photo.getAddon(V1COL_ALBUM_COVER_PHOTO_MIDDLE, ""));
        jg.writeStringField(V1COL_ALBUM_COVER_PHOTO_SMALL, (String) photo.getAddon(V1COL_ALBUM_COVER_PHOTO_SMALL, ""));
        jg.writeStringField(V1COL_ALBUM_DESCRIPTION, (String) photo.getAddon(V1COL_ALBUM_DESCRIPTION, ""));
        jg.writeStringField(V1COL_ALBUM_VISIBLE, (String) photo.getAddon(V1COL_ALBUM_VISIBLE, ""));

        jg.writeEndObject();
    }*/
    public Target[] getPhotoTargets() {
        ArrayList<Target> targets = new ArrayList<Target>();
        for (Photo photo : this) {
            if (photo != null)
                targets.add(photo.getPhotoTarget());
        }
        return targets.toArray(new Target[targets.size()]);
    }

    public Photos getPhotosByAlbum(long albumId) {
        Photos photos = new Photos();
        for (Photo photo : this) {
            if (photo != null && photo.getAlbum_id() == albumId)
                photos.add(photo);
        }
        return photos;
    }

    public long getPhotoSizeByAlbum(long albumId) {
        long l = 0;
        for (Photo photo : this) {
            if (photo != null && photo.getAlbum_id() == albumId)
                l++;
        }
        return l;
    }
}
