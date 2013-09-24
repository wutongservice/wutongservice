package com.borqs.server.platform.util.sender.email;


import com.borqs.server.platform.util.sender.AbstractAsyncSender;

public class DummyAsyncMailSender extends AbstractAsyncSender<Mail> {
    @Override
    public void asyncSend(final Mail m) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // TODO: print mail
            }
        });
    }
}
