package com.borqs.server.impl.migration.stream;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

public class StreamMigSql {

    public static String getStream(Context ctx, String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" destroyed_time = 0 ").toString();
    }

    public static String findAllPostIds(String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" destroyed_time = 0 ").toString();
    }

}
