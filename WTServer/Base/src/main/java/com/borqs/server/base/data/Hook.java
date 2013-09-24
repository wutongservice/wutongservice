package com.borqs.server.base.data;


import com.borqs.server.base.context.Context;

public interface Hook {
    void before(Context ctx, RecordSet recs);
    void after(Context ctx, RecordSet recs);
}
