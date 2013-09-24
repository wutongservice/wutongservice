package com.borqs.server.impl.migration.conversation;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConversationMigDb extends SqlSupport {
    private static final Logger L = Logger.get(ConversationMigDb.class);

    private Map<Long, String> userIdMap;
    private Map<Long, String> postIdMap;
    private Map<Long, String> commentIdMap;
    // table
    private Table conversationTable;

    public ConversationMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }

    public void setPostIdMap(Map<Long, String> postIdMap) {
        this.postIdMap = postIdMap;
    }

    public void setCommentIdMap(Map<Long, String> commentIdMap) {
        this.commentIdMap = commentIdMap;
    }

    public Table getConversationTable() {
        return conversationTable;
    }

    public void setConversationTable(Table conversationTable) {
        this.conversationTable = conversationTable;
    }

    private ShardResult shardConversation() {
        return conversationTable.getShard(0);
    }

    public List<Conversation> getConversations(final Context ctx) {
        final ShardResult conversationSR = shardConversation();

        return sqlExecutor.openConnection(conversationSR.db, new SingleConnectionHandler<List<Conversation>>() {
            @Override
            protected List<Conversation> handleConnection(Connection conn) {

                final List<Conversation> conversationList = new ArrayList<Conversation>();
                String sql = ConversationMigSql.getConversations(ctx, conversationSR.table);
                SqlExecutor.executeList(ctx, conn, sql, conversationList, new ResultSetReader<Conversation>() {
                    @Override
                    public Conversation read(ResultSet rs, Conversation reuse) throws SQLException {
                        return ConversationMigRs.readConversation(rs, null, userIdMap,postIdMap,commentIdMap,null);
                    }
                });
                return conversationList;
            }
        });
    }

}

