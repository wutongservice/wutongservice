package com.borqs.server.impl.migration.ticket;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

public class TicketMigSql {

    public static String getTickets(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

}
