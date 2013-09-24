package com.borqs.server.impl.stream;


import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.feature.stream.Post;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class StreamImpl extends ConfigurableBase {

    private String redisServerAddr;

    StreamDB streamDB = new StreamDB();
    StreamRedis streamRD = new StreamRedis();
    StreamCache streamCache = new StreamCache();
    Jedis jedis = new Jedis(redisServerAddr);


    public void init() throws SQLException {
        Configuration conf = getConfig();
        redisServerAddr = conf.getString("platform.redisServerpAddr", "localhost");
        streamDB.init();
        streamCache.init();
    }

    public void destroy() {
        streamDB.destroy();
        streamCache.destroy();
    }

    public Post createPost(Context ctx,Post post) throws SQLException {
        List<Post> Posts = new ArrayList<Post>();
        Posts.add(post);
        streamDB.createPost(ctx,post);
        streamCache.createPostCache(Posts);
        streamRD.createPostRedis(post);
        return post;
    }

    public Post getPost(Context ctx,long postId) throws IOException, SQLException {
        Post post = new Post();
        try {
            post = streamCache.getPostCache(postId);
        } catch (Exception e) {
        }
        if (post == null) {
            post = streamDB.getPost(ctx,postId);
            List<Post> posts = new ArrayList<Post>();
            posts.add(post);
            streamCache.createPostCache(posts);
        }
        return post;
    }

    public boolean updatePost(Context ctx,Post post) {
        boolean b = true;
        try {
            streamDB.updatePost(ctx,post);
            streamCache.updatePost(post);
            long[] t = new long[0];
            t[0] = post.getPostId();
            streamRD.deletePost(t) ;
            streamRD.createPostRedis(post);
        } catch (Exception e) {
            b = false;
        }
        return b;
    }

    public List<Post> getPosts(Context ctx,long[] postIds) throws SQLException, IOException {
        List<Post> hasPostFromCacheList = streamCache.getPosts(postIds);
        List<Post> outList = new ArrayList<Post>();

        List<Long> missPostId = new ArrayList<Long>();
        List<Long> hasPostId = new ArrayList<Long>();

        for (Post post0 : hasPostFromCacheList) {
            if (post0 != null && post0.getPostId() > 0) {
                outList.add(post0);
                hasPostId.add(post0.getPostId());
            }
        }

        for (long postId : postIds) {
            if (!hasPostId.contains(postId)) {
                missPostId.add(postId);
            }
        }

        long[] ll = new long[missPostId.size()];
        for (int i = 0; i < missPostId.size(); i++) {
            ll[i] = missPostId.get(i);
        }

        List<Post> hasPostFromDbList = streamDB.getPosts(ctx,ll);
        streamCache.createPostCache(hasPostFromDbList);

        hasPostFromCacheList.addAll(hasPostFromDbList);   // now hasPostFromCacheList has all post

        for (long pId : postIds) {
            for (Post post2 : hasPostFromCacheList) {
                if (pId == post2.getPostId()) {
                    outList.add(post2);
                    break;
                }
            }
        }

        return outList;
    }

    public boolean destroyPost(Context ctx,long[] postIds) throws SQLException {
        boolean b = streamDB.destroyPost(ctx,postIds);
        streamCache.destroyPost(postIds);
        streamRD.deletePost(postIds);
        return b;
    }

    public List<Post> getUserTimeline(Context ctx,long userId, long since, long max, int type, int appId, int page, int count) throws SQLException, IOException {
        List<String> postIdsFromOutbox = streamRD.getMyOutbox(userId);
        long[] outboxPostIds = new long[postIdsFromOutbox.size()];
        for (int i=0;i<postIdsFromOutbox.size();i++){
            outboxPostIds[i]=Long.parseLong(postIdsFromOutbox.get(i));
        }
        List<Post> posts = getPosts(ctx, outboxPostIds);
        if (ctx.getViewerId() != 0) {
            if (ctx.getViewerId() == userId) {
                List<String> postIdsFromInbox = streamRD.getMyInbox(userId);
                long[] inboxPostIds = new long[postIdsFromInbox.size()];
                for (int i = 0; i < postIdsFromInbox.size(); i++) {
                    inboxPostIds[i] = Long.parseLong(postIdsFromInbox.get(i));
                }
                posts.addAll(getPosts(ctx, inboxPostIds));
            }
            else
            {
                for (int j = posts.size() - 1; j >= 0; j--) {
                    List<String> longList = StringUtils2.splitList(StringUtils2.join(",",posts.get(j).getTo()),",",true);
                    if (posts.get(j).isPrivacy() && !longList.contains(ctx.getViewerId()))
                        posts.remove(j);
                }
            }
        }

        for (int j = posts.size() - 1; j >= 0; j--) {
            if (posts.get(j).getCreatedTime() < since)
                posts.remove(j);
            if (posts.get(j).getCreatedTime() > max)
                posts.remove(j);
            if (posts.get(j).getApp() != appId)
                posts.remove(j);
            if ((posts.get(j).getType() & type) == 0)
                posts.remove(j);
        }

        posts =  filterDoublePosts(posts) ;

        ComPost comparator = new ComPost();
        Collections.sort(posts, comparator);

        posts = streamSplitPage(posts, page, count);

        return posts;
    }

    public List<Post> getPublicTimeline(Context ctx, String friendIds, long since, long max, int type, int appId, int page, int count) throws SQLException, IOException {
        List<String> postIdsPublicTimeline = streamRD.getTimeline();
        long[] outboxPostIds = new long[postIdsPublicTimeline.size()];
        for (int i = 0; i < postIdsPublicTimeline.size(); i++) {
            outboxPostIds[i] = Long.parseLong(postIdsPublicTimeline.get(i));
        }
        List<Post> posts = getPosts(ctx, outboxPostIds);
        if (ctx.getViewerId() != 0) {
            List<String> ff = StringUtils2.splitList(friendIds, ",", true);
            for (int j = posts.size() - 1; j >= 0; j--) {
                if (ff.contains(String.valueOf(posts.get(j).getFrom())))
                    posts.remove(j);
            }
        }

        for (int j = posts.size() - 1; j >= 0; j--) {
            if (posts.get(j).getCreatedTime() < since)
                posts.remove(j);
            if (posts.get(j).getCreatedTime() > max)
                posts.remove(j);
            if (posts.get(j).getApp() != appId)
                posts.remove(j);
            if ((posts.get(j).getType() & type) == 0)
                posts.remove(j);
        }

        posts = filterDoublePosts(posts);

        ComPost comparator = new ComPost();
        Collections.sort(posts, comparator);

        posts = streamSplitPage(posts, page, count);

        return posts;
    }

    public List<Post> filterDoublePosts(List<Post> posts) {
       List<Post> out = new ArrayList<Post>();
       for (int i=0;i<posts.size()-1;i++)
       {
           for (Post post : out){
               if (post.getPostId()!=posts.get(i).getPostId())
                   out.add(posts.get(i));
           }
       }
        return out;
    }

    public List<Post> streamSplitPage(List<Post> streams, int page, int count) {
        List<Post> outPosts = new ArrayList<Post>();
        int rowsCont = streams.size();
        int pageCount = 0;
        if (streams.size() % count == 0) {
            pageCount = streams.size() / count;
        } else {
            pageCount = (streams.size() / count) + 1;
        }
        if (page > pageCount) {
            page = pageCount;
        }
        if (page == 0) {
            if (count <= rowsCont) {
                for (int i = 0; i < count; i++) {
                    outPosts.add(streams.get(i));
                }
            } else {
                outPosts = streams;
            }
        } else {
            for (int i = ((page - 1) * count); i < streams.size() && i < ((page) * count) && page > 0; i++) {
                outPosts.add(streams.get(i));
            }
        }
        return outPosts;
    }
}

class ComPost implements Comparator {
    public int compare(Object arg0, Object arg1) {
        Post c0 = (Post) arg0;
        Post c1 = (Post) arg1;
        int flag = String.valueOf(c0.getCreatedTime()).compareTo(String.valueOf(c1.getCreatedTime()));
        return flag;
    }
}

