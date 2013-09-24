package com.borqs.server.platform.photo;

import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import org.apache.commons.lang.StringUtils;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.service.platform.Constants.BLOCKED_CIRCLE;

public class SimplePhoto extends ConfigurableBase {
    private static final Logger L = LoggerFactory.getLogger(SimplePhoto.class);
    public final Schema photoSchema = Schema.loadClassPath(SimplePhoto.class, "photo.schema");
    public final Schema albumSchema = Schema.loadClassPath(SimplePhoto.class, "album.schema");

    private ConnectionFactory connectionFactory;
    private String db;

    private Configuration conf;

    public void init() {
        conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
    }

    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
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

    public boolean createAlbum(Record record) {
        String name = record.getString("title", null);
        if (StringUtils.isEmpty(name))
            throw new PhotoException("title is null , can't create");
        boolean can_create = true;
        if (record.getInt("album_type") == ALBUM_TYPE_OTHERS || record.getInt("album_type") == ALBUM_TYPE_TO_GROUP) {
            if (isMyAlbumExist(record.getString("user_id"), (int) record.getInt("album_type"), record.getString("title")))
                can_create = false;
        } else {
            if (isMyAlbumExistType(record.getString("user_id"), (int) record.getInt("album_type")))
                can_create = false;
        }
        long n = 0;
        if (can_create) {
            final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";
            String sql = SQLTemplate.merge(SQL,
                    "table", "album",
                    "alias", albumSchema.getAllAliases(),
                    "info", record);

            SQLExecutor se = getSqlExecutor();
            n = se.executeUpdate(sql);
            try {
                makPhotoDir(record.getString("user_id"), record.getString("album_id"));
            } catch (Exception e) {
            }
        }
        return n > 0;
    }

    public String makPhotoDir(String viewerId, String album_id) {
        String path = getPhotoPath(viewerId, album_id);
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        return path;
    }

    public String getPhotoPath(String viewerId, String album_id) {
        String a = Record.fromJson(conf.getString("platform.servlet.photoStorage", "")).getString("args");
        String c = StringUtils.substring(a, 1, a.length() - 1).replace("dir=", "");
        String path = c + "/" + viewerId + "/" + album_id + "/";
        return path;
    }

    public RecordSet getUserAlbum(String viewerId, String userId) {       //0 open 1 only me 2 friend open
        //1,我看自己  2，我看别人 3，我看圈子
        String sql10="";
        if (viewerId.equals(userId)) {
            sql10 = "SELECT * from album WHERE user_id = '" + userId + "' and destroyed_time=0 order by album_type,created_time";
        } else if (!viewerId.equals(userId) && userId.length() < 10) {
            sql10 = "SELECT * from album WHERE user_id = '" + userId + "' and destroyed_time=0 and album_type in (0,1,2,5,9) order by album_type,created_time";
        } else if (!viewerId.equals(userId) && userId.length() > 10) {
            sql10 = "SELECT * from album WHERE user_id = '" + userId + "' and destroyed_time=0 and album_type in (0,4) order by album_type,created_time";
        }
//        final String SQL = "SELECT * from album WHERE user_id = '" + userId + "' order by album_type,created_time";
        SQLExecutor se = getSqlExecutor();
        RecordSet rs = se.executeRecordSet(sql10, null);

        if (rs.size()<=0)
            return new RecordSet();

       boolean isFriend = false;
        if (!viewerId.equals("") && !userId.equals("") && !viewerId.equals("0") && !userId.equals("0")) {
            String sql0 = "select * from friend where user='" + userId + "' and friend='" + viewerId + "'" +
                    " AND circle<>" + BLOCKED_CIRCLE + ""
                    + " AND reason<>" + Constants.FRIEND_REASON_SOCIALCONTACT_DELETE + ""
                    + " AND reason<>" + Constants.FRIEND_REASON_DEFAULT_DELETE + " ";
            RecordSet rs1 = se.executeRecordSet(sql0, null);
            isFriend = rs1.size() > 0;
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
        if (rs.size()<=0)
            return new RecordSet();
        L.debug("album/all:rs=" + rs.toString());
        for (Record rc : rs) {
            rc.put("photo_count", getAlbumSize(viewerId, rc.getString("album_id")));
        }
        L.debug("album/all:rs new=" + rs.toString());

        for (int i = rs.size() - 1; i >= 0; i--) {
            if (rs.get(i).getInt("photo_count") <= 0)
                rs.remove(i);
        }
        return rs;
    }

    public RecordSet getAlbumPhotos(String viewerId, String album_id, int page, int count) {
        SQLExecutor se = getSqlExecutor();
        Record r = se.executeRecord("select album_type from album where album_id='" + album_id + "' and destroyed_time=0", null);
        int album_type = 0;
        if (!r.isEmpty()) {
            album_type = (int) r.getInt("album_type");
        }

        RecordSet out_recs = new RecordSet();
        String sql = "";
        if (album_type == ALBUM_TYPE_PROFILE || album_type == ALBUM_TYPE_COVER || album_type == ALBUM_TYPE_RECEIVED || album_type == ALBUM_TYPE_MY_SYNC || album_type == ALBUM_TYPE_OTHERS) {
            sql = "SELECT * from photo WHERE destroyed_time=0 and album_id = '" + album_id + "' order by created_time desc " + SQLUtils.pageToLimit(page, count);
            out_recs = se.executeRecordSet(sql,null);
        }
        if (album_type == ALBUM_TYPE_SHARE_OUT || album_type == ALBUM_TYPE_GROUP || album_type == ALBUM_TYPE_TO_GROUP) {
            if (viewerId.equals("") || viewerId.equals("0")) {
                sql = "SELECT * from photo WHERE destroyed_time=0 and album_id = '" + album_id + "'" +
                        " and stream_id<>0" +
                        " and stream_id in (select post_id from stream where destroyed_time=0 and privince=0)  order by created_time desc " + SQLUtils.pageToLimit(page, count);
                out_recs = se.executeRecordSet(sql,null);
            } else {
                //先看我在哪些group_id里面
                String sql_group = "select distinct(group_id) from group_members where member='" + viewerId + "' and destroyed_time=0";
                RecordSet recs_group = se.executeRecordSet(sql_group, null);
                List<String> gl = StringUtils2.splitList(recs_group.joinColumnValues("group_id", ","), ",", true);
                sql = "select p.*,p.stream_id,s.mentions,s.source,s.add_to,s.privince from photo p inner join stream s on p.stream_id=s.post_id" +
                        " where p.album_id='" + album_id + "' and p.destroyed_time=0 and s.destroyed_time=0 and p.stream_id<>0 order by p.created_time desc";
                out_recs = se.executeRecordSet(sql, null);
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
                                if (ml0.length() < 10) {  //   检查我在不在mentions里面
                                    if (ml0.equals(viewerId)) {
                                        v = true;
                                        break;
                                    }
                                } else {     //检查我参加的圈子里面有没有这个
                                    if (gl.contains(ml0)) {
                                        v = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!v) {
                            String add_to = out_recs.get(j).getString("add_to");
                            List<String> al = StringUtils2.splitList(add_to, ",", true);
                            for (String al0 : al) {
                                if (al0.length() < 10) {  //   检查我在不在add_to里面
                                    if (al0.equals(viewerId)) {
                                        v = true;
                                        break;
                                    }
                                } else {     //检查我参加的圈子里面有没有这个
                                    if (gl.contains(al0)) {
                                        v = true;
                                        break;
                                    }
                                }
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
        return out_recs;
    }

    public Record getAlbumOriginal(String album_id) {
        SQLExecutor se = getSqlExecutor();
        Record r = se.executeRecord("select * from album where destroyed_time=0 and album_id='" + album_id + "'", null);
        return r;
    }

    public RecordSet getPhotosIncludeMe(String viewerId,String user_id,int page, int count) {
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
        return recs;
    }


    public boolean isAlbumExist(String album_id) {
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT album_id from album WHERE destroyed_time=0 and album_id = " + "'" + album_id + "'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean isMyAlbumExist(String userId,int album_type,String album_name) {
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT album_id from album WHERE destroyed_time=0 and title = '" + album_name + "' and album_type="+album_type+" and user_id='"+userId+"'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean isMyAlbumExistType(String userId,int album_type) {
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT album_id from album WHERE destroyed_time=0 and album_type="+album_type+" and user_id='"+userId+"'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        return true;
    }


    public boolean updateAlbum(String album_id, Record rc) {
        rc.putMissing("updated_time", DateUtils.nowMillis());
        String sql = new SQLBuilder.Update(albumSchema).update("album").values(rc)
                .where("album_id = " + "'" + album_id + "'").toString();
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public boolean deleteAlbumById(String userId, String album_id, String bucketName_photo_key, StaticFileStorage photoStorage) {
        SQLExecutor se = getSqlExecutor();
        String sql0 = "select photo_id from photo where destroyed_time=0 and  album_id='" + album_id + "'";
        RecordSet rs = se.executeRecordSet(sql0.toString(), null);
        String photo_ids = rs.joinColumnValues("photo_id", ",");
        deletePhotoById(userId,photo_ids,false, bucketName_photo_key, photoStorage);
        // delete photo DB
        String sql = "update album set destroyed_time='"+DateUtils.nowMillis()+"',updated_time='"+DateUtils.nowMillis()+"' WHERE album_id = " + "'" + album_id + "'";
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public boolean saveUploadPhoto(Record record) {
        String album_id = record.getString("album_id", null);
        if (null == album_id)
            throw new PhotoException("no album , can't save");

        final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(SQL,
                "table", "photo",
                "alias", photoSchema.getAllAliases(),
                "info", record);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);

        String sql1 = "update album set updated_time='"+DateUtils.nowMillis()+"' WHERE album_id = " + "'" + album_id + "'";
        n = se.executeUpdate(sql1);
        return n > 0;
    }

    public boolean saveUploadPhotos(RecordSet recs) {
        ArrayList<String> sqls = new ArrayList<String>();
        final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";

        for (Record record : recs) {
            String album_id = record.getString("album_id", null);
            if (null == album_id)
                throw new PhotoException("no album , can't save");

            String sql = SQLTemplate.merge(SQL,
                    "table", "photo",
                    "alias", photoSchema.getAllAliases(),
                    "info", record);
            sqls.add(sql);
            sqls.add("update album set updated_time='"+DateUtils.nowMillis()+"' WHERE album_id = " + "'" + album_id + "'");
        }

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sqls);
        return n > 0;
    }

    public int getAlbumSize(String viewerId, String album_id) {
        return getAlbumPhotos(viewerId, album_id, 0, 5000).size();
    }

    public String getLatestPhotoId(String viewerId, String album_id) {
        return getLatestPhoto(viewerId, album_id).getString("photo_id");
    }

    public Record getLatestPhoto(String viewerId, String album_id) {
        RecordSet recs = getAlbumPhotos(viewerId, album_id, 0, 1);
        return recs.getFirstRecord();
    }

    public Record getAlbumById(String viewerId,String userId, String album_id) {
        String sql = "SELECT * from album WHERE destroyed_time=0 and  album_id = '" + album_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        if (rec.isEmpty())
            return new Record();
        boolean returnFlag = true;
        boolean isFriend = false;
        if (!viewerId.equals("") && !userId.equals("") && !viewerId.equals("0") && !userId.equals("0")) {
            String sql0 = "select * from friend where user='" + userId + "' and friend='" + viewerId + "'" +
                    " AND circle<>" + BLOCKED_CIRCLE + ""
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

        if (returnFlag) {
            rec.put("photo_count",getAlbumSize(viewerId,album_id));
             return rec;
        } else {
            return new Record();
        }
    }

    public RecordSet getPhotoByIds(String photo_ids) {
        String sql = "SELECT * from photo WHERE destroyed_time=0 and photo_id in ('" + photo_ids + "')";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);

        for (Record r : recs) {
            String album_id = r.getString("album_id", null);
            if (null != album_id) {
                sql = "SELECT * from album WHERE destroyed_time=0 and album_id = " + "'" + album_id + "'";
                Record rec1 = se.executeRecord(sql.toString(), null);
                if (null != rec1 && rec1.isEmpty())
                    throw new PhotoException("no such album, query error");
            }
            if (r.getString("tag").length() <= 2)
                r.put("tag", new RecordSet());
        }

        return recs;
    }

    public RecordSet getAllPhotos(String user_id) {
        String sql = "";
        if (user_id.equals("0")) {
            sql = "SELECT * from photo where destroyed_time=0 order by created_time desc ";
        } else {
            sql = "SELECT * from photo where user_id = '" + user_id + "' and destroyed_time=0 order by created_time desc ";
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    public boolean updatePhoto(String photo_id, Record rc) {
        String sql = "";
        SQLExecutor se = getSqlExecutor();
        long n = 0;
        if (rc.has("caption")) {
            sql = "update photo set caption='" + rc.getString("caption") + "',updated_time='"+DateUtils.nowMillis()+"' where photo_id = " + "'" + photo_id + "'";
            n = se.executeUpdate(sql);
        }
        if (rc.has("location")) {
            sql = "update photo set location='" + rc.getString("location") + "',updated_time='"+DateUtils.nowMillis()+"' where photo_id = " + "'" + photo_id + "'";
            n = se.executeUpdate(sql);
        }
        return n > 0;
    }

    public boolean updatePhotoStreamId(String stream_id, List<String> photo_ids) {
        String pids = StringUtils.join(photo_ids,",");
        String sql = "update photo set stream_id='" + stream_id + "' where photo_id in ("+pids+") and stream_id=0";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public boolean deletePhotoById(String viewerId, String photo_ids, boolean delete_all, String bucketName_photo_key, StaticFileStorage sfs) {
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
                    sql01 = "update photo set destroyed_time='"+DateUtils.nowMillis()+"' WHERE photo_id='" + rec.getString("photo_id") + "'";
                } else {
                    sql01 = "update photo set destroyed_time='"+DateUtils.nowMillis()+"' WHERE photo_id='" + rec.getString("photo_id") + "' and user_id='" + viewerId + "'";
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
            if (album_ids.length() > 0)
                se.executeUpdate("update album set updated_time='" + DateUtils.nowMillis() + "' WHERE album_id in (" + album_ids + ")");
        }
        return n > 0;
    }

    public String getAlbum(String userId,int album_type,String albumName) {
        if (!isMyAlbumExist(userId, album_type, albumName))
            createUserAlbum(userId,album_type,albumName);

        String sql="";
        if (album_type == ALBUM_TYPE_OTHERS || album_type == ALBUM_TYPE_TO_GROUP) {
            sql = "SELECT album_id from album WHERE destroyed_time=0 and user_id = '" + userId + "' and album_type=" + album_type + " AND title = '" + albumName + "' ORDER BY created_time ";
        } else {
            sql = "SELECT album_id from album WHERE destroyed_time=0 and user_id = '" + userId + "' and album_type=" + album_type + " ORDER BY created_time ";
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql.toString(), null);

        if (rec.size() > 0) {
            return rec.getFirstRecord().getString("album_id");
        } else
            throw new PhotoException("can't get album:"+ albumName);
    }

    public Record tagPhoto(String photo_id, int top, int left, int frame_width, int frame_height, String tagUserId, String tagText, boolean addTag) {
        Record old_photo = getPhotoByIds(photo_id).getFirstRecord();

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
        String sql = "update photo set tag='" + tag_recs.toString() + "',updated_time='"+DateUtils.nowMillis()+"',tag_ids = '" + StringUtils.join(oldFaceIds, ",") + "' where photo_id = " + "'" + photo_id + "'";
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql);
        old_photo.put("tag", tag_recs);
        old_photo.put("tag_ids", StringUtils.join(oldFaceIds, ","));

        return old_photo;
    }

    private boolean createUserAlbum(String userId,int album_type,String albumName) {
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

        return createAlbum(rc);
    }

    private boolean isUserAlbumExist(String userId,int album_type) {
        String sql = "SELECT album_id from album WHERE destroyed_time=0 and user_id = " + "'" + userId + "' AND album_type = '"+album_type+"'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        return !rec.isEmpty();
    }

    public String genPhotoId(String userId) {
        return Long.toString(RandomUtils.generateId());
    }

    public boolean updatePhotoStreamId(int album_type,String asc) {
        String sql = "SELECT img_small,photo_id from photo where destroyed_time=0 and album_id in (select album_id from album where destroyed_time=0 and album_type='"+album_type+"') order by created_time "+asc+" ";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        for (Record rec : recs) {
            String img_small = rec.getString("img_small");
            String sql0 = "select post_id from stream where type=2 and attachments like '%" + img_small + "%'";
            RecordSet recs0 = se.executeRecordSet(sql0.toString(), null);
            if (recs0.size() > 0) {
                Record rec0 = recs0.getFirstRecord();
                if (!rec0.isEmpty()) {
                    String sql1 = "update photo set stream_id='" + rec0.getString("post_id") + "' where photo_id='"+rec.getString("photo_id")+"'";
                    se.executeUpdate(sql1);
                }
            }
        }
        return true;
    }
}
