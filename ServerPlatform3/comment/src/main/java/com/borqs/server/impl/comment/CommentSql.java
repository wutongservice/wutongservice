package com.borqs.server.impl.comment;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.StringHelper;

import static com.borqs.server.platform.sql.Sql.value;
import static com.borqs.server.platform.sql.Sql.valueIf;


public class CommentSql {

    public static String saveComment(String table, Comment comment) {
        return new Sql().insertInto(table).values(
                value("comment_id", comment.getCommentId()),
                value("can_like", comment.getCanLike()),
                value("commenter", comment.getCommenterId()),
                value("created_time", comment.getCreatedTime()),
                value("destroyed_time", 0),
                value("device", comment.getDevice()),
                value("message", comment.getMessage()),
                value("target_id", comment.getTarget().id),
                value("target_type", comment.getTarget().type),
                value("add_to", comment.getAddTo() != null ? StringHelper.join(comment.getAddTo().getIds(PeopleId.USER), ","):"")
        ).toString();
    }

    public static String saveCommentTarget(String table, Comment comment) {
        return new Sql().insertInto(table).values(
                value("comment_id", comment.getCommentId()),
                value("target_id", comment.getTarget().id),
                value("target_type", comment.getTarget().type)
        ).toString();
    }

    public static String disableComments(Context ctx, String table, long now, long... comment) {
        Sql sql = new Sql().update(table).setValues(
                value("destroyed_time", now)
        );

        if (comment.length == 1)
            sql.where("`comment_id`=:comment_id", "comment_id", comment[0]);
        else
            sql.where("`comment_id` IN ($comment_id)", "comment_id", StringHelper.join(comment, ","));

        return sql.toString();
    }

    public static String updateComment(String table, Comment comment) {
        return new Sql()
                .update(table)
                .setValues(
                        valueIf("target_id", comment.getTarget().id, comment.getTarget().id != null),
                        valueIf("target_type", comment.getTarget().type, comment.getTarget().type > 0),
                        valueIf("destroyed_time", comment.getDestroyedTime(), comment.getDestroyedTime() > 0),
                        valueIf("commenter", comment.getCommenterId(), comment.getCommenterId() > 0),
                        valueIf("message", comment.getMessage(), comment.getMessage() != null),
                        valueIf("can_like", comment.getCanLike(),true)
                ).where("comment_id=:comment_id AND destroyed_time=0", "comment_id", comment.getCommentId()).and(" commenter=:commenter","commenter",comment.getCommenterId())
                .toString();
    }

    public static String updateCommentTarget(String table, Comment comment) {
        return new Sql()
                .update(table)
                .setValues(
                        valueIf("target_id", comment.getTarget().id, comment.getTarget().id != null),
                        valueIf("target_type", comment.getTarget().type, comment.getTarget().type > 0)
                ).where("comment_id=:comment_id", "comment_id", comment.getCommentId())
                .toString();
    }

    public static String getCommentCount(String table, Target targets) {

        return new Sql()
                .select("count(comment_id) ")
                .from(table)
                .where(" 1=1  ")
                .and("target_id=:target_id", "target_id", targets.id)
                .and("target_type=:target_type", "target_type", targets.type)
                .toString();

    }

    public static String getCommentsOnTarget(String table, Target target,Page page) {
        return new Sql()
                .select("* ")
                .from(table)
                .where(" destroyed_time = 0 ")
                .and("target_id=:target_id", "target_id", target.id)
                .and("target_type=:target_type", "target_type", target.type).orderBy("created_time","DESC").page(page)
                .toString();
    }

    public static String getComments(String table, long... commentIds) {
        if (commentIds.length == 1) {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("comment_id =:comment_ids and destroyed_time = 0 ", "comment_ids", commentIds[0]).orderBy("created_time","DESC")
                    .toString();
        } else {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("comment_id IN ($comment_ids)  and destroyed_time = 0 ", "comment_ids", StringHelper.join(commentIds, ",")).orderBy("created_time","DESC")
                    .toString();
        }
    }

    public static String getTargetIdsOrderByCommentCount(String table, int targetType, boolean asc, Page page) {
        //  select target_id, count(*) as comment_count from test_account3.comment_target_index where target_type=2 group by target_id order by comment_count desc limit 0,2
        return new Sql()
                .select("target_id", Sql.field("COUNT(*)", "comment_count"))
                .from(table)
                .where("target_type=:target_type", "target_type", targetType)
                .groupBy("target_id")
                .orderBy("comment_count", asc ? "ASC" : "DESC")
                .page(page)
                .toString();
    }
}
