package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.NameInfo;
import com.borqs.server.platform.feature.account.ProfileInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.feature.maker.Maker;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.platform.mq.QueueName;
import com.borqs.server.platform.util.*;
import com.borqs.server.platform.util.sender.email.AsyncMailSender;
import com.borqs.server.platform.util.sender.email.Mail;
import com.borqs.server.platform.util.template.FreeMarker;
import com.borqs.server.platform.web.UserAgent;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;
import com.borqs.server.pubapi.i18n.PackageClass;
import org.apache.commons.lang.StringUtils;

import java.util.ResourceBundle;

@IgnoreDocument
public class Register1Api extends PublicApiSupport {
    public static final FreeMarker FREE_MARKER = new FreeMarker(PackageClass.class);
    protected AsyncMailSender mailSender;

    protected FriendLogic friend;
    private String serverHost;
    protected CibindLogic cibind;

    private AccountLogic account;

    public QueueName getPostQueue() {
        return postQueue;
    }

    public void setPostQueue(QueueName postQueue) {
        this.postQueue = postQueue;
    }

    private QueueName postQueue;

    public Maker<Mail> getMaker() {
        return maker;
    }

    public void setMaker(Maker<Mail> maker) {
        this.maker = maker;
    }

    private Maker<Mail> maker;

    public Register1Api() {
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    public AsyncMailSender getMailSender() {
        return mailSender;
    }

    public void setMailSender(AsyncMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
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

    private void displayInvitePage(Context ctx, Request req, Response resp)
    {
        String info = "";
        String infoB64 = req.checkString("info");
        infoB64 = StringUtils.replace(infoB64, " ", "+");

        String[] arr = FeedbackParams.fromSegmentedBase64(infoB64, "/");
        int len = arr.length;
        String login_name = "";
        String name = "";
        String fromId = "";

        //now case
        if (len == 3) {
            login_name = arr[0];
            if (login_name.startsWith("+86"))
                login_name = StringUtils.substringAfter(login_name, "+86");
            name = arr[1];
            fromId = arr[2];
        }
        //Compatible with old version
        else {
            String phone = arr[0];
            phone = StringUtils.substringAfter(phone, "+86");

            String email = arr[1];

            name = arr[len - 2];
            fromId = arr[len - 1];

            if (len > 4) {
                int index1 = info.indexOf("/") + 1;
                int index2 = info.indexOf(name) - 1;
                email = info.substring(index1, index2);
            }

            if (StringUtils.isNotBlank(phone)) {
                login_name = phone;
            } else {
                login_name = email;
            }
        }

        String fromName = "";
        User fromUser = account.getUser(ctx, null, Long.parseLong(fromId));
        if(fromUser != null)
        {
            fromName = fromUser.getDisplayName();
        }

        long uid = cibind.whoBinding(ctx, login_name);
        int isFriend = 0;
        if (uid != 0)
            isFriend = friend.hasFriend(ctx, uid, PeopleId.fromId(fromId)) ? 1 : 0;

        String locale = ctx.getUserAgent().getLocale();
        String html = FREE_MARKER.merge("invite.ftl", locale, new Object[][]{
                {"host", serverHost},
                {"login_name", login_name},
                {"fromId", fromId},
                {"uid", String.valueOf(uid)},
                {"name0", name},
                {"fromName", fromName},
                {"isFriend", isFriend}
        });
        resp.type("text/html");
        resp.body(RawText.of(html));
    }

    private void mutualFriend(Context ctx, long userId, long fromId) {
        ctx.setViewer(userId);
        friend.addFriendsIntoCircle(ctx, FriendReasons.INVITED, PeopleIds.of(PeopleId.fromId(fromId)), Circle.CIRCLE_ACQUAINTANCE);
        friend.addFriendsIntoCircle(ctx, FriendReasons.INVITED, PeopleIds.of(PeopleId.fromId(fromId)), Circle.CIRCLE_ADDRESS_BOOK);

        ctx.setViewer(fromId);
        friend.addFriendsIntoCircle(ctx, FriendReasons.INVITED, PeopleIds.of(PeopleId.fromId(userId)), Circle.CIRCLE_ACQUAINTANCE);
        friend.addFriendsIntoCircle(ctx, FriendReasons.INVITED, PeopleIds.of(PeopleId.fromId(userId)), Circle.CIRCLE_ADDRESS_BOOK);
    }

    private void activeAccount(Context ctx, Request req, Response resp)
    {
        String pwd = req.checkString("password");

        User user = new User();
        String displayName = req.checkString("display_name");
        NameInfo name = NameInfo.split(displayName);
        user.setName(name);
        user.setPassword(pwd);
        ProfileInfo profile = new ProfileInfo();
        profile.setGender(req.getString("gender", "u"));
        user.setProfile(profile);
        long userId = account.createUser(ctx, user).getUserId();
        ctx.setViewer(userId);

        String bind = req.checkString("bind");
        String bindType = "";
        if(FormatMatch.isEmail(bind))
        {
            bindType = BindingInfo.EMAIL;
        }
        else if(FormatMatch.isPhone(bind))
        {
            bindType = BindingInfo.MOBILE_TEL;
        }
        if(StringUtils.isNotBlank(bindType))
        {
            cibind.bind(ctx, bindType, bind);
        }

        boolean mutual = req.checkBoolean("mutual");
        if(mutual)
        {
            long fromId = req.checkLong("fromid");
            mutualFriend(ctx, userId, fromId);
        }

        //send a post
        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);
        String message = bundle.getString("registerapi.create.account.post");
        autoPost(ctx, message);

        resp.redirect("http://api.borqs.com/qiupu/active_down?bind=" + bind + "&password=" + pwd);
    }

    private void bindFromInvite(Context ctx, Request req, Response resp)
    {
        long userId = login.validatePassword(ctx, req.checkString("borqs_account"), req.checkString("borqs_pwd"));

        if(userId == 0)
            throw new ServerException(E.INVALID_USER_OR_PASSWORD, "Login name or password is wrong!");

        String bind = req.checkString("bind");
        String bindType = "";
        if(FormatMatch.isEmail(bind))
        {
            bindType = BindingInfo.EMAIL;
        }
        else if(FormatMatch.isPhone(bind))
        {
            bindType = BindingInfo.MOBILE_TEL;
        }
        if(StringUtils.isNotBlank(bindType))
        {
            ctx.setViewer(userId);
            cibind.bind(ctx, bindType, bind);
        }

        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);
        String notice = bundle.getString("registerapi.invite.bind.success");
        String html = FREE_MARKER.merge("notice.ftl", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });
        resp.type("text/html");
        resp.charset("UTF-8");
        resp.body(RawText.of(html));
    }

    private void mutualFriend(Context ctx, Request req, Response resp)
    {
        long userId = req.checkLong("uid");
        long fromId = req.checkLong("fromid");
        mutualFriend(ctx, userId, fromId);
        resp.body(true);
    }

    protected void dealInvite0(Request req, Response resp)
    {
        Context ctx = checkContext(req, false);
        int action = req.getInt("action", 0);

        switch (action) {
            case 0: //display web page
                displayInvitePage(ctx, req, resp);
                break;
            case 1: //active account
                activeAccount(ctx, req, resp);
                break;
            case 2: //bind
                bindFromInvite(ctx, req, resp);
                break;
            case 3: //mutual add friend
                mutualFriend(ctx, req, resp);
                break;
        }
    }

    @Route(url = "/account/create")
    public void createAccount(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        String loginEmail = req.getString("login_email", "");
        String loginPhone = req.getString("login_phone", "");
        if (StringUtils.isBlank(loginEmail) && StringUtils.isBlank(loginPhone))
            throw new ServerException(E.PARAM, "Must have parameter 'login_email1' or 'login_phone1'");
        String password = req.checkString("password");
        int appId = req.getInt("appid", AppIds.NONE);
        String displayName = req.checkString("display_name");
        String nickName = req.getString("nick_name", "");
        String gender = req.getString("gender", "u");
        String imei = req.getString("imei", "");
        String imsi = req.getString("imsi", "");

        if (StringUtils.isNotBlank(loginEmail) && cibind.hasBinding(ctx, loginEmail))
            throw new ServerException(E.BINDING_EXISTS, "has binded");
        if (StringUtils.isNotBlank(loginPhone) && cibind.hasBinding(ctx, loginPhone))
            throw new ServerException(E.BINDING_EXISTS, "has binded");

        //send verify email
        if (StringUtils.isNotBlank(loginEmail)) {
            String locale = ctx.getUserAgent().getLocale();
            ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);

            String subject = bundle.getString("platformservlet.account.register.complete");
            String key = FeedbackParams.toSegmentedBase64(true, "/", loginEmail, password, String.valueOf(appId),
                    displayName, nickName, gender, imei, imsi);
            String url = "http://" + serverHost + "/v2/register/mail/deal?key=" + key;

            String template = bundle.getString("registerapi.account.register.message");
            String message = FREE_MARKER.mergeRaw(template, new Object[][]{
                    {"displayName", displayName},
                    {"regUrl", url}
            });

            Mail mail = maker.make(ctx, MakerTemplates.EMAIL_ESSENTIAL, new Object[][] {
                    {"from", mailSender.getSmtpUsername()},
                    {"to", loginEmail},
                    {"subject", subject},
                    {"serverHost", serverHost},
                    {"content", message}
            });
            mailSender.asyncSend(mail);
        }

        resp.body(true);
    }

    @Route(url = "/account/invite")
    public void invite(Request req, Response resp) {
        dealInvite0(req, resp);
    }

    @Route(url = "/account/email_invite")
    public void emailInvite(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long uid = ctx.getViewer();
        String viewerId = String.valueOf(uid);
        User user = account.getUser(ctx, null, uid);
        String fromName = user.getDisplayName();

        String[] emails = req.checkStringArray("emails", ",");
        String[] names = req.checkStringArray("names", ",");
        String message = req.getString("message", "");

        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);

        for(int i = 0; i < emails.length; i++) {
            String info = FeedbackParams.toSegmentedBase64(true, "/", emails[i], names[i], viewerId);
            String url = "http://" + serverHost + "/v2/invite/mail/deal?info=" + info;
            String template = bundle.getString("registerapi.email.invite.email");
            String emailContent = FREE_MARKER.mergeRaw(template, new Object[][]{
                    {"displayName", names[i]},
                    {"fromName", fromName},
                    {"Url", url}
            });

            if(StringUtils.isNotBlank(message))
            {
                template = bundle.getString("registerapi.email.invite.postscript");
                emailContent += FREE_MARKER.mergeRaw(template, new Object[][]{
                        {"message", message}
                });
            }

            template = bundle.getString("registerapi.email.invite.title");
            String title = FREE_MARKER.mergeRaw(template, new Object[][]{
                    {"fromName", fromName}
            });

            Mail mail = maker.make(ctx, MakerTemplates.EMAIL_ESSENTIAL, new Object[][] {
                    {"from", mailSender.getSmtpUsername()},
                    {"to", emails[i]},
                    {"subject", title},
                    {"serverHost", serverHost},
                    {"content", emailContent}
            });

            mailSender.asyncSend(mail);
        }
        resp.body(true);
    }
}
