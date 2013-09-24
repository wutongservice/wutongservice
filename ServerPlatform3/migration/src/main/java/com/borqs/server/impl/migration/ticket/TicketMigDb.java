package com.borqs.server.impl.migration.ticket;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TicketMigDb extends SqlSupport {
    private static final Logger L = Logger.get(TicketMigDb.class);

    private Map<Long, String> userIdMap;

    // table
    private Table ticketTable;

    public TicketMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }



    public Table getTicketTable() {
        return ticketTable;
    }

    public void setTicketTable(Table ticketTable) {
        this.ticketTable = ticketTable;
    }

    private ShardResult shardTicket() {
        return ticketTable.getShard(0);
    }

    public List<MigrationTicket> getTicket(final Context ctx) {
        final ShardResult ticketTableSR = shardTicket();

        return sqlExecutor.openConnection(ticketTableSR.db, new SingleConnectionHandler<List<MigrationTicket>>() {
            @Override
            protected List<MigrationTicket> handleConnection(Connection conn) {

                final List<MigrationTicket> ticketList = new ArrayList<MigrationTicket>();
                String sql = TicketMigSql.getTickets(ctx, ticketTableSR.table);
                SqlExecutor.executeList(ctx, conn, sql, ticketList, new ResultSetReader<MigrationTicket>() {
                    @Override
                    public MigrationTicket read(ResultSet rs, MigrationTicket reuse) throws SQLException {
                        return TicketMigRs.readTicket(rs, null, userIdMap);
                    }
                });
                return ticketList;
            }
        });
    }

}

