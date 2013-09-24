package com.borqs.server.platform.feature.status;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.sender.notif.NotifSenderSupport;

public class StatusNotifHooks {
    public static class UpdateStatusHook extends NotifSenderSupport implements StatusHook {
        @Override
        public void before(Context ctx, Status data) {

        }

        @Override
        public void after(Context ctx, Status data) {
            asyncSend(ctx, MakerTemplates.NOTIF_UPDATE_STATUS, new Object[][]{
                    {"status", data}
            });

        }
    }

}
