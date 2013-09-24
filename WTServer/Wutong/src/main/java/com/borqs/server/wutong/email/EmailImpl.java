package com.borqs.server.wutong.email;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.I18nUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.email.template.InnovTemplate;
import com.borqs.server.wutong.messagecenter.MessageCenter;
import com.borqs.server.wutong.messagecenter.MessageCenterLogic;
import com.borqs.server.wutong.messagecenter.MessageConfig;
import com.borqs.server.wutong.messagecenter.UserSettingFilter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class EmailImpl implements EmailLogic, Initializable {
    private static final Logger L = Logger.getLogger(EmailImpl.class);
    private static final PageTemplate pageTemplate = new PageTemplate(InnovTemplate.class);
    public static final String MAIL_CIRCLE_REQUEST_JOIN = "mail.circle_request_join";
    public static final String EMAIL_SEND_EMAIL_ERROR = "email.sendEmail.error";

    @Override
    public void init() {

    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean sendEmail(Context ctx, String title, String to, String username, String content, String type, String lang) {
        Configuration conf = GlobalConfig.get();

        String serverHost = conf.getString("server.host", "api.borqs.com");
        String template;
        String subscribe = "";
        if (!StringUtils.equals(type, "email.essential") && !StringUtils.equals(type, "email.share_to")) {
            //            content = content + "		这封邮件是发送给<a href=mailto:" + to + ">" + to + "</a>的，<br>"
            //                + "     如果您不想再接收到此种类型的邮件，请点击<a href=http://" + serverHost + "/preferences/subscribe?user=" + to + "&type=" + type + "&value=1 target=_blank>退订</a>。<br>";
            template = I18nUtils.getBundleStringByLang(lang, "asyncsendmail.mailbottom.subscribe");
            subscribe = SQLTemplate.merge(template, new Object[][]{
                    {"to", to},
                    {"serverHost", serverHost},
                    {"type", type}
            });

        } else if (StringUtils.equals(type, "email.share_to")) {
            template = I18nUtils.getBundleStringByLang(lang, "asyncsendmail.mailbottom.shareto");
            subscribe = SQLTemplate.merge(template, new Object[][]{
                    {"to", to},
                    {"serverHost", serverHost},
                    {"type", type}
            });
        }

        String imageUrl = GlobalLogics.getAccount().getUser(ctx, ctx.getViewerIdString(), ctx.getViewerIdString(), "large_image_url").getString("large_image_url");
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("icon", imageUrl);
        map.put("displayName", username);
        map.put("content", content);
        map.put("subscribe", subscribe);
        String html = pageTemplate.merge("default2.ftl", map);

        EmailModel email = new EmailModel(GlobalConfig.get());
        email.setContent(html);
        email.setTitle(title);
        email.setTo(to);
        email.setUsername(username);
        email.setRecord(new Record(map));

        sendEmailFinal(ctx, email);
        return true;
    }


    @Override
    public boolean sendCustomEmailP(Context ctx, String title, String to, String username, String templateFile, Map<String, Object> map, String type, String lang) {
        String html = pageTemplate.merge(templateFile, map);
        EmailModel email = new EmailModel(GlobalConfig.get());
        email.setContent(html);
        email.setTitle(title);
        email.setTo(to);
        email.setUsername(username);
        email.setRecord(new Record(map));
        sendEmailFinal(ctx, email);
        return true;
    }

    /**
     * 发送邮件方法，用于检测是否对该邮件做延迟和拆分处理
     *
     * @param ctx
     * @param email
     * @return
     */
    @Override
    public boolean sendEmailDelay(Context ctx, EmailModel email) {
        //是否需要发送邮件
        if (checkUserSetting(ctx, email)) {
            try {
                //检查是否延迟

                //如果被延迟了，那么查询它的 record
                //Record config = MessageConfig.getMessageRecord(email.getEmailType());

                MessageCenter messageCenter = new MessageCenter();
                messageCenter.setContent(email.getRecord().toString(false, false));
                String delayType = MessageConfig.getMessageConfigKeyString(email.getEmailType(), "email_send_period");
                messageCenter.setDelayType(delayType);
                messageCenter.setSendKey(email.getEmailType());
                //对于合并邮件这里的title被MessageConfig表中的title代替
                messageCenter.setTitle(email.getTitle());
                messageCenter.setTo(email.getTo());
                messageCenter.setUsername(email.getUsername());
                messageCenter.setFromId(ctx.getViewerIdString());

                AccountLogic accountLogic = GlobalLogics.getAccount();
                Record r = accountLogic.getUser(ctx, ctx.getViewerIdString(), ctx.getViewerIdString(), "display_name");
                messageCenter.setFromUsername(r.getString("display_name"));

                boolean isCombine = MessageConfig.getMessageConfigKeyBoolean(email.getEmailType(), "email_combine");
                String emailCombine = isCombine ? "1" : "0";
                messageCenter.setEmailCombine(emailCombine);

                //如果是用户申请加入某个圈子，那么记录下圈子名称
                if (MAIL_CIRCLE_REQUEST_JOIN.equals(email.getEmailType())) {
                    Map<String, Object> map = email.getMap();
                    if (MapUtils.isNotEmpty(map)) {
                        messageCenter.setTargetId((String) map.get("target_id"));
                        messageCenter.setTargetName((String) map.get("target_name"));
                        messageCenter.setTargetType((String) map.get("target_type"));
                    } else {
                        messageCenter.setTargetId("0");
                        messageCenter.setTargetName("0");
                        messageCenter.setTargetType("0");
                        L.info(ctx, "circle info error!");
                    }
                } else {
                    messageCenter.setTargetId("0");
                    messageCenter.setTargetName("0");
                    messageCenter.setTargetType("0");
                }

                MessageCenterLogic messageCenterLogic = GlobalLogics.getMessageCenter();
                messageCenterLogic.createMessageCenter(ctx, messageCenter);

            } catch (Exception e) {
                L.error(ctx, e, "combine and delay email error!");
            }
            return true;
        } else {
            EmailLogic emailLogic = GlobalLogics.getEmail();
            emailLogic.sendEmailFinal(ctx,email);
            //new AsyncMailTask().sendEmailFinal(ctx, email);
            //AsyncSendMailUtil.sendEmailFinal(ctx, email);
            return true;
        }
    }

    @Override
    public boolean sendEmailFinal(Context ctx, EmailModel email) {
        if(StringUtils.isBlank(email.getSendEmailName())){
            email.setSendEmailName(EmailModel.DEFAULT_SEND_EMAILNAME);
            email.setSendEmailPassword(EmailModel.DEFAULT_SEND_EMAILPASSWORD);
        }
        Commons.sendEmail(ctx, email);
        //new AsyncMailTask().sendEmailFinal(ctx, email);
//        AsyncSendMailUtil.sendEmailFinal(ctx, email);
        return true;
    }

    /**
     * 根据用户设置的setting 和系统默认配置求与 得到布尔值
     *
     * @param ctx
     * @param email
     * @return
     */
    private boolean checkUserSetting(Context ctx, EmailModel email) throws ServerException {
        try {
            String uid = GlobalLogics.getAccount().findUserIdByUserName(ctx, email.getTo());

            //用户设定，如果没有用户设置，那么默认取配置文件中的设置
            String userSetting = UserSettingFilter.getAllUserSettingFilter(ctx, email.getEmailType(), uid);

            //系统设定，配置表中的设置
            boolean sysConfig_send = MessageConfig.getMessageConfigKeyBoolean(email.getEmailType(), "email_send");
            boolean sysConfig_delay = MessageConfig.getMessageConfigKeyBoolean(email.getEmailType(), "email_delayed");

            return sysConfig_send && sysConfig_delay && BooleanUtils.toBoolean(userSetting);
        } catch (ServerException e) {
            L.error(ctx, e, "check User Email Setting error!");
            throw e;
        }
    }

    @Override
    public boolean saveFailureEmail(Context ctx, EmailModel emailModel) {
        if(emailModel == null )
            return false;

        // only save the type : delay_type is null
        if(StringUtils.isBlank(emailModel.getEmailType())){
            emailModel.setEmailType(MessageCenter.EMAIL_DELAY_TYPE_MINUTES);
        }
        MessageCenterLogic messageCenterLogic = GlobalLogics.getMessageCenter();

        MessageCenter messageCenter = new MessageCenter();
        messageCenter.setContent(JsonHelper.toJson(emailModel.getContent(),false));
        messageCenter.setDelayType(emailModel.getEmailType());
        messageCenter.setTitle(emailModel.getTitle());
        messageCenter.setTo(emailModel.getTo());
        messageCenter.setUsername(emailModel.getUsername());
        messageCenter.setDelayType(EMAIL_SEND_EMAIL_ERROR);

        messageCenterLogic.createMessageCenter(ctx,messageCenter);
        //this.sendEmailFinal(ctx,emailModel);

        return true;
    }
}
