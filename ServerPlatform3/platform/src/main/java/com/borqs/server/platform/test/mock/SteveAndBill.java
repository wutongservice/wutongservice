package com.borqs.server.platform.test.mock;



import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.NameInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.test.TestAccount;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestLogin;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;

public class SteveAndBill {
    public static final long STEVE_ID = 10001;
    public static final long BILL_ID = 10002;

    public static final String STEVE_NICKNAME = "Apple_CEO(in heaven)";
    public static final String BILL_NICKNAME = "MS_CEO";

    public static final String STEVE_FULL_NAME = "Steve Jobs";
    public static final String BILL_FULL_NAME = "Bill Gates";

    public static final String STEVE_PASSWORD = Encoders.md5Hex("steve");
    public static final String BILL_PASSWORD = Encoders.md5Hex("bill");

    private static volatile TestAccount account;
    private static volatile TestApp app;
    private static volatile TestLogin login;

    private static volatile String steveTicket;
    private static volatile String billTicket;

    public static TestAccount account() {
        if (account != null)
            return account;

        TestAccount a = new TestAccount();
        a.resetIdCounter(STEVE_ID);
        long now = DateHelper.nowMillis();

        User user1 = new User();
        user1.setPassword(STEVE_PASSWORD);
        user1.setCreatedTime(now);
        user1.setNickname(STEVE_NICKNAME);
        user1.setName(NameInfo.split(STEVE_FULL_NAME));
        a.createUser(null, user1);


        User user2 = new User();
        user2.setPassword(BILL_PASSWORD);
        user2.setCreatedTime(now);
        user2.setNickname(BILL_NICKNAME);
        user2.setName(NameInfo.split(BILL_FULL_NAME));
        a.createUser(null, user2);
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
        steveTicket = a.login(null, Long.toString(STEVE_ID), STEVE_PASSWORD, TestApp.APP1_ID).ticket;
        billTicket = a.login(null, Long.toString(BILL_ID), BILL_PASSWORD, TestApp.APP2_ID).ticket;
        login = a;
        return login;
    }

    public static String steveTicket() {
        return steveTicket;
    }

    public static String billTicket() {
        return billTicket;
    }

    public static User steve() {
        return account().getUser(Context.create(), null, STEVE_ID);
    }

    public static User bill() {
        return account().getUser(Context.create(), null, BILL_ID);
    }
}
