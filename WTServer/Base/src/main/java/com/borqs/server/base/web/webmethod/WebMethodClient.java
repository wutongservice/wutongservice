package com.borqs.server.base.web.webmethod;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.auth.WebSignatures;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.util.CollectionUtils2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class WebMethodClient {
    public static final int CURL_FLAG_COMPRESSED = 1;
    public static final int CURL_FLAG_FOR_TIME = 1 << 1;

    private String apiUri;
    private String ticket;
    private String appId;
    private String appSecret;
    private String userAgent;
    private boolean viewElapsed = false;

    protected WebMethodClient(String apiUri, String ticket, String appId, String appSecret) {
        this.apiUri = apiUri;
        this.ticket = ticket;
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public static WebMethodClient create(String apiUri) {
        Validate.notNull(apiUri);
        return new WebMethodClient(apiUri, null, null, null);
    }

    public static WebMethodClient create(String apiUri, String ticket) {
        Validate.notNull(apiUri);
        return new WebMethodClient(apiUri, ticket, null, null);
    }

    public static WebMethodClient create(String apiUri, String appId, String appSecret) {
        Validate.notNull(apiUri);
        return new WebMethodClient(apiUri, null, appId, appSecret);
    }

    public static WebMethodClient create(String apiUri, String ticket, String appId, String appSecret) {
        Validate.notNull(apiUri);
        return new WebMethodClient(apiUri, ticket, appId, appSecret);
    }

    public String getApiUri() {
        return apiUri;
    }

    public String getTicket() {
        return ticket;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isViewElapsed() {
        return viewElapsed;
    }

    public void setViewElapsed(boolean viewElapsed) {
        this.viewElapsed = viewElapsed;
    }

    public List<NameValuePair> makePairs(Map<String, Object> params) {
        ArrayList<NameValuePair> l = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            l.add(new BasicNameValuePair(e.getKey(), ObjectUtils.toString(e.getValue(), "")));
        }
        if (ticket != null) {
            l.add(new BasicNameValuePair("ticket", params.containsKey("ticket")
                    ? ObjectUtils.toString(params.get(ticket), "") : ticket));
        }
        if (appId != null) {
            l.add(new BasicNameValuePair("appid", params.containsKey("appid")
                    ? ObjectUtils.toString(params.get("appid"), "") : appId));

            l.add(new BasicNameValuePair("sign_method", ObjectUtils.toString(params.get("sign_method"), "md5")));
            l.add(new BasicNameValuePair("sign", WebSignatures.md5Sign(appSecret, params.keySet())));
        }
        return l;
    }

    public String encodeHttpParams(Map<String, Object> params) {
        return URLEncodedUtils.format(makePairs(params), "UTF-8");
    }

    public String makeInvokeUrl(String method) {
        return new StringBuilder(StringUtils.removeEnd(apiUri, "/")).append("/").append(method).toString();
    }

    public String makeGetUrl(String method, Object[][] params) {
        return makeGetUrl(method, CollectionUtils2.arraysToMap(params));
    }

    public String makeGetUrl(String method, Map<String, Object> params) {
        return (makeInvokeUrl(method) + "?" + encodeHttpParams(params));
    }


    private HttpClient createHttpClient() {
        return new DefaultHttpClient();
    }

    private void setupHeaders(HttpMessage http) {
        if (userAgent != null)
            http.setHeader("User-Agent", userAgent);
        if (viewElapsed)
            http.setHeader(WebMethodServlet.ELAPSED_COUNTER_HEADER, "1");
    }

    public HttpResponse get(String method, Object[][] params) {
        return get(method, CollectionUtils2.arraysToMap(params));
    }

    public HttpResponse get(String method, Map<String, Object> params) {
        try {
            HttpGet httpGet = new HttpGet(makeGetUrl(method, params));
            setupHeaders(httpGet);
            return createHttpClient().execute(httpGet);
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_WEBMETHOD_REQUEST_ERROR, e);
        }
    }

    public HttpResponse formPost(String method, Object[][] params) {
        return formPost(method, CollectionUtils2.arraysToMap(params));
    }

    public HttpResponse formPost(String method, Map<String, Object> params) {
        try {
            HttpPost httpPost = new HttpPost(makeInvokeUrl(method));
            httpPost.setEntity(new UrlEncodedFormEntity(makePairs(params), Charsets.DEFAULT));
            setupHeaders(httpPost);
            return createHttpClient().execute(httpPost);
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_WEBMETHOD_REQUEST_ERROR, e);
        }
    }

    private MultipartEntity makeMultipartEntity(Map<String, ContentBody> params) throws UnsupportedEncodingException {
        MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        for (Map.Entry<String, ContentBody> e : params.entrySet()) {
            entity.addPart(e.getKey(), e.getValue());
        }
        if (ticket != null) {
            entity.addPart("ticket", new StringBody(ticket, Charsets.DEFAULT_CHARSET));
        }
        if (appId != null) {
            entity.addPart("appid", new StringBody(appId, Charsets.DEFAULT_CHARSET));
            entity.addPart("sign_method", new StringBody("md5", Charsets.DEFAULT_CHARSET));
            entity.addPart("sign", new StringBody(WebSignatures.md5Sign(appSecret, params.keySet()), Charsets.DEFAULT_CHARSET));
        }

        return entity;
    }

    public HttpResponse multipartPost(String method, Object[][] params) {
        return multipartPost(method, CollectionUtils2.arraysToMap(params));
    }

    public HttpResponse multipartPost(String method, Map<String, Object> params) {
        try {
            LinkedHashMap<String, ContentBody> m = new LinkedHashMap<String, ContentBody>();
            try {
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    String k = e.getKey();
                    Object v = e.getValue();
                    if (!(v instanceof ContentBody)) {
                        if (v instanceof File) {
                            File f = (File) v;
                            String type = "application/octet-stream";
                            String ext = FilenameUtils.getExtension(f.getName());
                            if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")) {
                                type = "image/jpeg";
                            }
                            m.put(k, new FileBody(f, type));
                        } else {
                            m.put(k, new StringBody(ObjectUtils.toString(v, ""), Charsets.DEFAULT_CHARSET));
                        }
                    } else {
                        m.put(k, (ContentBody) v);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new ServerException(BaseErrors.PLATFORM_WEBMETHOD_REQUEST_ERROR, e);
            }


            HttpPost httpPost = new HttpPost(makeInvokeUrl(method));
            httpPost.setEntity(makeMultipartEntity(m));
            setupHeaders(httpPost);
            return createHttpClient().execute(httpPost);
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_WEBMETHOD_REQUEST_ERROR, e);
        }
    }

    public String curlCommandForGet(String method, int flags, Object[][] params) {
        return curlCommandForGet(method, flags, CollectionUtils2.arraysToMap(params));
    }

    private String makeCurlOpts(int flags) {
        String opts = "";
        if ((flags & CURL_FLAG_COMPRESSED) != 0)
            opts += " --compressed ";
        if ((flags & CURL_FLAG_FOR_TIME) != 0)
            opts += " -o /dev/null -s -w '"
                    + "------------------------------------\\n"
                    + "HTTP status   : %{http_code}\\n"
                    + "Content type  : %{content_type}\\n"
                    + "Download size : %{size_download} B\\n"
                    + "DNS lookup    : %{time_namelookup} s\\n"
                    + "Connect time  : %{time_connect} s\\n"
                    + "Transfer time : %{time_starttransfer} s\\n"
                    + "Total Time    : %{time_total} s\\n"
                    + "' ";

        if (viewElapsed)
            opts += " -H '" + WebMethodServlet.ELAPSED_COUNTER_HEADER + ": 1' ";
        return opts;
    }


    public String curlCommandForGet(String method, int flags, Map<String, Object> params) {
        String url = makeGetUrl(method, params);
        String opts = makeCurlOpts(flags);
        return String.format("curl -X GET %s '%s'", opts, url);
    }

    public String curlCommandForFormPost(String method, int flags, Object[][] params) {
        return curlCommandForFormPost(method, flags, CollectionUtils2.arraysToMap(params));
    }

    public String curlCommandForFormPost(String method, int flags, Map<String, Object> params) {
        String url = makeInvokeUrl(method);
        String data = encodeHttpParams(params);
        String opts = makeCurlOpts(flags);
        return String.format("curl -X POST %s -d '%s' '%s'", opts, data, url);
    }

    public String curlCommandForMultipartPost(String method, int flags, Object[][] params) {
        return curlCommandForMultipartPost(method, flags, CollectionUtils2.arraysToMap(params));
    }

    public String curlCommandForMultipartPost(String method, int flags, Map<String, Object> params) {
        String url = makeInvokeUrl(method);
        StringBuilder buff = new StringBuilder();
        String opts = makeCurlOpts(flags);
        for (Map.Entry<String, Object> e : params.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (v instanceof File)
                buff.append(String.format(" -F '%s=@%s'", k, ((File)v).getAbsolutePath()));
            else
                buff.append(String.format(" -F '%s=%s'", k, ObjectUtils.toString(v)));
        }
        return String.format("curl -X POST %s %s '%s'", opts, buff.toString(), url);
    }

    public static int getResponseCode(HttpResponse resp) {
        return resp.getStatusLine().getStatusCode();
    }

    public static InputStream getResponseContent(HttpResponse resp, SizeInfo sizeInfo) {
        try {
            HttpEntity entity = resp.getEntity();
            String contentEncoding = entity.getContentEncoding() != null ? entity.getContentEncoding().getValue() : "";
            if (sizeInfo == null) {
                return StringUtils.equalsIgnoreCase(contentEncoding, "gzip")
                        ? new GZIPInputStream(entity.getContent())
                        : entity.getContent();
            } else {
                if (StringUtils.equalsIgnoreCase(contentEncoding, "gzip")) {
                    ByteArrayOutputStream compressedBuff = new ByteArrayOutputStream();
                    IOUtils.copy(entity.getContent(), compressedBuff);
                    byte[] compressedBytes = compressedBuff.toByteArray();
                    ByteArrayOutputStream buff = new ByteArrayOutputStream();
                    IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(compressedBytes)), buff);
                    byte[] bytes = buff.toByteArray();
                    sizeInfo.compressedSize = compressedBytes.length;
                    sizeInfo.size = bytes.length;
                    return new ByteArrayInputStream(bytes);
                } else {
                    ByteArrayOutputStream buff = new ByteArrayOutputStream();
                    IOUtils.copy(entity.getContent(), buff);
                    byte[] bytes = buff.toByteArray();
                    sizeInfo.size = bytes.length;
                    sizeInfo.compressedSize = bytes.length;
                    return new ByteArrayInputStream(bytes);
                }
            }
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_WEBMETHOD_GET_CONTENT_ERROR, e);
        }
    }


    public static String getResponseText(HttpResponse resp, SizeInfo sizeInfo) {
        String charset = EntityUtils.getContentCharSet(resp.getEntity());
        try {
            return IOUtils.toString(getResponseContent(resp, sizeInfo), charset != null ? charset : Charsets.DEFAULT);
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_WEBMETHOD_GET_CONTENT_ERROR, e);
        }
    }

    public static class SizeInfo {
        public int size = 0;
        public int compressedSize = 0;
    }


}
