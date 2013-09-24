package com.borqs.server.service.platform.event;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.event.Hooks;

public class PlatformHooks extends Hooks<PlatformHook> {
    public void fireUserCreated(Record user) {
        for (PlatformHook l : hooks)
            l.onUserCreated(user);
    }

    public void fireUserDestroyed(Record user) {
        for (PlatformHook l : hooks)
            l.onUserDestroyed(user);
    }

    public void fireUserProfileChanged(Record changed) {
        for (PlatformHook l : hooks)
            l.onUserProfileChanged(changed);
    }

    public void fireFriendshipChanged(Record changed) {
        for (PlatformHook l : hooks)
            l.onFriendshipChange(changed);
    }

    public void sendAccountInfo(Record changed){
        for (PlatformHook l : hooks)
            l.onSetFriendChange(changed);
    }

}
