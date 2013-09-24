package com.borqs.server.platform.feature.staticfile.video;

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

public class StaticFile extends Addons implements JsonBean {

    public static final String COL_FILE_ID = "video_id";
    public static final String COL_TITLE = "title";
    public static final String COL_SUMMARY = "summary";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_FILE_SIZE = "file_size";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_EXP_NAME = "exp_name";
    public static final String COL_HTML_URL = "html_url";
    //public static final String COL_ARTISTS = "artists";
    public static final String COL_CONTENT_TYPE = "content_type";
    public static final String COL_NEW_FILE_NAME = "new_file_name";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_UPDATED_TIME = "updated_time";
    public static final String COL_DESTROYED_TIME = "destroyed_time";


    private long fileId;
    private String title;
    private String summary;
    private String description;
    private int level;
    private long fileSize;
    private String expName;
    private String htmlUrl;
    //private String artists;
    private long userId;
    private String contentType;
    private String newFileName;
    private long createdTime;
    private long destroyedTime;
    private long updatedTime;


    public StaticFile() {
        this(0L);
    }

    public StaticFile(long fileId) {
        this.fileId = fileId;
    }

    public static final String[] STANDARD_COLUMNS = {
            COL_FILE_ID ,
            COL_TITLE ,
            COL_SUMMARY ,
            COL_DESCRIPTION ,
            COL_FILE_SIZE ,
            COL_USER_ID ,
            COL_EXP_NAME ,
            COL_HTML_URL ,
            COL_CONTENT_TYPE ,
            COL_NEW_FILE_NAME ,
            COL_CREATED_TIME ,
            COL_UPDATED_TIME ,
            COL_DESTROYED_TIME
    };
    public static final String[] FULL_COLUMNS = {
            COL_FILE_ID ,
            COL_TITLE ,
            COL_SUMMARY ,
            COL_DESCRIPTION ,
            COL_FILE_SIZE ,
            COL_USER_ID ,
            COL_EXP_NAME ,
            COL_HTML_URL ,
            COL_CONTENT_TYPE ,
            COL_NEW_FILE_NAME ,
            COL_CREATED_TIME ,
            COL_UPDATED_TIME ,
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

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
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


    /*public String getArtists() {
        return artists;
    }

    public void setArtists(String artists) {
        this.artists = artists;
    }*/

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
        StaticFile.columnAliases = columnAliases;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        StaticFile other = (StaticFile) o;
        return fileId == other.fileId
                && createdTime == other.createdTime
                && destroyedTime == other.destroyedTime
                && StringUtils.equals(title, other.title)
                //&& StringUtils.equals(artists, other.artists)
                && destroyedTime == other.destroyedTime;
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(fileId, createdTime, destroyedTime,
                title, updatedTime, description);
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_FILE_ID))
            setFileId(jn.path(COL_FILE_ID).getValueAsLong());
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
        if (jn.has(COL_FILE_SIZE))
            setFileSize(jn.path(COL_FILE_SIZE).getValueAsInt());
        if (jn.has(COL_EXP_NAME))
            setExpName(jn.path(COL_EXP_NAME).getValueAsText());
        if (jn.has(COL_HTML_URL))
            setHtmlUrl(jn.path(COL_HTML_URL).getValueAsText());
        /*if (jn.has(COL_ARTISTS))
            setArtists(jn.path(COL_ARTISTS).getValueAsText());*/
        if (jn.has(COL_USER_ID))
            setUserId(jn.path(COL_USER_ID).getValueAsLong());
        if (jn.has(COL_CONTENT_TYPE))
            setContentType(jn.path(COL_CONTENT_TYPE).getValueAsText());
        if (jn.has(COL_NEW_FILE_NAME))
            setNewFileName(jn.path(COL_NEW_FILE_NAME).getValueAsText());

    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_FILE_ID))
            jg.writeNumberField(COL_FILE_ID, getFileId());
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
        if (outputColumn(cols, COL_FILE_SIZE))
            jg.writeNumberField(COL_FILE_SIZE, getFileSize());
        /*if (outputColumn(cols, COL_ARTISTS))
            jg.writeStringField(COL_ARTISTS, getArtists());*/
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


    public static StaticFile fromJsonNode(JsonNode jn) {
        StaticFile video = new StaticFile();
        video.deserialize(jn);
        return video;
    }

    public static StaticFile fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    @Override
    public String toString() {
        return toJson(null, true);
        //return super.toString();
    }

    public static String correctContentType(String expName, String oldContentType) {
            //video
            if (expName.equalsIgnoreCase("mp2"))
                oldContentType = "video/mpeg";
            if (expName.equalsIgnoreCase("mpa"))
                oldContentType = "video/mpeg";
            if (expName.equalsIgnoreCase("mpe"))
                oldContentType = "video/mpeg";
            if (expName.equalsIgnoreCase("mpeg"))
                oldContentType = "video/mpeg";
            if (expName.equalsIgnoreCase("mpg"))
                oldContentType = "video/mpeg";
            if (expName.equalsIgnoreCase("mpv2"))
                oldContentType = "video/mpeg";
            if (expName.equalsIgnoreCase("mov"))
                oldContentType = "video/quicktime";
            if (expName.equalsIgnoreCase("mov"))
                oldContentType = "video/quicktime";
            if (expName.equalsIgnoreCase("lsf"))
                oldContentType = "video/x-la-asf";
            if (expName.equalsIgnoreCase("lsx"))
                oldContentType = "video/x-la-asf";
            if (expName.equalsIgnoreCase("asf"))
                oldContentType = "video/x-ms-asf";
            if (expName.equalsIgnoreCase("asr"))
                oldContentType = "video/x-ms-asf";
            if (expName.equalsIgnoreCase("asx"))
                oldContentType = "video/x-ms-asf";

            if (expName.equalsIgnoreCase("avi"))
                oldContentType = "video/x-msvideo";
            if (expName.equalsIgnoreCase("movie"))
                oldContentType = "video/x-sgi-movie";
            if (expName.equalsIgnoreCase("wv"))
                oldContentType = "video/wavelet";
            if (expName.equalsIgnoreCase("wvx"))
                oldContentType = "video/x-ms-wvx";
            if (expName.equalsIgnoreCase("wmv"))
                oldContentType = "video/x-ms-wmv";
            if (expName.equalsIgnoreCase("wmx"))
                oldContentType = "video/x-ms-wmx";
            if (expName.equalsIgnoreCase("3gp"))
                oldContentType = "video/3gpp";
            if (expName.equalsIgnoreCase("asf"))
                oldContentType = "video/x-ms-asf";

            if (expName.equalsIgnoreCase("asx"))
                oldContentType = "video/x-ms-asf";
            if (expName.equalsIgnoreCase("avi"))
                oldContentType = "video/x-msvideo";
            if (expName.equalsIgnoreCase("fvi"))
                oldContentType = "video/isivideo";
            if (expName.equalsIgnoreCase("lsf"))
                oldContentType = "video/x-ms-asf";
            if (expName.equalsIgnoreCase("mng"))
                oldContentType = "video/x-mng";
            if (expName.equalsIgnoreCase("mp4"))
                oldContentType = "video/mp4";
            if (expName.equalsIgnoreCase("mpg4"))
                oldContentType = "video/mp4";
            if (expName.equalsIgnoreCase("pvx"))
                oldContentType = " video/x-pv-pvx";
            if (expName.equalsIgnoreCase("qt"))
                oldContentType = "video/quicktime";
            if (expName.equalsIgnoreCase("rv"))
                oldContentType = "video/vnd.rn-realvideo";

            if (expName.equalsIgnoreCase("vdo"))
                oldContentType = "video/vdo";
            if (expName.equalsIgnoreCase("viv"))
                oldContentType = "video/vivo";
            if (expName.equalsIgnoreCase("vivo"))
                oldContentType = "video/vivo";
            if (expName.equalsIgnoreCase("wm"))
                oldContentType = "video/x-ms-wm";
            if (expName.equalsIgnoreCase("wmv"))
                oldContentType = "video/x-ms-wmv";
            if (expName.equalsIgnoreCase("wmx"))
                oldContentType = "video/x-ms-wmx";

            if (expName.equalsIgnoreCase("flv"))
                oldContentType = "video/x-flv";
            if (expName.equalsIgnoreCase("f4v"))
               oldContentType = "video/x-f4v";
            if (expName.equalsIgnoreCase("rm"))
               oldContentType = "video/vnd.rn-realvideo";
            if (expName.equalsIgnoreCase("rmvb"))
               oldContentType = "video/vnd.rn-realvideo";

            //audio
            if (expName.equalsIgnoreCase("aif"))
               oldContentType = "audio/x-aiff";
            if (expName.equalsIgnoreCase("aifc"))
               oldContentType = "audio/x-aiff";
            if (expName.equalsIgnoreCase("aiff"))
               oldContentType = "audio/x-aiff";
            if (expName.equalsIgnoreCase("als"))
               oldContentType = "audio/X-Alpha5";
            if (expName.equalsIgnoreCase("au"))
               oldContentType = "audio/basic";
            if (expName.equalsIgnoreCase("awb"))
               oldContentType = "audio/amr-wb";
            if (expName.equalsIgnoreCase("es"))
               oldContentType = "audio/echospeech";
            if (expName.equalsIgnoreCase("esl"))
               oldContentType = "audio/echospeech";
            if (expName.equalsIgnoreCase("imy"))
               oldContentType = "audio/melody";
            if (expName.equalsIgnoreCase("it"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("itz"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("m15"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("m3u"))
               oldContentType = "audio/x-mpegurl";
            if (expName.equalsIgnoreCase("m3url"))
               oldContentType = "audio/x-mpegurl";
            if (expName.equalsIgnoreCase("ma1"))
               oldContentType = "audio/ma1";
            if (expName.equalsIgnoreCase("ma2"))
               oldContentType = "audio/ma2";
            if (expName.equalsIgnoreCase("ma3"))
               oldContentType = "audio/ma3";
            if (expName.equalsIgnoreCase("ma5"))
               oldContentType = "audio/ma5";
            if (expName.equalsIgnoreCase("mdz"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("mid"))
               oldContentType = "audio/midi";
            if (expName.equalsIgnoreCase("midi"))
               oldContentType = "audio/midi";
            if (expName.equalsIgnoreCase("mio"))
               oldContentType = "audio/x-mio";
            if (expName.equalsIgnoreCase("mod"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("mp2"))
               oldContentType = "audio/x-mpeg";
            if (expName.equalsIgnoreCase("mp3"))
               oldContentType = "audio/x-mpeg";
            if (expName.equalsIgnoreCase("mpga"))
               oldContentType = "audio/mpeg";
            if (expName.equalsIgnoreCase("nsnd"))
               oldContentType = "audio/nsnd";
            if (expName.equalsIgnoreCase("pac"))
               oldContentType = "audio/x-pac";
            if (expName.equalsIgnoreCase("pae"))
               oldContentType = "audio/x-epac";
            if (expName.equalsIgnoreCase("qcp"))
               oldContentType = "audio/vnd.qcelp";
            if (expName.equalsIgnoreCase("ra"))
               oldContentType = "audio/x-pn-realaudio";
            if (expName.equalsIgnoreCase("ram"))
               oldContentType = "audio/x-pn-realaudio";
            if (expName.equalsIgnoreCase("s3m"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("s3z"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("smd"))
               oldContentType = "audio/x-smd";
            if (expName.equalsIgnoreCase("smz"))
               oldContentType = "audio/x-smd";
            if (expName.equalsIgnoreCase("snd"))
               oldContentType = "audio/basic";
            if (expName.equalsIgnoreCase("stm"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("tsi"))
               oldContentType = "audio/tsplayer";
            if (expName.equalsIgnoreCase("ult"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("vib"))
               oldContentType = "audio/vib";
            if (expName.equalsIgnoreCase("wav"))
               oldContentType = "audio/x-wav";
            if (expName.equalsIgnoreCase("wax"))
               oldContentType = "audio/x-ms-wax";
            if (expName.equalsIgnoreCase("wma"))
               oldContentType = "audio/x-ms-wma";
            if (expName.equalsIgnoreCase("xm"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("xmz"))
               oldContentType = "audio/x-mod";
            if (expName.equalsIgnoreCase("rmi"))
               oldContentType = "audio/mid";
            return oldContentType;
        }

}
