package com.borqs.server.wutong.page;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.wutong.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PageLogicUtils {
    public static Record asUser(Record pageRec) {
        if (pageRec == null)
            return null;

        // TODO: xx
        return new Record();
    }

    public static RecordSet asUsers(RecordSet pageRecs) {
        if (pageRecs == null)
            return null;

        RecordSet userRecs = new RecordSet(pageRecs.size());
        for (Record pageRec : pageRecs) {
            userRecs.add(asUser(pageRec));
        }
        return userRecs;
    }


    public static List<Long> getPageIdsFromMentions(Context ctx, List<String> mentions) {
        LinkedHashSet<Long> pageIds = new LinkedHashSet<Long>();

        for (String mention : mentions) {
//            if (!StringUtils.startsWith(mention, "#"))
//                continue;

            try {
                long id = Long.parseLong(StringUtils.removeStart(mention, "#"));
                if (Constants.getUserTypeById(id) == Constants.PAGE_OBJECT) {
                    pageIds.add(id);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return new ArrayList<Long>(pageIds);
    }

    public static void removeIllegalPageIds(Context ctx, List<String> mentions) {
        // DO NOTHING
    }

    public static void removeAllPageIds(Context ctx, List<String> mentions) {
        for (String mention : mentions) {
            if (!StringUtils.startsWith(mention, "#"))
                continue;

            try {
                long id = Long.parseLong(StringUtils.removeStart(mention, "#"));
                if (Constants.getUserTypeById(id) == Constants.PAGE_OBJECT) {
                    mentions.remove(mention);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
