package com.borqs.server.test.pubapi.test1.psuggest;

import com.borqs.server.impl.psuggest.PeopleSuggestDb;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.psuggest.Status;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.json.JsonCompare;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.AbstractHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

public class PeopleSuggestApiTest1 extends ServletTestCase {
    public static final String PUB_API = "servlet.pubApi";

    @Override
    protected String[] getServletBeanIds() {
        return new String[]{PUB_API};
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(PeopleSuggestDb.class);
    }

    public void testCreateGet() {
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.jcsTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/suggest/recommend", new Object[][]{
                {"touser", ServerTeam.CG_ID},
                {"suggestedusers", String.valueOf(ServerTeam.WP_ID)}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
        resp = client.get(PUB_API + "/suggest/get", new Object[][]{
                {"user_id", ServerTeam.CG_ID}
        });
        ArrayNode arrNode = (ArrayNode)resp.getJsonNode();
        assertEquals(arrNode.size(), 1);
        assertTrue(JsonCompare.compare(arrNode.get(0).get("suggested"), JsonHelper.parse(
                JsonHelper.toJson(PeopleId.fromId(ServerTeam.WP_ID), false))).isEquals());

        client = newHttpApiClient(UA_EMPTY, ServerTeam.grxTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        resp = client.get(PUB_API + "/psuggest/create", new Object[][]{
                {"to", ServerTeam.CG_ID},
                {"suggested", StringHelper.join(new long[]{ServerTeam.WP_ID, ServerTeam.GRX_ID, ServerTeam.JCS_ID}, ",")}
        });
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
        resp = client.get(PUB_API + "/psuggest/get", new Object[][]{
                {"user_id", ServerTeam.CG_ID}
        });
        arrNode = (ArrayNode)resp.getJsonNode();
        assertEquals(arrNode.size(), 2);
        for (JsonNode node : arrNode) {
            if (JsonCompare.compare(node.get("suggested"), JsonHelper.parse(
                    JsonHelper.toJson(PeopleId.fromId(ServerTeam.WP_ID), false))).isEquals()) {
                assertEquals(node.get("source").getTextValue(), StringHelper.join(
                        new long[]{ServerTeam.JCS_ID, ServerTeam.GRX_ID}, ","));
            }
        }
    }

    public void testAcceptRejectGet() {
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.jcsTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
        client.get(PUB_API + "/suggest/recommend", new Object[][]{
                {"touser", ServerTeam.CG_ID},
                {"suggestedusers", String.valueOf(ServerTeam.WP_ID)}
        });
        client = newHttpApiClient(UA_EMPTY, ServerTeam.grxTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        client.get(PUB_API + "/psuggest/create", new Object[][]{
                {"to", ServerTeam.CG_ID},
                {"suggested", StringHelper.join(new long[]{ServerTeam.WP_ID, ServerTeam.GRX_ID, ServerTeam.JCS_ID}, ",")}
        });

        client = newHttpApiClient(UA_EMPTY, ServerTeam.cgTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
        client.get(PUB_API + "/psuggest/accept", new Object[][]{
                {"suggested", ServerTeam.GRX_ID},
                {"circles", StringHelper.join(new int[]{Circle.CIRCLE_DEFAULT, Circle.CIRCLE_FAMILY}, ",")}
        });
        client.get(PUB_API + "/psuggest/reject", new Object[][]{
                {"suggested", ServerTeam.WP_ID}
        });

        AbstractHttpClient.Response resp = client.get(PUB_API + "/psuggest/get", new Object[][]{
                {"status", Status.ACCEPTED}
        });
        ArrayNode arrNode = (ArrayNode)resp.getJsonNode();
        assertEquals(arrNode.size(), 1);
        assertTrue(JsonCompare.compare(arrNode.get(0).get("suggested"), JsonHelper.parse(
                JsonHelper.toJson(PeopleId.fromId(ServerTeam.GRX_ID), false))).isEquals());
        resp = client.get(PUB_API + "/psuggest/get", new Object[][]{
                {"status", Status.REJECTED}
        });
        arrNode = (ArrayNode)resp.getJsonNode();
        assertEquals(arrNode.size(), 1);
        assertTrue(JsonCompare.compare(arrNode.get(0).get("suggested"), JsonHelper.parse(
                JsonHelper.toJson(PeopleId.fromId(ServerTeam.WP_ID), false))).isEquals());
    }
}
