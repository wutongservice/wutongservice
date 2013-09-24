package com.borqs.server.qiupu;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface QiupuLogic {
    RecordSet getAllApps(Context ctx, String sub_category, boolean paid, String sort, String cols, int page, int count, int minSDK);

    RecordSet getApps(Context ctx, String packages, String cols, int minSDK);

    Record getSingleApp(Context ctx, String apkId, String cols, int minSDK);

    RecordSet getUserApps(Context ctx, String userId, int max, String action, String cols, int page, int count, String delApks, int minSDK);

    RecordSet getLinkedAppUsers(Context ctx, String viewerId, String app, String reason, String friendIds, int page, int count, int userType);

    RecordSet getSingleApps(Context ctx, String packagename, String cols, int minSDK);

    Record getUserLinkApps(Context ctx, String userId, String packagename);

    RecordSet getManualApkIds(Context ctx, String type, String maxcount, int minSDK);

    boolean setUserAppPrivancy(Context ctx, String userId, String app, String value);

    boolean setLinkUserApp(Context ctx, String userId, String apps, int version_code, int arch, String action, String deviceid, int minSDK);

    boolean existUserLinkedApp(Context ctx, String userId, String app, String reason);

    boolean existPackage(Context ctx, String packageName);

    int getReasonFromApp(Context ctx, String userId, String app);

    int getMaxVersionCode(Context ctx, String app, int minSDK);

    RecordSet searchApps(Context ctx, String value, String cols, int page, int count, int minSDK);

    boolean userSettingAll(Context ctx, String userId, String name, String value);

    boolean updateQappCount(Context ctx, String apkId, String column, long userId, String deviceId);

    boolean download(Context ctx, String userId, String app, String action, String deviceid);

    boolean deleteUserQapp(Context ctx, String userId, String app, String action, int minSDK);

    boolean uploadApk(Context ctx, String user, Record apkInfo, String deviceid);

    RecordSet syncApks(Context ctx, String user, String apkIds, boolean all, String deviceid, int minSDK);

    RecordSet findApkByPackageAndVersion(Context ctx, String packageName, String versionCode);

    RecordSet getLastInstalledApp(Context ctx, String user, String cols, int minSDK);

    String getUserSettingAll(Context ctx, String userId, String name);

    String getUserSingleAppPrivancy(Context ctx, String userId, String app);

    String getLastedDesc(Context ctx, String app);

    String getLastedScreenshotsUrls(Context ctx, String app);

    boolean manualApks(Context ctx, String apkIds, String types);

    RecordSet getManualApks(Context ctx, int type, int minSDK);

    RecordSet loadNeedExinfoApks(Context ctx, boolean isAll);

    int getApplicationCount(Context ctx);

    int getTodayAppCount(Context ctx);

    int getNeedExinfoAppCount(Context ctx);

    int getUserAppCount(Context ctx, String userId);

    boolean updateApk(Context ctx, Record info);

    RecordSet getQapkOtherVersion(Context ctx, String apkId, int minSDK);

    boolean saveQapkSuggest(Context ctx, String sub_id, String sub_name, String policy, String hdpi_img_url, String mdpi_img_url);

    boolean existSuggestApk(Context ctx, String sub_id, String sub_name);

    boolean deleteQapkSuggest(Context ctx, String sub_id);

    boolean updateQapkSuggest(Context ctx, String sub_id, String sub_name, String policy, String hdpi_img_url, String mdpi_img_url);

    boolean updateQapkIfSuggest(Context ctx, String sub_id);

    RecordSet getSuggestApkfromSubId(Context ctx, String sub_id, boolean ifsuggest);

    RecordSet getApkIdsListByTimes(Context ctx, long datediff, int action, int limit, int minSDK);

    RecordSet getApkIdsListByDownloadTimes(Context ctx, int limit, int minSDK);

    RecordSet getApkIdsListByRating(Context ctx, int limit, int minSDK);

    RecordSet getApkIdsListByBorqs(Context ctx, int limit, int minSDK);

    RecordSet getApkIdsListByRandom(Context ctx, int limit, int minSDK);

    RecordSet getUsersAppCount(Context ctx, String userIds, String reason);

    RecordSet getStrongMan(Context ctx, String sub_category, int page, int count);

    boolean createUpdateApkLessDesc(Context ctx, String packageName, int version_code, String app_name, String desc);

    RecordSet getUpdateApkLessDesc(Context ctx, int page, int count, String packageName, String app_name);

    RecordSet getTop1ApkByCategory(Context ctx, String sub_category);

    RecordSet formatOldDataToConversation(Context ctx, String viewerId);

    boolean updateBorqsByPackage(Context ctx, String package_, int borqs);

    RecordSet getApkByVersionCode(String packageName,int version_code);

    boolean updateReportUser(String packageName, int version_code, String report_user);
}