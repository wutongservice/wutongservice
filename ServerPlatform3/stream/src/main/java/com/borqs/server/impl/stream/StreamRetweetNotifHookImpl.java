package com.borqs.server.impl.stream;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostHook;
import com.borqs.server.platform.util.sender.notif.NotifSenderSupport;


public class StreamRetweetNotifHookImpl {
    public static class StreamRetweetHook extends NotifSenderSupport implements PostHook {
        @Override
        public void before(Context ctx, Post data) {

        }

        @Override
        public void after(Context ctx, Post data) {
            if (data != null && data.getQuote() > 0)
                asyncSend(ctx, MakerTemplates.NOTIF_MY_STREAM_RETWEET, new Object[][]{
                        {"post", data}
                });
        }
    }

    public static class SharedAppHook extends NotifSenderSupport implements PostHook {
        @Override
        public void before(Context ctx, Post data) {

        }

        @Override
        public void after(Context ctx, Post data) {
            if (data != null && data.getType() == Post.POST_APK) {
                asyncSend(ctx, MakerTemplates.NOTIF_APP_SHARE, new Object[][]{
                        {"post", data}
                });
            }
        }

    }

    public static class SharedHook extends NotifSenderSupport implements PostHook {
        @Override
        public void before(Context ctx, Post data) {

        }

        @Override
        public void after(Context ctx, Post data) {
            if (data != null && data.getQuote() == 0) {
                int type = data.getType();
                if (type == Post.POST_LINK || type == Post.POST_APK_LINK || type == Post.POST_BOOK || type == Post.POST_TEXT) {
                    asyncSend(ctx, MakerTemplates.NOTIF_OTHER_SHARE, new Object[][]{
                            {"post", data}
                    });
                }
            }
        }
    }
}
