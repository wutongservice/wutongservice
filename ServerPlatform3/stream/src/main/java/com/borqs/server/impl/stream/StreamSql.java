package com.borqs.server.impl.stream;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostFilter;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import static com.borqs.server.platform.sql.Sql.value;
import static com.borqs.server.platform.sql.Sql.valueIf;


public class StreamSql {

    public static String saveStream(String table, Post post) {
        return new Sql().insertInto(table).values(
                value("post_id", post.getPostId()),
                value("source", post.getSourceId()),
                value("updated_time", post.getUpdatedTime()),
                value("created_time", post.getCreatedTime()),
                value("destroyed_time", 0),
                value("quote", post.getQuote()),
                value("`to`", StringHelper.join(post.getTo().getIds(PeopleId.USER), ",")),
                value("add_to", StringHelper.join(post.getAddTo().getIds(PeopleId.USER), ",")),
                value("app", post.getApp()),
                value("type", post.getType()),
                value("message", ObjectUtils.toString(post.getMessage())),
                value("app_data", post.getAppData()),
                value("attachments", ObjectUtils.toString(post.getAttachments())),
                value("attachment_ids", ArrayUtils.isNotEmpty(post.getAttachmentIds()) ? JsonHelper.toJson(post.getAttachmentIds(), false) : "[]"),
                value("device", post.getDevice()),
                value("can_comment", post.getCanComment()),
                value("can_like", post.getCanLike()),
                value("can_quote", post.getCanQuote()),
                value("private", post.getPrivate()),
                value("location", ObjectUtils.toString(post.getLocation())),
                value("latitude", post.getLatitude()),
                value("longitude", post.getLongitude())
        ).toString();
    }

    public static String destroyedPosts(Context ctx, String table, long viewId, long... postIds) {
        Sql sql = new Sql().update(table).setValues(
                value("destroyed_time", DateHelper.nowMillis())
        ).where(" 1=1 ");

        if (ctx.isInternal()) {
            // internal ways can destroy any posts
             if (postIds.length == 1)
                sql.and("`post_id`=:post_id", "post_id", postIds[0]);
            else
                sql.and("`post_id` IN ($post_id)", "post_id", StringHelper.join(postIds, ","));
        } else {

            if (postIds.length == 1)
                sql.and("`post_id`=:post_id", "post_id", postIds[0]).and("`source`=:source", "source", viewId);
            else
                sql.and("`post_id` IN ($post_id)", "post_id", StringHelper.join(postIds, ",")).and("`source`=:source", "source", viewId);
        }
        return sql.toString();
    }

    public static String updatePost(String table, Post post) {
        return new Sql()
                .update(table)
                .setValues(
                        /*valueIf("source", post.getSourceId(), post.getSourceId() > 0),
                        valueIf("updated_time", post.getUpdatedTime(), post.getUpdatedTime() > 0),
                        valueIf("quote", post.getQuote(), post.getQuoteIds() > 0),
                        valueIf("to", post.getTo() != null ? post.getTo().toString() : "" , post.getTo() != null),
                        valueIf("add_to", post.getAddTo() != null ? post.getAddTo().toString() : "", post.getAddTo() != null),
                        valueIf("app", post.getApp(), post.getApp() > 0),
                        valueIf("type", post.getType(), post.getType() > 0),

                        valueIf("app_data", post.getAppData(), StringUtils.isNotEmpty(post.getAppData())),
                        valueIf("attachments", post.getAttachments(), StringUtils.isNotEmpty(post.getAttachments())),
                        valueIf("device", post.getDevice(), StringUtils.isNotEmpty(post.getDevice())), */
                        valueIf("message", post.getMessage(), post.getMessage() != null),
                        value("updated_time", DateHelper.nowMillis()),
                        valueIf("can_comment", post.getCanComment(), post.getCanComment() != null),
                        valueIf("attachments", post.getAttachments(), StringUtils.isNotEmpty(post.getAttachments())),
                        valueIf("can_quote", post.getCanQuote(), post.getCanQuote() != null),
                        valueIf("can_like", post.getCanLike(), post.getCanLike() != null),
                        valueIf("attachment_ids",ArrayUtils.isNotEmpty(post.getAttachmentIds()) ? JsonHelper.toJson(post.getAttachmentIds(), false) : "[]", post.getAttachmentIds() != null)
                        /*valueIf("private", post.getPrivate(), post.getPrivate() != null),
                        valueIf("latitude", post.getLatitude(), StringUtils.isNotEmpty(post.getLatitude())),
                        valueIf("longitude", post.getLongitude(), StringUtils.isNotEmpty(post.getLongitude()))*/
                ).where("post_id=:post_id AND destroyed_time=0", "post_id", post.getPostId()).and("`source`=:source", "source", post.getSourceId())
                .toString();
    }

    public static String hasPost(String table, long postId) {
        return hasAllPosts(table, postId);
    }

    public static String hasAllPosts(String table, long... postIds) {
        Sql sql = new Sql().select("count(post_id)").from(table).where("destroyed_time = 0");
        if (postIds.length == 1)
            sql.and("`post_id`=:post_id", "post_id", postIds[0]);
        else
            sql.and("`post_id` IN ($post_id)", "post_id", StringHelper.join(postIds, ",")).toString();

        return sql.toString();
    }

    public static String hasAnyPosts(String table, long... postIds) {
        Sql sql = new Sql().select("count(post_id)").from(table).where("destroyed_time = 0");
        if (postIds.length == 1)
            sql.and("`post_id`=:post_id", "post_id", postIds[0]);
        else
            sql.and("`post_id` IN ($post_id)", "post_id", StringHelper.join(postIds, ",")).toString();

        return sql.toString();
    }

    public static String getPosts(String table, long... postIds) {
        Sql sql = new Sql().select("*").from(table).where("destroyed_time = 0");
        if (postIds.length == 1)
            sql.and("`post_id`=:post_id", "post_id", postIds[0]);
        else
            sql.and("`post_id` IN ($post_id)", "post_id", StringHelper.join(postIds, ",")).toString();

        return sql.toString();
    }

    public static String getPostIds(String table) {
        Sql sql = new Sql().select(" * ").from(table).where("destroyed_time = 0");
        return sql.toString();
    }

    public static String getPublicTimeline(Context ctx, String table, PostFilter filter, Page page) {
        Sql sql = new Sql()
                .select("post_id")
                .from(table)
                .where("destroyed_time=0");

        if (ctx.isLogined()) {
            // login
            sql.and("private=0"); // TODO: to, add, private
        } else {
            // not login
            sql.and("private=0");
        }
        sql.where("post_id >= :min AND post_id < :max", "min", filter.min, "max", filter.max);

        sql.orderBy("post_id", "DESC").page(page);
        return sql.toString();
    }
}
