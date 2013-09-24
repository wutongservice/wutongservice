package com.borqs.server.platform.expansion;


import com.borqs.server.platform.context.Context;

public interface Expander<T> {
    void expand(Context ctx, String[] expCols, T data);
}
