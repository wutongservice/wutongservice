package com.borqs.server.market.models;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.i18n.SpringMessage;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;

public class StatTypes {
    public static final String STAT_PURCHASE_COUNT = "purchase_count";
    public static final String STAT_DOWNLOAD_COUNT = "download_count";

    public static Records allStatTypes(String locale) {
        Records types = new Records();
        types.add(new Record()
                .set("id", STAT_PURCHASE_COUNT)
                .set("name", SpringMessage.get("publish_productStat.text.purchaseCount", locale)));
        types.add(new Record()
                .set("id", STAT_DOWNLOAD_COUNT)
                .set("name", SpringMessage.get("publish_productStat.text.downloadCount", locale)));
        return types;
    }

    public static Records allStatTypes(ServiceContext ctx) {
        return allStatTypes(ctx.getClientLocale("en_US"));
    }

}
