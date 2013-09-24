package com.borqs.server.platform.request;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class SimpleRequest extends RequestBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String requestTable = "request";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("like.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("like.simple.db", null);
        this.requestTable = conf.getString("like.simple.requestTable", "request");
    }

    @Override
    public void destroy() {
        this.requestTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected long saveRequest(String userId, String sourceId, String app, String type, String message, String data, String options) {
        final String SQL1 = "SELECT ${alias.request_id} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.source}=${v(source)} AND ${alias.type}=${v(type)} AND ${alias.status}=0";
        final String SQL2 = "INSERT INTO ${table} ${values_join(alias, rec)}";
        final String SQL3 = "UPDATE ${table} SET ${alias.created_time}=${v(created_time)}, ${alias.message}=${v(message)}, ${alias.data}=${v(data)}, ${alias.options}=${v(options)} WHERE ${alias.user}=${v(user)} AND ${alias.source}=${v(source)} AND ${alias.type}=${v(type)} AND ${alias.status}=0";

        String sql = SQLTemplate.merge(SQL1, new Object[][]{
                {"table", requestTable},
                {"alias", requestSchema.getAllAliases()},
                {"user", Long.parseLong(userId)},
                {"source", Long.parseLong(sourceId)},
                {"type", type}});

        SQLExecutor se = getSqlExecutor();
        long requestId = se.executeIntScalar(sql, 0L);
        if (requestId != 0) {
            sql = SQLTemplate.merge(SQL3, new Object[][] {
                    {"table", requestTable},
                    {"alias", requestSchema.getAllAliases()},
                    {"created_time", DateUtils.nowMillis()},
                    {"message", StringUtils.trimToEmpty(message)},
                    {"data", StringUtils.trimToEmpty(data)},
                    {"options", StringUtils.trimToEmpty(options)},
                    {"user", userId},
                    {"source", sourceId},
                    {"type", type},
            });
            se.executeUpdate(sql);
            return requestId;
        } else {
            requestId = RandomUtils.generateId();

            Record rec = new Record();
            rec.put("request_id", requestId);
            rec.put("user", Long.parseLong(userId));
            rec.put("source", Long.parseLong(sourceId));
            rec.put("app", Integer.parseInt(app));
            rec.put("type", StringUtils.trimToEmpty(type));
            rec.put("created_time", DateUtils.nowMillis());
            rec.put("done_time", 0L);
            rec.put("status", 0);
            rec.put("message", StringUtils.trimToEmpty(message));
            rec.put("data", StringUtils.trimToEmpty(data));
            rec.put("options", StringUtils.trimToEmpty(options));
            sql = SQLTemplate.merge(SQL2, "table", requestTable, "alias", requestSchema.getAllAliases(), "rec", rec);

            se.executeUpdate(sql);
            return requestId;
        }
    }

    @Override
    protected boolean saveRequests(String userIds, String sourceId, String app, String type, String message, String data, String options) {
        String[] users = StringUtils2.splitArray(userIds, ",", true);
        int size = users.length;
        if (size == 1)
            return saveRequest(userIds, sourceId, app, type, message, data, options) != 0;
        else {
            long[] requestIds = new long[size];
            for (int i = 0; i < size; i++)
                requestIds[i] = RandomUtils.generateId();

            String sql = "INSERT INTO " + requestTable + " (`request_id`, `user`, `source`, `app`, `type`, `created_time`, `done_time`, `status`, `message`, `data`, `options`) VALUES ";
            for (int i = 0; i < size; i++) {
                sql += "(" + requestIds[i] + ", " + users[i] + ", " + sourceId + ", " + app + ", '" + type + "', "
                        + DateUtils.nowMillis() + ", " + 0L + ", " + 0 + ", '" + message + "', '" + data + "', '" + options + "'), ";
            }
            sql = StringUtils.substringBeforeLast(sql, ",");

            SQLExecutor se = getSqlExecutor();
            long n = se.executeUpdate(sql);
            return n > 0;
        }
    }

    @Override
    protected void destroyRequests0(String userId, List<Long> reqIds) {
        final String SQL = "DELETE FROM ${table} WHERE request_id IN ${vjoin(reqids)}";
        if (reqIds.isEmpty())
            return;

        String sql = SQLTemplate.merge(SQL, "table", requestTable, "reqids", reqIds);
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql);
    }

    @Override
    protected RecordSet getRequests0(String userId, String appId, String type) {
        final String SQL = "SELECT ${alias.request_id}, ${alias.source}, ${alias.app}, ${alias.type}, ${alias.created_time}, ${alias.message}, ${alias.data}" +
                " FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.status}<>1";

        String sql = SQLTemplate.merge(SQL, "table", requestTable, "alias", requestSchema.getAllAliases(), "user", Long.parseLong(userId));
        if (StringUtils.isNotBlank(appId))
            sql += " AND app=" + appId;
        if (StringUtils.isNotBlank(type)) {
//            sql += " AND type=" + SQLUtils.toSql(type);
            String[] types = StringUtils2.splitArray(type, ",", true);
            sql += " AND type IN (" + SQLUtils.valueJoin(",", types) + ")";
        }
        sql += " ORDER BY created_time DESC";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(requestSchema, recs);
        return recs;
    }

    @Override
    protected String getRelatedRequestIds0(String sourceIds, String datas) {
        String sql = "SELECT request_id FROM " + requestTable + " WHERE data REGEXP '" + datas + "'";

        if (StringUtils.isNotBlank(sourceIds) && !StringUtils.equals(sourceIds, "0"))
             sql += " AND source IN (" + sourceIds + ")";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        List<Long> requestIds = recs.getIntColumnValues("request_id");

        return StringUtils2.joinIgnoreBlank(",", requestIds);
    }

    @Override
    protected boolean doneRequest0(String userId, List<Long> requestIds) {
        if (requestIds.isEmpty())
            return true;

        final String SQL = "UPDATE ${table} SET ${alias.status}=1, ${alias.done_time}=${v(now)} WHERE ${alias.request_id} IN (${vjoin(reqids)})";
        String sql = SQLTemplate.merge(SQL, "table", requestTable, "alias", requestSchema.getAllAliases(), "now", DateUtils.nowMillis(), "reqids", requestIds);
        SQLExecutor se = getSqlExecutor();
        return se.executeUpdate(sql) > 0;
    }

    @Override
    protected int getCount0(String userId, String app, String type) {
        final String SQL = "SELECT COUNT(*) FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.status}<>1";

        String sql = SQLTemplate.merge(SQL, "table", requestTable, "alias", requestSchema.getAllAliases(), "user", Long.parseLong(userId));
        if (StringUtils.isNotBlank(app))
            sql += " AND app=" + app;
        if (StringUtils.isNotBlank(type))
            sql += " AND type=" + SQLUtils.toSql(type);

        SQLExecutor se = getSqlExecutor();
        return (int) se.executeIntScalar(sql, 0L);
    }

    @Override
    protected String getPeddingRequests0(String source, String user) {
        String types = "";
        final String SQL = "SELECT DISTINCT ${alias.type} FROM ${table} WHERE ${alias.source}=${source} AND ${alias.user}=${user} AND ${alias.status}=0";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", requestTable},
                {"alias", requestSchema.getAllAliases()},
                {"source", source},
                {"user", user}
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(requestSchema, recs);

        for (Record rec : recs) {
            types += rec.getString("type") + ",";
        }

        return StringUtils.substringBeforeLast(types, ",");
    }

    @Override
    protected RecordSet getPeddingRequests1(String source, String userIds) {
        final String sql = "SELECT DISTINCT(type),user FROM request WHERE source="+ source +" AND user in ("+ userIds +") AND status=0";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        RecordSet out0 = new RecordSet();
        List<String> userl = StringUtils2.splitList(toStr(userIds), ",", true);
        for (String u : userl) {
            if (recs.size() > 0) {
                String types = "";
                for (Record rec : recs) {
                    if (rec.getString("user").equals(u)) {
                        types += rec.getString("type") + ",";
                    }
                }
                out0.add(Record.of("user",u,"penddingRequest",types));
            }

        }
        return out0;
    }
}
