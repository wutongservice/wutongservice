package com.borqs.server.platform.feature.link;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

public interface LinkLogic extends Logic {
    LinkEntity get(Context ctx, String url);
}
