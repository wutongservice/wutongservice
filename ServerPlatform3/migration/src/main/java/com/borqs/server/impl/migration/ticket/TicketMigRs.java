package com.borqs.server.impl.migration.ticket;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class TicketMigRs {


    public static MigrationTicket readTicket(ResultSet rs, Map<String, String> setting, Map<Long, String> mapAccount) throws SQLException {
        MigrationTicket ms = new MigrationTicket();
        long user = rs.getLong("user");
        if(!mapAccount.containsKey(user))
            return null;
        ms.setUser(user);
        ms.setTicket(rs.getString("ticket"));
        ms.setApp(rs.getInt("app"));
        ms.setCratedTime(rs.getLong("created_time"));
        return ms;
    }


}
