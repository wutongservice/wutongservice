package com.borqs.server.wutong.commons;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;

public interface WutongHook {
    void onUserCreated(Context ctx, Record user);
    void onUserDestroyed(Context ctx, Record user);
    void onUserProfileChanged(Context ctx, Record changed);
    void onFriendshipChange(Context ctx, Record changed);
    void onSetFriendChange(Context ctx, Record changed);
}
