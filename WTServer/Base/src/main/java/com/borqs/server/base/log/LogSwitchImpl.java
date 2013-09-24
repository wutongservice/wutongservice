package com.borqs.server.base.log;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.Initializable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: b058
 * Date: 3/28/13
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class LogSwitchImpl implements LogSwitchLogic, Initializable {
    static HashMap<String, Boolean> logSwitchMap = new HashMap<String, Boolean>();
    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean switchLog(String Mode, boolean openOrClose) {
        logSwitchMap.put(Mode, openOrClose);
        return true;
    }

    @Override
    public RecordSet showLogSwitch() {
        RecordSet rs = new RecordSet();

        Set<String> sets = logSwitchMap.keySet();
        Iterator<String> it =  sets.iterator();
        while (it.hasNext())
        {
            String Key = it.next();
            Record rc = new Record();
            rc.put(Key, logSwitchMap.get(Key));

            rs.add(rc);
        }
        return rs;
    }

    //default debug is open
    @Override
    public boolean isDebugEnable(String Mode) {
        Boolean value = logSwitchMap.get(Mode);
        return value==null?true:value;
    }

    @Override
    public RecordSet ennableAllLog(boolean status) {

        Set<String> sets = logSwitchMap.keySet();
        Iterator<String> it =  sets.iterator();
        while (it.hasNext())
        {
            logSwitchMap.put(it.next(), status);
        }

        return showLogSwitch();
    }
}
