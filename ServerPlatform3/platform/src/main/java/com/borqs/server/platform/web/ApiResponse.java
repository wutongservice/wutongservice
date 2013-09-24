package com.borqs.server.platform.web;


import org.codehaus.jackson.JsonNode;

import java.io.InputStream;

public class ApiResponse extends AbstractHttpClient.Response {
    private JsonNode node;
    public final AbstractHttpClient.Response response;

    public ApiResponse(AbstractHttpClient.Response response) {
        this.response = response;
    }

    @Override
    public int getStatusCode() {
        return response.getStatusCode();
    }

    @Override
    public String getContentType() {
        return response.getContentType();
    }

    @Override
    public InputStream getContent() {
        return response.getContent();
    }

    @Override
    public String getText() {
        return response.getText();
    }

    @Override
    public JsonNode getJsonNode() {
        if (node == null) {
            try {
                node = response.getJsonNode();
            } catch (Exception e) {
                node = null;
            }
        }
        return node;
    }

    public boolean isJson() {
        return getJsonNode() != null;
    }

    public boolean isSucceed() {
        JsonNode jn = getJsonNode();
        if (jn == null)
            return false;

        if (getStatusCode() != 200)
            return false;

        if (jn.isObject() && jn.has("error_code") && jn.has("error_msg"))
            return false;

        return true;
    }
}
