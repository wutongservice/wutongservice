package com.borqs.server.impl.migration.setting;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

public class SettingMigSql {

    public static String getSettings(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

}
