package com.borqs.server.impl.stream.tools;


import com.borqs.server.impl.account.AccountImpl;
import com.borqs.server.impl.friend.FriendImpl;
import com.borqs.server.impl.stream.StreamDb;
import com.borqs.server.impl.stream.timeline.RedisOutboxTimeline;
import com.borqs.server.impl.stream.timeline.RedisWallTimeline;
import com.borqs.server.platform.cache.redis.Redis;
import com.borqs.server.platform.cache.redis.SingleRedis;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.feature.stream.timeline.TimelineEntry;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.sql.SimpleConnectionFactory;
import com.borqs.server.platform.sql.SingleTable;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.VfsHelper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.InputStream;
import java.util.Properties;

public class TimelineBuilder {

    //redis server and port
    //public static final String SERVER = "localhost:6371";
    //public static final String SERVER_WALLLINE = "192.168.5.22:6381";
    //public static final String SERVER_USERLINE = "192.168.5.22:6380";
    public static Properties p = new Properties();
    public static  String SERVER_WALLLINE = "192.168.5.22:6381";
    public static  String SERVER_USERLINE = "192.168.5.22:6380";

    //db url
    //public static final String DB = "jdbc:mysql://localhost:3306/account2?user=root&password=1234&allowMultiQueries=true";
    public static final String DB = "jdbc:mysql://192.168.5.22:3306/test_account3?user=root&password=111111&allowMultiQueries=true";

    private static FriendImpl friend;
    private static AccountImpl account;

    private RedisOutboxTimeline outboxTimeline = new RedisOutboxTimeline();
    private RedisWallTimeline wallTimeline = new RedisWallTimeline();

    // db
    private final StreamDb db = new StreamDb();

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public void setPostTable(Table postTable) {
        db.setPostTable(postTable);
    }

    public void setWallTimeLineRedis(Redis redis) {
        wallTimeline.setRedis(redis);
    }

    public void setUserTimeline(Redis redis) {
        outboxTimeline.setRedis(redis);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(":)---------------");
        if (ArrayUtils.isEmpty(args)) {
            System.out.println("args pls");
            return;
        }

        //getConfig(args[0]);

        if (args.length == 0) {
            printUsage();
            return;
        }

        SingleRedis redis_wallLine = new SingleRedis();
        redis_wallLine.setServer(SERVER_WALLLINE);
        redis_wallLine.init();

        SingleRedis redis_userLine = new SingleRedis();
        redis_userLine.setServer(SERVER_USERLINE);
        redis_userLine.init();

        String table = args[1];

        SqlExecutor sqlExecutor = new SqlExecutor();
        sqlExecutor.setConnectionFactory(new SimpleConnectionFactory());

        SingleTable userTable = new SingleTable();
        userTable.setDb(DB);
        userTable.setTable("user");

        SingleTable userTableProperty = new SingleTable();
        userTableProperty.setDb(DB);
        userTableProperty.setTable("user_property");

        SingleTable circleTable = new SingleTable();
        circleTable.setDb(DB);
        circleTable.setTable("circle");

        SingleTable followerTable = new SingleTable();
        followerTable.setDb(DB);
        followerTable.setTable("follower");

        SingleTable followerProperty = new SingleTable();
        followerProperty.setDb(DB);
        followerProperty.setTable("follower");

        SingleTable remarkTable = new SingleTable();
        remarkTable.setDb(DB);
        remarkTable.setTable("remark");

        SingleTable friendTable = new SingleTable();
        friendTable.setDb(DB);
        friendTable.setTable("friend");

        account = new AccountImpl();
        account.setUserTable(userTable);
        account.setPropertyTable(userTableProperty);
        account.setSqlExecutor(sqlExecutor);

        friend = new FriendImpl();
        friend.setCircleTable(circleTable);
        friend.setFollowerTable(followerTable);
        friend.setRemarkTable(remarkTable);
        friend.setFriendTable(friendTable);
        friend.setSqlExecutor(sqlExecutor);
        friend.setAccount(account);


        TimelineBuilder timelineBuilder = new TimelineBuilder();
        timelineBuilder.setUserTimeline(redis_userLine);
        timelineBuilder.setWallTimeLineRedis(redis_wallLine);
        timelineBuilder.setSqlExecutor(sqlExecutor);

        SingleTable singleTable = new SingleTable();
        singleTable.setDb(DB);
        singleTable.setTable(table);
        timelineBuilder.setPostTable(singleTable);

        timelineBuilder.buildLine(table);

    }

    private void buildLine(String table) {
        Context ctx = Context.create();

        Posts posts = db.getPostForTimeLine(ctx, table);
        Posts postError = new Posts();
        Posts postSuccess = new Posts();

        for (Post post : posts) {
            try {
                distributeTimeline(ctx, post);
                postSuccess.add(post);
            } catch (Exception e) {
                postError.add(post);
            }
        }

        if (postError.size() > 0) {
            System.out.println("----------------------------errorList-----------------------------------");
            System.out.println(postError.size());
            System.out.println("----------------------------errorList-----------------------------------");
        }
        if (postSuccess.size() > 0) {
            System.out.println(":)----------------------------successList-----------------------------------:)");
            System.out.println(postSuccess.size());
            System.out.println(":)----------------------------successList-----------------------------------:)");
        }
    }

    private static void printUsage() {
        System.out.print("Please inter the argument of the table name");
    }

    private void distributeTimeline(Context ctx, Post post) {
        
        outboxTimeline.removeTimeline(ctx,PeopleId.user(post.getSourceId()));
        // user timeline
        outboxTimeline.add(ctx, PeopleId.user(post.getSourceId()), TimelineEntry.create(post));

        // wall timeline
        wallTimeline.removeTimeline(ctx,PeopleId.user(post.getSourceId()));
        wallTimeline.add(ctx, PeopleId.user(post.getSourceId()), TimelineEntry.create(post));

        PeopleIds to = post.getToAndAddto();
        for (PeopleId friendId : to) {
            wallTimeline.removeTimeline(ctx,friendId);
            wallTimeline.add(ctx, friendId, TimelineEntry.create(post));
        }

    }

    public static void getConfig(String path) {

        try {
            InputStream in = IOUtils.toInputStream(VfsHelper.loadText(path), Charsets.DEFAULT);
            p.load(in);
            SERVER_WALLLINE = (String)p.get("redis.wallTimeline");
            SERVER_USERLINE = (String)p.get("redis.userTimeline");
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
