package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.compatible.CompatibleCircle;
import com.borqs.server.compatible.CompatibleUser;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.feature.privacy.PrivacyControlLogic;
import com.borqs.server.platform.feature.privacy.VcardFriendHook;
import com.borqs.server.platform.feature.request.RequestLogic;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;


@IgnoreDocument
public class Friend1Api extends PublicApiSupport {
    private FriendLogic friend;
    private AccountLogic account;
    private PrivacyControlLogic privacyControl;
    private RequestLogic request;


    public static final int FRIEND_REASON_MANUALSELECT = 8;

    public Friend1Api() {
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

    public void setRequest(RequestLogic request) {
        this.request = request;
    }


    @Route(url = "/circle/create")
    public void createCircle(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        Circle circle = friend.createCustomCircle(ctx, req.checkString("name"));
        resp.body(circle != null ? circle.getCircleId() : 0);
    }

    @Route(url = "/circle/destroy")
    public void destroyCircle(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        int[] circleIds = req.checkIntArray("circles", ",");
        for (int circleId : circleIds)
            friend.destroyCustomCircle(ctx, circleId);
        resp.body(true);
    }

    @Route(url = "/circle/show")
    public void getCircles(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        boolean withUsers = req.getBoolean("with_users", false);
        Circles circles = friend.getCircles(ctx, ctx.getViewer(),
                req.getIntArray("circles", ",", null),
                withUsers);
        resp.body(RawText.of(CompatibleCircle.circlesToJson(circles,
                withUsers ? CompatibleCircle.CIRCLE_COLUMNS_WITH_MEMBERS : CompatibleCircle.CIRCLE_COLUMNS,
                true)));
    }

    @Route(url = "/circle/update")
    public void updateCircleName(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        boolean b = friend.updateCustomCircleName(ctx, req.checkInt("circle"), req.checkString("name"));
        resp.body(b);
    }

    @Route(url = "/follower/show")
    public void showFollowers(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.getLong("user", ctx.getViewer());
        String[] v1Cols = CompatibleUser.expandV1Columns(req.getStringArray("columns", ",", CompatibleUser.V1_FULL_COLUMNS));
        Users users = friend.getFollowerUsers(ctx, PeopleId.user(userId), CompatibleUser.v1ToV2Columns(v1Cols), req.getPage(20, 50));
        resp.body(RawText.of(CompatibleUser.usersToJson(users, v1Cols, true)));
    }

    @Route(url = "/friend/both")
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
        String[] v1Cols = CompatibleUser.expandV1Columns(req.getStringArray("columns", ",", CompatibleUser.V1_FULL_COLUMNS));
        Users users = account.getUsers(ctx, CompatibleUser.v1ToV2Columns(v1Cols), commonIds);
        resp.body(RawText.of(users.toJson(v1Cols, true)));
    }

    @Route(url = "/friend/circlesset")
    public void setFriendIntoCircles(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        ctx.setToggle(VcardFriendHook.CTX_TOGGLE_KEY, true); // important!
        PeopleId friendId = PeopleId.parseStringId(req.checkString("friendId"));
        friend.setFriendIntoCircles(ctx, FriendReasons.USER_ACTION, friendId, req.checkIntArray("circleIds", ","));
        String[] v1Cols = CompatibleUser.V1_LIGHT_COLUMNS;
        User returnUser = account.getUser(ctx, CompatibleUser.v1ToV2Columns(v1Cols), friendId.getIdAsLong());
        if (returnUser == null)
            throw new ServerException(E.INVALID_USER, "Invalid user id " + friendId.getIdAsLong());
        resp.body(RawText.of(CompatibleUser.userToJson(returnUser, v1Cols, true)));
    }

    @Route(url = "/friend/show")
    public void showFriends(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long userId = req.getLong("user", ctx.getViewer());

        int[] circleIds = req.getIntArray("circles", ",", null);
        String[] v1Cols = CompatibleUser.expandV1Columns(req.getStringArray("columns", ",", CompatibleUser.V1_LIGHT_COLUMNS));
        Page page = req.getPage(20, Integer.MAX_VALUE);

        long[] friendIds = friend.getBorqsFriendIdsInCircles(ctx, userId, circleIds);
        friendIds = page.retains(friendIds);

        Users friends = account.getUsers(ctx, CompatibleUser.v1ToV2Columns(v1Cols), friendIds);
        resp.body(RawText.of(CompatibleUser.usersToJson(friends, v1Cols, true)));
    }

    @Route(url = "/friend/usersset")
    public void addOrRemoveFriendsIntoCircle(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        ctx.setToggle(VcardFriendHook.CTX_TOGGLE_KEY, true); // important!

        int circleId = req.checkInt("circleId");
        PeopleIds friendIds = PeopleIds.parse(null, req.checkString("friendIds"));
        boolean add = req.checkBoolean("isadd");
        if (add) {
            friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, friendIds, circleId);
        } else {
            friend.removeFriendsInCircle(ctx, friendIds, circleId);
        }

        //String[] v1Cols = CompatibleUser.expandV1Columns(req.getStringArray("columns", ",", CompatibleUser.V1_LIGHT_COLUMNS));
        //Users returnUsers = account.getUsers(ctx, CompatibleUser.v1ToV2Columns(v1Cols), friendIds.getUserIds());
        //resp.body(RawText.of(CompatibleUser.usersToJson(returnUsers, v1Cols, true)));
        resp.body(true);
    }

    @Route(url = "/remark/set")
    public void setRemark(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        PeopleId friendId = PeopleId.parseStringId(req.checkString("friend"));
        friend.setRemark(ctx, friendId, req.checkString("remark"));
        resp.body(true);
    }

    @Route(url = "/friend/exchange_vcard")
    public void exchangeVcard(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        boolean sendRequest = req.getBoolean("send_request", false);

        int[] circleIds = req.checkIntArray("circleIds", ",");
        PeopleId friendId = PeopleId.parseStringId(req.checkString("friendId"));
        friend.setFriendIntoCircles(ctx, FRIEND_REASON_MANUALSELECT, friendId, circleIds);

        // 业务逻辑：首先将当前的friendid加入我的隐私圈和熟人圈子，对外广播

        //TODO　 broadcasting the change of friendship

        if (sendRequest) {
            // this friendId is in my privacy circle or not
            boolean b = friend.hasFriendInCircles(ctx, ctx.getViewer(), new int[]{1}, friendId);
            if (!b) {
                //if this friend is not in my privacy circle ,then send request
                com.borqs.server.platform.feature.request.Request r = new com.borqs.server.platform.feature.request.Request();
                r.setCreatedTime(DateHelper.nowMillis());
                r.setFrom(ctx.getViewer());
                r.setTo(friendId.getIdAsLong());
                r.setStatus(com.borqs.server.platform.feature.request.Request.STATUS_PENDING);
                r.setType(com.borqs.server.platform.feature.request.Request.TYPE_ANY);
                request.create(ctx, r);
            }
        }
        User user = account.getUser(ctx, User.STANDARD_COLUMNS, friendId.getIdAsLong());
        resp.body(RawText.of(JsonHelper.toJson(user, false)));
    }


}
