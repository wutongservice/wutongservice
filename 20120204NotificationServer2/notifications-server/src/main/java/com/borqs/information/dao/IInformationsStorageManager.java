package com.borqs.information.dao;

import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;
import com.mongodb.DBObject;

public interface IInformationsStorageManager {

	// delete a record in database by record ID
	public abstract void delete(String id);

	// add a record to database when information ID is null
	// and update a record when information ID isn't null
	public abstract String save(Information info);

	public abstract InformationList list(String user);
	public abstract InformationList list(String userId, String status, Long from,
			Integer size);
	public abstract InformationList list(String appId, String userId, String status, Long from,
			Integer size);
	
	public abstract InformationList top(String userId, String status, Integer topNum);
	public abstract InformationList top(String appId, String userId, String status,
			Integer topNum);
    public abstract InformationList userTop(String appId, String userId,Integer type, String status,String scene,
    			Integer topNum);
	
	public abstract int count(String userId, String status);
	public abstract int count(String appId, String userId, String status);

	public abstract int countByPosition(Long mid, String userId, String status, Integer dir);
	public abstract int countByPosition(Long mid, String appId, String userId, String status, Integer dir);
	
	public abstract void markProcessed(String id);
	public abstract void markRead(String string);

	public abstract InformationList listById(String userId, String status,
			Long mid, Integer count);
	public abstract InformationList listById(String appId, String userId, String status,
			Long mid, Integer count);
	
	public abstract InformationList listByTime(String userId, String status,
			Long from, Integer count);
	public abstract InformationList listByTime(String appId, String userId, String status,
			Long time, Integer count);

    public abstract InformationList userListByTime( String userId, String status,int type,String scene,
   			Long from, Integer count);

    public abstract InformationList userReadListByTime( String userId, String status,int type,String scene,
       			int read,Long from, Integer count);

	public InformationList query(String appId, String type, String receiverId, String objectId);
	
	public String replace(final Information info);

    DBObject queryNotifByGroup(String userId, String scene);
}