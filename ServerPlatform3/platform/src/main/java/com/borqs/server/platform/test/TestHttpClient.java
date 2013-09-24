package com.borqs.server.platform.test;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.web.AbstractHttpClient;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class TestHttpClient extends AbstractHttpClient {
    private ServletUnitClient client;

    public TestHttpClient(ServletUnitClient client) {
        this.client = client;
    }

    public TestHttpClient(ServletUnitClient client, String host) {
        super(host);
        this.client = client;
    }

    public TestHttpClient(ServletUnitClient client, String host, Map<String, String> headers) {
        super(host, headers);
        this.client = client;
    }

    @Override
    public Response get(String path, Map<String, Object> params) {
        try {
            GetMethodWebRequest req = new GetMethodWebRequest(urlGet(path, params));
            return new TestResponse(client.getResponse(req));
        } catch (Exception e) {
            throw new ServerException(E.TEST, e);
        }
    }

    protected TestResponse post(String path, Map<String, Object> params) {
        try {
            PostMethodWebRequest req = new PostMethodWebRequest(urlInvoke(path));
            for (Map.Entry<String, Object> e : params.entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                if (v instanceof File) {
                    req.selectFile(k, (File)v);
                } else {
                    req.setParameter(k, ObjectUtils.toString(v));
                }
            }
            return new TestResponse(client.getResponse(req));
        } catch (Exception e) {
            throw new ServerException(E.TEST, e);
        }
    }

    @Override
    public Response formPost(String path, Map<String, Object> params) {
        return post(path, params);
    }

    @Override
    public Response multipartPost(String path, Map<String, Object> params) {
        return post(path, params);
    }

    public static class TestResponse extends Response {
        public final WebResponse webResponse;

        public TestResponse(WebResponse webResponse) {
            this.webResponse = webResponse;
        }

        @Override
        public int getStatusCode() {
            return webResponse.getResponseCode();
        }

        @Override
        public String getContentType() {
            return webResponse.getContentType();
        }

        @Override
        public InputStream getContent() {
            try {
                String contentEncoding = webResponse.getHeaderField("Content-Encoding");
                return StringUtils.equalsIgnoreCase(contentEncoding, "gzip")
                        ? new GZIPInputStream(webResponse.getInputStream())
                        : webResponse.getInputStream();
            } catch (IOException e) {
                throw new ServerException(E.TEST, e);
            }
        }

        @Override
        public String getText() {
            try {
                return webResponse.getText();
            } catch (IOException e) {
                throw new ServerException(E.TEST, e);
            }
        }
    }
}
