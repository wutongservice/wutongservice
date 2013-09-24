package com.borqs.server.impl.request;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.feature.request.Requests;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.ArrayUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RequestDb extends SqlSupport {
    private Table requestTable;
    private Table requestIndex;

    public Table getRequestTable() {
        return requestTable;
    }

    public void setRequestTable(Table requestTable) {
        this.requestTable = requestTable;
    }

    public Table getRequestIndex() {
        return requestIndex;
    }

    public void setRequestIndex(Table requestIndex) {
        this.requestIndex = requestIndex;
    }

    public RequestDb() {
    }

    private ShardResult shardRequest(long to) {
        return requestTable.shard(to);
    }

    private ShardResult shardRequestIndex(long from) {
        return requestIndex.shard(from);
    }

    private LinkedHashMap<Long, ArrayList<Request>> getUserRequestsMap(boolean isTo, Request... requests) {
        LinkedHashMap<Long, ArrayList<Request>> m = new LinkedHashMap<Long, ArrayList<Request>>();

        for(Request request : requests) {
            Long user = isTo ? request.getTo() : request.getFrom();
            ArrayList<Request> l = new ArrayList<Request>();
            if (m.containsKey(user)) {
                l = m.get(user);
            }
            l.add(request);
            m.put(user, l);
        }

        return m;
    }

    public void create(final Context ctx, final Request... requests) {
        LinkedHashMap<Long, ArrayList<Request>> m = getUserRequestsMap(true, requests);

        final long userId = ctx.getViewer();
        final ShardResult reqIndexSR = shardRequestIndex(userId);
        Requests requests_ = new Requests();
        Collections.addAll(requests_, requests);
        final GroupedShardResults groupedPropSR = TableHelper.shard(requestTable, CollectionsHelper.asSet(requests_.getToIds()));

        for (final ShardResult requestSR : groupedPropSR.getShardResults()) {
            List<Long> toIdsInShard = groupedPropSR.get(requestSR);
            ArrayList<Request> requestsInShardLst = new ArrayList<Request>();
            for (Long toId : toIdsInShard) {
                requestsInShardLst.addAll(m.get(toId));
            }
            final Request[] requestsInShard = requestsInShardLst.toArray(new Request[requestsInShardLst.size()]);
            sqlExecutor.openConnections(requestSR.db, reqIndexSR.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    List<String> sqls = RequestSql.insertRequest(requestSR.table, requestsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[0], sqls);
                    sqls = RequestSql.insertRequestIndex(reqIndexSR.table, requestsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[1], sqls);
                    return null;
                }
            });
        }
    }

    public void done(final Context ctx, final long... requestIds) {
        final long userId = ctx.getViewer();
        final ShardResult requestSR = shardRequest(userId);
        final Requests requests = new Requests();
        sqlExecutor.openConnection(requestSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                List<String> sqls = RequestSql.doneRequest(requestSR.table, false, requestIds);
                SqlExecutor.executeUpdate(ctx, conn, sqls);
                String sql = RequestSql.getRequests(requestSR.table, requestIds);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        requests.addAll(RequestRs.read(rs));
                    }
                });
                return null;
            }
        });

        LinkedHashMap<Long, ArrayList<Request>> m = getUserRequestsMap(false, requests.toArray(new Request[requests.size()]));
        final GroupedShardResults groupedPropSR = TableHelper.shard(requestIndex, CollectionsHelper.asSet(requests.getFromIds()));

        for (final ShardResult reqIndexSR : groupedPropSR.getShardResults()) {
            List<Long> fromIdsInShard = groupedPropSR.get(reqIndexSR);
            ArrayList<Request> requestsInShardLst = new ArrayList<Request>();
            for (Long fromId : fromIdsInShard) {
                requestsInShardLst.addAll(m.get(fromId));
            }
            final Request[] requestsInShard = requestsInShardLst.toArray(new Request[requestsInShardLst.size()]);
            long[] requestIdsInShard = new long[] {};
            for (Request req : requestsInShard)
                ArrayUtils.add(requestIdsInShard, req.getRequestId());
            sqlExecutor.openConnection(requestSR.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    List<String> sqls = RequestSql.doneRequest(reqIndexSR.table, true, requestIds);
                    SqlExecutor.executeUpdate(ctx, conn, sqls);
                    return null;
                }
            });
        }
    }

    public Requests gets(final Context ctx, final int status, final long toId, final int app, final int type, final int limit) {
        final Requests requests = new Requests();
        final ShardResult requestSR = shardRequest(toId);
        sqlExecutor.openConnection(requestSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = RequestSql.getRequests(requestSR.table, status, toId, app, type, limit);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        requests.addAll(RequestRs.read(rs));
                    }
                });
                return null;
            }
        });

        return requests;
    }

    public long getPendingCount(final Context ctx, final long toId, final int app, final int type) {
        final ShardResult requestSR = shardRequest(toId);
        return sqlExecutor.openConnection(requestSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = RequestSql.getPendingCount(requestSR.table, toId, app, type);
                return SqlExecutor.executeInt(ctx, conn, sql, 0L);
            }
        });
    }
    
    public Map<Long, int[]> getPendingTypes(final Context ctx, final long fromId, final long... toIds) {
        final LinkedHashMap<Long, int[]> m = new LinkedHashMap<Long, int[]>();
        int[] types = new int[] {};
        for (Long to : toIds)
            m.put(to, types);

        final ShardResult reqIndexSR = shardRequestIndex(fromId);
        sqlExecutor.openConnection(reqIndexSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = RequestSql.getPendingTypes(reqIndexSR.table, fromId, toIds);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        while (rs.next()) {
                            Long to = rs.getLong("to");
                            m.put(to, ArrayUtils.add(m.get(to), rs.getInt("type")));
                        }
                    }
                });
                return null;
            }
        });

        return m;
    }
}
