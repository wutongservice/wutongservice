package com.borqs.server.platform.feature.request;

import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public class Request extends Addons implements JsonSerializableWithType {

    public static final int STATUS_ANY = 0; // for query
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_DONE = 2;
    public static final int TYPE_ANY = 0; // for query

    public static final String COL_REQUEST_ID = "request_id";
    public static final String COL_FROM = "from";
    public static final String COL_SOURCE = "source";
    public static final String COL_TO = "to";
    public static final String COL_APP = "app";
    public static final String COL_TYPE = "type";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_DONE_TIME = "done_time";
    public static final String COL_STATUS = "status";
    public static final String COL_MESSAGE = "message";
    public static final String COL_DATA = "data";

    public static final String[] FULL_COLUMNS = {
            COL_REQUEST_ID,
            COL_FROM,
            COL_SOURCE,
            COL_TO,
            COL_APP,
            COL_TYPE,
            COL_CREATED_TIME,
            COL_DONE_TIME,
            COL_STATUS,
            COL_MESSAGE,
            COL_DATA,
    };

    private long requestId;
    private long from;
    private long to;
    private int app;
    private int type;
    private long createdTime;
    private long doneTime;
    private int status;
    private String message;
    private String data;


    public Request() {
    }

    public Request(long requestId, long from, long to, int app, int type,
                   long createdTime, long doneTime, int status, String message, String data) {
        this.requestId = requestId;
        this.from = from;
        this.to = to;
        this.app = app;
        this.type = type;
        this.createdTime = createdTime;
        this.doneTime = doneTime;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static Request newRandom(long from, long to, int app, int type, String message, String data) {
        long now = DateHelper.nowMillis();
        return new Request(RandomHelper.generateId(now),
                from, to, app, type,
                now, 0L, STATUS_PENDING, message, data);
    }


    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
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

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getDoneTime() {
        return doneTime;
    }

    public void setDoneTime(long doneTime) {
        this.doneTime = doneTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Request other = (Request) o;
        return from == other.from && to == other.to
                && app == other.app && type == other.type;
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(requestId, from, to, app, type, createdTime, doneTime, status, message, data);
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[])null);
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartObject();
        if (outputColumn(cols, COL_REQUEST_ID))
            jg.writeNumberField(COL_REQUEST_ID, requestId);

        if (outputColumn(cols, COL_FROM))
            jg.writeNumberField(COL_FROM, from);

        if (outputColumn(cols, COL_TO))
            jg.writeNumberField(COL_TO, to);

        if (outputColumn(cols, COL_APP))
            jg.writeNumberField(COL_APP, app);

        if (outputColumn(cols, COL_TYPE))
            jg.writeNumberField(COL_TYPE, type);

        if (outputColumn(cols, COL_CREATED_TIME))
            jg.writeNumberField(COL_CREATED_TIME, createdTime);

        if (outputColumn(cols, COL_DONE_TIME))
            jg.writeNumberField(COL_DONE_TIME, doneTime);

        if (outputColumn(cols, COL_STATUS))
            jg.writeNumberField(COL_STATUS, status);

        if (outputColumn(cols, COL_MESSAGE))
            jg.writeStringField(COL_MESSAGE, ObjectUtils.toString(message));

        if (outputColumn(cols, COL_DATA)) {
            jg.writeFieldName(COL_DATA);
            if (StringUtils.isEmpty(data)) {
                jg.writeString("");
            } else {
                if (JsonHelper.isJson(data))
                    jg.writeRawValue(data);
                else
                    jg.writeString(data);
            }
        }

        writeAddonsJson(jg, cols);

        jg.writeEndObject();
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
    public String toString() {
        return toJson(null, true);
    }
}
