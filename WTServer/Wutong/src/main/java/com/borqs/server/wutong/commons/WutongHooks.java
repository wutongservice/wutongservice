package com.borqs.server.wutong.commons;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;

import java.util.ArrayList;

public class WutongHooks extends ArrayList<WutongHook> {
    public WutongHooks() {
    }

    public void fireUserCreated(Context ctx, Record user) {
        for (WutongHook l : this)
            l.onUserCreated(ctx, user);
    }

    public void fireUserDestroyed(Context ctx, Record user) {
        for (WutongHook l : this)
            l.onUserDestroyed(ctx, user);
    }

    public void fireUserProfileChanged(Context ctx, Record changed) {
        for (WutongHook l : this)
            l.onUserProfileChanged(ctx, changed);
    }

    public void fireFriendshipChanged(Context ctx, Record changed) {
        for (WutongHook l : this)
            l.onFriendshipChange(ctx, changed);
    }

    public void sendAccountInfo(Context ctx, Record changed){
        for (WutongHook l : this)
            l.onSetFriendChange(ctx, changed);
    }
}
