package com.borqs.server.impl.conversation;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.feature.conversation.Conversation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class ConversationImpl {

    ConversationDB conversationDB = new ConversationDB();
    ConversationCache conversationCache = new ConversationCache();

    public void init() throws SQLException {
        conversationDB.init();
        conversationCache.init();
    }

    public void destroy() {
        conversationDB.destroy();
        conversationCache.destroy();
    }

    public List<Conversation> createConversation(Context ctx,long postId, long userId, int reason) throws SQLException {
        Conversation conversation = conversationDB.createConversation(ctx,postId, userId, reason);
        List<Conversation> lc = conversationCache.getConversationListCache(postId);

        long[] r = new long[0];
        r[0] = reason;
        long[] p = new long[0];
        p[0] = postId;
        List<Conversation> lcdb = conversationDB.getConversationByPostIds(ctx,p, r, 0, 1000);
        conversationCache.createConversationCache(lcdb);

        return lcdb;
    }

    public List<Conversation> getConversationByPostId(Context ctx,long postId, long[] reason, int page, int count) throws IOException, SQLException {
        List<Conversation> conversation = new ArrayList<Conversation>();
        try {
            conversation = conversationCache.getConversationListCache(postId);
        } catch (Exception e) {
        }
        if (conversation.size() <= 0) {
            long[] p = new long[0];
            p[0] = postId;
            conversation = conversationDB.getConversationByPostIds(ctx,p, reason, page, count);
        } else {
            if (reason.length > 0) {
                List<String> lReason = StringUtils2.splitList(StringUtils2.join(",", reason), ",", true);
                for (int i = conversation.size() - 1; i >= 0; i--) {
                    long fReason = conversation.get(i).getReason();
                    if (!lReason.contains(fReason))
                        conversation.remove(i);
                }
            }
            //split page count
            conversation = conversationSplitPage(ctx,conversation, page, count);
        }
        return conversation;
    }

    public List<Conversation> conversationSplitPage(Context ctx,List<Conversation> conversation, int page, int count) {
        List<Conversation> outConversation = new ArrayList<Conversation>();
        int rowsCont = conversation.size();
        int pageCount = 0;
        if (conversation.size() % count == 0) {
            pageCount = conversation.size() / count;
        } else {
            pageCount = (conversation.size() / count) + 1;
        }
        if (page > pageCount) {
            page = pageCount;
        }
        if (page == 0) {
            if (count <= rowsCont) {
                for (int i = 0; i < count; i++) {
                    outConversation.add(conversation.get(i));
                }
            } else {
                outConversation = conversation;
            }
        } else {
            for (int i = ((page - 1) * count); i < conversation.size() && i < ((page) * count) && page > 0; i++) {
                outConversation.add(conversation.get(i));
            }
        }
        return outConversation;
    }

    public List<Conversation> getConversationByUserId(Context ctx,long userId, long[] reason, int page, int count) throws IOException, SQLException {
        List<Conversation> conversation = new ArrayList<Conversation>();
        try {
            conversation = conversationDB.getConversationByUser(ctx,userId, reason, page, count);
        } catch (Exception e) {
        }
        return conversation;
    }

    public List<Conversation> getConversationByPostIds(Context ctx,long[] postIds, long[] reason, int page, int count) throws IOException, SQLException {
        List<Long> wantPostIdByDB = new ArrayList<Long>();
        List<Conversation> conversationFromCache = new ArrayList<Conversation>();
        List<Conversation> conversationFromDB = new ArrayList<Conversation>();
        List<String> lReason = StringUtils2.splitList(StringUtils2.join(",", reason), ",", true);
        try {
            for (long postId : postIds) {
                List<Conversation> temp = conversationCache.getConversationListCache(postId);
                if (temp.size() <= 0) {
                    wantPostIdByDB.add(postId);
                } else {
                    for (Conversation c : temp) {
                        if (lReason.contains(c.getReason()))
                            conversationFromCache.add(c);
                    }
                }
            }
        } catch (Exception e) {
        }
        if (wantPostIdByDB.size() > 0) {
            long[] want = new long[wantPostIdByDB.size()];
            for (int i = 0; i < wantPostIdByDB.size(); i++) {
                want[i] = wantPostIdByDB.get(i);
            }
            conversationFromDB = conversationDB.getConversationByPostIds(ctx,want, reason, 0, 100);
        }

        List<Conversation> outConversation = new ArrayList<Conversation>();
        outConversation.addAll(conversationFromCache);
        outConversation.addAll(conversationFromDB);

        //sort
        ComConversation comparator = new ComConversation();
        Collections.sort(outConversation, comparator);

        return conversationSplitPage(ctx,outConversation, page, count);
    }

    public int compareConversation(Object arg0, Object arg1) {
        Conversation c0 = (Conversation) arg0;
        Conversation c1 = (Conversation) arg1;
        int flag = String.valueOf(c0.getCreatedTime()).compareTo(String.valueOf(c1.getCreatedTime()));
        return flag;
    }

    public boolean destroyConversation(Context ctx,long postId, long userId, int reason) throws SQLException {
        boolean bb = conversationDB.destroyConversation(ctx,postId, userId, reason);
        boolean b = conversationCache.destroyConversation(postId, userId, reason);
        return b && bb;
    }
}

class ComConversation implements Comparator {
    public int compare(Object arg0, Object arg1) {
        Conversation c0 = (Conversation) arg0;
        Conversation c1 = (Conversation) arg1;
        int flag = String.valueOf(c0.getCreatedTime()).compareTo(String.valueOf(c1.getCreatedTime()));
        return flag;
    }
}

