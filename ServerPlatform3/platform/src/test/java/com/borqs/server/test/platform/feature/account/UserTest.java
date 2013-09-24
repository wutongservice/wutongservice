package com.borqs.server.test.platform.feature.account;


import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.test.mock.UserMocks;
import junit.framework.TestCase;


public class UserTest extends TestCase {

    public void testJsonSerialization() {
        User user = UserMocks.newFullInfoUser(10001, "123456", "hello world");
        String json = user.toJson(null, true);
        System.out.println(json);
        User user2 = User.fromJson(json);
        assertTrue(user.equalsWithoutUpdatedTimesAndAddons(user2));
    }
}
