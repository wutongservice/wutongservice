package com.borqs.server.test.pubapi.test1.contact;

import com.borqs.server.impl.contact.ContactDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.contact.ContactLogic;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.web.AbstractHttpClient;

public class ContactApiTest1 extends ServletTestCase {
    public static final String PUB_API = "servlet.pubApi";

    @Override
    protected String[] getServletBeanIds() {
        return new String[]{PUB_API};
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(ContactDb.class);
    }

    private ContactLogic getContactLogic() {
        return (ContactLogic)getBean("logic.contact");
    }

    public void testUpload() {
        ContactLogic contactImpl = getContactLogic();
        String contacts = "[{\"username\":\"gaorx\", \"type\":2, \"content\":\"grx@serverteam.com\"}, " +
                "{\"username\":\"cg\", \"type\":1, \"content\":\"13900007878\"}]";
        contacts = Encoders.toBase64(contacts);
        
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.jcsTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/contact/upload", new Object[][]{
                {"contacts", contacts}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());

        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);
        Contacts contacts_ = contactImpl.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID);
        assertEquals(2, contacts_.size());
        
        String wp = "[{\"username\":\"wp\", \"type\":2, \"content\":\"wp@serverteam.com\"}]";
        wp = Encoders.toBase64(wp);
        client = newHttpApiClient(UA_EMPTY, ServerTeam.jcsTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
        resp = client.get(PUB_API + "/contact/upload", new Object[][]{
                {"contacts", wp}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
        contacts_ = contactImpl.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID);
        assertEquals(3, contacts_.size());

        client = newHttpApiClient(UA_EMPTY, ServerTeam.jcsTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
        resp = client.get(PUB_API + "/contact/upload", new Object[][]{
                {"contacts", wp},
                {"full", true}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
        contacts_ = contactImpl.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ServerTeam.JCS_ID);
        assertEquals(1, contacts_.size());
    }
}
