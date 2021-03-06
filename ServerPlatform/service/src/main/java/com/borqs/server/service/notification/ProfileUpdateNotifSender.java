package com.borqs.server.service.notification;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProfileUpdateNotifSender extends NotificationSender {
    private static final Logger L = LoggerFactory.getLogger(ProfileUpdateNotifSender.class);
//	private String tab = "info";
    private int tab = 2;
	private String objectId = "";
	private String displayNames = "";
    private String nameLinks = "";
    private String colDesc = "";
    private String body = "";
	
	public ProfileUpdateNotifSender(Platform p, Qiupu qiupu) {
		super(p, qiupu);
		isReplace = true;
	}

	@Override
	protected List<Long> getScope(String senderId, Object... args) {		
		String profileUpdater = (String)args[0]; //profile update user id
		List<Long> scope = new ArrayList<Long>();
		
		try {
            L.debug("find people begin at:"+ DateUtils.nowMillis());
			RecordSet rs = p.getFollowers(profileUpdater, profileUpdater, 
					String.valueOf(Constants.FRIENDS_CIRCLE), "user_id", 0, 1000);
			scope = rs.getIntColumnValues("user_id");
            L.debug("find people end at:"+ DateUtils.nowMillis());
		} catch (AvroRemoteException e) {			
			e.printStackTrace();
		}
		
		//exclude sender
        if(StringUtils.isNotBlank(senderId))
        {
        	scope.remove(Long.parseLong(senderId));
        }
		try {
           scope = p.formatIgnoreUserList(senderId, scope, "","");
        } catch (Exception e) {
        }

        L.trace("Profile Update Notification Receive userIds: " + scope);
        return scope;
	}

	@Override
	protected String getSettingKey() {
		return Constants.NTF_PROFILE_UPDATE;
	}
	
	@Override
	protected String getAppId(Object... args)
	{
		return String.valueOf(Constants.APP_TYPE_BPC);
	}
		
	@Override
	protected String getTitle(Object... args)
	{
        Record user = Record.fromJson((String)args[0]);
        String displayName = (String)args[1];
        String senderId = (String)args[2];

        String lang = "zh";
        if(args[3] != null)
            lang = (String)args[3];

        colDesc = getUpdateColumn(user, lang, senderId);
        List<String> l = new ArrayList<String>();
		nameLinks = "";
		displayNames = displayName + ", ";
		l.add("<a href=\"borqs://profile/details?uid=" + senderId + "&tab=2\">" + displayName + "</a>");
//		ArrayNode arrNode = (ArrayNode)notif.query(String.valueOf(Constants.APP_TYPE_BPC), Constants.NTF_PROFILE_UPDATE, receiverId, objectId);
        
//        try
//        {       	
//        		if(arrNode.size() > 0)
//        		{
//        			JsonNode jn = arrNode.get(0);
//        		
//        			String oldNames = StringUtils.substringBefore(jn.path("title").getTextValue(), "更新");
//        			String oldLinks = StringUtils.substringBefore(jn.path("titleHtml").getTextValue(), "更新");
//        			List<String> oNameList = StringUtils2.splitList(oldNames, ",", true);
//        			List<String> oLinkList = StringUtils2.splitList(oldLinks, ",", true);
//        			long createdTime = jn.path("date").getLongValue();
//        			long period = 1 * 24 * 60 * 60 * 1000;
//        			if (createdTime > (new Date().getTime() - period)) {                                	
//        				String userId = jn.path("senderId").getValueAsText();
//        				displayName = p.getUser(userId, userId, "display_name").getString("display_name", "");
//        				if(!StringUtils.contains(displayNames, displayName)
//        						&& (!StringUtils.equals(userId, senderId)))        			
//        				{
//        					displayNames += displayName + ", ";
//        					l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");
//        				}
//        			    
//        				for(String name : oNameList)
//        				{
//        					if(!StringUtils.contains(displayNames, name))
//        						displayNames += name + ", ";
//        				}
//        				for(String link : oLinkList)
//        				{
//        					if(!l.contains(link))
//        						l.add(link);
//        				}
//        			}        			
//        		}
        	
        	nameLinks = StringUtils.join(l, ", ");
            if(StringUtils.isNotBlank(displayNames))
    		{
    			displayNames = StringUtils.substringBeforeLast(displayNames, ",");				
    		}
//        } catch (AvroRemoteException e) {			
//			L.debug("Parse query notifications failed");
//		}
        
//        return displayNames + "更新了" + colDesc;
//          return displayNames + "更新了资料";
//		return args[1] + "更新了他的" + getUpdateColumn(user);

        String template = Constants.getBundleStringByLang(lang, "profile.update.notif.title");
        String title = SQLTemplate.merge(template, new Object[][]{
                {"who", displayNames}
        });
        return title;
	}
	
	@Override
	protected String getUri(Object... args)
	{
		return "borqs://profile/details?uid=" + args[0] + "&tab=" + tab;
	}
	
	@Override
	protected String getObjectId(Object... args)
	{
		return objectId;
	}
	
	@Override
	protected String getTitleHtml(Object... args)
	{
//            Record user = Record.fromJson((String)args[0]);
//		return "<a href=\"borqs://profile/details?uid=" + args[1] + "&tab=" + tab +"\">" + args[2] 
//				+"</a>更新了他的" + getUpdateColumn(user);
//		return nameLinks + "更新了" + colDesc;
//		return nameLinks + "更新了资料";

        String lang = "zh";
        if((args != null) && (args.length > 0))
        {
            lang = (String)args[0];
        }

        String template = Constants.getBundleStringByLang(lang, "profile.update.notif.title");
        String title = SQLTemplate.merge(template, new Object[][]{
                {"who", nameLinks}
        });
        return title;
	}
    
	@Override
    protected String getBody(Object... args)
	{
		return body;
	}
	
        private String getUpdateColumn(Record r, String lang, String senderId){
            String str = "";
            body = "";

            if (r.has("display_name"))
            {
                String displayName = Constants.getBundleStringByLang(lang, "profile.update.notif.displayname");
                str += displayName + ", ";
                objectId = senderId + ".display_name";
                body += displayName + ": " + r.getString("display_name") + ", ";
            }
            if (r.has("nick_name"))
            {
                String nickName = Constants.getBundleStringByLang(lang, "profile.update.notif.nickname");
                str += nickName + ", ";
                objectId = senderId + ".nick_name";
                body += nickName + ": " + r.getString("nick_name") + ", ";
            }
            if (r.has("gender"))
            {
                String sex = Constants.getBundleStringByLang(lang, "profile.update.notif.gender");
                str += sex + ", ";
                objectId = senderId + ".gender";
                String mGender = Constants.getBundleStringByLang(lang, "profile.update.notif.hidegender");
                String gender = r.getString("gender");
                if(StringUtils.equals(gender, "f"))
                {
                    mGender = Constants.getBundleStringByLang(lang, "profile.update.notif.female");
                }
                else if(StringUtils.equals(gender, "m"))
                {
                    mGender = Constants.getBundleStringByLang(lang, "profile.update.notif.male");
                }
                else
                {
                    mGender = Constants.getBundleStringByLang(lang, "profile.update.notif.hidegender");
                }
                body += sex + ": " + mGender + ", ";
            }
            if (r.has("birthday"))
            {
                String birthday = Constants.getBundleStringByLang(lang, "profile.update.notif.birthday");
                str += birthday + ", ";
                objectId = senderId + ".birthday";
                body += birthday + ": " + r.getString("birthday") + ", ";
            }
            if (r.has("interests"))
            {
                String interests = Constants.getBundleStringByLang(lang, "profile.update.notif.interests");
                str += interests + ", ";
                objectId = senderId + ".interests";
                body += interests + ": " + r.getString("interests") + ", ";
            }
            if (r.has("marriage"))
            {
                String txtMarriage = Constants.getBundleStringByLang(lang, "profile.update.notif.marriage");
                String married= Constants.getBundleStringByLang(lang, "profile.update.notif.married");
                String unmarried = Constants.getBundleStringByLang(lang, "profile.update.notif.unmarried");
                str += txtMarriage + ", ";
                objectId = senderId + ".marriage";
                String marriage = r.getString("marriage");
                body += txtMarriage + ": " + (StringUtils.equals(marriage, "n") ? unmarried : married) + ", ";
            }
            if (r.has("religion"))
            {
                String religion = Constants.getBundleStringByLang(lang, "profile.update.notif.religion");
                str += religion + ", ";
                objectId = senderId + ".religion";
                body += religion + ": " + r.getString("religion") + ", ";
            }
            if (r.has("company"))
            {
                String company = Constants.getBundleStringByLang(lang, "profile.update.notif.company");
                str += company + ", ";
                objectId = senderId + ".company";
                body += company + ": " + r.getString("company") + ", ";
            }
            if (r.has("image_url"))
            {
                String image = Constants.getBundleStringByLang(lang, "profile.update.notif.image");
                str += image + ", ";
                objectId = senderId + ".image_url";
            	body += image + ", ";
            }
            if (r.has("contact_info"))
            {
                String contact = Constants.getBundleStringByLang(lang, "profile.update.notif.contact");
                str += contact + ", ";
                objectId = senderId + ".contact_info";
            	body += contact + ", ";
            }
            if (r.has("address"))
            {
                String address = Constants.getBundleStringByLang(lang, "profile.update.notif.address");
                str += address + ", ";
                objectId = senderId + ".address";
                String json = r.getString("address");
                ArrayNode an = (ArrayNode)JsonUtils.parse(json);
                body += address + ": " + an.get(0).path("street").getTextValue() + ", ";
            }
            if (r.has("status"))
            {
            	String status = Constants.getBundleStringByLang(lang, "profile.update.notif.status");
                str += status + ", ";
            	objectId = senderId + ".status";
//            	tab = "feed";
                tab = 0;
            	body += status + ": " + r.getString("status") + ", ";
            }
                        
            if (StringUtils.isNotBlank(str))
                str = StringUtils.substringBeforeLast(str, ", ");
            else
            	str = Constants.getBundleStringByLang(lang, "profile.update.notif.other");
            
            if (StringUtils.isNotBlank(body))
                body = StringUtils.substringBeforeLast(body, ", ");
            else
            	body = "";
            
            return str;
        }
        
        @Override
    	public void send(Object[] scopeArgs, Object[][] args) throws ResponseError, AvroRemoteException {
    		List<Long> l = getScope(getSenderId(args[1]), scopeArgs);
    		if(l.size() > 0)
    		{
    			Record setting = p.getPreferencesByUsers(getSettingKey(), StringUtils.join(l, ","));
    		
    			Iterator iter = setting.entrySet().iterator();
    			
    			while (iter.hasNext()) {
    				Map.Entry entry = (Map.Entry) iter.next();
    				receiverId = (String) entry.getKey();
    				String value = (String) entry.getValue();
                    
    				if(value.equals("0"))
    				{			
    					Record msg = createNotification(args); 
    		            try{
    		            	notif.send(msg, isReplace);
                            L.trace("Send Profile Update notification to: " + receiverId);
    		            }
    		            catch(Exception e){
    		                
    		            }
    				}				
    			}			     
    		}
    	}       
}