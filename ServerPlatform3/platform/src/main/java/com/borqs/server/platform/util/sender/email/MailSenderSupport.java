package com.borqs.server.platform.util.sender.email;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.maker.Maker;
import com.borqs.server.platform.util.sender.AsyncSender;

public class MailSenderSupport {
    protected AsyncSender<Mail> sender;
    protected Maker<Mail> maker;

    public MailSenderSupport() {
    }

    public AsyncSender<Mail> getSender() {
        return sender;
    }

    public void setSender(AsyncSender<Mail> sender) {
        this.sender = sender;
    }

    public Maker<Mail> getMaker() {
        return maker;
    }

    public void setMaker(Maker<Mail> maker) {
        this.maker = maker;
    }

    public void asyncSend(Mail mail) {
        sender.asyncSend(mail);
    }

    public void asyncSend(Context ctx, String template, Record opts) {
        Mail mail = maker.make(ctx, template, opts);
        asyncSend(mail);
    }

    public void asyncSend(Context ctx, String template, Object[][] opts) {
        asyncSend(ctx, template, Record.of(opts));
    }
}
