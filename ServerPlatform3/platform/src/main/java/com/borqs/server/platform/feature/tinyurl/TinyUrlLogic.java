package com.borqs.server.platform.feature.tinyurl;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

public interface TinyUrlLogic extends Logic {
    String toTiny(Context ctx, String fullUrl);
    String toFull(Context ctx, String tinyUrl);
}
