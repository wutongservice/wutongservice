package com.borqs.server.base.data;


import com.borqs.server.base.context.Context;

public interface Expansion {
    void expand(Context ctx, RecordSet recs, String[] cols);
}
