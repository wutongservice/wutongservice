package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.sender.notif.NotifSenderSupport;

import java.util.ArrayList;
import java.util.List;

public class FriendsNotifHooks {
    public static class CreateFriendsHook extends NotifSenderSupport implements FriendsHook {
        private FriendLogic friend;

        public void setFriend(FriendLogic friend) {
            this.friend = friend;
        }

        @Override
        public void before(Context ctx, List<Entry> data) {
            // find the foreigner to list for send notification
            List<Entry> e = new ArrayList<Entry>();
            for (Entry entry : data) {
                Relationship r = friend.getRelationship(ctx, PeopleId.user(entry.userId), entry.friendId);
                if (!r.isViewerFriend()) {
                    e.add(entry);
                }
            }
            ctx.putSession("foreigner", e);
        }

        @Override
        public void after(Context ctx, List<Entry> data) {
            asyncSend(ctx, MakerTemplates.NOTIF_ACCEPT_SUGGEST, new Object[][]{
                    {"entry", data}
            });
        }
    }

    public static class NewFollowerHook extends NotifSenderSupport implements FriendsHook {


        @Override
        public void before(Context ctx, List<Entry> data) {

        }

        @Override
        public void after(Context ctx, List<Entry> data) {

            asyncSend(ctx, MakerTemplates.NOTIF_NEW_FOLLOWER, new Object[][]{
                    {"entry", data}
            });
        }
    }

}
