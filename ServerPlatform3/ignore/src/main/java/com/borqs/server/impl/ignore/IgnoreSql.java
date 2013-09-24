package com.borqs.server.impl.ignore;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.RandomHelper;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;


public class IgnoreSql {

    public static List<String> ignore(Context ctx, String table, int feature, Target... targets) {
        List<String> sqlList = new ArrayList<String>();
        if (targets.length == 1) {
            String sql = new Sql().insertInto("`" + table + "`").values(
                    value("ignore_id", RandomHelper.generateId()),
                    value("user_id", ctx.getViewer()),
                    value("created_time", DateHelper.nowMillis()),
                    value("destroyed_time", 0),
                    value("target_id", targets[0].id),
                    value("target_type", targets[0].type),
                    value("feature", feature)
            ).toString();
            sqlList.add(sql);
        } else {

            for (Target target : targets) {
                String sql = new Sql().insertInto("`" + table + "`").values(
                        value("ignore_id", RandomHelper.generateId()),
                        value("user_id", ctx.getViewer()),
                        value("created_time", DateHelper.nowMillis()),
                        value("destroyed_time", 0),
                        value("target_id", target.id),
                        value("target_type", target.type),
                        value("feature", feature)
                ).toString();
                sqlList.add(sql);
            }

        }
        return sqlList;
    }

    private static String unIgnoreSingle(Context ctx, String table, int feature, Target target) {
        return new Sql().deleteFrom("`" + table + "`")
                .where("target_id =:target_id", "target_id", target.id)
                .and("target_type=:target_type", "target_type", target.type)
                .and("feature=:feature", "feature", feature)
                .and("user_id=:user_id", "user_id", ctx.getViewer()).toString();
    }

    public static List<String> unIgnore(Context ctx, String table, int feature, Target... targets) {
        List<String> listSql = new ArrayList<String>();
        if (targets.length == 1) {
            listSql.add(unIgnoreSingle(ctx, table, feature, targets[0]));
        } else {
            for (int i = 0; i < targets.length; i++) {
                String sql = unIgnoreSingle(ctx, table, feature, targets[i]);
                listSql.add(sql);
            }
        }
        return listSql;
    }

    public static String getIgnore(Context ctx, String table, long userId, int feature) {
        return new Sql().select("*").from("`" + table + "`")
                .where("feature=:feature", "feature", feature)
                .and("user_id=:user_id", "user_id", userId).toString();
    }

    public static String getIgnoreExists(Context ctx, String table, long userId, int feature, Target target) {
        return new Sql().select("*").from("`" + table + "`")
                .where("feature=:feature", "feature", feature)
                .and("user_id=:user_id", "user_id", userId)
                .and("target_id=:targetId","targetId",target.id)
                .and("target_type=:targetType","targetType",target.type).
                toString();
    }


}
