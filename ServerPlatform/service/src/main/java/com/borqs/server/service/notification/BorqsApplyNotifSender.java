package com.borqs.server.service.notification;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BorqsApplyNotifSender extends GroupNotificationSender {
    private ArrayList<Long> groups = new ArrayList<Long>();

    public BorqsApplyNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }
    
    @Override
	public List<Long> getScope(String senderId, Object... args) {
		List<Long> userIds = new ArrayList<Long>();

		try {
            List<String> reasons = new ArrayList<String>();
            reasons.add(String.valueOf(Constants.C_STREAM_TO));
            reasons.add(String.valueOf(Constants.C_STREAM_ADDTO));
            RecordSet conversation_users = p.getConversation(Constants.POST_OBJECT, (String) args[0], reasons, 0, 0, 100);
            for (Record r : conversation_users) {
                long userId = Long.parseLong(r.getString("from_"));
                if (!userIds.contains(userId)) {
                    if ((userId >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                            && (userId <= Constants.GROUP_ID_END)) {
                        groups.add(userId);
                        String members = p.getGroupMembers(userId);
                        List<Long> memberIds = StringUtils2.splitIntList(members, ",");
                        userIds.addAll(memberIds);
                    }
                    else
                        userIds.add(userId);
                }
            }
            //=========================new send to ,from conversation end ====================
		} catch (AvroRemoteException ex) {
			Logger.getLogger(SharedAppNotifSender.class.getName()).log(
					Level.SEVERE, null, ex);
		}

		//exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	userIds.remove(Long.parseLong(senderId));
        }
		try {
           userIds = p.formatIgnoreUserList(senderId, userIds, "","");
        } catch (Exception e) {
        }
		return userIds;
	}
    
   @Override
    protected String getSettingKey() {
        return Constants.NTF_FILE_SHARE;
    }

    @Override
    protected String getAppId(Object... args) {
        String sType = (String)args[0];
        return sType;
    }

    public String getFileType(String type){
         String return_type="文件";
        if (type.equals(String.valueOf(Constants.AUDIO_POST)))
            return_type = "音频";
         if (type.equals(String.valueOf(Constants.VIDEO_POST)))
            return_type = "视频";
        return return_type;
    }
    
    @Override
    public String getTitle(Object... args) {
        if ((Long.parseLong(String.valueOf(args[0])) & Constants.TEXT_POST) ==Constants.TEXT_POST)   {
            return args[1] + " have applied for Borqs Innovation Competition";
        }else {
            return args[1] + "have submitted his works:"+getFileType(String.valueOf(args[0]))+":"+String.valueOf(args[2])+"";
        }
    }

    @Override
    protected String getUri(Object... args) {
        Object postId = args[0];
        return "borqs://stream/comment?id=" + postId;
    }
    
    @Override
    protected String getTitleHtml(Object... args) {
        if ((Long.parseLong(String.valueOf(args[0])) & Constants.TEXT_POST) ==Constants.TEXT_POST) {
            return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a> have applied for Borqs Innovation Competition";
        } else {
            return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=2\">" + args[2]+ "</a>have submitted his works:"+getFileType(String.valueOf(args[0]))+":"+String.valueOf(args[3])+"";
        }

    }
    
    @Override
    public String getBody(Object... args)
	{
		return (String)args[0];
	}
}