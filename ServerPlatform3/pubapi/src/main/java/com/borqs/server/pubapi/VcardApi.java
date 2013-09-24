package com.borqs.server.pubapi;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.privacy.*;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class VcardApi extends PublicApiSupport {
    private AccountLogic account;
    private PrivacyControlLogic privacyControl;
    private FriendLogic friend;

    public VcardApi() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public PrivacyControlLogic getPrivacyControl() {
        return privacyControl;
    }

    public void setPrivacyControl(PrivacyControlLogic privacyControl) {
        this.privacyControl = privacyControl;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    /**
     * 允许一些用户查看自己的vcard
     *
     * @group Vcard
     * @http-param targets 逗号分隔的BORQS用户ID
     * @http-param ret:false 此方法是否返回设置过的用户列表
     * @http-param retcols:user_id,display_name,nickname,photo,allowed 如果ret为true，此参数为需要返回的User列名
     * @http-return true 或者 返回的用户列表
     * @http-example {
     *     "result":true
     * }
     */
    @Route(url = "/vcard/allow")
    public void allowVcardPrivacy(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        setVcardPrivacy(req, resp, ctx, true);
    }

    /**
     * 禁止一些用户查看自己的vcard
     *
     * @group Vcard
     * @http-param targets 逗号分隔的BORQS用户ID
     * @http-param ret:false 此方法是否返回设置过的用户列表
     * @http-param retcols:user_id,display_name,nickname,photo,allowed 如果ret为true，此参数为需要返回的User列名
     * @http-return true 或者 返回的用户列表
     * @http-example {
     *     "result":true
     * }
     */
    @Route(url = "/vcard/deny")
    public void denyVcardPrivacy(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        setVcardPrivacy(req, resp, ctx, false);
    }

    /**
     * 将自己的Vcard可见性设置进行重置，使所有自己设置过的vcard信息都清空
     *
     * @group Vcard
     * @http-return true
     * @http-example {
     *     "result":true
     * }
     */
    @Route(url = "/vcard/clear")
    public void clearVcardPrivacy(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        privacyControl.clearPrivacy(ctx, PrivacyResources.RES_VCARD);
        resp.body(true);
    }


    private static final String[] RETURN_USER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
            //RelUserExpansion.COL_REMARK,
            PrivacyControlUserExpansion.COL_ALLOWED,
    };

    private void setVcardPrivacy(Request req, Response resp, Context ctx, boolean allow) {
        long[] targetUserIds = req.checkLongArray("targets", ",");
        targetUserIds = account.getExistsIds(ctx, targetUserIds);

        PrivacyEntry[] pes = PrivacyEntry.forUserTargets(ctx.getViewer(), PrivacyResources.RES_VCARD, targetUserIds, allow);
        privacyControl.setPrivacy(ctx, pes);

        if (req.getBoolean("ret", false)) {
           returnUsers(req, resp, ctx, targetUserIds);
        } else {
            resp.body(true);
        }
    }

    private void returnUsers(Request req, Response resp, Context ctx, long[] targetUserIds) {
        String[] returnCols = User.expandColumns(req.getStringArray("retcols", ",", RETURN_USER_COLUMNS));
        Users returnUsers = account.getUsers(ctx, returnCols, targetUserIds);
        resp.body(RawText.of(returnUsers.toJson(returnCols, true)));
    }

    private void returnUser(Request req, Response resp, Context ctx, long targetUserId) {
        String[] returnCols = User.expandColumns(req.getStringArray("retcols", ",", RETURN_USER_COLUMNS));
        User returnUser = account.getUser(ctx, returnCols, targetUserId);
        if (returnUser == null)
            throw new ServerException(E.INVALID_USER, "Invalid user id " + targetUserId);
        resp.body(RawText.of(returnUser.toJson(returnCols, true)));
    }

    /**
     * 获得所有允许查看我名片的用户列表
     *
     * @group Vcard
     * @http-param cols:@std 逗号分隔的列名称，值为@std或者@full
     * @http-param in_friends:false 只返回我好友中的且允许查看我名片的用户
     * @http-param page:0 分页的页码，基于0
     * @http-param count:20 每页数量，最大50
     * @http-return 允许查看我名片的用户列表
     * @http-return @user_std.json
     */
    @Route(url = "/vcard/allowed")
    public void getAllowed(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        String[] cols = User.expandColumns(req.getStringArray("cols", ",", User.STANDARD_COLUMNS));
        AllowedIds allowed = privacyControl.getAllowIds(ctx, ctx.getViewer(), PrivacyResources.RES_VCARD);
        if (!allowed.isNormalMode())
            throw new ServerException(E.INVALID_PRIVACY_ALLOWED_MODE, "Invalid allowed");

        long[] allowedIds = ArrayHelper.removeElement(allowed.ids, ctx.getViewer());
        if (req.getBoolean("in_friends", false)) {
            long[] friendIds = friend.getBorqsFriendIds(ctx, ctx.getViewer());
            allowedIds = ArrayHelper.removeElements(allowedIds, friendIds);
        }

        Page page = req.getPage(20, 50);
        allowedIds = page.retains(allowedIds);

        Users users = account.getUsers(ctx, cols, allowedIds);
        resp.body(RawText.of(users.toJson(cols, true)));
    }


    // !!!!NOTE /vcard/exchange is NOT safe for 'target'

//    /**
//     * 登录用户与其他用户交换名片，交换后名片互相可见
//     *
//     * @http-param target BORQS用户ID
//     * @http-param ret:false 此方法是否返回设置过的用户列表
//     * @http-param retcols:user_id,display_name,nickname,photo,allowed 如果ret为true，此参数为需要返回的User列名
//     * @http-return true 或者 交换名片后目标用户信息
//     * @http-example {
//     *     "result":true
//     * }
//     */
//    @Route(url = "/vcard/exchange")
//    public void exchangeVcard(Request req, Response resp) {
//        Context ctx = checkContext(req, true);
//        long targetId = req.checkLong("target");
//        AccountHelper.checkUser(account, ctx, targetId);
//        privacyControl.mutualAllow(ctx, PrivacyResources.RES_VCARD, targetId);
//        if (req.getBoolean("ret", false)) {
//            returnUser(req, resp, ctx, targetId);
//        } else {
//            resp.body(true);
//        }
//    }
}
