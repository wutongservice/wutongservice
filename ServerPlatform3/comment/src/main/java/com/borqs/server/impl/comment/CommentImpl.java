package com.borqs.server.impl.comment;

import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.expansion.ExpansionHelper;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.comment.*;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.like.LikeLogic;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommentImpl implements CommentLogic {
    private static final Logger L = Logger.get(CommentImpl.class);

    // db
    private final CommentDb db = new CommentDb();

    // dep
    private ConversationLogic conversation;
    private LikeLogic like;
    private AccountLogic account;

    // expansions
    private final BuiltinExpansion builtinExpansion = new BuiltinExpansion();
    private List<CommentExpansion> expansions;

    // hooks
    private List<CommentHook> createCommentHook;
    private List<CommentHook> updateCommentHook;
    private List<CommentHook> destroyCommentHook;

    private static final String[] SOURCE_COLUMNS = {
            User.COL_USER_ID,
            User.COL_NAME,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };

    private static final String[] TO_COLUMNS = {
            User.COL_USER_ID,
            User.COL_NAME,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
    };

    private static final String[] ADDTO_COLUMNS = {
            User.COL_USER_ID,
            User.COL_NAME,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
    };

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getCommentTable() {
        return db.getCommentTable();
    }

    public void setCommentTable(Table commentTable) {
        db.setCommentTable(commentTable);
    }

    public void setCommentTargetTable(Table commentTargetTable) {
        db.setCommentTargetTable(commentTargetTable);
    }

    public List<CommentExpansion> getExpansions() {
        return expansions;
    }

    public void setExpansions(List<CommentExpansion> expansions) {
        this.expansions = expansions;
    }

    public ConversationLogic getConversation() {
        return conversation;
    }

    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }

    public LikeLogic getLike() {
        return like;
    }

    public void setLike(LikeLogic like) {
        this.like = like;
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public List<CommentHook> getCreateCommentHook() {
        return createCommentHook;
    }

    public void setCreateCommentHook(List<CommentHook> createCommentHook) {
        this.createCommentHook = createCommentHook;
    }

    public List<CommentHook> getUpdateCommentHook() {
        return updateCommentHook;
    }

    public void setUpdateCommentHook(List<CommentHook> updateCommentHook) {
        this.updateCommentHook = updateCommentHook;
    }

    public List<CommentHook> getDestroyCommentHook() {
        return destroyCommentHook;
    }

    public void setDestroyCommentHook(List<CommentHook> destroyCommentHook) {
        this.destroyCommentHook = destroyCommentHook;
    }

    @Override
    public Comment createComment(Context ctx, Comment comment) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "createComment",
                ctx, "comment", comment);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("comment", comment);

            HookHelper.before(createCommentHook, ctx, comment);
            if(ctx.hasSession("return") && (Boolean)ctx.getSession("return","false")){
                throw new ServerException(E.CANNOT_COMMENT,"duplicate comment message!");
            }
            Comment commentRes = db.createComment(ctx, comment);
            OpLine.append2(ctx,
                    Actions.CREATE, comment.toJson(null, false), Target.forComment(comment.getCommentId()),
                    Actions.COMMENT, comment.getCommenterId(), comment.getTarget());
            if (comment.getTarget().type != Target.APK) {
                conversation.create(ctx, new ConversationBase(comment.getTarget(), Actions.COMMENT));
            } else {
                // for APK , should split the package name without version
                String apkId = comment.getTarget().id;
                String[] o = apkId.split("-");
                String t = "";
                if (o.length == 3 || o.length == 1)
                    t = o[0];
                if (t.length() > 0){
                    conversation.create(ctx, new ConversationBase(Target.forApk(t), Actions.COMMENT));
                }
            }
            HookHelper.after(createCommentHook, ctx, comment);


            //TODO: opline
            LC.endCall();
            return commentRes;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public boolean destroyComment(Context ctx, long commentId) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "destroyComments",
                ctx, "commentId", commentId);

        try {
            ParamChecker.notNull("ctx", ctx);

            Comment comment = getComment(ctx, Comment.FULL_COLUMNS, commentId);
            boolean b = false;
            if (comment != null)
                b = ctx.isInternal() || ctx.getViewer() == comment.getCommenterId();

            if (b) {
                HookHelper.before(destroyCommentHook, ctx, comment);
                b = db.destroyComments(ctx, commentId);
                OpLine.append(ctx, Actions.DESTROY, commentId, Target.forComment(commentId));
                HookHelper.after(destroyCommentHook, ctx, comment);
                conversation.delete(ctx, new ConversationBase(comment.getTarget(), Actions.COMMENT));
            }
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean updateComment(Context ctx, Comment comment) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "updateComment", ctx, "comment", comment);

        try {
            ParamChecker.notNull("ctx", ctx);

            HookHelper.before(updateCommentHook, ctx, comment);
            boolean b = db.updateComment(ctx, comment);
            OpLine.append(ctx, Actions.UPDATE, comment.toJson(null, false), Target.forComment(comment.getCommentId()));
            HookHelper.after(updateCommentHook, ctx, comment);

            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Target, Integer> getCommentCounts(Context ctx, Target... targets) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "getCommentCounts", ctx, "targets", targets);

        try {
            ParamChecker.notNull("ctx", ctx);
            Map<Target, Integer> CommentCounts = db.getCommentCounts(ctx, targets);
            LC.endCall();
            return CommentCounts;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public int getCommentCount(Context ctx, Target target) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "getCommentCount", ctx, "target", target);

        try {
            ParamChecker.notNull("ctx", ctx);
            Map<Target, Integer> map = db.getCommentCounts(ctx, target);
            int count = map.get(target);
            LC.endCall();
            return count;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Target, Comment[]> getCommentsOnTarget(Context ctx, String[] expCols, Page page, Target... targets) {
        ParamChecker.notNull("ctx", ctx);
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "getCommentsOnTarget", ctx, "targets", targets);

        try {
            Map<Target, long[]> m = db.getCommentsOnTarget(ctx, page, targets);
            long[] commentIds = CollectionsHelper.getValuesUnionSet(m);
            Comments comments = getComments(ctx, expCols, commentIds);

            HashMap<Target, Comment[]> r = new HashMap<Target, Comment[]>();
            for (Map.Entry<Target, long[]> e : m.entrySet()) {
                Comments subComments = comments.getComments(null, e.getValue());
                r.put(e.getKey(), subComments.getCommentArray());
            }

            LC.endCall();
            return r;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }


    @Override
    public Comments getCommentsOnTarget(Context ctx, String[] expCols, Page page, Target target) {
        ParamChecker.notNull("ctx", ctx);
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "getCommentsOnTarget", ctx, "target", target);

        try {
            Map<Target, long[]> m = db.getCommentsOnTarget(ctx, page, target);
            long[] commentIds = m.get(target);
            if (commentIds == null)
                commentIds = new long[0];
            Comments comments = getComments(ctx, expCols, commentIds);
            LC.endCall();
            return comments;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Comments getComments(Context ctx, String[] expCols, long... commentIds) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "getComments", ctx, "commentIds", commentIds);

        try {
            ParamChecker.notNull("ctx", ctx);

            Comments comments = db.getComments(ctx, expCols, commentIds);
            expand(ctx, expCols, comments);
            LC.endCall();
            return comments;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Comment getComment(Context ctx, String[] expCols, long commentId) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "getComment", ctx, "commentId", commentId);
        try {
            Comments comments = getComments(ctx, expCols, commentId);
            Comment comment = CollectionUtils.isNotEmpty(comments) ? comments.get(0) : null;
            LC.endCall();
            return comment;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public long[] getUsersOnTarget(Context ctx, Target target, Page page) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "getComments", ctx, "target", target);

        try {
            Conversations convs = conversation.findByTarget(ctx, null, new int[]{Actions.COMMENT}, page, target);
            long[] userIds = convs.getUsers();
            LC.endCall();
            return userIds;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    // TODO: need optimize!
    @Override
    public String[] getTargetIdsOrderByCommentCount(Context ctx, int targetType, boolean asc, Page page) {
        final LogCall LC = LogCall.startCall(L, CommentImpl.class, "getTargetIdsOrderByCommentCount", ctx,
                "targetType", targetType, "asc", asc, "page", page);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("page", page);

            if (page.count < 0 || page.count <= 0 || page.count > 100)
                throw new ServerException(E.PARAM, "Illegal page");

            String[] targetIds = db.getTargetIdsOrderByCommentCount(ctx, targetType, asc, page);
            LC.endCall();
            return targetIds;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void expand(Context ctx, String[] expCols, Comments comments) {
        builtinExpansion.expand(ctx, expCols, comments);
        ExpansionHelper.expand(expansions, ctx, expCols, comments);
    }

    @Override
    public Comment expand(Context ctx, String[] expCols, Comment comment) {
        Comments comments = new Comments(comment);
        expand(ctx, expCols, comments);
        return comments.isEmpty() ? null : comments.get(0);
    }

    protected class BuiltinExpansion implements CommentExpansion {
        @Override
        public void expand(Context ctx, String[] expCols, Comments data) {
            if (CollectionUtils.isEmpty(data))
                return;

            if (expCols == null || ArrayUtils.contains(expCols, Comment.COL_COMMENTER))
                expandSource(ctx, data);

            if (expCols == null || ArrayUtils.contains(expCols, Comment.COL_ADD_TO))
                expandAddTo(ctx, data);
        }

        private void expandSource(Context ctx, Comments comments) {
            long[] sourceIds = comments.getSourceIds();
            Users users = account.getUsers(ctx, SOURCE_COLUMNS, sourceIds);
            for (Comment comment : comments) {
                if (comment == null)
                    continue;

                User sourceUser = users.getUser(comment.getCommenterId());
                String json = sourceUser != null ? sourceUser.toJson(SOURCE_COLUMNS, true) : "{}";
                comment.setAddon(Comment.COL_COMMENTER, Addons.jsonAddonValue(json));
            }
        }


        private void expandAddTo(Context ctx, Comments comments) {
            PeopleIds addTo = comments.getAddTo();
            Users users = account.getUsers(ctx, ADDTO_COLUMNS, addTo.getUserIds());
            for (Comment comment : comments) {
                if (comment == null)
                    continue;

                Users subUsers = users.getUsers(null, comment.getAddTo().getUserIds());
                String json = subUsers.toJson(ADDTO_COLUMNS, true);
                comment.setAddon(Comment.COL_ADD_TO, Addons.jsonAddonValue(json));
            }
        }
    }


}
