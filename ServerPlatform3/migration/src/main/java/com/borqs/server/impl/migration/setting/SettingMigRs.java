package com.borqs.server.impl.migration.setting;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class SettingMigRs {


    public static MigrationSetting readSetting(ResultSet rs, Map<String, String> setting, Map<Long, String> mapAccount) throws SQLException {
        MigrationSetting ms = new MigrationSetting();
        long user = rs.getLong("user");
        if(!mapAccount.containsKey(user))
            return null;
        ms.setUser(user);
        ms.setSettingKey(rs.getString("setting_key"));
        ms.setSettingValue(rs.getString("setting_value"));
        return ms;
    }


}
