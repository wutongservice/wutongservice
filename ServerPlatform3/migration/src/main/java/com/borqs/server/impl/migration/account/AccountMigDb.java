package com.borqs.server.impl.migration.account;

import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.PropertyEntries;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;

public class AccountMigDb extends SqlSupport {
    private static final Logger L = Logger.get(AccountMigDb.class);
    // table
    private Table accountTable;
    private Table cibindTable;


    public Table getCibindTable() {
        return cibindTable;
    }

    public void setCibindTable(Table cibindTable) {
        if (cibindTable != null)
            Validate.isTrue(cibindTable.getShardCount() == 1);
        this.cibindTable = cibindTable;
    }
    public AccountMigDb() {
    }


    private Table userTable;
    private Table propertyTable;


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
    private ShardResult shard() {
        return cibindTable.getShard(0);
    }
    public static long[] userIds;
    public long[] getAllUserIds(final Context ctx) {
        if(userIds != null)
            return userIds;
        final ShardResult userSR = shardUser();
        return sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<long[]>() {
            @Override
            protected long[] handleConnection(Connection conn) {
                String sql = AccountMigSql.findAllUserIds(userSR.table);
                final HashSet<Long> existsIds = new HashSet<Long>();
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        AccountMigRs.readIds(rs, existsIds);
                    }
                });
                userIds =  CollectionsHelper.toLongArray(existsIds);
                return userIds;
            }
        });
    }

    public Users getUsers(final Context ctx, final long[] userIds) {
        final Users users = new Users();
        if (userIds.length == 1) {
            final long userId = userIds[0];
            final ShardResult userSR = shardUser();
            final User user = sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<User>() {
                @Override
                protected User handleConnection(Connection conn) {
                    String sql = AccountMigSql.findUsers(userSR.table, userId);
                    return SqlExecutor.executeFirst(ctx, conn, sql, new ResultSetReader<User>() {
                        @Override
                        public User read(ResultSet rs, User reuse) throws SQLException {
                            return AccountMigRs.readUser(rs, reuse);
                        }
                    });
                }
            });

            if (user != null) {
                final ShardResult propSR = shardProperty(userId);
                sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Object>() {
                    @Override
                    protected Object handleConnection(Connection conn) {
                        String sql = AccountMigSql.findProperties(propSR.table, userId);
                        SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                            @Override
                            public void handle(ResultSet rs) throws SQLException {
                                PropertyEntries entries = AccountMigRs.readProperties(rs);
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
                    String sql = AccountMigSql.findUsers(userSR.table, userIds);
                    return SqlExecutor.executeList(ctx, conn, sql, users, new ResultSetReader<User>() {
                        @Override
                        public User read(ResultSet rs, User reuse) throws SQLException {
                            return AccountMigRs.readUser(rs, null);
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
                        String sql = AccountMigSql.findProperties(propSR.table, userIdsInShard);
                        SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                            @Override
                            public void handle(ResultSet rs) throws SQLException {
                                Map<Long, PropertyEntries> userProps = AccountMigRs.readGroupedProperties(rs);
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

    public void bind(final Context ctx, final BindingInfo bi) {
        final ShardResult cibindSR = shard();
        sqlExecutor.openConnection(cibindSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = AccountMigSql.insertBindingInfo(cibindSR.table, ctx.getViewer(), bi);
                try {
                    SqlExecutor.executeUpdate(ctx, conn, sql);
                } catch (Exception e) {
                    throw new ServerException(E.BINDING_EXISTS, "The binding is exists");
                }
                return null;
            }
        });
    }

    public boolean updateSortKey(final Context ctx, final User user) {

        final ShardResult userSR = user.getPassword() != null ? shardUser() : null;
        final ShardResult propSR = user.hasProperties() ? shardProperty(user.getUserId()) : null;

        String[] dbs = ShardResult.getDbs(userSR, propSR);

        if (dbs[0] != null || dbs[1] != null) {
            return sqlExecutor.openConnections(dbs, new ConnectionsHandler<Boolean>() {
                @Override
                public Boolean handle(Connection[] conns) {
                    Connection userConn = conns[0];
                    if (userConn != null) {
                        String updateUserSql = AccountMigSql.updateUser(userSR.table, user);
                        SqlExecutor.executeUpdate(ctx, userConn, updateUserSql);
                    }
                    return true;
                }
            });
        } else {
            return false;
        }
    }
}

