package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.hook.Hook;

import java.util.List;

public interface FriendsHook extends Hook<List<FriendsHook.Entry>> {
    static class Entry {
        public final long userId;
        public final PeopleId friendId;
        public final int reason;
        public int[] circleIds;

        public Entry(long userId, PeopleId friendId, int reason, int[] circleIds) {
            this.userId = userId;
            this.friendId = friendId;
            this.reason = reason;
            this.circleIds = circleIds;
        }
    }
}
