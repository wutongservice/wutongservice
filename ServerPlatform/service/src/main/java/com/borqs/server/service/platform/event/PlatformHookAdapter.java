package com.borqs.server.service.platform.event;


import com.borqs.server.base.data.Record;

public class PlatformHookAdapter implements PlatformHook {
    @Override
    public void onUserCreated(Record user) {
    }

    @Override
    public void onUserDestroyed(Record user) {
    }

    @Override
    public void onUserProfileChanged(Record changed) {
    }

    @Override
    public void onFriendshipChange(Record changed) {
    }

    @Override
    public void onSetFriendChange(Record changed) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
