package com.borqs.server.platform.feature.comment;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.feature.photo.PhotoExpansion;
import com.borqs.server.platform.feature.photo.Photos;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentPhotoExpansion extends CommentExpansionSupport implements PhotoExpansion {

    public static final String[] EXPAND_COLUMNS = {COL_COMMENTS};

    public CommentPhotoExpansion() {
    }

    public int getLastCommentCount() {
        return lastCommentCount;
    }

    public void setLastCommentCount(int lastCommentCount) {
        this.lastCommentCount = lastCommentCount;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Photos data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (expCols == null || ArrayUtils.contains(expCols, COL_COMMENTS))
            expandComments(ctx, data);
    }

    private void expandComments(Context ctx, Photos photos) {
        Target[] postsPhotoTargets = photos.getPhotoTargets();
        Map<Target, String> m = expandCommentsHelper(ctx, postsPhotoTargets);
        for (Photo photo : photos) {
            if (photo == null)
                continue;

            String json = MapUtils.getString(m, photo.getPhotoTarget(), "{}");
            photo.setAddon(COL_COMMENTS, Addons.jsonAddonValue(json));
        }
    }

    private Comment[] orderComments(Context ctx, Comment[] comments) {
        long[] friends = friend.getBorqsFriendIds(ctx, ctx.getViewer());
        Map<Long, Long> map = new HashMap<Long, Long>();
        for (long l : friends) {
            map.put(l, l);
        }

        if (ArrayUtils.isEmpty(comments))
            return new Comment[]{};

        List<Comment> commentsFriend = new ArrayList<Comment>();
        List<Comment> commentsOthers = new ArrayList<Comment>();

        for (Comment comment : comments) {
            if (map.containsKey(comment.getCommenterId()))
                commentsFriend.add(comment);
            else
                commentsOthers.add(comment);
        }
        commentsFriend.addAll(commentsOthers);
        commentsFriend = commentsFriend.subList(0,5);

        Comment[] c = new Comment[commentsFriend.size()];
        return commentsFriend.toArray(c);
    }



}
