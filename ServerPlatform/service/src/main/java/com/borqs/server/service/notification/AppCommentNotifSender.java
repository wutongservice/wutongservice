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

public class AppCommentNotifSender extends NotificationSender {
    private String displayNames = "";
    private String nameLinks = "";
	
    public AppCommentNotifSender(Platform p, Qiupu qiupu) {
        super(p, qiupu);
        isReplace = true;
    }
    
    protected List<Long> getScope(String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        List<Long> whoComments = new ArrayList<Long>();
        if (getSettingKey().equals(Constants.NTF_MY_APP_COMMENT)) {
            try {
                /*
                RecordSet es_shared_Users = p.findWhoSharedApp((String) args[0], 200);
                for (Record ue : es_shared_Users) {
                    if (!userIds.contains(Long.parseLong(ue.getString("source"))) && !ue.getString("source").equals("0") && !ue.getString("source").equals("")) {
                        userIds.add(Long.parseLong(ue.getString("source")));
                    }
                }
                String target0 = Constants.APK_OBJECT + ":" + (String)args[0].toString().trim();
                RecordSet recs_comments = p.findWhoCommentTarget(target0, 200);
                for (Record r0 : recs_comments) {
                    Long commenterId = Long.parseLong(r0.getString("commenter"));
                	if (!userIds.contains(commenterId) && !r0.getString("commenter").equals("") && !r0.getString("commenter").equals("0")) {
                        userIds.add(commenterId);                       
                    }
                	if (!whoComments.contains(commenterId) && !r0.getString("commenter").equals("") && !r0.getString("commenter").equals("0")) {                	
                		whoComments.add(commenterId);
                	}
                }
                */
                //=========================new send to ,from conversation=========================

                String packageName = (String)args[0].toString().trim();
                String[] a = StringUtils.split(packageName,"-");
                if (a.length>1)
                    packageName = a[0];

                List<String> reasons = new ArrayList<String>();
                reasons.add(String.valueOf(Constants.C_APK_SHARE));
                reasons.add(String.valueOf(Constants.C_APK_COMMENT));
                RecordSet conversation_users = p.getConversation(Constants.APK_OBJECT, packageName, reasons, 0, 0, 100);
                for (Record r : conversation_users){
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                List<String> reasons1 = new ArrayList<String>();
                reasons1.add(String.valueOf(Constants.C_APK_COMMENT));
                RecordSet conversation_users1 = p.getConversation(Constants.APK_OBJECT, packageName, reasons1, 0, 0, 100);
                for (Record r1 : conversation_users1) {
                    if (!whoComments.contains(Long.parseLong(r1.getString("from_"))))
                        whoComments.add(Long.parseLong(r1.getString("from_")));
                }

                List<String> reasons2 = new ArrayList<String>();
                reasons2.add(String.valueOf(Constants.C_COMMENT_ADDTO));
                RecordSet conversation_users_addto = p.getConversation(Constants.COMMENT_OBJECT, (String) args[2], reasons2, 0, 0, 100);
                for (Record r : conversation_users_addto) {
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                //=========================new send to ,from conversation end ====================

            } catch (AvroRemoteException ex) {
                Logger.getLogger(AppCommentNotifSender.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (userIds.contains(Long.parseLong((String)args[1])))
            userIds.remove(Long.parseLong((String)args[1]));
        
         //exclude sender
      		if(StringUtils.isNotBlank(senderId))
      		{
      			userIds.remove(Long.parseLong(senderId));      			
      		}      		     			

        try{
            userIds = p.formatIgnoreUserList(senderId, userIds, "", (String) args[2]);
            whoComments = p.formatIgnoreUserList(senderId, whoComments, "", (String) args[2]);
        }
        catch (Exception e)  {
        }


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
        
        return userIds;
    }
    
   @Override
    protected String getSettingKey() {
        return Constants.NTF_MY_APP_COMMENT;
    }

    @Override
    protected String getAppId(Object... args) {
//        return String.valueOf(Constants.APP_TYPE_QIUPU);
        return (String) args[0];
    }

    @Override
    protected String getTitle(Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String returnString = "";
        String act = "分享";
        try {
            RecordSet es_shared_Users = p.findWhoSharedApp(apk_id, 200);
            String a = es_shared_Users.joinColumnValues("source", ",");
            List<String> ll = StringUtils2.splitList(a, ",", true);
            if (!ll.contains(receiverId)) {
                act = "评论";
            }
        } catch (AvroRemoteException ex) {
            Logger.getLogger(AppCommentNotifSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (getSettingKey().equals(Constants.NTF_MY_APP_COMMENT)) {
        	//您" + act + "的
        	returnString = displayNames + "评论了应用" + appname;
        }
        return returnString;
    }

    @Override
    protected String getAction(Object... args) {
        return "android.intent.action.VIEW";
    }

    @Override
    protected String getUri(Object... args) {
        String apkId = (String)args[0];
        return "borqs://application/comment?id=" + apkId;
    }
    
    @Override
    protected String getTitleHtml(Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String returnString = "";
        String act = "分享";
        try {
            RecordSet es_shared_Users = p.findWhoSharedApp(apk_id, 200);
            String a = es_shared_Users.joinColumnValues("source", ",");
            List<String> ll = StringUtils2.splitList(a, ",", true);
            if (!ll.contains(receiverId)) {
                act = "评论";
            }
        } catch (AvroRemoteException ex) {
            Logger.getLogger(AppCommentNotifSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (getSettingKey().equals(Constants.NTF_MY_APP_COMMENT)) {
            //您"+act+"的
        	returnString = nameLinks + "评论了应用:<a href=\"borqs://application/details?id="+ apk_id +"\">"+appname+"</a>";
        }
        return returnString;
    }
    
    @Override
    protected String getBody(Object... args)
	{
		return (String)args[0];
	}
}