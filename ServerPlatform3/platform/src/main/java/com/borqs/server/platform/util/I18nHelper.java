package com.borqs.server.platform.util;


import org.apache.commons.lang.StringUtils;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18nHelper {

    public static final Locale DEFAULT_LOCALE = Locale.CHINESE;

    public static Locale parseLocale(String s) {
        if (s == null)
            return DEFAULT_LOCALE;

        String[] ss = StringUtils.split(s, "_", 3);
        if (ss.length == 1) {
            return new Locale(ss[0]);
        } else if (ss.length == 2) {
            return new Locale(ss[0], ss[1]);
        } else if (ss.length >= 3) {
            return new Locale(ss[0], ss[1], ss[2]);
        } else {
            return DEFAULT_LOCALE;
        }
    }


    public static ResourceBundle getBundle(String baseName) {
        return getBundle(baseName, (Locale)null);
    }

    public static ResourceBundle getBundle(String baseName, Locale locale) {
        if (locale == null)
            locale = DEFAULT_LOCALE;

        return ResourceBundle.getBundle(baseName, locale, XmlPropertiesResourceBundle.CONTROL);
    }

    public static ResourceBundle getBundle(String baseName, String localeName) {
        return getBundle(baseName, parseLocale(localeName));
    }

    public static String getString(String baseName, String localeName, String key) {
        ResourceBundle bundle =getBundle(baseName, localeName);
        return bundle.getString(key);
    }

    public static String getString(String baseName, String key) {
        ResourceBundle bundle =getBundle(baseName);
        return bundle.getString(key);
    }

//    private static class TheControl extends ResourceBundle.Control {
//        @Override
//        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
//            if (StringUtils.equals(format, "java.properties")) {
//                String bundleName = toBundleName(baseName, locale);
//                final String resourceName = toResourceName(bundleName, "properties");
//                final ClassLoader classLoader = loader;
//                final boolean reloadFlag = reload;
//                InputStream stream;
//                try {
//                    stream = AccessController.doPrivileged(
//                            new PrivilegedExceptionAction<InputStream>() {
//                                public InputStream run() throws IOException {
//                                    InputStream is = null;
//                                    if (reloadFlag) {
//                                        URL url = classLoader.getResource(resourceName);
//                                        if (url != null) {
//                                            URLConnection connection = url.openConnection();
//                                            if (connection != null) {
//                                                // Disable caches to get fresh data for
//                                                // reloading.
//                                                connection.setUseCaches(false);
//                                                is = connection.getInputStream();
//                                            }
//                                        }
//                                    } else {
//                                        is = classLoader.getResourceAsStream(resourceName);
//                                    }
//                                    return is;
//                                }
//                            });
//                } catch (PrivilegedActionException e) {
//                    throw (IOException) e.getException();
//                }
//
//                if (stream == null)
//                    return null;
//
//
//                Reader reader = null;
//                try {
//                    reader = new InputStreamReader(stream, Charsets.DEFAULT);
//                    return new PropertyResourceBundle(reader);
//                } finally {
//                    IOUtils.closeQuietly(reader);
//                }
//            } else {
//                return super.newBundle(baseName, locale, format, loader, reload);
//            }
//        }
//    }

}
