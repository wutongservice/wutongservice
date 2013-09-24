package com.borqs.server.platform.test.mock;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.NameInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.test.TestAccount;
import com.borqs.server.platform.util.Encoders;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class SomeUsers {

    public static final String PASSWORD = "111111";
    public static final String ENCODED_PASSWORD = Encoders.md5Hex(PASSWORD);

    public static TestAccount accountOf(List<String> displayNames) {
        TestAccount account = new TestAccount();
        Context ctx = Context.create();
        if (CollectionUtils.isNotEmpty(displayNames)) {
            for (String displayName : displayNames) {
                User user = new User();
                user.setPassword(ENCODED_PASSWORD);
                user.setName(NameInfo.split(displayName));
                account.createUser(ctx, user);
            }
        }
        return account;
    }

    public static TestAccount createAccountWith(int userCount) {
        ArrayList<String> displayNames = new ArrayList<String>();
        for (int i = 0; i < userCount; i++)
            displayNames.add("Test" + (i + 1));
        return accountOf(displayNames);
    }

    public static TestAccount createAccountWith10Users() {
        return createAccountWith(10);
    }

    public static TestAccount createAccountWith100Users() {
        return createAccountWith(100);
    }

    public static TestAccount createAccountWith1000Users() {
        return createAccountWith(1000);
    }

    public static TestAccount createAccountWith10000Users() {
        return createAccountWith(10000);
    }
}
