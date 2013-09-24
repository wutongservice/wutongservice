package com.borqs.server.wutong.group;

import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.memcache.XMemcachedOnlineStatus;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.DirectResponse;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.notif.GroupInviteNotifSender;
import com.borqs.server.wutong.photo.PhotoLogic;
import com.borqs.server.wutong.poll.PollLogic;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.JsonNodeFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static com.borqs.server.wutong.Constants.*;

public class GroupServlet extends WebMethodServlet {
    private static final PageTemplate pageTemplate = new PageTemplate(GroupServlet.class);
    private static final Logger L = Logger.getLogger(GroupServlet.class);
    private String serverHost;

    public static final int DEFAULT_USER_COUNT_IN_PAGE = 20;
    private static String prefix = "http://oss.aliyuncs.com/wutong-data/media/photo/";
    private static String sysPrefix = "http://oss.aliyuncs.com/wutong-data/system/";
    private StaticFileStorage photoStorage;
    private StaticFileStorage profileImageStorage;
    XMemcachedOnlineStatus xMemcached = new XMemcachedOnlineStatus();

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");
        prefix = conf.getString("platform.profileImagePattern", prefix);
        sysPrefix = conf.getString("platform.sysIconUrlPattern", sysPrefix);
        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
        profileImageStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.profileImageStorage", ""));
        profileImageStorage.init();
        xMemcached.init();
    }

    @Override
    public void destroy() {
        profileImageStorage.destroy();
        photoStorage.destroy();
        xMemcached.destroy();
        super.destroy();
    }

    public long getParamInt(QueryParams qp, String preferKey, String secondKey, boolean isMust, long def) {
        if (qp.containsKey(preferKey))
            return isMust ? qp.checkGetInt(preferKey) : qp.getInt(preferKey, def);
        else
            return isMust ? qp.checkGetInt(secondKey) : qp.getInt(secondKey, def);
    }

    public String getParamString(QueryParams qp, String preferKey, String secondKey, boolean isMust, String def) {
        if (qp.containsKey(preferKey))
            return isMust ? qp.checkGetString(preferKey) : qp.getString(preferKey, def);
        else
            return isMust ? qp.checkGetString(secondKey) : qp.getString(secondKey, def);
    }

    private static void addBorqsStaffsToGroup(Context ctx, long groupId) {
        GroupLogic g = GlobalLogics.getGroup();
        List<String> staffIds = StringUtils2.splitList(GlobalLogics.getAccount().getBorqsUserIds(ctx), ",", true);
        Record staffs = new Record();
        for (String staffId : staffIds)
            staffs.put(staffId, ROLE_MEMBER);

        List<String> excludes = StringUtils2.splitList(g.getCreatorAndAdmins(ctx, groupId), ",", true);
        for (String exclude : excludes)
            staffs.remove(exclude);
        g.addMembers(ctx, groupId, staffs, false);
    }

    public static Record createPublicCircle(Context ctx, QueryParams qp) {
        return createGroup(ctx, PUBLIC_CIRCLE_ID_BEGIN, TYPE_PUBLIC_CIRCLE, qp);
    }

    public static Record createGroup(Context ctx, long begin, String groupType, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        String viewerId = ctx.getViewerIdString();

        String parentIds = "";
        String pageIds = "";
        String name = qp.checkGetString("name");
        int memberLimit = (int) qp.getInt("member_limit", 1000);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        int isStreamPublic = 1;
        int canSearch = 1;
        int canViewMembers = 1;
        int privacy = (int) qp.getInt("privacy", GRP_PRIVACY_OPEN);
        if (privacy == GRP_PRIVACY_CLOSED) {
            isStreamPublic = 0;
        } else if (privacy == GRP_PRIVACY_SECRET) {
            isStreamPublic = 0;
            canSearch = 0;
            canViewMembers = 0;
        }

        boolean sendPost = qp.getBoolean("send_post", true);
        boolean sendEmail = qp.getBoolean("send_email", false);
        boolean sendSms = qp.getBoolean("send_sms", false);
        int canJoin = (int) qp.getInt("can_join", 1);
        int canMemberInvite = (int) qp.getInt("can_member_invite", 1);
        int canMemberApprove = (int) qp.getInt("can_member_approve", 1);
        int canMemberPost = (int) qp.getInt("can_member_post", 1);
        int canMemberQuit = (int) qp.getInt("can_member_quit", 1);
        int needInvitedConfirm = (int) qp.getInt("need_invited_confirm", 1);
        String label = qp.getString("label", "其它");
        String members = qp.getString("members", "");
        String sNames = qp.getString("names", "");

        // TODO: check names

        List<String> toIds = StringUtils2.splitList(members, ",", true);
        List<String> names = StringUtils2.splitList(sNames, ",", true);

        List<String> borqsIds = new ArrayList<String>();
        List<String> identifies = new ArrayList<String>();
        List<String> identifyNames = new ArrayList<String>();
        List<String> virtuals = new ArrayList<String>();
        int size = toIds.size();

        for (int i = 0; i < size; i++) {
            String toId = toIds.get(i);
            String toName = names.get(i);

            Record typeRec = g.getTypeByStr(ctx, toId);
            int type = (int) typeRec.getInt("type");
            toId = typeRec.getString("str");
            if (type == IDENTIFY_TYPE_BORQS_ID)
                borqsIds.add(toId);
            else if (type == IDENTIFY_TYPE_LOCAL_CIRCLE) {
                String circleId = toId;
                RecordSet friendRecs = GlobalLogics.getFriendship().getFriends(ctx, viewerId, circleId, 0, -1);
                String userIds = friendRecs.joinColumnValues("friend", ",");
                RecordSet users = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userIds);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    int lcIdType = (int) (g.getTypeByStr(ctx, userId).getInt("type"));
                    if (lcIdType == IDENTIFY_TYPE_BORQS_ID)
                        borqsIds.add(userId);
                    else if (lcIdType == IDENTIFY_TYPE_VIRTUAL_ID)
                        virtuals.add(userId);
                }
            } else if (type == IDENTIFY_TYPE_GROUP_ID) {
                String fromGroup = toId;
                if (g.isPublicCircle(ctx, Long.parseLong(fromGroup))) {
                    parentIds += fromGroup + ",";
                }
                String userIds = g.getAllMembers(ctx, Long.parseLong(fromGroup), -1, -1, "");
                RecordSet users = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userIds);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    borqsIds.add(userId);
                }
            } else if (type == IDENTIFY_TYPE_PAGE_ID) {
                String fromPage = toId;
                pageIds += fromPage + ",";
                long[] followerIds = GlobalLogics.getFriendship().getFollowerIds(ctx, Long.parseLong(fromPage), 0, Constants.GROUP_ID_BEGIN, -1, -1);
                String userIds = StringUtils2.joinIgnoreBlank(",", Arrays.asList(ArrayUtils.toObject(followerIds)));
                RecordSet users = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userIds);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    borqsIds.add(userId);
                }
            } else if (type == IDENTIFY_TYPE_VIRTUAL_ID)
                virtuals.add(toId);
            else {
                identifies.add(toId);
                identifyNames.add(toName);
            }
        }

        String virtualIds = StringUtils2.joinIgnoreBlank(",", virtuals);
        if (StringUtils.isNotBlank(virtualIds)) {
            RecordSet vUsers = GlobalLogics.getFriendship().getContactFriendByFid(ctx, virtualIds);
            for (Record vUser : vUsers) {
                identifies.add(vUser.getString("content"));
                identifyNames.add(vUser.getString("name"));
            }
        }

        String message = qp.getString("message", "");

        Record properties = new Record(qp);
        properties.remove("appid");
        properties.remove("sign");
        properties.remove("sign_method");
        properties.remove("ticket");

        properties.remove("name");
        properties.remove("member_limit");
        properties.remove("is_stream_public");
        properties.remove("can_search");
        properties.remove("can_view_members");
        properties.remove("privacy");
        properties.remove("can_join");
        properties.remove("can_member_invite");
        properties.remove("can_member_approve");
        properties.remove("can_member_post");
        properties.remove("can_member_quit");
        properties.remove("need_invited_confirm");
        properties.remove("members");
        properties.remove("names");
        properties.remove("admins");
        properties.remove("message");
        properties.remove("label");
        properties.remove("borqs_staff");
        properties.remove("call_id");
        properties.remove("send_post");
        properties.remove("send_email");
        properties.remove("send_sms");

        parentIds = StringUtils.substringBeforeLast(parentIds, ",");
        pageIds = StringUtils.substringBeforeLast(pageIds, ",");
        if (StringUtils.equals(groupType, TYPE_EVENT)) {
//            properties.put("parent_ids", parentIds);
//            properties.put("page_ids", pageIds);
            properties.put("parent_ids", qp.getString("parent_ids", ""));
            properties.put("page_ids", qp.getString("page_ids", ""));
        }
        // add address json check
        if(qp.containsKey("address")){
            boolean b = JsonUtils.isValidate(qp.getString("address",null));
            if(!b)
                throw  new ServerException(WutongErrors.GROUP_CREATE_ERROR,"address is not a json");
        }
        long groupId = g.createGroup(ctx, begin, groupType, name, memberLimit, isStreamPublic, canSearch,
                canViewMembers, canJoin, canMemberInvite, canMemberApprove, canMemberPost, canMemberQuit, needInvitedConfirm, Long.parseLong(viewerId), label, properties, sendPost);

        RecordSet recs = new RecordSet();
        if (needInvitedConfirm == 1) {
            L.debug(ctx, "Begin borqs user invite = " + DateUtils.nowMillis());
            String uids = StringUtils2.joinIgnoreBlank(",", borqsIds);
            try {
                RecordSet recs0 = g.inviteMembers(ctx, groupId, uids, message, sendEmail, sendSms);
                recs.addAll(recs0);
            } catch (Exception ne) {
                L.error(ctx, ne, "Fail to invite borqs user = " + uids);
            }
            L.debug(ctx, "End borqs user invite = " + DateUtils.nowMillis());

            L.debug(ctx, "Begin email or sms invite = " + DateUtils.nowMillis());
            int identifySize = identifies.size();
            for (int i = 0; i < identifySize; i++) {
                final String identify = identifies.get(i);
                try {
                    Record r = g.inviteMember(ctx, groupId, identify, names.get(i), message, sendEmail, sendSms);
                    r.put("key", identify);
                    recs.add(r);
                } catch (Exception ne) {
                    L.error(ctx, ne, "Fail to invite email or sms = " + identify);
                }
            }
            L.debug(ctx, "End email or sms invite = " + DateUtils.nowMillis());
        } else {
            Record membersRec = new Record();
            for (String borqsId : borqsIds)
                membersRec.put(borqsId, ROLE_MEMBER);

            List<String> excludes = StringUtils2.splitList(g.getCreatorAndAdmins(ctx, groupId), ",", true);
            for (String exclude : excludes)
                membersRec.remove(exclude);
            RecordSet recs0 = g.addMembers(ctx, groupId, membersRec, false);
            recs.addAll(recs0);

            L.debug(ctx, "Begin email or sms invite = " + DateUtils.nowMillis());
            int identifySize = identifies.size();
            for (int i = 0; i < identifySize; i++) {
                final String identify = identifies.get(i);
                try {
                    Record r = g.inviteMember(ctx, groupId, identify, names.get(i), message, sendEmail, sendSms);
                    r.put("key", identify);
                    recs.add(r);
                } catch (Exception ne) {
                    L.error(ctx, ne, "Fail to invite email or sms = " + identify);
                }
            }
            L.debug(ctx, "End email or sms invite = " + DateUtils.nowMillis());
        }

        Record rs = new Record();
        rs.put("result", groupId);
        rs.put("group_id", groupId);
        rs.put("users", recs);

        if (StringUtils.equals(groupType, TYPE_EVENT)) {
//            rs.put("parent_ids", parentIds);
            rs.put("parent_ids", qp.getString("parent_ids", ""));
        }

        //Borqs Staff
        /*boolean borqsStaff = qp.getBoolean("borqs_staff", false);
        if (borqsStaff) {
            addBorqsStaffsToGroup(ctx, groupId);
        }*/

        return rs;
    }

    @WebMethod("v2/public_circle/create")
    public Record createPublicCircle(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);


        // check page_id
        long pageId = qp.getInt("page_id", 0);
        long formal = qp.getInt("formal", Constants.PUBLIC_CIRCLE_FORMAL_FREE);
        Record newPageRec = new Record();
        if (pageId > 0) {
            Record pageRec = GlobalLogics.getPage().getPageNoThrow(ctx, pageId);
            if (pageRec == null)
                throw new ServerException(WutongErrors.PAGE_ILLEGAL, "Illegal page_id");

            newPageRec.put("page_id", pageRec.getInt("page_id"));
            if (formal == 0) {
                String freeCircleIds = pageRec.getString("free_circle_ids");
                if (StringUtils.isNotEmpty(freeCircleIds))
                    throw new ServerException(WutongErrors.PAGE_ASSOCIATED, "The page is not free");
            } else {
                long associatedId = pageRec.getInt("associated_id", 0L);
                if (associatedId > 0)
                    throw new ServerException(WutongErrors.PAGE_ASSOCIATED, "The page is not free");
            }
        }

        long parentCircleId = qp.getInt("parent_id", 0);
        if (parentCircleId > 0) {
            Record parentCircleRec = GlobalLogics.getGroup().getGroup(ctx, ctx.getViewerIdString(), parentCircleId, "formal, circle_ids", false);
            if (MapUtils.isEmpty(parentCircleRec))
                throw new ServerException(WutongErrors.GROUP_NOT_EXISTS);
            if (parentCircleRec.getInt("formal", Constants.PUBLIC_CIRCLE_FORMAL_FREE) != Constants.PUBLIC_CIRCLE_FORMAL_TOP)
                throw new ServerException(WutongErrors.GROUP_NOT_FORMAL);
        }

        Record circleRec = createGroup(ctx, PUBLIC_CIRCLE_ID_BEGIN, TYPE_PUBLIC_CIRCLE, qp);


        long publicCircleId = circleRec.getInt("group_id");

        if (pageId > 0) {
            if (formal == Constants.PUBLIC_CIRCLE_FORMAL_FREE || formal == Constants.PUBLIC_CIRCLE_FORMAL_SUB) {
                newPageRec.put("free_circle_ids", publicCircleId);
            } else if (formal == Constants.PUBLIC_CIRCLE_FORMAL_TOP) {
                newPageRec.put("associated_id", publicCircleId);
            }
            GlobalLogics.getPage().updatePage(ctx, newPageRec, true, false);
        }

        if (parentCircleId > 0) {
            Record parentCircleRec = GlobalLogics.getGroup().getGroup(ctx, ctx.getViewerIdString(), parentCircleId, "formal, circle_ids", false);
            if (MapUtils.isNotEmpty(parentCircleRec)) {
                String parentCircleIds = parentCircleRec.getString("circle_ids", "");
                parentCircleIds = StringUtils2.addIntToSet(parentCircleIds, publicCircleId);
                GlobalLogics.getGroup().updateGroupSimple(ctx, parentCircleId, new Record(), Record.of("circle_ids", parentCircleIds));
            }
        }

        return circleRec;
    }

    private boolean updateGroup(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String viewerId = ctx.getViewerIdString();

        Record info = new Record();
        if (qp.containsKey("privacy")) {
            int isStreamPublic = 1;
            int canSearch = 1;
            int canViewMembers = 1;
            int privacy = (int) qp.getInt("privacy", GRP_PRIVACY_OPEN);
            if (privacy == GRP_PRIVACY_CLOSED) {
                isStreamPublic = 0;
            } else if (privacy == GRP_PRIVACY_SECRET) {
                isStreamPublic = 0;
                canSearch = 0;
                canViewMembers = 0;
            }

            info.put(GRP_COL_IS_STREAM_PUBLIC, isStreamPublic);
            info.put(GRP_COL_CAN_SEARCH, canSearch);
            info.put(GRP_COL_CAN_VIEW_MEMBERS, canViewMembers);
        }

        Record properties = new Record();

        String[] buildInParams = new String[]{"sign_method", "sign", "appid", "ticket", "circle_id", "privacy", "call_id"};
        String[] groupParams = new String[]{GRP_COL_NAME, GRP_COL_MEMBER_LIMIT, GRP_COL_IS_STREAM_PUBLIC, GRP_COL_CAN_SEARCH,
                GRP_COL_CAN_VIEW_MEMBERS, GRP_COL_CAN_JOIN, GRP_COL_CAN_MEMBER_INVITE, GRP_COL_CAN_MEMBER_APPROVE, GRP_COL_CAN_MEMBER_POST, GRP_COL_CAN_MEMBER_QUIT, GRP_COL_NEED_INVITED_CONFIRM, GRP_COL_LABEL};

        for (Map.Entry<String, Object> entry : qp.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!ArrayUtils.contains(buildInParams, key)) {
                if (ArrayUtils.contains(groupParams, key))
                    info.put(key, value);
                else
                    properties.put(key, value);
            }
        }

        if (properties.has(COMM_COL_BULLETIN))
            properties.put(COMM_COL_BULLETIN_UPDATED_TIME, DateUtils.nowMillis());

        return g.updateGroup(ctx, groupId, info, properties);
    }

    @WebMethod("v2/public_circle/update")
    public boolean updatePublicCircle(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return updateGroup(ctx, groupId, qp);
    }

    private boolean uploadGroupProfileImage(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String viewerId = ctx.getViewerIdString();

        if (!g.hasRight(ctx, groupId, ctx.getViewerId(), ROLE_ADMIN))
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "You do not have right to upload group profile image");

        FileItem fi = qp.checkGetFile("profile_image");

        long uploaded_time = DateUtils.nowMillis();
        String imageName = "profile_" + groupId + "_" + uploaded_time;
        String loc = ctx.getLocation();

        PhotoLogic photo = GlobalLogics.getPhoto();
        String album_id = qp.getString("album_id", "");
        if (StringUtils.isEmpty(album_id))
            album_id = photo.getAlbum(ctx, String.valueOf(groupId), photo.ALBUM_TYPE_PROFILE, "Profile Pictures");
        if (!photo.isAlbumExist(ctx, album_id)) {
            throw new ServerException(WutongErrors.PHOTO_ALBUM_NOT_EXISTS, "album not exist, please create album first");
        }

        String sfn = imageName + "_S.jpg";
        String ofn = imageName + "_M.jpg";
        String lfn = imageName + "_L.jpg";
        String tfn = imageName + "_T.jpg";

        if (photoStorage instanceof OssSFS) {
            lfn = "media/photo/" + lfn;
            ofn = "media/photo/" + ofn;
            sfn = "media/photo/" + sfn;
            tfn = "media/photo/" + tfn;
        }

        SFSUtils.saveScaledUploadImage(fi, photoStorage, sfn, "50", "50", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, ofn, "80", "80", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, lfn, "180", "180", "jpg");
        SFSUtils.saveScaledUploadImage(fi, photoStorage, tfn, "120", "120", "jpg");

        String photoID = Long.toString(RandomUtils.generateId());
        Record rc_photo = new Record();
        rc_photo.put("photo_id", photoID);
        rc_photo.put("album_id", album_id);
        rc_photo.put("user_id", String.valueOf(groupId));
        rc_photo.put("img_middle", imageName + "_M.jpg");
        rc_photo.put("img_original", imageName + "_M.jpg");
        rc_photo.put("img_big", imageName + "_L.jpg");
        rc_photo.put("img_small", imageName + "_S.jpg");
        rc_photo.put("caption", "profile_image");
        rc_photo.put("location", loc);
        rc_photo.put("created_time", DateUtils.nowMillis());
        photo.saveUploadPhoto(ctx, rc_photo);

        Record properties = Record.of("image_url", imageName + "_M.jpg", "small_image_url", imageName + "_S.jpg",
                "large_image_url", imageName + "_L.jpg",
                "original_image_url", imageName + "_M.jpg");
        return g.updateGroup(ctx, groupId, new Record(), properties);
    }

    @WebMethod("v2/public_circle/upload_profile_image")
    public boolean uploadPublicCircleProfileImage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return uploadGroupProfileImage(ctx, groupId, qp);
    }

    @WebMethod("v2/public_circle/destroy")
    public boolean destroyPublicCircle(QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        Context ctx = WutongContext.getContext(qp, true);

        String groupIds = getParamString(qp, "circle_ids", "ids", true, "");
        for (long groupId : StringUtils2.splitIntArray(groupIds, ",")) {
            Record groupRec = g.getGroup(ctx, ctx.getViewerIdString(), groupId, "page_id, parent_id, circle_ids", false);
            if (MapUtils.isNotEmpty(groupRec)) {
                long pageId = groupRec.getInt("page_id");
                if (pageId > 0) {
                    Record pageRec = GlobalLogics.getPage().getPageNoThrow(ctx, pageId);
                    if (MapUtils.isNotEmpty(pageRec)) {
                        if (pageRec.getInt("associated_id", 0) == groupId) {
                            GlobalLogics.getPage().updatePage(ctx, Record.of("page_id", pageId, "associated_id", 0L), true, false);
                        }
                        long[] freeCircleIds = StringUtils2.splitIntArray(pageRec.getString("free_circle_ids"), ",");
                        if (ArrayUtils.contains(freeCircleIds, pageId)) {
                            freeCircleIds = ArrayUtils.removeElement(freeCircleIds, groupId);
                            GlobalLogics.getPage().updatePage(ctx, Record.of("page_id", pageId, "free_circle_ids", StringUtils2.join(freeCircleIds, ",")), true, false);
                        }
                    }
                }

                long parentId = groupRec.getInt("parent_id");
                if (parentId > 0) {
                    Record parentCircleRec = GlobalLogics.getGroup().getGroup(ctx, ctx.getViewerIdString(), parentId, "circle_ids", false);
                    if (MapUtils.isNotEmpty(parentCircleRec)) {
                        String parentSubCircleIds = parentCircleRec.getString("circle_ids");
                        parentSubCircleIds = StringUtils2.removeIntFromSet(parentSubCircleIds, groupId);
                        GlobalLogics.getGroup().updateGroupSimple(ctx, parentId, new Record(), Record.of("circle_ids", parentSubCircleIds));
                    }
                }

                String subCircleIds = groupRec.getString("circle_ids");
                LinkedHashSet<Long> removingCircleIds = new LinkedHashSet<Long>();

                if (StringUtils.isNotEmpty(subCircleIds)) {
                    RecordSet subCircleRecs = GlobalLogics.getGroup().getGroups(ctx,
                            Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END,
                            ctx.getViewerIdString(),
                            subCircleIds,
                            "formal", false);
                    for (Record subCircleRec : subCircleRecs) {
                        if (subCircleRec.getInt("formal") == Constants.PUBLIC_CIRCLE_FORMAL_SUB)
                            removingCircleIds.add(subCircleRec.getInt("id"));
                    }
                }
                //add by wangpeng at 2013-04-22
                if (!removingCircleIds.contains(groupRec.getInt("id")))
                    removingCircleIds.add(groupRec.getInt("id"));

                if (!removingCircleIds.isEmpty()) {
                    GlobalLogics.getGroup().destroyGroup(ctx, StringUtils.join(removingCircleIds, ","));
                }
            }
        }
        return g.destroyGroup(ctx, groupIds);
    }

    private Record getCircleProfileEvents(Context ctx, long circleId) {
        Record profileEvents = new Record();
        GroupLogic g = GlobalLogics.getGroup();
        Record circleRec = g.getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "event_ids").getFirstRecord();
        String eventIds = circleRec.getString("event_ids", "");
        if (StringUtils.isBlank(eventIds)) {
            profileEvents.put("count", 0);
            profileEvents.put("events", JsonNodeFactory.instance.arrayNode());
            return profileEvents;
        }

        List<String> l = StringUtils2.splitList(eventIds, ",", true);
        String eventId = l.get(l.size() - 1);
        String columns = GROUP_LIGHT_COLS + ",start_time,end_time,parent_ids";
        RecordSet recs = g.getGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, ctx.getViewerIdString(), eventId, columns, false, -1, -1);
        recs.renameColumn(GRP_COL_ID, "circle_id");
        recs.renameColumn(GRP_COL_NAME, "circle_name");
        profileEvents.put("count", l.size());
        profileEvents.put("events", recs);
        return profileEvents;
    }

    private Record getCircleProfilePolls(Context ctx, long circleId) {
        PollLogic pollLogic = GlobalLogics.getPoll();
//        long count = pollLogic.getRelatedPollCount(ctx, ctx.getViewerIdString(), String.valueOf(circleId));
//        RecordSet polls = pollLogic.getInvolvedPollsPlatform(ctx, ctx.getViewerIdString(), String.valueOf(circleId), 0, 2);

        Record circleRec = GlobalLogics.getGroup().getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "poll_ids").getFirstRecord();
        String pollIds = circleRec.getString("poll_ids", "");
        int count = StringUtils2.splitSet(pollIds, ",", true).size();
        if (count > 2) {
            String pollId1 = StringUtils.substringAfterLast(pollIds, ",");
            String s = StringUtils.substringBeforeLast(pollIds, ",");
            String pollId2 = StringUtils.substringAfterLast(s, ",");
            pollIds = pollId1 + "," + pollId2;
        }
        RecordSet polls = pollLogic.getPolls(ctx, ctx.getViewerIdString(), pollIds, false);

        Record profilePolls = new Record();
        profilePolls.put("count", count);
        profilePolls.put("polls", polls);
        return profilePolls;
    }

    private RecordSet getGroups(Context ctx, long begin, long end, String groupIds, QueryParams qp) {
        boolean isSingle = StringUtils.isNotBlank(groupIds) && !StringUtils.contains(groupIds, ",");
        GroupLogic g = GlobalLogics.getGroup();
        String viewerId = ctx.getViewerIdString();
        String cols = qp.getString("columns", "");
        String columns = StringUtils.isBlank(cols) ? GROUP_LIGHT_COLS : GROUP_LIGHT_COLS + "," + cols;
        boolean withMembers = qp.getBoolean("with_members", false);
        if (!isSingle) {
            RecordSet recs = g.getGroups(ctx, begin, end, viewerId, groupIds, columns, withMembers, (int) qp.getInt("page", -1), (int) qp.getInt("count", -1));
            recs.renameColumn(GRP_COL_ID, "circle_id");
            recs.renameColumn(GRP_COL_NAME, "circle_name");
            return recs;
        } else {
            Record rec = g.getGroup(ctx, viewerId, Long.parseLong(groupIds), columns, withMembers);

            if (!rec.isEmpty()) {
                rec.renameColumn(GRP_COL_ID, "circle_id");
                rec.renameColumn(GRP_COL_NAME, "circle_name");

                // return three status 5 users and three status count
                int count = (int) qp.getInt("profile_user_count", 5);
                Record appliedCount = g.getUsersCounts(ctx, groupIds, STATUS_APPLIED);
                Record invitedCount = g.getUsersCounts(ctx, groupIds, STATUS_INVITED);

                long groupId = rec.getInt("circle_id");

                rec.put("applied_count", appliedCount.getInt(String.valueOf(groupId), 0));
                rec.put("invited_count", invitedCount.getInt(String.valueOf(groupId), 0));

                RecordSet members = g.getGroupUsersByStatus(ctx, groupId, String.valueOf(STATUS_JOINED), -1, -1, "");
                RecordSet applied = g.getGroupUsersByStatus(ctx, groupId, String.valueOf(STATUS_APPLIED), 0, count, "");
                RecordSet invited = g.getGroupUsersByStatus(ctx, groupId, String.valueOf(STATUS_INVITED), -1, -1, "");
                rec.put("profile_members", members.size() > count ? members.subList(0, count) : members);
                rec.put("profile_applied", applied);
                rec.put("profile_invited", invited.size() > count ? invited.subList(0, count) : invited);

                RecordSet friends = GlobalLogics.getFriendship().getFriends(ctx, viewerId, Integer.toString(FRIENDS_CIRCLE), -1, -1);
                List<String> memberIds = StringUtils2.splitList(members.joinColumnValues("user_id", ","), ",", true);
                List<String> invitedIds = StringUtils2.splitList(invited.joinColumnValues("user_id", ","), ",", true);
                List<String> friendIds0 = StringUtils2.splitList(friends.joinColumnValues("friend", ","), ",", true);
                List<String> friendIds1 = new ArrayList<String>();
                friendIds1.addAll(friendIds0);
                List<String> l = new ArrayList<String>();
                friendIds0.retainAll(memberIds);
                friendIds1.retainAll(invitedIds);
                l.addAll(friendIds0);
                l.addAll(friendIds1);
                rec.put("invited_ids", StringUtils2.joinIgnoreBlank(",", l));

                //latest post
//            Record post = p.getFullUsersTimelineForQiuPu(viewerId, String.valueOf(groupId), 0, DateUtils.nowMillis(), ALL_POST, String.valueOf(APP_TYPE_BPC), 0, 1).getFirstRecord();
//            rec.put("latest_post", post);

                //latest event
                /*rec.put("profile_events", getCircleProfileEvents(ctx, groupId));
                rec.put("profile_polls", getCircleProfilePolls(ctx, groupId));*/
                rec.put("profile_events", JsonNodeFactory.instance.objectNode());
                rec.put("profile_polls", JsonNodeFactory.instance.objectNode());
                return RecordSet.of(rec);
            } else {
                return new RecordSet();
            }
        }
    }

    @WebMethod("v2/public_circle/show")
    public RecordSet getPublicCircles(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        String groupIds = getParamString(qp, "circle_ids", "ids", false, "");
        return getGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, groupIds, qp);
    }

    @WebMethod("v2/public_circle/top/show")
    public RecordSet getTopCircles(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String circleIds = GlobalLogics.getGroup().getTopCircleIds(ctx);
        return getGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, circleIds, qp);
    }

    @WebMethod("v2/public_circle/subcircles/show")
    public RecordSet getSubCircles(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = qp.checkGetInt("circle_id");
        String formal = qp.getString("formal", "all");
        Record rec = GlobalLogics.getGroup().getGroup(ctx, ctx.getViewerIdString(), groupId, "circle_ids", false);
        String circleIds = rec.getString("circle_ids");

        boolean inCircles = qp.getBoolean("in_circles", false);
        if (inCircles) {
            Map<Long, Integer> map = GlobalLogics.getGroup().getRolesWithGroups(ctx, StringUtils2.splitIntArray(circleIds, ","), ctx.getViewerId());
            if (map.size() == 0)
                circleIds = "";
            ArrayList<Long> l = new ArrayList<Long>();
            for (Map.Entry<Long, Integer> entry : map.entrySet()) {
                long circleId = entry.getKey();
                int role = entry.getValue();
                if (role >= Constants.ROLE_MEMBER) {
                    l.add(circleId);
                }
                circleIds = StringUtils2.joinIgnoreBlank(",", l);
            }
        }

        if (StringUtils.isNotBlank(circleIds)) {
            qp.put("columns", qp.getString("columns", "") + ",formal");
            RecordSet groupRecs = getGroups(ctx,
                    Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END,
                    circleIds, qp);
            if ("all".equalsIgnoreCase(formal)) {
                return groupRecs;
            } else if (Integer.toString(Constants.PUBLIC_CIRCLE_FORMAL_FREE).equals(formal)) {
                RecordSet groupRecs1 = new RecordSet();
                for (Record groupRec : groupRecs) {
                    if (groupRec.getInt("formal", 0) == Constants.PUBLIC_CIRCLE_FORMAL_FREE)
                        groupRecs1.add(groupRec);
                }
                return groupRecs1;
            } else if (Integer.toString(Constants.PUBLIC_CIRCLE_FORMAL_SUB).equals(formal)) {
                RecordSet groupRecs1 = new RecordSet();
                for (Record groupRec : groupRecs) {
                    if (groupRec.getInt("formal", 0) == Constants.PUBLIC_CIRCLE_FORMAL_SUB)
                        groupRecs1.add(groupRec);
                }
                return groupRecs1;
            } else {
                throw new ServerException(BaseErrors.PLATFORM_ILLEGAL_PARAM, "formal error");
            }
        } else {
            return new RecordSet();
        }
    }

    @WebMethod("v2/public_circle/detail")
    public Record getPublicCircleDetail(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        String groupId = getParamString(qp, "circle_id", "id", false, "");
        RecordSet recs = getGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, groupId, qp);
        if (recs.isEmpty()) {
            throw new ServerException(WutongErrors.GROUP_NOT_EXISTS, "The public circle is not exist");
        } else {
            return recs.get(0);
        }
    }

    private RecordSet getGroupUsers(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String viewerId = ctx.getViewerIdString();

        boolean admins = qp.getBoolean("admins", false);
        String status = qp.getString("status", String.valueOf(STATUS_JOINED));
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 100);

        if (admins)
            return RecordSet.of(Record.of("admins", g.getCreatorAndAdmins(ctx, groupId)));
        else {
            String searchKey = qp.getString("key", "");
            return g.getGroupUsersByStatus(ctx, groupId, status, page, count, searchKey);
        }
    }

    /*private RecordSet getGroupUsersUnion(long groupId, QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String viewerId = "0";
        String ticket = qp.getString("ticket", "");
        if (StringUtils.isNotBlank(ticket))
            viewerId = p.checkSignAndTicket(qp);

        boolean admins = qp.getBoolean("admins", false);
        String status = qp.getString("status", String.valueOf(GroupConstants.STATUS_JOINED));
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 100);

        if (admins)
            return RecordSet.of(Record.of("admins", p.getCreatorAndAdmins(groupId)));
        else {
            String searchKey = qp.getString("key", "");
            return p.getGroupUsersByStatusUnion(viewerId, groupId, status, page, count, searchKey);
        }
    }*/
    @WebMethod("v2/public_circle/users")
    public RecordSet getPublicCircleUsers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);

        GroupLogic group = GlobalLogics.getGroup();
        Record groupProperty = group.getGroup(ctx, ctx.getViewerIdString(), groupId, "view_phone", false);
        RecordSet rs = getGroupUsers(ctx, groupId, qp);

        if (groupProperty.getString("view_phone", "true").equals("false")) {
            for (Record r : rs) {
                if (r.has("mobile_tel"))
                    r.remove("mobile_tel");
            }
        }
        return rs;
    }

    private RecordSet groupInvite(Context ctx, long groupId, QueryParams qp) {
        // TODO: check members
        GroupLogic g = GlobalLogics.getGroup();
        String viewerId = ctx.getViewerIdString();
        String ua = ctx.getUa();
        String loc = ctx.getLocation();
        String tos = qp.checkGetString("to");
        String sNames = qp.checkGetString("names");
        List<String> toIds = StringUtils2.splitList(tos, ",", true);
        List<String> names = StringUtils2.splitList(sNames, ",", true);

        List<String> borqsIds = new ArrayList<String>();
        List<String> identifies = new ArrayList<String>();
        List<String> identifyNames = new ArrayList<String>();
        List<String> virtuals = new ArrayList<String>();
        int size = toIds.size();

        for (int i = 0; i < size; i++) {
            String toId = toIds.get(i);
            String toName = names.get(i);

            Record typeRec = g.getTypeByStr(ctx, toId);
            int type = (int) typeRec.getInt("type");
            toId = typeRec.getString("str");
            if (type == IDENTIFY_TYPE_BORQS_ID)
                borqsIds.add(toId);
            else if (type == IDENTIFY_TYPE_LOCAL_CIRCLE) {
                String circleId = toId;
                RecordSet friendRecs = GlobalLogics.getFriendship().getFriends(ctx, viewerId, circleId, 0, -1);
                String userIds = friendRecs.joinColumnValues("friend", ",");
                RecordSet users = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userIds);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    int lcIdType = (int) (g.getTypeByStr(ctx, userId).getInt("type"));
                    if (lcIdType == IDENTIFY_TYPE_BORQS_ID)
                        borqsIds.add(userId);
                    else if (lcIdType == IDENTIFY_TYPE_VIRTUAL_ID)
                        virtuals.add(userId);
                }
            } else if (type == IDENTIFY_TYPE_GROUP_ID) {
                String fromGroup = toId;
                String userIds = g.getAllMembers(ctx, Long.parseLong(fromGroup), -1, -1, "");
                RecordSet users = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userIds);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    borqsIds.add(userId);
                }
            } else if (type == IDENTIFY_TYPE_PAGE_ID) {
                String fromPage = toId;
                long[] followerIds = GlobalLogics.getFriendship().getFollowerIds(ctx, Long.parseLong(fromPage), 0, Constants.GROUP_ID_BEGIN, -1, -1);
                String userIds = StringUtils2.joinIgnoreBlank(",", Arrays.asList(ArrayUtils.toObject(followerIds)));
                RecordSet users = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userIds);
                for (Record user : users) {
                    String userId = user.getString("user_id");
                    borqsIds.add(userId);
                }
            } else if (type == IDENTIFY_TYPE_VIRTUAL_ID)
                virtuals.add(toId);
            else {
                identifies.add(toId);
                identifyNames.add(toName);
            }
        }

        String virtualIds = StringUtils2.joinIgnoreBlank(",", virtuals);
        if (StringUtils.isNotBlank(virtualIds)) {
            RecordSet vUsers = GlobalLogics.getFriendship().getContactFriendByFid(ctx, virtualIds);
            for (Record vUser : vUsers) {
                identifies.add(vUser.getString("content"));
                identifyNames.add(vUser.getString("name"));
            }
        }

        String message = qp.getString("message", "");
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        boolean sendEmail = qp.getBoolean("send_email", false);
        boolean sendSms = qp.getBoolean("send_sms", false);

        RecordSet recs = new RecordSet();

        Record groupRec = g.getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, GROUP_ID_END,
                String.valueOf(groupId), GRP_COL_NEED_INVITED_CONFIRM + "," + GRP_COL_NAME).getFirstRecord();
        long needInvitedConfirm = groupRec.getInt(GRP_COL_NEED_INVITED_CONFIRM, 1);
        String groupName = groupRec.getString(GRP_COL_NAME);
        Record source = GlobalLogics.getAccount().getUsersBaseColumns(ctx, viewerId).getFirstRecord();
        String viewerName = source.getString("display_name");

        if (needInvitedConfirm == 1) {
            L.debug(ctx, "Begin borqs user invite = " + DateUtils.nowMillis());
            String uids = StringUtils2.joinIgnoreBlank(",", borqsIds);
            try {
                RecordSet recs0 = g.inviteMembers(ctx, groupId, uids, message, sendEmail, sendSms);
                recs.addAll(recs0);
            } catch (Exception ne) {
                L.error(ctx, ne, "Fail to invite borqs user = " + uids);
            }
            L.debug(ctx, "End borqs user invite = " + DateUtils.nowMillis());

            L.debug(ctx, "Begin email or sms invite = " + DateUtils.nowMillis());
            int identifySize = identifies.size();
            for (int i = 0; i < identifySize; i++) {
                final String identify = identifies.get(i);
                try {
                    Record r = g.inviteMember(ctx, groupId, identify, names.get(i), message, sendEmail, sendSms);
                    r.put("key", identify);
                    recs.add(r);
                } catch (Exception ne) {
                    L.error(ctx, ne, "Fail to invite email or sms = " + identify);
                }
            }
            L.debug(ctx, "End email or sms invite = " + DateUtils.nowMillis());
        } else {
            Record membersRec = new Record();
            for (String borqsId : borqsIds)
                membersRec.put(borqsId, ROLE_MEMBER);

            List<String> excludes = StringUtils2.splitList(g.getCreatorAndAdmins(ctx, groupId), ",", true);
            for (String exclude : excludes)
                membersRec.remove(exclude);
            RecordSet recs0 = g.addMembers(ctx, groupId, membersRec, false);
            recs.addAll(recs0);
            String groupType = g.getGroupTypeStr(ctx, ctx.getLanguage(), groupId);
            Commons.sendNotification(ctx, NTF_GROUP_INVITE,
                    Commons.createArrayNodeFromStrings(appId),
                    Commons.createArrayNodeFromStrings(viewerId),
                    Commons.createArrayNodeFromStrings(viewerName, groupType, groupName, "将"),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(),
                    Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                    Commons.createArrayNodeFromStrings(viewerName, groupType, groupName, viewerId, String.valueOf(groupId), "将"),
                    Commons.createArrayNodeFromStrings(message),
                    Commons.createArrayNodeFromStrings(message),
                    Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                    Commons.createArrayNodeFromStrings(StringUtils2.joinIgnoreBlank(",", borqsIds))
            );
            g.sendGroupNotification(ctx, groupId, new RecordSet(), new GroupInviteNotifSender(), viewerId, new Object[]{StringUtils2.joinIgnoreBlank(",", borqsIds)}, message,
                    viewerName, groupType, groupName, "将");

            L.debug(ctx, "Begin email or sms invite = " + DateUtils.nowMillis());
            int identifySize = identifies.size();
            for (int i = 0; i < identifySize; i++) {
                final String identify = identifies.get(i);
                try {
                    Record r = g.inviteMember(ctx, groupId, identify, names.get(i), message, sendEmail, sendSms);
                    r.put("key", identify);
                    recs.add(r);
                } catch (Exception ne) {
                    L.error(ctx, ne, "Fail to invite email or sms = " + identify);
                }
            }
            L.debug(ctx, "End email or sms invite = " + DateUtils.nowMillis());
        }

        //Borqs Staff
        boolean borqsStaff = qp.getBoolean("borqs_staff", false);
        if (borqsStaff) {
            addBorqsStaffsToGroup(ctx, groupId);
        }

        return recs;
    }

    @WebMethod("v2/public_circle/invite")
    public RecordSet publicCircleInvite(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return groupInvite(ctx, groupId, qp);
    }

    private RecordSet groupApprove(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String userIds = qp.checkGetString("user_ids");
        List<String> users = StringUtils2.splitList(userIds, ",", true);
        int size = users.size();

        RecordSet recs = new RecordSet();
        for (int i = 0; i < size; i++) {
            Record r = g.approveMember(ctx, groupId, users.get(i));
            r.put("key", users.get(i));
            recs.add(r);
        }
        return recs;
    }

    @WebMethod("v2/public_circle/approve")
    public NoResponse publicCircleApprove(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        RecordSet recs = groupApprove(ctx, groupId, qp);

        if (qp.containsKey("from_email")) {
            String notice = "Operate Success!";
            String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });

            resp.setContentType("text/html");
            resp.getWriter().print(html);
        } else {
            output(qp, req, resp, recs.toString(false, false), 200, "text/plain");
        }

        return NoResponse.get();
    }

    private RecordSet groupIgnore(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String userIds = qp.checkGetString("user_ids");
        List<String> users = StringUtils2.splitList(userIds, ",", true);
        int size = users.size();

        RecordSet recs = new RecordSet();
        for (int i = 0; i < size; i++) {
            Record r = g.ignoreMember(ctx, groupId, users.get(i));
            r.put("key", users.get(i));
            recs.add(r);
        }
        return recs;
    }

    @WebMethod("v2/public_circle/ignore")
    public NoResponse publicCircleIgnore(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        RecordSet recs = groupIgnore(ctx, groupId, qp);

        if (qp.containsKey("from_email")) {
            String notice = "Operate Success!";
            String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                    {"host", serverHost},
                    {"notice", notice}
            });

            resp.setContentType("text/html");
            resp.getWriter().print(html);
        } else {
            output(qp, req, resp, recs.toString(false, false), 200, "text/plain");
        }

        return NoResponse.get();
    }

    private void onPublicCircleJoined(Context ctx, long userId, long groupId) {
        RecordSet groupRecs = GlobalLogics.getGroup().getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(groupId), "page_id,formal");
        if (CollectionUtils.isNotEmpty(groupRecs)) {
            Record groupRec = groupRecs.getFirstRecord();
            if (groupRec.getInt("formal", PUBLIC_CIRCLE_FORMAL_FREE) == PUBLIC_CIRCLE_FORMAL_TOP) {
                long pageId = groupRec.getInt("page_id", 0);
                if (getUserTypeById(pageId) == PAGE_OBJECT) {
                    GlobalLogics.getFriendship().followPage(ctx, userId, pageId);
                }
            }
        }
    }

    @WebMethod("v2/public_circle/upload_employees")
    public RecordSet uploadEmployees(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle_id");
        boolean merge = qp.getBoolean("merge", false);
        FileItem excelFile = qp.checkGetFile("file");
        return GlobalLogics.getGroup().uploadEmployees(ctx, circleId, excelFile, merge);
    }

    @WebMethod("v2/public_circle/add_employee")
    public int addEmployee(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle_id");
        String name = qp.checkGetString("name");
        String email = qp.checkGetString("email");

        GroupLogic g = GlobalLogics.getGroup();
        Record other = new Record();
        other.put(Constants.EMP_COL_NAME, name);
        other.put(Constants.EMP_COL_EMAIL, email);
        other.put(Constants.EMP_COL_EMPLOYEE_ID, qp.getString("employee_id", ""));
        other.put(Constants.EMP_COL_DEPARTMENT, qp.getString("department", ""));
        other.put(Constants.EMP_COL_JOB_TITLE, qp.getString("job_title", ""));
        other.put(Constants.EMP_COL_TEL, qp.getString("tel", ""));
        other.put(Constants.EMP_COL_MOBILE_TEL, qp.getString("mobile_tel", ""));
        return g.addEmployee(ctx, circleId, other);
    }

    @WebMethod("v2/public_circle/update_employee")
    public boolean updateEmployee(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle_id");
        String email = qp.checkGetString("email");


        GroupLogic g = GlobalLogics.getGroup();
        Record other = new Record();
        if (qp.containsKey("name"))
            other.put(Constants.EMP_COL_NAME, qp.checkGetString("name"));
        if (qp.containsKey("name_en"))
            other.put(Constants.EMP_COL_NAME_EN, qp.checkGetString("name_en"));
        if (qp.containsKey("employee_id"))
            other.put(Constants.EMP_COL_EMPLOYEE_ID, qp.checkGetString("employee_id"));
        if (qp.containsKey("department"))
            other.put(Constants.EMP_COL_DEPARTMENT, qp.checkGetString("department"));
        if (qp.containsKey("job_title"))
            other.put(Constants.EMP_COL_JOB_TITLE, qp.checkGetString("job_title"));
        if (qp.containsKey("tel"))
            other.put(Constants.EMP_COL_TEL, qp.checkGetString("tel"));
        if (qp.containsKey("mobile_tel"))
            other.put(Constants.EMP_COL_MOBILE_TEL, qp.checkGetString("mobile_tel"));
        return g.updateEmployee(ctx, circleId, email, other);
    }

    @WebMethod("v2/public_circle/delete_employees")
    public boolean deleteEmployees(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle_id");
        String[] emails = StringUtils2.splitArray(qp.checkGetString("emails"), ",", true);
        String userIds = qp.getString("userIds", "");

        GroupLogic g = GlobalLogics.getGroup();
        return g.deleteEmployees(ctx, circleId, userIds, emails);
    }

    @WebMethod("v2/public_circle/join")
    public int publicCircleJoin(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        int status = g.addMember(ctx, groupId, ctx.getViewerIdString(), qp.getString("message", ""), qp.getBoolean("send_post", true));
        if (status == STATUS_NONE)
            throw new ServerException(WutongErrors.GROUP_CANNOT_APPLY, "The public circle can not apply to join!");

        onPublicCircleJoined(ctx, ctx.getViewerId(), groupId);
        return status;
    }

    @WebMethod("v2/group/deal_invite")
    public NoResponse dealPublicCircleInvite(QueryParams qp, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);
        GroupLogic g = GlobalLogics.getGroup();
        String userId = qp.getString("user_id", "0");
        String source = qp.checkGetString("source");
        long groupId = qp.checkGetInt("group_id");
        boolean accept = qp.getBoolean("accept", false);

        int status;
        if (!StringUtils.equals(userId, "0")) {
            status = g.dealGroupInvite(ctx, groupId, userId, source, accept);
            onPublicCircleJoined(ctx, Long.parseLong(userId), groupId);
        } else {
            String name = qp.checkGetString("name");
            String identify = qp.checkGetString("identify");
            status = g.rejectGroupInviteForIdentify(ctx, groupId, name, identify, source);
        }

        String notice = "操作";
        if (accept && (status == STATUS_JOINED)) {
            notice += "成功！";
        } else if (!accept && (status == STATUS_REJECTED)) {
            notice += "成功！";
        } else {
            notice += "失败！";
        }

        String html = pageTemplate.merge("notice.freemarker", new Object[][]{
                {"host", serverHost},
                {"notice", notice}
        });

        resp.setContentType("text/html");
        resp.getWriter().print(html);

        return NoResponse.get();
    }

    @WebMethod("v2/group/invite_page")
    public DirectResponse groupInvitePage(QueryParams qp) throws UnsupportedEncodingException {

        String displayName = URLDecoder.decode(qp.checkGetString("display_name"), "utf-8");
        String fromName = URLDecoder.decode(qp.checkGetString("from_name"), "utf-8");
        String register = URLDecoder.decode(qp.getString("register", ""), "utf-8");
        String groupType = URLDecoder.decode(qp.checkGetString("group_type"), "utf-8");
        String groupName = URLDecoder.decode(qp.checkGetString("group_name"), "utf-8");
        String message = URLDecoder.decode(qp.getString("message", ""), "utf-8");
        String acceptUrl = URLDecoder.decode(qp.checkGetString("accept_url"), "utf-8");
        String rejectUrl = URLDecoder.decode(qp.checkGetString("reject_url"), "utf-8");

        String html = pageTemplate.merge("group_invite.ftl", new Object[][]{
                {"host", serverHost},
                {"displayName", displayName},
                {"fromName", fromName},
                {"register", register},
                {"groupType", groupType},
                {"groupName", groupName},
                {"message", message},
                {"acceptUrl", acceptUrl},
                {"rejectUrl", rejectUrl}
        });

        return DirectResponse.of("text/html", html);
    }

    @WebMethod("v2/public_circle/remove")
    public boolean removeMembersFromPublicCircle(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();

        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        String members = qp.checkGetString("members");
        String admins = qp.getString("admins", "");

        Record circleRec = GlobalLogics.getGroup().getGroup(ctx, ctx.getViewerIdString(),
                groupId, "formal", false);
        if (MapUtils.isNotEmpty(circleRec)
                && circleRec.getInt("formal") == Constants.PUBLIC_CIRCLE_FORMAL_TOP) {
            RecordSet subCircleRecs = GlobalLogics.getGroup().getGroups(ctx,
                    Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END,
                    ctx.getViewerIdString(), circleRec.getString("circle_ids", ""),
                    "formal", false);
            ArrayList<Long> subFormalIds = new ArrayList<Long>();
            for (Record subCircleRec : subCircleRecs) {
                if (subCircleRec.getInt("formal") == Constants.PUBLIC_CIRCLE_FORMAL_SUB)
                    subFormalIds.add(subCircleRec.getInt("id"));
            }
            for (long subFormalId : subFormalIds) {
                g.getDB().deleteMembers(ctx, subFormalId, members);
            }
        }

        return g.removeMembers(ctx, groupId, members, admins);
    }

    private RecordSet searchGroups(Context ctx, long begin, long end, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();

        String name = qp.checkGetString("name");
        String cols = qp.getString("columns", GROUP_LIGHT_COLS);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        RecordSet recs = g.searchGroups(ctx, begin, end, name, ctx.getViewerIdString(), page, count, cols);
        recs.renameColumn(GRP_COL_ID, "circle_id");
        recs.renameColumn(GRP_COL_NAME, "circle_name");

        return recs;
    }

    @WebMethod("v2/public_circle/search")
    public RecordSet searchPublicCircles(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        return searchGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, ACTIVITY_ID_BEGIN, qp);
    }

    private boolean groupGrant(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String admins = qp.getString("admins", "");
        String members = qp.getString("members", "");

        if (StringUtils.isBlank(admins) && StringUtils.isBlank(members))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Must have parameter 'admins' or 'members'");

        Record roles = new Record();
        if (StringUtils.isNotBlank(admins)) {
            long[] adminArr = StringUtils2.splitIntArray(admins, ",");
            for (long admin : adminArr) {
                roles.put(String.valueOf(admin), ROLE_ADMIN);
            }
        }
        if (StringUtils.isNotBlank(members)) {
            long[] memberArr = StringUtils2.splitIntArray(members, ",");
            for (long member : memberArr) {
                roles.put(String.valueOf(member), ROLE_MEMBER);
            }
        }

        return g.grants(ctx, groupId, roles);
    }

    @WebMethod("v2/public_circle/grant")
    public boolean publicCircleGrant(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return groupGrant(ctx, groupId, qp);
    }

    private boolean updateMemberNotification(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();

        String[] buildInParams = new String[]{"sign_method", "sign", "appid", "ticket", "circle_id", "group_id"};
        String[] notifParams = new String[]{"recv_notif", "notif_email", "notif_phone"};
        Record notif = new Record();
        for (Map.Entry<String, Object> entry : qp.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!ArrayUtils.contains(buildInParams, key)
                    && ArrayUtils.contains(notifParams, key)) {
                notif.put(key, value);
            }
        }

        return g.updateMemberNotification(ctx, groupId, ctx.getViewerIdString(), notif);
    }

    @WebMethod("v2/public_circle/update_notif")
    public boolean updateCircleMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return updateMemberNotification(ctx, groupId, qp);
    }

    private Record getMemberNotification(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        RecordSet recs = g.getMembersNotification(ctx, groupId, ctx.getViewerIdString());
        if (recs.isEmpty())
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "The user is not a member");
        else
            return recs.get(0);
    }

    @WebMethod("v2/public_circle/get_notif")
    public Record getCircleMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return getMemberNotification(ctx, groupId, qp);
    }

    private boolean defaultMemberNotification(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String userIds = qp.getString("users", ctx.getViewerIdString());
        return g.defaultMemberNotification(ctx, groupId, userIds);
    }

    @WebMethod("v2/public_circle/def_notif")
    public boolean defaultCircleMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "circle_id", "id", true, 0);
        return defaultMemberNotification(ctx, groupId, qp);
    }

    @WebMethod("v2/activity/create")
    public Record createActivity(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        qp.put("need_invited_confirm", 0);
        qp.put("privacy", GRP_PRIVACY_SECRET);
        if (!qp.containsKey("start_time"))
            qp.put("start_time", DateUtils.nowMillis());
        if (!qp.containsKey("end_time"))
            qp.put("end_time", 0);
//        return createGroup(ctx, ACTIVITY_ID_BEGIN, TYPE_ACTIVITY, qp);
        return createGroup(ctx, EVENT_ID_BEGIN, TYPE_EVENT, qp);
    }

    @WebMethod("v2/activity/show")
    public RecordSet getActivities(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String groupIds = getParamString(qp, "activity_ids", "ids", false, "");
//        return getGroups(ctx, ACTIVITY_ID_BEGIN, DEPARTMENT_ID_BEGIN, groupIds, qp);
        return getGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, groupIds, qp);
    }

    @WebMethod("v2/activity/search")
    public RecordSet searchAcitivities(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
//        return searchGroups(ctx, ACTIVITY_ID_BEGIN, DEPARTMENT_ID_BEGIN, qp);
        return searchGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, qp);
    }

    private void addOrRemove4Activity(Context ctx, long groupId, QueryParams qp) {
        GroupLogic g = GlobalLogics.getGroup();
        String add = qp.getString("add", "");
        if (StringUtils.isNotBlank(add)) {
            Set<String> borqsIds = StringUtils2.splitSet(add, ",", true);
            Record membersRec = new Record();
            for (String borqsId : borqsIds)
                membersRec.put(borqsId, ROLE_MEMBER);

            List<String> excludes = StringUtils2.splitList(g.getCreatorAndAdmins(ctx, groupId), ",", true);
            for (String exclude : excludes)
                membersRec.remove(exclude);
            if (!membersRec.isEmpty()) {
                g.addMembers(ctx, groupId, membersRec, false);
            }
        }

        String remove = qp.getString("remove", "");
        String admins = qp.getString("admins", "");
        if (StringUtils.isNotBlank(remove)) {
            g.removeMembers(ctx, groupId, remove, admins);
        }
    }

    @WebMethod("v2/activity/add_remove")
    public boolean addOrRemoveActivityMembers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);

        long groupId = getParamInt(qp, "activity_id", "id", true, 0);
        addOrRemove4Activity(ctx, groupId, qp);

        return true;
    }

    @WebMethod("v2/activity/update")
    public boolean updateActivity(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);

        long groupId = getParamInt(qp, "activity_id", "id", true, 0);
        addOrRemove4Activity(ctx, groupId, qp);

        return updateGroup(ctx, groupId, qp);
    }

    @WebMethod("v2/activity/add")
    public boolean addActivitiesMembers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();

        String groupIds = getParamString(qp, "activity_ids", "ids", true, "");
        if (StringUtils.isNotBlank(groupIds)) {
            Set<String> groups = StringUtils2.splitSet(groupIds, ",", true);
            for (String groupId : groups) {
                long id = Long.parseLong(groupId);
                String add = qp.checkGetString(groupId);
                Set<String> borqsIds = StringUtils2.splitSet(add, ",", true);
                Record membersRec = new Record();
                for (String borqsId : borqsIds)
                    membersRec.put(borqsId, ROLE_MEMBER);

                List<String> excludes = StringUtils2.splitList(g.getCreatorAndAdmins(ctx, id), ",", true);
                for (String exclude : excludes)
                    membersRec.remove(exclude);
                if (!membersRec.isEmpty()) {
                    g.addMembers(ctx, id, membersRec, false);
                }
            }
        }

        return true;
    }

    @WebMethod("v2/group/create")
    public Record createGeneralGroup(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();
        String type = qp.getString("type", "group");
        long begin = g.getBeginByGroupType(ctx, type);
        return createGroup(ctx, begin, type, qp);
    }

    @WebMethod("v2/group/update")
    public boolean updateGeneralGroup(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);

        return updateGroup(ctx, groupId, qp);
    }

    @WebMethod("v2/group/upload_profile_image")
    public boolean uploadGeneralGroupProfileImage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return uploadGroupProfileImage(ctx, groupId, qp);
    }

    @WebMethod("v2/group/destroy")
    public boolean destroyGeneralGroup(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();

        String groupIds = getParamString(qp, "group_ids", "ids", true, "");
        return g.destroyGroup(ctx, groupIds);
    }

    @WebMethod("v2/group/show")
    public RecordSet getGeneralGroups(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();
        String groupIds = getParamString(qp, "group_ids", "ids", false, "");
        String type = qp.getString("type", "group");
        long begin = g.getBeginByGroupType(ctx, type);
        long end = g.getEndByGroupType(ctx, type);
        return getGroups(ctx, begin, end, groupIds, qp);
    }

    @WebMethod("v2/group/detail")
    public Record getGroupDetail(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();
        String groupId = getParamString(qp, "group_id", "id", false, "");
        String type = qp.getString("type", "group");
        long begin = g.getBeginByGroupType(ctx, type);
        long end = g.getEndByGroupType(ctx, type);
        RecordSet recs = getGroups(ctx, begin, end, groupId, qp);
        if (recs.isEmpty()) {
            throw new ServerException(WutongErrors.GROUP_NOT_EXISTS, "The group is not exist");
        } else {
            return recs.get(0);
        }
    }

    @WebMethod("v2/group/users")
    public RecordSet getGeneralGroupUsers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return getGroupUsers(ctx, groupId, qp);
    }

    @WebMethod("v2/group/invite")
    public RecordSet generalGroupInvite(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return groupInvite(ctx, groupId, qp);
    }

    @WebMethod("v2/group/approve")
    public RecordSet generalGroupApprove(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return groupApprove(ctx, groupId, qp);
    }

    @WebMethod("v2/group/ignore")
    public RecordSet generalGroupIgnore(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return groupIgnore(ctx, groupId, qp);
    }

    @WebMethod("v2/group/join")
    public int generalGroupJoin(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();
        long groupId = getParamInt(qp, "group_id", "id", true, 0);

        int status = g.addMember(ctx, groupId, ctx.getViewerIdString(), qp.getString("message", ""), qp.getBoolean("send_post", true));
        if (status == STATUS_NONE)
            throw new ServerException(WutongErrors.GROUP_CANNOT_APPLY, "The group can not apply to join!");
        return status;
    }

    @WebMethod("v2/group/remove")
    public boolean removeMembersFromGeneralGroup(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();

        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        String members = qp.checkGetString("members");
        String admins = qp.getString("admins", "");
        return g.removeMembers(ctx, groupId, members, admins);
    }

    @WebMethod("v2/group/search")
    public RecordSet searchGeneralGroups(QueryParams qp) {
        String type = qp.getString("type", "group");
        Context ctx = WutongContext.getContext(qp, false);
        GroupLogic g = GlobalLogics.getGroup();
        long begin = g.getBeginByGroupType(ctx, type);
        long end = g.getEndByGroupType(ctx, type);
        return searchGroups(ctx, begin, end, qp);
    }

    @WebMethod("v2/group/grant")
    public boolean generalGroupGrant(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return groupGrant(ctx, groupId, qp);
    }

    @WebMethod("v2/group/update_notif")
    public boolean updateGroupMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return updateMemberNotification(ctx, groupId, qp);
    }

    @WebMethod("v2/group/get_notif")
    public Record getGroupMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return getMemberNotification(ctx, groupId, qp);
    }

    @WebMethod("v2/group/def_notif")
    public boolean defaultGroupMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "group_id", "id", true, 0);
        return defaultMemberNotification(ctx, groupId, qp);
    }


    //-----------   event  -----------
    @WebMethod("v2/event/create")
    public Record createEvent(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        qp.checkGetInt("start_time");
        qp.checkGetInt("end_time");

        Record eventRec = createGroup(ctx, EVENT_ID_BEGIN, TYPE_EVENT, qp);
        RecordSet parentCircleRecs = null;
        String parentCircleIds = eventRec.getString("parent_ids", "");
        if (StringUtils.isNotBlank(parentCircleIds)) {
            parentCircleRecs = GlobalLogics.getGroup().getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(parentCircleIds), "event_ids");
            if (CollectionUtils.isEmpty(parentCircleRecs))
                throw new ServerException(WutongErrors.GROUP_NOT_EXISTS);
        }


        long eventId = eventRec.getInt("group_id");
        if (StringUtils.isNotBlank(parentCircleIds)) {
            if (CollectionUtils.isNotEmpty(parentCircleRecs)) {
                for (Record parentCircleRec : parentCircleRecs) {
                    long parentCircleId = parentCircleRec.getInt("id");
                    String subEventIds = parentCircleRec.getString("event_ids", "");
                    subEventIds = StringUtils2.addIntToSet(subEventIds, eventId);
                    GlobalLogics.getGroup().updateGroupSimple(ctx, parentCircleId, new Record(), Record.of("event_ids", subEventIds));
                }
            }
        }

        return eventRec;
    }

    @WebMethod("v2/event/update")
    public boolean updateEvent(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);

        return updateGroup(ctx, groupId, qp);
    }

    @WebMethod("v2/event/upload_profile_image")
    public boolean uploadEventProfileImage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return uploadGroupProfileImage(ctx, groupId, qp);
    }

    @WebMethod("v2/event/destroy")
    public boolean destroyEvent(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();

        String eventIds = getParamString(qp, "event_ids", "ids", true, "");
        RecordSet eventRecs = g.getSimpleGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, eventIds, "parent_ids");
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (Record eventRec : eventRecs) {
            map.put(eventRec.getString("id"), eventRec.getString("parent_ids"));
        }

        for (Map.Entry<String, String> entry : map.entrySet()) {
            long eventId = Long.parseLong(entry.getKey());
            String parentIds = entry.getValue();
            if (StringUtils.isNotBlank(parentIds)) {
                RecordSet parentCircleRecs = g.getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, parentIds, "event_ids");
                if (CollectionUtils.isNotEmpty(parentCircleRecs)) {
                    for (Record parentCircleRec : parentCircleRecs) {
                        long parentId = parentCircleRec.getInt("id");
                        String parentSubEventIds = parentCircleRec.getString("event_ids");
                        parentSubEventIds = StringUtils2.removeIntFromSet(parentSubEventIds, eventId);
                        g.updateGroupSimple(ctx, parentId, new Record(), Record.of("event_ids", parentSubEventIds));
                    }
                }
            }
        }

        return g.destroyGroup(ctx, eventIds);
    }

    @WebMethod("v2/event/show")
    public RecordSet getEvents(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String cols = qp.getString("columns", GROUP_LIGHT_COLS);
        qp.put("columns", cols + ",start_time,end_time,parent_ids");
        String groupIds = getParamString(qp, "event_ids", "ids", false, "");
        return getGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, groupIds, qp);
    }

    @WebMethod("v2/event/detail")
    public Record getEventDetail(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String cols = qp.getString("columns", GROUP_LIGHT_COLS);
        qp.put("columns", cols + ",start_time,end_time,parent_ids");
        String groupId = getParamString(qp, "event_id", "id", false, "");
        RecordSet recs = getGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, groupId, qp);
        if (recs.isEmpty()) {
            throw new ServerException(WutongErrors.GROUP_NOT_EXISTS, "The event is not exist");
        } else {
            return recs.get(0);
        }
    }

    @WebMethod("v2/public_circle/events")
    public RecordSet getSubEvents(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();
        long circleId = qp.checkGetInt("circle_id");
        Record circleRec = g.getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "event_ids").getFirstRecord();
        String eventIds = circleRec.getString("event_ids", "");
        if (StringUtils.isBlank(eventIds)) {
            return new RecordSet();
        }

        String cols = qp.getString("columns", GROUP_LIGHT_COLS);
        qp.put("columns", cols + ",start_time,end_time,parent_ids");
        return getGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, eventIds, qp);
    }


    @WebMethod("v2/event/users")
    public RecordSet getEventUsers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return getGroupUsers(ctx, groupId, qp);
    }

    @WebMethod("v2/event/invite")
    public RecordSet eventInvite(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return groupInvite(ctx, groupId, qp);
    }

    @WebMethod("v2/event/approve")
    public RecordSet eventApprove(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return groupApprove(ctx, groupId, qp);
    }

    @WebMethod("v2/event/ignore")
    public RecordSet eventIgnore(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return groupIgnore(ctx, groupId, qp);
    }

    @WebMethod("v2/event/join")
    public int eventJoin(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));

        int status = g.addMember(ctx, groupId, ctx.getViewerIdString(), qp.getString("message", ""), qp.getBoolean("send_post", true));
        if (status == STATUS_NONE)
            throw new ServerException(WutongErrors.GROUP_CANNOT_APPLY, "The event can not apply to join!");
        return status;
    }

    @WebMethod("v2/event/remove")
    public boolean removeMembersFromEvent(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();

        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        String members = qp.checkGetString("members");
        String admins = qp.getString("admins", "");
        return g.removeMembers(ctx, groupId, members, admins);
    }

    @WebMethod("v2/event/search")
    public RecordSet searchEvents(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        String cols = qp.getString("columns", GROUP_LIGHT_COLS);
        qp.put("columns", cols + ",start_time,end_time");
        return searchGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, qp);
    }

    @WebMethod("v2/event/grant")
    public boolean eventGrant(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return groupGrant(ctx, groupId, qp);
    }

    @WebMethod("v2/event/update_notif")
    public boolean updateEventMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return updateMemberNotification(ctx, groupId, qp);
    }

    @WebMethod("v2/event/get_notif")
    public Record getEventMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return getMemberNotification(ctx, groupId, qp);
    }

    @WebMethod("v2/event/def_notif")
    public boolean defaultEventMemberNotification(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long groupId = getParamInt(qp, "event_id", "id", true, 0);
        return defaultMemberNotification(ctx, groupId, qp);
    }

    @WebMethod("v2/event/themes")
    public RecordSet getEventThemes(QueryParams qp) {
        EventThemeLogic et = GlobalLogics.getEventTheme();

        Context ctx = WutongContext.getContext(qp, false);

        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);

        return et.getEventThemes(ctx, page, count);
    }

    @WebMethod("v2/event/upload_theme")
    public Record updateTheme(QueryParams qp) {
        // TODO: upload image
        EventThemeLogic et = GlobalLogics.getEventTheme();

        Context ctx = WutongContext.getContext(qp, true);
        String name = qp.checkGetString("name");
        String image = qp.checkGetString("image");
        long now = DateUtils.nowMillis();
        return et.addEventTheme(ctx, RandomUtils.generateId(now), ctx.getViewerId(), now, name, image);
    }


    @WebMethod("v2/public_circle/members/list")
    public RecordSet getPublicCircleMembers(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle");
        String sort = qp.getString("sort", "fid");
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 50);
        return GlobalLogics.getMemberList().getMembers(ctx, circleId, sort, page, count);
    }

    @WebMethod("v2/public_circle/members/add")
    public Record addEmployeeOld(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle");
        String fid = qp.checkGetString(MemberListLogic.COL_FID);

        MemberListLogic ml = GlobalLogics.getMemberList();
        Record rec = new Record();
        rec.put(MemberListLogic.COL_FID, fid);
        for (String col : MemberListLogic.COLS) {
            rec.put(col, qp.getString(col, ""));
        }

        return ml.addMember(ctx, circleId, rec);
    }


    @WebMethod("v2/public_circle/members/update")
    public Record updateEmployeeOld(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle");
        String fid = qp.checkGetString(MemberListLogic.COL_FID);

        MemberListLogic ml = GlobalLogics.getMemberList();
        Record rec = new Record();
        rec.put(MemberListLogic.COL_FID, fid);
        for (String col : MemberListLogic.COLS) {
            rec.putIf(col, qp.getString(col, ""), qp.containsKey(col));
        }

        return ml.updateMember(ctx, circleId, rec);
    }

    @WebMethod("v2/public_circle/members/delete")
    public boolean deleteEmployeesOld(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle");
        String[] fids = StringUtils2.splitArray(qp.checkGetString("fids"), ",", true);
        GlobalLogics.getMemberList().deleteMembers(ctx, circleId, fids);
        return true;
    }

    @WebMethod("v2/public_circle/members/search")
    public RecordSet searchEmployee(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        long circleId = qp.checkGetInt("circle");
        String kw = qp.checkGetString("kw");

        MemberListLogic ml = GlobalLogics.getMemberList();
        return ml.searchMember(ctx, circleId,
                kw,
                qp.getString("sort", MemberListLogic.COL_FID),
                (int) qp.getInt("count", 100));
    }

    @WebMethod("v2/public_circle/members/upload_members")
    public int uploadEmployeesOld(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long circleId = qp.checkGetInt("circle");
        boolean merge = qp.getBoolean("merge", false);
        String json = qp.getString("json", "[]");
        RecordSet memberRecs = RecordSet.fromJson(json);
        MemberListLogic ml = GlobalLogics.getMemberList();
        return ml.putMembers(ctx, circleId, memberRecs, merge);
    }


    @WebMethod("v2/public_circle/members_status")
    public List<String> getMembersOnlineStatus(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String groupId = qp.checkGetString("group_id");
        GroupLogic g = GlobalLogics.getGroup();
        String members = g.getMembers(ctx, groupId, 0, 10000);
        List<String> memberList = StringUtils2.splitList(members, ",", true);

        Map<String, String> map = new HashMap<String, String>();
        List<String>  record = new ArrayList<String> ();

        try {
            map = xMemcached.readMultiCache(memberList);
        } catch (MemcachedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Map.Entry entry : map.entrySet()) {
            if (Integer.parseInt((String)entry.getValue())>0) {
                record.add((String)entry.getKey());
            }
        }
        return record;
    }
}
