package com.borqs.server.platform.test;


import com.borqs.server.platform.app.AppBootstrap;
import com.borqs.server.platform.app.GlobalSpringAppContext;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.util.VfsHelper;
import junit.framework.TestCase;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Properties;

public abstract class ConfigurableTestCase extends TestCase {

    protected static final String CONFIG_XML_FILE = "config.xml";
    protected static final String SETUP_SQL_FILE = "setup.sql";
    protected static final String TEARDOWN_SQL_FILE = "teardown.sql";

    @Override
    public void setUp() throws Exception {
        setupSpringContext();
        setupDb();

    }

    @Override
    public void tearDown() throws Exception {
        teardownDb();
        teardownSpringContext();
    }

    protected DBSchemaBuilder.Script[] buildSqls() {
        return new DBSchemaBuilder.Script[0];
    }

    protected DBSchemaBuilder.Script[] dbScriptsInClasspath(Class... classes) {
        DBSchemaBuilder.Script[] scripts = new DBSchemaBuilder.Script[classes.length];
        for (int i = 0; i < scripts.length; i++) {
            scripts[i] = DBSchemaBuilder.scriptInClasspath(classes[i], "build.sql", "clear.sql");
        }
        return scripts;
    }

    private static String replaceSystemProperties(String configXml) {
        String r = configXml;
        Properties props = System.getProperties();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("test."))
                r = r.replace("%" + key + "%", props.getProperty(key));
        }
        return r;
    }

    protected void setupSpringContext() {
        Class clazz = getClass();
        if (VfsHelper.hasFileInClasspath(clazz, CONFIG_XML_FILE)) {
            String config = VfsHelper.loadTextInClasspath(clazz, CONFIG_XML_FILE);
            config = replaceSystemProperties(config);
            AppBootstrap.initDirect(config);
        }
    }

    protected void teardownSpringContext() {
        if (GlobalSpringAppContext.getInstance() != null)
            AppBootstrap.destroy();
    }


    protected void setupDb() {
        String db = getDb();
        if (StringUtils.isNotBlank(db)) {
            new DBSchemaBuilder(buildSqls()).build(db, true);
            SqlExecutor.executeSource(db, getSetupSql());
        }
    }

    protected void teardownDb() {
        String db = getDb();
        if (StringUtils.isNotBlank(db)) {
            // new DBSchemaBuilder(buildSqls()).clear(db);
            SqlExecutor.executeSource(db, getTeardownSql());
        }
    }

    protected String getDb() {
        return System.getProperty("test.db", "");
    }


    private String getSetupSql() {
        Class clazz = getClass();
        if (VfsHelper.hasFileInClasspath(clazz, SETUP_SQL_FILE)) {
            return VfsHelper.loadTextInClasspath(clazz, SETUP_SQL_FILE);
        } else {
            return "";
        }
    }

    private String getTeardownSql() {
        Class clazz = getClass();
        if (VfsHelper.hasFileInClasspath(clazz, TEARDOWN_SQL_FILE)) {
            return VfsHelper.loadTextInClasspath(clazz, TEARDOWN_SQL_FILE);
        } else {
            return "";
        }
    }

    protected Object getBean(String id) {
        return GlobalSpringAppContext.getBean(id);
    }

    public void info(Object msg) {
        System.out.println("INFO  - " + ObjectUtils.toString(msg));
    }

    public void error(Object msg, Throwable t) {
        System.err.println("ERROR - " + ObjectUtils.toString(msg));
        if (t != null) {
            System.err.println("ERROR - Cause:");
            t.printStackTrace(System.err);
        }
    }

    public void error(Object msg) {
        error(msg, null);
    }
}
