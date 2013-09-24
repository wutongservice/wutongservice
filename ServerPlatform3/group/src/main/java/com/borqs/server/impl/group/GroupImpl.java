package com.borqs.server.impl.group;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.*;
import com.borqs.server.platform.feature.group.*;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import static com.borqs.server.platform.feature.group.Group.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupImpl extends SqlSupport implements GroupLogic {
    private static final Logger L = Logger.get(GroupImpl.class);

    private final Schema groupSchema = Schema.loadClassPath(GroupImpl.class, "group.schema");

    private Table groupTable;
    private Table propertiesTable;
    private Table membersTable;
    private Table pendingTable;
    private Table groupIdCounterTable;

    public GroupImpl() {
    }

    public Table getGroupTable() {
        return groupTable;
    }

    public void setGroupTable(Table groupTable) {
        if (groupTable != null)
            Validate.isTrue(groupTable.getShardCount() == 1);
        this.groupTable = groupTable;
    }

    public Table getPropertiesTable() {
        return propertiesTable;
    }

    public void setPropertiesTable(Table propertiesTable) {
        if (propertiesTable != null)
            Validate.isTrue(propertiesTable.getShardCount() == 1);
        this.propertiesTable = propertiesTable;
    }

    public Table getMembersTable() {
        return membersTable;
    }

    public void setMembersTable(Table membersTable) {
        if (membersTable != null)
            Validate.isTrue(membersTable.getShardCount() == 1);
        this.membersTable = membersTable;
    }

    public Table getPendingTable() {
        return pendingTable;
    }

    public void setPendingTable(Table pendingTable) {
        if (pendingTable != null)
            Validate.isTrue(pendingTable.getShardCount() == 1);
        this.pendingTable = pendingTable;
    }

    public Table getGroupIdCounterTable() {
        return groupIdCounterTable;
    }

    public void setGroupIdCounterTable(Table groupIdCounterTable) {
        this.groupIdCounterTable = groupIdCounterTable;
    }


    private ShardResult shardGroupIdCounter() {
        return groupIdCounterTable.getShard(0);
    }

    private ShardResult shardGroup() {
        return groupTable.getShard(0);
    }

    private ShardResult shardGroupProperties() {
        return propertiesTable.getShard(0);
    }

    private ShardResult shardGroupMembers() {
        return membersTable.getShard(0);
    }

    private ShardResult shardPending() {
        return pendingTable.getShard(0);
    }


    protected long generateGroupId(final Context ctx, final long begin, String type) {
        L.debug(ctx, "begin: " + begin);
        L.debug(ctx, "activity: " + ACTIVITY_ID_BEGIN);
        L.debug(ctx, "compare: " + (begin == ACTIVITY_ID_BEGIN));

        if (begin == ACTIVITY_ID_BEGIN) {
            type = TYPE_ACTIVITY;
        } else if (begin == ORGANIZATION_ID_BEGIN) {
            type = TYPE_ORGANIZATION;
        } else if (begin == PUBLIC_CIRCLE_ID_BEGIN) {
            type = TYPE_PUBLIC_CIRCLE;
        } else if (begin == GENERAL_GROUP_ID_BEGIN) {
            type = TYPE_GENERAL_GROUP;
        }

        final String type0 = type;
        final ShardResult gicSR = shardGroupIdCounter();
        return sqlExecutor.openConnection(gicSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                final String SQL1 = "INSERT INTO ${table} (key_, count_) VALUES ('${type}', 0)"
                        + " ON DUPLICATE KEY UPDATE count_ = count_ + 1";

                final String SQL2 = "SELECT count_ FROM ${table} WHERE key_ = '${type}'";

                String sql1 = SQLTemplate.merge(SQL1, "table", gicSR.table, "type", type0);
                String sql2 = SQLTemplate.merge(SQL2, "table", gicSR.table, "type", type0);
                SqlExecutor.executeUpdate(ctx, conn, sql1);
                long n = SqlExecutor.executeInt(ctx, conn, sql2, -1L);
                if (n < 0)
                    throw new ServerException(E.CREATE_GROUP_ID, "Create group id error");
                return begin + n;
            }
        });
    }

    @Override
    public long createGroup(final Context ctx, String type, String name, final GroupOptions options) {
        if (options.getMemberLimit() <= 0)
            throw new ServerException(E.MEMBER_LIMIT_ERROR, "Invalid member limit");

        final long now = DateHelper.nowMillis();
        final long groupId = generateGroupId(ctx, options.getBegin(), type);
        final Record info = new Record();
        info.put(COL_ID, groupId);
        info.put(COL_NAME, name);
        info.put(COL_MEMBER_LIMIT, options.getMemberLimit());
        info.put(COL_IS_STREAM_PUBLIC, options.getStreamPublic());
        info.put(COL_CAN_SEARCH, options.getCanSearch());
        info.put(COL_CAN_VIEW_MEMBERS, options.getCanViewMembers());
        info.put(COL_CAN_JOIN, options.getCanJoin());
        info.put(COL_CAN_MEMBER_INVITE, options.getCanMemberInvite());
        info.put(COL_CAN_MEMBER_APPROVE, options.getCanMemberApprove());
        info.put(COL_CAN_MEMBER_POST, options.getCanMemberPost());
        info.put(COL_CREATOR, ctx.getViewer());
        info.put(COL_LABEL, options.getLabel());
        info.put(COL_CREATED_TIME, now);
        info.put(COL_UPDATED_TIME, now);

        final ShardResult groupSR = shardGroup();
        final ShardResult propSR = shardGroupProperties();
        final ShardResult memberSR = shardGroupMembers();
        try {
            sqlExecutor.openConnections(groupSR.db, propSR.db, memberSR.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    Connection groupConn = conns[0];
                    Connection propConn = conns[1];
                    Connection memberConn = conns[2];

                    final String SQL = "INSERT INTO ${table} ${values_join(alias, info)}";
                    String sql1 = SQLTemplate.merge(SQL,
                            "table", groupTable,
                            "alias", groupSchema.getAllAliases(),
                            "info", info);
                    SqlExecutor.executeUpdate(ctx, groupConn, sql1);

                    Record properties = options.getProperties();
                    if (!properties.isEmpty()) {
                        String sql2 = "INSERT INTO " + propSR.table + " (`group_id`, `key_`, `value_`) VALUES ";
                        for (Map.Entry<String, Object> entry : properties.entrySet()) {
                            String key = entry.getKey();
                            String value = ObjectUtils.toString(entry.getValue());
                            sql2 += "(" + groupId + ", '" + key + "', '" + value + "'), ";
                        }
                        sql2 = StringUtils.substringBeforeLast(sql2, ",");
                        SqlExecutor.executeUpdate(ctx, propConn, sql2);
                    }

                    String sql3 = "INSERT INTO " + memberSR.table + " (group_id, member, role, joined_time, updated_time, created_time) VALUES ("
                            + groupId + ", " + info.getInt(COL_CREATOR) + ", " + ROLE_CREATOR + ", " + now + ", " + now + ", " + now + ")";
                    SqlExecutor.executeUpdate(ctx, memberConn, sql3);
                    return null;
                }
            });
            return groupId;
        } catch (Exception e) {
            throw new ServerException(E.CREATE_GROUP, "save public circle error");
        }
    }

    @Override
    public boolean updateGroup(final Context ctx, final long groupId, final Group group, final Record properties) {
        final Record info = group.toInfo();
        long now = DateHelper.nowMillis();
        info.set(Group.COL_UPDATED_TIME, now);

        final ShardResult groupSR = shardGroup();
        final ShardResult propSR = shardGroupProperties();
        return sqlExecutor.openConnections(groupSR.db, propSR.db, new ConnectionsHandler<Boolean>() {
            @Override
            public Boolean handle(Connection[] conns) {
                Connection groupConn = conns[0];
                Connection propConn = conns[1];
                String sql1 = new SQLBuilder.Update(groupSchema)
                        .update(groupSR.table)
                        .values(info)
                        .where("${alias.id}=${v(id)}", "id", groupId)
                        .and("destroyed_time=0")
                        .toString();
                long n = SqlExecutor.executeUpdate(ctx, groupConn, sql1);
                if (n == 0 || properties.isEmpty())
                    return false;

                String sql2 = "INSERT INTO " + propSR.table + " (`group_id`, `key_`, `value_`) VALUES ";

                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String key = entry.getKey();
                    String value = ObjectUtils.toString(entry.getValue());
                    sql2 += "(" + groupId + ", '" + key + "', '" + value + "'), ";
                }
                sql2 = StringUtils.substringBeforeLast(sql2, ",");
                sql2 += " ON DUPLICATE KEY UPDATE value_=VALUES(value_)";
                n = SqlExecutor.executeUpdate(ctx, propConn, sql2);
                return n > 0;
            }
        });
    }

    @Override
    public boolean destroyGroup(final Context ctx, final long[] groupIds) {
        final ShardResult groupSR = shardGroup();
        return sqlExecutor.openConnection(groupSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                final String SQL = "UPDATE ${table} SET destroyed_time=${v(deleted_time)} WHERE ${alias.id} IN (${ids})";
                String sql = SQLTemplate.merge(SQL,
                        "table", groupTable,
                        "deleted_time", DateHelper.nowMillis(),
                        "alias", groupSchema.getAllAliases(),
                        "ids", groupIds);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }

    @Override
    public Group getGroup(Context ctx, long groupId) {
        Groups groups = getGroups(ctx, new long[]{groupId}, new GroupIdRange(0, 0));
        return !groups.isEmpty() ? groups.get(0) : null;
    }

    @Override
    public Groups getGroups(Context ctx, long[] groupIds, GroupIdRange idRange) {
        return getGroupsHelper(ctx, idRange.begin, idRange.end, groupIds);
    }

    private static String filter(long begin, long end) {
        String condition = "";
        if (begin >= PUBLIC_CIRCLE_ID_BEGIN
                && end <= GROUP_ID_END
                && end >= begin) {
            condition = " AND id>=" + begin + " AND id<" + end;
        }

        return condition;
    }

    private void fillGroupsProperties(final Context ctx, final Groups groups) {
        final ShardResult propSR = shardGroupProperties();
        sqlExecutor.openConnection(propSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                final String SQL = "SELECT `key_`, `value_` FROM ${table} WHERE `group_id`=${groupId}";
                for (Group group : groups) {
                    String sql = SQLTemplate.merge(SQL,
                            "table", propSR.table,
                            "groupId", group.getGroupId());


                    RecordSet kvSet = SqlExecutor.executeRecords(ctx, conn, sql, null);
                    Record props = new Record();
                    for (Record kv : kvSet) {
                        props.put(kv.getString("key_"), kv.getString("value_"));
                    }
                    group.setProperties(props);
                }

                return null;
            }
        });
    }

    private Groups getGroupsHelper(final Context ctx, final long begin, final long end, final long[] groupIds) {
        final ShardResult groupSR = shardGroup();

        final Groups groups = new Groups();
        sqlExecutor.openConnection(groupSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String SQL = "SELECT ${as_join(alias)} FROM ${table} WHERE ${alias.id} IN (${ids}) AND destroyed_time=0";
                if (groupIds.length == 1)
                    SQL = "SELECT ${as_join(alias)} FROM ${table} WHERE ${alias.id}=${ids} AND destroyed_time=0";
                String sql = SQLTemplate.merge(SQL,
                        "alias", groupSchema.getAllAliases(),
                        "table", groupTable,
                        "ids", groupIds.length == 1 ? String.valueOf(groupIds[0]) : StringHelper.join(groupIds, ","));
                sql += filter(begin, end);
                RecordSet info = SqlExecutor.executeRecords(ctx, conn, sql, null);
                Schemas.standardize(groupSchema, info);
                groups.addGroupFromInfo(info);
                fillGroupsProperties(ctx, groups);
                return null;
            }
        });

        return groups;
    }

    @Override
    public Groups findGroupsByMember(final Context ctx, final GroupIdRange idRange, final long member) {
        final String SQL0 = "SELECT group_id FROM ${groupMembersTable} WHERE member=${member} AND destroyed_time=0";
        final String SQL = "SELECT ${as_join(alias)} FROM ${groupTable} WHERE ${alias.id} IN (" + SQL0  + ") AND destroyed_time=0";

        final Groups groups = new Groups();
        final ShardResult memberSR = shardGroupMembers();
        sqlExecutor.openConnection(memberSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = SQLTemplate.merge(SQL,
                        "alias", groupSchema.getAllAliases(),
                        "groupTable", groupTable,
                        "groupMembersTable", memberSR.table,
                        "member", String.valueOf(member));
                sql += filter(idRange.begin, idRange.end);
                RecordSet recs = SqlExecutor.executeRecords(ctx, conn, sql, null);
                Schemas.standardize(groupSchema, recs);
                groups.addGroupFromInfo(recs);
                fillGroupsProperties(ctx, groups);
                return null;
            }
        });
        return groups;
    }

    @Override
    public long[] findGroupIdsByMember(final Context ctx, final GroupIdRange idRange, final long member) {
        final String SQL0 = "SELECT group_id FROM ${groupMembersTable} WHERE member=${member} AND destroyed_time=0";
        final String SQL = "SELECT ${alias.id} FROM ${groupTable} WHERE ${alias.id} IN (" + SQL0  + ") AND destroyed_time=0";

        final ArrayList<Long> groupIds = new ArrayList<Long>();
        final ShardResult memberSR = shardGroupMembers();
        sqlExecutor.openConnection(memberSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = SQLTemplate.merge(SQL,
                        "alias", groupSchema.getAllAliases(),
                        "groupTable", groupTable,
                        "groupMembersTable", memberSR.table,
                        "member", String.valueOf(member));

                sql += filter(idRange.begin, idRange.end);
                SqlExecutor.executeList(ctx, conn, sql, groupIds, new ResultSetReader<Long>() {
                    @Override
                    public Long read(ResultSet rs, Long reuse) throws SQLException {
                        return rs.getLong("id");
                    }
                });
                return null;
            }
        });
        return CollectionsHelper.toLongArray(groupIds);
    }

    @Override
    public Groups findGroupsByName(final Context ctx, final String name, final GroupIdRange idRange) {
        final String SQL = "SELECT ${as_join(alias)} FROM ${table} WHERE ${alias.name} like '%${name}%' AND destroyed_time=0";

        final Groups groups = new Groups();
        final ShardResult groupSR = shardGroup();
        sqlExecutor.openConnection(groupSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = SQLTemplate.merge(SQL,
                        "alias", groupSchema.getAllAliases(),
                        "table", groupSR.table,
                        "name", name);
                sql += filter(idRange.begin, idRange.end);
                RecordSet recs = SqlExecutor.executeRecords(ctx, conn, sql, null);
                Schemas.standardize(groupSchema, recs);
                groups.addGroupFromInfo(recs);
                fillGroupsProperties(ctx, groups);
                return null;
            }
        });
        return groups;
    }

    protected boolean addOrGrantMembers(final Context ctx, final long groupId, final Map<Long, Integer> memberRoles) {

        final ShardResult groupSR = shardGroup();
        final ShardResult memberSR = shardGroupMembers();
        return sqlExecutor.openConnections(groupSR.db, memberSR.db, new ConnectionsHandler<Boolean>() {
            @Override
            public Boolean handle(Connection[] conns) {
                Connection groupConn = conns[0];
                Connection memberConn = conns[1];

                // member count
                String sql1 = "SELECT count(*) FROM " + memberSR.table + " WHERE group_id=" + groupId + " AND destroyed_time=0";
                int memberCount = (int)SqlExecutor.executeInt(ctx, memberConn, sql1, 0L);

                String sql0 = "SELECT member_limit FROM group_ WHERE id=" + groupId;
                long memberLimit = SqlExecutor.executeInt(ctx, groupConn, sql0, 100);
                long n = 0;

                if (memberCount < memberLimit) {
                    String sql = "INSERT INTO " + memberSR.table + " (group_id, member, role, joined_time, updated_time, created_time) VALUES ";

                    long now = DateHelper.nowMillis();
                    for (Map.Entry<Long, Integer> e : memberRoles.entrySet()) {
                        long member = e.getKey();
                        int role = e.getValue();
                        sql += "(" + groupId + ", " + member + ", " + role + ", " + now + ", " + now + ", " + now + "), ";
                    }

                    sql = StringUtils.substringBeforeLast(sql, ",");
                    sql += " ON DUPLICATE KEY UPDATE role=VALUES(role), joined_time=VALUES(joined_time), updated_time=VALUES(updated_time), destroyed_time=0";

                    n = SqlExecutor.executeUpdate(ctx, memberConn, sql);
                }
                return n > 0;
            }
        });
    }



    @Override
    public boolean addMember(Context ctx, long groupId, long member, int role) {
        HashMap<Long, Integer> roles = new HashMap<Long, Integer>();
        roles.put(member, role);
        return addOrGrantMembers(ctx, groupId, roles);
    }

    @Override
    public boolean addMembers(Context ctx, long groupId, Map<Long, Integer> roles) {
        return addOrGrantMembers(ctx, groupId, roles);
    }

    @Override
    public boolean removeMembers(final Context ctx, final long groupId, final long[] memberIds) {
        final ShardResult memberSR = shardGroupMembers();
        return sqlExecutor.openConnection(memberSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                final String SQL = "UPDATE ${table} SET destroyed_time=${v(deleted_time)} WHERE group_id=${groupId} AND member IN (${members})";
                String sql = SQLTemplate.merge(SQL,
                        "table", memberSR.table,
                        "deleted_time", DateHelper.nowMillis(),
                        "groupId", String.valueOf(groupId),
                        "members", StringHelper.join(memberIds, ","));
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }

    @Override
    public boolean grant(Context ctx, long groupId, long member, int role) {
        // TODO: check member exists?
        HashMap<Long, Integer> roles = new HashMap<Long, Integer>();
        roles.put(member, role);
        return addOrGrantMembers(ctx, groupId, roles);
    }

    @Override
    public boolean grants(Context ctx, long groupId, Map<Long, Integer> roles) {
        // TODO: check member exists?
        return addOrGrantMembers(ctx, groupId, roles);
    }

    @Override
    public long[] getMembersByRole(final Context ctx, final long groupId, final int role, final Page page) {
        final ShardResult memberSR = shardGroupMembers();
        final ArrayList<Long> memberIds = new ArrayList<Long>();
        sqlExecutor.openConnection(memberSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = "SELECT member FROM " + memberSR.table + " WHERE group_id=" + groupId;
                if (role != 0)
                    sql += " AND role=" + role;
                sql += " AND destroyed_time=0";

                sql += SQLTemplate.merge(" ${limit}",
                        "limit", Page.toLimit((int) page.page, (int) page.count));


                if (role == ROLE_CREATOR) {
                    long member = SqlExecutor.executeInt(ctx, conn, sql, 0L);
                    memberIds.add(member);
                } else {
                    RecordSet recs = SqlExecutor.executeRecords(ctx, conn, sql, null);
                    for (Record rec : recs) {
                        memberIds.add(rec.getInt("member"));
                    }
                }
                return null;
            }
        });

        return CollectionsHelper.toLongArray(memberIds);
    }

    @Override
    public long[] getAdmins(Context ctx, long groupId, Page page) {
        return getMembersByRole(ctx, groupId, ROLE_ADMIN, page);
    }

    @Override
    public long getCreator(Context ctx, long groupId) {
        long[] memberIds = getMembersByRole(ctx, groupId, ROLE_CREATOR, Page.of(1));
        return ArrayUtils.isNotEmpty(memberIds) ? memberIds[0] : 0L;
    }

    @Override
    public long[] getAllMembers(Context ctx, long groupId, Page page) {
        return getMembersByRole(ctx, groupId, 0, page);
    }

    @Override
    public long[] getMembers(final Context ctx, final long[] groupIds, final Page page) {
        final ShardResult memberSR = shardGroupMembers();
        final ArrayList<Long> memberIds = new ArrayList<Long>();
        sqlExecutor.openConnection(memberSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = "SELECT member FROM " + memberSR.table + " WHERE group_id IN (" + StringHelper.join(groupIds, ",") + ") AND destroyed_time=0";

                sql += SQLTemplate.merge(" ${limit}",
                        "limit", Page.toLimit((int)page.page, (int)page.count));

                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        while (rs.next())
                            memberIds.add(rs.getLong("member"));
                    }
                });
                return null;
            }
        });
        return CollectionsHelper.toLongArray(memberIds);
    }

    @Override
    public int getMembersCount(final Context ctx, final long groupId) {
        final ShardResult memberSR = shardGroupMembers();
        return sqlExecutor.openConnection(memberSR.db, new SingleConnectionHandler<Integer>() {
            @Override
            protected Integer handleConnection(Connection conn) {
                String sql = "SELECT count(*) FROM " + memberSR.table + " WHERE group_id=" + groupId + " AND destroyed_time=0";
                return (int)SqlExecutor.executeInt(ctx, conn, sql, 0L);
            }
        });
    }

    @Override
    public Map<Long, Integer> getMembersCounts(final Context ctx, final long[] groupIds) {
        final ShardResult memberSR = shardGroupMembers();
        final HashMap<Long, Integer> memberCounts = new HashMap<Long, Integer>();
        sqlExecutor.openConnection(memberSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = "SELECT group_id, count(*) AS count FROM " + memberSR.table + " WHERE group_id in (" + StringHelper.join(groupIds, ",") + ") AND destroyed_time=0 group by group_id";
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        while (rs.next())
                            memberCounts.put(rs.getLong("group_id"), rs.getInt("count"));
                    }
                });
                return null;
            }
        });
        return memberCounts;
    }

    @Override
    public boolean hasRight(final Context ctx, final long groupId, final long member, final int minRole) {
        final ShardResult memberSR = shardGroupMembers();
        return sqlExecutor.openConnection(memberSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = "SELECT role FROM " + memberSR.table + " WHERE group_id=" + groupId + " AND member=" + member + " AND destroyed_time=0";
                int role = (int)SqlExecutor.executeInt(ctx, conn, sql, 0);
                return role >= minRole;
            }
        });
    }

    @Override
    public boolean addOrUpdatePendings(final Context ctx, final long groupId, final List<GroupPending> statuses) {
        final ShardResult pendingSR = shardPending();
        return sqlExecutor.openConnection(pendingSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                ArrayList<String> sqls = new ArrayList<String>();
                long now = DateHelper.nowMillis();
                for (GroupPending status : statuses) {
                    String sql = "INSERT INTO " + pendingSR.table + " (group_id, user_id, display_name, identify, source, status, created_time, updated_time, destroyed_time) VALUES ";
                    long userId = status.getUserId();
                    String displayName = status.getDisplayName();
                    String identify = status.getIdentify();
                    long source = status.getSource();
                    int status_ = status.getStatus();
                    long destroyedTime = 0;
                    if ((status_ == STATUS_JOINED) || (status_ == STATUS_REJECTED)
                            || (status_ == STATUS_KICKED) || (status_ == STATUS_QUIT))
                        destroyedTime = now;
                    sql += "(" + groupId + ", " + userId + ", '" + displayName + "', '" + identify + "', " + source + ", " + status_ + ", " + now + ", " + now + ", " + destroyedTime + ")";
                    sql += " ON DUPLICATE KEY UPDATE status=VALUES(status), updated_time=VALUES(updated_time), destroyed_time=VALUES(destroyed_time)";
                    sqls.add(sql);

                    sql = "UPDATE " + pendingSR.table + " SET status=" + status_ + ", updated_time=" + now + ", destroyed_time=" + destroyedTime
                            + " WHERE group_id=" + groupId;
                    if (userId != 0)
                        sql += " AND user_id=" + userId;
                    else
                        sql += " AND identify='" + identify + "'";
                    sqls.add(sql);
                }
                long n = SqlExecutor.executeUpdate(ctx, conn, sqls);
                return n > 0;
            }
        });
    }

    private static GroupPending readPending(ResultSet rs) throws SQLException {
        GroupPending pending = new GroupPending();
        pending.setGroupId(rs.getLong("group_id"));
        pending.setUserId(rs.getLong("user_id"));
        pending.setIdentify(rs.getString("identify"));
        pending.setSource(rs.getLong("source"));
        pending.setDisplayName(rs.getString("display_name"));
        pending.setStatus(rs.getInt("status"));
        pending.setCreatedTime(rs.getLong("created_time"));
        pending.setUpdatedTime(rs.getLong("updated_time"));
        pending.setDestroyedTime(rs.getLong("destroyed_time"));
        return pending;
    }

    @Override
    public GroupPendings getPendingUsersByStatus(final Context ctx, final long groupId, final long source, final String status, final Page page) {
        final ShardResult pendingSR = shardPending();
        final GroupPendings pendings = new GroupPendings();
        sqlExecutor.openConnection(pendingSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = "SELECT * FROM " + pendingSR.table + " WHERE group_id=" + groupId;

                if (StringUtils.isNotBlank(status))
                    sql += " AND status IN (" + status + ")";

                if (source > 0) {
                    sql += " AND source=" + source;
                }

                sql += SQLTemplate.merge(" ${limit}",
                        "limit", Page.toLimit((int) page.page, (int) page.count));

                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        while (rs.next()) {
                            GroupPending pending = readPending(rs);
                            pendings.add(pending);
                        }
                    }
                });
                return null;
            }
        });
        return pendings;
    }

    @Override
    public int getUserStatusById(final Context ctx, final long groupId, final long userId) {
        final ShardResult pendingSR = shardPending();
        return sqlExecutor.openConnection(pendingSR.db, new SingleConnectionHandler<Integer>() {
            @Override
            protected Integer handleConnection(Connection conn) {
                String sql = "SELECT status FROM " + pendingSR.table + " WHERE user_id=" + userId + " AND group_id=" + groupId;
                return (int)SqlExecutor.executeInt(ctx, conn, sql, STATUS_NONE);
            }
        });
    }

    @Override
    public int getUserStatusByIdentify(final Context ctx, final long groupId, final String identify) {
        final ShardResult pendingSR = shardPending();
        return sqlExecutor.openConnection(pendingSR.db, new SingleConnectionHandler<Integer>() {
            @Override
            protected Integer handleConnection(Connection conn) {
                String sql = "SELECT status FROM " + pendingSR.table + " WHERE identify='" + identify + "' AND group_id=" + groupId;
                return (int) SqlExecutor.executeInt(ctx, conn, sql, STATUS_NONE);
            }
        });
    }

    @Override
    public Map<Long, Integer> getUserStatusByIds(final Context ctx, final long groupId, final long[] userIds) {
        final HashMap<Long, Integer> m = new HashMap<Long, Integer>();

        final ShardResult pendingSR = shardPending();
        sqlExecutor.openConnection(pendingSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                if (ArrayUtils.isNotEmpty(userIds)) {
                    String sql = "SELECT user_id, status FROM " + pendingSR.table + " WHERE user_id IN (" + StringHelper.join(userIds, ",") + ") AND group_id=" + groupId;
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            while (rs.next()) {
                                long userId = rs.getLong("user_id");
                                int status = rs.getInt("status");
                                if (!m.containsKey(userId))
                                    m.put(userId, status);
                            }
                        }
                    });
                }
                return null;
            }
        });
        return m;
    }

    @Override
    public Map<String, Integer> getUserStatusByIdentifies(final Context ctx, final long groupId, final String[] identifies) {
        final HashMap<String, Integer> m = new HashMap<String, Integer>();

        final ShardResult pendingSR = shardPending();
        sqlExecutor.openConnection(pendingSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                if (ArrayUtils.isNotEmpty(identifies)) {
                    String sql = "SELECT identify, status FROM " + pendingSR.table + " WHERE identify IN (" + Sql.joinSqlValues(identifies, ",") + ") AND group_id=" + groupId;
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            while (rs.next()) {
                                String identify = rs.getString("identify");
                                int status = rs.getInt("status");
                                if (!m.containsKey(identify))
                                    m.put(identify, status);
                            }
                        }
                    });
                }
                return null;
            }
        });
        return m;
    }

    @Override
    public Map<Long, Integer> getUsersCounts(final Context ctx, final long[] groupIds, final int status) {
        final HashMap<Long, Integer> m = new HashMap<Long, Integer>();

        final ShardResult pendingSR = shardPending();
        sqlExecutor.openConnection(pendingSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql1 = "SELECT group_id, count(DISTINCT user_id) AS count FROM " + pendingSR.table + " WHERE group_id in (" + StringHelper.join(groupIds, ",") + ") AND status="
                        + status + " AND user_id <> 0 group by group_id";
                String sql2 = "SELECT group_id, count(DISTINCT identify) AS count FROM " + pendingSR.table + " WHERE group_id in (" + StringHelper.join(groupIds, ",") + ") AND status="
                        + status + " AND identify <> '' AND user_id=0 group by group_id";

                SqlExecutor.executeCustom(ctx, conn, sql1, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        while (rs.next()) {
                            m.put(rs.getLong("group_id"), rs.getInt("count"));
                        }
                    }
                });

                SqlExecutor.executeCustom(ctx, conn, sql2, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        while (rs.next()) {
                            long groupId = rs.getLong("group_id");
                            int count = rs.getInt("count");
                            if (m.containsKey(groupId)) {
                                m.put(groupId, count + m.get(groupId));
                            } else {
                                m.put(groupId, count);
                            }
                        }
                    }
                });
                return null;
            }
        });
        return m;
    }

    @Override
    public boolean updateUserIdByIdentify(final Context ctx, final long userId, final String identify) {
        final ShardResult pendingSR = shardPending();
        return sqlExecutor.openConnection(pendingSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = "UPDATE " + pendingSR.table + " SET user_id=" + userId + " WHERE identify='" + identify + "'";
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }

    @Override
    public boolean isGroup(Context ctx, long id) {
        return id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END;
    }

    @Override
    public boolean isPublicCircle(Context ctx, long id) {
        return id >= PUBLIC_CIRCLE_ID_BEGIN && id < ACTIVITY_ID_BEGIN;
    }

    @Override
    public boolean isActivity(Context ctx, long id) {
        return id >= ACTIVITY_ID_BEGIN && id < ORGANIZATION_ID_BEGIN;
    }

    @Override
    public boolean isOrganization(Context ctx, long id) {
        return id >= ORGANIZATION_ID_BEGIN && id < GENERAL_GROUP_ID_BEGIN;
    }

    @Override
    public boolean isGeneralGroup(Context ctx, long id) {
        return id >= GENERAL_GROUP_ID_BEGIN && id < GENERAL_GROUP_ID_END;
    }
}
