package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.compatible.CompatiblePost;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.SpecificPeoples;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.feature.favorite.FavoriteLogic;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.like.LikeLogic;
import com.borqs.server.platform.feature.link.LinkEntities;
import com.borqs.server.platform.feature.link.LinkEntity;
import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.AlbumTypes;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.feature.photo.PhotoLogic;
import com.borqs.server.platform.feature.status.Status;
import com.borqs.server.platform.feature.status.StatusLogic;
import com.borqs.server.platform.feature.stream.*;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.platform.mq.QueueName;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.*;
import com.borqs.server.platform.web.UserAgent;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashSet;


@IgnoreDocument
public class Stream1Api extends PublicApiSupport {

    private AccountLogic account;
    private StatusLogic status;
    private StreamLogic stream;
    private FriendLogic friend;
    private LikeLogic like;
    private FavoriteLogic favorite;
    private CommentLogic comment;
    private PhotoLogic photo;

    private QueueName postQueue;
    private QueueName updateQueue;
    private QueueName destroyQueue;
    private QueueName commentQueue;

    private SFS photoSFS;

    public Stream1Api() {
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

    public StatusLogic getStatus() {
        return status;
    }

    public void setStatus(StatusLogic status) {
        this.status = status;
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

    public PhotoLogic getPhoto() {
        return photo;
    }

    public void setPhoto(PhotoLogic photo) {
        this.photo = photo;
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

    public SFS getPhotoSFS() {
        return photoSFS;
    }

    public void setPhotoSFS(SFS photoSFS) {
        this.photoSFS = photoSFS;
    }

    @Route(url = "/post/create")
    public void createPost(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        if (req.getBoolean("secretly", false))
            req.checkString("mentions");

        String message = req.checkString("msg");

        long now = DateHelper.nowMillis();
        Post post = new Post(RandomHelper.generateId(now));
        post.setType(req.getInt("type", Post.POST_TEXT));
        post.setAddTo(PeopleIds.parseAddTo(message));
        post.setTo(PeopleIds.forStringIds(PeopleId.USER, req.getStringArray("mentions", ",", new String[]{})));
        post.setUpdatedTime(now);
        post.setCreatedTime(now);
        post.setDestroyedTime(0);
        post.setApp(ctx.getApp());
        post.setAttachments(req.getString("attachments", "[]"));
        post.setPrivate(req.getBoolean("secretly", false));
        post.setMessage(message);
        post.setCanComment(req.getBoolean("can_comment", true));
        post.setCanLike(req.getBoolean("can_like", true));
        post.setCanQuote(req.getBoolean("can_reshare", true));
        post.setDevice(req.getRawUserAgent());
        post.setGeoLocation(ctx.getGeoLocation());
        post.setQuote(0L); // not repost
        post.setSourceId(ctx.getViewer());
        post.setAppData(req.getString("app_data", ""));
        post.setLocation(ctx.getLocation());

        // handle photo
        handlePhoto(ctx, req, post);
        handleApk(ctx, req, post);

        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
        post = stream.expand(ctx, Post.X_FULL_COLUMNS, post);
        resp.body(RawText.of(CompatiblePost.postToJson(post, CompatiblePost.V1_FULL_COLUMNS, true)));
    }

    private void handlePhoto(Context ctx, Request req, Post post) {
        FileItem photoFileItem = req.getFile("photo_image");
        if (photoFileItem == null || StringUtils.isBlank(photoFileItem.getName()))
            return;

        long albumId = req.getLong("album_id", 0);
        Album album;
        if (albumId > 0) {
            album = CollectionsHelper.getFirstItem(photo.getAlbums(ctx, false, albumId), null);
        } else {
            album = CollectionsHelper.getFirstItem(photo.getUserAlbum(ctx, ctx.getViewer(), AlbumTypes.ALBUM_TYPE_STREAM, false), null);
        }
        if (album == null) {
            //if album is null then create default album
            Album albumDefault = new Album();

            albumDefault.setAlbum_id(DateHelper.nowMillis());
            albumDefault.setAlbum_type(AlbumTypes.ALBUM_TYPE_STREAM);
            albumDefault.setUser_id(ctx.getViewer());
            albumDefault.setTitle("User default Album");
            albumDefault.setCover_photo_id(0);
            albumDefault.setPrivacy(true);
            albumDefault.setCan_upload(0);
            albumDefault.setNum_photos(0);

            album = photo.createAlbum(ctx, albumDefault);
        }
        //throw new ServerException(E.INVALID_ALBUM, "Invalid album");

        albumId = album.getAlbum_id();


        Photo p = null;
        try {
            p = photo.uploadPhoto(ctx, albumId, photoFileItem);
        } catch (Exception e) {
            throw new ServerException(E.SAVE_PHOTO, "Save photo error", e);
        }
        if (p == null)
            throw new ServerException(E.SAVE_PHOTO, "Save photo error");

        post.setAttachmentId(Post.POST_PHOTO + ":" + p.getPhoto_id());

//            long photoId = RandomHelper.generateId(now);
//            try {
//                PhotoHelper.saveUploadedPhoto(ctx, photoSFS, photoFileItem, ctx.getViewer(), albumId, photoId);
//            } catch (IOException e) {
//                throw new ServerException(E.IMAGE, "Save image error", e);
//            }
    }

    private void handleApk(Context ctx, Request req, Post post) {
        if ((post.getType() & Post.POST_APK) == 0)
            return;

        String apkId = req.checkString("apkId");
        //String packageName = req.getString("package", "");
        post.setAttachmentId(Target.forApk(apkId).toCompatibleString());
    }


    @Route(url = "/post/repost")
    public void createRepost(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        long quote = req.checkLong("postId");
        Post quotePost = stream.getPost(ctx, Post.FULL_COLUMNS, quote);
        if (quotePost == null)
            throw new ServerException(E.INVALID_POST, "Invalid quote post id " + quote);

        //if (quotePost.getQuote() != 0L)
        //    throw new ServerException(E.REPETITIVE_REPOST, "Repetitive repost");

        if (BooleanUtils.isNotTrue(quotePost.getCanQuote()))
            throw new ServerException(E.CANNOT_QUOTE, "The post can't be quoted");

        if (req.getBoolean("secretly", false))
            req.checkString("to");

        String message = req.checkString("newmsg");
        long now = DateHelper.nowMillis();

        Post post = new Post(RandomHelper.generateId(now));
        post.setAddTo(PeopleIds.parseAddTo(message));
        post.setTo(PeopleIds.forStringIds(PeopleId.USER, req.getStringArray("to", ",", new String[]{})));
        post.setUpdatedTime(now);
        post.setCreatedTime(now);
        post.setDestroyedTime(0);
        post.setApp(ctx.getApp());
        post.setAttachments("[]"); // TODO: xx?
        post.setPrivate(req.getBoolean("secretly", false));
        post.setMessage(Post.makeRepostMessage(message, quotePost));
        post.setCanComment(req.getBoolean("can_comment", true));
        post.setCanLike(req.getBoolean("can_like", true));
        post.setCanQuote(req.getBoolean("can_reshare", true));
        post.setType(quotePost.getType()); // TODO: Post.POST_TEXT or quotePost.getType() ?
        UserAgent ua = req.getUserAgent();
        post.setDevice(ua != null ? ua.getDeviceType() : "");
        post.setGeoLocation(ctx.getGeoLocation());
        post.setQuote(quote);
        post.setSourceId(ctx.getViewer());
        post.setAppData(req.getString("app_data", ""));
        post.setLocation(ctx.getLocation());

        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
        post = stream.expand(ctx, Post.X_FULL_COLUMNS, post);
        resp.body(RawText.of(CompatiblePost.postToJson(post, CompatiblePost.V1_FULL_COLUMNS, true)));
    }

    @Route(url = "/post/delete")
    public void destroyPost(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        long[] postIds = req.checkLongArray("postIds", ",");

        for (long postId : postIds) {
            new ContextObject(ctx, ContextObject.TYPE_DESTROY, postId).sendThisWith(destroyQueue);
        }
        resp.body(true);
    }


    @Route(url = "/post/update")
    public void updatePost(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        Post post = new Post(req.checkLong("postId"));
        post.setMessage(req.checkString("msg"));
        stream.updatePost(ctx, post);
        resp.body(true);
    }

    @Route(url = "/post/updateaction")
    public void updateAction(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        String can_comment = req.getString("can_comment", null);
        String can_like = req.getString("can_like", null);
        String can_reshare = req.getString("can_reshare", null);

        Post post = new Post(req.checkLong("postId"));
        if (can_comment != null)
            post.setCanComment(Boolean.parseBoolean(can_comment));
        if (can_like != null)
            post.setCanLike(Boolean.parseBoolean(can_like));
        if (can_reshare != null)
            post.setCanQuote(Boolean.parseBoolean(can_reshare));

        new ContextObject(ctx, ContextObject.TYPE_UPDATE, post).sendThisWith(updateQueue);
        resp.body(true);
    }

    @Route(url = {"/post/get", "/post/qiupuget"})
    public void getPosts(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long[] postIds = req.checkLongArray("postIds", ",");
        String[] v1Cols = CompatiblePost.expandV1Columns(req.getStringArray("cols", ",", CompatiblePost.V1_FULL_COLUMNS));
        Posts posts = stream.getPosts(ctx, CompatiblePost.v1ToV2Columns(v1Cols), postIds);
        resp.body(RawText.of(CompatiblePost.postsToJson(posts, v1Cols, true)));
    }

    @Route(url = {"/post/publictimeline", "/post/qiupupublictimeline"})
    public void getPublicTimeline(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        // params
        String[] v1Cols = CompatiblePost.expandV1Columns(req.getStringArray("cols", ",", CompatiblePost.V1_FULL_COLUMNS));
        int types = CompatiblePost.v1ToV2Type(req.getInt("type", CompatiblePost.V1_ALL_POST_TYPES));
        long min = CompatiblePost.timestampToId(req.getLong("start_time", 0));
        long max = CompatiblePost.timestampToId(req.getLong("end_time", 0));
        Page page = req.getPage(20, 100);
        int app = req.checkInt("appid");

        if (max > 0)
            max--;

        PostFilter filter = new PostFilter(types, app, min, max, null);
        Posts posts = ((CompatibleStreamLogic) stream).getPublicTimeline(ctx, filter, CompatiblePost.v1ToV2Columns(v1Cols), page);
        resp.body(RawText.of(CompatiblePost.postsToJson(posts, v1Cols, true)));
    }

    @Route(url = "/post/hot")
    public void getHot(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        Page page = req.getPage(20, 100);
        String[] v1Cols = CompatiblePost.expandV1Columns(req.getStringArray("cols", ",", CompatiblePost.V1_FULL_COLUMNS));
        long[] postIds = ArrayHelper.stringArrayToLongArray(comment.getTargetIdsOrderByCommentCount(ctx, Target.POST, false, page));
        Posts posts = stream.getPosts(ctx, CompatiblePost.v1ToV2Columns(v1Cols), postIds);
        resp.body(RawText.of(CompatiblePost.postsToJson(posts, v1Cols, true)));
    }

    @Route(url = "/post/nearby")
    public void getNearby(Request req, Response resp) {
        // TODO: xx
        resp.body(RawText.of("[]"));
    }

    @Route(url = {"/post/userstimeline", "/post/qiupuusertimeline"})
    public void getUserTimeline(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        long userId = req.checkLong("users");
        String[] v1Cols = CompatiblePost.expandV1Columns(req.getStringArray("cols", ",", CompatiblePost.V1_FULL_COLUMNS));
        long min = CompatiblePost.timestampToId(req.getLong("start_time", 0));
        long max = CompatiblePost.timestampToId(req.getLong("end_time", 0));
        int types = CompatiblePost.v1ToV2Type(req.getInt("type", CompatiblePost.V1_ALL_POST_TYPES));
        Page page = req.getPage(20, 50);
        int app = req.checkInt("appid");

        PostFilter filter = new PostFilter(types, app, min, max, null);
        Posts posts = stream.getUserTimeline(ctx, userId, filter, CompatiblePost.v1ToV2Columns(v1Cols), page);
        resp.body(RawText.of(CompatiblePost.postsToJson(posts, v1Cols, true)));
    }

    @Route(url = "/post/myshare")
    public void getShare(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        long viewerId;
        if (ctx.isLogined()) {
            viewerId = ctx.getViewer();
        } else {
            viewerId = req.checkLong("users");
        }

        String[] v1Cols = CompatiblePost.expandV1Columns(req.getStringArray("cols", ",", CompatiblePost.V1_FULL_COLUMNS));
        long min = CompatiblePost.timestampToId(req.getLong("start_time", 0));
        long max = CompatiblePost.timestampToId(req.getLong("end_time", 0));
        int types = CompatiblePost.v1ToV2Type(req.getInt("type", CompatiblePost.V1_ALL_POST_TYPES));
        Page page = req.getPage(20, 100);
        int app = req.checkInt("appid");

        PostFilter filter = new PostFilter(types, app, min, max, null);
        Posts posts = stream.getUserTimeline(ctx, viewerId, filter, CompatiblePost.v1ToV2Columns(v1Cols), page);
        resp.body(RawText.of(CompatiblePost.postsToJson(posts, v1Cols, true)));
    }

    @Route(url = {"/post/friendtimeline", "/post/qiupufriendtimeline"})
    public void getFriendsTimeline(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        String[] v1Cols = CompatiblePost.expandV1Columns(req.getStringArray("cols", ",", CompatiblePost.V1_FULL_COLUMNS));
        long startTime = req.getLong("start_time", 0);
        long endTime = req.getLong("end_time", 0);

        long min = CompatiblePost.timestampToId(startTime);
        long max = CompatiblePost.timestampToId(endTime - 1);
        int types = CompatiblePost.v1ToV2Type(req.getInt("type", CompatiblePost.V1_ALL_POST_TYPES));
        Page page = req.getPage(20, 100);
        int app = req.checkInt("appid");
        int[] circleIds = req.getIntArray("circleIds", ",", null);

        PostFilter filter = new PostFilter(types, app, min, max, null);
        PeopleIds friendIds;
        if (circleIds == null) {
            friendIds = friend.getFriends(ctx, ctx.getViewer());
            friendIds.add(PeopleId.user(ctx.getViewer())); // Add me!
        } else {
            friendIds = friend.getFriendsInCircles(ctx, ctx.getViewer(), circleIds);
        }
        filter.friendIds = friendIds.toSet(new LinkedHashSet<PeopleId>());
        Posts posts = stream.getFriendsTimeline(ctx, ctx.getViewer(), filter, CompatiblePost.v1ToV2Columns(v1Cols), page);
        resp.body(RawText.of(CompatiblePost.postsToJson(posts, v1Cols, true)));
    }

    @Route(url = "/post/canlike")
    public void getPostCanLike(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        Post post = stream.getPost(ctx, Post.FULL_COLUMNS, req.checkLong("postId"));
        resp.body(post != null && BooleanUtils.isTrue(post.getCanLike()));
    }

    @Route(url = "/post/cancomment")
    public void getPostCanComment(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        Post post = stream.getPost(ctx, Post.FULL_COLUMNS, req.checkLong("postId"));
        resp.body(post != null && BooleanUtils.isTrue(post.getCanComment()));
    }

    @Route(url = "/post/commented")
    public void getCommentedPosts(Request req, Response resp) {
        getPostsInConversion(req, resp, Actions.COMMENT);
    }

    @Route(url = "/post/liked")
    public void getLikedPosts(Request req, Response resp) {
        getPostsInConversion(req, resp, Actions.LIKE);
    }

    @Route(url = "/user/status/update")
    public void updateStatus(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        String message = req.checkString("newStatus");
        boolean isPost = req.getBoolean("post", true);
        long now = DateHelper.nowMillis();

        Post post = new Post(RandomHelper.generateId(now));
        post.setType(Post.POST_TEXT);
        post.setAddTo(new PeopleIds());
        post.setTo(new PeopleIds());
        post.setUpdatedTime(now);
        post.setCreatedTime(now);
        post.setDestroyedTime(0);
        post.setApp(ctx.getApp());
        post.setAttachments("[]");
        post.setPrivate(false);
        post.setMessage(message);
        post.setCanComment(true);
        post.setCanLike(true);
        post.setCanQuote(true);
        post.setDevice(req.getRawUserAgent());
        post.setGeoLocation(ctx.getGeoLocation());
        post.setQuote(0L); // not repost
        post.setSourceId(ctx.getViewer());
        post.setAppData("");
        post.setLocation(ctx.getLocation());


        status.updateStatus(ctx, new Status(message, DateHelper.nowMillis()));
        // add is can post
        if (isPost) {
            new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
            post = stream.expand(ctx, Post.X_FULL_COLUMNS, post);
        } else {
            post = new Post();
        }
        resp.body(RawText.of(CompatiblePost.postToJson(post, CompatiblePost.V1_FULL_COLUMNS, true)));
    }

    @Route(url = "/link/create")
    public void postLink(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        if (req.getBoolean("secretly", false))
            req.checkString("mentions");

        String url = req.checkString("url");
        url = URLHelper.catchHttpUrl(url);
        if (StringUtils.length(url) < 5)
            throw new ServerException(E.PARAM, "url error");

        String title = req.checkString("title");
        String message = req.checkString("msg");

        long now = DateHelper.nowMillis();
        Post post = new Post(RandomHelper.generateId(now));
        post.setType(Post.POST_LINK);
        post.setAddTo(PeopleIds.parseAddTo(message));
        post.setTo(PeopleIds.forStringIds(PeopleId.USER, req.getStringArray("mentions", ",", new String[]{})));
        post.setUpdatedTime(now);
        post.setCreatedTime(now);
        post.setDestroyedTime(0);
        post.setApp(ctx.getApp());
        post.setAttachments("[]");
        post.setPrivate(req.getBoolean("secretly", false));
        post.setMessage(message);
        post.setAttachmentIds(new String[]{url});
        post.setCanComment(req.getBoolean("can_comment", true));
        post.setCanLike(req.getBoolean("can_like", true));
        post.setCanQuote(req.getBoolean("can_reshare", true));
        post.setDevice(req.getRawUserAgent());
        post.setGeoLocation(ctx.getGeoLocation());
        post.setQuote(0L); // not repost
        post.setSourceId(ctx.getViewer());
        post.setAppData(req.getString("app_data", ""));
        post.setLocation(ctx.getLocation());


        LinkEntity le = new LinkEntity(url, title, "", "");
        post.setAttachments(new LinkEntities(le).toJson(true));

        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
        post = stream.expand(ctx, Post.X_FULL_COLUMNS, post);
        resp.body(RawText.of(CompatiblePost.postToJson(post, CompatiblePost.V1_FULL_COLUMNS, true)));
    }

    @Route(url = "/feedback/create")
    public void postFeedback(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        String message = req.checkString("msg");

        long now = DateHelper.nowMillis();
        Post post = new Post(RandomHelper.generateId(now));
        post.setType(Post.POST_TEXT);
        post.setAddTo(new PeopleIds());
        post.setTo(SpecificPeoples.getInstance().getPeopleIds("user.qiupu"));
        post.setUpdatedTime(now);
        post.setCreatedTime(now);
        post.setDestroyedTime(0);
        post.setApp(ctx.getApp());
        post.setAttachments("[]");
        post.setPrivate(true);
        post.setMessage(message);
        post.setCanComment(req.getBoolean("can_comment", true));
        post.setCanLike(req.getBoolean("can_like", true));
        post.setCanQuote(req.getBoolean("can_reshare", true));
        post.setDevice(req.getRawUserAgent());
        post.setGeoLocation(ctx.getGeoLocation());
        post.setLocation(ctx.getLocation());
        post.setQuote(0L); // not repost
        post.setSourceId(ctx.getViewer());
        post.setAppData(req.getString("app_data", ""));

        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
        post = stream.expand(ctx, Post.X_FULL_COLUMNS, post);
        resp.body(RawText.of(CompatiblePost.postToJson(post, CompatiblePost.V1_FULL_COLUMNS, true)));
    }

    private void getPostsInConversion(Request req, Response resp, int targetAction) {
        Context ctx = checkContext(req, true);

        // params
        String[] v1Cols = CompatiblePost.expandV1Columns(req.getStringArray("cols", ",", CompatiblePost.V1_FULL_COLUMNS));
        Page page = req.getPage(20, 50);

        // get
        Posts posts = stream.getPostsInConversation(ctx, ctx.getViewer(), targetAction, CompatiblePost.v1ToV2Columns(v1Cols), page);
        resp.body(RawText.of(CompatiblePost.postsToJson(posts, v1Cols, true)));
    }

}
