package com.borqs.server.platform.util.sender.notif;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.maker.Maker;
import com.borqs.server.platform.util.sender.AsyncSender;

public class NotifSenderSupport {
    protected AsyncSender<Notification> sender;
    protected Maker<Notification> maker;

    public NotifSenderSupport() {
    }

    public AsyncSender<Notification> getSender() {
        return sender;
    }

    public void setSender(AsyncSender<Notification> sender) {
        this.sender = sender;
    }

    public Maker<Notification> getMaker() {
        return maker;
    }

    public void setMaker(Maker<Notification> maker) {
        this.maker = maker;
    }

    public void asyncSend(Notification notif) {
        sender.asyncSend(notif);
    }

    public void asyncSend(Context ctx, String template, Record opts) {
        Notification notif = maker.make(ctx, template, opts);
        asyncSend(notif);
    }

    public void asyncSend(Context ctx, String template, Object[][] opts) {
        asyncSend(ctx, template, Record.of(opts));
    }
}
