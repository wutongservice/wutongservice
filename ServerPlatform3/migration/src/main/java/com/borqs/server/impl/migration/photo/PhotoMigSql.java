package com.borqs.server.impl.migration.photo;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

public class PhotoMigSql {

    public static String getPhotoes(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

    public static String getAlbums(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

}
