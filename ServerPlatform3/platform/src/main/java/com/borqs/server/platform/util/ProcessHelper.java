package com.borqs.server.platform.util;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.IOHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.lang.management.ManagementFactory;

public class ProcessHelper {

    public static final String PID_PATH = "pid.path";

    public static void addShutdownHook(final Runnable runnable) {
        Runtime.getRuntime().addShutdownHook(new Thread(runnable));
    }

    public static int getPid() {
        String s = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(StringUtils.substringBefore(s, "@").trim());
    }

    public static void checkPidAndSetPidCleaner() {
        String pidPath = System.getProperty(PID_PATH, "");
        if (pidPath.isEmpty())
            return;

        final File file = new File(pidPath);
        if (file.exists())
            throw new ServerException(E.PROCESS, "Pid exists");

        if (!IOHelper.writeTextFile(file, Integer.toString(getPid()), false))
            throw new ServerException(E.IO, "Write pid file error");

        addShutdownHook(new Runnable() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(file);
            }
        });
    }
}
