package com.borqs.server.base.log;

import com.borqs.server.base.data.RecordSet;

/**
 * Created with IntelliJ IDEA.
 * User: b058
 * Date: 3/28/13
 * Time: 2:05 PM
 * To change this template use File | Settings | File Templates.
 */
public interface LogSwitchLogic {
    boolean   switchLog(String Mode, boolean openOrClose);
    RecordSet showLogSwitch();
    boolean   isDebugEnable(String Mode);
    RecordSet ennableAllLog(boolean  status);
}
