package com.borqs.server.base.web.webmethod;


import com.borqs.server.base.auth.WebSignatures;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.web.WebException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpEntity;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class WebMethodClient {
    private String apiUri;
    private String ticket;
    private String appId;
    private String appSecret;
    private String userAgent;

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
        HttpClient client = new DefaultHttpClient();
        if (userAgent != null)
            client.getParams().setParameter("User-Agent", userAgent);

        return client;
    }

    public HttpResponse get(String method, Object[][] params) {
        return get(method, CollectionUtils2.arraysToMap(params));
    }

    public HttpResponse get(String method, Map<String, Object> params) {
        try {
            HttpGet httpGet = new HttpGet(makeGetUrl(method, params));
            return createHttpClient().execute(httpGet);
        } catch (IOException e) {
            throw new WebException(e);
        }
    }

    public HttpResponse formPost(String method, Object[][] params) {
        return formPost(method, CollectionUtils2.arraysToMap(params));
    }

    public HttpResponse formPost(String method, Map<String, Object> params) {
        try {
            HttpPost httpPost = new HttpPost(makeInvokeUrl(method));
            httpPost.setEntity(new UrlEncodedFormEntity(makePairs(params), Charsets.DEFAULT));
            return createHttpClient().execute(httpPost);
        } catch (IOException e) {
            throw new WebException(e);
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
                        if (v instanceof File)
                            m.put(k, new FileBody((File) v));
                        else
                            m.put(k, new StringBody(ObjectUtils.toString(v, ""), Charsets.DEFAULT_CHARSET));
                    } else {
                        m.put(k, (ContentBody)v);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new WebException(e);
            }


            HttpPost httpPost = new HttpPost(makeInvokeUrl(method));
            httpPost.setEntity(makeMultipartEntity(m));
            return createHttpClient().execute(httpPost);
        } catch (IOException e) {
            throw new WebException(e);
        }
    }

    public static int getResponseCode(HttpResponse resp) {
        return resp.getStatusLine().getStatusCode();
    }

    public static InputStream getResponseContent(HttpResponse resp) {
        try {
        	HttpEntity entity = resp.getEntity();
            String contentEncoding = entity.getContentEncoding() != null ? entity.getContentEncoding().getValue() : "";
        	return StringUtils.equalsIgnoreCase(contentEncoding, "gzip")
        			? new GZIPInputStream(entity.getContent()) 
        			: entity.getContent();         	
        } catch (IOException e) {
            throw new WebException(e);
        }
    }

    public static String getResponseText(HttpResponse resp) {
        String charset = EntityUtils.getContentCharSet(resp.getEntity());
        try {
            return IOUtils.toString(getResponseContent(resp), charset != null ? charset : Charsets.DEFAULT);
        } catch (IOException e) {
            throw new WebException(e);
        }
    }
}
