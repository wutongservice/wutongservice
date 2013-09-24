package com.borqs.server.impl.contact;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.feature.contact.ContactLogic;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.ParamChecker;

public class ContactImpl implements ContactLogic, Initializable {
    private static final Logger L = Logger.get(ContactImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final ContactDb db = new ContactDb();

    public ContactImpl() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getContactTable0() {
        return db.getContactTable0();
    }

    public void setContactTable0(Table contactTable0) {
        db.setContactTable0(contactTable0);
    }

    public Table getContactTable1() {
        return db.getContactTable1();
    }

    public void setContactTable1(Table contactTable1) {
        db.setContactTable1(contactTable1);
    }

    @Override
    public void init() throws Exception {
        // do nothing
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void destroy(Context ctx, Contact... contacts) {
        final LogCall LC = LogCall.startCall(L, ContactImpl.class, "destory",
                ctx, "contacts", contacts);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("contacts", contacts);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (contacts.length > 0) {
                db.delete(ctx, contacts);
                OpLine.append(ctx, Actions.DESTROY, "", Target.forContacts(new Contacts(contacts).getContactIds()));
            }

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void mergeUpdate(Context ctx, Contact... contacts) {
        final LogCall LC = LogCall.startCall(L, ContactImpl.class, "mergeUpdate",
                ctx, "contacts", contacts);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("contacts", contacts);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (contacts.length > 0) {
                db.create(ctx, contacts);
                OpLine.append(ctx, Actions.UPDATE, "", Target.forContacts(new Contacts(contacts).getContactIds()));
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void fullUpdate(Context ctx, Contact... contacts) {
        final LogCall LC = LogCall.startCall(L, ContactImpl.class, "fullUpdate",
                ctx, "contacts", contacts);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("contacts", contacts);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (contacts.length > 0) {
                Contacts contacts_ = new Contacts();
                contacts_.addAll(db.gets(ctx, Reasons.UPLOAD_CONTACTS, userId));
                contacts_.addAll(db.gets(ctx, Reasons.CONTACTS_FRIEND, userId));
                Contact[] rms = contacts_.toArray(new Contact[contacts_.size()]);
                db.delete(ctx, rms);
                db.create(ctx, contacts);
                OpLine.append(ctx, Actions.UPDATE, "", Target.forContacts(new Contacts(contacts).getContactIds()));
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Contact getContact(Context ctx, int reason, String contactId) {
        Contacts contacts = getContacts(ctx, reason, contactId);
        return contacts.isEmpty() ? null : contacts.get(0);
    }

    @Override
    public Contacts getContacts(Context ctx, int reason, String... contactIds) {
        final LogCall LC = LogCall.startCall(L, ContactImpl.class, "getContacts",
                ctx, "reason", reason, "contactIds", contactIds);

        try {
            Contacts contacts = new Contacts();
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("contactIds", contactIds);

            if (contactIds.length == 0)
                return contacts;

            Contact[] contacts_ = new Contact[contactIds.length];
            for (int i = 0; i < contactIds.length; i++) {
                contacts_[i] = Contact.of(contactIds[i]);
            }
            contacts.addAll(db.gets(ctx, reason, contacts_));
            LC.endCall();
            return contacts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Contacts getContacts(Context ctx, int reason, long userId) {
        final LogCall LC = LogCall.startCall(L, ContactImpl.class, "getContacts",
                ctx, "reason", reason, "userId", userId);

        try {
            Contacts contacts = new Contacts();
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId", userId > 0, "Invalid user id");

            contacts.addAll(db.gets(ctx, reason, userId));
            LC.endCall();
            return contacts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Contacts getCommonContacts(Context ctx, int reason, long userId1, long userId2) {
        final LogCall LC = LogCall.startCall(L, ContactImpl.class, "getContacts",
                ctx, "reason", reason, "userId1", userId1, "userId2", userId2);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.mustTrue("userId1", userId1 > 0, "Invalid user id");
            ParamChecker.mustTrue("userId2", userId2 > 0, "Invalid user id");

            Contacts contacts1 = getContacts(ctx, reason, userId1);
            Contacts contacts2 = getContacts(ctx, reason, userId2);

            contacts1.retainAll(contacts2);

            LC.endCall();
            return contacts1;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Contacts searchContacts(Context ctx, int reason, String content) {
        final LogCall LC = LogCall.startCall(L, ContactImpl.class, "searchContacts",
                ctx, "reason", reason, "content", content);

        try {
            Contacts contacts = new Contacts();
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("content", content);

            contacts.addAll(db.search(ctx, reason, content));
            LC.endCall();
            return contacts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }
}
