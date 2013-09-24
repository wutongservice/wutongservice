package com.borqs.server.impl.cibind;


import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.StringHelper;

import static com.borqs.server.platform.sql.Sql.value;

public class CibindSql {

    public static String findUserByInfo(String table, String info) {
        return new Sql().select("`user`").from(table).where("info=:info", "info", info).toString();
    }
    
    public static String findUsersByInfos(String table, String... infos) {
        if (infos.length == 1)
            return findUserByInfo(table, infos[0]);
        else
            return new Sql().select("info", "`user`").from(table).useIndex("user")
                    .where("info IN ($infos)", "infos", Sql.joinSqlValues(infos, ",")).toString();
    }

    public static String insertBindingInfo(String table, long userId, BindingInfo bi) {
        return new Sql().insertInto(table).values(
                value("info", bi.getInfo()),
                value("`user`", userId),
                value("`type`", bi.getType()),
                value("created_time", DateHelper.nowMillis())
        ).toString();
    }

    public static String deleteInfo(String table, String info) {
        return new Sql().deleteFrom(table).where("info=:info", "info", info).toString();
    }

    public static String getBindingInfo(String table, long[] userIds) {
        Sql sql = new Sql().select("info", "`user`", "`type`").from(table).useIndex("user");
        if (userIds.length == 1)
            sql.where("user=:user_id", "user_id", userIds[0]);
        else // userIds.length > 1
            sql.where("user IN ($user_ids)", "user_ids", StringHelper.join(userIds, ","));
        return sql.toString();
    }
}
