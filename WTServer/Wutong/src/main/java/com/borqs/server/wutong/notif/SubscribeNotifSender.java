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
import org.codehaus.jackson.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SubscribeNotifSender extends NotificationSender {

    private static final Logger L = Logger.getLogger(SubscribeNotifSender.class);
    private HashSet<Long> toIds = new HashSet<Long>();
    private String subscribeIds = "";

    public SubscribeNotifSender() {
        super();
        isReplace = true;
    }

    @Override
    public List<Long> getScope(Context ctx, String senderId, Object... args) {
        List<Long> userIds = new ArrayList<Long>();

        long receiverId = Long.parseLong((String) args[0]);
        toIds.add(receiverId);
        userIds.add(receiverId);

        JsonNode arr = notif.query(String.valueOf(Constants.APP_TYPE_BPC), Constants.NTF_SUBSCRIBE, String.valueOf(receiverId), "");
        if (arr.size() > 0) {
            JsonNode jn = arr.get(0);
            boolean read = jn.get("read").getBooleanValue();
            if (read) {
                this.subscribeIds = senderId;
            } else {
                String uri = jn.get("uri").getTextValue();
                subscribeIds = StringUtils.substringAfter(uri, "=") + "," + senderId;
            }
        } else  {
            this.subscribeIds = senderId;
        }
        return userIds;
    }

    @Override
    protected String getSettingKey(Context ctx) {
        return Constants.NTF_SUBSCRIBE;
    }

    @Override
    protected String getAppId(Context ctx, Object... args) {
        return String.valueOf(Constants.APP_TYPE_BPC);
    }

    @Override
    protected String getTitle(Context ctx, String lang, Object... args) {
        RecordSet recs = GlobalLogics.getAccount().getUsersBaseColumns(ctx, this.subscribeIds);
        String displayNames = recs.joinColumnValues("display_name", ",");
        String template = Constants.getBundleStringByLang(lang, "subscribe.notif");
        String title = SQLTemplate.merge(template, new Object[][]{
                {"names", displayNames}
        });
        return title;
    }

    @Override
    protected String getUri(Context ctx, Object... args) {
        return "borqs://subscribe/notification?ids=" + subscribeIds;
    }

    @Override
    protected String getData(Context ctx, Object... args) {
        if (toIds.isEmpty())
            return "";
        else
            return "," + StringUtils2.joinIgnoreBlank(",", toIds) + ",";
    }

    @Override
    protected String getTitleHtml(Context ctx, String lang, Object... args) {
        ArrayList<String> l = new ArrayList<String>();
        RecordSet recs = GlobalLogics.getAccount().getUsersBaseColumns(ctx, this.subscribeIds);
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
}
