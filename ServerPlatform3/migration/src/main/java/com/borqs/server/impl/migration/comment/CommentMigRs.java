package com.borqs.server.impl.migration.comment;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import org.apache.commons.lang.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommentMigRs {

    public static Comment readComment(ResultSet rs, Comment comment, Map<Long, String> map) throws SQLException {
        if (comment == null)
            comment = new Comment();

        comment.setCommentId(rs.getLong("comment_id"));
        comment.setMessage(rs.getString("message"));
        comment.setCanLike(rs.getBoolean("can_like"));

        long commenter = rs.getLong("commenter");
        if (!map.containsKey(commenter))
            return null;
        comment.setCommenterId(commenter);

        comment.setCreatedTime(rs.getLong("created_time"));
        comment.setDestroyedTime(rs.getLong("destroyed_time"));
        comment.setDevice(rs.getString("device"));
        String targetOld = rs.getString("target");

        String[] targetArray = new String[2];
        if (StringUtils.isNotEmpty(targetOld))
            targetArray = targetOld.split(":");
        Target target = new Target(Integer.parseInt(targetArray[0]), targetArray[1]);

        PeopleIds friendIds_addTo = new PeopleIds();
        for (String str : rs.getString("add_to").split(",")) {
            if (!map.containsValue(str))
                continue;
            friendIds_addTo.add(new PeopleId(PeopleId.USER, str));
        }
        comment.setAddTo(friendIds_addTo);
        comment.setTarget(target);

        return comment;
    }

    public static Set<Long> readIds(ResultSet rs, Set<Long> reuse) throws SQLException {
        if (reuse == null)
            reuse = new HashSet<Long>();
        while (rs.next()) {
            reuse.add(rs.getLong("comment_id"));
        }
        return reuse;
    }

}
