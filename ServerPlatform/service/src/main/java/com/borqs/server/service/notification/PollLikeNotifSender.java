package com.borqs.server.service.notification;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PollLikeNotifSender extends NotificationSender {
    private static final org.slf4j.Logger L = LoggerFactory.getLogger(PollLikeNotifSender.class);
    private String displayNames = "";
    private String nameLinks = "";

    public PollLikeNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }

    protected List<Long> getScope(String senderId, Object... args) {
        L.debug("=====NTF_POLL_LIKE=====begin");
        List<Long> userIds = new ArrayList<Long>();
        List<Long> whoComments = new ArrayList<Long>();
        if (getSettingKey().equals(Constants.NTF_POLL_LIKE)) {
            try {
                List<String> reasons = new ArrayList<String>();
                reasons.add(String.valueOf(Constants.C_POLL_CREATE));
                reasons.add(String.valueOf(Constants.C_POLL_LIKE));
                RecordSet conversation_users = p.getConversation(Constants.POLL_OBJECT, (String) args[0], reasons, 0, 0, 100);
                for (Record r : conversation_users) {
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                List<String> reasons1 = new ArrayList<String>();
                reasons1.add(String.valueOf(Constants.C_POLL_LIKE));
                RecordSet conversation_users1 = p.getConversation(Constants.POLL_OBJECT, (String) args[0], reasons1, 0, 0, 100);
                for (Record r1 : conversation_users1) {
                    if (!whoComments.contains(Long.parseLong(r1.getString("from_"))))
                        whoComments.add(Long.parseLong(r1.getString("from_")));
                }

                //=========================new send to ,from conversation end ====================
            } catch (AvroRemoteException ex) {
                Logger.getLogger(PollLikeNotifSender.class.getName()).log(Level.SEVERE, null, ex);
            }
            L.debug("=====NTF_POLL_LIKE=====join userIds"+StringUtils.join(userIds,","));
        }
        if (userIds.contains(Long.parseLong((String)args[1])))
            userIds.remove(Long.parseLong((String)args[1]));

        //exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
            userIds.remove(Long.parseLong(senderId));
        }
        try {
            userIds = p.formatIgnoreUserList(senderId, userIds, (String) args[0], "");
            whoComments = p.formatIgnoreUserList(senderId, whoComments, (String) args[0], "");
        } catch (Exception e) {
        }
        L.debug("=====NTF_POLL_LIKE=====join userIds add ignore"+StringUtils.join(userIds,","));
        try {
            List<String> l = new ArrayList<String>();
            int size = whoComments.size();
            if (size > 4) {
                size = 4;
            }
            for (int i = 0; i < size; i++) {
                String userId = String.valueOf(whoComments.get(i));
                String displayName = p.getUser(userId, userId, "display_name")
                        .getString("display_name", "");

                displayNames += displayName + ", ";
                l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");
            }

            nameLinks = StringUtils.join(l, ", ");
            if(StringUtils.isNotBlank(displayNames))
            {
                displayNames = StringUtils.substringBeforeLast(displayNames, ",");
            }
        } catch (AvroRemoteException e) {

        }
        L.debug("=====NTF_POLL_LIKE=====scope"+StringUtils.join(userIds,","));
        return userIds;
    }

    @Override
    protected String getSettingKey() {
        return Constants.NTF_POLL_LIKE;
    }

    @Override
    protected String getAppId(Object... args) {
        return String.valueOf(Constants.APP_TYPE_BPC);
    }

    @Override
    protected String getTitle(Object... args) {
        String title = (String)args[0];

        return displayNames + " have liked the poll 【" + title + "】";
    }

    @Override
    protected String getUri(Object... args) {
        return "borqs://poll/comment?id=" + (String)args[0];
    }

    @Override
    protected String getTitleHtml(Object... args) {
        String title = (String) args[0];
        String pollId = (String) args[1];
        String pollSchema = "<a href=\"borqs://poll/details?id=" + pollId + "\">" + title + "</a>";

        return nameLinks + " have liked the poll 【" + pollSchema + "】";
    }

}