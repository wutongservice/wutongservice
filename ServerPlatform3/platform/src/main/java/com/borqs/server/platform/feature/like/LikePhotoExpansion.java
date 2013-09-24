package com.borqs.server.platform.feature.like;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.feature.photo.PhotoExpansion;
import com.borqs.server.platform.feature.photo.Photos;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;

import java.util.Map;

public class LikePhotoExpansion extends LikeExpansionSupport implements PhotoExpansion {

    static {
        Comment.registerColumnsAlias("@xlike,#xlike", EXPAND_COLUMNS);
    }

    public LikePhotoExpansion() {
    }

    @Override
    public void expand(Context ctx, String[] expCols, Photos data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (ArrayUtils.contains(expCols, COL_LIKES))
            expandLikes(ctx, data);

        if (ArrayUtils.contains(expCols, COL_LIKED))
            expandLiked(ctx, data);
    }

    private void expandLikes(Context ctx, Photos photos) {
        Target[] photosCommentTargets = photos.getPhotoTargets();
        Map<Target, String> m = expandLikesHelper(ctx, photosCommentTargets);
        for (Photo photo : photos) {
            if (photo == null)
                continue;

            String json = MapUtils.getString(m, photo.getPhotoTarget(), "{}");
            photo.setAddon(COL_LIKES, Addons.jsonAddonValue(json));
        }
    }

    private void expandLiked(Context ctx, Photos photos) {
        Target[] photosCommentTargets = photos.getPhotoTargets();
        Map<Target, Boolean> m = expandLikedHelper(ctx, photosCommentTargets);
        for (Photo photo : photos) {
            if (photo == null)
                continue;

            Boolean b = m.get(photo.getPhotoTarget());
            photo.setAddon(COL_LIKED, BooleanUtils.isTrue(b));
        }
    }

}
