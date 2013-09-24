package com.borqs.server.impl.account;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.PropertyEntries;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.status.Status;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ObjectHolder;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserDb extends SqlSupport {
    // table
    private Table userTable;
    private Table propertyTable;

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

    private ShardResult shardUser() {
        return userTable.getShard(0);
    }

    private ShardResult shardProperty(long userId) {
        return propertyTable.shard(userId);
    }

    public User createUserMigration(final Context ctx, final User user) {
        user.setCreatedTime(DateHelper.nowMillis());
        final ShardResult userSR = shardUser();
        final long userId = sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = UserSql.insertUserMigration(userSR.table, user);
                final ObjectHolder<Object> idHolder = new ObjectHolder<Object>();
                long n = SqlExecutor.executeUpdate(ctx, conn, sql, idHolder);
                return n > 0 ? idHolder.toInt() : 0L;
            }
        });
        if (userId == 0)
            throw new ServerException(E.WRITE_USER, "Create account error");

        user.setUserId(userId);

        if (user.hasProperties()) {
            try {
                final ShardResult propSR = shardProperty(userId);
                sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Object>() {
                    @Override
                    protected Object handleConnection(Connection conn) {
                        List<String> sqls = UserSql.insertProperties(propSR.table, user);
                        SqlExecutor.executeUpdate(ctx, conn, sqls);
                        return null;
                    }
                });
            } catch (Exception e) {
                sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Object>() {
                    @Override
                    protected Object handleConnection(Connection conn) {
                        String sql = UserSql.purgeUser(userSR.table, userId);
                        SqlExecutor.executeUpdate(ctx, conn, sql);
                        return null;
                    }
                });
                throw new ServerException(E.WRITE_USER, e);
            }
        }

        return user;
    }


    public User createUser(final Context ctx, final User user) {
        user.setCreatedTime(DateHelper.nowMillis());
        final ShardResult userSR = shardUser();
        final long userId = sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = UserSql.insertUser(userSR.table, user);
                final ObjectHolder<Object> idHolder = new ObjectHolder<Object>();
                long n = SqlExecutor.executeUpdate(ctx, conn, sql, idHolder);
                return n > 0 ? idHolder.toInt() : 0L;
            }
        });
        if (userId == 0)
            throw new ServerException(E.WRITE_USER, "Create account error");

        user.setUserId(userId);

        if (user.hasProperties()) {
            try {
                final ShardResult propSR = shardProperty(userId);
                sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Object>() {
                    @Override
                    protected Object handleConnection(Connection conn) {
                        List<String> sqls = UserSql.insertProperties(propSR.table, user);
                        SqlExecutor.executeUpdate(ctx, conn, sqls);
                        return null;
                    }
                });
            } catch (Exception e) {
                sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Object>() {
                    @Override
                    protected Object handleConnection(Connection conn) {
                        String sql = UserSql.purgeUser(userSR.table, userId);
                        SqlExecutor.executeUpdate(ctx, conn, sql);
                        return null;
                    }
                });
                throw new ServerException(E.WRITE_USER, e);
            }
        }

        return user;
    }


    public boolean destroyUser(final Context ctx, final long userId) {
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = UserSql.destroyUser(userSR.table, userId, DateHelper.nowMillis());
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }


    public boolean recoverUser(final Context ctx, final long userId) {
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = UserSql.recoverUser(userSR.table, userId);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }

    public boolean update(final Context ctx, final User user) {

        final ShardResult userSR = user.getPassword() != null ? shardUser() : null;
        final ShardResult propSR = user.hasProperties() ? shardProperty(user.getUserId()) : null;

        String[] dbs = ShardResult.getDbs(userSR, propSR);

        if (dbs[0] != null || dbs[1] != null) {
            return sqlExecutor.openConnections(dbs, new ConnectionsHandler<Boolean>() {
                @Override
                public Boolean handle(Connection[] conns) {
                    Connection userConn = conns[0];
                    Connection propConn = conns[1];
                    if (userConn != null) {
                        String updateUserSql = UserSql.updateUser(userSR.table, user);
                        SqlExecutor.executeUpdate(ctx, userConn, updateUserSql);
                    }
                    if (propConn != null) {
                        List<String> updatePropSqls = UserSql.updateProperties(propSR.table, user);
                        SqlExecutor.executeUpdate(ctx, propConn, updatePropSqls);
                    }
                    return true;
                }
            });
        } else {
            return false;
        }
    }

    public Users getUsers(final Context ctx, final long[] userIds) {
        final Users users = new Users();
        if (userIds.length == 1) {
            final long userId = userIds[0];
            final ShardResult userSR = shardUser();
            final User user = sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<User>() {
                @Override
                protected User handleConnection(Connection conn) {
                    String sql = UserSql.findUsers(userSR.table, userId);
                    return SqlExecutor.executeFirst(ctx, conn, sql, new ResultSetReader<User>() {
                        @Override
                        public User read(ResultSet rs, User reuse) throws SQLException {
                            return UserRs.readUser(rs, reuse);
                        }
                    });
                }
            });

            if (user != null) {
                final ShardResult propSR = shardProperty(userId);
                sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Object>() {
                    @Override
                    protected Object handleConnection(Connection conn) {
                        String sql = UserSql.findProperties(propSR.table, userId);
                        SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                            @Override
                            public void handle(ResultSet rs) throws SQLException {
                                PropertyEntries entries = UserRs.readProperties(rs);
                                user.readProperties(entries);
                            }
                        });
                        return null;
                    }
                });
                users.add(user);
            }
        } else { // userIds.length > 1
            final ShardResult userSR = shardUser();
            sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = UserSql.findUsers(userSR.table, userIds);
                    return SqlExecutor.executeList(ctx, conn, sql, users, new ResultSetReader<User>() {
                        @Override
                        public User read(ResultSet rs, User reuse) throws SQLException {
                            return UserRs.readUser(rs, null);
                        }
                    });
                }
            });

            final long[] existedUserIds = users.getUserIds();
            final GroupedShardResults groupedPropSR = TableHelper.shard(propertyTable, existedUserIds);
            for (final ShardResult propSR : groupedPropSR.getShardResults()) {
                final long[] userIdsInShard = CollectionsHelper.toLongArray(groupedPropSR.get(propSR));
                sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Object>() {
                    @Override
                    protected Object handleConnection(Connection conn) {
                        String sql = UserSql.findProperties(propSR.table, userIdsInShard);
                        SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                            @Override
                            public void handle(ResultSet rs) throws SQLException {
                                Map<Long, PropertyEntries> userProps = UserRs.readGroupedProperties(rs);
                                for (Map.Entry<Long, PropertyEntries> e : userProps.entrySet()) {
                                    long userId = e.getKey();
                                    User user = users.getUser(userId);
                                    if (user != null)
                                        user.readProperties(e.getValue());
                                }
                            }
                        });
                        return null;
                    }
                });
            }
        }

        return users;
    }

    public String getPassword(final Context ctx, final long userId) {
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<String>() {
            @Override
            protected String handleConnection(Connection conn) {
                String sql = UserSql.findUserPassword(userSR.table, userId);
                return SqlExecutor.executeString(ctx, conn, sql, null);
            }
        });
    }

    public boolean hasAllUser(final Context ctx, final long[] userIds) {
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = UserSql.findUserIds(userSR.table, userIds);
                final ObjectHolder<Boolean> b = new ObjectHolder<Boolean>(false);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
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
            }
        });
    }

    public boolean hasAnyUser(final Context ctx, final long[] userIds) {
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = UserSql.findUserIds(userSR.table, userIds);
                final ObjectHolder<Boolean> b = new ObjectHolder<Boolean>(false);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        Set<Long> existsUserIds = UserRs.readIds(rs, null);
                        b.value = !existsUserIds.isEmpty();
                    }
                });
                return b.value;
            }
        });
    }

    public boolean hasUser(final Context ctx, final long userId) {
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = UserSql.findUserId(userSR.table, userId);
                long r = SqlExecutor.executeInt(ctx, conn, sql, 0);
                return r > 0 && r == userId;
            }
        });
    }

    public long[] getExistsIds(final Context ctx, final long[] userIds) {
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<long[]>() {
            @Override
            protected long[] handleConnection(Connection conn) {
                String sql = UserSql.findUserIds(userSR.table, userIds);
                final HashSet<Long> existsIds = new HashSet<Long>();
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        UserRs.readIds(rs, existsIds);
                    }
                });
                return CollectionsHelper.toLongArray(existsIds);
            }
        });
    }

    public Map<Long, Status> getStatuses(final Context ctx, final long[] userIds) {
        final ShardResult userSR = shardUser();
        final HashMap<Long, Status> m = new HashMap<Long, Status>();
        sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = UserSql.getStatuses(userSR.table, userIds);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        UserRs.readStatuses(rs, m);
                    }
                });
                return m;
            }
        });
        return m;
    }

    public Status getStatus(final Context ctx, final long userId) {
        Map<Long, Status> m = getStatuses(ctx, new long[]{userId});
        return MapUtils.isNotEmpty(m) ? m.values().iterator().next() : new Status("", 0L);
    }

    public boolean updateStatus(final Context ctx, final Status st) {
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = UserSql.updateStatus(userSR.table, ctx.getViewer(), st);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }


    public long[] searchUserIds(final Context ctx, final Record opts, final String word, Page page) {
        final int count = (int)page.count;
        final ArrayList<SearchUserEntry> sues = new ArrayList<SearchUserEntry>();
        for (final ShardResult propSR : TableHelper.getAllShards(propertyTable)) {
            sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = UserSql.search(propSR.table, word);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            UserRs.readSearchEntries(rs, sues, count);
                        }
                    });
                    return null;
                }
            });
            if (sues.size() >= count)
                break;
        }
        // TODO: sort by key and sub 'display_name'?
        LinkedHashSet<Long> userIds = new LinkedHashSet<Long>();
        for (SearchUserEntry sue : sues)
            userIds.add(sue.userId);
        return CollectionsHelper.toLongArray(userIds);
    }

    public static class SearchUserEntry {
        public final long userId;
        public final int key;
        public final int sub;

        public SearchUserEntry(long userId, int key, int sub) {
            this.userId = userId;
            this.key = key;
            this.sub = sub;
        }
    }
}

