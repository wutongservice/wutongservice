package com.borqs.server.wutong.friendship;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.*;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.log.TraceCall;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.page.PageLogicUtils;
import com.borqs.server.wutong.request.RequestLogic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.IOException;
import java.util.*;

import static com.borqs.server.wutong.Constants.*;

public class FriendshipImpl implements FriendshipLogic, Initializable {
    private static final Logger L = Logger.getLogger(FriendshipImpl.class);

    protected final Schema friendSchema = Schema.loadClassPath(FriendshipImpl.class, "friend.schema");
    protected final Schema circleSchema = Schema.loadClassPath(FriendshipImpl.class, "circle.schema");
    protected final Schema nameRemarkSchema = Schema.loadClassPath(FriendshipImpl.class, "name_remark.schema");

    private ConnectionFactory connectionFactory;
    private String db;
    private String friendTable;
    private String circleTable;
    private String nameRemarkTable;
    private String qiupuUid;

    public FriendshipImpl() {
    }

    @Override
    public void init() {
        Configuration conf = GlobalConfig.get();
        friendSchema.loadAliases(conf.getString("schema.friend.alias", null));
        circleSchema.loadAliases(conf.getString("schema.circle.alias", null));
        nameRemarkSchema.loadAliases(conf.getString("schema.name_remark.alias", null));

        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("friendship.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("friendship.simple.db", null);
        this.friendTable = conf.getString("friendship.simple.friendTable", "friend");
        this.circleTable = conf.getString("friendship.simple.circleTable", "circle");
        this.nameRemarkTable = conf.getString("friendship.simple.circleTable", "name_remark");
        this.qiupuUid = conf.getString("qiupu.uid", "102");
    }

    @Override
    public void destroy() {
        this.friendTable = null;
        this.circleTable = null;
        this.nameRemarkTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    private static String toStr(Object s) {
        return ObjectUtils.toString(s);
    }

    protected boolean saveCircle(Record circle0) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, circle)}";

        long now = DateUtils.nowMillis();
        circle0.put("created_time", now);
        circle0.put("updated_time", now);
        circle0.put("member_count", 0);

        String sql = SQLTemplate.merge(SQL,
                "table", circleTable, "alias", circleSchema.getAllAliases(),
                "circle", circle0);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    public boolean createBuiltinCircles(Context ctx, String userId) {
        // final String METHOD_NAME = "createBuiltinCircles";
        // L.traceStartCall(ctx, METHOD_NAME, userId);
        try {
            String userId0 = toStr(userId);
            saveCircle(Record.of("user", userId0, "name", "Blocked", "circle", BLOCKED_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Default", "circle", DEFAULT_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Address Book", "circle", ADDRESS_BOOK_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Family", "circle", FAMILY_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Closed Friends", "circle", CLOSE_FRIENDS_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Acquaintance", "circle", ACQUAINTANCE_CIRCLE));
            // L.traceEndCall(ctx, METHOD_NAME);
            return true;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean hasCircleName(String userId, String circleName) {
        final String SQL = "SELECT count(*) FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.name}=${v(name)}";
        String sql = SQLTemplate.merge(SQL,
                "table", circleTable,
                "alias", circleSchema.getAllAliases(),
                "user", userId,
                "name", circleName);

        SQLExecutor se = getSqlExecutor();
        return se.executeIntScalar(sql, 0) > 0;
    }

    protected int getCircleCount(String userId) {
        final String SQL = "SELECT count(*) FROM ${table} WHERE ${alias.user}=${v(user)}";
        String sql = SQLTemplate.merge(SQL,
                "table", circleTable,
                "alias", circleSchema.getAllAliases(),
                "user", userId);

        SQLExecutor se = getSqlExecutor();
        return (int) se.executeIntScalar(sql, 0);
    }

    protected int generateCustomCircleId(String userId) {
        final String SQL = "SELECT max(${alias.circle}) FROM ${table} WHERE ${alias.user}=${v(user)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", circleSchema.getAllAliases()},
                {"table", circleTable},
                {"user", userId},
        });

        SQLExecutor se = getSqlExecutor();
        int maxCircleIdObj = (int) se.executeIntScalar(sql, 0);
        if (maxCircleIdObj == 0 || maxCircleIdObj < 100)
            return 101;


        int maxCircleId = ((Number) maxCircleIdObj).intValue();
        if (maxCircleId < Short.MAX_VALUE - 1)
            return maxCircleId + 1;

        sql = new SQLBuilder.Select(circleSchema).select("circle")
                .from(circleTable)
                .where("${alias.user}=${v(user)}", "user", userId)
                .orderBy("circle", "ASC")
                .toString();

        RecordSet circleIds = se.executeRecordSet(sql, null);
        HashSet<Long> circleIdSet = new HashSet<Long>(circleIds.getIntColumnValues("circle"));
        for (long i = 100; i < Short.MAX_VALUE; i++) {
            if (!circleIdSet.contains(i))
                return (int) i;
        }

        throw new ServerException(WutongErrors.FRIEND_GENERATE_CIRCLE_ID, "Generate circle id error");
    }

    @TraceCall
    @Override
    public String createCircle(Context ctx, String userId, String name) {
        // final String METHOD_NAME = "createCircle";
        // L.traceStartCall(ctx, METHOD_NAME, userId, name);
        try {
            Validate.isTrue(StringUtils.isNotBlank(name) && name.length() < 20);

            String userId0 = toStr(userId);
            String name0 = toStr(name);
            if (hasCircleName(userId0, name0))
                throw new ServerException(WutongErrors.FRIEND_CIRCLE_EXISTS, "Circle name is exist (%s)", name0);

            int circleCount = getCircleCount(userId0);
            if (circleCount >= 100)
                throw new ServerException(WutongErrors.FRIEND_TOO_MANY_CIRCLES, "Too many circle");

            int circleId = generateCustomCircleId(userId0);
            Record circle = Record.of("user", userId0, "name", name, "circle", circleId);
            Schemas.standardize(circleSchema, circle);
            boolean b = saveCircle(circle);
            if (!b)
                throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "Save circle error");

            // L.traceEndCall(ctx, METHOD_NAME);
            return toStr(circleId);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean destroyCircles0(String userId, String... circleIds) {
        String sql1 = new SQLBuilder.Delete(friendSchema)
                .deleteFrom(friendTable)
                .where("${alias.user}=${v(user)} AND ${alias.circle} IN (${circles})",
                        "user", Long.parseLong(userId), "circles", StringUtils.join(circleIds, ","))
                .toString();

        final String SQL = "delete from ${table} WHERE ${alias.user}=${v(user)} AND ${alias.circle} IN (${circle_ids})";
        String sql2 = SQLTemplate.merge(SQL,
                "table", circleTable,
                "alias", circleSchema.getAllAliases(),
                "user", userId,
                "circle_ids", StringUtils.join(circleIds, ","));

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(Arrays.asList(sql1, sql2));
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean destroyCircles(Context ctx, String userId, String circleIds) {
        try {
            String[] circlesId0 = StringUtils2.splitArray(toStr(circleIds), ",", true);
            for (String circleId : circlesId0) {
                if (Integer.parseInt(circleId) < 100)
                    return false;
            }
            return circlesId0.length == 0 || destroyCircles0(toStr(userId), circlesId0);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean updateCircleName0(String userId, String circleId, String name) {
        final String SQL = "UPDATE ${table} SET ${alias.name}=${v(name)},${alias.updated_time}=${v(updated_time)} WHERE ${alias.user}=${v(user)} AND ${alias.circle}=${v(circle)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", circleSchema.getAllAliases()},
                {"name", name},
                {"table", circleTable},
                {"circle", Integer.parseInt(circleId)},
                {"updated_time", DateUtils.nowMillis()},
                {"user", userId},
        });

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean updateCircleName(Context ctx, String userId, String circleId, String name) {
        // final String METHOD_NAME = "updateCircleName";
        // L.traceStartCall(ctx, METHOD_NAME, userId, circleId, name);
        try {
            String userId0 = toStr(userId);
            String name0 = toStr(name);
            if (hasCircleName(userId0, name0))
                throw new ServerException(WutongErrors.FRIEND_CIRCLE_EXISTS, "Circle name is exist (%s)", name0);

            // L.traceEndCall(ctx, METHOD_NAME);
            return updateCircleName0(userId0, toStr(circleId), name0);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public boolean updateCircleMemberCount(Context ctx, String userId, String circleId, int member_count) {
        // final String METHOD_NAME = "updateCircleMemberCount";
        // L.traceStartCall(ctx, METHOD_NAME, userId, circleId, member_count);
        try {
            final String SQL = "UPDATE ${table} SET ${alias.member_count}=${v(member_count)} WHERE ${alias.user}=${v(user)} AND ${alias.circle}=${v(circle)}";
            String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", circleSchema.getAllAliases()},
                    {"table", circleTable},
                    {"circle", Integer.parseInt(circleId)},
                    {"member_count", member_count},
                    {"user", userId},
            });

            SQLExecutor se = getSqlExecutor();
            long n = se.executeUpdate(sql);
            // L.traceEndCall(ctx, METHOD_NAME);
            return n > 0;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected int getMemberCount0(String userId, int circleId) {
        String sql;
        if (circleId == FRIENDS_CIRCLE) {
            final String SQL = "SELECT sum(${alias.friend}) FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.circle} <> ${v(blocked_circle)}" +
                    " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + "" +
                    " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + "" +
                    " GROUP BY ${alias.friend}";
            sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", friendTable},
                    {"alias", friendSchema.getAllAliases()},
                    {"user", userId},
                    {"blocked_circle", BLOCKED_CIRCLE},
            });
        } else {
            sql = "select count(*) from friend where user=" + userId + " AND circle=" + circleId + "" +
                    " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                    + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + "";
        }

        SQLExecutor se = getSqlExecutor();
        return (int) se.executeIntScalar(sql, 0);
    }

    @TraceCall
    @Override
    public boolean updateMyCircleMemberCount(Context ctx, String userId, String circleId) {
        // final String METHOD_NAME = "updateMyCircleMemberCount";
        // L.traceStartCall(ctx, METHOD_NAME, userId, circleId);
        try {
            SQLExecutor se = getSqlExecutor();
            long n = 0;
            if (circleId.equals("")) {
                //update all my circle member_count
                String sql = "select circle from circle where user=" + userId;
                RecordSet recs = se.executeRecordSet(sql, null);
                for (Record r : recs) {
                    int c = getMemberCount0(userId, (int) r.getInt("circle"));
                    sql = "update circle set member_count=" + c + " where circle=" + r.getInt("circle") + " and user=" + userId;
                    n = se.executeUpdate(sql);
                }
            } else {
                int c = getMemberCount0(userId, Integer.parseInt(circleId));
                String sql = "update circle set member_count=" + c + " where circle=" + circleId + " and user=" + userId;
                n = se.executeUpdate(sql);
            }
            // L.traceEndCall(ctx, METHOD_NAME);
            return n > 0;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getCircles0(String userId, List<String> circleIds, boolean withMembers) {
        String sql;
        if (circleIds.isEmpty()) {
            final String SQL = "SELECT ${alias.circle} AS 'circle_id', ${alias.name} AS 'circle_name', ${alias.member_count} AS 'member_count', ${alias.updated_time} AS 'updated_time' FROM ${table}"
                    + " WHERE ${alias.user}=${v(user)} ORDER BY ${alias.circle} ASC";

            sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", circleSchema.getAllAliases()},
                    {"table", circleTable},
                    {"user", userId},
            });
        } else {
            ArrayList<String> circleIds2 = new ArrayList<String>(circleIds);
            circleIds2.remove(Integer.toString(FRIENDS_CIRCLE));
            circleIds2.remove(Integer.toString(FOLLOWERS_CIRCLE));

            if (circleIds2.isEmpty())
                return new RecordSet();

            final String SQL = "SELECT ${alias.circle} AS 'circle_id', ${alias.name} AS 'circle_name', ${alias.member_count} AS 'member_count', ${alias.updated_time} AS 'updated_time' FROM ${table}"
                    + " WHERE ${alias.user}=${v(user)} AND ${alias.circle} IN (" + StringUtils.join(circleIds2, ",") + ") ORDER BY ${alias.circle} ASC";

            sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", circleSchema.getAllAliases()},
                    {"table", circleTable},
                    {"user", userId},
            });
        }

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        for (Record r : recs) {
            r.put("member_count", getMemberCount0(userId, (int) r.getInt("circle_id")));
        }

        Schemas.standardize(circleSchema, recs);
        if (withMembers) {
            final String SQL = "SELECT ${alias.friend} AS 'friend' FROM ${table}"
                    + " WHERE ${alias.user}=${v(user_id)} AND ${alias.circle}=${v(circle_id)} AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + "";
            final Map<String, String> friendTableAliases = friendSchema.getAllAliases();
            for (Record rec : recs) {
                long circleId = rec.getInt("circle_id");
                sql = SQLTemplate.merge(SQL, new Object[][]{
                        {"table", friendTable},
                        {"alias", friendTableAliases},
                        {"user_id", Long.parseLong(userId)},
                        {"circle_id", circleId},
                });
                RecordSet friendSet = se.executeRecordSet(sql, null);
                rec.put("members", friendSet.joinColumnValues("friend", ","));
            }
        }

        // TODO: add friend/follower circle count
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getCircles(Context ctx, String userId, String circleIds, boolean withMembers) {
        // final String METHOD_NAME = "getCircles";
        // L.traceStartCall(ctx, METHOD_NAME, userId, circleIds, withMembers);
        try {
            List<String> circlesIds0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            for (String circleId : circlesIds0) {
                if (!isFiniteCircle(Integer.parseInt(circleId)))
                    throw new ServerException(WutongErrors.FRIEND_ILLEGAL_CIRCLE, "Invalidate circle ids");
            }


            RecordSet recs = getCircles0(toStr(userId), circlesIds0, withMembers);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean deleteVirtualFriend0(String userId, String friendId, String name, String content) {
        SQLExecutor se = getSqlExecutor();
        String sql = "delete from friend where type=1 and friend='" + friendId + "' and user='" + userId + "' and content='" + content + "' and name='" + name + "'";
        return se.executeUpdate(sql) > 0;
    }

    protected RecordSet getExistVirtualFriendId0(String userId, String virtualFriendId) {
        SQLExecutor se = getSqlExecutor();
        String sql = "select * from friend where type=1 and user='" + userId + "' and friend='" + virtualFriendId + "'";
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    protected List<String> ifExistContactInMyCircle0(String userId, String content) {
        List<String> ll = StringUtils2.splitList(toStr(content), ",", true);
        String sql = "";
        SQLExecutor se = getSqlExecutor();
        ArrayList<String> outCircle = new ArrayList<String>();
        sql = "select circle from friend where type=1 and user='" + userId + "' and content='" + ll.get(0) + "'";
        RecordSet recs = se.executeRecordSet(sql, null);
        if (recs.size() > 0) {
            for (Record r : recs) {
                if (!outCircle.contains(r.getString("circle"))) {
                    outCircle.add(r.getString("circle"));
                }
            }
        }
        return outCircle;
    }

    protected boolean setContactFriend0(String userId, String friendId, String fname, String content, String circleId, int reason, boolean isadd) {
        SQLExecutor se = getSqlExecutor();
        ArrayList<String> sqls = new ArrayList<String>();
        try {
            List<String> inCircle = ifExistContactInMyCircle0(userId, content);
            if (isadd) {
                if (!inCircle.contains(circleId)) {
                    String s = "INSERT INTO friend (user,friend,circle,created_time,reason,type,name,content) values ('" + userId + "','" + friendId + "','" + circleId + "'," + DateUtils.nowMillis() + "," + reason + ",'1','" + fname + "','" + content + "')";
                    sqls.add(s);
//                    sqls.add("update circle set member_count=member_count+1 where user='"+userId+"' and circle='"+circleId+"'");
                }
            } else {
                if (inCircle.contains(circleId)) {
                    sqls.add("delete from friend where user='" + userId + "' and type=1 and circle='" + circleId + "' and content='" + content + "'");
//                    sqls.add("update circleId set member_count=member_count-1 where user='"+userId+"' and circle='"+circleId+"'");
                }
            }
            se.executeUpdate(sqls);
        } finally {
        }
        return true;
    }

    protected boolean deleteVirtualFriendId0(String friendIds, String content) {
        SQLExecutor se = getSqlExecutor();
        String sql = "delete from virtual_friendid where virtual_friendid in (" + friendIds + ") and content='" + content + "'";
        return se.executeUpdate(sql) > 0;
    }

    @TraceCall
    @Override
    public boolean setContactFriend(Context ctx, String userId, String friendId, String fname, String content, String circleIds, int reason, boolean isadd, boolean deleteOld) {
        // final String METHOD_NAME = "setContactFriend";
        // L.traceStartCall(ctx, METHOD_NAME, userId, friendId, fname, content, circleIds, reason, isadd, deleteOld);
        try {
            if (deleteOld) {
                deleteVirtualFriend0(toStr(userId), toStr(friendId), toStr(fname), toStr(content));
            }
            List<String> ll = StringUtils2.splitList(toStr(circleIds), ",", true);
            for (String l : ll) {
                setContactFriend0(toStr(userId), toStr(friendId), toStr(fname), toStr(content), toStr(l), reason, isadd);
            }
            RecordSet recs = getExistVirtualFriendId0(toStr(userId), toStr(friendId));
            if (recs.size() == 0) {
                deleteVirtualFriendId0(toStr(friendId), toStr(content));
            }

            // L.traceEndCall(ctx, METHOD_NAME);
            return true;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public boolean getIfHeInMyCircles(Context ctx, String my_id, String other_id, String circle_id) {
        // final String METHOD_NAME = "getIfHeInMyCircles";
        // L.traceStartCall(ctx, METHOD_NAME, my_id, other_id, circle_id);
        try {
            String sql = "select * from friend where user='" + my_id + "' ";
            sql += " and friend='" + other_id + "' ";
            sql += " and circle='" + circle_id + "' ";
            sql += " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ";
            sql += " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ";

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs.size() > 0;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean setFriend0(String userId, String friendId, String circleId, int reason, boolean isadd) {
        SQLExecutor se = getSqlExecutor();
        try {
            if (userId.equals(friendId)) {
                return true;
            }
            ArrayList<String> sqls = new ArrayList<String>();
            if (!isadd) {
                if (Integer.valueOf(circleId) == ADDRESS_BOOK_CIRCLE) {
                    sqls.add("UPDATE friend set reason=" + FRIEND_REASON_SOCIALCONTACT_DELETE + " where user=" + userId + " and friend=" + friendId + " and circle=" + ADDRESS_BOOK_CIRCLE + "");
                } else {
                    String thisSql = "select circle from friend where user=" + userId + " and friend=" + friendId + ""
                            + " AND circle<>" + BLOCKED_CIRCLE + ""
                            + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                            + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " "
                            + " AND circle<>" + ADDRESS_BOOK_CIRCLE + "";
                    SQLExecutor se1 = getSqlExecutor();
                    RecordSet recs = se1.executeRecordSet(thisSql, null);
                    if (recs.size() == 1) {
                        sqls.add("UPDATE friend set reason=" + FRIEND_REASON_SOCIALCONTACT_DELETE + " where user=" + userId + " and friend=" + friendId + " and circle=" + ADDRESS_BOOK_CIRCLE + "");
                    }
                    if (Integer.valueOf(circleId) == DEFAULT_CIRCLE) {
                        sqls.add("UPDATE friend set reason=" + FRIEND_REASON_DEFAULT_DELETE + " where user=" + userId + " and friend=" + friendId + " and circle=" + DEFAULT_CIRCLE + "");
                    }

                    if (Integer.valueOf(circleId) != ADDRESS_BOOK_CIRCLE && Integer.valueOf(circleId) != DEFAULT_CIRCLE) {
                        //if the lasted circle not Constants.ADDRESS_BOOK_CIRCLE, Constants.DEFAULT_CIRCLE，if yes ,delete from Constants.ADDRESS_BOOK_CIRCLE both
                        sqls.add("DELETE FROM friend where user=" + userId + " and friend=" + friendId + " and circle=" + circleId + "");
                    }
                }

                //history
                long acted_time = DateUtils.nowMillis();
                sqls.add("INSERT INTO history VALUES(" + userId + ", 'delete_user_" + friendId + "', " + acted_time + ") ON DUPLICATE KEY UPDATE acted_time=" + acted_time);
            } else {
                if (Integer.valueOf(circleId) == DEFAULT_CIRCLE || Integer.valueOf(circleId) == ADDRESS_BOOK_CIRCLE) {
                    int this_circleId = 0;
                    int this_delete_reason = 0;
                    if (Integer.valueOf(circleId) == ADDRESS_BOOK_CIRCLE) {
                        this_circleId = ADDRESS_BOOK_CIRCLE;
                        this_delete_reason = FRIEND_REASON_SOCIALCONTACT_DELETE;
                        sqls.add("UPDATE request SET status=1, done_time=" + DateUtils.nowMillis() + " where user=" + userId + " and source=" + friendId + " and status=0");
                    }
                    if (Integer.valueOf(circleId) == DEFAULT_CIRCLE) {
                        this_circleId = DEFAULT_CIRCLE;
                        this_delete_reason = FRIEND_REASON_DEFAULT_DELETE;
                    }
                    String thisSql = "select circle from friend where user=" + userId + " and friend=" + friendId + " and circle=" + this_circleId + " AND reason=" + this_delete_reason + "";
                    SQLExecutor se1 = getSqlExecutor();
                    RecordSet recs = se1.executeRecordSet(thisSql, null);
                    if (recs.size() >= 1) {
                        if (reason == FRIEND_REASON_MANUALSELECT) {
                            sqls.add("UPDATE friend set reason=" + FRIEND_REASON_MANUALSELECT + ",created_time=" + DateUtils.nowMillis() + " where user=" + userId + " and friend=" + friendId + " and circle=" + this_circleId + " AND reason=" + this_delete_reason + "");
                        }
                    } else {
                        sqls.add("INSERT INTO  friend (user,friend,circle,created_time,reason) values (" + userId + "," + friendId + "," + circleId + "," + DateUtils.nowMillis() + "," + reason + ")  ON DUPLICATE KEY update created_time=" + DateUtils.nowMillis() + "");
                    }
                }

                if (Integer.valueOf(circleId) != ADDRESS_BOOK_CIRCLE && Integer.valueOf(circleId) != DEFAULT_CIRCLE) {
                    if (Integer.valueOf(circleId) == BLOCKED_CIRCLE) {
                        sqls.add("delete from friend where user=" + userId + " and friend=" + friendId + " and circle<>" + BLOCKED_CIRCLE + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + "");
                    }
                    sqls.add("INSERT INTO friend (user,friend,circle,created_time,reason) values (" + userId + "," + friendId + "," + circleId + "," + DateUtils.nowMillis() + "," + reason + ")  ON DUPLICATE KEY update created_time=" + DateUtils.nowMillis() + "");
                }

                sqls.add("delete from suggested_user where user=" + userId + " and suggested=" + friendId + "");
            }
            se.executeUpdate(sqls);
        } finally {
        }
        return true;
    }

    protected RecordSet getRelation0(String sourceUserId, String targetUserId, String circleId) {
        String sql;
        if (StringUtils.isBlank(circleId)) {
            sql = new SQLBuilder.Select(friendSchema)
                    .select("circle")
                    .from(friendTable)
                    .where("${alias.user}=${v(target)} AND ${alias.friend}=${v(source)}"
                            + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                            + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ",
                            "target", Long.parseLong(targetUserId), "source", Long.parseLong(sourceUserId))
                    .toString();
        } else if (Integer.parseInt(circleId) == FRIENDS_CIRCLE) {
            sql = new SQLBuilder.Select(friendSchema)
                    .select("circle")
                    .from(friendTable)
                    .where("${alias.user}=${v(target)} AND ${alias.friend}=${v(source)} AND ${alias.circle}<>${v(blocked_circle)}"
                            + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                            + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ",
                            "target", Long.parseLong(targetUserId), "source", Long.parseLong(sourceUserId), "blocked_circle", BLOCKED_CIRCLE)
                    .toString();
        } else {
            sql = new SQLBuilder.Select(friendSchema)
                    .select("circle")
                    .from(friendTable)
                    .where("${alias.user}=${v(target)} AND ${alias.friend}=${v(source)} AND ${alias.circle}=${v(circle)}"
                            + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                            + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ",
                            "target", Long.parseLong(targetUserId), "source", Long.parseLong(sourceUserId), "circle", Integer.parseInt(circleId))
                    .toString();
        }

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(friendSchema, recs);
        if (recs.isEmpty())
            return recs;

        String targetCircleIds = recs.joinColumnValues("circle", ",");
        sql = new SQLBuilder.Select(circleSchema)
                .select("circle", "name", "updated_time")
                .from(circleTable)
                .where("${alias.user}=${v(user)} AND ${alias.circle} IN (${circles})",
                        "user", Long.parseLong(targetUserId), "circles", targetCircleIds)
                .orderBy("circle", "ASC")
                .toString();
        recs = se.executeRecordSet(sql, null);
        Schemas.standardize(circleSchema, recs);
        recs.renameColumn("circle", "circle_id");
        recs.renameColumn("name", "circle_name");
        recs.renameColumn("updated_time", "circle_updated_time");
        return recs;
    }

    @TraceCall
    @Override
    public boolean setFriend(Context ctx, String userId, String friendId, String circleIds, int reason) {
        // final String METHOD_NAME = "setFriend";
        // L.traceStartCall(ctx, METHOD_NAME, userId, friendId, circleIds, reason);
        try {
            String userId0 = toStr(userId);

            List<String> circleIds0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (circleIds0.contains(Integer.toString(BLOCKED_CIRCLE))) {
                circleIds0.retainAll(Arrays.asList(Integer.toString(BLOCKED_CIRCLE)));
            }

            for (String circleId : circleIds0) {
                if (!isActualCircle(Integer.parseInt(circleId)) || !hasActualCircle(userId0, circleId)) {
                    throw new ServerException(WutongErrors.FRIEND_ILLEGAL_CIRCLE, "Circle id error (%s)", circleId);
                }
            }

            String friendId0 = toStr(friendId);
            if (StringUtils.isBlank(friendId0)) {
                return true;
            }

            //find this guy in my old circles,then delete
            RecordSet recsf = getRelation0(toStr(friendId), toStr(userId), "");
            for (Record r : recsf) {
                setFriend0(toStr(userId), toStr(friendId), r.getString("circle_id"), reason, false);
            }

            for (String l : circleIds0) {
                setFriend0(userId0, friendId0, l, reason, true);
            }
//            boolean b = updateMyCircleMemberCount0(toStr(userId),"");
//            return b;
            // L.traceEndCall(ctx, METHOD_NAME);
            return true;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean hasActualCircle(String userId, String circleId) {
        String sql = new SQLBuilder.Select(circleSchema)
                .select("circle")
                .from(circleTable)
                .where("${alias.user}=${v(user)} AND ${alias.circle}=${v(circle)}",
                        "user", Long.parseLong(userId), "circle", Integer.parseInt(circleId))
                .toString();
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return !recs.isEmpty();
    }

    @TraceCall
    @Override
    public boolean setFriends(Context ctx, String userId, String friendId, String circleId, int reason, boolean isadd) {
        // final String METHOD_NAME = "setFriends";
        // L.traceStartCall(ctx, METHOD_NAME, userId, friendId, circleId, reason, isadd);
        try {
            String userId0 = toStr(userId);
            String circleId0 = toStr(circleId);
            if (!circleId0.equals("")) {
                if (!isActualCircle(Integer.parseInt(circleId0)) || !hasActualCircle(userId0, circleId0)) {
                    throw new ServerException(WutongErrors.FRIEND_ILLEGAL_CIRCLE, "Circle id error (%s)", circleId);
                }
                if (!isadd) {
                    if (Integer.parseInt(circleId0) == BLOCKED_CIRCLE) {
                        List<String> circleIds0 = new ArrayList<String>();
                        circleIds0.add(circleId0);
                        circleIds0.retainAll(Arrays.asList(Integer.toString(BLOCKED_CIRCLE)));
                    }
                }
            }
            String friendId0 = toStr(friendId);
            if (StringUtils.isBlank(friendId0)) {
                // L.traceEndCall(ctx, METHOD_NAME);
                return true;
            }

            boolean b = setFriend0(userId0, friendId0, circleId0, reason, isadd);
            // L.traceEndCall(ctx, METHOD_NAME);
            return b;
//            boolean b = updateMyCircleMemberCount0(userId0,circleId0);
//            return  b;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getFriends0(String userId, List<String> circleIds, int page, int count) {
        SQLBuilder.Select select = new SQLBuilder.Select(friendSchema)
                .select("friend", "circle", "created_time")
                .from(friendTable)
                .where("${alias.user}=${v(user)}", "user", Long.parseLong(userId));

        if (circleIds.contains(Integer.toString(FRIENDS_CIRCLE)))
            select.and("${alias.circle}<>${blocked_circle}", "blocked_circle", BLOCKED_CIRCLE);
        else
            select.and("${alias.circle} IN (${circle_ids})", "circle_ids", StringUtils.join(circleIds, ","));

        select.append(" AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ");
        select.append(" AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + "");
        select.append(" GROUP BY friend  ORDER BY created_time DESC ");

        if (count > 0)
            select.page(page, count);

        String sql = select.toString();
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(friendSchema, recs);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getFriends(Context ctx, String userId, String circleIds, int page, int count) {
        // final String METHOD_NAME = "getFriends";
        // L.traceStartCall(ctx, METHOD_NAME, userId, circleIds, page, count);
        try {
            List<String> circleIds0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (circleIds0.isEmpty()) {
                // L.traceEndCall(ctx, METHOD_NAME);
                return new RecordSet();
            }

            for (String circleId : circleIds0) {
                if (!isFiniteCircle(Integer.parseInt(circleId)))
                    throw new ServerException(WutongErrors.FRIEND_ILLEGAL_CIRCLE, "Invalid circle (%s)", circleId);
            }
            RecordSet recs = getFriends0(toStr(userId), circleIds0, page, count);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getFollowers0(String userId, List<String> circleIds, int page, int count) {
        String sql = "select user,circle,created_time from friend use index (friend) where friend=" + userId + " ";


        if (circleIds.contains(Integer.toString(FRIENDS_CIRCLE)))
            sql += " and circle<>" + BLOCKED_CIRCLE + "";
        else {
            if (circleIds.size() > 0) {
                sql += " and circle in (" + StringUtils.join(circleIds, ",") + ")";
            }
        }

        sql += " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ";
        sql += " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ";

        sql += " GROUP BY user ORDER BY created_time DESC ";

        if (count > 0)
            sql += " " + SQLUtils.pageToLimit(page, count) + "";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(friendSchema, recs);
        recs.renameColumn("user", "follower");
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getFollowers(Context ctx, String userId, String circleIds, int page, int count) {
        // final String METHOD_NAME = "getFollowers";
        // L.traceStartCall(ctx, METHOD_NAME, userId, circleIds, page, count);
        try {
            List<String> circleIds0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (circleIds0.isEmpty()) {
                // L.traceEndCall(ctx, METHOD_NAME);
                return new RecordSet();
            }

            for (String circleId : circleIds0) {
                if (!isFiniteCircle(Integer.parseInt(circleId)))
                    throw new ServerException(WutongErrors.FRIEND_ILLEGAL_CIRCLE, "Invalid circle (%s)", circleId);
            }
            RecordSet recs = getFollowers0(toStr(userId), circleIds0, page, count);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getBothFriendsIds(Context ctx, String viewerId, String userId, int page, int count) {
        // final String METHOD_NAME = "getBothFriendsIds";
        // L.traceStartCall(ctx, METHOD_NAME, viewerId, userId, page, count);
        try {
            StringBuilder sql = new StringBuilder();
            sql.append(SQLTemplate.merge("SELECT distinct(${alias.friend}) FROM ${table} WHERE ${alias.circle}<>" + BLOCKED_CIRCLE + ""
                    + " AND ${alias.user}=${v(viewerId)}"
                    + " AND ${alias.friend}<>" + qiupuUid + " "
                    + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                    + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ",
                    "alias", friendSchema.getAllAliases(), "viewerId", viewerId, "table", friendTable));
            sql.append(SQLTemplate.merge("AND ${alias.friend} IN (SELECT ${alias.friend} FROM ${table} WHERE ${alias.circle}<>" + BLOCKED_CIRCLE + ""
                    + " AND ${alias.user}=${v(userId)}"
                    + " AND ${alias.friend}<>" + qiupuUid + " "
                    + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                    + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + ") order by created_time desc ",
                    "alias", friendSchema.getAllAliases(), "userId", userId, "table", friendTable));
            sql.append(SQLTemplate.merge(" ${limit}",
                    "limit", SQLUtils.pageToLimit(page, count)));

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql.toString(), null);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }


    @TraceCall
    @Override
    public RecordSet getRelation(Context ctx, String sourceUserId, String targetUserId, String circleId) {
        // final String METHOD_NAME = "getRelation";
        // L.traceStartCall(ctx, METHOD_NAME, sourceUserId, targetUserId, circleId);

        try {
            String circleId0 = toStr(circleId);
            if (StringUtils.isNotBlank(circleId0) && !isFiniteCircle(Integer.parseInt(circleId0)))
                throw new ServerException(WutongErrors.FRIEND_ILLEGAL_CIRCLE, "Invalid circle (%s)", circleId);
            RecordSet recs = getRelation0(toStr(sourceUserId), toStr(targetUserId), circleId0);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public Record getBidiRelation(Context ctx, String sourceUserId, String targetUserId, String circleId) {
        // final String METHOD_NAME = "getBidiRelation";
        // L.traceStartCall(ctx, METHOD_NAME, sourceUserId, targetUserId, circleId);
        try {
            String circleId0 = toStr(circleId);
            if (!isFiniteCircle(Integer.parseInt(circleId0)))
                throw new ServerException(WutongErrors.FRIEND_ILLEGAL_CIRCLE, "Invalid circle (%s)", circleId);

            String sourceUserId0 = toStr(sourceUserId);
            String targetUserId0 = toStr(targetUserId);
            Record rec = new Record();
            rec.put("relation1", getRelation0(sourceUserId0, targetUserId0, circleId0));
            rec.put("relation2", getRelation0(targetUserId0, sourceUserId0, circleId0));
            // L.traceEndCall(ctx, METHOD_NAME);
            return rec;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public boolean setRemark(Context ctx, String userId, String friendId, String remark) {
        // final String METHOD_NAME = "setRemark";
        // L.traceStartCall(ctx, METHOD_NAME, userId, friendId, remark);
        try {
            String sql;
            if (remark.isEmpty()) {
                sql = new SQLBuilder.Delete(nameRemarkSchema)
                        .deleteFrom(nameRemarkTable)
                        .where("${alias.user}=${v(user)} AND ${alias.friend}=${v(friend)}",
                                "user", Long.parseLong(userId), "friend", Long.parseLong(friendId))
                        .toString();
            } else {
                sql = new SQLBuilder.Replace(nameRemarkSchema)
                        .replaceInto(nameRemarkTable)
                        .value("user", Long.parseLong(userId))
                        .value("friend", Long.parseLong(friendId))
                        .value("remark", remark)
                        .toString();
            }

            SQLExecutor se = getSqlExecutor();
            se.executeUpdate(sql);
            // L.traceEndCall(ctx, METHOD_NAME);
            return true;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getRemarks0(String userId, String... friendIds) {
        String sql = new SQLBuilder.Select(nameRemarkSchema)
                .select("friend", "remark")
                .from(nameRemarkTable)
                .where("${alias.user}=${v(user)} AND ${alias.friend} IN (${friends})",
                        "user", Long.parseLong(userId), "friends", StringUtils.join(friendIds, ","))
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(nameRemarkSchema, recs);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getRemarks(Context ctx, String userId, String friendIds) {
        // final String METHOD_NAME = "getRemarks";
        // L.traceStartCall(ctx, METHOD_NAME, userId, friendIds);
        try {
            String[] friendIds0 = StringUtils2.splitArray(toStr(friendIds), ",", true);
            if (friendIds0.length == 0)
                return new RecordSet();

            RecordSet recs = getRemarks0(toStr(userId), friendIds0);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected Record isDeleteRecent0(String userId, List<String> friendIds,
                                     long period) {
        Record result = new Record();
        for (String friend : friendIds) {
            result.put(friend, false);
        }

        final String SQL = "SELECT action, count(*) AS count FROM ${table} WHERE user=${v(user)} AND action IN (${vjoin(l)}) AND acted_time>${time} group by action";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", "history"},
                {"user", userId},
                {"l", friendIds},
                {"time", String.valueOf(new Date().getTime() - period)}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        for (Record rec : recs) {
            String friend = StringUtils.substringAfter(rec.getString("action"), "delete_user_");
            boolean res = rec.getInt("count") > 0 ? true : false;
            result.put(friend, res);
        }

        return result;
    }

    @TraceCall
    @Override
    public Record isDeleteRecent(Context ctx, String userId, String friendIds, long period) {
        // final String METHOD_NAME = "isDeleteRecent";
        // L.traceStartCall(ctx, METHOD_NAME, userId, friendIds, period);
        try {
            List<String> l0 = StringUtils2.splitList(toStr(friendIds), ",", true);
            List<String> l = new ArrayList<String>();
            for (String friend : l0) {
                l.add("delete_user_" + friend);
            }
            Record rec = isDeleteRecent0(toStr(userId), l, period);
            // L.traceEndCall(ctx, METHOD_NAME);
            return rec;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getFriendOrFollowers(Context ctx, String userIds, String byFriendOrFollowers) {
        // final String METHOD_NAME = "getFriendOrFollowers";
        // L.traceStartCall(ctx, METHOD_NAME, userIds, byFriendOrFollowers);
        try {
            String sql = "";
            if (byFriendOrFollowers.equals("user")) {
                sql += "select user,friend from friend where user in (" + userIds + ") ";
            } else {
                sql += "select user,friend from friend use index (friend) where friend in (" + userIds + ") ";
            }
            sql += " and circle<>" + BLOCKED_CIRCLE + "";
            sql += " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ";
            sql += " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ";

            if (byFriendOrFollowers.equals("user")) {
                sql += " group by friend";
            } else {
                sql += " group by user";
            }

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            Schemas.standardize(nameRemarkSchema, recs);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getAllRelation(Context ctx, String viewerId, String userIds, String circleId, String inTheirOrInMine) {
        // final String METHOD_NAME = "getAllRelation";
        // L.traceStartCall(ctx, METHOD_NAME, viewerId, userIds, circleId, inTheirOrInMine);
        try {
            String sql = "";
            if (userIds.length() <= 0 || viewerId.length() <= 0) {
                // L.traceEndCall(ctx, METHOD_NAME);
                return new RecordSet();
            }

            RecordSet recs = new RecordSet();
            SQLExecutor se = getSqlExecutor();
            if (inTheirOrInMine.equals("their")) {
                String sql2 = "select circle,name,user from circle where user in (" + userIds + ")";
                RecordSet recs_c = se.executeRecordSet(sql2, null);
                Map<String, Object> m = new HashMap<String, Object>();
                for (Record c : recs_c) {
                    m.put(c.getString("user") + "_" + c.getString("circle"), c.getString("name"));
                }

                if (StringUtils.isBlank(circleId)) {
                    sql = "select user,circle from friend where friend=" + viewerId + " and user in (" + userIds + ") and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + "";
                } else if (Integer.parseInt(circleId) == FRIENDS_CIRCLE) {
                    sql = "select user,circle from friend where friend=" + viewerId + " and user in (" + userIds + ") and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " and circle<>" + BLOCKED_CIRCLE + "";
                } else {
                    sql = "select user,circle from friend where friend=" + viewerId + " and user in (" + userIds + ") and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " and circle=" + circleId + "";
                }

                recs = se.executeRecordSet(sql, null);
                if (!recs.isEmpty()) {
                    for (Record tu : recs) {
                        tu.put("name", m.get(tu.getString("user") + "_" + tu.getString("circle")));
                    }
                }
            }
            if (inTheirOrInMine.equals("mine")) {
                String sql2 = "select circle,name,user from circle where user in (" + viewerId + ")";

                RecordSet recs_c = se.executeRecordSet(sql2, null);
                Map<String, Object> m = new HashMap<String, Object>();
                for (Record c : recs_c) {
                    m.put(c.getString("user") + "_" + c.getString("circle"), c.getString("name"));
                }

                if (StringUtils.isBlank(circleId)) {
                    sql = "select friend,circle from friend where user=" + viewerId + " and friend in (" + userIds + ") and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + "";
                } else if (Integer.parseInt(circleId) == FRIENDS_CIRCLE) {
                    sql = "select friend,circle from friend where user=" + viewerId + " and friend in (" + userIds + ") and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " and circle<>" + BLOCKED_CIRCLE + "";
                } else {
                    sql = "select friend,circle from friend where user=" + viewerId + " and friend in (" + userIds + ") and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " and circle=" + circleId + "";
                }

                recs = se.executeRecordSet(sql, null);
                if (!recs.isEmpty()) {
                    for (Record tu : recs) {
                        tu.put("name", m.get(viewerId + "_" + tu.getString("circle")));
                    }
                }
            }
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet topUserFollowers(Context ctx, long userId, int limit) {
        // final String METHOD_NAME = "topUserFollowers";
        // L.traceStartCall(ctx, METHOD_NAME, userId, limit);
        try {
            String sql = "select count(distinct(user)) as count1,friend from friend where friend not in (select friend from friend where user=" + userId + " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " and circle<>" + BLOCKED_CIRCLE + ") group by friend limit " + limit + "";
            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            recs.sort("count1", false);
            RecordSet out0 = new RecordSet();
            if (recs.size() > limit) {
                for (int i = 0; i < limit; i++) {
                    out0.add(recs.get(i));
                }
            } else {
                for (int i = 0; i < recs.size(); i++) {
                    out0.add(recs.get(i));
                }
            }

            // L.traceEndCall(ctx, METHOD_NAME);
            return out0;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public Record getMyFriends(Context ctx, String userId, String friendId) {
        // final String METHOD_NAME = "getMyFriends";
        // L.traceStartCall(ctx, METHOD_NAME, userId, friendId);
        try {
            SQLBuilder.Select select = new SQLBuilder.Select(friendSchema)
                    .select("friend", "circle", "created_time")
                    .from(friendTable)
                    .where("${alias.user}=${v(user)} and ${alias.friend}=${v(friend)}", "user", Long.parseLong(userId), "friend", Long.parseLong(friendId));

            select.append(" AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ");
            select.append(" AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ");
            select.append(" ORDER BY created_time DESC limit 1");

            String sql = select.toString();
            SQLExecutor se = getSqlExecutor();
            Record rec = se.executeRecord(sql, null);
            Schemas.standardize(friendSchema, rec);
            // L.traceEndCall(ctx, METHOD_NAME);
            return rec;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public int getFollowersCount(Context ctx, String userId) {
        // final String METHOD_NAME = "getFollowersCount";
        // L.traceStartCall(ctx, METHOD_NAME, userId);
        try {
            String sql = "select count(*) from friend use index (friend) where friend=" + userId + " ";
            sql += " and circle<>" + BLOCKED_CIRCLE + "";
            sql += " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ";
            sql += " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ";

            SQLExecutor se = getSqlExecutor();
            int n = (int) se.executeIntScalar(sql, 0);
            // L.traceEndCall(ctx, METHOD_NAME);
            return n;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public int getFriendsCount(Context ctx, String userId) {
        // final String METHOD_NAME = "getFriendsCount";
        // L.traceStartCall(ctx, METHOD_NAME, userId);
        try {
            String sql = "select count(*) from friend where user=" + userId + " ";
            sql += " and circle<>" + BLOCKED_CIRCLE + "";
            sql += " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ";
            sql += " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ";
            sql += " group by friend";

            SQLExecutor se = getSqlExecutor();
            int n = (int) se.executeIntScalar(sql, 0);
            // L.traceEndCall(ctx, METHOD_NAME);
            return n;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public boolean createVirtualFriendId(Context ctx, String userId, String friendId, String content, String name) {
        // final String METHOD_NAME = "createVirtualFriendId";
        // L.traceStartCall(ctx, METHOD_NAME, userId, friendId, content, name);
        try {
            SQLExecutor se = getSqlExecutor();
            String sql = "insert into virtual_friendid (virtual_friendid,content,name,user_id) values('" + friendId + "','" + content + "','" + name + "','" + userId + "')";
            boolean b = se.executeUpdate(sql) > 0;
            // L.traceEndCall(ctx, METHOD_NAME);
            return b;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getVirtualFriendId0(String content) {
        try {
            SQLExecutor se = getSqlExecutor();
            String sql = "select virtual_friendid from virtual_friendid where content='" + content + "'";
            RecordSet recs = se.executeRecordSet(sql, null);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public boolean updateVirtualFriendIdToAct(Context ctx, String friendId, String content) {
        // final String METHOD_NAME = "updateVirtualFriendIdToAct";
        // L.traceStartCall(ctx, METHOD_NAME, friendId, content);
        try {
            SQLExecutor se = getSqlExecutor();
            RecordSet recs = getVirtualFriendId0(content);
            String virtualFriendIds = recs.joinColumnValues("virtual_friendid", ",");
            String sql = "update friend set friend='" + friendId + "',type=0 where friend in (" + virtualFriendIds + ") and type=1";
            se.executeUpdate(sql);
            deleteVirtualFriendId0(virtualFriendIds, content);
            String sql1 = "delete from friend where friend='" + friendId + "' and user=friend and type=0";
            se.executeUpdate(sql1);
            // L.traceEndCall(ctx, METHOD_NAME);
            return true;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getContactFriend(Context ctx, String userIds) {
        // final String METHOD_NAME = "getContactFriend";
        // L.traceStartCall(ctx, METHOD_NAME, userIds);
        try {
            String sql = "select * from friend where user in (" + userIds + ") and type=1 order by created_time desc";
            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            // L.traceEndCall(ctx, METHOD_NAME);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getContactFriendByFid(Context ctx, String friendIds) {
        try {
            //        String sql = "select distinct(friend),name,content from friend where friend in (" + friendIds + ") and type=1 order by created_time desc";
//        SQLExecutor se = getSqlExecutor();
//        RecordSet recs = se.executeRecordSet(sql, null);
//        return recs;

            String sql = "select virtual_friendid,name,content from virtual_friendid where virtual_friendid in (" + friendIds + ")";
            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getVirtualFriendId(Context ctx, String content) {
        try {
            SQLExecutor se = getSqlExecutor();
            String sql = "select virtual_friendid from virtual_friendid where content='" + content + "'";
            RecordSet recs = se.executeRecordSet(sql, null);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public String getUserFriendHasVirtualFriendId(Context ctx, String userId, String content) {
        try {
            SQLExecutor se = getSqlExecutor();
            String sql = "select virtual_friendid from virtual_friendid where content='" + content + "' and user_id='" + userId + "'";
            Record rec = se.executeRecordSet(sql, null).getFirstRecord();
            return rec.isEmpty() ? "0" : rec.getString("virtual_friendid");
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getVirtualFriendIdByName(Context ctx, String userId, String name) {
        try {
            SQLExecutor se = getSqlExecutor();
            String sql = "select virtual_friendid from virtual_friendid where user_id='" + userId + "' and name like '%" + name + "%'";
            RecordSet recs = se.executeRecordSet(sql, null);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }


    @Override
    public boolean followPage(Context ctx, long viewerId, long pageId) {
        if (viewerId <= 0)
            return false;

        String sql = new SQLBuilder.Insert()
                .insertIgnoreInto(friendTable)
                .value("user", viewerId)
                .value("friend", pageId)
                .value("circle", Constants.PAGE_CIRCLE)
                .value("created_time", DateUtils.nowMillis())
                .value("reason", FRIEND_REASON_MANUALSELECT)
                .value("type", 0)
                .value("name", "")
                .value("content", "")
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    public void unfollowPage(Context ctx, long viewerId, long pageId) {
        if (viewerId <= 0)
            return;

        String sql = new SQLBuilder.Delete()
                .deleteFrom(friendTable)
                .where("user=${v(user_id)} AND friend=${v(friend_id)} AND circle=${v(page_circle_id)}",
                        "user_id", viewerId, "friend_id", pageId, "page_circle_id", PAGE_CIRCLE)
                .toString();
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql);
    }

    @Override
    public long[] getFollowedPageIds(Context ctx, long viewerId) {
        if (viewerId <= 0)
            return new long[0];

        String sql = new SQLBuilder.Select()
                .select("friend")
                .from(friendTable)
                .where("user=${v(user_id)} AND circle=${v(page_circle_id)}",
                        "user_id", viewerId, "page_circle_id", PAGE_CIRCLE)
                .toString();

        final ArrayList<Long> pageIds = new ArrayList<Long>();
        SQLExecutor se = getSqlExecutor();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                pageIds.add(rec.getInt("friend"));
            }
        });
        return CollectionUtils2.toLongArray(pageIds);
    }

    @Override
    public long[] isFollowedPages(Context ctx, long viewerId, long[] pageIds) {
        if (viewerId <= 0 || ArrayUtils.isEmpty(pageIds))
            return new long[0];

        String sql = new SQLBuilder.Select()
                .select("friend")
                .from(friendTable)
                .where("user=${v(user_id)} AND circle=${v(page_circle_id)} AND friend IN (${page_ids})",
                        "user_id", viewerId, "page_circle_id", PAGE_CIRCLE, "page_ids", StringUtils2.join(pageIds, ","))
                .toString();
        final ArrayList<Long> pageIds1 = new ArrayList<Long>();
        SQLExecutor se = getSqlExecutor();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                pageIds1.add(rec.getInt("friend"));
            }
        });
        return CollectionUtils2.toLongArray(pageIds1);
    }

    @Override
    public long[] getFollowerIds(Context ctx, long userId, long idBegin, long idEnd, int page, int count) {
        if (userId <= 0)
            return new long[0];

        String sql = new SQLBuilder.Select()
                .select("distinct user")
                .from(friendTable)
                .where("friend=${v(user_id)} AND circle<>${v(blocked_circle_id)} AND user>=${v(begin)} AND user<${v(end)}", new Object[][]{
                        {"user_id", userId},
                        {"blocked_circle_id", Constants.BLOCKED_CIRCLE},
                        {"begin", idBegin},
                        {"end", idEnd}
                })
                .page(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        final ArrayList<Long> followerIds = new ArrayList<Long>();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                followerIds.add(rec.getInt("user"));
            }
        });
        return CollectionUtils2.toLongArray(followerIds);
    }

    // Platform method
    @TraceCall
    @Override
    public boolean destroyCircleP(Context ctx, String userId, String circleIds) {
        Validate.notNull(userId);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);


        List<String> cl = StringUtils2.splitList(toStr(circleIds), ",", true);

        for (String cl0 : cl) {
            RecordSet recs = getCircles(ctx, userId, cl0, true);
            String uids = recs.getFirstRecord().getString("members");
            List<String> l = StringUtils2.splitList(uids, ",", true);
            for (String uid : l) {
                if (uid.length() == 19) {
                    //contact friend,have virtual borqsId
                    Record v_f = getContactFriendByFid(ctx, uid).getFirstRecord();
                    if (!v_f.isEmpty()) {
                        setContactFriend(ctx, userId, uid, v_f.getString("name"), v_f.getString("content"), cl0, FRIEND_REASON_MANUALSELECT, false, false);
                    }
                } else {
                    setFriends(ctx, userId, uid, cl0, FRIEND_REASON_MANUALSELECT, false);
                }
            }
        }
        return destroyCircles(ctx, userId, circleIds);
    }

    @TraceCall
    @Override
    public RecordSet getCirclesP(Context ctx, String userId, String circleIds, boolean withUsers) {
        Validate.notNull(userId);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);
        RecordSet recs = getCircles(ctx, userId, circleIds, withUsers);
        if (withUsers) {
            for (Record rec : recs) {
                String memberIds = rec.getString("members");
                RecordSet members = GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), memberIds, "user_id, display_name, remark, image_url,perhaps_name");
                rec.put("members", members.toJsonNode());
            }
        }
        return recs;
    }


    @TraceCall
    @Override
    public RecordSet getRelationP(Context ctx, String sourceUserId, String targetUserId) {
        return getRelation(ctx, sourceUserId, targetUserId, "");
    }

    @TraceCall
    @Override
    public boolean isFriendP(Context ctx, String sourceUserId, String targetUserId) {
        return isHisFriendP(ctx, targetUserId, sourceUserId);
    }


    @TraceCall
    @Override
    public boolean isHisFriendP(Context ctx, String sourceUserId, String targetUserId) {
        RecordSet recs = getRelationP(ctx, sourceUserId, targetUserId);
        if (recs.isEmpty())
            return false;

        for (Record rec : recs) {
            if (rec.checkGetInt("circle_id") == BLOCKED_CIRCLE)
                return false;
        }
        return true;
    }

    private void sendPostBySetFriendP(Context ctx, String userId, String friendIds, int reason, boolean can_comment, boolean can_like, boolean can_reshare) {
        final String METHOD = "sendPostBySetFriendP";
        L.traceStartCall(ctx, METHOD, userId, friendIds, reason, can_comment, can_like, can_reshare);
        Record rec = new Record();
        rec.put("setFriend", true);
        rec.put("userId", userId);
        rec.put("friendIds", friendIds);
        rec.put("reason", reason);
        rec.put("can_comment", can_comment);
        rec.put("can_like", can_like);
        rec.put("can_reshare", can_reshare);
        rec.put("add_to", "");

        //context cols
        rec.put("viewerId", ctx.getViewerId());
        rec.put("app", ctx.getAppId());
        rec.put("ua", ctx.getUa());
        rec.put("location", ctx.getLocation());
        rec.put("language", ctx.getLanguage());
        rec.put("post_source", Constants.POST_SOURCE_SYSTEM);
        String json = rec.toString(false, false);
        String rec_str = json;
        try {
            rec_str = StringUtils2.compress(json);
        } catch (IOException e) {
            rec_str = json;
        }
        MQ mq = MQCollection.getMQ("platform");
        if ((mq != null) && (rec_str.length() < 1024))
            mq.send("stream", rec_str);
        L.traceEndCall(ctx, METHOD);
    }


    private void sendFriendFeedbackRequest(Context ctx, String source, String to, boolean addAddreddCircle) {
        if (GlobalLogics.getFriendship().isHisFriendP(ctx, to, source))
            GlobalLogics.getRequest().createRequestP(ctx, to, source, "0", REQUEST_FRIEND_FEEDBACK, "", "", addAddreddCircle);
    }

    @TraceCall
    @Override
    public boolean setFriendsP(Context ctx, String userId, String friendIds, String circleId, int reason, boolean isadd) {
        Validate.notNull(userId);

        List<String> l = StringUtils2.splitList(toStr(friendIds), ",", true);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);

        AccountLogic account = GlobalLogics.getAccount();
        RequestLogic reqre = GlobalLogics.getRequest();
        List<String> nl = new ArrayList<String>();


        for (String uid : l) {
            if (uid.length() > Constants.USER_ID_MAX_LEN) {
                //contact friend,have virtual borqsId
                Record v_f = getContactFriendByFid(ctx, uid).getFirstRecord();
                if (!v_f.isEmpty())
                    setContactFriend(ctx, userId, uid, v_f.getString("name"), v_f.getString("content"), circleId, reason, isadd, false);
            } else {
                //have borqs id
                // which circle i was in others
                RecordSet ys_recs = getRelation(ctx, userId, uid, String.valueOf(ADDRESS_BOOK_CIRCLE));
                boolean in_address_circle = false;
                boolean in_friend_circle = false;
                for (Record c_u : ys_recs) {
                    if (c_u.getString("circle_id").equals(String.valueOf(ADDRESS_BOOK_CIRCLE)))
                        in_address_circle = true;
                }
                if (ys_recs.size() > 0)
                    in_friend_circle = true;

                boolean beforeFriend = isFriendP(ctx, userId, uid);

                if (isadd && circleId.equals(String.valueOf(ADDRESS_BOOK_CIRCLE))) {
                    //if in his address_book circles
                    if (!in_address_circle && uid.length() < Constants.USER_ID_MAX_LEN) {
                        reqre.createRequest(ctx, uid, userId, "0", REQUEST_PROFILE_ACCESS, "", "", "[]");
                    }
                } else {
                    //if in one of his circles
                }
                setFriends(ctx, userId, uid, circleId, reason, isadd);
                boolean afterFriend = isFriendP(ctx, userId, uid);
                if (!beforeFriend && afterFriend) {
                    if (reason != FRIEND_REASON_INVITE) {
                        if (!in_friend_circle) {
                            //why have this
                            //we just want user quick take actions, but current we use notification to handler this,
                            //so ignore
                            if (false) {
                                sendFriendFeedbackRequest(ctx, userId, uid, false);
                            }
                        }
                    }
                    nl.add(uid);

                    //notification
                    String beSuggestedName = account.getUser(ctx, uid, uid, "display_name").getString("display_name", "");
                    String toName = account.getUser(ctx, userId, userId, "display_name").getString("display_name", "");

                    Commons.sendNotification(ctx,
                            NTF_ACCEPT_SUGGEST,
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(userId),
                            Commons.createArrayNodeFromStrings(toName, beSuggestedName),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(userId, toName, uid, beSuggestedName),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(userId, uid)
                    );
                }

                RecordSet rs = getRelationP(ctx, uid, userId);
                Record changed = Record.of("user", userId, "friend", uid, "circle", rs.joinColumnValues("circle_id", ","));
                GlobalLogics.getHooks().fireFriendshipChanged(ctx, changed);
            }
        }

        //notification
        List<String> rl = new ArrayList<String>();
        if (isadd && nl.size() > 0) {
            Record r = isDeleteRecent(ctx, userId, StringUtils.join(nl, ", "), 30L * 24 * 60 * 60 * 1000);

            for (String user : nl) {
                boolean res = r.getBoolean(user, false);
                if (!res)
                    rl.add(user);
            }

            Commons.sendNotification(ctx, NTF_NEW_FOLLOWER,
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(userId),
                    Commons.createArrayNodeFromStrings(userId),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(userId),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(rl.toArray(new String[rl.size()]))
            );

        }

        if (isadd && rl.size() > 0) {
            sendPostBySetFriendP(ctx, userId, StringUtils.join(rl, ","), reason, true, true, true);
        }
        return true;
    }

    public String getVirtualUIDP(Context ctx) {
        try {
            String friendId = Long.toString(RandomUtils.generateId());
            /*
            String maxUID = getNowUserGeneralId();
            int len = maxUID.length();
            String t1 = StringUtils.substring(friendId, 0, Constants.USER_ID_MAX_LEN);
            String t2 = "";
            for (int i = 0; i < 9 - len; i++) {
                t2 += "0";
            }
            return t1 + t2 + maxUID;
            */
            return friendId;
        } finally {
        }
    }

    @TraceCall
    @Override
    public String setContactFriendP(Context ctx, String userId, String friendName, String content, String circleIds, int reason) {
        Validate.notNull(userId);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);

        final String SERVER_HOST = GlobalConfig.get().getString("server.host", "api.borqs.com");

        String ua = ctx.getUa();
        String friendId = getVirtualUIDP(ctx);
        setContactFriend(ctx, userId, friendId, friendName, content, circleIds, reason, true, true);
        createVirtualFriendId(ctx, userId, friendId, content, friendName);
        String fromName = GlobalLogics.getAccount().getUser(ctx, userId, userId, "display_name").getString("display_name");
        String lang = parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";

        if (content.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
            String template = getBundleString(ua, "platformservlet.email.invite.email");
            String temp = FeedbackParams.toSegmentedBase64(true, "/", content, friendName, userId);
            String url = "http://" + SERVER_HOST + "/account/invite?info=" + temp;
            String emailContent = SQLTemplate.merge(template, new Object[][]{
                    {"displayName", fromName},
                    {"fromName", fromName},
                    {"url", url}
            });

            template = getBundleString(ua, "platformservlet.email.invite.title");
            String title = SQLTemplate.merge(template, new Object[][]{
                    {"fromName", fromName}
            });
            try {
                GlobalLogics.getEmail().sendEmail(ctx, title, content, friendName, emailContent, EMAIL_ESSENTIAL, lang);
            } catch (Exception e) {
                L.error(ctx, e, "send email error:add contact to friend,email=" + content);
            }
        } else {
            String smsTitle = getBundleString(ua, "platformservlet.sms.invite.title");
            String temp = FeedbackParams.toSegmentedBase64(true, "/", content, friendName, userId);
            String url = "http://" + SERVER_HOST + "/account/invite?info=" + temp;
            String smsContent = SQLTemplate.merge(smsTitle, new Object[][]{
                    {"fromName", fromName},
                    {"urll", GlobalLogics.getShortUrl().generalShortUrl(url)}
            });
            try {
                Commons.sendSms(ctx, content, smsContent + "\\");
            } catch (Exception e) {
                L.error(ctx, e, "send sms error:add contact to friend,phone=" + content + ",and smscontent=" + smsContent);
            }
        }

        return friendId;
    }


    @TraceCall
    @Override
    public Record setFriendP(Context ctx, String userId, String friendId, String circleIds, int reason) {
        Validate.notNull(userId);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);


        RequestLogic reqre = GlobalLogics.getRequest();

        List<String> ll = StringUtils2.splitList(toStr(circleIds), ",", true);
        if (ll.size() <= 0) {
            //delete from all my circles
            RecordSet ys_recs = getRelation(ctx, friendId, userId, String.valueOf(FRIENDS_CIRCLE));
            for (Record r : ys_recs) {
                setFriendsP(ctx, userId, friendId, r.getString("circle_id"), 0, false);
            }
        } else {
            boolean b = true;
            if (ll.size() == 1 && ll.get(0).contains(String.valueOf(ADDRESS_BOOK_CIRCLE))) {
                RecordSet ys_recs = getRelation(ctx, friendId, userId, "");
                for (Record r : ys_recs) {
                    setFriends(ctx, userId, friendId, r.getString("circle_id"), 0, false);
                }
            } else {
                boolean beforeFriend = isFriendP(ctx, userId, friendId);
                if (friendId.length() > Constants.USER_ID_MAX_LEN) {
                    //contact friend,have virtual borqsId
                    Record v_f = getContactFriendByFid(ctx, friendId).getFirstRecord();
                    if (!v_f.isEmpty())
                        b = setContactFriend(ctx, userId, friendId, v_f.getString("name"), v_f.getString("content"), circleIds, reason, true, true);
                } else {
                    b = setFriend(ctx, userId, friendId, circleIds, reason);
                }
                boolean afterFriend = isFriendP(ctx, userId, friendId);

                if (!beforeFriend && afterFriend && friendId.length() < Constants.USER_ID_MAX_LEN) {
                    RecordSet ys_recs = getRelation(ctx, userId, friendId, String.valueOf(FRIENDS_CIRCLE));
                    if (ys_recs.size() <= 0) {
                        if (ll.contains(String.valueOf(ADDRESS_BOOK_CIRCLE))) {
//                            sendFriendFeedbackRequest(userId, friendId, false, ua, loc);
                            reqre.createRequest(ctx, friendId, userId, "0", REQUEST_PROFILE_ACCESS, "", "", "[]");
                        } else {
//                            reqre.createRequest(friendId, userId, "0", Constants.REQUEST_FRIEND_FEEDBACK, "", "", "[]");
                        }
                    }

                    //notification
                    Record r = isDeleteRecent(ctx, userId, friendId, 30L * 24 * 60 * 60 * 1000);

                    boolean res = r.getBoolean(friendId, false);
                    if (!res) {
                        Commons.sendNotification(ctx, NTF_NEW_FOLLOWER,
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(userId),
                                Commons.createArrayNodeFromStrings(userId),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(userId),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(friendId)
                        );


                        //notification
                        String beSuggestedName = GlobalLogics.getAccount().getUser(ctx, friendId, friendId, "display_name").getString("display_name", "");
                        String toName = GlobalLogics.getAccount().getUser(ctx, userId, userId, "display_name").getString("display_name", "");

                        Commons.sendNotification(ctx, NTF_ACCEPT_SUGGEST,
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(userId),
                                Commons.createArrayNodeFromStrings(toName, beSuggestedName),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(userId, toName, friendId, beSuggestedName),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(userId, friendId)
                        );


                        //send stream
                        sendPostBySetFriendP(ctx, userId, friendId, reason, true, true, true);
                    }
                }

                Record changed = Record.of("user", userId, "friend", friendId, "circle", circleIds);
                GlobalLogics.getHooks().fireFriendshipChanged(ctx, changed);
            }
        }


        Record rec = GlobalLogics.getAccount().getUsers(ctx, userId, friendId, AccountLogic.USER_STANDARD_COLUMNS, true).getFirstRecord();
//            RecordSet inCircles = RecordSet.fromJson(JsonUtils.toJson(rec.get("in_circles"), false));
//            inCircles = dealWithInCirclesByGroups(PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, userId, rec.getString("user_id"), inCircles);
//            rec.put("in_circles", inCircles);
        return rec;
//            return b;

    }

    @TraceCall
    @Override
    public Record exchangeVcardP(Context ctx, String userId, String friendId, String circleIds, int reason, boolean send_request) {
        Validate.notNull(userId);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);


        List<String> ll = StringUtils2.splitList(toStr(circleIds), ",", true);
        if (ll.size() > 0) {
            boolean b = true;
            RequestLogic reqre = GlobalLogics.getRequest();
            if (ll.size() == 1 && ll.get(0).contains(String.valueOf(ADDRESS_BOOK_CIRCLE))) {
                RecordSet ys_recs = getRelation(ctx, friendId, userId, "");
                for (Record r : ys_recs) {
                    setFriends(ctx, userId, friendId, r.getString("circle_id"), 0, false);
                }
            } else {
                if (friendId.length() > Constants.USER_ID_MAX_LEN) {
                    //contact friend,have virtual borqsId
                    Record v_f = getContactFriendByFid(ctx, friendId).getFirstRecord();
                    if (!v_f.isEmpty())
                        b = setContactFriend(ctx, userId, friendId, v_f.getString("name"), v_f.getString("content"), circleIds, reason, true, true);
                } else {
                    b = setFriend(ctx, userId, friendId, circleIds, reason);

                    //add by wangpeng  2012-08-15
                    /*Account a = getProxy(Account.class, transAccount);
                RecordSet userRs =  RecordSet.fromByteBuffer(a.getUsers(userId,"ss"));
                RecordSet friendRs =  RecordSet.fromByteBuffer(a.getUsers(uid,"ss"));*/
                    Record changed = Record.of("user", userId, "friend", friendId);
                    GlobalLogics.getHooks().sendAccountInfo(ctx, changed);
                    //add by wangpeng  2012-08-15
                }

                Record changed = Record.of("user", userId, "friend", friendId, "circle", circleIds);
                GlobalLogics.getHooks().fireFriendshipChanged(ctx, changed);
            }
            if (send_request) {
                boolean if_in = getIfHeInMyCircles(ctx, friendId, userId, String.valueOf(ADDRESS_BOOK_CIRCLE));
                if (!if_in)
                    reqre.createRequest(ctx, friendId, userId, "0", REQUEST_PROFILE_ACCESS, "", "", "[]");
            }
        }

        return GlobalLogics.getAccount().getUsers(ctx, userId, friendId, AccountLogic.USER_STANDARD_COLUMNS, true).getFirstRecord();
//            return b;

    }


    @TraceCall
    @Override
    public RecordSet dealWithInCirclesByGroupsP(Context ctx, long begin, long end, String userId, String friendId, RecordSet reuse) {
        L.trace(ctx, "In dealWithInCirclesByGroups");
        GroupLogic group = GlobalLogics.getGroup();

        RecordSet userGroups = group.findGroupsByMember(ctx, begin, end, Long.parseLong(userId), GROUP_LIGHT_COLS);
        RecordSet friendGroups = group.findGroupsByMember(ctx, begin, end, Long.parseLong(friendId), GROUP_LIGHT_COLS);
        RecordSet recs = new RecordSet(CollectionUtils.intersection(userGroups, friendGroups));

        for (Record rec : recs) {
            long groupId = rec.getInt(GRP_COL_ID, 0);
            String groupName = rec.getString(GRP_COL_NAME, "");
            reuse.add(Record.of("circle_id", String.valueOf(groupId), "circle_name", groupName));
        }

        return reuse;
    }


    @TraceCall
    @Override
    public RecordSet getFriendsP(Context ctx, String viewerId, String userId, String circleIds, String cols, int page, int count) {
        return getFriendsP(ctx, viewerId, userId, circleIds, cols, false, page, count);
    }

    @TraceCall
    @Override
    public RecordSet getFriendsP(Context ctx, String viewerId, String userId, String circleIds, String cols, boolean inPublicCircles, int page, int count) {
        Validate.notNull(userId);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);

        cols = Commons.parseUserColumns(cols);
        RecordSet recs = getFriends(ctx, userId, circleIds, page, count);
//        if (cols.trim().equals("user_id")) {
//            recs.removeColumns("circle");
//            recs.renameColumn("friend", "user_id");
//            return recs;
//        } else {
//            String friendIds = recs.joinColumnValues("friend", ",");
//            return getUsers(viewerId, friendIds, cols);
//        }
        String friendIds = recs.joinColumnValues("friend", ",");

        RecordSet users = new RecordSet();
        if (StringUtils.isNotBlank(friendIds)) {
            if (cols.contains("contact_info")) {
                users = GlobalLogics.getAccount().getUsers(ctx, viewerId, friendIds, cols);
            }  else {
                users = GlobalLogics.getAccount().getUsersBaseColumnsContainsRemarkRequest(ctx, viewerId, friendIds);
            }

            if (inPublicCircles) {
                for (Record user : users) {
                    RecordSet inCircles = RecordSet.fromJson(JsonUtils.toJson(user.get("in_circles"), false));
                    inCircles = dealWithInCirclesByGroupsP(ctx, PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, userId, user.getString("user_id"), inCircles);
                    user.put("in_circles", inCircles);
                }
            }
        }

        return users;
    }


    @TraceCall
    @Override
    public RecordSet getFriendsV2P(Context ctx, String viewerId, String userId, String circleIds, String cols, int page, int count) {
        Validate.notNull(userId);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);


        cols = Commons.parseUserColumns(cols);
        List<String> l = new ArrayList<String>();
        List<String> groupsFromCircleIds = new ArrayList<String>();


        GroupLogic group = GlobalLogics.getGroup();
        if (StringUtils.equals(circleIds, String.valueOf(FRIENDS_CIRCLE))) {
            l.add(String.valueOf(FRIENDS_CIRCLE));
            RecordSet recs = group.findGroupsByMember(ctx, PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, Long.parseLong(userId), GROUP_LIGHT_COLS);
            groupsFromCircleIds = recs.getStringColumnValues(GRP_COL_ID);
        } else {
            l = StringUtils2.splitList(circleIds, ",", true);
            groupsFromCircleIds = group.getGroupIdsFromMentions(ctx, l);
            l.removeAll(groupsFromCircleIds);
            PageLogicUtils.removeAllPageIds(ctx, l);
        }

        RecordSet recs = getFriends(ctx, userId, StringUtils2.joinIgnoreBlank(",", l), page, count);
//        if (cols.trim().equals("user_id")) {
//            recs.removeColumns("circle");
//            recs.renameColumn("friend", "user_id");
//            return recs;
//        } else {
//            String friendIds = recs.joinColumnValues("friend", ",");
//            return getUsers(viewerId, friendIds, cols);
//        }
        int size = recs.size();
        String friendIds = recs.joinColumnValues("friend", ",");

        //groups
        if (size <= 0) {
            int friendCount = getFriendsCount(ctx, userId);
            int friendPageCount = friendCount / count;
            if (friendCount % count != 0)
                friendPageCount += 1;
            page -= friendPageCount;

            List<String> allFriendIds = getFriends(ctx, userId, StringUtils2.joinIgnoreBlank(",", l), -1, -1).getStringColumnValues("friend");
            friendIds = "";
            if (!groupsFromCircleIds.isEmpty()) {
                String groupIds = StringUtils2.joinIgnoreBlank(",", groupsFromCircleIds);
                String memberIds = toStr(group.getMembers(ctx, groupIds, page, count));
                List<String> members = StringUtils2.splitList(memberIds, ",", true);
                for (String member : members) {
                    if (!allFriendIds.contains(member))
                        friendIds += member + ",";
                }
                friendIds = StringUtils.substringBeforeLast(friendIds, ",");
            }
        }

        RecordSet users = new RecordSet();
        if (StringUtils.isNotBlank(friendIds)) {
            if (cols.contains("contact_info")) {
                users = GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), friendIds, cols);
            } else {
                users = GlobalLogics.getAccount().getUsersBaseColumnsContainsRemarkRequest(ctx, ctx.getViewerIdString(), friendIds);
            }

            for (Record user : users) {
                RecordSet inCircles = RecordSet.fromJson(JsonUtils.toJson(user.get("in_circles"), false));
                inCircles = dealWithInCirclesByGroupsP(ctx, PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, userId, user.getString("user_id"), inCircles);
                user.put("in_circles", inCircles);
            }
        }

        return users;
    }

    @TraceCall
    @Override
    public RecordSet getBothFriendsP(Context ctx, String viewerId, String userId, int page, int count) {
        RecordSet bo = getBothFriendsIds(ctx, viewerId, userId, page, count);
        String uids = bo.joinColumnValues("friend", ",");
        return GlobalLogics.getAccount().getUsersBaseColumnsContainsRemarkRequest(ctx, ctx.getViewerIdString(), uids);

    }


    @TraceCall
    @Override
    public RecordSet getFollowersP(Context ctx, String viewerId, String userId, String circleIds, String cols, int page, int count) {
        cols = Commons.parseUserColumns(cols);


        RecordSet recs = getFollowers(ctx, userId, circleIds, page, count);
        if (cols.trim().equals("user_id")) {
            recs.removeColumns("circle");
            recs.renameColumn("follower", "user_id");
            return recs;
        } else {
            //String friendIds = recs.joinColumnValues("follower", ",");
            String followerIds = recs.joinColumnValues("follower", ",");
            RecordSet recs_u = GlobalLogics.getAccount().getUsersBaseColumnsContainsRemarkRequest(ctx, ctx.getViewerIdString(), followerIds);

            Map map = new HashMap();
            for (Record u : recs_u) {
                map.put(u.getString("user_id"), u.toString(false, false));
            }


            RecordSet recs0 = new RecordSet();
            for (Record r : recs) {
                String fId = r.getString("follower");
                if (map.get(fId) != null) {
                    Record us = Record.fromJson(map.get(fId).toString());
                    if (!us.isEmpty()) {
                        us.put("relation_created_time", r.getString("created_time"));
                        recs0.add(us);
                    }
                }
            }
            return recs0;
        }
    }

    @TraceCall
    @Override
    public String getFriendsIdP(Context ctx, String userId, String circleIds, String cols, int page, int count) {
        Validate.notNull(userId);
        GlobalLogics.getAccount().checkUserIds(ctx, userId);
        RecordSet recs = getFriends0(userId, StringUtils2.splitList(circleIds, ",", true), page, count);
        String friendIds = recs.joinColumnValues("friend", ",");
        return friendIds;
    }

}
