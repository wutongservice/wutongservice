package com.borqs.server.wutong.app;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;

public class AppServlet extends WebMethodServlet {
    @WebMethod("app/test")
    public Record test(QueryParams qp) {
        return new Record().set("msg", "hello world!");
    }
}
