package com.borqs.server.platform.service;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.Charsets;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class ServiceApp extends ServiceAppSupport {

    public ServiceApp() {
    }

    @Override
    protected void loop() {
        try {
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in, Charsets.DEFAULT));
            while (true) {
                String line = reader.readLine();
                if (StringUtils.equalsIgnoreCase(line, "quit") || StringUtils.equalsIgnoreCase(line, "exit"))
                    break;
            }
        } catch (IOException e) {
            throw new ServerException(E.SERVICE, "Loop error");
        }
    }
}
