package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.compatible.CompatibleContactInfo;
import com.borqs.server.compatible.CompatibleRequest;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.request.RequestLogic;
import com.borqs.server.platform.feature.request.RequestTypes;
import com.borqs.server.platform.feature.request.Requests;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;
import org.apache.commons.collections.MapUtils;

import java.util.Map;

@IgnoreDocument
public class Request1Api extends PublicApiSupport {

    private AccountLogic account;
    private RequestLogic request;

    public Request1Api() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public RequestLogic getRequest() {
        return request;
    }

    public void setRequest(RequestLogic request) {
        this.request = request;
    }

    private static final String[] CONTACT_INFO_USER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_TEL,
            User.COL_EMAIL,
            User.COL_IM,
            User.COL_SIP_ADDRESS,
    };

    @Route(url = "/request/change_profile")
    public void sendChangeProfileRequest(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        User toUser = account.getUser(ctx, CONTACT_INFO_USER_COLUMNS, ctx.getViewer());
        if (toUser == null)
            throw new ServerException(E.INVALID_USER, "Invalid to");
        Map<String, String> ci = CompatibleContactInfo.toContactInfo(toUser.getTel(), toUser.getEmail(), toUser.getIm(), toUser.getSipAddress());
        Record data = new Record();
        // data值为"data":{"type":"email", "old":"old_email", "new":"new_email", "v1Type":"4"}
        for (String type : CompatibleRequest.CI_TYPES) {
            if (req.has(type)) {
                if (type.startsWith("mobile_")) {
                    data.put("type", "tel");
                } else if (type.startsWith("email_")) {
                    data.put("type", "email");
                }
                String old = MapUtils.getString(ci, type, "");
                String new_ = req.checkString(type);
                data.put("type", type);
                data.put("old", old);
                data.put("new", new_);
                data.put("v1Type", CompatibleRequest.getChangeProfileV1Type(type));
                break;
            }
        }
        String message = req.getString("message", "");
        long to = req.checkLong("to");
        com.borqs.server.platform.feature.request.Request newReq =
                com.borqs.server.platform.feature.request.Request.newRandom(ctx.getViewer(), to, ctx.getApp(),
                        RequestTypes.REQ_CHANGE_PROFILE, message, data.toJson());

        request.create(ctx, newReq);
        resp.body(true);
    }

    @Route(url = "/request/profile_access_approve")
    public void sendProfileAccessApproveRequest(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long to = AccountHelper.checkUser(account, ctx, req.checkLong("to"));
        String message = req.getString("message", "");

        com.borqs.server.platform.feature.request.Request newReq =
                com.borqs.server.platform.feature.request.Request.newRandom(ctx.getViewer(), to, ctx.getApp(),
                        RequestTypes.REQ_EXCHANGE_VCARD, message, "");
        request.create(ctx, newReq);
        resp.body(true);
    }

    @Route(url = "/request/get")
    public void getRequests(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        int type = req.getInt("type", 0);
        int app = req.getInt("app", App.APP_NONE);
        Requests reqs = request.getPendingRequests(ctx, ctx.getViewer(), app, type);
        resp.body(RawText.of(CompatibleRequest.requestsToJson(reqs, true)));
    }

    @Route(url = "/request/count")
    public void getRequestCount(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());
        int type = req.getInt("type", 0);
        int app = req.getInt("app", App.APP_NONE);
        long count = request.getPendingCount(ctx, ctx.getViewer(), app, type);
        resp.body(count);
    }

    @Route(url = "/request/done")
    public void doneRequest(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long[] reqIds = req.checkLongArray("requests", ",");
        request.done(ctx, reqIds);
        resp.body(true);
    }
}
