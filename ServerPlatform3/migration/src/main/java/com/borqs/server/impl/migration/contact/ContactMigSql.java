package com.borqs.server.impl.migration.contact;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.Sql;

public class ContactMigSql {

    public static String getContacts1(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

    public static String getContacts2(Context ctx, String table) {
        return new Sql()
                .select(" * ")
                .from(table)
                .toString();
    }

}
