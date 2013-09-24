package com.borqs.server.impl.login;


import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;

import static com.borqs.server.platform.sql.Sql.value;


public class TicketSql {
    public static String findUser(String table, String ticket) {
        return new Sql().select("user").from(table).where("ticket=:ticket", "ticket", ticket).toString();
    }

    public static String insertTicket(String table, long userId, String ticket, int app) {
        return new Sql().insertInto(table).values(
                value("ticket", ticket),
                value("user", userId),
                value("created_time", DateHelper.nowMillis()),
                value("app", app)
        ).toString();
    }

    public static String deleteTicket(String table, String ticket) {
        return new Sql().deleteFrom(table).where("ticket=:ticket", "ticket", ticket).toString();
    }
}
