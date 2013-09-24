package com.borqs.server.test.pubapi.test1.register;


import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.util.FeedbackParams;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.email.Mail;
import com.borqs.server.platform.util.template.FreeMarker;
import com.borqs.server.pubapi.RegisterApi;
import com.borqs.server.pubapi.i18n.PackageClass;
import org.apache.commons.lang.StringUtils;

import java.util.Locale;
import java.util.ResourceBundle;

public class RegisterApiTest1 extends ServletTestCase {

    public static final FreeMarker FREE_MARKER = new FreeMarker(PackageClass.class);

    private RegisterApi getRegissterApi() {
        return (RegisterApi) getBean("pubapi.register");
    }

    public void testSendRegisterMail(){
        RegisterApi regissterApi = getRegissterApi();
        String login_email1 = "peng.wang@user.com";
        String pwd = "123456";
        String appId = "0";
        String displayName = "王鹏test";
        String gender = "m";
        String nickName = "nick_name";
        String imei = "11111";
        String imsi = "22222";

        
        ResourceBundle bundle = I18nHelper.getBundle("com/user/server/pubapi/i18n/pubapi", Locale.CHINA);
        String bundleString = bundle.getString("platformservlet.account.register.complete");

        String key = FeedbackParams.toSegmentedBase64(true, "/", login_email1, pwd, appId,
                displayName,nickName, gender, imei, imsi);
        String url = "http://" + regissterApi.getServerHost() + "/account/create?key=" + key;

        String notice = bundle.getString("platformservlet.account.register.dear") + displayName + bundle.getString("platformservlet.account.register.comleteNotice");
        
        String html = FREE_MARKER.merge("completeRegisterAccount.ftl", new Object[][]{
                {"host", "api.user.com"},
                {"notice", notice},
                {"url", url}
        });

        System.out.println("------------------------------"+html+"----------------------");


        regissterApi.getMailSender().asyncSend(Mail.html(regissterApi.getMailSender().getSmtpUsername(), "wangpeng<peng.wang@user.com>", bundleString, html));

    }

    public void testDealRegisterMail(){
        String key = "spRujNhd5Mo2E6u4DfVQHmNSsnrteS5nXsq2eiLb3811mR+DO1Q0qfT/1XIZfIOrba4wXMSM7eQtLI3amILJOw==";
        key = StringUtils.replace(key, " ", "+");
        String[] arr = FeedbackParams.fromSegmentedBase64(key, "/", 8);
        String login_name = arr[0];
        String pwd = arr[1];
        String appId = arr[2];
        String displayName = arr[3];
        String nickName = arr[4];
        String gender = arr[5];
        String imei = arr[6];
        String imsi = arr[7];
        String login_email1 = "";
        String login_phone1 = "";
        if (login_name.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
            login_email1 = login_name;
        } else {
            login_phone1 = login_name;
        }
        
        System.out.println("done!");

        ResourceBundle bundle = I18nHelper.getBundle("com/user/server/pubapi/i18n/pubapi", Locale.CHINA);
        String notice = bundle.getString("platformservlet.create.account.success");

        String html = FREE_MARKER.merge("notice.ftl", new Object[][]{
                {"host", "api.user.com"},
                {"notice", notice}
        });

        System.out.println(html);

    }

    @Override
    protected String[] getServletBeanIds() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return new DBSchemaBuilder.Script[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
