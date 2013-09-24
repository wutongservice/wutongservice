package com.borqs.server.wutong.action.actions.holiday;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface HolidayRecordLogic {
    Record saveHolidayRecord(Context ctx, Record holiday);
    Record updateHolidayRecord(Context ctx, Record holiday);

    //get result by start_time,owner,holiday_type,dep et...
    RecordSet getTotalRecords(Context ctx,Record holiday);

    RecordSet getDetailRecords(Context ctx, Record holiday);
}
