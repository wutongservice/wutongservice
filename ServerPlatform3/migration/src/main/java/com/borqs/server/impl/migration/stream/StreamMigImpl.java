package com.borqs.server.impl.migration.stream;


import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.impl.stream.StreamDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.*;

public class StreamMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(StreamMigImpl.class);

    private final StreamMigDb db_migration = new StreamMigDb();
    private final StreamDb dbNewPost = new StreamDb();

    private AccountMigImpl account;

    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        dbNewPost.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
    }

    public void setNewPostTable(Table newPostTable) {
        dbNewPost.setPostTable(newPostTable);
    }

    public void setOldPostTable(Table oldPostTable) {
        db_migration.setStreamTable(oldPostTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("stream.mig")) {
            streamMigration(Context.create());
        }
    }

    public void streamMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, StreamMigImpl.class, "friendMigration", ctx);

        Posts posts = null;
        List<Long> postIds = new ArrayList<Long>();
        try {

            db_migration.setUserIdMap(getAllUserIdMap(ctx));

            posts = db_migration.getPost(ctx);

            for (Post post : posts) {
                try {
                    if (post != null) {
                        dbNewPost.createStream(ctx, post);
                        postIds.add(post.getPostId());
                    }
                } catch (RuntimeException e) {
                    LC.endCall();
                    throw e;
                }
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall();
            throw e;
        }

        //checkPost
        /* Posts errorPost = checkPost(ctx, postIds, posts);
if (errorPost.size() > 0)
    L.error(ctx, "Post check error", errorPost);*/
    }

    /*private Posts checkPost(Context ctx, List<Long> postIds, Posts postsOld) {
        long[] postId = CollectionsHelper.toLongArray(postIds);
        Posts posts = dbNewPost.getPosts(ctx, postId);
        Posts errorPost = new Posts();
        for (Post n : posts) {
            Post o = postsOld.getPost(n.getPostId());
            if (!comparePost(o, n)) {
                errorPost.add(n);
            }
        }
        return errorPost;
    }*/

    /*private boolean comparePost(Post o, Post n) {
        if (o.getCanComment() != n.getCanComment())
            return false;
        if(o.getCanLike() != n.getCanComment())
            return false;
        if(o.getCanQuote()!= n.getCanQuote())
            return false;
        if(o.getPrivate()!= n.getPrivate())

        return true;
    }*/
    public Map<Long, String> getAllPostIdMap(Context ctx) {
        long[] list = db_migration.getAllPostIds(ctx);
        Map<Long, String> map = new HashMap<Long, String>();
        for (Long l : list) {
            map.put(l, String.valueOf(l));
        }
        return map;
    }

    private Map<Long, String> getAllUserIdMap(Context ctx) {
        return account.getAllUserIdMap(ctx);
    }

}
