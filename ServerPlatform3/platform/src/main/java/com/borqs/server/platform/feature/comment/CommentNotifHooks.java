package com.borqs.server.platform.feature.comment;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.sender.notif.NotifSenderSupport;

public class CommentNotifHooks {
    public static class CreateCommentAppHook extends NotifSenderSupport implements CommentHook {
        @Override
        public void before(Context ctx, Comment data) {

        }

        @Override
        public void after(Context ctx, Comment data) {
            // if this post type is apk then execute 
            if (data.getTarget().type == Target.APK) {
                asyncSend(ctx, MakerTemplates.NOTIF_MY_APP_COMMENT, new Object[][]{
                        {"comment", data}
                });
            }
        }
    }
    public static class CreateCommentPostHook extends NotifSenderSupport implements CommentHook {
        @Override
        public void before(Context ctx, Comment data) {

        }

        @Override
        public void after(Context ctx, Comment data) {
            // if this post type is apk then execute
            if (data.getTarget().type == Target.POST) {
                asyncSend(ctx, MakerTemplates.NOTIF_MY_STREAM_COMMENT, new Object[][]{
                        {"comment", data}
                });
            }
        }
    }

    public static class CreateCommentPhotoHook extends NotifSenderSupport implements CommentHook {
        @Override
        public void before(Context ctx, Comment data) {

        }

        @Override
        public void after(Context ctx, Comment data) {
            // if this post type is apk then execute
            if (data.getTarget().type == Target.PHOTO) {
                asyncSend(ctx, MakerTemplates.NOTIF_MY_PHOTO_COMMENT, new Object[][]{
                        {"comment", data}
                });
            }
        }
    }
}
