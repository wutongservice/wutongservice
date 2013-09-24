package com.borqs.server.wutong.category;


import com.borqs.server.ServerException;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.group.GroupLogic;

public class CategoryServlet extends WebMethodServlet {

    public static final String REGEX = ",";

    @WebMethod("categorytype/create")
    public RecordSet createCategoryType(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        CategoryLogic tagLogic = GlobalLogics.getCategory();

        String viewerId = ctx.getViewerIdString();
        String category = qp.checkGetString("category");
        String type = qp.getString("type", "0");
        String scope = qp.checkGetString("scope");

        GroupLogic group = GlobalLogics.getGroup();
        boolean b = group.hasRight(ctx, Long.parseLong(scope), ctx.getViewerId(), Constants.ROLE_ADMIN);
        if (!b)
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "no permission to create");

        RecordSet rs = new RecordSet();
        for (String s : category.split(REGEX)) {
            Record record = Record.of("user_id", viewerId, "category", s, "type", type, "scope", scope, "created_time", DateUtils.nowMillis());
            rs.add(record);
        }

        RecordSet records = tagLogic.createCategoryType(ctx, rs);
        if (records.size() < 1)
            throw new ServerException(WutongErrors.COMMENT_REPEAT_CONTENT, "category is exists");
        return records;
    }


    @WebMethod("categorytype/update")
    public Record updateCategoryType(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        CategoryLogic tagLogic = GlobalLogics.getCategory();

        String viewerId = ctx.getViewerIdString();
        String category_id = qp.checkGetString("category_id");
        String category = qp.checkGetString("category");
        String userId = qp.getString("user_id", viewerId);
        String scope = qp.checkGetString("scope");

        GroupLogic group = GlobalLogics.getGroup();
        boolean b = group.hasRight(ctx, Long.parseLong(scope), ctx.getViewerId(), Constants.ROLE_ADMIN);
        if (!b)
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "no permission to update");


        Record record = Record.of("user_id", userId, "category_id", category_id, "category", category);

        return tagLogic.updateCategoryType(ctx, record);
    }

    @WebMethod("category/update")
    public Record updateCategory(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        CategoryLogic tagLogic = GlobalLogics.getCategory();

        String viewerId = ctx.getViewerIdString();
        String category_id = qp.checkGetString("category_id");
        String id = qp.checkGetString("id");

        Record record = Record.of("user_id", viewerId, "category_id", category_id, "id", id);

        return tagLogic.updateCategory(ctx, record);
    }

    @WebMethod("categorytype/destroy")
    public boolean destroyCategoryType(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        CategoryLogic tagLogic = GlobalLogics.getCategory();

        String viewerId = ctx.getViewerIdString();
        String userId = qp.getString("user_id", viewerId);
        String category_id = qp.checkGetString("category_id");
        Record record = Record.of("user_id", userId, "category_id", category_id);
        String scope = qp.checkGetString("scope");

        GroupLogic group = GlobalLogics.getGroup();
        boolean b = group.hasRight(ctx, Long.parseLong(scope), ctx.getViewerId(), Constants.ROLE_ADMIN);
        if (!b)
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "no permission to destroy");
        //tagLogic.getCategories(ctx, category_id);

        return tagLogic.destroyedCategories(ctx, record);
    }

    /*@WebMethod("categorytype/test")
    public void destroyCategoryType2(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        ActionsBizLogic holidayBiz = GlobalLogics.getHolidayBiz();

        StreamLogic streamLogic = GlobalLogics.getStream();
        Record r = streamLogic.getPostP(ctx, "2782690841099382943", "post_id,app_data,source");
        ActionLogic action = GlobalLogics.getAction();
        action.sendActionQueue(ctx, r);

    }*/

}
