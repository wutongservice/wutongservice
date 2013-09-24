package com.borqs.server.platform.feature.like;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;

public class UpdateTargetTimeLikeHook implements LikeHook {
    private StreamLogic stream;

    public UpdateTargetTimeLikeHook() {
    }

    public StreamLogic getStream() {
        return stream;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    @Override
    public void before(Context ctx, Target data) {
    }

    @Override
    public void after(Context ctx, Target data) {
        if (data == null)
            return;

        Target t = data;
        if (t.type == Target.POST)
            updatePostTime(ctx, t.getIdAsLong());
    }

    private void updatePostTime(Context ctx, long postId) {
        stream.updatePost(ctx, new Post(postId));
    }
}
