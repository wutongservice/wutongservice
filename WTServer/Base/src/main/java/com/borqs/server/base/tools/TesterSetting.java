package com.borqs.server.base.tools;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

class TesterSetting {
    public String uri;
    public String ticket;
    public String appid;
    public String secret;
    public String ua;
    public boolean print = true;

    TesterSetting() {
    }

    private static String getDefaultConfigPath() {
        return System.getProperty("user.home") + "/.borqs_web_tester_setting.properties";
    }

    private static void setProperty(Properties props, String key, Object o) {
        if (o != null)
            props.setProperty(key, ObjectUtils.toString(o, ""));
    }

    public void save() {
        save(new File(getDefaultConfigPath()));
    }

    public void save(File file) {
        Validate.notNull(file);
        Properties props = new Properties();
        setProperty(props, "uri", uri);
        setProperty(props, "ticket", ticket);
        setProperty(props, "appid", appid);
        setProperty(props, "secret", secret);
        setProperty(props, "ua", ua);
        setProperty(props, "print", print);

        FileWriter w = null;
        try {
            w = new FileWriter(file);
            props.store(w, "Web tester setting");
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        } finally {
            IOUtils.closeQuietly(w);
        }
    }

    public void load() {
        load(new File(getDefaultConfigPath()));
    }

    public boolean load(File file) {
        Validate.notNull(file);
        if (!file.exists())
            return false;
        Properties props = new Properties();
        FileReader r = null;
        try {
            r = new FileReader(file);
            props.load(r);
            uri = props.getProperty("uri", null);
            ticket = props.getProperty("ticket", null);
            appid = props.getProperty("appid", null);
            secret = props.getProperty("secret", null);
            ua = props.getProperty("ua", null);
            print = Boolean.parseBoolean(props.getProperty("print", "true"));
            return true;
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        } finally {
            IOUtils.closeQuietly(r);
        }
    }

}
