package com.borqs.server.compatible;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.UserHook;
import com.borqs.server.platform.feature.account.UserIdHook;
import com.borqs.server.platform.feature.friend.FriendsHook;
import com.borqs.server.platform.hook.AbstractRedisHook;

import java.util.List;

public class V1PlatformPubHooks {

    public static class CreateUserHook extends AbstractRedisHook implements UserHook {
        @Override
        public void before(Context ctx, User data) {
        }

        @Override
        public void after(Context ctx, User data) {
            // TODO: xx
        }
    }

    public static class DestroyUserHook extends AbstractRedisHook implements UserIdHook {
        @Override
        public void before(Context ctx, Long data) {
        }

        @Override
        public void after(Context ctx, Long data) {
            // TODO: xx
        }
    }

    public static class UpdateUserHook extends AbstractRedisHook implements UserHook {
        @Override
        public void before(Context ctx, User data) {
        }

        @Override
        public void after(Context ctx, User data) {
            // TODO: xx
        }
    }

    public static class SetFriendsHook extends AbstractRedisHook implements FriendsHook {
        @Override
        public void before(Context ctx, List<Entry> data) {
        }

        @Override
        public void after(Context ctx, List<Entry> data) {
            // TODO: xx
        }
    }
}
