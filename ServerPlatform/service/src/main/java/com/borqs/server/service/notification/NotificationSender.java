package com.borqs.server.service.notification;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class NotificationSender {
	protected Platform p;
	protected Qiupu qiupu;
	protected String NOTIF_SERVER_ADDR = "192.168.5.208:8083"; 
	protected Notification notif = null;
	protected String receiverId = "";
	private static final Logger L = LoggerFactory.getLogger(NotificationSender.class);	
	protected boolean isReplace = false;
	
	public NotificationSender(Platform p, Qiupu qiupu)
	{
		this.p = p;
		this.qiupu = qiupu;
		
		Configuration conf = p.getConfig();
		NOTIF_SERVER_ADDR = conf.getString("notif.server", "192.168.5.208:8083");
		notif = new Notification(NOTIF_SERVER_ADDR);
//		L.trace("Avro notif server address: " + NOTIF_SERVER_ADDR);
	}
	
	protected abstract List<Long> getScope(String senderId, Object... args); 
	protected abstract String getSettingKey();
	
	protected String getAppId(Object... args)
	{
		return String.valueOf(findAppIdFromObjectType((Integer)args[0]));
	}
	
	protected String getSenderId(Object... args)
	{
		return (args[0] == null) ? "" : (String)args[0];
	}
		
	protected String getTitle(Object... args)
	{
		return "";
	}
	
	protected String getAction(Object... args)
	{
		return "android.intent.action.VIEW";
	}
	
	protected String getType(Object... args)
	{
		return getSettingKey();
	}
	
	protected String getUri(Object... args)
	{
		return "";
	}
	
	protected String getTitleHtml(Object... args)
	{
		return getTitle(args);
	}
	
	protected String getBody(Object... args)
	{
		return "";
	}
	
	protected String getBodyHtml(Object... args)
	{
		return getBody(args);
	}

    protected String getData(Object... args)
	{
		return getUri(args);
	}
	
	protected String getObjectId(Object... args)
	{
		if((args != null) && (args.length > 0))
			return (String)args[0];
		else
		    return "";
	}
	
	protected Record createNotification(Object[][] args)
	{
		Record msg = new Record();
		
        String appId = getAppId(args[0]);
        L.trace("appId: " + appId);
        msg.put("appId", appId);
        
        String senderId = getSenderId(args[1]);
        L.trace("senderId: " + senderId);
		msg.put("senderId", senderId);
        
        L.trace("receiverId: " + receiverId);
		msg.put("receiverId", receiverId);
        
        String title = getTitle(args[2]);
        L.trace("title: " + title);
		msg.put("title", title);
        
        String action = getAction(args[3]);
        L.trace("action: " + action);
		msg.put("action", action);
        
        String type = getType(args[4]);
        L.trace("type: " + type);
		msg.put("type", type);
        
        String uri = getUri(args[5]);
        L.trace("uri: " + uri);
		msg.put("uri", uri);
		
        String data = getData(args[5]);
        L.trace("data: " + data);
        msg.put("data", data);

		String titleHtml = getTitleHtml(args[6]);
        L.trace("titleHtml: " + titleHtml);
        msg.put("titleHtml", titleHtml);
		
        String body = getBody(args[7]);
        L.trace("body", body);
        msg.put("body", body);
        
        String bodyHtml = getBodyHtml(args[8]);
		L.trace("bodyHtml: " + bodyHtml);
        msg.put("bodyHtml", bodyHtml);
        
        String objectId = getObjectId(args[9]);
        L.trace("objectId: " + objectId);
		msg.put("objectId", objectId);
		
		return msg;
	}
	
	protected int findAppIdFromObjectType(int objectType) {
        int appId = Constants.APP_TYPE_BPC;
        
        if (objectType == Constants.USER_OBJECT)
            appId = Constants.APP_TYPE_BPC;
        
        else if (objectType == Constants.POST_OBJECT)
            appId = Constants.APP_TYPE_BPC;
        
        else if (objectType == Constants.VIDEO_OBJECT)
            appId = Constants.APP_TYPE_VIDEO;
        
        else if (objectType == Constants.APK_OBJECT)
            appId = Constants.APP_TYPE_QIUPU;
        
        else if (objectType == Constants.MUSIC_OBJECT)
            appId = Constants.APP_TYPE_MUSIC;
        
        else if (objectType == Constants.BOOK_OBJECT)
            appId = Constants.APP_TYPE_BROOK;
        
        return appId;
    }
	
	protected int findAppIdFromPostType(int postType) {
		int appId = Constants.APP_TYPE_BPC;
		
		if((postType == Constants.TEXT_POST) 
				|| (postType == Constants.LINK_POST))
			appId = Constants.APP_TYPE_BPC;
		
		else if(postType == Constants.VIDEO_POST)
			appId = Constants.APP_TYPE_BPC;
        else if(postType == Constants.PHOTO_POST)
			appId = Constants.APP_TYPE_BPC;
        else if(postType == Constants.AUDIO_POST)
			appId = Constants.APP_TYPE_BPC;
        else if(postType == Constants.FILE_POST)
			appId = Constants.APP_TYPE_BPC;
		
		else if((postType == Constants.BOOK_POST)
				|| (postType == Constants.BOOK_LIKE_POST)
				|| (postType == Constants.BOOK_COMMENT_POST))
			appId = Constants.APP_TYPE_BROOK;
			
		else if((postType == Constants.APK_POST)
				|| (postType == Constants.APK_LINK_POST)
				|| (postType == Constants.APK_COMMENT_POST)
				|| (postType == Constants.APK_LIKE_POST))
			appId = Constants.APP_TYPE_QIUPU;
		
		return appId;
	}
	
	public void send(Object[] scopeArgs, Object[][] args) throws ResponseError, AvroRemoteException {
        List<Long> l = getScope(getSenderId(args[1]), scopeArgs);
		L.trace("send notification scope: " + l);
        if(l.size() > 0)
		{
			Record setting = p.getPreferencesByUsers(getSettingKey(), StringUtils.join(l, ","));

			Iterator iter = setting.entrySet().iterator();

			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String userId = (String) entry.getKey();
				String value = (String) entry.getValue();

				if(value.equals("1"))
				{
					//refuse notification
					l.remove(Long.parseLong(userId));
				}
			}
			L.trace("before scope join");
			receiverId = StringUtils.join(l, ",");
			L.debug("send notification receiverId: " + receiverId);
            Record msg = createNotification(args); 
            try{
            	L.debug("send notification content: " + msg.toString(true, true));
                String result = notif.send(msg, isReplace);
                L.debug("send notification result: " + result);
            }
            catch(Exception e){
                L.debug("NOTIF_SERVER_ADDR ERROR!");
            }            
		}       
	}
}