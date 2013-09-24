package com.borqs.server.market.controllers;


import com.borqs.server.market.utils.JsonResponse;

import java.util.LinkedHashMap;

public class APIResponse extends JsonResponse {
    protected APIResponse(Object data) {
        super(data);
    }

    @Override
    protected Object wrapData(Object data) {
        LinkedHashMap<String, Object> newData = new LinkedHashMap<String, Object>();
        newData.put("code", 0);
        newData.put("data", data);
        return newData;
    }

    public static APIResponse of(Object data) {
        return new APIResponse(data);
    }

    public static APIResponse raw(String json) {
        return new APIResponse(new RawJson(json));
    }
}
