package com.borqs.server.wutong.conf;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface ConfLogic{
    boolean saveConfiguration(Context ctx ,Record configuration) ;

    RecordSet getConfiguration(Context ctx,String userId, String key, int version_code) ;

    boolean deleteConfiguration(Context ctx,String userId, String key, int version_code) ;

    RecordSet getUserConfiguration(Context ctx,String userId) ;

}