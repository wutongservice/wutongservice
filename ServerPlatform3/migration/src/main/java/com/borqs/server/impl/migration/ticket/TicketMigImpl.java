package com.borqs.server.impl.migration.ticket;


import com.borqs.server.impl.login.TicketDb;
import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.*;

public class TicketMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(TicketMigImpl.class);

    private final TicketMigDb db_migration = new TicketMigDb();
    private final TicketDb dbNewTicket = new TicketDb();

    private AccountMigImpl account;


    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        dbNewTicket.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
    }

    public void setNewTicketTable(Table newTicketTable) {
        dbNewTicket.setTicketTable(newTicketTable);
    }


    public void setOldTicketTable(Table oldTicketTable) {
        db_migration.setTicketTable(oldTicketTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("ticket.mig")) {
            ticketMigration(Context.create());
        }
    }

    public void ticketMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, TicketMigImpl.class, "settingMigration", ctx);

        List<MigrationTicket> ticketList = null;

        try {

            db_migration.setUserIdMap(getAllUserIdMap(ctx));


            ticketList = db_migration.getTicket(ctx);

            for (MigrationTicket ticket : ticketList) {
                try {
                    if (ticket != null) {
                        ctx.setViewer(ticket.getUser());
                        Map<String,String> map = new HashMap<String, String>();
                        dbNewTicket.createTicket(ctx, ticket.getUser(), ticket.getTicket(), ticket.getApp());
                    }
                } catch (RuntimeException e) {
                    LC.endCall();
                    throw e;
                }
            }

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall();
            throw e;
        }
    }


    private Map<Long, String> getAllUserIdMap(Context ctx) {
        return account.getAllUserIdMap(ctx);
    }

}
