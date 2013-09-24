package com.borqs.server.platform.web;


import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractHttpClient {
    protected String host;
    protected Map<String, String> headers;

    protected AbstractHttpClient() {
    }

    protected AbstractHttpClient(String host) {
        this.host = host;
    }

    protected AbstractHttpClient(String host, Map<String, String> headers) {
        this.host = host;
        this.headers = headers;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String urlInvoke(String path) {
        if (StringUtils.isNotEmpty(path))
            return StringUtils.removeEnd(host, "/") + "/" + StringUtils.removeStart(path, "/");
        else
            return StringUtils.removeEnd(host, "/");
    }

    public String urlGet(String path, Map<String, Object> params) {
        if (MapUtils.isNotEmpty(params))
            return urlInvoke(path) + "?" + encodeHttpParams(params);
        else
            return urlInvoke(path);
    }

    public String encodeHttpParams(Map<String, Object> params) {
        return URLEncodedUtils.format(makePairs(params), "UTF-8");
    }

    protected List<NameValuePair> makePairs(Map<String, Object> params) {
        ArrayList<NameValuePair> l = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> e : params.entrySet())
            l.add(new BasicNameValuePair(e.getKey(), ObjectUtils.toString(e.getValue())));
        return l;
    }

    public abstract Response get(String path, Map<String, Object> params);
    public abstract Response formPost(String path, Map<String, Object> params);
    public abstract Response multipartPost(String path, Map<String, Object> params);

    public Response get(String path, Object[][] params) {
        return get(path, CollectionsHelper.arraysToMap(params));
    }

    public Response formPost(String path, Object[][] params) {
        return formPost(path, CollectionsHelper.arraysToMap(params));
    }

    public Response multipartPost(String path, Object[][] params) {
        return multipartPost(path, CollectionsHelper.arraysToMap(params));
    }

    public static abstract class Response {

        public abstract int getStatusCode();

        public abstract String getContentType();

        public abstract InputStream getContent();

        public abstract String getText();

        public JsonNode getJsonNode() {
            return JsonHelper.parse(getText());
        }
    }
}
