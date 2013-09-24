package com.borqs.server.test.account.test1;


import com.borqs.server.ServerException;
import com.borqs.server.impl.account.UserDb;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.UserMocks;
import com.borqs.server.platform.util.Encoders;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.TreeSet;

public class AccountLogicTest1 extends ConfigurableTestCase {

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(UserDb.class);
    }


    private AccountLogic getAccountLogic() {
        return (AccountLogic) getBean("logic.account");
    }

//    public void testCreateUser() {
//        AccountLogic account = getAccountLogic();
//        User user = UserMocks.newFullInfoUser(0, "111111", "luo xiaofei");
//        user.setTel(new TelInfo(TelInfo.TYPE_HOME, "18701302350"));
//        User user2 = account.createUser(Context.create(), user);
//        System.out.println(user2);
//    }


    public void testCreateDestroy() {
        AccountLogic account = getAccountLogic();

        Context ctx = Context.create();

        //  create account
        User user = UserMocks.newFullInfoUser(0, "123456", "full name");

        User user1 = account.createUser(ctx, user);
        System.out.println(user1);
        ctx = Context.createForViewer(user1.getUserId());

        assertTrue(user1.getUserId() == 10001);
        assertTrue(user1.getCreatedTime() > 0);
        assertEquals(user1.getPassword(), user.getPassword());
        assertTrue(user1.propertiesEquals(user));

        // exists
        assertEquals(true, account.hasUser(ctx, user1.getUserId()));

        // destroy
        assertEquals(true, account.destroyUser(ctx));

        // exists
        assertEquals(false, account.hasUser(ctx, user1.getUserId()));

        // recover
        assertEquals(true, account.recoverUser(ctx));

        // exists
        assertEquals(true, account.hasUser(ctx, user1.getUserId()));
    }

    public void testCreateUpdateGet() {
        final String PWD = "123456";
        AccountLogic account = getAccountLogic();

        Context ctx = Context.create();

        //  create account
        User user = new User();
        user.setPassword(Encoders.md5Hex(PWD));
        user.setNickname("gaorx0");

        User userResult1 = account.createUser(ctx, user);
        ctx = Context.createForViewer(userResult1.getUserId());

        // get
        User user1 = account.getUser(ctx, null, userResult1.getUserId());
        assertTrue(user1.equalsWithoutUpdatedTimesAndAddons(userResult1));

        // update
        User user2 = user1.copy();
        user2.setNickname("gaorx1");
        assertEquals(true, account.update(ctx, user2));

        // get
        User user3 = account.getUser(ctx, null, user2.getUserId());
        assertTrue(user2.equalsWithoutUpdatedTimesAndAddons(user3));
    }

    public void testResetPassword() {
        final String OLD_PWD = "123456";
        final String NEW_PWD = "111111";
        AccountLogic account = getAccountLogic();

        Context ctx = Context.create();

        //  create account
        User user = new User();
        user.setPassword(Encoders.md5Hex(OLD_PWD));
        long userId = account.createUser(ctx, user).getUserId();

        ctx = Context.createForViewer(userId);

        User user1 = account.getUser(ctx, null, userId);
        assertEquals(user.getPassword(), user1.getPassword());


        try {
            account.updatePassword(ctx, Encoders.md5Hex(OLD_PWD + "error"), Encoders.md5Hex(NEW_PWD), true);
        } catch (ServerException e) {
            assertEquals(e.getCode(), E.INVALID_USER_OR_PASSWORD);
        }

        account.updatePassword(ctx, Encoders.md5Hex(OLD_PWD), Encoders.md5Hex(NEW_PWD), true);

        User user2 = account.getUser(ctx, null, userId);
        assertEquals(user2.getPassword(), Encoders.md5Hex(NEW_PWD));
        assertEquals(account.getPassword(ctx, userId), Encoders.md5Hex(NEW_PWD));

        // random password
        String randomPwd = account.resetRandomPassword(ctx);
        assertEquals(account.getPassword(ctx, ctx.getViewer()), Encoders.md5Hex(randomPwd));
    }

    public void testHasUsers() {
        final String PWD = "123456";
        AccountLogic account = getAccountLogic();

        Context ctx = Context.create();

        //  create account
        User user1 = new User();
        user1.setPassword(Encoders.md5Hex(PWD));
        long userId1 = account.createUser(ctx, user1).getUserId();

        User user2 = new User();
        user2.setPassword(Encoders.md5Hex(PWD));
        long userId2 = account.createUser(ctx, user2).getUserId();

        // hasUser
        assertEquals(true, account.hasUser(ctx, userId1));
        assertEquals(true, account.hasUser(ctx, userId2));

        // hasAnyUser
        assertEquals(true, account.hasAnyUser(ctx, userId1, 999999));
        assertEquals(true, account.hasAnyUser(ctx, userId2, 999999));
        assertEquals(true, account.hasAnyUser(ctx, userId2, userId2));

        // hasAllUser
        assertEquals(true, account.hasAllUser(ctx, userId1));
        assertEquals(true, account.hasAllUser(ctx, userId2));
        assertEquals(true, account.hasAllUser(ctx, userId1, userId2));
        assertEquals(false, account.hasAllUser(ctx, userId1, 999999));
        assertEquals(false, account.hasAllUser(ctx, userId2, 999999));

        // exists
        assertTrue(unorderedLongArrayEquals(new long[]{userId1}, account.getExistsIds(ctx, userId1, 999999)));
        assertTrue(unorderedLongArrayEquals(new long[]{userId2}, account.getExistsIds(ctx, userId2, 999999)));
        assertTrue(unorderedLongArrayEquals(new long[]{userId1, userId2}, account.getExistsIds(ctx, userId1, userId2, 999999)));
        assertTrue(unorderedLongArrayEquals(new long[]{userId1, userId2}, account.getExistsIds(ctx, userId1, userId2)));
    }

    private static boolean unorderedLongArrayEquals(long[] a1, long[] a2) {
        TreeSet<Long> set1 = new TreeSet<Long>(Arrays.asList(ArrayUtils.toObject(a1)));
        TreeSet<Long> set2 = new TreeSet<Long>(Arrays.asList(ArrayUtils.toObject(a2)));
        return set1.equals(set2);
    }

    public void testCreateAndRead() {
        AccountLogic account = getAccountLogic();
        Context ctx = Context.create();

        //  create account
        User user = UserMocks.newFullInfoUser(0, "123456", "full name");
        User user1 = account.createUser(ctx, user);

        assertEquals(user.getPassword(), user1.getPassword());
        assertTrue(user1.propertiesEquals(user));

        User user2 = account.getUser(ctx, null, user1.getUserId());
        assertTrue(user2.propertiesEquals(user1));
    }

//    public void testReadMulti() {
//        AccountLogic account = getAccountLogic();
//        Context ctx = Context.create();
//
//        //  create account
//        User user = UserMocks.newFullInfoUser(0, "123456", "full name");
//        User user1 = account.createUser(ctx, user);
//
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < 1000; i++) {
//            User user2= account.getUser(ctx, user1.getUserId());
//            //assertTrue(user2.propertiesEquals(user1));
//        }
//        System.out.println((System.currentTimeMillis() - start) + "ms");
//    }

}
