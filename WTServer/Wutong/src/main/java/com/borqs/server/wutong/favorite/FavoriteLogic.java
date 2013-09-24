package com.borqs.server.wutong.favorite;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

import java.util.Map;

public interface FavoriteLogic {
    boolean saveFavorite(Context ctx, Record favorite);
    boolean destroyFavorite(Context ctx, String user_id, String target_type, String target_id);
    RecordSet getFavoriteSummary(Context ctx, String user_id, String target_types);
    boolean getIFavorited(Context ctx, String user_id, String target_type, String target_id);
    Map<String, Boolean> getIFavorited(Context ctx, String user_id, String target_type, String[] targetIds);
    String getFavoriteByType(Context ctx, String user_id, String target_type,int count,int page);
}