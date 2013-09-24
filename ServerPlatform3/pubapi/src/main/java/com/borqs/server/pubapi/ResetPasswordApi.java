package com.borqs.server.pubapi;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.maker.Maker;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.*;
import com.borqs.server.platform.util.sender.AsyncSender;
import com.borqs.server.platform.util.sender.email.AsyncMailSender;
import com.borqs.server.platform.util.sender.email.Mail;
import com.borqs.server.platform.util.sender.sms.Message;
import com.borqs.server.platform.util.template.FreeMarker;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.ResourceBundle;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class ResetPasswordApi extends PublicApiSupport {
    public static final FreeMarker FREE_MARKER = new FreeMarker(com.borqs.server.pubapi.i18n.PackageClass.class);
    private AccountLogic account;
    private Maker<Mail> maker;
    private AsyncSender<Mail> mailSender;
    private AsyncSender<Message> smsSender;

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    private String serverHost;

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    private CibindLogic cibind;

    public Maker<Mail> getMaker() {
        return maker;
    }

    public void setMaker(Maker<Mail> maker) {
        this.maker = maker;
    }

    public AsyncSender<Mail> getMailSender() {
        return mailSender;
    }

    public void setMailSender(AsyncSender<Mail> mailSender) {
        this.mailSender = mailSender;
    }

    public AsyncSender<Message> getSmsSender() {
        return smsSender;
    }

    public void setSmsSender(AsyncSender<Message> smsSender) {
        this.smsSender = smsSender;
    }

    public ResetPasswordApi() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    @Route(url = "/reset_password/email/send")
    public void sendResetPasswordEmail(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        String name = req.checkString("name", "login_name");
        long userId = 0;
        if (StringValidator.validateEmail(name) || StringValidator.validatePhone(name)) {
            userId = cibind.whoBinding(ctx, name);
        }
        else {
            try {
                userId = Long.parseLong(name);
            } catch (Exception e) {
                throw new ServerException(E.PARAM, "Invalid login name");
            }
        }
        
        String[] emails = cibind.getBindings(ctx, userId, BindingInfo.EMAIL);
        if (ArrayUtils.isEmpty(emails))
            throw new ServerException(E.EMAIL, "Do not have any bind email");

        if (StringUtils.contains(name, ".")) {
            name = StringUtils.replace(name, ".", "borqsdotborqs");
        }
        if (StringUtils.contains(name, "@")) {
            name = StringUtils.replace(name, "@", "borqsatborqs");
        }

        String key = Encoders.desEncryptBase64(name + "/" + DateHelper.nowMillis());
        String url = "http://" + serverHost + "/v2/reset_password/email/deal?key=" + key;

        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);
        String subject = bundle.getString("resetpwdapi.reset.password.confirm.subject");
        
        String template = bundle.getString("resetpwdapi.reset.password.confirm");
        String message = FREE_MARKER.mergeRaw(template, new Object[][]{
                {"resetPwdUrl", url}
        });
        
        for (String email : emails) {
            Mail mail = maker.make(ctx, MakerTemplates.EMAIL_ESSENTIAL, new Object[][] {
                    {"from", mailSender instanceof AsyncMailSender ? ((AsyncMailSender)mailSender).getSmtpUsername() : ""},
                    {"to", email},
                    {"subject", subject},
                    {"serverHost", serverHost},
                    {"content", message}
            });
            mailSender.asyncSend(mail);
        }

        resp.body(true);
    }

    @Route(url = "/reset_password/email/deal")
    public void dealResetPasswordEmail(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        String key = req.checkString("key");

        key = StringUtils.replace(key, " ", "+");
        long validPeriod = 3L * 24 * 60 * 60 * 1000; //email valid period: 3days
        String info = Encoders.desDecryptFromBase64(key);
        String name = StringUtils.substringBefore(info, "/");
        long valid = Long.parseLong(StringUtils.substringAfter(info, "/"));
        if (valid < (DateHelper.nowMillis() - validPeriod))
            throw new ServerException(E.PARAM, "The link is expired");

        if (StringUtils.contains(name, "borqsdotborqs")) {
            name = StringUtils.replace(name, "borqsdotborqs", ".");
        }
        if (StringUtils.contains(name, "borqsatborqs")) {
            name = StringUtils.replace(name, "borqsatborqs", "@");
        }

        long userId = 0;
        if (StringValidator.validateEmail(name) || StringValidator.validatePhone(name)) {
            userId = cibind.whoBinding(ctx, name);
        }
        else {
            try {
                userId = Long.parseLong(name);
            } catch (Exception e) {
                throw new ServerException(E.PARAM, "Invalid login name");
            }
        }

        ctx.setViewer(userId);
        String newPassword = RandomHelper.generateRandomNumberString(6);
        account.updatePassword(ctx, "", newPassword, false);

        String[] emails = cibind.getBindings(ctx, userId, BindingInfo.EMAIL);
        if (ArrayUtils.isEmpty(emails))
            throw new ServerException(E.EMAIL, "Do not have any bind email");

        String locale = ctx.getUserAgent().getLocale();
        ResourceBundle bundle = I18nHelper.getBundle("com/borqs/server/pubapi/i18n/pubapi", locale);
        String subject = bundle.getString("resetpwdapi.reset.password.message.subject");

        String template = bundle.getString("resetpwdapi.reset.password.message");
        String message = FREE_MARKER.mergeRaw(template, new Object[][]{
                {"loginName", name},
                {"newPwd", newPassword}
        });

        for (String email : emails) {
            Mail mail = maker.make(ctx, MakerTemplates.EMAIL_ESSENTIAL, new Object[][] {
                    {"from", mailSender instanceof AsyncMailSender ? ((AsyncMailSender)mailSender).getSmtpUsername() : ""},
                    {"to", email},
                    {"subject", subject},
                    {"serverHost", serverHost},
                    {"content", message}
            });
            mailSender.asyncSend(mail);
        }

        String notice = bundle.getString("resetpwdapi.reset.password.notice");
        String html = FREE_MARKER.merge("notice.ftl", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });
        resp.type("text/html");
        resp.charset("UTF-8");
        resp.body(RawText.of(html));
    }
}
