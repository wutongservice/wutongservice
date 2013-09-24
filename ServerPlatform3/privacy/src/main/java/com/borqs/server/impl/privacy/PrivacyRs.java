package com.borqs.server.impl.privacy;


import com.borqs.server.platform.feature.privacy.PrivacyEntry;
import com.borqs.server.platform.feature.privacy.PrivacyTarget;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrivacyRs {
    public static List<PrivacyEntry> read(ResultSet rs) throws SQLException {
        ArrayList<PrivacyEntry> list = new ArrayList<PrivacyEntry>();
        while (rs.next()) {
            PrivacyEntry pe = PrivacyEntry.of(rs.getLong("user"), rs.getString("res"),
                    new PrivacyTarget(rs.getInt("scope"), rs.getString("id")),
                    rs.getInt("allow") != 0);
            list.add(pe);
        }

        return list;
    }
}
