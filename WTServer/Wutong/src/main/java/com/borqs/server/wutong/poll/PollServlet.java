package com.borqs.server.wutong.poll;

import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.group.GroupLogic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.JsonNodeFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.borqs.server.wutong.Constants.*;

public class PollServlet extends WebMethodServlet {
    private static final PageTemplate pageTemplate = new PageTemplate(PollServlet.class);
    private static final Logger L = Logger.getLogger(PollServlet.class);
    private String serverHost;

    public static final int DEFAULT_USER_COUNT_IN_PAGE = 20;
    private static String prefix = "http://oss.aliyuncs.com/wutong-data/media/photo/";
    private static String sysPrefix = "http://oss.aliyuncs.com/wutong-data/system/";
    private StaticFileStorage photoStorage;
    private StaticFileStorage profileImageStorage;
    private static int MAX_GUSY_SHARE_TO = 400;
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
    }

    @Override
    public void destroy() {
        profileImageStorage.destroy();
        photoStorage.destroy();
        super.destroy();
    }

    private void dealParentCircles(Context ctx, String parentCircleIds, long pollId) {
        RecordSet parentCircleRecs = null;
        if (StringUtils.isNotBlank(parentCircleIds)) {
            parentCircleRecs = GlobalLogics.getGroup().getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(parentCircleIds), "poll_ids");
            if (CollectionUtils.isEmpty(parentCircleRecs))
                throw new ServerException(WutongErrors.GROUP_NOT_EXISTS);
        }

        if (StringUtils.isNotBlank(parentCircleIds)) {
            if (CollectionUtils.isNotEmpty(parentCircleRecs)) {
                for (Record parentCircleRec : parentCircleRecs) {
                    long parentCircleId = parentCircleRec.getInt("id");
                    String subPollIds = parentCircleRec.getString("poll_ids", "");
                    subPollIds = StringUtils2.addIntToSet(subPollIds, pollId);
                    GlobalLogics.getGroup().updateGroupSimple(ctx, parentCircleId, new Record(), Record.of("poll_ids", subPollIds));
                }
            }
        }
    }

    // poll
        @WebMethod("poll/create")
        public Record createPoll(QueryParams qp, HttpServletRequest req)  {
            Context ctx = WutongContext.getContext(qp, true);
            AccountLogic account = GlobalLogics.getAccount();
            GroupLogic groupLogic = GlobalLogics.getGroup();

            String viewerId = ctx.getViewerIdString();
    
            String ua = ctx.getUa();
            L.info(ctx,"---------------scene of poll create--------------------"+(String.valueOf(ctx.getSession("scene"))));
            L.info(ctx,"---------------scene of poll create--------------------"+(String)ctx.getSession("scene"));
            String loc = ctx.getLocation();
            String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));
    
            String mentions = qp.getString("target", "");
            List<String> groupIds = new ArrayList<String>();
            StringBuilder changeMentions = new StringBuilder();

            groupLogic.getUserAndGroup(ctx,changeMentions, mentions, groupIds);

            mentions = changeMentions.toString();
            String ids = account.parseUserIds(ctx,viewerId, mentions);
            List<String> l = StringUtils2.splitList(ids, ",", true);
            if (l.size() > MAX_GUSY_SHARE_TO)
                throw new ServerException(WutongErrors.STREAM_CANT_SHARE_TOO_MANY_PEOPLE, "Only can share to less than 400 guys!");

    
            String title = qp.checkGetString("title");
            String description = qp.getString("description", "");
            long multi = qp.getInt("multi", 1);
            long limit = qp.getInt("limit", 0);

            long defPrivacy = 0;
            if (StringUtils.contains(mentions, "#-2")) {
                defPrivacy = 0;
            }
            else if (StringUtils.isNotBlank(mentions)) {
                defPrivacy = 2;
            }
            else {
                defPrivacy = 0;
            }

            long privacy = qp.getInt("privacy", defPrivacy);
            long anonymous = qp.getInt("anonymous", 0);
            long mode = qp.getInt("mode", 0); // 0 - can not change vote   1 - can append vote  2- can change vote
            long now = DateUtils.nowMillis();
            long startTime = qp.getInt("start_time", now);
            long endTime = qp.getInt("end_time", 0);
            boolean sendPost = qp.getBoolean("send_post", true);
            boolean sendEmail = qp.getBoolean("send_email", false);
            boolean sendSms = qp.getBoolean("send_sms", false);
            long canAddItems = qp.getInt("can_add_items", 0L);

            long pollId = RandomUtils.generateId(now);
            Record poll = new Record();
            poll.put("id", pollId);
            poll.put("source", viewerId);
            poll.put("target", mentions);
            poll.put("title", title);
            poll.put("description", description);
            poll.put("multi", multi);
            poll.put("limit_", limit);
            poll.put("privacy", privacy);
            poll.put("anonymous", anonymous);
            poll.put("mode", mode);
            poll.put("type", TEXT_POST);
            poll.put("attachments", JsonNodeFactory.instance.arrayNode());
            poll.put("created_time", now);
            poll.put("start_time", startTime);
            poll.put("end_time", endTime);
            poll.put("updated_time", now);
            poll.put("destroyed_time", 0);
            poll.put("can_add_items", canAddItems);

            RecordSet items = new RecordSet();
            Set<String> set = qp.keySet();
            for (String key : set) {
                if (StringUtils.startsWith(key, "message")) {
                    int index = Integer.parseInt(StringUtils.substringAfter(key, "message"));
                    Record item = new Record();
                    item.put("poll_id", pollId);
                    item.put("item_id", RandomUtils.generateId());
                    item.put("type", TEXT_POST);
                    item.put("message", qp.checkGetString(key));
                    item.put("attachments", JsonNodeFactory.instance.arrayNode());
                    item.put("created_time", now);
                    item.put("updated_time", now);
                    item.put("destroyed_time", 0);
                    item.put("index_", index);
                    item.put("source", viewerId);
                    items.add(item);
                }
            }

            PollLogic pollLogic = GlobalLogics.getPoll();
            pollId = pollLogic.createPoll(ctx,poll, items, ua, loc, appId, sendPost, sendEmail, sendSms);
            dealParentCircles(ctx, qp.getString("parent_ids", ""), pollId);
            return pollLogic.getPolls(ctx,viewerId, String.valueOf(pollId), true).getFirstRecord();
        }


    @WebMethod("poll/add_items")
    public Record addItems(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        long pollId = qp.checkGetInt("id");
        boolean sendPost = qp.getBoolean("send_post", true);

        PollLogic pollLogic = GlobalLogics.getPoll();
        Record pollRec = pollLogic.getPolls(ctx, String.valueOf(pollId)).getFirstRecord();
        String source = pollRec.getString("source");
        if (StringUtils.equals(viewerId, source)) {
            long startTime = pollRec.getInt("start_time");
            long endTime = pollRec.getInt("end_time");
            long now = DateUtils.nowMillis();
            int status = 0; //  0 - not start    1 - process  2 - end
            if (now < startTime)
                status = 0;
            else if (((now >= startTime) && (now <= endTime)) || endTime == 0)
                status = 1;
            else
                status = 2;

            if (status == 2)
                throw new ServerException(WutongErrors.POLL_ADD_ITEMS_ERROR, "poll add items failed");
        }
        else if (!pollLogic.canVote(ctx,viewerId, pollId))
            throw new ServerException(WutongErrors.POLL_CANT_COMMENT, "You can not vote this poll");

        RecordSet items = new RecordSet();
        Set<String> set = qp.keySet();
        long now = DateUtils.nowMillis();
        for (String key : set) {
            if (StringUtils.startsWith(key, "message")) {
                int index = Integer.parseInt(StringUtils.substringAfter(key, "message"));
                Record item = new Record();
                item.put("poll_id", pollId);
                item.put("item_id", RandomUtils.generateId());
                item.put("type", TEXT_POST);
                item.put("message", qp.checkGetString(key));
                item.put("attachments", JsonNodeFactory.instance.arrayNode());
                item.put("created_time", now);
                item.put("updated_time", now);
                item.put("destroyed_time", 0);
                item.put("index_", index);
                item.put("source", viewerId);
                items.add(item);
            }
        }

        long multi = pollRec.getInt("multi");
        if (!StringUtils.equals(viewerId, source) && (items.size() > multi))
            throw new ServerException(WutongErrors.POLL_VOTE_ITEMS_OUT_OF_LIMIT, "You can only vote " + multi + " items");

        boolean b = pollLogic.addItems(ctx, items);
        if (b) {
            if (!StringUtils.equals(viewerId, source)) {
                Record itemsRec = new Record();
                for (Record item : items) {
                    itemsRec.put(item.getString("item_id"), 1L);
                }

                long mode = pollRec.getInt("mode");
                if (mode == 2L) {
                    String itemIds = qp.getString("item_ids", "");
                    if (StringUtils.isNotBlank(itemIds)) {
                        List<String> lItemIds = StringUtils2.splitList(itemIds, ",", true);
                        for (String itemId : lItemIds) {
                            itemsRec.put(itemId, 1L);
                        }
                    }
                }
                pollLogic.vote(ctx, viewerId, pollId, itemsRec, ctx.getUa(), ctx.getLocation(), ctx.getAppId(), sendPost);
            }
            return pollLogic.getPolls(ctx,viewerId, String.valueOf(pollId), true).getFirstRecord();
        } else {
            throw new ServerException(WutongErrors.POLL_ADD_ITEMS_ERROR, "poll add items failed");
        }
    }
        @WebMethod("poll/vote")
        public Record vote(QueryParams qp, HttpServletRequest req)  {
            Context ctx = WutongContext.getContext(qp, true);
            String viewerId = ctx.getViewerIdString();

            PollLogic pollLogic = GlobalLogics.getPoll();

            String ua = ctx.getUa();
            String loc = ctx.getLocation();
            String appId = qp.getString("appid", String.valueOf(APP_TYPE_BPC));
    
            long pollId = qp.checkGetInt("poll_id");


            if (!pollLogic.canVote(ctx,viewerId, pollId))
                throw new ServerException(WutongErrors.POLL_CANT_COMMENT, "You can not vote this poll");
    
            boolean sendPost = qp.getBoolean("send_post", true);
            String itemIds = qp.checkGetString("item_ids");
            String weights= qp.getString("weights", "");
            List<String> lItemIds = StringUtils2.splitList(itemIds, ",", true);
            List<Long> lWeights = StringUtils2.splitIntList(weights, ",");
            Record items = new Record();
            long voteCount = 0;
            int size = lItemIds.size();
            for (int i = 0; i < size; i++) {
                if (StringUtils.isBlank(weights)) {
                    items.put(lItemIds.get(i), 1L);
                    voteCount++;
                }
                else {
                    long weight = lWeights.get(i);
                    items.put(lItemIds.get(i), weight);
                    voteCount += weight;
                }
            }
    
            Record poll = pollLogic.getPolls(ctx, String.valueOf(pollId)).getFirstRecord();
            long multi = poll.getInt("multi");
            if (voteCount > multi)
                throw new ServerException(WutongErrors.POLL_VOTE_ITEMS_OUT_OF_LIMIT, "You can only vote " + multi + " items");
            
            pollLogic.vote(ctx,viewerId, pollId, items, ua, loc, appId, sendPost);
            return pollLogic.getPolls(ctx,viewerId, String.valueOf(pollId), true).getFirstRecord();
        }
    
        @WebMethod("poll/get")
        public RecordSet getPolls(QueryParams qp)  {
            Context ctx = WutongContext.getContext(qp, true);
            String viewerId = ctx.getViewerIdString();
            PollLogic pollLogic = GlobalLogics.getPoll();
            
            String pollIds = qp.checkGetString("ids");
            boolean withItems = qp.getBoolean("with_items", false);
            return pollLogic.getPolls(ctx,viewerId, pollIds, withItems);
        }
    
        @WebMethod("poll/detail")
        public Record getPoll(QueryParams qp)  {
            Context ctx = WutongContext.getContext(qp, true);
            String viewerId = ctx.getViewerIdString();
            PollLogic pollLogic = GlobalLogics.getPoll();

            String pollIds = qp.checkGetString("id");
            boolean withItems = qp.getBoolean("with_items", false);
            RecordSet recs = pollLogic.getPolls(ctx,viewerId, pollIds, withItems);
            if (recs.isEmpty()) {
                throw new ServerException(WutongErrors.POLL_NOT_EXISTS, "The poll is not exists");
            }
            else
                return recs.get(0);
        }
    
    private RecordSet getSubPolls(Context ctx, long circleId) {
        Record circleRec = GlobalLogics.getGroup().getSimpleGroups(ctx, PUBLIC_CIRCLE_ID_BEGIN, PUBLIC_CIRCLE_ID_END, String.valueOf(circleId), "poll_ids").getFirstRecord();
        String pollIds = circleRec.getString("poll_ids", "");
        PollLogic pollLogic = GlobalLogics.getPoll();
        return pollLogic.getPolls(ctx, ctx.getViewerIdString(), pollIds, false);
    }

    @WebMethod("poll/list/user")
        public RecordSet getUserPolls(QueryParams qp)  {
            Context ctx = WutongContext.getContext(qp, true);
            String viewerId = ctx.getViewerIdString();
            PollLogic pollLogic = GlobalLogics.getPoll();

            String userId = qp.getString("user_id", viewerId);
            int type = (int) qp.getInt("type", 0);
            int page = (int) qp.getInt("page", 0);
            int count = (int) qp.getInt("count", 20);
            
            if (type == 0)
                return pollLogic.getCreatedPollsPlatform(ctx, viewerId, userId, page, count);
            else if (type == 1)
                return pollLogic.getParticipatedPollsPlatform(ctx, viewerId, userId, page, count);
            else {
                long id = Long.parseLong(userId);
                if (id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END)
                    return getSubPolls(ctx, id);
                else
                    return pollLogic.getInvolvedPollsPlatform(ctx, viewerId, userId, page, count);
            }
        }
    
        @WebMethod("poll/list/friends")
        public RecordSet getFriendsPolls(QueryParams qp)  {
            Context ctx = WutongContext.getContext(qp, true);
            String viewerId = ctx.getViewerIdString();
            PollLogic pollLogic = GlobalLogics.getPoll();
            
            String userId = qp.getString("user_id", viewerId);
            int sort = (int) qp.getInt("sort", 0);
            int page = (int) qp.getInt("page", 0);
            int count = (int) qp.getInt("count", 20);
            return pollLogic.getFriendsPollsPlatform(ctx,viewerId, userId, sort, page, count);
        }
    
        @WebMethod("poll/list/public")
        public RecordSet getPublicPolls(QueryParams qp)  {
            Context ctx = WutongContext.getContext(qp, true);
            String viewerId = ctx.getViewerIdString();
            PollLogic pollLogic = GlobalLogics.getPoll();

            String userId = qp.getString("user_id", viewerId);
            int sort = (int) qp.getInt("sort", 0);
            int page = (int) qp.getInt("page", 0);
            int count = (int) qp.getInt("count", 20);
            return pollLogic.getPublicPollsPlatform(ctx, viewerId, userId, sort, page, count);
        }
    
        @WebMethod("poll/destroy")
        public boolean destroyPolls(QueryParams qp)  {
            Context ctx = WutongContext.getContext(qp, true);
            String viewerId = ctx.getViewerIdString();
            PollLogic pollLogic = GlobalLogics.getPoll();

            String pollIds = qp.checkGetString("ids");
            return pollLogic.deletePolls(ctx, viewerId, pollIds);
        }


}
