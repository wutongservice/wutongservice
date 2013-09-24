package com.borqs.server.platform.feature.audio;

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

public class Audio extends Addons implements JsonBean {

    public static final String COL_AUDIO_ID = "audio_id";
    public static final String COL_TITLE = "title";
    public static final String COL_SUMMARY = "summary";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_LEVEL = "level";
    public static final String COL_SCHOOLS = "schools";
    public static final String COL_FILE_SIZE = "file_size";
    public static final String COL_AUDIO_TIME_LENGTH = "audio_time_length";
    public static final String COL_AUDIO_BIT_RATE = "audio_bit_rate";
    public static final String COL_AUTHOR = "author";
    public static final String COL_AUDIO_ARTISTS = "audio_artists";
    public static final String COL_RECORD = "record";
    public static final String COL_RECORD_AUTHOR = "record_author";
    public static final String COL_RECORD_YEAR = "record_year";
    public static final String COL_CODING_TYPE = "coding_type";
    public static final String COL_COMPRESSION_TYPE = "compression_type";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_EXP_NAME = "exp_name";
    public static final String COL_HTML_URL = "html_url";
    public static final String COL_CONTENT_TYPE = "content_type";
    public static final String COL_NEW_FILE_NAME = "new_file_name";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_UPDATED_TIME = "updated_time";
    public static final String COL_DESTROYED_TIME = "destroyed_time";

    private long audioId;
    private String title;
    private long createdTime;
    private long destroyedTime;
    private long updatedTime;
    private String summary;
    private String description;
    private int level;
    private int schools;
    private long fileSize;
    private int audioTimeLength;
    private int audioBitRate;
    private String author;
    private String audioArtists;
    private String record;
    private String recordAuthor;
    private String recordYear;
    private String codingType;
    private String compressionType;
    private String expName;
    private String htmlUrl;
    private long userId;
    private String contentType;
    private String newFileName;

    public Audio() {
        this(0L);
    }

    public Audio(long audioId) {
        this.audioId = audioId;
    }

    public static final String[] STANDARD_COLUMNS = {
            COL_AUDIO_ID,
            COL_TITLE,
            COL_SUMMARY,
            COL_DESCRIPTION,
            COL_LEVEL,
            COL_SCHOOLS,
            COL_FILE_SIZE,
            COL_AUDIO_TIME_LENGTH,
            COL_AUDIO_BIT_RATE,
            COL_AUTHOR,
            COL_AUDIO_ARTISTS,
            COL_RECORD,
            COL_RECORD_AUTHOR,
            COL_RECORD_YEAR,
            COL_CODING_TYPE,
            COL_COMPRESSION_TYPE,
            COL_USER_ID,
            COL_EXP_NAME,
            COL_HTML_URL,
            COL_CONTENT_TYPE,
            COL_NEW_FILE_NAME,
            COL_CREATED_TIME,
            COL_UPDATED_TIME,
            COL_DESTROYED_TIME
    };
    public static final String[] FULL_COLUMNS = {
            COL_AUDIO_ID,
            COL_TITLE,
            COL_SUMMARY,
            COL_DESCRIPTION,
            COL_LEVEL,
            COL_SCHOOLS,
            COL_FILE_SIZE,
            COL_AUDIO_TIME_LENGTH,
            COL_AUDIO_BIT_RATE,
            COL_AUTHOR,
            COL_AUDIO_ARTISTS,
            COL_RECORD,
            COL_RECORD_AUTHOR,
            COL_RECORD_YEAR,
            COL_CODING_TYPE,
            COL_COMPRESSION_TYPE,
            COL_USER_ID,
            COL_EXP_NAME,
            COL_HTML_URL,
            COL_CONTENT_TYPE,
            COL_NEW_FILE_NAME,
            COL_CREATED_TIME,
            COL_UPDATED_TIME,
            COL_DESTROYED_TIME

    };


    private static Map<String, String[]> columnAliases = new ConcurrentHashMap<String, String[]>();

    static {
        registerColumnsAlias("@std,#std", STANDARD_COLUMNS);
        registerColumnsAlias("@full,#full", FULL_COLUMNS);
    }

    public static String[] expandColumns(String[] cols) {
        return ColumnsExpander.expand(cols, columnAliases);
    }

    public static void registerColumnsAlias(String alias, String[] cols) {
        columnAliases.put(alias, cols);
    }

    public static void unregisterColumnsAlias(String alias) {
        columnAliases.remove(alias);
    }

    public long getAudioId() {
        return audioId;
    }

    public void setAudioId(long audioId) {
        this.audioId = audioId;
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

    public int getSchools() {
        return schools;
    }

    public void setSchools(int schools) {
        this.schools = schools;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getAudioTimeLength() {
        return audioTimeLength;
    }

    public void setAudioTimeLength(int audioTimeLength) {
        this.audioTimeLength = audioTimeLength;
    }

    public int getAudioBitRate() {
        return audioBitRate;
    }

    public void setAudioBitRate(int audioBitRate) {
        this.audioBitRate = audioBitRate;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAudioArtists() {
        return audioArtists;
    }

    public void setAudioArtists(String audioArtists) {
        this.audioArtists = audioArtists;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    public String getRecordAuthor() {
        return recordAuthor;
    }

    public void setRecordAuthor(String recordAuthor) {
        this.recordAuthor = recordAuthor;
    }

    public String getRecordYear() {
        return recordYear;
    }

    public void setRecordYear(String recordYear) {
        this.recordYear = recordYear;
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
        Audio.columnAliases = columnAliases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Audio other = (Audio) o;
        return audioId == other.audioId
                && createdTime == other.createdTime
                && destroyedTime == other.destroyedTime
                && StringUtils.equals(title, other.title)
                && StringUtils.equals(audioArtists, other.audioArtists)
                && audioTimeLength == other.audioTimeLength
                && destroyedTime == other.destroyedTime;
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(audioId, createdTime, destroyedTime,
                title, audioArtists, audioBitRate, updatedTime, description);
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_AUDIO_ID))
            setAudioId(jn.path(COL_AUDIO_ID).getValueAsLong());
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
        if (jn.has(COL_SCHOOLS))
            setSchools(jn.path(COL_SCHOOLS).getValueAsInt());
        if (jn.has(COL_FILE_SIZE))
            setFileSize(jn.path(COL_FILE_SIZE).getValueAsInt());
        if (jn.has(COL_AUDIO_TIME_LENGTH))
            setAudioTimeLength(jn.path(COL_AUDIO_TIME_LENGTH).getValueAsInt());
        if (jn.has(COL_AUDIO_BIT_RATE))
            setAudioBitRate(jn.path(COL_AUDIO_BIT_RATE).getValueAsInt());
        if (jn.has(COL_AUTHOR))
            setAuthor(jn.path(COL_AUTHOR).getValueAsText());
        if (jn.has(COL_AUDIO_ARTISTS))
            setAudioArtists(jn.path(COL_AUDIO_ARTISTS).getValueAsText());
        if (jn.has(COL_RECORD))
            setRecord(jn.path(COL_RECORD).getValueAsText());
        if (jn.has(COL_RECORD_AUTHOR))
            setRecordAuthor(jn.path(COL_RECORD_AUTHOR).getValueAsText());
        if (jn.has(COL_RECORD_YEAR))
            setRecordYear(jn.path(COL_RECORD_YEAR).getValueAsText());
        if (jn.has(COL_CODING_TYPE))
            setCodingType(jn.path(COL_CODING_TYPE).getValueAsText());
        if (jn.has(COL_RECORD))
            setRecord(jn.path(COL_RECORD).getValueAsText());
        if (jn.has(COL_COMPRESSION_TYPE))
            setCompressionType(jn.path(COL_COMPRESSION_TYPE).getValueAsText());
        if (jn.has(COL_EXP_NAME))
            setExpName(jn.path(COL_EXP_NAME).getValueAsText());
        if (jn.has(COL_HTML_URL))
            setHtmlUrl(jn.path(COL_HTML_URL).getValueAsText());
        if (jn.has(COL_USER_ID))
            setUserId(jn.path(COL_USER_ID).getValueAsLong());
        if (jn.has(COL_CONTENT_TYPE))
            setContentType(jn.path(COL_CONTENT_TYPE).getValueAsText());
        if (jn.has(COL_NEW_FILE_NAME))
            setNewFileName(jn.path(COL_NEW_FILE_NAME).getValueAsText());


    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_AUDIO_ID))
            jg.writeNumberField(COL_AUDIO_ID, getAudioId());
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
        if (outputColumn(cols, COL_AUDIO_BIT_RATE))
            jg.writeNumberField(COL_AUDIO_BIT_RATE, getAudioBitRate());
        if (outputColumn(cols, COL_SCHOOLS))
            jg.writeNumberField(COL_SCHOOLS, getSchools());
        if (outputColumn(cols, COL_FILE_SIZE))
            jg.writeNumberField(COL_FILE_SIZE, getFileSize());
        if (outputColumn(cols, COL_AUDIO_TIME_LENGTH))
            jg.writeNumberField(COL_AUDIO_TIME_LENGTH, getAudioTimeLength());
        if (outputColumn(cols, COL_AUTHOR))
            jg.writeStringField(COL_AUTHOR, getAuthor());
        if (outputColumn(cols, COL_AUDIO_ARTISTS))
            jg.writeStringField(COL_AUDIO_ARTISTS, getAudioArtists());
        if (outputColumn(cols, COL_RECORD))
            jg.writeStringField(COL_RECORD, getRecord());
        if (outputColumn(cols, COL_RECORD_AUTHOR))
            jg.writeStringField(COL_RECORD_AUTHOR, getRecordAuthor());
        if (outputColumn(cols, COL_RECORD_YEAR))
            jg.writeStringField(COL_RECORD_YEAR, getRecordYear());
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


    public static Audio fromJsonNode(JsonNode jn) {
        Audio comment = new Audio();
        comment.deserialize(jn);
        return comment;
    }

    public static Audio fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    @Override
    public String toString() {
        return toJson(null, true);
        //return super.toString();
    }

}
