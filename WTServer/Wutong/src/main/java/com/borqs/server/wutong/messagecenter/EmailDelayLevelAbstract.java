package com.borqs.server.wutong.messagecenter;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.email.AsyncMailTask;
import com.borqs.server.wutong.email.EmailModel;
import com.borqs.server.wutong.email.template.InnovTemplate;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.*;

public abstract class EmailDelayLevelAbstract {
    private static final Logger L = Logger.getLogger(EmailDelayLevelAbstract.class);
    private static final PageTemplate pageTemplate = new PageTemplate(InnovTemplate.class);
    public static String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
    //public static String confPath = "F:\\work\\refactProduct\\Dist\\src\\main\\etc\\test.config.properties";
    public static final int MAX_SIZE_RECORD = 7;
    public static final String MAIL_RECOMMEND_USER = "mail.recommend_user";
    public static final String MAIL_CIRCLE_REQUEST_JOIN = "mail.circle_request_join";
    public static final String MAIL_NEW_FOLLOWERS = "mail.new_followers";
    public static final String EMAIL_SEND_EMAIL_ERROR = "email.sendEmail.error";


    public static void delayLevel(Context ctx, String delayType) throws IOException {
        try {
            try {
                GlobalLogics.init();
            } catch (Exception e) {
                L.error(new Context(), e, "-----------------error!!!!!!!!!!!!!!!!!!!!!!!!");
            }

            MessageCenterLogic messageCenterLogic = GlobalLogics.getMessageCenter();
            RecordSet rs = messageCenterLogic.getMessageDistinctListByDelayType(ctx, delayType);
            //得到需要合并的邮件接收人
            for (Record r : rs) {
                // 如果sendkey类型为向用户A推荐用户B，那么需要单独处理
                if (MAIL_RECOMMEND_USER.equals(r.getString("send_key")) || EMAIL_SEND_EMAIL_ERROR.equals(r.getString("send_key"))) {
                    continue;
                }
                String ids = "";
                String send_key = r.getString("send_key");
                //判断该邮件是否只延迟，不合并
                boolean isCombine = MessageConfig.getMessageConfigKeyBoolean(send_key, "email_combine");
                if (!isCombine) {
                    //非组合，只是时间延迟
                    ids = delayEmail(ctx, send_key, r.getString("to_"), r.getString("username"));
                } else {
                    if (StringUtils.equals(send_key, "mail.event.share_photo")) {
                        ids = combineEmailEventPhoto(ctx, send_key, r.getString("to_"), r.getString("username"), r.getString("target_id"));
                    } else {
                        ids = combineEmail(ctx, send_key, r.getString("to_"), r.getString("username"));
                    }
                }

                if (StringUtils.length(ids) > 0) {
                    messageCenterLogic.destroyMessageById(ctx, ids);
                    System.out.print(" delete ! the combine email!");
                }
            }

            //单独扫描向用户A推荐用户B的类型
            RecordSet rs1 = messageCenterLogic.getMessageDistinctListByDelayTypeAndSendKey(ctx, delayType, MAIL_RECOMMEND_USER);
            for (Record r : rs1) {
                String ids = "";
                String send_key = r.getString("send_key");

                ids = combineEmail(ctx, send_key, r.getString("to_"), r.getString("username"));


                if (StringUtils.length(ids) > 0) {
                    messageCenterLogic.destroyMessageById(ctx, ids);
                    System.out.print(" delete ! the combine email!");
                }
            }
        } catch (Exception e) {
            L.error(ctx, e);
        } finally {
            MQCollection.destroyMQs();
        }
    }

    //TODO add new method for combine the event email
    private static String combineEmailEventPhoto(Context ctx, String sendKey, String to, String username, String target_id) {
        MessageCenterLogic messageCenterLogic = GlobalLogics.getMessageCenter();
        RecordSet rs = messageCenterLogic.getMessageByKeyAndTarget(ctx, sendKey, to, target_id);
        Record r = rs.getFirstRecord();
        // query message_center_finish and get the latest record and checkout
        ObjectMapper mapper = new ObjectMapper();

        Record recordOld = messageCenterLogic.getMessageFinish(ctx, sendKey, target_id, to);
        List photoList = new ArrayList<String>();
        if (recordOld.size() > 0) {
            String c = recordOld.getString("content");
            try {
                Map<String, Object> map = mapper.readValue(c, Map.class);
                String[] photos = MapUtils.getString(map, "photos", "").split(",");
                photoList = Arrays.asList(photos);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String content = r.getString("content");


        try {
            Map<String, Object> map = mapper.readValue(content, Map.class);
            String[] photos = MapUtils.getString(map, "photos", "").split(",");
            List<String> pList = Arrays.asList(photos);
            pList = ListUtils.removeAll(pList, photoList);
            String photoHtml = formatImageTag(pList);
            map.put("photos", photoHtml);

            String template = MessageConfig.getMessageConfigKeyString(sendKey, "template_name");
            String html = pageTemplate.merge(template, map);

            EmailModel email = EmailModel.getDefaultEmailModule(GlobalConfig.get());
            email.setContent(html);
            email.setTitle(r.getString("title", ""));
            email.setTo(to);
            email.setUsername(username);
            email.setRecord(new Record(map));

            new AsyncMailTask().sendEmailFinal(ctx, email);
            List<String> ids = rs.getStringColumnValues("message_id");

            return StringUtils.join(ids, ",");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String formatImageTag(List<String> urls) {
        String html = "";
        for (String url : urls) {
            html += "<a href=\"" + url + "\"><img src=\"" + url + "\" alt=\"Large image\" style='margin-right: 2px;width:390px;border:0px #808080 solid;text-align:center;'></a>";
        }
        return html;
    }

    /**
     * combine the email
     *
     * @param ctx
     * @param sendKey
     * @param to
     * @param username
     * @return
     */
    private static String combineEmail(Context ctx, String sendKey, String to, String username) {


        MessageCenterLogic messageCenterLogic = GlobalLogics.getMessageCenter();
        RecordSet recordSet0 = messageCenterLogic.getMessageByKey(ctx, sendKey, to, "");

        RecordSet rs = reDuplicate(recordSet0);


        Map listUsername = rs.toRecordMap("from_username");
        String names = StringUtils.join(listUsername.keySet(), ",");

        //判断rs是否大于5，如果大于5个，那么需要显示 更多....
        RecordSet recordSet;
        String more = "";
        if (rs.size() > MAX_SIZE_RECORD) {
            recordSet = rs.sliceCopy(0, MAX_SIZE_RECORD);
            more = "true";
        } else {
            recordSet = rs;
        }

        // Loop the recordSet and kick out the guy have been your friend
        FriendshipLogic friendshipLogic = GlobalLogics.getFriendship();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        for (Record r : recordSet) {
            RecordSet ids = accountLogic.getUserIds(ctx, r.getString("to_"));
            boolean b = friendshipLogic.isFriendP(ctx, r.getString("from_id"), ids.getFirstRecord().getString("user_id"));
            if (b) {
                r.put("isFriend", "true");
            } else {
                r.put("isFriend", "false");
            }
        }
        //查询需要合并的邮件
        Map<String, Record> map = recordSet.toRecordMap("message_id");
        String template = MessageConfig.getMessageConfigKeyString(sendKey, "template_name");

        String content = MessageConfig.getMessageConfigKeyString(sendKey, "content");

        if (MAIL_NEW_FOLLOWERS.equals(sendKey)) {
            content = content + " " + names;
        } else {
            content = names + " " + content;
        }
        if (rs.size() > 0 && MAIL_CIRCLE_REQUEST_JOIN.equals(sendKey)) {
            content += "【" + rs.getFirstRecord().getString("target_name") + "】";
        }

        EmailModel emailModule = EmailModel.getDefaultEmailModule(GlobalConfig.get());
        Map<String, Object> map1 = new HashMap<String, Object>();
        map1.put("mapCombine", map);

        if (StringUtils.isNotBlank(more)) {
            map1.put("more", "true");
            map1.put("moreinfo", "");
        } else {
            map1.put("more", "false");
        }

        String htmlContent = pageTemplate.merge(template, new Object[][]{{"map", map1}});
        map1.put("content", content);
        map1.put("displayName", username);
        //显示退订信息
        map1.put("subscribe", "");
        map1.put("html", htmlContent);

        String html = pageTemplate.merge("combine.ftl", new Object[][]{{"map", map1}});
        emailModule.setContent(html);
        emailModule.setTo(to);
        emailModule.setUsername(username);
        String title = MessageConfig.getMessageConfigKeyString(sendKey, "title");
        emailModule.setTitle(title);
        //AsyncSendMailUtil.sendEmailFinal(ctx, emailModule);
        new AsyncMailTask().sendEmailFinal(ctx, emailModule);
        List<String> ids = recordSet0.getStringColumnValues("message_id");

        return StringUtils.join(ids, ",");
    }

    private static RecordSet reDuplicate(RecordSet recordSet0) {
        RecordSet rs = new RecordSet();
        Record record = new Record();
        for (Record r : recordSet0) {
            String content = r.getString("content");
            if (!record.containsValue(content)) {
                record.put(r.getString("message_id"), content);
                rs.add(r);
            }
        }
        return rs;
    }

    /**
     * delay the email
     *
     * @param ctx
     * @param sendKey
     * @param to
     * @param username
     * @return
     * @throws IOException
     */
    private static String delayEmail(Context ctx, String sendKey, String to, String username) throws IOException {
        MessageCenterLogic messageCenterLogic = GlobalLogics.getMessageCenter();
        String template = MessageConfig.getMessageConfigKeyString(sendKey, "template_name");
        RecordSet rs = messageCenterLogic.getMessageByKey(ctx, sendKey, to, "");
        EmailModel emailModule = EmailModel.getDefaultEmailModule(GlobalConfig.get());

        //需要独立的模板来显示，暂不能与合并邮件的模板公用
        for (Record r : rs) {
            String content = r.getString("content");

            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> map = mapper.readValue(content, Map.class);

            Map<String, Map> mapTemp = new HashMap<String, Map>();
            mapTemp.put("temp", map);

            Map<String, Object> map1 = new HashMap<String, Object>();
            map1.put("mapCombine", mapTemp);
            map1.put("content", content);
            map1.put("displayName", username);
            //显示退订信息
            map1.put("subscribe", "");
            String html = pageTemplate.merge(template, new Object[][]{{"map", map1}});


            emailModule.setContent(html);
            emailModule.setTo(to);
            emailModule.setUsername(username);
            emailModule.setTitle(r.getString("title"));

            //AsyncSendMailUtil.sendEmailFinal(ctx, emailModule);
            new AsyncMailTask().sendEmailFinal(ctx, emailModule);
        }

        List<String> ids = rs.getStringColumnValues("message_id");
        return StringUtils.join(ids, ",");
    }
}