package com.borqs.server.impl.conversation;

import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.StringHelper;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;

public class ConversationSql {

    public static List<String> insertConversion(String table, Conversation... conversations) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (Conversation conversation : conversations) {
            sqls.add(new Sql().insertInto(table)
                    .values(
                            value("`type`", conversation.getTarget().type),
                            value("`id`", conversation.getTarget().id),
                            value("`reason`", conversation.getReason()),
                            value("`user`", conversation.getUser()),
                            value("`created_time`", conversation.getCreatedTime())
                    ).onDuplicateKey().update().pairValues(
                            value("`created_time`", conversation.getCreatedTime())
                    )
                    .toString());
        }

        return sqls;
    }

    public static List<String> deleteConversion(String table, Conversation... conversations) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (Conversation conversation : conversations) {
            sqls.add(new Sql().deleteFrom(table)
                    .where("`type`=:type", "type", conversation.getTarget().type)
                    .and("`id`=:id", "id", conversation.getTarget().id)
                    .and("`reason`=:reason", "reason", conversation.getReason())
                    .and("`user`=:user", "user", conversation.getUser())
                    .toString());
        }

        return sqls;
    }
    
    public static List<String> deleteConversion(String table, Target... targets) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (Target target : targets) {
            sqls.add(new Sql().deleteFrom(table)
                    .where("`type`=:type", "type", target.type)
                    .and("`id`=:id", "id", target.id)
                    .toString());
        }

        return sqls;
    }

    public static String findByTarget(String table, int[] reasons, Page page, Target... targets) {
        ArrayList<Sql> sqls = new ArrayList<Sql>();

        for (Target target : targets) {
            Sql sql = new Sql()
                    .select("*")
                    .from(table).useIndex("`target`")
                    .where("`type`=:type", "type", target.type)
                    .and("`id`=:id", "id", target.id);
            if (reasons.length == 1)
                sql.and("`reason`=:reason", "reason", reasons[0]);
            else if (reasons.length > 1)
                sql.and("`reason` IN ($reasons)", "reasons", StringHelper.join(reasons, ","));
            sql.orderBy("created_time", "desc");
            sqls.add(sql);
        }

        return Sql.unionAll(sqls).page(page).toString();

    }

    public static String findByUser(String table, int[] reasons, int targetType, Page page, long... userIds) {
        Sql sql = new Sql()
                .select("*")
                .from(table).useIndex("`user`");

        if (userIds.length == 1)
            sql.where("`user`=:user", "user", userIds[0]);
        else
            sql.where("`user` IN ($users)", "users", StringHelper.join(userIds, ","));


        if (reasons.length == 1)
            sql.and("`reason`=:reason", "reason", reasons[0]);
        else if (reasons.length > 1)
            sql.and("`reason` IN ($reasons)", "reasons", StringHelper.join(reasons, ","));

        if (targetType != Target.NONE)
            sql.and("type = :target_type", "target_type", targetType);

        sql.orderBy("created_time", "desc");
        return sql.page(page).toString();
    }
    
    public static String getCount(String table, int reason, long userId, Target... targets) {
        if (userId > 0)
            return new Sql()
                    .select("count(*)")
                    .from(table).useIndex("`target`")
                    .where("`type`=:type", "type", targets[0].type)
                    .and("`id`=:id", "id", targets[0].id)
                    .and("`reason`=:reason", "reason", reason)
                    .and("`user`=:user", "user", userId)
                    .toString();
        else {
            return findByTarget(table, new int[]{reason}, null, targets);
        }
    }
}
