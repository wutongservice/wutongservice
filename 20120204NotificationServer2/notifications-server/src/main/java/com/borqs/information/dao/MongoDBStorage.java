package com.borqs.information.dao;

import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;
import com.mongodb.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MongoDBStorage implements IInformationsStorageManager {
    private static Logger logger = LoggerFactory.getLogger(MongoDBStorage.class);

    private static final String DB_NAME = "notification";
    private static final String COLLECTION = "notifications";
    private static final String COLLECTION_AUTOID = "notificationsautoid";
    private Mongo mongo;
    private DB db;
    private DBCollection collection;
    private DBCollection idcollection;
    private static final String COl_KEY = "_id";

    private String host = "127.0.0.1";
    private int port = 27017;

    private String userName;
    private String password;

    private MongoOptions option;

    private List<ServerAddress> replicaSetSeeds;

    public MongoDBStorage() {
        try {
            // create Mongo instance

            if (null != replicaSetSeeds) {
                // replica mode
                if (null != option) {
                    mongo = new Mongo(replicaSetSeeds, option);
                } else {
                    mongo = new Mongo(replicaSetSeeds);
                }
            } else {
                // single mongodb node mode
                if (null != option) {
                    mongo = new Mongo(host, option);
                } else {
                    mongo = new Mongo(host, port);
                }
            }

            // if set user name and password, try to authenticate it.
            boolean auth = false;
            if (null == userName && null == password) {
                auth = true;
            } else {
                auth = db.authenticate(userName, password.toCharArray());
            }

            // create db, collection and ID collection instance
            if (auth) {
                db = mongo.getDB(DB_NAME);
                collection = db.getCollection(COLLECTION);
                idcollection = db.getCollection(COLLECTION_AUTOID);
            } else {
                throw new Exception("failed to authenticate account!");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (MongoException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOption(MongoOptions option) {
        this.option = option;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void clearCollections() {
        collection.drop();
        idcollection.drop();
    }

    @Override
    public void delete(String id) {
        BasicDBObject dbobj = new BasicDBObject(COl_KEY, Long.parseLong(id));
        collection.remove(dbobj);
    }

    private String getInfoDBGuid(Information info) {
        String guid = info.getGuid();
        if (null != guid && !"".equals(guid)) {
            guid = guid + "-" + info.getReceiverId();
        }

        return guid;
    }

    private DBObject info2object(Information info) {
        DBObject dbObj = new BasicDBObject();

        dbObj.put(COl_KEY, info.getId());
        dbObj.put(Information.INFO_APP_ID, info.getAppId());
        dbObj.put(Information.INFO_SENDER_ID, info.getSenderId());
        dbObj.put(Information.INFO_RECEIVER_ID, info.getReceiverId());
        dbObj.put(Information.INFO_TYPE, info.getType());
        dbObj.put(Information.INFO_ACTION, info.getAction());
        dbObj.put(Information.INFO_TITLE, info.getTitle());
        dbObj.put(Information.INFO_DATE, info.getDate());
        dbObj.put(Information.INFO_DATA, info.getData());
        dbObj.put(Information.INFO_URI, info.getUri());
        dbObj.put(Information.INFO_PROCESSED, info.isProcessed());
        dbObj.put(Information.INFO_PROCESS_METHOD, info.getProcessMethod());
        dbObj.put(Information.INFO_IMPORTANCE, info.getImportance());
        dbObj.put(Information.INFO_TITLE_HTML, info.getTitleHtml());
        dbObj.put(Information.INFO_BODY, info.getBody());
        dbObj.put(Information.INFO_BODY_HTML, info.getBodyHtml());
        dbObj.put(Information.INFO_OBJECT_ID, info.getObjectId());
        String infoDBGuid = getInfoDBGuid(info);
        logger.info("GUID is " + infoDBGuid);
        dbObj.put(Information.INFO_GUID, infoDBGuid);
        dbObj.put(Information.INFO_READED, info.isRead());
        dbObj.put(Information.INFO_LAST_MODIFIED, System.currentTimeMillis());

        //add by WangPeng at 2013-04-23
        dbObj.put(Information.INFO_SCENE, info.getScene());
        dbObj.put(Information.INFO_IMAGE_URL, info.getImageUrl());

        return dbObj;
    }

    private String getStringValue(Object o) {
        if (o == null)
            return null;
        return (String) o;
    }

    private Long getLongValue(Object o) {
        if (o == null)
            return null;
        return (Long) o;
    }

    private Integer getIntegerValue(Object o) {
        if (o == null)
            return null;
        return (Integer) o;
    }

    private Boolean getBooleanValue(Object o) {
        if (o == null)
            return null;
        return Boolean.parseBoolean(o.toString());
    }

    private Information object2info(DBObject dbObj) {
        Information info = new Information();
        //logger.info("--------------" + dbObj.get(COl_KEY));
        //logger.info(dbObj.toString());
        info.setId(Long.valueOf(dbObj.get(COl_KEY) + ""));
        info.setAppId(getStringValue(dbObj.get(Information.INFO_APP_ID)));
        info.setSenderId(getStringValue(dbObj.get(Information.INFO_SENDER_ID)));
        info.setReceiverId(getStringValue(dbObj
                .get(Information.INFO_RECEIVER_ID)));
        info.setType(getStringValue(dbObj.get(Information.INFO_TYPE)));
        info.setAction(getStringValue(dbObj.get(Information.INFO_ACTION)));
        info.setTitle(getStringValue(dbObj.get(Information.INFO_TITLE)));
        info.setData(getStringValue(dbObj.get(Information.INFO_DATA)));
        info.setDate(Long.valueOf(dbObj.get(Information.INFO_DATE) + ""));
        info.setUri(getStringValue(dbObj.get(Information.INFO_URI)));
        info.setProcessed(getBooleanValue(dbObj.get(Information.INFO_PROCESSED)));
        info.setProcessMethod(getIntegerValue(dbObj
                .get(Information.INFO_PROCESS_METHOD)));
        info.setImportance(getIntegerValue(dbObj
                .get(Information.INFO_IMPORTANCE)));

        info.setTitleHtml(getStringValue(dbObj.get(Information.INFO_TITLE_HTML)));
        info.setBody(getStringValue(dbObj.get(Information.INFO_BODY)));
        info.setBodyHtml(getStringValue(dbObj.get(Information.INFO_BODY_HTML)));
        info.setObjectId(getStringValue(dbObj.get(Information.INFO_OBJECT_ID)));
        info.setGuid(getStringValue(dbObj.get(Information.INFO_GUID)));
        info.setRead(getBooleanValue(dbObj.get(Information.INFO_READED)));
        info.setLastModified(Long.valueOf(dbObj
                .get(Information.INFO_LAST_MODIFIED) + ""));

        //add by WangPeng at 2013-04-23
        info.setScene(getStringValue(dbObj.get(Information.INFO_SCENE)));
        info.setImageUrl(getStringValue(dbObj.get(Information.INFO_IMAGE_URL)));

        return info;
    }

    @Override
    public String save(Information info) {
        logger.info("---------------------------------------");
        logger.info("----------------"+info.toString()+"--------------------");
        logger.info("---------------------------------------");
        DBObject dbObj = info2object(info);
        long id = info.getId();
        /*
           * if(id<=0)//add { id = getAutoID(); dbObj.put(COl_KEY, id);
           * wr=collection.insert(dbObj); } else//update { BasicDBObject query =
           * new BasicDBObject(COl_KEY,id); wr = collection.update(query , dbObj,
           * false, false); }
           */
        if (id <= 0) {
            if (null != info.getGuid()) {
                String guid = getInfoDBGuid(info);
                BasicDBObject dbobj = new BasicDBObject(Information.INFO_GUID,
                        guid);
                collection.remove(dbobj);
            }
            id = getAutoID();
        }
        dbObj.put(COl_KEY, id);
        WriteResult wr = collection.save(dbObj);
        if (wr.getError() != null) {
            System.err.println(wr.getError());
        }
        return String.valueOf(id);
    }

    private long getAutoID() {
        BasicDBObject query = new BasicDBObject();
        query.put("name", COLLECTION);

        BasicDBObject update = new BasicDBObject();
        update.put("$inc", new BasicDBObject("value", 1L));

        DBObject dbObject2 = idcollection.findAndModify(query, null, null,
                false, update, true, true);

        Long id = (Long) dbObject2.get("value");
        return id;
    }

    @Override
    public InformationList list(String userID) {
        return internal_list(userID, null, null, null, null);
    }

    private DBCursor query(String userId, String status, Long from,
                           Integer size, BasicDBObject extraCondition) {
        return query(null, userId, status, from, size, extraCondition);
    }

    private DBCursor query(String appId, String userId, String status, Long from,
                           Integer size, BasicDBObject extraCondition) {
        BasicDBObject query = new BasicDBObject();

        if (null != appId && !"0".equals(appId)) {
            query.put(Information.INFO_APP_ID, appId);
        }
        if (null != userId) {
            query.put(Information.INFO_RECEIVER_ID, userId);
        }
        if (null != status) {
            Boolean value = Boolean.parseBoolean(status);
            if (status.equals("0")) {
                value = false;
            } else if (status.equals("1")) {
                value = true;
            }
            query.put(Information.INFO_PROCESSED, value);
        }

        if (null != extraCondition) {
            query.putAll((Map) extraCondition);
        }
        BasicDBObject sort = new BasicDBObject(Information.INFO_LAST_MODIFIED, -1);
        logger.info("---------query------" + query);
        DBCursor cursor = collection.find(query).sort(sort);

        if (size > 0) {
            cursor = cursor.limit(size).skip(from.intValue());
        }
        return cursor;
    }

    private InformationList internal_list(String userId, String status,
                                          Long from, Integer size, BasicDBObject extraCondition) {
        return internal_list(null, userId, status, from, size, extraCondition);
    }

    public InformationList internal_list(String appId, String userId, String status,
                                         Long from, Integer size, BasicDBObject extraCondition) {
        if (null == from) {
            from = 0l;
        }
        if (null == size) {
            size = 0;
        }
        DBCursor cursor = query(appId, userId, status, from, size, extraCondition);

        List<Information> list = new ArrayList<Information>();

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            Information info = object2info(object);
            logger.info("info is------------------------------ " + info.toString());
            list.add(info);
        }
        return new InformationList(list);
    }

    @Override
    public InformationList list(String userId, String status, Long from, Integer size) {
        return list(null, userId, status, from, size);
    }

    @Override
    public InformationList list(String appId, String userId, String status, Long from, Integer size) {
        // default to read unprocessed items
        if (null == status || "".equals(status.trim())) {
            status = "0";
        }

        // default start item is from zero position
        if (null == from) {
            from = 0L;
        }

        // default to read twenty items
        if (null == size) {
            size = 20;
        }
        return internal_list(appId, userId, status, from, size, null);
    }

    @Override
    public InformationList top(String userId, String status, Integer topNum) {
        return top(null, userId, status, topNum);
    }

    @Override
    public InformationList userTop(String appId, String userId, Integer type, String status,String scene, Integer topNum) {
        if (null == status || "".equals(status.trim())) {
            status = "0";
        }

        // default to read top five items
        if (null == topNum || topNum <= 0) {
            topNum = 5;
        }
        BasicDBObject extraCondition = new BasicDBObject();
        if (0 == type) {
            extraCondition.put(Information.INFO_DATA, java.util.regex.Pattern.compile("," + userId + ","));
        } else if (1 == type) {
            extraCondition.put(Information.INFO_DATA, new BasicDBObject("$not", java.util.regex.Pattern.compile("," + userId + ",")));
        }

        //charge scene
        if (StringUtils.isNotBlank(scene) && !StringUtils.equals("0", scene)) {
            //logger.info("--------------------userTop-------------------------scene="+scene);
            extraCondition.put(Information.INFO_SCENE, scene);
        }
        return internal_list(appId, userId, status, 0l, topNum, extraCondition);
    }

    @Override
    public InformationList top(String appId, String userId, String status, Integer topNum) {
        // default to read unprocessed items
        if (null == status || "".equals(status.trim())) {
            status = "0";
        }

        // default to read top five items
        if (null == topNum || topNum <= 0) {
            topNum = 5;
        }

        return internal_list(appId, userId, status, 0l, topNum, null);
    }

    @Override
    public int count(String userId, String status) {
        DBCursor cursor = query(userId, status, 0l, -1, null);
        return cursor.count();
    }

    @Override
    public int count(String appId, String userId, String status) {
        DBCursor cursor = query(appId, userId, status, 0l, -1, null);
        return cursor.count();
    }

    private int update(DBObject query, DBObject field) {
        BasicDBObject update = new BasicDBObject();
        update.put("$set", field);
        WriteResult wr = collection.update(query, update, false, true);

        if (wr.getError() != null) {
            logger.error(wr.getError());
        }
        return wr.getN();
    }

    @Override
    public void markProcessed(String id) {
        BasicDBObject query = new BasicDBObject(COl_KEY, Long.parseLong(id));
        BasicDBObject field = new BasicDBObject();

        field.put(Information.INFO_PROCESSED, true);
        field.put(Information.INFO_LAST_MODIFIED, System.currentTimeMillis());
        update(query, field);
    }

    @Override
    public void markRead(String id) {
        BasicDBObject query = new BasicDBObject(COl_KEY, Long.parseLong(id));
        BasicDBObject field = new BasicDBObject();

        field.put(Information.INFO_READED, true);
        //field.put(Information.INFO_LAST_MODIFIED, System.currentTimeMillis());
        update(query, field);

    }

    @Override
    public InformationList listById(String userId, String status, Long mid,
                                    Integer count) {
        return listById(null, userId, status, mid, count);
    }

    @Override
    public InformationList listById(String appId, String userId, String status, Long mid, Integer count) {
        InformationList informations = new InformationList();

        // default to read unprocessed items
        if (null == status || "".equals(status.trim())) {
            status = "0";
        }

        // default start item is from zero position
        if (null == mid) {
            return internal_list(appId, userId, status, null, null, null);

        }

        int total = countByPosition(mid, appId, userId, status, count);
        logger.info("total is " + total);
        if (total <= 0) {
            informations.setTotal(total);
            return informations;
        }

        int start = 0;
        String op = null;
        BasicDBObject extraCondition = new BasicDBObject();
        if (null == count || 0 == count) {
            op = "$gte";
        } else if (count < 0) {
            op = "$lt";
            count = -count;
        } else {
            op = "$gte";
            start = total - count;
            if (start < 0) {
                start = 0;
            }
        }
        extraCondition.put(COl_KEY, new BasicDBObject(op, mid));

        informations = internal_list(appId, userId, status, (long) start, count,
                extraCondition);
        if (null != informations) {
            informations.setTotal(total);
        }

        return informations;
    }

    @Override
    public InformationList listByTime(String userId, String status, Long time,
                                      Integer count) {
        return listByTime(null, userId, status, time, count);
    }

    @Override
    public InformationList listByTime(String appId, String userId, String status, Long time,
                                      Integer count) {
        InformationList informations = new InformationList();

        // default to read unprocessed items
        if (null == status || "".equals(status.trim())) {
            status = "0";
        }

        // default start item is from zero position
        if (null == time) {
            return internal_list(appId, userId, status, null, null, null);

        }
        Date from = new Date(time);

        int total = countByTime(from, appId, userId, status, count);
        logger.info("total is " + total);
        if (total <= 0) {
            informations.setTotal(total);
            return informations;
        }
        BasicDBObject extraCondition = new BasicDBObject();
        int start = 0;
        extraCondition = new BasicDBObject();
        String op = null;
        if (null == count || 0 == count) {
            op = "$gte";
        } else if (count < 0) {
            op = "$lt";
            count = -count;
        } else {
            op = "$gte";
            start = total - count;
            if (start < 0) {
                start = 0;
            }
        }
        extraCondition.put(Information.INFO_LAST_MODIFIED, new BasicDBObject(
                op, time));

        informations = internal_list(appId, userId, status, (long) start, count,
                extraCondition);
        if (null != informations) {
            informations.setTotal(total);
        }
//		if (informations.getInformations().size() > 0) {
//			BasicDBList values = new BasicDBList();
//			for (Information info : informations.getInformations()) {
//				values.add(info.getId());
//			}
//
//			BasicDBObject query = new BasicDBObject(COl_KEY, new BasicDBObject(
//					"$in", values));
//			query.put(Information.INFO_PROCESS_METHOD, 1);
//			BasicDBObject field = new BasicDBObject();
//			field.put(Information.INFO_READED, true);
//			field.put(Information.INFO_PROCESSED, true);
//			int c = update(query, field);
//
//		}
        return informations;

    }

    /**
     * modify by wangpeng
     *
     * @param userId
     * @param status
     * @param type
     * @param scene
     * @param time
     * @param count
     * @return
     */
    @Override
    public InformationList userListByTime(String userId, String status, int type, String scene, Long time, Integer count) {
        InformationList informations = new InformationList();

        // default to read unprocessed items
        if (null == status || "".equals(status.trim())) {
            status = "0";
        }

        // default start item is from zero position
        if (null == time) {
            return internal_list(userId, status, null, null, null);

        }
        Date from = new Date(time);

        int total = userCountByTime(from, null, userId, status, count, type, scene);
        logger.info("total is " + total);
        if (total <= 0) {
            informations.setTotal(total);
            return informations;
        }

        BasicDBObject extraCondition;
        int start = 0;
        extraCondition = new BasicDBObject();
        String op = null;
        if (null == count || 0 == count) {
            op = "$gte";
        } else if (count < 0) {
            op = "$lt";
            count = -count;
        } else {
            op = "$gte";
            start = total - count;
            if (start < 0) {
                start = 0;
            }
        }

        extraCondition.put(Information.INFO_LAST_MODIFIED, new BasicDBObject(
                op, time));
        // -----------add filter by type ----------------
        if (0 == type) {
            extraCondition.put(Information.INFO_DATA, java.util.regex.Pattern.compile("," + userId + ","));
        } else if (1 == type) {
            extraCondition.put(Information.INFO_DATA, new BasicDBObject("$not", java.util.regex.Pattern.compile("," + userId + ",")));
        }

        // add by wangpeng at 2013-04-24
        if (StringUtils.isNotBlank(scene) && !StringUtils.equals("0", scene)) {
            extraCondition.put(Information.INFO_SCENE, scene);
        }
        //logger.info("extraCondition is------------------------------ " + extraCondition.toString());
        informations = internal_list(userId, status, (long) start, count,
                extraCondition);
        if (null != informations) {
            informations.setTotal(total);
        }
        logger.info("informations is------------------------------ " + informations.toString());
        return informations;
    }


    /**
     * modify by wangpeng
     *
     * @param userId
     * @param status
     * @param type
     * @param scene
     * @param read
     * @param time
     * @param count
     * @return
     */
    @Override
    public InformationList userReadListByTime(String userId, String status, int type, String scene, int read, Long time, Integer count) {
        InformationList informations = new InformationList();

        // default to read unprocessed items
        if (null == status || "".equals(status.trim())) {
            status = "0";
        }

        // default start item is from zero position
        if (null == time) {
            return internal_list(userId, status, null, null, null);

        }
        Date from = new Date(time);

        int total = userCountByTime(from, null, userId, status, count, type, scene);
        logger.info("total is " + total);
        if (total <= 0) {
            informations.setTotal(total);
            return informations;
        }

        BasicDBObject extraCondition;
        int start = 0;
        extraCondition = new BasicDBObject();
        String op = null;
        if (null == count || 0 == count) {
            op = "$gte";
        } else if (count < 0) {
            op = "$lt";
            count = -count;
        } else {
            op = "$gte";
            start = total - count;
            if (start < 0) {
                start = 0;
            }
        }

        extraCondition.put(Information.INFO_LAST_MODIFIED, new BasicDBObject(
                op, time));
        // -----------add filter by type ----------------
        if (0 == type) {
            extraCondition.put(Information.INFO_DATA, java.util.regex.Pattern.compile("," + userId + ","));
        } else if (1 == type) {
            extraCondition.put(Information.INFO_DATA, new BasicDBObject("$not", java.util.regex.Pattern.compile("," + userId + ",")));
        }

        if (1 == read) {
            extraCondition.put(Information.INFO_READED, true);
        } else {
            extraCondition.put(Information.INFO_READED, false);
        }

        // add by wangpeng at 2013-04-24
        if (StringUtils.isNotBlank(scene) && !StringUtils.equals("0", scene)) {
            extraCondition.put(Information.INFO_SCENE, scene);
        }

        //logger.info("extraCondition is------------------------------ " + extraCondition.toString());
        informations = internal_list(userId, status, (long) start, count,
                extraCondition);
        if (null != informations) {
            informations.setTotal(total);
        }

        return informations;
    }

    @Override
    public int countByPosition(Long mid, String userId, String status,
                               Integer dir) {
        return countByPosition(mid, null, userId, status, dir);
    }

    @Override
    public int countByPosition(Long mid, String appId, String userId,
                               String status, Integer dir) {
        BasicDBObject extraCondition = null;
        if (null != appId && !"0".equals(appId)) {
            extraCondition = new BasicDBObject();
            extraCondition.put(Information.INFO_APP_ID, appId);
        }
        if (null != mid) {
            if (null == extraCondition) {
                extraCondition = new BasicDBObject();
            }
            String op = null;
            if (null == dir || dir >= 0) {
                op = "$gte";
            } else {
                op = "$lt";
            }
            extraCondition.put(COl_KEY, new BasicDBObject(op, mid));
        }
        DBCursor cursor = query(userId, status, 0l, -1, extraCondition);
        return cursor.count();
    }

    private int countByTime(Date from, String userId, String status, Integer dir) {
        return countByTime(from, null, userId, status, dir);
    }

    private int countByTime(Date from, String appId, String userId, String status, Integer dir) {
        BasicDBObject extraCondition = null;
        if (null != appId && !"0".equals(appId)) {
            extraCondition = new BasicDBObject();
            extraCondition.put(Information.INFO_APP_ID, appId);
        }
        if (null != from) {
            extraCondition = new BasicDBObject();
            String op = null;
            if (null == dir || dir >= 0) {
                op = "$gte";
            } else {
                op = "$lt";
            }
            extraCondition.put(Information.INFO_LAST_MODIFIED,
                    new BasicDBObject(op, from.getTime()));
        }
        DBCursor cursor = query(userId, status, 0l, -1, extraCondition);
        return cursor.count();
    }

    private int userCountByTime(Date from, String appId, String userId, String status, Integer dir, Integer type, String scene) {
        BasicDBObject extraCondition = null;
        if (null != appId && !"0".equals(appId)) {
            extraCondition = new BasicDBObject();
            extraCondition.put(Information.INFO_APP_ID, appId);
        }
        if (null != from) {
            extraCondition = new BasicDBObject();
            String op = null;
            if (null == dir || dir >= 0) {
                op = "$gte";
            } else {
                op = "$lt";
            }
            extraCondition.put(Information.INFO_LAST_MODIFIED,
                    new BasicDBObject(op, from.getTime()));

            if (type == 0) {
                extraCondition.put(Information.INFO_DATA, java.util.regex.Pattern.compile("," + userId + ","));
            }
            if (StringUtils.isNotBlank(scene) && !StringUtils.equals("0", scene)) {
                extraCondition.put(Information.INFO_SCENE, scene);
            }
        }
        //logger.info("extraCondition is------------------------------ " + extraCondition.toString());
        DBCursor cursor = query(userId, status, 0l, -1, extraCondition);
        return cursor.count();
    }


    @Override
    public InformationList query(String appId, String type, String receiverId,
                                 String objectId) {
        BasicDBObject extraCondition = new BasicDBObject();
        extraCondition.put(Information.INFO_APP_ID, appId);
        extraCondition.put(Information.INFO_TYPE, type);

        //add by wangpeng at 2013-03-25 for support batch receiverId

        if (receiverId.split(",").length > 1) {
            String[] receiverArray = receiverId.split(",");
            extraCondition.put(Information.INFO_RECEIVER_ID, new BasicDBObject("$in", receiverArray));
        } else
            extraCondition.put(Information.INFO_RECEIVER_ID, receiverId);

        extraCondition.put(Information.INFO_OBJECT_ID, objectId);
        //logger.info("---------------------------"+extraCondition.toMap().toString()+"--------------------------");
        return internal_list(null, null, null, null, extraCondition);
    }

    @Override
    public String replace(Information info) {
        logger.info("the saving data:" + info);

        BasicDBObject query = new BasicDBObject();
        query.put(Information.INFO_APP_ID, info.getAppId());
        query.put(Information.INFO_TYPE, info.getType());
        query.put(Information.INFO_RECEIVER_ID, info.getReceiverId());
        query.put(Information.INFO_OBJECT_ID, info.getObjectId());

        DBObject field = info2object(info);
        field.removeField(COl_KEY);
        int updated = update(query, field);
        logger.info("updated count is " + updated);
        if (0 >= updated) {
            save(info);
        }

        InformationList list = internal_list(null, null, null, null, query);
        logger.info("internal_list:\n" + list.description());

        StringBuilder values = new StringBuilder();
        for (Information i : list.getInformations()) {
            if (values.length() == 0) {
                values.append(i.getId());
            } else {
                values.append(",").append(i.getId());
            }

        }
        return values.toString();
    }

    @Override
    public DBObject queryNotifByGroup(String userId, String scene) {
        BasicDBObject key = new BasicDBObject();
        key.put("scene", true);

        BasicDBObject initial = new BasicDBObject();
        initial.put("readcount", 0);

        BasicDBObject condition = new BasicDBObject();
        condition.put("read", false);
        condition.put("processed", false);
        condition.put("receiverId", userId);
        condition.put("scene",new BasicDBObject("$ne",null));
        //condition.put("status",status);
        if(StringUtils.isNotBlank(scene) && scene.length()>1)
            condition.put("scene", scene);

        String reduceString = "function(doc,out){out.readcount += 1;}";


        DBObject dbObject = collection.group(key, condition, initial, reduceString);
        return dbObject;
    }

    public void closeMongo() {
        mongo.close();
    }
}
