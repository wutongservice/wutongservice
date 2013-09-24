package com.borqs.information.rpc.service;

import com.borqs.information.rest.bean.Information;
import com.borqs.notifications.thrift.Info;
import com.borqs.notifications.thrift.NotificationUnreadResult;
import com.mongodb.CommandResult;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationsThriftHelper {
    public static List<Info> convertToInfos(List<Information> informations) {
        List<Info> infos = new ArrayList<Info>();
        if (null == informations) {
            return infos;
        }

        for (Information information : informations) {
            Info info = convertToInfo(information);
            infos.add(info);
        }

        return infos;
    }

    public static Info convertToInfo(Information info) {
        Info inf = new Info();
        inf.setId(info.getId());

        inf.setAppId(info.getAppId());
        inf.setType(info.getType());
        inf.setReceiverId(info.getReceiverId());
        inf.setSenderId(info.getSenderId());

        // action
        inf.setUri(info.getUri());

        // data
        inf.setTitle(info.getTitle());
        inf.setTitleHtml(info.getTitleHtml());
        inf.setBody(info.getBody());
        inf.setBodyHtml(info.getBodyHtml());
        inf.setData((null == info.getData()) ? "" : info.getData());
        inf.setObjectId(info.getObjectId());

        // status
        inf.setImportance(info.getImportance());
        inf.setProcessed(info.isProcessed());
        inf.setProcessMethod(info.getProcessMethod());
        inf.setRead(info.isRead());

        inf.setDate(info.getDate());
        inf.setLastModified(info.getLastModified());

        // deprecated
        inf.setAction(info.getAction());
        inf.setGuid(info.getGuid());

        inf.setPush(info.isPush());

        //add by WangPeng at 2013-04-23
        inf.setScene(info.getScene());
        //add by wangpeng at 2013-06-03
        inf.setImageUrl(info.getImageUrl());

        return inf;
    }

    public static Information convertToInformation(Info info) {
        Information information = new Information();
        if (0 == info.getImportance()) {
            info.setImportance(30);
        }
        if (0 == info.getProcessMethod()) {
            info.setProcessMethod(1);
        }

        information.setId(info.getId());

        if (null != info.getAppId()) {
            information.setAppId(info.getAppId());
        }
        if (null != info.getGuid()) {
            information.setGuid(info.getGuid());
        }
        if (null != info.getType()) {
            information.setType(info.getType());
        }
        if (null != info.getAction()) {
            information.setAction(info.getAction());
        }
        if (null != info.getUri()) {
            information.setUri(info.getUri());
        }
        if (null != info.getReceiverId()) {
            information.setReceiverId(info.getReceiverId());
        }
        if (null != info.getSenderId()) {
            information.setSenderId(info.getSenderId());
        }
        if (null != info.getObjectId()) {
            information.setObjectId(info.getObjectId());
        }

        if (null != info.getTitle()) {
            information.setTitle(info.getTitle());
        }
        if (null != info.getTitleHtml()) {
            information.setTitleHtml(info.getTitleHtml());
        }
        if (null != info.getBody()) {
            information.setBody(info.getBody());
        }
        if (null != info.getBodyHtml()) {
            information.setBodyHtml(info.getBodyHtml());
        }
        if (null != info.getData()) {
            information.setData(info.getData());
        }

        information.setImportance(info.getImportance());
        information.setProcessed(info.isProcessed());
        information.setProcessMethod(info.getProcessMethod());
        information.setRead(info.isRead());

        information.setDate(info.getDate());
        information.setLastModified(info.getLastModified());

        information.setPush(info.isPush());

        //add by WangPeng at 2013-04-23
        information.setScene(info.getScene());

        return information;
    }

    public static List<NotificationUnreadResult> notifGroupConverter(DBObject dbObject) {
        List<NotificationUnreadResult> list = new ArrayList<NotificationUnreadResult>();
        if (dbObject == null)
            return list;

        for (String str : dbObject.keySet()) {
            CommandResult dbList = (CommandResult) dbObject.get(str);

            NotificationUnreadResult n = new NotificationUnreadResult();

            n.setReadcount(String.valueOf(dbList.get("readcount")));
            n.setScene((String) dbList.get("scene"));
            list.add(n);

        }
        return list;
    }
}
