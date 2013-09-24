package com.borqs.server.wutong.ignore;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.avro.AvroRemoteException;

public class IgnoreServlet extends WebMethodServlet {
    @WebMethod("ignore/create")
    public boolean createIgnore(QueryParams qp) throws AvroRemoteException {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        IgnoreLogic ignoreLogic = GlobalLogics.getIgnore();
        return ignoreLogic.createIgnore(ctx, viewerId, qp.checkGetString("target_type"), qp.checkGetString("target_ids"));
    }

    @WebMethod("ignore/delete")
    public boolean deleteIgnore(QueryParams qp) throws AvroRemoteException {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        IgnoreLogic ignoreLogic = GlobalLogics.getIgnore();
        return ignoreLogic.deleteIgnore(ctx, viewerId, qp.checkGetString("target_type"), qp.checkGetString("target_ids"));
    }

    @WebMethod("ignore/get")
    public RecordSet getIgnores(QueryParams qp) throws AvroRemoteException {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        IgnoreLogic ignoreLogic = GlobalLogics.getIgnore();

        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        return ignoreLogic.getIgnoreList(ctx, viewerId, qp.getString("target_type", ""), page, count);
    }
}
