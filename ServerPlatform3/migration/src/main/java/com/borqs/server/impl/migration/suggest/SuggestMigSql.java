package com.borqs.server.impl.migration.suggest;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

public class SuggestMigSql {

    public static String getSuggest(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

}
