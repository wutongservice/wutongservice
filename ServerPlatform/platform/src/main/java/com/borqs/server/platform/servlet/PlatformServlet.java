package com.borqs.server.platform.servlet;


import com.borqs.server.ErrorCode;
import com.borqs.server.base.BaseException;
import com.borqs.server.base.auth.WebSignatures;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.io.TextLoader;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.image.ImageException;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.DirectResponse;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.platform.PlatformException;
import com.borqs.server.platform.folder.SimpleFolder;
import com.borqs.server.platform.group.GroupConstants;
import com.borqs.server.platform.group.GroupException;
import com.borqs.server.platform.photo.PhotoException;
import com.borqs.server.platform.photo.SimplePhoto;
import com.borqs.server.platform.poll.PollException;
import com.borqs.server.platform.vote.VoteInfo;
import com.borqs.server.service.notification.GroupInviteNotifSender;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.platform.company.CompanyLogic;
import com.borqs.server.service.platform.company.EmployeeListConstants;
import com.borqs.server.service.platform.excel.InnovExcel;
import com.borqs.server.service.platform.template.InnovTemplate;
import com.borqs.server.service.qiupu.Qiupu;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.borqs.server.platform.group.GroupConstants.*;
import static com.borqs.server.service.platform.Constants.*;
import static com.borqs.server.service.platform.Constants.ROLE_ADMIN;
import static com.borqs.server.service.platform.Constants.ROLE_MEMBER;

public class PlatformServlet extends WebMethodServlet {
    public static final int DEFAULT_USER_COUNT_IN_PAGE = 20;
    private static final Logger L = LoggerFactory.getLogger(PlatformServlet.class);

    private final GenericTransceiverFactory transceiverFactory = new GenericTransceiverFactory();
    private StaticFileStorage profileImageStorage;
    private StaticFileStorage sysIconStorage;
    private StaticFileStorage linkImgStorage;

    private String linkImgAddr;
    private String serverHost;
    private String qiupuUid;
    private String qiupuParentPath;
    private static final PageTemplate pageTemplate = new PageTemplate(PlatformServlet.class);
    private static final PageTemplate pageTemplateInnov = new PageTemplate(InnovTemplate.class);

    private SimplePhoto photo;
    private SimpleFolder folder;
    private StaticFileStorage photoStorage;
    private static String PHOTO_TYPE_SMALL = "small";
    private static String PHOTO_TYPE_ORIGINAL = "original";
    private static String PHOTO_TYPE_LARGE = "large";
    private static int MAX_GUSY_SHARE_TO = 400;
    private static String INNOV_EXCEL_PATH;

    public PlatformServlet() {
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();

        serverHost = conf.getString("server.host", "api.borqs.com");
        qiupuUid = conf.getString("qiupu.uid", "102");
        qiupuParentPath = conf.getString("qiupu.parent", "/home/zhengwei/data/apk/com/borqs/qiupu/");

        transceiverFactory.setConfig(conf);
        transceiverFactory.init();

        profileImageStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.profileImageStorage", ""));
        profileImageStorage.init();

        sysIconStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.sysIconStorage", ""));
        sysIconStorage.init();

        linkImgStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.linkImgStorage", ""));
        linkImgStorage.init();

        linkImgAddr = conf.getString("platform.servlet.linkImgAddr", "");

        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();

        photo = new SimplePhoto();
        photo.setConfig(conf);
        photo.init();

        folder = new SimpleFolder();
        folder.setConfig(conf);
        folder.init();
        INNOV_EXCEL_PATH = conf.getString("platform.servlet.innov_excle_path", "\\home\\zhengwei\\data\\photo\\result.xls");
    }

    @Override
    public void destroy() {
        profileImageStorage.destroy();
        photoStorage.destroy();
        transceiverFactory.destroy();
        super.destroy();
    }

    private Platform platform() {
//        L.trace("---- begin platform constructor");
        Platform p = new Platform(transceiverFactory);
//    	L.trace("---- end platform constructor");
//    	L.trace("---- begin set config");
        p.setConfig(getConfiguration());
//        L.trace("---- end set config");
        return p;
    }

    private Qiupu qiupu() {
        Qiupu q = new Qiupu(transceiverFactory);
        q.setConfig(getConfiguration());
        return q;
    }

    @Override
    protected String getXmlDocumentPath() {
        return "document/platform";
    }

    @Override
    protected String getXmlDocument() {
        return getConfiguration().getBoolean("platform.servlet.document", false)
                ? TextLoader.loadClassPath(PlatformServlet.class, "platform_servlet_document.xml")
                : null;
    }

    private static boolean isDummyEmail(String email) {
        if (!StringUtils.contains(email, "@"))
            return false;

        String domain = StringUtils.substringAfter(email, "@");
        return StringUtils.startsWithIgnoreCase(domain, "aaaaaa");
    }

    @WebMethod("account/create")
    public NoResponse createAccount(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String login_name = "";
        String key = qp.getString("key", "");
        boolean hasKey = StringUtils.isNotBlank(key);
        String login_email1 = qp.getString("login_email", "");
        String login_phone1 = qp.getString("login_phone", "");
//        String login_phone1 = qp.getString("login_email", "");

//        if (qp.getString("login_email", "").length()<=0 || !qp.getString("login_email", "").matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"))
//            throw new BaseException(ErrorCode.PARAM_ERROR, "Must regist as email");

        if (!hasKey && StringUtils.isBlank(login_email1) && StringUtils.isBlank(login_phone1))
            throw new BaseException(ErrorCode.PARAM_ERROR, "Must have parameter 'login_email1' or 'login_phone1'");


        boolean isActive = qp.containsKey("from_id");
        String fromId = qp.getString("from_id", "zdcitkzbfqwmx");
        String pwd = hasKey ? "" : qp.checkGetString("password");
        String appId = qp.getString("appid", NULL_APP_ID);
        String displayName = hasKey ? "" : qp.checkGetString("display_name");
        String nickName = qp.getString("nick_name", "");
        String gender = qp.getString("gender", "u");
        String imei = qp.getString("imei", "");
        String imsi = qp.getString("imsi", "");

//        String ua = getDecodeHeader(req, "User-Agent", "","");
        String ua = "lang=US";
//        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        String lang = "en";
        String loc = getDecodeHeader(req, "location", "", "");

        try {
            p.checkLoginNameNotExists("", login_phone1, login_email1);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.PARAM_ERROR, e.getMessage());
        }
        if (!hasKey && !isActive) {
            if (!isDummyEmail(login_email1)) {
                //send verify email
                if (StringUtils.isNotBlank(login_email1)) {
                    key = FeedbackParams.toSegmentedBase64(true, "/", login_email1, pwd, appId,
                            displayName, nickName, gender, imei, imsi);
                    String url = "http://" + serverHost + "/account/create?key=" + key;

//                String emailContent = "		尊敬的" + displayName + "，请点击以下链接完成注册：<br>"
//                        + "		" + "<a href=\"" + url + "\">" + url + "</a>";
                    String template = Constants.getBundleString(ua, "platformservlet.create.account.email");
                    String emailContent = SQLTemplate.merge(template, new Object[][]{
                            {"displayName", displayName},
                            {"url0", url}
                    });

                    String title = Constants.getBundleString(ua, "platformservlet.create.account.title");
                    p.sendEmail(title, login_email1, login_email1, emailContent, Constants.EMAIL_ESSENTIAL, lang);

                    output(qp, req, resp, "{\"result\":true}", 200, "text/plain");
                    return NoResponse.get();
                }
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
                throw new BaseException(ErrorCode.PARAM_ERROR, "Invaild Invitation");
            }
            if (!StringUtils.equals(fromId, "zdcitkzbfqwmx")) {
                p.checkUserIds(fromId);
            }
        }


        String userId = "";
        try {
            userId = p.createAccount(login_email1, login_phone1,
                    pwd,
                    displayName,
                    nickName,
                    gender,
                    imei,
                    imsi,
                    ua, loc
            );
            String login_content = "";
            login_content = StringUtils.isBlank(login_email1) ? login_phone1 : login_email1;
            p.updateVirtualFriendIdToAct(userId, login_content);
        } catch (Exception e) {
            if (StringUtils.isNotBlank(login_email1)) {
                String msg = e.getMessage();
                String notice = StringUtils.isBlank(msg) ? Constants.getBundleString(ua, "platformservlet.create.account.failed") : msg;
                String html = pageTemplate.merge("notice.freemarker", new Object[][]{
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
                p.setFriends(userId, fromId, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
                p.setFriends(fromId, userId, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
                p.setFriends(userId, fromId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
                p.setFriends(fromId, userId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
            }
            /*
               Qiupu q = qiupu();
               String oldFileName = "com.borqs.qiupu-"
                       + q.getMaxVersionCode("com.borqs.qiupu",1000) + "-arm.apk";
               String filepath = qiupuParentPath + oldFileName;

               //check if file exists
               File obj = new File(filepath);
               if(!obj.exists())
               {
                   throw new BaseException(ErrorCode.GENERAL_ERROR, "BPC apk file is not exist");
               }

               //default filename
               String data = login_name + "abz_seperate_998" + pwd;
               data = URLEncoder.encode(data, "utf-8");
               String fileName = data + "beijj2012.com.borqs.qiupu.apk";

               //write stream
               ServletOutputStream out = resp.getOutputStream();
               resp.setHeader("Content-type", "application/vnd.android.package-archive");
               resp.setHeader("Content-disposition", "attachment; filename=" + fileName);
               BufferedInputStream bis = null;
               BufferedOutputStream bos = null;
               try
               {
                   bis = new BufferedInputStream(new FileInputStream(filepath));
                   bos = new BufferedOutputStream(out);
                   byte[] buff = new byte[2048];
                   int bytesRead;
                   while(-1 != (bytesRead = bis.read(buff, 0, buff.length)))
                   {
                       bos.write(buff, 0, bytesRead);
                   }
               }
               catch(IOException ioe)
               {
                   System.out.println("Download BPC apk file failed");
               }
               finally
               {
                   if(bis != null)
                       bis.close();
                   if(bos != null)
                       bos.close();
               }
           */
            //group
            login_name = StringUtils.isNotBlank(login_email1) ? login_email1 : login_phone1;
            long groupId = qp.getInt("group_id", 0);
            if (groupId != 0) {
                p.updateUserIdByIdentify(userId, login_name);
                p.addMember(groupId, userId, "", ua, loc, appId);
            }

            resp.sendRedirect("http://" + serverHost + "/qiupu/active_down?bind=" + login_name + "&password=" + pwd);
        }

//        return p.login(userId, pwd, appId);

//        String html = pageTemplate.merge("success.html", new HashMap<String, Object>());
//        return DirectResponse.of("text/html", html);
        else if (StringUtils.isNotBlank(login_email1)) {
            String notice = Constants.getBundleString(ua, "platformservlet.create.account.success");
            String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });

            resp.setContentType("text/html");
            resp.getWriter().print(html);

        } else if (StringUtils.isNotBlank(login_phone1)) {
            output(qp, req, resp, "{\"result\":true}", 200, "text/plain");
        }

        return NoResponse.get();
    }

    @WebMethod("account/invite")
    public DirectResponse invite(QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
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

        Platform p = platform();
        RecordSet rs0 = p.getUsers(fromId, fromId, "display_name");
        String fromName = rs0.getFirstRecord().getString("display_name", "");
        RecordSet rs1 = p.getUserIds(login_name);

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
        if (uid != 0l)
            isFriend = p.isFriend(String.valueOf(uid), fromId) ? 1 : 0;

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
    public boolean emailInvite(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
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
        String fromName = p.getUser(viewerId, viewerId, "display_name").getString("display_name");
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
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

            p.sendEmail(title, emails[i], emails[i], emailContent, Constants.EMAIL_ESSENTIAL, lang);
        }

        return true;
    }

    @WebMethod("account/who")
    public long who(QueryParams qp) throws AvroRemoteException {
//    	L.trace("before create platform");
        Platform p = platform();
//        L.trace("after create platform");
        if (qp.containsKey("ticket")) {
            return Long.parseLong(p.whoLogined(qp.checkGetString("ticket")));
        } else if (qp.containsKey("login")) {
//        	L.trace("111");
            long n = 0;
            String user_id = p.getUserIds(qp.checkGetString("login")).getFirstRecord().getString("user_id");
            if (StringUtils.isNotBlank(user_id)) {
                n = Long.parseLong(user_id);
            }
//        	L.trace("222");
            return n;
        } else {
            return 0;
        }
    }

    @WebMethod("place/checkin")
    public Record userSignIn(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");

            String message = Constants.getBundleString(ua, "platform.stream.sign.imhere");
            if (latitude.length() > 0 && latitude.length() > 0)
                p.signIn(viewerId, longitude, latitude, altitude, speed, geo, 0);
            String post_id = p.post(viewerId, Constants.SIGN_IN_POST, message, "[]", String.valueOf(Constants.APP_TYPE_BPC),
                    "", "", qp.getString("app_data", ""), "", false, "", ua, loc, "", "", true, true, true, "");
            return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
        } else {
            throw new BaseException(ErrorCode.PARAM_ERROR, "Must have location");
        }
    }

    @WebMethod("user/shaking")
    public RecordSet userShaking(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                p.signIn(viewerId, longitude, latitude, altitude, speed, geo, 1);
            RecordSet recs = p.getUserShaking(viewerId, longitude, latitude, (int) qp.getInt("page", 0), (int) qp.getInt("count", 100));
            //find who shaking in 3 minutes
            return recs;
        } else {
            throw new BaseException(ErrorCode.PARAM_ERROR, "Must have location");
        }
    }

    @WebMethod("user/nearby")
    public RecordSet userNearBy(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                p.signIn(viewerId, longitude, latitude, altitude, speed, geo, 2);
            RecordSet recs = p.getUserNearBy(viewerId, longitude, latitude, (int) qp.getInt("page", 0), (int) qp.getInt("count", 100));
            //find who shaking in 3 minutes
            return recs;
        } else {
            throw new BaseException(ErrorCode.PARAM_ERROR, "Must have location");
        }
    }

    @WebMethod("user/distance")
    public String getDistance(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();

        double lot1 = 116.4633908;
        double lat1 = 39.9851468;
        double lot2 = 116.4639472;
        double lat2 = 39.9853495;

        double n1 = p.GetDistance(lot1, lat1, lot2, lat2);

        double n2 = p.GetDistance(lot2, lat2, lot1, lat1);

        return "";
    }

    @WebMethod("place/get")
    public RecordSet userGetSignIn(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getSignIn(viewerId, qp.getBoolean("asc", false), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
    }

    @WebMethod("place/remove")
    public boolean userRemoveSignIn(HttpServletRequest req, QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.deleteSignIn(qp.checkGetString("checkin_ids"));
    }

    @WebMethod("user/delete")
    public boolean user_delete(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.destroyAccount(qp.checkGetString("userId"));
    }

    @WebMethod("account/login")
    public Record login(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.login(qp.checkGetString("login_name"), qp.checkGetString("password"), qp.getString("appid", NULL_APP_ID));
    }

    @WebMethod("account/logout")
    public boolean logout(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        p.checkTicket(qp);
        return p.logout(qp.checkGetString("ticket"));
    }

    // TODO: account/change_password

    @WebMethod("account/reset_password")
    public NoResponse resetPassword(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String loginName = qp.getString("login_name", "");
        String key = qp.getString("key", "");

        if (StringUtils.isBlank(loginName) && StringUtils.isBlank(key)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "Must have parameter 'login_name' or 'key'");
        }

//        String ua = getDecodeHeader(req, "User-Agent", "","");
        String ua = "lang=US";
//        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        String lang = "en";
        p.resetPassword(loginName, key, lang);

        if (StringUtils.isBlank(key)) {
            output(qp, req, resp, "{\"result\":true}", 200, "text/plain");
        } else {
            String notice = Constants.getBundleStringByLang(lang, "platformservlet.reset.password.notice");
            String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });
            resp.setContentType("text/html");
            resp.getWriter().print(html);
        }

        return NoResponse.get();
    }

    @WebMethod("account/reset_password_for_phone")
    public String resetPasswordForPhone(QueryParams qp) throws AvroRemoteException {
        String phone = qp.checkGetString("phone");
        Platform p = platform();
        p.resetPasswordForPhone(phone);
        return "OK";
    }

    @WebMethod("account/change_password")
    public boolean changePassword(QueryParams qp, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);

        String oldPassword = qp.checkGetString("oldPassword");
        String newPassword = qp.checkGetString("newPassword");

        if (oldPassword.equals(newPassword))
            throw new BaseException(ErrorCode.PARAM_ERROR, "oldPassword and newPassword is the same!");

        return p.changePassword(viewerId, oldPassword, newPassword);
    }

    @WebMethod("user/show")
    public RecordSet showUsers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        if (ticket != null) {
            viewerId = p.checkSignAndTicket(qp);
        }
        String userIds = qp.checkGetString("users");
        boolean withPublicCircles = qp.getBoolean("with_public_circles", false);

        if (!withPublicCircles)
            return p.getUsers(viewerId, userIds, qp.getString("columns", Platform.USER_LIGHT_COLUMNS_USER_SHOW));
        else {
            RecordSet users = p.getUsers(viewerId, userIds, qp.getString("columns", Platform.USER_LIGHT_COLUMNS_USER_SHOW));

            if (StringUtils.isNotBlank(viewerId)) {
                for (Record user : users) {
                    RecordSet inCircles = RecordSet.fromJson(JsonUtils.toJson(user.get("in_circles"), false));
                    inCircles = p.dealWithInCirclesByGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.ACTIVITY_ID_BEGIN, viewerId, user.getString("user_id"), inCircles);
                    user.put("in_circles", inCircles);
                }
            }

            return users;
        }
    }

    @WebMethod("user/status/update")
    public Record userStatusUpdate(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", "");
        String loc = getDecodeHeader(req, "location", "", viewerId);
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        boolean post = qp.getBoolean("post", true);
        return p.updateUserStatus(qp.getString("user", viewerId), qp.getString("newStatus", ""), ua, loc, post, can_comment, can_like, can_reshare);
    }

    @WebMethod("account/update")
    public boolean updateAccount(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
//        String displayName = p.getUser(viewerId, viewerId, "display_name").getString("display_name");
        Record user = new Record(qp.copy().removeKeys("appid", "sign", "sign_method", "ticket", "callback", "_"));
//        user.putMissing("display_name_temp", displayName);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";

        return p.updateAccount(viewerId, user, lang);
    }

    @WebMethod("account/update_temp")
    public boolean updateAccount_temp(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();

        String viewerId = p.getUserIds(qp.checkGetString("name")).getFirstRecord().getString("user_id");
            if (StringUtils.isNotBlank(viewerId)) {
                        Record user = new Record(qp.copy().removeKeys("appid","name", "sign", "sign_method", "ticket", "callback", "_"));
                        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
                        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
                p.updateAccount22(viewerId, user, lang);

            }
        return true;
    }


    @WebMethod("account/openface/phone")
    public boolean setOpenfacePhone(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String ua = getDecodeHeader(req, "User-Agent", "", "");
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        return p.setMiscellaneous(qp.checkGetString("userid"), qp.checkGetString("phone"), lang);
    }

    @WebMethod("account/openface/user_id")
    public int getOpenfaceUserIdByPhone(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.findUidByMiscellaneous(qp.checkGetString("phone"));
    }

    @WebMethod("account/search")
    public RecordSet searchUsers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", DEFAULT_USER_COUNT_IN_PAGE);
        return p.searchUser(viewerId, qp.checkGetString("username"), page, count);
    }

    @WebMethod("account/upload_profile_image")
    public Record uploadProfileImage(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
//        String displayName = p.getUser(viewerId, viewerId, "display_name").getString("display_name");
        FileItem fi = qp.checkGetFile("profile_image");

        long uploaded_time = DateUtils.nowMillis();
        String imageName = "profile_" + viewerId + "_" + uploaded_time;
        String loc = getDecodeHeader(req, "location", "", viewerId);

        String album_id = qp.getString("album_id", "");
        if (StringUtils.isEmpty(album_id))
            album_id = photo.getAlbum(viewerId, photo.ALBUM_TYPE_PROFILE, "Profile Pictures");
        if (!photo.isAlbumExist(album_id)) {
            throw new PhotoException("album not exist, please create album first");
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

        String photoID = photo.genPhotoId(viewerId);
        Record rc_photo = new Record();
        rc_photo.put("photo_id", photoID);
        rc_photo.put("album_id", album_id);
        rc_photo.put("user_id", viewerId);
        rc_photo.put("img_middle", imageName + "_M.jpg");
        rc_photo.put("img_original", imageName + "_M.jpg");
        rc_photo.put("img_big", imageName + "_L.jpg");
        rc_photo.put("img_small", imageName + "_S.jpg");
        rc_photo.put("caption", "profile_image");
        rc_photo.put("location", loc);
        rc_photo.put("created_time", DateUtils.nowMillis());
        boolean result = photo.saveUploadPhoto(rc_photo);

//        SFSUtils.saveScaledUploadImage(fi, profileImageStorage, sfn, "50", "50", "jpg");
//        SFSUtils.saveScaledUploadImage(fi, profileImageStorage, ofn, "80", "80", "jpg");
//        SFSUtils.saveScaledUploadImage(fi, profileImageStorage, lfn, "180", "180", "jpg");

        Record rc = Record.of("image_url", imageName + "_M.jpg", "small_image_url", imageName + "_S.jpg",
                "large_image_url", imageName + "_L.jpg",
                "original_image_url", imageName + "_M.jpg");
//        rc.putMissing("display_name_temp", displayName);

        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        p.updateAccount(viewerId, rc, lang);
        Record user = p.getUser(viewerId, viewerId, "image_url,small_image_url,large_image_url,original_image_url");
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
        Platform p = platform();
        String userId = p.checkTicket(qp);
        String phone = qp.getString("phone", "");
        String email = qp.getString("email", "");
        if (phone.equals("") && email.equals("")) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "Must have parameter 'phone' or 'email'");
        }
        if (!email.equals("") && !email.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "'email' error");
        }
        if (!phone.equals("") && !phone.matches("(1[\\d]{10})")) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "'phone' error");
        }
        if (!phone.equals("") && !email.equals("")) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "only can bind 'phone' or 'email' one time");
        }
        String key = qp.getString("key", "");
        if (!key.equals(""))
            L.debug("really called " + req.getRequestURI());

        String ua = getDecodeHeader(req, "User-Agent", "", userId);
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        boolean result = p.bindUserSendVerify(userId, phone, email, key, qp.checkGetString("ticket"), lang);

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

            String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });

            resp.setContentType("text/html");
            resp.getWriter().print(html);
        }

        return NoResponse.get();
    }

    @WebMethod("account/invite_bind")
    public boolean accountBindFromInvite(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        Record rec = p.login(qp.checkGetString("borqs_account"), qp.checkGetString("borqs_pwd"), NULL_APP_ID);
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

        String ua = getDecodeHeader(req, "User-Agent", "", userId);
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        String loc = getDecodeHeader(req, "location", "", userId);

        long groupId = qp.getInt("group_id", 0);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));
        if (groupId != 0) {
            p.updateUserIdByIdentify(userId, login_name);
            p.addMember(groupId, userId, "", ua, loc, appId);
        }

        return p.bindUserSendVerify(userId, phone, email, b2, qp.checkGetString("ticket"), lang);
    }

    @WebMethod("user/profile_image")
    public NoResponse downloadProfileImage(QueryParams qp, HttpServletResponse resp) {
        SFSUtils.writeResponse(resp, profileImageStorage, qp.checkGetString("file"));
        return NoResponse.get();
    }

    @WebMethod("privacy/set")
    public boolean setPrivacy(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

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

        return p.setPrivacy(viewerId, privacyItemList);
    }

    @WebMethod("privacy/get")
    public Record getViewerPrivacyConfig(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getViewerPrivacyConfig(viewerId, qp.checkGetString("resources"));
    }

    @WebMethod("preferences/set")
    public boolean setPreferences(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        Record values = new Record();
        Iterator iter = qp.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            String[] buildInParams = new String[]{"sign_method", "sign", "appid", "ticket"};
            if (!ArrayUtils.contains(buildInParams, key)) {
                values.put(key, value);
            }
        }

        return p.setPreferences(viewerId, values);
    }

    @WebMethod("preferences/subscribe")
    public NoResponse subscribeEmail(QueryParams qp, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String user = qp.checkGetString("user");
        String type = qp.checkGetString("type");
        String value = qp.getString("value", "0");

        String userId = p.findUserIdByUserName(user);
        boolean isUser = StringUtils.isNotBlank(userId) && !StringUtils.equals(userId, "0");

        Record values = Record.of(type, value);
        boolean r = isUser ? p.setPreferences(userId, values) : p.setNUserPreferences(user, values);

        String opt = StringUtils.equals(value, "0") ? "订阅" : "退订";
        String notice = r ? opt + "成功！" : opt + "失败，请稍候再试。";
        String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });

        resp.setContentType("text/html");
        resp.getWriter().print(html);

        return NoResponse.get();
    }

    @WebMethod("preferences/get")
    public Record getPreferences(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getPreferences(viewerId, qp.checkGetString("keys"));
    }

    @WebMethod("preferences/get_by_starts")
    public Record getPreferencesByStarts(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getPreferencesByStarts(viewerId, qp.checkGetString("starts"));
    }

    @WebMethod("preferences/get_by_users")
    public Record getPreferencesByUsers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getPreferencesByUsers(qp.checkGetString("key"), qp.checkGetString("users"));
    }

    @WebMethod("circle/create")
    public int createCircle(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return Integer.parseInt(p.createCircle(viewerId, qp.checkGetString("name")));
    }

    @WebMethod("circle/destroy")
    public boolean destroyCircle(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.destroyCircle(viewerId, qp.checkGetString("circles"));
    }

    @WebMethod("circle/update")
    public boolean updateCircleName(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.updateCircleName(viewerId, qp.checkGetString("circle"), qp.checkGetString("name"));
    }

    @WebMethod("circle/show")
    public RecordSet showCircles(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.getString("user", viewerId);
        boolean withPublicCircles = qp.getBoolean("with_public_circles", false);
        if (!withPublicCircles)
            return p.getCircles(userId, qp.getString("circles", ""), qp.getBoolean("with_users", false));
        else {
            String circleIds = qp.getString("circles", "");
            boolean withMembers = qp.getBoolean("with_users", false);
            List<String> circles = StringUtils2.splitList(circleIds, ",", true);
            List<String> groups = p.getGroupIdsFromMentions(circles);
            circles.removeAll(groups);
            RecordSet recs = p.getCircles(userId, StringUtils2.joinIgnoreBlank(",", circles), withMembers);
            RecordSet recs0 = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.ACTIVITY_ID_BEGIN, viewerId, StringUtils2.joinIgnoreBlank(",", groups), GroupConstants.GROUP_LIGHT_COLS, withMembers);
            recs0.renameColumn(GRP_COL_ID, "circle_id");
            recs0.renameColumn(GRP_COL_NAME, "circle_name");
            for (Record rec : recs)
                rec.put("type", CIRCLE_TYPE_LOCAL);
            for (Record rec : recs0)
                rec.put("type", CIRCLE_TYPE_PUBLIC);
            recs.addAll(recs0);
            return recs;
        }
    }

    @WebMethod("friend/usersset")
    public boolean setFriends(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        return p.setFriends(viewerId, qp.checkGetString("friendIds"), qp.checkGetString("circleId"), Constants.FRIEND_REASON_MANUALSELECT, qp.getBoolean("isadd", true), ua, loc);
    }

    @WebMethod("friend/contactset")
    public Record setContactFriends(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        Record rec = p.findUidLoginNameNotInID(qp.checkGetString("content"));
        String fid = "";
        String hasVirtualFriendId = p.getUserFriendhasVirtualFriendId(viewerId, qp.checkGetString("content"));
        if (rec.isEmpty() && hasVirtualFriendId.equals("0")) {
            fid = p.setContactFriend(viewerId, qp.checkGetString("name"), qp.checkGetString("content"), qp.checkGetString("circleIds"), Constants.FRIEND_REASON_MANUALSELECT, ua, loc);
            return p.getUser(viewerId, fid, qp.getString("columns", Platform.USER_LIGHT_COLUMNS));
        } else {
            fid = !rec.isEmpty() ? rec.getString("user_id") : hasVirtualFriendId;
            return p.setFriend(viewerId, fid, qp.checkGetString("circleIds"), Constants.FRIEND_REASON_MANUALSELECT, ua, loc);
        }
    }

    @WebMethod("friend/circlesset")
    public Record setCircles(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        return p.setFriend(viewerId, qp.checkGetString("friendId"), qp.checkGetString("circleIds"), Constants.FRIEND_REASON_MANUALSELECT, ua, loc);
    }

    @WebMethod("friend/exchange_vcard")
    public Record exchangeVcard(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        boolean send_request = qp.getBoolean("send_request", false);
        return p.exchangeVcard(viewerId, qp.checkGetString("friendId"), qp.checkGetString("circleIds"), Constants.FRIEND_REASON_MANUALSELECT, send_request, ua, loc);
    }

    @WebMethod("friend/mutual")
    public NoResponse mutualFriend(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String userId = qp.checkGetString("user_id");
        String fromId = qp.checkGetString("from_id");
        String ua = getDecodeHeader(req, "User-Agent", "", userId);
        String loc = getDecodeHeader(req, "location", "", userId);
        try {
            p.setFriends(userId, fromId, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
            p.setFriends(fromId, userId, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
            p.setFriends(userId, fromId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
            p.setFriends(fromId, userId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_INVITE, true, ua, loc);
        } catch (Exception e) {
            String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                    {"host", serverHost},
                    {"notice", "互相加为好友失败，请稍后再试。"}
            });
            resp.setContentType("text/html");
            resp.getWriter().print(html);

            return NoResponse.get();
        }

        String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                {"host", serverHost},
                {"notice", "互相加为好友成功！"}
        });
        resp.setContentType("text/html");
        resp.getWriter().print(html);

        return NoResponse.get();
    }


    @WebMethod("friend/show")
    public RecordSet showFriends(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.getString("user", viewerId);
        boolean withPublicCircles = qp.getBoolean("with_public_circles", false);
        if (!withPublicCircles)
            return p.getFriends(viewerId, userId,
                    qp.getString("circles", Integer.toString(FRIENDS_CIRCLE)),
                    qp.getString("columns", Platform.USER_STANDARD_COLUMNS),
                    qp.getBoolean("in_public_circles", false),
                    (int) qp.getInt("page", 0),
                    (int) qp.getInt("count", DEFAULT_USER_COUNT_IN_PAGE));
        else
            return p.getFriendsV2(viewerId, userId,
                    qp.getString("circles", Integer.toString(FRIENDS_CIRCLE)),
                    qp.getString("columns", Platform.USER_STANDARD_COLUMNS),
                    (int) qp.getInt("page", 0),
                    (int) qp.getInt("count", DEFAULT_USER_COUNT_IN_PAGE));
    }


    @WebMethod("friend/both")
    public RecordSet getBothFriends(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.checkGetString("user");
        return p.getBothFriends(viewerId, userId,
                (int) qp.getInt("page", 0),
                (int) qp.getInt("count", DEFAULT_USER_COUNT_IN_PAGE));
    }

    @WebMethod("follower/show")
    public RecordSet showFollowers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.getString("user", viewerId);
        return p.getFollowers(viewerId, userId,
                qp.getString("circles", Integer.toString(FRIENDS_CIRCLE)),
                qp.getString("columns", Platform.USER_STANDARD_COLUMNS),
                (int) qp.getInt("page", 0),
                (int) qp.getInt("count", DEFAULT_USER_COUNT_IN_PAGE));
    }


    @WebMethod("relation/get")
    public RecordSet getRelation(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.getString("source", viewerId);
        return p.getRelation(userId, qp.checkGetString("target"), qp.getString("circle", Integer.toString(FRIENDS_CIRCLE)));
    }


    @WebMethod("relation/bidi")
    public Record getBidiRelation(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.getString("source", viewerId);
        return p.getBidiRelation(userId, qp.checkGetString("target"), qp.getString("circle", Integer.toString(FRIENDS_CIRCLE)));
    }


    @WebMethod("remark/set")
    public boolean setUserRemark(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.setRemark(viewerId, qp.checkGetString("friend"), qp.getString("remark", ""));
    }

    private RecordSet dealWithGroupPhoto(Platform p, Record viewerPhotoRec, List<String> groupIds) throws AvroRemoteException {
        RecordSet recs = new RecordSet();
        String viewerId = viewerPhotoRec.getString("user_id");
        recs.add(viewerPhotoRec);

        RecordSet groups = p.getGroups(0, 0, viewerId, StringUtils2.joinIgnoreBlank(",", groupIds), GroupConstants.GROUP_LIGHT_COLS, false);
        for (Record group : groups) {
            String groupId = group.getString("id", "0");
            String groupName = group.getString("name", "Default");
            Record rec = viewerPhotoRec.copy();
            rec.put("user_id", groupId);
            rec.put("album_id", photo.getAlbum(groupId, photo.ALBUM_TYPE_GROUP, groupName));
            recs.add(rec);

            Record viewerGroupPhoto = viewerPhotoRec.copy();
            viewerGroupPhoto.put("album_id", photo.getAlbum(viewerId, photo.ALBUM_TYPE_TO_GROUP, groupName));
            recs.add(viewerGroupPhoto);
        }

        return recs;
    }

    private RecordSet dealWithGroupFile(Platform p, Record viewerFileRec, List<String> groupIds) throws AvroRemoteException {
        RecordSet recs = new RecordSet();
        String viewerId = viewerFileRec.getString("user_id");
        recs.add(viewerFileRec);

        RecordSet groups = p.getGroups(0, 0, viewerId, StringUtils2.joinIgnoreBlank(",", groupIds), GroupConstants.GROUP_LIGHT_COLS, false);
        for (Record group : groups) {
            String groupId = group.getString("id", "0");
            String groupName = group.getString("name", "Default");
            Record rec = viewerFileRec.copy();
            rec.put("user_id", groupId);
            rec.put("folder_id", folder.getFolder(groupId, folder.FOLDER_TYPE_GROUP, groupName));
            recs.add(rec);

            Record viewerGroupPhoto = viewerFileRec.copy();
            viewerGroupPhoto.put("folder_id", folder.getFolder(viewerId, folder.FOLDER_TYPE_TO_GROUP, groupName));
            recs.add(viewerGroupPhoto);
        }

        return recs;
    }

    @WebMethod("post/create")
    public Record createPost(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        L.debug("post.create begin============");
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        L.debug("viewerId============" + viewerId);
        String app_data = qp.getString("app_data", "");

        String url = "";
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);

        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                p.signIn(viewerId, longitude, latitude, altitude, speed, geo, 2);
        }

        String post_id = "";
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);

        String add_to = getAddToUserIds(qp.checkGetString("msg"));
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);
        List<String> groupIds = new ArrayList<String>();
        List<String> pids = new ArrayList<String>();
        String tmp_ids = "";
        if (mentions.length() > 0) {
            List<String> l0 = StringUtils2.splitList(mentions, ",", true);
            if (l0.contains("#-2")) {
                l0.remove("#-2");
                mentions = StringUtils.join(l0, ",");
            } else {
//                privacy = true;
            }

            //group
            groupIds = p.getGroupIdsFromMentions(l0);
            for (String groupId : groupIds) {
                l0.remove("#" + groupId);
                l0.remove(groupId);
                Record groupRec = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.GROUP_ID_END,
                        groupId, COL_CAN_MEMBER_POST).getFirstRecord();
                long canMemberPost = groupRec.getInt(COL_CAN_MEMBER_POST, 1);
                if ((canMemberPost == 1 && p.hasGroupRight(Long.parseLong(groupId), viewerId, ROLE_MEMBER))
                        || (canMemberPost == 0 && p.hasGroupRight(Long.parseLong(groupId), viewerId, ROLE_ADMIN))
                        || canMemberPost == 2) {
                    l0.add(groupId);
                }
            }
            mentions = StringUtils.join(l0, ",");
            tmp_ids = p.parseUserIds(viewerId, mentions);
            List<String> l = StringUtils2.splitList(tmp_ids, ",", true);
            if (l.size() > MAX_GUSY_SHARE_TO)
                throw new BaseException(ErrorCode.PARAM_ERROR, "Only can share to less than 400 guys!");
        }
        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new BaseException(ErrorCode.PARAM_ERROR, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new BaseException(ErrorCode.GROUP_ERROR, "You don't have right to post!");

        FileItem fi = qp.getFile("photo_image");
        String photo_id = qp.getString("photo_id", "");
        if (fi != null && StringUtils.isNotEmpty(fi.getName()) && photo_id.equals("")) {
            String fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
            String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());

            if (!fi.getContentType().contains("image/"))
                throw new PhotoException("file type error,not image");

            String album_id = photo.getAlbum(viewerId, photo.ALBUM_TYPE_SHARE_OUT, "Sharing Pictures");
            String path = photo.getPhotoPath(viewerId, album_id);
            if (!(photoStorage instanceof OssSFS)) {
                File file = new File(path);
                if (!file.exists()) {
                    file.mkdir();
                }
            }
            if (!photo.isAlbumExist(album_id)) {
                throw new PhotoException("album not exist, please create album first");
            }

            String photoID = photo.genPhotoId(viewerId);
            String caption = qp.getString("caption", "");
            String imageName = viewerId + "_" + album_id + "_" + photoID;

            String longitude = "";
            String latitude = "";
            String orientation = "";
            Record extendExif = new Record();
            if (expName.equalsIgnoreCase("jpg") || expName.equalsIgnoreCase("jpeg")) {
                try {
                    extendExif = getExifGpsFromJpeg(fi);
                } catch (JpegProcessingException e) {
                } catch (MetadataException e) {
                }
                if (!extendExif.isEmpty()) {
                    if (extendExif.has("longitude"))
                        longitude = String.valueOf(formatJWD(extendExif.getString("longitude")));
                    if (extendExif.has("latitude"))
                        latitude = String.valueOf(formatJWD(extendExif.getString("latitude")));
                    if (extendExif.has("orientation"))
                        orientation = extendExif.getString("orientation");
                }
            }


            Record rc = new Record();
            rc.put("photo_id", photoID);
            rc.put("album_id", album_id);
            rc.put("user_id", viewerId);
            rc.put("img_middle", imageName + "_O." + expName);
            rc.put("img_original", imageName + "_O." + expName);
            rc.put("img_big", imageName + "_L." + expName);
            rc.put("img_small", imageName + "_S." + expName);
            rc.put("caption", caption);
            rc.put("created_time", DateUtils.nowMillis());
            rc.put("location", loc);
            rc.put("tag", qp.getString("tag", ""));
            rc.put("from_user", viewerId);
            rc.put("original_pid", photoID);
            rc.put("longitude", longitude);
            rc.put("latitude", latitude);
            rc.put("orientation", orientation);

            saveUploadPhoto(fi, imageName, path, rc);
            L.trace("save upload photo success");
//            boolean result = photo.saveUploadPhoto(rc);
            RecordSet group_recs = new RecordSet();
            if (!groupIds.isEmpty()) {
                group_recs = dealWithGroupPhoto(p, rc, groupIds);
                pids.add(photoID);
            }

            boolean result = groupIds.isEmpty() ? photo.saveUploadPhoto(rc) : photo.saveUploadPhotos(group_recs);
//            boolean result = photo.saveUploadPhoto(rc);
            pids.add(photoID);
            //然后保存在mentions列表中的received相册中
            List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < 10)
                        l00.add(l011);
                }
            }
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= 10) {
                        try {
                            String other_album_id = photo.getAlbum(uid, photo.ALBUM_TYPE_RECEIVED, "Received Pictures");
                            String path00 = photo.getPhotoPath(uid, other_album_id);
                            if (!(photoStorage instanceof OssSFS)) {
                                File file0 = new File(path00);
                                if (!file0.exists()) {
                                    file0.mkdir();
                                }
                            }

                            Record rc00 = new Record();
                            rc00.put("photo_id", photoID);
                            rc00.put("album_id", other_album_id);
                            rc00.put("user_id", uid);
                            rc00.put("img_middle", imageName + "_O." + expName);
                            rc00.put("img_original", imageName + "_O." + expName);
                            rc00.put("img_big", imageName + "_L." + expName);
                            rc00.put("img_small", imageName + "_S." + expName);
                            rc00.put("caption", "");
                            rc00.put("created_time", DateUtils.nowMillis());
                            rc00.put("location", loc);
                            rc00.put("tag", "");
                            rc00.put("from_user", viewerId);
                            rc00.put("original_pid", photoID);
                            rc00.put("longitude", longitude);
                            rc00.put("latitude", latitude);
                            rc00.put("orientation", orientation);
                            photo.saveUploadPhoto(rc00);
//                        pids.add(photoID00);
                        } catch (Exception e) {
                        }
                    }
                }
            }


            if (result) {
                Record sRecord = new Record();
                Configuration conf = getConfiguration();
                Record album = photo.getAlbumOriginal(album_id);
                sRecord.put("album_id", album.getString("album_id"));
                sRecord.put("album_name", album.getString("title"));
                sRecord.put("photo_id", photoID);

                sRecord.put("album_photo_count", 0);
                sRecord.put("album_cover_photo_id", 0);
                sRecord.put("album_description", "");
                sRecord.put("album_visible", false);

                if (photoStorage instanceof OssSFS) {
                    sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_L." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                    sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_S." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                    sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_T." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                } else {
                    sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + imageName + "_L." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                    sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + imageName + "_S." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                    sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + imageName + "_T." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                }

                sRecord.put("photo_caption", rc.getString("caption"));
                sRecord.put("photo_location", rc.getString("location"));
                sRecord.put("photo_tag", rc.getString("tag"));
                sRecord.put("photo_created_time", rc.getString("created_time"));
                sRecord.put("longitude", rc.getString("longitude"));
                sRecord.put("latitude", rc.getString("latitude"));
                sRecord.put("orientation", rc.getString("orientation"));

                String msg = qp.getString("msg", "");
                L.debug("long message=" + msg);
                post_id = p.post(viewerId, Constants.PHOTO_POST, msg, sRecord.toString(false, false), qp.getString("appid", "1"),
                        "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to);
                if (pids.size() > 0 && !post_id.equals(""))
                    photo.updatePhotoStreamId(post_id, pids);
            }
        } else if (fi == null && !photo_id.equals("")) {
            Record old_photo = photo.getPhotoByIds(photo_id).getFirstRecord();
            if (old_photo.isEmpty())
                throw new PhotoException("this photo is not exist, author has deleted");
            String album_id = photo.getAlbum(viewerId, photo.ALBUM_TYPE_SHARE_OUT, "Sharing Pictures");

            Record rc = new Record();
            rc.put("photo_id", photo_id);
            rc.put("album_id", album_id);
            rc.put("user_id", viewerId);
            rc.put("img_middle", old_photo.getString("img_middle"));
            rc.put("img_original", old_photo.getString("img_original"));
            rc.put("img_big", old_photo.getString("img_big"));
            rc.put("img_small", old_photo.getString("img_small"));
            rc.put("caption", old_photo.getString("caption"));
            rc.put("created_time", DateUtils.nowMillis());
            rc.put("location", old_photo.getString("location"));
            rc.put("tag", old_photo.getString("tag"));
            rc.put("longitude", old_photo.getString("longitude"));
            rc.put("latitude", old_photo.getString("latitude"));
            rc.put("orientation", old_photo.getString("orientation"));

            boolean result = groupIds.isEmpty() ? photo.saveUploadPhoto(rc) : photo.saveUploadPhotos(dealWithGroupPhoto(p, rc, groupIds));
            L.trace("save upload photo to db success");
            pids.add(photo_id);
            List<String> l00 = StringUtils2.splitList(mentions, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < 10)
                        l00.add(l011);
                }
            }
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= 10) {
                        String other_album_id = photo.getAlbum(uid, photo.ALBUM_TYPE_RECEIVED, "Received Pictures");

                        Record rc00 = new Record();
                        rc00.put("photo_id", photo_id);
                        rc00.put("album_id", other_album_id);
                        rc00.put("user_id", uid);
                        rc00.put("img_middle", old_photo.getString("img_middle"));
                        rc00.put("img_original", old_photo.getString("img_original"));
                        rc00.put("img_big", old_photo.getString("img_big"));
                        rc00.put("img_small", old_photo.getString("img_small"));
                        rc00.put("caption", old_photo.getString("caption"));
                        rc00.put("created_time", DateUtils.nowMillis());
                        rc00.put("location", old_photo.getString("location"));
                        rc00.put("tag", old_photo.getString("tag"));
                        rc00.put("longitude", old_photo.getString("longitude"));
                        rc00.put("latitude", old_photo.getString("latitude"));
                        rc00.put("orientation", old_photo.getString("orientation"));
                        photo.saveUploadPhoto(rc00);
                    }
                }
            }

            Record album = photo.getAlbumOriginal(album_id);
            Record sRecord = new Record();
            Configuration conf = getConfiguration();

            sRecord.put("album_id", album.getString("album_id"));
            sRecord.put("album_name", album.getString("title"));
            sRecord.put("photo_id", photo_id);

            sRecord.put("album_photo_count", 0);
            sRecord.put("album_cover_photo_id", 0);
            sRecord.put("album_description", "");
            sRecord.put("album_visible", false);

            if (photoStorage instanceof OssSFS) {
                sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_big")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_small")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original").replace("O", "T")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
            } else {
                sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_big")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), viewerId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original").replace("O", "T")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
            }
            sRecord.put("photo_caption", rc.getString("caption"));
            sRecord.put("photo_location", rc.getString("location"));
            sRecord.put("photo_tag", rc.getString("tag"));
            sRecord.put("photo_created_time", rc.getString("created_time"));
            sRecord.put("longitude", rc.getString("longitude"));
            sRecord.put("latitude", rc.getString("latitude"));
            sRecord.put("orientation", rc.getString("orientation"));

            String msg = qp.getString("msg", "share photo");
            post_id = p.post(viewerId, Constants.PHOTO_POST, msg, sRecord.toString(false, false), qp.getString("appid", "1"),
                    "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to);
            if (pids.size() > 0 && !post_id.equals(""))
                photo.updatePhotoStreamId(post_id, pids);
        } else {
            String m = qp.checkGetString("msg");
            m = StringUtils.replace(m, "'", "");
            Pattern pat2 = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\!\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\!\\@?^=%&amp;/~\\+#])?", Pattern.CASE_INSENSITIVE);
            Matcher matcher2 = pat2.matcher(m);
            if (!matcher2.find()) {
                post_id = p.post(viewerId, (int) qp.getInt("type", 1), qp.checkGetString("msg"), qp.getString("attachments", "[]"), qp.checkGetString("appid"),
                        qp.getString("package", ""), qp.getString("apkId", ""), app_data, mentions, privacy, Qiupu.QAPK_COLUMNS, ua, loc, "", "", can_comment, can_like, can_reshare, add_to);
            } else {
                url = matcher2.group();
                if (url.trim().equals(qp.checkGetString("msg").trim()))
                    m = "";
                post_id = p.sendShareLink(viewerId, m, qp.checkGetString("appid"),
                        mentions, app_data, privacy, ua, loc, url, "", linkImgAddr, can_comment, can_like, can_reshare, add_to);
            }
            matcher2.reset();
        }
        L.debug("post send end and post_id=" + post_id);
        return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
    }

    @WebMethod("post/share_apk")
    public Record createApkPost(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        String app_data = qp.getString("app_data", "");

        String apkId = qp.checkGetString("apkId");

        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                p.signIn(viewerId, longitude, latitude, altitude, speed, geo, 2);
        }
        String post_id = "";
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);

        String msg = qp.getString("msg", "");
        msg = StringUtils.replace(msg, "'", "");
        String add_to = "";
        if (msg.length() > 0)
            add_to = getAddToUserIds(msg);
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);

        List<String> groupIds = new ArrayList<String>();
        StringBuilder changeMentions = new StringBuilder();
        if (getUserAndGroup(changeMentions, p, mentions, groupIds, viewerId)) {
            mentions = changeMentions.toString();
            String ids = p.parseUserIds(viewerId, mentions);
            List<String> l = StringUtils2.splitList(ids, ",", true);
            if (l.size() > MAX_GUSY_SHARE_TO)
                throw new BaseException(ErrorCode.PARAM_ERROR, "Only can share to less than 400 guys!");
        }

        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new BaseException(ErrorCode.PARAM_ERROR, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new BaseException(ErrorCode.GROUP_ERROR, "You don't have right to post!");
        post_id = p.post(viewerId, APK_POST, msg, "", qp.checkGetString("appid"),
                qp.getString("package", ""), apkId, app_data, mentions, privacy, Qiupu.QAPK_COLUMNS, ua, loc, "", "", can_comment, can_like, can_reshare, add_to);
        return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
    }

    @WebMethod("feedback/create")
    public Record postFeedBack(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String post_id = "";
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        post_id = p.post(viewerId, 1, qp.checkGetString("msg"), "", qp.checkGetString("appid"),
                "", "", qp.getString("app_data", ""), qiupuUid, true, "", ua, loc, "", "", can_comment, can_like, can_reshare, "");
        return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
    }

    static boolean getUserAndGroup(StringBuilder retMentions, final Platform p, final String mentions, List<String> groupIds, String viewerId) throws AvroRemoteException {
        String tmp = "";
        if (mentions != null && mentions.length() > 0) {
            List<String> l0 = StringUtils2.splitList(mentions, ",", true);
            if (l0.contains("#-2")) {
                l0.remove("#-2");
                tmp = StringUtils.join(l0, ",");
            } else {
//                privacy = true;
            }
            //group
            groupIds = p.getGroupIdsFromMentions(l0);
            for (String groupId : groupIds) {
                l0.remove("#" + groupId);
                l0.remove(groupId);
                Record groupRec = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.GROUP_ID_END,
                        groupId, COL_CAN_MEMBER_POST).getFirstRecord();
                long canMemberPost = groupRec.getInt(COL_CAN_MEMBER_POST, 1);
                if ((canMemberPost == 1 && p.hasGroupRight(Long.parseLong(groupId), viewerId, ROLE_MEMBER))
                        || (canMemberPost == 0 && p.hasGroupRight(Long.parseLong(groupId), viewerId, ROLE_ADMIN))
                        || canMemberPost == 2) {
                    l0.add(groupId);
                }
            }
            tmp = StringUtils.join(l0, ",");


            retMentions.append(tmp);
            return true;
        }

        return false;
    }

    @WebMethod("link/create")
    public Record createLinkPost(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String url = qp.checkGetString("url");
        String app_data = qp.getString("app_data", "");
        String title = qp.getString("title", "");
        String msg = qp.getString("msg", "");
        Pattern pat1 = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\!\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\!\\@?^=%&amp;/~\\+#])?", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pat1.matcher(url);
        while (matcher1.find()) {
            url = matcher1.group();
            if (url.length() > 0)
                break;
        }
        matcher1.reset();
        if (url.length() < 5)
            throw new BaseException(ErrorCode.PARAM_ERROR, "url error");

        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                p.signIn(viewerId, longitude, latitude, altitude, speed, geo, 2);
        }
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        String add_to = getAddToUserIds(msg);
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);

        List<String> groupIds = new ArrayList<String>();
        StringBuilder changeMentions = new StringBuilder();
        if (getUserAndGroup(changeMentions, p, mentions, groupIds, viewerId)) {
            mentions = changeMentions.toString();
            String ids = p.parseUserIds(viewerId, mentions);
            List<String> l = StringUtils2.splitList(ids, ",", true);
            if (l.size() > MAX_GUSY_SHARE_TO)
                throw new BaseException(ErrorCode.PARAM_ERROR, "Only can share to less than 400 guys!");
        }

        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new BaseException(ErrorCode.PARAM_ERROR, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new BaseException(ErrorCode.GROUP_ERROR, "You don't have right to post!");
        String post_id = p.sendShareLink(viewerId, msg, qp.checkGetString("appid"),
                mentions, app_data, privacy, ua, loc, url, title, linkImgAddr, can_comment, can_like, can_reshare, add_to);
        return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
    }

    @WebMethod("post/delete")
    public boolean destroyPosts(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.destroyPosts(viewerId, qp.getString("postIds", ""));
    }

    @WebMethod("link/longurl")
    public void getLongUrl(QueryParams qp, HttpServletResponse response) throws IOException {
        Platform p = platform();
        String param = qp.checkGetString("short_url");
        param = StringUtils.substringBefore(param, "\\");
        Configuration conf = getConfiguration();
        if (!param.toUpperCase().startsWith("HTTP://"))
            param = "http://" + param;
        String long_url = p.getLongUrl(param);
        try {
            response.sendRedirect(long_url);
        } catch (IOException e) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "url error");
        }
    }

    @WebMethod("link/expired")
    public NoResponse linkExpiredPage(QueryParams qp, HttpServletResponse resp) throws IOException {
        String notice = "对不起，您请求的短网址已过期。";
        String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });

        resp.setContentType("text/html");
        resp.getWriter().print(html);

        return NoResponse.get();
    }

    @WebMethod("post/repost")
    public Record rePost(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        String add_to = getAddToUserIds(qp.getString("newmsg", ""));
        boolean privacy = qp.getBoolean("secretly", false);
        if (privacy == true) {
            qp.checkGetString("to");
        }
        String mentions = qp.getString("to", "");

        List<String> groupIds = new ArrayList<String>();
        StringBuilder changeMentions = new StringBuilder();
        if (getUserAndGroup(changeMentions, p, mentions, groupIds, viewerId)) {
            mentions = changeMentions.toString();
            String ids = p.parseUserIds(viewerId, mentions);
            List<String> l = StringUtils2.splitList(ids, ",", true);
            if (l.size() > MAX_GUSY_SHARE_TO)
                throw new BaseException(ErrorCode.PARAM_ERROR, "Only can share to less than 400 guys!");
        }

        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new BaseException(ErrorCode.PARAM_ERROR, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new BaseException(ErrorCode.GROUP_ERROR, "You don't have right to post!");

        String post_id = p.repost(viewerId, mentions, privacy, qp.checkGetString("postId"), qp.getString("newmsg", ""), ua, loc, qp.getString("app_data", ""), can_comment, can_like, can_reshare, add_to);
        return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
    }

    @WebMethod("post/update")
    public boolean updatePost(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.updatePost(viewerId, qp.checkGetString("postId"), qp.getString("msg", ""));
    }

    @WebMethod("post/updateaction")
    public boolean updateAction(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        String can_comment = qp.getString("can_comment", null);
        String can_like = qp.getString("can_like", null);
        String can_reshare = qp.getString("can_reshare", null);

        Record rec = new Record();
        if (can_comment != null)
            rec.put("can_comment", can_comment);
        if (can_like != null)
            rec.put("can_like", can_like);
        if (can_reshare != null)
            rec.put("can_reshare", can_reshare);

        return p.updateStreamCanCommentOrcanLike(qp.checkGetString("postId"), viewerId, rec);
    }

    @WebMethod("post/get")
    public RecordSet getPosts(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        p.checkSignAndTicket(qp);
        return p.getPosts(qp.checkGetString("postIds"), qp.getString("cols", Platform.POST_FULL_COLUMNS));
    }

    @WebMethod("post/qiupuget")
    public RecordSet getPostsForQiuPu(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        List<String> postIds0 = StringUtils2.splitList(qp.checkGetString("postIds"), ",", true);
        if (postIds0.size() == 1) {
            Record rec = p.findStreamTemp(postIds0.get(0), "destroyed_time");
            if (rec.isEmpty()) {
                return new RecordSet();
            } else {
                long destroyed_time = rec.getInt("destroyed_time");
                if (destroyed_time > 0)
                    throw Errors.createResponseError(ErrorCode.POST_HAS_DELETED, "The Post has deleted", postIds0.get(0));
            }
        }
        boolean single_get = true;
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return p.getFullPostsForQiuPu(viewerId, qp.checkGetString("postIds"), single_get);
        } else {
            return p.getPostsForQiuPu(viewerId, qp.checkGetString("postIds"), qp.checkGetString("cols"), single_get);
        }
    }

    @WebMethod("post/report_abuse")
    public boolean reportAbuserCreate(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String post_id = qp.checkGetString("post_id");
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        return p.reportAbuserCreate(viewerId, post_id, ua, loc);
    }

    @WebMethod("post/publictimeline")
    public RecordSet getPublicTimeline(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = qp.containsKey("ticket") ? p.checkSignAndTicket(qp) : NULL_USER_ID;

        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", ALL_POST);
        String appId = qp.checkGetString("appid");

        if (qp.containsKey("cols")) {
            return p.getPublicTimeline(viewerId, qp.checkGetString("cols"), since, max, type, appId, page, count);
        } else {
            return p.getFullPublicTimeline(viewerId, since, max, type, appId, page, count);
        }
    }

    @WebMethod("post/hot")
    public RecordSet getHotStreams(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        long max = (int) qp.getInt("end_time", 0);
        long min = (int) qp.getInt("start_time", 0);
        int type = (int) qp.getInt("type", ALL_POST);

        if (max == 0)
            max = DateUtils.nowMillis();
        if (min == 0) {
            long dateDiff = 24 * 60 * 60 * 1000 * 30L;
            min = max - dateDiff;
        }

        if (count > 100)
            count = 100;
        String circle_id = qp.getString("circle_id", "");
        return p.getHotStream(viewerId, circle_id, qp.getString("cols", ""), type, max, min, page, count);
    }

    @WebMethod("post/qiupupublictimeline")
    public RecordSet getQiupuPublicTimeline(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = qp.containsKey("ticket") ? p.checkSignAndTicket(qp) : NULL_USER_ID;
        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", ALL_POST);
        String appId = qp.checkGetString("appid");

        if (qp.containsKey("cols")) {
            return p.getPublicTimelineForQiuPu(viewerId, qp.checkGetString("cols"), since, max, type, appId, page, count);
        } else {
            return p.getFullPublicTimelineForQiuPu(viewerId, since, max, type, appId, page, count);
        }
    }

//    @WebMethod("post/updateacctchments")
//    public boolean updateAcctchment() throws AvroRemoteException {
//        Platform p = platform();
//        Qiupu q = qiupu();
//        return p.updatePostAttachments(q.QAPK_COLUMNS);
//    }

    @WebMethod("post/userstimeline")
    public RecordSet getUsersTimeline(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        long maxt = qp.getInt("end_time", 0) <= 0 ? DateUtils.nowMillis() : qp.getInt("end_time", 0);
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return p.getFullUsersTimeline(viewerId, qp.checkGetString("users"), qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        } else {
            return p.getUsersTimeline(viewerId, qp.checkGetString("users"), qp.checkGetString("cols"), qp.getInt("start_time", 0), qp.getInt("max", DateUtils.nowMillis()), (int) qp.getInt("type", ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        }
    }

    @WebMethod("post/qiupuusertimeline")
    public RecordSet getUsersTimelineForQiuPu(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        long maxt = qp.getInt("end_time", 0) <= 0 ? DateUtils.nowMillis() : qp.getInt("end_time", 0);
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return p.getFullUsersTimelineForQiuPu(viewerId, qp.checkGetString("users"), qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        } else {
            return p.getUsersTimelineForQiuPu(viewerId, qp.checkGetString("users"), qp.checkGetString("cols"), qp.getInt("start_time", 0), maxt, (int) qp.getInt("type", ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        }

    }

    @WebMethod("post/myshare")
    public RecordSet getMyShare(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        long max = qp.getInt("end_time", 0) <= 0 ? DateUtils.nowMillis() : qp.getInt("end_time", 0);
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return p.getMyShareFullTimeline(viewerId, qp.checkGetString("users"), qp.getInt("start_time", 0), max, (int) qp.getInt("type", ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        } else {
            return p.getMyShareTimeline(viewerId, qp.checkGetString("users"), qp.checkGetString("cols"), qp.getInt("start_time", 0), max, (int) qp.getInt("type", ALL_POST), qp.checkGetString("appid"), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        }

    }

    @WebMethod("ignore/create")
    public boolean createIgnore(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.createIgnore(viewerId, qp.checkGetString("target_type"), qp.checkGetString("target_ids"));
    }

    @WebMethod("ignore/delete")
    public boolean deleteIgnore(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.deleteIgnore(viewerId, qp.checkGetString("target_type"), qp.checkGetString("target_ids"));
    }

    @WebMethod("ignore/get")
    public RecordSet getIgnores(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        return p.getIgnoreList(viewerId, qp.getString("target_type", ""), page, count);
    }

    @WebMethod("post/friendtimeline")
    public RecordSet getFriendsTimeline(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", ALL_POST);
        String appId = qp.checkGetString("appid");
        String viewerId = p.checkSignAndTicket(qp);
        if (qp.containsKey("cols")) {
            return p.getFriendsTimeline(viewerId, qp.getString("circleIds", ""), qp.checkGetString("cols"), since, max, type, appId, page, count);
        } else {
            return p.getFullFriendsTimeline(viewerId, qp.getString("circleIds", ""), since, max, type, appId, page, count);
        }
    }

    @WebMethod("post/qiupufriendtimeline")
    public RecordSet getFriendTimelineForQiuPu(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", ALL_POST);
        String appId = qp.checkGetString("appid");
        String viewerId = p.checkSignAndTicket(qp);
        if (qp.containsKey("cols")) {
            return p.getFriendsTimelineForQiuPu(viewerId, viewerId, qp.getString("circleIds", String.valueOf(Constants.FRIENDS_CIRCLE)), qp.checkGetString("cols"), since, max, type, appId, page, count);
        } else {
            return p.getFullFriendsTimelineForQiuPu(viewerId, viewerId, qp.getString("circleIds", String.valueOf(Constants.FRIENDS_CIRCLE)), since, max, type, appId, page, count);
        }
    }

    @WebMethod("post/nearby")
    public RecordSet getPostNearBy(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        long since = qp.getInt("start_time", 0);
        long max = qp.getInt("end_time", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100)
            count = 100;
        int type = (int) qp.getInt("type", ALL_POST);
        String appId = qp.checkGetString("appid");
        String viewerId = p.checkSignAndTicket(qp);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String longitude_me = Constants.parseLocation(loc, "longitude");
        String latitude_me = Constants.parseLocation(loc, "latitude");
        int distance = (int) qp.getInt("distance", 1000);

        if (longitude_me.equals("") || latitude_me.equals(""))
            throw new BaseException(ErrorCode.PARAM_ERROR, "want Correct location");
        return p.getNearByStream(viewerId, qp.getString("cols", ""), since, max, type, appId, page, count, loc, distance);
    }

    @WebMethod("sys/icon")
    public NoResponse getSysIcon(QueryParams qp, HttpServletResponse resp) {
        SFSUtils.writeResponse(resp, sysIconStorage, qp.checkGetString("file"));
        return NoResponse.get();
    }

    @WebMethod("post/canlike")
    public boolean postCanLike(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        p.checkSignAndTicket(qp);
        return p.postCanLike(qp.checkGetString("postId"));
    }

    @WebMethod("post/cancomment")
    public boolean postCanComment(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        p.checkSignAndTicket(qp);
        return p.postCanComment(qp.checkGetString("postId"));
    }

    @WebMethod("post/commented")
    public RecordSet getCommentedPosts(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getCommentedPosts(viewerId, qp.getString("cols", ""), (int) qp.getInt("objectType", 2), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
    }

    @WebMethod("post/liked")
    public RecordSet getLikedPosts(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getLikedPosts(viewerId, qp.getString("cols", ""), (int) qp.getInt("objectType", 2), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
    }

    @WebMethod("comment/create")
    public Record createComment(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        boolean can_like = qp.getString("can_like", "1").equals("1") ? true : false;
        String add_to = getAddToUserIds(qp.checkGetString("message"));
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));
        return p.createComment(viewerId, (int) qp.getInt("object", 2), qp.checkGetString("target"), qp.checkGetString("message"), ua, can_like, loc, add_to, appId);
    }

    @WebMethod("comment/destroy")
    public boolean destroyComments(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        RecordSet recs = p.destroyComments(viewerId, qp.checkGetString("comments"));
        for (Record rec : recs) {
            if (!rec.checkGetBoolean("result"))
                return false;
        }
        return true;
    }

    @WebMethod("comment/count")
    public int getCommentCount(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getCommentCount(viewerId, (int) qp.getInt("object", 2), qp.getString("target", ""));
    }

    @WebMethod("comment/for")
    public RecordSet getCommentsFor(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        if (qp.getString("comments", "").isEmpty() || qp.getString("comments", "").equals("")) {
            return p.getFullCommentsFor(viewerId, (int) qp.getInt("object", 2), qp.getString("target", ""), qp.getBoolean("asc", true), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        } else {
            return p.getCommentsFor(viewerId, (int) qp.getInt("object", 2), qp.getString("target", ""), qp.checkGetString("comments"), qp.getBoolean("asc", true), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
        }
    }

    @WebMethod("comment/get")
    public RecordSet getComments(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }

        if (qp.getString("columns", "").isEmpty() || qp.getString("columns", "").equals("")) {
            return p.getFullComments(viewerId, qp.checkGetString("comments"));
        } else {
            return p.getComments(viewerId, qp.checkGetString("comments"), qp.checkGetString("columns"));
        }
    }

    @WebMethod("comment/can_like")
    public boolean commentCanLike(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        return p.commentCanLike(viewerId, qp.checkGetString("comment"));
    }

    @WebMethod("comment/updateaction")
    public boolean commentUpdateCanLike(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.updateCommentCanLike(viewerId, qp.checkGetString("commentId"), qp.getBoolean("can_like", true));
    }

    @WebMethod("like/like")
    public boolean like(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));
        return p.like(viewerId, (int) qp.getInt("object", 2), qp.checkGetString("target"), ua, loc, appId);
    }

    @WebMethod("like/unlike")
    public boolean unlike(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.unlike(viewerId, (int) qp.getInt("object", 2), qp.checkGetString("target"));
    }

    @WebMethod("like/count")
    public int getLikeCount(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        p.checkSignAndTicket(qp);
        return p.getLikeCount((int) qp.getInt("object", 2), qp.checkGetString("target"));
    }

    @WebMethod("like/users")
    public RecordSet likedUsers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.likedUsers(viewerId, (int) qp.getInt("object", 2), qp.checkGetString("target"), qp.getString("columns", ""), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
    }

    @WebMethod("like/ifliked")
    public boolean likeIfliked(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.ifuserLiked(viewerId, qp.checkGetString("targetId"));
    }

    @WebMethod("suggest/refuse")
    public boolean suggestRefuse(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.refuseSuggestUser(viewerId, qp.checkGetString("suggested"));
    }

    @WebMethod("suggest/create")
    public boolean suggestCreate(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.createSuggestUser(qp.getString("toUser", viewerId), qp.checkGetString("suggestedusers"), (int) qp.getInt("type", 90), qp.getString("reason", ""));
    }

    @WebMethod("suggest/delete")
    public boolean suggestDeleteSuggestUser(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.deleteSuggestUser(viewerId, qp.checkGetString("suggesteduser"));
    }

    @WebMethod("suggest/get")
    public RecordSet suggestGet(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getSuggestUser(viewerId, (int) qp.getInt("count", 100), qp.getBoolean("getback", false));
    }

    @WebMethod("suggest/updatereason")
    public boolean suggestUpdateReason(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.updateSuggestUserReason();
    }

    @WebMethod("socialcontact/upload")
    public RecordSet socialContactUpload(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        try {
            String s = qp.getString("contactinfo", "");
            if (s.length() <= 0)
                return new RecordSet();
            String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
            String loc = getDecodeHeader(req, "location", "", viewerId);
            return p.createSocialContacts(viewerId, qp.checkGetString("contactinfo"), ua, loc);
        } catch (Exception e) {
            return new RecordSet();
        }
    }

    @WebMethod("suggest/recommend")
    public boolean suggestRecommend(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        try {
            return p.recommendUser(viewerId, qp.checkGetString("touser"), qp.checkGetString("suggestedusers"));
        } catch (Exception e) {
            return false;
        }
    }


    @WebMethod("account/movecircle")
    public String moveCircle(HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        RecordSet recs = p.findAllUserIds(true);
        String ua = getDecodeHeader(req, "User-Agent", "", "");
        String loc = getDecodeHeader(req, "location", "", "");
        for (Record r : recs) {
            RecordSet rs0 = p.getFriends0(r.getString("user_id"), String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), 0, 1000);
            for (Record r0 : rs0) {
                p.setFriends(r.getString("user_id"), r0.getString("friend"), String.valueOf(Constants.DEFAULT_CIRCLE), Constants.FRIEND_REASON_MANUALSELECT, true, ua, loc);
            }
        }
        return null;
    }

    @WebMethod("friend/updatecirclemembercount")
    public String friendUpdateCircleMemberCount(HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        RecordSet recs = p.findAllUserIds(false);
        for (Record r : recs) {
            //先找到我有多少个圈子
            String user_id = r.getString("user_id");
            RecordSet rsCircle = p.getCircles(user_id, "", false);
            for (Record rk : rsCircle) {
                RecordSet rs0 = p.getFriends0(user_id, rk.getString("circle_id"), 0, 1000);
                int myFriendCount = rs0.size();
                if (rk.getInt("member_count") != myFriendCount)
                    p.updateCircleMemberCount(user_id, rk.getString("circle_id"), myFriendCount);
            }
        }
        return null;
    }

    @WebMethod("request/count")
    public int getRequestCount(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getRequestCount(viewerId, qp.getString("app", ""), qp.getString("type", ""));
    }

    @WebMethod("request/attention")
    public boolean createRequestAttention(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.createRequestAttention(viewerId, qp.checkGetString("userId"));
    }

    @WebMethod("request/get")
    public RecordSet getRequest(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.getRequests(viewerId, qp.getString("app", ""), qp.getString("type", ""));
    }

    @WebMethod("request/done")
    public boolean doneRequest(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return p.doneRequests(viewerId, qp.checkGetString("requests"), qp.getString("type", ""), qp.getString("data", ""), qp.getBoolean("accept", false));
    }

    @WebMethod("request/profile_access_approve")
    public boolean sendProfileAccessApprove(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        p.createRequest(qp.checkGetString("to"), viewerId, "0", REQUEST_PROFILE_ACCESS, qp.getString("message", ""), "", true, ua, loc);
        return true;
    }

    protected String getDecodeHeader(HttpServletRequest req, String name, String def, String userId) throws UnsupportedEncodingException, AvroRemoteException {
        String v = req.getHeader(name);
        String getName = StringUtils.isNotEmpty(v) ? java.net.URLDecoder.decode(v, "UTF-8") : def;

        /*
        if (name.equals("location")) {
            if (StringUtils.isBlank(getName)) {
                Platform p = platform();
                if (!StringUtils.isBlank(userId)) {
                    Record r = p.getSignIn(userId, false, 0, 1).getFirstRecord();
                    String loc = "longitude=";
                    loc += r.getString("longitude") + ";";
                    loc += "latitude=";
                    loc += r.getString("latitude") + ";";
                    loc += "altitude=";
                    loc += r.getString("altitude") + ";";
                    loc += "speed=";
                    loc += r.getString("speed") + ";";
                    loc += "geo=";
                    loc += r.getString("geo");
                    getName = loc;
                }
            }
        }
        */
        return getName;
    }

    protected String getAddToUserIds(String message) throws UnsupportedEncodingException {
        String outUserId = "";
        Platform p = platform();
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


    @WebMethod("request/add_friend")
    public boolean sendAddFriend(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        p.createRequest(qp.checkGetString("to"), viewerId, "0", REQUEST_ADD_FRIEND, qp.getString("message", ""), "", true, ua, loc);
        return true;
    }

    @WebMethod("request/change_profile")
    public boolean sendChangeProfile(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        final HashMap<String, String> M = new HashMap<String, String>();
        M.put("mobile_telephone_number", REQUEST_CHANGE_MOBILE_TELEPHONE_NUMBER);
        M.put("mobile_2_telephone_number", REQUEST_CHANGE_MOBILE_2_TELEPHONE_NUMBER);
        M.put("mobile_3_telephone_number", REQUEST_CHANGE_MOBILE_3_TELEPHONE_NUMBER);
        M.put("email_address", REQUEST_CHANGE_EMAIL_ADDRESS);
        M.put("email_2_address", REQUEST_CHANGE_EMAIL_2_ADDRESS);
        M.put("email_3_address", REQUEST_CHANGE_EMAIL_3_ADDRESS);

        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        QueryParams.Value<String> v = qp.getSequentialString(M.keySet().toArray(new String[M.size()]));
        if (v == null || StringUtils.isBlank(v.value))
            throw Errors.createResponseError(com.borqs.server.platform.ErrorCode.PARAM_ERROR, "Can't find data");

        String type = M.get(v.key);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        p.createRequest(qp.checkGetString("to"), viewerId, "0", type, qp.getString("message", ""), v.value, true, ua, loc);
        return true;
    }

    @WebMethod("link/removecache")
    public boolean linkRemoveCache(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        return p.linkRemoveCache(qp.checkGetString("key"));
    }

    @WebMethod("account/api_list")
    public List<String> apiList() {
        Platform p = platform();
        return p.apiList();
    }

    @WebMethod("account/split_name")
    public boolean splitUserName(HttpServletRequest req) throws UnsupportedEncodingException, AvroRemoteException {
        Platform p = platform();
        String ua = getDecodeHeader(req, "User-Agent", "", "");
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        p.splitUserName(lang);
        return true;
    }

    @WebMethod("account/send_notification")
    public boolean accountSendNotification(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = "os=android-15-x86;client=B+ 203;lang=CN;model=unknown-generic;deviceid=564476eead582bd135690b08d942613";
        p.sendNotification(viewerId, ua, qp.getString("email", ""), qp.getString("phone", ""), "姜长胜", "m");
        return true;
    }


    //=======================================================

    @WebMethod("album/create")
    public boolean createAlbum(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String userId = p.checkTicket(qp);
        if (null == photo)
            throw new PhotoException("server error, can't save");
        String album_name = qp.checkGetString("title");
        int visible = (int) qp.getInt("privacy", 0);         //0 open 1 only me 2 friend open
        String description = qp.getString("summary", "");
        String album_id = Long.toString(RandomUtils.generateId());
        Record rc = new Record();

        rc.put("album_id", album_id);
        rc.put("album_type", photo.ALBUM_TYPE_OTHERS);
        rc.put("user_id", userId);
        rc.put("title", album_name);
        rc.put("created_time", DateUtils.nowMillis());
        rc.put("summary", description);
        rc.put("privacy", visible);
        photo.createAlbum(rc);
        return true;
    }


    @WebMethod("album/all")
    public RecordSet getAlbums(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        String ua = getDecodeHeader(req, "User-Agent", "", "");
        String userId = qp.getString("user_id", viewerId);
        if (null == photo)
            throw new PhotoException("server error, can't query album");

        RecordSet recs = photo.getUserAlbum(viewerId, userId);
        L.debug("album/all:recs=" + recs.toString());
        for (Record rec : recs) {
            rec = addAlbumLastedPhoto(viewerId, rec, rec.getString("album_id"));
            rec.put("title", formatAlbumName(ua, (int) rec.getInt("album_type"), rec.getString("title")));
        }
        L.debug("album/all:recs new=" + recs.toString());
        return recs;
    }

    public String formatAlbumName(String ua, int album_type, String album_name) {
        if (album_type == photo.ALBUM_TYPE_PROFILE)
            album_name = Constants.getBundleString(ua, "album.name.profile");
        if (album_type == photo.ALBUM_TYPE_SHARE_OUT)
            album_name = Constants.getBundleString(ua, "album.name.sharing");
        if (album_type == photo.ALBUM_TYPE_COVER)
            album_name = Constants.getBundleString(ua, "album.name.cover");
        if (album_type == photo.ALBUM_TYPE_RECEIVED)
            album_name = Constants.getBundleString(ua, "album.name.received");
        if (album_type == photo.ALBUM_TYPE_MY_SYNC)
            album_name = Constants.getBundleString(ua, "album.name.cloud");
        return album_name;
    }

    @WebMethod("album/get")
    public Record getAlbumById(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        String ua = getDecodeHeader(req, "User-Agent", "", "");
        String userId = qp.getString("user_id", viewerId);
        String album_id = qp.checkGetString("album_id");
        if (null == photo)
            throw new PhotoException("server error, can't query album");

        Record rec = photo.getAlbumById(viewerId, userId, album_id);
        rec = addAlbumLastedPhoto(viewerId, rec, album_id);
        rec.put("title", formatAlbumName(ua, (int) rec.getInt("album_type"), rec.getString("title")));
        return rec;
    }

    public Record addAlbumLastedPhoto(String viewerId, Record rec, String album_id) {
        Record lp = photo.getLatestPhoto(viewerId, album_id);
        Configuration conf = getConfiguration();
        if (!lp.isEmpty()) {
            rec.put("album_cover_photo_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), lp.getString("img_middle")));
            rec.put("album_cover_photo_original", String.format(conf.checkGetString("platform.photoUrlPattern"), lp.getString("img_middle")));
            rec.put("album_cover_photo_big", String.format(conf.checkGetString("platform.photoUrlPattern"), lp.getString("img_big")));
            rec.put("album_cover_photo_small", String.format(conf.checkGetString("platform.photoUrlPattern"), lp.getString("img_small")));
            rec.put("orientation", lp.getString("orientation"));
        } else {
            rec.put("album_cover_photo_middle", "");
            rec.put("album_cover_photo_original", "");
            rec.put("album_cover_photo_big", "");
            rec.put("album_cover_photo_small", "");
        }
        return rec;
    }

    @WebMethod("album/update")
    public boolean updateAlbum(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        if (null == photo)
            throw new PhotoException("server error, can't update album");

        String album_id = qp.checkGetString("album_id");
        Record r = photo.getAlbumOriginal(album_id);
        int album_type = (int) r.getInt("album_type");
        if (album_type != photo.ALBUM_TYPE_OTHERS)
            throw new PhotoException("only can update user album");
        if (!viewerId.equals(r.getString("user_id")))
            throw new PhotoException("can't update other album");
        String album_name = qp.getString("title", null);
        String description = qp.getString("summary", null);
        String visible = qp.getString("privacy", null);

        if (!StringUtils.isNotBlank(visible)) {
            if (!visible.equals("0") && !visible.equals("1") && !visible.equals("2"))
                throw new PhotoException("privacy error, privacy must be 0,1,2");
        }

        Record rc = new Record();
        if (StringUtils.isNotBlank(album_name)) {
            rc.put("title", album_name);
        }
        if (StringUtils.isNotBlank(description)) {
            rc.put("summary", description);
        }
        if (StringUtils.isNotBlank(visible)) {
            rc.put("privacy", Integer.valueOf(visible));
        }

        return photo.updateAlbum(album_id, rc);
    }

    @WebMethod("album/delete")
    public boolean deleteAlbum(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        if (null == photo)
            throw new PhotoException("server error, can't delete album");

        String album_id = qp.checkGetString("album_id");
        Record album = photo.getAlbumOriginal(album_id);
        if (album.getInt("album_type") != photo.ALBUM_TYPE_OTHERS)
            throw new PhotoException("can't delete this album");
        if (!viewerId.equals(album.getString("user_id")))
            throw new PhotoException("can't delete other album");
        return photo.deleteAlbumById(viewerId, album_id, p.bucketName_photo_key, photoStorage);
    }

    @WebMethod("photo/get")
    public RecordSet getPhotoByIds(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        RecordSet recs = photo.getPhotoByIds(qp.checkGetString("photo_ids"));
        Configuration conf = getConfiguration();
        for (Record rec : recs) {
            rec = formatPhotoUrlAndExtend(viewerId, rec, conf, p);
        }
        return recs;
    }

    @WebMethod("photo/album_get")
    public RecordSet getPhotoByAlbumIds(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        List<String> album_ids0 = StringUtils2.splitList(qp.checkGetString("album_ids"), ",", true);
        Record album = photo.getAlbumOriginal(album_ids0.get(0));
        String user_id = album.getString("user_id");
//        String cols = "photo_id,album_id,user_id,img_middle,img_original,img_big,img_small,caption,created_time,location,tag,tag_ids,from_user,original_pid,longitude,latitude,orientation,stream_id,privacy";
        RecordSet recs = photo.getAlbumPhotos(viewerId, album_ids0.get(0), page, count);
        Configuration conf = getConfiguration();
        for (Record rec : recs) {
            rec = formatPhotoUrlAndExtend(viewerId, rec, conf, p);
        }
        return recs;
    }

    @WebMethod("photo/update")
    public boolean updatePhoto(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        if (null == photo)
            throw new PhotoException("server error, can't update photo");

        String photo_id = qp.checkGetString("photo_id");
        String caption = qp.getString("caption", null);
        String location = qp.getString("location", null);
        Record rc = new Record();
        if (caption != null) {
            rc.put("caption", caption);
        }
        if (location != null) {
            rc.put("location", location);
        }
        return photo.updatePhoto(photo_id, rc);
    }

    @WebMethod("photo/delete")
    public boolean deletePhoto(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        if (null == photo)
            throw new PhotoException("server error, can't delete photo");
        String pIDs = qp.checkGetString("photo_ids");
        boolean delete_all = qp.getBoolean("delete_all", false);
        return photo.deletePhotoById(viewerId, pIDs, delete_all, p.bucketName_photo_key, photoStorage);
    }

    @WebMethod("photo/tag")
    public Record tagPhoto(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        String photo_id = qp.checkGetString("photo_id");
        String tagUserId = qp.checkGetString("user_id");
        String tagText = qp.checkGetString("tag_text");
        boolean addTag = qp.getBoolean("add_tag", true);
        int top = 0;
        int left = 0;
        int frame_width = 0;
        int frame_height = 0;
        if (addTag) {
            top = (int) qp.checkGetInt("top");
            left = (int) qp.checkGetInt("left");
            frame_width = (int) qp.checkGetInt("frame_width");
            frame_height = (int) qp.checkGetInt("frame_height");
        }
        Record record = photo.tagPhoto(photo_id, top, left, frame_width, frame_height, tagUserId, tagText, addTag);
        Configuration conf = getConfiguration();
        record = formatPhotoUrlAndExtend(viewerId, record, conf, p);
        return record;
    }

    @WebMethod("photo/update_stream_id")
    public boolean updatePhotoStreamId(QueryParams qp) throws AvroRemoteException {

        photo.updatePhotoStreamId((int) qp.getInt("album_type", 1), qp.getString("asc", "DESC"));

        return true;
    }

    @WebMethod("photo/include_me")
    public RecordSet photoContainsMe(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        String user_id = qp.getString("user_id", viewerId);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        RecordSet recs = photo.getPhotosIncludeMe(viewerId, user_id, page, count);
        Configuration conf = getConfiguration();
        for (Record rec : recs) {
            rec = formatPhotoUrlAndExtend(viewerId, rec, conf, p);
        }
        return recs;
    }

    public static boolean isNumeric(String str) {
        for (int i = str.length(); --i >= 0; ) {
            int chr = str.charAt(i);
            if (chr < 48 || chr > 57)
                return false;
        }
        return true;
    }

    public Record formatPhotoUrlAndExtend(String viewerId, Record rec, Configuration conf, Platform p) throws AvroRemoteException {
        if (!rec.isEmpty()) {
            rec.put("photo_url_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_middle")));
            rec.put("photo_url_original", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_original")));
            rec.put("photo_url_big", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_big")));
            rec.put("photo_url_small", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_small")));
            rec.put("photo_url_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), rec.getString("img_small").replace("S", "T")));
            //who shared to me

            String from_user = rec.getString("from_user");
            if (!from_user.equals("") && !from_user.equals("0")) {
                rec.put("from_user", p.getUser(viewerId, from_user, "user_id, display_name, image_url,perhaps_name"));
            } else {
                rec.put("from_user", new Record());
            }
            rec.remove("img_middle");
            rec.remove("img_big");
            rec.remove("img_small");
            rec.remove("img_original");

            //add comments and likes
            String photo_id = rec.getString("photo_id");

            Record Rec_photo_like = new Record();
            String objectPhotoId = String.valueOf(Constants.PHOTO_OBJECT) + ":" + String.valueOf(photo_id);
            int photo_like_count = p.likeGetCount(objectPhotoId);
            Rec_photo_like.put("count", photo_like_count);
            if (photo_like_count > 0) {
                RecordSet recs_liked_users = p.loadLikedUsers(objectPhotoId, 0, 5);
                List<Long> list_photo_liked_users = recs_liked_users.getIntColumnValues("liker");
                String likeuids = StringUtils.join(list_photo_liked_users, ",");
                RecordSet recs_user_liked = p.getUsers(rec.getString("source"), likeuids, p.USER_LIGHT_COLUMNS_LIGHT);
                Rec_photo_like.put("users", recs_user_liked);
            } else {
                Rec_photo_like.put("users", new Record());//3
            }

            Rec_photo_like.put("iliked", viewerId.equals("") ? false : p.ifuserLiked(viewerId, objectPhotoId));
            rec.put("likes", Rec_photo_like);

            Record Rec_comment = new Record();
            int comment_count = p.getCommentCount(viewerId, Constants.PHOTO_OBJECT, String.valueOf(photo_id));
            Rec_comment.put("count", comment_count);
            if (comment_count > 0) {
                RecordSet recs_com = p.getCommentsForContainsIgnore(viewerId, Constants.PHOTO_OBJECT, photo_id, p.FULL_COMMENT_COLUMNS, false, 0, 2);
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

    @WebMethod("photo/download_photo")
    public NoResponse downloadPhoto(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        String photo_id = qp.checkGetString("photo_id");
        String ft = qp.getString("filetype", PHOTO_TYPE_ORIGINAL);
        SFSUtils.writeResponse(resp, photoStorage, getFileName(photo_id, ft), "image/JPEG");
        return NoResponse.get();
    }

    @WebMethod("photo/format")
    public RecordSet formatPhotoThumbnail(QueryParams qp, HttpServletResponse resp) throws Exception {
        RecordSet recs = photo.getAllPhotos(qp.getString("user_id", "0"));
        Configuration conf = getConfiguration();
        RecordSet outList = new RecordSet();
        for (Record rec : recs) {
            Record tt = new Record();
            tt.put("photo_id", rec.getString("photo_id"));
            tt.put("img_small", rec.getString("img_small"));
            try {
                String new_file_name = makImage("http://storage.aliyun.com/wutong-data/media/photo/" + rec.getString("img_small"), rec.getString("img_small"));
                tt.put("img_t", String.format(conf.checkGetString("platform.photoUrlPattern"), new_file_name));
            } catch (Exception e) {
            }
            outList.add(tt);
        }
        return outList;
    }

    public static InputStream getUrlImage(String URLName) throws Exception {
        return getUrlImage(URLName, null);
    }

    public static InputStream getUrlImage(String URLName, StringBuilder retryUrl) throws Exception {
        int HttpResult = 0;
        try {
            URL url = new URL(URLName);
            URLConnection urlConn = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            HttpResult = httpConn.getResponseCode();
            if (HttpResult != HttpURLConnection.HTTP_OK) {
                return null;
            } else {
                if (retryUrl != null)
                    retryUrl.append(httpConn.getURL().toString());
                return new BufferedInputStream(urlConn.getInputStream());
            }
        } catch (Exception e) {
            L.error(e.toString());
            return null;
        }
    }

    public String makImage(String url, String file_name) throws Exception {
        InputStream input = getUrlImage(url);
        if (input != null) {
            byte[] imgBuf = IOUtils.toByteArray(input);
            BufferedImage image = null;
            image = ImageIO.read(new ByteArrayInputStream(imgBuf));

            long sw = 0;
            long sh = 0;
            if (image != null) {
                long bw = image.getWidth();
                long bh = image.getHeight();
                if (bw > bh) {
                    sh = 120;
                    sw = (bw * 120) / bh;
                } else {
                    sw = 120;
                    sh = (bh * 120) / bw;
                }
            }
            String img_name_thumbnail = file_name.replace("S", "T");
            img_name_thumbnail = "media/photo/" + img_name_thumbnail;

            SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), photoStorage, img_name_thumbnail, String.valueOf(sw), String.valueOf(sh), StringUtils.substringAfterLast(file_name, "."));
            return img_name_thumbnail;
        } else {
            return "";
        }
    }

    @WebMethod("phonebook/look_up")
    public RecordSet findBorqsId(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        Platform p = platform();
        String contact_info = qp.checkGetString("contact_info");
        RecordSet recs = RecordSet.fromJson(contact_info);
        if (recs.size() == 0)
            throw Errors.createResponseError(com.borqs.server.platform.ErrorCode.PARAM_ERROR, "Contact info error!");
        RecordSet outRecs = p.findBorqsIdFromContactInfo(recs);
        return outRecs;
    }

    @WebMethod("phonebook/all")
    public Record findMyAllPhoneBook(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        Platform p = platform();
        String contact_info = qp.getString("contact_info", "");
        String userId = p.checkTicket(qp);
        Record outRec = p.findMyAllPhoneBook(userId, contact_info);
        return outRec;
    }

    private String getFileName(String photo_id, String filetype) {
        if (null == photo)
            throw new PhotoException("server error, can't get");

        Record rc = photo.getPhotoByIds(photo_id).getFirstRecord();
        if (rc.isEmpty())
            throw new PhotoException("photo is not exist!!");
        if (PHOTO_TYPE_LARGE.equals(filetype)) {
            return rc.getString("img_big");
        } else if (PHOTO_TYPE_SMALL.equals(filetype)) {
            return rc.getString("img_small");
        } else
            return rc.getString("img_middle");
    }

    private void saveUploadPhoto(FileItem fileItem, String file, String path, Record record) {

        int width, height, sWidth = 0, sHeight = 0, mWidth = 0, mHeight = 0, tHeight = 0, tWidth = 0;
        try {
            long n = fileItem.getSize();
            String fileName = fileItem.getName().substring(fileItem.getName().lastIndexOf("\\") + 1, fileItem.getName().length());
            String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
            String rotateFile = SFSUtils.revertPhoto(fileItem, expName, record);
            File fileTmp = new File(rotateFile);


            BufferedImage image = ImageIO.read(fileTmp);
            width = image.getWidth();
            height = image.getHeight();

            if (width == height) {
                sHeight = sWidth = 360;
                mHeight = mWidth = 640;
                tHeight = tWidth = 120;
            }
            if (width > height) {
                sHeight = 360;
                sWidth = (int) 360 * width / height;
            }
            if (height > width) {
                sHeight = (int) 360 * height / width;
                sWidth = 360;
            }

            tHeight = height;
            tWidth = width;
            if (width > height) {
                tHeight = 120;
                tWidth = (int) 120 * width / height;
            }
            if (height > width) {
                tHeight = (int) 120 * height / width;
                tWidth = 120;
            }

            mHeight = height;
            mWidth = width;
            if (width > 640 || height > 640) {
                if (width > height) {
                    if (width > 640) {
                        mWidth = (int) 640 * width / height;
                        mHeight = 640;
                    }
                }
                if (height > width) {
                    if (height > 640) {
                        mHeight = (int) 640 * height / width;
                        mWidth = 640;
                    }
                }
            }

            Configuration conf = getConfiguration();
            if (!(photoStorage instanceof OssSFS)) {
                Record photoStoragePath = Record.fromJson(conf.getString("platform.servlet.photoStorage", ""));
                Record r1 = new Record();
                r1.put("dir", path);
                Record r2 = new Record();
                r2.put("class", photoStoragePath.getString("class"));
                r2.put("args", r1);
                photoStorage = (StaticFileStorage) ClassUtils2.newInstance(r2.toString(false, false));
            }


            String lfn = file + "_L." + expName;
            String ofn = file + "_O." + expName;
            String sfn = file + "_S." + expName;
            String tfn = file + "_T." + expName;
            if (photoStorage instanceof OssSFS) {
                lfn = "media/photo/" + lfn;
                ofn = "media/photo/" + ofn;
                sfn = "media/photo/" + sfn;
                tfn = "media/photo/" + tfn;
            }

            SFSUtils.saveScaledUploadImage(fileTmp, photoStorage, ofn, null, null, expName);
            SFSUtils.saveScaledUploadImage(fileTmp, photoStorage, lfn, Integer.toString(mWidth), Integer.toString(mHeight), expName);
            SFSUtils.saveScaledUploadImage(fileTmp, photoStorage, sfn, Integer.toString(sWidth), Integer.toString(sHeight), expName);
            SFSUtils.saveScaledUploadImage(fileTmp, photoStorage, tfn, Integer.toString(tWidth), Integer.toString(tHeight), expName);
            fileTmp.delete();

        } catch (IOException e) {
            throw new ImageException("can not read this file,not image");
        }
    }

    @WebMethod("photo/share")
    public Record sharePhoto(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String userId = p.checkTicket(qp);
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);
        List<String> groupIds = new ArrayList<String>();
        List<String> pids = new ArrayList<String>();
        String msg = qp.getString("msg", "share photo");
        String add_to = getAddToUserIds(msg);
        String tmp_ids = "";
        if (mentions.length() > 0) {
            List<String> l0 = StringUtils2.splitList(mentions, ",", true);
            if (l0.contains("#-2")) {
                l0.remove("#-2");
                mentions = StringUtils.join(l0, ",");
            } else {
//                privacy = true;
            }

            //group
            groupIds = p.getGroupIdsFromMentions(l0);
            for (String groupId : groupIds) {
                l0.remove("#" + groupId);
                l0.remove(groupId);
                Record groupRec = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.GROUP_ID_END,
                        groupId, COL_CAN_MEMBER_POST).getFirstRecord();
                long canMemberPost = groupRec.getInt(COL_CAN_MEMBER_POST, 1);
                if ((canMemberPost == 1 && p.hasGroupRight(Long.parseLong(groupId), userId, ROLE_MEMBER))
                        || (canMemberPost == 0 && p.hasGroupRight(Long.parseLong(groupId), userId, ROLE_ADMIN))
                        || canMemberPost == 2) {
                    l0.add(groupId);
                }
            }
            mentions = StringUtils.join(l0, ",");
            tmp_ids = p.parseUserIds(userId, mentions);
            List<String> l = StringUtils2.splitList(tmp_ids, ",", true);
            if (l.size() > MAX_GUSY_SHARE_TO)
                throw new BaseException(ErrorCode.PARAM_ERROR, "Only can share to less than 400 guys!");
        }
        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new BaseException(ErrorCode.PARAM_ERROR, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new BaseException(ErrorCode.GROUP_ERROR, "You don't have right to post!");

        String ua = getDecodeHeader(req, "User-Agent", "", userId);
        String loc = getDecodeHeader(req, "location", "", userId);
        if (!StringUtils.isBlank(loc)) {
            String longitude = Constants.parseLocation(loc, "longitude");
            String latitude = Constants.parseLocation(loc, "latitude");
            String altitude = Constants.parseLocation(loc, "altitude");
            String speed = Constants.parseLocation(loc, "speed");
            String geo = Constants.parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                p.signIn(userId, longitude, latitude, altitude, speed, geo, 1);
        }

        FileItem fi = qp.checkGetFile("photo_image");
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            if (!fi.getContentType().contains("image/"))
                throw new PhotoException("file type error,not image");
            String fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
            String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());

            String album_id = qp.getString("album_id", "");
            if (StringUtils.isEmpty(album_id))
                album_id = photo.getAlbum(userId, photo.ALBUM_TYPE_SHARE_OUT, "Sharing Pictures");

            String path = photo.getPhotoPath(userId, album_id);
            if (!(photoStorage instanceof OssSFS)) {
                File file = new File(path);
                if (!file.exists()) {
                    file.mkdir();
                }
            }
            if (!photo.isAlbumExist(album_id)) {
                throw new PhotoException("album not exist, please create album first");
            }

            String photoID = photo.genPhotoId(userId);

            String caption = qp.getString("caption", "");
            String imageName = userId + "_" + album_id + "_" + photoID;

            String longitude = "";
            String latitude = "";
            String orientation = "";
            Record extendExif = new Record();
            if (expName.equalsIgnoreCase("jpg") || expName.equalsIgnoreCase("jpeg")) {
                try {
                    extendExif = getExifGpsFromJpeg(fi);
                } catch (JpegProcessingException e) {
                } catch (MetadataException e) {
                }
                if (!extendExif.isEmpty()) {
                    if (extendExif.has("longitude"))
                        longitude = String.valueOf(formatJWD(extendExif.getString("longitude")));
                    if (extendExif.has("latitude"))
                        latitude = String.valueOf(formatJWD(extendExif.getString("latitude")));
                    if (extendExif.has("orientation"))
                        orientation = extendExif.getString("orientation");
                }
            }


            Record rc = new Record();
            rc.put("photo_id", photoID);
            rc.put("album_id", album_id);
            rc.put("user_id", userId);
            rc.put("img_middle", imageName + "_O." + expName);
            rc.put("img_original", imageName + "_O." + expName);
            rc.put("img_big", imageName + "_L." + expName);
            rc.put("img_small", imageName + "_S." + expName);
            rc.put("caption", caption);
            rc.put("created_time", DateUtils.nowMillis());
            rc.put("location", loc);
            rc.put("tag", "");
            rc.put("original_pid", photoID);
            rc.put("longitude", longitude);
            rc.put("longitude", longitude);
            rc.put("latitude", latitude);
            rc.put("orientation", orientation);

            saveUploadPhoto(fi, imageName, path, rc);
            RecordSet group_recs = new RecordSet();
            if (!groupIds.isEmpty()) {
                group_recs = dealWithGroupPhoto(p, rc, groupIds);
                pids.add(photoID);
            }
            boolean result = groupIds.isEmpty() ? photo.saveUploadPhoto(rc) : photo.saveUploadPhotos(group_recs);
            L.trace("save upload photo to db success");
            pids.add(photoID);
            List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < 10)
                        l00.add(l011);
                }
            }
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= 10) {
                        try {
                            String other_album_id = photo.getAlbum(uid, photo.ALBUM_TYPE_RECEIVED, "Received Pictures");
                            String path00 = photo.getPhotoPath(uid, other_album_id);
                            if (!(photoStorage instanceof OssSFS)) {
                                File file0 = new File(path00);
                                if (!file0.exists()) {
                                    file0.mkdir();
                                }
                            }

                            Record rc00 = new Record();
                            rc00.put("photo_id", photoID);
                            rc00.put("album_id", other_album_id);
                            rc00.put("user_id", uid);
                            rc00.put("img_middle", imageName + "_O." + expName);
                            rc00.put("img_original", imageName + "_O." + expName);
                            rc00.put("img_big", imageName + "_L." + expName);
                            rc00.put("img_small", imageName + "_S." + expName);
                            rc00.put("caption", caption);
                            rc00.put("created_time", DateUtils.nowMillis());
                            rc00.put("location", loc);
                            rc00.put("tag", "");
                            rc00.put("from_user", userId);
                            rc00.put("original_pid", photoID);
                            rc00.put("longitude", longitude);
                            rc00.put("latitude", latitude);
                            rc00.put("orientation", orientation);
                            photo.saveUploadPhoto(rc00);
//                        pids.add(photoID00);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            String post_id = "";
            Record album = photo.getAlbumOriginal(album_id);
            if (result) {            // && !album.getBoolean("privacy",false)
                Record sRecord = new Record();
                Configuration conf = getConfiguration();

                sRecord.put("album_id", album.getString("album_id"));
                sRecord.put("album_name", album.getString("title"));
                sRecord.put("photo_id", photoID);
                sRecord.put("album_photo_count", 0);
                sRecord.put("album_cover_photo_id", 0);
                sRecord.put("album_description", "");
                sRecord.put("album_visible", false);


                if (photoStorage instanceof OssSFS) {
                    sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_L." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                    sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_S." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                    sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), imageName + "_T." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                } else {
                    sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_O." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                    sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_L." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                    sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_S." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                    sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + imageName + "_T." + expName));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                }
                sRecord.put("photo_caption", rc.getString("caption"));
                sRecord.put("photo_location", rc.getString("location"));
                sRecord.put("photo_tag", rc.getString("tag"));
                sRecord.put("photo_created_time", rc.getString("created_time"));
                sRecord.put("longitude", rc.getString("longitude"));
                sRecord.put("latitude", rc.getString("latitude"));
                sRecord.put("orientation", rc.getString("orientation"));

                String app_data = qp.getString("app_data", "");
                if (qp.getBoolean("secretly", false) == true) {
                    qp.checkGetString("mentions");
                }

                boolean can_comment = qp.getBoolean("can_comment", true);
                boolean can_like = qp.getBoolean("can_like", true);
                boolean can_reshare = qp.getBoolean("can_reshare", true);

                post_id = p.post(userId, Constants.PHOTO_POST, msg, sRecord.toString(false, false), qp.getString("appid", "1"),
                        "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to);
                if (pids.size() > 0 && !post_id.equals(""))
                    photo.updatePhotoStreamId(post_id, pids);
            }
            return p.getFullPostsForQiuPu(userId, post_id, true).getFirstRecord();
        } else {
            String photoID = qp.checkGetString("photo_id");
            Record old_photo = photo.getPhotoByIds(photoID).getFirstRecord();
            if (old_photo.isEmpty())
                throw new PhotoException("this photo is not exist, author has deleted");

            String album_id = photo.getAlbum(userId, photo.ALBUM_TYPE_SHARE_OUT, "Sharing Pictures");

            Record rc = new Record();
            rc.put("photo_id", photoID);
            rc.put("album_id", album_id);
            rc.put("user_id", userId);
            rc.put("img_middle", old_photo.getString("img_middle"));
            rc.put("img_original", old_photo.getString("img_original"));
            rc.put("img_big", old_photo.getString("img_big"));
            rc.put("img_small", old_photo.getString("img_small"));
            rc.put("caption", old_photo.getString("caption"));
            rc.put("created_time", DateUtils.nowMillis());
            rc.put("location", old_photo.getString("location"));
            rc.put("tag", old_photo.getString("tag"));
            rc.put("longitude", old_photo.getString("longitude"));
            rc.put("latitude", old_photo.getString("latitude"));
            rc.put("orientation", old_photo.getString("orientation"));

            boolean result = groupIds.isEmpty() ? photo.saveUploadPhoto(rc) : photo.saveUploadPhotos(dealWithGroupPhoto(p, rc, groupIds));
            L.trace("save upload photo to db success");
            pids.add(photoID);
            List<String> l00 = StringUtils2.splitList(mentions, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < 10)
                        l00.add(l011);
                }
            }
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= 10) {
                        String other_album_id = photo.getAlbum(uid, photo.ALBUM_TYPE_RECEIVED, "Received Pictures");
                        String path00 = photo.getPhotoPath(uid, other_album_id);
                        if (!(photoStorage instanceof OssSFS)) {
                            File file0 = new File(path00);
                            if (!file0.exists()) {
                                file0.mkdir();
                            }
                        }

                        Record rc00 = new Record();
                        rc00.put("photo_id", photoID);
                        rc00.put("album_id", other_album_id);
                        rc00.put("user_id", uid);
                        rc00.put("img_middle", old_photo.getString("img_middle"));
                        rc00.put("img_original", old_photo.getString("img_original"));
                        rc00.put("img_big", old_photo.getString("img_big"));
                        rc00.put("img_small", old_photo.getString("img_small"));
                        rc00.put("caption", old_photo.getString("caption"));
                        rc00.put("created_time", DateUtils.nowMillis());
                        rc00.put("location", old_photo.getString("location"));
                        rc00.put("tag", old_photo.getString("tag"));
                        rc00.put("longitude", old_photo.getString("longitude"));
                        rc00.put("latitude", old_photo.getString("latitude"));
                        rc00.put("orientation", old_photo.getString("orientation"));
                        photo.saveUploadPhoto(rc00);
                    }
                }
            }

            String post_id = "";
            Record album = photo.getAlbumOriginal(album_id);
            Record sRecord = new Record();
            Configuration conf = getConfiguration();

            sRecord.put("album_id", album.getString("album_id"));
            sRecord.put("album_name", album.getString("title"));
            sRecord.put("photo_id", photoID);
            sRecord.put("album_photo_count", "");
            sRecord.put("album_cover_photo_id", "");
            sRecord.put("album_description", "");
            sRecord.put("album_visible", false);

            if (photoStorage instanceof OssSFS) {
                sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_big")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_small")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), old_photo.getString("img_original").replace("O", "T")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
            } else {
                sRecord.put("photo_img_middle", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_original", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_ORIGINAL));
                sRecord.put("photo_img_big", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_big")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_LARGE));
                sRecord.put("photo_img_small", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
                sRecord.put("photo_img_thumbnail", String.format(conf.checkGetString("platform.photoUrlPattern"), userId + "/" + album.getString("album_id") + "/" + old_photo.getString("img_original").replace("O", "T")));//genDownloadURL(photo.getLatestPhotoId(rc.getString("album_id")), PHOTO_TYPE_SMALL));
            }
            sRecord.put("photo_caption", rc.getString("caption"));
            sRecord.put("photo_location", rc.getString("location"));
            sRecord.put("photo_tag", rc.getString("tag"));
            sRecord.put("photo_created_time", rc.getString("created_time"));
            sRecord.put("longitude", rc.getString("longitude"));
            sRecord.put("latitude", rc.getString("latitude"));
            sRecord.put("orientation", rc.getString("orientation"));

            String app_data = qp.getString("app_data", "");
            if (qp.getBoolean("secretly", false) == true) {
                qp.checkGetString("mentions");
            }

            boolean can_comment = qp.getBoolean("can_comment", true);
            boolean can_like = qp.getBoolean("can_like", true);
            boolean can_reshare = qp.getBoolean("can_reshare", true);

            post_id = p.post(userId, Constants.PHOTO_POST, msg, sRecord.toString(false, false), qp.getString("appid", "1"),
                    "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to);
            if (pids.size() > 0 && !post_id.equals(""))
                photo.updatePhotoStreamId(post_id, pids);
            return p.getFullPostsForQiuPu(userId, post_id, true).getFirstRecord();
        }
    }

    @WebMethod("photo/myt")
    public boolean Photo(QueryParams qp, HttpServletRequest req) throws AvroRemoteException {
        L.debug("____________________testP___________start____________");
        FileItem fi = qp.checkGetFile("file");
        Record extendExif = new Record();
        String orientation = "";
        try {
            L.debug("____________________getExifGpsFromJpeg___________start____________");
            extendExif = getExifGpsFromJpeg(fi);
            L.debug("____________________getExifGpsFromJpeg_____________end__________");
        } catch (JpegProcessingException e) {
            e.printStackTrace();
        } catch (MetadataException e) {
            e.printStackTrace();
        }
        if (!extendExif.isEmpty()) {
            if (extendExif.has("orientation"))
                orientation = extendExif.getString("orientation");
        }
        Record record = new Record();
        record.put("orientation", orientation);
        //saveUploadPhoto(fi, "test.jpg", "D:/test/"+DateUtils.nowNano()+".jpg", record);
        saveUploadPhoto(fi, "test.jpg", "/home/zhengwei/data/photo/" + DateUtils.nowNano() + ".jpg", record);
        return true;
    }

    @WebMethod("photo/sync")
    public boolean syncPhoto(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException, JpegProcessingException, MetadataException {
        Platform p = platform();
        String userId = p.checkTicket(qp);
        FileItem fi = qp.checkGetFile("photo_image");
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            String fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
            String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());

            String album_id = qp.getString("album_id", "");
            if (StringUtils.isEmpty(album_id))
                album_id = photo.getAlbum(userId, photo.ALBUM_TYPE_MY_SYNC, "Cloud Album");

            String path = photo.getPhotoPath(userId, album_id);
            if (!(photoStorage instanceof OssSFS)) {
                File file = new File(path);
                if (!file.exists()) {
                    file.mkdir();
                }
            }

            String photoID = photo.genPhotoId(userId);
            String loc = getDecodeHeader(req, "location", "", userId);
            String imageName = userId + "_" + album_id + "_" + photoID;


            String longitude = "";
            String latitude = "";
            String orientation = "";
            Record extendExif = new Record();
            if (expName.equalsIgnoreCase("jpg") || expName.equalsIgnoreCase("jpeg")) {
                try {
                    extendExif = getExifGpsFromJpeg(fi);
                } catch (JpegProcessingException e) {
                } catch (MetadataException e) {
                }
                if (!extendExif.isEmpty()) {
                    if (extendExif.has("longitude"))
                        longitude = String.valueOf(formatJWD(extendExif.getString("longitude")));
                    if (extendExif.has("latitude"))
                        latitude = String.valueOf(formatJWD(extendExif.getString("latitude")));
                    if (extendExif.has("orientation"))
                        orientation = extendExif.getString("orientation");
                }
            }

            Record rc = new Record();
            rc.put("photo_id", photoID);
            rc.put("album_id", album_id);
            rc.put("user_id", userId);
            rc.put("img_middle", imageName + "_O." + expName);
            rc.put("img_original", imageName + "_O." + expName);
            rc.put("img_big", imageName + "_L." + expName);
            rc.put("img_small", imageName + "_S." + expName);
            rc.put("caption", fileName);
            rc.put("created_time", DateUtils.nowMillis());
            rc.put("location", loc);
            rc.put("tag", qp.getString("tag", ""));
            rc.put("longitude", longitude);
            rc.put("latitude", latitude);
            rc.put("orientation", orientation);
            rc.put("privacy", true);

            saveUploadPhoto(fi, imageName, path, rc);

            boolean result = photo.saveUploadPhoto(rc);
            L.trace("sync photo success");

            return result;
        } else {
            L.debug("upload file error");
            return false;
        }
    }

    public double formatJWD(String in_jwd) {
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

    public Record getExifGpsFromJpeg(FileItem fileItem) throws JpegProcessingException, MetadataException {
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


    @WebMethod("post/formatdata")
    public String postFormatdata(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String userId = p.checkTicket(qp);
        return p.formatOldDataToConversation(userId);
    }

    @WebMethod("post/test")
    public boolean postTest(QueryParams qp) throws IOException, SQLException {
        Platform p = platform();
//        String userId = p.checkTicket(qp);
//        String key = qp.checkGetString("key") ;
//        String short_url = p.generalShortUrl(key);
//        String  long_url = p.getLongUrl(short_url) ;
//        return "key="+ key + "\n" + "short_url="+short_url + "\n" + "long_url="+long_url;
//        FileItem fi = qp.checkGetFile("file1");
//        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
//           String fileName = fi.getName().substring(fi.getName().lastIndexOf("\\") + 1, fi.getName().length());
//            String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
//            Date date = new Date();
//            DateFormat f_year = new SimpleDateFormat("yyyyMMddhhmmss");
//            String c_name = f_year.format(date).toString();
//            SFSUtils.saveUpload(fi, photoStorage, c_name+"."+expName);
////        }
//        String fUids = "10365,10380,10417,10492,10519,10599,10615,10776,10778,10794,10801,10164,10611,10602,10792,10621,10525,10350,10727,10115,10205,10319,10208,10167,10261,10706,10786,10791,10281,10681,10642,10693,10522,10437,10688,10306,10278,10799,10800,10236,10607,10279,10162,10442,10680,10604,10273,10148,10598,10618,10129,10640,10673,10636,10685,10695,10347,10635,10813,10716,10808,10721,10311,10651,10789,10648,10393,10334,10779,10251,10788,10174,10715,10564,10314,10339,10249,10812,10372,10440,10444,10389,10797,10795,10269,10780,10216,10811,10274,10614,10248,10267,10683,10218,10103,10769,10303,10305,10772,10603,10600,10206,10343,10289,10312,10286,10605,10622,10169,10637,10243,10690,10712,10592,10172,10773,10242,10231,10814,10161,10658,10725,10108,10626,10689,10641,10726,10809,10230,10263,10160,10352,10655,10299,10198,10452,10652,10341,10647,10617,10597,10548,10606,10702,10610,10149,10446,10300,10699,10790,10684,10708,10711,10802,10692,10159,10609,10594,10643,10199,10210,10247,10151,10321,10649,10701,10280,10275,10192,10254,10138,10815,10330,10217,10722,10436,10595,10445,10787,10608,10596,10810,10441,10807,10679,10182,10301,10612,10650,10099,10793,10660,10694";
//        p.setFriendsTemp("10000", fUids, "6", 8, true, "", "");

//        Record user = p.getUser("42", "42", "user_id,login_email1,login_email2,login_email3,login_phone1,login_phone2,login_phone3", false);
//        String finallyString = "";
//        int flag = 0;
//        if (!user.getString("login_phone1").equals("")) {
//            finallyString = user.getString("login_phone1");
//        } else {
//            if (!user.getString("login_phone2").equals("")) {
//                finallyString = user.getString("login_phone2");
//            } else {
//                if (!user.getString("login_phone3").equals("")) {
//                    finallyString = user.getString("login_phone3");
//                }
//            }
//        }
//        if (!finallyString.equals(""))
//            flag = 1;
//
//        if (flag == 0) {
//            if (!user.getString("login_email1").equals("")) {
//                finallyString = user.getString("login_email1");
//            } else {
//                if (!user.getString("login_email2").equals("")) {
//                    finallyString = user.getString("login_email2");
//                } else {
//                    if (!user.getString("login_email3").equals("")) {
//                        finallyString = user.getString("login_email3");
//                    }
//                }
//            }
//            if (!finallyString.equals(""))
//                flag = 2;
//        }
//
//        if (flag == 0) {
//            finallyString = "42";
//            flag = 3;
//        }
//
//
//        String url_header = "http://api.borqs.com/dm/contacts/namecount/";
//        String url_middle = "";
//        if (flag == 1)
//            url_middle = "bymobile/" + finallyString;
//        if (flag == 2)
//            url_middle = "byemail/" + finallyString;
//        if (flag == 3)
//            url_middle = "byborqsid/" + finallyString;
//        String url_footer = ".json?limit=2";
//        String a = url_header + url_middle + url_footer;
        p.formatStreamLocation();
        return true;
    }

    @WebMethod("oss/upload")
    public NoResponse uploadFileToOSS(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        FileItem fi = qp.checkGetFile("file");
        String fileName = qp.checkGetString("file_name");
        String bucketName = qp.checkGetString("bucket_name");
        String callBack = qp.getString("callback", "");
        OssSFS ossStorage = new OssSFS(bucketName);
        SFSUtils.saveUpload(fi, ossStorage, fileName);

        if (StringUtils.isNotBlank(callBack))
            resp.sendRedirect(callBack);

        return NoResponse.get();
    }

    public String formatBucketKey(String content_type, Platform p) {
        String key = "";
        if (content_type.contains("video/")) {
            key = p.bucketName_video_key;
        }
        if (content_type.contains("audio/")) {
            key = p.bucketName_audio_key;
        }
        if (content_type.contains("text/") || content_type.contains("application/") || content_type.contains("image/")) {
            key = p.bucketName_static_file_key;
        }
        return key;
    }

    public Record uploadFile(Platform p, String viewerId, String file_id, long folder_id, FileItem fi, String summary, String description, String content_type, FileItem screen_shot, String file_name) throws AvroRemoteException, UnsupportedEncodingException {
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

            L.debug("===========content_type n=" + content_type);
            long file_size = fi.getSize();
//            if (file_size > 50 * 1024 * 1024L)
//                throw new BaseException(ErrorCode.DATA_ERROR, "file size is too large");
            String key = formatBucketKey(content_type, p);
            OssSFS ossStorage = new OssSFS(p.bucketName);

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

            b = folder.saveStaticFile(file0);
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
                folder.saveVideo(video0);
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

                folder.saveAudio(audio0);
            }
            if (b && !return_record.isEmpty()) {
                SFSUtils.saveUpload(fi, ossStorage, key + viewerId + "/" + newFileName);
                if (screen_shot != null && StringUtils.isNotEmpty(screen_shot.getName())) {
                    SFSUtils.saveUpload(screen_shot, ossStorage, key + viewerId + "/" + new_screen_shot_fileName);
                }
            }
            L.debug("===========upload return return_record=" + return_record);
            return return_record;
        } else {
            L.debug("upload file error");
            return new Record();
        }
    }

    @WebMethod("v2/file/upload")
    public Record fileUpload(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        FileItem fi = qp.checkGetFile("file");
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            String file_id = Long.toString(RandomUtils.generateId());
            FileItem screen_shot = qp.getFile("screen_shot");
            String summary = qp.getString("summary", "");
            String description = qp.getString("description", "");
            String content_type = qp.getString("content_type", "");
            String file_name = qp.getString("file_name", "");
            long folder_id = Long.parseLong(folder.getFolder(viewerId, folder.FOLDER_TYPE_MY_SYNC, "Sync Files"));
            Record rec = formatFileBucketUrl(viewerId, uploadFile(p, viewerId, file_id, folder_id, fi, summary, description, content_type, screen_shot, file_name));
            return rec;
        } else {
            L.debug("upload file error");
            return new Record();
        }
    }

    @WebMethod("v2/file/share")
    public Record fileShare(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        FileItem fi = qp.checkGetFile("file");
        String app_data = qp.getString("app_data", "");

        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String post_id = "";
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);

        String add_to = getAddToUserIds(qp.checkGetString("msg"));
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);
        List<String> groupIds = new ArrayList<String>();
        List<String> fids = new ArrayList<String>();
        String tmp_ids = "";

        if (mentions.length() > 0) {
            List<String> l0 = StringUtils2.splitList(mentions, ",", true);
            if (l0.contains("#-2")) {
                l0.remove("#-2");
                mentions = StringUtils.join(l0, ",");
            } else {
//                privacy = true;
            }

            //group
            groupIds = p.getGroupIdsFromMentions(l0);
            for (String groupId : groupIds) {
                l0.remove("#" + groupId);
                l0.remove(groupId);
                Record groupRec = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.GROUP_ID_END,
                        groupId, COL_CAN_MEMBER_POST).getFirstRecord();
                long canMemberPost = groupRec.getInt(COL_CAN_MEMBER_POST, 1);
                if ((canMemberPost == 1 && p.hasGroupRight(Long.parseLong(groupId), viewerId, ROLE_MEMBER))
                        || (canMemberPost == 0 && p.hasGroupRight(Long.parseLong(groupId), viewerId, ROLE_ADMIN))
                        || canMemberPost == 2) {
                    l0.add(groupId);
                }
            }
            mentions = StringUtils.join(l0, ",");
            tmp_ids = p.parseUserIds(viewerId, mentions);
            List<String> l = StringUtils2.splitList(tmp_ids, ",", true);
            if (l.size() > MAX_GUSY_SHARE_TO)
                throw new BaseException(ErrorCode.PARAM_ERROR, "Only can share to less than 400 guys!");
        }


//        if(getUserAndGroup(changeMentions, p, mentions, groupIds, viewerId))
//        {
//            mentions = changeMentions.toString();
//            String ids = p.parseUserIds(viewerId, mentions);
//            tmp_ids = ids;
//            List<String> l = StringUtils2.splitList(ids, ",", true);
//            if (l.size() > MAX_GUSY_SHARE_TO)
//                throw new BaseException(ErrorCode.PARAM_ERROR, "Only can share to less than 400 guys!");
//        }
        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new BaseException(ErrorCode.PARAM_ERROR, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new BaseException(ErrorCode.GROUP_ERROR, "You don't have right to post!");

        String share_file_id = qp.getString("file_id", "");

        if (fi != null && StringUtils.isNotEmpty(fi.getName()) && share_file_id.equals("")) {
            FileItem screen_shot = qp.getFile("screen_shot");
            String summary = qp.getString("summary", "");
            String description = qp.getString("description", "");
            String content_type = qp.getString("content_type", "");
            String file_name = qp.getString("file_name", "");

            String folder_id = folder.getFolder(viewerId, folder.FOLDER_TYPE_SHARE_OUT, "Sharing Files");
            if (!folder.isFolderExist(folder_id)) {
                throw new PhotoException("folder not exist, please create folder first");
            }

            String file_id = Long.toString(RandomUtils.generateId());
            L.debug("===============OSS INFO check=========viewerId=" +viewerId);
            Record static_file = uploadFile(p, viewerId, file_id, Long.parseLong(folder_id), fi, summary, description, content_type, screen_shot, file_name);
            L.debug("===============OSS INFO check=========static_file=" +static_file);
            fids.add(file_id);

            if (!groupIds.isEmpty()) {
                RecordSet group_recs = dealWithGroupFile(p, static_file, groupIds);
                fids.add(file_id);
                folder.saveStaticFiles(group_recs);
            }

            Record rec = formatFileBucketUrl(viewerId, static_file);
            L.debug("===============OSS INFO check=========Record rec=" +rec);
            String msg = qp.getString("msg", "");
            int type = Constants.FILE_POST;
//            if (rec.getString("content_type").contains("image/"))
//                type = Constants.PHOTO_POST;
            if (rec.getString("content_type").contains("video/")) {
                type = Constants.VIDEO_POST;
            } else if (rec.getString("content_type").contains("audio/")) {
                type = Constants.AUDIO_POST;
            }


            List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < 10)
                        l00.add(l011);
                }
            }
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= 10) {
                        try {
                            String other_folder_id = folder.getFolder(uid, folder.FOLDER_TYPE_RECEIVED, "Received Files");
                            if (static_file.has("file_url"))
                                static_file.removeColumns("file_url");
                            if (static_file.has("thumbnail_url"))
                                static_file.removeColumns("thumbnail_url");
                            if (static_file.has("likes"))
                                static_file.removeColumns("likes");
                            if (static_file.has("comments"))
                                static_file.removeColumns("comments");

                            static_file.put("folder_id", other_folder_id);
                            static_file.put("user_id", uid);
                            folder.saveStaticFile(static_file);
                        } catch (Exception e) {
                        }
                    }
                }
            }
            L.debug("===============OSS INFO check=========formatFileBucketUrlForStream=" +rec);
            post_id = p.post(viewerId, type, msg, formatFileBucketUrlForStream(viewerId,rec).toString(false, false), qp.getString("appid", "1"),
                    "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to);
            if (fids.size() > 0 && !post_id.equals(""))
                folder.updateStaticFileStreamId(post_id, fids);
            return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
        } else if (fi == null && !share_file_id.equals("")) {
            Record old_file_info = folder.getOriginalStaticFileByIds(share_file_id).getFirstRecord();
            old_file_info.put("user_id", viewerId);

            String folder_id = folder.getFolder(viewerId, folder.FOLDER_TYPE_SHARE_OUT, "Sharing Files");
            if (!folder.isFolderExist(folder_id)) {
                throw new PhotoException("folder not exist, please create folder first");
            }
            old_file_info.put("folder_id", folder_id);
            old_file_info.put("created_time", DateUtils.nowMillis());
            old_file_info.put("updated_time", DateUtils.nowMillis());
            folder.saveStaticFile(old_file_info);
            String file_id = old_file_info.getString("file_id");
            fids.add(file_id);

            if (!groupIds.isEmpty()) {
                RecordSet group_recs = dealWithGroupFile(p, old_file_info, groupIds);
                fids.add(file_id);
                folder.saveStaticFiles(group_recs);
            }

            Record rec = formatFileBucketUrl(viewerId, old_file_info);
            String msg = qp.getString("msg", "");
            int type = Constants.FILE_POST;
//            if (rec.getString("content_type").contains("image/"))
//                type = Constants.PHOTO_POST;
            if (rec.getString("content_type").contains("video/")) {
                type = Constants.VIDEO_POST;
            } else if (rec.getString("content_type").contains("audio/")) {
                type = Constants.AUDIO_POST;
            }


            List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < 10)
                        l00.add(l011);
                }
            }
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= 10) {
                        try {
                            String other_folder_id = folder.getFolder(uid, folder.FOLDER_TYPE_RECEIVED, "Received Files");
                            if (old_file_info.has("file_url"))
                                old_file_info.removeColumns("file_url");
                            if (old_file_info.has("thumbnail_url"))
                                old_file_info.removeColumns("thumbnail_url");
                            if (old_file_info.has("likes"))
                                old_file_info.removeColumns("likes");
                            if (old_file_info.has("comments"))
                                old_file_info.removeColumns("comments");

                            old_file_info.put("folder_id", other_folder_id);
                            old_file_info.put("user_id", uid);
                            folder.saveStaticFile(old_file_info);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            post_id = p.post(viewerId, type, msg, formatFileBucketUrlForStream(viewerId,rec).toString(false, false), qp.getString("appid", "1"),
                    "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to);
            if (fids.size() > 0 && !post_id.equals(""))
                folder.updateStaticFileStreamId(post_id, fids);
            return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
        } else {
            L.debug("share file error");
            return new Record();
        }
    }

    @WebMethod("v2/folder/create")
    public boolean createFolder(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String userId = p.checkTicket(qp);
        if (null == folder)
            throw new PhotoException("server error, can't save");
        String folder_name = qp.checkGetString("title");
        int visible = (int) qp.getInt("privacy", 0);         //0 open 1 only me 2 friend open
        String description = qp.getString("summary", "");
        String folder_id = Long.toString(RandomUtils.generateId());
        Record rc = new Record();

        rc.put("folder_id", folder_id);
        rc.put("folder_type", folder.FOLDER_TYPE_OTHERS);
        rc.put("user_id", userId);
        rc.put("title", folder_name);
        rc.put("created_time", DateUtils.nowMillis());
        rc.put("summary", description);
        rc.put("privacy", visible);
        folder.createFolder(rc);
        return true;
    }


    @WebMethod("v2/folder/all")
    public RecordSet getFolders(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        String ua = getDecodeHeader(req, "User-Agent", "", "");
        String userId = qp.getString("user_id", viewerId);
        if (null == folder)
            throw new PhotoException("server error, can't query folder");

        RecordSet recs = folder.getUserFolder(viewerId, userId);
        L.debug("folder/all:recs=" + recs.toString());
        for (Record rec : recs) {
            rec.put("title", formatFolderName(ua, (int) rec.getInt("folder_type"), rec.getString("title")));
        }
        L.debug("folder/all:recs new=" + recs.toString());
        return recs;
    }

    public String formatFolderName(String ua, int folder_type, String folder_name) {
        if (folder_type == folder.FOLDER_TYPE_SHARE_OUT)
            folder_name = Constants.getBundleString(ua, "folder.name.sharing");
        if (folder_type == folder.FOLDER_TYPE_RECEIVED)
            folder_name = Constants.getBundleString(ua, "folder.name.received");
        if (folder_type == folder.FOLDER_TYPE_MY_SYNC)
            folder_name = Constants.getBundleString(ua, "folder.name.cloud");
        return folder_name;
    }

    @WebMethod("v2/folder/get")
    public Record getFolderById(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = p.checkSignAndTicket(qp);
        }
        String ua = getDecodeHeader(req, "User-Agent", "", "");
        String userId = qp.getString("user_id", viewerId);
        String folder_id = qp.checkGetString("folder_id");
        if (null == folder)
            throw new PhotoException("server error, can't query folder");

        Record rec = folder.getFolderById(viewerId, userId, folder_id);
        rec.put("title", formatFolderName(ua, (int) rec.getInt("folder_type"), rec.getString("title")));
        return rec;
    }

    @WebMethod("v2/folder/update")
    public boolean updateFolder(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        if (null == folder)
            throw new PhotoException("server error, can't update folder");

        String folder_id = qp.checkGetString("folder_id");
        Record r = folder.getFolderOriginal(folder_id);
        int folder_type = (int) r.getInt("folder_type");
        if (folder_type != folder.FOLDER_TYPE_OTHERS)
            throw new PhotoException("only can update user folder");
        if (!viewerId.equals(r.getString("user_id")))
            throw new PhotoException("can't update other folder");
        String folder_name = qp.getString("title", null);
        String description = qp.getString("summary", null);
        String visible = qp.getString("privacy", null);

        if (!StringUtils.isNotBlank(visible) && visible != null) {
            if (!visible.equals("0") && !visible.equals("1") && !visible.equals("2"))
                throw new PhotoException("privacy error, privacy must be 0,1,2");
        }

        Record rc = new Record();
        if (StringUtils.isNotBlank(folder_name) && folder_name != null) {
            rc.put("title", folder_name);
        }
        if (StringUtils.isNotBlank(description) && description != null) {
            rc.put("summary", description);
        }
        if (StringUtils.isNotBlank(visible) && visible != null) {
            rc.put("privacy", Integer.valueOf(visible));
        }

        return folder.updateFolder(folder_id, rc);
    }

    @WebMethod("v2/folder/delete")
    public boolean deleteFolder(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        if (null == folder)
            throw new PhotoException("server error, can't delete folder");

        String folder_id = qp.checkGetString("folder_id");
        Record folder0 = folder.getFolderOriginal(folder_id);
        if (folder0.getInt("folder_type") != folder.FOLDER_TYPE_OTHERS)
            throw new PhotoException("can't delete this folder");
        if (!viewerId.equals(folder0.getString("user_id")))
            throw new PhotoException("can't delete other folder");
        return folder.deleteFolderById(viewerId, folder_id);
    }

    @WebMethod("v2/file/get")
    public RecordSet getFileByIds(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        RecordSet recs = folder.getStaticFileByIds(qp.checkGetString("file_ids"));
        for (Record rec : recs) {
            rec = formatFileBucketUrl(viewerId, rec);
        }
        return recs;
    }

    @WebMethod("v2/file/folder_get")
    public RecordSet getFileByFolderIds(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        List<String> folder_ids0 = StringUtils2.splitList(qp.checkGetString("folder_ids"), ",", true);
        Record folder_ = folder.getFolderOriginal(folder_ids0.get(0));
        String user_id = folder_.getString("user_id");
//        String cols = "photo_id,album_id,user_id,img_middle,img_original,img_big,img_small,caption,created_time,location,tag,tag_ids,from_user,original_pid,longitude,latitude,orientation,stream_id,privacy";
        RecordSet recs = folder.getFolderFiles(viewerId, folder_ids0.get(0), page, count);
        for (Record rec : recs) {
            rec = formatFileBucketUrl(viewerId, rec);
        }
        return recs;
    }

    @WebMethod("v2/file/update")
    public boolean updateFile(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        if (null == folder)
            throw new PhotoException("server error, can't update folder");

        String file_id = qp.checkGetString("file_id");
        String summary = qp.getString("summary", null);
        String description = qp.getString("description", null);
        Record rc = new Record();
        if (summary != null) {
            rc.put("summary", summary);
        }
        if (description != null) {
            rc.put("description", description);
        }
        return folder.updateFile(file_id, rc);
    }

    @WebMethod("v2/file/delete")
    public boolean deleteFile(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        if (null == folder)
            throw new PhotoException("server error, can't delete file");
        String fIDs = qp.checkGetString("file_ids");
        boolean delete_all = qp.getBoolean("delete_all", false);
        return folder.deleteFileById(viewerId, fIDs, delete_all);
    }

//    @WebMethod("v2/file/my_files")
//    public RecordSet fileGetMyShare(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        Platform p = platform();
//        String viewerId = p.checkTicket(qp);
//        String file_type = qp.checkGetString("file_type");   //video,audio,static_file
//        int page = (int) qp.getInt("page", 0);
//        int count = (int) qp.getInt("count", 20);
//        if (count > 100)
//            count = 100;
//        boolean asc = qp.getBoolean("asc", false);
//        RecordSet recs = p.fileGetMyShare(viewerId, file_type, asc, page, count);
//        for (Record rec : recs) {
//            rec = formatFileBucketUrl(viewerId,rec);
//        }
//        return recs;
//    }

    public Record formatFileBucketUrl(String viewerId, Record rec) throws AvroRemoteException {
        //http://storage.aliyun.com/wutong-photo/%s
        if (!rec.isEmpty()) {
            Platform p = platform();
            rec = formatFileBucketUrlForStream(viewerId, rec);
            String file_id = rec.getString("file_id");

            Record Rec_file_like = new Record();
            String objectFileId = String.valueOf(Constants.FILE_OBJECT) + ":" + String.valueOf(file_id);
            int file_like_count = p.likeGetCount(objectFileId);
            Rec_file_like.put("count", file_like_count);
            if (file_like_count > 0) {
                RecordSet recs_liked_users = p.loadLikedUsers(objectFileId, 0, 5);
                List<Long> list_file_liked_users = recs_liked_users.getIntColumnValues("liker");
                String likeuids = StringUtils.join(list_file_liked_users, ",");
                RecordSet recs_user_liked = p.getUsers(rec.getString("source"), likeuids, p.USER_LIGHT_COLUMNS_LIGHT);
                Rec_file_like.put("users", recs_user_liked);
            } else {
                Rec_file_like.put("users", new Record());//3
            }

            Rec_file_like.put("iliked", viewerId.equals("") ? false : p.ifuserLiked(viewerId, objectFileId));
            rec.put("likes", Rec_file_like);

            Record Rec_comment = new Record();
            int comment_count = p.getCommentCount(viewerId, Constants.FILE_OBJECT, String.valueOf(file_id));
            Rec_comment.put("count", comment_count);
            if (comment_count > 0) {
                RecordSet recs_com = p.getCommentsForContainsIgnore(viewerId, Constants.FILE_OBJECT, file_id, p.FULL_COMMENT_COLUMNS, false, 0, 2);
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

    public Record formatFileBucketUrlForStream(String viewerId, Record rec) throws AvroRemoteException {
        //http://storage.aliyun.com/wutong-photo/%s
        if (!rec.isEmpty()) {
            Platform p = platform();
            Configuration conf = getConfiguration();
            String content_type = rec.getString("content_type");
            String key = formatBucketKey(content_type, p);
            String new_file_name = rec.getString("new_file_name");
            String f0[] = StringUtils.split(new_file_name,"_");
            String v_id = f0.length > 0 ? f0[0].toString() : viewerId;
            rec.put("file_url", String.format(conf.checkGetString("platform.fileUrlPattern") + p.bucketName + "/" + key + v_id + "/" + new_file_name));
            String thumbnail = "";
            if (content_type.contains("/video")) {
                Record rec_video = folder.getVideoById(rec.getString("file_id"));
                thumbnail = String.format(conf.checkGetString("platform.fileUrlPattern") + p.bucketName + "/" + key + v_id + "/" + rec_video.getString("thumbnail"));
            }
            rec.put("thumbnail_url", thumbnail);
            return rec;
        } else {
            return new Record();
        }
    }

    @WebMethod("v2/configuration/upload")
    public boolean saveConfigration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        FileItem fi = qp.checkGetFile("file");
        Record configration = new Record();
        String config_key = qp.checkGetString("config_key");
        int version_code = (int) qp.getInt("version_code", 0);
        String value = qp.getString("value", "");
        int content_type = 0;
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            String folder_id = folder.getFolder(viewerId, folder.FOLDER_TYPE_CONFIGURATION, "Configuration Files");
            String file_id = Long.toString(RandomUtils.generateId());
            Record rec = formatFileBucketUrl(viewerId, uploadFile(p, viewerId, file_id, Long.parseLong(folder_id), fi, "", "", "", null, ""));
            content_type = 1;
            value = rec.getString("file_url");
        }
        configration.put("user_id", viewerId);
        configration.put("config_key", config_key);
        configration.put("version_code", version_code);
        configration.put("value", value);
        configration.put("content_type", content_type);
        boolean b = p.saveConfigration(viewerId, configration);
        return b;
    }

    @WebMethod("v2/configuration/upload/internal")
    public boolean saveNoTicketConfiguration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String viewerId = qp.checkGetString("id");
        p.checkConfigurationId(qp);
        FileItem fi = qp.checkGetFile("file");
        Record configration = new Record();
        String config_key = qp.checkGetString("config_key");
        int version_code = (int) qp.getInt("version_code", 0);
        String value = qp.getString("value", "");
        int content_type = 0;
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            String folder_id = folder.getFolder(viewerId, folder.FOLDER_TYPE_CONFIGURATION, "Configuration Files");
            String file_id = Long.toString(RandomUtils.generateId());
            Record rec = formatFileBucketUrl(viewerId, uploadFile(p, viewerId, file_id, Long.parseLong(folder_id), fi, "", "", "", null, ""));
            content_type = 1;
            value = rec.getString("file_url");
        }
        configration.put("user_id", viewerId);
        configration.put("config_key", config_key);
        configration.put("version_code", version_code);
        configration.put("value", value);
        configration.put("content_type", content_type);
        boolean b = p.saveConfigration(viewerId, configration);
        return b;
    }

    @WebMethod("v2/configuration/get")
    public Record getConfigration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        String config_key = qp.checkGetString("config_key");
        int version_code = (int) qp.getInt("version_code", 0);
        RecordSet recs = p.getConfigration(viewerId, config_key, version_code);
        return recs.getFirstRecord();
    }

    @WebMethod("v2/configuration/get/internal")
    public Record getNoTicketConfiguration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        p.checkConfigurationId(qp);
        String viewerId = qp.checkGetString("id");
        String config_key = qp.checkGetString("config_key");
        int version_code = (int) qp.getInt("version_code", 0);
        RecordSet recs = p.getConfigration(viewerId, config_key, version_code);
        return recs.getFirstRecord();
    }

    @WebMethod("v2/configuration/all")
    public RecordSet getUserConfigration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        RecordSet recs = p.getUserConfigration(viewerId);
        return recs;
    }

    @WebMethod("v2/configuration/delete")
    public boolean deleteConfigration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        String config_key = qp.getString("config_key", "");
        int version_code = (int) qp.getInt("version_code", -1);
        boolean b = p.deleteConfigration(viewerId, config_key, version_code);
        return b;
    }

    public long getParamInt(QueryParams qp, String preferKey, String secondKey, boolean isMust, long def) {
        if (qp.containsKey(preferKey))
            return isMust ? qp.checkGetInt(preferKey) : qp.getInt(preferKey, def);
        else
            return isMust ? qp.checkGetInt(secondKey) : qp.getInt(secondKey, def);
    }

    public String getParamString(QueryParams qp, String preferKey, String secondKey, boolean isMust, String def) {
        if (qp.containsKey(preferKey))
            return isMust ? qp.checkGetString(preferKey) : qp.getString(preferKey, def);
        else
            return isMust ? qp.checkGetString(secondKey) : qp.getString(secondKey, def);
    }

    private void addBorqsStaffsToGroup(Platform p, long groupId) throws AvroRemoteException {
        List<String> staffIds = StringUtils2.splitList(p.getBorqsUserIds(), ",", true);
        Record staffs = new Record();
        for (String staffId : staffIds)
            staffs.put(staffId, ROLE_MEMBER);

        List<String> excludes = StringUtils2.splitList(p.getCreatorAndAdmins(groupId), ",", true);
        for (String exclude : excludes)
            staffs.remove(exclude);
        p.addMembers(groupId, staffs, false);
    }

    private Record createGroup(long begin, String groupType, QueryParams qp, HttpServletRequest req, Platform p, String viewerId) throws AvroRemoteException, UnsupportedEncodingException {
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);

        String name = qp.checkGetString("name");
        int memberLimit = (int) qp.getInt("member_limit", 1000);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        int isStreamPublic = 1;
        int canSearch = 1;
        int canViewMembers = 1;
        int privacy = (int) qp.getInt("privacy", PRIVACY_OPEN);
        if (privacy == PRIVACY_CLOSED) {
            isStreamPublic = 0;
        } else if (privacy == PRIVACY_SECRET) {
            isStreamPublic = 0;
            canSearch = 0;
            canViewMembers = 0;
        }

        boolean sendPost = qp.getBoolean("send_post", true);
        int canJoin = (int) qp.getInt("can_join", 1);
        int canMemberInvite = (int) qp.getInt("can_member_invite", 1);
        int canMemberApprove = (int) qp.getInt("can_member_approve", 1);
        int canMemberPost = (int) qp.getInt("can_member_post", 1);
        int canMemberQuit = (int) qp.getInt("can_member_quit", 1);
        int needInvitedConfirm = (int) qp.getInt("need_invited_confirm", 1);
        String label = qp.getString("label", "其它");
        String members = qp.getString("members", "");
        String sNames = qp.getString("names", "");

        List<String> toIds = StringUtils2.splitList(members, ",", true);
        List<String> names = StringUtils2.splitList(sNames, ",", true);

        List<String> borqsIds = new ArrayList<String>();
        List<String> identifies = new ArrayList<String>();
        List<String> identifyNames = new ArrayList<String>();
        List<String> virtuals = new ArrayList<String>();
        int size = toIds.size();

        for (int i = 0; i < size; i++) {
            String toId = toIds.get(i);
            String toName = names.get(i);

            int type = p.getTypeByStr(toId);
            if (type == 0)
                borqsIds.add(toId);
            else if (type == 3) {
                String circleId = StringUtils.removeStart(toId, "#");
                RecordSet friendRecs = p.getFriends0(viewerId, circleId, 0, -1);
                String userIds = friendRecs.joinColumnValues("friend", ",");
                RecordSet users = p.getUsers(viewerId, userIds, "user_id,display_name", false);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    int lcIdType = p.getTypeByStr(userId);
                    if (lcIdType == 0)
                        borqsIds.add(userId);
                    else if (lcIdType == 4)
                        virtuals.add(userId);
                }
            } else if (type == 5) {
                String fromGroup = StringUtils.removeStart(toId, "$");
                String userIds = p.getGroupMembers(Long.parseLong(fromGroup));
                RecordSet users = p.getUsers(viewerId, userIds, "user_id,display_name", false);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    borqsIds.add(userId);
                }
            } else if (type == 4)
                virtuals.add(toId);
            else {
                identifies.add(toId);
                identifyNames.add(toName);
            }
        }

        String virtualIds = StringUtils2.joinIgnoreBlank(",", virtuals);
        if (StringUtils.isNotBlank(virtualIds)) {
            RecordSet vUsers = p.getContentByVirtualIds(virtualIds);
            for (Record vUser : vUsers) {
                identifies.add(vUser.getString("content"));
                identifyNames.add(vUser.getString("name"));
            }
        }

        String message = qp.getString("message", "");

        Record properties = new Record(qp);
        properties.remove("appid");
        properties.remove("sign");
        properties.remove("sign_method");
        properties.remove("ticket");

        properties.remove("name");
        properties.remove("member_limit");
        properties.remove("is_stream_public");
        properties.remove("can_search");
        properties.remove("can_view_members");
        properties.remove("privacy");
        properties.remove("can_join");
        properties.remove("can_member_invite");
        properties.remove("can_member_approve");
        properties.remove("can_member_post");
        properties.remove("can_member_quit");
        properties.remove("need_invited_confirm");
        properties.remove("members");
        properties.remove("names");
        properties.remove("admins");
        properties.remove("message");
        properties.remove("label");
        properties.remove("borqs_staff");
        properties.remove("call_id");
        properties.remove("send_post");


        long groupId = p.createGroup(begin, groupType, name, memberLimit, isStreamPublic, canSearch,
                canViewMembers, canJoin, canMemberInvite, canMemberApprove, canMemberPost, canMemberQuit, needInvitedConfirm, Long.parseLong(viewerId), label, properties,
                viewerId, ua, loc, appId, sendPost);

        RecordSet recs = new RecordSet();
        if (needInvitedConfirm == 1) {
            L.debug("Begin borqs user invite = " + DateUtils.nowMillis());
            String uids = StringUtils2.joinIgnoreBlank(",", borqsIds);
            try {
                RecordSet recs0 = p.inviteMembers(groupId, uids, viewerId, message, ua, loc, appId);
                recs.addAll(recs0);
            } catch (Exception ne) {
                L.error("Fail to invite borqs user = " + uids, ne);
            }
            L.debug("End borqs user invite = " + DateUtils.nowMillis());

            L.debug("Begin email or sms invite = " + DateUtils.nowMillis());
            int identifySize = identifies.size();
            for (int i = 0; i < identifySize; i++) {
                final String identify = identifies.get(i);
                try {
                    Record r = p.inviteMember(groupId, identify, names.get(i), viewerId, message, ua, loc, appId);
                    r.put("key", identify);
                    recs.add(r);
                } catch (Exception ne) {
                    L.error("Fail to invite email or sms = " + identify, ne);
                }
            }
            L.debug("End email or sms invite = " + DateUtils.nowMillis());
        } else {
            Record membersRec = new Record();
            for (String borqsId : borqsIds)
                membersRec.put(borqsId, ROLE_MEMBER);

            List<String> excludes = StringUtils2.splitList(p.getCreatorAndAdmins(groupId), ",", true);
            for (String exclude : excludes)
                membersRec.remove(exclude);
            RecordSet recs0 = p.addMembers(groupId, membersRec, false);
            recs.addAll(recs0);

            L.debug("Begin email or sms invite = " + DateUtils.nowMillis());
            int identifySize = identifies.size();
            for (int i = 0; i < identifySize; i++) {
                final String identify = identifies.get(i);
                try {
                    Record r = p.inviteMember(groupId, identify, names.get(i), viewerId, message, ua, loc, appId);
                    r.put("key", identify);
                    recs.add(r);
                } catch (Exception ne) {
                    L.error("Fail to invite email or sms = " + identify, ne);
                }
            }
            L.debug("End email or sms invite = " + DateUtils.nowMillis());
        }

        Record rs = new Record();
        rs.put("result", groupId);
        rs.put("group_id", groupId);
        rs.put("users", recs);

        //Borqs Staff
        boolean borqsStaff = qp.getBoolean("borqs_staff", false);
        if (borqsStaff) {
            addBorqsStaffsToGroup(p, groupId);
        }

        return rs;
    }

    @WebMethod("v2/public_circle/create")
    public Record createPublicCircle(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        return createGroup(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.TYPE_PUBLIC_CIRCLE, qp, req, p, viewerId);
    }

    private boolean updateGroup(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        Record info = new Record();
        if (qp.containsKey("privacy")) {
            int isStreamPublic = 1;
            int canSearch = 1;
            int canViewMembers = 1;
            int privacy = (int) qp.getInt("privacy", PRIVACY_OPEN);
            if (privacy == PRIVACY_CLOSED) {
                isStreamPublic = 0;
            } else if (privacy == PRIVACY_SECRET) {
                isStreamPublic = 0;
                canSearch = 0;
                canViewMembers = 0;
            }

            info.put(COL_IS_STREAM_PUBLIC, isStreamPublic);
            info.put(COL_CAN_SEARCH, canSearch);
            info.put(COL_CAN_VIEW_MEMBERS, canViewMembers);
        }

        Record properties = new Record();

        String[] buildInParams = new String[]{"sign_method", "sign", "appid", "ticket", "circle_id", "privacy", "call_id"};
        String[] groupParams = new String[]{COL_NAME, COL_MEMBER_LIMIT, COL_IS_STREAM_PUBLIC, COL_CAN_SEARCH,
                COL_CAN_VIEW_MEMBERS, COL_CAN_JOIN, COL_CAN_MEMBER_INVITE, COL_CAN_MEMBER_APPROVE, COL_CAN_MEMBER_POST, COL_CAN_MEMBER_QUIT, COL_NEED_INVITED_CONFIRM, COL_LABEL};

        for (Map.Entry<String, Object> entry : qp.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!ArrayUtils.contains(buildInParams, key)) {
                if (ArrayUtils.contains(groupParams, key))
                    info.put(key, value);
                else
                    properties.put(key, value);
            }
        }

        if (properties.has(GroupConstants.COMM_COL_BULLETIN))
            properties.put(GroupConstants.COMM_COL_BULLETIN_UPDATED_TIME, DateUtils.nowMillis());

        return p.updateGroup(viewerId, groupId, info, properties);
    }

    @WebMethod("v2/public_circle/update")
    public boolean updatePublicCircle(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);

        return updateGroup(groupId, qp);
    }

    private boolean uploadGroupProfileImage(long groupId, QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        if (!p.hasGroupRight(groupId, viewerId, ROLE_ADMIN))
            throw new GroupException("You do not have right to upload group profile image");

        FileItem fi = qp.checkGetFile("profile_image");

        long uploaded_time = DateUtils.nowMillis();
        String imageName = "profile_" + groupId + "_" + uploaded_time;
        String loc = getDecodeHeader(req, "location", "", String.valueOf(groupId));

        String album_id = qp.getString("album_id", "");
        if (StringUtils.isEmpty(album_id))
            album_id = photo.getAlbum(String.valueOf(groupId), photo.ALBUM_TYPE_PROFILE, "Profile Pictures");
        if (!photo.isAlbumExist(album_id)) {
            throw new PhotoException("album not exist, please create album first");
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

        String photoID = photo.genPhotoId(String.valueOf(groupId));
        Record rc_photo = new Record();
        rc_photo.put("photo_id", photoID);
        rc_photo.put("album_id", album_id);
        rc_photo.put("user_id", String.valueOf(groupId));
        rc_photo.put("img_middle", imageName + "_M.jpg");
        rc_photo.put("img_original", imageName + "_M.jpg");
        rc_photo.put("img_big", imageName + "_L.jpg");
        rc_photo.put("img_small", imageName + "_S.jpg");
        rc_photo.put("caption", "profile_image");
        rc_photo.put("location", loc);
        rc_photo.put("created_time", DateUtils.nowMillis());
        photo.saveUploadPhoto(rc_photo);

        Record properties = Record.of("image_url", imageName + "_M.jpg", "small_image_url", imageName + "_S.jpg",
                "large_image_url", imageName + "_L.jpg",
                "original_image_url", imageName + "_M.jpg");

        return p.updateGroup(viewerId, groupId, new Record(), properties);
    }

    @WebMethod("v2/public_circle/upload_profile_image")
    public boolean uploadPublicCircleProfileImage(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return uploadGroupProfileImage(groupId, qp, req);
    }

    @WebMethod("v2/public_circle/destroy")
    public boolean destroyPublicCircle(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        String groupIds = getParamString(qp, "circle_ids", "ids", true, "");
        return p.destroyGroup(viewerId, groupIds);
    }

    private RecordSet getGroups(long begin, long end, String groupIds, QueryParams qp, Platform p, String viewerId) throws AvroRemoteException {
        boolean isSingle = StringUtils.isNotBlank(groupIds) && !StringUtils.contains(groupIds, ",");
        String cols = qp.getString("columns", "");
        String columns = StringUtils.isBlank(cols) ? GroupConstants.GROUP_LIGHT_COLS : GroupConstants.GROUP_LIGHT_COLS + "," + cols;
        boolean withMembers = qp.getBoolean("with_members", false);
        if (!isSingle) {
            RecordSet recs = p.getGroups(begin, end, viewerId, groupIds, columns, withMembers);
            recs.renameColumn(GRP_COL_ID, "circle_id");
            recs.renameColumn(GRP_COL_NAME, "circle_name");
            return recs;
        }
        else {
            Record rec = p.getGroup(viewerId, Long.parseLong(groupIds), columns, withMembers);
            rec.renameColumn(GRP_COL_ID, "circle_id");
            rec.renameColumn(GRP_COL_NAME, "circle_name");

            // return three status 5 users and three status count
            int count = (int) qp.getInt("count", 5);
            Record appliedCount = p.getUsersCounts(groupIds, GroupConstants.STATUS_APPLIED);
            Record invitedCount = p.getUsersCounts(groupIds, GroupConstants.STATUS_INVITED);

            long groupId = rec.getInt("circle_id");

            rec.put("applied_count", appliedCount.getInt(String.valueOf(groupId), 0));
            rec.put("invited_count", invitedCount.getInt(String.valueOf(groupId), 0));

            RecordSet members = p.getGroupUsersByStatus(viewerId, groupId, String.valueOf(GroupConstants.STATUS_JOINED), -1, -1, "");
            RecordSet applied = p.getGroupUsersByStatus(viewerId, groupId, String.valueOf(GroupConstants.STATUS_APPLIED), 0, count, "");
            RecordSet invited = p.getGroupUsersByStatus(viewerId, groupId, String.valueOf(GroupConstants.STATUS_INVITED), -1, -1, "");
            rec.put("profile_members", members.size() > count ? members.subList(0, count) : members);
            rec.put("profile_applied", applied);
            rec.put("profile_invited", invited.size() > count ? invited.subList(0, count) : invited);

            RecordSet friends = p.getFriends(viewerId, viewerId, Integer.toString(FRIENDS_CIRCLE), "user_id", false, -1, -1);
            List<String> memberIds = StringUtils2.splitList(members.joinColumnValues("user_id", ","), ",", true);
            List<String> invitedIds = StringUtils2.splitList(invited.joinColumnValues("user_id", ","), ",", true);
            List<String> friendIds0 = StringUtils2.splitList(friends.joinColumnValues("user_id", ","), ",", true);
            List<String> friendIds1 = new ArrayList<String>();
            friendIds1.addAll(friendIds0);
            List<String> l = new ArrayList<String>();
            friendIds0.retainAll(memberIds);
            friendIds1.retainAll(invitedIds);
            l.addAll(friendIds0);
            l.addAll(friendIds1);
            rec.put("invited_ids", StringUtils2.joinIgnoreBlank(",", l));

            //latest post
//            Record post = p.getFullUsersTimelineForQiuPu(viewerId, String.valueOf(groupId), 0, DateUtils.nowMillis(), ALL_POST, String.valueOf(APP_TYPE_BPC), 0, 1).getFirstRecord();
//            rec.put("latest_post", post);

            return RecordSet.of(rec);
        }
    }

    @WebMethod("v2/public_circle/show")
    public RecordSet getPublicCircles(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String groupIds = getParamString(qp, "circle_ids", "ids", false, "");
        return getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.ACTIVITY_ID_BEGIN, groupIds, qp, p, viewerId);
    }

    @WebMethod("v2/public_circle/detail")
    public Record getPublicCircleDetail(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String groupId = getParamString(qp, "circle_id", "id", false, "");
        RecordSet recs = getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.ACTIVITY_ID_BEGIN, groupId, qp, p, viewerId);
        if (recs.isEmpty()) {
            throw new PlatformException(ErrorCode.GROUP_NOT_EXISTS, "The public circle is not exist");
        } else {
            return recs.get(0);
        }
    }

    private RecordSet getGroupUsers(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "0";
        String ticket = qp.getString("ticket", "");
        if (StringUtils.isNotBlank(ticket))
            viewerId = p.checkSignAndTicket(qp);

        boolean admins = qp.getBoolean("admins", false);
        String status = qp.getString("status", String.valueOf(GroupConstants.STATUS_JOINED));
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 100);

        if (admins)
            return RecordSet.of(Record.of("admins", p.getCreatorAndAdmins(groupId)));
        else {
            String searchKey = qp.getString("key", "");
            return p.getGroupUsersByStatus(viewerId, groupId, status, page, count, searchKey);
        }
    }

    /*private RecordSet getGroupUsersUnion(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "0";
        String ticket = qp.getString("ticket", "");
        if (StringUtils.isNotBlank(ticket))
            viewerId = p.checkSignAndTicket(qp);

        boolean admins = qp.getBoolean("admins", false);
        String status = qp.getString("status", String.valueOf(GroupConstants.STATUS_JOINED));
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 100);

        if (admins)
            return RecordSet.of(Record.of("admins", p.getCreatorAndAdmins(groupId)));
        else {
            String searchKey = qp.getString("key", "");
            return p.getGroupUsersByStatusUnion(viewerId, groupId, status, page, count, searchKey);
        }
    }*/
    @WebMethod("v2/public_circle/users")
    public RecordSet getPublicCircleUsers(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return getGroupUsers(groupId, qp);
    }

    /*@WebMethod("v2/public_circle/test")
    public List<String> getPublicCircleTest(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        RecordSet rs0 = getGroupUsers(groupId, qp);

        RecordSet rs1 = getGroupUsersUnion(groupId, qp);

        //check the different record between rs0 rs1
        Map<String, Record> map0 = rs0.toRecordMap("user_id");
        Map<String, Record> map1 = rs1.toRecordMap("user_id");
        List<String> listError = new ArrayList<String>();

        for (Map.Entry<String, Record> entry : map0.entrySet()) {
            if (!map1.containsKey(entry.getKey())) {
                StringUtils.center("error key NULL" + entry.getKey(), 40, "=");
                listError.add(entry.getKey());
                continue;
            }
            Record r0 = entry.getValue();
            Record r1 = map1.get(entry.getKey());

            for (String s : r0.getColumns()) {
                if (!r1.getString(s).equals(r0.getString(s))) {
                    StringUtils.center("error columns NULL" + s, 40, "=");
                    listError.add(entry.getKey()+s);
                }
            }
        }

        return listError;


    }*/

    private RecordSet groupInvite(long groupId, QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String tos = qp.checkGetString("to");
        String sNames = qp.checkGetString("names");
        List<String> toIds = StringUtils2.splitList(tos, ",", true);
        List<String> names = StringUtils2.splitList(sNames, ",", true);

        List<String> borqsIds = new ArrayList<String>();
        List<String> identifies = new ArrayList<String>();
        List<String> identifyNames = new ArrayList<String>();
        List<String> virtuals = new ArrayList<String>();
        int size = toIds.size();

        for (int i = 0; i < size; i++) {
            String toId = toIds.get(i);
            String toName = names.get(i);

            int type = p.getTypeByStr(toId);
            if (type == 0)
                borqsIds.add(toId);
            else if (type == 3) {
                String circleId = StringUtils.removeStart(toId, "#");
                RecordSet friendRecs = p.getFriends0(viewerId, circleId, 0, -1);
                String userIds = friendRecs.joinColumnValues("friend", ",");
                RecordSet users = p.getUsers(viewerId, userIds, "user_id,display_name", false);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    int lcIdType = p.getTypeByStr(userId);
                    if (lcIdType == 0)
                        borqsIds.add(userId);
                    else if (lcIdType == 4)
                        virtuals.add(userId);
                }
            } else if (type == 5) {
                String fromGroup = StringUtils.removeStart(toId, "$");
                String userIds = p.getGroupMembers(Long.parseLong(fromGroup));
                RecordSet users = p.getUsers(viewerId, userIds, "user_id,display_name", false);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    borqsIds.add(userId);
                }
            } else if (type == 4)
                virtuals.add(toId);
            else {
                identifies.add(toId);
                identifyNames.add(toName);
            }
        }

        String virtualIds = StringUtils2.joinIgnoreBlank(",", virtuals);
        if (StringUtils.isNotBlank(virtualIds)) {
            RecordSet vUsers = p.getContentByVirtualIds(virtualIds);
            for (Record vUser : vUsers) {
                identifies.add(vUser.getString("content"));
                identifyNames.add(vUser.getString("name"));
            }
        }

        String message = qp.getString("message", "");
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        RecordSet recs = new RecordSet();

        Record groupRec = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.GROUP_ID_END,
                String.valueOf(groupId), COL_NEED_INVITED_CONFIRM + "," + COL_NAME).getFirstRecord();
        long needInvitedConfirm = groupRec.getInt(COL_NEED_INVITED_CONFIRM, 1);
        String groupName = groupRec.getString(COL_NAME);
        Record source = p.getUser(viewerId, viewerId, "user_id, display_name");
        String viewerName = source.getString("display_name");

        if (needInvitedConfirm == 1) {
            L.debug("Begin borqs user invite = " + DateUtils.nowMillis());
            String uids = StringUtils2.joinIgnoreBlank(",", borqsIds);
            try {
                RecordSet recs0 = p.inviteMembers(groupId, uids, viewerId, message, ua, loc, appId);
                recs.addAll(recs0);
            } catch (Exception ne) {
                L.error("Fail to invite borqs user = " + uids, ne);
            }
            L.debug("End borqs user invite = " + DateUtils.nowMillis());

            L.debug("Begin email or sms invite = " + DateUtils.nowMillis());
            int identifySize = identifies.size();
            for (int i = 0; i < identifySize; i++) {
                final String identify = identifies.get(i);
                try {
                    Record r = p.inviteMember(groupId, identify, names.get(i), viewerId, message, ua, loc, appId);
                    r.put("key", identify);
                    recs.add(r);
                } catch (Exception ne) {
                    L.error("Fail to invite email or sms = " + identify, ne);
                }
            }
            L.debug("End email or sms invite = " + DateUtils.nowMillis());
        } else {
            Record membersRec = new Record();
            for (String borqsId : borqsIds)
                membersRec.put(borqsId, ROLE_MEMBER);

            List<String> excludes = StringUtils2.splitList(p.getCreatorAndAdmins(groupId), ",", true);
            for (String exclude : excludes)
                membersRec.remove(exclude);
            RecordSet recs0 = p.addMembers(groupId, membersRec, false);
            recs.addAll(recs0);
            String groupType = p.getGroupTypeStr(groupId, ua);
            p.sendNotification(Constants.NTF_GROUP_INVITE,
                    p.createArrayNodeFromStrings(appId),
                    p.createArrayNodeFromStrings(viewerId),
                    p.createArrayNodeFromStrings(viewerName, groupType, groupName, "将"),
                    p.createArrayNodeFromStrings(),
                    p.createArrayNodeFromStrings(),
                    p.createArrayNodeFromStrings(String.valueOf(groupId)),
                    p.createArrayNodeFromStrings(viewerName, groupType, groupName, viewerId, String.valueOf(groupId), "将"),
                    p.createArrayNodeFromStrings(message),
                    p.createArrayNodeFromStrings(message),
                    p.createArrayNodeFromStrings(String.valueOf(groupId)),
                    p.createArrayNodeFromStrings(StringUtils2.joinIgnoreBlank(",", borqsIds))
            );
            p.sendGroupNotification(groupId, new GroupInviteNotifSender(p, null), viewerId, new Object[]{StringUtils2.joinIgnoreBlank(",", borqsIds)}, message,
                    viewerName, groupType, groupName, "将");

            L.debug("Begin email or sms invite = " + DateUtils.nowMillis());
            int identifySize = identifies.size();
            for (int i = 0; i < identifySize; i++) {
                final String identify = identifies.get(i);
                try {
                    Record r = p.inviteMember(groupId, identify, names.get(i), viewerId, message, ua, loc, appId);
                    r.put("key", identify);
                    recs.add(r);
                } catch (Exception ne) {
                    L.error("Fail to invite email or sms = " + identify, ne);
                }
            }
            L.debug("End email or sms invite = " + DateUtils.nowMillis());
        }

        //Borqs Staff
        boolean borqsStaff = qp.getBoolean("borqs_staff", false);
        if (borqsStaff) {
            addBorqsStaffsToGroup(p, groupId);
        }

        return recs;
    }

    @WebMethod("v2/public_circle/invite")
    public RecordSet publicCircleInvite(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return groupInvite(groupId, qp, req);
    }

    private RecordSet groupApprove(long groupId, QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userIds = qp.checkGetString("user_ids");
        List<String> users = StringUtils2.splitList(userIds, ",", true);
        int size = users.size();

        RecordSet recs = new RecordSet();
        for (int i = 0; i < size; i++) {
            Record r = p.approveMember(groupId, viewerId, users.get(i));
            r.put("key", users.get(i));
            recs.add(r);
        }
        return recs;
    }

    @WebMethod("v2/public_circle/approve")
    public RecordSet publicCircleApprove(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return groupApprove(groupId, qp, req);
    }

    private RecordSet groupIgnore(long groupId, QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userIds = qp.checkGetString("user_ids");
        List<String> users = StringUtils2.splitList(userIds, ",", true);
        int size = users.size();

        RecordSet recs = new RecordSet();
        for (int i = 0; i < size; i++) {
            Record r = p.ignoreMember(groupId, viewerId, users.get(i));
            r.put("key", users.get(i));
            recs.add(r);
        }
        return recs;
    }

    @WebMethod("v2/public_circle/ignore")
    public RecordSet publicCircleIgnore(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return groupIgnore(groupId, qp, req);
    }

    @WebMethod("v2/public_circle/join")
    public int publicCircleJoin(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        int status = p.addMember(groupId, viewerId, qp.getString("message", ""), ua, loc, appId, qp.getBoolean("send_post", true));
        if (status == Constants.STATUS_NONE)
            throw new BaseException(ErrorCode.GROUP_ERROR, "The public circle can not apply to join!");
        return status;
    }

    @WebMethod("v2/group/deal_invite")
    public NoResponse dealPublicCircleInvite(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws AvroRemoteException, UnsupportedEncodingException, IOException {
        Platform p = platform();
        String userId = qp.getString("user_id", "0");
        String source = qp.checkGetString("source");
        String ua = getDecodeHeader(req, "User-Agent", "", userId);
        String loc = getDecodeHeader(req, "location", "", userId);
        long groupId = qp.checkGetInt("group_id");
        boolean accept = qp.getBoolean("accept", false);

        int status;
        if (!StringUtils.equals(userId, "0")) {
            status = p.dealGroupInvite(groupId, userId, source, accept, ua, loc);
        } else {
            String name = qp.checkGetString("name");
            String identify = qp.checkGetString("identify");
            status = p.rejectGroupInviteForIdentify(groupId, name, identify, source);
        }

        String notice = "操作";
        if (accept && (status == GroupConstants.STATUS_JOINED)) {
            notice += "成功！";
        } else if (!accept && (status == GroupConstants.STATUS_REJECTED)) {
            notice += "成功！";
        } else {
            notice += "失败！";
        }

        String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });

        resp.setContentType("text/html");
        resp.getWriter().print(html);

        return NoResponse.get();
    }

    @WebMethod("v2/group/invite_page")
    public DirectResponse groupInvitePage(QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {

        String displayName = URLDecoder.decode(qp.checkGetString("display_name"), "utf-8");
        String fromName = URLDecoder.decode(qp.checkGetString("from_name"), "utf-8");
        String register = URLDecoder.decode(qp.getString("register", ""), "utf-8");
        String groupType = URLDecoder.decode(qp.checkGetString("group_type"), "utf-8");
        String groupName = URLDecoder.decode(qp.checkGetString("group_name"), "utf-8");
        String message = URLDecoder.decode(qp.getString("message", ""), "utf-8");
        String acceptUrl = URLDecoder.decode(qp.checkGetString("accept_url"), "utf-8");
        String rejectUrl = URLDecoder.decode(qp.checkGetString("reject_url"), "utf-8");

        String html = pageTemplate.merge("group_invite.ftl", new Object[][]{
                {"host", serverHost},
                {"displayName", displayName},
                {"fromName", fromName},
                {"register", register},
                {"groupType", groupType},
                {"groupName", groupName},
                {"message", message},
                {"acceptUrl", acceptUrl},
                {"rejectUrl", rejectUrl}
        });

        return DirectResponse.of("text/html", html);
    }

    @WebMethod("v2/public_circle/remove")
    public boolean removeMembersFromPublicCircle(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        String members = qp.checkGetString("members");
        String admins = qp.getString("admins", "");
        return p.removeMembers(viewerId, groupId, members, admins);
    }

    private RecordSet searchGroups(long begin, long end, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "0";
        String ticket = qp.getString("ticket", "");
        if (StringUtils.isNotBlank(ticket))
            viewerId = p.checkTicket(qp);

        String name = qp.checkGetString("name");
        String cols = qp.getString("columns", GroupConstants.GROUP_LIGHT_COLS);
        RecordSet recs = p.searchGroups(begin, end, name, viewerId, cols);
        recs.renameColumn(GRP_COL_ID, "circle_id");
        recs.renameColumn(GRP_COL_NAME, "circle_name");

        return recs;
    }

    @WebMethod("v2/public_circle/search")
    public RecordSet searchPublicCircles(QueryParams qp) throws AvroRemoteException {
        return searchGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.ACTIVITY_ID_BEGIN, qp);
    }

    private boolean groupGrant(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String admins = qp.getString("admins", "");
        String members = qp.getString("members", "");

        if (StringUtils.isBlank(admins) && StringUtils.isBlank(members))
            throw new BaseException(ErrorCode.PARAM_ERROR, "Must have parameter 'admins' or 'members'");

        Record roles = new Record();
        if (StringUtils.isNotBlank(admins)) {
            long[] adminArr = StringUtils2.splitIntArray(admins, ",");
            for (long admin : adminArr) {
                roles.put(String.valueOf(admin), GroupConstants.ROLE_ADMIN);
            }
        }
        if (StringUtils.isNotBlank(members)) {
            long[] memberArr = StringUtils2.splitIntArray(members, ",");
            for (long member : memberArr) {
                roles.put(String.valueOf(member), GroupConstants.ROLE_MEMBER);
            }
        }

        return p.grants(viewerId, groupId, roles);
    }

    @WebMethod("v2/public_circle/grant")
    public boolean publicCircleGrant(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return groupGrant(groupId, qp);
    }

    private boolean updateMemberNotification(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        String[] buildInParams = new String[]{"sign_method", "sign", "appid", "ticket", "circle_id", "group_id"};
        String[] notifParams = new String[]{"recv_notif", "notif_email", "notif_phone"};
        Record notif = new Record();
        for (Map.Entry<String, Object> entry : qp.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!ArrayUtils.contains(buildInParams, key)
                    && ArrayUtils.contains(notifParams, key)) {
                notif.put(key, value);
            }
        }

        return p.updateMemberNotification(groupId, viewerId, notif);
    }

    @WebMethod("v2/public_circle/update_notif")
    public boolean updateCircleMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return updateMemberNotification(groupId, qp);
    }

    private Record getMemberNotification(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        RecordSet recs = p.getMembersNotification(groupId, viewerId);
        if (recs.isEmpty())
            throw new GroupException("The user is not a member");
        else
            return recs.get(0);
    }

    @WebMethod("v2/public_circle/get_notif")
    public Record getCircleMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return getMemberNotification(groupId, qp);
    }

    private boolean defaultMemberNotification(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userIds = qp.getString("users", viewerId);
        return p.defaultMemberNotification(groupId, userIds);
    }

    @WebMethod("v2/public_circle/def_notif")
    public boolean defaultCircleMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return defaultMemberNotification(groupId, qp);
    }

    private String groupTopPostsSet(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        RecordSet recs = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.GROUP_ID_END, String.valueOf(groupId), GroupConstants.COMM_COL_TOP_POSTS);
        String oldPostIds = recs.get(0).getString(GroupConstants.COMM_COL_TOP_POSTS);
        Set<String> postIds = StringUtils2.splitSet(oldPostIds, ",", true);
        String set = qp.getString("set", "");
        String unset = qp.getString("unset", "");
        Set<String> sl = StringUtils2.splitSet(set, ",", true);
        Set<String> ul = StringUtils2.splitSet(unset, ",", true);
        postIds.addAll(sl);
        postIds.removeAll(ul);
        String topPosts = StringUtils2.joinIgnoreBlank(",", postIds);

        Record info = new Record();
        Record properties = new Record();
        properties.put(GroupConstants.COMM_COL_TOP_POSTS, topPosts);
        boolean result = p.updateGroup(viewerId, groupId, info, properties);
        if (result)
            return topPosts;
        else
            return oldPostIds;
    }

    private String accountTopPostsSet(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        RecordSet recs = p.getUsers(viewerId, viewerId, "top_posts", false, false);
        String oldPostIds = recs.get(0).getString("top_posts");
        Set<String> postIds = StringUtils2.splitSet(oldPostIds, ",", true);
        String set = qp.getString("set", "");
        String unset = qp.getString("unset", "");
        Set<String> sl = StringUtils2.splitSet(set, ",", true);
        Set<String> ul = StringUtils2.splitSet(unset, ",", true);
        postIds.addAll(sl);
        postIds.removeAll(ul);
        String topPosts = StringUtils2.joinIgnoreBlank(",", postIds);

        boolean result = p.updateAccount(viewerId, Record.of("top_posts", topPosts));
        if (result)
            return topPosts;
        else
            return oldPostIds;
    }

    @WebMethod("post/top_posts_set")
    public String circleTopPostsSet(QueryParams qp) throws AvroRemoteException {
        long id = qp.checkGetInt("id");
        if (id >= GroupConstants.PUBLIC_CIRCLE_ID_BEGIN && id <= GroupConstants.GROUP_ID_END)
            return groupTopPostsSet(id, qp);
        else
            return accountTopPostsSet(qp);
    }

    private RecordSet groupTopPostsGet(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        RecordSet recs = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.GROUP_ID_END, String.valueOf(groupId), GroupConstants.COMM_COL_TOP_POSTS);
        String postIds = recs.get(0).getString(GroupConstants.COMM_COL_TOP_POSTS);
        boolean single_get = true;
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return p.getFullPostsForQiuPu(viewerId, postIds, single_get);
        } else {
            return p.getPostsForQiuPu(viewerId, postIds, qp.checkGetString("cols"), single_get);
        }
    }

    private RecordSet accountTopPostsGet(String userId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        RecordSet recs = p.getUsers(userId, userId, "top_posts", false, false);
        String postIds = recs.get(0).getString("top_posts");
        boolean single_get = true;
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            return p.getFullPostsForQiuPu(viewerId, postIds, single_get);
        } else {
            return p.getPostsForQiuPu(viewerId, postIds, qp.checkGetString("cols"), single_get);
        }
    }

    @WebMethod("post/top_posts_get")
    public RecordSet circleTopPostsGet(QueryParams qp) throws AvroRemoteException {
        long id = qp.checkGetInt("id");
        if (id >= GroupConstants.PUBLIC_CIRCLE_ID_BEGIN && id <= GroupConstants.GROUP_ID_END)
            return groupTopPostsGet(id, qp);
        else
            return accountTopPostsGet(String.valueOf(id), qp);
    }

    @WebMethod("v2/activity/create")
    public Record createActivity(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        qp.put("need_invited_confirm", 0);
        qp.put("privacy", PRIVACY_SECRET);

        return createGroup(GroupConstants.ACTIVITY_ID_BEGIN, GroupConstants.TYPE_ACTIVITY, qp, req, p, viewerId);
    }

    @WebMethod("v2/activity/show")
    public RecordSet getActivities(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String groupIds = getParamString(qp, "activity_ids", "ids", false, "");
        return getGroups(GroupConstants.ACTIVITY_ID_BEGIN, GroupConstants.DEPARTMENT_ID_BEGIN, groupIds, qp, p, viewerId);
    }

    @WebMethod("v2/activity/search")
    public RecordSet searchAcitivities(QueryParams qp) throws AvroRemoteException {
        return searchGroups(GroupConstants.ACTIVITY_ID_BEGIN, GroupConstants.DEPARTMENT_ID_BEGIN, qp);
    }

    @WebMethod("v2/activity/add_remove")
    public boolean addOrRemoveActivityMembers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        long groupId = getParamInt(qp, "activity_id", "id", true, 0);
        if (groupId >= GroupConstants.ACTIVITY_ID_BEGIN && groupId < GroupConstants.DEPARTMENT_ID_BEGIN) {
            String add = qp.getString("add", "");
            if (StringUtils.isNotBlank(add)) {
                Set<String> borqsIds = StringUtils2.splitSet(add, ",", true);
                Record membersRec = new Record();
                for (String borqsId : borqsIds)
                    membersRec.put(borqsId, ROLE_MEMBER);

                List<String> excludes = StringUtils2.splitList(p.getCreatorAndAdmins(groupId), ",", true);
                for (String exclude : excludes)
                    membersRec.remove(exclude);
                if (!membersRec.isEmpty()) {
                    p.addMembers(groupId, membersRec, false);
                }
            }

            String remove = qp.getString("remove", "");
            String admins = qp.getString("admins", "");
            if (StringUtils.isNotBlank(remove)) {
                p.removeMembers(viewerId, groupId, remove, admins);
            }

            return true;
        } else {
            return false;
        }
    }

    @WebMethod("v2/activity/add")
    public boolean addActivitiesMembers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        String groupIds = getParamString(qp, "activity_ids", "ids", true, "");
        if (StringUtils.isNotBlank(groupIds)) {
            Set<String> groups = StringUtils2.splitSet(groupIds, ",", true);
            for (String groupId : groups) {
                long id = Long.parseLong(groupId);
                if (id >= GroupConstants.ACTIVITY_ID_BEGIN && id < GroupConstants.DEPARTMENT_ID_BEGIN) {
                    String add = qp.checkGetString(groupId);
                    Set<String> borqsIds = StringUtils2.splitSet(add, ",", true);
                    Record membersRec = new Record();
                    for (String borqsId : borqsIds)
                        membersRec.put(borqsId, ROLE_MEMBER);

                    List<String> excludes = StringUtils2.splitList(p.getCreatorAndAdmins(id), ",", true);
                    for (String exclude : excludes)
                        membersRec.remove(exclude);
                    if (!membersRec.isEmpty()) {
                        p.addMembers(id, membersRec, false);
                    }
                }
            }
        }

        return true;
    }

    @WebMethod("v2/group/create")
    public Record createGeneralGroup(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String type = qp.getString("type", "group");
        long begin = p.getBeginByGroupType(type);
        return createGroup(begin, type, qp, req, p, viewerId);
    }

    @WebMethod("v2/group/update")
    public boolean updateGeneralGroup(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);

        return updateGroup(groupId, qp);
    }

    @WebMethod("v2/group/upload_profile_image")
    public boolean uploadGeneralGroupProfileImage(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return uploadGroupProfileImage(groupId, qp, req);
    }

    @WebMethod("v2/group/destroy")
    public boolean destroyGeneralGroup(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        String groupIds = getParamString(qp, "group_ids", "ids", true, "");
        return p.destroyGroup(viewerId, groupIds);
    }

    @WebMethod("v2/group/show")
    public RecordSet getGeneralGroups(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String groupIds = getParamString(qp, "group_ids", "ids", false, "");
        String type = qp.getString("type", "group");
        long begin = p.getBeginByGroupType(type);
        long end = p.getEndByGroupType(type);
        return getGroups(begin, end, groupIds, qp, p, viewerId);
    }

    @WebMethod("v2/group/detail")
    public Record getGroupDetail(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String groupId = getParamString(qp, "group_id", "id", false, "");
        String type = qp.getString("type", "group");
        long begin = p.getBeginByGroupType(type);
        long end = p.getEndByGroupType(type);
        RecordSet recs = getGroups(begin, end, groupId, qp, p, viewerId);
        if (recs.isEmpty()) {
            throw new PlatformException(ErrorCode.GROUP_NOT_EXISTS, "The group is not exist");
        } else {
            return recs.get(0);
        }
    }

    @WebMethod("v2/group/users")
    public RecordSet getGeneralGroupUsers(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return getGroupUsers(groupId, qp);
    }

    @WebMethod("v2/group/invite")
    public RecordSet generalGroupInvite(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return groupInvite(groupId, qp, req);
    }

    @WebMethod("v2/group/approve")
    public RecordSet generalGroupApprove(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return groupApprove(groupId, qp, req);
    }

    @WebMethod("v2/group/ignore")
    public RecordSet generalGroupIgnore(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return groupIgnore(groupId, qp, req);
    }

    @WebMethod("v2/group/join")
    public int generalGroupJoin(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        int status = p.addMember(groupId, viewerId, qp.getString("message", ""), ua, loc, appId, qp.getBoolean("send_post", true));
        if (status == Constants.STATUS_NONE)
            throw new BaseException(ErrorCode.GROUP_ERROR, "The group can not apply to join!");
        return status;
    }

    @WebMethod("v2/group/remove")
    public boolean removeMembersFromGeneralGroup(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        String members = qp.checkGetString("members");
        String admins = qp.getString("admins", "");
        return p.removeMembers(viewerId, groupId, members, admins);
    }

    @WebMethod("v2/group/search")
    public RecordSet searchGeneralGroups(QueryParams qp) throws AvroRemoteException {
        String type = qp.getString("type", "group");
        Platform p = platform();
        long begin = p.getBeginByGroupType(type);
        long end = p.getEndByGroupType(type);
        return searchGroups(begin, end, qp);
    }

    @WebMethod("v2/group/grant")
    public boolean generalGroupGrant(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return groupGrant(groupId, qp);
    }

    @WebMethod("v2/group/update_notif")
    public boolean updateGroupMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return updateMemberNotification(groupId, qp);
    }

    @WebMethod("v2/group/get_notif")
    public Record getGroupMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return getMemberNotification(groupId, qp);
    }

    @WebMethod("v2/group/def_notif")
    public boolean defaultGroupMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return defaultMemberNotification(groupId, qp);
    }


    //-----------   event  -----------
    @WebMethod("v2/event/create")
    public Record createEvent(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        qp.checkGetInt("start_time");
        qp.checkGetInt("end_time");
        return createGroup(GroupConstants.EVENT_ID_BEGIN, GroupConstants.TYPE_EVENT, qp, req, p, viewerId);
    }

    @WebMethod("v2/event/update")
    public boolean updateEvent(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);

        return updateGroup(groupId, qp);
    }

    @WebMethod("v2/event/upload_profile_image")
    public boolean uploadEventProfileImage(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return uploadGroupProfileImage(groupId, qp, req);
    }

    @WebMethod("v2/event/destroy")
    public boolean destroyEvent(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        String groupIds = getParamString(qp, "event_ids", "ids", true, "");
        return p.destroyGroup(viewerId, groupIds);
    }

    @WebMethod("v2/event/show")
    public RecordSet getEvents(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String cols = qp.getString("columns", "");
        String columns = StringUtils.isBlank(cols) ? "start_time,end_time" : cols + ",start_time,end_time";
        qp.put("columns", columns);
        String groupIds = getParamString(qp, "event_ids", "ids", false, "");
        long beginTime = DateUtils.nowMillis();
        RecordSet recs = getGroups(GroupConstants.EVENT_ID_BEGIN, GroupConstants.EVENT_ID_END, groupIds, qp, p, viewerId);
        long endTime = DateUtils.nowMillis();
        long totalTime = endTime - beginTime;
        L.trace("get event spend: " + totalTime);
        return recs;
    }

    @WebMethod("v2/event/detail")
    public Record getEventDetail(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String cols = qp.getString("columns", GroupConstants.GROUP_LIGHT_COLS);
        qp.put("columns", cols + ",start_time,end_time");
        String groupId = getParamString(qp, "event_id", "id", false, "");
        RecordSet recs = getGroups(GroupConstants.EVENT_ID_BEGIN, GroupConstants.EVENT_ID_END, groupId, qp, p, viewerId);
        if (recs.isEmpty()) {
            throw new PlatformException(ErrorCode.GROUP_NOT_EXISTS, "The event is not exist");
        } else {
            return recs.get(0);
        }
    }

    @WebMethod("v2/event/users")
    public RecordSet getEventUsers(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return getGroupUsers(groupId, qp);
    }

    @WebMethod("v2/event/invite")
    public RecordSet eventInvite(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return groupInvite(groupId, qp, req);
    }

    @WebMethod("v2/event/approve")
    public RecordSet eventApprove(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return groupApprove(groupId, qp, req);
    }

    @WebMethod("v2/event/ignore")
    public RecordSet eventIgnore(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return groupIgnore(groupId, qp, req);
    }

    @WebMethod("v2/event/join")
    public int eventJoin(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        int status = p.addMember(groupId, viewerId, qp.getString("message", ""), ua, loc, appId, qp.getBoolean("send_post", true));
        if (status == Constants.STATUS_NONE)
            throw new BaseException(ErrorCode.GROUP_ERROR, "The event can not apply to join!");
        return status;
    }

    @WebMethod("v2/event/remove")
    public boolean removeMembersFromEvent(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        String members = qp.checkGetString("members");
        String admins = qp.getString("admins", "");
        return p.removeMembers(viewerId, groupId, members, admins);
    }

    @WebMethod("v2/event/search")
    public RecordSet searchEvents(QueryParams qp) throws AvroRemoteException {
        String cols = qp.getString("columns", GroupConstants.GROUP_LIGHT_COLS);
        qp.put("columns", cols + ",start_time,end_time");
        Platform p = platform();
        return searchGroups(GroupConstants.EVENT_ID_BEGIN, GroupConstants.EVENT_ID_END, qp);
    }

    @WebMethod("v2/event/grant")
    public boolean eventGrant(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return groupGrant(groupId, qp);
    }

    @WebMethod("v2/event/update_notif")
    public boolean updateEventMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return updateMemberNotification(groupId, qp);
    }

    @WebMethod("v2/event/get_notif")
    public Record getEventMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return getMemberNotification(groupId, qp);
    }

    @WebMethod("v2/event/def_notif")
    public boolean defaultEventMemberNotification(QueryParams qp) throws AvroRemoteException {
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return defaultMemberNotification(groupId, qp);
    }


    // theme
    @WebMethod("v2/event/themes")
    public RecordSet getEventThemes(QueryParams qp) {
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        Platform p = platform();
        return p.getEventThemes(page, count);
    }

    @WebMethod("v2/event/upload_theme")
    public Record updateTheme(QueryParams qp) throws AvroRemoteException {
        // TODO: upload image
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String name = qp.checkGetString("name");
        String image = qp.checkGetString("image");
        long now = DateUtils.nowMillis();
        return p.addEventTheme(RandomUtils.generateId(now), Long.parseLong(viewerId), now, name, image);
    }

    // poll
    @WebMethod("poll/create")
    public Record createPoll(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        String mentions = qp.getString("target", "");
        List<String> groupIds = new ArrayList<String>();
        StringBuilder changeMentions = new StringBuilder();
        if (getUserAndGroup(changeMentions, p, mentions, groupIds, viewerId)) {
            mentions = changeMentions.toString();
            String ids = p.parseUserIds(viewerId, mentions);
            List<String> l = StringUtils2.splitList(ids, ",", true);
            if (l.size() > MAX_GUSY_SHARE_TO)
                throw new BaseException(ErrorCode.PARAM_ERROR, "Only can share to less than 400 guys!");
        }

        String title = qp.checkGetString("title");
        String description = qp.getString("description", "");
        long multi = qp.getInt("multi", 1);
        long limit = qp.getInt("limit", 0);
        long privacy = qp.getInt("privacy", 0);
        long anonymous = qp.getInt("anonymous", 0);
        long mode = qp.getInt("mode", 0); // 0 - can not change vote   1 - can append vote  2- can change vote
        long now = DateUtils.nowMillis();
        long startTime = qp.getInt("start_time", now);
        long endTime = qp.getInt("end_time", 0);
        boolean sendPost = qp.getBoolean("send_post", true);

        long pollId = RandomUtils.generateId(now);
        Record poll = new Record();
        poll.put("id", pollId);
        poll.put("source", viewerId);
        poll.put("target", mentions);
        poll.put("title", title);
        poll.put("description", description);
        poll.put("multi", multi);
        poll.put("limit_", limit);
        poll.put("privacy", privacy);
        poll.put("anonymous", anonymous);
        poll.put("mode", mode);
        poll.put("type", TEXT_POST);
        poll.put("attachments", JsonNodeFactory.instance.arrayNode());
        poll.put("created_time", now);
        poll.put("start_time", startTime);
        poll.put("end_time", endTime);
        poll.put("updated_time", now);
        poll.put("destroyed_time", 0);

        RecordSet items = new RecordSet();
        Set<String> set = qp.keySet();
        for (String key : set) {
            if (StringUtils.startsWith(key, "message")) {
                Record item = new Record();
                item.put("poll_id", pollId);
                item.put("item_id", RandomUtils.generateId());
                item.put("type", TEXT_POST);
                item.put("message", qp.checkGetString(key));
                item.put("attachments", JsonNodeFactory.instance.arrayNode());
                item.put("created_time", now);
                item.put("updated_time", now);
                item.put("destroyed_time", 0);
                items.add(item);
            }
        }

        pollId = p.createPoll(poll, items, ua, loc, appId, sendPost);
        return p.getPolls(viewerId, String.valueOf(pollId), true).getFirstRecord();
    }

    @WebMethod("poll/vote")
    public Record vote(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);

        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        long pollId = qp.checkGetInt("poll_id");

        if (!p.canVote(viewerId, pollId))
            throw new PollException("You can not vote this poll");

        boolean sendPost = qp.getBoolean("send_post", true);
        String itemIds = qp.checkGetString("item_ids");
        String weights = qp.getString("weights", "");
        List<String> lItemIds = StringUtils2.splitList(itemIds, ",", true);
        List<Long> lWeights = StringUtils2.splitIntList(weights, ",");
        Record items = new Record();
        long voteCount = 0;
        int size = lItemIds.size();
        for (int i = 0; i < size; i++) {
            if (StringUtils.isBlank(weights)) {
                items.put(lItemIds.get(i), 1L);
                voteCount++;
            } else {
                long weight = lWeights.get(i);
                items.put(lItemIds.get(i), weight);
                voteCount += weight;
            }
        }

        Record poll = p.getPolls(viewerId, String.valueOf(pollId), false).getFirstRecord();
        long multi = poll.getInt("multi");
        if (voteCount > multi)
            throw new PollException("You can only vote " + multi + " items");

        p.vote(viewerId, pollId, items, ua, loc, appId, sendPost);
        return p.getPolls(viewerId, String.valueOf(pollId), true).getFirstRecord();
    }

    @WebMethod("poll/get")
    public RecordSet getPolls(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String pollIds = qp.checkGetString("ids");
        boolean withItems = qp.getBoolean("with_items", false);
        return p.getPolls(viewerId, pollIds, withItems);
    }

    @WebMethod("poll/detail")
    public Record getPoll(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String pollIds = qp.checkGetString("id");
        boolean withItems = qp.getBoolean("with_items", false);
        RecordSet recs = p.getPolls(viewerId, pollIds, withItems);
        if (recs.isEmpty()) {
            throw new PlatformException(ErrorCode.POLL_NOT_EXISTS, "The poll is not exists");
        }
        else
            return recs.get(0);
    }

    @WebMethod("poll/list/user")
    public RecordSet getUserPolls(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.getString("user_id", viewerId);
        int type = (int) qp.getInt("type", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);

        if (type == 0)
            return p.getCreatedPolls(viewerId, userId, page, count);
        else if (type == 1)
            return p.getParticipatedPolls(viewerId, userId, page, count);
        else
            return p.getInvolvedPolls(viewerId, userId, page, count);
    }

    @WebMethod("poll/list/friends")
    public RecordSet getFriendsPolls(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.getString("user_id", viewerId);
        int sort = (int) qp.getInt("sort", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        return p.getFriendsPolls(viewerId, userId, sort, page, count);
    }

    @WebMethod("poll/list/public")
    public RecordSet getPublicPolls(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String userId = qp.getString("user_id", viewerId);
        int sort = (int) qp.getInt("sort", 0);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        return p.getPublicPolls(viewerId, userId, sort, page, count);
    }

    @WebMethod("poll/destroy")
    public boolean destroyPolls(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String pollIds = qp.checkGetString("ids");
        return p.destroyPolls(viewerId, pollIds);
    }

    // apply api
    @WebMethod("post/apply")
    public Record apply(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);
        FileItem fi = qp.getFile("file");
        String app_data = qp.getString("app_data", "");
        String msg = qp.getString("msg", "");
        String appId = qp.checkGetString("appid");

        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String post_id = "";

        String add_to = "";
        String mentions = qp.checkGetString("to");

        List<String> fids = new ArrayList<String>();
        String tmp_ids = "";

        Record appData = new Record();
        try {
            appData = Record.fromJson(app_data);
        } catch (Exception ignored) {
        }

        if (mentions.length() <= 0)
            throw new BaseException(ErrorCode.PARAM_ERROR, "want mentions!");

        if (StringUtils.isBlank(mentions))
            throw new BaseException(ErrorCode.GROUP_ERROR, "You don't have right to post!");

        String share_file_id = qp.getString("file_id", "");
        if (fi != null && StringUtils.isNotEmpty(fi.getName()) && share_file_id.equals("")) {
            FileItem screen_shot = qp.getFile("screen_shot");
            String summary = qp.getString("summary", "");
            String description = qp.getString("description", "");
            String content_type = qp.getString("content_type", "");
            String file_name = qp.getString("file_name", "");

            String folder_id = folder.getFolder(viewerId, folder.FOLDER_TYPE_SHARE_OUT, "Sharing Files");
            if (!folder.isFolderExist(folder_id)) {
                throw new PhotoException("folder not exist, please create folder first");
            }

            String file_id = Long.toString(RandomUtils.generateId());

            Record static_file = uploadFile(p, viewerId, file_id, Long.parseLong(folder_id), fi, summary, description, content_type, screen_shot, file_name);
            fids.add(file_id);


            Record rec = formatFileBucketUrl(viewerId, static_file);
            String fileUrl = rec.getString("file_url");

            int type = Constants.FILE_POST;
            if (rec.getString("content_type").contains("video/")) {
                type = Constants.VIDEO_POST;
            } else if (rec.getString("content_type").contains("audio/")) {
                type = Constants.AUDIO_POST;
            }
            type |= Constants.APPLY_POST;

            List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= 10) {
                        try {
                            String other_folder_id = folder.getFolder(uid, folder.FOLDER_TYPE_RECEIVED, "Received Files");
                            if (static_file.has("file_url"))
                                static_file.removeColumns("file_url");
                            if (static_file.has("thumbnail_url"))
                                static_file.removeColumns("thumbnail_url");
                            if (static_file.has("likes"))
                                static_file.removeColumns("likes");
                            if (static_file.has("comments"))
                                static_file.removeColumns("comments");

                            static_file.put("folder_id", other_folder_id);
                            static_file.put("user_id", uid);
                            folder.saveStaticFile(static_file);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            post_id = p.post(viewerId, type, msg, formatFileBucketUrlForStream(viewerId, rec).toString(false, false), appId,
                    "", "", app_data, mentions, true, "", ua, loc, "", "", true, true, false, add_to);

            if (fids.size() > 0 && !post_id.equals("")) {
                folder.updateStaticFileStreamId(post_id, fids);
            }

            appData.set("file_url", fileUrl);
        } else {
            post_id = p.post(viewerId, Constants.TEXT_POST | Constants.APPLY_POST, msg, "[]", appId,
                    "", "", app_data, mentions, true, "", ua, loc, "", "", true, true, false, add_to);
            appData.set("file_url", "");
        }

        if ("10001".equals(appId)) { // innov
            RecordSet userInfos = getUserInfos(p, mentions);
            postApply(p, viewerId, userInfos, appData);
        }
        return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
    }

    private void postApply(Platform p, String viewerId, RecordSet userInfos, Record appData) throws AvroRemoteException, UnsupportedEncodingException {
        // send email
        for (Record userInfo : userInfos) {
            String email = userInfo.getString("email");
            if (StringUtils.isNotEmpty(email)) {
                String userId = userInfo.getString("user_id");
                String name = userInfo.getString("name");
                String department = appData.getString("department");
                String memberIds = appData.getString("members");
                String memberNames = appData.getString("member_names");
                String product = appData.getString("product");
                String fileUrl = appData.getString("file_url");
                if (memberNames.isEmpty() && !memberIds.isEmpty()) {
                    memberNames = getInnopMemberNames(p, memberIds);
                }

                Record mailParams = new Record();
                // String.format("http://%s/innov/stat?appid=10001&sign_method=md5&sign=LC0Qrhwsgnrn4l%2Fhm3D2rA%3D%3D&key=%s", serverHost, )
                String statUrl = "http://" + serverHost
                        + "/innov/stat?appid=10001&sign_method=md5&sign=LC0Qrhwsgnrn4l%2Fhm3D2rA%3D%3D&key="
                        + URLEncoder.encode(Encoders.desEncryptBase64(userId), "UTF-8");
                Encoders.desEncryptBase64(viewerId);
                mailParams.set("stat_url", statUrl);
                Record sender = getUserInfo(p, viewerId);
                mailParams.set("replyName", sender.getString("name"));
                mailParams.set("replyEmail", sender.getString("email"));
                mailParams.set("uname", name);
                mailParams.set("email", email);
                mailParams.set("product", product);
                mailParams.set("department", department);
                mailParams.set("memberNames", memberNames);
                mailParams.set("fileUrl", fileUrl);
                p.sendInnovEmail("Participate Borqs Innovation Competition - " + sender.getString("name"), email, mailParams, "zh");
            }
        }
    }

    @WebMethod("post/applies_to_me")
    public RecordSet getAppliesToMe(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String appId = qp.checkGetString("appid");
        return p.getAppliesToUser(viewerId, appId, viewerId, qp.getString("cols", Platform.POST_FULL_COLUMNS));
    }

    @WebMethod("innov/gen_excel")
    public NoResponse getExcel(QueryParams qp, HttpServletResponse resp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException, IOException {
        //String s = WebSignatures.md5Sign("thO;deVA50", Arrays.asList("key"));
        //String s1 = Encoders.desEncryptBase64("41");
        Platform p = platform();
        String key = qp.checkGetString("key");
        String viewerId = Encoders.desDecryptFromBase64(key);
        String appId = qp.checkGetString("appid");
        RecordSet statRecs = innopStat(p, viewerId, appId);

        InnovExcel ie = new InnovExcel();
        List<List<String>> dataList = new ArrayList<List<String>>();
        //dataList.add(Arrays.asList("报名者", "部门", "项目名称", "其他参与人员", "报名日期"));
        int n = 1;
        for (Record statRec : statRecs) {
            // // applicant, department, product, members_names, date, file_url
            dataList.add(Arrays.asList(
                    Integer.toString(n++),
                    statRec.getString("applicant"),
                    statRec.getString("department"),
                    statRec.getString("product"),
                    statRec.getString("member_names"),
                    statRec.getString("date")));
        }


        byte[] buff = ie.genInnovSignUpExcel(dataList);
        if (buff != null) {
            resp.setContentType("application/vnd.ms-excel");
            resp.setHeader("Content-Disposition", "attachment; filename=\"InnovSummary" + DateUtils.formatDate(DateUtils.nowMillis()) + ".xls\"");
            IOUtils.copy(new ByteArrayInputStream(buff), resp.getOutputStream());
        } else {
            resp.setStatus(404);
        }
        return NoResponse.get();
    }

    @WebMethod("post/applies_by_me")
    public RecordSet getAppliesByMe(QueryParams qp) throws AvroRemoteException {
        final int POST_COUNT = 100000;
        Platform p = platform();
        String viewerId = p.checkSignAndTicket(qp);
        String appId = qp.checkGetString("appid");
        long max = DateUtils.nowMillis();
        RecordSet recs;
        if (qp.getString("cols", "").isEmpty() || qp.getString("cols", "").equals("")) {
            recs = p.getMyShareFullTimeline(viewerId, viewerId, 0, max, (int) qp.getInt("type", APPLY_POST), appId, 0, POST_COUNT);
        } else {
            recs = p.getMyShareTimeline(viewerId, viewerId, qp.checkGetString("cols"), 0, max, (int) qp.getInt("type", APPLY_POST), appId, 0, POST_COUNT);
        }
        // TODO: optimize
        RecordSet recs2 = new RecordSet();
        for (Record rec : recs) {
            if (rec != null && rec.getString("app").equals(appId))
                recs2.add(rec);
        }
        return recs2;
    }

    private RecordSet innopStat(Platform p, String viewerId, String appId) throws AvroRemoteException {
        RecordSet posts = p.getAppliesToUser(viewerId, appId, viewerId, "post_id, message,type,created_time,app_data,source,attachments");
        RecordSet result = new RecordSet();
        for (Record post : posts) {
            Record rec = getInnopRecord(p, post);
            Record rec2 = findInnopProduct(result, rec.getString("applicant"), rec.getString("product"));
            if (rec2 == null) {
                result.add(rec);
            } else {
                rec2.put("product", rec.getString("product"));
                rec2.set("department", rec.getString("department"));
                rec2.set("member_names", rec.getString("member_names"));
                rec2.set("date", rec.getString("date"));
                rec2.set("file_url", rec.getString("file_url"));
            }
        }
        RecordSet res2 = new RecordSet();
        boolean hasNonEmpty = false;
        for (Record rec : result) {
            if (!rec.getString("product").isEmpty()) {
                hasNonEmpty = true;
                break;
            }
        }
        if (hasNonEmpty) {
            for (Record rec : result) {
                if (!rec.getString("product").isEmpty())
                    res2.add(rec);
            }
        } else {
            res2.addAll(result);
        }
        return res2;
    }

    //@WebMethod("post/mail_test")
    public void mailTest(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        Record mailParams = new Record();
        // String.format("http://%s/innov/stat?appid=10001&sign_method=md5&sign=LC0Qrhwsgnrn4l%2Fhm3D2rA%3D%3D&key=%s", serverHost, Encoders.desEncryptBase64(viewerId))
        String statUrl = "http://" + serverHost + "/innov/stat?appid=10001&sign_method=md5&sign=LC0Qrhwsgnrn4l%2Fhm3D2rA%3D%3D&key=" + Encoders.desEncryptBase64("5");
        mailParams.set("stat_url", statUrl);
        Record sender = getUserInfo(p, "5");
        mailParams.set("replyName", sender.getString("name"));
        mailParams.set("replyEmail", sender.getString("email"));
        mailParams.set("uname", "wangpeng");
        mailParams.set("email", "peng.wang@borqs.com");

        mailParams.set("product", "car product");
        mailParams.set("department", "C&D");
        mailParams.set("memberNames", "A&B&C");

        mailParams.set("fileUrl", "http://bpc.borqs.com");
        //p.sendInnovEmail("Participate Borqs Innovation Competition - ", "peng.wang@borqs.com", mailParams, "zh");

        String html = pageTemplateInnov.merge("innov.ftl", mailParams);
        p.sendEmailEleaningHTML("Test case e-learning - ", "peng.wang@borqs.com", "", html, Constants.EMAIL_ESSENTIAL, "zh");
    }

    @WebMethod("innov/stat")
    public DirectResponse getInnopStat(QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        // return schema
        // applicant, department, product, members_names, date, file_url
        Platform p = platform();
        //String v = Encoders.desEncryptBase64("41");
        String key = qp.checkGetString("key");
        String viewerId = Encoders.desDecryptFromBase64(key);
        String appId = qp.checkGetString("appid");
        RecordSet statRecs = innopStat(p, viewerId, appId);
        String html = pageTemplate.merge("innovTableSignUp.ftl", new Object[][]{
                {"host", serverHost},
                {"rs", statRecs},
                {"downloadKey", URLEncoder.encode(Encoders.desEncryptBase64(viewerId), "UTF-8")},
                {"downloadSign", URLEncoder.encode(WebSignatures.md5Sign("thO;deVA50", Arrays.asList("key")), "UTF-8")}
        });
        return DirectResponse.of("text/html", html);
    }

    private static Record findInnopProduct(RecordSet recs, String applicant, String product) {
        for (Record rec : recs) {
            if (rec.checkGetString("applicant").equals(applicant) && (rec.getString("product").equals(product) || StringUtils.isBlank(rec.getString("product"))))
                return rec;
        }
        return null;
    }

    private static Record getInnopRecord(Platform p, Record post) throws AvroRemoteException {
        Record rec = new Record();
        rec.put("applicant", getInnopMemberNames(p, post.checkGetString("source")));
        rec.put("date", DateUtils.formatDateMinute(post.checkGetInt("created_time")));
        String appData = post.getString("app_data");
        try {
            JsonNode jn = JsonUtils.parse(appData);
            rec.put("product", ObjectUtils.toString(jn.path("product").getValueAsText()));
            rec.put("department", ObjectUtils.toString(jn.path("department").getValueAsText()));
            if (jn.has("member_names")) {
                rec.put("member_names", ObjectUtils.toString(jn.path("member_names").getValueAsText()));
            } else if (jn.has("members")) {
                rec.put("member_names", getInnopMemberNames(p, jn.path("members").getValueAsText()));
            }


            /*
           {"file_id":"2830637977035514670",
           "title":"NEWS.txt",
           "summary":"",
           "folder_id":"2830636378754258299",
           "description":"",
           "file_size":263050,
           "user_id":"14173",
           "exp_name":"txt",
           "html_url":"",
           "content_type":"text/plain",
           "new_file_name":"42_2830637977035514670.txt",
           "created_time":1349753371410,
           "updated_time":1349753371410,
           "destroyed_time":0,
           "file_url":"http://storage.aliyun.com/wutong-data/files/42/42_2830637977035514670.txt"
           }
            */
            String attachment = post.getString("attachments", "[]");
            JsonNode an = JsonUtils.parse(attachment);
            if (an.size() > 0) {
                JsonNode an0 = an.get(0);
                rec.put("file_url", ObjectUtils.toString(an0.path("file_url").getValueAsText()));
            } else {
                rec.put("file_url", "");
            }
        } catch (Exception ignored) {
        }
        return rec;
    }


    private static String getInnopMemberNames(Platform p, String memberIds) throws AvroRemoteException {
        RecordSet users = p.getUsers("", memberIds, "user_id, display_name", false);
        if (users.isEmpty()) {
            return "";
        }
        if (users.size() == 1) {
            return users.get(0).getString("display_name");
        } else {
            LinkedHashSet<String> names = new LinkedHashSet<String>();
            for (Record user : users) {
                names.add(user.getString("display_name"));
            }
            return StringUtils2.joinComma(names);
        }
    }

    private static RecordSet getUserInfos(Platform p, String userIds) throws AvroRemoteException {
        RecordSet users = p.getUsers("", userIds, "user_id, display_name, login_email1, login_email2, login_email3", false);
        if (users.isEmpty())
            return new RecordSet();

        HashMap<String, Record> m = new HashMap<String, Record>();
        for (Record user : users) {
            String userId = user.getString("user_id");
            String email1 = user.getString("login_email1");
            String email2 = user.getString("login_email2");
            String email3 = user.getString("login_email3");
            String name = user.getString("display_name");
            String email = "";
            if (StringUtils.endsWithIgnoreCase(email1, "@borqs.com"))
                email = email1;
            else if (StringUtils.endsWithIgnoreCase(email2, "@borqs.com"))
                email = email2;
            else if (StringUtils.endsWithIgnoreCase(email3, "@borqs.com"))
                email = email3;

            Record rec = m.get(userId);
            if (rec == null) {
                rec = new Record().set("name", name).set("email", email).set("user_id", userId);
                m.put(userId, rec);
            }
        }
        return new RecordSet(m.values());
    }

    @WebMethod("tag/create")
    public Record createTag(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String viewerId = p.checkSignAndTicket(qp);
        String tag = qp.checkGetString("tag");
        String type = qp.checkGetString("type");
        String taget_id = qp.checkGetString("target_id");
        Record record = Record.of("user", viewerId, "tag", tag, "type", type, "target_id", taget_id, "created_time", DateUtils.nowMillis());

        return p.createTag(record);
    }

    @WebMethod("tag/destroyed")
    public boolean destroyedTag(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String viewerId = p.checkSignAndTicket(qp);
        String tag = qp.checkGetString("tag");
        String type = qp.checkGetString("type");
        String taget_id = qp.checkGetString("target_id");
        Record record = Record.of("user", viewerId, "tag", tag, "type", type, "target_id", taget_id, "created_time", DateUtils.nowMillis());

        return p.destroyedTag(record);
    }

    @WebMethod("tag/finduserbytag")
    public RecordSet findUserByTag(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String viewerId = p.checkSignAndTicket(qp);
        String tag = qp.checkGetString("tag");
        int count = (int) qp.getInt("count", 20);
        int page = (int) qp.getInt("page", 0);

        return p.findUserByTag(tag, page, count);
    }

    @WebMethod("tag/hasTag")
    public boolean hasTag(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String viewerId = p.checkSignAndTicket(qp);
        String tag = qp.checkGetString("tag");

        return p.hasTag(viewerId, tag);
    }

    @WebMethod("tag/hasTarget")
    public boolean hasTarget(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String viewerId = p.checkSignAndTicket(qp);
        String tag = qp.checkGetString("target");
        String type = qp.checkGetString("type");

        return p.hasTarget(viewerId, tag, type);
    }

    @WebMethod("tag/findtagbyuser")
    public RecordSet findTagByUser(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String viewerId = p.checkSignAndTicket(qp);

        int count = (int) qp.getInt("count", 20);
        int page = (int) qp.getInt("page", 0);

        return p.findTagByUser(viewerId, page, count);
    }

    @WebMethod("tag/findtargetbyuser")
    public RecordSet findTargetByUser(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String viewerId = p.checkSignAndTicket(qp);

        String type = qp.checkGetString("type");
        int count = (int) qp.getInt("count", 20);
        int page = (int) qp.getInt("page", 0);

        return p.findTargetByUser(viewerId, type, page, count);
    }

    @WebMethod("tag/findusertagbytarget")
    public RecordSet findUserTagByTarget(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String viewerId = p.checkSignAndTicket(qp);

        String target = qp.checkGetString("target");
        String type = qp.checkGetString("type");
        int count = (int) qp.getInt("count", 20);
        int page = (int) qp.getInt("page", 0);

        return p.findUserTagByTarget(target, type, page, count);
    }

    public static Record getUserInfo(Platform p, String userId) throws AvroRemoteException {
        RecordSet userInfos = getUserInfos(p, userId);
        return userInfos.getFirstRecord();
    }

    @WebMethod("post/create_vote")
    public Record createVote(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();
        String viewerId = p.checkTicket(qp);

        VoteInfo vi = new VoteInfo();
        vi.setTitle(qp.getString("title", ""));
        vi.setDescription(qp.getString("description", ""));
        vi.setChoices(JsonUtils.fromJson(qp.getString("choices", "[]"), ArrayList.class));
        vi.setMinChoice((int) qp.getInt("min_choice", 1));
        vi.setMaxChoice((int) qp.getInt("max_choice", 1));
        vi.setStartTime(qp.getInt("start_time", 0));
        vi.setEndTime(qp.getInt("end_time", Long.MAX_VALUE));
        String appId = qp.checkGetString("appid");

        String ua = getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = getDecodeHeader(req, "location", "", viewerId);
        String post_id = "";

        String add_to = "";
        String mentions = qp.checkGetString("to");

        String tmp_ids = "";

        String share_file_id = qp.getString("file_id", "");

        post_id = p.post(viewerId, Constants.TEXT_POST | Constants.VOTE_POST, vi.getDisplayMessage(), "[]", appId,
                "", "", vi.toJson(false), mentions, true, "", ua, loc, "", "", true, true, false, add_to);

        return p.getFullPostsForQiuPu(viewerId, post_id, true).getFirstRecord();
    }


    // company API
    // ===================================

    @WebMethod("company/show")
    public RecordSet showCompanies(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = 0;
        if (qp.containsKey("ticket")) {
            viewerId = Long.parseLong(p.checkTicket(qp));
        }
        long[] companyIds = StringUtils2.splitIntArray(qp.checkGetString("companies"), ",");
        CompanyLogic cl = new CompanyLogic(p);
        return cl.getCompanies(viewerId, companyIds);
    }

    @WebMethod("company/update")
    public Record updateCompany(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));

        long companyId = qp.checkGetInt("company");
        Record info = new Record();
        info.putIf("email_domain1", qp.getString("email_domain1", ""), qp.containsKey("email_domain1"));
        info.putIf("email_domain2", qp.getString("email_domain2", ""), qp.containsKey("email_domain2"));
        info.putIf("email_domain3", qp.getString("email_domain3", ""), qp.containsKey("email_domain3"));
        info.putIf("email_domain4", qp.getString("email_domain4", ""), qp.containsKey("email_domain4"));
        info.putIf("name", qp.getString("name", ""), qp.containsKey("name"));
        info.putIf("email", qp.getString("email", ""), qp.containsKey("email"));
        info.putIf("address", qp.getString("address", ""), qp.containsKey("address"));
        info.putIf("website", qp.getString("website", ""), qp.containsKey("website"));
        info.putIf("tel", qp.getString("tel", ""), qp.containsKey("tel"));
        info.putIf("fax", qp.getString("fax", ""), qp.containsKey("fax"));
        info.putIf("zip_code", qp.getString("zip_code", ""), qp.containsKey("zip_code"));
        info.putIf("small_logo_url", qp.getString("small_logo_url", ""), qp.containsKey("small_logo_url"));
        info.putIf("logo_url", qp.getString("logo_url", ""), qp.containsKey("logo_url"));
        info.putIf("large_logo_url", qp.getString("large_logo_url", ""), qp.containsKey("large_logo_url"));
        info.putIf("small_cover_url", qp.getString("small_cover_url", ""), qp.containsKey("small_cover_url"));
        info.putIf("cover_url", qp.getString("cover_url", ""), qp.containsKey("cover_url"));
        info.putIf("large_cover_url", qp.getString("large_cover_url", ""), qp.containsKey("large_cover_url"));
        info.putIf("description", qp.getString("description", ""), qp.containsKey("description"));

        CompanyLogic cl = new CompanyLogic(p);
        return cl.updateCompany(viewerId, companyId, info);
    }

    private String[] saveCompanyImages(long companyId, String type, FileItem fi) {
        String[] urls = new String[3];

        long uploaded_time = DateUtils.nowMillis();
        String imageName = type + "_" + companyId + "_" + uploaded_time;

        String sfn = imageName + "_S.jpg";
        String ofn = imageName + "_M.jpg";
        String lfn = imageName + "_L.jpg";
        urls[0] = sfn;
        urls[1] = ofn;
        urls[2] = lfn;

        if (photoStorage instanceof OssSFS) {
            lfn = "media/photo/" + lfn;
            ofn = "media/photo/" + ofn;
            sfn = "media/photo/" + sfn;
        }

        SFSUtils.saveScaledUploadImage(fi, photoStorage, sfn, "50", "50", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, ofn, "80", "80", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, lfn, "180", "180", "jpg");

        return urls;
    }

    @WebMethod("company/upload_logo")
    public Record uploadCompanyLogo(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        FileItem fi = qp.checkGetFile("file");
        String[] urls = saveCompanyImages(companyId, "c_logo", fi);
        Record info = new Record();
        info.put("small_logo_url", urls[0]);
        info.put("logo_url", urls[1]);
        info.put("large_logo_url", urls[2]);
        CompanyLogic cl = new CompanyLogic(p);
        return cl.updateCompany(viewerId, companyId, info);
    }

    @WebMethod("company/upload_cover")
    public Record uploadCompanyCover(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        FileItem fi = qp.checkGetFile("file");
        String[] urls = saveCompanyImages(companyId, "c_cover", fi);
        Record info = new Record();
        info.put("small_cover_url", urls[0]);
        info.put("cover_url", urls[1]);
        info.put("large_cover_url", urls[2]);
        CompanyLogic cl = new CompanyLogic(p);
        return cl.updateCompany(viewerId, companyId, info);
    }

    @WebMethod("company/belongs")
    public RecordSet belongsCompanies(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        CompanyLogic cl = new CompanyLogic(p);
        return cl.belongsCompanies(viewerId, viewerId);
    }

    @WebMethod("company/search")
    public RecordSet searchCompanies(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        String kw = qp.checkGetString("kw");
        CompanyLogic cl = new CompanyLogic(p);
        return cl.searchCompanies(viewerId, kw);
    }

    @WebMethod("company/grant")
    public boolean companyGrant(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        HashMap<Long, Integer> roles = new HashMap<Long, Integer>();
        if (qp.containsKey("admins")) {
            long[] uids = StringUtils2.splitIntArray(qp.checkGetString("admins"), ",");
            for (long uid : uids)
                roles.put(uid, Constants.ROLE_ADMIN);
        }
        if (qp.containsKey("members")) {
            long[] uids = StringUtils2.splitIntArray(qp.checkGetString("members"), ",");
            for (long uid : uids)
                roles.put(uid, Constants.ROLE_MEMBER);
        }

        CompanyLogic cl = new CompanyLogic(p);
        return roles.isEmpty() || cl.grant(viewerId, companyId, roles);
    }

    @WebMethod("company/upload_employees")
    public RecordSet uploadEmployees(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        boolean merge = qp.getBoolean("merge", false);
        FileItem excelFile = qp.checkGetFile("file");
        CompanyLogic cl = new CompanyLogic(p);
        return cl.uploadEmployees(viewerId, companyId, excelFile, merge);
    }

    @WebMethod("company/employee/list")
    public RecordSet listEmployees(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        String sort = qp.getString("sort", EmployeeListConstants.COL_NAME);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 50);

        CompanyLogic cl = new CompanyLogic(p);
        return cl.listEmployees(viewerId, companyId, sort, page, count);
    }

    @WebMethod("company/employee/add")
    public Record addEmployee(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        String name = qp.checkGetString("name");
        String email = qp.checkGetString("email");

        CompanyLogic cl = new CompanyLogic(p);
        Record other = new Record();
        other.put(EmployeeListConstants.COL_EMPLOYEE_ID, qp.getString("employee_id", ""));
        other.put(EmployeeListConstants.COL_DEPARTMENT, qp.getString("department", ""));
        other.put(EmployeeListConstants.COL_JOB_TITLE, qp.getString("job_title", ""));
        other.put(EmployeeListConstants.COL_TEL, qp.getString("tel", ""));
        other.put(EmployeeListConstants.COL_MOBILE_TEL, qp.getString("mobile_tel", ""));
        return cl.addEmployee(viewerId, companyId, name, email, other);
    }

    @WebMethod("company/employee/update")
    public Record updateEmployee(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        String email = qp.checkGetString("email");


        CompanyLogic cl = new CompanyLogic(p);
        Record other = new Record();
        other.put(EmployeeListConstants.COL_NAME, qp.getString("name", ""));
        other.put(EmployeeListConstants.COL_EMPLOYEE_ID, qp.getString("employee_id", ""));
        other.put(EmployeeListConstants.COL_DEPARTMENT, qp.getString("department", ""));
        other.put(EmployeeListConstants.COL_JOB_TITLE, qp.getString("job_title", ""));
        other.put(EmployeeListConstants.COL_TEL, qp.getString("tel", ""));
        other.put(EmployeeListConstants.COL_MOBILE_TEL, qp.getString("mobile_tel", ""));
        return cl.updateEmployee(viewerId, companyId, email, other);
    }

    @WebMethod("company/employee/delete")
    public boolean deleteEmployees(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        String[] emails = StringUtils2.splitArray(qp.checkGetString("emails"), ",", true);

        CompanyLogic cl = new CompanyLogic(p);
        return cl.deleteEmployees(viewerId, companyId, emails);
    }

    @WebMethod("company/employee/search")
    public RecordSet searchEmployee(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        String kw = qp.checkGetString("kw");

        CompanyLogic cl = new CompanyLogic(p);
        return cl.searchEmployee(viewerId, companyId,
                kw,
                qp.getString("sort", EmployeeListConstants.COL_NAME),
                (int) qp.getInt("count", 100));
    }

    @WebMethod("company/employee/info")
    public RecordSet getEmployeeInfo(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));;
        long[] userIds = StringUtils2.splitIntArray(qp.checkGetString("users"), ",");
        CompanyLogic cl = new CompanyLogic(p);
        return cl.getEmployeeInfos(viewerId, userIds);
    }

    @WebMethod("company/users")
    public RecordSet getCompanyUsers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        String cols = qp.getString("cols", Platform.USER_LIGHT_COLUMNS);

        CompanyLogic cl = new CompanyLogic(p);
        return cl.getCompanyUsers(viewerId, companyId, cols, page, count);
    }

    @WebMethod("company/department_circles")
    public RecordSet getSubDeps(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        CompanyLogic cl = new CompanyLogic(p);
        return cl.getCompanyDepCircles(viewerId, companyId);
    }

    @WebMethod("company/create_from_circle")
    public Record createCompanyFromCircle(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long groupId = qp.checkGetInt("group");
        String name = qp.checkGetString("name");
        String emailDomain = qp.checkGetString("email_domain");
        CompanyLogic cl = new CompanyLogic(p);
        return cl.createCompanyFromGroup(0, name, emailDomain, groupId);
    }

    @WebMethod("company/auto_create_departments")
    public RecordSet autoCreateDepartments(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        long viewerId = Long.parseLong(p.checkTicket(qp));
        long companyId = qp.checkGetInt("company");
        CompanyLogic cl = new CompanyLogic(p);
        cl.createDepartmentCircleByEmployeeList(viewerId, companyId);
        return cl.getCompanyDepCircles(viewerId, companyId);
    }
}
