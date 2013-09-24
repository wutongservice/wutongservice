package com.borqs.server.wutong.subscribe;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.conversation.ConversationLogic;
import com.borqs.server.wutong.friendship.FriendshipLogic;

import javax.servlet.ServletException;
import java.util.Set;

public class SubscribeServlet extends WebMethodServlet {
    private static final Logger L = Logger.getLogger(SubscribeServlet.class);

    @Override
    public void init() throws ServletException {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @WebMethod("subscribe")
    public boolean subscribe(QueryParams qp) {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        int targetType = (int)qp.getInt("object_type", Constants.POST_OBJECT);
        String targetId = qp.checkGetString("object_id");

        ConversationLogic c = GlobalLogics.getConversation();
        if (targetType == Constants.POST_OBJECT) {
            int enabled = c.getEnabled(ctx, targetType, targetId);
            if (enabled == 1) {
                return c.enableConversion(ctx, targetType, targetId, 0);
            } else if (enabled == 0) {
                return c.enableConversion(ctx, targetType, targetId, 1);
            } else {
                return c.createConversationP(ctx, targetType, targetId, Constants.C_SUBSCRIBE_STREAM, viewerId);
            }
        }

        if (targetType == Constants.USER_OBJECT) {
            int enabled = c.getEnabled(ctx, targetType, targetId);
            if (enabled == 1) {
                return c.enableConversion(ctx, targetType, targetId, 0);
            } else if (enabled == 0) {
                return c.enableConversion(ctx, targetType, targetId, 1);
            } else {
                return c.createConversationP(ctx, targetType, targetId, Constants.C_SUBSCRIBE_USER, viewerId);
            }
        }

        if (targetType == Constants.LOCAL_CIRCLE_OBJECT) {
            FriendshipLogic fs = GlobalLogics.getFriendship();
            int enabled = c.getEnabled(ctx, targetType, targetId);
            if (enabled == 1) {
                String friendIds = fs.getFriendsIdP(ctx, viewerId, targetId, "", -1, -1);
                Set<String> friends = StringUtils2.splitSet(friendIds, ",", true);
                for (String friend : friends) {
                    c.deleteConversationP(ctx, Constants.USER_OBJECT, friend, Constants.C_SUBSCRIBE_LOCAL_CIRCLE, ctx.getViewerId());
                }
                return c.enableConversion(ctx, targetType, targetId, 0);
            } else if (enabled == 0) {
                String friendIds = fs.getFriendsIdP(ctx, viewerId, targetId, "", -1, -1);
                Set<String> friends = StringUtils2.splitSet(friendIds, ",", true);
                for (String friend : friends) {
                    c.createConversationP(ctx, Constants.USER_OBJECT, friend, Constants.C_SUBSCRIBE_LOCAL_CIRCLE, viewerId);
                }
                return c.enableConversion(ctx, targetType, targetId, 1);
            } else {
                String friendIds = fs.getFriendsIdP(ctx, viewerId, targetId, "", -1, -1);
                Set<String> friends = StringUtils2.splitSet(friendIds, ",", true);
                for (String friend : friends) {
                    c.createConversationP(ctx, Constants.USER_OBJECT, friend, Constants.C_SUBSCRIBE_LOCAL_CIRCLE, viewerId);
                }
                return c.createConversationP(ctx, targetType, targetId, Constants.C_SUBSCRIBE_LOCAL_CIRCLE, viewerId);
            }
        }

        return true;
    }
}
