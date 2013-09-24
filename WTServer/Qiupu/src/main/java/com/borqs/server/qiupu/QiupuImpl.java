package com.borqs.server.qiupu;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.log.TraceCall;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.*;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class QiupuImpl implements QiupuLogic, Initializable {
    private static final Logger L = Logger.getLogger(QiupuImpl.class);

    public final Schema qapkHistorySchema = Schema.loadClassPath(QiupuImpl.class, "qapk_history.schema");
    public final Schema qapkModelSchema = Schema.loadClassPath(QiupuImpl.class, "qapk_model.schema");
    public final Schema userQappSchema = Schema.loadClassPath(QiupuImpl.class, "user_qapp.schema");
    public final Schema qapkSchema = Schema.loadClassPath(QiupuImpl.class, "qapk.schema");
    public final Schema userQsettingSchema = Schema.loadClassPath(QiupuImpl.class, "user_qsetting.schema");
    public final Schema qapkNotinPoolSchema = Schema.loadClassPath(QiupuImpl.class, "qapk_notin_pool.schema");
    public final Schema qapkManualSchema = Schema.loadClassPath(QiupuImpl.class, "qapk_manual.schema");
    public final Schema qapkSuggestSchema = Schema.loadClassPath(QiupuImpl.class, "qapk_suggest.schema");
    public final Schema updateApkLessDescSchema = Schema.loadClassPath(QiupuImpl.class, "updateApkLessDesc.schema");


    private String apkUrlPattern;
    private String iconUrlPattern;
    private String screenshotUrlPattern;
    private String subApksImgUrlPattern;

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

    public QiupuImpl() {
    }

    @Override
    public void init() {
        Configuration conf = GlobalConfig.get();

        qapkHistorySchema.loadAliases(conf.getString("schema.qapkHistorySchema.alias", null));
        qapkModelSchema.loadAliases(conf.getString("schema.qapkModelSchema.alias", null));
        userQappSchema.loadAliases(conf.getString("schema.userQappSchema.alias", null));
        qapkSchema.loadAliases(conf.getString("schema.qapkSchema.alias", null));
        userQsettingSchema.loadAliases(conf.getString("schema.userQsettingSchema.alias", null));
        qapkNotinPoolSchema.loadAliases(conf.getString("schema.qapkNotinPoolSchema.alias", null));
        qapkManualSchema.loadAliases(conf.getString("schema.qapkManualSchema.alias", null));
        qapkSuggestSchema.loadAliases(conf.getString("schema.qapkSuggestSchema.alias", null));
        updateApkLessDescSchema.loadAliases(conf.getString("schema.updateApkLessDescSchema.alias", null));

        this.apkUrlPattern = conf.checkGetString("qiupu.apkUrlPattern");
        this.iconUrlPattern = conf.checkGetString("qiupu.iconUrlPattern");
        this.screenshotUrlPattern = conf.checkGetString("qiupu.screenshotUrlPattern");
        this.subApksImgUrlPattern = StringUtils.removeEnd(conf.checkGetString("qiupu.subApksImgUrlPattern").trim(), "/");

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
    }

    private static String toStr(Object o) {
        return ObjectUtils.toString(o);
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    protected RecordSet getApps0(Context ctx,String sub_category, boolean paid, String sort, List<String> cols, int page, int count,int minSDK) {
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
            if (!ctx.getViewerIdString().equals("")){
                sql.append(" AND instr(report_user,'"+ctx.getViewerIdString()+"')<=0 AND instr(report_user,',')<2 ");
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
        return returnPointVersionApp(ctx,recs, cols,minSDK);
    }

    protected RecordSet returnPointVersionApp(Context ctx,RecordSet inApkRecordSet, List<String> cols,int minSDK) {
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
                    vc = String.valueOf(getMaxVersionCodeFromApp(ctx,r.getString("package"),minSDK));
                    ar = String.valueOf("arm");
                }
                ApkId a = ApkId.parse(ap + "-" + vc + "-" + ar);
                Record rec = getAppsFromApkIds0(ctx,a, cols,minSDK);

                if (cols.contains("app_last_version_code") || cols.contains("app_last_version_name")) {
                    // TODO: add here
                }
                if (!rec.isEmpty())
                    outRecs.add(rec);

            }
        }
        return outRecs;
    }

    protected int getMaxVersionCodeFromApp(Context ctx,String app,int minSDK) {
        final String SQL = "SELECT MAX(${alias.version_code}) FROM ${table} WHERE ${alias.package}=${v(app)} and min_sdk_version<="+minSDK+" ";

        String sql = SQLTemplate.merge(SQL,
                "table", qapkTable, "alias", qapkSchema.getAllAliases(),
                "app", app);
        if (!ctx.getViewerIdString().equals("")) {
            sql += " AND instr(report_user,'" + ctx.getViewerIdString() + "')<=0 AND instr(report_user,',')<2 ";
        }
        SQLExecutor se = getSqlExecutor();
        int a = (int) se.executeIntScalar(sql, 0);
        return a;
    }

    protected Record getAppsFromApkIds0(Context ctx,ApkId apkId, List<String> cols,int minSDK) {
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
        if (!ctx.getViewerIdString().equals("")) {
            sql.append(" AND instr(report_user,'" + ctx.getViewerIdString() + "')<=0 AND instr(report_user,',')<2 ");
        }
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

    @TraceCall
    @Override
    public RecordSet getAllApps(Context ctx, String sub_category, boolean paid, String sort, String cols, int page, int count, int minSDK) {
        try {
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            RecordSet rs = getApps0(ctx,toStr(sub_category), paid, toStr(sort), l, page, count,minSDK);
            generateUrlsAndDesc(ctx, rs);
            return rs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    private String getApkUrl(String file) {
        return StringUtils.isNotBlank(file) ? String.format(apkUrlPattern, file) : file;
    }

    private String getIconUrl(String file) {
        return StringUtils.isNotBlank(file) ? String.format(iconUrlPattern, file) : file;
    }

    private JsonNode getScreenshotsUrls(JsonNode files) {
        if (!files.isArray() || files.size() == 0)
            return files;

        ArrayNode jn = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < files.size(); i++) {
            String file = files.get(i).getTextValue();
            jn.add(StringUtils.isNotBlank(file) ? String.format(screenshotUrlPattern, file) : file);
        }
        return jn;
    }

    protected void generateUrls(Context ctx, Record rec) {
        if (rec.has("file_url")) {
            rec.put("file_url", getApkUrl(rec.getString("file_url")));
        }
        if (rec.has("icon_url")) {
            rec.put("icon_url", getIconUrl(rec.getString("icon_url")));
        }
        if (rec.has("screenshots_urls")) {
            if (rec.getString("screenshots_urls").toString().length() <= 5) {
                ApkId a = ApkId.parse(toStr(rec.getString("apk_id")));
                String u = getLastedScreenshotsUrls(ctx, a.package_).toString();
                if (u.length() > 5) {
                    JsonNode surl = JsonUtils.fromJson(getLastedScreenshotsUrls(ctx, a.package_).toString(), JsonNode.class);
                    rec.put("screenshots_urls", getScreenshotsUrls((JsonNode) surl));
                } else {
                    rec.put("screenshots_urls", new ArrayList());
                }
            } else {
                rec.put("screenshots_urls", getScreenshotsUrls((JsonNode) rec.get("screenshots_urls")));
            }
        }
    }

    protected void generateDescriptions(Context ctx, Record rec) {
        if (rec.has("description")) {
            if (rec.getString("description").toString().length() <= 5) {
                ApkId a = ApkId.parse(toStr(rec.getString("apk_id")));
                rec.put("description", getLastedDesc(ctx, a.package_).toString());
            }
        }
    }

    protected void generateUrlsAndDesc(Context ctx, RecordSet recs) {
        for (Record rec : recs) {
            generateUrls(ctx, rec);
            generateDescriptions(ctx, rec);
        }
    }

    @TraceCall
    @Override
    public RecordSet getApps(Context ctx, String apps, String cols, int minSDK) {
        try {
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            List<String> apkids = StringUtils2.splitList(toStr(apps), ",", true);

            RecordSet rs = new RecordSet();
            for (String apkid : apkids) {
                ApkId a = ApkId.parse(toStr(apkid));
                Record r = getAppsFromApkIds0(ctx,a, l,minSDK);
                if (!r.isEmpty()) { rs.add(r);}
            }
            generateUrlsAndDesc(ctx, rs);
            return rs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public Record getSingleApp(Context ctx, String apkId, String cols, int minSDK) {
        try {
            ApkId a = ApkId.parse(toStr(apkId));
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            Record rec = getAppsFromApkIds0(ctx,a, l,minSDK);
            generateUrls(ctx, rec);
            generateDescriptions(ctx, rec);
            return rec;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected int findUserMaxVersionCodeFromPackage0(Context ctx,String packageName, String userId,int minSDK) {
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

    protected RecordSet getUserApks0(Context ctx,String userId, int max, String reason, int page, int count,String delApks,int minSDK) {
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
            r.put("version_code", findUserMaxVersionCodeFromPackage0(ctx,r.getString("package"), userId,minSDK));
        }
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getUserApps(Context ctx, String userId, int max, String action, String cols, int page, int count, String delApks, int minSDK) {
        try {
            RecordSet recs = getUserApks0(ctx,toStr(userId), max, toStr(action), page, count,toStr(delApks),minSDK);
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            RecordSet recs2 = returnPointVersionApp(ctx,recs, l,minSDK);
            generateUrlsAndDesc(ctx, recs2);
            return recs2;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getLinkedAppUsers0(Context ctx,String viewerId,String app, String reason,String friendIds, int page, int count,int userType) {
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

    @TraceCall
    @Override
    public RecordSet getLinkedAppUsers(Context ctx, String viewerId, String app, String reason, String friendIds, int page, int count, int userType) {
        try {
            RecordSet recs = getLinkedAppUsers0(ctx,toStr(viewerId),toStr(app), toStr(reason),toStr(friendIds), page, count,userType);
            generateUrlsAndDesc(ctx, recs);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getSingleApps(Context ctx, String packagename, String cols, int minSDK) {
        try {
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            Record rec = Record.of("package", packagename);
            RecordSet recs = new RecordSet();
            recs.add(rec);
            RecordSet rs = returnPointVersionApp(ctx,recs,l,minSDK);
            generateUrlsAndDesc(ctx, rs);
            return rs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected Record getUserLinkApps0(Context ctx,String userId, String packageName) {
        Map<String, String> alias = userQappSchema.getAllAliases();
        StringBuilder sql = new StringBuilder();
        sql.append(SQLTemplate.merge("SELECT * FROM ${table} WHERE ${alias.user}=${v(userId)} AND ${alias.package}=${v(packageName)} ORDER BY ${alias.version_code} DESC LIMIT 1",
                "alias", alias, "userId", userId, "packageName", packageName, "table", userQappTable));
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs.getFirstRecord();
    }

    @TraceCall
    @Override
    public Record getUserLinkApps(Context ctx, String userId, String packagename) {
        try {
            return getUserLinkApps0(ctx,toStr(userId), toStr(packagename));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getManualApkIds0(Context ctx,String type, String maxcount,int minSDK) {
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
                int v = getMaxVersionCodeFromApp(ctx,apkId.package_,minSDK);
                String tApkId = apkId.package_ + "-" + String.valueOf(v) + "-" + "arm";
                ApkId apkId1 = ApkId.parse(tApkId);
                Record app = getAppsFromApkIds0(ctx,apkId1, cols,minSDK);
                apps.add(app);
            }
        }
        return apps;
    }

    @TraceCall
    @Override
    public RecordSet getManualApkIds(Context ctx, String type, String maxcount, int minSDK) {
        try {
            boolean getManualApk = GlobalConfig.get().getBoolean("qiupu.getManualApk", false);
            if (getManualApk) {
                RecordSet recs = getManualApkIds0(ctx,toStr(type), toStr(maxcount), minSDK);
                return recs;
            } else {
                return new RecordSet();
            }
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public boolean setUserAppPrivancy(Context ctx, String userId, String app, String value) {
        try {
            return setUserAppPrivince0(toStr(userId), toStr(app), toStr(value));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected Record existUserActions(Context ctx,String packageName, String user_id, String deviceid, String reason) {
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

    protected boolean updateQappCount0(String packageName, String version_code, String column, long userId, String deviceId) {

        Map<String, String> alias = qapkSchema.getAllAliases();
        final String sql = SQLTemplate.merge("UPDATE ${table} SET " + column + "=" + column + "+1 WHERE ${alias.package}=${v(app)} AND ${alias.version_code}=${v(version_code)}",
                "alias", alias, "table", qapkTable, "app", packageName, "version_code", version_code);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    protected boolean doUpdateCount(Context ctx,String packageName, String version_code, String user_id, String deviceid, String action) {
        int reason = Qiupu.REASON_INSTALLED;
        switch (Integer.valueOf(action)) {
            case Qiupu.ACTION_DOWNLOAD: {
                reason = Qiupu.REASON_DOWNLOADED;
                try {
                    if (existUserActions(ctx,packageName, user_id, deviceid, String.valueOf(reason)).isEmpty()) {
                        updateQappCount0(packageName, version_code, "download_count", Long.parseLong(user_id), deviceid);
                    }
                } catch (Exception t) {
                }
                break;
            }
            case Qiupu.ACTION_FAVORITE: {
                reason = Qiupu.REASON_FAVORITE;
                try {
                    if (existUserActions(ctx,packageName,  user_id, deviceid, String.valueOf(reason)).isEmpty()) {
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
                    if (existUserActions(ctx,packageName,  user_id, deviceid, String.valueOf(reason)).isEmpty()) {
                        updateQappCount0(packageName, version_code, "install_count", Long.parseLong(user_id), deviceid);
                    }
                } catch (Exception t) {
                }
                break;
            }
            case Qiupu.ACTION_UNINSTALL: {
                reason = Qiupu.REASON_UNINSTALLED;
                try {
                    if (existUserActions(ctx,packageName, user_id, deviceid, String.valueOf(reason)).isEmpty()) {
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

    protected boolean doAction(Context ctx,Action action, List<String> sqls) {
        //r2 must be first
        if (action.action == Qiupu.ACTION_INSTALL || action.action == Qiupu.ACTION_DOWNLOAD || action.action == Qiupu.ACTION_FAVORITE || action.action == Qiupu.ACTION_UNINSTALL) {
            doUpdateCount(ctx,action.packageName, toStr(action.versionCode), toStr(action.user), action.deviceid, toStr(action.action));
        }
        if (action.action == Qiupu.ACTION_UPLOAD && !action.user.equals("0") && !action.user.equals(qiupuUid) && !action.user.equals("")) {
            doUpdateCount(ctx,action.packageName, toStr(action.versionCode), toStr(action.user), action.deviceid, toStr(action.action));
        }
        boolean r0 = doHistory(action, sqls);
        boolean r1 = doAssociate(action, sqls);
        return r0 && r1;
    }

    @TraceCall
    @Override
    public boolean setLinkUserApp(Context ctx, String userId, String app, int version_code, int arch, String action, String deviceid, int minSDK) {
        try {
            Action action1 = new Action();
            action1.action = Integer.valueOf(toStr(action));
            action1.packageName = toStr(app);
            action1.user = toStr(userId);
            action1.versionCode = (version_code <= 0)?getMaxVersionCodeFromApp(ctx,toStr(app),minSDK):version_code;
            action1.architecture=arch;
            action1.deviceid=toStr(deviceid);
            return doAction(ctx,action1, null);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean existUserLinkedApp0(Context ctx,String userId, String app, String reason) {
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

    @TraceCall
    @Override
    public boolean existUserLinkedApp(Context ctx, String userId, String app, String reason) {
        try {
            return existUserLinkedApp0(ctx,toStr(userId), toStr(app), toStr(reason));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean existPackage0(Context ctx,String packageName) {
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

    @TraceCall
    @Override
    public boolean existPackage(Context ctx, String packageName) {
        try {
            return existPackage0(ctx,toStr(packageName));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected int getReasonFromApp0(Context ctx,String userId, String app) {
        final String SQL = "SELECT ${alias.reason} FROM ${table} WHERE ${alias.package}=${v(app)} AND ${alias.user}=${v(user)} LIMIT 1";

        String sql = SQLTemplate.merge(SQL,
                "table", userQappTable, "alias", userQappSchema.getAllAliases(),
                "app", app, "user", userId);
        SQLExecutor se = getSqlExecutor();
        int a = (int) se.executeIntScalar(sql, 0);
        return a;
    }

    @TraceCall
    @Override
    public int getReasonFromApp(Context ctx, String userId, String app) {
        try {
            return getReasonFromApp0(ctx,toStr(userId), toStr(app));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public int getMaxVersionCode(Context ctx, String app, int minSDK) {
        try {
            String ss[] = StringUtils2.splitArray(toStr(app), "-", true);
            if (ss.length > 2) {
                return getMaxVersionCodeFromApp(ctx,toStr(ss[0]),minSDK);
            } else {
                return getMaxVersionCodeFromApp(ctx,toStr(app),minSDK);
            }
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet searchApps0(Context ctx,String value, List<String> cols, int page, int count, int minSDK) {
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
            if (!ctx.getViewerIdString().equals("")) {
                sql.append(" AND instr(report_user,'" + ctx.getViewerIdString() + "')<=0 AND instr(report_user,',')<2 ");
            }
            sql1.append(SQLTemplate.merge(" group by ${alias.package} ORDER BY count1 DESC ${limit}",
                    "alias", alias, "limit", SQLUtils.pageToLimit(0, count - recs.size())));

            RecordSet recs_t = se.executeRecordSet(sql1.toString(), null);
            recs.addAll(recs_t);
            recs.unique("package") ;
        }

        return returnPointVersionApp(ctx,recs, cols, minSDK);
    }

    @TraceCall
    @Override
    public RecordSet searchApps(Context ctx, String value, String cols, int page, int count, int minSDK) {
        try {
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            RecordSet recs = searchApps0(ctx,toStr(value), l, page, count,minSDK);
            generateUrlsAndDesc(ctx, recs);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    protected boolean saveUserSetting0(String userId, String name, String value) {
        final String sql = SQLTemplate.merge("INSERT INTO ${table} VALUES(${v(user)},${v(name)},${v(value)})",
                "table", userQsettingTable, "name", name, "user", userId, "value", value);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean userSettingAll(Context ctx, String userId, String name, String value) {
        try {
            if (existUserSettingAll0(toStr(userId), toStr(name))) {
                setUserAppPrivinceAll0(toStr(userId), toStr(value));
                updateUserSetting0(toStr(userId), toStr(name), toStr(value));
            } else {
                saveUserSetting0(toStr(userId), toStr(name), toStr(value));
            }

            return 1 > 0;

        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public boolean updateQappCount(Context ctx, String apkId, String column, long userId, String deviceId) {
        try {
            ApkId a = ApkId.parse(toStr(apkId));
            return updateQappCount0(toStr(a.package_), toStr(a.versionCode), toStr(column), userId, toStr(deviceId));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public boolean download(Context ctx, String userId, String app, String action, String deviceid) {
        try {
            ApkId a = ApkId.parse(toStr(app));

            Action action1 = new Action();
            action1.action = Integer.valueOf(toStr(action));
            action1.packageName = a.package_;
            action1.user = toStr(userId);
            action1.versionCode = a.versionCode;
            action1.deviceid = toStr(deviceid);

            return doAction(ctx,action1, null);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }


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

    @TraceCall
    @Override
    public boolean deleteUserQapp(Context ctx, String userId, String app, String reason, int minSDK) {
        try {
            List<String> apps_l = StringUtils2.splitList(toStr(app), ",", true);
            for (String l : apps_l) {
                Action action1 = new Action();
                action1.action = Integer.valueOf(toStr(reason));
                action1.packageName = toStr(l);
                action1.user = toStr(userId);
                action1.versionCode = getMaxVersionCodeFromApp(ctx,toStr(app),minSDK);
                doHistory(action1, null);
            }
            return deleteUserQapp0(toStr(userId), toStr(app));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public boolean uploadApk(Context ctx, String user, Record apkInfo, String deviceid) {
        boolean r0 = false;
        boolean r1 = true;
        boolean r2 = true;

        try {
            Record rec = apkInfo;

            // check missing columns
            Schemas.checkRecordIncludeColumns(rec, "package", "app_name", "version_code", "version_name", "icon_url",
                    "file_url", "file_size", "min_sdk_version", "target_sdk_version", "max_sdk_version", "architecture");

            //save apk info
            rec.put("created_time", DateUtils.nowMillis());

            r0 = saveApk(rec);

            //save associate and history
            String user0 = toStr(user);
            if (StringUtils.isNotBlank(user0)) {
                Action action = new Action();
                action.user = user0;
                action.packageName = rec.getString("package");
                action.action = Qiupu.ACTION_INSTALL;
                action.versionCode = (int) rec.getInt("version_code");
                action.architecture = (int) rec.getInt("architecture", Qiupu.ARCH_ARM);
                action.versionName = rec.getString("version_name");
                action.deviceid = toStr(deviceid);
                r1 = doAction(ctx,action, null);

                action.action = Qiupu.ACTION_UPLOAD;
                r2 = doAction(ctx,action, null);
            }
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }

        return r0 && r1 && r2;
    }

    protected boolean updateLastTimeApkForSync0(String userId, String packageName) {
        String sql ="update user_qapp set last_installed_time=" + DateUtils.nowMillis() + " where user=" + userId + " and package='" + packageName + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    protected RecordSet sync(Context ctx, String user, RecordSet userApps, String[] apkIds, boolean all,String deviceid,int minSDK) {

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
            int vc = getMaxVersionCodeFromApp(ctx,packageName,minSDK);      //获取当前传递进来的应用的最大版本号
            int ar = Qiupu.ARCH_ARM;
            ApkId a = ApkId.of(ap, vc, ar);             //获取最大版本号的APKID

            Record app = (versionCode > vc) ? null : getAppsFromApkIds0(ctx,a, l,minSDK);        //如果传递进来的版本比最大版本新，则返回null，否则   获取最新的APP
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

                    generateUrls(ctx, app);
                    generateDescriptions(ctx, app);

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

        doActions(ctx,actions);

        return all ? userApps : userApps0;
    }

    protected boolean doActions(Context ctx,List<Action> actions) {
        List<String> sqls = new ArrayList<String>();
        for (Action action : actions) {
            doAction(ctx,action, sqls);
        }

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sqls);
        return n > 0;
    }

    protected Record getLastInstalledTimeApp0(String userId,String packageName) {
        String sql = "SELECT last_installed_time FROM user_qapp where user='"+userId+"' and package='"+packageName+"' order by last_installed_time limit 1";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        return rec;
    }

    @TraceCall
    @Override
    public RecordSet syncApks(Context ctx, String user, String apkIds, boolean all, String deviceid, int minSDK) {
        String user0 = toStr(user);
        boolean b = StringUtils.isNotBlank(user0);
//        //先把上一次安装的所有为1的找出来
//        RecordSet last_package = new RecordSet();
//        if (b)
//            last_package = getLastInstalledApp0(user0);
//        List<String> lPackage = new ArrayList<String>();
//        for (Record r0 : last_package) {
//            lPackage.add(r0.getString("package"));
//        }
        String[] apkIdsArr = StringUtils2.splitArray(toStr(apkIds), ",", true);
        if (b == true && apkIdsArr.length > 0) {
            for (String a : apkIdsArr) {
                String packageName0 = a.split("-")[0];
                updateLastTimeApkForSync0(user0, packageName0);
            }
        }

        RecordSet userApps = new RecordSet();
        if (b == true) {
            userApps = getUserApps(ctx, user, Qiupu.PRIVACY_ME, "",
                    "package,version_code,version_name,file_url,rating,file_size,icon_url,app_name", 0, 1000, "", minSDK);
            //generateUrlsAndDesc(userApps);
        }
        RecordSet rs = sync(ctx, user0, userApps, apkIdsArr, all, toStr(deviceid), minSDK);
//        if (b==false) {
//            generateUrlsAndDesc(rs);
//        }
//        //先把本次的全部更新进去，置为1
//        if (all == true && b == true && apkIdsArr.length > 0) {
//            UpdateApkForSync0(user0, "", false);
//            for (String a : apkIdsArr) {
//                String packageName0 = a.split("-")[0];
//                UpdateApkForSync0(user0, packageName0, true);
//            }
//        }

        //循环把里面为1的标志出来
        for (Record r : rs) {
            String out_package = r.getString("package");
            r.put("last_installed_time",getLastInstalledTimeApp0(user0,out_package));
        }
        return rs;
    }

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

    @TraceCall
    @Override
    public RecordSet findApkByPackageAndVersion(Context ctx, String packageName, String versionCode) {
        try {
            return findApkByPackageAndVersion0(toStr(packageName), toStr(versionCode));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getLastInstalledApp0(String userId) {
        String sql = "SELECT distinct(package) FROM user_qapp where user="+userId+" and last_installed=1";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getLastInstalledApp(Context ctx, String user, String cols, int minSDK) {
        String user0 = toStr(user);
        boolean b = StringUtils.isNotBlank(user0);
        //先把上一次安装的所有为1的找出来
        RecordSet last_package = new RecordSet();
        if (b)
            last_package = getLastInstalledApp0(user0);
        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        RecordSet recs2 = returnPointVersionApp(ctx,last_package, l, minSDK);
        generateUrlsAndDesc(ctx, recs2);
        return recs2;
    }

    @TraceCall
    @Override
    public String getUserSettingAll(Context ctx, String userId, String name) {
        try {
            return getUserSettingAll0(toStr(userId), toStr(name));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public String getUserSingleAppPrivancy(Context ctx, String userId, String app) {
        try {
            return getUserSingleAppPrivancy0(toStr(userId), toStr(app));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public String getLastedDesc(Context ctx, String app) {
        try {
            return getLastedDesc0(toStr(app));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public String getLastedScreenshotsUrls(Context ctx, String app) {
        try {
            return getLastedScreenshotsUrls0(toStr(app));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public boolean manualApks(Context ctx, String apkIds, String types) {
        String[] apkIdsArr = StringUtils2.splitArray(toStr(apkIds), ",", true);
        long[] typesArr = StringUtils2.splitIntArray(toStr(types), ",");

        boolean result = true;
        if (typesArr.length < apkIdsArr.length) {
            for (int i = 0; i < apkIdsArr.length; i++) {
                result = result && manualApk(apkIdsArr[i], typesArr[0]);
            }
        } else {
            for (int i = 0; i < apkIdsArr.length; i++) {
                result = result && manualApk(apkIdsArr[i], typesArr[i]);
            }
        }
        return result;
    }

    protected RecordSet getManualApks0(Context ctx,int type,int minSDK) {
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
                int v = getMaxVersionCodeFromApp(ctx,apkId.package_,minSDK);
                String tApkId = apkId.package_ + "-" + String.valueOf(v) + "-" + "arm";
                ApkId apkId1 = ApkId.parse(tApkId);
                Record app = getAppsFromApkIds0(ctx,apkId1, cols,minSDK);
                apps.add(app);
            }
        }
        return apps;
    }

    @TraceCall
    @Override
    public RecordSet getManualApks(Context ctx, int type, int minSDK) {
        RecordSet rs = getManualApks0(ctx,type,minSDK);
        generateUrlsAndDesc(ctx, rs);
        return rs;
    }

    private String getAppName(String package_, int versionCode, long arch) {
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
        return (String) se.executeScalar(sql);
    }

    protected RecordSet loadNeedExinfoApks0(Context ctx,boolean isAll) {
        String SQL = "SELECT ${alias.app_name},${alias.package},${alias.version_code},${alias.architecture}"
                + " FROM ${table}"; //WHERE ${alias.description}='' OR ${alias.screenshots_urls}='[]'" +
//				" OR ${alias.sub_category}=0";

        if (!isAll) {
            SQL += " WHERE ${alias.description}='' OR ${alias.screenshots_urls}='[]' OR ${alias.screenshots_urls}=''";
        }
        if (!ctx.getViewerIdString().equals("")) {
            SQL += " AND instr(report_user,'" + ctx.getViewerIdString() + "')<=0 AND instr(report_user,',')<2 ";
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
            int maxVersion = getMaxVersionCodeFromApp(ctx,app,1000);
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

    @TraceCall
    @Override
    public RecordSet loadNeedExinfoApks(Context ctx, boolean isAll) {
        return loadNeedExinfoApks0(ctx,isAll);
    }

    protected int getApplicationCount0() {
        final String SQL = "SELECT count(distinct ${alias.package}) FROM ${table}";

        String sql = SQLTemplate.merge(SQL,
                "table", qapkTable,
                "alias", qapkSchema.getAllAliases());

        SQLExecutor se = getSqlExecutor();
        Object obj = se.executeScalar(sql);

        return ((Number) obj).intValue();
    }

    @TraceCall
    @Override
    public int getApplicationCount(Context ctx) {
        return getApplicationCount0();
    }

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

    @TraceCall
    @Override
    public int getTodayAppCount(Context ctx) {
        return getTodayAppCount0();
    }

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

    @TraceCall
    @Override
    public int getNeedExinfoAppCount(Context ctx) {
        return getNeedExinfoAppCount0();
    }

    protected int getUserAppCount0(String userId) {
        final String sql = "select count(distinct(package)) from user_qapp where user="+ userId +""; //+

        SQLExecutor se = getSqlExecutor();
        Object obj = se.executeScalar(sql);

        return ((Number) obj).intValue();
    }

    @TraceCall
    @Override
    public int getUserAppCount(Context ctx, String userId) {
        try {
            return getUserAppCount0(toStr(userId));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean updateApk0(Record info) {
        Schemas.standardize(qapkSchema, info);

        long now = DateUtils.nowMillis();
        String sql = new SQLBuilder.Update(qapkSchema).update(qapkTable).values(info).value("info_updated_time", now).where("${alias.package}=${v(package)}", "package", info.getString("package")).and("${alias.version_code}=${v(version_code)}", "version_code", info.getInt("version_code")).and("${alias.architecture}=${v(architecture)}", "architecture", info.getInt("architecture")).toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean updateApk(Context ctx, Record info) {
        Record apk = info;
        Schemas.checkRecordIncludeColumns(apk, "package", "version_code", "architecture");
        Schemas.checkRecordExcludeColumns(apk, "info_updated_time");
        if(apk.has("price"))
        {
            String price = apk.getString("price");
            apk.put("price", StringUtils.substringAfter(price, "US$"));
        }
        return updateApk0(apk);
    }

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

    @TraceCall
    @Override
    public RecordSet getQapkOtherVersion(Context ctx, String apkId, int minSDK) {
        try {
            RecordSet recs = getQapkOtherVersion0(toStr(apkId),minSDK);
            generateUrlsAndDesc(ctx, recs);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    protected boolean saveQapkSuggest0(Record suggestRecord) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, suggestRecord)}";
        String sql = SQLTemplate.merge(SQL,
                "table", qapkSuggestTable, "alias", qapkSuggestSchema.getAllAliases(),
                "suggestRecord", suggestRecord);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean saveQapkSuggest(Context ctx, String sub_id, String sub_name, String policy, String hdpi_img_url, String mdpi_img_url) {
        try {
            Record r = new Record();
            r.put("hdpi",toStr(hdpi_img_url));
            r.put("mdpi",toStr(mdpi_img_url));
            if (existSuggestApk(ctx, toStr(sub_id), toStr(sub_name))) {
                return updateQapkSuggest0(toStr(sub_id), toStr(sub_name), toStr(policy),r.toString(false,false));
            } else {
                Record suggestRecord = Record.of("sub_id", sub_id, "sub_name", sub_name, "policy", policy);
                suggestRecord.put("img_url", r.toString(false,false));
                suggestRecord.put("created_time", DateUtils.nowMillis());
                suggestRecord.put("destroyed_time", 0);
                return saveQapkSuggest0(suggestRecord);
            }
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public boolean existSuggestApk(Context ctx, String sub_id, String sub_name) {
        try {
            return existSuggestApk0(toStr(sub_id), toStr(sub_name));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean deleteQapkSuggest0(String sub_id) {
        final String SQL = "UPDATE ${table} SET ${alias.destroyed_time}=${v(destroyed_time)} WHERE ${alias.sub_id}=${v(sub_id)}";
        String sql = SQLTemplate.merge(SQL,
                "table", qapkSuggestTable, "alias", qapkSuggestSchema.getAllAliases(),
                "sub_id", sub_id,"destroyed_time", DateUtils.nowMillis());

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean deleteQapkSuggest(Context ctx, String sub_id) {
        try {
            return deleteQapkSuggest0(toStr(sub_id));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public boolean updateQapkSuggest(Context ctx, String sub_id, String sub_name, String policy, String hdpi_img_url, String mdpi_img_url) {
        try {
            Record r = new Record();
            r.put("hdpi",toStr(hdpi_img_url));
            r.put("mdpi",toStr(mdpi_img_url));
            return updateQapkSuggest0(toStr(sub_id),toStr(sub_name),toStr(policy),r.toString(false,false));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public boolean updateQapkIfSuggest(Context ctx, String sub_id) {
        try {
            return updateQapkSuggest1(toStr(sub_id));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    @TraceCall
    @Override
    public RecordSet getSuggestApkfromSubId(Context ctx, String sub_id, boolean ifsuggest) {
        try {
            RecordSet recs = getSuggestApkfromSubId0(toStr(sub_id),ifsuggest);
            for (Record r : recs) {
                String t = r.getString("img_url");
                if (t.length() > 0) {
                    Record rec = Record.fromJson(t);

                    Record rt = new Record();
                    rt.put("hdpi", String.format(subApksImgUrlPattern, rec.getString("hdpi")));
                    rt.put("mdpi", String.format(subApksImgUrlPattern, rec.getString("mdpi")));

                    r.put("img_url", rt);
                }
            }
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getApkIdsListByTimes0(Context ctx,long datediff, int action, int limit,int minSDK) {
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
                outRS.add(Record.of("apk_id", r.getString("package") + "-" + getMaxVersionCodeFromApp(ctx,r.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }

    @TraceCall
    @Override
    public RecordSet getApkIdsListByTimes(Context ctx, long datediff, int action, int limit, int minSDK) {
        try {
            RecordSet recs = getApkIdsListByTimes0(ctx,datediff,action,limit,minSDK);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getApkIdsListByDownloadTimes0(Context ctx,int limit,int minSDK) {
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
                outRS.add(Record.of("apk_id", r.getString("package") + "-" + getMaxVersionCodeFromApp(ctx,r.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }

    @TraceCall
    @Override
    public RecordSet getApkIdsListByDownloadTimes(Context ctx, int limit, int minSDK) {
        try {
            RecordSet recs = getApkIdsListByDownloadTimes0(ctx,limit,minSDK);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getApkIdsListByRating0(Context ctx,int limit,int minSDK) {
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
                outRS.add(Record.of("apk_id", r.getString("package") + "-" + getMaxVersionCodeFromApp(ctx,r.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }

    @TraceCall
    @Override
    public RecordSet getApkIdsListByRating(Context ctx, int limit, int minSDK) {
        try {
            RecordSet recs = getApkIdsListByRating0(ctx,limit,minSDK);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getApkIdsListByBorqs0(Context ctx,int limit,int minSDK) {
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
                outRS.add(Record.of("apk_id", r.getString("package") + "-" + getMaxVersionCodeFromApp(ctx,r.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }

    @TraceCall
    @Override
    public RecordSet getApkIdsListByBorqs(Context ctx, int limit, int minSDK) {
        try {
            RecordSet recs = getApkIdsListByBorqs0(ctx,limit,minSDK);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getApkIdsListByRandom0(Context ctx,int limit,int minSDK) {
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
                    outRS.add(Record.of("apk_id", m.getString("package") + "-" + getMaxVersionCodeFromApp(ctx,m.getString("package"),minSDK) + "-" + "arm"));
            }
        }
        return outRS;
    }

    @TraceCall
    @Override
    public RecordSet getApkIdsListByRandom(Context ctx, int limit, int minSDK) {
        try {
            RecordSet recs = getApkIdsListByRandom0(ctx,limit,minSDK);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public RecordSet getUsersAppCount(Context ctx, String userIds, String reason) {
        try {
            RecordSet recs = getUsersAppCount0(toStr(userIds),toStr(reason));
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public RecordSet getStrongMan(Context ctx, String sub_category, int page, int count) {
        try {
            RecordSet recs = getStrongMan0(toStr(sub_category),page,count);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public boolean createUpdateApkLessDesc(Context ctx, String packageName, int version_code, String app_name, String desc) {
        try {
            return createUpdateApkLessDesc0(toStr(packageName),version_code,toStr(app_name),toStr(desc));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    public RecordSet getApkByVersionCode(String packageName,int version_code) {
        String sql = "SELECT * FROM qapk WHERE package='"+packageName+"' AND version_code='"+version_code+"'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    public boolean updateReportUser(String packageName, int version_code, String report_user) {
        String sql = "UPDATE qapk SET report_user='" + report_user + "' WHERE package='" + packageName + "' AND version_code='" + version_code + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public RecordSet getUpdateApkLessDesc(Context ctx, int page, int count, String packageName, String app_name) {
        try {
            RecordSet recs = getUpdateApkLessDesc0(page,count,toStr(packageName),toStr(app_name));
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

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

    @TraceCall
    @Override
    public RecordSet getTop1ApkByCategory(Context ctx, String sub_category) {
        try {
            RecordSet recs = getTop1ApkByCategory0(toStr(sub_category));
            generateUrlsAndDesc(ctx, recs);
            return recs;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet formatOldDataToConversation0(String viewerId) {
//        String sql = "select upload_user,package,created_time from qapk where upload_user>0";

        String sql = "select user,package  from user_qapp where user>0 and reason & 8<>0";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet formatOldDataToConversation(Context ctx, String viewerId) {
        try {
            return formatOldDataToConversation0(toStr(viewerId));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean updateBorqsByPackage0(String package_, int borqs) {
        String sql = "UPDATE qapk SET borqs=" + borqs + " WHERE package='" + package_ + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean updateBorqsByPackage(Context ctx, String package_, int borqs) {
        try {
            return updateBorqsByPackage0(toStr(package_), borqs);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }
}
