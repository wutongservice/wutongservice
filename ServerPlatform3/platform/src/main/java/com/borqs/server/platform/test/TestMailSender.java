package com.borqs.server.platform.test;


import com.borqs.server.platform.util.sender.AbstractAsyncSender;
import com.borqs.server.platform.util.sender.email.Mail;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class TestMailSender extends AbstractAsyncSender<Mail> {
    private final List<Mail> mails = new LinkedList<Mail>();

    public TestMailSender() {
    }

    public List<Mail> getSentMails() {
        return Collections.unmodifiableList(mails);
    }

    @Override
    public void asyncSend(Mail mail) {
        mails.add(mail);
    }

    public boolean hasSentMail() {
        return !mails.isEmpty();
    }

    public Mail getFirstSentMail() {
        return hasSentMail() ? mails.get(0) : null;
    }

    public Mail getLastSentMail() {
        return hasSentMail() ? mails.get(mails.size() - 1) : null;
    }

    public int getSentMailCount() {
        return mails.size();
    }
}
