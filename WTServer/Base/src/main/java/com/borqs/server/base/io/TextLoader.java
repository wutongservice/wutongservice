package com.borqs.server.base.io;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TextLoader {

    private static final String FILE_PREFIX = "file://";
    private static final String CLASSPATH_PREFIX = "classpath://";

    public static String load(String path) {
        Validate.notNull(path);
        if (path.startsWith(FILE_PREFIX)) {
            return loadFile(StringUtils.removeStart(path, FILE_PREFIX));
        } else if (path.startsWith(CLASSPATH_PREFIX)) {
            return loadClassPath(StringUtils.removeStart(path, CLASSPATH_PREFIX));
        } else {
            return loadFile(path);
        }
    }

    public static String loadWithDefault(String path, String def) {
        try {
            return load(path);
        } catch (Throwable t) {
            return def;
        }
    }

    public static String loadFile(String file) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            return org.apache.commons.io.IOUtils.toString(in, Charsets.DEFAULT);
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static String loadClassPath(String cp) {
        InputStream in = null;
        try {
            in = TextLoader.class.getClassLoader().getResourceAsStream(cp);
            return org.apache.commons.io.IOUtils.toString(in, "UTF-8");
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public static String classpathFileToPath(Class clazz, String file) {
        return packageFileToPath(clazz.getPackage(), file);
    }

    public static String packageFileToPath(Package pkg, String file) {
        return String.format("classpath://%s/%s", StringUtils.replace(pkg.getName(), ".", "/"), ObjectUtils.toString(file, ""));
    }

    public static String loadClassPath(Class clazz, String f) {
        return loadClassPath(StringUtils.replace(clazz.getPackage().getName(), ".", "/") + "/" + f);
    }
}
