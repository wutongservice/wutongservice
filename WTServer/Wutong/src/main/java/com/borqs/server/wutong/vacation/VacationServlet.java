package com.borqs.server.wutong.vacation;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.ServletException;
import java.io.IOException;

public class VacationServlet extends WebMethodServlet {
    private String serverHost;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");
    }

    @WebMethod("vacation/upload")
    public boolean setAppSettingConfigs(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        FileItem file = qp.checkGetFile("file");


        VacationLogic vacation = GlobalLogics.getVacation();
        boolean b= false;
        try {
             b = vacation.setVacation(ctx, file.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b;
    }

    @WebMethod("vacation/get")
    public RecordSet getAppSettingConfigs(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String employee = qp.checkGetString("employee");

        VacationLogic holiday = GlobalLogics.getVacation();
        return holiday.getVacation(ctx, employee);
    }

}
