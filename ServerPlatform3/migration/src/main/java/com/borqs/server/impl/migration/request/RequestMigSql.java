package com.borqs.server.impl.migration.request;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

public class RequestMigSql {

    public static String getRequests(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

}
