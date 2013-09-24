package com.borqs.server.wutong.account2;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.ObjectHolder;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.user.PropertyEntries;
import com.borqs.server.wutong.account2.user.User;
import com.borqs.server.wutong.account2.user.UserHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserDb {
    private static final Logger L = Logger.getLogger(UserDb.class);
    // table
    private Table userTable;
    private Table propertyTable;
    private static String userSR_table = "user2";
    private static String propSR_table = "user_property";
    private String db;
    private String AccountTable;
    private String UserProertyTable;

    private ConnectionFactory connectionFactory;
    private static String userSR_Migration_table;
    private static String propSR_Migration_table;


    public void init() {


    }

    public void setConfig(Configuration conf) {
        this.db = conf.getString("db.account2", null);

        String url = ConnectionFactory.getConnectionString(db);

        this.AccountTable = conf.getString("db.account2.userTable", "user2");
        this.UserProertyTable = conf.getString("db.account2.user_property", "user_property");
        this.connectionFactory = ConnectionFactory.getConnectionFactory("dbcp");
        this.userSR_Migration_table = conf.getString("db.account2.userTable.migration", "user3");
        this.propSR_Migration_table = conf.getString("db.account2.user_property.migration", "user3_property");
    }

    public UserDb() {
    }

    public Table getUserTable() {
        return userTable;
    }

    public void setUserTable(Table userTable) {
        if (userTable != null)
            Validate.isTrue(userTable.getShardCount() == 1);
        this.userTable = userTable;
    }

    public Table getPropertyTable() {
        return propertyTable;
    }

    public void setPropertyTable(Table propertyTable) {
        this.propertyTable = propertyTable;
    }

    public User createUserMigration(final User user) throws SQLException {
        user.setCreatedTime(DateUtils.nowMillis());


        String sql = UserSql.insertUserMigration(userSR_Migration_table, user);
        //final ObjectHolder<Object> idHolder = new ObjectHolder<Object>();

        SQLExecutor.executeUpdate(connectionFactory, db, sql);

        long userId = user.getUserId();
        if (userId == 0)
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "Create account error");
        List<User> list = new ArrayList<User>();
        list.add(user);

        user.setUserId(userId);

        if (user.hasProperties()) {
            try {
                List<String> sqls = UserSql.insertProperties(propSR_Migration_table, user);
                SQLExecutor.executeUpdate(connectionFactory, db, sqls);
                return null;

            } catch (Exception e) {

                String sql2 = UserSql.purgeUser(userSR_Migration_table, userId);
                SQLExecutor.executeUpdate(connectionFactory, db, sql2);
                throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, e);
            }
        }

        return user;
    }

    public User createUser(final User user) throws SQLException {
        user.setCreatedTime(DateUtils.nowMillis());

        L.trace(null, "-------------before gen user Object-----------" + user.toString() + "--------------------------");
        String sql = UserSql.insertUser(userSR_table, user);
        L.trace(null, "-------------after gen sql -----------" + sql.toString() + "--------------------------");

        SQLExecutor.executeUpdate(connectionFactory, db, sql);

        long userId = user.getUserId();
        List<User> list = new ArrayList<User>();
        list.add(user);

        if (user.hasProperties()) {
            try {
                List<String> sqls = UserSql.insertProperties(propSR_table, user);
                SQLExecutor.executeUpdate(connectionFactory, db, sqls);
            } catch (Exception e) {

                String sql2 = UserSql.purgeUser(userSR_table, userId);
                SQLExecutor.executeUpdate(connectionFactory, db, sql2);
                throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, e);
            }
        }

        return user;
    }


    public boolean destroyUser(final long userId) {
        String sql = UserSql.destroyUser(userSR_table, userId, DateUtils.nowMillis());
        long n = SQLExecutor.executeUpdate(connectionFactory, db, sql);
        return n > 0;

    }

    public boolean updateVisitTime(final long userId,final long time) {
        String sql = UserSql.updateVisitTime(userSR_table, userId, time);
        long n = SQLExecutor.executeUpdate(connectionFactory, db, sql);
        return n > 0;

    }

    public boolean recoverUser(final long userId) {

        String sql = UserSql.recoverUser(userSR_table, userId);
        long n = SQLExecutor.executeUpdate(connectionFactory, db, sql);
        return n > 0;
    }

    public boolean update(final User user) {
        try {
            String updateUserSql = UserSql.updateUser(userSR_table, user);
            long l = SQLExecutor.executeUpdate(connectionFactory, db, updateUserSql);

            List<String> updatePropSqls = UserSql.updateProperties(propSR_table, user);
            long s = SQLExecutor.executeUpdate(connectionFactory, db, updatePropSqls);

            return l + s > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateSortKey4Migrate(final User user) {
        try {
            String updateUserSql = UserSql.updateUser(userSR_table, user);
            long l = SQLExecutor.executeUpdate(connectionFactory, db, updateUserSql);

            return l > 0;
        } catch (Exception e) {
            return false;
        }
    }

    protected User handleConnection(Connection conn, long userId) {
        String sql = UserSql.findUsers(AccountTable, userId);
        return SQLExecutor.executeFirst(conn, sql, new ResultSetReader<User>() {
            @Override
            public User read(ResultSet rs, User reuse) throws SQLException {
                return UserRs.readUser(rs, reuse);
            }
        });
    }

    protected Object handleConnection2(Connection conn, long userId, final User user) {
        String sql = UserSql.findProperties(UserProertyTable, userId);
        SQLExecutor.executeCustom(conn, sql, new ResultSetHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {

                PropertyEntries entries = UserRs.readProperties(rs);
                user.readProperties(entries);
            }
        });
        return null;
    }

    protected Object handleConnection3(Connection conn, final long[] userIds, final List<User> users) {
        String sql = UserSql.findUsers(AccountTable, userIds);
        return SQLExecutor.executeList(conn, sql, users, new ResultSetReader<User>() {
            @Override
            public User read(ResultSet rs, User reuse) throws SQLException {
                return UserRs.readUser(rs, null);
            }
        });
    }

    protected Object handleConnection4(Connection conn, long[] userIdsInShard, final List<User> users) {
        String sql = UserSql.findProperties(UserProertyTable, userIdsInShard);
        SQLExecutor.executeCustom(conn, sql, new ResultSetHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                //Map<Long, List<PropertyEntries>> userProps = UserRs.readGroupedProperties(rs);
                Map<Long, PropertyEntries> userProps = UserRs.readGroupedProperties(rs);
                for (Map.Entry<Long, PropertyEntries> e : userProps.entrySet()) {
                    long userId = e.getKey();
                    User user = UserHelper.findUser(users, userId);
                    if (user != null)
                        user.readProperties(e.getValue());
                }
            }
        });
        return null;
    }

    public List<User> getUsers(final long[] userIds) throws SQLException {

        List<User> users = new ArrayList<User>();
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection(db);
            if (userIds.length == 1) {
                final long userId = userIds[0];

                final User user = handleConnection(connection, userId);
                if (user != null) {
                    handleConnection2(connection, userId, user);

                    users.add(user);
                }

            } else { // userIds.length > 1
                handleConnection3(connection, userIds, users);

                final long[] existedUserIds = UserHelper.getUsersIds(users);
                handleConnection4(connection, existedUserIds, users);
            }
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        users = addUpdateTime(users);


        return users;
    }


    /**
     * add updated_time for user Object
     *
     * @param users
     * @return
     */
    private List<User> addUpdateTime(List<User> users) {
        if (CollectionUtils.isEmpty(users))
            return null;

        for (User user : users) {
            long basic_updated_time = user.getPropertyUpdatedTime(User.COL_PHOTO);
            if (basic_updated_time > 0)
                user.setAddon("basic_updated_time", basic_updated_time);

            long name_update_time = user.getPropertyUpdatedTime(User.COL_NAME);
            if (name_update_time > 0)
                user.setAddon("name_update_time", name_update_time);

            long profile_updated_time = user.getPropertyUpdatedTime(User.COL_PROFILE);
            if (profile_updated_time > 0)
                user.setAddon("profile_updated_time", profile_updated_time);

            long contact_info_updated_time = user.getPropertyUpdatedTime(User.COL_EMAIL);
            if (contact_info_updated_time > 0)
                user.setAddon("contact_info_updated_time", contact_info_updated_time);

            long tel_update_time = user.getPropertyUpdatedTime(User.COL_TEL);
            if (tel_update_time > 0)
                user.setAddon("tel_update_time", tel_update_time);

            long address_updated_time = user.getPropertyUpdatedTime(User.COL_ADDRESS);
            if (address_updated_time > 0)
                user.setAddon("address_updated_time", address_updated_time);
        }
        return users;
    }

    public String getPassword(final long userId) throws SQLException {
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection(db);
            String sql = UserSql.findUserPassword(userSR_table, userId);
            return SQLExecutor.executeString(connection, sql, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return null;
    }

    public boolean hasAllUser(final long[] userIds) throws SQLException {

        String sql = UserSql.findUserIds(userSR_table, userIds);
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection(db);
            final ObjectHolder<Boolean> b = new ObjectHolder<Boolean>(false);
            SQLExecutor.executeCustom(connection, sql, new ResultSetHandler() {
                @Override
                public void handle(ResultSet rs) throws SQLException {
                    Set<Long> existsUserIds = UserRs.readIds(rs, null);
                    for (long userId : userIds) {
                        if (!existsUserIds.contains(userId)) {
                            b.value = false;
                            return;
                        }
                    }
                    b.value = true;
                }
            });

            return b.value;
        } finally {
            connection.close();
        }
    }

    public boolean hasAnyUser(final long[] userIds) throws SQLException {

        String sql = UserSql.findUserIds(userSR_table, userIds);
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection(db);
            final ObjectHolder<Boolean> b = new ObjectHolder<Boolean>(false);
            SQLExecutor.executeCustom(connection, sql, new ResultSetHandler() {
                @Override
                public void handle(ResultSet rs) throws SQLException {
                    Set<Long> existsUserIds = UserRs.readIds(rs, null);
                    b.value = !existsUserIds.isEmpty();
                }
            });
            return b.value;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return false;
    }

    public boolean hasUser(final long userId) throws SQLException {

        String sql = UserSql.findUserId(userSR_table, userId);
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection(db);
            long r = SQLExecutor.executeInt(connection, sql, 0);
            return r > 0 && r == userId;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return false;
    }

    public long[] getExistsIds(final long[] userIds) throws SQLException {

        String sql = UserSql.findUserIds(userSR_table, userIds);
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection(db);
            final HashSet<Long> existsIds = new HashSet<Long>();
            SQLExecutor.executeCustom(connection, sql, new ResultSetHandler() {
                @Override
                public void handle(ResultSet rs) throws SQLException {
                    UserRs.readIds(rs, existsIds);
                }
            });
            return CollectionUtils2.toLongArray(existsIds);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return null;
    }

    public long deleteUserMigration(final long userId) throws SQLException {


        String sql = UserSql.purgeUser(userSR_table, userId);


        long userFlag = SQLExecutor.executeUpdate(connectionFactory, db, sql);

        String sql_property = UserSql.purgeUser_Property(propSR_table, userId);
        long user_PropertyFlag = SQLExecutor.executeUpdate(connectionFactory, db, sql_property);

        return userFlag & user_PropertyFlag;

    }

    public long[] getAllUserIds() throws SQLException {
        String sql = UserSql.getAllUserIds(userSR_table);
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection(db);
            final HashSet<Long> existsIds = new HashSet<Long>();
            SQLExecutor.executeCustom(connection, sql, new ResultSetHandler() {
                @Override
                public void handle(ResultSet rs) throws SQLException {
                    UserRs.readIds(rs, existsIds);
                }
            });
            return CollectionUtils2.toLongArray(existsIds);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return null;

    }

}

