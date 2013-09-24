package com.borqs.server.platform.feature.stream;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.expansion.Expander;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.logic.Logic;

import java.util.Map;

public interface StreamLogic extends Logic, Expander<Posts> {

    Post createPost(Context ctx, Post post);

    boolean destroyPosts(Context ctx, long... postIds);

    boolean updatePost(Context ctx, Post post);

    boolean hasPost(Context ctx, long postId);

    boolean hasAllPosts(Context ctx, long... postIds);

    boolean hasAnyPosts(Context ctx, long... postIds);

    Posts getPosts(Context ctx, String[] expCols, long... postIds);

    Post getPost(Context ctx, String[] expCols, long postId);



    Posts getUserTimeline(Context ctx, long userId, PostFilter filter, String[] expCols, Page page);

    Posts getWallTimeline(Context ctx, long userId, PostFilter filter, String[] expCols, Page page);

    Posts getFriendsTimeline(Context ctx, long userId, PostFilter filter, String[] expCols, Page page);

    Posts getPublicTimeline(Context ctx, PostFilter filter, String[] expCols, int count);

    Posts search(Context ctx, String text, PostFilter filter, String[] expCols, Page page);

    Map<Long, Integer> getUserTimelineCounts(Context ctx, long... userIds);

    int getUserTimelineCount(Context ctx, long userId);

    Map<Long, Integer> getWallTimelineCounts(Context ctx, long... userIds);

    int getWallTimelineCount(Context ctx, long userId);

    Posts getPostsInConversation(Context ctx, long userId, int reason, String[] expCols, Page page);

    Target[] getUserTimelineAttachments(Context ctx, long userId, PostFilter filter, Page page);

    PostAttachments getWallTimelineAttachments(Context ctx, long userId, PostFilter filter, boolean includeSelf, Page page);

    Post expand(Context ctx, String[] expCols, Post post);
}
