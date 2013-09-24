package com.borqs.information.dao;


import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;
import com.borqs.information.rpc.service.NotificationsThriftHelper;
import com.borqs.notifications.thrift.NotificationUnreadResult;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class MongoDBStorageTest {

    MongoDBStorage mongoDBStorage;

    @Before
    public void setUp() throws Exception {
        mongoDBStorage = new MongoDBStorage();
    }

    private void AssertEqual(Object a, Object b) {
        if (!a.toString().equals(b.toString())) {
            System.err.println("AssertEqual Error!" + a + ":" + b);
        }
    }

    @Test
    public void testQueryGroup(){
        DBObject dbObject = mongoDBStorage.queryNotifByGroup("10405", "");
        List<NotificationUnreadResult> list =  NotificationsThriftHelper.notifGroupConverter(dbObject);


        System.out.println(list);

    }
    @Test
    public void testQuery() {
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.put("scen","12");
        InformationList informationList = mongoDBStorage.internal_list("0",null,null,null,null,basicDBObject);
        for(Information information:informationList.getInformations()){
            System.out.println(information);
        }
    }

    @Test
    public void testSave() {
        mongoDBStorage.clearCollections();
        String id = "";
        int totalCount = 200;
        //test save new
        for (int i = 0; i < totalCount; i++) {
            Information info = new Information();
            info.setAppId("111");
            info.setSenderId("00_" + i);
            info.setReceiverId("01_" + i);
            info.setType("ketian's type");
            info.setAction("ketian's action");
            info.setTitle("ketian's title");
            info.setDate(System.currentTimeMillis());
            info.setUri("ketian's url");
            info.setProcessed(false);
            info.setProcessMethod(1);
            info.setImportance(200);
            info.setTitleHtml("this is title html");
            info.setBody("this is body");
            info.setBodyHtml("this is body html");
            info.setObjectId("this is object id");
            info.setGuid("this is guid");
            info.setRead(false);
            info.setScene("" + i);
            info.setLastModified(System.currentTimeMillis());
            id = mongoDBStorage.save(info);
        }

        //test list
        InformationList list = mongoDBStorage.list(null);


    }

    @Test
    public void testAll() {
        mongoDBStorage.clearCollections();
        String id = "";
        int totalCount = 200;
        //test save new
        for (int i = 0; i < totalCount; i++) {
            Information info = new Information();
            info.setAppId("111");
            info.setSenderId("00_" + i);
            info.setReceiverId("01_" + i);
            info.setType("ketian's type");
            info.setAction("ketian's action");
            info.setTitle("ketian's title");
            info.setDate(System.currentTimeMillis());
            info.setUri("ketian's url");
            info.setProcessed(false);
            info.setProcessMethod(1);
            info.setImportance(200);
            info.setTitleHtml("this is title html");
            info.setBody("this is body");
            info.setBodyHtml("this is body html");
            info.setObjectId("this is object id");
            info.setGuid("this is guid");
            info.setRead(false);
            info.setLastModified(System.currentTimeMillis());
            id = mongoDBStorage.save(info);
        }
        System.out.println("last id = " + id);

        long timestamp1 = System.currentTimeMillis();
        //test save update
        for (int i = 0; i < totalCount; i++) {
            Information info = new Information();
            info.setId(i + 1);
            info.setAppId("111");
            info.setSenderId("00_" + i);
            info.setReceiverId("01_" + i);
            info.setType("type1");
            info.setAction("ketian's action");
            info.setTitle("ketian's title");
            info.setDate(System.currentTimeMillis());
            info.setUri("ketian's url");
            info.setProcessed(i < 80 && i >= 50);
            info.setProcessMethod(i < 115 ? 1 : 0);
            info.setImportance(200);
            info.setTitleHtml("this is title html");
            info.setBody("this is body");
            info.setBodyHtml("this is body html");
            info.setObjectId("obid");
            info.setGuid("this is guid");
            info.setRead(false);
            info.setLastModified(System.currentTimeMillis());
            id = mongoDBStorage.save(info);
        }

        //test count,delete
        int count = mongoDBStorage.count(null, null);
        AssertEqual(count, 200);

        mongoDBStorage.delete("100");
        mongoDBStorage.delete("101");
        mongoDBStorage.delete("101");

        count = mongoDBStorage.count(null, null);
        AssertEqual(count, 198);

        //test list
        InformationList list = mongoDBStorage.list(null);
        AssertEqual(list.getCount(), 198);

        list = mongoDBStorage.list("01_10");
        AssertEqual(list.getCount(), 1);

        list = mongoDBStorage.list("01_10", "false", null, -1);
        AssertEqual(list.getCount(), 1);

        list = mongoDBStorage.list("01_10", "1", null, -1);
        AssertEqual(list.getCount(), 0);

        list = mongoDBStorage.list(null, "true", null, -1);
        AssertEqual(list.getCount(), 30);

        list = mongoDBStorage.list(null, "1", null, -1);
        AssertEqual(list.getCount(), 30);

        list = mongoDBStorage.list(null, "false", null, -1);
        AssertEqual(list.getCount(), count - 30);

        list = mongoDBStorage.list(null, "0", null, -1);
        AssertEqual(list.getCount(), count - 30);

        //test top
        System.out.println("test top values...");
        list = mongoDBStorage.top(null, "true", 10);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId());
        }
        list = mongoDBStorage.top(null, "0", 0);
        for (Information info : list.getInformations()) {
            System.out.println(info.toString());
        }

        for (int i = 110; i < 130; i++) {
            mongoDBStorage.markProcessed("" + i);
            mongoDBStorage.markRead("" + i);
        }

        //test markProcessed,markRead && listbyid

        long timestamp2 = System.currentTimeMillis();

        for (int i = 10; i < 30; i++) {
            mongoDBStorage.markProcessed("" + i);
            mongoDBStorage.markRead("" + i);
        }
        System.out.println("list by id values...10...");
        list = mongoDBStorage.listById(null, "1", 20L, 10);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " isReaded:" + info.isRead());
        }

        System.out.println("list by id values...0...");
        list = mongoDBStorage.listById(null, "1", 20L, 0);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " isReaded:" + info.isRead());
        }
        System.out.println("list by id values...-10...");
        list = mongoDBStorage.listById(null, "1", 20L, -10);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " isReaded:" + info.isRead());
        }

        int c = mongoDBStorage.countByPosition(20l, null, "true", 1);
        int c2 = mongoDBStorage.countByPosition(20l, null, "true", -1);

        System.out.println("countByPosition..." + c + ".." + c2);

        //test list by time
        System.out.println("list by time.." + timestamp2 + "...0...");
        list = mongoDBStorage.listByTime(null, "1", timestamp2, 0);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " isReaded:" + info.isRead()
                    + " ModifyTime:" + info.getLastModified());

        }

        System.out.println("list by time.." + timestamp2 + "...10...");
        list = mongoDBStorage.listByTime(null, "1", timestamp2, 10);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " isReaded:" + info.isRead()
                    + " ModifyTime:" + info.getLastModified());

        }

        System.out.println("list by time.." + timestamp2 + "...-10...");
        list = mongoDBStorage.listByTime(null, "1", timestamp2, -10);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " isReaded:" + info.isRead()
                    + " ModifyTime:" + info.getLastModified());

        }

        System.out.println("list by time upprocessed.." + timestamp2 + "...-10...");
        list = mongoDBStorage.listByTime(null, "0", timestamp2, -10);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " isReaded:" + info.isRead()
                    + " ModifyTime:" + info.getLastModified());

        }

        list = mongoDBStorage.list(null);
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " processMethod:" + info.getProcessMethod()
                    + " isReaded:" + info.isRead()
                    + " ModifyTime:" + info.getLastModified());

        }
        // test query & replace
        System.out.println("test query & replace ");
        list = mongoDBStorage.query("111", "type1", "01_30", "obid");
        AssertEqual(1, list.getCount());
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " processMethod:" + info.getProcessMethod()
                    + " isReaded:" + info.isRead()
                    + " ModifyTime:" + info.getLastModified());

            info.setProcessed(!info.isProcessed());
            info.setRead(!info.isRead());
            mongoDBStorage.replace(info);
        }
        list = mongoDBStorage.query("111", "type1", "01_30", "obid");
        for (Information info : list.getInformations()) {
            System.out.println(info.getId()
                    + " isProcessed:" + info.isProcessed()
                    + " processMethod:" + info.getProcessMethod()
                    + " isReaded:" + info.isRead()
                    + "ModifyTime:" + info.getLastModified());
        }

    }

}
