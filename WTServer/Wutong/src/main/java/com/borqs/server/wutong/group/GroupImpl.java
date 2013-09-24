package com.borqs.server.wutong.group;

import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.*;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import com.borqs.server.wutong.messagecenter.MessageDelayCombineUtils;
import com.borqs.server.wutong.notif.*;
import com.borqs.server.wutong.page.PageLogicUtils;
import com.borqs.server.wutong.stream.StreamLogic;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import static com.borqs.server.wutong.Constants.*;
import static com.borqs.server.wutong.WutongErrors.GROUP_ADMIN_QUIT_ERROR;
import static com.borqs.server.wutong.WutongErrors.GROUP_CREATE_ERROR;

public class GroupImpl implements GroupLogic {
    private static final Logger L = Logger.getLogger(GroupImpl.class);
    private GroupDb groupDb = new GroupDb();
    private String SERVER_HOST = GlobalConfig.get().getString("server.host", "api.borqs.com");

    @Override
    public long createGroup(Context ctx, long begin, String type, String name, int memberLimit, int isStreamPublic, int canSearch, int canViewMembers, int canJoin, int canMemberInvite, int canMemberApprove, int canMemberPost, int canMemberQuit, int needInvitedConfirm, long creator, String label, Record properties) {
        return createGroup(ctx, begin, type, name, memberLimit, isStreamPublic, canSearch, canViewMembers, canJoin, canMemberInvite, canMemberApprove, canMemberPost, canMemberQuit, needInvitedConfirm, creator, label, properties, false);
    }

    @Override
    public long createGroup(Context ctx, long begin, String type, String name, int memberLimit, int isStreamPublic, int canSearch, int canViewMembers, int canJoin, int canMemberInvite, int canMemberApprove, int canMemberPost, int canMemberQuit, int needInvitedConfirm, long creator, String label, Record properties, boolean sendPost) {
        final String METHOD = "createGroup";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, type, name, memberLimit, isStreamPublic, canSearch, canViewMembers, canJoin, canMemberInvite, canMemberApprove, canMemberPost, canMemberQuit, needInvitedConfirm, creator, label, properties, sendPost);

        if (memberLimit <= 0)
            throw new ServerException(WutongErrors.GROUP_MEMBER_OUT_OF_LIMIT, "Invalid member limit");

        long groupId = groupDb.generateGroupId(ctx, begin, type);
        if (L.isOpEnabled())
            L.op(ctx, "generate group id");

        Record info = new Record();
        info.put(GRP_COL_ID, groupId);
        info.put(GRP_COL_NAME, name);
        info.put(GRP_COL_MEMBER_LIMIT, memberLimit);
        info.put(GRP_COL_IS_STREAM_PUBLIC, isStreamPublic);
        info.put(GRP_COL_CAN_SEARCH, canSearch);
        info.put(GRP_COL_CAN_VIEW_MEMBERS, canViewMembers);
        info.put(GRP_COL_CAN_JOIN, canJoin);
        info.put(GRP_COL_CAN_MEMBER_INVITE, canMemberInvite);
        info.put(GRP_COL_CAN_MEMBER_APPROVE, canMemberApprove);
        info.put(GRP_COL_CAN_MEMBER_POST, canMemberPost);
        info.put(GRP_COL_CAN_MEMBER_QUIT, canMemberQuit);
        info.put(GRP_COL_NEED_INVITED_CONFIRM, needInvitedConfirm);
        info.put(GRP_COL_CREATOR, creator);
        info.put(GRP_COL_LABEL, label);
        info.put(GRP_COL_CREATED_TIME, DateUtils.nowMillis());
        info.put(GRP_COL_UPDATED_TIME, DateUtils.nowMillis());
        info.put("sort_key", makeGroupSortKey(name));

        boolean r = groupDb.saveGroup(ctx, info, properties);
        if (!r)
            throw new ServerException(GROUP_CREATE_ERROR, "save public circle error");
        if (L.isOpEnabled())
            L.op(ctx, "save group");

        Record statusRec = new Record();
        statusRec.put("user_id", creator);
        statusRec.put("display_name", "");
        statusRec.put("identify", "");
        statusRec.put("source", 0);
        statusRec.put("status", STATUS_JOINED);
        addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));

        //send a post
        if (sendPost) {
            String m = "";
            String tempNowAttachments = "[]";

            String groupType = getGroupTypeStr(ctx, ctx.getLanguage(), groupId);
            String template = getBundleString(ctx.getUa(), "platform.create.group.message");
            String groupSchema = "<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + name + "</a>";
            String message = SQLTemplate.merge(template, new Object[][]{
                    {"groupType", groupType},
                    {"groupName", groupSchema}
            });
            boolean secretly = false;
            if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
                secretly = true;
            GlobalLogics.getStream().autoPost(ctx, String.valueOf(ctx.getViewerId()), TEXT_POST, message, tempNowAttachments, ctx.getAppId(), "", "", m, String.valueOf(groupId), secretly, "", ctx.getUa(), ctx.getLocation(), true, true, true, "", "", false,Constants.POST_SOURCE_SYSTEM);
            if (L.isOpEnabled())
                L.op(ctx, "send create group post");
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return groupId;
    }

    @Override
    public boolean updateGroupSimple(Context ctx, long groupId, Record info, Record properties) {
        final String METHOD = "updateGroupSimple";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, info, properties);

        Schemas.checkRecordColumnsIn(info, GRP_COL_NAME, GRP_COL_MEMBER_LIMIT, GRP_COL_IS_STREAM_PUBLIC, GRP_COL_CAN_SEARCH,
                GRP_COL_CAN_VIEW_MEMBERS, GRP_COL_CAN_JOIN, GRP_COL_CAN_MEMBER_INVITE, GRP_COL_CAN_MEMBER_APPROVE, GRP_COL_CAN_MEMBER_POST, GRP_COL_CAN_MEMBER_QUIT, GRP_COL_NEED_INVITED_CONFIRM, GRP_COL_LABEL);
        info.put(GRP_COL_UPDATED_TIME, DateUtils.nowMillis());

        boolean b = groupDb.updateGroup0(ctx, groupId, info, properties);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);

        return b;
    }

    @Override
    public boolean updateGroup(Context ctx, long groupId, Record info, Record properties) {
        final String METHOD = "updateGroup";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, info, properties);

        if (groupDb.hasRight0(ctx, groupId, ctx.getViewerId(), ROLE_ADMIN)) {
            if (L.isDebugEnabled())
                L.debug(ctx, "has right to update group info");

            Schemas.checkRecordColumnsIn(info, GRP_COL_NAME, GRP_COL_MEMBER_LIMIT, GRP_COL_IS_STREAM_PUBLIC, GRP_COL_CAN_SEARCH,
                    GRP_COL_CAN_VIEW_MEMBERS, GRP_COL_CAN_JOIN, GRP_COL_CAN_MEMBER_INVITE, GRP_COL_CAN_MEMBER_APPROVE, GRP_COL_CAN_MEMBER_POST, GRP_COL_CAN_MEMBER_QUIT, GRP_COL_NEED_INVITED_CONFIRM, GRP_COL_LABEL);
            info.put(GRP_COL_UPDATED_TIME, DateUtils.nowMillis());

            boolean b = groupDb.updateGroup0(ctx, groupId, info, properties);

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return b;
        }
        else {
            if (L.isDebugEnabled())
                L.debug(ctx, "do not has right to update group info");
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return false;
        }
    }

    @Override
    public boolean destroyGroup(Context ctx, String groupIds) {
        final String METHOD = "destroyGroup";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupIds);

        long[] gids = StringUtils2.splitIntArray(groupIds, ",");
        if (groupDb.hasRight0(ctx, gids[0], ctx.getViewerId(), ROLE_ADMIN)) {
            if (L.isDebugEnabled())
                L.debug(ctx, "has right to destroy group");
            boolean r = groupDb.deleteGroups(ctx, groupIds);

            List<String> l = StringUtils2.splitList(groupIds, ",", true);
            for (String groupId : l) {
                GlobalLogics.getRequest().dealRelatedRequestsP(ctx, "0", "0", groupId);
            }
            if (L.isOpEnabled())
                L.op(ctx, "deal related requests");

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return r;
        }
        else {
            if (L.isDebugEnabled())
                L.debug(ctx, "do not has right to destroy group");
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return false;
        }
    }

    @Override
    public RecordSet getSimpleGroups(Context ctx, long begin, long end, String groupIds, String cols) {
        final String METHOD = "getSimpleGroups";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, end, groupIds, cols);

        RecordSet recs = new RecordSet();
        if (StringUtils.isNotBlank(groupIds))
            recs = groupDb.getGroups(ctx, begin, end, StringUtils2.splitIntArray(groupIds, ","),
                    StringUtils2.splitArray(cols, ",", true));

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getSimpleGroups(Context ctx, long begin, long end, String groupIds, String cols, int page, int count) {
        final String METHOD = "getSimpleGroups";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, end, groupIds, cols);

        RecordSet recs = new RecordSet();
        if (StringUtils.isNotBlank(groupIds))
            recs = groupDb.getGroups(ctx, begin, end, StringUtils2.splitIntArray(groupIds, ","),
                    page, count, StringUtils2.splitArray(cols, ",", true));

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    private static void addImageUrlPrefix(String profileImagePattern, Record rec) {
        if (rec.has("image_url")) {
            if (!rec.getString("image_url", "").startsWith("http:"))
                rec.put("image_url", String.format(profileImagePattern, rec.getString("image_url")));
        }

        if (rec.has("small_image_url")) {
            if (!rec.getString("small_image_url", "").startsWith("http:"))
                rec.put("small_image_url", String.format(profileImagePattern, rec.getString("small_image_url")));
        }

        if (rec.has("large_image_url")) {
            if (!rec.getString("large_image_url", "").startsWith("http:"))
                rec.put("large_image_url", String.format(profileImagePattern, rec.getString("large_image_url")));
        }
    }

    private Record additionalGroupInfo(Context ctx, String userId, Record rec) {
        long groupId = rec.getInt(GRP_COL_ID, 0);
        int memberCount = getMembersCount(ctx, groupId);

        //            String creator = String.valueOf(rec.getInt("creator", 0));
        String creator = String.valueOf(getCreator(ctx, groupId));
        String admins = getAdmins(ctx, groupId, -1, -1);
        rec.put("admins", admins);

        int adminCount = StringUtils2.splitList(admins, ",", true).size();
        if (!StringUtils.equals(creator, "0"))
            adminCount++;

        long uid = 0;
        try {
            uid = Long.parseLong(userId);
        } catch (Exception e) {
            uid = 0;
        }
        int role = hasRight(ctx, groupId, uid, ROLE_MEMBER) ? ROLE_MEMBER : ROLE_GUEST;
        boolean can_update = false;
        boolean can_destroy = false;
        boolean can_remove = false;
        boolean can_grant = false;
        boolean can_quit = rec.getInt(GRP_COL_CAN_MEMBER_QUIT, 1) == 1 ? true : false;

        if (StringUtils.equals(creator, userId)) {
            role = ROLE_CREATOR;
            can_update = true;
            can_destroy = true;
            can_remove = true;
            can_grant = true;

            if ((adminCount < 2) && (memberCount > 1))
                can_quit = false;
        }
        else if (StringUtils.contains(admins, userId)) {
            role = ROLE_ADMIN;
            can_update = true;
            can_destroy = true;
            can_remove = true;
            can_grant = true;

            if ((adminCount < 2) && (memberCount > 1))
                can_quit = false;
        }

        rec.put("role_in_group", role);
        rec.put("viewer_can_update", can_update);
        rec.put("viewer_can_destroy", can_destroy);
        rec.put("viewer_can_remove", can_remove);
        rec.put("viewer_can_grant", can_grant);
        rec.put("viewer_can_quit", can_quit);

        rec.put("member_count", memberCount);

        rec.putMissing(COMM_COL_DESCRIPTION, "");
        rec.putMissing(COMM_COL_COMPANY, "");
        rec.putMissing(COMM_COL_DEPARTMENT, "");
        rec.putMissing(COMM_COL_WEBSITE, "");
        rec.putMissing(COMM_COL_BULLETIN, "");
        rec.putMissing(COMM_COL_BULLETIN_UPDATED_TIME, 0);
        if (isEvent(ctx, groupId)) {
            rec.putMissing("parent_ids", "");
        }

        long themeId = rec.getInt(COMM_COL_THEME_ID, 0);
        if (themeId != 0) {
            Map<Long, Record> themeRecs = GlobalLogics.getEventTheme().getEventThemes(ctx, themeId);
            if (MapUtils.isNotEmpty(themeRecs)) {
                Record themeRec = themeRecs.values().iterator().next();
                rec.put(COMM_COL_THEME_NAME, ObjectUtils.toString(themeRec.getString("name")));
                rec.put(COMM_COL_THEME_IMAGE, ObjectUtils.toString(themeRec.getString("image_url")));
            }
        } else {
            rec.put(COMM_COL_THEME_ID, 0L);
            rec.put(COMM_COL_THEME_NAME, "");
            rec.put(COMM_COL_THEME_IMAGE, "");
        }

        if (rec.has(COMM_COL_CONTACT_INFO)) {
            Record contactInfo = Record.fromJson(rec.getString(COMM_COL_CONTACT_INFO));
            rec.put(COMM_COL_CONTACT_INFO, contactInfo);
        } else
            rec.put(COMM_COL_CONTACT_INFO, JsonNodeFactory.instance.objectNode());

        if (rec.has(COMM_COL_ADDRESS)) {
            RecordSet address = RecordSet.fromJson(rec.getString(COMM_COL_ADDRESS));
            rec.put(COMM_COL_ADDRESS, address);
        } else
            rec.put(COMM_COL_ADDRESS, JsonNodeFactory.instance.arrayNode());

        String urlPattern = GlobalConfig.get().getString("platform.profileImagePattern", "");
        if (!rec.has(COMM_COL_IMAGE_URL)) {
            rec.put(COMM_COL_IMAGE_URL, "default_public_circle.png");
            rec.put(COMM_COL_SMALL_IMG_URL, "default_public_circle_S.png");
            rec.put(COMM_COL_LARGE_IMG_URL, "default_public_circle_L.png");
            urlPattern = GlobalConfig.get().getString("platform.sysIconUrlPattern", "");
        }
        addImageUrlPrefix(urlPattern, rec);

        //shared count
        StreamLogic stream = GlobalLogics.getStream();
        Record sharedCount = new Record();
        int sharedText = stream.getSharedCount(ctx, userId, String.valueOf(groupId), TEXT_POST);
        sharedCount.put("shared_text", sharedText);
        int sharedPhoto = stream.getSharedCount(ctx, userId, String.valueOf(groupId), PHOTO_POST);
        sharedCount.put("shared_photo", sharedPhoto);
        int sharedBook = stream.getSharedCount(ctx, userId, String.valueOf(groupId), BOOK_POST);
        sharedCount.put("shared_book", sharedBook);
        int sharedApk = stream.getSharedCount(ctx, userId, String.valueOf(groupId), APK_POST);
        sharedCount.put("shared_apk", sharedApk);
        int sharedLink = stream.getSharedCount(ctx, userId, String.valueOf(groupId), LINK_POST);
        sharedCount.put("shared_link", sharedLink);
        int shared_static_file = stream.getSharedCount(ctx, userId, String.valueOf(groupId), FILE_POST);
        sharedCount.put("shared_static_file", shared_static_file);
        int shared_audio = stream.getSharedCount(ctx, userId, String.valueOf(groupId), AUDIO_POST);
        sharedCount.put("shared_audio", shared_audio);
        int shared_video = stream.getSharedCount(ctx, userId, String.valueOf(groupId), VIDEO_POST);
        sharedCount.put("shared_video", shared_video);
//        sharedCount.put("shared_poll", GlobalLogics.getPoll().getRelatedPollCount(ctx, userId, String.valueOf(groupId)));

//        if (rec.has("event_ids")) {
//            String eventIds = rec.getString("event_ids");
//            Set<String> set = StringUtils2.splitSet(eventIds, ",", true);
//            sharedCount.put("shared_event", set.size());
//        }

        rec.put("shared_count", sharedCount);

        //top posts
        String topName = rec.getString(COMM_COL_TOP_NAME, "");
        String topPostIds = rec.getString(COMM_COL_TOP_POSTS, "");
        int topCount = StringUtils2.splitSet(topPostIds, ",", true).size();
//            RecordSet posts = getFullPostsForQiuPu(userId, topPostIds, true);
        RecordSet posts = new RecordSet();
        Record topPosts = new Record();
        topPosts.put("name", topName);
        topPosts.put("count", topCount);
        topPosts.put("posts", posts);
        rec.put(COMM_COL_TOP_POSTS, topPosts);
        rec.remove(COMM_COL_TOP_NAME);

        int privacy = 0;
        long isStreamPublic = rec.getInt(GRP_COL_IS_STREAM_PUBLIC, 1);
        long canSearch = rec.getInt(GRP_COL_CAN_SEARCH, 1);
        long canViewMembers = rec.getInt(GRP_COL_CAN_VIEW_MEMBERS, 1);
        if ((isStreamPublic == 1) && (canSearch == 1) && (canViewMembers == 1))
            privacy = GRP_PRIVACY_OPEN;
        else if ((isStreamPublic == 0) && (canSearch == 1) && (canViewMembers == 1))
            privacy = GRP_PRIVACY_CLOSED;
        else if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
            privacy = GRP_PRIVACY_SECRET;
        else
            privacy = 0;
        rec.put("privacy", privacy);

        Record profileFreeCircles = new Record();
        Record profileFormalCircles = new Record();
        int freeCirclesCount = 0;
        int formalCirclesCount = 0;
        RecordSet formalRecs = new RecordSet();
        RecordSet freeRecs = new RecordSet();
        String circleIds = rec.getString("circle_ids", "");
        //String circleIds = rec.getString("circle_ids", "");
        if (StringUtils.isNotBlank(circleIds)) {
            RecordSet subCircleRecs = getSimpleGroups(ctx,
                    Constants.PUBLIC_CIRCLE_ID_BEGIN, Constants.PUBLIC_CIRCLE_ID_END,
                    circleIds, "parent_id, formal, image_url, large_image_url, small_image_url"
            );

            for (Record subCircle : subCircleRecs) {
                if (!subCircle.has(COMM_COL_IMAGE_URL)) {
                    subCircle.put(COMM_COL_IMAGE_URL, "default_public_circle.png");
                    subCircle.put(COMM_COL_SMALL_IMG_URL, "default_public_circle_S.png");
                    subCircle.put(COMM_COL_LARGE_IMG_URL, "default_public_circle_L.png");
                    urlPattern = GlobalConfig.get().getString("platform.sysIconUrlPattern", "");
                }
                addImageUrlPrefix(urlPattern, subCircle);

                String formal = subCircle.getString("formal", "");
                if (StringUtils.isBlank(formal)) {
                    formal = String.valueOf(Constants.PUBLIC_CIRCLE_FORMAL_FREE);
                }
                if (Integer.parseInt(formal) == Constants.PUBLIC_CIRCLE_FORMAL_FREE) {
                    freeCirclesCount++;
                    if (freeRecs.size() < 5)
                        freeRecs.add(subCircle);
                }
                if (Integer.parseInt(formal) == Constants.PUBLIC_CIRCLE_FORMAL_SUB) {
                    formalCirclesCount++;
                    if (formalRecs.size() < 5)
                        formalRecs.add(subCircle);
                }
            }

            int minSize = subCircleRecs.size() < 5 ? subCircleRecs.size() : 5;
            rec.put("circles", subCircleRecs.subList(0, minSize));
        } else {
            rec.put("circles", JsonNodeFactory.instance.arrayNode());
        }

        profileFreeCircles.put("count", freeCirclesCount);
        profileFreeCircles.put("circles", freeRecs);
        rec.put("free_circles", profileFreeCircles);

        profileFormalCircles.put("count", formalCirclesCount);
        profileFormalCircles.put("circles", formalRecs);
        rec.put("formal_circles", profileFormalCircles);

        RecordSet rs = GlobalLogics.getCategory().getCategoryTypes(ctx,String.valueOf(groupId));
        rec.put("categories",rs);
        return rec;
    }

    private RecordSet additionalGroupsInfo(Context ctx, String userId, RecordSet recs) {
        String groupIds = StringUtils2.joinIgnoreBlank(",", recs.getStringColumnValues(GRP_COL_ID));
        Record counts = new Record();
        if (StringUtils.isNotBlank(groupIds))
            counts = getMembersCounts(ctx, groupIds);

        for (Record rec : recs) {
            long groupId = rec.getInt(GRP_COL_ID, 0);
//            String creator = String.valueOf(rec.getInt("creator", 0));
            String creator = String.valueOf(getCreator(ctx, groupId));
            String admins = getAdmins(ctx, groupId, -1, -1);
            rec.put("admins", admins);

            int adminCount = StringUtils2.splitList(admins, ",", true).size();
            if (!StringUtils.equals(creator, "0"))
                adminCount++;

            long uid = 0;
            try {
                uid = Long.parseLong(userId);
            } catch (Exception e) {
                uid = 0;
            }
            int role = hasRight(ctx, groupId, uid, ROLE_MEMBER) ? ROLE_MEMBER : ROLE_GUEST;
            boolean can_update = false;
            boolean can_destroy = false;
            boolean can_remove = false;
            boolean can_grant = false;
            boolean can_quit = rec.getInt(GRP_COL_CAN_MEMBER_QUIT, 1) == 1 ? true : false;
            long memberCount = counts.getInt(String.valueOf(groupId));

            if (StringUtils.equals(creator, userId)) {
                role = ROLE_CREATOR;
                can_update = true;
                can_destroy = true;
                can_remove = true;
                can_grant = true;

                if ((adminCount < 2) && (memberCount > 1))
                    can_quit = false;
            }
            else if (StringUtils.contains(admins, userId)) {
                role = ROLE_ADMIN;
                can_update = true;
                can_destroy = true;
                can_remove = true;
                can_grant = true;

                if ((adminCount < 2) && (memberCount > 1))
                    can_quit = false;
            }

            rec.put("role_in_group", role);
            rec.put("viewer_can_update", can_update);
            rec.put("viewer_can_destroy", can_destroy);
            rec.put("viewer_can_remove", can_remove);
            rec.put("viewer_can_grant", can_grant);
            rec.put("viewer_can_quit", can_quit);

            rec.put("member_count", memberCount);

            rec.putMissing(COMM_COL_DESCRIPTION, "");
            rec.putMissing(COMM_COL_COMPANY, "");
            rec.putMissing(COMM_COL_DEPARTMENT, "");
            rec.putMissing(COMM_COL_WEBSITE, "");
            rec.putMissing(COMM_COL_BULLETIN, "");
            rec.putMissing(COMM_COL_BULLETIN_UPDATED_TIME, 0);

            long themeId = rec.getInt(COMM_COL_THEME_ID, 0);
            if (themeId != 0) {
                Map<Long, Record> themeRecs = GlobalLogics.getEventTheme().getEventThemes(ctx, themeId);
                if (MapUtils.isNotEmpty(themeRecs)) {
                    Record themeRec = themeRecs.values().iterator().next();
                    rec.put(COMM_COL_THEME_NAME, ObjectUtils.toString(themeRec.getString("name")));
                    rec.put(COMM_COL_THEME_IMAGE, ObjectUtils.toString(themeRec.getString("image_url")));
                }
            } else {
                rec.put(COMM_COL_THEME_ID, 0L);
                rec.put(COMM_COL_THEME_NAME, "");
                rec.put(COMM_COL_THEME_IMAGE, "");
            }

            if (rec.has(COMM_COL_CONTACT_INFO)) {
                Record contactInfo = Record.fromJson(rec.getString(COMM_COL_CONTACT_INFO));
                rec.put(COMM_COL_CONTACT_INFO, contactInfo);
            } else
                rec.put(COMM_COL_CONTACT_INFO, JsonNodeFactory.instance.objectNode());

            if (rec.has(COMM_COL_ADDRESS)) {
                RecordSet address = RecordSet.fromJson(rec.getString(COMM_COL_ADDRESS));
                rec.put(COMM_COL_ADDRESS, address);
            } else
                rec.put(COMM_COL_ADDRESS, JsonNodeFactory.instance.arrayNode());

            String urlPattern = GlobalConfig.get().getString("platform.profileImagePattern", "");
            if (!rec.has(COMM_COL_IMAGE_URL)) {
                rec.put(COMM_COL_IMAGE_URL, "default_public_circle.png");
                rec.put(COMM_COL_SMALL_IMG_URL, "default_public_circle_S.png");
                rec.put(COMM_COL_LARGE_IMG_URL, "default_public_circle_L.png");
                urlPattern = GlobalConfig.get().getString("platform.sysIconUrlPattern", "");
            }
            addImageUrlPrefix(urlPattern, rec);

            //shared count
/*            StreamLogic stream = GlobalLogics.getStream();
            Record sharedCount = new Record();
            int sharedText = stream.getSharedCount(ctx, userId, String.valueOf(groupId), TEXT_POST);
            sharedCount.put("shared_text", sharedText);
            int sharedPhoto = stream.getSharedCount(ctx, userId, String.valueOf(groupId), PHOTO_POST);
            sharedCount.put("shared_photo", sharedPhoto);
            int sharedBook = stream.getSharedCount(ctx, userId, String.valueOf(groupId), BOOK_POST);
            sharedCount.put("shared_book", sharedBook);
            int sharedApk = stream.getSharedCount(ctx, userId, String.valueOf(groupId), APK_POST);
            sharedCount.put("shared_apk", sharedApk);
            int sharedLink = stream.getSharedCount(ctx, userId, String.valueOf(groupId), LINK_POST);
            sharedCount.put("shared_link", sharedLink);
            int shared_static_file = stream.getSharedCount(ctx, userId, String.valueOf(groupId), FILE_POST);
            sharedCount.put("shared_static_file", shared_static_file);
            int shared_audio = stream.getSharedCount(ctx, userId, String.valueOf(groupId), AUDIO_POST);
            sharedCount.put("shared_audio", shared_audio);
            int shared_video = stream.getSharedCount(ctx, userId, String.valueOf(groupId), VIDEO_POST);
            sharedCount.put("shared_video", shared_video);
            sharedCount.put("shared_poll", GlobalLogics.getPoll().getRelatedPollCount(ctx, userId, String.valueOf(groupId)));
            rec.put("shared_count", sharedCount);
*/

            //top posts
            String topName = rec.getString(COMM_COL_TOP_NAME, "");
            String topPostIds = rec.getString(COMM_COL_TOP_POSTS, "");
            int topCount = StringUtils2.splitSet(topPostIds, ",", true).size();
//            RecordSet posts = getFullPostsForQiuPu(userId, topPostIds, true);
            RecordSet posts = new RecordSet();
            Record topPosts = new Record();
            topPosts.put("name", topName);
            topPosts.put("count", topCount);
            topPosts.put("posts", posts);
            rec.put(COMM_COL_TOP_POSTS, topPosts);
            rec.remove(COMM_COL_TOP_NAME);

            int privacy = 0;
            long isStreamPublic = rec.getInt(GRP_COL_IS_STREAM_PUBLIC, 1);
            long canSearch = rec.getInt(GRP_COL_CAN_SEARCH, 1);
            long canViewMembers = rec.getInt(GRP_COL_CAN_VIEW_MEMBERS, 1);
            if ((isStreamPublic == 1) && (canSearch == 1) && (canViewMembers == 1))
                privacy = GRP_PRIVACY_OPEN;
            else if ((isStreamPublic == 0) && (canSearch == 1) && (canViewMembers == 1))
                privacy = GRP_PRIVACY_CLOSED;
            else if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
                privacy = GRP_PRIVACY_SECRET;
            else
                privacy = 0;
            rec.put("privacy", privacy);
        }

        return recs;
    }

    @Override
    public Record getGroup(Context ctx, String userId, long groupId, String cols, boolean withMembers) {
        final String METHOD = "getGroup";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, groupId, cols, withMembers);

        boolean addCircleIds = false;
        if (isPublicCircle(ctx, groupId) && !StringUtils.contains(cols, "circle_ids")) {
            cols += ",circle_ids";
            addCircleIds = true;
        }
        Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), cols).getFirstRecord();
        if (rec.isEmpty())
            return rec;

        if (isPublicCircle(ctx, groupId) && !rec.has("circle_ids")) {
            rec.put("circle_ids", "");
        }
        rec = additionalGroupInfo(ctx, userId, rec);
        if (addCircleIds)
            rec.remove("circle_ids");

        if (withMembers) {
            long canViewMembers = rec.getInt(GRP_COL_CAN_VIEW_MEMBERS, 1);
            if (canViewMembers == 1) {
                String memberIds = getAllMembers(ctx, groupId, -1, -1, "");
                RecordSet members = GlobalLogics.getAccount().getUsersBaseColumns(ctx, memberIds);
                String creator = rec.getString("creator");
                String admins = rec.getString("admins");
                for (Record member : members) {
                    String memberId = member.getString("user_id");
                    int memberRole = ROLE_MEMBER;
                    if (StringUtils.equals(creator, memberId))
                        memberRole = ROLE_CREATOR;
                    else if (StringUtils.contains(admins, memberId))
                        memberRole = ROLE_ADMIN;
                    member.put("role_in_group", memberRole);
                }
                rec.put(GRP_COL_MEMBERS, members.toJsonNode());
            }
            else
                rec.put(GRP_COL_MEMBERS, JsonNodeFactory.instance.arrayNode());

            Record creator = GlobalLogics.getAccount().getUsersBaseColumns(ctx, String.valueOf(rec.getInt("creator", 0))).getFirstRecord();
            rec.put("creator", creator.toJsonNode());
            rec.remove("admins");
        } else {
            Record creator = GlobalLogics.getAccount().getUsersBaseColumns(ctx, String.valueOf(rec.getInt("creator", 0))).getFirstRecord();
            rec.put("creator", creator.toJsonNode());
            rec.remove("admins");
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    @Override
    public RecordSet getGroups(Context ctx, long begin, long end, String userId, String groupIds, String cols, boolean withMembers) {
        return getGroups(ctx, begin, end, userId, groupIds, cols, withMembers, -1, -1);
    }

    @Override
    public RecordSet getGroups(Context ctx, long begin, long end, String userId, String groupIds, String cols, boolean withMembers, int page, int count) {
        final String METHOD = "getGroups";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, end, userId, groupIds, cols, withMembers);

        RecordSet recs;
        if (StringUtils.isBlank(groupIds)) {
            if (L.isDebugEnabled())
                L.debug(ctx, "get user's all group list");

            recs = findGroupsByMember(ctx, begin, end, Long.parseLong(userId), cols, page, count);
            groupIds = StringUtils2.joinIgnoreBlank(",", recs.getStringColumnValues(GRP_COL_ID));
            if (L.isDebugEnabled())
                L.debug(ctx, groupIds);
        }
        else {
            if (L.isDebugEnabled())
                L.debug(ctx, "get specific groups");
            recs = getSimpleGroups(ctx, begin, end, groupIds, cols, page, count);
        }

        if (CollectionUtils.isEmpty(recs))
            return recs;

        recs = additionalGroupsInfo(ctx, userId, recs);
        if (L.isOpEnabled())
            L.op(ctx, "add additional group info");

        if (withMembers) {
            if (L.isDebugEnabled())
                L.debug(ctx, "get members info");

            for (Record rec : recs) {
                long groupId = rec.getInt(GRP_COL_ID, 0);
                long canViewMembers = rec.getInt(GRP_COL_CAN_VIEW_MEMBERS, 1);
                if (canViewMembers == 1) {
                    String memberIds = getAllMembers(ctx, groupId, -1, -1, "");
                    RecordSet members = GlobalLogics.getAccount().getUsersBaseColumns(ctx, memberIds);
                    String creator = rec.getString("creator");
                    String admins = rec.getString("admins");
                    for (Record member : members) {
                        String memberId = member.getString("user_id");
                        int memberRole = ROLE_MEMBER;
                        if (StringUtils.equals(creator, memberId))
                            memberRole = ROLE_CREATOR;
                        else if (StringUtils.contains(admins, memberId))
                            memberRole = ROLE_ADMIN;
                        member.put("role_in_group", memberRole);
                    }
                    rec.put(GRP_COL_MEMBERS, members.toJsonNode());
                }
                else
                    rec.put(GRP_COL_MEMBERS, JsonNodeFactory.instance.arrayNode());

                Record creator = GlobalLogics.getAccount().getUsersBaseColumns(ctx, String.valueOf(rec.getInt("creator", 0))).getFirstRecord();
                rec.put("creator", creator.toJsonNode());
                rec.remove("admins");
            }
        } else {
            for (Record rec : recs) {
                Record creator = GlobalLogics.getAccount().getUsersBaseColumns(ctx, String.valueOf(rec.getInt("creator", 0))).getFirstRecord();
                rec.put("creator", creator.toJsonNode());
                rec.remove("admins");
            }
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getCompatibleGroups(Context ctx, String viewerId, String groupIds) {
        final String METHOD = "getCompatibleGroups";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, groupIds);

        RecordSet groups = getGroups(ctx, 0, 0, viewerId, groupIds, GROUP_LIGHT_COLS, false);

        groups.renameColumn(GRP_COL_ID, "user_id");
        groups.renameColumn(GRP_COL_NAME, "display_name");
        groups.renameColumn(COMM_COL_DESCRIPTION, "status");
        groups.renameColumn(COMM_COL_WEBSITE, "domain_name");

        for (Record group : groups) {
            group.putMissing("last_visited_time", 0);
            group.putMissing("login_email1", "");
            group.putMissing("login_email2", "");
            group.putMissing("login_email3", "");
            group.putMissing("login_phone1", "");
            group.putMissing("login_phone2", "");
            group.putMissing("login_phone3", "");
            group.putMissing("status_updated_time", 0);
            group.putMissing("perhaps_name", JsonNodeFactory.instance.arrayNode());
            group.putMissing("basic_updated_time", 0);
            group.putMissing("first_name", "");
            group.putMissing("middle_name", "");
            group.putMissing("last_name", "");
            group.putMissing("gender", "m");
            group.putMissing("birthday", "");
            group.putMissing("job_title", "");
            group.putMissing("office_address", "");
            group.putMissing("job_description", "");
            group.putMissing("work_history", JsonNodeFactory.instance.arrayNode());
            group.putMissing("education_history", JsonNodeFactory.instance.arrayNode());
            group.putMissing("miscellaneous", JsonNodeFactory.instance.objectNode());
            group.putMissing("remark", "");
            group.putMissing("bidi", false);
            group.putMissing("in_circles", JsonNodeFactory.instance.arrayNode());
            group.putMissing("his_friend", false);
            group.putMissing("favorites_count", "0");
            group.putMissing("friends_count", "0");
            group.putMissing("followers_count", "0");
            group.putMissing("profile_privacy", false);
            group.putMissing("pedding_requests", JsonNodeFactory.instance.arrayNode());
            group.putMissing("profile_friends", JsonNodeFactory.instance.arrayNode());
            group.putMissing("profile_followers", JsonNodeFactory.instance.arrayNode());
            group.putMissing("profile_shared_photos", JsonNodeFactory.instance.arrayNode());
            group.putMissing("social_contacts_username", "");
            group.putMissing("who_suggested", JsonNodeFactory.instance.arrayNode());
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return groups;
    }

    @Override
    public RecordSet findGroupsByMember(Context ctx, long begin, long end, long member, String cols) {
        final String METHOD = "findGroupsByMember";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, end, member, cols);

        RecordSet recs = groupDb.findGroupsByMember0(ctx, begin, end, member, StringUtils2.splitArray(cols, ",", true));

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet findGroupsByMember(Context ctx, long begin, long end, long member, String cols, int page, int count) {
        final String METHOD = "findGroupsByMember";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, end, member, cols);

        RecordSet recs = groupDb.findGroupsByMember0(ctx, begin, end, member, page, count, StringUtils2.splitArray(cols, ",", true));

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public String findGroupIdsByMember(Context ctx, long begin, long end, long member) {
        final String METHOD = "findGroupIdsByMember";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, end, member);

        String groupIds = groupDb.findGroupIdsByMember0(ctx, begin, end, member);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return groupIds;
    }

    @Override
    public String findGroupIdsByTopPost(Context ctx, String postId) {
        final String METHOD = "findGroupIdsByTopPost";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, postId);
        String groupIds = groupDb.findGroupIdsByTopPost0(ctx, postId);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return groupIds;
    }
    @Override
    public String findStreamIdsByGroupId(Context ctx, String groupId) {
        final String METHOD = "findStreamIdsByGroupId";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId);
        String groupIds = groupDb.findPostIdsByTopGroup0(ctx, groupId);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return groupIds;
    }


    @Override
    public Map<String, String> findGroupIdsByTopPosts(Context ctx, String[] postIds) {
        final String METHOD = "findGroupIdsByTopPost";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, postIds);
        Map<String, String> m = groupDb.findGroupIdsByTopPosts0(ctx, postIds);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return m;
    }

    @Override
    public RecordSet findGroupsByName(Context ctx, long begin, long end, String name, int page, int count, String cols) {
        final String METHOD = "findGroupsByName";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, end, name, cols);

        RecordSet recs = groupDb.findGroupsByName0(ctx, begin, end, name, page, count, StringUtils2.splitArray(cols, ",", true));

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public boolean addSimpleMember(Context ctx, long groupId, long member, int role) {
        final String METHOD = "addSimpleMember";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, member, role);

        String userId = ObjectUtils.toString(member);
        boolean r1 = groupDb.addOrGrantMembers(ctx, groupId, Record.of(userId, role));
        boolean r2 = defaultMemberNotification(ctx, groupId, userId);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return r1 && r2;
    }

    @Override
    public boolean addSimpleMembers(Context ctx, long groupId, Record roles) {
        final String METHOD = "addSimpleMembers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, roles);

        String userIds = StringUtils2.joinIgnoreBlank(",", roles.keySet());
        boolean r1 = groupDb.addOrGrantMembers(ctx, groupId, roles);
        boolean r2 = defaultMemberNotification(ctx, groupId, userIds);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return r1 && r2;
    }

    @Override
    public String getGroupTypeStr(Context ctx, String lang, long groupId) {
        final String METHOD = "getGroupTypeStr";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId);

        String ua = ctx.getUa();
        boolean isEn = false;
        if (StringUtils.contains(lang, "en"))
            isEn = true;

        String groupType = "";
        if (isPublicCircle(ctx, groupId))
            groupType = isEn ? "public circle" : "公共圈子";
        else if (isActivity(ctx, groupId))
            groupType = isEn ? "activity" : "活动";
        else if (isOrganization(ctx, groupId))
            groupType = isEn ? "organization" : "组织";
        else if (isEvent(ctx, groupId))
            groupType = isEn ? "event" : "事件";
        else
            groupType = isEn ? "group" : "组";


        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return groupType;
    }

    @Override
    public String canApproveUsers(Context ctx, long groupId) {
        final String METHOD = "canApproveUsers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId);

        Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
        int minRole = ROLE_ADMIN;
        long canMemberApprove = rec.getInt(GRP_COL_CAN_MEMBER_APPROVE, 1);

        if (L.isDebugEnabled())
            L.debug(ctx, "canMemberApprove: " + String.valueOf(canMemberApprove));
        if (canMemberApprove == 1) {
            String userIds = getAllMembers(ctx, groupId, -1, -1, "");

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return userIds;
        } else {
            long creator = getCreator(ctx, groupId);
            String admins = getAdmins(ctx, groupId, -1, -1);
            String userIds = StringUtils.isBlank(admins) ? String.valueOf(creator) : creator + "," + admins;

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return userIds;
        }
    }

    public String getGroupRequestType(Context ctx, long groupId, boolean isInvite) {
        if (isPublicCircle(ctx, groupId))
            return isInvite ? REQUEST_PUBLIC_CIRCLE_INVITE : REQUEST_PUBLIC_CIRCLE_JOIN;
        else if (isActivity(ctx, groupId))
            return isInvite ? REQUEST_ACTIVITY_INVITE : REQUEST_ACTIVITY_JOIN;
        else if (isOrganization(ctx, groupId))
            return isInvite ? REQUEST_ORGANIZATION_INVITE : REQUEST_ORGANIZATION_JOIN;
        else if (isEvent(ctx, groupId))
            return isInvite ? REQUEST_EVENT_INVITE : REQUEST_EVENT_JOIN;
        else
            return isInvite ? REQUEST_GENERAL_GROUP_INVITE : REQUEST_GENERAL_GROUP_JOIN;
    }

    @Override
    public int addMember(Context ctx, long groupId, String userId, String message) {
        return addMember(ctx, groupId, userId, message, false);
    }

    @Override
    public int addMember(Context ctx, long groupId, String userId, String message, boolean sendPost) {
        // TODO: check name
        final String METHOD = "addMember";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userId, message);

        String userName = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userId)
                .getFirstRecord().getString("display_name");
        int status = getUserStatusById(ctx, groupId, Long.parseLong(userId));
        L.trace(ctx, "[Method addMember] status: " + status);
        String groupType = getGroupTypeStr(ctx, ctx.getLanguage(), groupId);

        Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
        String groupName = rec.getString(GRP_COL_NAME);
        int isStreamPublic = (int) rec.getInt(GRP_COL_IS_STREAM_PUBLIC);
        int canSearch = (int) rec.getInt(GRP_COL_CAN_SEARCH);
        int canViewMembers = (int) rec.getInt(GRP_COL_CAN_VIEW_MEMBERS);
        long canJoin = rec.getInt(GRP_COL_CAN_JOIN, 1);

        int changedStatus = status;
        if (status == STATUS_INVITED) {
            boolean b = addSimpleMembers(ctx, groupId, Record.of(userId, ROLE_MEMBER));
            if (b) {
                Record dataRec = new Record();
                dataRec.put("group_id", groupId);
                dataRec.put("group_name", groupName);
                String data = dataRec.toString(false, false);
                GlobalLogics.getRequest().dealRelatedRequestsP(ctx, userId, "0", data);

                changedStatus = STATUS_JOINED;

                // send a post
                if (sendPost) {
                    String m = "";
                    String tempNowAttachments = "[]";
                    String template = getBundleString(ctx.getUa(), "platform.join.group.message");
                    String groupSchema = "<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>";
                    String postMsg = SQLTemplate.merge(template, new Object[][]{
                            {"groupType", groupType},
                            {"groupName", groupSchema}
                    });

                    boolean secretly = false;
                    if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
                        secretly = true;
                    GlobalLogics.getStream().autoPost(ctx, userId, TEXT_POST, postMsg, tempNowAttachments, ctx.getAppId(), "", "", m, String.valueOf(groupId), secretly, "", ctx.getUa(), ctx.getLocation(), true, true, true, "", "", false,Constants.POST_SOURCE_SYSTEM);
                }

                // send notification
                Commons.sendNotification(ctx, Constants.NTF_GROUP_JOIN,
                        Commons.createArrayNodeFromStrings(ctx.getAppId()),
                        Commons.createArrayNodeFromStrings(userId),
                        Commons.createArrayNodeFromStrings(String.valueOf(groupId), groupType, groupName),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                        Commons.createArrayNodeFromStrings(String.valueOf(groupId), groupType, groupName),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                        Commons.createArrayNodeFromStrings(String.valueOf(groupId))
                );
            }
        } else if (status != STATUS_JOINED) {
            if (canJoin == 1) {
                boolean b = addSimpleMembers(ctx, groupId, Record.of(userId, ROLE_MEMBER));
                if (b) {
                    changedStatus = STATUS_JOINED;

                    // send notification
                    Commons.sendNotification(ctx, Constants.NTF_GROUP_JOIN,
                            Commons.createArrayNodeFromStrings(ctx.getAppId()),
                            Commons.createArrayNodeFromStrings(userId),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId), groupType, groupName),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId), groupType, groupName),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId))
                    );
                }
            } else if (canJoin == 0) {
                String tos = canApproveUsers(ctx, groupId);

                Record dataRec = new Record();
                dataRec.put("user_id", userId);
                dataRec.put("user_name", userName);
                dataRec.put("group_id", groupId);
                dataRec.put("group_name", groupName);
                String data = dataRec.toString(false, false);
                GlobalLogics.getRequest().createRequests(ctx, tos, userId, ctx.getAppId(), getGroupRequestType(ctx, groupId, false), message, data, "");

                Commons.sendNotification(ctx, Constants.NTF_GROUP_APPLY,
                        Commons.createArrayNodeFromStrings(ctx.getAppId()),
                        Commons.createArrayNodeFromStrings(userId),
                        Commons.createArrayNodeFromStrings(userName, groupType, groupName),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                        Commons.createArrayNodeFromStrings(userName, groupType, groupName, userId, String.valueOf(groupId)),
                        Commons.createArrayNodeFromStrings(message),
                        Commons.createArrayNodeFromStrings(message),
                        Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                        Commons.createArrayNodeFromStrings(String.valueOf(groupId))
                );
                sendGroupNotification(ctx, groupId, new RecordSet(), new GroupApplyNotifSender(), userId, new Object[]{String.valueOf(groupId)}, message,
                        userName, groupType, groupName);

                if (isPublicCircle(ctx, groupId)) {
                    List<String> toIds = StringUtils2.splitList(tos, ",", true);
                    for (String to : toIds) {
                        //TODO　add by wangpeng at 2013-01-09---------
                        try {
                            MessageDelayCombineUtils.sendEmailCombineAndDelayJoinGroup(ctx, userId, to, String.valueOf(groupId), groupName);
                        } catch (Exception e) {
                            L.error(ctx, e, "delay and combine join group  email error!@@@@");
                        }
                    }
                }
                changedStatus = STATUS_APPLIED;
            } else {
                changedStatus = STATUS_NONE;
            }
        }

        Record statusRec = new Record();
        statusRec.put("user_id", Long.parseLong(userId));
        statusRec.put("display_name", userName);
        statusRec.put("identify", "");
        statusRec.put("source", 0);
        statusRec.put("status", changedStatus);
        addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));
        L.trace(ctx, "[Method addMember] changedStatus: " + changedStatus);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return changedStatus;
    }

    @Override
    public RecordSet addMembers(Context ctx, long groupId, Record roles, boolean sendPost) {
        final String METHOD = "addMembers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, roles, sendPost);

        boolean r = addSimpleMembers(ctx, groupId, roles);

        if (r) {
            Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
            String groupName = rec.getString(GRP_COL_NAME);
            int isStreamPublic = (int) rec.getInt(GRP_COL_IS_STREAM_PUBLIC);
            int canSearch = (int) rec.getInt(GRP_COL_CAN_SEARCH);
            int canViewMembers = (int) rec.getInt(GRP_COL_CAN_VIEW_MEMBERS);

            RecordSet recs = new RecordSet();
            for (String userId : roles.keySet()) {
                Record statusRec = new Record();
                statusRec.put("user_id", Long.parseLong(userId));
                statusRec.put("display_name", GlobalLogics.getAccount().getUsersBaseColumns(ctx, userId).getFirstRecord().getString("display_name"));
                statusRec.put("identify", "");
                statusRec.put("source", 0);
                statusRec.put("status", STATUS_JOINED);
                recs.add(statusRec);

                // send a post
//                if (sendPost) {
//                    String m = "";
//                    String tempNowAttachments = "[]";
//                    String groupType = getGroupTypeStr(ctx, groupId);
//                    String template = getBundleString("", "platform.join.group.message");
//                    String groupSchema = "<a href=\"borqs://profile/details?uid=" + groupId + "&tab=2\">" + groupName + "</a>";
//                    String postMsg = SQLTemplate.merge(template, new Object[][]{
//                            {"groupType", groupType},
//                            {"groupName", groupSchema}
//                    });
//
//                    boolean secretly = false;
//                    if ((isStreamPublic == 0) && (canSearch == 0) && (canViewMembers == 0))
//                        secretly = true;
////                    GlobalLogics.getStream().autoPost(ctx, TEXT_POST, postMsg, tempNowAttachments, "", "", m, String.valueOf(groupId), secretly, "", true, true, true, "", "", false);
//                    GlobalLogics.getStream().autoPost(ctx, String.valueOf(ctx.getViewerId()), TEXT_POST, postMsg, tempNowAttachments, ctx.getAppId(), "", "", m, String.valueOf(groupId), secretly, "", ctx.getUa(), ctx.getLocation(), true, true, true, "", "", false);
//                }
            }
            addOrUpdatePendings(ctx, groupId, recs);
        }

        String memberIds = StringUtils2.joinIgnoreBlank(",", roles.keySet());
        RecordSet recs = GlobalLogics.getAccount().getUsersBaseColumns(ctx, memberIds);
        for (Record rec : recs) {
            rec.put("status", STATUS_JOINED);
            rec.put("source", JsonNodeFactory.instance.objectNode());
            rec.put("key", rec.getString("user_id"));
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public boolean removeMembers(Context ctx, long groupId, String members, String newAdmins) {
        final String METHOD = "removeMembers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, members, newAdmins);

        Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
        int canMemberQuit = (int) rec.getInt(GRP_COL_CAN_MEMBER_QUIT, 1);

        String userId = String.valueOf(ctx.getViewerId());
        if (hasRight(ctx, groupId, Long.parseLong(userId), ROLE_ADMIN)) {
            L.debug(ctx, "admin case");
            boolean r = true;
            if (StringUtils.contains(members, userId)) {
                List<String> memberIds = StringUtils2.splitList(members, ",", true);
                memberIds.remove(userId);
                members = StringUtils2.joinIgnoreBlank(",", memberIds);

                if (canMemberQuit == 1) {
                    L.debug(ctx, "admin quit case");
                    String creator = String.valueOf(getCreator(ctx, groupId));
                    String admins = getAdmins(ctx, groupId, -1, -1);

                    int adminCount = StringUtils2.splitList(admins, ",", true).size();
                    if (!StringUtils.equals(creator, "0"))
                        adminCount++;
                    int memberCount = getMembersCount(ctx, groupId);

                    if ((adminCount < 2) && (memberCount > 1)) {
                        L.debug(ctx, "admin quit need grant case");
                        if (StringUtils.isNotBlank(newAdmins)) {
                            Record roles = new Record();
                            long[] newAdminArr = StringUtils2.splitIntArray(newAdmins, ",");
                            for (long newAdmin : newAdminArr) {
                                roles.put(String.valueOf(newAdmin), ROLE_ADMIN);
                            }
                            grants(ctx, groupId, roles);
                        } else {
                            throw new ServerException(GROUP_ADMIN_QUIT_ERROR, "You must grant an admin before quit");
                        }
                    }

                    r = groupDb.deleteMembers(ctx, groupId, userId);
                    if (r) {
                        Record statusRec = new Record();
                        statusRec.put("user_id", Long.parseLong(userId));
                        statusRec.put("display_name", GlobalLogics.getAccount().getUsers(ctx, userId, "display_name").getFirstRecord().getString("display_name"));
                        statusRec.put("identify", "");
                        statusRec.put("source", 0);
                        statusRec.put("status", STATUS_QUIT);
                        addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));
                    }
                    L.op(ctx, "admin quit");

                    if (memberCount <= 1) {
                        L.debug(ctx, "destroy group case");
                        destroyGroup(ctx, String.valueOf(groupId));
                    }
                } else {
                    r = false;
                }
            }
            if (StringUtils.isNotBlank(members)) {
                L.debug(ctx, "kick members case");
                r = r && groupDb.deleteMembers(ctx, groupId, members);
                if (r) {
                    List<String> memberIds = StringUtils2.splitList(members, ",", true);
                    RecordSet recs = new RecordSet();
                    for (String memberId : memberIds) {
                        Record statusRec = new Record();
                        statusRec.put("user_id", Long.parseLong(memberId));
                        statusRec.put("display_name", GlobalLogics.getAccount().getUsers(ctx, memberId, "display_name").getFirstRecord().getString("display_name"));
                        statusRec.put("identify", "");
                        statusRec.put("source", 0);
                        statusRec.put("status", STATUS_KICKED);
                        recs.add(statusRec);
                    }
                    addOrUpdatePendings(ctx, groupId, recs);
                }
            }

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return r;
        } else if (StringUtils.equals(userId, members) && (canMemberQuit == 1)) {
            L.debug(ctx, "member quit case");
            boolean r = groupDb.deleteMembers(ctx, groupId, userId);
            if (r) {
                Record statusRec = new Record();
                statusRec.put("user_id", Long.parseLong(userId));
                statusRec.put("display_name", GlobalLogics.getAccount().getUsers(ctx, userId, "display_name").getFirstRecord().getString("display_name"));
                statusRec.put("identify", "");
                statusRec.put("source", 0);
                statusRec.put("status", STATUS_QUIT);
                addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));
            }

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return r;
        } else {
            L.debug(ctx, "right error case");

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return false;
        }
    }

    @Override
    public boolean grants(Context ctx, long groupId, Record roles) {
        final String METHOD = "grants";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, roles);

        if (hasRight(ctx, groupId, ctx.getViewerId(), ROLE_ADMIN)) {
            L.debug(ctx, "has right case");
            boolean b = groupDb.addOrGrantMembers(ctx, groupId, roles);

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return b;
        }
        else {
            L.debug(ctx, "right error case");
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return false;
        }
    }

    @Override
    public String getMembersByRole(Context ctx, long groupId, int role, int page, int count, String searchKey) {
        final String METHOD = "getMembersByRole";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, role, page, count, searchKey);

        String memberIds = groupDb.getMembersByRole0(ctx, groupId, role, page, count, searchKey);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return memberIds;
    }

    @Override
    public String getAdmins(Context ctx, long groupId, int page, int count) {
        final String METHOD = "getAdmins";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, page, count);

        String adminIds = groupDb.getMembersByRole0(ctx, groupId, ROLE_ADMIN, page, count, "");

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return adminIds;
    }

    @Override
    public long getCreator(Context ctx, long groupId) {
        final String METHOD = "getCreator";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId);

        long userId = Long.parseLong(groupDb.getMembersByRole0(ctx, groupId, ROLE_CREATOR, -1, -1, ""));

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return userId;
    }

    @Override
    public String getAllMembers(Context ctx, long groupId, int page, int count, String searchKey) {
        final String METHOD = "getAllMembers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, page, count, searchKey);

        String memberIds = groupDb.getMembersByRole0(ctx, groupId, 0, page, count, searchKey);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return memberIds;
    }

    @Override
    public String getMembers(Context ctx, String groupIds, int page, int count) {
        final String METHOD = "getMembers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupIds, page, count);

        String memberIds = groupDb.getMembers0(ctx, groupIds, page, count);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return memberIds;
    }

    @Override
    public int getMembersCount(Context ctx, long groupId) {
        final String METHOD = "getMembersCount";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId);

        int count = groupDb.getMembersCount0(ctx, groupId);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return count;
    }

    @Override
    public Record getMembersCounts(Context ctx, String groupIds) {
        final String METHOD = "getMembersCounts";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupIds);

        Record rec = groupDb.getMembersCounts0(ctx, groupIds);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    @Override
    public boolean hasRight(Context ctx, long groupId, long member, int minRole) {
        final String METHOD = "hasRight";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, member, minRole);

        boolean b = groupDb.hasRight0(ctx, groupId, member, minRole);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;
    }

    @Override
    public boolean addOrUpdatePendings(Context ctx, long groupId, RecordSet statuses) {
        final String METHOD = "addOrUpdatePendings";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, statuses);

        boolean b = groupDb.addOrUpdatePendings0(ctx, groupId, statuses);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;
    }

    @Override
    public RecordSet getPendingUsersByStatus(Context ctx, long groupId, long source, String status, int page, int count, String searchKey) {
        final String METHOD = "getPendingUsersByStatus";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, source, status, page, count, searchKey);

        RecordSet recs = groupDb.getPendingUserByStatus0(ctx, groupId, source, status, page, count, searchKey);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public int getUserStatusById(Context ctx, long groupId, long userId) {
        final String METHOD = "getUserStatusById";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userId);

        int status = groupDb.getUserStatusById0(ctx, groupId, userId);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return status;
    }

    @Override
    public int getUserStatusByIdentify(Context ctx, long groupId, String identify) {
        final String METHOD = "getUserStatusByIdentify";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, identify);

        int status = groupDb.getUserStatusByIdentify0(ctx, groupId, identify);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return status;
    }

    @Override
    public Record getUserStatusByIds(Context ctx, long groupId, String userIds) {
        final String METHOD = "getUserStatusByIds";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userIds);

        Record rec = groupDb.getUserStatusByIds0(ctx, groupId, userIds);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    @Override
    public Record getUserStatusByIdentifies(Context ctx, long groupId, String identifies) {
        final String METHOD = "getUserStatusByIdentifies";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, identifies);

        Record rec = groupDb.getUserStatusByIdentifies0(ctx, groupId, identifies);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    @Override
    public Record getUsersCounts(Context ctx, String groupIds, int status) {
        final String METHOD = "getUsersCounts";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupIds, status);

        Record rec = groupDb.getUsersCounts0(ctx, groupIds, status);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return rec;
    }

    @Override
    public boolean updateUserIdByIdentify(Context ctx, String userId, String identify) {
        final String METHOD = "updateUserIdByIdentify";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, identify);

        boolean b = groupDb.updateUserIdByIdentify0(ctx, userId, identify);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;
    }

    @Override
    public String getSourcesById(Context ctx, long groupId, String userId) {
        final String METHOD = "getSourcesById";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userId);

        String sources = groupDb.getSourcesById0(ctx, groupId, userId);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return sources;
    }

    @Override
    public String getSourcesByIdentify(Context ctx, long groupId, String identify) {
        final String METHOD = "getSourcesByIdentify";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, identify);

        String sources = groupDb.getSourcesByIdentify0(ctx, groupId, identify);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return sources;
    }

    @Override
    public boolean isGroup(Context ctx, long id) {
        return id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END;
    }

    @Override
    public boolean isPublicCircle(Context ctx, long id) {
        return id >= PUBLIC_CIRCLE_ID_BEGIN && id < ACTIVITY_ID_BEGIN;
    }

    @Override
    public boolean isActivity(Context ctx, long id) {
        return id >= ACTIVITY_ID_BEGIN && id < DEPARTMENT_ID_BEGIN;
    }

    @Override
    public boolean isOrganization(Context ctx, long id) {
        return id >= DEPARTMENT_ID_BEGIN && id < GENERAL_GROUP_ID_BEGIN;
    }

    @Override
    public boolean isGeneralGroup(Context ctx, long id) {
        return id >= GENERAL_GROUP_ID_BEGIN && id < EVENT_ID_BEGIN;
    }

    @Override
    public boolean isEvent(Context ctx, long id) {
        return id >= EVENT_ID_BEGIN && id < EVENT_ID_END;
    }

    @Override
    public boolean isSpecificType(Context ctx, long id, String type) {
        Configuration conf = GlobalConfig.get();
        long begin = conf.getInt("group." + type + ".begin", GENERAL_GROUP_ID_BEGIN);
        long end = conf.getInt("group." + type + ".end", EVENT_ID_BEGIN);
        return id >= begin && id < end;
    }

    @Override
    public boolean defaultMemberNotification(Context ctx, long groupId, String userIds) {
        final String METHOD = "defaultMemberNotification";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userIds);

        boolean b = groupDb.defaultMemberNotification0(ctx, groupId, userIds);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;
    }

    @Override
    public boolean updateMemberNotification(Context ctx, long groupId, String userId, Record notif) {
        final String METHOD = "updateMemberNotification";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userId, notif);

        boolean b = groupDb.updateMemberNotification0(ctx, groupId, userId, notif);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return b;
    }

    @Override
    public RecordSet getMembersNotification(Context ctx, long groupId, String userIds) {
        final String METHOD = "getMembersNotification";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userIds);

        RecordSet recs = groupDb.getMembersNotification0(ctx, groupId, userIds);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getGroupUsersByStatus(Context ctx, long groupId, String status, int page, int count, String searchKey) {
        final  String METHOD = "getGroupUsersByStatus";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, status, page, count, searchKey);

        List<String> l = StringUtils2.splitList(status, ",", true);
            Record group_ = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, GROUP_ID_END, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
            RecordSet recs = new RecordSet();

            long canViewMembers = group_.getInt(Constants.GRP_COL_CAN_VIEW_MEMBERS, 1);
            if (l.contains(String.valueOf(STATUS_JOINED)) && (canViewMembers == 1 || hasRight(ctx, groupId, ctx.getViewerId(), ROLE_MEMBER))) {
                l.remove(String.valueOf(STATUS_JOINED));
                // inject
                RecordSet rs = groupDb.getGroupUsersByStatus0(ctx, groupId, status, page, count, searchKey);

                RecordSet rs0 = new RecordSet();
                RecordSet rs1 = new RecordSet();
                //divide the rs into 2 parts
                for(Record r :rs){
                    String type = r.getString("t");
                    if("group_members".equals(type)){
                        rs0.add(r);
                    }else{
                        rs1.add(r);
                    }
                }

                if(rs0.size()>0)
                    getAllMembers(ctx, groupId, page, searchKey, l, recs, rs0);
                if(rs1.size()>0 && l.size()>0)
                    getPenddingGroupMembers(ctx, groupId, recs, rs1);

                if (L.isTraceEnabled())
                    L.traceEndCall(ctx, METHOD);
                return recs;

            } else if (!l.isEmpty()) {
                l.remove(String.valueOf(STATUS_JOINED));
                if (!l.isEmpty()) {

                    RecordSet recs0 = getPendingUsersByStatus(ctx, groupId, 0, StringUtils2.joinIgnoreBlank(",", l), page, count, searchKey);
                    getPenddingGroupMembers(ctx, groupId, recs, recs0);

                    if (L.isTraceEnabled())
                        L.traceEndCall(ctx, METHOD);
                    return recs;
                }
            }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    private RecordSet getAllMembers(Context ctx, long groupId, int page, String searchKey, List<String> l, RecordSet recs, RecordSet rs) {
        // analyze
        String memberIds = rs.joinColumnValues("user_id", ",");


        //String memberIds = toStr(group.getAllMembers(groupId, page, count, searchKey));


        RecordSet members = GlobalLogics.getAccount().getUsersBaseColumns(ctx, memberIds);
        LinkedHashMap<String, Record> map = new LinkedHashMap<String, Record>();
        for (Record member : members) {
            map.put(member.getString("user_id"), member);
        }

        String creator = String.valueOf(getCreator(ctx, groupId));
        String admins = getAdmins(ctx, groupId, -1, -1);
        RecordSet adminRecs = new RecordSet();
        Record creatorRec = GlobalLogics.getAccount().getUsersBaseColumns(ctx, creator).getFirstRecord();
        if (!creatorRec.isEmpty()) {
            creatorRec.put("role_in_group", ROLE_CREATOR);
            creatorRec.put("status", STATUS_JOINED);
            adminRecs.add(0, creatorRec);
        }
        RecordSet adminRecs0 = GlobalLogics.getAccount().getUsersBaseColumns(ctx, admins);
        if (!adminRecs0.isEmpty()) {
            for (Record admin : adminRecs0) {
                admin.put("role_in_group", ROLE_ADMIN);
                admin.put("status", STATUS_JOINED);
            }
            adminRecs.addAll(adminRecs0);
        }
        for (Record rec : rs) {
            String memberId = rec.getString("user_id");
            Record member = map.get(memberId);
            int memberRole = ROLE_MEMBER;
            if (StringUtils.equals(creator, memberId))
                memberRole = ROLE_CREATOR;
            else if (StringUtils.contains(admins, memberId))
                memberRole = ROLE_ADMIN;
            member.put("role_in_group", memberRole);
            member.put("status", STATUS_JOINED);
            recs.add(member);
        }

        if (StringUtils.isBlank(searchKey)) {
            recs.removeAll(adminRecs);
            if (page == 0 || page == -1)
                recs.addAll(0, adminRecs);
        }
        l.remove(String.valueOf(STATUS_JOINED));
        return attachEmployeeInfos(ctx, groupId, recs);
    }

    private void getPenddingGroupMembers(Context ctx, long groupId, RecordSet recs, RecordSet recs0) {
        RecordSet recs1 = new RecordSet(); // borqs id
        RecordSet recs2 = new RecordSet(); // identify
        for (Record rec : recs0) {
            String userId = rec.getString("user_id", "0");
            String identify = rec.getString("identify", "");
            String sourceIds = "";
            if (!StringUtils.equals(userId, "0")) {
                Record user = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userId).getFirstRecord();
                rec.putAll(user);

                sourceIds = getSourcesById(ctx, groupId, userId);
            } else
                sourceIds = getSourcesByIdentify(ctx, groupId, identify);
            rec.put("role_in_group", ROLE_GUEST);
            if (StringUtils.isNotBlank(sourceIds)) {
                RecordSet source = GlobalLogics.getAccount().getUsersBaseColumns(ctx, sourceIds);
                rec.put("sources", source.toJsonNode());
            }

            Record r = rec.copy();
            if (!StringUtils.equals(userId, "0"))
                recs1.add(r);
            else
                recs2.add(r);
        }
        recs1.unique("user_id");
        recs2.unique("identify");
        recs.addAll(recs1);
        recs.addAll(recs2);
    }

//    @Override
//    public int getTypeByStr(Context ctx, String str) {
//        L.debug(ctx, "id string: " + str);
//        int type = IDENTIFY_TYPE_BORQS_ID; //0 - borqs id   1 - email  2 - phone  3 - local circle  4- virtual id  5 - group id
//
//        if (StringValidator.validateEmail(str))
//            type = IDENTIFY_TYPE_EMAIL;
//        else if (StringValidator.validatePhone(str))
//            type = IDENTIFY_TYPE_PHONE;
//        else if (StringUtils.startsWith(str, "#"))
//            type = IDENTIFY_TYPE_LOCAL_CIRCLE;
//        else if (StringUtils.isNotBlank(str) && str.length() == 19)
//            type = IDENTIFY_TYPE_VIRTUAL_ID;
//        else if (StringUtils.startsWith(str, "$")) {
//            long id = Long.parseLong(StringUtils.removeStart(str, "$"));
//            if (isGroup(ctx, id))
//                type = IDENTIFY_TYPE_GROUP_ID;
//            else if (getUserTypeById(id) == PAGE_OBJECT)
//                type = IDENTIFY_TYPE_PAGE_ID;
//        }
//
//        L.debug(ctx, "type: " + type);
//        return type;
//    }

    @Override
    public Record getTypeByStr(Context ctx, String str) {
        L.info(ctx, "in id string: " + str);
        int type = IDENTIFY_TYPE_UNKNOWN;
        boolean isContact = StringUtils.startsWith(str, "*");

        str = StringUtils.removeStart(str, "*");
        str = StringUtils.removeStart(str, "$");


        if (StringValidator.validateEmail(str))
            type = IDENTIFY_TYPE_EMAIL;
        else if (StringValidator.validatePhone(str))
            type = IDENTIFY_TYPE_PHONE;
        else if (isContact && NumberUtils.isNumber(str))
            type = IDENTIFY_TYPE_PHONE;
        else if (StringUtils.startsWith(str, "#")) {
            str = StringUtils.removeStart(str, "#");
            long id = Long.parseLong(str);
            if (isGroup(ctx, id))
                type = IDENTIFY_TYPE_GROUP_ID;
            else if (getUserTypeById(id) == PAGE_OBJECT)
                type = IDENTIFY_TYPE_PAGE_ID;
            else
                type = IDENTIFY_TYPE_LOCAL_CIRCLE;
        }
        else if (StringUtils.isNotBlank(str) && str.length() == 19)
            type = IDENTIFY_TYPE_VIRTUAL_ID;
        else {
            if (!NumberUtils.isNumber(str)) {
                type = IDENTIFY_TYPE_UNKNOWN;
            } else {
                long id = Long.parseLong(str);
                if (isGroup(ctx, id))
                    type = IDENTIFY_TYPE_GROUP_ID;
                else if (getUserTypeById(id) == PAGE_OBJECT)
                    type = IDENTIFY_TYPE_PAGE_ID;
                else if (getUserTypeById(id) == USER_OBJECT)
                    type = IDENTIFY_TYPE_BORQS_ID;
                else
                    type = IDENTIFY_TYPE_UNKNOWN;
            }
        }

        L.info(ctx, "out id string: " + str);
        L.info(ctx, "type: " + type);
        return Record.of("type", type, "str", str);
    }

    @Override
    public RecordSet inviteMembers(Context ctx, long groupId, String userIds, String message, boolean sendEmail, boolean sendSms) {
        final String METHOD = "inviteMembers";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userIds, message);

        String viewerId = ctx.getViewerIdString();
        ArrayList<String> applied = new ArrayList<String>();
        ArrayList<String> toInvite = new ArrayList<String>();

        Record statuses = getUserStatusByIds(ctx, groupId, userIds);
        L.debug(ctx, "statuses: " + statuses.toString());
        for (Map.Entry<String, Object> entry : statuses.entrySet()) {
            String userId = entry.getKey();
            int status = ((Long) entry.getValue()).intValue();

            if (status == STATUS_APPLIED)
                applied.add(userId);
            else if (status != STATUS_JOINED)
                toInvite.add(userId);
        }

        Record source = GlobalLogics.getAccount().getUsersBaseColumns(ctx, viewerId).getFirstRecord();
        String viewerName = source.getString("display_name");
        Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
        String groupName = rec.getString(GRP_COL_NAME);
        long canMemberInvite = rec.getInt(GRP_COL_CAN_MEMBER_INVITE, 1);

        RecordSet statusRecs = new RecordSet();
        RecordSet recs0 = new RecordSet();
        RecordSet recs = new RecordSet();

        // applied
        if (!applied.isEmpty()) {
            L.debug(ctx, "applied case");
            Record appliedRec = new Record();
            for (String userId : applied)
                appliedRec.put(userId, ROLE_MEMBER);
            addSimpleMembers(ctx, groupId, appliedRec);

            String datas = "";
            String appliedIds = StringUtils2.joinIgnoreBlank(",", applied);
            recs0 = GlobalLogics.getAccount().getUsersBaseColumns(ctx, appliedIds);
            for (Record r : recs0) {
                String userId = r.getString("user_id");
                String name = r.getString("display_name");
                r.put("status", STATUS_JOINED);
                r.put("source", source.toJsonNode());
                r.put("key", userId);

                Record dataRec = new Record();
                dataRec.put("user_id", userId);
                dataRec.put("user_name", name);
                dataRec.put("group_id", groupId);
                dataRec.put("group_name", groupName);
                String data = dataRec.toString(false, false);
                datas += data + "|";

                Record statusRec = new Record();
                statusRec.put("user_id", Long.parseLong(userId));
                statusRec.put("display_name", name);
                statusRec.put("identify", "");
                statusRec.put("source", Long.parseLong(viewerId));
                statusRec.put("status", STATUS_JOINED);
                statusRecs.add(statusRec);
            }
            datas = StringUtils.substringBeforeLast(datas, "|");
            GlobalLogics.getRequest().dealRelatedRequestsP(ctx, "0", appliedIds, datas);
        }

        //to invite
        if (!toInvite.isEmpty()) {
            L.debug(ctx, "invite case");
            String toInviteIds = StringUtils2.joinIgnoreBlank(",", toInvite);
            if (canMemberInvite == 1 || hasRight(ctx, groupId, Long.parseLong(viewerId), ROLE_ADMIN)) {
                Record dataRec = new Record();
                dataRec.put("group_id", groupId);
                dataRec.put("group_name", groupName);
                String data = dataRec.toString(false, false);
                GlobalLogics.getRequest().createRequests(ctx, toInviteIds, viewerId, ctx.getAppId(), getGroupRequestType(ctx, groupId, true), message, data, "");

                String groupType = getGroupTypeStr(ctx, ctx.getLanguage(), groupId);
                int toSize = toInvite.size();
                if (toSize < 20) {
                    Commons.sendNotification(ctx, Constants.NTF_GROUP_INVITE,
                            Commons.createArrayNodeFromStrings(ctx.getAppId()),
                            Commons.createArrayNodeFromStrings(viewerId),
                            Commons.createArrayNodeFromStrings(viewerName, groupType, groupName, "邀请"),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                            Commons.createArrayNodeFromStrings(viewerName, groupType, groupName, viewerId, String.valueOf(groupId), "邀请"),
                            Commons.createArrayNodeFromStrings(message),
                            Commons.createArrayNodeFromStrings(message),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                            Commons.createArrayNodeFromStrings(toInviteIds)
                    );
                } else {
                    int count = toSize / 20;
                    for (int i = 0; i <= count; i++) {
                        int toIndex = (i * 20 + 20) > toSize ? toSize : i * 20 + 20;
                        List<String> subList = toInvite.subList(i * 20, toIndex);
                        String subToIds = StringUtils2.joinIgnoreBlank(",", subList);
                        Commons.sendNotification(ctx, Constants.NTF_GROUP_INVITE,
                                Commons.createArrayNodeFromStrings(ctx.getAppId()),
                                Commons.createArrayNodeFromStrings(viewerId),
                                Commons.createArrayNodeFromStrings(viewerName, groupType, groupName, "邀请"),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(),
                                Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                                Commons.createArrayNodeFromStrings(viewerName, groupType, groupName, viewerId, String.valueOf(groupId), "邀请"),
                                Commons.createArrayNodeFromStrings(message),
                                Commons.createArrayNodeFromStrings(message),
                                Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                                Commons.createArrayNodeFromStrings(subToIds)
                        );
                    }
                }
                sendGroupNotification(ctx, groupId, new RecordSet(), new GroupInviteNotifSender(), viewerId, new Object[]{toInviteIds}, message,
                        viewerName, groupType, groupName, "邀请");

                recs = GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), toInviteIds, "user_id, display_name,  image_url,perhaps_name,login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3", false);
                for (Record r : recs) {
                    String userId = r.getString("user_id");
                    if (!StringUtils.equals(userId, viewerId)) {
                        String name = r.getString("display_name");
                        r.put("status", STATUS_INVITED);
                        r.put("source", source.toJsonNode());
                        r.put("key", userId);

                        Record statusRec = new Record();
                        statusRec.put("user_id", Long.parseLong(userId));
                        statusRec.put("display_name", name);
                        statusRec.put("identify", "");
                        statusRec.put("source", Long.parseLong(viewerId));
                        statusRec.put("status", STATUS_INVITED);
                        statusRecs.add(statusRec);

                        //send mail
                        if (sendEmail || isEvent(ctx, groupId)) {
                            Record simpleGroup = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, GROUP_ID_END, String.valueOf(groupId), "start_time,end_time,address,description").getFirstRecord();
                            String adminIds = getCreatorAndAdmins(ctx, groupId);
                            RecordSet members = getGroupUsersByStatus(ctx, groupId, String.valueOf(STATUS_JOINED), -1, -1, "");
                            String[] emails = new String[3];
                            emails[0] = r.getString("login_email1", "");
                            emails[1] = r.getString("login_email2", "");
                            emails[2] = r.getString("login_email3", "");
                            for (int i = 0; i < 3; i++) {
                                if (StringUtils.isNotBlank(emails[i])) {
                                    groupEmailInvite(ctx, groupId, simpleGroup, adminIds, members, userId, name, viewerId, viewerName, emails[i], message);
                                }
                            }
                        }

                        //send sms
                        if (sendSms) {
                            String[] phones = new String[3];
                            phones[0] = r.getString("login_phone1", "");
                            phones[1] = r.getString("login_phone2", "");
                            phones[2] = r.getString("login_phone3", "");
                            for (int i = 0; i < 3; i++) {
                               if (StringUtils.isNotBlank(phones[i])) {
                                   groupSmsInvite(ctx, groupId, groupName, userId, name, viewerId, viewerName, phones[i], message);
                               }
                            }
                        }
                    }
                }
            }
        }
        if (!statusRecs.isEmpty())
            addOrUpdatePendings(ctx, groupId, statusRecs);

        recs.addAll(recs0);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public Record inviteMember(Context ctx, long groupId, String to, String name, String message, boolean sendEmail, boolean sendSms) {
        final String METHOD = "inviteMember";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, to, name, message);

        String viewerId = ctx.getViewerIdString();
        int type = (int)(getTypeByStr(ctx, to).getInt("type"));
        L.trace(ctx, "[Method inviteMember] type: " + type);

        int status = STATUS_NONE;
        if (type == IDENTIFY_TYPE_BORQS_ID)
            status = getUserStatusById(ctx, groupId, Long.parseLong(to));
        else
            status = getUserStatusByIdentify(ctx, groupId, to);
        L.trace(ctx, "[Method inviteMember] status: " + status);

        String userId = to;
        String key = "email";
        if (type == IDENTIFY_TYPE_PHONE)
            key = "phone";
        if (type != IDENTIFY_TYPE_BORQS_ID) {
                RecordSet recs = GlobalLogics.getSocialContacts().findBorqsIdFromContactInfo(ctx, RecordSet.of(Record.of(key, to)));
                userId = recs.getFirstRecord().getString("user_id", "0");

                if (!StringUtils.equals(userId, "0")) {
                    updateUserIdByIdentify(ctx, userId, to);
                }
        }
        L.trace(ctx, "[Method inviteMember] userId: " + userId);

        Record source = GlobalLogics.getAccount().getUsersBaseColumns(ctx, viewerId).getFirstRecord();
        String viewerName = source.getString("display_name");
        Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
        String groupName = rec.getString(GRP_COL_NAME);
        int changedStatus = status;
        if (status == STATUS_APPLIED) {
            boolean b = addSimpleMembers(ctx, groupId, Record.of(userId, ROLE_MEMBER));
            if (b) {
                Record dataRec = new Record();
                dataRec.put("user_id", userId);
                dataRec.put("user_name", name);
                dataRec.put("group_id", groupId);
                dataRec.put("group_name", groupName);
                String data = dataRec.toString(false, false);
                GlobalLogics.getRequest().dealRelatedRequestsP(ctx, "0", userId, data);

                changedStatus = STATUS_JOINED;
            }
        } else if (status != STATUS_JOINED) {
            long canMemberInvite = rec.getInt(GRP_COL_CAN_MEMBER_INVITE, 1);
            long needInvitedConfirm = rec.getInt(GRP_COL_NEED_INVITED_CONFIRM, 1);
            if (canMemberInvite == 1 || hasRight(ctx, groupId, Long.parseLong(viewerId), ROLE_ADMIN)) {
                Record simpleGroup = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, GROUP_ID_END, String.valueOf(groupId), "start_time,end_time,address,description").getFirstRecord();
                String adminIds = getCreatorAndAdmins(ctx, groupId);
                RecordSet members = getGroupUsersByStatus(ctx, groupId, String.valueOf(STATUS_JOINED), -1, -1, "");

                if (!StringUtils.equals(userId, "0")) {
                    if (needInvitedConfirm == 1) {
                        Record dataRec = new Record();
                        dataRec.put("group_id", groupId);
                        dataRec.put("group_name", groupName);
                        String data = dataRec.toString(false, false);
                        GlobalLogics.getRequest().createRequest(ctx, userId, viewerId, ctx.getAppId(), getGroupRequestType(ctx, groupId, true), message, data, "");

                        String userCols = "user_id, display_name, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3";
                        Record user = GlobalLogics.getAccount().getUser(ctx, userId, userId, userCols, false);
                        //send mail
                        if (sendEmail || isEvent(ctx, groupId)) {
                            String[] emails = new String[3];
                            emails[0] = user.getString("login_email1", "");
                            emails[1] = user.getString("login_email2", "");
                            emails[2] = user.getString("login_email3", "");
                            for (int i = 0; i < 3; i++) {
                                if (StringUtils.isNotBlank(emails[i])) {
                                    groupEmailInvite(ctx, groupId, simpleGroup, adminIds, members, userId, name, viewerId, viewerName, emails[i], message);
                                }
                            }
                        }
                        //send sms
                        if (sendSms) {
                            String[] phones = new String[3];
                            phones[0] = user.getString("login_phone1", "");
                            phones[1] = user.getString("login_phone2", "");
                            phones[2] = user.getString("login_phone3", "");
                            for (int i = 0; i < 3; i++) {
                                if (StringUtils.isNotBlank(phones[i])) {
                                    groupSmsInvite(ctx, groupId, groupName, userId, name, viewerId, viewerName, phones[i], message);
                                }
                            }
                        }

                        String groupType = getGroupTypeStr(ctx, ctx.getLanguage(), groupId);
                            Commons.sendNotification(ctx, Constants.NTF_GROUP_INVITE,
                                    Commons.createArrayNodeFromStrings(ctx.getAppId()),
                                    Commons.createArrayNodeFromStrings(viewerId),
                                    Commons.createArrayNodeFromStrings(viewerName, groupType, groupName, "邀请"),
                                    Commons.createArrayNodeFromStrings(),
                                    Commons.createArrayNodeFromStrings(),
                                    Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                                    Commons.createArrayNodeFromStrings(viewerName, groupType, groupName, viewerId, String.valueOf(groupId), "邀请"),
                                    Commons.createArrayNodeFromStrings(message),
                                    Commons.createArrayNodeFromStrings(message),
                                    Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                                    Commons.createArrayNodeFromStrings(userId)
                            );
                            sendGroupNotification(ctx, groupId, new RecordSet(), new GroupInviteNotifSender(), viewerId, new Object[]{userId}, message,
                                    viewerName, groupType, groupName, "邀请");
                        changedStatus = STATUS_INVITED;
                    } else {
                        boolean b = addSimpleMembers(ctx, groupId, Record.of(userId, ROLE_MEMBER));
                        if (b) {
                            changedStatus = STATUS_JOINED;
                        }
                    }
                }
                if (type == IDENTIFY_TYPE_EMAIL) {
                    groupEmailInvite(ctx, groupId, simpleGroup, adminIds, members, userId, name, viewerId, viewerName, to, message);
                    changedStatus = STATUS_INVITED;
                }
                if (type == IDENTIFY_TYPE_PHONE) {
                    groupSmsInvite(ctx, groupId, groupName, userId, name, viewerId, viewerName, to, message);
                    changedStatus = STATUS_INVITED;
                }
            }
        }

        Record statusRec = new Record();
        statusRec.put("user_id", Long.parseLong(userId));
        statusRec.put("display_name", name);
        statusRec.put("identify", type != IDENTIFY_TYPE_BORQS_ID ? to : "");
        statusRec.put("source", Long.parseLong(viewerId));
        statusRec.put("status", changedStatus);
        addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));
        L.trace(ctx, "[Method inviteMember] changedStatus: " + changedStatus);

        Record r = new Record();
        if (!StringUtils.equals(userId, "0")) {
            r = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userId).getFirstRecord();
            r.put("status", changedStatus);
            r.put("source", source.toJsonNode());
        } else {
            r = statusRec.copy();
            r.put("source", source.toJsonNode());
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return r;
    }


    private void circleEmailInvite(Context ctx, Record simpleGroup, String adminIds, RecordSet members, String userName, String viewerName, String register, String groupType,
                                  String acceptUrl, String rejectUrl, String email, String message) {
        final String METHOD = "circleEmailInvite";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, simpleGroup, adminIds, members, userName, viewerName, register, groupType, acceptUrl, rejectUrl, email, message);

        String description = simpleGroup.getString("description");
        String groupName = simpleGroup.getString(GRP_COL_NAME);
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        message = StringUtils.isNotBlank(message) ? "Here is his postscript: <br/>    " + message : "";

        map.put("displayName", userName);
        map.put("fromName", Commons.formatEmailUserName(ctx, ctx.getViewerIdString()));
        map.put("register", register);
        map.put("groupType", groupType);
        map.put("groupName", groupName);
        map.put("acceptUrl", GlobalLogics.getShortUrl().generalShortUrl(acceptUrl));
        map.put("rejectUrl", GlobalLogics.getShortUrl().generalShortUrl(rejectUrl));
        map.put("message", message);
        map.put("description", description);
        String imageUrl = GlobalLogics.getAccount().getUser(ctx, ctx.getViewerIdString(), ctx.getViewerIdString(), "large_image_url").getString("large_image_url");
        map.put("icon", imageUrl);

        String adminName = "";
        if (StringUtils.isNotBlank(adminIds)) {
            String adminId = StringUtils.substringBefore(adminIds, ",");
            adminName = Commons.formatEmailUserName(ctx, adminId);
        }
        map.put("creator", adminName);
        map.put("joined", members.joinColumnValues("display_name", ", "));

        String template = Constants.getBundleString(ctx.getUa(), "platform.group.email.invite.subject");
        String subject = SQLTemplate.merge(template, new Object[][]{
                {"fromName", viewerName},
                {"groupType", groupType},
                {"groupName", groupName}
        });

        L.debug(ctx, "acceptUrl: " + acceptUrl);
        L.debug(ctx, "rejectUrl: " + rejectUrl);

        GlobalLogics.getEmail().sendCustomEmailP(ctx,subject, email, email, "circle_invite.ftl", map, Constants.EMAIL_ESSENTIAL, ctx.getLanguage());

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }

    private void eventEmailInvite(Context ctx, Record simpleGroup, String adminIds, RecordSet members, String userName, String viewerName, String register, String groupType,
                                  String acceptUrl, String rejectUrl, String email, String message) {
        final String METHOD = "eventEmailInvite";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, simpleGroup, adminIds, members, userName, viewerName, register, groupType, acceptUrl, rejectUrl, email, message);

        String groupName = simpleGroup.getString(GRP_COL_NAME);
        long nStartTime = simpleGroup.getInt("start_time");
        String startTime = DateUtils.formatDateLocale(nStartTime);
        String endTime = simpleGroup.getInt("end_time") == 0 ? "No deadline" : DateUtils.formatDateLocale(simpleGroup.getInt("end_time"));
        String month = DateUtils.getMonth(nStartTime);
        String day = DateUtils.getDay(nStartTime);
        String weekday = DateUtils.getWeekday(nStartTime);
        String time = DateUtils.getTime(nStartTime);

        String address = "";
        String addrJsonStr = simpleGroup.getString("address");
        String description = simpleGroup.getString("description");
        if (StringUtils.isNotBlank(addrJsonStr)) {
            RecordSet addrRecs = RecordSet.fromJson(addrJsonStr);
            if (addrRecs != null) {
                Record addrRec = addrRecs.getFirstRecord();
                if (addrRec != null) {
                    String addrCountry = addrRec.getString("country", "");
                    String addrState = addrRec.getString("state", "");
                    String addrCity = addrRec.getString("city", "");
                    String addrStreet = addrRec.getString("street", "");
                    String addrCode = addrRec.getString("postal_code", "");
                    String addrBox = addrRec.getString("po_box", "");
                    String addrExt = addrRec.getString("extended_address", "");
                    address = addrCountry + addrState + addrCity + addrStreet + addrCode + addrExt + addrBox;
                }
            }
        }
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        message = StringUtils.isNotBlank(message) ? "Here is his postscript: <br/>    " + message : "";

        map.put("displayName", userName);
        map.put("fromName", Commons.formatEmailUserName(ctx, ctx.getViewerIdString()));
        map.put("register", register);
        map.put("groupType", groupType);
        map.put("groupName", groupName);
        map.put("acceptUrl", GlobalLogics.getShortUrl().generalShortUrl(acceptUrl));
        map.put("rejectUrl", GlobalLogics.getShortUrl().generalShortUrl(rejectUrl));
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("address", address);
        map.put("message", message);
        map.put("description", description);
        map.put("month", month);
        map.put("day", day);
        map.put("weekday", weekday);
        map.put("time", time);

        String adminName = "";
        if (StringUtils.isNotBlank(adminIds)) {
            String adminId = StringUtils.substringBefore(adminIds, ",");
            adminName = Commons.formatEmailUserName(ctx, adminId);
        }
        map.put("creator", adminName);
        map.put("joined", members.joinColumnValues("display_name", ", "));

        String template = Constants.getBundleString(ctx.getUa(), "platform.group.email.invite.subject");
        String subject = SQLTemplate.merge(template, new Object[][]{
                {"fromName", viewerName},
                {"groupType", groupType},
                {"groupName", groupName}
        });

        L.debug(ctx, "address: " + address);
        L.debug(ctx, "acceptUrl: " + acceptUrl);
        L.debug(ctx, "rejectUrl: " + rejectUrl);

        GlobalLogics.getEmail().sendCustomEmailP(ctx,subject, email, email, "event_invite3.ftl", map, Constants.EMAIL_ESSENTIAL, ctx.getLanguage());

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }

    private void groupEmailInvite(Context ctx, long groupId, Record simpleGroup, String adminIds, RecordSet members, String userId, String userName, String viewerId,
                                  String viewerName, String email, String message) {
        final String METHOD = "groupEmailInvite";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, simpleGroup, adminIds, members, userId, userName, viewerId, viewerName, email, message);

        boolean isEvent = (groupId >= EVENT_ID_BEGIN && groupId < EVENT_ID_END);

        String ua = ctx.getUa();
        String lang = parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        boolean isEn = isEvent ? true : StringUtils.equals(lang, "en");
        String groupType = isEvent ? "event" : getGroupTypeStr(ctx, ctx.getLanguage(), groupId);

        String acceptUrl = "";
        String rejectUrl = "";
        String register = "";
        if (StringUtils.equals(userId, "0")) {
            String info = FeedbackParams.toSegmentedBase64(true, "/", email, userName, viewerId);
            acceptUrl = "http://" + SERVER_HOST + "/account/invite?info=" + info + "&group_id=" + groupId;
            rejectUrl = "http://" + SERVER_HOST + "/v2/group/deal_invite?name=" + userName + "&identify=" + email
                    + "&group_id=" + groupId + "&source=" + viewerId;
            register = isEn ? "active borqs account and" : "激活播思账号并";
        } else {
            String s = "http://" + SERVER_HOST + "/v2/group/deal_invite?user_id=" + userId + "&source=" + viewerId + "&group_id=" + groupId + "&accept=";
            acceptUrl = s + true;
            rejectUrl = s + false;
        }

        if (isEvent) {
            eventEmailInvite(ctx, simpleGroup, adminIds, members, userName, viewerName, register, groupType, acceptUrl, rejectUrl, email, message);
        } else {
//            String template = getBundleString(ua, "platform.group.email.invite.content");
//            String emailContent = SQLTemplate.merge(template, new Object[][]{
//                    {"displayName", userName},
//                    {"fromName", viewerName},
//                    {"register", register},
//                    {"groupType", groupType},
//                    {"groupName", groupName},
//                    {"acceptUrl", GlobalLogics.getShortUrl().generalShortUrl(acceptUrl)},
//                    {"rejectUrl", GlobalLogics.getShortUrl().generalShortUrl(rejectUrl)}
//            });
//
//            if (StringUtils.isNotBlank(message)) {
//                template = getBundleString(ua, "platformservlet.email.invite.postscript");
//                emailContent += SQLTemplate.merge(template, new Object[][]{
//                        {"message", message}
//                });
//            }
//
//            template = getBundleString(ua, "platform.group.email.invite.subject");
//            String subject = SQLTemplate.merge(template, new Object[][]{
//                    {"fromName", viewerName},
//                    {"groupType", groupType},
//                    {"groupName", groupName}
//            });
//
//            L.debug(ctx, "acceptUrl: " + acceptUrl);
//            L.debug(ctx, "rejectUrl: " + rejectUrl);
//
//            GlobalLogics.getEmail().sendEmail(ctx,subject, email, userName, emailContent, EMAIL_ESSENTIAL, lang);
            circleEmailInvite(ctx, simpleGroup, adminIds, members, userName, viewerName, register, groupType, acceptUrl, rejectUrl, email, message);
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }

    private void groupSmsInvite(Context ctx, long groupId, String groupName, String userId, String userName, String viewerId,
                                String viewerName, String phone, String message) {
        final String METHOD = "groupSmsInvite";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, groupName, userId, userName, viewerId, viewerName, phone, message);

        String ua = ctx.getUa();
        String lang = parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        boolean isEn = StringUtils.equals(lang, "en");
        String groupType = getGroupTypeStr(ctx, ctx.getLanguage(), groupId);

        String acceptUrl = "";
        String rejectUrl = "";
        String register = "";
        if (StringUtils.equals(userId, "0")) {
            String info = FeedbackParams.toSegmentedBase64(true, "/", phone, userName, viewerId);
            acceptUrl = "http://" + SERVER_HOST + "/account/invite?info=" + info + "&group_id=" + groupId;
            rejectUrl = "http://" + SERVER_HOST + "/v2/group/deal_invite?name=" + userName + "&identify=" + phone
                    + "&group_id=" + groupId + "&source=" + viewerId;
            register = isEn ? "active borqs account and" : "激活播思账号并";
        } else {
            String s = "http://" + SERVER_HOST + "/v2/group/deal_invite?user_id=" + userId + "&source=" + viewerId + "&group_id=" + groupId + "&accept=";
            acceptUrl = s + true;
            rejectUrl = s + false;
        }

        String smsContent = viewerName + "邀请您加入" + groupType + "，请点击：";

        try {
            userName = URLEncoder.encode(userName, "utf-8");
            viewerName = URLEncoder.encode(viewerName, "utf-8");
            register = URLEncoder.encode(register, "utf-8");
            groupType = URLEncoder.encode(groupType, "utf-8");
            groupName = URLEncoder.encode(groupName, "utf-8");
            message = URLEncoder.encode(message, "utf-8");
            acceptUrl = URLEncoder.encode(acceptUrl, "utf-8");
            rejectUrl = URLEncoder.encode(rejectUrl, "utf-8");
        } catch (UnsupportedEncodingException e) {

        }

        String pageUrl = "http://" + SERVER_HOST + "/v2/group/invite_page?display_name=" + userName + "&from_name=" + viewerName + "&register="
                + register + "&group_type=" + groupType + "&group_name=" + groupName + "&message=" + message + "&accept_url=" + acceptUrl + "&reject_url=" + rejectUrl;

        String shortUrl = GlobalLogics.getShortUrl().generalShortUrl(pageUrl);
        smsContent += shortUrl + "\\";

        L.debug(ctx, "sms content: " + smsContent);
        Commons.sendSms(ctx,phone, smsContent);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }

    @Override
    public String getCreatorAndAdmins(Context ctx, long groupId) {
        long creator = getCreator(ctx, groupId);
        String admins = getAdmins(ctx, groupId, -1, -1);
        String r = StringUtils.isBlank(admins) ? String.valueOf(creator) : creator + "," + admins;

        L.debug(ctx, "creator: " + creator);
        L.debug(ctx, "admins: " + admins);
        L.debug(ctx, "result: " + r);
        return r;
    }

    @Override
    public boolean getUserAndGroup(Context ctx, StringBuilder retMentions, String mentions, List<String> groupIds) {
        L.debug(ctx, "mentions: " + mentions);
        boolean privacy = false;
        String tmp = "";
        if (mentions != null && mentions.length() > 0) {
            List<String> l0 = StringUtils2.splitList(mentions, ",", true);
            if (l0.contains("#-2")) {
                l0.remove("#-2");
                tmp = StringUtils.join(l0, ",");
            } else {
                privacy = true;
            }
            //group
            List<String> groupIds0 = getGroupIdsFromMentions(ctx, l0);
            String sGroupIds = StringUtils2.joinIgnoreBlank(",", groupIds0);
            RecordSet groupRecs = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, GROUP_ID_END,
                    sGroupIds, GRP_COL_CAN_MEMBER_POST);
            for (Record groupRec : groupRecs) {
                long groupId = groupRec.checkGetInt("id");
                l0.remove("#" + groupId);
                l0.remove(String.valueOf(groupId));
                long canMemberPost = groupRec.getInt(GRP_COL_CAN_MEMBER_POST, 1);
                if ((canMemberPost == 1 && hasRight(ctx, groupId, ctx.getViewerId(), ROLE_MEMBER))
                        || (canMemberPost == 0 && hasRight(ctx, groupId, ctx.getViewerId(), ROLE_ADMIN))
                        || canMemberPost == 2) {
                    l0.add(String.valueOf(groupId));
                    groupIds.add(String.valueOf(groupId));
                }
            }
            PageLogicUtils.removeIllegalPageIds(ctx, l0);
            tmp = StringUtils.join(l0, ",");

            retMentions.append(tmp);
            L.debug(ctx, "retMentions: " + retMentions.toString());
            return privacy;
        }

        return privacy;
    }

    @Override
    public List<String> getGroupIdsFromMentions(Context ctx, List<String> mentions) {
        ArrayList<String> groupIds = new ArrayList<String>();

        for (String mention : mentions) {
            if (StringUtils.startsWith(mention, "#"))
                mention = StringUtils.substringAfter(mention, "#");

            long id = 0;
            try {
                id = Long.parseLong(mention);
            } catch (NumberFormatException nfe) {
                continue;
            }
            if (isGroup(ctx, id))
                groupIds.add(String.valueOf(id));
        }

        return groupIds;
    }

    @Override
    public Record approveMember(Context ctx, long groupId, String userId) {
        final String METHOD = "approveMember";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userId);

        String viewerId = ctx.getViewerIdString();
        int status = STATUS_NONE;
        status = getUserStatusById(ctx, groupId, Long.parseLong(userId));
        L.trace(ctx, "[Method approveMember] status: " + status);

        int changedStatus = status;
        if (status == STATUS_APPLIED) {
            Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
            int minRole = ROLE_ADMIN;
            long canMemberApprove = rec.getInt(GRP_COL_CAN_MEMBER_APPROVE, 1);
            if (canMemberApprove == 1)
                minRole = ROLE_MEMBER;
            if (hasRight(ctx, groupId, Long.parseLong(viewerId), minRole)) {
                boolean b = addSimpleMembers(ctx, groupId, Record.of(userId, ROLE_MEMBER));
                if (b) {
                    String name = GlobalLogics.getAccount().getUser(ctx, userId, userId, "display_name").getString("display_name");
                    String groupName = rec.getString(GRP_COL_NAME);
                    Record dataRec = new Record();
                    dataRec.put("user_id", userId);
                    dataRec.put("user_name", name);
                    dataRec.put("group_id", groupId);
                    dataRec.put("group_name", groupName);
                    String data = dataRec.toString(false, false);
                    GlobalLogics.getRequest().dealRelatedRequestsP(ctx, "0", userId, data);

                    changedStatus = STATUS_JOINED;

                    // send notification
                    String groupType = getGroupTypeStr(ctx, ctx.getLanguage(), groupId);
                    Commons.sendNotification(ctx, Constants.NTF_GROUP_JOIN,
                            Commons.createArrayNodeFromStrings(ctx.getAppId()),
                            Commons.createArrayNodeFromStrings(userId),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId), groupType, groupName),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId), groupType, groupName),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId)),
                            Commons.createArrayNodeFromStrings(String.valueOf(groupId))
                    );
                }
            }
        }

        Record r = GlobalLogics.getAccount().getUser(ctx, userId, userId, "user_id, display_name,  image_url,perhaps_name");
        r.put("status", changedStatus);
        r.put("source", JsonNodeFactory.instance.objectNode());

        Record statusRec = new Record();
        statusRec.put("user_id", Long.parseLong(userId));
        statusRec.put("display_name", r.getString("display_name"));
        statusRec.put("identify", "");
        statusRec.put("source", 0);
        statusRec.put("status", changedStatus);
        addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));
        L.trace(ctx, "[Method approveMember] changedStatus: " + changedStatus);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return r;
    }

    @Override
    public Record ignoreMember(Context ctx, long groupId, String userId) {
        final String METHOD = "ignoreMember";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userId);

        String viewerId = ctx.getViewerIdString();
        int status = STATUS_NONE;
        status = getUserStatusById(ctx, groupId, Long.parseLong(userId));
        L.trace(ctx, "[Method ignoreMember] status: " + status);

        int changedStatus = status;
        if (status == STATUS_APPLIED) {
            Record rec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
            int minRole = ROLE_ADMIN;
            long canMemberApprove = rec.getInt(GRP_COL_CAN_MEMBER_APPROVE, 1);
            if (canMemberApprove == 1)
                minRole = ROLE_MEMBER;
            if (hasRight(ctx, groupId, Long.parseLong(viewerId), minRole)) {
                String name = GlobalLogics.getAccount().getUser(ctx, userId, userId, "display_name").getString("display_name");
                String groupName = rec.getString(GRP_COL_NAME);
                Record dataRec = new Record();
                dataRec.put("user_id", userId);
                dataRec.put("user_name", name);
                dataRec.put("group_id", groupId);
                dataRec.put("group_name", groupName);
                String data = dataRec.toString(false, false);
                GlobalLogics.getRequest().dealRelatedRequestsP(ctx, "0", userId, data);

                changedStatus = STATUS_REJECTED;
            }
        }

        Record r = GlobalLogics.getAccount().getUser(ctx, userId, userId, "user_id, display_name,  image_url,perhaps_name");
        r.put("status", changedStatus);
        r.put("source", JsonNodeFactory.instance.objectNode());

        Record statusRec = new Record();
        statusRec.put("user_id", Long.parseLong(userId));
        statusRec.put("display_name", r.getString("display_name"));
        statusRec.put("identify", "");
        statusRec.put("source", 0);
        statusRec.put("status", changedStatus);
        addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));
        L.trace(ctx, "[Method ignoreMember] changedStatus: " + changedStatus);

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return r;
    }

    @Override
    public int dealGroupInvite(Context ctx, long groupId, String userId, String source, boolean accept) {
        final String METHOD = "dealGroupInvite";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, groupId, userId, source, accept);

        FriendshipLogic friend = GlobalLogics.getFriendship();
        if (accept) {
            // add source as friend
//            friend.setFriends(ctx, userId, source, String.valueOf(ACQUAINTANCE_CIRCLE), FRIEND_REASON_INVITE, true);
//            friend.setFriends(ctx, source, userId, String.valueOf(ACQUAINTANCE_CIRCLE), FRIEND_REASON_INVITE, true);
//            friend.setFriends(ctx, userId, source, String.valueOf(ADDRESS_BOOK_CIRCLE), FRIEND_REASON_INVITE, true);
//            friend.setFriends(ctx, source, userId, String.valueOf(ADDRESS_BOOK_CIRCLE), FRIEND_REASON_INVITE, true);
            L.op(ctx, "accept case");
            int status = addMember(ctx, groupId, userId, "", true);
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return status;
        } else {
            L.op(ctx, "reject case");
            Record statusRec = new Record();
            statusRec.put("user_id", Long.parseLong(userId));
            statusRec.put("display_name", GlobalLogics.getAccount().getUser(ctx, userId, userId, "display_name").getString("display_name"));
            statusRec.put("identify", "");
            statusRec.put("source", source);
            statusRec.put("status", STATUS_REJECTED);

            addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));

            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
            return STATUS_REJECTED;
        }
    }

    @Override
    public int rejectGroupInviteForIdentify(Context ctx, long groupId, String name, String identify, String source) {
        Record statusRec = new Record();
        statusRec.put("user_id", 0);
        statusRec.put("display_name", name);
        statusRec.put("identify", identify);
        statusRec.put("source", source);
        statusRec.put("status", STATUS_REJECTED);

        addOrUpdatePendings(ctx, groupId, RecordSet.of(statusRec));
        return STATUS_REJECTED;
    }

    @Override
    public RecordSet searchGroups(Context ctx, long begin, long end, String name, String userId, int page, int count, String cols) {
        final String METHOD = "searchGroups";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, begin, end, name, userId, cols);

        RecordSet recs = findGroupsByName(ctx, begin, end, name, page, count, cols);
//        RecordSet copy = recs.copy();
//        for (Record rec : copy) {
//            long canSearch = rec.getInt(GRP_COL_CAN_SEARCH, 1);
//            if (canSearch != 1) {
//                recs.remove(rec);
//            }
//        }

        recs = additionalGroupsInfo(ctx, userId, recs);
        for (Record rec : recs) {
            Record creator = GlobalLogics.getAccount().getUser(ctx, userId, String.valueOf(rec.getInt("creator", 0)), "user_id, display_name,  image_url,perhaps_name");
            rec.put("creator", creator.toJsonNode());
            rec.remove("admins");
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    public RecordSet dealWithGroupFile(Context ctx, Record viewerFileRec, List<String> groupIds)  {
        final String METHOD = "dealWithGroupFile";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerFileRec, groupIds);

        RecordSet recs = new RecordSet();
        String viewerId = viewerFileRec.getString("user_id");
        recs.add(viewerFileRec);

        RecordSet groups = getGroups(ctx,0, 0, viewerId, StringUtils2.joinIgnoreBlank(",", groupIds), Constants.GROUP_LIGHT_COLS, false);
        for (Record group : groups) {
            String groupId = group.getString("id", "0");
            String groupName = group.getString("name", "Default");
            Record rec = viewerFileRec.copy();
            rec.put("user_id", groupId);
            rec.put("folder_id", GlobalLogics.getFile().getFolder(ctx,groupId, Constants.FOLDER_TYPE_GROUP, groupName));
            recs.add(rec);

            Record viewerGroupPhoto = viewerFileRec.copy();
            viewerGroupPhoto.put("folder_id", GlobalLogics.getFile().getFolder(ctx,viewerId, Constants.FOLDER_TYPE_TO_GROUP, groupName));
            recs.add(viewerGroupPhoto);
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public long getBeginByGroupType(Context ctx, String type) {
        Configuration conf = GlobalConfig.get();
        return conf.getInt("group." + type + ".begin", GENERAL_GROUP_ID_BEGIN);
    }

    @Override
    public long getEndByGroupType(Context ctx, String type) {
        Configuration conf = GlobalConfig.get();
        return conf.getInt("group." + type + ".end", EVENT_ID_BEGIN);
    }

    private String formatImageTag(List<String> urls) {
        String html = "";
        for (String url : urls) {
            html += "<a href=\"" + url + "\"><img src=\"" + url + "\" alt=\"Large image\" style='margin-right: 2px;width:390px;border:0px #808080 solid;text-align:center;'></a>";
        }
        return html;
    }

    @Override
    public void sendGroupEmail(Context ctx, long groupId, RecordSet attachments, GroupNotificationSender sender,
                               String senderId, Object[] scopeArgs, String bodyArg, String... titleArgs) {
        final String METHOD = "sendGroupEmail";
        if (L.isTraceEnabled()) {
            L.traceStartCall(ctx, METHOD, groupId, sender, senderId, scopeArgs, bodyArg, titleArgs);
        }

        Record groupRec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
        String groupName = groupRec.getString(GRP_COL_NAME);

        String urlPattern = GlobalConfig.get().getString("platform.profileImagePattern", "");
        String imageUrl = groupRec.getString(COMM_COL_LARGE_IMG_URL, "");
        if (StringUtils.isBlank(imageUrl)) {
            imageUrl = "default_public_circle_L.png";
            urlPattern = GlobalConfig.get().getString("platform.sysIconUrlPattern", "");
        }
        groupRec.put(COMM_COL_LARGE_IMG_URL, imageUrl);
        addImageUrlPrefix(urlPattern, groupRec);
        imageUrl = groupRec.getString(COMM_COL_LARGE_IMG_URL);

        String description = groupRec.getString(COMM_COL_DESCRIPTION);
        Record senderRec = GlobalLogics.getAccount().getUsersBaseColumns(ctx, senderId).getFirstRecord();
        String senderName =  senderRec.getString("display_name");
//        String imageUrl = senderRec.getString("large_image_url");


        List<Long> scope = sender.getScope(ctx, senderId, scopeArgs);
        String title = sender.getTitle(ctx, "zh", titleArgs);
        String body = sender.getBody(ctx, bodyArg);
        String message = title;
        String emailContent = title;
        if (StringUtils.isNotBlank(body)) {
            if (sender instanceof SharedNotifSender) {
//                String who = StringUtils.substringBefore(title, "在");
//                String groupName = StringUtils.substringBetween(title, "【", "】");
                message = body + " 来自" + senderName + "[" + groupName + "]";
            }
            else
                message += ":" + body;

            if (!(sender instanceof PhotoSharedNotifSender))
                emailContent += ":<br>" + body;
        }

        String who = StringUtils.substringBefore(title, "在");
        emailContent = StringUtils.replace(emailContent, who, Commons.formatEmailUserName(ctx, senderId));
        L.debug(ctx, "email content: " + emailContent);
        L.debug(ctx, "sms content: " + message);


        String groupType = isEvent(ctx, groupId) ? "事件" : "公共圈子";
        String userIds = StringUtils2.joinIgnoreBlank(",", scope);
        RecordSet users = GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), userIds,
                "user_id, display_name, login_email1, login_email2, login_email3", false);
        for (Record user : users) {
            String displayName = user.getString("display_name");
            String[] emails = new String[3];
            emails[0] = user.getString("login_email1", "");
            emails[1] = user.getString("login_email2", "");
            emails[2] = user.getString("login_email3", "");
            for (int i = 0; i < 3; i++) {
                if (StringUtils.isNotBlank(emails[i])) {
                    String subject = "来自" + groupType + "【" + groupName + "】的新消息";
                    if (sender instanceof PhotoSharedNotifSender) {
                        LinkedHashMap<String, Object> emailParams = new LinkedHashMap<String, Object>();
                        emailParams.put("icon", imageUrl);
                        emailParams.put("displayName", displayName);
                        emailParams.put("content", emailContent);
                        emailParams.put("subscribe", "");

//                        emailParams.put("postId", scopeArgs[0]);
                        String sUrls = attachments.joinColumnValues("photo_img_big", ",");
                        List<String> urls = StringUtils2.splitList(sUrls, ",", true);
                        emailParams.put("photos", sUrls);
                        emailParams.put("description", body);
                        emailParams.put("group_id",groupId);

                        MessageDelayCombineUtils.combineEventPhotos(ctx,subject,emailParams,displayName,emails[i]);

                        //GlobalLogics.getEmail().sendCustomEmailP(ctx, subject, emails[i], displayName, "share_photo.ftl", emailParams, Constants.EMAIL_ESSENTIAL, ctx.getLanguage());
                    } else {
                        GlobalLogics.getEmail().sendEmail(ctx, subject, emails[i], displayName, emailContent, Constants.EMAIL_ESSENTIAL, "zh");
                    }
                }
            }
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }

    @Override
    public void sendGroupSms(Context ctx, long groupId, RecordSet attachments, GroupNotificationSender sender,
                               String senderId, Object[] scopeArgs, String bodyArg, String... titleArgs) {
        final String METHOD = "sendGroupSms";
        if (L.isTraceEnabled()) {
            L.traceStartCall(ctx, METHOD, groupId, sender, senderId, scopeArgs, bodyArg, titleArgs);
        }

        List<Long> scope = sender.getScope(ctx, senderId, scopeArgs);
        String title = sender.getTitle(ctx, "zh", titleArgs);
        String body = sender.getBody(ctx, bodyArg);
        String message = title;
        String emailContent = title;
        if (StringUtils.isNotBlank(body)) {
            if (sender instanceof SharedNotifSender) {
                String who = StringUtils.substringBefore(title, "在");
                String groupName = StringUtils.substringBetween(title, "【", "】");
                message = body + " 来自" + who + "[" + groupName + "]";
            }
            else
                message += ":" + body;
            emailContent += ":<br>" + body;
        }

        L.debug(ctx, "email content: " + emailContent);
        L.debug(ctx, "sms content: " + message);

        String userIds = StringUtils2.joinIgnoreBlank(",", scope);
        RecordSet users = GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), userIds,
                "user_id, display_name, login_phone1, login_phone2, login_phone3", false);
        for (Record user : users) {
            String displayName = user.getString("display_name");
            String[] phones = new String[3];
            phones[0] = user.getString("login_phone1", "");
            phones[1] = user.getString("login_phone2", "");
            phones[2] = user.getString("login_phone3", "");
            for (int i = 0; i < 3; i++) {
                if (StringUtils.isNotBlank(phones[i]))
                    Commons.sendSms(ctx, phones[i], message);
            }
        }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }

    @Override
    public void sendGroupNotification(Context ctx, long groupId, RecordSet attachments, GroupNotificationSender sender,
                                      String senderId, Object[] scopeArgs, String bodyArg, String... titleArgs) {
        final String METHOD = "sendGroupNotification";
        if (L.isTraceEnabled()) {
            L.traceStartCall(ctx, METHOD, groupId, sender, senderId, scopeArgs, bodyArg, titleArgs);
        }

        Record groupRec = getSimpleGroups(ctx, 0, 0, String.valueOf(groupId), GROUP_LIGHT_COLS).getFirstRecord();
        String groupName = groupRec.getString(GRP_COL_NAME);

        String urlPattern = GlobalConfig.get().getString("platform.profileImagePattern", "");
        String imageUrl = groupRec.getString(COMM_COL_LARGE_IMG_URL, "");
        if (StringUtils.isBlank(imageUrl)) {
            imageUrl = "default_public_circle_L.png";
            urlPattern = GlobalConfig.get().getString("platform.sysIconUrlPattern", "");
        }
        groupRec.put(COMM_COL_LARGE_IMG_URL, imageUrl);
        addImageUrlPrefix(urlPattern, groupRec);
        imageUrl = groupRec.getString(COMM_COL_LARGE_IMG_URL);

        String description = groupRec.getString(COMM_COL_DESCRIPTION);
        Record senderRec = GlobalLogics.getAccount().getUsersBaseColumns(ctx, senderId).getFirstRecord();
        String senderName =  senderRec.getString("display_name");
//        String imageUrl = senderRec.getString("large_image_url");

        RecordSet recs = new RecordSet();
                List<Long> scope = sender.getScope(ctx, senderId, recs, scopeArgs);
            String title = sender.getTitle(ctx, "zh", titleArgs);
            String body = sender.getBody(ctx, bodyArg);
            String message = title;
            String emailContent = title;
            if (StringUtils.isNotBlank(body)) {
                if (sender instanceof SharedNotifSender) {
//                    String who = StringUtils.substringBefore(title, "在");
//                    String groupName = StringUtils.substringBetween(title, "【", "】");
                    message = body + " 来自" + senderName + "[" + groupName + "]";
                }
                else
                    message += ":" + body;

                if (!(sender instanceof PhotoSharedNotifSender))
                    emailContent += ":<br>" + body;
            }

        String who = StringUtils.substringBefore(title, "在");
        emailContent = StringUtils.replace(emailContent, who, Commons.formatEmailUserName(ctx, senderId));
        L.debug(ctx, "email content: " + emailContent);
        L.debug(ctx, "sms content: " + message);

        String groupType = isEvent(ctx, groupId) ? "事件" : "公共圈子";
        String userIds = StringUtils2.joinIgnoreBlank(",", scope);
        RecordSet users = GlobalLogics.getAccount().getUsersBaseColumns(ctx, userIds);
        Record map = new Record();
        for (Record user : users) {
            map.put(user.getString("user_id"), user.getString("display_name"));
        }

//        RecordSet recs = getMembersNotification(ctx, groupId, userIds);
            for (Record rec : recs) {
                long recGroupId = rec.getInt("group_id");
                if (recGroupId == groupId) {
                    String userId = rec.getString("member");
                    String displayName = map.getString(userId);
                    long recvNotif = rec.getInt("recv_notif", 0);
                    String notifEmail = rec.getString("notif_email", "");
                    String notifPhone = rec.getString("notif_phone", "");
                    if (recvNotif == 1) {
                        if (StringUtils.isNotBlank(notifEmail)) {
                            String subject = "来自" + groupType + "【" + groupName + "】的新消息";
                            if (sender instanceof PhotoSharedNotifSender) {
                                LinkedHashMap<String, Object> emailParams = new LinkedHashMap<String, Object>();
                                emailParams.put("icon", imageUrl);
                                emailParams.put("displayName", displayName);
                                emailParams.put("content", emailContent);
                                emailParams.put("subscribe", "");
//                            emailParams.put("postId", scopeArgs[0]);
                                String sUrls = attachments.joinColumnValues("photo_img_big", ",");
                                List<String> urls = StringUtils2.splitList(sUrls, ",", true);
                                emailParams.put("photos", formatImageTag(urls));
                                emailParams.put("description", body);
                                GlobalLogics.getEmail().sendCustomEmailP(ctx, subject, notifEmail, displayName, "share_photo.ftl", emailParams, Constants.EMAIL_ESSENTIAL, ctx.getLanguage());
                            } else {
                                GlobalLogics.getEmail().sendEmail(ctx, subject, notifEmail, displayName, emailContent, Constants.EMAIL_ESSENTIAL, "zh");
                            }
                        }
                        if (StringUtils.isNotBlank(notifPhone))
                            Commons.sendSms(ctx, notifPhone, message);
                    }
                }
            }

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
    }


    @Override
    public String findGroupIdsByProperty(Context ctx, String propKey, String propVal, int max) {
        final String METHOD = "findGroupIdsByProperty";
        if (L.isTraceEnabled()) {
            L.traceStartCall(ctx, METHOD, propKey, propVal, max);
        }

        String r = groupDb.findGroupIdsByProperty(propKey, propVal, max);
        if (L.isTraceEnabled()) {
            L.traceEndCall(ctx, METHOD);
        }
        return r;
    }

    public static String makeGroupSortKey(String name) {
        StringBuilder buff = new StringBuilder();
        buff.append(ObjectUtils.toString(name)).append(" ");
        buff.append(PinyinUtils.toFullPinyin(name, " ")).append(" ");
        buff.append(PinyinUtils.toShortPinyin(name));

        return buff.toString();
    }

    @Override
    public String getPageEvents(Context ctx, long pageId) {
        return groupDb.getPageEvents(pageId);
    }

    @Override
    public GroupDb getDB() {
        return groupDb;
    }

    @Override
    public int getRole(Context ctx, long groupId, long userId) {
        throw new NotImplementedException();
    }

    @Override
    public Map<Long, Integer> getRolesWithUsers(Context ctx, long groupId, long[] userIds) {
        throw new NotImplementedException();
    }

    @Override
    public Map<Long, Integer> getRolesWithGroups(Context ctx, long[] groupIds, long userId) {
        final String METHOD = "findGroupIdsByProperty";
        L.traceStartCall(ctx, METHOD, groupIds, userId);
        Map<Long, Integer> m = groupDb.getRolesWithGroups(ctx, groupIds, userId);
        L.traceEndCall(ctx, METHOD);
        return m;
    }

    private static String getCellText(Cell c) {
        return ObjectUtils.toString(c != null ? c.getContents() : "");
    }

    private static RecordSet loadExcelFile(FileItem fi) {
        //"name": "员工名称",
        //"employee_id":"工号"
        //"email":"员工email",
        //"tel":"员工电话",
        //"mobile_tel":"员工手机",
        //"department":"员工所属部门",
        //"job_title":"员工头衔",

        final int NAME_INDEX = 1;
        final int EMPLOYEE_ID_INDEX = 2;
        final int EMAIL_INDEX = 3;
        final int TEL_INDEX = 4;
        final int MOBILE_TEL_INDEX = 5;
        final int DEPARTMENT_INDEX = 6;
        final int JOB_TITLE_INDEX = 7;
        final int NAME_EN_INDEX = 8;


        RecordSet recs = new RecordSet();
        try {
            Workbook wb = Workbook.getWorkbook(fi.getInputStream());
            Sheet sheet = wb.getSheet(0);
            int rows = sheet.getRows();

            for (int i = 1; i < rows; i++) { // 从1开始，忽略title行
                Cell[] cells = sheet.getRow(i);
                if(cells.length == 0)
                    continue;
                Cell c;
                Record rec = new Record();
                rec.put("name", cells.length > NAME_INDEX ? getCellText(cells[NAME_INDEX]) : "");
                rec.put("employee_id", cells.length > EMPLOYEE_ID_INDEX ? getCellText(cells[EMPLOYEE_ID_INDEX]) : "");
                rec.put("email", cells.length > EMAIL_INDEX ? getCellText(cells[EMAIL_INDEX]) : "");
                rec.put("tel", cells.length > TEL_INDEX ? getCellText(cells[TEL_INDEX]) : "");
                rec.put("mobile_tel", cells.length > MOBILE_TEL_INDEX ? getCellText(cells[MOBILE_TEL_INDEX]) : "");
                rec.put("department", cells.length > DEPARTMENT_INDEX ? getCellText(cells[DEPARTMENT_INDEX]) : "");
                rec.put("job_title", cells.length > JOB_TITLE_INDEX ? getCellText(cells[JOB_TITLE_INDEX]) : "");
                rec.put("name_en", cells.length > NAME_EN_INDEX ? getCellText(cells[NAME_EN_INDEX]) : "");

                if(StringUtils.isEmpty(rec.getString("name")) || StringUtils.isEmpty(rec.getString("employee_id")) || StringUtils.isEmpty(rec.getString("email")))
                    continue;

                recs.add(rec);
            }
        } catch (Exception e) {
            recs.clear();
        } finally {
        }

        return recs;
    }

    @Override
    public RecordSet uploadEmployees(Context ctx, long circleId, FileItem excelFile, boolean merge) {
        long viewerId = ctx.getViewerId();

        if(!hasRight(ctx, circleId, viewerId, Constants.ROLE_ADMIN))
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR);

        Record groupRec = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "formal").getFirstRecord();
        if (groupRec.getInt("formal", PUBLIC_CIRCLE_FORMAL_FREE) != PUBLIC_CIRCLE_FORMAL_TOP)
            throw new ServerException(WutongErrors.GROUP_NOT_FORMAL);

        //find all members before upload employee list
        String membersOld = getMembers(ctx,String.valueOf(circleId),0,10000);
        //不删的id
        List<String> surviveIds = new ArrayList<String>();
        //如果是非merger 那么清除掉所有的groupMembers，保存当前用户
        if(!merge){

            List<String> idList = getSurviveMembers(ctx, circleId, surviveIds);
            deleteMembers(ctx, circleId, StringUtils2.joinIgnoreBlank(",", idList));
        }

        RecordSet l = loadExcelFile(excelFile);
        RecordSet emails = new RecordSet();
        for (Record rec : l) {
            String name = rec.getString("name");
            if (StringUtils.isBlank(name))
                name = rec.getString("name_en");
            emails.add(Record.of("email", rec.getString("email"), "name", name));
        }
        emails = GlobalLogics.getSocialContacts().findBorqsIdFromContactInfo(ctx, emails);
        Record roles = new Record();
        Record map = new Record();
        for (Record rec : emails) {
            String email = rec.getString("email");
            String name = rec.getString("name");
            long userId = rec.getInt("user_id", 0L);

            if (userId == 0L) {
//                inviteMember(ctx, circleId, email, name, "", false, false);
                String password = RandomUtils.generateRandomNumberString(6);
                String md5Pwd = Encoders.md5Hex(password);
                Record user = new Record()
                        .set("display_name", name)
                        .set("password", md5Pwd)
                        .set("login_email1", email);
                userId = Long.parseLong(GlobalLogics.getAccount().createAccount(ctx, user));

                String subject = "您的播思账号已创建";
                String emailContent = "尊敬的" + name + "，<br>\t\t您的播思账号已创建，请您尽快用下面的登录名和密码登录，修改您的密码。";
                emailContent += "\t\t登录名： " + email + "<br>";
                emailContent += "\t\t密  码： " + password;

                ctx.setViewerId(userId);
                GlobalLogics.getEmail().sendEmail(ctx, subject, email, name, emailContent, EMAIL_ESSENTIAL, "zh");
            }

            map.put(email, userId);
            roles.put(String.valueOf(userId), ROLE_MEMBER);
        }
        Set<String> admins = StringUtils2.splitSet(getCreatorAndAdmins(ctx, circleId), ",", true);
        for (String admin : admins) {
            roles.remove(admin);
        }
        if (addSimpleMembers(ctx, circleId, roles)) {
            RecordSet statusRecs = new RecordSet();
            for (Record rec : emails) {
                long userId = rec.getInt("user_id", 0L);
                if (userId != 0L) {
                    Record statusRec = new Record();
                    statusRec.put("user_id", userId);
                    statusRec.put("display_name", rec.getString("name"));
                    statusRec.put("identify", rec.getString("email"));
                    statusRec.put("source", viewerId);
                    statusRec.put("status", STATUS_JOINED);
                    statusRecs.add(statusRec);
                }
            }
            addOrUpdatePendings(ctx, circleId, statusRecs);
        }

        for (Record rec : l) {
            String email = rec.getString("email");
            rec.put("user_id", map.getInt(email));
            rec.put("sk", makeEmployeeSK(
                    ObjectUtils.toString(rec.getString("name", "")),
                    ObjectUtils.toString(email),
                    rec.getString("tel", ""),
                    rec.getString("mobile_tel", ""),
                    rec.getString("department", "")));
        }

        if(!merge){
            String membersNew = getMembers(ctx,String.valueOf(circleId),0,10000);

            List<String> deleteMembers  = new ArrayList<String>();
            for(String str:membersOld.split(",")){
                if(!StringUtils.contains(membersNew,str)){
                    deleteMembers.add(str);
                }
            }
            //delete top circle
            for (String member : deleteMembers) {
                deleteSingleCircleMembers(ctx, member, circleId);
            }
            //delete sub circles
            deleteSubCircleMembers(ctx,circleId,StringUtils.join(deleteMembers,","));

            /*RecordSet rs = groupDb.findAllSubCircleEventIds(ctx,circleId);
            for(Record r :rs)
                deleteMembers(ctx, r.getInt("group_id"), StringUtils2.joinIgnoreBlank(",", deleteMembers));*/
        }

        groupDb.addEmployeeInfos(ctx, circleId, l, merge,surviveIds);
        return getGroupUsersByStatus(ctx, circleId, String.valueOf(Constants.STATUS_JOINED), -1, -1, "");
    }

    /**
     * 删除当前圈子的members，创建者不删除，当前人员不删除
     * @param ctx
     * @param circleId
     * @param surviveIds
     */
    private List<String> getSurviveMembers(Context ctx, long circleId, List<String> surviveIds) {
        String ids = getAllMembers(ctx,circleId,0,10000,null);
        surviveIds.add(StringUtils.substringBefore(ids, ","));

/*        List l = new ArrayList<String>();
        ids = StringUtils.substringBeforeLast(ids,",");*/
        List<String> idList= new ArrayList<String>();
        if(ids.length()>0){
            for(String id : ids.split(",")){
                if(!StringUtils.equals(id,ctx.getViewerIdString()))
                    idList.add(id);
                else
                    surviveIds.add(id);
            }
        }
        return idList;

    }

    @Override
    public int addEmployee(Context ctx, long circleId, Record rec) {
        long viewerId = ctx.getViewerId();

        if(!hasRight(ctx, circleId, viewerId, Constants.ROLE_ADMIN))
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR);

        Record groupRec = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "formal").getFirstRecord();
        if (groupRec.getInt("formal", PUBLIC_CIRCLE_FORMAL_FREE) != PUBLIC_CIRCLE_FORMAL_TOP)
            throw new ServerException(WutongErrors.GROUP_NOT_FORMAL);

        RecordSet emails = new RecordSet();
        String name = rec.getString("name");
        if (StringUtils.isBlank(name))
            name = rec.getString("name_en");
        emails.add(Record.of("email", rec.getString("email"), "name", name));

        Record emailRec = GlobalLogics.getSocialContacts().findBorqsIdFromContactInfo(ctx, emails).getFirstRecord();
        String email = emailRec.getString("email");
        long userId = emailRec.getInt("user_id", 0L);

        if (userId == 0L) {
//            inviteMember(ctx, circleId, email, name, "", false, false);
            String password = RandomUtils.generateRandomNumberString(6);
            String md5Pwd = Encoders.md5Hex(password);
            Record user = new Record()
                    .set("display_name", name)
                    .set("password", md5Pwd)
                    .set("login_email1", email);
            userId = Long.parseLong(GlobalLogics.getAccount().createAccount(ctx, user));

            String subject = "您的播思账号已创建";
            String emailContent = "尊敬的" + name + "，<br>\t\t您的播思账号已创建，请您尽快用下面的登录名和密码登录，修改您的密码。";
            emailContent += "\t\t登录名： " + email + "<br>";
            emailContent += "\t\t密  码： " + password;

            GlobalLogics.getEmail().sendEmail(ctx, subject, email, name, emailContent, EMAIL_ESSENTIAL, "zh");
        }

        if (!hasRight(ctx, circleId, userId, ROLE_MEMBER)) {
            if (addSimpleMember(ctx, circleId, userId, ROLE_MEMBER)) {
                Record statusRec = new Record();
                statusRec.put("user_id", userId);
                statusRec.put("display_name", name);
                statusRec.put("identify", email);
                statusRec.put("source", viewerId);
                statusRec.put("status", STATUS_JOINED);
                addOrUpdatePendings(ctx, circleId, RecordSet.of(statusRec));
            }
        }


        rec.put("user_id", userId);
        rec.put("sk", makeEmployeeSK(
                ObjectUtils.toString(name),
                ObjectUtils.toString(email),
                rec.getString("tel", ""),
                rec.getString("mobile_tel", ""),
                rec.getString("department", "")));

        groupDb.addEmployeeInfo(ctx, circleId, rec);
        return getUserStatusByIdentify(ctx, circleId, email);
    }

    @Override
    public boolean updateEmployee(Context ctx, long circleId, String email, Record info) {
        long viewerId = ctx.getViewerId();

        if(!hasRight(ctx, circleId, viewerId, Constants.ROLE_ADMIN))
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR);

        Record groupRec = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "formal").getFirstRecord();
        if (groupRec.getInt("formal", PUBLIC_CIRCLE_FORMAL_FREE) != PUBLIC_CIRCLE_FORMAL_TOP)
            throw new ServerException(WutongErrors.GROUP_NOT_FORMAL);

        Record rec = groupDb.getEmployeeInfo(ctx, circleId, email);
        String name = info.has("name") ? info.checkGetString("name") : rec.getString("name", "");
        String tel = info.has("tel") ? info.checkGetString("tel") : rec.getString("tel", "");
        String mobileTel = info.has("mobile_tel") ? info.checkGetString("mobile_tel") : rec.getString("mobile_tel", "");
        String department = info.has("department") ? info.checkGetString("department") : rec.getString("department", "");

        if (MapUtils.isNotEmpty(info)) {
            info.put("sk", makeEmployeeSK(
                    name,
                    email,
                    tel,
                    mobileTel,
                    department
            ));
        }

        return groupDb.updateEmployeeInfo(ctx, circleId, email, info);
    }

    @Override
    public boolean deleteEmployees(Context ctx, long circleId,String userIds, String... emails) {
        long viewerId = ctx.getViewerId();

        if(!hasRight(ctx, circleId, viewerId, Constants.ROLE_ADMIN))
            throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR);

        Record groupRec = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "formal").getFirstRecord();
        if (groupRec.getInt("formal", PUBLIC_CIRCLE_FORMAL_FREE) != PUBLIC_CIRCLE_FORMAL_TOP)
            throw new ServerException(WutongErrors.GROUP_NOT_FORMAL);

        String members = groupDb.deleteEmployeeInfos(ctx, circleId, emails);
        if (StringUtils.isNotBlank(members)) {
            boolean r = deleteMembers(ctx, circleId, members);
            deleteSubCircleMembers(ctx,circleId,members);
            return r;
        } else {
            boolean r =deleteMembers(ctx,circleId,userIds);
            deleteSubCircleMembers(ctx,circleId,userIds);
            return r;
        }
    }

    /**
     * wrapping method to delete arrays
     * @param ctx
     * @param circle
     * @param memberIds
     */
    private void deleteSubCircleMembers(Context ctx,long circle,String memberIds){
        if(StringUtils.isBlank(memberIds) || circle == 0)
            return;

        for(String member :StringUtils2.splitArray(memberIds,",",true)){
            deleteSubCircleMembersFinal(ctx,circle,member);
        }

    }
    /**
     * 删除子圈子下所有可以删除的member
     * @param ctx
     * @param circle
     * @param member
     */
    private void deleteSubCircleMembersFinal(Context ctx,long circle,String member){
        //find all sub circles
        RecordSet rs = groupDb.findAllSubCircleEventIds(ctx, circle);
        if(rs == null || rs.size()<1)
            return;

        for(Record r:rs){
            Long group_id = r.getInt("group_id");
            deleteSingleCircleMembers(ctx, member, group_id);
        }

    }

    /**
     * delete single circle members
     * @param ctx
     * @param member
     * @param group_id
     */
    private void deleteSingleCircleMembers(Context ctx, String member, Long group_id) {
        String members = groupDb.getMembersByRole0(ctx,group_id,0,0,10000,"");
        List<String> memberList = StringUtils2.splitList(members, ",", true);
        if(memberList.contains(member) && memberList.size() == 1){
            //delete member and drop cricles
            groupDb.deleteMembers(ctx,group_id,member);
            groupDb.deleteGroups(ctx,String.valueOf(group_id));

        }else if(memberList.contains(member)&& memberList.size() >1){
            // charge the member is a creator
            if(groupDb.hasRight0(ctx,group_id,Long.parseLong(member), Constants.ROLE_CREATOR)){
                //if this member is a creator then grant the role admin to the second one and delete
                String second = "";
                for(String m :memberList){
                    if(!StringUtils.equals(m, member)){
                        second = m;
                        break;
                    }
                }
                Record role = Record.of(second,Constants.ROLE_CREATOR);
                boolean b = groupDb.addOrGrantMembers(ctx, group_id, role);


            }
            //delete the member
            groupDb.deleteMembers(ctx,group_id,member);
        }
    }

    /*private void deleteSubCircleMembers(Context ctx,long circleId,String members) {
        //find all sub_circles and delete members from top and sub circles---------------
        //TODO------find all members if members =1 then delete the circle,else grant creator role to the second person

        RecordSet rs = groupDb.findAllSubCircleEventIds(ctx, circleId);
        for (Record r : rs) {
            List<String> surviveIds = getSurviveResult(ctx, circleId, members);
            deleteMembers(ctx, r.getInt("group_id"), StringUtils2.joinIgnoreBlank(",",surviveIds));
        }
    }*/

    /*private List<String> getSurviveResult(Context ctx, long circleId, String members) {
        List<String> surviveIds = new ArrayList<String>();
        getSurviveMembers(ctx,circleId,surviveIds);
        String[] memberArrays = members.split(",");
        List<String> result = new ArrayList<String>();
        for(String str : memberArrays){
            if(!surviveIds.contains(str))
                result.add(str);
        }
        return result;
    }*/

    private boolean deleteMembers(Context ctx, long circleId, String members) {
        boolean r = groupDb.deleteMembers(ctx, circleId, members);
        if (r) {
            List<String> memberIds = StringUtils2.splitList(members, ",", true);
            RecordSet recs = new RecordSet();
            for (String memberId : memberIds) {
                Record statusRec = new Record();
                statusRec.put("user_id", Long.parseLong(memberId));
                statusRec.put("display_name", GlobalLogics.getAccount().getUsers(ctx, memberId, "display_name").getFirstRecord().getString("display_name"));
                statusRec.put("identify", "");
                statusRec.put("source", 0);
                statusRec.put("status", STATUS_KICKED);
                recs.add(statusRec);
            }
            addOrUpdatePendings(ctx, circleId, recs);
        }
        return r;
    }

    private RecordSet attachEmployeeInfos(Context ctx, long circleId, RecordSet recs) {
        Record groupRec = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "formal,parent_id").getFirstRecord();
        long formal = groupRec.getInt("formal", PUBLIC_CIRCLE_FORMAL_FREE);
        long parentId = groupRec.getInt("parent_id", 0L);
        if (formal == PUBLIC_CIRCLE_FORMAL_TOP) {
            String userIds = recs.joinColumnValues("user_id", ",");
            Map<String, Record> map = groupDb.getEmployeeInfos(ctx, circleId, userIds);
            for (Record rec : recs) {
                String userId = rec.getString("user_id");
                Record info = map.get(userId);
                rec.put("name", info.getString("name", ""));
                rec.put("name_en", info.getString("name_en", ""));
                rec.put("employee_id", info.getString("employee_id", ""));
                rec.put("email", info.getString("email", ""));
                rec.put("tel", info.getString("tel", ""));
                rec.put("mobile_tel", info.getString("mobile_tel", ""));
                rec.put("department", info.getString("department", ""));
                rec.put("job_title", info.getString("job_title", ""));
            }
            return recs;
        } else if (isGroup(ctx, parentId)) {
            return attachEmployeeInfos(ctx, parentId, recs);
        } else {
            return recs;
        }
    }

    @Override
    public String getTopCircleIds(Context ctx) {
        String circleIds = findGroupIdsByMember(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, ctx.getViewerId());
        RecordSet circles = getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, circleIds, "formal,parent_id");
        ArrayList<Long> l = new ArrayList<Long>();
        for (Record circle : circles) {
            long circleId = circle.getInt("id");
            long formal = circle.getInt("formal", PUBLIC_CIRCLE_FORMAL_FREE);
            long parentId = circle.getInt("parent_id", 0L);
            if ((formal == PUBLIC_CIRCLE_FORMAL_TOP) || (parentId == 0L)) {
                l.add(circleId);
            }
        }
        return StringUtils2.joinIgnoreBlank(",", l);
    }

    public static String makeEmployeeSK(String name, String email, String tel, String mobileTel, String department) {
        StringBuilder buff = new StringBuilder();
        buff.append(ObjectUtils.toString(name)).append(" ");
        buff.append(PinyinUtils.toFullPinyin(name, " ")).append(" ");
        buff.append(PinyinUtils.toShortPinyin(name)).append(" ");
        buff.append(ObjectUtils.toString(email)).append(" ");
        buff.append(ObjectUtils.toString(tel)).append(" ");
        buff.append(ObjectUtils.toString(mobileTel)).append(" ");
        buff.append(ObjectUtils.toString(department));
        return buff.toString();
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }
}