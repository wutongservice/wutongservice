package com.borqs.server.wutong.group;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

import java.util.Map;

public interface EventThemeLogic {
    Map<Long, Record> getEventThemes(Context ctx, long... themeIds);

    Record addEventTheme(Context ctx, long id, long creator, long updatedTime, String name, String imageUrl);

    RecordSet getEventThemes(Context ctx, int page, int count);
}
