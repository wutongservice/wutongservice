package com.borqs.server.platform.feature.like;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.sender.notif.NotifSenderSupport;

public class LikeNotifHooks {
    public static class CreateFriendsHook extends NotifSenderSupport implements LikeHook {
        @Override
        public void before(Context ctx, Target data) {

        }

        @Override
        public void after(Context ctx, Target data) {
            if (data.type == Target.APK)
                asyncSend(ctx, MakerTemplates.NOTIF_MY_APP_LIKE, new Object[][]{
                        {"like", data}
                });
        }
    }

    public static class CreatePostHook extends NotifSenderSupport implements LikeHook {
        @Override
        public void before(Context ctx, Target data) {

        }

        @Override
        public void after(Context ctx, Target data) {
            if (data.type == Target.POST)
                asyncSend(ctx, MakerTemplates.NOTIF_MY_STREAM_LIKE, new Object[][]{
                        {"like", data}
                });
        }
    }

    public static class LikePhotoHook extends NotifSenderSupport implements LikeHook {
        @Override
        public void before(Context ctx, Target data) {

        }

        @Override
        public void after(Context ctx, Target data) {
            if (data.type == Target.LIKE)
                asyncSend(ctx, MakerTemplates.NOTIF_MY_PHOTO_LIKE, new Object[][]{
                        {"like", data}
                });
        }
    }
}
