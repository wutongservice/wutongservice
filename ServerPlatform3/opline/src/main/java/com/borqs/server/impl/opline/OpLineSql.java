package com.borqs.server.impl.opline;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.opline.Operation;
import com.borqs.server.platform.feature.opline.Operations;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;

public class OpLineSql {

    public static List<String> insertOperations(String table, Operations opers) {
        ArrayList<String> sqls = new ArrayList<String>();
        for (Operation op : opers) {
            if (op == null)
                continue;

            sqls.add(new Sql()
                    .insertInto(table)
                    .values(
                            value("`oper_id`", op.getOperId()),
                            value("`user`", op.getUserId()),
                            value("`as_`", ObjectUtils.toString(op.getAsId())),
                            value("`action`", op.getAction()),
                            value("`flag`", 0),
                            value("`targets`", Target.toCompatibleString(op.getTargets(), ",")),
                            value("`info`", ObjectUtils.toString(op.getInfo()))
                    )
                    .toString());
        }
        return sqls;
    }

    public static String getOperationBefore(String table, long userId, long beforeOperId, long count) {
        return new Sql()
                .select("*")
                .from(table)
                .where("`user`=:user_id AND `oper_id` < :before_oper_id", "user_id", userId, "before_oper_id", beforeOperId)
                .orderBy("oper_id", "ASC")
                .limit(count)
                .toString();
    }

    public static String getLastOperation(String table, long userId, int[] actions) {
        Sql sql = new Sql()
                .select("*")
                .from(table)
                .where("`user`=:user_id", "user_id", userId);
        if (ArrayUtils.isNotEmpty(actions)) {
            sql.and("`action` IN ($actions)", "actions", StringHelper.join(actions,","));
        }
        sql.orderBy("oper_id", "DESC").limit(1);
        return sql.toString();
    }

    public static String getOpsWithFlag(String table, long userId, int[] actions, int flag, long minTime) {
        Sql sql = new Sql()
                .select("*")
                .from(table)
                .where("`user`=:user_id", "user_id", userId);
        if (ArrayUtils.isNotEmpty(actions)) {
            sql.and("`action` IN ($actions)", "actions", StringHelper.join(actions,","));
        }
        sql.and("oper_id > :min_oper_id", "min_oper_id", RandomHelper.timestampToId(minTime));
        sql.and("flag & :flag = 0", "flag", flag).orderBy("oper_id", "DESC");
        return sql.toString();
    }


    public static String setFlag(String table, int flag, long[] operIds) {
        return new Sql()
                .update(table)
                .setValues(
                        value("flag", Sql.raw("flag | " + flag))
                )
                .where("oper_id IN ($oper_ids)", "oper_ids", StringHelper.join(operIds, ","))
                .toString();
    }
}
