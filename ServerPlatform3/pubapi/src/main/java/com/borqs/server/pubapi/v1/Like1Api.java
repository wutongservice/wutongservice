package com.borqs.server.pubapi.v1;



import com.borqs.server.compatible.CompatibleTarget;
import com.borqs.server.compatible.CompatibleUser;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.like.LikeLogic;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;

@IgnoreDocument
public class Like1Api extends PublicApiSupport {
    private AccountLogic account;
    private LikeLogic like;

    public Like1Api() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public LikeLogic getLike() {
        return like;
    }

    public void setLike(LikeLogic like) {
        this.like = like;
    }

    private static Target getLikeTarget(Request req) {
        String target = req.checkString("target");
        if(target.contains(":")){
            target = target.substring(target.lastIndexOf(":")+1,target.length());
        }
        return new Target(CompatibleTarget.v1ToV2Type(req.getInt("object", CompatibleTarget.V1_POST)), target);
    }

    @Route(url = "/like/like")
    public void like(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        Target likeTarget = getLikeTarget(req);
        like.like(ctx, likeTarget);
        resp.body(true);
    }

    @Route(url = "/like/unlike")
    public void unlike(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        Target likeTarget = getLikeTarget(req);
        like.unlike(ctx, likeTarget);
        resp.body(true);
    }

    @Route(url = "/like/count")
    public void getLikeCount(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        Target likeTarget = getLikeTarget(req);
        long n = like.getLikeCount(ctx, likeTarget);
        resp.body(n);
    }

    @Route(url = "/like/ifliked")
    public void liked(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        Target likeTarget = getLikeTarget(req);
        boolean b = like.isLiked(ctx, ctx.getViewer(), likeTarget);
        resp.body(b);
    }

    @Route(url = "/like/users")
    public void getLikedUsers(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        Target likeTarget = getLikeTarget(req);
        long[] userIds = like.getLikedUsers(ctx, likeTarget, req.getPage(20, 100));
        String[] userV1Cols =
                CompatibleUser.expandV1Columns(
                        req.getStringArrayExcept("columns", ",", CompatibleUser.V1_LIGHT_COLUMNS,
                                new String[]{CompatibleUser.V1COL_PASSWORD, CompatibleUser.V1COL_DESTROYED_TIME}));

        Users users = account.getUsers(ctx, CompatibleUser.v1ToV2Columns(userV1Cols), userIds);
        resp.body(RawText.of(CompatibleUser.usersToJson(users, userV1Cols, true)));
    }
}
