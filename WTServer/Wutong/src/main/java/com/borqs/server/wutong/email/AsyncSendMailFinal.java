package com.borqs.server.wutong.email;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.log.Logger;
import org.codehaus.plexus.util.StringUtils;


public class AsyncSendMailFinal {
    private final static Logger log = Logger.getLogger(AsyncSendMailFinal.class);
    public static final String SMTP_BIZMAIL_YAHOO_COM = "smtp.bizmail.yahoo.com";
    public static final String SMTP_PORT = "465";
    private static AsyncSendMailFinal _instance;

    private String smtpFromUser;
    private String smtpFromUserPassword;

    private AsyncSendMailFinal(EmailModel email) {
        smtpFromUser = email.getSendEmailName();
        if (StringUtils.isBlank(smtpFromUser)) {
            smtpFromUser = EmailModel.DEFAULT_SEND_EMAILNAME;
        }
        smtpFromUserPassword = email.getSendEmailPassword();
        if (StringUtils.isBlank(smtpFromUserPassword)) {
            smtpFromUserPassword = EmailModel.DEFAULT_SEND_EMAILPASSWORD;
        }
    }

    synchronized public static AsyncSendMailFinal getInstance(EmailModel email) {
        if (_instance == null) {
            _instance = new AsyncSendMailFinal(email);
        }
        return _instance;
    }

    public void asyncSendMailFinal(Context ctx, EmailModel email) {
        asyncSendMail(ctx, email);
    }


    public void asyncSendMail(final Context ctx, final EmailModel email) {

        EmailThreadPool.executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    log.debug(null, "entering asyncSendMessage invoke method");
                    SendMail.sendMessage(ctx,email);
                    log.debug(null, "Send email to: " + email.getTo());
                    log.debug(null, "Thread " + Thread.currentThread().getName());
                } catch (Throwable t) {
                    System.err.println(t);
                }
                System.out.println("Thread exit");
                System.out.println("Thread " + Thread.currentThread().getName());
            }
        });

    }

}
