package com.borqs.server.platform.impl.account;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.platform.account2.User;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccountMigration extends WebMethodServlet {
    private static final Logger L = LoggerFactory.getLogger(AccountMigration.class);
    // db
    private UserDb db;
    protected final GenericTransceiverFactory transceiverFactory = new GenericTransceiverFactory();
    private StaticFileStorage profileImageStorage;
    private StaticFileStorage sysIconStorage;
    private StaticFileStorage linkImgStorage;
    private String linkImgAddr;
    private String serverHost;
    private String qiupuUid;
    private String qiupuParentPath;


    private String dbStr;
    private Connection con;
    private String accountTable;
    private ConnectionFactory connectionFactory;
    AccountImpl account = new AccountImpl();

    @Override
    public void init() {

        Configuration conf = getConfiguration();
        db = new UserDb();
        db.setConfig(conf);
        db.init();
        this.dbStr = ConnectionFactory.getConnectionString(conf.getString("db.account2", null));
        try {
            this.con = DriverManager.getConnection(dbStr);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.accountTable = conf.getString("db.account2.userTable", "user2");


        serverHost = conf.getString("server.host", "api.borqs.com");
        qiupuUid = conf.getString("qiupu.uid", "102");
        qiupuParentPath = conf.getString("qiupu.parent", "/home/zhengwei/data/apk/com/borqs/qiupu/");

        transceiverFactory.setConfig(conf);
        transceiverFactory.init();

        profileImageStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.profileImageStorage", ""));
        profileImageStorage.init();

        sysIconStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.sysIconStorage", ""));
        sysIconStorage.init();

        linkImgStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.linkImgStorage", ""));
        linkImgStorage.init();

        linkImgAddr = conf.getString("platform.servlet.linkImgAddr", "");

        account.setConfig(this.getConfiguration());
    }

    /**
     * migration method
     */
    @WebMethod("account2migration/migration")
    public void migrationOldData2New() {
        boolean ignoreErrorFlag = true;

        RecordSet users = null;
        List<User> userList = new ArrayList<User>();


        List<String> errorList = new ArrayList<String>();
        try {
            users = platform().findAllUserIds(false);
            List list = new ArrayList();
            /*for (int i = 0; i < users.size(); i++) {
                Record user = (Record) users.get(i);
                Long longId = (Long) user.get("user_id");

                //account.deleteUserMigration(user.getInt("user_id"));
            }*/


            //if any exception arises user follow code to debug
            //users = platform().getUsers("180", "180", Platform.USER_ALL_COLUMNS);
            for (Record strId : users) {
                String id = String.valueOf(strId.get("user_id"));
                Record record = platform().getUser(id, id, Platform.USER_ALL_COLUMNS);
                User user = null;
                try {
                    user = AccountConverter.converterRecord2User(record);
                } catch (Exception e) {
                    errorList.add(record.get("user_id") + "出现错误！" + e.getMessage());
                    L.error("Record—>User出现错误！id =" + record.get("user_id"));
                    continue;
                }
                userList.add(user);
            }
            /*for(User user:userList){
                L.debug("---------------------------"+user.getPhoto().getLargeUrl());
            L.debug("---------------------------"+user.getPhoto().getMiddleUrl());
            L.debug("---------------------------"+user.getPhoto().getSmallUrl());
            }*/

            if (errorList.size() < 1 || ignoreErrorFlag == true) {
                //读取配置文件中的数据库的配置，向目标数据库写入数据
                account.setConfig(this.getConfiguration());

                for (User user : userList)
                    account.createUserMigration(user);
            } else if (errorList.size() > 1 || ignoreErrorFlag == true) {
                L.error("Record—>User出现错误！all =" + errorList.toString());
            }
        } catch (AvroRemoteException e) {
            e.printStackTrace();
        }
        this.getConfiguration();
    }

    @WebMethod("account2migration/migration/sort_key")
    public boolean migrationSortKey() {

        try {

            List list = new ArrayList();
            long[] userIds = account.getAllUserIds();
            List<User> userList = account.getUsers(userIds);

            //读取配置文件中的数据库的配置，向目标数据库写入数据
            account.setConfig(this.getConfiguration());

            for (User user : userList){
                User user0 = new User();
                user0.setUserId(user.getUserId());

                if(user.getName()!=null)
                    user0.setName(user.getName());
                
                account.updateSortKeyMigrate(user0);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @WebMethod("account2migration/migration/displayname")
    public boolean migrationDisplayName() {

        try {

            List list = new ArrayList();
            long[] userIds = account.getAllUserIds();
            List<User> userList = account.getUsers(userIds);

            //读取配置文件中的数据库的配置，向目标数据库写入数据
            account.setConfig(this.getConfiguration());

            for (User user : userList){
                User user0 = new User();
                user0.setUserId(user.getUserId());
                user0.setName(user.getName());

                account.updateDisplayNameMigrate(user0);
            }
            return true;
        } catch (Exception e) {
            L.debug("ssss",e.getMessage());
            L.debug("222222",e.getStackTrace());
            e.printStackTrace();
            return false;
        }
    }



    private Platform platform() {
        Platform p = new Platform(transceiverFactory);
        p.setConfig(getConfiguration());
        return p;
    }

    @WebMethod("account2migration/CompareUser")
    public void CompareUserTest() {
        Record user = null;
        try {
            user = platform().getUser("10", "10", Platform.USER_ALL_COLUMNS);
        } catch (AvroRemoteException e) {
            e.printStackTrace();
        }
        //User userFromRecord = AccountConverter.converterRecord2User(user);
        //User userObj = account.getUser(8);

        //L.debug(userFromRecord.toString());
        JsonNode jsonNode = JsonUtils.parse(user.getString("address"));
        if (jsonNode.isArray()) {
            Iterator<JsonNode> iterator = jsonNode.getElements();
            while (iterator.hasNext()) {
                JsonNode jn = iterator.next();
                L.debug(jn.get("street").getTextValue());
            }
        }
        //L.debug(userObj.toString());
    }
}
