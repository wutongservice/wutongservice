package com.borqs.server.wutong.page;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.group.GroupLogic;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.wutong.Constants.*;
import static com.borqs.server.wutong.Constants.FRIENDS_CIRCLE;
import static com.borqs.server.wutong.Constants.STATUS_INVITED;

public class PageServlet extends WebMethodServlet {

    private StaticFileStorage photoStorage;

    public PageServlet() {
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = GlobalConfig.get();
        photoStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.photoStorage", ""));
        photoStorage.init();
    }

    @Override
    public void destroy() {
        photoStorage.destroy();
    }

    private static void readPage(Record pageRec, QueryParams qp) {
        pageRec.put("email_domain1", qp.getString("email_domain1", ""));
        pageRec.put("email_domain2", qp.getString("email_domain2", ""));
        pageRec.put("email_domain3", qp.getString("email_domain3", ""));
        pageRec.put("email_domain4", qp.getString("email_domain4", ""));
        pageRec.put("type", qp.getString("type", ""));
        pageRec.put("flags", qp.getInt("flags", 0L));
        pageRec.put("address", qp.getString("address", ""));
        pageRec.put("address_en", qp.getString("address_en", ""));
        pageRec.put("email", qp.getString("email", ""));
        pageRec.put("website", qp.getString("website", ""));
        pageRec.put("tel", qp.getString("tel", ""));
        pageRec.put("fax", qp.getString("fax", ""));
        pageRec.put("zip_code", qp.getString("zip_code", ""));
        pageRec.put("description", qp.getString("description", ""));
        pageRec.put("description_en", qp.getString("description_en", ""));
    }

    @WebMethod("page/create")
    public Record createPage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        NamePair name = checkName(qp, false);
        Record pageRec = Record.of("name", name.name, "name_en", name.nameEn);
        readPage(pageRec, qp);
        pageRec.put("associated_id", 0L);
        pageRec.put("free_circle_ids", "");

        return GlobalLogics.getPage().createPage(ctx, pageRec);
    }

    @WebMethod("page/create_from")
    public Record createPageFrom(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        NamePair name = checkName(qp, false);
        long associatedId = qp.getInt("circle", 0);
        if (associatedId <= 0L)
            associatedId = ctx.getViewerId();

        Record pageRec = Record.of("name", name.name, "name_en", name.nameEn);
        readPage(pageRec, qp);

        long groupId = 0;
        if (Constants.getUserTypeById(associatedId) == Constants.PUBLIC_CIRCLE_OBJECT) {
            boolean isAdmin = GlobalLogics.getGroup().hasRight(ctx, associatedId, ctx.getViewerId(), Constants.ROLE_ADMIN);
            if (!isAdmin)
                throw new ServerException(WutongErrors.GROUP_RIGHT_ERROR, "");

            Record circleRec = GlobalLogics.getGroup().getGroup(ctx, ctx.getViewerIdString(), associatedId, Constants.GROUP_LIGHT_COLS + ", formal,page_id", false);
            if (circleRec != null)
                groupId = circleRec.getInt("id");

            if (circleRec != null && circleRec.getInt("page_id", 0L) > 0L)
                throw new ServerException(WutongErrors.PAGE_CIRCLE_ASSOCIATED, "The circle is associated with a page");

            if (circleRec != null
                    && (circleRec.getInt("formal") == Constants.PUBLIC_CIRCLE_FORMAL_TOP
                    || circleRec.getInt("formal") == Constants.PUBLIC_CIRCLE_FORMAL_SUB)) {
                pageRec.set("associated_id", associatedId);
                pageRec.set("free_circle_ids", "");
            } else {
                pageRec.set("associated_id", 0L);
                pageRec.set("free_circle_ids", Long.toString(associatedId));
            }
        } else {
            pageRec.set("associated_id", associatedId);
            pageRec.set("free_circle_ids", "");
        }

        Record resultPageRec = GlobalLogics.getPage().createPage(ctx, pageRec);
        if (groupId > 0) {
            GlobalLogics.getGroup().updateGroup(ctx, groupId, new Record(), Record.of("page_id", resultPageRec.getInt("page_id")));
        }
        return resultPageRec;
    }

    @WebMethod("page/destroy")
    public boolean destroyPage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long pageId = qp.checkGetInt("page");
        boolean withAssociated = qp.getBoolean("with_associated", false);
        GlobalLogics.getPage().destroyPage(ctx, pageId, withAssociated);
        return true;
    }

    @WebMethod("page/update")
    public Record updatePage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        Record pageRec = new Record();
        pageRec.put("page_id", qp.checkGetInt("page_id"));
        pageRec.putIf("name", qp.getString("name", ""), qp.containsKey("name"));
        pageRec.putIf("name_en", qp.getString("name_en", ""), qp.containsKey("name_en"));
        pageRec.putIf("email_domain1", qp.getString("email_domain1", ""), qp.containsKey("email_domain1"));
        pageRec.putIf("email_domain2", qp.getString("email_domain2", ""), qp.containsKey("email_domain2"));
        pageRec.putIf("email_domain3", qp.getString("email_domain3", ""), qp.containsKey("email_domain3"));
        pageRec.putIf("email_domain4", qp.getString("email_domain4", ""), qp.containsKey("email_domain4"));
        pageRec.putIf("type", qp.getString("type", ""), qp.containsKey("type"));
        pageRec.putIf("flags", qp.getInt("flags", 0L), qp.containsKey("flags"));
        pageRec.putIf("address", qp.getString("address", ""), qp.containsKey("address"));
        pageRec.putIf("address_en", qp.getString("address_en", ""), qp.containsKey("address_en"));
        pageRec.putIf("email", qp.getString("email", ""), qp.containsKey("email"));
        pageRec.putIf("website", qp.getString("website", ""), qp.containsKey("website"));
        pageRec.putIf("tel", qp.getString("tel", ""), qp.containsKey("tel"));
        pageRec.putIf("fax", qp.getString("fax", ""), qp.containsKey("fax"));
        pageRec.putIf("zip_code", qp.getString("zip_code", ""), qp.containsKey("zip_code"));
        pageRec.putIf("description", qp.getString("description", ""), qp.containsKey("description"));
        pageRec.putIf("description_en", qp.getString("description_en", ""), qp.containsKey("description_en"));
        return GlobalLogics.getPage().updatePage(ctx, pageRec, false, true);
    }

    private String[] savePageImages(long pageId, String type, FileItem fi, boolean isLogo) {
        String[] urls = new String[3];

        long uploaded_time = DateUtils.nowMillis();
        String imageName = type + "_" + pageId + "_" + uploaded_time;

        String sfn = imageName + "_S.jpg";
        String ofn = imageName + "_M.jpg";
        String lfn = imageName + "_L.jpg";
        urls[0] = sfn;
        urls[1] = ofn;
        urls[2] = lfn;

        if (photoStorage instanceof OssSFS) {
            lfn = "media/photo/" + lfn;
            ofn = "media/photo/" + ofn;
            sfn = "media/photo/" + sfn;
        }

        if (isLogo) {
            SFSUtils.saveScaledUploadImage(fi, photoStorage, sfn, "50", "50", "jpg");
            SFSUtils.saveScaledUploadImage(fi, photoStorage, ofn, "80", "80", "jpg");
            SFSUtils.saveScaledUploadImage(fi, photoStorage, lfn, "180", "180", "jpg");
        } else {
            SFSUtils.saveUpload(fi, photoStorage, sfn);
            SFSUtils.saveUpload(fi, photoStorage, ofn);
            SFSUtils.saveUpload(fi, photoStorage, lfn);
        }

        return urls;
    }

    @WebMethod("page/upload_cover")
    public Record uploadCover(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long pageId = qp.checkGetInt("page");
        FileItem fi = qp.checkGetFile("file");
        String[] urls = savePageImages(pageId, "p_cover", fi, false);
        Record pageRec = new Record();
        pageRec.put("page_id", pageId);
        pageRec.put("small_cover_url", urls[0]);
        pageRec.put("cover_url", urls[1]);
        pageRec.put("large_cover_url", urls[2]);
        return GlobalLogics.getPage().updatePage(ctx, pageRec, false, true);
    }

    @WebMethod("page/upload_logo")
    public Record uploadLogo(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long pageId = qp.checkGetInt("page");
        FileItem fi = qp.checkGetFile("file");
        String[] urls = savePageImages(pageId, "p_logo", fi, true);
        Record pageRec = new Record();
        pageRec.put("page_id", pageId);
        pageRec.put("small_logo_url", urls[0]);
        pageRec.put("logo_url", urls[1]);
        pageRec.put("large_logo_url", urls[2]);
        return GlobalLogics.getPage().updatePage(ctx, pageRec, false, true);
    }

    @WebMethod("page/search")
    public RecordSet searchPages(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        String kw = StringUtils.trimToEmpty(qp.checkGetString("kw"));
        if (StringUtils.isEmpty(kw))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "kw is blank");

        return GlobalLogics.getPage().searchPages(ctx, kw, (int) qp.getInt("page", 0), (int) qp.getInt("count", 20));
    }

    @WebMethod("page/show")
    public RecordSet showPages(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        String pageIds = qp.getString("pages", "");
        if (pageIds.isEmpty()) {
            return ctx.getViewerId() >= 0 ? GlobalLogics.getPage().getPagesForMe(ctx) : new RecordSet();
        } else {
            return GlobalLogics.getPage().getPages(ctx, StringUtils2.splitIntArray(pageIds, ","));
        }
    }

    @WebMethod("page/show1")
    public Record showPage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, false);
        long pageId = qp.checkGetInt("page");
        return GlobalLogics.getPage().getPage(ctx, pageId);
    }

    @WebMethod("page/events")
    public RecordSet getPageEvents(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        GroupLogic g = GlobalLogics.getGroup();
        long pageId = qp.checkGetInt("page_id");
        String eventIds = g.getPageEvents(ctx, pageId);
        if (StringUtils.isBlank(eventIds)) {
            return new RecordSet();
        }

        String cols = qp.getString("columns", GROUP_LIGHT_COLS);
        qp.put("columns", cols + ",start_time,end_time,parent_ids");
        return getGroups(ctx, EVENT_ID_BEGIN, EVENT_ID_END, eventIds, qp);
    }

    private RecordSet getGroups(Context ctx, long begin, long end, String groupIds, QueryParams qp)  {
        boolean isSingle = StringUtils.isNotBlank(groupIds) && !StringUtils.contains(groupIds, ",");
        GroupLogic g = GlobalLogics.getGroup();
        String viewerId = ctx.getViewerIdString();
        String cols = qp.getString("columns", "");
        String columns = StringUtils.isBlank(cols) ? GROUP_LIGHT_COLS : GROUP_LIGHT_COLS + "," + cols;
        boolean withMembers = qp.getBoolean("with_members", false);
        if (!isSingle) {
            RecordSet recs = g.getGroups(ctx, begin, end, viewerId, groupIds, columns, withMembers, (int)qp.getInt("page", -1), (int)qp.getInt("count", -1));
            recs.renameColumn(GRP_COL_ID, "circle_id");
            recs.renameColumn(GRP_COL_NAME, "circle_name");
            return recs;
        }
        else {
            Record rec = g.getGroup(ctx, viewerId, Long.parseLong(groupIds), columns, withMembers);
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
            rec.put("profile_invited", invited.size() > count ? invited.subList(0, count): invited);

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

            return RecordSet.of(rec);
        }
    }

    @WebMethod("page/follow")
    public Record followPage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long pageId = qp.checkGetInt("page");
        if (Constants.getUserTypeById(pageId) != Constants.PAGE_OBJECT
                || !GlobalLogics.getPage().hasPage(ctx, pageId))
            throw new ServerException(WutongErrors.PAGE_ILLEGAL, "Illegal page");

        GlobalLogics.getFriendship().followPage(ctx, ctx.getViewerId(), pageId);
        return GlobalLogics.getPage().getPage(ctx, pageId);
    }

    @WebMethod("page/unfollow")
    public Record unfollowPage(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        long pageId = qp.checkGetInt("page");
        if (Constants.getUserTypeById(pageId) != Constants.PAGE_OBJECT
                || !GlobalLogics.getPage().hasPage(ctx, pageId))
            throw new ServerException(WutongErrors.PAGE_ILLEGAL, "Illegal page");

        GlobalLogics.getFriendship().unfollowPage(ctx, ctx.getViewerId(), pageId);
        return GlobalLogics.getPage().getPage(ctx, pageId);
    }


    private static NamePair checkName(QueryParams qp, boolean withPage) {
        String name = qp.getString(withPage ? "page_name" : "name", "");
        String nameEn = qp.getString(withPage ? "page_name_en" : "name_en", "");
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(nameEn))
            throw new ServerException(WutongErrors.SYSTEM_MISS_REQUIRED_PARAMETER, "Missing name or name_en");

        return new NamePair(name, nameEn);
    }

    private static class NamePair {
        String name;
        String nameEn;

        private NamePair(String name, String nameEn) {
            this.name = name;
            this.nameEn = nameEn;
        }
    }
}
