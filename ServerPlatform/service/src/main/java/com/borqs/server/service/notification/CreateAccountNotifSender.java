package com.borqs.server.service.notification;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CreateAccountNotifSender extends NotificationSender {
    private static final Logger L = LoggerFactory.getLogger(CreateAccountNotifSender.class);
	public CreateAccountNotifSender(Platform p, Qiupu qiupu) {
		super(p, qiupu);
        isReplace = true;
	}

    private static String usex  ="m";
    private static String udevice  ="";
	@Override
	protected List<Long> getScope(String senderId, Object... args) {		
		List<Long> scope = new ArrayList<Long>();

        try {
            RecordSet recs = p.getWhoHasMyContacts((String)args[0],(String)args[1],(String)args[2]) ;
            L.debug("=====getWhoHasMyContacts,recs="+recs.toString(false,false));
            for (Record r : recs){
                 scope.add(Long.parseLong(r.getString("owner")));
            }
        } catch (AvroRemoteException e) {
        }

		//exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	scope.remove(Long.parseLong(senderId));
        }
		
		return scope;
	}

	@Override
	protected String getSettingKey() {
		return Constants.NTF_CREATE_ACCOUNT;
	}
	
	@Override
	protected String getAppId(Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}

    protected String getPerhaps_name(String userId,String oldName) throws IOException {
        String perhaps_name = oldName;
        RecordSet r_perhaps_name = RecordSet.fromJson(p.getPerhapsName(p.formatUrl(userId)));
        if (r_perhaps_name.size() > 0) {
            Record rec00 = r_perhaps_name.get(0);
            perhaps_name = rec00.getString("fullname");
        }
        return  perhaps_name;
    }

    protected Record createNotification(Object[][] args)
	{
		Record msg = new Record();
		msg.put("appId", getAppId(args[0]));
		msg.put("senderId", getSenderId(args[1]));
		msg.put("receiverId", receiverId);
		msg.put("title", getTitle(args[2]));
		msg.put("action", getAction(args[3]));
		msg.put("type", getType(args[4]));
		msg.put("uri", getUri(args[5]));
		msg.put("data", getData(getSenderId(args[1])));

		msg.put("titleHtml", getTitleHtml(args[6]));
		msg.put("body", getBody(args[7]));
		msg.put("bodyHtml", getBodyHtml(args[8]));
		msg.put("objectId", getObjectId(args[9]));

		return msg;
	}

    @Override
    protected String getTitle(Object... args) {
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
            pName = getPerhaps_name((String) args[0], (String) args[2]);
        } catch (IOException e) {
        }
        String content = SQLTemplate.merge(message, new Object[][]{
                {"username", pName},
                {"sex", callName}
        });
        return content;
    }
	
	@Override
	protected String getUri(Object... args)
	{		
		return "borqs://profile/details?uid=" + args[0] + "&tab=2";
	}

    @Override
	protected String getData(Object... args)
	{
        String userId = (String)args[0];
        Record u=new Record();
        try {
            u = p.getUser(userId,userId,"user_id,login_email1,login_phone1",false) ;
            if (u.has("shared_count"))
                u.remove("shared_count");
            if (u.has("pedding_requests"))
                u.remove("pedding_requests");
        } catch (AvroRemoteException e) {
        }

        return u.toString(false,false);
	}
	
	@Override
	protected String getTitleHtml(Object... args)
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
            pName = getPerhaps_name((String) args[0], (String) args[2]);
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
//                    String value = (String) entry.getValue();
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