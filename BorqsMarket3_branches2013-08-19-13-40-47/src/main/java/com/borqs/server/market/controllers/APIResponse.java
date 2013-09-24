package com.borqs.server.market.controllers;


import com.borqs.server.market.utils.JsonResponse;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;

import java.util.LinkedHashMap;

public class APIResponse extends JsonResponse {

    protected APIResponse(Object data) {
        super(data);
    }

    @Override
    protected Object wrapData(Object data) {
        LinkedHashMap<String, Object> newData = new LinkedHashMap<String, Object>();
        newData.put("code", 0);
        if (data instanceof RecordsWithTotal) {
            RecordsWithTotal recsWithTotal = (RecordsWithTotal) data;
            newData.put("data", recsWithTotal.getRecords());
            newData.put("total", recsWithTotal.getTotal());
        } else {
            newData.put("data", data);
        }
        return newData;
    }

    public static APIResponse of(Object data) {
        return new APIResponse(data);
    }

    public static APIResponse raw(String json) {
        return new APIResponse(new RawJson(json));
    }
}
