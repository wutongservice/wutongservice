package com.borqs.server.pubapi;

import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.feature.favorite.FavoriteLogic;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.like.LikeLogic;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.platform.mq.QueueName;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.web.UserAgent;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;
import org.apache.commons.lang.BooleanUtils;

import java.util.LinkedHashSet;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class StreamApi extends PublicApiSupport {
    private AccountLogic account;
    private StreamLogic stream;
    private FriendLogic friend;
    private LikeLogic like;
    private FavoriteLogic favorite;
    private CommentLogic comment;


    private QueueName postQueue;
    private QueueName updateQueue;
    private QueueName destroyQueue;
    private QueueName commentQueue;
    private QueueName linkQueue;

    public StreamApi() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public StreamLogic getStream() {
        return stream;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    public LikeLogic getLike() {
        return like;
    }

    public void setLike(LikeLogic like) {
        this.like = like;
    }

    public FavoriteLogic getFavorite() {
        return favorite;
    }

    public void setFavorite(FavoriteLogic favorite) {
        this.favorite = favorite;
    }

    public CommentLogic getComment() {
        return comment;
    }

    public void setComment(CommentLogic comment) {
        this.comment = comment;
    }

    public QueueName getPostQueue() {
        return postQueue;
    }

    public void setPostQueue(QueueName postQueue) {
        this.postQueue = postQueue;
    }

    public QueueName getUpdateQueue() {
        return updateQueue;
    }

    public void setUpdateQueue(QueueName updateQueue) {
        this.updateQueue = updateQueue;
    }

    public QueueName getDestroyQueue() {
        return destroyQueue;
    }

    public void setDestroyQueue(QueueName destroyQueue) {
        this.destroyQueue = destroyQueue;
    }

    public QueueName getCommentQueue() {
        return commentQueue;
    }

    public void setCommentQueue(QueueName commentQueue) {
        this.commentQueue = commentQueue;
    }

    public QueueName getLinkQueue() {
        return linkQueue;
    }

    public void setLinkQueue(QueueName linkQueue) {
        this.linkQueue = linkQueue;
    }



    private static final String[] RETURN_POST_COLUMNS = null;

    private static final String[] EXPECT_COLUMNS = {
            Post.COL_DESTROYED_TIME,
    };


    /**
     * 发布一条文本类型的post
     *
     * @remark 如果private属性为true，那么to参数必须存在
     * @group Stream
     * @http-param message stream的内容
     * @http-param to: 收件人
     * @http-param can_comment:true 是否可评论
     * @http-param can_like:true 是否可like
     * @http-param can_quote:true 是否引用
     * @http-param private:false 隐私设置（所选人可见 true 所有人可见 false）
     * @http-param app_data: 附加数据
     * @http-return 发送的post
     * @http-example @post_std0.json
     */
    @Route(url = "/post/post")
    public void createPost(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        if (req.getBoolean("private", false))
            req.checkString("to");

        String message = req.checkString("message");

        long now = DateHelper.nowMillis();
        Post post = new Post(RandomHelper.generateId(now));
        post.setAddTo(PeopleIds.parseAddTo(message));
        post.setTo(PeopleIds.forStringIds(PeopleId.USER, req.getStringArray("to", ",", new String[]{})));
        post.setUpdatedTime(now);
        post.setCreatedTime(now);
        post.setDestroyedTime(0);
        post.setApp(ctx.getApp());
        post.setAttachments("[]"); // TODO: xx?
        post.setPrivate(req.getBoolean("private", false));
        post.setMessage(message);
        post.setCanComment(req.getBoolean("can_comment", true));
        post.setCanLike(req.getBoolean("can_like", true));
        post.setCanQuote(req.getBoolean("can_quote", true));
        post.setType(Post.POST_TEXT); // default type
        UserAgent ua = req.getUserAgent();
        post.setDevice(ua != null ? ua.getDeviceType() : "");
        post.setGeoLocation(ctx.getGeoLocation());
        post.setQuote(0L); // not repost
        post.setSourceId(ctx.getViewer());
        post.setAppData(req.getString("app_data", ""));

        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(linkQueue);
        post = stream.expand(ctx, Post.X_FULL_COLUMNS, post);
        resp.body(RawText.of(post.toJson(RETURN_POST_COLUMNS, true)));
    }


    /**
     * 转发Stream
     *
     * @remark 如果private属性为true，那么to参数必须存在
     * @group Stream
     * @http-param quote 引用 post_id 必填
     * @http-param message stream的内容
     * @http-param to 收件人
     * @http-param can_comment:true 是否可评论
     * @http-param can_like:true 是否可like
     * @http-param can_quote:true 是否引用
     * @http-param private:false 隐私设置（所选人可见 true 所有人可见 false）
     * @http-param app_data 附加数据
     * @http-return 转发过的此条post
     * @http-example @post_std0.json
     */
    @Route(url = "/post/repost")
    public void createRepost(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        long quote = req.checkLong("quote");
        Post quotePost = stream.getPost(ctx, Post.FULL_COLUMNS, quote);
        if (quotePost == null)
            throw new ServerException(E.INVALID_POST, "Invalid quote post id " + quote);

        if (quotePost.getQuote() != 0L)
            throw new ServerException(E.REPETITIVE_REPOST, "Repetitive repost");

        if (BooleanUtils.isNotTrue(quotePost.getCanQuote()))
            throw new ServerException(E.CANNOT_QUOTE, "The post can't be quoted");

        if (req.getBoolean("private", false))
            req.checkString("to");

        String message = req.checkString("message");
        long now = DateHelper.nowMillis();

        Post post = new Post(RandomHelper.generateId(now));
        post.setAddTo(PeopleIds.parseAddTo(message));
        post.setTo(PeopleIds.forStringIds(PeopleId.USER, req.getStringArray("to", ",", new String[]{})));
        post.setUpdatedTime(now);
        post.setCreatedTime(now);
        post.setDestroyedTime(0);
        post.setApp(ctx.getApp());
        post.setAttachments("[]"); // TODO: xx?
        post.setPrivate(req.getBoolean("private", false));
        post.setMessage(Post.makeRepostMessage(message, quotePost));
        post.setCanComment(req.getBoolean("can_comment", true));
        post.setCanLike(req.getBoolean("can_like", true));
        post.setCanQuote(req.getBoolean("can_quote", true));
        post.setType(quotePost.getType()); // TODO: Post.POST_TEXT or quotePost.getType() ?
        UserAgent ua = req.getUserAgent();
        post.setDevice(ua != null ? ua.getDeviceType() : "");
        post.setGeoLocation(ctx.getGeoLocation());
        post.setQuote(quote);
        post.setSourceId(ctx.getViewer());
        post.setAppData(req.getString("app_data", ""));

        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
        post = stream.expand(ctx, Post.X_FULL_COLUMNS, post);
        resp.body(RawText.of(post.toJson(RETURN_POST_COLUMNS, true)));
    }

    /**
     * 删除一条自己发送的post
     *
     * @remark 注意，由于异步删除，所以此方法总是返回true，但是这并不代表删除真正成功，例如试图删除其他人发送的post是不会最终成功的
     * @group Stream
     * @http-param post 要删除的post的id
     * @http-return true
     */
    @Route(url = "/post/destroy")
    public void destroyPost(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long postId = req.checkLong("post");
        new ContextObject(ctx, ContextObject.TYPE_DESTROY, postId).sendThisWith(destroyQueue);
        resp.body(true);
    }

    /**
     * 修改Stream
     *
     * @remark
     * @group Stream
     * @http-param post stream的id
     * @http-param can_comment:true 是否可以被评论
     * @http-param can_quote:true 是否可以被引用
     * @http-param can_like:true 是否可以被like
     * @http-return 更新过的post
     * @http-example @post_std0.json
     */
    @Route(url = "/post/update")
    public void updatePost(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        long postId = req.checkLong("post");
        Post post0 = stream.getPost(ctx, Post.FULL_COLUMNS, postId);
        if (post0 == null)
            throw new ServerException(E.INVALID_POST, "Invalid post " + postId);

        Boolean canComment = req.has("can_comment") ? req.checkBoolean("can_comment") : null;
        Boolean canQuote = req.has("can_quote") ? req.checkBoolean("can_quote") : null;
        Boolean canLike = req.has("can_like") ? req.checkBoolean("can_like") : null;

        Post post = new Post(postId);
        if (canComment != null) {
            post.setCanComment(canComment);
            post0.setCanComment(canComment);
        }

        if (canQuote != null) {
            post.setCanQuote(canQuote);
            post0.setCanQuote(canQuote);
        }

        if (canLike != null) {
            post.setCanLike(canLike);
            post0.setCanLike(canLike);
        }

        new ContextObject(ctx, ContextObject.TYPE_UPDATE, post).sendThisWith(updateQueue);
        post.setSourceId(ctx.getViewer());
        post = stream.expand(ctx, Post.FULL_COLUMNS, post);
        resp.body(RawText.of(post.toJson(RETURN_POST_COLUMNS, true)));
    }


    /**
     * 返回指定ID的post列表
     *
     * @group Stream
     * @http-param posts 逗号分隔的指定post的id
     * @http-param cols:@std 逗号分隔的列名，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-return post列表
     * @http-example @post_std.json
     */
    @Route(url = "/post/get")
    public void getPosts(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        String[] cols = Post.expandColumns(req.getStringArrayExcept("cols", ",",
                Post.STANDARD_COLUMNS, EXPECT_COLUMNS));
        Posts posts = stream.getPosts(ctx, cols, req.checkLongArray("posts", ","));

        resp.body(RawText.of(posts.toJson(cols, true)));
    }

    private static Integer appFromReq(Context ctx, Request req) {
        return (req.getBoolean("base_app", false) && ctx.getApp() != App.APP_NONE)
                ? ctx.getApp()
                : App.APP_NONE;
    }

    /**
     * 获取公共timeline
     *
     * @group Stream
     * @login n
     * @http-param types: 指定的post类型，如不指定，则代表所有类型
     * @http-param count:50 查看的条数，最大值100
     * @http-param base_app:false 为true则查看紧此app的post，否则查看所有app发送的post
     * @http-param cols:@std 逗号分隔的post列，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-return post列表
     * @http-example @post_std.json
     */
    @Route(url = "/timeline/public")
    public void getPublicTimeline(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        // params
        int types = req.getInt("types", Post.ALL_POST_TYPES);
        int count = req.getInt("count", 50);
        if (count > 100)
            count = 100;

        int app = appFromReq(ctx, req);
        String[] cols = Post.expandColumns(req.getStringArrayExcept("cols", ",",
                Post.STANDARD_COLUMNS, EXPECT_COLUMNS));

        // get
        PostFilter filter = new PostFilter(types, app, 0, 0, null);
        Posts posts = stream.getPublicTimeline(ctx, filter, cols, count);
        resp.body(RawText.of(posts.toJson(cols, true)));
    }

    /**
     * 获取自己或者其他用户的timeline
     *
     * @group Stream
     * @login n
     * @http-param user: 要查看的此人的timeline，此参数可缺失，如果缺失，表示查看登录用户自己的timeline，如果此时用户未登录，则返回错误
     * @http-param types: 指定的post类型，如不指定，则代表所有类型
     * @http-param min:0 最大的post_id，为0代表无限制
     * @http-param max:0 最小的post_id，为0代表无限制
     * @http-param base_app:false 为true则查看紧此app的post，否则查看所有app发送的post
     * @http-param cols:@std 逗号分隔的post列，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-param page:0 查看页码
     * @http-param count:20 每页查看的数量，最大50
     * @http-return post列表
     * @http-example @post_std.json
     */
    @Route(url = "/timeline/user")
    public void getUserTimeline(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        // params
        long userId = req.getLong("user", ctx.getViewer());
        AccountHelper.checkUser(account, ctx, userId);
        long max = req.getLong("max", 0);
        long min = req.getLong("min", 0);
        int types = req.getInt("types", Post.ALL_POST_TYPES);
        int app = appFromReq(ctx, req);
        String[] cols = Post.expandColumns(req.getStringArrayExcept("cols", ",",
                Post.STANDARD_COLUMNS, EXPECT_COLUMNS));
        Page page = req.getPage(20, 50);

        // get
        PostFilter filter = new PostFilter(types, app, min, max, null);
        Posts posts = stream.getUserTimeline(ctx, userId, filter, cols, page);
        resp.body(RawText.of(posts.toJson(cols, true)));
    }

    /**
     * 获取自己timeline
     *
     * @group Stream
     * @http-param types: 指定的post类型，如不指定，则代表所有类型
     * @http-param min:0 最大的post_id，为0代表无限制
     * @http-param max:0 最小的post_id，为0代表无限制
     * @http-param base_app:false 为true则查看紧此app的post，否则查看所有app发送的post
     * @http-param cols:@std 逗号分隔的post列，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-param page:0 查看页码
     * @http-param count:20 每页查看的数量，最大50
     * @http-return post列表
     * @http-example @post_std.json
     */
    @Route(url = "/timeline/my")
    public void getMyTimeline(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        // params
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        long max = req.getLong("max", 0);
        long min = req.getLong("min", 0);
        int types = req.getInt("types", Post.ALL_POST_TYPES);
        int app = appFromReq(ctx, req);
        String[] cols = Post.expandColumns(req.getStringArrayExcept("cols", ",",
                Post.STANDARD_COLUMNS, EXPECT_COLUMNS));
        Page page = req.getPage(20, 50);

        // get
        PostFilter filter = new PostFilter(types, app, min, max, null);
        Posts posts = stream.getUserTimeline(ctx, ctx.getViewer(), filter, cols, page);
        resp.body(RawText.of(posts.toJson(cols, true)));
    }

    /**
     * 登录用户获取自己及自己好友的timeline
     *
     * @remark 如果不指定要查看圈子的timeline，则结果中包含我自己发送的post，否则不包含
     * @group Stream
     * @http-param types: 指定的post类型，如不指定，则代表所有类型
     * @http-param min:0 最大的post_id，为0代表无限制
     * @http-param max:0 最小的post_id，为0代表无限制
     * @http-param circle:1 只查看此圈子中好友的timeline，如果此参数不填写或者为1，则返回所有好友的timeline。
     * @http-param base_app:false 为true则查看紧此app的post，否则查看所有app发送的post
     * @http-param cols:@std 逗号分隔的post列，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-param page:0 查看页码
     * @http-param count:20 每页查看的数量，最大50
     * @http-return post列表
     * @http-example @post_std.json
     */
    @Route(url = {"/timeline/friends", "/timeline/home"})
    public void getFriendsTimeline(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        // params
        long max = req.getLong("max", 0);
        long min = req.getLong("min", 0);
        int types = req.getInt("types", Post.ALL_POST_TYPES);
        int app = appFromReq(ctx, req);
        String[] cols = Post.expandColumns(req.getStringArrayExcept("cols", ",",
                Post.STANDARD_COLUMNS, EXPECT_COLUMNS));
        Page page = req.getPage(20, 50);

        // get
        PostFilter filter = new PostFilter(types, app, min, max, null);

        // calc friendIds
        PeopleIds friendIds;
        int circleId = req.getInt("circle", Circle.CIRCLE_ALL_FRIEND);
        if (circleId == Circle.CIRCLE_ALL_FRIEND) {
            friendIds = friend.getFriends(ctx, ctx.getViewer());
            friendIds.add(PeopleId.user(ctx.getViewer())); // Add me!
        } else {
            friendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), circleId);
        }
        filter.friendIds = friendIds.toSet(new LinkedHashSet<PeopleId>());

        Posts posts = stream.getFriendsTimeline(ctx, ctx.getViewer(), filter, cols, page);
        resp.body(RawText.of(posts.toJson(cols, true)));
    }

    /**
     * 获取自己或者某个用户的wall timeline
     *
     * @group Stream
     * @login n
     * @http-param types: 指定的post类型，如不指定，则代表所有类型
     * @http-param user: 要查看的此人的timeline，此参数可缺失，如果缺失，表示查看登录用户自己的timeline，如果此时用户未登录，则返回错误
     * @http-param min:0 最大的post_id，为0代表无限制
     * @http-param max:0 最小的post_id，为0代表无限制
     * @http-param base_app:false 为true则查看紧此app的post，否则查看所有app发送的post
     * @http-param cols:@std 逗号分隔的post列，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-param page:0 查看页码
     * @http-param count:20 每页查看的数量，最大50
     * @http-return post列表
     * @http-example @post_std.json
     */
    @Route(url = "/timeline/wall")
    public void getWallTimeline(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        // params
        long userId = req.getLong("user", ctx.getViewer());
        AccountHelper.checkUser(account, ctx, userId);
        long max = req.getLong("max", 0);
        long min = req.getLong("min", 0);
        int types = req.getInt("types", Post.ALL_POST_TYPES);
        int app = appFromReq(ctx, req);
        String[] cols = Post.expandColumns(req.getStringArrayExcept("cols", ",",
                Post.STANDARD_COLUMNS, EXPECT_COLUMNS));
        Page page = req.getPage(20, 50);

        // get
        PostFilter filter = new PostFilter(types, app, min, max, null);
        Posts posts = stream.getWallTimeline(ctx, userId, filter, cols, page);
        resp.body(RawText.of(posts.toJson(cols, true)));
    }


    private void getPostsInConversion(Request req, Response resp, int targetAction) {
        Context ctx = checkContext(req, true);

        // params
        String[] cols = Post.expandColumns(req.getStringArrayExcept("cols", ",",
                Post.STANDARD_COLUMNS, EXPECT_COLUMNS));
        Page page = req.getPage(20, 50);

        // get
        Posts posts = stream.getPostsInConversation(ctx, ctx.getViewer(), targetAction, cols, page);
        resp.body(RawText.of(posts.toJson(cols, true)));
    }


    // like

    /**
     * 赞一个post
     *
     * @remark 如果这个post的can_like为false，则无法对此post进行like
     * @group Stream
     * @http-param post 要赞的post_id
     * @http-return true
     */
    @Route(url = "/post/like")
    public void likePost(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long postId = req.checkLong("post");

        Post post = stream.getPost(ctx, Post.STANDARD_COLUMNS, postId);
        if (post == null)
            throw new ServerException(E.INVALID_POST, "Illegal post %s", postId);

        if (BooleanUtils.isNotTrue(post.getCanLike()))
            throw new ServerException(E.CANNOT_LIKE, "The post can't be like");

        // TODO: post like to an queue!!!

        Target postTarget = Target.of(Target.POST, postId);
        like.like(ctx, postTarget);
        resp.body(true);
    }

    /**
     * 去赞一个post
     *
     * @group Stream
     * @http-param post 要去赞的post_id
     * @http-return true
     */
    @Route(url = "/post/unlike")
    public void unlikePost(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        // TODO: post unlike to an queue
        long postId = req.checkLong("post");
        Target postTarget = Target.of(Target.POST, postId);
        like.unlike(ctx, postTarget);
        resp.body(true);
    }

    /**
     * 获取我like过的posts
     *
     * @group Stream
     * @http-param cols:@std 逗号分隔的post列，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-param page:0 查看页码
     * @http-param count:20 每页查看的数量，最大50
     * @http-return 我like过的post列表
     * @http-example TODO: xx
     */
    @Route(url = "/post/get_liked")
    public void getLikedPosts(Request req, Response resp) {
        getPostsInConversion(req, resp, Actions.LIKE);
    }


    // favorite
    /**
     * 收藏一个post
     *
     * @group Stream
     * @http-param post 要收藏的post_id
     * @http-return true
     */
    @Route(url = "/post/favorite")
    public void favoritePost(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        long postId = req.checkLong("post");
        boolean hasPost = stream.hasPost(ctx, postId);
        if (!hasPost)
            throw new ServerException(E.INVALID_POST, "Illegal post %s", postId);

        // TODO: post favorite to an queue!!!

        Target postTarget = Target.of(Target.POST, postId);
        favorite.favorite(ctx, postTarget);
        resp.body(true);
    }

    /**
     * 取消收藏一个post
     *
     * @group Stream
     * @http-param post 要取消收藏的post_id
     * @http-return true
     */
    @Route(url = "/post/unfavorite")
    public void unfavoritePost(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        // TODO: post favorite to an queue!!!
        long postId = req.checkLong("post");
        Target postTarget = Target.of(Target.POST, postId);
        favorite.unfavorite(ctx, postTarget);
        resp.body(true);
    }

    /**
     * 获取我收藏的posts
     *
     * @group Stream
     * @http-param cols:@std 逗号分隔的post列，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-param page:0 查看页码
     * @http-param count:20 每页查看的数量，最大50
     * @http-return 我收藏过的post列表
     * @http-example @post_std.json
     */
    @Route(url = "/post/get_favorite")
    public void getFavoritePosts(Request req, Response resp) {
        getPostsInConversion(req, resp, Actions.FAVORITE);
    }


    /**
     * 评论一个post。
     *
     * @remark 如果一个post的can_comment为false，评论这个post将失败
     * @group Stream
     * @http-param post 要评论的post的id
     * @http-param message 评论的信息
     * @http-param can_like:true 这个评论是否允许被like
     * @http-return 这个comment
     * @http-example TODO:Xx
     */
    @Route(url = "/post/comment")
    public void commentPost(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long postId = req.checkLong("post");
        String message = req.checkString("message");
        boolean canLike = req.getBoolean("can_like", true);

        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        Post post = stream.getPost(ctx, Post.STANDARD_COLUMNS, postId);
        if (post == null)
            throw new ServerException(E.INVALID_POST, "Illegal post %s", postId);

        if (BooleanUtils.isNotTrue(post.getCanComment()))
            throw new ServerException(E.CANNOT_COMMENT, "The post can't be comment");

        long now = DateHelper.nowMillis();
        Comment c = new Comment(RandomHelper.generateId(now));
        c.setTarget(Target.of(Target.POST, postId));
        c.setAddTo(PeopleIds.parseAddTo(message));
        c.setCanLike(canLike);
        c.setCommenterId(ctx.getViewer());
        c.setCreatedTime(now);
        c.setDestroyedTime(0);
        c.setDevice(ctx.getRawUserAgent()); // TODO: ?
        c.setMessage(message);

        new ContextObject(ctx, ContextObject.TYPE_CREATE, c).sendThisWith(commentQueue);

        c = comment.expand(ctx, Comment.FULL_COLUMNS, c);
        resp.body(RawText.of(c.toJson(Comment.FULL_COLUMNS, true)));
    }

    /**
     * 获取我comment过的posts
     *
     * @group Stream
     * @http-param cols:@std 逗号分隔的post列，其值可以为${com.borqs.server.platform.feature.stream.Post.DISPLAY_COLUMNS}
     * @http-param page:0 查看页码
     * @http-param count:20 每页查看的数量，最大50
     * @http-return 我comment过的post列表
     * @http-example @post_std.json
     */
    @Route(url = "/post/get_commented")
    public void getCommentedPosts(Request req, Response resp) {
        getPostsInConversion(req, resp, Actions.COMMENT);
    }
}
