package com.borqs.server.impl.migration;


import com.borqs.server.impl.migration.account.AccountConverter;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.util.GeoLocation;
import org.apache.commons.lang.math.NumberUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MigrationRs {
    public static Post readStream(ResultSet rs, Post post0) throws SQLException {
        Post post = new Post();
        post.setPostId(rs.getLong("post_id"));
        post.setSourceId(rs.getLong("source"));
        post.setCreatedTime(rs.getLong("created_time"));
        post.setUpdatedTime(rs.getLong("updated_time"));
        post.setDestroyedTime(rs.getLong("destroyed_time"));
        post.setQuote(rs.getLong("quote"));

        PeopleIds friendIds = new PeopleIds();
        for(String str:rs.getString("mentions").split(","))
            friendIds.add(new PeopleId(PeopleId.USER,str));
        post.setTo(friendIds);

        post.setApp(rs.getInt("app"));
        post.setType(rs.getInt("type"));
        post.setMessage(rs.getString("message"));
        post.setAppData(rs.getString("app_data"));
        post.setAttachments(rs.getString("attachments"));
        post.setDevice(rs.getString("device"));
        post.setCanComment(rs.getBoolean("can_comment"));
        post.setCanLike(rs.getBoolean("can_like"));
        post.setCanQuote(rs.getBoolean("can_reshare"));
        post.setPrivate(rs.getBoolean("privince"));
        double longitude = NumberUtils.toDouble(rs.getString("longitude"), 0.0);
        double latitude = NumberUtils.toDouble(rs.getString("latitude"), 0.0);
        post.setGeoLocation(new GeoLocation(longitude, latitude));

        PeopleIds friendIds_addTo = new PeopleIds();
        for(String str:rs.getString("add_to").split(","))
            friendIds_addTo.add(new PeopleId(PeopleId.USER,str));
        post.setAddTo(friendIds_addTo);
 
        return post;
    }

    public static MigrationFriend readFriend(ResultSet rs, MigrationFriend friend0) throws SQLException {
        MigrationFriend friend = new MigrationFriend();
        friend.setCricle(rs.getInt("circle"));
        friend.setFriend(rs.getLong("friend"));
        friend.setReason(rs.getInt("reason"));
        friend.setType(rs.getInt("type") == 0 ? 1 : 2);
        friend.setUpdated_time(0);
        friend.setUser(rs.getLong("user"));
        return friend;
    }

    public static MigrationCircle readCircle(ResultSet rs, MigrationCircle circle0) throws SQLException {
        MigrationCircle circle = new MigrationCircle();
        circle.setCircle(rs.getInt("circle"));
        circle.setName(rs.getString("name"));
        circle.setUpdated_time(0);
        circle.setUser(rs.getLong("user"));

        return circle;
    }

    public static User readAccount(ResultSet rs, List<String> errorList) throws SQLException {
        User user1 = new User();
        try {
            user1 =  AccountConverter.converterRecord2User(rs);
            user1.setAddon("status",rs.getString("status"));
            user1.setAddon("status_updated_time",rs.getLong("status_updated_time"));
        } catch (Exception e) {
            errorList.add(rs.getString("user_id"));
        }
        return user1;
    }
}
