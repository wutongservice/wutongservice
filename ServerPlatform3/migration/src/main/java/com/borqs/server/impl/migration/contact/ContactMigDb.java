package com.borqs.server.impl.migration.contact;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContactMigDb extends SqlSupport {
    private static final Logger L = Logger.get(ContactMigDb.class);

    private Map<Long, String> userIdMap;

    // table
    private Table socialContactTable;
    private Table virtualFriendTable;

    public ContactMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }


    public Table getSocialContactTable() {
        return socialContactTable;
    }

    public void setSocialContactTable(Table socialContactTable) {
        this.socialContactTable = socialContactTable;
    }

    public Table getVirtualFriendTable() {
        return virtualFriendTable;
    }

    public void setVirtualFriendTable(Table virtualFriendTable) {
        this.virtualFriendTable = virtualFriendTable;
    }

    private ShardResult shardSocialContact() {
        return socialContactTable.getShard(0);
    }

    private ShardResult shardVirtualContact() {
        return virtualFriendTable.getShard(0);
    }

    public List<Contact> getContact(final Context ctx) {
        final ShardResult virctualFriendContactSR = shardVirtualContact();
        final ShardResult socialContactSR = shardSocialContact();

        return sqlExecutor.openConnection(socialContactSR.db, new SingleConnectionHandler<List<Contact>>() {
            @Override
            protected List<Contact> handleConnection(Connection conn) {

                final List<Contact> contactList1 = new ArrayList<Contact>();
                String sql = ContactMigSql.getContacts1(ctx, socialContactSR.table);
                SqlExecutor.executeList(ctx, conn, sql, contactList1, new ResultSetReader<Contact>() {
                    @Override
                    public Contact read(ResultSet rs, Contact reuse) throws SQLException {
                        return ContactMigRs.readSocialContact(rs, null, userIdMap);
                    }
                });


                String sql2 = ContactMigSql.getContacts2(ctx, virctualFriendContactSR.table);
                SqlExecutor.executeList(ctx, conn, sql2, contactList1, new ResultSetReader<Contact>() {
                    @Override
                    public Contact read(ResultSet rs, Contact reuse) throws SQLException {
                        return ContactMigRs.readVirtualFriendContact(rs, null, userIdMap);
                    }
                });
                return contactList1;
            }
        });
    }

}

