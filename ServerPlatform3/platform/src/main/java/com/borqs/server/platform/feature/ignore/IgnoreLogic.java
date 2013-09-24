package com.borqs.server.platform.feature.ignore;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.logic.Logic;

public interface IgnoreLogic extends Logic {

    void ignore(Context ctx, int feature, Target... targets);
    void unignore(Context ctx, int feature, Target... targets);

    Target[] getIgnored(Context ctx, long userId, int feature);
}
