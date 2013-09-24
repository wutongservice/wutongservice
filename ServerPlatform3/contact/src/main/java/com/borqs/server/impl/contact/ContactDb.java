package com.borqs.server.impl.contact;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ObjectHolder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class ContactDb extends SqlSupport {
    private Table contactTable0;
    private Table contactTable1;

    public Table getContactTable0() {
        return contactTable0;
    }

    public void setContactTable0(Table contactTable0) {
        this.contactTable0 = contactTable0;
    }

    public Table getContactTable1() {
        return contactTable1;
    }

    public void setContactTable1(Table contactTable1) {
        this.contactTable1 = contactTable1;
    }

    public ContactDb() {
    }

    private ShardResult shardContact(long userId) {
        return contactTable0.shard(userId);
    }

    private ShardResult shardContact(String content) {
        return contactTable1.shard(content);
    }

    private LinkedHashMap<String, ArrayList<Contact>> getContentContactsMap(Contact... contacts) {
        LinkedHashMap<String, ArrayList<Contact>> m = new LinkedHashMap<String, ArrayList<Contact>>();

        for(Contact contact : contacts) {
            String content = contact.getContent();
            ArrayList<Contact> l = new ArrayList<Contact>();
            if (m.containsKey(content)) {
                l = m.get(content);
            }
            l.add(contact);
            m.put(content, l);
        }

        return m;
    }

    private LinkedHashMap<Long, ArrayList<Contact>> getOwnerContactsMap(Contact... contacts) {
        LinkedHashMap<Long, ArrayList<Contact>> m = new LinkedHashMap<Long, ArrayList<Contact>>();

        for(Contact contact : contacts) {
            Long owner = contact.getOwner();
            ArrayList<Contact> l = new ArrayList<Contact>();
            if (m.containsKey(owner)) {
                l = m.get(owner);
            }
            l.add(contact);
            m.put(owner, l);
        }

        return m;
    }

    public void create(final Context ctx, final Contact... contacts) {
        LinkedHashMap<String, ArrayList<Contact>> m = getContentContactsMap(contacts);
        
        final long userId = ctx.getViewer();
        final ShardResult contactSR0 = shardContact(userId);
        Contacts contacts_ = new Contacts();
        Collections.addAll(contacts_, contacts);
        final GroupedShardResults groupedPropSR = TableHelper.shard(contactTable1, CollectionsHelper.asSet(contacts_.getContents()));

        for (final ShardResult contactSR1 : groupedPropSR.getShardResults()) {
            List<String> contentsInShard = groupedPropSR.get(contactSR1);
            ArrayList<Contact> contactsInShardLst = new ArrayList<Contact>();
            for (String content : contentsInShard) {
                contactsInShardLst.addAll(m.get(content));
            }
            final Contact[] contactsInShard = contactsInShardLst.toArray(new Contact[contactsInShardLst.size()]);
            sqlExecutor.openConnections(contactSR0.db, contactSR1.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    List<String> sqls = ContactSql.insertContact(contactSR0.table, contactsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[0], sqls);
                    sqls = ContactSql.insertContact(contactSR1.table, contactsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[1], sqls);
                    return null;
                }
            });
        }
    }

    public void delete(final Context ctx, final Contact... contacts) {
        LinkedHashMap<String, ArrayList<Contact>> m = getContentContactsMap(contacts);

        final long userId = ctx.getViewer();
        final ShardResult contactSR0 = shardContact(userId);
        Contacts contacts_ = new Contacts();
        Collections.addAll(contacts_, contacts);
        final GroupedShardResults groupedPropSR = TableHelper.shard(contactTable1, CollectionsHelper.asSet(contacts_.getContents()));

        for (final ShardResult contactSR1 : groupedPropSR.getShardResults()) {
            List<String> contentsInShard = groupedPropSR.get(contactSR1);
            ArrayList<Contact> contactsInShardLst = new ArrayList<Contact>();
            for (String content : contentsInShard) {
                contactsInShardLst.addAll(m.get(content));
            }
            final Contact[] contactsInShard = contactsInShardLst.toArray(new Contact[contactsInShardLst.size()]);
            sqlExecutor.openConnections(contactSR0.db, contactSR1.db, new ConnectionsHandler<Object>() {
                @Override
                public Object handle(Connection[] conns) {
                    List<String> sqls = ContactSql.deleteContact(contactSR0.table, contactsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[0], sqls);
                    sqls = ContactSql.deleteContact(contactSR1.table, contactsInShard);
                    SqlExecutor.executeUpdate(ctx, conns[1], sqls);
                    return null;
                }
            });
        }
    }
    
    public Contacts gets(final Context ctx, final int reason, final Contact... contacts) {
        final Contacts contacts_ = new Contacts();
        LinkedHashMap<Long, ArrayList<Contact>> m = getOwnerContactsMap(contacts);

        final GroupedShardResults groupedPropSR = TableHelper.shard(contactTable0, m.keySet());

        for (final ShardResult contactSR0 : groupedPropSR.getShardResults()) {
            List<Long> ownerIdsInShard = groupedPropSR.get(contactSR0);
            ArrayList<Contact> contactsInShardLst = new ArrayList<Contact>();
            for (Long ownerId : ownerIdsInShard) {
                contactsInShardLst.addAll(m.get(ownerId));
            }
            final Contact[] contactsInShard = contactsInShardLst.toArray(new Contact[contactsInShardLst.size()]);
            sqlExecutor.openConnection(contactSR0.db, new SingleConnectionHandler<Object>() {
                @Override
                protected Object handleConnection(Connection conn) {
                    String sql = ContactSql.getContacts(contactSR0.table, reason, contactsInShard);
                    SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                        @Override
                        public void handle(ResultSet rs) throws SQLException {
                            contacts_.addAll(ContactRs.read(rs));
                        }
                    });
                    return null;
                }
            });
        }

        return contacts_;
    }

    public Contacts gets(final Context ctx, final int reason, final long userId) {
        final ShardResult contactSR = shardContact(userId);
        final ObjectHolder<Contacts> r = new ObjectHolder<Contacts>();
        sqlExecutor.openConnection(contactSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = ContactSql.getContacts(contactSR.table, reason, userId);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        r.value = ContactRs.read(rs);
                    }
                });
                return null;
            }
        });
        return r.value;
    }

    public Contacts search(final Context ctx, final int reason, final String content) {
        final ShardResult contactSR = shardContact(content);
        final ObjectHolder<Contacts> r = new ObjectHolder<Contacts>();
        sqlExecutor.openConnection(contactSR.db, new SingleConnectionHandler<Object>() {
            @Override
            protected Object handleConnection(Connection conn) {
                String sql = ContactSql.searchContacts(contactSR.table, reason, content);
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        r.value = ContactRs.read(rs);
                    }
                });
                return null;
            }
        });
        return r.value;
    }
}
