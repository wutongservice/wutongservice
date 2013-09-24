package com.borqs.server.platform.util.sender.email;


import com.borqs.server.platform.util.sender.AbstractAsyncSender;

import java.util.concurrent.Executor;

public class AsyncMailSender extends AbstractAsyncSender<Mail> {

    private String smtpAddress;
    private String smtpUsername;
    private String smtpPassword;

    public AsyncMailSender() {
    }

    public AsyncMailSender(Executor executor) {
        super(executor);
    }

    public String getSmtpAddress() {
        return smtpAddress;
    }

    public void setSmtpAddress(String smtpAddress) {
        this.smtpAddress = smtpAddress;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    private Mail fillSmtp(Mail mail) {
        if (mail.getSmtp() == null)
            mail.setSmtp(getSmtpAddress());
        if (mail.getUsername() == null)
            mail.setUsername(getSmtpUsername());
        if (mail.getPassword() == null)
            mail.setPassword(getSmtpPassword());
        return mail;
    }

    @Override
    public void asyncSend(Mail mail) {
        mail = fillSmtp(mail);
        mail.asyncSend(executor);
    }
}
