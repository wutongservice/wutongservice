package com.borqs.server.test.platform.service;


import com.borqs.server.platform.app.AppMain;
import org.apache.commons.lang.StringUtils;

public class Job2 implements AppMain {
    @Override
    public void run(String[] args) throws Exception {
        System.out.println("======== job2 : " + StringUtils.join(args, "~"));
    }
}
