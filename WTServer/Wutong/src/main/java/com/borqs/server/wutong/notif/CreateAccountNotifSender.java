package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountImpl;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.contacts.SocialContactsImpl;
import com.borqs.server.wutong.contacts.SocialContactsLogic;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CreateAccountNotifSender extends NotificationSender {
    private static final Logger L = Logger.getLogger(CreateAccountNotifSender.class);
	public CreateAccountNotifSender() {
		super();
        isReplace = true;
	}

    private static String usex  ="m";
    private static String udevice  ="";
	@Override
	protected List<Long> getScope(Context ctx, String senderId, Object... args) {
		List<Long> scope = new ArrayList<Long>();

        SocialContactsLogic socialContacts = GlobalLogics.getSocialContacts();
        RecordSet recs = socialContacts.getWhohasMyContacts(ctx, (String) args[0], (String) args[1], (String) args[2]) ;
            L.debug(ctx, "=====getWhoHasMyContacts,recs="+recs.toString(false,false));
            for (Record r : recs){
                 scope.add(Long.parseLong(r.getString("owner")));
            }

		//exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	scope.remove(Long.parseLong(senderId));
        }
		
		return scope;
	}

	@Override
	protected String getSettingKey(Context ctx) {
		return Constants.NTF_CREATE_ACCOUNT;
	}
	
	@Override
	protected String getAppId(Context ctx, Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}

    protected String getPerhaps_name(Context ctx, String userId, String oldName) throws IOException {
        String perhaps_name = oldName;
        AccountLogic account = GlobalLogics.getAccount();
        RecordSet r_perhaps_name = RecordSet.fromJson(account.getPerhapsNameP(ctx, account.formatUrlP(ctx, userId)));
        if (r_perhaps_name.size() > 0) {
            Record rec00 = r_perhaps_name.get(0);
            perhaps_name = rec00.getString("fullname");
        }
        return  perhaps_name;
    }

//    protected Record createNotification(Context ctx, Object[][] args)
//	{
//		Record msg = new Record();
//		msg.put("appId", getAppId(ctx, args[0]));
//		msg.put("senderId", getSenderId(ctx, args[1]));
//		msg.put("receiverId", receiverId);
//		msg.put("title", getTitle(ctx, args[2]));
//		msg.put("action", getAction(ctx, args[3]));
//		msg.put("type", getType(ctx, args[4]));
//		msg.put("uri", getUri(ctx, args[5]));
//		msg.put("data", getData(ctx, getSenderId(ctx, args[1])));
//
//		msg.put("titleHtml", getTitleHtml(ctx, args[6]));
//		msg.put("body", getBody(ctx, args[7]));
//		msg.put("bodyHtml", getBodyHtml(ctx, args[8]));
//		msg.put("objectId", getObjectId(ctx, args[9]));
//
//		return msg;
//	}

    @Override
    protected String getTitle(Context ctx, String lang, Object... args) {
        String sex = (String) args[3];
        usex = sex;
        udevice = (String) args[1];
        String callName = "";
        if (sex.equalsIgnoreCase("m")) {
            callName = Constants.getBundleString((String) args[3], "platform.profile.sex.man");
        } else {
            callName = Constants.getBundleString((String) args[3], "platform.profile.sex.woman");
        }

        String message = Constants.getBundleString((String) args[1], "platform.create.account.send.notification");
        String pName = (String) args[2];
        try {
            pName = getPerhaps_name(ctx, (String) args[0], (String) args[2]);
        } catch (IOException e) {
        }
        String content = SQLTemplate.merge(message, new Object[][]{
                {"username", pName},
                {"sex", callName}
        });
        return content;
    }
	
	@Override
	protected String getUri(Context ctx, Object... args)
	{		
		return "borqs://profile/details?uid=" + args[0] + "&tab=2";
	}

    @Override
	protected String getData(Context ctx, Object... args)
	{
        String userId = (String)args[0];
        Record u=new Record();

        AccountLogic account = GlobalLogics.getAccount();
        u = account.getUser(ctx, userId,userId,"user_id,login_email1,login_phone1",false) ;
            if (u.has("shared_count"))
                u.remove("shared_count");
            if (u.has("pedding_requests"))
                u.remove("pedding_requests");


        return u.toString(false,false);
	}
	
	@Override
	protected String getTitleHtml(Context ctx, String lang, Object... args)
	{
        String sex = (String) args[3];
        String callName = "";
        if (sex.equalsIgnoreCase("m")) {
            callName = Constants.getBundleString((String) args[3], "platform.profile.sex.man");
        } else {
            callName = Constants.getBundleString((String) args[3], "platform.profile.sex.woman");
        }

        String message = Constants.getBundleString((String) args[1], "platform.create.account.send.notification");
        String pName = (String) args[2];
        try {
            pName = getPerhaps_name(ctx, (String) args[0], (String) args[2]);
        } catch (IOException e) {
        }
        String content = SQLTemplate.merge(message, new Object[][]{
                {"username", pName},
                {"sex", callName}
        });
        return content;
	}

//    @Override
//    public void send(Object[] scopeArgs, Object[][] args) throws ResponseError, AvroRemoteException {
//        List<Long> l11 = getScope(getSenderId(args[1]), scopeArgs);
//        Record msg = createNotification(args);
//        String searchUid = msg.getString("senderId");
//        RecordSet userName_recs = p.getDistinctUsername(searchUid);
//        for (Record r_username : userName_recs) {
//            RecordSet owner_recs = p.getDistinctOwner(searchUid, r_username.getString("username"));
//            String receiverIds = owner_recs.joinColumnValues("owner", ",");
//            List<String> rl = StringUtils2.splitList(receiverIds, ",", true);
//            if (rl.size() > 0) {
//                Record setting = p.getPreferencesByUsers(getSettingKey(), StringUtils.join(rl, ","));
//                Iterator iterator = setting.entrySet().iterator();
//                while (iterator.hasNext()) {
//                    Map.Entry entry = (Map.Entry) iterator.next();
//                    String userId = (String) entry.getKey();
//                    String value = (String) entry.getSession();
//                    if (value.equals("1")) {
//                        rl.remove(Long.parseLong(userId));
//                    }
//                }
//
//                receiverId = StringUtils.join(rl, ",");
//                try {
//                    msg.put("titleHtml", getTitleNew(udevice,r_username.getString("username"),usex));
//                    msg.put("title", msg.getString("titleHtml"));
//
//                    notif.send(msg, isReplace);
//                } catch (Exception e) {
//                    L.debug("NOTIF_SERVER_ADDR ERROR!");
//                }
//
//                L.debug("send notification end at:" + DateUtils.nowMillis());
//            }
//        }
//    }

//    protected String getTitleNew(String device,String username,String usex) {
//        String callName = "";
//        if (usex.equalsIgnoreCase("m")) {
//            callName = Constants.getBundleString(device, "platform.profile.sex.man");
//        } else {
//            callName = Constants.getBundleString(device, "platform.profile.sex.woman");
//        }
//
//        String message = Constants.getBundleString(device, "platform.create.account.send.notification");
//        String content = SQLTemplate.merge(message, new Object[][]{
//                {"username", username},
//                {"sex", callName}
//        });
//        return content;
//    }
}