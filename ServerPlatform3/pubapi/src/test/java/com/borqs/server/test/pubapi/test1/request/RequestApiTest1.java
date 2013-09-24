package com.borqs.server.test.pubapi.test1.request;

import com.borqs.server.impl.request.RequestDb;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;

public class RequestApiTest1 extends ServletTestCase {
    public static final String PUB_API = "servlet.pubApi";

    @Override
    protected String[] getServletBeanIds() {
        return new String[]{PUB_API};
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(RequestDb.class);
    }

    public void testCreateGet() {
//        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.jcsTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
//        AbstractHttpClient.Response resp = client.get(PUB_API + "/request/exchange_vcard", new Object[][]{
//                {"to", ServerTeam.GRX_ID},
//                {"message", "req 0"}
//        });
//        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
//
//        client.setTicket(ServerTeam.cgTicket());
//        resp = client.get(PUB_API + "/request/change_tel", new Object[][]{
//                {"to", ServerTeam.GRX_ID},
//                {"message", "req 1"}
//        });
//        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
//
//        resp = client.get(PUB_API + "/request/get/" + ServerTeam.GRX_ID, new Object[][]{
//        });
//        ArrayNode arrayNode = (ArrayNode)resp.getJsonNode();
//        assertEquals(2, arrayNode.size());
//        for (JsonNode jsonNode : arrayNode) {
//            if (StringUtils.equals(jsonNode.get("message").getTextValue(), "req 0"))
//                assertEquals(RequestTypes.REQ_EXCHANGE_VCARD, jsonNode.get("type").getIntValue());
//            else
//                assertEquals(RequestTypes.REQ_CHANGE_PROFILE, jsonNode.get("type").getIntValue());
//        }
//        resp = client.get(PUB_API + "/request/count/" + ServerTeam.GRX_ID, new Object[][]{
//        });
//        assertEquals(2, resp.getJsonNode().get("result").getIntValue());
//
//
//        client.setTicket(ServerTeam.grxTicket());
//        resp = client.get(PUB_API + "/request/get", new Object[][]{
//        });
//        arrayNode = (ArrayNode)resp.getJsonNode();
//        assertEquals(2, arrayNode.size());
//        for (JsonNode jsonNode : arrayNode) {
//            if (StringUtils.equals(jsonNode.get("message").getTextValue(), "req 0"))
//                assertEquals(RequestTypes.REQ_EXCHANGE_VCARD, jsonNode.get("type").getIntValue());
//            else
//                assertEquals(RequestTypes.REQ_CHANGE_PROFILE, jsonNode.get("type").getIntValue());
//        }
//        resp = client.get(PUB_API + "/request/count", new Object[][]{
//        });
//        assertEquals(2, resp.getJsonNode().get("result").getIntValue());
    }

    public void testDoneGet() {
//        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.jcsTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
//        AbstractHttpClient.Response resp = client.get(PUB_API + "/request/exchange_vcard", new Object[][]{
//                {"to", ServerTeam.GRX_ID},
//                {"message", "req 0"}
//        });
//        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
//
//        client.setTicket(ServerTeam.cgTicket());
//        resp = client.get(PUB_API + "/request/change_tel", new Object[][]{
//                {"to", ServerTeam.GRX_ID},
//                {"message", "req 1"}
//        });
//        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
//
//        client.setTicket(ServerTeam.wpTicket());
//        resp = client.get(PUB_API + "/request/group_invite", new Object[][]{
//                {"to", ServerTeam.GRX_ID},
//                {"message", "req 2"}
//        });
//        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
//
//        client.setTicket(ServerTeam.grxTicket());
//        resp = client.get(PUB_API + "/request/get", new Object[][]{
//        });
//        ArrayNode arrayNode = (ArrayNode)resp.getJsonNode();
//        long[] requestIds = new long[arrayNode.size()];
//        for (int i = 0; i < arrayNode.size(); i++)
//            requestIds[i] = arrayNode.get(i).get("id").getLongValue();
//        resp = client.get(PUB_API + "/request/done", new Object[][]{
//                {"requests", requestIds[0] + "," + requestIds[1]}
//        });
//        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
//
//        resp = client.get(PUB_API + "/request/get", new Object[][]{
//        });
//        arrayNode = (ArrayNode)resp.getJsonNode();
//        assertEquals(1, arrayNode.size());
//        resp = client.get(PUB_API + "/request/get", new Object[][]{
//                {"status", "all"},
//                {"limit", 10}
//        });
//        arrayNode = (ArrayNode)resp.getJsonNode();
//        assertEquals(3, arrayNode.size());
//        resp = client.get(PUB_API + "/request/get", new Object[][]{
//                {"status", "done"},
//                {"limit", 10}
//        });
//        arrayNode = (ArrayNode)resp.getJsonNode();
//        assertEquals(2, arrayNode.size());
    }
}
