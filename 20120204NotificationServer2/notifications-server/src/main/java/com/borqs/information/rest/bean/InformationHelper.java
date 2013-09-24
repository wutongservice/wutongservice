package com.borqs.information.rest.bean;

import com.borqs.information.rpc.service.Info;
import com.borqs.information.rpc.service.SendInfo;

import java.util.ArrayList;
import java.util.List;

public class InformationHelper {
	
	public static List<Info> convertToInfos(List<Information> informations) {
		List<Info> infos = new ArrayList<Info>();
		if(null == informations) {
			return infos;
		}
		
		for(Information information: informations) {
			Info info = convertToInfo(information);
			infos.add(info);
		}
		
		return infos;
	}

	public static  Info convertToInfo(Information info) {
		Info inf = new Info();
		inf.id = info.getId();
		
		inf.appId = info.getAppId();
		inf.type = info.getType();
		inf.receiverId = info.getReceiverId();
		inf.senderId = info.getSenderId();
		
		// action
		inf.uri = info.getUri();
		
		// data
		inf.title = info.getTitle();
		inf.titleHtml = info.getTitleHtml();
		inf.body = info.getBody();
		inf.bodyHtml = info.getBodyHtml();
		inf.data = info.getData();
		inf.objectId = info.getObjectId();
		
		// status
		inf.importance = info.getImportance();
		inf.processed = info.isProcessed();
		inf.processMethod = info.getProcessMethod();
		inf.read = info.isRead();
		
		inf.cDateTime = info.getDate();
		inf.lastModified = info.getLastModified();
		
		// deprecated
		inf.action = info.getAction();
		inf.guid = info.getGuid();

        //add by wangpeng at 2013-04-23
        inf.scene = info.getScene();
		
		return inf;
	}
	
	public static  Information convertToInformation(Info info) {
		Information information = new Information();
		if(null == info.importance) {
			info.importance = 30;
		}
		if(null == info.processMethod) {
			info.processMethod = 1;
		}
		
		if(null!=info.id) {
			information.setId(info.id);
		}
		
		if(null!=info.appId) {
			information.setAppId(info.appId.toString());
		}
		if(null!=info.receiverId) {
			information.setReceiverId(info.receiverId.toString());
		}
		if(null!=info.senderId) {
			information.setSenderId(info.senderId.toString());
		}
		
		if(null!=info.type) {
			information.setType(info.type.toString());
		}		
		if(null!=info.action) {
			information.setAction(info.action.toString());
		}
		if(null!=info.uri) {
			information.setUri(info.uri.toString());
		}
		if(null!=info.objectId) {
			information.setObjectId(info.objectId.toString());
		}

		if(null!=info.title) {
			information.setTitle(info.title.toString());
		}
		if(null!=info.titleHtml) {
			information.setTitleHtml(info.titleHtml.toString());
		}		
		if(null!=info.body) {
			information.setBody(info.body.toString());
		}
		if(null!=info.bodyHtml) {
			information.setBodyHtml(info.bodyHtml.toString());
		}
		if(null!=info.data) {
			information.setData(info.data.toString());
		}
		
		if(null!=info.guid) {
			information.setGuid(info.guid.toString());
		}
		if(null!=info.importance) {
			information.setImportance(info.importance);
		}
		if(null!=info.processMethod) {
			information.setProcessMethod(info.processMethod);
		}
		if(null!=info.processed) {
			information.setProcessed(info.processed);
		}
		if(null!=info.read) {
			information.setRead(info.read);
		}
		
		if(null!=info.lastModified) {
			information.setLastModified(info.lastModified);
		}
		if(null!=info.cDateTime) {
			information.setDate(info.cDateTime);
		}

        //add by wangpeng at 2013-04-23
        if(null!=info.scene){
            information.setScene(info.scene.toString());
        }
		return information;
	}
	
	public static  Information convertToInformation(SendInfo info) {
		Information information = new Information();
		if(null == info.importance) {
			info.importance = 30;
		}
		if(null == info.processMethod) {
			info.processMethod = 1;
		}
//		if(null!=sendInfo.action) {
//			information.setAction(info.action.toString());
//		}
		if(null!=info.appId) {
			information.setAppId(info.appId.toString());
		}
		if(null!=info.body) {
			information.setBody(info.body.toString());
		}
		if(null!=info.bodyHtml) {
			information.setBodyHtml(info.bodyHtml.toString());
		}
		if(null!=info.data) {
			information.setData(info.data.toString());
		}
		if(null!=info.guid) {
			information.setGuid(info.guid.toString());
		}
		if(null!=info.importance) {
			information.setImportance(info.importance);
		}
		if(null!=info.processMethod) {
			information.setProcessMethod(info.processMethod);
		}
		if(null!=info.receiverId) {
			information.setReceiverId(info.receiverId.toString());
		}
		if(null!=info.senderId) {
			information.setSenderId(info.senderId.toString());
		}
		if(null!=info.title) {
			information.setTitle(info.title.toString());
		}
		if(null!=info.titleHtml) {
			information.setTitleHtml(info.titleHtml.toString());
		}
		if(null!=info.type) {
			information.setType(info.type.toString());
		}
		if(null!=info.uri) {
			information.setUri(info.uri.toString());
		}
		if(null!=info.objectId) {
			information.setObjectId(info.objectId.toString());
		}

        //add by wangpeng at 2013-04-23
        if(null!=info.scene){
            information.setScene(info.scene.toString());
        }
		return information;
	}
}
