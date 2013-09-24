package com.borqs.server.impl.migration.request;


import com.borqs.server.platform.feature.request.Request;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class RequestMigRs {


    public static Request readRequest(ResultSet rs, Map<String, String> request, Map<Long, String> mapAccount) throws SQLException {
        Request ms = new Request();
        long user = rs.getLong("user");
        if(!mapAccount.containsKey(user))
            return null;
        ms.setTo(user);

        long source = rs.getLong("source");
        if(!mapAccount.containsKey(source))
            return null;
        ms.setFrom(source);

        ms.setRequestId(rs.getLong("request_id"));
        ms.setApp(rs.getInt("app"));
        ms.setType(rs.getInt("type"));
        ms.setCreatedTime(rs.getLong("created_time"));
        ms.setDoneTime(rs.getLong("done_time"));
        ms.setStatus(rs.getInt("status"));
        ms.setMessage(rs.getString("message"));
        ms.setData(rs.getString("data"));
        
        
        return ms;
    }


}
