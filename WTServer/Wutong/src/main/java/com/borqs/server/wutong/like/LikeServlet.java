package com.borqs.server.wutong.like;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.WutongContext;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

public class LikeServlet extends WebMethodServlet {
    private static final Logger L = Logger.getLogger(LikeServlet.class);

    @WebMethod("like/like")
    public boolean like(QueryParams qp, HttpServletRequest req) throws UnsupportedEncodingException {
        final String METHOD = "like/like";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        String appId = qp.getString("appid", String.valueOf(Constants.APP_TYPE_BPC));
        return GlobalLogics.getLike().likeP(ctx, viewerId, (int) qp.getInt("object", 2), qp.checkGetString("target"), ua, loc, appId);
    }

//    @WebMethod("like/ttt")
//    public int ttt(QueryParams qp, HttpServletRequest req) throws UnsupportedEncodingException {
//        return GlobalLogics.getLike().updatedata();
//    }

    @WebMethod("like/unlike")
    public boolean unlike(QueryParams qp){
        final String METHOD = "like/unlike";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getLike().unlikeP(ctx,viewerId, (int) qp.getInt("object", 2), qp.checkGetString("target"));
    }

    @WebMethod("like/count")
    public int getLikeCount(QueryParams qp) {
        final String METHOD = "like/count";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getLike().getLikeCountP(ctx,(int) qp.getInt("object", 2), qp.checkGetString("target"));
    }

    @WebMethod("like/users")
    public RecordSet likedUsers(QueryParams qp) {
        final String METHOD = "like/users";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return  GlobalLogics.getLike().likedUsersP(ctx,viewerId, (int) qp.getInt("object", 2), qp.checkGetString("target"), qp.getString("columns", ""), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
    }

    @WebMethod("like/ifliked")
    public boolean likeIfliked(QueryParams qp) {
        final String METHOD = "like/ifliked";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return  GlobalLogics.getLike().ifuserLikedP(ctx,viewerId, qp.checkGetString("targetId"));
    }
}
