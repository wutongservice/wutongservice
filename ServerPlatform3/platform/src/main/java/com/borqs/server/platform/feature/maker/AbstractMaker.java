package com.borqs.server.platform.feature.maker;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;

public abstract class AbstractMaker<T> implements Maker<T> {
    protected AbstractMaker() {
    }

    @Override
    public T make(Context ctx, String template, Object[][] opts) {
        return make(ctx, template, Record.of(opts));
    }
}
