package com.borqs.server.base.io;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.vfs2.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VfsUtils {
    public static FileSystemManager getFileSystemManager() throws FileSystemException {
        return VFS.getManager();
    }


    public static void close(FileObject fo) throws FileSystemException {
        if (fo != null)
            fo.close();
    }

    public static void close(FileContent fc) throws FileSystemException {
        if (fc != null)
            fc.close();
    }

    public static boolean fileExists(String file) {
        try {
            FileSystemManager fsm = getFileSystemManager();
            FileObject fo = null;
            try {
                fo = fsm.resolveFile(file);
                return fo.exists();
            } finally {
                close(fo);
            }
        } catch (FileSystemException e) {
            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        }
    }

    public static boolean copyFile(String src, String dst, boolean override) {
        FileObject dstFO = null;
        try {
            FileSystemManager fsm = getFileSystemManager();
            FileObject srcFO = null;

            try {
                dstFO = fsm.resolveFile(dst);
                if (!override && dstFO.exists())
                    return false;

                if (!dstFO.exists())
                    dstFO.createFile();

                //System.out.println("AAA");
                srcFO = fsm.resolveFile(src);
                //System.out.println("BBB");

                FileContent srcFC = null, dstFC = null;
                try {
                    srcFC = srcFO.getContent();
                    dstFC = dstFO.getContent();

                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = srcFC.getInputStream();
                        out = dstFC.getOutputStream();
                        IOUtils.copy(in, out);
                        return true;
                    } finally {
                        IOUtils.closeQuietly(out);
                        IOUtils.closeQuietly(in);
                    }
                } finally {
                    close(dstFC);
                    close(srcFC);
                }

                //FileUtil.copyContent(srcFO, dstFO);
            } finally {
                close(dstFO);
                close(srcFO);
                //System.out.println("Close SRC");
            }
        } catch (IOException e) {
            if (dstFO != null) {
                try {
                    dstFO.delete();
                } catch (FileSystemException ignored) {
                }
            }

            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        }
    }

    public static boolean copyFile(FilePair filePair, boolean override) {
        Validate.notNull(filePair);
        return copyFile(filePair.source, filePair.destination, override);
    }

    public static void copyFile() {

    }
}
