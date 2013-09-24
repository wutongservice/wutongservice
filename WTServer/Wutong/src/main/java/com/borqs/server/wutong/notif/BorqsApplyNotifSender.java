package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.conversation.ConversationImpl;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.group.GroupImpl;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.ignore.IgnoreImpl;
import com.borqs.server.wutong.ignore.IgnoreLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BorqsApplyNotifSender extends GroupNotificationSender {
    private ArrayList<Long> groups = new ArrayList<Long>();

    public BorqsApplyNotifSender() {
        super();
        isReplace = true;
    }
    
    @Override
	public List<Long> getScope(Context ctx, String senderId, RecordSet recs, Object... args) {
		List<Long> userIds = new ArrayList<Long>();

            List<String> reasons = new ArrayList<String>();
            reasons.add(String.valueOf(Constants.C_STREAM_TO));
            reasons.add(String.valueOf(Constants.C_STREAM_ADDTO));
        ConversationLogic conversation = GlobalLogics.getConversation();
        RecordSet conversation_users = conversation.getConversation(ctx, Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
            for (Record r : conversation_users) {
                long userId = Long.parseLong(r.getString("from_"));
                if (!userIds.contains(userId)) {
                    if ((userId >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                            && (userId <= Constants.GROUP_ID_END)) {
                        groups.add(userId);
                        GroupLogic groupImpl = GlobalLogics.getGroup();
                        String members = groupImpl.getAllMembers(ctx, userId, -1, -1, "");
                        List<Long> memberIds = StringUtils2.splitIntList(members, ",");
                        userIds.addAll(memberIds);

                        if (recs == null) {
                            recs = new RecordSet();
                        }
                        RecordSet notifRecs = groupImpl.getMembersNotification(ctx, userId, members);
                        recs.addAll(notifRecs);

                        for (Record notifRec : notifRecs) {
                            String memberId = notifRec.getString("member");
                            long recvNotif = notifRec.getInt("recv_notif", 0);
                            if (recvNotif == 2) {
                                userIds.remove(Long.parseLong(memberId));
                            }
                        }
                    }
                    else
                        userIds.add(userId);
                }
            }
            //=========================new send to ,from conversation end ====================

		//exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	userIds.remove(Long.parseLong(senderId));
        }
		try {
            IgnoreLogic ignore = GlobalLogics.getIgnore();
            userIds = ignore.formatIgnoreUserListP(ctx, userIds, "","");
        } catch (Exception e) {
        }

        return userIds;
	}
    
   @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_FILE_SHARE;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
        String sType = (String)args[0];
        return sType;
    }

    public String getFileType(Context ctx, String type){
         String return_type="文件";
        if (type.equals(String.valueOf(Constants.AUDIO_POST)))
            return_type = "音频";
         if (type.equals(String.valueOf(Constants.VIDEO_POST)))
            return_type = "视频";
        return return_type;
    }
    
    @Override
    public String getTitle(Context ctx, String lang, Object... args) {
        if ((Long.parseLong(String.valueOf(args[0])) & Constants.TEXT_POST) ==Constants.TEXT_POST)   {
            return args[1] + " have applied for Borqs Innovation Competition";
        }else {
            return args[1] + "have submitted his works:"+getFileType(ctx, String.valueOf(args[0]))+":"+String.valueOf(args[2])+"";
        }
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        Object postId = args[0];
        return "borqs://stream/comment?id=" + postId;
    }
    
    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
        if ((Long.parseLong(String.valueOf(args[0])) & Constants.TEXT_POST) ==Constants.TEXT_POST) {
            return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a> have applied for Borqs Innovation Competition";
        } else {
            return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a>have submitted his works:"+getFileType(ctx, String.valueOf(args[0]))+":"+String.valueOf(args[3])+"";
        }

    }
    
    @Override
    public String getBody(Context ctx, Object... args)
	{
		return (String)args[0];
	}
}