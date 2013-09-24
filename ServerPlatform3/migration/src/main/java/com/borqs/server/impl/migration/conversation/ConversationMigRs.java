package com.borqs.server.impl.migration.conversation;


import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.Conversation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ConversationMigRs {
    public static final int C_STREAM_POST = 1;
    public static final int C_STREAM_RESHARE = 2;
    public static final int C_STREAM_COMMENT = 3;
    public static final int C_STREAM_LIKE = 4;
    public static final int C_STREAM_FAVORITE = 5;
    public static final int C_STREAM_IGNORE = 6;
    public static final int C_STREAM_TO = 7;
    public static final int C_STREAM_ADDTO = 8;
    public static final int C_COMMENT_CREATE = 11;
    public static final int C_COMMENT_LIKE = 12;
    public static final int C_COMMENT_TO = 13;
    public static final int C_COMMENT_ADDTO = 14;
    public static final int C_APK_UPLOAD = 21;
    public static final int C_APK_SHARE = 22;
    public static final int C_APK_COMMENT = 23;
    public static final int C_APK_LIKE = 24;
    public static final int C_APK_FAVORITE = 25;
    public static final int C_APK_TO = 26;
    public static final int C_APK_ADDTO = 27;
    public static final int C_LINK_SHARE = 31;
    public static final int C_BOOK_SHARE = 41;
    public static final int C_VIDEO_SHARE = 51;
    public static final int C_MUSIC_SHARE = 61;
    public static final int C_PHOTO_UPLOAD = 71;
    public static final int C_PHOTO_SHARE = 72;
    public static final int C_PHOTO_COMMENT = 73;
    public static final int C_PHOTO_LIKE = 74;
    public static final int C_PHOTO_FAVORITE = 75;
    public static final int C_PHOTO_TO = 76;
    public static final int C_PHOTO_ADDTO = 77;

    public static long Counter = 0;

    public static Conversation readConversation(ResultSet rs, Conversation conversation, Map<Long, String> mapAccount,Map<Long, String> mapPost,Map<Long, String> mapComment,Map<Long, String> mapPhoto) throws SQLException {
        if (conversation == null)
            conversation = new Conversation();

        conversation.setCreatedTime(rs.getLong("created_time"));
        long user = rs.getLong("from_");
        if (!mapAccount.containsKey(user)){
            Counter++;
            return null;
        }
        conversation.setUser(user);

        typeV1toV2(rs.getInt("reason"), conversation, rs.getString("target_id"));

        if(!checkConversation(conversation,mapPhoto,mapComment,mapPhoto))
            return null;

        return conversation;
    }

    private static boolean checkConversation(Conversation c, Map<Long, String> streamIds, Map<Long, String> commentIds, Map<Long, String> photoIds) {
        int type = c.getTarget().type;
        if (type == Target.POST && streamIds !=null) {
            if (!streamIds.containsValue(c.getTarget().id))
                return false;
        }
        if(type == Target.COMMENT && commentIds!=null){
            if(!commentIds.containsValue(c.getTarget().id))
                return false;
        }
        if(type == Target.PHOTO && photoIds != null){
            if(!photoIds.containsValue(c.getTarget().id))
                return false;
        }
        return  true;
    }

    private static Conversation typeV1toV2(int v1, Conversation c, String targetId) {
        switch (v1) {
            case C_STREAM_POST:
                c.setTarget(new Target(Target.POST, targetId));
                c.setReason(Actions.CREATE);
                break;
            case C_STREAM_RESHARE:
                c.setTarget(new Target(Target.POST, targetId));
                c.setReason(Actions.RESHARE);
                break;
            case C_STREAM_COMMENT:
                c.setTarget(new Target(Target.POST, targetId));
                c.setReason(Actions.COMMENT);
                break;
            case C_STREAM_LIKE:
                c.setTarget(new Target(Target.POST, targetId));
                c.setReason(Actions.LIKE);
                break;
            case C_STREAM_FAVORITE:
                c.setTarget(new Target(Target.POST, targetId));
                c.setReason(Actions.FAVORITE);
                break;
            case C_STREAM_IGNORE:
                c.setTarget(new Target(Target.POST, targetId));
                c.setReason(Actions.FAVORITE);
                break;
            case C_STREAM_TO:
                c.setTarget(new Target(Target.POST, targetId));
                c.setReason(Actions.TO);
                break;
            case C_STREAM_ADDTO:
                c.setTarget(new Target(Target.POST, targetId));
                c.setReason(Actions.ADDTO);
                break;

            case C_COMMENT_CREATE:
                c.setTarget(new Target(Target.COMMENT, targetId));
                c.setReason(Actions.CREATE);
                break;
            case C_COMMENT_LIKE:
                c.setTarget(new Target(Target.COMMENT, targetId));
                c.setReason(Actions.LIKE);
                break;
            case C_COMMENT_TO:
                c.setTarget(new Target(Target.COMMENT, targetId));
                c.setReason(Actions.TO);
                break;
            case C_COMMENT_ADDTO:
                c.setTarget(new Target(Target.COMMENT, targetId));
                c.setReason(Actions.ADDTO);
                break;

            case C_APK_UPLOAD:
                c.setTarget(new Target(Target.APK, targetId));
                c.setReason(Actions.UPLOAD);
                break;
            case C_APK_SHARE:
                c.setTarget(new Target(Target.APK, targetId));
                c.setReason(Actions.SHARE);
                break;
            case C_APK_COMMENT:
                c.setTarget(new Target(Target.APK, targetId));
                c.setReason(Actions.COMMENT);
                break;
            case C_APK_LIKE:
                c.setTarget(new Target(Target.APK, targetId));
                c.setReason(Actions.LIKE);
                break;
            case C_APK_FAVORITE:
                c.setTarget(new Target(Target.APK, targetId));
                c.setReason(Actions.FAVORITE);
                break;
            case C_APK_TO:
                c.setTarget(new Target(Target.APK, targetId));
                c.setReason(Actions.TO);
                break;
            case C_APK_ADDTO:
                c.setTarget(new Target(Target.APK, targetId));
                c.setReason(Actions.ADDTO);
                break;

            case C_PHOTO_UPLOAD:
                c.setTarget(new Target(Target.PHOTO, targetId));
                c.setReason(Actions.UPLOAD);
                break;
            case C_PHOTO_SHARE:
                c.setTarget(new Target(Target.PHOTO, targetId));
                c.setReason(Actions.SHARE);
                break;
            case C_PHOTO_COMMENT:
                c.setTarget(new Target(Target.PHOTO, targetId));
                c.setReason(Actions.COMMENT);
                break;
            case C_PHOTO_LIKE:
                c.setTarget(new Target(Target.PHOTO, targetId));
                c.setReason(Actions.LIKE);
                break;
            case C_PHOTO_FAVORITE:
                c.setTarget(new Target(Target.PHOTO, targetId));
                c.setReason(Actions.FAVORITE);
                break;
            case C_PHOTO_TO:
                c.setTarget(new Target(Target.PHOTO, targetId));
                c.setReason(Actions.TO);
                break;
            case C_PHOTO_ADDTO:
                c.setTarget(new Target(Target.PHOTO, targetId));
                c.setReason(Actions.ADDTO);
                break;

            case C_LINK_SHARE:
                c.setTarget(new Target(Target.LINK, targetId));
                c.setReason(Actions.SHARE);
                break;
            case C_BOOK_SHARE:
                c.setTarget(new Target(Target.BOOK, targetId));
                c.setReason(Actions.SHARE);
                break;
            case C_VIDEO_SHARE:
                c.setTarget(new Target(Target.VIDEO, targetId));
                c.setReason(Actions.SHARE);
                break;
            case C_MUSIC_SHARE:
                c.setTarget(new Target(Target.MUSIC, targetId));
                c.setReason(Actions.SHARE);
                break;

        }
        return c;
    }
}
