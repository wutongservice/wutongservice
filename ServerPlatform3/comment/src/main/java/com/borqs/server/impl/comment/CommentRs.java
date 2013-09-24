package com.borqs.server.impl.comment;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.friend.PeopleIds;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class CommentRs {
    public static Comment readComment(ResultSet rs, Comment comment) throws SQLException {
        if (comment == null)
            comment = new Comment();

        comment.setCommentId(rs.getLong("comment_id"));
        comment.setMessage(rs.getString("message"));
        comment.setCanLike(rs.getBoolean("can_like"));
        comment.setCommenterId(rs.getLong("commenter"));
        comment.setCreatedTime(rs.getLong("created_time"));
        comment.setDestroyedTime(rs.getLong("destroyed_time"));
        comment.setDevice(rs.getString("device"));
        Target target = new Target(rs.getInt("target_type"), rs.getString("target_id"));
        String addToStr = rs.getString("add_to");
        comment.setAddTo(PeopleIds.parse(null, addToStr));
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
