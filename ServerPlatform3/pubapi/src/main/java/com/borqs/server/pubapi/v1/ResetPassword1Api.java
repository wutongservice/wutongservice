package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.maker.Maker;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.*;
import com.borqs.server.platform.util.sender.AsyncSender;
import com.borqs.server.platform.util.sender.email.AsyncMailSender;
import com.borqs.server.platform.util.sender.email.Mail;
import com.borqs.server.platform.util.sender.sms.Message;
import com.borqs.server.platform.util.template.FreeMarker;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;

@IgnoreDocument
public class ResetPassword1Api extends PublicApiSupport {
    private static final Logger L = Logger.get(ResetPassword1Api.class);
    public static final FreeMarker FREE_MARKER = new FreeMarker(com.borqs.server.pubapi.i18n.PackageClass.class);
    private AccountLogic account;
    private Maker<Mail> maker;
    private AsyncSender<Mail> mailSender;
    private AsyncSender<Message> smsSender;

    public boolean isSyncBbsPwd() {
        return syncBbsPwd;
    }

    public void setSyncBbsPwd(boolean syncBbsPwd) {
        this.syncBbsPwd = syncBbsPwd;
    }

    private boolean syncBbsPwd;

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

    public ResetPassword1Api() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    @Route(url = "/account/reset_password")
    public void resetPassword(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        String name = req.checkString("login_name");
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

    @Route(url = "/account/reset_password_for_phone")
    public void resetPasswordForPhone(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        String phone = req.checkString("phone");
        long userId = 0;
        if (StringValidator.validatePhone(phone))
            userId = cibind.whoBinding(ctx, phone);
        if (userId == 0)
            throw new ServerException(E.INVALID_USER, "User '%s' is not exists", phone);

        ctx.setViewer(userId);
        String newPassword = RandomHelper.generateRandomNumberString(6);
        account.updatePassword(ctx, "", newPassword, false);

        String md5NewPwd = Encoders.md5Hex(newPassword);
        User user = account.getUser(ctx, new String[]{"password"}, userId);
        String md5OldPwd = user.getPassword();

        syncBorqsBbsPwd(ctx, phone, md5OldPwd, md5NewPwd);
        sendNewPasswordToPhone(phone, newPassword);

        resp.body("OK");
    }

    private boolean syncBorqsBbsPwd(Context ctx, String phone, String md5OldPwd, String md5NewPwd) {
        if (syncBbsPwd) {
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
                L.warn(ctx, e, "syncBorqsBbsPwd error");
                return false;
            }
        } else {
            return false;
        }
    }

    void sendNewPasswordToPhone(String phone, String newPwd) {
        String text = "您的密码已经重置成功，新密码是 " + newPwd + "。";
        try {
            smsSender.asyncSend(Message.forSend(phone, text));
        } catch (Exception e) {
            throw new ServerException(E.IO, "Send sms error");
        }
    }
}
