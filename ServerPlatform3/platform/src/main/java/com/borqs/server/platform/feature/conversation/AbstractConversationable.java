package com.borqs.server.platform.feature.conversation;


import com.borqs.server.platform.context.Context;
import org.apache.commons.lang.ArrayUtils;

public abstract class AbstractConversationable implements Conversationable {
    protected AbstractConversationable() {
    }

    @Override
    public final Conversations getConversations(Context ctx, Conversations reuse, long... ids) {
        return getConversations(ctx, reuse, ArrayUtils.toObject(ids));
    }

    @Override
    public final Conversations getConversations(Context ctx, Conversations reuse, String... ids) {
        return getConversations(ctx, reuse, (Object[]) ids);
    }
}
