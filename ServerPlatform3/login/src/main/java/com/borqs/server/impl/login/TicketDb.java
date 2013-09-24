package com.borqs.server.impl.login;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;

public class TicketDb extends SqlSupport {
    private Table ticketTable;


    public TicketDb() {
    }

    public Table getTicketTable() {
        return ticketTable;
    }

    public void setTicketTable(Table ticketTable) {
        this.ticketTable = ticketTable;
    }

    private ShardResult shardTicket(String ticket) {
        return ticketTable.shard(ticket);
    }

    public long who(final Context ctx, final String ticket) {
        final ShardResult ticketSR = shardTicket(ticket);
        return sqlExecutor.openConnection(ticketSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = TicketSql.findUser(ticketSR.table, ticket);
                return SqlExecutor.executeInt(ctx, conn, sql, 0L);
            }
        });
    }

    public void createTicket(final Context ctx, final long userId, final String ticket, final int app) {
        final ShardResult ticketSR = shardTicket(ticket);
        sqlExecutor.openConnection(ticketSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = TicketSql.insertTicket(ticketSR.table, userId, ticket, app);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return null;
            }
        });
    }

    public boolean deleteTicket(final Context ctx, final String ticket) {
        final ShardResult ticketSR = shardTicket(ticket);
        return sqlExecutor.openConnection(ticketSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = TicketSql.deleteTicket(ticketSR.table, ticket);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n > 0;
            }
        });
    }
}
