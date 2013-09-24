package com.borqs.server.test.pubapi.test1.privacy;

import com.borqs.server.impl.privacy.PrivacyDb;
import com.borqs.server.platform.feature.privacy.PrivacyPolicies;
import com.borqs.server.platform.feature.privacy.PrivacyResources;
import com.borqs.server.platform.feature.privacy.PrivacyTarget;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.util.json.JsonCompare;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.AbstractHttpClient;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

public class PrivacyApiTest1 extends ServletTestCase {
    public static final String PUB_API = "servlet.pubApi";

    @Override
    protected String[] getServletBeanIds() {
        return new String[]{PUB_API};
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(PrivacyDb.class);
    }

    public void testSetGet() {
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.grxTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/privacy/set/vcard", new Object[][]{
                {"allow", "all"},
                {"deny", ServerTeam.JCS_ID}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());

        resp = client.get(PUB_API + "/privacy/get/vcard", new Object[][]{
                {"user_id", ServerTeam.GRX_ID}
        });
        
        ArrayNode arr = (ArrayNode)resp.getJsonNode();
        for(int i = 0; i < arr.size(); i++) {
            JsonNode jn = arr.get(i);
            if(jn.get("target").get("scope").getIntValue() == PrivacyTarget.SCOPE_ALL)
            {
                assertEquals(jn.get("allow").getBooleanValue(), true);
            }
            if(jn.get("target").get("scope").getIntValue() == PrivacyTarget.SCOPE_USER)
            {
                assertEquals(jn.get("target").get("id").getTextValue(), String.valueOf(ServerTeam.JCS_ID));
                assertEquals(jn.get("allow").getBooleanValue(), false);
            }
        }
    }

    public void testClear() {
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.grxTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/privacy/set/vcard", new Object[][]{
                {"allow", "all"},
                {"deny", ServerTeam.JCS_ID}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());

        resp = client.get(PUB_API + "/privacy/clear/vcard", new Object[][]{});
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());

        resp = client.get(PUB_API + "/privacy/get/vcard", new Object[][]{
                {"user_id", ServerTeam.GRX_ID}
        });

        ArrayNode arr = (ArrayNode)resp.getJsonNode();
        assertEquals(arr.size(), 1);
        JsonNode jn = arr.get(0);
        JsonNode def = JsonHelper.parse(JsonHelper.toJson(PrivacyPolicies.getDefault(PrivacyResources.RES_VCARD), false));
        assertTrue(JsonCompare.compare(jn, def).isEquals());
    }

    public void testGetAllowIds() {
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.grxTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/privacy/set/vcard", new Object[][]{
                {"allow", "all"},
                {"deny", ServerTeam.JCS_ID}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());

        resp = client.get(PUB_API + "/privacy/allow/vcard", new Object[][]{
                {"user_id", ServerTeam.GRX_ID}
        });
        
        String result = resp.getJsonNode().get("result").getTextValue();
        assertTrue(StringUtils.startsWith(result, "-"));
        assertTrue(StringUtils.contains(result, String.valueOf(ServerTeam.JCS_ID)));
    }

    public void testMutual() {
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.grxTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/privacy/mutual/vcard", new Object[][]{
                {"user_id", ServerTeam.JCS_ID}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());

        resp = client.get(PUB_API + "/privacy/get/vcard", new Object[][]{
                {"user_id", ServerTeam.GRX_ID}
        });
        JsonNode jn = ((ArrayNode)resp.getJsonNode()).get(0);
        assertEquals(jn.get("target").get("id").getTextValue(), String.valueOf(ServerTeam.JCS_ID));
        assertEquals(jn.get("allow").getBooleanValue(), true);

        resp = client.get(PUB_API + "/privacy/get/vcard", new Object[][]{
                {"user_id", ServerTeam.JCS_ID}
        });
        jn = ((ArrayNode)resp.getJsonNode()).get(0);
        assertEquals(jn.get("target").get("id").getTextValue(), String.valueOf(ServerTeam.GRX_ID));
        assertEquals(jn.get("allow").getBooleanValue(), true);
    }
}
