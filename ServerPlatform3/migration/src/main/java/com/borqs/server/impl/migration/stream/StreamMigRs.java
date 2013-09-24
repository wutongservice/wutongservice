package com.borqs.server.impl.migration.stream;


import com.borqs.server.compatible.CompatiblePost;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.util.GeoLocation;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.jackson.JsonNode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class StreamMigRs {

    public static Post readStream(ResultSet rs, Post post0, Map<Long, String> map) throws SQLException {
        Post post = new Post();
        post.setPostId(rs.getLong("post_id"));

        long source = rs.getLong("source");
        if (!map.containsKey(source))
            return null;
        post.setSourceId(source);

        post.setCreatedTime(rs.getLong("created_time"));
        post.setUpdatedTime(rs.getLong("updated_time"));
        post.setDestroyedTime(rs.getLong("destroyed_time"));
        post.setQuote(rs.getLong("quote"));

        PeopleIds friendIds = new PeopleIds();
        for (String str : rs.getString("mentions").split(",")) {
            if (!map.containsValue(str))
                continue;
            friendIds.add(new PeopleId(PeopleId.USER, str));
        }
        post.setTo(friendIds);

        post.setApp(rs.getInt("app"));

        int type1 = rs.getInt("type");
        post.setType(CompatiblePost.v1ToV2Type(type1));

        post.setMessage(rs.getString("message"));
        post.setAppData(rs.getString("app_data"));

        String attachments = rs.getString("attachments");
        post.setAttachments(attachments);


        String attchmentIds = null;

        int type = post.getType();
        if (type == Post.POST_APK) {
            attchmentIds = getAttchmentId(attachments, "apk_id", Target.APK);
        } else if (type == Post.POST_LINK) {
            attchmentIds = getAttchmentId(attachments, "url", Target.LINK);
        } else if (type == Post.POST_PHOTO) {
            attchmentIds = getAttchmentId(attachments, "photo_id", Target.PHOTO);
        } else if (type == Post.POST_BOOK) {
            attchmentIds = getAttchmentId(attachments, "id", Target.BOOK);
        } else if (type == Post.POST_APK_LINK) {
            attchmentIds = getAttchmentId(attachments, "href", Target.APK_LINK);
        } /*else if (type == Post.POST_SIGN_IN) {
            attchmentIds = getAttchmentId(attachments, "user_id", type);
        }*/ else if (type == (Post.POST_APK | Post.POST_COMMENT_BROADCAST)) {
            attchmentIds = getAttchmentId(attachments, "package", Target.APK);
        } else if (type == (Post.POST_APK | Post.POST_LIKE_BROADCAST)) {
            attchmentIds = getAttchmentId(attachments, "package", Target.APK);
        }else if (type == (Post.POST_BOOK | Post.POST_LIKE_BROADCAST)) {
            attchmentIds = getAttchmentId(attachments, "id", Target.BOOK);
        } else if (type == (Post.POST_BOOK | Post.POST_COMMENT_BROADCAST)) {
            attchmentIds = getAttchmentId(attachments, "id", Target.BOOK);
        }/*else if (type == (Post.POST_MUSIC | Post.POST_COMMENT_BROADCAST)) {
            attchmentIds = getAttchmentId(attachments, "id", type);
        } else if (type == (Post.POST_MUSIC | Post.POST_LIKE_BROADCAST)) {
            attchmentIds = getAttchmentId(attachments, "package", type);
        }*/ else if (type == Post.POST_SYSTEM) {
            attchmentIds = getAttchmentId(attachments, "user_id", Target.USER);
        }
        final String attIds = attchmentIds;
        if (StringUtils.isNotEmpty(attIds)){
            post.setAttachmentIds(new String[]{attchmentIds});
        }
        
        post.setDevice(rs.getString("device"));
        post.setCanComment(rs.getBoolean("can_comment"));
        post.setCanLike(rs.getBoolean("can_like"));
        post.setCanQuote(rs.getBoolean("can_reshare"));
        post.setPrivate(rs.getBoolean("privince"));
        double longitude = NumberUtils.toDouble(rs.getString("longitude"), 0.0);
        double latitude = NumberUtils.toDouble(rs.getString("latitude"), 0.0);
        post.setGeoLocation(new GeoLocation(longitude, latitude));
        post.setLocation(rs.getString("location"));

        PeopleIds friendIds_addTo = new PeopleIds();
        for (String str : rs.getString("add_to").split(",")) {
            if (!map.containsValue(str))
                continue;
            friendIds_addTo.add(new PeopleId(PeopleId.USER, str));
        }
        post.setAddTo(friendIds_addTo);

        return post;
    }

    private static String getAttchmentId(String attachments, String id, int type) {

        if (StringUtils.isNotEmpty(attachments) && !StringUtils.equals(attachments, "[]")) {
            StringBuilder sb = new StringBuilder().append(type).append(":");
            JsonNode jn = null;
            try {
                jn = JsonHelper.parse(attachments);
            } catch (Exception ignored) {
            }
            if (jn != null) {
                if (!jn.isArray()) {
                    sb.append(jn.get(id).getValueAsText());
                } else {
                    sb.append(jn.get(0).get(id).getValueAsText());
                }
            }
            return sb.toString();
        }
        return "";
    }

    public static Post checkStream(ResultSet rs, Posts post0, Map<Long, String> map) throws SQLException {
        // new post
        Post n = post0.getPost(rs.getLong("post_id"));


        Post post = new Post();
        post.setPostId(rs.getLong("post_id"));

        long source = rs.getLong("source");
        if (!map.containsKey(source))
            return null;
        post.setSourceId(source);

        post.setCreatedTime(rs.getLong("created_time"));
        post.setUpdatedTime(rs.getLong("updated_time"));
        post.setDestroyedTime(rs.getLong("destroyed_time"));

        long quote = rs.getLong("quote");
        if (map.containsKey(quote))
            post.setQuote(quote);

        PeopleIds friendIds = new PeopleIds();
        for (String str : rs.getString("mentions").split(",")) {
            if (!map.containsValue(str))
                continue;
            friendIds.add(new PeopleId(PeopleId.USER, str));
        }
        post.setTo(friendIds);

        post.setApp(rs.getInt("app"));

        post.setType(CompatiblePost.v1ToV2Type(rs.getInt("type")));

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
        for (String str : rs.getString("add_to").split(",")) {
            if (!map.containsValue(str))
                continue;
            friendIds_addTo.add(new PeopleId(PeopleId.USER, str));
        }
        post.setAddTo(friendIds_addTo);

        return post;
    }

    public static Set<Long> readIds(ResultSet rs, Set<Long> reuse) throws SQLException {
        if (reuse == null)
            reuse = new HashSet<Long>();
        while (rs.next()) {
            reuse.add(rs.getLong("post_id"));
        }
        return reuse;
    }
}
