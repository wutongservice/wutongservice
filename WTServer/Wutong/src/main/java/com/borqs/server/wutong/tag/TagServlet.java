package com.borqs.server.wutong.tag;


import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.photo.PhotoServlet;

public class TagServlet extends WebMethodServlet {
    @WebMethod("tag/create")
    public Record createTag(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        TagLogic tagLogic = GlobalLogics.getTag();

        String viewerId = ctx.getViewerIdString();
        String tag = qp.checkGetString("tag");
        String scope = qp.getString("scope","");
        String type = qp.checkGetString("type");
        String taget_id = qp.checkGetString("target_id");
        Record record = Record.of("user", viewerId, "tag", tag, "type", type, "target_id", taget_id, "created_time", DateUtils.nowMillis());
        record.put("scope", scope);

        return tagLogic.createTag(ctx, record);
    }

    @WebMethod("tag/destroyed")
    public boolean destroyedTag(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        TagLogic tagLogic = GlobalLogics.getTag();

        String viewerId = ctx.getViewerIdString();
        String tag = qp.checkGetString("tag");
        String scope = qp.getString("scope","");
        String type = qp.checkGetString("type");
        String target_id = qp.checkGetString("target_id");
        Record record = Record.of("user", viewerId, "tag", tag, "type", type, "target_id", target_id, "created_time", DateUtils.nowMillis());
        record.put("scope", scope);

        return tagLogic.destroyedTag(ctx, record);
    }

    @WebMethod("tag/hasTag")
    public boolean hasTag(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        TagLogic tagLogic = GlobalLogics.getTag();

        String viewerId = qp.getString("user", ctx.getViewerIdString());
        String tag = qp.checkGetString("tag");
        String scope = qp.getString("scope","");
        String target_id = qp.checkGetString("target_id");
        String type = qp.checkGetString("type");

        return tagLogic.hasTag(ctx, viewerId, tag, scope, target_id, type);
    }


    @WebMethod("tag/findtagbyuser")
    public RecordSet findTagByUser(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        TagLogic tagLogic = GlobalLogics.getTag();

        String viewerId = qp.getString("user", ctx.getViewerIdString());
        String scope = qp.getString("scope", null);
        int count = (int) qp.getInt("count", 20);
        int page = (int) qp.getInt("page", 0);

        return tagLogic.findTagByUser(ctx, viewerId, scope, page, count);
    }

    @WebMethod("tag/findtargetbyuser")
    public RecordSet findTargetByUser(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        TagLogic tagLogic = GlobalLogics.getTag();

        String viewerId = qp.getString("user", ctx.getViewerIdString());

        String type = qp.getString("type", "");
        String scope = qp.getString("scope", null);
        int count = (int) qp.getInt("count", 20);
        int page = (int) qp.getInt("page", 0);

        RecordSet recs = tagLogic.findTargetsByUser(ctx, viewerId, scope, type, page, count);
        RecordSet recs0 = new RecordSet();
        String ids = recs.joinColumnValues("target_id", ",");
        if (type.equals(String.valueOf(Constants.POST_OBJECT))) {
            recs0 = GlobalLogics.getStream().getFullPostsForQiuPuP(ctx, viewerId, ids, false);
        }
        if (type.equals(String.valueOf(Constants.PHOTO_OBJECT))) {
            recs0 = GlobalLogics.getPhoto().getPhotoByIds(ctx, ids);
            PhotoServlet ps = new PhotoServlet();
            for (Record rec : recs0) {
                rec = ps.formatPhotoUrlAndExtend(ctx, viewerId, rec, GlobalConfig.get(), false, false);
            }
        }
        if (type.equals(String.valueOf(Constants.FILE_OBJECT))) {
            recs0 = GlobalLogics.getFile().getStaticFileByIds(ctx, ids);
            for (Record rec : recs0) {
                rec = Commons.formatFileBucketUrl(ctx, viewerId, rec);
            }
        }
        return recs0;
    }

    @WebMethod("tag/findAllUserByTargetTag")
    public RecordSet findAllUserByTargetTag(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        TagLogic tagLogic = GlobalLogics.getTag();

        String tag = qp.checkGetString("tag");
        String scope = qp.getString("scope", "");
        String target = qp.checkGetString("target_id");
        String type = qp.checkGetString("type");


        return tagLogic.findAllUserByTargetTag(ctx, tag, scope, target, type);
    }

    @WebMethod("tag/findusertagbytarget")
    public RecordSet findUserTagByTarget(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        TagLogic tagLogic = GlobalLogics.getTag();

        String target = qp.checkGetString("target_id");
        String scope = qp.getString("scope", "");
        String type = qp.checkGetString("type");
        int count = (int) qp.getInt("count", 20);
        int page = (int) qp.getInt("page", 0);

        return tagLogic.findUserTagByTarget(ctx, scope, target, type, page, count);
    }
}
