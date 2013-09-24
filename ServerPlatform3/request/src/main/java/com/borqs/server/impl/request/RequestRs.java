package com.borqs.server.impl.request;

import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.feature.request.Requests;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RequestRs {
    public static Requests read(ResultSet rs) throws SQLException {
        Requests requests = new Requests();
        while (rs.next()) {
            requests.add(new Request(rs.getLong("id"), rs.getLong("from"),
                    rs.getLong("to"), rs.getInt("app"), rs.getInt("type"),
                    rs.getLong("created_time"), rs.getLong("done_time"), rs.getInt("status"),
                    rs.getString("message"), rs.getString("data")));
        }

        return requests;
    }
}
