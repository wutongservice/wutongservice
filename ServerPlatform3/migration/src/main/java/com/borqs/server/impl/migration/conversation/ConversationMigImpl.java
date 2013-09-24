package com.borqs.server.impl.migration.conversation;


import com.borqs.server.impl.conversation.ConversationDb;
import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.impl.migration.comment.CommentMigImpl;
import com.borqs.server.impl.migration.stream.StreamMigImpl;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConversationMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(ConversationMigImpl.class);

    private final ConversationMigDb db_migration = new ConversationMigDb();
    private final ConversationDb dbNewCoversation = new ConversationDb();

    private AccountMigImpl account;
    private StreamMigImpl post;
    private CommentMigImpl comment;
    //private PhotoMigImpl photo;


    public void setPost(StreamMigImpl post) {
        this.post = post;
    }

    public void setComment(CommentMigImpl comment) {
        this.comment = comment;
    }

    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        dbNewCoversation.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
    }

    public void setNewConversationTable0(Table newConversationTable0) {
        dbNewCoversation.setConvTable0(newConversationTable0);
    }

    public void setNewConversationTable1(Table newConversationTable1) {
        dbNewCoversation.setConvTable1(newConversationTable1);
    }

    public void setOldConversationTable(Table oldConversationTable) {
        db_migration.setConversationTable(oldConversationTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("conversation.mig")) {
            conversationMigration(Context.create());
        }
    }

    public void conversationMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, ConversationMigImpl.class, "conversationMigration", ctx);

        List<Conversation> conversations = null;

        try {

            db_migration.setUserIdMap(getAllUserIdMap(ctx));
            db_migration.setCommentIdMap(getAllCommentIdMap(ctx));
            db_migration.setPostIdMap(getAllPostIdMap(ctx));

            conversations = db_migration.getConversations(ctx);

            for (Conversation conversation : conversations) {
                try {
                    if (conversation != null) {
                        dbNewCoversation.create(ctx, conversation);
                    }
                } catch (RuntimeException e) {
                    LC.endCall();
                    throw e;
                }
            }
            long a = ConversationMigRs.Counter;
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall();
            throw e;
        }
    }


    private Map<Long, String> getAllUserIdMap(Context ctx) {
        return account.getAllUserIdMap(ctx);
    }

    private Map<Long, String> getAllPostIdMap(Context ctx) {
        return post.getAllPostIdMap(ctx);
    }

    private Map<Long, String> getAllCommentIdMap(Context ctx) {
        return comment.getAllCommentIdMap(ctx);
    }

    private Map<Long, String> getAllPhotoIdMap(Context ctx) {
        return account.getAllUserIdMap(ctx);
    }

}
