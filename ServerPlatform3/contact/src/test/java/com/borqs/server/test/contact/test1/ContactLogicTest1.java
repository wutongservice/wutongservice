package com.borqs.server.test.contact.test1;

import com.borqs.server.impl.contact.ContactDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.contact.*;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.TestAccount;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.util.DateHelper;

public class ContactLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(ContactDb.class);
    }

    private ContactLogic getConversationLogic() {
        return (ContactLogic)getBean("logic.contact");
    }

    private TestAccount getAccountLogic() {
        return (TestAccount)getBean("logic.account");
    }

    public void testUpdateGet() {
        ContactLogic contactImpl = getConversationLogic();
        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);

        Contact contact0 = new Contact(ServerTeam.JCS_ID, "高荣欣", ContentTypes.CONTACT_CONTENT_EMAIL,
                "grx@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        Contact contact1 = new Contact(ServerTeam.JCS_ID, "高荣欣", ContentTypes.CONTACT_CONTENT_EMAIL,
                "grx@serverteam.com", Reasons.CONTACTS_FRIEND, DateHelper.nowMillis());
        Contact contact2 = new Contact(ServerTeam.JCS_ID, "陈果", ContentTypes.CONTACT_CONTENT_TEL,
                "13900007878", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());

        contactImpl.mergeUpdate(ctx, contact0, contact1, contact2);

        Contact contact = contactImpl.getContact(ctx, Reasons.CONTACTS_FRIEND, contact1.getId());
        assertEquals(contact1.getName(), contact.getName());
        assertEquals(contact1.getContent(), contact.getContent());
        Contacts contacts = contactImpl.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID);
        assertEquals(2, contacts.size());

        Contact contact3 = new Contact(ServerTeam.JCS_ID, "王鹏", ContentTypes.CONTACT_CONTENT_EMAIL,
                "wp@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        contactImpl.mergeUpdate(ctx, contact3);
        contacts = contactImpl.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID);
        assertEquals(3, contacts.size());

        contactImpl.fullUpdate(ctx, contact3);
        contacts = contactImpl.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID);
        assertEquals(1, contacts.size());
    }

    public void testDestroy() {
        ContactLogic contactImpl = getConversationLogic();
        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);

        Contact contact0 = new Contact(ServerTeam.JCS_ID, "高荣欣", ContentTypes.CONTACT_CONTENT_EMAIL,
                "grx@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        Contact contact1 = new Contact(ServerTeam.JCS_ID, "高荣欣", ContentTypes.CONTACT_CONTENT_EMAIL,
                "grx@serverteam.com", Reasons.CONTACTS_FRIEND, DateHelper.nowMillis());
        Contact contact2 = new Contact(ServerTeam.JCS_ID, "陈果", ContentTypes.CONTACT_CONTENT_TEL,
                "13900007878", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        Contact contact3 = new Contact(ServerTeam.JCS_ID, "王鹏", ContentTypes.CONTACT_CONTENT_EMAIL,
                "wp@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        contactImpl.mergeUpdate(ctx, contact0, contact1, contact2, contact3);
        Contacts contacts = contactImpl.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID);
        assertEquals(3, contacts.size());

        contactImpl.destroy(ctx, contact0, contact3);
        contacts = contactImpl.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID);
        assertEquals(1, contacts.size());
    }

    public void testGetCommon() {
        ContactLogic contactImpl = getConversationLogic();
        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);

        Contact contact0 = new Contact(ServerTeam.JCS_ID, "高荣欣", ContentTypes.CONTACT_CONTENT_EMAIL,
                "grx@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        Contact contact1 = new Contact(ServerTeam.JCS_ID, "陈果", ContentTypes.CONTACT_CONTENT_TEL,
                "13900007878", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        Contact contact2 = new Contact(ServerTeam.JCS_ID, "王鹏", ContentTypes.CONTACT_CONTENT_EMAIL,
                "wp@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        contactImpl.mergeUpdate(ctx, contact0, contact1, contact2);

        ctx = Context.createForViewer(ServerTeam.CG_ID);

        Contact contact3 = new Contact(ServerTeam.CG_ID, "高荣欣", ContentTypes.CONTACT_CONTENT_EMAIL,
                "grx@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        Contact contact4 = new Contact(ServerTeam.CG_ID, "刘华东", ContentTypes.CONTACT_CONTENT_TEL,
                "13900002222", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        Contact contact5 = new Contact(ServerTeam.CG_ID, "王鹏", ContentTypes.CONTACT_CONTENT_EMAIL,
                "wp@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        contactImpl.mergeUpdate(ctx, contact3, contact4, contact5);

        Contacts contacts = contactImpl.getCommonContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID, ServerTeam.CG_ID);
        assertEquals(2, contacts.size());
        for (Contact contact : contacts) {
            if (contact.getName().equals("高荣欣"))
                assertEquals(contact0, contact);
            else
                assertEquals(contact2, contact);
        }
    }
    
    public void testSearch() {
        ContactLogic contactImpl = getConversationLogic();
        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);

        Contact contact0 = new Contact(ServerTeam.JCS_ID, "高荣欣", ContentTypes.CONTACT_CONTENT_EMAIL,
                "grx@serverteam.com", Reasons.UPLOAD_CONTACTS, DateHelper.nowMillis());
        contactImpl.mergeUpdate(ctx, contact0);

        ctx = Context.createForViewer(ServerTeam.CG_ID);
        contact0.setOwner(ServerTeam.CG_ID);
        contactImpl.mergeUpdate(ctx, contact0);
        ctx = Context.createForViewer(ServerTeam.WP_ID);
        contact0.setOwner(ServerTeam.WP_ID);
        contactImpl.mergeUpdate(ctx, contact0);
        
        Contacts contacts = contactImpl.searchContacts(ctx, Reasons.UPLOAD_CONTACTS, "grx@serverteam.com");
        assertEquals(3, contacts.size());
    }
}
