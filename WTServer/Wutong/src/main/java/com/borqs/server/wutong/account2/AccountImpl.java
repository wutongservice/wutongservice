package com.borqs.server.wutong.account2;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.*;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.memcache.XMemcached;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.qiupu.QiupuLogic;
import com.borqs.server.qiupu.QiupuLogics;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.schema.AccountBase;
import com.borqs.server.wutong.account2.user.User;
import com.borqs.server.wutong.account2.util.ParamChecker;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongHooks;
import com.borqs.server.wutong.contacts.SocialContactsLogic;
import com.borqs.server.wutong.email.EmailLogic;
import com.borqs.server.wutong.extender.PlatformExtender;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.poll.PollLogic;
import com.borqs.server.wutong.request.RequestLogic;
import com.borqs.server.wutong.setting.SettingLogic;
import com.borqs.server.wutong.stream.StreamLogic;
import com.borqs.server.wutong.usersugg.SuggestedUserLogic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.*;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

import static com.borqs.server.base.util.ShortUrl.ShortText;


public class AccountImpl implements AccountLogic, Initializable {
    private static final Logger L = Logger.getLogger(AccountImpl.class);
    private final com.borqs.server.base.data.Schema userSchema = com.borqs.server.base.data.Schema.loadClassPath(AccountBase.class, "user.schema");
    private final com.borqs.server.base.data.Schema ticketSchema = com.borqs.server.base.data.Schema.loadClassPath(AccountBase.class, "ticket.schema");
    private final com.borqs.server.base.data.Schema contactSchema = com.borqs.server.base.data.Schema.loadClassPath(AccountBase.class, "contact_info.schema");
    private final com.borqs.server.base.data.Schema addressSchema = com.borqs.server.base.data.Schema.loadClassPath(AccountBase.class, "address.schema");
    private final com.borqs.server.base.data.Schema workSchema = com.borqs.server.base.data.Schema.loadClassPath(AccountBase.class, "work_history.schema");
    private final com.borqs.server.base.data.Schema educationSchema = com.borqs.server.base.data.Schema.loadClassPath(AccountBase.class, "education_history.schema");
    private final com.borqs.server.base.data.Schema privacySchema = com.borqs.server.base.data.Schema.loadClassPath(AccountBase.class, "privacy.schema");
    private String profileImagePattern;
    private String sysIconUrlPattern;
    private ConnectionFactory connectionFactory;
    private UserDb db;
    private String dbs;
    private String userTable;
    private String ticketTable;
    private String privacyTable;
    private String globalCounterTable;

    private String SERVER_HOST = "api.borqs.com";
    private String qiupuUid;

    private AccountLogic account;

    //add by wangpeng at 2013-04-27
    XMemcached xMemcached = new XMemcached();


    public AccountImpl() {
    }


    public void init() {
        Configuration conf = GlobalConfig.get();
        userSchema.loadAliases(conf.getString("schema.user.alias", null));
        ticketSchema.loadAliases(conf.getString("schema.ticket.alias", null));
        privacySchema.loadAliases(conf.getString("schema.privacy.alias", null));
        profileImagePattern = StringUtils.removeEnd(conf.checkGetString("platform.profileImagePattern").trim(), "/");
        sysIconUrlPattern = StringUtils.removeEnd(conf.checkGetString("platform.sysIconUrlPattern").trim(), "/");
        dbs = conf.getString("account.simple.db", null);
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.userTable = "user2";
        this.privacyTable = conf.getString("account.simple.privacyTable", "privacy");
        this.ticketTable = conf.getString("account.simple.ticketTable", "ticket");
        this.globalCounterTable = conf.getString("account.simple.globalCounterTable", "user_id_counter");
        qiupuUid = conf.getString("qiupu.uid", "102");
        db = new UserDb();
        db.setConfig(conf);
        account = GlobalLogics.getAccount();
        xMemcached.init();
    }


    public void destroy() {
        xMemcached.destroy();
        this.userTable = this.ticketTable = this.globalCounterTable = null;
        this.privacyTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, dbs);
    }

    @Override
    public User createUser(Context ctx, final User user0) {
        final String METHOD = "createUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, user0);


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
            if (L.isOpEnabled())
                L.op(ctx, "createUser");
            r = db.createUser(user);
        } catch (SQLException e) {
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "create user error");
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);

        return r;
    }

    /*public User createUserMigration(final User user0) {
        final User user = user0 != null ? user0.copy() : null;


        ParamChecker.notNull("user", user);
        ParamChecker.notEmpty("user.password", user.getPassword());

        try {
            User r = db.createUserMigration(user);
            return r;
        } catch (SQLException e) {
            L.error("-------------------------------create user error--------------------" + e.toString());
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "create user error");
        }

    }*/

    /*@WebMethod("account2/testDestroyUser")
    public String testDestroyUser() {
        if (this.destroyUser("10015"))
            return "success!";
        return "failure";

    }*/

    @Override
    public boolean destroyUser(Context ctx, String userId) {
        final String METHOD = "destroyUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);

        long userIdS = Long.parseLong(userId.toString());

        if (L.isOpEnabled())
            L.op(ctx, "destroyUser");

        boolean b = db.destroyUser(userIdS);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;
    }

    /*@WebMethod("account2/testRecoverUser")
    public String testRecoverUserr() {
        if (this.recoverUser("10015"))
            return "success!";
        return "failure";

    }*/

    @Override
    public boolean recoverUser(Context ctx, String userId) {
        final String METHOD = "recoverUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);

        long userIdS = Long.parseLong(userId.toString());
        if (L.isOpEnabled())
            L.op(ctx, "recoverUser");

        boolean r = db.recoverUser(userIdS);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
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
    public boolean update(Context ctx, User user) {
        final String METHOD = "update";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, user);

        ParamChecker.notNull("user", user);
        updateSortKey(user);

        if (L.isOpEnabled())
            L.op(ctx, "update");

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);

        //add modify memecached
        boolean b = update0(user);
        try {
            updateMemecached(user.getUserId());
        } catch (Exception e) {
            //do nothing
        }
        return b;
    }

    private void updateMemecached(long user_id){
        if(user_id==0)
            return;
        xMemcached.deleteCache(String.valueOf(user_id));
    }
    private void updateSortKey(User user) {
        if (user.getName() != null) {
            String displayName = user.getDisplayName();
            Hanyu2Pinyin hanyu = new Hanyu2Pinyin();
            String sort_key = hanyu.getStringPinYin(displayName);
            sort_key += displayName;
            user.setAddon("sort_key", sort_key);
            //add update display_name
            user.setAddon("display_name", displayName);
        }
    }

    private void updateDisplayName(User user) {

        if (user.getName() != null) {
            String displayName = user.getDisplayName();

            user.setAddon("display_name", displayName);
        }


    }

    private boolean update0(final User user) {


        boolean b = db.update(user);

        return b;
    }

    /*public boolean updateSortKeyMigrate(final User user) {

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
    }*/

    @Override
    public String resetRandomPassword(Context ctx, String userId) {
        return "";
    }

    /*@WebMethod("account2/testUpdatePassword")
    public void testUpdatePassword() {
        updatePassword("10015", "sssssss", "123456", false);
    }*/

    @Override
    public void updatePassword(Context ctx, String userId, String oldPwd, String newPwd, boolean verify) {
        final String METHOD = "updatePassword";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, oldPwd, newPwd, verify);

        long userIdS = Long.parseLong(userId.toString());

        ParamChecker.notNull("newPwd", newPwd);

        if (verify && oldPwd != null) {
            User user = getUser(ctx, userIdS);
            if (!StringUtils.equals(oldPwd, user.getPassword())) {
                if (L.isTraceEnabled())
                    L.traceEndCall(ctx, METHOD);
                throw new ServerException(WutongErrors.USER_UPDATE_PWD_ERROR, "old password error");
            }
        }
        User newUser = new User(userIdS);
        newUser.setPassword(newPwd);

        if (L.isOpEnabled())
            L.op(ctx, "updatePassword");

        update0(newUser);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }

    /*@WebMethod("account2/testGetUsers")
    public String testGetUsers() {
        long[] longs = {10015, 10016, 10017};
        return getUsers(longs).toArray().toString();
    }*/

    /**
     * 返回User对象的List，不常用
     *
     * @param ctx
     * @param userIds
     * @return
     */
    @Override
    public List<User> getUsers(Context ctx, long... userIds) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);

        if (ArrayUtils.isEmpty(userIds))
            return new ArrayList<User>();

        List<User> users = null;
        try {
            users = db.getUsers(userIds);
        } catch (SQLException e) {
            L.error(ctx, e);
            e.printStackTrace();
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);

        return users;
    }

    private static String firstId(String ids) {
        return StringUtils.substringBefore(ids, ",").trim();
    }


    /**
     * 普通的getUser方法，一般使用这个
     *
     * @param ctx
     * @param viewerId
     * @param userId
     * @param cols
     * @return
     */
    @Override
    public Record getUser(Context ctx, String viewerId, String userId, String cols) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userId, cols);

        RecordSet rec = getUsers(ctx, viewerId, firstId(userId), cols);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec.getFirstRecord();
    }

    /**
     * 带隐私参数的getUser，很常用
     *
     * @param ctx
     * @param viewerId
     * @param userId
     * @param cols
     * @param privacyEnabled
     * @return
     */
    @Override
    public Record getUser(Context ctx, String viewerId, String userId, String cols, boolean privacyEnabled) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userId, cols, privacyEnabled);
        RecordSet rec = getUsers(ctx, viewerId, firstId(userId), cols, privacyEnabled);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec.getFirstRecord();
    }

    /**
     * 返回User对象，一般不用这个
     *
     * @param ctx
     * @param userId
     * @return
     */
    @Override
    public User getUser(Context ctx, long userId) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);
        List<User> users = getUsers(ctx, userId);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return CollectionUtils.isEmpty(users) ? null : users.get(0);
    }

    /*@WebMethod("account2/testGetPassword")
    public String testGetPassword() {

        return getPassword(10015);
    }*/

    @Override
    public String getPassword(Context ctx, long userId) {

        final String METHOD = "getPassword";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);

        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return db.getPassword(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return null;
    }

    /*@WebMethod("account2/testHasAllUser")
    public boolean testHasAllUser() {
        long[] longs = {10015};
        return hasAllUser(longs);
    }*/

    @Override
    public boolean hasAllUser(Context ctx, long... userIds) {
        final String METHOD = "hasAllUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);

        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);

            return db.hasAllUser(userIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return false;
    }

    /*@WebMethod("account2/testhasAnyUser")
    public boolean testhasAnyUser() {
        long[] longs = {10015, 10016, 10017};
        return hasAnyUser(longs);
    }*/

    @Override
    public boolean hasAnyUser(Context ctx, long... userIds) {
        final String METHOD = "hasAnyUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);

        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return db.hasAnyUser(userIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return false;
    }

    /*@WebMethod("account2/testhasUser")
    public boolean testhasUser() {
        return hasUser(10015);
    }*/

    @Override
    public boolean hasUser(Context ctx, long userId) {
        final String METHOD = "hasAnyUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);
        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return db.hasUser(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return false;
    }

    /*@WebMethod("account2/testgetExistsIds")
    public long[] testgetExistsIds() {
        long[] longs = {10015, 10016, 10017};
        return getExistsIds(longs);
    }*/

    @Override
    public long[] getExistsIds(Context ctx, long... userIds) {
        final String METHOD = "getExistsIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);
        if (userIds.length == 0)
            return new long[0];

        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return db.getExistsIds(userIds);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return null;
    }

    /*public boolean deleteUserMigration(long userIds) {
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
    }*/

    public List<String> getGroupIdsFromMentions(Context ctx, List<String> mentions) {
        ArrayList<String> groupIds = new ArrayList<String>();

        GroupLogic group = GlobalLogics.getGroup();

        for (String mention : mentions) {
            if (StringUtils.startsWith(mention, "#"))
                mention = StringUtils.substringAfter(mention, "#");

            long id = 0;
            try {
                id = Long.parseLong(mention);
            } catch (NumberFormatException nfe) {
                continue;
            }
            if (group.isGroup(ctx, id))
                groupIds.add(String.valueOf(id));
        }

        return groupIds;

    }


    @Override
    public RecordSet getUsers(Context ctx, String viewerId, String userIds, String cols) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userIds, cols);
        GroupLogic groupLogic = GlobalLogics.getGroup();
        List<String> users = StringUtils2.splitList(userIds, ",", true);
        List<String> groups = getGroupIdsFromMentions(ctx, users);
        users.removeAll(groups);

        userIds = StringUtils2.joinIgnoreBlank(",", users);
        RecordSet userRecs = getUsers(ctx, viewerId, userIds, cols, true);

        if (CollectionUtils.isNotEmpty(groups)) {
            String groupIds = StringUtils2.joinIgnoreBlank(",", groups);
            userRecs.addAll(groupLogic.getCompatibleGroups(ctx, viewerId, groupIds));
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return userRecs;
    }

    @Override
    public RecordSet getUsers(Context ctx, String viewerId, String userIds_all, String cols, boolean privacyEnabled) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userIds_all, cols, privacyEnabled);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return getUsers(ctx, viewerId, userIds_all, cols, privacyEnabled, true);
    }

    private RecordsExtenders createUserExtenders(String viewerId) {
        RecordsExtenders res = new RecordsExtenders();
        res.add(new BuiltinUserExtender());
        res.addExtendersInConfig(GlobalConfig.get(), "platform.userExtenders");
        return res;
    }

    @Override
    public RecordSet getUsers(final Context ctx, String viewerId, String userIds_all, String cols, boolean privacyEnabled, boolean dealTopPosts) {
        ElapsedCounter ec = ctx.getElapsedCounter();
        ec.record("getUsers-start");
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userIds_all, cols, privacyEnabled, dealTopPosts);
        List<String> userIdListAll = StringUtils2.splitList(toStr(userIds_all), ",", true);
        List<String> userIdList_sys = new ArrayList<String>();
        List<String> userIdList_contact = new ArrayList<String>();

        ec.record("1");
        FriendshipLogic f = GlobalLogics.getFriendship();
        for (String u : userIdListAll) {
            if (u.length() > Constants.USER_ID_MAX_LEN) {
                userIdList_contact.add(u);
            } else {
                userIdList_sys.add(u);
            }
        }
        String userIds = StringUtils.join(userIdList_sys, ",");

        if (!StringUtils.contains(cols, "user_id")) {
            cols += ",user_id";
        }
        if (!StringUtils.contains(cols, "display_name")) {
            cols += ",display_name";
        }
        if (dealTopPosts && !StringUtils.contains(cols, "top_name"))
            cols += ",top_name";

        cols = parseUserColumns(cols);
        final String userIds0 = parseUserIds(ctx, viewerId, userIds);
        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        List<String> l2 = new ArrayList<String>();
        for (String a : l) {
            l2.add(a);
        }
        if (l.contains("favorites_count")) {
            l.remove("favorites_count");
        }
        if (l.contains("friends_count")) {
            l.remove("friends_count");
        }
        if (l.contains("followers_count")) {
            l.remove("followers_count");
        }
        if (l.contains("subscribe")) {
            l.remove("subscribe");
        }

        try {
            ec.record("2");
            RecordSet recs = getUsers(ctx, userIds0, StringUtils.join(l, ","));
            ec.record("3");

            RecordsExtenders userExtenders = createUserExtenders(viewerId);
            recs = userExtenders.extendRecords(ctx,
                    StringUtils2.splitSet(StringUtils.join(l, ","), ",", true),
                    new RecordsProducer() {

                        @Override
                        public RecordSet product(Set<String> produceCols) throws Exception {
                            return getUsers(ctx, userIds0, StringUtils.join(produceCols, ","));
                        }
                    });
            //L.debug("-------------------------111111-----------------------"+recs.toString()+"-----------------------------------");
            ec.record("4");
            if (l2.contains("favorites_count")) {
                if (recs.size() > 0) {

                    QiupuLogic qp = QiupuLogics.getQiubpu();
                    RecordSet recs_ucount = qp.getUsersAppCount(ctx, userIds, String.valueOf(1 << 3));
                    Map app_map = new HashMap();
                    for (Record ur : recs_ucount) {
                        app_map.put(ur.getString("user_id"), ur.getString("count"));
                    }
                    for (Record rec : recs) {
                        rec.put("favorites_count", app_map.get(rec.getString("user_id")));
                    }
                }
            }
            ec.record("5");
            if ((l2.contains("friends_count") || l2.contains("followers_count"))) {
                if (recs.size() > 0) {

                    List<String> userl = StringUtils2.splitList(toStr(userIds), ",", true);
                    RecordSet recs_fs = f.getFriendOrFollowers(ctx, userIds, "friend");
                    RecordSet recs_fri = f.getFriendOrFollowers(ctx, userIds, "user");

                    Map fs_map = new HashMap();
                    Map fri_map = new HashMap();
                    for (String ul : userl) {
                        int i = 0;
                        int j = 0;
                        for (Record fsr : recs_fs) {
                            if (fsr.getString("friend").equals(ul)) {
                                i++;
                            }
                        }
                        fs_map.put(ul, String.valueOf(i));
                        for (Record frir : recs_fri) {
                            if (frir.getString("user").equals(ul)) {
                                j++;
                            }
                        }
                        fri_map.put(ul, String.valueOf(j));
                    }

                    for (Record rec : recs) {
                        rec.put("friends_count", fri_map.get(rec.getString("user_id")));
                        rec.put("followers_count", fs_map.get(rec.getString("user_id")));
                    }
                }
            }
            ec.record("5");
            //add shared_count
            if (l2.contains("friends_count")) {
                for (Record rec : recs) {
                    int shared_text = 0;
                    int shared_photo = 0;
                    int shared_book = 0;
                    int shared_apk = 0;
                    int shared_link = 0;
                    int shared_static_file = 0;
                    int shared_audio = 0;
                    int shared_video = 0;
                    RecordSet share_count_all = GlobalLogics.getStream().getSharedCountAll(ctx, viewerId, rec.getString("user_id"));
                    for (Record r : share_count_all) {
                        if (r.getInt("type") == Constants.TEXT_POST)
                            shared_text += 1;
                        if (r.getInt("type") == Constants.PHOTO_POST)
                            shared_photo += 1;
                        if (r.getInt("type") == Constants.BOOK_POST)
                            shared_book += 1;
                        if (r.getInt("type") == Constants.APK_POST)
                            shared_apk += 1;
                        if (r.getInt("type") == Constants.LINK_POST)
                            shared_link += 1;
                        if (r.getInt("type") == Constants.FILE_POST)
                            shared_static_file += 1;
                        if (r.getInt("type") == Constants.AUDIO_POST)
                            shared_audio += 1;
                        if (r.getInt("type") == Constants.VIDEO_POST)
                            shared_video += 1;
                    }


                    Record shared_count = new Record();
                    shared_count.put("shared_text", shared_text);
                    shared_count.put("shared_photo", shared_photo);
                    shared_count.put("shared_book", shared_book);
                    shared_count.put("shared_apk", shared_apk);
                    shared_count.put("shared_link", shared_link);
                    shared_count.put("shared_static_file", shared_static_file);
                    shared_count.put("shared_audio", shared_audio);
                    shared_count.put("shared_video", shared_video);
                    shared_count.put("shared_poll", getRelatedPollCount(ctx, viewerId, rec.getString("user_id")));
                    rec.put("shared_count", shared_count);
                }
            }
            ec.record("6");
            if (privacyEnabled) {
                for (Record rec : recs) {
                    rec.putMissing("profile_privacy", true);
                    String uid = rec.getString("user_id");
                    if (!viewerId.equals(uid)) {
                        doAbsolutePrivateCols(rec);
                    }
                    boolean if_in = true;
                    if (!viewerId.equals("") && !viewerId.equals("0")) {
                        if_in = f.getIfHeInMyCircles(ctx, uid, viewerId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE));
                        rec.put("profile_privacy", !if_in);
                    }

                    if (!if_in && !viewerId.equals(uid)) {
                        rec.put("contact_info", new Record());
                        rec.put("address", new RecordSet());
                        rec.put("work_history", new RecordSet());
                        rec.put("education_history", new RecordSet());
                    }
                }
            }
            ec.record("7");
            //pendding requests
            RequestLogic req = GlobalLogics.getRequest();

            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    RecordSet recs_pend = req.getPendingRequestsAll(ctx, viewerId, userIds);
                    Map pend_map = new HashMap();
                    for (Record p : recs_pend) {
                        pend_map.put(p.getString("user"), p.getString("penddingRequest"));
                    }
                    for (Record rec : recs) {
                        List<String> ltypes = new ArrayList<String>();
                        String uid = rec.getString("user_id");

                        if (pend_map.get(uid) != null) {
                            String md = StringUtils.substringBeforeLast(pend_map.get(uid).toString(), ",");
                            if (md.length() > 0) {
                                ltypes = StringUtils2.splitList(md, ",", true);
                            }
                            rec.putMissing("pedding_requests", JsonUtils.parse(JsonUtils.toJson(ltypes, false)));
                        } else {
                            rec.putMissing("pedding_requests", new RecordSet());
                        }
                    }
                } else {
                    for (Record rec : recs) {
                        rec.putMissing("pedding_requests", new RecordSet());
                    }
                }
            }
            ec.record("8");
            //看看此人在我这通讯簿里面存的啥名字
            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    for (Record rec : recs) {
                        String uid = rec.getString("user_id");
                        RecordSet so_recss = getSocialcontactUsername(ctx, viewerId, uid);
                        List<String> sf = new ArrayList<String>();
                        for (Record so_rec : so_recss) {
                            if (!sf.contains(so_rec.getString("username")))
                                sf.add(so_rec.getString("username"));
                        }
                        rec.put("social_contacts_username", StringUtils.join(sf, ","));
                    }
                } else {
                    for (Record rec : recs) {
                        rec.put("social_contacts_username", "");
                    }
                }
            }
            ec.record("9");
            //如果不是好友，看看有没有人跟我推荐过他
            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    for (Record rec : recs) {
                        String uid = rec.getString("user_id");
                        rec.put("who_suggested", new RecordSet());
                        //                        RecordSet in_circles = RecordSet.fromJson(rec.getString("in_circles"));
                        boolean beforeFriend = isFriend(ctx, viewerId, uid);
                        if (!beforeFriend) {
                            //是好友，不用查推荐
                            List<Long> bs = getWhoSuggest(ctx, viewerId, uid);
                            if (bs.size() > 0) {
                                List<String> bs1 = new ArrayList<String>();
                                for (Long bs0 : bs) {
                                    bs1.add(String.valueOf(bs0));
                                }
                                //                                RecordSet users_suggested = getUsers(viewerId, StringUtils.join(bs1, ","), USER_LIGHT_COLUMNS_LIGHT);
                                RecordSet us = getUsers(ctx, StringUtils.join(bs1, ","), USER_LIGHT_COLUMNS_LIGHT);
                                rec.put("who_suggested", us);
                            }
                        }
                    }
                } else {
                    for (Record rec : recs) {
                        rec.put("who_suggested", new RecordSet());
                    }
                }
            }
            ec.record("9");
            if (userIdList_contact.size() > 0) {
                String userIds_contact = StringUtils.join(userIdList_contact, ",");
                RecordSet recs_contact_fri = f.getContactFriendByFid(ctx, userIds_contact);
                if (recs_contact_fri.size() > 0) {
                    RecordSet recs_mine = getAllRelation(ctx, viewerId, userIds_contact, Integer.toString(Constants.FRIENDS_CIRCLE), "mine");
                    for (Record r : recs_contact_fri) {
                        r.renameColumn("virtual_friendid", "user_id");
                        r.renameColumn("name", "display_name");
                        r.renameColumn("content", "content");

                        RecordSet temp0 = new RecordSet();
                        for (Record ru : recs_mine) {
                            if (ru.getString("friend").equals(r.getString("user_id"))) {
                                temp0.add(Record.of("circle_id", ru.getString("circle"), "circle_name", ru.getString("name")));
                            }
                        }
                        r.put("in_circles", temp0);
                        r.put("friends_count", 0);
                        RecordSet t = f.getFollowers(ctx, r.getString("user_id"), String.valueOf(Constants.FRIENDS_CIRCLE), 0, 500);
                        r.put("followers_count", t.size());
                    }
                    recs.addAll(recs_contact_fri);
                }
            }
            ec.record("10");
            //#######################################
            if (l2.contains("friends_count")) {
                int getSize = 5;
                StreamLogic stream = GlobalLogics.getStream();
//                FavoriteLogic favoriteLogic = GlobalLogics.getFavorite();
                for (Record r : recs) {
                    //if need favorite user ,then open this code
//                    r.put("favorited",favoriteLogic.getIFavorited(ctx,viewerId,String.valueOf(Constants.USER_OBJECT), r.getString("user_id")));
                    //1,
                    String uid = r.getString("user_id");
                    RecordSet bo = f.getBothFriendsIds(ctx, viewerId, uid, 0, 100);
                    String bothF = bo.joinColumnValues("friend", ",");
                    RecordSet friend_ = account.getUsers(ctx, bothF, "user_id,image_url");
                    RecordSet t = new RecordSet();
                    for (Record r0 : friend_) {
                        if (r0.getString("image_url").length() > 0) {
                            Record t0 = new Record();
                            t0.put("image_url", r0.getString("image_url"));
                            t0.put("user_id", r0.getString("user_id"));
                            t.add(t0);
                        }
                        if (t.size() >= getSize)
                            break;
                    }
                    if (t.size() < getSize) {
                        for (Record r0 : friend_) {
                            if (r0.getString("image_url").length() == 0) {
                                Record t0 = new Record();
                                t0.put("image_url", r0.getString("image_url"));
                                t0.put("user_id", r0.getString("user_id"));
                                t.add(t0);
                            }
                            if (t.size() >= getSize)
                                break;
                        }
                    }
                    r.put("profile_friends", t);
                    //2,
                    RecordSet recs_fs = f.getFollowers(ctx, uid, Integer.toString(Constants.FRIENDS_CIRCLE), 0, 100);
                    String fsIdString = recs_fs.joinColumnValues("follower", ",");
                    RecordSet follower_ = getUsers(ctx, fsIdString, "user_id,image_url");
                    RecordSet t_f = new RecordSet();
                    for (Record r0 : follower_) {
                        if (r0.getString("image_url").length() > 0) {
                            Record t0 = new Record();
                            t0.put("image_url", r0.getString("image_url"));
                            t0.put("user_id", r0.getString("user_id"));
                            t_f.add(t0);
                        }
                        if (t_f.size() >= getSize)
                            break;
                    }
                    if (t_f.size() < getSize) {
                        for (Record r0 : follower_) {
                            if (r0.getString("image_url").length() == 0) {
                                Record t0 = new Record();
                                t0.put("image_url", r0.getString("image_url"));
                                t0.put("user_id", r0.getString("user_id"));
                                t_f.add(t0);
                            }
                            if (t_f.size() >= getSize)
                                break;
                        }
                    }
                    r.put("profile_followers", t_f);
                    //3,
                    RecordSet share_photo = stream.getSharedByType(ctx, uid, Constants.PHOTO_POST, "post_id,attachments", 0, 5);
                    RecordSet t_p = new RecordSet();
                    for (Record rP : share_photo) {
                        Record attach = RecordSet.fromJson(rP.getString("attachments")).getFirstRecord();
                        Record t0 = new Record();
                        t0.put("photo_img_middle", attach.getString("photo_img_middle"));
                        t0.put("photo_img_original", attach.getString("photo_img_original"));
                        t0.put("post_id", rP.getString("post_id"));
                        t_p.add(t0);
                    }
                    r.put("profile_shared_photos", t_p);
                }
            }
            //L.debug("--------------------------222222----------------------"+recs.toString()+"-----------------------------------");
            //##############################################
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);


            RecordSet rs = dealTopPosts ? dealAccountTopPosts(recs) : recs;

            if (l2.contains("subscribe")) {
                attachSubscribe(ctx, rs);
            }

            ec.record("getUsers-end");
            return rs;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    private void attachSubscribe(Context ctx, RecordSet recs) {
        Map<String, Integer> m = GlobalLogics.getConversation().getEnabledByTargetIds(ctx, recs.joinColumnValues("user_id", ","));
        for (Record rec : recs) {
            String userId = rec.getString("user_id");
            rec.put("subscribe", m.get(userId));
        }
    }

    private static final String USER_BASE_COL = "user_id,display_name,perhaps_name,image_url";

    public RecordSet getUsersBaseColumns(final Context ctx, String userIds_all) {
        final String METHOD = "getUsersBaseColumns";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds_all);
        RecordSet rs = new RecordSet();

        //add by wangpeng at 2013-04-27
        String unFindUserIds = setUserMemcached(ctx, userIds_all, rs);

        RecordSet recs = GlobalLogics.getAccount().getUserByIdBaseColumns(ctx, unFindUserIds);

        //add by wangpeng at 2013-04-27
        addUserMemcached(ctx, recs);

        rs.addAll(recs);
        if (L.isDebugEnabled())
            //L.debug(ctx, "getUsersBaseColumns return data=" + rs);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
        return rs;
    }

    /**
     * add by wangpeng at 2013-04-27
     * @param ctx
     * @param rs
     */
    private void addUserMemcached(Context ctx, RecordSet rs) {
        if (rs.size() < 1)
            return;
        try {
            for (Record r : rs) {
                xMemcached.writeRecordCache(r.getString("user_id"), r);
            }
        } catch (Exception e) {
            L.error(ctx, e, "add User to memcached error!");
        }
    }

    /**
     * add by wangpeng at 2013-04-27
     * @param ctx
     * @param userId_all
     * @param result
     * @return
     */
    private String setUserMemcached(Context ctx, String userId_all, RecordSet result) {
        List<String> unFind = new ArrayList<String>();
        List<String> list = StringUtils2.splitList(userId_all, ",", true);
        try {
            Map<String, Record> map = xMemcached.readMultiRecordCache(list);
            for (String str : list) {
                if (map.containsKey(str)) {
                    result.add(map.get(str));
                } else {
                    unFind.add(str);
                }
            }
            return StringUtils.join(unFind, ",");
        } catch (Exception e) {
            return userId_all;
        }
    }

    private static final String USER_BASE_FRIEND_COL = "user_id,display_name,perhaps_name,image_url,bidi,in_circles,his_friend";

    public RecordSet getUsersBaseColumnsContainsFriend(final Context ctx, String viewerId, String userIds_all) {
        final String METHOD = "getUsersBaseColumnsContainsFriend";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userIds_all, USER_BASE_FRIEND_COL);
        List<String> l = StringUtils2.splitList(toStr(USER_BASE_FRIEND_COL), ",", true);
        final String userIds0 = parseUserIds(ctx, viewerId, userIds_all);

        try {
            //以下是获取到数据库的基本字段
            RecordSet recs = getUsersBaseColumns(ctx, userIds_all);
            //  ===============以下这部分增加了4个字段  remark bidi in_circles his_friend
            RecordsExtenders userExtenders = createUserExtenders(viewerId);
            recs = userExtenders.extendRecords(ctx,
                    StringUtils2.splitSet(StringUtils.join(l, ","), ",", true),
                    new RecordsProducer() {
                        @Override
                        public RecordSet product(Set<String> produceCols) throws Exception {
                            return getUsers(ctx, userIds0, StringUtils.join(produceCols, ","));
                        }
                    });
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);

            return recs;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return new RecordSet();
    }

    private static final String USER_BASE_FRIEND_REMARK_COL = "user_id, display_name, remark,perhaps_name,image_url, status, gender, in_circles, his_friend, bidi";

    public RecordSet getUsersBaseColumnsContainsRemarkRequest(final Context ctx, String viewerId, String userIds_all) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userIds_all, USER_BASE_FRIEND_COL);
        List<String> l = StringUtils2.splitList(toStr(USER_BASE_FRIEND_REMARK_COL), ",", true);
        final String userIds0 = parseUserIds(ctx, viewerId, userIds_all);

        try {
            //以下是获取到数据库的基本字段
            RecordSet recs = getUsers(ctx, userIds0, USER_BASE_FRIEND_REMARK_COL);


            //  ===============以下这部分增加了4个字段  remark bidi in_circles his_friend
            RecordsExtenders userExtenders = createUserExtenders(viewerId);
            recs = userExtenders.extendRecords(ctx,
                    StringUtils2.splitSet(StringUtils.join(l, ","), ",", true),
                    new RecordsProducer() {
                        @Override
                        public RecordSet product(Set<String> produceCols) throws Exception {
                            return getUsers(ctx, userIds0, StringUtils.join(produceCols, ","));
                        }
                    });

            RequestLogic req = GlobalLogics.getRequest();

            if (recs.size() > 0) {
                for (Record rec : recs) {
                    rec.put("small_image_url", rec.getString("image_url").replace("_M.", "_S."));
                    rec.put("large_image_url", rec.getString("image_url").replace("_M.", "_L."));
                }

                if (StringUtils.isNotBlank(viewerId)) {
                    RecordSet recs_pend = req.getPendingRequestsAll(ctx, viewerId, userIds_all);
                    Map pend_map = new HashMap();
                    for (Record p : recs_pend) {
                        pend_map.put(p.getString("user"), p.getString("penddingRequest"));
                    }
                    for (Record rec : recs) {
                        List<String> ltypes = new ArrayList<String>();
                        String uid = rec.getString("user_id");

                        if (pend_map.get(uid) != null) {
                            String md = StringUtils.substringBeforeLast(pend_map.get(uid).toString(), ",");
                            if (md.length() > 0) {
                                ltypes = StringUtils2.splitList(md, ",", true);
                            }
                            rec.putMissing("pedding_requests", JsonUtils.parse(JsonUtils.toJson(ltypes, false)));
                        } else {
                            rec.putMissing("pedding_requests", new RecordSet());
                        }
                    }
                } else {
                    for (Record rec : recs) {
                        rec.putMissing("pedding_requests", new RecordSet());
                    }
                }

                for (Record rec : recs) {
                    rec.putMissing("profile_privacy", true);
                    String uid = rec.getString("user_id");
                    if (!viewerId.equals(uid)) {
                        doAbsolutePrivateCols(rec);
                    }
                    boolean if_in = true;
                    if (!viewerId.equals("") && !viewerId.equals("0")) {
                        if_in = GlobalLogics.getFriendship().getIfHeInMyCircles(ctx, uid, viewerId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE));
                        rec.put("profile_privacy", !if_in);
                    }

                    if (!if_in && !viewerId.equals(uid)) {
                        rec.put("contact_info", new Record());
                        rec.put("address", new RecordSet());
                        rec.put("work_history", new RecordSet());
                        rec.put("education_history", new RecordSet());
                    }
                }
            }

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);

            return recs;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return new RecordSet();
    }


    public RecordSet getUsers11111(final Context ctx, String viewerId, String userIds_all, String cols, boolean privacyEnabled, boolean dealTopPosts) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userIds_all, cols, privacyEnabled, dealTopPosts);
        List<String> userIdListAll = StringUtils2.splitList(toStr(userIds_all), ",", true);
        List<String> userIdList_sys = new ArrayList<String>();
        List<String> userIdList_contact = new ArrayList<String>();

        FriendshipLogic f = GlobalLogics.getFriendship();
        for (String u : userIdListAll) {
            if (u.length() > Constants.USER_ID_MAX_LEN) {
                userIdList_contact.add(u);
            } else {
                userIdList_sys.add(u);
            }
        }
        String userIds = StringUtils.join(userIdList_sys, ",");

        if (!StringUtils.contains(cols, "user_id")) {
            cols += ",user_id";
        }
        if (!StringUtils.contains(cols, "display_name")) {
            cols += ",display_name";
        }
        if (dealTopPosts && !StringUtils.contains(cols, "top_name"))
            cols += ",top_name";

        cols = parseUserColumns(cols);
        final String userIds0 = parseUserIds(ctx, viewerId, userIds);
        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        List<String> l2 = new ArrayList<String>();
        for (String a : l) {
            l2.add(a);
        }
        if (l.contains("favorites_count")) {
            l.remove("favorites_count");
        }
        if (l.contains("friends_count")) {
            l.remove("friends_count");
        }
        if (l.contains("followers_count")) {
            l.remove("followers_count");
        }

        try {
            //以下是获取到数据库的基本字段
            RecordSet recs = getUsers(ctx, userIds0, StringUtils.join(l, ","));
            //  ===============以下这部分增加了4个字段  remark bidi in_circles his_friend
            RecordsExtenders userExtenders = createUserExtenders(viewerId);
            recs = userExtenders.extendRecords(ctx,
                    StringUtils2.splitSet(StringUtils.join(l, ","), ",", true),
                    new RecordsProducer() {
                        @Override
                        public RecordSet product(Set<String> produceCols) throws Exception {
                            return getUsers(ctx, userIds0, StringUtils.join(produceCols, ","));
                        }
                    });

            //==============获取  favorites_count 算法已优化==========

            if (recs.size() > 0) {
                QiupuLogic qp = QiupuLogics.getQiubpu();
                RecordSet recs_ucount = qp.getUsersAppCount(ctx, userIds, String.valueOf(1 << 3));
                Map app_map = new HashMap();
                for (Record ur : recs_ucount) {
                    app_map.put(ur.getString("user_id"), ur.getString("count"));
                }
                for (Record rec : recs) {
                    rec.put("favorites_count", app_map.get(rec.getString("user_id")));
                }
            }


            //==============获取  friends_count followers_count  算法已优化==========
            if (recs.size() > 0) {

                List<String> userl = StringUtils2.splitList(toStr(userIds), ",", true);
                RecordSet recs_fs = f.getFriendOrFollowers(ctx, userIds, "friend");
                RecordSet recs_fri = f.getFriendOrFollowers(ctx, userIds, "user");

                Map fs_map = new HashMap();
                Map fri_map = new HashMap();
                for (String ul : userl) {
                    int i = 0;
                    int j = 0;
                    for (Record fsr : recs_fs) {
                        if (fsr.getString("friend").equals(ul)) {
                            i++;
                        }
                    }
                    fs_map.put(ul, String.valueOf(i));
                    for (Record frir : recs_fri) {
                        if (frir.getString("user").equals(ul)) {
                            j++;
                        }
                    }
                    fri_map.put(ul, String.valueOf(j));
                }

                for (Record rec : recs) {
                    rec.put("friends_count", fri_map.get(rec.getString("user_id")));
                    rec.put("followers_count", fs_map.get(rec.getString("user_id")));
                }
            }

            //==============获取  shared_count  算法已优化==========
            if (recs.size() > 0) {
                for (Record rec : recs) {
                    int shared_text = 0;
                    int shared_photo = 0;
                    int shared_book = 0;
                    int shared_apk = 0;
                    int shared_link = 0;
                    int shared_static_file = 0;
                    int shared_audio = 0;
                    int shared_video = 0;
                    RecordSet share_count_all = GlobalLogics.getStream().getSharedCountAll(ctx, viewerId, rec.getString("user_id"));
                    for (Record r : share_count_all) {
                        if (r.getInt("type") == Constants.TEXT_POST)
                            shared_text += 1;
                        if (r.getInt("type") == Constants.PHOTO_POST)
                            shared_photo += 1;
                        if (r.getInt("type") == Constants.BOOK_POST)
                            shared_book += 1;
                        if (r.getInt("type") == Constants.APK_POST)
                            shared_apk += 1;
                        if (r.getInt("type") == Constants.LINK_POST)
                            shared_link += 1;
                        if (r.getInt("type") == Constants.FILE_POST)
                            shared_static_file += 1;
                        if (r.getInt("type") == Constants.AUDIO_POST)
                            shared_audio += 1;
                        if (r.getInt("type") == Constants.VIDEO_POST)
                            shared_video += 1;
                    }


                    Record shared_count = new Record();
                    shared_count.put("shared_text", shared_text);
                    shared_count.put("shared_photo", shared_photo);
                    shared_count.put("shared_book", shared_book);
                    shared_count.put("shared_apk", shared_apk);
                    shared_count.put("shared_link", shared_link);
                    shared_count.put("shared_static_file", shared_static_file);
                    shared_count.put("shared_audio", shared_audio);
                    shared_count.put("shared_video", shared_video);
                    shared_count.put("shared_poll", getRelatedPollCount(ctx, viewerId, rec.getString("user_id")));
                    rec.put("shared_count", shared_count);
                }
            }

            //==============获取  profile_privacy  ==========
            if (privacyEnabled) {
                for (Record rec : recs) {
                    rec.putMissing("profile_privacy", true);
                    String uid = rec.getString("user_id");
                    if (!viewerId.equals(uid)) {
                        doAbsolutePrivateCols(rec);
                    }
                    boolean if_in = true;
                    if (!viewerId.equals("") && !viewerId.equals("0")) {
                        //这里有一个查询对方是不是在我的 ADDRESS_BOOK_CIRCLE里面
                        //前面的extend里面 有获取了 remark bidi in_circles his_friend这四个字段的，可以用
                        if (rec.getString("in_circles").length() > 10) {
                            RecordSet record_in = RecordSet.fromJson(rec.getString("in_circles"));
                            if (record_in.size() > 0) {
                                int k = 0;
                                for (Record rk : record_in) {
                                    if (rk.getInt("circle_id") == Constants.ADDRESS_BOOK_CIRCLE) {
                                        k += 1;
                                        break;
                                    }
                                }
                                if (k == 0) {
                                    if_in = false;
                                }
                            }
                        } else {
                            if_in = f.getIfHeInMyCircles(ctx, uid, viewerId, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE));
                        }
                        rec.put("profile_privacy", !if_in);
                    }

                    if (!if_in && !viewerId.equals(uid)) {
                        rec.put("contact_info", new Record());
                        rec.put("address", new RecordSet());
                        rec.put("work_history", new RecordSet());
                        rec.put("education_history", new RecordSet());
                    }
                }
            }


            //pendding requests
            //==============获取  pendding requests 算法已优化  ==========
            RequestLogic req = GlobalLogics.getRequest();

            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    RecordSet recs_pend = req.getPendingRequestsAll(ctx, viewerId, userIds);
                    Map pend_map = new HashMap();
                    for (Record p : recs_pend) {
                        pend_map.put(p.getString("user"), p.getString("penddingRequest"));
                    }
                    for (Record rec : recs) {
                        List<String> ltypes = new ArrayList<String>();
                        String uid = rec.getString("user_id");

                        if (pend_map.get(uid) != null) {
                            String md = StringUtils.substringBeforeLast(pend_map.get(uid).toString(), ",");
                            if (md.length() > 0) {
                                ltypes = StringUtils2.splitList(md, ",", true);
                            }
                            rec.putMissing("pedding_requests", JsonUtils.parse(JsonUtils.toJson(ltypes, false)));
                        } else {
                            rec.putMissing("pedding_requests", new RecordSet());
                        }
                    }
                } else {
                    for (Record rec : recs) {
                        rec.putMissing("pedding_requests", new RecordSet());
                    }
                }
            }

            //看看此人在我这通讯簿里面存的啥名字
            //==============获取  social_contacts_username   ==========
            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    for (Record rec : recs) {
                        String uid = rec.getString("user_id");
                        RecordSet so_recss = getSocialcontactUsername(ctx, viewerId, uid);
                        List<String> sf = new ArrayList<String>();
                        for (Record so_rec : so_recss) {
                            if (!sf.contains(so_rec.getString("username")))
                                sf.add(so_rec.getString("username"));
                        }
                        rec.put("social_contacts_username", StringUtils.join(sf, ","));
                    }
                } else {
                    for (Record rec : recs) {
                        rec.put("social_contacts_username", "");
                    }
                }
            }

            //如果不是好友，看看有没有人跟我推荐过他
            //==============获取  who_suggested   ==========
            if (recs.size() > 0) {
                if (StringUtils.isNotBlank(viewerId)) {
                    for (Record rec : recs) {
                        String uid = rec.getString("user_id");
                        rec.put("who_suggested", new RecordSet());
                        //这里前面有可能取过remark bidi in_circles his_friend这四个字段
                        //如果取了，这里的isfriend是不需要计算的
                        boolean beforeFriend = false;
                        if (rec.has("in_circles")) {
                            RecordSet in_circles = RecordSet.fromJson(rec.getString("in_circles"));
                            if (in_circles.size() > 0) {
                                beforeFriend = true;
                            }
                        } else {
                            beforeFriend = isFriend(ctx, viewerId, uid);
                        }

                        if (!beforeFriend) {
                            //是好友，不用查推荐
                            List<Long> bs = getWhoSuggest(ctx, viewerId, uid);
                            if (bs.size() > 0) {
                                List<String> bs1 = new ArrayList<String>();
                                for (Long bs0 : bs) {
                                    bs1.add(String.valueOf(bs0));
                                }
                                //                                RecordSet users_suggested = getUsers(viewerId, StringUtils.join(bs1, ","), USER_LIGHT_COLUMNS_LIGHT);
                                RecordSet us = getUsers(ctx, StringUtils.join(bs1, ","), USER_LIGHT_COLUMNS_LIGHT);
                                rec.put("who_suggested", us);
                            }
                        }
                    }
                } else {
                    for (Record rec : recs) {
                        rec.put("who_suggested", new RecordSet());
                    }
                }
            }

            if (userIdList_contact.size() > 0) {
                String userIds_contact = StringUtils.join(userIdList_contact, ",");
                RecordSet recs_contact_fri = f.getContactFriendByFid(ctx, userIds_contact);
                if (recs_contact_fri.size() > 0) {
                    RecordSet recs_mine = getAllRelation(ctx, viewerId, userIds_contact, Integer.toString(Constants.FRIENDS_CIRCLE), "mine");
                    for (Record r : recs_contact_fri) {
                        r.renameColumn("virtual_friendid", "user_id");
                        r.renameColumn("name", "display_name");
                        r.renameColumn("content", "content");

                        RecordSet temp0 = new RecordSet();
                        for (Record ru : recs_mine) {
                            if (ru.getString("friend").equals(r.getString("user_id"))) {
                                temp0.add(Record.of("circle_id", ru.getString("circle"), "circle_name", ru.getString("name")));
                            }
                        }
                        r.put("in_circles", temp0);
                        r.put("friends_count", 0);
                        RecordSet t = f.getFollowers(ctx, r.getString("user_id"), String.valueOf(Constants.FRIENDS_CIRCLE), 0, 500);
                        r.put("followers_count", t.size());
                    }
                    recs.addAll(recs_contact_fri);
                }
            }

            //==============获取  此人跟我的5个共同好友以及头像   ==========
            //==============获取  此人的5个粉丝以及头像   ==========
            //==============获取  此人分享的5张照片   ==========
            //#######################################
            if (recs.size() > 0) {
                int getSize = 5;
                StreamLogic stream = GlobalLogics.getStream();
                //                FavoriteLogic favoriteLogic = GlobalLogics.getFavorite();
                for (Record r : recs) {
                    //if need favorite user ,then open this code
                    //                    r.put("favorited",favoriteLogic.getIFavorited(ctx,viewerId,String.valueOf(Constants.USER_OBJECT), r.getString("user_id")));
                    //1,
                    String uid = r.getString("user_id");
                    RecordSet bo = f.getBothFriendsIds(ctx, viewerId, uid, 0, 100);
                    String bothF = bo.joinColumnValues("friend", ",");
                    RecordSet friend_ = account.getUsers(ctx, bothF, "user_id,image_url");
                    RecordSet t = new RecordSet();
                    for (Record r0 : friend_) {
                        if (r0.getString("image_url").length() > 0) {
                            Record t0 = new Record();
                            t0.put("image_url", r0.getString("image_url"));
                            t0.put("user_id", r0.getString("user_id"));
                            t.add(t0);
                        }
                        if (t.size() >= getSize)
                            break;
                    }
                    if (t.size() < getSize) {
                        for (Record r0 : friend_) {
                            if (r0.getString("image_url").length() == 0) {
                                Record t0 = new Record();
                                t0.put("image_url", r0.getString("image_url"));
                                t0.put("user_id", r0.getString("user_id"));
                                t.add(t0);
                            }
                            if (t.size() >= getSize)
                                break;
                        }
                    }
                    r.put("profile_friends", t);
                    //2,
                    RecordSet recs_fs = f.getFollowers(ctx, uid, Integer.toString(Constants.FRIENDS_CIRCLE), 0, 100);
                    String fsIdString = recs_fs.joinColumnValues("follower", ",");
                    RecordSet follower_ = getUsers(ctx, fsIdString, "user_id,image_url");
                    RecordSet t_f = new RecordSet();
                    for (Record r0 : follower_) {
                        if (r0.getString("image_url").length() > 0) {
                            Record t0 = new Record();
                            t0.put("image_url", r0.getString("image_url"));
                            t0.put("user_id", r0.getString("user_id"));
                            t_f.add(t0);
                        }
                        if (t_f.size() >= getSize)
                            break;
                    }
                    if (t_f.size() < getSize) {
                        for (Record r0 : follower_) {
                            if (r0.getString("image_url").length() == 0) {
                                Record t0 = new Record();
                                t0.put("image_url", r0.getString("image_url"));
                                t0.put("user_id", r0.getString("user_id"));
                                t_f.add(t0);
                            }
                            if (t_f.size() >= getSize)
                                break;
                        }
                    }
                    r.put("profile_followers", t_f);
                    //3,
                    RecordSet share_photo = stream.getSharedByType(ctx, uid, Constants.PHOTO_POST, "post_id,attachments", 0, 5);
                    RecordSet t_p = new RecordSet();
                    for (Record rP : share_photo) {
                        Record attach = RecordSet.fromJson(rP.getString("attachments")).getFirstRecord();
                        Record t0 = new Record();
                        t0.put("photo_img_middle", attach.getString("photo_img_middle"));
                        t0.put("photo_img_original", attach.getString("photo_img_original"));
                        t0.put("post_id", rP.getString("post_id"));
                        t_p.add(t0);
                    }
                    r.put("profile_shared_photos", t_p);
                }
            }
            //L.debug("--------------------------222222----------------------"+recs.toString()+"-----------------------------------");
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);

            return dealTopPosts ? dealAccountTopPosts(recs) : recs;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    public RecordSet dealAccountTopPosts(RecordSet users) {
        for (Record user : users) {
            if (user.has("top_posts")) {
                String topName = user.getString("top_name", "");
                String topPostIds = user.getString("top_posts", "");
                int topCount = StringUtils2.splitSet(topPostIds, ",", true).size();
//                RecordSet posts = getFullPostsForQiuPu(user.getString("user_id"), topPostIds, true);
                RecordSet posts = new RecordSet();
                Record topPosts = new Record();
                topPosts.put("name", topName);
                topPosts.put("count", topCount);
                topPosts.put("posts", posts);
                user.put("top_posts", topPosts);
            }
            user.remove("top_name");
        }

        return users;
    }

    @Override
    public String createAccount(Context ctx, String login_email1, String login_phone1, String pwd,
                                String displayName, String nickName, String gender, String imei, String imsi, String device, String location) throws IOException {
        final String METHOD = "createAccount";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, login_email1, login_phone1, pwd, displayName, nickName, gender, imei, imsi, device, location);

        Record rec = Record.of("password", pwd, "display_name", displayName);
        FriendshipLogic friendLogic = GlobalLogics.getFriendship();
        SettingLogic settingLogic = GlobalLogics.getSetting();
        StreamLogic streamLogic = GlobalLogics.getStream();
        SuggestedUserLogic suggestedUserLogic = GlobalLogics.getSuggest();

        rec.putIf("login_email1", login_email1, StringUtils.isNotBlank(login_email1));
        rec.putIf("login_phone1", login_phone1, StringUtils.isNotBlank(login_phone1));
        rec.putIf("gender", gender, StringUtils.isNotBlank(gender));
        rec.putIf("nick_name", nickName, StringUtils.isNotBlank(nickName));
        if (StringUtils.isNotBlank(imei) || StringUtils.isNotBlank(imsi)) {
            Record miscellaneous = Record.of("imei", imei, "imsi", imsi);
            rec.put("miscellaneous", miscellaneous.toJsonNode());
        }
        String userId = createAccountPlatform(ctx, rec);


        Commons.sendNotification(ctx, Constants.NTF_CREATE_ACCOUNT,
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(userId),
                Commons.createArrayNodeFromStrings(userId, device, rec.getString("display_name"), rec.getString("gender")),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(userId),
                Commons.createArrayNodeFromStrings(userId, device, rec.getString("display_name"), rec.getString("gender")),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(userId),
                Commons.createArrayNodeFromStrings(userId, login_email1, login_phone1)
        );

        String m = "";
        String tempNowAttachments = "[]";
        //attachments want from client
        int appid = Constants.APP_TYPE_BPC;


        String message = Constants.getBundleString(device, "platform.create.account.message");


        streamLogic.autoPost(ctx, userId, Constants.TEXT_POST, message, tempNowAttachments, toStr(appid), "", "", m, "", false, "", device, location, true, true, true, "", "", false, Constants.POST_SOURCE_SYSTEM);

        friendLogic.setFriends(ctx, userId, qiupuUid, String.valueOf(Constants.DEFAULT_CIRCLE), Constants.FRIEND_REASON_AUTOCREATE, true);
        try {

            suggestedUserLogic.autoCreateSuggestUsersP(ctx, userId);
            Record values = Record.of("socialcontact.autoaddfriend", "100");

            settingLogic.setPreferences(ctx, userId, values);
        } catch (ServerException ex) {
            L.error(ctx, ex);
            ex.printStackTrace();
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return userId;
    }

    public String createAccountPlatform(Context ctx, Record info) {

        AccountLogic account = GlobalLogics.getAccount();
        FriendshipLogic friendshipLogic = GlobalLogics.getFriendship();
        WutongHooks wutongHook = GlobalLogics.getHooks();
        String userId = toStr(account.createAccount(ctx, info));

        friendshipLogic.createBuiltinCircles(ctx, userId);
        Record r = info.copy();
        r.set("user_id", userId);
        wutongHook.fireUserCreated(ctx, r);
        return userId;

    }

    @Override
    public void checkUserIds(Context ctx, String... userIds) {
        final String METHOD = "method1";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);


        String[] userIds0 = StringUtils2.splitArray(StringUtils.join(userIds, ","), ",", true);
        if (userIds0.length == 1) {
            String userId = userIds[0];
            if (!hasUser(ctx, Long.parseLong(userId))) {
                ServerException e = new ServerException(WutongErrors.USER_NOT_EXISTS, "User '%s' is not exists", userId);
                L.warn(ctx, e);
                throw e;
            }
        } else if (userIds.length > 1) {
            if (!hasAllUsers(ctx, StringUtils.join(userIds0, ","))) {
                ServerException e = new ServerException(WutongErrors.USER_NOT_EXISTS, "User is not exists");
                L.warn(ctx, e);
                throw e;
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }

    public RecordSet getAllRelation(Context ctx, String viewerId, String userIds, String circleId, String inTheirOrInMine) {

        FriendshipLogic fs = GlobalLogics.getFriendship();
        return fs.getAllRelation(ctx, viewerId, userIds, circleId, inTheirOrInMine);

    }

    public List<Long> getWhoSuggest(Context ctx, String to, String beSuggested) {
        Validate.notNull(to);
        Validate.notNull(beSuggested);
        checkUserIds(ctx, to, beSuggested);


        SuggestedUserLogic su = GlobalLogics.getSuggest();
        return StringUtils2.splitIntList(su.getWhoSuggest(ctx, to, beSuggested), ",");

    }

    public boolean isFriend(Context ctx, String sourceUserId, String targetUserId) {
        return isHisFriend(ctx, targetUserId, sourceUserId);
    }

    public RecordSet getRelation(Context ctx, String sourceUserId, String targetUserId) {
        return getRelation(ctx, sourceUserId, targetUserId, "");
    }

    public RecordSet getRelation(Context ctx, String sourceUserId, String targetUserId, String circleId) {
        Validate.notNull(sourceUserId);
        Validate.notNull(targetUserId);
//        checkUserIds(sourceUserId, targetUserId);


        FriendshipLogic fs = GlobalLogics.getFriendship();
        return fs.getRelation(ctx, sourceUserId, targetUserId, circleId);

    }

    public boolean isHisFriend(Context ctx, String sourceUserId, String targetUserId) {
        RecordSet recs = getRelation(ctx, sourceUserId, targetUserId);
        if (recs.isEmpty())
            return false;

        for (Record rec : recs) {
            if (rec.checkGetInt("circle_id") == Constants.BLOCKED_CIRCLE)
                return false;
        }
        return true;
    }

    public int getSharedCount(Context ctx, String viewerId, String userId, int type) {
        StreamLogic stream = GlobalLogics.getStream();
        return stream.getSharedCount(ctx, viewerId, userId, type);

    }

    public int getRelatedPollCount(Context ctx, String viewerId, String userId, int type) {

        StreamLogic stream = GlobalLogics.getStream();
        return stream.getSharedCount(ctx, viewerId, userId, type);

    }

    public long getRelatedPollCount(Context ctx, String viewerId, String userId) {

        PollLogic pollLogic = GlobalLogics.getPoll();
        long id = 0;
        try {
            id = Long.parseLong(userId);
            if (id >= Constants.PUBLIC_CIRCLE_ID_BEGIN && id <= Constants.GROUP_ID_END) {
                String involvedIds = pollLogic.getInvolvedPolls(ctx, viewerId, userId, -1, -1);
                Set<String> involved = StringUtils2.splitSet(involvedIds, ",", true);
                return involved.size();
            }
        } catch (NumberFormatException nfe) {

        }
        String createdIds = pollLogic.getCreatedPolls(ctx, viewerId, userId, -1, -1);
        Set<String> created = StringUtils2.splitSet(createdIds, ",", true);
        return created.size();

    }

    public RecordSet getSocialcontactUsername(Context ctx, String owner, String uid) {
        SocialContactsLogic so = GlobalLogics.getSocialContacts();

        RecordSet recs = so.getUserName(ctx, owner, uid);
        return recs;

    }

    private void doAbsolutePrivateCols(Record rec) {
        if (rec.has("login_email1")) {
            rec.put("login_email1", "");
        }

        if (rec.has("login_email2")) {
            rec.put("login_email2", "");
        }

        if (rec.has("login_email3")) {
            rec.put("login_email3", "");
        }

        if (rec.has("login_phone1")) {
            rec.put("login_phone1", "");
        }

        if (rec.has("login_phone2")) {
            rec.put("login_phone2", "");
        }

        if (rec.has("login_phone3")) {
            rec.put("login_phone3", "");
        }

        if (rec.has("password")) {
            rec.put("password", "");
        }
    }

    protected static String toStr(Object o) {
        return ObjectUtils.toString(o, "");
    }

    private static final Map<String, String> USER_COLUMNS = CollectionUtils2.of(
            "light", USER_LIGHT_COLUMNS,
            "full", USER_STANDARD_COLUMNS);

    public static String parseUserColumns(String cols) {
        return expandColumns(cols, USER_COLUMNS, USER_LIGHT_COLUMNS);
    }

    private static String expandColumns(String cols, Map<String, String> macros, String def) {
        StringBuilder buff = new StringBuilder();
        for (String col : StringUtils2.splitList(cols, ",", true)) {
            if (col.startsWith("#")) {
                String val = macros.get(StringUtils.removeStart(col, "#"));
                if (val == null)
                    val = def;
                buff.append(val);
            } else {
                buff.append(col);
            }
            buff.append(",");
        }
        return StringUtils2.stripItems(buff.toString(), ",", true);
    }


    @Override
    public String parseUserIds(Context ctx, String viewerId, String userIds) {
        final String METHOD = "parseUserIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userIds);

        StringBuilder buff = new StringBuilder();
        for (String userId : StringUtils2.splitList(userIds, ",", true)) {
            if (userId.startsWith("#")) {
                if (!viewerId.equals("0") && !viewerId.equals("")) {
                    String circleId = StringUtils.removeStart(userId, "#");
                    RecordSet friendRecs = getFriends0(ctx, viewerId, circleId, 0, -1);
                    buff.append(friendRecs.joinColumnValues("friend", ","));
                }
            } else {
                buff.append(userId);
                //                if (hasUser(userId)) {
                //                    buff.append(userId);
                //                }
            }
            buff.append(",");
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return StringUtils2.stripItems(buff.toString(), ",", true);
    }


    public RecordSet getFriends0(Context ctx, String userId, String circleIds, int page, int count) {
        FriendshipLogic fs = GlobalLogics.getFriendship();
        return fs.getFriends(ctx, userId, circleIds, page, count);

    }

    private Record findUserByLoginName(Context ctx, String name, String... cols) {
        Schemas.checkSchemaIncludeColumns(userSchema, cols);
        List<String> list = Arrays.asList(cols);
        List<String> colList = new ArrayList<String>();

        colList.addAll(list);
        if (colList.contains("display_name")) {
            colList.remove("display_name");
        }


        final String SQL = "SELECT user_id as id, ${as_join(alias, cols)}"
                + " FROM ${table}" + " JOIN user_property ON user2.user_id = user_property.user AND user_property.key = 13 "
                + " WHERE (CAST(${alias.user_id} AS CHAR)=${v(name)}) OR (${alias.login_email1}=${v(name)} OR ${alias.login_email2}=${v(name)} OR ${alias.login_email3}=${v(name)}"
                + " OR ${alias.login_phone1}=${v(name)} OR ${alias.login_phone2}=${v(name)} OR ${alias.login_phone3}=${v(name)})"
                + " AND destroyed_time=0";

        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "alias", userSchema.getAllAliases(),
                "cols", colList.toArray(),
                "name", name);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        if (!rec.isEmpty()) {
            long id = rec.getInt("user_id");
            User user = this.getUser(ctx, id);
            rec.set("display_name", user.getDisplayName());
        }
        Schemas.standardize(userSchema, rec);
        return rec;
    }

    private RecordSet findUserByLoginNameOrDisplayName(String name) {
        final String SQL = "SELECT user_id"
                + " FROM ${table}" + " "
                + " WHERE (CAST(${alias.user_id} AS CHAR)=${v(name)}) OR (${alias.login_email1}=${v(name)} OR ${alias.login_email2}=${v(name)} OR ${alias.login_email3}=${v(name)}"
                + " OR ${alias.login_phone1}=${v(name)} OR ${alias.login_phone2}=${v(name)} OR ${alias.login_phone3}=${v(name)} OR ${alias.display_name}=${v(name)})"
                + " AND destroyed_time=0";

        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "alias", userSchema.getAllAliases(),
                "name", name);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    public boolean saveTicket(String ticket, String userId, String appId, int type_) {
        final String SQL = "INSERT INTO ${table}"
                + " (${alias.ticket}, ${alias.user}, ${alias.app}, ${alias.created_time}, ${alias.type_})"
                + " VALUES"
                + " (${v(ticket)}, ${v(user_id)}, ${v(app_id)}, ${v(created_time)}, ${v(type_)})";


        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", ticketTable},
                {"alias", ticketSchema.getAllAliases()},
                {"ticket", ticket},
                {"user_id", Long.parseLong(userId)},
                {"app_id", Integer.parseInt(appId)},
                {"created_time", DateUtils.nowMillis()},
                {"type_", type_}
        });

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    private boolean saveTicket(String ticket, String userId, String appId) {
        return saveTicket(ticket, userId, appId, 0);
    }

    private boolean deleteTicket(String ticket) {
        final String SQL = "DELETE FROM ${table} WHERE ${alias.ticket}=${v(ticket)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", ticketSchema.getAllAliases(),
                "table", ticketTable,
                "ticket", ticket);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }


    private Record findUserByTicket(String ticket, String... cols) {
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE ${alias.ticket}=${v(ticket)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", ticketSchema.getAllAliases(),
                "table", ticketTable,
                "ticket", ticket,
                "cols", cols);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(ticketSchema, rec);
        return rec;
    }


    private RecordSet findTicketsByUserId(String userId, String appId, String... cols) {
        final String SQL = "SELECT ${as_join(alias)} FROM ${table} WHERE ${alias.user}=${v(user_id)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", ticketSchema.getAllAliases(),
                "table", ticketTable,
                "user_id", userId);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(ticketSchema, recs);
        return recs;
    }

    /**
     * unfinished
     *
     * @param miscellaneous
     * @return
     */
    private RecordSet findUidByMiscellaneous0(String miscellaneous) {
        final String PLATFORM_SQL_ERROR = "SELECT user as user_id FROM user_property WHERE `key`=27 AND sub =1 AND VALUE  like '%" + miscellaneous + "%'";

        String sql = SQLTemplate.merge(PLATFORM_SQL_ERROR,
                "alias", userSchema.getAllAliases(),
                "table", userTable);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(userSchema, recs);
        return recs;
    }


    private String findUserIdByUserName0(String username) {
        final String SQL = "SELECT ${alias.user_id} FROM ${table} WHERE ${alias.login_email1} like '%" + username + "%' or ${alias.login_email2} like '%" + username + "%' "
                + " or ${alias.login_email3} like '%" + username + "%' or ${alias.login_phone1} like '%" + username + "%' or ${alias.login_phone2} like '%" + username + "%' "
                + " or ${alias.login_phone3} like '%" + username + "%'";
        String sql = SQLTemplate.merge(SQL,
                "alias", userSchema.getAllAliases(),
                "table", userTable);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.size() <= 0 ? "0" : recs.getFirstRecord().getString("user_id");
    }


    private RecordSet searchUserByUserName0(String username, int page, int count) {
        //take out the display_name column for the new account
        final String SQL = "SELECT ${alias.user_id} FROM ${table} WHERE ${alias.login_email1} like '%" + username + "%' or ${alias.login_email2} like '%" + username + "%'"
                + " or ${alias.login_email3} like '%" + username + "%' or ${alias.login_phone1} like '%" + username + "%' or ${alias.login_phone2} like '%" + username + "%' "
                + " or ${alias.login_phone3} like '%" + username + "%' or ${alias.sort_key} like '%" + username + "%' ${limit}";
        String sql = SQLTemplate.merge(SQL,
                "alias", userSchema.getAllAliases(),
                "table", userTable,
                "limit", SQLUtils.pageToLimit(page, count));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }


    private String generateUserId() {
        final String SQL1 = "INSERT INTO ${table} (key_, count_) VALUES ('user', 10000)"
                + " ON DUPLICATE KEY UPDATE count_ = count_ + 1";

        final String SQL2 = "SELECT count_ FROM ${table} WHERE key_ = 'user'";

        String sql1 = SQLTemplate.merge(SQL1, "table", globalCounterTable);
        String sql2 = SQLTemplate.merge(SQL2, "table", globalCounterTable);

        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql1);
        Record rec = se.executeRecord(sql2, null);
        long count = rec.getInt("count_", 0L);
        if (count == 0L)
            throw new ServerException(WutongErrors.USER_GENERAL_ID_ERROR, "Generate user Id error");

        return Long.toString(count);
    }


    private boolean saveUser(Context ctx, Record info) {
        final String METHOD = "saveUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, info);
        Schemas.standardize(userSchema, info);
        //modify the old method, converter Record to User Object

        User user;
        try {
            user = AccountConverter.converterRecord2User(info);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
        } catch (Exception e) {
            L.error(ctx, e);
            e.printStackTrace();
            return false;
        }
        try {
            createUser(ctx, user);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return true;
        } catch (Exception e) {
            L.error(ctx, e);
            return false;
        }

    }


    private boolean deleteUser(String userId, long deleted_time) {
        final String SQL = "UPDATE ${table} SET destroyed_time=${v(deleted_time)} WHERE ${alias.user_id}=${v(user_id)}";
        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "deleted_time", deleted_time,
                "alias", userSchema.getAllAliases(),
                "user_id", Long.parseLong(userId));

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);

        sql = "update suggested_user set refuse_time = " + DateUtils.nowMillis() + " where suggested in (" + userId + ")";
        se.executeUpdate(sql);

        sql = "update suggested_user set refuse_time = " + DateUtils.nowMillis() + " where user in (" + userId + ")";
        se.executeUpdate(sql);

        sql = "delete from suggested_user where type=10 and reason like '%" + userId + "%'";
        se.executeUpdate(sql);

        sql = "delete from request where source in (" + userId + ")";
        se.executeUpdate(sql);

        sql = "update comment set destroyed_time = " + DateUtils.nowMillis() + " where commenter  in (" + userId + ")";
        se.executeUpdate(sql);

        sql = "delete from like_ where liker in (" + userId + ")";
        se.executeUpdate(sql);

        sql = "update social_contacts set uid=0 where uid in (" + userId + ")";
        se.executeUpdate(sql);

        return n > 0;
    }


    private boolean updateUser(Context ctx, Record user) {
        Schemas.standardize(userSchema, user);

        String[] groups = userSchema.getColumnsGroups(user.getColumns());
        long now = DateUtils.nowMillis();
        long userId = user.getInt("user_id", 0);
        if (userId == 0)
            return false;
        User user0 = AccountConverter.converterRecord2User(user);
        return update(ctx, user0);
    }

    private boolean bindUser0(Record user) {
        Schemas.standardize(userSchema, user);

        long userId = user.getInt("user_id", 0);
        if (userId == 0)
            return false;

        String sql = new SQLBuilder.Update(userSchema)
                .update(userTable)
                .values(user)
                .where("${alias.user_id}=${v(user_id)}", "user_id", userId)
                .and("destroyed_time = 0").toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }


    private RecordSet findUsersByUserIds(Context ctx, List<String> userIds, String... cols) {
        long[] longs = List2Array(userIds);

        List<User> users = getUsers(ctx, longs);
        return AccountConverter.convertUserList2RecordSet(users, cols);
    }

    private long[] List2Array(List<String> userIds) {
        if (userIds == null || userIds.size() < 1)
            return null;
        long[] longArray = new long[userIds.size()];
        int i = 0;
        for (String str : userIds) {
            longArray[i] = Long.parseLong(str);
            i++;
        }
        return longArray;
    }

    private RecordSet findUsersPasswordByUserIds(String userIds) {
        final String sql = "SELECT user_id,password FROM user2 WHERE destroyed_time = 0 and user_id in (" + userIds + ")";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(userSchema, recs);
        return recs;
    }


    private boolean updatePasswordByUserId(String userId, String password) {
        final String sql = "update user2 set password='" + password + "' where user_id=" + userId;
        SQLExecutor se = getSqlExecutor();
        return se.executeUpdate(sql) > 0;
    }


    private boolean setPrivacy0(String userId, RecordSet privacyItemList) {
        String sql = "INSERT INTO " + privacyTable + " (user, resource, auths) VALUES ";

        for (Record privacyItem : privacyItemList) {
            String resource = privacyItem.getString("resource");
            String auths = privacyItem.getString("auths");
            if (StringUtils.isBlank(auths)) {
                auths = "";
            }
            sql += "(" + userId + ", '" + resource + "', '" + auths + "'), ";
        }

        sql = StringUtils.substringBeforeLast(sql, ",");
        sql += " ON DUPLICATE KEY UPDATE auths=VALUES(auths)";

        String sql2 = "DELETE FROM " + privacyTable + " WHERE auths=''";

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        se.executeUpdate(sql2);

        return n > 0;
    }

    private List<String> getResourceNodesList(String resource) {
        List<String> l = new ArrayList<String>();
        String[] p = StringUtils2.splitArray(resource, ".", true);
        for (int i = p.length - 1; i >= 0; i--) {
            String temp = "";
            for (int j = 0; j <= i; j++) {
                temp += (p[j] + ".");
            }
            l.add(StringUtils.substringBeforeLast(temp, "."));
        }

        return l;
    }

    private String[] getResourceNodesArray(String resource) {
        String[] p = StringUtils2.splitArray(resource, ".", true);
        String[] arr = new String[p.length];
        for (int i = p.length - 1; i >= 0; i--) {
            String temp = "";
            for (int j = 0; j <= i; j++) {
                temp += (p[j] + ".");
            }
            arr[i] = StringUtils.substringBeforeLast(temp, ".");
        }

        return arr;
    }

    private Record findResourceAuths(String resource, RecordSet recs, String[] nodes) {
        Record privacyItem = new Record();
        privacyItem.put("resource", resource);
        privacyItem.put("auths", "");

        for (int i = nodes.length - 1; i >= 0; i--) {
            for (Record rec : recs) {
                if (rec.getString("resource").contains(nodes[i])) {
                    privacyItem.put("auths", rec.getString("auths"));
                    return privacyItem;
                }
            }
        }

        return privacyItem;
    }


    private RecordSet getAuths0(String userId, List<String> resources) {
        ArrayList<String> l = new ArrayList<String>();
        for (String resource : resources) {
            l.addAll(getResourceNodesList(resource));
        }

        final String SQL = "SELECT ${alias.resource},${alias.auths} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.resource} IN (${vjoin(l)})";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", privacyTable},
                {"alias", privacySchema.getAllAliases()},
                {"user", userId},
                {"l", l}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(privacySchema, recs);

        RecordSet privacyItemList = new RecordSet();

        for (String resource : resources) {
            String[] nodes = getResourceNodesArray(resource);
            Record privacyItem = findResourceAuths(resource, recs, nodes);
            privacyItemList.add(privacyItem);
        }

        return privacyItemList;
    }


    private RecordSet getUsersAuths0(String userIds) {
        String sql = "";
        if (userIds.length() > 0)
            return new RecordSet();

        sql = "select * from privacy where user in (" + userIds + ")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    public RecordSet getAlluser() {
        String sql = "";
        sql = "select display_name,user_id from user2";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    /**
     * modify the sql String to t adapt to the new version
     *
     * @param all
     * @return
     */
    private RecordSet findAllUserIds0(boolean all) {
        String sql = "";
        if (all) {
            sql = "SELECT user2.user_id,GROUP_CONCAT(user_property.value SEPARATOR '') AS display_name FROM user2 JOIN user_property ON user2.user_id = user_property.user AND user_property.key = 13 GROUP BY user2.user_id ORDER BY user2.user_id";
        } else {
            sql = "select user2.user_id,group_concat(user_property.value SEPARATOR '') AS display_name from user2 join user_property on user2.user_id = user_property.user and user_property.key = 13 and user2.destroyed_time=0 group by user2.user_id order by user2.user_id;";
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    public Record login(Context ctx, String name, String password, String appId) {
        final String METHOD = "login";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, name, password, appId);

        final String PASSKEY = Encoders.md5Hex("_passkey_passw0rd_");
        System.out.println("=== login");
        try {
            String name0 = name;
            String password0 = password;
            String appId0 = appId;
            Record rec = findUserByLoginName(ctx, name0, "user_id", "password", "display_name");
            if (rec.isEmpty()) {
                ServerException e = new ServerException(WutongErrors.USER_NAME_PASSWORD_ERROR, "Login name or password error");
                L.warn(ctx, e);
                throw e;
            }

            String userId = rec.getString("user_id");
            if (!PASSKEY.equals(password0)) {
                System.out.println("==== passkey");
                if (!StringUtils.equalsIgnoreCase(password0, rec.getString("password"))) {
                    ServerException e = new ServerException(WutongErrors.USER_NAME_PASSWORD_ERROR, "Login name or password error");
                    L.warn(ctx, e);
                    throw e;
                }
            } else {
                System.out.println("==== not passkey:" + password0);
            }

            String ticket = genTicket(name0);
            boolean b = saveTicket(ticket, userId, appId0);
            if (!b) {
                ServerException e = new ServerException(WutongErrors.USER_GENERAL_SESSION_ERROR, "Create session error");
                L.warn(ctx, e);
                throw e;
            }
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            //updateUser(Record.of("user_id", userId, "last_visited_time", DateUtils.nowMillis()));
            return Record.of(new Object[][]{
                    {"user_id", userId},
                    {"ticket", ticket},
                    {"display_name", rec.getString("display_name")},
                    {"login_name", name0},
            });
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    public static String genTicket(String loginName) {
        return Encoders.toBase64(loginName + "_" + DateUtils.nowMillis() + "_" + new Random().nextInt(10000));
    }

    @Override
    public Record genTicketForEmail(Context ctx, String loginName) {
        String userId = findUserByLoginName(ctx, loginName, "user_id").getString("user_id", "0");
        if (StringUtils.equals(userId, "0")) {
            throw new ServerException(WutongErrors.USER_NOT_EXISTS);
        } else {
            String sql = "SELECT ticket FROM ticket WHERE user=" + userId + " AND type_=1";
            SQLExecutor se = getSqlExecutor();
            Object obj = se.executeScalar(sql);
            if (obj == null) {
                return Record.of("user_id", userId, "ticket", genTicket(loginName));
            } else {
                return Record.of("user_id", userId, "ticket", ObjectUtils.toString(obj));
            }
        }
    }

    @Override
    public final boolean logout(Context ctx, String ticket) {
        final String METHOD = "login";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, ticket);
        try {
            String ticket0 = ticket;
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return deleteTicket(ticket0);
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public final String whoLogined(Context ctx, String ticket) {
        //final String METHOD = "whoLogined";
        //if (L.isTraceEnabled())
        //    L.traceStartCall(ctx, METHOD, ticket);
        try {
            Record rec = findUserByTicket(ticket, "user");
            //    if (L.isTraceEnabled())
            //        L.traceEndCall(ctx, METHOD);
            return rec.getString("user", Constants.NULL_USER_ID);
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public final RecordSet getLogined(Context ctx, String userId, String appId) {
        final String METHOD = "whoLogined";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, appId);
        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return findTicketsByUserId(userId, appId,
                    "ticket", "user", "app", "created_time");
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    public boolean checkLoginNameNotExists(Context ctx, String uid, String names) {
        String[] arrNames = StringUtils2.splitArray((names), ",", true);
        checkLoginNameNotExists(ctx, uid, arrNames);

        return true;
    }

    @Override
    public boolean checkBindNameNotExists(Context ctx, String names) {
        final String METHOD = "checkBindNameNotExists";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, names);

        List<String> nameList = StringUtils2.splitList((names), ",", true);
        boolean b = true;
        for (String name : nameList) {
            if (!name.equals("")) {
                Record rec = findUserByLoginName(ctx, name, "user_id");
                String userid = rec.getString("user_id");
                if (!rec.isEmpty() && !userid.equals("")) {
                    b = false;
                    break;
                }
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;
    }

    /*@Override
    public RecordSet getUsersAuths(Context ctx,String userIds)  {
        return getUsersAuths0((userIds));
    }*/

    @Override
    public final String createAccount(Context ctx, Record info) {
        final String METHOD = "createAccount";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, info);
        try {
            Record pf = info;

            // check missing columns
            Schemas.checkRecordIncludeColumns(pf, "password", "display_name");
            if (!pf.has("login_email1") && !pf.has("login_phone1"))
                throw new ServerException(0, "Must include column 'login_email1' or 'login_phone1'");

            // get values
            String login_email1 = pf.getString("login_email1", "");
            String login_phone1 = pf.getString("login_phone1", "");
            String displayName = pf.getString("display_name");
            String nickName = pf.getString("nick_name", " ");
            String password = pf.getString("password");
            String gender = pf.getString("gender", "m");
            String marriage = pf.getString("marriage", "n");
            String miscellaneous = pf.getString("miscellaneous", "{}");

            // check values
            if (StringUtils.isNotBlank(login_email1)) {
                checkLoginName(login_email1);
                checkEmail(login_email1);
            }

            if (StringUtils.isNotBlank(login_phone1)) {
                checkLoginName(login_phone1);
                checkPhone(login_phone1);
            }
            checkDisplayName(displayName);
            checkPassword(password);

            checkLoginNameNotExists(ctx, login_phone1, login_email1);

            // generate user_id
            String userId = generateUserId();

            // create record
            Record rec = new Record();
            rec.put("user_id", userId);
            rec.put("login_phone1", login_phone1);
            rec.put("login_email1", login_email1);
            rec.put("password", password);
            rec.put("display_name", displayName);
            rec.put("nick_name", nickName);
            rec.put("gender", gender);
            rec.put("miscellaneous", miscellaneous);
            rec.put("created_time", DateUtils.nowMillis());
            rec.put("marriage", marriage);

            NameSplitter nm = new NameSplitter("Mr, Ms, Mrs", "d', st, st., von", "Jr., M.D., MD, D.D.S.",
                    "&, AND", Locale.CHINA);

            final NameSplitter.Name name = new NameSplitter.Name();
            nm.split(name, displayName);

//            Map name_map = new HashMap();
//            name_map = StringUtils2.trandsUserName(displayName);

            String first_name = "";
            String middle_name = "";
            String last_name = "";
            if (name.getGivenNames() != null) {
                first_name = name.getGivenNames();
            }
            if (name.getMiddleName() != null) {
                middle_name = name.getMiddleName();
            }
            if (name.getFamilyName() != null) {
                last_name = name.getFamilyName();
            }
            rec.put("first_name", first_name);
            rec.put("middle_name", middle_name);
            rec.put("last_name", last_name);


            Record contactInfo = new Record();
            if (StringUtils.isNotBlank(login_phone1) && !StringUtils.equalsIgnoreCase(login_phone1, "null")) {
                contactInfo.put("mobile_telephone_number", login_phone1);
            }
            if (StringUtils.isNotBlank(login_email1) && !StringUtils.equalsIgnoreCase(login_email1, "null")) {
                contactInfo.put("email_address", login_email1);
            }

            rec.putMissing("contact_info", JsonUtils.parse(JsonUtils.toJson(contactInfo, false)));
            rec.putMissing("address", "[]");
            rec.putMissing("work_history", "[]");
            rec.putMissing("education_history", "[]");

            // save
            boolean b = saveUser(ctx, rec);
            if (!b) {
                ServerException e = new ServerException(WutongErrors.SYSTEM_DB_ERROR, "Save user info error");
                L.error(ctx, e);
                throw e;
            }

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return userId;
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }


    @Override
    public final boolean destroyAccount(Context ctx, String userId) {
        final String METHOD = "destroyAccount";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);
        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return deleteUser((userId), DateUtils.nowMillis());
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public String resetPassword(Context ctx, String loginName) {
        final String METHOD = "resetPassword";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, loginName);
        try {
            String loginName0 = (loginName);

            String newPassword = RandomUtils.generateRandomNumberString(6);
            String userId = findUserIdByLoginName(ctx, loginName0);
            if (StringUtils.isEmpty(userId)) {
                ServerException e = new ServerException(WutongErrors.USER_NOT_EXISTS, "User '%s' is not exists", loginName0);
                L.warn(ctx, e);
                throw e;
            }

            Record info = Record.of("user_id", userId, "password", Encoders.md5Hex(newPassword));
            updateUser(ctx, info);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return newPassword;
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public boolean updateAccount(Context ctx, String userId, Record info) {
        final String METHOD = "updateAccount";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, info);
        try {
            String userId0 = (userId);
            Record user = info;
            Schemas.checkRecordExcludeColumns(user, "user_id");
            Schemas.checkRecordExcludeColumns(user, userSchema.getColumnNames("unmodified"));

            if (user.has("contact_info")) {
                String contact_info = user.getString("contact_info");
                JsonNode jn = JsonUtils.parse(contact_info);

                Schemas.standardize(contactSchema, jn);
                //Schemas.checkRecordColumnsIn(JsonUtils.checkRecord(jn), "type", "info");
            }

            if (user.has("address")) {
                String address = user.getString("address");
                JsonNode jn = JsonUtils.parse(address);

                Schemas.standardize(addressSchema, jn);
                for (int i = 0; i < jn.size(); i++) {
                    JsonNode node = jn.get(i);
                    if (node instanceof ObjectNode)
                        Schemas.checkRecordColumnsIn(JsonUtils.checkRecord(node),
                                "type", "country", "state", "city", "street", "postal_code", "po_box", "extended_address");
                }

            }

            if (user.has("work_history")) {
                String work_history = user.getString("work_history");
                JsonNode jn = JsonUtils.parse(work_history);

                Schemas.standardize(workSchema, jn);
                ArrayNode an = JsonUtils.checkRecordSet(jn);
                int size = an.size();
                for (int i = 0; i < size; i++) {
                    JsonNode jn0 = an.get(i);
                    Schemas.checkRecordColumnsIn(JsonUtils.checkRecord(jn0),
                            "from", "to", "company", "address", "title", "profession", "description");
                }
            }

            if (user.has("education_history")) {
                String education_history = user.getString("education_history");
                JsonNode jn = JsonUtils.parse(education_history);

                Schemas.standardize(educationSchema, jn);
                ArrayNode an = JsonUtils.checkRecordSet(jn);
                int size = an.size();
                for (int i = 0; i < size; i++) {
                    JsonNode jn0 = an.get(i);
                    Schemas.checkRecordColumnsIn(JsonUtils.checkRecord(jn0),
                            "from", "to", "type", "school", "class", "degree", "major");
                }
            }

            if (user.has("display_name")) {
//                Map name_map = new HashMap();
//                name_map = StringUtils2.trandsUserName(user.getString("display_name"));

                NameSplitter nm = new NameSplitter("Mr, Ms, Mrs", "d', st, st., von", "Jr., M.D., MD, D.D.S.",
                        "&, AND", Locale.CHINA);

                final NameSplitter.Name name = new NameSplitter.Name();
                nm.split(name, user.getString("display_name"));

                String first_name = "";
                String middle_name = "";
                String last_name = "";
                if (name.getGivenNames() != null) {
                    first_name = name.getGivenNames();
                }
                if (name.getMiddleName() != null) {
                    middle_name = name.getMiddleName();
                }
                if (name.getFamilyName() != null) {
                    last_name = name.getFamilyName();
                }
                user.put("first_name", first_name);
                user.put("middle_name", middle_name);
                user.put("last_name", last_name);
            }

            //System.out.println(user);
//            checkLoginNameNotExists(userId0,
//                    user.getString("login_phone1", null), user.getString("login_phone2", null), user.getString("login_phone3", null),
//                    user.getString("login_email1", null), user.getString("login_email2", null), user.getString("login_email3", null));

            user.put("user_id", userId);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return updateUser(ctx, user);
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public boolean bindUser(Context ctx, String userId, Record info) {
        final String METHOD = "bindUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, info);
        try {
            Record user = info;
            user.put("user_id", userId);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return updateUser(ctx, user);
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public RecordSet findAllUserIds(Context ctx, boolean all) {
        final String METHOD = "findAllUserIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, all);
        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return findAllUserIds0(all);
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public RecordSet getUsersPasswordByUserIds(Context ctx, String userIds) {
        final String METHOD = "getUsersPasswordByUserIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return findUsersPasswordByUserIds((userIds));
    }

    @Override
    public boolean changePasswordByUserId(Context ctx, String userId, String password) {
        final String METHOD = "getUsersPasswordByUserIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, password);
        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return updatePasswordByUserId((userId), (password));
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public RecordSet getUsers(Context ctx, String userIds, String cols) {
        final String METHOD = "getUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds, cols);
        try {
            List<String> userIds0 = StringUtils2.splitList((userIds), ",", true);
            String[] cols0 = StringUtils2.splitArray((cols), ",", true);

            if (userIds0.isEmpty() || cols0.length == 0) {
                if (L.isTraceEnabled())
                    L.traceEndCall(ctx, METHOD);
                return new RecordSet();
            }
            //Schemas.checkSchemaIncludeColumns(userSchema, cols0);
            RecordSet recs = findUsersByUserIds(ctx, userIds0, cols0);
            if (recs != null) {
                for (Record rec : recs) {
                    addImageUrlPreifx(profileImagePattern, sysIconUrlPattern, rec);
                }
                if (L.isTraceEnabled())
                    L.traceEndCall(ctx, METHOD);
                return recs;
            } else {
                if (L.isTraceEnabled())
                    L.traceEndCall(ctx, METHOD);
                return new RecordSet();
            }
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public RecordSet findUidByMiscellaneous(Context ctx, String miscellaneous) {
        final String METHOD = "findUidByMiscellaneous";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, miscellaneous);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return findUidByMiscellaneous0((miscellaneous));
    }

    @Override
    public RecordSet getUserIds(Context ctx, String loginNames) {
        final String METHOD = "getUserIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, loginNames);
        try {
            List<String> names0 = StringUtils2.splitList((loginNames), ",", true);
            RecordSet recs = new RecordSet();
            for (String name : names0) {
                String userId = findUserIdByLoginName(ctx, name);
                recs.add(Record.of("user_id", userId, "login_name", name));
            }
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public RecordSet getUserIdsByNames(Context ctx, String loginNames) {
        final String METHOD = "getUserIdsByNames";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, loginNames);
        try {
            RecordSet recs = findUserByLoginNameOrDisplayName((loginNames));
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return recs;
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public RecordSet hasUsers(Context ctx, String userIds) {
        final String METHOD = "hasUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);
        List<String> userIds0 = StringUtils2.splitList((userIds), ",", true);
        RecordSet recs = new RecordSet();
        for (String userId : userIds0) {
            boolean b = !findUsersByUserIds(ctx, Arrays.asList(userId), "user_id").isEmpty();
            recs.add(Record.of("user_id", Long.parseLong(userId), "result", b));
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet searchUserByUserName(Context ctx, String username, int page, int count) {
        final String METHOD = "searchUserByUserName";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, username, page, count);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return searchUserByUserName0((username), page, count);
    }

    @Override
    public boolean hasOneUsers(Context ctx, String userIds) {
        final String METHOD = "hasOneUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);
        List<String> userIds0 = StringUtils2.splitList((userIds), ",", true);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        if (userIds0.isEmpty())
            return false;

        RecordSet recs = findUsersByUserIds(ctx, userIds0, "user_id");
        return !recs.isEmpty();
    }

    @Override
    public boolean hasAllUsers(Context ctx, String userIds) {
        final String METHOD = "hasAllUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds);
        /*Set<String> userIds0 = StringUtils2.splitSet((userIds), ",", true);

        int inl = userIds0.size();
        if (userIds0.size() > 0) {
            RecordSet recs = findUsersByUserIds(new ArrayList<String>(userIds0), "user_id");
            if (inl > recs.size()) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }*/
        Set set = StringUtils2.splitSet((userIds), ",", true);
        List list = new ArrayList();
        list.addAll(set);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return hasAllUsers(list, true);
    }

    private boolean hasAllUsers(List<String> userIds, boolean isDestroyed) {
        if (userIds.size() == 0)
            return true;

        String sql = "";
        if (isDestroyed) {
            sql = "SELECT count(user_id) from user2 where user_id in (" + StringUtils.join(userIds.toArray(), ",") + ")";
        } else {
            sql = "SELECT count(user_id) from user2 where destroyed_time=0 and user_id in (" + StringUtils.join(userIds.toArray(), ",") + ")";
        }
        SQLExecutor se = getSqlExecutor();
        long recs = se.executeIntScalar(sql, 0);
        return userIds.size() == recs;
    }

    @Override
    public String findUserIdByUserName(Context ctx, String username) {
        final String METHOD = "findUserIdByUserName";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, username);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return findUserIdByUserName0((username));
    }

    @Override
    public boolean setPrivacy(Context ctx, String userId, RecordSet privacyItemList) {
        final String METHOD = "setPrivacy";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, privacyItemList);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return setPrivacy0((userId), privacyItemList);
    }

    @Override
    public RecordSet getAuths(Context ctx, String userId, String resources) {
        final String METHOD = "getAuths";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, resources);
        String resources0 = (resources);
        List<String> rl = StringUtils2.splitList(resources0, ",", true);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return getAuths0((userId), rl);
    }

    @Override
    public boolean getDefaultPrivacy(Context ctx, String resource, String circleId) {
        final String METHOD = "getDefaultPrivacy";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, resource, circleId);
        String res = (resource);
        int circle = Integer.parseInt((circleId));
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        switch (circle) {
            case Constants.ADDRESS_BOOK_CIRCLE: {
                return true;
            }
            case Constants.FAMILY_CIRCLE: {
                return true;
            }
            case Constants.CLOSE_FRIENDS_CIRCLE: {
                return true;
            }
            case Constants.DEFAULT_CIRCLE: {
                if (StringUtils.contains(res, Constants.RESOURCE_PHONEBOOK)
                        || StringUtils.contains(res, Constants.RESOURCE_BUSINESS)) {
                    return false;
                } else {
                    return true;
                }
            }
            case Constants.BLOCKED_CIRCLE: {
                return false;
            }
            default: {
//			if(StringUtils.contains(res, Constants.RESOURCE_COMMON))
//			{
//				return true;
//			}
//			else
//			{
                return false;
//			}
            }
        }
    }


    public void checkLoginNameNotExists(Context ctx, String uid, String... names) {
        for (String name : names) {
            if (StringUtils.isBlank(name))
                continue;
            Record rec = findUserByLoginName(ctx, name, "user_id");
            String userid = rec.getString("user_id");
            if (!userid.equals(uid) && !rec.isEmpty())
                throw new ServerException(WutongErrors.USER_LOGIN_NAME_EXISTED, "The login name is existing");
        }
    }

    public static void checkLoginName(String loginName) {
        if (loginName != null && StringUtils.isBlank(loginName))
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "Invalid login name");
    }

    public static void checkDisplayName(String displayName) {
        if (displayName != null && StringUtils.isBlank(displayName))
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "Invalid display name");
    }

    public static void checkPassword(String pwd) {
        if (pwd != null && pwd.isEmpty())
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "Invalid password");
    }

    public static void checkPhone(String phone) {
        if (phone != null && !phone.matches("^+?\\d+$"))
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "Invalid phone number");
    }

    public static void checkEmail(String email) {
        if (email != null && !email.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"))
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "Invalid email");
    }

    private String findUserIdByLoginName(Context ctx, String name) {
        return findUserByLoginName(ctx, name, "user_id").getString("user_id");
    }

    public static void addImageUrlPreifx(String profileImagePattern, String sysIconUrlPattern, Record rec) {
        if (rec.has("image_url")) {
            String pattern = profileImagePattern;
            String image_url = rec.getString("image_url");
            if (StringUtils.isBlank(image_url)) {
                pattern = sysIconUrlPattern;
                image_url = "1.gif";
                if (rec.has("gender") && rec.getString("gender").equals("f")) {
                    image_url = "0.gif";
                }
            }
            rec.put("image_url", String.format(pattern, image_url));
        }
        if (rec.has("small_image_url")) {
            String pattern = profileImagePattern;
            String small_image_url = rec.getString("small_image_url");
            if (StringUtils.isBlank(small_image_url)) {
                pattern = sysIconUrlPattern;
                small_image_url = "1_S.jpg";
                if (rec.has("gender") && rec.getString("gender").equals("f")) {
                    small_image_url = "0_S.jpg";
                }
            }
            rec.put("small_image_url", String.format(pattern, small_image_url));
        }
        if (rec.has("large_image_url")) {
            String pattern = profileImagePattern;
            String large_image_url = rec.getString("large_image_url");
            if (StringUtils.isBlank(large_image_url)) {
                pattern = sysIconUrlPattern;
                large_image_url = "1_L.jpg";
                if (rec.has("gender") && rec.getString("gender").equals("f")) {
                    large_image_url = "0_L.jpg";
                }
            }
            rec.put("large_image_url", String.format(pattern, large_image_url));
        }
    }

    @Override
    public String findLongUrl(Context ctx, String short_url) {
        final String METHOD = "getDefaultPrivacy";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, short_url);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        try {
            return findLongUrl0((short_url));
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    protected String findLongUrl0(String short_url) {
        long dateTime = DateUtils.nowMillis();
        String sql = "select long_url from short_url where short_url='" + short_url + "'";
//        String sql = "select long_url from short_url where short_url='" + short_url + "' and failure_time>" + dateTime + "";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        String out_url = !rec.isEmpty() ? rec.getString("long_url") : "";
//        if (!rec.isEmpty()) {
//            if (!short_url.contains("borqs.com/z/v2mIRf")) {
//                String sql1 = "update short_url set failure_time = " + dateTime + " where short_url='" + short_url + "'";
//                se.executeUpdate(sql1);
//            }
//        }
        return out_url;
    }

    @Override
    public boolean saveShortUrl(Context ctx, String long_url, String short_url) {
        final String METHOD = "saveShortUrl";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, long_url, short_url);

        try {
            if (L.isOpEnabled())
                L.op(ctx, "saveShortUrl");
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return saveShortUrl0((long_url), (short_url));
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    protected boolean saveShortUrl0(String long_url, String short_url) {
        if (findLongUrl0(short_url).equals("")) {
            long dateTime = DateUtils.nowMillis();
            dateTime += 3 * 24 * 60 * 60 * 1000L;
            final String sql = "INSERT INTO short_url"
                    + " (long_url,short_url,failure_time)"
                    + " VALUES"
                    + " ('" + long_url + "','" + short_url + "','" + dateTime + "')";
            SQLExecutor se = getSqlExecutor();
            se.executeUpdate(sql);
        }
        return true;
    }


    @Override
    public boolean updateVisitTime(Context ctx, String userId, long time) {
        final String METHOD = "updateVisitTime";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);

        long userIdS = Long.parseLong(userId.toString());

        if (L.isOpEnabled())
            L.op(ctx, "updateVisitTime");

        boolean b = db.updateVisitTime(userIdS, time);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;
    }

    @Override
    public final String getNowGenerateUserId(Context ctx) {
        final String METHOD = "getNowGenerateUserId";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD);

        try {
            if (L.isOpEnabled())
                L.op(ctx, "getNowGenerateUserId");
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return generateUserId();
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    protected Record findUserByLoginNameNotInID(String name, String... cols) {
        Schemas.checkSchemaIncludeColumns(userSchema, cols);

        final String SQL = "SELECT ${as_join(alias, cols)}"
                + " FROM ${table}"
                + " WHERE (${alias.login_email1}=${v(name)} OR ${alias.login_email2}=${v(name)} OR ${alias.login_email3}=${v(name)}"
                + " OR ${alias.login_phone1}=${v(name)} OR ${alias.login_phone2}=${v(name)} OR ${alias.login_phone3}=${v(name)})"
                + " AND destroyed_time=0";

        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "alias", userSchema.getAllAliases(),
                "cols", cols,
                "name", name);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(userSchema, rec);
        return rec;
    }


    public RecordSet getUserByIdBaseColumns(Context ctx, String userIds) {
        if (userIds.length() > 0) {
            final String sql = "select user_id,display_name,perhaps_name from user2 where user_id in (" + userIds + ") and destroyed_time=0";
            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            final String sql1 = "select user,`key`,`value`,sub from user_property where user in (" + userIds + ") and `key`=15 and `type`=4 and sub=1";
            RecordSet recs_p = se.executeRecordSet(sql1, null);
            Map p_map = new HashMap();
            for (Record ur : recs_p) {
                p_map.put(ur.getString("user"), ur.getString("value"));
            }

            for (Record rec : recs) {
                if (p_map.get(rec.getString("user_id")) != null) {
                    rec.put("image_url", p_map.get(rec.getString("user_id")));
                } else {
                    rec.put("image_url", "");
                }
            }
            for (Record rec : recs) {
                addImageUrlPreifx(profileImagePattern, sysIconUrlPattern, rec);
            }
            return recs;
        } else {
            return new RecordSet();
        }
    }

    @Override
    public Record findUidLoginNameNotInID(Context ctx, String name) {
        final String METHOD = "findUidLoginNameNotInID";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, name);
        try {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return findUserByLoginNameNotInID(name, "user_id");
        } catch (Throwable t) {
            ServerException e = ErrorUtils.wrapResponseError(t);
            L.error(ctx, e);
            throw e;
        }
    }

    @Override
    public String getBorqsUserIds(Context ctx) {
        final String METHOD = "getBorqsUserIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD);
        final String sql = "select user_id from user2 where login_email1 like '%borqs.com' or login_email2 like '%borqs.com' or login_email3 like '%borqs.com'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs.joinColumnValues("user_id", ",");
    }

    @Override
    public boolean resetPassword(Context ctx, String loginName, String key, String lang) {
        final String METHOD = "resetPassword";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, loginName, key, lang);

        AccountLogic account = GlobalLogics.getAccount();
        EmailLogic emailLogic = GlobalLogics.getEmail();
        String[] emails = new String[3];

        if (StringUtils.isBlank(key)) {
            RecordSet userIdRs = account
                    .getUserIds(ctx, loginName);
            String userId = userIdRs.getFirstRecord().getString("user_id");
            if (StringUtils.isEmpty(userId))
                throw new ServerException(WutongErrors.USER_NOT_EXISTS,
                        "User '%s' is not exists", loginName);
            RecordSet recs = account.getUsers(ctx,
                    userId, "login_email1,login_email2,login_email3, display_name");
            Record rec = recs.getFirstRecord();
            emails[0] = rec.getString("login_email1");
            emails[1] = rec.getString("login_email2");
            emails[2] = rec.getString("login_email3");
            String displayName = rec.getString("display_name");

            if (StringUtils.isBlank(emails[0]) && StringUtils.isBlank(emails[1]) && StringUtils.isBlank(emails[2])) {
                ServerException e = new ServerException(WutongErrors.USER_EMAIL_NOT_EXISTS, "Do not have any binded email.");
                L.warn(ctx, e);
                throw e;
            }

            if (loginName.indexOf(".") != -1) {
                loginName = loginName.replace(".", "borqsdotborqs");
            }
            if (loginName.indexOf("@") != -1) {
                loginName = loginName.replace("@", "borqsatborqs");
            }

            key = Encoders.desEncryptBase64(loginName + "/" + new Date().getTime());
            String url = "http://" + SERVER_HOST + "/account/reset_password?key=" + key;
            //                String content = "		Hello, if you confirm that you forget Borqs's password, "
            //                        + "please click the link as below in 72 hours: <br>"
            //                        + "<a href=\"" + url + "\">" + url + "</a>";
            String template = Constants.getBundleStringByLang(lang, "platform.reset.password.content");
            String content = SQLTemplate.merge(template, new Object[][]{
                    {"url", url}
            });
            String title = Constants.getBundleStringByLang(lang, "platform.reset.password.title");
            for (String email : emails) {
                if (StringUtils.isNotBlank(email)) {
                    emailLogic.sendEmail(ctx, title, email, displayName, content, Constants.EMAIL_ESSENTIAL, lang);
                }
            }
        } else {
            key = StringUtils.replace(key, " ", "+");
            long validPeriod = 3L * 24 * 60 * 60 * 1000; //email valid period: 3days
            String info = Encoders.desDecryptFromBase64(key);
            int index = info.lastIndexOf("/");
            loginName = info.substring(0, index);
            long valid = Long.parseLong(info.substring(index + 1));
            if (valid < (new Date().getTime() - validPeriod)) {
                ServerException e = new ServerException(WutongErrors.USER_VERIFY_LINK_EXPIRED, "The link is expired");
                L.warn(ctx, e);
                throw e;
            }

            if (loginName.indexOf("borqsdotborqs") != -1) {
                loginName = loginName.replaceAll("borqsdotborqs", ".");
            }
            if (loginName.indexOf("borqsatborqs") != -1) {
                loginName = loginName.replaceAll("borqsatborqs", "@");
            }
            String newPwd = toStr(account.resetPassword(ctx, loginName));

            //                String content = "		Hello, your Borqs account: "
            //                        + loginName
            //                        + " password have been changed as: "
            //                        + newPwd
            //                        + ".<br>"
            //                        + "              Please use it to login Borqs application and then modify your password soon.";

            String template = Constants.getBundleStringByLang(lang, "platform.reset.password.message");
            String content = SQLTemplate.merge(template, new Object[][]{
                    {"loginName", loginName},
                    {"newPwd", newPwd}
            });

            RecordSet userIdRs = account
                    .getUserIds(ctx, loginName);
            String userId = userIdRs.getFirstRecord().getString("user_id");
            if (StringUtils.isEmpty(userId)) {
                ServerException e = new ServerException(WutongErrors.USER_NOT_EXISTS,
                        "User '%s' is not exists", loginName);
                L.warn(ctx, e);
                throw e;
            }
            RecordSet recs = account.getUsers(ctx,
                    userId, "login_email1,login_email2,login_email3, display_name");
            Record rec = recs.getFirstRecord();
            emails[0] = rec.getString("login_email1");
            emails[1] = rec.getString("login_email2");
            emails[2] = rec.getString("login_email3");
            String displayName = rec.getString("display_name");

            if (StringUtils.isBlank(emails[0]) && StringUtils.isBlank(emails[1]) && StringUtils.isBlank(emails[2])) {
                ServerException e = new ServerException(WutongErrors.USER_EMAIL_NOT_EXISTS, "Do not have any binded email.");
                L.warn(ctx, e);
                throw e;
            }

            for (String email : emails) {
                if (StringUtils.isNotBlank(email)) {
                    emailLogic.sendEmail(ctx, Constants.getBundleStringByLang(lang, "platform.reset.password.msgtitle"), email, displayName, content, Constants.EMAIL_ESSENTIAL, lang);
                }
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;

    }

    @Override
    public boolean resetPasswordForPhone(Context ctx, String loginName) {
        final String METHOD = "resetPasswordForPhone";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, loginName, loginName);
        loginName = StringUtils.trimToEmpty(loginName);
        if (!StringUtils.isNumeric(loginName))
            return false;

        AccountLogic account = GlobalLogics.getAccount();
        String userId = account.getUserIds(ctx, loginName).getFirstRecord().getString("user_id");
        if (StringUtils.isEmpty(userId) || "0".equals(userId)) {
            ServerException e = new ServerException(WutongErrors.USER_NOT_EXISTS,
                    "User '%s' is not exists", loginName);
            L.warn(ctx, e);
            throw e;
        }
        Record userRec = account.getUsers(ctx, userId, "user_id,password").getFirstRecord();
        String md5OldPwd = userRec.getString("password");
        L.op(ctx, "resetPasswordForPhone");
        String newPwd = account.resetPassword(ctx, loginName);
        String md5NewPwd = Encoders.md5Hex(newPwd);

        syncBorqsBbsPwd(loginName, md5OldPwd, md5NewPwd);
        sendNewPasswordToPhone(loginName, newPwd);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;

    }

    private boolean syncBorqsBbsPwd(String phone, String md5OldPwd, String md5NewPwd) {
        boolean b = GlobalConfig.get().getBoolean("platform.syncBbsPwd", false);
        if (b) {
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("login_phone", phone));
            params.add(new BasicNameValuePair("oldpwd", md5OldPwd));
            params.add(new BasicNameValuePair("newpwd", md5NewPwd));
            HttpGet g = new HttpGet("http://bbs.borqs.com/account/change_password?" + URLEncodedUtils.format(params, "UTF-8"));
            HttpClient client = new DefaultHttpClient();
            try {
                HttpResponse resp = client.execute(g);
                String s = IOUtils.toString(resp.getEntity().getContent());
                return true;
            } catch (IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private void sendNewPasswordToPhone(String phone, String newPwd) {
        String smsHost = GlobalConfig.get().getString("phoneVerification.smsHost", null);
        if (smsHost == null)
            throw new ServerException(WutongErrors.SYSTEM_MESSAGE_GATEWAY_HOST_ERROR, "Send sms error");

        String text = "您的密码已经重置成功，新密码是 " + newPwd + "。";
        try {
            HttpClient client = new DefaultHttpClient();
            //HttpPost httpPost = new HttpPost("http://" + smsHost + "/smsgw/sendsms.php");
            HttpPost httpPost = new HttpPost(smsHost);
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            //            params.add(new BasicNameValuePair("sendto", phone));
            //            params.add(new BasicNameValuePair("content", text));
            params.add(new BasicNameValuePair("appname", "qiupu"));
            params.add(new BasicNameValuePair("data", String.format("{\"to\":\"%s\",\"subject\":\"%s\"}", phone, StringEscapeUtils.escapeJavaScript(text))));

            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            client.execute(httpPost);
        } catch (IOException e) {
            throw new ServerException(WutongErrors.SYSTEM_MESSAGE_GATEWAY_SEND_ERROR, "Send sms error");
        }
    }

    @Override
    public Record updateUserStatus(Context ctx, String userId, String newStatus, String device, String location, boolean post, boolean can_comment, boolean can_like, boolean can_reshare) {
        final String METHOD = "updateUserStatus";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, newStatus, device, location, post, can_comment, can_like, can_reshare);

        Validate.notNull(userId);

        StreamLogic streamLogic = GlobalLogics.getStream();

        Record user = new Record();
        user.put("status", newStatus);

        //update Status
        String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";

        L.op(ctx, "updateAccount");
        boolean b = updateAccount(ctx, userId, user, lang);
        Record out_rec = new Record();
        if (b) {
            if (post) {
                out_rec = streamLogic.postP(ctx, userId, Constants.TEXT_POST, newStatus, "[]", toStr(Constants.APP_TYPE_QIUPU), "", "", "", "", false, "", device, location, "", "", can_comment, can_like, can_reshare, "", Constants.POST_SOURCE_SYSTEM, 0L);
//                if (Long.parseLong(postid) > 0) {
//                    out_rec = streamLogic.getFullPostsForQiuPuP(ctx, userId, postid, true).getFirstRecord();
//                }
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return out_rec;
    }

    @Override
    public boolean updateAccount(Context ctx, String userId, Record user, String lang) {
        final String METHOD = "updateAccount";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, user, lang);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return updateAccount(ctx, userId, user, lang, true);
    }

    public boolean updateAccount(Context ctx, String userId, Record user, String lang, boolean sendNotif) {
        //        L.debug("update account begin at:"+DateUtils.nowMillis());

        AccountLogic account = GlobalLogics.getAccount();

        if (CollectionUtils2.containsOne(user.keySet(), "login_email1", "login_email2", "login_email3", "login_phone1", "login_phone2", "login_phone3")) {
            throw new ServerException(WutongErrors.USER_UPDATE_INFO_ERROR_CANT_ACTION_COLUMNS, "can't update this column.");
        }

        if (CollectionUtils2.containsOne(user.keySet(), "contact_info")) {
            Record rec = getUser(ctx, userId, userId, "user_id,contact_info,login_email1,login_email2,login_email3,login_phone1,login_phone2,login_phone3");
            if (!rec.isEmpty()) {
                Record con_r = Record.fromJson(rec.getString("contact_info"));
                Record con_in_u = Record.fromJson(user.getString("contact_info"));
                if (!con_r.getString("email_address").equals(con_in_u.getString("email_address"))) {
                    if (con_in_u.getString("email_address").equals("")) {
                        if (rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                            throw new ServerException(WutongErrors.USER_LASTED_BIND_ITEM, "can't delete this only column.");
                        } else {
                            user.put("login_email1", con_in_u.getString("email_address"));
                            con_in_u.removeColumns("email_address");
                        }
                    } else {
                        if (!con_r.getString("email_address").equals("")) {
                            if (rec.getString("login_email1").equals(con_r.getString("email_address")) || rec.getString("login_email2").equals(con_r.getString("email_address")) || rec.getString("login_email3").equals(con_r.getString("email_address"))
                                    || rec.getString("login_phone1").equals(con_r.getString("email_address")) || rec.getString("login_phone2").equals(con_r.getString("email_address")) || rec.getString("login_phone3").equals(con_r.getString("email_address"))) {
                                throw new ServerException(WutongErrors.USER_HAS_BIND_EMAIL, "can't update column:email_address,because has bind.");
                            }
                        }
                        if (rec.getString("login_email1").equals(con_in_u.getString("email_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_address"))
                                || rec.getString("login_phone1").equals(con_in_u.getString("email_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_address"))) {
                            throw new ServerException(WutongErrors.USER_HAS_BIND_EMAIL, "can't update column:email_address,because has bind.");
                        }
                    }
                }

                if (!con_r.getString("email_2_address").equals(con_in_u.getString("email_2_address"))) {
                    if (con_in_u.getString("email_2_address").equals("")) {
                        if (rec.getString("login_email1").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                            throw new ServerException(WutongErrors.USER_LASTED_BIND_ITEM, "can't delete this column.");
                        } else {
                            user.put("login_email2", con_in_u.getString("email_2_address"));
                            con_in_u.removeColumns("email_2_address");
                        }
                    } else {
                        if (!con_r.getString("email_2_address").equals("")) {
                            if (rec.getString("login_email1").equals(con_r.getString("email_2_address")) || rec.getString("login_email2").equals(con_r.getString("email_2_address")) || rec.getString("login_email3").equals(con_r.getString("email_2_address"))
                                    || rec.getString("login_phone1").equals(con_r.getString("email_2_address")) || rec.getString("login_phone2").equals(con_r.getString("email_2_address")) || rec.getString("login_phone3").equals(con_r.getString("email_2_address"))) {
                                throw new ServerException(WutongErrors.USER_HAS_BIND_EMAIL, "can't update column:email_2_address,because has bind.");
                            }
                        }
                        if (rec.getString("login_email1").equals(con_in_u.getString("email_2_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_2_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_2_address"))
                                || rec.getString("login_phone1").equals(con_in_u.getString("email_2_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_2_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_2_address"))) {
                            throw new ServerException(WutongErrors.USER_HAS_BIND_EMAIL, "can't update column:email_2_address,because has bind.");
                        }
                    }
                }

                if (!con_r.getString("email_3_address").equals(con_in_u.getString("email_3_address"))) {
                    if (con_in_u.getString("email_3_address").equals("")) {
                        if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                            throw new ServerException(WutongErrors.USER_LASTED_BIND_ITEM, "can't delete this column.");
                        } else {
                            user.put("login_email3", con_in_u.getString("email_3_address"));
                            con_in_u.removeColumns("email_3_address");
                        }
                    } else {
                        if (!con_r.getString("email_3_address").equals("")) {
                            if (rec.getString("login_email1").equals(con_r.getString("email_3_address")) || rec.getString("login_email2").equals(con_r.getString("email_3_address")) || rec.getString("login_email3").equals(con_r.getString("email_3_address"))
                                    || rec.getString("login_phone1").equals(con_r.getString("email_3_address")) || rec.getString("login_phone2").equals(con_r.getString("email_3_address")) || rec.getString("login_phone3").equals(con_r.getString("email_3_address"))) {
                                throw new ServerException(WutongErrors.USER_HAS_BIND_EMAIL, "can't update column:email_3_address,because has bind.");
                            }
                        }
                        if (rec.getString("login_email1").equals(con_in_u.getString("email_3_address")) || rec.getString("login_email2").equals(con_in_u.getString("email_3_address")) || rec.getString("login_email3").equals(con_in_u.getString("email_3_address"))
                                || rec.getString("login_phone1").equals(con_in_u.getString("email_3_address")) || rec.getString("login_phone2").equals(con_in_u.getString("email_3_address")) || rec.getString("login_phone3").equals(con_in_u.getString("email_3_address"))) {
                            throw new ServerException(WutongErrors.USER_HAS_BIND_EMAIL, "can't update column:email_3_address,because has bind.");
                        }
                    }
                }

                if (!con_r.getString("mobile_telephone_number").equals(con_in_u.getString("mobile_telephone_number"))) {
                    if (con_in_u.getString("mobile_telephone_number").equals("")) {
                        if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone2").equals("") && rec.getString("login_phone3").equals("")) {
                            throw new ServerException(WutongErrors.USER_LASTED_BIND_ITEM, "can't delete this column.");
                        } else {
                            user.put("login_phone1", con_in_u.getString("mobile_telephone_number"));
                            con_in_u.removeColumns("mobile_telephone_number");
                        }
                    } else {
                        if (!con_r.getString("mobile_telephone_number").equals("")) {
                            if (rec.getString("login_email1").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_telephone_number"))
                                    || rec.getString("login_phone1").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_telephone_number"))) {
                                throw new ServerException(WutongErrors.USER_HAS_BIND_PHONE, "can't update column:mobile_telephone_number,because has bind.");
                            }
                        }
                        if (rec.getString("login_email1").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_telephone_number"))
                                || rec.getString("login_phone1").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_telephone_number"))) {
                            throw new ServerException(WutongErrors.USER_HAS_BIND_PHONE, "can't update column:mobile_telephone_number,because has bind.");
                        }
                    }
                }

                if (!con_r.getString("mobile_2_telephone_number").equals(con_in_u.getString("mobile_2_telephone_number"))) {
                    if (con_in_u.getString("mobile_2_telephone_number").equals("")) {
                        if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone3").equals("")) {
                            throw new ServerException(WutongErrors.USER_LASTED_BIND_ITEM, "can't delete this column.");
                        } else {
                            user.put("login_phone2", con_in_u.getString("mobile_2_telephone_number"));
                            con_in_u.removeColumns("mobile_2_telephone_number");
                        }
                    } else {
                        if (!con_r.getString("mobile_2_telephone_number").equals("")) {
                            if (rec.getString("login_email1").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_2_telephone_number"))
                                    || rec.getString("login_phone1").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_2_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_2_telephone_number"))) {
                                throw new ServerException(WutongErrors.USER_HAS_BIND_PHONE, "can't update column:mobile_2_telephone_number,because has bind.");
                            }
                        }
                        if (rec.getString("login_email1").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_2_telephone_number"))
                                || rec.getString("login_phone1").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_2_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_2_telephone_number"))) {
                            throw new ServerException(WutongErrors.USER_HAS_BIND_PHONE, "can't update column:mobile_2_telephone_number,because has bind.");
                        }
                    }
                }

                if (!con_r.getString("mobile_3_telephone_number").equals(con_in_u.getString("mobile_3_telephone_number"))) {
                    if (con_in_u.getString("mobile_3_telephone_number").equals("")) {
                        if (rec.getString("login_email1").equals("") && rec.getString("login_email2").equals("") && rec.getString("login_email3").equals("") && rec.getString("login_phone1").equals("") && rec.getString("login_phone2").equals("")) {
                            throw new ServerException(WutongErrors.USER_LASTED_BIND_ITEM, "can't delete this column.");
                        } else {
                            user.put("login_phone3", con_in_u.getString("mobile_3_telephone_number"));
                            con_in_u.removeColumns("mobile_3_telephone_number");
                        }
                    } else {
                        if (!con_r.getString("mobile_3_telephone_number").equals("")) {
                            if (rec.getString("login_email1").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_email2").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_email3").equals(con_r.getString("mobile_3_telephone_number"))
                                    || rec.getString("login_phone1").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_phone2").equals(con_r.getString("mobile_3_telephone_number")) || rec.getString("login_phone3").equals(con_r.getString("mobile_3_telephone_number"))) {
                                throw new ServerException(WutongErrors.USER_HAS_BIND_PHONE, "can't update column:mobile_3_telephone_number,because has bind.");
                            }
                        }
                        if (rec.getString("login_email1").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_email2").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_email3").equals(con_in_u.getString("mobile_3_telephone_number"))
                                || rec.getString("login_phone1").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_phone2").equals(con_in_u.getString("mobile_3_telephone_number")) || rec.getString("login_phone3").equals(con_in_u.getString("mobile_3_telephone_number"))) {
                            throw new ServerException(WutongErrors.USER_HAS_BIND_PHONE, "can't update column:mobile_3_telephone_number,because has bind.");
                        }
                    }
                }

                if (con_in_u.getString("mobile_telephone_number").equals(""))
                    con_in_u.removeColumns("mobile_telephone_number");
                if (con_in_u.getString("mobile_2_telephone_number").equals(""))
                    con_in_u.removeColumns("mobile_2_telephone_number");
                if (con_in_u.getString("mobile_3_telephone_number").equals(""))
                    con_in_u.removeColumns("mobile_3_telephone_number");
                if (con_in_u.getString("mobile_telephone_number").equals(""))
                    con_in_u.removeColumns("mobile_telephone_number");
                if (con_in_u.getString("mobile_2_telephone_number").equals(""))
                    con_in_u.removeColumns("mobile_2_telephone_number");
                if (con_in_u.getString("mobile_3_telephone_number").equals(""))
                    con_in_u.removeColumns("mobile_3_telephone_number");

                user.put("contact_info", con_in_u.toString(false, false));
            }
        }
        //            String displayName = user.getString("display_name_temp",
        //            		getUser(userId, userId, "display_name").getString("display_name"));
        String displayName = getUser(ctx, userId, userId, "display_name").getString("display_name");
        user.removeColumns("phone", "email"/*, "display_name_temp"*/);

        boolean b = account.updateAccount(ctx, userId, user);
        //            L.debug("update account end at:"+DateUtils.nowMillis());
        if (!user.has("perhaps_name")) {
            Record user0 = user.copy();
            addImageUrlPrefix(GlobalConfig.get().getString("platform.profileImagePattern", ""), user0);

            WutongHooks wutongHooks = GlobalLogics.getHooks();
            wutongHooks.fireUserProfileChanged(ctx, user0.set("user_id", Long.parseLong(userId)));

            if (b && sendNotif) {
                Commons.sendNotification(ctx, Constants.NTF_PROFILE_UPDATE,
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(userId),
                        Commons.createArrayNodeFromStrings(user.toString(false, false), displayName, userId, lang),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(userId),
                        Commons.createArrayNodeFromStrings(lang),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(userId)
                );
            }
        }
        //            L.debug("notification end at:"+DateUtils.nowMillis() + ",so return.");
        return b;
    }

    public boolean updateAccountForNamSpliter(Context ctx, String userId, Record user) {
        AccountLogic account = GlobalLogics.getAccount();
        boolean b = account.updateAccount(ctx, userId, user);
        return b;
    }


    public void sendNodificationInternal(Context ctx, String userId, Record user, String lang, String displayName) {
        Commons.sendNotification(ctx, Constants.NTF_PROFILE_UPDATE,
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(userId),
                Commons.createArrayNodeFromStrings(user.toString(false, false), displayName, userId, lang),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(userId),
                Commons.createArrayNodeFromStrings(lang),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(userId)
        );
    }

    private static void addImageUrlPrefix(String profileImagePattern, Record rec) {
        if (rec.has("image_url")) {
            if (!rec.getString("image_url", "").startsWith("http:"))
                rec.put("image_url", String.format(profileImagePattern, rec.getString("image_url")));
        }

        if (rec.has("small_image_url")) {
            if (!rec.getString("small_image_url", "").startsWith("http:"))
                rec.put("small_image_url", String.format(profileImagePattern, rec.getString("small_image_url")));
        }

        if (rec.has("large_image_url")) {
            if (!rec.getString("large_image_url", "").startsWith("http:"))
                rec.put("large_image_url", String.format(profileImagePattern, rec.getString("large_image_url")));
        }
    }

    @Override
    public boolean setMiscellaneous(Context ctx, String userId, String phone, String lang) {
        final String METHOD = "setMiscellaneous";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, phone, lang);
        Validate.notNull(userId);
        checkUserIds(ctx, userId);
        Record user = new Record();
        RecordSet recs = getUsers(ctx, userId, userId, "miscellaneous");
        if (recs.size() > 0) {
            String ml = recs.getFirstRecord().getString("miscellaneous");
            if (ml.length() > 10) {
                Record rec = Record.fromJson(ml);
                if (phone.equals("0")) {
                    rec.removeColumns("openface.phone");
                } else {
                    rec.put("openface.phone", phone);
                }
                user.put("miscellaneous", rec.toString(false, false));
            } else {
                user.put("miscellaneous", Record.of("openface.phone", phone));
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        L.op(ctx, "setMiscellaneous");
        return updateAccount(ctx, userId, user, lang);
    }

    @Override
    public int findUidByMiscellaneousPlatform(Context ctx, String miscellaneous) {
        final String METHOD = "findUidByMiscellaneousPlatform";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, miscellaneous);
        AccountLogic a = GlobalLogics.getAccount();
        RecordSet rec = a.findUidByMiscellaneous(ctx, miscellaneous);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec.size() <= 0 ? 0 : (int) rec.getFirstRecord().getInt("user_id", 0);

    }

    @Override
    public RecordSet searchUser(Context ctx, String viewerId, String username, int page, int count) {
        final String METHOD = "searchUser";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, username, page, count);
        AccountLogic account = GlobalLogics.getAccount();
        FriendshipLogic fs = GlobalLogics.getFriendship();
        RecordSet r = account.searchUserByUserName(ctx, username, page, count);
        RecordSet u1 = getUsers(ctx, viewerId, r.joinColumnValues("user_id", ","), USER_STANDARD_COLUMNS);
        RecordSet f = fs.getVirtualFriendIdByName(ctx, viewerId, username);
        if (f.size() > 0) {
            RecordSet u2 = getUsers(ctx, f.joinColumnValues("virtual_friendid", ","), USER_STANDARD_COLUMNS);
            for (Record r0 : u2) {
                u1.add(r0);
            }
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return u1;

    }

    public boolean checkBindNameNotExists(Context ctx, String... names) {
        AccountLogic account = GlobalLogics.getAccount();
        String sNames = StringUtils.join(names, ",");
        return account.checkBindNameNotExists(ctx, sNames);

    }

    public String generalShortUrl(Context ctx, String long_url) {

        AccountLogic account = GlobalLogics.getAccount();
        String short_url = "";
        if (!long_url.toUpperCase().startsWith("HTTP://"))
            long_url = "http://" + long_url;
        if (long_url.substring(long_url.length() - 1).equals("//") || long_url.substring(long_url.length() - 1).equals("\\")) {
            long_url = long_url.substring(0, long_url.length() - 1);
        }

        if (long_url.substring(long_url.lastIndexOf("\\") + 1, long_url.length()).contains("?")) {
            long_url += "&generate_time=" + DateUtils.nowMillis();
        } else {
            long_url += "?generate_time=" + DateUtils.nowMillis();
        }
        URL ur = null;
        try {
            ur = new URL(long_url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        String host = ur.getHost();    //api.borqs.com
        String lastUrlStr = StringUtils.replace(long_url, "http://" + host + "/", "");
        String formatUrl = ShortText(lastUrlStr)[0];
        short_url = "http://" + host + "/" + "z" + "/" + formatUrl;
        account.saveShortUrl(ctx, long_url, short_url);
        return short_url;

    }

    @Override
    public boolean bindUserSendVerify(Context ctx, String userId, String phone, String email, String key, String ticket, String lang) {
        final String METHOD = "bindUserSendVerify";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, phone, email, key, ticket, lang);
        Validate.notNull(userId);
        checkUserIds(ctx, userId);


        AccountLogic a = GlobalLogics.getAccount();
        EmailLogic emailLogic = GlobalLogics.getEmail();
        FriendshipLogic friendshipLogic = GlobalLogics.getFriendship();

        boolean b = checkBindNameNotExists(ctx, phone, email);
        if (!b) {
            ServerException e = new ServerException(WutongErrors.USER_PHONE_EMAIL_BIND_BY_OTHERS, "phone or email has bind by others!");
            L.warn(ctx, e);
            throw e;
        }
        Record r = getUsers(ctx, userId, userId, "user_id, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, display_name,contact_info").getFirstRecord();

        if (!phone.equals("")) {
            if (r.getString("login_email1").equals(phone) || r.getString("login_email2").equals(phone) || r.getString("login_email3").equals(phone)
                    || r.getString("login_phone1").equals(phone) || r.getString("login_phone2").equals(phone) || r.getString("login_phone3").equals(phone)) {
                ServerException e = new ServerException(WutongErrors.USER_PHONE_EMAIL_BIND_BY_OTHERS, "has binded");
                L.warn(ctx, e);
                throw e;
            }
        }

        if (!email.equals("")) {
            if (r.getString("login_email1").equals(email) || r.getString("login_email2").equals(email) || r.getString("login_email3").equals(email)
                    || r.getString("login_phone1").equals(email) || r.getString("login_phone2").equals(email) || r.getString("login_phone3").equals(email)) {
                ServerException e = new ServerException(WutongErrors.USER_PHONE_EMAIL_BIND_BY_OTHERS, "has binded");
                L.warn(ctx, e);
                throw e;
            }
        }
        String host = "http://" + SERVER_HOST + "/";
        if (key.equals("")) {
            if (phone.equals("") && !email.equals("")) //verify email
            {
                //                  update contact_info first
                String con_column = "";
                if (r.getString("contact_info").length() > 10) {
                    Record c_r = Record.fromJson(r.getString("contact_info"));

                    //update contact_info for this email
                    if (!c_r.getString("email_address").equals("") && !c_r.getString("email_2_address").equals("") && !c_r.getString("email_3_address").equals("")) {
                        if (c_r.getString("email_address").equals(email) || c_r.getString("email_2_address").equals(email) || c_r.getString("email_3_address").equals(email)) {
                        } else {
                            ServerException e = new ServerException(WutongErrors.USER_PHONE_EMAIL_BIND_LIMIT_COUNT, "has 3 email already!");
                            L.warn(ctx, e);
                            throw e;
                        }
                    }
                    if (c_r.getString("email_address").equals("") && !c_r.getString("email_2_address").equals(email) && !c_r.getString("email_3_address").equals(email)) {
                        con_column = "email_address";
                    }
                    if (!c_r.getString("email_address").equals("") && c_r.getString("email_2_address").equals("") && !c_r.getString("email_address").equals(email) && !c_r.getString("email_3_address").equals(email)) {
                        con_column = "email_2_address";
                    }
                    if (!c_r.getString("email_address").equals("") && !c_r.getString("email_2_address").equals("") && c_r.getString("email_3_address").equals("") && !c_r.getString("email_address").equals(email) && !c_r.getString("email_2_address").equals(email)) {
                        con_column = "email_3_address";
                    }
                    if (!con_column.equals("")) {
                        c_r.put(con_column, email);
                        Record user = new Record();
                        user.put("contact_info", c_r.toString(false, false));
                        a.bindUser(ctx, userId, user);
                        fireChangeProfileHooksForBind(ctx, userId, phone, email);
                    }
                }
                //send email
                String title = r.getString("display_name") + "，欢迎您绑定邮箱到播思通行证";
                String to = r.getString("display_name");
                String content = "尊敬的 " + to + " :<br>";
                content += "　　您的播思通行证ID是：" + r.getString("user_id") + "，您已输入 " + email + " 绑定到您的播思通行证。要完成该流程，只需验证该电子邮件地址是否属于您即可。"
                        + "请点击下方链接，如果无法点击，请复制链接到地址栏转入。<br>";

                String url = host + "account/bind?ticket=" + ticket + "&";
                String param = "userId=" + userId + "&phone=" + phone + "&email=" + email + "&key=";

                String gkey = userId + "/" + phone + "/" + email;
                FeedbackParams fp = new FeedbackParams().set("param", gkey);
                String b2 = fp.toBase64(true);
                url = url + param + b2;

                String link = "<a href=" + url + " target=_blank>" + url + "</a>";

                content += link;
                emailLogic.sendEmail(ctx, title, email, to, content, Constants.EMAIL_ESSENTIAL, lang);
            }
            if (!phone.equals("") && email.equals("")) //verify phone
            {
                //======================================================= begin====================================
                String upcolumn = "";
                String con_column = "";
                if (!r.getString("login_phone1").equals("") && !r.getString("login_phone2").equals("") && !r.getString("login_phone3").equals("")) {
                    ServerException e = new ServerException(WutongErrors.USER_PHONE_EMAIL_BIND_LIMIT_COUNT, "has 3 phones binded already");
                    L.warn(ctx, e);
                    throw e;
                }
                if (r.getString("contact_info").length() > 10) {
                    Record c_r = Record.fromJson(r.getString("contact_info"));
                    if (c_r.getString("mobile_telephone_number").equals(phone)) {
                        upcolumn = "login_phone1";
                        con_column = "mobile_telephone_number";
                    } else if (c_r.getString("mobile_2_telephone_number").equals(phone)) {
                        upcolumn = "login_phone2";
                        con_column = "mobile_2_telephone_number";
                    } else if (c_r.getString("mobile_3_telephone_number").equals(phone)) {
                        upcolumn = "login_phone3";
                        con_column = "mobile_3_telephone_number";
                    } else {
                        if (r.getString("login_phone1").equals("")) {
                            upcolumn = "login_phone1";
                            con_column = "mobile_telephone_number";
                        }
                        if (!r.getString("login_phone1").equals("") && r.getString("login_phone2").equals("")) {
                            upcolumn = "login_phone2";
                            con_column = "mobile_2_telephone_number";
                        }
                        if (!r.getString("login_phone1").equals("") && !r.getString("login_phone2").equals("") && r.getString("login_phone3").equals("")) {
                            upcolumn = "login_phone3";
                            con_column = "mobile_3_telephone_number";
                        }
                        //throw Errors.createResponseError(WutongErrors.PARAM_ERROR, "must in contact_info first");
                    }
                    c_r.put(con_column, phone);
                    Record user = new Record();
                    //                        user.put(upcolumn, phone);
                    user.put("contact_info", c_r.toString(false, false));
                    a.bindUser(ctx, userId, user);
                    fireChangeProfileHooksForBind(ctx, userId, phone, email);
                }
                //======================================================= end====================================

                //send message
                String content = "请点击下面的链接，将您的手机号码绑定到播思账号：";

                String param = "account/bind?ticket=" + ticket + "&phone=" + phone + "&email=" + email + "&key=";

                String gkey = userId + "/" + phone + "/" + email;
                FeedbackParams fp = new FeedbackParams().set("param", gkey);
                String b2 = fp.toBase64(true);
                // sendmessage(from,to,content);
                String url = host + param + b2;

                String short_url = generalShortUrl(ctx, url);


                Commons.sendSms(ctx, phone, content + short_url + "\\");
            }
        } else {
            key = key.replaceAll(" ", "+");
            String fp = FeedbackParams.fromBase64(key).get("param");
            String[] ss = StringUtils2.splitArray(fp, "/", 3, false);//fp.split("/");
            //                if (ss.length < 3) {
            //                    throw Errors.createResponseError(WutongErrors.BIND_KEY_ERROR, "key error");
            //                }

            String keyUserId = ss[0];
            String keyPhone = ss[1];
            String keyEmail = ss[2];

            String upcolumn = "";
            String con_column = "";
            if (keyPhone.equals("") && !email.equals("")) {//bind email
                if (!r.getString("login_email1").equals("") && !r.getString("login_email2").equals("") && !r.getString("login_email3").equals("")) {
                    ServerException e = new ServerException(WutongErrors.USER_PHONE_EMAIL_BIND_LIMIT_COUNT, "has 3 emails binded already");
                    L.warn(ctx, e);
                    throw e;
                }
                if (r.getString("contact_info").length() > 10) {
                    Record c_r = Record.fromJson(r.getString("contact_info"));
                    if (c_r.getString("email_address").equals(keyEmail)) {
                        upcolumn = "login_email1";
                        con_column = "email_address";
                    } else if (c_r.getString("email_2_address").equals(keyEmail)) {
                        upcolumn = "login_email2";
                        con_column = "email_2_address";
                    } else if (c_r.getString("email_3_address").equals(keyEmail)) {
                        upcolumn = "login_email3";
                        con_column = "email_3_address";
                    } else {
                        if (r.getString("login_email1").equals("")) {
                            upcolumn = "login_email1";
                            con_column = "email_address";
                        }
                        if (!r.getString("login_email1").equals("") && r.getString("login_email2").equals("")) {
                            upcolumn = "login_email2";
                            con_column = "email_2_address";
                        }
                        if (!r.getString("login_email1").equals("") && !r.getString("login_email2").equals("") && r.getString("login_email3").equals("")) {
                            upcolumn = "login_email3";
                            con_column = "email_3_address";
                        }
                        //throw Errors.createResponseError(WutongErrors.PARAM_ERROR, "must in contact_info first");
                    }
                    c_r.put(con_column, keyEmail);
                    Record user = new Record();
                    user.put(upcolumn, keyEmail);
                    user.put("contact_info", c_r.toString(false, false));
                    a.bindUser(ctx, keyUserId, user);


                    friendshipLogic.updateVirtualFriendIdToAct(ctx, userId, keyEmail);

                    fireChangeProfileHooksForBind(ctx, userId, phone, email);
                }
            }
            if (!keyPhone.equals("") && email.equals("")) {//bind phone
                if (!r.getString("login_phone1").equals("") && !r.getString("login_phone2").equals("") && !r.getString("login_phone3").equals("")) {
                    ServerException e = new ServerException(WutongErrors.USER_PHONE_EMAIL_BIND_LIMIT_COUNT, "has 3 phones binded already");
                    L.warn(ctx, e);
                    throw e;
                }
                if (r.getString("contact_info").length() > 10) {
                    Record c_r = Record.fromJson(r.getString("contact_info"));
                    if (c_r.getString("mobile_telephone_number").equals(keyPhone)) {
                        upcolumn = "login_phone1";
                        con_column = "mobile_telephone_number";
                    } else if (c_r.getString("mobile_2_telephone_number").equals(keyPhone)) {
                        upcolumn = "login_phone2";
                        con_column = "mobile_2_telephone_number";
                    } else if (c_r.getString("mobile_3_telephone_number").equals(keyPhone)) {
                        upcolumn = "login_phone3";
                        con_column = "mobile_3_telephone_number";
                    } else {
                        if (r.getString("login_phone1").equals("")) {
                            upcolumn = "login_phone1";
                            con_column = "mobile_telephone_number";
                        }
                        if (!r.getString("login_phone1").equals("") && r.getString("login_phone2").equals("")) {
                            upcolumn = "login_phone2";
                            con_column = "mobile_2_telephone_number";
                        }
                        if (!r.getString("login_phone1").equals("") && !r.getString("login_phone2").equals("") && r.getString("login_phone3").equals("")) {
                            upcolumn = "login_phone3";
                            con_column = "mobile_3_telephone_number";
                        }
                        //throw Errors.createResponseError(WutongErrors.PARAM_ERROR, "must in contact_info first");
                    }
                    c_r.put(con_column, keyPhone);
                    Record user = new Record();
                    user.put(upcolumn, keyPhone);
                    user.put("contact_info", c_r.toString(false, false));
                    a.bindUser(ctx, keyUserId, user);

                    friendshipLogic.updateVirtualFriendIdToAct(ctx, userId, keyPhone);
                    fireChangeProfileHooksForBind(ctx, userId, phone, email);
                }
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;
    }

    private void fireChangeProfileHooksForBind(Context ctx, String userId, String phone, String email) {
        ObjectNode on = JsonNodeFactory.instance.objectNode();
        if (StringUtils.isNotEmpty(phone))
            on.put("mobile_telephone_number", phone);
        if (StringUtils.isNotEmpty(email))
            on.put("email_address", email);
        if (on.size() > 0) {
            Record userRec = Record.of("user_id", userId, "contact_info", on);

            WutongHooks wutongHooks = GlobalLogics.getHooks();
            wutongHooks.fireUserProfileChanged(ctx, userRec);
        }
    }

    @Override
    public Record getViewerPrivacyConfig(Context ctx, String viewerId, String resources) {
        final String METHOD = "getViewerPrivacyConfig";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, resources);
        AccountLogic account = GlobalLogics.getAccount();
        RecordSet privacyItemList = account.getAuths(ctx, viewerId, resources);

        Record privacyConfig = new Record();
        for (Record privacyItem : privacyItemList) {
            String resource = privacyItem.getString("resource");
            String auths = privacyItem.getString("auths");
            privacyConfig.put(resource, auths);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return privacyConfig;

    }

    @Override
    public String getPerhapsNameP(Context ctx, String url) throws IOException {
        final String METHOD = "getPerhapsNameP";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, url);
//        String url = "http://api.borqs.com/dm/contacts/namecount/byborqsid/10015.json?limit=2";
        URL ur = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) ur.openConnection();
        conn.setConnectTimeout(10 * 1000);

        String s = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(ur.openStream(), "utf-8"));
        StringBuffer sb = new StringBuffer();
        while ((s = in.readLine()) != null) {
            sb.append(s);
        }
        in.close();
        conn.disconnect();
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return sb.toString().trim();
    }
    @Override
    public List<String> getEmailFromUsers(Context ctx, long userId) {
        return this.getEmailFromUsers(ctx,String.valueOf(userId));
    }
    @Override
    public List<String> getEmailFromUsers(Context ctx, String userIds) {
        List<String> emailList = new ArrayList<String>();
        if (userIds == null || userIds.length() < 1)
            return emailList;

        RecordSet users = this.getUsers(ctx, ctx.getViewerIdString(), userIds,
                "user_id, display_name, login_email1, login_email2, login_email3", false);
        for (Record user : users) {

            String emails1 = user.getString("login_email1", "");
            String emails2 = user.getString("login_email2", "");
            String emails3 = user.getString("login_email3", "");

            if (StringUtils.isNotBlank(emails1))
                emailList.add(emails1);
            if (StringUtils.isNotBlank(emails2))
                emailList.add(emails2);
            if (StringUtils.isNotBlank(emails3))
                emailList.add(emails3);
        }

        return emailList;
    }
    @Override
    public String formatUrlP(Context ctx, String user_id) throws IOException {
        final String METHOD = "formatUrlP";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, user_id);
        Record user = getUser(ctx, user_id, user_id, "user_id,login_email1,login_email2,login_email3,login_phone1,login_phone2,login_phone3", false);
        String finallyString = "";
        int flag = 0;
        if (!user.getString("login_phone1").equals("")) {
            finallyString = user.getString("login_phone1");
        } else {
            if (!user.getString("login_phone2").equals("")) {
                finallyString = user.getString("login_phone2");
            } else {
                if (!user.getString("login_phone3").equals("")) {
                    finallyString = user.getString("login_phone3");
                }
            }
        }
        if (!finallyString.equals(""))
            flag = 1;

        if (flag == 0) {
            if (!user.getString("login_email1").equals("")) {
                finallyString = user.getString("login_email1");
            } else {
                if (!user.getString("login_email2").equals("")) {
                    finallyString = user.getString("login_email2");
                } else {
                    if (!user.getString("login_email3").equals("")) {
                        finallyString = user.getString("login_email3");
                    }
                }
            }
            if (!finallyString.equals(""))
                flag = 2;
        }

        if (flag == 0) {
            finallyString = user_id;
            flag = 3;
        }


        String url_header = "http://api.borqs.com/dm/contacts/namecount/";
        String url_middle = "";
        if (flag == 1)
            url_middle = "bymobile/" + finallyString;
        if (flag == 2)
            url_middle = "byemail/" + finallyString;
        if (flag == 3)
            url_middle = "byborqsid/" + finallyString;
        String url_footer = ".json?limit=2";
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return url_header + url_middle + url_footer;
    }

    @Override
    public boolean changePassword(Context ctx, String userId, String oldPassword, String newPassword) {

        AccountLogic account = GlobalLogics.getAccount();
        //1,check oldPassword
        Record r = account.getUsersPasswordByUserIds(ctx, userId).getFirstRecord();
        if (r.isEmpty())
            throw new ServerException(WutongErrors.AUTH_TICKET_INVALID, "Invalid userId!");
        if (!r.getString("password").equalsIgnoreCase(oldPassword))
            throw new ServerException(WutongErrors.USER_NAME_PASSWORD_ERROR, "Invalid old password!");
        //update newPassword

        return account.changePasswordByUserId(ctx, userId, newPassword);
    }
}

class BuiltinUserExtender extends PlatformExtender {
    private static final Set<String> NECESSARY_COLUMNS = CollectionUtils2.asSet(
            "user_id");

    private static final Set<String> EXTEND_COLUMNS = CollectionUtils2.asSet(
            "remark", "in_circles", "his_friend", "bidi", "friends_count", "followers_count", "favorites_count");


    @Override
    public Set<String> necessaryColumns() {
        return NECESSARY_COLUMNS;
    }

    @Override
    public Set<String> extendedColumns() {
        return EXTEND_COLUMNS;
    }

    @Override
    public void extend(Context ctx, RecordSet recs, Set<String> cols) {
        if (cols.contains("remark"))
            extendRemark(ctx, recs);

        extendCircles(ctx, recs, cols);
    }

    private void extendRemark(Context ctx, RecordSet recs) {
        if (Constants.isNullUserId(ctx.getViewerIdString())) {
            for (Record rec : recs)
                rec.put("remark", "");
            return;
        }

        String userIds = recs.joinColumnValues("user_id", ",");
        FriendshipLogic friend = GlobalLogics.getFriendship();
        RecordSet remarks = friend.getRemarks(ctx, ctx.getViewerIdString(), userIds);
        recs.mergeByKeys("user_id", remarks, "friend", Record.of("remark", ""));
    }

    private static boolean isFriend(JsonNode rel) {
        if (rel == null || rel.size() == 0)
            return false;

        for (int i = 0; i < rel.size(); i++) {
            JsonNode cn = rel.get(i);
            if (cn != null) {
                if (cn.path("circle_id").getValueAsInt() == Constants.BLOCKED_CIRCLE)
                    return false;
            }
        }
        return true;
    }

    private void extendCircles(Context ctx, RecordSet recs, Set<String> cols) {
        if (Constants.isNullUserId(ctx.getViewerIdString())) {
            JsonNodeFactory jnf = JsonNodeFactory.instance;
            for (Record rec : recs) {
                if (cols.contains("in_circles"))
                    rec.put("in_circles", jnf.arrayNode());
                if (cols.contains("his_friend"))
                    rec.put("his_friend", false);
                if (cols.contains("bidi"))
                    rec.put("bidi", false);
            }
            return;
        }

        if (cols.contains("bidi") || cols.contains("in_circles") || cols.contains("his_friend")) {
            if (recs.joinColumnValues("user_id", ",").length() > 0) {
                FriendshipLogic friend = GlobalLogics.getFriendship();
                RecordSet recs_mine = friend.getAllRelation(ctx, ctx.getViewerIdString(), recs.joinColumnValues("user_id", ","), Integer.toString(Constants.FRIENDS_CIRCLE), "mine");
                RecordSet recs_their = friend.getAllRelation(ctx, ctx.getViewerIdString(), recs.joinColumnValues("user_id", ","), Integer.toString(Constants.FRIENDS_CIRCLE), "their");
                if (cols.contains("bidi")) {
                    for (Record rec : recs) {
                        int i_mine = 0;
                        int i_their = 0;
                        for (Record ru : recs_mine) {
                            if (ru.getString("friend").equals(rec.getString("user_id"))) {
                                i_mine += 1;
                            }
                        }
                        for (Record ru : recs_their) {
                            if (ru.getString("user").equals(rec.getString("user_id"))) {
                                i_their += 1;
                            }
                        }
                        boolean b = false;
                        if (i_mine > 0 && i_their > 0)
                            b = true;

                        rec.put("bidi", b);
                    }
                }

                if (cols.contains("in_circles")) {
                    for (Record rec : recs) {
                        RecordSet temp0 = new RecordSet();
                        for (Record ru : recs_mine) {
                            if (ru.getString("friend").equals(rec.getString("user_id"))) {
                                temp0.add(Record.of("circle_id", ru.getString("circle"), "circle_name", ru.getString("name")));
                            }
                        }
                        rec.put("in_circles", temp0.toJsonNode());
                    }
                }

                if (cols.contains("his_friend")) {

                    for (Record rec : recs) {
                        RecordSet temp0 = new RecordSet();
                        int i = 0;
                        boolean b = false;
                        for (Record ru : recs_their) {
                            if (ru.getString("user").equals(rec.getString("user_id"))) {
                                i += 1;
                            }
                        }
                        if (i > 0)
                            b = true;
                        rec.put("his_friend", b);
                    }
                }
            }
        }
    }

}



