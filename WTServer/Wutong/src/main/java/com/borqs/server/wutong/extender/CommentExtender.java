package com.borqs.server.wutong.extender;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;

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
    public void extend(Context ctx, RecordSet recs, Set<String> cols) {
        if (cols.contains("comment_count")) {
            for (Record rec : recs) {
                int commentCount = GlobalLogics.getComment().getCommentCountP(ctx, "", objectType, rec.getString(targetIdColumn));
                rec.put("comment_count", commentCount);
            }
        }

        if (cols.contains("comments")) {
            for (Record rec : recs) {
                RecordSet comments = GlobalLogics.getComment().getCommentsForP(ctx, "",objectType, rec.getString(targetIdColumn),
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
