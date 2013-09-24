package com.borqs.server.wutong.favorite;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.photo.PhotoServlet;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class FavoriteServlet extends WebMethodServlet {
    private static final Logger L = Logger.getLogger(FavoriteServlet.class);

    @WebMethod("favorite/create")
    public boolean favoriteCreate(QueryParams qp, HttpServletRequest req) throws UnsupportedEncodingException {
        final String METHOD = "favorite/create";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        Record rec = new Record();
        rec.put("user_id",viewerId);
        rec.put("target_type",qp.checkGetString("target_type"));
        rec.put("target_id",qp.checkGetString("target_id"));
        String appId = qp.getString("appid", String.valueOf(Constants.APP_TYPE_BPC));
        rec.put("appid",appId);
        return GlobalLogics.getFavorite().saveFavorite(ctx, rec);
    }

    @WebMethod("favorite/delete")
    public boolean favoriteDelete(QueryParams qp, HttpServletRequest req) throws UnsupportedEncodingException {
        final String METHOD = "favorite/delete";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        String target_type  = qp.checkGetString("target_type");
        String target_id  = qp.checkGetString("target_id");
        return GlobalLogics.getFavorite().destroyFavorite(ctx, viewerId, target_type, target_id);
    }

    @WebMethod("favorite/summary")
    public RecordSet favoriteSummary(QueryParams qp) {
        final String METHOD = "favorite/summary";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        String target_types = qp.checkGetString("target_types");
        return GlobalLogics.getFavorite().getFavoriteSummary(ctx, viewerId, target_types);
    }

    @WebMethod("favorite/get")
    public RecordSet favoriteGet(QueryParams qp) {
        final String METHOD = "favorite/get";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        int target_type = (int)qp.checkGetInt("target_type");
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);

        String ids = GlobalLogics.getFavorite().getFavoriteByType(ctx,viewerId,String.valueOf(target_type),page,count);

        RecordSet recs = new RecordSet();

        if (target_type == Constants.USER_OBJECT) {
            recs = GlobalLogics.getAccount().getUsers(ctx, viewerId, ids, Constants.USER_COLUMNS_SHAK);
        }

        if (target_type == Constants.POST_OBJECT) {
            recs = GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, ids, false);
        }
        if (target_type==Constants.PHOTO_OBJECT){
            recs = GlobalLogics.getPhoto().getPhotoByIds(ctx, ids);
            PhotoServlet ps = new PhotoServlet();
            for (Record rec : recs) {
                rec = ps.formatPhotoUrlAndExtend(ctx, viewerId, rec, GlobalConfig.get(),false,false);
            }
        }
        if (target_type == Constants.FILE_OBJECT) {
            recs = GlobalLogics.getFile().getStaticFileByIds(ctx, ids);
            for (Record rec : recs) {
                rec = Commons.formatFileBucketUrl(ctx, viewerId, rec);
            }
        }
        //Constants.FULL_COMMENT_COLUMNS;
//        if (target_type == Constants.COMMENT_OBJECT) {
//            recs = GlobalLogics.getComment().getCommentsP(ctx,viewerId,ids, Constants.FULL_COMMENT_COLUMNS);
//        }
        return recs;
    }

}
