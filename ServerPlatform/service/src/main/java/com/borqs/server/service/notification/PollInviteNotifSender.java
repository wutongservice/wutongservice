package com.borqs.server.service.notification;

import com.borqs.server.base.util.StringUtils2;
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

public class PollInviteNotifSender extends NotificationSender {

    private static final org.slf4j.Logger L = LoggerFactory.getLogger(PollInviteNotifSender.class);

    public PollInviteNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = false;
    }

    @Override
    public List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        String targetIds = (String) args[0];
        List<Long> targets = StringUtils2.splitIntList(targetIds, ",");

        try {
            for (long target : targets) {
                if (!userIds.contains(target)) {
                    if ((target >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                            && (target <= Constants.GROUP_ID_END)) {
                        String members = p.getGroupMembers(target);
                        List<Long> memberIds = StringUtils2.splitIntList(members, ",");
                        userIds.addAll(memberIds);
                    } else
                        userIds.add(target);
                }
            }
        } catch (AvroRemoteException ex) {
            Logger.getLogger(SharedAppNotifSender.class.getName()).log(
                    Level.SEVERE, null, ex);
        }

        //exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
            userIds.remove(Long.parseLong(senderId));
        }

        L.trace("Poll Invite Notification Receive userIds: " + userIds);
        return userIds;
    }

    @Override
    protected String getSettingKey() {
        return Constants.NTF_POLL_INVITE;
    }

    @Override
    protected String getAppId(Object... args) {
        return (String) args[0];
    }

    @Override
    public String getTitle(Object... args) {
        String title = (String) args[0];
        String source = (String) args[1];

        return source + " invite you to vote 【" + title + "】";
    }

    @Override
    protected String getUri(Object... args) {
        return "borqs://poll/details?id=" + args[0];
    }

    @Override
    protected String getTitleHtml(Object... args) {
        String title = (String) args[0];
        String pollId = (String) args[1];
        String pollSchema = "<a href=\"borqs://poll/details?id=" + pollId + "\">" + title + "</a>";
        String sourceId = (String) args[2];
        String source = (String) args[3];
        String sourceSchema = "<a href=\"borqs://profile/details?uid=" + sourceId + "&tab=2\">" + source + "</a>";
        
        return sourceSchema + " invite you to vote 【" + pollSchema + "】";
    }

}
