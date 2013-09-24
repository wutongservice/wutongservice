package com.borqs.server.wutong.favorite;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.ArrayUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteImpl implements FavoriteLogic, Initializable {
    private static final Logger L = Logger.getLogger(FavoriteImpl.class);
    public final Schema likeSchema = Schema.loadClassPath(FavoriteImpl.class, "favorite.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String favoriteTable;
    private Configuration conf;

    public void init() {
        conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("like.simple.db", null);
        this.favoriteTable = conf.getString("favorite.simple.favoriteTable", "favorite");
    }

    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    public boolean saveFavorite(Context ctx, Record favorite) {
        final String METHOD = "saveFavorite";
        L.traceStartCall(ctx, METHOD, favorite);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, favorite, add)}";

        String sql = SQLTemplate.merge(SQL,
                "table", favoriteTable, "alias", likeSchema.getAllAliases(),
                "favorite", favorite, "add", Record.of("created_time", DateUtils.nowMillis()));

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean destroyFavorite(Context ctx, String user_id, String target_type, String target_id) {
        final String METHOD = "destroyFavorite";
        L.traceStartCall(ctx, METHOD, user_id, target_type, target_id);
        String sql = "DELETE FROM " + favoriteTable + " WHERE user_id='" + user_id + "' AND target_type='" + target_type + "' AND target_id='" + target_id + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean getIFavorited(Context ctx, String user_id, String target_type, String target_id) {
        final String METHOD = "favorited";
        L.traceStartCall(ctx, METHOD, user_id, target_type, target_id);
        String sql = "SELECT count(*) as count1 FROM " + favoriteTable + " WHERE user_id='" + user_id + "' AND target_type='" + target_type + "' AND target_id='" + target_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        int n = (int) rec.getInt("count1");
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public Map<String, Boolean> getIFavorited(Context ctx, String user_id, String target_type, String[] targetIds) {
        final String METHOD = "getIFavorited";
        L.traceStartCall(ctx, METHOD, user_id, target_type, targetIds);
        final HashMap<String, Boolean> m = new HashMap<String, Boolean>();
        if (ArrayUtils.isNotEmpty(targetIds)) {
        // SELECT target_id FROM favorite where user_id=10015 and target_type=1 and target_id IN (10000, 10001);
        String sql = new SQLBuilder.Select()
                .select("target_id")
                .from(favoriteTable)
                .where("user_id=${v(user_id)} AND target_type=${v(target_type)} AND target_id IN (${vjoin(target_ids)})",
                        "user_id", user_id,
                        "target_type", target_type,
                        "target_ids", targetIds
                )
                .toString();
            SQLExecutor se = getSqlExecutor();
            se.executeRecordHandler(sql, new RecordHandler() {
                @Override
                public void handle(Record rec) {
                    m.put(rec.getString("target_id"), true);
                }
            });
            for (String targetId : targetIds) {
                if (!m.containsKey(targetId))
                    m.put(targetId, false);
            }
        }
        L.traceEndCall(ctx, METHOD);
        return m;
    }

    public RecordSet getFavoriteSummary(Context ctx, String user_id, String target_types) {
        final String METHOD = "getFavoriteSummary";
        L.traceStartCall(ctx, METHOD, user_id, target_types);
        String sql = "select target_id,target_type from " + favoriteTable + " where user_id='" + user_id + "'";
        if (!target_types.equals(""))
            sql += " and target_type in (" + target_types + ")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        RecordSet out_recs = new RecordSet();
        List<String> type_list = StringUtils2.splitList(target_types, ",", true);
        if (type_list.size() <= 0) {
            String sql0 = "select distinct(target_type) from " + favoriteTable + " where user_id='" + user_id + "'";
            RecordSet recs0 = se.executeRecordSet(sql0, null);
            type_list = StringUtils2.splitList(recs0.joinColumnValues("target_type", ","), ",", true);
        }
        if (type_list.size() > 0) {
            for (String type : type_list) {
                Record out_rec = new Record();
                String target_id_str = "";
                int c = 0;
                for (Record rec : recs) {
                    if (rec.getString("target_type").equals(type)) {
                        target_id_str += rec.getString("target_id") + ",";
                        c += 1;
                    }
                }
                out_rec.put("target_type", type);
                out_rec.put("target_ids", target_id_str);
                out_rec.put("count", c);
                out_recs.add(out_rec);
            }
        }

        return out_recs;
    }

    public String getFavoriteByType(Context ctx, String user_id, String target_type,int page,int count) {
            final String METHOD = "getFavoriteByType";
            L.traceStartCall(ctx, METHOD, user_id, target_type);
            String sql = "select target_id from " + favoriteTable + " where user_id='" + user_id + "' and target_type='"+target_type+"' order by created_time desc  "+SQLUtils.pageToLimit(page, count)+" ";
            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);
            return recs.joinColumnValues("target_id",",");
        }
}
