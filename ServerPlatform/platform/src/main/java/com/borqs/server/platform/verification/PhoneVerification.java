package com.borqs.server.platform.verification;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.Sql;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.platform.ErrorCode;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static com.borqs.server.base.sql.Sql.value;

public class PhoneVerification extends ConfigurableBase implements Initializable {

    private static Logger L = LoggerFactory.getLogger(PhoneVerification.class);

    public static final int OK = 0;
    public static final int REQUEST_TOO_FREQUENT = 1;
    public static final int VERIFICATION_ERROR = 2;
    public static final int VERIFY_TOO_FREQUENT = 3;

    private static final int MAX_VERIFICATION_COUNT = 5;
    private static final long VERIFICATION_TIMEOUT = (1 + 3 + 30) * 60L * 1000;


    private ConnectionFactory connectionFactory;
    private String db;
    private String table = "phone_verification";
    private String smsHost = null;


    public PhoneVerification() {
    }

    @Override
    public void init() {
        Configuration conf = getConfig();
        connectionFactory = ConnectionFactory.getConnectionFactory("dbcp");
        db = conf.getString("phoneVerification.simple.db", null);
        smsHost = conf.getString("phoneVerification.smsHost", null);
    }

    @Override
    public void destroy() {
        smsHost = null;
        db = null;
        connectionFactory = ConnectionFactory.close(connectionFactory);
    }

    public static String normalizePhone(String phone) {
        return StringUtils.removeStart(phone, "+86");
    }

    public int getNextRequestSpan(String phone) {
        Sql sql = new Sql().select("*").from(table).where("phone=:phone", "phone", phone);
        Record rec = SQLExecutor.executeRecord(connectionFactory, db, sql.toString(), null);
        if (rec == null || rec.isEmpty())
            return 0;

        long ms = calcNextRequestSpan(rec.checkGetInt("created_time"), (int)rec.checkGetInt("send_count"));
        return ms > 0 ? (int)((double)ms / 1000) + 2 : 0;
    }

    public int request(String phone) {
        long now = DateUtils.nowMillis();
        Sql sql = new Sql().select("*").from(table).where("phone=:phone", "phone", phone);
        Record rec = SQLExecutor.executeRecord(connectionFactory, db, sql.toString(), null);
        if (rec == null || rec.isEmpty()) {
            String code = genCode();
            Sql sql2 = new Sql().insertInto(table).values(
                    value("phone", phone),
                    value("created_time", now),
                    value("send_count", 1),
                    value("verification_count", 0),
                    value("code", code)
            );

            SQLExecutor.executeUpdate(connectionFactory, db, sql2.toString());
            sendCodeSms(phone, code);
            L.trace("Send code to phone " + phone);
            return OK;
        } else {
            int count = (int) rec.getInt("send_count");
            long expiry = calcExpiry(rec.getInt("created_time"), count + 1);
            if (now < expiry) {
                L.trace("phone " + phone + " send request too many");
                return REQUEST_TOO_FREQUENT;
            }

            Sql sql2;
            if (count >= 3 || now - rec.getInt("created_time")  > VERIFICATION_TIMEOUT) {
                sql2 = new Sql().deleteFrom(table).where("phone=:phone", "phone", phone);
                SQLExecutor.executeUpdate(connectionFactory, db, sql2.toString());
                L.trace("phone" + phone + " over 3 times so resend");
                return request(phone);
            } else {
                sendCodeSms(phone, rec.getString("code"));
                sql2 = new Sql().update(table).setValues(
                        value("send_count", count + 1)
                ).where("phone=:phone", "phone", phone);
                SQLExecutor.executeUpdate(connectionFactory, db, sql2.toString());
                L.trace("phone " + phone + " send request count:" + count + 1);
            }

            return OK;
        }
    }

    public int feedback(String phone, String code) {
        phone = normalizePhone(phone);
        Sql sql = new Sql().select("*").from(table).where("phone=:phone", "phone", phone);
        Record rec = SQLExecutor.executeRecord(connectionFactory, db, sql.toString(), null);
        if (rec == null || rec.isEmpty()) {
            L.trace("phone " + phone + " verify error");
            return VERIFICATION_ERROR;
        } else {
            if (StringUtils.equals(code, rec.getString("code"))) {
                long now = DateUtils.nowMillis();
                if (now - rec.getInt("created_time") < VERIFICATION_TIMEOUT) {
                    L.trace("phone " + phone + " verify ok");
                    return OK;
                } else {
                    // timeout
                    Sql sql2 = new Sql().deleteFrom(table).where("phone=:phone", "phone", phone);
                    SQLExecutor.executeUpdate(connectionFactory, db, sql2.toString());
                    L.trace("phone " + phone + " verify timeout");
                    return VERIFICATION_ERROR;
                }
            } else {
                int verificationCount = (int) rec.getInt("verification_count");
                if (verificationCount >= MAX_VERIFICATION_COUNT) {
                    L.trace("phone " + phone + " verify error (too many)");
                    return VERIFY_TOO_FREQUENT;
                }

                Sql sql2 = new Sql().update(table).setValues(
                        value("verification_count", verificationCount + 1)
                ).where("phone=:phone", "phone", phone);
                SQLExecutor.executeUpdate(connectionFactory, db, sql2.toString());
                L.trace("phone " + phone + " verify error count: " + verificationCount + 1);
                return VERIFICATION_ERROR;
            }
        }
    }


    private void sendSms(String phone, String text) {
        if (smsHost == null)
            throw new ServerException(ErrorCode.SEND_SMS_ERROR, "Send sms error");

        try {
            HttpClient client = new DefaultHttpClient();
            //HttpPost httpPost = new HttpPost("http://" + smsHost + "/smsgw/sendsms.php");
            HttpPost httpPost = new HttpPost(smsHost);
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("appname", "qiupu"));
            params.add(new BasicNameValuePair("data", String.format("{\"to\":\"%s\",\"subject\":\"%s\"}", phone, StringEscapeUtils.escapeJavaScript(text))));
            //params.add(new BasicNameValuePair("sendto", phone));
            //params.add(new BasicNameValuePair("content", text));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            client.execute(httpPost);
        } catch (IOException e) {
            throw new ServerException(ErrorCode.SEND_SMS_ERROR, "Send sms error");
        }
    }

    private void sendCodeSms(String phone, String code) {
        final String template = "您于%s使用播思通行证，激活码是%s，有效期是30分钟，请尽快操作。如非本人操作，请忽略。";
        String s = String.format(template, DateUtils.formatDateCh(DateUtils.nowMillis()), code);
        sendSms(phone, s);
    }

    private static String genCode() {
        return RandomUtils.generateRandomNumberString(4);
    }

    private static long calcExpiry(long createdTime, int count) {
        if (count == 1)
            return createdTime;
        else if (count == 2)
            return createdTime + 1 * 60L * 1000;
        else if (count == 3)
            return createdTime + (1 + 3) * 60L * 1000;
        else if (count > 3)
            return createdTime + (1 + 3 + 30) * 60L * 1000;
        else
            throw new IllegalArgumentException();
    }

    private static long calcNextRequestSpan(long createdTime, int count) {
        long r = calcExpiry(createdTime, count + 1) - DateUtils.nowMillis();
        return r >= 0 ? r : 0;
    }
}
