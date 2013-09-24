package com.borqs.server.wutong.email;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.log.Logger;
import com.borqs.server.wutong.GlobalLogics;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.rmi.server.UID;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

public class SendMail {
    private static Logger log = Logger.getLogger(SendMail.class);
    public static final String SMTP_BIZMAIL_YAHOO_COM = "smtp.bizmail.yahoo.com";
    public static final String SMTP_PORT = "465";



    private static boolean isHTML = true;


    private SendMail() {
    }


    private static boolean rawSend(Message msg) {
        try {
            System.out.println("--------------start------------------" + msg);
            Transport.send(msg);
            System.out.println("----------------end----------------" + msg);
            return true;
        } catch (MessagingException me) {
            log.info(new Context(), "send Email error!==" + me.getMessage());
            log.error(new Context(), me, " =======send email error!");
            return false;
        }
    }

    /*public boolean sendMessage(String message, String host, String port, String username, String password) {
        boolean ok = false;

        String uidString = "";
        try {
            // Set the email properties necessary to send email
            final Properties props = System.getProperties();

            props.put("mail.smtp.starttls.enable", true);
            props.put("mail.smtp.host", host);
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.server", host);
            props.put("mail.smtp.auth", "true");
            //props.put("mail.smtp.port", port);
            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.socketFactory.port", host);
            props.put("mail.smtp.starttls.required", true);
            Session sess = Session.getInstance(props, new MailAuthentication(username, password));


            Message msg = new MimeMessage(sess);

            StringTokenizer toST = new StringTokenizer(toField, ",");
            if (toST.countTokens() > 1) {
                InternetAddress[] address = new InternetAddress[toST.countTokens()];
                int addrIndex = 0;
                String addrString = "";
                while (toST.hasMoreTokens()) {
                    addrString = toST.nextToken();
                    address[addrIndex] = (new InternetAddress(addrString));
                    addrIndex = addrIndex + 1;
                }
                msg.setRecipients(Message.RecipientType.TO, address);
            } else {
                InternetAddress[] address = {new InternetAddress(toField)};
                msg.setRecipients(Message.RecipientType.TO, address);
            }

            InternetAddress from = new InternetAddress(myAddress);

            msg.setFrom(from);
            msg.setSubject(subjectField);

            UID msgUID = new UID();

            uidString = msgUID.toString();

            msg.setHeader("X-Mailer", uidString);

            msg.setSentDate(new Date());

            MimeMultipart mp = new MimeMultipart();

            // create body part for textarea
            MimeBodyPart mbp1 = new MimeBodyPart();

            if (getCustomerName() != null) {
                messageText = "From: " + getCustomerName() + "\n" + messageText;
            }

            if (isHTML) {
                mbp1.setContent(messageText, "text/html;charset=utf-8");
            } else {
                mbp1.setContent(messageText, "text/plain;charset=utf-8");
            }
            mp.addBodyPart(mbp1);


            if (!isHTML) {
                msg.setContent(messageText, "text/plain;charset=utf-8");
            } else {
                msg.setContent(messageText, "text/html;charset=utf-8");
            }
            for (int i = 0; i < 3; i++) {
                if (rawSend(msg)) {
                    ok = true;
                    break;
                } else {
                    log.info(null, "Email send error!=>> send " + i + " times" + msg.toString());
                    continue;
                }

                //TODO add failure email to the queue

            }

        } catch (Exception eq) {
            eq.printStackTrace();
            log.info(null, "Messaging Exception: " + eq.getMessage());
        }

        if (!ok) {
            log.info(null, "Could not connect to SMTP server.");
        }

        return ok;
    }*/

    public static boolean sendMessage(Context ctx, EmailModel email) {
        EmailLogic emailLogic = GlobalLogics.getEmail();
        String mailContent = email.getContent();

        boolean ok = false;

        String uidString = "";
        try {
            // Set the email properties necessary to send email
            final Properties props = System.getProperties();

            props.put("mail.smtp.starttls.enable", true);
            props.put("mail.smtp.host", SMTP_BIZMAIL_YAHOO_COM);
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.server", SMTP_BIZMAIL_YAHOO_COM);
            props.put("mail.smtp.auth", "true");
            //props.put("mail.smtp.port", port);
            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.socketFactory.port", SMTP_BIZMAIL_YAHOO_COM);
            props.put("mail.smtp.starttls.required", true);
            Session sess = Session.getInstance(props, new MailAuthentication(email.getSendEmailName(), email.getSendEmailPassword()));


            Message msg = new MimeMessage(sess);

            StringTokenizer toST = new StringTokenizer(email.getTo(), ",");
            if (toST.countTokens() > 1) {
                InternetAddress[] address = new InternetAddress[toST.countTokens()];
                int addrIndex = 0;
                String addrString = "";
                while (toST.hasMoreTokens()) {
                    addrString = toST.nextToken();
                    address[addrIndex] = (new InternetAddress(addrString));
                    addrIndex = addrIndex + 1;
                }
                msg.setRecipients(Message.RecipientType.TO, address);
            } else {
                InternetAddress[] address = {new InternetAddress(email.getTo())};
                msg.setRecipients(Message.RecipientType.TO, address);
            }

            InternetAddress from = new InternetAddress(email.getSendEmailName());

            msg.setFrom(from);
            msg.setSubject(email.getTitle());

            UID msgUID = new UID();

            uidString = msgUID.toString();

            msg.setHeader("X-Mailer", uidString);

            msg.setSentDate(new Date());

            MimeMultipart mp = new MimeMultipart();

            // create body part for textarea
            MimeBodyPart mbp1 = new MimeBodyPart();
            String messageText = email.getContent();
            /*if (getCustomerName() != null) {
                messageText = "From: " + getCustomerName() + "\n" + messageText;
            }*/

            if (isHTML) {
                mbp1.setContent(messageText, "text/html;charset=utf-8");
            } else {
                mbp1.setContent(messageText, "text/plain;charset=utf-8");
            }
            mp.addBodyPart(mbp1);


            if (!isHTML) {
                msg.setContent(messageText, "text/plain;charset=utf-8");
            } else {
                msg.setContent(messageText, "text/html;charset=utf-8");
            }
            for (int i = 0; i < 3; i++) {
                if (rawSend(msg)) {
                    ok = true;
                    break;
                } else {
                    log.info(null, "Email send error!=>> send " + i + " times" + msg.toString());
                    continue;
                }
            }

        } catch (Exception eq) {
            log.info(null, "Messaging Exception: " + eq.getMessage());
            emailLogic.saveFailureEmail(ctx, email);
            eq.printStackTrace();
        }

        if (!ok) {
            log.info(null, "Could not connect to SMTP server.");
            emailLogic.saveFailureEmail(ctx, email);
        }

        return ok;
    }

    static class MailAuthentication extends Authenticator {
        String smtpUsername = null;
        String smtpPassword = null;

        public MailAuthentication(String username, String password) {
            smtpUsername = username;
            smtpPassword = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(smtpUsername, smtpPassword);
        }
    }


    /*public void setToField(String toField) {
        this.toField = toField;
    }


    public String getToField() {
        return toField;
    }


    public void setSubjectField(String subjectField) {
        this.subjectField = subjectField;
    }


    public String getSubjectField() {
        return subjectField;
    }


    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }


    public String getMessageText() {
        return messageText;
    }


    public void setMyAddress(String myAddress) {
        this.myAddress = myAddress;
    }


    public String getMyAddress() {
        return myAddress;
    }


    public void setAttachmentFile(String attachmentFile) {
        this.attachmentFile = attachmentFile;
    }


    public String getAttachmentFile() {
        return attachmentFile;
    }


    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }


    public String getCustomerName() {
        return customerName;
    }*/

    public void setHTML(boolean isHTML) {
        this.isHTML = isHTML;
    }

    public boolean getHTML() {
        return isHTML;
    }

//    public  static void  main(String[] args){
//    	SendMail sendMail = new SendMail();
//    	sendMail.setToField("xiuli.kong@gmail.com");
//    	sendMail.setSubjectField("Hello");
//    	//sendMail.setCustomerName(" ME");
//    	sendMail.setMessageText("nothing happens");
//    	sendMail.setMyAddress("kongkong2046@gmail.com");
//    	sendMail.sendMessage("hello", "smtp.gmail.com", "465", "kongkong2046@gmail.com", "Kxl19810325");
//    }
}
