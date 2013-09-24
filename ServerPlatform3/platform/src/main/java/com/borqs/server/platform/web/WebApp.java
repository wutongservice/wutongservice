package com.borqs.server.platform.web;


import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.MacroExpander;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;

public class WebApp {
    private String contextPath;
    private String warPath;
    private String rootDirectory;
    private String descriptorPath;

    protected WebApp() {
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getWarPath() {
        return warPath;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getDescriptorPath() {
        return descriptorPath;
    }

    public void setup(WebAppContext ctx) {
        Validate.notNull(ctx);
        String tmpDir = FilenameUtils.concat(FileUtils.getTempDirectoryPath(), "jetty_webapp_tmp_" + DateHelper.nowMillis());
        ctx.setTempDirectory(new File(tmpDir));
        ctx.setContextPath(contextPath);
        if (warPath != null) {
            ctx.setWar(warPath);
        } else if (rootDirectory != null) {
            ctx.setResourceBase(rootDirectory);
            ctx.setDescriptor(descriptorPath);
        } else if (descriptorPath != null) {
            ctx.setResourceBase("");
            ctx.setDescriptor(descriptorPath);
        }
    }

    public static WebApp createWarBased(String contextPath, String warPath) {
        Validate.notEmpty(contextPath);
        Validate.notEmpty(warPath);

        WebApp app = new WebApp();
        app.contextPath = MacroExpander.expandSystemMacros(contextPath);
        app.warPath = MacroExpander.expandSystemMacros(warPath);
        app.rootDirectory = null;
        app.descriptorPath = null;
        return app;
    }

    public static WebApp createDirectoryBased(String contextPath, String rootDir, String descPath) {
        Validate.notEmpty(contextPath);
        Validate.notEmpty(rootDir);

        if (StringUtils.isEmpty(descPath))
            descPath = FilenameUtils.concat(rootDir, "WEB-INF/web.xml");

        WebApp app = new WebApp();
        app.contextPath = MacroExpander.expandSystemMacros(contextPath);
        app.warPath = null;
        app.rootDirectory = MacroExpander.expandSystemMacros(rootDir);
        app.descriptorPath = MacroExpander.expandSystemMacros(descPath);
        return app;
    }

    public static WebApp createDirectoryBased(String contextPath, String rootDir) {
        return createDirectoryBased(contextPath, rootDir, null);
    }

    public static WebApp createDescriptorBased(String contextPath, String descPath) {
        Validate.notEmpty(contextPath);
        Validate.notEmpty(descPath);

        WebApp app = new WebApp();
        app.contextPath = MacroExpander.expandSystemMacros(contextPath);
        app.warPath = null;
        app.rootDirectory = null;
        app.descriptorPath = MacroExpander.expandSystemMacros(descPath);
        return app;
    }
}
