package com.borqs.server.intrapi.v1;

import com.borqs.server.ServerException;
import com.borqs.server.compatible.*;
import com.borqs.server.intrapi.InternalApiSupport;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.*;
import com.borqs.server.platform.feature.app.AppLogic;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.friend.Circles;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.login.LoginHelper;
import com.borqs.server.platform.feature.login.LoginResult;
import com.borqs.server.platform.util.*;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

@RoutePrefix("/internal")
@IgnoreDocument
public class Account1Api extends InternalApiSupport {
    private CibindLogic cibind;
    private AccountLogic account;
    private AppLogic app;

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

    public void setApp(AppLogic app) {
        this.app = app;
    }

    @Route(url = "/internal/verify_password")
    public long verifyPassword(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        String name = req.checkString("name");
        String pwd = req.checkString("pwd");

        long userId = cibind.whoBinding(ctx, name);
        if (req.getBoolean("encode_pwd", false))
            pwd = Encoders.md5Hex(pwd);

        if (userId == 0)
            return 0;

        account.getPassword(ctx, userId);
        String userPwd = account.getPassword(ctx, userId);
        if (StringUtils.equals(userPwd, pwd))
            return userId;

        return 0;
    }

    @Route(url = "/internal/getUsers")
    public String getUsers(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String userIds = req.checkString("userIds");
        String[] users = StringHelper.splitArray(userIds, ",", true);

        Users userList = account.getUsers(ctx, User.FULL_COLUMNS, ArrayHelper.stringArrayToLongArray(users));

        return CompatibleUser.usersToJson(userList, CompatibleUser.V1_FULL_COLUMNS, true);
    }

    @Route(url = "/internal/getFriends0")
    public List<Long> getFriends0(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.checkLong("userId");
        int[] circleIds = req.checkIntArray("circleIds", ",");

        long[] userList = friend.getBorqsFriendIdsInCircles(ctx, userId, circleIds);
        return CollectionsHelper.toLongList(userList);
    }

    @Route(url = "/internal/hasUser")
    public boolean hasUser(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.checkLong("userId");

        boolean b = account.hasUser(ctx, userId);
        return b;
    }

    @Route(url = "/internal/getFriends")
    public String getFriends(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.checkLong("userId");
        int[] circleIds = req.checkIntArray("circleIds", ",");
        Users users = friend.getBorqsFriendsInCircles(ctx, userId, circleIds, User.FULL_COLUMNS);

        return CompatibleUser.usersToJson(users, CompatibleUser.V1_FULL_COLUMNS, true);
    }

    @Route(url = "/internal/findUserIdByUserName")
    public long findUserIdByUserName(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String userName = req.checkString("username");

        return cibind.whoBinding(ctx, userName);
    }

    @Route(url = "/internal/updateAccount")
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

    }


    @Route(url = "/internal/checkTicket")
    public long checkTicket(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String ticket = req.checkString("ticket");

        return LoginHelper.checkTicket(login, ctx, ticket);
    }

    @Route(url = "/internal/checkSign")
    public void checkSign(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        int appId = req.checkInt("appid");
        String sign = req.checkString("sign");
        String signMethod = req.getString("sign_method", "md5");
        String validating = computeValidating(req);

        String secret = app.getApp(ctx, appId).getSecret();
        if (secret == null)
            throw new RuntimeException("App secret error");

        if (!"md5".equalsIgnoreCase(signMethod))
            throw new RuntimeException("Invalid sign method");

        String expectantSign = md5Sign(secret, validating);
        if (!StringUtils.equals(sign, expectantSign))
            throw new RuntimeException("Invalid md5 signatures");
    }

    @Route(url = "/internal/checkSignAndTicket")
    public long checkSignAndTicket(Request req, Response resp) {
        checkSign(req, resp);
        Context ctx = checkContext(req, true);
        String ticket = req.checkString("ticket");
        return LoginHelper.checkTicket(login, ctx, ticket);
    }


    @Route(url = "/internal/getUser")
    public String getUser(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.checkLong("userId");
        User user = account.getUser(ctx, User.FULL_COLUMNS, userId);

        return CompatibleUser.userToJson(user, CompatibleUser.V1_FULL_COLUMNS, true);
    }

    @Route(url = "/internal/getCircles")
    public String getCircles(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.checkLong("userId");
        boolean withUsers = req.checkBoolean("withUsers");
        int[] circleIds = req.checkIntArray("circleIds", ",");

        Circles circles = friend.getCircles(ctx, userId, circleIds, withUsers);

        return CompatibleCircle.circlesToJson(circles, CompatibleCircle.CIRCLE_COLUMNS_WITH_MEMBERS, true);
    }

    @Route(url = "/internal/login")
    public String login(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String name = req.checkString("name");
        String password = req.checkString("password");
        int app = req.checkInt("app");
        LoginResult lr = login.login(ctx, name, password, app);

        return lr.toString();
    }

    @Route(url = "/internal/logout")
    public boolean logout(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String ticket = req.checkString("ticket");
        boolean b = login.logout(ctx, ticket);

        return b;
    }

    public static String md5Sign(String appSecret, Collection<String> paramNames) {
        TreeSet<String> set = new TreeSet<String>(paramNames);
        set.remove("ticket");
        set.remove("appid");
        set.remove("sign");
        set.remove("sign_method");
        set.remove("callback");
        set.remove("_");
        return md5Sign(appSecret, StringUtils.join(set, ""));
    }

    public static String md5Sign(String appSecret, String s) {
        Validate.notNull(appSecret);
        Validate.notNull(s);
        return Encoders.md5Base64(appSecret + s + appSecret);
    }

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


    private static String computeValidating(Request qp) {
        TreeSet<String> paramNames = new TreeSet<String>(qp.getParams().keySet());
        paramNames.remove("ticket");
        paramNames.remove("appid");
        paramNames.remove("sign");
        paramNames.remove("sign_method");
        paramNames.remove("callback");
        paramNames.remove("_");
        return StringUtils.join(paramNames, "");
    }


}
