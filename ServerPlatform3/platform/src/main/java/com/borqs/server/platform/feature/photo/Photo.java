package com.borqs.server.platform.feature.photo;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.CommentExpansionSupport;
import com.borqs.server.platform.feature.comment.CommentPhotoExpansion;
import com.borqs.server.platform.feature.like.LikeExpansionSupport;
import com.borqs.server.platform.feature.like.LikePhotoExpansion;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public class Photo extends Addons implements JsonBean, Copyable<Photo> {

    public static int ALBUM_TYPE_PROFILE = 0;
    public static int ALBUM_TYPE_SHARE_OUT = 1;
    public static int ALBUM_TYPE_COVER = 2;
    public static int ALBUM_TYPE_RECEIVED = 3;
    public static int ALBUM_TYPE_PUBLIC_CIRCLE = 4;
    public static int ALBUM_TYPE_MY_SYNC = 8;
    public static int ALBUM_TYPE_OTHERS = 9;
    private long photo_id;
    private long user_id;
    private String title;
    private String summary;
    private String thumbnail_url;
    private String keywords;
    private String content_type;
    private String content_url;
    private String html_page_url;
    private long size;
    private int height;
    private int width;
    private long album_id;
    private String longitude;
    private String latitude;
    private String location;
    private String tags;
    private String fingerprint;
    private String face_rectangles;
    private String face_names;
    private String face_ids;
    private String exif_model;
    private String exif_make;
    private String exif_focal_length;
    private long created_time;
    private long updated_time;
    private long taken_time;
    private long published_time;
    private long fingerprint_hash;
    private long display_index;
    private String exif_exposure;
    private String exif_flash;
    private int rotation;
    private int camera_sync;
    private int exif_iso;
    private int exif_fstop;


    public static final String COL_PHOTO_ID = "photo_id";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_TITLE = "title";
    public static final String COL_SUMMARY = "summary";
    public static final String COL_THUMBNAIL_URL = "thumbnail_url";
    public static final String COL_KEYWORDS = "keywords";
    public static final String COL_CONTENT_TYPE = "content_type";
    public static final String COL_CONTENT_URL = "content_url";
    public static final String COL_HTML_PAGE_URL = "html_page_url";
    public static final String COL_SIZE = "size";
    public static final String COL_HEIGHT = "height";
    public static final String COL_WIDTH = "width";
    public static final String COL_ALBUM_ID = "album_id";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LOCATION = "location";
    public static final String COL_TAGS = "tags";
    public static final String COL_FINGERPRINT = "fingerprint";
    public static final String COL_FACE_RECTANGLES = "face_rectangles";
    public static final String COL_FACE_NAMES = "face_names";
    public static final String COL_FACE_IDS = "face_ids";
    public static final String COL_EXIF_MODEL = "exif_model";
    public static final String COL_EXIF_MAKE = "exif_make";
    public static final String COL_EXIF_FOCAL_LENGTH = "exif_focal_length";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_UPDATED_TIME = "updated_time";
    public static final String COL_TAKEN_TIME = "taken_time";
    public static final String COL_PUBLISHED_TIME = "published_time";
    public static final String COL_FINGERPRINT_HASH = "fingerprint_hash";
    public static final String COL_DISPLAY_INDEX = "display_index";
    public static final String COL_EXIF_EXPOSURE = "exif_exposure";
    public static final String COL_EXIF_FLASH = "exif_flash";
    public static final String COL_ROTATION = "rotation";
    public static final String COL_CAMERA_SYNC = "camera_sync";
    public static final String COL_EXIF_ISO = "exif_iso";
    public static final String COL_EXIF_FSTOP = "exif_fstop";

    public static final String[] STANDARD_COLUMNS = {
            COL_PHOTO_ID,
            COL_USER_ID,
            COL_CREATED_TIME,
            COL_TITLE,
            COL_SUMMARY,
            COL_THUMBNAIL_URL,
            COL_ALBUM_ID
    };
    public static final String[] FULL_COLUMNS = {
            COL_PHOTO_ID,
            COL_USER_ID,
            COL_TITLE,
            COL_SUMMARY,
            COL_THUMBNAIL_URL,
            COL_KEYWORDS,
            COL_CONTENT_TYPE,
            COL_CONTENT_URL,
            COL_HTML_PAGE_URL,
            COL_SIZE,
            COL_HEIGHT,
            COL_WIDTH,
            COL_ALBUM_ID,
            COL_LONGITUDE,
            COL_LATITUDE,
            COL_LOCATION,
            COL_TAGS,
            COL_FINGERPRINT,
            COL_FACE_RECTANGLES,
            COL_FACE_NAMES,
            COL_FACE_IDS,
            COL_EXIF_MODEL,
            COL_EXIF_MAKE,
            COL_EXIF_FOCAL_LENGTH,
            COL_CREATED_TIME,
            COL_UPDATED_TIME,
            COL_TAKEN_TIME,
            COL_PUBLISHED_TIME,
            COL_FINGERPRINT_HASH,
            COL_DISPLAY_INDEX,
            COL_EXIF_EXPOSURE,
            COL_EXIF_FLASH,
            COL_ROTATION,
            COL_CAMERA_SYNC,
            COL_EXIF_ISO,
            COL_EXIF_FSTOP,
            //LikeExpansionSupport.COL_LIKED,
            LikeExpansionSupport.COL_LIKES,
            CommentExpansionSupport.COL_COMMENTS,
            
    };

    public Photo() {
    }

    public Photo(long photo_id, long user_id, String title, String summary, String thumbnail_url, String keywords, String content_type, String content_url,
                 String html_page_url, long size, int height, int width, long album_id, String longitude, String latitude, String location, String tags,
                 String fingerprint, String face_rectangles, String face_names, String face_ids, String exif_model, String exif_make, String exif_focal_length,
                 long created_time, long updated_time, long taken_time, long published_time, long fingerprint_hash, long display_index, String exif_exposure, String exif_flash,
                 int rotation, int camera_sync, int exif_iso, int exif_fstop) {
        this.photo_id = photo_id;
        this.user_id = user_id;
        this.title = title;
        this.summary = summary;
        this.thumbnail_url = thumbnail_url;
        this.keywords = keywords;
        this.content_type = content_type;
        this.content_url = content_url;
        this.html_page_url = html_page_url;
        this.location = location;
        this.html_page_url = html_page_url;
        this.size = size;
        this.height = height;
        this.width = width;
        this.album_id = album_id;
        this.longitude = longitude;
        this.latitude = latitude;
        this.tags = tags;
        this.fingerprint = fingerprint;
        this.face_rectangles = face_rectangles;
        this.face_names = face_names;
        this.face_ids = face_ids;
        this.exif_model = exif_model;
        this.exif_make = exif_make;
        this.exif_focal_length = exif_focal_length;
        this.created_time = created_time;
        this.updated_time = updated_time;
        this.taken_time = taken_time;
        this.published_time = published_time;
        this.fingerprint_hash = fingerprint_hash;
        this.display_index = display_index;
        this.exif_exposure = exif_exposure;
        this.exif_flash = exif_flash;
        this.rotation = rotation;
        this.camera_sync = camera_sync;
        this.exif_iso = exif_iso;
        this.exif_fstop = exif_fstop;
    }

    public Photo(long photo_id, long user_id, String title, String summary, String thumbnail_url, String keywords, String content_type, String content_url,
                 String html_page_url, long size, int height, int width, long album_id, String longitude, String latitude, String location, String tags,
                 long created_time, long updated_time, long published_time) {
        this.photo_id = photo_id;
        this.user_id = user_id;
        this.title = title;
        this.summary = summary;
        this.thumbnail_url = thumbnail_url;
        this.keywords = keywords;
        this.content_type = content_type;
        this.content_url = content_url;
        this.html_page_url = html_page_url;
        this.location = location;
        this.html_page_url = html_page_url;
        this.size = size;
        this.height = height;
        this.width = width;
        this.album_id = album_id;
        this.longitude = longitude;
        this.latitude = latitude;
        this.tags = tags;
        this.created_time = created_time;
        this.updated_time = updated_time;
        this.published_time = published_time;
    }

    public Target getPhotoTarget() {
        return Target.of(Target.PHOTO, photo_id);
    }

    public long getPhoto_id() {
        return photo_id;
    }

    public void setPhoto_id(long photo_id) {
        this.photo_id = photo_id;
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

    public String getThumbnail_url() {
        return thumbnail_url;
    }

    public void setThumbnail_url(String thumbnail_url) {
        this.thumbnail_url = thumbnail_url;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;

    }

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type(String content_type) {
        this.content_type = content_type;
    }

    public String getContent_url() {
        return content_url;
    }

    public void setContent_url(String content_url) {
        this.content_url = content_url;
    }

    public String getHtml_page_url() {
        return html_page_url;
    }

    public void setHtml_page_url(String html_page_url) {
        this.html_page_url = html_page_url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public long getAlbum_id() {
        return album_id;
    }

    public void setAlbum_id(long album_id) {
        this.album_id = album_id;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getFace_rectangles() {
        return face_rectangles;
    }

    public void setFace_rectangles(String face_rectangles) {
        this.face_rectangles = face_rectangles;
    }

    public String getFace_names() {
        return face_names;
    }

    public void setFace_names(String face_names) {
        this.face_names = face_names;
    }

    public String getFace_ids() {
        return face_ids;
    }

    public void setFace_ids(String face_ids) {
        this.face_ids = face_ids;
    }

    public String getExif_model() {
        return exif_model;
    }

    public void setExif_model(String exif_model) {
        this.exif_model = exif_model;
    }

    public String getExif_make() {
        return exif_make;
    }

    public void setExif_make(String exif_make) {
        this.exif_make = exif_make;
    }

    public String getExif_focal_length() {
        return exif_focal_length;
    }

    public void setExif_focal_length(String exif_focal_length) {
        this.exif_focal_length = exif_focal_length;
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

    public long getTaken_time() {
        return taken_time;
    }

    public void setTaken_time(long taken_time) {
        this.taken_time = taken_time;
    }

    public long getPublished_time() {
        return published_time;
    }

    public void setPublished_time(long published_time) {
        this.published_time = published_time;
    }

    public long getFingerprint_hash() {
        return fingerprint_hash;
    }

    public void setFingerprint_hash(long fingerprint_hash) {
        this.fingerprint_hash = fingerprint_hash;
    }

    public long getDisplay_index() {
        return display_index;
    }

    public void setDisplay_index(long display_index) {
        this.display_index = display_index;
    }

    public String getExif_exposure() {
        return exif_exposure;
    }

    public void setExif_exposure(String exif_exposure) {
        this.exif_exposure = exif_exposure;
    }

    public String getExif_flash() {
        return exif_flash;
    }

    public void setExif_flash(String exif_flash) {
        this.exif_flash = exif_flash;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public int getCamera_sync() {
        return camera_sync;
    }

    public void setCamera_sync(int camera_sync) {
        this.camera_sync = camera_sync;
    }

    public int getExif_iso() {
        return exif_iso;
    }

    public void setExif_iso(int exif_iso) {
        this.exif_iso = exif_iso;
    }

    public int getExif_fstop() {
        return exif_fstop;
    }

    public void setExif_fstop(int exif_fstop) {
        this.exif_fstop = exif_fstop;
    }

    @Override
    public Photo copy() {
        Photo photo = new Photo();
        photo.setAlbum_id(album_id);
        photo.setCamera_sync(camera_sync);
        photo.setContent_type(content_type);
        photo.setContent_url(content_url);
        photo.setCreated_time(created_time);
        photo.setDisplay_index(display_index);
        photo.setExif_exposure(exif_exposure);
        photo.setExif_flash(exif_flash);
        photo.setExif_focal_length(exif_focal_length);
        photo.setExif_fstop(exif_fstop);
        photo.setExif_iso(exif_iso);
        photo.setExif_make(exif_make);
        photo.setExif_model(exif_model);
        photo.setFace_ids(face_ids);
        photo.setFace_names(face_names);
        photo.setFace_rectangles(face_rectangles);
        photo.setFingerprint(fingerprint);
        photo.setHeight(height);
        photo.setHtml_page_url(html_page_url);
        photo.setKeywords(keywords);
        photo.setLatitude(latitude);
        photo.setLocation(location);
        photo.setLongitude(longitude);
        photo.setPhoto_id(photo_id);
        photo.setPublished_time(published_time);
        photo.setRotation(rotation);
        photo.setSize(size);
        photo.setSummary(summary);
        photo.setTags(tags);
        photo.setTaken_time(taken_time);
        photo.setTitle(title);
        photo.setUpdated_time(updated_time);
        photo.setWidth(width);
        photo.setUser_id(user_id);
        photo.setFingerprint_hash(fingerprint_hash);
        photo.setThumbnail_url(thumbnail_url);
        return photo;
    }

    public static Photo fromJsonNode(JsonNode jn) {
        Photo photo = new Photo();
        photo.deserialize(jn);
        return photo;
    }

    public static Photo fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[]) null);
    }

    @Override
    public String toString() {
        return toJson(null, true);
        //return super.toString();
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }


    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_PHOTO_ID))
            jg.writeNumberField(COL_PHOTO_ID, getPhoto_id());

        if (outputColumn(cols, COL_USER_ID))
            jg.writeNumberField(COL_USER_ID, getUser_id());
        if (outputColumn(cols, COL_TITLE))
            jg.writeStringField(COL_TITLE, getTitle());
        if (outputColumn(cols, COL_SUMMARY))
            jg.writeStringField(COL_SUMMARY, getSummary());
        if (outputColumn(cols, COL_THUMBNAIL_URL))
            jg.writeStringField(COL_THUMBNAIL_URL, getThumbnail_url());
        if (outputColumn(cols, COL_KEYWORDS))
            jg.writeStringField(COL_KEYWORDS, getKeywords());
        if (outputColumn(cols, COL_CONTENT_TYPE))
            jg.writeStringField(COL_CONTENT_TYPE, getContent_type());
        if (outputColumn(cols, COL_CONTENT_URL))
            jg.writeStringField(COL_CONTENT_URL, getContent_url());
        if (outputColumn(cols, COL_HTML_PAGE_URL))
            jg.writeStringField(COL_HTML_PAGE_URL, getHtml_page_url());
        if (outputColumn(cols, COL_SIZE))
            jg.writeNumberField(COL_SIZE, getSize());
        if (outputColumn(cols, COL_HEIGHT))
            jg.writeNumberField(COL_HEIGHT, getHeight());
        if (outputColumn(cols, COL_WIDTH))
            jg.writeNumberField(COL_WIDTH, getWidth());
        if (outputColumn(cols, COL_ALBUM_ID))
            jg.writeNumberField(COL_ALBUM_ID, getAlbum_id());
        if (outputColumn(cols, COL_LONGITUDE))
            jg.writeStringField(COL_LONGITUDE, getLongitude());
        if (outputColumn(cols, COL_LATITUDE))
            jg.writeStringField(COL_LATITUDE, getLatitude());
        if (outputColumn(cols, COL_LOCATION))
            jg.writeStringField(COL_LOCATION, getLocation());
        if (outputColumn(cols, COL_TAGS))
            jg.writeStringField(COL_TAGS, getTags());
        if (outputColumn(cols, COL_FINGERPRINT))
            jg.writeStringField(COL_FINGERPRINT, getFingerprint());
        if (outputColumn(cols, COL_FACE_RECTANGLES))
            jg.writeStringField(COL_FACE_RECTANGLES, getFace_rectangles());
        if (outputColumn(cols, COL_FACE_NAMES))
            jg.writeStringField(COL_FACE_NAMES, getFace_names());
        if (outputColumn(cols, COL_FACE_IDS))
            jg.writeStringField(COL_FACE_IDS, getFace_ids());
        if (outputColumn(cols, COL_EXIF_MODEL))
            jg.writeStringField(COL_EXIF_MODEL, getExif_model());
        if (outputColumn(cols, COL_EXIF_MAKE))
            jg.writeStringField(COL_EXIF_MAKE, getExif_make());
        if (outputColumn(cols, COL_EXIF_FOCAL_LENGTH))
            jg.writeStringField(COL_EXIF_FOCAL_LENGTH, getExif_focal_length());
        if (outputColumn(cols, COL_CREATED_TIME))
            jg.writeNumberField(COL_CREATED_TIME, getCreated_time());
        if (outputColumn(cols, COL_UPDATED_TIME))
            jg.writeNumberField(COL_UPDATED_TIME, getUpdated_time());
        if (outputColumn(cols, COL_TAKEN_TIME))
            jg.writeNumberField(COL_TAKEN_TIME, getTaken_time());
        if (outputColumn(cols, COL_PUBLISHED_TIME))
            jg.writeNumberField(COL_PUBLISHED_TIME, getPublished_time());
        if (outputColumn(cols, COL_FINGERPRINT_HASH))
            jg.writeNumberField(COL_FINGERPRINT_HASH, getFingerprint_hash());
        if (outputColumn(cols, COL_DISPLAY_INDEX))
            jg.writeNumberField(COL_DISPLAY_INDEX, getDisplay_index());
        if (outputColumn(cols, COL_EXIF_EXPOSURE))
            jg.writeStringField(COL_EXIF_EXPOSURE, getExif_exposure());
        if (outputColumn(cols, COL_EXIF_FLASH))
            jg.writeStringField(COL_EXIF_FLASH, getExif_flash());
        if (outputColumn(cols, COL_ROTATION))
            jg.writeNumberField(COL_ROTATION, getRotation());
        if (outputColumn(cols, COL_CAMERA_SYNC))
            jg.writeNumberField(COL_CAMERA_SYNC, getCamera_sync());
        if (outputColumn(cols, COL_EXIF_ISO))
            jg.writeNumberField(COL_EXIF_ISO, getExif_iso());
        if (outputColumn(cols, COL_EXIF_FSTOP))
            jg.writeNumberField(COL_EXIF_FSTOP, getExif_fstop());
        /*String[] addons = getAddonColumns();
        if (ArrayUtils.isNotEmpty(addons)) {
            for (String s : addons) {
                jg.writeStringField(s, getAddonAsString(s, ""));
            }
        }*/

        writeAddonsJson(jg, cols);
        jg.writeEndObject();
    }


    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_PHOTO_ID))
            setPhoto_id(jn.path(COL_PHOTO_ID).getValueAsLong());
        if (jn.has(COL_USER_ID))
            setUser_id(jn.path(COL_USER_ID).getValueAsLong());
        if (jn.has(COL_TITLE))
            setTitle(jn.path(COL_TITLE).getValueAsText());
        if (jn.has(COL_SUMMARY))
            setSummary(jn.path(COL_SUMMARY).getValueAsText());
        if (jn.has(COL_THUMBNAIL_URL))
            setThumbnail_url(jn.path(COL_THUMBNAIL_URL).getValueAsText());
        if (jn.has(COL_KEYWORDS))
            setKeywords(jn.path(COL_KEYWORDS).getValueAsText());
        if (jn.has(COL_CONTENT_TYPE))
            setContent_type(jn.path(COL_CONTENT_TYPE).getValueAsText());
        if (jn.has(COL_CONTENT_URL))
            setContent_url(jn.path(COL_CONTENT_URL).getValueAsText());
        if (jn.has(COL_HTML_PAGE_URL))
            setHtml_page_url(jn.path(COL_HTML_PAGE_URL).getValueAsText());
        if (jn.has(COL_SIZE))
            setSize(jn.path(COL_SIZE).getValueAsLong());
        if (jn.has(COL_HEIGHT))
            setHeight(jn.path(COL_HEIGHT).getValueAsInt());
        if (jn.has(COL_WIDTH))
            setWidth(jn.path(COL_WIDTH).getValueAsInt());
        if (jn.has(COL_ALBUM_ID))
            setAlbum_id(jn.path(COL_ALBUM_ID).getValueAsLong());
        if (jn.has(COL_LONGITUDE))
            setLongitude(jn.path(COL_LONGITUDE).getValueAsText());
        if (jn.has(COL_LATITUDE))
            setLatitude(jn.path(COL_LATITUDE).getValueAsText());
        if (jn.has(COL_LOCATION))
            setLocation(jn.path(COL_LOCATION).getValueAsText());
        if (jn.has(COL_TAGS))
            setTags(jn.path(COL_TAGS).getValueAsText());
        if (jn.has(COL_FINGERPRINT))
            setFingerprint(jn.path(COL_FINGERPRINT).getValueAsText());
        if (jn.has(COL_FACE_RECTANGLES))
            setFace_rectangles(jn.path(COL_FACE_RECTANGLES).getValueAsText());
        if (jn.has(COL_FACE_NAMES))
            setFace_names(jn.path(COL_FACE_NAMES).getValueAsText());
        if (jn.has(COL_FACE_IDS))
            setFace_ids(jn.path(COL_FACE_IDS).getValueAsText());
        if (jn.has(COL_EXIF_MODEL))
            setExif_model(jn.path(COL_EXIF_MODEL).getValueAsText());
        if (jn.has(COL_EXIF_MAKE))
            setExif_make(jn.path(COL_EXIF_MAKE).getValueAsText());
        if (jn.has(COL_EXIF_FOCAL_LENGTH))
            setExif_focal_length(jn.path(COL_EXIF_FOCAL_LENGTH).getValueAsText());
        if (jn.has(COL_CREATED_TIME))
            setCreated_time(jn.path(COL_CREATED_TIME).getValueAsLong());
        if (jn.has(COL_UPDATED_TIME))
            setUpdated_time(jn.path(COL_UPDATED_TIME).getValueAsLong());
        if (jn.has(COL_TAKEN_TIME))
            setTaken_time(jn.path(COL_TAKEN_TIME).getValueAsLong());
        if (jn.has(COL_PUBLISHED_TIME))
            setPublished_time(jn.path(COL_PUBLISHED_TIME).getValueAsLong());
        if (jn.has(COL_FINGERPRINT_HASH))
            setFingerprint_hash(jn.path(COL_FINGERPRINT_HASH).getValueAsLong());
        if (jn.has(COL_DISPLAY_INDEX))
            setDisplay_index(jn.path(COL_DISPLAY_INDEX).getValueAsLong());
        if (jn.has(COL_EXIF_EXPOSURE))
            setExif_exposure(jn.path(COL_EXIF_EXPOSURE).getValueAsText());
        if (jn.has(COL_EXIF_FLASH))
            setExif_flash(jn.path(COL_EXIF_FLASH).getValueAsText());
        if (jn.has(COL_ROTATION))
            setRotation(jn.path(COL_ROTATION).getValueAsInt());
        if (jn.has(COL_CAMERA_SYNC))
            setCamera_sync(jn.path(COL_CAMERA_SYNC).getValueAsInt());
        if (jn.has(COL_EXIF_ISO))
            setExif_iso(jn.path(COL_EXIF_ISO).getValueAsInt());
        if (jn.has(COL_EXIF_FSTOP))
            setExif_fstop(jn.path(COL_EXIF_FSTOP).getValueAsInt());
        if (jn.has(LikePhotoExpansion.COL_LIKED))
            setAddon(LikePhotoExpansion.COL_LIKED, jn.get(LikePhotoExpansion.COL_LIKED));

        if (jn.has(LikePhotoExpansion.COL_LIKES))
            setAddon(LikePhotoExpansion.COL_LIKES, jn.get(LikePhotoExpansion.COL_LIKES));

        if (jn.has(CommentPhotoExpansion.COL_COMMENTS))
            setAddon(CommentPhotoExpansion.COL_COMMENTS, jn.get(CommentPhotoExpansion.COL_COMMENTS));

    }
}