package com.borqs.server.qiupu;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.service.qiupu.Qiupu;
import com.borqs.server.service.qiupu.QiupuInterface;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class QiupuBase extends RPCService implements QiupuInterface {

    public final Schema qapkHistorySchema = Schema.loadClassPath(QiupuBase.class, "qapk_history.schema");
    public final Schema qapkModelSchema = Schema.loadClassPath(QiupuBase.class, "qapk_model.schema");
    public final Schema userQappSchema = Schema.loadClassPath(QiupuBase.class, "user_qapp.schema");
    public final Schema qapkSchema = Schema.loadClassPath(QiupuBase.class, "qapk.schema");
    public final Schema userQsettingSchema = Schema.loadClassPath(QiupuBase.class, "user_qsetting.schema");
    public final Schema qapkNotinPoolSchema = Schema.loadClassPath(QiupuBase.class, "qapk_notin_pool.schema");
    public final Schema qapkManualSchema = Schema.loadClassPath(QiupuBase.class, "qapk_manual.schema");
    public final Schema qapkSuggestSchema = Schema.loadClassPath(QiupuBase.class, "qapk_suggest.schema");
    public final Schema updateApkLessDescSchema = Schema.loadClassPath(QiupuBase.class, "updateApkLessDesc.schema");

    private String apkUrlPattern;
    private String iconUrlPattern;
    private String screenshotUrlPattern;
    private String subApksImgUrlPattern;

    public QiupuBase() {
    }

    @Override
    public Class getInterface() {
        return QiupuInterface.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        Configuration conf = getConfig();

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
    }

    @Override
    public void destroy() {
    }

    protected abstract boolean saveApk(Record apkInfo);

    protected abstract boolean doHistory(Action action, List<String> sqls);

    protected abstract boolean doAssociate(Action action, List<String> sqls);

    protected abstract boolean doAction(Action action, List<String> sqls);

    protected abstract boolean doActions(List<Action> actions);

    protected abstract boolean isAssociate(long user, String packageName);
    
    protected abstract boolean isAssociate(String deviceId, String packageName);

    protected abstract RecordSet getApps0(String sub_category, boolean paid, String sort, List<String> cols, int page, int count,int minSDK);

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

    protected void generateUrls(Record rec) throws AvroRemoteException {
        if (rec.has("file_url")) {
            rec.put("file_url", getApkUrl(rec.getString("file_url")));
        }
        if (rec.has("icon_url")) {
            rec.put("icon_url", getIconUrl(rec.getString("icon_url")));
        }
        if (rec.has("screenshots_urls")) {
            if (rec.getString("screenshots_urls").toString().length() <= 5) {
                ApkId a = ApkId.parse(toStr(rec.getString("apk_id")));
                String u = getLastedScreenshotsUrls(a.package_).toString();
                if (u.length() > 5) {
                    JsonNode surl = JsonUtils.fromJson(getLastedScreenshotsUrls(a.package_).toString(), JsonNode.class);
                    rec.put("screenshots_urls", getScreenshotsUrls((JsonNode) surl));
                } else {
                    rec.put("screenshots_urls", new ArrayList());
                }
            } else {
                rec.put("screenshots_urls", getScreenshotsUrls((JsonNode) rec.get("screenshots_urls")));
            }
        }
    }
    protected void generateDescriptions(Record rec) throws AvroRemoteException {
        if (rec.has("description")) {
            if (rec.getString("description").toString().length() <= 5) {
                ApkId a = ApkId.parse(toStr(rec.getString("apk_id")));
                rec.put("description", getLastedDesc(a.package_).toString());
            }
        }
    }
    protected void generateUrlsAndDesc(RecordSet recs) throws AvroRemoteException {
        for (Record rec : recs) {
            generateUrls(rec);
            generateDescriptions(rec);
        }
    }

    protected abstract boolean existPackage0(String packageName);
    
    @Override
    public boolean existPackage(CharSequence packageName) throws AvroRemoteException, ResponseError {
        try {
            return existPackage0(toStr(packageName));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    @Override
    public ByteBuffer getAllApps(CharSequence sub_category, boolean paid, CharSequence sort, CharSequence cols, int page, int count,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            RecordSet rs = getApps0(toStr(sub_category), paid, toStr(sort), l, page, count,minSDK);
            generateUrlsAndDesc(rs);
            return rs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet findApkByPackageAndVersion0(String packageName,String versionCode);
    
    @Override
    public ByteBuffer findApkByPackageAndVersion(CharSequence packageName, CharSequence versionCode) throws AvroRemoteException, ResponseError {
        try {
            return findApkByPackageAndVersion0(toStr(packageName),toStr(versionCode)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet returnPointVersionApp(RecordSet packages, List<String> cols,int minSDK);

    @Override
    public ByteBuffer getApps(CharSequence apps, CharSequence cols,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            List<String> apkids = StringUtils2.splitList(toStr(apps), ",", true);

            RecordSet rs = new RecordSet();
            for (String apkid : apkids) {
                ApkId a = ApkId.parse(toStr(apkid));
                Record r = getAppsFromApkIds0(a, l,minSDK);
                if (!r.isEmpty()) { rs.add(r);}
            }
            generateUrlsAndDesc(rs);
            return rs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getSingleApps(CharSequence packagename, CharSequence cols,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            Record rec = Record.of("package", packagename);
            RecordSet recs = new RecordSet();
            recs.add(rec);
            RecordSet rs = returnPointVersionApp(recs,l,minSDK);
            generateUrlsAndDesc(rs);
            return rs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet getManualApkIds0(String type,String maxcount,int minSDK);
    
    @Override
    public ByteBuffer getManualApkIds(CharSequence type, CharSequence maxcount,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getManualApkIds0(toStr(type), toStr(maxcount),minSDK);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract int getReasonFromApp0(String userId, String app);

    @Override
    public int getReasonFromApp(CharSequence userId, CharSequence app) throws AvroRemoteException, ResponseError {
        try {
            return getReasonFromApp0(toStr(userId), toStr(app));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int getUserAppCount0(String userId);

    @Override
    public int getUserAppCount(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return getUserAppCount0(toStr(userId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getAppsFromApkIds0(ApkId apkId, List<String> cols,int minSDK);

    @Override
    public ByteBuffer getSingleApp(CharSequence apkId, CharSequence cols,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            ApkId a = ApkId.parse(toStr(apkId));
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            Record rec = getAppsFromApkIds0(a, l,minSDK);
            generateUrls(rec);
            generateDescriptions(rec);
            return rec.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getUserApks0(String userId, int max, String action, int page, int count,String delApks,int minSDK);

    @Override
    public ByteBuffer getUserApps(CharSequence userId, int max, CharSequence action, CharSequence cols, int page, int count,CharSequence delApks,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getUserApks0(toStr(userId), max, toStr(action), page, count,toStr(delApks),minSDK);
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            RecordSet recs2 = returnPointVersionApp(recs, l,minSDK);
            generateUrlsAndDesc(recs2);
            return recs2.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean existUserLinkedApp0(String userId, String app, String reason);

    @Override
    public boolean existUserLinkedApp(CharSequence userId, CharSequence app, CharSequence reason) throws AvroRemoteException, ResponseError {
        try {
            return existUserLinkedApp0(toStr(userId), toStr(app), toStr(reason));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }


    protected abstract RecordSet getLinkedAppUsers0(String viewerId,String app, String reason,String friendIds, int page, int count,int userType);

    @Override
    public ByteBuffer getLinkedAppUsers(CharSequence viewerId,CharSequence app, CharSequence reason,CharSequence friendIds, int page, int count,int userType) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getLinkedAppUsers0(toStr(viewerId),toStr(app), toStr(reason),toStr(friendIds), page, count,userType);
            generateUrlsAndDesc(recs);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean setUserAppPrivince0(String userId, String app, String value);

    protected abstract boolean setUserAppPrivinceAll0(String userId, String value);

    protected abstract int getMaxVersionCodeFromApp(String app,int minSDK);

    @Override
    public int getMaxVersionCode(CharSequence app,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            String ss[] = StringUtils2.splitArray(toStr(app), "-", true);
            if (ss.length > 2) {
                return getMaxVersionCodeFromApp(toStr(ss[0]),minSDK);
            } else {
                return getMaxVersionCodeFromApp(toStr(app),minSDK);
            }
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    @Override
    public boolean setUserAppPrivancy(CharSequence userId, CharSequence app, CharSequence value) throws AvroRemoteException, ResponseError {
        try {
            return setUserAppPrivince0(toStr(userId), toStr(app), toStr(value));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean setUserQapp0(String userId, String app, String reason);

    @Override
    public boolean setLinkUserApp(CharSequence userId, CharSequence app, int version_code,int arch, CharSequence action, CharSequence deviceid,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            Action action1 = new Action();
            action1.action = Integer.valueOf(toStr(action));
            action1.packageName = toStr(app);
            action1.user = toStr(userId);
            action1.versionCode = (version_code <= 0)?getMaxVersionCodeFromApp(toStr(app),minSDK):version_code;
            action1.architecture=arch;
            action1.deviceid=toStr(deviceid);
            return doAction(action1, null);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getUserLinkApps0(String userId, String packageName);
    
    @Override
    public ByteBuffer getUserLinkApps(CharSequence userId, CharSequence packageName) throws AvroRemoteException, ResponseError {
        try {
            return getUserLinkApps0(toStr(userId), toStr(packageName)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    
    protected abstract boolean deleteUserQapp0(String userId, String apps);

    @Override
    public boolean deleteUserQapp(CharSequence userId, CharSequence app, CharSequence reason,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            List<String> apps_l = StringUtils2.splitList(toStr(app), ",", true);
            for (String l : apps_l) {
                Action action1 = new Action();
                action1.action = Integer.valueOf(toStr(reason));
                action1.packageName = toStr(l);
                action1.user = toStr(userId);
                action1.versionCode = getMaxVersionCodeFromApp(toStr(app),minSDK);
                doHistory(action1, null);
            }
            return deleteUserQapp0(toStr(userId), toStr(app));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet searchApps0(String value, List<String> cols, int page, int count,int minSDK);

    @Override
    public ByteBuffer searchApps(CharSequence value, CharSequence cols, int page, int count,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
            RecordSet recs = searchApps0(toStr(value), l, page, count,minSDK);
            generateUrlsAndDesc(recs);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateQappCount0(String packageName, String version_code, String column, long userId, String deviceId);

    @Override
    public boolean updateQappCount(CharSequence apkId, CharSequence column, long userId, CharSequence deviceId) throws AvroRemoteException, ResponseError {
        try {
            ApkId a = ApkId.parse(toStr(apkId));
            return updateQappCount0(toStr(a.package_), toStr(a.versionCode), toStr(column), userId, toStr(deviceId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean saveUserSetting0(String userId, String app, String value);

    protected abstract boolean updateUserSetting0(String userId, String app, String value);

    protected abstract boolean existUserSettingAll0(String userId, String name);

    protected abstract String getUserSettingAll0(String userId, String name);
    @Override
    public CharSequence getUserSettingAll(CharSequence userId, CharSequence name) throws AvroRemoteException, ResponseError {
        try {
               return getUserSettingAll0(toStr(userId), toStr(name));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract String getUserSingleAppPrivancy0(String userId, String app);
    
    @Override
    public CharSequence getUserSingleAppPrivancy(CharSequence userId, CharSequence app) throws AvroRemoteException, ResponseError {
        try {
               return getUserSingleAppPrivancy0(toStr(userId), toStr(app));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract String getLastedDesc0(String app);
    
    @Override
    public CharSequence getLastedDesc(CharSequence app) throws AvroRemoteException, ResponseError {
        try {
               return getLastedDesc0(toStr(app));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract String getLastedScreenshotsUrls0(String app);
    
    @Override
    public CharSequence getLastedScreenshotsUrls(CharSequence app) throws AvroRemoteException, ResponseError {
        try {
               return getLastedScreenshotsUrls0(toStr(app));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet sync(String user, RecordSet userApps, String[] packages, boolean all,String deviceid,int minSDK);
    protected abstract boolean manualApk(String apk_id, long type);
    protected abstract RecordSet getManualApks0(int type,int minSDK);
    
    protected abstract RecordSet loadNeedExinfoApks0(boolean isAll);
    protected abstract int getApplicationCount0();
    protected abstract int getTodayAppCount0();
    protected abstract int getNeedExinfoAppCount0();
    protected abstract boolean updateApk0(Record info);

    @Override
    public boolean userSettingAll(CharSequence userId, CharSequence name, CharSequence value) throws AvroRemoteException, ResponseError {
        try {
            if (existUserSettingAll0(toStr(userId), toStr(name))) {
                setUserAppPrivinceAll0(toStr(userId), toStr(value));
                updateUserSetting0(toStr(userId), toStr(name), toStr(value));
            } else {
                saveUserSetting0(toStr(userId), toStr(name), toStr(value));
            }

            return 1 > 0;

        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean download(CharSequence userId, CharSequence app, CharSequence action, CharSequence deviceid) throws AvroRemoteException, ResponseError {
        try {
            ApkId a = ApkId.parse(toStr(app));
            
            Action action1 = new Action();
            action1.action = Integer.valueOf(toStr(action));
            action1.packageName = a.package_;
            action1.user = toStr(userId);
            action1.versionCode = a.versionCode;
            action1.deviceid = toStr(deviceid);

            return doAction(action1, null);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean uploadApk(CharSequence user, ByteBuffer apkInfo, CharSequence deviceid) throws AvroRemoteException, ResponseError {
        boolean r0 = false;
        boolean r1 = true;
        boolean r2 = true;

        try {
            Record rec = Record.fromByteBuffer(apkInfo);

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
                r1 = doAction(action, null);

                action.action = Qiupu.ACTION_UPLOAD;
                r2 = doAction(action, null);
            }
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }

        return r0 && r1 && r2;
    }

    protected abstract boolean updateLastTimeApkForSync0(String userId, String packageName);
    protected abstract Record getLastInstalledTimeApp0(String userId,String packageName);
    @Override
    public ByteBuffer syncApks(CharSequence user, CharSequence apkIds, boolean all, CharSequence deviceid,int minSDK) throws AvroRemoteException {
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
            userApps = RecordSet.fromByteBuffer(getUserApps(user, Qiupu.PRIVACY_ME, "",
                    "package,version_code,version_name,file_url,rating,file_size,icon_url,app_name", 0, 1000, "", minSDK));
            //generateUrlsAndDesc(userApps);
        }
        RecordSet rs = RecordSet.fromByteBuffer(sync(user0, userApps, apkIdsArr, all, toStr(deviceid), minSDK).toByteBuffer());
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
        return rs.toByteBuffer();
    }

    @Override
    public ByteBuffer getLastInstalledApp(CharSequence user, CharSequence cols, int minSDK) throws AvroRemoteException {
        String user0 = toStr(user);
        boolean b = StringUtils.isNotBlank(user0);
        //先把上一次安装的所有为1的找出来
        RecordSet last_package = new RecordSet();
        if (b)
            last_package = getLastInstalledApp0(user0);
        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        RecordSet recs2 = returnPointVersionApp(last_package, l, minSDK);
        generateUrlsAndDesc(recs2);
        return recs2.toByteBuffer();
    }

    protected abstract boolean UpdateApkForSync0(String userId, String packageName, boolean last_installed);
    protected abstract RecordSet getLastInstalledApp0(String userId);
    @Override
    public boolean manualApks(CharSequence apkIds, CharSequence types) throws AvroRemoteException {
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
    
    @Override
    public ByteBuffer getManualApks(int type,int minSDK) throws AvroRemoteException {
    	RecordSet rs = getManualApks0(type,minSDK);
    	generateUrlsAndDesc(rs);
    	return rs.toByteBuffer();
    }
        
    @Override
    public ByteBuffer loadNeedExinfoApks(boolean isAll) throws AvroRemoteException {
    	return loadNeedExinfoApks0(isAll).toByteBuffer();
    }
    
    @Override
    public int getApplicationCount() throws AvroRemoteException {
        return getApplicationCount0();
    }

    @Override
    public int getTodayAppCount() throws AvroRemoteException {
        return getTodayAppCount0();
    }

    @Override
    public int getNeedExinfoAppCount() throws AvroRemoteException {
        return getNeedExinfoAppCount0();
    }

    @Override
    public boolean updateApk(ByteBuffer info) throws AvroRemoteException, ResponseError {
        Record apk = Record.fromByteBuffer(info);
        Schemas.checkRecordIncludeColumns(apk, "package", "version_code", "architecture");
        Schemas.checkRecordExcludeColumns(apk, "info_updated_time");
        if(apk.has("price"))
        {
        	String price = apk.getString("price");
        	apk.put("price", StringUtils.substringAfter(price, "US$"));
        }
        return updateApk0(apk);
    }
    
    protected abstract RecordSet getQapkOtherVersion0(String apkId,int minSDK);
    
    @Override
    public ByteBuffer getQapkOtherVersion(CharSequence apkId,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getQapkOtherVersion0(toStr(apkId),minSDK);
            generateUrlsAndDesc(recs);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract boolean saveQapkSuggest0(Record suggestRecord);
    
    @Override
    public boolean saveQapkSuggest(CharSequence sub_id, CharSequence sub_name, CharSequence policy, CharSequence hdpi_img_url, CharSequence mdpi_img_url) throws AvroRemoteException, ResponseError {
        try {
            Record r = new Record();
            r.put("hdpi",toStr(hdpi_img_url));
            r.put("mdpi",toStr(mdpi_img_url));
            if (existSuggestApk(toStr(sub_id), toStr(sub_name))) {
                return updateQapkSuggest0(toStr(sub_id), toStr(sub_name), toStr(policy),r.toString(false,false));
            } else {
                Record suggestRecord = Record.of("sub_id", sub_id, "sub_name", sub_name, "policy", policy);
                suggestRecord.put("img_url", r.toString(false,false));
                suggestRecord.put("created_time", DateUtils.nowMillis());
                suggestRecord.put("destroyed_time", 0);
                return saveQapkSuggest0(suggestRecord);
            }
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract boolean existSuggestApk0(String sub_id, String sub_name);
    
    @Override
    public boolean existSuggestApk(CharSequence sub_id, CharSequence sub_name) throws AvroRemoteException, ResponseError {
        try {
            return existSuggestApk0(toStr(sub_id), toStr(sub_name));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    
    protected abstract boolean deleteQapkSuggest0(String sub_id);
    
    @Override
    public boolean deleteQapkSuggest(CharSequence sub_id) throws AvroRemoteException, ResponseError {
        try {
            return deleteQapkSuggest0(toStr(sub_id));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    } 
    
    protected abstract boolean updateQapkSuggest0(String sub_id,String sub_name,String policy,String img_url);
    
    @Override
    public boolean updateQapkSuggest(CharSequence sub_id,CharSequence sub_name,CharSequence policy, CharSequence hdpi_img_url, CharSequence mdpi_img_url) throws AvroRemoteException, ResponseError {
        try {
            Record r = new Record();
            r.put("hdpi",toStr(hdpi_img_url));
            r.put("mdpi",toStr(mdpi_img_url));
            return updateQapkSuggest0(toStr(sub_id),toStr(sub_name),toStr(policy),r.toString(false,false));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    } 
    
    protected abstract RecordSet getSuggestApkfromSubId0(String sub_id,boolean ifsuggest);
    
    @Override
    public ByteBuffer getSuggestApkfromSubId(CharSequence sub_id,boolean ifsuggest) throws AvroRemoteException, ResponseError {
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
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract boolean updateQapkSuggest1(String sub_id);
    
    @Override
    public boolean updateQapkIfSuggest(CharSequence sub_id) throws AvroRemoteException, ResponseError {
        try {
            return updateQapkSuggest1(toStr(sub_id));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
        
    protected abstract RecordSet getApkIdsListByTimes0(long datediff, int action, int limit,int minSDK);
    protected abstract RecordSet getApkIdsListByDownloadTimes0(int limit,int minSDK);
    protected abstract RecordSet getApkIdsListByRating0(int limit,int minSDK);
    protected abstract RecordSet getApkIdsListByBorqs0(int limit,int minSDK);
    protected abstract RecordSet getApkIdsListByRandom0(int limit,int minSDK);
    
    @Override
    public ByteBuffer getApkIdsListByTimes(long datediff, int action, int limit,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getApkIdsListByTimes0(datediff,action,limit,minSDK);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    @Override
    public ByteBuffer getApkIdsListByDownloadTimes( int limit,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getApkIdsListByDownloadTimes0(limit,minSDK);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    @Override
    public ByteBuffer getApkIdsListByRating(int limit,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getApkIdsListByRating0(limit,minSDK);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    @Override
    public ByteBuffer getApkIdsListByBorqs(int limit,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getApkIdsListByBorqs0(limit,minSDK);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
        
    @Override
    public ByteBuffer getApkIdsListByRandom(int limit,int minSDK) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getApkIdsListByRandom0(limit,minSDK);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet getStrongMan0(String sub_category, int page, int count);
    
    @Override
    public ByteBuffer getStrongMan(CharSequence sub_category,int page,int count) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getStrongMan0(toStr(sub_category),page,count);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract boolean createUpdateApkLessDesc0(String packageName,int version_code,String app_name,String desc);
    
    @Override
    public boolean createUpdateApkLessDesc(CharSequence packageName,int version_code,CharSequence app_name,CharSequence desc) throws AvroRemoteException, ResponseError {
        try {
            return createUpdateApkLessDesc0(toStr(packageName),version_code,toStr(app_name),toStr(desc));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet getUpdateApkLessDesc0(int page,int count,String packageName,String app_name);
    
    @Override
    public ByteBuffer getUpdateApkLessDesc(int page,int count,CharSequence packageName,CharSequence app_name) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getUpdateApkLessDesc0(page,count,toStr(packageName),toStr(app_name));
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getUsersAppCount0(String userIds,String reason);

    @Override
    public ByteBuffer getUsersAppCount(CharSequence userIds,CharSequence reason) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getUsersAppCount0(toStr(userIds),toStr(reason));
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getTop1ApkByCategory0(String sub_category) ;

    @Override
    public ByteBuffer getTop1ApkByCategory(CharSequence sub_category) throws AvroRemoteException, ResponseError {
        try {
            RecordSet recs = getTop1ApkByCategory0(toStr(sub_category));
            generateUrlsAndDesc(recs);
            return recs.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet formatOldDataToConversation0(String viewerId);

    @Override
    public ByteBuffer formatOldDataToConversation(CharSequence viewerId) throws AvroRemoteException, ResponseError {
        try {
            return formatOldDataToConversation0(toStr(viewerId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateBorqsByPackage0(String package_, int borqs);
    
    @Override
    public boolean updateBorqsByPackage(CharSequence package_, int borqs) throws AvroRemoteException, ResponseError {
        try {
            return updateBorqsByPackage0(toStr(package_), borqs);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
