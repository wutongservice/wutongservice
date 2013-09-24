package com.borqs.server.market.models;

import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.mybatis.typehandlers.AbstractStringHandler;
import org.codehaus.jackson.JsonNode;

import java.util.ArrayList;

public class VersionedProductIdArrayHandler extends AbstractStringHandler<VersionedProductId[]> {
    public VersionedProductIdArrayHandler() {
    }

    @Override
    protected String toString(VersionedProductId[] val) throws Exception {
        return JsonUtils.toJson(val, false);
    }

    @Override
    protected VersionedProductId[] fromString(String s) throws Exception {
        JsonNode arr = JsonUtils.parseJson(s);
        ArrayList<VersionedProductId> l = new ArrayList<VersionedProductId>();
        for (int i = 0; i < arr.size(); i++) {
            l.add(VersionedProductId.fromJsonNode(arr.get(i)));
        }
        return l.toArray(new VersionedProductId[l.size()]);
    }
}
