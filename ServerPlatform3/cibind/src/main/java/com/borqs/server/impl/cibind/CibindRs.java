package com.borqs.server.impl.cibind;


import com.borqs.server.platform.feature.cibind.BindingInfo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CibindRs {

    public static Map<Long, List<BindingInfo>> readMultipleBindingInfo(ResultSet rs, Map<Long, List<BindingInfo>> reuse) throws SQLException {
        Map<Long, List<BindingInfo>> m = reuse != null ? reuse : new HashMap<Long, List<BindingInfo>>();
        while (rs.next()) {
            long userId = rs.getLong("user");
            List<BindingInfo> bis = m.get(userId);
            if (bis == null) {
                bis = new ArrayList<BindingInfo>();
                m.put(userId, bis);
            }
            bis.add(new BindingInfo(rs.getString("type"), rs.getString("info")));
        }
        return m;
    }

//    static List<BindingInfo> readOneBindingInfo(ResultSet rs, List<BindingInfo> reuse) throws SQLException {
//        List<BindingInfo> bis = reuse != null ? reuse : new ArrayList<BindingInfo>();
//        while (rs.next()) {
//            bis.add(new BindingInfo(rs.getString("type"), rs.getString("info")));
//        }
//        return bis;
//    }
}
