package com.borqs.server.platform.util;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

import java.io.IOException;
import java.io.InputStream;

public class VfsHelper {

    public static void closeQuietly(FileObject fo) {
        if (fo != null) {
            try {
                fo.close();
            } catch (FileSystemException ignored) {
            }
        }
    }

    public static boolean hasFile(String path) {
        FileObject fo = null;
        try {
            fo = VFS.getManager().resolveFile(path);
            return fo != null && fo.exists();
        } catch (IOException e) {
            return false;
        } finally {
            closeQuietly(fo);
        }
    }

    public static boolean hasFileInClasspath(Class clazz, String file) {
        return hasFile(classpathFileToPath(clazz, file));
    }

    public static String loadText(String path) {
        FileObject fo = null;
        try {
            fo = VFS.getManager().resolveFile(path);

            InputStream in = null;
            try {
                in = fo.getContent().getInputStream();
                return IOUtils.toString(in, Charsets.DEFAULT);
            } finally {
                IOUtils.closeQuietly(in);
            }
        } catch (IOException e) {
            throw new ServerException(E.VFS, e);
        } finally {
            closeQuietly(fo);
        }
    }

    public static String loadTextInClasspath(Class clazz, String file) {
        return loadText(classpathFileToPath(clazz, file));
    }

    public static String classpathFileToPath(Class clazz, String file) {
        return packageFileToPath(clazz.getPackage(), file);
    }

    public static String packageFileToPath(Package pkg, String file) {
        return String.format("res:%s/%s", StringUtils.replace(pkg.getName(), ".", "/"), ObjectUtils.toString(file));
    }
}
