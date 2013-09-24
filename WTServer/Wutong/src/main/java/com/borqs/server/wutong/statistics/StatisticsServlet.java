package com.borqs.server.wutong.statistics;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import org.apache.avro.AvroRemoteException;

import javax.servlet.ServletException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class StatisticsServlet extends WebMethodServlet {
    private Record statistics = new Record();
    private Timer timer;
    private StatisticsTask task;
    private final long interval = 60 * 1000;

    @Override
    public void init() throws ServletException {
        super.init();
        timer = new Timer();
        task = new StatisticsTask();
        timer.schedule(task, interval, interval);
    }
    private class StatisticsTask extends TimerTask {

        @Override
        public void run() {
            try {
                saveStatistics();
            } catch (AvroRemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean saveStatistics() throws AvroRemoteException {

        StatisticsLogic statisticsLogic = GlobalLogics.getStatisticsLogic();
//         L.debug("Begin save statistics");
        boolean r = statisticsLogic.save(statistics);
//         L.debug("End save statistics");
        statistics.clear();
        return r;
    }

    @WebMethod("internal/statistics")
    public Record httpCallStatistics(QueryParams qp) {
        String api = qp.checkGetString("api");
//        L.debug("api: " + api);
        long increment = statistics.getInt(api, 0L);
        increment++;
        statistics.put(api, increment);
        return Record.of(api, increment);
    }

    @WebMethod("internal/keyvalue")
    public boolean keyValueStatistics(QueryParams qp) {

        Record keyRecord = new Record();
        keyRecord.put("device", qp.checkGetString("device"));
        keyRecord.put("key", qp.checkGetString("key"));
        keyRecord.put("value", qp.checkGetString("value"));
        keyRecord.put("created_time", new Date().toString());

        StatisticsLogic statisticsLogic = GlobalLogics.getStatisticsLogic();
        boolean r = statisticsLogic.keyValue(keyRecord);

        return r;
    }

    /*
     *show all internal command
     */
    @WebMethod("internal/showcommand")
    public Record showCommand(QueryParams qp) {

        Record keyRecord = new Record();
        keyRecord.put("internal/keyvalue",      "p1: device, p2:key, p3:value");
        keyRecord.put("internal/showkeyvalue",  "no parameters");
        keyRecord.put("internal/debugswitch",   "p1: mode, loger name p2:status(boolean true, false)");
        keyRecord.put("internal/debugstatus",    "no parameters");
        keyRecord.put("internal/debugenableall", "p1: status(boolean, true, false)");
        keyRecord.put("internal/isdebugenable",  "p1: mode(logger name)");
        keyRecord.put("internal/showkeyvaluecount",  "no parameters");

        return keyRecord;
    }

    @WebMethod("internal/showkeyvalue")
    public RecordSet showKeyValueStatistics(QueryParams qp) {
        StatisticsLogic statisticsLogic = GlobalLogics.getStatisticsLogic();
        Record keyRecord = new Record();
        try{
            keyRecord.put("start", qp.getInt("start", 0));
            keyRecord.put("end", qp.getInt("end", 10000000));
        }catch(Exception ne){}
        return statisticsLogic.showKeyValue(keyRecord);
    }
    @WebMethod("internal/showkeyvaluecount")
    public RecordSet showKeyValueCount(QueryParams qp) {
        StatisticsLogic statisticsLogic = GlobalLogics.getStatisticsLogic();
        return statisticsLogic.showCount();
    }
}
