package com.borqs.server.impl.conversation;

import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.feature.conversation.Conversations;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ConversationRs {
    public static Conversations read(ResultSet rs) throws SQLException {
        Conversations conversations = new Conversations();
        while (rs.next()) {
            conversations.add(new Conversation(rs.getLong("user"),
                    new Target(rs.getInt("type"), rs.getString("id")),
                    rs.getInt("reason"), rs.getLong("created_time")));
        }

        return conversations;
    }
}
