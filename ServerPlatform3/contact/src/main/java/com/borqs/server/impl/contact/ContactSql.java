package com.borqs.server.impl.contact;

import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.sql.Sql;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;

public class ContactSql {
    public static List<String> insertContact(String table, Contact... contacts) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (Contact contact : contacts) {
            sqls.add(new Sql().insertIgnoreInto(table)
                    .values(
                            value("`owner`", contact.getOwner()),
                            value("`name`", contact.getName()),
                            value("`type`", contact.getType()),
                            value("`content`", contact.getContent()),
                            value("`reason`", contact.getReason()),
                            value("`created_time`", contact.getCreatedTime())
                    )
                    .toString());
        }

        return sqls;
    }

    public static List<String> deleteContact(String table, Contact... contacts) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (Contact contact : contacts) {
            sqls.add(new Sql().deleteFrom(table)
                    .where("`owner`=:owner", "owner", contact.getOwner())
                    .and("`type`=:type", "type", contact.getType())
                    .and("`content`=:content", "content", contact.getContent())
                    .and("`reason`=:reason", "reason", contact.getReason())
                    .toString());
        }

        return sqls;
    }

    public static String getContacts(String table, int reason, Contact... contacts) {
        ArrayList<Sql> sqls = new ArrayList<Sql>();
        
        for (Contact contact : contacts) {
            sqls.add(new Sql()
                    .select("*")
                    .from(table).useIndex("`owner`")
                    .where("`owner`=:owner", "owner", contact.getOwner())
                    .and("`type`=:type", "type", contact.getType())
                    .and("`content`=:content", "content", contact.getContent())
                    .and("`reason`=:reason", "reason", reason));
        }

        return Sql.unionAll(sqls).toString();
    }
    
    public static String getContacts(String table, int reason, long userId) {
        return new Sql().select("*")
                .from(table).useIndex("`owner`")
                .where("`owner`=:owner", "owner", userId)
                .and("`reason`=:reason", "reason", reason)
                .toString();
    }

    public static String searchContacts(String table, int reason, String content) {
        return new Sql().select("*")
                .from(table).useIndex("`content`")
                .where("`content`=:content", "content", content)
                .and("`reason`=:reason", "reason", reason)
                .toString();
    }
}
