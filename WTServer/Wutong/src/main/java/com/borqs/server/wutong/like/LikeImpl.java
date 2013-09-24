package com.borqs.server.wutong.like;

import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.*;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.Commons;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class LikeImpl implements LikeLogic, Initializable {
    private static final Logger L = Logger.getLogger(LikeImpl.class);
    public final Schema likeSchema = Schema.loadClassPath(LikeImpl.class, "like.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String likeTable;
    private Commons commons;
    private Configuration conf;

    public void init() {
        conf = GlobalConfig.get();
        commons = new Commons();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("like.simple.db", null);
        this.likeTable = conf.getString("like.simple.likeTable", "like_");
    }

    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    public boolean saveLike(Context ctx, Record like) {
        final String METHOD = "saveLike";
        L.traceStartCall(ctx, METHOD, like);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, like, add)}";

        String sql = SQLTemplate.merge(SQL,
                "table", likeTable, "alias", likeSchema.getAllAliases(),
                "like", like, "add", Record.of("created_time", DateUtils.nowMillis()));

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean destroyLike(Context ctx, String userId, String targetId) {
        final String METHOD = "destroyLike";
        L.traceStartCall(ctx, METHOD, userId,targetId);
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "DELETE FROM ${table} WHERE ${alias.liker}=${v(user_id)} AND ${alias.target} like '%" + a2[0] + "-%'";
        } else {
            SQL = "DELETE FROM ${table} WHERE ${alias.liker}=${v(user_id)} AND ${alias.target}=${v(target)}";
        }

        String sql = SQLTemplate.merge(SQL, "table", likeTable, "alias", likeSchema.getAllAliases(),
                "user_id", userId, "target", targetId);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public int getLikeCount(Context ctx, String targetId) {
        final String METHOD = "getLikeCount";
        L.traceStartCall(ctx, METHOD, targetId);
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT count(*) FROM ${table} WHERE ${alias.target} like '%" + a2[0] + "-%'";
        } else {
            SQL = "SELECT count(*) FROM ${table} WHERE ${alias.target}=${v(target)}";
        }
        String sql = SQLTemplate.merge(SQL, "table", likeTable, "alias", likeSchema.getAllAliases(), "target", targetId);
        SQLExecutor se = getSqlExecutor();
        int r = 0;
        if (se.executeScalar(sql) == null || ((Number) se.executeScalar(sql)).intValue() < 0) {
        } else {
            r = ((Number) se.executeScalar(sql)).intValue();
        }
        L.traceEndCall(ctx, METHOD);
        return r;
    }

    public boolean ifUserLiked(Context ctx, String userId, String targetId) {
        final String METHOD = "ifUserLiked";
        L.traceStartCall(ctx, METHOD, userId,targetId);
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${alias.target} FROM ${table} WHERE ${alias.target} like '%" + a2[0] + "-%' AND ${alias.liker}=${v(user_id)}";
        } else {
            SQL = "SELECT ${alias.target} FROM ${table} WHERE ${alias.target}=${v(target)} AND ${alias.liker}=${v(user_id)}";
        }
        String sql = SQLTemplate.merge(SQL, "alias", likeSchema.getAllAliases(), "table", likeTable,
                "target", targetId, "user_id", userId);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        L.traceEndCall(ctx, METHOD);
        return !rec.isEmpty();
    }

    public RecordSet loadLikedUsers(Context ctx, String targetId, int page, int count) {
        final String METHOD = "loadLikedUsers";
        L.traceStartCall(ctx, METHOD, targetId,page,count);
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${alias.liker} FROM ${table}"
                    + " WHERE ${alias.target} like '%" + a2[0] + "%' ORDER BY created_time DESC ${limit}";
        } else {
            SQL = "SELECT ${alias.liker} FROM ${table}"
                    + " WHERE ${alias.target}=${v(target)} ORDER BY created_time DESC ${limit}";
        }

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", likeSchema.getAllAliases()},
                {"table", likeTable},
                {"target", targetId},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(likeSchema, recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getLikedPost(Context ctx, String userId, int page, int count, int objectType) {
        final String METHOD = "getLikedPost";
        L.traceStartCall(ctx, METHOD, userId,page,count);
        final String SQL = "SELECT DISTINCT(${alias.target}) FROM ${table} use index (liker)"
                + " WHERE ${alias.liker}=${liker} AND LEFT(${alias.target},1)=${objectType} AND LEFT(${alias.target},1)<>${v(objectType1)} ORDER BY ${alias.created_time} DESC ${limit}";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", likeSchema.getAllAliases()},
                {"table", likeTable},
                {"objectType", String.valueOf(objectType)},
                {"objectType1", String.valueOf(objectType) + ":0"},
                {"liker", userId},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(likeSchema, recs);
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet getObjectLikedByUsers(Context ctx, String viewerId, String userIds, String objectType, int page, int count) {
        final String METHOD = "getObjectLikedByUsers";
        L.traceStartCall(ctx, METHOD, viewerId,userIds,objectType,page,count);
        List<String> cols0 = StringUtils2.splitList("target,liker", ",", true);
        final String sql = new SQLBuilder.Select(likeSchema)
                .select(cols0)
                .from("like_")
                .where("0 = 0")
                .and("left(target,2)='" + objectType + ":'")
                .and("length(target)>" + Constants.USER_ID_MAX_LEN)
                .andIf(!userIds.isEmpty(), "liker IN (" + userIds + ")")
                .orderBy("created_time", "DESC")
                .limitByPage(page, count)
                .toString();

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        for (Record r : recs) {
            r.put("target", r.getString("target").replace(objectType + ":", ""));
            r.renameColumn("liker", "source");
        }
        L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public boolean updateLikeTarget(Context ctx, String old_target, String new_target) {
        final String METHOD = "updateLikeTarget";
        L.traceStartCall(ctx, METHOD, old_target,new_target);
        String sql = "update " + likeTable + " set target = '" + new_target + "' where target='" + old_target + "'";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean createLike(Context ctx, String userId, String targetId) {
        final String METHOD = "createLike";
        L.traceStartCall(ctx, METHOD, userId,targetId);
        boolean b = false;
        if (!ifUserLiked(ctx, userId, targetId)) {
            Record like = Record.of("target", targetId, "liker", userId);
            Schemas.standardize(likeSchema, like);
            b= saveLike(ctx, like);
        }
        L.traceEndCall(ctx, METHOD);
        return b;
    }

    public boolean likeP(Context ctx, String userId, int objectType, String target, String device, String location, String appId) {
        final String METHOD = "likeP";
        L.traceStartCall(ctx, METHOD, userId,objectType,target,device,location,appId);
        Record user = GlobalLogics.getAccount().getUser(ctx, userId, userId, "user_id, display_name, login_email1, login_email2, login_email3", false);
        if (user.isEmpty()) {
            throw new ServerException(WutongErrors.USER_NOT_EXISTS, " User '%s' is not exists", userId);
        }

        if (objectType == Constants.POST_OBJECT) {
            if (!GlobalLogics.getStream().postCanLikeP(ctx, target)) {
                throw new ServerException(WutongErrors.STREAM_CANT_LIKE, "The formPost '%s' is not can like", target);
            }
        } else if (objectType == Constants.COMMENT_OBJECT) {
            if (!GlobalLogics.getComment().commentCanLikeP(ctx, userId, target)) {
                throw new ServerException(WutongErrors.COMMENT_CANT_LIKE, "The comment '%s' is not can like", target);
            }
        } else if (objectType == Constants.APK_OBJECT) {
            // OK
        } else if (objectType == Constants.PHOTO_OBJECT) {
            // OK
        } else if (objectType == Constants.POLL_OBJECT) {
            // OK
        } else {
            throw new ServerException(WutongErrors.LIKE_OBJECT_CANT_LIKE, "The object '%s' is not can like", Constants.objectId(objectType, target));
        }

        String targetObjectId = Constants.objectId(objectType, target);
        Record thisUser = GlobalLogics.getAccount().getUsers(ctx, userId, userId, "display_name", true).getFirstRecord();

        boolean b = createLike(ctx, userId, targetObjectId);

        L.debug(ctx, "b="+b);
        if (b) {
            String sLike = Constants.getBundleString(device, "platform.like.like");
            if (objectType == Constants.POST_OBJECT) {
                Record this_stream = GlobalLogics.getStream().getPostP(ctx, target, "post_id,source,message");
//                    NotificationSender notif = new StreamLikeNotifSender(this, null);
                String body = sLike;

                commons.sendNotification(ctx, Constants.NTF_MY_STREAM_LIKE,
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(userId),
                        commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), this_stream.getString("source"), this_stream.getString("message")),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), this_stream.getString("source"), this_stream.getString("message")),
                        commons.createArrayNodeFromStrings(body),
                        commons.createArrayNodeFromStrings(body),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId)
                );
            }
            if (objectType == Constants.APK_OBJECT) {
//                    NotificationSender notif = new AppLikeNotifSender(this, null);
                Record mcs = commons.thisTrandsGetApkInfo(ctx, userId, target, "app_name", 1000).getFirstRecord();
                String body = sLike;

                commons.sendNotification(ctx, Constants.NTF_MY_APP_LIKE,
                        commons.createArrayNodeFromStrings(appId),
                        commons.createArrayNodeFromStrings(userId),
                        commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name"), mcs.getString("app_name")),
                        commons.createArrayNodeFromStrings(body),
                        commons.createArrayNodeFromStrings(body),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId)
                );
            }

            if (objectType == Constants.APK_OBJECT) {
                String[] a = target.split("-");
                if (a.length == 1 || a.length == 3) {
                    GlobalLogics.getConversation().createConversationP(ctx, Constants.APK_OBJECT, a[0], Constants.C_APK_LIKE, userId);
                }
                //like in 2 minutes,dont send stream,dont send nail
                //get my lasted like stream
                boolean send = false;
                RecordSet temprec = GlobalLogics.getStream().myTopOneStreamByTarget(ctx, userId, Constants.APK_LIKE_POST, target, "created_time");
                if (temprec.size() <= 0) {
                    send = true;
                } else {
                    long oldTime = temprec.getFirstRecord().getInt("created_time");
                    long datediff = 2 * 60 * 1000; //diff 2 minutes
                    long now = DateUtils.nowMillis();
                    send = (now - datediff > oldTime);
                }
                if (send) {
                    String tempNowAttachments = "[]";
                    //attachments for client

                    GlobalLogics.getStream().autoPost(ctx, userId, Constants.APK_LIKE_POST, sLike, tempNowAttachments, String.valueOf(Constants.APP_TYPE_QIUPU), "", target, "", "", false, Constants.QAPK_FULL_COLUMNS, device, location, true, true, true, "", "", false,Constants.POST_SOURCE_SYSTEM);
//                        String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
//                        sendCommentOrLikeEmail(Constants.APK_OBJECT, userId, user, target, "likes", lang);
                }
            } else if (objectType == Constants.POST_OBJECT) {
                GlobalLogics.getStream().touch(ctx, target);
                //send notify email
//                    String lang = Constants.parseUserAgent(device, "lang").equalsIgnoreCase("US") ? "en" : "zh";
//                    sendCommentOrLikeEmail(Constants.POST_OBJECT, userId, user, target, "likes", lang);
                GlobalLogics.getConversation().createConversationP(ctx, Constants.POST_OBJECT, target, Constants.C_STREAM_LIKE, userId);
            } else if (objectType == Constants.BOOK_OBJECT)//for books
            {

            } else if (objectType == Constants.COMMENT_OBJECT)//for books
            {
                GlobalLogics.getConversation().createConversationP(ctx, Constants.COMMENT_OBJECT, target, Constants.C_COMMENT_LIKE, userId);
            } else if (objectType == Constants.PHOTO_OBJECT)//for photo
            {
                GlobalLogics.getConversation().createConversationP(ctx, Constants.PHOTO_OBJECT, target, Constants.C_PHOTO_LIKE, userId);

                commons.sendNotification(ctx, Constants.NTF_PHOTO_LIKE,
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(userId),
                        commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name")),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name")),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId)
                );
            } else if (objectType == Constants.FILE_OBJECT)//for photo
            {
                GlobalLogics.getConversation().createConversationP(ctx, Constants.FILE_OBJECT, target, Constants.C_FILE_LIKE, userId);

                commons.sendNotification(ctx, Constants.NTF_FILE_LIKE,
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(userId),
                        commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name")),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId, thisUser.getString("display_name")),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId)
                );
            } else if (objectType == Constants.POLL_OBJECT) {//for poll
                GlobalLogics.getConversation().createConversationP(ctx, Constants.POLL_OBJECT, target, Constants.C_POLL_LIKE, userId);
                String title = GlobalLogics.getPoll().getPolls(ctx, target).getFirstRecord().getString("title");
                commons.sendNotification(ctx, Constants.NTF_POLL_LIKE,
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(userId),
                        commons.createArrayNodeFromStrings(title),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(title, target),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target),
                        commons.createArrayNodeFromStrings(target, userId)
                );

            }
        }
        L.traceEndCall(ctx, METHOD);
        return b;
    }

    public boolean unlikeP(Context ctx, String userId, int objectType, String target) {
        final String METHOD = "unlikeP";
        L.traceStartCall(ctx, METHOD, objectType,target);
        if (!GlobalLogics.getAccount().hasUser(ctx, Long.parseLong(userId)))
            throw new ServerException(WutongErrors.USER_NOT_EXISTS, "User '%s' is not exists", userId);

        String targetObjectId = Constants.objectId(objectType, target);
        boolean b = destroyLike(ctx, userId, targetObjectId);
        if (b) {
            if (String.valueOf(objectType).equals(String.valueOf(Constants.APK_OBJECT))) {
                //get stream，update
                Record r = GlobalLogics.getStream().topOneStreamByTarget(ctx, Constants.APK_LIKE_POST, target).getFirstRecord();
                String attach = commons.thisTrandsGetApkInfo(ctx, userId, target, Constants.QAPK_FULL_COLUMNS, 1000).toString(false, false);
                GlobalLogics.getStream().updateAttachment(ctx, r.getString("post_id"), attach);
                String[] a = target.split("-");
                if (a.length == 1 || a.length == 3) {
                    GlobalLogics.getConversation().deleteConversationP(ctx, Constants.APK_OBJECT, a[0], Constants.C_APK_LIKE, Long.parseLong(userId));
                }
            } else if (String.valueOf(objectType).equals(String.valueOf(Constants.POST_OBJECT))) {
                GlobalLogics.getConversation().deleteConversationP(ctx, Constants.POST_OBJECT, target, Constants.C_STREAM_LIKE, Long.parseLong(userId));
            } else if (String.valueOf(objectType).equals(String.valueOf(Constants.COMMENT_OBJECT))) {
                GlobalLogics.getConversation().deleteConversationP(ctx, Constants.COMMENT_OBJECT, target, Constants.C_COMMENT_LIKE, Long.parseLong(userId));
            } else if (String.valueOf(objectType).equals(String.valueOf(Constants.PHOTO_OBJECT))) {
                GlobalLogics.getConversation().deleteConversationP(ctx, Constants.PHOTO_OBJECT, target, Constants.C_PHOTO_LIKE, Long.parseLong(userId));
            }
        }
        L.traceEndCall(ctx, METHOD);
        return b;
    }

    public int getLikeCountP(Context ctx, int objectType, String target) {
        final String METHOD = "getLikeCountP";
        L.traceStartCall(ctx, METHOD, objectType,target);
        String targetObjectId = Constants.objectId(objectType, target);
        int a =  getLikeCount(ctx, targetObjectId);
        L.traceEndCall(ctx, METHOD);
        return a;
    }

    public RecordSet likedUsersP(Context ctx, String userId, int objectType, String target, String cols, int page, int count) {
        final String METHOD = "likedUsersP";
        L.traceStartCall(ctx, METHOD, userId,objectType,target,cols,page,count);

        String targetObjectId = Constants.objectId(objectType, target);
        if (cols.isEmpty() || cols.equals("")) {
            cols = Constants.USER_STANDARD_COLUMNS;
        }
        RecordSet recs = loadLikedUsers(ctx, targetObjectId, page, count);
        RecordSet recordSet =  GlobalLogics.getAccount().getUsers(ctx, userId, recs.joinColumnValues("liker", ","), cols, false);
        L.traceEndCall(ctx, METHOD);
        return recordSet;
    }

    public boolean ifuserLikedP(Context ctx, String userId, String targetId) {
        return ifUserLiked(ctx, userId, targetId);
    }

    public RecordSet getLikedPostsP(Context ctx, String userId, String cols, int objectType, int page, int count) {
        final String METHOD = "getLikedPostsP";
        L.traceStartCall(ctx, METHOD, userId,cols,objectType,page, count);
        RecordSet recs = GlobalLogics.getLike().getLikedPost(ctx, userId, page, count, objectType);

        for (Record rec : recs) {
            rec.put("post_id", rec.getString("target").replace(objectType + ":", ""));
        }
        if (cols.isEmpty() || cols.equals("")) {
            cols = Constants.POST_FULL_COLUMNS;
        }
        RecordSet recordSet = GlobalLogics.getStream().getPosts(ctx, recs.joinColumnValues("post_id", ","), cols);
        return recordSet;
    }
//
//    public int updatedata() {
//        SQLExecutor se = getSqlExecutor();
//        String sql = "select * from user_property where `key`=13 ";
//
//        RecordSet recs_good = se.executeRecordSet(sql, null);
//
//        String user_ids = recs_good.joinColumnValues("user", ",");
//        String sql0 = "delete from user_property2 where user in (" + user_ids + ") and `key`='13' ";
//        se.executeUpdate(sql0);
//
//        for (Record rec : recs_good) {
//            String user_id = rec.getString("user");
//            String _key = rec.getString("key");
//            String sub = rec.getString("sub");
//            String index = rec.getString("index");
//            String updated_time = rec.getString("updated_time");
//            String type = rec.getString("type");
//            String _value = rec.getString("value");
//
//            String sql1 = "insert into user_property2(user,`key`,`sub`,`index`,updated_time,type,`value`) values ('"+user_id+"','"+_key+"','"+sub+"','"+index+"','"+updated_time+"','"+type+"','"+_value+"')";
//            se.executeUpdate(sql1);
//        }
//        return 1;
//    }
//
//    public int updatedata1() {
//            SQLExecutor se = getSqlExecutor();
//            String sql = "SELECT * FROM user_property2 WHERE `key`=13 AND updated_time > 1354862042934";
//            RecordSet recs_bad1 = se.executeRecordSet(sql, null);
//
//             for (Record rec : recs_bad1) {
//
//                String user_id = rec.getString("user");
//                String _key = rec.getString("key");
//                String sub = rec.getString("sub");
//                String index = rec.getString("index");
//                String updated_time = rec.getString("updated_time");
//                String type = rec.getString("type");
//                String _value = rec.getString("value");
//
//                 String sql22 = "SELECT count(*) as count1  FROM user_property2 WHERE user='"+user_id+"' and `key`=13";
//                 RecordSet recs_good = se.executeRecordSet(sql22, null);
//                 if (recs_good.size()>1){
//                     //把这个用户的sub 的 1和3     换个位置
//
//                 }
//            }
//            return 1;
//        }

    private static void separateApkTargetIdsAndOtherTargetIds(String[] targetIds, Collection<String> apkTargetIds, Collection<String> otherTargetIds) {
        for (String targetId : targetIds) {
            if (StringUtils.contains(targetId, ':')
                    && Integer.toString(Constants.APK_OBJECT).equals(StringUtils.substringBefore(targetId, ":"))) {
                if (apkTargetIds != null)
                    apkTargetIds.add(targetId);
            } else {
                if (otherTargetIds != null)
                    otherTargetIds.add(targetId);
            }
        }
    }


    @Override
    public Map<String, Integer> getLikeCounts(Context ctx, String[] targetIds) {
        if (ArrayUtils.isEmpty(targetIds))
            return new HashMap<String, Integer>();

        LinkedHashSet<String> apkTargetIds = new LinkedHashSet<String>();
        LinkedHashSet<String> otherTargetIds = new LinkedHashSet<String>();
        separateApkTargetIdsAndOtherTargetIds(targetIds, apkTargetIds, otherTargetIds);

        // select '4:com.borqs.se-arm-199' as target, count(*) as like_count from like_ where target like '4:com.borqs.se-%'
        // union
        // select target, count(*) as like_count from like_ where target IN ('7:2802055039077282387', '7:2802589130499641847') group by target;
        ArrayList<String> sqls = new ArrayList<String>();
        for (String apkTargetId : apkTargetIds) {
            String targetTypeWithApkPackage = StringUtils.substringBefore(apkTargetId, "-");
            sqls.add(new SQLBuilder.Select()
                    .select(Sql.sqlValue(apkTargetId) + " AS target", "COUNT(*) AS like_count")
                    .from(likeTable)
                    .where("target like ${v(package)}", "package", targetTypeWithApkPackage + "-%")
                            //.groupBy("target")
                    .toString());
        }
        if (!otherTargetIds.isEmpty()) {
            sqls.add(new SQLBuilder.Select()
                    .select("target", "COUNT(*) as like_count")
                    .from(likeTable)
                    .where("target IN (${vjoin(target_ids)})", "target_ids", otherTargetIds)
                    .groupBy("target")
                    .toString());
        }
        String sql = SQLBuilder.union(sqls);
        final HashMap<String, Integer> m = new HashMap<String, Integer>();
        SQLExecutor se = getSqlExecutor();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                m.put(rec.getString("target"), (int) rec.getInt("like_count"));
            }
        });
        CollectionUtils2.fillMissingWithValue(m, targetIds, 0);
        return m;
    }

    @Override
    public Map<String, String> getLikedUserIds(Context ctx, String[] targetIds, int likedUserCount) {
        if (ArrayUtils.isEmpty(targetIds))
            return new HashMap<String, String>();

        LinkedHashSet<String> apkTargetIds = new LinkedHashSet<String>();
        LinkedHashSet<String> otherTargetIds = new LinkedHashSet<String>();
        separateApkTargetIdsAndOtherTargetIds(targetIds, apkTargetIds, otherTargetIds);
        ArrayList<String> sqls = new ArrayList<String>();
        for (String apkTargetId : apkTargetIds) {
            String targetTypeWithApkPackage = StringUtils.substringBefore(apkTargetId, "-");
            sqls.add(new SQLBuilder.Select()
                    .select(Sql.sqlValue(apkTargetId) + " AS target", "liker")
                    .from(likeTable)
                    .where("target like ${v(package)}", "package", targetTypeWithApkPackage + "-%")
                    .orderBy("created_time", "DESC")
                    .limit(likedUserCount)
                    .toString());
        }
        for (String otherTargetId : otherTargetIds) {
            sqls.add(new SQLBuilder.Select()
                    .select(Sql.sqlValue(otherTargetId) + " AS target", "liker")
                    .from(likeTable)
                    .where("target=${v(target)}", "target", otherTargetId)
                    .orderBy("created_time", "DESC")
                    .limit(likedUserCount)
                    .toString());
        }

        String sql = SQLBuilder.union(sqls);
        final HashMap<String, List<String>> m0 = new HashMap<String, List<String>>();
        SQLExecutor se = getSqlExecutor();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                String targetId = rec.getString("target");
                String likerId = rec.getString("liker");
                List<String> likerIds = m0.get(targetId);
                if (likerIds == null) {
                    likerIds = new ArrayList<String>();
                    m0.put(targetId, likerIds);
                }
                likerIds.add(likerId);
            }
        });
        HashMap<String, String> m = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> e : m0.entrySet()) {
            m.put(e.getKey(), StringUtils.join(e.getValue(), ","));
        }
        CollectionUtils2.fillMissingWithValue(m, targetIds, "");
        return m;
    }

    /*
    final String METHOD = "ifUserLiked";
        L.traceStartCall(ctx, METHOD, userId,targetId);
        String[] a1 = StringUtils2.splitArray(StringUtils.trimToEmpty(targetId), ":", true);
        String SQL = "";
        if (a1[0].equals("4")) {
            String[] a2 = StringUtils2.splitArray(StringUtils.trimToEmpty(a1[1]), "-", true);
            SQL = "SELECT ${alias.target} FROM ${table} WHERE ${alias.target} like '%" + a2[0] + "-%' AND ${alias.liker}=${v(user_id)}";
        } else {
            SQL = "SELECT ${alias.target} FROM ${table} WHERE ${alias.target}=${v(target)} AND ${alias.liker}=${v(user_id)}";
        }
        String sql = SQLTemplate.merge(SQL, "alias", likeSchema.getAllAliases(), "table", likeTable,
                "target", targetId, "user_id", userId);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        L.traceEndCall(ctx, METHOD);
        return !rec.isEmpty();
     */
    @Override
    public Map<String, Boolean> ifUserLiked(Context ctx, String userId, String[] targetIds) {
        final String METHOD = "ifUserLiked";
        L.traceStartCall(ctx, METHOD, userId, targetIds);

        if (ArrayUtils.isEmpty(targetIds))
            return new HashMap<String, Boolean>();

        LinkedHashSet<String> apkTargetIds = new LinkedHashSet<String>();
        LinkedHashSet<String> otherTargetIds = new LinkedHashSet<String>();
        separateApkTargetIdsAndOtherTargetIds(targetIds, apkTargetIds, otherTargetIds);

        ArrayList<String> sqls = new ArrayList<String>();
        for (String apkTargetId : apkTargetIds) {
            String targetTypeWithApkPackage = StringUtils.substringBefore(apkTargetId, "-");
            String sub = new SQLBuilder.Select()
                    .select("*")
                    .from(likeTable)
                    .where("liker=${liker_id} AND target like ${v(package)}", "liker_id", userId, "package", targetTypeWithApkPackage + "-%")
                    .toString();
            sqls.add(new SQLBuilder.Select()
                    .select(Sql.sqlValue(apkTargetId) + " AS target", "EXISTS (" + sub + ") AS b")
                    .toString());
        }
        for (String otherTargetId : otherTargetIds) {
            String sub = new SQLBuilder.Select()
                    .select("*")
                    .from(likeTable)
                    .where("liker=${liker_id} AND target = ${v(target)}", "liker_id", userId, "target", otherTargetId)
                    .toString();
            sqls.add(new SQLBuilder.Select()
                    .select(Sql.sqlValue(otherTargetId) + " AS target", "EXISTS (" + sub + ") AS b")
                    .toString());
        }
        String sql = SQLBuilder.union(sqls);

        final HashMap<String, Boolean> m = new HashMap<String, Boolean>();
        SQLExecutor se = getSqlExecutor();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                m.put(rec.getString("target"), rec.getInt("b") == 1);
            }
        });

        CollectionUtils2.fillMissingWithValue(m, targetIds, false);
        L.traceEndCall(ctx, METHOD);
        return m;
    }
}
