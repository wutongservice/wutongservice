package com.borqs.server.wutong.email;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;

import java.io.Serializable;
import java.util.Map;

public class EmailModel implements Serializable {

    private String title;
    private String to;
    private String username;
    private String content;
    private String emailType;
    private Record record;
    private Map<String,Object> map;

    private String sendEmailName;
    private String sendEmailPassword;
    private transient Configuration conf;


    public static String DEFAULT_SEND_EMAILNAME;
    public static String DEFAULT_SEND_EMAILPASSWORD;
    public static String ELEARNING_SEND_EMAILNAME;
    public static String ELEARNING_SEND_EMAILPASSWORD;
    public static String INNOV_SEND_EMAILNAME;
    public static String INNOV_SEND_EMAILPASSWORD;
    public static String SERVER_HOST;



    public EmailModel(Configuration conf) {
        this.conf = conf;
        DEFAULT_SEND_EMAILNAME = conf.getString("DEFAULT_SEND_EMAILNAME", "borqs.support@borqs.com");
        DEFAULT_SEND_EMAILPASSWORD = conf.getString("DEFAULT_SEND_EMAILPASSWORD", "borqsbpc");
        ELEARNING_SEND_EMAILNAME = conf.getString("ELEARNING_SEND_EMAILNAME", "e_learning@borqs.com");
        ELEARNING_SEND_EMAILPASSWORD = conf.getString("ELEARNING_SEND_EMAILPASSWORD", "Borqs.com");
        INNOV_SEND_EMAILNAME = conf.getString("INNOV_SEND_EMAILNAME", "innovation@borqs.com");
        INNOV_SEND_EMAILPASSWORD = conf.getString("INNOV_SEND_EMAILPASSWORD", "innovation");
        SERVER_HOST = conf.getString("server.host", "api.borqs.com");
    }

    public static EmailModel getDefaultEmailModule(Configuration conf) {
        EmailModel email = new EmailModel(conf);
        email.setSendEmailName(DEFAULT_SEND_EMAILNAME);
        email.setSendEmailPassword(DEFAULT_SEND_EMAILPASSWORD);
        return email;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSendEmailName() {
        return sendEmailName;
    }

    public void setSendEmailName(String sendEmailName) {
        this.sendEmailName = sendEmailName;
    }

    public String getSendEmailPassword() {
        return sendEmailPassword;
    }

    public void setSendEmailPassword(String sendEmailPassword) {
        this.sendEmailPassword = sendEmailPassword;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }
    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }
}
