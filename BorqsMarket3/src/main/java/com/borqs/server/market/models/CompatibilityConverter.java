package com.borqs.server.market.models;


import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;

public class CompatibilityConverter {

    public static Record renameDownloadCountToPurchaseCount(Record rec) {
        if (rec != null) {
            if (rec.hasField("download_count")) {
                rec.renameField("download_count", "purchase_count");
            }
        }
        return rec;
    }

    public static Records renameDownloadCountToPurchaseCount(Records recs) {
        if (recs != null) {
            for (Record rec : recs) {
                renameDownloadCountToPurchaseCount(rec);
            }
        }
        return recs;
    }

    public static RecordsWithTotal renameDownloadCountToPurchaseCount(RecordsWithTotal recsWithTotal) {
        if (recsWithTotal != null) {
            renameDownloadCountToPurchaseCount(recsWithTotal.getRecords());
        }
        return recsWithTotal;
    }


    public static Params supportedModAsAppMod(Params params) {
        if (params.hasParam("supported_mod") && !params.hasParam("app_mod")) {
            params.renameParam("supported_mod", "app_mod");
        }
        return params;
    }
}
