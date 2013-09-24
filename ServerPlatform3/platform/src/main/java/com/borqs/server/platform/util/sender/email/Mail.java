package com.borqs.server.platform.util.sender.email;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.RW;
import com.borqs.server.platform.io.Writable;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.NetAddress;
import com.borqs.server.platform.util.StringHelper;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.commons.lang.StringUtils;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class Mail implements Writable {

    private String to;
    private String cc;
    private String bcc;
    private String from;
    private String subject;
    private String message;
    private String customer;
    private String smtp;
    private String username;
    private String password;
    private boolean html = true;

    public Mail() {
    }

    public String getTo() {
        return to;
    }

    public Mail setTo(String to) {
        this.to = to;
        return this;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getBcc() {
        return bcc;
    }

    public void setBcc(String bcc) {
        this.bcc = bcc;
    }

    public String getFrom() {
        return from;
    }

    public Mail setFrom(String from) {
        this.from = from;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public Mail setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Mail setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getCustomer() {
        return customer;
    }

    public Mail setCustomer(String customer) {
        this.customer = customer;
        return this;
    }

    public String getSmtp() {
        return smtp;
    }

    public Mail setSmtp(String smtp) {
        this.smtp = smtp;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public Mail setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Mail setPassword(String password) {
        this.password = password;
        return this;
    }

    public boolean isHtml() {
        return html;
    }

    public Mail setHtml(boolean html) {
        this.html = html;
        return this;
    }

    public Mail setSmtpServer(String address, String username, String password) {
        setSmtp(address);
        setUsername(username);
        setPassword(password);
        return this;
    }

    public static Mail plain(String from, String to, String subject, String message) {
        return new Mail().setHtml(false).setFrom(from).setTo(to).setSubject(subject).setMessage(message);
    }

    public static Mail html(String from, String to, String subject, String message) {
        return new Mail().setHtml(true).setFrom(from).setTo(to).setSubject(subject).setMessage(message);
    }

    public void syncSend() {
        final String PLAIN_TYPE = "text/plain;charset=utf-8";
        final String HTML_TYPE = "text/html;charset=utf-8";

        NetAddress server = NetAddress.parse(getSmtp());
        try {
            Properties props = System.getProperties();
            props.put("mail.smtp.starttls.enable", true);
            props.put("mail.smtp.host", server.host);
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp", server.host);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", Integer.toString(server.port));
            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.socketFactory.port", server.host); // TODO: ?HOST?
            props.put("mail.smtp.starttls.required", true);

            Session session = Session.getInstance(props, new SmtpAuth(getUsername(), getPassword()));
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(getFrom()));
            msg.setSubject(getSubject());
            msg.setHeader("X-Mailer", new UID().toString());
            msg.setSentDate(DateHelper.now());

            if (StringUtils.isNotBlank(getTo()))
                msg.setRecipients(Message.RecipientType.TO, parseAddress(getTo()));

            if (StringUtils.isNotBlank(getCc()))
                msg.setRecipients(Message.RecipientType.CC, parseAddress(getCc()));

            if (StringUtils.isNotBlank(getBcc()))
                msg.setRecipients(Message.RecipientType.BCC, parseAddress(getBcc()));

            String text = getMessage();
            if (getCustomer() != null)
                text = String.format("From: %s\n%s", getCustomer(), text);

            msg.setContent(text, isHtml() ? HTML_TYPE : PLAIN_TYPE);

            Transport.send(msg);
        } catch (Exception e) {
            throw new ServerException(E.EMAIL, e);
        }
    }

    @Override
    public void write(Encoder out, boolean flush) throws IOException {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("to", to);
        m.put("cc", cc);
        m.put("bcc", bcc);
        m.put("from", from);
        m.put("subject", subject);
        m.put("message", message);
        m.put("customer", customer);
        m.put("smtp", smtp);
        m.put("username", username);
        m.put("password", password);
        RW.write(out, m, flush);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readIn(Decoder in) throws IOException {
        Map<String, Object> m = (Map) RW.read(in);
        to = (String) m.get("to");
        cc = (String) m.get("cc");
        bcc = (String) m.get("bcc");
        from = (String) m.get("from");
        subject = (String) m.get("subject");
        message = (String) m.get("message");
        customer = (String) m.get("customer");
        smtp = (String) m.get("smtp");
        username = (String) m.get("username");
        password = (String) m.get("password");
    }

    public void asyncSend(Executor exec, final Callback callback) {
        final Mail mail = this;
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (callback != null)
                        callback.before(mail);

                    mail.syncSend();

                    if (callback != null)
                        callback.after(mail);
                } catch (Throwable t) {
                    if (callback != null)
                        callback.except(mail, t);
                }
            }
        });
    }

    public void asyncSend(Executor exec) {
        asyncSend(exec, null);
    }

    private static InternetAddress[] parseAddress(String s) throws AddressException {
        ArrayList<InternetAddress> toAddrs = new ArrayList<InternetAddress>();
        for (String toAddr : StringHelper.splitList(s, ",", true))
            toAddrs.add(new InternetAddress(toAddr));
        return toAddrs.toArray(new InternetAddress[toAddrs.size()]);
    }

    private static class SmtpAuth extends Authenticator {
        final String username;
        final String password;

        public SmtpAuth(String username, String password) {
            this.username = username;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }
    }

    public static interface Callback {
        void before(Mail mail);

        void after(Mail mail);

        void except(Mail mail, Throwable t);
    }
}
