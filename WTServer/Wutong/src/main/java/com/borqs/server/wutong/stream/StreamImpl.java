package com.borqs.server.wutong.stream;


import com.borqs.server.ServerException;
import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.qiupu.QiupuLogic;
import com.borqs.server.qiupu.QiupuLogics;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.account2.UserScopeNames;
import com.borqs.server.wutong.comment.CommentLogic;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.email.EmailLogic;
import com.borqs.server.wutong.favorite.FavoriteLogic;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.like.LikeLogic;
import com.borqs.server.wutong.notif.FileSharedNotifSender;
import com.borqs.server.wutong.notif.PhotoSharedNotifSender;
import com.borqs.server.wutong.notif.SharedAppNotifSender;
import com.borqs.server.wutong.notif.SharedNotifSender;
import com.borqs.server.wutong.page.PageLogic;
import com.borqs.server.wutong.page.PageLogicUtils;
import com.borqs.server.wutong.reportabuse.ReportAbuseLogic;
import com.borqs.server.wutong.search.PostDoc;
import com.borqs.server.wutong.search.SearchLogic;
import com.borqs.server.wutong.tag.TagLogic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.borqs.server.base.util.StringUtils2.joinIgnoreBlank;

public class StreamImpl implements StreamLogic, Initializable {
    public final Schema streamSchema = Schema.loadClassPath(StreamImpl.class, "stream.schema");
    private static final Logger L = Logger.getLogger(StreamImpl.class);
    private ConnectionFactory connectionFactory;
    private String db;
    private String streamTable = "stream";
    private static Configuration conf;
    private String prefix;
    private ExecutorService pool;

    public StreamImpl() {
    }

    @Override
    public void init() {
        conf = GlobalConfig.get();

        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.streamTable = conf.getString("stream.simple.streamTable", "stream");
        this.prefix = conf.getString("platform.fileUrlPattern", "http://oss.aliyuncs.com/");

        this.pool = Executors.newCachedThreadPool();
    }

    public void destroy() {
        this.streamTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
        prefix = null;

        pool.shutdown();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    public boolean savePost(Context ctx, Record post) {
        final String METHOD = "savePost";
        L.traceStartCall(ctx, METHOD, post);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, formPost)}";

        String sql = SQLTemplate.merge(SQL,
                "table", streamTable, "alias", streamSchema.getAllAliases(),
                "formPost", post);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean disablePosts(Context ctx, String userId, List<String> postIds) {
        final String METHOD = "disablePosts";
        L.traceStartCall(ctx, METHOD, userId, postIds.toString());
        if (postIds.isEmpty())
            return false;

        boolean result = true;
        String sql0 = "SELECT post_id, source, mentions, privince FROM stream WHERE post_id IN (" + joinIgnoreBlank(",", postIds) + ")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql0, null);
        L.debug(ctx, "sql executeRecordSet result=" + recs);
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
                List<String> groupIds = findGroupsFromUserIds(ctx, ml);
                if (groupIds.isEmpty()) {
                    result = false;
                    continue;
                }
                for (String groupId : groupIds) {
                    if (hasRight(ctx, Long.parseLong(groupId), Long.parseLong(userId), Constants.ROLE_ADMIN)) {
                        ml.remove(groupId);
                    }
                }
                String newMentions = joinIgnoreBlank(",", ml);
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
        L.traceEndCall(ctx, METHOD);
        return result && (n > 0);
    }

    public Record findPost(Context ctx, String postId, List<String> cols) {
        final String METHOD = "findPost";
        L.traceStartCall(ctx, METHOD, postId, cols.toString());
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE ${alias.post_id}=${v(post_id)} AND destroyed_time=0";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "cols", cols,
                "table", streamTable,
                "post_id", Long.parseLong(postId));

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(streamSchema, rec);
        L.traceEndCall(ctx, METHOD);
        return rec;
    }

    public Record findPostTemp(Context ctx, String postId, List<String> cols) {
        final String METHOD = "findPostTemp";
        L.traceStartCall(ctx, METHOD, postId, cols.toString());
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE ${alias.post_id}=${v(post_id)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "cols", cols,
                "table", streamTable,
                "post_id", Long.parseLong(postId));

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(streamSchema, rec);
        L.traceEndCall(ctx, METHOD);
        return rec;
    }

    public RecordSet findWhoSharedApp(Context ctx, String packageName, int limit) {
        final String METHOD = "findWhoSharedApp";
        L.traceStartCall(ctx, METHOD, packageName, limit);
        final String SQL = "SELECT DISTINCT(source) FROM ${table} WHERE ${alias.type}=" + Constants.APK_POST + " AND destroyed_time=0 AND target LIKE '%" + packageName + "%' ORDER BY created_time DESC LIMIT " + limit + "";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "table", streamTable);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    public RecordSet findWhoRetweetStream(Context ctx, String target, int limit) {
        final String METHOD = "findWhoRetweetStream";
        L.traceStartCall(ctx, METHOD, target, limit);
        final String SQL = "SELECT DISTINCT(source) FROM ${table} WHERE ${alias.quote}=${v(quote)} ORDER BY created_time DESC LIMIT " + limit;
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "table", streamTable,
                "quote", target);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet findPosts(Context ctx, List<String> postIds, List<String> cols) {
        final String METHOD = "findPosts";
        ElapsedCounter ec = ctx.getElapsedCounter();
        //ec.record("find Posts start");
        L.traceStartCall(ctx, METHOD, postIds.toString(), cols.toString());
        RecordSet recs = new RecordSet();
        for (String postId : postIds) {
            Record rec = findPost(ctx, postId, cols);
            if (!rec.isEmpty())
                recs.add(rec);
        }
        //ec.record("find Posts end");
        L.traceEndCall(ctx, METHOD);
        return recs;
    }


    public boolean updatePost0(Context ctx, String userId, String postId, Record post) {
        final String METHOD = "updatePost0";
        L.traceStartCall(ctx, METHOD, userId, postId, post);
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
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public String sinceAndMaxSQL(Context ctx, Map<String, String> alias, long since, long max) {
        final String METHOD = "sinceAndMaxSQL";
        L.traceStartCall(ctx, METHOD, alias.toString(), since, max);
        if (since < 0)
            since = 0;

        if (max <= 0)
            max = Long.MAX_VALUE;

        String s = SQLTemplate.merge(" ${alias.updated_time}>${v(since)} AND ${alias.updated_time}<${v(max)}",
                "alias", alias, "since", since, "max", max);
        L.traceEndCall(ctx, METHOD);
        return s;
    }


    public RecordSet selectPosts(Context ctx, String sql) {
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        return recs;
    }


    public RecordSet getUsersPosts01(Context ctx, String viewerId, List<String> userIds, List<String> circleIds, List<String> cols, long since, long max, int type, String appId, int page, int count) {
        final String METHOD = "getUsersPosts01";
        L.traceStartCall(ctx, METHOD, userIds.toString(), circleIds.toString(), cols.toString(), since, max, type, appId, page, count);
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
                    .and("source in (" + StringUtils.join(userIds, ",") + ")")
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
                                .and("((source in (" + StringUtils.join(userIds, ",") + ") and privince = 0) or (instr(concat(',',mentions,','),concat(','," + StringUtils.join(userIds, ",") + ",','))>0 and source=" + viewerId + ") or (privince=0 and instr(concat(',',mentions,','),concat(','," + StringUtils.join(userIds, ",") + ",','))>0))")
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public List<String> findGroupsFromUserIds(Context ctx, List<String> userIds) {
        final String METHOD = "findGroupsFromUserIds";
        L.traceStartCall(ctx, METHOD, userIds.toString());
        ArrayList<String> groupIds = new ArrayList<String>();
        for (String userId : userIds) {
            long id = 0;
            try {
                id = Long.parseLong(userId);
            } catch (NumberFormatException nfe) {
                continue;
            }
            if (id >= Constants.PUBLIC_CIRCLE_ID_BEGIN && id <= Constants.GROUP_ID_END)
                groupIds.add(String.valueOf(id));
        }
        L.traceEndCall(ctx, METHOD);
        return groupIds;
    }

    public String sceneFilter(String viewerId, String groupId) {
        return " or (source=" + viewerId + " and scene=" + groupId + ")" +
                " or (instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0 and scene=" + groupId + ")" +
                " or (instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0 and scene=" + groupId + ")" +
                " or (privince=0 and scene=" + groupId + ")";
    }

    public String groupFilter(Context ctx, List<String> groupIds) {
        String viewerId = ctx.getViewerIdString();
        String condition = "";

        for (String groupId : groupIds) {
            condition += " or instr(concat(',',mentions,','),concat(','," + groupId + ",','))>0";
            condition += sceneFilter(viewerId, groupId);
        }

        return condition;
    }

    public String pageFilter(Context ctx, List<Long> pageIds) {
        StringBuilder buff = new StringBuilder();

        for (long pageId : pageIds) {
            buff.append(" or instr(concat(',',mentions,','),concat(',',").append(pageId).append(",','))>0");
        }

        return buff.toString();
    }

    public boolean hasRight(Context ctx, long groupId, long member, int minRole) {
        final String METHOD = "hasRight";
        L.traceStartCall(ctx, METHOD, groupId, member, minRole);
        String sql = "SELECT role FROM group_members WHERE group_id=" + groupId + " AND member=" + member + " AND destroyed_time=0";
        SQLExecutor se = getSqlExecutor();
        int role = (int) se.executeIntScalar(sql, 0);
        L.traceEndCall(ctx, METHOD);
        return role >= minRole;
    }

    public boolean isGroupStreamPublic(Context ctx, long groupId) {
        final String METHOD = "isGroupStreamPublic";
        L.traceStartCall(ctx, METHOD, groupId);
        String sql = "SELECT is_stream_public FROM group_ WHERE id=" + groupId + " AND destroyed_time=0";
        SQLExecutor se = getSqlExecutor();
        long isStreamPublic = se.executeIntScalar(sql, 1);
        L.traceEndCall(ctx, METHOD);
        return isStreamPublic == 1;
    }

    @Override
    public RecordSet getRelatedPosts0(Context ctx, String userId, List<String> cols, long since, long max, int type, String appId, int page, int count, List<String> groupIds) {
        final String METHOD = "getRelatedPosts";
        L.traceStartCall(ctx, METHOD, userId, cols, since, max, type, appId, page, count);
        if (since < 0)
            since = 0;
        if (max <= 0)
            max = Long.MAX_VALUE;

        SQLExecutor se = getSqlExecutor();
        String sql0 =  "select cast(substring(target, 3) as unsigned) as post_id from comment where instr(concat(',',add_to,','), concat(','," + userId + ",','))>0";
        RecordSet recs0 = se.executeRecordSet(sql0, null);
        String postIds = recs0.joinColumnValues("post_id", ",");
        String commentAddto = "";
        if (StringUtils.isNotBlank(postIds)) {
            commentAddto = " or post_id in (" + postIds + ")";
        }

        String tempSql = "(" +
                "source=" + userId + "" +
                " or (source<>" + userId + " and instr(concat(',',mentions,','),concat(','," + userId + ",','))>0)" +
                " or (instr(concat(',',add_to,','),concat(','," + userId + ",','))>0)" +
                commentAddto +
                ")";

        if (CollectionUtils.isNotEmpty(groupIds)) {
            String groupFilterSql = StringUtils.substringAfter(groupFilter(ctx, groupIds), "or");
            tempSql += " and (" + groupFilterSql + ")";
        }

        String sql = new SQLBuilder.Select(streamSchema)
                .select(cols)
                .from(streamTable)
                .where("destroyed_time = 0")
                .and(tempSql)
                .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                .and("${alias.type} & ${v(type)} <> 0", "type", type)
                .orderBy("created_time", "DESC")
                .limitByPage(page, count)
                .toString();


        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getUsersPosts0(Context ctx, String viewerId, List<String> userIds, List<String> circleIds, List<String> cols, long since, long max, int type, String appId, int page, int count) {
        final String METHOD = "getUsersPosts0";
        L.traceStartCall(ctx, METHOD, userIds.toString(), circleIds.toString(), cols.toString(), since, max, type, appId, page, count);

        String scene = (String)(ctx.getSession("scene") == null?'0':ctx.getSession("scene"));
        Boolean from_home =(Boolean)(ctx.getSession("from_home") == null?Boolean.FALSE:ctx.getSession("from_home"));

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
                        ") and scene = "+scene;//TODO ---------------- add sence ----------------
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
                if (groupId >= Constants.PUBLIC_CIRCLE_ID_BEGIN && groupId <= Constants.GROUP_ID_END
                        && !isGroupStreamPublic(ctx, groupId) && !hasRight(ctx, groupId, Long.parseLong(viewerId), Constants.ROLE_MEMBER)) {
                    return new RecordSet();
                } else if (groupId >= Constants.PUBLIC_CIRCLE_ID_BEGIN && groupId <= Constants.GROUP_ID_END
                        && !isGroupStreamPublic(ctx, groupId) && hasRight(ctx, groupId, Long.parseLong(viewerId), Constants.ROLE_MEMBER)) {
                    tempSql = "(" +
                            "(source in (" + userIds.get(0) + ") and privince = 0)" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0 and source=" + viewerId + ")" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source=" + userIds.get(0) + " and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)" +
                            " or (source=" + viewerId + " and instr(concat(',',add_to,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source=" + userIds.get(0) + " and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                            sceneFilter(viewerId, userIds.get(0)) +
                            ")";

                } else if (groupId >= Constants.PUBLIC_CIRCLE_ID_BEGIN && groupId <= Constants.GROUP_ID_END
                        && isGroupStreamPublic(ctx, groupId)) {
                    tempSql = "(" +
                            "(source in (" + userIds.get(0) + ") and privince = 0)" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0 and source=" + viewerId + ")" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source=" + userIds.get(0) + " and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)" +
                            " or (source=" + viewerId + " and instr(concat(',',add_to,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source=" + userIds.get(0) + " and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                            sceneFilter(viewerId, userIds.get(0)) +
                            ")";

                } else {
                    tempSql = "(" +
                            "(source in (" + userIds.get(0) + ") and privince = 0)" +
                            " or (instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0 and source=" + viewerId + ")" +
                            " or (privince=0 and instr(concat(',',mentions,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (privince=1 and source=" + userIds.get(0) + " and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)" +
                            " or (source=" + viewerId + " and instr(concat(',',add_to,','),concat(','," + userIds.get(0) + ",','))>0)" +
                            " or (source=" + userIds.get(0) + " and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                            " or (source=" + userIds.get(0) + " and " + isTarget(ctx, viewerId) + ")" +
                            ") and scene = "+scene;//TODO ---------------- add sence ----------------

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
                    List<String> groupIds = findGroupsFromUserIds(ctx, userIds);
                    List<Long> pageIds = PageLogicUtils.getPageIdsFromMentions(ctx, userIds);
                    tempSql = "(" +
                            " (source IN (" + StringUtils.join(userIds, ",") + ") and (privince=0 or (privince=1 and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)) )" +
                            " or (source=" + viewerId + ")" +
                            " or (instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0)" +
                            " or (source IN (" + StringUtils.join(userIds, ",") + ") and privince=0 and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0 )" +
                            groupFilter(ctx, groupIds) +
                            pageFilter(ctx, pageIds) + ")";

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
                    List<String> groupIds = findGroupsFromUserIds(ctx, userIds);
                    List<Long> pageIds = PageLogicUtils.getPageIdsFromMentions(ctx, userIds);
                    tempSql = "(" +
                            " (source IN (" + StringUtils.join(userIds, ",") + ") and (privince=0 or (privince=1 and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)) )" +
                            " or (source IN (" + StringUtils.join(userIds, ",") + ") and privince=0 and instr(concat(',',add_to,','),concat(','," + viewerId + ",','))>0 )" +
                            groupFilter(ctx, groupIds) +
                            pageFilter(ctx, pageIds) + ")";

                    sql = new SQLBuilder.Select(streamSchema)
                            .select(cols)
                            .from(streamTable)
                            .where("destroyed_time = 0")
                            .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                            .and("${alias.type} & ${v(type)} <> 0", "type", type)
                            .and(tempSql)
                            .andIf(!from_home,"mentions<>''")
                            .orderBy("created_time", "DESC")
                            .limitByPage(page, count)
                            .toString();

                }
            }
        }

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getPostsNearBy0(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count) {
        final String METHOD = "getPostsNearBy0";
        L.traceStartCall(ctx, METHOD, viewerId, cols, since, max, type, appId, page, count);
        if (since < 0)
            since = 0;
        if (max <= 0)
            max = Long.MAX_VALUE;
        String sql = "";
        String tempSql = "1=1";
        //@@have not login，get publictimeline
        List<String> cols0 = StringUtils2.splitList(cols, ",", true);
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public int getSharedCount(Context ctx, String viewerId, String userId, int type) {
        final String METHOD = "getSharedCount";
        L.traceStartCall(ctx, METHOD, userId, type);
        String sql = "";
        String tempSql = "1=1";

        long id = 0;
        try {
            id = Long.parseLong(userId);
        } catch (Exception e) {

        }

        if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN && id <= Constants.GROUP_ID_END)
                || (Constants.getUserTypeById(id) == Constants.PAGE_OBJECT)) {
            sql = "select count(*) as sharedcount from stream where instr(concat(',',mentions,','),concat(','," + id + ",','))>0 and destroyed_time = 0 and type='" + type + "'";
        } else {
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
        int c = Integer.valueOf(rec.getString("sharedcount"));
        L.traceEndCall(ctx, METHOD);
        return c;
    }

    public RecordSet getSharedCountAll(Context ctx, String viewerId, String userId) {
            final String METHOD = "getSharedCount";
            L.traceStartCall(ctx, METHOD, userId);
            String sql = "";
            String tempSql = "1=1";

            long id = 0;
            try {
                id = Long.parseLong(userId);
            } catch (Exception e) {

            }

            if (id >= Constants.PUBLIC_CIRCLE_ID_BEGIN && id <= Constants.GROUP_ID_END) {
                sql = "select post_id,type from stream where instr(concat(',',mentions,','),concat(','," + id + ",','))>0 and destroyed_time = 0 ";
            } else {
                if (viewerId.equals("") || viewerId.equals("0")) {
                    sql = "select post_id,type from stream where source='" + userId + "' and destroyed_time = 0 and privince=0 ";
                } else {    //have login
                    if (viewerId.equals(userId.toString()))      //get 1 person and this guy is me
                    {
                        sql = "select post_id,type from stream where source=" + viewerId + " and destroyed_time = 0 ";
                    }
                    if (!viewerId.equals(userId.toString()))      //get 1 person and this guy is not me
                    {
                        sql = "select post_id,type from stream where source=" + userId + " and destroyed_time = 0 " +
                                " and  (privince = 0 or (privince = 1 and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0))";
                    }
                }
            }

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            L.traceEndCall(ctx, METHOD);
            return recs;
        }

    public String isTarget(Context ctx, String viewerId) {
        final String METHOD = "isTarget";
        L.traceStartCall(ctx, METHOD);
        String sql0 = "SELECT group_id FROM group_members WHERE member=" + viewerId + " AND destroyed_time=0";
        String sql = "SELECT id FROM group_ WHERE id IN (" + sql0 + ") AND destroyed_time=0";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        ArrayList<String> groupIds = new ArrayList<String>();
        for (Record rec : recs) {
            groupIds.add("," + rec.getString("id") + ",");
        }
        groupIds.add("," + viewerId + ",");
        String arg = "'" + StringUtils2.joinIgnoreBlank("|", groupIds) + "'";
        String s = "concat(',',mentions,',') regexp concat(" + arg + ")";
        L.traceEndCall(ctx, METHOD);
        return s;
    }

    public RecordSet getMySharePosts0(Context ctx, String viewerId, List<String> userIds, List<String> cols, long since, long max, int type, String appId, int page, int count) {
        final String METHOD = "getMySharePosts0";
        L.traceStartCall(ctx, METHOD, userIds.toString(), cols.toString(), since, max, type, appId, page, count);
        if (since < 0)
            since = 0;
        if (max <= 0)
            max = Long.MAX_VALUE;
        String sql = "";
        String tempSql = "1=1";

        List<String> groupIds = findGroupsFromUserIds(ctx, userIds);
        userIds.removeAll(groupIds);

        if (viewerId.equals("") || viewerId.equals("0")) {
            sql = new SQLBuilder.Select(streamSchema)
                    .select(cols)
                    .from(streamTable)
                    .where("destroyed_time = 0")
                    .and("source in (" + StringUtils.join(userIds, ",") + ")")
                    .and("privince=0")
                    .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                    .and(" type & " + type + "<>0 ")
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
                        .and(" type & " + type + "<>0 ")
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
                        .and(" type & " + type + "<>0 ")
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
            if (userIds.size() > 1) {
                tempSql = "(" +
                        " (source IN (" + StringUtils.join(userIds, ",") + ") and (privince=0 or (privince=1 and instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)) )" +
                        ")";
                sql = new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and(" type & " + type + "<>0 ")
                        .and(tempSql)
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
        }
        for (String groupId : groupIds) {
            long gid = Long.parseLong(groupId);
            if (isGroupStreamPublic(ctx, gid)
                    || (!isGroupStreamPublic(ctx, gid) && hasRight(ctx, gid, Long.parseLong(viewerId), Constants.ROLE_MEMBER))) {
                tempSql = "(" +
                        "instr(concat(',',mentions,','),concat(','," + groupId + ",','))>0" +
                        ")";
                sql += " union all " + new SQLBuilder.Select(streamSchema)
                        .select(cols)
                        .from(streamTable)
                        .where("destroyed_time = 0")
                        .and(tempSql)
                        .and("${alias.created_time} > ${v(since)} AND ${alias.created_time} < ${v(max)}", "since", since, "max", max)
                        .and(" type & " + type + "<>0 ")
                        .orderBy("created_time", "DESC")
                        .limitByPage(page, count)
                        .toString();
            }
        }
        if (StringUtils.startsWith(sql, " union all ")) {
            sql = StringUtils.substringAfter(sql, "union all ");
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public boolean updateAttachment(Context ctx, String post_id, String Attachments) {
        final String METHOD = "updateAttachment";
        L.traceStartCall(ctx, METHOD, post_id, Attachments);
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
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean updatePostFor0(Context ctx, String post_id, String newPost_id, String Attachments, long created_time, long updated_time) {
        final String METHOD = "updatePostFor0";
        L.traceStartCall(ctx, METHOD, post_id, newPost_id, Attachments, created_time, updated_time);
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
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean updatePostForAttachmentsAndUpdateTime(Context ctx, String post_id, String Attachments, long updated_time) {
        final String METHOD = "updatePostForAttachmentsAndUpdateTime";
        L.traceStartCall(ctx, METHOD, post_id, Attachments, updated_time);
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
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean updatePostForCommentOrLike(Context ctx, String post_id, String viewerId, String column, int value) {
        final String METHOD = "updatePostForCommentOrLike";
        L.traceStartCall(ctx, METHOD, post_id, viewerId, column, value);
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
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public RecordSet topOneStreamByTarget0(Context ctx, int type, String target) {
        final String METHOD = "topOneStreamByTarget0";
        L.traceStartCall(ctx, METHOD, type, target);
        final String SQL = "SELECT ${alias.post_id} FROM ${table} WHERE ${alias.type}=${v(type)} AND destroyed_time=0 AND ${alias.target}=${v(target)} order by created_time desc limit 1";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "target", target,
                "table", streamTable,
                "type", type);
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet topOneStreamBySetFriend0(Context ctx, int type, String source, long created_time) {
        final String METHOD = "topOneStreamBySetFriend0";
        L.traceStartCall(ctx, METHOD, type, source, created_time);
        final String SQL = "SELECT * FROM ${table} WHERE ${alias.type}=${v(type)} AND destroyed_time=0 AND ${alias.source}=${v(source)} AND created_time>" + created_time + " and length(attachments)<6800 order by created_time desc limit 1";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "source", source,
                "table", streamTable,
                "type", type);
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(streamSchema, recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet topOneStreamByShare(Context ctx, int type, String source, String message, String mentions, int privince, long dateDiff) {
        final String METHOD = "topOneStreamByShare";
        L.traceStartCall(ctx, METHOD, type, source, message, mentions, privince, dateDiff);
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet myTopOneStreamByTarget0(Context ctx, String userId, int type, String target, List<String> cols) {
        final String METHOD = "myTopOneStreamByTarget0";
        L.traceStartCall(ctx, METHOD, userId, type, target, cols.toString());
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public boolean touch(Context ctx, String postId) {
        final String METHOD = "touch";
        L.traceStartCall(ctx, METHOD, postId);
        final String SQL = "UPDATE ${table} SET ${alias.updated_time}=${v(now)} WHERE ${alias.post_id}=${v(post_id)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", streamSchema.getAllAliases(),
                "table", streamTable,
                "now", DateUtils.nowMillis(),
                "post_id", Long.parseLong(postId));
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public RecordSet topSendStreamUser(Context ctx, int limit) {
        final String METHOD = "topSendStreamUser";
        L.traceStartCall(ctx, METHOD, limit);
        final String sql = "SELECT COUNT(post_id) AS COUNT1,source FROM stream group by source order by COUNT1 DESC limit " + limit + "";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getApkSharedToMe(Context ctx, String viewerId, String userIds, boolean tome, String packageName, int page, int count) {
        final String METHOD = "getApkSharedToMe";
        L.traceStartCall(ctx, METHOD, userIds, tome, packageName, page, count);
        List<String> cols0 = StringUtils2.splitList("target,source,mentions", ",", true);
        final String sql = new SQLBuilder.Select(streamSchema)
                .select(cols0)
                .from("stream")
                .where("destroyed_time = 0")
                .and("type=" + Constants.APK_POST + "")
                .and("target<>''")
                .andIf(!userIds.isEmpty(), "source IN (" + userIds + ")")
                .andIf(!packageName.isEmpty(), "instr(target,'" + packageName + "')>0")
                .andIf(tome, "(instr(concat(',',mentions,','),concat(','," + viewerId + ",','))>0)")
                .orderBy("created_time", "DESC")
                .limitByPage(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getSharedByType(Context ctx, String userIds, int type, String cols, int page, int count) {
        final String METHOD = "getSharedByType";
        L.traceStartCall(ctx, METHOD, type, cols, page, count);
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getSharedPost(Context ctx, String viewerId, String postId) {
        final String METHOD = "getSharedPost";
        L.traceStartCall(ctx, METHOD, viewerId, postId);
        final String sql = "select post_id,source,privince,created_time,quote from stream where destroyed_time = 0 and quote='" + postId + "' order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getSharedPostHasContact1(Context ctx, String contact) {
        final String METHOD = "getSharedPostHasContact1";
        L.traceStartCall(ctx, METHOD, contact);
        final String sql = "select post_id,source,mentions,add_contact,has_contact from stream where has_contact=1 and (instr(concat(',',add_contact,','),concat(',','," + contact + ",',','))>0) order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }


    public RecordSet getSharedPostHasContact2(Context ctx, String virtual_friendId) {
        final String METHOD = "getSharedPostHasContact2";
        L.traceStartCall(ctx, METHOD, virtual_friendId);
        final String sql = "select post_id,source,mentions,add_contact,has_contact from stream where has_contact=1 and (instr(concat(',',mentions,','),concat(',','," + virtual_friendId + ",',','))>0) order by created_time desc";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }


    public boolean updatePostHasContact2(Context ctx, String postId, String newMentions, String newAddContact, boolean newHasContact) {
        final String METHOD = "updatePostHasContact2";
        L.traceStartCall(ctx, METHOD, postId, newMentions, newAddContact, newHasContact);
        final String sql = "update stream set mentions='" + newMentions + "',add_contact='" + newAddContact + "',has_contact=" + newHasContact + " where post_id='" + postId + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }


    public RecordSet formatOldDataToConversation0(Context ctx, String viewerId) {
        final String METHOD = "formatOldDataToConversation0";
        L.traceStartCall(ctx, METHOD);
        String sql = "select source,created_time,post_id,quote,mentions from stream where quote>0";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        for (Record r : recs) {
            //1,查询所有没有进入conversation的stream，塞进去
            if (!ifExistConversation0(ctx, Constants.POST_OBJECT, r.getString("quote"), Constants.C_STREAM_RESHARE, Long.parseLong(r.getString("source")))) {
                String sql1 = "INSERT INTO conversation_ (target_type,target_id,reason,from_created_time) values" +
                        " ('" + Constants.POST_OBJECT + "','" + r.getString("quote") + "','" + Constants.C_STREAM_RESHARE + "','" + r.getString("source") + "','" + r.getString("created_time") + "')";
                se.executeUpdate(sql1);
            }
        }

        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public boolean ifExistConversation0(Context ctx, int target_type, String target_id, int reason, long from) {
        final String METHOD = "ifExistConversation0";
        L.traceStartCall(ctx, METHOD, target_type, target_id, reason, from);
        final String SQL = "SELECT reason FROM conversation_ WHERE target_type='" + target_type + "'" +
                " AND target_id='" + target_id + "'" +
                " AND reason='" + reason + "'" +
                " AND from_='" + from + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(SQL, null);
        L.traceEndCall(ctx, METHOD);
        return recs.size() > 0;
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

    public boolean formatLocation(Context ctx, String sql) {
        final String METHOD = "formatLocation";
        L.traceStartCall(ctx, METHOD, sql);
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
        L.traceEndCall(ctx, METHOD);
        return true;
    }

    public boolean getPhoto(Context ctx, String viewerId, String photo_id) {
        final String METHOD = "getPhoto";
        L.traceStartCall(ctx, METHOD, photo_id);
        boolean visible = true;
        final String sql = "select * from photo where photo_id='" + photo_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        if (recs.size() <= 0) {
            return false;
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
        L.debug(ctx, "get all photos for photo 's stream_id" + recs);
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
        L.traceEndCall(ctx, METHOD);
        return visible;
    }

    public boolean getFile(Context ctx, String viewerId, String file_id) {
        final String METHOD = "getFile";
        L.traceStartCall(ctx, METHOD, file_id);
        boolean visible = true;
        final String sql = "select * from static_file where file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        if (recs.size() <= 0) {
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
        L.debug(ctx, "get all files for file 's stream_id" + recs);
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
        L.traceEndCall(ctx, METHOD);
        return visible;
    }

    public Record getVideo(Context ctx, String viewerId, String file_id) {
        final String METHOD = "getVideo";
        L.traceStartCall(ctx, METHOD, file_id);
        final String sql = "select * from video where file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        L.traceEndCall(ctx, METHOD);
        return rec;
    }

    public Record getAudio0(Context ctx, String viewerId, String file_id) {
        final String METHOD = "getAudio0";
        L.traceStartCall(ctx, METHOD, file_id);
        final String sql = "select * from audio where file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        L.traceEndCall(ctx, METHOD);
        return rec;
    }

    public Record getStaticFile(Context ctx, String viewerId, String file_id) {
        final String METHOD = "getStaticFile";
        L.traceStartCall(ctx, METHOD, file_id);
        final String sql = "select * from static_file where file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        L.traceEndCall(ctx, METHOD);
        return rec;
    }

    public RecordSet getAppliesToUser(Context ctx, String viewerId, String appId, String userId, String cols) {
        final String METHOD = "getAppliesToUser";
        L.traceStartCall(ctx, METHOD, appId, userId, cols);
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
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    //===========================================================================================
    public static String genPostId() {
        return Long.toString(RandomUtils.generateId());
    }

    private static Record addPostIdStrCol(Record rec) {
        if (rec != null) {
            if (rec.has("post_id") && !rec.has("post_id_s"))
                rec.put("post_id_s", rec.getString("post_id"));
            if (rec.has("quote") && !rec.has("quote_s"))
                rec.put("quote_s", rec.getString("quote"));
        }
        return rec;
    }

    private static RecordSet addPostIdsStrCol(RecordSet recs) {
        if (recs != null) {
            for (Record rec : recs)
                addPostIdStrCol(rec);
        }
        return recs;
    }

    public String createPost(Context ctx, String userId, Record post0) {
        final String METHOD = "createPost";
        L.traceStartCall(ctx, METHOD, userId, post0);
        String userId0 = userId;
        Schemas.checkRecordIncludeColumns(post0, "type", "message");

        long now = DateUtils.nowMillis();
        post0.put("created_time", now);
        post0.put("updated_time", now);
        post0.putMissing("quote", 0L);
        post0.putMissing("root", 0L);
        post0.putMissing("app", Constants.NULL_APP_ID);
        post0.putMissing("attachments", "[]");
        post0.putMissing("app_data", "");
        post0.putMissing("can_comment", true);
        post0.putMissing("can_like", true);
        post0.putMissing("can_reshare", true);
        post0.putMissing("destroyed_time", 0);
        post0.putMissing("device", "");

        String postId = genPostId();
        post0.put("post_id", postId);
        post0.put("source", userId0);
        if (post0.getString("location").length() > 0) {
            String longitude = Constants.parseLocation(post0.getString("location"), "longitude");
            String latitude = Constants.parseLocation(post0.getString("location"), "latitude");
            post0.put("longitude", longitude);
            post0.put("latitude", latitude);
        }
        if (post0.getString("attachments").length()>2){
            RecordSet recs_temp = RecordSet.fromJson(post0.getString("attachments")) ;
            post0.put("attachments",recs_temp.toString());
        }


        Schemas.standardize(streamSchema, post0);
        L.debug(ctx, "create post for post0=" + post0);
        boolean b = savePost(ctx, post0);
        if (!b)
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "Save formPost error");

        addPostDocForSearch(ctx, post0);
        L.traceEndCall(ctx, METHOD);
        return postId;
    }

    private static void addPostDocForSearch(Context ctx, Record post) {
        SearchLogic search = GlobalLogics.getSearch();
        try {



            /*
            {
  "message" : "Jah wow",
  "app" : 1,
  "attachments" : [ ],
  "type" : 1,
  "target" : "",
  "device" : "Apache-HttpClient/4.1.1 (java 1.5)",
  "app_data" : "",
  "mentions" : "",
  "privince" : false,
  "location" : "",
  "can_comment" : true,
  "can_like" : true,
  "can_reshare" : true,
  "add_to" : "",
  "add_contact" : "",
  "has_contact" : 0,
  "post_source" : 1,
  "created_time" : 1362565062127,
  "updated_time" : 1362565062127,
  "quote" : 0,
  "root" : 0,
  "destroyed_time" : 0,
  "post_id" : 2857506045170898575,
  "source" : 10012
}
             */
            ArrayList<Long> userIds = new ArrayList<Long>();
            ArrayList<Long> groupIds = new ArrayList<Long>();
            userIds.add(post.getInt("source"));
            for (long id : StringUtils2.splitIntArray(post.getString("mentions"), ",")) {
                if (id > 0 && id < Constants.GROUP_ID_BEGIN) {
                    userIds.add(id);
                } else if (id >= Constants.GROUP_ID_BEGIN && id < Constants.GROUP_ID_END) {
                    groupIds.add(id);
                }
            }
            for (long id : StringUtils2.splitIntArray(post.getString("add_to"), ",")) {
                if (id > 0 && id < Constants.GROUP_ID_BEGIN)
                    userIds.add(id);
            }

            RecordSet userRecs = GlobalLogics.getAccount().getUsersBaseColumns(ctx, StringUtils.join(userIds, ","));
            RecordSet groupRecs = GlobalLogics.getGroup().getSimpleGroups(ctx, Constants.GROUP_ID_BEGIN, Constants.GROUP_ID_END, StringUtils.join(groupIds, ","), "id, name");
            HashMap<Long, String> userNames = new HashMap<Long, String>();
            for (Record rec : userRecs) {
                userNames.put(rec.getInt("user_id"), rec.getString("display_name"));
            }
            HashMap<Long, String> groupNameMap = new HashMap<Long, String>();
            for (Record rec : groupRecs) {
                groupNameMap.put(rec.getInt("id"), rec.getString("name"));
            }
            ArrayList<String> groupNames = new ArrayList<String>(groupNameMap.values());

            long postId = post.getInt("post_id");

            long createdTime = post.getInt("created_time");
            long updatedTime = post.getInt("updated_time");

            long fromId = post.getInt("source");
            String from = userNames.get(fromId);

            ArrayList<Long> toIds = new ArrayList<Long>();
            ArrayList<String> to = new ArrayList<String>();
            for (long id : StringUtils2.splitIntArray(post.getString("mentions"), ",")) {
                if (id > 0 && id < Constants.GROUP_ID_BEGIN) {
                    toIds.add(id);
                    to.add(userNames.get(id));
                }
            }

            ArrayList<Long> addToIds = new ArrayList<Long>();
            ArrayList<String> addTos = new ArrayList<String>();
            for (long id : StringUtils2.splitIntArray(post.getString("add_to"), ",")) {
                if (id > 0 && id < Constants.GROUP_ID_BEGIN) {
                    addToIds.add(id);
                    addTos.add(userNames.get(id));
                }
            }

            String message = post.getString("message");

            boolean private_ = post.getInt("privince") != 0;

            PostDoc postDoc = new PostDoc();
            postDoc.setId(postId);
            postDoc.setCreatedTime(createdTime);
            postDoc.setUpdatedTime(updatedTime);
            postDoc.setFrom(from);
            postDoc.setFromId(fromId);
            postDoc.setTo(to.toArray(new String[to.size()]));
            postDoc.setToIds(CollectionUtils2.toLongArray(toIds));
            postDoc.setAddTos(addTos.toArray(new String[addTos.size()]));
            postDoc.setAddToIds(CollectionUtils2.toLongArray(addToIds));
            postDoc.setGroups(groupNames.toArray(new String[groupNames.size()]));
            postDoc.setGroupIds(CollectionUtils2.toLongArray(groupIds));
            postDoc.setCategory("");
            postDoc.setCategoryId(0L);
            postDoc.setTags(null);
            postDoc.setMessage(message);
            postDoc.setPrivate_(private_);

            search.addPosts(Arrays.asList(postDoc));
        } catch (Exception e) {
            L.error(ctx, e, "Add post into search database error");
        }
    }

    public Record destroyPosts(Context ctx, String userId, String postIds) {
        List<String> postIds0 = StringUtils2.splitList(postIds, ",", true);
        Record rec = Record.of("result", disablePosts(ctx, userId, postIds0));
        deletePostDocsForSearch(ctx, StringUtils2.splitIntArray(postIds, ","));
        return rec;
    }

    private void deletePostDocsForSearch(Context ctx, long[] postIds) {
        SearchLogic search = GlobalLogics.getSearch();
        try {
            search.deletePosts(postIds);
        } catch (Exception e) {
            L.error(ctx, e, "Delete post into search database error");
        }
    }

    public Record findStreamTemp(Context ctx, String postId, String cols) {
        List<String> cols0 = StringUtils2.splitList(cols, ",", true);
        return findPostTemp(ctx, postId, cols0);
    }

    public RecordSet getPostsNearBy(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count) {
        return getPostsNearBy0(ctx, viewerId, cols, since, max, type, appId, page, count);
    }

    public RecordSet topOneStreamByTarget(Context ctx, int type, String target) {
        return addPostIdsStrCol(topOneStreamByTarget0(ctx, type, target));
    }

    public String createRepost(Context ctx, String userId, String mentions, boolean secretly, String postId, String message, String device, String location, String appData, boolean can_comment, boolean can_like, boolean can_reshare, String add_to, String add_contact, boolean has_contact) {
        final String METHOD = "createRepost";
        L.traceStartCall(ctx, METHOD, userId, mentions, secretly, postId, message, device, location, appData, can_comment, can_like, can_reshare, add_to, add_contact, has_contact);
        String userId0 = userId;
        String postId0 = postId;
        String message0 = message;

        Record rec = findPost(ctx, postId0, streamSchema.getNames());
        if (rec.isEmpty())
            throw new ServerException(WutongErrors.STREAM_NOT_EXISTS, "The quote formPost '%s' is not exists", postId0);

        //if (rec.getString("source").equals(userId0))
        //    throw new StreamException("Can't repost by self");

        rec.put("source", userId0);
        rec.put("quote", postId0);
        rec.put("type", Constants.TEXT_POST);

        rec.put("root", rec.getInt("root") != 0 ? rec.getInt("root") : postId0);

        rec.put("message", message0);

        String newPostId = genPostId();
        rec.put("app_data", appData);
        rec.put("attachments", "[]");
        rec.put("mentions", mentions);
        rec.put("privince", secretly);
        rec.put("post_id", newPostId);
        long now = DateUtils.nowMillis();
        rec.put("created_time", now);
        rec.put("updated_time", now);
        rec.putMissing("app", Constants.NULL_APP_ID);
        rec.put("device", device);
        rec.put("location", location);
        rec.put("can_comment", can_comment);
        rec.put("can_like", can_like);
        rec.put("can_reshare", can_reshare);
        rec.put("add_to", add_to);
        rec.put("has_contact", has_contact);
        rec.put("add_contact", add_contact);
        if (rec.getString("location").length() > 0) {
            String longitude = Constants.parseLocation(rec.getString("location"), "longitude");
            String latitude = Constants.parseLocation(rec.getString("location"), "latitude");
            rec.put("longitude", longitude);
            rec.put("latitude", latitude);
        }
        L.debug(ctx, "before save post rec=" + rec);
        boolean b = savePost(ctx, rec);
        if (!b)
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "Save formPost error");

        L.traceEndCall(ctx, METHOD);
        return newPostId;
    }

    public boolean updatePost(Context ctx, String userId, String postId, Record post) {
        final String METHOD = "updatePost";
        L.traceStartCall(ctx, METHOD, userId, postId, post);
        String userId0 = userId;
        String postId0 = postId;
        Schemas.checkRecordColumnsIn(post, "message", "can_comment", "can_like", "can_reshare");
        if (post.has("message"))
            post.put("updated_time", DateUtils.nowMillis());

        boolean b = updatePost0(ctx, userId0, postId0, post);
        if (!b)
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "Update formPost error");
        L.traceEndCall(ctx, METHOD);
        return b;
    }

    public RecordSet getPosts(Context ctx, String postIds, String cols) {
        List<String> postIds0 = StringUtils2.splitList(postIds, ",", true);
        List<String> cols0 = StringUtils2.splitList(cols, ",", true);
        if (postIds0.isEmpty() || cols0.isEmpty())
            return new RecordSet();

        RecordSet recs = addPostIdsStrCol(findPosts(ctx, postIds0, cols0));
        if (cols0.contains("post_id")){
            transTimelineForQiupuP(ctx, ctx.getViewerIdString(), recs, 2, 2, true);
        }
        return recs;
    }

    public boolean hasPost(Context ctx, String postId) {
        Record rec = findPost(ctx, postId, Arrays.asList("post_id"));
        return !rec.isEmpty();
    }


    public RecordSet getUsersPosts(Context ctx, String viewerId, String userIds, String circleIds, String cols, long since, long max, int type, String appId, int page, int count) {
        final String METHOD = "getUsersPosts";
        L.traceStartCall(ctx, METHOD, userIds, circleIds, cols, since, max, type, appId, page, count);
        if (count < 0 || count >= 1000)
            count = 1000;

        List<String> userIds0 = StringUtils2.splitList(userIds, ",", true);
        List<String> cols0 = StringUtils2.splitList(cols, ",", true);

        List<String> circleId0 = StringUtils2.splitList(circleIds, ",", true);
        if (cols0.isEmpty())
            return new RecordSet();

        Schemas.checkSchemaIncludeColumns(streamSchema, cols0.toArray(new String[cols0.size()]));
        RecordSet recordSet = addPostIdsStrCol(getUsersPosts0(ctx, viewerId, userIds0, circleId0, cols0, since, max, type, appId, page, count));
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    public RecordSet getRelatedPosts(Context ctx, String userId, String cols, long since, long max, int type, String appId, int page, int count, List<String> groupIds) {
        final String METHOD = "getRelatedPosts";
        L.traceStartCall(ctx, METHOD, userId, cols, since, max, type, appId, page, count);
        if (count < 0 || count >= 1000)
            count = 1000;

        List<String> cols0 = StringUtils2.splitList(cols, ",", true);

        if (cols0.isEmpty())
            return new RecordSet();

        Schemas.checkSchemaIncludeColumns(streamSchema, cols0.toArray(new String[cols0.size()]));
        RecordSet recordSet = addPostIdsStrCol(getRelatedPosts0(ctx, userId, cols0, since, max, type, appId, page, count, groupIds));
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    public RecordSet getMySharePosts(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) {
        final String METHOD = "getMySharePosts";
        L.traceStartCall(ctx, METHOD, userIds, cols, since, max, type, appId, page, count);
        if (count < 0 || count >= 100000)
            count = 100000;

        List<String> userIds0 = StringUtils2.splitList(userIds, ",", true);
        List<String> cols0 = StringUtils2.splitList(cols, ",", true);

        if (cols0.isEmpty())
            return new RecordSet();

        Schemas.checkSchemaIncludeColumns(streamSchema, cols0.toArray(new String[cols0.size()]));
        RecordSet recordSet = addPostIdsStrCol(getMySharePosts0(ctx, viewerId, userIds0, cols0, since, max, type, appId, page, count));
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }


    public RecordSet selectPostsBySql(Context ctx, String sql) {
        return addPostIdsStrCol(selectPosts(ctx, sql));
    }


    public RecordSet myTopOneStreamByTarget(Context ctx, String userId, int type, String target, String cols) {
        List<String> cols0 = StringUtils2.splitList(cols, ",", true);
        return addPostIdsStrCol(myTopOneStreamByTarget0(ctx, userId, type, target, cols0));
    }


    public RecordSet topOneStreamBySetFriend(Context ctx, int type, String source, long created_time) {
        return addPostIdsStrCol(topOneStreamBySetFriend0(ctx, type, source, created_time));
    }

    public String updatePostFor(Context ctx, String post_id, String Attachments, long created_time, long updated_time) {
        String new_postId = genPostId();
        boolean b = updatePostFor0(ctx, post_id, new_postId, Attachments, created_time, updated_time);
        return b ? new_postId : "";
    }

    //====================================================================================================================

    public boolean postCanCommentP(Context ctx, String postId) {
        return getPostP(ctx, postId, "can_comment").getBoolean("can_comment", false);
    }

    public Record getPostP(Context ctx, String postId, String cols) {
        return getPosts(ctx, Commons.firstId(postId), cols).getFirstRecord();
    }

    public boolean postCanLikeP(Context ctx, String postId) {
        return getPostP(ctx, postId, "can_like").getBoolean("can_like", false);
    }

    public RecordSet getNearByStreamP(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count, String location, int dis) throws ResponseError {
        final String METHOD = "getNearByStreamP";
        L.traceStartCall(ctx, METHOD, cols, since, max, type, appId, page, count, location, dis);
        cols = cols.length() > 0 ? cols : Constants.POST_FULL_COLUMNS;
        cols = !cols.contains("longitude") ? cols + ",longitude" : cols;
        cols = !cols.contains("latitude") ? cols + ",latitude" : cols;


        double longitude_me = Double.parseDouble(Constants.parseLocation(location, "longitude"));
        double latitude_me = Double.parseDouble(Constants.parseLocation(location, "latitude"));

        RecordSet cList = getPostsNearBy(ctx, viewerId, cols, since, max, type, appId, 0, 200);
        L.debug(ctx, "get nearby post in db out list=" + cList);
        for (Record r : cList) {
            double longitude = Double.parseDouble(r.getString("longitude"));
            double latitude = Double.parseDouble(r.getString("latitude"));
            double distance = Commons.GetDistance(longitude_me, latitude_me, longitude, latitude);
            r.put("distance", distance);
        }

        for (int i = cList.size() - 1; i >= 0; i--) {
            double distance = Double.parseDouble(cList.get(i).getString("distance"));
            if (distance > Double.parseDouble(String.valueOf(dis)))
                cList.remove(i);
        }

        if (cList.size() > 0) {
            cList.sliceByPage(page, count);
        }
        RecordSet recordSet = transTimelineForQiupuP(ctx, viewerId, cList, 2, 5, false);
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    ///////////////////////////

    private void attachFavorite(Context ctx, String viewerId, RecordSet reds) {
        FavoriteLogic favoriteLogic = GlobalLogics.getFavorite();

//        for (Record rec : reds) {
//            rec.put("favorited", favoriteLogic.getIFavorited(ctx, viewerId, String.valueOf(Constants.POST_OBJECT), rec.getString("post_id")));
//        }

        Map<String, Boolean> m = favoriteLogic.getIFavorited(ctx, viewerId, String.valueOf(Constants.POST_OBJECT), reds.getStringColumnValuesAsArray("post_id"));
        for (Record rec : reds) {
            rec.put("favorited", m.get(rec.getString("post_id")));
        }
    }


    private void attachTag(Context ctx, String viewerId, RecordSet reds) {
        TagLogic tagLogic = GlobalLogics.getTag();

//        for (Record rec : reds) {
//            Record tg = tagLogic.findUserTagByTarget(ctx, rec.getString("post_id"), String.valueOf(Constants.POST_OBJECT), 0, 1).getFirstRecord();
//            if (tg.isEmpty()) {
//                rec.put("taged", false);
//                rec.put("tag_content", "");
//            } else {
//                rec.put("taged", true);
//                rec.put("tag_content", tg.getString("tag"));
//            }
//        }
        Map<String, String[]> tagsMap = tagLogic.getTagContentsByTargets(ctx, reds.getStringColumnValuesAsArray("post_id"),"", String.valueOf(Constants.POST_OBJECT));
        for (Record rec : reds) {
            String[] tags = tagsMap.get(rec.getString("post_id"));
            if (tags == null) {
                rec.put("taged", false);
                rec.put("tag_content", "");
            } else {
                rec.put("taged", true);
                rec.put("tag_content", StringUtils.join(tags, ","));
            }
        }
    }

    private void separateId(String id, Collection<String> userIds, Collection<String> groupIds, Collection<String> pageIds) {
        long nId = 0;
        try {
            nId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return;
        }

        if (nId == 0)
            return;

        if (nId >= Constants.PUBLIC_CIRCLE_ID_BEGIN && nId <= Constants.GROUP_ID_END) {
            if (groupIds != null)
                groupIds.add(id);
        } else if (nId >= Constants.PAGE_ID_BEGIN && nId <= Constants.PAGE_ID_END) {
            if (pageIds != null)
                pageIds.add(id);
        } else if (id.length() < Constants.USER_ID_MAX_LEN) {
            if (userIds != null)
                userIds.add(id);
        }
    }

    private static Record groupRecordToUserRecord(Record groupRec) {
        if (groupRec == null)
            return null;

        Record userRec = new Record();
        userRec.put("user_id", groupRec.getInt(Constants.GRP_COL_ID));
        userRec.put("display_name", groupRec.getString(Constants.GRP_COL_NAME));
        userRec.put("perhaps_name", JsonNodeFactory.instance.arrayNode());

        String urlPattern = conf.getString("platform.profileImagePattern", "");
        if (!groupRec.has(Constants.COMM_COL_IMAGE_URL)) {
            groupRec.put(Constants.COMM_COL_IMAGE_URL, "default_public_circle.png");
            urlPattern = conf.getString("platform.sysIconUrlPattern", "");
        }
        Commons.addImageUrlPrefix(urlPattern, groupRec);

        userRec.put("image_url", groupRec.getString(Constants.COMM_COL_IMAGE_URL));
        userRec.put("profile_privacy", false);
        userRec.put("pedding_requests", JsonNodeFactory.instance.arrayNode());
        return userRec;
    }

    private static Record pageRecordToUserRecord(Record pageRec) {
        if (pageRec == null)
            return null;

        Record userRec = new Record();
        userRec.put("user_id", pageRec.getInt("page_id"));
        userRec.put("display_name", pageRec.getString("name"));
        userRec.put("image_url", pageRec.getString("logo_url"));

        String urlPattern = conf.getString("platform.profileImagePattern", "");
        if (!userRec.has("image_url") || StringUtils.isBlank(userRec.getString("image_url", ""))) {
            userRec.put("image_url", "default_public_circle.png");
            urlPattern = conf.getString("platform.sysIconUrlPattern", "");
        }
        Commons.addImageUrlPrefix(urlPattern, userRec);
        return userRec;
    }

    private Record attachMock(Context ctx, Record rec) {
        rec.putMissing("longitude", "");
        rec.putMissing("latitude", "");
        rec.putMissing("quote_s", String.valueOf(rec.getInt("quote", 0)));
        rec.putMissing("favorited", false);
        rec.putMissing("taged", false);
        rec.putMissing("tag_content", "");
        rec.putMissing("secretly", String.valueOf(rec.getBoolean("privince", false)));
        rec.putMissing("can_delete", "-1");
        rec.putMissing("top_in_targets", "");
        rec.putMissing("subscribe", 1);
        rec.putMissing("likes", JsonNodeFactory.instance.objectNode());
        rec.putMissing("comments", JsonNodeFactory.instance.objectNode());
        rec.putMissing("root_id", "");
        rec.putMissing("iliked", false);
        rec.putMissing("reshare_count", 0);
        rec.putMissing("report_abuse_count", 0);
        rec.putMissing("icon", "");
        rec.putMissing("add_new_users", JsonNodeFactory.instance.arrayNode());

        RecordSet recs = RecordSet.of(rec);
        attachUsers(ctx, ctx.getViewerIdString(), recs);
        return recs.getFirstRecord();
    }

    private void attachUsers(Context ctx, String viewerId, RecordSet reds) {
        AccountLogic accountLogic = GlobalLogics.getAccount();
        GroupLogic groupLogic = GlobalLogics.getGroup();
        PageLogic pageLogic = GlobalLogics.getPage();

        HashSet<String> allUserIds = new HashSet<String>();
        HashSet<String> allGroupIds = new HashSet<String>();
        HashSet<String> allPageIds = new HashSet<String>();
        for (Record rec : reds) {
            separateId(rec.getString("source"), allUserIds, allGroupIds, allPageIds);
            for (String s : StringUtils2.splitArray(rec.getString("mentions"), ",", true)) {
                separateId(StringUtils.removeStart(s, "#"), allUserIds, allGroupIds, allPageIds);
            }
        }
        Map<String, Record> userMaps = allUserIds.isEmpty()
                ? new HashMap<String, Record>()
                : accountLogic.getUsersBaseColumns(ctx, StringUtils.join(allUserIds, ","))
                .toRecordMap("user_id");

        Map<String, Record> groupMaps = allGroupIds.isEmpty()
                ? new HashMap<String, Record>()
                : groupLogic.getSimpleGroups(ctx, 0, 0, StringUtils.join(allGroupIds, ","), Constants.GROUP_LIGHT_COLS)
                .toRecordMap(Constants.GRP_COL_ID);

        Map<String, Record> pageMaps = allPageIds.isEmpty()
                ? new HashMap<String, Record>()
                : pageLogic.getPages(ctx, CollectionUtils2.toLongArray(allPageIds))
                .toRecordMap("page_id");


        LinkedHashSet<String> userIds = new LinkedHashSet<String>();
        LinkedHashSet<String> groupIds = new LinkedHashSet<String>();
        LinkedHashSet<String> pageIds = new LinkedHashSet<String>();
        LinkedHashSet<String> userIdsAndGroupIds = new LinkedHashSet<String>();
        for (Record rec : reds) {
            userIds.clear();
            groupIds.clear();
            pageIds.clear();
            userIdsAndGroupIds.clear();

            // from
            rec.put("from", MapUtils.getObject(userMaps, rec.getString("source"), new Record()));

            // mentions
            for (String s : StringUtils2.splitArray(rec.getString("mentions"), ",", true)) {
                separateId(StringUtils.removeStart(s, "#"), userIds, groupIds, pageIds);
            }
            userIdsAndGroupIds.addAll(userIds);
            userIdsAndGroupIds.addAll(groupIds);
            rec.put("mentions", StringUtils.join(userIdsAndGroupIds, ","));

            // to
            RecordSet toRecs = new RecordSet();
            for (String userId : userIds) {
                Record userRec = userMaps.get(userId);
                if (userRec != null)
                    toRecs.add(userRec.copy());
            }
            for (String groupId : groupIds) {
                Record groupRec = groupMaps.get(groupId);
                if (groupRec != null)
                    toRecs.add(groupRecordToUserRecord(groupRec));
            }
            toRecs = Commons.transUserAddressForQiupu(toRecs);
            for (String pageId : pageIds) {
                Record pageRec = pageMaps.get(pageId);
                if (pageRec != null)
                    toRecs.add(pageRecordToUserRecord(pageRec));
            }
            rec.put("to", toRecs);
        }

        //////////////////////////////////////////////////
//        for (Record rec : reds) {
//            rec.put("from", accountLogic.getUsers(ctx, rec.getString("source"), rec.getString("source"), Constants.USER_LIGHT_COLUMNS_LIGHT).getFirstRecord());//1
//        }
//
//        for (Record rec : reds) {
//            String t_mentions = rec.getString("mentions");
//
//            // col: to
//            List<String> l_t_mentions = StringUtils2.splitList(t_mentions, ",", true);
//            List<String> groupList = groupLogic.getGroupIdsFromMentions(ctx, l_t_mentions);
//            String groupIds = joinIgnoreBlank(",", groupList);
//            l_t_mentions.removeAll(groupList);
//
//            List<Long> pageIdList = PageLogicUtils.getPageIdsFromMentions(ctx, l_t_mentions);
//            PageLogicUtils.removeAllPageIds(ctx, l_t_mentions);
//
//            if (l_t_mentions.size() > 0) {
//                if (!viewerId.equals(rec.getString("source"))) {
//                    for (int jj = l_t_mentions.size() - 1; jj >= 0; jj--) {
//                        if (l_t_mentions.get(jj).toString().length() > Constants.USER_ID_MAX_LEN)
//                            l_t_mentions.remove(jj);
//                    }
//                }
//            }
//            String nowMentions = StringUtils.join(l_t_mentions, ",");
//            String users = commons.parseAllUsers(nowMentions);
//            String mentions = users;
//            if (StringUtils.isNotBlank(groupIds)) {
//                if (StringUtils.isBlank(users))
//                    mentions = groupIds;
//                else
//                    mentions = users + "," + groupIds;
//            }
//            rec.put("mentions", mentions);
//
//            RecordSet userto = accountLogic.getUsers(ctx, rec.getString("source"), users, Constants.USER_LIGHT_COLUMNS_LIGHT);
//            if (viewerId.equals(rec.getString("source"))) {
//                if (rec.getString("add_contact").length() > 0 && rec.getBoolean("has_contact", false)) {
//                    List<String> l_add_contact = StringUtils2.splitList(rec.getString("add_contact"), ",", true);
//                    for (String a : l_add_contact) {
//                        Record r = new Record();
//                        r.put("user_id", 0);
//                        r.put("display_name", a);
//                        r.put("in_circles", new RecordSet());
//                        r.put("friends_count", 0);
//                        r.put("followers_count", 0);
//                        userto.add(r);
//                    }
//                }
//            }
//
//            if (StringUtils.isNotBlank(groupIds)) {
//                RecordSet groups = groupLogic.getSimpleGroups(ctx, 0, 0, groupIds, Constants.GROUP_LIGHT_COLS);
//                for (Record group_ : groups) {
//                    Record r = new Record();
//                    r.put("user_id", group_.getInt(Constants.GRP_COL_ID));
//                    r.put("display_name", group_.getString(Constants.GRP_COL_NAME));
//                    r.put("perhaps_name", JsonNodeFactory.instance.arrayNode());
//
//                    String urlPattern = conf.getString("platform.profileImagePattern", "");
//                    if (!group_.has(Constants.COMM_COL_IMAGE_URL)) {
//                        group_.put(Constants.COMM_COL_IMAGE_URL, "default_public_circle.png");
//                        urlPattern = conf.getString("platform.sysIconUrlPattern", "");
//                    }
//                    commons.addImageUrlPrefix(urlPattern, group_);
//
//                    r.put("image_url", group_.getString(Constants.COMM_COL_IMAGE_URL));
//                    r.put("profile_privacy", false);
//                    r.put("pedding_requests", JsonNodeFactory.instance.arrayNode());
//                    userto.add(r);
//                }
//            }
//
//            RecordSet toRecs = commons.transUserAddressForQiupu(userto);
//            if (CollectionUtils.isNotEmpty(pageIdList)) {
//                Long[] arr = pageIdList.toArray(new Long[pageIdList.size()]);
//                long[] pageIds = ArrayUtils.toPrimitive(arr);
//                RecordSet pageRecs = GlobalLogics.getPage().getPages(ctx, pageIds);
//                pageRecs.renameColumn("page_id", "user_id");
//                pageRecs.renameColumn("name", "display_name");
//                pageRecs.renameColumn("logo_url", "image_url");
//
//                for (Record pageRec : pageRecs) {
//                    String urlPattern = conf.getString("platform.profileImagePattern", "");
//                    if (!pageRec.has("image_url") || StringUtils.isBlank(pageRec.getString("image_url", ""))) {
//                        pageRec.put("image_url", "default_public_circle.png");
//                        urlPattern = conf.getString("platform.sysIconUrlPattern", "");
//                    }
//                    commons.addImageUrlPrefix(urlPattern, pageRec);
//                }
//
//                toRecs.addAll(pageRecs);
//            }
//            rec.put("to", toRecs);//2
//        }

    }

    private void attachSecretly(Context ctx, String viewerId, RecordSet reds) {
        for (Record rec : reds) {
            rec.put("secretly", rec.getString("privince"));
        }
    }

    private void attachCanDelete(Context ctx, String viewerId, RecordSet reds) {
        GroupLogic groupLogic = GlobalLogics.getGroup();

        HashSet<String> allGroupIds = new HashSet<String>();
        for (Record rec : reds) {
            for (String s : StringUtils2.splitArray(rec.getString("mentions"), ",", true)) {
                separateId(StringUtils.removeStart(s, "#"), null, allGroupIds, null);
            }
        }
        Map<Long, Integer> rolesInGroups = groupLogic.getRolesWithGroups(ctx, CollectionUtils2.toLongArray(allGroupIds), Long.parseLong(viewerId));
        for (Record rec : reds) {
            String canDelete;
            try {
                if (Long.parseLong(viewerId) == rec.getInt("source")) {
                    canDelete = "-1";
                } else {
                    LinkedHashSet<String> groupIds = new LinkedHashSet<String>();
                    for (String s : StringUtils2.splitArray(rec.getString("mentions"), ",", true)) {
                        separateId(StringUtils.removeStart(s, "#"), null, groupIds, null);
                    }
                    LinkedHashSet<String> l = new LinkedHashSet<String>();
                    for (String groupId : groupIds) {
                        if (MapUtils.getIntValue(rolesInGroups, Long.parseLong(groupId), 0) >= Constants.ROLE_ADMIN)
                            l.add(groupId);
                    }
                    canDelete = joinIgnoreBlank(",", l);
                }
            } catch (Exception ignored) {
                canDelete = "";
            }
            rec.put("can_delete", canDelete);
        }


        /*
        if (StringUtils.equals(viewerId, source))
            return "-1";

        String s = "";
        try {
            ArrayList<String> l = new ArrayList<String>();
            for (String groupId : groupIds) {
                if (GlobalLogics.getGroup().hasRight(ctx, Long.parseLong(groupId), Long.parseLong(viewerId), Constants.ROLE_ADMIN))
                    l.add(groupId);
            }
            s = joinIgnoreBlank(",", l);
        } catch (Exception e) {
            s = "";
        }
         */

//        GroupLogic groupLogic = GlobalLogics.getGroup();
//        for (Record rec : reds) {
//            String t_mentions = rec.getString("mentions");
//            List<String> l_t_mentions = StringUtils2.splitList(t_mentions, ",", true);
//            List<String> groupList = groupLogic.getGroupIdsFromMentions(ctx, l_t_mentions);
//            rec.put("can_delete", canDeletePost(ctx, viewerId, rec.getString("source"), groupList));
//        }
    }

    private void attachTopInTargets(Context ctx, String viewerId, RecordSet reds) {
        GroupLogic groupLogic = GlobalLogics.getGroup();
        Map<String, String> m = groupLogic.findGroupIdsByTopPosts(ctx, reds.getStringColumnValuesAsArray("post_id"));
        for (Record rec : reds) {
            String postId = rec.getString("post_id");
            rec.put("top_in_targets", m.containsKey(postId) ? m.get(postId) : "");
        }
//        for (Record rec : reds) {
//            rec.put("top_in_targets", groupLogic.findGroupIdsByTopPost(ctx, rec.getString("post_id")));
//        }
    }

    private void attachSubscribe(Context ctx, RecordSet recs) {
        Map<String, Integer> m = GlobalLogics.getConversation().getEnabledByTargetIds(ctx, recs.joinColumnValues("post_id", ","));
        for (Record rec : recs) {
            String postId = rec.getString("post_id");
            rec.put("subscribe", m.get(postId));
        }
    }

    private void attachApkAttachments(Context ctx, String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers) {
        CommentLogic commentLogic = GlobalLogics.getComment();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        LikeLogic likeLogic = GlobalLogics.getLike();
        for (Record rec : reds) {
            long type = rec.getInt("type");
            if (type != Constants.APK_COMMENT_POST && type != Constants.APK_LIKE_POST && type != Constants.APK_POST)
                continue;

            //以下是算针对post的
            String this_targetID = Constants.POST_OBJECT + ":" + rec.getInt("post_id");
            Record Rec_post_like = new Record();
            // TODO: agg
            int like_post_count = likeLogic.getLikeCount(ctx, this_targetID);

            Rec_post_like.put("count", like_post_count);
            if (like_post_count > 0) {
                // TODO: agg
                RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, this_targetID, 0, getLikeUsers);
                List<Long> liked_userIds = recs_liked_users.getIntColumnValues("liker");
                String liked_uids = StringUtils.join(liked_userIds, ",");
                // TODO: agg
                RecordSet liked_users = accountLogic.getUsersBaseColumns(ctx, liked_uids);
                Rec_post_like.put("users", Commons.transUserAddressForQiupu(liked_users));
                rec.put("likes", Rec_post_like);//3
            } else {
                rec.put("likes", new Record());//3
            }

            Record Rec_post_comment = new Record();
            // TODO: agg
            int comment_post_count = commentLogic.getCommentCount(ctx, viewerId, this_targetID);
            Rec_post_comment.put("count", comment_post_count);
            if (comment_post_count > 0) {
                // TODO: agg
                RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                Rec_post_comment.put("latest_comments", recs_com);
                rec.put("comments", Rec_post_comment);//4
            } else {
                rec.put("comments", new Record());//4
            }
            // TODO: agg
            rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, this_targetID));//4

            //stream from comment and like ,so get comments and likes in stream's comments and like
            /*
            String attach = rec.getString("attachments");
            if (attach.length() > 20) {
                Record a_rec = RecordSet.fromJson(attach).getFirstRecord();

                Record rec_apk_like = new Record();
                int apk_like_count = (int) a_rec.getInt("app_like_count");
                rec_apk_like.put("count", apk_like_count);

                if (apk_like_count > 0) {
                    rec_apk_like.put("users", RecordSet.fromJson(a_rec.toJsonNode().findValue("app_liked_users").toString()));
                    rec.put("likes", rec_apk_like);//3
                } else {
                    rec.put("likes", new Record());//3
                }
                a_rec.remove("app_like_count");
                a_rec.remove("app_liked_users");


                Record rec_apk_comment = new Record();
                int apk_comment_count = (int) a_rec.getInt("app_comment_count");
                apk_comment_count = commentLogic.getCommentCount(ctx, viewerId, String.valueOf(Constants.APK_OBJECT) + ":" + rec.getString("target"));
                rec_apk_comment.put("count", apk_comment_count);
                if (apk_comment_count > 0) {
                    // TODO: agg
                    RecordSet newComments = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.APK_OBJECT, rec.getString("target"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                    rec_apk_comment.put("latest_comments", newComments);
                    //                                rec_apk_comment.put("latest_comments", RecordSet.fromJson(a_rec.toJsonNode().findValue("app_comments").toString()));
                    rec.put("comments", rec_apk_comment);//4
                } else {
                    rec.put("comments", new Record());//4
                }

                // TODO: agg
                rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, Constants.APK_OBJECT + ":" + a_rec.getString("apk_id")));//4

                a_rec.remove("app_comment_count");
                a_rec.remove("app_comments");
                a_rec.remove("app_likes");

                RecordSet t = new RecordSet();
                t.add(a_rec);
                rec.put("attachments", t);
                rec.put("root_id", a_rec.getString("apk_id"));
            }
            */
        }
    }

    private void attachFileAttachments(Context ctx, String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers) {
        AccountLogic accountLogic = GlobalLogics.getAccount();
        CommentLogic commentLogic = GlobalLogics.getComment();
        LikeLogic likeLogic = GlobalLogics.getLike();
        for (Record rec : reds) {
            long type = rec.getInt("type");
            if (type != Constants.VIDEO_POST && type != Constants.AUDIO_POST && type != Constants.FILE_POST)
                continue;

            String this_targetID = Constants.POST_OBJECT + ":" + rec.getInt("post_id");
            String attach0 = rec.getString("attachments");
            if (attach0.length() > 20) {
                RecordSet tmp_attachments = RecordSet.fromJson(attach0);
                for (Record p_rec : tmp_attachments) {
                    String file_id = p_rec.getString("file_id");
                    boolean file_tmp = getFile(ctx, viewerId, String.valueOf(file_id));
                    if (!file_tmp) {
                        p_rec.put("folder_id", "");
                        p_rec.put("file_id", file_id);
                        p_rec.put("title", "");
                        p_rec.put("summary", "");
                        p_rec.put("description", "");
                        p_rec.put("file_size", 0);
                        p_rec.put("html_url", this.prefix + "wutong-data/media/photo/ERROR_O.jpg");
                        p_rec.put("user_id", 0);
                        p_rec.put("exp_name", "");
                        p_rec.put("content_type", "");
                        p_rec.put("new_file_name", "");
                        p_rec.put("file_url", "");
                        p_rec.put("thumbnail_url", "");
                        p_rec.put("created_time", 0);
                        p_rec.put("updated_time", 0);
                        p_rec.put("destroyed_time", 0);
                    } else {
                        Record Rec_file_like = new Record();
                        String objectFileId = String.valueOf(Constants.FILE_OBJECT) + ":" + String.valueOf(file_id);
                        int file_like_count = likeLogic.getLikeCount(ctx, objectFileId);
                        Rec_file_like.put("count", file_like_count);
                        if (file_like_count > 0) {
                            // TODO: agg
                            RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, objectFileId, 0, getLikeUsers);
                            List<Long> list_file_liked_users = recs_liked_users.getIntColumnValues("liker");
                            String likeuids = StringUtils.join(list_file_liked_users, ",");
                            RecordSet recs_user_liked = accountLogic.getUsersBaseColumns(ctx, likeuids);
                            Rec_file_like.put("users", Commons.transUserAddressForQiupu(recs_user_liked));
                        } else {
                            Rec_file_like.put("users", new Record());//3
                        }

                        // TODO: agg
                        Rec_file_like.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, objectFileId));
                        p_rec.put("likes", Rec_file_like);

                        Record Rec_comment = new Record();
                        int comment_count = commentLogic.getCommentCount(ctx, viewerId, objectFileId);
                        Rec_comment.put("count", comment_count);
                        if (comment_count > 0) {
                            // TODO: agg
                            RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.PHOTO_OBJECT, file_id, Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                            Rec_comment.put("latest_comments", recs_com);
                        } else {
                            Rec_comment.put("latest_comments", new Record());
                        }
                        p_rec.put("comments", Rec_comment);
                    }
                }
                rec.put("attachments", tmp_attachments);
            }
            //以下是算针对post的
            Record Rec_post_like = new Record();
            // TODO: agg
            int like_post_count = likeLogic.getLikeCount(ctx, this_targetID);

            Rec_post_like.put("count", like_post_count);
            if (like_post_count > 0) {
                // TODO: agg
                RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, this_targetID, 0, getLikeUsers);
                List<Long> liked_userIds = recs_liked_users.getIntColumnValues("liker");
                String liked_uids = StringUtils.join(liked_userIds, ",");
                // TODO: agg
                RecordSet liked_users = accountLogic.getUsersBaseColumns(ctx, liked_uids);
                Rec_post_like.put("users", Commons.transUserAddressForQiupu(liked_users));
                rec.put("likes", Rec_post_like);//3
            } else {
                rec.put("likes", new Record());//3
            }

            Record Rec_post_comment = new Record();
            // TODO: agg
            int comment_post_count = commentLogic.getCommentCount(ctx, viewerId, this_targetID);
            Rec_post_comment.put("count", comment_post_count);
            if (comment_post_count > 0) {
                // TODO: agg
                RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                Rec_post_comment.put("latest_comments", recs_com);
                rec.put("comments", Rec_post_comment);//4
            } else {
                rec.put("comments", new Record());//4
            }
            // TODO: agg
            rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, this_targetID));//4
        }
    }

    private void attachPhotoAttachments(Context ctx, String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers) {
        AccountLogic accountLogic = GlobalLogics.getAccount();
        CommentLogic commentLogic = GlobalLogics.getComment();
        LikeLogic likeLogic = GlobalLogics.getLike();
        ReportAbuseLogic reportAbuseLogic = GlobalLogics.getReportAbuse();
        for (Record rec : reds) {
            if (rec.getInt("type") != Constants.PHOTO_POST)
                continue;

            String this_targetID = Constants.POST_OBJECT + ":" + rec.getInt("post_id");
            String attach0 = rec.getString("attachments");
            if (attach0.length() > 20) {
                RecordSet tmp_attachments = RecordSet.fromJson(attach0);

                try {
                    for (Record moveR : tmp_attachments) {
                        int reportAbuseCount = reportAbuseLogic.getReportAbuseCount(ctx, Constants.PHOTO_OBJECT, moveR.getString("photo_id"), Constants.APP_TYPE_BPC);
                        if (reportAbuseCount >= Constants.REPORT_ABUSE_COUNT)
                            moveR.put("caption", "###DELETE###");
                        if (reportAbuseLogic.iHaveReport(ctx, viewerId, Constants.PHOTO_OBJECT, moveR.getString("photo_id"), Constants.APP_TYPE_BPC) >= 1)
                            moveR.put("caption", "###DELETE###");
                    }
                    for (int jj = tmp_attachments.size() - 1; jj >= 0; jj--) {
                        Record p = tmp_attachments.get(jj);
                        if (p.getString("caption").equals("###DELETE###")) {
                            tmp_attachments.remove(jj);
                        }
                    }
                    if (tmp_attachments.size() == 0) {
                        Record tmp = new Record();
                        tmp.put("photo_id", 0);
                        tmp_attachments.add(tmp);
                    }
                } catch (Exception e) {
                    L.debug(ctx, "get photo filter report abuse error in stream");
                }

                for (Record p_rec : tmp_attachments) {
                    p_rec.put("album_photo_count", 0);
                    p_rec.put("album_cover_photo_id", 0);
                    String photo_id = p_rec.getString("photo_id");

                    // TODO: agg
                    boolean photo_tmp = getPhoto(ctx, viewerId, String.valueOf(photo_id));
                    if (!photo_tmp) {
                        p_rec.put("album_id", "");
                        p_rec.put("album_name", "");
                        p_rec.put("photo_id", photo_id);
                        p_rec.put("album_photo_count", 0);
                        p_rec.put("album_cover_photo_id", 0);
                        p_rec.put("album_description", "");
                        p_rec.put("album_visible", false);
                        p_rec.put("photo_img_middle", this.prefix + "wutong-data/media/photo/ERROR_O.jpg");
                        p_rec.put("photo_img_original", this.prefix + "wutong-data/media/photo/ERROR_O.jpg");
                        p_rec.put("photo_img_big", this.prefix + "wutong-data/media/photo/ERROR_L.jpg");
                        p_rec.put("photo_img_small", this.prefix + "wutong-data/media/photo/ERROR_S.jpg");
                        p_rec.put("photo_img_thumbnail", this.prefix + "wutong-data/media/photo/ERROR_T.jpg");
                        p_rec.put("photo_caption", "");
                        p_rec.put("photo_location", "");
                        p_rec.put("photo_tag", "");
                        p_rec.put("photo_created_time", 0);
                        p_rec.put("longitude", "");
                        p_rec.put("latitude", "");
                        p_rec.put("orientation", "");
                    } else {
                        Record Rec_photo_like = new Record();
                        String objectPhotoId = String.valueOf(Constants.PHOTO_OBJECT) + ":" + String.valueOf(photo_id);
                        int photo_like_count = likeLogic.getLikeCount(ctx, objectPhotoId);
                        Rec_photo_like.put("count", photo_like_count);
                        if (photo_like_count > 0) {
                            // TODO: agg
                            RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, objectPhotoId, 0, getLikeUsers);
                            List<Long> list_photo_liked_users = recs_liked_users.getIntColumnValues("liker");
                            String likeuids = StringUtils.join(list_photo_liked_users, ",");
                            RecordSet recs_user_liked = accountLogic.getUsersBaseColumns(ctx, likeuids);
                            Rec_photo_like.put("users", Commons.transUserAddressForQiupu(recs_user_liked));
                        } else {
                            Rec_photo_like.put("users", new Record());//3
                        }

                        // TODO: agg
                        Rec_photo_like.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, objectPhotoId));
                        p_rec.put("likes", Rec_photo_like);

                        Record Rec_comment = new Record();
                        // TODO: agg
                        int comment_count = commentLogic.getCommentCount(ctx, viewerId, objectPhotoId);
                        Rec_comment.put("count", comment_count);
                        if (comment_count > 0) {
                            // TODO: agg
                            RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.PHOTO_OBJECT, photo_id, Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                            Rec_comment.put("latest_comments", recs_com);
                        } else {
                            Rec_comment.put("latest_comments", new Record());
                        }
                        p_rec.put("comments", Rec_comment);
                    }
                }
                rec.put("attachments", tmp_attachments);
            }
            //以下是算针对post的
            String attach = rec.getString("attachments");
            if (attach.length() > 20) {
                RecordSet tmp_attach = RecordSet.fromJson(attach);
                if (tmp_attach.size() > 0) {
                    for (Record r : tmp_attach) {
                        r.put("photo_img_original", r.getString("photo_img_middle"));
                    }
                }
                rec.put("attachments", tmp_attach);
            }

            Record Rec_post_like = new Record();
            int like_post_count = likeLogic.getLikeCount(ctx, this_targetID);

            Rec_post_like.put("count", like_post_count);
            if (like_post_count > 0) {
                // TODO: agg
                RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, this_targetID, 0, getLikeUsers);
                List<Long> liked_userIds = recs_liked_users.getIntColumnValues("liker");
                String liked_uids = StringUtils.join(liked_userIds, ",");
                // TODO: agg
                RecordSet liked_users = accountLogic.getUsersBaseColumns(ctx, liked_uids);
                Rec_post_like.put("users", Commons.transUserAddressForQiupu(liked_users));
                rec.put("likes", Rec_post_like);//3
            } else {
                rec.put("likes", new Record());//3
            }

            Record Rec_post_comment = new Record();
            // TODO: agg
            int comment_post_count = commentLogic.getCommentCount(ctx, viewerId, this_targetID);
            Rec_post_comment.put("count", comment_post_count);
            if (comment_post_count > 0) {
                // TODO: agg
                RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                Rec_post_comment.put("latest_comments", recs_com);
                rec.put("comments", Rec_post_comment);//4
            } else {
                rec.put("comments", new Record());//4
            }
            // TODO; agg
            rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, this_targetID));//4
        }
    }

    private void attachOtherAttachments(Context ctx, String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers) {
        final long[] specialTypes = {
                Constants.APK_COMMENT_POST, Constants.APK_LIKE_POST, Constants.APK_POST,
                Constants.VIDEO_POST, Constants.AUDIO_POST, Constants.FILE_POST,
                Constants.PHOTO_POST
        };

        AccountLogic accountLogic = GlobalLogics.getAccount();
        CommentLogic commentLogic = GlobalLogics.getComment();
        LikeLogic likeLogic = GlobalLogics.getLike();

        for (Record rec : reds) {
            if (ArrayUtils.contains(specialTypes, rec.getInt("type")))
                continue;

            String this_targetID = Constants.POST_OBJECT + ":" + rec.getInt("post_id");

            Record rec_stream = new Record();
            int stream_like_count = likeLogic.getLikeCount(ctx, this_targetID);
            rec_stream.put("count", stream_like_count);

            if (stream_like_count > 0) {
                RecordSet recs_stream_liked_users = likeLogic.loadLikedUsers(ctx, this_targetID, 0, getLikeUsers);
                List<Long> list_liked_users = recs_stream_liked_users.getIntColumnValues("liker");
                String likeuids = StringUtils.join(list_liked_users, ",");
                RecordSet recs_users_liked = accountLogic.getUsersBaseColumns(ctx, likeuids);
                rec_stream.put("users", Commons.transUserAddressForQiupu(recs_users_liked));
                rec.put("likes", rec_stream);//3
            } else {
                rec.put("likes", new Record());//3
            }

            Record tempRec = new Record();
            int comc = commentLogic.getCommentCount(ctx, viewerId, this_targetID);
            tempRec.put("count", comc);
            if (comc > 0) {
                RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                tempRec.put("latest_comments", recs_com);
                rec.put("comments", tempRec);//4
            } else {
                rec.put("comments", new Record());//4
            }
            rec.put("root_id", "");

            rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, this_targetID));//4
        }

//        RecordSet reds0 = new RecordSet();
//        LinkedHashSet<Long> postIds = new LinkedHashSet<Long>();
//        LinkedHashSet<String> postTargetIds = new LinkedHashSet<String>();
//        for (Record rec : reds) {
//            long type = rec.getInt("type");
//            if (!ArrayUtils.contains(specialTypes, type)) {
//                postIds.add(rec.getInt("post_id"));
//                postTargetIds.add(Constants.POST_OBJECT + ":" + rec.getInt("post_id"));
//                reds0.add(rec);
//            }
//        }
//
//        HashSet<String> userIds = new HashSet<String>();

//        // likes
//        String[] postTargetIdsArray = postTargetIds.toArray(new String[postTargetIds.size()]);
//        Map<String, Integer> likeCount = likeLogic.getLikeCounts(ctx, postTargetIdsArray);
//        Map<String, String> likedUserIds = likeLogic.getLikedUserIds(ctx, postTargetIdsArray, getLikeUsers);
//        for (String likedUserIds0 : likedUserIds.values()) {
//            userIds.addAll(StringUtils2.splitList(likedUserIds0, ",", true));
//        }
//        Map<String, Record> users = accountLogic.getUsers(ctx, viewerId, StringUtils.join(userIds, ","), Constants.USER_LIGHT_COLUMNS_LIGHT).toRecordMap("user_id");
//        for (Record rec : reds0) {
//            String postTargetId = Constants.POST_OBJECT + ":" + rec.getInt("post_id");
//            Record likesRec = new Record();
//            likesRec.put("count", MapUtils.getIntValue(likeCount, postTargetId, 0));
//            RecordSet likerRecs = new RecordSet();
//            for (String likerUserId : StringUtils2.splitArray(MapUtils.getString(likedUserIds, postTargetId, ""), ",", true)) {
//                likerRecs.addIf(users.containsKey(likerUserId), users.get(likerUserId));
//            }
//            likesRec.put("users", likerRecs);
//            rec.put("likes", likesRec);
//        }
//
//
//        // iliked
//        if (viewerId == null || viewerId.isEmpty() || viewerId.equals("0")) {
//            for (Record rec : reds) {
//                rec.put("iliked", false);
//            }
//        } else {
//            Map<String, Boolean> liked = likeLogic.ifUserLiked(ctx, viewerId, postTargetIdsArray);
//            for (Record rec : reds) {
//                String postTargetId = Constants.POST_OBJECT + ":" + rec.getInt("post_id");
//                rec.put("iliked", MapUtils.getBooleanValue(liked, postTargetId, false));
//            }
//        }
//
//
//        // comment
//        Map<String, Integer> commentCounts = commentLogic.getCommentCounts(ctx, viewerId, postTargetIdsArray);
//        for (Record rec : reds0) {
//            String postTargetId = Constants.POST_OBJECT + ":" + rec.getInt("post_id");
//            Record commentsRec = new Record();
//            int commentCount = MapUtils.getIntValue(commentCounts, postIds, 0);
//            commentsRec.put("count", commentCount);
//            RecordSet latestComments = commentCount > 0
//                    ? commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount)
//                    : new RecordSet();
//            commentsRec.put("latest_comments", latestComments);
//            rec.put("comments", commentsRec);
//        }
//
//        // root_id
//        for (Record rec : reds0) {
//            rec.put("root_id", "");
//        }
    }

    private void attachOtherFields(Context ctx, String viewerId, RecordSet reds) {
        AccountLogic accountLogic = GlobalLogics.getAccount();
        ReportAbuseLogic reportAbuseLogic = GlobalLogics.getReportAbuse();
        for (Record rec : reds) {
            RecordSet reshare_rec = getSharedPost(ctx, viewerId, rec.getString("post_id"));
            rec.put("reshare_count", reshare_rec.size());

            if (rec.getInt("root") > 0) {
                RecordSet retweeted = transTimelineForQiupuP(ctx, viewerId, getPosts(ctx, String.valueOf(rec.getInt("root")), Constants.POST_FULL_COLUMNS), 5, 5, false);
                rec.put("retweeted_stream", retweeted.isEmpty() ? "" : retweeted.getFirstRecord());//6
            }

            int reportAbuseCount = reportAbuseLogic.getReportAbuseCount(ctx, Constants.POST_OBJECT,rec.getString("post_id"),Constants.APP_TYPE_BPC);
            rec.put("report_abuse_count", reportAbuseCount);

            String iconUrlPattern = conf.checkGetString("platform.sysIconUrlPattern");
            if (rec.getInt("type") == 1) {
                rec.put("icon", "");
            }
            if (rec.getInt("type") == 32) {
                rec.put("icon", String.format(iconUrlPattern, "apk.gif"));
            }
            if (rec.getInt("type") == 256) {
                rec.put("icon", String.format(iconUrlPattern, "comment.gif"));
            }
            if (rec.getInt("type") == 512) {
                rec.put("icon", String.format(iconUrlPattern, "like.gif"));
            }
            if (rec.getInt("type") == 64) {
                rec.put("icon", String.format(iconUrlPattern, "link.gif"));
            }
            if (rec.getInt("type") == 4096) {
                rec.put("icon", String.format(iconUrlPattern, "friend.gif"));
            }

            String add_to_user = rec.getString("add_to");
            if (add_to_user.length() > 0) {
                RecordSet recs = accountLogic.getUsersBaseColumns(ctx, add_to_user);
                rec.put("add_new_users", recs.toString());
            } else {
                rec.put("add_new_users", new RecordSet());
            }
        }
    }

    private void removeAbused(Context ctx, String viewerId, RecordSet reds, Set<Record> removing) {
        ReportAbuseLogic reportAbuseLogic = GlobalLogics.getReportAbuse();
        for (Record rec : reds) {
            if ((int) rec.getInt("report_abuse_count") >= Constants.REPORT_ABUSE_COUNT)
                removing.add(rec);
            if (reportAbuseLogic.iHaveReport(ctx, viewerId, Constants.POST_OBJECT,rec.getString("post_id"),Constants.APP_TYPE_BPC) >= 1)
                removing.add(rec);
        }
    }

    private void removeUselessCommentAndLikePostWithApk(Context ctx, String viewerId, RecordSet reds, Set<Record> removing) {
        for (Record rec : reds) {
            long type = rec.getInt("type");
            String target0 = rec.getString("target");
            long updated_time0 = rec.getInt("updated_time");
            if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                for (Record rec1 : reds) {
                    if (target0.equals(rec1.getString("target")) && updated_time0 < rec1.getInt("updated_time")) {
                        removing.add(rec);
                        break;
                    }
                }
            }
        }
    }

    private void removeSourceIfNotHuman(Context ctx, String viewerId, RecordSet reds, Set<Record> removing) {
        for (Record rec : reds) {//
            if (rec.getString("from").length() < Constants.USER_ID_MAX_LEN)
                removing.add(rec);
        }
    }


    public RecordSet newTransTimelineForQiupuP(Context ctx, String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers, boolean single_get) {
        final String METHOD = "transTimelineForQiupuP";
        L.traceStartCall(ctx, METHOD, reds, getCommentCount, getLikeUsers, single_get);
        if (CollectionUtils.isEmpty(reds))
            return reds;

        attachFavorite(ctx, viewerId, reds);
        attachTag(ctx, viewerId, reds);
        attachUsers(ctx, viewerId, reds);
        attachSecretly(ctx, viewerId, reds);
        attachCanDelete(ctx, viewerId, reds);
        attachTopInTargets(ctx, viewerId, reds);
        attachSubscribe(ctx, reds);
        attachApkAttachments(ctx, viewerId, reds, getCommentCount, getLikeUsers);
        attachFileAttachments(ctx, viewerId, reds, getCommentCount, getLikeUsers);
        attachPhotoAttachments(ctx, viewerId, reds, getCommentCount, getLikeUsers);
        attachOtherAttachments(ctx, viewerId, reds, getCommentCount, getLikeUsers);
        attachOtherFields(ctx, viewerId, reds);

        HashSet<Record> removing = new HashSet<Record>();
        if (!single_get) {
            removeAbused(ctx, viewerId, reds, removing);
        }
        removeUselessCommentAndLikePostWithApk(ctx, viewerId, reds, removing);
        removeSourceIfNotHuman(ctx, viewerId, reds, removing);
        reds.removeAll(removing);

        L.debug(ctx, "after remove report get recs=" + reds);
        L.traceEndCall(ctx, METHOD);
        return reds;
    }

    ///////////////////////////

    public RecordSet transTimelineForQiupuP(Context ctx, String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers, boolean single_get) {
        Configuration conf = GlobalConfig.get();
        ElapsedCounter ec = ctx.getElapsedCounter();

        if (conf.getBoolean("stream.optTransTimelineForQiupu", false) || ctx.inUserScope(UserScopeNames.DEV_TEAMS)) {
            ec.record("***********newTransTimelineForQiupuP************");
            return newTransTimelineForQiupuP(ctx, viewerId, reds, getCommentCount, getLikeUsers, single_get);
        } else {

            ec.record("***********oldTransTimelineForQiupuP************");
            return oldTransTimelineForQiupuP(ctx, viewerId, reds, getCommentCount, getLikeUsers, single_get);
        }
    }

        ///////////////////////////

    public String canDeletePost(Context ctx, String viewerId, String source, List<String> groupIds) {
        final String METHOD = "canDeletePost";
        L.traceStartCall(ctx, METHOD, source, groupIds.toString());
        if (StringUtils.equals(viewerId, source))
            return "-1";

        String s = "";
        try {
            ArrayList<String> l = new ArrayList<String>();
            for (String groupId : groupIds) {
                if (GlobalLogics.getGroup().hasRight(ctx, Long.parseLong(groupId), Long.parseLong(viewerId), Constants.ROLE_ADMIN))
                    l.add(groupId);
            }
            s = joinIgnoreBlank(",", l);
        } catch (Exception e) {
            s = "";
        }
        L.traceEndCall(ctx, METHOD);
        return s;
    }

    public RecordSet oldTransTimelineForQiupuP(Context ctx, String viewerId, RecordSet reds, int getCommentCount, int getLikeUsers, boolean single_get) {
            final String METHOD = "transTimelineForQiupuP";
        ElapsedCounter ec = ctx.getElapsedCounter();
            ec.record("START oldTransTimelineForQiupuP");
            L.traceStartCall(ctx, METHOD, reds, getCommentCount, getLikeUsers, single_get);
            LikeLogic likeLogic = GlobalLogics.getLike();
            FavoriteLogic favoriteLogic = GlobalLogics.getFavorite();
            TagLogic tagLogic = GlobalLogics.getTag();
            CommentLogic commentLogic = GlobalLogics.getComment();
            AccountLogic accountLogic = GlobalLogics.getAccount();
            GroupLogic groupLogic = GlobalLogics.getGroup();
            ReportAbuseLogic reportAbuseLogic = GlobalLogics.getReportAbuse();

        String users_or_circle = (String) ctx.getSession("users_or_circle");
            if (reds.size() > 0) {
                //get top stream by group id
                String streamIds = groupLogic.findStreamIdsByGroupId(ctx, users_or_circle);
                List<String> streamIdList = StringUtils2.splitList(streamIds,",",true);
                ec.record("-------circleId--------"+users_or_circle);
                ec.record("-------streamIdList--------"+streamIdList);
                //ec.record("START DEAL WITH KIND OF BUSSINESS");
                for (int i = 0; i < reds.size(); i++) {
                    Record rec = reds.get(i);
                    String this_targetID = Constants.POST_OBJECT + ":" + rec.getInt("post_id");
                    ec.record("favorite-start--->");
                    rec.put("favorited",favoriteLogic.getIFavorited(ctx,viewerId,String.valueOf(Constants.POST_OBJECT), rec.getString("post_id")));
                    ec.record("favorite-end--->");
                    ec.record("tag-start--->");
                    Record tg = tagLogic.findUserTagByTarget(ctx,"", rec.getString("post_id"), String.valueOf(Constants.POST_OBJECT), 0, 1).getFirstRecord();
                    if (tg.isEmpty()) {
                        rec.put("taged", false);
                        rec.put("tag_content", "");
                    } else {
                        rec.put("taged", true);
                        rec.put("tag_content", tg.getString("tag"));
                    }
                    ec.record("tag-end--->");
                    //    private JsonNode from;1
                    //    private JsonNode to;2
                    //    private JsonNode likes;3
                    //    private JsonNode comments;4
                    //    private JsonNode custom;5
                    //    private JsonNode retweeted_stream;6

                    //modify by wangpeng at 2013-04-27 change getUsers to getUsersBaseColumn
                    //rec.put("from", accountLogic.getUsers(ctx, rec.getString("source"), rec.getString("source"), Constants.USER_LIGHT_COLUMNS_LIGHT).getFirstRecord());//1
                    ec.record("-------getUser start------");
                    rec.put("from", accountLogic.getUsersBaseColumns(ctx, rec.getString("source")).getFirstRecord());//1
                    ec.record("-------getUser end------");
                    String t_mentions = rec.getString("mentions");
                    List<String> l_t_mentions = StringUtils2.splitList(t_mentions, ",", true);

                    //ec.record("-------getGroupIdsFromMentions start------");
                    List<String> groupList = groupLogic.getGroupIdsFromMentions(ctx, l_t_mentions);
                    //ec.record("-------getGroupIdsFromMentions start------");
                    String groupIds = joinIgnoreBlank(",", groupList);
                    l_t_mentions.removeAll(groupList);

                    List<Long> pageIdList = PageLogicUtils.getPageIdsFromMentions(ctx, l_t_mentions);
                    PageLogicUtils.removeAllPageIds(ctx, l_t_mentions);

                    if (l_t_mentions.size() > 0) {
                        if (!viewerId.equals(rec.getString("source"))) {
                            for (int jj = l_t_mentions.size() - 1; jj >= 0; jj--) {
                                if (l_t_mentions.get(jj).toString().length() > Constants.USER_ID_MAX_LEN)
                                    l_t_mentions.remove(jj);
                            }
                        }
                    }
                    String nowMentions = StringUtils.join(l_t_mentions, ",");
                    String users = Commons.parseAllUsers(nowMentions);
                    String mentions = users;
                    if (StringUtils.isNotBlank(groupIds)) {
                        if (StringUtils.isBlank(users))
                            mentions = groupIds;
                        else
                            mentions = users + "," + groupIds;
                    }

                    rec.put("mentions", mentions);

                    //modify by wangpeng at 2013-04-27 change getUsers to getUsersBaseColumn
                    //RecordSet userto = accountLogic.getUsers(ctx, rec.getString("source"), users, Constants.USER_LIGHT_COLUMNS_LIGHT);
                    //ec.record("-----------getUsersBaseColumns start---------");
                    RecordSet userto = accountLogic.getUsersBaseColumns(ctx,  users);
                    //ec.record("-----------getUsersBaseColumns start---------");

                    if (viewerId.equals(rec.getString("source"))) {
                        if (rec.getString("add_contact").length() > 0 && rec.getBoolean("has_contact", false)) {
                            List<String> l_add_contact = StringUtils2.splitList(rec.getString("add_contact"), ",", true);
                            for (String a : l_add_contact) {
                                Record r = new Record();
                                r.put("user_id", 0);
                                r.put("display_name", a);
                                r.put("in_circles", new RecordSet());
                                r.put("friends_count", 0);
                                r.put("followers_count", 0);
                                userto.add(r);
                            }
                        }
                    }
                    /*else{
                        for (int kk = userto.size()-1;kk>=0;kk--){
                             if (userto.get(kk).getString("user_id").length()>Constants.USER_ID_MAX_LEN)
                                 userto.remove(kk);
                        }
                    }
                    */
                    if (StringUtils.isNotBlank(groupIds)) {
                        //ec.record("-----------getSimpleGroups start---------");
                        RecordSet groups = groupLogic.getSimpleGroups(ctx, 0, 0, groupIds, Constants.GROUP_LIGHT_COLS);

                        for (Record group_ : groups) {
                            Record r = new Record();
                            r.put("user_id", group_.getInt(Constants.GRP_COL_ID));
                            r.put("display_name", group_.getString(Constants.GRP_COL_NAME));
                            r.put("perhaps_name", JsonNodeFactory.instance.arrayNode());

                            String urlPattern = conf.getString("platform.profileImagePattern", "");
                            if (!group_.has(Constants.COMM_COL_IMAGE_URL)) {
                                group_.put(Constants.COMM_COL_IMAGE_URL, "default_public_circle.png");
                                urlPattern = conf.getString("platform.sysIconUrlPattern", "");
                            }
                            Commons.addImageUrlPrefix(urlPattern, group_);

                            r.put("image_url", group_.getString(Constants.COMM_COL_IMAGE_URL));
                            r.put("profile_privacy", false);
                            r.put("pedding_requests", JsonNodeFactory.instance.arrayNode());
                            userto.add(r);
                        }
                    }
                    //ec.record("-----------getSimpleGroups end---------");

                    ec.record("-----------getPage start---------");
                    RecordSet toRecs = Commons.transUserAddressForQiupu(userto);
                    ec.record("-----------getPage start-1--------");
                    if (CollectionUtils.isNotEmpty(pageIdList)) {
                        Long[] arr = pageIdList.toArray(new Long[pageIdList.size()]);
                        long[] pageIds = ArrayUtils.toPrimitive(arr);
                        RecordSet pageRecs = GlobalLogics.getPage().getPages(ctx, pageIds);
                        pageRecs.renameColumn("page_id", "user_id");
                        pageRecs.renameColumn("name", "display_name");
                        pageRecs.renameColumn("logo_url", "image_url");
                        ec.record("-----------getPage start-2--------");
                        for (Record pageRec : pageRecs) {
                            String urlPattern = conf.getString("platform.profileImagePattern", "");
                            if (!pageRec.has("image_url") || StringUtils.isBlank(pageRec.getString("image_url", ""))) {
                                pageRec.put("image_url", "default_public_circle.png");
                                urlPattern = conf.getString("platform.sysIconUrlPattern", "");
                            }
                            Commons.addImageUrlPrefix(urlPattern, pageRec);
                        }
                        ec.record("-----------getPage start--3-------");
                        toRecs.addAll(pageRecs);
                    }

                    rec.put("to", toRecs);//2
                    rec.put("secretly", rec.getString("privince"));
                    ec.record("-----------getPage start----4-----");
                    rec.put("can_delete", canDeletePost(ctx, viewerId, rec.getString("source"), groupList));
                    ec.record("-----------getPage start-----5----");
                    if(streamIdList.contains(rec.getString("post_id")))
                        rec.put("top_in_targets", users_or_circle);
                    else
                        rec.put("top_in_targets", "");
                    ec.record("-----------getPage end---------");
                   // if (rec.getInt("type") == Constants.APK_COMMENT_POST || rec.getInt("type") == Constants.APK_LIKE_POST|| rec.getInt("type") == Constants.APK_POST) {
                        //stream from comment and like ,so get comments and likes in stream's comments and like
                        /*
                        String attach = rec.getString("attachments");
                        if (attach.length() > 20) {
                            Record a_rec = RecordSet.fromJson(attach).getFirstRecord();

                            Record rec_apk_like = new Record();
                            int apk_like_count = (int) a_rec.getInt("app_like_count");
                            rec_apk_like.put("count", apk_like_count);

                            if (apk_like_count > 0) {
                                rec_apk_like.put("users", RecordSet.fromJson(a_rec.toJsonNode().findValue("app_liked_users").toString()));
                                rec.put("likes", rec_apk_like);//3
                            } else {
                                rec.put("likes", new Record());//3
                            }
                            a_rec.remove("app_like_count");
                            a_rec.remove("app_liked_users");


                            Record rec_apk_comment = new Record();
                            int apk_comment_count = (int) a_rec.getInt("app_comment_count");
                            apk_comment_count = commentLogic.getCommentCount(ctx, viewerId, String.valueOf(Constants.APK_OBJECT) + ":" + rec.getString("target"));
                            rec_apk_comment.put("count", apk_comment_count);
                            if (apk_comment_count > 0) {
                                RecordSet newComments = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.APK_OBJECT, rec.getString("target"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                                rec_apk_comment.put("latest_comments", newComments);
    //                                rec_apk_comment.put("latest_comments", RecordSet.fromJson(a_rec.toJsonNode().findValue("app_comments").toString()));
                                rec.put("comments", rec_apk_comment);//4
                            } else {
                                rec.put("comments", new Record());//4
                            }

                            rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, Constants.APK_OBJECT + ":" + a_rec.getString("apk_id")));//4

                            a_rec.remove("app_comment_count");
                            a_rec.remove("app_comments");
                            a_rec.remove("app_likes");

                            RecordSet t = new RecordSet();
                            t.add(a_rec);
                            rec.put("attachments", t);
                            rec.put("root_id", a_rec.getString("apk_id"));
                        }
                        */
                  //  } else
                    ec.record("-----------type charge1---------");
                if (rec.getInt("type") == Constants.VIDEO_POST || rec.getInt("type") == Constants.AUDIO_POST || rec.getInt("type") == Constants.FILE_POST) {
                        String attach0 = rec.getString("attachments");
                        if (attach0.length() > 20) {
                            RecordSet tmp_attachments = RecordSet.fromJson(attach0);
                            for (Record p_rec : tmp_attachments) {
                                String file_id = p_rec.getString("file_id");
                                boolean file_tmp = getFile(ctx, viewerId, String.valueOf(file_id));
                                if (!file_tmp) {
                                    p_rec.put("folder_id", "");
                                    p_rec.put("file_id", file_id);
                                    p_rec.put("title", "");
                                    p_rec.put("summary", "");
                                    p_rec.put("description", "");
                                    p_rec.put("file_size", 0);
                                    p_rec.put("html_url", this.prefix + "wutong-data/media/photo/ERROR_O.jpg");
                                    p_rec.put("user_id", 0);
                                    p_rec.put("exp_name", "");
                                    p_rec.put("content_type", "");
                                    p_rec.put("new_file_name", "");
                                    p_rec.put("file_url", "");
                                    p_rec.put("thumbnail_url", "");
                                    p_rec.put("created_time", 0);
                                    p_rec.put("updated_time", 0);
                                    p_rec.put("destroyed_time", 0);
                                } else {
                                    Record Rec_file_like = new Record();
                                    String objectFileId = String.valueOf(Constants.FILE_OBJECT) + ":" + String.valueOf(file_id);
                                    int file_like_count = likeLogic.getLikeCount(ctx, objectFileId);
                                    Rec_file_like.put("count", file_like_count);
                                    if (file_like_count > 0) {
                                        RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, objectFileId, 0, getLikeUsers);
                                        List<Long> list_file_liked_users = recs_liked_users.getIntColumnValues("liker");
                                        String likeuids = StringUtils.join(list_file_liked_users, ",");

                                        //modify by wangpeng at 2013-04-27 change getUsers to getUsersBaseColumn
                                        //RecordSet recs_user_liked = accountLogic.getUsers(ctx, rec.getString("source"), likeuids, Constants.USER_LIGHT_COLUMNS_LIGHT);
                                        RecordSet recs_user_liked = accountLogic.getUsersBaseColumns(ctx,  likeuids);
                                        Rec_file_like.put("users", Commons.transUserAddressForQiupu(recs_user_liked));
                                    } else {
                                        Rec_file_like.put("users", new Record());//3
                                    }

                                    Rec_file_like.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, objectFileId));
                                    p_rec.put("likes", Rec_file_like);

                                    Record Rec_comment = new Record();
                                    int comment_count = commentLogic.getCommentCount(ctx, viewerId, objectFileId);
                                    Rec_comment.put("count", comment_count);
                                    if (comment_count > 0) {
                                        RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.PHOTO_OBJECT, file_id, Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                                        Rec_comment.put("latest_comments", recs_com);
                                    } else {
                                        Rec_comment.put("latest_comments", new Record());
                                    }
                                    p_rec.put("comments", Rec_comment);

                                    if (rec.getInt("type") == Constants.VIDEO_POST) {
                                        L.debug(ctx,"===get thumbnail_url"+ p_rec.getString("new_file_name"));
                                        if (rec.getString("thumbnail_url").equals("")) {
                                            String f0[] = StringUtils.split(p_rec.getString("new_file_name"), "_");
                                            String v_id = f0.length > 0 ? f0[0].toString() : viewerId;
                                            Record v = GlobalLogics.getFile().getVideoById(ctx, file_id, v_id);
                                            L.debug(ctx,"===get thumbnail_url v "+v);
                                            if (!v.isEmpty()) {
                                                if (!v.getString("thumbnail_url").equals("")) {
                                                    rec.put("thumbnail_url", v.getString("thumbnail_url"));
                                                }
                                            }
                                            L.debug(ctx,"===get thumbnail_url v "+rec.getString("thumbnail_url"));
                                        }
                                    }
                                }
                            }
                            rec.put("attachments", tmp_attachments);
                        }
                        //以下是算针对post的
                        Record Rec_post_like = new Record();
                        int like_post_count = likeLogic.getLikeCount(ctx, this_targetID);

                        Rec_post_like.put("count", like_post_count);
                        if (like_post_count > 0) {
                            RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, this_targetID, 0, getLikeUsers);
                            List<Long> liked_userIds = recs_liked_users.getIntColumnValues("liker");
                            String liked_uids = StringUtils.join(liked_userIds, ",");

                            //modify by wangpeng at 2013-04-27 change getUsers to getUsersBaseColumn
                            //RecordSet liked_users = accountLogic.getUsers(ctx, rec.getString("source"), liked_uids, Constants.USER_LIGHT_COLUMNS_LIGHT);
                            RecordSet liked_users = accountLogic.getUsersBaseColumns(ctx, liked_uids);
                            Rec_post_like.put("users", Commons.transUserAddressForQiupu(liked_users));
                            rec.put("likes", Rec_post_like);//3
                        } else {
                            rec.put("likes", new Record());//3
                        }

                        Record Rec_post_comment = new Record();
                        int comment_post_count = commentLogic.getCommentCount(ctx, viewerId, this_targetID);
                        Rec_post_comment.put("count", comment_post_count);
                        if (comment_post_count > 0) {
                            RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                            Rec_post_comment.put("latest_comments", recs_com);
                            rec.put("comments", Rec_post_comment);//4
                        } else {
                            rec.put("comments", new Record());//4
                        }
                        rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, this_targetID));//4
                    } else if (rec.getInt("type") == Constants.PHOTO_POST) {
                        String attach0 = rec.getString("attachments");
                        if (attach0.length() > 20) {
                            RecordSet tmp_attachments = RecordSet.fromJson(attach0);
                            for (Record p_rec : tmp_attachments) {
                                p_rec.put("album_photo_count", 0);
                                p_rec.put("album_cover_photo_id", 0);
                                String photo_id = p_rec.getString("photo_id");

                                boolean photo_tmp = getPhoto(ctx, viewerId, String.valueOf(photo_id));
                                if (!photo_tmp) {
                                    p_rec.put("album_id", "");
                                    p_rec.put("album_name", "");
                                    p_rec.put("photo_id", photo_id);
                                    p_rec.put("album_photo_count", 0);
                                    p_rec.put("album_cover_photo_id", 0);
                                    p_rec.put("album_description", "");
                                    p_rec.put("album_visible", false);
                                    p_rec.put("photo_img_middle", this.prefix + "wutong-data/media/photo/ERROR_O.jpg");
                                    p_rec.put("photo_img_original", this.prefix + "wutong-data/media/photo/ERROR_O.jpg");
                                    p_rec.put("photo_img_big", this.prefix + "wutong-data/media/photo/ERROR_L.jpg");
                                    p_rec.put("photo_img_small", this.prefix + "wutong-data/media/photo/ERROR_S.jpg");
                                    p_rec.put("photo_img_thumbnail", this.prefix + "wutong-data/media/photo/ERROR_T.jpg");
                                    p_rec.put("photo_caption", "");
                                    p_rec.put("photo_location", "");
                                    p_rec.put("photo_tag", "");
                                    p_rec.put("photo_created_time", 0);
                                    p_rec.put("longitude", "");
                                    p_rec.put("latitude", "");
                                    p_rec.put("orientation", "");
                                } else {
                                    Record Rec_photo_like = new Record();
                                    String objectPhotoId = String.valueOf(Constants.PHOTO_OBJECT) + ":" + String.valueOf(photo_id);
                                    int photo_like_count = likeLogic.getLikeCount(ctx, objectPhotoId);
                                    Rec_photo_like.put("count", photo_like_count);
                                    if (photo_like_count > 0) {
                                        RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, objectPhotoId, 0, getLikeUsers);
                                        List<Long> list_photo_liked_users = recs_liked_users.getIntColumnValues("liker");
                                        String likeuids = StringUtils.join(list_photo_liked_users, ",");

                                        //modify by wangpeng at 2013-04-27 change getUsers to getUsersBaseColumn
                                        //RecordSet recs_user_liked = accountLogic.getUsers(ctx, rec.getString("source"), likeuids, Constants.USER_LIGHT_COLUMNS_LIGHT);
                                        RecordSet recs_user_liked = accountLogic.getUsersBaseColumns(ctx, likeuids);
                                        Rec_photo_like.put("users", Commons.transUserAddressForQiupu(recs_user_liked));
                                    } else {
                                        Rec_photo_like.put("users", new Record());//3
                                    }

                                    Rec_photo_like.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, objectPhotoId));
                                    p_rec.put("likes", Rec_photo_like);

                                    Record Rec_comment = new Record();
                                    int comment_count = commentLogic.getCommentCount(ctx, viewerId, objectPhotoId);
                                    Rec_comment.put("count", comment_count);
                                    if (comment_count > 0) {
                                        RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.PHOTO_OBJECT, photo_id, Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                                        Rec_comment.put("latest_comments", recs_com);
                                    } else {
                                        Rec_comment.put("latest_comments", new Record());
                                    }
                                    p_rec.put("comments", Rec_comment);
                                }
                            }
                            rec.put("attachments", tmp_attachments);
                        }
                        //以下是算针对post的
                        String attach = rec.getString("attachments");
                        if (attach.length() > 20) {
                            RecordSet tmp_attach = RecordSet.fromJson(attach);
                            if (tmp_attach.size() > 0) {
                                for (Record r : tmp_attach) {
                                    r.put("photo_img_original", r.getString("photo_img_middle"));
                                }
                            }
                            rec.put("attachments", tmp_attach);
                        }

                        Record Rec_post_like = new Record();
                        int like_post_count = likeLogic.getLikeCount(ctx, this_targetID);

                        Rec_post_like.put("count", like_post_count);
                        if (like_post_count > 0) {
                            RecordSet recs_liked_users = likeLogic.loadLikedUsers(ctx, this_targetID, 0, getLikeUsers);
                            List<Long> liked_userIds = recs_liked_users.getIntColumnValues("liker");
                            String liked_uids = StringUtils.join(liked_userIds, ",");

                            //modify by wangpeng at 2013-04-27 change getUsers to getUsersBaseColumn
                            //RecordSet liked_users = accountLogic.getUsers(ctx, rec.getString("source"), liked_uids, Constants.USER_LIGHT_COLUMNS_LIGHT);
                            RecordSet liked_users = accountLogic.getUsersBaseColumns(ctx, liked_uids);
                            Rec_post_like.put("users", Commons.transUserAddressForQiupu(liked_users));
                            rec.put("likes", Rec_post_like);//3
                        } else {
                            rec.put("likes", new Record());//3
                        }

                        Record Rec_post_comment = new Record();
                        int comment_post_count = commentLogic.getCommentCount(ctx, viewerId, this_targetID);
                        Rec_post_comment.put("count", comment_post_count);
                        if (comment_post_count > 0) {
                            RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                            Rec_post_comment.put("latest_comments", recs_com);
                            rec.put("comments", Rec_post_comment);//4
                        } else {
                            rec.put("comments", new Record());//4
                        }
                        rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, this_targetID));//4
                    } else {
                        Record rec_stream = new Record();
                        int stream_like_count = likeLogic.getLikeCount(ctx, this_targetID);
                        rec_stream.put("count", stream_like_count);

                        if (stream_like_count > 0) {
                            RecordSet recs_stream_liked_users = likeLogic.loadLikedUsers(ctx, this_targetID, 0, getLikeUsers);
                            List<Long> list_liked_users = recs_stream_liked_users.getIntColumnValues("liker");
                            String likeuids = StringUtils.join(list_liked_users, ",");

                            //modify by wangpeng at 2013-04-27 change getUsers to getUsersBaseColumn
                            //RecordSet recs_users_liked = accountLogic.getUsers(ctx, rec.getString("source"), likeuids, Constants.USER_LIGHT_COLUMNS_LIGHT);
                            RecordSet recs_users_liked = accountLogic.getUsersBaseColumns(ctx, likeuids);
                            rec_stream.put("users", Commons.transUserAddressForQiupu(recs_users_liked));
                            rec.put("likes", rec_stream);//3
                        } else {
                            rec.put("likes", new Record());//3
                        }

                        Record tempRec = new Record();
                        int comc = commentLogic.getCommentCount(ctx, viewerId, this_targetID);
                        tempRec.put("count", comc);
                        if (comc > 0) {
                            RecordSet recs_com = commentLogic.getCommentsForContainsIgnoreP(ctx, viewerId, Constants.POST_OBJECT, rec.getInt("post_id"), Constants.FULL_COMMENT_COLUMNS, false, 0, getCommentCount);
                            tempRec.put("latest_comments", recs_com);
                            rec.put("comments", tempRec);//4
                        } else {
                            rec.put("comments", new Record());//4
                        }
                        rec.put("root_id", "");

                        rec.put("iliked", viewerId.equals("") ? false : likeLogic.ifUserLiked(ctx, viewerId, this_targetID));//4

                    }
    //                    rec.put("custom", userto);//5
                    ec.record("-----------type charge1 end---------");
                    RecordSet reshare_rec = getSharedPost(ctx, viewerId, rec.getString("post_id"));
                    rec.put("reshare_count", reshare_rec.size());

                    ec.record("-----------type reportAbuse---------");
                    if (rec.getInt("root") > 0) {
                        RecordSet retweeted = transTimelineForQiupuP(ctx, viewerId, getPosts(ctx, String.valueOf(rec.getInt("root")), Constants.POST_FULL_COLUMNS), 5, 5, false);
                        rec.put("retweeted_stream", retweeted.isEmpty() ? "" : retweeted.getFirstRecord());//6
                    }

                    int reportAbuseCount = reportAbuseLogic.getReportAbuseCount(ctx, Constants.POST_OBJECT,rec.getString("post_id"),Constants.APP_TYPE_BPC);
                    rec.put("report_abuse_count", reportAbuseCount);

                    String iconUrlPattern = conf.checkGetString("platform.sysIconUrlPattern");
                    if (rec.getInt("type") == 1) {
                        rec.put("icon", "");
                    }
                    if (rec.getInt("type") == 32) {
                        rec.put("icon", String.format(iconUrlPattern, "apk.gif"));
                    }
                    if (rec.getInt("type") == 256) {
                        rec.put("icon", String.format(iconUrlPattern, "comment.gif"));
                    }
                    if (rec.getInt("type") == 512) {
                        rec.put("icon", String.format(iconUrlPattern, "like.gif"));
                    }
                    if (rec.getInt("type") == 64) {
                        rec.put("icon", String.format(iconUrlPattern, "link.gif"));
                    }
                    if (rec.getInt("type") == 4096) {
                        rec.put("icon", String.format(iconUrlPattern, "friend.gif"));
                    }

                    String add_to_user = rec.getString("add_to");
                    ec.record("-----------type reportAbuse--end-------");

                    ec.record("-----------type getUsersBaseColumns start---------");
                    if (add_to_user.length() > 0) {

                        //modify by wangpeng at 2013-04-27 change getUsers to getUsersBaseColumn
                        //RecordSet recs = accountLogic.getUsers(ctx, viewerId, add_to_user, Constants.USER_LIGHT_COLUMNS_LIGHT);
                        RecordSet recs = accountLogic.getUsersBaseColumns(ctx, add_to_user);
                        rec.put("add_new_users", recs.toString());
                    } else {
                        rec.put("add_new_users", new RecordSet());
                    }
                    ec.record("-----------type getUsersBaseColumns end---------");
                }
                ec.record("END DEAL WITH KIND OF BUSSINESS");
            }
            //L.debug(ctx, "after format recs=" + reds);

            //want merge，if stream from like and comment
            RecordSet out1Rs = new RecordSet();
            out1Rs = reds;
        ec.record("START REPORTABUSE");
            for (Record moveR : reds) {
                if (!single_get) {
                    if ((int) moveR.getInt("report_abuse_count") >= Constants.REPORT_ABUSE_COUNT)
                        moveR.put("message", "###DELETE###");
                    if (reportAbuseLogic.iHaveReport(ctx, viewerId, Constants.POST_OBJECT,moveR.getString("post_id"),Constants.APP_TYPE_BPC) >= 1)
                        moveR.put("message", "###DELETE###");
                }

                int type0 = (int) moveR.getInt("type");
                String target0 = moveR.getString("target");
                long updated_time0 = moveR.getInt("updated_time");
                if (type0 != 256 && type0 != 512) {
                } else {
                    for (Record check : out1Rs) {
                        //int type1 = (int) check.getInt("type");
                        String target1 = check.getString("target");
                        long updated_time1 = check.getInt("updated_time");

                        //if(type0==type1 && target0.equals(target1) && updated_time0<updated_time1){
                        if (target0.equals(target1) && updated_time0 < updated_time1) {
                            moveR.put("message", "###DELETE###");
                            break;
                        }
                    }
                }
            }

            for (Record moveR : reds) {//
                //String s = moveR.getString("from");
                if (moveR.getString("from").length() < 10)
                    moveR.put("message", "###DELETE###");
            }
            for (int jj = reds.size() - 1; jj >= 0; jj--) {
                Record p = reds.get(jj);
                if (p.getString("message").equals("###DELETE###")) {
                    reds.remove(jj);
                }
            }
        ec.record("END REPORT ABUSE");
            L.debug(ctx, "after remove report get recs=" + reds);
        ec.record("START SUBSCRIBE ");
            attachSubscribe(ctx, reds);
        ec.record("END SUBSCRIBE ");
            L.traceEndCall(ctx, METHOD);

        ec.record("END oldTransTimelineForQiupuP");
            return reds;
        }

    public RecordSet getFriendsTimelineForQiuPuP(Context ctx, String viewerId, String userId, String circleIds, String cols, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getFriendsTimelineP(ctx, userId, circleIds, cols, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getFullFriendsTimelineP(Context ctx, String userId, String circleIds, long since, long max, int type, String appId, int page, int count) {
        return getFriendsTimelineP(ctx, userId, circleIds, Constants.POST_FULL_COLUMNS, since, max, type, appId, page, count);
    }

    public RecordSet getFullFriendsTimelineForQiuPuP(Context ctx, String viewerId, String userId, String circleIds, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getFriendsTimelineP(ctx, userId, circleIds, Constants.POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getFriendsTimelineP(Context ctx, String userId, String circleIds, String cols, long since, long max, int type, String appId, int page, int count) {
        final String METHOD = "getFriendsTimelineP";
        L.traceStartCall(ctx, METHOD, userId, circleIds, cols, since, max, type, appId, page, count);
//        if (circleIds.equals(""))
//            circleIds = Integer.toString(Constants.FRIENDS_CIRCLE);
        GroupLogic groupLogic = GlobalLogics.getGroup();
        List<String> l = StringUtils2.splitList(circleIds, ",", true);
        List<String> groupsFromCircleIds = GlobalLogics.getGroup().getGroupIdsFromMentions(ctx, l);
        l.removeAll(groupsFromCircleIds);
        PageLogicUtils.removeAllPageIds(ctx, l);


        List<String> friendIds = new ArrayList<String>();
        if (!l.isEmpty()) {
            RecordSet recs = GlobalLogics.getFriendship().getFriendsP(ctx, userId, userId, joinIgnoreBlank(",", l), "user_id", false, 0, 1000);
            friendIds = recs.getStringColumnValues("user_id");
            if (circleIds.equals(Integer.toString(Constants.FRIENDS_CIRCLE))) {
                friendIds.add(userId); // Add me
            }
        }
        friendIds.addAll(groupsFromCircleIds);

        if (circleIds.equals(Integer.toString(Constants.FRIENDS_CIRCLE))) {
            String groupIds = groupLogic.findGroupIdsByMember(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.ACTIVITY_ID_BEGIN, Long.parseLong(userId));
            friendIds.addAll(StringUtils2.splitList(groupIds, ",", true));
            String eventIds = groupLogic.findGroupIdsByMember(ctx, Constants.EVENT_ID_BEGIN, Constants.EVENT_ID_END, Long.parseLong(userId));
            friendIds.addAll(StringUtils2.splitList(eventIds, ",", true));
        }


        String frendIds = StringUtils.join(friendIds, ",");
        frendIds = GlobalLogics.getIgnore().formatIgnoreUsers(ctx, userId, frendIds);
        L.debug(ctx, "get friendIds=" + friendIds);
        RecordSet recs_out = getUsersPosts(ctx, userId, frendIds, circleIds, cols, since, max, type, appId, page, count);
        L.debug(ctx,"get post recs_out="+recs_out);
        recs_out = GlobalLogics.getIgnore().formatIgnoreStreamOrCommentsP(ctx, userId, "stream", recs_out);
        L.debug(ctx,"get post format ignore recs_out="+recs_out);
        L.traceEndCall(ctx, METHOD);
        return recs_out;
    }

    public RecordSet getMyShareFullTimelineP(Context ctx, String viewerId, String userIds, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getMyShareP(ctx, viewerId, userIds, Constants.POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getMyShareTimelineP(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getMyShareP(ctx, viewerId, userIds, cols, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getMyShareP(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) {
        final String METHOD = "getMyShareP";
        L.traceStartCall(ctx, METHOD, userIds, cols, since, max, type, appId, page, count);
        List<String> users = StringUtils2.splitList(userIds, ",", true);
        List<String> groups = GlobalLogics.getGroup().getGroupIdsFromMentions(ctx, users);
        users.removeAll(groups);
        PageLogicUtils.removeAllPageIds(ctx, users);

        if (CollectionUtils.isNotEmpty(users) && !GlobalLogics.getAccount().hasAllUsers(ctx, joinIgnoreBlank(",", users))) {
            throw new ServerException(WutongErrors.USER_NOT_EXISTS, "Users is not exists");
        }

        userIds = GlobalLogics.getIgnore().formatIgnoreUsers(ctx, viewerId, userIds);
        L.debug(ctx, "format ignore users=" + userIds);
        RecordSet recs = getMySharePosts(ctx, viewerId, userIds, cols, since, max, type, appId, page, count);
        recs = GlobalLogics.getIgnore().formatIgnoreStreamOrCommentsP(ctx, viewerId, "stream", recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getFullUsersTimelineForQiuPuP(Context ctx, String viewerId, String userIds, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getUsersTimelineP(ctx, viewerId, userIds, Constants.POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getFullRelatedTimelineForQiupuP(Context ctx, String userId, long since, long max, int type, String appId, int page, int count, List<String> groupIds) {
        return transTimelineForQiupuP(ctx, userId, getRelatedTimelineP(ctx, userId, Constants.POST_FULL_COLUMNS, since, max, type, appId, page, count, groupIds), 2, 5, false);
    }

    public RecordSet getFullUsersTimelineP(Context ctx, String viewerId, String userIds, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getUsersTimelineP(ctx, viewerId, userIds, Constants.POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getUsersTimelineForQiuPuP(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getUsersTimelineP(ctx, viewerId, userIds, cols, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getRelatedTimelineForQiupuP(Context ctx, String userId, String cols, long since, long max, int type, String appId, int page, int count, List<String> groupIds) {
        return transTimelineForQiupuP(ctx, userId, getRelatedTimelineP(ctx, userId, cols, since, max, type, appId, page, count, groupIds), 2, 5, false);
    }

    public RecordSet getUsersTimelineP(Context ctx, String viewerId, String userIds, String cols, long since, long max, int type, String appId, int page, int count) {
        userIds = GlobalLogics.getIgnore().formatIgnoreUsers(ctx, viewerId, userIds);
        ElapsedCounter ec = ctx.getElapsedCounter();
        ec.record("------getUsersPosts start------");
        RecordSet recs = getUsersPosts(ctx, viewerId, userIds, "", cols, since, max, type, appId, page, count);
        ec.record("------getUsersPosts end------");

        ec.record("------formatIgnoreStreamOrCommentsP start------");
        recs = GlobalLogics.getIgnore().formatIgnoreStreamOrCommentsP(ctx, viewerId, "stream", recs);
        ec.record("------formatIgnoreStreamOrCommentsP end------");
        return recs;
    }

    public RecordSet getRelatedTimelineP(Context ctx, String userId, String cols, long since, long max, int type, String appId, int page, int count, List<String> groupIds) {
        RecordSet recs = getRelatedPosts(ctx, userId, cols, since, max, type, appId, page, count, groupIds);
        recs = GlobalLogics.getIgnore().formatIgnoreStreamOrCommentsP(ctx, userId, "stream", recs);
        return recs;
    }

    public RecordSet getPublicTimelineP(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count) {
        return getUsersTimelineP(ctx, viewerId, "", cols, since, max, type, appId, page, count);
    }

    public RecordSet getFullPublicTimelineP(Context ctx, String viewerId, long since, long max, int type, String appId, int page, int count) {
        return getPublicTimelineP(ctx, viewerId, Constants.POST_FULL_COLUMNS, since, max, type, appId, page, count);
    }

    public RecordSet getPublicTimelineForQiuPuP(Context ctx, String viewerId, String cols, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getPublicTimelineP(ctx, viewerId, cols, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getFullPublicTimelineForQiuPuP(Context ctx, String viewerId, long since, long max, int type, String appId, int page, int count) {
        return transTimelineForQiupuP(ctx, viewerId, getPublicTimelineP(ctx, viewerId, Constants.POST_FULL_COLUMNS, since, max, type, appId, page, count), 2, 5, false);
    }

    public RecordSet getHotStream(Context ctx, String viewerId, String circle_ids, String cols, int type, long max, long min, int page, int count) {
        final String METHOD = "getHotStream";
        long start = System.currentTimeMillis();
        L.traceStartCall(ctx, METHOD, circle_ids, cols, type, max, min, page, count);
        List<String> circle_id_list = StringUtils2.splitList(circle_ids, ",", true);
        String circle_id0 = "";
        if (circle_id_list.size() > 0)
            circle_id0 = circle_id_list.get(0);
        String postIds = "";
        RecordSet recs0 = new RecordSet();
        if (circle_id0.equals("")) {
            recs0 = getUsersTimelineP(ctx, viewerId, viewerId, "post_id,created_time", min, max, type, "1", 0, 200);
        } else {
            if (circle_id0.length() > 5) {
                recs0 = getUsersTimelineP(ctx, viewerId, circle_id0, "post_id,created_time", min, max, type, "1", 0, 200);
            } else {
                //local circle
                if (circle_id0.startsWith("#"))
                    circle_id0 = circle_id0.replace("#", "");
                RecordSet recs01 = GlobalLogics.getFriendship().getCircles(ctx, viewerId, circle_id0, true);
                String users = recs01.getFirstRecord().getString("members");
                if (users.length() > 0)
                    recs0 = getUsersTimelineP(ctx, viewerId, users, "post_id,created_time", min, max, type, "1", 0, 200);
            }
        }

        System.out.println("------------------------------1-----------------------"+(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();


        L.debug(ctx, "get stream in db before get hot:" + recs0.toString());
        List<String> l = new ArrayList<String>();
        String [] postIdList = new String[recs0.size()];
        for(Record r:recs0){
            String id = r.getString("post_id");
            if(StringUtils.isNotBlank(id))
                l.add(String.valueOf(Constants.POST_OBJECT) + ":"+id);
        }
        l.toArray(postIdList);

        //String[] postIdList = recs0.getStringColumnValuesAsArray("post_id");
        CommentLogic comment = GlobalLogics.getComment();
        Map<String,Integer> rsComment  = comment.getCommentCounts(ctx,viewerId,postIdList);

        LikeLogic like = GlobalLogics.getLike();
        Map<String,Integer> rsLike = like.getLikeCounts(ctx, postIdList);


        //rsComment.addAll(rsLike);

        //comment.getCommentCount()
        for (Record record : recs0) {
            String target  = String.valueOf(Constants.POST_OBJECT) + ":" + record.getString("post_id");
            //get comment count
            //int c_count = GlobalLogics.getComment().getCommentCount(ctx, viewerId, String.valueOf(Constants.POST_OBJECT) + ":" + record.getString("post_id"));
            //get like count
            //int l_count = GlobalLogics.getLike().getLikeCount(ctx, String.valueOf(Constants.POST_OBJECT) + ":" + record.getString("post_id"));

            //get time
            DecimalFormat df2 = new DecimalFormat("###.0");
            long ori_time = record.getInt("created_time");
            long now_time = DateUtils.nowMillis();
            long date_diff = now_time - ori_time;
            long req_diff = max - min;
            long m = (req_diff - date_diff) * 100 / req_diff;
            int all_count = (int) (m * 10) + rsComment.get(target) * 40 + rsLike.get(target) * 10;
            record.put("all_count", all_count);
        }

        System.out.println("------------------------------2-----------------------"+(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();


        L.debug(ctx, "get hot stream before get count:" + recs0.toString());
        recs0.sort("all_count", false);
        L.debug(ctx, "get hot stream after sort:" + recs0.toString());
        recs0.sliceByPage(page, count);
        L.debug(ctx, "get hot stream after slice page:" + recs0.toString());
        postIds = recs0.joinColumnValues("post_id", ",");
        RecordSet recs_stream = getPosts(ctx, postIds, cols.equals("") ? Constants.POST_FULL_COLUMNS : cols);
        RecordSet recordSet = transTimelineForQiupuP(ctx, viewerId, recs_stream, 2, 5, false);
        System.out.println("------------------------------3-----------------------"+(System.currentTimeMillis()-start));
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    public Record findStreamTempP(Context ctx, String postId, String cols) {
        return findStreamTemp(ctx, postId, cols);
    }

    public RecordSet getPostsForQiuPuP(Context ctx, String viewerId, String postsIds, String cols, boolean single_get) {
        return transTimelineForQiupuP(ctx, viewerId, getPosts(ctx, postsIds, cols), 5, 5, single_get);
    }

    public RecordSet getFullPostsForQiuPuP(Context ctx, String viewerId, String postIds, boolean single_get) {
        return transTimelineForQiupuP(ctx, viewerId, getPosts(ctx, postIds, Constants.POST_FULL_COLUMNS), 5, 5, single_get);
    }

    public boolean updateStreamCanCommentOrcanLike(Context ctx, String post_id, String viewerId, Record rec) {
        final String METHOD = "updateStreamCanCommentOrcanLike";
        L.traceStartCall(ctx, METHOD, post_id, rec);
        String can_comment = rec.getString("can_comment");
        String can_like = rec.getString("can_like");
        String can_reshare = rec.getString("can_reshare");
        if (can_comment.length() > 0) {
            int v = 0;
            if (rec.getString("can_comment").equalsIgnoreCase("true") || can_comment.equals("1")) {
                v = 1;
            }
            updatePostForCommentOrLike(ctx, post_id, viewerId, "can_comment", v);
        }
        if (can_like.length() > 0) {
            int v = 0;
            if (rec.getString("can_like").equalsIgnoreCase("true") || can_like.equals("1")) {
                v = 1;
            }
            updatePostForCommentOrLike(ctx, post_id, viewerId, "can_like", v);
        }
        if (can_reshare.length() > 0) {
            int v = 0;
            if (rec.getString("can_reshare").equalsIgnoreCase("true") || can_reshare.equals("1")) {
                v = 1;
            }
            updatePostForCommentOrLike(ctx, post_id, viewerId, "can_reshare", v);
        }

        L.traceEndCall(ctx, METHOD);
        return true;
    }

    public boolean updatePostP(Context ctx, String userId, String postId, String message) {
        final String METHOD = "updatePostP";
        L.traceStartCall(ctx, METHOD, postId, message);
        Record rec = new Record();
        rec.putIf("message", message, message != null);
        boolean b = updatePost(ctx, userId, postId, rec);
        L.traceEndCall(ctx, METHOD);
        return b;
    }

    public String repostP(Context ctx, String userId, String mentions, boolean secretly, String postId, String newMessage, String device, String location, String appData, boolean can_comment, boolean can_like, boolean can_reshare, String add_to, boolean sendEmail, boolean sendSms, boolean isTop) {
        final String METHOD = "repostP";
        L.traceStartCall(ctx, METHOD, mentions, secretly, postId, newMessage, device, location, appData, can_comment, can_like, can_reshare, add_to);
        Commons commons = new Commons();
        ConversationLogic conversationLogic = GlobalLogics.getConversation();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        List<String> emails = commons.getEmails(mentions);
        List<String> phones = commons.getPhones(mentions);
        mentions = commons.getOldMentions(ctx, userId, mentions);
        List<String> add_contact = new ArrayList<String>();
        add_contact.addAll(emails);
        add_contact.addAll(phones);
        for (int k = add_contact.size() - 1; k >= 0; k--) {
            String virtualFriendId = GlobalLogics.getFriendship().getUserFriendHasVirtualFriendId(ctx, userId, add_contact.get(k));
            if (!StringUtils.equals(virtualFriendId, "0")) {
                add_contact.remove(k);
            }
        }
        String add_contact_s = StringUtils.join(add_contact, ",");
        boolean has_contact = false;
        if (add_contact_s.length() > 0)
            has_contact = true;

        if (mentions.length() > 0)
            mentions = commons.parseUserIds(ctx, userId, mentions);
        String newPostId = createRepost(ctx, userId, mentions, secretly, postId, newMessage != null ? newMessage : "", device, location, appData, can_comment, can_like, can_reshare, add_to, add_contact_s, has_contact);
        conversationLogic.createConversationP(ctx, Constants.POST_OBJECT, postId, Constants.C_STREAM_RESHARE, userId);
        conversationLogic.createConversationP(ctx, Constants.POST_OBJECT, newPostId, Constants.C_STREAM_POST, userId);
        if (add_to.length() > 0)
            conversationLogic.createConversationP(ctx, Constants.POST_OBJECT, newPostId, Constants.C_STREAM_ADDTO, add_to);
        Record thisUser = accountLogic.getUserByIdBaseColumns(ctx, userId).getFirstRecord();
        Record old_stream = getPostP(ctx, postId, "post_id,mentions,source,target,type,message,attachments");
//                Record new_stream = getPost(newPostId, "post_id,source,message");
        String body = newMessage;

        commons.sendNotification(ctx, Constants.NTF_MY_STREAM_RETWEET,
                commons.createArrayNodeFromStrings(),
                commons.createArrayNodeFromStrings(userId),
                commons.createArrayNodeFromStrings(newPostId, userId, thisUser.getString("display_name"), postId, old_stream.getString("source"), old_stream.getString("message")),
                commons.createArrayNodeFromStrings(),
                commons.createArrayNodeFromStrings(),
                commons.createArrayNodeFromStrings(newPostId),
                commons.createArrayNodeFromStrings(newPostId, userId, thisUser.getString("display_name"), postId, old_stream.getString("source"), old_stream.getString("message")),
                commons.createArrayNodeFromStrings(newMessage),
                commons.createArrayNodeFromStrings(newMessage),
                commons.createArrayNodeFromStrings(postId),
                commons.createArrayNodeFromStrings(postId, userId, newPostId)
        );

        String displayName = thisUser.getString("display_name");
//                NotificationSender notif2 = new SharedNotifSender(this, null);
        int type = (int) old_stream.getInt("type");

        commons.sendNotification(ctx, Constants.NTF_MY_APP_COMMENT,
                commons.createArrayNodeFromStrings(String.valueOf(type)),
                commons.createArrayNodeFromStrings(userId),
                commons.createArrayNodeFromStrings(String.valueOf(type), displayName),
                commons.createArrayNodeFromStrings(),
                commons.createArrayNodeFromStrings(),
                commons.createArrayNodeFromStrings(newPostId),
                commons.createArrayNodeFromStrings(String.valueOf(type), userId, displayName),
                commons.createArrayNodeFromStrings(body),
                commons.createArrayNodeFromStrings(body),
                commons.createArrayNodeFromStrings(),
                commons.createArrayNodeFromStrings(newPostId)
        );

        List<String> l = StringUtils2.splitList(mentions, ",", true);
        for (String l0 : l) {
            long id = Long.parseLong(l0);
            if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                    && (id <= Constants.GROUP_ID_END))
            GlobalLogics.getGroup().sendGroupNotification(ctx, id, new RecordSet(), new SharedNotifSender(), userId, new Object[]{newPostId}, body, String.valueOf(type), displayName);
            boolean hasRight = GlobalLogics.getGroup().hasRight(ctx, id, ctx.getViewerId(), Constants.ROLE_ADMIN);
            if (hasRight && sendEmail) {
                GlobalLogics.getGroup().sendGroupEmail(ctx, id, new RecordSet(), new SharedNotifSender(), userId, new Object[]{newPostId}, body, String.valueOf(type), displayName);
            }
            if (hasRight && sendSms) {
                GlobalLogics.getGroup().sendGroupSms(ctx, id, new RecordSet(), new SharedNotifSender(), userId, new Object[]{newPostId}, body, String.valueOf(type), displayName);
            }
            if (hasRight && isTop) {
                groupTopPostsSet(ctx, id, newPostId);
            }
        }

        //not borqs account
        String body2 = old_stream.getString("message", "");
        if (type == Constants.APK_POST) {
            Record mcs = commons.thisTrandsGetApkInfo(ctx, userId, old_stream.getString("target"), "app_name", 1000).getFirstRecord();
            body2 = mcs.getString("app_name");
        } else if ((type == Constants.APK_LINK_POST)) {
            String attachments = old_stream.getString("attachments");
            ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
            if ((aNode != null) && (aNode.get(0) != null))
                body2 = aNode.get(0).get("href").getTextValue();
        } else if (type == Constants.BOOK_POST) {
            String attachments = old_stream.getString("attachments");
            if (attachments.length() >= 2) {
                ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                if ((aNode != null) && (aNode.get(0) != null))
                    body2 = aNode.get(0).get("summary").getTextValue();
            }
        } else if (type == Constants.PHOTO_POST) {
            String attachments = old_stream.getString("attachments");
            RecordSet r0 = RecordSet.fromJson(attachments);
            if (!r0.getFirstRecord().isEmpty()) {
                body2 = r0.getFirstRecord().getString("photo_caption");
            }
        } else if (type == Constants.LINK_POST) {
            String attachments = old_stream.getString("attachments");
            if (attachments.length() > 2) {
                ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                if ((aNode != null) && (aNode.get(0) != null))
                    body2 = aNode.get(0).get("url").getTextValue();
            }
        }

        //not borqs account
        String emailContent = commons.composeShareContent(ctx, userId, type, body2, true, device);
        String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        String template = Constants.getBundleString(device, "platform.compose.share.title");
        String title = SQLTemplate.merge(template, new Object[][]{
                {"displayName", displayName}
        });
        for (String email : emails) {
            String uid = accountLogic.findUserIdByUserName(ctx, email);
            if (StringUtils.equals(uid, "0") || StringUtils.isBlank(uid)) {
                Record setting = GlobalLogics.getSetting().getByUsers(ctx, Constants.EMAIL_SHARE_TO, email);
                String value = setting.getString(email, "0");
                if (value.equals("0")) {
                    GlobalLogics.getEmail().sendEmail(ctx,title, email, email, emailContent, Constants.EMAIL_SHARE_TO, lang);
                }
            } else if (commons.sendEmail) {
                Record setting = GlobalLogics.getSetting().getByUsers(ctx, Constants.EMAIL_SHARE_TO, uid);
                String value = setting.getString(uid, "0");
                if (value.equals("0")) {
                    GlobalLogics.getEmail().sendEmail(ctx,title, email, email, emailContent, Constants.EMAIL_SHARE_TO, lang);
                }
            }
        }

        String smsContent = commons.composeShareContent(ctx, userId, type, body2, false, device);
        for (String phone : phones) {
            String uid = accountLogic.findUserIdByUserName(ctx, phone);
            if (StringUtils.equals(uid, "0") || StringUtils.isBlank(uid)) {
                commons.sendSms(ctx, phone, smsContent + "\\");
                template = Constants.getBundleString(device, "platform.compose.share.download");
                String download = SQLTemplate.merge(template, new Object[][]{
                        {"serverHost", commons.SERVER_HOST}
                });
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {

                }
                commons.sendSms(ctx, phone, download + "\\");
            } else if (commons.sendSms) {
                commons.sendSms(ctx, phone, smsContent + "\\");
            }
        }

        //subscribe
        ArrayList<String> reasons = new ArrayList<String>();
        reasons.add(String.valueOf(Constants.C_SUBSCRIBE_USER));
        reasons.add(String.valueOf(Constants.C_SUBSCRIBE_LOCAL_CIRCLE));
        ConversationLogic conversation = GlobalLogics.getConversation();
        RecordSet convUsers = conversation.getConversation(ctx, Constants.USER_OBJECT, userId, reasons, 0, 0, 1000);
        for (Record convUser : convUsers) {
            String receiverId = convUser.getString("from_");
            if (canReceiveSubscribeNotification(ctx, secretly, mentions, receiverId)) {
                commons.sendNotification(ctx, Constants.NTF_SUBSCRIBE,
                        commons.createArrayNodeFromStrings(ctx.getAppId()),
                        commons.createArrayNodeFromStrings(userId),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(receiverId)
                );
            }
        }

        L.traceEndCall(ctx, METHOD);
        return newPostId;
    }

    public boolean destroyPostsP(Context ctx, String userId, String postIds) {
        final String METHOD = "destroyPostsP";
        L.traceStartCall(ctx, METHOD, postIds);
        List<String> p = StringUtils2.splitList(postIds, ",", true);
        for (String p0 : p) {
            GlobalLogics.getConversation().deleteConversationP(ctx, Constants.POST_OBJECT, p0, -1, 0);
        }
        boolean b = destroyPosts(ctx, userId, postIds).getBoolean("result", false);

        L.traceEndCall(ctx, METHOD);
        return b;
    }

    public Record sendShareLinkP(Context ctx, String userId, String msg, String appId, String mentions, String app_data,
                                 boolean secretly, String device, String location, String url, String title, String linkImagAddr, boolean can_comment, boolean can_like, boolean can_reshare, String add_to, boolean sendEmail, boolean sendSms, boolean isTop, long scene) {
        final String METHOD = "sendShareLinkP";
        L.traceStartCall(ctx, METHOD, userId, msg, appId, mentions, app_data, secretly, device, location, url, title, linkImagAddr, can_comment, can_like, can_reshare, add_to);
        List<String> emails = Commons.getEmails(mentions);
        List<String> phones = Commons.getPhones(mentions);
        mentions = Commons.getOldMentions(ctx, userId, mentions);

        List<String> add_contact = new ArrayList<String>();
        add_contact.addAll(emails);
        add_contact.addAll(phones);
        for (int k = add_contact.size() - 1; k >= 0; k--) {
            String virtualFriendId = GlobalLogics.getFriendship().getUserFriendHasVirtualFriendId(ctx, userId, add_contact.get(k));
            if (!StringUtils.equals(virtualFriendId, "0")) {
                add_contact.remove(k);
            }
        }
        String add_contact_s = StringUtils.join(add_contact, ",");
        boolean has_contact = false;
        if (add_contact_s.length() > 0)
            has_contact = true;

        Record nowPost = Record.of("message", msg, "app", appId);
        nowPost.put("type", String.valueOf(Constants.LINK_POST));
        nowPost.put("target", url);
        nowPost.put("device", device);
        nowPost.put("app_data", app_data);
        nowPost.put("mentions", Commons.parseUserIds(ctx, userId, mentions));
        nowPost.put("privince", secretly);
        nowPost.put("location", location);
        nowPost.put("can_comment", can_comment);
        nowPost.put("can_like", can_like);
        nowPost.put("can_reshare", can_reshare);
        nowPost.put("add_to", add_to);

        nowPost.put("add_contact", add_contact_s);
        nowPost.put("has_contact", has_contact);
        nowPost.put("scene", scene);

        RecordSet t = new RecordSet();
        Record at = Record.of("url", url, "title", title);
        String h = "";
        try {
            URL ur = new URL(url);
            h = ur.getHost();
        } catch (MalformedURLException e) {

        }
        at.put("host", h);
        at.put("description", "");
        at.put("img_url", "");
        at.put("many_img_url", "[]");
        t.add(at);
        nowPost.put("attachments", t);

        Record post = Record.of("userId", userId);
        post.put("url", url);
        post.put("linkImagAddr", linkImagAddr);
        L.debug(ctx,"--------------------linkstart----------------------------");
        Record mock = postP(ctx, userId, nowPost, emails, phones, appId, sendEmail, sendSms, isTop);
        String post_id = mock.checkGetString("post_id");
        L.debug(ctx,"--------------------linkend----------------------------postid="+post_id);
        post.put("post_id", post_id);
        L.debug(ctx, "send share link format post=" + post);
        MQ mq = MQCollection.getMQ("platform");
        if (mq != null)
            mq.send("link", post.toString(false, false));

        L.traceEndCall(ctx, METHOD);
        return mock;
    }

    private String groupTopPostsSet(Context ctx, long groupId, String postId) {
        String viewerId = ctx.getViewerIdString();
        RecordSet recs = GlobalLogics.getGroup().getSimpleGroups(ctx, Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END, String.valueOf(groupId), Constants.COMM_COL_TOP_POSTS);
        String oldPostIds = recs.get(0).getString(Constants.COMM_COL_TOP_POSTS);
        Set<String> postIds = StringUtils2.splitSet(oldPostIds, ",", true);
        String set = postId;
        String unset = "";
        Set<String> sl = StringUtils2.splitSet(set, ",", true);
        Set<String> ul = StringUtils2.splitSet(unset, ",", true);
        postIds.addAll(sl);
        postIds.removeAll(ul);
        String topPosts = StringUtils2.joinIgnoreBlank(",", postIds);

        Record info = new Record();
        Record properties = new Record();
        properties.put(Constants.COMM_COL_TOP_POSTS, topPosts);
        boolean result = GlobalLogics.getGroup().updateGroup(ctx, groupId, info, properties);
        if (result)
            return topPosts;
        else
            return oldPostIds;
    }

    private Runnable afterPost(final Context ctx, final String userId, final Record post, final List<String> emails, final List<String> phones, final String appId,
                               final boolean sendEmail, final boolean sendSms, final boolean isTop, final Record r_post) {
        return new Thread() {
            @Override
            public void run() {
                ConversationLogic conversationLogic = GlobalLogics.getConversation();
                String postId = r_post.getString("post_id");
                String body = r_post.getString("message", "");
                String message = body;
                Record thisUser = GlobalLogics.getAccount().getUserByIdBaseColumns(ctx, userId).getFirstRecord();
                String displayName = thisUser.getString("display_name");
                int type = Integer.valueOf(r_post.getString("type"));
                if (type == Constants.APK_POST) {
                    if (post.getString("target").split("-")[0].length() > 0)
                        conversationLogic.createConversationP(ctx, Constants.APK_OBJECT, post.getString("target").split("-")[0].toString(), Constants.C_APK_SHARE, userId);
//                NotificationSender notif = new SharedAppNotifSender(this, null);

                    Record mcs = Commons.thisTrandsGetApkInfo(ctx, userId, r_post.getString("target"), "app_name", 1000).getFirstRecord();
                    body = mcs.getString("app_name");

                    Commons.sendNotification(ctx, Constants.NTF_APP_SHARE,
                            Commons.createArrayNodeFromStrings(appId),
                            Commons.createArrayNodeFromStrings(userId),
                            Commons.createArrayNodeFromStrings(r_post.getString("target"), userId, displayName, mcs.getString("app_name")),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(r_post.getString("target")),
                            Commons.createArrayNodeFromStrings(r_post.getString("target"), userId, displayName, mcs.getString("app_name")),
                            Commons.createArrayNodeFromStrings(body),
                            Commons.createArrayNodeFromStrings(body),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(postId, userId)
                    );

                    String mentions = post.getString("mentions");
                    List<String> l = StringUtils2.splitList(mentions, ",", true);
                    for (String l0 : l) {
                        long id = Long.parseLong(l0);
                        if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                                && (id <= Constants.GROUP_ID_END)) {
                            GlobalLogics.getGroup().sendGroupNotification(ctx, id, RecordSet.of(mcs), new SharedAppNotifSender(), userId, new Object[]{postId, userId}, body, r_post.getString("target"), userId, displayName, mcs.getString("app_name"));
                            boolean hasRight = GlobalLogics.getGroup().hasRight(ctx, id, ctx.getViewerId(), Constants.ROLE_ADMIN);
                            if (hasRight && sendEmail) {
                                GlobalLogics.getGroup().sendGroupEmail(ctx, id, RecordSet.of(mcs), new SharedAppNotifSender(), userId, new Object[]{postId, userId}, body, r_post.getString("target"), userId, displayName, mcs.getString("app_name"));
                            }
                            if (hasRight && sendSms) {
                                GlobalLogics.getGroup().sendGroupSms(ctx, id, RecordSet.of(mcs), new SharedAppNotifSender(), userId, new Object[]{postId, userId}, body, r_post.getString("target"), userId, displayName, mcs.getString("app_name"));
                            }
                            if (hasRight && isTop) {
                                groupTopPostsSet(ctx, id, postId);
                            }
                        }
                        //TODO-----------add send email to a friend--------------
                        int userType = Constants.getUserTypeById(id);
                        if (userType == Constants.USER_OBJECT) {
                            if (sendEmail) {
                                EmailLogic email = GlobalLogics.getEmail();
                                Record user = GlobalLogics.getAccount().getUserByIdBaseColumns(ctx, String.valueOf(id)).getFirstRecord();
                                List<String> list = GlobalLogics.getAccount().getEmailFromUsers(ctx,id);
                                for(String str:list)
                                    email.sendEmail(ctx, "You have new message", str, user.getString("display_name"), message, "email.essential", ctx.getLanguage());
                            }
                        }
                    }
                } else if ((type == Constants.TEXT_POST) || (type == Constants.LINK_POST) || (type == Constants.APK_LINK_POST) || type == Constants.BOOK_POST) {
                    //notification
                    if ((type == Constants.APK_LINK_POST)) {
                        String attachments = r_post.getString("attachments");
                        ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                        if ((aNode != null) && (aNode.get(0) != null))
                            body = aNode.get(0).get("href").getTextValue();
                    } else if (type == Constants.BOOK_POST) {
                        String attachments = post.getString("attachments");
                        if (attachments.length() >= 2) {
                            ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                            if ((aNode != null) && (aNode.get(0) != null))
                                body = aNode.get(0).get("summary").getTextValue();
                        }
                    } else if (type == Constants.LINK_POST) {
                        String attachments = post.getString("attachments");
                        if (attachments.length() > 2) {
                            ArrayNode aNode = (ArrayNode) JsonUtils.parse(attachments);
                            if ((aNode != null) && (aNode.get(0) != null))
                                body = aNode.get(0).get("url").getTextValue();
                        }
                    }

                    Commons.sendNotification(ctx, Constants.NTF_OTHER_SHARE,
                            Commons.createArrayNodeFromStrings(String.valueOf(type)),
                            Commons.createArrayNodeFromStrings(userId),
                            Commons.createArrayNodeFromStrings(String.valueOf(type), displayName),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(postId),
                            Commons.createArrayNodeFromStrings(String.valueOf(type), userId, displayName),
                            Commons.createArrayNodeFromStrings(body),
                            Commons.createArrayNodeFromStrings(body),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(postId)
                    );

                    String mentions = post.getString("mentions");
                    List<String> l = StringUtils2.splitList(mentions, ",", true);
                    for (String l0 : l) {
                        long id = Long.parseLong(l0);
                        if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                                && (id <= Constants.GROUP_ID_END)) {
                            GlobalLogics.getGroup().sendGroupNotification(ctx, id, new RecordSet(), new SharedNotifSender(), userId, new Object[]{postId}, body, String.valueOf(type), displayName);
                            boolean hasRight = GlobalLogics.getGroup().hasRight(ctx, id, ctx.getViewerId(), Constants.ROLE_ADMIN);
                            if (hasRight && sendEmail) {
                                GlobalLogics.getGroup().sendGroupEmail(ctx, id, new RecordSet(), new SharedNotifSender(), userId, new Object[]{postId}, body, String.valueOf(type), displayName);
                            }
                            if (hasRight && sendSms) {
                                GlobalLogics.getGroup().sendGroupSms(ctx, id, new RecordSet(), new SharedNotifSender(), userId, new Object[]{postId}, body, String.valueOf(type), displayName);
                            }
                            if (hasRight && isTop) {
                                groupTopPostsSet(ctx, id, postId);
                            }
                        }

                        //TODO-----------add send email to a friend--------------
                        int userType = Constants.getUserTypeById(id);
                        if (userType == Constants.USER_OBJECT) {
                            if (sendEmail) {
                                EmailLogic email = GlobalLogics.getEmail();
                                Record user = GlobalLogics.getAccount().getUserByIdBaseColumns(ctx, String.valueOf(id)).getFirstRecord();
                                List<String> list = GlobalLogics.getAccount().getEmailFromUsers(ctx,id);
                                for(String str:list)
                                    email.sendEmail(ctx, "You have new message", str, user.getString("display_name"), message, "email.essential", ctx.getLanguage());
                            }
                        }
                    }
                } else if (type == Constants.PHOTO_POST) {
                    String attachments = post.getString("attachments");
                    RecordSet r0 = RecordSet.fromJson(attachments);
                    if (!r0.getFirstRecord().isEmpty()) {
                        conversationLogic.createConversationP(ctx, Constants.PHOTO_OBJECT, r0.getFirstRecord().getString("photo_id"), Constants.C_PHOTO_SHARE, userId);
                        body = r0.getFirstRecord().getString("photo_caption");

                        String rAttachments = r_post.getString("attachments");
                        Record rAttachRec = RecordSet.fromJson(rAttachments).getFirstRecord();
                        if (!rAttachRec.has("have_notify")) {
                            Commons.sendNotification(ctx, Constants.NTF_PHOTO_SHARE,
                                    Commons.createArrayNodeFromStrings(String.valueOf(type)),
                                    Commons.createArrayNodeFromStrings(userId),
                                    Commons.createArrayNodeFromStrings(String.valueOf(type), displayName),
                                    Commons.createArrayNodeFromStrings(),
                                    Commons.createArrayNodeFromStrings(),
                                    Commons.createArrayNodeFromStrings(r0.getFirstRecord().getString("photo_id")),
                                    Commons.createArrayNodeFromStrings(String.valueOf(type), userId, displayName),
                                    Commons.createArrayNodeFromStrings(body),
                                    Commons.createArrayNodeFromStrings(body),
                                    Commons.createArrayNodeFromStrings("PHOTO"),
                                    Commons.createArrayNodeFromStrings(postId)
                            );
                        }
                    }

                    String mentions = post.getString("mentions");
                    List<String> l = StringUtils2.splitList(mentions, ",", true);
                    for (String l0 : l) {
                        long id = Long.parseLong(l0);
                        if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                                && (id <= Constants.GROUP_ID_END)) {
                            String rAttachments = r_post.getString("attachments");
                            RecordSet rAttachsRecs = RecordSet.fromJson(rAttachments);
                            GlobalLogics.getGroup().sendGroupNotification(ctx, id, rAttachsRecs, new PhotoSharedNotifSender(), userId, new Object[]{postId}, message, String.valueOf(type), displayName);
                            boolean hasRight = GlobalLogics.getGroup().hasRight(ctx, id, ctx.getViewerId(), Constants.ROLE_ADMIN);
                            if (hasRight && sendEmail) {
                                GlobalLogics.getGroup().sendGroupEmail(ctx, id, rAttachsRecs, new PhotoSharedNotifSender(), userId, new Object[]{postId}, message, String.valueOf(type), displayName);
                            }
                            if (hasRight && sendSms) {
                                GlobalLogics.getGroup().sendGroupSms(ctx, id, rAttachsRecs, new PhotoSharedNotifSender(), userId, new Object[]{postId}, message, String.valueOf(type), displayName);
                            }
                            if (hasRight && isTop) {
                                groupTopPostsSet(ctx, id, postId);
                            }
                        }
                        //TODO-----------add send email to a friend--------------
                        int userType = Constants.getUserTypeById(id);
                        if (userType == Constants.USER_OBJECT) {
                            if (sendEmail) {
                                EmailLogic email = GlobalLogics.getEmail();
                                Record user = GlobalLogics.getAccount().getUserByIdBaseColumns(ctx, String.valueOf(id)).getFirstRecord();
                                List<String> list = GlobalLogics.getAccount().getEmailFromUsers(ctx,id);
                                for(String str:list)
                                    email.sendEmail(ctx, "You have new message", str, user.getString("display_name"), message, "email.essential", ctx.getLanguage());
                            }
                        }
                    }
                } else if (type == Constants.FILE_POST || type == Constants.AUDIO_POST || type == Constants.VIDEO_POST) {
                    String attachments = post.getString("attachments");
                    RecordSet r0 = RecordSet.fromJson(attachments);
                    if (!r0.getFirstRecord().isEmpty()) {
                        conversationLogic.createConversationP(ctx, Constants.FILE_OBJECT, r0.getFirstRecord().getString("file_id"), Constants.C_FILE_SHARE, userId);
                        body = r0.getFirstRecord().getString("title");
                    }
                    Commons.sendNotification(ctx, Constants.NTF_FILE_SHARE,
                            Commons.createArrayNodeFromStrings(String.valueOf(type)),
                            Commons.createArrayNodeFromStrings(userId),
                            Commons.createArrayNodeFromStrings(String.valueOf(type), displayName, body),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(postId),
                            Commons.createArrayNodeFromStrings(String.valueOf(type), userId, displayName, body),
                            Commons.createArrayNodeFromStrings(body),
                            Commons.createArrayNodeFromStrings(body),
                            Commons.createArrayNodeFromStrings("FILE"),
                            Commons.createArrayNodeFromStrings(postId)
                    );

                    String mentions = post.getString("mentions");
                    List<String> l = StringUtils2.splitList(mentions, ",", true);
                    for (String l0 : l) {
                        long id = Long.parseLong(l0);
                        if ((id >= Constants.PUBLIC_CIRCLE_ID_BEGIN)
                                && (id <= Constants.GROUP_ID_END)) {
                            GlobalLogics.getGroup().sendGroupNotification(ctx, id, r0, new FileSharedNotifSender(), userId, new Object[]{postId}, body, String.valueOf(type), displayName, body);
                            boolean hasRight = GlobalLogics.getGroup().hasRight(ctx, id, ctx.getViewerId(), Constants.ROLE_ADMIN);
                            if (hasRight && sendEmail) {
                                GlobalLogics.getGroup().sendGroupEmail(ctx, id, r0, new FileSharedNotifSender(), userId, new Object[]{postId}, body, String.valueOf(type), displayName, body);
                            }
                            if (hasRight && sendSms) {
                                GlobalLogics.getGroup().sendGroupSms(ctx, id, r0, new FileSharedNotifSender(), userId, new Object[]{postId}, body, String.valueOf(type), displayName, body);
                            }
                            if (hasRight && isTop) {
                                groupTopPostsSet(ctx, id, postId);
                            }
                        }
                        //TODO-----------add send email to a friend--------------
                        int userType = Constants.getUserTypeById(id);
                        if (userType == Constants.USER_OBJECT) {
                            if (sendEmail) {
                                EmailLogic email = GlobalLogics.getEmail();
                                Record user = GlobalLogics.getAccount().getUserByIdBaseColumns(ctx, String.valueOf(id)).getFirstRecord();
                                List<String> list = GlobalLogics.getAccount().getEmailFromUsers(ctx,id);
                                for(String str:list)
                                    email.sendEmail(ctx, "You have new message", str, user.getString("display_name"), message, "email.essential", ctx.getLanguage());
                            }
                        }
                    }
                } else if (type == Constants.FRIEND_SET_POST) {

                }
                if ((type & Constants.APPLY_POST) == Constants.APPLY_POST && appId.equals("10001")) {   // 播思创意大赛
                    if ((type & Constants.TEXT_POST) == Constants.TEXT_POST) {    //只报名
                        body = "报名参加了播思创意大赛！";
                    } else {      //提交作品
                        String attachments = post.getString("attachments");
                        RecordSet r0 = RecordSet.fromJson(attachments);
                        if (!r0.getFirstRecord().isEmpty()) {
                            conversationLogic.createConversationP(ctx, Constants.FILE_OBJECT, r0.getFirstRecord().getString("file_id"), Constants.C_FILE_SHARE, userId);
                            body = r0.getFirstRecord().getString("title");
                        }
                    }
                    Commons.sendNotification(ctx, Constants.NTF_BORQS_APPLY,
                            Commons.createArrayNodeFromStrings(String.valueOf(type)),
                            Commons.createArrayNodeFromStrings(userId),
                            Commons.createArrayNodeFromStrings(String.valueOf(type), displayName, body),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(postId),
                            Commons.createArrayNodeFromStrings(String.valueOf(type), userId, displayName, body),
                            Commons.createArrayNodeFromStrings(body),
                            Commons.createArrayNodeFromStrings(body),
                            Commons.createArrayNodeFromStrings("APPLY"),
                            Commons.createArrayNodeFromStrings(postId)
                    );
                }
                //not borqs account
                String device = post.getString("device", "");

                if (CollectionUtils.isNotEmpty(emails)) {
                    String emailContent = Commons.composeShareContent(ctx, userId, type, body, true, device);
                    String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
                    String template = Constants.getBundleString(device, "platform.compose.share.title");
                    String title = SQLTemplate.merge(template, new Object[][]{
                            {"displayName", displayName}
                    });
                    for (String email : emails) {
                        String uid = GlobalLogics.getAccount().findUserIdByUserName(ctx, email);
                        if (StringUtils.equals(uid, "0") || StringUtils.isBlank(uid)) {
                            Record setting = GlobalLogics.getSetting().getByUsers(ctx, Constants.EMAIL_SHARE_TO, email);
                            String value = setting.getString(email, "0");
                            if (value.equals("0")) {
                                GlobalLogics.getEmail().sendEmail(ctx, title, email, email, emailContent, Constants.EMAIL_SHARE_TO, lang);
                            }
                        } else if (Commons.sendEmail) {
                            Record setting = GlobalLogics.getSetting().getByUsers(ctx, Constants.EMAIL_SHARE_TO, uid);
                            String value = setting.getString(uid, "0");
                            if (value.equals("0")) {
                                GlobalLogics.getEmail().sendEmail(ctx, title, email, email, emailContent, Constants.EMAIL_SHARE_TO, lang);
                            }
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(phones)) {
                    String smsContent = Commons.composeShareContent(ctx, userId, type, body, false, device);
                    for (String phone : phones) {
                        String uid = GlobalLogics.getAccount().findUserIdByUserName(ctx, phone);
                        if (StringUtils.equals(uid, "0") || StringUtils.isBlank(uid)) {
                            Commons.sendSms(ctx, phone, smsContent + "\\");
                            String template = Constants.getBundleString(device, "platform.compose.share.download");
                            String download = SQLTemplate.merge(template, new Object[][]{
                                    {"serverHost", Commons.SERVER_HOST}
                            });
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {

                            }
                            Commons.sendSms(ctx, phone, download + "\\");
                        } else if (Commons.sendSms) {
                            Commons.sendSms(ctx, phone, smsContent + "\\");
                        }
                    }
                }


                //subscribe
                ArrayList<String> reasons = new ArrayList<String>();
                reasons.add(String.valueOf(Constants.C_SUBSCRIBE_USER));
                reasons.add(String.valueOf(Constants.C_SUBSCRIBE_LOCAL_CIRCLE));
                ConversationLogic conversation = GlobalLogics.getConversation();
                RecordSet convUsers = conversation.getConversation(ctx, Constants.USER_OBJECT, userId, reasons, 0, 0, 1000);
                for (Record convUser : convUsers) {
                    String receiverId = convUser.getString("from_");
                    if (canReceiveSubscribeNotification(ctx, post.checkGetBoolean("privince"), post.getString("mentions"), receiverId)) {
                        Commons.sendNotification(ctx, Constants.NTF_SUBSCRIBE,
                                Commons.createArrayNodeFromStrings(appId),
                                Commons.createArrayNodeFromStrings(userId),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(receiverId)
                        );
                    }
                }
            }
        };
    }

    public Record postP(Context ctx, String userId, Record post, List<String> emails, List<String> phones, String appId, boolean sendEmail, boolean sendSms, boolean isTop) {
        final String METHOD = "postP";
        L.traceStartCall(ctx, METHOD, post, emails.toString(), phones.toString(), appId);
        ElapsedCounter ec = ctx.getElapsedCounter();

        Record r_post = new Record();
        ConversationLogic conversationLogic = GlobalLogics.getConversation();
        String postId = "";
        int type0 = Integer.valueOf(post.getString("type"));
        int flag = 0;
        try {
            if (type0 == Constants.PHOTO_POST) {
                //ec.record("Begin Merge PhotoPost");
                long dateDiff = 1000 * 60 * 10L;
                Record old_stream = topOneStreamByShare(ctx, Constants.PHOTO_POST, userId, post.getString("message"), post.getString("mentions"), (int) post.getInt("privince"), dateDiff).getFirstRecord();
                L.debug(ctx, "share photo to get old stream=" + old_stream);
                if (!old_stream.isEmpty()) {
                    postId = old_stream.getString("post_id");
                    RecordSet old_attachments = RecordSet.fromJson(old_stream.getString("attachments"));
                    RecordSet new_attachments = RecordSet.fromJson(post.getString("attachments"));
                    old_attachments.add(0, new_attachments.get(0));
                    for (Record attachRec : old_attachments) {
                        attachRec.putMissing("have_notify", true);
                    }
                    updatePostForAttachmentsAndUpdateTime(ctx, postId, old_attachments.toString(false, false), DateUtils.nowMillis());
                    r_post.putAll(old_stream);
                    r_post.put("post_id", Long.parseLong(postId));
                    r_post.put("post_id_s", postId);
                    r_post.put("attachments", old_attachments);
                    flag = 1;
                }
                //ec.record("End Merge PhotoPost");
            }
        } catch (Exception e) {
        }

        if (flag == 0) {
            //ec.record("Begin CreatePost");
            postId = createPost(ctx, userId, post);
            //ec.record("End CreatePost");

            //ec.record("Begin Conversation");
            conversationLogic.createConversationP(ctx, Constants.POST_OBJECT, postId, Constants.C_STREAM_POST, userId);
            if (post.getString("mentions").length() > 0)
                conversationLogic.createConversationP(ctx, Constants.POST_OBJECT, postId, Constants.C_STREAM_TO, post.getString("mentions"));
            if (post.getString("add_to").length() > 0)
                conversationLogic.createConversationP(ctx, Constants.POST_OBJECT, postId, Constants.C_STREAM_ADDTO, post.getString("add_to"));
            //ec.record("End Conversation");
            r_post.putAll(post);
            r_post.put("post_id", Long.parseLong(postId));
            r_post.put("post_id_s", postId);
        }
        //notification

//        Record r_post = getPosts(ctx, postId, "post_id,mentions,source,target,type,message,attachments").getFirstRecord();

        pool.execute(afterPost(ctx, userId, post, emails, phones, appId, sendEmail, sendSms, isTop, r_post));

        L.traceEndCall(ctx, METHOD);
//        return postId;
        return attachMock(ctx, r_post);
    }

    private boolean canReceiveSubscribeNotification(Context ctx, boolean secretly, String mentions, String receiverId) {
        if (secretly) {
            List<String> l = StringUtils2.splitList(mentions, ",", true);
            ArrayList<String> ul = new ArrayList<String>();
            ArrayList<Long> gl = new ArrayList<Long>();
            GroupLogic g = GlobalLogics.getGroup();
            for (String s : l) {
                if (g.isGroup(ctx, Long.parseLong(s)))
                    gl.add(Long.parseLong(s));
                else
                    ul.add(s);
            }

            if (ul.contains(receiverId)) {
                return true;
            }
            else if (gl.size() > 0) {
                Map<Long, Integer> map = g.getRolesWithGroups(ctx, ArrayUtils.toPrimitive(gl.toArray(new Long[gl.size()])), Long.parseLong(receiverId));
                for (Integer role : map.values()) {
                    if (role >= Constants.ROLE_MEMBER)
                        return true;
                }

                return false;
            }
            else {
                return false;
            }
        }
        else
            return true;
    }

    public void autoPost(Context ctx, String userId, int type, String msg, String attachments,
                         String appId, String packageName, String apkId,
                         String appData,
                         String mentions,
                         boolean secretly, String cols, String device, String location,
                         boolean can_comment, boolean can_like, boolean can_reshare, String add_to, String add_contact, boolean has_contact,int post_source) {
        final String METHOD = "autoPost";
        L.traceStartCall(ctx, METHOD, userId, type, msg, attachments, appId, packageName, apkId, appData, mentions, secretly, cols, device, location, can_comment, can_like, can_reshare, add_to, add_contact, has_contact);
        Record rec = new Record();
        rec.put("setFriend", false);
        rec.put("userId", userId);
        rec.put("type", type);
        rec.put("msg", msg);
        rec.put("attachments", attachments);
        rec.put("appId", appId);
        rec.put("packageName", packageName);
        rec.put("apkId", apkId);
        rec.put("app_data", appData);
        rec.put("mentions", mentions);
        rec.put("secretly", secretly);
        rec.put("cols", cols);
        rec.put("device", device);
        rec.put("location", location);
        rec.put("can_comment", can_comment);
        rec.put("can_like", can_like);
        rec.put("can_reshare", can_reshare);
        rec.put("add_to", add_to);

        rec.put("add_contact", add_contact);
        rec.put("has_contact", 1);

        //context cols
        rec.put("viewerId", ctx.getViewerId());
        rec.put("app", ctx.getAppId());
        rec.put("ua", ctx.getUa());
        rec.put("location", ctx.getLocation());
        rec.put("language", ctx.getLanguage());
        rec.put("post_source", post_source);
        //scene
        rec.put("scene",ctx.getSession("scene"));
        L.debug(ctx,"scene============="+(String)ctx.getSession("scene"));
        L.debug(ctx, "auto post record=" + rec);
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

    @Override
    public void sendPostBySetFriend(Context ctx, String userId, String friendIds, int reason, boolean can_comment, boolean can_like, boolean can_reshare,int post_source) {
        final String METHOD = "sendPostBySetFriend";
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
        rec.put("post_source", post_source);
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

    @Override
    public boolean sendPostBySetFriend0(Context ctx, String userId, String friendIds, int reason, boolean can_comment, boolean can_like, boolean can_reshare,int post_source) {
        final String METHOD = "sendPostBySetFriend0";
        L.traceStartCall(ctx, METHOD, userId, friendIds, reason, can_comment, can_like, can_reshare);
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        long dateDiff = 24 * 60 * 60 * 1000L;
        long minDate = DateUtils.nowMillis() - dateDiff;
        RecordSet old_recs = GlobalLogics.getStream().topOneStreamBySetFriend(ctx, Constants.FRIEND_SET_POST, userId, minDate);
        L.debug(ctx, "send post for set friend to get old post RecordSet=" + old_recs);
        if (old_recs.size() <= 0) {
            String message = "";
            if (reason == Constants.FRIEND_REASON_SOCIALCONTACT)
//                message = "基于系统智能推荐";
                message = "";
            //send stream
            Record post = Record.of("message", message, "app", Constants.APP_TYPE_BPC);
            RecordSet u = GlobalLogics.getAccount().getUserByIdBaseColumns(ctx, friendIds);
            post.put("attachments", u.toString(false, false));
            post.put("type", Constants.FRIEND_SET_POST);
            post.put("target", "");
            post.put("device", ua);
            post.put("app_data", "");
            post.put("mentions", "");
            post.put("privince", false);
            post.put("location", loc);
            post.put("can_comment", can_comment);
            post.put("can_like", can_like);
            post.put("can_reshare", can_reshare);
            post.put("add_to", "");
            post.put("post_source", post_source);
            post.put("scene",ctx.getSession("scene"));
            GlobalLogics.getStream().postP(ctx, userId, post, new ArrayList<String>(), new ArrayList<String>(), ctx.getAppId(), false, false, false);
        } else {
            Record r = old_recs.getFirstRecord();
            if (r.getString("attachments").length() > 2) {

                List<String> nowUserList = new ArrayList<String>();
                RecordSet oldAttachment = RecordSet.fromJson(r.getString("attachments"));
                //friendIds
                String olduserString = oldAttachment.joinColumnValues("user_id", ",");
                List<String> tempUserList = StringUtils2.splitList(friendIds, ",", true);
                List<String> olduserList = StringUtils2.splitList(olduserString, ",", true);

                for (String uid : tempUserList) {
                    if (!olduserList.contains(uid)) {
                        nowUserList.add(uid);
                    }
                }

                RecordSet u = GlobalLogics.getAccount().getUserByIdBaseColumns(ctx, StringUtils.join(nowUserList, ","));
                for (Record u0 : u) {
                    oldAttachment.add(u0);
                }

                for (Record rd : oldAttachment) {
                    String uid = rd.getString("user_id");
                    long created_time = GlobalLogics.getFriendship().getMyFriends(ctx, userId, uid).getInt("created_time");
                    if ((created_time + dateDiff) < DateUtils.nowMillis())
                        oldAttachment.remove(rd);
                }

                GlobalLogics.getStream().updatePostFor(ctx, r.getString("post_id"), oldAttachment.toString(false, false), DateUtils.nowMillis(), DateUtils.nowMillis());
            }
        }

        L.traceEndCall(ctx, METHOD);
        return true;
    }

    public Record postP(Context ctx, String userId, int type, String msg, String attachments,
                        String appId, String packageName, String apkId,
                        String appData,
                        String mentions,
                        boolean secretly, String cols, String device, String location,
                        String url, String linkImagAddr,
                        boolean can_comment, boolean can_like, boolean can_reshare,
                        String add_to,int post_source, long scene) {
        return postP(ctx, userId, type, msg, attachments, appId, packageName, apkId, appData, mentions,
                secretly, cols, device, location, url, linkImagAddr, can_comment, can_like, can_reshare, add_to, false, false, false,post_source, scene);
    }

    public Record postP(Context ctx, String userId, int type, String msg, String attachments,
                        String appId, String packageName, String apkId,
                        String appData,
                        String mentions,
                        boolean secretly, String cols, String device, String location,
                        String url, String linkImagAddr,
                        boolean can_comment, boolean can_like, boolean can_reshare,
                        String add_to, boolean sendEmail, boolean sendSms, boolean isTop,int post_source, long scene) {
        final String METHOD = "postP";
        L.traceStartCall(ctx, METHOD, userId, type, msg, attachments, appId, packageName, apkId, appData, mentions, secretly, cols, device, location, url, linkImagAddr, can_comment, can_like, can_reshare, add_to);
        ElapsedCounter ec = ctx.getElapsedCounter();

        //ec.record("Begin Contact");
        List<String> emails = Commons.getEmails(mentions);
        List<String> phones = Commons.getPhones(mentions);
        mentions = Commons.getOldMentions(ctx, userId, mentions);
        QiupuLogic qp = QiupuLogics.getQiubpu();
        List<String> add_contact = new ArrayList<String>();
        add_contact.addAll(emails);
        add_contact.addAll(phones);

        for (int k = add_contact.size() - 1; k >= 0; k--) {
            String virtualFriendId = GlobalLogics.getFriendship().getUserFriendHasVirtualFriendId(ctx, userId, add_contact.get(k));
            if (!StringUtils.equals(virtualFriendId, "0")) {
                add_contact.remove(k);
            }
        }
        String add_contact_s = StringUtils.join(add_contact, ",");

        boolean has_contact = false;
        if (add_contact_s.length() > 0)
            has_contact = true;
        //ec.record("End Contact");

        if (msg.toString().length() > 4096)
            msg = msg.substring(0, 4000);
        Record post = Record.of("message", msg, "app", appId);

        if (!apkId.equals("") || !packageName.equals(""))
            appId = String.valueOf(Constants.APP_TYPE_QIUPU);

        post.put("attachments", (attachments.length() <= 2) ? "[]" : "[" + attachments + "]");
        post.put("type", type);
        post.put("target", "");

        post.put("device", device);
        post.put("app_data", appData);
        post.put("mentions", Commons.parseUserIds(ctx, userId, mentions));
        post.put("privince", secretly);
        post.put("location", location);
        post.put("can_comment", can_comment);
        post.put("can_like", can_like);
        post.put("can_reshare", can_reshare);
        post.put("add_to", add_to);
        post.put("add_contact", add_contact_s);
        post.put("has_contact", has_contact);
        post.put("post_source", post_source);

        if(scene ==0){
            scene = Long.parseLong((String)ctx.getSession("scene"));
        }
        post.put("scene", scene);

        if (appId.equals(String.valueOf(Constants.APP_TYPE_QIUPU))) {
            //ec.record("Begin Apk");
            if (!apkId.equals("")) {
                String[] ss = StringUtils.split(StringUtils.trimToEmpty(apkId), '-');
                String package_ = ss[0].trim();
                if (qp.existPackage(ctx, package_)) {
                    RecordSet mcs = Commons.thisTrandsGetApkInfo(ctx, userId, apkId, cols, 1000);
                    if (mcs.size() > 0) {
                        post.put("attachments", mcs.toString(false,false));
                        post.put("target", apkId);
                    } else {
                        RecordSet s = Commons.thisTrandsGetSingleApkInfo(ctx, userId, ss[0], cols, 1000);
                        post.put("attachments", s.toString(false,false));
                        post.put("target", s.getFirstRecord().getString("apk_id"));
                    }
                    if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                        post.put("type", String.valueOf(type));
                    } else {
                        post.put("type", String.valueOf(Constants.APK_POST));
                    }
                } else {
                    String href = "http://market.android.com/details?id=" + package_;
                    RecordSet r = new RecordSet();
                    r.add(Record.of("href", href));
                    post.put("attachments", r.toString(false,false));
                    post.put("type", String.valueOf(Constants.APK_LINK_POST));
                    post.put("target", apkId);
                }
            }
            if (!packageName.equals("")) {
                if (qp.existPackage(ctx, packageName)) {
                    RecordSet s = Commons.thisTrandsGetSingleApkInfo(ctx, userId, packageName, cols, 1000);
                    post.put("target", s.getFirstRecord().getString("apk_id"));
                    post.put("attachments", s);
                    if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                        post.put("type", String.valueOf(type));
                    } else {
                        post.put("type", String.valueOf(Constants.APK_POST));
                    }
                } else {
                    post.put("target", packageName);
                    String href = "http://market.android.com/details?id=" + packageName;
                    RecordSet r = new RecordSet();
                    r.add(Record.of("href", href));
                    post.put("attachments", r);
                    post.put("type", String.valueOf(Constants.APK_LINK_POST));
                }
            }
            //ec.record("End Apk");
        }

        /*
        //use appid now,this is temp method for qiupu.attachments must be json after update .
        if (!appId.equals(toStr(Constants.APP_TYPE_QIUPU))) {
            if (type != Constants.LINK_POST) {
                post.put("attachments", (attachments.length() <= 2) ? "[]" : "[" + attachments + "]");
                post.put("type", String.valueOf(type));
            }
        } else {
            String a = "";
            //1,if exist in server
            Transceiver tranq = getTransceiver(QiupuInterface.class);
            QiupuInterface qp = getProxy(QiupuInterface.class, tranq);
            if (apkId.equals("") && packageName.equals("")) {
                if (type != Constants.LINK_POST && type != Constants.PHOTO_POST) {
                    post.put("attachments", "[]");
                    post.put("type", String.valueOf(Constants.TEXT_POST));
                    post.put("target", "");
                }
            } else {
                if (!apkId.equals("")) {
                    String[] ss = StringUtils.split(StringUtils.trimToEmpty(apkId), '-');
                    String package_ = ss[0].trim();

                    if (qp.existPackage(package_)) {
                        //post.put("attachments", RecordSet.fromByteBuffer(qp.getApps(toStr(apkId), cols)));
                        //old
                        RecordSet mcs = thisTrandsGetApkInfo(userId, toStr(apkId), cols, 1000);
                        if (mcs.size() > 0) {
                            post.put("attachments", mcs);
                            post.put("target", apkId);
                        } else {
                            RecordSet s = thisTrandsGetSingleApkInfo(userId, toStr(ss[0]), cols, 1000);
                            post.put("attachments", s);
                            post.put("target", s.getFirstRecord().getString("apk_id"));
                        }
                        if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                            post.put("type", String.valueOf(type));
                        } else {
                            post.put("type", String.valueOf(Constants.APK_POST));
                        }
                    } else {
                        String href = "http://market.android.com/details?id=" + package_;
                        RecordSet r = new RecordSet();
                        r.add(Record.of("href", href));
                        post.put("attachments", r);
                        post.put("type", String.valueOf(Constants.APK_LINK_POST));
                        post.put("target", apkId);
                    }
                } else {
                    if (!packageName.equals("")) {
                        if (qp.existPackage(packageName)) {
                            RecordSet s = thisTrandsGetSingleApkInfo(userId, toStr(packageName), cols, 1000);
                            post.put("target", s.getFirstRecord().getString("apk_id"));
                            post.put("attachments", s);
                            if (type == Constants.APK_COMMENT_POST || type == Constants.APK_LIKE_POST) {
                                post.put("type", String.valueOf(type));
                            } else {
                                post.put("type", String.valueOf(Constants.APK_POST));
                            }
                        } else {
                            post.put("target", packageName);
                            String href = "http://market.android.com/details?id=" + packageName;
                            RecordSet r = new RecordSet();
                            r.add(Record.of("href", href));
                            post.put("attachments", r);
                            post.put("type", String.valueOf(Constants.APK_LINK_POST));
                        }
                    }
                }
            }
        }
        post.put("device", device);
        post.put("app_data", appData);
        post.put("mentions", parseUserIds(userId, mentions));
        post.put("privacy", secretly);
        post.put("location", location);
        post.put("can_comment", can_comment);
        post.put("can_like", can_like);
        post.put("can_reshare", can_reshare);
        post.put("add_to", add_to);
        */
        String tempAttach = post.getString("attachments", "[]");
        if (StringUtils.isBlank(tempAttach) || StringUtils.equals(tempAttach, "[]")) {
            post.put("type", type | Constants.TEXT_POST);
        }
        Record mock = postP(ctx, userId, post, emails, phones, appId, sendEmail, sendSms, isTop);
        L.traceEndCall(ctx, METHOD);
        return mock;
    }


    public RecordSet getPostsP(Context ctx, String postIds, String cols) {
        cols = Commons.parsePostColumns(cols);
        RecordSet recs = getPosts(ctx, postIds, cols);
        return recs;
    }

    public int updatePhotoOld(Context ctx) {
        String sql = "SELECT * FROM stream WHERE INSTR(mentions,'10000000055')>0 AND TYPE=2  ORDER BY created_time DESC LIMIT 100";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        for (Record rec : recs){
             RecordSet rrr = RecordSet.fromJson(rec.getString("attachments"));
             for (Record rr : rrr){
                 Record rc = new Record();
                 rc.put("photo_id", rr.getString("photo_id"));
                 rc.put("album_id", rr.getString("album_id"));
                 rc.put("user_id", rec.getString("source"));
                 String m1 = rr.getString("photo_img_middle").substring(rr.getString("photo_img_middle").lastIndexOf("/") + 1, rr.getString("photo_img_middle").length());
                 rc.put("img_middle", m1);

                 rc.put("img_original", m1);
                 String m3 = rr.getString("photo_img_big").substring(rr.getString("photo_img_big").lastIndexOf("/") + 1, rr.getString("photo_img_big").length());
                 rc.put("img_big", m3);
                 String m4 = rr.getString("photo_img_small").substring(rr.getString("photo_img_small").lastIndexOf("/") + 1, rr.getString("photo_img_small").length());
                 rc.put("img_small", m4);
                 rc.put("caption", "");
                 rc.put("created_time", rec.getInt("created_time"));
                 rc.put("location", "");
                 rc.put("tag", "");
                 rc.put("from_user", rec.getString("source"));
                 rc.put("original_pid", "0");
                 rc.put("longitude", "");
                 rc.put("latitude", "");
                 rc.put("stream_id", rec.getString("post_id"));

                 GlobalLogics.getPhoto().saveUploadPhoto(ctx,  rc);
             }
        }

        return recs.size();
    }
}
