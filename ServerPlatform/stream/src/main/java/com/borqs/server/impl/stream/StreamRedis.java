package com.borqs.server.impl.stream;


import com.borqs.server.base.redis.Redis;
import com.borqs.server.feature.stream.Post;

import java.util.ArrayList;
import java.util.List;

public class StreamRedis {

    Redis rd = new Redis();
    StreamCache streamCache = new StreamCache();
    public void init() {
        rd.init();
    }

    public void destroy() {
        rd.destroy();
    }

    public String timelineKey() {
        return "timeline";
    }
    public String myOutboxKey(long uid) {
        return "outbox" + String.valueOf(uid);
    }
    public String myInboxKey(long uid) {
        return "inbox" + String.valueOf(uid);
    }

    public boolean createPostRedis(Post post) {
        rd.writeToList(timelineKey(), String.valueOf(post.getPostId()));
        rd.writeToList(myOutboxKey(post.getFrom()), String.valueOf(post.getPostId()));
        if (post.getTo().length > 0) {
            for (long t : post.getTo()) {
                rd.writeToList(myInboxKey(t), String.valueOf(post.getPostId()));
            }
        }
        return true;
    }

    public List<String> getMyOutbox(long uid) {
        List<String> ls = new ArrayList<String>();
        ls = rd.readInList(myOutboxKey(uid));
        return ls;
    }

    public List<String> getMyInbox(long uid) {
        List<String> ls = new ArrayList<String>();
        ls = rd.readInList(myInboxKey(uid));
        return ls;
    }

    public List<String> getTimeline() {
        List<String> ls = new ArrayList<String>();
        ls = rd.readInList(timelineKey());
        return ls;
    }

    public boolean deletePost(long[] postIds) {
        boolean b = true;
        for (long postId : postIds) {
            long[] t = new long[0];
            t[0] = postId;
            Post p = streamCache.getPosts(t).get(0);
            if (p != null) {
                rd.deleteFromList(myOutboxKey(p.getFrom()), String.valueOf(postId));
            }
            rd.deleteFromList(timelineKey(), String.valueOf(postId));
        }
        return b;
    }
}

