package com.borqs.server.impl.setting;


import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.borqs.server.platform.sql.Sql.value;

public class SettingSql {

    public static List<String> insertSetting(String table, long userId, Map<String, String> setting) {
        ArrayList<String> sqls = new ArrayList<String>();
        long now = DateHelper.nowMillis();
        for (Map.Entry<String, String> e : setting.entrySet()) {
            String key = e.getKey();
            String val = StringUtils.trimToEmpty(e.getValue());
            if (StringUtils.isEmpty(key))
                continue;

            sqls.add(new Sql().insertInto(table)
                    .values(
                            value("`user`", userId),
                            value("`key`", key),
                            value("`value`", val),
                            value("updated_time", now)
                    ).onDuplicateKey().update().pairValues(
                            value("`value`", val),
                            value("updated_time", now)
                    )
                    .toString());
        }
        return sqls;
    }

    public static String findSetting(String table, long userId, String[] keys) {
        Sql sql = new Sql()
                .select("`key`", "`value`")
                .from(table).useIndex("`user`")
                .where("`user`=:user_id", "user_id", userId);
        if (keys.length == 1)
            sql.and("`key`=:key", "key", StringUtils.trimToEmpty(keys[0]));
        else
            sql.and("`key` IN ($keys)", "keys", Sql.joinSqlValues(keys, ","));
        return sql.toString();
    }

    public static String findSettingStartsWithKey(String table, long userId, String keyStartsWith) {
        return new Sql().select("`key`", "`value`").from(table).useIndex("`user`")
                .where("`user`=:user_id AND POSITION(:key_starts_with IN `key`) = 1", "user_id", userId, "key_starts_with", keyStartsWith)
                .toString();
    }

    public static String deleteSetting(String table, long userId, String[] keys) {
        Sql sql = new Sql().deleteFrom(table).where("`user`=:user_id", "user_id", userId);
        if (keys.length == 1) {
            sql.and("`key`=:key", "key", keys[0]);
        } else if (keys.length > 1) {
            sql.and("`key` IN ($keys)", "keys", Sql.joinSqlValues(keys, ","));
        } else {
            throw new IllegalArgumentException();
        }
        return sql.toString();
    }
}
