package com.borqs.server.platform.feature.stream;

import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.CommentPostExpansion;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.like.LikePostExpansion;
import com.borqs.server.platform.io.RW;
import com.borqs.server.platform.io.Writable;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.GeoLocation;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Post extends Addons implements JsonBean, Copyable<Post>, Writable {

    public static final int POST_TEXT = 1;
    public static final int POST_PHOTO = 1 << 1;
    public static final int POST_VIDEO = 1 << 2;
    public static final int POST_AUDIO = 1 << 3;
    public static final int POST_BOOK = 1 << 4;
    public static final int POST_APK = 1 << 5;
    public static final int POST_LINK = 1 << 6;
    public static final int POST_APK_LINK = 1 << 7;
    public static final int POST_MUSIC = 1 << 8;
    public static final int POST_SIGN_IN = 1 << 9;
    public static final int POST_SYSTEM = 1 << 20;


    public static final int POST_LIKE_BROADCAST = 1 << 29;
    public static final int POST_COMMENT_BROADCAST = 1 << 30;


    public static final int ALL_POST_TYPES = POST_TEXT | POST_VIDEO
            | POST_AUDIO | POST_BOOK | POST_APK | POST_LINK | POST_SYSTEM | POST_SIGN_IN | POST_APK_LINK
            | POST_MUSIC | POST_PHOTO | POST_LIKE_BROADCAST | POST_COMMENT_BROADCAST;

    public static final String COL_POST_ID = "post_id";
    public static final String COL_SOURCE_ID = "source_id";
    public static final String COL_SOURCE = "source";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_UPDATED_TIME = "updated_time";
    public static final String COL_DESTROYED_TIME = "destroyed_time";
    public static final String COL_QUOTE = "quote";
    public static final String COL_QUOTED_POST = "quoted_post";
    public static final String COL_TO = "to";
    public static final String COL_TO_IDS = "to_ids";
    public static final String COL_ADD_TO = "add_to";
    public static final String COL_ADD_TO_IDS = "add_to_ids";
    public static final String COL_APP = "app";
    public static final String COL_TYPE = "type";
    public static final String COL_MESSAGE = "message";
    public static final String COL_APP_DATA = "app_data";
    public static final String COL_ATTACHMENTS = "attachments";
    public static final String COL_ATTACHMENT_IDS = "attachment_ids";
    public static final String COL_DEVICE = "device";
    public static final String COL_CAN_COMMENT = "can_comment";
    public static final String COL_CAN_LIKE = "can_like";
    public static final String COL_CAN_QUOTE = "can_quote";
    public static final String COL_PRIVATE = "private";
    public static final String COL_LOCATION = "location";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_TYPE_ICON_URL = "type_icon_url";

    public static final String COL_TITLE = "title";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_ICON_URL = "icon_url";
    public static final String COL_IMAGE_URLS = "image_urls";

    public static final String[] STANDARD_COLUMNS = {
            COL_POST_ID,
            COL_SOURCE,
            COL_SOURCE_ID,
            COL_CREATED_TIME,
            COL_UPDATED_TIME,
            COL_QUOTE,
            COL_QUOTED_POST,
            COL_TO,
            COL_TO_IDS,
            COL_ADD_TO,
            COL_ADD_TO_IDS,
            COL_APP,
            COL_TYPE,
            COL_MESSAGE,
            COL_APP_DATA,
            COL_ATTACHMENTS,
            COL_DEVICE,
            COL_CAN_COMMENT,
            COL_CAN_LIKE,
            COL_CAN_QUOTE,
            //COL_PRIVATE,
            COL_LOCATION,
            COL_LONGITUDE,
            COL_LATITUDE
    };


    public static final String[] FULL_COLUMNS = {
            COL_POST_ID,
            COL_SOURCE,
            COL_SOURCE_ID,
            COL_CREATED_TIME,
            COL_UPDATED_TIME,
            COL_QUOTE,
            COL_QUOTED_POST,
            COL_TO,
            COL_TO_IDS,
            COL_ADD_TO,
            COL_ADD_TO_IDS,
            COL_APP,
            COL_TYPE,
            COL_MESSAGE,
            COL_APP_DATA,
            COL_ATTACHMENTS,
            COL_DEVICE,
            COL_CAN_COMMENT,
            COL_CAN_LIKE,
            COL_CAN_QUOTE,
            //COL_PRIVATE,
            COL_LOCATION,
            COL_LONGITUDE,
            COL_LATITUDE,
    };

    public static final String[] DISPLAY_COLUMNS = {
            COL_POST_ID,
            COL_SOURCE,
            COL_SOURCE_ID,
            COL_CREATED_TIME,
            COL_UPDATED_TIME,
            COL_QUOTE,
            COL_QUOTED_POST,
            COL_TO,
            COL_TO_IDS,
            COL_ADD_TO,
            COL_ADD_TO_IDS,
            COL_APP,
            COL_TYPE,
            COL_MESSAGE,
            COL_APP_DATA,
            COL_ATTACHMENTS,
            COL_DEVICE,
            COL_CAN_COMMENT,
            COL_CAN_LIKE,
            COL_CAN_QUOTE,
            //COL_PRIVATE,
            COL_LOCATION,
            COL_LONGITUDE,
            COL_LATITUDE,
    };

    public static final String[] X_STANDARD_COLUMNS = ArrayHelper.merge(STANDARD_COLUMNS,
            CommentPostExpansion.EXPAND_COLUMNS, LikePostExpansion.EXPAND_COLUMNS);

    public static final String[] X_FULL_COLUMNS = ArrayHelper.merge(FULL_COLUMNS,
            CommentPostExpansion.EXPAND_COLUMNS, LikePostExpansion.EXPAND_COLUMNS);

    private static Map<String, String[]> columnAliases = new ConcurrentHashMap<String, String[]>();

    static {
        registerColumnsAlias("@std,#std", STANDARD_COLUMNS);
        registerColumnsAlias("@full,#full", FULL_COLUMNS);
        registerColumnsAlias("@xstd,#xstd", X_STANDARD_COLUMNS);
        registerColumnsAlias("@xfull,#xfull", X_FULL_COLUMNS);
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

    private long postId;
    private long sourceId;
    private long createdTime;
    private long updatedTime;
    private long destroyedTime;
    private long quote;
    private PeopleIds to;
    private PeopleIds addTo;
    private int app;
    private int type;
    private String message;
    private String appData;
    private String attachments;
    private String[] attachmentIds;
    private String device;
    private Boolean canComment;
    private Boolean canLike;
    private Boolean canQuote;
    private Boolean private_;
    private String location;
    private GeoLocation geoLocation;

    public Post() {
        this(0L);
    }

    public Post(long postId) {
        this.postId = postId;
    }

    public long getPostId() {
        return postId;
    }

    public Target getPostTarget() {
        return Target.of(Target.POST, postId);
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getSourceId() {
        return sourceId;
    }

    public PeopleId getSourcePeople() {
        return PeopleId.user(sourceId);
    }

    public void setSourceId(long sourceId) {
        this.sourceId = sourceId;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public long getDestroyedTime() {
        return destroyedTime;
    }

    public void setDestroyedTime(long destroyedTime) {
        this.destroyedTime = destroyedTime;
    }

    public long getQuote() {
        return quote;
    }

    public void setQuote(long quote) {
        this.quote = quote;
    }

    public PeopleIds getTo() {
        return to;
    }

    public void setTo(PeopleIds to) {
        this.to = to;
    }

    public PeopleIds getAddTo() {
        return addTo;
    }

    public void setAddTo(PeopleIds addTo) {
        this.addTo = addTo;
    }

    public int getApp() {
        return app;
    }

    public void setApp(int app) {
        this.app = app;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAppData() {
        return appData;
    }

    public void setAppData(String appData) {
        this.appData = appData;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public String[] getAttachmentIds() {
        return attachmentIds;
    }

    public Target[] getAttachmentTargetIds() {
        return attachmentIds == null ? null : Target.fromCompatibleStringArray(attachmentIds);
    }

    public void setAttachmentIds(String[] attachmentIds) {
        this.attachmentIds = attachmentIds;
    }

    public void setAttachmentId(String attachmentId) {
        setAttachmentIds(new String[] {attachmentId});
    }

    public void setAttachmentIds(long[] attachmentIds) {
        this.attachmentIds = ArrayHelper.longArrayToStringArray(attachmentIds);
    }

    public void setAttachmentId(long attachmentId) {
        setAttachmentIds(new String[] {Long.toString(attachmentId)});
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Boolean getCanComment() {
        return canComment;
    }

    public void setCanComment(Boolean canComment) {
        this.canComment = canComment;
    }

    public Boolean getCanLike() {
        return canLike;
    }

    public void setCanLike(Boolean canLike) {
        this.canLike = canLike;
    }

    public Boolean getCanQuote() {
        return canQuote;
    }

    public void setCanQuote(Boolean canQuote) {
        this.canQuote = canQuote;
    }

    public Boolean getPrivate() {
        return private_;
    }

    public void setPrivate(Boolean aPrivate) {
        private_ = aPrivate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public double getLatitude() {
        return geoLocation != null ? geoLocation.latitude : 0.0;
    }

    public double getLongitude() {
        return geoLocation != null ? geoLocation.longitude : 0.0;
    }

    public PeopleIds getToAndAddto() {
        PeopleIds friendIds = new PeopleIds();
        if (to != null)
            friendIds.addAll(to);
        if (addTo != null)
            friendIds.addAll(addTo);
        friendIds.unique();
        return friendIds;
    }

    @Override
    public Post copy() {
        Post post = new Post();
        post.setAddTo(addTo != null ? new PeopleIds(addTo) : null);
        post.setApp(app);
        post.setAppData(appData);
        post.setAttachments(attachments);
        post.setAttachmentIds(attachmentIds);
        post.setCanComment(canComment);
        post.setCanLike(canLike);
        post.setCanQuote(canQuote);
        post.setCreatedTime(createdTime);
        post.setDestroyedTime(destroyedTime);
        post.setDevice(device);
        post.setLocation(location);
        post.setGeoLocation(geoLocation != null ? geoLocation.copy() : null);
        post.setMessage(message);
        post.setPostId(postId);
        post.setPrivate(private_);
        post.setQuote(quote);
        post.setSourceId(sourceId);
        post.setTo(to != null ? new PeopleIds(to) : null);
        post.setType(type);
        post.setUpdatedTime(updatedTime);
        return post;
    }

    @Override
    public String toString() {
        return toJson(null, true);
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_POST_ID))
            setPostId(jn.path(COL_POST_ID).getValueAsLong());

        double longitude = 0.0, latitude = 0.0;
        if (jn.has(COL_LATITUDE))
            longitude = jn.path(COL_LATITUDE).getValueAsDouble(0.0);
        if (jn.has(COL_LONGITUDE))
            latitude = jn.path(COL_LONGITUDE).getValueAsDouble(0.0);

        setGeoLocation(new GeoLocation(longitude, latitude));

        if (jn.has(COL_LOCATION))
            setLocation(jn.path(COL_LOCATION).getTextValue());

        if (jn.has(COL_ADD_TO_IDS)) {
            String s = jn.path(COL_ADD_TO_IDS).getTextValue();
            setAddTo(s != null ? PeopleIds.parse(null, s) : null);
        }
        if (jn.has(COL_ADD_TO))
            setAddon(COL_ADD_TO, Addons.jsonAddonValue(JsonHelper.toJson(jn.path(COL_ADD_TO), true)));
        if (jn.has(COL_APP))
            setApp(jn.path(COL_APP).getValueAsInt());
        if (jn.has(COL_APP_DATA))
            setAppData(jn.path(COL_APP_DATA).getTextValue());
        if (jn.has(COL_ATTACHMENTS)) {
            JsonNode sub = jn.path(COL_ATTACHMENTS);
            if (sub.isArray() || sub.isObject()) {
                setAttachments(JsonHelper.toJson(sub, false));
            } else {
                setAttachments(sub.getValueAsText());
            }
        }
        if (jn.has(COL_ATTACHMENT_IDS)) {
            JsonNode sub = jn.path(COL_ATTACHMENT_IDS);
            String[] attachmentIds;
            if (sub.isTextual()) {
                attachmentIds = new String[] {sub.getTextValue()};
            } else { // array
                int len = sub.size();
                attachmentIds = new String[len];
                for (int i = 0; i < len; i++)
                    attachmentIds[i] = ObjectUtils.toString(sub.path(i).getTextValue());
            }
            setAttachmentIds(attachmentIds);
        }
        if (jn.has(COL_CAN_COMMENT))
            setCanComment(jn.path(COL_CAN_COMMENT).getValueAsBoolean());
        if (jn.has(COL_CAN_LIKE))
            setCanLike(jn.path(COL_CAN_LIKE).getValueAsBoolean());
        if (jn.has(COL_CAN_QUOTE))
            setCanQuote(jn.path(COL_CAN_QUOTE).getValueAsBoolean());
        if (jn.has(COL_CREATED_TIME))
            setCreatedTime(jn.path(COL_CREATED_TIME).getValueAsLong());
        if (jn.has(COL_DESTROYED_TIME))
            setDestroyedTime(jn.path(COL_DESTROYED_TIME).getValueAsLong());
        if (jn.has(COL_DEVICE))
            setDevice(jn.path(COL_DEVICE).getTextValue());
        if (jn.has(COL_PRIVATE))
            setPrivate(jn.path(COL_PRIVATE).getValueAsBoolean());
        if (jn.has(COL_MESSAGE))
            setMessage(jn.path(COL_MESSAGE).getTextValue());
        if (jn.has(COL_QUOTE))
            setQuote(jn.path(COL_QUOTE).getValueAsLong());
        if (jn.has(COL_SOURCE_ID))
            setSourceId(jn.path(COL_SOURCE_ID).getValueAsLong());
        if (jn.has(COL_SOURCE))
            setAddon(COL_SOURCE, Addons.jsonAddonValue(JsonHelper.toJson(jn.path(COL_SOURCE), true)));
        if (jn.has(COL_TO_IDS)) {
            String s = jn.path(COL_TO_IDS).getTextValue();
            setTo(s != null ? PeopleIds.parse(null, s) : null);
        }
        if (jn.has(COL_TO))
            setAddon(COL_TO, Addons.jsonAddonValue(JsonHelper.toJson(jn.path(COL_TO), true)));
        if (jn.has(COL_TYPE))
            setType(jn.path(COL_TYPE).getValueAsInt());
        if (jn.has(COL_UPDATED_TIME))
            setUpdatedTime(jn.path(COL_UPDATED_TIME).getValueAsLong());
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_POST_ID))
            jg.writeNumberField(COL_POST_ID, getPostId());
        if (outputColumn(cols, COL_LATITUDE))
            jg.writeNumberField(COL_LATITUDE, geoLocation != null ? geoLocation.latitude : 0.0);
        if (outputColumn(cols, COL_LONGITUDE))
            jg.writeNumberField(COL_LONGITUDE, geoLocation != null ? geoLocation.longitude : 0.0);
        if (outputColumn(cols, COL_LOCATION))
            jg.writeStringField(COL_LOCATION, ObjectUtils.toString(getLocation()));
        if (outputColumn(cols, COL_DESTROYED_TIME))
            jg.writeNumberField(COL_DESTROYED_TIME, getDestroyedTime());
        if (outputColumn(cols, COL_CREATED_TIME))
            jg.writeNumberField(COL_CREATED_TIME, getCreatedTime());

        if (outputColumn(cols, COL_DEVICE))
            jg.writeStringField(COL_DEVICE, ObjectUtils.toString(getDevice()));
        if (outputColumn(cols, COL_MESSAGE))
            jg.writeStringField(COL_MESSAGE, ObjectUtils.toString(getMessage()));
        if (outputColumn(cols, COL_ADD_TO)) {
            PeopleIds addTo = getAddTo();
            if (addTo == null)
                jg.writeNullField(COL_ADD_TO);
            else
                jg.writeStringField(COL_ADD_TO, addTo.toString());
        }
        if (outputColumn(cols, COL_ADD_TO_IDS)) {
            PeopleIds addTo = getAddTo();
            jg.writeStringField(COL_ADD_TO_IDS, CollectionUtils.isNotEmpty(addTo) ? addTo.toString() : "");
        }
        if (outputColumn(cols, COL_TO)) {
            PeopleIds to = getTo();
            if (to == null)
                jg.writeNullField(COL_TO);
            else
                jg.writeStringField(COL_TO, to.toString());
        }
        if (outputColumn(cols, COL_TO_IDS)) {
            PeopleIds to = getTo();
            jg.writeStringField(COL_TO_IDS, CollectionUtils.isNotEmpty(to) ? to.toString() : "");
        }
        if (outputColumn(cols, COL_APP))
            jg.writeNumberField(COL_APP, getApp());
        if (outputColumn(cols, COL_APP_DATA))
            jg.writeStringField(COL_APP_DATA, ObjectUtils.toString(getAppData()));
        if (outputColumn(cols, COL_ATTACHMENTS))
            serializeAttachment(jg, getAttachments());
        if (outputColumn(cols, COL_ATTACHMENT_IDS)) {
            jg.writeFieldName(COL_ATTACHMENT_IDS);
            jg.writeStartArray();
            if (ArrayUtils.isNotEmpty(attachmentIds)) {
                for (String attachmentId : attachmentIds)
                    jg.writeString(ObjectUtils.toString(attachmentId));
            }
            jg.writeEndArray();
        }
        if (outputColumn(cols, COL_CAN_COMMENT))
            jg.writeBooleanField(COL_CAN_COMMENT, BooleanUtils.isTrue(getCanComment()));
        if (outputColumn(cols, COL_CAN_LIKE))
            jg.writeBooleanField(COL_CAN_LIKE, BooleanUtils.isTrue(getCanLike()));
        if (outputColumn(cols, COL_CAN_QUOTE))
            jg.writeBooleanField(COL_CAN_QUOTE, BooleanUtils.isTrue(getCanQuote()));
        if (outputColumn(cols, COL_TYPE))
            jg.writeNumberField(COL_TYPE, getType());
        if (outputColumn(cols, COL_SOURCE_ID))
            jg.writeNumberField(COL_SOURCE_ID, getSourceId());

        if (outputColumn(cols, COL_QUOTE))
            jg.writeNumberField(COL_QUOTE, getQuote());
        if ((outputColumn(cols, COL_PRIVATE)) && getPrivate() != null)
            jg.writeBooleanField(COL_PRIVATE, getPrivate());
        if (outputColumn(cols, COL_UPDATED_TIME))
            jg.writeNumberField(COL_UPDATED_TIME, getUpdatedTime());


        writeAddonsJson(jg, cols);
        jg.writeEndObject();
    }

    private static void serializeAttachment(JsonGenerator jg, String attachment) throws IOException {
        jg.writeFieldName(COL_ATTACHMENTS);
        if (StringUtils.isBlank(attachment)) {
            jg.writeRawValue("[]");
        } else {
            JsonNode jn = null;
            try {
                jn = JsonHelper.parse(attachment);
            } catch (Exception ignored) {
            }
            if (jn != null) {
                if (!jn.isArray()) {
                    ArrayNode arr = JsonNodeFactory.instance.arrayNode();
                    arr.add(jn);
                    jn = arr;
                }
                jg.writeTree(jn);
            } else {
                jg.writeRawValue("[]");
            }
        }
    }

    public static Post fromJsonNode(JsonNode jn) {
        Post post = new Post();
        post.deserialize(jn);
        return post;
    }

    public static Post fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
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
    public void write(Encoder out, boolean flush) throws IOException {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put(COL_POST_ID, postId);
        m.put(COL_SOURCE_ID, sourceId);
        m.put(COL_CREATED_TIME, createdTime);
        m.put(COL_UPDATED_TIME, updatedTime);
        m.put(COL_DESTROYED_TIME, destroyedTime);
        m.put(COL_QUOTE, quote);
        m.put(COL_TO, to == null? null : to.toString());
        m.put(COL_ADD_TO, addTo == null? null: addTo.toString());
        m.put(COL_APP, app);
        m.put(COL_TYPE, type);
        m.put(COL_MESSAGE, message);
        m.put(COL_APP_DATA, appData);
        m.put(COL_ATTACHMENTS, attachments);
        m.put(COL_ATTACHMENT_IDS, attachmentIds);
        m.put(COL_DEVICE, device);
        m.put(COL_CAN_LIKE, canLike);
        m.put(COL_CAN_COMMENT, canComment);
        m.put(COL_CAN_QUOTE, canQuote);
        m.put(COL_PRIVATE, private_);
        m.put(COL_LOCATION, location);
        m.put(COL_LATITUDE, geoLocation != null ? geoLocation.latitude : null);
        m.put(COL_LONGITUDE, geoLocation != null ? geoLocation.longitude : null);
        RW.write(out, m, flush);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readIn(Decoder in) throws IOException {
        Map<String, Object> m = (Map<String, Object>) RW.read(in);
        postId = (Long) m.get(COL_POST_ID);
        sourceId = (Long) m.get(COL_SOURCE_ID);
        createdTime = (Long) m.get(COL_CREATED_TIME);
        updatedTime = (Long) m.get(COL_UPDATED_TIME);
        destroyedTime = (Long) m.get(COL_DESTROYED_TIME);
        quote = (Long) m.get(COL_QUOTE);
        to = PeopleIds.parse(null, (String) m.get(COL_TO));
        addTo = PeopleIds.parse(null, (String) m.get(COL_ADD_TO));
        app = (Integer) m.get(COL_APP);
        type = (Integer) m.get(COL_TYPE);
        message = (String) m.get(COL_MESSAGE);
        attachments = (String) m.get(COL_ATTACHMENTS);
        attachmentIds = (String[]) m.get(COL_ATTACHMENT_IDS);
        appData = (String) m.get(COL_APP_DATA);
        device = (String) m.get(COL_DEVICE);
        canLike = (Boolean) m.get(COL_CAN_LIKE);
        canComment = (Boolean) m.get(COL_CAN_COMMENT);
        canQuote = (Boolean) m.get(COL_CAN_QUOTE);
        private_ = (Boolean) m.get(COL_PRIVATE);
        location = (String) m.get(COL_LOCATION);
        Double latitude = (Double) m.get(COL_LATITUDE);
        Double longitude = (Double) m.get(COL_LONGITUDE);
        if (latitude != null || longitude != null)
            setGeoLocation(new GeoLocation(longitude != null ? longitude : 0.0, latitude != null ? latitude : 0.0));
        else
            setGeoLocation(null);
    }



    public static String makeRepostMessage(String message, Post quotePost) {
        return message;
    }
}
