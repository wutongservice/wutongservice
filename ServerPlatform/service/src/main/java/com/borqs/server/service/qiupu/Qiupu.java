package com.borqs.server.service.qiupu;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordPredicates;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.RPCClient;
import com.borqs.server.base.rpc.TransceiverFactory;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.TextCollection;
import com.borqs.server.service.platform.*;
import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.Transceiver;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.borqs.server.service.platform.Constants.FRIENDS_CIRCLE;


public class Qiupu extends RPCClient {
   private static final Logger L = LoggerFactory.getLogger(Qiupu.class);
    public static final int REASON_INSTALLING = 1;
    public static final int REASON_INSTALLED = 1 << 1;
    public static final int REASON_UNINSTALLED = 1 << 2;
    public static final int REASON_FAVORITE = 1 << 3;
    public static final int REASON_DOWNLOADED = 1 << 4;
    public static final int REASON_UPLOADED = 1 << 5;

    public static final int ACTION_INSTALL = 1;
    public static final int ACTION_UNINSTALL = 2;
    public static final int ACTION_DOWNLOAD = 3;
    public static final int ACTION_UPLOAD = 4;
    public static final int ACTION_FAVORITE = 5;
    public static final int ACTION_UNFAVORITE = 6;

    public static final int ACTION_REMOVE_USER_APP = 9;

    public static final int SETTING_APP_PRIVACY = 1;
    public static final int SETTING_POST_INSTALL = 2;
    public static final int SETTING_POST_UPLOAD = 3;

    public static final int PRIVACY_PUBLIC = 1;
    public static final int PRIVACY_FRIEND = 2;
    public static final int PRIVACY_ME = 3;

    public static final int MANUAL_HOT = 1;
    public static final int MANUAL_SELECTED = 2;
    public static final int MANUAL_SUGGEST = 3;

    public static final int SOURCE_APP_CHINA = 0;
    public static final int SOURCE_GOOGLE_MARKET_PAGE = 1;
    public static final int SOURCE_MANUAL = 2;
    public static final int SOURCE_GFAN = 3;
    public static final int SOURCE_GOOGLE_MARKET_API = 4;

    public static final int APPS_MAX_COUNT = 1000;

    //serverUrl, parentpath
    public static final String serverUrl = "";
    public static final String parentpath = "";

    public static final TextCollection REASONS = TextCollection.of(
            "installing", REASON_INSTALLING,
            "installed", REASON_INSTALLED,
            "uninstalled", REASON_UNINSTALLED,
            "favorite", REASON_FAVORITE,
            "downloaded", REASON_DOWNLOADED,
            "uploaded", REASON_UPLOADED
    );

    public static final int ARCH_ARM = 1;
    public static final int ARCH_X86 = 2;
    public static final TextCollection ARCHS = TextCollection.of(
            "arm", ARCH_ARM,
            "x86", ARCH_X86
    );


    public static final String QAPK_COLUMNS =
            "package,app_name,version_code,version_name,architecture,target_sdk_version,category,sub_category,"
                    + "created_time,info_updated_time,description,recent_change,rating,"
                    + "download_count,install_count,uninstall_count,favorite_count,upload_user,screen_support,icon_url,price,borqs,"
                    + "market_url,file_size,file_url,tag,screenshots_urls";

    public static final String QAPK_BASE_COLUMNS =
            "package,app_name,version_code,version_name,min_sdk_version,target_sdk_version,max_sdk_version,architecture,"
                    + "created_time,destroyed_time,info_updated_time,description,recent_change,category,sub_category,rating,"
                    + "download_count,install_count,uninstall_count,favorite_count,upload_user,screen_support,icon_url,price,borqs,"
                    + "developer,developer_email,developer_phone,developer_website,market_url,other_urls,file_size,file_url,file_md5,"
                    + "tag,screenshots_urls";

    public static final String QAPK_FULL_COLUMNS = QAPK_COLUMNS
            + ",app_comment_count,app_comments,app_like_count,app_liked_users,app_installed_users,"
            + "app_likes,compatibility,app_used,app_favorite,lasted_version_code,lasted_version_name";

    public static final String QAPK_BASE_FULL_COLUMNS = QAPK_BASE_COLUMNS
            + ",app_comment_count,app_comments,app_like_count,app_liked_users,app_installed_users,"
            + "app_likes,compatibility,app_used,app_favorite,app_installing,app_installed,app_uninstalled,app_downloaded,lasted_version_code,lasted_version_name";

    public Qiupu(TransceiverFactory tf) {
        super(tf);
    }

    public RecordSet getAllApps(String viewerId, String sub_category, boolean paid, String sort, String cols, int page, int count, boolean history_version,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(qp.getAllApps(sub_category, paid, sort, removeExtenderColumn(cols), page, count,minSDK));
            return transDs(viewerId, recs, cols, history_version,minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getAllAppsFull(String viewerId, String sub_category, boolean paid, String sort, int page, int count, boolean history_version,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return transDs(viewerId, RecordSet.fromByteBuffer(qp.getAllApps(sub_category, paid, sort, removeExtenderColumn(QAPK_FULL_COLUMNS), page, count,minSDK)), QAPK_FULL_COLUMNS, history_version,minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getTop1ApkByCategory(String sub_category) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return RecordSet.fromByteBuffer(qp.getTop1ApkByCategory(sub_category));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getApps(String viewerId, String apps, String cols, boolean history_version, String ua,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            //maybe apps is package name,must transform to apkid
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            if (!checkQiupuFitVersion(viewerId, apps, ua, minSDK)) {
                List<String> l = StringUtils2.splitList(toStr(apps), ",", true);
                String returnString = "version is too old";
                for (String l0 : l) {
                    String[] ss = StringUtils.split(StringUtils.trimToEmpty(l0), '-');
                    if (ss[0].equals("com.borqs.qiupu")) {
                        int version_code = getMaxVersionCode(l0,minSDK);
                        returnString = RecordSet.fromByteBuffer(qp.getApps(ss[0].trim()+"-"+String.valueOf(version_code)+"-arm", removeExtenderColumn("version_name,file_size,recent_change"), minSDK)).getFirstRecord().toString(false, false);
                        break;
                    }
                }

                throw Errors.createResponseError(391, returnString);
            }
            apps = transformPackageNamesToApkids(apps,minSDK);


            RecordSet r = RecordSet.fromByteBuffer(qp.getApps(apps, removeExtenderColumn(cols),minSDK));
            return r.size() > 0 ? transDs(viewerId, r, cols, history_version,minSDK) : new RecordSet();
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getAppsFull(String viewerId, String apps, boolean history_version, String ua,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            if (!checkQiupuFitVersion(viewerId, apps, ua, minSDK)) {
                List<String> l = StringUtils2.splitList(toStr(apps), ",", true);
                String returnString = "version is too old";
                for (String l0 : l) {
                    String[] ss = StringUtils.split(StringUtils.trimToEmpty(l0), '-');
                    if (ss[0].equals("com.borqs.qiupu")) {
                        int version_code = getMaxVersionCode(l0,minSDK);
                        returnString = RecordSet.fromByteBuffer(qp.getApps(ss[0].trim()+"-"+String.valueOf(version_code)+"-arm", removeExtenderColumn("version_name,file_size,recent_change"), minSDK)).getFirstRecord().toString(false, false);
                        break;
                    }
                }

                throw Errors.createResponseError(391, returnString);
            }
            apps = transformPackageNamesToApkids(apps,minSDK);

            RecordSet r = RecordSet.fromByteBuffer(qp.getApps(apps, removeExtenderColumn(QAPK_FULL_COLUMNS),minSDK));
            return r.size() > 0 ? transDs(viewerId, r, QAPK_FULL_COLUMNS, history_version,minSDK) : new RecordSet();
        } finally {
            closeTransceiver(trans);
        }
    }

    public String parseUserIds(String viewerId, String userIds) throws AvroRemoteException {
        StringBuilder buff = new StringBuilder();
        Transceiver tranf = getTransceiver(Friendship.class);
        Friendship f = getProxy(Friendship.class, tranf);
        for (String userId : StringUtils2.splitList(userIds, ",", true)) {
            if (userId.startsWith("#")) {
                if (!viewerId.equals("0") && !viewerId.equals("")) {
                    String circleId = StringUtils.removeStart(userId, "#");
                    RecordSet friendRecs = RecordSet.fromByteBuffer(f.getFriends(viewerId, circleId, 0, -1));
                    buff.append(friendRecs.joinColumnValues("friend", ","));
                }
            } else {
                buff.append(userId);
            }
            buff.append(",");
        }
        return StringUtils2.stripItems(buff.toString(), ",", true);
    }

    public RecordSet getAppsSharedToMe(String viewerId, String cols, String userIds, boolean tome, String getType, boolean friend, int page, int count, boolean history_version,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        Transceiver transs = getTransceiver(Stream.class);
        Transceiver transa = getTransceiver(Account.class);
        Transceiver transc = getTransceiver(Comment.class);
        Transceiver transl = getTransceiver(Like.class);
        Transceiver transf = getTransceiver(Friendship.class);
        try {
            Stream s = getProxy(Stream.class, transs);
            Account a = getProxy(Account.class, transa);
            Comment c = getProxy(Comment.class, transc);
            Like l = getProxy(Like.class, transl);
            Friendship f = getProxy(Friendship.class, transf);

            if (cols.equals(""))
                cols = QAPK_FULL_COLUMNS;

            if (friend) {
                if (userIds.length() > 0) {
                    userIds = parseUserIds(viewerId, userIds);
                } else {
                    userIds = RecordSet.fromByteBuffer(f.getFriends(viewerId, "", 0, 1000)).joinColumnValues("", ",");
                }
            } else {
                if (userIds.length() > 0) {
                    userIds = parseUserIds(viewerId, userIds);
                }
            }

            RecordSet r = new RecordSet();
            if (getType.equals("comment")) {
                r = RecordSet.fromByteBuffer(c.getObjectCommentByUsers(viewerId, userIds, "4", page, count));
            } else if (getType.equals("like")) {
                r = RecordSet.fromByteBuffer(l.getObjectLikedByUsers(viewerId, userIds, "4", page, count));
            } else {
                r = RecordSet.fromByteBuffer(s.getApkSharedToMe(viewerId, userIds, tome, "", page, count));
            }

            Map map = new HashMap();
            for (Record r0 : r) {
                String source = r0.getString("source");
                String target = r0.getString("target");
                if (map.containsKey(target)) {
                    map.put(target, map.get(target).toString() + "," + source);
                } else {
                    map.put(target, source);
                }
            }

            String apps = r.unique("target").joinColumnValues("target", ",");

            QiupuInterface qp = getProxy(QiupuInterface.class, trans);

            RecordSet outr = RecordSet.fromByteBuffer(qp.getApps(apps, removeExtenderColumn(cols),minSDK));
            RecordSet out_recs = outr.size() > 0 ? transDs(viewerId, outr, cols, history_version,minSDK) : new RecordSet();
            for (Record r1 : out_recs) {
                if (map.get(r1.getString("apk_id")) != null) {
                    String users = map.get(r1.getString("apk_id")).toString();
                    RecordSet u = RecordSet.fromByteBuffer(a.getUsers(users, "user_id, display_name, image_url"));
                    r1.put("whoshared", u);
                } else {
                    r1.put("whoshared", "[]");
                }

                //tome
                String[] ss = StringUtils.split(StringUtils.trimToEmpty(r1.getString("apk_id")), '-');
                String package_ = ss[0].trim();
                RecordSet recs_tome = RecordSet.fromByteBuffer(s.getApkSharedToMe(viewerId, userIds, true, package_, 0, 1));
                r1.put("tome", recs_tome.size() > 0 ? true : false);

                RecordSet recs_to_count = RecordSet.fromByteBuffer(s.getApkSharedToMe(viewerId, userIds, false, package_, 0, 1000));
                r1.put("share_count", recs_to_count.size());
            }
            return out_recs;
        } finally {
            closeTransceiver(trans);
            closeTransceiver(transs);
            closeTransceiver(transa);
        }
    }

    protected String transformPackageNamesToApkids(String apps,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            List<String> l = StringUtils2.splitList(toStr(apps), ",", true);
            List<String> m = new ArrayList<String>();

            for (String app : l) {
                List<String> n = StringUtils2.splitList(toStr(app), "-", true);
                String ss[] = StringUtils2.splitArray(app, "-", true);
                if (n.size() > 2) {
                    //is APKID
                    RecordSet r = RecordSet.fromByteBuffer(qp.findApkByPackageAndVersion(ss[0], ss[1]));
                    if (r.size() > 0) {
                        //in db
                        m.add(app);
                    } else {
                        //not in db
                        int maxV = getMaxVersionCode(ss[0],minSDK);
                        if (maxV == 0) {
                            m.add(app);
                        } else {
                            m.add(ss[0] + "-" + getMaxVersionCode(ss[0],minSDK) + "-" + "arm");
                        }
                    }
                } else {
                    m.add(ss[0] + "-" + getMaxVersionCode(ss[0],minSDK) + "-" + "arm");
                }
            }
            return StringUtils.join(m, ",");
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getAppsOtherVersion(String apkId,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            RecordSet r = RecordSet.fromByteBuffer(qp.getQapkOtherVersion(apkId,minSDK));
            return r.size() > 0 ? r : new RecordSet();
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean saveQapkSuggest(String sub_id, String sub_name, int manual_count,
                                   int week_download_count, int month_download_count, int year_download_count, int sum_download_count,
                                   int rating_count, int borqs_count, int random_count, String hdpi_img_url, String mdpi_img_url) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);

            Record r = new Record();
            r.put("manual_count", manual_count);
            r.put("week_download_count", week_download_count);
            r.put("month_download_count", month_download_count);
            r.put("year_download_count", year_download_count);
            r.put("sum_download_count", sum_download_count);
            r.put("rating_count", rating_count);
            r.put("borqs_count", borqs_count);
            r.put("random_count", random_count);

            return qp.saveQapkSuggest(sub_id, sub_name, r.toString(false, false), hdpi_img_url, hdpi_img_url);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean deleteQapkSuggest(String sub_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return qp.deleteQapkSuggest(sub_id);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateQapkIfSuggest(String sub_id) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return qp.updateQapkIfSuggest(sub_id);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getAllQapkSuggestType(int type, boolean ifsuggest) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return RecordSet.fromByteBuffer(qp.getSuggestApkfromSubId(String.valueOf(type), ifsuggest));
        } finally {
            closeTransceiver(trans);
        }
    }


    public RecordSet getPrefecturApps(String viewerId, String type, String cols, int page, int count, boolean history_version,String ua,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            RecordSet rsOut = new RecordSet();

            //get policy from type
            Record rec = RecordSet.fromByteBuffer(qp.getSuggestApkfromSubId(type, false)).getFirstRecord();

            String policy = rec.getString("policy");
            if (policy.length() > 0) {
                Record p_rec = Record.fromJson(policy);

                //NO.1： manual
                int count1 = (int) p_rec.getInt("manual_count");
                if (count1 > 0) {
                    RecordSet recs0 = RecordSet.fromByteBuffer(qp.getManualApkIds(type, String.valueOf(count1),minSDK));
                    rsOut.addAll(recs0);
                }

                //NO.2： download ranking list in week
                int count2 = (int) p_rec.getInt("week_download_count");
                if (count2 > 0) {
                    long datediff = 60 * 60 * 24 * 7 * 1000;
                    RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByTimes(datediff, ACTION_DOWNLOAD, count2,minSDK));
                    if (recs0.size() > 0) {
                        rsOut.addAll(recs0);
                    }
                }

                //NO.3： download ranking list in month
                int count3 = (int) p_rec.getInt("month_download_count");
                if (count3 > 0) {
                    long datediff = 60 * 60 * 24 * 30 * 1000;
                    RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByTimes(datediff, ACTION_DOWNLOAD, count3,minSDK));
                    if (recs0.size() > 0) {
                        rsOut.addAll(recs0);
                    }
                }

                //NO.4： download ranking list in year
                int count4 = (int) p_rec.getInt("year_download_count");
                if (count4 > 0) {
                    long datediff = 60 * 60 * 24 * 365 * 1000;
                    RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByTimes(datediff, ACTION_DOWNLOAD, count4,minSDK));
                    if (recs0.size() > 0) {
                        rsOut.addAll(recs0);
                    }
                }

                //NO.5： download ranking list in db
                int count5 = (int) p_rec.getInt("sum_download_count");
                if (count5 > 0) {
                    RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByDownloadTimes(count5,minSDK));
                    if (recs0.size() > 0) {
                        rsOut.addAll(recs0);
                    }
                }

                //NO.6： rating ranking list
                int count6 = (int) p_rec.getInt("rating_count");
                if (count6 > 0) {
                    RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByRating(count6,minSDK));
                    if (recs0.size() > 0) {
                        rsOut.addAll(recs0);
                    }
                }

                //NO.7：force recommended borqs app
                int count7 = (int) p_rec.getInt("borqs_count");
                if (count7 > 0) {
                    RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByBorqs(count7,minSDK));
                    rsOut.addAll(recs0);
                }

                //NO.8：random apps from lasted 1000
                int count8 = (int) p_rec.getInt("random_count");
                if (count8 > 0) {
                    RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByRandom(count8,minSDK));
                    if (recs0.size() > 0) {
                        rsOut.addAll(recs0);
                    }
                }
            }

            //delete repeat
            rsOut.unique("apk_id");

            //for page
            rsOut.sliceByPage(page, count);

            //get apk info
            RecordSet recs = getApps(viewerId, rsOut.joinColumnValues("apk_id", ","), cols.equals("") ? QAPK_BASE_FULL_COLUMNS : cols, history_version, ua,minSDK);
            return recs;
        } finally {
            closeTransceiver(trans);
        }
    }

    //get strong man
    public RecordSet getStrongMen(String sub_category, int page, int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);

            Transceiver transa = getTransceiver(Account.class);
            Account ac = getProxy(Account.class, transa);

            RecordSet r = RecordSet.fromByteBuffer(qp.getStrongMan(sub_category, page, count));
            RecordSet rs = new RecordSet();

            for (Record t : r) {
                Record p = new Record();
                Record t0 = RecordSet.fromByteBuffer(ac.getUsers(toStr(t.getInt("user")), "user_id, display_name, image_url")).getFirstRecord();

                p.put("user_id", t0.getString("user_id"));
                p.put("display_name", t0.getString("display_name"));
                p.put("image_url", t0.getString("image_url"));

                p.put("app_count", t.getString("COUNT1"));
                rs.add(p);
            }
            return rs;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getApp(String viewerId, String apkId, String cols, boolean history_version,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            Record r = Record.fromByteBuffer(qp.getSingleApp(apkId, removeExtenderColumn(cols),minSDK));
            RecordSet rs = new RecordSet();
            rs.add(r);
            return transDs(viewerId, rs, cols, history_version,minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public Record getSingleApp(String apkId, String cols,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            Record r = Record.fromByteBuffer(qp.getSingleApp(apkId, removeExtenderColumn(cols),minSDK));
            return r;
        } finally {
            closeTransceiver(trans);
        }
    }

    public int getMaxVersionCode(String app,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return qp.getMaxVersionCode(app,minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean checkQiupuFitVersion(String viewerId, String apkIds, String ua,int minSDK) throws AvroRemoteException {
        //must qiupu
        //versioncode < 180,return false,else return true
        //versioncode :110
        //user id must :180
        Transceiver trans = getTransceiver(Application.class);
        try {

            Application app = getProxy(Application.class, trans);
            List<String> l = StringUtils2.splitList(toStr(apkIds), ",", true);
            int qiupuVersion = 0;
            for (String ll : l) {
                String[] ss = StringUtils.split(StringUtils.trimToEmpty(ll), '-');
                if (ss[0].equals("com.borqs.qiupu")) {
                    if (ss.length > 2) {
                        qiupuVersion = Integer.valueOf(ss[1]);
                        break;
                    } else {
                        if (ua.length() > 0) {
                            String[] ua0 = StringUtils.split(StringUtils.trimToEmpty(ua), ';');
                            if (ua0.length > 2) {
                                String v = ua0[1].toString();
                                String v1 = v;
                                String v0 = v.replaceAll("[0-9]*", "");
                                qiupuVersion = Integer.valueOf(v1.replace(v0, ""));
                                break;
                            } else {
                                qiupuVersion = getMaxVersionCode(ll,minSDK);
                                break;
                            }
                        } else {
                            qiupuVersion = getMaxVersionCode(ll,minSDK);
                            break;
                        }
                    }
                }
            }
            return qiupuVersion == 0 ? true : (qiupuVersion >= Integer.valueOf(app.findQiupuMinVersion().toString()));

        } finally {
            closeTransceiver(trans);
        }
    }


    public RecordSet getAppFull(String viewerId, String apkId, boolean history_version,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return transDs(viewerId, RecordSet.fromByteBuffer(qp.getSingleApp(apkId, removeExtenderColumn(QAPK_FULL_COLUMNS),minSDK)), QAPK_FULL_COLUMNS, history_version,minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    private String removeExtenderColumn(String cols) {
        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        l.remove("app_comment_count");
        l.remove("app_comments");
        l.remove("app_like_count");
        l.remove("app_liked_users");
        l.remove("app_likes");
        l.remove("compatibility");
        l.remove("app_used");
        l.remove("app_favorite");
        l.remove("app_installing");
        l.remove("app_installed");
        l.remove("app_uninstalled");
        l.remove("app_downloaded");
        l.remove("lasted_version_code");
        l.remove("lasted_version_name");
        l.remove("app_installed_users");
        String newCols = StringUtils.join(l, ",").toString();
        return newCols;
    }

    //add qiupu other columns
    private RecordSet transDs(String viewerId, RecordSet ds, String cols, boolean history_version,int minSDK) throws AvroRemoteException {
        Transceiver transc = getTransceiver(Comment.class);
        Comment c = getProxy(Comment.class, transc);

        Transceiver transa = getTransceiver(Account.class);
        Account ac = getProxy(Account.class, transa);

        Transceiver transl = getTransceiver(Like.class);
        Like lk = getProxy(Like.class, transl);

        Transceiver trans = getTransceiver(QiupuInterface.class);
        QiupuInterface qp = getProxy(QiupuInterface.class, trans);

        List<String> l = StringUtils2.splitList(toStr(cols), ",", true);
        if (ds.size() > 0) {
            for (Record rec : ds) {
                //parse apkid
                String apk_id = rec.getString("apk_id");
                if (apk_id.length() > 0) {
                    String[] ss = StringUtils.split(StringUtils.trimToEmpty(apk_id), '-');
                    String package_ = ss[0].trim();
                    String versionCode_ = ss[1].trim();
                    String arch_ = ss[2].trim();
                    //if (!arch_.equals("")) {
                    //    arch_ = ARCHS.getText(Integer.valueOf(arch_));
                    //}
                    rec.put("apk_id", package_ + "-" + versionCode_ + "-" + arch_);
                    String targetObjectId = Constants.objectId(Constants.APK_OBJECT, rec.getString("apk_id"));

                    if (l.contains("upload_user")) {
                        if (rec.getInt("upload_user") > 0) {
                            rec.put("upload_user", RecordSet.fromByteBuffer(ac.getUsers(toStr(rec.getInt("upload_user")), "user_id, display_name, image_url, address")).getFirstRecord());
                        } else {
                            rec.put("upload_user", new RecordSet());
                        }
                    }

                    if (l.contains("app_comment_count")) {
                        rec.put("app_comment_count", c.getCommentCount(viewerId,targetObjectId));
                    }
                    if (l.contains("app_comments")) {
                        String ccols = "comment_id, target, created_time,commenter, commenter_name, message, device, can_like,destroyed_time";
                        RecordSet ds1 = RecordSet.fromByteBuffer(c.getCommentsForContainsIgnore(viewerId,targetObjectId, ccols, false, 0, 2));
                        for (Record r : ds1){
                            r.put("iliked", viewerId.equals("") ? false : lk.ifUserLiked(viewerId, Constants.COMMENT_OBJECT + ":" + r.getString("comment_id")));
                        }
                        rec.put("app_comments", ds1);
                    }
                    if (l.contains("app_like_count")) {
                        rec.put("app_like_count", lk.getLikeCount(targetObjectId));
                    }
                    if (l.contains("app_liked_users")) {
                        RecordSet uids = RecordSet.fromByteBuffer(lk.loadLikedUsers(targetObjectId, 0, 5));
                        if (uids.size() > 0) {
                            String uids0 = uids.joinColumnValues("liker", ",");
                            RecordSet recs_users = RecordSet.fromByteBuffer(ac.getUsers(uids0, "user_id, display_name, image_url"));
                            rec.put("app_liked_users", recs_users);
                        } else {
                            rec.put("app_liked_users", new RecordSet());
                        }
                    }

                    if (l.contains("app_installed_users")) {
                        RecordSet installed_users = getUsedAppUsers(viewerId,package_,"installed","user_id, display_name, image_url",0,4);
                        if (installed_users.size() > 0) {
                            rec.put("app_installed_users", installed_users);
                        } else {
                            rec.put("app_installed_users", new RecordSet());
                        }
                    }
                    if (l.contains("app_likes")) {
                        String targetId = Constants.objectId(Constants.APK_OBJECT, apk_id);
                        rec.put("app_likes", viewerId.equals("") ? false : lk.ifUserLiked(viewerId, targetId));
                    }
                    if (l.contains("compatibility")) {
                        rec.put("compatibility", true);
                    }

                    if (l.contains("app_used")) {
                        rec.put("app_used", viewerId.equals("") ? false : qp.existUserLinkedApp(viewerId, rec.getString("package"), ""));
                    }

                    if (l.contains("app_favorite") || l.contains("app_installing") || l.contains("app_installed") || l.contains("app_uninstalled") || l.contains("app_downloaded")) {
                        if (viewerId.equals("")) {
                            rec.put("app_favorite", false);
                            rec.put("app_installing", false);
                            rec.put("app_installed", false);
                            rec.put("app_uninstalled", false);
                            rec.put("app_downloaded", false);
                        } else {
                            int reason = qp.getReasonFromApp(viewerId, rec.getString("package"));
                            rec.put("app_favorite", (reason & REASON_FAVORITE) != 0);
                            rec.put("app_installing", (reason & REASON_INSTALLING) != 0);
                            rec.put("app_installed", (reason & REASON_INSTALLED) != 0);
                            rec.put("app_uninstalled", (reason & REASON_UNINSTALLED) != 0);
                            rec.put("app_downloaded", (reason & REASON_DOWNLOADED) != 0);
                        }
                    }

                    int maxId = getMaxVersionCode(package_,minSDK);
                    String apkidTemp = package_ + "-" + String.valueOf(maxId) + "-" + arch_;

                    if (l.contains("lasted_version_code")) {
                        rec.put("lasted_version_code", String.valueOf(maxId));
                    }

                    Record rectemp = getSingleApp(apkidTemp, "version_name",minSDK);
                    if (l.contains("lasted_version_name")) {
                        rec.put("lasted_version_name", rectemp.getString("version_name"));
                    }

                    //INSERT PRIVACY
                    if (!viewerId.equals("")) {
                        String setting_value = getUserSingleAppPrivancy(viewerId, package_);
                        if (setting_value.equals("0"))
                            setting_value = "1";
                        rec.put("visibility", setting_value);
                    } else {
                        rec.put("visibility", "1");
                    }

                    if (history_version) {
                        RecordSet o = getAppsOtherVersion(apk_id,minSDK);
                        RecordSet o100 = new RecordSet();
                        if (o.size() > 0) {
                            for (Record o0 : o) {
                                o100.add(Record.of("apk_id", o0.getString("package") + "-" + o0.getString("version_code") + "-" + ARCHS.getText(Integer.valueOf(o0.getString("architecture"))), "version_name", o0.getString("version_name"), "file_url", o0.getString("file_url")));
                            }
                            rec.put("otherVersion", o100);
                        }
                    } else {
                        if (rec.has("description")) {
                            String desc = rec.getString("description");
                            if (desc.length() > 100)
                                rec.put("description", desc.substring(0, 100));
                        }
                    }

                }
            }
        }
        return ds;
    }

    public RecordSet getUserApps(String viewerId, String userId, String action, String cols, int page, int count, boolean history_version, String delApks,int minSDK) throws AvroRemoteException {
        return getUserAppsFor(viewerId, userId, action, cols, page, count, history_version, delApks,minSDK);
    }

    public RecordSet getUserAppsFull(String viewerId, String userId, String action, int page, int count, boolean history_version, String delApks,int minSDK) throws AvroRemoteException {
        return getUserAppsFor(viewerId, userId, action, "", page, count, history_version, delApks,minSDK);
    }

    public RecordSet getUserAppsFor(String viewerId, String userId, String action, String cols, int page, int count, boolean history_version, String delApks,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        Transceiver transfs = getTransceiver(Friendship.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            Friendship fs = getProxy(Friendship.class, transfs);
            int max = PRIVACY_PUBLIC;
            if (!viewerId.equals("")) {
                RecordSet recsfs = RecordSet.fromByteBuffer(fs.getRelation(viewerId, userId, String.valueOf(Constants.FRIENDS_CIRCLE)));
                max = viewerId.equals(userId) ? PRIVACY_ME : (recsfs.size() > 0 ? PRIVACY_FRIEND : PRIVACY_PUBLIC);
            }
            if (cols.equals("")) {
                return transDs(viewerId, RecordSet.fromByteBuffer(qp.getUserApps(userId, max, action, removeExtenderColumn(QAPK_FULL_COLUMNS), page, count, delApks,minSDK)), QAPK_FULL_COLUMNS, history_version,minSDK);
            } else {
                return transDs(viewerId, RecordSet.fromByteBuffer(qp.getUserApps(userId, max, action, removeExtenderColumn(cols), page, count, delApks,minSDK)), cols, history_version,minSDK);
            }
        } finally {
            closeTransceiver(trans);
        }
    }


    public RecordSet getUsedAppUsers(String viewerId, String app, String reason, String cols, int page,int count) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        Transceiver transa = getTransceiver(Account.class);
        Transceiver transf = getTransceiver(Friendship.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            Account a = getProxy(Account.class, transa);
            Friendship f = getProxy(Friendship.class, transf);
            RecordSet recsfs = new RecordSet();
            if (viewerId.equals("")) {
                recsfs = RecordSet.fromByteBuffer(qp.getLinkedAppUsers(viewerId, app, toStr(REASONS.getValue("reason", REASON_INSTALLED)),"", page, count, 0));
            } else {
                //get myfriend
                RecordSet recs_friend = RecordSet.fromByteBuffer(f.getFriends(viewerId,String.valueOf(Constants.FRIENDS_CIRCLE),0,1000));
                String friendsIds = recs_friend.joinColumnValues("friend",",");
                recsfs = RecordSet.fromByteBuffer(qp.getLinkedAppUsers(viewerId, app, toStr(REASONS.getValue("reason", REASON_INSTALLED)),friendsIds, page, count, 1));
                if (recsfs.size() < count) {
                    RecordSet recs_t = RecordSet.fromByteBuffer(qp.getLinkedAppUsers(viewerId, app, toStr(REASONS.getValue("reason", REASON_INSTALLED)),friendsIds, 0, count-recsfs.size(), 2));
                    if (recs_t.size()>0){
                        recsfs.addAll(recs_t);
                    }
                }
            }
            RecordSet recsfs_0 = new RecordSet();
            for (Record r : recsfs) {
                if (viewerId.equals("")) {
                    recsfs_0.add(r);
                } else {
                    String privacy = getUserSingleAppPrivancy(r.getString("user"), toStr(app)).toString();
                    if (privacy.equals("1")) {
                        recsfs_0.add(r);
                    }
                    if (privacy.equals("2")) {    //friend can see
                        RecordSet recordSet = RecordSet.fromByteBuffer(f.getRelation(viewerId, r.getString("user"), String.valueOf(FRIENDS_CIRCLE)));
                        if (recordSet.size() > 0)
                            recsfs_0.add(r);
                    }
                    if (privacy.equals("3")) {    //me can see
                        if (viewerId.equals(r.getString("user")))
                            recsfs_0.add(r);
                    }
                }
            }

            if (recsfs_0.size() > 0) {
                RecordSet u = RecordSet.fromByteBuffer(a.getUsers(recsfs_0.joinColumnValues("user", ","), cols));
                return u;
            } else {
                return new RecordSet();
            }
        } finally {
            closeTransceiver(trans);
            closeTransceiver(transa);
        }
    }

    public boolean setLinkUserApp(String userId, String app, int version_code, int arch, String action, String deviceid,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return qp.setLinkUserApp(userId, app, version_code, arch, toStr(action), toStr(deviceid),minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean removeUserLinkApp(String userId, String apps,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            boolean b = qp.deleteUserQapp(toStr(userId), toStr(apps), toStr(ACTION_REMOVE_USER_APP),minSDK);
            return b;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet searchApps(String viewerId, String value, String cols, int page, int count, boolean history_version,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return transDs(viewerId, RecordSet.fromByteBuffer(qp.searchApps(value, removeExtenderColumn(cols), page, count,minSDK)), cols, history_version,minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet searchAppsFull(String viewerId, String value, int page, int count, boolean history_version,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return transDs(viewerId, RecordSet.fromByteBuffer(qp.searchApps(value, removeExtenderColumn(QAPK_FULL_COLUMNS), page, count,minSDK)), QAPK_FULL_COLUMNS, history_version,minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean setAppfavorite(String userId, String app, String deviceid,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            String inApkId = app;
            String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(inApkId), "-", true);
            if (a1.length < 3) {
                inApkId = app + "-" + getMaxVersionCode(app,minSDK) + "-arm";
            }
            String[] a = StringUtils2.splitArray(StringUtils.trimToEmpty(inApkId), "-", true);

            Record r = Record.fromByteBuffer(qp.getUserLinkApps(userId, a[0]));

            int nowReason = (int) r.getInt("reason", 0);
            int action;
            if (nowReason == 0) {
                action = ACTION_FAVORITE;
            } else {
                boolean isFavorite = ((nowReason & Qiupu.REASON_FAVORITE) == Qiupu.REASON_FAVORITE);
                action = isFavorite ? ACTION_UNFAVORITE : ACTION_FAVORITE;
            }

            if (!StringUtils.isEmpty(a[0])) {
                Transceiver tranc = getTransceiver(Conversation.class);
                Conversation c = getProxy(Conversation.class, tranc);
                if (action == ACTION_FAVORITE) {
                    Record r0 = new Record();
                    r0.put("target_type", Constants.APK_OBJECT);
                    r0.put("target_id", a[0]);
                    r0.put("reason", Constants.C_APK_FAVORITE);
                    r0.put("from_", userId);
                    c.createConversation(r0.toByteBuffer());
                } else if (action == ACTION_UNFAVORITE) {
                    c.deleteConversation(Constants.APK_OBJECT,a[0],Constants.C_APK_FAVORITE,Long.parseLong(userId));
                }
            }
            return qp.setLinkUserApp(userId, a[0], Integer.parseInt(a[1]), ARCHS.getValue(a[2], 1), toStr(action), toStr(deviceid),minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean userSettingAll(String userId, String value) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return qp.userSettingAll(userId, toStr(SETTING_APP_PRIVACY), value);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean setUserSingleAppPrivancy(String userId, String app, String value) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return qp.setUserAppPrivancy(userId, toStr(app), value);
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getUserSettingAll(String userId) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return qp.getUserSettingAll(userId, toStr(SETTING_APP_PRIVACY)).toString();
        } finally {
            closeTransceiver(trans);
        }
    }

    public String getUserSingleAppPrivancy(String userId, String app) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            return qp.getUserSingleAppPrivancy(userId, toStr(app)).toString();
        } finally {
            closeTransceiver(trans);
        }
    }

    public String download(String userId, String apkId, String deviceid,int minSDK) throws AvroRemoteException, IOException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, trans);
            String[] a = StringUtils2.splitArray(StringUtils.trimToEmpty(apkId), "-", true);
            String inApkId = apkId;
            if (a.length < 3) {
                inApkId = apkId + "-" + getMaxVersionCode(apkId,minSDK) + "-arm";
            }

            qp.download(userId, inApkId, toStr(ACTION_DOWNLOAD), toStr(deviceid));
            //if this user has downloaded then 
            return "download success!";
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean uploadApk(String user, Record apkInfo, String deviceid) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            if (!StringUtils.isEmpty(user)) {
                Transceiver tranc = getTransceiver(Conversation.class);
                Conversation c = getProxy(Conversation.class, tranc);
                Record r0 = new Record();
                r0.put("target_type", Constants.APK_OBJECT);
                r0.put("target_id", apkInfo.getString("package"));
                r0.put("reason", Constants.C_APK_UPLOAD);
                r0.put("from_", user);
                c.createConversation(r0.toByteBuffer());
            }
            return qiupuInterface.uploadApk(user, apkInfo.toByteBuffer(), toStr(deviceid));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet syncApks(String user, String apkIds, boolean all, String deviceid,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return RecordSet.fromByteBuffer(qiupuInterface.syncApks(user, apkIds, all, deviceid,minSDK));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getLastInstalledApp(String user,boolean  history_version,int minSDK) throws AvroRemoteException {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans) ;
            return transDs(user,RecordSet.fromByteBuffer(qiupuInterface.getLastInstalledApp(user,removeExtenderColumn(QAPK_FULL_COLUMNS),minSDK)), QAPK_FULL_COLUMNS,history_version,minSDK);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean manualApks(String apkIds, String types) throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return qiupuInterface.manualApks(apkIds, types);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getManualApks(int type,int minSDK) throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return RecordSet.fromByteBuffer(qiupuInterface.getManualApks(type,minSDK));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet suggestApks0(String user, String cols, int page, int count,int minSDK) throws Exception {
        RecordSet suggestedApps = new RecordSet();
        //Part 1 :manual
        suggestedApps.addAll(getManualApks(Qiupu.MANUAL_SUGGEST,minSDK));

        if (StringUtils.isNotBlank(user)) {
            RecordSet viewerApps = getUserApps(user, user, "", removeExtenderColumn(cols), 0, 1000, false, "",minSDK);
            viewerApps = transDs(user, viewerApps, cols, false,minSDK);

            //delete user's link apps
            RecordSet removedApps = new RecordSet();
            for (Record rec : suggestedApps) {
                if (viewerApps.contains(rec)) {
                    removedApps.add(rec);
                }
            }
            suggestedApps.removeAll(removedApps);
        }

        suggestedApps.sliceByPage(page, count);
        return suggestedApps;
    }

    public RecordSet suggestApks1(String user, String cols, int page, int count,int minSDK) throws Exception {
        Transceiver trans = getTransceiver(Friendship.class);
        RecordSet suggestedApps = new RecordSet();
        try {
            Friendship fs = getProxy(Friendship.class, trans);
            if (StringUtils.isNotBlank(user)) {
                RecordSet viewerApps = getUserApps(user, user, "", removeExtenderColumn(cols), 0, 1000, false, "",minSDK);
                viewerApps = transDs(user, viewerApps, cols, false,minSDK);

                //Part 2 : get apps from friend
                RecordSet friendApps = new RecordSet();
                RecordSet friends = RecordSet.fromByteBuffer(fs.getFriends(user, String.valueOf(Constants.FRIENDS_CIRCLE), 0, 1000));
                for (Record rec : friends) {
                    String friend = rec.getString("friend");
                    friendApps.addAll(transDs(user, getUserApps(user, friend, "", removeExtenderColumn(cols), 0, 1000, false, "",minSDK), cols, false,minSDK));
                }

                for (Record rec : friendApps) {
                    String packageName = rec.getString("package");
                    friendApps.foreachIf(RecordPredicates.valueEquals("package", packageName),
                            new RecordHandler() {
                                @Override
                                public void handle(Record rec) {
                                    long fcount = rec.getInt("fcount", 0);
                                    rec.put("fcount", fcount + 1);
                                }
                            });
                }

                friendApps.unique("package");
                friendApps.sort("fcount", false);
                friendApps.removeColumns("fcount");

                for (Record rec : friendApps) {
                    if (!viewerApps.contains(rec)) {
                        suggestedApps.add(rec);
                    }
                }
            }

            suggestedApps.sliceByPage(page, count);
            return suggestedApps;
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet suggestApks2(String user, String category, String cols, int page, int count) throws Exception {
        RecordSet suggestedApps = new RecordSet();

        //TODO: Part 3 :：get apps from rating desc

        suggestedApps.sliceByPage(page, count);
        return suggestedApps;
    }

    public RecordSet suggestApks(String user, String category, String cols, int page, int count, String ua, int minSDK) throws Exception {
        Transceiver transqp = getTransceiver(QiupuInterface.class);
        RecordSet suggestedApps = new RecordSet();
        try {
            QiupuInterface qp = getProxy(QiupuInterface.class, transqp);
            if (StringUtils.isNotBlank(user)) {


                RecordSet userInstalledApps = RecordSet.fromByteBuffer(qp.getUserApps(user, 1, "1", "package,version_code,version_name,sub_category", 0, 1000, "", minSDK));
                List<String> packageList = new ArrayList<String>();

                List<Integer> ll = new ArrayList<Integer>();
                for (Record r : userInstalledApps) {
                    int sub_category = (int) r.getInt("sub_category");
                    if (!ll.contains(sub_category) && sub_category != 0)
                        ll.add(sub_category);
                }

                if (ll.size() > 0) {
                    Map map = new HashMap();
                    for (int l : ll) {
                        int lcount = 0;
                        for (Record r : userInstalledApps) {
                            int sub_category = (int) r.getInt("sub_category");
                            if (sub_category == l)
                                lcount += 1;
                        }
                        map.put(l, lcount);
                    }

                    List<Map.Entry<String, Integer>> info = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
                    Collections.sort(info, new Comparator<Map.Entry<String, Integer>>() {
                        public int compare(Map.Entry<String, Integer> obj1, Map.Entry<String, Integer> obj2) {
                            return obj2.getValue() - obj1.getValue();
                        }
                    });

                    for (int j = 0; j < info.size(); j++) {
                        if (j <= 4) {
                            Map.Entry<String, Integer> entry1 = info.get(j);
                            String now_sub_category = String.valueOf(entry1.getKey());
                            int now_get_count = entry1.getValue();
                            if (now_get_count < 10)
                                now_get_count = 10;
                            RecordSet n_recs = RecordSet.fromByteBuffer(qp.getAllApps(now_sub_category, false, "download", "package,version_code,version_name,sub_category", 0, now_get_count, minSDK));
                            for (Record r : n_recs) {
                                if (!packageList.contains(r.getString("package")))
                                    packageList.add(r.getString("package"));
                            }
                        }
                    }

                    for (int i = packageList.size() - 1; i >= 0; i--) {
                        for (Record r : userInstalledApps) {
                            if (r.getString("package").equals(packageList.get(i).toString())) {
                                packageList.remove(i);
                                break;
                            }
                        }
                    }
                    suggestedApps = getApps(user, StringUtils.join(packageList, ","), cols, false, ua, minSDK);
                } else {
                    suggestedApps.addAll(getManualApks(user, category, minSDK, cols));
                }
                /*
                                RecordSet viewerApps = getUserApps(user, user, "", removeExtenderColumn(cols), 0, 1000, false, "");
                //            	viewerApps = transDs(user, viewerApps, cols);
                                //Part 1 : manual
                                suggestedApps.addAll(getManualApks(Qiupu.MANUAL_SUGGEST));
                                //delete user's link apps
                                RecordSet removedApps = new RecordSet();
                                for (Record rec : suggestedApps) {
                                    if (viewerApps.contains(rec)) {
                                        removedApps.add(rec);
                                    }
                                }
                                suggestedApps.removeAll(removedApps);

                                //Part 2 : get apps from friend
                                int index = suggestedApps.size();
                                if (index < APPS_MAX_COUNT) {
                                    RecordSet friendApps = new RecordSet();
                                    RecordSet friends = RecordSet.fromByteBuffer(fs.getFriends(user, String.valueOf(Constants.FRIENDS_CIRCLE), 0, 1000));
                                    for (Record rec : friends) {
                                        String friend = rec.getString("friend");
                                        friendApps.addAll(getUserApps(user, friend, "", removeExtenderColumn(cols), 0, 1000, false, ""));
                                    }

                                    for (Record rec : friendApps) {
                                        String packageName = rec.getString("package");
                                        friendApps.foreachIf(RecordPredicates.valueEquals("package", packageName),
                                                new RecordHandler() {
                                                    @Override
                                                    public void handle(Record rec) {
                                                        long fcount = rec.getInt("fcount", 0);
                                                        rec.put("fcount", fcount + 1);
                                                    }
                                                });
                                    }

                                    friendApps.unique("package");
                                    friendApps.sort("fcount", false);
                                    friendApps.removeColumns("fcount");

                                    for (Record rec : friendApps) {
                                        if (!viewerApps.contains(rec)) {
                                            suggestedApps.add(rec);
                                        }
                                    }
                                }

                                //Part 3 :：get apps from rating desc
                                index = suggestedApps.size();
                                int num = APPS_MAX_COUNT - index;
                                if (index < APPS_MAX_COUNT) {
                                    suggestedApps.addAll(index, getAllApps(user, category, false, "rating", removeExtenderColumn(cols), 0, num, false));
                                    suggestedApps.unique("package");

                                    //delete user's link apps
                                    removedApps = new RecordSet();
                                    for (Record rec : suggestedApps) {
                                        if (viewerApps.contains(rec)) {
                                            removedApps.add(rec);
                                        }
                                    }
                                    suggestedApps.removeAll(removedApps);

                                    index = suggestedApps.size();
                                    int tempPage = 1;
                                    while (index < APPS_MAX_COUNT) {
                                        suggestedApps.addAll(index, transDs(user, getAllApps(user, category, false, "rating", removeExtenderColumn(cols), tempPage, num, false), cols, false));
                                        suggestedApps.unique("package");

                                        //delete user's link apps
                                        removedApps = new RecordSet();
                                        for (Record rec : suggestedApps) {
                                            if (viewerApps.contains(rec)) {
                                                removedApps.add(rec);
                                            }
                                        }
                                        suggestedApps.removeAll(removedApps);

                                        index = suggestedApps.size();
                                        tempPage++;
                                    }
                                }
                */

                suggestedApps.sliceByPage(page, count);
            } else {
                suggestedApps.addAll(getManualApks(user, category, minSDK, cols));
                suggestedApps.sliceByPage(page, count);
            }

            return suggestedApps;
        } finally {
            closeTransceiver(transqp);
        }
    }

    public RecordSet getManualApks(String userId, String category, int minSDK,String cols) throws Exception {
        RecordSet suggestedApps = new RecordSet();
        suggestedApps.addAll(getManualApks(Qiupu.MANUAL_SUGGEST, minSDK));

        //Part 2 :：get apps from rating desc
        int index = suggestedApps.size();
        int num = APPS_MAX_COUNT - index;
        if (index < APPS_MAX_COUNT) {
            suggestedApps.addAll(index, getAllApps(userId, category, false, "rating", removeExtenderColumn(cols), 0, num, false, minSDK));
            suggestedApps.unique("package");

            index = suggestedApps.size();
            int tempPage = 1;
            while (index < APPS_MAX_COUNT) {
                suggestedApps.addAll(index, getAllApps(userId, category, false, "rating", removeExtenderColumn(cols), tempPage, num, false, minSDK));
                suggestedApps.unique("package");
                index = suggestedApps.size();
                tempPage++;
            }
        }
        return suggestedApps;

    }

    public RecordSet getHotOrSelectedApps(String viewerId, int type, String category, String cols, int page, int count,int minSDK) throws Exception {
        RecordSet resultApps = new RecordSet();
        //Part 1 : manual
        resultApps.addAll(getManualApks(type,minSDK));

        //Part 2 : get Hot apps by download count desc，chosen apps by rating desc 
        int index = resultApps.size();
        int num = APPS_MAX_COUNT - index;
        if (index < APPS_MAX_COUNT) {
            String sort = (type == Qiupu.MANUAL_HOT) ? "download" : "rating";
            resultApps.addAll(index, getAllApps(viewerId, category, false, sort, removeExtenderColumn(cols), 0, num, false,minSDK));
            resultApps.unique("package");

            index = resultApps.size();
            int tempPage = 1;
            while (index < APPS_MAX_COUNT) {
                resultApps.addAll(index, getAllApps(viewerId, category, false, sort, removeExtenderColumn(cols), tempPage, num, false,minSDK));
                resultApps.unique("package");
                index = resultApps.size();
                tempPage++;
            }
        }

        resultApps.sliceByPage(page, count);
        return resultApps;
    }

    public RecordSet loadNeedExinfoApks(boolean isAll) throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return RecordSet.fromByteBuffer(qiupuInterface.loadNeedExinfoApks(isAll));
        } finally {
            closeTransceiver(trans);
        }
    }

    public int getApplicationCount() throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return qiupuInterface.getApplicationCount();
        } finally {
            closeTransceiver(trans);
        }
    }

    public int getTodayAppCount() throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return qiupuInterface.getTodayAppCount();
        } finally {
            closeTransceiver(trans);
        }
    }

    public int getNeedExinfoAppCount() throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return qiupuInterface.getNeedExinfoAppCount();
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateApk(Record info) throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return qiupuInterface.updateApk(info.toByteBuffer());
        } finally {
            closeTransceiver(trans);
        }
    }
    
    public boolean updateBorqsByPackage(String package_, int borqs) throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return qiupuInterface.updateBorqsByPackage(package_, borqs);
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean createUpdateApkLessDesc(String packageName, int version_code, String app_name, String desc) throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return qiupuInterface.createUpdateApkLessDesc(packageName, version_code, app_name, desc);
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet getUpdateApkLessDesc(int page, int count, String packageName, String app_name) throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            return RecordSet.fromByteBuffer(qiupuInterface.getUpdateApkLessDesc(page, count, packageName, app_name));
        } finally {
            closeTransceiver(trans);
        }
    }

    public RecordSet qiupuFormatData(String viewerId) throws Exception {
        Transceiver trans = getTransceiver(QiupuInterface.class);
        try {
            QiupuInterface qiupuInterface = getProxy(QiupuInterface.class, trans);
            RecordSet recs = RecordSet.fromByteBuffer(qiupuInterface.formatOldDataToConversation(viewerId));
            Transceiver tranc = getTransceiver(Conversation.class);
            Conversation c = getProxy(Conversation.class, tranc);
            /*
            for (Record r : recs) {
                if (!c.ifExistConversation(Constants.APK_OBJECT, r.getString("package"), Constants.C_APK_UPLOAD, Long.parseLong(r.getString("upload_user")))) {
                    Record conversation = new Record();
                    conversation.put("target_type", Constants.APK_OBJECT);
                    conversation.put("target_id", r.getString("package"));
                    conversation.put("reason", Constants.C_APK_UPLOAD);
                    conversation.put("from_", r.getString("upload_user"));
                    conversation.put("created_time", r.getString("created_time"));
                    c.createConversation(conversation.toByteBuffer());
                }
            }
            */
            for (Record r : recs) {
                if (!c.ifExistConversation(Constants.APK_OBJECT, r.getString("package"), Constants.C_APK_FAVORITE, Long.parseLong(r.getString("user")))) {
                    Record conversation = new Record();
                    conversation.put("target_type", Constants.APK_OBJECT);
                    conversation.put("target_id", r.getString("package"));
                    conversation.put("reason", Constants.C_APK_FAVORITE);
                    conversation.put("from_", r.getString("user"));
                    conversation.put("created_time", "1314836326708");
                    c.createConversation(conversation.toByteBuffer());
                }
            }

            return new RecordSet();
        } finally {
            closeTransceiver(trans);
        }
    }

    public boolean updateApkCategory() throws Exception {
        ConnectionFactory connectionFactory = ConnectionFactory.getConnectionFactory("dbcp");
        String db = "mysql/192.168.5.22/test_account2/root/111111";

        String sql = "SELECT distinct(package) from qapk";
        SQLExecutor se = new SQLExecutor(connectionFactory, db);
        RecordSet recs = se.executeRecordSet(sql, null);

        for (Record rec : recs) {
            String packageName = rec.getString("package");
            String sql1 = "select sub_category from qapk where package='" + packageName + "' order by sub_category desc limit 1";
            int a = (int) se.executeIntScalar(sql1, 0);
            String sql2 = "update qapk set sub_category='" + a + "' where package='" + packageName + "'";
            se.executeUpdate(sql2);
        }


        connectionFactory = ConnectionFactory.close(connectionFactory);
        return true;
    }
}
