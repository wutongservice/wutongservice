package com.borqs.server.wutong.account2;

import com.borqs.server.base.sql.Sql;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;

import com.borqs.server.wutong.account2.user.PropertyEntries;
import com.borqs.server.wutong.account2.user.PropertyEntry;
import com.borqs.server.wutong.account2.user.User;
import com.borqs.server.wutong.account2.util.ValuesNewAccount;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.base.sql.Sql.value;
import static com.borqs.server.base.sql.Sql.valueIf;


public class UserSql {
    public static String insertUserMigration(String table, User user) {
        return new Sql().insertInto(table).values(
                value("user_id", user.getUserId()),
                value("password", user.getPassword()),
                value("created_time", user.getCreatedTime()),
                value("login_email1", user.getLoginEmail1()),
                value("login_email2", user.getLoginEmail2()),
                value("login_email3", user.getLoginEmail3()),
                value("login_phone1", user.getLoginPhone1()),
                value("login_phone2", user.getLoginPhone2()),
                value("login_phone3", user.getLoginPhone3()),
                value("destroyed_time", 0)
        ).toString();
    }

    public static String insertUser(String table, User user) {
        return new Sql().insertInto(table).values(
                value("user_id", user.getUserId()),
                value("password", user.getPassword()),
                value("created_time", user.getCreatedTime()),
                value("login_email1", user.getLoginEmail1()),
                value("login_email2", user.getLoginEmail2()),
                value("login_email3", user.getLoginEmail3()),
                value("login_phone1", user.getLoginPhone1()),
                value("login_phone2", user.getLoginPhone2()),
                value("login_phone3", user.getLoginPhone3()),
                value("sort_key", user.getAddon("sort_key", "")),
                value("display_name", user.getAddon("display_name", "")),
                value("destroyed_time", 0)
        ).toString();
    }

    public static List<String> insertProperties(String table, User user) {
        ArrayList<String> sqls = new ArrayList<String>();
        for (PropertyEntry entry : user.writeProperties(null)) {
            long userId = user.getUserId();
            long createdTime = user.getCreatedTime();
            Sql sql = new Sql().insertInto(table).values(
                    value("`user`", userId),
                    value("`key`", entry.key),
                    value("`sub`", entry.sub),
                    value("`index`", entry.index),
                    value("`updated_time`", createdTime),
                    value("`type`", ValuesNewAccount.simpleTypeOf(entry.value)),
                    value("`value`", ValuesNewAccount.toString(entry.value))
            );
            sqls.add(sql.toString());
        }
        return sqls;
    }

    public static String purgeUser(String table, long userId) {
        return new Sql().deleteFrom(table).where("user_id=:user_id", "user_id", userId).toString();
    }

    public static String purgeUser_Property(String table, long userId) {
        return new Sql().deleteFrom(table).where("user=:user_id", "user_id", userId).toString();
    }

    public static String destroyUser(String table, long userId, long now) {
        return new Sql().update(table).setValues(value("destroyed_time", now)).where("user_id=:user_id", "user_id", userId).toString();
    }

    public static String updateVisitTime(String table, long userId, long time) {
        return new Sql().update(table).setValues(value("lastvisit_time", time)).where("user_id=:user_id", "user_id", userId).toString();
    }

    public static String recoverUser(String table, long userId) {
        return new Sql().update(table).setValues(value("destroyed_time", 0L)).where("user_id=:user_id", "user_id", userId).toString();
    }

    public static String findUsers(String table, long... userIds) {
        Validate.isTrue(userIds.length > 0);
        if (userIds.length == 1) {
            return new Sql().select("*").from(table).where("user_id=:user_id AND destroyed_time=0", "user_id", userIds[0]).toString();
        } else { // userIds.length > 1
            return new Sql().select("*").from(table).where("user_id IN ($user_ids) AND destroyed_time=0", "user_ids", StringUtils2.join(userIds, ",")).toString();
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
                    .where("user IN ($user_ids)", "user_ids", StringUtils2.join(userIds, ","))
                    .orderBy("`index`", "ASC")
                    .toString();
        }
    }

    public static String findUserId(String table, long userId) {
        return new Sql().select("user_id").from(table).where("user_id=:user_id AND destroyed_time=0", "user_id", userId).toString();
    }

    public static String findUserIds(String table, long[] userIds) {
        Validate.notNull(userIds.length > 0);
        return new Sql().select("user_id").from(table).where("user_id IN ($user_ids) AND destroyed_time=0", "user_ids", StringUtils2.join(userIds, ",")).toString();
    }

    public static String updateUser(String table, User user) {
        if (!user.hasAddons())
            user.setAddon("undefine", "");
        if (StringUtils.isEmpty(user.getPassword()) && StringUtils.isEmpty(user.getStatus())
                && (user.getLoginEmail1() == null)
                && (user.getLoginEmail2() == null)
                && (user.getLoginEmail3() == null)
                && (user.getLoginPhone1() == null)
                && (user.getLoginPhone2() == null)
                && (user.getLoginPhone3() == null)
                && StringUtils.isEmpty((String) user.getAddon("sort_key", ""))
                && StringUtils.isEmpty((String) user.getAddon("perhaps_name", ""))
                && StringUtils.isEmpty((String) user.getAddon("display_name", ""))
                )
            return "";
        return new Sql()
                .update(table)
                .setValues(
                        valueIf("password", user.getPassword(), user.getPassword() != null),
                        valueIf("login_email1", user.getLoginEmail1(), user.getLoginEmail1() != null),
                        valueIf("login_email2", user.getLoginEmail2(), user.getLoginEmail2() != null),
                        valueIf("login_email3", user.getLoginEmail3(), user.getLoginEmail3() != null),
                        valueIf("login_phone1", user.getLoginPhone1(), user.getLoginPhone1() != null),
                        valueIf("login_phone2", user.getLoginPhone2(), user.getLoginPhone2() != null),
                        valueIf("login_phone3", user.getLoginPhone3(), user.getLoginPhone3() != null),
                        valueIf("sort_key", user.getAddon("sort_key", ""), StringUtils.isNotEmpty((String) user.getAddon("sort_key", ""))),
                        valueIf("perhaps_name", user.getAddon("perhaps_name", ""), StringUtils.isNotEmpty((String) user.getAddon("perhaps_name", ""))),
                        valueIf("status", user.getStatus(), user.getStatus() != null),
                        valueIf("display_name", user.getAddon("display_name", ""), StringUtils.isNotEmpty((String) user.getAddon("display_name", ""))),
                        valueIf("status_updated_time", DateUtils.nowMillis(), user.getStatus() != null)
                ).where("user_id=:user_id AND destroyed_time=0", "user_id", user.getUserId())
                .toString();
    }

    public static List<String> updateProperties(String table, User user) {

        ArrayList<String> sqls = new ArrayList<String>();
        if (user.hasProperties()) {
            long userId = user.getUserId();
            long now = DateUtils.nowMillis();
            PropertyEntries entries = user.writeProperties(null);
            int[] keys = entries.getKeys();
            sqls.add(new Sql()
                    .deleteFrom(table)
                    .where("`user`=:user_id AND `key` IN ($keys)", "user_id", userId, "keys", StringUtils2.join(keys, ","))
                    .toString());


            int index = 0;
            for (PropertyEntry entry : entries) {
                if (StringUtils.isNotEmpty(ValuesNewAccount.toString(entry.value)))
                    sqls.add(new Sql()
                            .insertInto(table).values(
                                    value("`user`", userId),
                                    value("`key`", entry.key),
                                    value("`sub`", entry.sub),
                                    value("`index`", entry.index),
                                    value("`updated_time`", now),
                                    value("`type`", ValuesNewAccount.simpleTypeOf(entry.value)),
                                    value("`value`", ValuesNewAccount.toString(entry.value))
                            )
                            .toString());
            }

        }
        return sqls;
    }

    public static String getAllUserIds(String table) {
        return new Sql().select("user_id").from(table).where(" destroyed_time=0").toString();
    }

}
