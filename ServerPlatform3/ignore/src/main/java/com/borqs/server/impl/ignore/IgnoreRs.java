package com.borqs.server.impl.ignore;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.ignore.Ignore;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IgnoreRs {
    public static Target readIgnore(ResultSet rs, Ignore ignore) throws SQLException {
        Target target = new Target(rs.getInt("target_type"), rs.getString("target_id"));
        return target;
    }
}
