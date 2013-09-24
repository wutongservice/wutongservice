package com.borqs.server.wutong.logswitch;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.LogSwitchLogic;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;

/**
 * Created with IntelliJ IDEA.
 * User: b058
 * Date: 3/28/13
 * Time: 2:08 PM
 *
 * is use to switch log print, developer can open/close the log in dynamic way
 *
 */
public class LogSwitchServlet extends WebMethodServlet {

    @WebMethod("internal/debugswitch")
    public boolean logswitch(QueryParams qp) {
        LogSwitchLogic lsl = GlobalLogics.getLogSwitchLogic();
        return lsl.switchLog(qp.checkGetString("mode"), qp.checkGetBoolean("status"));
    }

    @WebMethod("internal/debugstatus")
    public RecordSet showDebugSwitch(QueryParams qp) {
        LogSwitchLogic lsl = GlobalLogics.getLogSwitchLogic();
        return lsl.showLogSwitch();
    }

    @WebMethod("internal/debugenableall")
    public RecordSet enableAllLogSwitch(QueryParams qp) {
        LogSwitchLogic lsl = GlobalLogics.getLogSwitchLogic();
        return lsl.ennableAllLog(qp.checkGetBoolean("status"));
    }

    @WebMethod("internal/isdebugenable")
    public boolean isDebugEnable(QueryParams qp) {
        LogSwitchLogic lsl = GlobalLogics.getLogSwitchLogic();
        return lsl.isDebugEnable(qp.checkGetString("mode"));
    }
}

