package com.borqs.server.wutong.messagecenter;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.StringMap;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.contacts.SocialContactsLogic;
import com.borqs.server.wutong.email.AsyncMailTask;
import com.borqs.server.wutong.email.EmailLogic;
import com.borqs.server.wutong.email.EmailModel;
import com.borqs.server.wutong.email.template.InnovTemplate;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageDelayCombineUtils {
    private static final Logger L = Logger.getLogger(MessageDelayCombineUtils.class);
    public static final String HTTP_BPC_BORQS_COM_LOGIN_HTML_SHOWHOME_GROUPID = "http://bpc.borqs.com/login.html#showhome/groupid=";
    public static final String HTTP_API_BORQS_COM_FRIEND_CIRCLESSET = "http://api.borqs.com/friend/circlesset?";
    public static final String HTTP_API_BORQS_COM_V2_PUBLIC_CIRCLE_APPROVE = "http://api.borqs.com/v2/public_circle/approve?";
    public static final String HTTP_API_BORQS_COM_V2_PUBLIC_CIRCLE_IGNORE = "http://api.borqs.com/v2/public_circle/ignore?";
    private static final PageTemplate pageTemplate = new PageTemplate(InnovTemplate.class);
    public static final String CIRCLE_SHUREN = "11";


    public static void combinePeopleYouMayKnow(Context ctx, RecordSet recordSet, String sendKey, String username, String to, String lang) {

        for (Record r : recordSet) {
            r.set("url", HTTP_BPC_BORQS_COM_LOGIN_HTML_SHOWHOME_GROUPID + r.getString("user_id"));
            r.set("iconUrl", r.getString("image_url"));
            r.set("follower_name", r.getString("display_name"));
            StringMap qp2 = new StringMap();
            //得到circleIds、user_ids
            qp2.put("circleIds", "11");
            qp2.put("friendId", r.getString("user_id"));
            qp2.put("secretly", true);

            String url = HTTP_API_BORQS_COM_FRIEND_CIRCLESSET + Commons.getEmailActionUrl(new Context(), to, qp2);
            r.set("addFriendUrl", url);
        }
        Map<String, Record> map = recordSet.toRecordMap("user_id");

        StringBuffer userName = new StringBuffer();
        for (Record r : recordSet) {
            userName.append(r.getString("display_name")).append(",");
        }

        String names = "";
        if (userName.length() > 0) {
            names = StringUtils.substring(userName.toString(), 0, userName.length() - 1);
        }
        String template = MessageConfig.getMessageConfigKeyString(sendKey, "template_name");


        //String content = MessageConfig.getMessageConfigKeyString(sendKey, "content");
        String content = Constants.getBundleStringByLang(lang, sendKey + ".content");


        //content = names + " " + content;


        EmailModel emailModule = EmailModel.getDefaultEmailModule(GlobalConfig.get());
        Map<String, Object> map1 = new HashMap<String, Object>();
        map1.put("mapCombine", map);

        map1.put("content", content);
        map1.put("displayName", username);
        //显示退订信息
        map1.put("subscribe", "");

        String htmlContent = pageTemplate.merge(template, new Object[][]{{"map", map1}});

        map1.put("html", htmlContent);

        String html = pageTemplate.merge("combine.ftl", new Object[][]{{"map", map1}});
        emailModule.setContent(html);
        emailModule.setTo(to);
        emailModule.setUsername(username);


        //String title = MessageConfig.getMessageConfigKeyString(sendKey, "title");
        String title = Constants.getBundleStringByLang(lang, sendKey + ".title");
        emailModule.setTitle(title);
//        AsyncSendMailUtil.sendEmailFinal(ctx, emailModule);
        new AsyncMailTask().sendEmailFinal(ctx, emailModule);
        List<String> ids = recordSet.getStringColumnValues("message_id");
    }

    /**
     * join group
     *
     * @param ctx
     * @param userId
     * @param toId
     * @param groupId
     * @param groupName
     */
    public static void sendEmailCombineAndDelayJoinGroup(Context ctx, String userId, String toId, String groupId, String groupName) {
        EmailLogic emailLogic = GlobalLogics.getEmail();
        AccountLogic accountLogic = GlobalLogics.getAccount();

        Record user2 = accountLogic.getUser(ctx, toId, toId, "image_url,display_name,login_email1,login_email2,login_email3", false);
        Record user = accountLogic.getUser(ctx, userId, userId, "image_url,display_name");

        EmailModel email = new EmailModel(GlobalConfig.get());
        email.setContent("this is content!");
        email.setEmailType("mail.circle_request_join");

        Record record = Record.of("iconUrl", user.getString("image_url"), "url", HTTP_BPC_BORQS_COM_LOGIN_HTML_SHOWHOME_GROUPID + userId, "follower_name", user.getString("display_name"));
        StringMap qp2 = new StringMap();
        //得到circleIds、user_ids
        qp2.put("id", groupId);
        qp2.put("user_ids", userId);

        String url = HTTP_API_BORQS_COM_V2_PUBLIC_CIRCLE_APPROVE + Commons.getEmailActionUrl(new Context(), toId, qp2);
        String url2 = HTTP_API_BORQS_COM_V2_PUBLIC_CIRCLE_IGNORE + Commons.getEmailActionUrl(new Context(), toId, qp2);

        record.put("approve", url);
        record.put("ignore", url2);

        email.setRecord(record);
        email.setSendEmailName(EmailModel.DEFAULT_SEND_EMAILNAME);
        email.setSendEmailPassword(EmailModel.DEFAULT_SEND_EMAILPASSWORD);
        email.setTitle("this is the title");

        String loginEmail1 = user2.getString("login_email1");
        String loginEmail2 = user2.getString("login_email2");
        String loginEmail3 = user2.getString("login_email3");
        List<String> l = new ArrayList<String>();
        if (StringUtils.isNotBlank(loginEmail1))
            l.add(loginEmail1);
        if (StringUtils.isNotBlank(loginEmail2))
            l.add(loginEmail2);
        if (StringUtils.isNotBlank(loginEmail3))
            l.add(loginEmail3);

        for (String s : l) {
            email.setTo(s);
            email.setUsername(user2.getString("display_name"));

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("target_id", groupId);
            //暂时用0表示group
            map.put("target_type", "0");
            map.put("target_name", groupName);
            email.setMap(map);
            emailLogic.sendEmailDelay(ctx, email);
        }

    }

    /**
     * combine event photo and delay
     *
     * @param ctx
     * @param map
     * @param username
     * @param to
     */
    public static void combineEventPhotos(Context ctx, String subject, Map<String, Object> map, String username, String to) {
        Record r = new Record(map);

        EmailModel email = new EmailModel(GlobalConfig.get());
        email.setRecord(r);
        email.setEmailType("mail.event.share_photo");
        email.setTitle(subject);
        email.setTo(to);
        email.setUsername(username);
        email.setSendEmailName(EmailModel.DEFAULT_SEND_EMAILNAME);
        email.setSendEmailPassword(EmailModel.DEFAULT_SEND_EMAILPASSWORD);

        EmailLogic emailLogic = GlobalLogics.getEmail();
        emailLogic.sendEmailDelay(ctx,email);
    }

    /**
     * recommend_user
     *
     * @param ctx
     * @param userIdA
     * @param userIdB
     */
    public static void sendEmailCombineAndDelayRecommendUser(Context ctx, String userIdA, List<String> userIdB) {
        EmailLogic emailLogic = GlobalLogics.getEmail();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        RecordSet users = accountLogic.getUsers(ctx, StringUtils2.joinIgnoreBlank(",", userIdB), "image_url,display_name");
        Record user2 = accountLogic.getUser(ctx, userIdA, userIdA, "image_url,display_name,login_email1,login_email2,login_email3", false);

        EmailModel email = new EmailModel(GlobalConfig.get());
        email.setContent("this is content!");
        email.setEmailType("mail.recommend_user");


        String loginEmail1 = user2.getString("login_email1");
        String loginEmail2 = user2.getString("login_email2");
        String loginEmail3 = user2.getString("login_email3");
        List<String> l = new ArrayList<String>();
        if (StringUtils.isNotBlank(loginEmail1))
            l.add(loginEmail1);
        if (StringUtils.isNotBlank(loginEmail2))
            l.add(loginEmail2);
        if (StringUtils.isNotBlank(loginEmail3))
            l.add(loginEmail3);

        for (String s : l) {
            for (Record userb : users) {

                StringMap qp2 = new StringMap();
                Record record = Record.of("iconUrl", userb.getString("image_url"), "url", HTTP_BPC_BORQS_COM_LOGIN_HTML_SHOWHOME_GROUPID + userb.getString("user_id"), "follower_name", userb.getString("display_name"));
                //得到circleIds、user_ids 默认为11号圈子 ”熟人“
                qp2.put("circleIds", "11");
                qp2.put("friendId", userb.getString("user_id"));
                qp2.put("secretly", true);
                String url = HTTP_API_BORQS_COM_FRIEND_CIRCLESSET + Commons.getEmailActionUrl(new Context(), s, qp2);

                record.put("addFriendUrl", url);
                email.setRecord(record);
                email.setSendEmailName(EmailModel.DEFAULT_SEND_EMAILNAME);
                email.setSendEmailPassword(EmailModel.DEFAULT_SEND_EMAILPASSWORD);
                email.setTitle("this is the title");

                email.setTo(s);
                email.setUsername(user2.getString("display_name"));
                emailLogic.sendEmailDelay(ctx, email);
            }
        }

    }

    /**
     * add new follower
     *
     * @param ctx
     * @param userId
     * @param userIds
     */
    public static void sendEmailCombineAndDelayNewFollower(Context ctx, String userId, List<String> userIds) {
        //check the user is his friend
        List<String> friendList = new ArrayList<String>();
        for (String s : userIds) {
            FriendshipLogic friendshipLogic = GlobalLogics.getFriendship();
            boolean b = friendshipLogic.isHisFriendP(ctx, userId, s);
            if (!b)
                friendList.add(s);

        }

        EmailLogic emailLogic = GlobalLogics.getEmail();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        Record user = accountLogic.getUser(ctx, userId, userId, "image_url,display_name");

        String cUserIds = StringUtils2.joinIgnoreBlank(",", friendList);
        RecordSet users = accountLogic.getUsers(ctx, userId, cUserIds, "image_url,display_name,login_email1,login_email2,login_email3", false);

        for (Record r : users) {
            String loginEmail1 = r.getString("login_email1");
            String loginEmail2 = r.getString("login_email2");
            String loginEmail3 = r.getString("login_email3");
            List<String> l = new ArrayList<String>();
            if (StringUtils.isNotBlank(loginEmail1))
                l.add(loginEmail1);
            if (StringUtils.isNotBlank(loginEmail2))
                l.add(loginEmail2);
            if (StringUtils.isNotBlank(loginEmail3))
                l.add(loginEmail3);

            for (String e : l) {
                EmailModel email = new EmailModel(GlobalConfig.get());
                email.setContent("this is content!");
                email.setEmailType("mail.new_followers");

                Record record = Record.of("iconUrl", user.getString("image_url"), "url", HTTP_BPC_BORQS_COM_LOGIN_HTML_SHOWHOME_GROUPID + userId, "follower_name", user.getString("display_name"));
                StringMap qp2 = new StringMap();

                //得到circleIds、user_ids 默认为11号圈子 ”熟人“
                qp2.put("circleIds", "11");
                qp2.put("friendId", userId);
                qp2.put("secretly", true);


                String url = HTTP_API_BORQS_COM_FRIEND_CIRCLESSET + Commons.getEmailActionUrl(new Context(), e, qp2);

                record.put("addFriendUrl", url);
                email.setRecord(record);
                email.setSendEmailName(EmailModel.DEFAULT_SEND_EMAILNAME);
                email.setSendEmailPassword(EmailModel.DEFAULT_SEND_EMAILPASSWORD);
                email.setTitle("this is the title");
                email.setTo(e);
                email.setUsername(r.getString("display_name"));

                emailLogic.sendEmailDelay(ctx, email);
            }
        }

    }

    /**
     * account create
     *
     * @param ctx
     * @param userId
     * @param login_email1
     */

    public static void sendEmailCombineAndDelayAccountCreate(Context ctx, String userId, String login_email1) {
        EmailLogic emailLogic = GlobalLogics.getEmail();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        Record user = accountLogic.getUser(ctx, userId, userId, "image_url,display_name");

        EmailModel email = new EmailModel(GlobalConfig.get());
        email.setContent("this is content!");
        email.setEmailType("mail.create_account");
        Record record = Record.of("iconUrl", user.getString("image_url"), "url", HTTP_BPC_BORQS_COM_LOGIN_HTML_SHOWHOME_GROUPID + userId, "follower_name", user.getString("display_name"));
        StringMap qp2 = new StringMap();

        //得到circleIds、user_ids 默认为11号圈子 ”熟人“
        qp2.put("circleIds", CIRCLE_SHUREN);
        qp2.put("friendId", userId);
        qp2.put("secretly", true);
        String url = HTTP_API_BORQS_COM_FRIEND_CIRCLESSET + Commons.getEmailActionUrl(new Context(), login_email1, qp2);

        record.put("addFriendUrl", url);

        email.setRecord(record);
        email.setSendEmailName(EmailModel.DEFAULT_SEND_EMAILNAME);
        email.setSendEmailPassword(EmailModel.DEFAULT_SEND_EMAILPASSWORD);
        email.setTitle("this is the title");

        List<String> longs = getWhohasMyContacts(ctx, userId, login_email1);

        RecordSet recordSet = accountLogic.getUsers(ctx, userId, StringUtils.join(longs, ","), "display_name,login_email1,login_email2,login_email3", false);
        for (Record r : recordSet) {
            String loginEmail1 = r.getString("login_email1");
            String loginEmail2 = r.getString("login_email2");
            String loginEmail3 = r.getString("login_email3");
            List<String> l = new ArrayList<String>();
            if (StringUtils.isNotBlank(loginEmail1))
                l.add(loginEmail1);
            if (StringUtils.isNotBlank(loginEmail2))
                l.add(loginEmail2);
            if (StringUtils.isNotBlank(loginEmail3))
                l.add(loginEmail3);

            for (String s : l) {
                email.setTo(s);
                email.setUsername(r.getString("display_name"));
                emailLogic.sendEmailDelay(ctx, email);
            }
        }
    }

    private static List<String> getWhohasMyContacts(Context ctx, String senderId, String email) {
        List<String> scope = new ArrayList<String>();

        SocialContactsLogic socialContacts = GlobalLogics.getSocialContacts();
        RecordSet recs = socialContacts.getWhohasMyContacts(ctx, senderId, email, "");
        L.debug(ctx, "=====getWhoHasMyContacts,recs=" + recs.toString(false, false));
        for (Record r : recs) {
            scope.add(r.getString("owner"));
        }

        //exclude sender
        if (StringUtils.isNotBlank(senderId)) {
            scope.remove(Long.parseLong(senderId));
        }

        return scope;
    }
}
