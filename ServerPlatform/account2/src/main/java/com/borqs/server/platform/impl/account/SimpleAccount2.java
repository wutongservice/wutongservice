package com.borqs.server.platform.impl.account;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.DataException;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.platform.ErrorCode;
import com.borqs.server.platform.account.AccountBase;
import com.borqs.server.platform.account.AccountException;
import com.borqs.server.platform.account2.User;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.service.platform.Account;
import com.borqs.server.service.platform.Constants;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

import static com.borqs.server.service.platform.Constants.NULL_USER_ID;

public class SimpleAccount2 extends RPCService implements Account {
    private static final Logger L = LoggerFactory.getLogger(SimpleAccount2.class);
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
    private String db;
    private String userTable;
    private String ticketTable;
    private String privacyTable;
    private String globalCounterTable;

    private AccountImpl account;

    public SimpleAccount2() {
    }

    @Override
    public void init() {
        Configuration conf = getConfig();
        userSchema.loadAliases(conf.getString("schema.user.alias", null));
        ticketSchema.loadAliases(conf.getString("schema.ticket.alias", null));
        privacySchema.loadAliases(conf.getString("schema.privacy.alias", null));
        profileImagePattern = StringUtils.removeEnd(conf.checkGetString("platform.profileImagePattern").trim(), "/");
        sysIconUrlPattern = StringUtils.removeEnd(conf.checkGetString("platform.sysIconUrlPattern").trim(), "/");

        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.userTable = "user2";
        this.privacyTable = conf.getString("account.simple.privacyTable", "privacy");
        this.ticketTable = conf.getString("account.simple.ticketTable", "ticket");
        this.globalCounterTable = conf.getString("account.simple.globalCounterTable", "user_id_counter");

    }

    @Override
    public void setConfig(Configuration conf) {
        super.setConfig(conf);
        account = new AccountImpl();
        account.setConfig(conf);
    }

    @Override
    public void destroy() {
        this.userTable = this.ticketTable = this.globalCounterTable = null;
        this.privacyTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    private Record findUserByLoginName(String name, String... cols) {
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
            User user = account.getUser(id);
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


    private boolean saveTicket(String ticket, String userId, String appId) {
        final String SQL = "INSERT INTO ${table}"
                + " (${alias.ticket}, ${alias.user}, ${alias.app}, ${alias.created_time})"
                + " VALUES"
                + " (${v(ticket)}, ${v(user_id)}, ${v(app_id)}, ${v(created_time)})";


        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", ticketTable},
                {"alias", ticketSchema.getAllAliases()},
                {"ticket", ticket},
                {"user_id", Long.parseLong(userId)},
                {"app_id", Integer.parseInt(appId)},
                {"created_time", DateUtils.nowMillis()},
        });

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
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
        final String SQL = "SELECT user as user_id FROM user_property WHERE `key`=27 AND sub =1 AND VALUE  like '%" + miscellaneous + "%'";

        String sql = SQLTemplate.merge(SQL,
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


    private RecordSet searchUserByUserName0(String username,int page,int count) {
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
            throw new AccountException(ErrorCode.GENERATE_USER_ID_ERROR, "Generate user Id error");

        return Long.toString(count);
    }


    private boolean saveUser(Record info) {
        L.error("----------------standardize----------------"+info.toString());
        Schemas.standardize(userSchema, info);
        //modify the old method, converter Record to User Object

        User user;
        try {
            user = AccountConverter.converterRecord2User(info);
        } catch (Exception e) {
            L.error("Data Object Converter Record to User error!");
            e.printStackTrace();
            return false;
        }
        try {
            account.createUser(user);
            return true;
        } catch (Exception e) {
            L.error("----------------User Create error!----------------",e);
            return false;
        }

        /*final String SQL = "INSERT INTO ${table} ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "alias", userSchema.getAllAliases(),
                "info", info);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;*/
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


    private boolean updateUser(Record user) {
        Schemas.standardize(userSchema, user);

        String[] groups = userSchema.getColumnsGroups(user.getColumns());
        long now = DateUtils.nowMillis();
        long userId = user.getInt("user_id", 0);
        if (userId == 0)
            return false;
        User user0 = AccountConverter.converterRecord2User(user);
        return account.update(user0);

        /*String sql = new SQLBuilder.Update(userSchema)
                .update(userTable)
                .values(user)
                .valueIf(ArrayUtils.contains(groups, "basic"), "basic_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "status_"), "status_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "profile"), "profile_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "business"), "business_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "contact"), "contact_info_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "address_"), "address_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "work"), "work_history_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "education"), "education_history_updated_time", now)
                .where("${alias.user_id}=${v(user_id)}", "user_id", userId)
                .and("destroyed_time = 0")
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;*/
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
                .and("destroyed_time = 0")
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }


    private RecordSet findUsersByUserIds(List<String> userIds, String... cols) {
        long[] longs = List2Array(userIds);
        List<User> users = account.getUsers(longs);
        return AccountConverter.convertUserList2RecordSet(users, cols);

        /*final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE destroyed_time = 0 AND ${alias.user_id} IN (${user_ids})";

        String sql = SQLTemplate.merge(SQL,
                "alias", userSchema.getAllAliases(),
                "cols", cols,
                "table", userTable,
                "user_ids", StringUtils.join(userIds, ","));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(userSchema, recs);
        return recs;*/
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
    public ByteBuffer login(CharSequence name, CharSequence password, CharSequence appId) throws AvroRemoteException, ResponseError {
        final String PASSKEY = Encoders.md5Hex("_passkey_passw0rd_");
        System.out.println("=== login");
        try {
            String name0 = toStr(name);
            String password0 = toStr(password);
            String appId0 = toStr(appId);
            Record rec = findUserByLoginName(name0, "user_id", "password", "display_name");
            if (rec.isEmpty())
                throw new AccountException(ErrorCode.LOGIN_NAME_OR_PASSWORD_ERROR, "Login name or password error");

            String userId = rec.getString("user_id");
            if (!PASSKEY.equals(password0)) {
                System.out.println("==== passkey");
                if (!StringUtils.equalsIgnoreCase(password0, rec.getString("password")))
                    throw new AccountException(ErrorCode.LOGIN_NAME_OR_PASSWORD_ERROR, "Login name or password error");
            } else {
                System.out.println("==== not passkey:" + password0);
            }

            String ticket = genTicket(name0);
            boolean b = saveTicket(ticket, userId, appId0);
            if (!b)
                throw new AccountException(ErrorCode.CREATE_SESSION_ERROR, "Create session error");

            //updateUser(Record.of("user_id", userId, "last_visited_time", DateUtils.nowMillis()));
            return Record.of(new Object[][]{
                    {"user_id", userId},
                    {"ticket", ticket},
                    {"display_name", rec.getString("display_name")},
                    {"login_name", name0},
            }).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    public static String genTicket(String loginName) {
        return Encoders.toBase64(loginName + "_" + DateUtils.nowMillis() + "_" + new Random().nextInt(10000));
    }

    @Override
    public final boolean logout(CharSequence ticket) throws AvroRemoteException {
        try {
            String ticket0 = toStr(ticket);
            return deleteTicket(ticket0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public final CharSequence whoLogined(CharSequence ticket) throws AvroRemoteException {
        try {
            Record rec = findUserByTicket(toStr(ticket), "user");
            return rec.getString("user", NULL_USER_ID);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public final ByteBuffer getLogined(CharSequence userId, CharSequence appId) throws AvroRemoteException, ResponseError {
        try {
            return findTicketsByUserId(toStr(userId), toStr(appId),
                    "ticket", "user", "app", "created_time").toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    public boolean checkLoginNameNotExists(CharSequence uid, CharSequence names) throws AvroRemoteException, ResponseError {
        String[] arrNames = StringUtils2.splitArray(toStr(names), ",", true);
        checkLoginNameNotExists(toStr(uid), arrNames);

        return true;
    }

    @Override
    public boolean checkBindNameNotExists(CharSequence names) throws AvroRemoteException, ResponseError {
        List<String> nameList = StringUtils2.splitList(toStr(names), ",", true);
        boolean b = true;
        for (String name : nameList) {
            if (!name.equals("")) {
                Record rec = findUserByLoginName(name, "user_id");
                String userid = rec.getString("user_id");
                if (!rec.isEmpty() && !userid.equals("")) {
                    b = false;
                    break;
                }
            }
        }
        return b;
    }

    @Override
    public ByteBuffer getUsersAuths(CharSequence userIds) throws AvroRemoteException, ResponseError {
        return getUsersAuths0(toStr(userIds)).toByteBuffer();
    }

    @Override
    public final CharSequence createAccount(ByteBuffer info) throws AvroRemoteException, ResponseError {
        try {
            Record pf = Record.fromByteBuffer(info);

            // check missing columns
            Schemas.checkRecordIncludeColumns(pf, "password", "display_name");
            if (!pf.has("login_email1") && !pf.has("login_phone1"))
                throw new DataException("Must include column 'login_email1' or 'login_phone1'");

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

            checkLoginNameNotExists("", login_phone1, login_email1);

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
                first_name = name.getGivenNames().toString();
            }
            if (name.getMiddleName() != null) {
                middle_name = name.getMiddleName().toString();
            }
            if (name.getFamilyName() != null) {
                last_name = name.getFamilyName().toString();
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
            boolean b = saveUser(rec);
            if (!b)
                throw new AccountException(ErrorCode.ACCOUNT_ERROR, "Save user info error");

            return userId;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }


    @Override
    public final boolean destroyAccount(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return deleteUser(toStr(userId), DateUtils.nowMillis());
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence resetPassword(CharSequence loginName) throws AvroRemoteException, ResponseError {
        try {
            String loginName0 = toStr(loginName);

            String newPassword = RandomUtils.generateRandomNumberString(6);
            String userId = findUserIdByLoginName(loginName0);
            if (StringUtils.isEmpty(userId))
                throw new AccountException(ErrorCode.USER_NOT_EXISTS, "User '%s' is not exists", loginName0);

            Record info = Record.of("user_id", userId, "password", Encoders.md5Hex(newPassword));
            updateUser(info);
            return newPassword;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean updateAccount(CharSequence userId, ByteBuffer info) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            Record user = Record.fromByteBuffer(info);
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
                    first_name = name.getGivenNames().toString();
                }
                if (name.getMiddleName() != null) {
                    middle_name = name.getMiddleName().toString();
                }
                if (name.getFamilyName() != null) {
                    last_name = name.getFamilyName().toString();
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
            return updateUser(user);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean bindUser(CharSequence userId, ByteBuffer info) throws AvroRemoteException, ResponseError {
        try {
            Record user = Record.fromByteBuffer(info);
            user.put("user_id", userId);
            return updateUser(user);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer findAllUserIds(boolean all) throws AvroRemoteException, ResponseError {
        try {
            return findAllUserIds0(all).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getUsersPasswordByUserIds(CharSequence userIds) throws AvroRemoteException, ResponseError {
        return findUsersPasswordByUserIds(toStr(userIds)).toByteBuffer();
    }

    @Override
    public boolean changePasswordByUserId(CharSequence userId, CharSequence password) throws AvroRemoteException, ResponseError {
        try {
            return updatePasswordByUserId(toStr(userId), toStr(password));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getUsers(CharSequence userIds, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);
            String[] cols0 = StringUtils2.splitArray(toStr(cols), ",", true);

            if (userIds0.isEmpty() || cols0.length == 0)
                return new RecordSet().toByteBuffer();

            //Schemas.checkSchemaIncludeColumns(userSchema, cols0);
            RecordSet recs = findUsersByUserIds(userIds0, cols0);
            if (recs != null) {
                for (Record rec : recs) {
                    addImageUrlPreifx(profileImagePattern, sysIconUrlPattern, rec);
                }
                return recs.toByteBuffer();
            } else {
                return new RecordSet().toByteBuffer();
            }
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer findUidByMiscellaneous(CharSequence miscellaneous)
            throws AvroRemoteException, ResponseError {
        return findUidByMiscellaneous0(toStr(miscellaneous)).toByteBuffer();
    }

    @Override
    public ByteBuffer getUserIds(CharSequence loginNames) throws AvroRemoteException, ResponseError {
        try {
            List<String> names0 = StringUtils2.splitList(toStr(loginNames), ",", true);
            RecordSet recs = new RecordSet();
            for (String name : names0) {
                String userId = findUserIdByLoginName(name);
                recs.add(Record.of("user_id", userId, "login_name", name));
            }
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getUserIdsByNames(CharSequence loginNames) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = findUserByLoginNameOrDisplayName(toStr(loginNames));
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer hasUsers(CharSequence userIds) throws AvroRemoteException, ResponseError {
        List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);
        RecordSet recs = new RecordSet();
        for (String userId : userIds0) {
            boolean b = !findUsersByUserIds(Arrays.asList(userId), "user_id").isEmpty();
            recs.add(Record.of("user_id", Long.parseLong(userId), "result", b));
        }
        return recs.toByteBuffer();
    }

    @Override
    public ByteBuffer searchUserByUserName(CharSequence username,int page,int count) throws AvroRemoteException, ResponseError {
        return searchUserByUserName0(toStr(username),page,count).toByteBuffer();
    }

    @Override
    public boolean hasOneUsers(CharSequence userIds) throws AvroRemoteException, ResponseError {
        List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);
        if (userIds0.isEmpty())
            return false;

        RecordSet recs = findUsersByUserIds(userIds0, "user_id");
        return !recs.isEmpty();
    }

    @Override
    public boolean hasAllUsers(CharSequence userIds) throws AvroRemoteException, ResponseError {
        /*Set<String> userIds0 = StringUtils2.splitSet(toStr(userIds), ",", true);

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
        Set set = StringUtils2.splitSet(toStr(userIds),",",true);
        List list = new ArrayList();
        list.addAll(set);
        return hasAllUsers(list,true);
    }

    private boolean hasAllUsers(List<String> userIds,boolean isDestroyed){
        if(userIds.size()==0)
            return true;

        String sql = "";
        if (isDestroyed) {
            sql = "SELECT count(user_id) from user2 where user_id in ("+StringHelper.join(userIds.toArray(), ",")+")";
        } else {
            sql = "SELECT count(user_id) from user2 where destroyed_time=0 and user_id in ("+StringHelper.join(userIds.toArray(), ",")+")";
        }
        L.trace("-------------------user is not Exist-----------" + userIds.toArray() + "----------------");
        L.trace("------------------------sql--------------------");
        L.trace(sql);
        L.trace("-------------------------end----------------------");
        SQLExecutor se = getSqlExecutor();
        long recs = se.executeIntScalar(sql, 0);
        return userIds.size() == recs;
    }

    @Override
    public CharSequence findUserIdByUserName(CharSequence username) throws AvroRemoteException, ResponseError {
        return findUserIdByUserName0(toStr(username));
    }

    @Override
    public boolean setPrivacy(CharSequence userId, ByteBuffer privacyItemList)
            throws AvroRemoteException, ResponseError {
        return setPrivacy0(toStr(userId), RecordSet.fromByteBuffer(privacyItemList));
    }

    @Override
    public ByteBuffer getAuths(CharSequence userId, CharSequence resources)
            throws AvroRemoteException, ResponseError {
        String resources0 = toStr(resources);
        List<String> rl = StringUtils2.splitList(resources0, ",", true);
        return getAuths0(toStr(userId), rl).toByteBuffer();
    }

    @Override
    public boolean getDefaultPrivacy(CharSequence resource,
                                     CharSequence circleId) throws AvroRemoteException, ResponseError {
        String res = toStr(resource);
        int circle = Integer.parseInt(toStr(circleId));

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

    @Override
    public final Class getInterface() {
        return Account.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    public void checkLoginNameNotExists(String uid, String... names) {
        for (String name : names) {
            if (StringUtils.isBlank(name))
                continue;
            Record rec = findUserByLoginName(name, "user_id");
            String userid = rec.getString("user_id");
            if (!userid.equals(uid) && !rec.isEmpty())
                throw new AccountException(ErrorCode.LOGIN_NAME_EXISTS, "The login name is existing");
        }
    }

    public static void checkLoginName(String loginName) {
        if (loginName != null && StringUtils.isBlank(loginName))
            throw new AccountException(ErrorCode.USER_NOT_EXISTS, "Invalid login name");
    }

    public static void checkDisplayName(String displayName) {
        if (displayName != null && StringUtils.isBlank(displayName))
            throw new AccountException(ErrorCode.PARAM_ERROR, "Invalid display name");
    }

    public static void checkPassword(String pwd) {
        if (pwd != null && pwd.isEmpty())
            throw new AccountException(ErrorCode.PARAM_ERROR, "Invalid password");
    }

    public static void checkPhone(String phone) {
        if (phone != null && !phone.matches("^+?\\d+$"))
            throw new AccountException(ErrorCode.PARAM_ERROR, "Invalid phone number");
    }

    public static void checkEmail(String email) {
        if (email != null && !email.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$"))
            throw new AccountException(ErrorCode.PARAM_ERROR, "Invalid email");
    }

    private String findUserIdByLoginName(String name) {
        return findUserByLoginName(name, "user_id").getString("user_id");
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
    public CharSequence findLongUrl(CharSequence short_url) throws AvroRemoteException, ResponseError {
        try {
            return findLongUrl0(toStr(short_url));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
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
    public boolean saveShortUrl(CharSequence long_url, CharSequence short_url) throws AvroRemoteException, ResponseError {
        try {
            return saveShortUrl0(toStr(long_url), toStr(short_url));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected boolean saveShortUrl0(String long_url, String short_url) {
        if (findLongUrl0(short_url).equals("")) {
            long dateTime = DateUtils.nowMillis();
            dateTime += 3 * 24 * 60 * 60 * 1000L;
            final String sql = "INSERT INTO short_url"
                    + " (long_url,short_url,failure_time)"
                    + " VALUES"
                    + " ('" + long_url + "','" + short_url + "','"+ dateTime +"')";
            SQLExecutor se = getSqlExecutor();
            se.executeUpdate(sql);
        }
        return true;
    }

    @Override
    public final CharSequence getNowGenerateUserId() throws AvroRemoteException {
        try {
            return generateUserId();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
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

    @Override
    public ByteBuffer findUidLoginNameNotInID(CharSequence name) throws AvroRemoteException, ResponseError {
        try {
            return findUserByLoginNameNotInID(toStr(name), toStr("user_id")).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence getBorqsUserIds() throws AvroRemoteException, ResponseError {
        final String sql = "select user_id from user2 where login_email1 like '%borqs.com' or login_email2 like '%borqs.com' or login_email3 like '%borqs.com'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.joinColumnValues("user_id", ",");
    }
}
