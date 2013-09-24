package com.borqs.server.wutong.notif;

import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;

public class SubscribeNotifComposer {
    private static final Logger L = Logger.getLogger(SubscribeNotifComposer.class);

    public static String getTitle(Context ctx, String lang, String subscribeIds) {
        RecordSet recs = GlobalLogics.getAccount().getUsersBaseColumns(ctx, subscribeIds);
        String displayNames = recs.joinColumnValues("display_name", ",");
        String template = Constants.getBundleStringByLang(lang, "subscribe.notif");
        String title = SQLTemplate.merge(template, new Object[][]{
                {"names", displayNames}
        });
        return title;
    }

    public static String getUri(Context ctx, String subscribeIds) {
        return "borqs://subscribe/notification?ids=" + subscribeIds;
    }

    public static String getData(Context ctx, String receiverId) {
        return "," + receiverId + ",";
    }

    public static String getTitleHtml(Context ctx, String lang, String subscribeIds) {
        ArrayList<String> l = new ArrayList<String>();
        RecordSet recs = GlobalLogics.getAccount().getUsersBaseColumns(ctx, subscribeIds);
        for (Record rec : recs) {
            String userId = rec.getString("user_id");
            String displayName = rec.getString("display_name");
            l.add("<a href=\"borqs://profile/details?uid=" + userId + "&tab=2\">" + displayName + "</a>");
        }
        String nameLinks = StringUtils.join(l, ", ");

        String template = Constants.getBundleStringByLang(lang, "subscribe.notif");
        String titleHtml = SQLTemplate.merge(template, new Object[][]{
                {"names", nameLinks}
        });
        return titleHtml;
    }

    public static Record createNotification(Context ctx, String senderId, String receiverId, String lang, String subscribeIds) {
        final String METHOD = "createNotification";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, senderId);

        Record msg = new Record();
        msg.put("appId", String.valueOf(Constants.APP_TYPE_BPC));
        msg.put("senderId", senderId);
        msg.put("receiverId", receiverId);

        String title = getTitle(ctx, lang, subscribeIds);
        L.trace(ctx, "title: " + title);
        msg.put("title", title);

        msg.put("action", "android.intent.action.VIEW");
        msg.put("type", Constants.NTF_SUBSCRIBE);

        String uri = getUri(ctx, subscribeIds);
        L.trace(ctx, "uri: " + uri);
        msg.put("uri", uri);

        String data = getData(ctx, receiverId);
        L.trace(ctx, "data: " + data);
        msg.put("data", data);

        String titleHtml = getTitleHtml(ctx, lang, subscribeIds);
        L.trace(ctx, "titleHtml: " + titleHtml);
        msg.put("titleHtml", titleHtml);

        msg.put("body", "");
        msg.put("bodyHtml", "");
        msg.put("objectId", "");

        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return msg;
    }
}
