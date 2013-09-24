package com.borqs.server.impl.psuggest;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.PeopleSuggests;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeopleSuggestDb extends SqlSupport {
    public PeopleSuggestDb() {
    }

    public Table getPsuggestTable() {

        return psuggestTable;
    }

    public void setPsuggestTable(Table psuggestTable) {
        this.psuggestTable = psuggestTable;
    }

    private Table psuggestTable;

    private ShardResult shardPsuggest(long userId) {
        return psuggestTable.shard(userId);
    }

    public void create(final Context ctx, final PeopleSuggest... suggests) {
        final long userId = ctx.getViewer();
        final ShardResult psuggestSR = shardPsuggest(userId);
        sqlExecutor.openConnection(psuggestSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                List<Sql.Entry> entries = PeopleSuggestSql.insertPeopleSuggest(psuggestSR.table, suggests);
                SqlExecutor.executeUpdate(ctx, conn, entries);

                ArrayList<PeopleSuggest> needUpdate = new ArrayList<PeopleSuggest>();
                for (Sql.Entry entry : entries) {
                    PeopleSuggest suggest = (PeopleSuggest)entry.tag;
                    if (entry.count < 1)
                        needUpdate.add(suggest);
                }

                if (needUpdate.size() > 0) {
                    final PeopleSuggests origSuggests = new PeopleSuggests();
                    PeopleSuggest[] needUpdateSuggests = needUpdate.toArray(new PeopleSuggest[needUpdate.size()]);
                    String sql = PeopleSuggestSql.getPeopleSuggest(psuggestSR.table, needUpdateSuggests);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            origSuggests.addAll(PeopleSuggestRs.read(rs));
                        }
                    });

                    Map<PeopleId, Map<Integer, long[]>> original = origSuggests.getGroupedSources();
                    List<String> sqls = PeopleSuggestSql.updateSource(psuggestSR.table, original, needUpdateSuggests);
                    SqlExecutor.executeUpdate(ctx, conn, sqls);
                }
                return null;
            }
        });
    }
    
    public PeopleSuggests gets(final Context ctx, final long user, final int reason, final int status, final long limit) {
        final PeopleSuggests suggests = new PeopleSuggests();
        final ShardResult psuggestSR = shardPsuggest(user);
        sqlExecutor.openConnection(psuggestSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = PeopleSuggestSql.getPeopleSuggest(psuggestSR.table, user, reason, status, limit);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        suggests.addAll(PeopleSuggestRs.read(rs));
                    }
                });
                return null;    
            }
        });
            
        return suggests;
    }

    public void deal(final Context ctx, final int status, final long dealTime, final PeopleId... friendIds) {
        final long userId = ctx.getViewer();
        final ShardResult psuggestSR = shardPsuggest(userId);
        sqlExecutor.openConnection(psuggestSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                List<String> sqls = PeopleSuggestSql.updateStatus(psuggestSR.table, userId, status, dealTime, friendIds);
                SqlExecutor.executeUpdate(ctx, conn, sqls);
                return null;
            }
        });
    }

    public PeopleSuggests getPeopleSource(final Context ctx, final long user,final long id, final int reason, final int status, final long limit) {
            final PeopleSuggests suggests = new PeopleSuggests();
            final ShardResult psuggestSR = shardPsuggest(user);
            sqlExecutor.openConnection(psuggestSR.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = PeopleSuggestSql.getPeopleSuggest(psuggestSR.table, user,id, reason, status, limit);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            suggests.addAll(PeopleSuggestRs.read(rs));
                        }
                    });
                    return null;
                }
            });

            return suggests;
        }
}
