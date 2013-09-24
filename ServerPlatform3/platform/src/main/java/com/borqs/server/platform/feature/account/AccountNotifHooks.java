package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.sender.notif.NotifSenderSupport;

public class AccountNotifHooks {
    public static class CreateAccountHook extends NotifSenderSupport implements UserHook {
        @Override
        public void before(Context ctx, User data) {
        }

        @Override
        public void after(Context ctx, User data) {
            asyncSend(ctx, MakerTemplates.NOTIF_CREATE_ACCOUNT, new Object[][] {
                    {"user", data},
            });
        }
    }

    public static class UpdateProfileHook extends NotifSenderSupport implements UserHook {
        @Override
        public void before(Context ctx, User data) {
        }

        @Override
        public void after(Context ctx, User data) {
            asyncSend(ctx, MakerTemplates.NOTIF_UPDATE_PROFILE, new Object[][] {
                    {"user",data}
            });
        }
    }
}
