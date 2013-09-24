package com.borqs.server.platform.util.sender.notif;


import com.borqs.server.platform.util.sender.AbstractAsyncSender;

public class DummyAsyncNotifSender extends AbstractAsyncSender<Notification> {
    public DummyAsyncNotifSender() {
    }

    @Override
    public void asyncSend(final Notification n) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (n.isReplace())
                    System.out.println("Replace-Notif: " + n.toJson(false));
                else
                    System.out.println("Send-Notif: " + n.toJson(false));
            }
        });
    }
}
