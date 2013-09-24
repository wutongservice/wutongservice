package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountImpl;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.conversation.ConversationImpl;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.ignore.IgnoreImpl;
import com.borqs.server.wutong.ignore.IgnoreLogic;
import com.borqs.server.wutong.stream.StreamImpl;
import com.borqs.server.wutong.stream.StreamLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppCommentNotifSender extends NotificationSender {
    private String displayNames = "";
    private String nameLinks = "";
    ArrayList<Long> toIds = new ArrayList<Long>();
	
    public AppCommentNotifSender() {
        super();
        isReplace = true;
    }
    
    @Override
    protected List<Long> getScope(Context ctx, String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        List<Long> whoComments = new ArrayList<Long>();
        if (getSettingKey(ctx).equals(Constants.NTF_MY_APP_COMMENT)) {
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
            ConversationLogic conversation = GlobalLogics.getConversation();
                RecordSet conversation_users = conversation.getConversation(ctx, Constants.APK_OBJECT, packageName, reasons, 0, 0, 100);
                for (Record r : conversation_users){
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                List<String> reasons1 = new ArrayList<String>();
                reasons1.add(String.valueOf(Constants.C_APK_COMMENT));
                RecordSet conversation_users1 = conversation.getConversation(ctx, Constants.APK_OBJECT, packageName, reasons1, 0, 0, 100);
                for (Record r1 : conversation_users1) {
                    if (!whoComments.contains(Long.parseLong(r1.getString("from_"))))
                        whoComments.add(Long.parseLong(r1.getString("from_")));
                }

                List<String> reasons2 = new ArrayList<String>();
                reasons2.add(String.valueOf(Constants.C_COMMENT_ADDTO));
                RecordSet conversation_users_addto = conversation.getConversation(ctx, Constants.COMMENT_OBJECT, (String) args[2], reasons2, 0, 0, 100);
                for (Record r : conversation_users_addto) {
                    long addToId = Long.parseLong(r.getString("from_"));
                    if (!userIds.contains(addToId)) {
                        userIds.add(addToId);
                    }
                    if (!toIds.contains(addToId)) {
                        toIds.add(addToId);
                    }
                }

                //=========================new send to ,from conversation end ====================

        }
        if (userIds.contains(Long.parseLong((String)args[1])))
            userIds.remove(Long.parseLong((String)args[1]));
        
         //exclude sender
      		if(StringUtils.isNotBlank(senderId))
      		{
      			userIds.remove(Long.parseLong(senderId));      			
      		}      		     			

        try{
            IgnoreLogic ignore = GlobalLogics.getIgnore();
            userIds = ignore.formatIgnoreUserListP(ctx, userIds, "", (String) args[2]);
            whoComments = ignore.formatIgnoreUserListP(ctx, whoComments, "", (String) args[2]);
        }
        catch (Exception e)  {
        }


			List<String> l = new ArrayList<String>();
			int size = whoComments.size();
			if (size > 4) {
				size = 4;
			}
			for (int i = 0; i < size; i++) {
				String userId = String.valueOf(whoComments.get(i));
                AccountLogic account = GlobalLogics.getAccount();
                String displayName = account.getUser(ctx, userId, userId, "display_name")
						.getString("display_name", "");
				
				displayNames += displayName + ", ";
				l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");				
			}
			
			nameLinks = StringUtils.join(l, ", ");
			if(StringUtils.isNotBlank(displayNames))
			{
				displayNames = StringUtils.substringBeforeLast(displayNames, ",");				
			}

        return userIds;
    }
    
   @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_MY_APP_COMMENT;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
//        return String.valueOf(Constants.APP_TYPE_QIUPU);
        return (String) args[0];
    }

    @Override
    protected String getTitle(Context ctx, String lang, Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String returnString = "";
        String act = "分享";

        StreamLogic stream = GlobalLogics.getStream();
        RecordSet es_shared_Users = stream.findWhoSharedApp(ctx, apk_id, 200);
            String a = es_shared_Users.joinColumnValues("source", ",");
            List<String> ll = StringUtils2.splitList(a, ",", true);
            if (!ll.contains(receiverId)) {
                act = "评论";
            }

        if (getSettingKey(ctx).equals(Constants.NTF_MY_APP_COMMENT)) {
        	//您" + act + "的
        	returnString = displayNames + Constants.getBundleStringByLang(lang, "app.comment.notif") + appname;
        }
        return returnString;
    }

    @Override
    protected String getAction(Context ctx, Object... args) {
        return "android.intent.action.VIEW";
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        String apkId = (String)args[0];
        return "borqs://application/comment?id=" + apkId + "&comment_id=" + (String)args[1];
    }

    @Override
    protected String getData(Context ctx, Object... args) {
        if (toIds.isEmpty())
            return "";
        else
            return "," + StringUtils2.joinIgnoreBlank(",", toIds) + ",";
    }
    
    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String returnString = "";
        String act = "分享";

        StreamLogic stream = GlobalLogics.getStream();
            RecordSet es_shared_Users = stream.findWhoSharedApp(ctx, apk_id, 200);
            String a = es_shared_Users.joinColumnValues("source", ",");
            List<String> ll = StringUtils2.splitList(a, ",", true);
            if (!ll.contains(receiverId)) {
                act = "评论";
            }

        if (getSettingKey(ctx).equals(Constants.NTF_MY_APP_COMMENT)) {
            //您"+act+"的
        	returnString = nameLinks + Constants.getBundleStringByLang(lang, "app.comment.notif") + ":<a href=\"borqs://application/details?id="+ apk_id +"\">"+appname+"</a>";
        }
        return returnString;
    }
    
    @Override
    protected String getBody(Context ctx, Object... args)
	{
		return (String)args[0];
	}
}