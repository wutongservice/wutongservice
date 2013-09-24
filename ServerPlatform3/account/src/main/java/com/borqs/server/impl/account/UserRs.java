package com.borqs.server.impl.account;


import com.borqs.server.platform.data.Values;
import com.borqs.server.platform.feature.account.PropertyEntries;
import com.borqs.server.platform.feature.account.PropertyEntry;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.status.Status;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserRs {
    public static User readUser(ResultSet rs, User user) throws SQLException {
        if (user == null)
            user = new User();

        user.setUserId(rs.getLong("user_id"));
        user.setPassword(rs.getString("password"));
        user.setCreatedTime(rs.getLong("created_time"));
        user.setDestroyedTime(rs.getLong("destroyed_time"));
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

    public static Map<Long, Status> readStatuses(ResultSet rs, Map<Long, Status> reuse) throws SQLException {
        Map<Long, Status> m = reuse != null ? reuse : new HashMap<Long, Status>();
        while (rs.next()) {
            long userId = rs.getLong("user_id");
            Status st = new Status(rs.getString("status"), rs.getLong("status_updated_time"));
            m.put(userId, st);
        }
        return m;
    }

    public static void readSearchEntries(ResultSet rs, List<UserDb.SearchUserEntry> entries, int count) throws SQLException {
        while (rs.next()) {
            long userId = rs.getLong("user");
            int key = rs.getInt("key");
            int sub = rs.getInt("sub");
            entries.add(new UserDb.SearchUserEntry(userId, key, sub));
            if (entries.size() >= count)
                break;
        }
    }
}
