package com.borqs.server.test.pubapi.test2.account;

import com.borqs.server.impl.account.UserDb;
import com.borqs.server.impl.cibind.CibindDb;
import com.borqs.server.impl.login.TicketDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.web.AbstractHttpClient;
import org.codehaus.jackson.JsonNode;

import java.util.LinkedHashMap;

public class AccountApiTest2 extends ServletTestCase {
    public static final String PUB_API = "servlet.pubApi";

    @Override
    protected String[] getServletBeanIds() {
        return new String[]{PUB_API};
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(UserDb.class, TicketDb.class, CibindDb.class);
    }

    private CibindLogic getCibindLogic() {
        return (CibindLogic) getBean("logic.cibind");
    }

    public void testFindUserId() {
        CibindLogic cibind = getCibindLogic();
        cibind.bind(Context.createForViewer(ServerTeam.GRX_ID), BindingInfo.EMAIL, "grx@serverteam.com");
        cibind.bind(Context.createForViewer(ServerTeam.JCS_ID), BindingInfo.MOBILE_TEL, "13600001111");
        cibind.bind(Context.createForViewer(ServerTeam.CG_ID), BindingInfo.EMAIL, "cg@serverteam.com");
        cibind.bind(Context.createForViewer(ServerTeam.WP_ID), BindingInfo.MOBILE_TEL, "13500002222");

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ServerTeam.jcsTicket(), TestApp.APP2_ID, TestApp.APP2_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/user/id", new Object[][]{
                {"names", "grx@serverteam.com, 13600001111, cg@serverteam.com, 13500002222, wp@serverteam.com"}
        });

        LinkedHashMap<String, Long> m = new LinkedHashMap<String, Long>();
        m.put("grx@serverteam.com", ServerTeam.GRX_ID);
        m.put("13600001111", ServerTeam.JCS_ID);
        m.put("cg@serverteam.com", ServerTeam.CG_ID);
        m.put("13500002222", ServerTeam.WP_ID);
        m.put("wp@serverteam.com", 0L);
        
        JsonNode jsonNode = resp.getJsonNode();
        for (String content : m.keySet())
            assertEquals(m.get(content).longValue(), jsonNode.get(content).getLongValue());
    }
}
