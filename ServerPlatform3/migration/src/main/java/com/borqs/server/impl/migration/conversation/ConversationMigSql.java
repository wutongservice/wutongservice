package com.borqs.server.impl.migration.conversation;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

public class ConversationMigSql {

    public static String getConversations(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

}
