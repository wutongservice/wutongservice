package com.borqs.server.platform.hook;


import com.borqs.server.platform.context.Context;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class HookHelper {

    public static <T> void before(List<? extends Hook<T>> hooks, Context ctx, T data) {
        if (CollectionUtils.isNotEmpty(hooks)) {
            for (Hook<T> hook : hooks)
                hook.before(ctx, data);
        }
    }

    public static <T> void after(List<? extends Hook<T>> hooks, Context ctx, T data) {
        if (CollectionUtils.isNotEmpty(hooks)) {
            for (Hook<T> hook : hooks)
                hook.after(ctx, data);
        }
    }

}
