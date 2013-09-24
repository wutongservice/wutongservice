package com.borqs.server.service.platform.extender;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.service.platform.Constants;
import org.apache.avro.AvroRemoteException;

import java.util.Set;

public class CommentExtender extends PlatformExtender {
    private static final Set<String> EXTENDED_COLUMNS = CollectionUtils2.asSet("comment_count", "comments");
    private int objectType;
    private String targetIdColumn;


    public CommentExtender(Configuration conf) {
        this.objectType = Constants.OBJECTS.getValue(conf.checkGetString("object"));
        this.targetIdColumn = conf.checkGetString("column");
    }

    public CommentExtender(int objectType, String targetIdColumn) {
        this.objectType = objectType;
        this.targetIdColumn = targetIdColumn;
    }

    @Override
    protected void extend0(RecordSet recs, Set<String> cols) throws AvroRemoteException {
        if (cols.contains("comment_count")) {
            for (Record rec : recs) {
                int commentCount = platform.getCommentCount("",objectType, rec.getString(targetIdColumn));
                rec.put("comment_count", commentCount);
            }
        }

        if (cols.contains("comments")) {
            for (Record rec : recs) {
                RecordSet comments = platform.getCommentsFor("",objectType, rec.getString(targetIdColumn),
                        "comment_id, created_time, commenter, commenter_name, message", false, 0, 2);
                rec.put("comments", comments.toJsonNode());
            }
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
