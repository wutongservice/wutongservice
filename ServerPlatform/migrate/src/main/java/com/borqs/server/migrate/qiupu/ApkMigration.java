package com.borqs.server.migrate.qiupu;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.io.FilePair;
import com.borqs.server.base.io.VfsUtils;
import com.borqs.server.base.migrate.MigrateStopException;
import com.borqs.server.base.migrate.handler.CounterMigrateHandler;
import com.borqs.server.base.migrate.input.SQLInput;
import com.borqs.server.base.migrate.output.SQLInsertOutput;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.tools.CopyFiles;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.ThreadUtils;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.migrate.Migration;
import com.borqs.server.qiupu.util.sfs.ApkSFS;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class ApkMigration extends Migration {
    // old: apks
    /*
       +------------------+---------------+------+-----+---------+----------------+
       | Field            | Type          | Null | Key | Default | Extra          |
       +------------------+---------------+------+-----+---------+----------------+
       | id               | bigint(20)    | NO   | PRI | NULL    | auto_increment |
       | apkname          | varchar(255)  | NO   |     | NULL    |                |
       | apkcomponentname | varchar(255)  | NO   | MUL | NULL    |                |
       | apkVersionCode   | int(11)       | NO   |     | -1      |                |
       | apkversionName   | varchar(255)  | NO   |     | NULL    |                |
       | apkdesc          | varchar(2048) | YES  |     | NULL    |                |
       | filesize         | bigint(20)    | NO   |     | NULL    |                |
       | filepath         | varchar(255)  | NO   |     | NULL    |                |
       | categoryid       | bigint(20)    | NO   |     | 0       |                |
       | subcategoryid    | bigint(20)    | NO   |     | 0       |                |
       | downloadtimes    | bigint(20)    | NO   |     | 0       |                |
       | screenshot       | varchar(255)  | YES  |     | NULL    |                |
       | rating           | varchar(20)   | YES  |     | NULL    |                |
       | icon             | varchar(255)  | YES  |     | NULL    |                |
       | updatetime       | bigint(20)    | YES  |     | NULL    |                |
       | borqs            | tinyint(1)    | NO   |     | 0       |                |
       | targetsdk        | int(11)       | YES  |     | NULL    |                |
       | creator          | varchar(255)  | YES  |     | NULL    |                |
       | installtimes     | bigint(20)    | NO   |     | 0       |                |
       | uploader         | varchar(2048) | YES  |     | NULL    |                |
       | uploadtime       | bigint(20)    | YES  |     | NULL    |                |
       +------------------+---------------+------+-----+---------+----------------+
    */

    // new: qapk
    /*
       +--------------------+---------------+------+-----+---------+-------+
       | Field              | Type          | Null | Key | Default | Extra |
       +--------------------+---------------+------+-----+---------+-------+
       | package            | varchar(255)  | NO   | PRI | NULL    |       |
       | enabled            | tinyint(4)    | YES  | MUL | 0       |       |
       | app_name           | varchar(255)  | NO   | MUL | NULL    |       |
       | version_code       | int(11)       | NO   |     | NULL    |       |
       | version_name       | varchar(255)   | YES  |     | NULL    |       |
       | min_sdk_version    | smallint(6)   | YES  | MUL | 0       |       |
       | target_sdk_version | smallint(6)   | YES  |     | 0       |       |
       | max_sdk_version    | smallint(6)   | YES  |     | 0       |       |
       | architecture       | tinyint(4)    | YES  |     | 0       |       |
       | created_time       | bigint(20)    | NO   |     | NULL    |       |
       | destroyed_time     | bigint(20)    | YES  |     | 0       |       |
       | info_updated_time  | bigint(20)    | YES  | MUL | 0       |       |
       | description        | varchar(8192) | YES  |     |         |       |
       | recent_change      | varchar(8192) | YES  |     |         |       |
       | category           | tinyint(4)    | YES  | MUL | 0       |       |
       | sub_category       | tinyint(4)    | YES  | MUL | 0       |       |
       | rating             | varchar(20)   | YES  |     |         |       |
       | download_count     | bigint(20)    | YES  | MUL | 0       |       |
       | install_count      | bigint(20)    | YES  | MUL | 0       |       |
       | uninstall_count    | bigint(20)    | YES  |     | 0       |       |
       | favorite_count     | bigint(20)    | YES  |     | 0       |       |
       | upload_user        | bigint(20)    | YES  |     | 0       |       |
       | screen_support     | varchar(1024) | YES  |     | NULL    |       |
       | icon_url           | varchar(255)  | YES  |     |         |       |
       | price              | float         | YES  |     | 0       |       |
       | borqs              | tinyint(4)    | YES  |     | 0       |       |
       | developer          | varchar(128)  | YES  |     |         |       |
       | developer_email    | varchar(64)   | YES  |     |         |       |
       | developer_phone    | varchar(64)   | YES  |     |         |       |
       | developer_website  | varchar(255)  | YES  |     |         |       |
       | market_url         | varchar(255)  | YES  |     |         |       |
       | other_urls         | varchar(512)  | YES  |     | NULL    |       |
       | file_size          | int(11)       | YES  |     | 0       |       |
       | file_url           | varchar(255)  | YES  |     |         |       |
       | file_md5           | varchar(32)   | YES  |     |         |       |
       | tag                | varchar(255)  | YES  |     |         |       |
       | screenshots_urls   | varchar(1024) | YES  |     |         |       |
       +--------------------+---------------+------+-----+---------+-------+
    */

    @Override
    public void migrate() {
        apk(getOldQiupuDb(), getNewQiupuDb());
        apkFiles(getOldQiupuDb(), getNewQiupuDb(), getOldQiupuDataDir(), getNewQiupuDataDir());
    }

    public static void apk(String inDb, String outDb) {
        SQLExecutor.executeUpdate(getConnectionFactory(), outDb, "DELETE FROM qapk");
        com.borqs.server.base.migrate.Migrate.migrate(
                new SQLInput(getConnectionFactory(), inDb, "select * from apks"),
                new SQLInsertOutput(getConnectionFactory(), outDb, "qapk"),
                new CounterMigrateHandler() {
                    @Override
                    public void handle0(Record in, Record[] out) throws MigrateStopException {
                        Record out1 = out[0];

                        out1.put("package", in.checkGetString("apkcomponentname"));
                        out1.put("enabled", 1);
                        out1.put("app_name", in.checkGetString("apkname"));
                        out1.put("version_code", in.checkGetInt("apkVersionCode"));
                        out1.put("version_name", in.checkGetString("apkversionName"));
                        out1.put("min_sdk_version", 0);
                        out1.put("target_sdk_version", in.checkGetInt("targetsdk"));
                        out1.put("max_sdk_version", 0);
                        out1.put("architecture", Qiupu.ARCH_ARM);
                        out1.put("created_time", in.checkGetInt("uploadtime"));
                        out1.put("destroyed_time", 0);
                        out1.put("info_updated_time", in.checkGetInt("updatetime"));
                        out1.put("description", in.checkGetString("apkdesc"));
                        out1.put("recent_change", "");
                        out1.put("category", in.checkGetInt("categoryid"));
                        out1.put("sub_category", in.checkGetInt("subcategoryid"));
                        out1.put("rating", in.checkGet("rating"));
                        out1.put("download_count", in.checkGetInt("downloadtimes"));
                        out1.put("install_count", in.checkGetInt("installtimes"));
                        out1.put("uninstall_count", 0);
                        out1.put("favorite_count", 0);
                        out1.put("upload_user", JsonUtils.parse(in.checkGetString("uploader")).get("id").getLongValue());
                        out1.put("screen_support", "");
                        out1.put("icon_url", "");
                        out1.put("price", 0);
                        out1.put("borqs", in.checkGetInt("borqs"));
                        out1.put("developer", StringUtils.trimToEmpty(in.checkGetString("creator")));
                        out1.put("developer_email", "");
                        out1.put("developer_phone", "");
                        out1.put("developer_website", "");
                        out1.put("market_url", "");
                        out1.put("other_urls", "[]");
                        out1.put("file_size", in.checkGetInt("filesize"));
                        out1.put("file_url", "");
                        out1.put("file_md5", "");
                        out1.put("tag", "");
                        out1.put("screenshots_urls", "[]");

                        if (counter() % 100 == 0)
                            System.out.println(counter());
                    }
                });
    }


    public void apkFiles(String inDb, String outDb, String inDir, String outDir) {
        final String UPDATE_FILE_URL_SQL = "UPDATE qapk SET file_url=${v(file_url)} WHERE package=${v(package)} AND version_code=${v(version_code)}";
        final String UPDATE_ICON_URL_SQL = "UPDATE qapk SET icon_url=${v(icon_url)} WHERE package=${v(package)} AND version_code=${v(version_code)}";
        final String UPDATE_SCREENSHOTS_URLS_SQL = "UPDATE qapk SET screenshots_urls=${v(screenshots_urls)} WHERE package=${v(package)} AND version_code=${v(version_code)}";


        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), inDb, "SELECT apkcomponentname, apkVersionCode, filepath, icon, screenshot FROM apks", null);
        int seq = 0;
        ArrayList<FilePair> filePairs = new ArrayList<FilePair>();
        for (Record rec : recs) {
            String inApkPath = dirJoin(inDir, trimPath(rec.getString("filepath")));
            String inIconPath = dirJoin(inDir, trimPath(rec.getString("icon")));
            String[] inScreenshotPaths = getInScreenshotPaths(inDir, trimPath(rec.getString("screenshot", "")));
            String sql;

            String outApkPath = ApkSFS.calculatePath(outDir, makeApkId(rec) + ".apk");
            copyFile(filePairs, seq++, inApkPath, outApkPath);
            sql = SQLTemplate.merge(UPDATE_FILE_URL_SQL, "file_url", getFileName(outApkPath), "package", rec.checkGetString("apkcomponentname"), "version_code", rec.checkGetInt("apkVersionCode"));
            //System.out.println(sql);
            SQLExecutor.executeUpdate(getConnectionFactory(), outDb, sql);

            if (rec.getString("icon", null) != null) {
                String outIconPath = ApkSFS.calculatePath(outDir, makeApkId(rec) + ".icon." + getImageExt(rec.getString("icon")));
                copyFile(filePairs, seq++, inIconPath, outIconPath);
                sql = SQLTemplate.merge(UPDATE_ICON_URL_SQL, "icon_url", getFileName(outIconPath), "package", rec.checkGetString("apkcomponentname"), "version_code", rec.checkGetInt("apkVersionCode"));
                //System.out.println(sql);
                SQLExecutor.executeUpdate(getConnectionFactory(), outDb, sql);
            }

            ArrayList<String> outScreenshotPaths = new ArrayList<String>();
            for (int i = 0; i < inScreenshotPaths.length; i++) {
                String inScreenshotPath = inScreenshotPaths[i];
                String outScreenshotPath = ApkSFS.calculatePath(outDir, makeApkId(rec) + ".screenshot" + (i + 1) + "." + getImageExt(inScreenshotPath));
                int r = copyFile(filePairs, seq++, inScreenshotPath, outScreenshotPath);
                if (r != COPY_ERROR)
                    outScreenshotPaths.add(getFileName(outScreenshotPath));
            }
            sql = SQLTemplate.merge(UPDATE_SCREENSHOTS_URLS_SQL, "screenshots_urls", JsonUtils.toJson(outScreenshotPaths, false), "package", rec.checkGetString("apkcomponentname"), "version_code", rec.checkGetInt("apkVersionCode"));
            //System.out.println(sql);
            SQLExecutor.executeUpdate(getConnectionFactory(), outDb, sql);
        }
    }

    private static String getFileName(String s) {
        return StringUtils.substringAfterLast(s, "/");
    }

    final int COPY_IGNORED = 0;
    final int COPY_OK = 1;
    final int COPY_ERROR = 2;

    private int copyFile(List<FilePair> filePairs, int seq, String in, String out) {
        try {
            System.out.printf("[%5d] %s   =>   %s\n", seq, in, out);
            boolean r = VfsUtils.copyFile(in, out, false);

            if (r) {
                System.out.printf("[%5d] OK\n", seq);
            } else {
                System.out.printf("[%5d] Ignore\n", seq);
            }
            return r ? COPY_OK : COPY_IGNORED;
        } catch (Throwable t) {
            if (!getCopyErrorResume()) {
                throw new RuntimeException(t);
            } else {
                System.out.printf("[%5d] ERROR\n", seq);
                t.printStackTrace(System.out);
                return COPY_ERROR;
            }
        }

    }

    private static String dirJoin(String dir, String file) {
        return StringUtils.isEmpty(file) ? null : dir + file;
    }

    private static String[] getInScreenshotPaths(String inDir, String screenshot) {
        if (StringUtils.isBlank(screenshot) || StringUtils.equals(screenshot, "null"))
            return new String[0];

        String file = StringUtils.substringBeforeLast(screenshot, ".");
        String ext = StringUtils.substringAfterLast(screenshot, ".");

        String base = StringUtils.substringBeforeLast(file, "_");
        int n = Integer.parseInt(StringUtils.substringAfterLast(file, "_"));
        ArrayList<String> l = new ArrayList<String>();
        for (int i = 1; i <= n; i++) {
            l.add(dirJoin(inDir, String.format("%s_%s.%s", base, i, ext)));
        }
        return l.toArray(new String[l.size()]);
    }

    private static String makeApkId(Record rec) {
        return StringUtils2.join("-", rec.getString("apkcomponentname"), rec.getString("apkVersionCode"), "arm");
    }

    private static String getImageExt(String url) {
        String ext = FilenameUtils.getExtension(url);
        if (ext.equalsIgnoreCase("png"))
            return "png";
        else if (ext.equalsIgnoreCase("jpg"))
            return "jpg";
        else if (ext.equalsIgnoreCase("apk"))
            return "png";
        else
            throw new IllegalArgumentException();
    }
}
