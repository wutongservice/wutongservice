package com.borqs.server.service.qiupu;

import com.borqs.server.base.conf.Configurable;
import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordPredicates;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.TextCollection;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.compatible.CompatibleAddressInfo;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.friend.Relationship;
import com.borqs.server.platform.feature.like.LikeLogic;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostAttachments;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.qiupu.SimpleQiupu;
import com.borqs.server.service.platform.*;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


public class Qiupu extends ConfigurableBase {
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


    private QiupuInterface qiupu;
    private AccountLogic account;
    private FriendLogic friend;
    private StreamLogic stream;
    private CommentLogic comment;
    private LikeLogic like;
    private ConversationLogic conversation;


    public Qiupu(QiupuInterface qiupu, AccountLogic account, FriendLogic friend, StreamLogic stream, CommentLogic comment, LikeLogic like, ConversationLogic conversation) {
        this.qiupu = qiupu;
        this.account = account;
        this.friend = friend;
        this.stream = stream;
        this.comment = comment;
        this.like = like;
        this.conversation = conversation;
    }

    protected static String toStr(Object o) {
        return ObjectUtils.toString(o, "");
    }

    public RecordSet getAllApps(String viewerId, String sub_category, boolean paid, String sort, String cols, int page, int count, boolean history_version, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        RecordSet recs = RecordSet.fromByteBuffer(qp.getAllApps(sub_category, paid, sort, removeExtenderColumn(cols), page, count, minSDK));
        return transDs(viewerId, recs, cols, history_version, minSDK);
    }

    public RecordSet getAllAppsFull(String viewerId, String sub_category, boolean paid, String sort, int page, int count, boolean history_version, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return transDs(viewerId, RecordSet.fromByteBuffer(qp.getAllApps(sub_category, paid, sort, removeExtenderColumn(QAPK_FULL_COLUMNS), page, count, minSDK)), QAPK_FULL_COLUMNS, history_version, minSDK);
    }

    public RecordSet getTop1ApkByCategory(String sub_category) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return RecordSet.fromByteBuffer(qp.getTop1ApkByCategory(sub_category));
    }

    public RecordSet getApps(String viewerId, String apps, String cols, boolean history_version, String ua, int minSDK) throws AvroRemoteException {
        //maybe apps is package name,must transform to apkid
        if (!checkQiupuFitVersion(viewerId, apps, ua, minSDK)) {
            throw Errors.createResponseError(391, "version is too old");
        }
        apps = transformPackageNamesToApkids(apps, minSDK);
        QiupuInterface qp = qiupu;

        RecordSet r = RecordSet.fromByteBuffer(qp.getApps(apps, removeExtenderColumn(cols), minSDK));
        return r.size() > 0 ? transDs(viewerId, r, cols, history_version, minSDK) : new RecordSet();
    }

    public RecordSet getAppsFull(String viewerId, String apps, boolean history_version, String ua, int minSDK) throws AvroRemoteException {
        if (!checkQiupuFitVersion(viewerId, apps, ua, minSDK)) {
            throw Errors.createResponseError(391, "version is too old");
        }
        apps = transformPackageNamesToApkids(apps, minSDK);
        QiupuInterface qp = qiupu;
        RecordSet r = RecordSet.fromByteBuffer(qp.getApps(apps, removeExtenderColumn(QAPK_FULL_COLUMNS), minSDK));
        return r.size() > 0 ? transDs(viewerId, r, QAPK_FULL_COLUMNS, history_version, minSDK) : new RecordSet();
    }

    private static String getFriendIds(FriendLogic f, String viewerId, String circleIds) {
        PeopleIds friendIds;
        Context ctx = Context.createDummy();
        if (StringUtils.isNotEmpty(circleIds))
            friendIds = f.getFriendsInCircles(ctx, Long.parseLong(viewerId), StringHelper.splitIntArray(circleIds, ","));
        else
            friendIds = f.getFriends(ctx, Long.parseLong(viewerId));
        return StringHelper.join(friendIds.getUserIds(), ",");
    }

    public String parseUserIds(String viewerId, String userIds) throws AvroRemoteException {
        StringBuilder buff = new StringBuilder();
        FriendLogic f = friend;
        for (String userId : StringUtils2.splitList(userIds, ",", true)) {
            if (userId.startsWith("#")) {
                if (!viewerId.equals("0") && !viewerId.equals("")) {
                    String circleId = StringUtils.removeStart(userId, "#");

                    //RecordSet friendRecs = RecordSet.fromByteBuffer(f.getFriends(viewerId, circleId, 0, -1));
                    //buff.append(friendRecs.joinColumnValues("friend", ","));
                    // port:
                    buff.append(getFriendIds(f, viewerId, circleId));
                }
            } else {
                buff.append(userId);
            }
            buff.append(",");
        }
        return StringUtils2.stripItems(buff.toString(), ",", true);
    }

    private static RecordSet convsToApkRecs(Conversations convs) {
        RecordSet recs = new RecordSet();
        for (Conversation conv : convs) {
            recs.add(Record.of("source", Long.toString(conv.getUser()), "target", ObjectUtils.toString(conv.getTarget().id)));
        }
        return recs;
    }

    private static RecordSet targetsToApkRecs(PostAttachments attachments) {
        RecordSet recs = new RecordSet();
        for (PeopleId pid : attachments.getPeopleIds()) {
            if (pid.isUser()) {
                for (Target t : attachments.getTarget(pid)) {
                    if (t.type == Target.APK)
                        recs.add(Record.of("source", Long.toString(pid.getIdAsLong()), "target", ObjectUtils.toString(t.id)));
                }
            }
        }
        return recs;
    }

    public RecordSet getAppsSharedToMe(String viewerId, String cols, String userIds, boolean tome, String getType, boolean friend, int page, int count, boolean history_version, int minSDK) throws AvroRemoteException {
        StreamLogic s = stream;
        AccountLogic a = account;
        CommentLogic c = comment;
        ConversationLogic cc = conversation;
        FriendLogic f = this.friend;

        Context ctx = Context.createDummyForViewer(Long.parseLong(viewerId));

        if (cols.equals(""))
            cols = QAPK_FULL_COLUMNS;

        if (friend) {
            if (userIds.length() > 0) {
                userIds = parseUserIds(viewerId, userIds);
            } else {
                //userIds = RecordSet.fromByteBuffer(f.getFriends(viewerId, "", 0, 1000)).joinColumnValues("", ",");
                // port:
                userIds = getFriendIds(f, viewerId, "");
            }
        } else {
            if (userIds.length() > 0) {
                userIds = parseUserIds(viewerId, userIds);
            }
        }

        RecordSet r;
        if (getType.equals("comment")) {
            //r = RecordSet.fromByteBuffer(c.getObjectCommentByUsers(viewerId, userIds, "4", page, count));
            // port:
            r = convsToApkRecs(cc.findByUser(ctx, null, Actions.COMMENT, Target.APK, Page.of(page, count), StringHelper.splitLongArray(userIds, ",")));
        } else if (getType.equals("like")) {
            //r = RecordSet.fromByteBuffer(l.getObjectLikedByUsers(viewerId, userIds, "4", page, count));
            // port:
            r = convsToApkRecs(cc.findByUser(ctx, null, Actions.LIKE, Target.APK, Page.of(page, count), StringHelper.splitLongArray(userIds, ",")));
        } else {
            //r = RecordSet.fromByteBuffer(s.getApkSharedToMe(viewerId, userIds, tome, "", page, count));
            // port:
            PostFilter filter = new PostFilter(Post.POST_APK, App.APP_NONE, 0, 0, null);
            r = targetsToApkRecs(s.getWallTimelineAttachments(ctx, Long.parseLong(viewerId), filter, false, Page.of(page, count)));
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

        QiupuInterface qp = qiupu;

        RecordSet outr = RecordSet.fromByteBuffer(qp.getApps(apps, removeExtenderColumn(cols), minSDK));
        RecordSet out_recs = outr.size() > 0 ? transDs(viewerId, outr, cols, history_version, minSDK) : new RecordSet();
        for (Record r1 : out_recs) {
            if (map.get(r1.getString("apk_id")) != null) {
                String users = map.get(r1.getString("apk_id")).toString();
                //RecordSet u = RecordSet.fromByteBuffer(a.getUsers(users, "user_id, display_name, image_url"));
                // port:
                RecordSet u = getSimpleUsers(a, viewerId, users, "user_id, display_name, image_url");
                r1.put("whoshared", u);
            } else {
                r1.put("whoshared", "[]");
            }

            //tome
            String[] ss = StringUtils.split(StringUtils.trimToEmpty(r1.getString("apk_id")), '-');
            String package_ = ss[0].trim();


            //RecordSet recs_tome = RecordSet.fromByteBuffer(s.getApkSharedToMe(viewerId, userIds, true, package_, 0, 1));
            //r1.put("tome", recs_tome.size() > 0 ? true : false);

            //RecordSet recs_to_count = RecordSet.fromByteBuffer(s.getApkSharedToMe(viewerId, userIds, false, package_, 0, 1000));
            //r1.put("share_count", recs_to_count.size());
            // port:
            PostFilter filter = new PostFilter(Post.POST_APK, App.APP_NONE, 0, 0, null);
            PostAttachments pa = s.getWallTimelineAttachments(ctx, Long.parseLong(viewerId), filter, false, Page.of(0, 1000));
            int apkCount = pa.getAllTargetsCount();
            r1.put("tome", apkCount > 0);
            r1.put("share_count", apkCount);
        }
        return out_recs;
    }

    protected String transformPackageNamesToApkids(String apps, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
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
                    int maxV = getMaxVersionCode(ss[0], minSDK);
                    if (maxV == 0) {
                        m.add(app);
                    } else {
                        m.add(ss[0] + "-" + getMaxVersionCode(ss[0], minSDK) + "-" + "arm");
                    }
                }
            } else {
                m.add(ss[0] + "-" + getMaxVersionCode(ss[0], minSDK) + "-" + "arm");
            }
        }
        return StringUtils.join(m, ",");
    }

    public RecordSet getAppsOtherVersion(String apkId, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        RecordSet r = RecordSet.fromByteBuffer(qp.getQapkOtherVersion(apkId, minSDK));
        return r.size() > 0 ? r : new RecordSet();
    }

    public boolean saveQapkSuggest(String sub_id, String sub_name, int manual_count,
                                   int week_download_count, int month_download_count, int year_download_count, int sum_download_count,
                                   int rating_count, int borqs_count, int random_count, String hdpi_img_url, String mdpi_img_url) throws AvroRemoteException {

        QiupuInterface qp = qiupu;

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
    }

    public boolean deleteQapkSuggest(String sub_id) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return qp.deleteQapkSuggest(sub_id);
    }

    public boolean updateQapkIfSuggest(String sub_id) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return qp.updateQapkIfSuggest(sub_id);
    }

    public RecordSet getAllQapkSuggestType(int type, boolean ifsuggest) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return RecordSet.fromByteBuffer(qp.getSuggestApkfromSubId(String.valueOf(type), ifsuggest));
    }


    public RecordSet getPrefecturApps(String viewerId, String type, String cols, int page, int count, boolean history_version, String ua, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        RecordSet rsOut = new RecordSet();

        //get policy from type
        Record rec = RecordSet.fromByteBuffer(qp.getSuggestApkfromSubId(type, false)).getFirstRecord();

        String policy = rec.getString("policy");
        if (policy.length() > 0) {
            Record p_rec = Record.fromJson(policy);

            //NO.1： manual
            int count1 = (int) p_rec.getInt("manual_count");
            if (count1 > 0) {
                RecordSet recs0 = RecordSet.fromByteBuffer(qp.getManualApkIds(type, String.valueOf(count1), minSDK));
                rsOut.addAll(recs0);
            }

            //NO.2： download ranking list in week
            int count2 = (int) p_rec.getInt("week_download_count");
            if (count2 > 0) {
                long datediff = 60 * 60 * 24 * 7 * 1000;
                RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByTimes(datediff, ACTION_DOWNLOAD, count2, minSDK));
                if (recs0.size() > 0) {
                    rsOut.addAll(recs0);
                }
            }

            //NO.3： download ranking list in month
            int count3 = (int) p_rec.getInt("month_download_count");
            if (count3 > 0) {
                long datediff = 60 * 60 * 24 * 30 * 1000;
                RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByTimes(datediff, ACTION_DOWNLOAD, count3, minSDK));
                if (recs0.size() > 0) {
                    rsOut.addAll(recs0);
                }
            }

            //NO.4： download ranking list in year
            int count4 = (int) p_rec.getInt("year_download_count");
            if (count4 > 0) {
                long datediff = 60 * 60 * 24 * 365 * 1000;
                RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByTimes(datediff, ACTION_DOWNLOAD, count4, minSDK));
                if (recs0.size() > 0) {
                    rsOut.addAll(recs0);
                }
            }

            //NO.5： download ranking list in db
            int count5 = (int) p_rec.getInt("sum_download_count");
            if (count5 > 0) {
                RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByDownloadTimes(count5, minSDK));
                if (recs0.size() > 0) {
                    rsOut.addAll(recs0);
                }
            }

            //NO.6： rating ranking list
            int count6 = (int) p_rec.getInt("rating_count");
            if (count6 > 0) {
                RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByRating(count6, minSDK));
                if (recs0.size() > 0) {
                    rsOut.addAll(recs0);
                }
            }

            //NO.7：force recommended borqs app
            int count7 = (int) p_rec.getInt("borqs_count");
            if (count7 > 0) {
                RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByBorqs(count7, minSDK));
                rsOut.addAll(recs0);
            }

            //NO.8：random apps from lasted 1000
            int count8 = (int) p_rec.getInt("random_count");
            if (count8 > 0) {
                RecordSet recs0 = RecordSet.fromByteBuffer(qp.getApkIdsListByRandom(count8, minSDK));
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
        RecordSet recs = getApps(viewerId, rsOut.joinColumnValues("apk_id", ","), cols.equals("") ? QAPK_BASE_FULL_COLUMNS : cols, history_version, ua, minSDK);
        return recs;
    }

    private static Record getSimpleUser(AccountLogic account, String viewerId, String userId, String cols) {
        User u0 = account.getUser(Context.createDummyForViewer(Long.parseLong(viewerId)),
                User.STANDARD_COLUMNS, Long.parseLong(userId));
        return userToRecord(u0, cols);
    }

    private static RecordSet getSimpleUsers(AccountLogic account, String viewerId, String userIds, String cols) {
        Users users = account.getUsers(Context.createDummyForViewer(Long.parseLong(viewerId)),
                User.STANDARD_COLUMNS, StringHelper.splitLongArray(userIds, ","));
        RecordSet recs = new RecordSet();
        for (User user : users)
            recs.add(userToRecord(user, cols));
        return recs;
    }

    private static Record userToRecord(User user, String cols) {
        Record rec = new Record();
        if (user != null) {
            String[] v1Cols = StringHelper.splitArray(cols, ",", true);
            if (ArrayUtils.contains(v1Cols, "user_id"))
                rec.set("user_id", user.getUserId());
            if (ArrayUtils.contains(v1Cols, "display_name"))
                rec.set("display_name", ObjectUtils.toString(user.getDisplayName()));
            if (ArrayUtils.contains(v1Cols, "image_url"))
                rec.put("image_url", user.getPhoto() != null ? ObjectUtils.toString(user.getPhoto().getMiddleUrl()) : "");
            if (ArrayUtils.contains(v1Cols, "address")) {
                String addrsJson = user.getAddress() != null ? CompatibleAddressInfo.addressesToJson(user.getAddress(), false) : "[]";
                rec.put("address", JsonUtils.parse(addrsJson));
            }
        }
        return rec;
    }

    //get strong man
    public RecordSet getStrongMen(String sub_category, int page, int count) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        AccountLogic ac = account;

        RecordSet r = RecordSet.fromByteBuffer(qp.getStrongMan(sub_category, page, count));
        RecordSet rs = new RecordSet();

        for (Record t : r) {
            Record p = new Record();
            //Record t0 = RecordSet.fromByteBuffer(ac.getUsers(toStr(t.getInt("user")), "user_id, display_name, image_url")).getFirstRecord();
            // port:
            String viewerId = toStr(t.getInt("user"));
            Record t0 = getSimpleUser(ac, viewerId, viewerId, "user_id, display_name, image_url");

            p.put("user_id", t0.getString("user_id"));
            p.put("display_name", t0.getString("display_name"));
            p.put("image_url", t0.getString("image_url"));

            p.put("app_count", t.getString("COUNT1"));
            rs.add(p);
        }
        return rs;
    }

    public RecordSet getApp(String viewerId, String apkId, String cols, boolean history_version, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        Record r = Record.fromByteBuffer(qp.getSingleApp(apkId, removeExtenderColumn(cols), minSDK));
        RecordSet rs = new RecordSet();
        rs.add(r);
        return transDs(viewerId, rs, cols, history_version, minSDK);
    }

    public Record getSingleApp(String apkId, String cols, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        Record r = Record.fromByteBuffer(qp.getSingleApp(apkId, removeExtenderColumn(cols), minSDK));
        return r;
    }

    public int getMaxVersionCode(String app, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return qp.getMaxVersionCode(app, minSDK);
    }

    public boolean checkQiupuFitVersion(String viewerId, String apkIds, String ua, int minSDK) throws AvroRemoteException {
        //must qiupu
        //versioncode < 180,return false,else return true
        //versioncode :110
        //user id must :180
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
                            qiupuVersion = getMaxVersionCode(ll, minSDK);
                            break;
                        }
                    } else {
                        qiupuVersion = getMaxVersionCode(ll, minSDK);
                        break;
                    }
                }
            }
        }
        return qiupuVersion == 0 ? true : (qiupuVersion >= Integer.valueOf(findQiupuMinVersion()));
    }

    private String findQiupuMinVersion() {
        // TODO: IMPL
        return "";
    }


    public RecordSet getAppFull(String viewerId, String apkId, boolean history_version, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return transDs(viewerId, RecordSet.fromByteBuffer(qp.getSingleApp(apkId, removeExtenderColumn(QAPK_FULL_COLUMNS), minSDK)), QAPK_FULL_COLUMNS, history_version, minSDK);
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

    private static RecordSet getAppComments(String viewerId, String apkObjectId, String v1Cols, int page, int count) {
        // TODO: xx!!!!!
        return new RecordSet();
    }

    //add qiupu other columns
    private RecordSet transDs(String viewerId, RecordSet ds, String cols, boolean history_version, int minSDK) throws AvroRemoteException {
        CommentLogic c = comment;
        AccountLogic ac = account;
        LikeLogic lk = like;
        QiupuInterface qp = qiupu;

        Context ctx = Context.createDummyForViewer(Long.parseLong(viewerId));

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
                            //rec.put("upload_user", RecordSet.fromByteBuffer(ac.getUsers(toStr(rec.getInt("upload_user")), "user_id, display_name, image_url, address")).getFirstRecord());
                            // port:
                            rec.put("upload_user", getSimpleUser(ac, viewerId, toStr(rec.getInt("upload_user")), "user_id, display_name, image_url, address"));
                        } else {
                            rec.put("upload_user", new RecordSet());
                        }
                    }

                    if (l.contains("app_comment_count")) {
                        // rec.put("app_comment_count", c.getCommentCount(viewerId, targetObjectId));
                        // port:
                        rec.put("app_comment_count", c.getCommentCount(ctx, Target.parseCompatibleString(targetObjectId)));
                    }
                    if (l.contains("app_comments")) {
                        String ccols = "comment_id, target, created_time,commenter, commenter_name, message, device, can_like,destroyed_time";
                        //RecordSet ds1 = RecordSet.fromByteBuffer(c.getCommentsForContainsIgnore(viewerId, targetObjectId, ccols, false, 0, 2));
                        // port:
                        RecordSet ds1 = getAppComments(viewerId, targetObjectId, ccols, 0, 2);
                        for (Record r : ds1) {
                            //r.put("iliked", viewerId.equals("") ? false : lk.ifUserLiked(viewerId, Constants.COMMENT_OBJECT + ":" + r.getString("comment_id")));
                            // port:
                            r.put("iliked", viewerId.equals("") ? false : lk.isLiked(ctx, Long.parseLong(viewerId), Target.forComment(r.getInt("comment_id"))));
                        }
                        rec.put("app_comments", ds1);
                    }
                    if (l.contains("app_like_count")) {
                        //rec.put("app_like_count", lk.getLikeCount(targetObjectId));
                        // port:
                        rec.put("app_like_count", lk.getLikeCount(ctx, Target.parseCompatibleString(targetObjectId)));
                    }
                    if (l.contains("app_liked_users")) {
                        //RecordSet uids = RecordSet.fromByteBuffer(lk.loadLikedUsers(targetObjectId, 0, 5));
                        //if (uids.size() > 0) {
                        //    String uids0 = uids.joinColumnValues("liker", ",");
                        //    RecordSet recs_users = RecordSet.fromByteBuffer(ac.getUsers(uids0, "user_id, display_name, image_url"));
                        //    rec.put("app_liked_users", recs_users);
                        //} else {
                        //    rec.put("app_liked_users", new RecordSet());
                        //}
                        // port:
                        long[] likerIds = lk.getLikedUsers(ctx, Target.parseCompatibleString(targetObjectId), Page.of(0, 5));
                        if (ArrayUtils.isNotEmpty(likerIds)) {
                            RecordSet recs_users = getSimpleUsers(ac, viewerId, StringHelper.join(likerIds, ","), "user_id, display_name, image_url");
                            rec.put("app_liked_users", recs_users);
                        } else {
                            rec.put("app_liked_users", new RecordSet());
                        }
                    }

                    if (l.contains("app_installed_users")) {
                        RecordSet installed_users = getUsedAppUsers(viewerId, package_, "installed", "user_id, display_name, image_url", 0, 4);
                        if (installed_users.size() > 0) {
                            rec.put("app_installed_users", installed_users);
                        } else {
                            rec.put("app_installed_users", new RecordSet());
                        }
                    }
                    if (l.contains("app_likes")) {
                        String targetId = Constants.objectId(Constants.APK_OBJECT, apk_id);
                        //rec.put("app_likes", viewerId.equals("") ? false : lk.ifUserLiked(viewerId, targetId));
                        // port:
                        rec.put("app_likes", StringUtils.isEmpty(viewerId) ? false : lk.isLiked(ctx, Long.parseLong(viewerId), Target.parseCompatibleString(targetId)));
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

                    int maxId = getMaxVersionCode(package_, minSDK);
                    String apkidTemp = package_ + "-" + String.valueOf(maxId) + "-" + arch_;

                    if (l.contains("lasted_version_code")) {
                        rec.put("lasted_version_code", String.valueOf(maxId));
                    }

                    Record rectemp = getSingleApp(apkidTemp, "version_name", minSDK);
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
                        RecordSet o = getAppsOtherVersion(apk_id, minSDK);
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

    public RecordSet getUserApps(String viewerId, String userId, String action, String cols, int page, int count, boolean history_version, String delApks, int minSDK) throws AvroRemoteException {
        return getUserAppsFor(viewerId, userId, action, cols, page, count, history_version, delApks, minSDK);
    }

    public RecordSet getUserAppsFull(String viewerId, String userId, String action, int page, int count, boolean history_version, String delApks, int minSDK) throws AvroRemoteException {
        return getUserAppsFor(viewerId, userId, action, "", page, count, history_version, delApks, minSDK);
    }

    public RecordSet getUserAppsFor(String viewerId, String userId, String action, String cols, int page, int count, boolean history_version, String delApks, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        FriendLogic fs = friend;
        Context ctx = Context.createDummyForViewer(Long.parseLong(viewerId));
        int max = PRIVACY_PUBLIC;
        if (!viewerId.equals("")) {
            //RecordSet recsfs = RecordSet.fromByteBuffer(fs.getRelation(viewerId, userId, String.valueOf(Constants.FRIENDS_CIRCLE)));
            //max = viewerId.equals(userId) ? PRIVACY_ME : (recsfs.size() > 0 ? PRIVACY_FRIEND : PRIVACY_PUBLIC);
            // port:
            Relationship rs = fs.getRelationship(ctx, PeopleId.user(ctx.getViewer()), PeopleId.user(Long.parseLong(userId)));
            max = viewerId.equals(userId) ? PRIVACY_ME : (rs.isViewerFriend() ? PRIVACY_FRIEND : PRIVACY_PUBLIC);
        }
        if (cols.equals("")) {
            return transDs(viewerId, RecordSet.fromByteBuffer(qp.getUserApps(userId, max, action, removeExtenderColumn(QAPK_FULL_COLUMNS), page, count, delApks, minSDK)), QAPK_FULL_COLUMNS, history_version, minSDK);
        } else {
            return transDs(viewerId, RecordSet.fromByteBuffer(qp.getUserApps(userId, max, action, removeExtenderColumn(cols), page, count, delApks, minSDK)), cols, history_version, minSDK);
        }
    }


    public RecordSet getUsedAppUsers(String viewerId, String app, String reason, String cols, int page, int count) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        AccountLogic a = account;
        FriendLogic f = friend;
        Context ctx = Context.createDummyForViewer(Long.parseLong(viewerId));
        RecordSet recsfs = new RecordSet();
        if (viewerId.equals("")) {
            recsfs = RecordSet.fromByteBuffer(qp.getLinkedAppUsers(viewerId, app, toStr(REASONS.getValue("reason", REASON_INSTALLED)), "", page, count, 0));
        } else {
            //get myfriend
            //RecordSet recs_friend = RecordSet.fromByteBuffer(f.getFriends(viewerId, String.valueOf(Constants.FRIENDS_CIRCLE), 0, 1000));
            //String friendsIds = recs_friend.joinColumnValues("friend", ",");
            // port:
            String friendsIds = StringHelper.join(f.getFriends(ctx, Long.parseLong(viewerId)).getUserIds(), ",");
            recsfs = RecordSet.fromByteBuffer(qp.getLinkedAppUsers(viewerId, app, toStr(REASONS.getValue("reason", REASON_INSTALLED)), friendsIds, page, count, 1));
            if (recsfs.size() < count) {
                RecordSet recs_t = RecordSet.fromByteBuffer(qp.getLinkedAppUsers(viewerId, app, toStr(REASONS.getValue("reason", REASON_INSTALLED)), friendsIds, 0, count - recsfs.size(), 2));
                if (recs_t.size() > 0) {
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
                    //RecordSet recordSet = RecordSet.fromByteBuffer(f.getRelation(viewerId, r.getString("user"), String.valueOf(FRIENDS_CIRCLE)));
                    //if (recordSet.size() > 0)
                    //    recsfs_0.add(r);
                    // port:
                    Relationship rs = f.getRelationship(ctx, PeopleId.user(ctx.getViewer()), PeopleId.user(r.getInt("user")));
                    if (rs.isViewerFriend())
                        recsfs_0.add(r);
                }
                if (privacy.equals("3")) {    //me can see
                    if (viewerId.equals(r.getString("user")))
                        recsfs_0.add(r);
                }
            }
        }

        if (recsfs_0.size() > 0) {
            //RecordSet u = RecordSet.fromByteBuffer(a.getUsers(recsfs_0.joinColumnValues("user", ","), cols));
            // port:
            RecordSet u = getSimpleUsers(a, viewerId, recsfs_0.joinColumnValues("user", ","), cols);
            return u;
        } else {
            return new RecordSet();
        }
    }

    public boolean setLinkUserApp(String userId, String app, int version_code, int arch, String action, String deviceid, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return qp.setLinkUserApp(userId, app, version_code, arch, toStr(action), toStr(deviceid), minSDK);
    }

    public boolean removeUserLinkApp(String userId, String apps, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        boolean b = qp.deleteUserQapp(toStr(userId), toStr(apps), toStr(ACTION_REMOVE_USER_APP), minSDK);
        return b;
    }

    public RecordSet searchApps(String viewerId, String value, String cols, int page, int count, boolean history_version, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return transDs(viewerId, RecordSet.fromByteBuffer(qp.searchApps(value, removeExtenderColumn(cols), page, count, minSDK)), cols, history_version, minSDK);
    }

    public RecordSet searchAppsFull(String viewerId, String value, int page, int count, boolean history_version, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return transDs(viewerId, RecordSet.fromByteBuffer(qp.searchApps(value, removeExtenderColumn(QAPK_FULL_COLUMNS), page, count, minSDK)), QAPK_FULL_COLUMNS, history_version, minSDK);
    }

    public boolean setAppfavorite(String userId, String app, String deviceid, int minSDK) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        Context ctx = Context.createDummyForViewer(Long.parseLong(userId));

        String inApkId = app;
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(inApkId), "-", true);
        if (a1.length < 3) {
            inApkId = app + "-" + getMaxVersionCode(app, minSDK) + "-arm";
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
            ConversationLogic c = conversation;
            if (action == ACTION_FAVORITE) {
                //Record r0 = new Record();
                //r0.put("target_type", Constants.APK_OBJECT);
                //r0.put("target_id", a[0]);
                //r0.put("reason", Constants.C_APK_FAVORITE);
                //r0.put("from_", userId);
                //c.createConversation(r0.toByteBuffer());
                // port:
                c.create(ctx, new ConversationBase(Target.forApk(a[0]), Actions.FAVORITE));
            } else if (action == ACTION_UNFAVORITE) {
                //c.deleteConversation(Constants.APK_OBJECT, a[0], Constants.C_APK_FAVORITE, Long.parseLong(userId));
                // port:
                c.delete(ctx, new ConversationBase(Target.forApk(a[0]), Actions.FAVORITE));
            }
        }
        return qp.setLinkUserApp(userId, a[0], Integer.parseInt(a[1]), ARCHS.getValue(a[2], 1), toStr(action), toStr(deviceid), minSDK);
    }

    public boolean userSettingAll(String userId, String value) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return qp.userSettingAll(userId, toStr(SETTING_APP_PRIVACY), value);
    }

    public boolean setUserSingleAppPrivancy(String userId, String app, String value) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return qp.setUserAppPrivancy(userId, toStr(app), value);
    }

    public String getUserSettingAll(String userId) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return qp.getUserSettingAll(userId, toStr(SETTING_APP_PRIVACY)).toString();
    }

    public String getUserSingleAppPrivancy(String userId, String app) throws AvroRemoteException {
        QiupuInterface qp = qiupu;
        return qp.getUserSingleAppPrivancy(userId, toStr(app)).toString();
    }

    public String download(String userId, String apkId, String deviceid, int minSDK) throws AvroRemoteException, IOException {
        QiupuInterface qp = qiupu;
        String[] a = StringUtils2.splitArray(StringUtils.trimToEmpty(apkId), "-", true);
        String inApkId = apkId;
        if (a.length < 3) {
            inApkId = apkId + "-" + getMaxVersionCode(apkId, minSDK) + "-arm";
        }

        qp.download(userId, inApkId, toStr(ACTION_DOWNLOAD), toStr(deviceid));
        //if this user has downloaded then
        return "download success!";
    }

    public boolean uploadApk(String user, Record apkInfo, String deviceid) throws AvroRemoteException {
        QiupuInterface qiupuInterface = qiupu;
        Context ctx = Context.createDummyForViewer(Long.parseLong(user));
        if (!StringUtils.isEmpty(user)) {
            ConversationLogic c = conversation;
            //Record r0 = new Record();
            //r0.put("target_type", Constants.APK_OBJECT);
            //r0.put("target_id", apkInfo.getString("package"));
            //r0.put("reason", Constants.C_APK_UPLOAD);
            //r0.put("from_", user);
            //c.createConversation(r0.toByteBuffer());
            // port:
            c.create(ctx, new ConversationBase(Target.forApk(apkInfo.getString("package")), Actions.UPLOAD));
        }
        return qiupuInterface.uploadApk(user, apkInfo.toByteBuffer(), toStr(deviceid));
    }

    public RecordSet syncApks(String user, String apkIds, boolean all, String deviceid, int minSDK) throws AvroRemoteException {
        QiupuInterface qiupuInterface = qiupu;
        return RecordSet.fromByteBuffer(qiupuInterface.syncApks(user, apkIds, all, deviceid, minSDK));
    }

    public RecordSet getLastInstalledApp(String user, boolean history_version, int minSDK) throws AvroRemoteException {
        QiupuInterface qiupuInterface = qiupu;
        return transDs(user, RecordSet.fromByteBuffer(qiupuInterface.getLastInstalledApp(user, removeExtenderColumn(QAPK_FULL_COLUMNS), minSDK)), QAPK_FULL_COLUMNS, history_version, minSDK);
    }

    public boolean manualApks(String apkIds, String types) throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return qiupuInterface.manualApks(apkIds, types);
    }

    public RecordSet getManualApks(int type, int minSDK) throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return RecordSet.fromByteBuffer(qiupuInterface.getManualApks(type, minSDK));
    }

    public RecordSet suggestApks0(String user, String cols, int page, int count, int minSDK) throws Exception {
        RecordSet suggestedApps = new RecordSet();
        //Part 1 :manual
        suggestedApps.addAll(getManualApks(Qiupu.MANUAL_SUGGEST, minSDK));

        if (StringUtils.isNotBlank(user)) {
            RecordSet viewerApps = getUserApps(user, user, "", removeExtenderColumn(cols), 0, 1000, false, "", minSDK);
            viewerApps = transDs(user, viewerApps, cols, false, minSDK);

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

    public RecordSet suggestApks1(String user, String cols, int page, int count, int minSDK) throws Exception {
        RecordSet suggestedApps = new RecordSet();
        FriendLogic fs = friend;
        Context ctx = Context.createDummyForViewer(Long.parseLong(user));
        if (StringUtils.isNotBlank(user)) {
            RecordSet viewerApps = getUserApps(user, user, "", removeExtenderColumn(cols), 0, 1000, false, "", minSDK);
            viewerApps = transDs(user, viewerApps, cols, false, minSDK);

            //Part 2 : get apps from friend
            RecordSet friendApps = new RecordSet();
            //RecordSet friends = RecordSet.fromByteBuffer(fs.getFriends(user, String.valueOf(Constants.FRIENDS_CIRCLE), 0, 1000));
            //for (Record rec : friends) {
            //    String friend = rec.getString("friend");
            //    friendApps.addAll(transDs(user, getUserApps(user, friend, "", removeExtenderColumn(cols), 0, 1000, false, "", minSDK), cols, false, minSDK));
            //}
            // port:
            long[] friendIds = fs.getFriends(ctx, Long.parseLong(user)).getUserIds();
            for (long friendId : friendIds) {
                friendApps.addAll(transDs(user, getUserApps(user, Long.toString(friendId), "", removeExtenderColumn(cols), 0, 1000, false, "", minSDK), cols, false, minSDK));
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
    }

    public RecordSet suggestApks2(String user, String category, String cols, int page, int count) throws Exception {
        RecordSet suggestedApps = new RecordSet();

        //TODO: Part 3 :：get apps from rating desc

        suggestedApps.sliceByPage(page, count);
        return suggestedApps;
    }

    public RecordSet suggestApks(String user, String category, String cols, int page, int count, String ua, int minSDK) throws Exception {
        RecordSet suggestedApps = new RecordSet();
        QiupuInterface qp = qiupu;
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
    }

    public RecordSet getManualApks(String userId, String category, int minSDK, String cols) throws Exception {
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

    public RecordSet getHotOrSelectedApps(String viewerId, int type, String category, String cols, int page, int count, int minSDK) throws Exception {
        RecordSet resultApps = new RecordSet();
        //Part 1 : manual
        resultApps.addAll(getManualApks(type, minSDK));

        //Part 2 : get Hot apps by download count desc，chosen apps by rating desc 
        int index = resultApps.size();
        int num = APPS_MAX_COUNT - index;
        if (index < APPS_MAX_COUNT) {
            String sort = (type == Qiupu.MANUAL_HOT) ? "download" : "rating";
            resultApps.addAll(index, getAllApps(viewerId, category, false, sort, removeExtenderColumn(cols), 0, num, false, minSDK));
            resultApps.unique("package");

            index = resultApps.size();
            int tempPage = 1;
            while (index < APPS_MAX_COUNT) {
                resultApps.addAll(index, getAllApps(viewerId, category, false, sort, removeExtenderColumn(cols), tempPage, num, false, minSDK));
                resultApps.unique("package");
                index = resultApps.size();
                tempPage++;
            }
        }

        resultApps.sliceByPage(page, count);
        return resultApps;
    }

    public RecordSet loadNeedExinfoApks(boolean isAll) throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return RecordSet.fromByteBuffer(qiupuInterface.loadNeedExinfoApks(isAll));
    }

    public int getApplicationCount() throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return qiupuInterface.getApplicationCount();
    }

    public int getTodayAppCount() throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return qiupuInterface.getTodayAppCount();
    }

    public int getNeedExinfoAppCount() throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return qiupuInterface.getNeedExinfoAppCount();
    }

    public boolean updateApk(Record info) throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return qiupuInterface.updateApk(info.toByteBuffer());
    }

    public boolean createUpdateApkLessDesc(String packageName, int version_code, String app_name, String desc) throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return qiupuInterface.createUpdateApkLessDesc(packageName, version_code, app_name, desc);
    }

    public RecordSet getUpdateApkLessDesc(int page, int count, String packageName, String app_name) throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        return RecordSet.fromByteBuffer(qiupuInterface.getUpdateApkLessDesc(page, count, packageName, app_name));
    }

    public RecordSet qiupuFormatData(String viewerId) throws Exception {
        QiupuInterface qiupuInterface = qiupu;
        Context ctx = Context.createDummyForViewer(Long.parseLong(viewerId));

        RecordSet recs = RecordSet.fromByteBuffer(qiupuInterface.formatOldDataToConversation(viewerId));
        ConversationLogic c = conversation;
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
            //if (!c.ifExistConversation(Constants.APK_OBJECT, r.getString("package"), Constants.C_APK_FAVORITE, Long.parseLong(r.getString("user")))) {
            //    Record conversation = new Record();
            //    conversation.put("target_type", Constants.APK_OBJECT);
            //    conversation.put("target_id", r.getString("package"));
            //    conversation.put("reason", Constants.C_APK_FAVORITE);
            //    conversation.put("from_", r.getString("user"));
            //    conversation.put("created_time", "1314836326708");
            //    c.createConversation(conversation.toByteBuffer());
            //}
            // port:
            String appId = r.getString("package");
            if (!c.has(ctx, ctx.getViewer(), Target.forApk(appId), Actions.FAVORITE)) {
                c.create(ctx, new ConversationBase(Target.forApk(appId), Actions.FAVORITE));
            }
        }

        return new RecordSet();
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

    public RecordSet getUsersAppCount(String userIds, String reason) throws Exception {
        return RecordSet.fromByteBuffer(qiupu.getUsersAppCount(userIds, reason));
    }
}
