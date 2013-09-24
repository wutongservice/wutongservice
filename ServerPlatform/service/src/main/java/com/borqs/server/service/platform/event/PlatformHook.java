package com.borqs.server.service.platform.event;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.event.Hook;

public interface PlatformHook extends Hook {
    // Users
    void onUserCreated(Record user);
    void onUserDestroyed(Record user);
    void onUserProfileChanged(Record changed);
    void onFriendshipChange(Record changed);
    void onSetFriendChange(Record changed);
}
