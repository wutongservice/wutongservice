package com.borqs.server.qiupu.tools;


import com.borqs.server.qiupu.ApkId;
import com.borqs.server.qiupu.util.apkinfo.ApkInfo;
import com.borqs.server.qiupu.util.apkinfo.ApkInfoReader;
import com.borqs.server.qiupu.util.sfs.ApkSFS;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class ApkInfoUpdater {

    private String dbUrl;
    private String apkDir;
    private String logDir;

    public ApkInfoUpdater(String dbUrl, String apkDir, String logDir) {
        this.dbUrl = dbUrl;
        this.apkDir = apkDir;
        this.logDir = logDir;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }

        ApkInfoUpdater updater = new ApkInfoUpdater(args[0], args[1], args[2]);
        updater.updateApkInfo();
    }

    private static void printUsage() {
        System.out.println("Usage: ApkInfoUpdater dbUrl apkDir logDir");
    }

    private File getAllApkIdsFile() {
        return new File(FilenameUtils.concat(logDir, "all"));
    }

    private File getCompletedApkIdsFile() {
        return new File(FilenameUtils.concat(logDir, "completed"));
    }

    private File getLogFile() {
        return new File(FilenameUtils.concat(logDir, "log"));
    }

    public void updateApkInfo() throws Exception {
        FileUtils.forceMkdir(new File(logDir));
        final List<String> process = getProcessApkIds();
        executeSql(new ConnectionHandler() {
            @Override
            public Object handle(Connection conn) throws Exception {
                for (String apkId : process) {
                    String status = updateApkInfo(conn, apkId);
                    System.out.println(status + " " + apkId);
                }
                return null;
            }
        });
    }

    private String updateApkInfo(Connection conn, String apkId) {
        ApkId apkId1 = ApkId.parse(apkId);
        String apkFile = ApkSFS.calculatePath(apkDir, apkId + ".apk");

        String status = "";
        ApkInfo apkInfo = null;
        if (!new File(apkFile).exists()) {
            status = "apk_not_exists";
        } else {
            apkInfo = ApkInfoReader.getApkInfoByAapt(new File(apkFile));
            if (apkInfo == null)
                status = "apk_min_target_max_version_error";
        }

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String sql;

            if (apkInfo != null && status.isEmpty()) {
                sql = String.format("update qapk set min_sdk_version=%s, target_sdk_version=%s, max_sdk_version=%s, tag='ok' where package='%s' and version_code=%s and architecture=%s",
                        apkInfo.getMinSdkVersion(), apkInfo.getTargetSdkVersion(), apkInfo.getMaxSdkVersion(),
                        apkId1.package_, apkId1.versionCode, apkId1.arch
                );
            } else {
                sql = String.format("update qapk set tag='%s' where package='%s' and version_code=%s and architecture=%s",
                        status,
                        apkId1.package_, apkId1.versionCode, apkId1.arch
                );
            }
            int n = stmt.executeUpdate(sql);
            if (n == 0)
                status = "db_update_error";
        } catch (Exception e) {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
            }
            status = "db_error";
        }

        try {
            if (status.isEmpty())
                logComplete(apkId, apkInfo);
            else
                logError(apkId);
        } catch (Exception ignored) {
        }

        return status;
    }

    private List<String> getProcessApkIds() throws Exception {
        List<String> all = loadAllApkIds();
        List<String> completed = loadCompletedApkIds();
        HashSet<String> completedSet = new HashSet<String>(completed);
        ArrayList<String> process = new ArrayList<String>();
        for (String apkId : all) {
            if (!completedSet.contains(apkId))
                process.add(apkId);
        }
        return process;
    }

    private List<String> loadAllApkIds() throws Exception {
        File allFile = getAllApkIdsFile();
        if (allFile.exists())
            return FileUtils.readLines(allFile);

        List<String> apkIds = (List<String>) executeSql(new ConnectionHandler() {
            @Override
            public Object handle(Connection conn) throws Exception {
                ArrayList<String> l = new ArrayList<String>();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select package, version_code, architecture from qapk");
                while (rs.next()) {
                    l.add(ApkId.of(rs.getString("package"), rs.getInt("version_code"), rs.getInt("architecture")).toString());
                }
                stmt.close();
                return l;
            }
        });
        FileUtils.writeLines(allFile, apkIds);
        return apkIds;
    }

    private List<String> loadCompletedApkIds() throws Exception {
        File completedFile = getCompletedApkIdsFile();
        if (!completedFile.exists())
            return new ArrayList<String>();

        return FileUtils.readLines(completedFile);
    }

    private void logComplete(String apkId, ApkInfo apkInfo) throws Exception {
        appendLine(getCompletedApkIdsFile(), apkId);
        appendLine(getLogFile(), "OK  " + now() + " - " + apkId + " - " + apkInfo.getAppName() + " - " + apkInfo.getMinSdkVersion() + ":" + apkInfo.getTargetSdkVersion() + ":" + apkInfo.getMaxSdkVersion());
    }

    private void logError(String apkId) throws Exception {
        appendLine(getLogFile(), "ERR " + now() + " - " + apkId);
    }

    private static String now() throws Exception {
        Date now = new java.util.Date(System.currentTimeMillis());
        return now.toString();
    }

    private static void appendLine(File file, String line) throws Exception {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file, true));
            writer.print(line + "\n");
            writer.flush();
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private Object executeSql(ConnectionHandler handler) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(dbUrl);
            return handler.handle(conn);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    private static interface ConnectionHandler {
        Object handle(Connection conn) throws Exception;
    }
}
