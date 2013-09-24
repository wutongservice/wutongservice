package com.borqs.server.wutong.email;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EmailThreadPool {
    // init thread pool for send email
    public static ExecutorService executor = Executors.newFixedThreadPool(15);
}
