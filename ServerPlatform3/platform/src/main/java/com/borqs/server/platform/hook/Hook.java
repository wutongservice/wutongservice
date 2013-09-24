package com.borqs.server.platform.hook;


import com.borqs.server.platform.context.Context;

public interface Hook<T> {
    void before(Context ctx, T data);

    void after(Context ctx, T data);
}
