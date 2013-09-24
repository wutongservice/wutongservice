package com.borqs.server.wutong.appsettings;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface AppSettingLogic {
    Record setSetting(Context ctx,Record setting);
    RecordSet getSettings(Context ctx,String key,int version);
}