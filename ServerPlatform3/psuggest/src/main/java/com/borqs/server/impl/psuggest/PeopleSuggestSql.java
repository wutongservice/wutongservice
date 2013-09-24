package com.borqs.server.impl.psuggest;

import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.Status;
import com.borqs.server.platform.feature.psuggest.SuggestionReasons;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.StringHelper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.borqs.server.platform.sql.Sql.value;

public class PeopleSuggestSql {
    public static List<Sql.Entry> insertPeopleSuggest(String table, PeopleSuggest... suggests) {
        ArrayList<Sql.Entry> entries = new ArrayList<Sql.Entry>();

        for (PeopleSuggest suggest : suggests) {
            entries.add(Sql.entry(new Sql().insertIgnoreInto(table)
                    .values(
                            value("`user`", suggest.getUser()),
                            value("`type`", suggest.getSuggested().type),
                            value("`id`", suggest.getSuggested().id),
                            value("`reason`", suggest.getReason()),
                            value("`source`", suggest.getSource()),
                            value("`status`", suggest.getStatus()),
                            value("`created_time`", suggest.getCreatedTime()),
                            value("`deal_time`", suggest.getDealTime())
                    )
                    .toString(), suggest));
        }

        return entries;
    }

    public static String getPeopleSuggest(String table, PeopleSuggest... suggests) {
        ArrayList<Sql> sqls = new ArrayList<Sql>();
        for (PeopleSuggest suggest : suggests) {
            Sql sql = new Sql()
                    .select("*")
                    .from(table).useIndex("`user`")
                    .where("`user`=:user", "user", suggest.getUser())
                    .and("`type`=:type", "type", suggest.getSuggested().type)
                    .and("`id`=:id", "id", suggest.getSuggested().id)
                    .and("`reason`=:reason", "reason", suggest.getReason());
            sqls.add(sql);
        }
        return Sql.unionAll(sqls).toString();

    }
    
    public static String getPeopleSuggest(String table, long user, int reason, int status, long limit) {
        Sql sql = new Sql()
                .select("*")
                .from(table).useIndex("`user`")
                .where("`user`=:user", "user", user)                
                .and("`status`=:status", "status", status);
        if (reason != SuggestionReasons.REASON_NONE)
            sql.and("`reason`=:reason", "reason", reason);
        String orderBy = status == Status.INIT ? "`created_time`" : "`deal_time`";
        sql.orderBy(orderBy, "desc");
        if (limit > 0)
            sql.limit(limit);
        return sql.toString();
    }
    
    public static List<String> updateStatus(String table, long user, int status, long dealTime, PeopleId... friendIds) {
        ArrayList<String> sqls = new ArrayList<String>();
        
        for (PeopleId friendId : friendIds) {
            sqls.add(new Sql().update(table).setValues(value("`status`", status), value("`deal_time`", dealTime))
                    .where("`user`=:user", "user", user)
                    .and("`type`=:type", "type", friendId.type)
                    .and("`id`=:id", "id", friendId.id).toString());
        }

        return sqls;
    }

    public static List<String> updateSource(String table, Map<PeopleId, Map<Integer, long[]>> original, PeopleSuggest... suggests) {
        ArrayList<String> sqls = new ArrayList<String>();

        for (PeopleSuggest suggest : suggests) {
            PeopleId friendId = suggest.getSuggested();
            int reason = suggest.getReason();
            List<Long> addiSource = StringHelper.splitLongList(suggest.getSource(), ",");
            Map<Integer, long[]> m = original.get(friendId);
            List<Long> origSource = CollectionsHelper.toLongList(m.get(reason));
            LinkedList<Long> sourceLst = new LinkedList<Long>();
            sourceLst.addAll(origSource);
            sourceLst.addAll(addiSource);
            int size = sourceLst.size();            
            List<Long> l = size > 20 ? sourceLst.subList(size - 21, size - 1) : sourceLst;
            long[] source = CollectionsHelper.toLongArray(l);

            sqls.add(new Sql().update(table).setValues(value("`source`", StringHelper.join(source, ",")))
                    .where("`user`=:user", "user", suggest.getUser())
                    .and("`type`=:type", "type", friendId.type)
                    .and("`id`=:id", "id", friendId.id)
                    .and("`reason`=:reason", "reason", reason)
                    .toString());
        }

        return sqls;
    }

    public static String getPeopleSuggest(String table, long user,long id, int reason, int status, long limit) {
        Sql sql = new Sql()
                .select("*")
                .from(table).useIndex("`user`")
                .where("`user`=:user", "user", user)
                .and("`status`=:status", "status", status)
                .and("`id`=:id","id",id);
        if (reason != SuggestionReasons.REASON_NONE)
            sql.and("`reason`=:reason", "reason", reason);
        String orderBy = status == Status.INIT ? "`created_time`" : "`deal_time`";
        sql.orderBy(orderBy, "desc");
        if (limit > 0)
            sql.limit(limit);
        return sql.toString();
    }
}
