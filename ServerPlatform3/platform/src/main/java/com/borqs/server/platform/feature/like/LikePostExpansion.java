package com.borqs.server.platform.feature.like;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostExpansion;
import com.borqs.server.platform.feature.stream.Posts;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;

import java.util.Map;

public class LikePostExpansion extends LikeExpansionSupport implements PostExpansion {

    static {
        Post.registerColumnsAlias("@xlike,#xlike", EXPAND_COLUMNS);
    }

    public LikePostExpansion() {
    }

    @Override
    public void expand(Context ctx, String[] expCols, Posts posts) {
        if (CollectionUtils.isEmpty(posts))
            return;

        if (ArrayUtils.contains(expCols, COL_LIKES))
            expandLikes(ctx, posts);

        if (ArrayUtils.contains(expCols, COL_LIKED))
            expandLiked(ctx, posts);
    }

    private void expandLikes(Context ctx, Posts posts) {
        Target[] postTargets = posts.getPostTargets();
        Map<Target, String> m = expandLikesHelper(ctx, postTargets);
        for (Post post : posts) {
            if (post == null)
                continue;

            String json = MapUtils.getString(m, post.getPostTarget(), "{}");
            post.setAddon(COL_LIKES, Addons.jsonAddonValue(json));
        }
    }

    private void expandLiked(Context ctx, Posts posts) {
        Target[] postTargets = posts.getPostTargets();
        Map<Target, Boolean> m = expandLikedHelper(ctx, postTargets);
        for (Post post : posts) {
            if (post == null)
                continue;

            Boolean b = m.get(post.getPostTarget());
            post.setAddon(COL_LIKED, BooleanUtils.isTrue(b));
        }
    }
}
