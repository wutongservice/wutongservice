package com.borqs.server.wutong.account2;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.DirectResponse;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.user.*;
import com.borqs.server.wutong.account2.util.CollectionsHelper;
import com.borqs.server.wutong.account2.util.TextEnum;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.messagecenter.MessageDelayCombineUtils;
import com.borqs.server.wutong.photo.PhotoLogic;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class AccountServlet extends WebMethodServlet {
    private static final PageTemplate pageTemplate = new PageTemplate(AccountServlet.class);
    private static final Logger L = Logger.getLogger(AccountServlet.class);
    private String serverHost;

    public static final int DEFAULT_USER_COUNT_IN_PAGE = 20;
    private static String prefix = "http://oss.aliyuncs.com/wutong-data/media/photo/";
    private static String sysPrefix = "http://oss.aliyuncs.com/wutong-data/system/";
    private StaticFileStorage photoStorage;
    private StaticFileStorage profileImageStorage;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");
        prefix = conf.getString("platform.profileImagePattern", prefix);
        sysPrefix = conf.getString("platform.sysIconUrlPattern", sysPrefix);
        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
        profileImageStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.profileImageStorage", ""));
        profileImageStorage.init();
    }

    @Override
    public void destroy() {
        profileImageStorage.destroy();
        photoStorage.destroy();
        super.destroy();
    }

    @WebMethod("account/create")
    public NoResponse createAccount(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);
        AccountLogic account = GlobalLogics.getAccount();
        FriendshipLogic firend = GlobalLogics.getFriendship();
        GroupLogic groupLogic = GlobalLogics.getGroup();

        String login_name = "";
        String key = qp.getString("key", "");
        boolean hasKey = StringUtils.isNotBlank(key);
        String login_email1 = qp.getString("login_email", "");
        String login_phone1 = qp.getString("login_phone", "");
        //        String login_phone1 = qp.getString("login_email", "");

        //        if (qp.getString("login_email", "").length()<=0 || !qp.getString("login_email", "").matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"))
        //            throw new ServerException(WutongErrors.PARAM_ERROR, "Must regist as email");

        if (!hasKey && StringUtils.isBlank(login_email1) && StringUtils.isBlank(login_phone1))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Must have parameter 'login_email1' or 'login_phone1'");


        boolean isActive = qp.containsKey("from_id");
        String fromId = qp.getString("from_id", "zdcitkzbfqwmx");
        String pwd = hasKey ? "" : qp.checkGetString("password");
        String appId = qp.getString("appid", Constants.NULL_APP_ID);
        String displayName = hasKey ? "" : qp.checkGetString("display_name");
        String nickName = qp.getString("nick_name", "");
        String gender = qp.getString("gender", "u");
        String imei = qp.getString("imei", "");
        String imsi = qp.getString("imsi", "");

        //        String ua = getDecodeHeader(req, "User-Agent", "","");
//        String ua = "lang=US";
        //        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
//        String lang = "en";
        String ua = ctx.getUa();
        String lang = ctx.getLanguage();
        String loc = ctx.getLocation();

        try {
            account.checkLoginNameNotExists(ctx, login_phone1, login_email1);
        } catch (Exception e) {
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, e.getMessage());
        }
        if (!hasKey && !isActive) {
            //send verify email
            if (StringUtils.isNotBlank(login_email1)) {
                key = FeedbackParams.toSegmentedBase64(true, "/", login_email1, pwd, appId,
                        displayName, nickName, gender, imei, imsi);
                String url = "http://" + serverHost + "/account/create?key=" + key;

                //                String emailContent = "		尊敬的" + displayName + "，请点击以下链接完成注册：<br>"
                //                        + "		" + "<a href=\"" + url + "\">" + url + "</a>";
                String template = Constants.getBundleStringByLang(lang, "platformservlet.create.account.email");
                String emailContent = SQLTemplate.merge(template, new Object[][]{
                        {"displayName", displayName},
                        {"url", url}
                });

                String title = Constants.getBundleStringByLang(lang, "platformservlet.create.account.title");
                GlobalLogics.getEmail().sendEmail(ctx, title, login_email1, displayName, emailContent, Constants.EMAIL_ESSENTIAL, lang);

                output(qp, req, resp, "{\"result\":true}", 200, "text/plain");
                return NoResponse.get();
            }

        } else if (hasKey) {
            key = StringUtils.replace(key, " ", "+");
            String[] arr = FeedbackParams.fromSegmentedBase64(key, "/", 8);
            login_name = arr[0];
            pwd = arr[1];
            appId = arr[2];
            displayName = arr[3];
            nickName = arr[4];
            gender = arr[5];
            imei = arr[6];
            imsi = arr[7];
            if (login_name.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
                login_email1 = login_name;
            } else {
                login_phone1 = login_name;
            }
        } else if (isActive) {
            if (StringUtils.isBlank(fromId)) {
                throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "Invaild Invitation");
            }
            if (!StringUtils.equals(fromId, "zdcitkzbfqwmx")) {
                account.checkUserIds(ctx, fromId);
            }
        }


        String userId = "";
        try {
            userId = account.createAccount(ctx, login_email1, login_phone1,
                    pwd,
                    displayName,
                    nickName,
                    gender,
                    imei,
                    imsi,
                    ua, loc
            );
            account.updateAccount(ctx, userId, Record.of("language", lang));
            String login_content = "";
            login_content = StringUtils.isBlank(login_email1) ? login_phone1 : login_email1;

            firend.updateVirtualFriendIdToAct(ctx, userId, login_content);
        } catch (Exception e) {
            if (StringUtils.isNotBlank(login_email1)) {
                String msg = e.getMessage();
                String notice = StringUtils.isBlank(msg) ? Constants.getBundleStringByLang(lang, "platformservlet.create.account.failed") : msg;
                String html = pageTemplate.merge("notice.ftl", new Object[][]{
                        {"host", serverHost},
                        {"notice", notice}
                });

                resp.setContentType("text/html");
                resp.getWriter().print(html);
            } else if (StringUtils.isNotBlank(login_phone1)) {
                output(qp, req, resp, "{\"result\":false}", 200, "text/plain");
            }

            return NoResponse.get();
        }

        if (isActive) {
            if (StringUtils.isNotBlank(fromId) && (!StringUtils.equals(fromId, "zdcitkzbfqwmx"))) {
                firend.setFriends(ctx, userId, fromId, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_INVITE, true);
                firend.setFriends(ctx, fromId, userId, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_INVITE, true);
                firend.setFriends(ctx, userId, fromId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_INVITE, true);
                firend.setFriends(ctx, fromId, userId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_INVITE, true);
            }

            //group
            login_name = StringUtils.isNotBlank(login_email1) ? login_email1 : login_phone1;
            long groupId = qp.getInt("group_id", 0);
            if (groupId != 0) {
                groupLogic.updateUserIdByIdentify(ctx, userId, login_name);
                groupLogic.addMember(ctx, groupId, userId, "");
            }


            resp.sendRedirect("http://" + serverHost + "/qiupu/active_down?bind=" + login_name + "&password=" + pwd);
        } else if (StringUtils.isNotBlank(login_email1)) {
            String notice = Constants.getBundleStringByLang(lang, "platformservlet.create.account.success");
            String html = pageTemplate.merge("notice.ftl", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });

            resp.setContentType("text/html");
            resp.getWriter().print(html);

        } else if (StringUtils.isNotBlank(login_phone1)) {
            output(qp, req, resp, "{\"result\":true}", 200, "text/plain");
        }

        //add by wangpeng for delay and combine email
        try {
            MessageDelayCombineUtils.sendEmailCombineAndDelayAccountCreate(ctx, userId, login_email1);
        } catch (Exception e) {
            L.error(ctx, e, "delay and combine account create email error!@@@@");
        }
        return NoResponse.get();
    }

    @WebMethod("account/invite")
    public DirectResponse invite(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        FriendshipLogic friendshipLogic = GlobalLogics.getFriendship();
        String info = "";
        String infoB64 = qp.checkGetString("info");
        infoB64 = StringUtils.replace(infoB64, " ", "+");
        //        info = new String(Encoders.fromBase64(infoB64), "UTF-8");
        //
        //        String[] arr = info.split("/");
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
            //            if (StringUtils.isBlank(email)) {
            //                email = "用于获取密码";
            //            }
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

        AccountLogic account = GlobalLogics.getAccount();
        RecordSet rs0 = account.getUsers(ctx, fromId, "display_name");
        String fromName = rs0.getFirstRecord().getString("display_name", "");
        RecordSet rs1 = account.getUserIds(ctx, login_name);

        long uid = 0l;
        if (!rs1.isEmpty()) {
            uid = rs1.getFirstRecord().getInt("user_id", 0l);
        }

        String freeMarkerFile = "";
        boolean exchangeVcard = qp.getBoolean("exchange_vcard", false);
        if (exchangeVcard)
            freeMarkerFile = uid == 0l ? "exchange_vcard0.ftl" : "exchange_vcard1.ftl";
        else
            freeMarkerFile = uid == 0l ? "invite0.freemarker" : "invite1.freemarker";

        int isFriend = 0;
        if (uid != 0l) {
            isFriend = friendshipLogic.isFriendP(ctx, String.valueOf(uid), fromId) ? 1 : 0;
        }
        String groupId = qp.getString("group_id", "0");

        String html = pageTemplate.merge(freeMarkerFile, new Object[][]{
                {"host", serverHost},
                {"login_name", login_name},
                {"fromId", fromId},
                {"uid", String.valueOf(uid)},
                {"name0", name},
                {"fromName", fromName},
                {"isFriend", isFriend},
                {"group", groupId}
        });

        return DirectResponse.of("text/html", html);
    }

    @WebMethod("account/email_invite")
    public boolean emailInvite(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic account = GlobalLogics.getAccount();
        String viewerId = ctx.getViewerIdString();
        boolean exchangeVcard = qp.getBoolean("exchange_vcard", false);
        String[] emails = StringUtils2.splitArray(qp.checkGetString("emails"), ",", true);

        //        String[] phones = new String[emails.length];
        //		for(int i = 0; i < emails.length; i++)
        //			phones[i] = "";
        //		String temp = qp.getString("phones", "");
        //		if(StringUtils.isNotBlank(temp))
        //			phones = StringUtils2.splitArray(temp, ",", true);

        String[] names = new String[emails.length];
        for (int i = 0; i < emails.length; i++)
            names[i] = "";
        String temp = qp.getString("names", "");
        if (StringUtils.isNotBlank(temp))
            names = StringUtils2.splitArray(temp, ",", true);

        String message = qp.getString("message", "");
        String fromName = account.getUser(ctx, viewerId, viewerId, "display_name").getString("display_name");
        String ua = ctx.getUa();
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        for (int i = 0; i < emails.length; i++) {
            //			temp = Encoders.toBase64(phones[i] + "/" + emails[i] + "/"
            //					+ names[i] + "/" + viewerId);

            temp = FeedbackParams.toSegmentedBase64(true, "/", emails[i], names[i], viewerId);
            String url = "http://" + serverHost + "/account/invite?info=" + temp + "&exchange_vcard=" + exchangeVcard;

            //            String emailContent = "		尊敬的" + names[i] + ", " + fromName + "邀请您激活播思账号，"
            //                    + "并希望成为您的好友，请点击以下链接接受邀请：<br>"
            //                    + "		" + "<a href=\"" + url + "\">" + url + "</a>";
            String template = Constants.getBundleString(ua, "platformservlet.email.invite.email");
            String emailContent = SQLTemplate.merge(template, new Object[][]{
                    {"displayName", names[i]},
                    {"fromName", fromName},
                    {"url", url}
            });

            if (StringUtils.isNotBlank(message)) {
                template = Constants.getBundleString(ua, "platformservlet.email.invite.postscript");
                emailContent += SQLTemplate.merge(template, new Object[][]{
                        {"message", message}
                });
            }

            template = Constants.getBundleString(ua, "platformservlet.email.invite.title");
            String title = SQLTemplate.merge(template, new Object[][]{
                    {"fromName", fromName}
            });

            GlobalLogics.getEmail().sendEmail(ctx, title, emails[i], names[i], emailContent, Constants.EMAIL_ESSENTIAL, lang);
        }

        return true;
    }

    @WebMethod("account/who")
    public long who(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        if (qp.containsKey("ticket")) {
            return Long.parseLong(accountLogic.whoLogined(ctx, qp.checkGetString("ticket")));
        } else if (qp.containsKey("login")) {

            long n = 0;
            String user_id = accountLogic.getUserIds(ctx, qp.checkGetString("login")).getFirstRecord().getString("user_id");
            if (StringUtils.isNotBlank(user_id)) {
                n = Long.parseLong(user_id);
            }
            return n;
        } else {
            return 0;
        }
    }

    @WebMethod("account/login")
    public Record login(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        return accountLogic.login(ctx, qp.checkGetString("login_name"), qp.checkGetString("password"), qp.getString("appid", Constants.NULL_APP_ID));
    }

    @WebMethod("account/logout")
    public boolean logout(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        return accountLogic.logout(ctx, qp.checkGetString("ticket"));
    }

    @WebMethod("account/change_password")
    public boolean changePassword(QueryParams qp, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);
        String viewerId = WutongContext.checkTicket(qp);

        String oldPassword = qp.checkGetString("oldPassword");
        String newPassword = qp.checkGetString("newPassword");

        if (oldPassword.equals(newPassword))
            throw new ServerException(WutongErrors.USER_NAME_PASSWORD_ERROR, "oldPassword and newPassword is the same!");

        return GlobalLogics.getAccount().changePassword(ctx, viewerId, oldPassword, newPassword);
    }

    @WebMethod("account/reset_password")
    public NoResponse resetPassword(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String loginName = qp.getString("login_name", "");
        String key = qp.getString("key", "");

        if (StringUtils.isBlank(loginName) && StringUtils.isBlank(key)) {
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Must have parameter 'login_name' or 'key'");
        }

        //        String ua = getDecodeHeader(req, "User-Agent", "","");
        String ua = "lang=US";
        //        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        String lang = "en";
        accountLogic.resetPassword(ctx, loginName, key, lang);

        if (StringUtils.isBlank(key)) {
            output(qp, req, resp, "{\"result\":true}", 200, "text/plain");
        } else {
            String notice = Constants.getBundleStringByLang(lang, "platformservlet.reset.password.notice");
            String html = pageTemplate.merge("notice.ftl", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });
            resp.setContentType("text/html");
            resp.getWriter().print(html);
        }

        return NoResponse.get();
    }

    @WebMethod("account/reset_password_for_phone")
    public String resetPasswordForPhone(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String phone = qp.checkGetString("phone");

        accountLogic.resetPasswordForPhone(ctx, phone);
        return "OK";
    }

    @WebMethod("user/show")
    public RecordSet showUsers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        FriendshipLogic friendshipLogic = GlobalLogics.getFriendship();

        AccountLogic accountLogic = GlobalLogics.getAccount();
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        if (ticket != null) {
            viewerId = ctx.getViewerIdString();
        }
        String userIds = qp.checkGetString("users");
        boolean withPublicCircles = qp.getBoolean("with_public_circles", false);

        if (!withPublicCircles) {
            return accountLogic.getUsers(ctx, viewerId, userIds, qp.getString("columns", AccountLogic.USER_LIGHT_COLUMNS_USER_SHOW));
        } else {
            RecordSet users = accountLogic.getUsers(ctx, viewerId, userIds, qp.getString("columns", AccountLogic.USER_LIGHT_COLUMNS_USER_SHOW));

            if (StringUtils.isNotBlank(viewerId)) {
                for (Record user : users) {
                    RecordSet inCircles = RecordSet.fromJson(JsonUtils.toJson(user.get("in_circles"), false));
                    inCircles = friendshipLogic.dealWithInCirclesByGroupsP(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.ACTIVITY_ID_BEGIN, viewerId, user.getString("user_id"), inCircles);
                    user.put("in_circles", inCircles);
                }
            }

            return users;
        }
    }

    @WebMethod("user/status/update")
    public Record userStatusUpdate(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String viewerId = ctx.getViewerIdString();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        boolean post = qp.getBoolean("post", true);
        return accountLogic.updateUserStatus(ctx, qp.getString("user", viewerId), qp.getString("newStatus", ""), ua, loc, post, can_comment, can_like, can_reshare);
    }

    @WebMethod("account/update")
    public boolean updateAccount(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String viewerId = ctx.getViewerIdString();
//        String displayName = p.getUser(viewerId, viewerId, "display_name").getString("display_name");
        Record user = new Record(qp.copy().removeKeys("appid", "sign", "sign_method", "ticket", "callback", "_"));
//        user.putMissing("display_name_temp", displayName);
        String ua = ctx.getUa();
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";

        return accountLogic.updateAccount(ctx, viewerId, user, lang);
    }

    @WebMethod("account/update_display_name_for_namesplit")
    public boolean updateAccountDisplayName(QueryParams qp, HttpServletRequest req) {
        AccountLogic accountLogic = GlobalLogics.getAccount();
        RecordSet all = accountLogic.getAlluser();
        for (Record rec : all) {
            Record user = new Record();
            user.put("display_name", rec.getString("display_name"));
            accountLogic.updateAccountForNamSpliter(null, rec.getString("user_id"), user);
        }
        return true;
    }

    @WebMethod("account/openface/phone")
    public boolean setOpenfacePhone(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String ua = ctx.getUa();
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        return accountLogic.setMiscellaneous(ctx, qp.checkGetString("userid"), qp.checkGetString("phone"), lang);
    }

    @WebMethod("account/openface/user_id")
    public int getOpenfaceUserIdByPhone(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        return accountLogic.findUidByMiscellaneousPlatform(ctx, qp.checkGetString("phone"));
    }

    @WebMethod("account/search")
    public RecordSet searchUsers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String viewerId = ctx.getViewerIdString();
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", DEFAULT_USER_COUNT_IN_PAGE);
        return accountLogic.searchUser(ctx, viewerId, qp.checkGetString("username"), page, count);
    }

    @WebMethod("account/upload_profile_image")
    public Record uploadProfileImage(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        PhotoLogic photo = GlobalLogics.getPhoto();

        String viewerId = ctx.getViewerIdString();
        //        String displayName = p.getUser(viewerId, viewerId, "display_name").getString("display_name");
        FileItem fi = qp.checkGetFile("profile_image");

        long uploaded_time = DateUtils.nowMillis();
        String imageName = "profile_" + viewerId + "_" + uploaded_time;
        String loc = ctx.getLocation();

        String album_id = qp.getString("album_id", "");


        if (StringUtils.isEmpty(album_id))
            album_id = photo.getAlbum(ctx, viewerId, photo.ALBUM_TYPE_PROFILE, "Profile Pictures");
        if (!photo.isAlbumExist(ctx, album_id)) {
            throw new ServerException(WutongErrors.PHOTO_ALBUM_NOT_EXISTS, "album not exist, please create album first");
        }

        String sfn = imageName + "_S.jpg";
        String ofn = imageName + "_M.jpg";
        String lfn = imageName + "_L.jpg";
        String tfn = imageName + "_T.jpg";

        if (photoStorage instanceof OssSFS) {
            lfn = "media/photo/" + lfn;
            ofn = "media/photo/" + ofn;
            sfn = "media/photo/" + sfn;
            tfn = "media/photo/" + tfn;
        }

        SFSUtils.saveScaledUploadImage(fi, photoStorage, sfn, "50", "50", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, ofn, "80", "80", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, lfn, "180", "180", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, tfn, "120", "120", "jpg");

        String photoID = Long.toString(RandomUtils.generateId());
        Record rc_photo = new Record();
        //rc_photo.put("photo_id", photoID);
        rc_photo.put("album_id", album_id);
        rc_photo.put("user_id", viewerId);
        rc_photo.put("img_middle", imageName + "_M.jpg");
        rc_photo.put("img_original", imageName + "_M.jpg");
        rc_photo.put("img_big", imageName + "_L.jpg");
        rc_photo.put("img_small", imageName + "_S.jpg");
        rc_photo.put("caption", "profile_image");
        rc_photo.put("location", loc);
        rc_photo.put("created_time", DateUtils.nowMillis());


        boolean result = photo.saveUploadPhoto(ctx, rc_photo);

        //        SFSUtils.saveScaledUploadImage(fi, profileImageStorage, sfn, "50", "50", "jpg");
        //        SFSUtils.saveScaledUploadImage(fi, profileImageStorage, ofn, "80", "80", "jpg");
        //        SFSUtils.saveScaledUploadImage(fi, profileImageStorage, lfn, "180", "180", "jpg");

        Record rc = Record.of("image_url", imageName + "_M.jpg", "small_image_url", imageName + "_S.jpg",
                "large_image_url", imageName + "_L.jpg",
                "original_image_url", imageName + "_M.jpg");
        //        rc.putMissing("display_name_temp", displayName);

        String ua = ctx.getUa();
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        accountLogic.updateAccount(ctx, viewerId, rc, lang);
        Record user = accountLogic.getUser(ctx, viewerId, viewerId, "image_url,small_image_url,large_image_url,original_image_url");
        Record out_user = new Record();
        out_user.put("result", true);
        out_user.put("image_url", user.getString("image_url"));
        out_user.put("small_image_url", user.getString("small_image_url"));
        out_user.put("large_image_url", user.getString("large_image_url"));
        out_user.put("original_image_url", user.getString("original_image_url"));
        return out_user;
    }

    @WebMethod("account/bind")
    public NoResponse accountBind(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String userId = ctx.getViewerIdString();
        String phone = qp.getString("phone", "");
        String email = qp.getString("email", "");
        if (phone.equals("") && email.equals("")) {
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Must have parameter 'phone' or 'email'");
        }
        if (!email.equals("") && !email.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "'email' error");
        }
        if (!phone.equals("") && !phone.matches("(1[\\d]{10})")) {
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "'phone' error");
        }
        if (!phone.equals("") && !email.equals("")) {
            throw new ServerException(WutongErrors.USER_PHONE_EMAIL_BIND_LIMIT_COUNT, "only can bind 'phone' or 'email' one time");
        }
        String key = qp.getString("key", "");
        if (!key.equals(""))
            L.debug(null, "really called " + req.getRequestURI());

        String ua = ctx.getUa();
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        boolean result = accountLogic.bindUserSendVerify(ctx, userId, phone, email, key, qp.checkGetString("ticket"), lang);

        if (StringUtils.isBlank(key)) {
            output(qp, req, resp, "{\"result\":" + result + "}", 200, "text/plain");
        } else {
            //        	String notice = result ? "恭喜您，绑定成功！" : "抱歉，绑定失败，请稍后再试。";

            String notice = "";
            if (result) {
                notice = Constants.getBundleStringByLang(lang, "platformservlet.account.bind.success");
            } else {
                notice = Constants.getBundleStringByLang(lang, "platformservlet.account.bind.failed");
            }

            String html = pageTemplate.merge("notice.ftl", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });

            resp.setContentType("text/html");
            resp.getWriter().print(html);
        }

        return NoResponse.get();
    }

    @WebMethod("account/invite_bind")
    public boolean accountBindFromInvite(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        GroupLogic groupLogic = GlobalLogics.getGroup();

        Record rec = accountLogic.login(ctx, qp.checkGetString("borqs_account"), qp.checkGetString("borqs_pwd"), Constants.NULL_APP_ID);
        String userId = rec.getString("user_id");
        String login_name = qp.checkGetString("login_name");
        String phone = "";
        String email = "";
        if (login_name.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
            email = login_name;
        } else {
            phone = login_name;
        }
        //generate key
        String gkey = userId + "/" + phone + "/" + email;
        FeedbackParams fp = new FeedbackParams().set("param", gkey);
        String b2 = fp.toBase64(true);

        String ua = ctx.getUa();
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        String loc = ctx.getLocation();

        long groupId = qp.getInt("group_id", 0);
        String appId = qp.getString("appid", String.valueOf(Constants.APP_TYPE_BPC));
        if (groupId != 0) {
            groupLogic.updateUserIdByIdentify(ctx, userId, login_name);
            groupLogic.addMember(ctx, groupId, userId, "");
        }

        return accountLogic.bindUserSendVerify(ctx, userId, phone, email, b2, qp.checkGetString("ticket"), lang);
    }

    /*@WebMethod("user/profile_image")
    public NoResponse downloadProfileImage(QueryParams qp, HttpServletResponse resp) {
        SFSUtils.writeResponse(resp, profileImageStorage, qp.checkGetString("file"));
        return NoResponse.get();
    }*/

    @WebMethod("privacy/set")
    public boolean setPrivacy(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String viewerId = ctx.getViewerIdString();

        RecordSet privacyItemList = new RecordSet();
        Iterator iter = qp.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String[] buildInParams = new String[]{"sign_method", "sign", "appid", "ticket"};
            if (!ArrayUtils.contains(buildInParams, key)
                    && !key.startsWith("private")) {
                Record privacyItem = new Record();
                privacyItem.put("resource", key);
                if (value.contains("#" + Constants.PUBLIC_CIRCLE)) {
                    value = "#" + Constants.PUBLIC_CIRCLE;
                }
                if (value.contains("#" + Constants.ME_CIRCLE)) {
                    value = viewerId;
                }
                privacyItem.put("auths", value);
                privacyItemList.add(privacyItem);
            }
        }

        return accountLogic.setPrivacy(ctx, viewerId, privacyItemList);
    }

    @WebMethod("privacy/get")
    public Record getViewerPrivacyConfig(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        AccountLogic accountLogic = GlobalLogics.getAccount();
        String viewerId = ctx.getViewerIdString();
        return accountLogic.getViewerPrivacyConfig(ctx, viewerId, qp.checkGetString("resources"));
    }


    @WebMethod("v2/internal/updateAccount")
    public boolean update(QueryParams qp, HttpServletRequest req) {
        Context ctx = WutongContext.getContext(qp, false);
        Long user_id = qp.checkGetInt("user");
        String ua = ctx.getUa();
        String lang = ctx.getLanguage();

        Record record = new Record();
        for (String str : qp.keySet()) {
            record.put(str, qp.getString(str, ""));
        }
        AccountLogic account = GlobalLogics.getAccount();
        User userOrg = account.getUser(ctx, user_id);
        User user0 = readUser(record, userOrg);
        boolean b = account.update(ctx, user0);


        if (b) {

            Record record0 = AccountConverter.converUser2Record(user0, null);
            Commons.sendNotification(ctx, Constants.NTF_PROFILE_UPDATE,
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(String.valueOf(user_id)),
                    Commons.createArrayNodeFromStrings(user0.toString(), user0.getDisplayName(), String.valueOf(user_id), lang),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(String.valueOf(user_id)),
                    Commons.createArrayNodeFromStrings(lang),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(String.valueOf(user_id)));
        }
        return b;
    }

    @WebMethod("v2/internal/getUsers")
    public JsonNode getUsers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);

        String users = qp.checkGetString("userIds");
        long[] longs = StringUtils2.splitIntArray(users, ",");

        String[] s = (String[]) ArrayUtils.addAll(StringUtils2.splitArray(qp.getString("cols", ""), ",", true), User.FULL_COLUMNS);
        String[] cols = CollectionsHelper.removeElements(s, new String[]{User.COL_PASSWORD, User.COL_DESTROYED_TIME});

        return JsonHelper.parse(UserHelper.usersToJson(getUsers0(ctx, qp, longs, users), cols, true));
    }

    @WebMethod("v2/internal/user/show")
    public JsonNode showUsersInteral(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);


        String userIds = qp.checkGetString("users");
        long[] longs = StringUtils2.splitIntArray(userIds, ",");

        String[] s = (String[]) ArrayUtils.addAll(StringUtils2.splitArray(qp.getString("columns", ""), ",", true), User.FULL_COLUMNS);
        String[] cols = CollectionsHelper.removeElements(s, new String[]{User.COL_PASSWORD, User.COL_DESTROYED_TIME});

        return JsonHelper.parse(UserHelper.usersToJson(getUsers0(ctx, qp, longs, userIds), cols, true));
        //return p.getUsers(viewerId, userIds, qp.getString("columns", Platform.USER_LIGHT_COLUMNS_USER_SHOW));
    }

    @WebMethod("v2/internal/getFriends")
    public JsonNode getFriends(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        FriendshipLogic friendshipLogic = GlobalLogics.getFriendship();
        String friendsId = friendshipLogic.getFriendsIdP(ctx,
                qp.checkGetString("userId"),
                qp.checkGetString("circleIds"),
                qp.checkGetString("cols"),
                (int) qp.checkGetInt("page"),
                (int) qp.checkGetInt("count"));

        if (StringUtils.isEmpty(friendsId))
            return null;

        long[] longs = StringUtils2.splitIntArray(friendsId, ",");

        String[] s = (String[]) ArrayUtils.addAll(StringUtils2.splitArray(qp.getString("cols", ""), ",", true), User.FULL_COLUMNS);
        String[] cols = CollectionsHelper.removeElements(s, new String[]{User.COL_PASSWORD, User.COL_DESTROYED_TIME});

        return JsonHelper.parse(UserHelper.usersToJson(getUsers0(ctx, qp, longs, friendsId), cols, true));

    }

    /*@WebMethod("account/testEmailAccountCreate")
    public String testEmail(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        MessageDelayCombineUtils.sendEmailCombineAndDelayAccountCreate(ctx, "10008", "xiaofei.luo@borqs.com");
        return "true";
    }

    @WebMethod("account/testEmailSuggestCreate")
    public String testEmail1(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        List<String> list = new ArrayList<String>();
        list.add("10405");
        MessageDelayCombineUtils.sendEmailCombineAndDelayRecommendUser(ctx, "10405", list);
        return "true";
    }

    @WebMethod("account/testEmailNewFollower")
    public String testEmail2(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        List<String> list = new ArrayList<String>();
        list.add("10405");
        list.add("10058");
        list.add("10001");
        MessageDelayCombineUtils.sendEmailCombineAndDelayNewFollower(ctx, ctx.getViewerIdString(), list);
        return "true";
    }

    @WebMethod("account/testEmailJoinGroup")
    public String testEmail3(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        MessageDelayCombineUtils.sendEmailCombineAndDelayJoinGroup(ctx, "10008", "10405", "11000000076", "户外旅行(08-22)");
        return "true";
    }*/

    private List<User> getUsers0(Context ctx, QueryParams qp, long[] userIds, String userId) {
        AccountLogic account = GlobalLogics.getAccount();
        List<User> users = account.getUsers(ctx, userIds);
        if (users.isEmpty())
            return null;

        Map<Long, User> map = new HashMap<Long, User>();
        map.clear();
        for (User u : users)
            map.put(u.getUserId(), u);


        RecordSet rs = account.getUsers(ctx, ctx.getViewerIdString(), userId, qp.getString("cols", ""), true);

        if (rs.isEmpty())
            return null;

        List<User> userList = new ArrayList<User>();
        for (Record r : rs) {
            long user_id = r.getInt("user_id");
            User user = map.get(user_id);
            users.get(0).toString();
            if (user == null)
                continue;

            user.setAddon("miscellaneous", r.getString("miscellaneous", ""));
            user.setAddon("bidi", r.getBoolean("bidi", false));
            user.setAddon("in_circles", r.getString("in_circles", "[]"));
            user.setAddon("his_friend", r.getBoolean("his_friend", false));
            user.setAddon("favorites_count", r.getInt("favorites_count", 1));
            user.setAddon("friends_count", r.getInt("friends_count", 0));
            user.setAddon("followers_count", r.getInt("followers_count", 0));
            user.setAddon("shared_count", r.getString("shared_count", ""));
            user.setAddon("profile_privacy", r.getBoolean("profile_privacy", false));
            user.setAddon("pedding_requests", r.getString("pedding_requests", "[]"));
            user.setAddon("profile_friends", r.getString("profile_friends", "[]"));
            user.setAddon("profile_followers", r.getString("profile_followers", "[]"));
            user.setAddon("profile_shared_photos", r.getString("profile_shared_photos", "[]"));
            userList.add(user);
        }
        addPrefix(userList);
        return userList;
    }

    private void addPrefix(List<User> data) {
        for (User user : data) {
            if (user != null) {
                PhotoInfo pi = user.getPhoto();
                if (pi != null)
                    pi.addUrlPrefix(prefix);
                else {
                    pi = new PhotoInfo();
                    pi.addDefualtUrlPrefix(sysPrefix);
                    user.setPhoto(pi);
                }
            }
        }
    }


    private static User readUser(Record req, User org) {
        User user = new User();
        user.setUserId(org.getUserId());
        if (req.has(User.COL_DISPLAY_NAME))
            user.setName(NameInfo.split(req.getString(User.COL_DISPLAY_NAME)));

        for (Schema.Column c : Schema.columns()) {
            String col = c.column;
            if (c.type == Schema.Column.Type.SIMPLE) {
                if (!req.has(col))
                    continue;

                Object value = Schema.parseSimpleValue(c.simpleType, req.getString(col));
                user.setProperty(col, value);
            } else if (c.type == Schema.Column.Type.OBJECT) {
                Map<String, String> strMap = getMap(col + ".", true, null, req);

                if (MapUtils.isEmpty(strMap))
                    continue;

                Object oldVal = org.getProperty(col, null);
                StringablePropertyBundle newVal = (StringablePropertyBundle) (oldVal != null ? ((Copyable) oldVal).copy() : c.newDefaultValue());
                Map<Integer, Object> props = toProperties(strMap, newVal.subMap());
                newVal.readProperties(props, true);
                user.setProperty(col, newVal);
            } else if (c.type == Schema.Column.Type.OBJECT_ARRAY || c.type == Schema.Column.Type.SIMPLE_ARRAY) {
                if (!req.has(col))
                    continue;

                JsonNode jn = JsonHelper.parse(req.getString(col));
                Object v = User.propertyFromJsonNode(c, jn);
                user.setProperty(col, v);
            }
        }
        return user;
    }

    public static Map<String, String> getMap(String keyPrefix, boolean removePrefix, Map<String, String> reuse, Record record) {
        if (reuse == null)
            reuse = new LinkedHashMap<String, String>();
        for (String key : record.keySet()) {
            if (key.startsWith(keyPrefix)) {
                String v = record.getString(key);
                if (removePrefix)
                    reuse.put(StringUtils.removeStart(key, keyPrefix), v);
                else
                    reuse.put(key, v);
            }
        }
        return reuse;
    }

    private static Map<Integer, Object> toProperties(Map<String, String> strMap, TextEnum te) {
        HashMap<Integer, Object> props = new HashMap<Integer, Object>();
        for (Map.Entry<String, String> e : strMap.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            Integer nk = te.getValue(k);
            if (nk != null)
                props.put(nk, v);
        }
        return props;
    }

    @WebMethod("user/show_test")
    public RecordSet showUsers_test(QueryParams qp) {
        System.out.println(DateUtils.nowMillis());
        Context ctx = WutongContext.getContext(qp, false);
        return GlobalLogics.getAccount().getUsersBaseColumnsContainsRemarkRequest(ctx,"10015","10000");
    }

}
