package com.borqs.server.platform.feature.conversation;

import com.borqs.server.platform.context.Context;

@Deprecated
public interface Conversationable {
    Conversations getConversations(Context ctx, Conversations reuse, Object[] ids);
    Conversations getConversations(Context ctx, Conversations reuse, long... ids);
    Conversations getConversations(Context ctx, Conversations reuse, String... ids);
}
