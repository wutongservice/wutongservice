package com.borqs.server.platform.test;

import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.web.AbstractHttpApiClient;
import com.borqs.server.platform.web.AbstractHttpClient;
import com.meterware.servletunit.ServletUnitClient;


public class TestHttpApiClient extends AbstractHttpApiClient {

    private final ServletUnitClient client;

    public TestHttpApiClient(ServletUnitClient client) {
        this.client = client;
    }

    @Override
    protected AbstractHttpClient createClient() {
        return new TestHttpClient(client, host, CollectionsHelper.of("User-Agent", userAgent));
    }
}
