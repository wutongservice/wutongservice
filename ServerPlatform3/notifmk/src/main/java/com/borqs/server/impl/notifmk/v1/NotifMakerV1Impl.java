package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.maker.AbstractMaker;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

public class NotifMakerV1Impl extends AbstractMaker<Notification> {
    private CreateAccount1NotifMaker createAccount1NotifMaker;
    private UpdateAccount1NotifMaker updateAccount1NotifMaker;
    private AcceptSuggestNotifMaker acceptSuggestNotifMaker;
    private AppCommentNotifMaker appCommentNotifMaker;
    private AppLikeNotifMaker appLikeNotifMaker;
    private StreamRetweetNotifMaker streamRetweetNotifMaker;
    private StreamLikeNotifMaker streamLikeNotifMaker;
    private StreamCommentNotifMaker streamCommentNotifMaker;
    private SharedNotifMaker sharedNotifMaker;
    private SharedAppNotifMaker sharedAppNotifMaker;
    private RequestAttentionNotifMaker requestAttentionNotifMaker;
    private SuggestUserNotifMaker suggestUserNotifMaker;
    private NewFollowerNotifMaker newFollowerNotifMaker;
    private UpdateStatusNotifMaker updateStatusNotifMaker;
    private PhotoCommentNotifMaker photoCommentNotifMaker;
    private PhotoLikeNotifMaker photoLikeNotifMaker;

    public void setCreateAccount1NotifMaker(CreateAccount1NotifMaker createAccount1NotifMaker) {
        this.createAccount1NotifMaker = createAccount1NotifMaker;
    }

    public void setUpdateAccount1NotifMaker(UpdateAccount1NotifMaker updateAccount1NotifMaker) {
        this.updateAccount1NotifMaker = updateAccount1NotifMaker;
    }

    public void setAcceptSuggestNotifMaker(AcceptSuggestNotifMaker acceptSuggestNotifMaker) {
        this.acceptSuggestNotifMaker = acceptSuggestNotifMaker;
    }

    public void setAppCommentNotifMaker(AppCommentNotifMaker appCommentNotifMaker) {
        this.appCommentNotifMaker = appCommentNotifMaker;
    }

    public void setAppLikeNotifMaker(AppLikeNotifMaker appLikeNotifMaker) {
        this.appLikeNotifMaker = appLikeNotifMaker;
    }

    public void setStreamRetweetNotifMaker(StreamRetweetNotifMaker streamRetweetNotifMaker) {
        this.streamRetweetNotifMaker = streamRetweetNotifMaker;
    }

    public void setStreamLikeNotifMaker(StreamLikeNotifMaker streamLikeNotifMaker) {
        this.streamLikeNotifMaker = streamLikeNotifMaker;
    }

    public void setStreamCommentNotifMaker(StreamCommentNotifMaker streamCommentNotifMaker) {
        this.streamCommentNotifMaker = streamCommentNotifMaker;
    }

    public void setSharedNotifMaker(SharedNotifMaker sharedNotifMaker) {
        this.sharedNotifMaker = sharedNotifMaker;
    }

    public void setSharedAppNotifMaker(SharedAppNotifMaker sharedAppNotifMaker) {
        this.sharedAppNotifMaker = sharedAppNotifMaker;
    }

    public void setRequestAttentionNotifMaker(RequestAttentionNotifMaker requestAttentionNotifMaker) {
        this.requestAttentionNotifMaker = requestAttentionNotifMaker;
    }

    public void setSuggestUserNotifMaker(SuggestUserNotifMaker suggestUserNotifMaker) {
        this.suggestUserNotifMaker = suggestUserNotifMaker;
    }

    public void setNewFollowerNotifMaker(NewFollowerNotifMaker newFollowerNotifMaker) {
        this.newFollowerNotifMaker = newFollowerNotifMaker;
    }

    public void setUpdateStatusNotifMaker(UpdateStatusNotifMaker updateStatusNotifMaker) {
        this.updateStatusNotifMaker = updateStatusNotifMaker;
    }

    public void setPhotoCommentNotifMaker(PhotoCommentNotifMaker photoCommentNotifMaker) {
        this.photoCommentNotifMaker = photoCommentNotifMaker;
    }

    public void setPhotoLikeNotifMaker(PhotoLikeNotifMaker photoLikeNotifMaker) {
        this.photoLikeNotifMaker = photoLikeNotifMaker;
    }

    @Override
    public String[] getTemplates() {
        return new String[]{
                MakerTemplates.NOTIF_CREATE_ACCOUNT,
                MakerTemplates.NOTIF_UPDATE_PROFILE,
        };
    }

    @Override
    public Notification make(Context ctx, String template, Record opts) {
        if (StringUtils.equals(template, MakerTemplates.NOTIF_CREATE_ACCOUNT)) {
            return createAccount1NotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_UPDATE_PROFILE)) {
            return updateAccount1NotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_ACCEPT_SUGGEST)) {
            return acceptSuggestNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_MY_APP_COMMENT)) {
            return appCommentNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_MY_APP_LIKE)) {
            return appLikeNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_MY_STREAM_RETWEET)) {
            return streamRetweetNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_MY_STREAM_LIKE)) {
            return streamLikeNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_MY_STREAM_COMMENT)) {
            return streamCommentNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_OTHER_SHARE)) {
            return sharedNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_APP_SHARE)) {
            return sharedAppNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_REQUEST_ATTENTION)) {
            return requestAttentionNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_SUGGEST_USER)) {
            return suggestUserNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_NEW_FOLLOWER)) {
            return newFollowerNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_UPDATE_STATUS)) {
            return updateStatusNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_MY_PHOTO_COMMENT)) {
            return photoCommentNotifMaker.make(ctx, opts);
        } else if (StringUtils.equals(template, MakerTemplates.NOTIF_MY_PHOTO_LIKE)) {
            return photoLikeNotifMaker.make(ctx, opts);
        } else
            throw new IllegalArgumentException();
    }


}
