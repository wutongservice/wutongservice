package com.borqs.server.platform.feature.link;


import com.borqs.server.platform.io.RW;
import com.borqs.server.platform.io.Writable;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.URLHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class LinkEntity implements JsonBean, Copyable<LinkEntity>,Writable {

    public static final String COL_URL = "url";
    public static final String COL_TITLE = "title";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_ICON_URL = "icon_url";
    public static final String COL_IMAGE_URLS = "image_urls";
    public static final String COL_HOST = "host";

    private String url;
    private String title;
    private String description;
    private String iconUrl;
    private String[] imageUrls;

    public LinkEntity() {
        this(null, "", "", null);
    }

    public LinkEntity(String url, String title, String description, String iconUrl) {
        this(url, title, description, null, null);
    }

    public LinkEntity(String url, String title, String description, String iconUrl, String[] imageUrls) {
        setUrl(url);
        setTitle(title);
        setDescription(description);
        setIconUrl(iconUrl);
        setImageUrls(imageUrls);
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = ObjectUtils.toString(url);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = ObjectUtils.toString(title);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = ObjectUtils.toString(description);
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = ObjectUtils.toString(iconUrl);
    }

    public String[] getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String[] imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new String[0];
    }

    public String getImageUrl() {
        return ArrayUtils.isEmpty(imageUrls) ? "" : ObjectUtils.toString(imageUrls[0]);
    }

    @Override
    public void deserialize(JsonNode jn) {
        if (jn.has(COL_URL))
            setUrl(jn.path(COL_URL).getTextValue());
        if (jn.has(COL_TITLE))
            setTitle(jn.path(COL_TITLE).getTextValue());
        if (jn.has(COL_DESCRIPTION))
            setDescription(jn.path(COL_DESCRIPTION).getTextValue());
        if (jn.has(COL_ICON_URL))
            setIconUrl(jn.path(COL_ICON_URL).getTextValue());
        if (jn.has(COL_IMAGE_URLS)) {
            JsonNode sub = jn.path(COL_IMAGE_URLS);
            ArrayList<String> imageUrls = new ArrayList<String>();
            for (int i = 0; i < sub.size(); i++) {
                imageUrls.add(sub.path(i).getTextValue());
            }
            setImageUrls(imageUrls.toArray(new String[imageUrls.size()]));
        }
    }

    public static LinkEntity fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    public static LinkEntity fromJsonNode(JsonNode jn) {
        LinkEntity le = new LinkEntity();
        le.deserialize(jn);
        return le;
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg);
    }

    public void serialize(JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField(COL_URL, getUrl());
        jg.writeStringField(COL_HOST, URLHelper.getHost(getUrl()));
        jg.writeStringField(COL_TITLE, getTitle());
        jg.writeStringField(COL_DESCRIPTION, getDescription());
        jg.writeStringField(COL_ICON_URL, getIconUrl());
        jg.writeFieldName(COL_IMAGE_URLS);
        jg.writeStartArray();
        for (String imageUrl : getImageUrls()) {
            jg.writeString(ObjectUtils.toString(imageUrl));
        }
        jg.writeEndArray();
        jg.writeEndObject();
    }

    public String toJson(boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg);
            }
        }, human);
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    @Override
    public LinkEntity copy() {
        return new LinkEntity(url, title, description, iconUrl, imageUrls);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LinkEntity other = (LinkEntity) o;
        return StringUtils.equals(url, other.url)
                || StringUtils.equals(title, other.title)
                || StringUtils.equals(description, other.description)
                || StringUtils.equals(iconUrl, other.iconUrl)
                || Arrays.equals(imageUrls, other.imageUrls);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(url, title, description, iconUrl, imageUrls);
    }

     @Override
    public void write(Encoder out, boolean flush) throws IOException {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put(COL_DESCRIPTION, description);
        m.put(COL_HOST, URLHelper.getHost(getUrl()));
        m.put(COL_ICON_URL,iconUrl);
        m.put(COL_IMAGE_URLS,imageUrls);
        m.put(COL_TITLE,title);
        m.put(COL_URL,url);
        RW.write(out, m, flush);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readIn(Decoder in) throws IOException {
        Map<String, Object> m = (Map<String, Object>) RW.read(in);
        description = (String) m.get(COL_DESCRIPTION);
        iconUrl = (String) m.get(COL_ICON_URL);
        imageUrls = (String[]) m.get(COL_IMAGE_URLS);
        title = (String) m.get(COL_TITLE);
        url = (String) m.get(COL_URL);
    }
}
