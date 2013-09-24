package com.borqs.server.service.platform;


import com.borqs.server.ErrorCode;
import com.borqs.server.ServerException;
import com.borqs.server.base.ResponseError;
import com.borqs.server.base.auth.AuthException;
import com.borqs.server.base.auth.WebSignatures;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.RecordsExtenders;
import com.borqs.server.base.data.RecordsProducer;
import com.borqs.server.base.memcache.XMemcached;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.rpc.RPCClient;
import com.borqs.server.base.rpc.TransceiverFactory;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.service.SendMail;
import com.borqs.server.service.notification.*;
import com.borqs.server.service.platform.company.CompanyLogic;
import com.borqs.server.service.platform.event.PlatformHooks;
import com.borqs.server.service.platform.event.theme.EventThemeLogic;
import com.borqs.server.service.platform.extender.PlatformExtender;
import com.borqs.server.service.platform.template.InnovTemplate;
import com.borqs.server.service.qiupu.QiupuInterface;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.Transceiver;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.*;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;

import static com.borqs.server.base.util.ShortUrl.ShortText;
import static com.borqs.server.service.platform.Constants.*;

public class Platform extends RPCClient {
    private static final Logger L = LoggerFactory.getLogger(Platform.class);
    private static final PageTemplate pageTemplate = new PageTemplate(InnovTemplate.class);

    public static final String USER_ALL_COLUMNS =
            "user_id, password, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name,  remark,display_name,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time, first_name, middle_name, last_name, gender, birthday, timezone, interests, languages, marriage, religion, about_me, profile_updated_time, company, department, job_title, office_address, profession, job_description, business_updated_time, contact_info, contact_info_updated_time, family, coworker, address, address_updated_time, work_history, work_history_updated_time, education_history, education_history_updated_time, miscellaneous, in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history";

    public static final String USER_FULL_COLUMNS =
            "user_id,login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name, display_name, remark,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time, first_name, middle_name, last_name, gender, birthday, timezone, interests, languages, marriage, religion, about_me, profile_updated_time, company, department, job_title, office_address, profession, job_description, business_updated_time, contact_info, contact_info_updated_time, family, coworker, address, address_updated_time, work_history, work_history_updated_time, education_history, education_history_updated_time, miscellaneous, in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history";

    public static final String USER_STANDARD_COLUMNS =
            "user_id,login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name, display_name, remark,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time,gender, birthday,company, department, job_title, office_address, profession, job_description,  contact_info,  family,  address,   work_history_updated_time, miscellaneous,  in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history,top_posts,top_name";


    public static final String USER_LIGHT_COLUMNS =
            "user_id, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3,display_name,perhaps_name, image_url, small_image_url, large_image_url, remark, in_circles, his_friend, bidi";
    public static final String USER_LIGHT_COLUMNS_USER_SHOW =
            "user_id, display_name, image_url, remark, in_circles, his_friend, bidi,perhaps_name";

    public static final String USER_LIGHT_COLUMNS_QIUPU =
            "user_id, display_name, image_url, address,perhaps_name";

    public static final String USER_LIGHT_COLUMNS_LIGHT =
            "user_id, display_name, image_url,perhaps_name";

    public static final String USER_COLUMNS_SHAK =
            "user_id, display_name, remark,perhaps_name,image_url, status, gender, in_circles, his_friend, bidi";

    public static final long DEFAULT_IGNORE_BACK_DATE = 24L * 60 * 60 * 30 * 1000;

    public static String bucketName = "wutong-data";
    public static String bucketName_photo_key = "media/photo/" ;
    public static String bucketName_video_key = "media/video/" ;
    public static String bucketName_audio_key = "media/audio/" ;
    public static String bucketName_static_file_key = "files/" ;
    public static int REPORT_ABUSE_COUNT = 3 ;

    private String qiupuUid;

    private static String expandColumns(String cols, Map<String, String> macros, String def) {
        StringBuilder buff = new StringBuilder();
        for (String col : StringUtils2.splitList(cols, ",", true)) {
            if (col.startsWith("#")) {
                String val = macros.get(StringUtils.removeStart(col, "#"));
                if (val == null)
                    val = def;
                buff.append(val);
            } else {
                buff.append(col);
            }
            buff.append(",");
        }
        return StringUtils2.stripItems(buff.toString(), ",", true);
    }

    private static final Map<String, String> USER_COLUMNS = CollectionUtils2.of(
            "light", USER_LIGHT_COLUMNS,
            "full", USER_STANDARD_COLUMNS);

    public static String parseUserColumns(String cols) {
        return expandColumns(cols, USER_COLUMNS, USER_LIGHT_COLUMNS);
    }


    public String parseUserIds(String viewerId, String userIds) throws AvroRemoteException {
        StringBuilder buff = new StringBuilder();
        for (String userId : StringUtils2.splitList(userIds, ",", true)) {
            if (userId.startsWith("#")) {
                if (!viewerId.equals("0") && !viewerId.equals("")) {
                    String circleId = StringUtils.removeStart(userId, "#");
                    RecordSet friendRecs = getFriends0(viewerId, circleId, 0, -1);
                    buff.append(friendRecs.joinColumnValues("friend", ","));
                }
            } else {
                buff.append(userId);
//                if (hasUser(userId)) {
//                    buff.append(userId);
//                }
            }
            buff.append(",");
        }
        return StringUtils2.stripItems(buff.toString(), ",", true);
    }

    public List<String> getGroupIdsFromMentions(List<String> mentions) throws AvroRemoteException {
        ArrayList<String> groupIds = new ArrayList<String>();
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);

            for (String mention : mentions) {
                if (StringUtils.startsWith(mention, "#"))
                    mention = StringUtils.substringAfter(mention, "#");

                long id = 0;
                try {
                    id = Long.parseLong(mention);
                }
                catch (NumberFormatException nfe) {
                    continue;
                }
                if (group.isGroup(id))
                    groupIds.add(String.valueOf(id));
            }

            return groupIds;
        } catch (ResponseError e) {
            throw new ServerException(e.code, toStr(e.message));
        } finally {
            closeTransceiver(trans);
        }
    }

    public static final String POST_FULL_COLUMNS = "post_id, source, created_time, updated_time, " +
            "quote, root, mentions, app, type, app_data, message, device, can_comment, can_like,can_reshare,add_to,privince, attachments,destroyed_time,target,location,add_contact,has_contact,longitude,latitude";


    private static final Map<String, String> POST_COLUMNS = CollectionUtils2.of(
            "full", POST_FULL_COLUMNS);

    public static String parsePostColumns(String cols) {
        return expandColumns(cols, POST_COLUMNS, POST_FULL_COLUMNS);
    }

    private String SERVER_HOST = "api.borqs.com";
    private String smsHost = null;
    private boolean sendSms = false;
    private boolean sendEmail = false;

    private final PlatformHooks hooks = new PlatformHooks();

    public Platform(TransceiverFactory tf) {
        super(tf);
    }

    private static String firstId(String ids) {
        return StringUtils.substringBefore(ids, ",").trim();
    }

    @Override
    public void setConfig(Configuration conf) {
//        L.trace("set config 1");
        super.setConfig(conf);
//    	L.trace("set config 2");
        hooks.addHooksInConfig(conf, "platform.hooks");
//        L.trace("set config 3");
        SERVER_HOST = conf.getString("server.host", "api.borqs.com");
        smsHost = conf.getString("phoneVerification.smsHost", null);
        sendSms = conf.getBoolean("platform.share.sms", false);
        sendEmail = conf.getBoolean("platform.share.email", false);
        qiupuUid = conf.getString("qiupu.uid", "102");
    }

    private static String computeValidating(QueryParams qp) {
        TreeSet<String> paramNames = new TreeSet<String>(qp.keySet());
        paramNames.remove("ticket");
        paramNames.remove("appid");
        paramNames.remove("sign");
        paramNames.remove("sign_method");
        paramNames.remove("callback");
        paramNames.remove("_");
        return StringUtils.join(paramNames, "");
    }

    private static String checkGetAuthParam(QueryParams qp, String p) {
        String s = qp.getString(p, null);
        if (s == null)
            throw new AuthException("Missing '%s'", p);
        return s;
    }

    public String checkTicket(QueryParams qp) throws AvroRemoteException {
        String userId = whoLogined(checkGetAuthParam(qp, "ticket"));
        if (Constants.isNullUserId(userId))
            throw new AuthException("Invalid ticket");
        return userId;
    }

    public void checkSign(QueryParams qp) throws AvroRemoteException {
        String appId = checkGetAuthParam(qp, "appid");
        String sign = checkGetAuthParam(qp, "sign");
        String signMethod = qp.getString("sign_method", "md5");
        String validating = computeValidating(qp);

        String secret = getAppSecret(appId);
        if (secret == null)
            throw new AuthException("App secret error");

        if (!"md5".equalsIgnoreCase(signMethod))
            throw new AuthException("Invalid sign method");

        String expectantSign = WebSignatures.md5Sign(secret, validating);
        if (!StringUtils.equals(sign, expectantSign))
            throw new AuthException("Invalid md5 signatures");
    }

    public String checkSignAndTicket(QueryParams qp) throws AvroRemoteException {
        checkSign(qp);
        return checkTicket(qp);
    }

    public void checkConfigurationId(QueryParams qp){
        String viewerId = qp.checkGetString("id");
        Configuration conf = getConfig();
        String configId = conf.getString("configuration.internal.id", "");
        if (configId.indexOf(viewerId) == -1)
            throw new AuthException("id is error!");
    }

    // Application

    public String getAppSecret(String appId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Application.class);
        try {
            Application app = getProxy(Application.class, trans);
            return toStr(app.getAppSecret(appId));
        } finally {
            closeTransceiver(trans);
        }
    }

    public List<String> apiList() {
        ConnectionFactory connectionFactory = ConnectionFactory.getConnectionFactory("dbcp");
        String db = "mysql/192.168.5.22/apidoc/root/111111";

        String sql = "SELECT id FROM api";
        SQLExecutor se = new SQLExecutor(connectionFactory, db);
        RecordSet recs = se.executeRecordSet(sql, null);
        connectionFactory = ConnectionFactory.close(connectionFactory);

        List<String> l = new ArrayList<String>();
        for (Record rec : recs) {
            String id = rec.getString("id");
            l.add(id);
        }

        return l;
    }

    public JsonNode createArrayNodeFromStrings(String... args) {
        List<String> l = new ArrayList<String>();

        if ((args != null) && (args.length > 0)) {
            for (String arg : args) {
                l.add(arg);
            }
        }

        return JsonUtils.parse(JsonUtils.toJson(l, false));
    }

    public void sendGroupNotification(long groupId, GroupNotificationSender sender,
                                      String senderId, Object[] scopeArgs, String bodyArg, String... titleArgs) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);

            List<Long> scope = sender.getScope(senderId, scopeArgs);
            String title = sender.getTitle(titleArgs);
            String body = sender.getBody(bodyArg);
            String message = title;
            String emailContent = title;
            if (StringUtils.isNotBlank(body)) {
                if (sender instanceof SharedNotifSender) {
                    String who = StringUtils.substringBefore(title, "在");
                    String groupName = StringUtils.substringBetween(title, "【", "】");
                    message = body + " 来自" + who + "[" + groupName + "]";
                }
                else
                    message += ":" + body;
                emailContent += ":<br>\t\t" + body;
            }

            RecordSet recs = RecordSet.fromByteBuffer(group.getMembersNotification(groupId, StringUtils2.joinIgnoreBlank(",", scope)));
            for (Record rec : recs) {
                String userId = rec.getString("member");
                long recvNotif = rec.getInt("recv_notif", 0);
                String notifEmail = rec.getString("notif_email", "");
                String notifPhone = rec.getString("notif_phone", "");
                if (recvNotif == 1) {
                    if (StringUtils.isNotBlank(notifEmail))
                        sendEmail("您有来自公共圈子新的消息", notifEmail, notifEmail, emailContent, Constants.EMAIL_ESSENTIAL, "zh");
                    if (StringUtils.isNotBlank(notifPhone))
                        sendSms(notifPhone, message);
                }
            }
        } finally {
            closeTransceiver(trans);
        }
    }

    public void sendNotification(String nType, JsonNode appId, JsonNode senderId, JsonNode title,
                                  JsonNode action, JsonNode type, JsonNode uri, JsonNode titleHtml,
                                  JsonNode body, JsonNode bodyHtml, JsonNode objectId, JsonNode scope) {
        Record rec = new Record();
        rec.put("nType", nType);
        rec.put("appId", appId);
        rec.put("senderId", senderId);
        rec.put("title", title);
        rec.put("action", action);
        rec.put("type", type);
        rec.put("uri", uri);
        rec.put("titleHtml", titleHtml);
        rec.put("body", body);
        rec.put("bodyHtml", bodyHtml);
        rec.put("objectId", objectId);
        rec.put("scope", scope);

        String rec_str = rec.toString(false, false);
        MQ mq = MQCollection.getMQ("platform");
        if ((mq != null) && (rec_str.length() < 1024))
            try {
                mq.send("notif", rec_str);
            } catch (Exception e) {
                L.debug("send Notif error begin #############################:" + e.toString());
            }
    }

    // Account
    public Record login(String name, String password, String appId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return Record.fromByteBuffer(account.login(name, password, appId));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record directLogin(String name, String password, String appId) throws AvroRemoteException {
        return login(name, Encoders.md5Hex(password), appId);
    }

    public boolean logout(String ticket) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return account.logout(ticket);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean changePassword(String userId, String oldPassword, String newPassword) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            //1,check oldPassword
            Record r = RecordSet.fromByteBuffer(account.getUsersPasswordByUserIds(userId)).getFirstRecord();
            if (r.isEmpty())
                throw new AuthException("Invalid userId!");
            if (!r.getString("password").equalsIgnoreCase(oldPassword))
                throw new AuthException("Invalid old password!");
            //update newPassword

            return account.changePasswordByUserId(userId, newPassword);
        } finally {
            closeTransceiver(trans);
        }
    }


    public String whoLogined(String ticket) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return toStr(account.whoLogined(ticket));
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean signIn(String userId, String longitude, String latitude, String altitude, String speed, String geo,int type) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SignIn.class);
        try {
            SignIn s = getProxy(SignIn.class, trans);
            Record r = new Record();
            r.put("user_id", userId);
            r.put("longitude", longitude);
            r.put("latitude", latitude);
            r.put("altitude", altitude);
            r.put("speed", speed);
            r.put("geo", geo);
            r.put("type", type);
            return s.saveSignIn(r.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getSignIn(String userId, boolean asc, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SignIn.class);
        try {
            SignIn s = getProxy(SignIn.class, trans);
            return RecordSet.fromByteBuffer(s.getSignIn(userId, asc, page, count));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getUserShaking(String userId,String longitude0,String latitude0, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SignIn.class);
        try {
            SignIn s = getProxy(SignIn.class, trans);
            long dateDiff = 10 * 60 * 1000L;

            double longitude_me = Double.parseDouble(longitude0);
            double latitude_me = Double.parseDouble(latitude0);

            RecordSet recs = RecordSet.fromByteBuffer(s.getUserShaking(userId, dateDiff, false, 0, 1000));
            // 找同时在摇的1000个人出来
            if (recs.size() > 0) {
                for (Record rec : recs) {
                    //每个人的距离算出来
                    double longitude = Double.parseDouble(rec.getString("longitude"));
                    double latitude = Double.parseDouble(rec.getString("latitude"));
                    double distance = GetDistance(longitude_me, latitude_me, longitude, latitude);
                    rec.put("distance", distance);
                }
                recs.sort("distance", true);
                recs.sliceByPage(page, count);

                for (Record rec1 : recs) {
                    Record user = getUser(userId, rec1.getString("user_id"), USER_COLUMNS_SHAK, true);
                    user.copyTo(rec1);
                }
            }
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getUserNearBy(String userId,String longitude0,String latitude0, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SignIn.class);
        try {
            SignIn s = getProxy(SignIn.class, trans);

            double longitude_me = Double.parseDouble(longitude0);
            double latitude_me = Double.parseDouble(latitude0);

            RecordSet recs = RecordSet.fromByteBuffer(s.getUserNearBy(userId,  0, 1000));
            // 找最近签到的1000个人出来
            if (recs.size() > 0) {
                for (Record rec : recs) {
                    //每个人的距离算出来
                    double longitude = Double.parseDouble(rec.getString("longitude"));
                    double latitude = Double.parseDouble(rec.getString("latitude"));
                    double distance = GetDistance(longitude_me, latitude_me, longitude, latitude);
                    rec.put("distance", distance);
                }
                recs.sort("distance", true);
                recs.sliceByPage(page, count);

                for (Record rec1 : recs) {
                    Record user = getUser(userId, rec1.getString("user_id"), USER_COLUMNS_SHAK, true);
                    user.copyTo(rec1);
                }
            }
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean deleteSignIn(String sign_ids) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SignIn.class);
        try {
            SignIn s = getProxy(SignIn.class, trans);
            return s.deleteSignIn(sign_ids);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean ticketExists(String ticket) throws AvroRemoteException {
        return !Constants.isNullUserId(whoLogined(ticket));
    }

    public RecordSet getLogined(String userId, String appId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return RecordSet.fromByteBuffer(account.getLogined(userId, appId));
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean checkLoginNameNotExists(String uid, String... names) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            String sNames = StringUtils.join(names, ",");
            return account.checkLoginNameNotExists(uid, sNames);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean checkBindNameNotExists(String... names) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            String sNames = StringUtils.join(names, ",");
            return account.checkBindNameNotExists(sNames);
        } finally {
            closeTransceiver(trans);
        }
    }

    public String createAccount(Record info) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            String userId = toStr(account.createAccount(info.toByteBuffer()));
            createBuiltinCircles(userId);
            Record r = info.copy();
            r.set("user_id", userId);
            String email = info.getString("login_email1", "");
            if (StringUtils.isNotEmpty(email)) {
                onBindEmain(Long.parseLong(userId), email);
            }
            hooks.fireUserCreated(r);
            return userId;
        } catch (ResponseError e) {
            throw new ServerException(e.code, toStr(e.message));
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getBorqsUserIds() throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return toStr(account.getBorqsUserIds());
        } catch (ResponseError e) {
            throw new ServerException(e.code, toStr(e.message));
        } finally {
            closeTransceiver(trans);
        }
    }

    //=====================================conversation begin ======================================
    public boolean createConversation(int target_type, String target_id, int reason, String fromUsers) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Conversation.class);
        try {
            Conversation c = getProxy(Conversation.class, trans);
            List<String> l = StringUtils2.splitList(fromUsers, ",", true);
            boolean b = true;
            for (String l0 : l) {
                if (!l0.equals("0") && l0.length() < 12) {
                    Record r0 = new Record();
                    r0.put("target_type", target_type);
                    r0.put("target_id", target_id);
                    r0.put("reason", reason);
                    r0.put("from_", l0);
                    c.createConversation(r0.toByteBuffer());
                }
            }
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateConversationTarget(String old_target_id, String new_target_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Conversation.class);
        try {
            Conversation c = getProxy(Conversation.class, trans);
            return c.updateConversationTarget(old_target_id, new_target_id);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateCommentTarget(String target_type, String old_target, String new_target) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            Comment c = getProxy(Comment.class, trans);
            old_target = target_type + ":" + old_target;
            new_target = target_type + ":" + new_target;
            return c.updateCommentTarget(old_target, new_target);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateLikeTarget(String target_type, String old_target, String new_target) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like l = getProxy(Like.class, trans);
            old_target = target_type + ":" + old_target;
            new_target = target_type + ":" + new_target;
            return l.updateLikeTarget(old_target, new_target);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean deleteConversation(int target_type, String target_id, int reason, long from) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Conversation.class);
        try {
            Conversation c = getProxy(Conversation.class, trans);

            int flag = 0;
            if ((target_type == Constants.POST_OBJECT && reason == Constants.C_STREAM_COMMENT) ||
                    (target_type == Constants.APK_OBJECT && reason == Constants.C_APK_COMMENT) ||
                    (target_type == Constants.PHOTO_OBJECT && reason == Constants.C_PHOTO_COMMENT)) {
                if (getIHasCommented(String.valueOf(from), target_type, target_id))
                    flag = 1;
            }

            if (flag == 0)
                c.deleteConversation(target_type, target_id, reason, from);

            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getConversation(int target_type, String target_id, List<String> reasons, long from, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Conversation.class);
        try {
            Conversation c = getProxy(Conversation.class, trans);
            return RecordSet.fromByteBuffer(c.getConversation(target_type, target_id, StringUtils.join(reasons, ","), from, page, count));
        } finally {
            closeTransceiver(trans);
        }
    }

    //=====================================conversation end ======================================
    public String createAccountWithoutNotif(String login_email1, String login_phone1, String pwd,
                                            String displayName, String nickName, String gender, String imei, String imsi) throws IOException {
        Record rec = Record.of("password", pwd, "display_name", displayName);
        rec.putIf("login_email1", login_email1, StringUtils.isNotBlank(login_email1));
        rec.putIf("login_phone1", login_phone1, StringUtils.isNotBlank(login_phone1));
        rec.putIf("gender", gender, StringUtils.isNotBlank(gender));
        rec.putIf("nick_name", nickName, StringUtils.isNotBlank(nickName));
        if (StringUtils.isNotBlank(imei) || StringUtils.isNotBlank(imsi)) {
            Record miscellaneous = Record.of("imei", imei, "imsi", imsi);
            rec.put("miscellaneous", miscellaneous.toJsonNode());
        }
        String userId = createAccount(rec);

        //add friend
        setFriendsTemp(userId, qiupuUid, String.valueOf(Constants.DEFAULT_CIRCLE), Constants.FRIEND_REASON_AUTOCREATE, true);
        try {
            autoCreateSuggestusers(userId);
            Record values = Record.of("socialcontact.autoaddfriend", "100");
            setPreferences(userId, values);
        } catch (Exception ex) {
            ex.printStackTrace();
            L.trace(ex.getMessage());
        }

        return userId;
    }

    public String createAccount(String login_email1, String login_phone1, String pwd,
                                String displayName, String nickName, String gender, String imei, String imsi, String device, String location) throws IOException {
        Record rec = Record.of("password", pwd, "display_name", displayName);
        rec.putIf("login_email1", login_email1, StringUtils.isNotBlank(login_email1));
        rec.putIf("login_phone1", login_phone1, StringUtils.isNotBlank(login_phone1));
        rec.putIf("gender", gender, StringUtils.isNotBlank(gender));
        rec.putIf("nick_name", nickName, StringUtils.isNotBlank(nickName));
        if (StringUtils.isNotBlank(imei) || StringUtils.isNotBlank(imsi)) {
            Record miscellaneous = Record.of("imei", imei, "imsi", imsi);
            rec.put("miscellaneous", miscellaneous.toJsonNode());
        }
        String userId = createAccount(rec);

        sendNotification(Constants.NTF_CREATE_ACCOUNT,
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId),
                createArrayNodeFromStrings(userId, device, rec.getString("display_name"), rec.getString("gender")),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId),
                createArrayNodeFromStrings(userId, device, rec.getString("display_name"), rec.getString("gender")),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId),
                createArrayNodeFromStrings(userId, login_email1, login_phone1)
        );

        //post a stream
//        Record m = new Record();
//        m.put("appData", "");
        String m = "";
        String tempNowAttachments = "[]";
        //attachments want from client
        int appid = Constants.APP_TYPE_BPC;

//        boolean isCN = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("CN") ? true : false;

//        String mGender = isCN ? "他" : "him";
//        String message = "";
//        if (gender.equals("f")) {
//            mGender = isCN ? "她" : "her";
//        }
//        if (isCN) {
//            message = displayName + "开通了" + mGender + "的播思账号，大家赶快关注" + mGender + "吧～";
//        } else {
//            message = displayName + " have activated borqs account, let us follow " + mGender + " sooner.";
//        }
//        if (isCN) {
//        message = "我已经开通了播思账号，大家赶快关注我吧～";
//        } else {
//            message = "I have activeted borqs account, please follow me ~.";
//        }

        String message = Constants.getBundleString(device, "platform.create.account.message");

        autoPost(userId, Constants.TEXT_POST, message, tempNowAttachments, toStr(appid), "", "", m, "", false, "", device, location, true, true, true, "", "", false);

        //add friend
        setFriends(userId, qiupuUid, String.valueOf(Constants.DEFAULT_CIRCLE), Constants.FRIEND_REASON_AUTOCREATE, true, device, location);
        try {
            autoCreateSuggestusers(userId);
            Record values = Record.of("socialcontact.autoaddfriend", "100");
            setPreferences(userId, values);
        } catch (Exception ex) {
            ex.printStackTrace();
            L.trace(ex.getMessage());
        }

        return userId;
    }

    public String getPerhapsName(String url) throws IOException {
//        String url = "http://api.borqs.com/dm/contacts/namecount/byborqsid/10015.json?limit=2";
        URL ur = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) ur.openConnection();
        conn.setConnectTimeout(10 * 1000);

        String s = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(ur.openStream(), "utf-8"));
        StringBuffer sb = new StringBuffer();
        while ((s = in.readLine()) != null) {
            sb.append(s);
        }
        in.close();
        conn.disconnect();
        return sb.toString().trim();
    }

    public String formatUrl(String user_id) throws IOException {
        Record user = getUser(user_id, user_id, "user_id,login_email1,login_email2,login_email3,login_phone1,login_phone2,login_phone3", false);
        String finallyString = "";
        int flag = 0;
        if (!user.getString("login_phone1").equals("")) {
            finallyString = user.getString("login_phone1");
        } else {
            if (!user.getString("login_phone2").equals("")) {
                finallyString = user.getString("login_phone2");
            } else {
                if (!user.getString("login_phone3").equals("")) {
                    finallyString = user.getString("login_phone3");
                }
            }
        }
        if (!finallyString.equals(""))
            flag = 1;

        if (flag == 0) {
            if (!user.getString("login_email1").equals("")) {
                finallyString = user.getString("login_email1");
            } else {
                if (!user.getString("login_email2").equals("")) {
                    finallyString = user.getString("login_email2");
                } else {
                    if (!user.getString("login_email3").equals("")) {
                        finallyString = user.getString("login_email3");
                    }
                }
            }
            if (!finallyString.equals(""))
                flag = 2;
        }

        if (flag == 0) {
            finallyString = user_id;
            flag = 3;
        }


        String url_header = "http://api.borqs.com/dm/contacts/namecount/";
        String url_middle = "";
        if (flag == 1)
            url_middle = "bymobile/" + finallyString;
        if (flag == 2)
            url_middle = "byemail/" + finallyString;
        if (flag == 3)
            url_middle = "byborqsid/" + finallyString;
        String url_footer = ".json?limit=2";
        return url_header + url_middle + url_footer;
    }

    public RecordSet findBorqsIdFromContactInfo(RecordSet in_contact) throws AvroRemoteException {
        if (in_contact.size() > 0) {
            for (Record rec : in_contact) {
                String user_id_by_email = "";
                String user_id_by_phone = "";
                String user_id_by_name = "";
                String real_user_id = "";
                if (rec.has("email")) {
                    try {
                        user_id_by_email = getUserIdsByNames(rec.getString("email")).getFirstRecord().getString("user_id");
                    } catch (AvroRemoteException e) {
                    }
                }
                if (rec.has("phone")) {
                    try {
                        user_id_by_phone = getUserIdsByNames(rec.getString("phone")).getFirstRecord().getString("user_id");
                    } catch (AvroRemoteException e) {
                    }
                }
                if (rec.has("name")) {
                    try {
                        RecordSet tmp = getUserIdsByNames(rec.getString("name"));
                        if (tmp.size() == 1) {
                            user_id_by_name = tmp.getFirstRecord().getString("user_id");
                        }
                    } catch (AvroRemoteException e) {
                    }
                }
                //以上已经确定是不是返回了，如果返回，只能返回唯一的一个ID
                //1,
                if (!user_id_by_email.equals("") && !user_id_by_phone.equals("") && !user_id_by_name.equals("")) {
                    if (user_id_by_email.equals(user_id_by_phone) && user_id_by_email.equals(user_id_by_name))
                        real_user_id = user_id_by_email;
                }
                //2,
                if (!user_id_by_email.equals("") && !user_id_by_phone.equals("") && user_id_by_name.equals("")) {
                    if (user_id_by_email.equals(user_id_by_phone))
                        real_user_id = user_id_by_email;
                }
                //3,
                if (!user_id_by_email.equals("") && user_id_by_phone.equals("") && !user_id_by_name.equals("")) {
                    if (user_id_by_email.equals(user_id_by_name))
                        real_user_id = user_id_by_email;
                }
                //4,
                if (user_id_by_email.equals("") && !user_id_by_phone.equals("") && !user_id_by_name.equals("")) {
                    if (user_id_by_phone.equals(user_id_by_name))
                        real_user_id = user_id_by_phone;
                }
                //5,
                if (!user_id_by_email.equals("") && user_id_by_phone.equals("") && user_id_by_name.equals("")) {
                    real_user_id = user_id_by_email;
                }
                //6,
                if (user_id_by_email.equals("") && !user_id_by_phone.equals("") && user_id_by_name.equals("")) {
                    real_user_id = user_id_by_phone;
                }
                //7,
                if (user_id_by_email.equals("") && user_id_by_phone.equals("") && !user_id_by_name.equals("")) {
                    real_user_id = user_id_by_name;
                }

                if (!real_user_id.equals("")) {
                    Record u = getUser(real_user_id, real_user_id, USER_LIGHT_COLUMNS_QIUPU);
                    rec.put("user_id", real_user_id);
                    rec.put("display_name", u.getString("display_name"));
                    rec.put("image_url", u.getString("image_url"));
                    rec.put("perhaps_name", u.getString("perhaps_name"));
                    rec.put("address", u.getString("address"));
                }
            }
        }
        return in_contact;
    }

    public void sendNotification(String userId, String device, String login_email1, String login_phone1, String nick_name, String gender) {
        sendNotification(Constants.NTF_CREATE_ACCOUNT,
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId),
                createArrayNodeFromStrings(userId, device, nick_name, gender),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId),
                createArrayNodeFromStrings(userId, device, nick_name, gender),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId, login_email1, login_phone1)
        );
    }

    public boolean destroyAccount(String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        Transceiver transfs = getTransceiver(Friendship.class);
        try {
            Account account = getProxy(Account.class, trans);
            Friendship fs = getProxy(Friendship.class, transfs);

            //find all followers
//            RecordSet recs = getFollowers(userId, userId, "", "user_id", 0, 10000);
//            if (recs.size() > 0) {
//                for (Record r : recs) {
//                    fs.updateMyCircleMemberCount(r.getString("user_id"), "");
//                }
//            }

            boolean b = account.destroyAccount(userId);
            hooks.fireUserDestroyed(Record.of("user_id", Long.parseLong(userId)));

            return b;
        } finally {
            closeTransceiver(trans);
            closeTransceiver(transfs);
        }
    }

    public String findUserIdByUserName(String username) throws AvroRemoteException {
        if (username.equals("")) {
            throw new AuthException("Invalid username");
        }
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return account.findUserIdByUserName(username).toString();
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet searchUser(String viewerId, String username,int page,int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        Transceiver tranf = getTransceiver(Friendship.class);
        try {
            Account account = getProxy(Account.class, trans);
            Friendship fs = getProxy(Friendship.class, tranf);
            RecordSet r = RecordSet.fromByteBuffer(account.searchUserByUserName(username,page,count));
            RecordSet u1 = getUsers(viewerId, r.joinColumnValues("user_id", ","), USER_STANDARD_COLUMNS);
            RecordSet f = RecordSet.fromByteBuffer(fs.getVirtualFriendIdByName(viewerId, username));
            if (f.size() > 0) {
                RecordSet u2 = getUsers(viewerId, f.joinColumnValues("virtual_friendid", ","), USER_STANDARD_COLUMNS);
                for (Record r0 : u2) {
                    u1.add(r0);
                }
            }
            return u1;
        } finally {
            closeTransceiver(trans);
            closeTransceiver(tranf);
        }
    }

    public boolean sendEmail(String title, String to, String username, String content, String type, String lang) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SendMail.class);
        try {
            SendMail sendMail = getProxy(SendMail.class, trans);
            sendMail.sendEmail(title, to, username, content, type, lang);
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    /**
     * sendEmail with innovTemplate
     * @param title
     * @param to
     * @param map
     * @param lang
     * @return
     * @throws AvroRemoteException
     */
    public boolean sendInnovEmail(String title, String to,  Map<String,Object> map, String lang) throws AvroRemoteException {
        String html = pageTemplate.merge("innov.ftl", map);
        return this.sendEmailHTML(title, to, "", html, Constants.EMAIL_ESSENTIAL, lang);
    }

    /**
     * use innovation.borqs.com send email
     * @param title
     * @param to
     * @param username
     * @param content
     * @param type
     * @param lang
     * @return
     * @throws AvroRemoteException
     */
    public boolean sendEmailHTML(String title, String to, String username, String content, String type, String lang) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SendMail.class);
        try {
            SendMail sendMail = getProxy(SendMail.class, trans);
            sendMail.sendEmailHTML(title, to, username, content, type, lang);
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean sendCustomEmail(String title, String to, String username, String templateFile, Map<String, Object> map, String type, String lang) throws AvroRemoteException {
        String html = pageTemplate.merge(templateFile, map);
        Transceiver trans = getTransceiver(SendMail.class);
        try {
            SendMail sendMail = getProxy(SendMail.class, trans);
            sendMail.sendCustomEmail(title, to, username, html, type, lang);
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean sendEmailEleaningHTML(String title, String to, String username, String content, String type, String lang) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SendMail.class);
        try {
            SendMail sendMail = getProxy(SendMail.class, trans);
            sendMail.sendEmailElearningHTML(title, to, username, content, type, lang);
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }
    public void sendSms(String phone, String text) {
        if (smsHost == null)
            throw new ServerException(ErrorCode.SEND_SMS_ERROR, "Send sms error");

        try {
            HttpClient client = new DefaultHttpClient();
            //HttpPost httpPost = new HttpPost("http://" + smsHost + "/smsgw/sendsms.php");
            HttpPost httpPost = new HttpPost(smsHost);
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            //params.add(new BasicNameValuePair("sendto", phone));
            //params.add(new BasicNameValuePair("content", text));

            params.add(new BasicNameValuePair("appname", "qiupu"));
            params.add(new BasicNameValuePair("data", String.format("{\"to\":\"%s\",\"subject\":\"%s\"}", phone, StringEscapeUtils.escapeJavaScript(text))));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            client.execute(httpPost);
        } catch (IOException e) {
            throw new ServerException(ErrorCode.SEND_SMS_ERROR, "Send sms error");
        }
    }

    public boolean resetPassword(String loginName, String key, String lang) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            String[] emails = new String[3];

            if (StringUtils.isBlank(key)) {
                RecordSet userIdRs = RecordSet.fromByteBuffer(account
                        .getUserIds(loginName));
                String userId = userIdRs.getFirstRecord().getString("user_id");
                if (StringUtils.isEmpty(userId))
                    throw Errors.createResponseError(ErrorCode.USER_NOT_EXISTS,
                            "User '%s' is not exists", loginName);
                RecordSet recs = RecordSet.fromByteBuffer(account.getUsers(
                        userId, "login_email1,login_email2,login_email3"));
                Record rec = recs.getFirstRecord();
                emails[0] = rec.getString("login_email1");
                emails[1] = rec.getString("login_email2");
                emails[2] = rec.getString("login_email3");

                if (StringUtils.isBlank(emails[0]) && StringUtils.isBlank(emails[1]) && StringUtils.isBlank(emails[2])) {
                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "Do not have any binded email.");
                }

                if (loginName.indexOf(".") != -1) {
                    loginName = loginName.replace(".", "borqsdotborqs");
                }
                if (loginName.indexOf("@") != -1) {
                    loginName = loginName.replace("@", "borqsatborqs");
                }

                key = Encoders.desEncryptBase64(loginName + "/" + new Date().getTime());
                String url = "http://" + SERVER_HOST + "/account/reset_password?key=" + key;
//                String content = "		Hello, if you confirm that you forget Borqs's password, "
//                        + "please click the link as below in 72 hours: <br>"
//                        + "<a href=\"" + url + "\">" + url + "</a>";
                String template = Constants.getBundleStringByLang(lang, "platform.reset.password.content");
                String content = SQLTemplate.merge(template, new Object[][]{
                        {"url", url}
                });
                String title = Constants.getBundleStringByLang(lang, "platform.reset.password.title");
                for (String email : emails) {
                    if (StringUtils.isNotBlank(email))
                        sendEmail(title, email,
                                email, content, Constants.EMAIL_ESSENTIAL, lang);
                }
            } else {
                key = StringUtils.replace(key, " ", "+");
                long validPeriod = 3L * 24 * 60 * 60 * 1000; //email valid period: 3days
                String info = Encoders.desDecryptFromBase64(key);
                int index = info.lastIndexOf("/");
                loginName = info.substring(0, index);
                long valid = Long.parseLong(info.substring(index + 1));
                if (valid < (new Date().getTime() - validPeriod)) {
                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "The link is expired");
                }

                if (loginName.indexOf("borqsdotborqs") != -1) {
                    loginName = loginName.replaceAll("borqsdotborqs", ".");
                }
                if (loginName.indexOf("borqsatborqs") != -1) {
                    loginName = loginName.replaceAll("borqsatborqs", "@");
                }
                String newPwd = toStr(account.resetPassword(loginName));

//                String content = "		Hello, your Borqs account: "
//                        + loginName
//                        + " password have been changed as: "
//                        + newPwd
//                        + ".<br>"
//                        + "              Please use it to login Borqs application and then modify your password soon.";

                String template = Constants.getBundleStringByLang(lang, "platform.reset.password.message");
                String content = SQLTemplate.merge(template, new Object[][]{
                        {"loginName", loginName},
                        {"newPwd", newPwd}
                });

                RecordSet userIdRs = RecordSet.fromByteBuffer(account
                        .getUserIds(loginName));
                String userId = userIdRs.getFirstRecord().getString("user_id");
                if (StringUtils.isEmpty(userId))
                    throw Errors.createResponseError(ErrorCode.USER_NOT_EXISTS,
                            "User '%s' is not exists", loginName);
                RecordSet recs = RecordSet.fromByteBuffer(account.getUsers(
                        userId, "login_email1,login_email2,login_email3"));
                Record rec = recs.getFirstRecord();
                emails[0] = rec.getString("login_email1");
                emails[1] = rec.getString("login_email2");
                emails[2] = rec.getString("login_email3");

                if (StringUtils.isBlank(emails[0]) && StringUtils.isBlank(emails[1]) && StringUtils.isBlank(emails[2])) {
                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "Do not have any binded email.");
                }

                for (String email : emails) {
                    if (StringUtils.isNotBlank(email))
                        sendEmail(Constants.getBundleStringByLang(lang, "platform.reset.password.msgtitle"),
                                email, email, content, Constants.EMAIL_ESSENTIAL, lang);
                }
            }

            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean resetPasswordForPhone(String loginName) throws AvroRemoteException {
        loginName = StringUtils.trimToEmpty(loginName);
        if (!StringUtils.isNumeric(loginName))
            return false;

        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            String userId = RecordSet.fromByteBuffer(account.getUserIds(loginName)).getFirstRecord().getString("user_id");
            if (StringUtils.isEmpty(userId) || "0".equals(userId)) {
                throw Errors.createResponseError(ErrorCode.USER_NOT_EXISTS,
                        "User '%s' is not exists", loginName);
            }
            Record userRec = RecordSet.fromByteBuffer(account.getUsers(userId, "user_id,password")).getFirstRecord();
            String md5OldPwd = userRec.getString("password");
            String newPwd = toStr(account.resetPassword(loginName));
            String md5NewPwd = Encoders.md5Hex(newPwd);

            syncBorqsBbsPwd(loginName, md5OldPwd, md5NewPwd);
            sendNewPasswordToPhone(loginName, newPwd);
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    private boolean syncBorqsBbsPwd(String phone, String md5OldPwd, String md5NewPwd) {
        boolean b = getConfig().getBoolean("platform.syncBbsPwd", false);
        if (b) {
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("login_phone", phone));
            params.add(new BasicNameValuePair("oldpwd", md5OldPwd));
            params.add(new BasicNameValuePair("newpwd", md5NewPwd));
            HttpGet g = new HttpGet("http://bbs.borqs.com/account/change_password?" + URLEncodedUtils.format(params, "UTF-8"));
            HttpClient client = new DefaultHttpClient();
            try {
                HttpResponse resp = client.execute(g);
                String s = IOUtils.toString(resp.getEntity().getContent());
                return true;
            } catch (IOException e) {
                L.warn("syncBorqsBbsPwd error", e);
                return false;
            }
        } else {
            return false;
        }
    }

    void sendNewPasswordToPhone(String phone, String newPwd) {
        if (smsHost == null)
            throw new ServerException(ErrorCode.SEND_SMS_ERROR, "Send sms error");

        String text = "您的密码已经重置成功，新密码是 " + newPwd + "。";
        try {
            HttpClient client = new DefaultHttpClient();
            //HttpPost httpPost = new HttpPost("http://" + smsHost + "/smsgw/sendsms.php");
            HttpPost httpPost = new HttpPost(smsHost);
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
//            params.add(new BasicNameValuePair("sendto", phone));
//            params.add(new BasicNameValuePair("content", text));
            params.add(new BasicNameValuePair("appname", "qiupu"));
            params.add(new BasicNameValuePair("data", String.format("{\"to\":\"%s\",\"subject\":\"%s\"}", phone, StringEscapeUtils.escapeJavaScript(text))));

            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            client.execute(httpPost);
        } catch (IOException e) {
            throw new ServerException(ErrorCode.SEND_SMS_ERROR, "Send sms error");
        }
    }

    public boolean updateAccount(String userId, Record user) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return account.updateAccount(userId, user.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateAccount(String userId, Record user, String lang) throws AvroRemoteException {
        return updateAccount(userId, user, lang, true);
    }

    public boolean updateAccount22(String userId, Record user, String lang) throws AvroRemoteException {
        return updateAccount22(userId, user, lang, true);
    }

    public boolean updateAccount(String userId, Record user, String lang, boolean sendNotif) throws AvroRemoteException {
//        L.debug("update account begin at:"+DateUtils.nowMillis());
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            if (CollectionUtils2.containsOne(user.keySet(), "login_email1", "login_email2", "login_email3", "login_phone1", "login_phone2", "login_phone3")) {
                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update this column.");
            }

            if (CollectionUtils2.containsOne(user.keySet(), "contact_info")) {
                Record rec = getUser(userId, userId, "user_id,contact_info,login_email1,login_email2,login_email3,login_phone1,login_phone2,login_phone3");
                if (!rec.isEmpty()) {
                    Record con_r = Record.fromJson(rec.getString("contact_info"));
                    Record con_in_u = Record.fromJson(user.getString("contact_info"));
                    if (!con_r.getString("email_address").equals(con_in_u.getString("email_address"))) {
                        if (con_in_u.getString("email_address").equals("")) {
                            if (rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this only column.");
                            } else {
                                user.put("login_email1", con_in_u.getString("email_address"));
                                con_in_u.removeColumns("email_address");
                            }
                        } else {
                            if (!con_r.getString("email_address").equals("")) {
                                if (rec.getString("login_email1").equals(con_r.getString("email_address")) || rec.getString("login_email2").equals(con_r.getString("email_address")) || rec.getString("login_email3").equals(con_r.getString("email_address"))
                                        || rec.getString("login_phone1").equals(con_r.getString("email_address")) || rec.getString("login_phone2").equals(con_r.getString("email_address")) || rec.getString("login_phone3").equals(con_r.getString("email_address"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_address,because has bind.");
                                }
                            }
                            if (rec.getString("login_email1").equals(con_in_u.getString("email_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_address"))
                                    || rec.getString("login_phone1").equals(con_in_u.getString("email_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_address"))) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_address,because has bind.");
                            }
                        }
                    }

                    if (!con_r.getString("email_2_address").equals(con_in_u.getString("email_2_address"))) {
                        if (con_in_u.getString("email_2_address").equals("")) {
                            if (rec.getString("login_email1").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                            } else {
                                user.put("login_email2", con_in_u.getString("email_2_address"));
                                con_in_u.removeColumns("email_2_address");
                            }
                        } else {
                            if (!con_r.getString("email_2_address").equals("")) {
                                if (rec.getString("login_email1").equals(con_r.getString("email_2_address")) || rec.getString("login_email2").equals(con_r.getString("email_2_address")) || rec.getString("login_email3").equals(con_r.getString("email_2_address"))
                                        || rec.getString("login_phone1").equals(con_r.getString("email_2_address")) || rec.getString("login_phone2").equals(con_r.getString("email_2_address")) || rec.getString("login_phone3").equals(con_r.getString("email_2_address"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_2_address,because has bind.");
                                }
                            }
                            if (rec.getString("login_email1").equals(con_in_u.getString("email_2_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_2_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_2_address"))
                                    || rec.getString("login_phone1").equals(con_in_u.getString("email_2_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_2_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_2_address"))) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_2_address,because has bind.");
                            }
                        }
                    }

                    if (!con_r.getString("email_3_address").equals(con_in_u.getString("email_3_address"))) {
                        if (con_in_u.getString("email_3_address").equals("")) {
                            if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                            } else {
                                user.put("login_email3", con_in_u.getString("email_3_address"));
                                con_in_u.removeColumns("email_3_address");
                            }
                        } else {
                            if (!con_r.getString("email_3_address").equals("")) {
                                if (rec.getString("login_email1").equals(con_r.getString("email_3_address")) || rec.getString("login_email2").equals(con_r.getString("email_3_address")) || rec.getString("login_email3").equals(con_r.getString("email_3_address"))
                                        || rec.getString("login_phone1").equals(con_r.getString("email_3_address")) || rec.getString("login_phone2").equals(con_r.getString("email_3_address")) || rec.getString("login_phone3").equals(con_r.getString("email_3_address"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_3_address,because has bind.");
                                }
                            }
                            if (rec.getString("login_email1").equals(con_in_u.getString("email_3_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_3_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_3_address"))
                                    || rec.getString("login_phone1").equals(con_in_u.getString("email_3_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_3_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_3_address"))) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_3_address,because has bind.");
                            }
                        }
                    }

                    if (!con_r.getString("mobile_telephone_number").equals(con_in_u.getString("mobile_telephone_number"))) {
                        if (con_in_u.getString("mobile_telephone_number").equals("")) {
                            if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                            } else {
                                user.put("login_phone1", con_in_u.getString("mobile_telephone_number"));
                                con_in_u.removeColumns("mobile_telephone_number");
                            }
                        } else {
                            if (!con_r.getString("mobile_telephone_number").equals("")) {
                                if (rec.getString("login_email1").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_telephone_number"))
                                        || rec.getString("login_phone1").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_telephone_number"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_telephone_number,because has bind.");
                                }
                            }
                            if (rec.getString("login_email1").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_telephone_number"))
                                    || rec.getString("login_phone1").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_telephone_number"))) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_telephone_number,because has bind.");
                            }
                        }
                    }

                    if (!con_r.getString("mobile_2_telephone_number").equals(con_in_u.getString("mobile_2_telephone_number"))) {
                        if (con_in_u.getString("mobile_2_telephone_number").equals("")) {
                            if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone3").equals("")) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                            } else {
                                user.put("login_phone2", con_in_u.getString("mobile_2_telephone_number"));
                                con_in_u.removeColumns("mobile_2_telephone_number");
                            }
                        } else {
                            if (!con_r.getString("mobile_2_telephone_number").equals("")) {
                                if (rec.getString("login_email1").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_2_telephone_number"))
                                        || rec.getString("login_phone1").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_2_telephone_number"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_2_telephone_number,because has bind.");
                                }
                            }
                            if (rec.getString("login_email1").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_2_telephone_number"))
                                    || rec.getString("login_phone1").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_2_telephone_number"))) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_2_telephone_number,because has bind.");
                            }
                        }
                    }

                    if (!con_r.getString("mobile_3_telephone_number").equals(con_in_u.getString("mobile_3_telephone_number"))) {
                        if (con_in_u.getString("mobile_3_telephone_number").equals("")) {
                            if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("")) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                            } else {
                                user.put("login_phone3", con_in_u.getString("mobile_3_telephone_number"));
                                con_in_u.removeColumns("mobile_3_telephone_number");
                            }
                        } else {
                            if (!con_r.getString("mobile_3_telephone_number").equals("")) {
                                if (rec.getString("login_email1").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_3_telephone_number"))
                                        || rec.getString("login_phone1").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_3_telephone_number"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_3_telephone_number,because has bind.");
                                }
                            }
                            if (rec.getString("login_email1").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_3_telephone_number"))
                                    || rec.getString("login_phone1").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_3_telephone_number"))) {
                                throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_3_telephone_number,because has bind.");
                            }
                        }
                    }

                    if (con_in_u.getString("mobile_telephone_number").equals(""))
                        con_in_u.removeColumns("mobile_telephone_number");
                    if (con_in_u.getString("mobile_2_telephone_number").equals(""))
                        con_in_u.removeColumns("mobile_2_telephone_number");
                    if (con_in_u.getString("mobile_3_telephone_number").equals(""))
                        con_in_u.removeColumns("mobile_3_telephone_number");
                    if (con_in_u.getString("mobile_telephone_number").equals(""))
                        con_in_u.removeColumns("mobile_telephone_number");
                    if (con_in_u.getString("mobile_2_telephone_number").equals(""))
                        con_in_u.removeColumns("mobile_2_telephone_number");
                    if (con_in_u.getString("mobile_3_telephone_number").equals(""))
                        con_in_u.removeColumns("mobile_3_telephone_number");

                    user.put("contact_info", con_in_u.toString(false, false));
                }
            }
//            String displayName = user.getString("display_name_temp",
//            		getUser(userId, userId, "display_name").getString("display_name"));
            String displayName = getUser(userId, userId, "display_name").getString("display_name");
            user.removeColumns("phone", "email"/*, "display_name_temp"*/);
            L.debug("=================updateAccount test,user="+user.toString());
            boolean b = account.updateAccount(userId, user.toByteBuffer());
//            L.debug("update account end at:"+DateUtils.nowMillis());
            if (!user.has("perhaps_name")) {
                Record user0 = user.copy();
                addImageUrlPrefix(getConfig().getString("platform.profileImagePattern", ""), user0);
                hooks.fireUserProfileChanged(user0.set("user_id", Long.parseLong(userId)));
//            L.debug("update account hooks end at:"+DateUtils.nowMillis());
//            L.debug("send notification begin at:"+DateUtils.nowMillis());
                if (b && sendNotif) {
                    sendNotification(Constants.NTF_PROFILE_UPDATE,
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(user.toString(false, false), displayName, userId, lang),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(lang),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId)
                    );
                }
            }
//            L.debug("notification end at:"+DateUtils.nowMillis() + ",so return.");
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }
    public boolean updateAccount22(String userId, Record user, String lang, boolean sendNotif) throws AvroRemoteException {
    //        L.debug("update account begin at:"+DateUtils.nowMillis());
            Transceiver trans = getTransceiver(Account.class);
            try {
                Account account = getProxy(Account.class, trans);
                boolean b = account.updateAccount(userId, user.toByteBuffer());
                return b;
            } finally {
                closeTransceiver(trans);
            }
        }
    public boolean updateAccount11(String userId, Record user, String lang, boolean sendNotif) throws AvroRemoteException {
    //        L.debug("update account begin at:"+DateUtils.nowMillis());
            Transceiver trans = getTransceiver(Account.class);
            try {
                Account account = getProxy(Account.class, trans);
                if (CollectionUtils2.containsOne(user.keySet(), "login_email1", "login_email2", "login_email3", "login_phone1", "login_phone2", "login_phone3")) {
                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update this column.");
                }

                if (CollectionUtils2.containsOne(user.keySet(), "contact_info")) {
                    Record rec = getUser(userId, userId, "user_id,contact_info,login_email1,login_email2,login_email3,login_phone1,login_phone2,login_phone3");
                    if (!rec.isEmpty()) {
                        Record con_r = Record.fromJson(rec.getString("contact_info"));
                        Record con_in_u = Record.fromJson(user.getString("contact_info"));
                        if (!con_r.getString("email_address").equals(con_in_u.getString("email_address"))) {
                            if (con_in_u.getString("email_address").equals("")) {
                                if (rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this only column.");
                                } else {
                                    user.put("login_email1", con_in_u.getString("email_address"));
                                    con_in_u.removeColumns("email_address");
                                }
                            } else {
                                if (!con_r.getString("email_address").equals("")) {
                                    if (rec.getString("login_email1").equals(con_r.getString("email_address")) || rec.getString("login_email2").equals(con_r.getString("email_address")) || rec.getString("login_email3").equals(con_r.getString("email_address"))
                                            || rec.getString("login_phone1").equals(con_r.getString("email_address")) || rec.getString("login_phone2").equals(con_r.getString("email_address")) || rec.getString("login_phone3").equals(con_r.getString("email_address"))) {
                                        throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_address,because has bind.");
                                    }
                                }
                                if (rec.getString("login_email1").equals(con_in_u.getString("email_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_address"))
                                        || rec.getString("login_phone1").equals(con_in_u.getString("email_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_address"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_address,because has bind.");
                                }
                            }
                        }

                        if (!con_r.getString("email_2_address").equals(con_in_u.getString("email_2_address"))) {
                            if (con_in_u.getString("email_2_address").equals("")) {
                                if (rec.getString("login_email1").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                                } else {
                                    user.put("login_email2", con_in_u.getString("email_2_address"));
                                    con_in_u.removeColumns("email_2_address");
                                }
                            } else {
                                if (!con_r.getString("email_2_address").equals("")) {
                                    if (rec.getString("login_email1").equals(con_r.getString("email_2_address")) || rec.getString("login_email2").equals(con_r.getString("email_2_address")) || rec.getString("login_email3").equals(con_r.getString("email_2_address"))
                                            || rec.getString("login_phone1").equals(con_r.getString("email_2_address")) || rec.getString("login_phone2").equals(con_r.getString("email_2_address")) || rec.getString("login_phone3").equals(con_r.getString("email_2_address"))) {
                                        throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_2_address,because has bind.");
                                    }
                                }
                                if (rec.getString("login_email1").equals(con_in_u.getString("email_2_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_2_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_2_address"))
                                        || rec.getString("login_phone1").equals(con_in_u.getString("email_2_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_2_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_2_address"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_2_address,because has bind.");
                                }
                            }
                        }

                        if (!con_r.getString("email_3_address").equals(con_in_u.getString("email_3_address"))) {
                            if (con_in_u.getString("email_3_address").equals("")) {
                                if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                                } else {
                                    user.put("login_email3", con_in_u.getString("email_3_address"));
                                    con_in_u.removeColumns("email_3_address");
                                }
                            } else {
                                if (!con_r.getString("email_3_address").equals("")) {
                                    if (rec.getString("login_email1").equals(con_r.getString("email_3_address")) || rec.getString("login_email2").equals(con_r.getString("email_3_address")) || rec.getString("login_email3").equals(con_r.getString("email_3_address"))
                                            || rec.getString("login_phone1").equals(con_r.getString("email_3_address")) || rec.getString("login_phone2").equals(con_r.getString("email_3_address")) || rec.getString("login_phone3").equals(con_r.getString("email_3_address"))) {
                                        throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_3_address,because has bind.");
                                    }
                                }
                                if (rec.getString("login_email1").equals(con_in_u.getString("email_3_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_3_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_3_address"))
                                        || rec.getString("login_phone1").equals(con_in_u.getString("email_3_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_3_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_3_address"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:email_3_address,because has bind.");
                                }
                            }
                        }

                        if (!con_r.getString("mobile_telephone_number").equals(con_in_u.getString("mobile_telephone_number"))) {
                            if (con_in_u.getString("mobile_telephone_number").equals("")) {
                                if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                                } else {
                                    user.put("login_phone1", con_in_u.getString("mobile_telephone_number"));
                                    con_in_u.removeColumns("mobile_telephone_number");
                                }
                            } else {
                                if (!con_r.getString("mobile_telephone_number").equals("")) {
                                    if (rec.getString("login_email1").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_telephone_number"))
                                            || rec.getString("login_phone1").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_telephone_number"))) {
                                        throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_telephone_number,because has bind.");
                                    }
                                }
                                if (rec.getString("login_email1").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_telephone_number"))
                                        || rec.getString("login_phone1").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_telephone_number"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_telephone_number,because has bind.");
                                }
                            }
                        }

                        if (!con_r.getString("mobile_2_telephone_number").equals(con_in_u.getString("mobile_2_telephone_number"))) {
                            if (con_in_u.getString("mobile_2_telephone_number").equals("")) {
                                if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone3").equals("")) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                                } else {
                                    user.put("login_phone2", con_in_u.getString("mobile_2_telephone_number"));
                                    con_in_u.removeColumns("mobile_2_telephone_number");
                                }
                            } else {
                                if (!con_r.getString("mobile_2_telephone_number").equals("")) {
                                    if (rec.getString("login_email1").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_2_telephone_number"))
                                            || rec.getString("login_phone1").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_2_telephone_number"))) {
                                        throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_2_telephone_number,because has bind.");
                                    }
                                }
                                if (rec.getString("login_email1").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_2_telephone_number"))
                                        || rec.getString("login_phone1").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_2_telephone_number"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_2_telephone_number,because has bind.");
                                }
                            }
                        }

                        if (!con_r.getString("mobile_3_telephone_number").equals(con_in_u.getString("mobile_3_telephone_number"))) {
                            if (con_in_u.getString("mobile_3_telephone_number").equals("")) {
                                if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("")) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't delete this column.");
                                } else {
                                    user.put("login_phone3", con_in_u.getString("mobile_3_telephone_number"));
                                    con_in_u.removeColumns("mobile_3_telephone_number");
                                }
                            } else {
                                if (!con_r.getString("mobile_3_telephone_number").equals("")) {
                                    if (rec.getString("login_email1").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_3_telephone_number"))
                                            || rec.getString("login_phone1").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_3_telephone_number"))) {
                                        throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_3_telephone_number,because has bind.");
                                    }
                                }
                                if (rec.getString("login_email1").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_3_telephone_number"))
                                        || rec.getString("login_phone1").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_3_telephone_number"))) {
                                    throw Errors.createResponseError(ErrorCode.GENERAL_ERROR, "can't update column:mobile_3_telephone_number,because has bind.");
                                }
                            }
                        }

                        if (con_in_u.getString("mobile_telephone_number").equals(""))
                            con_in_u.removeColumns("mobile_telephone_number");
                        if (con_in_u.getString("mobile_2_telephone_number").equals(""))
                            con_in_u.removeColumns("mobile_2_telephone_number");
                        if (con_in_u.getString("mobile_3_telephone_number").equals(""))
                            con_in_u.removeColumns("mobile_3_telephone_number");
                        if (con_in_u.getString("mobile_telephone_number").equals(""))
                            con_in_u.removeColumns("mobile_telephone_number");
                        if (con_in_u.getString("mobile_2_telephone_number").equals(""))
                            con_in_u.removeColumns("mobile_2_telephone_number");
                        if (con_in_u.getString("mobile_3_telephone_number").equals(""))
                            con_in_u.removeColumns("mobile_3_telephone_number");

                        user.put("contact_info", con_in_u.toString(false, false));
                    }
                }
                boolean b = account.updateAccount(userId, user.toByteBuffer());
    //            L.debug("update account end at:"+DateUtils.nowMillis());

                return b;
            } finally {
                closeTransceiver(trans);
            }
        }

    public void sendNodificationInternal(String userId, Record user, String lang, String displayName) {
        sendNotification(Constants.NTF_PROFILE_UPDATE,
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId),
                createArrayNodeFromStrings(user.toString(false, false), displayName, userId, lang),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId),
                createArrayNodeFromStrings(lang),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(),
                createArrayNodeFromStrings(userId)
        );
    }

    private static void addImageUrlPrefix(String profileImagePattern, Record rec) {
        if (rec.has("image_url")) {
            if (!rec.getString("image_url", "").startsWith("http:"))
                rec.put("image_url", String.format(profileImagePattern, rec.getString("image_url")));
        }

        if (rec.has("small_image_url")) {
            if (!rec.getString("small_image_url", "").startsWith("http:"))
                rec.put("small_image_url", String.format(profileImagePattern, rec.getString("small_image_url")));
        }

        if (rec.has("large_image_url")) {
            if (!rec.getString("large_image_url", "").startsWith("http:"))
                rec.put("large_image_url", String.format(profileImagePattern, rec.getString("large_image_url")));
        }
    }

    private RecordsExtenders createUserExtenders(String viewerId) {
        RecordsExtenders res = new RecordsExtenders();
        res.add(PlatformExtender.setViewerId(new BuiltinUserExtender(this), viewerId));
        res.addExtendersInConfig(getConfig(), "platform.userExtenders");
        PlatformExtender.configPlatformExtenders(res, this, viewerId);
        return res;
    }

    public boolean saveStatistics(Record rec) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Statistics.class);
        try {
            Statistics statistics = getProxy(Statistics.class, trans);
//            L.debug("In Platform saveStatistics");
            return statistics.save(rec.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean setPreferences(String viewerId, Record values) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Setting.class);
        try {
            Setting setting = getProxy(Setting.class, trans);
            return setting.set(viewerId, values.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getPreferences(String viewerId, String keys) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(Setting.class);
        try {
            Setting setting = getProxy(Setting.class, trans);
            return Record.fromByteBuffer(setting.gets(viewerId, keys));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getPreferencesByStarts(String viewerId, String startsWith) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(Setting.class);
        try {
            Setting setting = getProxy(Setting.class, trans);
            return Record.fromByteBuffer(setting.getsByStartsWith(viewerId, startsWith));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getPreferencesByUsers(String key, String users) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(Setting.class);
        try {
            Setting setting = getProxy(Setting.class, trans);
            return Record.fromByteBuffer(setting.getByUsers(key, users));
        } finally {
            closeTransceiver(trans);
        }
    }

    public String generalShortUrl(String long_url) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account a = getProxy(Account.class, trans);
            String short_url = "";
            if (!long_url.toUpperCase().startsWith("HTTP://"))
                long_url = "http://" + long_url;
            if (long_url.substring(long_url.length() - 1).equals("//") || long_url.substring(long_url.length() - 1).equals("\\")) {
                long_url = long_url.substring(0, long_url.length() - 1);
            }

            if (long_url.substring(long_url.lastIndexOf("\\") + 1, long_url.length()).contains("?")) {
                long_url += "&generate_time=" + DateUtils.nowMillis();
            } else {
                long_url += "?generate_time=" + DateUtils.nowMillis();
            }
            URL ur = null;
            try {
                ur = new URL(long_url);
            } catch (MalformedURLException e) {
                L.error("format url error=" + e);
            }
            String host = ur.getHost();    //api.borqs.com
            String lastUrlStr = StringUtils.replace(long_url, "http://" + host + "/", "");
            String formatUrl = ShortText(lastUrlStr)[0];
            short_url = "http://" + host + "/" + "z" + "/" + formatUrl;
            a.saveShortUrl(long_url, short_url);
            return short_url;
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getLongUrl(String short_url) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account a = getProxy(Account.class, trans);
            //http://api.borqs.com/z/yqArQr
            String out_url = toStr(a.findLongUrl(short_url));
            if (out_url.length() < 10) {
                out_url = "http://" + SERVER_HOST + "/link/expired";
            }
            return out_url;
        } finally {
            closeTransceiver(trans);
        }
    }

    ////
    public boolean setNUserPreferences(String viewerId, Record values) throws AvroRemoteException {
        Transceiver trans = getTransceiver(NUserSetting.class);
        try {
            NUserSetting setting = getProxy(NUserSetting.class, trans);
            return setting.set(viewerId, values.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getNUserPreferences(String viewerId, String keys) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(NUserSetting.class);
        try {
            NUserSetting setting = getProxy(NUserSetting.class, trans);
            return Record.fromByteBuffer(setting.gets(viewerId, keys));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getNUserPreferencesByStarts(String viewerId, String startsWith) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(NUserSetting.class);
        try {
            NUserSetting setting = getProxy(NUserSetting.class, trans);
            return Record.fromByteBuffer(setting.getsByStartsWith(viewerId, startsWith));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getNUserPreferencesByUsers(String key, String users) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(NUserSetting.class);
        try {
            NUserSetting setting = getProxy(NUserSetting.class, trans);
            return Record.fromByteBuffer(setting.getByUsers(key, users));
        } finally {
            closeTransceiver(trans);
        }
    }

    private List<String> parseAuths(String auths, boolean isCircle) {
        List<String> l = StringUtils2.splitList(auths, ",", true);
        List<String> c = new ArrayList<String>();
        List<String> u = new ArrayList<String>();

        for (String element : l) {
            if (element.startsWith("#")) //element is circle
            {
                element = StringUtils.substringAfter(element, "#");
                c.add(element);
            } else {
                u.add(element);
            }
        }

        return isCircle ? c : u;
    }

    public boolean setPrivacy(String viewerId, RecordSet privacyItemList) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return account.setPrivacy(viewerId, privacyItemList.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }
    //=======================

    public RecordSet findAllUserIds(boolean all) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return RecordSet.fromByteBuffer(account.findAllUserIds(all));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getViewerPrivacyConfig(String viewerId, String resources) throws ResponseError, AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            RecordSet privacyItemList = RecordSet.fromByteBuffer(account.getAuths(viewerId, resources));

            Record privacyConfig = new Record();
            for (Record privacyItem : privacyItemList) {
                String resource = privacyItem.getString("resource");
                String auths = privacyItem.getString("auths");
                privacyConfig.put(resource, auths);
            }

            return privacyConfig;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getPrivacy(String viewerId, String userId, String resources) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);

            // get circles in userId from viewerId
            List<String> fcl = new ArrayList<String>();
            if (StringUtils.isNotBlank(viewerId) && !viewerId.equals("0"))
                fcl = getRelation(viewerId, userId).getStringColumnValues("circle_id");

            // get authorize list from userId
            RecordSet privacyItemList = RecordSet.fromByteBuffer(account.getAuths(userId, resources));
            for (Record privacyItem : privacyItemList) {
                String resource = privacyItem.getString("resource");
                String auths = privacyItem.getString("auths");
                List<String> cl = parseAuths(auths, true);
                List<String> ul = parseAuths(auths, false);

                // in block
                if (fcl.contains(Constants.BLOCKED_CIRCLE)) {
                    privacyItem.putMissing("result", false);
                    continue;
                }

                // if have not set authorize, then return default authorize
                if (StringUtils.isBlank(auths)) {
                    for (String circleId : fcl) {
                        if (account.getDefaultPrivacy(resource, circleId)) {
                            privacyItem.putMissing("result", true);
                            break;
                        }
                    }

                    privacyItem.putMissing("result", false);
                    continue;
                }

                //viewerId in userId's  authorize list
                if (ul.contains(viewerId)) {
                    privacyItem.putMissing("result", true);
                    continue;
                }

                //public circle case
                if (cl.contains(String.valueOf(Constants.PUBLIC_CIRCLE))) {
                    privacyItem.putMissing("result", true);
                    continue;
                }

                //friend circle case
                if (cl.contains(String.valueOf(Constants.FRIENDS_CIRCLE))) {
                    if (!fcl.isEmpty()) {
                        privacyItem.putMissing("result", true);
                        continue;
                    }
                }

                //traversal viewerId in userId' circle
                for (String circleId : fcl) {
                    if (cl.contains(circleId)) {
                        //the circle have authorize
                        privacyItem.putMissing("result", true);
                        break;
                    }
                }

                privacyItem.putMissing("result", false);
            }

            return privacyItemList;
        } finally {
            closeTransceiver(trans);
        }
    }

    private void doAbsolutePrivateCols(Record rec) {
        if (rec.has("login_email1")) {
            rec.put("login_email1", "");
        }

        if (rec.has("login_email2")) {
            rec.put("login_email2", "");
        }

        if (rec.has("login_email3")) {
            rec.put("login_email3", "");
        }

        if (rec.has("login_phone1")) {
            rec.put("login_phone1", "");
        }

        if (rec.has("login_phone2")) {
            rec.put("login_phone2", "");
        }

        if (rec.has("login_phone3")) {
            rec.put("login_phone3", "");
        }

        if (rec.has("password")) {
            rec.put("password", "");
        }
    }

    public RecordSet dealWithInCirclesByGroups(long begin, long end, String userId, String friendId, RecordSet reuse) throws AvroRemoteException {
        L.trace("In dealWithInCirclesByGroups");
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);

            RecordSet userGroups = RecordSet.fromByteBuffer(group.findGroupsByMember(begin, end, Long.parseLong(userId), GROUP_LIGHT_COLS));
            RecordSet friendGroups = RecordSet.fromByteBuffer(group.findGroupsByMember(begin, end, Long.parseLong(friendId), GROUP_LIGHT_COLS));
            RecordSet recs = new RecordSet(CollectionUtils.intersection(userGroups, friendGroups));

            for (Record rec : recs) {
                long groupId = rec.getInt(Constants.GRP_COL_ID, 0);
                String groupName = rec.getString(Constants.GRP_COL_NAME, "");
                reuse.add(Record.of("circle_id", String.valueOf(groupId), "circle_name", groupName));
            }

            return reuse;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getContentByVirtualIds(String virtualIds) throws AvroRemoteException {
        Transceiver transfs = getTransceiver(Friendship.class);
        Friendship f = getProxy(Friendship.class, transfs);
        try {
            RecordSet recs = RecordSet.fromByteBuffer(f.getContactFriendByFid(virtualIds));
            return recs;
        } finally {
            closeTransceiver(transfs);
        }
    }

    public RecordSet getUsers(String viewerId, String userIds_all, String cols, boolean privacyEnabled) throws AvroRemoteException {
        return getUsers(viewerId, userIds_all, cols, privacyEnabled, true);
    }
    
    public RecordSet getUsers(String viewerId, String userIds_all, String cols, boolean privacyEnabled, boolean dealTopPosts) throws AvroRemoteException {
        List<String> userIdListAll = StringUtils2.splitList(toStr(userIds_all), ",", true);
        List<String> userIdList_sys = new ArrayList<String>();
        List<String> userIdList_contact = new ArrayList<String>();
        for (String u : userIdListAll) {
            if (u.length() > 10) {
                userIdList_contact.add(u);
            } else {
                userIdList_sys.add(u);
            }
        }
        String userIds = StringUtils.join(userIdList_sys, ",");

        if (!StringUtils.contains(cols, "user_id")) {
            cols += ",user_id";
        }
        if (!StringUtils.contains(cols, "display_name")) {
            cols += ",display_name";
        }
        if (dealTopPosts && !StringUtils.contains(cols, "top_name"))
            cols += ",top_name";

        cols = parseUserColumns(cols);
        final String userIds0 = parseUserIds(viewerId, userIds);
        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        List<String> l2 = new ArrayList<String>();
        for (String a : l) {
            l2.add(a);
        }
        if (l.contains("favorites_count")) {
            l.remove("favorites_count");
        }
        if (l.contains("friends_count")) {
            l.remove("friends_count");
        }
        if (l.contains("followers_count")) {
            l.remove("followers_count");
        }

        Transceiver trans = getTransceiver(Account.class);
        Transceiver trans_ = getTransceiver(Request.class);
        Transceiver transfs = getTransceiver(Friendship.class);
        Transceiver transstream = getTransceiver(Stream.class);
        Friendship f = getProxy(Friendship.class, transfs);
        try {
            RecordSet recs = new RecordSet();
            final Account account = getProxy(Account.class, trans);
            RecordsExtenders userExtenders = createUserExtenders(viewerId);
            recs = userExtenders.extendRecords(
                    StringUtils2.splitSet(StringUtils.join(l, ","), ",", true),
                    new RecordsProducer() {

                        @Override
                        public RecordSet product(Set<String> produceCols) throws Exception {
                            return RecordSet.fromByteBuffer(account.getUsers(userIds0, StringUtils.join(produceCols, ",")));
                        }
                    });
            //L.debug("-------------------------111111-----------------------"+recs.toString()+"-----------------------------------");
            if (l2.contains("favorites_count")) {
                if (recs.size() > 0) {
                    Transceiver tranq = getTransceiver(QiupuInterface.class);
                    QiupuInterface qp = getProxy(QiupuInterface.class, tranq);
                    RecordSet recs_ucount = RecordSet.fromByteBuffer(qp.getUsersAppCount(userIds, String.valueOf(1 << 3)));
                    Map app_map = new HashMap();
                    for (Record ur : recs_ucount) {
                        app_map.put(ur.getString("user_id"), ur.getString("count"));
                    }
                    for (Record rec : recs) {
                        rec.put("favorites_count", app_map.get(rec.getString("user_id")));
                    }
                }
            }

            if ((l2.contains("friends_count") || l2.contains("followers_count"))) {
                if (recs.size() > 0) {

                    List<String> userl = StringUtils2.splitList(toStr(userIds), ",", true);
                    RecordSet recs_fs = RecordSet.fromByteBuffer(f.getFriendOrFollowers(userIds, "friend"));
                    RecordSet recs_fri = RecordSet.fromByteBuffer(f.getFriendOrFollowers(userIds, "user"));

                    Map fs_map = new HashMap();
                    Map fri_map = new HashMap();
                    for (String ul : userl) {
                        int i = 0;
                        int j = 0;
                        for (Record fsr : recs_fs) {
                            if (fsr.getString("friend").equals(ul)) {
                                i++;
                            }
                        }
                        fs_map.put(ul, String.valueOf(i));
                        for (Record frir : recs_fri) {
                            if (frir.getString("user").equals(ul)) {
                                j++;
                            }
                        }
                        fri_map.put(ul, String.valueOf(j));
                    }

                    for (Record rec : recs) {
                        rec.put("friends_count", fri_map.get(rec.getString("user_id")));
                        rec.put("followers_count", fs_map.get(rec.getString("user_id")));
                    }
                }
            }

            //add shared_count
            if (l2.contains("friends_count")) {
                for (Record rec : recs) {
                    Record shared_count = new Record();
                    int shared_text = getSharedCount(viewerId, rec.getString("user_id"), Constants.TEXT_POST);
                    shared_count.put("shared_text", shared_text);
                    int shared_photo = getSharedCount(viewerId, rec.getString("user_id"), Constants.PHOTO_POST);
                    shared_count.put("shared_photo", shared_photo);
                    int shared_book = getSharedCount(viewerId, rec.getString("user_id"), Constants.BOOK_POST);
                    shared_count.put("shared_book", shared_book);
                    int shared_apk = getSharedCount(viewerId, rec.getString("user_id"), Constants.APK_POST);
                    shared_count.put("shared_apk", shared_apk);
                    int shared_link = getSharedCount(viewerId, rec.getString("user_id"), Constants.LINK_POST);
                    shared_count.put("shared_link", shared_link);
                    int shared_static_file = getSharedCount(viewerId, rec.getString("user_id"), Constants.FILE_POST);
                    shared_count.put("shared_static_file", shared_static_file);
                    int shared_audio = getSharedCount(viewerId, rec.getString("user_id"), Constants.AUDIO_POST);
                    shared_count.put("shared_audio", shared_audio);
                    int shared_video = getSharedCount(viewerId, rec.getString("user_id"), Constants.VIDEO_POST);
                    shared_count.put("shared_video", shared_video);
                    /*
                    int  shared_video = getSharedCount(viewerId,rec.getString("user_id"),Constants.VIDEO_POST);
                    shared_count.put("shared_video",shared_video);
                    int  shared_audio = getSharedCount(viewerId,rec.getString("user_id"),Constants.AUDIO_POST);
                    shared_count.put("shared_audio",shared_audio);
                    int  shared_music = getSharedCount(viewerId,rec.getString("user_id"),Constants.MUSIC_POST);
                    shared_count.put("shared_music",shared_link);
                    */
                    shared_count.put("shared_poll", getRelatedPollCount(viewerId, rec.getString("user_id")));
                    rec.put("shared_count", shared_count);
                }
            }

            if (privacyEnabled) {
                for (Record rec : recs) {
                    rec.putMissing("profile_privacy", true);
                    String uid = rec.getString("user_id");
                    if (!viewerId.equals(uid)) {
                        doAbsolutePrivateCols(rec);
                    }
                    boolean if_in = true;
                    if (!viewerId.equals("") && !viewerId.equals("0")) {
                        if_in = f.getIfHeInMyCircles(uid, viewerId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE));
                        rec.put("profile_privacy", !if_in);
                    }

                    if (!if_in && !viewerId.equals(uid)) {
                        rec.put("contact_info", new Record());
                        rec.put("address", new RecordSet());
                        rec.put("work_history", new RecordSet());
                        rec.put("education_history", new RecordSet());
                    }
                }
            }
//            //privacy
//            if (privacyEnabled) {
//                if (recs.size() > 0) {
//                    //1,get all users'auths
//                    RecordSet recs_auth = RecordSet.fromByteBuffer(account.getUsersAuths(userIds));
//
//                    for (Record r_auth : recs_auth) {
//                        r_auth.put("auths", parseUserIds(r_auth.getString("user"), r_auth.getString("auths")));
//                    }
//
//                    Map a_map = new HashMap();
//                    for (Record r_auth : recs_auth) {
//                        a_map.put(r_auth.getString("user") + "_" + r_auth.getString("resource"), r_auth.getString("auths"));
//                    }
//
//                    RecordSet recs_their = getAllRelation(viewerId, recs.joinColumnValues("user_id", ","), "", "their");
//                    String user_string = recs_their.joinColumnValues("user", ",");
//                    Set<String> user_set = StringUtils2.splitSet(toStr(user_string), ",", true);
//
//                    Map u_map = new HashMap();
//                    for (String u : user_set) {
//                        String circleIds = "";
//                        for (Record r_their : recs_their) {
//                            if (r_their.getString("user").equals(u)) {
//                                circleIds += r_their.getString("circle") + ",";
//                            }
//                            u_map.put(u, StringUtils.substringBeforeLast(circleIds, ","));
//                        }
//                    }
//
//                    for (Record rec : recs) {
//                        //0,default not open
//                        rec.putMissing("profile_privacy", true);
//                        //1,if this guy have not set phonebook.address ,then find   phonebook ,eventhou set default
//                        //2，if has，get userids，Whether i was in userids,in is true,not in is false
//                        String uid = rec.getString("user_id");
//                        if (!viewerId.equals(uid)) {
//                            doAbsolutePrivateCols(rec);
//                        }
//                        if (a_map.get(uid + "_" + "phonebook.address") == null) {    //have not set    phonebook.address
//                            if (a_map.get(uid + "_" + "phonebook") == null) {          //have not set    phonebook
//                                //which circle i was in his circles,and his circles have one open
//                                if (u_map.get(uid) != null) {    //not find   circle
//                                    String cIds = u_map.get(uid).toString();
//                                    List<String> allow_circle_l = StringUtils2.splitList(toStr(cIds), ",", true);
//                                    for (String c : allow_circle_l) {
//                                        if (account.getDefaultPrivacy("phonebook", c)) {
//                                            rec.put("profile_privacy", false);
//                                            break;
//                                        }
//                                    }
//                                }
//                            } else {    //exist set by  phonebook
//                                String allowUserIds = a_map.get(uid + "_" + "phonebook").toString();
//                                List<String> allow_user_l = StringUtils2.splitList(toStr(allowUserIds), ",", true);
//                                if (allow_user_l.contains(viewerId)) {
//                                    rec.put("profile_privacy", false);
//                                }
//                            }
//                        } else {      //exist set by  phonebook.address
//                            String allowUserIds = a_map.get(uid + "_" + "phonebook.address").toString();
//                            List<String> allow_user_l = StringUtils2.splitList(toStr(allowUserIds), ",", true);
//                            if (allow_user_l.contains(viewerId)) {      //contain me
//                                rec.put("profile_privacy", false);
//                            }
//                        }
//
//                        if (rec.getBoolean("profile_privacy", true)) {
//                            if (!viewerId.equals(uid)) {
//                                String col = Constants.res_col.get("phonebook");
//                                if (rec.has(col)) {
//                                    if (col.equals("contact_info")) {
//                                        rec.put(col, JsonNodeFactory.instance.objectNode());
//                                    } else if (col.equals("address") || col.equals("work_history") || col.equals("education_history")) {
//                                        rec.put(col, JsonNodeFactory.instance.arrayNode());
//                                    } else {
//                                        rec.put(col, "");
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }

            //pendding requests
            Request req = getProxy(Request.class, trans_);

            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    RecordSet recs_pend = RecordSet.fromByteBuffer(req.getPeddingRequestsAll(viewerId, userIds));
                    Map pend_map = new HashMap();
                    for (Record p : recs_pend) {
                        pend_map.put(p.getString("user"), p.getString("penddingRequest"));
                    }
                    for (Record rec : recs) {
                        List<String> ltypes = new ArrayList<String>();
                        String uid = rec.getString("user_id");

                        if (pend_map.get(uid) != null) {
                            String md = StringUtils.substringBeforeLast(pend_map.get(uid).toString(), ",");
                            if (md.length() > 0) {
                                ltypes = StringUtils2.splitList(md, ",", true);
                            }
                            rec.putMissing("pedding_requests", JsonUtils.parse(JsonUtils.toJson(ltypes, false)));
                        } else {
                            rec.putMissing("pedding_requests", new RecordSet());
                        }
                    }
                } else {
                    for (Record rec : recs) {
                        rec.putMissing("pedding_requests", new RecordSet());
                    }
                }
            }

            //看看此人在我这通讯簿里面存的啥名字
            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    for (Record rec : recs) {
                        String uid = rec.getString("user_id");
                        RecordSet so_recss = getSocialcontactUsername(viewerId, uid);
                        List<String> sf = new ArrayList<String>();
                        for (Record so_rec : so_recss) {
                            if (!sf.contains(so_rec.getString("username")))
                                sf.add(so_rec.getString("username"));
                        }
                        rec.put("social_contacts_username", StringUtils.join(sf, ","));
                    }
                } else {
                    for (Record rec : recs) {
                        rec.put("social_contacts_username", "");
                    }
                }
            }

            //如果不是好友，看看有没有人跟我推荐过他
            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    for (Record rec : recs) {
                        String uid = rec.getString("user_id");
                        rec.put("who_suggested", new RecordSet());
//                        RecordSet in_circles = RecordSet.fromJson(rec.getString("in_circles"));
                        boolean beforeFriend = isFriend(viewerId, uid);
                        if (!beforeFriend) {
                            //是好友，不用查推荐
                            List<Long> bs = getWhoSuggest(viewerId, uid);
                            if (bs.size() > 0) {
                                List<String> bs1 = new ArrayList<String>();
                                for (Long bs0 : bs) {
                                    bs1.add(String.valueOf(bs0));
                                }
//                                RecordSet users_suggested = getUsers(viewerId, StringUtils.join(bs1, ","), USER_LIGHT_COLUMNS_LIGHT);
                                RecordSet us = RecordSet.fromByteBuffer(account.getUsers(StringUtils.join(bs1, ","),USER_LIGHT_COLUMNS_LIGHT));
                                rec.put("who_suggested", us);
                            }
                        }
                    }
                } else {
                    for (Record rec : recs) {
                        rec.put("who_suggested", new RecordSet());
                    }
                }
            }

            if (userIdList_contact.size() > 0) {
                String userIds_contact = StringUtils.join(userIdList_contact, ",");
                RecordSet recs_contact_fri = RecordSet.fromByteBuffer(f.getContactFriendByFid(userIds_contact));
                if (recs_contact_fri.size() > 0) {
                    RecordSet recs_mine = getAllRelation(viewerId, userIds_contact, Integer.toString(Constants.FRIENDS_CIRCLE), "mine");
                    for (Record r : recs_contact_fri) {
                        r.renameColumn("virtual_friendid", "user_id");
                        r.renameColumn("name", "display_name");
                        r.renameColumn("content", "content");

                        RecordSet temp0 = new RecordSet();
                        for (Record ru : recs_mine) {
                            if (ru.getString("friend").equals(r.getString("user_id"))) {
                                temp0.add(Record.of("circle_id", ru.getString("circle"), "circle_name", ru.getString("name")));
                            }
                        }
                        r.put("in_circles", temp0);
                        r.put("friends_count", 0);
                        RecordSet t = RecordSet.fromByteBuffer(f.getFollowers(r.getString("user_id"), String.valueOf(Constants.FRIENDS_CIRCLE), 0, 500));
                        r.put("followers_count", t.size());
                    }
                    recs.addAll(recs_contact_fri);
                }
            }
            //#######################################
            if (l2.contains("friends_count")) {
                int getSize = 5;
                Stream stream = getProxy(Stream.class, transstream);
                for (Record r : recs) {
                    //1,
                    String uid = r.getString("user_id");
                    RecordSet bo = RecordSet.fromByteBuffer(f.getBothFriendsIds(viewerId, uid, 0, 100));
                    String bothF = bo.joinColumnValues("friend", ",");
                    RecordSet friend_ = RecordSet.fromByteBuffer(account.getUsers(bothF, "user_id,image_url"));
                    RecordSet t = new RecordSet();
                    for (Record r0 : friend_) {
                        if (r0.getString("image_url").length() > 0) {
                            Record t0 = new Record();
                            t0.put("image_url", r0.getString("image_url"));
                            t0.put("user_id", r0.getString("user_id"));
                            t.add(t0);
                        }
                        if (t.size() >= getSize)
                            break;
                    }
                    if (t.size() < getSize) {
                        for (Record r0 : friend_) {
                            if (r0.getString("image_url").length() == 0) {
                                Record t0 = new Record();
                                t0.put("image_url", r0.getString("image_url"));
                                t0.put("user_id", r0.getString("user_id"));
                                t.add(t0);
                            }
                            if (t.size() >= getSize)
                                break;
                        }
                    }
                    r.put("profile_friends", t);
                    //2,
                    RecordSet recs_fs = RecordSet.fromByteBuffer(f.getFollowers(uid, Integer.toString(FRIENDS_CIRCLE), 0, 100));
                    String fsIdString = recs_fs.joinColumnValues("follower", ",");
                    RecordSet follower_ = RecordSet.fromByteBuffer(account.getUsers(fsIdString, "user_id,image_url"));
                    RecordSet t_f = new RecordSet();
                    for (Record r0 : follower_) {
                        if (r0.getString("image_url").length() > 0) {
                            Record t0 = new Record();
                            t0.put("image_url", r0.getString("image_url"));
                            t0.put("user_id", r0.getString("user_id"));
                            t_f.add(t0);
                        }
                        if (t_f.size() >= getSize)
                            break;
                    }
                    if (t_f.size() < getSize) {
                        for (Record r0 : follower_) {
                            if (r0.getString("image_url").length() == 0) {
                                Record t0 = new Record();
                                t0.put("image_url", r0.getString("image_url"));
                                t0.put("user_id", r0.getString("user_id"));
                                t_f.add(t0);
                            }
                            if (t_f.size() >= getSize)
                                break;
                        }
                    }
                    r.put("profile_followers", t_f);
                    //3,
                    RecordSet share_photo = RecordSet.fromByteBuffer(stream.getSharedByType(uid, Constants.PHOTO_POST, "post_id,attachments", 0, 5));
                    RecordSet t_p = new RecordSet();
                    for (Record rP : share_photo) {
                        Record attach = RecordSet.fromJson(rP.getString("attachments")).getFirstRecord();
                        Record t0 = new Record();
                        t0.put("photo_img_middle", attach.getString("photo_img_middle"));
                        t0.put("photo_img_original", attach.getString("photo_img_original"));
                        t0.put("post_id", rP.getString("post_id"));
                        t_p.add(t0);
                    }
                    r.put("profile_shared_photos", t_p);
                }
            }
            //L.debug("--------------------------222222----------------------"+recs.toString()+"-----------------------------------");
            //##############################################
            return dealTopPosts ? dealAccountTopPosts(recs) : recs;
        } finally {
            closeTransceiver(trans);
            closeTransceiver(trans_);
        }
    }




    public RecordSet getUsers(String viewerId, String userIds, String cols) throws AvroRemoteException {
        List<String> users = StringUtils2.splitList(userIds, ",", true);
        List<String> groups = getGroupIdsFromMentions(users);
        users.removeAll(groups);

        userIds = StringUtils2.joinIgnoreBlank(",", users);
        RecordSet userRecs = getUsers(viewerId, userIds, cols, true);

        if (CollectionUtils.isNotEmpty(groups)) {
            String groupIds = StringUtils2.joinIgnoreBlank(",", groups);
            userRecs.addAll(getCompatibleGroups(viewerId, groupIds));
        }

        return userRecs;
    }

    public RecordSet getCompatibleGroups(String viewerId, String groupIds) throws AvroRemoteException {
        RecordSet groups = getGroups(0, 0, viewerId, groupIds, GROUP_LIGHT_COLS, false);

        groups.renameColumn(GRP_COL_ID, "user_id");
        groups.renameColumn(GRP_COL_NAME, "display_name");
        groups.renameColumn(COMM_COL_DESCRIPTION, "status");
        groups.renameColumn(COMM_COL_WEBSITE, "domain_name");

        for (Record group : groups) {
            group.putMissing("last_visited_time", 0);
            group.putMissing("login_email1", "");
            group.putMissing("login_email2", "");
            group.putMissing("login_email3", "");
            group.putMissing("login_phone1", "");
            group.putMissing("login_phone2", "");
            group.putMissing("login_phone3", "");
            group.putMissing("status_updated_time", 0);
            group.putMissing("perhaps_name", JsonNodeFactory.instance.arrayNode());
            group.putMissing("basic_updated_time", 0);
            group.putMissing("first_name", "");
            group.putMissing("middle_name", "");
            group.putMissing("last_name", "");
            group.putMissing("gender", "m");
            group.putMissing("birthday", "");
            group.putMissing("job_title", "");
            group.putMissing("office_address", "");
            group.putMissing("job_description", "");
            group.putMissing("work_history", JsonNodeFactory.instance.arrayNode());
            group.putMissing("education_history", JsonNodeFactory.instance.arrayNode());
            group.putMissing("miscellaneous", JsonNodeFactory.instance.objectNode());
            group.putMissing("remark", "");
            group.putMissing("bidi", false);
            group.putMissing("in_circles", JsonNodeFactory.instance.arrayNode());
            group.putMissing("his_friend", false);
            group.putMissing("favorites_count", "0");
            group.putMissing("friends_count", "0");
            group.putMissing("followers_count", "0");
            group.putMissing("profile_privacy", false);
            group.putMissing("pedding_requests", JsonNodeFactory.instance.arrayNode());
            group.putMissing("profile_friends", JsonNodeFactory.instance.arrayNode());
            group.putMissing("profile_followers", JsonNodeFactory.instance.arrayNode());
            group.putMissing("profile_shared_photos", JsonNodeFactory.instance.arrayNode());
            group.putMissing("social_contacts_username", "");
            group.putMissing("who_suggested", JsonNodeFactory.instance.arrayNode());
        }


        return groups;
    }

    public Record getUser(String viewerId, String userId, String cols) throws AvroRemoteException {
        RecordSet rec = getUsers(viewerId, firstId(userId), cols);
        return rec.getFirstRecord();
    }

    public Record getUser(String viewerId, String userId, String cols, boolean privacyEnabled) throws AvroRemoteException {
        RecordSet rec = getUsers(viewerId, firstId(userId), cols, privacyEnabled);
        return rec.getFirstRecord();
    }

    public RecordSet getUserIds(String names) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return RecordSet.fromByteBuffer(account.getUserIds(names));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getUserIdsByNames(String names) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return RecordSet.fromByteBuffer(account.getUserIdsByNames(names));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet hasUsers(String userIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return RecordSet.fromByteBuffer(account.hasUsers(userIds));
        } finally {
            closeTransceiver(trans);
        }
    }

    public String parseAllUsers(String userIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            String returnValue = "";
            if (userIds.length() > 0) {
                List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);
                RecordSet recs = new RecordSet();
                for (String userId : userIds0) {
                    if (StringUtils.isNumeric(userId)) {
                        recs.add(Record.of("user_id", userId));
                    }
                }
                if (recs.size() > 0) {
                    returnValue = recs.joinColumnValues("user_id", ",");
                }
            }
            return returnValue;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean hasUser(String userId) throws AvroRemoteException {
        return hasOneUsers(firstId(userId));
    }

    public boolean hasOneUsers(String userIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return account.hasOneUsers(userIds);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean hasAllUsers(String userIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, trans);
            return account.hasAllUsers(userIds);
        } finally {
            closeTransceiver(trans);
        }
    }

    public void checkUserIds(String... userIds) throws AvroRemoteException {
        String[] userIds0 = StringUtils2.splitArray(StringUtils.join(userIds, ","), ",", true);
        if (userIds0.length == 1) {
            String userId = userIds[0];
            if (!hasUser(userId))
                throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);
        } else if (userIds.length > 1) {
            if (!hasAllUsers(StringUtils.join(userIds0, ","))){
                throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User is not exists");
            }
        }
    }

    private void fireChangeProfileHooksForBind(String userId, String phone, String email) {
        ObjectNode on = JsonNodeFactory.instance.objectNode();
        if (StringUtils.isNotEmpty(phone))
            on.put("mobile_telephone_number", phone);
        if (StringUtils.isNotEmpty(email))
            on.put("email_address", email);
        if (on.size() > 0) {
            Record userRec = Record.of("user_id", userId, "contact_info", on);
            hooks.fireUserProfileChanged(userRec);
        }
    }

    public boolean bindUserSendVerify(String userId, String phone, String email, String key, String ticket, String lang) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Account.class);
        try {
//            if (!phone.equals("")) {
//                throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "phone can't bind now!");
//            }
            Account a = getProxy(Account.class, trans);
            boolean b = checkBindNameNotExists(phone, email);
            if (!b)
                throw Errors.createResponseError(ErrorCode.BIND_HASBIND_ERROR, "phone or email has bind by others!");

            Record r = getUsers(userId, userId, "user_id, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, display_name,contact_info").getFirstRecord();

            if (!phone.equals("")) {
                if (r.getString("login_email1").equals(phone) || r.getString("login_email2").equals(phone) || r.getString("login_email3").equals(phone)) {
                    throw Errors.createResponseError(ErrorCode.BIND_HASBIND_ERROR, "email has binded by others");
                }
                if (r.getString("login_phone1").equals(phone) || r.getString("login_phone2").equals(phone) || r.getString("login_phone3").equals(phone)) {
                    throw Errors.createResponseError(ErrorCode.BIND_HASBIND_ERROR, "phone has binded by others");
                }
            }

            if (!email.equals("")) {
                if (r.getString("login_email1").equals(email) || r.getString("login_email2").equals(email) || r.getString("login_email3").equals(email)) {
                    throw Errors.createResponseError(ErrorCode.BIND_HASBIND_ERROR, "email has binded by others");
                }
                if (r.getString("login_phone1").equals(email) || r.getString("login_phone2").equals(email) || r.getString("login_phone3").equals(email)) {
                    throw Errors.createResponseError(ErrorCode.BIND_HASBIND_ERROR, "phone has binded by others");
                }
            }
            String host = "http://" + SERVER_HOST + "/";
            if (key.equals("")) {
                if (phone.equals("") && !email.equals("")) //verify email
                {
//                  update contact_info first
                    String con_column = "";

                    // 注释下面代码保证如果邮件未验证不加入contact_info
                    /*
                    if (r.getString("contact_info").length() > 10) {
                        Record c_r = Record.fromJson(r.getString("contact_info"));

                        //update contact_info for this email
                        if (!c_r.getString("email_address").equals("") && !c_r.getString("email_2_address").equals("") && !c_r.getString("email_3_address").equals("")) {
                            if (c_r.getString("email_address").equals(email) || c_r.getString("email_2_address").equals(email) || c_r.getString("email_3_address").equals(email)) {
                            } else {
                                throw Errors.createResponseError(ErrorCode.BIND_BINDMANY_ERROR, "has 3 email already!");
                            }
                        }
                        if (c_r.getString("email_address").equals("") && !c_r.getString("email_2_address").equals(email) && !c_r.getString("email_3_address").equals(email)) {
                            con_column = "email_address";
                        }
                        if (!c_r.getString("email_address").equals("") && c_r.getString("email_2_address").equals("") && !c_r.getString("email_address").equals(email) && !c_r.getString("email_3_address").equals(email)) {
                            con_column = "email_2_address";
                        }
                        if (!c_r.getString("email_address").equals("") && !c_r.getString("email_2_address").equals("") && c_r.getString("email_3_address").equals("") && !c_r.getString("email_address").equals(email) && !c_r.getString("email_2_address").equals(email)) {
                            con_column = "email_3_address";
                        }
                        if (!con_column.equals("")) {
                            c_r.put(con_column, email);
                            Record user = new Record();
                            user.put("contact_info", c_r.toString(false, false));
                            a.bindUser(userId, user.toByteBuffer());
                            fireChangeProfileHooksForBind(userId, phone, email);
                        }
                    }
                    */


                    //send email
                    String title = r.getString("display_name") + "，欢迎您绑定邮箱到播思通行证";
                    String to = r.getString("display_name");
                    String content = "尊敬的 " + to + " :<br>";
                    content += "　　您的播思通行证ID是：" + r.getString("user_id") + "，您已输入 " + email + " 绑定到您的播思通行证。要完成该流程，只需验证该电子邮件地址是否属于您即可。"
                            + "请点击下方链接，如果无法点击，请复制链接到地址栏转入。<br>";

                    String url = host + "account/bind?ticket=" + ticket + "&";
                    String param = "userId=" + userId + "&phone=" + phone + "&email=" + email + "&key=";

                    String gkey = userId + "/" + phone + "/" + email;
                    FeedbackParams fp = new FeedbackParams().set("param", gkey);
                    String b2 = fp.toBase64(true);
                    url = url + param + b2;

                    String link = "<a href=" + url + " target=_blank>" + url + "</a>";

                    content += link;
                    sendEmail(title, email, to, content, Constants.EMAIL_ESSENTIAL, lang);
                }
                if (!phone.equals("") && email.equals("")) //verify phone
                {
                    //======================================================= begin====================================
                    String upcolumn = "";
                    String con_column = "";
                    if (!r.getString("login_phone1").equals("") && !r.getString("login_phone2").equals("") && !r.getString("login_phone3").equals("")) {
                        throw Errors.createResponseError(ErrorCode.BIND_BINDMANY_ERROR, "has 3 phones binded already");
                    }
                    // 注释下面代码保证如果手机未验证不加入contact_info
                    /*
                    if (r.getString("contact_info").length() > 10) {
                        Record c_r = Record.fromJson(r.getString("contact_info"));
                        if (c_r.getString("mobile_telephone_number").equals(phone)) {
                            upcolumn = "login_phone1";
                            con_column = "mobile_telephone_number";
                        } else if (c_r.getString("mobile_2_telephone_number").equals(phone)) {
                            upcolumn = "login_phone2";
                            con_column = "mobile_2_telephone_number";
                        } else if (c_r.getString("mobile_3_telephone_number").equals(phone)) {
                            upcolumn = "login_phone3";
                            con_column = "mobile_3_telephone_number";
                        } else {
                            if (r.getString("login_phone1").equals("")) {
                                upcolumn = "login_phone1";
                                con_column = "mobile_telephone_number";
                            }
                            if (!r.getString("login_phone1").equals("") && r.getString("login_phone2").equals("")) {
                                upcolumn = "login_phone2";
                                con_column = "mobile_2_telephone_number";
                            }
                            if (!r.getString("login_phone1").equals("") && !r.getString("login_phone2").equals("") && r.getString("login_phone3").equals("")) {
                                upcolumn = "login_phone3";
                                con_column = "mobile_3_telephone_number";
                            }
                            //throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "must in contact_info first");
                        }
                        c_r.put(con_column, phone);
                        Record user = new Record();
//                        user.put(upcolumn, phone);
                        user.put("contact_info", c_r.toString(false, false));
                        a.bindUser(userId, user.toByteBuffer());
                        fireChangeProfileHooksForBind(userId, phone, email);
                    }
                    */
                    //======================================================= end====================================

                    //send message
                    String content = "请点击下面的链接，将您的手机号码绑定到播思账号：";

                    String param = "account/bind?ticket=" + ticket + "&phone=" + phone + "&email=" + email + "&key=";

                    String gkey = userId + "/" + phone + "/" + email;
                    FeedbackParams fp = new FeedbackParams().set("param", gkey);
                    String b2 = fp.toBase64(true);
                    // sendmessage(from,to,content);
                    String url = host + param + b2;
                    L.debug("=========send short sms,phone=" + phone + ",content=" + content + ",old url=" + url);
                    String short_url = generalShortUrl(url);
                    L.debug("=========send short sms,new url=" + short_url);
                    sendSms(phone, content + short_url + "\\");
                }
            } else {
                key = key.replaceAll(" ", "+");
                String fp = FeedbackParams.fromBase64(key).get("param");
                String[] ss = StringUtils2.splitArray(fp, "/", 3, false);//fp.split("/");
//                if (ss.length < 3) {
//                    throw Errors.createResponseError(ErrorCode.BIND_KEY_ERROR, "key error");
//                }

                String keyUserId = ss[0];
                String keyPhone = ss[1];
                String keyEmail = ss[2];

                String upcolumn = "";
                String con_column = "";
                if (keyPhone.equals("") && !email.equals("")) {//bind email
                    if (!r.getString("login_email1").equals("") && !r.getString("login_email2").equals("") && !r.getString("login_email3").equals("")) {
                        throw Errors.createResponseError(ErrorCode.BIND_BINDMANY_ERROR, "has 3 emails binded already");
                    }
                    if (r.getString("contact_info").length() > 10) {
                        Record c_r = Record.fromJson(r.getString("contact_info"));
                        if (c_r.getString("email_address").equals(keyEmail)) {
                            upcolumn = "login_email1";
                            con_column = "email_address";
                        } else if (c_r.getString("email_2_address").equals(keyEmail)) {
                            upcolumn = "login_email2";
                            con_column = "email_2_address";
                        } else if (c_r.getString("email_3_address").equals(keyEmail)) {
                            upcolumn = "login_email3";
                            con_column = "email_3_address";
                        } else {
                            if (r.getString("login_email1").equals("")) {
                                upcolumn = "login_email1";
                                con_column = "email_address";
                            }
                            if (!r.getString("login_email1").equals("") && r.getString("login_email2").equals("")) {
                                upcolumn = "login_email2";
                                con_column = "email_2_address";
                            }
                            if (!r.getString("login_email1").equals("") && !r.getString("login_email2").equals("") && r.getString("login_email3").equals("")) {
                                upcolumn = "login_email3";
                                con_column = "email_3_address";
                            }
                            //throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "must in contact_info first");
                        }
                        c_r.put(con_column, keyEmail);
                        Record user = new Record();
                        user.put(upcolumn, keyEmail);
                        user.put("contact_info", c_r.toString(false, false));
                        a.bindUser(keyUserId, user.toByteBuffer());
                        updateVirtualFriendIdToAct(userId, keyEmail);

                        if (StringUtils.isNotBlank(email)) {
                            onBindEmain(Long.parseLong(userId), email);
                        }
                        fireChangeProfileHooksForBind(userId, phone, email);
                    }
                }
                if (!keyPhone.equals("") && email.equals("")) {//bind phone
                    if (!r.getString("login_phone1").equals("") && !r.getString("login_phone2").equals("") && !r.getString("login_phone3").equals("")) {
                        throw Errors.createResponseError(ErrorCode.BIND_BINDMANY_ERROR, "has 3 phones binded already");
                    }
                    if (r.getString("contact_info").length() > 10) {
                        Record c_r = Record.fromJson(r.getString("contact_info"));
                        if (c_r.getString("mobile_telephone_number").equals(keyPhone)) {
                            upcolumn = "login_phone1";
                            con_column = "mobile_telephone_number";
                        } else if (c_r.getString("mobile_2_telephone_number").equals(keyPhone)) {
                            upcolumn = "login_phone2";
                            con_column = "mobile_2_telephone_number";
                        } else if (c_r.getString("mobile_3_telephone_number").equals(keyPhone)) {
                            upcolumn = "login_phone3";
                            con_column = "mobile_3_telephone_number";
                        } else {
                            if (r.getString("login_phone1").equals("")) {
                                upcolumn = "login_phone1";
                                con_column = "mobile_telephone_number";
                            }
                            if (!r.getString("login_phone1").equals("") && r.getString("login_phone2").equals("")) {
                                upcolumn = "login_phone2";
                                con_column = "mobile_2_telephone_number";
                            }
                            if (!r.getString("login_phone1").equals("") && !r.getString("login_phone2").equals("") && r.getString("login_phone3").equals("")) {
                                upcolumn = "login_phone3";
                                con_column = "mobile_3_telephone_number";
                            }
                            //throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "must in contact_info first");
                        }
                        c_r.put(con_column, keyPhone);
                        Record user = new Record();
                        user.put(upcolumn, keyPhone);
                        user.put("contact_info", c_r.toString(false, false));
                        a.bindUser(keyUserId, user.toByteBuffer());
                        updateVirtualFriendIdToAct(userId, keyPhone);
                        fireChangeProfileHooksForBind(userId, phone, email);
                    }
                }
            }
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }


    //=============================================firendship===begin================================================

    private boolean createBuiltinCircles(String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return fs.createBuiltinCircles(userId);
        } finally {
            closeTransceiver(trans);
        }
    }

    public String createCircle(String userId, String name) throws AvroRemoteException {
        Validate.notNull(userId);
        Validate.notNull(name);
        Validate.isTrue(name.length() < 20);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return toStr(fs.createCircle(userId, name));
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean destroyCircle(String userId, String circleIds) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            List<String> cl = StringUtils2.splitList(toStr(circleIds), ",", true);

            for (String cl0 : cl) {
                RecordSet recs = RecordSet.fromByteBuffer(fs.getCircles(userId, cl0, true));
                String uids =recs.getFirstRecord().getString("members");
                List<String> l = StringUtils2.splitList(uids, ",", true);
                for (String uid : l) {
                    if (uid.length() ==19) {
                        //contact friend,have virtual borqsId
                        Record v_f = RecordSet.fromByteBuffer(fs.getContactFriendByFid(uid)).getFirstRecord();
                        if (!v_f.isEmpty()) {
                            fs.setContactFriend(userId, uid, v_f.getString("name"), v_f.getString("content"), cl0, Constants.FRIEND_REASON_MANUALSELECT, false, false);
                        }
                    } else {
                        fs.setFriends(userId, uid, cl0, Constants.FRIEND_REASON_MANUALSELECT, false);
                    }
                }
            }
            return fs.destroyCircles(userId, circleIds);
        } finally {
            closeTransceiver(trans);
        }
    }


    public boolean updateCircleName(String userId, String circleId, String newName) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return fs.updateCircleName(userId, circleId, newName);
        } finally {
            closeTransceiver(trans);
        }
    }


    public RecordSet getCircles(String userId, String circleIds, boolean withUsers) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(fs.getCircles(userId, circleIds, withUsers));
            if (withUsers) {
                for (Record rec : recs) {
                    String memberIds = rec.getString("members");
                    RecordSet members = getUsers(userId, memberIds, "user_id, display_name, remark, image_url,perhaps_name");
                    rec.put("members", members.toJsonNode());
                }
            }
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateCircleMemberCount(String userId, String circleId, int member_count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return fs.updateCircleMemberCount(userId, circleId, member_count);
        } finally {
            closeTransceiver(trans);
        }
    }

    public List<Long> getWhoSuggest(String to, String beSuggested) throws AvroRemoteException {
        Validate.notNull(to);
        Validate.notNull(beSuggested);
        checkUserIds(to, beSuggested);

        Transceiver trans = getTransceiver(SuggestedUser.class);
        try {
            SuggestedUser su = getProxy(SuggestedUser.class, trans);
            return StringUtils2.splitIntList(toStr(su.getWhoSuggest(to, beSuggested)), ",");
        } finally {
            closeTransceiver(trans);
        }
    }
    public boolean setFriendsTemp(String userId, String friendIds, String circleId, int reason, boolean isadd) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        Friendship fs = getProxy(Friendship.class, trans);
        List<String> l = StringUtils2.splitList(toStr(friendIds), ",", true);
        for (String uid : l) {
             fs.setFriends(userId, uid, circleId, reason, isadd);
        }
        return true;
    }


    public boolean setFriends(String userId, String friendIds, String circleId, int reason, boolean isadd, String ua, String loc) throws AvroRemoteException {
        Validate.notNull(userId);

        List<String> l = StringUtils2.splitList(toStr(friendIds), ",", true);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Friendship.class);
        Transceiver transre = getTransceiver(Request.class);
        Request reqre = getProxy(Request.class, transre);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            List<String> nl = new ArrayList<String>();

            for (String uid : l) {
                if (uid.length() > 10) {
                    //contact friend,have virtual borqsId
                    Record v_f = RecordSet.fromByteBuffer(fs.getContactFriendByFid(uid)).getFirstRecord();
                    if (!v_f.isEmpty())
                        fs.setContactFriend(userId, uid, v_f.getString("name"), v_f.getString("content"), circleId, reason, isadd, false);
                } else {
                    //have borqs id
                    // which circle i was in others
                    RecordSet ys_recs = getRelation(userId, uid, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE));
                    boolean in_address_circle = false;
                    boolean in_friend_circle = false;
                    for (Record c_u : ys_recs) {
                        if (c_u.getString("circle_id").equals(String.valueOf(Constants.ADDRESS_BOOK_CIRCLE)))
                            in_address_circle = true;
                    }
                    if (ys_recs.size() > 0)
                        in_friend_circle = true;

                    boolean beforeFriend = isFriend(userId, uid);

                    if (circleId.equals(String.valueOf(Constants.ADDRESS_BOOK_CIRCLE)) && isadd == true) {
                        //if in his address_book circles
                        if (!in_address_circle && uid.length() < 10) {
                            reqre.createRequest(uid, userId, "0", Constants.REQUEST_PROFILE_ACCESS, "", "", "[]");
                        }
                    } else {
                        //if in one of his circles
                    }
                    fs.setFriends(userId, uid, circleId, reason, isadd);
                    boolean afterFriend = isFriend(userId, uid);
                    if (!beforeFriend && afterFriend) {
                        if (reason != Constants.FRIEND_REASON_INVITE) {
                            if (!in_friend_circle) {
                                //why have this
                                //we just want user quick take actions, but current we use notification to handler this,
                                //so ignore
                                if (false) {
                                    sendFriendFeedbackRequest(userId, uid, false, ua, loc);
                                }
                            }
                        }
                        nl.add(uid);

                        //notification
                        String beSuggestedName = getUser(uid, uid, "display_name").getString("display_name", "");
                        String toName = getUser(userId, userId, "display_name").getString("display_name", "");

                        sendNotification(Constants.NTF_ACCEPT_SUGGEST,
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(userId),
                                createArrayNodeFromStrings(toName, beSuggestedName),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(userId, toName, uid, beSuggestedName),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(userId, uid)
                        );
                    }

                    RecordSet rs = getRelation(uid, userId);
                    Record changed = Record.of("user", userId, "friend", uid, "circle", rs.joinColumnValues("circle_id", ","));
                    hooks.fireFriendshipChanged(changed);
                }
            }

            //notification
            List<String> rl = new ArrayList<String>();
            if (isadd && nl.size() > 0) {
                Record r = Record.fromByteBuffer(fs.isDeleteRecent(userId, StringUtils.join(nl, ", "), 30 * 24 * 60 * 60 * 1000));

                for (String user : nl) {
                    boolean res = r.getBoolean(user, false);
                    if (!res)
                        rl.add(user);
                }

                sendNotification(Constants.NTF_NEW_FOLLOWER,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(rl.toArray(new String[rl.size()]))
                );

            }

            if (isadd && rl.size() > 0) {
                sendPostBySetFriend(userId, StringUtils.join(rl, ","), reason, ua, loc, true, true, true);
            }
            return true;
        } finally {
            closeTransceiver(trans);
            closeTransceiver(transre);
        }
    }

    public Record setFriend(String userId, String friendId, String circleIds, int reason, String ua, String loc) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Friendship.class);
        Transceiver transre = getTransceiver(Request.class);
        Request reqre = getProxy(Request.class, transre);
        try {
            List<String> ll = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (ll.size() <= 0) {
                //delete from all my circles
                RecordSet ys_recs = getRelation(friendId, userId, String.valueOf(Constants.FRIENDS_CIRCLE));
                for (Record r : ys_recs) {
                    setFriends(userId, friendId, r.getString("circle_id"), 0, false, ua, loc);
                }
            } else {
                Friendship fs = getProxy(Friendship.class, trans);
                boolean b = true;
                if (ll.size() == 1 && ll.get(0).contains(String.valueOf(Constants.ADDRESS_BOOK_CIRCLE))) {
                    RecordSet ys_recs = getRelation(friendId, userId, "");
                    for (Record r : ys_recs) {
                        fs.setFriends(userId, friendId, r.getString("circle_id"), 0, false);
                    }
                } else {
                    boolean beforeFriend = isFriend(userId, friendId);
                    if (friendId.length() > 10) {
                        //contact friend,have virtual borqsId
                        Record v_f = RecordSet.fromByteBuffer(fs.getContactFriendByFid(friendId)).getFirstRecord();
                        if (!v_f.isEmpty())
                            b = fs.setContactFriend(userId, friendId, v_f.getString("name"), v_f.getString("content"), circleIds, reason, true, true);
                    } else {
                        b = fs.setFriend(userId, friendId, circleIds, reason);
                    }
                    boolean afterFriend = isFriend(userId, friendId);

                    if (!beforeFriend && afterFriend && friendId.length() < 10) {
                        RecordSet ys_recs = getRelation(userId, friendId, String.valueOf(Constants.FRIENDS_CIRCLE));
                        if (ys_recs.size() <= 0) {
                            if (ll.contains(String.valueOf(Constants.ADDRESS_BOOK_CIRCLE))) {
//                            sendFriendFeedbackRequest(userId, friendId, false, ua, loc);
                                reqre.createRequest(friendId, userId, "0", Constants.REQUEST_PROFILE_ACCESS, "", "", "[]");
                            } else {
//                            reqre.createRequest(friendId, userId, "0", Constants.REQUEST_FRIEND_FEEDBACK, "", "", "[]");
                            }
                        }

                        //notification
                        Record r = Record.fromByteBuffer(fs.isDeleteRecent(userId, friendId, 30 * 24 * 60 * 60 * 1000));

                        boolean res = r.getBoolean(friendId, false);
                        if (!res) {
                            sendNotification(Constants.NTF_NEW_FOLLOWER,
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(userId),
                                    createArrayNodeFromStrings(userId),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(userId),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(friendId)
                            );


                            //notification
                            String beSuggestedName = getUser(friendId, friendId, "display_name").getString("display_name", "");
                            String toName = getUser(userId, userId, "display_name").getString("display_name", "");

                            sendNotification(Constants.NTF_ACCEPT_SUGGEST,
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(userId),
                                    createArrayNodeFromStrings(toName, beSuggestedName),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(userId, toName, friendId, beSuggestedName),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(userId, friendId)
                            );


                            //send stream
                            sendPostBySetFriend(userId, friendId, reason, ua, loc, true, true, true);
                        }
                    }

                    Record changed = Record.of("user", userId, "friend", friendId, "circle", circleIds);
                    hooks.fireFriendshipChanged(changed);
                }
            }

            Record rec = getUsers(userId, friendId, USER_STANDARD_COLUMNS, true).getFirstRecord();
//            RecordSet inCircles = RecordSet.fromJson(JsonUtils.toJson(rec.get("in_circles"), false));
//            inCircles = dealWithInCirclesByGroups(PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, userId, rec.getString("user_id"), inCircles);
//            rec.put("in_circles", inCircles);
            return rec;
//            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record exchangeVcard(String userId, String friendId, String circleIds, int reason,boolean send_request,String ua, String loc) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Friendship.class);
        Transceiver transAccount = getTransceiver(Account.class);
        try {
            List<String> ll = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (ll.size() > 0){
                Friendship fs = getProxy(Friendship.class, trans);
                boolean b = true;
                Transceiver transre = getTransceiver(Request.class);
                Request reqre = getProxy(Request.class, transre);
                if (ll.size() == 1 && ll.get(0).contains(String.valueOf(Constants.ADDRESS_BOOK_CIRCLE))) {
                    RecordSet ys_recs = getRelation(friendId, userId, "");
                    for (Record r : ys_recs) {
                        fs.setFriends(userId, friendId, r.getString("circle_id"), 0, false);
                    }
                } else {
                    if (friendId.length() > 10) {
                        //contact friend,have virtual borqsId
                        Record v_f = RecordSet.fromByteBuffer(fs.getContactFriendByFid(friendId)).getFirstRecord();
                        if (!v_f.isEmpty())
                            b = fs.setContactFriend(userId, friendId, v_f.getString("name"), v_f.getString("content"), circleIds, reason, true, true);
                    } else {
                        b = fs.setFriend(userId, friendId, circleIds, reason);

                        //add by wangpeng  2012-08-15
                        /*Account a = getProxy(Account.class, transAccount);
                    RecordSet userRs =  RecordSet.fromByteBuffer(a.getUsers(userId,"ss"));
                    RecordSet friendRs =  RecordSet.fromByteBuffer(a.getUsers(uid,"ss"));*/
                        Record changed = Record.of("user", userId, "friend", friendId);
                        hooks.sendAccountInfo(changed);
                        //add by wangpeng  2012-08-15
                    }

                    Record changed = Record.of("user", userId, "friend", friendId, "circle", circleIds);
                    hooks.fireFriendshipChanged(changed);
                }
                if (send_request) {
                    boolean if_in = fs.getIfHeInMyCircles(friendId, userId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE));
                    if (!if_in)
                        reqre.createRequest(friendId, userId, "0", Constants.REQUEST_PROFILE_ACCESS, "", "", "[]");
                }
            }

            return getUsers(userId, friendId, USER_STANDARD_COLUMNS, true).getFirstRecord();
//            return b;
        } finally {
            closeTransceiver(transAccount);
            closeTransceiver(trans);
        }
    }

    public String getNowUserGeneralId() throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account a = getProxy(Account.class, trans);
            return toStr(a.getNowGenerateUserId());
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getVirtualUID() throws AvroRemoteException {
        try {
            String friendId = Long.toString(RandomUtils.generateId());
            /*
            String maxUID = getNowUserGeneralId();
            int len = maxUID.length();
            String t1 = StringUtils.substring(friendId, 0, 10);
            String t2 = "";
            for (int i = 0; i < 9 - len; i++) {
                t2 += "0";
            }
            return t1 + t2 + maxUID;
            */
            return friendId;
        } finally {
        }
    }

    public boolean updateVirtualFriendIdToAct(String friendId, String content) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        Transceiver transr = getTransceiver(Stream.class);
        try {
            Friendship f = getProxy(Friendship.class, trans);
            Stream s = getProxy(Stream.class, transr);

            //update stream  add-contact
            RecordSet s1 = RecordSet.fromByteBuffer(s.getSharedPostHasContact1(content));
            if (s1.size() > 0) {
                for (Record r : s1) {
                    String post_id = r.getString("post_id");
                    String mentions = r.getString("mentions");
                    String add_contact = r.getString("add_contact");
                    String has_contact = r.getString("has_contact");

                    List<String> l_mentions = StringUtils2.splitList(mentions, ",", true);
                    List<String> l_contact = StringUtils2.splitList(add_contact, ",", true);

                    for (int jj = l_contact.size() - 1; jj >= 0; jj--) {
                        if (l_contact.get(jj).toString().equalsIgnoreCase(content)) {
                            l_contact.remove(jj);
                            if (!l_mentions.contains(friendId)) {
                                l_mentions.add(friendId);
                                createConversation(Constants.POST_OBJECT, post_id, Constants.C_STREAM_TO, friendId);
                            }
                        }
                    }

                    String newMentions = StringUtils.join(l_mentions, ",");
                    String newAddContact = StringUtils.join(l_contact, ",");
                    boolean has = true;
                    if (l_contact.size() <= 0) {
                        has = false;
                    }
                    s.updatePostHasContact2(post_id, newMentions, newAddContact, has);
                }
            }

            //update friend
            RecordSet recs_oldFriendId = RecordSet.fromByteBuffer(f.getVirtualFriendId(content));
            if (recs_oldFriendId.size() > 0) {
                f.updateVirtualFriendIdToAct(friendId, content);

                //update stream mentions
                for (Record rec : recs_oldFriendId) {
                    RecordSet s2 = RecordSet.fromByteBuffer(s.getSharedPostHasContact2(rec.getString("virtual_friendid")));
                    if (s2.size() > 0) {
                        for (Record r : s2) {
                            String post_id = r.getString("post_id");
                            String mentions = r.getString("mentions");
                            String add_contact = r.getString("add_contact");
                            boolean has_contact = r.getBoolean("has_contact", false);

                            List<String> l_mentions = StringUtils2.splitList(mentions, ",", true);
                            List<String> l_mentions_change = new ArrayList<String>();

                            for (int jj = l_mentions.size() - 1; jj >= 0; jj--) {
                                if (l_mentions.get(jj).toString().equalsIgnoreCase(rec.getString("virtual_friendid"))) {
                                    if (!l_mentions.contains(friendId)) {
                                        l_mentions_change.add(friendId);
                                    }
                                    l_mentions.remove(jj);
                                }
                            }

                            if (l_mentions_change.size() > 0) {
                                for (String fid : l_mentions_change) {
                                    if (!l_mentions.contains(fid))
                                        l_mentions.add(fid);
                                    //add conversation
                                    createConversation(Constants.POST_OBJECT, post_id, Constants.C_STREAM_TO, friendId);
                                }
                            }
                            String newMentions = StringUtils.join(l_mentions, ",");
                            s.updatePostHasContact2(post_id, newMentions, add_contact, has_contact);
                        }
                    }
                }
            }
            return true;
        } finally {
            closeTransceiver(transr);
            closeTransceiver(trans);
        }
    }

    public RecordSet getContactFriend(String userIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship f = getProxy(Friendship.class, trans);
            return RecordSet.fromByteBuffer(f.getContactFriend(userIds));
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getUserFriendhasVirtualFriendId(String userId, String content) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship f = getProxy(Friendship.class, trans);
            return toStr(f.getUserFriendhasVirtualFriendId(userId, content));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record findUidLoginNameNotInID(String content) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Account.class);
        try {
            Account a = getProxy(Account.class, trans);
            return Record.fromByteBuffer(a.findUidLoginNameNotInID(content));
        } finally {
            closeTransceiver(trans);
        }
    }

    public String setContactFriend(String userId, String friendName, String content, String circleIds, int reason, String ua, String loc) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            String friendId = getVirtualUID();
            fs.setContactFriend(userId, friendId, friendName, content, circleIds, reason, true, true);
            fs.createVirtualFriendId(userId, friendId, content, friendName);
            String fromName = getUser(userId, userId, "display_name").getString("display_name");
            String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";

            if (content.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
                String template = Constants.getBundleString(ua, "platformservlet.email.invite.email");
                String temp = FeedbackParams.toSegmentedBase64(true, "/", content, friendName, userId);
                String url = "http://" + SERVER_HOST + "/account/invite?info=" + temp;
                String emailContent = SQLTemplate.merge(template, new Object[][]{
                        {"displayName", fromName},
                        {"fromName", fromName},
                        {"url", url}
                });

                template = Constants.getBundleString(ua, "platformservlet.email.invite.title");
                String title = SQLTemplate.merge(template, new Object[][]{
                        {"fromName", fromName}
                });
                try {
                    sendEmail(title, content, content, emailContent, Constants.EMAIL_ESSENTIAL, lang);
                } catch (Exception e) {
                    L.error("send email error:add contact to friend,email=" + content);
                }
            } else {
                String smsTitle = Constants.getBundleString(ua, "platformservlet.sms.invite.title");
                String temp = FeedbackParams.toSegmentedBase64(true, "/", content, friendName, userId);
                String url = "http://" + SERVER_HOST + "/account/invite?info=" + temp;
                String smsContent = SQLTemplate.merge(smsTitle, new Object[][]{
                        {"fromName", fromName},
                        {"urll", generalShortUrl(url)}
                });
                try {
                    sendSms(content, smsContent + "\\");
                } catch (Exception e) {
                    L.error("send sms error:add contact to friend,phone=" + content + ",and smscontent=" + smsContent);
                }
            }

            return friendId;
        } finally {
            closeTransceiver(trans);
        }
    }

    private void sendPostBySetFriend(String userId, String friendIds, int reason, String ua, String loc, boolean can_comment, boolean can_like, boolean can_reshare) {
        Record rec = new Record();
        rec.put("setFriend", true);
        rec.put("userId", userId);
        rec.put("friendIds", friendIds);
        rec.put("reason", reason);
        rec.put("ua", ua);
        rec.put("loc", loc);
        rec.put("can_comment", can_comment);
        rec.put("can_like", can_like);
        rec.put("can_reshare", can_reshare);
        rec.put("add_to", "");

        MQ mq = MQCollection.getMQ("platform");
        if (mq != null)
            mq.send("stream", rec.toString(false, false));
    }

    public boolean sendPostBySetFriend0(String userId, String friendIds, int reason, String ua, String loc, boolean can_comment, boolean can_like, boolean can_reshare) throws AvroRemoteException {
        //if exist stream in 24 hours ,by addfriend
        Transceiver transtream = getTransceiver(Stream.class);
        Transceiver transf = getTransceiver(Friendship.class);
        Stream stream = getProxy(Stream.class, transtream);
        long dateDiff = 24 * 60 * 60 * 1000L;
        long minDate = DateUtils.nowMillis() - dateDiff;
        RecordSet old_recs = RecordSet.fromByteBuffer(stream.topOneStreamBySetFriend(Constants.FRIEND_SET_POST, userId, minDate));
        if (old_recs.size() <= 0) {
            String message = "";
            if (reason == Constants.FRIEND_REASON_SOCIALCONTACT)
//                message = "基于系统智能推荐";
                message = "";
            //send stream
            Record post = Record.of("message", message, "app", Constants.APP_TYPE_BPC);
            RecordSet u = getUsers(userId, friendIds, USER_LIGHT_COLUMNS_QIUPU, false);
            post.put("attachments", u.toString(false, false));
            post.put("type", Constants.FRIEND_SET_POST);
            post.put("target", "");
            post.put("device", ua);
            post.put("app_data", "");
            post.put("mentions", "");
            post.put("privince", false);
            post.put("location", loc);
            post.put("can_comment", can_comment);
            post.put("can_like", can_like);
            post.put("can_reshare", can_reshare);
            post.put("add_to", "");

            post(userId, post, String.valueOf(Constants.APP_TYPE_BPC));
        } else {
            Record r = old_recs.getFirstRecord();
            if (r.getString("attachments").length() > 2) {

                List<String> nowUserList = new ArrayList<String>();
                RecordSet oldAttachment = RecordSet.fromJson(r.getString("attachments"));
                //friendIds
                String olduserString = oldAttachment.joinColumnValues("user_id", ",");
                List<String> tempUserList = StringUtils2.splitList(toStr(friendIds), ",", true);
                List<String> olduserList = StringUtils2.splitList(toStr(olduserString), ",", true);

                for (String uid : tempUserList) {
                    if (!olduserList.contains(uid)) {
                        nowUserList.add(uid);
                    }
                }

                RecordSet u = getUsers(userId, StringUtils.join(nowUserList, ","), USER_LIGHT_COLUMNS_QIUPU, false);
                for (Record u0 : u) {
                    oldAttachment.add(u0);
                }
                Friendship f = getProxy(Friendship.class, transf);
                for (Record rd : oldAttachment) {
                    String uid = rd.getString("user_id");
                    long created_time = Record.fromByteBuffer(f.getMyFriends(userId, uid)).getInt("created_time");
                    if ((created_time + dateDiff) < DateUtils.nowMillis())
                        oldAttachment.remove(rd);
                }

                stream.updatePostFor(r.getString("post_id"), oldAttachment.toString(false, false), DateUtils.nowMillis(), DateUtils.nowMillis());
            }
        }

        return true;
    }


    public boolean isFriend(String sourceUserId, String targetUserId) throws AvroRemoteException {
        return isHisFriend(targetUserId, sourceUserId);
    }

    public boolean isHisFriend(String sourceUserId, String targetUserId) throws AvroRemoteException {
        RecordSet recs = getRelation(sourceUserId, targetUserId);
        if (recs.isEmpty())
            return false;

        for (Record rec : recs) {
            if (rec.checkGetInt("circle_id") == Constants.BLOCKED_CIRCLE)
                return false;
        }
        return true;
    }

    public RecordSet getFriends0(String userId, String circleIds, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return RecordSet.fromByteBuffer(fs.getFriends(userId, circleIds, page, count));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getBothFriends(String viewerId, String userId, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            RecordSet bo = RecordSet.fromByteBuffer(fs.getBothFriendsIds(viewerId, userId, page, count));
            String uids = bo.joinColumnValues("friend", ",");
            return getUsers(viewerId, uids, USER_STANDARD_COLUMNS);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getFriends(String viewerId, String userId, String circleIds, String cols, int page, int count) throws AvroRemoteException {
        return getFriends(viewerId, userId, circleIds, cols, false, page, count);
    }

    public RecordSet getFriends(String viewerId, String userId, String circleIds, String cols, boolean inPublicCircles, int page, int count) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        cols = parseUserColumns(cols);
        RecordSet recs = getFriends0(userId, circleIds, page, count);
//        if (cols.trim().equals("user_id")) {
//            recs.removeColumns("circle");
//            recs.renameColumn("friend", "user_id");
//            return recs;
//        } else {
//            String friendIds = recs.joinColumnValues("friend", ",");
//            return getUsers(viewerId, friendIds, cols);
//        }
        String friendIds = recs.joinColumnValues("friend", ",");

        RecordSet users = new RecordSet();
        if (StringUtils.isNotBlank(friendIds)) {
            users = getUsers(viewerId, friendIds, cols);

            if (inPublicCircles) {
                for (Record user : users) {
                    RecordSet inCircles = RecordSet.fromJson(JsonUtils.toJson(user.get("in_circles"), false));
                    inCircles = dealWithInCirclesByGroups(PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, userId, user.getString("user_id"), inCircles);
                    user.put("in_circles", inCircles);
                }
            }
        }

        return users;
    }

    public RecordSet getFriendsV2(String viewerId, String userId, String circleIds, String cols, int page, int count) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        cols = parseUserColumns(cols);
        List<String> l = new ArrayList<String>();
        List<String> groupsFromCircleIds = new ArrayList<String>();
        Transceiver trans = getTransceiver(Group.class);
        Transceiver trans0 = getTransceiver(Friendship.class);

        try {
            Group group = getProxy(Group.class, trans);
            if (StringUtils.equals(circleIds, String.valueOf(FRIENDS_CIRCLE))) {
                l.add(String.valueOf(FRIENDS_CIRCLE));
                RecordSet recs = RecordSet.fromByteBuffer(group.findGroupsByMember(PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, Long.parseLong(userId), GROUP_LIGHT_COLS));
                groupsFromCircleIds = recs.getStringColumnValues(GRP_COL_ID);
            } else {
                l = StringUtils2.splitList(circleIds, ",", true);
                groupsFromCircleIds = getGroupIdsFromMentions(l);
                l.removeAll(groupsFromCircleIds);
            }

            RecordSet recs = getFriends0(userId, StringUtils2.joinIgnoreBlank(",", l), page, count);
//        if (cols.trim().equals("user_id")) {
//            recs.removeColumns("circle");
//            recs.renameColumn("friend", "user_id");
//            return recs;
//        } else {
//            String friendIds = recs.joinColumnValues("friend", ",");
//            return getUsers(viewerId, friendIds, cols);
//        }
            int size = recs.size();
            String friendIds = recs.joinColumnValues("friend", ",");

            //groups
            if (size <= 0) {
                Friendship fs = getProxy(Friendship.class, trans0);
                int friendCount = fs.getFriendsCount(userId);
                int friendPageCount = friendCount / count;
                if (friendCount % count != 0)
                    friendPageCount += 1;
                page -= friendPageCount;

                List<String> allFriendIds = getFriends0(userId, StringUtils2.joinIgnoreBlank(",", l), -1, -1).getStringColumnValues("friend");
                friendIds = "";
                if (!groupsFromCircleIds.isEmpty()) {
                    String groupIds = StringUtils2.joinIgnoreBlank(",", groupsFromCircleIds);
                    String memberIds = toStr(group.getMembers(groupIds, page, count));
                    List<String> members = StringUtils2.splitList(memberIds, ",", true);
                    for (String member : members) {
                        if (!allFriendIds.contains(member))
                            friendIds += member + ",";
                    }
                    friendIds = StringUtils.substringBeforeLast(friendIds, ",");
                }
            }

            RecordSet users = new RecordSet();
            if (StringUtils.isNotBlank(friendIds)) {
                users = getUsers(viewerId, friendIds, cols);
                for (Record user : users) {
                    RecordSet inCircles = RecordSet.fromJson(JsonUtils.toJson(user.get("in_circles"), false));
                    inCircles = dealWithInCirclesByGroups(PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, userId, user.getString("user_id"), inCircles);
                    user.put("in_circles", inCircles);
                }
            }

            return users;
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getFriendsId(String viewerId, String userId, String circleIds, String cols, int page, int count) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);
        cols = parseUserColumns(cols);
        RecordSet recs = getFriends0(userId, circleIds, page, count);
        String friendIds = recs.joinColumnValues("friend", ",");
        return friendIds;
    }

    public RecordSet getFollowers(String viewerId, String userId, String circleIds, String cols, int page, int count) throws AvroRemoteException {
        cols = parseUserColumns(cols);
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(fs.getFollowers(userId, circleIds, page, count));
            if (cols.trim().equals("user_id")) {
                recs.removeColumns("circle");
                recs.renameColumn("follower", "user_id");
                return recs;
            } else {
                //String friendIds = recs.joinColumnValues("follower", ",");
                String followerIds = recs.joinColumnValues("follower", ",");
                RecordSet recs_u = getUsers(viewerId, followerIds, cols);

                Map map = new HashMap();
                for (Record u : recs_u) {
                    map.put(u.getString("user_id"), u.toString(false, false));
                }


                RecordSet recs0 = new RecordSet();
                for (Record r : recs) {
                    String fId = r.getString("follower");
                    if (map.get(fId) != null) {
                        Record us = Record.fromJson(map.get(fId).toString());
                        if (!us.isEmpty()) {
                            us.put("relation_created_time", r.getString("created_time"));
                            recs0.add(us);
                        }
                    }
                }
                return recs0;
            }
        } finally {
            closeTransceiver(trans);
        }
    }


    public RecordSet getRelation(String sourceUserId, String targetUserId, String circleId) throws AvroRemoteException {
        Validate.notNull(sourceUserId);
        Validate.notNull(targetUserId);
//        checkUserIds(sourceUserId, targetUserId);

        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return RecordSet.fromByteBuffer(fs.getRelation(sourceUserId, targetUserId, circleId));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getAllRelation(String viewerId, String userIds, String circleId, String inTheirOrInMine) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return RecordSet.fromByteBuffer(fs.getAllRelation(viewerId, userIds, circleId, inTheirOrInMine));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getRelation(String sourceUserId, String targetUserId) throws AvroRemoteException {
        return getRelation(sourceUserId, targetUserId, "");
    }

    public Record getBidiRelation(String sourceUserId, String targetUserId, String circleId) throws AvroRemoteException {
        Validate.notNull(sourceUserId);
        Validate.notNull(targetUserId);
//        checkUserIds(sourceUserId, targetUserId);

        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return Record.fromByteBuffer(fs.getBidiRelation(sourceUserId, targetUserId, circleId));
        } finally {
            closeTransceiver(trans);
        }
    }


    public boolean setRemark(String userId, String friendId, String remark) throws AvroRemoteException {
        Validate.notNull(userId);
        Validate.notNull(friendId);
//        Validate.notNull(remark);
        checkUserIds(userId, friendId);

        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return fs.setRemark(userId, friendId, remark);
        } finally {
            closeTransceiver(trans);
        }
    }

    RecordSet getRemarks0(String userId, String friendIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Friendship.class);
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            return RecordSet.fromByteBuffer(fs.getRemarks(userId, friendIds));
        } finally {
            closeTransceiver(trans);
        }
    }


    public RecordSet getRemarks(String userId, String friendIds) throws AvroRemoteException {
        Validate.notNull(userId);
//        checkUserIds(userId, friendIds);

        friendIds = parseUserIds(userId, friendIds);
        return getRemarks0(userId, friendIds);
    }


//=============================================suggested_user===begin================================================

    public boolean createSuggestUser(String userId, String suggestedUsers, int type, String reason) throws AvroRemoteException {
        Validate.notNull(userId);
        Validate.notNull(suggestedUsers);
        List<String> suggestedUsers0 = StringUtils2.splitList(toStr(suggestedUsers), ",", true);
        if (suggestedUsers0.size() > 0) {
            for (int i = suggestedUsers0.size() - 1; i >= 0; i--) {
                if (suggestedUsers0.get(i).length() > 10)
                    suggestedUsers0.remove(i);
            }
        }
        suggestedUsers = StringUtils.join(suggestedUsers0, ",");
        suggestedUsers = parseUserIds(userId, suggestedUsers);
        checkUserIds(userId, suggestedUsers);

        Transceiver trans = getTransceiver(SuggestedUser.class);
        try {
            SuggestedUser su = getProxy(SuggestedUser.class, trans);
            return su.createSuggestUser(userId, suggestedUsers, type, reason);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean refuseSuggestUser(String userId, String suggested) throws AvroRemoteException {
        Validate.notNull(userId);
        Validate.notNull(suggested);
        checkUserIds(userId, suggested);

        Transceiver trans = getTransceiver(SuggestedUser.class);
        try {
            SuggestedUser su = getProxy(SuggestedUser.class, trans);
            return su.refuseSuggestUser(userId, suggested);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean backSuggestUser(String userId) throws AvroRemoteException {
        Validate.notNull(userId);
        Transceiver trans = getTransceiver(SuggestedUser.class);
        try {
            SuggestedUser su = getProxy(SuggestedUser.class, trans);
            return su.backSuggestUser(userId, DEFAULT_IGNORE_BACK_DATE);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean createRequestAttention(String userId, String friendId) throws AvroRemoteException {
        Validate.notNull(userId);
        Transceiver trans = getTransceiver(SuggestedUser.class);
        Transceiver tranf = getTransceiver(Friendship.class);
        try {
            SuggestedUser su = getProxy(SuggestedUser.class, trans);
            Friendship f = getProxy(Friendship.class, tranf);

            RecordSet recs = RecordSet.fromByteBuffer(f.getRelation(userId, friendId, String.valueOf(Constants.FRIENDS_CIRCLE)));

            if (recs.size() == 0) {
                createSuggestUser(userId, String.valueOf(friendId), Integer.valueOf(Constants.REQUEST_ATTENTION), "");

                Record u = getUser(userId, userId, "display_name");

                sendNotification(Constants.NTF_REQUEST_ATTENTION,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(userId, u.getString("display_name")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId, u.getString("display_name")),
                        createArrayNodeFromStrings(userId, u.getString("display_name")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(friendId)
                );


            }
            return true;
        } finally {
            closeTransceiver(trans);
            closeTransceiver(tranf);
        }
    }

    public boolean deleteSuggestUser(String userId, String suggested) throws AvroRemoteException {
        Validate.notNull(userId);
        Validate.notNull(suggested);
        checkUserIds(userId, suggested);

        Transceiver trans = getTransceiver(SuggestedUser.class);
        try {
            SuggestedUser su = getProxy(SuggestedUser.class, trans);
            return su.deleteSuggestUser(userId, suggested);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateSuggestUserReason() throws AvroRemoteException {
        Transceiver transu = getTransceiver(Account.class);
        Transceiver trans = getTransceiver(SuggestedUser.class);
        Account a = getProxy(Account.class, transu);
        SuggestedUser su = getProxy(SuggestedUser.class, trans);
        Transceiver transs = getTransceiver(SocialContacts.class);
        SocialContacts so = getProxy(SocialContacts.class, transs);
        Transceiver tranf = getTransceiver(Friendship.class);
        Friendship f = getProxy(Friendship.class, tranf);

        //==================================================

        RecordSet recs = findAllUserIds(true);
        for (Record r : recs) {
            RecordSet rs0 = RecordSet.fromByteBuffer(su.getSuggestUserHistory(r.getString("user_id"), 1000));
            for (Record rs : rs0) {
                if (rs.getInt("type") == Integer.valueOf(Constants.FROM_ADDRESS_HAVECOMMONBORQSID)) {
                    //  共同联系人
                    RecordSet bo = RecordSet.fromByteBuffer(so.getCommSocialContactsU(rs.getString("user"), rs.getString("suggested")));
                    String bothF = bo.joinColumnValues("uid", ",");

                    if (bothF.length() > 0) {
                        //update
                        su.updateSuggestUser(rs.getString("user"), rs.getString("suggested"), Integer.valueOf(Constants.FROM_ADDRESS_HAVECOMMONBORQSID), bothF);
                    }

                }

                if (rs.getInt("type") == Integer.valueOf(Constants.IN_COMMON_FRIENDS)) {
                    //  共同好友
                    RecordSet bo = RecordSet.fromByteBuffer(f.getBothFriendsIds(rs.getString("user"), rs.getString("suggested"), 0, 1000));
                    String bothF = bo.joinColumnValues("friend", ",");
                    if (bothF.length() > 0) {
                        su.updateSuggestUser(rs.getString("user"), rs.getString("suggested"), Integer.valueOf(Constants.IN_COMMON_FRIENDS), bothF);
                    }
                }
            }
        }

        //==================================================

        return true;
    }

    public RecordSet getDistinctUsername(String uid) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        SocialContacts so = getProxy(SocialContacts.class, trans);
        try {
            RecordSet recs = RecordSet.fromByteBuffer(so.getDistinctUsername(uid));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getSocialcontactUsername(String owner,String uid) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        SocialContacts so = getProxy(SocialContacts.class, trans);
        try {
            RecordSet recs = RecordSet.fromByteBuffer(so.getUserName(owner,uid));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getDistinctOwner(String uid, String username) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        SocialContacts so = getProxy(SocialContacts.class, trans);
        try {
            RecordSet recs = RecordSet.fromByteBuffer(so.getDistinctOwner(uid, username));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getSuggestUser(String userId, int limit, boolean getBack) throws AvroRemoteException {
        Validate.notNull(userId);
//        checkUserIds(userId);
        if (getBack) {
            backSuggestUser(userId);
        }
        //if
        Transceiver trans = getTransceiver(SuggestedUser.class);
        Transceiver transu = getTransceiver(Account.class);
        SuggestedUser su = getProxy(SuggestedUser.class, trans);
        Account account = getProxy(Account.class, transu);

        RecordSet rs = new RecordSet();
        RecordSet rs0 = RecordSet.fromByteBuffer(su.getSuggestUser(userId, limit));
        if (rs0.size() <= 10 && rs0.size()<limit) {
            //study from address ,contact have borqsid,
            createSuggestUserFromHaveBorqsId(userId);

            //study from address ,have common  lxr,
            createSuggestUserFromHaveCommLXR(userId);

            //study from address,for has my contactinfo
            createSuggestUserByHasMyContact(userId);

            //study from friend ,for common friend
            createSuggestUserFromCommonFriends(userId);

            // the same school

            // the same company

            //get delete from suggest
            backSuggestUser(userId);

            rs = RecordSet.fromByteBuffer(su.getSuggestUser(userId, limit));
            if (rs.size() == 0) {
                autoCreateSuggestusers(userId);
                rs.addAll(RecordSet.fromByteBuffer(su.getSuggestUser(userId, limit)));
            }
        } else {
            rs = rs0;
        }

        try {
//            RecordSet rs=  RecordSet.fromByteBuffer(su.getSuggestUser(userId, limit));
            RecordSet outrs0 = new RecordSet();
            if (rs.size() > 0) {
                for (int i = 0; i < rs.size(); i++) {
                    String suggestedId = rs.get(i).getString("suggested");
                    Record u = getUsers(userId, suggestedId, "user_id, display_name, image_url,remark,perhaps_name").getFirstRecord();
                    if (!u.isEmpty()) {
                        u.put("suggest_type", rs.get(i).getString("type"));
                        u.put("suggest_reason", "");
                        if (rs.get(i).getInt("type") == Long.parseLong(Constants.RECOMMENDER_USER)
                                || rs.get(i).getInt("type") == Long.parseLong(Constants.FROM_ADDRESS_HAVECOMMONBORQSID)
                                || rs.get(i).getInt("type") == Long.parseLong(Constants.IN_COMMON_FRIENDS)) {
                            String reasonStr = rs.get(i).getString("reason");
                            if (reasonStr.length() > 0) {
                                RecordSet reasonUser = RecordSet.fromByteBuffer(account.getUsers(reasonStr, "user_id, display_name, image_url,remark,perhaps_name"));
                                u.put("suggest_reason", reasonUser);
                            }
                        }
                        outrs0.add(u);
                    }
                }
            }
            return outrs0;
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record updateUserStatus(String userId, String newStatus, String device, String location,boolean post,boolean can_comment, boolean can_like, boolean can_reshare) throws AvroRemoteException, UnsupportedEncodingException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Record user = new Record();
        user.put("status", newStatus);

        //update Status
        String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        boolean b = updateAccount(userId, user, lang);
        Record out_rec = new Record();
        if (b) {
            if (post) {
                String postid = post(userId, Constants.TEXT_POST, newStatus, "[]", toStr(Constants.APP_TYPE_QIUPU), "", "", "", "", false, "", device, location, "", "", can_comment, can_like, can_reshare, "");
                if (Long.parseLong(postid) > 0) {
                    out_rec = getFullPostsForQiuPu(userId, postid,true).getFirstRecord();
                }
            }
        }
        return out_rec;
    }

    public boolean setMiscellaneous(String userId, String phone, String lang) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);
        Record user = new Record();
        RecordSet recs = getUsers(userId, userId, "miscellaneous");
        if (recs.size() > 0) {
            String ml = recs.getFirstRecord().getString("miscellaneous");
            if (ml.length() > 10) {
                Record rec = Record.fromJson(ml);
                if (phone.equals("0")) {
                    rec.removeColumns("openface.phone");
                } else {
                    rec.put("openface.phone", phone);
                }
                user.put("miscellaneous", rec.toString(false, false));
            } else {
                user.put("miscellaneous", Record.of("openface.phone", phone));
            }
        }
        return updateAccount(userId, user, lang);
    }

    public int findUidByMiscellaneous(String miscellaneous) throws AvroRemoteException {
        Transceiver transu = getTransceiver(Account.class);
        try {
            Account a = getProxy(Account.class, transu);
            RecordSet rec = RecordSet.fromByteBuffer(a.findUidByMiscellaneous(toStr(miscellaneous)));
            return rec.size() <= 0 ? 0 : (int) rec.getFirstRecord().getInt("user_id", 0);
        } finally {
            closeTransceiver(transu);
        }
    }

    //=============================================suggested_user===end================================================
    protected String post(String userId, Record post, String appId) throws AvroRemoteException {
        return post(userId, post, new ArrayList<String>(), new ArrayList<String>(), appId);
    }

    // Stream
    protected String post(String userId, Record post, List<String> emails, List<String> phones, String appId) throws AvroRemoteException {
        Validate.notNull(userId);
        Validate.notNull(post);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            String postId = "";
            int type0 = Integer.valueOf(post.getString("type"));
            int flag = 0;
            try {
                if (type0 == Constants.PHOTO_POST) {
                    long dateDiff = 1000 * 60 * 10L;
                    Record old_stream = RecordSet.fromByteBuffer(stream.topOneStreamByShare(Constants.PHOTO_POST, userId,post.getString("message"), post.getString("mentions"),(int)post.getInt("privince"), dateDiff)).getFirstRecord();
                    if (!old_stream.isEmpty()) {
                        postId = old_stream.getString("post_id");
                        RecordSet old_attachments = RecordSet.fromJson(old_stream.getString("attachments"));
                        RecordSet new_attachments = RecordSet.fromJson(post.getString("attachments"));
                        old_attachments.add(0, new_attachments.get(0));
                        stream.updatePostForAttachmentsAndUpdateTime(postId, old_attachments.toString(false, false), DateUtils.nowMillis());
                        flag = 1;
                    }
                }
            } catch (Exception e) {
            }

            if (flag == 0) {
                postId = toStr(stream.createPost(userId, post.toByteBuffer()));
                createConversation(Constants.POST_OBJECT, postId, Constants.C_STREAM_POST, userId);
                if (post.getString("mentions").length() > 0)
                    createConversation(Constants.POST_OBJECT, postId, Constants.C_STREAM_TO, post.getString("mentions"));
                if (post.getString("add_to").length() > 0)
                    createConversation(Constants.POST_OBJECT, postId, Constants.C_STREAM_ADDTO, post.getString("add_to"));
            }
            //notification

            Record r_post = getPosts(postId, "post_id,mentions,source,target,type,message").getFirstRecord();

            String body = r_post.getString("message", "");
            Record thisUser = getUsers(userId, userId, "display_name", true).getFirstRecord();
            int type = Integer.valueOf(r_post.getString("type"));
            if (type == Constants.APK_POST) {
                if (post.getString("target").split("-")[0].length() > 0)
                    createConversation(Constants.APK_OBJECT, post.getString("target").split("-")[0].toString(), Constants.C_APK_SHARE, userId);
//                NotificationSender notif = new SharedAppNotifSender(this, null);

                Record mcs = thisTrandsGetApkInfo(userId, r_post.getString("target"), "app_name", 1000).getFirstRecord();

                sendNotification(Constants.NTF_APP_SHARE,
                        createArrayNodeFromStrings(appId),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(r_post.getString("target"), userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(r_post.getString("target")),
                        createArrayNodeFromStrings(r_post.getString("target"), userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(postId, userId)
                );

                String mentions = post.getString("mentions");
                List<String> l = StringUtils2.splitList(mentions, ",", true);
                for (String l0 : l) {
                    long id = Long.parseLong(l0);
                    if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                            && (id <= Constants.GROUP_ID_END))
                        sendGroupNotification(id, new SharedAppNotifSender(this, null), userId, new Object[]{postId, userId}, body, r_post.getString("target"), userId, thisUser.getString("display_name"), mcs.getString("app_name"));
                }

                body = mcs.getString("app_name");
            } else if ((type == Constants.TEXT_POST) || (type == Constants.LINK_POST) || (type == Constants.APK_LINK_POST) || type == Constants.BOOK_POST) {
                //notification
                if ((type == Constants.APK_LINK_POST)) {
                    String attachments = r_post.getString("attachments");
                    ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                    if ((aNode != null) && (aNode.get(0) != null))
                        body = aNode.get(0).get("href").getTextValue();
                } else if (type == Constants.BOOK_POST) {
                    String attachments = post.getString("attachments");
                    if (attachments.length() >= 2) {
                        ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                        if ((aNode != null) && (aNode.get(0) != null))
                            body = aNode.get(0).get("summary").getTextValue();
                    }
                } else if (type == Constants.LINK_POST) {
                    String attachments = post.getString("attachments");
                    if (attachments.length() > 2) {
                        ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                        if ((aNode != null) && (aNode.get(0) != null))
                            body = aNode.get(0).get("url").getTextValue();
                    }
                }

                String displayName = getUser(userId, userId, "display_name").getString("display_name", "");

                sendNotification(Constants.NTF_OTHER_SHARE,
                        createArrayNodeFromStrings(String.valueOf(type)),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(String.valueOf(type), displayName),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(postId),
                        createArrayNodeFromStrings(String.valueOf(type), userId, displayName),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(postId)
                );

                String mentions = post.getString("mentions");
                List<String> l = StringUtils2.splitList(mentions, ",", true);
                for (String l0 : l) {
                    long id = Long.parseLong(l0);
                    if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                            && (id <= Constants.GROUP_ID_END))
                        sendGroupNotification(id, new SharedNotifSender(this, null), userId, new Object[]{postId}, body, String.valueOf(type), displayName);
                }
            } else if (type == Constants.PHOTO_POST) {
                String attachments = post.getString("attachments");
                RecordSet r0 = RecordSet.fromJson(attachments);
                String displayName = getUser(userId, userId, "display_name").getString("display_name", "");
                if (!r0.getFirstRecord().isEmpty()) {
                    createConversation(Constants.PHOTO_OBJECT, r0.getFirstRecord().getString("photo_id"), Constants.C_PHOTO_SHARE, userId);
                    body = r0.getFirstRecord().getString("photo_caption");
                    sendNotification(Constants.NTF_PHOTO_SHARE,
                            createArrayNodeFromStrings(String.valueOf(type)),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(String.valueOf(type), displayName),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(r0.getFirstRecord().getString("photo_id")),
                            createArrayNodeFromStrings(String.valueOf(type), userId, displayName),
                            createArrayNodeFromStrings(body),
                            createArrayNodeFromStrings(body),
                            createArrayNodeFromStrings("PHOTO"),
                            createArrayNodeFromStrings(postId)
                    );
                }

                String mentions = post.getString("mentions");
                List<String> l = StringUtils2.splitList(mentions, ",", true);
                for (String l0 : l) {
                    long id = Long.parseLong(l0);
                    if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                            && (id <= Constants.GROUP_ID_END))
                        sendGroupNotification(id, new PhotoSharedNotifSender(this, null), userId, new Object[]{postId}, body, String.valueOf(type), displayName);
                }
            } else if (type == Constants.FILE_POST || type == Constants.AUDIO_POST || type == Constants.VIDEO_POST) {
                String attachments = post.getString("attachments");
                RecordSet r0 = RecordSet.fromJson(attachments);
                if (!r0.getFirstRecord().isEmpty()) {
                    createConversation(Constants.FILE_OBJECT, r0.getFirstRecord().getString("file_id"), Constants.C_FILE_SHARE, userId);
                    body = r0.getFirstRecord().getString("title");
                }
                String displayName = getUser(userId, userId, "display_name").getString("display_name", "");
                sendNotification(Constants.NTF_FILE_SHARE,
                        createArrayNodeFromStrings(String.valueOf(type)),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(String.valueOf(type), displayName,body),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(postId),
                        createArrayNodeFromStrings(String.valueOf(type), userId, displayName,body),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings("PHOTO"),
                        createArrayNodeFromStrings(postId)
                );

                String mentions = post.getString("mentions");
                List<String> l = StringUtils2.splitList(mentions, ",", true);
                for (String l0 : l) {
                    long id = Long.parseLong(l0);
                    if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                            && (id <= Constants.GROUP_ID_END))
                        sendGroupNotification(id, new PhotoSharedNotifSender(this, null), userId, new Object[]{postId}, body, String.valueOf(type), displayName);
                }
            }
            else if (type == Constants.FRIEND_SET_POST) {

            }
            if ((type & Constants.APPLY_POST) ==Constants.APPLY_POST && appId .equals("10001")) {   // 播思创意大赛
                if ((type & Constants.TEXT_POST) ==Constants.TEXT_POST) {    //只报名
                      body = "报名参加了播思创意大赛！";
                } else {      //提交作品
                    String attachments = post.getString("attachments");
                    RecordSet r0 = RecordSet.fromJson(attachments);
                    if (!r0.getFirstRecord().isEmpty()) {
                        createConversation(Constants.FILE_OBJECT, r0.getFirstRecord().getString("file_id"), Constants.C_FILE_SHARE, userId);
                        body = r0.getFirstRecord().getString("title");
                    }
                }
                String displayName = getUser(userId, userId, "display_name").getString("display_name", "");
                    sendNotification(Constants.NTF_BORQS_APPLY,
                            createArrayNodeFromStrings(String.valueOf(type)),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(String.valueOf(type), displayName, body),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(postId),
                            createArrayNodeFromStrings(String.valueOf(type), userId, displayName, body),
                            createArrayNodeFromStrings(body),
                            createArrayNodeFromStrings(body),
                            createArrayNodeFromStrings("APPLY"),
                            createArrayNodeFromStrings(postId)
                    );
            }
            //not borqs account
            String device = post.getString("device", "");
            String emailContent = composeShareContent(userId, type, body, true, device);
            String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
            String template = Constants.getBundleString(device, "platform.compose.share.title");
            String title = SQLTemplate.merge(template, new Object[][]{
                    {"displayName", thisUser.getString("display_name")}
            });
            for (String email : emails) {
                String uid = findUserIdByUserName(email);
                if (StringUtils.equals(uid, "0") || StringUtils.isBlank(uid)) {
                    Record setting = getNUserPreferencesByUsers(Constants.EMAIL_SHARE_TO, email);
                    String value = setting.getString(email, "0");
                    if (value.equals("0")) {
                        sendEmail(title, email, email, emailContent, Constants.EMAIL_SHARE_TO, lang);
                    }
                } else if (sendEmail) {
                    Record setting = getPreferencesByUsers(Constants.EMAIL_SHARE_TO, uid);
                    String value = setting.getString(uid, "0");
                    if (value.equals("0")) {
                        sendEmail(title, email, email, emailContent, Constants.EMAIL_SHARE_TO, lang);
                    }
                }
            }

            String smsContent = composeShareContent(userId, type, body, false, device);
            for (String phone : phones) {
                String uid = findUserIdByUserName(phone);
                if (StringUtils.equals(uid, "0") || StringUtils.isBlank(uid)) {
                    sendSms(phone, smsContent + "\\");
                    template = Constants.getBundleString(device, "platform.compose.share.download");
                    String download = SQLTemplate.merge(template, new Object[][]{
                            {"serverHost", SERVER_HOST}
                    });
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {

                    }
                    sendSms(phone, download + "\\");
                } else if (sendSms) {
                    sendSms(phone, smsContent + "\\");
                }
            }

            return postId;
        } finally {
            closeTransceiver(trans);
        }
    }


    private void autoPost(String userId, int type, String msg, String attachments,
                          String appId, String packageName, String apkId,
                          String appData,
                          String mentions,
                          boolean secretly, String cols, String device, String location,
                          boolean can_comment, boolean can_like, boolean can_reshare, String add_to, String add_contact, boolean has_contact) {
        Record rec = new Record();
        rec.put("setFriend", false);
        rec.put("userId", userId);
        rec.put("type", type);
        rec.put("msg", msg);
        rec.put("attachments", attachments);
        rec.put("appId", appId);
        rec.put("packageName", packageName);
        rec.put("apkId", apkId);
        rec.put("app_data", appData);
        rec.put("mentions", mentions);
        rec.put("secretly", secretly);
        rec.put("cols", cols);
        rec.put("device", device);
        rec.put("location", location);
        rec.put("can_comment", can_comment);
        rec.put("can_like", can_like);
        rec.put("can_reshare", can_reshare);
        rec.put("add_to", add_to);

        rec.put("add_contact", add_contact);
        rec.put("has_contact", 1);

        String rec_str = rec.toString(false, false);
        MQ mq = MQCollection.getMQ("platform");
        if ((mq != null) && (rec_str.length() < 1024))
            mq.send("stream", rec_str);
    }

    public String post(String userId, int type, String msg, String attachments,
                       String appId, String packageName, String apkId,
                       String appData,
                       String mentions,
                       boolean secretly, String cols, String device, String location,
                       String url, String linkImagAddr,
                       boolean can_comment, boolean can_like, boolean can_reshare,
                       String add_to) throws AvroRemoteException, UnsupportedEncodingException {
        List<String> emails = getEmails(mentions);
        List<String> phones = getPhones(mentions);
        mentions = getOldMentions(userId, mentions);

        List<String> add_contact = new ArrayList<String>();
        add_contact.addAll(emails);
        add_contact.addAll(phones);

        for (int k = add_contact.size() - 1; k >= 0; k--) {
            String virtualFriendId = getUserFriendhasVirtualFriendId(userId, add_contact.get(k));
            if (!StringUtils.equals(virtualFriendId, "0")) {
                add_contact.remove(k);
            }
        }
        String add_contact_s = StringUtils.join(add_contact, ",");

        boolean has_contact = false;
        if (add_contact_s.length() > 0)
            has_contact = true;

        if (msg.toString().length() > 4096)
            msg = msg.substring(0, 4000);
        Record post = Record.of("message", msg, "app", appId);

        if (!apkId.equals("") || !packageName.equals(""))
            appId = toStr(Constants.APP_TYPE_QIUPU);

        post.put("attachments", (attachments.length() <= 2) ? "[]" : "[" + attachments + "]");
        post.put("type", type);
        post.put("target", "");

        post.put("device", device);
        post.put("app_data", appData);
        post.put("mentions", parseUserIds(userId, mentions));
        post.put("privince", secretly);
        post.put("location", location);
        post.put("can_comment", can_comment);
        post.put("can_like", can_like);
        post.put("can_reshare", can_reshare);
        post.put("add_to", add_to);
        post.put("add_contact", add_contact_s);
        post.put("has_contact", has_contact);

        if (appId.equals(toStr(Constants.APP_TYPE_QIUPU))) {
            Transceiver tranq = getTransceiver(QiupuInterface.class);
            QiupuInterface qp = getProxy(QiupuInterface.class, tranq);
            if (!apkId.equals("")) {
                String[] ss = StringUtils.split(StringUtils.trimToEmpty(apkId), '-');
                String package_ = ss[0].trim();

                if (qp.existPackage(package_)) {
                    RecordSet mcs = thisTrandsGetApkInfo(userId, toStr(apkId), cols, 1000);
                    if (mcs.size() > 0) {
                        post.put("attachments", mcs);
                        post.put("target", apkId);
                    } else {
                        RecordSet s = thisTrandsGetSingleApkInfo(userId, toStr(ss[0]), cols, 1000);
                        post.put("attachments", s);
                        post.put("target", s.getFirstRecord().getString("apk_id"));
                    }
                    if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                        post.put("type", String.valueOf(type));
                    } else {
                        post.put("type", String.valueOf(Constants.APK_POST));
                    }
                } else {
                    String href = "http://market.android.com/details?id=" + package_;
                    RecordSet r = new RecordSet();
                    r.add(Record.of("href", href));
                    post.put("attachments", r);
                    post.put("type", String.valueOf(Constants.APK_LINK_POST));
                    post.put("target", apkId);
                }
            }
            if (!packageName.equals("")) {
                if (qp.existPackage(packageName)) {
                    RecordSet s = thisTrandsGetSingleApkInfo(userId, toStr(packageName), cols, 1000);
                    post.put("target", s.getFirstRecord().getString("apk_id"));
                    post.put("attachments", s);
                    if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                        post.put("type", String.valueOf(type));
                    } else {
                        post.put("type", String.valueOf(Constants.APK_POST));
                    }
                } else {
                    post.put("target", packageName);
                    String href = "http://market.android.com/details?id=" + packageName;
                    RecordSet r = new RecordSet();
                    r.add(Record.of("href", href));
                    post.put("attachments", r);
                    post.put("type", String.valueOf(Constants.APK_LINK_POST));
                }
            }
        }

        /*
        //use appid now,this is temp method for qiupu.attachments must be json after update .
        if (!appId.equals(toStr(Constants.APP_TYPE_QIUPU))) {
            if (type != Constants.LINK_POST) {
                post.put("attachments", (attachments.length() <= 2) ? "[]" : "[" + attachments + "]");
                post.put("type", String.valueOf(type));
            }
        } else {
            String a = "";
            //1,if exist in server
            Transceiver tranq = getTransceiver(QiupuInterface.class);
            QiupuInterface qp = getProxy(QiupuInterface.class, tranq);
            if (apkId.equals("") && packageName.equals("")) {
                if (type != Constants.LINK_POST && type != Constants.PHOTO_POST) {
                    post.put("attachments", "[]");
                    post.put("type", String.valueOf(Constants.TEXT_POST));
                    post.put("target", "");
                }
            } else {
                if (!apkId.equals("")) {
                    String[] ss = StringUtils.split(StringUtils.trimToEmpty(apkId), '-');
                    String package_ = ss[0].trim();

                    if (qp.existPackage(package_)) {
                        //post.put("attachments", RecordSet.fromByteBuffer(qp.getApps(toStr(apkId), cols)));
                        //old
                        RecordSet mcs = thisTrandsGetApkInfo(userId, toStr(apkId), cols, 1000);
                        if (mcs.size() > 0) {
                            post.put("attachments", mcs);
                            post.put("target", apkId);
                        } else {
                            RecordSet s = thisTrandsGetSingleApkInfo(userId, toStr(ss[0]), cols, 1000);
                            post.put("attachments", s);
                            post.put("target", s.getFirstRecord().getString("apk_id"));
                        }
                        if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                            post.put("type", String.valueOf(type));
                        } else {
                            post.put("type", String.valueOf(Constants.APK_POST));
                        }
                    } else {
                        String href = "http://market.android.com/details?id=" + package_;
                        RecordSet r = new RecordSet();
                        r.add(Record.of("href", href));
                        post.put("attachments", r);
                        post.put("type", String.valueOf(Constants.APK_LINK_POST));
                        post.put("target", apkId);
                    }
                } else {
                    if (!packageName.equals("")) {
                        if (qp.existPackage(packageName)) {
                            RecordSet s = thisTrandsGetSingleApkInfo(userId, toStr(packageName), cols, 1000);
                            post.put("target", s.getFirstRecord().getString("apk_id"));
                            post.put("attachments", s);
                            if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                                post.put("type", String.valueOf(type));
                            } else {
                                post.put("type", String.valueOf(Constants.APK_POST));
                            }
                        } else {
                            post.put("target", packageName);
                            String href = "http://market.android.com/details?id=" + packageName;
                            RecordSet r = new RecordSet();
                            r.add(Record.of("href", href));
                            post.put("attachments", r);
                            post.put("type", String.valueOf(Constants.APK_LINK_POST));
                        }
                    }
                }
            }
        }
        post.put("device", device);
        post.put("app_data", appData);
        post.put("mentions", parseUserIds(userId, mentions));
        post.put("privacy", secretly);
        post.put("location", location);
        post.put("can_comment", can_comment);
        post.put("can_like", can_like);
        post.put("can_reshare", can_reshare);
        post.put("add_to", add_to);
        */
        String tempAttach = post.getString("attachments", "[]");
        if (StringUtils.isBlank(tempAttach) || StringUtils.equals(tempAttach, "[]")) {
            post.put("type", type | Constants.TEXT_POST);
        }
        L.debug("long message,record post=" + post.toString(false, false));
        return post(userId, post, emails, phones, appId);
    }


    public String sendShareLink(String userId, String msg, String appId, String mentions, String app_data,
                                boolean secretly, String device, String location, String url, String title, String linkImagAddr, boolean can_comment, boolean can_like, boolean can_reshare, String add_to) throws AvroRemoteException, UnsupportedEncodingException {
        List<String> emails = getEmails(mentions);
        List<String> phones = getPhones(mentions);
        mentions = getOldMentions(userId, mentions);

        List<String> add_contact = new ArrayList<String>();
        add_contact.addAll(emails);
        add_contact.addAll(phones);
        for (int k = add_contact.size() - 1; k >= 0; k--) {
            String virtualFriendId = getUserFriendhasVirtualFriendId(userId, add_contact.get(k));
            if (!StringUtils.equals(virtualFriendId, "0")) {
                add_contact.remove(k);
            }
        }
        String add_contact_s = StringUtils.join(add_contact, ",");
        boolean has_contact = false;
        if (add_contact_s.length() > 0)
            has_contact = true;

        Record nowPost = Record.of("message", msg, "app", appId);
        nowPost.put("type", String.valueOf(Constants.LINK_POST));
        nowPost.put("target", url);
        nowPost.put("device", device);
        nowPost.put("app_data", app_data);
        nowPost.put("mentions", parseUserIds(userId, mentions));
        nowPost.put("privince", secretly);
        nowPost.put("location", location);
        nowPost.put("can_comment", can_comment);
        nowPost.put("can_like", can_like);
        nowPost.put("can_reshare", can_reshare);
        nowPost.put("add_to", add_to);

        nowPost.put("add_contact", add_contact_s);
        nowPost.put("has_contact", has_contact);

        RecordSet t = new RecordSet();
        Record at = Record.of("url", url, "title", title);
        String h = "";
        try {
            URL ur = new URL(url);
            h = ur.getHost();
        } catch (MalformedURLException e) {

        }
        at.put("host", h);
        at.put("description", "");
        at.put("img_url", "");
        at.put("many_img_url", "[]");
        t.add(at);
        nowPost.put("attachments", t);

        Record post = Record.of("userId", userId);
        post.put("url", url);
        post.put("linkImagAddr", linkImagAddr);

        String post_id = post(userId, nowPost, emails, phones, appId);
        post.put("post_id", post_id);

        MQ mq = MQCollection.getMQ("platform");
        if (mq != null)
            mq.send("link", post.toString(false, false));

        return post_id;
    }

    public boolean destroyPosts(String userId, String postIds) throws AvroRemoteException {
        if (Constants.isNullUserId(userId))
            checkUserIds(userId);

        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            List<String> p = StringUtils2.splitList(toStr(postIds), ",", true);
            for (String p0 : p) {
                deleteConversation(Constants.POST_OBJECT, p0, -1, 0);
            }

            return Record.fromByteBuffer(stream.destroyPosts(userId, postIds)).getBoolean("result", false);
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record findStreamTemp(String postId, String cols) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            return Record.fromByteBuffer(stream.findStreamTemp(postId, cols));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getTopOneStreamByTarget(int type, String target) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            return RecordSet.fromByteBuffer(stream.topOneStreamByTarget(type, target));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getSharedPost(String viewerId, String postId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            return RecordSet.fromByteBuffer(stream.getSharedPost(viewerId, postId));
        } finally {
            closeTransceiver(trans);
        }
    }


    public int getSharedCount(String viewerId, String userId, int type) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            return stream.getSharedCount(viewerId, userId, type);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean directDestroyPosts(String postIds) throws AvroRemoteException {
        return destroyPosts(Constants.NULL_USER_ID, postIds);
    }

    public String repost(String userId, String mentions, boolean secretly, String postId, String newMessage, String device, String location, String appData, boolean can_comment, boolean can_like, boolean can_reshare, String add_to) throws AvroRemoteException, UnsupportedEncodingException {
        L.debug("**for long message test,send post,newMessage=" + newMessage);
        List<String> emails = getEmails(mentions);
        List<String> phones = getPhones(mentions);
        mentions = getOldMentions(userId, mentions);
        List<String> add_contact = new ArrayList<String>();
        add_contact.addAll(emails);
        add_contact.addAll(phones);
        for (int k = add_contact.size() - 1; k >= 0; k--) {
            String virtualFriendId = getUserFriendhasVirtualFriendId(userId, add_contact.get(k));
            if (!StringUtils.equals(virtualFriendId, "0")) {
                add_contact.remove(k);
            }
        }
        String add_contact_s = StringUtils.join(add_contact, ",");
        boolean has_contact = false;
        if (add_contact_s.length() > 0)
            has_contact = true;

        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            if (mentions.length() > 0)
                mentions = parseUserIds(userId, mentions);
            String newPostId = toStr(stream.createRepost(userId, mentions, secretly, postId, newMessage != null ? newMessage : "", device, location, appData, can_comment, can_like, can_reshare, add_to, add_contact_s, has_contact));
            createConversation(Constants.POST_OBJECT, postId, Constants.C_STREAM_RESHARE, userId);
            createConversation(Constants.POST_OBJECT, newPostId, Constants.C_STREAM_POST, userId);
            if (add_to.length() > 0)
                createConversation(Constants.POST_OBJECT, newPostId, Constants.C_STREAM_ADDTO, add_to);
            try {
                Record thisUser = getUsers(userId, userId, "display_name", true).getFirstRecord();
                Record old_stream = getPost(postId, "post_id,mentions,source,target,type,message,attachments");
//                Record new_stream = getPost(newPostId, "post_id,source,message");
                String body = newMessage;

                sendNotification(Constants.NTF_MY_STREAM_RETWEET,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(newPostId, userId, thisUser.getString("display_name"), postId, old_stream.getString("source"), old_stream.getString("message")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(newPostId),
                        createArrayNodeFromStrings(newPostId, userId, thisUser.getString("display_name"), postId, old_stream.getString("source"), old_stream.getString("message")),
                        createArrayNodeFromStrings(newMessage),
                        createArrayNodeFromStrings(newMessage),
                        createArrayNodeFromStrings(postId),
                        createArrayNodeFromStrings(postId, userId, newPostId)
                );

                String displayName = thisUser.getString("display_name");
//                NotificationSender notif2 = new SharedNotifSender(this, null);
                int type = (int) old_stream.getInt("type");

                sendNotification(Constants.NTF_MY_APP_COMMENT,
                        createArrayNodeFromStrings(String.valueOf(type)),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(String.valueOf(type), displayName),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(newPostId),
                        createArrayNodeFromStrings(String.valueOf(type), userId, displayName),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(newPostId)
                );

                List<String> l = StringUtils2.splitList(mentions, ",", true);
                for (String l0 : l) {
                    long id = Long.parseLong(l0);
                    if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                            && (id <= Constants.GROUP_ID_END))
                        sendGroupNotification(id, new SharedNotifSender(this, null), userId, new Object[]{newPostId}, body, String.valueOf(type), displayName);
                }

                //not borqs account
                String body2 = old_stream.getString("message", "");
                if (type == Constants.APK_POST) {
                    Record mcs = thisTrandsGetApkInfo(userId, old_stream.getString("target"), "app_name", 1000).getFirstRecord();
                    body2 = mcs.getString("app_name");
                } else if ((type == Constants.APK_LINK_POST)) {
                    String attachments = old_stream.getString("attachments");
                    ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                    if ((aNode != null) && (aNode.get(0) != null))
                        body2 = aNode.get(0).get("href").getTextValue();
                } else if (type == Constants.BOOK_POST) {
                    String attachments = old_stream.getString("attachments");
                    if (attachments.length() >= 2) {
                        ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                        if ((aNode != null) && (aNode.get(0) != null))
                            body2 = aNode.get(0).get("summary").getTextValue();
                    }
                } else if (type == Constants.PHOTO_POST) {
                    String attachments = old_stream.getString("attachments");
                    RecordSet r0 = RecordSet.fromJson(attachments);
                    if (!r0.getFirstRecord().isEmpty()) {
                        body2 = r0.getFirstRecord().getString("photo_caption");
                    }
                } else if (type == Constants.LINK_POST) {
                    String attachments = old_stream.getString("attachments");
                    if (attachments.length() > 2) {
                        ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                        if ((aNode != null) && (aNode.get(0) != null))
                            body2 = aNode.get(0).get("url").getTextValue();
                    }
                }

                //not borqs account
                String emailContent = composeShareContent(userId, type, body2, true, device);
                String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
                String template = Constants.getBundleString(device, "platform.compose.share.title");
                String title = SQLTemplate.merge(template, new Object[][]{
                        {"displayName", displayName}
                });
                for (String email : emails) {
                    String uid = findUserIdByUserName(email);
                    if (StringUtils.equals(uid, "0") || StringUtils.isBlank(uid)) {
                        Record setting = getNUserPreferencesByUsers(Constants.EMAIL_SHARE_TO, email);
                        String value = setting.getString(email, "0");
                        if (value.equals("0")) {
                            sendEmail(title, email, email, emailContent, Constants.EMAIL_SHARE_TO, lang);
                        }
                    } else if (sendEmail) {
                        Record setting = getPreferencesByUsers(Constants.EMAIL_SHARE_TO, uid);
                        String value = setting.getString(uid, "0");
                        if (value.equals("0")) {
                            sendEmail(title, email, email, emailContent, Constants.EMAIL_SHARE_TO, lang);
                        }
                    }
                }

                String smsContent = composeShareContent(userId, type, body2, false, device);
                for (String phone : phones) {
                    String uid = findUserIdByUserName(phone);
                    if (StringUtils.equals(uid, "0") || StringUtils.isBlank(uid)) {
                        sendSms(phone, smsContent + "\\");
                        template = Constants.getBundleString(device, "platform.compose.share.download");
                        String download = SQLTemplate.merge(template, new Object[][]{
                                {"serverHost", SERVER_HOST}
                        });
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {

                        }
                        sendSms(phone, download + "\\");
                    } else if (sendSms) {
                        sendSms(phone, smsContent + "\\");
                    }
                }
            } finally {
            }
            return newPostId;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updatePost(String userId, String postId, String message) throws AvroRemoteException {
        Validate.notNull(userId);
        Validate.isTrue(!Constants.isNullPostId(postId));
        checkUserIds(userId);

        Record rec = new Record();
        rec.putIf("message", message, message != null);

        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            return stream.updatePost(userId, postId, rec.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }


    public boolean hasPost(String postId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            return stream.hasPost(postId);
        } finally {
            closeTransceiver(trans);
        }
    }

    private RecordsExtenders createStreamExtenders(String viewerId) {
        RecordsExtenders res = new RecordsExtenders();
        res.add(PlatformExtender.setViewerId(new BuiltinStreamExtender(this), viewerId));
        res.addExtendersInConfig(getConfig(), "platform.streamExtenders");
        PlatformExtender.configPlatformExtenders(res, this, viewerId);
        return res;
    }

    public RecordSet getPosts(String postIds, String cols) throws AvroRemoteException {
        cols = parsePostColumns(cols);
        RecordSet recs;
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            recs = RecordSet.fromByteBuffer(stream.getPosts(postIds, cols));
        } finally {
            closeTransceiver(trans);
        }

        // TODO: add extended columns for post

        cols = parsePostColumns(cols);
        final String postIds0 = postIds;

        return recs;
    }

    public Record getPost(String postId, String cols) throws AvroRemoteException {
        return getPosts(firstId(postId), cols).getFirstRecord();
    }

    public RecordSet getHotStream(String viewerId,String circle_ids, String cols,int type,long max,long min, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        Transceiver transf = getTransceiver(Friendship.class);
        Transceiver transl = getTransceiver(Like.class);
        try {
             List<String> circle_id_list = StringUtils2.splitList(circle_ids,",",true);
            String circle_id0 = "";
            if (circle_id_list.size() > 0)
                circle_id0 = circle_id_list.get(0);
            Comment c = getProxy(Comment.class, trans);
            Like l = getProxy(Like.class, transl);
            Friendship fs = getProxy(Friendship.class, transf);
            String postIds="";
            RecordSet recs0 = new RecordSet();
            if (circle_id0.equals("")) {
                recs0 = getFriendsTimeline(viewerId, String.valueOf(FRIENDS_CIRCLE), "post_id,created_time", min, max, ALL_POST, "1", 0, 500);
            } else {
                if (circle_id0.length() > 5) {
                    recs0 = getUsersTimeline(viewerId, circle_id0, "post_id,created_time", min, max, type, "1",  0, 500);
                } else {
                    //local circle
                    if (circle_id0.startsWith("#"))
                        circle_id0 = circle_id0.replace("#","");
                    RecordSet recs01 = RecordSet.fromByteBuffer(fs.getCircles(viewerId, circle_id0, true));
                    String users = recs01.getFirstRecord().getString("members");
                    if (users.length() > 0)
                        recs0 = getUsersTimeline(viewerId, users, "post_id,created_time", min, max, type, "1", 0, 500);
                }
            }

            L.debug("==hot stream 0==:"+ recs0.toString());
            for (Record record : recs0) {
                //get comment count
                int c_count = c.getCommentCount(viewerId, String.valueOf(Constants.POST_OBJECT) + ":" + record.getString("post_id"));
                //get like count
                int l_count = l.getLikeCount(String.valueOf(Constants.POST_OBJECT) + ":" + record.getString("post_id"));

                //get time
                DecimalFormat df2  = new DecimalFormat("###.0");
                long ori_time = record.getInt("created_time");
                long now_time = DateUtils.nowMillis();
                long date_diff = now_time - ori_time;
                long req_diff = max - min;
                long m = (req_diff - date_diff) * 100 / req_diff;
                int all_count = (int)(m * 10) + c_count * 40 + l_count * 10;
                record.put("all_count",all_count);
            }
            L.debug("==hot stream 1==:"+ recs0.toString());
            recs0.sort("all_count",false);
            recs0.sliceByPage(page,count);
            L.debug("==hot stream 2==:"+ recs0.toString());
            postIds = recs0.joinColumnValues("post_id",",");
            RecordSet recs_stream = getPosts(postIds, cols.equals("") ? POST_FULL_COLUMNS : cols);
            return transTimelineForQiupu(viewerId, recs_stream, 2, 5,false);
        } finally {
            closeTransceiver(trans);
            closeTransceiver(transf);
            closeTransceiver(transl);
        }
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public static double GetDistance(double lng1, double lat1, double lng2, double lat2) {
        double EARTH_RADIUS = 6378137;
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000;
        return s;
    }

    public RecordSet getNearByStream(String viewerId, String cols, long since, long max, int type, String appId, int page, int count, String location, int dis) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream s = getProxy(Stream.class, trans);
            cols = cols.length() > 0 ? cols : POST_FULL_COLUMNS;
            cols = !cols.contains("longitude") ? cols + ",longitude" : cols;
            cols = !cols.contains("latitude") ? cols + ",latitude" : cols;


            double longitude_me = Double.parseDouble(Constants.parseLocation(location, "longitude"));
            double latitude_me = Double.parseDouble(Constants.parseLocation(location, "latitude"));

            RecordSet cList = RecordSet.fromByteBuffer(s.getPostsNearBy(viewerId, cols, since, max, type, appId, 0, 200));
            for (Record r : cList) {
                double longitude = Double.parseDouble(r.getString("longitude"));
                double latitude = Double.parseDouble(r.getString("latitude"));
                double distance = GetDistance(longitude_me, latitude_me, longitude, latitude);
                r.put("distance", distance);
            }

            for (int i = cList.size() - 1; i >= 0; i--) {
                double distance = Double.parseDouble(cList.get(i).getString("distance"));
                if (distance > Double.parseDouble(String.valueOf(dis)))
                    cList.remove(i);
            }

            if (cList.size() > 0) {
                cList.sliceByPage(page, count);
            }
            return transTimelineForQiupu(viewerId, cList, 2, 5,false);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean createIgnore(String userId, String target_type, String targetIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Ignore.class);
        try {
            Ignore ignore = getProxy(Ignore.class, trans);
            List<String>  targetIds0 = StringUtils2.splitList(targetIds, ",", true);
            for (String target_id : targetIds0) {
                ignore.createIgnore(userId, target_type, target_id);
            }
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean deleteIgnore(String userId, String target_type, String targetIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Ignore.class);
        try {
            Ignore ignore = getProxy(Ignore.class, trans);
            List<String> targetIds0 = StringUtils2.splitList(targetIds, ",", true);
            for (String target_id : targetIds0) {
                ignore.deleteIgnore(userId, target_type, target_id);
            }
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getIgnoreList(String userId, String target_type, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Ignore.class);
        Transceiver tranc = getTransceiver(Comment.class);
        try {
            Ignore ignore = getProxy(Ignore.class, trans);
            Comment comment = getProxy(Comment.class, tranc);
            RecordSet recs = RecordSet.fromByteBuffer(ignore.getIgnoreList(userId, target_type, page, count));
            RecordSet out_recs = new RecordSet();
            if (recs.size() > 0) {
                if (target_type.equals(String.valueOf(Constants.IGNORE_STREAM))) {
                    RecordSet rs = getPosts(recs.joinColumnValues("target_id", ","), POST_FULL_COLUMNS);
                    out_recs = transTimelineForQiupu(userId, rs, 2, 5,false);
                }

                if (target_type.equals(String.valueOf(Constants.IGNORE_COMMENT))) {
                    RecordSet rs = RecordSet.fromByteBuffer(comment.getComments(recs.joinColumnValues("target_id", ","), FULL_COMMENT_COLUMNS));
                    out_recs = transComment(userId, rs);
                }

                if (target_type.equals(String.valueOf(Constants.IGNORE_USER))) {
                    RecordSet rs = getUsers(userId, recs.joinColumnValues("target_id", ","), USER_FULL_COLUMNS);
                    out_recs = rs;
                }
            }
            return out_recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public List<Long> formatIgnoreUserList(String viewerId,List<Long> userList,String post_id,String comment_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Ignore.class);
        Transceiver transw = getTransceiver(Stream.class);
        Transceiver tranc = getTransceiver(Comment.class);
        try {
            Ignore ignore = getProxy(Ignore.class, trans);
            Stream stream = getProxy(Stream.class, transw);
            Comment comment = getProxy(Comment.class, tranc);

            //先看这个人有没有ignore我
            for (int i = userList.size() - 1; i >= 0; i--) {
                if (ignore.getExistsIgnore(String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_USER), viewerId))
                    userList.remove(i);
            }

            if (!post_id.equals("")) {
                //看看那个人有没有ignore这个stream ,如果有，把那个人干掉
                //看看发stream的是谁
//                String source_user = RecordSet.fromByteBuffer(stream.getPosts(post_id, "source")).getFirstRecord().getString("source");
                for (int i = userList.size() - 1; i >= 0; i--) {
                    if (ignore.getExistsIgnore(String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_STREAM), post_id))
                        userList.remove(i);
                }
//                if (!source_user.equals("")) {
//                    for (int i = userList.size() - 1; i >= 0; i--) {
//                        if (ignore.getExistsIgnore(String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_STREAM), post_id))
//                            userList.remove(i);
//                    }
//                }
            }

            if (!comment_id.equals("")) {
                //看看发comment的是谁
//                String commenter_user = RecordSet.fromByteBuffer(comment.getComments(comment_id, "commenter")).getFirstRecord().getString("commenter");
                for (int i = userList.size() - 1; i >= 0; i--) {
                    if (ignore.getExistsIgnore(String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_COMMENT), comment_id))
                        userList.remove(i);
                }
//                if (!comment_id.equals("")) {
//                    for (int i = userList.size() - 1; i >= 0; i--) {
//                        if (ignore.getExistsIgnore(String.valueOf(userList.get(i)), String.valueOf(Constants.IGNORE_COMMENT), comment_id))
//                            userList.remove(i);
//                    }
//                }
            }


            return userList;
        } finally {
            closeTransceiver(trans);
            closeTransceiver(transw);
            closeTransceiver(tranc);
        }
    }



    public RecordSet getIgnoreListSimple(String userId, String target_type, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Ignore.class);
        Transceiver tranc = getTransceiver(Comment.class);
        try {
            Ignore ignore = getProxy(Ignore.class, trans);
            Comment comment = getProxy(Comment.class, tranc);
            RecordSet recs = RecordSet.fromByteBuffer(ignore.getIgnoreList(userId, target_type, page, count));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getPostsForQiuPu(String viewerId, String postsIds, String cols,boolean single_get) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getPosts(postsIds, cols), 5, 5,single_get);
    }

    public RecordSet getFullPostsForQiuPu(String viewerId, String postIds,boolean single_get) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getPosts(postIds, POST_FULL_COLUMNS), 5, 5,single_get);
    }

    public RecordSet getPublicTimeline(String viewerId, String cols, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return getUsersTimeline(viewerId, "", cols, since, max, type, appId, page, count);
    }

    public RecordSet getFullPublicTimeline(String viewerId, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return getPublicTimeline(viewerId, POST_FULL_COLUMNS, since, max, type, appId, page, count);
    }

    public RecordSet getPublicTimelineForQiuPu(String viewerId, String cols, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getPublicTimeline(viewerId, cols, since, max, type, appId, page, count), 2, 5,false);
    }

    public RecordSet getFullPublicTimelineForQiuPu(String viewerId, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getPublicTimeline(viewerId, POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5,false);
    }


    public RecordSet getUsersTimeline(String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            userIds = formatIgnoreUsers(viewerId, userIds);
            RecordSet recs = RecordSet.fromByteBuffer(stream.getUsersPosts(viewerId, userIds, "", cols, since, max, type, appId, page, count));
            recs = formatIgnoreStreamOrComments(viewerId, "stream", recs);
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean getPhoto(String viewerId,String photo_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            boolean rec = stream.getPhoto(viewerId,photo_id);
            return rec;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean getFile(String viewerId,String file_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            boolean rec = stream.getFile(viewerId, file_id);
            return rec;
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getVideo(String viewerId, String file_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            Record rec = Record.fromByteBuffer(stream.getVideo(viewerId, file_id));
            return rec;
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getAudio(String viewerId, String file_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            Record rec = Record.fromByteBuffer(stream.getAudio(viewerId, file_id));
            return rec;
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getStaticFile(String viewerId, String file_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            Record rec = Record.fromByteBuffer(stream.getStaticFile(viewerId, file_id));
            return rec;
        } finally {
            closeTransceiver(trans);
        }
    }

    public String formatIgnoreUsers(String viewerId, String userIds) throws AvroRemoteException {
        List<String> user_ignore = new ArrayList<String>();
        List<String> user_old = StringUtils2.splitList(userIds, ",", true);
        if (!viewerId.equals("") && !viewerId.equals("0")) {
            RecordSet recs_ignore = getIgnoreListSimple(viewerId, String.valueOf(Constants.IGNORE_USER), 0, 1000);
            if (recs_ignore.size() > 0) {
                for (Record r : recs_ignore) {
                    user_ignore.add(r.getString("target_id"));
                }
            }
        }
        for (int i = user_old.size() - 1; i >= 0; i--) {
            if (user_ignore.contains(user_old.get(i)))
                user_old.remove(i);
        }
        return StringUtils.join(user_old, ",");
    }

    public RecordSet formatIgnoreStreamOrComments(String viewerId, String sORc, RecordSet recs) throws AvroRemoteException {
        if (!viewerId.equals("") && !viewerId.equals("0")) {
            List<String> stream_ignore = new ArrayList<String>();
            List<String> user_ignore_list = new ArrayList<String>();
            String target_type = sORc.equals("stream") ? String.valueOf(Constants.IGNORE_STREAM) : String.valueOf(Constants.IGNORE_COMMENT);
            String column_name = sORc.equals("stream") ? "post_id" : "comment_id";
            String source_name = sORc.equals("stream") ? "source" : "commenter";
            RecordSet recs_ignore = getIgnoreListSimple(viewerId, target_type, 0, 1000);
            RecordSet recs_user_ignore = getIgnoreListSimple(viewerId, String.valueOf(Constants.IGNORE_USER), 0, 1000);
            if (recs_ignore.size() > 0) {
                for (Record r : recs_ignore) {
                    stream_ignore.add(r.getString("target_id"));
                }
            }

            if (recs_user_ignore.size() > 0) {
                for (Record r : recs_user_ignore) {
                    user_ignore_list.add(r.getString("target_id"));
                }
            }

            for (int i = recs.size() - 1; i >= 0; i--) {
                if (stream_ignore.contains(recs.get(i).getString(column_name)) || user_ignore_list.contains(recs.get(i).getString(source_name))) {
                    recs.remove(i);
                }
            }
        }
        return recs;
    }


    public RecordSet getMyShare(String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        List<String> users = StringUtils2.splitList(userIds, ",", true);
        List<String> groups = getGroupIdsFromMentions(users);
        users.removeAll(groups);

        if (CollectionUtils.isNotEmpty(users) && !hasAllUsers(StringUtils2.joinIgnoreBlank(",", users))){
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "Users is not exists");
        }
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            userIds = formatIgnoreUsers(viewerId, userIds);
            RecordSet recs = RecordSet.fromByteBuffer(stream.getMySharePosts(viewerId, userIds, cols, since, max, type, appId, page, count));
            recs = formatIgnoreStreamOrComments(viewerId, "stream", recs);
            L.debug("===###===recs=" + recs.toString(false, false));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet findWhoSharedApp(String packagename, int limit) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            return RecordSet.fromByteBuffer(stream.findWhoSharedApp(packagename, limit));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet findWhoRetweetStream(String target, int limit) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            return RecordSet.fromByteBuffer(stream.findWhoRetweetStream(target, limit));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet findWhoCommentTarget(String target, int limit) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            Comment comment = getProxy(Comment.class, trans);
            return RecordSet.fromByteBuffer(comment.findWhoCommentTarget(target, limit));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getFullUsersTimeline(String viewerId, String userIds, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getUsersTimeline(viewerId, userIds, POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5,false);
    }

    public RecordSet getUsersTimelineForQiuPu(String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getUsersTimeline(viewerId, userIds, cols, since, max, type, appId, page, count), 2, 5,false);
    }

    public RecordSet getMyShareFullTimeline(String viewerId, String userIds, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getMyShare(viewerId, userIds, POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5,false);
    }

    public RecordSet getMyShareTimeline(String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getMyShare(viewerId, userIds, cols, since, max, type, appId, page, count), 2, 5,false);
    }

    public RecordSet getFullUsersTimelineForQiuPu(String viewerId, String userIds, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getUsersTimeline(viewerId, userIds, POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5,false);
    }

    public RecordSet getFriendsTimeline(String userId, String circleIds, String cols, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
//        if (circleIds.equals(""))
//            circleIds = Integer.toString(Constants.FRIENDS_CIRCLE);

        List<String> l = StringUtils2.splitList(circleIds, ",", true);
        List<String> groupsFromCircleIds = getGroupIdsFromMentions(l);
        l.removeAll(groupsFromCircleIds);

        List<String> friendIds = new ArrayList<String>();
        if (!l.isEmpty()) {
            RecordSet recs = getFriends(userId, userId, StringUtils2.joinIgnoreBlank(",", l), "user_id", 0, 1000);
            friendIds = recs.getStringColumnValues("user_id");
            if (circleIds.equals(Integer.toString(Constants.FRIENDS_CIRCLE))) {
                friendIds.add(userId); // Add me
            }
        }
        friendIds.addAll(groupsFromCircleIds);

        Transceiver trans0 = getTransceiver(Group.class);
        Transceiver trans = getTransceiver(Stream.class);
        try {
            //add user's groups
            if (circleIds.equals(Integer.toString(Constants.FRIENDS_CIRCLE))) {
                Group group = getProxy(Group.class, trans0);
                String groupIds = toStr(group.findGroupIdsByMember(PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, Long.parseLong(userId)));
                friendIds.addAll(StringUtils2.splitList(groupIds, ",", true));
                String eventIds = toStr(group.findGroupIdsByMember(EVENT_ID_BEGIN, EVENT_ID_END, Long.parseLong(userId)));
                friendIds.addAll(StringUtils2.splitList(eventIds, ",", true));
            }

            Stream stream = getProxy(Stream.class, trans);
            String frendIds = StringUtils.join(friendIds, ",");
            frendIds = formatIgnoreUsers(userId, frendIds);
            RecordSet recs_out = RecordSet.fromByteBuffer(stream.getUsersPosts(userId, frendIds, circleIds, cols, since, max, type, appId, page, count));
            recs_out = formatIgnoreStreamOrComments(userId, "stream", recs_out);
            return recs_out;
        } finally {
            closeTransceiver(trans0);
            closeTransceiver(trans);
        }
    }

    public RecordSet getFullFriendsTimeline(String userId, String circleIds, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return getFriendsTimeline(userId, circleIds, POST_FULL_COLUMNS, since, max, type, appId, page, count);
    }

    public RecordSet getFriendsTimelineForQiuPu(String viewerId, String userId, String circleIds, String cols, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getFriendsTimeline(userId, circleIds, cols, since, max, type, appId, page, count), 2, 5,false);
    }

    public RecordSet getFullFriendsTimelineForQiuPu(String viewerId, String userId, String circleIds, long since, long max, int type, String appId, int page, int count) throws AvroRemoteException {
        return transTimelineForQiupu(viewerId, getFriendsTimeline(userId, circleIds, POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5,false);
    }

    public boolean reportAbuserCreate(String viewerId, String post_id,String ua,String loc) throws AvroRemoteException {
        Transceiver trans = getTransceiver(ReportAbuse.class);
        try {
            ReportAbuse ra = getProxy(ReportAbuse.class, trans);
            Record rec = new Record();
            rec.put("post_id", post_id);
            rec.put("user_id", viewerId);
            rec.put("created_time", DateUtils.nowMillis());
            boolean b = ra.saveReportAbuse(rec.toByteBuffer());
            if (b)
            {
                String report = Constants.getBundleString(ua, "platform.sendmail.stream.report.abuse");
                Record this_stream = getPost(post_id, "post_id,message");
                sendNotification(Constants.NTF_REPORT_ABUSE,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(viewerId),
                        createArrayNodeFromStrings(post_id, viewerId, this_stream.getString("message")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(post_id),
                        createArrayNodeFromStrings(post_id, viewerId, this_stream.getString("message")),
                        createArrayNodeFromStrings(report),
                        createArrayNodeFromStrings(report),
                        createArrayNodeFromStrings(post_id),
                        createArrayNodeFromStrings(post_id, viewerId)
                );
            }
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public int getReportAbuserCount(String post_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(ReportAbuse.class);
        try {
            ReportAbuse ra = getProxy(ReportAbuse.class, trans);
            int count = ra.getReportAbuseCount(toStr(post_id));
            return count;
        } finally {
            closeTransceiver(trans);
        }
    }

    public int iHaveReport(String viewerId,String post_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(ReportAbuse.class);
        try {
            ReportAbuse ra = getProxy(ReportAbuse.class, trans);
            int count = ra.iHaveReport(toStr(viewerId),toStr(post_id));
            return count;
        } finally {
            closeTransceiver(trans);
        }
    }

    public int likeGetCount(String objectTargetId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like l = getProxy(Like.class, trans);
            int count = l.getLikeCount(objectTargetId);
            return count;
        } finally {
            closeTransceiver(trans);
        }
    }

    public String canDeletePost(String viewerId, String source, List<String> groupIds) {
        if (StringUtils.equals(viewerId, source))
            return "-1";

        try {
            ArrayList<String> l = new ArrayList<String>();
            for (String groupId : groupIds) {
                if (hasGroupRight(Long.parseLong(groupId), viewerId, ROLE_ADMIN))
                    l.add(groupId);
            }
            return StringUtils2.joinIgnoreBlank(",", l);
        } catch (Exception e) {
            return "";
        }
    }
    
    public RecordSet transTimelineForQiupu(String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers,boolean single_get) throws AvroRemoteException {
        if (reds.size() > 0) {
            Transceiver liketrans = getTransceiver(Like.class);
            Like like = getProxy(Like.class, liketrans);

            Transceiver commenttrans = getTransceiver(Comment.class);
            Comment comment = getProxy(Comment.class, commenttrans);

            if (reds.size() > 0) {
                for (int i = 0; i < reds.size(); i++) {
                    Record rec = reds.get(i);
                    String this_targetID = Constants.POST_OBJECT + ":" + rec.getInt("post_id");
                    //    private JsonNode from;1
                    //    private JsonNode to;2
                    //    private JsonNode likes;3
                    //    private JsonNode comments;4
                    //    private JsonNode custom;5
                    //    private JsonNode retweeted_stream;6

                    rec.put("from", getUsers(rec.getString("source"), rec.getString("source"), USER_LIGHT_COLUMNS_LIGHT).getFirstRecord());//1

                    String t_mentions = rec.getString("mentions");
                    List<String> l_t_mentions = StringUtils2.splitList(t_mentions, ",", true);

                    List<String> groupList = getGroupIdsFromMentions(l_t_mentions);
                    String groupIds = StringUtils2.joinIgnoreBlank(",", groupList);
                    l_t_mentions.removeAll(groupList);

                    if (l_t_mentions.size() > 0) {
                        if (!viewerId.equals(rec.getString("source"))) {
                            for (int jj = l_t_mentions.size() - 1; jj >= 0; jj--) {
                                if (l_t_mentions.get(jj).toString().length() > 10)
                                    l_t_mentions.remove(jj);
                            }
                        }
                    }
                    String nowMentions = StringUtils.join(l_t_mentions, ",");
                    String users = parseAllUsers(nowMentions);
                    String mentions = users;
                    if (StringUtils.isNotBlank(groupIds)) {
                        if (StringUtils.isBlank(users))
                            mentions = groupIds;
                        else
                            mentions = users + "," + groupIds;
                    }

                    rec.put("mentions", mentions);
                    RecordSet userto = getUsers(rec.getString("source"), users, USER_LIGHT_COLUMNS_LIGHT);
                    if (viewerId.equals(rec.getString("source"))) {
                        if (rec.getString("add_contact").length() > 0 && rec.getBoolean("has_contact", false)) {
                            List<String> l_add_contact = StringUtils2.splitList(rec.getString("add_contact"), ",", true);
                            for (String a : l_add_contact) {
                                Record r = new Record();
                                r.put("user_id", 0);
                                r.put("display_name", a);
                                r.put("in_circles", new RecordSet());
                                r.put("friends_count", 0);
                                r.put("followers_count", 0);
                                userto.add(r);
                            }
                        }
                    }
                    /*else{
                        for (int kk = userto.size()-1;kk>=0;kk--){
                             if (userto.get(kk).getString("user_id").length()>10)
                                 userto.remove(kk);
                        }
                    }
                    */
                    if (StringUtils.isNotBlank(groupIds)) {
                        Transceiver trans = getTransceiver(Group.class);
                        try {
                            Group group = getProxy(Group.class, trans);
                            RecordSet groups = RecordSet.fromByteBuffer(group.getGroups(0, 0, groupIds, GROUP_LIGHT_COLS));
                            for (Record group_ : groups) {
                                Record r = new Record();
                                r.put("user_id", group_.getInt(Constants.GRP_COL_ID));
                                r.put("display_name", group_.getString(Constants.GRP_COL_NAME));
                                r.put("perhaps_name", JsonNodeFactory.instance.arrayNode());

                                String urlPattern = getConfig().getString("platform.profileImagePattern", "");
                                if (!group_.has(COMM_COL_IMAGE_URL)) {
                                    group_.put(COMM_COL_IMAGE_URL, "default_public_circle.png");
                                    urlPattern = getConfig().getString("platform.sysIconUrlPattern", "");
                                }
                                addImageUrlPrefix(urlPattern, group_);

                                r.put("image_url", group_.getString(COMM_COL_IMAGE_URL));
                                r.put("profile_privacy", false);
                                r.put("pedding_requests", JsonNodeFactory.instance.arrayNode());
                                userto.add(r);
                            }
                        } finally {
                            closeTransceiver(trans);
                        }
                    }

                    rec.put("to", transUserAddressForQiupu(userto));//2
                    rec.put("secretly", rec.getString("privince"));
                    
                    rec.put("can_delete", canDeletePost(viewerId, rec.getString("source"), groupList));
                    rec.put("top_in_targets", findGroupIdsByTopPost(rec.getString("post_id")));

                    if (rec.getInt("type") == Constants.APK_COMMENT_POST || rec.getInt("type") == Constants.APK_LIKE_POST || rec.getInt("type") == Constants.APK_POST) {
                        //stream from comment and like ,so get comments and likes in stream's comments and like
                        String attach = rec.getString("attachments");
                        if (attach.length() > 20) {
                            Record a_rec = RecordSet.fromJson(attach).getFirstRecord();

                            Record rec_apk_like = new Record();
                            int apk_like_count = (int) a_rec.getInt("app_like_count");
                            rec_apk_like.put("count", apk_like_count);

                            if (apk_like_count > 0) {
                                rec_apk_like.put("users", RecordSet.fromJson(a_rec.toJsonNode().findValue("app_liked_users").toString()));
                                rec.put("likes", rec_apk_like);//3
                            } else {
                                rec.put("likes", new Record());//3
                            }
                            a_rec.remove("app_like_count");
                            a_rec.remove("app_liked_users");


                            Record rec_apk_comment = new Record();
                            int apk_comment_count = (int) a_rec.getInt("app_comment_count");
                            apk_comment_count = comment.getCommentCount(viewerId, String.valueOf(Constants.APK_OBJECT) + ":" + rec.getString("target"));
                            rec_apk_comment.put("count", apk_comment_count);
                            if (apk_comment_count > 0) {
                                RecordSet newComments = getCommentsForContainsIgnore(viewerId, Constants.APK_OBJECT, rec.getString("target"), FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                                rec_apk_comment.put("latest_comments", newComments);
//                                rec_apk_comment.put("latest_comments", RecordSet.fromJson(a_rec.toJsonNode().findValue("app_comments").toString()));
                                rec.put("comments", rec_apk_comment);//4
                            } else {
                                rec.put("comments", new Record());//4
                            }

                            rec.put("iliked", viewerId.equals("") ? false : like.ifUserLiked(viewerId, Constants.APK_OBJECT + ":" + a_rec.getString("apk_id")));//4

                            a_rec.remove("app_comment_count");
                            a_rec.remove("app_comments");
                            a_rec.remove("app_likes");

                            RecordSet t = new RecordSet();
                            t.add(a_rec);
                            rec.put("attachments", t);
                            rec.put("root_id", a_rec.getString("apk_id"));
                        }
                    } else if (rec.getInt("type") == Constants.VIDEO_POST || rec.getInt("type") == Constants.AUDIO_POST || rec.getInt("type") == Constants.FILE_POST) {
                          String attach0 = rec.getString("attachments");
                        if (attach0.length() > 20) {
                            RecordSet tmp_attachments = RecordSet.fromJson(attach0);
                            for (Record p_rec : tmp_attachments) {
                                String file_id = p_rec.getString("file_id");
                                boolean file_tmp = getFile(viewerId, String.valueOf(file_id));
                                if (!file_tmp) {
                                    p_rec.put("folder_id", "");
                                    p_rec.put("file_id", file_id);
                                    p_rec.put("title", "");
                                    p_rec.put("summary", "");
                                    p_rec.put("description", "");
                                    p_rec.put("file_size", 0);
                                    p_rec.put("html_url","http://storage.aliyun.com/wutong-data/media/photo/ERROR_O.jpg");
                                    p_rec.put("user_id", 0);
                                    p_rec.put("exp_name", "");
                                    p_rec.put("content_type", "");
                                    p_rec.put("new_file_name","");
                                    p_rec.put("file_url","");
                                    p_rec.put("thumbnail_url","");
                                    p_rec.put("created_time",0);
                                    p_rec.put("updated_time",0);
                                    p_rec.put("destroyed_time",0);
                                } else {
                                    Record Rec_file_like = new Record();
                                    String objectFileId = String.valueOf(Constants.FILE_OBJECT) + ":" + String.valueOf(file_id);
                                    int file_like_count = like.getLikeCount(objectFileId);
                                    Rec_file_like.put("count", file_like_count);
                                    if (file_like_count > 0) {
                                        RecordSet recs_liked_users = RecordSet.fromByteBuffer(like.loadLikedUsers(objectFileId, 0, getLikeUsers));
                                        List<Long> list_file_liked_users = recs_liked_users.getIntColumnValues("liker");
                                        String likeuids = StringUtils.join(list_file_liked_users, ",");
                                        RecordSet recs_user_liked = getUsers(rec.getString("source"), likeuids, USER_LIGHT_COLUMNS_LIGHT);
                                        Rec_file_like.put("users", transUserAddressForQiupu(recs_user_liked));
                                    } else {
                                        Rec_file_like.put("users", new Record());//3
                                    }

                                    Rec_file_like.put("iliked", viewerId.equals("") ? false : like.ifUserLiked(viewerId, objectFileId));
                                    p_rec.put("likes", Rec_file_like);

                                    Record Rec_comment = new Record();
                                    int comment_count = comment.getCommentCount(viewerId, objectFileId);
                                    Rec_comment.put("count", comment_count);
                                    if (comment_count > 0) {
                                        RecordSet recs_com = getCommentsForContainsIgnore(viewerId, Constants.PHOTO_OBJECT, file_id, FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                                        Rec_comment.put("latest_comments", recs_com);
                                    } else {
                                        Rec_comment.put("latest_comments", new Record());
                                    }
                                    p_rec.put("comments", Rec_comment);
                                }
                            }
                            rec.put("attachments",tmp_attachments);
                        }
                    } else if (rec.getInt("type") == Constants.PHOTO_POST) {
                        String attach0 = rec.getString("attachments");
                        if (attach0.length() > 20) {
                            RecordSet tmp_attachments = RecordSet.fromJson(attach0);
                            for (Record p_rec : tmp_attachments) {
                                p_rec.put("album_photo_count",0);
                                p_rec.put("album_cover_photo_id",0);
                                String photo_id = p_rec.getString("photo_id");

                                boolean photo_tmp = getPhoto(viewerId,String.valueOf(photo_id));
                                if (!photo_tmp) {
                                    p_rec.put("album_id", "");
                                    p_rec.put("album_name", "");
                                    p_rec.put("photo_id", photo_id);
                                    p_rec.put("album_photo_count", 0);
                                    p_rec.put("album_cover_photo_id", 0);
                                    p_rec.put("album_description", "");
                                    p_rec.put("album_visible", false);
                                    p_rec.put("photo_img_middle","http://storage.aliyun.com/wutong-data/media/photo/ERROR_O.jpg");
                                    p_rec.put("photo_img_original","http://storage.aliyun.com/wutong-data/media/photo/ERROR_O.jpg");
                                    p_rec.put("photo_img_big","http://storage.aliyun.com/wutong-data/media/photo/ERROR_L.jpg");
                                    p_rec.put("photo_img_small","http://storage.aliyun.com/wutong-data/media/photo/ERROR_S.jpg");
                                    p_rec.put("photo_img_thumbnail","http://storage.aliyun.com/wutong-data/media/photo/ERROR_T.jpg");
                                    p_rec.put("photo_caption", "");
                                    p_rec.put("photo_location", "");
                                    p_rec.put("photo_tag", "");
                                    p_rec.put("photo_created_time",0);
                                    p_rec.put("longitude","");
                                    p_rec.put("latitude", "");
                                    p_rec.put("orientation", "");
                                } else {
                                    Record Rec_photo_like = new Record();
                                    String objectPhotoId = String.valueOf(Constants.PHOTO_OBJECT) + ":" + String.valueOf(photo_id);
                                    int photo_like_count = like.getLikeCount(objectPhotoId);
                                    Rec_photo_like.put("count", photo_like_count);
                                    if (photo_like_count > 0) {
                                        RecordSet recs_liked_users = RecordSet.fromByteBuffer(like.loadLikedUsers(objectPhotoId, 0, getLikeUsers));
                                        List<Long> list_photo_liked_users = recs_liked_users.getIntColumnValues("liker");
                                        String likeuids = StringUtils.join(list_photo_liked_users, ",");
                                        RecordSet recs_user_liked = getUsers(rec.getString("source"), likeuids, USER_LIGHT_COLUMNS_LIGHT);
                                        Rec_photo_like.put("users", transUserAddressForQiupu(recs_user_liked));
                                    } else {
                                        Rec_photo_like.put("users", new Record());//3
                                    }

                                    Rec_photo_like.put("iliked", viewerId.equals("") ? false : like.ifUserLiked(viewerId, objectPhotoId));
                                    p_rec.put("likes", Rec_photo_like);

                                    Record Rec_comment = new Record();
                                    int comment_count = comment.getCommentCount(viewerId, objectPhotoId);
                                    Rec_comment.put("count", comment_count);
                                    if (comment_count > 0) {
                                        RecordSet recs_com = getCommentsForContainsIgnore(viewerId, Constants.PHOTO_OBJECT, photo_id, FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                                        Rec_comment.put("latest_comments", recs_com);
                                    } else {
                                        Rec_comment.put("latest_comments", new Record());
                                    }
                                    p_rec.put("comments", Rec_comment);
                                }
                            }
                            rec.put("attachments",tmp_attachments);
                        }
                        //以下是算针对post的
                        String attach = rec.getString("attachments");
                        if (attach.length() > 20) {
                            RecordSet tmp_attach = RecordSet.fromJson(attach);
                            if (tmp_attach.size() > 0) {
                                for (Record r : tmp_attach) {
                                    r.put("photo_img_original", r.getString("photo_img_middle"));
                                }
                            }
                            rec.put("attachments", tmp_attach);
                        }

                        Record Rec_post_like = new Record();
                        int like_post_count = like.getLikeCount(this_targetID);

                        Rec_post_like.put("count", like_post_count);
                        if (like_post_count > 0) {
                            RecordSet recs_liked_users = RecordSet.fromByteBuffer(like.loadLikedUsers(this_targetID, 0, getLikeUsers));
                            List<Long> liked_userIds = recs_liked_users.getIntColumnValues("liker");
                            String liked_uids = StringUtils.join(liked_userIds, ",");
                            RecordSet liked_users = getUsers(rec.getString("source"), liked_uids, USER_LIGHT_COLUMNS_LIGHT);
                            Rec_post_like.put("users", transUserAddressForQiupu(liked_users));
                            rec.put("likes", Rec_post_like);//3
                        } else {
                            rec.put("likes", new Record());//3
                        }

                        Record Rec_post_comment = new Record();
                        int comment_post_count = comment.getCommentCount(viewerId, this_targetID);
                        Rec_post_comment.put("count", comment_post_count);
                        if (comment_post_count > 0) {
                            RecordSet recs_com = getCommentsForContainsIgnore(viewerId, Constants.POST_OBJECT,rec.getInt("post_id") , FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                            Rec_post_comment.put("latest_comments", recs_com);
                            rec.put("comments", Rec_post_comment);//4
                        } else {
                            rec.put("comments", new Record());//4
                        }
                        rec.put("iliked", viewerId.equals("") ? false : like.ifUserLiked(viewerId, this_targetID));//4
                    } else {
                        Record rec_stream = new Record();
                        int stream_like_count = like.getLikeCount(this_targetID);
                        rec_stream.put("count", stream_like_count);

                        if (stream_like_count > 0) {
                            RecordSet recs_stream_liked_users = RecordSet.fromByteBuffer(like.loadLikedUsers(this_targetID, 0, getLikeUsers));
                            List<Long> list_liked_users = recs_stream_liked_users.getIntColumnValues("liker");
                            String likeuids = StringUtils.join(list_liked_users, ",");
                            RecordSet recs_users_liked = getUsers(rec.getString("source"), likeuids, USER_LIGHT_COLUMNS_LIGHT);
                            rec_stream.put("users", transUserAddressForQiupu(recs_users_liked));
                            rec.put("likes", rec_stream);//3
                        } else {
                            rec.put("likes", new Record());//3
                        }

                        Record tempRec = new Record();
                        int comc = comment.getCommentCount(viewerId, this_targetID);
                        tempRec.put("count", comc);
                        if (comc > 0) {
                            RecordSet recs_com = getCommentsForContainsIgnore(viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                            tempRec.put("latest_comments", recs_com);
                            rec.put("comments", tempRec);//4
                        } else {
                            rec.put("comments", new Record());//4
                        }
                        rec.put("root_id", "");

                        rec.put("iliked", viewerId.equals("") ? false : like.ifUserLiked(viewerId, this_targetID));//4

                    }
//                    rec.put("custom", userto);//5

                    RecordSet reshare_rec = getSharedPost(viewerId, rec.getString("post_id"));
                    rec.put("reshare_count", reshare_rec.size());

                    if (rec.getInt("root") > 0) {
                        RecordSet retweeted = transTimelineForQiupu(viewerId, getPosts(String.valueOf(rec.getInt("root")), POST_FULL_COLUMNS), 5, 5,false);
                        rec.put("retweeted_stream", retweeted.isEmpty() ? "" : retweeted.getFirstRecord());//6
                    }

                    int reportAbuseCount = getReportAbuserCount(rec.getString("post_id"));
                    rec.put("report_abuse_count",reportAbuseCount);

                    Configuration conf = getConfig();
                    String iconUrlPattern = conf.checkGetString("platform.sysIconUrlPattern");
                    if (rec.getInt("type") == 1) {
                        rec.put("icon", "");
                    }
                    if (rec.getInt("type") == 32) {
                        rec.put("icon", String.format(iconUrlPattern, "apk.gif"));
                    }
                    if (rec.getInt("type") == 256) {
                        rec.put("icon", String.format(iconUrlPattern, "comment.gif"));
                    }
                    if (rec.getInt("type") == 512) {
                        rec.put("icon", String.format(iconUrlPattern, "like.gif"));
                    }
                    if (rec.getInt("type") == 64) {
                        rec.put("icon", String.format(iconUrlPattern, "link.gif"));
                    }
                    if (rec.getInt("type") == 4096) {
                        rec.put("icon", String.format(iconUrlPattern, "friend.gif"));
                    }

                    String add_to_user = rec.getString("add_to");
                    if (add_to_user.length() > 0) {
                        RecordSet recs = getUsers(viewerId, add_to_user, USER_LIGHT_COLUMNS_LIGHT);
                        rec.put("add_new_users", recs.toString());
                    } else {
                        rec.put("add_new_users", new RecordSet());
                    }
                }
            }
        }

        //want merge，if stream from like and comment
        RecordSet out1Rs = new RecordSet();
        out1Rs = reds;
        for (Record moveR : reds) {//
            if (!single_get) {
                if ((int) moveR.getInt("report_abuse_count") >= REPORT_ABUSE_COUNT)
                    moveR.put("message", "###DELETE###");
                if (iHaveReport(viewerId, moveR.getString("post_id")) >= 1)
                    moveR.put("message", "###DELETE###");
            }

            int type0 = (int) moveR.getInt("type");
            String target0 = moveR.getString("target");
            long updated_time0 = moveR.getInt("updated_time");
            if (type0 != 256 && type0 != 512) {
            } else {
                for (Record check : out1Rs) {
                    //int type1 = (int) check.getInt("type");
                    String target1 = check.getString("target");
                    long updated_time1 = check.getInt("updated_time");

                    //if(type0==type1 && target0.equals(target1) && updated_time0<updated_time1){
                    if (target0.equals(target1) && updated_time0 < updated_time1) {
                        moveR.put("message", "###DELETE###");
                        break;
                    }
                }
            }
        }

        for (Record moveR : reds) {//
            if (moveR.getString("from").length() < 10)
                moveR.put("message", "###DELETE###");
        }

        for (int jj = reds.size() - 1; jj >= 0; jj--) {
            Record p = reds.get(jj);
            if (p.getString("message").equals("###DELETE###")) {
                reds.remove(jj);
            }
        }
        return reds;
    }

    public RecordSet transUserAddressForQiupu(RecordSet urecs) throws AvroRemoteException {
        String outAddr = "";

        for (Record rec : urecs) {
            if (rec.has("address")) {
                JsonNode a = JsonUtils.fromJson(rec.getString("address"), JsonNode.class);
                String oa = "";
                for (int i = 0; i < a.size(); i++) {
                    oa += a.get(i).findValue("street") + ",";
                }
                oa = oa.endsWith(",") ? oa.substring(0, oa.length() - 1) : oa;
                outAddr += oa + ",";
                rec.remove("address");
                rec.put("address", outAddr.endsWith(",") ? outAddr.substring(0, outAddr.length() - 1) : outAddr);
            }
        }
        return urecs;
    }

    public boolean postCanComment(String postId) throws AvroRemoteException {
        return getPost(postId, "can_comment").getBoolean("can_comment", false);
    }

    public boolean postCanLike(String postId) throws AvroRemoteException {
        return getPost(postId, "can_like").getBoolean("can_like", false);
    }

    public static final String QAPK_COLUMNS =
            "package,app_name,version_code,version_name,architecture,target_sdk_version,category,sub_category,"
                    + "created_time,info_updated_time,description,recent_change,rating,"
                    + "download_count,install_count,uninstall_count,favorite_count,upload_user,screen_support,icon_url,price,borqs,"
                    + "market_url,file_size,file_url,tag,screenshots_urls";

    public static final String QAPK_FULL_COLUMNS = QAPK_COLUMNS
            + ",app_comment_count,app_comments,app_like_count,app_liked_users,"
            + "app_likes,compatibility,app_used,app_favorite,lasted_version_code,lasted_version_name";


    // Comment
    public Record createComment(String userId, int objectType, String target, String message, String device, Boolean canLike, String location, String add_to, String appId) throws AvroRemoteException {
        // check user
        Record user = getUser(userId, userId, "user_id, display_name, login_email1, login_email2, login_email3", false);
        if (user.isEmpty())
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);

        // check can comment
        String targetObjectId;
        if (objectType == Constants.POST_OBJECT) {
            if (!postCanComment(target))
                throw Errors.createResponseError(ErrorCode.CANT_COMMENT, "The formPost '%s' is not can comment", target);

            targetObjectId = Constants.postObjectId(target);
        } else if (objectType == Constants.APK_OBJECT) {
            // TODO: move into hook
            targetObjectId = Constants.apkObjectId(target);
        } else if (objectType == Constants.FILE_OBJECT) {
            // TODO: move into hook
            targetObjectId = Constants.fileObjectId(target);
        } else if (objectType == Constants.PHOTO_OBJECT) {
            // TODO: move into hook
            targetObjectId = Constants.photoObjectId(target);
        } else if (objectType == Constants.POLL_OBJECT) {
            targetObjectId = Constants.pollObjectId(target);
        } else {
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "The object '%s' is not can comment", Constants.objectId(objectType, target));
        }

        //get user lasted comment for target
        Transceiver trans = getTransceiver(Comment.class);
        Comment comment = getProxy(Comment.class, trans);
        Record last_comment = Record.fromByteBuffer(comment.findMyLastedCommented(objectType + ":" + target,userId));
        if (last_comment.getString("message").equals(message))    {
            long created_time = last_comment.getInt("created_time");
            if (DateUtils.nowMillis() - created_time <= 1000 * 60 * 10L)
                throw Errors.createResponseError(ErrorCode.REPEAT_MESSAGE, "Repeat Comment in 10 minutes!", Constants.objectId(objectType, target));
        }

        // create comment
        Record rec = Record.of("message", message,
                "commenter_name", user.getString("display_name"));
        rec.put("device", device);
        if (canLike != null)
            rec.put("can_like", canLike);

        rec.put("add_to", add_to);

        String commentId;

        try {
            Record thisUser = getUsers(userId, userId, "display_name", true).getFirstRecord();

            commentId = toStr(comment.createComment(userId, targetObjectId, rec.toByteBuffer()));
            createConversation(Constants.COMMENT_OBJECT, commentId, Constants.C_COMMENT_CREATE, userId);
            if (add_to.length() > 0)
                createConversation(Constants.COMMENT_OBJECT, commentId, Constants.C_COMMENT_ADDTO, add_to);
            if (objectType == Constants.POST_OBJECT) {
                createConversation(Constants.POST_OBJECT, target, Constants.C_STREAM_COMMENT, userId);
                Record this_stream = getPost(target, "post_id,source,message");
                String body = message;

                sendNotification(Constants.NTF_MY_STREAM_COMMENT,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), this_stream.getString("source"), this_stream.getString("message")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), this_stream.getString("source"), this_stream.getString("message"), commentId),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, commentId)
                );
            }
            if (objectType == Constants.APK_OBJECT) {
                String[] o = target.split("-");
                String t = "";
                if (o.length == 3 || o.length == 1)
                    t = o[0];
                if (t.length() > 0)
                    createConversation(Constants.APK_OBJECT, t, Constants.C_APK_COMMENT, userId);
                Record mcs = thisTrandsGetApkInfo(userId, toStr(target), "app_name", 1000).getFirstRecord();
                String body = message;

                sendNotification(Constants.NTF_MY_APP_COMMENT,
                        createArrayNodeFromStrings(appId),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(body),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, commentId)
                );
            }

            if (objectType == Constants.APK_OBJECT) {
                String m = "";
                String tempNowAttachments = "[]";
                autoPost(userId, Constants.APK_COMMENT_POST, message, tempNowAttachments, toStr(Constants.APP_TYPE_QIUPU), "", target, m, "", false, QAPK_FULL_COLUMNS, device, location, true, true, true, "", "", false);
//                String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
//                sendCommentOrLikeEmail(Constants.APK_OBJECT, userId, user, target, message, lang);
            } else if (objectType == Constants.BOOK_OBJECT) {//for book

            } else if (objectType == Constants.PHOTO_OBJECT) {//for PHOTO
                createConversation(Constants.PHOTO_OBJECT, target, Constants.C_PHOTO_COMMENT, userId);

                sendNotification(Constants.NTF_PHOTO_COMMENT,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), message, commentId),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), message, commentId),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, commentId)
                );

            }  else if (objectType == Constants.FILE_OBJECT) {//for PHOTO
                createConversation(Constants.FILE_OBJECT, target, Constants.C_FILE_COMMENT, userId);

                sendNotification(Constants.NTF_FILE_COMMENT,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), message, commentId),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), message, commentId),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, commentId)
                );

            } else if (objectType == Constants.POLL_OBJECT) {//for poll
                createConversation(Constants.POLL_OBJECT, target, Constants.C_POLL_COMMENT, userId);
                String title = getSimplePolls(target).getFirstRecord().getString("title");
                sendNotification(Constants.NTF_POLL_COMMENT,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(title),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(title, target),
                        createArrayNodeFromStrings(message),
                        createArrayNodeFromStrings(message),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, commentId)
                );

            }
        } finally {
            closeTransceiver(trans);
        }

        Record commentRec = getComment(userId, commentId, "comment_id, target, commenter, commenter_name, created_time, message,add_to");

        trans = getTransceiver(Stream.class);
        Stream stream = getProxy(Stream.class, trans);
        if (objectType == Constants.POST_OBJECT) {
            try {
                stream.touch(target);
                //send notify email
//                String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
//                sendCommentOrLikeEmail(Constants.POST_OBJECT, userId, user, target, message, lang);
            } finally {
                closeTransceiver(trans);
            }
        }

        return commentRec;
    }


    public Record createComment1(String userId, int objectType, String target, String message, String device, String location, String add_to, String appId) throws AvroRemoteException {
        return createComment(userId, objectType, target, message, device, true, location, add_to, appId);
    }

    private void sendCommentOrLikeEmail(int type, String viewerId, Record user, String target, String message, String lang) {
        Record rec = new Record();
        rec.put("type", type);
        rec.put("viewerId", viewerId);
        rec.put("user", user.toString(false, false));
        rec.put("target", target);
        rec.put("message", message);
        rec.put("lang", lang);

        String rec_str = rec.toString(false, false);
        MQ mq = MQCollection.getMQ("platform");
        if ((mq != null) && (rec_str.length() < 1024))
            mq.send("mail", rec_str);
    }

    public String formatOldDataToConversation(String viewerId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        Stream stream = getProxy(Stream.class, trans);
        stream.formatOldDataToConversation(viewerId);
        return "";
    }

    public boolean formatStreamLocation() throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        Stream stream = getProxy(Stream.class, trans);
        return stream.formatStreamLocation();
    }

    public String testConversation(String viewerId) throws AvroRemoteException {
        List<Long> userIds = new ArrayList<Long>();
        List<Long> whoComments = new ArrayList<Long>();
        String post_id = "2787585188212716614";
        String s_userid = getPost(post_id, "source").getString("source");
        userIds.add(Long.parseLong(s_userid));
        RecordSet recs_comments = getCommentsFor("", Constants.POST_OBJECT, post_id, "commenter", false, 0, 200);
        for (Record r0 : recs_comments) {
            Long commenterId = Long.parseLong(r0.getString("commenter"));
            if (!userIds.contains(commenterId) && !r0.getString("commenter").equals("") && !r0.getString("commenter").equals("0")) {
                userIds.add(commenterId);
            }
            if (!whoComments.contains(commenterId) && !r0.getString("commenter").equals("") && !r0.getString("commenter").equals("0")) {
                whoComments.add(commenterId);
            }
        }


        //=========================new send to ,from conversation=========================
        List<Long> userIds1 = new ArrayList<Long>();
        List<Long> whoComments1 = new ArrayList<Long>();
        List<String> reasons = new ArrayList<String>();
        reasons.add(String.valueOf(Constants.C_STREAM_POST));
        reasons.add(String.valueOf(Constants.C_STREAM_COMMENT));
        RecordSet conversation_users = getConversation(Constants.POST_OBJECT, post_id, reasons, 0, 0, 100);
        for (Record r : conversation_users) {
            if (!userIds1.contains(Long.parseLong(r.getString("from_"))))
                userIds1.add(Long.parseLong(r.getString("from_")));
        }

        List<String> reasons1 = new ArrayList<String>();
        reasons1.add(String.valueOf(Constants.C_STREAM_COMMENT));
        RecordSet conversation_users1 = getConversation(Constants.POST_OBJECT, post_id, reasons1, 0, 0, 100);
        for (Record r1 : conversation_users1) {
            if (!whoComments1.contains(Long.parseLong(r1.getString("from_"))))
                whoComments1.add(Long.parseLong(r1.getString("from_")));
        }

        //=========================new send to ,from conversation end ====================


        return "";
    }


    public void sendApkCommentOrLikeEmail(String viewerId, Record user, String target, String message, String lang) throws AvroRemoteException {
        String[] emails = new String[3];
        emails[0] = user.getString("login_email1");
        emails[1] = user.getString("login_email2");
        emails[2] = user.getString("login_email3");
        HashSet<String> haveSend = new HashSet<String>();
        for (String email : emails) {
            if (StringUtils.isNotBlank(email))
                haveSend.add(email);
        }

        String userCols = "user_id, display_name, login_email1, login_email2, login_email3";
        Record apk = thisTrandsGetApkInfo(viewerId, toStr(target), "app_name,upload_user", 1000).getFirstRecord();
        String appName = apk.getString("app_name");
        /*
        long uploaderId = apk.getInt("upload_user");
        RecordSet sendTo = new RecordSet();
        int qid = Integer.parseInt(qiupuUid);
        if ((uploaderId != 0) && (uploaderId != qid)) {
            Record uploader = getUser(viewerId, String.valueOf(uploaderId), userCols, false);
            sendTo.add(uploader);
        }

        RecordSet participants = new RecordSet();
        boolean isLike = message.equalsIgnoreCase("likes");
        if (isLike) {
            participants = likedUsers(viewerId, Constants.APK_OBJECT, target, userCols, 0, 5);
        } else {
            RecordSet comments = getCommentsFor(viewerId, Constants.APK_OBJECT, target, "commenter", false, 0, 5);
            List<Long> list = comments.unique("commenter").getIntColumnValues("commenter");
            String temp = StringUtils2.joinIgnoreBlank(",", list);
            participants = getUsers(viewerId, temp, userCols, false);
        }

        sendTo.addAll(participants);
        */
        //=========================new send to ,from conversation=========================
        boolean isLike = message.equalsIgnoreCase("likes");
        RecordSet participants = new RecordSet();

        RecordSet conversation_users = getConversation(Constants.APK_OBJECT, apk.getString("package"), new ArrayList<String>(), 0, 0, 100);
        String cUserIds = conversation_users.joinColumnValues("from_", ",");
        participants = getUsers(viewerId, cUserIds, userCols, false);

        RecordSet sendTo = new RecordSet();
        sendTo.addAll(participants);
        //=========================new send to ,from conversation end ====================

        String username = user.getString("display_name");
        String emailContent = "";
        if (isLike) {
            String template = Constants.getBundleStringByLang(lang, "platform.sendmail.apk.like.content");
            emailContent = SQLTemplate.merge(template, new Object[][]{
                    {"username", username},
                    {"appName", appName}
            });
        } else {
            String template = Constants.getBundleStringByLang(lang, "platform.sendmail.apk.comment.content");
            emailContent = SQLTemplate.merge(template, new Object[][]{
                    {"username", username},
                    {"appName", appName},
                    {"message", message}
            });
        }

        String key = isLike ? Constants.EMAIL_APK_LIKE : Constants.EMAIL_APK_COMMENT;
        String likeTitle = Constants.getBundleStringByLang(lang, "platform.sendmail.apk.like.title");
        String commentTitle = Constants.getBundleStringByLang(lang, "platform.sendmail.apk.comment.title");
        String titleTemplate = isLike ? likeTitle : commentTitle;
        String title = SQLTemplate.merge(titleTemplate, new Object[][]{
                {"username", username}
        });
        String emailType = isLike ? Constants.EMAIL_APK_LIKE : Constants.EMAIL_APK_COMMENT;

        for (Record r : sendTo) {
            String userId = r.getString("user_id");
            Record setting = getPreferencesByUsers(key, userId);
            String value = setting.getString(userId, "0");
            L.trace("Up to send mail info -> User: " + userId + " Key: " + key + " Value: " + value);
            if (value.equals("0")) {
                emails[0] = r.getString("login_email1");
                emails[1] = r.getString("login_email2");
                emails[2] = r.getString("login_email3");

                for (String email : emails) {
                    if (StringUtils.isNotBlank(email) && !haveSend.contains(email)) {
                        sendEmail(title, email, email, emailContent, emailType, lang);
                        haveSend.add(email);
                    }
                }
            }
        }
    }

    public void sendStreamCommentOrLikeEmail(String viewerId, Record user, String target, String message, String lang) throws AvroRemoteException {
        String[] emails = new String[3];
        emails[0] = user.getString("login_email1");
        emails[1] = user.getString("login_email2");
        emails[2] = user.getString("login_email3");
        HashSet<String> haveSend = new HashSet<String>();
        for (String email : emails) {
            if (StringUtils.isNotBlank(email))
                haveSend.add(email);
        }

        String userCols = "user_id, display_name, login_email1, login_email2, login_email3";
        Record post = getPost(target, "source,mentions,type,attachments,message");
        String source = post.getString("source");
        Record from = getUser(viewerId, source, userCols, false);
        /*
        String mentions = post.getString("mentions");
        RecordSet to = getUsers(viewerId, mentions, userCols, false);

        RecordSet participants = new RecordSet();
        boolean isLike = message.equalsIgnoreCase("likes");
        if (isLike) {
            participants = likedUsers(viewerId, Constants.POST_OBJECT, target, userCols, 0, 5);
        } else {
            RecordSet comments = getCommentsFor(viewerId, Constants.POST_OBJECT, target, "commenter", false, 0, 5);
            List<Long> list = comments.unique("commenter").getIntColumnValues("commenter");
            String temp = StringUtils2.joinIgnoreBlank(",", list);
            participants = getUsers(viewerId, temp, userCols, false);
        }
        RecordSet sendTo = new RecordSet();
        sendTo.add(from);
        sendTo.addAll(to);
        sendTo.addAll(participants);
        */

        //=========================new send to ,from conversation=========================
        boolean isLike = message.equalsIgnoreCase("likes");
        RecordSet participants = new RecordSet();
//        List<String> reason = new ArrayList<String>();
//        reason.add(String.valueOf(Constants.C_STREAM_TO));
//        reason.add(String.valueOf(Constants.C_STREAM_ADDTO));
        RecordSet conversation_users = getConversation(Constants.POST_OBJECT, target, new ArrayList<String>(), 0, 0, 100);
        String cUserIds = conversation_users.joinColumnValues("from_", ",");
        participants = getUsers(viewerId, cUserIds, userCols, false);

        RecordSet sendTo = new RecordSet();
        sendTo.addAll(participants);
        //=========================new send to ,from conversation end ====================

        int type = (int) post.getInt("type");
        String attachments = post.getString("attachments");
        JsonNode jnode = JsonUtils.parse(attachments);
        jnode = jnode.get(0);
        String shareType = Constants.getBundleStringByLang(lang, "platform.sendmail.stream.type.message");
        String shareContent = post.getString("message");

        if (type == Constants.APK_POST) {
            shareType = Constants.getBundleStringByLang(lang, "platform.sendmail.stream.type.app");
            shareContent = jnode.get("app_name").getTextValue();
        } else if (type == Constants.APK_LINK_POST) {
            shareType = Constants.getBundleStringByLang(lang, "platform.sendmail.stream.type.apklink");
            shareContent = jnode.get("href").getTextValue();
        }

        String username = user.getString("display_name");
        String emailContent = "";
        if (isLike) {
            String template = Constants.getBundleStringByLang(lang, "platform.sendmail.stream.like.content");
            emailContent = SQLTemplate.merge(template, new Object[][]{
                    {"username", username},
                    {"fromname", from.getString("display_name")},
                    {"shareType", shareType},
                    {"shareContent", shareContent}
            });
        } else {
            String template = Constants.getBundleStringByLang(lang, "platform.sendmail.stream.comment.content");
            emailContent = SQLTemplate.merge(template, new Object[][]{
                    {"username", username},
                    {"fromname", from.getString("display_name")},
                    {"shareType", shareType},
                    {"shareContent", shareContent},
                    {"message", message}
            });
        }

        String key = isLike ? Constants.EMAIL_STREAM_LIKE : Constants.EMAIL_STREAM_COMMENT;
        String likeTitle = Constants.getBundleStringByLang(lang, "platform.sendmail.stream.like.title");
        String commentTitle = Constants.getBundleStringByLang(lang, "platform.sendmail.stream.comment.title");
        String titleTemplate = isLike ? likeTitle : commentTitle;
        String title = SQLTemplate.merge(titleTemplate, new Object[][]{
                {"username", username}
        });
        String emailType = isLike ? Constants.EMAIL_STREAM_LIKE : Constants.EMAIL_STREAM_COMMENT;

        for (Record r : sendTo) {
            String userId = r.getString("user_id");

            Record setting = getPreferencesByUsers(key, userId);
            String value = setting.getString(userId, "0");
            L.trace("Up to send mail info -> User: " + userId + " Key: " + key + " Value: " + value);
            if (value.equals("0")) {
                emails[0] = r.getString("login_email1");
                emails[1] = r.getString("login_email2");
                emails[2] = r.getString("login_email3");

                for (String email : emails) {
                    if (StringUtils.isNotBlank(email) && !haveSend.contains(email)) {
                        sendEmail(title, email, email, emailContent, emailType, lang);
                        haveSend.add(email);
                    }
                }
            }
        }
    }

    public String composeShareContent(String viewerId, int type, String body, boolean isEmail, String device) throws AvroRemoteException {
        String displayName = getUser(viewerId, viewerId, "display_name", false).getString("display_name");
        String shareType = "";
        switch (type) {
            case Constants.TEXT_POST:
                shareType = Constants.getBundleString(device, "platform.sendmail.stream.type.message");
                break;
            case Constants.APK_POST:
                shareType = Constants.getBundleString(device, "platform.sendmail.stream.type.app");
                break;
            case Constants.APK_LINK_POST:
                shareType = Constants.getBundleString(device, "platform.sendmail.stream.type.apklink");
                break;
            case Constants.LINK_POST:
                shareType = Constants.getBundleString(device, "platform.sendmail.stream.type.link");
                break;
            case Constants.BOOK_POST:
                shareType = Constants.getBundleString(device, "platform.sendmail.stream.type.book");
                break;
            case Constants.PHOTO_POST:
                shareType = Constants.getBundleString(device, "platform.sendmail.stream.type.photo");
                break;
            default:
                shareType = Constants.getBundleString(device, "platform.sendmail.stream.type.message");
                break;
        }

        String key = isEmail ? "platform.compose.share.content" : "platform.compose.share.sms";
        String template = Constants.getBundleString(device, key);
        String content = SQLTemplate.merge(template, new Object[][]{
                {"displayName", displayName},
                {"shareType", shareType},
                {"body", body},
                {"serverHost", SERVER_HOST}
        });

        return content;
    }

    public List<String> getEmails(String fromMentions) throws UnsupportedEncodingException {
        List<String> outEmailList = new ArrayList<String>();
        if (fromMentions.trim().length() > 0) {
            List<String> m = StringUtils2.splitList(fromMentions, ",", true);
            for (String s : m) {
                if (s.startsWith("*")) {
                    String b = s.substring(1, s.length());
                    if (b.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
                        outEmailList.add(b);
                    }
                }
            }
        }
        return outEmailList;
    }

    public List<String> getPhones(String fromMentions) throws UnsupportedEncodingException {
        List<String> outPhoneList = new ArrayList<String>();
        if (fromMentions.trim().length() > 0) {
            List<String> m = StringUtils2.splitList(fromMentions, ",", true);
            for (String s : m) {
                if (s.startsWith("*")) {
                    String b = s.substring(1, s.length());
                    if (b.matches("(13[\\d]{9})")) {
                        outPhoneList.add(b);
                    }
                }
            }
        }
        return outPhoneList;
    }

    public String getOldMentions(String userId, String fromMentions) throws UnsupportedEncodingException, AvroRemoteException {
        List<String> outUserList = new ArrayList<String>();
        if (fromMentions.trim().length() > 0) {
            List<String> m = StringUtils2.splitList(fromMentions, ",", true);
            for (String s : m) {
                if (!s.startsWith("*")) {
                    outUserList.add(s);
                } else {
                    String emailOrPhone = StringUtils.substringAfter(s, "*");
                    if (!StringUtils.contains(emailOrPhone, '@')) {
                        emailOrPhone = PhoneNumberHelper.stripMobilePhoneNumber(emailOrPhone);
                    }

                    String uid = findUserIdByUserName(emailOrPhone);
                    if (StringUtils.isNotBlank(uid) && !StringUtils.equals(uid, "0")) {
                        outUserList.add(uid);
                    }
                    String virtualFriendId = getUserFriendhasVirtualFriendId(userId, emailOrPhone);
                    if (!outUserList.contains(virtualFriendId) && !StringUtils.equals(virtualFriendId, "0")) {
                        outUserList.add(virtualFriendId);
                    }
                }
            }
        }
        String outString = "";
        if (outUserList.size() > 0)
            outString = StringUtils.join(outUserList, ",");
        return outString;
    }

    public RecordSet destroyComments(String userId, String commentIds) throws AvroRemoteException {
        // check user
        if (Constants.isNullUserId(userId)) {
            if (!hasUser(userId)) {
                throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);
            }
        }

        Transceiver trans = getTransceiver(Comment.class);
        Transceiver transt = getTransceiver(Stream.class);
        boolean b = false;
        try {
            Comment comment = getProxy(Comment.class, trans);
            Stream s = getProxy(Stream.class, transt);
            RecordSet rsd = new RecordSet();
            //update lasted stream' acctachments
            //get target from comment_id
            List<String> cIds = StringUtils2.splitList(toStr(commentIds), ",", true);

            for (String commentId : cIds) {
                Record rec = getCommentsAll(commentId, "target").getFirstRecord();
                String[] ss = StringUtils.split(StringUtils.trimToEmpty(rec.getString("target")), ':');

                if (ss[0].equals(String.valueOf(Constants.POST_OBJECT))) {
                    //get stream，
                    Record r0 = getPost(ss[1], "source");
                    rsd.add(Record.fromByteBuffer(comment.destroyComments(userId, commentId, r0.getString("source"), ss[0])));
                } else {
                    rsd.add(Record.fromByteBuffer(comment.destroyComments(userId, commentId, "", ss[0])));
                }

                //must after delete comment
                deleteConversation(Constants.COMMENT_OBJECT, commentId, -1, 0);
                if (ss[0].equals(String.valueOf(Constants.POST_OBJECT))) {
                    deleteConversation(Constants.POST_OBJECT, ss[1], Constants.C_STREAM_COMMENT, Long.parseLong(userId));
                } else if (ss[0].equals(String.valueOf(Constants.APK_OBJECT))) {
                    String pg = ss[1];
                    if (ss[1].split("-").length > 1)
                        pg = ss[1].split("-")[0].toString();
                    deleteConversation(Constants.APK_OBJECT, pg, Constants.C_APK_COMMENT, Long.parseLong(userId));
                } else if (ss[0].equals(String.valueOf(Constants.PHOTO_OBJECT))) {
                    String pg = ss[1];
                    deleteConversation(Constants.PHOTO_OBJECT, pg, Constants.C_PHOTO_COMMENT, Long.parseLong(userId));
                }
            }

            for (String commentId : cIds) {
                Record rec = getCommentsAll(commentId, "target").getFirstRecord();
                String[] ss = StringUtils.split(StringUtils.trimToEmpty(rec.getString("target")), ':');
                if (ss[0].equals(String.valueOf(Constants.APK_OBJECT))) {
                    //get stream，update
                    String apk_id = ss[1];
                    Record r = getTopOneStreamByTarget(Constants.APK_COMMENT_POST, apk_id).getFirstRecord();
                    String attach = thisTrandsGetApkInfo(userId, toStr(apk_id), QAPK_FULL_COLUMNS, 1000).toString(false, false);
                    b = s.updateAttachment(r.getString("post_id"), attach);
                }
            }
            return rsd;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet destroyComments(String commentIds) throws AvroRemoteException {
        return destroyComments(Constants.NULL_USER_ID, commentIds);
    }

    public int getCommentCount(String viewerId, int objectType, Object id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            String targetObjectId = Constants.objectId(objectType, id);
            Comment comment = getProxy(Comment.class, trans);
            return comment.getCommentCount(viewerId, targetObjectId);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean linkRemoveCache(String key) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            XMemcached mc = new XMemcached();
//            mc.path="file://D:\\3workspace\\mytest\\src\\main\\java\\company\\test\\PlatformWebServer.properties") ;
            if (SERVER_HOST.equals("api.borqs.com")) {
                mc.path = "/home/wutong/etc/test_web_server.properties";
            } else {
                mc.path = "/home/zhengwei/work2/dist/etc/test_web_server.properties";
            }

            mc.init();
            mc.deleteCache(key);
            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateCommentCanLike(String userId, String commentId, boolean can_like) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            Comment comment = getProxy(Comment.class, trans);
            return comment.updateCanLike(userId, commentId, can_like);
        } finally {
            closeTransceiver(trans);
        }
    }

    public static final String FULL_COMMENT_COLUMNS = "comment_id, target, created_time, " +
            "commenter, commenter_name, message, device, can_like,destroyed_time,add_to";

    public RecordSet getCommentsFor(String viewerId, int objectType, Object id, String cols, boolean asc, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            String targetObjectId = Constants.objectId(objectType, id);
            Comment comment = getProxy(Comment.class, trans);

            RecordSet recs = RecordSet.fromByteBuffer(comment.getCommentsFor(targetObjectId, cols, asc, page, count));
            recs = formatIgnoreStreamOrComments(viewerId, "comment", recs);
            return transComment(viewerId, recs);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getCommentsForContainsIgnore(String viewerId, int objectType, Object id, String cols, boolean asc, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            String targetObjectId = Constants.objectId(objectType, id);
            Comment comment = getProxy(Comment.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(comment.getCommentsForContainsIgnore(viewerId, targetObjectId, cols, asc, page, count));
            return transComment(viewerId, recs);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getFullCommentsFor(String viewerId, int objectType, Object id, boolean asc, int page, int count) throws AvroRemoteException {
        return getCommentsForContainsIgnore(viewerId, objectType, id, FULL_COMMENT_COLUMNS, asc, page, count);
    }

    public RecordSet getComments(String viewerId, String commentIds, String cols) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            Comment comment = getProxy(Comment.class, trans);
            if (cols.isEmpty() || cols.equals("")) {
                cols = FULL_COMMENT_COLUMNS;
            }
            RecordSet recs = RecordSet.fromByteBuffer(comment.getComments(commentIds, cols));
            recs = formatIgnoreStreamOrComments(viewerId, "comment", recs);
            return transComment(viewerId, recs);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet transComment(String viewerId, RecordSet commentDs) throws AvroRemoteException {
        if (commentDs.size() > 0) {
            Transceiver trans = getTransceiver(Like.class);
            Like l = getProxy(Like.class, trans);
            for (Record r : commentDs) {
                r.put("image_url",getUser(r.getString("commenter"),r.getString("commenter"),"image_url").getString("image_url"));
                Record likes = new Record();
                likes.put("count", l.getLikeCount(String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id")));
                RecordSet lu = RecordSet.fromByteBuffer(l.loadLikedUsers(String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id"), 0, 500));
                if (lu.size() > 0) {
                    String uid = lu.joinColumnValues("liker", ",");
                    likes.put("users", getUsers(viewerId, uid, USER_LIGHT_COLUMNS_LIGHT));
                } else {
                    likes.put("users", new RecordSet());
                }
                r.put("likes", likes);

                if (!viewerId.equals("")) {
                    r.put("iliked", l.ifUserLiked(viewerId, String.valueOf(Constants.COMMENT_OBJECT) + ":" + r.getString("comment_id")));
                } else {
                    r.put("iliked", false);
                }
                if (r.getString("add_to").trim().length() > 0) {
                    RecordSet recs = getUsers(viewerId, r.getString("add_to"), USER_LIGHT_COLUMNS_LIGHT);
                    r.put("add_new_users", recs.toString());
                } else {
                    r.put("add_new_users", new RecordSet());
                }
            }
        }
        return commentDs;
    }

    public RecordSet getCommentsAll(String commentIds, String cols) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            Comment comment = getProxy(Comment.class, trans);
            if (cols.isEmpty() || cols.equals("")) {
                cols = FULL_COMMENT_COLUMNS;
            }
            RecordSet recs = RecordSet.fromByteBuffer(comment.getCommentsAll(commentIds, cols));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getFullComments(String viewerId, String commentIds) throws AvroRemoteException {
        return getComments(viewerId, commentIds, FULL_COMMENT_COLUMNS);
    }

    public Record getComment(String viewerId, String commentId, String cols) throws AvroRemoteException {
        return getComments(viewerId, firstId(commentId), cols).getFirstRecord();
    }

    public Record getFullComment(String viewerId, String commentId) throws AvroRemoteException {
        return getComment(viewerId, commentId, FULL_COMMENT_COLUMNS);
    }


    public boolean commentCanLike(String viewerId, String commentId) throws AvroRemoteException {
        return getComment(viewerId, commentId, "can_like").getBoolean("can_like", false);
    }


    public RecordSet getCommentedPostTarget(String userId, int objectType, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            Comment comment = getProxy(Comment.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(comment.getCommentedPost(userId, page, count, objectType));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean getIHasCommented(String commenter, int target_type, String target_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            Comment comment = getProxy(Comment.class, trans);
            return comment.getIHasCommented(commenter, String.valueOf(target_type) + ":" + target_id);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getCommentedPosts(String userId, String cols, int objectType, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Comment.class);
        try {
            Comment comment = getProxy(Comment.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(comment.getCommentedPost(userId, page, count, objectType));

            for (Record rec : recs) {
                rec.put("post_id", rec.getString("target").replace(toStr(objectType) + ":", ""));
            }

            if (cols.isEmpty() || cols.equals("")) {
                cols = POST_FULL_COLUMNS;
            }

            return getPosts(recs.joinColumnValues("post_id", ","), cols);
        } finally {
            closeTransceiver(trans);
        }
    }

    // Likes
    public boolean like(String userId, int objectType, String target, String device, String location, String appId) throws AvroRemoteException {
        Record user = getUser(userId, userId, "user_id, display_name, login_email1, login_email2, login_email3", false);
        if (user.isEmpty()) {
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);
        }

        // TODO: move to hook
        if (objectType == Constants.POST_OBJECT) {
            if (!postCanLike(target)) {
                throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "The formPost '%s' is not can like", target);
            }
        } else if (objectType == Constants.COMMENT_OBJECT) {
            if (!commentCanLike(userId, target)) {
                throw Errors.createResponseError(ErrorCode.CANT_LIKE, "The comment '%s' is not can like", target);
            }
        } else if (objectType == Constants.APK_OBJECT) {
            // OK
        } else if (objectType == Constants.PHOTO_OBJECT) {
            // OK
        } else if (objectType == Constants.POLL_OBJECT) {
            // OK
        }
        else {
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "The object '%s' is not can like", Constants.objectId(objectType, target));
        }

        String targetObjectId = Constants.objectId(objectType, target);
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like like = getProxy(Like.class, trans);
            Record thisUser = getUsers(userId, userId, "display_name", true).getFirstRecord();

            boolean b = like.createLike(userId, targetObjectId);

            //if like for APK,send stream
            if (b) {
                String sLike = Constants.getBundleString(device, "platform.like.like");
                if (objectType == Constants.POST_OBJECT) {
                    Record this_stream = getPost(target, "post_id,source,message");
//                    NotificationSender notif = new StreamLikeNotifSender(this, null);
                    String body = sLike;

                    sendNotification(Constants.NTF_MY_STREAM_LIKE,
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), this_stream.getString("source"), this_stream.getString("message")),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(target),
                            createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), this_stream.getString("source"), this_stream.getString("message")),
                            createArrayNodeFromStrings(body),
                            createArrayNodeFromStrings(body),
                            createArrayNodeFromStrings(target),
                            createArrayNodeFromStrings(target, userId)
                    );
                }
                if (objectType == Constants.APK_OBJECT) {
//                    NotificationSender notif = new AppLikeNotifSender(this, null);
                    Record mcs = thisTrandsGetApkInfo(userId, toStr(target), "app_name", 1000).getFirstRecord();
                    String body = sLike;

                    sendNotification(Constants.NTF_MY_APP_LIKE,
                            createArrayNodeFromStrings(appId),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(target),
                            createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                            createArrayNodeFromStrings(body),
                            createArrayNodeFromStrings(body),
                            createArrayNodeFromStrings(target),
                            createArrayNodeFromStrings(target, userId)
                    );
                }

                Transceiver trans1 = getTransceiver(Stream.class);
                Stream stream = getProxy(Stream.class, trans1);
                if (objectType == Constants.APK_OBJECT) {
                    String[] a = target.split("-");
                    if (a.length == 1 || a.length == 3) {
                        createConversation(Constants.APK_OBJECT, a[0], Constants.C_APK_LIKE, userId);
                    }
                    //like in 2 minutes,dont send stream,dont send nail
                    //get my lasted like stream
                    boolean send = false;
                    RecordSet temprec = RecordSet.fromByteBuffer(stream.myTopOneStreamByTarget(userId, Constants.APK_LIKE_POST, target, "created_time"));
                    if (temprec.size() <= 0) {
                        send = true;
                    } else {
                        long oldTime = temprec.getFirstRecord().getInt("created_time");
                        long datediff = 2 * 60 * 1000; //diff 2 minutes
                        long now = DateUtils.nowMillis();
                        send = (now - datediff > oldTime);
                    }
                    if (send) {
                        String tempNowAttachments = "[]";
                        //attachments for client

                        autoPost(userId, Constants.APK_LIKE_POST, sLike, tempNowAttachments, toStr(Constants.APP_TYPE_QIUPU), "", target, "", "", false, QAPK_FULL_COLUMNS, device, location, true, true, true, "", "", false);
//                        String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
//                        sendCommentOrLikeEmail(Constants.APK_OBJECT, userId, user, target, "likes", lang);
                    }
                } else if (objectType == Constants.POST_OBJECT) {
                    stream.touch(target);
                    //send notify email
//                    String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
//                    sendCommentOrLikeEmail(Constants.POST_OBJECT, userId, user, target, "likes", lang);
                    createConversation(Constants.POST_OBJECT, target, Constants.C_STREAM_LIKE, userId);
                } else if (objectType == Constants.BOOK_OBJECT)//for books
                {

                } else if (objectType == Constants.COMMENT_OBJECT)//for books
                {
                    createConversation(Constants.COMMENT_OBJECT, target, Constants.C_COMMENT_LIKE, userId);
                } else if (objectType == Constants.PHOTO_OBJECT)//for photo
                {
                    createConversation(Constants.PHOTO_OBJECT, target, Constants.C_PHOTO_LIKE, userId);

                    sendNotification(Constants.NTF_PHOTO_LIKE,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId, thisUser.getString("display_name")),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(target),
                        createArrayNodeFromStrings(target, userId)
                    );
                } else if (objectType == Constants.FILE_OBJECT)//for photo
                {
                    createConversation(Constants.FILE_OBJECT, target, Constants.C_FILE_LIKE, userId);

                    sendNotification(Constants.NTF_FILE_LIKE,
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(target, userId, thisUser.getString("display_name")),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(target),
                            createArrayNodeFromStrings(target, userId, thisUser.getString("display_name")),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(target),
                            createArrayNodeFromStrings(target, userId)
                    );
                } else if (objectType == Constants.POLL_OBJECT) {//for poll
                    createConversation(Constants.POLL_OBJECT, target, Constants.C_POLL_LIKE, userId);
                    String title = getSimplePolls(target).getFirstRecord().getString("title");
                    sendNotification(Constants.NTF_POLL_LIKE,
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(title),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(target),
                            createArrayNodeFromStrings(title, target),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(target),
                            createArrayNodeFromStrings(target, userId)
                    );

                }
            }
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean unlike(String userId, int objectType, String target) throws AvroRemoteException {
        if (!hasUser(userId))
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);

        String targetObjectId = Constants.objectId(objectType, target);
        Transceiver trans = getTransceiver(Like.class);
        Transceiver transs = getTransceiver(Stream.class);
        Like like = getProxy(Like.class, trans);
        boolean b = like.destroyLike(userId, targetObjectId);
        try {
            if (b) {
                Stream s = getProxy(Stream.class, transs);
                if (String.valueOf(objectType).equals(String.valueOf(Constants.APK_OBJECT))) {
                    //get stream，update
                    Record r = getTopOneStreamByTarget(Constants.APK_LIKE_POST, target).getFirstRecord();
                    String attach = thisTrandsGetApkInfo(userId, toStr(target), QAPK_FULL_COLUMNS, 1000).toString(false, false);
                    s.updateAttachment(r.getString("post_id"), attach);
                    String[] a = target.split("-");
                    if (a.length == 1 || a.length == 3) {
                        deleteConversation(Constants.APK_OBJECT, a[0], Constants.C_APK_LIKE, Long.parseLong(userId));
                    }
                } else if (String.valueOf(objectType).equals(String.valueOf(Constants.POST_OBJECT))) {
                    deleteConversation(Constants.POST_OBJECT, target, Constants.C_STREAM_LIKE, Long.parseLong(userId));
                } else if (String.valueOf(objectType).equals(String.valueOf(Constants.COMMENT_OBJECT))) {
                    deleteConversation(Constants.COMMENT_OBJECT, target, Constants.C_COMMENT_LIKE, Long.parseLong(userId));
                } else if (String.valueOf(objectType).equals(String.valueOf(Constants.PHOTO_OBJECT))) {
                    deleteConversation(Constants.PHOTO_OBJECT, target, Constants.C_PHOTO_LIKE, Long.parseLong(userId));
                }
            }
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateStreamAttachment(String post_id, String attachment) throws AvroRemoteException {
        Transceiver transs = getTransceiver(Stream.class);
        try {
            Stream s = getProxy(Stream.class, transs);
            boolean b = s.updateAttachment(post_id, attachment);
            return b;
        } finally {
            closeTransceiver(transs);
        }
    }

    public boolean updateStreamCanCommentOrcanLike(String post_id, String viewerId, Record rec) throws AvroRemoteException {
        Transceiver transs = getTransceiver(Stream.class);
        try {
            Stream s = getProxy(Stream.class, transs);
            String can_comment = rec.getString("can_comment");
            String can_like = rec.getString("can_like");
            String can_reshare = rec.getString("can_reshare");
            if (can_comment.length() > 0) {
                int v = 0;
                if (rec.getString("can_comment").equalsIgnoreCase("true") || can_comment.equals("1")) {
                    v = 1;
                }
                s.updatePostForCommentOrLike(post_id, viewerId, "can_comment", v);
            }
            if (can_like.length() > 0) {
                int v = 0;
                if (rec.getString("can_like").equalsIgnoreCase("true") || can_like.equals("1")) {
                    v = 1;
                }
                s.updatePostForCommentOrLike(post_id, viewerId, "can_like", v);
            }
            if (can_reshare.length() > 0) {
                int v = 0;
                if (rec.getString("can_reshare").equalsIgnoreCase("true") || can_reshare.equals("1")) {
                    v = 1;
                }
                s.updatePostForCommentOrLike(post_id, viewerId, "can_reshare", v);
            }

            return true;
        } finally {
            closeTransceiver(transs);
        }
    }

    public int getLikeCount(int objectType, String target) throws AvroRemoteException {
        String targetObjectId = Constants.objectId(objectType, target);
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like like = getProxy(Like.class, trans);
            return like.getLikeCount(targetObjectId);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet likedUsers(String userId, int objectType, String target, String cols, int page, int count) throws AvroRemoteException {

        String targetObjectId = Constants.objectId(objectType, target);
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like like = getProxy(Like.class, trans);
            if (cols.isEmpty() || cols.equals("")) {
                cols = USER_STANDARD_COLUMNS;
            }
            RecordSet recs = RecordSet.fromByteBuffer(like.loadLikedUsers(targetObjectId, page, count));
            return getUsers(userId, recs.joinColumnValues("liker", ","), cols, false);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet loadLikedUsers(String targetObjectId, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like like = getProxy(Like.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(like.loadLikedUsers(targetObjectId, page, count));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet likedUsers(int objectType, String target, int page, int count) throws AvroRemoteException {
        String targetObjectId = Constants.objectId(objectType, target);
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like like = getProxy(Like.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(like.loadLikedUsers(targetObjectId, page, count));
            RecordSet result = new RecordSet();
            for (Record rec : recs) {
                String userId = rec.getString("liker");
                String displayName = getUser(userId, userId, "display_name").getString("display_name", "");
                result.add(Record.of("user_id", userId, "display_name", displayName));
            }
            return result;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean ifuserLiked(String userId, String targetId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like like = getProxy(Like.class, trans);
            return like.ifUserLiked(userId, targetId);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getLikedPostTarget(String userId, int objectType, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like like = getProxy(Like.class, trans);
            return RecordSet.fromByteBuffer(like.getLikedPost(userId, page, count, objectType));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getLikedPosts(String userId, String cols, int objectType, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Like.class);
        try {
            Like like = getProxy(Like.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(like.getLikedPost(userId, page, count, objectType));

            for (Record rec : recs) {
                rec.put("post_id", rec.getString("target").replace(toStr(objectType) + ":", ""));
            }
            if (cols.isEmpty() || cols.equals("")) {
                cols = POST_FULL_COLUMNS;
            }
            return getPosts(recs.joinColumnValues("post_id", ","), cols);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet createSocialContacts(String userId, String updateInfo, String ua, String loc) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        try {
            SocialContacts s = getProxy(SocialContacts.class, trans);
            L.debug("===0 upload socialcontact,updateInfo=:" + updateInfo);
            updateInfo = new String(Encoders.fromBase64(updateInfo));
            L.debug("===0 upload socialcontact,updateInfo from base64=:" + updateInfo);
            RecordSet recs = RecordSet.fromJson(updateInfo);
            boolean wantAdd = true;
            Record setting = getPreferencesByUsers("socialcontact.autoaddfriend", userId);
            //Record setting = Record.of(c1, v1, c2, v2, c3, v3, c4, v4, c5, v5)
            RecordSet return_rs = new RecordSet();
            L.debug("===0 upload socialcontact:" + recs.toString(false, false));
            boolean b = true;
            List<String> ul = new ArrayList<String>();
            for (Record rec : recs) {
                //parse user info from upload
                String username = rec.getString("username");
                int type = (int) rec.getInt("type");
                String content = rec.getString("content");
                //if in user info
                String user_id = findUserIdByUserName(content);
                //if exist ,add friend,insert into db
                if (!user_id.equals("0") && !user_id.equals(userId)) {
                    Record return_r = new Record();
                    return_r.put("contact_id", rec.getString("contact_id"));
                    return_r.put("user_id", user_id);
                    return_r.put("username", username);
                    return_r.put("type", type);
                    return_r.put("content", content);

                    boolean isFriend = false;
                    try {
                        isFriend = isFriend(userId, String.valueOf(user_id));
                    } catch (Exception e) {
                        L.debug("isFriend error,userId=:" + userId + ",user_id=" + user_id);
                    }
                    if (!isFriend && (!user_id.equals(userId))) {
                        ul.add(String.valueOf(user_id));
                    }

                    return_r.put("isfriend", isFriend);

                    Record u = getUser(userId, user_id, USER_COLUMNS_SHAK);
                    return_r.put("display_name", u.getString("display_name"));
                    return_r.put("image_url", u.getString("image_url"));
                    return_r.put("remark", u.getString("remark"));
                    return_r.put("perhaps_name", u.getString("perhaps_name"));
                    if(StringUtils.isNotEmpty(u.getString("in_circles")))
                        return_r.put("in_circles", RecordSet.fromJson(u.getString("in_circles")));
                    return_r.put("his_friend", u.getBoolean("his_friend",false));
                    return_r.put("bidi", u.getBoolean("bidi",false));
                    return_rs.add(return_r);
                } //if not exist ,insert into db
                try {
                    b = s.createSocialContacts(userId, username, type, content, String.valueOf(user_id));
                } catch (Exception e) {
                    L.debug("createSocialContacts error,userId=:" + userId + ",username=" + username + ",type=" + type + ",content=" + content + ",user_id=" + user_id);
                }
            }

            //数据库里的标志，0为   允许加好友，1为不许自动加好友，
            int flag = 0;    //现在定的标志，0允许加好友，不发notification   1为不加好友，只发notification，加入people you may know

            //第一次自动加，不给自己发notification
            //以后,加入people you may know，给自己发notification
            if (!setting.isEmpty()) {
                String f = setting.getString(userId);
                if (f.equals("100")) {
                    flag = 0;
                    Record values = Record.of("socialcontact.autoaddfriend", "1");
                    setPreferences(userId, values);
                } else if (f.equals("1")) {
                    flag = 1;
                } else if (f.equals("0")) {
                    flag = 0;
                }
            }

            String ulc = ul.size() > 0 ? StringUtils.join(ul, ",") : "";

            if (flag == 0) {
                if (ul.size() > 0) {
                    setFriends(String.valueOf(userId), ulc, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_SOCIALCONTACT, true, ua, loc);
                    setFriends(String.valueOf(userId), ulc, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_SOCIALCONTACT, true, ua, loc);
                }
            } else if (flag == 1) {
                if (ul.size() > 0) {
                    L.debug("===0 upload socialcontact,ul=:" + StringUtils.join(ul, ","));
                    //add in people you may know
                    //createSuggestUserFromHaveBorqsId(userId);
                    createSuggestUser(userId, StringUtils.join(ul, ","), Integer.valueOf(Constants.FROM_ADDRESS_HAVEBORQSID), "");
                    //send  notification    to myself
                    sendNotification(Constants.NTF_PEOPLE_YOU_MAY_KNOW,
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId, ulc),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(userId)
                    );
                }
            }
            L.debug("===0 upload socialcontact,return_rs=:" + return_rs.toString(false, false));
            return return_rs;
        } catch (Exception e) {
            L.debug("==upload error==" + e);
            return null;
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record findMyAllPhoneBook(String userId, String updateInfo) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        try {
            SocialContacts s = getProxy(SocialContacts.class, trans);
            if (updateInfo.length() > 0) {
                updateInfo = new String(Encoders.fromBase64(updateInfo));
                RecordSet recs = RecordSet.fromJson(updateInfo);

                for (Record rec : recs) {
                    //parse user info from upload
                    String username = rec.getString("username");
                    int type = (int) rec.getInt("type");
                    String content = rec.getString("content");
                    //if in user info
                    String user_id = findUserIdByUserName(content);
                    //if exist ,add friend,insert into db

                    try {
                        s.createSocialContacts(userId, username, type, content, String.valueOf(user_id));
                    } catch (Exception e) {
                        L.debug("createSocialContacts error,userId=:" + userId + ",username=" + username + ",type=" + type + ",content=" + content + ",user_id=" + user_id);
                    }
                }
            }
            RecordSet recs_all = RecordSet.fromByteBuffer(s.getSocialContacts(userId, 0, 0, 1000));
            RecordSet out1 = new RecordSet();
            RecordSet out2 = new RecordSet();
            for (Record rec : recs_all) {
                if (!rec.getString("uid").equals("") && !rec.getString("uid").equals("0")) {
                    out1.add(rec);
                } else {
                    out2.add(rec);
                }
            }
            String uids = out1.joinColumnValues("uid", "");
            RecordSet users = getUsers(userId, uids, USER_COLUMNS_SHAK);

            Record out_all = new Record();
            out_all.put("in_borqs", users);
            out_all.put("social_contacts", out2);
            return out_all;
        } catch (Exception e) {
            L.debug("==upload error==" + e);
            return null;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean autoCreateSuggestusers(String userId) throws AvroRemoteException {

        //2,send stream many 10
        Transceiver trans = getTransceiver(Stream.class);
        Stream stream = getProxy(Stream.class, trans);
        RecordSet stream_user = RecordSet.fromByteBuffer(stream.topSendStreamUser(10));
        String sUserids = "";
        if (stream_user.size() > 0)
            sUserids = stream_user.joinColumnValues("source", ",");

        //3,shared apps many 10
        Transceiver tranq = getTransceiver(QiupuInterface.class);
        QiupuInterface qp = getProxy(QiupuInterface.class, tranq);
        RecordSet qiupu_user = RecordSet.fromByteBuffer(qp.getStrongMan("", 0, 10));
        String qUserids = "";
        if (qiupu_user.size() > 0)
            qUserids = stream_user.joinColumnValues("user", ",");

        //4,many followers 10
        Transceiver tranf = getTransceiver(Friendship.class);
        Friendship fs = getProxy(Friendship.class, tranf);
        RecordSet fs_user = RecordSet.fromByteBuffer(fs.topuserFollowers(Long.parseLong(userId), 10));
        String fUserids = "";
        if (fs_user.size() > 0)
            fUserids = stream_user.joinColumnValues("friend", ",");


        //5,same company  5


        //6,same school  5


        //7,same sex  5


        //merge userid
        List<String> out_list = new ArrayList<String>();
        String userIds0 = "";
        if (sUserids.length() > 0)
            userIds0 = sUserids + ",";
        if (qUserids.length() > 0)
            userIds0 += qUserids + ",";
        if (fUserids.length() > 0)
            userIds0 += fUserids + ",";

        if (userIds0.length() >= 2) {
            List<String> l0 = StringUtils2.splitList(toStr(userIds0), ",", true);
            for (String u : l0) {
                if (!out_list.contains(u) && !u.equals("")) {
                    out_list.add(u);
                }
            }

            String userIds = "";
            for (String u : out_list) {
                if (!u.equals(userId))
                    userIds += u + ",";
            }
            createSuggestUser(userId, StringUtils.substringBeforeLast(userIds, ","), Integer.valueOf(Constants.FROM_SYSTEM), "");
        }
        return true;
    }


    //createSuggestUser
    public boolean createSuggestUserFromHaveBorqsId(String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        try {
            SocialContacts s = getProxy(SocialContacts.class, trans);
            boolean b = false;
            //get info from socialContact，insert into suggested_user
            RecordSet recs = RecordSet.fromByteBuffer(s.getSocialContactsUid(userId));
            String uids = recs.joinColumnValues("uid", ",");
            if (uids.length() > 0) {
                createSuggestUser(userId, String.valueOf(uids), Integer.valueOf(Constants.FROM_ADDRESS_HAVEBORQSID), "");
            }
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    //createSuggestUser
    public boolean createSuggestUserFromHaveCommLXR(String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        try {
            SocialContacts s = getProxy(SocialContacts.class, trans);
            boolean b = false;
            //get info from socialContact，insert into suggested_user
            RecordSet recs = RecordSet.fromByteBuffer(s.getCommSocialContactsM(userId));
            for (Record r : recs) {
                String uid = r.getString("owner");
                RecordSet bo = RecordSet.fromByteBuffer(s.getCommSocialContactsU(userId, uid));
                String bothF = bo.joinColumnValues("uid", ",");
                if (bothF.length() > 0) {
                    createSuggestUser(userId, uid, Integer.valueOf(Constants.FROM_ADDRESS_HAVECOMMONBORQSID), bothF);
                }
            }
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean createSuggestUserFromHaveCommLXR0(String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        try {
            SocialContacts s = getProxy(SocialContacts.class, trans);
            boolean b = false;
            //get info from socialContact，insert into suggested_user
            RecordSet recs = RecordSet.fromByteBuffer(s.getCommSocialContactsM(userId));
            if(recs.size()==0)
                return true;
            String owner = recs.joinColumnValues("owner",",");
            RecordSet rs = RecordSet.fromByteBuffer(s.getCommSocialContactsByUid(userId,owner));

            Map<String,String> map = new HashMap<String,String>();
            for(Record r :rs){
                String owner0 = r.getString("owner");
                String uid = r.getString("uid");
                if(map.get(owner0)==null)
                    map.put(owner0,uid);
                else
                    map.put(owner0,map.get(owner0)+","+uid);
            }

            for(Map.Entry<String,String> entry:map.entrySet()){
                createSuggestUser(userId, entry.getKey(), Integer.valueOf(Constants.FROM_ADDRESS_HAVECOMMONBORQSID), entry.getValue());
            }

            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    //createSuggestUser
    public RecordSet getWhoHasMyContacts(String userId, String email, String phone) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SocialContacts.class);
        try {
            SocialContacts s = getProxy(SocialContacts.class, trans);
            return RecordSet.fromByteBuffer(s.getWhohasMyContacts(userId, email, phone));
        } finally {
            closeTransceiver(trans);
        }
    }

    //createSuggestUser
    public boolean createSuggestUserFromCommonFriends(String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SuggestedUser.class);
        Transceiver tranf = getTransceiver(Friendship.class);
        try {
            SuggestedUser s = getProxy(SuggestedUser.class, trans);
            Friendship f = getProxy(Friendship.class, tranf);
            boolean b = false;
            //get from friend，insert into suggested_user
            RecordSet recs = RecordSet.fromByteBuffer(s.getSuggestFromBothFriend(userId));

            for (Record r : recs) {
                String uid = r.getString("user");
                RecordSet bo = RecordSet.fromByteBuffer(f.getBothFriendsIds(userId, uid, 0, 200));
                String bothF = bo.joinColumnValues("friend", ",");
                if (bothF.length() > 0) {
                    createSuggestUser(userId, uid, Integer.valueOf(Constants.IN_COMMON_FRIENDS), bothF);
                }
            }
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    //the same school
    public boolean createSuggestUserFromSameSchool(String userId, Map<String, List<String>> map) throws AvroRemoteException {
        if (map == null)
            return true;

        Transceiver trans = getTransceiver(SuggestedUser.class);
        Transceiver tranAccont = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, tranAccont);
            RecordSet users = RecordSet.fromByteBuffer(account.getUsers(userId, "education_history"));
            if (users.size() == 0) {
                return true;
            }

            Record r = users.getFirstRecord();
            JsonNode jn = r.toJsonNode();
            JsonNode schools = jn.get("education_history");
            List<String> schoolNames = schools.findValuesAsText("school");

            Set<String> set = new TreeSet<String>();
            set.addAll(schoolNames);

            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if(entry.getKey().equals(r.getString("user_id")))
                    continue;

                List listEntry = entry.getValue();
                for (String s : set) {
                    if (StringUtils.isNotEmpty(s)) {
                        if (listEntry.contains(s)){
                            createSuggestUser(userId, entry.getKey(), Integer.valueOf(Constants.FROM_USERPROFILE_EDUINFO), s);
                        }
                    }
                }

            }

            return true;
        } finally {
            closeTransceiver(trans);
        }
    }

    //the same company
    public boolean createSuggestUserFromSameCompany(String userId, Map<String, List<String>> map) throws AvroRemoteException {
        if(map == null)
            return true;
        Transceiver trans = getTransceiver(SuggestedUser.class);
        Transceiver tranAccont = getTransceiver(Account.class);
        try {
            Account account = getProxy(Account.class, tranAccont);
            RecordSet users = RecordSet.fromByteBuffer(account.getUsers(userId, "work_history"));
            if (users.size() == 0) {
                return true;
            }

            Record r = users.getFirstRecord();
            JsonNode jn = r.toJsonNode();


            JsonNode companies = jn.get("work_history");
            List<String> companyNames = companies.findValuesAsText("company");

            Set<String> set = new TreeSet<String>();
            set.addAll(companyNames);

            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (entry.getKey().equals(r.getString("user_id")))
                    continue;

                List listEntry = entry.getValue();
                for (String s : set) {
                    if (StringUtils.isNotEmpty(s)) {
                        if (listEntry.contains(s)) {
                            createSuggestUser(userId, entry.getKey(), Integer.valueOf(Constants.FROM_USERPROFILE_WORKINFO), s);
                        }
                    }
                }

            }

            return true;
        } finally {
            closeTransceiver(trans);
        }
    }
    //createSuggestUser
    public boolean createSuggestUserByHasMyContact(String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(SuggestedUser.class);
        try {
            SuggestedUser s = getProxy(SuggestedUser.class, trans);
            boolean b = false;
            //get from friend，insert into suggested_user
            RecordSet recs = RecordSet.fromByteBuffer(s.getSuggestFromHasMyContactinfo(userId));
            String uids = recs.joinColumnValues("user", ",");
            if (uids.length() > 0) {
                createSuggestUser(userId, String.valueOf(uids), Integer.valueOf(Constants.FROM_ADDRESS_HASMYCONTACTINFO), "");
            }
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean recommendUser(String whoSuggest, String toUserId, String beSuggestedUserIds) throws AvroRemoteException {
        try {
            String srcName = getUser(whoSuggest, whoSuggest, "display_name")
                    .getString("display_name", "您的朋友");

            boolean b = false;
            List<String> uIds = StringUtils2.splitList(toStr(beSuggestedUserIds), ",", true);
            for (String uid : uIds) {
                b = createSuggestUser(toUserId, uid, Integer.valueOf(Constants.RECOMMENDER_USER), whoSuggest);
            }

            String beSuggested = uIds.get(0);
            String beSuggestedName = getUser(beSuggested, beSuggested, "display_name")
                    .getString("display_name", "");

            sendNotification(Constants.NTF_SUGGEST_USER,
                    createArrayNodeFromStrings(),
                    createArrayNodeFromStrings(whoSuggest),
                    createArrayNodeFromStrings(srcName, beSuggestedName, String.valueOf(uIds.size())),
                    createArrayNodeFromStrings(),
                    createArrayNodeFromStrings(),
                    createArrayNodeFromStrings(),
                    createArrayNodeFromStrings(whoSuggest, srcName, beSuggested, beSuggestedName, String.valueOf(uIds.size())),
                    createArrayNodeFromStrings(),
                    createArrayNodeFromStrings(),
                    createArrayNodeFromStrings(),
                    createArrayNodeFromStrings(toUserId)
            );

            return b;
        } finally {
        }
    }

    //============================================file upload begin===========================
        public static String utf8Togb2312(String str){

        StringBuffer sb = new StringBuffer();

        for ( int i=0; i<str.length(); i++) {

            char c = str.charAt(i);
            switch (c) {
               case '+' :
                   sb.append( ' ' );
               break ;
               case '%' :
                   try {
                        sb.append(( char )Integer.parseInt (
                        str.substring(i+1,i+3),16));
                   }
                   catch (NumberFormatException e) {
                       throw new IllegalArgumentException();
                  }

                  i += 2;

                  break ;

               default :

                  sb.append(c);

                  break ;

             }

        }

        String result = sb.toString();

        String res= null ;

        try {

             byte [] inputBytes = result.getBytes( "8859_1" );

            res= new String(inputBytes, "UTF-8" );

        }

        catch (Exception e){}

        return res;

  }

//
//    public boolean saveVideo(String viewerId, Record video0) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(Video.class);
//        try {
//            Video video = getProxy(Video.class, trans);
//            boolean b = video.saveVideo(video0.toByteBuffer());
//            return b;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public RecordSet getUserVideo(String viewerId, boolean asc, int page, int count) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(Video.class);
//        try {
//            Video video = getProxy(Video.class, trans);
//            RecordSet recs = RecordSet.fromByteBuffer(video.getVideo(viewerId, asc, page, count));
//            return recs;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public Record getVideoById(String video_id) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(Video.class);
//        try {
//            Video video = getProxy(Video.class, trans);
//            Record rec = Record.fromByteBuffer(video.getVideoById(video_id));
//            return rec;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public boolean deleteVideo(String video_ids) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(Video.class);
//        try {
//            Video video = getProxy(Video.class, trans);
//            boolean b = video.deleteVideo(video_ids);
//            return b;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public boolean saveAudio(String viewerId, Record audio0) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(Audio.class);
//        try {
//            Audio audio = getProxy(Audio.class, trans);
//            boolean b = audio.saveAudio(audio0.toByteBuffer());
//            return b;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public RecordSet getUserAudio(String viewerId, boolean asc, int page, int count) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(Audio.class);
//        try {
//            Audio audio = getProxy(Audio.class, trans);
//            RecordSet recs = RecordSet.fromByteBuffer(audio.getAudio(viewerId, asc, page, count));
//            return recs;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public Record getAudioById(String audio_id) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(Audio.class);
//        try {
//            Audio audio = getProxy(Audio.class, trans);
//            Record rec = Record.fromByteBuffer(audio.getAudioById(audio_id));
//            return rec;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public boolean deleteAudio(String audio_ids) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(Audio.class);
//        try {
//            Audio audio = getProxy(Audio.class, trans);
//            boolean b = audio.deleteAudio(audio_ids);
//            return b;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public boolean saveStaticFile(String viewerId, Record fileRecord) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(StaticFile.class);
//        try {
//            StaticFile file = getProxy(StaticFile.class, trans);
//            boolean b = file.saveStaticFile(fileRecord.toByteBuffer());
//            return b;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public RecordSet getUserStaticFile(String viewerId, boolean asc, int page, int count) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(StaticFile.class);
//        try {
//            StaticFile staticFile = getProxy(StaticFile.class, trans);
//            RecordSet recs = RecordSet.fromByteBuffer(staticFile.getStaticFile(viewerId, asc, page, count));
//            return recs;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public Record getStaticFileById(String file_id) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(StaticFile.class);
//        try {
//            StaticFile staticFile = getProxy(StaticFile.class, trans);
//            Record rec = Record.fromByteBuffer(staticFile.getStaticFileById(file_id));
//            return rec;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public boolean deleteStaticFile(String file_ids) throws AvroRemoteException {
//        Transceiver trans = getTransceiver(StaticFile.class);
//        try {
//            StaticFile staticFile = getProxy(StaticFile.class, trans);
//            boolean b = staticFile.deleteStaticFile(file_ids);
//            return b;
//        } finally {
//            closeTransceiver(trans);
//        }
//    }
//
//    public boolean deleteMyFile(String file_type, String file_ids) throws AvroRemoteException {
//        boolean b = false;
//        if (file_type.equalsIgnoreCase("video")) {
//            b = deleteVideo(file_ids);
//        } else if (file_type.equalsIgnoreCase("audio")) {
//            b = deleteAudio(file_ids);
//        } else {
//            b = deleteStaticFile(file_ids);
//        }
//        return b;
//    }
//
//    public RecordSet fileGetMyShare(String viewerid,String file_type,boolean asc,int page,int count) throws AvroRemoteException {
//        RecordSet recs = new RecordSet()  ;
//        if (file_type.equalsIgnoreCase("video")) {
//            recs =  getUserVideo(viewerid, asc, page, count);
//        } else if (file_type.equalsIgnoreCase("audio")) {
//            recs =  getUserAudio(viewerid, asc, page, count);
//        } else {
//            recs =  getUserStaticFile(viewerid, asc, page, count);
//        }
//        return recs;
//    }
     //============================================file upload end===========================

     //===================user configration  begin==============================

     public boolean saveConfigration(String viewerId, Record configration) throws AvroRemoteException {
        Transceiver trans = getTransceiver(UserConfigration.class);
        try {
            UserConfigration uc = getProxy(UserConfigration.class, trans);
            configration.put("created_time",DateUtils.nowMillis());
            boolean b = uc.saveConfigration(configration.toByteBuffer());
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getConfigration(String viewerId,String key, int version_code) throws AvroRemoteException {
        Transceiver trans = getTransceiver(UserConfigration.class);
        try {
            UserConfigration uc = getProxy(UserConfigration.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(uc.getConfigration(viewerId,key,version_code));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getUserConfigration(String viewerId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(UserConfigration.class);
        try {
            UserConfigration uc = getProxy(UserConfigration.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(uc.getUserConfigration(viewerId));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean deleteConfigration(String viewerId,String key, int version_code) throws AvroRemoteException {
        Transceiver trans = getTransceiver(UserConfigration.class);
        try {
            UserConfigration uc = getProxy(UserConfigration.class, trans);
            boolean b = uc.deleteConfigration(viewerId,key,version_code);
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

     //===================user configration  end==============================
    //from qiupu，want to update
    //================================================================================================
    public RecordSet thisTrandsGetApkInfo(String viewerId, String apps, String cols, int minSDK) throws AvroRemoteException {
        Transceiver tranq = getTransceiver(QiupuInterface.class);
        QiupuInterface qp = getProxy(QiupuInterface.class, tranq);
        RecordSet r = RecordSet.fromByteBuffer(qp.getApps(apps, removeExtenderColumnQiupu(cols), minSDK));
        return r.size() > 0 ? transDs(viewerId, r, cols) : new RecordSet();
    }

    public RecordSet thisTrandsGetSingleApkInfo(String viewerId, String packageName, String cols, int minSDK) throws AvroRemoteException {
        Transceiver tranq = getTransceiver(QiupuInterface.class);
        QiupuInterface qp = getProxy(QiupuInterface.class, tranq);
        RecordSet r = RecordSet.fromByteBuffer(qp.getSingleApps(packageName, removeExtenderColumnQiupu(cols), minSDK));
        return r.size() > 0 ? transDs(viewerId, r, cols) : new RecordSet();
    }

    private String removeExtenderColumnQiupu(String cols) {
        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        l.remove("app_comment_count");
        l.remove("app_comments");
        l.remove("app_like_count");
        l.remove("app_liked_users");
        l.remove("app_likes");
        l.remove("compatibility");
        l.remove("app_used");
        l.remove("app_favorite");
        l.remove("app_installing");
        l.remove("app_installed");
        l.remove("app_uninstalled");
        l.remove("app_downloaded");
        l.remove("lasted_version_code");
        l.remove("lasted_version_name");
        String newCols = StringUtils.join(l, ",").toString();
        return newCols;
    }

    public static final int REASON_INSTALLING = 1;
    public static final int REASON_INSTALLED = 1 << 1;
    public static final int REASON_UNINSTALLED = 1 << 2;
    public static final int REASON_FAVORITE = 1 << 3;
    public static final int REASON_DOWNLOADED = 1 << 4;
    public static final int REASON_UPLOADED = 1 << 5;

    private RecordSet transDs(String viewerId, RecordSet ds, String cols) throws AvroRemoteException {
        Transceiver transc = getTransceiver(Comment.class);
        Comment c = getProxy(Comment.class, transc);

        Transceiver transa = getTransceiver(Account.class);
        Account ac = getProxy(Account.class, transa);

        Transceiver transl = getTransceiver(Like.class);
        Like lk = getProxy(Like.class, transl);

        Transceiver trans = getTransceiver(QiupuInterface.class);
        QiupuInterface qp = getProxy(QiupuInterface.class, trans);

        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        if (ds.size() > 0) {
            for (Record rec : ds) {
                //transcat apkid
                String apk_id = rec.getString("apk_id");
                if (apk_id.length() > 0) {
                    String[] ss = StringUtils.split(StringUtils.trimToEmpty(apk_id), '-');
                    String package_ = ss[0].trim();
                    String versionCode_ = ss[1].trim();
                    String arch_ = ss[2].trim();
                    //if (!arch_.equals("")) {
                    //    arch_ = ARCHS.getText(Integer.valueOf(arch_));
                    //}
                    rec.put("apk_id", package_ + "-" + versionCode_ + "-" + arch_);
                    String targetObjectId = Constants.objectId(Constants.APK_OBJECT, rec.getString("apk_id"));

                    if (l.contains("upload_user")) {
                        if (rec.getInt("upload_user") > 0) {
                            rec.put("upload_user", RecordSet.fromByteBuffer(ac.getUsers(toStr(rec.getInt("upload_user")), "user_id, display_name, image_url, address,remark,perhaps_name")).getFirstRecord());
                        } else {
                            rec.put("upload_user", new RecordSet());
                        }
                    }

                    if (l.contains("app_comment_count")) {
                        rec.put("app_comment_count", c.getCommentCount(viewerId, targetObjectId));
                    }
                    if (l.contains("app_comments")) {
                        String comment_cols = "comment_id, target, created_time,commenter, commenter_name, message, device, can_like,destroyed_time";
                        RecordSet ds1 = getCommentsForContainsIgnore(viewerId, Constants.APK_OBJECT, rec.getString("apk_id"), comment_cols, false, 0, 2);
                        rec.put("app_comments", ds1);
                    }
                    if (l.contains("app_like_count")) {
                        rec.put("app_like_count", lk.getLikeCount(targetObjectId));
                    }
                    if (l.contains("app_liked_users")) {
                        RecordSet u = RecordSet.fromByteBuffer(lk.loadLikedUsers(targetObjectId, 0, 5));
                        rec.put("app_liked_users", getUsers(viewerId, u.joinColumnValues("liker", ","), USER_LIGHT_COLUMNS_QIUPU));
                    }
                    if (l.contains("app_likes")) {
                        String targetId = Constants.objectId(Constants.APK_OBJECT, apk_id);
                        rec.put("app_likes", viewerId.equals("") ? false : lk.ifUserLiked(viewerId, targetId));
                    }
                    if (l.contains("compatibility")) {
                        rec.put("compatibility", true);
                    }

                    if (l.contains("app_used")) {
                        rec.put("app_used", viewerId.equals("") ? false : qp.existUserLinkedApp(viewerId, rec.getString("package"), ""));
                    }

                    if (l.contains("app_favorite") || l.contains("app_installing") || l.contains("app_installed") || l.contains("app_uninstalled") || l.contains("app_downloaded")) {
                        if (viewerId.equals("")) {
                            rec.put("app_favorite", false);
                            rec.put("app_installing", false);
                            rec.put("app_installed", false);
                            rec.put("app_uninstalled", false);
                            rec.put("app_downloaded", false);
                        } else {
                            int reason = qp.getReasonFromApp(viewerId, rec.getString("package"));
                            rec.put("app_favorite", (reason & REASON_FAVORITE) != 0);
                            rec.put("app_installing", (reason & REASON_INSTALLING) != 0);
                            rec.put("app_installed", (reason & REASON_INSTALLED) != 0);
                            rec.put("app_uninstalled", (reason & REASON_UNINSTALLED) != 0);
                            rec.put("app_downloaded", (reason & REASON_DOWNLOADED) != 0);
                        }
                    }

                    int maxId = qp.getMaxVersionCode(package_, 1000);
                    String apkidTemp = package_ + "-" + String.valueOf(maxId) + "-" + arch_;

                    if (l.contains("lasted_version_code")) {
                        rec.put("lasted_version_code", String.valueOf(maxId));
                    }

                    Record rectemp = Record.fromByteBuffer(qp.getSingleApp(apkidTemp, "version_name", 1000));
                    if (l.contains("lasted_version_name")) {
                        rec.put("lasted_version_name", rectemp.getString("version_name"));
                    }
                }
            }
        }
        return ds;
    }
    //============================================================================================

    // Request
    public String createRequest(String userId, String sourceId, String app, String type, String message, String data, boolean addAddressCircle, String ua, String loc) throws AvroRemoteException {
        if (!hasUser(userId))
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);

        if (!hasUser(sourceId))
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", sourceId);

        Transceiver trans = getTransceiver(Request.class);
        try {
            Request req = getProxy(Request.class, trans);
            //set friends
            if (addAddressCircle) {
                setFriends(sourceId, userId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_MANUALSELECT, true, ua, loc);
            }
            String str = toStr(req.createRequest(userId, sourceId, app, type, message, data, "[]"));

            //notif
            int count = getRequestCount(userId, "0", Constants.REQUEST_PROFILE_ACCESS);
            String srcName = getUser(sourceId, sourceId, "display_name").getString("display_name");

            Set<String> excludeReqs = new HashSet<String>();
            excludeReqs.add(Constants.REQUEST_ADD_FRIEND);
            excludeReqs.add(Constants.REQUEST_FRIEND_FEEDBACK);
            excludeReqs.add(Constants.REQUEST_ATTENTION);
            excludeReqs.add(Constants.REQUEST_PUBLIC_CIRCLE_INVITE);
            excludeReqs.add(Constants.REQUEST_PUBLIC_CIRCLE_JOIN);
            excludeReqs.add(Constants.REQUEST_ACTIVITY_INVITE);
            excludeReqs.add(Constants.REQUEST_ACTIVITY_JOIN);
            excludeReqs.add(Constants.REQUEST_ORGANIZATION_INVITE);
            excludeReqs.add(Constants.REQUEST_ORGANIZATION_JOIN);
            excludeReqs.add(Constants.REQUEST_GENERAL_GROUP_INVITE);
            excludeReqs.add(Constants.REQUEST_GENERAL_GROUP_JOIN);
            excludeReqs.add(Constants.REQUEST_EVENT_INVITE);
            excludeReqs.add(Constants.REQUEST_EVENT_JOIN);

            if (!excludeReqs.contains(type)) {
                sendNotification(Constants.NTF_NEW_REQUEST,
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(sourceId),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(userId),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(type, srcName, String.valueOf(count)),
                        createArrayNodeFromStrings(type, srcName, String.valueOf(count)),
                        createArrayNodeFromStrings(type),
                        createArrayNodeFromStrings(userId)
                );
            }

            return str;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean createRequests(String userIds, String sourceId, String app, String type, String message, String data) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Request.class);
        try {
            Request req = getProxy(Request.class, trans);
            return req.createRequests(userIds, sourceId, app, type, message, data, "[]");
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean destroyRequests(String userId, String requestIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Request.class);
        try {
            Request req = getProxy(Request.class, trans);
            return req.destroyRequests(userId, requestIds);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getRequests(String userId, String app, String type) throws AvroRemoteException {
        if (!hasUser(userId))
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);

        Transceiver trans = getTransceiver(Request.class);
        try {
            Request req = getProxy(Request.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(req.getRequests(userId, app, type));
            List<Long> sourceIds = recs.getIntColumnValues("source");
            RecordSet sourceRecs = getUsers(userId, StringUtils.join(sourceIds, ","), "user_id, display_name, remark, image_url, small_image_url, large_image_url, in_circles, contact_info,perhaps_name");
            sourceRecs.renameColumn("user_id", "uid");
            Map<String, Record> sources = sourceRecs.toRecordMap("uid");
            
            RecordSet rtRecs = new RecordSet();
            ArrayList<String> rmRequests = new ArrayList<String>();
            for (Record rec : recs) {
                String requestId = rec.checkGetString("request_id");
                String sourceId = rec.checkGetString("source");
                Record sourceRec = sources.get(sourceId);
                rec.put("source", sourceRec == null || sourceRec.isEmpty() ? JsonNodeFactory.instance.objectNode() : sourceRec.toJsonNode());
                if (sourceRec != null && !sourceRec.isEmpty()){
                    rtRecs.add(rec);
                }
                else {
                    rmRequests.add(requestId);
                }
            }

            destroyRequests(userId, StringUtils2.joinIgnoreBlank(",", rmRequests));

            return rtRecs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean isGroupInviteRequest(String type) {
        String[] types = new String[] {REQUEST_PUBLIC_CIRCLE_INVITE, REQUEST_ACTIVITY_INVITE, REQUEST_ORGANIZATION_INVITE,
                                    REQUEST_GENERAL_GROUP_INVITE, REQUEST_EVENT_INVITE};
        return ArrayUtils.contains(types, type);
    }

    public boolean isGroupJoinRequest(String type) {
        String[] types = new String[] {REQUEST_PUBLIC_CIRCLE_JOIN, REQUEST_ACTIVITY_JOIN, REQUEST_ORGANIZATION_JOIN,
                REQUEST_GENERAL_GROUP_JOIN, REQUEST_EVENT_JOIN};
        return ArrayUtils.contains(types, type);
    }

    public boolean doneRequests(String userId, String requestIds, String type, String data, boolean accept) throws AvroRemoteException {
        L.trace("[Method doneRequests] requestIds: " + requestIds);
        L.trace("[Method doneRequests] type: " + type);
        L.trace("[Method doneRequests] data: " + data);
        L.trace("[Method doneRequests] accept: " + accept);

        if (!hasUser(userId))
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);

        Transceiver trans0 = getTransceiver(Group.class);
        Transceiver trans = getTransceiver(Request.class);
        try {
            Group group = getProxy(Group.class, trans0);
            Request req = getProxy(Request.class, trans);
            if (isGroupInviteRequest(type)) {
                JsonNode jn = JsonUtils.parse(data);
                long groupId = jn.get("group_id").getLongValue();
                if (accept) {
                    addMembers(groupId, Record.of(userId, ROLE_MEMBER), true);
                    requestIds = toStr(req.getRelatedRequestIds("0", data));
                } else {
                    Record statusRec = new Record();
                    statusRec.put("user_id", Long.parseLong(userId));
                    statusRec.put("display_name", getUser(userId, userId, "display_name").getString("display_name"));
                    statusRec.put("identify", "");
                    statusRec.put("source", 0);
                    statusRec.put("status", STATUS_REJECTED);
                    group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
                }
            } else if (isGroupJoinRequest(type)) {
                JsonNode jn = JsonUtils.parse(data);
                String joiner = jn.get("user_id").getTextValue();
                String joinerName = jn.get("user_name").getTextValue();
                long groupId = jn.get("group_id").getLongValue();
                if (accept) {
                    addMembers(groupId, Record.of(joiner, ROLE_MEMBER), true);
                    requestIds = toStr(req.getRelatedRequestIds(joiner, data));
                } else {
                    Record statusRec = new Record();
                    statusRec.put("user_id", Long.parseLong(joiner));
                    statusRec.put("display_name", joinerName);
                    statusRec.put("identify", "");
                    statusRec.put("source", 0);
                    statusRec.put("status", STATUS_REJECTED);
                    group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
                }
            }
            L.trace("[Method doneRequests] before doneRequest requestIds: " + requestIds);

            return req.doneRequest(userId, requestIds);
        } finally {
            closeTransceiver(trans0);
            closeTransceiver(trans);
        }
    }

    public int getRequestCount(String userId, String app, String type) throws AvroRemoteException {
        if (!hasUser(userId))
            throw Errors.createResponseError(ErrorCode.PARAM_ERROR, "User '%s' is not exists", userId);

        Transceiver trans = getTransceiver(Request.class);
        try {
            Request req = getProxy(Request.class, trans);
            return req.getCount(userId, app, type);
        } finally {
            closeTransceiver(trans);
        }
    }

    public void sendFriendFeedbackRequest(String source, String to, boolean addAddreddCircle, String ua, String loc) throws AvroRemoteException {
        if (isHisFriend(to, source))
            createRequest(to, source, "0", Constants.REQUEST_FRIEND_FEEDBACK, "", "", addAddreddCircle, ua, loc);
    }


    public boolean splitUserName(String lang) {
        try {

            Transceiver trans = getTransceiver(Account.class);
            Account a = getProxy(Account.class, trans);
            RecordSet rs = RecordSet.fromByteBuffer(a.findAllUserIds(true));

            NameSplitter nm = new NameSplitter("Mr, Ms, Mrs", "d', st, st., von", "Jr., M.D., MD, D.D.S.",
                    "&, AND", Locale.CHINA);

            for (Record r : rs) {
                final NameSplitter.Name name = new NameSplitter.Name();
                nm.split(name, r.getString("display_name"));

                String first_name = "";
                String middle_name = "";
                String last_name = "";
                if (name.getGivenNames() != null) {
                    first_name = name.getGivenNames().toString();
                }
                if (name.getMiddleName() != null) {
                    middle_name = name.getMiddleName().toString();
                }
                if (name.getFamilyName() != null) {
                    last_name = name.getFamilyName().toString();
                }

                Record rec = new Record();
                rec.put("first_name", last_name);
                rec.put("middle_name", middle_name);
                rec.put("last_name", first_name);

                updateAccount11(r.getString("user_id"), rec, lang,false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //Group
    public long createGroup(long begin, String type, String name, int memberLimit, int isStreamPublic, int canSearch, int canViewMembers, int canJoin,
                            int canMemberInvite, int canMemberApprove, int canMemberPost, int canMemberQuit, int needInvitedConfirm, long creator, String label, Record properties,
                            String viewerId, String ua, String loc, String appId) throws AvroRemoteException {
        return createGroup(begin, type, name, memberLimit, isStreamPublic, canSearch, canViewMembers, canJoin, canMemberInvite, canMemberApprove, canMemberPost, canMemberQuit, needInvitedConfirm, creator, label, properties, viewerId, ua, loc, appId, false);
    }


    public long createGroup(long begin, String type, String name, int memberLimit, int isStreamPublic, int canSearch, int canViewMembers, int canJoin,
                            int canMemberInvite, int canMemberApprove, int canMemberPost, int canMemberQuit, int needInvitedConfirm, long creator, String label, Record properties,
                            String viewerId, String ua, String loc, String appId, boolean autoPost) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            long groupId = group.createGroup(begin, type, name, memberLimit, isStreamPublic, canSearch, canViewMembers, canJoin,
                    canMemberInvite, canMemberApprove, canMemberPost, canMemberQuit, needInvitedConfirm, creator, label, properties.toByteBuffer());

            if (autoPost) {
                //send a post
                String m = "";
                String tempNowAttachments = "[]";

                String groupType = getGroupTypeStr(groupId, ua);
                String template = Constants.getBundleString(ua, "platform.create.group.message");
                String groupSchema = "<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + name + "</a>";
                String message = SQLTemplate.merge(template, new Object[][]{
                        {"groupType", groupType},
                        {"groupName", groupSchema}
                });
                boolean secretly = false;
                if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
                    secretly = true;

                autoPost(viewerId, Constants.TEXT_POST, message, tempNowAttachments, appId, "", "", m, String.valueOf(groupId), secretly, "", ua, loc, true, true, true, "", "", false);
            }
            return groupId;
        } catch (ResponseError e) {
            throw new ServerException(e.code, toStr(e.message));
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateGroup(String userId, long groupId, Record info, Record properties) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            if (group.hasRight(groupId, Long.parseLong(userId), Constants.ROLE_ADMIN))
                return group.updateGroup(groupId, info.toByteBuffer(), properties.toByteBuffer());
            else
                return false;
        } catch (ResponseError e) {
            throw new ServerException(e.code, toStr(e.message));
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean destroyGroup(String userId, String groupIds) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            long[] gids = StringUtils2.splitIntArray(groupIds, ",");
            if (group.hasRight(gids[0], Long.parseLong(userId), Constants.ROLE_ADMIN)) {
                boolean r = group.destroyGroup(groupIds);
                List<String> l = StringUtils2.splitList(groupIds, ",", true);
                for (String groupId : l)
                    dealRelatedRequests("0", "0", groupId);
                return r;
            }
            else
                return false;
        } catch (ResponseError e) {
            throw new ServerException(e.code, toStr(e.message));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getUsersCounts(String groupIds, int status) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            Record counts = new Record();
            if (StringUtils.isNotBlank(groupIds))
                counts = Record.fromByteBuffer(group.getUsersCounts(groupIds, status));

            return counts;
        } finally {
            closeTransceiver(trans);
        }
    }

    private Record additionalGroupInfo(Group group, String userId, Record rec) throws AvroRemoteException {
        long groupId = rec.getInt(GRP_COL_ID, 0);
        int memberCount = group.getMembersCount(groupId);

//            String creator = String.valueOf(rec.getInt("creator", 0));
            String creator = String.valueOf(group.getCreator(groupId));
            String admins = toStr(group.getAdmins(groupId, -1, -1));
            rec.put("admins", admins);

            int adminCount = StringUtils2.splitList(admins, ",", true).size();
            if (!StringUtils.equals(creator, "0"))
                adminCount++;

            long uid = 0;
            try {
                uid = Long.parseLong(userId);
            } catch (Exception e) {
                uid = 0;
            }
            int role = group.hasRight(groupId, uid, ROLE_MEMBER) ? ROLE_MEMBER : ROLE_GUEST;
            boolean can_update = false;
            boolean can_destroy = false;
            boolean can_remove = false;
            boolean can_grant = false;
            boolean can_quit = rec.getInt(Constants.GRP_COL_CAN_MEMBER_QUIT, 1) == 1 ? true : false;

            if (StringUtils.equals(creator, userId)) {
                role = ROLE_CREATOR;
                can_update = true;
                can_destroy = true;
                can_remove = true;
                can_grant = true;

                if ((adminCount < 2) && (memberCount > 1))
                    can_quit = false;
            }
            else if (StringUtils.contains(admins, userId)) {
                role = ROLE_ADMIN;
                can_update = true;
                can_destroy = true;
                can_remove = true;
                can_grant = true;

                if ((adminCount < 2) && (memberCount > 1))
                    can_quit = false;
            }

            rec.put("role_in_group", role);
            rec.put("viewer_can_update", can_update);
            rec.put("viewer_can_destroy", can_destroy);
            rec.put("viewer_can_remove", can_remove);
            rec.put("viewer_can_grant", can_grant);
            rec.put("viewer_can_quit", can_quit);

            rec.put("member_count", memberCount);

            rec.putMissing(COMM_COL_DESCRIPTION, "");
            rec.putMissing(COMM_COL_COMPANY, "");
            rec.putMissing(COMM_COL_DEPARTMENT, "");
            rec.putMissing(COMM_COL_WEBSITE, "");
            rec.putMissing(COMM_COL_BULLETIN, "");
            rec.putMissing(COMM_COL_BULLETIN_UPDATED_TIME, 0);


            long themeId = rec.getInt(COMM_COL_THEME_ID, 0);
            if (themeId != 0) {
                Map<Long, Record> themeRecs = getEventThemes(themeId);
                if (MapUtils.isNotEmpty(themeRecs)) {
                    Record themeRec = themeRecs.values().iterator().next();
                    rec.put(COMM_COL_THEME_NAME, ObjectUtils.toString(themeRec.getString("name")));
                    rec.put(COMM_COL_THEME_IMAGE, ObjectUtils.toString(themeRec.getString("image_url")));
                }
            } else {
                rec.put(COMM_COL_THEME_ID, 0L);
                rec.put(COMM_COL_THEME_NAME, "");
                rec.put(COMM_COL_THEME_IMAGE, "");
            }

            if (rec.has(COMM_COL_CONTACT_INFO)) {
                Record contactInfo = Record.fromJson(rec.getString(COMM_COL_CONTACT_INFO));
                rec.put(COMM_COL_CONTACT_INFO, contactInfo);
            } else
                rec.put(COMM_COL_CONTACT_INFO, JsonNodeFactory.instance.objectNode());

            if (rec.has(COMM_COL_ADDRESS)) {
                RecordSet address = RecordSet.fromJson(rec.getString(COMM_COL_ADDRESS));
                rec.put(COMM_COL_ADDRESS, address);
            } else
                rec.put(COMM_COL_ADDRESS, JsonNodeFactory.instance.arrayNode());

            String urlPattern = getConfig().getString("platform.profileImagePattern", "");
            if (!rec.has(COMM_COL_IMAGE_URL)) {
                rec.put(COMM_COL_IMAGE_URL, "default_public_circle.png");
                rec.put(COMM_COL_SMALL_IMG_URL, "default_public_circle_S.png");
                rec.put(COMM_COL_LARGE_IMG_URL, "default_public_circle_L.png");
                urlPattern = getConfig().getString("platform.sysIconUrlPattern", "");
            }
            addImageUrlPrefix(urlPattern, rec);

            //shared count
            Record sharedCount = new Record();
            int sharedText = getSharedCount(userId, String.valueOf(groupId), Constants.TEXT_POST);
            sharedCount.put("shared_text", sharedText);
            int sharedPhoto = getSharedCount(userId, String.valueOf(groupId), Constants.PHOTO_POST);
            sharedCount.put("shared_photo", sharedPhoto);
            int sharedBook = getSharedCount(userId, String.valueOf(groupId), Constants.BOOK_POST);
            sharedCount.put("shared_book", sharedBook);
            int sharedApk = getSharedCount(userId, String.valueOf(groupId), Constants.APK_POST);
            sharedCount.put("shared_apk", sharedApk);
            int sharedLink = getSharedCount(userId, String.valueOf(groupId), Constants.LINK_POST);
            sharedCount.put("shared_link", sharedLink);
            int shared_static_file = getSharedCount(userId, String.valueOf(groupId), Constants.FILE_POST);
            sharedCount.put("shared_static_file", shared_static_file);
            int shared_audio = getSharedCount(userId, String.valueOf(groupId), Constants.AUDIO_POST);
            sharedCount.put("shared_audio", shared_audio);
            int shared_video = getSharedCount(userId, String.valueOf(groupId), Constants.VIDEO_POST);
            sharedCount.put("shared_video", shared_video);
            sharedCount.put("shared_poll", getRelatedPollCount(userId, String.valueOf(groupId)));
            rec.put("shared_count", sharedCount);

            //top posts
            String topName = rec.getString(COMM_COL_TOP_NAME, "");
            String topPostIds = rec.getString(COMM_COL_TOP_POSTS, "");
            int topCount = StringUtils2.splitSet(topPostIds, ",", true).size();
//            RecordSet posts = getFullPostsForQiuPu(userId, topPostIds, true);
            RecordSet posts = new RecordSet();
            Record topPosts = new Record();
            topPosts.put("name", topName);
            topPosts.put("count", topCount);
            topPosts.put("posts", posts);
            rec.put(COMM_COL_TOP_POSTS, topPosts);
            rec.remove(COMM_COL_TOP_NAME);

            int privacy = 0;
            long isStreamPublic = rec.getInt(GRP_COL_IS_STREAM_PUBLIC, 1);
            long canSearch = rec.getInt(GRP_COL_CAN_SEARCH, 1);
            long canViewMembers = rec.getInt(GRP_COL_CAN_VIEW_MEMBERS, 1);
            if ((isStreamPublic == 1) && (canSearch == 1) && (canViewMembers == 1))
                privacy = GRP_PRIVACY_OPEN;
            else if ((isStreamPublic == 0) && (canSearch == 1) && (canViewMembers == 1))
                privacy = GRP_PRIVACY_CLOSED;
            else if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
                privacy = GRP_PRIVACY_SECRET;
            else
                privacy = 0;
            rec.put("privacy", privacy);


        return rec;
    }

    private RecordSet additionalGroupsInfo(Group group, String userId, RecordSet recs) throws AvroRemoteException {
        String groupIds = StringUtils2.joinIgnoreBlank(",", recs.getStringColumnValues(GRP_COL_ID));
        Record counts = new Record();
        if (StringUtils.isNotBlank(groupIds))
            counts = Record.fromByteBuffer(group.getMembersCounts(groupIds));

        for (Record rec : recs) {
            long groupId = rec.getInt(Constants.GRP_COL_ID, 0);
//            String creator = String.valueOf(rec.getInt("creator", 0));
/*            String creator = String.valueOf(group.getCreator(groupId));
            String admins = toStr(group.getAdmins(groupId, -1, -1));
            rec.put("admins", admins);

            int adminCount = StringUtils2.splitList(admins, ",", true).size();
            if (!StringUtils.equals(creator, "0"))
                adminCount++;

            long uid = 0;
            try {
                uid = Long.parseLong(userId);
            } catch (Exception e) {
                uid = 0;
            }
            int role = group.hasRight(groupId, uid, ROLE_MEMBER) ? ROLE_MEMBER : ROLE_GUEST;
            boolean can_update = false;
            boolean can_destroy = false;
            boolean can_remove = false;
            boolean can_grant = false;
            boolean can_quit = rec.getInt(Constants.GRP_COL_CAN_MEMBER_QUIT, 1) == 1 ? true : false; */
            long memberCount = counts.getInt(String.valueOf(groupId));
/*
            if (StringUtils.equals(creator, userId)) {
                role = ROLE_CREATOR;
                can_update = true;
                can_destroy = true;
                can_remove = true;
                can_grant = true;

                if ((adminCount < 2) && (memberCount > 1))
                    can_quit = false;
            }
            else if (StringUtils.contains(admins, userId)) {
                role = ROLE_ADMIN;
                can_update = true;
                can_destroy = true;
                can_remove = true;
                can_grant = true;

                if ((adminCount < 2) && (memberCount > 1))
                    can_quit = false;
            }

            rec.put("role_in_group", role);
            rec.put("viewer_can_update", can_update);
            rec.put("viewer_can_destroy", can_destroy);
            rec.put("viewer_can_remove", can_remove);
            rec.put("viewer_can_grant", can_grant);
            rec.put("viewer_can_quit", can_quit);
*/
            rec.put("member_count", memberCount);

            rec.putMissing(COMM_COL_DESCRIPTION, "");
            rec.putMissing(COMM_COL_COMPANY, "");
            rec.putMissing(COMM_COL_DEPARTMENT, "");
            rec.putMissing(COMM_COL_WEBSITE, "");
            rec.putMissing(COMM_COL_BULLETIN, "");
            rec.putMissing(COMM_COL_BULLETIN_UPDATED_TIME, 0);


            long themeId = rec.getInt(COMM_COL_THEME_ID, 0);
            if (themeId != 0) {
                Map<Long, Record> themeRecs = getEventThemes(themeId);
                if (MapUtils.isNotEmpty(themeRecs)) {
                    Record themeRec = themeRecs.values().iterator().next();
                    rec.put(COMM_COL_THEME_NAME, ObjectUtils.toString(themeRec.getString("name")));
                    rec.put(COMM_COL_THEME_IMAGE, ObjectUtils.toString(themeRec.getString("image_url")));
                }
            } else {
                rec.put(COMM_COL_THEME_ID, 0L);
                rec.put(COMM_COL_THEME_NAME, "");
                rec.put(COMM_COL_THEME_IMAGE, "");
            }

            if (rec.has(COMM_COL_CONTACT_INFO)) {
                Record contactInfo = Record.fromJson(rec.getString(COMM_COL_CONTACT_INFO));
                rec.put(COMM_COL_CONTACT_INFO, contactInfo);
            } else
                rec.put(COMM_COL_CONTACT_INFO, JsonNodeFactory.instance.objectNode());

            if (rec.has(COMM_COL_ADDRESS)) {
                RecordSet address = RecordSet.fromJson(rec.getString(COMM_COL_ADDRESS));
                rec.put(COMM_COL_ADDRESS, address);
            } else
                rec.put(COMM_COL_ADDRESS, JsonNodeFactory.instance.arrayNode());

            String urlPattern = getConfig().getString("platform.profileImagePattern", "");
            if (!rec.has(COMM_COL_IMAGE_URL)) {
                rec.put(COMM_COL_IMAGE_URL, "default_public_circle.png");
                rec.put(COMM_COL_SMALL_IMG_URL, "default_public_circle_S.png");
                rec.put(COMM_COL_LARGE_IMG_URL, "default_public_circle_L.png");
                urlPattern = getConfig().getString("platform.sysIconUrlPattern", "");
            }
            addImageUrlPrefix(urlPattern, rec);

            //shared count
/*            Record sharedCount = new Record();
            int sharedText = getSharedCount(userId, String.valueOf(groupId), Constants.TEXT_POST);
            sharedCount.put("shared_text", sharedText);
            int sharedPhoto = getSharedCount(userId, String.valueOf(groupId), Constants.PHOTO_POST);
            sharedCount.put("shared_photo", sharedPhoto);
            int sharedBook = getSharedCount(userId, String.valueOf(groupId), Constants.BOOK_POST);
            sharedCount.put("shared_book", sharedBook);
            int sharedApk = getSharedCount(userId, String.valueOf(groupId), Constants.APK_POST);
            sharedCount.put("shared_apk", sharedApk);
            int sharedLink = getSharedCount(userId, String.valueOf(groupId), Constants.LINK_POST);
            sharedCount.put("shared_link", sharedLink);
            int shared_static_file = getSharedCount(userId, String.valueOf(groupId), Constants.FILE_POST);
            sharedCount.put("shared_static_file", shared_static_file);
            int shared_audio = getSharedCount(userId, String.valueOf(groupId), Constants.AUDIO_POST);
            sharedCount.put("shared_audio", shared_audio);
            int shared_video = getSharedCount(userId, String.valueOf(groupId), Constants.VIDEO_POST);
            sharedCount.put("shared_video", shared_video);
            sharedCount.put("shared_poll", getRelatedPollCount(userId, String.valueOf(groupId)));
            rec.put("shared_count", sharedCount);
*/
            //top posts
            String topName = rec.getString(COMM_COL_TOP_NAME, "");
            String topPostIds = rec.getString(COMM_COL_TOP_POSTS, "");
            int topCount = StringUtils2.splitSet(topPostIds, ",", true).size();
//            RecordSet posts = getFullPostsForQiuPu(userId, topPostIds, true);
            RecordSet posts = new RecordSet();
            Record topPosts = new Record();
            topPosts.put("name", topName);
            topPosts.put("count", topCount);
            topPosts.put("posts", posts);
            rec.put(COMM_COL_TOP_POSTS, topPosts);
            rec.remove(COMM_COL_TOP_NAME);
            
            int privacy = 0;
            long isStreamPublic = rec.getInt(GRP_COL_IS_STREAM_PUBLIC, 1);
            long canSearch = rec.getInt(GRP_COL_CAN_SEARCH, 1);
            long canViewMembers = rec.getInt(GRP_COL_CAN_VIEW_MEMBERS, 1);
            if ((isStreamPublic == 1) && (canSearch == 1) && (canViewMembers == 1))
                privacy = GRP_PRIVACY_OPEN;
            else if ((isStreamPublic == 0) && (canSearch == 1) && (canViewMembers == 1))
                privacy = GRP_PRIVACY_CLOSED;
            else if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
                privacy = GRP_PRIVACY_SECRET;
            else
                privacy = 0;
            rec.put("privacy", privacy);
        }

        return recs;
    }

    public RecordSet dealAccountTopPosts(RecordSet users) throws AvroRemoteException {
        for (Record user : users) {
            if (user.has("top_posts")) {
                String topName = user.getString("top_name", "");
                String topPostIds = user.getString("top_posts", "");
                int topCount = StringUtils2.splitSet(topPostIds, ",", true).size();
//                RecordSet posts = getFullPostsForQiuPu(user.getString("user_id"), topPostIds, true);
                RecordSet posts = new RecordSet();
                Record topPosts = new Record();
                topPosts.put("name", topName);
                topPosts.put("count", topCount);
                topPosts.put("posts", posts);
                user.put("top_posts", topPosts);
            }
            user.remove("top_name");
        }

        return users;
    }
    
    public RecordSet getGroups(long begin, long end, String groupIds, String cols) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            RecordSet recs = new RecordSet();
            if (StringUtils.isNotBlank(groupIds))
                recs = RecordSet.fromByteBuffer(group.getGroups(begin, end, groupIds, cols));
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public <T> T useRawGroup(GroupAccessor<T> accessor) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return accessor.access(group);
        } finally {
            closeTransceiver(trans);
        }
    }

    public static interface GroupAccessor<T> {
        T access(Group g) throws AvroRemoteException;
    }

    public Record getGroup(String userId, long groupId, String cols, boolean withMembers) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            Record rec = Record.fromByteBuffer(group.getGroup(groupId, cols));
            rec = additionalGroupInfo(group, userId, rec);

            if (withMembers) {
                long canViewMembers = rec.getInt(Constants.GRP_COL_CAN_VIEW_MEMBERS, 1);
                if (canViewMembers == 1) {
                    String memberIds = toStr(group.getAllMembers(groupId, -1, -1, ""));
                    RecordSet members = getUsers(userId, memberIds, "user_id, display_name, remark, image_url,perhaps_name");
                    String creator = rec.getString("creator");
                    String admins = rec.getString("admins");
                    for (Record member : members) {
                        String memberId = member.getString("user_id");
                        int memberRole = ROLE_MEMBER;
                        if (StringUtils.equals(creator, memberId))
                            memberRole = ROLE_CREATOR;
                        else if (StringUtils.contains(admins, memberId))
                            memberRole = ROLE_ADMIN;
                        member.put("role_in_group", memberRole);
                    }
                    rec.put(Constants.GRP_COL_MEMBERS, members.toJsonNode());
                }
                else
                    rec.put(Constants.GRP_COL_MEMBERS, JsonNodeFactory.instance.arrayNode());

                Record creator = getUser(userId, String.valueOf(rec.getInt("creator", 0)), "user_id, display_name, remark, image_url,perhaps_name");
                rec.put("creator", creator.toJsonNode());
                rec.remove("admins");
            } else {
                Record creator = getUser(userId, String.valueOf(rec.getInt("creator", 0)), "user_id, display_name, remark, image_url,perhaps_name");
                rec.put("creator", creator.toJsonNode());
                rec.remove("admins");
            }

            return rec;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getGroups(long begin, long end, String userId, String groupIds, String cols, boolean withMembers) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            RecordSet recs;
            if (StringUtils.isBlank(groupIds)) {
                recs = RecordSet.fromByteBuffer(group.findGroupsByMember(begin, end, Long.parseLong(userId), cols));
                groupIds = StringUtils2.joinIgnoreBlank(",", recs.getStringColumnValues(GRP_COL_ID));
            }
            else
                recs = RecordSet.fromByteBuffer(group.getGroups(begin, end, groupIds, cols));

            recs = additionalGroupsInfo(group, userId, recs);

            if (withMembers) {
                for (Record rec : recs) {
                    long groupId = rec.getInt(Constants.GRP_COL_ID, 0);
                    long canViewMembers = rec.getInt(Constants.GRP_COL_CAN_VIEW_MEMBERS, 1);
                    if (canViewMembers == 1) {
                        String memberIds = toStr(group.getAllMembers(groupId, -1, -1, ""));
                        RecordSet members = getUsers(userId, memberIds, "user_id, display_name, remark, image_url,perhaps_name");
                        String creator = rec.getString("creator");
                        String admins = rec.getString("admins");
                        for (Record member : members) {
                            String memberId = member.getString("user_id");
                            int memberRole = ROLE_MEMBER;
                            if (StringUtils.equals(creator, memberId))
                                memberRole = ROLE_CREATOR;
                            else if (StringUtils.contains(admins, memberId))
                                memberRole = ROLE_ADMIN;
                            member.put("role_in_group", memberRole);
                        }
                        rec.put(Constants.GRP_COL_MEMBERS, members.toJsonNode());
                    }
                    else
                        rec.put(Constants.GRP_COL_MEMBERS, JsonNodeFactory.instance.arrayNode());

                    Record creator = getUser(userId, String.valueOf(rec.getInt("creator", 0)), "user_id, display_name, remark, image_url,perhaps_name");
                    rec.put("creator", creator.toJsonNode());
                    rec.remove("admins");
                }
            } else {
                for (Record rec : recs) {
                    Record creator = getUser(userId, String.valueOf(rec.getInt("creator", 0)), "user_id, display_name, remark, image_url,perhaps_name");
                    rec.put("creator", creator.toJsonNode());
                    rec.remove("admins");
                }
            }
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getMemberIds(long groupId, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return toStr(group.getAllMembers(groupId, page, count, ""));
        } finally {
            closeTransceiver(trans);
        }
    }

    private RecordSet getAllMembers(long groupId, int page, String searchKey, List<String> l, Group group, RecordSet recs, RecordSet rs) throws AvroRemoteException {
        // analyze
        List<String> memberList = rs.getStringColumnValues("user_id");
        String memberIds = StringUtils2.joinIgnoreBlank(",", memberList);


        //String memberIds = toStr(group.getAllMembers(groupId, page, count, searchKey));


        RecordSet members = getUsers("", memberIds, "user_id, display_name, remark, image_url,perhaps_name");
        String creator = String.valueOf(group.getCreator(groupId));
        String admins = toStr(group.getAdmins(groupId, -1, -1));
        RecordSet adminRecs = new RecordSet();
        Record creatorRec = getUser("", creator, "user_id, display_name, remark, image_url,perhaps_name");
        creatorRec.put("role_in_group", ROLE_CREATOR);
        creatorRec.put("status", STATUS_JOINED);
        adminRecs.add(0, creatorRec);
        RecordSet adminRecs0 = getUsers("", admins, "user_id, display_name, remark, image_url,perhaps_name");
        for (Record admin : adminRecs0) {
            admin.put("role_in_group", ROLE_ADMIN);
            admin.put("status", STATUS_JOINED);
        }
        adminRecs.addAll(adminRecs0);
        for (Record member : members) {
            String memberId = member.getString("user_id");
            int memberRole = ROLE_MEMBER;
            if (StringUtils.equals(creator, memberId))
                memberRole = ROLE_CREATOR;
            else if (StringUtils.contains(admins, memberId))
                memberRole = ROLE_ADMIN;
            member.put("role_in_group", memberRole);
            member.put("status", STATUS_JOINED);
        }
        recs.addAll(members);
        if (StringUtils.isBlank(searchKey)) {
            recs.removeAll(adminRecs);
            if (page == 0 || page == -1)
                recs.addAll(0, adminRecs);
        }
        l.remove(String.valueOf(STATUS_JOINED));
        return recs;
    }

    private void getPenddingGroupMembers(String viewerId, long groupId, Group group, RecordSet recs, RecordSet recs0) throws AvroRemoteException {
        RecordSet recs1 = new RecordSet(); // borqs id
        RecordSet recs2 = new RecordSet(); // identify
        for (Record rec : recs0) {
            String userId = rec.getString("user_id", "0");
            String identify = rec.getString("identify", "");
            String sourceIds = "";
            if (!StringUtils.equals(userId, "0")) {
                Record user = getUser(viewerId, userId, "user_id, display_name, remark, image_url,perhaps_name");
                rec.putAll(user);

                sourceIds = toStr(group.getSourcesById(groupId, userId));
            } else
                sourceIds = toStr(group.getSourcesByIdentify(groupId, identify));
            rec.put("role_in_group", ROLE_GUEST);
            if (StringUtils.isNotBlank(sourceIds)) {
                RecordSet source = getUsers(viewerId, sourceIds, "user_id, display_name, remark, image_url,perhaps_name");
                rec.put("sources", source.toJsonNode());
            }

            Record r = rec.copy();
            if (!StringUtils.equals(userId, "0"))
                recs1.add(r);
            else
                recs2.add(r);
        }
        recs1.unique("user_id");
        recs2.unique("identify");
        recs.addAll(recs1);
        recs.addAll(recs2);
    }

    /**
     * union 2 tables force to return result
     * @param viewerId
     * @param groupId
     * @param status
     * @param page
     * @param count
     * @param searchKey
     * @return
     * @throws AvroRemoteException
     */
    public RecordSet getGroupUsersByStatus(String viewerId, long groupId, String status, int page, int count, String searchKey) throws AvroRemoteException {
            List<String> l = StringUtils2.splitList(status, ",", true);
            Transceiver trans = getTransceiver(Group.class);

            try {
                Group group = getProxy(Group.class, trans);
                Record group_ = Record.fromByteBuffer(group.getGroup(groupId, GROUP_LIGHT_COLS));
                RecordSet recs = new RecordSet();

                long canViewMembers = group_.getInt(Constants.GRP_COL_CAN_VIEW_MEMBERS, 1);
                if (l.contains(String.valueOf(STATUS_JOINED)) && (canViewMembers == 1 || group.hasRight(groupId, Long.parseLong(viewerId), ROLE_MEMBER))) {
                    l.remove(String.valueOf(STATUS_JOINED));
                    // inject
                    RecordSet rs = RecordSet.fromByteBuffer(group.getGroupUsersByStatus(groupId, status, page, count, searchKey));

                    RecordSet rs0 = new RecordSet();
                    RecordSet rs1 = new RecordSet();
                    //divide the rs into 2 parts
                    for(Record r :rs){
                        String type = r.getString("t");
                        if("group_members".equals(type)){
                            rs0.add(r);
                        }else{
                            rs1.add(r);
                        }
                    }

                    if(rs0.size()>0)
                        getAllMembers(groupId, page, searchKey, l, group, recs, rs0);
                    if(rs1.size()>0 && l.size()>0)
                        getPenddingGroupMembers(viewerId, groupId, group, recs, rs1);

                    return recs;

                } else if (!l.isEmpty()) {
                    l.remove(String.valueOf(STATUS_JOINED));
                    if (!l.isEmpty()) {

                        RecordSet recs0 = RecordSet.fromByteBuffer(group.getPendingUsersByStatus(groupId, 0, StringUtils2.joinIgnoreBlank(",", l), page, count, searchKey));
                        getPenddingGroupMembers(viewerId, groupId, group, recs, recs0);
                        return recs;
                    }
                }
                return recs;

            } finally {
                closeTransceiver(trans);
            }
        }
    public RecordSet getGroupUsersByStatus1(String viewerId, long groupId, String status, int page, int count, String searchKey) throws AvroRemoteException {
        List<String> l = StringUtils2.splitList(status, ",", true);
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            Record group_ = Record.fromByteBuffer(group.getGroup(groupId, GROUP_LIGHT_COLS));
            RecordSet recs = new RecordSet();
            if (l.contains(String.valueOf(STATUS_JOINED))) {
                long canViewMembers = group_.getInt(Constants.GRP_COL_CAN_VIEW_MEMBERS, 1);
                if (canViewMembers == 1 || group.hasRight(groupId, Long.parseLong(viewerId), ROLE_MEMBER)) {
                    String memberIds = toStr(group.getAllMembers(groupId, page, count, searchKey));
                    RecordSet members = getUsers("", memberIds, "user_id, display_name, remark, image_url,perhaps_name");
                    String creator = String.valueOf(group.getCreator(groupId));
                    String admins = toStr(group.getAdmins(groupId, -1, -1));
                    RecordSet adminRecs = new RecordSet();
                    Record creatorRec = getUser("", creator, "user_id, display_name, remark, image_url,perhaps_name");
                    creatorRec.put("role_in_group", ROLE_CREATOR);
                    creatorRec.put("status", STATUS_JOINED);
                    adminRecs.add(0, creatorRec);
                    RecordSet adminRecs0 = getUsers("", admins, "user_id, display_name, remark, image_url,perhaps_name");
                    for (Record admin : adminRecs0) {
                        admin.put("role_in_group", ROLE_ADMIN);
                        admin.put("status", STATUS_JOINED);
                    }
                    adminRecs.addAll(adminRecs0);
                    for (Record member : members) {
                        String memberId = member.getString("user_id");
                        int memberRole = ROLE_MEMBER;
                        if (StringUtils.equals(creator, memberId))
                            memberRole = ROLE_CREATOR;
                        else if (StringUtils.contains(admins, memberId))
                            memberRole = ROLE_ADMIN;
                        member.put("role_in_group", memberRole);
                        member.put("status", STATUS_JOINED);
                    }
                    recs.addAll(members);
                    if (StringUtils.isBlank(searchKey)) {
                        recs.removeAll(adminRecs);
                        if (page == 0 || page == -1)
                            recs.addAll(0, adminRecs);
                    }
                }
                l.remove(String.valueOf(STATUS_JOINED));
            }
            if (!l.isEmpty()) {
                RecordSet recs0 = RecordSet.fromByteBuffer(group.getPendingUsersByStatus(groupId, 0, StringUtils2.joinIgnoreBlank(",", l), page, count, searchKey));
                RecordSet recs1 = new RecordSet(); // borqs id
                RecordSet recs2 = new RecordSet(); // identify
                for (Record rec : recs0) {
                    String userId = rec.getString("user_id", "0");
                    String identify = rec.getString("identify", "");
                    String sourceIds = "";
                    if (!StringUtils.equals(userId, "0")) {
                        Record user = getUser(viewerId, userId, "user_id, display_name, remark, image_url,perhaps_name");
                        rec.putAll(user);

                        sourceIds = toStr(group.getSourcesById(groupId, userId));
                    } else
                        sourceIds = toStr(group.getSourcesByIdentify(groupId, identify));
                    rec.put("role_in_group", ROLE_GUEST);
                    if (StringUtils.isNotBlank(sourceIds)) {
                        RecordSet source = getUsers(viewerId, sourceIds, "user_id, display_name, remark, image_url,perhaps_name");
                        rec.put("sources", source.toJsonNode());
                    }

                    Record r = rec.copy();
                    if (!StringUtils.equals(userId, "0"))
                        recs1.add(r);
                    else
                        recs2.add(r);
                }
                recs1.unique("user_id");
                recs2.unique("identify");
                recs.addAll(recs1);
                recs.addAll(recs2);
            }
            return recs;

        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet addMembers(long groupId, Record roles, boolean sendPost) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            boolean r = group.addMembers(groupId, roles.toByteBuffer());

            if (r) {
                Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
                String groupName = rec.getString(GRP_COL_NAME);
                int isStreamPublic = (int) rec.getInt(GRP_COL_IS_STREAM_PUBLIC);
                int canSearch = (int) rec.getInt(GRP_COL_CAN_SEARCH);
                int canViewMembers = (int) rec.getInt(GRP_COL_CAN_VIEW_MEMBERS);

                RecordSet recs = new RecordSet();
                for (String userId : roles.keySet()) {
                    Record statusRec = new Record();
                    statusRec.put("user_id", Long.parseLong(userId));
                    statusRec.put("display_name", getUser(userId, userId, "display_name").getString("display_name"));
                    statusRec.put("identify", "");
                    statusRec.put("source", 0);
                    statusRec.put("status", STATUS_JOINED);
                    recs.add(statusRec);

                    // send a post
//                    if (sendPost) {
//                        String m = "";
//                        String tempNowAttachments = "[]";
//                        int appid = Constants.APP_TYPE_BPC;
//                        String groupType = getGroupTypeStr(groupId, "");
//                        String template = Constants.getBundleString("", "platform.join.group.message");
//                        String groupSchema = "<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>";
//                        String postMsg = SQLTemplate.merge(template, new Object[][]{
//                                {"groupType", groupType},
//                                {"groupName", groupSchema}
//                        });
//
//                        boolean secretly = false;
//                        if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
//                            secretly = true;
//                        autoPost(userId, Constants.TEXT_POST, postMsg, tempNowAttachments, toStr(appid), "", "", m, String.valueOf(groupId), secretly, "", "", "", true, true, true, "", "", false);
//                    }
                }
                group.addOrUpdatePendings(groupId, recs.toByteBuffer());
            }

            String memberIds = StringUtils2.joinIgnoreBlank(",", roles.keySet());
            RecordSet recs = getUsers("", memberIds, "user_id, display_name, remark, image_url,perhaps_name");
            for (Record rec : recs) {
                rec.put("status", STATUS_JOINED);
                rec.put("source", JsonNodeFactory.instance.objectNode());
                rec.put("key", rec.getString("user_id"));
            }

            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public int dealGroupInvite(long groupId, String userId, String source, boolean accept, String ua, String loc) throws AvroRemoteException {
        if (accept) {
            // add source as friend
//            setFriends(userId, source, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
//            setFriends(source, userId, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
//            setFriends(userId, source, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
//            setFriends(source, userId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);

            return addMember(groupId, userId, "", ua, loc, NULL_APP_ID);
        } else {
            Record statusRec = new Record();
            statusRec.put("user_id", Long.parseLong(userId));
            statusRec.put("display_name", getUser(userId, userId, "display_name").getString("display_name"));
            statusRec.put("identify", "");
            statusRec.put("source", source);
            statusRec.put("status", STATUS_REJECTED);

            Transceiver trans = getTransceiver(Group.class);
            try {
                Group group = getProxy(Group.class, trans);
                group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
                return STATUS_REJECTED;
            } finally {
                closeTransceiver(trans);
            }
        }
    }

    public int rejectGroupInviteForIdentify(long groupId, String name, String identify, String source) throws AvroRemoteException {
        Record statusRec = new Record();
        statusRec.put("user_id", 0);
        statusRec.put("display_name", name);
        statusRec.put("identify", identify);
        statusRec.put("source", source);
        statusRec.put("status", STATUS_REJECTED);

        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
            return STATUS_REJECTED;
        } finally {
            closeTransceiver(trans);
        }
    }

    public int addMember(long groupId, String userId, String message, String ua, String loc, String appId) throws AvroRemoteException {
        return addMember(groupId, userId, message, ua, loc, appId, false);
    }

    public int addMember(long groupId, String userId, String message, String ua, String loc, String appId, boolean sendPost) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            String userName = getUser(userId, userId, "display_name").getString("display_name");
            Group group = getProxy(Group.class, trans);
            int status = group.getUserStatusById(groupId, Long.parseLong(userId));
            L.trace("[Method addMember] status: " + status);
            String groupType = getGroupTypeStr(groupId, ua);

            int changedStatus = status;
            if (status == STATUS_INVITED) {
                boolean b = group.addMembers(groupId, Record.of(userId, ROLE_MEMBER).toByteBuffer());
                if (b) {
                    Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
                    String groupName = rec.getString(GRP_COL_NAME);
                    int isStreamPublic = (int) rec.getInt(GRP_COL_IS_STREAM_PUBLIC);
                    int canSearch = (int) rec.getInt(GRP_COL_CAN_SEARCH);
                    int canViewMembers = (int) rec.getInt(GRP_COL_CAN_VIEW_MEMBERS);
                    Record dataRec = new Record();
                    dataRec.put("group_id", groupId);
                    dataRec.put("group_name", groupName);
                    String data = dataRec.toString(false, false);
                    dealRelatedRequests("0", "0", data);

                    changedStatus = STATUS_JOINED;

                    // send a post
                    if (sendPost) {
                        String m = "";
                        String tempNowAttachments = "[]";
//                    String groupType = getGroupTypeStr(groupId, ua);
                        String template = Constants.getBundleString(ua, "platform.join.group.message");
                        String groupSchema = "<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>";
                        String postMsg = SQLTemplate.merge(template, new Object[][]{
                                {"groupType", groupType},
                                {"groupName", groupSchema}
                        });

                        boolean secretly = false;
                        if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
                            secretly = true;
                        autoPost(userId, Constants.TEXT_POST, postMsg, tempNowAttachments, appId, "", "", m, String.valueOf(groupId), secretly, "", ua, loc, true, true, true, "", "", false);
                    }

                    // send notification
                    sendNotification(Constants.NTF_GROUP_JOIN,
                            createArrayNodeFromStrings(appId),
                            createArrayNodeFromStrings(userId),
                            createArrayNodeFromStrings(String.valueOf(groupId), groupType, groupName),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(String.valueOf(groupId)),
                            createArrayNodeFromStrings(String.valueOf(groupId), groupType, groupName),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(),
                            createArrayNodeFromStrings(String.valueOf(groupId)),
                            createArrayNodeFromStrings(String.valueOf(groupId))
                    );
                }
            } else if (status != STATUS_JOINED) {
                Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
                long canJoin = rec.getInt(GRP_COL_CAN_JOIN, 1);
                if (canJoin == 1) {
                    boolean b = group.addMembers(groupId, Record.of(userId, ROLE_MEMBER).toByteBuffer());
                    if (b)
                        changedStatus = STATUS_JOINED;
                } else if (canJoin == 0) {
                    String groupName = rec.getString(GRP_COL_NAME);
                    String tos = canApproveUsers(groupId);
                    List<String> toIds = StringUtils2.splitList(tos, ",", true);

                    for (String to : toIds) {
                        Record dataRec = new Record();
                        dataRec.put("user_id", userId);
                        dataRec.put("user_name", userName);
                        dataRec.put("group_id", groupId);
                        dataRec.put("group_name", groupName);
                        String data = dataRec.toString(false, false);
                        createRequest(to, userId, "0", getGroupRequestType(groupId, false), message, data, false, ua, loc);

                        sendNotification(Constants.NTF_GROUP_APPLY,
                                createArrayNodeFromStrings(appId),
                                createArrayNodeFromStrings(userId),
                                createArrayNodeFromStrings(userName, groupType, groupName),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(String.valueOf(groupId)),
                                createArrayNodeFromStrings(userName, groupType, groupName, userId, String.valueOf(groupId)),
                                createArrayNodeFromStrings(message),
                                createArrayNodeFromStrings(message),
                                createArrayNodeFromStrings(String.valueOf(groupId)),
                                createArrayNodeFromStrings(String.valueOf(groupId))
                        );
                        sendGroupNotification(groupId, new GroupApplyNotifSender(this, null), userId, new Object[]{String.valueOf(groupId)}, message,
                                userName, groupType, groupName);
                    }
                    changedStatus = STATUS_APPLIED;
                } else {
                    changedStatus = STATUS_NONE;
                }
            }

            Record statusRec = new Record();
            statusRec.put("user_id", Long.parseLong(userId));
            statusRec.put("display_name", userName);
            statusRec.put("identify", "");
            statusRec.put("source", 0);
            statusRec.put("status", changedStatus);
            group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
            L.trace("[Method addMember] changedStatus: " + changedStatus);

            return changedStatus;
        } finally {
            closeTransceiver(trans);
        }
    }

    public int getTypeByStr(String str) {
        int type = 0; //0 - borqs id   1 - email  2 - phone  3 - local circle  4- virtual id  5 - group id

        if (StringValidator.validateEmail(str))
            type = 1;
        else if (StringValidator.validatePhone(str))
            type = 2;
        else if (StringUtils.startsWith(str, "#"))
            type = 3;
        else if (StringUtils.isNotBlank(str) && str.length() == 19)
            type = 4;
        else if (StringUtils.startsWith(str, "$")) {
            type = 5;
        }

        return type;
    }

    public boolean hasGroupRight(long groupId, String viewerId, int minRole) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return group.hasRight(groupId, Long.parseLong(viewerId), minRole);
        } finally {
            closeTransceiver(trans);
        }
    }

    public String findGroupIdsByTopPost(String postId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return toStr(group.findGroupIdsByTopPost(postId));
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record approveMember(long groupId, String viewerId, String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            int status = STATUS_NONE;
            status = group.getUserStatusById(groupId, Long.parseLong(userId));
            L.trace("[Method approveMember] status: " + status);

            int changedStatus = status;
            if (status == STATUS_APPLIED) {
                Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
                int minRole = Constants.ROLE_ADMIN;
                long canMemberApprove = rec.getInt(GRP_COL_CAN_MEMBER_APPROVE, 1);
                if (canMemberApprove == 1)
                    minRole = Constants.ROLE_MEMBER;
                if (group.hasRight(groupId, Long.parseLong(viewerId), minRole)) {
                    boolean b = group.addMembers(groupId, Record.of(userId, ROLE_MEMBER).toByteBuffer());
                    if (b) {
                        String name = getUser(userId, userId, "display_name").getString("display_name");
                        String groupName = rec.getString(GRP_COL_NAME);
                        Record dataRec = new Record();
                        dataRec.put("user_id", userId);
                        dataRec.put("user_name", name);
                        dataRec.put("group_id", groupId);
                        dataRec.put("group_name", groupName);
                        String data = dataRec.toString(false, false);
                        dealRelatedRequests(viewerId, userId, data);

                        changedStatus = STATUS_JOINED;
                    }
                }
            }

            Record r = getUser("", userId, "user_id, display_name, remark, image_url,perhaps_name");
            r.put("status", changedStatus);
            r.put("source", JsonNodeFactory.instance.objectNode());

            Record statusRec = new Record();
            statusRec.put("user_id", Long.parseLong(userId));
            statusRec.put("display_name", r.getString("display_name"));
            statusRec.put("identify", "");
            statusRec.put("source", 0);
            statusRec.put("status", changedStatus);
            group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
            L.trace("[Method approveMember] changedStatus: " + changedStatus);

            return r;
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record ignoreMember(long groupId, String viewerId, String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            int status = STATUS_NONE;
            status = group.getUserStatusById(groupId, Long.parseLong(userId));
            L.trace("[Method ignoreMember] status: " + status);

            int changedStatus = status;
            if (status == STATUS_APPLIED) {
                Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
                int minRole = Constants.ROLE_ADMIN;
                long canMemberApprove = rec.getInt(GRP_COL_CAN_MEMBER_APPROVE, 1);
                if (canMemberApprove == 1)
                    minRole = Constants.ROLE_MEMBER;
                if (group.hasRight(groupId, Long.parseLong(viewerId), minRole)) {
                    String name = getUser(userId, userId, "display_name").getString("display_name");
                    String groupName = rec.getString(GRP_COL_NAME);
                    Record dataRec = new Record();
                    dataRec.put("user_id", userId);
                    dataRec.put("user_name", name);
                    dataRec.put("group_id", groupId);
                    dataRec.put("group_name", groupName);
                    String data = dataRec.toString(false, false);
                    dealRelatedRequests(viewerId, userId, data);

                    changedStatus = STATUS_REJECTED;
                }
            }

            Record r = getUser("", userId, "user_id, display_name, remark, image_url,perhaps_name");
            r.put("status", changedStatus);
            r.put("source", JsonNodeFactory.instance.objectNode());

            Record statusRec = new Record();
            statusRec.put("user_id", Long.parseLong(userId));
            statusRec.put("display_name", r.getString("display_name"));
            statusRec.put("identify", "");
            statusRec.put("source", 0);
            statusRec.put("status", changedStatus);
            group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
            L.trace("[Method ignoreMember] changedStatus: " + changedStatus);

            return r;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean dealRelatedRequests(String userId, String sourceIds, String datas) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Request.class);
        try {
            Request req = getProxy(Request.class, trans);
            String requestIds = toStr(req.getRelatedRequestIds(sourceIds, datas));
            return req.doneRequest(userId, requestIds);
        } finally {
            closeTransceiver(trans);
        }
    }

    public long getBeginByGroupType(String type) {
        Configuration conf = getConfig();
        return conf.getInt("group." + type + ".begin", GENERAL_GROUP_ID_BEGIN);
    }

    public long getEndByGroupType(String type) {
        Configuration conf = getConfig();
        return conf.getInt("group." + type + ".end", EVENT_ID_BEGIN);
    }

    public String getGroupTypeStr(long groupId, String ua) throws AvroRemoteException {
        boolean isEn = false;
        if (StringUtils.isBlank(ua)) {
            isEn = false;
        }
        else {
            String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
            isEn = StringUtils.equals(lang, "en");
        }

        String groupType = "";
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            if (group.isPublicCircle(groupId))
                groupType = isEn ? "public circle" : "公共圈子";
            else if (group.isActivity(groupId))
                groupType = isEn ? "activity" : "活动";
            else if (group.isOrganization(groupId))
                groupType = isEn ? "organization" : "组织";
            else if (group.isEvent(groupId))
                groupType = isEn ? "event" : "事件";
            else
                groupType = isEn ? "group" : "组";

            return groupType;
        } finally {
            closeTransceiver(trans);
        }
    }

    private void eventEmailInvite(long groupId, String groupName, String userName, String viewerName, String register, String groupType,
                                  String acceptUrl, String rejectUrl, String email, String message, String ua, String lang) throws AvroRemoteException {
        Record rec = getGroups(EVENT_ID_BEGIN, EVENT_ID_END, String.valueOf(groupId), "start_time,end_time,address").getFirstRecord();
        String startTime = DateUtils.formatDateAndTime(rec.getInt("start_time"));
        String endTime = DateUtils.formatDateAndTime(rec.getInt("end_time"));
        String address = "";
        String addrJsonStr = rec.getString("address");
        if (StringUtils.isNotBlank(addrJsonStr)) {
            RecordSet addrRecs = RecordSet.fromJson(addrJsonStr);
            if (addrRecs != null) {
                Record addrRec = addrRecs.getFirstRecord();
                if (addrRec != null) {
                    String addrCountry = addrRec.getString("country", "");
                    String addrState = addrRec.getString("state", "");
                    String addrCity = addrRec.getString("city", "");
                    String addrStreet = addrRec.getString("street", "");
                    String addrCode = addrRec.getString("postal_code", "");
                    String addrBox = addrRec.getString("po_box", "");
                    String addrExt = addrRec.getString("extended_address", "");
                    address = addrCountry + addrState + addrCity + addrStreet + addrCode + addrExt + addrBox;
                }
            }
        }
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        message = StringUtils.isNotBlank(message) ? "Here is his postscript: <br/>    " + message : "";

        map.put("displayName", userName);
        map.put("fromName", viewerName);
        map.put("register", register);
        map.put("groupType", groupType);
        map.put("groupName", groupName);
        map.put("acceptUrl", generalShortUrl(acceptUrl));
        map.put("rejectUrl", generalShortUrl(rejectUrl));
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("address", address);
        map.put("message", message);

        String template = Constants.getBundleString(ua, "platform.group.email.invite.subject");
        String subject = SQLTemplate.merge(template, new Object[][]{
                {"fromName", viewerName},
                {"groupType", groupType},
                {"groupName", groupName}
        });

        sendCustomEmail(subject, email, email, "event_invite2.ftl", map, Constants.EMAIL_ESSENTIAL, lang);
    }

    private void groupEmailInvite(long groupId, String groupName, String userId, String userName, String viewerId,
                                  String viewerName, String email, String message, String ua) throws AvroRemoteException {

        boolean isEvent = (groupId >= EVENT_ID_BEGIN && groupId < EVENT_ID_END);

        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        boolean isEn = isEvent ? true : StringUtils.equals(lang, "en");
        String groupType = isEvent ? "event" : getGroupTypeStr(groupId, ua);

        String acceptUrl = "";
        String rejectUrl = "";
        String register = "";
        if (StringUtils.equals(userId, "0")) {
            String info = FeedbackParams.toSegmentedBase64(true, "/", email, userName, viewerId);
            acceptUrl = "http://" + SERVER_HOST + "/account/invite?info=" + info + "&group_id=" + groupId;
            rejectUrl = "http://" + SERVER_HOST + "/v2/group/deal_invite?name=" + userName + "&identify=" + email
                    + "&group_id=" + groupId + "&source=" + viewerId;
            register = isEn ? "active borqs account and" : "激活播思账号并";
        } else {
            String s = "http://" + SERVER_HOST + "/v2/group/deal_invite?user_id=" + userId + "&source=" + viewerId + "&group_id=" + groupId + "&accept=";
            acceptUrl = s + true;
            rejectUrl = s + false;
        }

        if (isEvent) {
            eventEmailInvite(groupId, groupName, userName, viewerName, register, groupType, acceptUrl, rejectUrl, email, message, ua, lang);
        } else {
            String template = Constants.getBundleString(ua, "platform.group.email.invite.content");
            String emailContent = SQLTemplate.merge(template, new Object[][]{
                    {"displayName", userName},
                    {"fromName", viewerName},
                    {"register", register},
                    {"groupType", groupType},
                    {"groupName", groupName},
                    {"acceptUrl", generalShortUrl(acceptUrl)},
                    {"rejectUrl", generalShortUrl(rejectUrl)}
            });

            if (StringUtils.isNotBlank(message)) {
                template = Constants.getBundleString(ua, "platformservlet.email.invite.postscript");
                emailContent += SQLTemplate.merge(template, new Object[][]{
                        {"message", message}
                });
            }

            template = Constants.getBundleString(ua, "platform.group.email.invite.subject");
            String subject = SQLTemplate.merge(template, new Object[][]{
                    {"fromName", viewerName},
                    {"groupType", groupType},
                    {"groupName", groupName}
            });

            sendEmail(subject, email, email, emailContent, Constants.EMAIL_ESSENTIAL, lang);
        }
    }

    private void groupSmsInvite(long groupId, String groupName, String userId, String userName, String viewerId,
                                String viewerName, String phone, String message, String ua) throws AvroRemoteException, UnsupportedEncodingException {
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        boolean isEn = StringUtils.equals(lang, "en");
        String groupType = getGroupTypeStr(groupId, ua);

        String acceptUrl = "";
        String rejectUrl = "";
        String register = "";
        if (StringUtils.equals(userId, "0")) {
            String info = FeedbackParams.toSegmentedBase64(true, "/", phone, userName, viewerId);
            acceptUrl = "http://" + SERVER_HOST + "/account/invite?info=" + info + "&group_id=" + groupId;
            rejectUrl = "http://" + SERVER_HOST + "/v2/group/deal_invite?name=" + userName + "&identify=" + phone
                    + "&group_id=" + groupId + "&source=" + viewerId;
            register = isEn ? "active borqs account and" : "激活播思账号并";
        } else {
            String s = "http://" + SERVER_HOST + "/v2/group/deal_invite?user_id=" + userId + "&source=" + viewerId + "&group_id=" + groupId + "&accept=";
            acceptUrl = s + true;
            rejectUrl = s + false;
        }

        String smsContent = viewerName + "邀请您加入" + groupType + "，请点击：";

        userName = URLEncoder.encode(userName, "utf-8");
        viewerName = URLEncoder.encode(viewerName, "utf-8");
        register = URLEncoder.encode(register, "utf-8");
        groupType = URLEncoder.encode(groupType, "utf-8");
        groupName = URLEncoder.encode(groupName, "utf-8");
        message = URLEncoder.encode(message, "utf-8");
        acceptUrl = URLEncoder.encode(acceptUrl, "utf-8");
        rejectUrl = URLEncoder.encode(rejectUrl, "utf-8");

        String pageUrl = "http://" + SERVER_HOST + "/v2/group/invite_page?display_name=" + userName + "&from_name=" + viewerName + "&register="
                + register + "&group_type=" + groupType + "&group_name=" + groupName + "&message=" + message + "&accept_url=" + acceptUrl + "&reject_url=" + rejectUrl;

        String shortUrl = generalShortUrl(pageUrl);
        smsContent += shortUrl + "\\";
        sendSms(phone, smsContent);
    }

    public boolean updateUserIdByIdentify(String userId, String identify) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return group.updateUserIdByIdentify(userId, identify);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet inviteMembers(long groupId, String userIds, String viewerId, String message, String ua, String loc, String appId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);

            ArrayList<String> applied = new ArrayList<String>();
            ArrayList<String> toInvite = new ArrayList<String>();

            Record statuses = Record.fromByteBuffer(group.getUserStatusByIds(groupId, userIds));
            for (Map.Entry<String, Object> entry : statuses.entrySet()) {
                String userId = entry.getKey();
                int status = ((Long) entry.getValue()).intValue();

                if (status == STATUS_APPLIED)
                    applied.add(userId);
                else if (status != STATUS_JOINED)
                    toInvite.add(userId);
            }

            Record source = getUser(viewerId, viewerId, "user_id, display_name, remark, image_url,perhaps_name");
            String viewerName = source.getString("display_name");
            Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
            String groupName = rec.getString(GRP_COL_NAME);
            long canMemberInvite = rec.getInt(GRP_COL_CAN_MEMBER_INVITE, 1);

            RecordSet statusRecs = new RecordSet();
            RecordSet recs0 = new RecordSet();
            RecordSet recs = new RecordSet();

            // applied
            if (!applied.isEmpty()) {
                Record appliedRec = new Record();
                for (String userId : applied)
                    appliedRec.put(userId, ROLE_MEMBER);
                group.addMembers(groupId, appliedRec.toByteBuffer());

                String datas = "";
                String appliedIds = StringUtils2.joinIgnoreBlank(",", applied);
                recs0 = getUsers("", appliedIds, "user_id, display_name, remark, image_url,perhaps_name");
                for (Record r : recs0) {
                    String userId = r.getString("user_id");
                    String name = r.getString("display_name");
                    r.put("status", STATUS_JOINED);
                    r.put("source", source.toJsonNode());
                    r.put("key", userId);

                    Record dataRec = new Record();
                    dataRec.put("user_id", userId);
                    dataRec.put("user_name", name);
                    dataRec.put("group_id", groupId);
                    dataRec.put("group_name", groupName);
                    String data = dataRec.toString(false, false);
                    datas += data + "|";

                    Record statusRec = new Record();
                    statusRec.put("user_id", Long.parseLong(userId));
                    statusRec.put("display_name", name);
                    statusRec.put("identify", "");
                    statusRec.put("source", Long.parseLong(viewerId));
                    statusRec.put("status", STATUS_JOINED);
                    statusRecs.add(statusRec);
                }
                datas = StringUtils.substringBeforeLast(datas, "|");
                dealRelatedRequests(viewerId, appliedIds, datas);
            }

            //to invite
            if (!toInvite.isEmpty()) {
                String toInviteIds = StringUtils2.joinIgnoreBlank(",", toInvite);
                if (canMemberInvite == 1 || group.hasRight(groupId, Long.parseLong(viewerId), Constants.ROLE_ADMIN)) {
                    Record dataRec = new Record();
                    dataRec.put("group_id", groupId);
                    dataRec.put("group_name", groupName);
                    String data = dataRec.toString(false, false);
                    createRequests(toInviteIds, viewerId, "0", getGroupRequestType(groupId, true), message, data);

                    String groupType = getGroupTypeStr(groupId, ua);

                    int toSize = toInvite.size();
                    if (toSize < 20) {
                        sendNotification(Constants.NTF_GROUP_INVITE,
                                createArrayNodeFromStrings(appId),
                                createArrayNodeFromStrings(viewerId),
                                createArrayNodeFromStrings(viewerName, groupType, groupName, "邀请"),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(),
                                createArrayNodeFromStrings(String.valueOf(groupId)),
                                createArrayNodeFromStrings(viewerName, groupType, groupName, viewerId, String.valueOf(groupId), "邀请"),
                                createArrayNodeFromStrings(message),
                                createArrayNodeFromStrings(message),
                                createArrayNodeFromStrings(String.valueOf(groupId)),
                                createArrayNodeFromStrings(toInviteIds)
                        );
                    } else {
                        int count = toSize / 20;
                        for (int i = 0; i <= count; i++) {
                            int toIndex = (i * 20 + 20) > toSize ? toSize : i * 20 + 20;
                            List<String> subList = toInvite.subList(i * 20, toIndex);
                            String subToIds = StringUtils2.joinIgnoreBlank(",", subList);
                            sendNotification(Constants.NTF_GROUP_INVITE,
                                    createArrayNodeFromStrings(appId),
                                    createArrayNodeFromStrings(viewerId),
                                    createArrayNodeFromStrings(viewerName, groupType, groupName, "邀请"),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(String.valueOf(groupId)),
                                    createArrayNodeFromStrings(viewerName, groupType, groupName, viewerId, String.valueOf(groupId), "邀请"),
                                    createArrayNodeFromStrings(message),
                                    createArrayNodeFromStrings(message),
                                    createArrayNodeFromStrings(String.valueOf(groupId)),
                                    createArrayNodeFromStrings(subToIds)
                            );
                        }
                    }
                    sendGroupNotification(groupId, new GroupInviteNotifSender(this, null), viewerId, new Object[]{toInviteIds}, message,
                            viewerName, groupType, groupName, "邀请");

                    recs = getUsers("", toInviteIds, "user_id, display_name, remark, image_url,perhaps_name,login_email1, login_email2, login_email3", false);
                    for (Record r : recs) {
                        String userId = r.getString("user_id");
                        if (!StringUtils.equals(userId, viewerId)) {
                            String name = r.getString("display_name");
                            r.put("status", STATUS_INVITED);
                            r.put("source", source.toJsonNode());
                            r.put("key", userId);

                            Record statusRec = new Record();
                            statusRec.put("user_id", Long.parseLong(userId));
                            statusRec.put("display_name", name);
                            statusRec.put("identify", "");
                            statusRec.put("source", Long.parseLong(viewerId));
                            statusRec.put("status", STATUS_INVITED);
                            statusRecs.add(statusRec);

                            //event send mail
                            if (group.isEvent(groupId)) {
                                String[] emails = new String[3];
                                emails[0] = r.getString("login_email1", "");
                                emails[1] = r.getString("login_email2", "");
                                emails[2] = r.getString("login_email3", "");
                                for (int i = 0; i < 3; i++) {
                                    if (StringUtils.isNotBlank(emails[i])) {
                                        groupEmailInvite(groupId, groupName, userId, name, viewerId, viewerName, emails[i], message, ua);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!statusRecs.isEmpty())
                group.addOrUpdatePendings(groupId, statusRecs.toByteBuffer());

            recs.addAll(recs0);
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getGroupRequestType(long groupId, boolean isInvite) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            if (group.isPublicCircle(groupId))
                return isInvite ? REQUEST_PUBLIC_CIRCLE_INVITE : REQUEST_PUBLIC_CIRCLE_JOIN;
            else if (group.isActivity(groupId))
                return isInvite ? REQUEST_ACTIVITY_INVITE : REQUEST_ACTIVITY_JOIN;
            else if (group.isOrganization(groupId))
                return isInvite ? REQUEST_ORGANIZATION_INVITE : REQUEST_ORGANIZATION_JOIN;
            else if (group.isEvent(groupId))
                return isInvite ? REQUEST_EVENT_INVITE : REQUEST_EVENT_JOIN;
            else
                return isInvite ? REQUEST_GENERAL_GROUP_INVITE : REQUEST_GENERAL_GROUP_JOIN;
        } finally {
            closeTransceiver(trans);
        }
    }
    
    public Record inviteMember(long groupId, String to, String name, String viewerId, String message, String ua, String loc, String appId) throws AvroRemoteException, UnsupportedEncodingException {
        int type = getTypeByStr(to);
        L.trace("[Method inviteMember] type: " + type);

        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            int status = STATUS_NONE;
            if (type == 0)
                status = group.getUserStatusById(groupId, Long.parseLong(to));
            else
                status = group.getUserStatusByIdentify(groupId, to);
            L.trace("[Method inviteMember] status: " + status);

            String userId = to;
            String key = "email";
            if (type == 2)
                key = "phone";
            if (type != 0) {
                RecordSet recs = findBorqsIdFromContactInfo(RecordSet.of(Record.of(key, to)));
                userId = recs.getFirstRecord().getString("user_id", "0");

                if (!StringUtils.equals(userId, "0")) {
                    updateUserIdByIdentify(userId, to);
                }
            }
            L.trace("[Method inviteMember] userId: " + userId);

            Record source = getUser(viewerId, viewerId, "user_id, display_name, remark, image_url,perhaps_name");
            String viewerName = source.getString("display_name");
            Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
            String groupName = rec.getString(GRP_COL_NAME);
            int changedStatus = status;
            if (status == STATUS_APPLIED) {
                boolean b = group.addMembers(groupId, Record.of(userId, ROLE_MEMBER).toByteBuffer());
                if (b) {
                    Record dataRec = new Record();
                    dataRec.put("user_id", userId);
                    dataRec.put("user_name", name);
                    dataRec.put("group_id", groupId);
                    dataRec.put("group_name", groupName);
                    String data = dataRec.toString(false, false);
                    dealRelatedRequests(viewerId, userId, data);

                    changedStatus = STATUS_JOINED;
                }
            } else if (status != STATUS_JOINED) {
                long canMemberInvite = rec.getInt(GRP_COL_CAN_MEMBER_INVITE, 1);
                long needInvitedConfirm = rec.getInt(GRP_COL_NEED_INVITED_CONFIRM, 1);
                if (canMemberInvite == 1 || group.hasRight(groupId, Long.parseLong(viewerId), Constants.ROLE_ADMIN)) {
                    if (!StringUtils.equals(userId, "0")) {
                        if (needInvitedConfirm == 1) {
                            Record dataRec = new Record();
                            dataRec.put("group_id", groupId);
                            dataRec.put("group_name", groupName);
                            String data = dataRec.toString(false, false);
                            createRequest(userId, viewerId, "0", getGroupRequestType(groupId, true), message, data, false, ua, loc);
                            //event send mail
                            if (group.isEvent(groupId)) {
                                String userCols = "user_id, display_name, login_email1, login_email2, login_email3";
                                Record user = getUser(userId, userId, userCols, false);
                                String[] emails = new String[3];
                                emails[0] = user.getString("login_email1", "");
                                emails[1] = user.getString("login_email2", "");
                                emails[2] = user.getString("login_email3", "");
                                for (int i = 0; i < 3; i++) {
                                    if (StringUtils.isNotBlank(emails[i])) {
                                        groupEmailInvite(groupId, groupName, userId, name, viewerId, viewerName, emails[i], message, ua);
                                    }
                                }
                            }
                            
                            
                            String groupType = getGroupTypeStr(groupId, ua);
                            sendNotification(Constants.NTF_GROUP_INVITE,
                                    createArrayNodeFromStrings(appId),
                                    createArrayNodeFromStrings(viewerId),
                                    createArrayNodeFromStrings(viewerName, groupType, groupName, "邀请"),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(),
                                    createArrayNodeFromStrings(String.valueOf(groupId)),
                                    createArrayNodeFromStrings(viewerName, groupType, groupName, viewerId, String.valueOf(groupId), "邀请"),
                                    createArrayNodeFromStrings(message),
                                    createArrayNodeFromStrings(message),
                                    createArrayNodeFromStrings(String.valueOf(groupId)),
                                    createArrayNodeFromStrings(userId)
                            );
                            sendGroupNotification(groupId, new GroupInviteNotifSender(this, null), viewerId, new Object[]{userId}, message,
                                    viewerName, groupType, groupName, "邀请");
                            changedStatus = STATUS_INVITED;
                        } else {
                            boolean b = group.addMembers(groupId, Record.of(userId, ROLE_MEMBER).toByteBuffer());
                            if (b) {
                                changedStatus = STATUS_JOINED;
                            }
                        }
                    }
                    if (type == 1) {
                        groupEmailInvite(groupId, groupName, userId, name, viewerId, viewerName, to, message, ua);
                        changedStatus = STATUS_INVITED;
                    }
                    if (type == 2) {
                        groupSmsInvite(groupId, groupName, userId, name, viewerId, viewerName, to, message, ua);
                        changedStatus = STATUS_INVITED;
                    }
                }
            }

            Record statusRec = new Record();
            statusRec.put("user_id", Long.parseLong(userId));
            statusRec.put("display_name", name);
            statusRec.put("identify", type !=0 ? to : "");
            statusRec.put("source", Long.parseLong(viewerId));
            statusRec.put("status", changedStatus);
            group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
            L.trace("[Method inviteMember] changedStatus: " + changedStatus);

            Record r = new Record();
            if (!StringUtils.equals(userId, "0")) {
                r = getUser("", userId, "user_id, display_name, remark, image_url,perhaps_name");
                r.put("status", changedStatus);
                r.put("source", source.toJsonNode());
            } else {
                r = statusRec.copy();
                r.put("source", source.toJsonNode());
            }

            return r;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean grants(String userId, long groupId, Record roles) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            if (group.hasRight(groupId, Long.parseLong(userId), Constants.ROLE_ADMIN))
                return group.grants(groupId, roles.toByteBuffer());
            else
                return false;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean removeMembers(String userId, long groupId, String members, String newAdmins) throws AvroRemoteException {
        Validate.notNull(userId);
        checkUserIds(userId);

        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
            int canMemberQuit = (int) rec.getInt(GRP_COL_CAN_MEMBER_QUIT, 1);

            if (group.hasRight(groupId, Long.parseLong(userId), Constants.ROLE_ADMIN)) {
                boolean r = false;
                if (StringUtils.contains(members, userId)) {
                    List<String> memberIds = StringUtils2.splitList(members, ",", true);
                    memberIds.remove(userId);
                    members = StringUtils2.joinIgnoreBlank(",", memberIds);

                    if (canMemberQuit == 1) {
                        String creator = String.valueOf(group.getCreator(groupId));
                        String admins = toStr(group.getAdmins(groupId, -1, -1));

                        int adminCount = StringUtils2.splitList(admins, ",", true).size();
                        if (!StringUtils.equals(creator, "0"))
                            adminCount++;
                        int memberCount = group.getMembersCount(groupId);

                        if ((adminCount < 2) && (memberCount > 1)) {
                            if (StringUtils.isNotBlank(newAdmins)) {
                                Record roles = new Record();
                                long[] newAdminArr = StringUtils2.splitIntArray(newAdmins, ",");
                                for (long newAdmin : newAdminArr) {
                                    roles.put(String.valueOf(newAdmin), Constants.ROLE_ADMIN);
                                }
                                grants(userId, groupId, roles);
                            } else {
                                throw new ServerException(ErrorCode.GROUP_ERROR, "You must grant an admin before quit");
                            }
                        }

                        r = group.removeMembers(groupId, userId);
                        if (r) {
                            Record statusRec = new Record();
                            statusRec.put("user_id", Long.parseLong(userId));
                            statusRec.put("display_name", getUser(userId, userId, "display_name").getString("display_name"));
                            statusRec.put("identify", "");
                            statusRec.put("source", 0);
                            statusRec.put("status", STATUS_QUIT);
                            group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
                        }

                        if (memberCount <= 1) {
                            destroyGroup(userId, String.valueOf(groupId));
                        }
                    }
                }
                if (StringUtils.isNotBlank(members)) {
                    r = r && group.removeMembers(groupId, members);
                    if (r) {
                        List<String> memberIds = StringUtils2.splitList(members, ",", true);
                        RecordSet recs = new RecordSet();
                        for (String memberId : memberIds) {
                            Record statusRec = new Record();
                            statusRec.put("user_id", Long.parseLong(memberId));
                            statusRec.put("display_name", getUser(memberId, memberId, "display_name").getString("display_name"));
                            statusRec.put("identify", "");
                            statusRec.put("source", 0);
                            statusRec.put("status", STATUS_KICKED);
                            recs.add(statusRec);
                        }
                        group.addOrUpdatePendings(groupId, recs.toByteBuffer());
                    }
                }

                return r;
            }
            else if (StringUtils.equals(userId, members) && (canMemberQuit == 1)) {
                boolean r = group.removeMembers(groupId, userId);
                if (r) {
                    Record statusRec = new Record();
                    statusRec.put("user_id", Long.parseLong(userId));
                    statusRec.put("display_name", getUser(userId, userId, "display_name").getString("display_name"));
                    statusRec.put("identify", "");
                    statusRec.put("source", 0);
                    statusRec.put("status", STATUS_QUIT);
                    group.addOrUpdatePendings(groupId, RecordSet.of(statusRec).toByteBuffer());
                }
                return r;
            }
            else {
                return false;
            }
        } catch (ResponseError e) {
            throw new ServerException(e.code, toStr(e.message));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet searchGroups(long begin, long end, String name, String userId, String cols) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(group.findGroupsByName(begin, end, name, cols));
            RecordSet copy = recs.copy();
                for (Record rec : copy) {
                    long canSearch = rec.getInt(Constants.GRP_COL_CAN_SEARCH, 1);
                    if (canSearch != 1) {
                        recs.remove(rec);
                    }
                }

            recs = additionalGroupsInfo(group, userId, recs);
            for (Record rec : recs) {
                Record creator = getUser(userId, String.valueOf(rec.getInt("creator", 0)), "user_id, display_name, remark, image_url,perhaps_name");
                rec.put("creator", creator.toJsonNode());
                rec.remove("admins");
            }

            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getCreatorAndAdmins(long groupId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            long creator = group.getCreator(groupId);
            String admins = toStr(group.getAdmins(groupId, -1, -1));
            return StringUtils.isBlank(admins) ? String.valueOf(creator) : creator + "," + admins;
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getGroupMembers(long groupId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return toStr(group.getAllMembers(groupId, -1, -1, ""));
        } finally {
            closeTransceiver(trans);
        }
    }

    public String canApproveUsers(long groupId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            Record rec = RecordSet.fromByteBuffer(group.getGroups(0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS)).getFirstRecord();
            int minRole = Constants.ROLE_ADMIN;
            long canMemberApprove = rec.getInt(GRP_COL_CAN_MEMBER_APPROVE, 1);
            if (canMemberApprove == 1) {
                return toStr(group.getAllMembers(groupId, -1, -1, ""));
            } else {
                long creator = group.getCreator(groupId);
                String admins = toStr(group.getAdmins(groupId, -1, -1));
                return StringUtils.isBlank(admins) ? String.valueOf(creator) : creator + "," + admins;
            }

        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean defaultMemberNotification(long groupId, String userIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return group.defaultMemberNotification(groupId, userIds);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateMemberNotification(long groupId, String userId, Record notif) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return group.updateMemberNotification(groupId, userId, notif.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getMembersNotification(long groupId, String userIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Group.class);
        try {
            Group group = getProxy(Group.class, trans);
            return RecordSet.fromByteBuffer(group.getMembersNotification(groupId, userIds));
        } finally {
            closeTransceiver(trans);
        }
    }
    
    // poll
    public long createPoll(Record poll, RecordSet items, String ua, String loc, String appId, boolean sendPost) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            long pollId = pollLogic.createPoll(poll.toByteBuffer(), items.toByteBuffer());
            String viewerId = poll.getString("source");
            String mentions = poll.getString("target");
            String title = poll.getString("title");
            createConversation(Constants.POLL_OBJECT, String.valueOf(pollId), Constants.C_POLL_CREATE, viewerId);

            //send a post
            if (sendPost) {
                String m = "";
                String tempNowAttachments = "[]";
                String template = Constants.getBundleString(ua, "platform.create.poll.message");
                String pollSchema = "<a href=\"borqs://poll/details?id=" + pollId + "\">" + title + "</a>";
                String message = SQLTemplate.merge(template, new Object[][]{
                        {"title", pollSchema}
                });

                long privacy = poll.getInt("privacy");
                boolean secretly = (privacy == 2);
                autoPost(viewerId, Constants.VOTE_POST, message, tempNowAttachments, appId, "", "", m, mentions, secretly, "", ua, loc, true, true, true, "", "", false);
            }

            if (StringUtils.isNotBlank(mentions)) {
                L.trace("Send poll notification");
                Record source = getUser(viewerId, viewerId, "user_id, display_name, remark, image_url,perhaps_name");
                String sourceName = source.getString("display_name");
                sendNotification(Constants.NTF_POLL_INVITE,
                        createArrayNodeFromStrings(appId),
                        createArrayNodeFromStrings(viewerId),
                        createArrayNodeFromStrings(title, sourceName),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(String.valueOf(pollId)),
                        createArrayNodeFromStrings(title, String.valueOf(pollId), viewerId, sourceName),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(),
                        createArrayNodeFromStrings(mentions)
                );
            }

            return pollId;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean vote(String userId, long pollId, Record items, String ua, String loc, String appId, boolean sendPost) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            boolean b = pollLogic.vote(userId, pollId, items.toByteBuffer());
            
            if (b && sendPost) {
                Record poll = RecordSet.fromByteBuffer(pollLogic.getPolls(String.valueOf(pollId))).getFirstRecord();
                String sourceId = poll.getString("source");
                Record source = getUser(sourceId, sourceId, "user_id, display_name, remark, image_url,perhaps_name");
                String sourceName = source.getString("display_name");
                String sourceSchema = "<a href=\"borqs://profile/details?uid=" + sourceId + "&tab=2\">" + sourceName + "</a>";

                String title = poll.getString("title");
                String pollSchema = "<a href=\"borqs://poll/details?id=" + pollId + "\">" + title + "</a>";

                String itemIds = StringUtils2.joinIgnoreBlank(",", items.keySet());
                RecordSet itemRecs = RecordSet.fromByteBuffer(pollLogic.getItemsByItemIds(itemIds));
                String itemMsg = "【" + itemRecs.joinColumnValues("message", "】, 【") + "】";

                String m = "";
                String tempNowAttachments = "[]";
                String template = Constants.getBundleString(ua, "platform.vote.message");
                String message = SQLTemplate.merge(template, new Object[][]{
                        {"source", sourceSchema},
                        {"title", pollSchema},
                        {"items", itemMsg}
                });
                
                long privacy = poll.getInt("privacy");
                boolean secretly = (privacy == 2);
                String mentions = (privacy == 2) ? sourceId : "";
                autoPost(userId, Constants.VOTE_POST, message, tempNowAttachments, appId, "", "", m, mentions, secretly, "", ua, loc, true, true, true, "", "", false);
            }
            
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }
    
    public RecordSet getSimplePolls(String pollIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            if (StringUtils.isBlank(pollIds))
                return new RecordSet();

            RecordSet polls = RecordSet.fromByteBuffer(pollLogic.getPolls(pollIds));
            return polls;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean destroyPolls(String viewerId, String pollIds) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            return pollLogic.deletePolls(viewerId, pollIds);
        } finally {
            closeTransceiver(trans);
        }
    }
    
    public RecordSet getPolls(String viewerId, String pollIds, boolean withItems) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            if (StringUtils.isBlank(pollIds))
                return new RecordSet();

            RecordSet polls = RecordSet.fromByteBuffer(pollLogic.getPolls(pollIds));
            Record counts = Record.fromByteBuffer(pollLogic.getCounts(pollIds));
            
            for (Record poll : polls) {
                long pollId = poll.getInt("id");
                poll.put("id", ObjectUtils.toString(pollId));
                
                String sourceId = poll.getString("source");
                Record source = getUser(sourceId, sourceId, "user_id, display_name, remark, image_url,perhaps_name");
                poll.put("source", source);
                
                long startTime = poll.getInt("start_time");
                long endTime = poll.getInt("end_time");
                long now = DateUtils.nowMillis();
                int status = 0; //  0 - not start    1 - process  2 - end
                if (now < startTime)
                    status = 0;
                else if (((now >= startTime) && (now <= endTime)) || endTime == 0)
                    status = 1;
                else
                    status = 2;
                poll.put("status", status);
                poll.put("left", endTime - now);

                poll.put("count", counts.getInt(String.valueOf(pollId)));
                
                String target = poll.getString("target");
                List<String> targetIds = StringUtils2.splitList(target, ",", true);
                List<String> groupIds = getGroupIdsFromMentions(targetIds);
                targetIds.removeAll(groupIds);
                RecordSet targetUsers = getUsers(viewerId, StringUtils2.joinIgnoreBlank(",", targetIds), "user_id, display_name, remark, image_url,perhaps_name");
                RecordSet targetGroups = groupIds.size() == 0 ? new RecordSet() : getGroups(PUBLIC_CIRCLE_ID_BEGIN, GROUP_ID_END, viewerId, StringUtils2.joinIgnoreBlank(",", groupIds), GROUP_LIGHT_COLS, false);
                targetUsers.addAll(targetGroups);
                poll.put("target", targetUsers);
                int commentCount = getCommentCount(viewerId, Constants.POLL_OBJECT, String.valueOf(pollId));
                Record comments = new Record();
                comments.put("count", commentCount);
                poll.put("comments", comments);
                
                if (withItems) {
                    long anonymous = poll.getInt("anonymous");
                    RecordSet items = RecordSet.fromByteBuffer(pollLogic.getItemsByPollId(pollId));
                    for (Record item : items) {
                        long itemId = item.getInt("id");
                        item.put("id", ObjectUtils.toString(itemId));
                        RecordSet participants = RecordSet.fromJson(JsonUtils.toJson(item.get("participants"), false));
                        if (anonymous == 1)
                            item.put("participants", JsonNodeFactory.instance.arrayNode());
                        else {                            
                            boolean viewerVoted = false;
                            RecordSet participantsInFriends = new RecordSet();
                            for (Record participant : participants) {
                                String userId = participant.getString("user");
                                if (isFriend(viewerId, userId) || StringUtils.equals(viewerId, userId)) {
                                    Record user = getUser(userId, userId, "user_id, display_name, remark, image_url,perhaps_name");
                                    Record r = participant.copy();
                                    r.put("user", user);
                                    participantsInFriends.add(r);

                                    if (StringUtils.equals(viewerId, userId) && !viewerVoted)
                                        viewerVoted = true;
                                }
                            }
                            item.put("participants", participantsInFriends);
                            item.put("viewer_voted", viewerVoted);
                        }
                    }
                    poll.put("items", items);
                }
                boolean canVote = canVote(viewerId, pollId);
                poll.put("viewer_can_vote", canVote);
                
                long mode = poll.getInt("mode");
                long multi = poll.getInt("multi");
                long hasVoted = pollLogic.hasVoted(viewerId, pollId);
                poll.put("has_voted", hasVoted > 0);
                
                long viewerLeft = 0;
                if (canVote) {
                    if (mode == 0)
                        viewerLeft = multi;
                    else if (mode == 1)
                        viewerLeft = multi - hasVoted;
                    else
                        viewerLeft = multi;
                }
                poll.put("viewer_left", viewerLeft);
            }

            return polls;
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean canVote(String viewerId, long pollId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);

            Record poll = RecordSet.fromByteBuffer(pollLogic.getPolls(String.valueOf(pollId))).getFirstRecord();

            //poll mode
            long mode = poll.getInt("mode");
            long multi = poll.getInt("multi");
            long hasVoted = pollLogic.hasVoted(viewerId, pollId);
            boolean con1 = false;
            if (mode == 0)
                con1 = (hasVoted <= 0);
            else if (mode == 1)
                con1 = (hasVoted < multi);
            else
                con1 = true;
            
            //end time
            long startTime = poll.getInt("start_time");
            long endTime = poll.getInt("end_time");
            long now = DateUtils.nowMillis();
            int status = 0; //  0 - not start    1 - process  2 - end
            if (now < startTime)
                status = 0;
            else if (((now >= startTime) && (now <= endTime)) || endTime == 0)
                status = 1;
            else
                status = 2;
            boolean con2 = (status == 1);

            //privacy
            boolean con3 = false;
            long privacy = poll.getInt("privacy");
            String source = poll.getString("source");
            String targets = poll.getString("target");
            if (privacy == 0)
                con3 = true;
            else if (privacy == 1) {
                con3 = isFriend(source, viewerId);
            }
            else {
                List<String> l = StringUtils2.splitList(targets, ",", true);
                for (String target : l) {
                    long id = 0;
                    try {
                        id = Long.parseLong(target);
                        if (id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END) {
                            if (hasGroupRight(id, viewerId, ROLE_MEMBER)) {
                                con3 = true;
                                break;
                            }
                        }
                        else {
                            if (StringUtils.equals(viewerId, target)) {
                                con3 = true;
                                break;
                            }
                        }
                    }
                    catch (NumberFormatException nfe) {

                    }
                }
            }

            //gender limit
            boolean con4 = false;
            long limit = poll.getInt("limit_");
            if (limit == 0)
                con4 = true;
            else {
                String gender = getUser(viewerId, viewerId, "gender").getString("gender");
                if (limit == 1)
                    con4 = StringUtils.equals(gender, "m");
                else if (limit == 2)
                    con4 = StringUtils.equals(gender, "f");
            }


            return con1 && con2 && con3 && con4;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getCreatedPolls(String viewerId, String userId, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            String pollIds = toStr(pollLogic.getCreatedPolls(viewerId, userId, page, count));
            return getPolls(viewerId, pollIds, false);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getParticipatedPolls(String viewerId, String userId, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            String pollIds = toStr(pollLogic.getParticipatedPolls(viewerId, userId, page, count));
            return getPolls(viewerId, pollIds, false);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getInvolvedPolls(String viewerId, String userId, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            String pollIds = toStr(pollLogic.getInvolvedPolls(viewerId, userId, page, count));
            return getPolls(viewerId, pollIds, false);
        } finally {
            closeTransceiver(trans);
        }
    }

    public long getRelatedPollCount(String viewerId, String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            long id = 0;
            try {
                id = Long.parseLong(userId);
                if (id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END) {
                    String involvedIds = toStr(pollLogic.getInvolvedPolls(viewerId, userId, -1, -1));
                    Set<String> involved = StringUtils2.splitSet(involvedIds, ",", true);
                    return involved.size();
                }
            }
            catch (NumberFormatException nfe) {

            }
            String createdIds = toStr(pollLogic.getCreatedPolls(viewerId, userId, -1, -1));
            Set<String> created = StringUtils2.splitSet(createdIds, ",", true);
            return created.size();
        } finally {
            closeTransceiver(trans);
        }
    }
    
    public RecordSet getFriendsPolls(String viewerId, String userId, int sort, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            String pollIds = toStr(pollLogic.getFriendsPolls(viewerId, userId, sort, page, count));
            return getPolls(viewerId, pollIds, false);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getPublicPolls(String viewerId, String userId, int sort, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Poll.class);
        try {
            Poll pollLogic = getProxy(Poll.class, trans);
            String pollIds = toStr(pollLogic.getPublicPolls(viewerId, userId, sort, page, count));
            return getPolls(viewerId, pollIds, false);
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record createTag(Record tag) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Tag.class);
        try {
            Tag tags = getProxy(Tag.class, trans);
            tags.createTag(tag.toByteBuffer());
            return tag;
        } finally {
            closeTransceiver(trans);
        }
    }
    public boolean destroyedTag(Record tag) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Tag.class);
        try {
            Tag tags = getProxy(Tag.class, trans);
            return tags.destroyedTag(tag.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet findUserByTag(String tag,int page,int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Tag.class);
        try {
            Tag tags = getProxy(Tag.class, trans);
            return RecordSet.fromByteBuffer(tags.findUserByTag(tag,page,count));
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean hasTag(String user,String tag) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Tag.class);
        try {
            Tag tags = getProxy(Tag.class, trans);
            return tags.hasTag(user, tag);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean hasTarget(String user,String target,String type) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Tag.class);
        try {
            Tag tags = getProxy(Tag.class, trans);
            return tags.hasTarget(user, target, type);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet findTagByUser(String user, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Tag.class);
        try {
            Tag tags = getProxy(Tag.class, trans);
            return RecordSet.fromByteBuffer(tags.findTagByUser(user, page, count));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet findTargetByUser(String user, String type, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Tag.class);
        try {
            Tag tags = getProxy(Tag.class, trans);
            return RecordSet.fromByteBuffer(tags.findTargetsByUser(user, type, page, count));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet findUserTagByTarget(String target, String type, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Tag.class);
        try {
            Tag tags = getProxy(Tag.class, trans);
            return RecordSet.fromByteBuffer(tags.findUserTagByTarget(target, type, page, count));
        } finally {
            closeTransceiver(trans);
        }
    }
    public RecordSet getAppliesToUser(String viewerId, String appId, String userId, String cols) throws AvroRemoteException {
        Transceiver trans = getTransceiver(Stream.class);
        try {
            Stream stream = getProxy(Stream.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(stream.getAppliesToUser(viewerId, appId, userId, cols));
            return transTimelineForQiupu(viewerId, recs, 2, 2,false);
        } finally {
            closeTransceiver(trans);
        }
    }


    public EventThemeLogic getThemeLogic() {
        EventThemeLogic et = new EventThemeLogic();
        et.setConfig(getConfig());
        et.init();
        return et;
    }


    public Map<Long, Record> getEventThemes(long... themeIds) {
        EventThemeLogic et = getThemeLogic();
        try {
            return et.getEventThemes(themeIds);
        } finally {
            et.destroy();
        }
    }

    public Record addEventTheme(long id, long creator, long updatedTime, String name, String imageUrl) {
        EventThemeLogic et = getThemeLogic();
        try {
            return et.addEventTheme(id, creator, updatedTime, name, imageUrl);
        } finally {
            et.destroy();
        }
    }

    public RecordSet getEventThemes(int page, int count) {
        EventThemeLogic et = getThemeLogic();
        try {
            return et.getEventThemes(page, count);
        } finally {
            et.destroy();
        }
    }


    // on bind email
    public void onBindEmain(long viewerId, String email) throws AvroRemoteException {
        CompanyLogic cl = new CompanyLogic(this);
        System.out.println("====================1 " + email);
        cl.joinCompanyOrCreateCompany(viewerId, 0, email); // TODO: xxx
        System.out.println("====================2 " + email);
    }

}

class BuiltinUserExtender extends PlatformExtender {
    private static final Set<String> NECESSARY_COLUMNS = CollectionUtils2.asSet(
            "user_id");

    private static final Set<String> EXTEND_COLUMNS = CollectionUtils2.asSet(
            "remark", "in_circles", "his_friend", "bidi", "friends_count", "followers_count", "favorites_count");


    final Platform platform;


    public BuiltinUserExtender(Platform platform) {
        this.platform = platform;
    }

    @Override
    public Set<String> necessaryColumns() {
        return NECESSARY_COLUMNS;
    }

    @Override
    public Set<String> extendedColumns() {
        return EXTEND_COLUMNS;
    }

    @Override
    protected void extend0(RecordSet recs, Set<String> cols) throws AvroRemoteException {
        if (cols.contains("remark"))
            extendRemark(recs);

        extendCircles(recs, cols);
    }

    private void extendRemark(RecordSet recs) throws AvroRemoteException {
        if (Constants.isNullUserId(viewerId)) {
            for (Record rec : recs)
                rec.put("remark", "");
            return;
        }

        String userIds = recs.joinColumnValues("user_id", ",");
        RecordSet remarks = platform.getRemarks0(viewerId, userIds);
        recs.mergeByKeys("user_id", remarks, "friend", Record.of("remark", ""));
    }

    private static boolean isFriend(JsonNode rel) {
        if (rel == null || rel.size() == 0)
            return false;

        for (int i = 0; i < rel.size(); i++) {
            JsonNode cn = rel.get(i);
            if (cn != null) {
                if (cn.path("circle_id").getValueAsInt() == Constants.BLOCKED_CIRCLE)
                    return false;
            }
        }
        return true;
    }

    private void extendCircles(RecordSet recs, Set<String> cols) throws AvroRemoteException {
        if (Constants.isNullUserId(viewerId)) {
            JsonNodeFactory jnf = JsonNodeFactory.instance;
            for (Record rec : recs) {
                if (cols.contains("in_circles"))
                    rec.put("in_circles", jnf.arrayNode());
                if (cols.contains("his_friend"))
                    rec.put("his_friend", false);
                if (cols.contains("bidi"))
                    rec.put("bidi", false);
            }
            return;
        }

        if (cols.contains("bidi") || cols.contains("in_circles") || cols.contains("his_friend")) {
            if (recs.joinColumnValues("user_id", ",").length() > 0) {
                RecordSet recs_mine = platform.getAllRelation(viewerId, recs.joinColumnValues("user_id", ","), Integer.toString(Constants.FRIENDS_CIRCLE), "mine");
                RecordSet recs_their = platform.getAllRelation(viewerId, recs.joinColumnValues("user_id", ","), Integer.toString(Constants.FRIENDS_CIRCLE), "their");
                if (cols.contains("bidi")) {
                    for (Record rec : recs) {
                        int i_mine = 0;
                        int i_their = 0;
                        for (Record ru : recs_mine) {
                            if (ru.getString("friend").equals(rec.getString("user_id"))) {
                                i_mine += 1;
                            }
                        }
                        for (Record ru : recs_their) {
                            if (ru.getString("user").equals(rec.getString("user_id"))) {
                                i_their += 1;
                            }
                        }
                        boolean b = false;
                        if (i_mine > 0 && i_their > 0)
                            b = true;

                        rec.put("bidi", b);
                    }
                }

                if (cols.contains("in_circles")) {
                    for (Record rec : recs) {
                        RecordSet temp0 = new RecordSet();
                        for (Record ru : recs_mine) {
                            if (ru.getString("friend").equals(rec.getString("user_id"))) {
                                temp0.add(Record.of("circle_id", ru.getString("circle"), "circle_name", ru.getString("name")));
                            }
                        }
                        rec.put("in_circles", temp0.toJsonNode());
                    }
                }

                if (cols.contains("his_friend")) {

                    for (Record rec : recs) {
                        RecordSet temp0 = new RecordSet();
                        int i = 0;
                        boolean b = false;
                        for (Record ru : recs_their) {
                            if (ru.getString("user").equals(rec.getString("user_id"))) {
                                i += 1;
                            }
                        }
                        if (i > 0)
                            b = true;
                        rec.put("his_friend", b);
                    }
                }
            }
        }
    }
}

class BuiltinStreamExtender extends PlatformExtender {
    private static final Set<String> NECESSARY_COLUMNS = CollectionUtils2.asSet(
            "source", "mentions");

    private static final Set<String> EXTEND_COLUMNS = CollectionUtils2.asSet(
            "source", "mentions");


    public BuiltinStreamExtender(Platform platform) {
        setPlatform(platform);
    }

    @Override
    public Set<String> necessaryColumns() {
        return NECESSARY_COLUMNS;
    }

    @Override
    public Set<String> extendedColumns() {
        return EXTEND_COLUMNS;
    }

    @Override
    protected void extend0(RecordSet recs, Set<String> cols) throws AvroRemoteException {
        if (cols.contains("source"))
            extendSource(recs);

        if (cols.contains("mentions"))
            extendMentions(recs);
    }

    protected void extendSource(RecordSet recs) throws AvroRemoteException {
        String sourceUserIds = recs.joinColumnValues("source", ",");
        RecordSet users = platform.getUsers(viewerId, sourceUserIds, "#light");
        recs.mergeJsonByKeys("source", users, "user_id", new Record());
    }

    protected void extendMentions(RecordSet recs) throws AvroRemoteException {
        LinkedHashSet<String> mentionsUserId = new LinkedHashSet<String>();
        for (Record rec : recs)
            mentionsUserId.addAll(StringUtils2.splitList(rec.getString("mentions", ""), ",", true));

        if (mentionsUserId.isEmpty())
            return;

        RecordSet users = platform.getUsers(viewerId, StringUtils.join(mentionsUserId, ","), "#light");
        Map<String, Record> usersMap = users.toRecordMap("user_id");
        for (Record rec : recs) {
            RecordSet mentionsRecs = new RecordSet();
            for (String userId : StringUtils2.splitList(rec.getString("mentions", ""), ",", true)) {
                Record userRec = usersMap.get(userId);
                if (userRec != null)
                    mentionsRecs.add(userRec);
            }
            rec.put("mentions", mentionsRecs.toJsonNode());
        }
    }
}



