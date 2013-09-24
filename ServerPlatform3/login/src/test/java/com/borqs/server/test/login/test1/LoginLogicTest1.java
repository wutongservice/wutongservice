package com.borqs.server.test.login.test1;


import com.borqs.server.ServerException;
import com.borqs.server.impl.login.LoginImpl;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.login.LoginLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.util.Encoders;

public class LoginLogicTest1 extends ConfigurableTestCase {

    public static final long USER1_ID = 10001;
    public static final String USER1_EMAIL = "user1@test.com";
    public static final String USER1_PHONE = "13800000000";
    public static final String USER1_PASSWORD = Encoders.md5Hex("password1");

    public static final int USER2_ID = 10002;

    public static final String ERROR_PASSWORD = Encoders.md5Hex("error_password1");

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(LoginImpl.class);
    }

    private LoginLogic getLoginLogic() {
        return (LoginLogic) getBean("logic.login");
    }

    public void testLoginAndLogout() {
        LoginLogic login = getLoginLogic();
        Context ctx = Context.create();

        // login
        String ticket1 = login.login(ctx, USER1_EMAIL, USER1_PASSWORD, 1).ticket;
        String ticket2 = login.login(ctx, USER1_PHONE, USER1_PASSWORD, 1).ticket;
        String ticket3 = login.login(ctx, Long.toString(USER1_ID), USER1_PASSWORD, 1).ticket;

        info("ticket1 = " + ticket1);
        info("ticket2 = " + ticket2);
        info("ticket3 = " + ticket3);

        try {
            login.login(ctx, USER1_EMAIL, ERROR_PASSWORD, 1);
        } catch (Exception e) {
            assertTrue(e instanceof ServerException);
        }

        try {
            login.login(ctx, USER1_PHONE, ERROR_PASSWORD, 1);
        } catch (Exception e) {
            assertTrue(e instanceof ServerException);
        }

        try {
            login.login(ctx, Long.toString(USER1_ID), ERROR_PASSWORD, 1);
        } catch (Exception e) {
            assertTrue(e instanceof ServerException);
        }

        try {
            login.login(ctx, Long.toString(USER2_ID), ERROR_PASSWORD, 1);
        } catch (Exception e) {
            assertTrue(e instanceof ServerException);
        }

        // logout
        ctx = Context.createForViewer(USER1_ID);
        assertTrue(login.logout(ctx, ticket1));
        assertTrue(login.logout(ctx, ticket2));
        assertTrue(login.logout(ctx, ticket3));
        assertFalse(login.logout(ctx, "ERROR_TICKET"));
    }

    public void testValidatePassword() {
        LoginLogic login = getLoginLogic();
        Context ctx = Context.create();

        assertEquals(USER1_ID, login.validatePassword(ctx, USER1_EMAIL, USER1_PASSWORD));
        assertEquals(USER1_ID, login.validatePassword(ctx, USER1_PHONE, USER1_PASSWORD));
        assertEquals(USER1_ID, login.validatePassword(ctx, Long.toString(USER1_ID), USER1_PASSWORD));
        assertTrue(login.validatePassword(ctx, USER1_EMAIL, ERROR_PASSWORD) <= 0);
        assertTrue(login.validatePassword(ctx, Long.toString(USER2_ID), USER1_PASSWORD) <= 0);
    }

    public void testWho() {
        LoginLogic login = getLoginLogic();
        Context ctx = Context.create();

        String ticket = login.login(ctx, USER1_EMAIL, USER1_PASSWORD, 1).ticket;
        assertEquals(USER1_ID, login.whoLogined(ctx, ticket));
        assertTrue(login.whoLogined(ctx, "ERROR_TICKET") <= 0);
        assertTrue(login.hasTicket(ctx, ticket));
    }
}
