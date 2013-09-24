package com.borqs.server.platform.poll;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static com.borqs.server.service.platform.Constants.GROUP_ID_END;
import static com.borqs.server.service.platform.Constants.PUBLIC_CIRCLE_ID_BEGIN;

public class SimplePoll extends PollBase {
    private static final Logger L = LoggerFactory.getLogger(SimplePoll.class);

    private ConnectionFactory connectionFactory;
    private String db;

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("poll.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("poll.simple.db", null);
    }

    @Override
    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean savePoll(Record poll, RecordSet items) {
        Schemas.standardize(pollSchema, poll);

        ArrayList<String> sqls = new ArrayList<String>();
        final String SQL = "INSERT INTO ${table} ${values_join(alias, rec)}";
        String sql = SQLTemplate.merge(SQL,
                "table", "poll",
                "alias", pollSchema.getAllAliases(),
                "rec", poll);
        sqls.add(sql);

        for (Record item : items) {
            sql = SQLTemplate.merge(SQL,
                    "table", "poll_items",
                    "alias", itemSchema.getAllAliases(),
                    "rec", item);
            sqls.add(sql);
        }
        
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sqls);

        return n > 0;
    }

    @Override
    protected boolean vote0(String userId, long pollId, Record items) {
        String sql = "SELECT mode FROM poll WHERE id=" + pollId;
        SQLExecutor se = getSqlExecutor();
        long mode = se.executeIntScalar(sql, 0);
        if (mode == 2) {
            sql = "DELETE FROM poll_participants WHERE poll_id=" + pollId + " AND user=" + userId;
            se.executeUpdate(sql);
        }

        sql = "INSERT INTO poll_participants (poll_id, item_id, user, weight, created_time) VALUES ";
        for (Map.Entry<String, Object> entry : items.entrySet()) {
            String item = entry.getKey();
            long weight = ((Long)entry.getValue()).longValue();
            long now = DateUtils.nowMillis();

            sql += "(" + pollId + ", " + item + ", " + userId + ", " + weight + ", " + now + "), ";
        }
        
        sql = StringUtils.substringBeforeLast(sql, ",");
        if (mode == 1) {
            sql += " ON DUPLICATE KEY UPDATE poll_id=VALUES(poll_id), weight=VALUES(weight), created_time=VALUES(created_time)";
        }
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getPolls0(String pollIds) {
        String sql = "SELECT * FROM poll WHERE id IN (" + pollIds + ") AND destroyed_time=0 order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql, null);
    }

    @Override
    protected RecordSet getItemsByPollId0(long pollId) {
        String sql = "SELECT * FROM poll_items WHERE poll_id=" + pollId;
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        sql = "SELECT item_id, sum(weight) AS count FROM poll_participants WHERE poll_id=" + pollId + " group by item_id";
        RecordSet counts = se.executeRecordSet(sql, null);
        Record r = new Record();
        for (Record count : counts)
            r.put(count.getString("item_id"), count.getInt("count", 0));

        for (Record rec : recs) {
            String itemId = rec.getString("item_id");
            rec.put("count", r.getInt(itemId));
            
            sql = "SELECT user, created_time FROM poll_participants WHERE poll_id=" + pollId + " AND item_id=" + itemId;
            RecordSet participants = se.executeRecordSet(sql, null);
            participants.renameColumn("created_time", "voted_time");
            rec.put("participants", participants);
        }

        recs.removeColumns("poll_id");
        recs.renameColumn("item_id", "id");

        return recs;
    }

    @Override
    protected long hasVoted0(String userId, long pollId) {
        String sql = "SELECT sum(weight) FROM poll_participants WHERE poll_id=" + pollId + " AND user=" + userId;
        SQLExecutor se = getSqlExecutor();
        long n = se.executeIntScalar(sql, 0);
        
        return n;
    }

    @Override
    protected RecordSet getItemsByItemIds0(String itemIds) {
        String sql = "SELECT * FROM poll_items WHERE item_id IN (" + itemIds + ")";
        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql, null);
    }

    @Override
    protected Record getCounts0(String pollIds) {
        String sql = "SELECT poll_id, count(distinct user) AS count FROM poll_participants WHERE poll_id IN (" + pollIds + ") group by poll_id";
        SQLExecutor se = getSqlExecutor();
        RecordSet counts = se.executeRecordSet(sql, null);
        Record r = new Record();
        for (Record count : counts)
            r.put(count.getString("poll_id"), count.getInt("count", 0));
        
        return r;
    }

    private String pollListFilter(String sql, String viewerId, String userId, int page, int count) {
        sql += " AND (privacy=0";
        if (isFriend(userId, viewerId))
            sql += " OR privacy=1";
        sql += " OR (privacy=2 AND " + isTarget(viewerId) + ")";
        sql += ") order by created_time desc";
        sql += SQLTemplate.merge(" ${limit}",
                "limit", SQLUtils.pageToLimit(page, count));

        return sql;
    }
    
    @Override
    protected String getCreatedPolls0(String viewerId, String userId, int page, int count) {
        String sql = "SELECT id FROM poll WHERE source=" + userId + " AND destroyed_time=0";
        sql = pollListFilter(sql, viewerId, userId, page, count);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.joinColumnValues("id", ",");
    }

    @Override
    protected String getParticipatedPolls0(String viewerId, String userId, int page, int count) {
        String sql = "SELECT distinct poll_id AS poll_id FROM poll_participants WHERE user=" + userId + " order by created_time desc";
        
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        String pollIds = recs.joinColumnValues("poll_id", ",");
        sql = "SELECT id FROM poll WHERE id IN (" + pollIds + ") AND destroyed_time=0";
        sql = pollListFilter(sql, viewerId, userId, page, count);

        recs = se.executeRecordSet(sql, null);
        return recs.joinColumnValues("id", ",");
    }

    private String inTargetGroup(String userId) {
        String sql0 = "SELECT group_id FROM group_members WHERE member=" + userId + " AND destroyed_time=0";
        String sql = "SELECT id FROM group_ WHERE id IN (" + sql0  + ") AND destroyed_time=0";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        ArrayList<String> groupIds = new ArrayList<String>();
        for (Record rec : recs) {
            groupIds.add(rec.getString("id"));
        }

        String condition = "";

        for (String groupId : groupIds) {
            condition += " or instr(concat(',',target,','),concat(','," + groupId + ",','))>0";
        }

        return condition;
    }
    
    private boolean isFriend(String source, String viewerId) {
        if (StringUtils.equals(source, viewerId))
            return true;

        String sql = "SELECT count(*) AS count FROM friend WHERE user=" + source + " AND friend=" + viewerId + " and circle<>4 and reason<>5 and reason<>6";
        SQLExecutor se = getSqlExecutor();
        long count = se.executeIntScalar(sql, 0);
        return count > 0;
    }
    
    private String isTarget(String viewerId) {
        String sql0 = "SELECT group_id FROM group_members WHERE member=" + viewerId + " AND destroyed_time=0";
        String sql = "SELECT id FROM group_ WHERE id IN (" + sql0  + ") AND destroyed_time=0";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        ArrayList<String> groupIds = new ArrayList<String>();
        for (Record rec : recs) {
            groupIds.add("," + rec.getString("id") + ",");
        }
        groupIds.add("," + viewerId + ",");
        String arg = "'" + StringUtils2.joinIgnoreBlank("|", groupIds) + "'";
        return "concat(',',target,',') regexp concat(" + arg + ")";
    }
    
    @Override
    protected String getInvolvedPolls0(String viewerId, String userId, int page, int count) {
        String sql = "SELECT id FROM poll WHERE instr(concat(',',target,','),concat(','," + userId + ",','))>0" + inTargetGroup(userId) + " AND destroyed_time=0";

        long id = 0;
        try {
            id = Long.parseLong(userId);
            if (id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END) {
                sql = "SELECT id FROM poll WHERE instr(concat(',',target,','),concat(','," + userId + ",','))>0";
            }
        }
        catch (NumberFormatException nfe) {

        }

        sql = pollListFilter(sql, viewerId, userId, page, count);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.joinColumnValues("id", ",");
    }

    @Override
    protected String getFriendsPolls0(String viewerId, String userId, int sort, int page, int count) {
        String sql = "select distinct(friend) AS friend from friend where user=" + userId + " and circle<>4 and reason<>5 and reason<>6";
        SQLExecutor se = getSqlExecutor();
        RecordSet friends = se.executeRecordSet(sql, null);
        String friendIds = friends.joinColumnValues("friend", ",");

        if (StringUtils.isBlank(friendIds)) {
            return "";
        }
        else {
            sql = "SELECT id FROM poll WHERE source IN (" + friendIds + ") AND destroyed_time=0";
            sql = pollListFilter(sql, viewerId, userId, page, count);

            RecordSet recs = se.executeRecordSet(sql, null);
            return recs.joinColumnValues("id", ",");
        }
    }

    @Override
    protected String getPublicPolls0(String viewerId, String userId, int sort, int page, int count) {
        String sql = "SELECT id FROM poll WHERE destroyed_time=0";
        sql = pollListFilter(sql, viewerId, userId, page, count);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.joinColumnValues("id", ",");
    }

    @Override
    protected boolean deletePolls0(String viewerId, String pollIds) {
        String sql = "UPDATE poll SET destroyed_time=" + DateUtils.nowMillis() + " WHERE id IN (" + pollIds + ") AND source=" + viewerId;
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
}
