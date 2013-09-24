package com.borqs.server.pubapi;

import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.feature.privacy.PrivacyControlLogic;
import com.borqs.server.platform.feature.privacy.PrivacyEntry;
import com.borqs.server.platform.feature.privacy.PrivacyResources;
import com.borqs.server.platform.feature.privacy.PrivacyTarget;
import com.borqs.server.platform.feature.request.RequestUserExpansion;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;
import org.apache.commons.lang.StringUtils;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class FriendApi extends PublicApiSupport {

    private FriendLogic friend;
    private AccountLogic account;
    private PrivacyControlLogic privacyControl;

    public FriendApi() {
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
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

    /**
     * 创建一个属于个人的圈子
     *
     * @remark 每个人最多可以创建20个属于自己的圈子
     * @group Circle/Friend
     * @http-param name 创建圈子的名称，不可以为空字符串
     * @http-return 创建成功的圈子的ID
     * @http-example {
     * "result":101
     * }
     */
    @Route(url = "/circle/create")
    public void createCircle(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        Circle circle = friend.createCustomCircle(ctx, req.checkString("name"));
        resp.body(RawText.of(circle.toJson(Circle.CIRCLE_COLUMNS, true)));
    }

    /**
     * 删除一个自己创建的圈子
     *
     * @remark 如果这个圈子中有好友存在，那么这些好友也会随着被删除，但在其他圈子中的这些好友不会受到影响
     * @group Circle/Friend
     * @http-param circle 要删除的圈子ID，此ID不可以是内置的圈子ID，必须是用户自己创建的圈子ID
     * @http-return 是否成功删除了此圈子
     * @http-example {
     * "result":true
     * }
     */
    @Route(url = "/circle/destroy")
    public void destroyCircle(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        boolean b = friend.destroyCustomCircle(ctx, req.checkInt("circle"));
        resp.body(b);
    }

    /**
     * 获取某个人自己的圈子信息
     *
     * @remark 如果要返回的圈子ID不存在，那么返回结果中则不包含此圈子信息
     * @group Circle/Friend
     * @http-param circles: 逗号分隔的圈子ID列表，如果此参数缺失，那么则返回所有圈子
     * @http-param members|with_users:false 在返回圈子信息的时候是否返回圈子中成员ID
     * @http-return 圈子列表
     * @http-example @circle.json
     */
    @Route(url = "/circle/show")
    public void getCircles(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        boolean withMembers = req.getBoolean("members", false);
        Circles circles = friend.getCircles(ctx, ctx.getViewer(),
                req.getIntArray("circles", ",", null),
                withMembers);
        resp.body(RawText.of(circles.toJson(withMembers ? Circle.CIRCLE_COLUMNS_WITH_MEMBERS : Circle.CIRCLE_COLUMNS, true)));
    }

    /**
     * 用户更新自己的自定义圈子的名称
     *
     * @remark 内置圈子无法被更名，只有自定义圈子可以被更名
     * @group Circle/Friend
     * @http-param circle 自定义圈子ID
     * @http-param name 圈子的新名称，不能为空字符串
     * @http-return 成功为true
     * @http-example {
     * "result":true
     * }
     */
    @Route(url = "/circle/update")
    public void updateCircleName(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        boolean b = friend.updateCustomCircleName(ctx, req.checkInt("circle"), req.checkString("name"));
        resp.body(b);
    }

    /**
     * 获取某个用户的粉丝列表
     *
     * @remark 如果此粉丝不存在，那么返回结果中不包含此粉丝的信息
     * @group Circle/Friend
     * @http-param user 查询此用户的粉丝，如果此参数不指定，那么则查询当前登录用户的粉丝
     * @http-param cols:@std 获取粉丝的列名称
     * @http-param page:0 要查询的页码，基于0开始
     * @http-param count:20 要查询的每页数量，最大50
     * @http-return 粉丝列表，参见/user/show
     * @http-example @user_std.json
     */
    @Route(url = "/follower/show")
    public void showFollowers(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.getLong("user", ctx.getViewer());
        String[] cols = User.expandColumns(req.getStringArray("cols", ",", User.STANDARD_COLUMNS));
        Users users = friend.getFollowerUsers(ctx, PeopleId.user(userId), cols, req.getPage(20, 50));
        resp.body(RawText.of(users.toJson(cols, true)));
    }

    /**
     * 获取登录用户与另外一个用户的共同好友信息
     *
     * @remark 如果不指定另外一个用户，则返回登录用户的好友列表
     * @group Circle/Friend
     * @http-param user:LOGIN_ID 登录用户查看与此用户的共有的好友信息
     * @http-param cols:@std 共有用户的列名称
     * @http-param page:0 要查询的页码，基于0开始
     * @http-param count:20 要查询的每页数量，最大50
     * @http-return 共同好友列表，参见/user/show
     * @http-example @user_std.json
     */
    @Route(url = "/friend/common")
    public void showCommonFriends(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.getLong("user", ctx.getViewer());

        long[] commonIds;
        if (ctx.getViewer() == userId) {
            commonIds = friend.getBorqsFriendIds(ctx, userId);
        } else {
            long[] friendIds = friend.getBorqsFriendIds(ctx, ctx.getViewer());
            long[] userFriendIds = friend.getBorqsFriendIds(ctx, userId);
            commonIds = ArrayHelper.intersection(friendIds, userFriendIds);
        }
        Page page = req.getPage(20, 50);
        commonIds = page.retains(commonIds);
        String[] cols = User.expandColumns(req.getStringArray("cols", ",", User.STANDARD_COLUMNS));
        Users users = account.getUsers(ctx, cols, commonIds);
        resp.body(RawText.of(users.toJson(cols, true)));
    }

    private static final String[] FRIEND_USER_RETURN_COLUMNS = {
            User.COL_USER_ID,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
            RelUserExpansion.COL_REMARK,
            RequestUserExpansion.COL_PENDING_REQ_TYPES,
    };

    /**
     * 登录用户将一个好友设置到自己的某些圈子中去
     *
     * @remark 如果circles参数为空，那么其实是将此好友删除
     * @group Circle/Friend
     * @http-param friend|friendId 要设置的好友的ID
     * @http-param circles|circleIds 要将此好友设置到这些圈子中（逗号分隔的圈子ID）
     * @http-param allow.vcard:default true|false|default在加入用户为好友时可以设置自己的vcard是否对其可见，
     *              <ul>
     *              <li>如果为default不想进行额外的vcard可见性设设置</li>
     *              <li>如果为true则明确的设置我的vcard对friend可见</li>
     *              <li>如果为false则明确的设置我的vcard对friend不可见</li>
     *              </ul>
     * @http-param ret:false 此方法是否返回设置过的好友信息
     * @http-param retcols:user_id,display_name,nickname,photo,remark,pending_req_types 如果ret为true，此参数用来指定返回的好友信息的列
     * @http-return true 或者 返回的用户信息
     * @http-example {
     * "result":true
     * }
     */
    @Route(url = "/friend/set_circles")
    public void setFriendIntoCircles(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        PeopleId friendId = PeopleId.parseStringId(req.checkString("friend", "friendId"));
        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, friendId, req.checkIntArray("circles", "circleIds", ","));

        // privacy
        if (friendId.isUser()) {
            String allowVcard = req.getString("allow.vcard", "default");

            Boolean b = null;
            if (StringUtils.equals(allowVcard, "true"))
                b = true;
            else if (StringUtils.equals(allowVcard, "false"))
                b = false;

            if (b != null) {
                PrivacyEntry pe = PrivacyEntry.of(ctx.getViewer(), PrivacyResources.RES_VCARD, PrivacyTarget.user(friendId.getIdAsLong()), b);
                privacyControl.setPrivacy(ctx, pe);
            }
        }

        if (friendId.isUser() && req.getBoolean("ret", false)) {
            returnUser(req, resp, ctx, friendId);
        } else {
            resp.body(true);
        }
    }

    private void returnUser(Request req, Response resp, Context ctx, PeopleId friendId) {
        String[] returnCols = User.expandColumns(req.getStringArray("retcols", ",", FRIEND_USER_RETURN_COLUMNS));
        User returnUser = account.getUser(ctx, returnCols, friendId.getIdAsLong());
        if (returnUser == null)
            throw new ServerException(E.INVALID_USER, "Invalid user id " + friendId.getIdAsLong());
        resp.body(RawText.of(returnUser.toJson(returnCols, true)));
    }

    /**
     * 登录用户查看自己的好友列表
     *
     * @remark 如果此好友信息不存在，那么返回结果中不包含此好友信息
     * @group Circle/Friend
     * @http-param user 查询此用户的好友，如果此参数不指定，那么则查询当前登录用户的好友
     * @http-param circles 要查看哪些圈子中的好友（逗号分隔的圈子ID），如果不指定此参数，则查看自己所有好友
     * @http-param cols:@std 好友的列名称
     * @http-param page:0 要查询的页码，基于0开始
     * @http-param count:20 要查询的每页数量
     * @http-return 好友列表，参见/user/show
     * @http-example @user_std.json
     */
    @Route(url = "/friend/show")
    public void showFriends(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.getLong("user", ctx.getViewer());
        int[] circleIds = req.getIntArray("circles", ",", null);
        String[] cols = User.expandColumns(req.getStringArray("cols", ",", User.STANDARD_COLUMNS));
        Page page = req.getPage(20, Integer.MAX_VALUE);

        long[] friendIds = friend.getBorqsFriendIdsInCircles(ctx, userId, circleIds);
        friendIds = page.retains(friendIds);

        Users friends = account.getUsers(ctx, cols, friendIds);
        resp.body(RawText.of(friends.toJson(cols, true)));
    }


    /**
     * 登录用户将一批好友加入一个圈子或者从一个圈子中删除
     *
     * @remark 此方法不会检测要加入的好友是否是已经存在的用户
     * @group Circle/Friend
     * @http-param circle|circleId 要加入或者删除好友的圈子ID
     * @http-param friends|friendIds 逗号分隔的一批好友的ID列表
     * @http-param add|isadd 为true或者false，如果为true，表示将这批好友加入到圈子中，如果为false则从圈子中删除
     * @http-param ret:false 此方法是否返回设置过的好友信息列表
     * @http-param retcols:user_id,display_name,nickname,photo,remark,pending_req_types 如果ret为true，此参数用来指定返回的好友信息的列
     * @http-return true 或者 返回的用户信息
     * @http-example {
     * "result":true
     * }
     */
//    @Deprecated
//    @Route(url = "/circle/set_friends")
//    public void addOrRemoveFriendsIntoCircle(Request req, Response resp) {
//        Context ctx = checkContext(req, true);
//        int circleId = req.checkInt("circle", "circleId");
//        PeopleIds friendIds = PeopleIds.parse(null, req.checkString("friends", "friendIds"));
//        boolean add = req.checkBoolean("add", "isadd");
//        if (add) {
//            friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, friendIds, circleId);
//        } else {
//            friend.removeFriendsInCircle(ctx, friendIds, circleId);
//        }
//
//        if (req.getBoolean("ret", false)) {
//            returnAddOrRemovedUsers(req, resp, ctx, friendIds);
//        } else {
//            resp.body(true);
//        }
//    }

    private void returnAddOrRemovedUsers(Request req, Response resp, Context ctx, PeopleIds friendIds) {
        String[] returnCols = User.expandColumns(req.getStringArray("retcols", ",", FRIEND_USER_RETURN_COLUMNS));
        Users returnUsers = account.getUsers(ctx, returnCols, friendIds.getUserIds());
        resp.body(RawText.of(returnUsers.toJson(returnCols, true)));
    }

    /**
     * 登录用户将一批好友加入到某个圈子中
     *
     * @remark 此方法不会检测要加入的好友是否是已经存在的用户
     * @group Circle/Friend
     * @http-param circle 要加入好友的圈子ID
     * @http-param friends 逗号分隔的一批好友的ID列表
     * @http-param ret:false 此方法是否返回设置过的好友信息列表
     * @http-param retcols:user_id,display_name,nickname,photo,remark,pending_req_types 如果ret为true，此参数用来指定返回的好友信息的列
     * @http-return true 或者 返回的用户信息
     * @http-example {
     * "result":true
     * }
     */
    @Route(url = "/circle/add_friends")
    public void addFriendsIntoCircles(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        int circleId = req.checkInt("circle");
        PeopleIds friendIds = PeopleIds.parse(null, req.checkString("friends"));
        friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, friendIds, circleId);
        if (req.getBoolean("ret", false)) {
            returnAddOrRemovedUsers(req, resp, ctx, friendIds);
        } else {
            resp.body(true);
        }
    }

    /**
     * 登录用户将一批好友从某个圈子中移除
     *
     * @remark 此方法不会检测要加入的好友是否是已经存在的用户
     * @group Circle/Friend
     * @http-param circle 要删除好友的圈子ID
     * @http-param friends 逗号分隔的一批好友的ID列表
     * @http-param ret:false 此方法是否返回设置过的好友信息列表
     * @http-param retcols:user_id,display_name,nickname,photo,remark,pending_req_types 如果ret为true，此参数用来指定返回的好友信息的列
     * @http-return true 或者 返回的用户信息
     * @http-example {
     * "result":true
     * }
     */
    @Route(url = "/circle/remove_friends")
    public void removeFriendsIntoCircles(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        int circleId = req.checkInt("circle");
        PeopleIds friendIds = PeopleIds.parse(null, req.checkString("friends"));
        friend.removeFriendsInCircle(ctx, friendIds, circleId);
        if (req.getBoolean("ret", false)) {
            returnAddOrRemovedUsers(req, resp, ctx, friendIds);
        } else {
            resp.body(true);
        }
    }

    /**
     * 登录用户为某个好友设置备注名称
     *
     * @remark 要设置的用户名称可以不是此用户的好友
     * @group Remark
     * @http-param friend 要设置备注的好友的ID
     * @http-param remark 要设置的备注，为空字符串就是清除备注
     * @http-return true
     * @http-example {
     * "result":true
     * }
     */
    @Route(url = "/remark/set")
    public void setRemark(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        PeopleId friendId = PeopleId.parseStringId(req.checkString("friend"));
        friend.setRemark(ctx, friendId, req.checkString("remark"));
        resp.body(true);
    }
}
