package com.borqs.server.platform.web;


import com.borqs.server.platform.util.CollectionsHelper;

public class HttpApiClient extends AbstractHttpApiClient {
    public HttpApiClient() {
    }

    protected AbstractHttpClient createClient() {
        HttpClient client = new HttpClient(host, CollectionsHelper.of("User-Agent", userAgent));
        client.setTimeout(timeout);
        return client;
    }
}
