package com.borqs.server.platform.test.mock;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.NameInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.FriendReasons;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.test.TestAccount;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestFriend;
import com.borqs.server.platform.test.TestLogin;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;

public class ServerTeam {

    public static final long GRX_ID = 10001;
    public static final long JCS_ID = 10002;
    public static final long CG_ID = 10003;
    public static final long WP_ID = 10004;

    public static final String GRX_NICKNAME = "荣欣";
    public static final String JCS_NICKNAME = "长胜";
    public static final String CG_NICKNAME = "陈果";
    public static final String WP_NICKNAME = "王鹏";


    public static final String GRX_FULL_NAME = "高荣欣";
    public static final String JCS_FULL_NAME = "姜长胜";
    public static final String CG_FULL_NAME = "陈果";
    public static final String WP_FULL_NAME = "王鹏";

    public static final String GRX_PASSWORD = Encoders.md5Hex("111111");
    public static final String JCS_PASSWORD = Encoders.md5Hex("111111");
    public static final String CG_PASSWORD = Encoders.md5Hex("111111");
    public static final String WP_PASSWORD = Encoders.md5Hex("111111");

    private static volatile TestAccount account;
    private static volatile TestApp app;
    private static volatile TestLogin login;
    private static volatile TestFriend friend;

    private static volatile String grxTicket;
    private static volatile String jcsTicket;
    private static volatile String cgTicket;
    private static volatile String wpTicket;

    public static TestAccount account() {
        if (account != null)
            return account;

        TestAccount a = new TestAccount();

        a.resetIdCounter(GRX_ID);
        long now = DateHelper.nowMillis();



        User user1 = new User();
        user1.setPassword(GRX_PASSWORD);
        user1.setCreatedTime(now);
        user1.setNickname(GRX_NICKNAME);
        user1.setName(NameInfo.split(GRX_FULL_NAME));
        a.createUser(Context.create(), user1);

        User user2 = new User();
        user2.setPassword(JCS_PASSWORD);
        user2.setCreatedTime(now);
        user2.setNickname(JCS_NICKNAME);
        user2.setName(NameInfo.split(JCS_FULL_NAME));
        a.createUser(Context.create(), user2);

        User user3 = new User();
        user3.setPassword(CG_PASSWORD);
        user3.setCreatedTime(now);
        user3.setNickname(CG_NICKNAME);
        user3.setName(NameInfo.split(CG_FULL_NAME));
        a.createUser(Context.create(), user3);

        User user4 = new User();
        user4.setPassword(WP_PASSWORD);
        user4.setCreatedTime(now);
        user4.setNickname(WP_NICKNAME);
        user4.setName(NameInfo.split(WP_FULL_NAME));
        a.createUser(Context.create(), user4);

        account = a;

        return account;
    }

    public static TestApp app() {
        if (app != null)
            return app;

        app = new TestApp();
        return app;
    }

    public static TestLogin login() {
        if (login != null)
            return login;

        TestLogin a = new TestLogin();
        a.setAccount(account());
        grxTicket = a.login(null, Long.toString(GRX_ID), GRX_PASSWORD, TestApp.APP1_ID).ticket;
        jcsTicket = a.login(null, Long.toString(JCS_ID), JCS_PASSWORD, TestApp.APP2_ID).ticket;
        cgTicket = a.login(null, Long.toString(CG_ID), CG_PASSWORD, TestApp.APP2_ID).ticket;
        wpTicket = a.login(null, Long.toString(WP_ID), WP_PASSWORD, TestApp.APP1_ID).ticket;
        login = a;
        return login;
    }


    public static TestFriend friend() {
        if (friend != null)
            return friend;

        TestFriend f = new TestFriend();
        f.setAccount(account());

        // grx
        //      jcs in (CIRCLE_DEFAULT, 101)
        //      cg in (CIRCLE_DEFAULT)
        //      wp in (101)

        Circle c1 = f.createCustomCircle(Context.createForViewer(GRX_ID), "grx's colleague");
        f.setFriendIntoCircles(Context.createForViewer(GRX_ID), FriendReasons.USER_ACTION, PeopleId.user(JCS_ID), Circle.CIRCLE_DEFAULT, c1.getCircleId());
        f.setFriendIntoCircles(Context.createForViewer(GRX_ID), FriendReasons.USER_ACTION, PeopleId.user(CG_ID), Circle.CIRCLE_DEFAULT);
        f.setFriendIntoCircles(Context.createForViewer(GRX_ID), FriendReasons.USER_ACTION, PeopleId.user(WP_ID), c1.getCircleId());

        // jcs
        //      grx in (CIRCLE_DEFAULT, 101)
        //      wp in (101)
        c1 = f.createCustomCircle(Context.createForViewer(JCS_ID), "jcs's colleague");
        f.setFriendIntoCircles(Context.createForViewer(JCS_ID), FriendReasons.USER_ACTION, PeopleId.user(GRX_ID), Circle.CIRCLE_DEFAULT, c1.getCircleId());
        f.setFriendIntoCircles(Context.createForViewer(JCS_ID), FriendReasons.USER_ACTION, PeopleId.user(WP_ID), c1.getCircleId());

        // cg
        //      jcs in (CIRCLE_DEFAULT)
        f.setFriendIntoCircles(Context.createForViewer(CG_ID), FriendReasons.USER_ACTION, PeopleId.user(JCS_ID), Circle.CIRCLE_DEFAULT);

        // wp
        //      grx in (CIRCLE_DEFAULT)
        //      cg in (CIRCLE_BLOCKED)
        //      jcs in (CIRCLE_DEFAULT)
        f.setFriendIntoCircles(Context.createForViewer(WP_ID), FriendReasons.USER_ACTION, PeopleId.user(GRX_ID), Circle.CIRCLE_DEFAULT);
        f.setFriendIntoCircles(Context.createForViewer(WP_ID), FriendReasons.USER_ACTION, PeopleId.user(CG_ID), Circle.CIRCLE_BLOCKED);
        f.setFriendIntoCircles(Context.createForViewer(WP_ID), FriendReasons.USER_ACTION, PeopleId.user(JCS_ID), Circle.CIRCLE_DEFAULT);

        friend = f;
        return friend;
    }

    public static String grxTicket() {
        return grxTicket;
    }

    public static String jcsTicket() {
        return jcsTicket;
    }

    public static String cgTicket() {
        return cgTicket;
    }

    public static String wpTicket() {
        return wpTicket;
    }

    public static User grx() {
        return account().getUser(Context.create(), null, GRX_ID);
    }

    public static User jcs() {
        return account().getUser(Context.create(), null, JCS_ID);
    }

    public static User cg() {
        return account().getUser(Context.create(), null, CG_ID);
    }

    public static User wp() {
        return account().getUser(Context.create(), null, WP_ID);
    }
}
