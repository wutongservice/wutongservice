package com.borqs.server.platform.mq.receiver;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SimpleConnectionFactory;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

public class CreateAccountsFromExcel {
    private static final Logger L = LoggerFactory.getLogger(CreateAccountsFromExcel.class);
    private static final ConnectionFactory CONNECTION_FACTORY = new SimpleConnectionFactory();
    private static final String ALIYUN_ACCOUNT_DB = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";
//    private static final String ALIYUN_ACCOUNT_DB = "mysql/127.0.0.1/accounts/root/111111";

    public static ConnectionFactory getConnectionFactory() {
        return CONNECTION_FACTORY;
    }

    public static boolean sendCreateAccountEmail(Platform p, String name, String email, boolean isEn) throws AvroRemoteException {
        String zhSubject = "欢迎使用梧桐了解播思动态";

        String enSubject = "Welcome to use wutong to obtain Borqs time line";

        String zhMsg = "尊敬的" + name + "：<br>"
                     + "&nbsp;&nbsp;&nbsp;&nbsp;您的梧桐帐号已创建，请使用您的播思邮箱：" + email + "登录梧桐，初始密码为123456，请您登录后尽快修改您的密码。<br>"
                     + "&nbsp;&nbsp;&nbsp;&nbsp;如果您之前已经注册过梧桐帐号，并且绑定了播思邮箱，您可以使用原来的帐号和密码进行登录。<br>"
                     + "&nbsp;&nbsp;&nbsp;&nbsp;欢迎您使用梧桐，了解播思和好友的动态，希望您提供宝贵意见和建议。<br>"
                     + "&nbsp;&nbsp;&nbsp;&nbsp;下载梧桐客户端，请访问<a href=http://apps.borqs.com target=_blank>http://apps.borqs.com</a>。";

        String enMsg = "Dear " + name + ",<br>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;Your wutong account has been created, please use your Borqs email " + email + "to login wutong. Initial password is 123456, please modify your password as soon.<br>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;If you have Registered, and have bind Borqs email, you can use that account and password to login.<br>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;Welcome to use wutong to obtain Borqs time line and give us proposal.<br>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;Download wutong android client, <a href=http://apps.borqs.com target=_blank>http://apps.borqs.com</a>.";
        
        String subject = isEn ? enSubject : zhSubject;
        String message = isEn ? enMsg :zhMsg;
        String lang = isEn ? "en" : "zh";
        return p.sendEmail(subject, email, email, message, Constants.EMAIL_ESSENTIAL, lang);
    }
    
    public static void main(String[] args) throws IOException, BiffException {
        GenericTransceiverFactory tf = new GenericTransceiverFactory();
        String confPath = "/home/wutong/work2/dist/etc/test_web_server.properties";
//        String confPath = "/home/b516/BorqsServerPlatform2/distribution/src/main/etc/test_web_server.properties";
        if ((args != null) && (args.length > 0)) {
            confPath = args[0];
        }

        Configuration conf = Configuration.loadFiles(confPath).expandMacros();
        tf.setConfig(conf);
        tf.init();
        final Platform p = new Platform(tf);
        p.setConfig(conf);

//        String ef = "/home/wutong/Directory.xls";
//        createAccounts(p, ef, "BJ");
//        createAccounts(p, ef, "BJIntern");
//        createAccounts(p, ef, "WH");
//        createAccounts(p, ef, "India");

//        Record roles = new Record();
//        for (int i = 15294; i <= 15430; i++) {
//            roles.put(String.valueOf(i), Constants.ROLE_MEMBER);
//        }
//        p.addMembers(10000000076L, roles, false);

/*
        Record roles = new Record();
        try {
            String userId = p.createAccountWithoutNotif("ischo@borqs.com", "", "E10ADC3949BA59ABBE56E057F20F883E", "I.S.Cho", "I.S.Cho", "u", "", "");
            roles.put(userId, Constants.ROLE_MEMBER);
            System.out.println("Create Account: " + "I.S.Cho" + " - " + "ischo@borqs.com" + " Success   BorqsId: " + userId);
        } catch (Exception e) {
            System.out.println("Create Account: " + "I.S.Cho" + " - " + "ischo@borqs.com" + " Failed");
        }
        try {
            String userId = p.createAccountWithoutNotif("kelvin.leews@yahoo.com", "", "E10ADC3949BA59ABBE56E057F20F883E", "W.S. Lee", "W.S. Lee", "u", "", "");
            roles.put(userId, Constants.ROLE_MEMBER);
            System.out.println("Create Account: " + "W.S. Lee" + " - " + "kelvin.leews@yahoo.com" + " Success   BorqsId: " + userId);
        } catch (Exception e) {
            System.out.println("Create Account: " + "W.S. Lee" + " - " + "kelvin.leews@yahoo.com" + " Failed");
        }
        try {
            String userId = p.createAccountWithoutNotif("yjko0430@yahoo.com", "", "E10ADC3949BA59ABBE56E057F20F883E", "Y.J. Ko", "Y.J. Ko", "u", "", "");
            roles.put(userId, Constants.ROLE_MEMBER);
            System.out.println("Create Account: " + "Y.J. Ko" + " - " + "yjko0430@yahoo.com" + " Success   BorqsId: " + userId);
        } catch (Exception e) {
            System.out.println("Create Account: " + "Y.J. Ko" + " - " + "yjko0430@yahoo.com" + " Failed");
        }
        try {
            String userId = p.createAccountWithoutNotif("baul97@gmail.com", "", "E10ADC3949BA59ABBE56E057F20F883E", "Y.H. Kim", "Y.H. Kim", "u", "", "");
            roles.put(userId, Constants.ROLE_MEMBER);
            System.out.println("Create Account: " + "Y.H. Kim" + " - " + "baul97@gmail.com" + " Success   BorqsId: " + userId);
        } catch (Exception e) {
            System.out.println("Create Account: " + "Y.H. Kim" + " - " + "baul97@gmail.com" + " Failed");
        }

        p.addMembers(14000000000L, roles, false);
 */
/*
        String ef = "/home/wutong/Directory_Phone.xls";
        bindPhonesFromExcel(p, ef, "BJ");
 */

/*
        String ef = "/home/wutong/old_new_ids.xls";
//        String ef = "/home/b516/Downloads/old_new_ids.xls";
        mergeAccounts(p, ef, "idmap");
        */
/*
        Record roles = new Record();
        try {
            String userId = p.createAccountWithoutNotif("yingxiong.zhu@borqs.com", "", "E10ADC3949BA59ABBE56E057F20F883E", "朱应雄", "朱应雄", "u", "", "");
            roles.put(userId, Constants.ROLE_MEMBER);
            System.out.println("Create Account: " + "朱应雄" + " - " + "yingxiong.zhu@borqs.com" + " Success   BorqsId: " + userId);
        } catch (Exception e) {
            System.out.println("Create Account: " + "朱应雄" + " - " + "yingxiong.zhu@borqs.com" + " Failed");
        }
        try {
            String userId = p.createAccountWithoutNotif("lei.liu@borqs.com", "", "E10ADC3949BA59ABBE56E057F20F883E", "刘磊", "刘磊", "u", "", "");
            roles.put(userId, Constants.ROLE_MEMBER);
            System.out.println("Create Account: " + "刘磊" + " - " + "lei.liu@borqs.com" + " Success   BorqsId: " + userId);
        } catch (Exception e) {
            System.out.println("Create Account: " + "刘磊" + " - " + "lei.liu@borqs.com" + " Failed");
        }

        p.addMembers(10000000072L, roles, false);
        p.addMembers(10000000078L, roles, false);
        p.addMembers(14000000000L, roles, false);
        p.addMembers(14000000003L, roles, false);
 */

        Record roles = new Record();
        try {
            String userId = p.createAccountWithoutNotif("amitabh.m@borqs.com", "", "E10ADC3949BA59ABBE56E057F20F883E", "Amitabh", "Amitabh", "u", "", "");
            roles.put(userId, Constants.ROLE_MEMBER);
            System.out.println("Create Account: " + "Amitabh" + " - " + "amitabh.m@borqs.com" + " Success   BorqsId: " + userId);
        } catch (Exception e) {
            System.out.println("Create Account: " + "Amitabh" + " - " + "amitabh.m@borqs.com" + " Failed");
        }

        p.addMembers(14000000000L, roles, false);
    }

    public static void createAccounts(Platform p, String ef, String sn) throws IOException, BiffException {
        InputStream is = new FileInputStream(new File(ef));
        Workbook wb = Workbook.getWorkbook(is);
        Sheet sheet = wb.getSheet(sn);
        int rowCount = sheet.getRows();
        for (int i = 0; i < rowCount; i++) {
            Cell[] row = sheet.getRow(i);
            String name = row[1].getContents();
            String email = row[2].getContents();
            try {
                String userId = p.createAccountWithoutNotif(email, "", "E10ADC3949BA59ABBE56E057F20F883E", name, name, "u", "", "");
                L.trace("Create Account: " + name + " - " + email + " Success   BorqsId: " + userId);
                System.out.println("Create Account: " + name + " - " + email + " Success   BorqsId: " + userId);
            } catch (Exception e) {
                //update display name
                String userId = p.findUserIdByUserName(email);
                boolean b = p.updateAccount(userId, Record.of("display_name", name), "zh", false);
                if (b) {
                    L.trace("Update Account: " + name + " - " + email + " Success   BorqsId: " + userId);
                    System.out.println("Update Account: " + name + " - " + email + " Success   BorqsId: " + userId);
                } else {
                    L.trace("Create or Update Account: " + name + " - " + email + " Failed");
                    System.out.println("Create or Update Account: " + name + " - " + email + " Failed");
                }
                continue;
            }
        }
    }
    
    public static void bindPhonesFromExcel(Platform p, String ef, String sn) throws IOException, BiffException {
        InputStream is = new FileInputStream(new File(ef));
        Workbook wb = Workbook.getWorkbook(is);
        Sheet sheet = wb.getSheet(sn);
        int rowCount = sheet.getRows();
        for (int i = 0; i < rowCount; i++) {
            Cell[] row = sheet.getRow(i);
            String phone = row[5].getContents();
//            String email = row[7].getContents();

        /*
            String targetUserId = p.findUserIdByUserName(email);
            String oldUserId = p.findUserIdByUserName(phone);
            if (StringUtils.isNotBlank(oldUserId) && !StringUtils.equals(oldUserId, "0")) {
                try {
                    String sql = "UPDATE user2 SET login_phone1='', login_phone2='', login_phone3='' WHERE user_id=" + oldUserId;
                    SQLExecutor.executeUpdate(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql);
                    System.out.println("Unbind phone Success: " + oldUserId);
                } catch (Exception e) {
                    System.out.println("Unbind phone Failed: " + oldUserId);
                    continue;
                }
                try {
                    String sql = "UPDATE user2 SET login_phone1='" + phone + "' WHERE user_id=" + targetUserId;
                    SQLExecutor.executeUpdate(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql);
                    System.out.println("Bind phone Success: " + targetUserId);
                } catch (Exception e) {
                    System.out.println("Bind phone Failed: " + targetUserId);
                    continue;
                }
                
                System.out.println("Finished " + oldUserId + " -> " + targetUserId);
            }
            */

            String userId = p.findUserIdByUserName(phone);
            if (StringUtils.isNotBlank(userId) && !StringUtils.equals(userId, "0")) {
                Record r = p.getUser(userId, userId, "contact_info", false);
                Record contactInfo = new Record();
                String c = r.getString("contact_info");
                if (StringUtils.isNotBlank(c))
                    contactInfo = Record.fromJson(c);
                String mobile = contactInfo.getString("mobile_telephone_number", "");
                if (StringUtils.isBlank(mobile))
                    contactInfo.put("mobile_telephone_number", phone);
                else if (!StringUtils.equals(phone, mobile)) {
                    mobile = contactInfo.getString("mobile_2_telephone_number", "");
                    if (StringUtils.isBlank(mobile))
                        contactInfo.put("mobile_2_telephone_number", phone);
                    else if (!StringUtils.equals(phone, mobile)) {
                        contactInfo.put("mobile_3_telephone_number", phone);
                    }
                }

                try {
                    boolean b = p.updateAccount(userId, Record.of("contact_info", contactInfo));
                    if (b)
                        System.out.println("Update contact information success:" + userId);
                    else
                        System.out.println("Update contact information failed:" + userId);
                } catch (Exception e) {
                    System.out.println("Update contact information failed:" + userId);
                    continue;
                }
            }
        }
    }

    public static void mergeAccounts(Platform p, String ef, String sn) throws IOException, BiffException {
        InputStream is = new FileInputStream(new File(ef));
        Workbook wb = Workbook.getWorkbook(is);
        Sheet sheet = wb.getSheet(sn);
        int rowCount = sheet.getRows();
        for (int i = 0; i < rowCount; i++) {
            Cell[] row = sheet.getRow(i);
            String userId1 = row[0].getContents();
            String userId2 = row[1].getContents();

            try {
                String sql = "SELECT count(*) FROM stream where source=" + userId1;
                long count1 = SQLExecutor.executeIntScalar(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, 0);
                sql = "SELECT count(*) FROM stream where source=" + userId2;
                long count2 = SQLExecutor.executeIntScalar(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, 0);
                if (count2 > count1) {
                    continue;
                } else {
                    ArrayList<String> sqls = new ArrayList<String>();

                    sql = "SELECT login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3 FROM user2 WHERE user_id=" + userId1;
                    Record rec = SQLExecutor.executeRecord(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, null);
                    String loginEmail1_1 = rec.getString("login_email1", "");
                    String loginEmail1_2 = rec.getString("login_email2", "");
                    String loginEmail1_3 = rec.getString("login_email3", "");
                    String loginPhone1_1 = rec.getString("login_phone1", "");
                    String loginPhone1_2 = rec.getString("login_phone2", "");
                    String loginPhone1_3 = rec.getString("login_phone3", "");

                    String emailCol = "login_email1";
                    String phoneCol = "login_phone1";
                    if (StringUtils.isBlank(loginEmail1_1))
                        emailCol = "login_email1";
                    else if (StringUtils.isBlank(loginEmail1_2))
                        emailCol = "login_email2";
                    else if (StringUtils.isBlank(loginEmail1_3))
                        emailCol = "login_email3";

                    if (StringUtils.isBlank(loginPhone1_1))
                        phoneCol = "login_phone1";
                    else if (StringUtils.isBlank(loginPhone1_2))
                        phoneCol = "login_phone2";
                    else if (StringUtils.isBlank(loginPhone1_3))
                        phoneCol = "login_phone3";
                    
                    sql = "SELECT login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3 FROM user2 WHERE user_id=" + userId2;
                    rec = SQLExecutor.executeRecord(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, null);
                    String loginEmail2_1 = rec.getString("login_email1", "");
                    String loginEmail2_2 = rec.getString("login_email2", "");
                    String loginEmail2_3 = rec.getString("login_email3", "");
                    String loginPhone = rec.getString("login_phone1", "");
                    
                    String loginEmail = loginEmail2_1;
                    if (StringUtils.endsWith(loginEmail2_1, "borqs.com"))
                        loginEmail = loginEmail2_1;
                    else if (StringUtils.endsWith(loginEmail2_2, "borqs.com"))
                        loginEmail = loginEmail2_2;
                    else if (StringUtils.endsWith(loginEmail2_3, "borqs.com"))
                        loginEmail = loginEmail2_3;

                    sqls.add("UPDATE user2 SET login_email1='', login_email2='', login_email3='', login_phone1='', login_phone2='', login_phone3='' WHERE user_id=" + userId2);
                    sqls.add("UPDATE user2 SET " + emailCol + "='" + loginEmail + "', " + phoneCol + "='" + loginPhone + "' WHERE user_id=" + userId1);
                    sqls.add("UPDATE group_members SET member=" + userId1 + " WHERE group_id IN (10000000072, 10000000078, 14000000000) AND member=" + userId2);
                    sqls.add("UPDATE user2 SET destroyed_time=" + DateUtils.nowMillis() + " WHERE user_id=" + userId2);
                    SQLExecutor.executeUpdate(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sqls);

                    Record r = p.getUser(userId1, userId1, "contact_info", false);
                    Record contactInfo = new Record();
                    String c = r.getString("contact_info");
                    if (StringUtils.isNotBlank(c))
                        contactInfo = Record.fromJson(c);
                    String email = contactInfo.getString("email_address", "");
                    if (StringUtils.isBlank(email))
                        contactInfo.put("email_address", loginEmail);
                    else if (!StringUtils.equals(loginEmail, email)) {
                        email = contactInfo.getString("email_2_address", "");
                        if (StringUtils.isBlank(email))
                            contactInfo.put("email_2_address", loginEmail);
                        else if (!StringUtils.equals(loginEmail, email)) {
                            contactInfo.put("email_3_address", loginEmail);
                        }
                    }

                    p.updateAccount(userId1, Record.of("contact_info", contactInfo));

                    System.out.println("Success: " + userId1 + " - " + userId2);
                    L.trace("Success: " + userId1 + " - " + userId2);
                }
            } catch (Exception e) {
                System.out.println("Failed: " + userId1 + " - " + userId2);
                L.trace("Failed: " + userId1 + " - " + userId2);
                continue;
            }
        }
    }
}
