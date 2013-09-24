package com.borqs.server.qiupu.util.sfs;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.sfs.SFSException;
import com.borqs.server.base.sfs.local.LocalSFS;
import org.apache.avro.io.ValidatingEncoder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApkSFS extends LocalSFS {

    public static final Pattern FILE_PATTERN = Pattern.compile("^(\\w+(\\.\\w+)*-\\d+-(x86|arm))(\\.apk|\\.screenshot\\d*\\.(png|jpg)|\\.icon\\d*\\.(png|jpg))$");

    public ApkSFS(Configuration conf) {
        this(conf.checkGetString("dir"));
    }

    public ApkSFS(String directory) {
        super(directory);
    }

    public static String calculatePath(String dir, String file) {
        if (!dir.endsWith("/"))
            dir += "/";
        return dir + calculatePath(file);
    }

    public static String calculatePath(String file) {
        Validate.isTrue(isValidFile0(file));
        Matcher matcher = FILE_PATTERN.matcher(file);
        if (!matcher.find())
            throw new SFSException();

        String baseName = matcher.group(0);
        String dir = StringUtils.replaceChars(StringUtils.substringBefore(baseName, "-"), '.', '/');
        return dir + "/" + file;
    }

    @Override
    protected String calculateRelativePath(String file) {
        return calculatePath(file);
    }

    protected static boolean isValidFile0(String file) {
        return FILE_PATTERN.matcher(file).matches();
    }

    @Override
    protected boolean isValidFile(String file) {
        return isValidFile0(file);
    }
}
