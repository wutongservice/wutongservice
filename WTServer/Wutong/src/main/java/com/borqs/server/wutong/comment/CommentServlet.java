package com.borqs.server.wutong.comment;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.avro.AvroRemoteException;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

public class CommentServlet extends WebMethodServlet {
     private static final Logger L = Logger.getLogger(CommentImpl.class);


    @WebMethod("comment/create")
    public Record createComment(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        final String METHOD = "comment/create";
        Commons commons = new Commons();
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        boolean can_like =  qp.getString("can_like",  "1").equals("1")?true:false;
        String add_to = commons.getAddToUserIds(qp.checkGetString("message"));
        String appId = qp.getString("appid", String.valueOf(Constants.APP_TYPE_BPC));
        String parentId = qp.getString("parent_id", "0");
        return GlobalLogics.getComment().createCommentP(ctx,viewerId, (int) qp.getInt("object", 2), qp.checkGetString("target"), qp.checkGetString("message"), ua, can_like, loc,add_to, appId, parentId);
    }

    @WebMethod("comment/destroy")
    public boolean destroyComments(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "comment/destroy";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        RecordSet recs = GlobalLogics.getComment().destroyCommentsP(ctx,viewerId, qp.checkGetString("comments"));
        for (Record rec : recs) {
            if (!rec.checkGetBoolean("result"))
                return false;
        }
        return true;
    }

    @WebMethod("comment/count")
    public int getCommentCount(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "comment/count";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getComment().getCommentCountP(ctx,viewerId,(int) qp.getInt("object", 2), qp.getString("target", ""));
    }

    @WebMethod("comment/for")
    public RecordSet getCommentsFor(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "comment/for";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        if (qp.getString("comments", "").isEmpty() || qp.getString("comments", "").equals("")) {
            RecordSet recs = GlobalLogics.getComment().getFullCommentsForP(ctx, viewerId, (int) qp.getInt("object", 2), qp.getString("target", ""), qp.getBoolean("asc", false), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
            L.debug(ctx, "comment for return" + recs);
            return recs;
        } else {
            RecordSet recs = GlobalLogics.getComment().getCommentsForP(ctx, viewerId, (int) qp.getInt("object", 2), qp.getString("target", ""), qp.checkGetString("comments"), qp.getBoolean("asc", false), (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
            L.debug(ctx, "comment for return" + recs);
            return recs;

        }
    }

    @WebMethod("comment/get")
    public RecordSet getComments(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "comment/get";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();

        if (qp.getString("columns", "").isEmpty() || qp.getString("columns", "").equals("")) {
            return GlobalLogics.getComment().getFullComments(ctx,viewerId,qp.checkGetString("comments"));
        } else {
            return GlobalLogics.getComment().getCommentsP(ctx,viewerId,qp.checkGetString("comments"), qp.checkGetString("columns"));
        }
    }

    @WebMethod("comment/can_like")
    public boolean commentCanLike(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "comment/can_like";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getComment().commentCanLikeP(ctx,viewerId,qp.checkGetString("comment"));
    }

    @WebMethod("comment/updateaction")
    public boolean commentUpdateCanLike(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "comment/updateaction";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        return GlobalLogics.getComment().updateCommentCanLikeP(ctx,viewerId,qp.checkGetString("commentId"),qp.getBoolean("can_like",true));
    }
}
