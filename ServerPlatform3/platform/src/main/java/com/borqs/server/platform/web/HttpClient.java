package com.borqs.server.platform.web;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.Charsets;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HttpClient extends AbstractHttpClient {

    public static final int DEFAULT_TIMEOUT = 20 * 1000;

    private int timeout = DEFAULT_TIMEOUT;

    public HttpClient() {
    }

    public HttpClient(String host) {
        super(host);
    }

    public HttpClient(String host, Map<String, String> headers) {
        super(host, headers);
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout <= 0 ? DEFAULT_TIMEOUT : timeout;
    }

    protected org.apache.http.client.HttpClient createHttpClient() {
        org.apache.http.client.HttpClient client = new DefaultHttpClient();
        client.getParams().setIntParameter("http.socket.timeout", timeout);

        if (MapUtils.isNotEmpty(headers)) {
            for (Map.Entry<String, String> e : headers.entrySet())
                client.getParams().setParameter(e.getKey(), e.getValue());
        }
        return client;
    }

    @Override
    public Response get(String path, Map<String, Object> params) {
        try {
            HttpGet httpGet = new HttpGet(urlGet(path, params));
            return new ApacheHttpClientResponse(createHttpClient().execute(httpGet));
        } catch (IOException e) {
            throw new ServerException(E.WEB_CLIENT, e);
        }
    }

    @Override
    public Response formPost(String path, Map<String, Object> params) {
        try {
            HttpPost httpPost = new HttpPost(urlInvoke(path));
            httpPost.setEntity(new UrlEncodedFormEntity(makePairs(params), Charsets.DEFAULT));
            return new ApacheHttpClientResponse(createHttpClient().execute(httpPost));
        } catch (IOException e) {
            throw new ServerException(E.WEB_CLIENT, e);
        }
    }

    @Override
    public Response multipartPost(String path, Map<String, Object> params) {
        try {
            LinkedHashMap<String, ContentBody> m = new LinkedHashMap<String, ContentBody>();
            try {
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();
                    if (!(v instanceof ContentBody)) {
                        if (v instanceof File)
                            m.put(k, new FileBody((File) v));
                        else
                            m.put(k, new StringBody(ObjectUtils.toString(v), Charsets.DEFAULT_CHARSET));
                    } else {
                        m.put(k, (ContentBody) v);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new ServerException(E.WEB_CLIENT, e);
            }

            MultipartEntity me = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            for (Map.Entry<String, ContentBody> e : m.entrySet())
                me.addPart(e.getKey(), e.getValue());

            HttpPost httpPost = new HttpPost(urlInvoke(path));
            httpPost.setEntity(me);
            return new ApacheHttpClientResponse(createHttpClient().execute(httpPost));
        } catch (IOException e) {
            throw new ServerException(E.WEB_CLIENT, e);
        }
    }

    protected static class ApacheHttpClientResponse extends Response {
        public final HttpResponse httpResponse;

        public ApacheHttpClientResponse(HttpResponse httpResponse) {
            this.httpResponse = httpResponse;
        }

        @Override
        public int getStatusCode() {
            return httpResponse.getStatusLine().getStatusCode();
        }

        @Override
        public String getContentType() {
            return httpResponse.getEntity().getContentType().getValue();
        }

        @Override
        public InputStream getContent() {
            try {
                HttpEntity entity = httpResponse.getEntity();
                String contentEncoding = entity.getContentEncoding() != null ? entity.getContentEncoding().getValue() : "";
                return StringUtils.equalsIgnoreCase(contentEncoding, "gzip")
                        ? new GZIPInputStream(entity.getContent())
                        : entity.getContent();
            } catch (IOException e) {
                throw new ServerException(E.WEB_CLIENT, e);
            }
        }

        @Override
        public String getText() {
            String charset = EntityUtils.getContentCharSet(httpResponse.getEntity());
            try {
                return IOUtils.toString(getContent(), charset != null ? charset : Charsets.DEFAULT);
            } catch (IOException e) {
                throw new ServerException(E.WEB_CLIENT, e);
            }
        }
    }
}
