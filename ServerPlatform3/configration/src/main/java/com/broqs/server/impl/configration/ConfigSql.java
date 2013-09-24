package com.broqs.server.impl.configration;


import com.borqs.server.platform.feature.configuration.Config;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import org.apache.commons.lang.StringUtils;

import static com.borqs.server.platform.sql.Sql.value;

public class ConfigSql {
    public static String insertConfig(String table, Config config) {
        return new Sql().insertInto(table).values(
                value("user_id", config.getUserId()),
                value("config_key", config.getConfigKey()),
                value("value", config.getValue()),
                value("version_code", config.getVersionCode()),
                value("content_type", config.getContentType()),
                value("created_time", DateHelper.nowMillis())
        ).toString();
    }

    public static String deleteConfig(String table, long userId, String key, int version_code) {
        Sql sql = new Sql().deleteFrom(table).where("user_id=:userId", "userId", userId);

        if (StringUtils.isNotEmpty(key))
            sql.and("key=:key", "key", key);
        if (version_code > -1)
            sql.and("version_code=version_code", "version_code", version_code);
        return sql.toString();

    }

    public static String getConfigs(String table, long userId, String key, int version_code) {
        return new Sql()
                .select("* ")
                .from(table)
                .where("user_id =:userId ", "userId", userId)
                .and("key=:key", "key", key)
                .and("version_code=:version_code", "version_code", version_code)
                .toString();
    }


    public static String getConfigByUserId(String table, long userId) {
        return new Sql()
                .select("* ")
                .from(table)
                .where("user_id =:userId ", "userId", userId).orderBy("config_key", "DESC")
                .toString();
    }
}
