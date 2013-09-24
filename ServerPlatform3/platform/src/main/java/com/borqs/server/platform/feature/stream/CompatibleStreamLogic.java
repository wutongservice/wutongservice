package com.borqs.server.platform.feature.stream;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.logic.Logic;

@Deprecated
public interface CompatibleStreamLogic extends Logic {
    Posts getPublicTimeline(Context ctx, PostFilter filter, String[] expCols, Page page);
}
