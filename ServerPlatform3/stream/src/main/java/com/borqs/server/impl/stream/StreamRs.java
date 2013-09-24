package com.borqs.server.impl.stream;

import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.util.GeoLocation;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StreamRs {
    public static Post readStream(ResultSet rs, Post post) throws SQLException {
        if (post == null)
            post = new Post();

        PeopleIds addTo = PeopleIds.parse(null, rs.getString("add_to"));
        PeopleIds to = PeopleIds.parse(null, rs.getString("to"));

        post.setPostId(rs.getLong("post_id"));
        post.setMessage(rs.getString("message"));
        post.setAddTo(addTo);
        post.setApp(rs.getInt("app"));
        post.setCreatedTime(rs.getLong("created_time"));
        post.setUpdatedTime(rs.getLong("updated_time"));
        post.setDestroyedTime(rs.getLong("destroyed_time"));
        post.setDevice(rs.getString("device"));
        post.setCanComment(rs.getBoolean("can_comment"));
        post.setCanLike(rs.getBoolean("can_like"));
        post.setCanQuote(rs.getBoolean("can_quote"));
        post.setSourceId(rs.getLong("source"));
        post.setAppData(rs.getString("app_data"));
        post.setAttachments(rs.getString("attachments"));
        String attachmentIds = rs.getString("attachment_ids");
        if (StringUtils.isEmpty(attachmentIds))
            post.setAttachmentIds(new String[0]);
        else
            post.setAttachmentIds(JsonHelper.fromJson(attachmentIds, String[].class));
        post.setLocation(rs.getString("location"));
        post.setGeoLocation(GeoLocation.fromStringPair(rs.getString("longitude"), rs.getString("latitude")));
        post.setPrivate(rs.getBoolean("private"));
        post.setQuote(rs.getLong("quote"));
        post.setType(rs.getInt("type"));
        post.setTo(to);

        return post;
    }

}
