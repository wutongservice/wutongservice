package com.borqs.server.impl.privacy;


import com.borqs.server.platform.feature.privacy.PrivacyEntry;
import com.borqs.server.platform.feature.privacy.PrivacyTarget;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;

public class PrivacySql {

    public static List<String> insertPrivacy(String table, long userId, PrivacyEntry... entries) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (PrivacyEntry pe : entries) {
            sqls.add(new Sql().insertInto(table)
                    .values(
                            value("`user`", userId),
                            value("`res`", pe.resource),
                            value("`scope`", pe.target.scope),
                            value("`id`", pe.target.id),
                            value("`allow`", pe.allow)
                    ).onDuplicateKey().update().pairValues(
                            value("`allow`", pe.allow)
                    )
                    .toString());
        }
        return sqls;
    }

    public static String findPrivacy(String table, long userId, String[] res) {
        Sql sql = new Sql()
                .select("`user`", "`res`", "`scope`", "`id`", "`allow`")
                .from(table).useIndex("`user`")
                .where("`user`=:user_id", "user_id", userId);
        if (res.length == 1)
            sql.and("`res`=:resource", "resource", StringUtils.trimToEmpty(res[0]));
        else
            sql.and("`res` IN ($resources)", "resources", Sql.joinSqlValues(res, ","));
        return sql.toString();
    }

    public static String check(String table, long[] userIds, String res, int scope, String id) {
        Sql sql = new Sql()
                .select("`user`", "`res`", "`scope`", "`id`", "`allow`")
                .from(table).useIndex("`user`")
                .where("`res`=:resource", "resource", StringUtils.trimToEmpty(res))
                .and("`scope`=:scope", "scope", scope);

        if(scope == PrivacyTarget.SCOPE_USER)
        {
            sql.and("`id`=:id", "id", StringUtils.trimToEmpty(id));
        }

        if (userIds.length == 1)
            sql.and("`user`=:user_id", "user_id", userIds[0]);
        else
            sql.and("`user` IN ($user_ids)", "user_ids", StringHelper.join(userIds, ","));

        return sql.toString();
    }

    public static String deletePrivacy(String table, long userId, String[] resources) {
        Sql sql = new Sql().deleteFrom(table).where("`user`=:user_id", "user_id", userId);
        if (resources.length == 1) {
            sql.and("`res`=:resource", "resource", resources[0]);
        } else if (resources.length > 1) {
            sql.and("`res` IN ($resources)", "resources", Sql.joinSqlValues(resources, ","));
        } else {
            throw new IllegalArgumentException();
        }
        return sql.toString();
    }
}
