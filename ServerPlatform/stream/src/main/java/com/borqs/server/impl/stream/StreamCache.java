package com.borqs.server.impl.stream;


import com.borqs.server.base.memcache.XMemcached;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.feature.stream.Post;
import org.codehaus.jackson.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.feature.stream.Post.JsonToPost;
import static com.borqs.server.feature.stream.Post.postToJson;

public class StreamCache {

    XMemcached mc = new XMemcached();

    public void init() {
        mc.init();
    }

    public void destroy() {
        mc.destroy();
    }

    public String postKey(String key) {
        return "p" + key;
    }

    public List<Post> createPostCache(List<Post> post) {
        for (Post p : post) {
            mc.deleteCache(postKey(String.valueOf(p.getPostId())));
            mc.writeCache(postKey(String.valueOf(p.getPostId())), postToJson(p));
        }
        return post;
    }

    public Post getPostCache(long postId) {
        String postIdKey = postKey(String.valueOf(postId));
        String cache = mc.readCache(postIdKey);
        Post post = null;
        if (cache.length() > 0) {
            post = JsonToPost(JsonUtils.fromJson(cache, JsonNode.class));
        }
        return post;
    }

    public boolean updatePost(Post post) {
        boolean b = true;
        mc.deleteCache(postKey(String.valueOf(post.getPostId())));
        mc.writeCache(postKey(String.valueOf(post.getPostId())), postToJson(post));
        return b;
    }

    public List<Post> getPosts(long[] postIds) {
        List<Post> postList = new ArrayList<Post>();
        for (long postId : postIds) {
            Post p = getPostCache(postId);
            if (p != null) {
                postList.add(p);
            }
        }
        return postList;
    }

    public boolean destroyPost(long[] postIds) {
        boolean b = true;
        for (long postId : postIds) {
            mc.deleteCache(postKey(String.valueOf(postId)));
        }
        return b;
    }
}

