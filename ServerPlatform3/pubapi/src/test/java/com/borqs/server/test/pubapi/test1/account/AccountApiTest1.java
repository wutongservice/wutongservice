package com.borqs.server.test.pubapi.test1.account;


import com.borqs.server.impl.account.UserDb;
import com.borqs.server.impl.cibind.CibindDb;
import com.borqs.server.impl.login.TicketDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.NameInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestApp;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.test.mock.UserMocks;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.web.AbstractHttpClient;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

public class AccountApiTest1 extends ServletTestCase {

    public static final String PUB_API = "servlet.pubApi";

    @Override
    protected String[] getServletBeanIds() {
        return new String[]{PUB_API};
    }

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(UserDb.class, TicketDb.class, CibindDb.class);
    }

    private AccountLogic getAccountLogic() {
        return (AccountLogic) getBean("logic.account");
    }

    private User createUser(String nickname, String password, String firstName, String lastName) {
        Context ctx = Context.create();
        AccountLogic account = getAccountLogic();
        User user = new User();
        user.setPassword(password);
        user.setNickname(nickname);
        user.setName(new NameInfo(firstName, lastName));
        return account.createUser(ctx, user);
    }

    public void testShowUser() {
        AccountLogic account = getAccountLogic();

        // create one user
        User user1 = account.createUser(Context.create(), UserMocks.newFullInfoUser(0L, "123456", "hello world"));

        // get user
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/user/show", new Object[][]{
                {"users", user1.getUserId()},
                {"columns", StringHelper.join(User.FULL_COLUMNS, ",")},
        });

        String json = resp.getText();
        Users users = Users.fromJson(null, json);
        assertTrue(users.size() == 1);
        User user1a = users.get(0);
        assertEquals(user1.getUserId(), user1a.getUserId());
        assertEquals(user1.getCreatedTime(), user1a.getCreatedTime());
        assertTrue(user1.propertiesEquals(user1a));
    }

    public void testUpdateUser() {
        User newUser = UserMocks.newFullInfoUser(SteveAndBill.STEVE_ID, "111111", SteveAndBill.STEVE_FULL_NAME);
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, SteveAndBill.steveTicket(), TestApp.APP1_ID, TestApp.APP1_SECRET);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/user/update", new Object[][] {
                {"profile", newUser},
        });

        JsonNode jn = resp.getJsonNode();
        assertEquals(jn.path("result").getBooleanValue(), true);

        resp = client.get(PUB_API + "/user/show", new Object[][] {
                {"users", newUser.getUserId()},
                {"columns", StringUtils.join(User.FULL_COLUMNS, ",")},
        });

        Users users = Users.fromJson(null, resp.getText());
        assertTrue(users.size() == 1);
        assertTrue(users.get(0).propertiesEquals(newUser));
    }
}
