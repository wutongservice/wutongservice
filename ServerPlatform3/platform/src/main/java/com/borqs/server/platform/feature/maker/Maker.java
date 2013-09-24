package com.borqs.server.platform.feature.maker;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;

public interface Maker<T> {
    String[] getTemplates();
    T make(Context ctx, String template, Record opts);
    T make(Context ctx, String template, Object[][] opts);
}
