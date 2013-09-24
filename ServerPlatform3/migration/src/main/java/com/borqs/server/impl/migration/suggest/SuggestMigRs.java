package com.borqs.server.impl.migration.suggest;


import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.Status;
import org.apache.commons.lang.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SuggestMigRs {


    public static PeopleSuggest readSuggest(ResultSet rs, Map<String, String> suggest, Map<Long, String> mapAccount) throws SQLException {
        PeopleSuggest ps = new PeopleSuggest();
        long user = rs.getLong("user");
        if(!mapAccount.containsKey(user))
            return null;
        ps.setUser(user);
        ps.setDealTime(rs.getLong("refuse_time"));
        ps.setCreatedTime(rs.getLong("create_time"));
        String reason = rs.getString("reason");
        List<String> list = new ArrayList<String>();
        if(StringUtils.isNotEmpty(reason)){
            String [] r = reason.split(",");
            for(String s :r){
                if(mapAccount.containsValue(s))
                    list.add(s);
            }
        }
        ps.setSource(StringUtils.join(list,","));
        if(rs.getLong("refuse_time")>0)
            ps.setStatus(Status.REJECTED);
        else
            ps.setStatus(Status.ACCEPTED);

        ps.setReason(rs.getInt("type"));
        PeopleId peopleId = new PeopleId(PeopleId.USER,String.valueOf(rs.getInt("suggested")));
        ps.setSuggested(peopleId);

        return ps;
    }


}
