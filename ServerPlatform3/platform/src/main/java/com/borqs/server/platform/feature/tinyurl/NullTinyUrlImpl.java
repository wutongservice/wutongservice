package com.borqs.server.platform.feature.tinyurl;


import com.borqs.server.platform.context.Context;

public class NullTinyUrlImpl implements TinyUrlLogic {
    @Override
    public String toTiny(Context ctx, String fullUrl) {
        return fullUrl;
    }

    @Override
    public String toFull(Context ctx, String tinyUrl) {
        return tinyUrl;
    }
}
