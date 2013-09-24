package com.borqs.server.impl.psuggest;

import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.PeopleSuggests;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PeopleSuggestRs {
    public static PeopleSuggests read(ResultSet rs) throws SQLException {
        PeopleSuggests suggests = new PeopleSuggests();
        while (rs.next()) {
            suggests.add(new PeopleSuggest(rs.getLong("user"),
                    new PeopleId(rs.getInt("type"), rs.getString("id")),
                    rs.getInt("reason"), rs.getString("source"), rs.getInt("status"),
                    rs.getLong("created_time"), rs.getLong("deal_time")));
        }

        return suggests;
    }
}
