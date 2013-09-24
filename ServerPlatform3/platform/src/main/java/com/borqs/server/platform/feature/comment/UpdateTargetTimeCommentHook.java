package com.borqs.server.platform.feature.comment;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;

public class UpdateTargetTimeCommentHook implements CommentHook {
    private StreamLogic stream;

    public UpdateTargetTimeCommentHook() {
    }

    public StreamLogic getStream() {
        return stream;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    @Override
    public void before(Context ctx, Comment data) {
    }

    @Override
    public void after(Context ctx, Comment data) {
        if (data == null || data.getTarget() == null)
            return;

        Target t = data.getTarget();
        if (t.type == Target.POST)
            updatePostTime(ctx, t.getIdAsLong());
    }

    private void updatePostTime(Context ctx, long postId) {
        stream.updatePost(ctx, new Post(postId));
    }
}
