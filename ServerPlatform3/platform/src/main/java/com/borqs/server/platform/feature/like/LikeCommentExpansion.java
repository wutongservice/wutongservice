package com.borqs.server.platform.feature.like;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.CommentExpansion;
import com.borqs.server.platform.feature.comment.Comments;
import com.borqs.server.platform.feature.conversation.ConversationJsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;

import java.util.List;
import java.util.Map;

public class LikeCommentExpansion extends LikeExpansionSupport implements CommentExpansion {

    static {
        Comment.registerColumnsAlias("@xlike,#xlike", EXPAND_COLUMNS);
    }

    public LikeCommentExpansion() {
    }

    @Override
    public void expand(Context ctx, String[] expCols, Comments data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (ArrayUtils.contains(expCols, COL_LIKES))
            expandLikes(ctx, data);

        if (ArrayUtils.contains(expCols, COL_LIKED))
            expandLiked(ctx, data);
    }

    private void expandLikes(Context ctx, Comments comments) {
        Target[] commentTargets = comments.getCommentTargets();
        Map<Target, String> m = expandLikesHelper(ctx, commentTargets);
        for (Comment comment : comments) {
            if (comment == null)
                continue;

            String json = MapUtils.getString(m, comment.getCommentTarget(), "{}");
            comment.setAddon(COL_LIKES, Addons.jsonAddonValue(json));
        }
    }

    private void expandLiked(Context ctx, Comments comments) {
        Target[] commentTargets = comments.getCommentTargets();
        Map<Target, Boolean> m = expandLikedHelper(ctx, commentTargets);
        for (Comment comment : comments) {
            if (comment == null)
                continue;

            Boolean b = m.get(comment.getCommentTarget());
            comment.setAddon(COL_LIKED, BooleanUtils.isTrue(b));
        }
    }
}
