package com.borqs.server.wutong.account2;


import com.borqs.server.wutong.account2.user.PropertyEntries;
import com.borqs.server.wutong.account2.user.PropertyEntry;
import com.borqs.server.wutong.account2.user.User;
import com.borqs.server.wutong.account2.util.ValuesNewAccount;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserRs {
    public static User readUser(ResultSet rs, User user) throws SQLException {
        if (user == null)
            user = new User();

        user.setUserId(rs.getLong("user_id"));
        user.setPassword(rs.getString("password"));
        user.setCreatedTime(rs.getLong("created_time"));
        user.setDestroyedTime(rs.getLong("destroyed_time"));
        user.setLoginEmail1(rs.getString("login_email1"));
        user.setLoginEmail2(rs.getString("login_email2"));
        user.setLoginEmail3(rs.getString("login_email3"));
        user.setLoginPhone1(rs.getString("login_phone1"));
        user.setLoginPhone2(rs.getString("login_phone2"));
        user.setLoginPhone3(rs.getString("login_phone3"));
        user.setStatus(rs.getString("status"));
        user.setAddon("sort_key",rs.getString("sort_key"));
        user.setAddon("perhaps_name",rs.getString("perhaps_name"));
        user.setStatusUpdatedTime(rs.getLong("status_updated_time"));
        return user;
    }

    public static PropertyEntry readProperty(ResultSet rs) throws SQLException {
        int type = rs.getInt("type");
        String s = rs.getString("value");
        return new PropertyEntry(rs.getInt("key"), rs.getInt("sub"), rs.getInt("index"), ValuesNewAccount.to(s, type), rs.getLong("updated_time"));
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
