package com.borqs.server.platform.friendship;


import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.borqs.server.service.platform.Constants.*;

public class SimpleFriendship extends FriendshipBase {

    private static final Logger L = LoggerFactory.getLogger(SimpleFriendship.class);

    private ConnectionFactory connectionFactory;
    private String db;
    private String friendTable;
    private String circleTable;
    private String nameRemarkTable;
    private String qiupuUid;

    public SimpleFriendship() {

    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
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

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
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

    @Override
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

    @Override
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
    
    @Override
    protected boolean updateCircleMemberCount0(String userId, String circleId,int member_count) {
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
        return n > 0;
    }

    @Override
    protected boolean updateMyCircleMemberCount0(String userId, String circleId) {
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
        return n > 0;
    }

    @Override
    protected int getCircleCount(String userId) {
        final String SQL = "SELECT count(*) FROM ${table} WHERE ${alias.user}=${v(user)}";
        String sql = SQLTemplate.merge(SQL,
                "table", circleTable,
                "alias", circleSchema.getAllAliases(),
                "user", userId);

        SQLExecutor se = getSqlExecutor();
        return (int) se.executeIntScalar(sql, 0);
    }

    protected int getMemberCount0(String userId, int circleId) {
        String sql;
        if (circleId == FRIENDS_CIRCLE) {
            final String SQL = "SELECT sum(${alias.friend}) FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.circle} <> ${v(blocked_circle)}" +
                    " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + "" +
                    " AND reason<>" + Constants.FRIEND_REASON_DEFAULT_DELETE + "" +
                    " GROUP BY ${alias.friend}";
            sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", friendTable},
                    {"alias", friendSchema.getAllAliases()},
                    {"user", userId},
                    {"blocked_circle", BLOCKED_CIRCLE},
            });
        } else {
            sql = "select count(*) from friend where user="+userId+" AND circle="+circleId+"" +
                    " AND reason<>" + Constants.FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                  + " AND reason<>" + Constants.FRIEND_REASON_DEFAULT_DELETE + "";
        }

        SQLExecutor se = getSqlExecutor();
        return (int) se.executeIntScalar(sql, 0);
    }

    @Override
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
            r.put("member_count", getMemberCount0(userId, (int)r.getInt("circle_id")));
        }

        Schemas.standardize(circleSchema, recs);
        if (withMembers) {
            final String SQL = "SELECT ${alias.friend} AS 'friend' FROM ${table}"
                    + " WHERE ${alias.user}=${v(user_id)} AND ${alias.circle}=${v(circle_id)} AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " AND reason<>" + Constants.FRIEND_REASON_DEFAULT_DELETE + "";
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


    @Override
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

        throw new FriendshipException("Generate circle id error");
    }

    @Override
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

    @Override
    protected boolean setFriend0(String userId, String friendId, String circleId, int reason, boolean isadd) {
        SQLExecutor se = getSqlExecutor();
        try {
            if (userId.equals(friendId)) {
                return true;
            }
            ArrayList<String> sqls = new ArrayList<String>();
            if (!isadd) {
                if (Integer.valueOf(circleId) == Constants.ADDRESS_BOOK_CIRCLE) {
                    sqls.add("UPDATE friend set reason=" + Constants.FRIEND_REASON_SOCIALCONTACT_DELETE + " where user=" + userId + " and friend=" + friendId + " and circle=" + Constants.ADDRESS_BOOK_CIRCLE + "");
                } else {
                    String thisSql = "select circle from friend where user=" + userId + " and friend=" + friendId + ""
                            + " AND circle<>" + BLOCKED_CIRCLE + ""
                            + " AND reason<>" + Constants.FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                            + " AND reason<>" + Constants.FRIEND_REASON_DEFAULT_DELETE + " "
                            + " AND circle<>" + Constants.ADDRESS_BOOK_CIRCLE + "";
                    SQLExecutor se1 = getSqlExecutor();
                    RecordSet recs = se1.executeRecordSet(thisSql, null);
                    if (recs.size() == 1) {
                        sqls.add("UPDATE friend set reason=" + Constants.FRIEND_REASON_SOCIALCONTACT_DELETE + " where user=" + userId + " and friend=" + friendId + " and circle=" + Constants.ADDRESS_BOOK_CIRCLE + "");
                    }
                    if (Integer.valueOf(circleId) == Constants.DEFAULT_CIRCLE) {
                        sqls.add("UPDATE friend set reason=" + Constants.FRIEND_REASON_DEFAULT_DELETE + " where user=" + userId + " and friend=" + friendId + " and circle=" + Constants.DEFAULT_CIRCLE + "");
                    }

                    if (Integer.valueOf(circleId) != Constants.ADDRESS_BOOK_CIRCLE && Integer.valueOf(circleId) != Constants.DEFAULT_CIRCLE) {
                        //if the lasted circle not Constants.ADDRESS_BOOK_CIRCLE, Constants.DEFAULT_CIRCLE，if yes ,delete from Constants.ADDRESS_BOOK_CIRCLE both
                        sqls.add("DELETE FROM friend where user=" + userId + " and friend=" + friendId + " and circle=" + circleId + "");
                    }
                }
                
                //history                
                long acted_time = DateUtils.nowMillis();
                sqls.add("INSERT INTO history VALUES(" + userId + ", 'delete_user_" + friendId + "', " + acted_time + ") ON DUPLICATE KEY UPDATE acted_time=" + acted_time);
            } else {
                if (Integer.valueOf(circleId) == Constants.DEFAULT_CIRCLE || Integer.valueOf(circleId) == Constants.ADDRESS_BOOK_CIRCLE) {
                    int this_circleId = 0;
                    int this_delete_reason = 0;
                    if (Integer.valueOf(circleId) == Constants.ADDRESS_BOOK_CIRCLE) {
                        this_circleId = Constants.ADDRESS_BOOK_CIRCLE;
                        this_delete_reason = Constants.FRIEND_REASON_SOCIALCONTACT_DELETE;
                        sqls.add("UPDATE request SET status=1, done_time=" + DateUtils.nowMillis() + " where user="+userId+" and source="+friendId+" and status=0");
                    }
                    if (Integer.valueOf(circleId) == Constants.DEFAULT_CIRCLE) {
                        this_circleId = Constants.DEFAULT_CIRCLE;
                        this_delete_reason = Constants.FRIEND_REASON_DEFAULT_DELETE;
                    }
                    String thisSql = "select circle from friend where user=" + userId + " and friend=" + friendId + " and circle=" + this_circleId + " AND reason=" + this_delete_reason + "";
                    SQLExecutor se1 = getSqlExecutor();
                    RecordSet recs = se1.executeRecordSet(thisSql, null);
                    if (recs.size() >= 1) {
                        if (reason == Constants.FRIEND_REASON_MANUALSELECT) {
                            sqls.add("UPDATE friend set reason=" + Constants.FRIEND_REASON_MANUALSELECT + ",created_time="+DateUtils.nowMillis()+" where user=" + userId + " and friend=" + friendId + " and circle=" + this_circleId + " AND reason=" + this_delete_reason + "");
                        }
                    } else {
                        sqls.add("INSERT INTO  friend (user,friend,circle,created_time,reason) values (" + userId + "," + friendId + "," + circleId + "," + DateUtils.nowMillis() + "," + reason + ")  ON DUPLICATE KEY update created_time=" + DateUtils.nowMillis() + "");
                    }
                }

                if (Integer.valueOf(circleId) != Constants.ADDRESS_BOOK_CIRCLE && Integer.valueOf(circleId) != Constants.DEFAULT_CIRCLE) {
                    if (Integer.valueOf(circleId) == BLOCKED_CIRCLE) {
                        sqls.add("delete from friend where user=" + userId + " and friend=" + friendId + " and circle<>" + BLOCKED_CIRCLE + " AND reason<>" + Constants.FRIEND_REASON_SOCIALCONTACT_DELETE + " AND reason<>" + Constants.FRIEND_REASON_DEFAULT_DELETE + "");
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
    
    @Override
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

    @Override
    protected Record getMyFriends0(String userId, String friendId) {
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
        return rec;
    }

    @Override
    protected RecordSet getBothFriendsIds0(String viewerId, String userId, int page, int count) {
        StringBuilder sql = new StringBuilder();
        sql.append(SQLTemplate.merge("SELECT distinct(${alias.friend}) FROM ${table} WHERE ${alias.circle}<>"+Constants.BLOCKED_CIRCLE+""
                + " AND ${alias.user}=${v(viewerId)}"
                + " AND ${alias.friend}<>"+qiupuUid+" "
                + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ",
                "alias", friendSchema.getAllAliases(),"viewerId", viewerId, "table", friendTable));
        sql.append(SQLTemplate.merge("AND ${alias.friend} IN (SELECT ${alias.friend} FROM ${table} WHERE ${alias.circle}<>"+Constants.BLOCKED_CIRCLE+""
                + " AND ${alias.user}=${v(userId)}"
                + " AND ${alias.friend}<>"+qiupuUid+" "
                + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + ") order by created_time desc ",
                  "alias", friendSchema.getAllAliases(), "userId", userId, "table", friendTable));
        sql.append(SQLTemplate.merge(" ${limit}",
                 "limit", SQLUtils.pageToLimit(page, count)));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
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

    @Override
    protected RecordSet getRelation0(String sourceUserId, String targetUserId, String circleId) {
        String sql;
        if(StringUtils.isBlank(circleId))
        {
            sql = new SQLBuilder.Select(friendSchema)
            .select("circle")
            .from(friendTable)
            .where("${alias.user}=${v(target)} AND ${alias.friend}=${v(source)}"
                    + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                    + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ",
                    "target", Long.parseLong(targetUserId), "source", Long.parseLong(sourceUserId))
            .toString();
        }
        else if (Integer.parseInt(circleId) == FRIENDS_CIRCLE) {
            sql = new SQLBuilder.Select(friendSchema)
                    .select("circle")
                    .from(friendTable)
                    .where("${alias.user}=${v(target)} AND ${alias.friend}=${v(source)} AND ${alias.circle}<>${v(blocked_circle)}"
                    + " AND reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                    + " AND reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ",
                            "target", Long.parseLong(targetUserId), "source", Long.parseLong(sourceUserId), "blocked_circle", BLOCKED_CIRCLE)
                    .toString();
        } 
        else {
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

    @Override
    protected boolean setRemark0(String userId, String friendId, String remark) {
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
        return true;
    }

    @Override
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

	@Override
	protected Record isDeleteRecent0(String userId, List<String> friendIds,
			long period) {
		Record result = new Record();
		for(String friend : friendIds)
		{
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
	    
	    for(Record rec : recs)
	    {
	    	String friend = StringUtils.substringAfter(rec.getString("action"), "delete_user_");
	    	boolean res = rec.getInt("count") > 0 ? true : false;
	    	result.put(friend, res);
	    }
		
	    return result;
	}

    @Override
    protected int getFollowersCount0(String userId) {
        String sql = "select count(*) from friend use index (friend) where friend=" + userId + " ";
        sql += " and circle<>" + BLOCKED_CIRCLE + "";
        sql += " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ";
        sql += " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ";

        SQLExecutor se = getSqlExecutor();
        return (int) se.executeIntScalar(sql, 0);
    }
    @Override
    protected int getFriendsCount0(String userId) {
        String sql = "select count(*) from friend where user=" + userId + " ";
        sql += " and circle<>" + BLOCKED_CIRCLE + "";
        sql += " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ";
        sql += " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ";
        sql += " group by friend";

        SQLExecutor se = getSqlExecutor();
        return (int) se.executeIntScalar(sql, 0);
    }

    @Override
    protected boolean getIfHeInMyCircles0(String my_id,String other_id,String circle_id) {
        String sql = "select * from friend where user='" + my_id + "' ";
        sql += " and friend='" + other_id + "' ";
        sql += " and circle='" + circle_id + "' ";
        sql += " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " ";
        sql += " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " ";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.size()>0;
    }

    @Override
    protected RecordSet getFriendOrFollowers0(String userIds, String byFriendOrFollowers) {
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
        return recs;
    }

    @Override
    protected RecordSet getAllRelation0(String viewerId, String userIds, String circleId, String inTheirOrInMine) {
        String sql = "";
        if (userIds.length() <= 0 || viewerId.length() <= 0)
            return new RecordSet();

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
        return recs;
    }

    @Override
    protected RecordSet topuserFollowers0(long userId,int limit) {
        String sql="select count(distinct(user)) as count1,friend from friend where friend not in (select friend from friend where user="+userId+" and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + " and circle<>" + BLOCKED_CIRCLE + ") group by friend limit "+limit+"";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        recs.sort("count1",false);
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

        return out0;
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

    @Override
    protected boolean deleteVirtualFriend0(String userId,String friendId,String name,String content) {
        SQLExecutor se = getSqlExecutor();
        String sql="delete from friend where type=1 and friend='"+friendId+"' and user='"+userId+"' and content='"+content+"' and name='"+name+"'";
        return se.executeUpdate(sql)>0;
    }

    @Override
    protected boolean setContactFriend0(String userId,String friendId,String fname, String content, String circleId, int reason, boolean isadd) {
        SQLExecutor se = getSqlExecutor();
        ArrayList<String> sqls = new ArrayList<String>();
        try {
            List<String> inCircle = ifExistContactInMyCircle0(userId, content);
            if (isadd) {
                if (!inCircle.contains(circleId)) {
                    String s = "INSERT INTO friend (user,friend,circle,created_time,reason,type,name,content) values ('" + userId + "','" + friendId + "','" + circleId + "'," + DateUtils.nowMillis() + "," + reason + ",'1','"+fname+"','"+content+"')";
                    sqls.add(s);
//                    sqls.add("update circle set member_count=member_count+1 where user='"+userId+"' and circle='"+circleId+"'");
                }
            } else {
                if (inCircle.contains(circleId)) {
                    sqls.add("delete from friend where user='"+userId+"' and type=1 and circle='"+circleId+"' and content='"+content+"'");
//                    sqls.add("update circleId set member_count=member_count-1 where user='"+userId+"' and circle='"+circleId+"'");
                }
            }
            se.executeUpdate(sqls);
        } finally {
        }
        return true;
    }

    @Override
    protected boolean createVirtualFriendId0(String userId,String friendId,String content,String name) {
        SQLExecutor se = getSqlExecutor();
        String sql="insert into virtual_friendid (virtual_friendid,content,name,user_id) values('"+friendId+"','"+content+"','"+name+"','"+userId+"')";
        return se.executeUpdate(sql)>0;
    }

    @Override
    protected boolean deleteVirtualFriendId0(String friendIds,String content) {
        SQLExecutor se = getSqlExecutor();
        String sql="delete from virtual_friendid where virtual_friendid in ("+friendIds+") and content='"+content+"'";
        return se.executeUpdate(sql)>0;
    }

    @Override
    protected RecordSet getVirtualFriendId0(String content) {
        SQLExecutor se = getSqlExecutor();
        String sql="select virtual_friendid from virtual_friendid where content='"+content+"'";
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }
    @Override
    protected RecordSet getVirtualFriendIdByName0(String userId,String name) {
        SQLExecutor se = getSqlExecutor();
        String sql="select virtual_friendid from virtual_friendid where user_id='"+userId+"' and name like '%"+name+"%'";
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getExistVirtualFriendId0(String userId,String virtualFriendId) {
        SQLExecutor se = getSqlExecutor();
        String sql="select * from friend where type=1 and user='"+userId+"' and friend='"+virtualFriendId+"'";
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected String getUserFriendhasVirtualFriendId0(String userId,String content) {
        SQLExecutor se = getSqlExecutor();
        String sql="select virtual_friendid from virtual_friendid where content='"+content+"' and user_id='"+userId+"'";
        Record rec = se.executeRecordSet(sql, null).getFirstRecord();
        return rec.isEmpty()?"0":rec.getString("virtual_friendid");
    }

    @Override
    protected boolean updateVirtualFriendIdToAct0(String friendId,String content) {
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = getVirtualFriendId0(content);
        String virtualFriendIds = recs.joinColumnValues("virtual_friendid",",") ;
        String sql="update friend set friend='"+friendId+"',type=0 where friend in ("+virtualFriendIds+") and type=1";
        se.executeUpdate(sql);
        deleteVirtualFriendId0(virtualFriendIds,content);
        String sql1="delete from friend where friend='"+friendId+"' and user=friend and type=0";
        se.executeUpdate(sql1);

        return true;
    }

    @Override
    protected RecordSet getContactFriendO(String userIds) {
        String sql = "select * from friend where user in (" + userIds + ") and type=1 order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

     @Override
    protected RecordSet getContactFriendByFidO(String friendIds) {
//        String sql = "select distinct(friend),name,content from friend where friend in (" + friendIds + ") and type=1 order by created_time desc";
//        SQLExecutor se = getSqlExecutor();
//        RecordSet recs = se.executeRecordSet(sql, null);
//        return recs;

        String sql = "select virtual_friendid,name,content from virtual_friendid where virtual_friendid in (" + friendIds + ")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }
}
