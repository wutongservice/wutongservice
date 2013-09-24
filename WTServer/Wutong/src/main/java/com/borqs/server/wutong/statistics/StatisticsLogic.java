
package com.borqs.server.wutong.statistics;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface StatisticsLogic {
    boolean save(Record statistics) ;
    boolean keyValue(Record statistics) ;
    RecordSet showKeyValue(Record rec);
    RecordSet showCount();
}