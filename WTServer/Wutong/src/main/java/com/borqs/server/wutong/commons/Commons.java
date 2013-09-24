package com.borqs.server.wutong.commons;


import com.borqs.server.ServerException;
import com.borqs.server.base.auth.WebSignatures;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.qiupu.QiupuLogic;
import com.borqs.server.qiupu.QiupuLogics;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.email.EmailModel;
import com.borqs.server.wutong.folder.FolderLogic;
import com.borqs.server.wutong.like.LikeLogic;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commons {
    private static final Logger L = Logger.getLogger(Commons.class);
    public static final Map<String, String> USER_COLUMNS = CollectionUtils2.of(
            "light", AccountLogic.USER_LIGHT_COLUMNS,
            "full", AccountLogic.USER_STANDARD_COLUMNS);
    public static String SERVER_HOST = "api.borqs.com";

    public static boolean sendSms = false;
    public static boolean sendEmail = false;

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

    public static String parseUserColumns(String cols) {
        return expandColumns(cols, USER_COLUMNS, AccountLogic.USER_LIGHT_COLUMNS);
    }

    public static List<String> getEmails(String fromMentions) {
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

    public static JsonNode createArrayNodeFromStrings(String... args) {
        List<String> l = new ArrayList<String>();

        if ((args != null) && (args.length > 0)) {
            for (String arg : args) {
                l.add(arg);
            }
        }

        return JsonUtils.parse(JsonUtils.toJson(l, false));
    }

    public static String getDecodeHeader(HttpServletRequest req, String name, String def, String userId) throws UnsupportedEncodingException {
        String v = req.getHeader(name);
        String getName = StringUtils.isNotEmpty(v) ? java.net.URLDecoder.decode(v, "UTF-8") : def;
        return getName;
    }

    public static List<String> getPhones(String fromMentions) {
        List<String> outPhoneList = new ArrayList<String>();
        if (fromMentions.trim().length() > 0) {
            List<String> m = StringUtils2.splitList(fromMentions, ",", true);
            for (String s : m) {
                if (s.startsWith("*")) {
                    String b = s.substring(1, s.length());
                    if (b.matches("(1[3,5,8][\\d]{9})")) {
                        outPhoneList.add(b);
                    }
                }
            }
        }
        return outPhoneList;
    }

    public static void sendCommentOrLikeEmail(Context ctx, int type, Record user, String target, String message) {
        Record rec = new Record();
        rec.put("type", type);
        rec.put("user", user.toString(false, false));
        rec.put("target", target);
        rec.put("message", message);

        //context cols
        rec.put("viewerId", ctx.getViewerId());
        rec.put("app", ctx.getAppId());
        rec.put("ua", ctx.getUa());
        rec.put("location", ctx.getLocation());
        rec.put("language", ctx.getLanguage());

        String rec_str = rec.toString(false, false);
        L.op(ctx, "rec_str=" + rec_str);
        MQ mq = MQCollection.getMQ("platform");
        if ((mq != null) && (rec_str.length() < 1024))
            mq.send("mail", rec_str);
    }

    public static void sendApkCommentOrLikeEmail(Context ctx, Record user, String target, String message) {
        String viewerId = ctx.getViewerIdString();
        String lang = ctx.getLanguage();

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
        Record apk = thisTrandsGetApkInfo(ctx, viewerId, target, "app_name,upload_user", 1000).getFirstRecord();
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

        RecordSet conversation_users = GlobalLogics.getConversation().getConversation(ctx, Constants.APK_OBJECT, apk.getString("package"), new ArrayList<String>(), 0, 0, 100);
        String cUserIds = conversation_users.joinColumnValues("from_", ",");
        participants = GlobalLogics.getAccount().getUsers(ctx, viewerId, cUserIds, userCols, false);

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
            String displayName = r.getString("display_name");
            Record setting = GlobalLogics.getSetting().getByUsers(ctx, key, userId);
            String value = setting.getString(userId, "0");
            L.op(ctx, "Up to send mail info -> User: " + userId + " Key: " + key + " Value: " + value);
            if (value.equals("0")) {
                emails[0] = r.getString("login_email1");
                emails[1] = r.getString("login_email2");
                emails[2] = r.getString("login_email3");

                for (String email : emails) {
                    if (StringUtils.isNotBlank(email) && !haveSend.contains(email)) {
                        GlobalLogics.getEmail().sendEmail(ctx, title, email, displayName, emailContent, emailType, lang);
                        haveSend.add(email);
                    }
                }
            }
        }
    }

    public static void sendStreamCommentOrLikeEmail(Context ctx, Record user, String target, String message) {
        String viewerId = ctx.getViewerIdString();
        String lang = ctx.getLanguage();
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
        Record post = GlobalLogics.getStream().getPostP(ctx, target, "source,mentions,type,attachments,message");
        String source = post.getString("source");
        Record from = GlobalLogics.getAccount().getUser(ctx, viewerId, source, userCols, false);
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
        RecordSet conversation_users = GlobalLogics.getConversation().getConversation(ctx, Constants.POST_OBJECT, target, new ArrayList<String>(), 0, 0, 100);
        String cUserIds = conversation_users.joinColumnValues("from_", ",");
        participants = GlobalLogics.getAccount().getUsers(ctx, viewerId, cUserIds, userCols, false);

        RecordSet sendTo = new RecordSet();
        L.debug(ctx, "sendTo: " + sendTo);
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
            String displayName = r.getString("display_name");
            Record setting = GlobalLogics.getSetting().getByUsers(ctx, key, userId);
            String value = setting.getString(userId, "0");
            L.op(ctx, "Up to send mail info -> User: " + userId + " Key: " + key + " Value: " + value);
            if (value.equals("0")) {
                emails[0] = r.getString("login_email1");
                emails[1] = r.getString("login_email2");
                emails[2] = r.getString("login_email3");

                for (String email : emails) {
                    if (StringUtils.isNotBlank(email) && !haveSend.contains(email)) {
                        GlobalLogics.getEmail().sendEmail(ctx, title, email, displayName, emailContent, emailType, lang);
                        haveSend.add(email);
                    }
                }
            }
        }
    }

    public static void sendNotification(Context ctx, String nType, JsonNode appId, JsonNode senderId, JsonNode title,
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
        rec.put("scene", Commons.createArrayNodeFromStrings((String)ctx.getSession("scene")));
        AccountLogic accountLogic = GlobalLogics.getAccount();
        Record userInfo = accountLogic.getUsersBaseColumns(ctx,senderId.get(0).getValueAsText()).getFirstRecord();
        String imageUrl = userInfo.getString("image_url");
        JsonNode imageUrlNode = Commons.createArrayNodeFromStrings(imageUrl);
        rec.put("imageUrl", imageUrlNode);

        //context cols
        rec.put("viewerId", ctx.getViewerId());
        rec.put("app", ctx.getAppId());
        rec.put("ua", ctx.getUa());
        rec.put("location", ctx.getLocation());
        rec.put("language", ctx.getLanguage());

        String rec_str = rec.toString(false, false);
        L.debug(ctx, "rec_str=" + rec_str);

        MQ mq = MQCollection.getMQ("platform");
        if (mq != null) {
            if ((rec_str.length() < 1024)) {
                try {
                    mq.send("notif", rec_str);
                } catch (Exception e) {
                    L.error(ctx, e);
                }
            } else {
                if (bodyHtml.size() == 1) {
                    String bodyHtmlText = bodyHtml.get(0).getValueAsText();
                    JsonNode simpleBodyHtml = createArrayNodeFromStrings(StringUtils.substring(bodyHtmlText, 0, 20) + "...");
                    rec.put("bodyHtml", simpleBodyHtml);
                }
                int titleSize = title.size();
                String[] arr = new String[titleSize];
                for (int i = 0; i < titleSize; i++) {
                    arr[i] = title.get(i).getValueAsText();
                    if (arr[i].length() > 200) {
                        arr[i] = StringUtils.substring(arr[i], 0, 20) + "...";
                    }
                }
                rec.put("title", createArrayNodeFromStrings(arr));

                rec_str = rec.toString(false, false);
                L.debug(ctx, "simple rec_str=" + rec_str);

                if ((rec_str.length() < 1024)) {
                    try {
                        mq.send("notif", rec_str);
                    } catch (Exception e) {
                        L.error(ctx, e);
                    }
                }

            }
        }
    }

    public static void sendEmail(Context ctx, EmailModel email) {
        Record r = new Record();
        r.put("sendEmailName", email.getSendEmailName());
        r.put("content", email.getContent());
        r.put("sendEmailPassword", email.getSendEmailPassword());
        r.put("title", email.getTitle());
        r.put("to", email.getTo());
        r.put("username", email.getUsername());
        String rec_str = r.toString(false, false);
        String body = "";
        try {
            body = StringUtils2.compress(rec_str);
        } catch (IOException e) {
            L.error(ctx, e, "compress error!");
        }
        MQ mq = MQCollection.getMQ("platform");

        try {
            if (body.getBytes().length > 3072) {
                L.info(ctx, "------------------------------邮件发送超长--------------------------------" + rec_str);
            }
            mq.send("email", body);
        } catch (Exception e) {
            L.error(ctx, e);
        }
    }
    public static void sendSms(Context ctx, String phone, String text) {
        String smsHost = GlobalConfig.get().getString("phoneVerification.smsHost", null);
        if (smsHost == null)
            throw new ServerException(WutongErrors.SYSTEM_MESSAGE_GATEWAY_HOST_ERROR, "Send sms error");

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
            L.op(ctx, "params=" + params);
        } catch (IOException e) {
            throw new ServerException(WutongErrors.SYSTEM_MESSAGE_GATEWAY_SEND_ERROR, "Send sms error");
        }
    }

    public static RecordSet thisTrandsGetApkInfo(Context ctx, String viewerId, String apps, String cols, int minSDK) {
        QiupuLogic qp = QiupuLogics.getQiubpu();
        RecordSet r = qp.getApps(ctx, apps, removeExtenderColumnQiupu(cols), minSDK);
        return r.size() > 0 ? transDs(ctx, viewerId, r, cols) : new RecordSet();
    }

    public static RecordSet thisTrandsGetSingleApkInfo(Context ctx, String viewerId, String packageName, String cols, int minSDK) {
        QiupuLogic qp = QiupuLogics.getQiubpu();
        RecordSet r = qp.getSingleApps(ctx, packageName, removeExtenderColumnQiupu(cols), minSDK);
        return r.size() > 0 ? transDs(ctx, viewerId, r, cols) : new RecordSet();
    }

    private static String removeExtenderColumnQiupu(String cols) {
        List<String> l = StringUtils2.splitList(cols, ",", true);
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

    private static RecordSet transDs(Context ctx, String viewerId, RecordSet ds, String cols) {
        LikeLogic likeLogic = GlobalLogics.getLike();
        QiupuLogic qp = QiupuLogics.getQiubpu();
        List<String> l = StringUtils2.splitList(cols, ",", true);
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
                            rec.put("upload_user", GlobalLogics.getAccount().getUsers(ctx, rec.getString("upload_user"), "user_id, display_name, image_url, address,remark,perhaps_name").getFirstRecord());
                        } else {
                            rec.put("upload_user", new RecordSet());
                        }
                    }

                    if (l.contains("app_comment_count")) {
                        rec.put("app_comment_count", GlobalLogics.getComment().getCommentCount(ctx, viewerId, targetObjectId));
                    }
                    if (l.contains("app_comments")) {
                        String comment_cols = "comment_id, target, created_time,commenter, commenter_name, message, device, can_like,destroyed_time";
                        RecordSet ds1 = GlobalLogics.getComment().getCommentsForContainsIgnoreP(ctx, viewerId, Constants.APK_OBJECT, rec.getString("apk_id"), comment_cols, false, 0, 2);
                        rec.put("app_comments", ds1);
                    }
                    if (l.contains("app_like_count")) {
                        rec.put("app_like_count", likeLogic.getLikeCount(ctx, targetObjectId));
                    }
                    if (l.contains("app_liked_users")) {
                        RecordSet u = likeLogic.loadLikedUsers(ctx, targetObjectId, 0, 5);
                        rec.put("app_liked_users", GlobalLogics.getAccount().getUsers(ctx, viewerId, u.joinColumnValues("liker", ","), Constants.USER_LIGHT_COLUMNS_QIUPU));
                    }
                    if (l.contains("app_likes")) {
                        String targetId = Constants.objectId(Constants.APK_OBJECT, apk_id);
                        rec.put("app_likes", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, targetId));
                    }
                    if (l.contains("compatibility")) {
                        rec.put("compatibility", true);
                    }

                    if (l.contains("app_used")) {
                        rec.put("app_used", viewerId.equals("") ? false : qp.existUserLinkedApp(ctx, viewerId, rec.getString("package"), ""));
                    }

                    if (l.contains("app_favorite") || l.contains("app_installing") || l.contains("app_installed") || l.contains("app_uninstalled") || l.contains("app_downloaded")) {
                        if (viewerId.equals("")) {
                            rec.put("app_favorite", false);
                            rec.put("app_installing", false);
                            rec.put("app_installed", false);
                            rec.put("app_uninstalled", false);
                            rec.put("app_downloaded", false);
                        } else {
                            int reason = qp.getReasonFromApp(ctx, viewerId, rec.getString("package"));
                            rec.put("app_favorite", (reason & REASON_FAVORITE) != 0);
                            rec.put("app_installing", (reason & REASON_INSTALLING) != 0);
                            rec.put("app_installed", (reason & REASON_INSTALLED) != 0);
                            rec.put("app_uninstalled", (reason & REASON_UNINSTALLED) != 0);
                            rec.put("app_downloaded", (reason & REASON_DOWNLOADED) != 0);
                        }
                    }
                    int maxId = qp.getMaxVersionCode(ctx, package_, 1000);
                    String apkidTemp = package_ + "-" + String.valueOf(maxId) + "-" + arch_;

                    if (l.contains("lasted_version_code")) {
                        rec.put("lasted_version_code", String.valueOf(maxId));
                    }
                    Record rectemp = qp.getSingleApp(ctx, apkidTemp, "version_name", 1000);
                    if (l.contains("lasted_version_name")) {
                        rec.put("lasted_version_name", rectemp.getString("version_name"));
                    }
                }
            }
        }
        return ds;
    }

    public static String firstId(String ids) {
        return StringUtils.substringBefore(ids, ",").trim();
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

    public static String parseAllUsers(String userIds) {
        String returnValue = "";
        if (userIds.length() > 0) {
            List<String> userIds0 = StringUtils2.splitList(userIds, ",", true);
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
    }

    public static RecordSet transUserAddressForQiupu(RecordSet urecs) {
        String outAddr = "";
        for (Record rec : urecs) {
            String addr = rec.getString("address", "");
            if (rec.has("address") && !addr.isEmpty()) {
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

    public static void addImageUrlPrefix(String profileImagePattern, Record rec) {
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

    public static String getAddToUserIds(String message) throws UnsupportedEncodingException {
        String outUserId = "";
        if (message.trim().length() > 0 && message.trim().contains("uid=")) {
            List<String> l = new ArrayList<String>();
            Pattern pat = Pattern.compile("(<A [^>]+>(.+?)<\\/A>)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pat.matcher(message);
            while (matcher.find()) {
                String a = matcher.group();
                if (a.contains("borqs")) {
                    String[] s = a.split("uid=");
                    if (s.length > 0) {
                        String uu = s[1];
                        String uu2 = "";
                        char[] aa = uu.toCharArray();
                        for (int i = 0; i < aa.length - 1; i++) {
                            if (StringUtils.isNumeric(String.valueOf(aa[i]))) {
                                uu2 += String.valueOf(aa[i]);
                            } else {
                                break;
                            }
                        }
/*
                    String[] s1 = s[1].split("'");
                    if (s1.length > 0)   {
                        try {
                            Record r = p.getUser(s1[0],s1[0],"user_id");
                            if (!r.isEmpty())
                               l.add(String.valueOf(s1[0]));
                        } catch (AvroRemoteException e) {
                        }
                    }
                    */
                        l.add(uu2);
                    }
                }
            }
            matcher.reset();
            outUserId = StringUtils.join(l, ",");
        }
        return outUserId;
    }

    public static String parseUserIds(Context ctx, String viewerId, String userIds) {
        StringBuilder buff = new StringBuilder();
        for (String userId : StringUtils2.splitList(userIds, ",", true)) {
            if (userId.startsWith("#")) {
                if (!viewerId.equals("0") && !viewerId.equals("")) {
                    String circleId = StringUtils.removeStart(userId, "#");
                    RecordSet friendRecs = GlobalLogics.getFriendship().getFriends(ctx, viewerId, circleId, 0, -1);
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

    public static String getOldMentions(Context ctx, String userId, String fromMentions) {
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

                    String uid = GlobalLogics.getAccount().findUserIdByUserName(ctx, emailOrPhone);
                    if (StringUtils.isNotBlank(uid) && !StringUtils.equals(uid, "0")) {
                        outUserList.add(uid);
                    }
                    String virtualFriendId = GlobalLogics.getFriendship().getUserFriendHasVirtualFriendId(ctx, userId, emailOrPhone);
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

    public static String composeShareContent(Context ctx, String viewerId, int type, String body, boolean isEmail, String device) {
        String displayName = GlobalLogics.getAccount().getUsersBaseColumns(ctx, viewerId).getFirstRecord().getString("display_name");
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

    public static Record getExifGpsFromJpeg(FileItem fileItem) throws JpegProcessingException, MetadataException {
        Record pics = new Record();
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(fileItem.getInputStream());
            Directory exifGPS = metadata.getDirectory(GpsDirectory.class);
            Iterator tags_pgs = exifGPS.getTagIterator();

            Directory exif = metadata.getDirectory(ExifDirectory.class);
            Iterator tags = exif.getTagIterator();

            while (tags.hasNext()) {
                Tag tag = (Tag) tags.next();
                if (tag.getTagName().contains("Orientation")) {
                    pics.put("orientation", tag.getDescription());
                    break;
                }
            }

            while (tags_pgs.hasNext()) {
                Tag tag0 = (Tag) tags_pgs.next();
                if (tag0.getTagName().equalsIgnoreCase("GPS Latitude Ref")) {
                    pics.put("latitude ref", tag0.getDescription());
                }
                if (tag0.getTagName().equalsIgnoreCase("GPS Latitude")) {
                    pics.put("latitude", tag0.getDescription());
                }
                if (tag0.getTagName().equalsIgnoreCase("GPS Longitude Ref")) {
                    pics.put("longitude ref", tag0.getDescription());
                }
                if (tag0.getTagName().equalsIgnoreCase("GPS Longitude")) {
                    pics.put("longitude", tag0.getDescription());
                }
            }
        } catch (IOException e) {
        }
        return pics;
    }

    public static double formatJWD(String in_jwd) {
        //  116"29'17.13
        double out = 0;
        if (in_jwd.length() > 0) {
            List<String> ss = new ArrayList<String>();
            for (String sss : in_jwd.replaceAll("[^0-9|^\\.]", ",").split(",")) {
                if (sss.length() > 0)
                    ss.add(sss);
            }
            out = Double.parseDouble(ss.get(0)) + Double.parseDouble(ss.get(1)) / 60 + Double.parseDouble(ss.get(2)) / 3600;
        }
        return out;
    }

    public static String parsePostColumns(String cols) {
        return expandColumns(cols, POST_COLUMNS, Constants.POST_FULL_COLUMNS);
    }

    private static final Map<String, String> POST_COLUMNS = CollectionUtils2.of(
            "full", Constants.POST_FULL_COLUMNS);

    public static String formatBucketKey(String content_type) {
        String key = "";
        if (content_type.contains("video/")) {
            key = Constants.bucketName_video_key;
        }
        if (content_type.contains("audio/")) {
            key = Constants.bucketName_audio_key;
        }
        if (content_type.contains("text/") || content_type.contains("application/") || content_type.contains("image/")) {
            key = Constants.bucketName_static_file_key;
        }
        return key;
    }

    public static Record uploadFile(Context ctx, String viewerId, String file_id, long folder_id, FileItem fi, String summary, String description, String content_type, FileItem screen_shot, String file_name) throws UnsupportedEncodingException {
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            Record return_record = new Record();
            //1,取得 文件名和扩展名
            String fileName = file_name;
            if (fileName.equals(""))
                fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
            String expName = "";
            if (fileName.contains(".")) {
                expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
            }
            //2,取得文件类型，
            if (content_type.equals(""))
                content_type = fi.getContentType();
            //3,纠正文件类型
            content_type = Constants.correctContentType(expName, content_type);

            long file_size = fi.getSize();
//            if (file_size > 50 * 1024 * 1024L)
//                throw new BaseException(ErrorCode.DATA_ERROR, "file size is too large");
            String key = formatBucketKey(content_type);
            OssSFS ossStorage = new OssSFS(Constants.bucketName);

            //获取截图url
            String screen_shot_expName = "";
            String new_screen_shot_fileName = "";
            if (screen_shot != null && StringUtils.isNotEmpty(screen_shot.getName())) {
                String screen_shot_fileName = screen_shot.getName().substring(screen_shot.getName().lastIndexOf("\\") + 1, screen_shot.getName().length());
                if (screen_shot_fileName.contains(".")) {
                    screen_shot_expName = screen_shot_fileName.substring(screen_shot_fileName.lastIndexOf(".") + 1, screen_shot_fileName.length());
                }
                if (!screen_shot_expName.equals(""))
                    new_screen_shot_fileName = String.valueOf(DateUtils.nowMillis()) + "." + screen_shot_expName;
                //获取截图url end
            }

            boolean b = false;
            String newFileName = "";

            //先将静态文件写入到数据库
            Record file0 = new Record();
            file0.put("file_id", file_id);
            if (expName.equals("")) {
                newFileName = viewerId + "_" + file_id;
            } else {
                newFileName = viewerId + "_" + file_id + "." + expName;
            }
            file0.put("title", fileName);
            file0.put("summary", summary);
            file0.put("folder_id", folder_id);
            file0.put("description", description);
            file0.put("file_size", file_size);
            file0.put("user_id", viewerId);
            file0.put("exp_name", expName);
            file0.put("html_url", "");
            file0.put("content_type", content_type);
            file0.put("new_file_name", newFileName);
            file0.put("created_time", DateUtils.nowMillis());
            file0.put("updated_time", DateUtils.nowMillis());
            file0.put("destroyed_time", 0);

            b = GlobalLogics.getFile().saveStaticFile(ctx, file0);
            if (b) {
                return_record = file0;
            }
            //3,再区分是不是音频或者视频 根据不同的类型获取不同的属性值，并且写入到数据库
            if (content_type.contains("video/")) {
                Record video0 = new Record();
                video0.put("file_id", file_id);

                video0.put("level", 0);
                video0.put("video_time_length", 0);
                video0.put("video_width", 0);
                video0.put("video_height", 0);
                video0.put("video_data_rate", 0);
                video0.put("video_bit_rate", 0);
                video0.put("video_frame_rate", 0);
                video0.put("audio_bit_rate", 0);
                video0.put("audio_channel", 0);
                video0.put("audio_Sampling_rate", 0);
                video0.put("thumbnail", new_screen_shot_fileName);
                video0.put("coding_type", "");
                video0.put("compression_type", "");
                video0.put("artists", "");
                GlobalLogics.getFile().saveVideo(ctx, video0);
            } else if (content_type.contains("audio/")) {
                Record audio0 = new Record();
                audio0.put("file_id", file_id);

                audio0.put("level", 0);
                audio0.put("schools", "");
                audio0.put("audio_time_length", 0);
                audio0.put("audio_bit_rate", 0);
                audio0.put("author", "");
                audio0.put("audio_artists", "");
                audio0.put("record", "");
                audio0.put("record_author", "");
                audio0.put("record_year", "");
                audio0.put("coding_type", "");
                audio0.put("compression_type", "");

                GlobalLogics.getFile().saveAudio(ctx, audio0);
            }
            if (b && !return_record.isEmpty()) {
                SFSUtils.saveUpload(fi, ossStorage, key + viewerId + "/" + newFileName, fileName);
                if (screen_shot != null && StringUtils.isNotEmpty(screen_shot.getName())) {
                    SFSUtils.saveUpload(screen_shot, ossStorage, key + viewerId + "/" + new_screen_shot_fileName);
                }
            }
            L.debug(ctx, "return_record=" + return_record);
            return return_record;
        } else {
            L.debug(ctx, "file item is null or fi.getName() is empty");
            return new Record();
        }
    }

    public static Record formatFileBucketUrl(Context ctx, String viewerId, Record rec) {
        //http://oss.aliyuncs.com/wutong-photo/%s
        if (!rec.isEmpty()) {
            rec = formatFileBucketUrlForStream(ctx, viewerId, rec);
            String file_id = rec.getString("file_id");

            Record Rec_file_like = new Record();
            String objectFileId = String.valueOf(Constants.FILE_OBJECT) + ":" + String.valueOf(file_id);
            int file_like_count = GlobalLogics.getLike().getLikeCount(ctx, objectFileId);
            Rec_file_like.put("count", file_like_count);
            if (file_like_count > 0) {
                RecordSet recs_liked_users = GlobalLogics.getLike().loadLikedUsers(ctx, objectFileId, 0, 5);
                List<Long> list_file_liked_users = recs_liked_users.getIntColumnValues("liker");
                String likeuids = StringUtils.join(list_file_liked_users, ",");
                RecordSet recs_user_liked = GlobalLogics.getAccount().getUsers(ctx, rec.getString("source"), likeuids, Constants.USER_LIGHT_COLUMNS_LIGHT);
                Rec_file_like.put("users", recs_user_liked);
            } else {
                Rec_file_like.put("users", new Record());//3
            }

            Rec_file_like.put("iliked", viewerId.equals("") ? false : GlobalLogics.getLike().ifuserLikedP(ctx, viewerId, objectFileId));
            rec.put("likes", Rec_file_like);

            Record Rec_comment = new Record();
            int comment_count = GlobalLogics.getComment().getCommentCountP(ctx, viewerId, Constants.FILE_OBJECT, String.valueOf(file_id));
            Rec_comment.put("count", comment_count);
            if (comment_count > 0) {
                RecordSet recs_com = GlobalLogics.getComment().getCommentsForContainsIgnore(ctx, viewerId, Constants.FILE_OBJECT, file_id, Constants.FULL_COMMENT_COLUMNS, false, 0, 2);
                Rec_comment.put("latest_comments", recs_com);
            } else {
                Rec_comment.put("latest_comments", new Record());
            }
            rec.put("comments", Rec_comment);
            return rec;
        } else {
            return new Record();
        }
    }

    public static Record formatFileBucketUrlForStream(Context ctx, String viewerId, Record rec) {
        //http://oss.aliyuncs.com/wutong-photo/%s
        if (!rec.isEmpty()) {
            Configuration conf = GlobalConfig.get();
            String content_type = rec.getString("content_type");
            String key = formatBucketKey(content_type);

            String new_file_name = rec.getString("new_file_name");
            String f0[] = StringUtils.split(new_file_name, "_");
            String v_id = f0.length > 0 ? f0[0].toString() : viewerId;

            rec.put("file_url", String.format(conf.checkGetString("platform.fileUrlPattern") + Constants.bucketName + "/" + key + v_id + "/" + new_file_name));
            String thumbnail = "";
            if (content_type.contains("video")) {
                FolderLogic folderLogic = GlobalLogics.getFile();
                Record rec_video = folderLogic.getVideoById(ctx, rec.getString("file_id"), v_id);
                thumbnail = String.format(conf.checkGetString("platform.fileUrlPattern") + Constants.bucketName + "/" + key + v_id + "/" + rec_video.getString("thumbnail"));
            }
            rec.put("thumbnail_url", thumbnail);
            return rec;
        } else {
            return new Record();
        }
    }

    public static Record getUnifiedUser(Context ctx, long id) {
        int userType = Constants.getUserTypeById(id);
        if (userType == Constants.USER_OBJECT) {
            return GlobalLogics.getAccount().getUser(ctx, ctx.getViewerIdString(), Long.toString(id), AccountLogic.USER_ALL_COLUMNS, true);
        } else if (userType == Constants.PUBLIC_CIRCLE_OBJECT || userType == Constants.EVENT_OBJECT) {
            return GlobalLogics.getGroup().getGroup(ctx, ctx.getViewerIdString(), id, Constants.GROUP_LIGHT_COLS, false);
        } else if (userType == Constants.PAGE_OBJECT) {
            return GlobalLogics.getPage().getPage(ctx, id);
        } else {
            return new Record();
        }
    }

    public static String formatEmailUserName(Context ctx, String userId) {
        Record user = GlobalLogics.getAccount().getUser(ctx, userId, userId,
                "login_email1,login_email2,login_email3,login_phone1,login_phone2,login_phone3,display_name", false);
        String userName = user.getString("display_name");

        String userEmail = user.getString("login_email1", "");
        if (StringUtils.isBlank(userEmail)) {
            userEmail = user.getString("login_email2", "");
            if (StringUtils.isBlank(userEmail))
                userEmail = user.getString("login_email3", "");
        }

        String userPhone = user.getString("login_phone1", "");
        if (StringUtils.isBlank(userPhone)) {
            userPhone = user.getString("login_phone2", "");
            if (StringUtils.isBlank(userPhone))
                userPhone = user.getString("login_phone3", "");
        }

        if (StringUtils.isNotBlank(userEmail)) {
            userName = "<a href=\"mailto:" + userEmail + "\">" + userName + "</a>";
        }
        if (StringUtils.isNotBlank(userPhone)) {
            userName += " (" + userPhone + ") ";
        }

        return userName;
    }

    public static String getEmailActionUrl(Context ctx, String loginName, StringMap qp) {
        qp.put("from_email", "1");
        String sign = WebSignatures.md5Sign("appSecret9", qp.keySet());
        Record rec = GlobalLogics.getAccount().genTicketForEmail(ctx, loginName);
        String userId = rec.getString("user_id");
        String ticket = rec.getString("ticket");
        GlobalLogics.getAccount().saveTicket(ticket, userId, "9", 1);

        qp.put("sign_method", "md5");
        qp.put("appid", "9");
        qp.put("sign", sign);
        qp.put("ticket", ticket);

        HashSet<String> set = new HashSet<String>();
        for (Map.Entry<String, Object> entry : qp.entrySet()) {
            set.add(entry.getKey() + "=" + entry.getValue());
        }

        String url = StringUtils2.joinIgnoreBlank("&", set);
        L.trace(ctx, "email action url: " + url);
        return url;
    }

    public static   RecordSet addIdStrs(RecordSet rs ,String idName){
        if(rs == null)
            return new RecordSet();
        if(rs.size()<1)
            return rs;

        for(Record r:rs){
            r.put(idName+"_s",r.getString(idName));
        }
        return rs;
    }
}
