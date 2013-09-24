package com.borqs.server.platform.folder;

import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.platform.photo.PhotoException;
import com.borqs.server.service.platform.Constants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.service.platform.Constants.BLOCKED_CIRCLE;

public class SimpleFolder extends ConfigurableBase {
    private static final Logger L = LoggerFactory.getLogger(SimpleFolder.class);
    public final Schema folderSchema = Schema.loadClassPath(SimpleFolder.class, "folder.schema");
    protected final Schema staticFileSchema = Schema.loadClassPath(SimpleFolder.class, "staticfile.schema");
    protected final Schema audioSchema = Schema.loadClassPath(SimpleFolder.class, "audio.schema");
    protected final Schema videoSchema = Schema.loadClassPath(SimpleFolder.class, "video.schema");

    public static int FOLDER_TYPE_SHARE_OUT = 1;
    public static int FOLDER_TYPE_RECEIVED = 3;
    public static int FOLDER_TYPE_GROUP = 4;
    public static int FOLDER_TYPE_TO_GROUP = 5;     //              我在group里面发文件创建的文件夹
    public static int FOLDER_TYPE_MY_SYNC = 8;
    public static int FOLDER_TYPE_OTHERS = 9;
    public static int FOLDER_TYPE_CONFIGURATION = 20;

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


    public boolean createFolder(Record record) {
        String name = record.getString("title", null);
        if (StringUtils.isEmpty(name))
            throw new PhotoException("title is null , can't create");
        boolean can_create = true;
        if (record.getInt("folder_type") == FOLDER_TYPE_OTHERS || record.getInt("folder_type") == FOLDER_TYPE_TO_GROUP) {
            if (isMyFolderExist(record.getString("user_id"), (int) record.getInt("folder_type"), record.getString("title")))
                can_create = false;
        } else {
            if (isMyFolderExistType(record.getString("user_id"), (int) record.getInt("folder_type")))
                can_create = false;
        }
        long n = 0;
        if (can_create) {
            final String SQL = "INSERT INTO ${table}  ${values_join(alias, info)}";
            String sql = SQLTemplate.merge(SQL,
                    "table", "folder",
                    "alias", folderSchema.getAllAliases(),
                    "info", record);

            SQLExecutor se = getSqlExecutor();
            n = se.executeUpdate(sql);
        }
        return n > 0;
    }

    public RecordSet getUserFolder(String viewerId, String userId) {       //0 open 1 only me 2 friend open
        //1,我看自己  2，我看别人 3，我看圈子
        String sql10="";
        if (viewerId.equals(userId)) {
            sql10 = "SELECT * from folder WHERE user_id = '" + userId + "' and destroyed_time=0 order by folder_type,created_time";
        } else if (!viewerId.equals(userId) && userId.length() < 10) {
            sql10 = "SELECT * from folder WHERE user_id = '" + userId + "' and destroyed_time=0 and folder_type in (1,5,9) order by folder_type,created_time";
        } else if (!viewerId.equals(userId) && userId.length() > 10) {
            sql10 = "SELECT * from folder WHERE user_id = '" + userId + "' and destroyed_time=0 and folder_type in (4) order by folder_type,created_time";
        }
//        final String SQL = "SELECT * from folder WHERE user_id = '" + userId + "' order by folder_type,created_time";
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
        L.debug("folder/all:rs=" + rs.toString());
        for (Record rc : rs) {
            rc.put("file_count", getFolderSize(viewerId, rc.getString("folder_id")));
        }
        L.debug("folder/all:rs new=" + rs.toString());

        for (int i = rs.size() - 1; i >= 0; i--) {
            if (rs.get(i).getInt("file_count") <= 0)
                rs.remove(i);
        }
        return rs;
    }

    public RecordSet getFolderFiles(String viewerId, String folder_id, int page, int count) {
        SQLExecutor se = getSqlExecutor();
        Record r = se.executeRecord("select folder_type from folder where folder_id='" + folder_id + "' and destroyed_time=0", null);
        int folder_type = 0;
        if (!r.isEmpty()) {
            folder_type = (int) r.getInt("folder_type");
        }

        RecordSet out_recs = new RecordSet();
        String sql = "";
        if (folder_type == FOLDER_TYPE_RECEIVED || folder_type == FOLDER_TYPE_MY_SYNC || folder_type == FOLDER_TYPE_OTHERS) {
            sql = "SELECT * from static_file WHERE destroyed_time=0 and folder_id = '" + folder_id + "' order by created_time desc " + SQLUtils.pageToLimit(page, count);
            out_recs = se.executeRecordSet(sql,null);
        }
        if (folder_type == FOLDER_TYPE_SHARE_OUT || folder_type == FOLDER_TYPE_GROUP || folder_type == FOLDER_TYPE_TO_GROUP) {
            if (viewerId.equals("") || viewerId.equals("0")) {
                sql = "SELECT * from static_file WHERE destroyed_time=0 and folder_id = '" + folder_id + "'" +
                        " and stream_id<>0" +
                        " and stream_id in (select post_id from stream where destroyed_time=0 and privince=0)  order by created_time desc " + SQLUtils.pageToLimit(page, count);
                out_recs = se.executeRecordSet(sql,null);
            } else {
                //先看我在哪些group_id里面
                String sql_group = "select distinct(group_id) from group_members where member='" + viewerId + "' and destroyed_time=0";
                RecordSet recs_group = se.executeRecordSet(sql_group, null);
                List<String> gl = StringUtils2.splitList(recs_group.joinColumnValues("group_id", ","), ",", true);
                sql = "select p.*,p.stream_id,s.mentions,s.source,s.add_to,s.privince from static_file p inner join stream s on p.stream_id=s.post_id" +
                        " where p.folder_id='" + folder_id + "' and p.destroyed_time=0 and s.destroyed_time=0 and p.stream_id<>0 order by p.created_time desc";
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

    public Record getFolderOriginal(String folder_id) {
        SQLExecutor se = getSqlExecutor();
        Record r = se.executeRecord("select * from folder where destroyed_time=0 and folder_id='" + folder_id + "'", null);
        return r;
    }

    public boolean isFolderExist(String folder_id) {
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT folder_id from folder WHERE destroyed_time=0 and folder_id = " + "'" + folder_id + "'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean isMyFolderExist(String userId,int folder_type,String folder_name) {
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT folder_id from folder WHERE destroyed_time=0 and title = '" + folder_name + "' and folder_type="+folder_type+" and user_id='"+userId+"'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean isMyFolderExistType(String userId,int folder_type) {
        SQLExecutor se = getSqlExecutor();
        final String SQL = "SELECT folder_id from folder WHERE destroyed_time=0 and folder_type="+folder_type+" and user_id='"+userId+"'";
        Record rec = se.executeRecord(SQL.toString(), null);
        if (null != rec && rec.isEmpty()) {
            return false;
        }
        return true;
    }


    public boolean updateFolder(String folder_id, Record rc) {
        String sql = new SQLBuilder.Update(folderSchema).update("folder").values(rc)
                .where("folder_id = " + "'" + folder_id + "'").toString();
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public boolean deleteFolderById(String userId, String folder_id) {
        SQLExecutor se = getSqlExecutor();
        String sql0 = "select file_id from static_file where destroyed_time=0 and  folder_id='" + folder_id + "'";
        RecordSet rs = se.executeRecordSet(sql0.toString(), null);
        String file_ids = rs.joinColumnValues("file_id", ",");
        if (file_ids.length() > 0)
            deleteFileById(userId, file_ids, false);
        // delete file DB
        String sql = "update folder set destroyed_time='"+DateUtils.nowMillis()+"' WHERE folder_id = " + "'" + folder_id + "'";
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public int getFolderSize(String viewerId, String folder_id) {
        return getFolderFiles(viewerId, folder_id, 0, 5000).size();
    }

    public Record getFolderById(String viewerId,String userId, String folder_id) {
        String sql = "SELECT * from folder WHERE destroyed_time=0 and  folder_id = '" + folder_id + "'";
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
            rec.put("file_count",getFolderSize(viewerId, folder_id));
             return rec;
        } else {
            return new Record();
        }
    }

    public boolean updateStaticFileStreamId(String stream_id, List<String> file_ids) {
        String pids = StringUtils.join(file_ids,",");
        String sql = "update static_file set stream_id='" + stream_id + "' where file_id in ("+pids+") and stream_id=0";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public String getFolder(String userId,int folder_type,String folderName) {
        if (!isMyFolderExist(userId, folder_type, folderName))
            createUserFolder(userId, folder_type, folderName);

        String sql="";
        if (folder_type == FOLDER_TYPE_OTHERS || folder_type == FOLDER_TYPE_TO_GROUP) {
            sql = "SELECT folder_id from folder WHERE destroyed_time=0 and user_id = '" + userId + "' and folder_type=" + folder_type + " AND title = '" + folderName + "' ORDER BY created_time ";
        } else {
            sql = "SELECT folder_id from folder WHERE destroyed_time=0 and user_id = '" + userId + "' and folder_type=" + folder_type + " ORDER BY created_time ";
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet rec = se.executeRecordSet(sql.toString(), null);

        if (rec.size() > 0) {
            return rec.getFirstRecord().getString("folder_id");
        } else
            throw new PhotoException("can't get folder:"+ folderName);
    }

    private boolean createUserFolder(String userId,int folder_type,String folderName) {
        Record rc = new Record();
        String folder_id = Long.toString(RandomUtils.generateId());
        long uploaded_time = DateUtils.nowMillis();

        rc.put("folder_id", folder_id);
        rc.put("user_id", userId);
        rc.put("folder_type", folder_type);
        rc.put("title", folderName);
        rc.put("summary", "");
        rc.put("created_time", uploaded_time);
        if (folder_type == FOLDER_TYPE_RECEIVED || folder_type == FOLDER_TYPE_MY_SYNC) {
            rc.put("privacy", 1);
        } else {
            rc.put("privacy", 0);
        }

        return createFolder(rc);
    }

    private boolean isUserFolderExist(String userId,int folder_type) {
        String sql = "SELECT folder_id from folder WHERE destroyed_time=0 and user_id = " + "'" + userId + "' AND folder_type = '"+folder_type+"'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql.toString(), null);
        return !rec.isEmpty();
    }

    //========================================================================
    //======================以上是folder的



    public boolean deleteFileById(String viewerId, String file_ids, boolean delete_all) {
        SQLExecutor se = getSqlExecutor();

        //delete the storage files

        //先看谁在删除，源，还是接收者
       if (file_ids.length()<=10)
           return false;

        String sql0 = "SELECT file_id,new_file_name,user_id,stream_id from static_file WHERE destroyed_time=0 and file_id IN (" + file_ids + ")";
        RecordSet rs = se.executeRecordSet(sql0.toString(), null);
        long n = 0;
        if (rs.size() > 0) {
            for (Record rec : rs) {
                if (rec.isEmpty())
                    continue;
                String new_file_name = rec.getString("new_file_name");
                String o_user_id = StringUtils.split(new_file_name, "_")[0];
                String sql01 = "";
                if (viewerId.equals(o_user_id) && delete_all) {
                    sql01 = "update static_file set destroyed_time='"+DateUtils.nowMillis()+"' WHERE file_id='" + rec.getString("file_id") + "'";
                } else {
                    sql01 = "update static_file set destroyed_time='"+DateUtils.nowMillis()+"' WHERE file_id='" + rec.getString("file_id") + "' and user_id='" + viewerId + "'";
                }
                n = se.executeUpdate(sql01);
                if (rec.getInt("stream_id") != 0) {
                    String sql02 = "update stream set updated_time='" + DateUtils.nowMillis() + "' where post_id='" + rec.getInt("stream_id") + "'";
                    se.executeUpdate(sql02);
                }
                n += 1;
            }
        }
        return n > 0;
    }

    public boolean updateFile(String file_id, Record rc) {
        String sql = new SQLBuilder.Update(staticFileSchema).update("static_file").values(rc)
                .where("file_id = " + "'" + file_id + "'").toString();
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public boolean updateFileStreamId(int folder_type,String asc) {
        String sql = "SELECT file_id from static_file where destroyed_time=0 and folder_id in (select folder_id from folder where destroyed_time=0 and folder_type='"+folder_type+"') order by created_time "+asc+" ";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        for (Record rec : recs) {
            String img_small = rec.getString("img_small");
            String sql0 = "select post_id from stream where type in ("+Constants.FILE_POST+","+Constants.AUDIO_POST+","+Constants.VIDEO_POST+") and attachments like '%" + img_small + "%'";
            RecordSet recs0 = se.executeRecordSet(sql0.toString(), null);
            if (recs0.size() > 0) {
                Record rec0 = recs0.getFirstRecord();
                if (!rec0.isEmpty()) {
                    String sql1 = "update static_file set stream_id='" + rec0.getString("post_id") + "' where file_id='"+rec.getString("file_id")+"'";
                    se.executeUpdate(sql1);
                }
            }
        }
        return true;
    }

    public boolean saveStaticFile(Record staticFile) {
        final String SQL = "INSERT INTO static_file ${values_join(alias, staticFile)}";

        String sql = SQLTemplate.merge(SQL, "alias", staticFileSchema.getAllAliases(),
                "staticFile", staticFile);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public boolean saveStaticFiles(RecordSet staticFiles) {
        ArrayList<String> sqls = new ArrayList<String>();
        final String SQL = "INSERT INTO static_file ${values_join(alias, staticFile)}";
        for (Record rec : staticFiles) {
            String sql = SQLTemplate.merge(SQL, "alias", staticFileSchema.getAllAliases(),
                    "staticFile", rec);
            sqls.add(sql);
        }

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sqls);
        return n > 0;
    }

    public RecordSet getStaticFileByIds(String file_ids) {
        List<String> l01 = StringUtils2.splitList(file_ids, ",", true);
        RecordSet recs = new RecordSet();
        for (String l0 : l01) {
            String sql = "SELECT * FROM static_file WHERE file_id='" + l0 + "'";
            SQLExecutor se = getSqlExecutor();
            Record rec = se.executeRecord(sql, null);
            if (rec.getString("content_type").contains("video/")) {
                Record video = getVideoById(l0);
                video.copyTo(rec);
            }
            if (rec.getString("content_type").contains("audio/")) {
                Record audio = getAudioById(l0);
                audio.copyTo(rec);
            }
            recs.add(rec);
        }

        return recs;
    }

    public RecordSet getOriginalStaticFileByIds(String file_ids) {
        List<String> l01 = StringUtils2.splitList(file_ids, ",", true);
        RecordSet recs = new RecordSet();
        for (String l0 : l01) {
            String sql = "SELECT * FROM static_file WHERE file_id='" + l0 + "'";
            SQLExecutor se = getSqlExecutor();
            Record rec = se.executeRecord(sql, null);
            recs.add(rec);
        }

        return recs;
    }

    public boolean saveVideo(Record video) {
        final String SQL = "INSERT INTO video ${values_join(alias, video)}";

        String sql = SQLTemplate.merge(SQL,"alias", videoSchema.getAllAliases(),
                "video", video);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public Record getVideoById(String file_id) {
        String sql = "SELECT * FROM video WHERE file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }

    public boolean saveAudio(Record audio) {
        final String SQL = "INSERT INTO audio ${values_join(alias, audio)}";
        String sql = SQLTemplate.merge(SQL,"alias", audioSchema.getAllAliases(),
                "audio", audio);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public Record getAudioById(String file_id) {
        String sql = "SELECT * FROM audio WHERE file_id='" + file_id + "'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }









}
