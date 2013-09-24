package com.borqs.server.qiupu;


import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public class ApkId {
    public static final int MAX_VERSION_CODE = -1;

    public final String package_;
    public final int versionCode;
    public final int arch;

    private ApkId(String package_, int versionCode, int arch) {
        this.package_ = package_;
        this.versionCode = versionCode;
        this.arch = arch;
    }

    public boolean isMaxVersion() {
        return versionCode == MAX_VERSION_CODE;
    }

    public boolean isArmArch() {
        return arch == Qiupu.ARCH_ARM;
    }

    public boolean isX86Arch() {
        return arch == Qiupu.ARCH_X86;
    }

    public static ApkId of(String package_, int versionCode, int arch) {
        return new ApkId(package_, versionCode, arch);
    }

    public static boolean isValid(String s) {
        String[] ss = StringUtils2.splitArray(StringUtils.trimToEmpty(s), "-", true);
        return (ss.length >= 1 && ss.length <= 3);
    }

    public static ApkId parse(String s) {
        String[] ss = StringUtils.split(StringUtils.trimToEmpty(s), "-", 3);

        String package_ = ss[0].trim();
        Validate.notEmpty(package_);

        int versionCode;
        int arch;
        if (ss.length == 1) {
            versionCode = MAX_VERSION_CODE;
            arch = Qiupu.ARCH_ARM;
        } else if (ss.length == 2) {
            versionCode = parseVersionCode(ss[1].trim());
            arch = Qiupu.ARCH_ARM;
        } else if (ss.length == 3) {
            versionCode = parseVersionCode(ss[1].trim());
            String def = "arm";
            if (ss[2].trim().equals("arm") || ss[2].trim().equals("x86")) {
                def = ss[2].trim();
            }
            arch = parseArch(def);
        } else {
            throw new IllegalArgumentException();
        }
        return new ApkId(package_, versionCode, arch);
    }

    @Override
    public String toString() {
        return StringUtils2.join("-", package_,
                isMaxVersion() ? "max" : Integer.toString(versionCode),
                Qiupu.ARCHS.getText(arch));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ApkId apkId = (ApkId) o;
        return StringUtils.equals(package_, apkId.package_)
                && versionCode == apkId.versionCode
                && arch == apkId.arch;
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.hashCode(package_);
        result = 31 * result + versionCode;
        result = 31 * result + arch;
        return result;
    }

    private static int parseVersionCode(String s) {
        return s.equalsIgnoreCase("max") ? MAX_VERSION_CODE : Integer.parseInt(s);
    }

    private static int parseArch(String s) {
        Integer arch = Qiupu.ARCHS.getValue(s);
        if (arch == null) {
            if (StringUtils.containsIgnoreCase(s, "arm"))
                return Qiupu.ARCH_ARM;
            else
                throw new IllegalArgumentException("arch is " + s);
        }

        return arch;
    }
}
