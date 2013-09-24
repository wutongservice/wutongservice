package com.borqs.server.impl.stream;

import com.borqs.server.impl.stream.timeline.RedisFriendsTimeline;
import com.borqs.server.impl.stream.timeline.RedisOutboxTimeline;
import com.borqs.server.impl.stream.timeline.RedisPublicTimeline;
import com.borqs.server.impl.stream.timeline.RedisWallTimeline;
import com.borqs.server.platform.cache.redis.Redis;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.expansion.ExpansionHelper;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.ignore.Features;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.feature.opline.Operation;
import com.borqs.server.platform.feature.opline.Operations;
import com.borqs.server.platform.feature.stream.*;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntries;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntry;
import com.borqs.server.platform.feature.stream.timeline.TimelineResult;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ClassHelper;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.*;


public class StreamImpl implements StreamLogic, CompatibleStreamLogic {
    private static final Logger L = Logger.get(StreamImpl.class);

    private List<PostHook> createPostHooks;
    private List<PostHook> updatePostHooks;
    private List<PostHook> destroyPostHooks;
    // db
    private final StreamDb db = new StreamDb();
    // expansion
    private final BuiltinExpansion builtinExpansion = new BuiltinExpansion();
    private List<PostExpansion> expansions;

    private ConversationLogic conversation;
    private AccountLogic account;
    private FriendLogic friend;
    private IgnoreLogic ignore;
    private Redis redis;

    private RedisOutboxTimeline outboxTimeline = new RedisOutboxTimeline();
    private RedisWallTimeline wallTimeline = new RedisWallTimeline();
    private RedisFriendsTimeline friendsTimeline = new RedisFriendsTimeline();
    private RedisPublicTimeline publicTimeline = new RedisPublicTimeline();

    private Map<Integer, String> typeIconUrls;

    public StreamImpl() {
        outboxTimeline.setStream(this);
        wallTimeline.setStream(this);
        friendsTimeline.setStream(this);
        friendsTimeline.setOutboxTimeline(outboxTimeline);
        publicTimeline.setStream(this);
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public List<PostHook> getCreatePostHooks() {
        return createPostHooks;
    }

    public void setCreatePostHooks(List<PostHook> createPostHooks) {
        this.createPostHooks = createPostHooks;
    }

    public List<PostHook> getUpdatePostHooks() {
        return updatePostHooks;
    }

    public void setUpdatePostHooks(List<PostHook> updatePostHooks) {
        this.updatePostHooks = updatePostHooks;
    }

    public List<PostHook> getDestroyPostHooks() {
        return destroyPostHooks;
    }

    public void setDestroyPostHooks(List<PostHook> destroyPostHooks) {
        this.destroyPostHooks = destroyPostHooks;
    }

    public List<PostExpansion> getExpansions() {
        return expansions;
    }

    public void setExpansions(List<PostExpansion> expansions) {
        this.expansions = expansions;
    }

    public ConversationLogic getConversation() {
        return conversation;
    }

    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getPostTable() {
        return db.getPostTable();
    }

    public void setPostTable(Table postTable) {
        db.setPostTable(postTable);
    }

    public Redis getOutboxTimelineRedis() {
        return outboxTimeline.getRedis();
    }

    public void setOutboxTimelineRedis(Redis redis) {
        outboxTimeline.setRedis(redis);
    }

    public Redis getWallTimelineRedis() {
        return wallTimeline.getRedis();
    }

    public void setWallTimelineRedis(Redis redis) {
        wallTimeline.setRedis(redis);
    }

    public int getWallTimelineStorageLimit() {
        return wallTimeline.getStorageLimit();
    }

    public void setWallTimelineStorageLimit(int storageLimit) {
        wallTimeline.setStorageLimit(storageLimit);
    }

    public Redis getFriendsTimelineRedis() {
        return friendsTimeline.getRedis();
    }

    public void setFriendsTimelineRedis(Redis redis) {
        friendsTimeline.setRedis(redis);
    }

    public int getFriendsTimelineExpireSeconds() {
        return friendsTimeline.getExpireSeconds();
    }

    public void setFriendsTimelineExpireSeconds(int expireSeconds) {
        friendsTimeline.setExpireSeconds(expireSeconds);
    }

    public int getFriendsTimelineLimit() {
        return friendsTimeline.getLimit();
    }

    public void setFriendsTimelineLimit(int limit) {
        friendsTimeline.setLimit(limit);
    }

    public Redis getPublicTimelineRedis() {
        return publicTimeline.getRedis();
    }

    public void setPublicTimelineRedis(Redis redis) {
        publicTimeline.setRedis(redis);
    }

    public int getPublicTimelineLimit() {
        return publicTimeline.getLimit();
    }

    public void setPublicTimelineLimit(int limit) {
        publicTimeline.setLimit(limit);
    }

    public IgnoreLogic getIgnore() {
        return ignore;
    }

    public void setIgnore(IgnoreLogic ignore) {
        this.ignore = ignore;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Map<Integer, String> getTypeIconUrls() {
        return typeIconUrls;
    }

    public void setTypeIconUrls(Map<Object, String> typeIconUrls) {
        if (typeIconUrls == null) {
            this.typeIconUrls = null;
        } else if (typeIconUrls.isEmpty()) {
            this.typeIconUrls = new HashMap<Integer, String>();
        } else {
            HashMap<Integer, String> m = new HashMap<Integer, String>();
            for (Map.Entry<Object, String> e : typeIconUrls.entrySet()) {
                Object k = e.getKey();
                String v = e.getValue();

                if (k instanceof Number) {
                    m.put(((Number) k).intValue(), v);
                } else {
                    int k1 = ((Number) ClassHelper.getConstant(ObjectUtils.toString(k), -1)).intValue();
                    if (k1 > 0)
                        m.put(k1, v);
                }
            }
            this.typeIconUrls = m;
        }
    }


    @Override
    public Post createPost(Context ctx, Post post0) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "createPost",
                ctx, "post", post0);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("post", post0);
            Post post = post0.copy();
            HookHelper.before(createPostHooks, ctx, post);

            //TODO add photo part

            /*String m = post.getMessage();

            final String regex = "(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\!\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\!\\@?^=%&amp;/~\\+#])?";
            Pattern pat2 = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher2 = pat2.matcher(m);
            String url = "";
            if (!matcher2.find()) {
                //TODO post
            } else {
                url = matcher2.group();
                //TODO sharelink
            }

            matcher2.reset();*/

            if(ctx.hasSession("return") && (Boolean)ctx.getSession("return","false"))
                return post;

            Post post1 = db.createStream(ctx, post);

            Target postTarget = post.getPostTarget();

            String[] attachmentIds = post.getAttachmentIds();
            if (ArrayUtils.isNotEmpty(attachmentIds) && post.getType() != Post.POST_LINK) {
                ArrayList<ConversationBase> convs = new ArrayList<ConversationBase>();
                convs.add(new ConversationBase(postTarget, Actions.SHARE));
                for (String attachmentId : attachmentIds) {
                    convs.add(new ConversationBase(Target.parseCompatibleString(attachmentId), Actions.SHARE_ATTACHMENTS));
                }
                conversation.create(ctx, convs.toArray(new ConversationBase[convs.size()]));
            } else {
                conversation.create(ctx, new ConversationBase(postTarget, Actions.SHARE));
            }

            if (CollectionUtils.isNotEmpty(post.getAddTo())) {
                for (PeopleId peopleId : post.getAddTo()) {
                    conversation.create(Context.createForViewer(peopleId.getIdAsLong()), new ConversationBase(postTarget, Actions.ADDTO));
                }
            }

            if (CollectionUtils.isNotEmpty(post.getTo())) {
                for (PeopleId peopleId : post.getTo()) {
                    conversation.create(Context.createForViewer(peopleId.getIdAsLong()), new ConversationBase(postTarget, Actions.TO));
                }
            }
            // timeline
            distributeTimeline(ctx, post);
            OpLine.appends(ctx, createPostOperations(ctx, post1));

            HookHelper.after(createPostHooks, ctx, post);

            //TODO call OPline to send notification and email
            LC.endCall();
            return post1;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    private static Operation[] createPostOperations(Context ctx, Post post) {
        long now = DateHelper.nowMillis();
        Operations opers = new Operations();
        opers.add(Operation.newOperation(ctx, now, Actions.CREATE, post.toJson(null, false), Target.forPost(post.getPostId())));
        PeopleIds to = post.getTo();
        PeopleIds addTo = post.getAddTo();
        if (CollectionUtils.isNotEmpty(to))
            opers.add(Operation.newOperation(ctx, now, Actions.TO, "", to.toIdArray()));
        if (CollectionUtils.isNotEmpty(addTo))
            opers.add(Operation.newOperation(ctx, now, Actions.ADDTO, "", addTo.toIdArray()));
        return opers.toOperationsArray();
    }

    private void distributeTimeline(Context ctx, Post post) {
        // user timeline
        outboxTimeline.add(ctx, PeopleId.user(ctx.getViewer()), TimelineEntry.create(post));

        // wall timeline
        wallTimeline.add(ctx, PeopleId.user(ctx.getViewer()), TimelineEntry.create(post));
        PeopleIds to = post.getToAndAddto();
        for (PeopleId friendId : to) {
            wallTimeline.add(ctx, friendId, TimelineEntry.create(post));
        }

        // follower's friend timeline
        final int COUNT_IN_PAGE = 1000;
        int followerCount = friend.getFollowersCount(ctx, PeopleId.user(ctx.getViewer()));
        int pageCount = followerCount / COUNT_IN_PAGE + 1;
        for (int i = 0; i < pageCount; i++) {
            long[] followerIds = friend.getFollowers(ctx, PeopleId.user(ctx.getViewer()), new Page(i, COUNT_IN_PAGE));
            for (long followerId : followerIds)
                friendsTimeline.add(ctx, PeopleId.user(followerId), TimelineEntry.create(post));
        }

        // public timeline
        publicTimeline.add(ctx, PeopleId.user(ctx.getViewer()), TimelineEntry.create(post));
    }

    @Override
    public boolean destroyPosts(Context ctx, long... postIds) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "destroyPosts",
                ctx, "postIds", postIds);
        boolean b = false;
        try {
            ParamChecker.notNull("ctx", ctx);
            if (ArrayUtils.isEmpty(postIds)) {
                return false;
            }
            for (long postId : postIds) {
                Post post = this.getPost(ctx, null, postId);
                HookHelper.before(destroyPostHooks, ctx, post);
                b = db.destroyPosts(ctx, postId);
                OpLine.append(ctx, Actions.DESTROY, "", Target.forPost(postId));
                HookHelper.after(destroyPostHooks, ctx, post);
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
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "updatePost", ctx, "post", post0);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("post", post0);

            Post post = post0.copy();
            HookHelper.before(updatePostHooks, ctx, post);
            boolean b = db.updateStream(ctx, post);
            OpLine.append(ctx, Actions.UPDATE, post.toJson(null, false), Target.forPost(post.getPostId()));
            HookHelper.after(updatePostHooks, ctx, post);

            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasPost(Context ctx, long postId) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "updatePost", ctx, "postId", postId);

        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = db.hasAllPost(ctx, postId);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAllPosts(Context ctx, long... postIds) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "updatePost", ctx, "postIds", postIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = db.hasAllPost(ctx, postIds);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasAnyPosts(Context ctx, long... postIds) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "updatePost", ctx, "postIds", postIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            boolean b = db.hasAnyPost(ctx, postIds);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts getPosts(Context ctx, String[] expCols, long... postIds) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getPosts", ctx, "postIds", postIds);

        try {
            ParamChecker.notNull("ctx", ctx);
            Posts posts = db.getPosts(ctx, postIds);
            Map<Long, Post> map = new HashMap<Long, Post>();

            // sort
            Posts r = new Posts();
            for (Post post : posts) {
                map.put(post.getPostId(), post);
            }
            for (Long l : postIds) {
                if (map.containsKey(l))
                    r.add(map.get(l));
            }

            expand(ctx, expCols, r);
            LC.endCall();
            return r;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Post getPost(Context ctx, String[] expCols, long postId) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getPost", ctx, "postId", postId);
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
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getUserTimeline", ctx,
                "userId", userId, "filter", filter, "page", page);
        try {
            ParamChecker.notNull("ctx", ctx);
            AccountHelper.checkUser(account, ctx, userId);

            TimelineResult tr = outboxTimeline.get(ctx, PeopleId.user(userId), filter, page);
            Posts posts = getPostsByTimeline(ctx, expCols, tr.timeline);
            filterIgnoredPosts(ctx, posts);
            LC.endCall();
            return posts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts getWallTimeline(Context ctx, long userId, PostFilter filter, String[] expCols, Page page) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getWallTimeline", ctx,
                "userId", userId, "filter", filter, "page", page);
        try {
            ParamChecker.notNull("ctx", ctx);
            AccountHelper.checkUser(account, ctx, userId);

            TimelineResult tr = wallTimeline.get(ctx, PeopleId.user(userId), filter, page);
            Posts posts = getPostsByTimeline(ctx, expCols, tr.timeline);
            filterIgnoredPosts(ctx, posts);
            LC.endCall();
            return posts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts getFriendsTimeline(Context ctx, long userId, PostFilter filter, String[] expCols, Page page) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getFriendsTimeline", ctx,
                "userId", userId, "filter", filter, "page", page);
        try {
            ParamChecker.notNull("ctx", ctx);

            TimelineResult tr = friendsTimeline.get(ctx, PeopleId.user(userId), filter, page);
            Posts posts = getPostsByTimeline(ctx, expCols, tr.timeline);
            filterIgnoredPosts(ctx, posts);
            LC.endCall();
            return posts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts getPublicTimeline(Context ctx, PostFilter filter, String[] expCols, int count) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getPublicTimeline", ctx,
                "filter", filter, "expCols", expCols, "count", count);

        try {
            ParamChecker.notNull("ctx", ctx);
            TimelineResult tr = publicTimeline.get(ctx, PeopleId.user(ctx.getViewer()), filter, new Page(0, count));
            Posts posts = getPostsByTimeline(ctx, expCols, tr.timeline);
            filterIgnoredPosts(ctx, posts);
            LC.endCall();
            return posts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts getPublicTimeline(Context ctx, PostFilter filter, String[] expCols, Page page) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getPublicTimeline", ctx,
                "filter", filter, "expCols", expCols, "page", page);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("page", page);
            long[] postIds = db.getPublicTimelinePostIds(ctx, filter, page);
            Posts posts = getPosts(ctx, expCols, postIds);
            LC.endCall();
            return posts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts search(Context ctx, String text, PostFilter filter, String[] expCols, Page page) {
        // TODO: xx
        throw new UnsupportedOperationException();
    }

    private Posts getPostsByTimeline(Context ctx, String[] expCols, TimelineEntries te) {
        return getPosts(ctx, expCols, te.getPostIds());
    }

    private void filterIgnoredPosts(Context ctx, Posts posts) {
        Target[] ignored = ignore.getIgnored(ctx, ctx.getViewer(), Features.STREAM);
        Posts filtered = new Posts();
        for (Post post : posts) {
            if (filterIgnored(post, ignored))
                filtered.add(post);
        }
        posts.clear();
        posts.addAll(filtered);
    }

    private boolean filterIgnored(Post post, Target[] ignored) {
        for (Target target : ignored) {
            switch (target.type) {
                case Target.POST: {
                    if (post.getPostId() == target.getIdAsLong())
                        return false;
                }

                case Target.USER: {
                    if (post.getSourceId() == target.getIdAsLong())
                        return false;
                }
            }
        }
        return true;
    }

//    public List<String> getEmails(String fromMentions) throws UnsupportedEncodingException {
//        List<String> outEmailList = new ArrayList<String>();
//        if (fromMentions.trim().length() > 0) {
//            List<String> m = StringHelper.splitList(fromMentions, ",", true);
//            for (String s : m) {
//                if (s.startsWith("*")) {
//                    String b = s.substring(1, s.length());
//                    if (b.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
//                        outEmailList.add(b);
//                    }
//                }
//            }
//        }
//        return outEmailList;
//    }
//
//    public List<String> getPhones(String fromMentions) throws UnsupportedEncodingException {
//        List<String> outPhoneList = new ArrayList<String>();
//        if (fromMentions.trim().length() > 0) {
//            List<String> m = StringHelper.splitList(fromMentions, ",", true);
//            for (String s : m) {
//                if (s.startsWith("*")) {
//                    String b = s.substring(1, s.length());
//                    if (b.matches("(13[\\d]{9})")) {
//                        outPhoneList.add(b);
//                    }
//                }
//            }
//        }
//        return outPhoneList;
//    }

    //ignore the post and the user 
    private Posts ignoreStream(Context ctx, Posts posts) {
        Posts postList = new Posts();
        Target[] targets = ignore.getIgnored(ctx, ctx.getViewer(), Features.STREAM);
        for (Post post : posts) {
            for (Target target : targets) {
                if (target.type == Target.USER) {
                    if (!String.valueOf(post.getSourceId()).equals(target.id)) {
                        postList.add(post);
                    }
                } else if (target.type == Target.POST) {
                    if (!String.valueOf(post.getPostId()).equals(target.id)) {
                        postList.add(post);
                    }
                }
            }
        }
        return postList;
    }

    @Override
    public Map<Long, Integer> getUserTimelineCounts(Context ctx, long... userIds) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getUserTimelineCounts", ctx,
                "userIds", userIds);
        try {
            HashMap<Long, Integer> m = new HashMap<Long, Integer>();
            for (long userId : userIds) {
                int n = outboxTimeline.getCount(ctx, PeopleId.user(userId));
                m.put(userId, n);
            }
            LC.endCall();
            return m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public int getUserTimelineCount(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getUserTimelineCount", ctx,
                "userId", userId);
        try {
            int n = outboxTimeline.getCount(ctx, PeopleId.user(userId));
            LC.endCall();
            return n;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Long, Integer> getWallTimelineCounts(Context ctx, long... userIds) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getWallTimelineCounts", ctx,
                "userIds", userIds);
        try {
            HashMap<Long, Integer> m = new HashMap<Long, Integer>();
            for (long userId : userIds) {
                int n = wallTimeline.getCount(ctx, PeopleId.user(userId));
                m.put(userId, n);
            }
            LC.endCall();
            return m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public int getWallTimelineCount(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getWallTimelineCount", ctx,
                "userId", userId);
        try {
            int n = wallTimeline.getCount(ctx, PeopleId.user(userId));
            LC.endCall();
            return n;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Posts getPostsInConversation(Context ctx, long userId, int reason, String[] expCols, Page page) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getPostsInConversation", ctx,
                "userId", userId, "reason", reason, "expCols", expCols, "page", page);
        try {
            Conversations convs = conversation.findByUser(ctx, null, new int[]{reason}, page, userId);
            long[] postIds = convs.getTargetIdsAsLong(Target.POST);
            Posts posts = getPosts(ctx, expCols, postIds);
            LC.endCall();
            return posts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Target[] getUserTimelineAttachments(Context ctx, long userId, PostFilter filter, Page page) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getUserTimelineAttachments", ctx,
                "userId", userId, "filter", filter, "page", page);
        try {
            Posts posts = getUserTimeline(ctx, userId, filter, Post.STANDARD_COLUMNS, page);
            LinkedHashSet<Target> l = new LinkedHashSet<Target>();
            for (Post post : posts) {
                String[] attachmentIds = post.getAttachmentIds();
                if (ArrayUtils.isNotEmpty(attachmentIds))
                    Collections.addAll(l, Target.fromCompatibleStringArray(attachmentIds));
            }
            return l.toArray(new Target[l.size()]);
        } catch (RuntimeException e) {
            LC.endCall();
            throw e;
        }
    }

    @Override
    public PostAttachments getWallTimelineAttachments(Context ctx, long userId, PostFilter filter, boolean includeSelf, Page page) {
        final LogCall LC = LogCall.startCall(L, StreamImpl.class, "getWallTimelineAttachments", ctx,
                "userId", userId, "filter", filter, "includeSelf", includeSelf, "page", page);
        try {
            Posts posts;
            if (includeSelf) {
                posts = getWallTimeline(ctx, userId, filter, Post.STANDARD_COLUMNS, page);
            } else {
                ParamChecker.notNull("ctx", ctx);
                AccountHelper.checkUser(account, ctx, userId);

                final int PAGE_COUNT = 1000;
                long max = page.getEnd();

                int allCount = wallTimeline.getCount(ctx, PeopleId.user(userId));
                int pageCount;
                if (allCount < PAGE_COUNT)
                    pageCount = 1;
                else
                    pageCount = allCount % PAGE_COUNT == 0 ? allCount / PAGE_COUNT : allCount / PAGE_COUNT + 1;

                posts = new Posts();
                for (int i = 0; i < pageCount; i++) {
                    Posts posts0 = getWallTimeline(ctx, userId, filter, Post.STANDARD_COLUMNS, Page.of(i, PAGE_COUNT));
                    for (Post post0 : posts0) {
                        long sourceId = post0.getSourceId();
                        if (sourceId > 0 && sourceId != userId)
                            posts.add(post0);
                    }
                    if (posts.size() >= max)
                        break;
                }
                page.retains(posts);
            }

            PostAttachments pa = new PostAttachments();
            for (Post post : posts) {
                if (post != null)
                    pa.addAttachments(post);
            }
            return pa;
        } catch (RuntimeException e) {
            LC.endCall();
            throw e;
        }
    }

    @Override
    public void expand(Context ctx, String[] expCols, Posts data) {
        builtinExpansion.expand(ctx, expCols, data);
        ExpansionHelper.expand(expansions, ctx, expCols, data);
    }

    @Override
    public Post expand(Context ctx, String[] expCols, Post post) {
        Posts posts = new Posts(post);
        expand(ctx, expCols, posts);
        return posts.isEmpty() ? null : posts.get(0);
    }

    private static final String[] SOURCE_COLUMNS = {
            User.COL_USER_ID,
            User.COL_NAME,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };

    private static final String[] TO_COLUMNS = {
            User.COL_USER_ID,
            User.COL_NAME,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };

    private static final String[] ADDTO_COLUMNS = {
            User.COL_USER_ID,
            User.COL_NAME,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };

    protected class BuiltinExpansion implements PostExpansion {

        @Override
        public void expand(Context ctx, String[] expCols, Posts data) {
            if (CollectionUtils.isEmpty(data))
                return;

            data.removeSourceless(ctx, account);
            if (data.isEmpty())
                return;

            if (expCols == null || ArrayUtils.contains(expCols, Post.COL_SOURCE))
                expandSource(ctx, data);

            if (expCols == null || ArrayUtils.contains(expCols, Post.COL_TO))
                expandTo(ctx, data);

            if (expCols == null || ArrayUtils.contains(expCols, Post.COL_ADD_TO))
                expandAddTo(ctx, data);

            if (expCols == null || ArrayUtils.contains(expCols, Post.COL_QUOTED_POST))
                expandQuotedPost(ctx, expCols, data);

            if (expCols == null || ArrayUtils.contains(expCols, Post.COL_TYPE_ICON_URL))
                expandTypeIconUrl(data);
        }

        private void expandSource(Context ctx, Posts posts) {
            long[] sourceIds = posts.getSourceIds();
            Users users = account.getUsers(ctx, SOURCE_COLUMNS, sourceIds);
            for (Post post : posts) {
                if (post == null)
                    continue;

                User sourceUser = users.getUser(post.getSourceId());
                String json = sourceUser != null ? sourceUser.toJson(SOURCE_COLUMNS, true) : "{}";
                post.setAddon(Post.COL_SOURCE, Addons.jsonAddonValue(json));
            }
        }

        private void expandTo(Context ctx, Posts posts) {
            PeopleIds to = posts.getTo();
            Users users = account.getUsers(ctx, TO_COLUMNS, to.getUserIds());
            for (Post post : posts) {
                if (post == null)
                    continue;

                PeopleIds to0 = post.getTo();
                long[] subUserIds = to0 != null ? to0.getUserIds() : new long[0];
                Users subUsers = users.getUsers(null, subUserIds);
                String json = subUsers.toJson(TO_COLUMNS, true);
                post.setAddon(Post.COL_TO, Addons.jsonAddonValue(json));
            }
        }

        private void expandAddTo(Context ctx, Posts posts) {
            PeopleIds addTo = posts.getAddTo();
            Users users = account.getUsers(ctx, ADDTO_COLUMNS, addTo.getUserIds());
            for (Post post : posts) {
                if (post == null)
                    continue;

                PeopleIds addTo0 = post.getAddTo();
                Users subUsers = addTo0 != null ? users.getUsers(null, addTo0.getUserIds()) : new Users();
                String json = subUsers.toJson(ADDTO_COLUMNS, true);
                post.setAddon(Post.COL_ADD_TO, Addons.jsonAddonValue(json));
            }
        }

        private void expandQuotedPost(Context ctx, String[] expCols, Posts posts) {
            long[] quoteIds = posts.getQuoteIds();
            if (ArrayUtils.isEmpty(quoteIds)) {
                for (Post post : posts) {
                    if (post != null)
                        post.setAddon(Post.COL_QUOTED_POST, "{}");
                }
            } else {
                Posts quotedPosts = getPosts(ctx, expCols, quoteIds);
                for (Post post : posts) {
                    if (post == null)
                        continue;

                    long quote = post.getQuote();
                    if (quote <= 0)
                        continue;

                    Post quotedPost = quotedPosts.getPost(quote);
                    if (quotedPost == null) {
                        post.setAddon(Post.COL_QUOTED_POST, "{}");
                    } else {
                        String json = quotedPost.toJson(expCols, true);
                        post.setAddon(Post.COL_QUOTED_POST, Addons.jsonAddonValue(json));
                    }
                }
            }
        }

        private void expandTypeIconUrl(Posts posts) {
            for (Post post : posts) {
                if (post == null)
                    continue;

                String typeIconUrl = MapUtils.getString(typeIconUrls, post.getType(), "");
                post.setAddon(Post.COL_TYPE_ICON_URL, typeIconUrl);
            }
        }
    }
}
