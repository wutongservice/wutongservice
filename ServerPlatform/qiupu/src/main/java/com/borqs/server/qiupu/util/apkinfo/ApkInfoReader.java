package com.borqs.server.qiupu.util.apkinfo;


import com.borqs.server.base.io.IOUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApkInfoReader {
    private static volatile String aaptPath = null;
    private static volatile String unzipPath = null;

    static {
        aaptPath = System.getProperty("qiupu.aaptPath", "/usr/bin/aapt");
        if (!ProcessUtils.isCanExecuted(aaptPath))
            throw new ApkInfoException("Can't locate aapt");

        unzipPath = System.getProperty("qiupu.unzipPath", "/usr/bin/unzip");
        if (!ProcessUtils.isCanExecuted(unzipPath))
            throw new ApkInfoException("Can't locate unzip");
    }

    public static ApkInfo getApkInfoByAapt(File apkFile) {
        if (!apkFile.isFile() || !apkFile.exists())
            return null;

        String text = ProcessUtils.executeOutput(false, aaptPath, "dump", "badging", apkFile.getPath());
        ApkInfo apkInfo = parseApkInfo(text);
        apkInfo.setFileSize(apkFile.length());
        loadIcon(apkInfo, apkFile);
        
        FileUtils.deleteQuietly(apkFile);
        
        return apkInfo;
    }

    public static ApkInfo getApkInfoByAapt(FileItem fileItem) {
        String tmpApkPath = FilenameUtils.concat(FileUtils.getTempDirectoryPath(), "qiupu_tmp_apk_" + DateUtils.nowMillis() + ".apk");
        try {
            File tmpApkFile = new File(tmpApkPath);
            IOUtils2.writeToFile(tmpApkFile, fileItem.getInputStream());
            //fileItem.write(tmpApkFile);
            return getApkInfoByAapt(tmpApkFile);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void loadIcon(ApkInfo apkInfo, File apkFile) {
        if (StringUtils.isBlank(apkInfo.getIconPath()))
            return;

        String unzipDir = FilenameUtils.concat(FileUtils.getTempDirectoryPath(), "qiupu_apk_unzip_" + DateUtils.nowMillis());
        try {
            ProcessUtils.executeOutput(false, unzipPath, apkFile.getPath(), "-d", unzipDir);
            File iconFile = new File(FilenameUtils.concat(unzipDir, apkInfo.getIconPath()));
            apkInfo.setIcon(IOUtils2.loadFileToBytes(iconFile));
        } finally {
            try {
                FileUtils.deleteDirectory(new File(unzipDir));
            } catch (IOException ignored) {
            }
        }
    }

    private static ApkInfo parseApkInfo(String text) {
        ApkInfo apkInfo = new ApkInfo();
        apkInfo.setMaxSdkVersion(10000);
        apkInfo.setMinSdkVersion(7);
        apkInfo.setTargetSdkVersion(7);
        for (String line : StringUtils2.splitList(text, "\n", true)) {
            if (line.startsWith("package:")) {
                apkInfo.setPackage(capture("name='([^']+)'", line, 1, ""));
                apkInfo.setVersionCode(Integer.parseInt(capture("versionCode='([^']+)'", line, 1, "0")));
                apkInfo.setVersionName(capture("versionName='([^']+)'", line, 1, ""));
            } else if (line.startsWith("application-label:")) {
                apkInfo.setAppName(capture("application-label: *'([^']+)'", line, 1, ""));
            } else if (line.startsWith("application-icon")) {
                apkInfo.setIconPath(capture("application-icon(-\\w+)?: *'([^']+)'", line, 2, ""));
            } else if (line.startsWith("sdkVersion:")) {
                apkInfo.setMinSdkVersion(Integer.parseInt(capture("sdkVersion: *'([^']+)'", line, 1, "0")));
            } if (line.startsWith("targetSdkVersion:")) {
                apkInfo.setTargetSdkVersion(Integer.parseInt(capture("targetSdkVersion: *'([^']+)'", line, 1, "0")));
            } if (line.startsWith("maxSdkVersion:")) {
                apkInfo.setMaxSdkVersion(Integer.parseInt(capture("maxSdkVersion: *'([^']+)'", line, 1, "0")));
            } else if (line.startsWith("supports-screens:")) {
                apkInfo.setScreenSupports(capture("supports-screens: *'([^']+)'", line, 1, ""));
            }
            // add other info here
        }
        return apkInfo;
    }


    private static String capture(String regex, String s, int groupIndex, String def) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(s);
        ArrayList<String> groups = new ArrayList<String>();
        while (matcher.find()) {
            for (int i = 0; i <= matcher.groupCount(); i++)
                groups.add(matcher.group(i));
        }
        return (groupIndex >= 0 && groupIndex < groups.size()) ? groups.get(groupIndex) : def;
    }
}
