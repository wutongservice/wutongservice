package com.borqs.server.wutong.reportabuse;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

public class ReportAbuseServlet extends WebMethodServlet {


    @WebMethod("post/report_abuse")
    public boolean reportAbuserCreate(QueryParams qp, HttpServletRequest req) throws UnsupportedEncodingException {
        Commons commons = new Commons();
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        String target_id = qp.getString("post_id","");
        String target_id1 = qp.getString("target_id",target_id);
        String reason = qp.getString("reason","");
        int target_type = (int)qp.getInt("target_type", Constants.POST_OBJECT);
        int appid=  (int)qp.getInt("appId", Constants.APP_TYPE_BPC);
        String ua = commons.getDecodeHeader(req, "User-Agent", "", viewerId);
        String loc = commons.getDecodeHeader(req, "location", "", viewerId);
        return GlobalLogics.getReportAbuse().reportAbuserCreate(ctx, viewerId,target_type, target_id1,reason,appid, ua, loc);
    }
}
