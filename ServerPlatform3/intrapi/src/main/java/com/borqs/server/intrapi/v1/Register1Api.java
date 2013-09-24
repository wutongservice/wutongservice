package com.borqs.server.intrapi.v1;

import com.borqs.server.intrapi.InternalApiSupport;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.ProfileInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.platform.mq.QueueName;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.web.UserAgent;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;

import java.util.ResourceBundle;

@RoutePrefix("/internal")
@IgnoreDocument
public class Register1Api extends InternalApiSupport {
    private CibindLogic cibind;
    private AccountLogic account;
    private QueueName postQueue;

    public QueueName getPostQueue() {
        return postQueue;
    }

    public void setPostQueue(QueueName postQueue) {
        this.postQueue = postQueue;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    private FriendLogic friend;

    public long getQiupuId() {
        return qiupuId;
    }

    public void setQiupuId(long qiupuId) {
        this.qiupuId = qiupuId;
    }

    private long qiupuId;

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    private void autoPost(Context ctx, String message) {
        long now = DateHelper.nowMillis();
        Post post = new Post(RandomHelper.generateId(now));
        post.setType(Post.POST_TEXT);
        post.setAddTo(PeopleIds.parseAddTo(message));
        post.setTo(PeopleIds.forStringIds(PeopleId.USER, new String[]{}));
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
        UserAgent ua = ctx.getUserAgent();
        post.setDevice(ua != null ? ua.getDeviceType() : "");
        post.setGeoLocation(ctx.getGeoLocation());
        post.setQuote(0L);
        post.setSourceId(ctx.getViewer());
        post.setAppData("");

        new ContextObject(ctx, ContextObject.TYPE_CREATE, post).sendThisWith(postQueue);
    }
    
    @Route(url = "/createAccount")
    public void createAccount(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        
        String loginEmail = req.checkString("loginEmail1");
        String loginPhone = req.checkString("loginPhone1");
        String password = req.checkString("pwd");
        String displayName = req.checkString("displayName");
        String nickName = req.getString("nickName", "");
        String gender = req.checkString("gender");
        String imei = req.checkString("imei");
        String imsi = req.checkString("imsi");
        String device = req.checkString("device");
        String location = req.checkString("location");

        User user = new User();

        //init User Object
        user.setPassword(Encoders.md5Hex(password));
        user.setNickname(nickName);
        ProfileInfo profileInfo = new ProfileInfo();
        profileInfo.setGender(gender);
        user.setProfile(profileInfo);



        user = account.createUser(ctx, user);
        long userId = user.getUserId();
        ctx.setViewer(userId);

        //bind user info
        BindingInfo bindingInfo = new BindingInfo();
        bindingInfo.setType(BindingInfo.EMAIL);
        bindingInfo.setInfo(loginEmail);
        cibind.bind(ctx, bindingInfo);

        //add friend qiupu
        friend.addFriendsIntoCircle(ctx, FriendReasons.AUTO_CREATE, PeopleIds.of(PeopleId.fromId(qiupuId)), Circle.CIRCLE_FOLLOWER);

        //send a post
        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);
        String message = bundle.getString("registerapi.create.account.post");
        autoPost(ctx, message);

        resp.body(userId);
    }
}
