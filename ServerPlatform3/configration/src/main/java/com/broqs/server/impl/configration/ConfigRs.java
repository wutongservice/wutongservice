package com.broqs.server.impl.configration;


import com.borqs.server.platform.feature.configuration.Config;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigRs {

    public static Config readConfig(ResultSet rs, Config configration) throws SQLException {
        if (configration == null)
            configration = new Config();

        configration.setUserId(rs.getLong("user_id"));
        configration.setContentType(rs.getInt("content_type"));
        configration.setCreatedTime(rs.getLong("created_time"));

        return configration;
    }

}
