package com.borqs.server.wutong.request;


import com.borqs.server.ServerException;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.group.GroupLogic;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import static com.borqs.server.wutong.Constants.*;


public class RequestServlet extends WebMethodServlet {

    public RequestServlet() {
    }

    @WebMethod("request/count")
    public int getRequestCount(QueryParams qp) throws AvroRemoteException {
        RequestLogic req = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);
        return req.getRequestCountP(ctx, ctx.getViewerIdString(), qp.getString("app", ""), qp.getString("type", ""));
    }

    @WebMethod("request/attention")
    public boolean createRequestAttention(QueryParams qp) throws AvroRemoteException {
        RequestLogic req = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);
        return req.createRequestAttentionP(ctx, ctx.getViewerIdString(), qp.checkGetString("userId"));
    }

    @WebMethod("request/get_new_top")
    public RecordSet getRequestNewTop(QueryParams qp) throws AvroRemoteException {
        RequestLogic req = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic group = GlobalLogics.getGroup();
        String ids = group.getTopCircleIds(ctx);
        RecordSet rs = req.getRequestsNewTop(ctx, ctx.getViewerIdString(), qp.getString("app", ""), qp.getString("type", ""), ids);

        return Commons.addIdStrs(rs, "request_id");
    }

    @WebMethod("request/unread_count")
    public RecordSet getUnReadCount(QueryParams qp) throws AvroRemoteException {
        RequestLogic req = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic group = GlobalLogics.getGroup();
        String ids = group.getTopCircleIds(ctx);

        return req.getUnDoneRequestsGroupByScene(ctx, ctx.getViewerIdString(), qp.getString("app", ""), qp.getString("type", ""));
    }

    @WebMethod("request/get")
    public RecordSet getRequest(QueryParams qp) throws AvroRemoteException {
        RequestLogic req = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);
        RecordSet rs = req.getRequestsP(ctx, ctx.getViewerIdString(), qp.getString("app", ""), qp.getString("type", ""));

        return Commons.addIdStrs(rs, "request_id");
    }

    @WebMethod("request/summary")
    public Record getSummary(QueryParams qp) throws AvroRemoteException {
        RequestLogic req = GlobalLogics.getRequest();
        Context ctx = WutongContext.getContext(qp, true);
        return req.getRequestSummary(ctx, qp.getString("app", ""));
    }

    @WebMethod("request/done")
    public boolean doneRequest(QueryParams qp) throws AvroRemoteException {
        RequestLogic req = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);
        return req.doneRequestsP(ctx, ctx.getViewerIdString(), qp.checkGetString("requests"), qp.getString("type", ""), qp.getString("data", ""), qp.getBoolean("accept", false));
    }

    @WebMethod("request/profile_access_approve")
    public boolean sendProfileAccessApprove(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        RequestLogic reqLogic = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);

        reqLogic.createRequestP(ctx, qp.checkGetString("to"), ctx.getViewerIdString(), "0", REQUEST_PROFILE_ACCESS, qp.getString("message", ""), "", true);
        return true;
    }


    @WebMethod("request/add_friend")
    public boolean sendAddFriend(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        RequestLogic reqLogic = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);
        reqLogic.createRequestP(ctx, qp.checkGetString("to"), ctx.getViewerIdString(), "0", REQUEST_ADD_FRIEND, qp.getString("message", ""), "", true);
        return true;
    }

    @WebMethod("request/change_profile")
    public boolean sendChangeProfile(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        final HashMap<String, String> M = new HashMap<String, String>();
        M.put("mobile_telephone_number", REQUEST_CHANGE_MOBILE_TELEPHONE_NUMBER);
        M.put("mobile_2_telephone_number", REQUEST_CHANGE_MOBILE_2_TELEPHONE_NUMBER);
        M.put("mobile_3_telephone_number", REQUEST_CHANGE_MOBILE_3_TELEPHONE_NUMBER);
        M.put("email_address", REQUEST_CHANGE_EMAIL_ADDRESS);
        M.put("email_2_address", REQUEST_CHANGE_EMAIL_2_ADDRESS);
        M.put("email_3_address", REQUEST_CHANGE_EMAIL_3_ADDRESS);

        RequestLogic reqLogic = GlobalLogics.getRequest();

        Context ctx = WutongContext.getContext(qp, true);

        QueryParams.Value<String> v = qp.getSequentialString(M.keySet().toArray(new String[M.size()]));
        if (v == null || StringUtils.isBlank(v.value))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Can't find data");

        String type = M.get(v.key);
        reqLogic.createRequestP(ctx, qp.checkGetString("to"), ctx.getViewerIdString(), "0", type, qp.getString("message", ""), v.value, true);
        return true;
    }

}
