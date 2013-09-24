package com.borqs.server.platform.expansion;


import com.borqs.server.platform.context.Context;

public interface Expansion<T> {
    void expand(Context ctx, String[] expCols, T data);
}
