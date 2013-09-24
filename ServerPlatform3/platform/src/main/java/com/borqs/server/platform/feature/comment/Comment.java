package com.borqs.server.platform.feature.comment;

import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.io.RW;
import com.borqs.server.platform.io.Writable;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Comment extends Addons implements JsonBean, Copyable<Comment>, Writable {

    public static final String COL_COMMENT_ID = "comment_id";
    public static final String COL_TARGET = "target";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_DESTROYED_TIME = "destroyed_time";
    public static final String COL_COMMENTER = "commenter";
    public static final String COL_COMMENTER_ID = "commenter_id";
    public static final String COL_MESSAGE = "message";
    public static final String COL_DEVICE = "device";
    public static final String COL_CAN_LIKE = "can_like";
    public static final String COL_ADD_TO = "add_to";

    private long commentId;
    private Target target;
    private long createdTime;
    private long destroyedTime;
    private long commenterId;
    private String message = "";
    private String device = "";
    private boolean canLike = true;
    private PeopleIds addTo;

    public Comment() {
        this(0L);
    }

    public Comment(long commentId) {
        this.commentId = commentId;
    }

    public static final String[] STANDARD_COLUMNS = {
            COL_COMMENT_ID,
            COL_TARGET,
            COL_CREATED_TIME,
            COL_DESTROYED_TIME,
            COL_COMMENTER,
            COL_COMMENTER_ID,
            COL_MESSAGE,
            COL_DEVICE,
            COL_CAN_LIKE,
            COL_ADD_TO
    };
    public static final String[] FULL_COLUMNS = {
            COL_COMMENT_ID,
            COL_TARGET,
            COL_CREATED_TIME,
            COL_DESTROYED_TIME,
            COL_COMMENTER,
            COL_COMMENTER_ID,
            COL_MESSAGE,
            COL_DEVICE,
            COL_CAN_LIKE,
            COL_ADD_TO,
            
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

    public Comment(long commentId, Target target, long createdTime, long destroyedTime, long commenterId, String message, String device, boolean canLike, PeopleIds addTo) {
        this.commentId = commentId;
        this.target = target;
        this.createdTime = createdTime;
        this.destroyedTime = destroyedTime;
        this.commenterId = commenterId;
        this.message = message;
        this.device = device;
        this.canLike = canLike;
        this.addTo = addTo;
    }

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    public Target getCommentTarget() {
        return Target.of(Target.COMMENT, commentId);
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
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

    public long getCommenterId() {
        return commenterId;
    }

    public void setCommenterId(long commenterId) {
        this.commenterId = commenterId;
    }
     public PeopleId getCommenterPeople() {
        return PeopleId.user(commenterId);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean getCanLike() {
        return canLike;
    }

    public void setCanLike(boolean canLike) {
        this.canLike = canLike;
    }

    public void setProperty(String col, Object value) {
        Validate.notEmpty(col);
        Target target = (Target) value;
        setTarget(target);
    }

    public PeopleIds getAddTo() {
        return addTo;
    }

    public void setAddTo(PeopleIds addTo) {
        this.addTo = addTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Comment other = (Comment) o;
        return commentId == other.commentId
                && ObjectUtils.equals(target, other.target)
                && createdTime == other.createdTime
                && destroyedTime == other.destroyedTime
                && StringUtils.equals(device, other.device)
                && StringUtils.equals(message, other.message)
                && canLike == other.canLike
                && addTo == other.addTo;
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(commentId, target, createdTime, destroyedTime,
                message, device, commenterId, canLike, addTo);
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_COMMENT_ID))
            setCommentId(jn.path(COL_COMMENT_ID).getValueAsLong());
        if (jn.has(COL_CREATED_TIME))
            setCreatedTime(jn.path(COL_CREATED_TIME).getValueAsLong());
        if (jn.has(COL_DESTROYED_TIME))
            setDestroyedTime(jn.path(COL_DESTROYED_TIME).getValueAsLong());
        if (jn.has(COL_COMMENTER)) {
            setAddon(COL_COMMENTER, Addons.jsonAddonValue(JsonHelper.toJson(jn.path(COL_COMMENTER), true)));
        }
        if (jn.has(COL_COMMENTER_ID))
            setCommenterId(jn.path(COL_COMMENTER_ID).getValueAsLong());
        if (jn.has(COL_TARGET)) {
            Target target = Target.parseCompatibleString(jn.path(COL_TARGET).getTextValue());
            setProperty(COL_TARGET, target);
        }
        if (jn.has(COL_CAN_LIKE))
            setCanLike(jn.path(COL_CAN_LIKE).getValueAsBoolean());
        if (jn.has(COL_DEVICE))
            setDevice(jn.path(COL_DEVICE).getValueAsText());
        if (jn.has(COL_MESSAGE))
            setMessage(jn.path(COL_MESSAGE).getValueAsText());
        if (jn.has(COL_ADD_TO)) {
            String s = jn.path(COL_ADD_TO).getTextValue();
            setAddTo(s != null ? PeopleIds.parse(null, s) : null);
        }


    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_COMMENT_ID))
            jg.writeNumberField(COL_COMMENT_ID, getCommentId());
        if (outputColumn(cols, COL_CAN_LIKE))
            jg.writeBooleanField(COL_CAN_LIKE, getCanLike());
        if (outputColumn(cols, COL_COMMENTER_ID))
            jg.writeNumberField(COL_COMMENTER_ID, getCommenterId());
        if (outputColumn(cols, COL_DESTROYED_TIME))
            jg.writeNumberField(COL_DESTROYED_TIME, getDestroyedTime());
        if (outputColumn(cols, COL_CREATED_TIME))
            jg.writeNumberField(COL_CREATED_TIME, getCreatedTime());

        if (outputColumn(cols, COL_DEVICE))
            jg.writeStringField(COL_DEVICE, getDevice());
        if (outputColumn(cols, COL_MESSAGE))
            jg.writeStringField(COL_MESSAGE, getMessage());
        if (outputColumn(cols, COL_TARGET))
            jg.writeObjectField(COL_TARGET, getTarget().toCompatibleString());

        if (outputColumn(cols, COL_ADD_TO)) {
            PeopleIds addTo = getAddTo();
            if (addTo == null)
                jg.writeNullField(COL_ADD_TO);
            else
                jg.writeStringField(COL_ADD_TO, addTo.toString());
        }

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


    public static Comment fromJsonNode(JsonNode jn) {
        Comment comment = new Comment();
        comment.deserialize(jn);
        return comment;
    }

    public static Comment fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    @Override
    public String toString() {
        return toJson(null, true);
        //return super.toString();
    }

    @Override
    public Comment copy() {
        Comment comment = new Comment();
        comment.setCanLike(canLike);
        comment.setCommenterId(commenterId);
        comment.setCommentId(commentId);
        comment.setCreatedTime(createdTime);
        comment.setDestroyedTime(destroyedTime);
        comment.setDevice(device);
        comment.setMessage(message);
        comment.setTarget(target);
        comment.setAddTo(addTo);
        return comment;
    }

    @Override
    public void write(Encoder out, boolean flush) throws IOException {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put(COL_COMMENT_ID, commentId);
        m.put(COL_TARGET, target.toCompatibleString());
        m.put(COL_CREATED_TIME, createdTime);
        m.put(COL_DESTROYED_TIME, destroyedTime);
        m.put(COL_COMMENTER, commenterId);
        m.put(COL_MESSAGE, message);
        m.put(COL_DEVICE, device);
        m.put(COL_CAN_LIKE, canLike);
        m.put(COL_ADD_TO, ObjectUtils.toString(addTo, ""));
        RW.write(out, m, flush);
    }

    @Override
    public void readIn(Decoder in) throws IOException {
        Map<String, Object> m = (Map<String, Object>) RW.read(in);
        commentId = (Long) m.get(COL_COMMENT_ID);
        target = Target.parseCompatibleString((String) m.get(COL_TARGET));
        createdTime = (Long) m.get(COL_CREATED_TIME);
        destroyedTime = (Long) m.get(COL_DESTROYED_TIME);
        commenterId = (Long) m.get(COL_COMMENTER);
        message = (String) m.get(COL_MESSAGE);
        device = (String) m.get(COL_DEVICE);
        canLike = (Boolean) m.get(COL_CAN_LIKE);
        String addToStr = (String) m.get(COL_ADD_TO);
        addTo = StringUtils.isBlank(addToStr) ? null : PeopleIds.parse(null, addToStr);
    }
}
