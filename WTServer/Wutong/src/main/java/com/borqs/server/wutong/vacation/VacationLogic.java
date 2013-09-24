package com.borqs.server.wutong.vacation;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;

import java.io.InputStream;

public interface VacationLogic {
    boolean setVacation(Context ctx, InputStream inputStream);

    RecordSet getVacation(Context ctx,String employee);
}