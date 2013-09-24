package com.borqs.server.market.service;


import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;


public class ShareServiceUtils {
    public static void swapShareIdAndProductId(Record shareRec) {
        if (shareRec != null) {
            shareRec.renameField("id", "share_id");
            shareRec.renameField("file_id", "id");
        }
    }

    public static void swapShareIdAndProductId(Records shareRecs) {
        if (shareRecs != null) {
            shareRecs.renameField("id", "share_id");
            shareRecs.renameField("file_id", "id");
        }
    }
}
