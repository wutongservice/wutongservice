package com.borqs.server.market.utils;


import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.springframework.util.StringUtils;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils2 {

    public static String homePath(String name) {
        String home = FileUtils.getUserDirectoryPath();
        return StringUtils.isEmpty(name)
                ? home
                : FilenameUtils.concat(home, name);
    }

    public static void ensureDirectory(String dir) throws IOException {
        FileUtils.forceMkdir(new File(dir));
    }

    public static long getFileSize(String path) {
        return FileUtils.sizeOf(new File(path));
    }

    public static String getFileMd5(String path) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream in = new BufferedInputStream(new FileInputStream(path));
        try {
            DigestInputStream din = new DigestInputStream(in, md);
            IOUtils.copy(din, new NullOutputStream());
            return Hex.encodeHexString(md.digest());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static String getFileMd5(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        InputStream in = new BufferedInputStream(inputStream);
        try {
            DigestInputStream din = new DigestInputStream(in, md);
            IOUtils.copy(din, new NullOutputStream());
            return Hex.encodeHexString(md.digest());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
