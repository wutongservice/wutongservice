package com.borqs.server.impl.account;

import com.borqs.server.platform.data.Values;
import com.borqs.server.platform.feature.account.PropertyEntries;
import com.borqs.server.platform.feature.account.PropertyEntry;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.status.Status;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;
import static com.borqs.server.platform.sql.Sql.valueIf;


public class UserSql {
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

    public static String insertUser(String table, User user) {
        return new Sql().insertInto(table).values(
                value("password", user.getPassword()),
                value("created_time", user.getCreatedTime()),
                value("destroyed_time", 0)
        ).toString();
    }

    public static List<String> insertProperties(String table, User user) {
        ArrayList<String> sqls = new ArrayList<String>();
        for (PropertyEntry entry : user.writeProperties(null, null)) {
            long userId = user.getUserId();
            long createdTime = user.getCreatedTime();
            Sql sql = new Sql().insertInto(table).values(
                    value("`user`", userId),
                    value("`key`", entry.key),
                    value("`sub`", entry.sub),
                    value("`index`", entry.index),
                    value("`updated_time`", createdTime),
                    value("`type`", Values.simpleTypeOf(entry.value)),
                    value("`value`", Values.toString(entry.value))
            );
            sqls.add(sql.toString());
        }
        return sqls;
    }

    public static String purgeUser(String table, long userId) {
        return new Sql().deleteFrom(table).where("user_id=:user_id", "user_id", userId).toString();
    }

    public static String destroyUser(String table, long userId, long now) {
        return new Sql().update(table).setValues(value("destroyed_time", now)).where("user_id=:user_id", "user_id", userId).toString();
    }

    public static String recoverUser(String table, long userId) {
        return new Sql().update(table).setValues(value("destroyed_time", 0L)).where("user_id=:user_id", "user_id", userId).toString();
    }

    public static String findUsers(String table, long... userIds) {
        Validate.isTrue(userIds.length > 0);
        if (userIds.length == 1) {
            return new Sql().select("*").from(table).where("user_id=:user_id AND destroyed_time=0", "user_id", userIds[0]).toString();
        } else { // userIds.length > 1
            return new Sql().select("*").from(table).where("user_id IN ($user_ids) AND destroyed_time=0", "user_ids", StringHelper.join(userIds, ",")).toString();
        }
    }

    public static String findUserPassword(String table, long userId) {
        return new Sql().select("password").from(table).where("user_id=:user_id AND destroyed_time=0", "user_id", userId).toString();
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

    public static String findUserId(String table, long userId) {
        return new Sql().select("user_id").from(table).where("user_id=:user_id AND destroyed_time=0", "user_id", userId).toString();
    }

    public static String findUserIds(String table, long[] userIds) {
        Validate.notNull(userIds.length > 0);
        return new Sql().select("user_id").from(table).where("user_id IN ($user_ids) AND destroyed_time=0", "user_ids", StringHelper.join(userIds, ",")).toString();
    }

    public static String updateUser(String table, User user) {
        if (user.getPassword() == null)
            return "";
        else
            return new Sql()
                    .update(table)
                    .setValues(
                            valueIf("password", user.getPassword(), user.getPassword() != null)
                    ).where("user_id=:user_id AND destroyed_time=0", "user_id", user.getUserId())
                    .toString();
    }

    public static List<String> updateProperties(String table, User user) {
        ArrayList<String> sqls = new ArrayList<String>();
        if (user.hasProperties()) {
            long userId = user.getUserId();
            long now = DateHelper.nowMillis();
            HashSet<Integer> affectedKeys = new HashSet<Integer>();
            PropertyEntries entries = user.writeProperties(null, affectedKeys);
            sqls.add(new Sql()
                    .deleteFrom(table)
                    .where("`user`=:user_id AND `key` IN ($keys)", "user_id", userId, "keys", StringUtils.join(affectedKeys, ","))
                    .toString());


            for (PropertyEntry entry : entries) {
                sqls.add(new Sql()
                        .insertInto(table).values(
                                value("`user`", userId),
                                value("`key`", entry.key),
                                value("`sub`", entry.sub),
                                value("`index`", entry.index),
                                value("`updated_time`", now),
                                value("`type`", Values.simpleTypeOf(entry.value)),
                                value("`value`", Values.toString(entry.value))
                        )
                        .toString());
            }

        }
        return sqls;
    }

    public static String getStatuses(String table, long[] userIds) {
        Sql sql = new Sql()
                .select("user_id", "status", "status_updated_time")
                .from(table);
        if (userIds.length == 1) {
            sql.where("user_id=:user_id AND destroyed_time=0", "user_id", userIds[0]);
        } else if (userIds.length > 1) {
            sql.where("user_id IN ($user_ids) AND destroyed_time=0", "user_ids", StringHelper.join(userIds, ","));
        } else {
            throw new IllegalArgumentException();
        }
        return sql.toString();
    }

    public static String updateStatus(String table, long userId, Status st) {
        return new Sql()
                .update(table)
                .setValues(
                        value("status", ObjectUtils.toString(st.status)),
                        value("status_updated_time", st.updatedTime))
                .where("user_id=:user_id", "user_id", userId)
                .toString();
    }

    public static String search(String table, String word) {
        return new Sql()
                .select("`user`", "`key`", "`sub`")
                .from(table)
                .where("`value` LIKE :word", "word", "%" + ObjectUtils.toString(word) + "%")
                .toString();
    }

    public static String getUserCount(String table) {
        return new Sql()
                .select(" count(user_id) ")
                .from(table)
                .where(" destroyed_time=0").toString();
    }
}
