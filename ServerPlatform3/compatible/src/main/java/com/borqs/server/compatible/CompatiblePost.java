package com.borqs.server.compatible;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.comment.CommentPostExpansion;
import com.borqs.server.platform.feature.like.LikePostExpansion;
import com.borqs.server.platform.feature.link.LinkEntities;
import com.borqs.server.platform.feature.link.LinkEntity;
import com.borqs.server.platform.feature.photo.Photos;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.io.IOException;
import java.util.LinkedHashSet;

public class CompatiblePost {

    public static final int V1POST_TEXT = 1;
    public static final int V1POST_PHOTO = 1 << 1;
    public static final int V1POST_VIDEO = 1 << 2;
    public static final int V1POST_AUDIO = 1 << 3;
    public static final int V1POST_BOOK = 1 << 4;
    public static final int V1POST_APK = 1 << 5;
    public static final int V1POST_LINK = 1 << 6;
    public static final int V1POST_APK_LINK = 1 << 7;
    public static final int V1POST_APK_COMMENT = 1 << 8;
    public static final int V1POST_APK_LIKE = 1 << 9;
    public static final int V1POST_BOOK_LIKE = 1 << 10;
    public static final int V1POST_BOOK_COMMENT = 1 << 11;
    public static final int V1POST_FRIEND_SET = 1 << 12;
    public static final int V1POST_MUSIC = 1 << 13;
    public static final int V1POST_MUSIC_COMMENT = 1 << 14;
    public static final int V1POST_MUSIC_LIKE = 1 << 15;
    public static final int V1POST_SIGN_IN = 1 << 16;

    public static int V1_ALL_POST_TYPES = V1POST_TEXT | V1POST_PHOTO | V1POST_VIDEO | V1POST_AUDIO
            | V1POST_BOOK | V1POST_APK | V1POST_LINK | V1POST_APK_LINK | V1POST_APK_COMMENT | V1POST_APK_LIKE
            | V1POST_BOOK_LIKE | V1POST_BOOK_COMMENT | V1POST_FRIEND_SET | V1POST_MUSIC | V1POST_MUSIC_COMMENT
            | V1POST_MUSIC_LIKE | V1POST_SIGN_IN;

    public static final String V1COL_POST_ID = "post_id";
    public static final String V1COL_SOURCE = "source";
    public static final String V1COL_FROM = "from";
    public static final String V1COL_CREATED_TIME = "created_time";
    public static final String V1COL_UPDATED_TIME = "updated_time";
    public static final String V1COL_QUOTE = "quote";
    public static final String V1COL_ROOT = "root";
    public static final String V1COL_ROOT_ID = "root_id";
    public static final String V1COL_MENTIONS = "mentions";
    public static final String V1COL_APP = "app";
    public static final String V1COL_TYPE = "type";
    public static final String V1COL_APP_DATA = "app_data";
    public static final String V1COL_MESSAGE = "message";
    public static final String V1COL_DEVICE = "device";
    public static final String V1COL_CAN_COMMENT = "can_comment";
    public static final String V1COL_CAN_LIKE = "can_like";
    public static final String V1COL_CAN_RESHARE = "can_reshare";
    public static final String V1COL_ADD_TO = "add_to";
    public static final String V1COL_ADD_NEW_USERS = "add_new_users";
    public static final String V1COL_PRIVATE = "privince";
    public static final String V1COL_SECRETLY = "secretly";
    public static final String V1COL_ATTACHMENTS = "attachments";
    public static final String V1COL_DESTROYED_TIME = "destroyed_time";
    public static final String V1COL_TARGET = "target";
    public static final String V1COL_LOCATION = "location";
    public static final String V1COL_ADD_CONTACT = "add_contact";
    public static final String V1COL_HAS_CONTACT = "has_contact";
    public static final String V1COL_LONGITUDE = "longitude";
    public static final String V1COL_LATITUDE = "latitude";
    public static final String V1COL_TO = "to";
    public static final String V1COL_COMMENTS = "comments";
    public static final String V1COL_LIKED = "iliked";
    public static final String V1COL_LIKES = "likes";
    public static final String V1COL_RESHARE_COUNT = "reshare_count";
    public static final String V1COL_RETWEETED_STREAM = "retweeted_stream";
    public static final String V1COL_ICON = "icon";

    public static final String[] V1_FULL_COLUMNS = {
            V1COL_POST_ID,
            V1COL_SOURCE,
            V1COL_FROM,
            V1COL_CREATED_TIME,
            V1COL_UPDATED_TIME,
            V1COL_QUOTE,
            V1COL_ROOT,
            V1COL_ROOT_ID,
            V1COL_MENTIONS,
            V1COL_APP,
            V1COL_TYPE,
            V1COL_ICON,
            V1COL_APP_DATA,
            V1COL_MESSAGE,
            V1COL_DEVICE,
            V1COL_CAN_COMMENT,
            V1COL_CAN_LIKE,
            V1COL_CAN_RESHARE,
            V1COL_ADD_TO,
            V1COL_ADD_NEW_USERS,
            V1COL_PRIVATE,
            V1COL_ATTACHMENTS,
            V1COL_DESTROYED_TIME,
            V1COL_TARGET,
            V1COL_LOCATION,
            V1COL_ADD_CONTACT,
            V1COL_HAS_CONTACT,
            V1COL_LONGITUDE,
            V1COL_LATITUDE,
            V1COL_SECRETLY,
            V1COL_TO,
            V1COL_COMMENTS,
            V1COL_LIKED,
            V1COL_LIKES,
            V1COL_RESHARE_COUNT,
            V1COL_RETWEETED_STREAM,
    };

    public static String[] expandV1Columns(String[] v1Cols) {
        return ColumnsExpander.expand(v1Cols, "#full", V1_FULL_COLUMNS);
    }

    public static String[] v1ToV2Columns(String[] v1Cols) {
        LinkedHashSet<String> l = new LinkedHashSet<String>();
        if (ArrayUtils.contains(v1Cols, V1COL_POST_ID))
            l.add(Post.COL_POST_ID);
        if (ArrayUtils.contains(v1Cols, V1COL_SOURCE))
            l.add(Post.COL_SOURCE);
        if (ArrayUtils.contains(v1Cols, V1COL_FROM))
            l.add(Post.COL_SOURCE);
        if (ArrayUtils.contains(v1Cols, V1COL_CREATED_TIME))
            l.add(Post.COL_CREATED_TIME);
        if (ArrayUtils.contains(v1Cols, V1COL_UPDATED_TIME))
            l.add(Post.COL_UPDATED_TIME);
        if (ArrayUtils.contains(v1Cols, V1COL_QUOTE))
            l.add(Post.COL_QUOTE);
        // root
        if (ArrayUtils.contains(v1Cols, V1COL_MENTIONS) || ArrayUtils.contains(v1Cols, V1COL_TO)) {
            l.add(Post.COL_TO);
            l.add(Post.COL_TO_IDS);
        }
        if (ArrayUtils.contains(v1Cols, V1COL_APP))
            l.add(Post.COL_APP);
        if (ArrayUtils.contains(v1Cols, V1COL_TYPE))
            l.add(Post.COL_TYPE);
        if (ArrayUtils.contains(v1Cols, V1COL_APP_DATA))
            l.add(Post.COL_APP_DATA);
        if (ArrayUtils.contains(v1Cols, V1COL_MESSAGE))
            l.add(Post.COL_MESSAGE);
        if (ArrayUtils.contains(v1Cols, V1COL_DEVICE))
            l.add(Post.COL_DEVICE);
        if (ArrayUtils.contains(v1Cols, V1COL_CAN_COMMENT))
            l.add(Post.COL_CAN_COMMENT);
        if (ArrayUtils.contains(v1Cols, V1COL_CAN_LIKE))
            l.add(Post.COL_CAN_LIKE);
        if (ArrayUtils.contains(v1Cols, V1COL_CAN_RESHARE))
            l.add(Post.COL_CAN_QUOTE);
        if (ArrayUtils.contains(v1Cols, V1COL_ADD_TO) || ArrayUtils.contains(v1Cols, V1COL_ADD_NEW_USERS)) {
            l.add(Post.COL_ADD_TO);
            l.add(Post.COL_ADD_TO_IDS);
        }
        if (ArrayUtils.contains(v1Cols, V1COL_PRIVATE) || ArrayUtils.contains(v1Cols, V1COL_SECRETLY))
            l.add(Post.COL_PRIVATE);
        if (ArrayUtils.contains(v1Cols, V1COL_ATTACHMENTS))
            l.add(Post.COL_ATTACHMENTS);
        if (ArrayUtils.contains(v1Cols, V1COL_DESTROYED_TIME))
            l.add(Post.COL_DESTROYED_TIME);
        // target
        if (ArrayUtils.contains(v1Cols, V1COL_LOCATION))
            l.add(Post.COL_LOCATION);
        // add_contact
        // has_contact
        if (ArrayUtils.contains(v1Cols, V1COL_LONGITUDE))
            l.add(Post.COL_LONGITUDE);
        if (ArrayUtils.contains(v1Cols, V1COL_LATITUDE))
            l.add(Post.COL_LATITUDE);
        if (ArrayUtils.contains(v1Cols, V1COL_COMMENTS))
            l.add(CommentPostExpansion.COL_COMMENTS);
        if (ArrayUtils.contains(v1Cols, V1COL_LIKED))
            l.add(LikePostExpansion.COL_LIKED);
        if (ArrayUtils.contains(v1Cols, V1COL_LIKES))
            l.add(LikePostExpansion.COL_LIKES);
        if (ArrayUtils.contains(v1Cols, V1COL_RETWEETED_STREAM))
            l.add(Post.COL_QUOTED_POST);
        if (ArrayUtils.contains(v1Cols, V1COL_ROOT_ID)) {
            l.add(Post.COL_ATTACHMENTS);
            l.add(Post.COL_ATTACHMENT_IDS);
        }
        if (ArrayUtils.contains(v1Cols, V1COL_ICON))
            l.add(Post.COL_TYPE_ICON_URL);

        return l.toArray(new String[l.size()]);
    }

    public static int v1ToV2Type(int v1Type) {
        int type = 0;
        if ((v1Type & V1POST_TEXT) != 0)
            type |= Post.POST_TEXT;
        if ((v1Type & V1POST_PHOTO) != 0)
            type |= Post.POST_PHOTO;
        if ((v1Type & V1POST_VIDEO) != 0)
            type |= Post.POST_VIDEO;
        if ((v1Type & V1POST_AUDIO) != 0)
            type |= Post.POST_AUDIO;
        if ((v1Type & V1POST_BOOK) != 0)
            type |= Post.POST_BOOK;
        if ((v1Type & V1POST_APK) != 0)
            type |= Post.POST_APK;
        if ((v1Type & V1POST_LINK) != 0)
            type |= Post.POST_LINK;
        if ((v1Type & V1POST_APK_LINK) != 0)
            type |= Post.POST_APK_LINK;
        if ((v1Type & V1POST_TEXT) != 0)
            type |= Post.POST_TEXT;
        if ((v1Type & V1POST_APK_COMMENT) != 0)
            type |= (Post.POST_APK | Post.POST_COMMENT_BROADCAST);
        if ((v1Type & V1POST_APK_LIKE) != 0)
            type |= (Post.POST_APK | Post.POST_LIKE_BROADCAST);
        if ((v1Type & V1POST_BOOK_LIKE) != 0)
            type |= (Post.POST_BOOK | Post.POST_LIKE_BROADCAST);
        if ((v1Type & V1POST_BOOK_COMMENT) != 0)
            type |= (Post.POST_BOOK | Post.POST_COMMENT_BROADCAST);
        if ((v1Type & V1POST_FRIEND_SET) != 0)
            type |= Post.POST_SYSTEM;
        if ((v1Type & V1POST_MUSIC) != 0)
            type |= Post.POST_MUSIC;
        if ((v1Type & V1POST_MUSIC_COMMENT) != 0)
            type |= (Post.POST_MUSIC | Post.POST_COMMENT_BROADCAST);
        if ((v1Type & V1POST_MUSIC_LIKE) != 0)
            type |= (Post.POST_MUSIC | Post.POST_LIKE_BROADCAST);
        if ((v1Type & V1POST_SIGN_IN) != 0)
            type |= Post.POST_SIGN_IN;

        return type;
    }

    public static int v2ToV1Type(int type) {
        int v1Type = 0;
        if ((type & Post.POST_TEXT) != 0)
            v1Type |= V1POST_TEXT;
        if ((type & Post.POST_PHOTO) != 0)
            v1Type |= V1POST_PHOTO;
        if ((type & Post.POST_AUDIO) != 0)
            v1Type |= V1POST_AUDIO;
        if ((type & Post.POST_BOOK) != 0)
            v1Type |= V1POST_BOOK;
        if ((type & Post.POST_APK) != 0)
            v1Type |= V1POST_APK;
        if ((type & Post.POST_LINK) != 0)
            v1Type |= V1POST_LINK;
        if ((type & Post.POST_APK_LINK) != 0)
            v1Type |= V1POST_APK_LINK;
        if ((type & (Post.POST_APK | Post.POST_COMMENT_BROADCAST)) == (Post.POST_APK | Post.POST_COMMENT_BROADCAST))
            v1Type |= V1POST_APK_COMMENT;
        if ((type & (Post.POST_APK | Post.POST_LIKE_BROADCAST)) == (Post.POST_APK | Post.POST_LIKE_BROADCAST))
            v1Type |= V1POST_APK_LIKE;
        if ((type & (Post.POST_BOOK | Post.POST_LIKE_BROADCAST)) == (Post.POST_BOOK | Post.POST_LIKE_BROADCAST))
            v1Type |= V1POST_BOOK_LIKE;
        if ((type & (Post.POST_BOOK | Post.POST_COMMENT_BROADCAST)) == (Post.POST_BOOK | Post.POST_COMMENT_BROADCAST))
            v1Type |= V1POST_BOOK_COMMENT;
        if ((type & Post.POST_SYSTEM) != 0)
            v1Type |= V1POST_FRIEND_SET;
        if ((type & Post.POST_MUSIC) != 0)
            v1Type |= V1POST_MUSIC;
        if ((type & (Post.POST_MUSIC | Post.POST_COMMENT_BROADCAST)) == (Post.POST_MUSIC | Post.POST_COMMENT_BROADCAST))
            v1Type |= V1POST_MUSIC_COMMENT;
        if ((type & (Post.POST_MUSIC | Post.POST_LIKE_BROADCAST)) == (Post.POST_MUSIC | Post.POST_LIKE_BROADCAST))
            v1Type |= V1POST_MUSIC_LIKE;
        if ((type & Post.POST_SIGN_IN) != 0)
            v1Type |= V1POST_SIGN_IN;

        return v1Type;
    }

    public static String postToJson(final Post post, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializePost(jg, post, v1Cols);
            }
        }, human);
    }

    public static String postsToJson(final Posts posts, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializePosts(jg, posts, v1Cols);
            }
        }, human);
    }

    public static void serializePost(JsonGenerator jg, Post post, String[] v1Cols) throws IOException {
        jg.writeStartObject();
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_POST_ID))
            jg.writeNumberField(V1COL_POST_ID, post.getPostId());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_SOURCE))
            jg.writeNumberField(V1COL_SOURCE, post.getSourceId());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_FROM)) {
            post.writeAddonJsonAs(jg, Post.COL_SOURCE, V1COL_FROM, USER_TRANSFORMER);
        }
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CREATED_TIME))
            jg.writeNumberField(V1COL_CREATED_TIME, post.getCreatedTime());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_UPDATED_TIME))
            jg.writeNumberField(V1COL_UPDATED_TIME, post.getUpdatedTime());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_QUOTE))
            jg.writeNumberField(V1COL_QUOTE, post.getQuote());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ROOT)) // root = quote
            jg.writeNumberField(V1COL_ROOT, post.getQuote());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_APP))
            jg.writeNumberField(V1COL_APP, post.getApp());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_TYPE))
            jg.writeNumberField(V1COL_TYPE, v2ToV1Type(post.getType()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_APP_DATA))
            jg.writeStringField(V1COL_APP_DATA, ObjectUtils.toString(post.getAppData()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_MESSAGE))
            jg.writeStringField(V1COL_MESSAGE, ObjectUtils.toString(post.getMessage()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_DEVICE))
            jg.writeStringField(V1COL_DEVICE, ObjectUtils.toString(post.getDevice()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CAN_COMMENT))
            jg.writeBooleanField(V1COL_CAN_COMMENT, BooleanUtils.isTrue(post.getCanComment()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CAN_LIKE))
            jg.writeBooleanField(V1COL_CAN_LIKE, BooleanUtils.isTrue(post.getCanLike()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CAN_RESHARE))
            jg.writeBooleanField(V1COL_CAN_RESHARE, BooleanUtils.isTrue(post.getCanQuote()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ADD_TO))
            jg.writeStringField(V1COL_ADD_TO, post.getAddTo() != null ? post.getAddTo().toString() : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_PRIVATE))
            jg.writeBooleanField(V1COL_PRIVATE, BooleanUtils.isTrue(post.getPrivate()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_SECRETLY))
            jg.writeStringField(V1COL_SECRETLY, Boolean.toString(BooleanUtils.isTrue(post.getPrivate())));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ATTACHMENTS))
            serializeAttachment(jg, post.getType(), post.getAttachments());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_MENTIONS))
            jg.writeStringField(V1COL_MENTIONS, post.getTo() != null ? post.getTo().toString() : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_DESTROYED_TIME))
            jg.writeNumberField(V1COL_DESTROYED_TIME, post.getDestroyedTime());

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_TARGET)) {
            if (post.getType() == Post.POST_LINK)
                serializeTarget(jg, post.getAttachments());
            else
                jg.writeStringField(V1COL_TARGET, "");
        }

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LOCATION))
            jg.writeStringField(V1COL_LOCATION, ObjectUtils.toString(post.getLocation()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ADD_CONTACT))
            jg.writeStringField(V1COL_ADD_CONTACT, "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_HAS_CONTACT))
            jg.writeNumberField(V1COL_HAS_CONTACT, 0);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LATITUDE))
            jg.writeNumberField(V1COL_LATITUDE, post.getGeoLocation() != null ? post.getGeoLocation().latitude : 0.0);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LONGITUDE))
            jg.writeNumberField(V1COL_LONGITUDE, post.getGeoLocation() != null ? post.getGeoLocation().longitude : 0.0);

        // addons
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_TO))
            post.writeAddonJsonAs(jg, Post.COL_TO, V1COL_TO, USER_TRANSFORMER);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ADD_NEW_USERS))
            post.writeAddonJsonAs(jg, Post.COL_ADD_TO, V1COL_ADD_NEW_USERS, USER_TRANSFORMER);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_COMMENTS))
            post.writeAddonJsonAs(jg, CommentPostExpansion.COL_COMMENTS, V1COL_COMMENTS, CompatibleComment.COMMENTS_ADDON_TRANSFORMER);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LIKED))
            post.writeAddonJsonAs(jg, LikePostExpansion.COL_LIKED, V1COL_LIKED);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LIKES))
            post.writeAddonJsonAs(jg, LikePostExpansion.COL_LIKES, V1COL_LIKES, CompatibleLike.LIKES_ADDON_TRANSFORMER);

        if (post.getQuote() > 0) {
            if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_RETWEETED_STREAM))
                post.writeAddonJsonAs(jg, Post.COL_QUOTED_POST, V1COL_RETWEETED_STREAM, RETWEETED_POST_TRANSFORMER);
        }

        // TODO: reshare count
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_RESHARE_COUNT))
            jg.writeNumberField(V1COL_RESHARE_COUNT, 0);

        // root id
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ROOT_ID)) {
            if (v2ToV1Type(post.getType()) == V1POST_APK)
                jg.writeStringField(V1COL_ROOT_ID, post.getAttachmentIds() != null ? StringUtils.join(post.getAttachmentIds(), ",") : "");
            else
                jg.writeStringField(V1COL_ROOT_ID, "");
        }

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ICON))
            post.writeAddonJsonAs(jg, Post.COL_TYPE_ICON_URL, V1COL_ICON);

        jg.writeEndObject();
    }

    private static final PostJsonTransformer RETWEETED_POST_TRANSFORMER = new PostJsonTransformer(V1_FULL_COLUMNS);

    private static final String[] USER_V1_COLUMNS = {
            CompatibleUser.V1COL_USER_ID,
            CompatibleUser.V1COL_DISPLAY_NAME,
            CompatibleUser.V1COL_IMAGE_URL,
    };

    private static final CompatibleUser.UserJsonTransformer USER_TRANSFORMER = new CompatibleUser.UserJsonTransformer(USER_V1_COLUMNS);

    private static void serializeAttachment(JsonGenerator jg, int type, String attachment) throws IOException {
        jg.writeFieldName(V1COL_ATTACHMENTS);
        if (StringUtils.isBlank(attachment)) {
            jg.writeRawValue("[]");
        } else {
            JsonNode jn = null;
            try {
                jn = JsonHelper.parse(attachment);
            } catch (Exception ignored) {
            }
            if (jn != null) {
                if (!jn.isArray()) {
                    ArrayNode arr = JsonNodeFactory.instance.arrayNode();
                    arr.add(jn);
                    jn = arr;
                }
                jn = transformAttachments(type, jn);
                jg.writeTree(jn);
            } else {
                jg.writeRawValue("[]");
            }
        }
    }

    private static JsonNode transformAttachments(int type, JsonNode jn) {
        if (type == Post.POST_LINK) {
            LinkEntities les = LinkEntities.fromJsonNode(jn);
            String json = CompatibleLinkEntity.linksToJson(les, false);
            return JsonHelper.parse(json);
        }else if(type == Post.POST_PHOTO){
            Photos photos = Photos.fromJsonNode(null,jn);
            String json = CompatiblePhoto.photosToJson(photos, CompatiblePhoto.V1_FULL_COLUMNS, false);
            return JsonHelper.parse(json);
            
        } else {
            // other post type
            return jn;
        }
    }


    private static void serializeTarget(JsonGenerator jg, String attachment) throws IOException {
        if (StringUtils.isNotEmpty(attachment)) {
            LinkEntities les = LinkEntities.fromJson(attachment);
            LinkEntity le = CollectionUtils.isNotEmpty(les) ? les.get(0) : null;
            jg.writeStringField(V1COL_TARGET, le != null ? le.getUrl() : "");
        } else {
            jg.writeStringField(V1COL_TARGET, "");
        }
    }


    public static void serializePosts(JsonGenerator jg, Posts posts, final String[] v1Cols) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(posts)) {
            for (Post post : posts) {
                if (post != null)
                    serializePost(jg, post, v1Cols);
            }
        }
        jg.writeEndArray();
    }

    public static long timestampToId(long timestamp) {
        return RandomHelper.timestampToId(timestamp);
    }

    public static String v2JsonNodeToV1Json(JsonNode jn, String[] v1Cols, boolean human) {
        if (jn.isArray()) {
            final Posts posts = Posts.fromJsonNode(null, jn);
            return postsToJson(posts, v1Cols, human);
        } else if (jn.isObject()) {
            final Post post = Post.fromJsonNode(jn);
            return postToJson(post, v1Cols, human);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static String v2ToV1Json(String json, String[] v1Cols, boolean human) {
        JsonNode jn = JsonHelper.parse(json);
        return v2JsonNodeToV1Json(jn, v1Cols, human);
    }

    public static class PostJsonTransformer implements Addons.AddonValueTransformer {

        public final String[] postV1Columns;

        public PostJsonTransformer(String[] postV1Columns) {
            this.postV1Columns = postV1Columns;
        }

        @Override
        public Object transform(Object old) {
            String json = ObjectUtils.toString(old);
            return Addons.jsonAddonValue(CompatiblePost.v2ToV1Json(json, postV1Columns, true));
        }
    }
}
