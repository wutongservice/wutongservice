package com.borqs.server.platform.web;


import com.borqs.server.platform.util.CollectionsHelper;
import org.codehaus.jackson.JsonNode;

import java.io.InputStream;
import java.util.Map;

public class DualHttpApiClient {
    private HttpApiClient client1;
    private HttpApiClient client2;

    public DualHttpApiClient() {
    }

    public DualHttpApiClient(HttpApiClient client1, HttpApiClient client2) {
        this.client1 = client1;
        this.client2 = client2;
    }

    public HttpApiClient getClient1() {
        return client1;
    }

    public void setClient1(HttpApiClient client1) {
        this.client1 = client1;
    }

    public HttpApiClient getClient2() {
        return client2;
    }

    public void setClient2(HttpApiClient client2) {
        this.client2 = client2;
    }

    public DualResponse get(String path, Map<String, Object> params) {
        HttpClient.Response resp1 = client1.get(path, params);
        HttpClient.Response resp2 = client2.get(path, params);
        return new DualResponse(resp1, resp2);
    }

    public DualResponse formPost(String path, Map<String, Object> params) {
        HttpClient.Response resp1 = client1.formPost(path, params);
        HttpClient.Response resp2 = client2.formPost(path, params);
        return new DualResponse(resp1, resp2);
    }

    public DualResponse multipartPost(String path, Map<String, Object> params) {
        HttpClient.Response resp1 = client1.multipartPost(path, params);
        HttpClient.Response resp2 = client2.multipartPost(path, params);
        return new DualResponse(resp1, resp2);
    }

    public DualResponse get(String path, Object[][] params) {
        return get(path, CollectionsHelper.arraysToMap(params));
    }

    public DualResponse formPost(String path, Object[][] params) {
        return formPost(path, CollectionsHelper.arraysToMap(params));
    }

    public DualResponse multipartPost(String path, Object[][] params) {
        return multipartPost(path, CollectionsHelper.arraysToMap(params));
    }

    public static class DualResponse {
        public final HttpClient.Response response1;
        public final HttpClient.Response response2;

        public DualResponse(HttpClient.Response response1, HttpClient.Response response2) {
            this.response1 = response1;
            this.response2 = response2;
        }

        public int getStatusCode1() {
            return response1.getStatusCode();
        }

        public String getContentType1() {
            return response1.getContentType();
        }

        public InputStream getContent1() {
            return response1.getContent();
        }

        public String getText1() {
            return response1.getText();
        }

        public JsonNode getJson1() {
            return response1.getJsonNode();
        }

        public int getStatusCode2() {
            return response2.getStatusCode();
        }

        public String getContentType2() {
            return response2.getContentType();
        }

        public InputStream getContent2() {
            return response2.getContent();
        }

        public String getText2() {
            return response2.getText();
        }

        public JsonNode getJson2() {
            return response2.getJsonNode();
        }
    }
}
