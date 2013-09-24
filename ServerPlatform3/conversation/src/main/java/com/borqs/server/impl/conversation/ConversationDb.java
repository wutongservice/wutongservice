package com.borqs.server.impl.conversation;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ObjectHolder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ConversationDb extends SqlSupport {
    private Table convTable0;
    private Table convTable1;

    public Table getConvTable0() {
        return convTable0;
    }

    public void setConvTable0(Table convTable0) {
        this.convTable0 = convTable0;
    }

    public Table getConvTable1() {
        return convTable1;
    }

    public void setConvTable1(Table convTable1) {
        this.convTable1 = convTable1;
    }

    public ConversationDb() {

    }

    private ShardResult shardConversation(Target target) {
        return convTable0.shard(target);
    }

    private ShardResult shardConversation(long userId) {
        return convTable1.shard(userId);
    }

    private LinkedHashMap<Target, ArrayList<Conversation>> getTargetConvsMap(Conversation... conversations) {
        LinkedHashMap<Target, ArrayList<Conversation>> m = new LinkedHashMap<Target, ArrayList<Conversation>>();

        for(Conversation conversation : conversations) {
            Target target = conversation.getTarget();
            ArrayList<Conversation> l = new ArrayList<Conversation>();
            if (m.containsKey(target)) {
                l = m.get(target);
            }
            l.add(conversation);
            m.put(target, l);
        }

        return m;
    }

    public void create(final Context ctx, final Conversation... conversations) {
        LinkedHashMap<Target, ArrayList<Conversation>> m = getTargetConvsMap(conversations);
        
        final long userId = ctx.getViewer();
        final ShardResult convSR1 = shardConversation(userId);
        Conversations convs = new Conversations();
        Collections.addAll(convs, conversations);
        final GroupedShardResults groupedPropSR = TableHelper.shard(convTable0, CollectionsHelper.asSet(convs.getTargets()));

        for (final ShardResult convSR0 : groupedPropSR.getShardResults()) {
            List<Target> targetsInShard = groupedPropSR.get(convSR0);
            ArrayList<Conversation> convsInShardLst = new ArrayList<Conversation>();
            for (Target target : targetsInShard) {
                convsInShardLst.addAll(m.get(target));
            }
            final Conversation[] convsInShard = convsInShardLst.toArray(new Conversation[convsInShardLst.size()]);
            sqlExecutor.openConnections(convSR0.db, convSR1.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    List<String> sqls = ConversationSql.insertConversion(convSR0.table, convsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[0], sqls);
                    sqls = ConversationSql.insertConversion(convSR1.table, convsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[1], sqls);
                    return null;
                }
            });
        }
    }

    public void delete(final Context ctx, final Conversation... conversations) {
        LinkedHashMap<Target, ArrayList<Conversation>> m = getTargetConvsMap(conversations);

        final long userId = ctx.getViewer();
        final ShardResult convSR1 = shardConversation(userId);
        Conversations convs = new Conversations();
        Collections.addAll(convs, conversations);
        final GroupedShardResults groupedPropSR = TableHelper.shard(convTable0, CollectionsHelper.asSet(convs.getTargets()));

        for (final ShardResult convSR0 : groupedPropSR.getShardResults()) {
            List<Target> targetsInShard = groupedPropSR.get(convSR0);
            ArrayList<Conversation> convsInShardLst = new ArrayList<Conversation>();
            for (Target target : targetsInShard) {
                convsInShardLst.addAll(m.get(target));
            }
            final Conversation[] convsInShard = convsInShardLst.toArray(new Conversation[convsInShardLst.size()]);
            sqlExecutor.openConnections(convSR0.db, convSR1.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    List<String> sqls = ConversationSql.deleteConversion(convSR0.table, convsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[0], sqls);
                    sqls = ConversationSql.deleteConversion(convSR1.table, convsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[1], sqls);
                    return null;
                }
            });
        }
    }

    public void delete(final Context ctx, final Target... targets) {
        final long userId = ctx.getViewer();
        final ShardResult convSR1 = shardConversation(userId);
        final GroupedShardResults groupedPropSR = TableHelper.shard(convTable0, CollectionsHelper.asSet(targets));

        for (final ShardResult convSR0 : groupedPropSR.getShardResults()) {
            List<Target> targetsInShardLst = groupedPropSR.get(convSR0);
            final Target[] targetsInShard = targetsInShardLst.toArray(new Target[targetsInShardLst.size()]);
            sqlExecutor.openConnections(convSR0.db, convSR1.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    List<String> sqls = ConversationSql.deleteConversion(convSR0.table, targetsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[0], sqls);
                    sqls = ConversationSql.deleteConversion(convSR1.table, targetsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[1], sqls);
                    return null;
                }
            });
        }
    }

    public Conversations findByTarget(final Context ctx, final int[] reasons, final Page page, final Target... targets) {
        final Conversations conversations = new Conversations();
        final GroupedShardResults groupedPropSR = TableHelper.shard(convTable0, CollectionsHelper.asSet(targets));

        for (final ShardResult convSR0 : groupedPropSR.getShardResults()) {
            List<Target> targetsInShardLst = groupedPropSR.get(convSR0);
            final Target[] targetsInShard = targetsInShardLst.toArray(new Target[targetsInShardLst.size()]);
            sqlExecutor.openConnection(convSR0.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = ConversationSql.findByTarget(convSR0.table, reasons, page, targetsInShard);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            conversations.addAll(ConversationRs.read(rs));
                        }
                    });
                    return null;
                }
            });
        }

        return conversations;
    }

    public Conversations findByUser(final Context ctx, final int[] reasons, final int targetType, final Page page, final long... userIds) {
        final Conversations conversations = new Conversations();
        final GroupedShardResults groupedPropSR = TableHelper.shard(convTable1, userIds);

        for (final ShardResult convSR1 : groupedPropSR.getShardResults()) {
            final long[] userIdsInShard = CollectionsHelper.toLongArray(groupedPropSR.get(convSR1));
            sqlExecutor.openConnection(convSR1.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = ConversationSql.findByUser(convSR1.table, reasons, targetType, page, userIdsInShard);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            conversations.addAll(ConversationRs.read(rs));
                        }
                    });
                    return null;
                }
            });
        }

        return conversations;
    }

    public Map<Target, Long> getCount(final Context ctx, final int reason, final long userId, final Target... targets) {
        final ObjectHolder<LinkedHashMap<Target, Long>> r = new ObjectHolder<LinkedHashMap<Target, Long>>();
        final Conversations conversations = new Conversations();
        final GroupedShardResults groupedPropSR = TableHelper.shard(convTable0, CollectionsHelper.asSet(targets));

        for (final ShardResult convSR0 : groupedPropSR.getShardResults()) {
            List<Target> targetsInShardLst = groupedPropSR.get(convSR0);
            final Target[] targetsInShard = targetsInShardLst.toArray(new Target[targetsInShardLst.size()]);
            sqlExecutor.openConnection(convSR0.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = ConversationSql.getCount(convSR0.table, reason, userId, targetsInShard);
                    if (userId > 0) {
                        LinkedHashMap<Target, Long> m = new LinkedHashMap<Target, Long>();
                        m.put(targets[0], SqlExecutor.executeInt(ctx, conn, sql, 0L));
                        r.value = m;
                    }
                    else
                        SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                            @Override
                            public void handle(ResultSet rs) throws SQLException {
                                conversations.addAll(ConversationRs.read(rs));
                                Map<Target, long[]> map = conversations.getGroupedUsers();
                                LinkedHashMap<Target, Long> m = new LinkedHashMap<Target, Long>();
                                for (Map.Entry<Target, long[]> entry : map.entrySet()) {
                                    m.put(entry.getKey(), (long)entry.getValue().length);
                                }
                                r.value = m;
                            }
                        });
                    return null;
                }
            });
        }

        return r.value;
    }
}
