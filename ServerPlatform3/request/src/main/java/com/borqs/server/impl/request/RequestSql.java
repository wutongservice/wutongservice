package com.borqs.server.impl.request;

import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.platform.sql.Sql.value;

public class RequestSql {
    public static List<String> insertRequest(String table, Request... requests) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (Request request : requests) {
            sqls.add(new Sql().insertInto(table)
                    .values(
                            value("`id`", request.getRequestId()),
                            value("`from`", request.getFrom()),
                            value("`to`", request.getTo()),
                            value("`app`", request.getApp()),
                            value("`type`", request.getType()),
                            value("`message`", request.getMessage()),
                            value("`data`", request.getData()),
                            value("`status`", request.getStatus()),
                            value("`done_time`", request.getDoneTime()),
                            value("`created_time`", request.getCreatedTime())
                    ).toString());
        }

        return sqls;
    }

    public static List<String> insertRequestIndex(String table, Request... requests) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (Request request : requests) {
            sqls.add(new Sql().insertIgnoreInto(table)
                    .values(
                            value("`id`", request.getRequestId()),
                            value("`from`", request.getFrom()),
                            value("`to`", request.getTo()),
                            value("`type`", request.getType()),
                            value("`status`", request.getStatus())
                    ).toString());
        }
        
        return sqls;
    }

    public static List<String> doneRequest(String table, boolean isIndex, long... requestIds) {
        ArrayList<String> sqls = new ArrayList<String>();
        Sql.ValuePair[] values = new Sql.ValuePair[]{value("`status`", Request.STATUS_DONE)};
        if (!isIndex)
            ArrayUtils.add(values, value("`done_time`", DateHelper.nowMillis()));

        for (long requestId : requestIds) {
            sqls.add(new Sql().update(table).setValues(values)
                    .where("`id`=:id", "id", requestId).toString());
        }

        return sqls;
    }
    
    public static String getRequests(String table, long... requestIds) {
        return new Sql()
                .select("*")
                .from(table).useIndex("`to`")
                .where("`id` IN ($ids)", "ids", StringHelper.join(requestIds, ","))
                .toString();
    }
    
    public static String getRequests(String table, int status, long toId, int app, int type, int limit) {
        Sql sql = new Sql()
                .select("*")
                .from(table).useIndex("`to`")
                .where("`to`=:to", "to", toId);

        if (app != App.APP_NONE)
            sql.and("`app`=:app", "app", app);
        
        if (type != Request.TYPE_ANY)
            sql.and("`type`=:type", "type", type);

        if (status != Request.STATUS_ANY)
            sql.and("`status`=:status", "status", status);

        sql.orderBy("`created_time`", "DESC");

        if (status != Request.STATUS_PENDING)
            sql.limit(limit);
        
        return sql.toString();
    }
    
    public static String getPendingCount(String table, long toId, int app, int type) {
        Sql sql = new Sql()
                .select("count(*)")
                .from(table).useIndex("`to`")
                .where("`to`=:to", "to", toId)
                .and("`app`=:app", "app", app)                
                .and("`status`=:status", "status", Request.STATUS_PENDING);

        if (type != 0)
            sql.and("`type`=:type", "type", type);

        return sql.toString();
    }
    
    public static String getPendingTypes(String table, long fromId, long... toIds) {
        return new Sql()
                .select("DISTINCT(`type`), `to`")
                .from(table).useIndex("`from`")
                .where("`from`=:from", "from", fromId)
                .and("`to` IN ($toIds)", "toIds", StringHelper.join(toIds, ","))
                .and("`status`=:status", "status", Request.STATUS_PENDING)
                .toString();
    }
}
