package com.borqs.server.test.pubapi.test1.register;


import com.borqs.server.impl.account.UserDb;
import com.borqs.server.impl.cibind.CibindDb;
import com.borqs.server.impl.login.TicketDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.NameInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.FriendReasons;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.login.LoginLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ServletTestCase;
import com.borqs.server.platform.test.TestFriend;
import com.borqs.server.platform.test.TestHttpApiClient;
import com.borqs.server.platform.test.TestMailSender;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.FeedbackParams;
import com.borqs.server.platform.util.sender.email.Mail;
import com.borqs.server.platform.web.AbstractHttpClient;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class RegisterApiTest2 extends ServletTestCase {

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

    private LoginLogic getLoginLogic() {
        return (LoginLogic) getBean("logic.login");
    }

    private TestMailSender getMailSender() {
       return (TestMailSender) getBean("sender.mail");
    }

    private CibindLogic getCibindLogic() {
        return (CibindLogic) getBean("logic.cibind");
    }

    private TestFriend getFriendLogic() {
        return (TestFriend) getBean("logic.friend");
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
    
    private String login(User user)
    {
        Context ctx = Context.create();
        LoginLogic login = getLoginLogic();
        String ticket = login.login(ctx, Long.toString(user.getUserId()), user.getPassword(), 1).ticket;
        return ticket;
    }

    public void testSendInviteMail() {
        // create one user
        User user1 = createUser("DickJiang", Encoders.md5Hex("123456"), "Jiang", "Changsheng");

        // login
        String ticket = login(user1);

        // send invite email
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY, ticket, 1, "appSecret1");
        AbstractHttpClient.Response resp = client.get(PUB_API + "/invite/mail/send", new Object[][]{
                {"emails", "jcsheng86@sina.com,jcsheng86@163.com"},
                {"names", "jiangchsh,b516"},
                {"message", "test send invite mail"}
        });

        assertTrue(resp.getStatusCode() == 200);
        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
        TestMailSender sender = getMailSender();
        List<Mail> mails = sender.getSentMails();
        assertTrue(mails.size() == 2);
        for(Mail mail : mails)
        {
            assertTrue(StringUtils.contains(mail.getMessage(), "Jiang Changsheng"));
            assertTrue(StringUtils.contains(mail.getMessage(), "test send invite mail"));
        }
    }

    public void testDisplayInvitePage() {
        User user1 = createUser("DickJiang", Encoders.md5Hex("123456"), "Jiang", "Changsheng");
        User user2 = createUser("jiangchsh", Encoders.md5Hex("123456"), "Jiang", "Chsh");

        String info = FeedbackParams.toSegmentedBase64(true, "/", "jcsheng86@sina.com", "jiangchsh",
                String.valueOf(user1.getUserId()));

        // not bind case
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/invite/mail/deal", new Object[][]{
                {"info", info}
        });
        
        assertTrue(StringUtils.contains(resp.getText(), "activeBtn"));
        assertTrue(!StringUtils.contains(resp.getText(), "addBtn"));
        
       // have bind case
       CibindLogic cibind = getCibindLogic();
       Context ctx = Context.create();
       ctx.setViewer(user2.getUserId());
       cibind.bind(ctx, BindingInfo.EMAIL, "jcsheng86@sina.com");

       client = newHttpApiClient(UA_EMPTY);
       resp = client.get(PUB_API + "/invite/mail/deal", new Object[][]{
               {"info", info}
       });

        assertTrue(!StringUtils.contains(resp.getText(), "activeBtn"));
        assertTrue(StringUtils.contains(resp.getText(), "addBtn"));

       // not friend
        assertTrue(StringUtils.contains(resp.getText(), "希望成为您的好友"));
        assertTrue(!StringUtils.contains(resp.getText(), "邀请您使用播思服务"));

        // is friend
       TestFriend friend = getFriendLogic();
       friend.addFriendsIntoCircle(ctx, FriendReasons.USER_ACTION, PeopleIds.of(PeopleId.fromId(user1.getUserId())), Circle.CIRCLE_DEFAULT);
       client = newHttpApiClient(UA_EMPTY);
       resp = client.get(PUB_API + "/invite/mail/deal", new Object[][]{
               {"info", info}
       });

        assertTrue(!StringUtils.contains(resp.getText(), "activeBtn"));
        assertTrue(!StringUtils.contains(resp.getText(), "addBtn"));
        assertTrue(!StringUtils.contains(resp.getText(), "希望成为您的好友"));
        assertTrue(StringUtils.contains(resp.getText(), "邀请您使用播思服务"));
    }

    public void testBindFromInvite() {
       User user1 = createUser("DickJiang", Encoders.md5Hex("123456"), "Jiang", "Changsheng");
       CibindLogic cibind = getCibindLogic();
       Context ctx = Context.create();
       ctx.setViewer(user1.getUserId());
       cibind.bind(ctx, BindingInfo.MOBILE_TEL, "13681547152");

       TestHttpApiClient client = newHttpApiClient(UA_EMPTY);
       AbstractHttpClient.Response resp = client.get(PUB_API + "/invite/mail/deal", new Object[][]{
               {"borqs_account", "13681547152"},
               {"borqs_pwd", Encoders.md5Hex("123456")},
               {"bind", "jcsheng86@sina.com"},
               {"action", 2}
       });

       assertEquals(cibind.whoBinding(ctx, "jcsheng86@sina.com"), user1.getUserId());
    }

    public void testMutualFriend() {
        TestHttpApiClient client = newHttpApiClient(UA_EMPTY);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/invite/mail/deal", new Object[][]{
                {"uid", SteveAndBill.STEVE_ID},
                {"fromid", SteveAndBill.BILL_ID},
                {"action", 3}
        });

        assertTrue(resp.getJsonNode().get("result").getBooleanValue());
    }

    public void testActiveAccount() {
        Context ctx = Context.create();
        AccountLogic account = getAccountLogic();

        TestHttpApiClient client = newHttpApiClient(UA_EMPTY);
        AbstractHttpClient.Response resp = client.get(PUB_API + "/invite/mail/deal", new Object[][]{
                {"password", Encoders.md5Hex("123456")},
                {"display_name", "jiangchsh"},
                {"gender", "m"},
                {"bind", "jcsheng86@sina.com"},
                {"mutual", false}, //TODO mutual = true
                {"action", 1}
        });
        
        CibindLogic cibind = getCibindLogic();
        long userId = cibind.whoBinding(ctx, "jcsheng86@sina.com");
        User user = account.getUser(ctx, null, userId);
        
        assertEquals(Encoders.md5Hex("123456"), user.getPassword());
        assertEquals("jiangchsh", user.getDisplayName());
        assertEquals("m", user.getProfile().getGender());
    }
}
