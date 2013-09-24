package com.borqs.server.test.platform.service;


import org.apache.commons.lang.StringUtils;

public class Job1 {
    public static void main(String[] args) {
        System.out.println("======== job1 : " + StringUtils.join(args, "~"));
    }
}
