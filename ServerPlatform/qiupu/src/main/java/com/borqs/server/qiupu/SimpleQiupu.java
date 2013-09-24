package com.borqs.server.qiupu;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SimpleQiupu extends QiupuBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String qapkTable = "qapk";
    private String qapkHistoryTable = "qapk_history";
    private String qapkModelTable = "qapk_model";
    private String userQappTable = "user_qapp";
    private String userQsettingTable = "user_qsetting";
    private String qapkNotinPoolTable = "qapk_notin_pool";
    private String qapkManualTable = "qapk_manual";
    private String qapkSuggestTable = "qapk_suggest";
    private String updateApkLessDescTable = "updateApkLessDesc";
    //private String profileImagePattern = "http://cloud.borqs.com/borqsusercenter";
    private String qiupuUid = "10002";

    public SimpleQiupu() {
    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("qiupu.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("qiupu.simple.db", null);
        this.qapkTable = conf.getString("qiupu.simple.qapkTable", "qapk");
        this.qapkHistoryTable = conf.getString("qiupu.simple.qapkTable", "qapk_history");
        this.qapkModelTable = conf.getString("qiupu.simple.qapkTable", "qapk_model");
        this.userQappTable = conf.getString("qiupu.simple.qapkTable", "user_qapp");
        this.userQsettingTable = conf.getString("qiupu.simple.userQsettingTable", "user_qsetting");
        this.qapkNotinPoolTable = conf.getString("qiupu.simple.qapkNotinPoolTable", "qapk_notin_pool");
        this.qapkManualTable = conf.getString("qiupu.simple.qapkManualTable", "qapk_manual");
        this.qapkSuggestTable = conf.getString("qiupu.simple.qapkSuggestTable", "qapk_suggest");
        this.updateApkLessDescTable = conf.getString("qiupu.simple.updateApkLessDescTable", "updateApkLessDesc");
        //this.profileImagePattern = conf.getString("account.profileImagePattern", "http://cloud.borqs.com/borqsusercenter");
        this.qiupuUid = conf.getString("qiupu.uid", "10002");
    }

    @Override
    public void destroy() {
        this.qapkTable = null;
        this.qapkHistoryTable = null;
        this.qapkModelTable = null;
        this.userQappTable = null;
        this.userQsettingTable = null;
        this.qapkNotinPoolTable = null;
        this.qapkManualTable = null;
        this.qapkSuggestTable = null;
        this.updateApkLessDescTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected RecordSet getApps0(String sub_category, boolean paid, String sort, List<String> cols, int page, int count,int minSDK) {
        Map<String, String> alias = qapkSchema.getAllAliases();
        StringBuilder sql = new StringBuilder();

        if (sort.equals("download")) {
            sql.append(SQLTemplate.merge("SELECT SUM(${alias.download_count}) AS COUNT,${alias.package} FROM ${table} WHERE destroyed_time = 0 ",
                    "alias", alias, "table", qapkTable));

            if (Integer.parseInt(sub_category) > 0) {
                if (Integer.parseInt(sub_category) == 9527) {
                    sql.append(SQLTemplate.merge(" AND ${alias.borqs}=1",
                            "alias", alias));
                } else {
                    if (Integer.parseInt(sub_category) == 256) {
                        sql.append(SQLTemplate.merge(" AND ${alias.sub_category}>255 AND ${alias.sub_category}<512",
                                "alias", alias, "sub_category", sub_category));
                    } else if (Integer.parseInt(sub_category) == 512) {
                        sql.append(SQLTemplate.merge(" AND ${alias.sub_category}>512",
                                "alias", alias, "sub_category", sub_category));
                    } else {
                        sql.append(SQLTemplate.merge(" AND ${alias.sub_category}=${sub_category}",
                                "alias", alias, "sub_category", sub_category));
                    }
                }
            }

            if (paid) {
                sql.append(SQLTemplate.merge(" AND ${alias.price}>0",
                        "alias", alias));
            }
            sql.append(SQLTemplate.merge("  GROUP BY ${alias.package} order by COUNT DESC ${limit}",
                    "alias", alias, "limit", SQLUtils.pageToLimit(page, count)));
        } else {
            sql.append(SQLTemplate.merge("SELECT DISTINCT(${alias.package}) FROM ${table} WHERE destroyed_time = 0",
                    "alias", alias, "table", qapkTable));

            if (Integer.parseInt(sub_category) > 0) {
                if (Integer.parseInt(sub_category) == 256) {
                    sql.append(SQLTemplate.merge(" AND ${alias.sub_category}>255 AND ${alias.sub_category}<512",
                            "alias", alias, "sub_category", sub_category));
                } else if (Integer.parseInt(sub_category) == 512) {
                    sql.append(SQLTemplate.merge(" AND ${alias.sub_category}>512",
                            "alias", alias, "sub_category", sub_category));
                } else {
                    sql.append(SQLTemplate.merge(" AND ${alias.sub_category}=${sub_category}",
                            "alias", alias, "sub_category", sub_category));
                }
            }

            if (paid) {
                sql.append(SQLTemplate.merge(" AND ${alias.price}>0",
                        "alias", alias));
            }

            if (sort.equals("rating")) {
                sql.append(SQLTemplate.merge(" ORDER BY ${alias.rating} DESC ${limit}",
                        "alias", alias, "limit", SQLUtils.pageToLimit(page, count)));
            } else {
                sql.append(SQLTemplate.merge(" ORDER BY ${alias.created_time} DESC ${limit}",
                        "alias", alias, "limit", SQLUtils.pageToLimit(page, count)));
            }
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return returnPointVersionApp(recs, cols,minSDK);
    }

    @Override
    protected Record getAppsFromApkIds0(ApkId apkId, List<String> cols,int minSDK) {
        Map<String, String> alias = qapkSchema.getAllAliases();
        StringBuilder sql = new StringBuilder();

        List<String> l = new ArrayList<String>();

        if (!cols.contains("architecture")) {
            l.add("architecture");
        }
        if (!cols.contains("version_code")) {
            l.add("version_code");
        }
        if (!cols.contains("package")) {
            l.add("package");
        }
        l.addAll(cols);

        sql.append(SQLTemplate.merge("SELECT ${as_join(alias, cols)} FROM ${table} WHERE destroyed_time = 0 AND min_sdk_version<="+minSDK+" ",
                "alias", alias, "cols", l, "table", qapkTable));

        sql.append(SQLTemplate.merge(" AND ${alias.package}=${v(package)}",
                "alias", alias, "package", apkId.package_));
        sql.append(SQLTemplate.merge(" AND ${alias.version_code}=${v(version_code)}",
                "alias", alias, "version_code", apkId.versionCode));

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        if (rec != null && !rec.isEmpty()) {
            //rec.put("apk_id", rec.getString("package") + "-" + rec.getString("version_code") + "-" + rec.getString("architecture"));
            rec.put("apk_id", ApkId.of(rec.getString("package"), (int) rec.getInt("version_code"), (int) rec.getInt("architecture")).toString());

            //count
            if (cols.contains("download_count")) {
                final String SQLCount = "SELECT sum(${alias.download_count}) AS download_count FROM ${table} WHERE ${alias.package}=${v(packageName)}";
                String sqlCount = SQLTemplate.merge(SQLCount, new Object[][]{
                            {"alias", qapkSchema.getAllAliases()},
                            {"table", qapkTable},
                            {"packageName", apkId.package_},});
                Record rec1 = se.executeRecord(sqlCount.toString(), null);
                rec.removeColumns("download_count");
                rec.put("download_count", rec1.getString("download_count"));
            }
            if (cols.contains("install_count")) {
                final String SQLCount = "SELECT sum(${alias.install_count}) AS install_count FROM ${table} WHERE ${alias.package}=${v(packageName)}";
                String sqlCount = SQLTemplate.merge(SQLCount, new Object[][]{
                            {"alias", qapkSchema.getAllAliases()},
                            {"table", qapkTable},
                            {"packageName", apkId.package_},});
                Record rec1 = se.executeRecord(sqlCount.toString(), null);
                rec.removeColumns("install_count");
                rec.put("install_count", rec1.getString("install_count"));
            }
            if (cols.contains("uninstall_count")) {
                final String SQLCount = "SELECT sum(${alias.uninstall_count}) AS uninstall_count FROM ${table} WHERE ${alias.package}=${v(packageName)}";
                String sqlCount = SQLTemplate.merge(SQLCount, new Object[][]{
                            {"alias", qapkSchema.getAllAliases()},
                            {"table", qapkTable},
                            {"packageName", apkId.package_},});
                Record rec1 = se.executeRecord(sqlCount.toString(), null);
                rec.removeColumns("uninstall_count");
                rec.put("uninstall_count", rec1.getString("uninstall_count"));
            }
            if (cols.contains("favorite_count")) {
                final String SQLCount = "SELECT sum(${alias.favorite_count}) AS favorite_count FROM ${table} WHERE ${alias.package}=${v(packageName)}";
                String sqlCount = SQLTemplate.merge(SQLCount, new Object[][]{
                            {"alias", qapkSchema.getAllAliases()},
                            {"table", qapkTable},
                            {"packageName", apkId.package_},});
                Record rec1 = se.executeRecord(sqlCount.toString(), null);
                rec.removeColumns("favorite_count");
                rec.put("favorite_count", rec1.getString("favorite_count"));
            }

            if (!cols.contains("architecture")) {
                rec.remove("architecture");
            }
            if (!cols.contains("version_code")) {
                rec.remove("version_code");
            }
            if (!cols.contains("package")) {
                rec.remove("package");
            }
            Schemas.standardize(qapkSchema, rec);
        }
        return rec;
    }

    @Override
    protected RecordSet returnPointVersionApp(RecordSet inApkRecordSet, List<String> cols,int minSDK) {
        RecordSet outRecs = new RecordSet();
        if (inApkRecordSet.size() > 0) {
            for (Record r : inApkRecordSet) {
                String ap = r.getString("package");
                String vc, ar;
                //if record has vc andar，then find point apk，eventhou find max version apk
                if (r.getInt("version_code", -1) >= 0 && r.getInt("architecture", -1) >= 0) {
                    vc = String.valueOf(r.getInt("version_code"));
                    ar = String.valueOf(r.getInt("architecture"));
                } else {
                    vc = String.valueOf(getMaxVersionCodeFromApp(r.getString("package"),minSDK));
                    ar = String.valueOf("arm");
                }
                ApkId a = ApkId.parse(ap + "-" + vc + "-" + ar);
                Record rec = getAppsFromApkIds0(a, cols,minSDK);

                if (cols.contains("app_last_version_code") || cols.contains("app_last_version_name")) {
                    // TODO: add here
                }
                if (!rec.isEmpty())
                    outRecs.add(rec);

            }
        }
        return outRecs;
    }
    
    @Override
    protected RecordSet findApkByPackageAndVersion0(String packageName,String versionCode) {
         final String SQL = "SELECT * FROM ${table} "
                    + "WHERE ${alias.package}=${v(package)} AND ${alias.version_code}=${v(versionCode)} AND ${alias.destroyed_time}=0 ORDER BY ${alias.version_code} DESC";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", qapkSchema.getAllAliases()},
                    {"table", qapkTable},
                    {"package", packageName},
                    {"versionCode", versionCode},});

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet getUserApks0(String userId, int max, String reason, int page, int count,String delApks,int minSDK) {
        Map<String, String> alias = userQappSchema.getAllAliases();
        Map<String, String> alias1 = qapkSchema.getAllAliases();
        StringBuilder sql = new StringBuilder();
        sql.append(SQLTemplate.merge("SELECT DISTINCT(${alias.package}),${alias.version_code} FROM ${table} WHERE ${alias.user}=${v(userId)}",
                "alias", alias, "userId", userId, "table", userQappTable));

        sql.append(SQLTemplate.merge("AND ${alias.privacy}<=${max}",
                "alias", alias, "max", max));

        if (!delApks.equals("")) {
            List<String> apk_l0 = new ArrayList<String>();
            List<String> apk_l1 = StringUtils2.splitList(toStr(delApks), ",", true);
            for (String apk : apk_l1) {
                apk_l0.add(apk.split("-")[0]);
            }
            sql.append(SQLTemplate.merge(" AND ${alias.package} NOT IN (${vjoin(apps)})",
                    "alias", alias, "apps", apk_l0));
        }


        sql.append(SQLTemplate.merge(" AND ${alias.package} IN (SELECT DISTINCT(${alias1.package}) FROM ${table1}  where min_sdk_version<="+minSDK+" ) ",
                "alias", alias, "alias1", alias1, "table1", qapkTable));

        if (reason.isEmpty() || reason.equals("")) {
        } else {
            sql.append(SQLTemplate.merge(" AND ${alias.reason}&${reason}<>0",
                    "alias", alias, "reason", reason));
        }
        sql.append(SQLTemplate.merge(" GROUP BY ${alias.package} ${limit}",
                "alias", alias, "limit", SQLUtils.pageToLimit(page, count)));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        for (Record r : recs) {
            r.put("version_code", findUserMaxVersionCodeFromPackage0(r.getString("package"), userId,minSDK));
        }
        return recs;
    }
    
    protected int findUserMaxVersionCodeFromPackage0(String packageName, String userId,int minSDK) {
        final String SQL = "SELECT ${alias.version_code} FROM ${table} WHERE ${alias.package}=${v(packageName)} AND ${alias.user}=${v(userId)} AND version_code in (select version_code from qapk where package='"+packageName+"' and min_sdk_version<="+minSDK+")  ORDER BY ${alias.version_code} DESC LIMIT 1";
        String sql = SQLTemplate.merge(SQL,
                "table", userQappTable,
                "alias", userQappSchema.getAllAliases(),
                "packageName", packageName,
                "userId", userId);
        SQLExecutor se = getSqlExecutor();
        int a = (int) se.executeIntScalar(sql, 0);
        return a;
    }

    @Override
    protected RecordSet getLinkedAppUsers0(String viewerId,String app, String reason,String friendIds, int page, int count,int userType) {
        //userType 0-all 1-myfriend 2-notmyfriend
        Map<String, String> alias = userQappSchema.getAllAliases();
        StringBuilder sql = new StringBuilder();
        if (!viewerId.equals("") && userType==1){
            if (friendIds.length()<=0)
                friendIds = viewerId;
            sql.append(SQLTemplate.merge("SELECT DISTINCT(${alias.user}) FROM ${table} WHERE ${alias.package}=${v(app)} and user<>0 and user in ("+friendIds+")",
                "alias", alias, "table", userQappTable, "app", app));
        }
        else if (!viewerId.equals("") && userType==2){
            if (friendIds.length()<=0)
                friendIds = viewerId;
            sql.append(SQLTemplate.merge("SELECT DISTINCT(${alias.user}) FROM ${table} WHERE ${alias.package}=${v(app)} and user<>0 and user not in ("+friendIds+")",
                "alias", alias, "table", userQappTable, "app", app));
        }
        else
        {
            sql.append(SQLTemplate.merge("SELECT DISTINCT(${alias.user}) FROM ${table} WHERE ${alias.package}=${v(app)} and user<>0 ",
                "alias", alias, "table", userQappTable, "app", app));
        }

        if (!reason.isEmpty()) {
            sql.append(SQLTemplate.merge(" AND ${alias.reason}&${reason}<>0",
                    "alias", alias, "reason", reason));
        }

        sql.append(SQLTemplate.merge(" ${limit}",
                "alias", alias, "limit", SQLUtils.pageToLimit(page, count)));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected boolean setUserAppPrivince0(String userId, String app, String value) {
        final String SQL = "UPDATE ${table} SET ${alias.privacy}=${v(value)} WHERE ${alias.user}=${v(userId)} AND ${alias.package}=${v(app)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQappSchema.getAllAliases()},
                    {"value", value},
                    {"table", userQappTable},
                    {"userId", userId},
                    {"app", app},});

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean setUserAppPrivinceAll0(String userId, String value) {
        final String SQL = "UPDATE ${table} SET ${alias.privacy}=${v(value)} WHERE ${alias.user}=${v(userId)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQappSchema.getAllAliases()},
                    {"value", value},
                    {"table", userQappTable},
                    {"userId", userId},});

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean setUserQapp0(String userId, String apps, String reason) {
        final String SQL = "UPDATE ${table} SET ${alias.reason}=${alias.reason}&(~${v(reason)}) WHERE ${alias.package} IN (${v(apps)}) AND ${alias.user}=${userId}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQappSchema.getAllAliases()},
                    {"userId", userId},
                    {"table", userQappTable},
                    {"apps", apps},
                    {"reason", reason},});

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected int getReasonFromApp0(String userId, String app) {
        final String SQL = "SELECT ${alias.reason} FROM ${table} WHERE ${alias.package}=${v(app)} AND ${alias.user}=${v(user)} LIMIT 1";

        String sql = SQLTemplate.merge(SQL,
                "table", userQappTable, "alias", userQappSchema.getAllAliases(),
                "app", app, "user", userId);
        SQLExecutor se = getSqlExecutor();
        int a = (int) se.executeIntScalar(sql, 0);
        return a;
    }

    @Override
    protected boolean deleteUserQapp0(String userId, String apps) {
        if (StringUtils.isBlank(apps))
            return true;

        List<String> l = StringUtils2.splitList(apps, ",", true);
        ArrayList<String> apps0 = new ArrayList<String>();
        for (String s : l)
            apps0.add(ApkId.parse(s).package_);

        final String SQL = "DELETE FROM ${table} WHERE ${alias.package} IN (${vjoin(apps)}) AND ${alias.user}=${userId}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQappSchema.getAllAliases()},
                    {"userId", userId},
                    {"table", userQappTable},
                    {"apps", apps0},});

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet searchApps0(String value, List<String> cols, int page, int count, int minSDK) {
        Map<String, String> alias = qapkSchema.getAllAliases();
        StringBuilder sql = new StringBuilder();
        sql.append(SQLTemplate.merge("SELECT SUM(${alias.download_count}) as count1,${alias.package} FROM ${table} WHERE destroyed_time = 0 ",
                "alias", alias, "cols", cols, "table", qapkTable));

        sql.append(SQLTemplate.merge(" AND INSTR(${alias.app_name},'" + value + "')>0",
                "alias", alias));
        sql.append(SQLTemplate.merge(" group by ${alias.package} ORDER BY count1 DESC ${limit}",
                "alias", alias, "limit", SQLUtils.pageToLimit(page, count)));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);

        if (recs.size() < count) {
            StringBuilder sql1 = new StringBuilder();

            sql1.append(SQLTemplate.merge("SELECT SUM(${alias.download_count}) as count1,${alias.package} FROM ${table} WHERE destroyed_time = 0 ",
                    "alias", alias, "cols", cols, "table", qapkTable));

            sql1.append(SQLTemplate.merge(" AND INSTR(${alias.package},'" + value + "')>0",
                    "alias", alias));
            sql1.append(SQLTemplate.merge(" group by ${alias.package} ORDER BY count1 DESC ${limit}",
                    "alias", alias, "limit", SQLUtils.pageToLimit(0, count - recs.size())));

            RecordSet recs_t = se.executeRecordSet(sql1.toString(), null);
            recs.addAll(recs_t);
            recs.unique("package") ;
        }

        return returnPointVersionApp(recs, cols, minSDK);
    }

    @Override
    protected Record getUserLinkApps0(String userId, String packageName) {
        Map<String, String> alias = userQappSchema.getAllAliases();
        StringBuilder sql = new StringBuilder();
        sql.append(SQLTemplate.merge("SELECT * FROM ${table} WHERE ${alias.user}=${v(userId)} AND ${alias.package}=${v(packageName)} ORDER BY ${alias.version_code} DESC LIMIT 1",
                "alias", alias, "userId", userId, "packageName", packageName, "table", userQappTable));
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs.getFirstRecord();
    }

    @Override
    protected int getMaxVersionCodeFromApp(String app,int minSDK) {
        final String SQL = "SELECT MAX(${alias.version_code}) FROM ${table} WHERE ${alias.package}=${v(app)} and min_sdk_version<="+minSDK+" ";

        String sql = SQLTemplate.merge(SQL,
                "table", qapkTable, "alias", qapkSchema.getAllAliases(),
                "app", app);
        SQLExecutor se = getSqlExecutor();
        int a = (int) se.executeIntScalar(sql, 0);
        return a;
    }
    
    private String getAppName(String package_, int versionCode, long arch)
    {
    	final String SQL = "SELECT ${alias.app_name} FROM ${table} WHERE ${alias.package}=${v(package)} " +
    			"AND ${alias.version_code}=${v(version)} AND ${alias.architecture}=${v(arch)}";
    	
    	String sql = SQLTemplate.merge(SQL, new Object[][]{
    			{"table", qapkTable}, 
    			{"alias", qapkSchema.getAllAliases()},
    			{"package", package_},
    			{"version", versionCode},
    			{"arch", arch}
    	});
        SQLExecutor se = getSqlExecutor();        
        return (String)se.executeScalar(sql);
    }

    @Override
    protected boolean updateQappCount0(String packageName, String version_code, String column, long userId, String deviceId) {
            	
    	Map<String, String> alias = qapkSchema.getAllAliases();
        final String sql = SQLTemplate.merge("UPDATE ${table} SET " + column + "=" + column + "+1 WHERE ${alias.package}=${v(app)} AND ${alias.version_code}=${v(version_code)}",
                "alias", alias, "table", qapkTable, "app", packageName, "version_code", version_code);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean saveUserSetting0(String userId, String name, String value) {
        final String sql = SQLTemplate.merge("INSERT INTO ${table} VALUES(${v(user)},${v(name)},${v(value)})",
                "table", userQsettingTable, "name", name, "user", userId, "value", value);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
 
    @Override
    protected boolean updateUserSetting0(String userId, String name, String value) {
        final String SQL = "UPDATE ${table} SET ${alias.setting_value}=${value} WHERE ${alias.user}=${v(user)} AND ${alias.setting_name}=${v(name)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQsettingSchema.getAllAliases()},
                    {"value", value},
                    {"table", userQsettingTable},
                    {"user", userId},
                    {"name", name},});

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected String getUserSettingAll0(String userId, String name) {
        String SQL = "";
        String sql = "";
        SQL = "SELECT ${alias.setting_value} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.setting_name}=${v(setting_name)}";
        sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQsettingSchema.getAllAliases()},
                    {"table", userQsettingTable},
                    {"user", userId},
                    {"setting_name", name},});
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs.size() <= 0 ? "0" : recs.getFirstRecord().getString("setting_value");
    }

    @Override
    protected String getUserSingleAppPrivancy0(String userId, String app) {
        String SQL = "";
        String sql = "";
        SQL = "SELECT ${alias.privacy} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.package}=${v(package)}";
        sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQappSchema.getAllAliases()},
                    {"table", userQappTable},
                    {"user", userId},
                    {"package", app},});
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs.size() <= 0 ? "0" : recs.getFirstRecord().getString("privacy");
    }

    @Override
    protected String getLastedDesc0(String app) {
        String SQL = "";
        String sql = "";
        SQL = "SELECT ${alias.description} FROM ${table} WHERE ${alias.description}<>'' AND ${alias.package}=${v(package)} order by ${alias.version_code} DESC limit 1";
        sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", qapkSchema.getAllAliases()},
                    {"table", qapkTable},
                    {"package", app},});
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs.size() <= 0 ? "" : recs.getFirstRecord().getString("description");
    }

    @Override
    protected String getLastedScreenshotsUrls0(String app) {
        String SQL = "";
        String sql = "";
        SQL = "SELECT ${alias.screenshots_urls} FROM ${table} WHERE LENGTH(${alias.screenshots_urls})>3 AND ${alias.package}=${v(package)} order by ${alias.version_code} DESC limit 1";
        sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", qapkSchema.getAllAliases()},
                    {"table", qapkTable},
                    {"package", app},});
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs.size() <= 0 ? "" : recs.getFirstRecord().getString("screenshots_urls");
    }

    @Override
    protected boolean existUserSettingAll0(String userId, String name) {
        final String SQL = "SELECT COUNT(*) FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.setting_name}=${v(name)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQsettingSchema.getAllAliases()},
                    {"table", userQsettingTable},
                    {"user", userId},
                    {"name", name},});

        SQLExecutor se = getSqlExecutor();
        Number count = (Number) se.executeScalar(sql);
        return count.intValue() > 0;
    }

    @Override
    protected boolean existUserLinkedApp0(String userId, String app, String reason) {
        String SQL = "";
        String sql = "";
        if (reason.equals("")) {
            SQL = "SELECT COUNT(*) FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.package}=${v(app)}";
            sql = SQLTemplate.merge(SQL, new Object[][]{
                        {"alias", userQappSchema.getAllAliases()},
                        {"table", userQappTable},
                        {"user", userId},
                        {"app", app},});
        } else {
            SQL = "SELECT COUNT(*) FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.package}=${v(app)} AND ${alias.reason}&${v(reason)}<>0";
            sql = SQLTemplate.merge(SQL, new Object[][]{
                        {"alias", userQappSchema.getAllAliases()},
                        {"table", userQappTable},
                        {"user", userId},
                        {"reason", reason},
                        {"app", app},});
        }
        SQLExecutor se = getSqlExecutor();
        Number count = (Number) se.executeScalar(sql);
        return count.intValue() > 0;
    }

    @Override
    protected boolean existPackage0(String packageName) {
        String SQL = "";
        String sql = "";
        SQL = "SELECT COUNT(*) FROM ${table} WHERE ${alias.package}=${v(packageName)} AND destroyed_time=0";
        sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", qapkSchema.getAllAliases()},
                    {"table", qapkTable},
                    {"packageName", packageName},});
        SQLExecutor se = getSqlExecutor();
        Number count = (Number) se.executeScalar(sql);
        return count.intValue() > 0;
    }
    
    protected Record existUserActions(String packageName, String user_id, String deviceid, String reason) {
        String SQL = "";
        String sql = "";
        if (!user_id.equals("")) {
            SQL = "SELECT ${alias.package} FROM ${table} WHERE ${alias.package}=${v(packageName)}"
                    + " AND ${alias.user}=${v(user)} AND ${alias.reason}&${v(reason)}<>0 ORDER BY ${alias.version_code} DESC LIMIT 1";
            sql = SQLTemplate.merge(SQL, new Object[][]{
                        {"alias", userQappSchema.getAllAliases()},
                        {"table", userQappTable},
                        {"user", user_id},
                        {"reason", reason},
                        {"packageName", packageName},});
            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql.toString(), null);
            return recs.size() <= 0 ? new Record() : recs.getFirstRecord();
        } else if (user_id.equals("") && !deviceid.equals("")) {
            SQL = "SELECT ${alias.package} FROM ${table} WHERE ${alias.package}=${v(packageName)}"
                    + " AND ${alias.device}=${v(deviceid)} AND ${alias.reason}&${v(reason)}<>0 ORDER BY ${alias.version_code} DESC LIMIT 1";
            sql = SQLTemplate.merge(SQL, new Object[][]{
                        {"alias", userQappSchema.getAllAliases()},
                        {"table", userQappTable},
                        {"deviceid", deviceid},
                        {"reason", reason},
                        {"packageName", packageName},});
            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql.toString(), null);
            return recs.size() <= 0 ? new Record() : recs.getFirstRecord();
        } else {
            return new Record();
        }
    }

    protected boolean doUpdateCount(String packageName, String version_code, String user_id, String deviceid, String action) {
        int reason = Qiupu.REASON_INSTALLED;
        switch (Integer.valueOf(action)) {
            case Qiupu.ACTION_DOWNLOAD: {
                reason = Qiupu.REASON_DOWNLOADED;
                try {
                    if (existUserActions(packageName, user_id, deviceid, String.valueOf(reason)).isEmpty()) {
                        updateQappCount0(packageName, version_code, "download_count", Long.parseLong(user_id), deviceid);
                    }
                } catch (Exception t) {
                }
                break;
            }
            case Qiupu.ACTION_FAVORITE: {
                reason = Qiupu.REASON_FAVORITE;
                try {
                    if (existUserActions(packageName,  user_id, deviceid, String.valueOf(reason)).isEmpty()) {
                        updateQappCount0(packageName, version_code, "favorite_count", Long.parseLong(user_id), deviceid);
                    }
                } catch (Exception t) {
                }
                break;
            }
            case Qiupu.ACTION_UNFAVORITE: {
                reason = ~Qiupu.REASON_FAVORITE;
                break;
            }
            case Qiupu.ACTION_INSTALL: {
                reason = Qiupu.REASON_INSTALLED | Qiupu.REASON_INSTALLING;
                try {
                    if (existUserActions(packageName,  user_id, deviceid, String.valueOf(reason)).isEmpty()) {
                        updateQappCount0(packageName, version_code, "install_count", Long.parseLong(user_id), deviceid);
                    }
                } catch (Exception t) {
                }
                break;
            }
            case Qiupu.ACTION_UNINSTALL: {
                reason = Qiupu.REASON_UNINSTALLED;
                try {
                    if (existUserActions(packageName, user_id, deviceid, String.valueOf(reason)).isEmpty()) {
                        updateQappCount0(packageName, version_code, "uninstall_count", Long.parseLong(user_id), deviceid);
                    }
                } catch (Exception t) {
                }
                break;
            }
            case Qiupu.ACTION_UPLOAD: {
                reason = Qiupu.REASON_UPLOADED;
                try {
                    updateQappCount0(packageName, version_code, "download_count", Long.parseLong(user_id), deviceid);
                    updateQappCount0(packageName, version_code, "install_count", Long.parseLong(user_id), deviceid);
                } catch (Exception t) {
                }
                break;
            }
        }
        return true;
    }
    
    @Override
    protected boolean doActions(List<Action> actions) {
        List<String> sqls = new ArrayList<String>();
        for (Action action : actions) {
            doAction(action, sqls);           
        }

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sqls);
        return n > 0;
    }
    
    @Override
    protected boolean doHistory(Action action, List<String> sqls) {
        final String SQL = "INSERT INTO ${table}"
                + " (${alias.qapk_history_id}, ${alias.user}, ${alias.package}, ${alias.version_code}, ${alias.architecture}, ${alias.version_name}, ${alias.created_time}, ${alias.action}, ${alias.device})"
                + " VALUES"
                + " (${v(id)}, ${v(user_id)}, ${v(package)}, ${v(version_code)}, ${v(architecture)}, ${v(version_name)}, ${v(created_time)}, ${v(action)}, ${v(device)})";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", qapkHistoryTable},
                    {"alias", qapkHistorySchema.getAllAliases()},
                    {"id", RandomUtils.generateId()},
                    {"user_id", action.user.equals("")?"0":action.user},
                    {"package", action.packageName},
                    {"version_code", action.versionCode},
                    {"architecture", action.architecture},
                    {"version_name", action.versionName},
                    {"created_time", DateUtils.nowMillis()},
                    {"action", action.action},
                    {"device", action.deviceid}
                });

        if (sqls != null) {
            sqls.add(sql);
            return true;
        } else {
            SQLExecutor se = getSqlExecutor();
            long n = se.executeUpdate(sql);
            return n > 0;
        }
    }

    @Override
    protected boolean doAssociate(Action action, List<String> sqls) {
        int reason = Qiupu.REASON_INSTALLED;

        boolean isUnfavorite = (action.action == Qiupu.ACTION_UNFAVORITE);
        boolean isUninstall = (action.action == Qiupu.ACTION_UNINSTALL);
        switch (action.action) {
            case Qiupu.ACTION_DOWNLOAD: {
                reason = Qiupu.REASON_DOWNLOADED;
                break;
            }
            case Qiupu.ACTION_FAVORITE: {
                reason = Qiupu.REASON_FAVORITE;
                break;
            }
            case Qiupu.ACTION_UNFAVORITE: {
                reason = ~Qiupu.REASON_FAVORITE;
                break;
            }
            case Qiupu.ACTION_INSTALL: {
                reason = Qiupu.REASON_INSTALLED | Qiupu.REASON_INSTALLING;
                break;
            }
            case Qiupu.ACTION_UNINSTALL: {
                reason = Qiupu.REASON_UNINSTALLED;
                break;
            }
            case Qiupu.ACTION_UPLOAD: {
                reason = Qiupu.REASON_UPLOADED;
                break;
            }
        }

        SQLExecutor se = getSqlExecutor();

        String sql1 = "select reason from user_qapp where user=" + action.user + " and package='" + action.packageName + "'";
        RecordSet recs = se.executeRecordSet(sql1.toString(), null);
        if (recs.size() > 0) {
            int inR = (int) recs.getFirstRecord().getInt("reason");
            if (isUnfavorite) {
                reason = inR & reason;
            } else {
                reason = inR | reason;
            }

            if (isUninstall) {
                reason = inR & (~Qiupu.REASON_INSTALLING);
            }
            String sql2 = "update user_qapp set reason=" + reason + " where user=" + action.user + " and package='" + action.packageName + "'";
            long n = se.executeUpdate(sql2);
            return n > 0;
        } else {

            String SQL = "INSERT INTO ${table}"
                    + " (${alias.user}, ${alias.package}, ${alias.reason}, ${alias.privacy}, ${alias.version_code}, ${alias.architecture}, ${alias.device})"
                    + " VALUES"
                    + " (${v(user_id)}, ${v(package)}, ${v(reason)}, ${v(privacy)}, ${v(version_code)}, ${v(architecture)}, ${v(device)})"
                    + " ON DUPLICATE KEY UPDATE ${alias.reason}=${alias.reason}";

            if (isUnfavorite) {
                SQL += "&${v(reason)}";
            } else {
                SQL += "|${v(reason)}";
            }

            if (isUninstall) {
                SQL += "&" + (~Qiupu.REASON_INSTALLING);
            }

            String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", userQappTable},
                    {"alias", userQappSchema.getAllAliases()},
                    {"user_id", action.user.equals("") ? "0" : action.user},
                    {"package", action.packageName},
                    {"reason", reason},
                    {"privacy", Qiupu.PRIVACY_PUBLIC},
                    {"version_code", action.versionCode},
                    {"architecture", action.architecture},
                    {"device", action.deviceid}
            });
            if (sqls != null) {
                sqls.add(sql);
                return true;
            } else {
                long n = se.executeUpdate(sql);
                return n > 0;
            }
        }
    }
    
    @Override
    protected boolean createUpdateApkLessDesc0(String packageName,int version_code,String app_name,String desc) {
        
        String SQL = "INSERT INTO ${table}"
                + " (${alias.package}, ${alias.version_code}, ${alias.app_name}, ${alias.description})"
                + " VALUES"
                + " (${v(packageName)}, ${v(version_code)}, ${v(app_name)}, ${v(desc)})"
                + " ON DUPLICATE KEY UPDATE ${alias.description}=${alias.description}";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", updateApkLessDescTable},
                    {"alias", updateApkLessDescSchema.getAllAliases()},
                    {"packageName", packageName},
                    {"version_code", version_code},
                    {"app_name", app_name},
                    {"desc",desc}
                });

            SQLExecutor se = getSqlExecutor();
            long n = se.executeUpdate(sql);
            return n > 0;
    }

    @Override
    protected RecordSet getUpdateApkLessDesc0(int page, int count, String packageName, String app_name) {
        String SQL = "SELECT * FROM ${table} WHERE length(${alias.description})<10 ";
        if (!packageName.equals("")) {
            SQL += " AND ${alias.package} LIKE '%" + packageName + "%'";
        }
        if (!app_name.equals("")) {
            SQL += " AND ${alias.app_name} LIKE '%" + app_name + "%'";
        }
        SQL += " ORDER BY ${alias.package} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"table", updateApkLessDescTable},
                    {"alias", updateApkLessDescSchema.getAllAliases()},
                    {"limit", SQLUtils.pageToLimit(page, count)}
                });
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }
    
    @Override
    protected boolean doAction(Action action, List<String> sqls) {
        //r2 must be first
        if (action.action == Qiupu.ACTION_INSTALL || action.action == Qiupu.ACTION_DOWNLOAD || action.action == Qiupu.ACTION_FAVORITE || action.action == Qiupu.ACTION_UNINSTALL) {       
            doUpdateCount(action.packageName, toStr(action.versionCode), toStr(action.user), action.deviceid, toStr(action.action));
        }
        if (action.action == Qiupu.ACTION_UPLOAD && !action.user.equals("0") && !action.user.equals(qiupuUid) && !action.user.equals("")) {       
            doUpdateCount(action.packageName, toStr(action.versionCode), toStr(action.user), action.deviceid, toStr(action.action));
        }
        boolean r0 = doHistory(action, sqls);
        boolean r1 = doAssociate(action, sqls);
        return r0 && r1;
    }

    @Override
    protected boolean saveApk(Record apkInfo) {
        Schemas.standardize(qapkSchema, apkInfo);

        final String SQL = "INSERT INTO ${table} ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(SQL,
                "table", qapkTable,
                "alias", qapkSchema.getAllAliases(),
                "info", apkInfo);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean UpdateApkForSync0(String userId, String packageName, boolean last_installed) {
        String sql ="";
        if (packageName.equals("")) {
            sql = "update user_qapp set last_installed=" + last_installed + " where user=" + userId + "";
        } else {
            sql = "update user_qapp set last_installed=" + last_installed + " where user=" + userId + " and package='" + packageName + "'";
        }
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean updateLastTimeApkForSync0(String userId, String packageName) {
        String sql ="update user_qapp set last_installed_time=" + DateUtils.nowMillis() + " where user=" + userId + " and package='" + packageName + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getLastInstalledApp0(String userId) {
        String sql = "SELECT distinct(package) FROM user_qapp where user="+userId+" and last_installed=1";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected Record getLastInstalledTimeApp0(String userId,String packageName) {
        String sql = "SELECT last_installed_time FROM user_qapp where user='"+userId+"' and package='"+packageName+"' order by last_installed_time limit 1";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        return rec;
    }

    @Override
    protected RecordSet sync(String user, RecordSet userApps, String[] apkIds, boolean all,String deviceid,int minSDK) {

        List<Action> actions = new ArrayList<Action>();
        List<String> userPackages = userApps.getStringColumnValues("package");
        RecordSet userApps0 = new RecordSet();     //用于返回不是全部同步的应用的

        String[] packages = new String[apkIds.length];
        int[] versionCodes = new int[apkIds.length];
        int[] archs = new int[apkIds.length];
        for (int i = 0; i < apkIds.length; i++) {
            ApkId oApkId = ApkId.parse(apkIds[i]);
            packages[i] = oApkId.package_;
            versionCodes[i] = oApkId.versionCode;
            archs[i] = oApkId.arch;
        }

        //traversal user's phone
        for (int i = 0; i < packages.length; i++) {
            String packageName = packages[i];
            int versionCode = versionCodes[i];
            List<String> l = new ArrayList<String>();
            l.add("package");
            l.add("version_code");
            l.add("version_name");
            l.add("file_url");
            l.add("rating");
            l.add("file_size");
            l.add("icon_url");
            l.add("app_name");

            String ap = packageName;
            int vc = getMaxVersionCodeFromApp(packageName,minSDK);      //获取当前传递进来的应用的最大版本号
            int ar = Qiupu.ARCH_ARM;
            ApkId a = ApkId.of(ap, vc, ar);             //获取最大版本号的APKID

            Record app = (versionCode > vc) ? null : getAppsFromApkIds0(a, l,minSDK);        //如果传递进来的版本比最大版本新，则返回null，否则   获取最新的APP
            if(versionCode > vc)
            {
            	a = ApkId.of(ap, versionCode, ar);         //给a赋值为当前最大的APKID，不管是 库里的，还是传递进来的
            }
            if ((app == null) || app.isEmpty()) {       //传递进来的版本大，server上没有，进入到 DB中
                //not exist in server,want upload
                //insert into db values(apk info)
                final String SQL0 = "SELECT * FROM ${table} WHERE ${alias.apk_id}=${v(apk_id)}";
                String sql = SQLTemplate.merge(SQL0,
                        "table", qapkNotinPoolTable,
                        "alias", qapkNotinPoolSchema.getAllAliases(),
                        "apk_id", a.toString());

                SQLExecutor se = getSqlExecutor();
                Record rec = se.executeRecord(sql, null);
                if (rec.isEmpty()) {
                    final String SQL = "INSERT INTO ${table}(${alias.apk_id},${alias.created_time}) VALUES(${v(apk_id)}, ${v(created_time)})";
                    sql = SQLTemplate.merge(SQL,
                            "table", qapkNotinPoolTable,
                            "alias", qapkNotinPoolSchema.getAllAliases(),
                            "apk_id", a.toString(),
                            "created_time", DateUtils.nowMillis());

                    se.executeUpdate(sql);
                }

                app = Record.of(new Object[][]{
                            {"apk_id", a.toString()},
                            {"package", packageName},
                            {"version_code", -1},
                            {"version_name", ""},
                            {"file_url", ""},
                            {"rating", ""},
                            {"file_size", 0},
                            {"icon_url", ""},
                            {"app_name", ""}
                        });
            }
            else    //传递进来的版本小       直接把大的返回去
            {
            	try {
            		generateUrls(app);
            		generateDescriptions(app);
            	}
            	catch(AvroRemoteException e)
            	{
            		
            	}
            }
            
            //get not linked apps
            if (!userPackages.contains(packageName)) {     //如果我自己的应用里面不包含这个新传递进来的APK，则加到跟自己关联的里面去
                    //linked apps
                    if (StringUtils.isNotBlank(user)) {
                        Action action = new Action();
                        action.action = Qiupu.ACTION_INSTALL;
                        action.packageName = packageName;
                        action.user = user;
                        action.versionCode = versionCodes[i];
                        action.deviceid=deviceid;
                        actions.add(action);
                    }
                
                userApps.add(app);    //这里是加入到 userApps里面，因为   userApps是获取到用户的之前关联的
            }
            userApps0.add(app);     //每一次循环，都把这个应用加到不是全部返回的列表中
        }

        if (all) {    //如果全部同步
            //traversal user linked apps list，get uninstalled list
            for (Record rec : userApps) {        //再一次循环  把删除的从用户关联的干掉
                String packageName = rec.getString("package");
                int versionCode = (int) rec.getInt("version_code");
                if (!ArrayUtils.contains(packages, packageName)) {
                    //uninstalled
                    if (StringUtils.isNotBlank(user)) {
                        Action action = new Action();
                        action.action = Qiupu.ACTION_UNINSTALL;
                        action.packageName = packageName;
                        action.user = user;
                        action.versionCode = versionCode;
                        action.deviceid=deviceid;
                        //actions.add(action);
                    }
                }
            }
        }

        doActions(actions);

        return all ? userApps : userApps0;
    }

    @Override
    protected boolean isAssociate(long user, String packageName) {
        final String SQL = "SELECT ${alias.user} FROM ${table} WHERE ${alias.user}=${v(user_id)}"
                + " AND ${alias.package}=${v(package)}";
        String sql = SQLTemplate.merge(SQL,
                "table", userQappTable,
                "alias", userQappSchema.getAllAliases(),
                "user_id", user,
                "package", packageName);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        return !recs.isEmpty();
    }
    
    @Override
    protected boolean isAssociate(String deviceId, String packageName) {
        final String SQL = "SELECT ${alias.user} FROM ${table} WHERE ${alias.device}=${v(device)}"
                + " AND ${alias.package}=${v(package)}";
        String sql = SQLTemplate.merge(SQL,
                "table", userQappTable,
                "alias", userQappSchema.getAllAliases(),
                "device", deviceId,
                "package", packageName);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        return !recs.isEmpty();
    }

    @Override
    protected boolean manualApk(String apk_id, long type) {
        final String SQL = "INSERT INTO ${table}(${alias.apk_id},${alias.type}) VALUES(${v(apk_id)}, ${v(type)})";
        String sql = SQLTemplate.merge(SQL,
                "table", qapkManualTable,
                "alias", qapkManualSchema.getAllAliases(),
                "apk_id", apk_id,
                "type", type);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getManualApks0(int type,int minSDK) {
        final String SQL = "SELECT ${alias.apk_id} FROM ${table} WHERE ${alias.type}=${v(type)} ";
        String sql = SQLTemplate.merge(SQL,
                "table", qapkManualTable,
                "alias", qapkManualSchema.getAllAliases(),
                "type", type);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(qapkManualSchema, recs);

        RecordSet apps = new RecordSet();
        if (!recs.isEmpty()) {
            List<String> cols = StringUtils2.splitList(Qiupu.QAPK_COLUMNS, ",", true);
            for (Record rec : recs) {
                String apk_id = rec.getString("apk_id");
                ApkId apkId = ApkId.parse(apk_id);
                int v = getMaxVersionCodeFromApp(apkId.package_,minSDK);
                String tApkId = apkId.package_ + "-" + String.valueOf(v) + "-" + "arm";
                ApkId apkId1 = ApkId.parse(tApkId);
                Record app = getAppsFromApkIds0(apkId1, cols,minSDK);
                apps.add(app);
            }
        }
        return apps;
    }

    @Override
    protected RecordSet getManualApkIds0(String type, String maxcount,int minSDK) {
        final String SQL = "SELECT ${alias.apk_id} FROM ${table} WHERE ${alias.type}=${v(type)} limit ${v(maxcount)}";
        String sql = SQLTemplate.merge(SQL,
                "table", qapkManualTable,
                "alias", qapkManualSchema.getAllAliases(),
                "maxcount", Integer.valueOf(maxcount),
                "type", Integer.valueOf(type));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(qapkManualSchema, recs);
        RecordSet apps = new RecordSet();
        if (!recs.isEmpty()) {
            List<String> cols = StringUtils2.splitList(Qiupu.QAPK_COLUMNS, ",", true);
            for (Record rec : recs) {
                String apk_id = rec.getString("apk_id");
                ApkId apkId = ApkId.parse(apk_id);
                int v = getMaxVersionCodeFromApp(apkId.package_,minSDK);
                String tApkId = apkId.package_ + "-" + String.valueOf(v) + "-" + "arm";
                ApkId apkId1 = ApkId.parse(tApkId);
                Record app = getAppsFromApkIds0(apkId1, cols,minSDK);
                apps.add(app);
            }
        }
        return apps;
    }

    @Override
    protected RecordSet loadNeedExinfoApks0(boolean isAll) {
        String SQL = "SELECT ${alias.app_name},${alias.package},${alias.version_code},${alias.architecture}"
                + " FROM ${table}"; //WHERE ${alias.description}='' OR ${alias.screenshots_urls}='[]'" +
//				" OR ${alias.sub_category}=0";

        if (!isAll) {
            SQL += " WHERE ${alias.description}='' OR ${alias.screenshots_urls}='[]' OR ${alias.screenshots_urls}=''";
        }

        SQL += " ORDER BY ${alias.created_time} DESC";
        
        String sql = SQLTemplate.merge(SQL,
                "table", qapkTable,
                "alias", qapkSchema.getAllAliases());

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        recs.unique("package");
        for (Record rec : recs) {
            String app = rec.getString("package");
            int maxVersion = getMaxVersionCodeFromApp(app,1000);
            rec.put("version_code", maxVersion);
            
            String maxAppName = getAppName(app, maxVersion, rec.getInt("architecture"));
            rec.put("app_name", maxAppName);
        }

        Schemas.standardize(qapkManualSchema, recs);

        //write to file
        File file = new File("need_info_apklist.txt");

        if (file.exists()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
            }
        }

        HashSet<String> set = new HashSet<String>();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file, true));

            for (Record apk : recs) {
                String packageName = apk.getString("package");
                long versionCode = apk.getInt("version_code");
                long architecture = apk.getInt("architecture");
                String key = packageName + "#" + versionCode + "#" + architecture;
                if (!set.contains(key)) {
                    out.write(key);
                    out.newLine();
                    out.flush();
                    set.add(key);
                }
            }
            out.close();
        } catch (IOException ioe) {
        }

        return recs;
    }

    @Override
    protected int getApplicationCount0() {
        final String SQL = "SELECT count(distinct ${alias.package}) FROM ${table}";

        String sql = SQLTemplate.merge(SQL,
                "table", qapkTable,
                "alias", qapkSchema.getAllAliases());

        SQLExecutor se = getSqlExecutor();
        Object obj = se.executeScalar(sql);

        return ((Number) obj).intValue();
    }

    @Override
    protected int getTodayAppCount0() {
        String temp_str = "";
        Date dt = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        temp_str = sdf.format(dt);
        Date d = null;
        try {
            d = sdf.parse(temp_str);
        } catch (ParseException e) {
        }
        long today = d.getTime();

        final String SQL = "SELECT count(*) FROM ${table} where ${alias.created_time}>" + today;

        String sql = SQLTemplate.merge(SQL,
                "table", qapkTable,
                "alias", qapkSchema.getAllAliases());

        SQLExecutor se = getSqlExecutor();
        Object obj = se.executeScalar(sql);

        return ((Number) obj).intValue();
    }

    @Override
    protected int getNeedExinfoAppCount0() {
        final String SQL = "SELECT count(distinct ${alias.package})"
                + " FROM ${table} WHERE ${alias.description}='' OR ${alias.screenshots_urls}='[]'"; //+
//				" OR ${alias.sub_category}=0";

        String sql = SQLTemplate.merge(SQL,
                "table", qapkTable,
                "alias", qapkSchema.getAllAliases());

        SQLExecutor se = getSqlExecutor();
        Object obj = se.executeScalar(sql);

        return ((Number) obj).intValue();
    }

    @Override
    protected int getUserAppCount0(String userId) {
        final String sql = "select count(distinct(package)) from user_qapp where user="+ userId +""; //+

        SQLExecutor se = getSqlExecutor();
        Object obj = se.executeScalar(sql);

        return ((Number) obj).intValue();
    }

    @Override
    protected RecordSet getUsersAppCount0(String userIds,String reason) {
        final String sql = "select distinct(package),user from user_qapp where reason&"+reason+"<>0 and user in (" + userIds + ")"; //+

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        RecordSet count0 = new RecordSet();
        List<String> ul = StringUtils2.splitList(toStr(userIds), ",", true);
        for (String userId : ul) {
            int tmp = 0;
            for (Record r : recs) {
                if (r.getString("user").equals(userId)) {
                    tmp += 1;
                }
            }
            count0.add(Record.of("user_id",userId, "count",tmp));
        }
        return count0;
    }

    @Override
    protected boolean updateApk0(Record info) {
        Schemas.standardize(qapkSchema, info);

        long now = DateUtils.nowMillis();
        String sql = new SQLBuilder.Update(qapkSchema).update(qapkTable).values(info).value("info_updated_time", now).where("${alias.package}=${v(package)}", "package", info.getString("package")).and("${alias.version_code}=${v(version_code)}", "version_code", info.getInt("version_code")).and("${alias.architecture}=${v(architecture)}", "architecture", info.getInt("architecture")).toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
    
    @Override
    protected boolean saveQapkSuggest0(Record suggestRecord) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, suggestRecord)}";
        String sql = SQLTemplate.merge(SQL,
                "table", qapkSuggestTable, "alias", qapkSuggestSchema.getAllAliases(),
                "suggestRecord", suggestRecord);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
    
   @Override
    protected RecordSet getQapkOtherVersion0(String apkId,int minSDK) {
        ApkId a = ApkId.parse(apkId);
        
        Map<String, String> alias = qapkSchema.getAllAliases();
        StringBuilder sql = new StringBuilder();
        sql.append(SQLTemplate.merge("SELECT ${alias.package},${alias.version_code},${alias.version_name},${alias.architecture},${alias.file_url} FROM ${table} WHERE destroyed_time = 0 and min_sdk_version<="+minSDK+" ",
                "alias", alias, "table", qapkTable));

        sql.append(SQLTemplate.merge(" AND ${alias.package} ='" + a.package_ + "' AND ${alias.version_code}<>'" + a.versionCode + "' ORDER BY ${alias.version_code}",
                "alias", alias));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected boolean deleteQapkSuggest0(String sub_id) {
        final String SQL = "UPDATE ${table} SET ${alias.destroyed_time}=${v(destroyed_time)} WHERE ${alias.sub_id}=${v(sub_id)}";
        String sql = SQLTemplate.merge(SQL,
                "table", qapkSuggestTable, "alias", qapkSuggestSchema.getAllAliases(),
                "sub_id", sub_id,"destroyed_time", DateUtils.nowMillis());

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
    
    @Override
    protected boolean updateQapkSuggest0(String sub_id,String sub_name,String policy,String img_url) {
        final String SQL = "UPDATE ${table} SET ${alias.policy}=${v(policy)},${alias.sub_name}=${v(sub_name)},${alias.img_url}=${v(img_url)} WHERE ${alias.sub_id}=${v(sub_id)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", qapkSuggestSchema.getAllAliases()},
                    {"table", qapkSuggestTable},
                    {"policy", policy},
                    {"sub_id", sub_id},
                    {"img_url", img_url},
                    {"sub_name", sub_name},});
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
    
    @Override
    protected boolean updateQapkSuggest1(String sub_id) {
        boolean b = false;
        RecordSet rs = getSuggestApkfromSubId0(sub_id, false);
        if (!rs.getFirstRecord().getBoolean("ifsuggest", false)) {
            RecordSet rs0 = getSuggestApkfromSubId0("0", true);
            if (rs0.size() >= 4) {
                return false;
            }
            b = true;
        }
        final String SQL = "UPDATE ${table} SET ${alias.ifsuggest}=" + b + " WHERE ${alias.sub_id}=${v(sub_id)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", qapkSuggestSchema.getAllAliases()},
                    {"table", qapkSuggestTable},
                    {"sub_id", sub_id},});
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
    
    @Override
    protected boolean existSuggestApk0(String sub_id, String sub_name) {
        final String SQL = "SELECT COUNT(*) FROM ${table} WHERE ${alias.sub_id}=${v(sub_id)} AND ${alias.sub_name}=${v(sub_name)} AND ${alias.destroyed_time}=0";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", qapkSuggestSchema.getAllAliases()},
                    {"table", qapkSuggestTable},
                    {"sub_id", sub_id},
                    {"sub_name", sub_name},});

        SQLExecutor se = getSqlExecutor();
        Number count = (Number) se.executeScalar(sql);
        return count.intValue() > 0;
    }
    
    @Override
    protected RecordSet getSuggestApkfromSubId0(String sub_id, boolean ifsuggest) {
        String SQL = "";
        if (Integer.valueOf(sub_id) > 0) {
            if (!ifsuggest) {
                SQL = "SELECT ${alias.sub_id},${alias.sub_name},${alias.img_url},${alias.ifsuggest},${alias.policy} FROM ${table} WHERE ${alias.sub_id}=${v(sub_id)} AND ${alias.destroyed_time}=0 ORDER BY ${alias.sub_id}";
            } else {
                SQL = "SELECT ${alias.sub_id},${alias.sub_name},${alias.img_url},${alias.ifsuggest},${alias.policy} FROM ${table} WHERE ${alias.sub_id}=${v(sub_id)} AND ${alias.destroyed_time}=0 AND ${alias.ifsuggest}="+ifsuggest+" ORDER BY ${alias.sub_id}";
            }
        } else {
            if (!ifsuggest) {
                SQL = "SELECT ${alias.sub_id},${alias.sub_name},${alias.img_url},${alias.ifsuggest},${alias.policy} FROM ${table} WHERE ${alias.destroyed_time}=0 ORDER BY ${alias.sub_id}";
            } else {
                SQL = "SELECT ${alias.sub_id},${alias.sub_name},${alias.img_url},${alias.ifsuggest},${alias.policy} FROM ${table} WHERE ${alias.destroyed_time}=0 AND ${alias.ifsuggest}="+ifsuggest+" ORDER BY ${alias.sub_id}";
            }
        }
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", qapkSuggestSchema.getAllAliases()},
                    {"table", qapkSuggestTable},
                    {"sub_id", sub_id},});

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(qapkSuggestSchema, recs);
        return recs;
    }
    
    //get strongman by type
    @Override
    protected RecordSet getStrongMan0(String sub_category, int page, int count) {
        String SQL = "";
        if (!sub_category.equals("")) {
            SQL = "SELECT COUNT(DISTINCT(${alias.package})) AS COUNT1,${alias.user} FROM ${table} "
                    + "WHERE user<>10002 and user<>102 and user<>282 and ${alias.package} in (select package from qapk where sub_category in ('" + sub_category + "'))"
                    + " group by ${alias.user} order by COUNT1 DESC ${limit}";
        } else {
            SQL = "SELECT COUNT(DISTINCT(${alias.package})) AS COUNT1,${alias.user} FROM ${table} "
                    + "where user<>102 and user<>282 group by ${alias.user} order by COUNT1 DESC ${limit}";
        }
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", userQappSchema.getAllAliases()},
                    {"table", userQappTable},
                    {"limit", SQLUtils.pageToLimit(page, count)},});

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }
    
    //many methods to get APKID list downstarirs
    
    //ranking list by datediff ,in time area,ex:week,month,year
    @Override
    protected RecordSet getApkIdsListByTimes0(long datediff, int action, int limit,int minSDK) {
        RecordSet outRS = new RecordSet();
        if (limit > 0) {
            long now = DateUtils.nowMillis();
            final String SQL = "SELECT ${alias.package} FROM ${table} WHERE ("+now+"-"+datediff+")<=${alias.created_time} AND ${alias.action}=${v(action)} "
                    + " group by ${alias.package} order by ${alias.created_time} desc limit ${v(limit)}";
            String sql = SQLTemplate.merge(SQL, new Object[][]{
                        {"alias", qapkHistorySchema.getAllAliases()},
                        {"table", qapkHistoryTable},
                        {"limit", limit},
                        {"action", action},});

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql, null);

            for (Record r : recs) {
                outRS.add(Record.of("apk_id", r.getString("package") + "-" + getMaxVersionCodeFromApp(r.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }

    //ranking list by download count
    @Override
    protected RecordSet getApkIdsListByDownloadTimes0(int limit,int minSDK) {
        RecordSet outRS = new RecordSet();
        if (limit > 0) {
            Map<String, String> alias = qapkSchema.getAllAliases();
            StringBuilder sql = new StringBuilder();

            sql.append(SQLTemplate.merge("SELECT SUM(${alias.download_count}) AS COUNT,${alias.package} FROM ${table} WHERE destroyed_time = 0",
                    "alias", alias, "table", qapkTable));

            sql.append(SQLTemplate.merge("  GROUP BY ${alias.package} order by COUNT DESC LIMIT ${v(limit)}",
                    "alias", alias, "limit", limit));

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql.toString(), null);

            for (Record r : recs) {
                outRS.add(Record.of("apk_id", r.getString("package") + "-" + getMaxVersionCodeFromApp(r.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }
    
    //ranking list by rating
    @Override
    protected RecordSet getApkIdsListByRating0(int limit,int minSDK) {
        RecordSet outRS = new RecordSet();
        if (limit > 0) {
            Map<String, String> alias = qapkSchema.getAllAliases();
            StringBuilder sql = new StringBuilder();

            sql.append(SQLTemplate.merge("SELECT ${alias.package} FROM ${table} WHERE destroyed_time = 0",
                    "alias", alias, "table", qapkTable));

            sql.append(SQLTemplate.merge("  GROUP BY ${alias.package} order by rating DESC LIMIT ${v(limit)}",
                    "alias", alias, "limit", limit));

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql.toString(), null);

            for (Record r : recs) {
                outRS.add(Record.of("apk_id", r.getString("package") + "-" + getMaxVersionCodeFromApp(r.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }
    
    //ranking list by force recommended borqs apps
    @Override
    protected RecordSet getApkIdsListByBorqs0(int limit,int minSDK) {
        RecordSet outRS = new RecordSet();
        if (limit > 0) {
            Map<String, String> alias = qapkSchema.getAllAliases();
            StringBuilder sql = new StringBuilder();

            sql.append(SQLTemplate.merge("SELECT ${alias.package} FROM ${table} WHERE destroyed_time = 0 AND ${alias.borqs}=1",
                    "alias", alias, "table", qapkTable));

            sql.append(SQLTemplate.merge("  GROUP BY ${alias.package} order by ${alias.app_name} LIMIT ${v(limit)}",
                    "alias", alias, "limit", limit));

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql.toString(), null);

            for (Record r : recs) {
                outRS.add(Record.of("apk_id", r.getString("package") + "-" + getMaxVersionCodeFromApp(r.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }
    
    //get random apps from lasted 1000
    
    @Override
    protected RecordSet getApkIdsListByRandom0(int limit,int minSDK) {
        RecordSet outRS = new RecordSet();
        if (limit > 0) {
            Map<String, String> alias = qapkSchema.getAllAliases();
            StringBuilder sql = new StringBuilder();

            sql.append(SQLTemplate.merge("SELECT ${alias.package} FROM ${table} WHERE destroyed_time = 0",
                    "alias", alias, "table", qapkTable));

            sql.append(SQLTemplate.merge("  GROUP BY ${alias.package} order by ${alias.created_time} DESC LIMIT 1000",
                    "alias", alias));

            SQLExecutor se = getSqlExecutor();
            RecordSet recs = se.executeRecordSet(sql.toString(), null);

            
            RecordSet t = new RecordSet();
            
            int j=0;
            for (Record r : recs) {
                j+=1;
                t.add(Record.of("package",r.getString("package"),"index",j));
            }
            
            Random rnd = new Random();
            List nums = new ArrayList(limit);
            for (int i = 1; i < limit + 1; i++) {
                int p = rnd.nextInt(1000);
                if (!nums.contains(p)) {
                    nums.add(p);
                }
            }
            
            for (Record m : t) {
                if (nums.contains(m.getInt("index")))
                outRS.add(Record.of("apk_id", m.getString("package") + "-" + getMaxVersionCodeFromApp(m.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }
    
     @Override
     protected RecordSet getTop1ApkByCategory0(String sub_category) {
         RecordSet outRS = new RecordSet();
         String sql="";
         if (sub_category.equals("256")) {
            sql="select distinct(sub_category) from qapk where sub_category>256 and sub_category<512 order by sub_category";
         }
         else if (sub_category.equals("512")){
            sql="select distinct(sub_category) from qapk where sub_category>=512 order by sub_category";
         }
         else {
            sql="select distinct(sub_category) from qapk order by sub_category";
         }
         SQLExecutor se = getSqlExecutor();
         RecordSet recs = se.executeRecordSet(sql.toString(), null);

         String sql1 ="";
         for (Record r : recs){
             String now_categoryid = r.getString("sub_category") ;
             //get hot top 1
             sql1 = "SELECT SUM(download_count) AS COUNT1,package,icon_url FROM qapk WHERE destroyed_time = 0 AND sub_category="+ now_categoryid +"  GROUP BY package order by COUNT1 DESC LIMIT 1";
             Record r0 = se.executeRecordSet(sql1,null).getFirstRecord() ;
             r0.put("sub_category",now_categoryid) ;
             r0.removeColumns("COUNT1");
             outRS.add(r0);
         }

         return outRS;
     }

     @Override
    protected RecordSet formatOldDataToConversation0(String viewerId) {
//        String sql = "select upload_user,package,created_time from qapk where upload_user>0";

        String sql = "select user,package  from user_qapp where user>0 and reason & 8<>0";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected boolean updateBorqsByPackage0(String package_, int borqs) {
        String sql = "UPDATE qapk SET borqs=" + borqs + " WHERE package='" + package_ + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
}
