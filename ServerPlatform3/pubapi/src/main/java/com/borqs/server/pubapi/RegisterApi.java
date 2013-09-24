package com.borqs.server.pubapi;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.NameInfo;
import com.borqs.server.platform.feature.account.ProfileInfo;
import com.borqs.server.platform.feature.account.User;
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
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.i18n.PackageClass;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ResourceBundle;


@RoutePrefix("/v2")
@HttpExamplePackage(com.borqs.server.pubapi.example.PackageClass.class)
public class RegisterApi extends PublicApiSupport {
    public static final FreeMarker FREE_MARKER = new FreeMarker(PackageClass.class);
    protected AsyncMailSender mailSender;

    public Maker<Mail> getMaker() {
        return maker;
    }

    public void setMaker(Maker<Mail> maker) {
        this.maker = maker;
    }

    private Maker<Mail> maker;
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

    public RegisterApi() {
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

    /**
     * 邮件邀请多个用户注册
     *
     * @group Register
     * @http-param emails 被邀请者的email列表，逗号隔开
     * @http-param names 被邀请者的名字列表，逗号隔开
     * @http-param message:空串 邀请者向被邀请者附加的信息
     * @http-return true或者异常
     * @http-example {
     * "result":"true"
     * }
     */
    @Route(url = "/invite/mail/send")
    public void sendInviteMail(Request req, Response resp) {
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

    /**
     * 被邀请者对邮件邀请的处理
     *
     * @group Register
     * @login n
     * @http-param action 动作数值： 0-显示邀请的网页 1-激活帐号 2-绑定已有帐号 3-互相加为好友
     * @http-param info 当action=0的时候使用，它是“登录名（邮箱或手机号）/被邀请者名字/邀请者ID的base64”字符串
     * @http-param password 当action=1的时候使用，用户的密码md5加密
     * @http-param display_name 当action=1的时候使用，用户的显示名称
     * @http-param gender:u 当action=1的时候使用，性别，u-不明 m-男 f-女
     * @http-param bind 当action=1或action=2的时候使用，绑定信息，用于登录的email或手机号
     * @http-param mutual 当action=1的时候使用，是否互相加为好友，true-是 false-否，若为true，则互相将对方加入到自己的地址本和熟人的圈子中
     * @http-param fromid 当action=1或action=3的时候使用，邀请者的用户ID
     * @http-param borqs_account 当action=2的时候使用，已有播思账号的登录名
     * @http-param borqs_pwd 当action=2的时候使用，已有播思账号的密码md5
     * @http-param uid 当action=3的时候使用，被邀请者的用户ID
     * @http-return 当action=0时，返回网页；当action=1时，返回梧桐最新版本apk的二进制流；当action=2时，返回网页；当action=3时，返回true或异常
     * @http-example {
     * "result":"true"
     * }
     */
    @Route(url = "/invite/mail/deal")
    public void dealInviteMail(Request req, Response resp) {
        dealInvite0(req, resp);
    }

    /**
     * 被邀请者对短信邀请的处理
     *
     * @group Register
     * @login n
     * @http-param action 动作数值： 0-显示邀请的网页 1-激活帐号 2-绑定已有帐号 3-互相加为好友
     * @http-param info 当action=0的时候使用，它是“登录名（邮箱或手机号）/被邀请者名字/邀请者ID的base64”字符串
     * @http-param password 当action=1的时候使用，用户的密码md5加密
     * @http-param display_name 当action=1的时候使用，用户的显示名称
     * @http-param gender:u 当action=1的时候使用，性别，u-不明 m-男 f-女
     * @http-param bind 当action=1或action=2的时候使用，绑定信息，用于登录的email或手机号
     * @http-param mutual 当action=1的时候使用，是否互相加为好友，true-是 false-否，若为true，则互相将对方加入到自己的地址本和熟人的圈子中
     * @http-param fromid 当action=1或action=3的时候使用，邀请者的用户ID
     * @http-param borqs_account 当action=2的时候使用，已有播思账号的登录名
     * @http-param borqs_pwd 当action=2的时候使用，已有播思账号的密码md5
     * @http-param uid 当action=3的时候使用，被邀请者的用户ID
     * @http-return 当action=0时，返回网页；当action=1时，返回梧桐最新版本apk的二进制流；当action=2时，返回网页；当action=3时，返回true或异常
     * @http-example {
     * "result":"true"
     * }
     */
    @Route(url = "/invite/message/deal")
    public void dealInviteMessage(Request req, Response resp) {
        dealInvite0(req, resp);
    }

    // message register
    @Route(url = "/register/message/send")
    public void sendRegisterMessage(Request req, Response resp) {
        // TODO: xx
    }

    @Route(url = "/register/message/deal")
    public void dealRegisterMessage(Request req, Response resp) {
        // TODO: xx
    }


    // mail register
    @Route(url = "/register/mail/send")
    public void sendRegisterMail(Request req, Response resp) throws ServerException {
        String loginEmail = req.getString("login_email", "");
        
        if ("".equals(loginEmail))
            throw new ServerException(E.INVALID_VERIFICATION_CODE, "Must have parameter 'login_email1'");

        String loginPhone = req.getString("login_phone", "");
        String pwd = req.getString("password", "");
        String appId = req.getString("appid", "");
        String displayName = req.getString("display_name", "");
        String gender = req.getString("gender", "m");
        String nickName = req.getString("nick_name", "");
        String imei = req.getString("imei", "");
        String imsi = req.getString("imsi", "");


        if (StringUtils.isNotBlank(loginEmail)) {
            //get I18N Param
            Context ctx = checkContext(req, true);
            String locale = ctx.getUserAgent().getLocale();
            ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);
            String subject = bundle.getString("platformservlet.account.register.complete");

            //converter params to base64
            String key = FeedbackParams.toSegmentedBase64(true, "/", loginEmail, pwd, appId,
                    displayName, nickName, gender, imei, imsi);
            String url = "http://" + serverHost + "/v2/register/mail/deal?key=" + key;

            String template = bundle.getString("registerapi.account.register.message");
            String message = FREE_MARKER.mergeRaw(template, new Object[][]{
                    {"displayName", displayName},
                    {"regUrl", url}
            });

            Mail mail = maker.make(ctx, MakerTemplates.EMAIL_ESSENTIAL, new Object[][]{
                    {"from", mailSender.getSmtpUsername()},
                    {"to", loginEmail},
                    {"subject", subject},
                    {"serverHost", serverHost},
                    {"content", message}
            });

            mailSender.asyncSend(mail);
        }
    }

    @Route(url = "/register/mail/deal")
    public void dealRegisterMail(Request req, Response resp) throws IOException {
        Context ctx = checkContext(req, false);
        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);

        String key = req.getString("key", "");
        key = StringUtils.replace(key, " ", "+");

        String[] arr = FeedbackParams.fromSegmentedBase64(key, "/", 8);
        String login_name = arr[0];
        String pwd = arr[1];
        String appId = arr[2];
        String displayName = arr[3];
        String nickName = arr[4];
        String gender = arr[5];
        String imei = arr[6];
        String imsi = arr[7];
        
        String login_email1 = "";
        if (login_name.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
            login_email1 = login_name;
        } else {
             throw new ServerException(E.INVALID_VERIFICATION_CODE, "Must have parameter 'login_email1'");
        }

        try {
            if (cibind.hasBinding(ctx, login_email1)) {
                String notice = bundle.getString("platformservlet.create.account.failed");
                FreeMarker freeMarker = new FreeMarker(PackageClass.class);
                String html = freeMarker.merge("notice.ftl", new Object[][]{
                        {"host", serverHost},
                        {"notice", notice}
                });
                resp.type("text/html");
                resp.charset("UTF-8");
                resp.body(RawText.of(html));
                return;
            }
            User user = new User();

            //init User Object
            user.setPassword(Encoders.md5Hex(pwd));
            user.setNickname(nickName);
            ProfileInfo profileInfo = new ProfileInfo();
            profileInfo.setGender(gender);
            user.setProfile(profileInfo);


            user.setName(NameInfo.split(displayName));
            user = account.createUser(ctx, user);
            ctx.setViewer(user.getUserId());
            //bind user info
            BindingInfo bindingInfo = new BindingInfo();
            bindingInfo.setType(BindingInfo.EMAIL);
            bindingInfo.setInfo(login_email1);
            cibind.bind(ctx, bindingInfo);

            
        } catch (Exception e) {
            String notice = bundle.getString("platformservlet.create.account.error");
                FreeMarker freeMarker = new FreeMarker(PackageClass.class);
                String html = freeMarker.merge("notice.ftl", new Object[][]{
                        {"host", serverHost},
                        {"notice", notice}
                });
                resp.type("text/html");
                resp.charset("UTF-8");
                resp.body(RawText.of(html));
            e.printStackTrace();
            return;
        }

        String notice = bundle.getString("platformservlet.create.account.success");
        FreeMarker freeMarker = new FreeMarker(PackageClass.class);
        String html = freeMarker.merge("notice.ftl", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });
        resp.type("text/html");
        resp.charset("UTF-8");
        resp.body(RawText.of(html));
    }
}
