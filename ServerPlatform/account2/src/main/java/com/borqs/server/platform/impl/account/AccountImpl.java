package com.borqs.server.platform.impl.account;


import com.borqs.server.E;
import com.borqs.server.ServerException;
import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.Hanyu2Pinyin;
import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.account.AccountException;
import com.borqs.server.platform.account2.AccountLogic;
import com.borqs.server.platform.account2.User;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccountImpl extends ConfigurableBase implements AccountLogic {
    private static final Logger L = LoggerFactory.getLogger(AccountImpl.class);
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


    public void init() {

        Configuration conf = getConfig();
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

    }

    @Override
    public void setConfig(Configuration conf) {
        super.setConfig(conf);
        db = new UserDb();
        db.setConfig(conf);
    }

    public AccountImpl() {
    }

    /**
     * 测试createUser
     *
     * @return
     */
    /*@WebMethod("account2/create")
    public String createUserTest() {
        final String NICKNAME = "peng.wang";
        User user = new User();
        user.setPassword("sssssss");
        user.setName(new NameInfo("鹏", "王"));
        user.setNickname(NICKNAME);
        this.createUser(user);
        return "test";
    }*/
    @Override
    public User createUser(final User user0) {
        String disPlay_name = user0.getDisplayName();
        Hanyu2Pinyin hanyu = new Hanyu2Pinyin();
        String sort_key = hanyu.getStringPinYin(disPlay_name);
        sort_key += disPlay_name;
        user0.setAddon("sort_key", sort_key);
        final User user = user0 != null ? user0.copy() : null;

        ParamChecker.notNull("user", user);
        ParamChecker.notEmpty("user.password", user.getPassword());


        User r = null;
        try {
            r = db.createUser(user);
        } catch (SQLException e) {
            L.error("-------------------------------create user error--------------------" + e.toString());
            throw new AccountException(ErrorCode.ACCOUNT_ERROR, "create user error");
        }
        return r;
    }

    public User createUserMigration(final User user0) {
        final User user = user0 != null ? user0.copy() : null;


        ParamChecker.notNull("user", user);
        ParamChecker.notEmpty("user.password", user.getPassword());

        try {
            User r = db.createUserMigration(user);
            return r;
        } catch (SQLException e) {
            L.error("-------------------------------create user error--------------------" + e.toString());
            throw new AccountException(ErrorCode.ACCOUNT_ERROR, "create user error");
        }

    }

    /*@WebMethod("account2/testDestroyUser")
    public String testDestroyUser() {
        if (this.destroyUser("10015"))
            return "success!";
        return "failure";

    }*/

    @Override
    public boolean destroyUser(CharSequence userId) {
        long userIdS = Long.parseLong(userId.toString());
        boolean b = db.destroyUser(userIdS);

        return b;
    }

    /*@WebMethod("account2/testRecoverUser")
    public String testRecoverUserr() {
        if (this.recoverUser("10015"))
            return "success!";
        return "failure";

    }*/

    @Override
    public boolean recoverUser(CharSequence userId) {
        long userIdS = Long.parseLong(userId.toString());
        boolean r = db.recoverUser(userIdS);
        return r;
    }

    /*@WebMethod("account2/testUpdate")
    public String testupdate() {
        User user = this.getUser(10015);
        User user0 = user.copy();
        user0.setNickname("nickName_wangpeng");
        PhotoInfo photoInfo = new PhotoInfo();
        photoInfo.setLargeUrl("http://www.borqs.com/api/largePhoto.jpg");
        photoInfo.setSmallUrl("http://www.borqs.com/api/smallPhoto.jpg");
        photoInfo.setMiddleUrl("http://www.borqs.com/api/middlePhoto.jpg");
        user0.setPhoto(photoInfo);

        if (update(user0))
            return "success!";
        return "failure!";
    }*/

    @Override
    public boolean update(User user) {
        ParamChecker.notNull("user", user);
        user.getDisplayName();
        long userId = user.getUserId();
        updateSortKey(user);
        return update0(user);
    }

    private void updateSortKey(User user) {
        try {
            if (user.getName() != null) {
                String displayName = user.getDisplayName();
                Hanyu2Pinyin hanyu = new Hanyu2Pinyin();
                String sort_key = hanyu.getStringPinYin(displayName);
                sort_key += displayName;
                user.setAddon("sort_key", sort_key);
                //add update display_name
                user.setAddon("display_name",displayName);
            }
        } catch (Exception e) {
            L.error("update Sort Key error :userId=" + user.getUserId() + "   userName" + user.getDisplayName());
        }

    }

    private void updateDisplayName(User user) {
        try {
            if (user.getName() != null) {
                String displayName = user.getDisplayName();

                user.setAddon("display_name", displayName);
            }
        } catch (Exception e) {
            L.error("update Sort Key error :userId=" + user.getUserId() + "   userName" + user.getDisplayName());
        }

    }

    private boolean update0(final User user) {


        boolean b = db.update(user);

        return b;
    }

    public boolean updateSortKeyMigrate(final User user) {

        ParamChecker.notNull("user", user);
        user.getDisplayName();
        long userId = user.getUserId();
        updateSortKey(user);

        boolean b = db.updateSortKey4Migrate(user);

        return b;
    }

    public boolean updateDisplayNameMigrate(final User user) {

        ParamChecker.notNull("user", user);
        user.getDisplayName();
        long userId = user.getUserId();
        updateDisplayName(user);

        boolean b = db.updateSortKey4Migrate(user);

        return b;
    }

    @Override
    public String resetRandomPassword(CharSequence userId) {
        return "";
    }

    /*@WebMethod("account2/testUpdatePassword")
    public void testUpdatePassword() {
        updatePassword("10015", "sssssss", "123456", false);
    }*/

    @Override
    public void updatePassword(CharSequence userId, String oldPwd, String newPwd, boolean verify) {
        long userIdS = Long.parseLong(userId.toString());

        ParamChecker.notNull("newPwd", newPwd);

        if (verify && oldPwd != null) {
            User user = getUser(userIdS);
            if (!StringUtils.equals(oldPwd, user.getPassword()))
                throw new ServerException(E.INVALID_USER_OR_PASSWORD, "old password error");
        }
        User newUser = new User(userIdS);
        newUser.setPassword(newPwd);
        update0(newUser);
    }

    /*@WebMethod("account2/testGetUsers")
    public String testGetUsers() {
        long[] longs = {10015, 10016, 10017};
        return getUsers(longs).toArray().toString();
    }*/

    @Override
    public List<User> getUsers(long... userIds) {
        if (ArrayUtils.isEmpty(userIds))
            return new ArrayList<User>();

        List<User> users = null;
        try {
            users = db.getUsers(userIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }


    @Override
    public User getUser(long userId) {
        List<User> users = getUsers(userId);
        return CollectionUtils.isEmpty(users) ? null : users.get(0);
    }

    /*@WebMethod("account2/testGetPassword")
    public String testGetPassword() {

        return getPassword(10015);
    }*/

    @Override
    public String getPassword(long userId) {

        // ignore cache

        try {
            return db.getPassword(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*@WebMethod("account2/testHasAllUser")
    public boolean testHasAllUser() {
        long[] longs = {10015};
        return hasAllUser(longs);
    }*/

    @Override
    public boolean hasAllUser(long... userIds) {
        try {
            return db.hasAllUser(userIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*@WebMethod("account2/testhasAnyUser")
    public boolean testhasAnyUser() {
        long[] longs = {10015, 10016, 10017};
        return hasAnyUser(longs);
    }*/

    @Override
    public boolean hasAnyUser(long... userIds) {
        try {
            return db.hasAnyUser(userIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*@WebMethod("account2/testhasUser")
    public boolean testhasUser() {
        return hasUser(10015);
    }*/

    @Override
    public boolean hasUser(long userId) {
        try {
            return db.hasUser(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*@WebMethod("account2/testgetExistsIds")
    public long[] testgetExistsIds() {
        long[] longs = {10015, 10016, 10017};
        return getExistsIds(longs);
    }*/

    @Override
    public long[] getExistsIds(long... userIds) {
        if (userIds.length == 0)
            return new long[0];

        try {
            return db.getExistsIds(userIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteUserMigration(long userIds) {
        try {
            return db.deleteUserMigration(userIds) == -1 ? false : true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public long[] getAllUserIds() {
        try {
            return db.getAllUserIds();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


}
