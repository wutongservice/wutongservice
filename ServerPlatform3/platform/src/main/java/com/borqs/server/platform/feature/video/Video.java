package com.borqs.server.platform.feature.video;

import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Video extends Addons implements JsonBean {

    public static final String COL_VIDEO_ID = "video_id";
    public static final String COL_TITLE = "title";
    public static final String COL_SUMMARY = "summary";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_LEVEL = "level";
    public static final String COL_FILE_SIZE = "file_size";
    public static final String COL_VIDEO_TIME_LENGTH = "video_time_length";
    public static final String COL_VIDEO_HEIGHT = "video_height";
    public static final String COL_VIDEO_WIDTH = "video_width";
    public static final String COL_VIDEO_DATA_RATE = "video_data_rate";
    public static final String COL_VIDEO_BIT_RATE = "video_bit_rate";
    public static final String COL_AUDIO_BIT_RATE = "audio_bit_rate";
    public static final String COL_VIDEO_FRAME_RATE = "video_frame_rate";
    public static final String COL_AUDIO_CHANNEL = "audio_channel";
    public static final String COL_AUDIO_SAMPLING_RATE = "audio_Sampling_rate";
    public static final String COL_THUMBNAIL = "thumbnail";
    public static final String COL_CODING_TYPE = "coding_type";
    public static final String COL_COMPRESSION_TYPE = "compression_type";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_EXP_NAME = "exp_name";
    public static final String COL_HTML_URL = "html_url";
    public static final String COL_ARTISTS = "artists";
    public static final String COL_CONTENT_TYPE = "content_type";
    public static final String COL_NEW_FILE_NAME = "new_file_name";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_UPDATED_TIME = "updated_time";
    public static final String COL_DESTROYED_TIME = "destroyed_time";


    private long videoId;
    private String title;
    private String summary;
    private String description;
    private int level;
    private long fileSize;
    private long createdTime;
    private long destroyedTime;
    private long updatedTime;
    private int videoTimeLength;
    private int videoWidth;
    private int videoHeight;
    private int videoDataRate;
    private int videoBitRate;
    private int videoFrameRate;
    private int audioBitRate;
    private int audioChannel;
    private int audioSamplingRate;
    private String thumbnail;
    private String codingType;
    private String compressionType;
    private String expName;
    private String htmlUrl;
    private String artists;
    private long userId;
    private String contentType;
    private String newFileName;


    public Video() {
        this(0L);
    }

    public Video(long videoId) {
        this.videoId = videoId;
    }

    public static final String[] STANDARD_COLUMNS = {
            COL_VIDEO_ID,
            COL_TITLE,
            COL_SUMMARY,
            COL_DESCRIPTION,
            COL_LEVEL,
            COL_FILE_SIZE,
            COL_VIDEO_TIME_LENGTH,
            COL_VIDEO_HEIGHT,
            COL_VIDEO_WIDTH,
            COL_VIDEO_DATA_RATE,
            COL_VIDEO_BIT_RATE,
            COL_AUDIO_BIT_RATE,
            COL_VIDEO_FRAME_RATE,
            COL_AUDIO_CHANNEL,
            COL_AUDIO_SAMPLING_RATE,
            COL_THUMBNAIL,
            COL_CODING_TYPE,
            COL_COMPRESSION_TYPE,
            COL_USER_ID,
            COL_EXP_NAME,
            COL_HTML_URL,
            COL_ARTISTS,
            COL_CONTENT_TYPE,
            COL_NEW_FILE_NAME,
            COL_CREATED_TIME,
            COL_UPDATED_TIME,
            COL_DESTROYED_TIME
    };
    public static final String[] FULL_COLUMNS = {
            COL_VIDEO_ID,
            COL_TITLE,
            COL_SUMMARY,
            COL_DESCRIPTION,
            COL_LEVEL,
            COL_FILE_SIZE,
            COL_VIDEO_TIME_LENGTH,
            COL_VIDEO_HEIGHT,
            COL_VIDEO_WIDTH,
            COL_VIDEO_DATA_RATE,
            COL_VIDEO_BIT_RATE,
            COL_AUDIO_BIT_RATE,
            COL_VIDEO_FRAME_RATE,
            COL_AUDIO_CHANNEL,
            COL_AUDIO_SAMPLING_RATE,
            COL_THUMBNAIL,
            COL_CODING_TYPE,
            COL_COMPRESSION_TYPE,
            COL_USER_ID,
            COL_EXP_NAME,
            COL_HTML_URL,
            COL_ARTISTS,
            COL_CONTENT_TYPE,
            COL_NEW_FILE_NAME,
            COL_CREATED_TIME,
            COL_UPDATED_TIME,
            COL_DESTROYED_TIME

    };


    private static Map<String, String[]> columnAliases = new ConcurrentHashMap<String, String[]>();

/*    static {
        registerColumnsAlias("@std,#std", STANDARD_COLUMNS);
        registerColumnsAlias("@full,#full", FULL_COLUMNS);
    }*/

    public static String[] expandColumns(String[] cols) {
        return ColumnsExpander.expand(cols, columnAliases);
    }

    public static void registerColumnsAlias(String alias, String[] cols) {
        columnAliases.put(alias, cols);
    }

    public static void unregisterColumnsAlias(String alias) {
        columnAliases.remove(alias);
    }

    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getDestroyedTime() {
        return destroyedTime;
    }

    public void setDestroyedTime(long destroyedTime) {
        this.destroyedTime = destroyedTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }


    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getVideoTimeLength() {
        return videoTimeLength;
    }

    public void setVideoTimeLength(int videoTimeLength) {
        this.videoTimeLength = videoTimeLength;
    }

    public String getArtists() {
        return artists;
    }

    public void setArtists(String artists) {
        this.artists = artists;
    }


    public String getCodingType() {
        return codingType;
    }

    public void setCodingType(String codingType) {
        this.codingType = codingType;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public String getExpName() {
        return expName;
    }

    public void setExpName(String expName) {
        this.expName = expName;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public static Map<String, String[]> getColumnAliases() {
        return columnAliases;
    }

    public static void setColumnAliases(Map<String, String[]> columnAliases) {
        Video.columnAliases = columnAliases;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public int getVideoDataRate() {
        return videoDataRate;
    }

    public void setVideoDataRate(int videoDataRate) {
        this.videoDataRate = videoDataRate;
    }

    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    public void setVideoFrameRate(int videoFrameRate) {
        this.videoFrameRate = videoFrameRate;
    }

    public int getAudioBitRate() {
        return audioBitRate;
    }

    public void setAudioBitRate(int audioBitRate) {
        this.audioBitRate = audioBitRate;
    }

    public int getAudioChannel() {
        return audioChannel;
    }

    public void setAudioChannel(int audioChannel) {
        this.audioChannel = audioChannel;
    }

    public int getAudioSamplingRate() {
        return audioSamplingRate;
    }

    public void setAudioSamplingRate(int audioSamplingRate) {
        this.audioSamplingRate = audioSamplingRate;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getVideoBitRate() {
        return videoBitRate;
    }

    public void setVideoBitRate(int videoBitRate) {
        this.videoBitRate = videoBitRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Video other = (Video) o;
        return videoId == other.videoId
                && createdTime == other.createdTime
                && destroyedTime == other.destroyedTime
                && StringUtils.equals(title, other.title)
                && StringUtils.equals(artists, other.artists)
                && videoTimeLength == other.videoTimeLength
                && destroyedTime == other.destroyedTime;
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(videoId, createdTime, destroyedTime,
                title, artists, updatedTime, description);
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_VIDEO_ID))
            setVideoId(jn.path(COL_VIDEO_ID).getValueAsLong());
        if (jn.has(COL_CREATED_TIME))
            setCreatedTime(jn.path(COL_CREATED_TIME).getValueAsLong());
        if (jn.has(COL_DESTROYED_TIME))
            setDestroyedTime(jn.path(COL_DESTROYED_TIME).getValueAsLong());
        if (jn.has(COL_UPDATED_TIME))
            setUpdatedTime(jn.path(COL_UPDATED_TIME).getValueAsLong());
        if (jn.has(COL_TITLE))
            setTitle(jn.path(COL_TITLE).getValueAsText());
        if (jn.has(COL_SUMMARY))
            setSummary(jn.path(COL_SUMMARY).getValueAsText());
        if (jn.has(COL_DESCRIPTION))
            setDescription(jn.path(COL_DESCRIPTION).getValueAsText());
        if (jn.has(COL_LEVEL))
            setLevel(jn.path(COL_LEVEL).getValueAsInt());
        if (jn.has(COL_FILE_SIZE))
            setFileSize(jn.path(COL_FILE_SIZE).getValueAsInt());
        if (jn.has(COL_VIDEO_TIME_LENGTH))
            setVideoTimeLength(jn.path(COL_VIDEO_TIME_LENGTH).getValueAsInt());
        if (jn.has(COL_VIDEO_WIDTH))
            setVideoTimeLength(jn.path(COL_VIDEO_WIDTH).getValueAsInt());
        if (jn.has(COL_VIDEO_HEIGHT))
            setVideoTimeLength(jn.path(COL_VIDEO_HEIGHT).getValueAsInt());
        if (jn.has(COL_VIDEO_DATA_RATE))
            setVideoTimeLength(jn.path(COL_VIDEO_DATA_RATE).getValueAsInt());
        if (jn.has(COL_VIDEO_BIT_RATE))
            setVideoBitRate(jn.path(COL_VIDEO_BIT_RATE).getValueAsInt());
        if (jn.has(COL_VIDEO_FRAME_RATE))
            setVideoTimeLength(jn.path(COL_VIDEO_FRAME_RATE).getValueAsInt());
        if (jn.has(COL_AUDIO_BIT_RATE))
            setAudioBitRate(jn.path(COL_AUDIO_BIT_RATE).getValueAsInt());
        if (jn.has(COL_AUDIO_CHANNEL))
            setAudioChannel(jn.path(COL_AUDIO_CHANNEL).getValueAsInt());
        if (jn.has(COL_AUDIO_SAMPLING_RATE))
            setAudioSamplingRate(jn.path(COL_AUDIO_SAMPLING_RATE).getValueAsInt());
        if (jn.has(COL_THUMBNAIL))
            setThumbnail(jn.path(COL_THUMBNAIL).getValueAsText());
        if (jn.has(COL_CODING_TYPE))
            setCodingType(jn.path(COL_CODING_TYPE).getValueAsText());
        if (jn.has(COL_COMPRESSION_TYPE))
            setCompressionType(jn.path(COL_COMPRESSION_TYPE).getValueAsText());
        if (jn.has(COL_EXP_NAME))
            setExpName(jn.path(COL_EXP_NAME).getValueAsText());
        if (jn.has(COL_HTML_URL))
            setHtmlUrl(jn.path(COL_HTML_URL).getValueAsText());
        if (jn.has(COL_ARTISTS))
            setArtists(jn.path(COL_ARTISTS).getValueAsText());
        if (jn.has(COL_USER_ID))
            setUserId(jn.path(COL_USER_ID).getValueAsLong());
        if (jn.has(COL_CONTENT_TYPE))
            setContentType(jn.path(COL_CONTENT_TYPE).getValueAsText());
        if (jn.has(COL_NEW_FILE_NAME))
            setNewFileName(jn.path(COL_NEW_FILE_NAME).getValueAsText());

    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_VIDEO_ID))
            jg.writeNumberField(COL_VIDEO_ID, getVideoId());
        if (outputColumn(cols, COL_CREATED_TIME))
            jg.writeNumberField(COL_CREATED_TIME, getCreatedTime());
        if (outputColumn(cols, COL_DESTROYED_TIME))
            jg.writeNumberField(COL_DESTROYED_TIME, getDestroyedTime());
        if (outputColumn(cols, COL_UPDATED_TIME))
            jg.writeNumberField(COL_UPDATED_TIME, getUpdatedTime());
        if (outputColumn(cols, COL_TITLE))
            jg.writeStringField(COL_TITLE, getTitle());
        if (outputColumn(cols, COL_SUMMARY))
            jg.writeStringField(COL_SUMMARY, getSummary());
        if (outputColumn(cols, COL_DESCRIPTION))
            jg.writeStringField(COL_DESCRIPTION, getDescription());
        if (outputColumn(cols, COL_LEVEL))
            jg.writeNumberField(COL_LEVEL, getLevel());
        if (outputColumn(cols, COL_FILE_SIZE))
            jg.writeNumberField(COL_FILE_SIZE, getFileSize());
        if (outputColumn(cols, COL_VIDEO_TIME_LENGTH))
            jg.writeNumberField(COL_VIDEO_TIME_LENGTH, getVideoTimeLength());
        if (outputColumn(cols, COL_VIDEO_WIDTH))
            jg.writeNumberField(COL_VIDEO_WIDTH, getVideoWidth());
        if (outputColumn(cols, COL_VIDEO_HEIGHT))
            jg.writeNumberField(COL_VIDEO_HEIGHT, getVideoHeight());
        if (outputColumn(cols, COL_VIDEO_DATA_RATE))
            jg.writeNumberField(COL_VIDEO_DATA_RATE, getVideoDataRate());
        if (outputColumn(cols, COL_VIDEO_BIT_RATE))
            jg.writeNumberField(COL_VIDEO_BIT_RATE, getVideoBitRate());
        if (outputColumn(cols, COL_VIDEO_FRAME_RATE))
            jg.writeNumberField(COL_VIDEO_FRAME_RATE, getVideoFrameRate());
        if (outputColumn(cols, COL_AUDIO_BIT_RATE))
            jg.writeNumberField(COL_AUDIO_BIT_RATE, getAudioBitRate());
        if (outputColumn(cols, COL_AUDIO_CHANNEL))
            jg.writeNumberField(COL_AUDIO_CHANNEL, getAudioChannel());
        if (outputColumn(cols, COL_AUDIO_SAMPLING_RATE))
            jg.writeNumberField(COL_AUDIO_SAMPLING_RATE, getAudioSamplingRate());
        if (outputColumn(cols, COL_THUMBNAIL))
            jg.writeStringField(COL_THUMBNAIL, getThumbnail());
        if (outputColumn(cols, COL_ARTISTS))
            jg.writeStringField(COL_ARTISTS, getArtists());
        if (outputColumn(cols, COL_CODING_TYPE))
            jg.writeStringField(COL_CODING_TYPE, getCodingType());
        if (outputColumn(cols, COL_COMPRESSION_TYPE))
            jg.writeStringField(COL_COMPRESSION_TYPE, getCompressionType());
        if (outputColumn(cols, COL_EXP_NAME))
            jg.writeStringField(COL_EXP_NAME, getExpName());
        if (outputColumn(cols, COL_HTML_URL))
            jg.writeStringField(COL_HTML_URL, getHtmlUrl());
        if (outputColumn(cols, COL_CONTENT_TYPE))
            jg.writeStringField(COL_CONTENT_TYPE, getContentType());
        if (outputColumn(cols, COL_NEW_FILE_NAME))
            jg.writeStringField(COL_NEW_FILE_NAME, getNewFileName());
        if (outputColumn(cols, COL_USER_ID))
            jg.writeNumberField(COL_USER_ID, getUserId());

        writeAddonsJson(jg, cols);
        jg.writeEndObject();
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException {
        serialize(jg, (String[]) null);
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }


    public static Video fromJsonNode(JsonNode jn) {
        Video video = new Video();
        video.deserialize(jn);
        return video;
    }

    public static Video fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    @Override
    public String toString() {
        return toJson(null, true);
        //return super.toString();
    }

}
