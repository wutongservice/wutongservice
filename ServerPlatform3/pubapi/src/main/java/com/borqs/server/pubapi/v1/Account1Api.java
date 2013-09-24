package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.compatible.*;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.*;
import com.borqs.server.platform.feature.app.AppSign;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.maker.Maker;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.status.Status;
import com.borqs.server.platform.feature.status.StatusLogic;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.*;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.util.sender.AsyncSender;
import com.borqs.server.platform.util.sender.email.AsyncMailSender;
import com.borqs.server.platform.util.sender.email.Mail;
import com.borqs.server.platform.util.sender.sms.AsyncProxyMessageSender;
import com.borqs.server.platform.util.sender.sms.Message;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;
import com.borqs.server.platform.util.template.FreeMarker;
import com.borqs.server.pubapi.i18n.PackageClass;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;


@IgnoreDocument
public class Account1Api extends PublicApiSupport {
    public static final FreeMarker FREE_MARKER = new FreeMarker(PackageClass.class);
    private AccountLogic account;
    private CibindLogic cibind;
    private String serverHost;

    private Maker<Mail> maker;
    private SFS profileImageSFS;

    private AsyncSender<Mail> mailSender;
    private AsyncSender<Message> smsSender;

    public Account1Api() {
    }

    public AsyncSender<Message> getSmsSender() {
        return smsSender;
    }

    public void setSmsSender(AsyncSender<Message> smsSender) {
        this.smsSender = smsSender;
    }

    public AsyncSender<Mail> getMailSender() {
        return mailSender;
    }

    public void setMailSender(AsyncSender<Mail> mailSender) {
        this.mailSender = mailSender;
    }

    public Maker<Mail> getMaker() {
        return maker;
    }

    public void setMaker(Maker<Mail> maker) {
        this.maker = maker;
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public SFS getProfileImageSFS() {
        return profileImageSFS;
    }

    public void setProfileImageSFS(SFS profileImageSFS) {
        this.profileImageSFS = profileImageSFS;
    }

    @Route(url = "/user/show")
    public void showUsers(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long[] userIds = req.checkLongArray("users", ",");
        String[] v1Cols = CompatibleUser.expandV1Columns(req.getStringArray("columns", ",", CompatibleUser.V1_LIGHT_COLUMNS));
        Users users = account.getUsers(ctx, CompatibleUser.v1ToV2Columns(v1Cols), userIds);
        resp.body(RawText.of(CompatibleUser.usersToJson(users, v1Cols, true)));
    }

    @Route(url = "/account/update")
    public void updateAccount(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        User org = account.getUser(ctx, User.FULL_COLUMNS, ctx.getViewer());
        if (org == null)
            throw new ServerException(E.INVALID_USER, "Invalid user id");

        LinkedHashSet<String> removedContactInfo = new LinkedHashSet<String>();
        User user = readUser(req, org, removedContactInfo);
        user.setUserId(ctx.getViewer());
        user.setPassword(null); // can't update password
        user.removeProperty(User.COL_PHOTO); // use /account/upload_profile_image
        account.update(ctx, user);
        if (!removedContactInfo.isEmpty()) {
            for (String ci : removedContactInfo)
                cibind.unbind(ctx, ci);
        }
        resp.body(true);
    }

    @SuppressWarnings("unchecked")
    private static User readUser(Request req, User org, Collection<String> removedContactInfo) {
        User user = new User(org.getUserId());
        if (req.has(CompatibleUser.V1COL_DISPLAY_NAME))
            user.setName(NameInfo.split(req.checkString(CompatibleUser.V1COL_DISPLAY_NAME)));

        boolean hasFirstName = req.has(CompatibleUser.V1COL_FIRST_NAME);
        boolean hasMiddleName = req.has(CompatibleUser.V1COL_MIDDLE_NAME);
        boolean hasLastName = req.has(CompatibleUser.V1COL_LAST_NAME);
        if (hasFirstName || hasMiddleName || hasLastName) {
            NameInfo name = org.getName();
            name = name != null ? name.copy() : new NameInfo();
            if (hasFirstName)
                name.setFirst(req.checkString(CompatibleUser.V1COL_FIRST_NAME));
            if (hasMiddleName)
                name.setMiddle(req.checkString(CompatibleUser.V1COL_MIDDLE_NAME));
            if (hasLastName)
                name.setLast(req.checkString(CompatibleUser.V1COL_LAST_NAME));
            user.setName(name);
        }

        boolean hasGender = req.has(CompatibleUser.V1COL_GENDER);
        boolean hasTimezone = req.has(CompatibleUser.V1COL_TIMEZONE);
        boolean hasInterests = req.has(CompatibleUser.V1COL_INTERESTS);
        boolean hasLanguages = req.has(CompatibleUser.V1COL_LANGUAGES);
        boolean hasMarriage = req.has(CompatibleUser.V1COL_MARRIAGE);
        boolean hasReligion = req.has(CompatibleUser.V1COL_RELIGION);
        boolean hasAboutMe = req.has(CompatibleUser.V1COL_ABOUT_ME);
        if (hasGender || hasTimezone || hasInterests || hasLanguages || hasMarriage || hasReligion || hasAboutMe) {
            ProfileInfo profile = org.getProfile();
            profile = profile != null ? profile.copy() : new ProfileInfo();
            if (hasGender)
                profile.setGender(req.checkString(CompatibleUser.V1COL_GENDER));
            if (hasTimezone)
                profile.setTimezone(req.checkString(CompatibleUser.V1COL_TIMEZONE));
            if (hasInterests)
                profile.setInterests(req.checkString(CompatibleUser.V1COL_INTERESTS));
            if (hasLanguages)
                profile.setLanguages(req.checkString(CompatibleUser.V1COL_LANGUAGES));
            if (hasMarriage)
                profile.setMarriage(req.checkString(CompatibleUser.V1COL_MARRIAGE));
            if (hasReligion)
                profile.setReligion(req.checkString(CompatibleUser.V1COL_RELIGION));
            if (hasAboutMe)
                profile.setDescription(req.checkString(CompatibleUser.V1COL_ABOUT_ME));
            user.setProfile(profile);
        }

        if (req.has(CompatibleUser.V1COL_BIRTHDAY)) {
            String birthday = req.checkString(CompatibleUser.V1COL_BIRTHDAY);
            List<DateInfo> dates = org.getDate();
            if (dates == null) {
                dates = new ArrayList<DateInfo>();
                dates.add(new DateInfo(DateInfo.TYPE_BIRTHDAY, birthday));
            } else {
                dates = new ArrayList<DateInfo>(dates);
                boolean updated = false;
                for (DateInfo di : dates) {
                    if (di != null && DateInfo.TYPE_BIRTHDAY.equals(di.getType())) {
                        di.setInfo(birthday);
                        updated = true;
                    }
                }
                if (!updated)
                    dates.add(new DateInfo(DateInfo.TYPE_BIRTHDAY, birthday));
            }
            user.setDate(dates);
        }

        boolean hasCompany = req.has(CompatibleUser.V1COL_COMPANY);
        boolean hasDepartment = req.has(CompatibleUser.V1COL_DEPARTMENT);
        boolean hasJobTitle = req.has(CompatibleUser.V1COL_JOB_TITLE);
        boolean hasOfficeAddress = req.has(CompatibleUser.V1COL_OFFICE_ADDRESS);
        boolean hasJobDesc = req.has(CompatibleUser.V1COL_JOB_DESCRIPTION);
        if (hasCompany || hasDepartment || hasJobTitle || hasOfficeAddress || hasJobDesc) {
            List<OrgInfo> orgs = org.getOrganization();
            orgs = orgs != null ? new ArrayList<OrgInfo>(orgs) : new ArrayList<OrgInfo>();
            OrgInfo oi = new OrgInfo();
            oi.setType(OrgInfo.TYPE_WORK);
            if (hasCompany)
                oi.setCompany(req.checkString(CompatibleUser.V1COL_COMPANY));
            if (hasDepartment)
                oi.setDepartment(req.checkString(CompatibleUser.V1COL_DEPARTMENT));
            if (hasJobTitle)
                oi.setTitle(req.checkString(CompatibleUser.V1COL_JOB_TITLE));
            if (hasOfficeAddress)
                oi.setOfficeLocation(req.checkString(CompatibleUser.V1COL_OFFICE_ADDRESS));
            if (hasJobDesc)
                oi.setJobDescription(req.checkString(CompatibleUser.V1COL_JOB_DESCRIPTION));

            if (orgs.isEmpty())
                orgs.add(oi);
            else
                orgs.set(0, oi);

            user.setOrganization(orgs);
        }

        if (req.has(CompatibleUser.V1COL_ADDRESS)) {
            String json = req.checkString(CompatibleUser.V1COL_ADDRESS);
            List<AddressInfo> addrs = CompatibleAddressInfo.jsonToAddressInfos(new ArrayList<AddressInfo>(), json);
            user.setAddress(addrs);
        }

        if (req.has(CompatibleUser.V1COL_WORK_HISTORY)) {
            String json = req.checkString(CompatibleUser.V1COL_WORK_HISTORY);
            List<WorkHistory> whs = CompatibleWorkHistory.jsonToWorkHistories(new ArrayList<WorkHistory>(), json);
            user.setWorkHistory(whs);
        }

        if (req.has(CompatibleUser.V1COL_EDUCATION_HISTORY)) {
            String json = req.checkString(CompatibleUser.V1COL_EDUCATION_HISTORY);
            List<EduHistory> ehs = CompatibleEduHistory.jsonToEduHistories(new ArrayList<EduHistory>(), json);
            user.setEducationHistory(ehs);
        }

        // misc
        if (req.has(CompatibleUser.V1COL_CONTACT_INFO)) {
            String json = req.checkString(CompatibleUser.V1COL_CONTACT_INFO);
            Map<String, String> ci = JsonHelper.fromJson(json, HashMap.class);
            if (!ci.isEmpty()) {
                ObjectHolder<List<TelInfo>> tels = new ObjectHolder<List<TelInfo>>(org.getTel());
                ObjectHolder<List<EmailInfo>> emails = new ObjectHolder<List<EmailInfo>>(org.getEmail());
                ObjectHolder<List<ImInfo>> ims = new ObjectHolder<List<ImInfo>>(org.getIm());
                ObjectHolder<List<SipAddressInfo>> sis = new ObjectHolder<List<SipAddressInfo>>(org.getSipAddress());

                CompatibleContactInfo.fromContactInfo(ci, removedContactInfo, tels, emails, ims, sis);
                user.setTel(tels.value != null ? tels.value : new ArrayList<TelInfo>());
                user.setEmail(emails.value != null ? emails.value : new ArrayList<EmailInfo>());
                user.setIm(ims.value != null ? ims.value : new ArrayList<ImInfo>());
                user.setSipAddress(sis.value != null ? sis.value : new ArrayList<SipAddressInfo>());
            }
        }

        return user;
    }

    @Route(url = "/account/change_password")
    public void resetPassword(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        account.updatePassword(ctx, req.checkString("oldPassword"), req.checkString("newPassword"), true);
        resp.body(true);
    }


    @Route(url = "/account/openface/phone")
    public void setOpenfacePhone(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long userId = req.checkLong("userid");
        String phone = req.checkString("phone");
        User user = account.getUser(ctx, CompatibleUser.v1ToV2Columns(new String[]{CompatibleUser.V1COL_MISCELLANEOUS}), userId);
        MiscInfo miscInfo = user.getMiscellaneous();
        miscInfo.setOpenfacePhone(phone);
        user.setMiscellaneous(miscInfo);
        account.update(ctx, user);
        resp.body(true);
    }

    @Route(url = "/account/openface/user_id")
    public void getOpenfaceUserIdByPhone(Request req, Response resp) {
        // TODO: xx
        throw new ServerException(E.UNSUPPORTED, "Unsupported");
    }

    @Route(url = "/account/search")
    public void search(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String word = req.checkString("username");
        String[] v1Cols = CompatibleUser.expandV1Columns(req.getStringArray("columns", ",", CompatibleUser.V1_LIGHT_COLUMNS));
        Users users = account.search(ctx, word, CompatibleUser.v1ToV2Columns(v1Cols), new Page(0, 100));
        resp.body(RawText.of(CompatibleUser.usersToJson(users, v1Cols, true)));
    }

    @Route(url = "/account/upload_profile_image")
    public void uploadProfileImage(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        FileItem imageFileItem = req.checkFile("profile_image");
        // TODO: photo and album
        PhotoInfo info = ProfileImageHelper.saveUploadedProfileImage(ctx, profileImageSFS, ctx.getViewer(), DateHelper.nowMillis(), imageFileItem);
        User user = new User(ctx.getViewer());
        user.setPhoto(info);
        boolean b = account.update(ctx, user);
        resp.body(b);
    }

//    @Route(url = "/user/profile_image")
//    public void downloadProfileImage(Request req, Response resp) {
//        throw new ServerException(E.UNSUPPORTED, "Unsupported");
//    }

    @Route(url = "/account/bind")
    public void bind(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long viewerId = ctx.getViewer();
        String phone = req.getString("phone", "");
        String email = req.getString("email", "");
        String ticket = req.checkString("ticket");

        if (StringUtils.isBlank(phone) && StringUtils.isBlank(email))
            throw new ServerException(E.PARAM, "Must have parameter 'phone' or 'email'");
        if (StringUtils.isNotBlank(phone) && StringUtils.isNotBlank(email))
            throw new ServerException(E.PARAM, "only can bind 'phone' or 'email' one time");
        if (StringUtils.isNotBlank(phone) && !StringValidator.validatePhone(phone))
            throw new ServerException(E.PARAM, "'phone' error");
        if (StringUtils.isNotBlank(email) && !StringValidator.validateEmail(email))
            throw new ServerException(E.PARAM, "'email' error");

        String key = req.getString("key", "");
        User viewer = account.getUser(ctx, CompatibleUser.v1ToV2Columns(new String[]{CompatibleUser.V1COL_USER_ID,
                CompatibleUser.V1COL_CONTACT_INFO, CompatibleUser.V1COL_DISPLAY_NAME}), viewerId);

        if (StringUtils.isNotBlank(phone) && cibind.hasBinding(ctx, phone))
            throw new ServerException(E.BINDING_EXISTS, "has binded");
        if (StringUtils.isNotBlank(email) && cibind.hasBinding(ctx, email))
            throw new ServerException(E.BINDING_EXISTS, "has binded");

        if (StringUtils.isBlank(key)) {
            if (StringUtils.isNotBlank(email) && StringUtils.isBlank(phone))
                sendVerifyEmail(ctx, ticket, viewer, email);
            else
                sendVerifySms(ctx, ticket, viewer, phone);

            resp.body(true);
        } else {
            key = StringUtils.replace(key, " ", "+");
            String fp = FeedbackParams.fromBase64(key).get("param");
            String[] ss = StringHelper.splitArray(fp, "/", 3, false);
            String userId = ss[0];
            phone = ss[1];
            email = ss[2];

            String bindType = BindingInfo.MOBILE_TEL;
            String bindInfo = phone;
            if (StringUtils.isNotBlank(email) && StringUtils.isBlank(phone)) {
                bindType = BindingInfo.EMAIL;
                bindInfo = email;
            }
            cibind.bind(ctx, bindType, bindInfo);

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
    }

    private void sendVerifyEmail(Context ctx, String ticket, User user, String email) {
        //update contact info
        List<EmailInfo> el = user.getEmail();
        el.add(new EmailInfo(EmailInfo.TYPE_OTHER, email));
        user.setEmail(el);

        //send verify email
        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);

        String displayName = user.getDisplayName();
        long userId = user.getUserId();
        String template = bundle.getString("accountapi.email.bind.title");
        String subject = FREE_MARKER.mergeRaw(template, new Object[][]{
                {"userName", displayName}
        });

        String phone = "";
        String url = "http://" + serverHost + "/account/bind?ticket=" + ticket + "&";
        String param = "userId=" + userId + "&phone=" + phone + "&email=" + email + "&key=";

        String gkey = userId + "/" + phone + "/" + email;
        FeedbackParams fp = new FeedbackParams().set("param", gkey);
        String b2 = fp.toBase64(true);
        url = url + param + b2;

        String link = "<a href=" + url + " target=_blank>" + url + "</a>";

        template = bundle.getString("accountapi.email.bind.content");
        String message = FREE_MARKER.mergeRaw(template, new Object[][]{
                {"userName", displayName},
                {"userId", userId},
                {"email", email},
                {"link", link}
        });
        Mail mail = maker.make(ctx, MakerTemplates.EMAIL_ESSENTIAL, new Object[][] {
                {"from", mailSender instanceof AsyncMailSender ? ((AsyncMailSender)mailSender).getSmtpUsername() : ""},
                {"to", email},
                {"subject", subject},
                {"serverHost", serverHost},
                {"content", message}
        });
        mailSender.asyncSend(mail);
    }

    private void sendVerifySms(Context ctx, String ticket, User user, String phone) {
        //update contact info
        List<TelInfo> tl = user.getTel();
        tl.add(new TelInfo(TelInfo.TYPE_OTHER, phone));
        user.setTel(tl);

        //send verify sms
        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);
        String content = bundle.getString("accountapi.phone.bind.content");

        long userId = user.getUserId();
        String email = "";
        String param = "/account/bind?ticket=" + ticket + "&phone=" + phone + "&email=" + email + "&key=";
        String gkey = userId + "/" + phone + "/" + email;
        FeedbackParams fp = new FeedbackParams().set("param", gkey);
        String b2 = fp.toBase64(true);
        String url = "http://" + serverHost + param + b2;

        //TODO: short url
        smsSender.asyncSend(Message.forSend(phone, content + url));
    }
}
