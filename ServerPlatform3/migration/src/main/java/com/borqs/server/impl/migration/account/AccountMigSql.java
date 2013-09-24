package com.borqs.server.impl.migration.account;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import static com.borqs.server.platform.sql.Sql.value;
import static com.borqs.server.platform.sql.Sql.valueIf;


public class AccountMigSql {

    public static String getAccount(Context ctx, String table) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" 1=1 ").toString();
    }

    public static String insertUserMigration(String table, User user) {
        return new Sql().insertInto(table).values(
                value("user_id", user.getUserId()),
                value("password", user.getPassword()),
                value("created_time", user.getCreatedTime()),
                value("status", user.getAddon("status", "") == null ? "" : user.getAddon("status", "")),
                value("status_updated_time", user.getAddon("status_updated_time", 0)),
                value("destroyed_time", 0)
        ).toString();
    }

    public static String findAllUserIds(String table) {
        return new Sql().select("user_id").from(table).where(" destroyed_time=0").toString();
    }

    public static String findUsers(String table, long... userIds) {
        Validate.isTrue(userIds.length > 0);
        if (userIds.length == 1) {
            return new Sql().select("*").from(table).where("user_id=:user_id AND destroyed_time=0", "user_id", userIds[0]).toString();
        } else { // userIds.length > 1
            return new Sql().select("*").from(table).where("user_id IN ($user_ids) AND destroyed_time=0", "user_ids", StringHelper.join(userIds, ",")).toString();
        }
    }

    public static String findProperties(String table, long... userIds) {
        Validate.isTrue(userIds.length > 0);
        if (userIds.length == 1) {
            return new Sql()
                    .select("*")
                    .from(table)
                    .where("user=:user_id", "user_id", userIds[0])
                    .orderBy("`index`", "ASC")
                    .toString();
        } else {
            return new Sql()
                    .select("*")
                    .from(table)
                    .where("user IN ($user_ids)", "user_ids", StringHelper.join(userIds, ","))
                    .orderBy("`index`", "ASC")
                    .toString();
        }
    }

    public static String insertBindingInfo(String table, long userId, BindingInfo bi) {
        return new Sql().insertInto(table).values(
                value("info", bi.getInfo()),
                value("`user`", userId),
                value("`type`", bi.getType()),
                value("created_time", DateHelper.nowMillis())
        ).toString();
    }

    public static String updateUser(String table, User user) {
        if (StringUtils.isEmpty((String) user.getAddon("sort_key", "")))
            return "";
        else
            return new Sql()
                    .update(table)
                    .setValues(
                            valueIf("sort_key", user.getAddon("sort_key", ""), user.getAddon("sort_key", "") != null)
                    ).where("user_id=:user_id AND destroyed_time=0", "user_id", user.getUserId())
                    .toString();
    }

}
