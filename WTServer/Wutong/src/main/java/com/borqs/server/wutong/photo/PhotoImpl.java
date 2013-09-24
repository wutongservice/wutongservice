package com.borqs.server.wutong.photo;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.*;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.favorite.FavoriteLogic;
import com.borqs.server.wutong.reportabuse.ReportAbuseLogic;
import com.borqs.server.wutong.tag.TagLogic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PhotoImpl implements PhotoLogic, Initializable {
    Logger L = Logger.getLogger(PhotoImpl.class);
    public final Schema photoSchema = Schema.loadClassPath(PhotoImpl.class, "photo.schema");
    public final Schema albumSchema = Schema.loadClassPath(PhotoImpl.class, "album.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    Configuration conf;
    private StaticFileStorage photoStorage;

    public void init() {
        conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);

        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
    }

    @Override
    public void destroy() {

    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    public static int ALBUM_TYPE_PROFILE = 0;
    public static int ALBUM_TYPE_SHARE_OUT = 1;
    public static int ALBUM_TYPE_COVER = 2;
    public static int ALBUM_TYPE_RECEIVED = 3;
    public static int ALBUM_TYPE_GROUP = 4;
    public static int ALBUM_TYPE_TO_GROUP = 5;     //              我在group里面发图片创建的相册
    public static int ALBUM_TYPE_MY_SYNC = 8;
    public static int ALBUM_TYPE_OTHERS = 9;

    @Override
    public boolean createAlbum(Context ctx, Record record) {
        final String METHOD = "createAlbum";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, record);

        String name = record.getString("title", null);
        if (StringUtils.isEmpty(name))
            throw new ServerException(WutongErrors.SYSTEM_DB_ERROR, "title is null , can't create");

        long n = 0;
            final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";
            String sql = SQLTemplate.merge(SQL,
                    "table", "album",
                    "alias", albumSchema.getAllAliases(),
                    "info", record);

            SQLExecutor se = getSqlExecutor();
            L.op(ctx, "createAlbum");
            n = se.executeUpdate(sql);
            try {
                makPhotoDir(ctx, record.getString("user_id"), record.getString("album_id"));
            } catch (Exception e) {
            }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public boolean createAlbums(Context ctx, RecordSet recs) {
        long n = 0;
        ArrayList<String> sqls = new ArrayList<String>();

        for (Record rec : recs) {
            final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";
            String sql = SQLTemplate.merge(SQL,
                    "table", "album",
                    "alias", albumSchema.getAllAliases(),
                    "info", rec);
            sqls.add(sql);

            try {
                makPhotoDir(ctx, rec.getString("user_id"), rec.getString("album_id"));
            } catch (Exception e) {
            }
        }

        SQLExecutor se = getSqlExecutor();
        n = se.executeUpdate(sqls);
        return n > 0;
    }

    public String makPhotoDir(Context ctx, String viewerId, String album_id) {
        String path = getPhotoPath(ctx, viewerId, album_id);
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        return path;
    }

    @Override
    public String getPhotoPath(Context ctx, String viewerId, String album_id) {
        String a = Record.fromJson(conf.getString("platform.servlet.photoStorage", "")).getString("args");
        String c = StringUtils.substring(a, 1, a.length() - 1).replace("dir=", "");
        String path = c + "/" + viewerId + "/" + album_id + "/";
        return path;
    }

    @Override
    public RecordSet getUserAlbum(Context ctx, String viewerId, String userId) {       //0 open 1 only me 2 friend open
        //1,我看自己  2，我看别人 3，我看圈子
        final String METHOD = "getUserAlbum";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userId);
        String sql10 = "";
        if (viewerId.equals(userId)) {
            sql10 = "SELECT * from album WHERE user_id = '" + userId + "' and destroyed_time=0 order by album_type,created_time";
        } else if (!viewerId.equals(userId) && userId.length() < Constants.USER_ID_MAX_LEN) {
            sql10 = "SELECT * from album WHERE user_id = '" + userId + "' and destroyed_time=0 and album_type in (0,1,2,5,9) order by album_type,created_time";
        } else if (!viewerId.equals(userId) && userId.length() > Constants.USER_ID_MAX_LEN) {
            sql10 = "SELECT * from album WHERE user_id = '" + userId + "' and destroyed_time=0 and album_type in (0,4) order by album_type,created_time";
        }
        //        final String PLATFORM_SQL_ERROR = "SELECT * from album WHERE user_id = '" + userId + "' order by album_type,created_time";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql10, null);

        if (rs.size() <= 0)
            return new RecordSet();

        boolean isFriend = false;
        if (!viewerId.equals("") && !userId.equals("") && !viewerId.equals("0") && !userId.equals("0")) {
            if (!viewerId.equals(userId) && userId.length() < Constants.USER_ID_MAX_LEN && userId.length() > 6) {
                //groupID
                isFriend = GlobalLogics.getGroup().hasRight(ctx, Long.parseLong(userId), Long.parseLong(viewerId), Constants.ROLE_MEMBER);
            } else {
                String sql0 = "select * from friend where user='" + userId + "' and friend='" + viewerId + "'" +
                        " AND circle<>" + Constants.BLOCKED_CIRCLE + ""
                        + " AND reason<>" + Constants.FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                        + " AND reason<>" + Constants.FRIEND_REASON_DEFAULT_DELETE + " ";
                RecordSet rs1 = se.executeRecordSet(sql0, null);
                isFriend = rs1.size() > 0;
            }

        }

        for (int i = rs.size() - 1; i >= 0; i--) {
            if (!viewerId.equals(userId)) {
                if (rs.get(i).getInt("privacy", 0) == 1) {      //如果设置的仅自己可以见，那么排除
                    rs.remove(i);
                }
                if (rs.get(i).getInt("privacy", 0) == 2) {      //如果设置的仅好友可以见，那么排除
                    if (!isFriend)
                        rs.remove(i);
                }
            }
        }
        if (rs.size() <= 0)
            return new RecordSet();
        L.debug(ctx,"album/all:rs=" + rs.toString());
        for (Record rc : rs) {
            rc.put("photo_count", getAlbumSize(ctx, viewerId, rc.getString("album_id")));
        }
        L.debug(ctx,"album/all:rs new=" + rs.toString());

        for (int i = rs.size() - 1; i >= 0; i--) {
            if (rs.get(i).getInt("photo_count") <= 0)
                rs.remove(i);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rs;
    }

    @Override
    public RecordSet getAlbumPhotos(Context ctx, String viewerId, String album_id, int page, int count) {
        final String METHOD = "getAlbumPhotos";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, album_id, page, count);
        SQLExecutor se = getSqlExecutor();
        Record r = se.executeRecord("select album_type,user_id from album where album_id='" + album_id + "' and destroyed_time=0", null);
        int album_type = 0;
        if (!r.isEmpty()) {
            album_type = (int) r.getInt("album_type");
        }

        RecordSet out_recs = new RecordSet();
        String sql = "";
        if (album_type == ALBUM_TYPE_PROFILE || album_type == ALBUM_TYPE_COVER || album_type == ALBUM_TYPE_RECEIVED || album_type == ALBUM_TYPE_MY_SYNC || album_type == ALBUM_TYPE_OTHERS) {
            sql = "SELECT * from photo WHERE destroyed_time=0 and album_id = '" + album_id + "' order by created_time desc " + SQLUtils.pageToLimit(page, count);
            out_recs = se.executeRecordSet(sql, null);
        }
        if (album_type == ALBUM_TYPE_SHARE_OUT || album_type == ALBUM_TYPE_GROUP || album_type == ALBUM_TYPE_TO_GROUP) {
            if (viewerId.equals("") || viewerId.equals("0")) {
                sql = "SELECT * from photo WHERE destroyed_time=0 and album_id = '" + album_id + "'" +
                        " and stream_id<>0" +
                        " and stream_id in (select post_id from stream where destroyed_time=0 and privince=0)  order by created_time desc " + SQLUtils.pageToLimit(page, count);
                out_recs = se.executeRecordSet(sql, null);
            } else {
                //先看我在哪些group_id里面
                String sql_group = "select distinct(group_id) from group_members where member='" + viewerId + "' and destroyed_time=0";
                RecordSet recs_group = se.executeRecordSet(sql_group, null);
                List<String> gl = StringUtils2.splitList(recs_group.joinColumnValues("group_id", ","), ",", true);
                sql = "select p.*,p.stream_id,s.mentions,s.source,s.add_to,s.privince from photo p inner join stream s on p.stream_id=s.post_id" +
                        " where p.album_id='" + album_id + "' and p.destroyed_time=0 and s.destroyed_time=0 and p.stream_id<>0 order by p.created_time desc";
                out_recs = se.executeRecordSet(sql, null);
                if (album_type == ALBUM_TYPE_GROUP || album_type == ALBUM_TYPE_TO_GROUP) {
                    for (Record tmp_rec : out_recs) {
                        if (tmp_rec.getString("mentions").length() >0)
                            tmp_rec.put("user_id", r.getString("user_id"));
                    }
                }

                for (int j = out_recs.size() - 1; j >= 0; j--) {
                    boolean v = false;
                    if (out_recs.get(j).getInt("privince") == 1) {
                        if (out_recs.get(j).getString("source").equals(viewerId)) {
                            v = true;
                        }
                        if (!v) {
                            String mentions = out_recs.get(j).getString("mentions");
                            List<String> ml = StringUtils2.splitList(mentions, ",", true);
                            for (String ml0 : ml) {
                                if (ml0.length() < Constants.USER_ID_MAX_LEN && ml0.length() > 6) {
                                    if (gl.contains(ml0)) {
                                        v = true;
                                        break;
                                    }
                                } else if (ml0.length() < 6) {
                                    if (ml0.equals(viewerId)) {
                                        v = true;
                                        break;
                                    }
                                }

//                                if (ml0.length() < Constants.USER_ID_MAX_LEN) {  //   检查我在不在mentions里面
//                                    if (ml0.equals(viewerId)) {
//                                        v = true;
//                                        break;
//                                    }
//                                } else {     //检查我参加的圈子里面有没有这个
//                                    if (gl.contains(ml0)) {
//                                        v = true;
//                                        break;
//                                    }
//                                }
                            }
                        }

                        if (!v) {
                            String add_to = out_recs.get(j).getString("add_to");
                            List<String> al = StringUtils2.splitList(add_to, ",", true);
                            for (String al0 : al) {
                                if (al0.length() < Constants.USER_ID_MAX_LEN && al0.length() > 6) {
                                    if (gl.contains(al0)) {
                                        v = true;
                                        break;
                                    }
                                } else if (al0.length() < 6) {
                                    if (al0.equals(viewerId)) {
                                        v = true;
                                        break;
                                    }
                                }


//                                if (al0.length() < Constants.USER_ID_MAX_LEN) {  //   检查我在不在add_to里面
//                                    if (al0.equals(viewerId)) {
//                                        v = true;
//                                        break;
//                                    }
//                                } else {     //检查我参加的圈子里面有没有这个
//                                    if (gl.contains(al0)) {
//                                        v = true;
//                                        break;
//                                    }
//                                }
                            }
                        }
                    } else {    //privince=0直接就是true
                        v = true;
                    }
                    if (!v)  //删除不符合的数据
                        out_recs.remove(j);
                }
                out_recs.sliceByPage(page, count);
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        FavoriteLogic favoriteLogic = GlobalLogics.getFavorite();
        TagLogic tagLogic = GlobalLogics.getTag();
        try {
            ReportAbuseLogic reportAbuseLogic = GlobalLogics.getReportAbuse();
            for (Record moveR : out_recs) {
                int reportAbuseCount = reportAbuseLogic.getReportAbuseCount(ctx, Constants.PHOTO_OBJECT, moveR.getString("photo_id"), Constants.APP_TYPE_BPC);
                if (reportAbuseCount >= Constants.REPORT_ABUSE_COUNT)
                    moveR.put("caption", "###DELETE###");
                if (reportAbuseLogic.iHaveReport(ctx, viewerId, Constants.PHOTO_OBJECT, moveR.getString("photo_id"), Constants.APP_TYPE_BPC) >= 1)
                    moveR.put("caption", "###DELETE###");
            }
            for (int jj = out_recs.size() - 1; jj >= 0; jj--) {
                Record p = out_recs.get(jj);
                if (p.getString("caption").equals("###DELETE###")) {
                    out_recs.remove(jj);
                }
            }
        } catch (Exception e) {
            L.debug(ctx,"get photo filter report abuse error");
        }

        for (Record rec :out_recs){
            Record t = tagLogic.findUserTagByTarget(ctx,"", rec.getString("photo_id"), String.valueOf(Constants.PHOTO_OBJECT), 0, 1).getFirstRecord();
            if (t.isEmpty()) {
                rec.put("taged", false);
                rec.put("tag_content", "");
            } else {
                rec.put("taged", true);
                rec.put("tag_content", t.getString("tag"));
            }
            rec.put("favorited",favoriteLogic.getIFavorited(ctx,viewerId,String.valueOf(Constants.PHOTO_OBJECT) , rec.getString("photo_id")));
        }
        return out_recs;
    }

    @Override
    public Record getAlbumOriginal(Context ctx, String album_id) {
        final String METHOD = "getAlbumOriginal";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, album_id);
        SQLExecutor se = getSqlExecutor();
        Record r = se.executeRecord("select * from album where destroyed_time=0 and album_id='" + album_id + "'", null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return r;
    }

    @Override
    public RecordSet getPhotosIncludeMe(Context ctx, String viewerId, String user_id, int page, int count) {
        final String METHOD = "getPhotosIncludeMe";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, user_id, page, count);
        final String SQL = "SELECT * FROM photo WHERE destroyed_time=0 and  (instr(concat(',',tag_ids,','),concat(','," + user_id + ",','))>0)" +
                " and album_id in (select album_id from album where privacy=0)  ORDER BY ${alias.created_time} ${asc} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", photoSchema.getAllAliases()},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", "DESC"},
        });
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        for (Record rec : recs) {
            String sql0 = "select album_type from album where destroyed_time=0 and album_id='" + rec.getString("album_id") + "'";
            Record album = se.executeRecord(sql0, null);
            boolean visibility = true;
            if (album.getInt("album_type") == ALBUM_TYPE_SHARE_OUT || album.getInt("album_type") == ALBUM_TYPE_GROUP || album.getInt("album_type") == ALBUM_TYPE_TO_GROUP) {
                String sql00 = "select source,mentions,privince,add_to from stream where post_id='" + rec.getString("stream_id") + "'";
                Record stream = se.executeRecord(sql00.toString(), null);
                if (stream.getString("privince").equals("1")) {
                    if (!stream.getString("source").equals(viewerId)) {
                        List<String> u_list = new ArrayList<String>();
                        if (stream.getString("source").length() > 0) {
                            String s[] = StringUtils.split(stream.getString("source"), ",");
                            for (int i = 0; i <= s.length - 1; i++) {
                                u_list.add(s[i]);
                            }
                            String s1[] = StringUtils.split(stream.getString("add_to"), ",");
                            for (int i = 0; i <= s1.length - 1; i++) {
                                u_list.add(s1[i]);
                            }
                            if (!u_list.contains(viewerId))
                                visibility = false;
                        }
                    }
                }
            }
            rec.put("visibility", visibility);
        }

        for (int j = recs.size() - 1; j >= 0; j--) {
            if (recs.get(j).getBoolean("visibility", true) == false)
                recs.remove(j);
        }
        recs.removeColumns("visibility");
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        FavoriteLogic favoriteLogic = GlobalLogics.getFavorite();
        TagLogic tagLogic = GlobalLogics.getTag();
        for (Record rec : recs) {
            Record t = tagLogic.findUserTagByTarget(ctx, "",rec.getString("photo_id"), String.valueOf(Constants.PHOTO_OBJECT), 0, 1).getFirstRecord();
            if (t.isEmpty()) {
                rec.put("taged", false);
                rec.put("tag_content", "");
            } else {
                rec.put("taged", true);
                rec.put("tag_content", t.getString("tag"));
            }
            rec.put("favorited", favoriteLogic.getIFavorited(ctx, viewerId, String.valueOf(Constants.PHOTO_OBJECT), rec.getString("photo_id")));
        }
        return recs;
    }


    @Override
    public boolean isAlbumExist(Context ctx, String album_id) {
        final String METHOD = "isAlbumExist";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, album_id);
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT album_id from album WHERE destroyed_time=0 and album_id = " + "'" + album_id + "'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;
    }

    public boolean isMyAlbumExist(String userId, int album_type, String album_name) {
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT album_id from album WHERE destroyed_time=0 and title = '" + album_name + "' and album_type=" + album_type + " and user_id='" + userId + "'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        return true;
    }

    public Map<String, Boolean> isUsersAlbumExist(List<String> userIds, int album_type, String album_name) {
        LinkedHashMap<String, Boolean> map = new LinkedHashMap<String, Boolean>();
        if (CollectionUtils.isNotEmpty(userIds)) {
            for (String userId : userIds) {
                map.put(userId, false);
            }
            SQLExecutor se = getSqlExecutor();
            final String sql = "SELECT user_id, count(album_id) as count from album WHERE destroyed_time=0 and title = '" + album_name + "' and album_type=" + album_type
                        + " and user_id in (" + SQLUtils.valueJoin(",", userIds.toArray(new String[userIds.size()])) + ") group by user_id";

            RecordSet recs = se.executeRecordSet(sql, null);
            for (Record rec : recs) {
                map.put(rec.getString("user_id"), rec.getInt("count", 0L) > 0L);
            }
        }

        return map;
    }

    public boolean isMyAlbumExistType(String userId, int album_type) {
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT album_id from album WHERE destroyed_time=0 and album_type=" + album_type + " and user_id='" + userId + "'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        return true;
    }

    public Map<String, Boolean> isUsersAlbumExistType(List<String> userIds, int album_type) {
        LinkedHashMap<String, Boolean> map = new LinkedHashMap<String, Boolean>();
        if (CollectionUtils.isNotEmpty(userIds)) {
            for (String userId : userIds) {
                map.put(userId, false);
            }
            SQLExecutor se = getSqlExecutor();
            final String sql = "SELECT user_id, count(album_id) as count from album WHERE destroyed_time=0 and album_type=" + album_type
                    + " and user_id in (" + SQLUtils.valueJoin(",", userIds.toArray(new String[userIds.size()])) + ") group by user_id";

            RecordSet recs = se.executeRecordSet(sql, null);
            for (Record rec : recs) {
                map.put(rec.getString("user_id"), rec.getInt("count", 0L) > 0L);
            }
        }

        return map;
    }


    @Override
    public boolean updateAlbum(Context ctx, String album_id, Record rc) {
        final String METHOD = "updateAlbum";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, album_id, rc);
        rc.putMissing("updated_time", DateUtils.nowMillis());
        String sql = new SQLBuilder.Update(albumSchema).update("album").values(rc)
                .where("album_id = " + "'" + album_id + "'").toString();
        SQLExecutor se = getSqlExecutor();
        L.op(ctx, "updateAlbum");
        long n = se.executeUpdate(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public boolean deleteAlbumById(Context ctx, String userId, String album_id, String bucketName_photo_key, StaticFileStorage photoStorage) {
        final String METHOD = "deleteAlbumById";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, album_id, userId, bucketName_photo_key, photoStorage);
        SQLExecutor se = getSqlExecutor();
        String sql0 = "select photo_id from photo where destroyed_time=0 and  album_id='" + album_id + "'";
        RecordSet rs = se.executeRecordSet(sql0.toString(), null);
        String photo_ids = rs.joinColumnValues("photo_id", ",");
        deletePhotoById(ctx, userId, photo_ids, false, bucketName_photo_key, photoStorage);
        // delete photo DB
        String sql = "update album set destroyed_time='" + DateUtils.nowMillis() + "',updated_time='" + DateUtils.nowMillis() + "' WHERE album_id = " + "'" + album_id + "'";
        L.op(ctx, "deleteAlbumById");
        long n = se.executeUpdate(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public boolean saveUploadPhoto(Context ctx, Record record) {
        final String METHOD = "saveUploadPhoto";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, record);
        String album_id = record.getString("album_id", null);
        if (null == album_id)
            throw new ServerException(WutongErrors.PHOTO_ALBUM_NOT_EXISTS, "no album , can't save");

        final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(SQL,
                "table", "photo",
                "alias", photoSchema.getAllAliases(),
                "info", record);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);

        String sql1 = "update album set updated_time='" + DateUtils.nowMillis() + "' WHERE album_id = " + "'" + album_id + "'";

        L.op(ctx, "saveUploadPhoto");
        n = se.executeUpdate(sql1);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public boolean saveUploadPhotos(RecordSet recs) {
        ArrayList<String> sqls = new ArrayList<String>();
        final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";

        for (Record record : recs) {
            String album_id = record.getString("album_id", null);
            if (null == album_id)
                throw new ServerException(WutongErrors.PHOTO_ALBUM_NOT_EXISTS, "no album , can't save");

            String sql = SQLTemplate.merge(SQL,
                    "table", "photo",
                    "alias", photoSchema.getAllAliases(),
                    "info", record);
            sqls.add(sql);
            sqls.add("update album set updated_time='" + DateUtils.nowMillis() + "' WHERE album_id = " + "'" + album_id + "'");
        }

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sqls);

        return n > 0;
    }

    public int getAlbumSize(Context ctx, String viewerId, String album_id) {
        return getAlbumPhotos(ctx, viewerId, album_id, 0, 5000).size();
    }

    public String getLatestPhotoId(Context ctx, String viewerId, String album_id) {
        return getLatestPhoto(ctx, viewerId, album_id).getString("photo_id");
    }

    @Override
    public Record getLatestPhoto(Context ctx, String viewerId, String album_id) {
        final String METHOD = "getLatestPhoto";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, album_id);
        RecordSet recs = getAlbumPhotos(ctx, viewerId, album_id, 0, 1);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs.getFirstRecord();
    }

    @Override
    public Record getAlbumById(Context ctx, String viewerId, String userId, String album_id) {
        final String METHOD = "getAlbumById";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, userId, album_id);
        String sql = "SELECT * from album WHERE destroyed_time=0 and  album_id = '" + album_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        if (rec.isEmpty())
            return new Record();
        boolean returnFlag = true;
        boolean isFriend = false;
        if (!viewerId.equals("") && !userId.equals("") && !viewerId.equals("0") && !userId.equals("0")) {
            String sql0 = "select * from friend where user='" + userId + "' and friend='" + viewerId + "'" +
                    " AND circle<>" + Constants.BLOCKED_CIRCLE + ""
                    + " AND reason<>" + Constants.FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                    + " AND reason<>" + Constants.FRIEND_REASON_DEFAULT_DELETE + " ";
            RecordSet rs1 = se.executeRecordSet(sql0, null);
            isFriend = rs1.size() > 0;
        }

        int privacy = (int) rec.getInt("privacy", 0);
        if (!viewerId.equals(userId)) {
            if (privacy == 1)
                returnFlag = false;
            if (privacy == 2) {
                if (!isFriend)
                    returnFlag = false;
            }
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        if (returnFlag) {
            rec.put("photo_count", getAlbumSize(ctx, viewerId, album_id));
            return rec;
        } else {
            return new Record();
        }
    }

    @Override
    public RecordSet getPhotoByIds(Context ctx, String photo_ids) {
        final String METHOD = "getPhotoByIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, photo_ids);
        String sql = "SELECT * from photo WHERE destroyed_time=0 and photo_id in ('" + photo_ids + "') group by photo_id";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);

        for (Record r : recs) {
//            String album_id = r.getString("album_id", null);
//            if (null != album_id) {
//                sql = "SELECT * from album WHERE destroyed_time=0 and album_id = " + "'" + album_id + "'";
//                Record rec1 = se.executeRecord(sql.toString(), null);
//                if (null != rec1 && rec1.isEmpty())
//                    throw new ServerException(WutongErrors.PHOTO_ALBUM_NOT_EXISTS, "no such album, query error");
//            }
            if (r.getString("tag").length() <= 2)
                r.put("tag", new RecordSet());
            r.remove("album_id");
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        FavoriteLogic favoriteLogic = GlobalLogics.getFavorite();
        TagLogic tagLogic = GlobalLogics.getTag();
        for (Record rec : recs) {
            Record t = tagLogic.findUserTagByTarget(ctx,"", rec.getString("photo_id"), String.valueOf(Constants.PHOTO_OBJECT), 0, 1).getFirstRecord();
            if (t.isEmpty()) {
                rec.put("taged", false);
                rec.put("tag_content", "");
            } else {
                rec.put("taged", true);
                rec.put("tag_content", t.getString("tag"));
            }
            rec.put("favorited", favoriteLogic.getIFavorited(ctx, ctx.getViewerIdString(), String.valueOf(Constants.PHOTO_OBJECT) , rec.getString("photo_id")));
        }
        return recs;
    }

    @Override
    public RecordSet getAllPhotos(Context ctx, String user_id) {
        final String METHOD = "getAllPhotos";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, user_id);
        String sql = "";
        if (user_id.equals("0")) {
            sql = "SELECT * from photo where destroyed_time=0 order by created_time desc ";
        } else {
            sql = "SELECT * from photo where user_id = '" + user_id + "' and destroyed_time=0 order by created_time desc ";
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        FavoriteLogic favoriteLogic = GlobalLogics.getFavorite();
        TagLogic tagLogic = GlobalLogics.getTag();
        for (Record rec : recs) {
            Record t = tagLogic.findUserTagByTarget(ctx, rec.getString("photo_id"),"", String.valueOf(Constants.PHOTO_OBJECT), 0, 1).getFirstRecord();
            if (t.isEmpty()) {
                rec.put("taged", false);
                rec.put("tag_content", "");
            } else {
                rec.put("taged", true);
                rec.put("tag_content", t.getString("tag"));
            }
            rec.put("favorited", favoriteLogic.getIFavorited(ctx, ctx.getViewerIdString(), String.valueOf(Constants.PHOTO_OBJECT), rec.getString("photo_id")));
        }
        return recs;
    }

    @Override
    public boolean updatePhoto(Context ctx, String photo_id, Record rc) {
        final String METHOD = "getAllPhotos";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, photo_id, rc);
        String sql = "";
        SQLExecutor se = getSqlExecutor();
        long n = 0;
        if (rc.has("caption")) {
            sql = "update photo set caption='" + rc.getString("caption") + "',updated_time='" + DateUtils.nowMillis() + "' where photo_id = " + "'" + photo_id + "'";
            n = se.executeUpdate(sql);
        }
        if (rc.has("location")) {
            sql = "update photo set location='" + rc.getString("location") + "',updated_time='" + DateUtils.nowMillis() + "' where photo_id = " + "'" + photo_id + "'";
            n = se.executeUpdate(sql);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public boolean updatePhotoStreamId(Context ctx, String stream_id, List<String> photo_ids) {
        final String METHOD = "getAllPhotos";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, stream_id, photo_ids);
        String pids = StringUtils.join(photo_ids, ",");
        String sql = "update photo set stream_id='" + stream_id + "' where photo_id in (" + pids + ") and stream_id=0";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public boolean deletePhotoById(Context ctx, String viewerId, String photo_ids, boolean delete_all, String bucketName_photo_key, StaticFileStorage sfs) {
        final String METHOD = "deletePhotoById";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, photo_ids, delete_all, bucketName_photo_key, sfs);
        SQLExecutor se = getSqlExecutor();

        //delete the storage files

        //先看谁在删除，源，还是接收者


        String sql0 = "SELECT photo_id,user_id,img_middle,img_big,img_small,img_original,stream_id,album_id from photo WHERE destroyed_time=0 and photo_id IN (" + photo_ids + ")";
        RecordSet rs = se.executeRecordSet(sql0.toString(), null);
        long n = 0;
        if (rs.size() > 0) {
            for (Record rec : rs) {
                if (rec.isEmpty())
                    continue;
                String img_middle = rec.getString("img_middle");
                String o_user_id = StringUtils.split(img_middle, "_")[0];
                String sql01 = "";
                if (viewerId.equals(o_user_id) && delete_all) {
                    sql01 = "update photo set destroyed_time='" + DateUtils.nowMillis() + "' WHERE photo_id='" + rec.getString("photo_id") + "'";
                } else {
                    sql01 = "update photo set destroyed_time='" + DateUtils.nowMillis() + "' WHERE photo_id='" + rec.getString("photo_id") + "' and user_id='" + viewerId + "'";
                }
                n = se.executeUpdate(sql01);
                if (rec.getInt("stream_id") != 0) {
                    String sql02 = "update stream set updated_time='" + DateUtils.nowMillis() + "' where post_id='" + rec.getInt("stream_id") + "'";
                    se.executeUpdate(sql02);
                }
                n += 1;
                //                try {
                //                    String img_middle = String.format(bucketName_photo_key, rec.getString("img_middle"));
                //                    String img_big = String.format(bucketName_photo_key, rec.getString("img_big"));
                //                    String img_small = String.format(bucketName_photo_key, rec.getString("img_small"));
                //                    String img_original = String.format(bucketName_photo_key, rec.getString("img_original"));
                //                    String img_thumbnail = String.format(bucketName_photo_key, rec.getString("img_small").replace("S", "T"));
                //
                //                    SFSUtils.deleteObjectFromOSS(sfs, img_middle);
                //                    SFSUtils.deleteObjectFromOSS(sfs, img_big);
                //                    SFSUtils.deleteObjectFromOSS(sfs, img_small);
                //                    SFSUtils.deleteObjectFromOSS(sfs, img_original);
                //                    SFSUtils.deleteObjectFromOSS(sfs, img_thumbnail);
                //                } catch (Exception e) {
                //                }
            }

            String album_ids = rs.joinColumnValues("album_id", ",");
            L.op(ctx, "deletePhotoById");
            if (album_ids.length() > 0)
                se.executeUpdate("update album set updated_time='" + DateUtils.nowMillis() + "' WHERE album_id in (" + album_ids + ")");
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public String getAlbum(Context ctx, String userId, int album_type, String albumName) {
        final String METHOD = "getByUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, album_type, albumName);

        boolean can_create = true;
        if (album_type == ALBUM_TYPE_OTHERS || album_type == ALBUM_TYPE_TO_GROUP) {
            if (isMyAlbumExist(userId, album_type, albumName))
                can_create = false;
        } else {
            if (isMyAlbumExistType(userId, album_type))
                can_create = false;
        }

        if (can_create)
            createUserAlbum(ctx, userId, album_type, albumName);

        String sql = "";
        if (album_type == ALBUM_TYPE_OTHERS || album_type == ALBUM_TYPE_TO_GROUP) {
            sql = "SELECT album_id from album WHERE destroyed_time=0 and user_id = '" + userId + "' and album_type=" + album_type + " AND title = '" + albumName + "' ORDER BY created_time ";
        } else {
            sql = "SELECT album_id from album WHERE destroyed_time=0 and user_id = '" + userId + "' and album_type=" + album_type + " ORDER BY created_time ";
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql.toString(), null);

        if (rec.size() > 0) {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return rec.getFirstRecord().getString("album_id");
        } else
            throw new ServerException(WutongErrors.PHOTO_ALBUM_NOT_EXISTS, "can't get album:" + albumName);
    }

    @Override
    public Map<String, String> getAlbums(Context ctx, List<String> userIds, int albumType, String albumName) {
        Map<String, Boolean> map;
        if (albumType == ALBUM_TYPE_OTHERS || albumType == ALBUM_TYPE_TO_GROUP) {
                map = isUsersAlbumExist(userIds, albumType, albumName);
        } else {
                map = isUsersAlbumExistType(userIds, albumType);
        }

        ArrayList<String> l = new ArrayList<String>();
        for (String userId : userIds) {
            if (!map.get(userId))
                l.add(userId);
        }
        createUsersAlbum(ctx, l, albumType, albumName);

        String sql = "";
        String con = SQLUtils.valueJoin(",", userIds.toArray(new String[userIds.size()]));
        if (albumType == ALBUM_TYPE_OTHERS || albumType == ALBUM_TYPE_TO_GROUP) {
            sql = "SELECT user_id, album_id from album WHERE destroyed_time=0 and user_id in (" + con + ") and album_type=" + albumType + " AND title = '" + albumName + "' ORDER BY created_time ";
        } else {
            sql = "SELECT user_id, album_id from album WHERE destroyed_time=0 and user_id in (" + con + ") and album_type=" + albumType + " ORDER BY created_time ";
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        for (Record rec : recs) {
            result.put(rec.getString("user_id"), rec.getString("album_id"));
        }

        return result;
    }

    @Override
    public Record tagPhoto(Context ctx, String photo_id, int top, int left, int frame_width, int frame_height, String tagUserId, String tagText, boolean addTag) {
        final String METHOD = "tagPhoto";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, photo_id, top, left, frame_height, frame_width, tagUserId, tagText, addTag);
        Record old_photo = getPhotoByIds(ctx, photo_id).getFirstRecord();

        List<String> oldFaceIds = StringUtils2.splitList(old_photo.getString("tag_ids"), ",", true);
        RecordSet tag_recs = new RecordSet();
        if (!old_photo.getString("tag").equals("")) {
            tag_recs = RecordSet.fromJson(old_photo.getString("tag"));
        }

        if (addTag) {
            int flag = 0;
            for (Record r : tag_recs) {
                if (r.getString("user_id").equals(tagUserId)) {
                    flag += 1;
                    break;
                }
            }
            if (flag == 0) {
                Record rec = new Record();
                rec.put("top", top);
                rec.put("left", left);
                rec.put("frame_width", frame_width);
                rec.put("frame_height", frame_height);
                rec.put("user_id", tagUserId);
                rec.put("tag_text", tagText);
                tag_recs.add(rec);             //add json
                oldFaceIds.add(tagUserId);     //add id
            }
        } else {
            for (int i = oldFaceIds.size() - 1; i >= 0; i--) {
                String userId = oldFaceIds.get(i);
                if (userId.equals(tagUserId))
                    oldFaceIds.remove(i);
            }
            for (int j = tag_recs.size() - 1; j >= 0; j--) {
                if (tag_recs.get(j).getString("user_id").equals(tagUserId)) {
                    tag_recs.remove(j);
                }
            }
        }
        String sql = "update photo set tag='" + tag_recs.toString() + "',updated_time='" + DateUtils.nowMillis() + "',tag_ids = '" + StringUtils.join(oldFaceIds, ",") + "' where photo_id = " + "'" + photo_id + "'";
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql);
        old_photo.put("tag", tag_recs);
        old_photo.put("tag_ids", StringUtils.join(oldFaceIds, ","));
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return old_photo;
    }

    private boolean createUserAlbum(Context ctx, String userId, int album_type, String albumName) {
        final String METHOD = "createUserAlbum";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, album_type, albumName);
        Record rc = new Record();
        String album_id = Long.toString(RandomUtils.generateId());
        long uploaded_time = DateUtils.nowMillis();

        rc.put("album_id", album_id);
        rc.put("user_id", userId);
        rc.put("album_type", album_type);
        rc.put("title", albumName);
        rc.put("summary", "");
        rc.put("created_time", uploaded_time);
        if (album_type == ALBUM_TYPE_RECEIVED || album_type == ALBUM_TYPE_MY_SYNC) {
            rc.put("privacy", 1);
        } else {
            rc.put("privacy", 0);
        }
        L.op(ctx, "createUserAlbum");
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return createAlbum(ctx, rc);
    }

    private boolean createUsersAlbum(Context ctx, List<String> userIds, int album_type, String albumName) {
        final String METHOD = "createUserAlbum";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userIds, album_type, albumName);
        RecordSet recs = new RecordSet();
        for (String userId : userIds) {
            Record rc = new Record();
            String album_id = Long.toString(RandomUtils.generateId());
            long uploaded_time = DateUtils.nowMillis();

            rc.put("album_id", album_id);
            rc.put("user_id", userId);
            rc.put("album_type", album_type);
            rc.put("title", albumName);
            rc.put("summary", "");
            rc.put("created_time", uploaded_time);
            if (album_type == ALBUM_TYPE_RECEIVED || album_type == ALBUM_TYPE_MY_SYNC) {
                rc.put("privacy", 1);
            } else {
                rc.put("privacy", 0);
            }
            recs.add(rc);
        }

        L.op(ctx, "createUserAlbum");
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return createAlbums(ctx, recs);
    }

    private boolean isUserAlbumExist(String userId, int album_type) {
        String sql = "SELECT album_id from album WHERE destroyed_time=0 and user_id = " + "'" + userId + "' AND album_type = '" + album_type + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        return !rec.isEmpty();
    }

    public String genPhotoId(String userId) {
        return Long.toString(RandomUtils.generateId());
    }

    @Override
    public boolean updatePhotoStreamId(Context ctx, int album_type, String asc) {

        final String METHOD = "updatePhotoStreamId";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, album_type, asc);
        String sql = "SELECT img_small,photo_id from photo where destroyed_time=0 and album_id in (select album_id from album where destroyed_time=0 and album_type='" + album_type + "') order by created_time " + asc + " ";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        for (Record rec : recs) {
            String img_small = rec.getString("img_small");
            String sql0 = "select post_id from stream where type=2 and attachments like '%" + img_small + "%'";
            RecordSet recs0 = se.executeRecordSet(sql0.toString(), null);
            if (recs0.size() > 0) {
                Record rec0 = recs0.getFirstRecord();
                if (!rec0.isEmpty()) {
                    String sql1 = "update photo set stream_id='" + rec0.getString("post_id") + "' where photo_id='" + rec.getString("photo_id") + "'";
                    se.executeUpdate(sql1);
                }
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;
    }

    @Override
    public void saveUploadPhoto(Context ctx, FileItem fileItem, String file, String path, Record record) {
        final String METHOD = "saveUploadPhoto";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, fileItem, file, record);
        int width, height, sWidth = 0, sHeight = 0, mWidth = 0, mHeight = 0, tHeight = 0, tWidth = 0;
        try {
            long n = fileItem.getSize();
            String fileName = fileItem.getName().substring(fileItem.getName().lastIndexOf("\\") + 1, fileItem.getName().length());
            String expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
            if (expName.equalsIgnoreCase("gif")) {
                Configuration conf = GlobalConfig.get();
                if (!(photoStorage instanceof OssSFS)) {
                    Record photoStoragePath = Record.fromJson(conf.getString("platform.servlet.photoStorage", ""));
                    Record r1 = new Record();
                    r1.put("dir", path);
                    Record r2 = new Record();
                    r2.put("class", photoStoragePath.getString("class"));
                    r2.put("args", r1);
                    photoStorage = (StaticFileStorage) ClassUtils2.newInstance(r2.toString(false, false));
                }
                String lfn = file + "_L." + expName;
                String ofn = file + "_O." + expName;
                String sfn = file + "_S." + expName;
                String tfn = file + "_T." + expName;
                if (photoStorage instanceof OssSFS) {
                    lfn = "media/photo/" + lfn;
                    ofn = "media/photo/" + ofn;
                    sfn = "media/photo/" + sfn;
                    tfn = "media/photo/" + tfn;
                }
                SFSUtils.saveUpload(fileItem, photoStorage, lfn);
                SFSUtils.saveUpload(fileItem, photoStorage, ofn);
                SFSUtils.saveUpload(fileItem, photoStorage, sfn);
                SFSUtils.saveUpload(fileItem, photoStorage, tfn);
            } else {
                String rotateFile = SFSUtils.revertPhoto(fileItem, expName, record);
                File fileTmp = new File(rotateFile);


                BufferedImage image = ImageIO.read(fileTmp);
                width = image.getWidth();
                height = image.getHeight();

                //360
                if (width < 360 || height < 360) {
                    sHeight = height;
                    sWidth = width;
                } else {
                    if (width == height) {
                        sHeight = sWidth = 360;
                    }
                    if (width > height) {
                        sHeight = 360;
                        sWidth = (int) 360 * width / height;
                    }
                    if (height > width) {
                        sHeight = (int) 360 * height / width;
                        sWidth = 360;
                    }
                }

                //640
                if (width < 640 || height < 640) {
                    mHeight = height;
                    mWidth = width;
                } else {
                    if (width == height) {
                        mHeight = mWidth = 640;
                    }
                    if (width > height) {
                        mHeight = 640;
                        mWidth = (int) 640 * width / height;
                    }
                    if (height > width) {
                        mHeight = (int) 640 * height / width;
                        mWidth = 640;
                    }
                }

                //120
                if (width < 120 || height < 120) {
                    tHeight = height;
                    tWidth = width;
                } else {
                    if (width == height) {
                        tHeight = tWidth = 120;
                    }
                    if (width > height) {
                        tHeight = 120;
                        tWidth = (int) 120 * width / height;
                    }
                    if (height > width) {
                        tHeight = (int) 120 * height / width;
                        tWidth = 120;
                    }
                }


                /*
                if (width == height) {
                    sHeight = sWidth = 360;
                    mHeight = mWidth = 640;
                    tHeight = tWidth = 120;
                }
                if (width > height) {
                    sHeight = 360;
                    sWidth = (int) 360 * width / height;
                }
                if (height > width) {
                    sHeight = (int) 360 * height / width;
                    sWidth = 360;
                }

                tHeight = height;
                tWidth = width;
                if (width > height) {
                    tHeight = 120;
                    tWidth = (int) 120 * width / height;
                }
                if (height > width) {
                    tHeight = (int) 120 * height / width;
                    tWidth = 120;
                }

                mHeight = height;
                mWidth = width;
                if (width > 640 || height > 640) {
                    if (width > height) {
                        if (width > 640) {
                            mWidth = (int) 640 * width / height;
                            mHeight = 640;
                        }
                    }
                    if (height > width) {
                        if (height > 640) {
                            mHeight = (int) 640 * height / width;
                            mWidth = 640;
                        }
                    }
                }

                */
                Configuration conf = GlobalConfig.get();
                if (!(photoStorage instanceof OssSFS)) {
                    Record photoStoragePath = Record.fromJson(conf.getString("platform.servlet.photoStorage", ""));
                    Record r1 = new Record();
                    r1.put("dir", path);
                    Record r2 = new Record();
                    r2.put("class", photoStoragePath.getString("class"));
                    r2.put("args", r1);
                    photoStorage = (StaticFileStorage) ClassUtils2.newInstance(r2.toString(false, false));
                }


                String lfn = file + "_L." + expName;
                String ofn = file + "_O." + expName;
                String sfn = file + "_S." + expName;
                String tfn = file + "_T." + expName;
                if (photoStorage instanceof OssSFS) {
                    lfn = "media/photo/" + lfn;
                    ofn = "media/photo/" + ofn;
                    sfn = "media/photo/" + sfn;
                    tfn = "media/photo/" + tfn;
                }
                L.op(ctx, "saveUploadPhoto");
                if (L.isTraceEnabled())
                    L.traceEndCall(ctx, METHOD);
                SFSUtils.saveScaledUploadImage(fileTmp, photoStorage, ofn, null, null, expName);
                SFSUtils.saveScaledUploadImage(fileTmp, photoStorage, lfn, Integer.toString(mWidth), Integer.toString(mHeight), expName);
                SFSUtils.saveScaledUploadImage(fileTmp, photoStorage, sfn, Integer.toString(sWidth), Integer.toString(sHeight), expName);
                SFSUtils.saveScaledUploadImage(fileTmp, photoStorage, tfn, Integer.toString(tWidth), Integer.toString(tHeight), expName);
                fileTmp.delete();
            }
        } catch (IOException e) {
            throw new ServerException(WutongErrors.PHOTO_UPLOAD_OR_RESIZE_ERROR, "can not read this file,not image");
        }
    }

    @Override
    public RecordSet dealWithGroupPhoto(Context ctx, Record viewerPhotoRec, List<String> groupIds) {
        final String METHOD = "dealWithGroupPhoto";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerPhotoRec, groupIds);
        RecordSet recs = new RecordSet();
        String viewerId = viewerPhotoRec.getString("user_id");
        recs.add(viewerPhotoRec);

        RecordSet groups = GlobalLogics.getGroup().getGroups(ctx, 0, 0, viewerId, StringUtils2.joinIgnoreBlank(",", groupIds), Constants.GROUP_LIGHT_COLS, false);
        for (Record group : groups) {
            String groupId = group.getString("id", "0");
            String groupName = group.getString("name", "Default");
            Record rec = viewerPhotoRec.copy();
            rec.put("user_id", groupId);
            rec.put("album_id", getAlbum(ctx, groupId, ALBUM_TYPE_GROUP, groupName));
            recs.add(rec);

            Record viewerGroupPhoto = viewerPhotoRec.copy();
            viewerGroupPhoto.put("album_id", getAlbum(ctx, groupId, ALBUM_TYPE_TO_GROUP, groupName));
            recs.add(viewerGroupPhoto);
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }
}
