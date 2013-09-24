package com.borqs.server.test.platform.log;


import com.borqs.server.platform.app.AppBootstrap;
import com.borqs.server.platform.util.VfsHelper;

public class LogTest {
    public static void main(String[] args) throws Exception {
        AppBootstrap.run(VfsHelper.classpathFileToPath(LogTest.class, "config.xml"), "main", new String[]{});
    }
}
