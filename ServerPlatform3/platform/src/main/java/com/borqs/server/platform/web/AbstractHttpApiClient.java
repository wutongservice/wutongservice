package com.borqs.server.platform.web;


import com.borqs.server.platform.data.Values;
import com.borqs.server.platform.feature.app.AppSign;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.ObjectUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractHttpApiClient {
    protected String host;
    protected String ticket;
    protected int appId;
    protected String appSecret;
    protected String userAgent;
    protected int timeout = 40 * 1000;

    protected AbstractHttpApiClient() {
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getHost() {
        return host;
    }

    public AbstractHttpApiClient setHost(String host) {
        this.host = host;
        return this;
    }

    public String getTicket() {
        return ticket;
    }

    public AbstractHttpApiClient setTicket(String ticket) {
        this.ticket = ticket;
        return this;
    }

    public int getAppId() {
        return appId;
    }

    public AbstractHttpApiClient setAppId(int appId) {
        this.appId = appId;
        return this;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public AbstractHttpApiClient setAppSecret(String appSecret) {
        this.appSecret = appSecret;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public AbstractHttpApiClient setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    protected abstract AbstractHttpClient createClient();

    public Map<String, Object> createParams(Map<String, Object> params) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>(params);
        Object theTicket = params.containsKey("ticket") ? params.get("ticket") : ticket;
        if (theTicket != null)
            m.put("ticket", theTicket);

        Object theAppId = params.containsKey("appid") ? params.get("appid") : appId;
        if (theAppId != null && Values.toInt(theAppId) > 0) {
            m.put("appid", theAppId.toString());
            m.put("sign_method", params.containsValue("sign_method") ? ObjectUtils.toString(params.get("sign_method"), "md5") : "md5");
            m.put("sign", params.containsKey("sign") ? ObjectUtils.toString(params.get("sign")) : AppSign.md5(appSecret, m.keySet()));
        }
        return m;
    }

    public AbstractHttpClient.Response get(String path, Map<String, Object> params) {
        AbstractHttpClient client = createClient();
        return client.get(path, createParams(params));
    }

    public AbstractHttpClient.Response formPost(String path, Map<String, Object> params) {
        AbstractHttpClient client = createClient();
        return client.formPost(path, createParams(params));
    }

    public AbstractHttpClient.Response multipartPost(String path, Map<String, Object> params) {
        AbstractHttpClient client = createClient();
        return client.multipartPost(path, createParams(params));
    }

    public AbstractHttpClient.Response get(String path, Object[][] params) {
        return get(path, CollectionsHelper.arraysToMap(params));
    }

    public AbstractHttpClient.Response formPost(String path, Object[][] params) {
        return formPost(path, CollectionsHelper.arraysToMap(params));
    }

    public AbstractHttpClient.Response multipartPost(String path, Object[][] params) {
        return multipartPost(path, CollectionsHelper.arraysToMap(params));
    }
}
