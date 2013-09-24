package com.borqs.server.base.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.borqs.server.base.io.Charsets;

public class I18nHelper {


    public static ResourceBundle getBundle(String baseName) {
        return getBundle(baseName, null);
    }

    public static ResourceBundle getBundle(String baseName, Locale locale) {
        if (locale == null)
            locale = Locale.getDefault();

        //return ResourceBundle.getBundle(baseName,  locale);
        return ResourceBundle.getBundle(baseName, locale, new TheControl());
    }

    private static class TheControl extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            if (StringUtils.equals(format, "java.properties")) {
                String bundleName = toBundleName(baseName, locale);
                final String resourceName = toResourceName(bundleName, "properties");
                final ClassLoader classLoader = loader;
                final boolean reloadFlag = reload;
                InputStream stream;
                try {
                    stream = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<InputStream>() {
                                public InputStream run() throws IOException {
                                    InputStream is = null;
                                    if (reloadFlag) {
                                        URL url = classLoader.getResource(resourceName);
                                        if (url != null) {
                                            URLConnection connection = url.openConnection();
                                            if (connection != null) {
                                                // Disable caches to get fresh data for
                                                // reloading.
                                                connection.setUseCaches(false);
                                                is = connection.getInputStream();
                                            }
                                        }
                                    } else {
                                        is = classLoader.getResourceAsStream(resourceName);
                                    }
                                    return is;
                                }
                            });
                } catch (PrivilegedActionException e) {
                    throw (IOException) e.getException();
                }

                if (stream == null)
                    return null;


                Reader reader = null;
                try {
                    reader = new InputStreamReader(stream, Charsets.DEFAULT);
                    return new PropertyResourceBundle(reader);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            } else {
                return super.newBundle(baseName, locale, format, loader, reload);
            }
        }
    }

}