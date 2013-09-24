package com.borqs.server.test.conversation.test1;

import com.borqs.server.impl.conversation.ConversationDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.Conversation;
import com.borqs.server.platform.feature.conversation.ConversationBase;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.conversation.Conversations;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.TestAccount;
import com.borqs.server.platform.test.mock.SteveAndBill;
import org.apache.commons.lang.ArrayUtils;

import java.util.Map;

public class ConversationLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(ConversationDb.class);
    }

    private ConversationLogic getConversationLogic() {
        return (ConversationLogic)getBean("logic.conversation");
    }

    private TestAccount getAccountLogic() {
        return (TestAccount)getBean("logic.account");
    }

    public void testCreate() {
        ConversationLogic convImpl = getConversationLogic();
        Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);
        ConversationBase conv0 = new ConversationBase(new Target(Target.APK, "com.user.qiupu"), Actions.COMMENT);
        convImpl.create(ctx, conv0);

        Conversations convs = convImpl.findByTarget(ctx, null, null, new Target(Target.APK, "com.user.qiupu"));
        assertEquals(convs.size(), 1);
        Conversation conv1 = convs.get(0);
        assertEquals(conv1.getTarget(), conv0.getTarget());
        assertEquals(conv1.getReason(), conv0.getReason());
        assertEquals(conv1.getUser(), SteveAndBill.STEVE_ID);
    }

    public void testDeleteByConvs() {
        ConversationLogic convImpl = getConversationLogic();
        Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);
        ConversationBase conv0 = new ConversationBase(new Target(Target.APK, "com.user.qiupu"), Actions.COMMENT);
        convImpl.create(ctx, conv0);

        Conversations convs = convImpl.findByTarget(ctx, null, null, new Target(Target.APK, "com.user.qiupu"));
        assertEquals(convs.size(), 1);

        convImpl.delete(ctx, conv0);
        convs = convImpl.findByTarget(ctx, null, null, new Target(Target.APK, "com.user.qiupu"));
        assertEquals(convs.size(), 0);
    }

    public void testDeleteByTargets() {
        ConversationLogic convImpl = getConversationLogic();
        Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);
        ConversationBase conv0 = new ConversationBase(new Target(Target.APK, "com.user.qiupu"), Actions.COMMENT);
        convImpl.create(ctx, conv0);

        Conversations convs = convImpl.findByTarget(ctx, null, null, new Target(Target.APK, "com.user.qiupu"));
        assertEquals(convs.size(), 1);

        convImpl.delete(ctx, new Target(Target.APK, "com.user.qiupu"));
        convs = convImpl.findByTarget(ctx, null, null, new Target(Target.APK, "com.user.qiupu"));
        assertEquals(convs.size(), 0);
    }

    public void testFindByTarget() {
        ConversationLogic convImpl = getConversationLogic();
        Context ctx0 = Context.createForViewer(SteveAndBill.STEVE_ID);
        ConversationBase conv0 = new ConversationBase(new Target(Target.APK, "com.user.qiupu"), Actions.COMMENT);
        ConversationBase conv1 = new ConversationBase(new Target(Target.POST, "123456"), Actions.COMMENT);
        convImpl.create(ctx0, conv0, conv1);

        Context ctx1 = Context.createForViewer(SteveAndBill.BILL_ID);
        ConversationBase conv3 = new ConversationBase(new Target(Target.APK, "com.user.qiupu"), Actions.LIKE);
        ConversationBase conv4 = new ConversationBase(new Target(Target.POST, "123456"), Actions.LIKE);
        convImpl.create(ctx1, conv3, conv4);
        
        Conversations reuse = new Conversations();
        convImpl.findByTarget(ctx0,  reuse, null, new Target(Target.APK, "com.user.qiupu"));
        assertEquals(reuse.size(), 2);
        for (Conversation conv : reuse) {
            if (conv.getUser() == SteveAndBill.STEVE_ID)
                assertEquals(conv.getReason(), Actions.COMMENT);
            else 
                assertEquals(conv.getReason(), Actions.LIKE);
        }
        convImpl.findByTarget(ctx0, reuse, new int[]{Actions.LIKE}, null, new Target(Target.POST, "123456"));
        assertEquals(reuse.size(), 3);

        Conversations convs = convImpl.findByTarget(ctx1, null, null, new Target(Target.APK, "com.user.qiupu"),
                new Target(Target.POST, "123456"));
        assertEquals(convs.size(), 4);

        long[] users = convs.getUsers();
        assertEquals(users.length, 2);
        assertTrue(ArrayUtils.contains(users, SteveAndBill.BILL_ID));
        Target[] targets = convs.getTargets();
        assertEquals(targets.length, 2);
        Map<Long, Target[]> m0 = convs.getGroupedTargets();
        assertEquals(m0.size(), 2);
        assertEquals(m0.get(SteveAndBill.BILL_ID).length, 2);
        Map<Target, long[]> m1 = convs.getGroupedUsers();
        assertEquals(m1.size(), 2);
        assertTrue(ArrayUtils.contains(m1.get(new Target(Target.POST, "123456")), SteveAndBill.BILL_ID));
        
        //test has
        boolean has = convImpl.has(ctx0,  SteveAndBill.BILL_ID, new Target(Target.APK, "com.user.qiupu"), Actions.LIKE);
        assertTrue(has);
        has = convImpl.has(ctx0,  SteveAndBill.STEVE_ID, new Target(Target.APK, "com.user.qiupu"), Actions.LIKE);
        assertFalse(has);
        
        //test get counts
        ConversationBase conv5 = new ConversationBase(new Target(Target.POST, "123459"), Actions.LIKE);
        convImpl.create(ctx0, conv5);
        convImpl.create(ctx1, conv5);
        Map<Target, Long> m = convImpl.getCounts(ctx0, Actions.LIKE, new Target(Target.POST, "123456"), new Target(Target.POST, "123459"));
        assertEquals(1, m.get(new Target(Target.POST, "123456")).longValue());
        assertEquals(2, m.get(new Target(Target.POST, "123459")).longValue());

        //test get count
        long count = convImpl.getCount(ctx0, Actions.LIKE, new Target(Target.POST, "123459"));
        assertEquals(2, count);

        //test get target users
        users = convImpl.getTargetUsers(ctx0, new Target(Target.POST, "123459"), Actions.LIKE, null);
        assertEquals(2, users.length);
        assertTrue(ArrayUtils.contains(users, SteveAndBill.BILL_ID));

        //test get user targets
        targets = convImpl.getUserTargets(ctx0, SteveAndBill.BILL_ID, Actions.LIKE, null);
        assertEquals(2, targets.length);
    }

    public void testFindByUser() {
        ConversationLogic convImpl = getConversationLogic();
        Context ctx0 = Context.createForViewer(SteveAndBill.STEVE_ID);
        ConversationBase conv0 = new ConversationBase(new Target(Target.APK, "com.user.qiupu"), Actions.COMMENT);
        ConversationBase conv1 = new ConversationBase(new Target(Target.POST, "123456"), Actions.COMMENT);
        convImpl.create(ctx0, conv0, conv1);

        Context ctx1 = Context.createForViewer(SteveAndBill.BILL_ID);
        ConversationBase conv3 = new ConversationBase(new Target(Target.APK, "com.user.qiupu"), Actions.LIKE);
        ConversationBase conv4 = new ConversationBase(new Target(Target.POST, "123456"), Actions.LIKE);
        convImpl.create(ctx1, conv3, conv4);

        Conversations reuse = new Conversations();
        convImpl.findByUser(ctx0, reuse, null, SteveAndBill.STEVE_ID);
        assertEquals(reuse.size(), 2);
        for (Conversation conv : reuse) {
            if (conv.getReason() == Actions.COMMENT)
                assertEquals(conv.getTarget(), new Target(Target.APK, "com.user.qiupu"));
            else
                assertEquals(conv.getTarget(), new Target(Target.POST, "123456"));
        }
        convImpl.findByUser(ctx0, reuse, new int[]{Actions.LIKE}, null, SteveAndBill.BILL_ID);
        assertEquals(reuse.size(), 3);

        Conversations convs = convImpl.findByUser(ctx1, null, null, SteveAndBill.STEVE_ID,
                SteveAndBill.BILL_ID);
        assertEquals(convs.size(), 4);
    }
}
