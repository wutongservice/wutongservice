package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.hook.Hook;

public interface RemarkHook extends Hook<RemarkHook.Data> {
    static class Data {
        public final long userId;
        public final PeopleId friendId;
        public final String remark;

        public Data(long userId, PeopleId friendId, String remark) {
            this.userId = userId;
            this.friendId = friendId;
            this.remark = remark;
        }
    }
}
