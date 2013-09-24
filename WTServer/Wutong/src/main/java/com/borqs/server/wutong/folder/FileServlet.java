package com.borqs.server.wutong.folder;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.page.PageLogicUtils;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class FileServlet extends WebMethodServlet {
    private static final Logger L = Logger.getLogger(FileServlet.class);

    private StaticFileStorage photoStorage;
    private Configuration conf;
    private Commons commons;

    @Override
    public void init() throws ServletException {
        super.init();
        conf = GlobalConfig.get();
        commons = new Commons();
        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
    }

    @WebMethod("oss/upload")
    public NoResponse uploadFileToOSS(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String METHOD = "oss/upload";
        Context ctx = WutongContext.getContext(qp, false);
        L.traceStartCall(ctx, METHOD, qp.toString());
        String viewerId = ctx.getViewerIdString();
        FileItem fi = qp.checkGetFile("file");
        String fileName = qp.checkGetString("file_name");
        String bucketName = qp.checkGetString("bucket_name");
        String callBack = qp.getString("callback", "");
        OssSFS ossStorage = new OssSFS(bucketName);
        SFSUtils.saveUpload(fi, ossStorage, fileName);

        if (StringUtils.isNotBlank(callBack))
            resp.sendRedirect(callBack);

        return NoResponse.get();
    }



    @WebMethod("v2/file/upload")
    public Record fileUpload(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String METHOD = "v2/file/upload";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());

        String viewerId = String.valueOf(ctx.getViewerId());
        FileItem fi = qp.checkGetFile("file");
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            String file_id = Long.toString(RandomUtils.generateId());
            FileItem screen_shot = qp.getFile("screen_shot");
            String summary = qp.getString("summary","");
            String description = qp.getString("description","");
            String content_type = qp.getString("content_type","");
            String file_name = qp.getString("file_name","");
            long folder_id = Long.parseLong(GlobalLogics.getFile().getFolder(ctx,viewerId, Constants.FOLDER_TYPE_MY_SYNC, "Sync Files"));
            Record rec = commons.formatFileBucketUrl(ctx,viewerId,commons.uploadFile(ctx, viewerId, file_id, folder_id, fi, summary, description, content_type, screen_shot, file_name));
            L.debug(ctx,"rec="+rec);
            return rec;
        } else {
            L.debug(ctx,"fi null or fi.getName() is empty");
            return new Record();
        }
    }

    @WebMethod("v2/file/share")
    public Record fileShare(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final String METHOD = "v2/file/share";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());

        String viewerId = String.valueOf(ctx.getViewerId());
        FileItem fi = qp.checkGetFile("file");
        String app_data = qp.getString("app_data", "");

        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        String post_id = "";
        boolean can_comment = qp.getBoolean("can_comment", true);
        boolean can_like = qp.getBoolean("can_like", true);
        boolean can_reshare = qp.getBoolean("can_reshare", true);
        boolean sendEmail = qp.getBoolean("send_email", false);
        boolean sendSms = qp.getBoolean("send_sms", false);
        boolean isTop = qp.getBoolean("is_top", false);
        long scene = qp.getInt("scene", 0L);

        String add_to = commons.getAddToUserIds(qp.checkGetString("msg"));
        String mentions = qp.getString("mentions", "");
        boolean privacy = qp.getBoolean("secretly", false);
        List<String> groupIds = new ArrayList<String>();
        List<String> fids = new ArrayList<String>();
        String tmp_ids = "";

        if (mentions.length() > 0) {
            List<String> l0 = StringUtils2.splitList(mentions, ",", true);
            if (l0.contains("#-2")) {
                l0.remove("#-2");
                mentions = StringUtils.join(l0, ",");
            } else {
//                privacy = true;
            }

            //group
            groupIds = GlobalLogics.getGroup().getGroupIdsFromMentions(ctx,l0);
            for (String groupId : groupIds) {
                l0.remove("#" + groupId);
                l0.remove(groupId);
                Record groupRec = GlobalLogics.getGroup().getSimpleGroups(ctx,Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.GROUP_ID_END,
                        groupId, Constants.GRP_COL_CAN_MEMBER_POST).getFirstRecord();
                long canMemberPost = groupRec.getInt(Constants.GRP_COL_CAN_MEMBER_POST, 1);
                if ((canMemberPost == 1 && GlobalLogics.getGroup().hasRight(ctx,Long.parseLong(groupId), Long.parseLong(viewerId), Constants.ROLE_MEMBER))
                        || (canMemberPost == 0 && GlobalLogics.getGroup().hasRight(ctx,Long.parseLong(groupId), Long.parseLong(viewerId), Constants.ROLE_ADMIN))
                        || canMemberPost == 2) {
                    l0.add(groupId);
                }
            }

            PageLogicUtils.removeIllegalPageIds(ctx, l0);

            mentions = StringUtils.join(l0, ",");
            tmp_ids = commons.parseUserIds(ctx,viewerId, mentions);
            List<String> l = StringUtils2.splitList(tmp_ids, ",", true);
            if (l.size() > Constants.MAX_GUSY_SHARE_TO)
                throw new ServerException(WutongErrors.STREAM_CANT_SHARE_TOO_MANY_PEOPLE, "Only can share to less than 400 guys!");
        }
        L.debug(ctx,"tmp_ids="+tmp_ids);

        if (privacy == true) {
            if (mentions.length() <= 0 && groupIds.isEmpty())
                throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "want mentions!");
        }
        if (StringUtils.isBlank(mentions) && !groupIds.isEmpty())
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "You don't have right to post!");

        String share_file_id = qp.getString("file_id","");

        if (fi != null && StringUtils.isNotEmpty(fi.getName()) && share_file_id.equals("")) {
            FileItem screen_shot = qp.getFile("screen_shot");
            String summary = qp.getString("summary","");
            String description = qp.getString("description","");
            String content_type = qp.getString("content_type","");
            String file_name = qp.getString("file_name","");

            String folder_id = GlobalLogics.getFile().getFolder(ctx,viewerId,Constants.FOLDER_TYPE_SHARE_OUT, "Sharing Files");
            if (!GlobalLogics.getFile().isFolderExist(ctx,folder_id)) {
                throw new ServerException(WutongErrors.FILE_FOLDER_NOT_EXISTS,"folder not exist, please create folder first");
            }

            String file_id = Long.toString(RandomUtils.generateId());

            Record static_file =  commons.uploadFile(ctx,viewerId, file_id, Long.parseLong(folder_id), fi, summary, description, content_type, screen_shot, file_name);
            fids.add(file_id);

            if (!groupIds.isEmpty()) {
                RecordSet group_recs = GlobalLogics.getGroup().dealWithGroupFile(ctx, static_file, groupIds);
                fids.add(file_id);
                GlobalLogics.getFile().saveStaticFiles(ctx,group_recs);
            }

            Record rec = commons.formatFileBucketUrl(ctx,viewerId,static_file);
            String msg = qp.getString("msg", "");
            int type = Constants.FILE_POST;
//            if (rec.getString("content_type").contains("image/"))
//                type = Constants.PHOTO_POST;
            if (rec.getString("content_type").contains("video/")) {
                type = Constants.VIDEO_POST;
            } else if (rec.getString("content_type").contains("audio/")) {
                type = Constants.AUDIO_POST;
            }


            List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < Constants.USER_ID_MAX_LEN)
                        l00.add(l011);
                }
            }
            L.debug(ctx,"received people list ="+l00.toString());
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= Constants.USER_ID_MAX_LEN) {
                        try {
                            String other_folder_id = GlobalLogics.getFile().getFolder(ctx,uid, Constants.FOLDER_TYPE_RECEIVED, "Received Files");
                            if (static_file.has("file_url"))
                                static_file.removeColumns("file_url");
                            if (static_file.has("thumbnail_url"))
                                static_file.removeColumns("thumbnail_url");
                            if (static_file.has("likes"))
                                static_file.removeColumns("likes");
                            if (static_file.has("comments"))
                                static_file.removeColumns("comments");

                            static_file.put("folder_id", other_folder_id);
                            static_file.put("user_id", uid);
                            GlobalLogics.getFile().saveStaticFile(ctx,static_file);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            Record o = GlobalLogics.getStream().postP(ctx,viewerId, type, msg, commons.formatFileBucketUrlForStream(ctx,viewerId,rec).toString(false, false), qp.getString("appid", "1"),
                    "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to, sendEmail, sendSms, isTop,Constants.POST_SOURCE_PEOPLE, scene);
            post_id = o.checkGetString("post_id");
            if (!post_id.equals("") && !qp.getString("category_id","").equals(""))
               GlobalLogics.getCategory().createCategory(ctx,viewerId,qp.getString("category_id",""),String.valueOf(Constants.POST_OBJECT),post_id);
            if (fids.size() > 0 && !post_id.equals(""))
                GlobalLogics.getFile().updateStaticFileStreamId(ctx,post_id, fids);
//            Record o =  GlobalLogics.getStream().getFullPostsForQiuPuP(ctx,viewerId, post_id,true).getFirstRecord();
            L.debug(ctx,"ourt record  ="+o);
            return o;
        }
        else if (fi == null && !share_file_id.equals("")){
            Record old_file_info = GlobalLogics.getFile().getOriginalStaticFileByIds(ctx,share_file_id).getFirstRecord();
            old_file_info.put("user_id",viewerId);

            String folder_id = GlobalLogics.getFile().getFolder(ctx,viewerId, Constants.FOLDER_TYPE_SHARE_OUT, "Sharing Files");
            if (!GlobalLogics.getFile().isFolderExist(ctx,folder_id)) {
                throw new ServerException(WutongErrors.FILE_FOLDER_NOT_EXISTS,"folder not exist, please create folder first");
            }
            old_file_info.put("folder_id",folder_id);
            old_file_info.put("created_time", DateUtils.nowMillis());
            old_file_info.put("updated_time", DateUtils.nowMillis());
            GlobalLogics.getFile().saveStaticFile(ctx,old_file_info);
            String file_id = old_file_info.getString("file_id");
            fids.add(file_id);

            if (!groupIds.isEmpty()) {
                RecordSet group_recs = GlobalLogics.getGroup().dealWithGroupFile(ctx,old_file_info, groupIds);
                fids.add(file_id);
                GlobalLogics.getFile().saveStaticFiles(ctx,group_recs);
            }

            Record rec = commons.formatFileBucketUrl(ctx,viewerId,old_file_info);
            String msg = qp.getString("msg", "");
            int type = Constants.FILE_POST;
//            if (rec.getString("content_type").contains("image/"))
//                type = Constants.PHOTO_POST;
            if (rec.getString("content_type").contains("video/")) {
                type = Constants.VIDEO_POST;
            } else if (rec.getString("content_type").contains("audio/")) {
                type = Constants.AUDIO_POST;
            }


            List<String> l00 = StringUtils2.splitList(tmp_ids, ",", true);
            if (add_to.length() > 0) {
                List<String> l01 = StringUtils2.splitList(add_to, ",", true);
                for (String l011 : l01) {
                    if (!l00.contains(l011) && l011.length() < Constants.USER_ID_MAX_LEN)
                        l00.add(l011);
                }
            }
            L.debug(ctx,"received people list ="+l00.toString());
            if (l00.size() > 0) {
                for (String uid : l00) {
                    if (uid.length() <= Constants.USER_ID_MAX_LEN) {
                        try {
                            String other_folder_id = GlobalLogics.getFile().getFolder(ctx,uid, Constants.FOLDER_TYPE_RECEIVED, "Received Files");
                            if (old_file_info.has("file_url"))
                                old_file_info.removeColumns("file_url");
                            if (old_file_info.has("thumbnail_url"))
                                old_file_info.removeColumns("thumbnail_url");
                            if (old_file_info.has("likes"))
                                old_file_info.removeColumns("likes");
                            if (old_file_info.has("comments"))
                                old_file_info.removeColumns("comments");

                            old_file_info.put("folder_id", other_folder_id);
                            old_file_info.put("user_id", uid);
                            GlobalLogics.getFile().saveStaticFile(ctx,old_file_info);
                        } catch (Exception e) {
                        }
                    }
                }
            }

            Record o = GlobalLogics.getStream().postP(ctx,viewerId, type, msg, commons.formatFileBucketUrlForStream(ctx,viewerId,rec).toString(false, false), qp.getString("appid", "1"),
                    "", "", app_data, mentions, privacy, "", ua, loc, "", "", can_comment, can_like, can_reshare, add_to,Constants.POST_SOURCE_PEOPLE, scene);
            post_id = o.checkGetString("post_id");
            if (fids.size() > 0 && !post_id.equals(""))
                GlobalLogics.getFile().updateStaticFileStreamId(ctx,post_id, fids);
//            Record o = GlobalLogics.getStream().getFullPostsForQiuPuP(ctx,viewerId, post_id,true).getFirstRecord();
            L.debug(ctx,"ourt record  ="+o);
            return o;
        }
        else {
            L.debug(ctx,"share file type error");
            return new Record();
        }
    }

    @WebMethod("v2/folder/create")
    public boolean createFolder(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "v2/folder/create";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String userId = String.valueOf(ctx.getViewerId());
        String folder_name = qp.checkGetString("title");
        int visible = (int)qp.getInt("privacy", 0);         //0 open 1 only me 2 friend open
        String description = qp.getString("summary", "");
        String folder_id = Long.toString(RandomUtils.generateId());
        Record rc = new Record();

        rc.put("folder_id", folder_id);
        rc.put("folder_type", Constants.FOLDER_TYPE_OTHERS);
        rc.put("user_id", userId);
        rc.put("title", folder_name);
        rc.put("created_time", DateUtils.nowMillis());
        rc.put("summary", description);
        rc.put("privacy", visible);
        GlobalLogics.getFile().createFolder(ctx,rc);
        return true;
    }


    @WebMethod("v2/folder/all")
    public RecordSet getFolders(QueryParams qp,HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        final String METHOD = "v2/folder/all";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = String.valueOf(ctx.getViewerId());
        }
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        String userId = qp.getString("user_id", viewerId);
        RecordSet recs = folderLogic.getUserFolder(ctx,viewerId, userId);
        L.debug(ctx,"user folder=" + recs.toString());
        for (Record rec : recs) {
            rec.put("title",formatFolderName(ua, (int) rec.getInt("folder_type"), rec.getString("title")));
        }
        L.debug(ctx,"user folder=" + recs.toString());
        return recs;
    }

    public String formatFolderName(String ua, int folder_type, String folder_name) {
        if (folder_type == Constants.FOLDER_TYPE_SHARE_OUT)
            folder_name = Constants.getBundleString(ua, "folder.name.sharing");
        if (folder_type == Constants.FOLDER_TYPE_RECEIVED)
            folder_name = Constants.getBundleString(ua, "folder.name.received");
        if (folder_type == Constants.FOLDER_TYPE_MY_SYNC)
            folder_name = Constants.getBundleString(ua, "folder.name.cloud");
        return folder_name;
    }

    @WebMethod("v2/folder/get")
    public Record getFolderById(QueryParams qp,HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        final String METHOD = "v2/folder/get";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = String.valueOf(ctx.getViewerId());
        }
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        String userId = qp.getString("user_id", viewerId);
        String folder_id = qp.checkGetString("folder_id");

        Record rec = folderLogic.getFolderById(ctx,viewerId, userId, folder_id);
        rec.put("title",formatFolderName(ua, (int) rec.getInt("folder_type"), rec.getString("title")));
        L.debug(ctx,"return rec=" + rec.toString());
        return rec;
    }

    @WebMethod("v2/folder/update")
    public boolean updateFolder(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "v2/folder/update";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String viewerId = String.valueOf(ctx.getViewerId());

        String folder_id = qp.checkGetString("folder_id");
        Record r = folderLogic.getFolderOriginal(ctx,folder_id);
        int folder_type = (int) r.getInt("folder_type");
        if (folder_type != Constants.FOLDER_TYPE_OTHERS)
            throw new ServerException(WutongErrors.FOLDER_CANT_UPDATE,"only can update user folder");
        if (!viewerId.equals(r.getString("user_id")))
            throw new ServerException(WutongErrors.FOLDER_CANT_UPDATE,"can't update other folder");
        String folder_name = qp.getString("title", null);
        String description = qp.getString("summary", null);
        String visible = qp.getString("privacy", null);

        if (!StringUtils.isNotBlank(visible) && visible != null) {
            if (!visible.equals("0") && !visible.equals("1") && !visible.equals("2"))
                throw new ServerException(WutongErrors.FOLDER_PRIVACY_ERROR,"privacy error, privacy must be 0,1,2");
        }

        Record rc = new Record();
        if (StringUtils.isNotBlank(folder_name) && folder_name != null) {
            rc.put("title", folder_name);
        }
        if (StringUtils.isNotBlank(description) && description != null) {
            rc.put("summary", description);
        }
        if (StringUtils.isNotBlank(visible) && visible != null) {
            rc.put("privacy", Integer.valueOf(visible));
        }

        return folderLogic.updateFolder(ctx,folder_id, rc);
    }

    @WebMethod("v2/folder/delete")
    public boolean deleteFolder(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "v2/folder/delete";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String viewerId = String.valueOf(ctx.getViewerId());

        String folder_id = qp.checkGetString("folder_id");
        Record folder0 = folderLogic.getFolderOriginal(ctx,folder_id);
        if (folder0.getInt("folder_type") != Constants.FOLDER_TYPE_OTHERS)
            throw new ServerException(WutongErrors.FOLDER_CANT_DELETE,"can't delete this folder");
        if (!viewerId.equals(folder0.getString("user_id")))
            throw new ServerException(WutongErrors.FOLDER_CANT_DELETE,"can't delete other folder");
        return folderLogic.deleteFolderById(ctx,viewerId, folder_id);
    }

    @WebMethod("v2/file/get")
    public RecordSet getFileByIds(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        final String METHOD = "v2/file/get";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String viewerId = String.valueOf(ctx.getViewerId());
        RecordSet recs = folderLogic.getStaticFileByIds(ctx,qp.checkGetString("file_ids"));
        for (Record rec : recs) {
            rec = commons.formatFileBucketUrl(ctx,viewerId,rec);
        }
        L.debug(ctx,"return recs=" + recs.toString());
        return recs;
    }

    @WebMethod("v2/file/folder_get")
    public RecordSet getFileByFolderIds(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        final String METHOD = "v2/file/folder_get";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String viewerId = String.valueOf(ctx.getViewerId());
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        List<String> folder_ids0 = StringUtils2.splitList(qp.checkGetString("folder_ids"),",",true);
        Record folder_ = folderLogic.getFolderOriginal(ctx,folder_ids0.get(0));
        String user_id = folder_.getString("user_id");
//        String cols = "photo_id,album_id,user_id,img_middle,img_original,img_big,img_small,caption,created_time,location,tag,tag_ids,from_user,original_pid,longitude,latitude,orientation,stream_id,privacy";
        RecordSet recs = folderLogic.getFolderFiles(ctx,viewerId, folder_ids0.get(0), page, count);
        for (Record rec : recs) {
            rec = commons.formatFileBucketUrl(ctx,viewerId,rec);
        }
        L.debug(ctx,"return recs=" + recs.toString());
        return recs;
    }

    @WebMethod("v2/file/update")
    public boolean updateFile(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "v2/file/update";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String viewerId = String.valueOf(ctx.getViewerId());

        String file_id = qp.checkGetString("file_id");
        String summary = qp.getString("summary", null);
        String description = qp.getString("description", null);
        Record rc = new Record();
        if (summary!=null){
            rc.put("summary", summary);
        }
        if (description!=null){
            rc.put("description", description);
        }
        return folderLogic.updateFile(ctx,file_id, rc);
    }

    @WebMethod("v2/file/delete")
    public boolean deleteFile(QueryParams qp) throws AvroRemoteException {
        final String METHOD = "v2/file/delete";
        Context ctx = WutongContext.getContext(qp, true);
        L.traceStartCall(ctx, METHOD, qp.toString());
        FolderLogic folderLogic = GlobalLogics.getFile();
        String viewerId = String.valueOf(ctx.getViewerId());
        String fIDs = qp.checkGetString("file_ids");
        boolean delete_all = qp.getBoolean("delete_all",false);
        return folderLogic.deleteFileById(ctx,viewerId,fIDs,delete_all);
    }


}
