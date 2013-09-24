package com.borqs.server.wutong.search;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;

public class SearchServlet extends WebMethodServlet {
    private static final Logger L = Logger.getLogger(SearchServlet.class);

    public SearchServlet() {
    }

    @WebMethod("objects/search")
    public Record search(QueryParams qp) {
        final String METHOD = "objects/search";
        Context ctx = WutongContext.getContext(qp, false);

        String q = StringUtils.trim(qp.checkGetString("q"));
        if (q.isEmpty())
            throw new ServerException(BaseErrors.PLATFORM_ILLEGAL_PARAM, "Param q is blank");

        String type = qp.getString("type", "all");

        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();

        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        if (count > 100) {
            count = 100;
        }

        SearchLogic search = GlobalLogics.getSearch();

        HashMap<String, String> opts = new HashMap<String, String>();
        if ("post".equalsIgnoreCase(type)) {
            if (qp.containsKey("group"))
                opts.put("group", qp.checkGetString("group"));
            if (qp.containsKey("sort"))
                opts.put("sort", qp.checkGetString("sort"));
        }
        return search.search(ctx, q, type, opts, page, count);
    }
}
