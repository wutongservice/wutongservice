package com.borqs.server.impl.migration.contact;


import com.borqs.server.impl.contact.ContactDb;
import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ContactMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(ContactMigImpl.class);

    private final ContactMigDb db_migration_virtualFriend = new ContactMigDb();

    private final ContactDb dbNewContact = new ContactDb();

    private AccountMigImpl account;


    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        dbNewContact.setSqlExecutor(sqlExecutor);
        db_migration_virtualFriend.setSqlExecutor(sqlExecutor);

    }

    public void setNewContactTable0(Table newSettingTable0) {
        dbNewContact.setContactTable0(newSettingTable0);
    }

    public void setNewContactTable1(Table newSettingTable1) {
        dbNewContact.setContactTable1(newSettingTable1);
    }


    public void setOldVirtualFriednTable(Table virtualFriednTable) {
        db_migration_virtualFriend.setVirtualFriendTable(virtualFriednTable);
    }

    public void setOldSocialConatctTable(Table socialConatctTable) {
        db_migration_virtualFriend.setSocialContactTable(socialConatctTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("contact.mig")) {
            contactMigration(Context.create());
        }
    }

    public void contactMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, ContactMigImpl.class, "settingMigration", ctx);

        List<Contact> contacts = null;

        try {
            db_migration_virtualFriend.setUserIdMap(getAllUserIdMap(ctx));

            contacts = db_migration_virtualFriend.getContact(ctx);
            List<Contact> result = new ArrayList<Contact>();
            for (Contact c : contacts) {
                if (c != null)
                    result.add(c);
            }

            Contact[] c = new Contact[result.size()];
            result.toArray(c);

            try {
                dbNewContact.create(ctx, c);

            } catch (RuntimeException e) {
                LC.endCall();
                throw e;
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
