package com.borqs.server.qiupu.util.apkinfo;


import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.qiupu.ApkId;
import com.borqs.server.service.qiupu.Qiupu;

import java.util.LinkedHashMap;

public class ApkInfo {
    private String package_;
    private int versionCode;
    private String versionName;
    private String appName;
    private int minSdkVersion;
    private int targetSdkVersion;
    private int maxSdkVersion;
    private String screenSupports;
    private String iconPath;
    private int architecture = Qiupu.ARCH_ARM;
    private long fileSize;
    private byte[] icon;

    public ApkInfo() {
    }

    public String getPackage() {
        return package_;
    }

    public void setPackage(String package_) {
        this.package_ = package_;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getMinSdkVersion() {
        return minSdkVersion;
    }

    public void setMinSdkVersion(int minSdkVersion) {
        this.minSdkVersion = minSdkVersion;
    }

    public int getMaxSdkVersion() {
        return maxSdkVersion;
    }

    public void setMaxSdkVersion(int maxSdkVersion) {
        this.maxSdkVersion = maxSdkVersion;
    }

    public int getTargetSdkVersion() {
        return targetSdkVersion;
    }

    public void setTargetSdkVersion(int targetSdkVersion) {
        this.targetSdkVersion = targetSdkVersion;
    }

    public String getScreenSupports() {
        return screenSupports;
    }

    public void setScreenSupports(String screenSupports) {
        this.screenSupports = screenSupports;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public boolean hasIcon() {
        return icon != null && icon.length > 0;
    }

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    public int getArchitecture() {
        return architecture;
    }

    public void setArchitecture(int architecture) {
        this.architecture = architecture;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public ApkId getApkId() {
        return ApkId.of(package_, versionCode, architecture);
    }

    @Override
    public String toString() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("package", package_);
        m.put("versionCode", versionCode);
        m.put("arch", Qiupu.ARCHS.getText(architecture));
        m.put("versionName", versionName);
        m.put("appName", appName);
        m.put("iconPath", iconPath);
        m.put("minSdkVersion", minSdkVersion);
        m.put("targetSdkVersion", targetSdkVersion);
        m.put("maxSdkVersion", maxSdkVersion);
        m.put("screenSupports", screenSupports);
        m.put("fileSize", fileSize);
        m.put("icon", hasIcon() ? "size " + icon.length : null);
        return JsonUtils.toJson(m, true);
    }
}
