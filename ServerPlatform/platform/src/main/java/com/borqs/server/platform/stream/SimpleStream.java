package com.borqs.server.platform.stream;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.platform.conversation.ConversationBase;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.service.platform.Constants;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.borqs.server.service.platform.Constants.*;

public class SimpleStream extends StreamBase {
    private static final Logger L = LoggerFactory.getLogger(SimpleStream.class);
    private ConnectionFactory connectionFactory;
    private String db;
    private String streamTable = "stream";

    public SimpleStream() {
    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("stream.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("stream.simple.db", null);
        this.streamTable = conf.getString("stream.simple.streamTable", "stream");
    }

    @Override
    public void destroy() {
        this.streamTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean savePost(Record post) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, formPost)}";

        String sql = SQLTemplate.merge(SQL,
                "table", streamTable, "alias", streamSchema.getAllAliases(),
                "formPost", post);
        L.debug("long message,sql="+sql);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean disablePosts(String userId, List<String> postIds) {
        if (postIds.isEmpty())
            return false;

        boolean result = true;
        String sql0 = "SELECT post_id, source, mentions, privince FROM stream WHERE post_id IN (" + StringUtils2.joinIgnoreBlank(",", postIds) + ")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql0, null);
        
        String sql = "";
        ArrayList<String> sqls = new ArrayList<String>();
        for (Record rec : recs) {
            String postId = rec.getString("post_id");
            String source = rec.getString("source");
            String mentions = rec.getString("mentions");
            String privacy = rec.getString("privince");
            if (StringUtils.equals(userId, source)) {
                sql = "UPDATE stream SET destroyed_time=" + DateUtils.nowMillis() + " WHERE post_id=" + postId + " AND destroyed_time=0";
            } else {
                List<String> ml = StringUtils2.splitList(mentions, ",", true);
                List<String> groupIds = findGroupsFromUserIds(ml);
                if (groupIds.isEmpty()) {
                    result = false;
                    continue;
                }
                for (String groupId : groupIds) {
                    if (hasRight(Long.parseLong(groupId), Long.parseLong(userId), ROLE_ADMIN)) {
                        ml.remove(groupId);
                    }
                }
                String newMentions = StringUtils2.joinIgnoreBlank(",", ml);
                if (StringUtils.equals(mentions, newMentions)) {
                    result = false;
                    continue;
                } else if (StringUtils.isBlank(newMentions) && StringUtils.equals(privacy, "1")) {
                    sql = "UPDATE stream SET destroyed_time=" + DateUtils.nowMillis() + " WHERE post_id=" + postId + " AND destroyed_time=0";
                } else {
                    sql = "UPDATE stream SET mentions='" + newMentions + "' WHERE post_id=" + postId + " AND destroyed_time=0";
                }
            }

            sqls.add(sql);
        }
        
        long n = se.executeUpdate(sqls);
        return result && (n > 0);
    }

    @Override
    protected Record findPost(String postId, List<String> cols) {
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE ${alias.post_id}=${v(post_id)} AND destroyed_time=0";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "cols", cols,
                "table", streamTable,
                "post_id", Long.parseLong(postId));

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(streamSchema, rec);
        return rec;
    }

    @Override
    protected Record findPostTemp(String postId, List<String> cols) {
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE ${alias.post_id}=${v(post_id)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "cols", cols,
                "table", streamTable,
                "post_id", Long.parseLong(postId));

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(streamSchema, rec);
        return rec;
    }
    
    @Override
    protected RecordSet findWhoSharedApp0(String packageName, int limit) {
        final String SQL = "SELECT DISTINCT(source) FROM ${table} WHERE ${alias.type}="+APK_POST+" AND destroyed_time=0 AND target LIKE '%"+packageName+"%' ORDER BY created_time DESC LIMIT "+limit+"";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "table", streamTable);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }
    
    @Override
    protected RecordSet findWhoRetweetStream0(String target, int limit) {
    	final String SQL = "SELECT DISTINCT(source) FROM ${table} WHERE ${alias.quote}=${v(quote)} ORDER BY created_time DESC LIMIT " + limit;
    	String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "table", streamTable,
                "quote", target);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet findPosts(List<String> postIds, List<String> cols) {
        RecordSet recs = new RecordSet();
        for (String postId : postIds) {
            Record rec = findPost(postId, cols);
            if (!rec.isEmpty())
                recs.add(rec);
        }
        return recs;
    }

    @Override
    protected boolean updatePost(String userId, String postId, Record post) {
        Schemas.standardize(streamSchema, post);

        final String SQL = "UPDATE ${table} SET ${pair_join(formPost)}"
                + " WHERE destroyed_time = 0 AND ${alias.post_id}=${v(post_id)} AND ${alias.source}=${v(user_id)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", streamTable},
                {"formPost", post},
                {"alias", streamSchema.getAllAliases()},
                {"post_id", postId},
                {"user_id", userId},
        });
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    protected String sinceAndMaxSQL(Map<String, String> alias, long since, long max) {
        if (since < 0)
            since = 0;

        if (max <= 0)
            max = Long.MAX_VALUE;

        return SQLTemplate.merge(" ${alias.updated_time}>${v(since)} AND ${alias.updated_time}<${v(max)}",
                "alias", alias, "since", since, "max", max);
    }

    @Override
    protected RecordSet selectPosts(String sql) {
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }
    

    protected RecordSet getUsersPosts01(String viewerId, List<String> userIds, List<String> circleIds, List<String> cols, long since, long max, int type, String appId, int page, int count) {
        if (since < 0)
            since = 0;
        if (max <= 0)
            max = Long.MAX_VALUE;
        String sql = "";
        String tempSql = "1=1";
        if (viewerId.equals("") || viewerId.equals("0")) {
            //not login ,get public post
            sql = new SQLBuilder.Select(streamSchema)
                                    .select(cols)
                                    .from(streamTable)
                                    .where("destroyed_time = 0")
                                    .and("privince=0")
                                    .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                                    .and("${alias.type} & ${v(type)} <> 0", "type", type)
                                    .and("source in ("+StringUtils.join(userIds, ",")+")")
                                    .orderBy("created_time", "DESC")
                                    .limitByPage(page, count)
                                    .toString();
        } else {    //login
            if (userIds.size() == 1 && viewerId.equals(userIds.get(0).toString()))      //get 1 person and this guy is me
            {
                tempSql = "(source=" + viewerId + " or (source<>" + viewerId + " and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0))";

                sql = new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and(tempSql)
                        .andIf(userIds.isEmpty(), "1=2")
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and("${alias.type} & ${v(type)} <> 0", "type", type)
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            } else {    //get many person
                if (!userIds.isEmpty()) {
//                String user_id = StringUtils.join(userIds, ",");
                    tempSql = "(" +
                            "(privince = 0)" +
                            " or " +
                            "(privince=1 and (instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0  or source=" + viewerId + "))" +
                            ")";
                }
                if (circleIds.isEmpty()) //get ,by userIds    ,has login,
                {
                    if (userIds.isEmpty()) {
                        if (!viewerId.equals("") && !viewerId.equals("0")) { //has login    get public timeline    not by my friends
                            sql = new SQLBuilder.Select(streamSchema)
                                    .select(cols)
                                    .from(streamTable)
                                    .where("destroyed_time = 0")
                                    .and("privince=0")
                                    .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                                    .and("${alias.type} & ${v(type)} <> 0", "type", type)
                                    .and("source <> " + Integer.valueOf(viewerId) + " and source not in (select distinct(friend) from friend where user =" + Integer.valueOf(viewerId) + "  and circle<>4 and reason<>5 and reason<>6)")
                                    .orderBy("created_time", "DESC")
                                    .limitByPage(page, count)
                                    .toString();

                        }
//                        else {   //not login    get public timeline  not include my friends
//                            sql = new SQLBuilder.Select(streamSchema)
//                                    .select(cols)
//                                    .from(streamTable)
//                                    .where("destroyed_time = 0")
//                                    .and("privince=0")
//                                    .and("${alias.created_time} >= ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
//                                    .and("${alias.type} & ${v(type)} <> 0", "type", type)
//                                    .orderBy("created_time", "DESC")
//                                    .limitByPage(page, count)
//                                    .toString();
//                        }
                    } else {
                        sql = new SQLBuilder.Select(streamSchema)
                                .select(cols)
                                .from(streamTable)
                                .where("destroyed_time = 0")
                                .and("${alias.source} IN (${user_ids})", "user_ids", StringUtils.join(userIds, ","))
                                .and("((source in ("+StringUtils.join(userIds, ",")+") and privince = 0) or (instr(concat(',',mentions,','),concat(',',"+StringUtils.join(userIds, ",")+",','))>0 and source="+viewerId+") or (privince=0 and instr(concat(',',mentions,','),concat(',',"+StringUtils.join(userIds, ",")+",','))>0))")
                                .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                                .and("${alias.type} & ${v(type)} <> 0", "type", type)
                                .orderBy("created_time", "DESC")
                                .limitByPage(page, count)
                                .toString();
                    }

                } else           //get timeline by circles
                {
                    sql = new SQLBuilder.Select(streamSchema)
                            .select(cols)
                            .from(streamTable)
                            .where("destroyed_time = 0")
                            .andIf(!userIds.isEmpty(), "${alias.source} IN (${user_ids})", "user_ids", StringUtils.join(userIds, ","))
                            .and(tempSql)
                            .andIf(userIds.isEmpty(), "1=2")
                            .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                            .and("${alias.type} & ${v(type)} <> 0", "type", type)
                            .orderBy("created_time", "DESC")
                            .limitByPage(page, count)
                            .toString();
                }
            }

        }

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }

    private List<String> findGroupsFromUserIds(List<String> userIds) {
        ArrayList<String> groupIds = new ArrayList<String>();
        for (String userId : userIds) {
            long id = 0;
            try {
                id = Long.parseLong(userId);
            }
            catch (NumberFormatException nfe) {
                continue;
            }
            if (id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END)
                groupIds.add(String.valueOf(id));
        }
        return groupIds;
    }
    
    private String groupFilter(List<String> groupIds) {
        String condition = "";

        for (String groupId : groupIds) {
            condition += " or instr(concat(',',mentions,','),concat(','," + groupId + ",','))>0";
        }
        
        return condition;
    }

    private boolean hasRight(long groupId, long member, int minRole) {
        String sql = "SELECT role FROM group_members WHERE group_id=" + groupId + " AND member=" + member + " AND destroyed_time=0";
        SQLExecutor se = getSqlExecutor();
        int role = (int) se.executeIntScalar(sql, 0);
        return role >= minRole;
    }

    private boolean isGroupStreamPublic(long groupId) {
        String sql = "SELECT is_stream_public FROM group_ WHERE id=" + groupId + " AND destroyed_time=0";
        SQLExecutor se = getSqlExecutor();
        long isStreamPublic = se.executeIntScalar(sql, 1);
        return isStreamPublic == 1;
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
        return "concat(',',mentions,',') regexp concat(" + arg + ")";
    }

    @Override
    protected RecordSet getUsersPosts0(String viewerId, List<String> userIds, List<String> circleIds, List<String> cols, long since, long max, int type, String appId, int page, int count) {
        if (since < 0)
            since = 0;
        if (max <= 0)
            max = Long.MAX_VALUE;
        String sql = "";
        String tempSql = "1=1";
        //@@have not login，get publictimeline
        if (viewerId.equals("") || viewerId.equals("0")) {
            sql = new SQLBuilder.Select(streamSchema)
                    .select(cols)
                    .from(streamTable)
                    .where("destroyed_time = 0")
                    .and("privince=0")
                    .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                    .and("${alias.type} & ${v(type)} <> 0", "type", type)
                    .orderBy("created_time", "DESC")
                    .limitByPage(page, count)
                    .toString();
        } else {    //have login

            //  see mine
            //	1，all i have send
            //	2，all to me
            //	3，all Mention that me in stream，and Mention that me in comment
            if (userIds.size() == 1 && viewerId.equals(userIds.get(0).toString()))      //get 1 person and this guy is me
            {
                tempSql = "(" +
                            "source=" + viewerId + "" +
                        " or (source<>" + viewerId + " and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)" +
                        " or (instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                        ")";
                sql = new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and(tempSql)
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and("${alias.type} & ${v(type)} <> 0", "type", type)
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
            //see other ，only one person
            //  1，he send and public
            //	2，to him and public
            //	3，i share to him but privacy
            //	4，he share to me but privacy
            //	5，i mentioned him in stream and mentioned him in comment
            //	6，he mentioned me in stream and he mentioned me in comment
            if (userIds.size() == 1 && !viewerId.equals(userIds.get(0).toString()))      //get 1 person and this guy is not me
            {
                long groupId = Long.parseLong(userIds.get(0));
                if (groupId >= PUBLIC_CIRCLE_ID_BEGIN && groupId <= GROUP_ID_END
                        && !isGroupStreamPublic(groupId) && !hasRight(groupId, Long.parseLong(viewerId), ROLE_MEMBER)) {
                    return new RecordSet();
                } 
                else if (groupId >= PUBLIC_CIRCLE_ID_BEGIN && groupId <= GROUP_ID_END
                        && !isGroupStreamPublic(groupId) && hasRight(groupId, Long.parseLong(viewerId), ROLE_MEMBER)) {
                    tempSql = "(" +
                            "(source in (" + userIds.get(0) + ") and privince = 0)" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0 and source=" + viewerId + ")" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source="+userIds.get(0)+" and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)" +
                            " or (source="+viewerId+" and instr(concat(',',add_to,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source="+userIds.get(0)+" and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                            ")";
                }
                else if (groupId >= PUBLIC_CIRCLE_ID_BEGIN && groupId <= GROUP_ID_END
                        && isGroupStreamPublic(groupId)) {
                    tempSql = "(" +
                            "(source in (" + userIds.get(0) + ") and privince = 0)" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0 and source=" + viewerId + ")" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source="+userIds.get(0)+" and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)" +
                            " or (source="+viewerId+" and instr(concat(',',add_to,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source="+userIds.get(0)+" and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                            ")";
                }
                else {
                tempSql = "(" +
                        "(source in (" + userIds.get(0) + ") and privince = 0)" +
                        " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0 and source=" + viewerId + ")" +
                        " or (privince=0 and instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0)" +
                        " or (privince=1 and source="+userIds.get(0)+" and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)" +
                        " or (source="+viewerId+" and instr(concat(',',add_to,','),concat(','," + userIds.get(0) + ",','))>0)" +
                        " or (source="+userIds.get(0)+" and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                        " or (source=" + userIds.get(0) + " and " + isTarget(viewerId) + ")" +
                        ")";

                }
                sql = new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and(tempSql)
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and("${alias.type} & ${v(type)} <> 0", "type", type)
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
            //  who send public but not my friend
            if (userIds.size() == 0) {
                tempSql = "(source <> " + Integer.valueOf(viewerId) + " and privince=0 and source not in (select distinct(friend) from friend where user =" + Integer.valueOf(viewerId) + "  and circle<>4 and reason<>5 and reason<>6))";
                sql = new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and(tempSql)
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and("${alias.type} & ${v(type)} <> 0", "type", type)
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
            //see many men's ,get userIds     //see circles timeline,eventhough many men's timeline
            //  1，these guys'send,and public
            //	2，these guys'send,and privacy to me
            //	3，i have send
            //	4，these guys'send,and public,and mentioned me
            if (userIds.size() > 1) {
                if (circleIds.size() == 1 && circleIds.get(0).toString().equals("1")) {
                    //circle timeline
                    List<String> groupIds = findGroupsFromUserIds(userIds);
                    tempSql = "(" +
                            " (source IN (" + StringUtils.join(userIds, ",") + ") and (privince=0 or (privince=1 and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)) )" +
                            " or (source=" + viewerId + ")" +
                            " or (instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                            " or (source IN (" + StringUtils.join(userIds, ",") + ") and privince=0 and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0 )" +
                            groupFilter(groupIds) + ")";

                    sql = new SQLBuilder.Select(streamSchema)
                            .select(cols)
                            .from(streamTable)
                            .where("destroyed_time = 0")
                            .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                            .and("${alias.type} & ${v(type)} <> 0", "type", type)
                            .and(tempSql)
                            .orderBy("created_time", "DESC")
                            .limitByPage(page, count)
                            .toString();
                } else {
                    //all friend timeline
                    List<String> groupIds = findGroupsFromUserIds(userIds);
                    tempSql = "(" +
                            " (source IN (" + StringUtils.join(userIds, ",") + ") and (privince=0 or (privince=1 and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)) )" +
                            " or (source IN (" + StringUtils.join(userIds, ",") + ") and privince=0 and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0 )" +
                            groupFilter(groupIds) + ")";

                    sql = new SQLBuilder.Select(streamSchema)
                            .select(cols)
                            .from(streamTable)
                            .where("destroyed_time = 0")
                            .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                            .and("${alias.type} & ${v(type)} <> 0", "type", type)
                            .and(tempSql)
                            .orderBy("created_time", "DESC")
                            .limitByPage(page, count)
                            .toString();

                }
            }
        }

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet getPostsNearBy0(String viewerId, String cols, long since, long max, int type, String appId, int page, int count) {
        if (since < 0)
            since = 0;
        if (max <= 0)
            max = Long.MAX_VALUE;
        String sql = "";
        String tempSql = "1=1";
        //@@have not login，get publictimeline
         List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
        if (viewerId.equals("") || viewerId.equals("0")) {
            sql = new SQLBuilder.Select(streamSchema)
                    .select(cols0)
                    .from(streamTable)
                    .where("destroyed_time = 0")
                    .and("privince=0")
                    .and("length(location)>0")
                    .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                    .and("${alias.type} & ${v(type)} <> 0", "type", type)
                    .orderBy("created_time", "DESC")
                    .limitByPage(page, count)
                    .toString();
        } else {    //have login
            tempSql = "(" +
                    "source=" + viewerId + "" +
                    " or (source<>" + viewerId + " and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)" +
                    " or (instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                    ")";
            sql = new SQLBuilder.Select(streamSchema)
                    .select(cols0)
                    .from(streamTable)
                    .where("destroyed_time = 0")
                    .and(tempSql)
                    .and("length(location)>0")
                    .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                    .and("${alias.type} & ${v(type)} <> 0", "type", type)
                    .orderBy("created_time", "DESC")
                    .limitByPage(page, count)
                    .toString();
        }

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }

    @Override
    protected int getSharedCount0(String viewerId, String userId, int type) {
        String sql = "";
        String tempSql = "1=1";
        
        long id = 0;
        try {
            id = Long.parseLong(userId);
        } catch (Exception e) {

        }
        
        if (id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END) {
            sql = "select count(*) as sharedcount from stream where instr(concat(',',mentions,','),concat(','," + id + ",','))>0 and destroyed_time = 0 and type='" + type + "'";
        }
        else {
        if (viewerId.equals("") || viewerId.equals("0")) {
            sql = "select count(*) as sharedcount from stream where source='" + userId + "' and destroyed_time = 0 and privince=0 and type='" + type + "'";
        } else {    //have login
            if (viewerId.equals(userId.toString()))      //get 1 person and this guy is me
            {
                sql = "select count(*) as sharedcount from stream where source=" + viewerId + " and destroyed_time = 0 and type='" + type + "'";
            }
            if (!viewerId.equals(userId.toString()))      //get 1 person and this guy is not me
            {
                sql = "select count(*) as sharedcount from stream where source=" + userId + " and destroyed_time = 0 and type='" + type + "'" +
                        " and  (privince = 0 or (privince = 1 and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0))";
            }
        }
        }

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return Integer.valueOf(rec.getString("sharedcount"));
    }

    @Override
    protected RecordSet getMySharePosts0(String viewerId, List<String> userIds, List<String> cols, long since, long max, int type, String appId, int page, int count) {
        if (since < 0)
            since = 0;
        if (max <= 0)
            max = Long.MAX_VALUE;
        String sql = "";
        String tempSql = "1=1";
        
        List<String> groupIds = findGroupsFromUserIds(userIds);
        userIds.removeAll(groupIds);
        
        if (viewerId.equals("") || viewerId.equals("0")) {
            sql = new SQLBuilder.Select(streamSchema)
                    .select(cols)
                    .from(streamTable)
                    .where("destroyed_time = 0")
                    .and("source in (" + StringUtils.join(userIds, ",") + ")")
                    .and("privince=0")
                    .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                    .and(" type & "+type+"<>0 ")
                    .orderBy("created_time", "DESC")
                    .limitByPage(page, count)
                    .toString();
        } else {    //have login
            if (userIds.size() == 1 && viewerId.equals(userIds.get(0).toString()))      //get 1 person and this guy is me
            {
                tempSql = "(" +
                            "source=" + viewerId + "" +
                        ")";
                sql = new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and(tempSql)
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and(" type & "+type+"<>0 ")
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
            if (userIds.size() == 1 && !viewerId.equals(userIds.get(0).toString()))      //get 1 person and this guy is not me
            {
                tempSql = "(" +
                        "source in (" + userIds.get(0) + ") and " +
                        "(privince = 0 or (privince = 1 and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0))" +
                        ")";
                sql = new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and(tempSql)
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and(" type & "+type+"<>0 ")
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
            if (userIds.size() > 1) {
                tempSql = "(" +
                        " (source IN (" + StringUtils.join(userIds, ",") + ") and (privince=0 or (privince=1 and instr(concat(',',mentions,','),concat(',',"+viewerId+",','))>0)) )" +
                        ")";
                sql = new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and(" type & "+type+"<>0 ")
                        .and(tempSql)
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
        }
        for (String groupId : groupIds) {
            long gid = Long.parseLong(groupId);
            if (isGroupStreamPublic(gid)
                    || (!isGroupStreamPublic(gid) && hasRight(gid, Long.parseLong(viewerId), ROLE_MEMBER))) {
                tempSql = "(" +
                        "instr(concat(',',mentions,','),concat(','," + groupId + ",','))>0" +
                        ")";
                sql += " union all " + new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and(tempSql)
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and(" type & "+type+"<>0 ")
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
        }
        if (StringUtils.startsWith(sql, " union all ")) {
            sql = StringUtils.substringAfter(sql, "union all ");
        }
        L.debug("===###===sql="+sql.toString());
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        L.debug("===###===sql query and get recs="+recs.toString(false,false));
        return recs;
    }

    @Override
    protected boolean updateAttachments(String post_id, String Attachments) {
        final String SQL = "UPDATE ${table} SET ${alias.attachments}=${v(attachments)},${alias.updated_time}=${v(updated_time)}"
                + " WHERE ${alias.post_id}=${v(post_id)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", streamTable},
                {"alias", streamSchema.getAllAliases()},
                {"post_id", post_id},
                {"updated_time", DateUtils.nowMillis()},
                {"attachments", Attachments},
        });
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean updatePostFor0(String post_id, String newPost_id, String Attachments, long created_time, long updated_time) {
        final String SQL = "UPDATE ${table} SET ${alias.attachments}=${v(attachments)},${alias.created_time}=${v(created_time)},${alias.updated_time}=${v(updated_time)} "
                + " WHERE ${alias.post_id}=${v(post_id)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", streamTable},
                {"alias", streamSchema.getAllAliases()},
                {"post_id", post_id},
                {"created_time", created_time},
                {"updated_time", updated_time},
                {"attachments", Attachments},
        });
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0 ;
    }

    @Override
    protected boolean updatePostForAttachmentsAndUpdateTime0(String post_id, String Attachments, long updated_time) {
        final String SQL = "UPDATE ${table} SET ${alias.attachments}=${v(attachments)},${alias.updated_time}=${v(updated_time)},${alias.created_time}=${v(updated_time)} "
                + " WHERE ${alias.post_id}=${v(post_id)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", streamTable},
                {"alias", streamSchema.getAllAliases()},
                {"post_id", post_id},
                {"created_time", updated_time},
                {"updated_time", updated_time},
                {"attachments", Attachments},
        });
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean updatePostForCommentOrLike0(String post_id, String viewerId, String column, int value) {
        if (!column.equals("can_comment") && !column.equals("can_like") && !column.equals("can_reshare"))
            return false;
        final String SQL = "UPDATE ${table} SET " + column + "=" + value + ""
                + " WHERE ${alias.post_id}=${v(post_id)} AND source='" + viewerId + "'";
        final String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", streamTable},
                {"alias", streamSchema.getAllAliases()},
                {"post_id", post_id},
        });
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet topOneStreamByTarget0(int type, String target) {
        final String SQL = "SELECT ${alias.post_id} FROM ${table} WHERE ${alias.type}=${v(type)} AND destroyed_time=0 AND ${alias.target}=${v(target)} order by created_time desc limit 1";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "target", target,
                "table", streamTable,
                "type", type);
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet topOneStreamBySetFriend0(int type, String source,long created_time) {
        final String SQL = "SELECT * FROM ${table} WHERE ${alias.type}=${v(type)} AND destroyed_time=0 AND ${alias.source}=${v(source)} AND created_time>"+created_time+" and length(attachments)<6800 order by created_time desc limit 1";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "source", source,
                "table", streamTable,
                "type", type);
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet topOneStreamByShare0(int type, String source, String message,  String mentions,int privince, long dateDiff) {
        long max_created_time = DateUtils.nowMillis() - dateDiff;
        final String SQL = "SELECT * FROM ${table} WHERE ${alias.type}=${v(type)} AND destroyed_time=0" +
                " AND ${alias.source}=${v(source)} AND ${alias.mentions}=${v(mentions)} AND ${alias.privince}=${v(privince)}  AND ${alias.message}=${v(message)} " +
                " AND created_time>" + max_created_time + " and length(attachments) < 6800 order by created_time desc limit 1 ";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", streamTable},
                {"alias", streamSchema.getAllAliases()},
                {"source", source},
                {"mentions", mentions},
                {"message", message},
                {"privince", privince},
                {"type", type},});
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }
    
    @Override
    protected RecordSet myTopOneStreamByTarget0(String userId, int type, String target,List<String> cols) {
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE ${alias.source}=${v(userId)} AND ${alias.type}=${v(type)} AND destroyed_time=0 AND ${alias.target}=${v(target)} order by created_time desc limit 1";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", streamTable},
                    {"alias", streamSchema.getAllAliases()},
                    {"cols", cols},
                    {"target", target},
                    {"userId", userId},
                    {"type", type},});
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }

    @Override
    protected boolean touch0(String postId) {
        final String SQL = "UPDATE ${table} SET ${alias.updated_time}=${v(now)} WHERE ${alias.post_id}=${v(post_id)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "table", streamTable,
                "now", DateUtils.nowMillis(),
                "post_id", Long.parseLong(postId));
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet topSendStreamUser0(int limit) {
        final String sql = "SELECT COUNT(post_id) AS COUNT1,source FROM stream group by source order by COUNT1 DESC limit "+limit+"";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getApkSharedToMe0(String viewerId, String userIds,boolean tome,String packageName,int page,int count) {
        List<String> cols0 = StringUtils2.splitList(toStr("target,source,mentions"), ",", true);
        final String sql = new SQLBuilder.Select(streamSchema)
                    .select(cols0)
                    .from("stream")
                    .where("destroyed_time = 0")
                    .and("type="+APK_POST+"")
                    .and("target<>''")
                    .andIf(!userIds.isEmpty(), "source IN ("+userIds+")")
                    .andIf(!packageName.isEmpty(), "instr(target,'"+packageName+"')>0")
                    .andIf(tome,"(instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)")
                    .orderBy("created_time", "DESC")
                    .limitByPage(page, count)
                    .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getSharedByType0(String userIds,int type,String cols,int page,int count) {
        List<String> cols0 = StringUtils2.splitList(cols, ",", true);
        final String sql = new SQLBuilder.Select(streamSchema)
                .select(cols0)
                .from("stream")
                .where("destroyed_time = 0")
                .and("type=" + type + "")
                .and("privince=0")
                .andIf(!userIds.isEmpty(), "source IN (" + userIds + ")")
                .orderBy("created_time", "DESC")
                .limitByPage(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getSharedPost0(String viewerId, String postId) {
        final String sql = "select post_id,source,privince,created_time,quote from stream where destroyed_time = 0 and quote='"+postId+"' order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getSharedPostHasContact11(String contact) {
        final String sql = "select post_id,source,mentions,add_contact,has_contact from stream where has_contact=1 and (instr(concat(',',add_contact,','),concat(',','," + contact + ",',','))>0) order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getSharedPostHasContact12(String virtual_friendId) {
        final String sql = "select post_id,source,mentions,add_contact,has_contact from stream where has_contact=1 and (instr(concat(',',mentions,','),concat(',','," + virtual_friendId + ",',','))>0) order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected boolean updatePostHasContact12(String postId,String newMentions,String newAddContact,boolean newHasContact) {
        final String sql = "update stream set mentions='"+newMentions+"',add_contact='"+newAddContact+"',has_contact="+newHasContact+" where post_id='"+postId+"'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }


    @Override
    protected RecordSet formatOldDataToConversation0(String viewerId) {
        String sql = "select source,created_time,post_id,quote,mentions from stream where quote>0";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schema conversationSchema = Schema.loadClassPath(ConversationBase.class, "conversation.schema");

        for (Record r : recs) {
            //1,查询所有没有进入conversation的stream，塞进去
            if (!ifExistConversation0(Constants.POST_OBJECT, r.getString("quote"), Constants.C_STREAM_RESHARE, Long.parseLong(r.getString("source")))) {

                Record conversation = new Record();
                conversation.put("target_type", Constants.POST_OBJECT);
                conversation.put("target_id", r.getString("quote"));
                conversation.put("reason", Constants.C_STREAM_RESHARE);
                conversation.put("from_", r.getString("source"));
                conversation.put("created_time", r.getString("created_time"));

                String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                String sql1 = SQLTemplate.merge(SQL1,
                        "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                        "conversation", conversation);
                se.executeUpdate(sql1);
            }
        }

        return recs;
    }

    @Override
    protected RecordSet formatOldDataToConversation1(String viewerId) {
        String sql = "select source,created_time,post_id,mentions from stream";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schema conversationSchema = Schema.loadClassPath(ConversationBase.class, "conversation.schema");

        for (Record r : recs) {
            //1,查询所有没有进入conversation的stream，塞进去
            if (!ifExistConversation0(Constants.POST_OBJECT, r.getString("post_id"), Constants.C_STREAM_POST, Long.parseLong(r.getString("source")))) {

                Record conversation = new Record();
                conversation.put("target_type", Constants.POST_OBJECT);
                conversation.put("target_id", r.getString("post_id"));
                conversation.put("reason", Constants.C_STREAM_POST);
                conversation.put("from_", r.getString("source"));
                conversation.put("created_time", r.getString("created_time"));

                String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                String sql1 = SQLTemplate.merge(SQL1,
                        "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                        "conversation", conversation);
                se.executeUpdate(sql1);
            }

            //2，查询所有针对stream的评论
            String target = "2:" + r.getString("post_id");
            String sql2 = "select comment_id,target,created_time,commenter from comment where destroyed_time=0 and target='" + target + "'";
            RecordSet recs_comment = se.executeRecordSet(sql2, null);
            for (Record comment : recs_comment) {
                if (!ifExistConversation0(Constants.POST_OBJECT, r.getString("post_id"), Constants.C_STREAM_COMMENT, Long.parseLong(comment.getString("commenter")))) {

                    Record conversation = new Record();
                    conversation.put("target_type", Constants.POST_OBJECT);
                    conversation.put("target_id", r.getString("post_id"));
                    conversation.put("reason", Constants.C_STREAM_COMMENT);
                    conversation.put("from_", comment.getString("commenter"));
                    conversation.put("created_time", comment.getString("created_time"));

                    String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                    String sql1 = SQLTemplate.merge(SQL1,
                            "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                            "conversation", conversation);
                    se.executeUpdate(sql1);
                }
            }

            //3，查询所有针对stream的like
            String sql3 = "select target,liker,created_time from like_ where target='" + target + "'";
            RecordSet recs_like = se.executeRecordSet(sql3, null);
            for (Record like : recs_like) {
                if (!ifExistConversation0(Constants.POST_OBJECT, r.getString("post_id"), Constants.C_STREAM_LIKE, Long.parseLong(like.getString("liker")))) {

                    Record conversation = new Record();
                    conversation.put("target_type", Constants.POST_OBJECT);
                    conversation.put("target_id", r.getString("post_id"));
                    conversation.put("reason", Constants.C_STREAM_LIKE);
                    conversation.put("from_", like.getString("liker"));
                    conversation.put("created_time", like.getString("created_time"));

                    String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                    String sql1 = SQLTemplate.merge(SQL1,
                            "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                            "conversation", conversation);
                    se.executeUpdate(sql1);
                }

            }
            //4，查询所有针对stream的to
            String mentions = r.getString("mentions");
            if (mentions.length() > 0) {
                List<String> m = StringUtils2.splitList(toStr(mentions), ",", true);
                for (String m0 : m) {
                    if (isInteger(m0)) {
                        if (!ifExistConversation0(Constants.POST_OBJECT, r.getString("post_id"), Constants.C_STREAM_TO, Long.parseLong(m0))) {

                            Record conversation = new Record();
                            conversation.put("target_type", Constants.POST_OBJECT);
                            conversation.put("target_id", r.getString("post_id"));
                            conversation.put("reason", Constants.C_STREAM_TO);
                            conversation.put("from_", m0);
                            conversation.put("created_time", r.getString("created_time"));

                            String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                            String sql1 = SQLTemplate.merge(SQL1,
                                    "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                                    "conversation", conversation);
                            se.executeUpdate(sql1);
                        }
                    }
                }

            }
        }
        //5,把所有的comment加进去     ，comment本身要创建conversation

        String sql5 = "select comment_id,target,created_time,commenter from comment where destroyed_time=0";
        RecordSet recs_c = se.executeRecordSet(sql5, null);
        for (Record comment : recs_c) {
                if (!ifExistConversation0(Constants.COMMENT_OBJECT, comment.getString("comment_id"), Constants.C_COMMENT_CREATE, Long.parseLong(comment.getString("commenter")))) {

                    Record conversation = new Record();
                    conversation.put("target_type", Constants.COMMENT_OBJECT);
                    conversation.put("target_id", comment.getString("comment_id"));
                    conversation.put("reason", Constants.C_COMMENT_CREATE);
                    conversation.put("from_", comment.getString("commenter"));
                    conversation.put("created_time", comment.getString("created_time"));

                    String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                    String sql1 = SQLTemplate.merge(SQL1,
                            "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                            "conversation", conversation);
                    se.executeUpdate(sql1);
                }
            }

        return recs;
    }

    @Override
    protected RecordSet formatOldDataToConversation2(String viewerId) {
        String sql = "select source,created_time,post_id,mentions,target from stream where type=32";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schema conversationSchema = Schema.loadClassPath(ConversationBase.class, "conversation.schema");

        for (Record r : recs) {
            //1,查询所有没有进入conversation的stream，塞进去
            String apkId = r.getString("target");
            String a[] = StringUtils2.splitArray(apkId, "-", true);
            String packageName = apkId;
            if (a.length > 1) {
                packageName = a[0];
            }
            if (packageName.length() > 0) {
                if (!ifExistConversation0(Constants.APK_OBJECT, packageName, Constants.C_APK_SHARE, Long.parseLong(r.getString("source")))) {

                    Record conversation = new Record();
                    conversation.put("target_type", Constants.APK_OBJECT);
                    conversation.put("target_id", packageName);
                    conversation.put("reason", Constants.C_APK_SHARE);
                    conversation.put("from_", r.getString("source"));
                    conversation.put("created_time", r.getString("created_time"));

                    String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                    String sql1 = SQLTemplate.merge(SQL1,
                            "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                            "conversation", conversation);
                    se.executeUpdate(sql1);
                }

                //2，查询所有针对stream的评论
                String target = "4:" + packageName;
                String sql2 = "select comment_id,target,created_time,commenter from comment where destroyed_time=0 and target='" + target + "'";
                RecordSet recs_comment = se.executeRecordSet(sql2, null);
                for (Record comment : recs_comment) {
                    if (!ifExistConversation0(Constants.APK_OBJECT, packageName, Constants.C_APK_COMMENT, Long.parseLong(comment.getString("commenter")))) {

                        Record conversation = new Record();
                        conversation.put("target_type", Constants.APK_OBJECT);
                        conversation.put("target_id", packageName);
                        conversation.put("reason", Constants.C_APK_COMMENT);
                        conversation.put("from_", comment.getString("commenter"));
                        conversation.put("created_time", comment.getString("created_time"));

                        String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                        String sql1 = SQLTemplate.merge(SQL1,
                                "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                                "conversation", conversation);
                        se.executeUpdate(sql1);
                    }
                }

                //3，查询所有针对stream的like
                String sql3 = "select target,liker,created_time from like_ where target='" + target + "'";
                RecordSet recs_like = se.executeRecordSet(sql3, null);
                for (Record like : recs_like) {
                    if (!ifExistConversation0(Constants.APK_OBJECT, packageName, Constants.C_APK_LIKE, Long.parseLong(like.getString("liker")))) {

                        Record conversation = new Record();
                        conversation.put("target_type", Constants.APK_OBJECT);
                        conversation.put("target_id", packageName);
                        conversation.put("reason", Constants.C_APK_LIKE);
                        conversation.put("from_", like.getString("liker"));
                        conversation.put("created_time", like.getString("created_time"));

                        String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                        String sql1 = SQLTemplate.merge(SQL1,
                                "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                                "conversation", conversation);
                        se.executeUpdate(sql1);
                    }

                }
                //4，查询所有针对stream的to
                String mentions = r.getString("mentions");
                if (mentions.length() > 0) {
                    List<String> m = StringUtils2.splitList(toStr(mentions), ",", true);
                    for (String m0 : m) {
                        if (isInteger(m0)) {
                            if (!ifExistConversation0(Constants.APK_OBJECT, packageName, Constants.C_APK_TO, Long.parseLong(m0))) {

                                Record conversation = new Record();
                                conversation.put("target_type", Constants.APK_OBJECT);
                                conversation.put("target_id", packageName);
                                conversation.put("reason", Constants.C_APK_TO);
                                conversation.put("from_", m0);
                                conversation.put("created_time", r.getString("created_time"));

                                String SQL1 = "INSERT INTO ${table} ${values_join(alias, conversation)}";
                                String sql1 = SQLTemplate.merge(SQL1,
                                        "table", "conversation_", "alias", conversationSchema.getAllAliases(),
                                        "conversation", conversation);
                                se.executeUpdate(sql1);
                            }
                        }
                    }

                }
            }

        }

        return recs;
    }

   protected boolean ifExistConversation0(int target_type, String target_id, int reason, long from) {
        final String SQL = "SELECT reason FROM conversation_ WHERE target_type='"+target_type+"'" +
                " AND target_id='"+target_id+"'" +
                " AND reason='"+reason+"'" +
                " AND from_='"+from+"'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(SQL, null);
        return recs.size()>0;
    }

    public boolean isInteger(String s) {
        boolean ko = true;
        if (s == null || s.equals(""))
            return false;
        else {
            for (int i = 0; i < s.length() && ko; i++) {
                if (s.charAt(i) > '9' || s.charAt(i) < '0')
                    ko = false;
            }
            return ko;
        }
    }

   protected boolean formatLocation(String sql) {
//        sql = "SELECT post_id,location from stream where length(location)>0";
//        SQLExecutor se = getSqlExecutor();
//        RecordSet recs = se.executeRecordSet(sql, null);
//
//        for (Record r : recs){
//            String longitude = Constants.parseLocation(r.getString("location"), "longitude");
//            String latitude = Constants.parseLocation(r.getString("location"), "latitude");
//            String post_id = r.getString("post_id");
//            String sql1 = "update stream set longitude='" + longitude + "',latitude='" + latitude + "' where post_id=" + post_id;
//            se.executeUpdate(sql1);
//        }
//        return true;
       final String SQL = "SELECT post_id,attachments FROM stream WHERE type=2";
       SQLExecutor se = getSqlExecutor();
       RecordSet recs = se.executeRecordSet(SQL, null);
       for (Record rec : recs) {
           RecordSet att = RecordSet.fromJson(rec.getString("attachments"));
           if (att.size() > 0) {
               for (Record r : att) {
                   r.put("photo_img_original", r.getString("photo_img_middle"));
               }
           }
           String sql1 = "update stream set attachments='" + att.toString() + "' where post_id='" + rec.getString("post_id") + "'";
           se.executeUpdate(sql1);
       }
       return true;
   }

    @Override
    protected boolean getPhoto0(String viewerId, String photo_id) {
        boolean visible = true;
        final String sql = "select * from photo where photo_id='" + photo_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        if (recs.size()<=0) {
            return visible;
        }

        int K = 0;
        for (Record rec : recs) {
            if (rec.getInt("destroyed_time") > 0)
                K += 1;
        }
        if (K == recs.size()) {
            visible = false;
            return visible;
        }

        boolean has_me = false;
        for (Record rec : recs) {
            if (rec.getString("user_id").equals(viewerId)) {
                has_me = true;
                break;
            }
        }

        String sql00 = "select privince from stream where destroyed_time=0 and post_id='" + recs.getFirstRecord().getString("stream_id") + "'";
        Record rec_post = se.executeRecord(sql00, null);
        int privacy = rec_post.isEmpty() ? 0 : (int) rec_post.getInt("privince");

        if (privacy == 0) {
            if (has_me) {     //有我一份     说明分享给多个人了
                for (Record rec : recs) {
                    if (rec.getString("user_id").equals(viewerId)) {
                        long destroyed_time = rec.getInt("destroyed_time");
                        if (destroyed_time > 0) {
                            visible = false;
                            break;
                        }
                    }
                }
            }
        }

        if (privacy == 1) {
            if (has_me) {     //有我一份
                for (Record rec : recs) {
                    if (rec.getString("user_id").equals(viewerId)) {
                        long destroyed_time = rec.getInt("destroyed_time");
                        if (destroyed_time > 0) {
                            visible = false;
                            break;
                        }
                    }
                }
            }
        }
        return visible;
    }

    @Override
    protected boolean getFile0(String viewerId, String file_id) {
        boolean visible = true;
        final String sql = "select * from static_file where file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        if (recs.size()<=0) {
            return visible;
        }

        int K = 0;
        for (Record rec : recs) {
            if (rec.getInt("destroyed_time") > 0)
                K += 1;
        }
        if (K == recs.size()) {
            visible = false;
            return visible;
        }

        boolean has_me = false;
        for (Record rec : recs) {
            if (rec.getString("user_id").equals(viewerId)) {
                has_me = true;
                break;
            }
        }

        String sql00 = "select privince from stream where destroyed_time=0 and post_id='" + recs.getFirstRecord().getString("stream_id") + "'";
        Record rec_post = se.executeRecord(sql00, null);
        int privacy = rec_post.isEmpty() ? 0 : (int) rec_post.getInt("privince");

        if (privacy == 0) {
            if (has_me) {     //有我一份     说明分享给多个人了
                for (Record rec : recs) {
                    if (rec.getString("user_id").equals(viewerId)) {
                        long destroyed_time = rec.getInt("destroyed_time");
                        if (destroyed_time > 0) {
                            visible = false;
                            break;
                        }
                    }
                }
            }
        }

        if (privacy == 1) {
            if (has_me) {     //有我一份
                for (Record rec : recs) {
                    if (rec.getString("user_id").equals(viewerId)) {
                        long destroyed_time = rec.getInt("destroyed_time");
                        if (destroyed_time > 0) {
                            visible = false;
                            break;
                        }
                    }
                }
            }
        }
        return visible;
    }

    @Override
    protected Record getVideo0(String viewerId, String file_id) {
        final String sql = "select * from video where file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }

    @Override
    protected Record getAudio0(String viewerId, String file_id) {
        final String sql = "select * from audio where file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }

    @Override
    protected Record getStaticFile0(String viewerId, String file_id) {
        final String sql = "select * from static_file where file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }

    @Override
    protected RecordSet getAppliesToUser0(String viewerId, String appId, String userId, String cols) throws AvroRemoteException {
        final String sql = new SQLBuilder.Select(streamSchema)
                .select(StringUtils2.splitArray(cols, ",", true))
                .from(streamTable)
                .where("app=${app_id}", "app_id", appId)
                .and("destroyed_time=0")
                .and("type&${v(apply_type)}<>0", "apply_type", Constants.APPLY_POST)
                .and("instr(concat(',',mentions,','),concat(',',${v(user_id)},','))>0", "user_id", userId)
                .orderBy("created_time", "asc")
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }
}

