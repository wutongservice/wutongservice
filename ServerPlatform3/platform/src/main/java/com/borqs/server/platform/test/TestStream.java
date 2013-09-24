package com.borqs.server.platform.test;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.stream.*;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TestStream implements StreamLogic {
    private static final Logger L = Logger.get(TestStream.class);

    private Map<Long, Post> map = new HashMap<Long, Post>();

    private List<PostHook> createStreamHooks;
    private List<PostHook> updateStreamHooks;
    private List<PostHook> destroyStreamHooks;

    // expansion
    private List<PostExpansion> expansions;

    private ConversationLogic conversation;
    private AccountLogic account;

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public List<PostHook> getCreateStreamHooks() {
        return createStreamHooks;
    }

    public void setCreateStreamHooks(List<PostHook> createStreamHooks) {
        this.createStreamHooks = createStreamHooks;
    }

    public List<PostHook> getUpdateStreamHooks() {
        return updateStreamHooks;
    }

    public void setUpdateStreamHooks(List<PostHook> updateStreamHooks) {
        this.updateStreamHooks = updateStreamHooks;
    }

    public List<PostHook> getDestroyStreamHooks() {
        return destroyStreamHooks;
    }

    public void setDestroyStreamHooks(List<PostHook> destroyStreamHooks) {
        this.destroyStreamHooks = destroyStreamHooks;
    }


    public ConversationLogic getConversation() {
        return conversation;
    }

    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }


    @Override
    public Post createPost(Context ctx, Post post0) {
        final LogCall LC = LogCall.startCall(L, TestStream.class, "createPost",
                ctx, "post", post0);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("post", post0);
            Post post = post0.copy();

            HookHelper.before(createStreamHooks, ctx, post);

            map.put(post.getPostId(), post);


            Target postTarget = post.getPostTarget();
            conversation.create(ctx, new ConversationBase(postTarget, Actions.CREATE));

            if (CollectionUtils.isNotEmpty(post.getAddTo()))
                conversation.create(ctx, new ConversationBase(postTarget, Actions.ADDTO));

            if (CollectionUtils.isNotEmpty(post.getTo()))
                conversation.create(ctx, new ConversationBase(postTarget, Actions.TO));

            HookHelper.after(createStreamHooks, ctx, post);

            //conversationLogic.create(ctx, new ConversationBase(post.getTarget(), Reasons.COMMENT_CREATE));
            //TODO call OPline to send notification and email
            LC.endCall();
            return post;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean destroyPosts(Context ctx, long... postIds) {
        final LogCall LC = LogCall.startCall(L, TestStream.class, "destroyPosts",
                ctx, "postIds", postIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = true;
            for (long postId : postIds) {
                Post post = map.remove(postId);
                if (post == null)
                    b = false;
            }
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean updatePost(Context ctx, Post post0) {
        final LogCall LC = LogCall.startCall(L, TestStream.class, "updatePost", ctx, "post", post0);
        try {
            ParamChecker.notNull("ctx", ctx);

            Post post = post0.copy();
            boolean b = false;
            HookHelper.before(updateStreamHooks, ctx, post);
            if (map.containsKey(post.getPostId())) {
                map.put(post.getPostId(), post);
                b = true;
            }

            HookHelper.after(updateStreamHooks, ctx, post);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasPost(Context ctx, long postId) {
        final LogCall LC = LogCall.startCall(L, TestStream.class, "updatePost", ctx, "postId", postId);

        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = map.containsKey(postId);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAllPosts(Context ctx, long... postIds) {
        final LogCall LC = LogCall.startCall(L, TestStream.class, "updatePost", ctx, "postIds", postIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = true;
            for (long postId : postIds) {
                if (!map.containsKey(postId)) {
                    b = false;
                    break;
                }
            }
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAnyPosts(Context ctx, long... postIds) {
        final LogCall LC = LogCall.startCall(L, TestStream.class, "updatePost", ctx, "postIds", postIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = false;
            for (long postId : postIds) {
                if (map.containsKey(postId)) {
                    b = true;
                    break;
                }
            }
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts getPosts(Context ctx, String[] expCols, long... postIds) {
        final LogCall LC = LogCall.startCall(L, TestStream.class, "getPosts", ctx, "postIds", postIds);
        try {
            ParamChecker.notNull("ctx", ctx);
            Posts posts = new Posts();
            for (long postId : postIds) {
                Post post = map.get(postId);
                if (post != null)
                    posts.add(post.copy());
            }
            LC.endCall();
            return posts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Post getPost(Context ctx, String[] expCols, long postId) {
        final LogCall LC = LogCall.startCall(L, TestStream.class, "getPost", ctx, "postId", postId);
        try {
            Posts posts = this.getPosts(ctx, expCols, postId);
            Post post = posts.isEmpty() ? null : posts.get(0);
            LC.endCall();
            return post;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts getUserTimeline(Context ctx, long userId, PostFilter filter, String[] expCols, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Posts getWallTimeline(Context ctx, long userId, PostFilter filter, String[] expCols, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Posts getFriendsTimeline(Context ctx, long userId, PostFilter filter, String[] expCols, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Posts getPublicTimeline(Context ctx, PostFilter filter, String[] expCols, int count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Posts search(Context ctx, String text, PostFilter filter, String[] expCols, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, Integer> getUserTimelineCounts(Context ctx, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getUserTimelineCount(Context ctx, long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, Integer> getWallTimelineCounts(Context ctx, long... userIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getWallTimelineCount(Context ctx, long userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Posts getPostsInConversation(Context ctx, long userId, int reason, String[] expCols, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Post expand(Context ctx, String[] expCols, Post post) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void expand(Context ctx, String[] expCols, Posts data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Target[] getUserTimelineAttachments(Context ctx, long userId, PostFilter filter, Page page) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PostAttachments getWallTimelineAttachments(Context ctx, long userId, PostFilter filter, boolean includeSelf, Page page) {
        throw new UnsupportedOperationException();
    }
}
