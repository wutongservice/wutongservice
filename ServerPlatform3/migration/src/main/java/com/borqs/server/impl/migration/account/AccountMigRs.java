package com.borqs.server.impl.migration.account;


import com.borqs.server.platform.data.Values;
import com.borqs.server.platform.feature.account.PropertyEntries;
import com.borqs.server.platform.feature.account.PropertyEntry;
import com.borqs.server.platform.feature.account.User;
import org.apache.commons.lang.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccountMigRs {

    public static User readUser(ResultSet rs, User user) throws SQLException {
        if (user == null)
            user = new User();

        user.setUserId(rs.getLong("user_id"));
        user.setPassword(rs.getString("password"));
        user.setCreatedTime(rs.getLong("created_time"));
        user.setDestroyedTime(rs.getLong("destroyed_time"));

        String login_email1 = rs.getString("login_email1");
        if(StringUtils.isNotEmpty(login_email1))
            user.setAddon("login_email1", login_email1);

        String login_email2 = rs.getString("login_email2");
        if(StringUtils.isNotEmpty(login_email2))
            user.setAddon("login_email2", login_email2);

        String login_email3 = rs.getString("login_email3");
        if(StringUtils.isNotEmpty(login_email3))
            user.setAddon("login_email3", login_email3);

        String login_phone1 = rs.getString("login_phone1");
        if(StringUtils.isNotEmpty(login_phone1))
            user.setAddon("login_phone1", login_phone1);

        String login_phone2 = rs.getString("login_phone2");
        if(StringUtils.isNotEmpty(login_phone2))
            user.setAddon("login_phone2", login_phone2);

        String login_phone3 = rs.getString("login_phone3");
        if(StringUtils.isNotEmpty(login_phone3))
            user.setAddon("login_phone3", login_phone3);

        user.setAddon("status", rs.getString("status"));
        user.setAddon("status_updated_time", rs.getLong("status_updated_time"));

        return user;
    }

    public static PropertyEntry readProperty(ResultSet rs) throws SQLException {
        int type = rs.getInt("type");
        String s = rs.getString("value");
        return new PropertyEntry(rs.getInt("key"), rs.getInt("sub"), rs.getInt("index"), Values.to(s, type), rs.getLong("updated_time"));
    }

    public static PropertyEntries readProperties(ResultSet rs) throws SQLException {
        PropertyEntries entries = new PropertyEntries();
        while (rs.next()) {
            entries.add(readProperty(rs));
        }

        return entries;
    }

    public static Map<Long, PropertyEntries> readGroupedProperties(ResultSet rs) throws SQLException {
        HashMap<Long, PropertyEntries> m = new HashMap<Long, PropertyEntries>();
        while (rs.next()) {
            long userId = rs.getLong("user");
            PropertyEntries props = m.get(userId);
            if (props == null) {
                props = new PropertyEntries();
                m.put(userId, props);
            }
            props.add(readProperty(rs));
        }
        return m;
    }

    public static Set<Long> readIds(ResultSet rs, Set<Long> reuse) throws SQLException {
        if (reuse == null)
            reuse = new HashSet<Long>();
        while (rs.next()) {
            reuse.add(rs.getLong("user_id"));
        }
        return reuse;
    }

}
