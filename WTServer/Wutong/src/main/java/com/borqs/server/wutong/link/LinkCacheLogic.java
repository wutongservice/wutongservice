package com.borqs.server.wutong.link;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;

public interface LinkCacheLogic {
    boolean saveLinkCache(Context ctx, String host,String img_name);
    boolean hasImgInHost(Context ctx, String host, String img_name);
}