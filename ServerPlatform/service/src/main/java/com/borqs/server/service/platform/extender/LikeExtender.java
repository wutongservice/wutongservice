package com.borqs.server.service.platform.extender;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;

import java.util.Set;

public class LikeExtender extends PlatformExtender {
    private static final Set<String> EXTENDED_COLUMNS = CollectionUtils2.asSet("likes", "like_count", "liked_users");

    private int objectType;
    private String targetIdColumn;

    public LikeExtender(Configuration conf) {
        this.objectType = Constants.OBJECTS.getValue(conf.checkGetString("object"));
        this.targetIdColumn = conf.checkGetString("column");
    }

    public LikeExtender(int objectType, String targetIdColumn) {
        this.objectType = objectType;
        this.targetIdColumn = targetIdColumn;
    }

    @Override
    protected void extend0(RecordSet recs, Set<String> cols) throws AvroRemoteException {
        if (cols.contains("likes")) {
            for (Record rec : recs) {
                if (Constants.isNullUserId(viewerId)) {
                    rec.put("likes", false);
                } else {
                    // TODO: xx
                    rec.put("likes", false);
                }
            }
        }

        if (cols.contains("like_count")) {
            for (Record rec : recs) {
                int likeCount = platform.getLikeCount(objectType, rec.getString(targetIdColumn));
                rec.put("like_count", likeCount);
            }
        }

        if (cols.contains("liked_users")) {
            // TODO: xx
        }
    }

    @Override
    public Set<String> necessaryColumns() {
        return CollectionUtils2.asSet(targetIdColumn);
    }

    @Override
    public Set<String> extendedColumns() {
        return EXTENDED_COLUMNS;
    }
}
