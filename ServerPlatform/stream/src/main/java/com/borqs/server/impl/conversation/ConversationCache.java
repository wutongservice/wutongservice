package com.borqs.server.impl.conversation;


import com.borqs.server.base.memcache.XMemcached;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.feature.conversation.Conversation;
import net.sf.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.feature.conversation.Conversation.JsonArrayToConversationList;
import static com.borqs.server.feature.conversation.Conversation.conversationToJsonArray;

public class ConversationCache {

    XMemcached mc = new XMemcached();

    public void init() {
        mc.init();
    }

    public void destroy() {
        mc.destroy();
    }

    public String conversationKey(String key) {
        return "c" + key;
    }

    public List<Conversation> createConversationCache(List<Conversation> conversation) {
        mc.deleteCache(conversationKey(String.valueOf(conversation.get(0).getPostId())));
        mc.writeCache(conversationKey(String.valueOf(conversation.get(0).getPostId())), conversationToJsonArray(conversation));
        return conversation;
    }

    public List<Conversation> getConversationListCache(long postId) {
        String conversationIdKey = conversationKey(String.valueOf(postId));
        String cache = mc.readCache(conversationIdKey);
        List<Conversation> lc = new ArrayList<Conversation>();
        if (cache.length() > 0) {
            JSONArray jsonArray = JsonUtils.fromJson(cache, JSONArray.class);
            lc = JsonArrayToConversationList(jsonArray);
        }
        return lc;
    }

    public boolean updateConversation(List<Conversation> conversation) {
        boolean b = true;
        mc.deleteCache(conversationKey(String.valueOf(conversation.get(0).getPostId())));
        mc.writeCache(conversationKey(String.valueOf(conversation.get(0).getPostId())), conversationToJsonArray(conversation));
        return b;
    }

    public List<Conversation> getPosts(long[] postIds) {
        List<Conversation> lcOut = new ArrayList<Conversation>();
        for (long postId : postIds) {
            List<Conversation> lc = getConversationListCache(postId);
            if (lc.size()>0) {
                lcOut.addAll(lc);
            }
        }
        return lcOut;
    }

    public boolean destroyConversation(long postId, long userId, int reason) {
        boolean b = true;
        List<Conversation> conversations = getConversationListCache(postId);
        for (int i = conversations.size() - 1; i >= 0; i--) {
            if (conversations.get(i).getReason() == reason && conversations.get(i).getFrom() == userId && conversations.get(i).getPostId() == postId)
                conversations.remove(i);
        }
        mc.deleteCache(conversationKey(String.valueOf(postId)));
        if (conversations.size() > 0) {
            mc.writeCache(conversationKey(String.valueOf(postId)), conversationToJsonArray(conversations));
        }
        return b;
    }
}

