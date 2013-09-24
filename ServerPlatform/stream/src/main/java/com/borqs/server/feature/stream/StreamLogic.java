package com.borqs.server.feature.stream;


import com.borqs.server.base.context.Context;

import java.util.List;

public interface StreamLogic {
    Post createPost(Context ctx, Post post);
    boolean destroyPost(Context ctx, long postId);
    boolean updatePost(Context ctx, Post post);
    List<Post> getPosts(Context ctx, long[] postIds);
    Post getPost(Context ctx, long postId);
}
