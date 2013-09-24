package com.borqs.server.impl.setting;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingRs {
    public static Map<String, String> read(ResultSet rs) throws SQLException {
        LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
        while (rs.next()) {
            String key = rs.getString("key");
            String val = rs.getString("value");
            m.put(key, val);
        }
        return m;
    }
}
