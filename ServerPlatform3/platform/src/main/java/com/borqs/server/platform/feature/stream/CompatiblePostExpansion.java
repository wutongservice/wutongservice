package com.borqs.server.platform.feature.stream;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.like.LikePostExpansion;
import org.apache.commons.collections.CollectionUtils;

@Deprecated
public class CompatiblePostExpansion implements PostExpansion {

    public static final String COL_I_LIKED = "iliked";

    public CompatiblePostExpansion() {
    }

    @Override
    public void expand(Context ctx, String[] expCols, Posts data) {
        if (CollectionUtils.isEmpty(data))
            return;

        for (Post post : data) {
            if (post == null)
                continue;

            if (post.hasAddon(LikePostExpansion.COL_LIKED))
                post.setAddon(COL_I_LIKED, post.checkAddon(LikePostExpansion.COL_LIKED));
        }
    }
}
