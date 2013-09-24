package com.borqs.server.test.request.test1;

import com.borqs.server.impl.request.RequestDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.feature.request.RequestLogic;
import com.borqs.server.platform.feature.request.RequestTypes;
import com.borqs.server.platform.feature.request.Requests;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.ServerTeam;
import com.borqs.server.platform.util.json.JsonCompare;
import com.borqs.server.platform.util.json.JsonHelper;

import java.util.Map;

public class RequestLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(RequestDb.class);
    }

    private RequestLogic getRequestLogic() {
        return (RequestLogic)getBean("logic.request");
    }

    private AccountLogic getAccountLogic() {
        return (AccountLogic)getBean("logic.account");
    }

    private Request request0 = Request.newRandom(ServerTeam.JCS_ID, ServerTeam.GRX_ID, 1, RequestTypes.REQ_EXCHANGE_VCARD, "req 0", "");
    private Request request1 = Request.newRandom(ServerTeam.JCS_ID, ServerTeam.CG_ID, 1, RequestTypes.REQ_EXCHANGE_VCARD, "req 1", "");
    private Request request2 = Request.newRandom(ServerTeam.JCS_ID, ServerTeam.WP_ID, 1, RequestTypes.REQ_GROUP_INVITE, "req 2", "");
    private Request request3 = Request.newRandom(ServerTeam.CG_ID, ServerTeam.GRX_ID, 1, RequestTypes.REQ_GROUP_INVITE, "req 3", "");

    public void testCreateGet() {
        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);
        RequestLogic requestLogic = getRequestLogic();
        requestLogic.create(ctx, request0, request1, request2);
        ctx = Context.createForViewer(ServerTeam.CG_ID);
        requestLogic.create(ctx, request3);

        Requests requests = requestLogic.getAllRequests(ctx, ServerTeam.GRX_ID, 1, 0, 10);
        assertEquals(2, requests.size());
        for (Request request : requests) {
            if (request.getMessage().equals("req 0"))
                assertEquals(request0, request);
            else
                assertEquals(request3, request);
        }
    }

    public void testDoneGetAndExpansion() {
        Context ctx = Context.createForViewer(ServerTeam.JCS_ID);
        RequestLogic requestLogic = getRequestLogic();
        requestLogic.create(ctx, request0, request1, request2);
        ctx = Context.createForViewer(ServerTeam.CG_ID);
        requestLogic.create(ctx, request3);

        ctx = Context.createForViewer(ServerTeam.GRX_ID);
        requestLogic.done(ctx, request0.getRequestId());

        Requests requests = requestLogic.getAllRequests(ctx, ServerTeam.GRX_ID, 1, 0, 10);
        assertEquals(2, requests.size());
        requests = requestLogic.getPendingRequests(ctx, ServerTeam.GRX_ID, 1, 0);
        assertEquals(1, requests.size());
        assertEquals(1, requestLogic.getPendingCount(ctx, ServerTeam.GRX_ID, 1, 0));
        assertEquals(request3, requests.get(0));
        requests = requestLogic.getDoneRequests(ctx, ServerTeam.GRX_ID, 1, 0, 10);
        assertEquals(1, requests.size());
        assertEquals(request0, requests.get(0));
        Map<Long, int[]> m = requestLogic.getPendingTypes(ctx, ServerTeam.JCS_ID, ServerTeam.GRX_ID, ServerTeam.CG_ID, ServerTeam.WP_ID);
        System.out.println(JsonHelper.toJson(m, true));

        ctx = Context.createForViewer(ServerTeam.JCS_ID);
        AccountLogic account = getAccountLogic();
        User grx = account.getUser(ctx, new String[]{"pending_req_types"}, ServerTeam.GRX_ID);
        int[] types = (int[])grx.getAddon("pending_req_types", new int[]{});
        assertTrue(JsonCompare.compare(JsonHelper.parse(JsonHelper.toJson(new int[]{}, false)),
                JsonHelper.parse(JsonHelper.toJson(types, false))).isEquals());

        User cg = account.getUser(ctx, new String[]{"pending_req_types"}, ServerTeam.CG_ID);
        types = (int[])cg.getAddon("pending_req_types", new int[]{});
        assertTrue(JsonCompare.compare(JsonHelper.parse(JsonHelper.toJson(new int[]{RequestTypes.REQ_EXCHANGE_VCARD}, false)),
                JsonHelper.parse(JsonHelper.toJson(types, false))).isEquals());

        User wp = account.getUser(ctx, new String[]{"pending_req_types"}, ServerTeam.WP_ID);
        types = (int[])wp.getAddon("pending_req_types", new int[]{});
        assertTrue(JsonCompare.compare(JsonHelper.parse(JsonHelper.toJson(new int[]{RequestTypes.REQ_GROUP_INVITE}, false)),
                JsonHelper.parse(JsonHelper.toJson(types, false))).isEquals());

        Users users = new Users();
        users.add(grx);
        users.add(cg);
        users.add(wp);
        System.out.println(users.toJson(new String[]{"pending_req_types"}, true));
    }
}
