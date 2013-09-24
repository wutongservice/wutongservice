package com.borqs.server.platform.feature.cibind;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface CibindLogic extends Logic {

    long whoBinding(Context ctx, String info);
    
    Map<String, Long> whoBinding(Context ctx, String... infos);

    boolean hasBinding(Context ctx, String info);

    boolean hasBinding(Context ctx, long userId, String info);

    void bind(Context ctx, BindingInfo info);

    void bind(Context ctx, String type, String info);

    boolean unbind(Context ctx, String info);

    Map<Long, BindingInfo[]> getBindings(Context ctx, long[] userIds);

    BindingInfo[] getBindings(Context ctx, long userId);

    String[] getBindings(Context ctx, long userId, String type);
}
