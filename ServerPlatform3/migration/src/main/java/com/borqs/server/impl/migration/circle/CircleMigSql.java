package com.borqs.server.impl.migration.circle;

import com.borqs.server.platform.sql.Sql;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;


public class CircleMigSql {

   public static List<String> insertCircles(String table, List<MigrationCircle> circleList) {
        List<String> stringSqls = new ArrayList<String>();
        for (MigrationCircle circle : circleList) {
            if(circle == null || circle.getCircle()==0)
                continue;
            String sql = new Sql().insertInto(table).values(
                    value("user", circle.getUser()),
                    value("circle_id", circle.getCircle()),
                    value("name",circle.getName()),
                    value("updated_time", circle.getUpdated_time())
            ).toString();
            stringSqls.add(sql);
        }
        return stringSqls;
    }
    public static String getCircles(String table) {
            return new Sql()
                    .select(" * ")
                    .from(table)
                    .toString();
        }


}
