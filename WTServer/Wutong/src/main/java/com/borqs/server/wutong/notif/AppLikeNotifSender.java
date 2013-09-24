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

public class AppLikeNotifSender extends NotificationSender {
	private String displayNames = "";
    private String nameLinks = "";
	
    public AppLikeNotifSender() {
        super();
        isReplace = true;
    }

    @Override
    protected List<Long> getScope(Context ctx, String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();
        RecordSet liked_Users = new RecordSet();
        if (getSettingKey(ctx).equals(Constants.NTF_MY_APP_LIKE)) {
                /*
                    RecordSet es_shared_Users = p.findWhoSharedApp((String)args[0], 200);
                    for (Record ue : es_shared_Users) {
                    if (!userIds.contains(Long.parseLong(ue.getString("source"))) && !ue.getString("source").equals("0") && !ue.getString("source").equals("")) {
                        userIds.add(Long.parseLong(ue.getString("source")));
                    }

                    
                    liked_Users = p.likedUsers(Constants.APK_OBJECT, (String)args[0], 0, 200);
                }
                */

                //=========================new send to ,from conversation=========================

                String packageName = (String) args[0].toString().trim();
                String[] a = StringUtils.split(packageName, "-");
                if (a.length > 1)
                    packageName = a[0];

                List<String> reasons = new ArrayList<String>();
                reasons.add(String.valueOf(Constants.C_APK_SHARE));
            ConversationLogic conversation = GlobalLogics.getConversation();
            RecordSet conversation_users = conversation.getConversation(ctx, Constants.APK_OBJECT, packageName, reasons, 0, 0, 100);
                for (Record r : conversation_users) {
                    if (!userIds.contains(Long.parseLong(r.getString("from_"))))
                        userIds.add(Long.parseLong(r.getString("from_")));
                }

                List<String> reasons1 = new ArrayList<String>();
                reasons1.add(String.valueOf(Constants.C_APK_LIKE));
                RecordSet conversation_users1 = conversation.getConversation(ctx, Constants.APK_OBJECT, packageName, reasons1, 0, 0, 100);

            AccountLogic account = GlobalLogics.getAccount();
            liked_Users.addAll(account.getUsers(ctx, (String) args[1], conversation_users1.joinColumnValues("from_", ","), "user_id,display_name", false));

                //=========================new send to ,from conversation end ====================
        }
        if (userIds.contains(Long.parseLong((String)args[1])))
            userIds.remove(Long.parseLong((String)args[1]));
        
        //exclude sender
        if(StringUtils.isNotBlank(senderId))
  		{
  			userIds.remove(Long.parseLong(senderId));
  		}

        try {
            IgnoreLogic ignore = GlobalLogics.getIgnore();
            userIds = ignore.formatIgnoreUserListP(ctx, userIds, "", "");
            List<Long> lu = new ArrayList<Long>();
            for (Record r : liked_Users) {
                lu.add(Long.parseLong(r.getString("user_id")));
            }
            lu = ignore.formatIgnoreUserListP(ctx, lu, "", "");
            for (int i = liked_Users.size() - 1; i >= 0; i--) {
                if (!lu.contains(Long.parseLong(liked_Users.get(i).getString("user_id"))))
                    liked_Users.remove(i);
            }
        } catch (Exception e) {
        }

        List<String> l = new ArrayList<String>();
        int i = 0;
        for(Record user: liked_Users)
        {
        	if(i > 3)
        		break;
        	
        	String userId = user.getString("user_id");
        	String displayName = user.getString("display_name");
        	if(StringUtils.contains(displayNames, displayName))
        	{
        		continue;
        	}
        	else
        	{
        		displayNames += displayName + ", ";
				l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");
				i++;
        	}
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
        return Constants.NTF_MY_APP_LIKE;
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
        String act = "分享";
        String returnString = "";

        if (getSettingKey(ctx).equals(Constants.NTF_MY_APP_LIKE)) {
            //returnString = "您分享(评论)的<apk id="+apk_id+">"+appname+"</apk>被<user id="+userid+">"+username+"</user> like了";
            //您" + act + "的
        	returnString = displayNames + "赞了应用" + appname;
        }
        return returnString;
    }

    @Override
    protected String getAction(Context ctx, Object... args) {
        return "android.intent.action.VIEW";
    }
    
    @Override
    protected String getUri(Context ctx, Object... args) {
        String apk_id = (String)args[0];
        return "borqs://application/details?id="+apk_id+"";
    }
    
    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
        String apk_id = (String)args[0];
        String userid = (String)args[1];
        String username = (String)args[2];
        String appname = (String)args[3];
        String act = "分享";
        String returnString = "";

        StreamLogic stream = GlobalLogics.getStream();
        RecordSet es_shared_Users = stream.findWhoSharedApp(ctx, apk_id, 200);
            String a = es_shared_Users.joinColumnValues("source", ",");
            List<String> ll = StringUtils2.splitList(a, ",", true);
            if (!ll.contains(userid)) {
                act = "评论";
            }

        if (getSettingKey(ctx).equals(Constants.NTF_MY_APP_LIKE)) {
            //returnString = "您分享(评论)的<stream id="+streamid+">stream</stream>被<user id="+userid+">"+username+"</user> like了";
            //您"+act+"的
        	returnString = nameLinks + "赞了应用:<a href=\"borqs://application/details?id=" +apk_id+ "\">"+appname+"</a>";
        }
        return returnString;
    }
    
    @Override
    protected String getBody(Context ctx, Object... args)
	{
		return (String)args[0];
	}
}