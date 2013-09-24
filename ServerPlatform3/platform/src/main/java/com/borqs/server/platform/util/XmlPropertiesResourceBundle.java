package com.borqs.server.platform.util;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class XmlPropertiesResourceBundle extends ResourceBundle {
    private Properties props;

    XmlPropertiesResourceBundle(InputStream stream) throws IOException {
        props = new Properties();
        props.loadFromXML(stream);
    }

    @Override
    protected Object handleGetObject(String key) {
        return props.getProperty(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        Set<String> handleKeys = props.stringPropertyNames();
        return Collections.enumeration(handleKeys);
    }

    public static final Control CONTROL = new TheControl();



    private static class TheControl extends Control {
        private static String XML = "xml";


        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName == null)
                throw new NullPointerException();

//            Locale defaultLocale = Locale.ROOT;
//            return locale.equals(defaultLocale) ? null : defaultLocale;
            //return Locale.ROOT;
            return null;
        }

        @Override
        public List<String> getFormats(String baseName) {
            return Collections.singletonList(XML);
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            if ((baseName == null) || (locale == null) || (format == null) || (loader == null))
                throw new NullPointerException();

            ResourceBundle bundle;
            if (!format.equals(XML))
                return null;


            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, format);
            URL url = loader.getResource(resourceName);
            if (url == null)
                return null;

            URLConnection connection = url.openConnection();
            if (connection == null)
                return null;

            if (reload)
                connection.setUseCaches(false);

            InputStream stream = connection.getInputStream();
            if (stream == null)
                return null;

            BufferedInputStream bis = new BufferedInputStream(stream);
            bundle = new XmlPropertiesResourceBundle(bis);
            bis.close();

            return bundle;
        }
    }
}
