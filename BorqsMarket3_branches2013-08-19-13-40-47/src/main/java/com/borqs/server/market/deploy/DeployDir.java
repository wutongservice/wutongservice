package com.borqs.server.market.deploy;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

public class DeployDir implements ServletContextListener {

    private static String rootPath;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        rootPath = servletContextEvent.getServletContext().getRealPath("/");
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }

    public static String rootPath() {
        return rootPath;
    }

    public static String path(String relPath) {
        relPath = StringUtils.removeStart(relPath, "/");
        relPath = StringUtils.removeStart(relPath, "\\");
        return FilenameUtils.concat(rootPath, relPath);
    }

    public static File pathAsFile(String relPath) {
        String path = path(relPath);
        return path != null ? new File(path) : null;
    }
}
